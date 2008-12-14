/*
 Copyright (c) 2000, The JAP-TeamAll rights reserved.
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
package anon.xmlrpc.server;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Vector;

import org.apache.xmlrpc.XmlRpcHandler;
import org.apache.xmlrpc.WebServer;

import anon.AnonChannel;
import anon.AnonService;
import anon.shared.AbstractChannel;

public class AnonServiceImplRemote implements XmlRpcHandler
{
	private AnonService m_AnonService;
	private WebServer m_RpcServer;
	private ClientList m_ClientList;
	public AnonServiceImplRemote(AnonService anonService)
	{
		m_AnonService = anonService;
		m_ClientList = new ClientList();
	}

	public int startService()
	{
		try
		{
			m_RpcServer = new WebServer(8889);
			m_RpcServer.addHandler("ANONXMLRPC", this);
			m_RpcServer.start();
			return 0;
		}
		catch (Exception e)
		{
			return -1;
		}
	}

	public int stopService()
	{
		return 0;
	}

	public Object execute(String method, Vector params) throws Exception
	{
		if (method.equals("registerClient"))
		{
			return doRegisterClient(params);
		}
		else if (method.equals("createChannel"))
		{
			return doCreateChannel(params);
		}
		else if (method.equals("channelInputStreamRead"))
		{
			return doChannelInputStreamRead(params);
		}
		else if (method.equals("channelOutputStreamWrite"))
		{
			return doChannelOutputStreamWrite(params);
		}
		throw new Exception("Unknown Method");
	}

	private Object doRegisterClient(Vector params) throws Exception
	{
		int id = m_ClientList.addNewClient();
		return new Integer(id);
	}

	private Object doCreateChannel(Vector params) throws Exception
	{
		Integer i = (Integer) params.elementAt(0);
		ClientEntry c = m_ClientList.getClient(i);
		AnonChannel channel;
		channel = m_AnonService.createChannel(AnonChannel.HTTP);
		c.addChannel(channel);
		return new Integer( ( (AbstractChannel) channel).hashCode());
	}

	private Object doChannelInputStreamRead(Vector params) throws Exception
	{
		Integer i = (Integer) params.elementAt(0);
		ClientEntry c = m_ClientList.getClient(i);
		AnonChannel channel = c.getChannel( (Integer) params.elementAt(1));
		InputStream in = channel.getInputStream();
		int len = ( (Integer) params.elementAt(2)).intValue();
		byte[] buff = new byte[len];
		int retlen = in.read(buff);
		if (retlen < 0)
		{
			return new Integer( -1);
		}
		byte[] outbuff = new byte[retlen];
		System.arraycopy(buff, 0, outbuff, 0, retlen);
		return outbuff;
	}

	private Object doChannelOutputStreamWrite(Vector params) throws Exception
	{
		Integer i = (Integer) params.elementAt(0);
		ClientEntry c = m_ClientList.getClient(i);
		AnonChannel channel = c.getChannel( (Integer) params.elementAt(1));
		OutputStream out = channel.getOutputStream();
		byte[] buff = (byte[]) params.elementAt(2);
		out.write(buff);
		out.flush();
		return new Integer(0);
	}

}
