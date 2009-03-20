/*
 Copyright (c) 2008 The JAP-Team, JonDos GmbH

 All rights reserved.

 Redistribution and use in source and binary forms, with or without modification, 
 are permitted provided that the following conditions are met:

    * Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
    * Redistributions in binary form must reproduce the above copyright notice,
       this list of conditions and the following disclaimer in the documentation and/or
       other materials provided with the distribution.
    * Neither the name of the University of Technology Dresden, Germany, nor the name of
       the JonDos GmbH, nor the names of their contributors may be used to endorse or
       promote products derived from this software without specific prior written permission.

 THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE REGENTS OR
 CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package infoservice.performance;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import anon.crypto.SignatureVerifier;
import anon.infoservice.Database;
import anon.util.IXMLEncodable;
import anon.util.XMLParseException;
import anon.util.XMLUtil;

/**
 * Java representation of a performance evaluation request.<br />
 * <br />
 * An inquiring <code>InfoService</code> should send a <code>PerformanceRequest</code>
 * to another info service (or itself) to request random data
 * of required size. This requires a valid <code>PerformanceToken</code>.
 * 
 * @see infoservice.performance.PerformanceToken
 * 
 * @author Christian Banse
 */
public class PerformanceRequest implements IXMLEncodable 
{
	/**
	 * The XML element name
	 */
	public static final String XML_ELEMENT_NAME = "PerformanceRequest";
	
	/**
	 * The token id XML element name
	 */
	public static final String XML_ELEMENT_TOKEN_ID = "TokenId";
	
	/**
	 * The data size XML element name
	 */
	public static final String XML_ELEMENT_DATASIZE = "DataSize";
	
	/**
	 * The token id.
	 */
	private String m_tokenId;
	
	/**
	 * The data size.
	 */
	private int m_dataSize;
	
	/**
	 * Constructs a new <code>PeformanceRequest</code> object from XML data.
	 * 
	 * @param a_node The XML node.
	 * @throws XMLParseException If the XML data or the token id was invalid.
	 */
	public PerformanceRequest(Element a_node) throws XMLParseException
	{
		// check if the element's name matches the required name
		XMLUtil.assertNodeName(a_node, XML_ELEMENT_NAME);
		
		// check if it has a valid info service signature
		if(!SignatureVerifier.getInstance().verifyXml(a_node, SignatureVerifier.DOCUMENT_CLASS_INFOSERVICE))
		{
			throw new XMLParseException("Could not verify XML Document.");
		}
		
		// extract the tokenId
		Node nodeToken = XMLUtil.getFirstChildByName(a_node, XML_ELEMENT_TOKEN_ID);
		XMLUtil.assertNotNull(nodeToken);
		m_tokenId = XMLUtil.parseValue(nodeToken, "");
		
		// check against available tokens
		PerformanceToken token = (PerformanceToken) Database.getInstance(PerformanceToken.class).getEntryById(m_tokenId);
		
		if(token == null)
		{
			throw new XMLParseException("Request did not specify a valid token.");
		}

		// extract the data size
		Node nodeDataSize = XMLUtil.getFirstChildByName(a_node, XML_ELEMENT_DATASIZE);
		XMLUtil.assertNotNull(nodeDataSize);
		m_dataSize = XMLUtil.parseValue(nodeDataSize, -1);		
	}
	
	/**
	 * Constructs a new <code>PerformanceRequest</code> object 
	 * from a valid token id and dataSize
	 * 
	 * @param a_tokenId A valid token id.
	 * @param a_dataSize The required data size.
	 */
	public PerformanceRequest(String a_tokenId, int a_dataSize)
	{
		m_tokenId = a_tokenId;
		m_dataSize = a_dataSize;
	}
	
	/**
	 * Generates the XML representation of the <code>PerformanceRequest</code> object
	 */
	public Element toXmlElement(Document a_doc) 
	{
		Element elem = a_doc.createElement(XML_ELEMENT_NAME);
		
		Element elemTokenId = a_doc.createElement(XML_ELEMENT_TOKEN_ID);
		XMLUtil.setValue(elemTokenId, m_tokenId);
		
		Element elemDataSize = a_doc.createElement(XML_ELEMENT_DATASIZE);
		XMLUtil.setValue(elemDataSize, m_dataSize);
		
		elem.appendChild(elemTokenId);
		elem.appendChild(elemDataSize);
		
		return elem;
	}
	
	/**
	 * Returns the token id used by the request.
	 * 
	 * @return The token id used by the request.
	 */
	public String getTokenId()
	{
		return m_tokenId;
	}
	
	/**
	 * Returns the data size of the request.
	 * 
	 * @return The data size of the request.
	 */
	public int getDataSize()
	{
		return m_dataSize;
	}
}
