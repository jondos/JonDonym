/*
 Copyright (c) 2007, JonDos GmbH
 All rights reserved.
 Redistribution and use in source and binary forms, with or without modification,
 are permitted provided that the following conditions are met:

  - Redistributions of source code must retain the above copyright notice,
 this list of conditions and the following disclaimer.

  - Redistributions in binary form must reproduce the above copyright notice,
 this list of conditions and the following disclaimer in the documentation and/or
 other materials provided with the distribution.

  - Neither the name of the JonDos GmbH nor the names of its contributors
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
package anon.infoservice;

import anon.util.IXMLEncodable;
import anon.util.XMLUtil;
import org.w3c.dom.Element;
import org.w3c.dom.Document;
import anon.util.XMLParseException;

/**
 * Stored all message db entries deleted by the user.
 *
 * @author Rolf Wendolsky
 */
public abstract class AbstractMarkedMessageIDDBEntry extends AbstractDatabaseEntry implements IXMLEncodable
{
	private long m_serial;
	private String m_id;
	private long m_creationTimeStamp;

	public AbstractMarkedMessageIDDBEntry(MessageDBEntry a_messageEntry)
	{
		super(Long.MAX_VALUE);
		m_serial = a_messageEntry.getVersionNumber();
		m_id = a_messageEntry.getId();
		m_creationTimeStamp = a_messageEntry.getLastUpdate();
	}

	public AbstractMarkedMessageIDDBEntry(Element a_messageEntryElement) throws XMLParseException
	{
		super(Long.MAX_VALUE);
		XMLUtil.assertNodeName(a_messageEntryElement, getXmlElementName());

		m_serial = XMLUtil.parseAttribute(
				  a_messageEntryElement, AbstractDistributableDatabaseEntry.XML_ATTR_SERIAL, -1);
		m_id = XMLUtil.parseAttribute(a_messageEntryElement, XML_ATTR_ID, null);
		m_creationTimeStamp = XMLUtil.parseAttribute(
				  a_messageEntryElement, AbstractDistributableDatabaseEntry.XML_ATTR_LAST_UPDATE,
				System.currentTimeMillis());
		if (m_serial < 0 || m_id == null)
		{
			throw new XMLParseException(getXmlElementName(), "Illegal attribute values!");
		}
	}

	public abstract String getXmlElementName();

	public final long getVersionNumber()
	{
		return m_serial;
	}

	public final String getId()
	{
		return m_id;
	}

	public final long getLastUpdate()
	{
		return m_creationTimeStamp;
	}

	public final Element toXmlElement(Document a_doc)
	{
		if (a_doc == null)
		{
			return null;
		}
		Element elem = a_doc.createElement(getXmlElementName());
		XMLUtil.setAttribute(elem, AbstractDistributableDatabaseEntry.XML_ATTR_SERIAL, m_serial);
		XMLUtil.setAttribute(elem, XML_ATTR_ID, m_id);
		XMLUtil.setAttribute(elem,
							 AbstractDistributableDatabaseEntry.XML_ATTR_LAST_UPDATE, m_creationTimeStamp);

		return elem;
	}
}
