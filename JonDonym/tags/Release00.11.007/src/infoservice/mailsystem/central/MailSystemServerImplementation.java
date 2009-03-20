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

import infoservice.mailsystem.central.server.AbstractServerImplementation;
import infoservice.mailsystem.central.server.util.LimitedLengthInputStream;
import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;

/**
 * This class is the server implementation for receiving mails from the MailForwarder.
 * It's used when the GenericServer accepts a new connection (and the mailsystem factory is
 * defined).
 */
public class MailSystemServerImplementation extends AbstractServerImplementation {
  
  /**
   * Handles a request from the MailForwarder.
   */
  protected void handleClientRequest() {
    try {
      /* set a timeout of 10s for each read-operation */
      getSocketContainer().setSocketTimeout(10000);
      /* cut off every mail after 100.000 bytes */
      LimitedLengthInputStream requestStream = new LimitedLengthInputStream(getSocketContainer().getInputStream(), 100000);
      /* parse the input stream and answer the message */
      LogHolder.log(LogLevel.DEBUG, LogType.MISC, "MailSystemServerImplementation: handleClientRequest: Reading the request mail...");
      MailHandler mailHandler = new MailHandler(requestStream);
      LogHolder.log(LogLevel.DEBUG, LogType.MISC, "MailSystemServerImplementation: handleClientRequest: Request mail read. Creating reply...");
      mailHandler.createReply();
    }
    catch (Exception e) {
      LogHolder.log(LogLevel.ERR, LogType.MISC, "MailSystemServerImplementation: handleClientRequest: Error occured while responding to received mail: " + e.toString());
    }
    /* the socket is closed automatically */
  }

}