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
 * Created on May 9, 2004
 */
package anon.tor;

import java.io.IOException;

import anon.shared.AbstractChannel;
import anon.tor.cells.RelayCell;
import anon.util.ByteArrayUtil;
import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;
import anon.ErrorCodes;

/**
 * @author stefan
 */
public class TorChannel extends AbstractChannel
{

	private final static int MAX_CELL_DATA = 498;

	protected Circuit m_circuit;
	///Send (receive) cell windows used by the Tor flow control protocol
	private volatile int m_recvcellcounter;
	private volatile int m_sendcellcounter;

	///Number of relay cells waiting in the circuit send queue for delivery
	private volatile int m_iSendRelayCellsWaitingForDelivery;

	private volatile boolean m_bChannelCreated;
	private volatile boolean m_bCreateError;
	private Object m_oWaitForOpen;
	private Object m_oSyncSendCellCounter;
	private Object m_oSyncSend;
	private Object m_oSyncSendRelayCellsWaitingForDelivery;
	//Flag which signals if we should ignore any "close" actvity.
	//This is basically necessary in case of error during channel establishment,
	//so that we have a chance to retry without closing the whole channel.
	private volatile boolean m_bDoNotCloseChannelOnError;

	public TorChannel()
	{
		super();
		m_oWaitForOpen = new Object();
		m_oSyncSendCellCounter = new Object();
		m_oSyncSend = new Object();
		m_oSyncSendRelayCellsWaitingForDelivery = new Object();
		m_bDoNotCloseChannelOnError = false;
		m_iSendRelayCellsWaitingForDelivery = 0;
	}

	private void addToSendCellCounter(int value)
	{
		synchronized (m_oSyncSendCellCounter)
		{
			m_sendcellcounter += value;
		}
	}

	protected void decreaseSendRelayCellsWaitingForDelivery()
	{
		synchronized (m_oSyncSendRelayCellsWaitingForDelivery)
		{
			m_iSendRelayCellsWaitingForDelivery--;
		}
	}

	protected void setStreamID(int id)
	{
		m_id = id;
	}

	protected void setCircuit(Circuit c)
	{
		m_circuit = c;
	}

	public int getOutputBlockSize()
	{
		return MAX_CELL_DATA;
	}

	protected void send(byte[] arg0, int len) throws IOException
	{
		if (m_bIsClosed || m_bIsClosedByPeer)
		{
			throw new IOException("Tor channel is closed");
		}
		synchronized (m_oSyncSend)
		{
			byte[] b = arg0;
			RelayCell cell;
			while (len != 0 && !m_bIsClosed)
			{
				if (len > MAX_CELL_DATA)
				{
					cell = new RelayCell(m_circuit.getCircID(), RelayCell.RELAY_DATA, m_id,
										 ByteArrayUtil.copy(b, 0, MAX_CELL_DATA));
					b = ByteArrayUtil.copy(b, MAX_CELL_DATA, len - MAX_CELL_DATA);
					len -= MAX_CELL_DATA;
				}
				else
				{
					cell = new RelayCell(m_circuit.getCircID(), RelayCell.RELAY_DATA, m_id,
										 ByteArrayUtil.copy(b, 0, len));
					len = 0;
				}
				try
				{
					while ( (m_sendcellcounter <= 0 || m_iSendRelayCellsWaitingForDelivery > 10)
						   && ! (m_bIsClosed || m_bIsClosedByPeer))

					{ //@todo remove this busy waiting
						try
						{
							Thread.sleep(100);
						}
						catch (Exception e)
						{
						}
					}
					synchronized (m_oSyncSendRelayCellsWaitingForDelivery)
					{
						m_iSendRelayCellsWaitingForDelivery++;
					}
					m_circuit.send(cell);

				}
				catch (Throwable t)
				{
					throw new IOException("TorChannel send - error in sending a cell!");
				}
				addToSendCellCounter( -1);
			}
		}
	}

	/** Close this channel (called from inside the class) but respecting the doNotCloseOnError flag!*/
	private void internalClose()
	{
		m_bCreateError = true;
		if (!m_bDoNotCloseChannelOnError)
		{
			close();
		}
		else
		{ //just send the close cell
			final byte[] reason = new byte[]
				{
				6};
			RelayCell cell = new RelayCell(m_circuit.getCircID(), RelayCell.RELAY_END, m_id, reason);
			try
			{
				m_circuit.sendUrgent(cell);
			}
			catch (Exception ex)
			{
			}
		}
	}

	// Note: This close method always closed the channel - it will not respect
	// the doNotCloseOnError flags - mostly because this method
	//is called from "outside" e.g. if the socket belonging to
	//the channel is closed.
	public void close()
	{
		m_bCreateError = true;
		super.close();
		synchronized (m_oWaitForOpen)
		{
			m_oWaitForOpen.notify();
		}
	}
	
	public boolean isClosed()
	{
		return m_bCreateError;
	}

	public void closedByPeer()
	{
		m_bCreateError = true;
		if (!m_bDoNotCloseChannelOnError)
		{
			super.closedByPeer();
		}
		synchronized (m_oWaitForOpen)
		{
			m_oWaitForOpen.notify();
		}

	}

