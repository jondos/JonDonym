/*
 * Copyright (c) 2006, The JAP-Team
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *   - Redistributions of source code must retain the above copyright notice,
 *     this list of conditions and the following disclaimer.
 *
 *   - Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer in the
 *     documentation and/or other materials provided with the distribution.
 *
 *   - Neither the name of the University of Technology Dresden, Germany nor
 *     the names of its contributors may be used to endorse or promote
 *     products derived from this software without specific prior written
 *     permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package anon.client;

import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Observable;

import anon.transport.connection.ConnectionException;
import anon.transport.connection.IStreamConnection;



/**
 * @author Stefan Lieske
 */
public class SocketHandler extends Observable {

  private IStreamConnection m_underlyingConnection;

  private SocketInputStreamImplementation m_socketInputStream;

  private SocketOutputStreamImplementation m_socketOutputStream;

  private Object m_internalSynchronization;


  private class SocketInputStreamImplementation extends InputStream {

    private InputStream m_underlyingStream;

    public SocketInputStreamImplementation(InputStream a_underlyingStream) {
      m_underlyingStream = a_underlyingStream;
    }

    public int read() throws IOException {
      int byteRead = -1;
      try {
        byteRead = m_underlyingStream.read();
      }
      catch (IOException e) {
        handleIOException(e);
        throw e;
      }
      if (byteRead == -1) {
        handleEndOfInputStream();
      }
      return byteRead;
    }

    public int read(byte[] a_buffer, int a_offset, int a_length) throws IOException {
      int bytesRead = -1;
      try {
        bytesRead = m_underlyingStream.read(a_buffer, a_offset, a_length);
      }
      catch (IOException e) {
        handleIOException(e);
        throw e;
      }
      if (bytesRead == -1) {
        handleEndOfInputStream();
      }
      return bytesRead;
    }

    public int available() throws IOException {
      int bytesAvailable = 0;
      try {
        bytesAvailable = m_underlyingStream.available();
      }
      catch (IOException e) {
        handleIOException(e);
        throw e;
      }
      return bytesAvailable;
    }

    public void close() {
      handleInputStreamClose();
    }
  }

  private class SocketOutputStreamImplementation extends OutputStream {

    private OutputStream m_underlyingStream;

    public SocketOutputStreamImplementation(OutputStream a_underlyingStream) {
      m_underlyingStream = a_underlyingStream;
    }

    public void write(int a_dataByte) throws IOException {
      try {
        m_underlyingStream.write(a_dataByte);
      }
      catch (IOException e) {
        handleIOException(e);
        throw e;
      }
    }

    public void write(byte[] a_buffer, int a_offset, int a_length) throws IOException {
      try {
        m_underlyingStream.write(a_buffer, a_offset, a_length);
      }
      catch (IOException e) {
        handleIOException(e);
        throw e;
      }
    }

    public void flush() throws IOException {
      try {
        m_underlyingStream.flush();
      }
      catch (IOException e) {
        handleIOException(e);
        throw e;
      }
    }

    public void close() {
      handleOutputStreamClose();
    }
  }


  public SocketHandler(IStreamConnection a_baseConnection) throws IOException {
    m_underlyingConnection = a_baseConnection;
    // keep in sync with old behavior
    if (m_underlyingConnection.getCurrentState() == IStreamConnection.ConnectionState_CLOSE)
    	throw new IOException("Connection allready closed");
    m_internalSynchronization = new Object();
    
    m_socketInputStream = new SocketInputStreamImplementation(m_underlyingConnection.getInputStream());
    m_socketOutputStream = new SocketOutputStreamImplementation(m_underlyingConnection.getOutputStream());
  }


  public void closeSocket() {
    try {
      m_underlyingConnection.close();
    }
    catch (IOException e) {
      /* no handling necessary */
    }
  }

  public InputStream getInputStream() {
    return m_socketInputStream;
  }

  public OutputStream getOutputStream() {
    return m_socketOutputStream;
  }


  private void handleIOException(IOException a_exception) {
    /* notify the observers */
    synchronized (m_internalSynchronization) {
      setChanged();
      notifyObservers(a_exception);
    }
  }

  private void handleEndOfInputStream() {
    /* unexpected end of input-stream */
    handleIOException(new IOException("SocketHandler: handleEndOfInputStream(): Unexpected end of input stream."));
  }

  private void handleInputStreamClose() {
    /* ignore it */
  }

  private void handleOutputStreamClose() {
    /* ignore it */
  }

}
