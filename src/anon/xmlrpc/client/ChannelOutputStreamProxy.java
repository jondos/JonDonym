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
import java.io.OutputStream;

class ChannelOutputStreamProxy extends OutputStream
{
	private ChannelProxy m_channel;
	private boolean m_bIsClosed = false;
	public ChannelOutputStreamProxy(ChannelProxy proxychannel)
	{
		m_channel = proxychannel;
		m_bIsClosed = false;
	}

	public void write(byte[] buff) throws IOException
	{
		write(buff, 0, buff.length);
	}

	public void write(byte[] buff, int off, int len) throws IOException
	{
		if ( /*m_bIsClosedByPeer||*/m_bIsClosed)
		{
			throw new IOException("Channel closed by peer");
		}
		m_channel.send(buff, off, len);
	}

	public void write(int i) throws IOException
	{
		if ( /*m_bIsClosedByPeer||*/m_bIsClosed)
		{
			throw new IOException("Channel closed by peer");
		}
		byte[] buff = new byte[1];
		buff[0] = (byte) i;
		m_channel.send(buff, 0, 1);
	}

}