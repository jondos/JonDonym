/*
 Copyright (c) 2000 - 2006, The JAP-Team
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
import java.util.Vector;

import anon.util.Util;

/**
 * Objects of this class store a language code and can translate it into
 * the localised name of the corresponding language. The default locale of
 * this class is bound to the locale used in JAPMessages.
 *
 * @author Rolf Wendolsky
 * @see gui.JAPMessages
 * @see http://www.w3.org/WAI/ER/IG/ert/iso639.htm
 * @version ISO 639 on 27.12.05
 */
public class LanguageMapper extends AbstractISOCodeMapper
{
	private static final String[] ms_languageCodes =
		{
		// Officially assigned code elements
		"AA", "AB", "AF", "AM", "AR", "AS", "AY", "AZ", "BA", "BE", "BG", "BH",
		"BI", "BN", "BO", "BR", "CA", "CO", "CS", "CY", "DA", "DE", "DZ", "EL",
		"EN", "EO", "ES", "ET", "EU", "FA", "FI", "FJ", "FO", "FR", "FY", "GA",
		"GD", "GL", "GN", "GU", "HA", "HI", "HR", "HU", "HY", "IA", "IE", "IK",
		"IN", "IS", "IT", "IW", "JA", "JI", "JW", "KA", "KK", "KL", "KM", "KN",
		"KO", "KS", "KU", "KY", "LA", "LN", "LO", "LT", "LV", "MG", "MI", "MK",
		"ML", "MN", "MO", "MR", "MS", "MT", "MY", "NA", "NE", "NL", "NO", "OC",
		"OM", "OR", "PA", "PL", "PS", "PT", "QU", "RM", "RN", "RO", "RU", "RW",
		"SA", "SD", "SG", "SH", "SI", "SK", "SL", "SM", "SN", "SO", "SQ", "SR",
		"SS", "ST", "SU", "SV", "SW", "TA", "TE", "TG", "TH", "TI", "TK", "TL",
		"TN", "TO", "TR", "TS", "TT", "TW", "UK", "UR", "UZ", "VI", "VO", "WO",
		"XH", "YO", "ZH", "ZU"
	};

	private static final String MSG_CHOOSE_LANGUAGE = LanguageMapper.class.getName() + "_ChooseLanguage";

	private Locale m_locale;

	/**
	 * Constructs an empty LanguageMapper object. Its toString() method
	 * returns a message that requests to choose a valid language code.
	 * The message is defined by the default Locale.
	 */
	public LanguageMapper()
	{
		super();
		createLocale();
	}

	/**
	 * Constructs an empty LanguageMapper object. Its toString() method
	 * returns a message that requests to choose a valid language code.
	 * The message is defined by the default Locale.
	 * @param a_maxTextLength the maximum length of the toString() output
	 */
	public LanguageMapper(int a_maxTextLength)
	{
		super(a_maxTextLength);
		createLocale();
	}

	/**
	 * Constructs a new LanguageMapper that uses the default Locale to
	 * translate its language code.
	 * @see getLocalisedCountries(int)
	 * @param a_ISOCode a two-letter language code
	 * @param a_maxTextLength the maximum length of the toString() output
	 * @throws IllegalArgumentException
	 */
	public LanguageMapper(String a_ISOCode, int a_maxTextLength)
		throws IllegalArgumentException
	{
		super(a_ISOCode, a_maxTextLength);
		createLocale();
	}

	/**
	 * Constructs a new LanguageMapper that uses the default Locale to
	 * translate its language code.
	 * @see getLocalisedCountries(int)
	 * @param a_ISOCode a two-letter language code
	 * @throws IllegalArgumentException
	 */
	public LanguageMapper(String a_ISOCode)
		throws IllegalArgumentException
	{
		super(a_ISOCode);
		createLocale();
	}

