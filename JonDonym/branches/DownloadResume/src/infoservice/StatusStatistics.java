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

import java.io.FileWriter;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import anon.infoservice.StatusInfo;
import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;

/**
 * This class is the implementation for the status statistics. It is used to get some statistics
 * for every mixcascade. This class is a singleton.
 */
public class StatusStatistics implements Runnable {

  /**
   * Stores the instance of StatusStatistics (Singleton).
   */
  private static StatusStatistics ms_statusStatisticsInstance = null;


  /**
   * Stores the statistics.
   */
  private Hashtable m_statisticsDatabase;


  /**
   * Returns the instance of StatusStatistics (Singleton). If there is no instance, there is a
   * new one created. Also the included thread is been started (only if isStatusStatisticsEnabled()
   * in Configuration.java returns true).
   *
   * @return The StatusStatistics instance.
   */
  public static StatusStatistics getInstance() {
    synchronized (StatusStatistics.class) {
      if (ms_statusStatisticsInstance == null) {
        ms_statusStatisticsInstance = new StatusStatistics();
        if (Configuration.getInstance().isStatusStatisticsEnabled() == true) {
          /* start the thread only, if statistics are enabled */
          Thread statusStatisticsThread = new Thread(ms_statusStatisticsInstance);
          statusStatisticsThread.setDaemon(true);
          statusStatisticsThread.start();
        }
      }
    }
    return ms_statusStatisticsInstance;
  }


  /**
   * Creates a new instance of StatusStatistics.
   */
  private StatusStatistics() {
    m_statisticsDatabase = new Hashtable();
  }


  /**
   * This does the periodically write out of the statistics. This method is called by the thread
   * started in getInstance().
   */
  public void run() {
    while (true) {
      /* calculate the time we can sleep */
      long sleepTime = Configuration.getInstance().getStatusStatisticsInterval() - (System.currentTimeMillis() % Configuration.getInstance().getStatusStatisticsInterval());
      /* sleep until next write out of the statistics */
      try {
        Thread.sleep(sleepTime);
      }
      catch (Exception e) {
      }
      long currentTime = System.currentTimeMillis();
      /* the timestamp is always the start of the last statistics period */
      String timeStamp = Long.toString( ( (currentTime / Configuration.getInstance().getStatusStatisticsInterval()) - 1) * Configuration.getInstance().getStatusStatisticsInterval());
      Vector entryList = new Vector();
      synchronized (m_statisticsDatabase) {
        /* we need exclusive access to the database */
        /* create the list of all database values */
        Enumeration statisticsDatabaseElements = m_statisticsDatabase.elements();
        while (statisticsDatabaseElements.hasMoreElements()) {
          entryList.addElement(statisticsDatabaseElements.nextElement());
        }
        /* clear the database -> reset the statistics */
        m_statisticsDatabase.clear();
      }
      /* now write out the statistics to the files */
      Enumeration enumer = entryList.elements();
      while (enumer.hasMoreElements()) {
        StatusStatisticsEntry currentEntry = (StatusStatisticsEntry) (enumer.nextElement());
        try {
          FileWriter fw = new FileWriter(Configuration.getInstance().getStatusStatisticsLogDir() + currentEntry.getFileName(), true);
          fw.write(timeStamp + ",");
          fw.write(currentEntry.getLogString() + "\n");
          fw.flush();
          fw.close();
        }
        catch (Exception e) {
          LogHolder.log(LogLevel.ERR, LogType.MISC, "StatusStatistics: run: Could not write statistics for " + currentEntry.getId() + " to the file " + Configuration.getInstance().getStatusStatisticsLogDir() + currentEntry.getFileName() + ".\n" + e);
        }
      }
    }
  }

  /**
   * Updates the statistics for one mixcascade. If there are no statistics for the mixcascade
   * the status belongs to in this statistics period, there is a new statistics created.
   * This method does nothing, if isStatusStatisticsEnabled() in Configuration.java returns false.
   *
   * @param a_statusEntry The status for updating the statistics.
   */
  public void update(StatusInfo a_statusEntry) {
    if (Configuration.getInstance().isStatusStatisticsEnabled() == true) {
      /* do only something, if statistics are enabled */
      synchronized (m_statisticsDatabase) {
        /* we need exclusive access to the database */
        StatusStatisticsEntry statisticsEntry = (StatusStatisticsEntry) (m_statisticsDatabase.get(a_statusEntry.getId()));
        if (statisticsEntry == null) {
          /* there is no statistics for this mixcascade -> create a new one and update the database */
          m_statisticsDatabase.put(a_statusEntry.getId(), new StatusStatisticsEntry(a_statusEntry));
        }
        else {
          /* we have a statistics for the mixcascade -> update it */
          statisticsEntry.updateStatistics(a_statusEntry);
        }
      }
    }
  }

}
