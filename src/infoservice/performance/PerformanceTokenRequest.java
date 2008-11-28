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
import anon.infoservice.InfoServiceDBEntry;
import anon.util.IXMLEncodable;
import anon.util.XMLParseException;
import anon.util.XMLUtil;

/**
 * Java representation of a token request.<br />
 * <br />
 * An inquiring <code>InfoService</code> should send a <code>PerformanceTokenRequest</code>
 * to another info service (or itself) to request a token 
 * for the performance evaluation
 * 
 * @see infoservice.performance.PerformanceToken
 * 
 * @author Christian Banse
 */
public class PerformanceTokenRequest implements IXMLEncodable 
{
	/**
	 * The token request XML element name.
	 */
	public static final String XML_ELEMENT_NAME = "PerformanceTokenRequest";
	
	/**
	 * The info service id XML element name. 
	 */
	public static final String XML_ELEMENT_INFOSERVICE_ID = "InfoServiceId";

	/**
	 * The info service id
	 */
	private String m_infoServiceId;
	
	/**
	 * Constructs a new <code>PerformanceTokenRequest</code> from its XML representation
	 * 
	 * @param a_node The XML node.
	 * 
	 * @throws XMLParseException If the XML data is invalid or if an invalid info service is specified.
	 */
	public PerformanceTokenRequest(Element a_node) throws XMLParseException
	{
		XMLUtil.assertNodeName(a_node, XML_ELEMENT_NAME);
		
		if(!SignatureVerifier.getInstance().verifyXml(a_node, SignatureVerifier.DOCUMENT_CLASS_INFOSERVICE))
		{
			throw new XMLParseException("Could not verify XML Document.");
		}
		
		Node isId = XMLUtil.getFirstChildByName(a_node, XML_ELEMENT_INFOSERVICE_ID);
		
		XMLUtil.assertNotNull(isId);
		
		m_infoServiceId = XMLUtil.parseValue(isId, "");
		
		// check against available infoservices
		/*
		InfoServiceDBEntry infoService = (InfoServiceDBEntry) Database.getInstance(InfoServiceDBEntry.class).getEntryById(m_infoServiceId);
		
		if(infoService == null)
		{
			throw new XMLParseException("Request did not specify a valid infoservice id.");
		}*/
	}
	
	/**
	 * Constructs a new <code>PerformanceTokenRequest</code> with a given info service id.
	 * 
	 * @param a_infoServiceId The id of the requesting info service.
	 */
	public PerformanceTokenRequest(String a_infoServiceId)
	{
		m_infoServiceId = a_infoServiceId;
	}
	
	public Element toXmlElement(Document a_doc) 
	{
		Element elem = a_doc.createElement(XML_ELEMENT_NAME);
		
		Element elemISId = a_doc.createElement(XML_ELEMENT_INFOSERVICE_ID);
		XMLUtil.setValue(elemISId, m_infoServiceId);
		
		elem.appendChild(elemISId);

		return elem;
	}
	
	/**
	 * Returns the id of the inquiring info service.
	 * 
	 * @return The inquiring info service id.
	 */
	public String getInfoServiceId()
	{
		return m_infoServiceId;
	}
}
