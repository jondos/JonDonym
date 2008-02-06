/*
 Copyright (c) 2000, The JAP-Team
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

import java.util.Vector;
import anon.util.IXMLEncodable;
import anon.util.XMLUtil;
import org.w3c.dom.Element;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.w3c.dom.Node;
import java.util.Enumeration;
import java.io.ByteArrayInputStream;

/**
 * This class represents an external charge request. It can be used for charging
 * accounts for given transfer numbers with given amounts and currencies.
 * With the help of this class one can charge accounts manually, e.g. by checking
 * bank account statements for transfer numbers in the reference field.
 *
 * @author Tobias Bayer
 */
public class XMLExternalChargeRequest implements IXMLEncodable
{
	/** Holds a String[] of the following form: 1: tan, 2: currency, 3: amount */
	private Vector m_chargeRequest = new Vector();

	/** Password */
	private String m_password;

	public XMLExternalChargeRequest()
	{
	}

	public XMLExternalChargeRequest(String xml) throws Exception
	{
		Document doc = XMLUtil.toXMLDocument(xml);
		setValues(doc.getDocumentElement());
	}
	
	public XMLExternalChargeRequest(char[] xml) throws Exception
	{
		this(new String(xml));
	}
	
	public XMLExternalChargeRequest(byte[] xml) throws Exception
	{
		setValues(XMLUtil.toXMLDocument(xml).getDocumentElement());
	}

	public XMLExternalChargeRequest(Document a_doc)
	{
		try
		{
			setValues(a_doc.getDocumentElement());
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	public void setPassword(String a_password)
	{
		m_password = a_password;
	}

	public String getPassword()
	{
		return m_password;
	}

	public void addCharge(String a_tan, String a_currency, String a_amount)
	{
		m_chargeRequest.addElement(new String[]
								   {a_tan, a_currency, a_amount});
	}

	public Element toXmlElement(Document a_doc)
	{
		Element elemRoot = a_doc.createElement("ExternalChargeRequest");
		Element elem;

		//Add password
		elem = a_doc.createElement("Password");
		elem.appendChild(a_doc.createTextNode(m_password));
		elemRoot.appendChild(elem);

		//Add charge lines
		for (int i = 0; i < m_chargeRequest.size(); i++)
		{
			String[] charge = (String[]) m_chargeRequest.elementAt(i);
			Element elemCharge = a_doc.createElement("Charge");
			elem = a_doc.createElement("TransferNumber");
			elem.appendChild(a_doc.createTextNode(charge[0]));
			elemCharge.appendChild(elem);
			elem = a_doc.createElement("Currency");
			elem.appendChild(a_doc.createTextNode(charge[1]));
			elemCharge.appendChild(elem);
			elem = a_doc.createElement("Amount");
			elem.appendChild(a_doc.createTextNode(charge[2]));
			elemCharge.appendChild(elem);
			elemRoot.appendChild(elemCharge);
		}

		return elemRoot;
	}

	protected void setValues(Element elemRoot) throws Exception
	{
		if (!elemRoot.getTagName().equals("ExternalChargeRequest"))
		{
			throw new Exception("ExternalChargeRequest wrong XML structure");
		}

		Node pw = XMLUtil.getFirstChildByName(elemRoot, "Password");
		if (pw != null)
		{
			m_password = XMLUtil.parseValue(pw, "");
		}

		NodeList charges = elemRoot.getElementsByTagName("Charge");
		for (int i = 0; i < charges.getLength(); i++)
		{
			String tan = "";
			String currency = "";
			String amount = "";

			Node n = XMLUtil.getFirstChildByName(charges.item(i), "TransferNumber");
			if (n != null)
			{
				tan = XMLUtil.parseValue(n, "");
			}
			n = XMLUtil.getFirstChildByName(charges.item(i), "Currency");
			if (n != null)
			{
				currency = XMLUtil.parseValue(n, "");
			}
			n = XMLUtil.getFirstChildByName(charges.item(i), "Amount");
			if (n != null)
			{
				amount = XMLUtil.parseValue(n, "");
			}
			addCharge(tan, currency, amount);
		}

	}

	public Enumeration getChargeLines()
	{
		return m_chargeRequest.elements();
	}
}
