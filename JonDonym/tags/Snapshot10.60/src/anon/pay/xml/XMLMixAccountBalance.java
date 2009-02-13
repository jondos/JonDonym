/*
 Copyright (c) 2000-2006, The JAP-Team
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

import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import anon.crypto.IMyPrivateKey;
import anon.crypto.XMLSignature;
import anon.util.IXMLEncodable;
import anon.util.XMLUtil;

/**
 * A simple XML structure storing the current balance of a MixOperator
 * (i.e. how much money does the BI operator owe the MixOperator for the traffic handled by his Mixes?)
 *
 * @author Elmar Schraml
 */
public class XMLMixAccountBalance implements IXMLEncodable
{
	//private fields

	private int m_balance;
	private java.sql.Timestamp  m_lastUpdate;
	private Document m_docTheMixAccountBalance;
	public static String ms_strElemName = "MixAccountBalance";

	//Constructors

	public XMLMixAccountBalance(int balance, java.sql.Timestamp lastUpdate)
	{
		m_balance = balance;
		m_lastUpdate = lastUpdate;
		m_docTheMixAccountBalance = XMLUtil.createDocument();
		m_docTheMixAccountBalance.appendChild(internal_toXmlElement(m_docTheMixAccountBalance));
	}

	/**
	 * internal_toXmlElement
	 *
	 * @param m_docTheMixAccountBalance Document
	 * @return Node
	 */
	private Node internal_toXmlElement(Document a_doc)
	{
		Element elemRoot = a_doc.createElement(ms_strElemName);
		elemRoot.setAttribute("version", "1.0");
		Element elem;

		elem = a_doc.createElement("Balance");
		XMLUtil.setValue(elem, m_balance);
		elemRoot.appendChild(elem);

		elem = a_doc.createElement("LastUpdate");
		XMLUtil.setValue(elem, m_lastUpdate.toString());
		elemRoot.appendChild(elem);

		return elemRoot;
	}

	public XMLMixAccountBalance(String xml) throws Exception
	{
		ByteArrayInputStream in = new ByteArrayInputStream(xml.getBytes());
		Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(in);
		setValues(doc.getDocumentElement());
		m_docTheMixAccountBalance = doc;
	}

	public XMLMixAccountBalance(byte[] xmldata) throws Exception
	{
		ByteArrayInputStream in = new ByteArrayInputStream(xmldata);
		Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(in);
		setValues(doc.getDocumentElement());
		m_docTheMixAccountBalance = doc;
	}

	public XMLMixAccountBalance(Element xml) throws Exception
	{
		setValues(xml);
		m_docTheMixAccountBalance = XMLUtil.createDocument();
		m_docTheMixAccountBalance.appendChild(XMLUtil.importNode(m_docTheMixAccountBalance, xml, true));
	}

	public XMLMixAccountBalance(Document doc) throws Exception
	{
		Element elemRoot = doc.getDocumentElement();
		setValues(elemRoot);
	}


	/**
	 * Parses the XML representation and sets the internal values
	 *
	 * @param xml Node
	 * @throws Exception
	 */
	private void setValues(Element xml) throws Exception
	{
		if (!xml.getTagName().equals("MixAccountBalance"))
		{
			throw new Exception("XMLMixAccountBalance: cannot parse, wrong xml format!");
		}
		if (!xml.getAttribute("version").equals("1.0"))
		{
			throw new Exception("XMLMixAccountBalance: cannot parse, cert version is " +
								xml.getAttribute("version") + " but 1.0 was expected.");
		}

		// parse balance
		Element elem = (Element) XMLUtil.getFirstChildByName(xml, "Balance");
		m_balance = XMLUtil.parseValue(elem, -1000);
		if (m_balance == -1000 )
		{
			throw new Exception("XMLMixAccountBalance: cannot parse the balance");
		}

		// parse time of last update
		elem = (Element) XMLUtil.getFirstChildByName(xml, "LastUpdate");
		String timestamp = XMLUtil.parseValue(elem, "0");
		m_lastUpdate = java.sql.Timestamp.valueOf(timestamp);

	}

	public boolean sign(IMyPrivateKey key)
	{
		try
		{
			XMLSignature.sign(m_docTheMixAccountBalance, key);
			return true;
		}
		catch (Exception e)
		{
			return false;
		}
	}


	//Getters

	public int getBalance()
	{
		return m_balance;
	}

	public java.sql.Timestamp getLastUpdate()
	{
		return m_lastUpdate;
	}


	//Methods implementing interface IXMLEncodable
	/**
	 * Return an element that can be appended to the document.
	 * @return the interface as xml element
	 */
	public Element toXmlElement(Document a_doc)
	{
		try
		{
			return (Element) XMLUtil.importNode(a_doc, m_docTheMixAccountBalance.getDocumentElement(), true);
		}
		catch (Exception e)
		{
			return null;
		}
	}




}
