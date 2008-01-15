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
import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;

/**
 * simple XML representation of the subject, return Address, sender name (not an email field,
 * just to distinguish the sender from the rest of the body text) and body text
 *
 * Geared towards sending support emails form the Mixconfig tool to the BI (which then sends it on using a local smtp
 * server),
 * currently does not support attachments, and no server data
 *
 * For support messages, use the simplest constructor without receiver and subject, will be set to defaults for
 * sending to jap support
 *
 * senderIdentification can be used to pass along some kind of information that allows the recipient of this object
 * to look up more information on the sender (typically a public certificate), can be null
 *
 * @author Elmar Schraml
 */
public class XMLEmail implements IXMLEncodable
{
	private String m_senderName;
	private String m_replyAddress;
	private String m_bodyText;
	private String m_receiverAddress;
	private String m_subject;
	private String m_senderIdentification;

	private Document m_docTheEmail;
	public static String ms_strElemName = "Email";

	/**
	 *  A support email, sent to the JAP support team with a default subject
	 * @param senderName String
	 * @param replyAddress String
	 * @param bodyText String
	 * @param senderIdentification: can be null
	 */
	public XMLEmail(String senderName, String replyAddress, String bodyText, String senderIdentification)
	{
		m_senderName = senderName;
		m_replyAddress = replyAddress;
		m_bodyText = bodyText;
		m_senderIdentification = senderIdentification;
		setDefaultValues();
	}

	/**
	 * A generic email with custom subject and receiver address
	 *
	 * @param senderName String
	 * @param replyAddress String
	 * @param bodyText String
	 * @param receiverAddress String
	 * @param subject String
	 * @param senderIdentification: can be null
	 */
	public XMLEmail(String senderName, String replyAddress, String bodyText, String receiverAddress, String subject, String senderIdentification)
	{
		m_senderName = senderName;
		m_replyAddress = replyAddress;
		m_bodyText = bodyText;
		m_receiverAddress = receiverAddress;
		m_subject = subject;
		m_senderIdentification = senderIdentification;
		setDefaultValues();
	}
	/**
	 * ensure that no empty xml elements are encoded
	 * (since that would throw an exception upon parsing the xml)
	 */
	private void setDefaultValues()
	{
		if (m_receiverAddress == null || m_receiverAddress.equals("") )
		{
			m_receiverAddress = "jap@inf.tu-dresden.de";
		}

		if (m_replyAddress == null || m_replyAddress.equals("") )
		{
			m_replyAddress = "no return";
		}

		if (m_senderName == null || m_senderName.equals("") )
		{
			m_senderName = "Unknown Sender";
		}

		if (m_subject == null || m_subject.equals("") )
		{
			m_subject = "AN.ON support request";
		}

		if (m_bodyText == null || m_bodyText.equals("") )
		{
			m_bodyText = "message is empty";
		}

	}

	private Node internal_toXmlElement(Document a_doc)
	{
		Element elemRoot = a_doc.createElement(ms_strElemName);
		Element elem;

		elem = a_doc.createElement("SenderName");
		XMLUtil.setValue(elem, m_senderName);
		elemRoot.appendChild(elem);

		elem = a_doc.createElement("ReplyAddress");
		XMLUtil.setValue(elem, m_replyAddress);
		elemRoot.appendChild(elem);

		elem = a_doc.createElement("ReceiverAddress");
		XMLUtil.setValue(elem, m_receiverAddress);
		elemRoot.appendChild(elem);

		elem = a_doc.createElement("Subject");
		XMLUtil.setValue(elem, m_subject);
		elemRoot.appendChild(elem);

		elem = a_doc.createElement("BodyText");
		XMLUtil.setValue(elem, m_bodyText);
		elemRoot.appendChild(elem);

		elem = a_doc.createElement("SenderIdentification");
		XMLUtil.setValue(elem, m_senderIdentification);
		elemRoot.appendChild(elem);

		return elemRoot;
	}

	public XMLEmail(String xml) throws Exception
	{
		ByteArrayInputStream in = new ByteArrayInputStream(xml.getBytes());
		Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(in);
		setValues(doc.getDocumentElement());
		m_docTheEmail = doc;
	}

	public XMLEmail(byte[] xmldata) throws Exception
	{
		ByteArrayInputStream in = new ByteArrayInputStream(xmldata);
		Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(in);
		setValues(doc.getDocumentElement());
		m_docTheEmail = doc;
	}

	public XMLEmail(Element xml) throws Exception
	{
		setValues(xml);
		m_docTheEmail = XMLUtil.createDocument();
		m_docTheEmail.appendChild(XMLUtil.importNode(m_docTheEmail, xml, true));
	}

	public XMLEmail(Document xml) throws Exception
	{
		Element elemRoot = xml.getDocumentElement();
		setValues(elemRoot);
		m_docTheEmail = xml;
	}

	public String getSenderName()
	{
		return m_senderName ;
	}

	public String getReplyAddress()
	{
		return m_replyAddress;
	}

	public String getReceiverAddress()
	{
		return m_receiverAddress;
	}

	public String getSubject()
	{
		return m_subject ;
	}

	public String getBodyText()
	{
		return m_bodyText;
	}

	public String getSenderIdentification()
	{
		return m_senderIdentification;
	}

	private void setValues(Element xml) throws Exception
	{
		if (!xml.getTagName().equals("Email"))
		{
			throw new Exception("XMLEmail: cannot parse, wrong xml format!");
		}

		Element elem;

		// parse sender name
		elem = (Element) XMLUtil.getFirstChildByName(xml, "SenderName");
		m_senderName = XMLUtil.parseValue(elem, "");
		if (m_senderName.equals("") )
		{
			throw new Exception("XMLEmail: cannot parse the sender name");
		}

		// parse receiver address
		elem = (Element) XMLUtil.getFirstChildByName(xml, "ReceiverAddress");
		m_receiverAddress = XMLUtil.parseValue(elem, "");
		if (m_receiverAddress.equals("") )
		{
			throw new Exception("XMLEmail: cannot parse the receiver address");
		}

		// parse reply address
		elem = (Element) XMLUtil.getFirstChildByName(xml, "ReplyAddress");
		m_replyAddress = XMLUtil.parseValue(elem, "");
		if (m_replyAddress.equals("") )
		{
			throw new Exception("XMLEmail: cannot parse the reply address");
		}

		// parse subject
		elem = (Element) XMLUtil.getFirstChildByName(xml, "Subject");
		m_subject = XMLUtil.parseValue(elem, "");
		if (m_subject.equals("") )
		{
			throw new Exception("XMLEmail: cannot parse the Subject");
		}

		// parse body text
		elem = (Element) XMLUtil.getFirstChildByName(xml, "BodyText");
		m_bodyText = XMLUtil.parseValue(elem, "");
		if (m_bodyText.equals("") )
		{
			throw new Exception("XMLEmail: cannot parse the body text");
		}

		//parse sender identification
		elem = (Element) XMLUtil.getFirstChildByName(xml, "SenderIdentification");
		m_senderIdentification = XMLUtil.parseValue(elem, "");
		//do NOT throw Exception if empty, might very well not be set

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
			return (Element) XMLUtil.importNode(a_doc, m_docTheEmail.getDocumentElement(), true);
		}
		catch (Exception e)
		{
			return null;
		}
	}
}
