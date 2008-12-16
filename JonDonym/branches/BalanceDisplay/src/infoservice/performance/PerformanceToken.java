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

import java.util.Random;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.Document;

import anon.infoservice.AbstractDatabaseEntry;
import anon.util.XMLParseException;
import anon.util.XMLUtil;
import anon.util.IXMLEncodable;
import anon.util.Base64;

/**
 * A single-use token used by the <code>PerformanceRequestHandler</code> to
 * check if a <code>PerformanceRequest</code> is sent by a valid 
 * <code>InfoService</code>.
 * 
 * @see infoservice.performance.PerformanceRequest
 * @see infoservice.performance.PerformanceRequestHandler
 *  
 * @author Christian Banse
 */
public class PerformanceToken extends AbstractDatabaseEntry implements IXMLEncodable
{
	/**
	 * Time-to-live for the performance token.
	 */
	public static final int PERFORMANCE_TOKEN_TTL = 120*1000;
	
	/**
	 * Length of the token.
	 */
	public static final int PERFORMANCE_TOKEN_BASE_LENGTH = 16;
	
	/**
	 * The token XML element name.
	 */
	public static final String XML_ELEMENT_NAME = "PerformanceToken";
	
	/**
	 * The token id XML element name.
	 */
	public static final String XML_ELEMENT_ID = "Id";
	
	/**
	 * The random generator.
	 */
	private static Random ms_rnd = new Random();
	
	/**
	 * The token id.
	 */
	private String m_id;
	
	/**
	 * Time of last update.
	 */
	private long m_lastUpdate;
	
	/**
	 * Constructs a new random token.
	 */
	public PerformanceToken() 
	{
		super(System.currentTimeMillis() + PERFORMANCE_TOKEN_TTL);
		
		m_lastUpdate = System.currentTimeMillis();
		
		byte[] token = new byte[PERFORMANCE_TOKEN_BASE_LENGTH];
		ms_rnd.nextBytes(token);
		m_id = Base64.encode(token, false) + Base64.encodeString(String.valueOf(System.currentTimeMillis()));
	}
	
	/**
	 * Constructs a <code>PerformanceToken</code> object from its XML representation.
	 * 
	 * @param a_node
	 * @throws XMLParseException
	 */
	public PerformanceToken(Element a_node) throws XMLParseException
	{
		super(-1);
		m_lastUpdate = -1;
		
		XMLUtil.assertNodeName(a_node, XML_ELEMENT_NAME);
		
		Node token = XMLUtil.getFirstChildByName(a_node, XML_ELEMENT_ID);
		
		XMLUtil.assertNotNull(token);
		
		m_id = XMLUtil.parseValue(token, "");
	}
	
	/**
	 * Returns the token id.
	 * 
	 * @return The token id.
	 */
	public String getId() 
	{
		return m_id;
	}

	public long getLastUpdate() 
	{
		return m_lastUpdate;
	}


	public long getVersionNumber() 
	{
		return m_lastUpdate;
	}
	
	/**
	 * Returns the XML representation of the token.
	 * 
	 * @return The XML representation of the token.
	 */
	public Element toXmlElement(Document a_doc)
	{
		Element elem = a_doc.createElement(XML_ELEMENT_NAME);
		
		Element elemToken = a_doc.createElement(XML_ELEMENT_ID);
		XMLUtil.setValue(elemToken, m_id);
		
		elem.appendChild(elemToken);
		
		return elem;
	}
}