	/**
	 * Constructs a new LanguageMapper that uses a specific Locale to
	 * translate its language code.
	 * @see getLocalisedCountries(int, Locale)
	 * @param a_ISOCode a two-letter language code
	 * @param a_locale a Locale
	 * @throws IllegalArgumentException
	 */
	public LanguageMapper(String a_ISOCode, Locale a_locale)
		throws IllegalArgumentException
	{
		super(a_ISOCode, a_locale);
		createLocale();
	}

	/**
	 * Constructs a new LanguageMapper that uses a specific Locale to
	 * translate its language code.
	 * @see getLocalisedCountries(int, Locale)
	 * @param a_ISOCode a two-letter language code
	 * @param a_locale a Locale
	 * @param a_maxTextLength the maximum length of the toString() output
	 * @throws IllegalArgumentException
	 */
	public LanguageMapper(String a_ISOCode, int a_maxTextLength, Locale a_locale)
		throws IllegalArgumentException
	{
		super(a_ISOCode, a_maxTextLength, a_locale);
		createLocale();
	}

	/**
	 * Returns a Locale object constructed from the language code.
	 * @return a Locale object constructed from the language code
	 */
	public Locale getLocale()
	{
		return m_locale;
	}

	/**
	 * Creates a Locale object from the language code and a given country code.
	 * @param a_countryCode an ISO country code
	 * @return a Locale object from the language code and a given country code
	 * @see gui.CountryMapper
	 */
	public Locale getLocale(String a_countryCode)
	{
		if (getISOCode().length() == 0)
		{
			return null;
		}
		return new Locale(getISOCode(), a_countryCode);
	}

	/**
	 * Returns a Vector with LanguageMappers for all officially assigned
	 * language codes. The constructed LanguageMappers use the default locale.
	 * @return a Vector with LanguageMappers for all officially assigned
	 * language codes
	 */
	public static Vector getLocalisedLanguages()
	{
		return (getLocalisedLanguages(0, null));
	}

	/**
	 * Returns a Vector with LanguageMappers for all officially assigned
	 * language codes. The constructed LanguageMappers use the default locale.
	 * @param a_loc the locale that is used in all contructed LanguageMappers
	 * @return a Vector with LanguageMappers for all officially assigned
	 * language codes
	 */
	public static Vector getLocalisedLanguages(Locale a_loc)
	{
		return (getLocalisedLanguages(0, a_loc));
	}

	/**
	 * Returns a Vector with LanguageMappers for all officially assigned
	 * language codes. The constructed LanguageMappers use the default locale.
	 * @param a_maxTextLength the maximum length of the toString() output
	 * @return a Vector with LanguageMappers for all officially assigned
	 * language codes
	 */
	public static Vector getLocalisedLanguages(int a_maxTextLength)
	{
		return (getLocalisedLanguages(a_maxTextLength, null));
	}

	/**
	 * Returns a Vector with LanguageMappers for all officially assigned
	 * language codes.
	 * @param a_loc the locale that is used in all contructed LanguageMappers
	 * @param a_maxTextLength the maximum length of the toString() output
	 * @return a Vector with LanguageMappers for all officially assigned
	 * language codes
	 */
	public static Vector getLocalisedLanguages(int a_maxTextLength, Locale a_loc)
	{
		Vector localisedCountries = new Vector();

		for (int i = 0; i < ms_languageCodes.length; i++)
		{
			localisedCountries.addElement(
				 new LanguageMapper(ms_languageCodes[i], a_maxTextLength, a_loc));
		}
		return Util.sortStrings(localisedCountries);
	}

	protected final String getChooseMessage()
	{
		return JAPMessages.getString(MSG_CHOOSE_LANGUAGE);
	}

	protected String getJRETransaltionOfISOCode(String a_ISOLanguageCode, Locale a_locale)
	{
		return new Locale(a_ISOLanguageCode, a_locale.getCountry()).getDisplayLanguage(a_locale);
	}

	/**
	 * Creates the internal Locale object from the language code.
	 */
	private void createLocale()
	{
		if (getISOCode().length() > 0)
		{
			m_locale = new Locale(getISOCode(), "");
		}
	}
}
