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
package gui;

import java.util.Locale;

/**
 * Objects of this class store an ISO two-letter code and can translate it into a localised name.
 *
 * @author Kuno G. Gruen
 * @author Rolf Wendolsky
 */
public abstract class AbstractISOCodeMapper
{
	private final int MAX_LENGTH;
	private boolean m_bUseDefaultLocale;
	private String m_iso2;
	private Locale m_locale;

	/**
	 * Constructs an empty mapper object. Its toString() method
	 * returns a message that requests to choose a valid mapped object.
	 * The message is defined by the default Locale.
	 */
	public AbstractISOCodeMapper()
	{
		this(null, 0);
	}

	/**
	 * Constructs an empty mapper object. Its toString() method
	 * returns a message that requests to choose a valid country code.
	 * The message is defined by the default Locale.
	 * @param a_maxTextLength the maximum length of the toString() output
	 */
	public AbstractISOCodeMapper(int a_maxTextLength)
	{
		this(null, a_maxTextLength);
	}

	/**
	 * Constructs a new mapper that uses the default Locale to translate its ISO code.
	 * @param a_ISOCode a two-letter ISO code
	 * @param a_maxTextLength the maximum length of the toString() output
	 * @throws IllegalArgumentException if the ISO code does not have two characters
	 */
	public AbstractISOCodeMapper(String a_ISOCode, int a_maxTextLength) throws IllegalArgumentException
	{
		this(a_ISOCode, a_maxTextLength, null);
	}

	/**
	 * Constructs a new mapper object that uses the default Locale to translate its ISO code.
	 * @param a_ISOCode a two-letter ISO code
	 * @throws IllegalArgumentException if the ISO code does not have two characters
	 */
	public AbstractISOCodeMapper(String a_ISOCode) throws IllegalArgumentException
	{
		this(a_ISOCode, 0, null);
	}

	/**
	 * Constructs a new mapper object that uses a specific Locale to translate its ISO code.
	 * @param a_ISOCode a two-letter ISO code
	 * @param a_locale a Locale
	 * @throws IllegalArgumentException if the ISO code does not have two characters
	 */
	public AbstractISOCodeMapper(String a_ISOCode, Locale a_locale) throws IllegalArgumentException
	{
		this(a_ISOCode, 0, a_locale);
	}

	/**
	 * Constructs a new mapper object that uses a specific Locale to translate its ISO code.
	 * @param a_ISOCode a two-letter ISO code
	 * @param a_locale a Locale
	 * @param a_maxTextLength the maximum length of the toString() output
	 * @throws IllegalArgumentException if the ISO code does not have two characters
	 */
	public AbstractISOCodeMapper(String a_ISOCode, int a_maxTextLength,
								Locale a_locale) throws IllegalArgumentException
	{
		MAX_LENGTH = a_maxTextLength;

		if (a_ISOCode == null || a_ISOCode.trim().length() == 0)
		{
			a_ISOCode = "";
		}
		if (a_ISOCode.length() > 0 && a_ISOCode.length() != 2)
		{
			throw new IllegalArgumentException(
				"Mapped ISO code must have a length of two characters!");
		}

		m_iso2 = a_ISOCode.trim().toUpperCase();

		if (a_locale == null)
		{
			m_bUseDefaultLocale = true;
			m_locale = JAPMessages.getLocale();
		}
		else
		{
			m_bUseDefaultLocale = false;
			m_locale = a_locale;
		}
	}

	/**
	 * Returns the ISO country code stored in this CountryMapper object. The country code may be an empty
	 * String of the length zero or a valid two-letter country code.
	 * @return the ISO country code stored in this CountryMapper object
	 */
	public final String getISOCode()
	{
		return m_iso2.toLowerCase();
	}

	/**
	 * Returns if the ISO codes of two mapper objects are equal.
	 * @param a_object an Object
	 * @return true if the ISO country codes of two mapper objects are
	 *         equal; false otherwise
	 */
	public final boolean equals(Object a_object)
	{
		if (a_object == null || ! (a_object instanceof AbstractISOCodeMapper))
		{
			return false;
		}
		return getISOCode().equals( ( (AbstractISOCodeMapper) a_object).getISOCode());
	}

	/**
	 * Returns the hash code of the ISO country code.
	 * @return the hash code of the ISO country code
	 */
	public final int hashCode()
	{
		return getISOCode().hashCode();
	}

	/**
	 * Returns a message that is displayed when this mapped object has an empty ISO code.
	 * The user is asked to choose a mapped object.
	 * @return a message that is displayed when this mapped object has an empty ISO code
	 */
	protected abstract String getChooseMessage();

	protected abstract String getJRETransaltionOfISOCode(String a_ISOCode, Locale a_locale);

	/**
	 * Returns the localised name of the ISO country code of this
	 * CountryMapper object. The output may depend on the current locale or
	 * on the locale that may be defined in the constructor.
	 * @return the localised name of the ISO country code
	 */
	public final String toString()
	{
		String strCName;
		String temp;
		Locale locale;

		if (m_iso2.length() == 0)
		{
			strCName = getChooseMessage();
		}
		else
		{
			if (m_bUseDefaultLocale)
			{
				locale = JAPMessages.getLocale();
			}
			else
			{
				locale = m_locale;
			}

			strCName = getJRETransaltionOfISOCode(m_iso2, locale);

			if (strCName == null || strCName.trim().length() == 0 || strCName.equals(m_iso2) ||
				// some old JREs cannot resolve the country code correctly
				(strCName.equals(getJRETransaltionOfISOCode("AA", locale)) &&
				 strCName.equals(getJRETransaltionOfISOCode("ZZ", locale))))
			{
				temp = getClass().getName() + "_" + m_iso2;
				strCName = JAPMessages.getString(temp);
				if (strCName.equals(temp))
				{
					// the country could not be translated
					strCName = m_iso2;
				}
			}
		}

		if (MAX_LENGTH > 0 && strCName.length() > MAX_LENGTH)
		{
			strCName = strCName.substring(0, MAX_LENGTH);
		}
		if (strCName != null && strCName.length() > 1)
		{
			strCName = strCName.substring(0, 1).toUpperCase() + strCName.substring(1, strCName.length());
		}

		return strCName;
	}
}
