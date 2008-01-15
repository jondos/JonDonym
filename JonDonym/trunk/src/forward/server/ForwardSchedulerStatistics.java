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

import java.util.Enumeration;
import java.util.Vector;

/**
 * This is the implementation for the statistics of a ForwardScheduler.
 */
public class ForwardSchedulerStatistics {

  /**
   * The backwards interval in milliseconds over which the current bandwidth usage statistics are
   * created.
   */
  private static final long BANDWIDTH_STATISTICS_INTERVAL = 1000;

  /**
   * Stores the number of rejected connections.
   */
  private int m_rejectedConnections;

  /**
   * Stores the number of accepted connections.
   */
  private int m_acceptedConnections;

  /**
   * Stores the number of transfered bytes.
   */
  private long m_transferedBytes;

  /**
   * Stores the list with the last transfered byte amounts. See the constant in this class to
   * see how long every amount is in that list to create the bandwidth usage statistics.
   */
  private Vector m_lastTransferVolumes;


  /**
   * Creates a new ForwardSchedulerStatistics instance.
   */
  public ForwardSchedulerStatistics() {
    m_rejectedConnections = 0;
    m_acceptedConnections = 0;
    m_transferedBytes = 0;
    m_lastTransferVolumes = new Vector();
  }


  /**
   * Increments the number of rejected connections by one.
   */
  public void incrementRejectedConnections() {
    synchronized (this) {
      m_rejectedConnections++;
    }
  }

  /**
   * Returns the number of rejected connections.
   *
   * @return The number of rejected connections.
   */
  public synchronized int getRejectedConnections() {
    int rejectedConnections = 0;
	rejectedConnections = m_rejectedConnections;
    return rejectedConnections;
  }

  /**
   * Increments the number of accepted connections by one.
   */
  public void incrementAcceptedConnections() {
    synchronized (this) {
      m_acceptedConnections++;
    }
  }

  /**
   * Returns the number of accepted connections.
   *
   * @return The number of accepted connections.
   */
  public int getAcceptedConnections() {
    int acceptedConnections = 0;
    synchronized (this) {
      acceptedConnections = m_acceptedConnections;
    }
    return acceptedConnections;
  }

  /**
   * Increments the number of transfered bytes and adds the transfer volume to the list of
   * volumes for calculating the current bandwidth usage. Also outdated values from that list
   * are removed.
   *
   * @param a_volume A number of currently transfered bytes.
   */
  public void incrementTransferVolume(int a_volume) {
    synchronized (m_lastTransferVolumes) {
      /* we need exclusive access to the transfer volumes -> remove the old ones to save space
       * and later add the current transfer volume
       */
      removeOutdatedTransferVolumes();
      m_lastTransferVolumes.addElement(new TransferVolume(a_volume));
    }
    synchronized (this) {
      /* add the volume to the total number of transfered bytes */
      m_transferedBytes = m_transferedBytes + (long)a_volume;
    }
  }

  /**
   * Returns the current bandwidth usage. It is calculated from the list of currently transfered
   * volumes. See the constant in this class to get the time how long backwards the list stores
   * every transfer volume. First all outdated volumes are removed from the list and the rest is
   * used for calculating the current bandwidth usage.
   *
   * @return The current bandwidth usage in bytes/sec.
   */
  public int getCurrentBandwidthUsage() {
    int transferedBytes = 0;
    synchronized (m_lastTransferVolumes) {
      /* we need exclusive access to the transfer volumes -> remove the old ones and calculate
       * the current bandwidth usage with the rest
       */
      removeOutdatedTransferVolumes();
      Enumeration transferVolumes = m_lastTransferVolumes.elements();
      while (transferVolumes.hasMoreElements()) {
        transferedBytes = transferedBytes + ((TransferVolume)(transferVolumes.nextElement())).getVolume();
      }
    }
    return Math.round(((float)transferedBytes) * ((float)1000) / ((float)BANDWIDTH_STATISTICS_INTERVAL));
  }

  /**
   * Returns the total number of transfered bytes.
   *
   * @return The total number of transfered bytes.
   */
  public long getTransferedBytes() {
    long transferedBytes = 0;
    synchronized (this) {
      transferedBytes = m_transferedBytes;
    }
    return transferedBytes;
  }


  /**
   * Removes all outdated values from the list of currently transfered volumes.
   */
  private void removeOutdatedTransferVolumes() {
    synchronized (m_lastTransferVolumes) {
      /* we need exclusive access to the list of transfer volumes */
      long currentTimeStamp = System.currentTimeMillis();
      boolean moreOldEntries = true;
      while ((m_lastTransferVolumes.size() > 0) && (moreOldEntries == true)) {
        if (((TransferVolume)(m_lastTransferVolumes.firstElement())).getTimeStamp() + BANDWIDTH_STATISTICS_INTERVAL < currentTimeStamp) {
          /* the value is outdated -> remove it */
          m_lastTransferVolumes.removeElementAt(0);
        }
        else {
          /* value not outdated -> every value in the list behind is also not outdated -> stop */
          moreOldEntries = false;
        }
      }
    }
  }

}
