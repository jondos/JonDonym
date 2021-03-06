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

import anon.util.XMLUtil;
import anon.util.IXMLEncodable;
import anon.crypto.IMyPrivateKey;
import anon.crypto.PKCS12;
import anon.crypto.XMLSignature;
import anon.crypto.IMyPublicKey;
import java.util.Enumeration;
import java.util.Hashtable;

import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;

import org.w3c.dom.NodeList;
import anon.util.XMLParseException;
import anon.util.Util;
import anon.infoservice.MixPosition;

/**
 * XML structure for a easy cost confirmation (without micropayment function) which is sent to the AI by the Jap
 *
 * @author Grischan Gl&auml;nzel, Bastian Voigt, Tobias Bayer, Elmar Schraml
 */
public class XMLEasyCC implements IXMLEncodable
{
	
	public static final String XML_ELEMENT_NAME = "CC";
	
	//~ Instance fields ********************************************************

	private long m_lTransferredBytes;
	private long m_lAccountNumber;
	private int m_id = 0; //to be used as primary key in the BI database, 0 if not yet stored in db
	private Hashtable m_priceCerts = new Hashtable(); //key: Subjectkeyidentifier of Mix (String, id attribute of pricecerthash element)
									//value: value of PriceCerthash element (String)
	private String m_cascadeID; //stored as its own variable, since Hashtable doesnt guarantee order (so we don't know which mix is the first one)
	private Document m_docTheEasyCC;

	private String m_priceCertHashesConcatenated;
	private boolean m_bOldHashFormat = false;

	/** The Payment Instance ID */
	private String m_strPIID;
	//~ Constructors ***********************************************************

	/**
	 * XMLEasyCC
	 *  construct a CC including a Vector of price certificates (one per mix of the cascade)
	 *  id is added when the CC is stored in the BI's database, so it's not passed as an argument here
	 * @param accountNumber long
	 * @param transferred long
	 * @param a_certificate PKCS12
	 * @param a_priceCerts Vector
	 * @throws XMLParseException 
	 * @throws Exception
	 */
	
	public XMLEasyCC(long accountNumber, long transferred, PKCS12 a_certificate, 
			Hashtable a_priceCerts, String a_AiName, String a_strPIID) throws XMLParseException
	{
		m_priceCerts = a_priceCerts;
		m_priceCertHashesConcatenated = 
			createConcatenatedPriceCertHashes(a_priceCerts, true);

		m_lTransferredBytes = transferred;
		m_lAccountNumber = accountNumber;
		m_cascadeID = a_AiName;
		m_strPIID = a_strPIID;
		m_docTheEasyCC = XMLUtil.createDocument();
		m_docTheEasyCC.appendChild(internal_toXmlElement(m_docTheEasyCC));

		if (a_certificate != null)
		{
			XMLSignature.sign(m_docTheEasyCC, a_certificate);
		}
	}


	public XMLEasyCC(byte[] data) throws Exception
	{
		Document doc = XMLUtil.toXMLDocument(data);
		setValues(doc.getDocumentElement());
		m_docTheEasyCC = doc;
	}

	public XMLEasyCC(String xml) throws XMLParseException
	{
		Document doc = XMLUtil.toXMLDocument(xml);
		setValues(doc.getDocumentElement());
		m_docTheEasyCC = doc;
	}
	
	public XMLEasyCC(char[] data) throws XMLParseException
	{
		this(new String(data));
	}
	
	public XMLEasyCC(Element xml) throws Exception
	{
		//System.out.println(XMLUtil.toString(xml)  + "\n");
		setValues(xml);
		m_docTheEasyCC = XMLUtil.createDocument();
		m_docTheEasyCC.appendChild(XMLUtil.importNode(m_docTheEasyCC, xml, true));
	}

	public XMLEasyCC(XMLEasyCC a_copiedCc) throws XMLParseException
	{
		 m_lTransferredBytes = a_copiedCc.m_lTransferredBytes;
		 m_lAccountNumber = a_copiedCc.m_lAccountNumber;
		 m_id = a_copiedCc.m_id = 0;
		 m_priceCerts = (Hashtable)a_copiedCc.m_priceCerts.clone();
		 m_cascadeID =  a_copiedCc.m_cascadeID;
		 m_docTheEasyCC = XMLUtil.createDocument();
		 m_docTheEasyCC.appendChild(XMLUtil.importNode(m_docTheEasyCC,
													   a_copiedCc.m_docTheEasyCC.getDocumentElement(), true));
		 m_priceCertHashesConcatenated = a_copiedCc.m_priceCertHashesConcatenated;
		 m_strPIID = a_copiedCc.m_strPIID;
	 }

