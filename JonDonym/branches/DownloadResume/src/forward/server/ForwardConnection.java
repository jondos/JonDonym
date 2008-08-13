/*
 Copyright (c) 2000 - 2004, The JAP-Team
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

/**
 * This is the implementation of the forwarding component between client and the protocol
 * handler. The bandwidth limit is implemented here.
 */
public class ForwardConnection
{

	/**
	 * Stores the connection to the client.
	 */
	private Socket m_clientConnection;

	/**
	 * Stores the protocol handler, which processes the data or forwards them to the mixcascade.
	 */
	private IProtocolHandler m_serverConnection;

	/**
	 * Stores the scheduler, which controls this ForwardConnection.
	 */
	private ForwardScheduler m_parentScheduler;

	/**
	 * Stores whether the ForwardConnection shall be closed. This indicates the internal threads,
	 * that they should come to the end.
	 */
	private boolean m_closeConnection;

	/**
	 * Stores the amount of data which can be transfered on the client -> server direction this
	 * round.
	 */
	private int m_transferFromClient;

	/**
	 * Stores the amount of data which can be transfered on the server -> client direction this
	 * round.
	 */
	private int m_transferFromServer;

	/**
	 * Stores the instance of the data transfer thread for the client -> server direction.
	 */
	private Thread m_clientReadThread;

	/**
	 * Stores the instance of the data transfer thread for the server -> client direction.
	 */
	private Thread m_serverReadThread;

	/**
	 * Stores the instance of the thread, which checks for a timeout on the connection.
	 */
	private Thread m_timeoutThread;

