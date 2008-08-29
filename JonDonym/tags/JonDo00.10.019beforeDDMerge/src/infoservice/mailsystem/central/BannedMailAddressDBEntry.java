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

import javax.mail.Address;

import anon.infoservice.AbstractDatabaseEntry;

/**
 * This class is the implementation of an entry in the list of banned mailaddresses.
 */
public class BannedMailAddressDBEntry extends AbstractDatabaseEntry {

  /**
   * Stores the banned mailaddress.
   */
  private Address m_mailAddress;

  /**
   * Creates a new banned mailaddress entry.
   *
   * @param a_mailAddress The mailaddress to ban.
   */
  public BannedMailAddressDBEntry(Address a_mailAddress) {
    super(System.currentTimeMillis() + MailContext.BAN_PERIOD);
    m_mailAddress = a_mailAddress;
  }


  /**
   * Returns the ID of this banned mailaddress entry. It's just the string representation of the
   * banned mailaddress.
   *
   * @return The ID of this banned address.
   */
  public String getId() {
    return m_mailAddress.toString();
  }

  public long getLastUpdate()
  {
	  return getVersionNumber();
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

}
