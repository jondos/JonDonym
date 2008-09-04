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
package forward.server;

/**
 * This interface describes the methods needed from ForwardConnection. Implementations of this
 * interface must check the incoming stream for messages, react on this messages or forward them
 * to the server.
 */
public interface IProtocolHandler {
  
  /**
   * Returns the number of bytes which are ready to read without blocking by the next call
   * of read(). This call throws an exception, if there is something wrong with the connection
   * to the server.
   *
   * @return The number of bytes which can be read.
   */
  public int available() throws Exception;
  
  /**
   * Read a_buffer.length bytes from the server in the buffer a_buffer. This call blocks until
   * a_buffer.length bytes could be read. This call throws an exception, if there is something
   * wrong with the connection to the server.
   *
   * @param a_buffer A buffer for the read bytes.
   *
   * @return The bytes really read in the buffer or -1, if the end of the stream is reached.
   */
  public int read(byte[] a_buffer) throws Exception;
  
  /**
   * Writes the bytes in a_buffer to the server or the protocol handler. This call blocks until
   * the bytes could be written in the send queue. This call throws an exception, if there is
   * something wrong with the connection to the server.
   *
   * @param a_buffer A buffer with the bytes to write.
   */
  public void write(byte[] a_buffer) throws Exception;
  
  /**
   * Closes the connection to the server and stops handling of protocol messages. All later calls
   * of available(), read(), write() will throw an exception.
   */
  public void close();
  
}