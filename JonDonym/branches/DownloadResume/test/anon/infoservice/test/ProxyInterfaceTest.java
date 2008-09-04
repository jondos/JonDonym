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

import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import junitx.framework.extension.XtendedPrivateTestCase;
import anon.infoservice.ProxyInterface;

/**
 *  These are the tests for the ProxyInterface class.
 * @author Wendolsky
 */
public class ProxyInterfaceTest extends XtendedPrivateTestCase
{
	/**
	 * Creates a new instance of this test.
	 * @param a_name the name of this test
	 */
	public ProxyInterfaceTest(String a_name)
	{
		super(a_name);
	}

	/**
	 * Tests if an object of this class can be created.
	 */
	public void testCreation()
	{
		new ProxyInterface("myhost2", 863, ProxyInterface.PROTOCOL_TYPE_HTTPS, null);
		new ProxyInterface("myhost", 1, ProxyInterface.PROTOCOL_TYPE_HTTP, "Jupp",
						   new DummyPasswordReader(), true, true);

		// authentication is not used, therefore we have no problem here
		new ProxyInterface("myhost", 1, ProxyInterface.PROTOCOL_TYPE_HTTPS, "Jupp", null, false, true);


		new ProxyInterface(null, 1, ProxyInterface.PROTOCOL_TYPE_HTTPS, "Jupp",
							   new DummyPasswordReader(), false, true);

		// illegal host name
		assertTrue(!(new ProxyInterface(
				  null, 1, ProxyInterface.PROTOCOL_TYPE_HTTP, "Jupp",
				  new DummyPasswordReader(), false, true).isValid()));

		// no authentication possible for this protocol type
		new ProxyInterface("myhost", 1, ProxyInterface.PROTOCOL_TYPE_SOCKS, "Jupp",
						   new DummyPasswordReader(), true, true);
	}

	/**
	 * Tests if authentication mode is set correctly.
	 */
	public void testSetUseAuthentication()
	{
		ProxyInterface proxyInterface;
		DummyPasswordReader pwr = new DummyPasswordReader();

		// set the password
		pwr.setPassword("secretpassss");
		proxyInterface =
			new ProxyInterface("myHost",3414, ProxyInterface.PROTOCOL_TYPE_HTTP, "Max", pwr, false, true);

		assertTrue(!proxyInterface.isAuthenticationUsed());
		proxyInterface.setUseAuthentication(true);
		assertTrue(proxyInterface.isAuthenticationUsed());
		proxyInterface.setUseAuthentication(false);
		assertTrue(!proxyInterface.isAuthenticationUsed());
	}

	/**
	 * Tests if the authorization header checks if authentication mode is enabled.
	 */
	public void testGetProxyAuthorizationHeader()
	{
		ProxyInterface proxyInterface =
			new ProxyInterface("aHost", 952, ProxyInterface.PROTOCOL_TYPE_HTTP, "Bill",
							   new DummyPasswordReader(), false, true);

		try
		{
			proxyInterface.getProxyAuthorizationHeader();
			fail();
		}
		catch (IllegalStateException a_e)
		{
		}

		try
		{
			proxyInterface.getProxyAuthorizationHeaderAsString();
			fail();
		}
		catch (IllegalStateException a_e)
		{
		}

		proxyInterface.setUseAuthentication(true);
		proxyInterface.getProxyAuthorizationHeader();
		proxyInterface.getProxyAuthorizationHeaderAsString();
	}

	/**
	 * Tests if the password is set and read correctly.
	 */
	public void testGetAuthenticationPassword()
	{
		ProxyInterface proxyInterface;
		DummyPasswordReader pwr = new DummyPasswordReader();

		// get the password
		pwr.setPassword("myPass123");
		proxyInterface = new ProxyInterface("aHost", 952, pwr);
		assertTrue(proxyInterface.getAuthenticationPassword().equals(pwr.readPassword(proxyInterface)));
		assertTrue(proxyInterface.getAuthenticationPassword().equals("myPass123"));

		// without changing something else, the password doesn`t change
		pwr.setPassword("otherpass456");
		assertTrue(!pwr.isRead());
		assertTrue(!proxyInterface.getAuthenticationPassword().equals(pwr.readPassword(proxyInterface)));
		assertTrue(!proxyInterface.getAuthenticationPassword().equals("otherpass456"));


		// after changing the user id, another password must be set automatically
		proxyInterface.setAuthenticationUserID("Bob");
		assertTrue(pwr.isRead());
		assertTrue(proxyInterface.getAuthenticationPassword().equals(pwr.readPassword(proxyInterface)));
	}

	/**
	 * Tests if a ProxyInterface can be succesfully converted in XML and reconverted
	 * to a ProxyInterface with the same values.
	 * Also, a structure of the xml content is created.
	 * @throws Exception
	 */
	public void testToXMLNode()
		throws Exception
	{
		ProxyInterface interfaceOrigin, interfaceFromXML;
		Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();

		interfaceOrigin = new ProxyInterface(
				  "localhost", 525, ProxyInterface.PROTOCOL_TYPE_HTTP, "Mike",
				  new DummyPasswordReader(), true, true);
		interfaceFromXML = new ProxyInterface(interfaceOrigin.toXmlElement(doc), null);
		// as we have no password reader, authentication cannot be used
		assertTrue(!interfaceOrigin.equals(interfaceFromXML));
		assertEquals(!interfaceOrigin.isAuthenticationUsed(), interfaceFromXML.isAuthenticationUsed());

		interfaceFromXML = new ProxyInterface(interfaceOrigin.toXmlElement(doc), new DummyPasswordReader());
		// we have a password reader!
		assertTrue(interfaceOrigin.equals(interfaceFromXML));
		assertEquals(interfaceOrigin.isAuthenticationUsed(), interfaceFromXML.isAuthenticationUsed());

		// change some values and try if they are still equal
		interfaceOrigin.setUseAuthentication(false);
		interfaceOrigin.setAuthenticationUserID("Bob");
		interfaceFromXML = new ProxyInterface(interfaceOrigin.toXmlElement(doc), new DummyPasswordReader());
		assertTrue(interfaceOrigin.equals(interfaceFromXML));
		assertEquals(interfaceOrigin.isAuthenticationUsed(), interfaceFromXML.isAuthenticationUsed());


		// create a structure file
		writeXMLOutputToFile(interfaceOrigin);
	}
}
