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
package junitx.framework.extension;

import java.io.File;
import java.util.Enumeration;
import java.util.Vector;

import org.w3c.dom.Comment;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import anon.util.ClassUtil;
import anon.util.IXMLEncodable;
import anon.util.XMLUtil;
import junitx.framework.PrivateTestCase;

/**
 * Extends the PrivateTestCase with useful functions and should be used instead of it.
 * @author Rolf Wendolsky
 */
public class XtendedPrivateTestCase extends PrivateTestCase
{
	/**
	 * A file that can be written and read for testing purposes. Should be deleted after
	 * all test operations.
	 */
	public static final File TEST_FILE = new File("documentation/~testfile~");

	private static final String XML_STRUCTURE_PATH = "documentation/xmlStructures/";

	/**
	 * Creates a new test case.
	 * @param a_name the name of the test case
	 */
	public XtendedPrivateTestCase(String a_name)
	{
		super(a_name);
	}

	public static void assertContains(Vector a_expected, Vector a_actual)
	{
		Enumeration expectedElements;
		Enumeration actualElements;
		boolean bFound;
		Object currentExpectedElement;
		Object currentActualElement;

		expectedElements = a_expected.elements();
		actualElements = a_actual.elements();
		while (expectedElements.hasMoreElements())
		{
			currentExpectedElement = expectedElements.nextElement();

			bFound = false;
			while (actualElements.hasMoreElements())
			{
				currentActualElement = actualElements.nextElement();
				if (currentActualElement.equals(currentExpectedElement))
				{
					bFound = true;
					break;
				}
			}

			if (!bFound)
			{
				fail("The element '" + currentExpectedElement.toString() +
					 "' was not found! Found: " +
					 a_actual.toString());
			}
		}
	}

	/**
	 * Tests if all expected Strings are contained in the actual Strings in the same order.
	 * @param a_expected Vector
	 * @param a_actual Vector
	 */
	public static void assertContainsString(Vector a_expected, Vector a_actual)
	{
		Enumeration expectedStrings;
		Enumeration actualStrings;
		boolean bFound;
		String currentExpectedString;
		String currentActualString;

		expectedStrings = a_expected.elements();
		actualStrings = a_actual.elements();
		while (expectedStrings.hasMoreElements())
		{
			currentExpectedString = (String) expectedStrings.nextElement();
			bFound = false;
			while (actualStrings.hasMoreElements())
			{
				currentActualString = (String) actualStrings.nextElement();
				if (currentActualString.indexOf(currentExpectedString) >= 0)
				{
					bFound = true;
					break;
				}
			}

			if (!bFound)
			{
				fail("The element '" + currentExpectedString + "' was not found! Found: " +
					 a_actual.toString());
			}
		}
	}

	/**
	 * Writes an xml document to a file in the XML_STRUCTURE_PATH with the filename
	 * <testclass>_<a_sequenceNumber>.xml.
	 * @param a_doc an XML document
	 * @param a_sequenceNumber a sequence number; only one XML element can be written with the
	 * same sequence number;
	 * @return the file it was written to
	 * @exception Exception if an error occurs
	 */
	protected File writeXMLOutputToFile(Document a_doc, int a_sequenceNumber)
		throws Exception {
		return writeXMLOutputToFile(a_doc.getDocumentElement(), a_sequenceNumber);
	}


	/**
	 * Writes an xml element to a file in the XML_STRUCTURE_PATH with the filename
	 * <testclass>_<a_sequenceNumber>.xml.
	 * @param a_xmlElement an XML element
	 * @param a_sequenceNumber a sequence number; only one XML element can be written with the
	 * same sequence number;
	 * @return the file it was written to
	 * @exception Exception if an error occurs
	 */
	protected File writeXMLOutputToFile(Element a_xmlElement, int a_sequenceNumber)
		throws Exception {
		return writeXMLOutputToFile(a_xmlElement, getClass(),
									ClassUtil.getShortClassName(getClass()),
									a_sequenceNumber);
	}

