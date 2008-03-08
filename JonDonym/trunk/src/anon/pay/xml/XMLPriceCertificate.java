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
import anon.util.XMLParseException;
import java.util.Enumeration;
import logging.LogType;
import logging.LogLevel;
import logging.LogHolder;
import java.text.SimpleDateFormat;
import java.text.NumberFormat;
import java.util.Locale;
import anon.pay.PaymentInstanceDBEntry;

/**
 * Contains the functionality for creating and parsing XML Price Certificates
 * can be constructed from an xml Document in String, byte[] or Element representation
 *
 * @author Elmar Schraml
 * @version 1.0
 */
public class XMLPriceCertificate implements IXMLEncodable
{
	public static final String XML_ELEMENT_NAME = "PriceCertificate";

	//private fields

	private String  m_subjectKeyIdentifier; //the X509SubjectKeyIdentifier of the corresponding Mix
	private double m_rate; //cents per MB, may be fractional
	private java.sql.Timestamp m_signatureTime;
	private String m_biID;
	private String m_hashValue;
	private Document m_docThePriceCert;


	private static final String XML_ELEM_SUBJECT_KEY_IDENTIFIER = "SubjectKeyIdentifier";
	private static final String XML_ELEM_RATE = "Rate";
	private static final String XML_ELEM_SIG_TIME = "SignatureTime";
	private static final String XML_ELEM_BIID = "BiID";

	//Constructors

	/**
	 *
	 * @param rate int
	 * @param creationTime Timestamp
	 * @param biID String
	 */
	public XMLPriceCertificate(String subjectKeyIdentifier, double rate, java.sql.Timestamp signatureTime, String biID)
	{
		m_subjectKeyIdentifier = subjectKeyIdentifier;
		m_signatureTime = signatureTime;
		m_rate = rate;
		m_biID = biID;
	    m_docThePriceCert = XMLUtil.createDocument();
		m_docThePriceCert.appendChild(internal_toXmlElement(m_docThePriceCert));
		m_hashValue = XMLSignature.getHashValueOfElement(m_docThePriceCert);
	}

	/**
	 * XMLPriceCertificate: new price cert that has not been signed yet, and therefor has no signature-node and signatureTime
	 *
	 * @param subjectKeyIdentifier String
	 * @param rate double
	 * @param biID String
	 */
	public XMLPriceCertificate(String subjectKeyIdentifier, double rate, String biID)
	{
		m_subjectKeyIdentifier = subjectKeyIdentifier;
		m_signatureTime = null;
		m_rate = rate;
		m_biID = biID;
		m_docThePriceCert = XMLUtil.createDocument();
		m_docThePriceCert.appendChild(internal_toXmlElement(m_docThePriceCert));
		m_hashValue = XMLSignature.getHashValueOfElement(m_docThePriceCert);
	}


	public XMLPriceCertificate(String subjectKeyIdentifier, double rate, java.sql.Timestamp signatureTime, String biID, String signatureXml)
	{
		m_subjectKeyIdentifier = subjectKeyIdentifier;
		m_signatureTime = signatureTime;
		m_rate = rate;
		m_biID = biID;
		m_docThePriceCert = XMLUtil.createDocument();
		m_docThePriceCert.appendChild(internal_toXmlElement(m_docThePriceCert));
		addSignatureNode(m_docThePriceCert,signatureXml);
		m_hashValue = XMLSignature.getHashValueOfElement(m_docThePriceCert);
	}

	public void addSignature(String signatureXml)
	{
		addSignatureNode(m_docThePriceCert,signatureXml);
	}

	private void addSignatureNode(Document a_doc, String signatureXml)
	{
		Document signatureDoc = null;
		try
		{
			signatureDoc = XMLUtil.toXMLDocument(signatureXml);
			Element signatureElem = signatureDoc.getDocumentElement();
			XMLUtil.importNode(a_doc,signatureElem,true);
		} catch (Exception e)
		{
			LogHolder.log(LogLevel.DEBUG, LogType.PAY, "Could not parse signature node from string");
			LogHolder.log(LogLevel.DEBUG, LogType.PAY, e.getMessage());
		}
	}

	/**
	 * internal_toXmlElement
	 *
	 * @param m_docThePriceCert Document
	 * @return Node
	 */
	private Node internal_toXmlElement(Document a_doc)
	{
		Element elemRoot = a_doc.createElement(XML_ELEMENT_NAME);
		elemRoot.setAttribute("version", "1.1");
		Element elem;

		elem = a_doc.createElement("SubjectKeyIdentifier");
		XMLUtil.setValue(elem, m_subjectKeyIdentifier);
		elemRoot.appendChild(elem);

		elem = a_doc.createElement("Rate");
		//do NOT pass the raw double .setValue(),
		//since we need to make sure the xml contains exactly two decimal digits
		//set locale to english to make sure a full stop is used as decimal point, not a comma
		NumberFormat twoDecDigits = NumberFormat.getInstance(Locale.ENGLISH);
		twoDecDigits.setMinimumFractionDigits(2);
		twoDecDigits.setMaximumFractionDigits(2);
		String rateString = twoDecDigits.format(m_rate);
		XMLUtil.setValue(elem, rateString);
		elemRoot.appendChild(elem);

		elem = a_doc.createElement("SignatureTime");
		//if signatureTime is null (for unsigned cert), set the element to an empty string
		String signatureTimeString = "";
		if (m_signatureTime != null)
		{
			signatureTimeString = m_signatureTime.toString();
		}
		XMLUtil.setValue(elem, signatureTimeString);
		elemRoot.appendChild(elem);

		elem = a_doc.createElement("BiID");
		XMLUtil.setValue(elem, m_biID);
		elemRoot.appendChild(elem);

		return elemRoot;
	}

