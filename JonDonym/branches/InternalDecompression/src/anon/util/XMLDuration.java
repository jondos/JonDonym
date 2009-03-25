/*
 Copyright (c) 2009, The JAP-Team, JonDos GmbH
 All rights reserved.
 Redistribution and use in source and binary forms, with or without modification,
 are permitted provided that the following conditions are met:

  - Redistributions of source code must retain the above copyright notice,
 this list of conditions and the following disclaimer.

  - Redistributions in binary form must reproduce the above copyright notice,
 this list of conditions and the following disclaimer in the documentation and/or
 other materials provided with the distribution.

  - Neither the name of the University of Technology Dresden, Germany, nor the name of
 the JonDos GmbH, nor the names of their contributors may be used to endorse or
 promote products derived from this software without specific prior written permission.


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

import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Enumeration;
import java.util.Vector;


/**
 * <p>Immutable representation of a time span as defined in
 * the W3C XML Schema 1.0 specification.</p>
 * 
 * <p>A Duration object represents a period of Gregorian time,
 * which consists of six fields (years, months, days, hours,
 * minutes, and seconds) plus a sign (+/-) field.</p>
 * 
 * <p>The first five fields have non-negative (>=0) integers or null
 * (which represents that the field is not set),
 * and the seconds field has a non-negative decimal or null.
 * A negative sign indicates a negative duration.</p> 
 * 
 * <p>This class provides a number of methods that make it easy
 * to use for the duration datatype of XML Schema 1.0 with
 * the errata.</p>
 * 
 * <h2>Order relationship</h2>
 * <p>Duration objects only have partial order, where two values A and B
 * maybe either:</p>
 * <ol>
 *  <li>A&lt;B (A is shorter than B)
 *  <li>A&gt;B (A is longer than B)
 *  <li>A==B   (A and B are of the same duration)
 *  <li>A&lt;>B (Comparison between A and B is indeterminate)
 * </ol>

 *  * <p>For example, 30 days cannot be meaningfully compared to one month.
 * The {@link #compare(Duration duration)} method implements this
 * relationship.</p>
 * 
 * <p>See the {@link #isLongerThan(Duration)} method for details about
 * the order relationship among <code>Duration</code> objects.</p>
 * 
 */
public class XMLDuration 
{
	public static final int DURATION = 0;
	public static final int DURATION_DAYTIME = 1;
	public static final int DURATION_YEARMONTH = 2;
	
	public static final int LESSER = -1;
	public static final int EQUAL =  0;
	public static final int GREATER =  1;
	public static final int INDETERMINATE =  2;
	
	
	private static final int YEARS = 1;
	private static final int MONTHS = 2;
	private static final int DAYS = 3;
	private static final int HOURS = 4;
	private static final int MINUTES = 5;
	private static final int SECONDS = 6;
	
	private static final String[] NAMES = new String[]{
		"years", "months", "days", "hours", "minutes", "seconds"
	};
	
	private long m_years;
	private long m_months;
	private long m_days;
	private long m_hours;
	private long m_minutes;
	private double m_seconds;
	private boolean m_bNegativeSign;
	private String m_theDuration;
	
	private Vector m_setFields;
	
	private long m_calcYears;
	private long m_calcMonths;
	private long m_calcDays;
	private long m_calcHours;
	private long m_calcMinutes;
	private double m_calcSeconds;
	private int m_hashCode;
	
	public XMLDuration() 
	{
		m_years = 0;
		m_months = 0;
		m_days = 0;
		m_hours = 0;
		m_minutes = 0;
		m_seconds = 0;
		m_bNegativeSign = false;
		m_theDuration = "P0Y";
		
		m_setFields = new Vector();
		m_setFields.addElement(new Integer(YEARS));
		
		init();
	}
	
	
	public XMLDuration(XMLDuration a_duration)
	{
		if (a_duration == null)
		{
			throw new NullPointerException();
		}
		
		m_years = a_duration.m_years;
		m_months = a_duration.m_months;
		m_days = a_duration.m_days;
		m_hours = a_duration.m_hours;
		m_minutes = a_duration.m_minutes;
		m_seconds = a_duration.m_seconds;
		m_bNegativeSign = a_duration.m_bNegativeSign;
		m_theDuration = a_duration.m_theDuration;
		
		m_setFields = a_duration.m_setFields;
		
		init();
	}
	
