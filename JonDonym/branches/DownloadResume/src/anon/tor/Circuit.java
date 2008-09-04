/*
 Copyright (c) 2004, The JAP-Team
 All rights reserved.
 Redistribution and use in source and binary forms, with or without modification,
 are permitted provided that the following conditions are met:

 - Redistributions of source code must retain the above copyright notice,
   this list of conditions and the following disclaimer.

 - Redistributions in binary form must reproduce the above copyright notice,
   this list of conditions and the following disclaimer in the documentation and/or
  other materials provided with the distribution.

 - Neither the name of the University of Technology Dresden, Germany nor the names of its contributors
   may be used to endorse or promote products derived from this software without specific
  prior written permission.


 THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS ``AS IS'' AND ANY EXPRESS
 OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY
 AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE REGENTS OR CONTRIBUTORS
 BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA,
 OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER
 IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
 OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE
 */
/*
 * Created on Apr 21, 2004
 */
package anon.tor;

import java.io.IOException;
import java.security.SecureRandom;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import anon.crypto.MyRandom;
import anon.tor.cells.Cell;
import anon.tor.cells.CreatedCell;
import anon.tor.cells.DestroyCell;
import anon.tor.cells.PaddingCell;
import anon.tor.cells.RelayCell;
import anon.tor.ordescription.ORDescriptor;
import anon.util.ByteArrayUtil;
import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;
import anon.ErrorCodes;

/**
 * @author stefan
 *
 */
public final class Circuit implements Runnable
{

	//max number of streams over a circuit
	public final static int MAX_STREAMS_OVER_CIRCUIT = 1000;

	private OnionRouter m_FirstOR;
	private ORDescriptor m_lastORDescription;
	private FirstOnionRouterConnection m_FirstORConnection;
	private Vector m_onionRouters;

	private int m_circID;
	private Hashtable m_streams;

	private volatile int m_State;
	//Workaround if to many relay cells signals error -> just shutdown the circuit...
	private volatile int m_iRelayErrors;

	///Is this circuit destroyed?
	private final static int STATE_CLOSED = 0;

	/// Can we creat new channels?
	private final static int STATE_SHUTDOWN = 1;
	private final static int STATE_READY = 2;
	private final static int STATE_CREATING = 3;
	private int m_streamCounter;
	private int m_circuitLength;
	private int m_MaxStreamsPerCircuit;
	private volatile int m_recvCellCounter;
	private volatile int m_sendCellCounter;
	private boolean m_destroyed;

	private byte[] m_resolvedData;
	private Object m_oResolveSync;
	private Object m_oSendCellCounterSync;
	private Object m_oSendSync;
	private Object m_oDestroyedByPeerSync;
	private volatile boolean m_bReceivedCreatedOrExtendedCell;
	private Object m_oNotifySync;
	private MyRandom m_rand;

	private Thread m_threadSendCellLoop; //this thread takes cells from the cell queue and sends them
	private CellQueue m_cellqueueSend;

	/**
	 * constructor
	 * @param circID
	 * ID of this circuit
	 * @param orList
	 * list of onionrouters to use for this circuit
	 * @param onionProxy
	 * FirstOnionRouter, where all the data will be send. the onionProxy has to be the firstOR in the orList
	 * @throws IOException
	 */
	public Circuit(int circID, FirstOnionRouterConnection onionProxy, Vector orList) throws
		IOException
	{
		m_oResolveSync = new Object();
		m_oSendCellCounterSync = new Object();
		m_oSendSync = new Object();
		m_oDestroyedByPeerSync = new Object();
		m_oNotifySync = new Object();
		m_FirstORConnection = onionProxy;
		m_circID = circID;
		m_streams = new Hashtable();
		m_streamCounter = 0;
		m_MaxStreamsPerCircuit = MAX_STREAMS_OVER_CIRCUIT;
		m_onionRouters = (Vector) orList.clone();
		m_circuitLength = orList.size();
		m_lastORDescription = (ORDescriptor) m_onionRouters.elementAt(m_circuitLength - 1);
		if (this.m_onionRouters.size() < 1)
		{
			throw new IOException("No Onionrouters defined for this circuit");
		}
		this.m_recvCellCounter = 1000;
		this.m_sendCellCounter = 1000;
		this.m_rand = new MyRandom(new SecureRandom());
		m_State = STATE_CREATING;
		m_destroyed = false;
		m_iRelayErrors = 0;
		m_cellqueueSend = new CellQueue();
		m_threadSendCellLoop = new Thread(this, "Tor - Circuit - SendCellLoop");
		m_threadSendCellLoop.setDaemon(true);
		m_threadSendCellLoop.start();
	}

