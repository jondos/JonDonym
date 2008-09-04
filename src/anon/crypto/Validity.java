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
package anon.crypto;

import java.util.Calendar;
import java.util.Date;

/**
 * Describes the validity period of something by defining a start date (valid from) and an
 * end date (valid to). The validity can then be tested for a given date.
 * @author Rolf Wendolsky
 */
public class Validity
{
	private Calendar m_validFrom;
	private Calendar m_validTo;

	/**
	 * This is the minimal time (in minutes) for a validity created by
	 * createValidTo().
	 */
	private static final int TEMPORARY_VALIDITY_IN_MINUTES = 10;

	/**
	 * Creates a new Validity.
	 * @param a_validFrom a Calendar object that represents a start date
	 * @param a_validityInYears an amount of years that the created date should lie ahead from the
	 *                          start date; if the amount is less than 0 the created date will
	 *                          only be ahead from the start date for a very small time
	 *                          (some minutes)
	 * @throws IllegalArgumentException if the calendar is null
	 */
	public Validity(Calendar a_validFrom, int a_validityInYears)
	{
		this(a_validFrom, createValidTo(a_validFrom, a_validityInYears));
	}

	/**
	 * Creates a new Validity.
	 * @param a_validFrom a Calendar object that represents a start date
	 * @param a_validTo a Calendar object that represents a end date; if the date lies before
	 * the start date it will be set equal to the start date
	 * @throws IllegalArgumentException if on of the calendars is null
	 */
	public Validity(Calendar a_validFrom, Calendar a_validTo)
	{
		if (a_validFrom == null || a_validTo == null)
		{
			throw new IllegalArgumentException("Calendars for validity must not be null!");
		}

		m_validFrom = (Calendar)a_validFrom.clone();
		if (a_validTo.before(a_validFrom))
		{
			m_validTo = m_validFrom;
		}
		else
		{
			m_validTo = (Calendar)a_validTo.clone();
		}
	}

	/**
	 * Returns the first valid date in the validity period.
	 * @return the first valid date in the validity period
	 */
	public Date getValidFrom()
	{
		return m_validFrom.getTime();
	}

	/**
	 * Returns the last valid date in the validity period.
	 * @return the last valid date in the validity period
	 */
	public Date getValidTo()
	{
		return m_validTo.getTime();
	}

	/**
	 * Checks if the given date is contained in the validity period.
	 * @param a_date a Date
	 * @return true if the given date is contained in the validity period; false otherwise
	 */
	public boolean isValid(Date a_date)
	{
		return !(a_date.before(getValidFrom()) || a_date.after(getValidTo()));
	}

	/**
	 * For a given Calendar object that represents a start date (valid from), this method
	 * creates an end date (valid to) that lies a specified amount of years ahead from the start
	 * date.
	 * @param a_validFrom a Calendar object that represents a start date
	 * @param a_validityInYears an amount of years that the created date should lie ahead from the
	 *                          start date; if the amount is less than 0 the created date will
	 *                          only be ahead from the start date for a very small time
	 *                          (some minutes)
	 * @return an end date (valid to) that lies a specified amount of years ahead from the start
	 *         date
	 */
	private static Calendar createValidTo(Calendar a_validFrom, int a_validityInYears)
	{
		if (a_validFrom == null)
		{
			return null;
		}

		Calendar validTo = (Calendar) a_validFrom.clone();

		if (a_validityInYears < 0)
		{
			validTo.add(Calendar.MINUTE, TEMPORARY_VALIDITY_IN_MINUTES);
		}
		else
		{
			validTo.add(Calendar.YEAR, a_validityInYears);
		}

		return validTo;
	}

}