	public XMLDuration(String a_duration) throws XMLParseException
	{
		String strDuration;
		
		if (a_duration == null)
		{
			throw new XMLParseException(XMLParseException.NODE_NULL_TAG);
		}
		m_theDuration = a_duration;
		
		if (a_duration.length() == 0)
		{
			// there is nothing to parse
			return;
		}
		
		a_duration = a_duration.trim();
		
		if (a_duration.length() < 3)
		{
			throw new XMLParseException("Duration string is too short to parse: " + m_theDuration);
		}
		
		if (a_duration.startsWith("-"))
		{
			m_bNegativeSign = true;
			a_duration = a_duration.substring(1, a_duration.length());
		}
		
		if (!a_duration.startsWith("P"))
		{
			throw new XMLParseException("Duration string has invalid format: " + m_theDuration);
		}
		
		m_setFields = new Vector();
		
		a_duration = a_duration.substring(1, a_duration.length());
		strDuration = a_duration;
		
		
		a_duration = parseXMLSchemaPart(YEARS, a_duration);
		a_duration = parseXMLSchemaPart(MONTHS, a_duration);
		a_duration = parseXMLSchemaPart(DAYS, a_duration);
		
		
		if (a_duration.startsWith("T"))
		{
			a_duration = a_duration.substring(1, a_duration.length());
			strDuration = a_duration;
			a_duration = parseXMLSchemaPart(HOURS, a_duration);
			a_duration = parseXMLSchemaPart(MINUTES, a_duration);
			a_duration = parseXMLSchemaPart(SECONDS, a_duration);
		}
		else if (a_duration.length() > 0)
		{
			throw new XMLParseException("Duration string has invalid format (T): " + m_theDuration);
		}
		
		if (a_duration.equals(strDuration))
		{
			// Nothing was parsed!
			throw new XMLParseException("Duration string has invalid format: " + m_theDuration);
		}
		
		init();
	}
	
	private void setField(int a_field, Number a_value)
	{
		if (a_field == YEARS)
		{
			m_years = a_value.intValue(); 
		}
		else if (a_field == MONTHS)
		{
			m_months = a_value.intValue(); 
		}
		else if (a_field == DAYS)
		{
			m_days = a_value.intValue(); 
		}
		else if (a_field == HOURS)
		{
			m_hours = a_value.intValue(); 
		}
		else if (a_field == MINUTES)
		{
			m_minutes = a_value.intValue(); 
		}
		else if (a_field == SECONDS)
		{
			m_seconds = a_value.doubleValue(); 
		}
	}
	
	private String parseXMLSchemaPart(int a_field, String a_strXMLDurationPart) throws XMLParseException
	{
		int index;
		String schemaChar = "";
		String strValueToParse;
		
		if (a_field == YEARS)
		{
			schemaChar = "Y";
		}
		else if (a_field == MONTHS)
		{
			schemaChar = "M";
		}
		else if (a_field == DAYS)
		{
			schemaChar = "D";
		}
		else if (a_field == HOURS)
		{
			schemaChar = "H";
		}
		else if (a_field == MINUTES)
		{
			schemaChar = "M";
		}
		else if (a_field == SECONDS)
		{
			schemaChar = "S";
		}
			
		
		index = a_strXMLDurationPart.indexOf(schemaChar);
		if (index > 0)
		{
			strValueToParse = a_strXMLDurationPart.substring(0, index);
			
			if (a_field == MONTHS && strValueToParse.indexOf("T") >= 0)
			{
				return a_strXMLDurationPart;
			}
			m_setFields.addElement(new Integer(a_field));
			
			try
			{
				if (a_field == SECONDS)
				{
					setField(a_field, Double.valueOf(strValueToParse));
				}
				else
				{
					setField(a_field, Integer.valueOf(strValueToParse));
				}
				
				if (a_strXMLDurationPart.length() > index)
				{
					a_strXMLDurationPart = a_strXMLDurationPart.substring(index + 1, a_strXMLDurationPart.length());
				}
				else
				{
					a_strXMLDurationPart = "";
				}
			}
			catch (NumberFormatException a_e)
			{
				throw new XMLParseException("Duration string has invalid format (" + schemaChar + ", " + 
						"NumberFormatException: " + a_e.getMessage()  + "): " + 
						m_theDuration);
			}
		}
		return a_strXMLDurationPart;
	}

	public String getXMLSchema()
	{
		return m_theDuration;
	}
	
