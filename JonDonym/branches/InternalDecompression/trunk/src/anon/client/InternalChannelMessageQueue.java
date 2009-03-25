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

import java.util.Observable;
import java.util.Vector;


/** 
 * @author Stefan Lieske
 */
public class InternalChannelMessageQueue extends Observable {

  private Vector m_messageQueue;
    
  
  public InternalChannelMessageQueue() {
    m_messageQueue = new Vector();
  }
  
  
  public void addChannelMessage(InternalChannelMessage a_message) {
    synchronized (m_messageQueue) {
      m_messageQueue.addElement(a_message);
      m_messageQueue.notify();
    }
    setChanged();
    notifyObservers(a_message);
  }
  
  public InternalChannelMessage getFirstMessage() {
    InternalChannelMessage nextMessage = null;
    synchronized (m_messageQueue) {
      if (m_messageQueue.size() > 0) {
        nextMessage = (InternalChannelMessage)(m_messageQueue.firstElement());
      }
    }
    return nextMessage;
  }
  
  public void removeFirstMessage() {
    synchronized (m_messageQueue) {
      if (m_messageQueue.size() > 0) {
        m_messageQueue.removeElementAt(0);
      }
    }
  }
  
  public InternalChannelMessage waitForNextMessage() throws InterruptedException {
    InternalChannelMessage returnedMessage;
    synchronized (m_messageQueue) {
      while (m_messageQueue.size() == 0) {
        m_messageQueue.wait();
      }
      returnedMessage = (InternalChannelMessage)(m_messageQueue.firstElement());
    }
    return returnedMessage;
  }
  
}
