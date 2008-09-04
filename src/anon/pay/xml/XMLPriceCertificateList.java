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
import java.util.Enumeration;
import java.util.Vector;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import anon.crypto.XMLSignature;
import anon.util.IXMLEncodable;
import anon.util.XMLUtil;
import org.w3c.dom.DOMException;
import logging.LogType;
import logging.LogLevel;
import logging.LogHolder;
import anon.util.XMLParseException;

/**
 *  XML representation of a Vector of XMLPriceCertificates
 *  used by the BI to send all price certificates of one MixOperator to the MixConfig tool
 *
 * Note: the "List" in the name has nothing to do with the data structure of the same name,
 * it uses a Vector internally
 * "XMLPriceCertificates" would have been to easy to confuse with XMLPriceCertificate (a single price certificate)
 *
 * @author Elmar Schraml
 */
public class XMLPriceCertificateList implements IXMLEncodable
{
	private Vector m_thePriceCerts; //contains objects of type XMLPriceCertificate
	private Document m_docThePriceCerts;
	private static final String ms_strElemName = "PriceCertificateList";

	/**
	 * XMLPriceCertificateList
	 *
	 * @param a_priceCerts Vector: should contain one PriceCertificate per Mix of a Cascade
	 *
	 */
	public XMLPriceCertificateList(Vector a_priceCerts)
	{
		m_thePriceCerts = a_priceCerts;
		m_docThePriceCerts = XMLUtil.createDocument();
		m_docThePriceCerts = internal_toXmlElement(m_docThePriceCerts);
	}

	/**
	 * internal_toXmlElement
	 *
	 * @param m_docThePriceCerts Document
	 * @return Node
	 */
	  //different from same method in other IXMLEncodables!
	  private Document internal_toXmlElement(Document a_doc)
	  {
		  Element elemRoot = a_doc.createElement(ms_strElemName);
		  elemRoot.setAttribute("version", "1.0");
		  a_doc.appendChild(elemRoot);
		  XMLPriceCertificate curCert;
		  Document certDoc;
		  Element certElem;
		  for (Enumeration allCerts = m_thePriceCerts.elements();allCerts.hasMoreElements(); )
		  {
			  curCert = (XMLPriceCertificate) allCerts.nextElement();
			  //certDoc = curCert.getDocument();
			  //certElem = certDoc.getDocumentElement();
			  try
			  {
				  certElem = curCert.toXmlElement(a_doc);
				  elemRoot.appendChild(certElem);
				  //XMLUtil.importNode(a_doc,certElem,true);
				  //a_doc.importNode(certDoc,true);
			  } catch (DOMException de)
			  {
				  LogHolder.log(LogLevel.DEBUG, LogType.PAY, de.getMessage());
			  }

		  }
			  return a_doc;
	}


	public XMLPriceCertificateList(String xml) throws Exception
	{
		ByteArrayInputStream in = new ByteArrayInputStream(xml.getBytes());
		Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(in);
		m_thePriceCerts = new Vector();
		setValues(doc.getDocumentElement());
		m_docThePriceCerts = doc;
	}

	public XMLPriceCertificateList(byte[] xmldata) throws Exception
	{
		ByteArrayInputStream in = new ByteArrayInputStream(xmldata);
		Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(in);
		m_thePriceCerts = new Vector();
		setValues(doc.getDocumentElement());
		m_docThePriceCerts = doc;
	}

	public XMLPriceCertificateList(Element xml) throws Exception
	{
		m_thePriceCerts = new Vector();
		setValues(xml);
		m_docThePriceCerts = XMLUtil.createDocument();
		m_docThePriceCerts.appendChild(XMLUtil.importNode(m_docThePriceCerts, xml, true));
	}

	public XMLPriceCertificateList(Document xml) throws Exception
	{
		Element elemRoot = xml.getDocumentElement();
		m_thePriceCerts = new Vector();
		setValues(elemRoot);
		m_docThePriceCerts = xml;
	}


	public Vector getPriceCerts()
	{
		return m_thePriceCerts;
	}

	public static String getXMLElementName()
	{
		return ms_strElemName;
	}

	public Vector getPriceCertHashes()
	{
		Vector allHashes = new Vector();
		XMLPriceCertificate curCert;
		String curHash;
		for (Enumeration e = m_thePriceCerts.elements();e.hasMoreElements(); )
		{
			curCert = (XMLPriceCertificate) e.nextElement();
			curHash = XMLSignature.getHashValueOfElement(curCert.getDocument());
			allHashes.addElement(curHash);
		}
		return allHashes;
	}

	private void setValues(Element root) throws Exception
	{
		if (!root.getTagName().equals("PriceCertificateList"))
		{
			throw new Exception("XMLPriceCertificateList: cannot parse, wrong xml format!");
		}
		if (!root.getAttribute("version").equals("1.0"))
		{
			throw new Exception("XMLPriceCertificate: cannot parse, cert version is " +
								root.getAttribute("version") + " but 1.0 was expected.");
		}


		//parse price certs
		NodeList allCerts = root.getElementsByTagName("PriceCertificate");
		if (allCerts == null )
		{
			throw new Exception("XMLPriceCertificate: cannot parse price certificates");
		}
		Element curCertElem;
		XMLPriceCertificate curCert;
		for (int i = 0; /*forever until break*/; i++)
		{
			if (allCerts.item(i) != null )
			{
				//Element.getElementsByTagName -> NodeList containting Nodes of type Element
				//need to cast, because a IXMLEncodable has constructors for Element and Document, but not the supertype Node
				curCertElem = (Element) allCerts.item(i);
				curCert = new XMLPriceCertificate(curCertElem);
				m_thePriceCerts.addElement(curCert);
			} else {
			  //NodeList doesn't have a length attribute, end is reached as soon as item(i) returns null
			  break;
			}
		}

	}


	/**
	 * Return an element that can be appended to the document.
	 *
	 * @param a_doc a document
	 * @return the interface as xml element
	 */
	public Element toXmlElement(Document a_doc)
	{
		try
		{
			return (Element) XMLUtil.importNode(a_doc, m_docThePriceCerts.getDocumentElement(), true);
		}
		catch (Exception e)
		{
			return null;
		}
	}


}