	protected void close_impl()
	{
		try
		{
			if (!m_bIsClosed)
			{
				m_circuit.close(m_id);
			}
		}
		catch (Exception e)
		{
		}
	}

	/* Sets if the channel should be left open even after connection error for easy re-try.
	 * @param b if true, close the channel if the connection
	 * could not be established
	 * otherwise let the channel 'open' so that we can re-try to connect to the destination address
	 * using maybe another circuit
	 */
	protected void setDoNotCloseChannelOnErrorDuringConnect(boolean b)
	{
		m_bDoNotCloseChannelOnError = b;
	}

	/**
	 * connects to a host over the Tor network
	 * @param addr
	 * address
	 * @param port
	 * port
	 * @throws ConnectException
	 */
	protected boolean connect(String addr, int port)
	{
		try
		{
			if (m_bIsClosed || m_bIsClosedByPeer)
			{
				return false;
			}
			m_recvcellcounter = 500;
			m_sendcellcounter = 500;
			byte[] data = (addr + ":" + Integer.toString(port)).getBytes();
			data = ByteArrayUtil.conc(data, new byte[1]);
			RelayCell cell = new RelayCell(m_circuit.getCircID(), RelayCell.RELAY_BEGIN, m_id, data);
			m_bChannelCreated = false;
			m_bCreateError = false;
			m_circuit.sendUrgent(cell);
			synchronized (m_oWaitForOpen)
			{
				//I got many InterruptedExceptions here - I do not no why at the moment...
				//Therefore the following "workaorund" to ensure, that we
				//wait the expected time
				long currTime = System.currentTimeMillis();
				int waitTime = 60000;
				while (waitTime > 0)
				{
					try
					{
						m_oWaitForOpen.wait(waitTime);
					}
					catch (InterruptedException e)
					{
						LogHolder.log(LogLevel.DEBUG, LogType.TOR,
									  "InterruptedException in TorChannel:connect()");
					}
					if (m_bCreateError)
					{
						LogHolder.log(LogLevel.DEBUG, LogType.TOR,
									  "TorChannel - connect() - establishing channel over circuit NOT successful. Channel was closed before!");
						return false;
					}
					else if (m_bChannelCreated)
					{
						m_bDoNotCloseChannelOnError = false;
						LogHolder.log(LogLevel.DEBUG, LogType.TOR,
									  "TorChannel - connect() - establishing channel over circuit successful. Time needed [ms]: " +
									  Long.toString(System.currentTimeMillis() - currTime));
						return true;
					}
					long diffTime = System.currentTimeMillis() - currTime;
					if (diffTime < 0) //the system clock was changed in a bad way...
					{
						return false;
					}
					waitTime -= diffTime;
				}
			}
			LogHolder.log(LogLevel.DEBUG, LogType.TOR,
						  "TorChannel - connect() - establishing channel over circuit NOT successful. Timed out!");
			internalClose();
			return false;
		}
		catch (Throwable t)
		{
			LogHolder.log(LogLevel.DEBUG, LogType.TOR, "Exception in TorChannel:connect()");
			internalClose();
			return false;
		}
	}

	/**
	 * dispatches the cells to the outputstream
	 * @param cell
	 * cell
	 */
	public int dispatchCell(RelayCell cell)
	{
		int ret = ErrorCodes.E_SUCCESS;
		switch (cell.getRelayCommand())
		{
			case RelayCell.RELAY_CONNECTED:
			{
				m_bChannelCreated = true;
				m_bDoNotCloseChannelOnError = false;
				synchronized (m_oWaitForOpen)
				{
					m_oWaitForOpen.notify();
				}
				break;
			}
			case RelayCell.RELAY_SENDME:
			{
				addToSendCellCounter(50);
				break;
			}
			case RelayCell.RELAY_DATA:
			{
				m_recvcellcounter--;
				if (m_recvcellcounter < 250)
				{
					RelayCell rc = new RelayCell(m_circuit.getCircID(), RelayCell.RELAY_SENDME, m_id, null);
					try
					{
						m_circuit.sendUrgent(rc);
					}
					catch (Throwable t)
					{
						closedByPeer();
						return ret;
					}
					m_recvcellcounter += 50;
				}
				try
				{
					byte[] buffer = cell.getRelayPayload();
					recv(buffer, 0, buffer.length);
				}
				catch (Exception ex)
				{
					closedByPeer();
					return ret;
				}
				break ;
			}
			case RelayCell.RELAY_END:
			{
				int reason = cell.getPayload()[0];
				LogHolder.log(LogLevel.DEBUG, LogType.TOR,
							  "RELAY_END: Relay stream closed with reason: " + reason);
				if (reason == 1)
				{ //unknown reason
					ret = ErrorCodes.E_UNKNOWN;
				}
				closedByPeer();
				break;
			}
			default:
			{
				closedByPeer();
			}
		}
		return ret;
	}

	/**
	 * gets if the connection was closed by peer
	 * @return
	 */
	public boolean isClosedByPeer()
	{
		return m_bIsClosedByPeer;
	}

}
