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
package anon.shared;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import anon.AnonChannel;

abstract public class AbstractChannel implements AnonChannel
{
	protected volatile boolean m_bIsClosedByPeer = false;
	protected volatile boolean m_bIsClosed = false;
	protected int m_id;

	private ChannelInputStream m_inputStream;
	private ChannelOutputStream m_outputStream;

	public AbstractChannel(int id)
	{
		this();
		m_id = id;
	}

	public AbstractChannel()
	{
		m_bIsClosedByPeer = false;
		m_bIsClosed = false;
		m_inputStream = new ChannelInputStream(this);
		m_outputStream = new ChannelOutputStream(this);
	}

	public void finalize()
	{
		close();
	}

	public int hashCode()
	{
		return m_id;
	}

	public InputStream getInputStream()
	{
		return m_inputStream;
	}

	public OutputStream getOutputStream()
	{
		return m_outputStream;
	}

	public boolean isClosed()
	{
		return m_bIsClosed;
	}
	
	public synchronized void close()
	{
		try
		{
			if (!m_bIsClosed)
			{
				m_outputStream.close();
				m_inputStream.close();
				if (!m_bIsClosedByPeer)
				{
					close_impl();
				}
			}
		}
		catch (Exception e)
		{
		}
		m_bIsClosed = true;
	}

	//Use m_id
	abstract protected void close_impl();

	//called from the AnonService to send data to this channel
	protected void recv(byte[] buff, int pos, int len) throws IOException
	{
		m_inputStream.recv(buff, pos, len);
	}

	//called from ChannelOutputStream to send data to the AnonService which belongs to this channel
	abstract protected void send(byte[] buff, int len) throws IOException;

	public void closedByPeer()
	{
		try
		{
			m_inputStream.closedByPeer();
			m_outputStream.closedByPeer();
		}
		catch (Exception e)
		{
		}
		m_bIsClosedByPeer = true;
	}

}
