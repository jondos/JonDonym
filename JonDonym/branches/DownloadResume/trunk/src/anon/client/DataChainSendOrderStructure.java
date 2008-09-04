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
public class DataChainSendOrderStructure {

  private byte[] m_orderData;
  
  private Object m_additionalProtocolData;
  
  private int m_processedBytes;
  
  private IOException m_thrownException;

  private byte[] m_channelCell;
    
  private Object m_internalSynchronization;
  
  private boolean m_processingDone;
  
  
  public DataChainSendOrderStructure(byte[] a_orderData) {
    m_orderData = a_orderData;
    m_processedBytes = 0;
    m_thrownException = null;
    m_channelCell = null;
    m_internalSynchronization = new Object();
    m_processingDone = false;
    m_additionalProtocolData = null;
  }
     
  
  public byte[] getOrderData() {
    return m_orderData;
  }
  
  public Object getAdditionalProtocolData() {
    return m_additionalProtocolData;
  }
  
  public void setAdditionalProtocolData(Object a_protocolData) {
    m_additionalProtocolData = a_protocolData;
  }
    
  public void processingDone() {
    synchronized (m_internalSynchronization) {
      m_processingDone = true;
      /* wake up waiting threads */
      m_internalSynchronization.notify();
    }
  }
  
  public boolean isProcessingDone() {
    return m_processingDone;
  }
  
  public Object getSynchronizationObject() {
    return m_internalSynchronization;
  }
  
  public void setThrownException(IOException a_thrownException) {
    m_thrownException = a_thrownException;
  }
  
  public IOException getThrownException() {
    return m_thrownException;
  }
  
  public void setProcessedBytes(int a_processedBytes) {
    m_processedBytes = a_processedBytes;
  }
  
  public int getProcessedBytes() {
    return m_processedBytes;
  }
  
  public void setChannelCell(byte[] a_channelCell) {
    m_channelCell = a_channelCell;
  }
  
  public byte[] getChannelCell() {
    return m_channelCell;
  }
  
}
