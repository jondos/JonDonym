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
package jap;

import anon.util.AbstractMessage;

/**
 * This is the message implementation used for JAPController messages. It is used from the
 * instance of the JAPController class to notify their observers about some events or changes of
 * settings. The message code identifies the reason of the notification.
 */
public class JAPControllerMessage extends AbstractMessage {

  /**
   * This message is sent after the automatic infoservice requests policy has been changed.
   */
  public static final int INFOSERVICE_POLICY_CHANGED = 1;

  public static final int CURRENT_MIXCASCADE_CHANGED = 2;

  public static final int ASK_SAVE_PAYMENT_CHANGED = 3;


  /**
   * This creates a new JAPControllerMessage. The message data is set to null.
   *
   * @param a_messageCode The message code. See the constants in this class.
   */
  public JAPControllerMessage(int a_messageCode) {
    super(a_messageCode);
  }

  /**
   * This creates a new JAPControllerMessage.
   *
   * @param a_messageCode The message code. See the constants in this class.
   * @param a_messageData The data to send with the message.
   */
  public JAPControllerMessage(int a_messageCode, Object a_messageData) {
    super(a_messageCode, a_messageData);
  }

}
