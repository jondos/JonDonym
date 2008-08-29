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
package anon.shared;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketException;

import anon.transport.address.IAddress;
import anon.transport.address.TcpIpAddress;
import anon.transport.connection.ConnectionException;
import anon.transport.connection.IStreamConnection;

final public class ProxyConnection implements IStreamConnection
	{

		private Socket m_ioSocket;
		private InputStream m_In;
		private OutputStream m_Out;
		private int m_State;

		public ProxyConnection(Socket a_connectedSocket) throws Exception
			{
				m_ioSocket = a_connectedSocket;
				m_State = IStreamConnection.ConnectionState_OPEN;
				try
					{
						m_ioSocket.setSoTimeout(0);
					}
				catch (Exception e)
					{
						/* do nothing */
					}
				try
					{
						m_In = m_ioSocket.getInputStream();
						m_Out = m_ioSocket.getOutputStream();
					}
				catch (Exception e)
					{
						/* close everything */
						close();
						throw (e);
					}
			}

		public Socket getSocket()
			{
				return m_ioSocket;
			}

		public InputStream getInputStream()
			{
				return m_In;
			}

		public OutputStream getOutputStream()
			{
				return m_Out;
			}

		public void setSoTimeout(int ms) throws SocketException
			{
				m_ioSocket.setSoTimeout(ms);
			}

		public void close()
			{
				try
					{
						m_In.close();
					}
				catch (Exception e)
					{
					}
				try
					{
						m_Out.close();
					}
				catch (Exception e)
					{
					}
				try
					{
						m_ioSocket.close();
					}
				catch (Exception e)
					{
					}
				m_State = IStreamConnection.ConnectionState_CLOSE;
			}

		public int getCurrentState()
			{
				return m_State;
			}

		public int getTimeout() throws ConnectionException
			{

				try
					{
						return m_ioSocket.getSoTimeout();
					}
				catch (SocketException e)
					{
						throw new ConnectionException(e);
					}
			}

		public void setTimeout(int value) throws ConnectionException
			{
				try
					{
						m_ioSocket.setSoTimeout(value);
					}
				catch (SocketException e)
					{
						throw new ConnectionException(e);
					}
			}

		public IAddress getLocalAddress()
			{
				return new TcpIpAddress(m_ioSocket.getLocalAddress(), m_ioSocket
						.getLocalPort());
			}

		public IAddress getRemoteAddress()
			{
				return new TcpIpAddress(m_ioSocket.getInetAddress(), m_ioSocket
						.getPort());
			}

	}
