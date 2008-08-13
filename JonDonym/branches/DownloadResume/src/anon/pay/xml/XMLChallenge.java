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

import anon.util.Base64;
import anon.util.IXMLEncodable;
import anon.util.XMLUtil;

public class XMLChallenge implements IXMLEncodable
{
	//~ Constructors ***********************************************************

	private byte[] m_arbChallenge;
	private int m_prepaidBytes;
	public static final String XML_ELEMENT_NAME = "Challenge";

	public XMLChallenge(String xml) throws Exception
	{
		Document doc = XMLUtil.toXMLDocument(xml);
		setValues(doc.getDocumentElement());
	}

	/**
	 * XMLChallenge
	 *
	 * @param element Element
	 */
	public XMLChallenge(Element element) throws Exception
	{
		setValues(element);
	}

	public XMLChallenge(Document doc) throws Exception
	{
		setValues(doc.getDocumentElement());
	}


	/** Note: this does not parse XML, but sets the challenge byte-array directly... */
	public XMLChallenge(byte[] data)
	{
		m_arbChallenge = data;
	}

	//~ Methods ****************************************************************

	private void setValues(Element elemRoot) throws Exception
	{
		if (!elemRoot.getTagName().equals(XML_ELEMENT_NAME))
		{
			throw new Exception("XMLChallenge wrong XML structure");
		}
		Element element = (Element) XMLUtil.getFirstChildByName(elemRoot, "DontPanic");
		m_arbChallenge = Base64.decode(XMLUtil.parseValue(element, ""));
		m_prepaidBytes = XMLUtil.parseValue(XMLUtil.getFirstChildByName(elemRoot, "PrepaidBytes"), 0);
	}

	public int getPrepaidBytes()
	{
		return m_prepaidBytes;
	}

	public byte[] getChallengeForSigning()
	{   // new challenge
		byte[] challenge = new byte[m_arbChallenge.length];
		System.arraycopy(m_arbChallenge, 0, challenge, 0, challenge.length);
		return challenge;
	}

	public byte[] getChallengeForCaptcha()
	{
		String tmp = "<DontPanic>" + Base64.encodeBytes(m_arbChallenge) + "</DontPanic>";
		return tmp.getBytes();
	}

	public Element toXmlElement(Document a_doc)
	{
		Element elemRoot = a_doc.createElement(XML_ELEMENT_NAME);
		Element elemChallenge = a_doc.createElement("DontPanic");
		elemRoot.appendChild(elemChallenge);
		XMLUtil.setValue(elemChallenge, Base64.encodeBytes(m_arbChallenge));
		return elemRoot;
	}
}
