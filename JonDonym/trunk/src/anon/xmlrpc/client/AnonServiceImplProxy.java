/*
 Copyright (c) 2000, The JAP-Team
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
package anon.xmlrpc.client;

import java.io.IOException;
import java.net.ConnectException;
import java.util.Vector;

import org.apache.xmlrpc.XmlRpcClientLite;

import anon.AnonChannel;
import anon.AnonServerDescription;
import anon.AnonService;
import anon.AnonServiceEventListener;
import anon.ErrorCodes;
import anon.IServiceContainer;
import anon.infoservice.IMutableProxyInterface;
import anon.terms.TermsAndConditionConfirmation;
public class AnonServiceImplProxy implements AnonService
{
	String m_RpcServerHost;
	int m_RpcServerPort;
	int m_ClientID;

	public AnonServiceImplProxy(String addr, int port) throws Exception
	{
		m_RpcServerHost = addr;
		m_RpcServerPort = port;
		Object o = doRemote("registerClient", new Vector());
		m_ClientID = ( (Integer) o).intValue();
	}

	public int initialize(AnonServerDescription mixCascade, IServiceContainer a_serviceContainer,
			TermsAndConditionConfirmation termsConfirmation)
	{
		return 0;
	}

	public boolean isConnected()
	{
		return true;
	}

	public int setProxy(IMutableProxyInterface a_Proxy)
	{
		return ErrorCodes.E_UNKNOWN;
	}

	public void shutdown(boolean a_bResetTransferredBytes)
	{
	}

	public AnonChannel createChannel(int type) throws ConnectException
	{
		try
		{
			Vector v = new Vector(1);
			v.addElement(new Integer(m_ClientID));
			Object o = doRemote("createChannel", v);
			return new ChannelProxy( ( (Integer) o).intValue(), this);
		}
		catch (Exception e)
		{
			throw new ConnectException("Could not connect");
		}
	}

	public AnonChannel createChannel(String host, int port) throws ConnectException
	{
		return null;
	}

	public void addEventListener(AnonServiceEventListener l)
	{
	}

	public void removeEventListener(AnonServiceEventListener l)
	{
	}

	public void removeEventListeners()
	{
	}


//Implementation
	private Object doRemote(String method, Vector params) throws IOException
	{
		try
		{
			XmlRpcClientLite xmlrpc = new XmlRpcClientLite("http://localhost:8889/RPC2"); //m_RpcServerHost.getHostAddress(),m_RpcServerPort);
			return xmlrpc.execute("ANONXMLRPC." + method, params);
		}
		catch (Exception e)
		{
			throw new IOException("Error processing XML-RCP: " + method);
		}
	}

	protected void send(int channelid, byte[] buff, int off, int len) throws IOException
	{
		Vector v = new Vector();
		v.addElement(new Integer(m_ClientID));
		v.addElement(new Integer(channelid));
		byte[] tmpBuff = new byte[len];
		System.arraycopy(buff, off, tmpBuff, 0, len);
		v.addElement(tmpBuff);
		Object o = doRemote("channelOutputStreamWrite", v);
	}

	protected int recv(int channelid, byte[] buff, int off, int len) throws IOException
	{
		Vector v = new Vector();
		v.addElement(new Integer(m_ClientID));
		v.addElement(new Integer(channelid));
		v.addElement(new Integer(len));
		Object o = doRemote("channelInputStreamRead", v);
		if (o instanceof byte[])
		{
			len = ( (byte[]) o).length;
			System.arraycopy(o, 0, buff, off, len);
			return len;
		}
		return -1;
	}
}
