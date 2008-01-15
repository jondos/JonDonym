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

import java.io.ByteArrayInputStream;

import org.w3c.dom.CharacterData;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import anon.util.IXMLEncodable;
import anon.util.XMLUtil;

/**
 * @todo document :-)
 * <p>\u00DCberschrift: </p>
 * <p>Beschreibung: </p>
 * <p>Copyright: Copyright (c) 2001</p>
 * <p>Organisation: </p>
 * @author not attributable
 * @version 1.0
 */
public class XMLDescription implements IXMLEncodable // extends XMLDocument
{
	//~ Constructors ***********************************************************
	private String m_strDescription;


	public XMLDescription(byte[] data) throws Exception
	{
		Document doc = XMLUtil.toXMLDocument(data);
		setValues(doc);
	}

	public XMLDescription(String data) throws Exception
	{
		m_strDescription = data;
	}

	//~ Methods ****************************************************************

	private void setValues(Document doc) throws Exception
	{
		Element element = doc.getDocumentElement();
		if (!element.getTagName().equals("Description"))
		{
			throw new Exception("XMLDescription wrong xml structure");
		}

		CharacterData chdata = (CharacterData) element.getFirstChild();
		m_strDescription = chdata.getData();
	}

	public Element toXmlElement(Document a_doc)
	{
		Element elemRoot = a_doc.createElement("Description");
		XMLUtil.setValue(elemRoot, m_strDescription);
		return elemRoot;
	}


	public String getDescription()
	{return m_strDescription;}
}
