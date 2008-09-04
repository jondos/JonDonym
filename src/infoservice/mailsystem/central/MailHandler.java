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

import java.io.InputStream;
import java.util.Vector;

import javax.mail.Address;
import javax.mail.MessagingException;
import javax.mail.Transport;
import javax.mail.internet.MimeMessage;

import anon.infoservice.Database;
import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;

/**
 * This class handles the received mails.
 */
public class MailHandler {
  
  /**
   * Stores the received message.
   */
  private MimeMessage m_receivedMessage;
  
  
  /**
   * Creates a new MailHandler. The message is read and parsed from the specified InputStream.
   * If the data of the InputStream is not a valid mail message, a MessagingException is thrown.
   *
   * @param a_receivedMail An InputStream, which contains the data of a received e-mail.
   */
  public MailHandler(InputStream a_receivedMail) throws MessagingException {
    m_receivedMessage = new MimeMessage(MailContext.getInstance().getSession(), a_receivedMail);
  } 
  
  
  /**
   * This creates the reply for the received message, depending on the subject and message data.
   * The reply is sent back to the sender. If there occurs an error while replying to the message,
   * an Exception is thrown.
   */
  public void createReply() throws Exception {
    Address[] replyAddresses = m_receivedMessage.getReplyTo();
    if (replyAddresses != null) {
      Vector validReplyAddresses = new Vector();
      Vector warnReplyAddresses = new Vector();
      for (int i = 0; i < replyAddresses.length; i++) {
        Address currentAddress = replyAddresses[i];
        if (currentAddress != null) {
          synchronized (MailHandler.class) {
            /* we have exclusive access to the database, no other mail-handling thread will
             * access it
             */
            if (!validReplyAddresses.contains(currentAddress) && !warnReplyAddresses.contains(currentAddress)) {
              /* we will handle every recipient address only once */
              if (Database.getInstance(BannedMailAddressDBEntry.class).getEntryById(currentAddress.toString()) == null) {
                /* the current reply address is not banned */           
                MailAddressDBEntry oldEntry = (MailAddressDBEntry)(Database.getInstance(MailAddressDBEntry.class).getEntryById(currentAddress.toString()));
                Vector lastRequestTimes = null;
                if (oldEntry != null) {
                  /* we know the current recipient address already --> get the times of the last
                   * requests
                   */
                  lastRequestTimes = oldEntry.getRequestTimes();
                }
                else {
                  /* we don't know the current recipient address -> this request will be the first
                   */
                  lastRequestTimes = new Vector();
                }
                while ((lastRequestTimes.size() > 0) && (((Long)(lastRequestTimes.firstElement())).longValue() < (System.currentTimeMillis() - MailContext.MAIL_ADDRESSES_TIMEOUT))) {
                  /* we can remove the first request time, because it is outdated */
                  lastRequestTimes.removeElementAt(0);
                }
                /* check how often we have already sent a message to that address within the last
                 * minutes
                 */
                if (lastRequestTimes.size() >= MailContext.MAXIMUM_NUMBER_OF_REQUESTS) {
                  /* ban the current recipient address and send a warning message */
                  warnReplyAddresses.addElement(currentAddress);
                  Database.getInstance(BannedMailAddressDBEntry.class).update(new BannedMailAddressDBEntry(currentAddress));
                  LogHolder.log(LogLevel.INFO, LogType.MISC, "MailHandler: createReply: The reply address " + currentAddress.toString() + " is added to the ban-list and will receive a warning.");
                }
                else {
                  /* the number of requests is within the limit -> send the normal reply and add
                   * this request to the request list
                   */
                  validReplyAddresses.addElement(currentAddress);
                  lastRequestTimes.addElement(new Long(System.currentTimeMillis()));
                  Database.getInstance(MailAddressDBEntry.class).update(new MailAddressDBEntry(currentAddress, lastRequestTimes));
                  LogHolder.log(LogLevel.DEBUG, LogType.MISC, "MailHandler: createReply: The reply address " + currentAddress.toString() + " is valid and will receive a reply.");
                }
              }
              else {
                LogHolder.log(LogLevel.DEBUG, LogType.MISC, "MailHandler: createReply: The reply address " + currentAddress.toString() + " is banned and will not receive anything.");
              }
            }
            else {
              LogHolder.log(LogLevel.DEBUG, LogType.MISC, "MailHandler: createReply: The reply address " + currentAddress.toString() + " was already handled within this request.");
            }
          }
        }
      }               
      if (validReplyAddresses.size() > 0) {
        LogHolder.log(LogLevel.DEBUG, LogType.MISC, "MailHandler: createReply: Create reply for valid recipients.");    
        Address[] currentReplyAddresses = new Address[validReplyAddresses.size()];
        validReplyAddresses.copyInto(currentReplyAddresses);
        m_receivedMessage.setReplyTo(currentReplyAddresses);
        String subject = m_receivedMessage.getSubject().trim();
        MimeMessage reply = (MimeMessage)(m_receivedMessage.reply(false));
        CommandFactory.getCommandImplementation(subject, false).createReplyMessage(m_receivedMessage, reply);
        LogHolder.log(LogLevel.DEBUG, LogType.MISC, "MailHandler: createReply: Reply created. Starting transport to SMTP server.");    
        Transport.send(reply);
        LogHolder.log(LogLevel.DEBUG, LogType.MISC, "MailHandler: createReply: Reply was sent."); 
      }   
      if (warnReplyAddresses.size() > 0) {
        LogHolder.log(LogLevel.DEBUG, LogType.MISC, "MailHandler: createReply: Create warning for recipients added to the ban-list.");    
        Address[] currentReplyAddresses = new Address[warnReplyAddresses.size()];
        warnReplyAddresses.copyInto(currentReplyAddresses);
        m_receivedMessage.setReplyTo(currentReplyAddresses);
        String subject = m_receivedMessage.getSubject().trim();
        MimeMessage reply = (MimeMessage)(m_receivedMessage.reply(false));
        CommandFactory.getCommandImplementation(subject, true).createReplyMessage(m_receivedMessage, reply);
        LogHolder.log(LogLevel.DEBUG, LogType.MISC, "MailHandler: createReply: Ban warning created. Starting transport to SMTP server.");    
        Transport.send(reply);
        LogHolder.log(LogLevel.DEBUG, LogType.MISC, "MailHandler: createReply: Ban warning was sent."); 
      } 
    }
    else {
      LogHolder.log(LogLevel.ERR, LogType.MISC, "MailHandler: createReply: Received request doesn't contain any addresses useable for creating a reply."); 
    }  
  }

}