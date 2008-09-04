/*
 Copyright (c) 2000 - 2005, The JAP-Team
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
package forward.server;

import java.net.Socket;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import anon.transport.address.Endpoint;
import anon.transport.connection.IStreamConnection;


import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;

/**
 * This class allocates the bandwidth to all forwarded connections. Also the server part is
 * managed here.
 */
public class ForwardScheduler implements Runnable
{

	/**
	 * This is the time between two allocation rounds in milliseconds.
	 */
	private static final long CYCLE_TIME = (long) 100;

	/**
	 * This stores the maximum number of simultaneous connections. If this number is reached,
	 * any new connection will be rejected (immediately closed after acception) until one of the
	 * active connections is closed.
	 */
	private int m_nrOfConnections;

	/**
	 * The maximum bandwidth for all connections together in bytes/second. Because of the forwarding,
	 * this is the amount of upstream = amount of downstream data.
	 */
	private int m_netBandwidth;

	/**
	 * This stores all active connections (ForwardConnection).
	 * @see ForwardConnection
	 */
	private Vector m_connectionHandler;

	/**
	 * This value is set to true, for signalizing the shutdown of this ForwardScheduler to the
	 * internal thread.
	 * @see run()
	 */
	private boolean m_shutdown;

	/**
	 * This stores the internal scheduling thread.
	 */
	private Thread m_schedulerThread;

	/**
	 * This stores the associated ServerManagers (ID as keys and the instances as values) managing
	 * the server sockets of this ForwardScheduler.
	 * @see ServerManager
	 */
	private Hashtable m_serverManagers;

	/**
	 * This stores the statistics for the ForwardScheduler.
	 */
	private ForwardSchedulerStatistics m_statistics;

	/**
	 * Creates a new ForwardScheduler. Also the internal bandwidth allocation thread is started.
	 * The initial number of simultaneously forwarded client connections is set to 0 and the
	 * bandwidth is also set to 0 bytes/sec.
	 */
	public ForwardScheduler()
	{
		m_nrOfConnections = 0;
		m_netBandwidth = 0;
		m_connectionHandler = new Vector();
		m_serverManagers = new Hashtable();
		m_shutdown = false;
		m_schedulerThread = new Thread(this, "ForwardScheduler");
		m_schedulerThread.setDaemon(true);
		m_schedulerThread.start();
		m_statistics = new ForwardSchedulerStatistics();
	}

	/**
	 * The associated ServerManager signalize any new connection here. If there are empty slots,
	 * we accept this new connection. If we already holds the maximum number of connections, the
	 * new connection is immediately closed.
	 *
	 * @param a_newConnection The new connection.
	 */
	public void handleNewConnection(IStreamConnection a_newConnection)
	{
		boolean connectionAccepted = false;
		ForwardConnection newConnection = null;
		synchronized (m_connectionHandler)
		{
			if ( (m_connectionHandler.size() < m_nrOfConnections) && (m_shutdown == false))
			{
				try
				{
					newConnection = new ForwardConnection(a_newConnection, this);
					m_connectionHandler.addElement(newConnection);
					connectionAccepted = true;
					LogHolder.log(LogLevel.INFO, LogType.NET,
							  "ForwardScheduler: handleNewConnection: New forwarding connection from " +
							  Endpoint.toURN(a_newConnection.getRemoteAddress()) + " accepted."); 
					m_statistics.incrementAcceptedConnections();
				}
				catch (Exception e)
				{
					LogHolder.log(LogLevel.EXCEPTION, LogType.NET,
							"ForwardScheduler: handleNewConnection: Error initializing protocol on forwarding connection from " +
							Endpoint.toURN(a_newConnection.getRemoteAddress()) + " (" + e.toString() + ").");
				}
			}
		}
		if (connectionAccepted == false)
		{
			/* no success while accepting the connection -> maximum connection number reached or an
			 * error while initialzing the protocol
			 */
			LogHolder.log(LogLevel.INFO, LogType.NET,
						  "ForwardScheduler: handleNewConnection: New forwarding connection from " +
						  Endpoint.toURN(a_newConnection.getRemoteAddress()) +
						  " rejected (maximum number of connections is reached).");
			m_statistics.incrementRejectedConnections();
			try
			{
				a_newConnection.close();
			}
			catch (Exception e)
			{
			}
		}
	}

	/**
	 * This method is called from a ForwardConnection to signalize, that the connection is closed
	 * and can be removed from the connection store of all forwarded connections.
	 *
	 * @param a_connectionToRemove The closed connection which can be removed.
	 */
	public void removeConnection(ForwardConnection a_connectionToRemove)
	{
		synchronized (m_connectionHandler)
		{
			m_connectionHandler.removeElement(a_connectionToRemove);
		}
		LogHolder.log(LogLevel.INFO, LogType.NET,
					  "ForwardScheduler: removeConnection: Forwarded connection from " +
					  a_connectionToRemove.toString() + " was closed.");
	}

