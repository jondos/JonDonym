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

import anon.AnonChannel;
import anon.TooMuchDataForPacketException;


/** 
 * @author Stefan Lieske
 */
public class TypeFilterDataChain implements AnonChannel {

  private AnonChannel m_originChannel;
  
  private Object m_internalSynchronization;
  
  private boolean m_firstPacket;
  
  private OutputStream m_typeFilterOutputStream;
  
  
  private class TypeFilterOutputStreamImplementation extends OutputStream {
    
    private int m_dataChainType;
    
    private OutputStream m_originOutputStream;
    
    
    public TypeFilterOutputStreamImplementation(OutputStream a_originOutputStream, int a_dataChainType) {
      m_originOutputStream = a_originOutputStream;
      m_dataChainType = a_dataChainType;
    }
    
    
    public void write(int a_byte) throws IOException {
      byte[] byteAsArray = {(byte)a_byte};
      write(byteAsArray);   
    }
    
    public void write(byte[] a_buffer, int a_offset, int a_length) throws IOException {
      synchronized(m_internalSynchronization) {
        byte[] typedPacket = a_buffer;
        if (m_firstPacket) {
          /* add the type-byte to the stream */
          typedPacket = new byte[a_length + 1];
          typedPacket[0] = (byte)m_dataChainType;
          System.arraycopy(a_buffer, a_offset, typedPacket, 1, a_length);
          a_offset = 0;
          a_length = typedPacket.length;
        }
        try {
          m_originOutputStream.write(typedPacket, a_offset, a_length);
          /* no error while sending packet -> type has been sent, if it was the first
           * packet
           */
          m_firstPacket = false;
        }
        catch (TooMuchDataForPacketException e) {
          if (m_firstPacket) {
            if (e.getBytesSent() > 0) {
              /* at least the type has been sent */
              m_firstPacket = false;
            }
            /* modify the exception because we have added one byte to the origin
             * message
             */
            throw (new TooMuchDataForPacketException(Math.max(0, e.getBytesSent() - 1)));
          }
          else {
            /* it was not the first packet -> no modification on the packet was done
             * -> throw the exception unmodified
             */
            throw (e);
          }
        }
      }
    }
    
    public void flush() throws IOException {
      m_originOutputStream.flush();
    }
    
    public void close() throws IOException {
      m_originOutputStream.close();      
    }   
  }
  
  
  public TypeFilterDataChain(AnonChannel a_originChannel, int a_dataChainType) {
    m_originChannel = a_originChannel;
    m_firstPacket = true;
    m_internalSynchronization = new Object();
    m_typeFilterOutputStream = new TypeFilterOutputStreamImplementation(a_originChannel.getOutputStream(), a_dataChainType);
  }
  
  public InputStream getInputStream() {
    return m_originChannel.getInputStream();
  }

  public OutputStream getOutputStream() {
    return m_typeFilterOutputStream;
  }

  public int getOutputBlockSize() {
    int outputBlockSize = m_originChannel.getOutputBlockSize();
    synchronized (m_internalSynchronization) {
      if ((m_firstPacket) && (outputBlockSize > 0)) {
        /* we will add one byte -> make blocksize one byte smaller */
        outputBlockSize = outputBlockSize - 1;
      }
    }
    return outputBlockSize;
  }

  public void close() {
    m_originChannel.close();
  }

  public boolean isClosed()
  {
	  return m_originChannel.isClosed();
  }
}
