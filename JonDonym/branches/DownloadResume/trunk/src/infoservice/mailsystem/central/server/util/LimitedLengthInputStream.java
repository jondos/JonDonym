/*
 Copyright (c) 2000 - 2005, The JAP-Team
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

import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.IOException;

/**
 * This class is the implementation for an InputStream where only a specified number of bytes can
 * be read from.
 */
public class LimitedLengthInputStream extends InputStream {
  
  /**
   * Stores the underlying stream from which the data are read.
   */  
  private InputStream m_underlyingStream;
  
  /**
   * Stores the maximum number of bytes which can still be read from the stream.
   */ 
  private long m_readLimit;

  
  /**
   * Creates an input stream with a limited length. After reading the specified number of bytes,
   * the end of this stream is reached (any read call will return -1).
   *
   * @param a_underlyingStream The stream to read the data from. If this is null, this constructor
   *                           will throw a NullPointerException.
   * @param a_maximumLength The maximum number of bytes to read from the underlying stream. This
   *                        value needs to be >= -1. A value of -1 disables the limit end enables
   *                        reading until the end of the underlying stream is reached.
   */
  public LimitedLengthInputStream(InputStream a_underlyingStream, long a_maximumLength) {
    if (a_underlyingStream == null) {
      throw (new NullPointerException("LimitedLengthInputStream: Constructor: The underlying stream must not be null!"));
    }
    m_underlyingStream = a_underlyingStream;
    m_readLimit = a_maximumLength;
  }
  
  /**
   * Reads one byte from the underlying InputStream. If the call is successful, the limit of bytes
   * able to read from the stream is also decremented by 1. If the byte limit is exhausted -1 is
   * returned and no byte is read from the underlying stream. In the case of a read exception,
   * that exception is thrown. 
   *
   * @return The byte read from the stream or -1, if the end of the underlying stream or the read
   *         limit is reached.
   *
   * @exception IOException If an I/O error occurs.
   */
  public int read() throws IOException {
    int returnByte = -1;
    if (m_readLimit == -1) {
      returnByte = m_underlyingStream.read();
    }
    else {
      synchronized (this) {
        if (m_readLimit > 0) {
          /* we can read some more bytes */
          returnByte = m_underlyingStream.read();
          if (returnByte != -1) {
            /* we have read one byte -> decrement the counter by 1 */
            m_readLimit--;
          }
        }
      }
    }
    return returnByte;
  }

  /**
   * Reads bytes into a portion of an array.  This method will block until some input is
   * available, an I/O error occurs or the end of the stream or the byte limit is reached. It's
   * always tried to fill the buffer completely with data from the underlying stream. If the end of
   * stream is detected or the byte limit is reached, this method returns the number of bytes
   * read until the end of the stream or the byte limit. If the end of stream is reached or the
   * byte limit is exhausted and no byte was read from the stream, -1 is returned (of course only,
   * if at least one byte should be read from the stream).
   *
   * @param a_buffer The destination buffer.
   *
   * @return The number of bytes read, or <code>-1</code> if the end of the stream was reached or
   *         the read limit is already exhausted and no byte could be read.
   *
   * @exception IOException If an I/O error occurs.
   */
  public int read(byte[] a_buffer) throws IOException {
    return read(a_buffer, 0, a_buffer.length);
  }
  
  /**
   * Reads bytes into a portion of an array.  This method will block until some input is
   * available, an I/O error occurs or the end of the stream or the byte limit is reached. It's
   * always tried to fill the buffer completely with data from the underlying stream. If the end of
   * stream is detected or the byte limit is reached, this method returns the number of bytes
   * read until the end of the stream or the byte limit. If the end of stream is reached or the
   * byte limit is exhausted and no byte was read from the stream, -1 is returned (of course only,
   * if at least one byte should be read from the stream).
   *
   * @param a_buffer The destination buffer.
   * @param a_offset The offset in the destination buffer at which to start storing bytes.
   * @param a_length The maximum number of bytes to read.
   *
   * @return The number of bytes read, or <code>-1</code> if the end of the stream was reached or
   *         the read limit is already exhausted and no byte could be read.
   *
   * @exception IOException If an I/O error occurs.
   */
  public int read(byte[] a_buffer, int a_offset, int a_length) throws IOException {
    int bytesRead = 0;
    if (m_readLimit == -1) {
      bytesRead = m_underlyingStream.read(a_buffer, a_offset, a_length);
    }
    else {
      synchronized (this) {
        if (m_readLimit > 0) {
          /* we can read some more bytes */
          int bytesToRead = a_length;
          if (((long)a_length) > m_readLimit) {
            /* casting to an int is no problem, because m_readLimit was smaller than another int
             * -> m_readLimit fits into an int
             */
            bytesToRead = (int)m_readLimit;
          }
          try {
            bytesRead = m_underlyingStream.read(a_buffer, a_offset, bytesToRead);
          }
          catch (InterruptedIOException e) {
            /* update the read limit before throwing the exception */
            m_readLimit = m_readLimit - (long)(e.bytesTransferred);
            throw (e);
          }
          if (bytesRead != -1) {
            /* we have read some bytes -> decrement the counter by the number of bytes read */
            m_readLimit = m_readLimit - (long)bytesRead;
          }
        }
        else {
          /* read limit is exhausted -> if we shall read more than 0 bytes, we have to return -1
           */
          if (a_length > 0) {
            bytesRead = -1;
          }
        }
      }
    }
    return bytesRead;
  }
  
  /**
   * Returns the number of bytes which can be read from this input stream without blocking. If a
   * read limit was specified, this number is never bigger than the number of remaining bytes from
   * the read limit. 
   *
   * @return The number of bytes which can be read from the stream without blocking or reaching
   *         the end of the read limit.
   *
   * @exception IOException If an I/O error occurs.
   */
  public int available() throws IOException {
    int availableBytes = 0;
    if (m_readLimit == -1) {
      availableBytes = m_underlyingStream.available();
    }
    else {
      synchronized (this) {
        if (m_readLimit > 0) {
          availableBytes = m_underlyingStream.available();
          if ((long)availableBytes > m_readLimit) {
            /* casting to an int is no problem, because m_readLimit was smaller than another int
             * -> m_readLimit fits into an int
             */
            availableBytes = (int)m_readLimit;
          }
        }
      }
    }
    return availableBytes;
  }
  
  /**
   * Closes the underlying stream. The read limit is set to 0 bytes.
   *
   * @exception IOException If an I/O error occurs.
   */  
  public void close() throws IOException {
    /* close the underlying stream and set the read limit to 0 */
    m_underlyingStream.close();
    synchronized (this) {
      m_readLimit = 0;
    }
  }
  
  /**
   * Returns the number of remaining bytes for the read limit. If no read limit was specified, -1
   * is returned.
   *
   * @return The number of remaining bytes for the read limit (after the stream is closed, always
   *         0 is returned).
   */
  public synchronized long getRemainingBytes() {
    return m_readLimit;
  }
 
}  