	/**
	 * This method must be called, if the ForwardScheduler shall come to an end. All associated
	 * ServerSockets and all forward connections are closed and the internal scheduling thread is
	 * stopped. This method will block until all ServerSockets and forwarded connections are closed
	 * and the internal scheduling thread has come to the end.
	 */
	public void shutdown()
	{
		m_shutdown = true;
		/* close all ServerManagers */
		removeAllServerManagers();
		/* close all forwarded connections */
		synchronized (m_connectionHandler)
		{
			Enumeration connections = m_connectionHandler.elements();
			while (connections.hasMoreElements())
			{
				( (ForwardConnection) (connections.nextElement())).closeConnection();
			}
		}
		/* wait for the internal thread */
		try
		{
			/* interrupt the thread, if it sleeps */
			m_schedulerThread.interrupt();
			m_schedulerThread.join();
		}
		catch (Exception e)
		{
		}
	}

	/**
	 * Returns the maximum bandwidth a connection can use in bytes/sec. That is the total bandwidth,
	 * because if there are no other connections or they have no bytes to transmit, one connection
	 * gan get all the bandwidth we have.
	 *
	 * @return The maximum bandwidth for one connection in bytes/sec.
	 */
	public int getMaximumBandwidth()
	{
		return m_netBandwidth;
	}

	/**
	 * Returns the guaranteed bandwidth for one connection in bytes/sec. If there is no other
	 * bottleneck, every connection gets [total bandwidth / maximum number of connections] as
	 * guaranteed minimal bandwidth.
	 *
	 * @return The guaranteed bandwidth for every connection.
	 */
	public int getGuaranteedBandwidth()
	{
		return (m_netBandwidth / m_nrOfConnections);
	}

	/**
	 * Changes the number of simultaneously forwarded client connections. If the new number is less
	 * than the old one and there are more forwarded connections at the moment, we closes some
	 * randomly choosed connections.
	 *
	 * @a_maximumNumberOfConnections The new maximum number of simultaneously forwarded client
	 *                               connections.
	 */
	public void setMaximumNumberOfConnections(int a_maximumNumberOfConnections)
	{
		if (a_maximumNumberOfConnections >= 0)
		{
			if (a_maximumNumberOfConnections < m_nrOfConnections)
			{
				/* we have to close some connections */
				m_nrOfConnections = a_maximumNumberOfConnections;
				synchronized (m_connectionHandler)
				{
					while (m_connectionHandler.size() > m_nrOfConnections)
					{
						/* close a random connection */
						try
						{
							( (ForwardConnection) (m_connectionHandler.elementAt( (int) (Math.round(Math.abs(
								Math.random() * (double) (m_connectionHandler.size()))))))).closeConnection();
						}
						catch (Exception e)
						{
							/* this can be an IndexOutOfBoundsException, if one connection was closed meanwhile,
							 * and we have wanted to close the last connection in the list -> do nothing
							 */
						}
					}
				}
			}
			else
			{
				/* the new connection number is bigger or equal to the old one */
				m_nrOfConnections = a_maximumNumberOfConnections;
			}
		}
	}

	/**
	 * Changes the maximum bandwidth (net bandwidth, without TCP/IP headers...) which can be used
	 * by all client connections together.
	 *
	 * @param a_netBandwidth The maximum bandwidth for all client connections together (= average
	 *                       upstream = average downstream) in bytes/sec.
	 */
	public void setNetBandwidth(int a_netBandwidth)
	{
		m_netBandwidth = a_netBandwidth;
	}

	/**
	 * Adds a ServerManager to the list of associated ServerManagers. Also the ServerManager is
	 * started (startServerManager() is called). This call throws an exception, if there was an
	 * error while starting the ServerManager.
	 *
	 * @param a_serverManager The ServerManager to add.
	 */
	public void addServerManager(IServerManager a_serverManager) throws Exception
	{
		synchronized (m_serverManagers)
		{
			if (m_shutdown == false)
			{
				/* add it only, if this ForwardScheduler is running and we don't have already a server
				 * manager with the same ID
				 */
				if (m_serverManagers.containsKey(a_serverManager.getId()) == false)
				{
					a_serverManager.startServerManager(this);
					/* the ServerManager was successfully started -> add it to our table */
					m_serverManagers.put(a_serverManager.getId(), a_serverManager);
				}
				else
				{
					throw (new Exception(
						"ForwardScheduler: addServerManager: Already a ServerManager with this ID running."));
				}
			}
		}
	}

