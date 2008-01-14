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

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import anon.crypto.IMyPublicKey;
import anon.util.XMLUtil;
import anon.util.IXMLEncodable;
import anon.crypto.IMyPrivateKey;
import anon.crypto.XMLSignature;

/**
 * This class contains the functionality for creating and parsing XML account
 * certificates. It provides access to the public key {@link pubKey}, the
 * account number {@link accountNumber} and the timestamp {@link validTime}.
 * <strong>Note: This class does not perform any signing or signature
 * checking!</strong> This is done in {@link XMLSignature}, so if you want to
 * generate a signed XML certificate, utilize this class to sign it! The XML
 * account certificates have the following format:
 * <pre>
 *    &lt;?xml version="1.0"?&gt;
 *    &lt;AccountCertificate version="1.0"&gt;
 *      &lt;AccountNumber&gt;123456789012&lt;/AccountNumber&gt;
 *      &lt;BiID&gt;The BI ID&lt;BiID&gt;
 *      &lt;JapPublicKey&gt;
 *        ... public key xml data (see below)
 *      &lt;/JapPublicKey&gt;
 *      &lt;CreationTime&gt;YYYY-MM-DD&lt;/CreationTime&gt;
 *      &lt;Signature&gt;
 *        ... signature xml data (see below)
 *      &lt;/Signature&gt;
 *    &lt;/AccountCertificate&gt;
 * </pre>
 *
 * <ul>
 * <li>
 * The public key is stored in w3c xmlsig format (DSAKeyValue or RSAKeyValue)
 * inside the JapPublicKey tags
 * </li>
 * <li>
 * Ths signature is stored in {@link XMLSignature} format inside the Signature
 * tags
 * </li>
 * </ul>
 */
public class XMLAccountCertificate implements IXMLEncodable
{

	//~ Instance fields ********************************************************

	private IMyPublicKey m_publicKey;
	private java.sql.Timestamp m_creationTime;
	private long m_accountNumber;
	private String m_biID;
	private Document m_docTheAccountCert;

	//~ Constructors ***********************************************************

	/**
	 * creates a new XML certificate (NOTE: It will NOT be signed!)
	 *
	 * @param publicKey public key
	 * @param accountNumber account number
	 * @param creationTime creation timestamp
	 * @param biID the signing BI's ID
	 */
	public XMLAccountCertificate(IMyPublicKey publicKey, long accountNumber,
								 java.sql.Timestamp creationTime, String biID
		)
	{
		m_publicKey = publicKey;
		m_accountNumber = accountNumber;
		m_creationTime = creationTime;
		m_biID = biID;
		m_docTheAccountCert = XMLUtil.createDocument();
		m_docTheAccountCert.appendChild(internal_toXmlElement(m_docTheAccountCert));
	}

	/**
	 * parses an existing XML certificate
	 *
	 * @param xml the certificate as string
	 */
	public XMLAccountCertificate(String xml) throws Exception
	{
		Document doc = XMLUtil.toXMLDocument(xml);
		setValues(doc.getDocumentElement());
		m_docTheAccountCert = doc;
	}

	public XMLAccountCertificate(byte[] xmldata) throws Exception
	{
		Document doc = XMLUtil.toXMLDocument(xmldata);
		setValues(doc.getDocumentElement());
		m_docTheAccountCert = doc;
	}

	/**
	 * Creates an AccountCertifcate from  an existing XML docuemnt
	 *
	 * @param xml the node that represents the AccountCertifcate
	 */
	public XMLAccountCertificate(Element xml) throws Exception
	{
		setValues(xml);
		m_docTheAccountCert = XMLUtil.createDocument();
		m_docTheAccountCert.appendChild(XMLUtil.importNode(m_docTheAccountCert, xml, true));
	}

	/**
	 * Parses the XML representation and sets the internal values
	 *
	 * @param xml Node
	 * @throws Exception
	 */
	private void setValues(Element xml) throws Exception
	{
		if (!xml.getTagName().equals("AccountCertificate"))
		{
			throw new Exception("XMLAccountCertificate: cannot parse, wrong xml format!");
		}
		if (!xml.getAttribute("version").equals("1.0"))
		{
			throw new Exception("XMLAccountCertificate: cannot parse, cert version is " +
								xml.getAttribute("version") + " but 1.0 was expected.");
		}

		// parse accountnumber
		Element elem = (Element) XMLUtil.getFirstChildByName(xml, "AccountNumber");
		m_accountNumber = XMLUtil.parseValue(elem, 0l);
		if (m_accountNumber == 0)
		{
			throw new Exception("XMLAccountCertificate: cannot parse accountnumber");
		}

		// parse biID
		elem = (Element) XMLUtil.getFirstChildByName(xml, "BiID");
		m_biID = XMLUtil.parseValue(elem, "");
		if (m_biID.equals(""))
		{
			throw new Exception("XMLAccountCertificate: cannot parse BiID");
		}

		// parse creation time
		elem = (Element) XMLUtil.getFirstChildByName(xml, "CreationTime");
		String timestamp = XMLUtil.parseValue(elem, "0");
		m_creationTime = java.sql.Timestamp.valueOf(timestamp);

		// parse publickey
		elem = (Element) XMLUtil.getFirstChildByName(xml, "JapPublicKey");
		if (elem == null)
		{
			throw new Exception("XMLAccountCertificate: cannot parse public key");
		}
		m_publicKey = new XMLJapPublicKey(elem).getPublicKey();

	}

	/**
	 * Returns an XML represenation
	 *
	 * @return Document
	 */
	private Element internal_toXmlElement(Document a_doc)
	{
		Element elemRoot = a_doc.createElement("AccountCertificate");
		elemRoot.setAttribute("version", "1.0");

		Element elem = a_doc.createElement("AccountNumber");
		XMLUtil.setValue(elem, Long.toString(m_accountNumber));
		elemRoot.appendChild(elem);

		elem = a_doc.createElement("BiID");
		XMLUtil.setValue(elem, m_biID);
		elemRoot.appendChild(elem);

		/** @todo check timestamp format */
		elem = a_doc.createElement("CreationTime");

		XMLUtil.setValue(elem, m_creationTime.toString());
		elemRoot.appendChild(elem);

		elem = a_doc.createElement("JapPublicKey");
		elemRoot.appendChild(elem);
		elem.setAttribute("version", "1.0");

		elem.appendChild(m_publicKey.toXmlElement(a_doc));

		return elemRoot;
	}

	//~ Methods ****************************************************************

	public long getAccountNumber()
	{
		return m_accountNumber;
	}

	public java.sql.Timestamp getCreationTime()
	{
		return m_creationTime;
	}

	public IMyPublicKey getPublicKey()
	{
		return m_publicKey;
	}

	public boolean sign(IMyPrivateKey key)
	{
		try
		{
			XMLSignature.sign(m_docTheAccountCert, key);
			return true;
		}
		catch (Exception e)
		{
			return false;
		}
	}

	public Element toXmlElement(Document a_doc)
	{
		try
		{
			return (Element) XMLUtil.importNode(a_doc, m_docTheAccountCert.getDocumentElement(), true);
		}
		catch (Exception e)
		{
			return null;
		}
	}

	/**
	 * Gets the Payment Instance ID of the Payment Instance where this account
	 * is residing.
	 * @return String
	 */
	public String getPIID()
	{
		return m_biID;
	}

}
