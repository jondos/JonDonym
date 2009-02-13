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
package anon.infoservice.test;

import java.applet.Applet;
import java.io.IOException;
import java.net.InetAddress;
import java.net.URL;
import java.util.Hashtable;

import HTTPClient.HTTPConnection;
import HTTPClient.HTTPResponse;
import HTTPClient.HttpOutputStream;
import HTTPClient.ModuleException;
import HTTPClient.NVPair;
import HTTPClient.ProtocolNotSuppException;
import HTTPClient.URI;

/**
 * This class simulation n HTTPConnction but does virtually nothing more than
 * returning previously specified HTTPResponses. GET, HEAD and POST requests do
 * the same, OPTIONS and PUT requests are not supported.
 * @todo not really implemented, will be my next step
 * @author Wendolsky
 */
public class DummyHTTPConnection extends HTTPConnection
{
	private static Hashtable ms_httpResponses = new Hashtable();

	private int m_actualPort;
	private String m_actualHost;
	private InetAddress m_actualLocalAddress;
	private int m_actualLocalPort;
	private String m_actualProtocol;
	private int m_actualTimeout;
	private NVPair[] m_actualDefaultHeaders;
	private boolean m_bActualAllow;

	public DummyHTTPConnection(Applet p0) throws ProtocolNotSuppException
	{
		this(p0.getCodeBase().getHost(), p0.getCodeBase().getPort());
	}

	public DummyHTTPConnection(String a_host)
	{
		this(a_host, 80);
	}

	public DummyHTTPConnection(String a_host, int a_port)
	{
		super(a_host, a_port);
		m_actualHost = a_host;
		m_actualPort = a_port;
		m_actualProtocol = "http";
	}

	public DummyHTTPConnection(String a_protocol, String a_host, int a_port) throws ProtocolNotSuppException
	{
		this(a_host, a_port);
		m_actualProtocol = a_protocol;
	}

	public DummyHTTPConnection(String a_protocol, String a_host, int a_port,
							   InetAddress a_localAddress, int a_localPort) throws
		ProtocolNotSuppException
	{
		this(a_protocol, a_host, a_port);
		m_actualLocalAddress = a_localAddress;
		m_actualLocalPort = a_localPort;
	}

	public DummyHTTPConnection(URL a_url) throws ProtocolNotSuppException
	{
		this(a_url.getProtocol(), a_url.getHost(), a_url.getPort());

	}

	public DummyHTTPConnection(URI a_uri) throws ProtocolNotSuppException
	{
		this(a_uri.getHost(), a_uri.getPort());
		try
		{
			m_actualProtocol = a_uri.toURL().getProtocol();
		} catch (java.net.MalformedURLException a_e)
		{
		}
	}

	public static void addHTTPResponse(String a_file, HTTPResponse a_response)
	{
		ms_httpResponses.put(a_file, a_response);
	}

	public static void clearHTTPResponses()
	{
		ms_httpResponses.clear();
	}

	public HTTPResponse Head(String file) throws IOException, ModuleException
	{
		return null;
	}

	public HTTPResponse Head(String file, NVPair[] form_data) throws IOException, ModuleException
	{
		return null;
	}

	public HTTPResponse Head(String file, NVPair[] form_data, NVPair[] headers) throws IOException,
		ModuleException
	{
		return null;
	}

	public HTTPResponse Head(String file, String query) throws IOException, ModuleException
	{
		return null;
	}

	public HTTPResponse Head(String file, String query, NVPair[] headers) throws IOException, ModuleException
	{
		return null;
	}

	public HTTPResponse Get(String file) throws IOException, ModuleException
	{
		return null;
	}

	public HTTPResponse Get(String file, NVPair[] form_data) throws IOException, ModuleException
	{
		return null;
	}

	public HTTPResponse Get(String file, NVPair[] form_data, NVPair[] headers) throws IOException,
		ModuleException
	{
		return null;
	}

	public HTTPResponse Get(String file, String query) throws IOException, ModuleException
	{
		return null;
	}

	public HTTPResponse Get(String file, String query, NVPair[] headers) throws IOException, ModuleException
	{
		return null;
	}

	public HTTPResponse Post(String file) throws IOException, ModuleException
	{
		return null;
	}

	public HTTPResponse Post(String file, NVPair[] form_data) throws IOException, ModuleException
	{
		return null;
	}

