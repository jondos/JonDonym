/*
 Copyright (c) 2000 - 2005 The JAP-Team
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
package infoservice.mailsystem.central;

import java.io.FileInputStream;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.Vector;

import javax.mail.Session;

import anon.infoservice.Database;
import anon.infoservice.InfoServiceDBEntry;
import anon.infoservice.InfoServiceHolder;
import anon.infoservice.ListenerInterface;
import logging.LogHolder;

/**
 * This class stores the configuration for the JAP mailsystem (Singleton).
 */
public class MailContext {

  /**
   * Defines the period mail addresses will be stored in the local database to prevent DoS
   * attacks. The default is 10 minutes, if you change it also update the ban warning message sent
   * to the users.
   */
  public static final long MAIL_ADDRESSES_TIMEOUT = 10 * 60 * 1000L;

  /**
   * Defines how many requests are processed within the MAIL_ADDRESS_TIMEOUT without banning the
   * mailaddress of the recipient. If we receive more requests, the address of the recipient will
   * be banned for some time. The default is 5 messages, if you change it also update the ban
   * warning message sent to the users.
   */
  public static final int MAXIMUM_NUMBER_OF_REQUESTS = 5;

  /**
   * The period a recipient will be banned after we have received to many requests for his
   * address. The default is 10 minutes,  if you change it also update the ban warning message
   * sent to the users.
   */
  public static final long BAN_PERIOD = 10 * 60 * 1000L;


  /**
   * The time in ms until the InfoServices loaded from the config file are outdated. This should
   * never occur (1000 years should be enough).
   */
  private static final long INFOSERVICE_TIMEOUT = 1000 * 365 * 24 * 3600 * 1000L;


  /**
   * This stores the instance of MailContext (Singleton).
   */
  private static MailContext ms_mcInstance = null;


  /**
   * This stores the Session instance of the Java Mail API. All mails should be sended using this
   * session instance, because the configuration for the Java Mail API is only stored here.
   */
  private Session m_mailSession;

  /**
   * Stores the port where this central mailsystem process is listening for mail requests. Only
   * mail requests from localhost are accepted.
   */
  private int m_centralProcessPort;


  /**
   * Creates a new instance of MailContext. We load the configuration from the specified file and
   * configure the Java Mail API and the InfoServiceDatabase. If there occurs an error while
   * loading the configuration, an Exception is thrown.
   *
   * @param a_configFile The path and the filename of the configuration file.
   */
  private MailContext(String a_configFile) throws Exception {
    Properties mailConfig = new Properties();
    /* set some default values, they can be overwritten by the properties loaded from the config
     * file
     */
    mailConfig.put("mail.stmp.sendpartial", "true");
    mailConfig.put("mail.transport.protocol", "smtp");
    /* load the configuration */
    mailConfig.load(new FileInputStream(a_configFile));
    /* configuration was loaded successfully */

    /* try to read the logging configuration */
    LogHolder.setLogInstance(new MailSystemLog(mailConfig));
    LogHolder.setDetailLevel(Integer.parseInt(mailConfig.getProperty("messageDetailLevel", "0").trim()));

    /* read the mail configuration */
    m_mailSession = Session.getInstance(mailConfig);

    /* read the port where to listen for mail requests */
    m_centralProcessPort = Integer.parseInt(mailConfig.getProperty("MailSystemMainProcessPort").trim());
    if ((m_centralProcessPort < 1) || (m_centralProcessPort > 65535)) {
      throw (new Exception("MailSystemMainProcessPort is invalid."));
    }

    /* try to read the infoservices to use */
    String infoServiceList = mailConfig.getProperty("MailSystemInfoServiceList");
    if (infoServiceList != null) {
      infoServiceList = infoServiceList.trim();
      /* we have a list of infoservices */
      StringTokenizer stInfoServiceList = new StringTokenizer(infoServiceList, ",");
      while (stInfoServiceList.hasMoreTokens()) {
        StringTokenizer stCurrentInfoService = new StringTokenizer(stInfoServiceList.nextToken(), ":");
        Database.getInstance(InfoServiceDBEntry.class).update(new InfoServiceDBEntry(null, null, new ListenerInterface(stCurrentInfoService.nextToken().trim(), Integer.parseInt(stCurrentInfoService.nextToken().trim())).toVector(), true, true, 0, 0, false));
      }
    }
    InfoServiceHolder.getInstance().setChangeInfoServices(true);
  }


  /**
   * Creates an instance of MailContext, if there is already one, the old one is overwritten
   * (Singleton).
   *
   * @param a_configFile The path and the filename of the configuration file to use for the
   *                     new instance.
   */
  public static void createInstance(String a_configFile) throws Exception {
    ms_mcInstance = new MailContext(a_configFile);
  }

  /**
   * Returns the instance of MailContext (Singleton). If there was no one created until yet, null
   * is returned.
   *
   * @return The instance of MailContext.
   */
  public static MailContext getInstance() {
    return ms_mcInstance;
  }


  /**
   * Returns the initialized Session instance for the Java Mail API.
   *
   * @return The Session instance to use with the Java Mail API.
   */
  public Session getSession() {
    return m_mailSession;
  }

  /**
   * Retruns the port where this central mailsystem process is listening for mail requests. Only
   * mail requests from localhost are accepted.
   *
   * @return The port on localhost where this central mailsystem process is listening for mail
   *         requests.
   */
  public int getCentralProcessPort() {
    return m_centralProcessPort;
  }

}


