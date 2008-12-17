/*
Copyright (c) 2008 The JAP-Team, JonDos GmbH

All rights reserved.

Redistribution and use in source and binary forms, with or without modification, 
are permitted provided that the following conditions are met:

    * Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
    * Redistributions in binary form must reproduce the above copyright notice,
       this list of conditions and the following disclaimer in the documentation and/or
       other materials provided with the distribution.
    * Neither the name of the University of Technology Dresden, Germany, nor the name of
       the JonDos GmbH, nor the names of their contributors may be used to endorse or
       promote products derived from this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
"AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE REGENTS OR
CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package anon.proxy;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * JUnit-Testcase for the method validityCheck of the HTTPProxyCallback
 */
public class HTTPValidityCheckerTest extends TestCase 
{
	public HTTPValidityCheckerTest() 
	{
		super();
	}

	public HTTPValidityCheckerTest(String name) 
	{
		super(name);
	}


	static class ValidityAssertion
	{
		String str = "";
		boolean valid = false;
		
		public ValidityAssertion(String str, boolean valid) 
		{
			this.str = str;
			this.valid = valid;
		}
		
		public String toString()
		{
			return str;
		}
	}
	
	private final static ValidityAssertion[] headerTestAssertions = 
		new ValidityAssertion[]
 		{
 				new ValidityAssertion(
 						"GET https://localhost1/getIP?ip=blablabla\nUser-agent: Mozilla\r\n",
 						false),
 				new ValidityAssertion(
 						"GET https://localhost2/getIP?ip=blablabla\r\nUser-agent: Mozilla\r\n",
 						true),
 				new ValidityAssertion(
 						"GET https://localhost3/getIP?ip=blablabla\r\nUser-agent: Mozilla\r",
 						true),
				new ValidityAssertion(
 						"GET https://localhost4/getIP?ip=blablabla\r\nUser-agent: Mozilla\n",
 						false),
 				new ValidityAssertion(
 						"GET https://localhost4/getIP?ip=blablabla\r\nUser-agent: Mozilla\n\r",
 						false),
				new ValidityAssertion(
 						"GET https://localhost3/getIP?ip=blablabla\r\nUser-agent: Mozilla\r\r\r",
 						false),	 		
 		};
	
	public static Test[] createAllValidityTests()
	{
		Test[] allTests = new Test[headerTestAssertions.length];
		for (int i = 0; i < headerTestAssertions.length; i++) 
		{
			final ValidityAssertion assi = headerTestAssertions[i];
			allTests[i] =
				new HTTPValidityCheckerTest("validityCheck "+(i+1)) 
				{
					public void runTest()
					{
						checkValidityTest(assi);
					}
				};
		}
		return allTests;
	}
	
	public void checkValidityTest(ValidityAssertion assi)
	{
		if(assi.valid)
		{
			assertTrue(HTTPProxyCallback.checkValidity(assi.str));
		}
		else
		{
			assertFalse(HTTPProxyCallback.checkValidity(assi.str));
		}
	}
	
	public static Test suite()
	{
		Test[] allTests = createAllValidityTests();
		TestSuite suite = new TestSuite(HTTPValidityCheckerTest.class.getName());
		for (int i = 0; i < allTests.length; i++) 
		{
			suite.addTest(allTests[i]);
		}
		return suite;
	}
}
