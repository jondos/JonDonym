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

import java.io.ByteArrayInputStream;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import anon.util.IXMLEncodable;
import anon.util.XMLUtil;

/**
 * Datacontainer: This is used by the AI to signal the Jap that it wants a costconfirmation
 * or a balance certificate signed.
 *
 * <PayRequest version="1.0">
 *   <BalanceRequest>            (optional,  meaning: Jap should send a XMLBalance)
 *     <NewerThan> Timestamp <NewerThan>
 *   </BalanceRequest>
 *   <AccountRequest />          (optional, meaning: AI wants the Jap to send AccountCert.)
 *   <CC version="1.0">          (optional, meaning: AI wants the Jap to sign this CC)
 *      .... XMLEasyCC structure ....
 *   </CC>
 * </PayRequest>
 *
 *  To check if an instance of XMLPayRequest contains a BalanceRequest, AccountRequest or CC, use:
 *  <ul>
 *  <li>getBalanceTimestamp != null</li>
 *  <li>isAccountRequest()</li>
 *  <li>getCC() != null </li>
 *  </ul>
 * @author Bastian Voigt, Stefan Koepsell
 */
public class XMLPayRequest implements IXMLEncodable
{
	private XMLEasyCC m_cc = null;
	private java.sql.Timestamp m_balanceNewerThan = null;
	private boolean m_bIsAccountRequest;
	public static final Object XML_ELEMENT_NAME = "PayRequest";

	public XMLPayRequest(String xml) throws Exception
	{
        Document doc = XMLUtil.toXMLDocument(xml);
		setValues(doc.getDocumentElement());
	}

	public XMLPayRequest(byte[] xml) throws Exception
	{
        Document doc = XMLUtil.toXMLDocument(xml);
		setValues(doc.getDocumentElement());
	}

	public XMLPayRequest(Document doc) throws Exception
	{
		setValues(doc.getDocumentElement());
	}

	/**
	 * XMLPayRequest
	 *
	 * @param element Element
	 */
	public XMLPayRequest(Element element) throws Exception
	{
		setValues(element);
	}

	private void setValues(Element elemRoot) throws Exception
	{
		if (!elemRoot.getTagName().equals(XML_ELEMENT_NAME) ||
			!elemRoot.getAttribute("version").equals("1.0"))
		{
			throw new Exception("PayRequest wrong format or wrong version number");
		}

		// look for a Balance request
		Element elem = (Element) XMLUtil.getFirstChildByName(elemRoot, "BalanceRequest");
		if (elem != null)
		{
			Element elemTimestamp = (Element) XMLUtil.getFirstChildByName(elem, "NewerThan");
			m_balanceNewerThan = java.sql.Timestamp.valueOf(XMLUtil.parseValue(elemTimestamp, ""));
		}
		else
		{
			m_balanceNewerThan = null;
		}

		// look for a costconfirmation
		elem = (Element) XMLUtil.getFirstChildByName(elemRoot, "CC");
		if (elem != null)
		{
			m_cc = new XMLEasyCC(elem);
		}
		else
		{
			m_cc = null;
		}

		// look for accountrequest
		elem = (Element) XMLUtil.getFirstChildByName(elemRoot, "AccountRequest");
		if (elem != null)
		{
			m_bIsAccountRequest = true;
		}
		else
		{
			m_bIsAccountRequest = false;
		}
	}

	/** @todo implement (not needed atm, only for the interface */
	public Element toXmlElement(Document a_doc)
	{
		return null;
	}

	public XMLEasyCC getCC()
	{
		return m_cc;
	}

	public java.sql.Timestamp getBalanceTimestamp()
	{
		return m_balanceNewerThan;
	}

	/**
	 * isAccountRequest
	 *
	 * @return boolean
	 */
	public boolean isAccountRequest()
	{
		return m_bIsAccountRequest;
	}

}
