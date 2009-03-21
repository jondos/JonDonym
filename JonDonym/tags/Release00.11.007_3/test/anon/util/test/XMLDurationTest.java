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
package anon.util.test;

/**
 * These are the tests for the class XMLUtil.
 */
import junitx.framework.extension.XtendedPrivateTestCase;

import anon.util.XMLDuration;
import anon.util.XMLParseException;

/**
 * @author Rolf Wendolsky
 */
public class XMLDurationTest extends XtendedPrivateTestCase
{

	public XMLDurationTest(String a_strName)
	{
		super(a_strName);
	}

	public void testEmptyConstructor() throws Exception
	{
		XMLDuration duration = new XMLDuration();
		assertEquals(0, duration.getYears());
		assertEquals(0, duration.getMonths());
		assertEquals(0, duration.getDays());
		assertEquals(0, duration.getHours());
		assertEquals(0, duration.getMinutes());
		assertEquals(0.0, duration.getSeconds(), 0.01);
	}
	
	public void testSimpleFormats() throws Exception
	{
		XMLDuration duration;
		
		duration = new XMLDuration("P6M");
		assertEquals(6, duration.getMonths());
		assertEquals(0, duration.getYears());
		try
		{
			duration.getXMLSchemaType();
			fail("This is no date/time!");
		}
		catch (IllegalStateException a_e)
		{
			// ignore
		}
		assertEquals(1, duration.getSign());
		
		duration = new XMLDuration("-P1Y");
		assertEquals(0, duration.getMonths());
		assertEquals(1, duration.getYears());
		assertEquals(-1, duration.getSign());
		
		duration = new XMLDuration("P1Y8M");
		assertEquals(8, duration.getMonths());
		assertEquals(1, duration.getYears());
		assertEquals(XMLDuration.DURATION_YEARMONTH, duration.getXMLSchemaType());

		try
		{
			duration = new XMLDuration("P1Y2MT");
			fail("Format not allowed!");
		}
		catch (XMLParseException a_e)
		{
			// ignore
		}

		try
		{
			duration = new XMLDuration("PT2Y");
			fail("Format not allowed!");
		}
		catch (XMLParseException a_e)
		{
			// ignore
		}

	}

	public void testAdvancedFormats() throws Exception
	{
		XMLDuration duration;

		duration = new XMLDuration("P10Y20M200D");
		assertEquals(10, duration.getYears());
		assertEquals(20, duration.getMonths());
		assertEquals(200, duration.getDays());


		duration = new XMLDuration("PT12H10M60.556S");
		assertEquals(0, duration.getYears());
		assertEquals(12, duration.getHours());
		assertEquals(10, duration.getMinutes());
		assertEquals(60.556, duration.getSeconds(), 0.01);

		duration = new XMLDuration("P6DT26H0M303.111S");
		assertEquals(6, duration.getDays());
		assertEquals(26, duration.getHours());
		assertEquals(0, duration.getMinutes());
		assertEquals(303.111, duration.getSeconds(), 0.01);
		assertEquals(XMLDuration.DURATION_DAYTIME, duration.getXMLSchemaType());
	}

	public void testComparison() throws Exception
	{
		XMLDuration duration;

		duration = new XMLDuration("P1Y12M123D");
		assertEquals(duration.getXMLSchema(), XMLDuration.EQUAL, duration.compare(new XMLDuration(duration)));


		assertEquals(XMLDuration.EQUAL, new XMLDuration("P10Y20M200D").compare(new XMLDuration("P12Y2M22D")));
		assertEquals(XMLDuration.EQUAL, new XMLDuration("P2DT5S").compare(new XMLDuration("PT23H1498M125S")));

		assertEquals(XMLDuration.GREATER, new XMLDuration("P4Y12M30DT60S").compare(new XMLDuration("P5Y1M1DT80S")));
	}

	public void testToString() throws Exception
	{
		assertEquals("P2Y7M22DT7H0M8S", new XMLDuration("P2Y7M22DT7H8S").toString());
	}
}
