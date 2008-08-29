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

import org.w3c.dom.*;
import anon.util.*;
import java.io.ByteArrayInputStream;
import javax.xml.parsers.DocumentBuilderFactory;

/**
 * XML representation of a transfer request, i.e.
 * a MixOperator letting the operator of the BI know that he wants his current balance
 * that he earned by running his Mixes to be transferred to the bank account specified via BankAccountDialog
 *
 * @author Elmar Schraml
 */
public class XMLTransferRequest implements IXMLEncodable
{
	private int m_requested; //used as boolean, 1 = I want to be payed now, has nothing to do with a HTTP request
	private String m_operatorCert; //used to identify the operator when the BI database handles the transfer request
	private Document m_docTheRequest;
	private static String ms_strElemName = "TransferRequest";


	public XMLTransferRequest(int a_requested)
	{
		m_requested = a_requested;
		m_docTheRequest = XMLUtil.createDocument();
		m_docTheRequest.appendChild(internal_toXmlElement(m_docTheRequest));
	}

	public XMLTransferRequest(int a_requested, String a_operator)
	{
		m_requested = a_requested;
		m_operatorCert = a_operator;
		m_docTheRequest = XMLUtil.createDocument();
		m_docTheRequest.appendChild(internal_toXmlElement(m_docTheRequest));
	}


	public XMLTransferRequest(String xml) throws Exception
	{
		ByteArrayInputStream in = new ByteArrayInputStream(xml.getBytes());
		Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(in);
		setValues(doc.getDocumentElement());
		m_docTheRequest = doc;
	}
	
	public XMLTransferRequest(char[] xmldata) throws Exception
	{
		this(new String(xmldata));
	}
	
	public XMLTransferRequest(byte[] xmldata) throws Exception
	{
		ByteArrayInputStream in = new ByteArrayInputStream(xmldata);
		Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(in);
		setValues(doc.getDocumentElement());
		m_docTheRequest = doc;
	}

	public XMLTransferRequest(Element xml) throws Exception
	{
		setValues(xml);
		m_docTheRequest = XMLUtil.createDocument();
		m_docTheRequest.appendChild(XMLUtil.importNode(m_docTheRequest, xml, true));
	}

	public XMLTransferRequest(Document xml) throws Exception
	{
		Element elemRoot = xml.getDocumentElement();
		setValues(elemRoot);
		m_docTheRequest = xml;
	}

	private void setValues(Element xml) throws Exception
	{
		Element elem;
		if (!xml.getTagName().equals("TransferRequest"))
		{
			throw new Exception("XMLTransferRequest: cannot parse, wrong xml format!");
		}
		if (!xml.getAttribute("version").equals("1.0"))
		{
			throw new Exception("XMLTransferRequest: cannot parse, cert version is " +
								xml.getAttribute("version") + " but 1.0 was expected.");
		}
		//parse requested
		elem = (Element) XMLUtil.getFirstChildByName(xml, "Requested");
		m_requested = XMLUtil.parseValue(elem, 0);
		if (m_requested == 0)
		{
			throw new Exception("XMLTransferRequest: cannot parse requested");
		}
		//parse operator cert
		elem = (Element) XMLUtil.getFirstChildByName(xml, "Operator");
		m_operatorCert = XMLUtil.parseValue(elem, "none");
		if (m_operatorCert.equals("none") )
		{
			throw new Exception("no operator cert set in XMLTransferRequest");
		}
	}

	public int getRequested()
	{
		return m_requested;
	}

	public String getOperatorCert()
	{
		return m_operatorCert;
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
			return (Element) XMLUtil.importNode(a_doc, m_docTheRequest.getDocumentElement(), true);
		}
		catch (Exception e)
		{
			return null;
		}
	}

	private Node internal_toXmlElement(Document a_doc)
	{
		Element elemRoot = a_doc.createElement(ms_strElemName);
			elemRoot.setAttribute("version", "1.0");
			Element elem;

			elem = a_doc.createElement("Requested");
			XMLUtil.setValue(elem, m_requested);
			elemRoot.appendChild(elem);

			elem = a_doc.createElement("Operator");
			XMLUtil.setValue(elem, m_operatorCert);
			elemRoot.appendChild(elem);

			return elemRoot;
	}



}