	private void addToSendCellCounter(int value)
	{
		synchronized (m_oSendCellCounterSync)
		{
			m_sendCellCounter += value;
		}
	}

	/**
	 * creates a circuit and connects to all onionrouters
	 * @throws IOException
	 */
	protected void create() throws IOException
	{
		LogHolder.log(LogLevel.DEBUG, LogType.TOR, "[TOR] Creating Circuit '" + m_circID + "'");
		m_FirstOR = new OnionRouter(m_circID, (ORDescriptor) (m_onionRouters.elementAt(0)));
		try
		{
			synchronized (m_oNotifySync)
			{
				m_bReceivedCreatedOrExtendedCell = false;
				m_FirstORConnection.send(m_FirstOR.createConnection());
				//wait until a created cell arrives or an erro occured or a time out occured
				m_oNotifySync.wait(15000);
			}
			if (m_State != STATE_CREATING || !m_bReceivedCreatedOrExtendedCell) //Error or time out
			{
				throw new IOException("Error during Circuit creation");
			}
			LogHolder.log(LogLevel.DEBUG, LogType.TOR, "[TOR] created!");
			for (int i = 1; i < this.m_onionRouters.size(); i++)
			{
				ORDescriptor nextOR = (ORDescriptor) (m_onionRouters.elementAt(i));
				LogHolder.log(LogLevel.DEBUG, LogType.TOR, "[TOR] trying to extend!");
				synchronized (m_oNotifySync)
				{
					m_bReceivedCreatedOrExtendedCell = false;
					RelayCell cell = m_FirstOR.extendConnection(nextOR);
					m_FirstORConnection.send(cell);
					m_oNotifySync.wait(25000);
				}
				if (m_State != STATE_CREATING || !m_bReceivedCreatedOrExtendedCell)
				{
					throw new IOException("Error during Circuit creation");
				}
				LogHolder.log(LogLevel.DEBUG, LogType.TOR, "[TOR] extended!");
			}
			m_State = STATE_READY;
			LogHolder.log(LogLevel.DEBUG, LogType.MISC,
						  "[TOR] Circuit '" + this.m_circID + "' ready!!! - Length of this Circuit : " +
						  this.m_circuitLength + " Onionrouters");
		}
		catch (Exception ex)
		{
			//send destroy on error, when circuit wasn't allready destroyed
			try
			{
				if (!m_destroyed)
				{
					this.send(new DestroyCell(this.m_circID));
				}
			}
			catch (Throwable t)
			{
			}
			m_State = STATE_CLOSED;
			throw new IOException(ex.getLocalizedMessage());
		}
	}

	/**
	 * shutdown this circuit so that it cannot be used for new connections
	 * but leaves the circuit opened for all currently used channels
	 *
	 * to close the circuit immediately use close()
	 *
	 * @throws Exception
	 */
	public synchronized void shutdown()
	{
		if (m_State == STATE_CLOSED || m_State == STATE_SHUTDOWN)
		{
			return;
		}
		if (m_streams.isEmpty())
		{
			close();
		}
		m_State = STATE_SHUTDOWN;
	}

	/**
	 * closes the circuit immediately.
	 *
	 * @throws Exception
	 */
	public synchronized void close()
	{
		if (m_State == STATE_CLOSED)
		{
			return;
		}
		try
		{
			Enumeration enumer = m_streams.elements();
			while (enumer.hasMoreElements())
			{
				try
				{
					TorChannel c = (TorChannel) enumer.nextElement();
					c.close();
				}
				catch (Exception e1)
				{
				}
			}
		}
		catch (Exception e)
		{}
		m_streams.clear();
		try
		{
			m_FirstORConnection.send(new DestroyCell(m_circID));
			LogHolder.log(LogLevel.DEBUG, LogType.TOR, "[TOR] circuit " + m_circID + " destroyed!");
		}
		catch (Exception e)
		{}
		///todo flush cell send queue!
		m_State = STATE_CLOSED;
		try
		{
			m_threadSendCellLoop.join(2000);
		}
		catch (InterruptedException ex)
		{
		}
		m_FirstORConnection.notifyCircuitClosed(this);
	}

