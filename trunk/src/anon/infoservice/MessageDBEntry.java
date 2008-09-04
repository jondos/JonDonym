/*
 Copyright (c) 2000 - 2006, The JAP-Team
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
package anon.infoservice;

import java.net.MalformedURLException;
import java.net.URL;
import java.security.SignatureException;
import java.util.Hashtable;
import java.util.Locale;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import anon.crypto.SignatureVerifier;
import anon.util.Base64;
import anon.util.XMLParseException;
import anon.util.XMLUtil;
import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;
import anon.util.URLDecoder;

/**
 * Used to send messages to JAP.
 * @author Rolf Wendolsky
 */
public class MessageDBEntry extends AbstractDistributableDatabaseEntry implements IDistributable
{
	public static final String XML_ELEMENT_CONTAINER_NAME = "Messages";
	public static final String XML_ELEMENT_NAME = "Message";

	public static final String HTTP_REQUEST_STRING = "/messages";
	public static final String HTTP_SERIALS_REQUEST_STRING = "/messageserials";

	public static final String PROPERTY_NAME = "messageFileName";

	public static final String POST_FILE = "/message";

	private static final String XML_TEXT = "MessageText";
	private static final String XML_URL = "MessageURL";
	private static final String XML_ATTR_LANG = "lang";
	private static final String XML_ATTR_POPUP = "popup";
	private static final String XML_ATTR_ENCODING = "encoding";
	private static final String XML_ATTR_FREE = "free"; // show on free cascades only
	private static final String XML_ELEM_POPUP_TEXT = "MessagePopupText";
	private static final String ENCODING_URL = "url";
	private static final String ENCODING_BASE64 = "base64"; //default

	private static final long TIMEOUT = 7 * 24 * 60 * 60 * 1000L; // one week

	private int m_externalIdentifier;
	private long m_serial;
	private long m_lastUpdate;
	private boolean m_bIsDummy;
	private boolean m_bFree;
	private boolean m_bShowPopup;
	private String m_id;
	private Element m_xmlDescription;

	private Hashtable m_hashText = new Hashtable();
	private Hashtable m_hashPopupText = new Hashtable();
	private Hashtable m_hashUrl = new Hashtable();

	public MessageDBEntry(Element a_xmlElement) throws XMLParseException, SignatureException
	{
		super(System.currentTimeMillis() +  TIMEOUT);
		XMLUtil.assertNodeName(a_xmlElement, XML_ELEMENT_NAME);
		if (SignatureVerifier.getInstance().getVerifiedXml(
			  a_xmlElement, SignatureVerifier.DOCUMENT_CLASS_INFOSERVICE) == null)
		{
			throw new SignatureException();
		}

		m_serial = XMLUtil.parseAttribute(a_xmlElement, XML_ATTR_SERIAL, Long.MIN_VALUE);
		m_id = XMLUtil.parseAttribute(a_xmlElement, XML_ATTR_ID, null);
		m_bShowPopup = XMLUtil.parseAttribute(a_xmlElement, XML_ATTR_POPUP, false);
		m_bFree = XMLUtil.parseAttribute(a_xmlElement, XML_ATTR_FREE, false);

		if (m_id == null)
		{
			throw new XMLParseException("No id given!");
		}

		m_bIsDummy = parseTextNodes(a_xmlElement.getElementsByTagName(XML_TEXT), m_hashText);
		if (!m_bIsDummy)
		{
			String content, lang;
			NodeList textNodes = a_xmlElement.getElementsByTagName(XML_URL);
			for (int i = 0; i < textNodes.getLength(); i++)
			{
				content = XMLUtil.parseValue(textNodes.item(i), null);
				lang = XMLUtil.parseAttribute(textNodes.item(i), XML_ATTR_LANG, "en");
				if (content != null)
				{
					try
					{
						m_hashUrl.put(lang, new URL(content));
					}
					catch (MalformedURLException ex1)
					{
						// invalid url
						continue;
					}
				}
			}
			parseTextNodes(a_xmlElement.getElementsByTagName(XML_ELEM_POPUP_TEXT), m_hashPopupText);
		}

		m_lastUpdate = XMLUtil.parseValue(XMLUtil.getFirstChildByName(a_xmlElement, XML_LAST_UPDATE), -1L);
		if (m_lastUpdate == -1)
		{
			m_lastUpdate = System.currentTimeMillis();
			//throw (new Exception("JAPMinVersion: Constructor: No LastUpdate node found."));
		}

		m_xmlDescription = a_xmlElement;
	}

	public URL getURL(Locale a_locale)
	{
		if (a_locale == null)
		{
			return null;
		}

		URL url = null;
		Object hashedURL = m_hashUrl.get(a_locale.getLanguage());
		if (hashedURL != null && hashedURL instanceof URL)
		{
			url = (URL) hashedURL;
		}
		else
		{
			hashedURL = m_hashUrl.get("en");
			if (hashedURL != null && hashedURL instanceof URL)
			{
				url = (URL)hashedURL;
			}
		}
		if (url == null)
		{
			LogHolder.log(LogLevel.WARNING, LogType.MISC, "Could not get URL for message: " + getText(a_locale));
		}

		return url;
	}

	public String getText(Locale a_locale)
	{
		return getText(a_locale, m_hashText);
	}

	public String getPopupText(Locale a_locale)
	{
		return getText(a_locale, m_hashPopupText);
	}

	public int getExternalIdentifier()
	{
		return m_externalIdentifier;
	}

	public void setExternalIdentifier(int a_identifier)
	{
		m_externalIdentifier = a_identifier;
	}

	public boolean isPopupShown()
	{
		return m_bShowPopup;
	}

	public boolean isForFreeCascadesOnly()
	{
		return m_bFree;
	}

	public boolean isDummy()
	{
		return m_bIsDummy;
	}

	public long getVersionNumber()
	{
		return m_serial;
	}

	public String getId()
	{
		return m_id;
	}

	public String getPostFile()
	{
		return POST_FILE;
	}

	public long getLastUpdate()
	{
		return m_lastUpdate;
	}

	public Element getXmlStructure()
	{
		return m_xmlDescription;
	}

	private String getText(Locale a_locale, Hashtable a_textHash)
	{
		if (a_locale == null)
		{
			return null;
		}
		String text = (String)a_textHash.get(a_locale.getLanguage());
		if (text == null)
		{
			text = (String)a_textHash.get("en");
		}
		return text;
	}

	private boolean parseTextNodes(NodeList a_textNodes, Hashtable a_nodeTable)
	{
		String content, lang, encoding;
		boolean bFoundText;

		for (int i = 0; i < a_textNodes.getLength(); i++)
		{
			content = XMLUtil.parseValue(a_textNodes.item(i), null);
			lang = XMLUtil.parseAttribute(a_textNodes.item(i), XML_ATTR_LANG, "en");
			encoding = XMLUtil.parseAttribute(
				a_textNodes.item(i), XML_ATTR_ENCODING, ENCODING_BASE64);
			if (content != null)
			{
				if (encoding.equals(ENCODING_URL))
				{
					content = URLDecoder.decode(content);
				}
				else if (encoding.equals(ENCODING_BASE64))
				{
					content = Base64.decodeToString(content);
				}
				else
				{
					// encoding not supported
					content = null;
				}
				if (content != null)
				{
					a_nodeTable.put(lang, content);
				}
			}
		}

		if (a_nodeTable.size() == 0 || a_nodeTable.get("en") == null)
		{
			// if there is not text (or no english text), this in interpreted as dummy message
			bFoundText = true;
		}
		else
		{
			bFoundText = false;
		}

		return bFoundText;
	}
}
