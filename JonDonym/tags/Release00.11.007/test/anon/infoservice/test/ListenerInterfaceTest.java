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

import org.w3c.dom.Document;

import anon.infoservice.ListenerInterface;
import anon.util.XMLUtil;

/**
 *  These are the tests for the ListenerInterface class.
 * @author Wendolsky
 */
public class ListenerInterfaceTest extends XtendedPrivateTestCase
{
	public ListenerInterfaceTest(String a_name)
	{
		super(a_name);
	}

	/**
	 * Test if valid and invalid ports are recognized.
	 */
	public void testIsValidPort()
	{
		assertTrue(!ListenerInterface.isValidPort(0));
		assertTrue(!ListenerInterface.isValidPort(-53));
		assertTrue(ListenerInterface.isValidPort(1));
		assertTrue(ListenerInterface.isValidPort(461));
		assertFalse(ListenerInterface.isValidPort(65536));
		assertFalse(ListenerInterface.isValidPort(65537));
	}

	/**
	 * Test if valid and invalid protocols are recognized.
	 */
	public void testIsValidProtocol()
	{
		assertTrue(!ListenerInterface.isValidProtocol(null));
		assertTrue(!ListenerInterface.isValidProtocol(""));
		assertTrue(!ListenerInterface.isValidProtocol("ftp"));
		assertTrue(!ListenerInterface.isValidProtocol("HTTP"));
		assertTrue(!ListenerInterface.isValidProtocol("http"));
		assertTrue(ListenerInterface.isValidProtocol(ListenerInterface.PROTOCOL_TYPE_HTTP));
		assertTrue(ListenerInterface.isValidProtocol(ListenerInterface.PROTOCOL_TYPE_HTTPS));
		assertTrue(ListenerInterface.isValidProtocol(ListenerInterface.PROTOCOL_TYPE_SOCKS));
	}

	/**
	 * Test if valid and invalid host names are recognized.
	 */
	public void testIsValidHostname()
	{
		assertTrue(!ListenerInterface.isValidHostname(null));
		assertTrue(!ListenerInterface.isValidHostname(""));
		assertTrue(ListenerInterface.isValidHostname("44t4zhs3loe"));
	}

	/**
	 * Test if valid and invalid IPs are recognized.
	 */
	public void testIsValidIP()
	{
		assertTrue(!ListenerInterface.isValidIP(null));
		assertTrue(!ListenerInterface.isValidIP(""));
		assertTrue(!ListenerInterface.isValidIP("rttrhrt543"));
		assertTrue(!ListenerInterface.isValidIP("123"));
		assertTrue(ListenerInterface.isValidIP("0.0.0.0"));
		assertTrue(!ListenerInterface.isValidIP("127.-5.2.1"));
		assertTrue(ListenerInterface.isValidIP("127.5.2.1"));
		assertTrue(!ListenerInterface.isValidIP("127.5.2.1.5"));
		assertTrue(!ListenerInterface.isValidIP("234.345.126.119"));
		assertTrue(ListenerInterface.isValidIP(
				  "234.56.126.119.4.83.67.178.42.0.0.23.234.64.34.82"));
	}

	/**
	 * Test if an object can be successfully instanciated.
	 */
	public void testConstructor()
	{
		ListenerInterface lf;

		lf = new ListenerInterface("68.43.14.98", 443, ListenerInterface.PROTOCOL_TYPE_SOCKS);
		assertEquals("68.43.14.98", lf.getHost());
		assertEquals(443, lf.getPort());
		assertEquals(ListenerInterface.PROTOCOL_TYPE_SOCKS, lf.getProtocol());

		new ListenerInterface("127.0.0.1", 80, 0);
		new ListenerInterface("127.0.0.1", 3000, ListenerInterface.PROTOCOL_TYPE_SOCKS);
		new ListenerInterface("127.0.256.1", 3000, ListenerInterface.PROTOCOL_TYPE_SOCKS);
		new ListenerInterface("127.0.255.1", 3000, ListenerInterface.PROTOCOL_TYPE_SOCKS);

		// illegal port
		assertTrue(!(new ListenerInterface(
				  "127.0.0.1", -80, ListenerInterface.PROTOCOL_TYPE_SOCKS).isValid()));
		// illegal host name
		assertTrue(!(new ListenerInterface(
				  null, 80, ListenerInterface.PROTOCOL_TYPE_SOCKS).isValid()));
		// illegal protocol
		assertTrue((new ListenerInterface("myhost", 80, 0).isValid()));
	}

	/**
	 * Tests if a ListenerInterface can be succesfully converted in XML and reconverted
	 * to a ListenerInterface with the same values.
	 * Also, a structure of the xml content is created.
	 * @throws Exception
	 */
	public void testToXMLNode() throws Exception
	{
		ListenerInterface listenerOrigin, listenerFromXML;
		Document doc;

		doc = XMLUtil.createDocument();
		listenerOrigin = new ListenerInterface("127.0.0.1", 443, ListenerInterface.PROTOCOL_TYPE_HTTPS);
		listenerFromXML = new ListenerInterface(listenerOrigin.toXmlElement(doc));
		assertTrue(listenerOrigin.equals(listenerFromXML));

		// create a structure file
		writeXMLOutputToFile(listenerOrigin);
	}

	/**
	 * Tests if two objects can be correctly compared.
	 */
	public void testEquals()
	{
		ListenerInterface one, two, three, four;

		one = new ListenerInterface("myhost", 80);
		two = new ListenerInterface("myhost", 80, ListenerInterface.PROTOCOL_TYPE_HTTP);
		three = new ListenerInterface("otherhost", 80);
		four = new ListenerInterface("myhost", 80, ListenerInterface.PROTOCOL_TYPE_SOCKS);

		assertTrue(one.equals(one));
		assertTrue(one.equals(two));
		assertTrue(!one.equals(three));
		assertTrue(!one.equals(four));
		assertTrue(!two.equals(three));
		assertTrue(!two.equals(four));
		assertTrue(!three.equals(four));
		assertTrue(four.equals(four));
	}

	/**
	 * Tests if an object can succesfully be invalidated.
	 */
	public void testInvalidate()
	{
		ListenerInterface li = new ListenerInterface("testhost", 1);
		assertTrue(li.isValid());
		li.setUseInterface(false);
		assertTrue(!li.isValid());
	}
}
