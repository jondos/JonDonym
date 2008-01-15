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

import java.util.Locale;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;

/**
 * This class is the implementation of the mail message localizations.
 */
public class MailMessages {

  /**
   * This is the base of the resource files. The single single resource files append this base
   * .properties (default resource, if necessary) or _??.properties, where ?? is the language code,
   * e.g. 'de' (localized resources).
   */
  private static final String RESOURCE_BASE = "infoservice/mailsystem/central/messages/MailMessages";

  /**
   * This is the default localization (English as default).
   */
  private static final Locale DEFAULT_LOCALE = Locale.ENGLISH;


  /**
   * This stores the resource for the used localization.
   */
  private ResourceBundle m_messageResource;


  /**
   * Creates a new MailMessages instance. 
   *
   * @param a_localeToUse The localization to use when obtaining strings via the new instance. If
   *                      there is no resource for the specified localization, the default one is
   *                      used.
   */
  public MailMessages(Locale a_localeToUse) {
    try {
      m_messageResource = PropertyResourceBundle.getBundle(RESOURCE_BASE, a_localeToUse);
    }
    catch (Exception e) {
      /* the resources for the specified locale could not be found, try to get it for the default
       * locale
       */
      try {
        m_messageResource = PropertyResourceBundle.getBundle(RESOURCE_BASE, DEFAULT_LOCALE);
      }
      catch (Exception e2) {
        /* also the default locale isn't available, we can't do anything */
        m_messageResource = null;
      }
    }
  }


  /**
   * Returns the localized version of the message assigned to the specified key in the resource
   * files.
   *
   * @param a_key A key, which specifies the wanted message.
   *
   * @return The localized version of the specified message. If we can't find a localized version
   *         of the message, the default localization is returned. If that also fails, the key
   *         itself is returned.
   */
  public String getString(String a_key) {
    String localeString = null;
    try {
      localeString = m_messageResource.getString(a_key);
    }
    catch (Exception e) {
      /* if there was an exception, the default value (the key itself) is returned */
      localeString = a_key;
    }
    return localeString;
  }
  
}