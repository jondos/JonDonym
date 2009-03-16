/*
 Copyright (c) 2000 - 2004, The JAP-Team
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

import junitx.framework.extension.XtendedPrivateTestCase;

import anon.infoservice.InfoServiceDBEntry;
import anon.infoservice.ListenerInterface;
import anon.util.XMLUtil;

/**
 * Tests the class InfoServiceDBEntry.
 * @author Wendolsky
 */
public class InfoServiceDBEntryTest extends XtendedPrivateTestCase
{
	public InfoServiceDBEntryTest(String a_strName)
	{
		super(a_strName);
	}

	/**
	 * Tests if an object can be successfully written and read from xml.
	 * @throws Exception if an error occurs
	 */
	public void testToXML() throws Exception
	{
		InfoServiceDBEntry dbEntry, dbEntryFromXML;

		dbEntry = new InfoServiceDBEntry("127.0.0.1", 50);
		assertTrue(dbEntry.isUserDefined());
		dbEntryFromXML = new InfoServiceDBEntry(XMLUtil.toXMLElement(dbEntry));

		//dbEntry.

		writeXMLOutputToFile(dbEntry, true);
	}
}
