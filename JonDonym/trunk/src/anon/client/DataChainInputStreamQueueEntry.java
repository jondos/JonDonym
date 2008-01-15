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

import java.io.IOException;


/** 
 * @author Stefan Lieske
 */
public class DataChainInputStreamQueueEntry {

  public static final int TYPE_DATA_AVAILABLE = 1;
  
  public static final int TYPE_STREAM_END = 2;
  
  public static final int TYPE_IO_EXCEPTION = 3;
  
  
  private int m_type;
  
  private byte[] m_data;
  
  private int m_alreadyReadBytes;
  
  private IOException m_ioException;
  
  
  public DataChainInputStreamQueueEntry(int a_type, byte[] a_data) {
    m_type = a_type;
    m_data = a_data;
    m_alreadyReadBytes = 0;
    m_ioException = null;
  }

  public DataChainInputStreamQueueEntry(IOException a_exception) {
    m_type = TYPE_IO_EXCEPTION;
    m_data = null;
    m_alreadyReadBytes = 0;
    m_ioException = a_exception;
  }

  
  public int getType() {
    return m_type;
  }
  
  public byte[] getData() {
    return m_data;
  }

  public int getAlreadyReadBytes() {
    return m_alreadyReadBytes;
  }
  
  public void setAlreadyReadBytes(int a_alreadyReadBytes) {
    m_alreadyReadBytes = a_alreadyReadBytes;
  }
  
  public IOException getIOException() {
    return m_ioException;
  }
  
}
