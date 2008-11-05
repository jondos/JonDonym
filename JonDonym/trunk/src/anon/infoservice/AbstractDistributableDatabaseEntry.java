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

import java.util.Enumeration;
import java.util.Hashtable;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import anon.crypto.IVerifyable;
import anon.util.IXMLEncodable;
import anon.util.XMLParseException;
import anon.util.XMLUtil;
import anon.util.Util;

/**
 * This class implements common methods that may be used by distributabe database entries.
 *
 * @author Rolf Wendolsky
 */
public abstract class AbstractDistributableDatabaseEntry extends AbstractDatabaseEntry
	implements IDistributable, IXMLEncodable
{
	public static final String XML_ATTR_SERIAL = "serial";
	public static final String XML_ATTR_VERIFIED = "verified";
	public static final String XML_ATTR_VALID = "valid";
	public static final String XML_ATTR_LAST_UPDATE = "lastUpdate";

	public AbstractDistributableDatabaseEntry(long a_expireTime)
	{
		super(a_expireTime);
	}

	public static String getHttpRequestString(Class a_xmlEncodableClass)
	{
		return Util.getStaticFieldValue(a_xmlEncodableClass, IDistributable.FIELD_HTTP_REQUEST_STRING);
	}

	public static String getHttpSerialsRequestString(Class a_xmlEncodableClass)
	{
		return Util.getStaticFieldValue(a_xmlEncodableClass, IDistributable.FIELD_HTTP_SERIALS_REQUEST_STRING);
	}

	public static class Serials implements IXMLEncodable
	{
		private static final String XML_ELEMENT_NAME = "Serials";

		private Class m_thisDBEntryClass;

		public Serials(Class a_thisDBEntryClass) throws IllegalArgumentException
		{
			if (a_thisDBEntryClass == null ||
				!AbstractDistributableDatabaseEntry.class.isAssignableFrom(a_thisDBEntryClass))
			{
				throw new IllegalArgumentException("Illegal class argument!");
			}
			m_thisDBEntryClass = a_thisDBEntryClass;
		}

		public Hashtable parse(Element a_elemSerials) throws XMLParseException
		{
			if (a_elemSerials == null || a_elemSerials.getNodeName() == null ||
				!a_elemSerials.getNodeName().equals(XML_ELEMENT_NAME))
			{
				throw new XMLParseException(XMLParseException.NODE_NULL_TAG);
			}

			String id;
			long serial, lastUpdate;
			boolean bVerified, bValid;
			String context = null;
			
			Hashtable hashSerials;
			NodeList serialNodes =
				a_elemSerials.getElementsByTagName(XMLUtil.getXmlElementName(m_thisDBEntryClass));

			if (serialNodes.getLength() > 0)
			{
				hashSerials = new Hashtable(serialNodes.getLength());
			}
			else
			{
				hashSerials = new Hashtable();
			}

			for (int i = 0; i < serialNodes.getLength(); i++)
			{
				id = XMLUtil.parseAttribute(serialNodes.item(i), XML_ATTR_ID, null);
				if (id != null)
				{
					serial = XMLUtil.parseAttribute(serialNodes.item(i), XML_ATTR_SERIAL, 0L);
					lastUpdate = XMLUtil.parseAttribute(serialNodes.item(i), XML_ATTR_LAST_UPDATE, 0L);
					bVerified = XMLUtil.parseAttribute(serialNodes.item(i), XML_ATTR_VERIFIED, false);
					bValid = XMLUtil.parseAttribute(serialNodes.item(i), XML_ATTR_VALID, false);
					context = XMLUtil.parseAttribute(serialNodes.item(i), 
												IServiceContextContainer.XML_ATTR_CONTEXT, 
												IServiceContextContainer.CONTEXT_JONDONYM);
				}
				else
				{
					continue;
				}

				hashSerials.put(id, new SerialDBEntry(id, serial, lastUpdate, bVerified, bValid, context));
			}

			return hashSerials;
		}


		public Element toXmlElement(Document a_doc)
		{
			if (a_doc == null)
			{
				return null;
			}

			Element nodeSerials = a_doc.createElement(XML_ELEMENT_NAME);
			Element nodeASerial;
			AbstractDistributableDatabaseEntry currentEntry;
			Enumeration knownEntries = Database.getInstance(m_thisDBEntryClass).
				getEntrySnapshotAsEnumeration();

			while (knownEntries.hasMoreElements())
			{
				currentEntry = (AbstractDistributableDatabaseEntry) knownEntries.nextElement();

				if (currentEntry.getVersionNumber() <= 0)
				{/*
					LogHolder.log(LogLevel.ERR, LogType.MISC, "Invalid version number: " +
								  currentEntry.getVersionNumber() + "\n" +
								  XMLUtil.toString(currentEntry.getXmlStructure()));*/
					//continue;
				}
				nodeASerial = a_doc.createElement(XMLUtil.getXmlElementName(m_thisDBEntryClass));
				nodeSerials.appendChild(nodeASerial);
				XMLUtil.setAttribute(nodeASerial, XML_ATTR_ID, currentEntry.getId());
				XMLUtil.setAttribute(nodeASerial, XML_ATTR_LAST_UPDATE, currentEntry.getLastUpdate());
				XMLUtil.setAttribute(nodeASerial, XML_ATTR_SERIAL, currentEntry.getVersionNumber());
				if (currentEntry instanceof IVerifyable)
				{
					XMLUtil.setAttribute(nodeASerial, XML_ATTR_VALID,((IVerifyable)currentEntry).isValid());
					XMLUtil.setAttribute(nodeASerial, XML_ATTR_VERIFIED,
										 ((IVerifyable)currentEntry).isVerified() &&
						((IVerifyable)currentEntry).getCertPath().verify());
				}
				if (currentEntry instanceof IServiceContextContainer)
				{
					String context = ((IServiceContextContainer)currentEntry).getContext();
					if(context != null)
					{
						XMLUtil.setAttribute(nodeASerial, IServiceContextContainer.XML_ATTR_CONTEXT, context);
					}
				}
			}
			return nodeSerials;
		}
	}

	public static class SerialDBEntry extends AbstractDatabaseEntry 
		implements IServiceContextContainer
	{
		private String m_id;
		private long m_version;
		private long m_lastUpdate;
		private boolean m_bVerified;
		private boolean m_bValid;
		private String m_context;

		public SerialDBEntry(String a_id, long a_version, long a_lastUpdate,
							 boolean a_bVerified, boolean a_bValid, String context)
		{
			super(0);

			m_id = a_id;
			m_version = a_version;
			m_lastUpdate = a_lastUpdate;
			m_bVerified = a_bVerified;
			m_bValid = a_bValid;
			m_context = context;
		}

		public boolean isVerified()
		{
			return m_bVerified;
		}

		public boolean isValid()
		{
			return m_bValid;
		}

		public long getLastUpdate()
		{
			return m_lastUpdate;
		}

		public String getId()
		{
			return m_id;
		}

		public long getVersionNumber()
		{
			return m_version;
		}

		public String getContext() 
		{
			return m_context;
		}

		public void setContext(String context) {
			this.m_context = context;
		}
	}


	/**
	 * Returns the XML structure for this db entry.
	 *
	 * @return The XML node of this db entry
	 */
	public abstract Element getXmlStructure();

	/**
	 * This returns the data, which are posted to other InfoServices. It's the whole XML structure
	 * of this DBEntry by default but may be overwritten
	 *
	 * @return The data, which are posted to other InfoServices when this entry is forwarded.
	 */
	public byte[] getPostData()
	{
		return (XMLUtil.toString(getXmlStructure()).getBytes());
	}

	/**
	 * Returns the content encoding of the post data.
	 * @return the content encoding of the post data
	 */
	public int getPostEncoding()
	{
		return HTTPConnectionFactory.HTTP_ENCODING_PLAIN;
	}

	/**
	 * Creates an XML node for this db entry.
	 * @param a_doc The XML document, which is the environment for the created XML node.
	 * @return The db entry XML node or null if an error occured
	 */
	public final Element toXmlElement(Document a_doc)
	{
		Element returnXmlStructure = null;
		try
		{
			returnXmlStructure = (Element) (XMLUtil.importNode(a_doc, getXmlStructure(), true));
		}
		catch (Exception e)
		{
		}
		return returnXmlStructure;
	}
}