	/**
	 * circuit was destroyed by peer.
	 *
	 * @throws Exception
	 */
	public void destroyedByPeer()
	{
		synchronized (m_oDestroyedByPeerSync)
		{
			try
			{
				Enumeration enumer = m_streams.elements();
				while (enumer.hasMoreElements())
				{
					try
					{
						TorChannel c = (TorChannel) enumer.nextElement();
						c.closedByPeer();
					}
					catch (Exception e1)
					{
					}
				}
				m_streams.clear();
				m_FirstORConnection.notifyCircuitClosed(this);
			}
			catch (Exception e)
			{}
			m_State = STATE_CLOSED;
		}
		synchronized (m_oNotifySync)
		{
			m_oNotifySync.notify();
		}
	}

	/**
	 * check if the circuit is already destroyed
	 *
	 * @return if the channel is closed
	 */
	public boolean isClosed()
	{
		return m_State == STATE_CLOSED;
	}

	/** check if the circuit is already shutdown
	 *
	 * @return if the circuit is shutdown
	 */
	public boolean isShutdown()
	{
		return (m_State == STATE_SHUTDOWN) || (m_State == STATE_CLOSED);
	}

	/**
	 * dispatches cells to the opended channels
	 * @param cell
	 * cell
	 * @throws IOException
	 */
	public void dispatchCell(Cell cell) throws IOException
	{
		try
		{
			if (cell instanceof RelayCell)
			{
				if (m_State == STATE_CREATING)
				{
					if (!m_FirstOR.checkExtendedCell( (RelayCell) cell))
					{
						//((RelayCell)cell).getRelayCommand()==9 -> truncated
						//maybe we can handle it better later
						this.send(new DestroyCell(this.m_circID));
						m_State = STATE_CLOSED;
						destroyedByPeer();
						m_destroyed = true;
					}
					else
					{
						synchronized (m_oNotifySync)
						{
							m_bReceivedCreatedOrExtendedCell = true;
							m_oNotifySync.notify();
						}
					}
				}
				else
				{
					m_recvCellCounter--;
					if (m_recvCellCounter < 900)
					{
						RelayCell rc = new RelayCell(m_circID, RelayCell.RELAY_SENDME, 0, null);
						send(rc);
						m_recvCellCounter += 100;
					}

					RelayCell c = m_FirstOR.decryptCell( (RelayCell) cell);
					Integer streamID = c.getStreamID();
					if (c.getStreamID().intValue() == 0) // Relay cells that belong to the circuit
					{
						switch (c.getRelayCommand())
						{
							case RelayCell.RELAY_SENDME:
							{
								addToSendCellCounter(100);
								break;
							}
							default:
							{
								LogHolder.log(LogLevel.DEBUG, LogType.TOR, "Upps...");
							}
						}
					}
					else if (m_streams.containsKey(streamID)) //dispatch cell to the circuit where it belongs to
					{
						if (c.getRelayCommand() == RelayCell.RELAY_RESOLVED)
						{
							byte[] tmp = c.getPayload();
							m_resolvedData = ByteArrayUtil.copy(tmp, 11,
								( (tmp[9] & 0xFF) << 8) + (tmp[10] & 0xFF));
							synchronized (m_oNotifySync)
							{
								m_oNotifySync.notify();
							}
						}
						else
						{
							TorChannel channel = (TorChannel) m_streams.get(streamID);
							if (channel != null)
							{
								if (channel.dispatchCell(c) != ErrorCodes.E_SUCCESS)
								{
									m_iRelayErrors++;
									if (m_iRelayErrors > 10)
									{
										shutdown();
									}
								}
							}
							else
							{
								LogHolder.log(LogLevel.DEBUG, LogType.TOR, "Upps...");

							}
						}
					}
					else
					{
						LogHolder.log(LogLevel.DEBUG, LogType.TOR, "Upps...Unknown stream");
					}
				}
			}
			else if (cell instanceof CreatedCell)
			{
				if (!m_FirstOR.checkCreatedCell(cell))
				{
					LogHolder.log(LogLevel.DEBUG, LogType.TOR,
								  "[TOR] Should never be here - 'created' cell was wrong");
					m_State = STATE_CLOSED;
					destroyedByPeer();
				}
				else
				{
					LogHolder.log(LogLevel.DEBUG, LogType.TOR, "[TOR] Connected to the first OR");
					synchronized (m_oNotifySync)
					{
						m_bReceivedCreatedOrExtendedCell = true;
						m_oNotifySync.notify();
					}
				}
			}
			else if (cell instanceof PaddingCell)
			{

			}
			else if (cell instanceof DestroyCell)
			{
				int reason = cell.getPayload()[0];
				LogHolder.log(LogLevel.DEBUG, LogType.TOR,
							  "[TOR] recieved destroycell - circuit destroyed - reason: " +
							  Integer.toString(reason));
				m_destroyed = true;
				destroyedByPeer();
			}
			else
			{
				LogHolder.log(LogLevel.DEBUG, LogType.MISC, "tor kein bekannter cell type");
			}
		}
		catch (Exception ex)
		{
			destroyedByPeer();
			throw new IOException("Unable to dispatch the cell \n" + ex.getLocalizedMessage());
		}
	}

