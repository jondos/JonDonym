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
 * This is the message implementation used for database-specific messages. It is used from the
 * instances of the Database class to notify their observers about database events. The message
 * code identifies the reason of the notification.
 */
public class DatabaseMessage extends AbstractMessage {

  /**
   * This message is sent after an entry was added to the database. The appended object in this
   * message will be the added entry.
   */
  public static final int ENTRY_ADDED = 1;

  /**
   * This message is sent after an entry was renewed in the database. The appended object in this
   * message will be the entry after the update.
   */
  public static final int ENTRY_RENEWED = 2;

  /**
   * This message is sent, if a single entry is removed from the database. The appended object
   * will be the removed entry.
   */
  public static final int ENTRY_REMOVED = 3;

  /**
   * This message is sent, if the database is cleared (all entries are removed at once). There
   * will be no appended object in this case.
   */
  public static final int ALL_ENTRIES_REMOVED = 4;

  /**
   * This message is sent, if an observer registers at the database. The appended object will be
   * a Vector of all database entries. This will be always the first message after the
   * registration.
   */
  public static final int INITIAL_OBSERVER_MESSAGE = 5;

  /**
   * This creates a new DatabaseMessage. The message data is set to null.
   *
   * @param a_messageCode The message code. See the constants in this class.
   */
  public DatabaseMessage(int a_messageCode)
  {
    super(a_messageCode);
  }

  /**
   * This creates a new DatabaseMessage.
   *
   * @param a_messageCode The message code. See the constants in this class.
   * @param a_messageData The data to send with the message.
   */
  public DatabaseMessage(int a_messageCode, Object a_messageData)
  {
    super(a_messageCode, a_messageData);
  }

}