	/**
	 * Creates a new ForwardConnection. Also the internal threads for transfering the data and
	 * checking the timeout are created and started. Everything runs until this connection is
	 * closed by the scheduler or an error on the connection or while doing the protocol occurs.
	 * Then the closeConnection() method is called automatically. If there is an error while
	 * starting the protocol (DefaultProtocolHandler at the moment), no thread is started and an
	 * Exception is thrown. The client connection keeps open.
	 *
	 * @param a_clientConnection The Socket with the active connection to the client.
	 * @param a_parentScheduler The scheduler which controls this connection.
	 */
	public ForwardConnection(Socket a_clientConnection, ForwardScheduler a_parentScheduler) throws Exception
	{
		m_clientConnection = a_clientConnection;
		/* it's important to set the parent scheduler before creating the ProtocolHandler, because
		 * the ProtocolHandler needs this info
		 */
		m_parentScheduler = a_parentScheduler;
		/* if the following throws an exception, doesn't matter, because the thread will not be
		 * started, and the parent scheduler will clean up the rest
		 */
		m_serverConnection = new DefaultProtocolHandler(this);

		/* do some initialization */
		m_transferFromServer = 0;
		m_transferFromClient = 0;
		m_closeConnection = false;

		/* create the thread for the client -> server transfer direction */
		m_clientReadThread = new Thread(new Runnable()
		{
			public void run()
			{
				/* this is the transfer thread for the client->server direction */
				while (m_closeConnection == false)
				{
					byte[] bufferClient = null;
					synchronized (m_clientReadThread)
					{
						if (m_transferFromClient > 0)
						{
							bufferClient = new byte[m_transferFromClient];
							try
							{
								int availableData = m_clientConnection.getInputStream().available();
								if (bufferClient.length > availableData)
								{
									/* should not happen, but so it's save */
									bufferClient = new byte[availableData];
								}
							}
							catch (Exception e)
							{
								closeConnection();
							}
							/* reset the transfer volume */
							m_transferFromClient = 0;
						}
						else
						{
							/* read at least always one byte -> if no bytes available this method blocks ->
							 * we get -1, if the connection is closed by the client or a timeout, if there is
							 * no traffic on the connection within a whole timeout interval
							 */
							bufferClient = new byte[1];
						}
					}
					try
					{
						int readBytes = m_clientConnection.getInputStream().read(bufferClient);
						if (readBytes == -1)
						{
							/* end of InputStream reached -> client has closed the connection -> close also */
							closeConnection();
						}
						else
						{
							if (readBytes < bufferClient.length)
							{
								/* should not happen, but so it's save */
								byte[] tempBuffer = new byte[readBytes];
								System.arraycopy(bufferClient, 0, tempBuffer, 0, readBytes);
								bufferClient = tempBuffer;
							}
							if (readBytes > 0)
							{
								/* we read something from the client connection -> reset timeout */
								try
								{
									m_timeoutThread.interrupt();
								}
								catch (Exception e)
								{
								}
								/* add the transfered bytes to the statistics of the parent scheduler */
								m_parentScheduler.getStatistics().incrementTransferVolume(readBytes);
								/* write the bytes to the protocol handler, which process them or forwards them
								 * to the mixcascade
								 */
								m_serverConnection.write(bufferClient);
							}
						}
					}
					catch (Exception e)
					{
						/* there was an error -> close the connection */
						closeConnection();
					}
					synchronized (m_clientReadThread)
					{
						if (m_closeConnection == false)
						{
							try
							{
								/* wait until the next round */
								m_clientReadThread.wait();
							}
							catch (Exception e)
							{
							}
						}
					}
				}
			}
		});
		m_clientReadThread.setDaemon(true);

		/* create the thread for the server -> client transfer direction */
		m_serverReadThread = new Thread(new Runnable()
		{
			public void run()
			{
				/* this is the transfer thread for the server->client direction */
				while (m_closeConnection == false)
				{
					byte[] bufferServer = null;
					synchronized (m_serverReadThread)
					{
						/* don't make this thread also blocking like the client read thread, this would
						 * be dangerous when switching from protocol mode to server mode, because it could
						 * result in a indefinite read call on the protocol-send-buffer
						 */
						bufferServer = new byte[m_transferFromServer];
						try
						{
							int availableData = m_serverConnection.available();
							if (bufferServer.length > availableData)
							{
								/* should not happen, but so it's save */
								bufferServer = new byte[availableData];
							}
						}
						catch (Exception e)
						{
							closeConnection();
						}
						/* reset the transfer volume */
						m_transferFromServer = 0;
					}
					try
					{
						int readBytes = m_serverConnection.read(bufferServer);
						if (readBytes == -1)
						{
							/* end of InputStream reached -> server has closed the connection -> close also */
							closeConnection();
						}
						else
						{
							if (readBytes < bufferServer.length)
							{
								/* should not happen, but so it's save */
								byte[] tempBuffer = new byte[readBytes];
								System.arraycopy(bufferServer, 0, tempBuffer, 0, readBytes);
								bufferServer = tempBuffer;
							}
							if (readBytes > 0)
							{
								/* we will make some traffic on the client connection -> reset timeout */
								try
								{
									m_timeoutThread.interrupt();
								}
								catch (Exception e)
								{
								}
								/* add the transfered bytes to the statistics of the parent scheduler */
								m_parentScheduler.getStatistics().incrementTransferVolume(readBytes);
								/* write the bytes to the client connection */
								m_clientConnection.getOutputStream().write(bufferServer);
							}
						}
					}
					catch (Exception e)
					{
						/* there was an error -> close the connection */
						closeConnection();
					}
					synchronized (m_serverReadThread)
					{
						if (m_closeConnection == false)
						{
							try
							{
								/* wait until the next round */
								m_serverReadThread.wait();
							}
							catch (Exception e)
							{
							}
						}
					}
				}
			}
		});
		m_serverReadThread.setDaemon(true);

		/* create the thread, which checks for the timeout */
		m_timeoutThread = new Thread(new Runnable()
		{
			/* this is the client connection timeout thread */
			public void run()
			{
				while (m_closeConnection == false)
				{
					try
					{
						Thread.sleep( (long) ForwardServerManager.CLIENT_CONNECTION_TIMEOUT);
						/* if we reach the timeout without interruption, close the connection */
						closeConnection();
					}
					catch (Exception e)
					{
						/* if we got an interruption within the timeout, everything is ok */
					}
				}
			}
		});
		m_timeoutThread.setDaemon(true);

		/* start all threads */
		m_clientReadThread.start();
		m_serverReadThread.start();
		m_timeoutThread.start();
	}

	/**
	 * Returns the sum of available bytes (bytes which we have already received, but waiting in the
	 * incoming buffers for the connection) for the client -> server direction and the server ->
	 * client direction.
	 *
	 * @return The sum of available bytes (both directions) for this connection.
	 */
	public int getAvailableBytes()
	{
		int availableBytes = 0;
		try
		{
			availableBytes = m_clientConnection.getInputStream().available() + m_serverConnection.available();
		}
		catch (Exception e)
		{
			/* there was an error -> close the connection */
			closeConnection();
		}
		return availableBytes;
	}

