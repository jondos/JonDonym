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
package infoservice.mailsystem.central.server;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;

/**
 * This class is a generic server implementation. It waits for incoming connections and forwards
 * them to the specified connection handler.
 */
public class GenericServer implements Runnable
{

	/**
	 * Stores the local address the server will listen on. If this value is null, the server will
	 * listen on all local addresses.
	 */
	private InetAddress m_serverAddress;

	/**
	 * Stores the local port the server will listen on.
	 */
	private int m_serverPort;

	/**
	 * Stores the ServerSocket used for listening. If this value is null, the server is currently
	 * not listening for new requests.
	 */
	private ServerSocket m_serverSocket;

	/**
	 * Stores the thread which handles all requests reaching the server socket. If the server isn't
	 * running, this value is null.
	 */
	private Thread m_serverSocketThread;

	/**
	 * This object is used for internal synchronization with the server socket handling thread.
	 */
	private Object m_synchronizer;

	/**
	 * Stores the factory which creates the server implementations for handling the accepted
	 * connections from the clients. If this value is null, new connections are not handled and
	 * are closed immediately.
	 */
	private IServerImplementationFactory m_serverImplementationFactory;

	/**
	 * Creates a new instance of GenericServer. Only some initialization is done here, startServer()
	 * has to be called in order to start listening for requests.
	 *
	 * @param a_serverPort The port the server will listen on.
	 * @param a_serverAddress The local address the server will be bind on. If this value is null,
	 *                        the server will accept requests on all local addresses.
	 */
	public GenericServer(int a_serverPort, InetAddress a_serverAddress)
	{
		m_serverPort = a_serverPort;
		m_serverAddress = a_serverAddress;
		m_synchronizer = new Object();
		m_serverImplementationFactory = null;
		m_serverSocketThread = null;
		m_serverSocket = null;
	}

	/**
	 * Starts the GenericServer. The server is bound to the port and address specified in the
	 * constructor and will listen for requests. If the server is already listening, nothing
	 * happens. If there occurs an exception while creating the ServerSocket for the server, the
	 * server isn't started and that exception is thrown.
	 */
	public void startServer() throws IOException
	{
		synchronized (this)
		{
			if (m_serverSocket == null)
			{
				m_serverSocket = new ServerSocket(m_serverPort, 50, m_serverAddress);
				try
				{
					m_serverSocket.setSoTimeout(0);
				}
				catch (IOException e)
				{
					/* it's necessary to have an infinite timeout on the ServerSocket -> reset everything
					 * and throw the exception
					 */
					m_serverSocket = null;
					throw (e);
				}
				if (m_serverAddress != null)
				{
					LogHolder.log(LogLevel.DEBUG, LogType.NET,
								  "GenericServer: startServer: Server is listening at port " +
								  Integer.toString(m_serverSocket.getLocalPort()) + " on interface " +
								  m_serverAddress.toString() + ".");
				}
				else
				{
					LogHolder.log(LogLevel.DEBUG, LogType.NET,
								  "GenericServer: startServer: Server is listening at port " +
								  Integer.toString(m_serverSocket.getLocalPort()) +
								  " on all local interfaces.");
				}
				m_serverSocketThread = new Thread(this, "GernericServer");
				m_serverSocketThread.setDaemon(true);
				m_serverSocketThread.start();
			}
		}
	}

	/**
	 * Stops listening for new requests. If the server isn't running, nothing happens. Attention:
	 * Already handled connections are not terminated by a call of this method.
	 */
	public void stopServer()
	{
		synchronized (this)
		{
			if (m_serverSocket != null)
			{
				try
				{
					m_serverSocket.close();
				}
				catch (Exception e)
				{
					/* should not happen */
				}
				try
				{
					m_serverSocketThread.join();
				}
				catch (Exception e)
				{
					/* should not happen */
				}
				m_serverSocket = null;
				m_serverSocketThread = null;
			}
		}
	}

	/**
	 * Changes the factory used for creating the server implementations for the accepted connections
	 * from the clients. The factory specified here is used immediately with the next accepted
	 * connection (but already handled connections are not influenced).
	 *
	 * @param a_serverImplementationFactory The new server implementation factory. If this value is
	 *                                      null, any new arriving connection is not handled and is
	 *                                      closed immediately.
	 */
	public void setConnectionHandlerFactory(IServerImplementationFactory a_serverImplementationFactory)
	{
		synchronized (m_synchronizer)
		{
			m_serverImplementationFactory = a_serverImplementationFactory;
		}
	}

	/**
	 * This is the implementation of the ServerSocket management thread. It waits for new
	 * connections from the clients and forwards them to the connection handlers.
	 */
	public void run()
	{
		boolean shutdown = false;
		LogHolder.log(LogLevel.DEBUG, LogType.NET,
					  "GenericServer: run: Starting the server-socket management thread.");
		while (shutdown == false)
		{
			Socket newConnection = null;
			try
			{
				newConnection = m_serverSocket.accept();
			}
			catch (Exception e)
			{
				/* we got an exception while waiting for new clients -> serious error or socket was closed
				 * -> stop the thread
				 */
				shutdown = true;
			}
			if (shutdown == false)
			{
				/* we got a new connection -> handle it */
				LogHolder.log(LogLevel.DEBUG, LogType.NET,
							  "GenericServer: run: New connection accepted. Request handling is started...");
				ConnectionHandle connectionHandle = new ConnectionHandle(newConnection,
					getServerImplementationFactory());
			}
		}
		LogHolder.log(LogLevel.DEBUG, LogType.NET,
					  "GenericServer: run: Server-socket management thread finished.");
	}

	/**
	 * Returns the current server implementation factory. It is used to create the server
	 * implementation for every new accpeted connection from the clients.
	 *
	 * @return The current server implementation factory. If this value is null, new connections are
	 *         are not handled and are closed immediately.
	 */
	private IServerImplementationFactory getServerImplementationFactory()
	{
		IServerImplementationFactory currentFactory = null;
		synchronized (m_synchronizer)
		{
			currentFactory = m_serverImplementationFactory;
		}
		return currentFactory;
	}

}
