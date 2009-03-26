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
package infoservice;

import anon.infoservice.StatusInfo;

/**
 * Holds the status statistics of a mixcascade.
 */
public class StatusStatisticsEntry {

  /**
   * This is the ID of the mixcascade to which this status statistics belongs.
   */
  private String m_mixCascadeId;

  /**
   * This is the minimum number of active users in the current statistics period.
   */
  private int m_minNrOfActiveUsers;

  /**
   * This is the maximum number of active users in the current statistics period.
   */
  private int m_maxNrOfActiveUsers;

  /**
   * This is the sum of active users in the current statistics period.
   */
  private int m_nrOfActiveUsersSum;

  /**
   * This is the number of mixed packets.
   */
  private long m_mixedPackets;
  
  /**
   * This is the number of updates of the statistics in this statistics period. We need it for
   * calculating the average number of users. So we count only updates, where the number of
   * active users is not -1.
   */
  private int m_userStatisticsUpdates;

  /**
   * This is the creation time of the last received statistics message -> only newer messages are
   * accepted for updating the statistics.
   */
  private long m_timeInLastStatisticsMessage;


  /**
   * This creates a new StatusStatisticsDBEntry. The values of the first status are added to the
   * statistics immediately.
   *
   * @param a_currentStatus The first status for the statistics.
   */
  public StatusStatisticsEntry(StatusInfo a_currentStatus) {
    m_mixCascadeId = a_currentStatus.getId();
    m_nrOfActiveUsersSum = a_currentStatus.getNrOfActiveUsers();
    m_minNrOfActiveUsers = a_currentStatus.getNrOfActiveUsers();
    m_maxNrOfActiveUsers = a_currentStatus.getNrOfActiveUsers();
    m_mixedPackets = a_currentStatus.getMixedPackets();
    m_userStatisticsUpdates = 1;
    m_timeInLastStatisticsMessage = a_currentStatus.getVersionNumber();
  }


  /**
   * This updates the statistics. The update is only be done, if the status message is a really
   * new one, which we don't know already.
   *
   * @param a_currentStatus A new status which is added to the statistics.
   */
  public void updateStatistics(StatusInfo a_currentStatus) {  
    if (a_currentStatus.getVersionNumber() > m_timeInLastStatisticsMessage) {
      /* we have received a new status message -> update the statistics */
      m_timeInLastStatisticsMessage = a_currentStatus.getVersionNumber();
      if (a_currentStatus.getNrOfActiveUsers() > -1) {
        /* update only, if we have a valid number of active users */
        if ((m_minNrOfActiveUsers == -1) || (a_currentStatus.getNrOfActiveUsers() < m_minNrOfActiveUsers)) {
          m_minNrOfActiveUsers = a_currentStatus.getNrOfActiveUsers();
        }
        if ((m_maxNrOfActiveUsers == -1) || (a_currentStatus.getNrOfActiveUsers() > m_maxNrOfActiveUsers)) {
          m_maxNrOfActiveUsers = a_currentStatus.getNrOfActiveUsers();
        }
        if (m_nrOfActiveUsersSum != -1) {
          m_nrOfActiveUsersSum = m_nrOfActiveUsersSum + a_currentStatus.getNrOfActiveUsers();
        }
        else {
          m_nrOfActiveUsersSum = a_currentStatus.getNrOfActiveUsers();
        }
        m_userStatisticsUpdates++;
      }
      m_mixedPackets = a_currentStatus.getMixedPackets();
    }
  }

  /**
   * Returns a unique ID for this StatusStatisticsDBEntry. It's the same as the ID of the
   * corresponding mixcascade.
   *
   * @return The ID of this StatusStatisticsDBEntry.
   */
  public String getId() {
    return m_mixCascadeId;
  }

  /**
   * Returns a String with all information needed for the write out of the statistics.
   *
   * @return A String for the statistics log.
   */
  public String getLogString() {
    return Integer.toString(m_minNrOfActiveUsers) + "," + Integer.toString(m_maxNrOfActiveUsers) + "," + Integer.toString(Math.round( ( (float) m_nrOfActiveUsersSum) / m_userStatisticsUpdates)) + "," + Long.toString(m_mixedPackets);
  }

  /**
   * Returns the filename of the corresponding log-file. The name is logStatusMIXCASCADEID.log.
   * MIXCASCADEID is replaced by the ID of the mixcascade this statistics belongs to. If there are
   * characters in MIXCASCADEID which are not a letter or a digit, they are replaced by "_".
   *
   * @return The filename (without path) of the log file.
   */
  public String getFileName() {
    char[] fileName = m_mixCascadeId.toCharArray();
    for (int i = 0; i < fileName.length; i++) {
      if (Character.isLetterOrDigit(fileName[i]) == false) {
        fileName[i] = '_';
      }
    }
    return ("logStatus" + (new String(fileName)) + ".log");
  }

}