	/**
	 * <p>Return the name of the XML Schema date/time type that this instance 
	 * maps to. Type is computed based on fields that are set,
	 * i.e. {@link #isSet(int a_field)} == <code>true</code>.</p>
	 *
	 * <table border="2" rules="all" cellpadding="2">
	 *   <thead>
	 *     <tr>
	 *       <th align="center" colspan="7">
	 *         Required fields for XML Schema 1.0 Date/Time Datatypes.<br/>
	 *         <i>(timezone is optional for all date/time datatypes)</i>
	 *       </th>
	 *     </tr>
	 *   </thead>
	 *   <tbody>
	 *     <tr>
	 *       <td>Datatype</td>
	 *       <td>year</td>
	 *       <td>month</td>
	 *       <td>day</td>
	 *       <td>hour</td>
	 *       <td>minute</td>
	 *       <td>second</td>
	 *     </tr>
	 *     <tr>
	 *       <td>DURATION</td>
	 *       <td>X</td>
	 *       <td>X</td>
	 *       <td>X</td>
	 *       <td>X</td>
	 *       <td>X</td>
	 *       <td>X</td>
	 *     </tr>
	 *     <tr>
	 *       <td>DURATION_DAYTIME</td>
	 *       <td></td>
	 *       <td></td>
	 *       <td>X</td>
	 *       <td>X</td>
	 *       <td>X</td>
	 *       <td>X</td>
	 *     </tr>
	 *     <tr>
	 *       <td>URATION_YEARMONTH</td>
	 *       <td>X</td>
	 *       <td>X</td>
	 *       <td></td>
	 *       <td></td>
	 *       <td></td>
	 *       <td></td>
	 *     </tr>
	 *   </tbody>
	 * </table>
	 * 
	 * @return one of the following constants:
	 *   DURATION,
	 *   DURATION_DAYTIME or
	 *   DURATION_YEARMONTH.
	 *  
	 * @throws IllegalStateException If the combination of set fields does not match one of the XML Schema date/time datatypes.
	 */
	public int getXMLSchemaType() throws IllegalStateException {
		
		boolean yearSet = isSet(YEARS);
		boolean monthSet = isSet(MONTHS);
		boolean daySet = isSet(DAYS);
		boolean hourSet = isSet(HOURS);
		boolean minuteSet = isSet(MINUTES);
		boolean secondSet = isSet(SECONDS);
		
		// DURATION
		if (yearSet
			&& monthSet
			&& daySet
			&& hourSet
			&& minuteSet
			&& secondSet) 
		{
			return DURATION;
		}

		// DURATION_DAYTIME
		if (!yearSet
			&& !monthSet
			&& daySet
			&& hourSet
			&& minuteSet
			&& secondSet) 
		{
			return DURATION_DAYTIME;
		}

		// DURATION_YEARMONTH
		if (yearSet
			&& monthSet
			&& !daySet
			&& !hourSet
			&& !minuteSet
			&& !secondSet) 
		{
			return DURATION_YEARMONTH;
		}

		// nothing matches
		throw new IllegalStateException(
			 "This Duration does not match one of the XML Schema date/time datatypes:"
				+ " year set = " + yearSet
				+ " month set = " + monthSet
				+ " day set = " + daySet
				+ " hour set = " + hourSet
				+ " minute set = " + minuteSet
				+ " second set = " + secondSet
		);
	}

	/**
	 * Returns the sign of this duration in -1,0, or 1.
	 * 
	 * @return
	 *      -1 if this duration is negative, 0 if the duration is zero,
	 *      and 1 if the duration is positive.
	 */
	public int getSign()
	{
		if (m_bNegativeSign)
		{
			return -1;
		}
		return 1;
	}


	public long getYears() 
	{
		return m_years;
	}
    
	public long getMonths() 
	{
		return m_months;
	}
    

	public long getDays() 
	{
		return m_days;
	}
    
	public long getHours() 
	{
		return m_hours;
	}
    

	public long getMinutes() 
	{
		return m_minutes;
	}
    

	public double getSeconds() 
	{
		return m_seconds;
	}
    
	
	public static String getFieldName(Object a_field)
	{
		if (a_field == null || !(a_field instanceof Integer))
		{
			return null;
		}
		return getFieldName(((Integer)a_field).intValue());
	}
	