	/**
	 * Writes an xml node to a file in the XML_STRUCTURE_PATH with the filename <class>.xml.
	 * @param a_XmlCreaterObject the object that created the node
	 * @param a_bLongClassName if true, the filename will be the class name plus package,
	 *                         if false, it will be the classname only
	 * @return the file it was written to
	 * @throws Exception if an error occurs
	 */
	protected File writeXMLOutputToFile(IXMLEncodable a_XmlCreaterObject, boolean a_bLongClassName)
		throws Exception
	{
		return writeXMLOutputToFile(a_XmlCreaterObject, -1, a_bLongClassName);
	}


	/**
	 * Writes an xml node to a file in the XML_STRUCTURE_PATH with the filename <class>.xml.
	 * @param a_XmlCreaterObject the object that created the node
	 * @param a_sequenceNumber a sequence number; only one XML element can be written with the
	 * same sequence number;
	 * @param a_bLongClassName if true, the filename will be the class name plus package,
	 *                         if false, it will be the classname only
	 * @return the file it was written to
	 * @throws Exception if an error occurs
	 */
	protected File writeXMLOutputToFile(IXMLEncodable a_XmlCreaterObject, int a_sequenceNumber,
										boolean a_bLongClassName)
		throws Exception
	{
		Element element;
		String filename;
		Document doc = XMLUtil.createDocument();

		if (a_bLongClassName)
		{
			filename = a_XmlCreaterObject.getClass().getName();
		}
		else
		{
			filename = ClassUtil.getShortClassName(a_XmlCreaterObject.getClass());
		}

		element = a_XmlCreaterObject.toXmlElement(doc);

		return writeXMLOutputToFile(
				  element, a_XmlCreaterObject.getClass(), filename, a_sequenceNumber);
	}


	/**
	 * Writes an xml node to a file in the XML_STRUCTURE_PATH with the filename <class>.xml.
	 * @param a_XmlCreaterObject the object that created the node
	 * @return the file it was written to
	 * @throws Exception if an error occurs
	 */
	protected File writeXMLOutputToFile(IXMLEncodable a_XmlCreaterObject)
		throws Exception
	{
		return writeXMLOutputToFile(a_XmlCreaterObject,false);
	}

	/**
	 * Writes an xml node to a file in the XML_STRUCTURE_PATH with the filename <class>.xml.
	 * @param a_XmlCreaterObject the object that created the node
	 * @param a_sequenceNumber a sequence number; only one XML element can be written with the
	 * same sequence number;
	 * @return the file it was written to
	 * @throws Exception if an error occurs
	 */
	protected File writeXMLOutputToFile(IXMLEncodable a_XmlCreaterObject, int a_sequenceNumber)
		throws Exception
	{
		return writeXMLOutputToFile(a_XmlCreaterObject, a_sequenceNumber, false);
	}



	private File writeXMLOutputToFile(Element a_xmlElement, Class a_creatorClass, String a_filename,
									  int a_sequenceNumber)
		throws Exception {
		File file;
		String sequenceNumber = "";
		Comment comment1, comment2;
		Document doc = XMLUtil.createDocument();
		Element element = (Element)XMLUtil.importNode(doc, a_xmlElement, true);

		if (a_sequenceNumber >= 0) {
			sequenceNumber = "_" + a_sequenceNumber;
		}

		// set a comment
		comment1 = doc.createComment("This xml structure has been created by " +
				  a_creatorClass.getName() + ".");
		comment2 = doc.createComment( "The calling test class was " + getClass().getName() + ".");
		//doc.appendChild(comment);
		//doc.appendChild(doc.createComment("\n"));

		element.insertBefore(comment2, element.getFirstChild());
		element.insertBefore(comment1, element.getFirstChild());

		doc.appendChild(element);

		// write to file
		file = new File(XML_STRUCTURE_PATH + a_filename + sequenceNumber + ".xml");
		XMLUtil.write(doc, file);
		return file;
	}



}
