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

import java.util.Vector;

import javax.mail.Address;

import anon.infoservice.AbstractDatabaseEntry;

/**
 * This class is the implementation of an entry in the list of the latest mailaddresses.
 */
public class MailAddressDBEntry extends AbstractDatabaseEntry {

  /**
   * Stores the mailaddress.
   */
  private Address m_mailAddress;

  private long m_creationTime;

  /**
   * Stores the times we have sent a reply to this mailaddress (as Long objects storing the
   * corresponding System.currentTimeMillis() values).
   */
  private Vector m_requestTimes;


  /**
   * Creates a new mailaddress entry.
   *
   * @param a_mailAddress The mailaddress to store.
   * @param a_requestTimes Vector of times, a response was sent to this mailaddress.
   */
  public MailAddressDBEntry(Address a_mailAddress, Vector a_requestTimes) {
    super(System.currentTimeMillis() + MailContext.MAIL_ADDRESSES_TIMEOUT);
    m_mailAddress = a_mailAddress;
    m_requestTimes = a_requestTimes;
	m_creationTime = System.currentTimeMillis();
  }


  /**
   * Returns the ID of this mailaddress entry. It's just the string representation of the
   * mailaddress.
   *
   * @return The ID of this address.
   */
  public String getId() {
    return m_mailAddress.toString();
  }

 /**
   * Returns a version number which is used to determine the more recent entry, if two entries are
   * compared (higher version number -> more recent entry).
   *
   * @return The version number for this entry, it's just the expire time.
   */
  public long getVersionNumber() {
    return getExpireTime();
  }

  public long getLastUpdate()
  {
	  return getVersionNumber();
  }

  /**
   * Returns a list of timestamps (as Long objects storing the corresponding
   * System.currentTimeMillis() values) when we have sent a reply to this mailaddress. Only the
   * latest reply timestamps are stored.
   *
   * @return The list of timestamps when we have sent a reply to this mailaddress.
   */
  public Vector getRequestTimes() {
    return m_requestTimes;
  }

}