	private void setValues(Element element) throws XMLParseException
	{
		if(!element.getTagName().equals(XML_ELEMENT_NAME))
		{
			throw new XMLParseException("XMLEasyCC wrong xml root element name");
		}
		String strVersion=XMLUtil.parseAttribute(element,"version",null);
		if (strVersion==null ||
			!(strVersion.equals("1.2")||strVersion.equals("1.1")))
		{
			throw new XMLParseException("XMLEasyCC wrong version");
		}
		Element elem;
		elem = (Element) XMLUtil.getFirstChildByName(element, "AccountNumber");
		m_lAccountNumber = XMLUtil.parseValue(elem, 0l);
		elem = (Element) XMLUtil.getFirstChildByName(element, "TransferredBytes");
		m_lTransferredBytes = XMLUtil.parseValue(elem, -1l);
		elem = (Element) XMLUtil.getFirstChildByName(element, "PIID");
		m_strPIID = XMLUtil.parseValue(elem, (String)null);
		elem = (Element) XMLUtil.getFirstChildByName(element, "Cascade");
		m_cascadeID = XMLUtil.parseValue(elem, (String)null);
		Element elemPriceCerts;
		elemPriceCerts	= (Element) XMLUtil.getFirstChildByName(element, "PriceCertificates");
		Element elemHash; // = (Element) elemPriceCerts.getFirstChild();
		String curHash;
		String curId;
		int position;
		if (elemPriceCerts != null)
		{
			NodeList allHashes = elemPriceCerts.getElementsByTagName("PriceCertHash");
			for (int i = 0; i < allHashes.getLength(); i++)
			{
				elemHash = (Element) allHashes.item(i);
				curHash = XMLUtil.parseValue(elemHash, "abc");
				curId = XMLUtil.parseAttribute(elemHash, "id", "abc");
				if (curId.equals("abc"))
				{
					throw new XMLParseException("wrong or missing id of price certificate");
				}
				else
				{
					position = XMLUtil.parseAttribute(elemHash, "position", -1);
					if (position < 0)
					{
						m_bOldHashFormat = true;
					}
					m_priceCerts.put(new MixPosition(position, curId), curHash);
				}
			}
		}
		m_priceCertHashesConcatenated = 
			createConcatenatedPriceCertHashes(m_priceCerts, !m_bOldHashFormat);
		if (m_bOldHashFormat)
		{
			LogHolder.log(LogLevel.WARNING, LogType.PAY, "Found old hash format for CC: " + m_priceCertHashesConcatenated);
		}
	}
	
	private Element internal_toXmlElement(Document a_doc)
	{
		Element elemRoot = a_doc.createElement(XML_ELEMENT_NAME);
		elemRoot.setAttribute("version", "1.2");
		Element elem;

		elem = a_doc.createElement("TransferredBytes");
	/*	if (m_lTransferredBytes < 0)
		{
			XMLUtil.setValue(elem, "18399999999999999999");
		}
		else */
		{
			XMLUtil.setValue(elem, Long.toString(m_lTransferredBytes));
		}
		elemRoot.appendChild(elem);

		elem = a_doc.createElement("AccountNumber");
		XMLUtil.setValue(elem, Long.toString(m_lAccountNumber));
		elemRoot.appendChild(elem);

		elem = a_doc.createElement("PIID");
		if (m_strPIID != null)
		{
			XMLUtil.setValue(elem, m_strPIID);
		}
		elemRoot.appendChild(elem);

		elem = a_doc.createElement("Cascade");
		if (m_cascadeID != null)
		{
			XMLUtil.setValue(elem, m_cascadeID);
		}
		elemRoot.appendChild(elem);

		Element elemPriceCerts = a_doc.createElement("PriceCertificates");
		elemRoot.appendChild(elemPriceCerts);
		Enumeration certs = m_priceCerts.keys();
		String curHash;
		MixPosition curId;
		Element curElem;
		while(certs.hasMoreElements() )
		{
			curId = (MixPosition) certs.nextElement();
			curHash = (String) m_priceCerts.get(curId);
			curElem = a_doc.createElement("PriceCertHash");
			XMLUtil.setValue(curElem,curHash);
			XMLUtil.setAttribute(curElem,"id",curId.getId());
			if (!m_bOldHashFormat)
			{
				XMLUtil.setAttribute(curElem, "position", curId.getPosition());
			}
			elemPriceCerts.appendChild(curElem);
		}

		return elemRoot;
	}

//~ Methods ****************************************************************

