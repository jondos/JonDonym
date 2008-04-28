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

import java.sql.Timestamp;
import java.util.Date;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import anon.crypto.IMyPrivateKey;
import anon.crypto.XMLSignature;
import anon.util.IXMLEncodable;
import anon.util.XMLUtil;
import logging.*;

/** @todo add spent, BiID */
public class XMLTransCert implements IXMLEncodable
{
	//~ Instance fields ********************************************************

	private java.sql.Timestamp m_validTime; // how long does the JPI allow the transation to be completed
	private Date m_receivedDate;
	private Date m_usedDate;
	private long m_accountNumber;
	private long m_transferNumber;
	private long m_deposit;	
	private Document m_docTheTransCert;

	public final static String XML_ELEMENT_NAME_TRANSFER_CERTIFICATES = "TransferCertificates";
	public final static String XML_ELEMENT_NAME_TRANSFER_CERTIFICATE = "TransferCertificate";
	//~ Constructors ***********************************************************

	public XMLTransCert(long accountNumber, long transferNumber,
						long deposit, java.sql.Timestamp validTime)
	{
		m_accountNumber = accountNumber;
		m_transferNumber = transferNumber;
		m_deposit = deposit;
		m_validTime = validTime;
		m_docTheTransCert = XMLUtil.createDocument();
		m_docTheTransCert.appendChild(internal_toXmlElement(m_docTheTransCert));
	}

	public XMLTransCert(String xml) throws Exception
	{
        Document doc = XMLUtil.toXMLDocument(xml);
		Element elemRoot = doc.getDocumentElement();
		setValues(elemRoot);
		m_docTheTransCert = doc;
	}

	/**
	 * Creates an TransCert from  an existing XML docuemnt
	 *
	 * @param xml the node that represents the TransCert
	 */
	public XMLTransCert(Element xml) throws Exception
	{
		setValues(xml);
		m_docTheTransCert = XMLUtil.createDocument();
		m_docTheTransCert.appendChild(XMLUtil.importNode(m_docTheTransCert, xml, true));
	}

	public XMLTransCert(Document xml) throws Exception
	{
		Element elemRoot = xml.getDocumentElement();
		setValues(elemRoot);
		m_docTheTransCert = xml;
	}

	//~ Methods ****************************************************************

	public void setReceivedDate(Date a_date)
	{
		m_receivedDate = a_date;
		m_docTheTransCert = XMLUtil.createDocument();
		m_docTheTransCert.appendChild(internal_toXmlElement(m_docTheTransCert));
	}

	public void setUsedDate(Date a_date)
	{
		m_usedDate = a_date;
		m_docTheTransCert = XMLUtil.createDocument();
		m_docTheTransCert.appendChild(internal_toXmlElement(m_docTheTransCert));
	}

	public Date getReceivedDate()
	{
		return m_receivedDate;
	}

	public Date getUsedDate()
	{
		return m_usedDate;
	}

	public long getAccountNumber()
	{
		return m_accountNumber;
	}

	public long getTransferNumber()
	{
		return m_transferNumber;
	}

	public java.sql.Timestamp getValidTime()
	{
		return m_validTime;
	}

	private void setValues(Element elemRoot) throws Exception
	{
		if (!elemRoot.getTagName().equals("TransferCertificate"))
		{
			throw new Exception("XMLTransCert wrong xml structure: " + XMLUtil.toString(elemRoot));
		}

		Element element = (Element) XMLUtil.getFirstChildByName(elemRoot, "AccountNumber");
		String str = XMLUtil.parseValue(element, null);
		m_accountNumber = Long.parseLong(str);

		element = (Element) XMLUtil.getFirstChildByName(elemRoot, "TransferNumber");
		str = XMLUtil.parseValue(element, null);
		m_transferNumber = Long.parseLong(str);

		element = (Element) XMLUtil.getFirstChildByName(elemRoot, "ValidTime");
		str = XMLUtil.parseValue(element, null);
		m_validTime = java.sql.Timestamp.valueOf(str);

		element = (Element) XMLUtil.getFirstChildByName(elemRoot, "ReceivedDate");
		str = XMLUtil.parseValue(element, null);
		if (str != null)
		{
			m_receivedDate = new Date(Long.parseLong(str));
		}
	}

	/**
	 * toXmlElement
	 *
	 * @param a_doc Document
	 * @return Element
	 */
	private Element internal_toXmlElement(Document a_doc)
	{
		Element elemRoot = a_doc.createElement("TransferCertificate");
		elemRoot.setAttribute("version", "1.2");
		Element elem = a_doc.createElement("AccountNumber");
		XMLUtil.setValue(elem, Long.toString(m_accountNumber));
		elemRoot.appendChild(elem);
		elem = a_doc.createElement("TransferNumber");
		XMLUtil.setValue(elem, Long.toString(m_transferNumber));
		elemRoot.appendChild(elem);
		elem = a_doc.createElement("Deposit");
		XMLUtil.setValue(elem, Long.toString(m_deposit));
		elemRoot.appendChild(elem);
		elem = a_doc.createElement("ValidTime");
		XMLUtil.setValue(elem, m_validTime.toString());
		elemRoot.appendChild(elem);
		elem = a_doc.createElement("ReceivedDate");
		if (m_receivedDate != null)
		{
			XMLUtil.setValue(elem, m_receivedDate.getTime());
			elemRoot.appendChild(elem);
		}
		return elemRoot;
	}

	public Element toXmlElement(Document a_doc)
	{
		try
		{
			return (Element) XMLUtil.importNode(a_doc, m_docTheTransCert.getDocumentElement(), true);
		}
		catch (Exception e)
		{
			return null;
		}
	}

	public boolean sign(IMyPrivateKey key)
	{
		try
		{
			XMLSignature.sign(m_docTheTransCert, key);
			return true;
		}
		catch (Exception e)
		{
			return false;
		}
	}

}
