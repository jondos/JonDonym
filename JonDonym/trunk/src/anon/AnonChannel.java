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
package anon;

import java.io.InputStream;
import java.io.OutputStream;

/**
 * An AnonChannel could be used to send and receive data. There are different types of
 * channels and the transmitted data must match the (proxy-)protocol implied by the type
 * of the channel. A new channel is created via {@link AnonService#createChannel(int)  createChannel()}
 *
 * @version 1.0
 */
public interface AnonChannel
{
	/// Constant for the channel type: HTTP-Proxy. Such a channel could be used to transmit
	/// data which conforms to the HTTP-Porxy-Protocol
	public final static int HTTP = 0;

	/// Constant for the channel type: SOCKS-Proxy. Such a channel could be used to transmit
	/// data which conforms to the SOCKS-Proxy-Protocol
	public final static int SOCKS = 1;

	/// Constant for the channel type: SMTP-Proxy. Such a channel could be used to transmit
	/// data which conforms to the SMTP-Protocol
	public final static int SMTP = 2;

	/** The returned InputStream could be used to receive data.
	 *  @return InputStream, useful for receiving data
	 */
	public InputStream getInputStream();

	/** The returned OutputStream could be used to send data.
	 *  @return OutputStream, useful for sending data
	 */
	public OutputStream getOutputStream();

	/** Returns a value that indicates the current optimum size of data to write.. Because often
	 *  anon services transport the data splited into packets for optimum performance
	 *  it may be good to send data according to the packet size to avoid unneccessary overheads.
	 *
	 * @return the current optimum size for output data. If 1 is returned the size does not matter.
	 */
	public int getOutputBlockSize();

	/** Closes the channel and releases all resources used. */
	public void close();
	
	public boolean isClosed();
}
