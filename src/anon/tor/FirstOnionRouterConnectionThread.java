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

package anon.tor;

import java.io.IOException;
import java.net.SocketException;

import anon.infoservice.ImmutableProxyInterface;
import anon.crypto.tinytls.TinyTLS;

public class FirstOnionRouterConnectionThread implements Runnable
{

	private TinyTLS m_tls;
	private Thread m_thread = null;
	private String m_name;
	private int m_port;
	private long m_timeout;
	private Exception m_exception;
	private Object m_oNotifySync = new Object();
	private ImmutableProxyInterface m_proxyInterface;

	/**
	 * Constructor
	 * @param name
	 * host
	 * @param port
	 * port
	 * @param timeout
	 * timeout
	 * @param a_ProxyInterface
	 * a proxy interface
	 */
	public FirstOnionRouterConnectionThread(String name, int port, long timeout,
											ImmutableProxyInterface a_ProxyInterface)
	{
		m_name = name;
		m_port = port;
		m_timeout = timeout;
		m_exception = null;
		m_proxyInterface = a_ProxyInterface;
	}

	/**
	 * gets a TinyTLS connection if no timeout is reached
	 * @return
	 * on succes : a tinytls connection
	 * on timeout : null
	 * @throws IOException
	 */
	public TinyTLS getConnection() throws IOException
	{
		m_thread = new Thread(this, "FirstOnionRouterConnectionThread");
		m_thread.setDaemon(true);
		m_thread.start();
		synchronized (m_oNotifySync)
		{
			try
			{
				m_oNotifySync.wait(m_timeout);
			}
			catch (InterruptedException ex)
			{
			}
		}
		//if an error occured throw the exception
		if (m_exception != null)
		{
			throw new IOException(m_exception.getMessage());
		}
		if (m_tls == null)
		{
			throw new SocketException("Connection timed out");
		}
		return m_tls;
	}

	public void run()
	{
		TinyTLS tls = null;
		try
		{
			tls = new TinyTLS(m_name, m_port, m_proxyInterface);
		}
		catch (Exception ex)
		{
			m_exception = ex;
		}
		m_tls = tls;
		synchronized (m_oNotifySync)
		{
			m_oNotifySync.notify();
		}
	}

}
