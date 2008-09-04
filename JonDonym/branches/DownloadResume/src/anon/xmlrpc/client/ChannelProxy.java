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
import java.io.InputStream;
import java.io.OutputStream;

import anon.AnonChannel;

class ChannelProxy implements AnonChannel {
  private boolean m_bIsClosedByPeer = false;

  private boolean m_bIsClosed = false;

  protected int m_id;

  protected int m_type;

  protected AnonServiceImplProxy m_RemoteAnonService;

  private ChannelInputStreamProxy m_inputStream;

  private ChannelOutputStreamProxy m_outputStream;

  public ChannelProxy(int id, AnonServiceImplProxy remote) throws IOException {
    m_bIsClosedByPeer = false;
    m_bIsClosed = false;
    m_id = id;
    m_RemoteAnonService = remote;
    m_inputStream = new ChannelInputStreamProxy(this);
    m_outputStream = new ChannelOutputStreamProxy(this);
  }

  public void finalize() {
    close();
  }

  public int hashCode() {
    return m_id;
  }

  public InputStream getInputStream() {
    return m_inputStream;
  }

  public OutputStream getOutputStream() {
    return m_outputStream;
  }

  // called from ChannelOutputStream to send data to the AnonService which
  // belongs to this channel
  protected/* synchronized */
  void send(byte[] buff, int off, int len) throws IOException {
    m_RemoteAnonService.send(m_id, buff, off, len);
  }

  protected int recv(byte[] buff, int off, int len) throws IOException {
    return m_RemoteAnonService.recv(m_id, buff, off, len);
  }

  public int getOutputBlockSize() {
    // TODO: FixMe
    // return MuxSocket.PAYLOAD_SIZE-10*MuxSocket.KEY_SIZE;
    return (989 - 10 * 16);
  }

  public synchronized void close() {
    /*
     * try { if(!m_bIsClosed) { m_outputStream.close(); m_inputStream.close();
     * if(!m_bIsClosedByPeer) close_impl(); } } catch(Exception e) { }
     * m_bIsClosed=true;
     */
  }

  /*
   * protected void closedByPeer() { try { m_inputStream.closedByPeer();
   * m_outputStream.closedByPeer(); } catch(Exception e) { }
   * m_bIsClosedByPeer=true; }
   */

}