	public String getPIID()
	{
		return m_strPIID;
	}

	/**
	 * sets the PI ID. This makes the signature invalid!
	 *
	 * @param Id of the payment instance
	 */
	public synchronized void setPIID(String a_piID)
	{
		m_strPIID = a_piID;
		m_docTheEasyCC = XMLUtil.createDocument();
		m_docTheEasyCC.appendChild(internal_toXmlElement(m_docTheEasyCC));
	}

	/**
	 * getId
	 * Warning: field is not set as long as the CC has not been inserted in the payment instance's database
	 * you only need it as db primary key, so there should be no need to call getId before the CC has been stored in the database
	 *
	 * @return int
	 */
	public int getId()
	{
		return m_id;
	}

	public void setId(int a_id)
	{
		m_id = a_id;
	}

	public void setCascadeID(String a_id)
	{
		m_cascadeID = a_id;
		m_docTheEasyCC = XMLUtil.createDocument();
		m_docTheEasyCC.appendChild(internal_toXmlElement(m_docTheEasyCC));
	}

	public long getAccountNumber()
	{
		return m_lAccountNumber;
	}

	public long getTransferredBytes()
	{
		return m_lTransferredBytes;
	}

	public Enumeration getMixIds()
	{
		//formerly: return m_priceCerts.keys();
		//caller will expect a list of SKIs, NOT the MixPosition objects in the hashtable, so we transform beforehand
		Enumeration mixPositions = m_priceCerts.keys();
		Hashtable keysOnly = new Hashtable();
		MixPosition curPos;
		String curId;
		while (mixPositions.hasMoreElements() )
		{
			curPos = (MixPosition) mixPositions.nextElement();
			curId = curPos.getId();
			keysOnly.put(curId,m_priceCerts.get(curPos)); //we dont need the value, but it may not be null
		}
		return keysOnly.keys();
	}

	/**
	 * getAIName: returns the subjectkeyidentifier of the first Mix in the Cascade
	 * @deprecated : use getConcatenatedPriceCertHashes() instead
	 *
	 * @return String
	 */
	public String getCascadeID() {
		return m_cascadeID;
	}

	/**
	 * getPriceCertElements
	 *
	 * @return Hashtable ( key: MixCascade.MixPosition (contains subjectkeyidentifier of mix and position of mix in cascade)
	 *                     value: hash of price certificate )
	 */
	public Hashtable getPriceCertHashes()
	{
		return (Hashtable)m_priceCerts.clone();
	}

	public String getConcatenatedPriceCertHashes()
	{
		return m_priceCertHashesConcatenated;
	}

	public static String createConcatenatedPriceCertHashes(
			Hashtable priceCerts, boolean newFormat)
	{
		// sort hashes after their position in cascade

		String[] ids, hashes;
		Enumeration enumer;
		Object currentKey;
		StringBuffer priceCertHashesConcatenated = new StringBuffer();
		
		if (priceCerts != null)
		{
			synchronized (priceCerts)
			{
				ids = new String[priceCerts.size()];
				hashes = new String[priceCerts.size()];

				enumer = priceCerts.keys();
				for (int i = 0; i < priceCerts.size(); i++)
				{
					currentKey = enumer.nextElement();
					if (!newFormat)
					{
						// the position does not tell anything about the real position...
						ids[i] = ( (MixPosition) currentKey).getId();
					}
					else
					{
						ids[i] = Integer.toString( ( (MixPosition) currentKey).getPosition());
					}
					hashes[i] = priceCerts.get(currentKey).toString();
				}
				if (!newFormat)
				{
					Util.sort(hashes, ids); //sort alphabetically
				}
				else
				{
					Util.sort(ids, hashes); //sort by position
				}

				for (int i = 0; i < hashes.length; i++)
				{
					priceCertHashesConcatenated.append(hashes[i]);
				}
			}
		}
		return priceCertHashesConcatenated.toString();
	}
	
	/*private void createConcatenatedPriceCertHashes()
	{
		// sort hashes after their position in cascade

		String[] ids, hashes;
		Enumeration enumer;
		Object currentKey;

		if (m_priceCerts != null)
		{
			synchronized (m_priceCerts)
			{
				ids = new String[m_priceCerts.size()];
				hashes = new String[m_priceCerts.size()];

				enumer = m_priceCerts.keys();
				for (int i = 0; i < m_priceCerts.size(); i++)
				{
					currentKey = enumer.nextElement();
					if (m_bOldHashFormat)
					{
						// the position does not tell anything about the real position...
						ids[i] = ( (MixPosition) currentKey).getId();
					}
					else
					{
						ids[i] = Integer.toString( ( (MixPosition) currentKey).getPosition());
					}
					hashes[i] = m_priceCerts.get(currentKey).toString();
				}
				if (m_bOldHashFormat)
				{
					Util.sort(hashes, ids); //sort alphabetically
				}
				else
				{
					Util.sort(ids, hashes); //sort by psoition
				}

				m_priceCertHashesConcatenated = "";
				for (int i = 0; i < hashes.length; i++)
				{
					m_priceCertHashesConcatenated += hashes[i];
				}
			}
		}
	}*/

