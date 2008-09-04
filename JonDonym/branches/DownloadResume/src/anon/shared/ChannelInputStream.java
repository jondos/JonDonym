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

import java.io.InputStream;
import java.io.IOException;

final class ChannelInputStream extends InputStream
{
	private IOQueue m_Queue = null;
	private boolean m_bIsClosed = false;

	ChannelInputStream(AbstractChannel c)
	{
		m_Queue = new IOQueue();
		m_bIsClosed = false;
	}

	protected void recv(byte[] buff, int pos, int len) throws IOException
	{
		try
		{
			m_Queue.write(buff, pos, len);
		}
		catch (IOException ioe)
		{
			throw ioe;
		}
		catch (Exception e)
		{
			throw new IOException(e.getMessage());
		}
	}

	public synchronized int available() throws IOException
	{
		return m_Queue.available();
	}

	public int read() throws IOException
	{
		return m_Queue.read();
	}

	public int read(byte[] out) throws IOException
	{
		return m_Queue.read(out, 0, out.length);
	}

	public int read(byte[] out, int pos, int len) throws IOException
	{
		return m_Queue.read(out, pos, len);
	}

	protected
		/*synchronized*/
		void closedByPeer()
	{
		try
		{
			m_Queue.closeWrite();
		}
		catch (Exception e)
		{}
	}

	public synchronized void close() throws IOException
	{
		if (!m_bIsClosed)
		{
			try
			{
				m_Queue.closeWrite();
			}
			catch (Exception e)
			{}
			m_Queue.closeRead();
			m_Queue = null;
			m_bIsClosed = true;
		}
	}
}