	public HTTPResponse Post(String file, NVPair[] form_data, NVPair[] headers) throws IOException,
		ModuleException
	{
		return null;
	}

	public HTTPResponse Post(String file, String data) throws IOException, ModuleException
	{
		return null;
	}

	public HTTPResponse Post(String file, String data, NVPair[] headers) throws IOException, ModuleException
	{
		return null;
	}

	public HTTPResponse Post(String file, byte[] data) throws IOException, ModuleException
	{
		return null;
	}

	public HTTPResponse Post(String file, byte[] data, NVPair[] headers) throws IOException, ModuleException
	{
		return null;
	}

	public HTTPResponse Post(String file, HttpOutputStream stream) throws IOException, ModuleException
	{
		return null;
	}

	public HTTPResponse Post(String file, HttpOutputStream stream, NVPair[] headers) throws IOException,
		ModuleException
	{
		return null;
	}

	public HTTPResponse Put(String file, String data) throws IOException, ModuleException
	{
		return null;
	}

	public HTTPResponse Put(String file, String data, NVPair[] headers) throws IOException, ModuleException
	{
		return null;
	}

	public HTTPResponse Put(String file, byte[] data) throws IOException, ModuleException
	{
		return null;
	}

	public HTTPResponse Put(String file, byte[] data, NVPair[] headers) throws IOException, ModuleException
	{
		return null;
	}

	public HTTPResponse Put(String file, HttpOutputStream stream) throws IOException, ModuleException
	{
		return null;
	}

	public HTTPResponse Put(String file, HttpOutputStream stream, NVPair[] headers) throws IOException,
		ModuleException
	{
		return null;
	}

	public HTTPResponse Options(String file) throws IOException, ModuleException
	{
		return null;
	}

	public HTTPResponse Options(String file, NVPair[] headers) throws IOException, ModuleException
	{
		return null;
	}

	public HTTPResponse Options(String file, NVPair[] headers, byte[] data) throws IOException,
		ModuleException
	{
		return null;
	}

	public HTTPResponse Options(String file, NVPair[] headers, HttpOutputStream stream) throws IOException,
		ModuleException
	{
		return null;
	}

	public HTTPResponse Delete(String file) throws IOException, ModuleException
	{
		return null;
	}

	public HTTPResponse Delete(String file, NVPair[] headers) throws IOException, ModuleException
	{
		return null;
	}

	public HTTPResponse Trace(String file, NVPair[] headers) throws IOException, ModuleException
	{
		return null;
	}

	public HTTPResponse Trace(String file) throws IOException, ModuleException
	{
		return null;
	}

	public HTTPResponse ExtensionMethod(String method, String file, byte[] data, NVPair[] headers) throws
		IOException, ModuleException
	{
		return null;
	}

	public HTTPResponse ExtensionMethod(String method, String file, HttpOutputStream os, NVPair[] headers) throws
		IOException, ModuleException
	{
		return null;
	}

	public void stop()
	{}

	public void setDefaultHeaders(NVPair[] a_headers)
	{
		m_actualDefaultHeaders = a_headers;
	}

	public NVPair[] getDefaultHeaders()
	{
		return m_actualDefaultHeaders;
	}

	public String getProtocol()
	{
		return m_actualProtocol;
	}

	public String getHost()
	{
		return m_actualHost;
	}

	public int getPort()
	{
		return m_actualPort;
	}

	public String getProxyHost()
	{
		return null;
	}

	public int getProxyPort()
	{
		return 0;
	}

	public boolean isCompatibleWith(URI uri)
	{
		return false;
	}

	public void setTimeout(int a_time)
	{
		m_actualTimeout = a_time;
	}

	public int getTimeout()
	{
		return m_actualTimeout;
	}

	public void setAllowUserInteraction(boolean a_bAllow)
	{
		m_bActualAllow = a_bAllow;
	}

	public boolean getAllowUserInteraction()
	{
		return m_bActualAllow;
	}

	public Class[] getModules()
	{
		return null;
	}

	public boolean addModule(Class module, int pos)
	{
		return false;
	}

	public boolean removeModule(Class module)
	{
		return false;
	}

	public void setContext(Object context)
	{}

	public Object getContext()
	{
		return null;
	}

	public void addDigestAuthorization(String realm, String user, String passwd)
	{}

	public void addBasicAuthorization(String realm, String user, String passwd)
	{}

	public synchronized void setCurrentProxy(String host, int port)
	{}
}
