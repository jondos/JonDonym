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

public class XMLResponse implements IXMLEncodable
{
	private byte[] m_arbResponse;

	/** Creates a XMLResponse from the XML representation **/
	public XMLResponse(String xml) throws Exception
	{
        Document doc = XMLUtil.toXMLDocument(xml);
		setValues(doc.getDocumentElement());
	}

	/** Creates an XML Response from the supplied signature bytes
	 *
	 */
	public XMLResponse(byte[] sig) throws Exception
	{
		m_arbResponse = sig;
	}

	private void setValues(Element elemRoot) throws Exception
	{
//		Element element = m_theDocument.getDocumentElement();
		if (!elemRoot.getTagName().equals("Response"))
		{
			throw new Exception("XMLResponse wrong xml structure");
		}
		String strBase64Response = XMLUtil.parseValue(elemRoot, "");
		m_arbResponse = Base64.decode(strBase64Response);
	}

	public byte[] getResponse()
	{
		return m_arbResponse;
	}

	public Element toXmlElement(Document a_doc)
	{
		Element elemRoot = a_doc.createElement("Response");
		XMLUtil.setValue(elemRoot, Base64.encodeBytes(m_arbResponse));
		return elemRoot;
	}
}