	public static String getFieldName(int a_field)
	{
		if (a_field < YEARS || a_field > SECONDS)
		{
			return null;
		}
		return NAMES[a_field - 1];
	}
	
	public Enumeration getFields()
	{
		return m_setFields.elements();
	}
	
	public Number getField(Object a_field)
	{
		if (a_field == null || !(a_field instanceof Integer))
		{
			return null;
		}
		
		return getField(((Integer)a_field).intValue());
	}
	
	
	/**
	 * Gets the value of a field. 
	 * 
	 * Fields of a duration object may contain arbitrary large value.
	 * Therefore this method is designed to return a {@link Number} object.
	 * 
	 * In case of YEARS, MONTHS, DAYS, HOURS, and MINUTES, the returned
	 * number will be a non-negative integer. In case of seconds,
	 * the returned number may be a non-negative decimal value.
	 * 
	 * @param field
	 *      one of the six Field constants (YEARS,MONTHS,DAYS,HOURS,
	 *      MINUTES, or SECONDS.)
	 * @return
	 *      If the specified field is present, this method returns
	 *      a non-null non-negative {@link Number} object that
	 *      represents its value. If it is not present, return null.
	 *      For YEARS, MONTHS, DAYS, HOURS, and MINUTES, this method
	 *      returns a {@link java.math.BigInteger} object. For SECONDS, this
	 *      method returns a {@link java.math.BigDecimal}. 
	 * 
	 * @throws NullPointerException If the <code>field</code> is <code>null</code>.
	 */
	public Number getField(int a_field)
	{
		if (a_field == YEARS)
		{
			return BigInteger.valueOf(m_years);
		}
		else if (a_field == MONTHS)
		{
			return BigInteger.valueOf(m_months);
		}
		else if (a_field == DAYS)
		{
			return BigInteger.valueOf(m_days);
		}
		else if (a_field == HOURS)
		{
			return BigInteger.valueOf(m_hours);
		}
		else if (a_field == MINUTES)
		{
			return BigInteger.valueOf(m_minutes);
		}
		else if (a_field == SECONDS)
		{
			return new BigDecimal(m_seconds);
		}
		
		return null;
	}
	
	/**
	 * Checks if a field is set.
	 * 
	 * A field of a duration object may or may not be present.
	 * This method can be used to test if a field is present.
	 * 
	 * @param field
	 *      one of the six Field constants (YEARS,MONTHS,DAYS,HOURS,
	 *      MINUTES, or SECONDS.)
	 * @return
	 *      true if the field is present. false if not.
	 */
	public boolean isSet(int a_field)
	{	
		return m_setFields.contains(new Integer(a_field));
	}
	
	
	/**
	 * Returns a new <code>Duration</code> object whose
	 * value is <code>-this</code>.
	 * 
	 * <p>
	 * Since the <code>Duration</code> class is immutable, this method
	 * doesn't change the value of this object. It simply computes
	 * a new Duration object and returns it.
	 * 
	 * @return
	 *      always return a non-null valid <code>Duration</code> object.
	 */
	public XMLDuration negate()
	{
		XMLDuration duration = new XMLDuration(this);
		duration.m_bNegativeSign = !m_bNegativeSign;
		return duration;
	}
	
	
	/**
	 * <p>Partial order relation comparison with this <code>Duration</code> instance.</p>
	 * 
	 * <p>Comparison result must be in accordance with
	 * <a href="http://www.w3.org/TR/xmlschema-2/#duration-order">W3C XML Schema 1.0 Part 2, Section 3.2.7.6.2,
	 * <i>Order relation on duration</i></a>.</p>
	 * 
	 * <p>Return:</p>
	 * <ul>
	 *   <li>LESSER if this <code>Duration</code> is shorter than <code>duration</code> parameter</li>
	 *   <li>EQUAL if this <code>Duration</code> is equal to <code>duration</code> parameter</li>
	 *   <li>GREATER if this <code>Duration</code> is longer than <code>duration</code> parameter</li>
	 *   <li>NDETERMINATE if a conclusive partial order relation cannot be determined</li>
	 * </ul>
	 *
	 * @param duration to compare
	 * 
	 * @return the relationship between <code>this</code> <code>Duration</code>and <code>duration</code> parameter as
	 *   LESSER, EQUAL,GREATER or INDETERMINATE.
	 * 
	 * @throws NullPointerException if <code>duration</code> is <code>null</code>. 
	 *
	 * @see #isShorterThan(Duration)
	 * @see #isLongerThan(Duration)
	 */
	public int compare(XMLDuration a_duration)
	{
		if (m_calcYears > a_duration.m_calcYears)
		{
			return GREATER;
		}
		else if (m_calcYears < a_duration.m_calcYears)
		{
			return LESSER;
		}
		
		if (m_calcMonths > a_duration.m_calcMonths)
		{
			return GREATER;
		}
		else if (m_calcMonths < a_duration.m_calcMonths)
		{
			return LESSER;
		}
		
		if (m_calcDays > a_duration.m_calcDays)
		{
			return GREATER;
		}
		else if (m_calcDays < a_duration.m_calcDays)
		{
			return LESSER;
		}
		
		if (m_calcHours > a_duration.m_calcHours)
		{
			return GREATER;
		}
		else if (m_calcHours < a_duration.m_calcHours)
		{
			return LESSER;
		}
		
		if (m_calcMinutes > a_duration.m_calcMinutes)
		{
			return GREATER;
		}
		else if (m_calcMinutes < a_duration.m_calcMinutes)
		{
			return LESSER;
		}
		
		if (m_calcSeconds > a_duration.m_calcSeconds)
		{
			return GREATER;
		}
		else if (m_calcSeconds < a_duration.m_calcSeconds)
		{
			return LESSER;
		}
		
		return EQUAL;
	}
	
