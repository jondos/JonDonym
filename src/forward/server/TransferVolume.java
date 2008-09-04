/*
 Copyright (c) 2000 - 2004, The JAP-Team
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
package forward.server;

/**
 * This class stores an amount of transfered bytes and when this transfer was done. So we can use
 * the information for calculating bandwidth usage for a specified time interval.
 */
public class TransferVolume {
  
  /**
   * Stores the number of transfered bytes.
   */
  private int m_transferedBytes;
  
  /**
   * Stores the timestamp, when the transfer was done.
   */
  private long m_timeStamp;
  
  /**
   * Creates a new instance of TransferVolume. The amoun of transfered bytes is set to the given
   * volume. The timestamp is set to the current system time.
   *
   * @param a_volume The amount of transfered bytes.
   */
  public TransferVolume(int a_volume) {
    m_transferedBytes = a_volume;
    m_timeStamp = System.currentTimeMillis();
  }
  
  /**
   * Returns the number of transfered bytes.
   *
   * @return The number of transfered bytes.
   */
  public int getVolume() {
    return m_transferedBytes;
  }
  
  /**
   * Returns the timestamp, when this TransferVolume was created (the transfer was done).
   *
   * @return The time the transfer was finished.
   */
  public long getTimeStamp() {
    return m_timeStamp;
  }
  
}