/*
 Copyright (c) 2000 - 2007, The JAP-Team
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

package anon.pay.xml;

import java.io.ByteArrayInputStream;
import java.util.Enumeration;
import java.util.Vector;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import anon.util.IXMLEncodable;
import anon.util.XMLUtil;

/**
 * A Vector of XMLVolumePlans
 *
 * <VolumePlans>
 *     <VolumePlan>...content of a single XMLVolumePlan ...</VolumePlan>
 *     <VolumePlan>...
 *     ...
 * </VolumePlans>
 *
 * @author Elmar Schraml
 */
public class XMLVolumePlans implements IXMLEncodable
{
	public static final String XML_ELEMENT_NAME = "VolumePlans";


	private Vector m_volumePlans = new Vector();
	private Document m_docTheVolumePlans;

	public XMLVolumePlans()
	{
		//at least create a document, to avoid nullpointerexceptions when calling .toXmlElement on an empty object
		m_docTheVolumePlans = XMLUtil.createDocument();
	}

	public XMLVolumePlans(Vector thePlans)
	{
		m_volumePlans = thePlans;
		m_docTheVolumePlans = XMLUtil.createDocument();
		m_docTheVolumePlans.appendChild(internal_toXmlElement(m_docTheVolumePlans));
	}

	public XMLVolumePlans(String xml) throws Exception
	{
		Document doc = XMLUtil.toXMLDocument(xml);
		setValues(doc.getDocumentElement());
		m_docTheVolumePlans = doc;
	}

	public XMLVolumePlans(Element elemPlans) throws Exception
	{
		setValues(elemPlans);
		m_docTheVolumePlans = XMLUtil.createDocument();
		m_docTheVolumePlans.appendChild(XMLUtil.importNode(m_docTheVolumePlans, elemPlans, true));

	}

	public XMLVolumePlans(Document document) throws Exception
	{
		setValues(document.getDocumentElement());
		m_docTheVolumePlans = document;
	}


	/**
	 * Return an element that can be appended to the document.
	 *
	 * @param a_doc a document
	 * @return the interface as xml element
	 * @todo Implement this anon.util.IXMLEncodable method
	 */
	public Element toXmlElement(Document a_doc)
	{
		try
			{
				return (Element) XMLUtil.importNode(a_doc, m_docTheVolumePlans.getDocumentElement(), true);
			}
			catch (Exception e)
			{
				return null;
			}
	}

	private void setValues(Element elemRoot) throws Exception
	{
		XMLUtil.assertNodeName(elemRoot, XML_ELEMENT_NAME);

		NodeList allPlans = elemRoot.getElementsByTagName(XMLVolumePlan.XML_ELEMENT_NAME);
		for (int i = 0; i < allPlans.getLength(); i++)
		{
			insertByPrice(new XMLVolumePlan( (Element) allPlans.item(i)));
		}
	}

	private void insertByPrice(XMLVolumePlan a_plan)
	{
		int j = 0;
		for (; j < m_volumePlans.size(); j++)
		{
			if ( ( (XMLVolumePlan) m_volumePlans.elementAt(j)).getPrice() >= a_plan.getPrice())
			{
				break;
			}
		}
		m_volumePlans.insertElementAt(a_plan, j);
	}

	private Element internal_toXmlElement(Document doc)
	{
		Element elemRoot = doc.createElement(XML_ELEMENT_NAME);
		Element elem;
		XMLVolumePlan aPlan;

		for( Enumeration e = m_volumePlans.elements(); e.hasMoreElements();)
		{
			aPlan = (XMLVolumePlan) e.nextElement();
			elem = aPlan.toXmlElement(doc);
			elemRoot.appendChild(elem);
		}

		return elemRoot;
	}

	/**
	 * getVolumePlans
	 *
	 * @return Vector: contains XMLVolumePlan objects, might be empty if no plans have been defined
	 */
	public Vector getVolumePlans()
	{
		return m_volumePlans;
	}

	/**
	 * get a specific plan by name (currently does NOT support localized names!)
	 *
	 * @param name String
	 * @return XMLVolumePlan
	 */
	public XMLVolumePlan getVolumePlan(String name)
	{
		for (Enumeration e = m_volumePlans.elements(); e.hasMoreElements(); )
		{
			XMLVolumePlan curPlan = (XMLVolumePlan) e.nextElement();
			if (curPlan.getName().equalsIgnoreCase(name))
			{
				return curPlan;
			}
		}
		return null;
	}

	public XMLVolumePlan getVolumePlan(int index)
	{
		return (XMLVolumePlan) m_volumePlans.elementAt(index);
	}

	public int getNrOfPlans()
	{
		return m_volumePlans.size();
	}

	public void addVolumePlan(XMLVolumePlan a_plan)
	{
		insertByPrice(a_plan);
		m_docTheVolumePlans = XMLUtil.createDocument();
		m_docTheVolumePlans.appendChild(internal_toXmlElement(m_docTheVolumePlans));
	}
}