	public boolean equals(Object a_duration)
	{
		if (a_duration == null || !(a_duration instanceof XMLDuration))
		{
			return false;
		}
		
		return compare((XMLDuration)a_duration) == EQUAL;
	}
	
	public int hashCode()
	{
		return m_hashCode;
	}
	
	/**
	 * <p>Checks if this duration object is strictly longer than
	 * another <code>Duration</code> object.</p>
	 * 
	 * <p>Duration X is "longer" than Y if and only if X>Y 
	 * as defined in the section 3.2.6.2 of the XML Schema 1.0
	 * specification.</p>
	 * 
	 * <p>For example, "P1D" (one day) > "PT12H" (12 hours) and
	 * "P2Y" (two years) > "P23M" (23 months).</p> 
	 * 
	 * @param duration <code>Duration</code> to test this <code>Duration</code> against.
	 * 
	 * @throws NullPointerException If <code>duration</code> is null.
	 * 
	 * @return
	 *      true if the duration represented by this object
	 *      is longer than the given duration. false otherwise.
	 * 
	 * @see #isShorterThan(Duration)
	 * @see #compare(Duration duration)
	 */
	public boolean isLongerThan(final XMLDuration a_duration) 
	{
		return compare(a_duration) == GREATER;
	}
    
	/**
	 * <p>Checks if this duration object is strictly shorter than
	 * another <code>Duration</code> object.</p>
	 * 
	 * @param duration <code>Duration</code> to test this <code>Duration</code> against.
	 * 
	 * @return <code>true</code> if <code>duration</code> parameter is shorter than this <code>Duration</code>,
	 *   else <code>false</code>. 
	 * 
	 * @throws NullPointerException if <code>duration</code> is null.
	 *
	 * @see #isLongerThan(Duration duration)
	 * @see #compare(Duration duration)
	 */
	public boolean isShorterThan(final XMLDuration a_duration)
	{
		return compare(a_duration) == LESSER;
	}
	
