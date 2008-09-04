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
package anon.util;

/**
 * This is a generic message implementation. Messages normally are used by observed objects when
 * calling the update() method of the observers (but there are also other usage scenarios
 * possible). Thus it is possible to send the reason for the update() call. Messages have a
 * message code containing the reason for the update. Also there is the possibility to append an
 * Object to the message with more detailed information. The message codes and the usage of the
 * appended object are defined within the children of this class.
 */
public abstract class AbstractMessage {

  /**
   * Stores the message code.
   */
  private int m_messageCode;

  /**
   * Stores some message data, which maybe was sent with the message.
   */
  private Object m_messageData;


  /**
   * This creates a new AbstractMessage. The message data is set to null.
   *
   * @param a_messageCode The message code.
   */
  protected AbstractMessage(int a_messageCode) {
    this(a_messageCode, null);
  }

  /**
   * This creates a new AbstractMessage.
   *
   * @param a_messageCode The message code.
   * @param a_messageData The data to send with the message.
   */
  protected AbstractMessage(int a_messageCode, Object a_messageData) {
    m_messageCode = a_messageCode;
    m_messageData = a_messageData;
  }


  /**
   * This returns the message code of this message.
   *
   * @return The message code.
   */
  public int getMessageCode() {
    return m_messageCode;
  }

  /**
   * Returns the message data, which was sent with the message. If there was no data sent with
   * the message, null is returned.
   *
   * @return The message data.
   */
  public Object getMessageData() {
    return m_messageData;
  }

}