	/**
	 * Removes one ServerManager from the list of associated ServerManagers of this
	 * ForwardScheduler. The shutdown() method is called on that ServerManager and it is removed
	 * from the internal list. Active forwarded connections are not affected by this call. If this
	 * ForwardScheduler doesn't know any ServerManager with the specified ID, nothing is done.
	 *
	 * @param a_serverManagerId The ID of the ServerManager to close, see IServerManager.getId().
	 */
	public void removeServerManager(Object a_serverManagerId)
	{
		if (a_serverManagerId != null)
		{
			synchronized (m_serverManagers)
			{
				IServerManager serverManagerToClose = (IServerManager) (m_serverManagers.get(
					a_serverManagerId));
				if (serverManagerToClose != null)
				{
					serverManagerToClose.shutdown();
					m_serverManagers.remove(a_serverManagerId);
				}
			}
		}
	}

	/**
	 * Removes all ServerManagers from the list of associated ServerManagers of this
	 * ForwardScheduler. The shutdown() method is called on every ServerManager and all
	 * ServerManagers are removed from the internal list. There can't be opened new connections
	 * until at least one server manager is added to the list. Active forwarded connections are
	 * not affected by this call.
	 */
	public void removeAllServerManagers()
	{
		synchronized (m_serverManagers)
		{
			Enumeration serverManagers = m_serverManagers.elements();
			/* close all ServerManagers */
			while (serverManagers.hasMoreElements())
			{
				( (IServerManager) (serverManagers.nextElement())).shutdown();
			}
			/* remove all ServerManagers from the table */
			m_serverManagers.clear();
		}
	}

	/**
	 * Returns the statistics instance for this ForwardScheduler.
	 *
	 * @return The statistics for this ForwardScheduler.
	 */
	public ForwardSchedulerStatistics getStatistics()
	{
		return m_statistics;
	}

	/**
	 * Returns the number of currently forwarded connections.
	 *
	 * @return The number of currently forwarded connections.
	 */
	public int getCurrentlyForwardedConnections()
	{
		int forwardedConnections = 0;
		synchronized (m_connectionHandler)
		{
			/* get a consistent value */
			forwardedConnections = m_connectionHandler.size();
		}
		return forwardedConnections;
	}

	/**
	 * This is the implementation of the internal thread. It allocates the bandwidth for all
	 * forwarded connections periodically every CYCLE_TIME milliseconds.
	 */
	public void run()
	{
		while (m_shutdown == false)
		{
			synchronized (m_connectionHandler)
			{
				int openConnections = m_connectionHandler.size();
				int[] readyBytes = new int[openConnections];
				Vector connectionRanking = new Vector();
				/* sort the connections by the bytes they want to transfer */
				for (int i = 0; i < openConnections; i++)
				{
					readyBytes[i] = ( (ForwardConnection) (m_connectionHandler.elementAt(i))).
						getAvailableBytes();
					int insertPos = 0;
					for (int j = 0; j < i; j++)
					{
						if (readyBytes[j] < readyBytes[i])
						{
							insertPos++;
						}
					}
					connectionRanking.insertElementAt(new Integer(i), insertPos);
				}
				int bytesPerRound = (m_netBandwidth * (int) (CYCLE_TIME)) / 1000;
				/* now allocate the transfer capacity to the connections */
				if (openConnections > 0)
				{
					int bytesPerConnection = bytesPerRound / openConnections;
					for (int i = 0; i < openConnections; i++)
					{
						int currentConnection = ( (Integer) (connectionRanking.elementAt(i))).intValue();
						if ( (readyBytes[currentConnection] < bytesPerConnection) &&
							( (i + 1) != openConnections))
						{
							/* increase the transfer volume for the other connections, if this is not the last one */
							bytesPerConnection = bytesPerConnection +
								( (bytesPerConnection - readyBytes[currentConnection]) /
								 (openConnections - (i + 1)));
							/* connection can send all the bytes */
							( (ForwardConnection) (m_connectionHandler.elementAt(currentConnection))).
								allowTransfer(readyBytes[currentConnection]);
						}
						else
						{
							/* limit the transfer capacity for this connection */
							( (ForwardConnection) (m_connectionHandler.elementAt(currentConnection))).
								allowTransfer(bytesPerConnection);
						}
					}
				}
			}
			/* sleep until next round */
			long nextWakeUp = ( (System.currentTimeMillis() / CYCLE_TIME) + 1) * CYCLE_TIME;
			long currentTime = System.currentTimeMillis();
			/* we need this loop construction, because of the time granularity -> without this, sometimes
			 * the thread can wake up to early and would loop the whole thing two or more times
			 */
			while (currentTime < nextWakeUp)
			{
				try
				{
					Thread.sleep(nextWakeUp - currentTime);
				}
				catch (InterruptedException e)
				{
				}
				currentTime = System.currentTimeMillis();
			}
		}
	}

}
