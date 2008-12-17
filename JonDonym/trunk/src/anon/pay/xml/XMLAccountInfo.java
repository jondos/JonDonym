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

import java.util.Enumeration;
import java.util.Hashtable;

import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;


import org.w3c.dom.Document;
import org.w3c.dom.Element;

import anon.util.IXMLEncodable;
import anon.util.XMLUtil;

/**
 * This class represents an XML AccountInfo structure
 * (XMLBalance plus cost confirmations for the account)
 *
 *
 * The following XML structure is used:
 * <pre>
 * &lt;AccountInfo&gt;
 *    &lt;Balance&gt;
 *       &lt;AccountNumber&gt; ..&lt;/AccountNumber&gt;
 *       &lt;Deposit&gt;...&lt;/Deposit&gt;
 *       &lt;Spent&gt;....&lt;/Spent&gt;
 *       &lt;Validtime&gt;...&lt;/Validtime&gt;
 *       &lt;Timestamp&gt;...&lt;/Timestamp&gt;
 *       &lt;Signature&gt; //Unterschrift der BI
 *          ...
 *       &lt;/Signature&gt;
 *    &lt;/Balance&gt;
 *    &lt;CostConfirmations&gt; //Kostenbest\u00E4tigungen, die
 *		                           von den einzelnen AI's
 *		                           abgerechnet wurden
 *       &lt;CC&gt;...&lt;/CC&gt;
 *       &lt;CC&gt;...&lt;/CC&gt;
 *    &lt;/CostConfirmations&gt;
 * &lt;/AccountInfo&gt;
 * </pre>
 *
 * @author Bastian Voigt
 */
public class XMLAccountInfo implements IXMLEncodable //extends XMLDocument
{
	//~ Instance fields ********************************************************

	public static final String XML_ELEMENT_NAME_COST_CONFIRMATIONS = "CostConfirmations";
	
	/** the balance certificate */
	private XMLBalance m_balance = null;

	/**
	 * a collection of costconfirmations (one for each mixcascade
	 * that was used with this account)
	 */
	private Hashtable m_costConfirmations = new Hashtable();

	//~ Constructors ***********************************************************

	public XMLAccountInfo(XMLBalance bal)
	{
		m_balance = bal;
	}

	/**
	 * Creates an AccountInfo object from a string.
	 */
	public XMLAccountInfo(String xml) throws Exception
	{
		Document doc = XMLUtil.toXMLDocument(xml);
		setValues(doc.getDocumentElement());
	}

	public XMLAccountInfo()
	{}

	/**
	 * Creates a Balance from  an existing XML docuemnt
	 *
	 * @param xml the node that represents the Balance
	 * @param verifier JAPSignature must be initialized and ready to verify XML (or null)
	 * @throws Exception on invalid xml format or invalid signature
	 */
	public XMLAccountInfo(Element xml) throws Exception
	{
		setValues(xml);
	}

	//~ Methods ****************************************************************
	public Element toXmlElement(Document a_doc)
	{
		Element elemRoot = a_doc.createElement("AccountInfo");
		elemRoot.setAttribute("version", "1.1");
		Element elem;

		// add balance
		elem = m_balance.toXmlElement(a_doc);
		elemRoot.appendChild(elem);

		// add CCs
		Element elemCCs = a_doc.createElement("CostConfirmations");
		elemRoot.appendChild(elemCCs);
		synchronized (m_costConfirmations)
		{
			Enumeration enumer = m_costConfirmations.elements();
			XMLEasyCC cc;
			while (enumer.hasMoreElements())
			{
				cc = (XMLEasyCC) enumer.nextElement();
				elem = cc.toXmlElement(a_doc);
				elemCCs.appendChild(elem);
			}
		}
		return elemRoot;
	}

	/**
	 * Adds a cost confirmation xml structure to the accountinfo.
	 * Note: If a cost confirmation for the same AI is already present
	 * it will be overwritten. Only adds the CC if it has the same amount or
	 * more bytes than the previous one.
	 *
	 * @param xmlCC XMLEasyCC
	 * @return the difference in the number of bytes between the old and the
	 * new costconfirmation for the same AI
	 */
	public long addCC(XMLEasyCC a_cc) throws Exception
	{
		long oldBytes = 0;
		XMLEasyCC cc;
		synchronized (m_costConfirmations)
		{
			cc = (XMLEasyCC)m_costConfirmations.get(a_cc.getConcatenatedPriceCertHashes());			
			if (cc != null)
			{
				oldBytes = cc.getTransferredBytes();
			}
			if (a_cc.getTransferredBytes() >= oldBytes)
			{
				// do not add old cost confirmations
				m_costConfirmations.put(a_cc.getConcatenatedPriceCertHashes(), a_cc);
			}
		}
		return a_cc.getTransferredBytes() - oldBytes;
	}

	private void setValues(Element elemRoot) throws Exception
	{
		if (!elemRoot.getTagName().equals("AccountInfo"))
		{
			LogHolder.log(LogLevel.EXCEPTION, LogType.PAY, "invalid XML structure: "+XMLUtil.toString(elemRoot));
			throw new Exception("XMLAccountInfo wrong XML structure");
		}
		Element elemBalance = (Element) XMLUtil.getFirstChildByName(elemRoot, "Balance");
		m_balance = new XMLBalance(elemBalance);

		Element elemCCs = (Element) XMLUtil.getFirstChildByName(elemRoot, XML_ELEMENT_NAME_COST_CONFIRMATIONS);
		Element elemCC = (Element) XMLUtil.getFirstChildByName(elemCCs, XMLEasyCC.XML_ELEMENT_NAME);
		XMLEasyCC cc = null;
		while (elemCC != null)
		{
			cc = new XMLEasyCC(elemCC);
			m_costConfirmations.put(cc.getConcatenatedPriceCertHashes(), cc);
			elemCC = (Element) XMLUtil.getNextSiblingByName(elemCC, XMLEasyCC.XML_ELEMENT_NAME);
		}
	}

	public XMLBalance getBalance()
	{
		return m_balance;
	}

	/**
	 * getCC - returns the cost confirmation with the specified aiName
	 *
	 * @param string String
	 * @return XMLEasyCC
	 */
	public XMLEasyCC getCC(String priceCertHash)
	{
		return (XMLEasyCC)m_costConfirmations.get(priceCertHash);
	}

	/**
	 * Returns an enumeration of all the available cost confirmations for
	 * this account.
	 * @return Enumeration
	 */
	public Enumeration getCCs()
	{
		return ((Hashtable)m_costConfirmations.clone()).elements();
	}
	
	/**
	 * returns the overall sum of all the transfered bytes of all 
	 * CCs belonging to this account.
	 * @return
	 */
	public long getAllCCsTransferredBytes()
	{
		long sum = 0l;
		Enumeration e = m_costConfirmations.elements();
		while(e.hasMoreElements())
		{
			sum += ((XMLEasyCC) e.nextElement()).getTransferredBytes();
		}
		return sum;
	}
	
	/**
	 * setBalance
	 *
	 * @param b1 XMLBalance
	 */
	public void setBalance(XMLBalance b1)
	{
		m_balance = b1;
	}

	/**
	 * XMLAccountInfo
	 *
	 * @param document Document
	 */
	public XMLAccountInfo(Document document) throws Exception
	{
		setValues(document.getDocumentElement());
	}
}
