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

import anon.client.crypto.ASymCipher;
import anon.client.replay.ReplayTimestamp;


/** 
 * @author Stefan Lieske
 */
public class MixParameters {

  private String m_mixId;

  private ASymCipher m_mixCipher;

  private ReplayTimestamp m_replayTimestamp;
  
  private int m_replayOffset;
  
  private Object m_internalSynchronization;
  
  public static long m_referenceTime;
  
  
  public MixParameters(String a_mixId, ASymCipher a_mixCipher) {
    m_mixId = a_mixId;
    m_mixCipher = a_mixCipher;
    m_replayTimestamp = null;
    m_internalSynchronization = new Object();
    m_replayOffset = 0;
  }
 
  
  public String getMixId() {
    return m_mixId;
  }
  
  public ASymCipher getMixCipher() {
    return m_mixCipher;
  }
  
  public ReplayTimestamp getReplayTimestamp() {
    synchronized (m_internalSynchronization) {
      return m_replayTimestamp;
    }
  }
  
  public byte[] getReplayOffset() 
  {
      byte[] r_replayOffset = new byte[3];
      m_replayOffset=m_replayOffset&0xffffff;
      r_replayOffset[0] = (byte)(m_replayOffset >> 16);
      r_replayOffset[1] = (byte)((m_replayOffset >> 8)&0xff);
      r_replayOffset[2] = (byte)(m_replayOffset&0xff);
      return r_replayOffset;
  }

  public byte[] getCurrentReplayOffset(int diff) 
  {
	  if (m_replayOffset==0) return null;
	  byte[] r_replayOffset = new byte[3];
      int tmp_replayOffset=(m_replayOffset+diff)&0xffffff;
      r_replayOffset[0] = (byte)(tmp_replayOffset >> 16);
      r_replayOffset[1] = (byte)((tmp_replayOffset >> 8)&0xff);
      r_replayOffset[2] = (byte)(tmp_replayOffset&0xff);
      return r_replayOffset;
  }

  public void setReplayTimestamp(ReplayTimestamp a_replayTimestamp) {
    synchronized (m_internalSynchronization) {
      m_replayTimestamp = a_replayTimestamp;
    }
  }

  public void setReplayOffset(int a_replayTimestamp) 
  {
	  m_replayOffset=a_replayTimestamp&0xffffff;
  }

}