	public XMLPriceCertificate(String xml) throws Exception
	{
		ByteArrayInputStream in = new ByteArrayInputStream(xml.getBytes());
		Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(in);
		setValues(doc.getDocumentElement());
		m_docThePriceCert = doc;
		m_hashValue = XMLSignature.getHashValueOfElement(m_docThePriceCert);
	}
	
	public XMLPriceCertificate(char[] xmldata) throws Exception
	{
		this(new String(xmldata));
	}
	
	public XMLPriceCertificate(byte[] xmldata) throws Exception
	{
		ByteArrayInputStream in = new ByteArrayInputStream(xmldata);
		Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(in);
		setValues(doc.getDocumentElement());
		m_docThePriceCert = doc;
		m_hashValue = XMLSignature.getHashValueOfElement(m_docThePriceCert);
	}

	public XMLPriceCertificate(Element xml) throws XMLParseException
	{
		setValues(xml);
		m_docThePriceCert = XMLUtil.createDocument();
		m_docThePriceCert.appendChild(XMLUtil.importNode(m_docThePriceCert, xml, true));
		m_hashValue = XMLSignature.getHashValueOfElement(m_docThePriceCert);
	}

	public XMLPriceCertificate(Document xml) throws Exception
	{
		Element elemRoot = xml.getDocumentElement();
		setValues(elemRoot);
		m_docThePriceCert = xml;
		m_hashValue = XMLSignature.getHashValueOfElement(m_docThePriceCert);
	}


	public boolean sign(IMyPrivateKey key)
	{
		try
		{
			XMLSignature theSignature = XMLSignature.sign(m_docThePriceCert, key);
			//removing the certificates saves bandwidth, and all parties involved have the BI's cert anyway
			theSignature.clearCertificates();
			return true;
		}
		catch (Exception e)
		{
			LogHolder.log(LogLevel.EXCEPTION, LogType.PAY, "Error signing the certificate: ",e);
			return false;
		}
	}

	public boolean verify(PaymentInstanceDBEntry a_bi)
	{
		try
		{
			return XMLSignature.verify(m_docThePriceCert, a_bi.getCertificate()) != null;
		}
		catch (XMLParseException ex)
		{
			return false;
		}
	}


	/**
	 * Parses the XML representation and sets the internal values
	 *
	 * @param xml Node
	 * @throws Exception
	 */
	private void setValues(Element xml) throws XMLParseException
	{
		XMLUtil.assertNodeName(xml, XML_ELEMENT_NAME);

		// parse SubjectKeyIdentifier
		Element elem = (Element) XMLUtil.getFirstChildByName(xml, XML_ELEM_SUBJECT_KEY_IDENTIFIER);
		XMLUtil.assertNotNull(elem);

		m_subjectKeyIdentifier = XMLUtil.parseValue(elem, null);
		if (m_subjectKeyIdentifier == null)
		{
			throw new XMLParseException(XML_ELEM_SUBJECT_KEY_IDENTIFIER);
		}

		// parse rate
		elem = (Element) XMLUtil.getFirstChildByName(xml, XML_ELEM_RATE);
		m_rate = XMLUtil.parseValue(elem, -9999.99);
		if (m_rate == -9999.99)
		{
			throw new XMLParseException(XML_ELEM_RATE);
		}

		// parse signature time
		elem = (Element) XMLUtil.getFirstChildByName(xml, XML_ELEM_SIG_TIME);
		//might not be signed yet
		if (elem != null)
		{
			String timestamp = XMLUtil.parseValue(elem, "0");
			if (!timestamp.equals("0"))
			{
				m_signatureTime = java.sql.Timestamp.valueOf(timestamp);
			}
		}
		//parse BI id
		elem = (Element) XMLUtil.getFirstChildByName(xml, XML_ELEM_BIID);
		m_biID = XMLUtil.parseValue(elem, "unknown");
		if (m_biID.equals("unknown") )
		{
			throw new XMLParseException(XML_ELEM_BIID);
		}
	}


	public java.sql.Timestamp getSignatureTime()
	{
		return m_signatureTime;
	}

	public double getRate()
	{
		return m_rate;
	}

	public String getSubjectKeyIdentifier()
	{
		return m_subjectKeyIdentifier;
	}

	public String getBiID()
	{
		return m_biID;
	}

	public String getHashValue()
	{
		return m_hashValue;
	}

	public Document getDocument()
	{
		return m_docThePriceCert;
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
			Element priceCertDocElem = m_docThePriceCert.getDocumentElement();
			return (Element) XMLUtil.importNode(a_doc, priceCertDocElem, true);
		}
		catch (Exception e)
		{
			return null;
		}
	}

	/**
	 * toString: responsible for the representation of a price cert in a GUI component
	 *
	 * @return String:Formatted as: Price: XX.XX Eurocent, [signed: dd.mm.yyyy | not signed]
	 */
	public String toString()
	{
		String priceLabel = new String("Price: ");
		String price = formatEuroCentValue(this.getRate());
		String sig;
		if (this.getSignatureTime() == null )
		{
			sig = "Not signed";
		}
		else
		{
			SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yy");
			sig = "Signed on : " + sdf.format(this.getSignatureTime());
		}
		return priceLabel+price+", "+sig;
	}

	private static String formatEuroCentValue(double centvalue)
	{
		NumberFormat nf = NumberFormat.getInstance();
		nf.setMaximumFractionDigits(2);
		nf.setMinimumFractionDigits(2);
		return nf.format(centvalue)+" Eurocent";
	}

}
