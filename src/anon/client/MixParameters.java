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
  
  private Object m_internalSynchronization;
  
  
  public MixParameters(String a_mixId, ASymCipher a_mixCipher) {
    m_mixId = a_mixId;
    m_mixCipher = a_mixCipher;
    m_replayTimestamp = null;
    m_internalSynchronization = new Object();
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
  
  public void setReplayTimestamp(ReplayTimestamp a_replayTimestamp) {
    synchronized (m_internalSynchronization) {
      m_replayTimestamp = a_replayTimestamp;
    }
  }
  
}
