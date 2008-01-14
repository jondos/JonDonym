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
package anon.client.replay;

import java.util.Enumeration;
import java.util.Observable;
import java.util.Observer;
import java.util.Vector;

import anon.client.MixParameters;
import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;


/** 
 * @author Stefan Lieske
 */
public class TimestampUpdater implements Observer {
  
  private MixParameters[] m_mixParameters;
  
  private boolean m_responseReceived;
  
  private Object m_internalSynchronization;
  
  
  public TimestampUpdater(MixParameters[] a_mixParameters, ReplayControlChannel a_controlChannel) throws Exception {
    m_mixParameters = a_mixParameters;
    m_responseReceived = false;
    m_internalSynchronization = new Object();
    synchronized (m_internalSynchronization) {
      a_controlChannel.getMessageDistributor().addObserver(this);
      a_controlChannel.requestTimestamps();
      while (!m_responseReceived) {
        m_internalSynchronization.wait();
      }
    }
    for (int i = 0; i < m_mixParameters.length; i++) {
      if (m_mixParameters[i].getReplayTimestamp() == null) {
        throw (new Exception("TimestampUpdater: Constructor: Timestamp of Mix '" + m_mixParameters[i].getMixId() + "' is missing." ));
      }
    }   
  }
  
  
  public void update(Observable a_object, Object a_argument) {
    if (a_argument instanceof Vector) {
      /* we've received some timestamps */
      LogHolder.log(LogLevel.DEBUG, LogType.NET, "TimestampUpdater: update(): Received some timestamps.");
      Enumeration timestamps = ((Vector)a_argument).elements();
      Vector updatedMixes = new Vector();
      while (timestamps.hasMoreElements()) {
        ReplayTimestamp currentTimestamp = (ReplayTimestamp)(timestamps.nextElement());
        int i = 0;
        boolean mixFound = false;
        while ((i < m_mixParameters.length) && (!mixFound)) {
          if (m_mixParameters[i].getMixId().equals(currentTimestamp.getMixId())) {
            m_mixParameters[i].setReplayTimestamp(currentTimestamp);
            mixFound = true;
            if (updatedMixes.contains(new Integer(i))) {
              LogHolder.log(LogLevel.INFO, LogType.NET, "TimestampUpdater: update(): Received timestamp for Mix '" + currentTimestamp.getMixId() + "' twice.");              
            }
            else {
              updatedMixes.addElement(new Integer(i));
            }
          }
          i++;
        }
        if (!mixFound) {
          LogHolder.log(LogLevel.INFO, LogType.NET, "TimestampUpdater: update(): Received timestamp of Mix '" + currentTimestamp.getMixId() + "' is not necessary for the current cascade.");
        }
      }
      for (int i = 0; i < m_mixParameters.length; i++) {
        if (!updatedMixes.contains(new Integer(i))) {
          LogHolder.log(LogLevel.ERR, LogType.NET, "TimestampUpdater: update(): Timestamp of Mix '" + m_mixParameters[i].getMixId() + "' is missing.");
        }
      }
      synchronized (m_internalSynchronization) {
        /* wake up waiting threads */
        m_responseReceived = true;
        m_internalSynchronization.notifyAll();
      }
    }
    else if (a_argument instanceof Exception) {
      LogHolder.log(LogLevel.ERR, LogType.NET, "TimestampUpdater: update(): Received exception: " + a_argument.toString());
      synchronized (m_internalSynchronization) {
        /* wake up waiting threads */
        m_responseReceived = true;
        m_internalSynchronization.notifyAll();
      }
    }
  }
  
}
