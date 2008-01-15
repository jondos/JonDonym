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
package infoservice.mailsystem.central.commands;

import javax.mail.internet.MimeMessage;

import infoservice.mailsystem.central.MailMessages;
import infoservice.mailsystem.central.AbstractMailSystemCommand;

/**
 * This is the implementation for generating a reply message for the BanWarning command.
 */
public class BanWarningCommand extends AbstractMailSystemCommand {
    
  /**
   * Creates a new instance of BanWarningCommand.
   *
   * @param a_localization The MailMessages instance with the localization to use when creating
   *                       the response message.
   */
  public BanWarningCommand(MailMessages a_localization) {
    super(a_localization);
  }

  
  /**
   * Creates the reply for the BanWarning command.
   *
   * @param a_receivedMessage The message we have received (not used).
   * @param a_replyMessage A pre-initialized message (recipients and subject already set), which
   *                       shall be filled with the BanWarning reply.
   */
  public void createReplyMessage(MimeMessage a_receivedMessage, MimeMessage a_replyMessage) throws Exception {
    a_replyMessage.setContent(getLocalization().getString("banWarningMessage"), "text/plain");
  }
  
} 