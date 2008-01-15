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

import java.io.*;
import javax.xml.parsers.*;

import org.w3c.dom.*;
import anon.util.*;
import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;

/**
 * XML Structure for the bank account information for a Mix Operator
 *  (e.g. email-address used for paypal, or bank name and bank account number)
 *
 *  type: how does the Mixoperator want to get paid? (e.g. paypal)
 *  details: information needed by the BI operator for paying the MixOperator (e.g. email address)
 *           Since the structure of the details depends on the type (different types of payment require
 *           different kinds and amounts of information), all the info is lumped together into the one String m_details
 *
 * @author Elmar Schraml
 */
public class XMLBankAccount implements IXMLEncodable
{
	//private fields

	private String m_type;
	private String m_details;
	private String m_operatorCert = "none";
	//when storing update bank information, the BI database needs to know which operator to store it for
	//the operatorCert saves the MixConfig tool the effort of sending a second data structure just for that

	private Document m_docTheBankAccount;
	public static String ms_strElemName = "BankAccount";

	//Constructors

	public XMLBankAccount(String type, String details)
	{
		m_type = type;
		m_details = details;
		m_docTheBankAccount = XMLUtil.createDocument();
		m_docTheBankAccount.appendChild(internal_toXmlElement(m_docTheBankAccount));
	}

	public XMLBankAccount(String type, String details, String operatorCert)
	{
		m_type = type;
		m_details = details;
		m_operatorCert = operatorCert;
		m_docTheBankAccount = XMLUtil.createDocument();
		m_docTheBankAccount.appendChild(internal_toXmlElement(m_docTheBankAccount));
	}

	/**
	 * internal_toXmlElement
	 *
	 * @param m_docTheBankAccount Document
	 * @return Node
	 */
	private Node internal_toXmlElement(Document a_doc)
	{
		Element elemRoot = a_doc.createElement(ms_strElemName);
		elemRoot.setAttribute("version", "1.0");
		Element elem;

		elem = a_doc.createElement("Type");
		XMLUtil.setValue(elem, m_type);
		elemRoot.appendChild(elem);

		elem = a_doc.createElement("Details");
		XMLUtil.setValue(elem, m_details);
		elemRoot.appendChild(elem);

		if (! m_operatorCert.equals("none") )
		{
			elem = a_doc.createElement("OperatorCert");
			XMLUtil.setValue(elem, m_operatorCert);
			elemRoot.appendChild(elem);
		}
		return elemRoot;
	}

	public XMLBankAccount(String xml) throws Exception
	{
		ByteArrayInputStream in = new ByteArrayInputStream(xml.getBytes());
		Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(in);
		setValues(doc.getDocumentElement());
		m_docTheBankAccount = doc;
	}

	public XMLBankAccount(byte[] xmldata) throws Exception
	{
		ByteArrayInputStream in = new ByteArrayInputStream(xmldata);
		Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(in);
		setValues(doc.getDocumentElement());
		m_docTheBankAccount = doc;
	}

	public XMLBankAccount(Element xml) throws Exception
	{
		setValues(xml);
		m_docTheBankAccount = XMLUtil.createDocument();
		m_docTheBankAccount.appendChild(XMLUtil.importNode(m_docTheBankAccount, xml, true));
	}

	public XMLBankAccount(Document xml) throws Exception
{
	Element elemRoot = xml.getDocumentElement();
	setValues(elemRoot);
	m_docTheBankAccount = xml;
}



	/**
	 * Parses the XML representation and sets the internal values
	 *
	 * @param xml Node
	 * @throws Exception
	 */
	private void setValues(Element xml) throws Exception
	{
		if (!xml.getTagName().equals("BankAccount"))
		{
			throw new Exception("XMLBankAccount: cannot parse, wrong xml format!");
		}
		/**********
		if (!xml.getAttribute("version").equals("1.0"))
		{
			throw new Exception("XMLBankAccount: cannot parse, cert version is " +
				xml.getAttribute("version") + " but 1.0 was expected.");
		}
		************/

		// parse type
		Element elem = (Element) XMLUtil.getFirstChildByName(xml, "Type");
		m_type = XMLUtil.parseValue(elem, "error");
		if (m_type.equals("error") )
		{
			throw new Exception("XMLBankAccount: cannot parse the account type");
		}

		// parse details
		elem = (Element) XMLUtil.getFirstChildByName(xml, "Details");
		m_details = XMLUtil.parseValue(elem, "error");
		if (m_details.equals("error"))
		{
			throw new Exception("XMLBankAccount: cannot parse the account details");
		}

		//parse operatorCert
		elem = (Element) XMLUtil.getFirstChildByName(xml, "OperatorCert");
		m_operatorCert = XMLUtil.parseValue(elem, "none");
		if (m_operatorCert.equals("error"))
		{
			LogHolder.log(LogLevel.ERR, LogType.PAY,"XMLBankAccount: no operator cert set");
			//do not throw an exception here, since operator does not have to be set
		}

	}



	//Getters

	public String getType()
	{
		return m_type;
	}

	public String getDetails()
	{
		return m_details;
	}

	public String getOperatorCert()
	{
		return m_operatorCert;
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
			return (Element) XMLUtil.importNode(a_doc, m_docTheBankAccount.getDocumentElement(), true);
		}
		catch (Exception e)
		{
			return null;
		}
	}




}