	/**
	 * Returns the scheduler for this connection.
	 *
	 * @return The scheduler for this connection.
	 */
	public ForwardScheduler getParentScheduler()
	{
		return m_parentScheduler;
	}

	/**
	 * Closes the connection. The call is asynchronous, which means that it not waits until the
	 * end of the internal transfer threads. That is no problem because the internal threads will
	 * end immediately, but the wait for the end of the threads could result in a deadlock (if
	 * the thread himself has called closeConnection() -> thread waits for the end of
	 * closeConnection(), closeConnection() wait on the end of the thread). The
	 * connection is also removed from the connection list of the parent scheduler.
	 */
	public void closeConnection()
	{
		boolean alreadyClosed = false;
		synchronized (m_serverReadThread)
		{
			synchronized (m_clientReadThread)
			{
				/* synchronize with both threads -> avoiding deadlocks */
				alreadyClosed = m_closeConnection;
				m_closeConnection = true;
			}
		}
		if (alreadyClosed == false)
		{
			/* only do anything, if this is the first close call */
			try
			{
				m_clientConnection.close();
				if (m_serverConnection != null)
				{
					m_serverConnection.close();
				}
			}
			catch (Exception e)
			{
			}
			m_parentScheduler.removeConnection(this);
			/* wake up all threads -> they will end immediately */
			synchronized (m_clientReadThread)
			{
				m_clientReadThread.notify();
			}
			synchronized (m_serverReadThread)
			{
				m_serverReadThread.notify();
			}
			try
			{
				m_timeoutThread.interrupt();
			}
			catch (Exception e)
			{
			}
		}
	}

	/**
	 * This method is called by the scheduler to submit, how many bytes can be transfered on this
	 * connection in the next round. This method splits the assigned bytes into the transfer
	 * capacities for both directions (client->server, server->client) and notifies the transfer
	 * threads.
	 *
	 * @param a_transferBytes The bytes which can be transfered on this connection in the next
	 *                        round.
	 */
	public void allowTransfer(int a_transferBytes)
	{
		int transferFromServer = 0;
		int transferFromClient = 0;
		if (a_transferBytes > 0)
		{
			try
			{
				int availableBytesClient = m_clientConnection.getInputStream().available();
				int availableBytesServer = m_serverConnection.available();
				/* split the transfer capacity in the two directions (client -> server and
				 * server -> client)
				 */
				if (availableBytesClient > availableBytesServer)
				{
					/* there are more bytes waiting on the client side -> check whether the server side
					 * needs less than the half of the capacity
					 */
					if (availableBytesServer > (a_transferBytes / 2))
					{
						transferFromServer = a_transferBytes / 2;
					}
					else
					{
						transferFromServer = availableBytesServer;
					}
					if (availableBytesClient > a_transferBytes - transferFromServer)
					{
						/* take every byte we can get */
						transferFromClient = a_transferBytes - transferFromServer;
					}
					else
					{
						/* we don't need the full capacity */
						transferFromClient = availableBytesClient;
					}
				}
				else
				{
					/* there are more bytes waiting on the server side -> check whether the client side
					 * needs less than the half of the capacity
					 */
					if (availableBytesClient > (a_transferBytes / 2))
					{
						transferFromClient = a_transferBytes / 2;
					}
					else
					{
						transferFromClient = availableBytesClient;
					}
					if (availableBytesServer > a_transferBytes - transferFromClient)
					{
						/* take every byte we can get */
						transferFromServer = a_transferBytes - transferFromClient;
					}
					else
					{
						/* we don't need the full capacity */
						transferFromServer = availableBytesServer;
					}
				}
			}
			catch (Exception e)
			{
				/* there was an error -> close the connection */
				closeConnection();
			}
		}
		synchronized (m_clientReadThread)
		{
			/* notify the client thread */
			m_transferFromClient = transferFromClient;
			m_clientReadThread.notify();
		}
		synchronized (m_serverReadThread)
		{
			/* notify the server thread */
			m_transferFromServer = transferFromServer;
			m_serverReadThread.notify();
		}
	}

	/**
	 * Returns a string representation of this connection It's the IP and the port of the client.
	 *
	 * @return IP:port of the client.
	 */
	public String toString()
	{
		return (m_clientConnection.getInetAddress().getHostAddress() + ":" +
				Integer.toString(m_clientConnection.getPort()));
	}

}
