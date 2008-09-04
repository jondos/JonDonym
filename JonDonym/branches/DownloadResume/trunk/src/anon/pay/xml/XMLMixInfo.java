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

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import anon.util.IXMLEncodable;
import java.io.ByteArrayInputStream;
import javax.xml.parsers.DocumentBuilderFactory;
import anon.util.XMLUtil;

/**
 * XML Structure containing information about a single Mix,
 * intended for use in the MixConfig tool to give a MixOperator an overview
 * of his Mixes and finacial status.
 *
 * @author Elmar Schraml
 */
public class XMLMixInfo implements IXMLEncodable
{
	//private fields

	private String m_mixCert; //
	private int  m_balance; //cents per MB, may be fractional
	private java.sql.Timestamp m_updateTime;
	private int m_operatorId;
	private int m_id; //*NOT* the MixId, just the sequence number that the database uses as primary key

	private Document m_docTheMixInfo;



	//Constructors

	/**
	 * contains all the information contained in the database about one particular Mix
	 * (but for balance info, use the signed MixAccountBalance)
	 *
	 * @param rate int
	 * @param creationTime Timestamp
	 * @param biID String
	 */
	public XMLMixInfo(String mixCert, int balance, java.sql.Timestamp updateTime, int operatorId, int id)
	{
		m_mixCert = mixCert;
		m_updateTime = updateTime;
		m_balance = balance;
		m_updateTime = updateTime;
		m_operatorId = operatorId;
		m_id = id;
		m_docTheMixInfo = XMLUtil.createDocument();
	}

	public XMLMixInfo(String xml) throws Exception
	{
		ByteArrayInputStream in = new ByteArrayInputStream(xml.getBytes());
		Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(in);
		setValues(doc.getDocumentElement());
		m_docTheMixInfo = doc;
	}

	public XMLMixInfo(byte[] xmldata) throws Exception
	{
		ByteArrayInputStream in = new ByteArrayInputStream(xmldata);
		Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(in);
		setValues(doc.getDocumentElement());
		m_docTheMixInfo = doc;
	}

	public XMLMixInfo(Element xml) throws Exception
	{
		setValues(xml);
		m_docTheMixInfo = XMLUtil.createDocument();
		m_docTheMixInfo.appendChild(XMLUtil.importNode(m_docTheMixInfo, xml, true));
	}

	/**
	 * Parses the XML representation and sets the internal values
	 *
	 * @param xml Node
	 * @throws Exception
	 */
	private void setValues(Element xml) throws Exception
	{
		if (!xml.getTagName().equals("MixInfo"))
		{
			throw new Exception("XMLMixInfo: cannot parse, wrong xml format!");
		}
		if (!xml.getAttribute("version").equals("1.0"))
		{
			throw new Exception("XMLMixInfo: cannot parse, cert version is " +
								xml.getAttribute("version") + " but 1.0 was expected.");
		}

		// parse MixCertificate
		Element elem = (Element) XMLUtil.getFirstChildByName(xml, "MixCert");
		m_mixCert = XMLUtil.parseValue(elem, "error");
		if (m_mixCert.equals("error") )
		{
			throw new Exception("XMLMixInfo: cannot parse the MixCertificate");
		}

		// parse balance
		elem = (Element) XMLUtil.getFirstChildByName(xml, "Balance");
		m_balance = XMLUtil.parseValue(elem, -1000);
		if (m_balance == -1000)
		{
			throw new Exception("XMLMixInfo: cannot parse balance");
		}

		// parse creation time
		elem = (Element) XMLUtil.getFirstChildByName(xml, "updateTime");
		String timestamp = XMLUtil.parseValue(elem, "0");
		m_updateTime = java.sql.Timestamp.valueOf(timestamp);

	    //parse Operator Id
		elem = (Element) XMLUtil.getLastChildByName(xml,"operatorId");
		m_operatorId = XMLUtil.parseValue(elem, -1);
		if (m_operatorId == -1)
		{
			throw new Exception("XMLMixInfo: cannot parse operator id");
		}
		//parse Mix Id
		elem = (Element) XMLUtil.getLastChildByName(xml,"id");
		m_id = XMLUtil.parseValue(elem, -1);
		if (m_id == -1)
		{
			throw new Exception("XMLMixInfo: cannot parse id");
		}



	}



	//Getters

	public java.sql.Timestamp getUpdateTime()
	{
		return m_updateTime;
	}

	public int getBalance()
	{
		return m_balance;
	}

	public String getMixCert()
	{
		return m_mixCert;
	}

	public int getOperatorId()
	{
		return m_operatorId;
	}

	public int getId()
	{
		return m_id;
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
			return (Element) XMLUtil.importNode(a_doc, m_docTheMixInfo.getDocumentElement(), true);
		}
		catch (Exception e)
		{
			return null;
		}
	}
}