	/**
	 * sends a cell through the circuit - the cells are placed in a queue for sending
	 * @param cell
	 * cell to send
	 * @throws IOException
	 */
	public void send(Cell cell) throws IOException, Exception
	{
		if (m_State == STATE_CLOSED)
		{
			throw new IOException("circuit alread closed");
		}
		m_cellqueueSend.addElement(cell);
	}

	/**
	 * urgently sends a cell through the circuit (send it directly without placing it in the send queue)
	 * @param cell
	 * cell to send
	 * @throws IOException
	 */
	public void sendUrgent(Cell cell) throws IOException, Exception
	{
		if (m_State == STATE_CLOSED)
		{
			throw new IOException("circuit alread closed");
		}
		synchronized (m_oSendSync)
		{
			if (cell instanceof RelayCell)
			{
				cell = m_FirstOR.encryptCell( (RelayCell) cell);
				addToSendCellCounter( -1);

			}
			m_FirstORConnection.send(cell);
		}
	}

	/**
	 * Returns a address to a given name
	 * @param name
	 * @return
	 *  Type   (1 octet)
	 *  Length (1 octet)
	 *  Value  (variable-width)
	 *"Length" is the length of the Value field.
	 * "Type" is one of:
	 * 0x04 -- IPv4 address
	 * 0x06 -- IPv6 address
	 * 0xF0 -- Error, transient
	 * 0xF1 -- Error, nontransient
	 */
	public String resolveDNS(String name)
	{
		if (m_State != STATE_READY)
		{
			return null;
		}
		synchronized (m_oResolveSync)
		{
			Integer resolveStreamID;
			synchronized (m_streams)
			{
				do
				{
					resolveStreamID = new Integer(m_rand.nextInt(65535));
				}
				while (m_streams.containsKey(resolveStreamID));
				//temp add this stream id...
				m_streams.put(resolveStreamID, resolveStreamID);
			}
			RelayCell cell = new RelayCell(getCircID(), RelayCell.RELAY_RESOLVE, resolveStreamID.intValue(),
										   name.getBytes());
			synchronized (m_oNotifySync)
			{
				try
				{
					m_resolvedData = null;
					send(cell);
					m_oNotifySync.wait(20000);
				}
				catch (Exception ex)
				{
					m_streams.remove(resolveStreamID);
					return null;
				}
			}
			m_streams.remove(resolveStreamID);
			if (m_State == STATE_CLOSED || m_resolvedData == null || m_resolvedData[0] != 4 ||
				m_resolvedData[1] != 4)
			{
				return null;
			}
			StringBuffer sb = new StringBuffer();
			sb.append(Integer.toString(m_resolvedData[2] & 0x00FF));
			sb.append('.');
			sb.append(Integer.toString(m_resolvedData[3] & 0x00FF));
			sb.append('.');
			sb.append(Integer.toString(m_resolvedData[4] & 0x00FF));
			sb.append('.');
			sb.append(Integer.toString(m_resolvedData[5] & 0x00FF));
			return sb.toString();
		}
	}

	/**
	 * closes a stream
	 * @param streamID
	 * streamID
	 * @throws Exception
	 */
	protected void close(int streamID) throws Exception
	{
		if (m_State == STATE_CLOSED)
		{
			return;
		}
		final byte[] reason = new byte[]
			{
			6};
		Integer key = new Integer(streamID);
		if (m_streams.containsKey(key))
		{
			m_streams.remove(key);
			RelayCell cell = new RelayCell(m_circID, RelayCell.RELAY_END, streamID, reason);
			send(cell);
			if (m_State == STATE_SHUTDOWN && m_streams.isEmpty())
			{ //close this circuit after the last stream was closed
				close();
			}
		}
	}

	/**
	 * returns the ID of this circuit
	 * @return ID
	 */
	public int getCircID()
	{
		return m_circID;
	}

