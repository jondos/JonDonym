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
package anon.crypto.test;

import java.security.Key;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import anon.util.IXMLEncodable;
import anon.util.Util;
import anon.util.XMLParseException;
import anon.util.XMLUtil;

public abstract class DummySignatureKey implements Key, IXMLEncodable
{
	public static final String XML_ELEMENT_NAME = "DummyKeyValue";

	private long m_key;

	/**
	 * Creates a new dummy key.
	 * @param a_key the key as long value
	 */
	public DummySignatureKey(long a_key)
	{
		m_key = a_key;
	}

	/**
	 * Creates a new dummy key form an xml signature element.
	 * @param a_element a_key the key as long value
	 * @exception XMLParseException if an error occurs while parsing the XML structure
	 */
	public DummySignatureKey(Element a_element) throws XMLParseException
	{
		if (!a_element.getNodeName().equals(XML_ELEMENT_NAME))
		{
			throw new XMLParseException(XMLParseException.ROOT_TAG, "Illegal element name!");
		}

		try
		{
			m_key = XMLUtil.parseValue(XMLUtil.getFirstChildByName(a_element, "Key"), Long.MAX_VALUE);
		}
		catch (Exception a_e)
		{
			throw new XMLParseException("Key", Util.getStackTrace(a_e));
		}
	}


	/**
	 * Returns the long value of this key.
	 * @return the long value of this key
	 */
	public long getKeyValue()
	{
		return m_key;
	}

	public String getAlgorithm()
	{
		return "Dummy";
	}

	public String getFormat()
	{
		return "X.Dummy";
	}

	public Element toXmlElement(Document a_doc)
	{
		Element elementRoot, element;

		elementRoot = a_doc.createElement(XML_ELEMENT_NAME);
		element = a_doc.createElement("Key");
		XMLUtil.setValue(element, Long.toString(m_key));
		elementRoot.appendChild(element);

		return elementRoot;
	}

	public boolean equals(Object a_key)
	{
		if (a_key == null || !(a_key instanceof DummySignatureKey))
		{
			return false;
		}

		return (getKeyValue() == ((DummySignatureKey)a_key).getKeyValue());
	}

	/**
	 * @return the public key`s hash code
	 * @see java.lang.Object#hashCode()
	 */
	public int hashCode()
	{
		return (int)m_key;
	}

}