	public int getNrOfPriceCerts()
	{
		return m_priceCerts.size();
	}

	/**
	 * setPriceCerts: inserts the hash values of known PriceCertificates
	 *
	 * @param a_priceCertHashes Vector
	 */
	public void setPriceCerts(Hashtable a_priceCertHashes)
	{
		m_bOldHashFormat = false;
		m_priceCerts = a_priceCertHashes;
		m_priceCertHashesConcatenated = 
			createConcatenatedPriceCertHashes(m_priceCerts, !m_bOldHashFormat);
		if (m_bOldHashFormat)
		{
			LogHolder.log(LogLevel.WARNING, LogType.PAY, "Found old hash format for CC: " + m_priceCertHashesConcatenated);
		}

		m_docTheEasyCC = XMLUtil.createDocument();
		m_docTheEasyCC.appendChild(internal_toXmlElement(m_docTheEasyCC));
	}


	/** this makes the signature invalid! */
	public synchronized void addTransferredBytes(long plusBytes)
	{
		m_lTransferredBytes += plusBytes;
		m_docTheEasyCC = XMLUtil.createDocument();
		m_docTheEasyCC.appendChild(internal_toXmlElement(m_docTheEasyCC));
	}

	/**
	 * setTransferredBytes. this makes the signature invalid!
	 *
	 * @param numBytes long
	 */
	public synchronized void setTransferredBytes(long numBytes)
	{
		m_lTransferredBytes = numBytes;
		m_docTheEasyCC = XMLUtil.createDocument();
		m_docTheEasyCC.appendChild(internal_toXmlElement(m_docTheEasyCC));
	}

	public boolean sign(IMyPrivateKey key)
	{
		try
		{
			XMLSignature.sign(m_docTheEasyCC, key);
			return true;
		}
		catch (Exception e)
		{
			return false;
		}
	}

	public boolean verify(IMyPublicKey key)
	{
		try
		{
			return XMLSignature.verifyFast(m_docTheEasyCC, key);
		}
		catch (Throwable t)
		{
			return false;
		}
	}

	public Document getDocument()
	{
		return m_docTheEasyCC;
	}
	
	public synchronized Element toXmlElement(Document a_doc)
	{
		try
		{
			return (Element) XMLUtil.importNode(a_doc, m_docTheEasyCC.getDocumentElement(), true);
		}
		catch (Exception e)
		{
			return null;
		}
	}
	
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + (m_bOldHashFormat ? 1231 : 1237);
		result = prime * result + ((m_cascadeID == null) ? 0 : m_cascadeID.hashCode());
		result = prime * result + (int) (m_lAccountNumber ^ (m_lAccountNumber >>> 32));
		result = prime * result + (int) (m_lTransferredBytes ^ (m_lTransferredBytes >>> 32));
		result = prime
				* result
				+ ((m_priceCertHashesConcatenated == null) ? 0 : m_priceCertHashesConcatenated
						.hashCode());
		result = prime * result + ((m_strPIID == null) ? 0 : m_strPIID.hashCode());
		return result;
	}

	public boolean equals(Object obj)
	{
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		XMLEasyCC other = (XMLEasyCC) obj;
		if (m_bOldHashFormat != other.m_bOldHashFormat)
			return false;
		if (m_cascadeID == null)
		{
			if (other.m_cascadeID != null)
				return false;
		}
		else if (!m_cascadeID.equals(other.m_cascadeID))
			return false;
		if (m_lAccountNumber != other.m_lAccountNumber)
			return false;
		if (m_lTransferredBytes != other.m_lTransferredBytes)
			return false;
		if (m_priceCertHashesConcatenated == null)
		{
			if (other.m_priceCertHashesConcatenated != null)
				return false;
		}
		else if (!m_priceCertHashesConcatenated.equals(other.m_priceCertHashesConcatenated))
			return false;
		if (m_strPIID == null)
		{
			if (other.m_strPIID != null)
				return false;
		}
		else if (!m_strPIID.equals(other.m_strPIID))
			return false;
		return true;
	}
}