	public int getLastFieldSet()
	{
		if (isSet(SECONDS))
		{
			return SECONDS;
		}
		else if (isSet(MINUTES))
		{
			return MINUTES;
		}
		else if (isSet(HOURS))
		{
			return HOURS;
		}
		else if (isSet(DAYS))
		{
			return DAYS;
		}
		else if (isSet(MONTHS))
		{
			return MONTHS;
		}
		
		return YEARS;
	}
	
    
	/**
	 * <p>Returns a <code>String</code> representation of this <code>Duration</code> <code>Object</code>.</p>
	 * 
	 * <p>The result is formatted according to the XML Schema 1.0 spec and can be always parsed back later into the
	 * equivalent <code>Duration</code> <code>Object</code>.</p>
	 * 
	 * <p>Formally, the following holds for any <code>Duration</code>
	 * <code>Object</code> x:</p> 
	 * <pre>
	 * new Duration(x.toString()).equals(x)
	 * </pre>
	 * 
	 * @return A non-<code>null</code> valid <code>String</code> representation of this <code>Duration</code>.
	 */
	public String toString() 
	{	
        StringBuffer buf = new StringBuffer();
        
        if (getSign() < 0) 
        {
            buf.append('-');
        }
        buf.append('P');
        
        BigInteger years = (BigInteger) getField(YEARS);
        if (years != null) 
        {
            buf.append(years + "Y");
        }
        
        BigInteger months = (BigInteger) getField(MONTHS);
        if (months != null) 
        {
            buf.append(months + "M");
        }
        
        BigInteger days = (BigInteger) getField(DAYS);
        if (days != null) 
        {
            buf.append(days + "D");
        }

        BigInteger hours = (BigInteger) getField(HOURS);
        BigInteger minutes = (BigInteger) getField(MINUTES);
        BigDecimal seconds = (BigDecimal) getField(SECONDS);
        if (hours != null || minutes != null || seconds != null) 
        {
            buf.append('T');
            if (hours != null) 
            {
                buf.append(hours + "H");
            }
            if (minutes != null) 
            {
                buf.append(minutes + "M");
            }
            if (seconds != null) 
            {
                buf.append(toString(seconds) + "S");
            }
        }
        
        return buf.toString();
	}
	
	private String toString(BigDecimal bd) 
	{
		String result = toStringJDK5(bd);
		if (result == null)
		{
			result = bd.toString();
		}
		return result;
	}
	
    /**
     * <p>Turns {@link BigDecimal} to a string representation.</p>
     * 
     * <p>Due to a behavior change in the {@link BigDecimal#toString()}
     * method in JDK 5, this had to be implemented here.</p>
     * 
     * @param bd <code>BigDecimal</code> to format as a <code>String</code>
     * 
     * @return  <code>String</code> representation of <code>BigDecimal</code> 
     */
    private String toStringJDK5(BigDecimal bd) 
    {
        String intString;
        BigInteger unscaledValue;
       
        try 
        {
			unscaledValue = 
				(BigInteger)(BigDecimal.class.getMethod("unscaledValue", (Class[])null).invoke(bd, (Object[])null));
		} 
        catch (Exception e) 
        {
        	// it seems that we are running under JRE 1.4 or lower
			return null;
		}
        
        intString = unscaledValue.toString();
        
        int scale = bd.scale();

        if (scale == 0) 
        {
            return intString;
        }

        /* Insert decimal point */
        StringBuffer buf;
        int insertionPoint = intString.length() - scale;
        if (insertionPoint == 0) 
        { /* Point goes right before intVal */
            return "0." + intString;
        } else if (insertionPoint > 0) 
        { /* Point goes inside intVal */
            buf = new StringBuffer(intString);
            buf.insert(insertionPoint, '.');
        } else 
        { /* We must insert zeros between point and intVal */
            buf = new StringBuffer(3 - insertionPoint + intString.length());
            buf.append("0.");
            for (int i = 0; i < -insertionPoint; i++) 
            {
                buf.append('0');
            }
            buf.append(intString);
        }
        return buf.toString();
    }
    
    private void init()
    {
    	m_calcSeconds = m_seconds;
    	m_calcMinutes = m_minutes;
    	m_calcHours = m_hours;
    	m_calcDays = m_days;
    	m_calcMonths = m_months;
    	m_calcYears = m_years;
    	
    	m_calcMinutes += ((int)m_calcSeconds / 60);
    	m_calcSeconds -= (double)(((int)m_calcSeconds / 60) * 60);
    	
    	m_calcHours += (m_calcMinutes / 60);
    	m_calcMinutes = (m_calcMinutes % 60);
    	
    	m_calcDays += (m_calcHours / 24);
    	m_calcHours = (m_calcHours % 24);
    	
    	m_calcYears += (m_calcDays / 365);
    	m_calcDays = (m_calcDays % 365);
    	
    	
    	m_calcMonths += 5 * (m_calcDays / 150);
    	m_calcDays = (m_calcDays % 150);
    	
    	
    	m_calcMonths += (m_calcDays / 28);
    	m_calcDays = (m_calcDays % 28);
    	
    	m_calcYears  += (m_calcMonths / 12);
    	m_calcMonths  = (m_calcMonths % 12);
    	
    	m_hashCode = (int)((long)m_calcSeconds + m_calcMinutes + m_calcHours + 
    			m_calcDays + m_calcMonths + m_calcYears) * getSign();
    }
}
