/*
 Copyright (c) 2000 - 2004 The JAP-Team
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
package anon.infoservice.test;

import junit.framework.Test;
import junit.framework.TestSuite;
import anon.infoservice.TestProxy;

public class AllTests
{
	/**
	 * The proxy is not used here, but it must be imported for automatic compilation.
	 */
	TestProxy m_proxy;

	/**
	 * The main function.
	 *
	 * @param a_Args (no arguments needed)
	 */
	public static void main(String[] a_Args)
	{
		junit.swingui.TestRunner.run(AllTests.class);
	}

	/**
	 * Returns the test suite that combines all other tests of the project.
	 *
	 * @return Test The test suite that combines all other tests of the project.
	 */
	public static Test suite()
	{
		TestSuite suite = new TestSuite(AllTests.class.getName());
		//suite.addTestSuite(DatabaseStaticTest.class);
		suite.addTestSuite(DatabaseTest.class);
		suite.addTestSuite(HTTPConnectionFactoryTest.class);
		suite.addTestSuite(ListenerInterfaceTest.class);
		suite.addTestSuite(ProxyInterfaceTest.class);
		suite.addTestSuite(ServiceSoftwareTest.class);
		suite.addTestSuite(InfoServiceDBEntryTest.class);
		return suite;
	}
}