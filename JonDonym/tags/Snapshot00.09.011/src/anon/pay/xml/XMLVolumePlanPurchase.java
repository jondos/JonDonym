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

import org.w3c.dom.*;
import anon.util.*;
import java.io.ByteArrayInputStream;
import javax.xml.parsers.DocumentBuilderFactory;

/**
 *
 * @author Elmar Schraml
 *
 * XML representation of a request by a JAP to buy a specific volume plan
 * The plan is identified by name, it's up to the JPI to get the corresponding information
 * from its database (reason: we don't want a JAP to supply modified values for e.g. price
 *
 */
public class XMLVolumePlanPurchase implements IXMLEncodable
{
	private Document m_docTheVolumePlanPurchase;
	private long m_accountNumber;
	private String m_planName;

	public XMLVolumePlanPurchase(long a_accountNumber, String a_planName)
	{
		m_accountNumber = a_accountNumber;
		m_planName = a_planName;
		m_docTheVolumePlanPurchase = XMLUtil.createDocument();
		m_docTheVolumePlanPurchase.appendChild(internal_toXmlElement(m_docTheVolumePlanPurchase));

	}

	public XMLVolumePlanPurchase(String xml) throws Exception
{
	ByteArrayInputStream in = new ByteArrayInputStream(xml.getBytes());
	Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(in);
	setValues(doc.getDocumentElement());
	m_docTheVolumePlanPurchase = doc;
}


	public String getPlanName()
	{
		return m_planName;
	}

	public long getAccountNumber()
	{
		return m_accountNumber;
	}

	/**
	 * Return an element that can be appended to the document.
	 *
	 * @param a_doc a document
	 * @return the interface as xml element
	 * @todo Implement this anon.util.IXMLEncodable method
	 */
	public Element internal_toXmlElement(Document a_doc)
	{
		Element elemRoot = a_doc.createElement("VolumePlanPurchase");
		Element elem;

		elem = a_doc.createElement("AccountNumber");
		XMLUtil.setValue(elem, m_accountNumber);
		elemRoot.appendChild(elem);

		elem = a_doc.createElement("VolumePlanName");
		XMLUtil.setValue(elem, m_planName);
		elemRoot.appendChild(elem);

		return elemRoot;
	}

	public Element toXmlElement(Document a_doc)
	{
		try
		{
			return (Element) XMLUtil.importNode(a_doc, m_docTheVolumePlanPurchase.getDocumentElement(), true);
		}
		catch (Exception e)
		{
			return null;
		}
	}

	protected void setValues(Element elemRoot) throws Exception
	{
		if (!elemRoot.getTagName().equals("VolumePlanPurchase"))
		{
			throw new Exception("XMLVolumePlan: wrong XML structure");
		}

		Element elem;
		String str;

		elem = (Element) XMLUtil.getFirstChildByName(elemRoot, "AccountNumber");
		long m_accountNumber = XMLUtil.parseValue(elem, 0);

		elem = (Element) XMLUtil.getFirstChildByName(elemRoot, "VolumePlanName");
		String m_planName = XMLUtil.parseValue(elem, null);

	}
}
