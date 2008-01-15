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

import java.util.Random;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import anon.util.IXMLEncodable;
import anon.util.Util;
import anon.util.XMLParseException;
import anon.util.XMLUtil;

/**
 * This class provides a dummy implementation of an IXMLEncodable object
 * for testing purposes.
 * @author Rolf Wendolsky
 */
public class DummyXMLEncodable implements IXMLEncodable
{
	public static final String XML_ELEMENT_NAME = "DummyElement";
	public static final String XML_ELEMENT_CONTAINER_NAME = "DummyElements";

	public static final String NODE_NUMBER = "NumberNode";
	public static final String NODE_BOOLEAN = "BooleanNode";
	public static final String NODE_STRING = "TextNode";
	public static final String NODE_CONTAINER = "ContainerNode";
	public static final String ATTRIBUTE_BOOLEAN = "BooleanAttribute";
	public static final String ATTRIBUTE_INT = "IntAttribute";
	public static final String ATTRIBUTE_ID = "id";

	public String m_id;
	private int m_valueInt;
	private long m_valueLong;
	private boolean m_valueBoolean;
	private String m_valueString;
	private boolean m_attributeBoolean;
	private int m_attributeInt;

	/**
	 * Creates a new dummy.
	 * @param a_id String
	 * @param a_valueInt int
	 * @param a_valueLong long
	 * @param a_valueBoolean boolean
	 * @param a_valueString String
	 * @param a_attributeBoolean boolean
	 * @param a_attributeInt int
	 */
	public DummyXMLEncodable(String a_id, int a_valueInt, long a_valueLong, boolean a_valueBoolean,
							 String a_valueString, boolean a_attributeBoolean, int a_attributeInt)
	{
		m_id = a_id;
		m_valueInt = a_valueInt;
		m_valueLong = a_valueLong;
		m_valueBoolean = a_valueBoolean;
		m_valueString = a_valueString;
		m_attributeBoolean = a_attributeBoolean;
		m_attributeInt = a_attributeInt;
	}

	/**
	 * Creates a new dummy with dummy values.
	 */
	public DummyXMLEncodable()
	{
		this("encodableAK33#3", 142, 5721672, true, "dummy", false, 682);
	}

	/**
	 * Creates a new dummy with random dummy values.
	 * @param a_random a random number generator
	 */
	public DummyXMLEncodable(Random a_random)
	{
		this(String.valueOf(a_random.nextDouble()),
			 a_random.nextInt(),
			 a_random.nextLong(),
			 (a_random.nextInt() % 2) == 0 ? false : true,
			 "dummy",
			 (a_random.nextInt() % 2) == 0 ? false : true,
			 a_random.nextInt());
	}


	/**
	 * Creates a new dummy from xml description.
	 * @param a_element an xml element
	 * @exception XMLParseException if an error occurs while parsing an XML structure
	 */
	public DummyXMLEncodable(Element a_element)
		throws XMLParseException
	{
		Element element;

		try
		{
			m_id = XMLUtil.parseAttribute(a_element, ATTRIBUTE_ID, null);
			element = (Element) XMLUtil.getFirstChildByName(a_element, NODE_STRING);
			m_valueString = XMLUtil.parseValue(element, null);
			m_attributeBoolean = XMLUtil.parseAttribute(element, ATTRIBUTE_BOOLEAN, false);

			element = (Element) XMLUtil.getFirstChildByName(a_element, NODE_CONTAINER);
			m_attributeInt = XMLUtil.parseAttribute(element, ATTRIBUTE_INT, -1);

			element = (Element)XMLUtil.getFirstChildByNameUsingDeepSearch(a_element, NODE_NUMBER);
			m_valueInt = XMLUtil.parseValue(element, -1);

			element = (Element)element.getNextSibling();
			m_valueLong = XMLUtil.parseValue(element, -1l);

			m_valueBoolean =
				XMLUtil.parseValue(XMLUtil.getFirstChildByName(a_element, NODE_BOOLEAN), false);
		}
		catch (Exception a_e)
		{
			throw new XMLParseException(null, Util.getStackTrace(a_e));
		}
	}

	public String getID()
	{
		if (m_id == null)
		{
			return "";
		}

		return m_id;
	}

	public void setID(String a_id)
	{
		m_id = a_id;
	}

	public int getValueInt()
	{
		return m_valueInt;
	}

	public void setValueInt(int a_value)
	{
		m_valueInt = a_value;
	}

	public long getValueLong()
	{
		return m_valueLong;
	}

	public void setValueLong(long a_value)
	{
		m_valueLong = a_value;
	}

	public boolean equals(DummyXMLEncodable a_dummy)
	{
		if (m_valueInt != a_dummy.m_valueInt)
		{
			return false;
		}
		else if (m_valueLong != a_dummy.m_valueLong)
		{
			return false;
		}
		else if (m_valueBoolean != a_dummy.m_valueBoolean)
		{
			return false;
		}
		else if (!m_valueString.equals(a_dummy.m_valueString))
		{
			return false;
		}
		else if (m_attributeBoolean != a_dummy.m_attributeBoolean)
		{
			return false;
		}
		else if (m_attributeInt != a_dummy.m_attributeInt)
		{
			return false;
		}
		else if (!(m_id == null && a_dummy.m_id == null))
		{
			System.out.println(m_id);
			System.out.println(a_dummy.m_id);

			if ((m_id == null && a_dummy.m_id != null) ||
				(m_id != null && a_dummy.m_id == null))
			{
				return false;
			}
			else if (!m_id.equals(a_dummy.m_id))
			{
				return false;
			}
		}

		return true;
	}

	/**
	 * Transforms the dummy into an xml element.
	 * @param a_doc an XML document
	 * @return the dummy as xml element
	 */
	public Element toXmlElement(Document a_doc)
	{
		Element element, element1, element2;

		element = a_doc.createElement(XML_ELEMENT_NAME);
		if (m_id != null && m_id.length() > 0)
		{
			XMLUtil.setAttribute(element, ATTRIBUTE_ID, m_id);
		}


		element1 = a_doc.createElement(NODE_STRING);
		XMLUtil.setAttribute(element1, ATTRIBUTE_BOOLEAN, m_attributeBoolean);
		element1.appendChild(a_doc.createTextNode(m_valueString));
		element.appendChild(element1);

		element1 = a_doc.createElement(NODE_CONTAINER);
		XMLUtil.setAttribute(element1, ATTRIBUTE_INT, m_attributeInt);

		element2 = a_doc.createElement(NODE_NUMBER);
		XMLUtil.setValue(element2, String.valueOf(m_valueInt));
		element1.appendChild(element2);

		element2 = a_doc.createElement(NODE_NUMBER);
		XMLUtil.setValue(element2, String.valueOf(m_valueLong));
		element1.appendChild(element2);

		element.appendChild(element1);

		element1 = a_doc.createElement(NODE_BOOLEAN);
		XMLUtil.setValue(element1, String.valueOf(m_valueBoolean));
		element.appendChild(element1);

		return element;
	}
}
