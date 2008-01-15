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

import javax.mail.internet.MimeMessage;

/**
 * This interface specifies the methods any JAP mailsystem command has to implement.
 */
public abstract class AbstractMailSystemCommand {
  
  /**
   * Stores the MailMessages instance used for creating localized versions of the response
   * messages.
   */
  private MailMessages m_localization;
  
  
  /**
   * Initializes a new instance of AbstractMailSystemCommand.
   *
   * @param a_localization The MailMessages instance with the localization to use when creating
   *                       the response messages.
   */
  protected AbstractMailSystemCommand(MailMessages a_localization) {
    m_localization = a_localization;
  }

    
  /**
   * This method shall create the reply message. If there is something wrong with the received
   * message (maybe invalid data) or while creating the reply, an Exception can be thrown. The
   * children of the class must implement this method.
   *
   * @param a_receivedMessage The message the JAP mailsystem has received from a user.
   * @param a_replyMessage A pre-initialized message (recipients and subject already set), which
   *                       shall be filled with the reply.
   */
  public abstract void createReplyMessage(MimeMessage a_receivedMessage, MimeMessage a_replyMessage) throws Exception;
  
  
  /**
   * Returns the MailMessages instance with the localization to use when creating response
   * messages.
   *
   * @return The MailMessages instance creating localized versions of the response messages.
   */
  protected MailMessages getLocalization() {
    return m_localization;
  }
  
}