	/**
	 * creates a channel through the tor-network
	 * @param addr
	 * address of the server you want do connect
	 * @param port
	 * port
	 * @return
	 * a channel
	 * @throws IOException
	 */
	public synchronized TorChannel createChannel(String addr, int port) throws IOException
	{
		TorChannel channel = new TorChannel();
		int ret = connectChannel(channel, addr, port);
		if (ret != ErrorCodes.E_SUCCESS)
		{
			throw new IOException("Circuit:createChannel(addr,port) failed! Reason:" + Integer.toString(ret));
		}
		return channel;
	}

	/**
	 * creates a channel through the tor-network
	 * @param addr
	 * address of the server you want do connect
	 * @param port
	 * port
	 * @return
	 * ErrorCode.E_SUCCESS if channel could be established
	 * @return ErrorCodes.E_NOT_CONNECTED if the circuit is not connected or shutdown
	 * @return ErrorCodes.E_CONNECT if the channel could not be established
	 * @return ErrorCode.E_UNKNOWN otherwise
	 */
	protected int connectChannel(TorChannel channel, String addr, int port)
	{
		try
		{
			if (isShutdown())
			{
				LogHolder.log(LogLevel.DEBUG, LogType.TOR,
							  "Circuit:connectChannel() - Circuit Closed - cannot connect");
				return ErrorCodes.E_NOT_CONNECTED;
			}
			Integer streamID;
			int ret = ErrorCodes.E_SUCCESS;
			synchronized (this)
			{
				m_streamCounter++;
				synchronized (m_streams)
				{
					do
					//nearly dead code (29/06/2004)
					//	LJ OKI918
					{
						streamID = new Integer(m_rand.nextInt(0xFFFF));
					}
					while (m_streams.contains(streamID));
					channel.setStreamID(streamID.intValue());
					channel.setCircuit(this);
					m_streams.put(streamID, channel);
				}
			}
			if (!channel.connect(addr, port))
			{
				synchronized (this)
				{
					m_streams.remove(streamID);
				}
				LogHolder.log(LogLevel.DEBUG, LogType.TOR,
							  "Circuit:connectChannel() - Channel could not be created");
				ret = ErrorCodes.E_CONNECT;
			}

			if (m_streamCounter >= m_MaxStreamsPerCircuit)
			{
				shutdown();
			}
			return ret;
		}
		catch (Throwable t)
		{
			LogHolder.log(LogLevel.DEBUG, LogType.TOR, "Circuit:connectChannel() - Unkown Error", t);
			return ErrorCodes.E_UNKNOWN;
		}
	}

	/**
	 * Checks if it is possible to connect to given host on a given port via this circuit
	 * @param adr
	 * address
	 * @param port
	 * port
	 * @return
	 * true if a connection is possible, else if it is not
	 */
	public boolean isAllowed(String adr, int port)
	{
		return m_lastORDescription.getAcl().isAllowed(adr, port);
	}

	/**
	 * Sets the maximum number of possible streams over this circuit
	 * @param i
	 * number of streams
	 */
	public void setMaxNrOfStreams(int i)
	{
		if (i > 0 && i <= MAX_STREAMS_OVER_CIRCUIT)
		{
			m_MaxStreamsPerCircuit = i;
		}
		if (m_streamCounter >= MAX_STREAMS_OVER_CIRCUIT)
		{
			shutdown();
		}
	}

	public void run()
	{
		try
		{
			while (m_State != STATE_CLOSED)
			{
				///@todo remove this busy waiting - make a more intelligent CellQueue!
				while (m_cellqueueSend.isEmpty())
				{
					try
					{
						if (m_State == STATE_CLOSED)
						{
							return;
						}
						Thread.sleep(100);
					}
					catch (Exception e)
					{
					}
				}
				Cell c = m_cellqueueSend.removeElement();
				while (m_sendCellCounter <= 0 && (m_State != STATE_CLOSED))
				{
					try
					{
						Thread.sleep(100);
					}
					catch (Exception e)
					{
					}
				}
				synchronized (m_oSendSync)
				{
					if (! (c instanceof RelayCell))
					{
						LogHolder.log(LogLevel.DEBUG, LogType.TOR,
									  "Tor-Circuit-sendCellLoop: sending no releay cell.");
					}
					else
					{
						TorChannel channel = (TorChannel) m_streams.get( ( (RelayCell) c).getStreamID());
						c = m_FirstOR.encryptCell( (RelayCell) c);
						addToSendCellCounter( -1);
						if (channel != null)
						{
							channel.decreaseSendRelayCellsWaitingForDelivery();
						}
					}
					m_FirstORConnection.send(c);
				}
			}
		}
		catch (Throwable t)
		{
			destroyedByPeer();
		}
	}

}
