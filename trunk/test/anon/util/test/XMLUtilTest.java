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
package anon.util.test;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * These are the tests for the class XMLUtil.
 */
import junitx.framework.extension.XtendedPrivateTestCase;

import anon.util.XMLUtil;

/**
 * These are the tests for the XMLUtil class.
 * @author Rolf Wendolsky
 */
public class XMLUtilTest extends XtendedPrivateTestCase
{

	public XMLUtilTest(String a_strName)
	{
		super(a_strName);
	}

	protected void setUp() throws Exception
	{

	}

	protected void tearDown() throws Exception
	{

	}

	/**
	 * Test if an xml document can successfully be written to a file.
	 * @exception Exception if an error occurs
	 */
	public void testWriteToFile() throws Exception
	{
		// we use the functions from XMLUtil to write to file
		writeXMLOutputToFile(new DummyXMLEncodable());
	}

	/**
	 * Test if the deep search method is successful.
	 */
	public void testGetFirstChildByNameUsingDeepSearch()
	{
		DummyXMLEncodable dummy = new DummyXMLEncodable();
		Element element;
		Node node;

		element = XMLUtil.toXMLElement(dummy);

		node = XMLUtil.getFirstChildByNameUsingDeepSearch(element, "TextNode");
		assertNotNull(node);

		node = XMLUtil.getFirstChildByNameUsingDeepSearch(element, "ContainerNode");
		assertNotNull(node);

		node = XMLUtil.getFirstChildByNameUsingDeepSearch(element, "NumberNode");
		assertNotNull(node);
	}

	/**
	 * Test if a node can successfully be transormed to a byte array and back.
	 * @throws Exception if an error occurs
	 */
	public void testToByteArray() throws Exception
	{
		DummyXMLEncodable dummyOriginal, dummyCopy;
		byte[] xml;

		// create a byte array from an xml element
		dummyOriginal = new DummyXMLEncodable();
		xml = XMLUtil.toByteArray(XMLUtil.toXMLElement(dummyOriginal));

		// create a new instance from the byte array
		dummyCopy = new DummyXMLEncodable(XMLUtil.toXMLDocument(xml).getDocumentElement());
		assertTrue(dummyOriginal.equals(dummyCopy));

		// --

		// create a byte array from an xml document
		dummyOriginal = new DummyXMLEncodable();
		xml = XMLUtil.toByteArray(XMLUtil.toXMLDocument(dummyOriginal));

		// create a new instance from the byte array
		dummyCopy = new DummyXMLEncodable(XMLUtil.toXMLDocument(xml).getDocumentElement());
		assertTrue(dummyOriginal.equals(dummyCopy));
	}
}
