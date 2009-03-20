/*
 Copyright (c) 2000 - 2005 The JAP-Team
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
package infoservice.mailsystem.central.server.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketException;

/**
 * This class enables usage of alternative input and output streams for a socket.
 */
public class SocketContainer {
  
  /**
   * Stores the origin socket.
   */
  private Socket m_socket;
  
  /**
   * Stores an alternative input stream. So read requests can be detoured to this stream. If this
   * value is null, the origin input stream of the socket is used.
   */
  private InputStream m_alternativeInputStream;
  
  /**
   * Stores an alternative output stream. So write requests can be detoured to this stream. If
   * this value is null, the origin output stream of the socket is used.
   */
  private OutputStream m_alternativeOutputStream;
  
  
  /**
   * Creates a new instance of SocketContainer.
   *
   * @param a_socket The underlying socket to use.
   * @param a_alternativeInputStream Specifies an alternative InputStream the SocketContainer
   *                                 shall use. If null is specified, it will use the InputStream
   *                                 of the socket itself.
   * @param a_alternativeOutputStream Specifies an alternative OutputStream the SocketContainer
   *                                  shall use. If null is specified, it will use the
   *                                  OutputStream of the socket itself.
   */
  public SocketContainer(Socket a_socket, InputStream a_alternativeInputStream, OutputStream a_alternativeOutputStream) {
    m_alternativeInputStream = a_alternativeInputStream;
    m_alternativeOutputStream = a_alternativeOutputStream;
    if (a_socket != null) {
      m_socket = a_socket;
    }
    else {
      throw (new NullPointerException("SocketContainer: Constructor: Socket must not be null."));
    }
  }
    
  
  /**
   * Returns the InputStream to use. If an alternative InputStream was specified in the
   * constructor, that stream is returned. If no alternative InputStream was specified, the origin
   * stream of the socket is used.
   *
   * @return The InputStream to use for reading data.
   */
  public InputStream getInputStream() throws IOException {
    InputStream inputStream = null;
    if (m_alternativeInputStream != null) {
      inputStream = m_alternativeInputStream;
    }
    else {
      inputStream = m_socket.getInputStream();
    }
    return inputStream;
  }

  /**
   * Returns the OutputStream to use. If an alternative OutputStream was specified in the
   * constructor, that stream is returned. If no alternative OutputStream was specified, the
   * origin stream of the socket is used.
   *
   * @return The OutputStream to use for writing data.
   */
  public OutputStream getOutputStream() throws IOException {
    OutputStream outputStream = null;
    if (m_alternativeOutputStream != null) {
      outputStream = m_alternativeOutputStream;
    }
    else {
      outputStream = m_socket.getOutputStream();
    }
    return outputStream;
  }
  
  
  /**
   * Closes the socket, see Socket.close().
   */
  public void close() throws IOException {
    m_socket.close();
  }

  /**
   * Changes the timeout for the underlying socket. See Socket.setSoTimeout().
   *
   * @param a_timeout The timeout for the socket in ms. If 0 is specified, no timeout is used.
   */
  public void setSocketTimeout(int a_timeout) throws SocketException {
    m_socket.setSoTimeout(a_timeout);
  } 
  
  /**
   * Creates a child of this SocketContainer. The child uses the same socket and as default also
   * the same input and output streams as this container (unless something else is specified).
   *
   * @param a_alternativeInputStream Specifies an alternative InputStream the child shall use. If
   *                                 null is specified, it will use the InputStream returned by
   *                                 getInputStream().
   * @param a_alternativeOutputStream Specifies an alternative OutputStream the child shall use.
   *                                  If null is specified, it will use the OutputStream returned
   *                                  by getOutputStream().
   *
   * @return The SocketContainer using the same underlying socket and the specified input and
   *         output streams.
   */
  public SocketContainer createChildContainer(InputStream a_alternativeInputStream, OutputStream a_alternativeOutputStream) throws IOException {
    InputStream inputStream = null;
    if (a_alternativeInputStream == null) {
      inputStream = getInputStream();
    }
    else {
      inputStream = a_alternativeInputStream;
    }
    OutputStream outputStream = null;
    if (a_alternativeOutputStream == null) {
      outputStream = getOutputStream();
    }
    else {
      outputStream = a_alternativeOutputStream;
    }
    return (new SocketContainer(m_socket, inputStream, outputStream));
  }
  
}