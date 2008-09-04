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
package forward;

import java.net.Socket;

import anon.infoservice.HTTPConnectionFactory;
import anon.infoservice.ImmutableProxyInterface;
import anon.infoservice.ListenerInterface;
import anon.shared.ProxyConnection;
import anon.transport.address.Endpoint;
import anon.transport.address.IAddress;
import anon.transport.address.SkypeAddress;
import anon.transport.address.TcpIpAddress;
import anon.transport.connection.ConnectionException;
import anon.transport.connection.IStreamConnection;
import anon.transport.connector.IConnector;
import anon.transport.connector.SkypeConnector;
import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;

/**
 * This is the implementation of some helper methods for the forwarding client
 * and server. This class is a singleton.
 */
public class ForwardUtils
	{

		/**
		 * Stores the instance of ForwardUtils (Singleton).
		 */
		private static ForwardUtils ms_fuInstance = null;

		/**
		 * The current proxy interface.
		 */
		ImmutableProxyInterface m_proxyInterface;

		/**
		 * Returns the instance of ForwardUtils (Singleton). If there is no
		 * instance, there is a new one created.
		 * 
		 * @return The ForwardUtils instance.
		 */
		public static ForwardUtils getInstance()
			{
				if (ms_fuInstance == null)
					{
						ms_fuInstance = new ForwardUtils();
					}
				return ms_fuInstance;
			}

		/**
		 * This creates a new instance of ForwardUtils with disabled proxy settings.
		 */
		private ForwardUtils()
			{
			}

		/**
		 * This changes the proxy settings for all new forwarding connections.
		 * Currently active forwarding connections are not concerned.
		 * 
		 * @param a_proxyInterface
		 *          the proxy interface
		 */
		public synchronized void setProxySettings(
				ImmutableProxyInterface a_proxyInterface)
			{
				m_proxyInterface = a_proxyInterface;
			}

		/**
		 * Creates a new connection to the specified target using the current
		 * firewall settings.
		 * 
		 * @param a_host
		 *          The hostname or IP address of the target.
		 * @param a_port
		 *          The port number of the connection target.
		 * 
		 * @return The new connection or null, if there was an error connecting to
		 *         that target.
		 */
		public ProxyConnection createProxyConnection(String a_host, int a_port)
			{
				ProxyConnection proxyConnection = null;
				try
					{
						synchronized (this)
							{
								/* get consistent proxy server data */
								proxyConnection = new ProxyConnection(
										HTTPConnectionFactory.getInstance()
												.createHTTPConnection(
														new ListenerInterface(a_host, a_port),
														m_proxyInterface).Connect());
							}
					}
				catch (Exception e)
					{
						LogHolder.log(LogLevel.ERR, LogType.NET, e);
					}
				return proxyConnection;
			}

		/**
		 * Creates a ForwardConnection according to the Type of the given Address.
		 * Unified the creating of Connection to different Forwarder.
		 */
		public IStreamConnection createForwardingConnection(IAddress a_address)
			{
				if (a_address instanceof TcpIpAddress)
					{
						TcpIpAddress spezialisedAddress = (TcpIpAddress) a_address;
						return createProxyConnection(spezialisedAddress.getHostname(),
								spezialisedAddress.getPort());
					}
				else if (a_address instanceof SkypeAddress)
					{
						LogHolder.log(LogLevel.DEBUG, LogType.NET,"forwardUtils:createconnection() start connection to skype forwarder");						
						SkypeConnector connector = new SkypeConnector();
						if(connector!=null)
							LogHolder.log(LogLevel.DEBUG, LogType.NET,"forwardUtils:createconnection() skype conector object created");						
						else
							LogHolder.log(LogLevel.DEBUG, LogType.NET,"forwardUtils:createconnection() skype conector object NOT created");						
						try
							{
								return connector.connect((SkypeAddress) a_address);
							}
						catch (ConnectionException e)
							{
								LogHolder.log(LogLevel.ERR, LogType.TRANSPORT,
										"Unable to create Skype Forwarding Connection. Cause: "
												+ e.getMessage());
							}
					}
				else if (a_address instanceof LocalAddress)
					{
						try
							{
								return (IStreamConnection) LocalForwarder.getConnector()
										.connect(a_address);
							}
						catch (ConnectionException e)
							{
								LogHolder.log(LogLevel.ERR, LogType.TRANSPORT,
										"unable to contact local forwarder. " + e.getMessage());
							}
					}
				return null;
			}

		/**
		 * Creates a new connection to the specified target using the current
		 * firewall settings.
		 * 
		 * @param a_host
		 *          The hostname or IP address of the target.
		 * @param a_port
		 *          The port number of the connection target.
		 * 
		 * @return The new connection or null, if there was an error connecting to
		 *         that target.
		 */
		public Socket createConnection(String a_host, int a_port)
			{
				ProxyConnection proxyConnection = createProxyConnection(a_host, a_port);
				Socket newSocket = null;
				if (proxyConnection != null)
					{
						newSocket = proxyConnection.getSocket();
					}
				return newSocket;
			}

	}
