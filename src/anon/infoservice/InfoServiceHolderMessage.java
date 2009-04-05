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
package anon.infoservice;

import anon.util.AbstractMessage;

/**
 * This is the message implementation used for InfoServiceHolder messages. It is used from the
 * instance of the InfoServiceHolder class to notify their observers about some events (like 
 * changes of the preferred infoservice). The message code identifies the reason of the
 * notification.
 */
public class InfoServiceHolderMessage extends AbstractMessage {

  /**
   * This message is sent after the preferred infoservice was changed. The appended object is the
   * InfoServiceDBEntry of the new preferred infoservice.
   */
  public static final int PREFERRED_INFOSERVICE_CHANGED = 1;

  /**
   * This message is sent after the policy for automatic infoservice changes has been changed. The
   * appended object is a Boolean with the new change policy (true, if automatic changes of the
   * infoservices including the preferred infoservice are possible in case of failure or false, if
   * only the pererred infoservice is used all the time).
   */
  public static final int INFOSERVICE_MANAGEMENT_CHANGED = 2;
  
  /**
   * Indicated that all InfoServices either have invalid or expired certificates.
   */
  public static final int INFOSERVICES_NOT_VERIFYABLE = 3;


  /**
   * This creates a new InfoServiceHolderMessage. The message data is set to null.
   *
   * @param a_messageCode The message code. See the constants in this class.
   */
  public InfoServiceHolderMessage(int a_messageCode) {
    super(a_messageCode);
  }

  /**
   * This creates a new InfoServiceHolderMessage.
   *
   * @param a_messageCode The message code. See the constants in this class.
   * @param a_messageData The data to send with the message.
   */
  public InfoServiceHolderMessage(int a_messageCode, Object a_messageData) {
    super(a_messageCode, a_messageData);
  }

}