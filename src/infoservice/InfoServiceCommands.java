/*
 Copyright (c) 2000 - 2005, The JAP-Team
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
package infoservice;

import jap.JAPUtil;

import java.net.InetAddress;
import java.text.NumberFormat;
import java.io.File;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import anon.crypto.SignatureCreator;
import anon.crypto.SignatureVerifier;
import anon.crypto.XMLSignature;
import anon.infoservice.AbstractDatabaseEntry;
import anon.infoservice.AbstractDistributableDatabaseEntry;
import anon.infoservice.Constants;
import anon.infoservice.Database;
import anon.infoservice.InfoServiceDBEntry;
import anon.infoservice.InfoServiceIDEntry;
import anon.infoservice.JAPMinVersion;
import anon.infoservice.JAPVersionInfo;
import anon.infoservice.JavaVersionDBEntry;
import anon.infoservice.MessageDBEntry;
import anon.infoservice.MixCascade;
import anon.infoservice.MixInfo;
import anon.infoservice.StatusInfo;
import anon.infoservice.PerformanceEntry;
import anon.pay.PaymentInstanceDBEntry;
import anon.util.IXMLEncodable;
import anon.util.XMLParseException;
import anon.util.XMLUtil;
import anon.util.ZLibTools;
import infoservice.agreement.IInfoServiceAgreementAdapter;
import infoservice.dynamic.DynamicCommandsExtension;
import infoservice.dynamic.DynamicConfiguration;
import infoservice.japforwarding.JapForwardingTools;
import infoservice.tor.MixminionDirectoryAgent;
import infoservice.tor.TorDirectoryAgent;
import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;
import java.util.Random;

/**
 * This is the implementation of all commands the InfoService supports.
 */
final public class InfoServiceCommands implements JWSInternalCommands
{
	private final HTTPResponseGetter m_isResponseGetter = new HTTPResponseGetter()
	{
		public Class getDatabaseClass()
		{
			return InfoServiceDBEntry.class;
		}
	};
	private final HTTPResponseGetter m_cascadeResponseGetter = new HTTPResponseGetter()
	{
		public Class getDatabaseClass()
		{
			return MixCascade.class;
		}
	};
	private final HTTPResponseGetter m_messageResponseGetter = new HTTPResponseGetter()
	{
		public Class getDatabaseClass()
		{
			return MessageDBEntry.class;
		}
	};
	private final HTTPResponseGetter m_javaVersionResponseGetter = new HTTPResponseGetter()
	{
		public Class getDatabaseClass()
		{
			return JavaVersionDBEntry.class;
		}
	};
	private final HTTPResponseGetter m_performanceResponseGetter = new HTTPResponseGetter()
	{
		public Class getDatabaseClass()
		{
			return PerformanceEntry.class;
		}
	};

	private IInfoServiceAgreementAdapter m_agreementAdapter = DynamicConfiguration.getInstance().
		getAgreementHandler();

	private DynamicCommandsExtension m_dynamicExtension = new DynamicCommandsExtension();

	// Ok the cache the StatusInfo DB here for performance reasonses (the
	// database objete itself will always remain the same during the lifetime of
	// the Is -so
	//no problem so far
	private Database m_statusinfoDB;

	/** Stores an object which can generated some randomness...*/
	private Random m_Random;
	public InfoServiceCommands()
	{
		m_statusinfoDB = Database.getInstance(StatusInfo.class);
		m_Random = new Random();
	}

	/**
	 * This method is called, when we receive data directly from a infoservice
	 * or when we receive data from a remote infoservice, which posts data about
	 * another infoservice.
	 *
	 * @param a_postData The data we have received.
	 *
	 * @return The HTTP response for the client.
	 */
	private HttpResponseStructure infoServerPostHelo(byte[] a_postData)
	{
		HttpResponseStructure httpResponse = new HttpResponseStructure(HttpResponseStructure.HTTP_RETURN_OK);
		try
		{
			LogHolder.log(LogLevel.DEBUG, LogType.NET, "Infoserver received: XML: " + (new String(a_postData)));
			Element infoServiceNode = (Element) (XMLUtil.getFirstChildByName(XMLUtil.toXMLDocument(a_postData),
				InfoServiceDBEntry.XML_ELEMENT_NAME));

			InfoServiceDBEntry newEntry = new InfoServiceDBEntry(infoServiceNode, false);
			/* verify the signature --> if requested */

			AbstractDatabaseEntry idEntry = Database.getInstance(InfoServiceIDEntry.class).getEntryById(
				newEntry.getId());

			if (newEntry.isVerified() && !newEntry.getId().equals(Configuration.getInstance().getID()) &&
				newEntry.isNewerThan(idEntry))
			{
				Database.getInstance(InfoServiceIDEntry.class).update(new InfoServiceIDEntry(newEntry));
				Database.getInstance(InfoServiceDBEntry.class).update(newEntry);
			}
			else
			{
				LogHolder.log(LogLevel.WARNING, LogType.NET,
							  "Signature check failed for infoservice entry! XML: " + (new String(a_postData)));
				httpResponse = new HttpResponseStructure(HttpResponseStructure.
					HTTP_RETURN_INTERNAL_SERVER_ERROR);
			}
		}
		catch (Exception e)
		{
			LogHolder.log(LogLevel.ERR, LogType.NET, e);
			httpResponse = new HttpResponseStructure(HttpResponseStructure.HTTP_RETURN_BAD_REQUEST);
		}
		return httpResponse;
	}

	/**
	 * This method is called, when we receive data from a payment instance or when we receive
	 * data from a remote infoservice, which posts data about another payment instance.
	 *
	 * @param a_postData The data we have received.
	 *
	 * @return The HTTP response for the client.
	 */
	private HttpResponseStructure paymentInstancePostHelo(byte[] a_postData)
	{
		HttpResponseStructure httpResponse = new HttpResponseStructure(HttpResponseStructure.HTTP_RETURN_OK);
		try
		{
			LogHolder.log(LogLevel.DEBUG, LogType.NET, "Infoserver received: XML: " + (new String(a_postData)));
			Element paymentInstanceNode = (Element) (XMLUtil.getFirstChildByName(XMLUtil.toXMLDocument(
				a_postData),
				PaymentInstanceDBEntry.XML_ELEMENT_NAME));
			/* verify the signature --> if requested */
			// no signature check at the monent...
			//	if (!Configuration.getInstance().isInfoServiceMessageSignatureCheckEnabled() ||
			//	SignatureVerifier.getInstance().verifyXml(infoServiceNode,
			//SignatureVerifier.DOCUMENT_CLASS_INFOSERVICE) == true)
			{
				PaymentInstanceDBEntry newEntry = new PaymentInstanceDBEntry(paymentInstanceNode);
				Database.getInstance(PaymentInstanceDBEntry.class).update(newEntry);
			}
			/*			else
			   {
			 LogHolder.log(LogLevel.WARNING, LogType.NET,
			   "Signature check failed for infoservice entry! XML: " + (new String(a_postData)));
			 httpResponse = new HttpResponseStructure(HttpResponseStructure.
			  HTTP_RETURN_INTERNAL_SERVER_ERROR);
			   }*/
		}
		catch (Exception e)
		{
			LogHolder.log(LogLevel.ERR, LogType.NET, e);
			httpResponse = new HttpResponseStructure(HttpResponseStructure.HTTP_RETURN_BAD_REQUEST);
		}
		return httpResponse;
	}

	/**
	 * Sends info about a special payment instance to the client.
	 *
	 * @return The HTTP response for the client.
	 */
	private HttpResponseStructure japFetchPaymentInstanceInfo(String a_piID)
	{
		HttpResponseStructure httpResponse =
			new HttpResponseStructure(HttpResponseStructure.HTTP_RETURN_INTERNAL_SERVER_ERROR);
		try
		{
			Enumeration en = Database.getInstance(PaymentInstanceDBEntry.class).getEntrySnapshotAsEnumeration();
			while (en.hasMoreElements())
			{
				PaymentInstanceDBEntry entry = (PaymentInstanceDBEntry) en.nextElement();
				if (entry.getId().equals(a_piID))
				{
					Document doc = XMLUtil.createDocument();
					doc.appendChild(entry.toXmlElement(doc));
					httpResponse = new HttpResponseStructure(doc);
				}
			}
		}
		catch (Exception e)
		{

			LogHolder.log(LogLevel.ERR, LogType.NET, e);
		}

		return httpResponse;
	}

	/**
	 * Sends the complete list of all known payment instances to the client.
	 *
	 * @return The HTTP response for the client.
	 */
	private HttpResponseStructure japFetchPaymentInstances()
	{
		/* this is only the default, if something is going wrong */
		HttpResponseStructure httpResponse;
		try
		{
			Document doc = XMLUtil.createDocument();
			/* create the InfoServices element */
			Element paymentInstances = doc.createElement("PaymentInstances");
			/* append the nodes of all infoservices we know */
			Enumeration allPaymentInstances = Database.getInstance(PaymentInstanceDBEntry.class).
				getEntrySnapshotAsEnumeration();
			while (allPaymentInstances.hasMoreElements())
			{
				/* import the infoservice node in this document */
				Node paymentInstanceNode = ( (PaymentInstanceDBEntry) (allPaymentInstances.nextElement())).
					toXmlElement(
						doc);
				paymentInstances.appendChild(paymentInstanceNode);
			}
			SignatureCreator.getInstance().signXml(SignatureVerifier.DOCUMENT_CLASS_INFOSERVICE,
				paymentInstances);
			doc.appendChild(paymentInstances);
			/* send the XML document to the client */
			httpResponse = new HttpResponseStructure(doc);
		}
		catch (Exception e)
		{
			httpResponse = new HttpResponseStructure(HttpResponseStructure.HTTP_RETURN_INTERNAL_SERVER_ERROR);
			LogHolder.log(LogLevel.ERR, LogType.NET, e);
		}
		return httpResponse;
	}

	/**
	 * This method is called, when we receive data from a mixcascade (first mix) or when we
	 * receive data from a remote infoservice, which posts data about a mixcascade.
	 *
	 * @param a_postData The data we have received.
	 * @param a_encoding the encoding chosen by the client
	 *
	 * @return The HTTP response for the client.
	 */
	/* Now in DnyamicExtensions ....
	private HttpResponseStructure cascadePostHelo(byte[] a_postData, int a_encoding)
	{
		HttpResponseStructure httpResponse = new HttpResponseStructure(HttpResponseStructure.HTTP_RETURN_OK);
		try
		{
			LogHolder.log(LogLevel.DEBUG, LogType.NET,
						  "MixCascade HELO received: XML: " + (new String(a_postData)));

			// verify the signature 
			MixCascade mixCascadeEntry;
			if (a_encoding == HttpResponseStructure.HTTP_ENCODING_ZLIB)
			{
				mixCascadeEntry = new MixCascade(a_postData);
			}
			else if (a_encoding == HttpResponseStructure.HTTP_ENCODING_PLAIN)
			{
				Element mixCascadeNode =
					(Element) (XMLUtil.getFirstChildByName(XMLUtil.toXMLDocument(a_postData),
					MixCascade.XML_ELEMENT_NAME));
				mixCascadeEntry = new MixCascade(mixCascadeNode);
			}
			else
			{
				throw new Exception("Unsupported post encoding:" + a_encoding);
			}

			Database.getInstance(MixCascade.class).update(mixCascadeEntry);
		}
		catch (Exception e)
		{
			LogHolder.log(LogLevel.ERR, LogType.NET, e);
			httpResponse = new HttpResponseStructure(HttpResponseStructure.HTTP_RETURN_BAD_REQUEST);
		}
		return httpResponse;
	}
*/
	private HttpResponseStructure messagePost(byte[] a_postData, int a_encoding)
	{
		HttpResponseStructure httpResponse = new HttpResponseStructure(HttpResponseStructure.HTTP_RETURN_OK);
		try
		{
			LogHolder.log(LogLevel.DEBUG, LogType.NET,
						  "Message received: XML: " + (new String(a_postData)));

			MessageDBEntry entry;
			if (a_encoding == HttpResponseStructure.HTTP_ENCODING_PLAIN)
			{
				Element mixCascadeNode =
					(Element) (XMLUtil.getFirstChildByName(XMLUtil.toXMLDocument(a_postData),
					MessageDBEntry.XML_ELEMENT_NAME));
				entry = new MessageDBEntry(mixCascadeNode);
			}
			else
			{
				throw new Exception("Unsupported post encoding:" + a_encoding);
			}

			Database.getInstance(MessageDBEntry.class).update(entry);
		}
		catch (Exception e)
		{
			LogHolder.log(LogLevel.ERR, LogType.NET, e);
			httpResponse = new HttpResponseStructure(HttpResponseStructure.HTTP_RETURN_BAD_REQUEST);
		}
		return httpResponse;
	}

	private abstract class HTTPResponseGetter
	{
		private static final long CACHE_SERIALS_MS = 10000;
		private static final long CACHE_MS = 10000;

		private HttpResponseStructure m_cachedSerialsResponse;
		private HttpResponseStructure m_cachedSerialsCompressedResponse;
		private final Object SYNC_CACHE_SERIALS = new Object();
		private long m_lastSerialsUpdate = 0;

		private HttpResponseStructure m_cachedResponse;
		private HttpResponseStructure m_cachedCompressedResponse;
		private final Object SYNC_CACHE = new Object();
		private long m_lastUpdate = 0;

		public HTTPResponseGetter()
		{
		}

		public abstract Class getDatabaseClass();

		protected HttpResponseStructure fetchResponse(int a_supportedEncodings)
		{
			HttpResponseStructure httpResponse;
			Document doc;
			Element containerNode;

			synchronized (SYNC_CACHE)
			{
				if (m_lastUpdate < (System.currentTimeMillis() - CACHE_MS))
				{
					doc = XMLUtil.createDocument();
					containerNode = doc.createElement(XMLUtil.getXmlElementContainerName(getDatabaseClass()));
					
					/* @todo cbanse: i'm not really happy with this.... */
					XMLUtil.setAttribute(containerNode, "id", Configuration.getInstance().getID());
					
					/* append the nodes of all mixcascades we know */
					Enumeration knownMixCascades = Database.getInstance(getDatabaseClass()).
						getEntrySnapshotAsEnumeration();
					IXMLEncodable currentCascade;
					Element node;
					while (knownMixCascades.hasMoreElements())
					{
						/* import the MixCascade XML structure in this document */
						currentCascade = (IXMLEncodable) (knownMixCascades.nextElement());
						node = currentCascade.toXmlElement(doc);
						containerNode.appendChild(node);
					}
					SignatureCreator.getInstance().signXml(SignatureVerifier.DOCUMENT_CLASS_INFOSERVICE,
						containerNode);
					doc.appendChild(containerNode);
					/* send the XML document to the client */
					//if ( (a_supportedEncodings & HttpResponseStructure.HTTP_ENCODING_ZLIB) > 0)
					{
						try
						{
							m_cachedCompressedResponse = new HttpResponseStructure(
								HttpResponseStructure.HTTP_TYPE_TEXT_XML,
								HttpResponseStructure.HTTP_ENCODING_ZLIB,
								//ZLibTools.compress(XMLUtil.toByteArray(doc)));
								ZLibTools.compress(XMLSignature.toCanonical(doc)));
						}
						catch (XMLParseException ex)
						{
							m_cachedCompressedResponse = new HttpResponseStructure(HttpResponseStructure.
								HTTP_RETURN_INTERNAL_SERVER_ERROR);
						}
					}
					//else
					{
						m_cachedResponse = new HttpResponseStructure(doc);
					}
					m_lastUpdate = System.currentTimeMillis();
				}
			}

			if ( (a_supportedEncodings & HttpResponseStructure.HTTP_ENCODING_ZLIB) > 0)
			{
				httpResponse = m_cachedCompressedResponse;
			}
			else
			{
				httpResponse = m_cachedResponse;
			}
			return httpResponse;
		}

		protected HttpResponseStructure fetchSerialsResponse(int a_supportedEncodings)
		{
			HttpResponseStructure httpResponse;
			Document doc;
			Element node;

			synchronized (SYNC_CACHE_SERIALS)
			{
				if (m_lastSerialsUpdate < (System.currentTimeMillis() - CACHE_SERIALS_MS))
				{
					doc = XMLUtil.createDocument();
					node =
						new AbstractDistributableDatabaseEntry.Serials(getDatabaseClass()).toXmlElement(doc);
					SignatureCreator.getInstance().signXml(SignatureVerifier.
						DOCUMENT_CLASS_INFOSERVICE,
						node);
					doc.appendChild(node);

					//if ( (a_supportedEncodings & HttpResponseStructure.HTTP_ENCODING_ZLIB) > 0)
					{
						try
						{
							m_cachedSerialsCompressedResponse = new HttpResponseStructure(
								HttpResponseStructure.HTTP_TYPE_TEXT_XML,
								HttpResponseStructure.HTTP_ENCODING_ZLIB,
								//ZLibTools.compress(XMLUtil.toByteArray(doc)));
								ZLibTools.compress(XMLSignature.toCanonical(doc)));
						}
						catch (XMLParseException ex)
						{
							m_cachedCompressedResponse = new HttpResponseStructure(HttpResponseStructure.
								HTTP_RETURN_INTERNAL_SERVER_ERROR);
						}
					}
					//else
					{
						m_cachedSerialsResponse = new HttpResponseStructure(doc);
					}
					m_lastSerialsUpdate = System.currentTimeMillis();
				}
			}
			/* send the XML document to the client */
			if ( (a_supportedEncodings & HttpResponseStructure.HTTP_ENCODING_ZLIB) > 0)
			{
				httpResponse = m_cachedSerialsCompressedResponse;
			}
			else
			{
				httpResponse = m_cachedSerialsResponse;
			}
			return httpResponse;
		}

		/**
		 * Sends the complete list of all known db entries to the client.
		 *
		 * @param a_supportedEncodings defines the encoding supported by the client (deflate, gzip,...)
		 * @param a_bSerialsOnly only return a list with db entry serial numbers so that the caller may decide
		 * which db entries have changed since the last request
		 * @return The HTTP response for the client.
		 */
		public HttpResponseStructure fetchResponse(int a_supportedEncodings, boolean a_bSerialsOnly)
		{
			HttpResponseStructure httpResponse;
			try
			{
				if (a_bSerialsOnly)
				{
					httpResponse = fetchSerialsResponse(a_supportedEncodings);
				}
				else
				{
					httpResponse = fetchResponse(a_supportedEncodings);
				}
			}
			catch (Exception e)
			{
				/* should never occur */
				httpResponse = new HttpResponseStructure(HttpResponseStructure.
					HTTP_RETURN_INTERNAL_SERVER_ERROR);
				LogHolder.log(LogLevel.ERR, LogType.NET, e);
			}
			return httpResponse;
		}
	}

	/**
	 * This method is called, when we receive data from a non-free mix or when we receive data
	 * from a remote infoservice, which posts data about a non-free mix.
	 *
	 * @param a_postData The data we have received.
	 *
	 * @return The HTTP response for the client.
	 */
	private HttpResponseStructure mixPostHelo(byte[] a_postData)
	{
		HttpResponseStructure httpResponse = new HttpResponseStructure(HttpResponseStructure.HTTP_RETURN_OK);
		try
		{
			LogHolder.log(LogLevel.DEBUG, LogType.NET, "Mix HELO received: XML: " + (new String(a_postData)));
			Element mixNode = (Element) (XMLUtil.getFirstChildByName(XMLUtil.toXMLDocument(a_postData),
				MixInfo.XML_ELEMENT_NAME));
			MixInfo mixEntry = new MixInfo(mixNode);
			/* verify the signature */
			if (mixEntry.isVerified())
			{
				Database.getInstance(MixInfo.class).update(mixEntry);
				//extract possible last proxy addresses
				VisibleProxyAddresses.addAddresses(mixNode);
			}
			else
			{
				LogHolder.log(LogLevel.WARNING, LogType.NET,
							  "Signature check failed for Mix entry! XML: " + (new String(a_postData)));
				httpResponse = new HttpResponseStructure(HttpResponseStructure.
					HTTP_RETURN_INTERNAL_SERVER_ERROR);
			}
		}
		catch (Exception e)
		{
			LogHolder.log(LogLevel.ERR, LogType.NET, e);
			httpResponse = new HttpResponseStructure(HttpResponseStructure.HTTP_RETURN_BAD_REQUEST);
		}
		return httpResponse;
	}

	/**
	 * This method is called, when we receive data from a mix which wants to get configured
	 * automatically (inserted in a auto-configure-cascade). If the mix is already assigned to a
	 * mixcascade, we will send back the XML structure of the cascade the mix now belongs to. This
	 * makes it possible for the mix to connect his neighbours.
	 *
	 * @param a_postData The data we have received.
	 *
	 * @return The HTTP response for the client.
	 */
	private HttpResponseStructure mixPostConfigure(byte[] a_postData)
	{
		HttpResponseStructure httpResponse = new HttpResponseStructure(HttpResponseStructure.HTTP_RETURN_OK);
		try
		{
			LogHolder.log(LogLevel.DEBUG, LogType.NET,
						  "Mix Configure received: XML: " + (new String(a_postData)));
			Element mixNode = (Element) (XMLUtil.getFirstChildByName(XMLUtil.toXMLDocument(a_postData),
				MixInfo.XML_ELEMENT_NAME));
			MixInfo mixEntry = new MixInfo(mixNode);
			/* verify the signature */
			if (mixEntry.isVerified())
			{
				/* check whether the mix is already assigned to a mixcascade */
				Enumeration knownMixCascades = Database.getInstance(MixCascade.class).getEntryList().elements();
				MixCascade assignedCascade = null;
				while (knownMixCascades.hasMoreElements() && (assignedCascade == null))
				{
					MixCascade currentCascade = (MixCascade) (knownMixCascades.nextElement());
					if (currentCascade.getMixIds().contains(mixEntry.getId()))
					{
						/* the mix is assigned to that cascade */
						assignedCascade = currentCascade;
					}
				}
				if (assignedCascade == null)
				{
					/* the mix is not assigned to any cascade and wants to get configured -> it's a free mix
					 */
					mixEntry.setFreeMix(true);
				}
				else
				{
					/* send back the XML structure of the cascade the mix belongs to */
					httpResponse = new HttpResponseStructure(HttpResponseStructure.HTTP_TYPE_TEXT_XML,
						HttpResponseStructure.HTTP_ENCODING_PLAIN,
						XMLUtil.toString(assignedCascade.getXmlStructure()));
				}
				/* update the database in every case */
				Database.getInstance(MixInfo.class).update(mixEntry);
			}
			else
			{
				LogHolder.log(LogLevel.WARNING, LogType.NET,
							  "Signature check failed for Mix entry! XML: " + (new String(a_postData)));
				httpResponse = new HttpResponseStructure(HttpResponseStructure.
					HTTP_RETURN_INTERNAL_SERVER_ERROR);
			}
		}
		catch (Exception e)
		{
			LogHolder.log(LogLevel.ERR, LogType.NET, e);
			httpResponse = new HttpResponseStructure(HttpResponseStructure.HTTP_RETURN_BAD_REQUEST);
		}
		return httpResponse;
	}

	/**
	 * Sends the XML encoded mix entry for the specified ID to the client.
	 *
	 * @param a_mixId The ID of the requested mix.
	 *
	 * @return The HTTP response for the client.
	 */
	private HttpResponseStructure japGetMix(String a_mixId)
	{
		/* this is only the default, if something is going wrong */
		HttpResponseStructure httpResponse;
		try
		{
			MixInfo mixEntry = (MixInfo) (Database.getInstance(MixInfo.class).getEntryById(a_mixId));
			if (mixEntry == null)
			{
				httpResponse = new HttpResponseStructure(HttpResponseStructure.HTTP_RETURN_NOT_FOUND);
			}
			else
			{
				httpResponse = new HttpResponseStructure(HttpResponseStructure.HTTP_TYPE_TEXT_XML,
					HttpResponseStructure.HTTP_ENCODING_PLAIN,
					XMLUtil.toString(mixEntry.getXmlStructure()));
			}
		}
		catch (Exception e)
		{
			/* should never occur */
			httpResponse = new HttpResponseStructure(HttpResponseStructure.HTTP_RETURN_INTERNAL_SERVER_ERROR);
			LogHolder.log(LogLevel.ERR, LogType.NET, e);
		}
		return httpResponse;
	}

	/**
	 * This method is called, when we receive data from a mixcascade about the status or when we
	 * receive data from a remote infoservice, which posts data about mixcascade status.
	 *
	 * @param a_postData The data we have received.
	 *
	 * @return The HTTP response for the client.
	 */
	private HttpResponseStructure cascadePostStatus(byte[] a_postData)
	{
		HttpResponseStructure httpResponse = new HttpResponseStructure(HttpResponseStructure.HTTP_RETURN_OK);
		try
		{
			LogHolder.log(LogLevel.DEBUG, LogType.NET, "Status received: XML: " + (new String(a_postData)));
			Element mixCascadeStatusNode = (Element) (XMLUtil.getFirstChildByName(XMLUtil.toXMLDocument(
				a_postData), StatusInfo.XML_ELEMENT_NAME));
			StatusInfo statusEntry = new StatusInfo(mixCascadeStatusNode);
			/* verify the signature */
			if (statusEntry.isVerified())
			{
				Database.getInstance(StatusInfo.class).update(statusEntry);
				/* update the statistics, if they are not enabled or we know the received status message
				 * already, nothing is done by this call
				 */
				StatusStatistics.getInstance().update(statusEntry);
			}
			else
			{
				LogHolder.log(LogLevel.WARNING, LogType.NET,
							  "Signature check failed for mixcascade status entry! XML: " +
							  (new String(a_postData)));
				httpResponse = new HttpResponseStructure(HttpResponseStructure.
					HTTP_RETURN_INTERNAL_SERVER_ERROR);
			}
		}
		catch (Exception e)
		{
			LogHolder.log(LogLevel.ERR, LogType.NET, e);
			httpResponse = new HttpResponseStructure(HttpResponseStructure.HTTP_RETURN_BAD_REQUEST);
		}
		return httpResponse;
	}

	/**
	 * Sends the XML encoded status of the mixcascade with the ID given by cascadeId. It uses
	 * the original version (as the infoservice has received it) of the XML structure.
	 *
	 * @param a_cascadeId The ID of the mixcascade.
	 *
	 * @return The HTTP response for the client.
	 */
	private HttpResponseStructure japGetCascadeStatus(String a_cascadeId)
	{
		ISRuntimeStatistics.ms_lNrOfGetMixCascadeStatusRequests++;
		/* this is only the default, if something is going wrong */
		HttpResponseStructure httpResponse = null;
		try
		{
			StatusInfo statusEntry = (StatusInfo) m_statusinfoDB.getEntryById(a_cascadeId);
			if (statusEntry == null)
			{
				/* we don't have a status for the given id */
				httpResponse = new HttpResponseStructure(HttpResponseStructure.HTTP_RETURN_NOT_FOUND);
			}
			else
			{
				httpResponse = new HttpResponseStructure(HttpResponseStructure.HTTP_TYPE_TEXT_XML,
					HttpResponseStructure.HTTP_ENCODING_PLAIN, statusEntry.getPostData());
			}
		}
		catch (Exception e)
		{
			httpResponse = new HttpResponseStructure(HttpResponseStructure.HTTP_RETURN_INTERNAL_SERVER_ERROR);
			LogHolder.log(LogLevel.ERR, LogType.NET, e);
		}
		return httpResponse;
	}

	/*
	  private HttpResponseStructure getLatestJavaVersions()
	  {

	   HttpResponseStructure httpResponse;
	   Document doc = XMLUtil.createDocument();
	   try
	   {
		Element entries = Database.getInstance(JavaVersionDBEntry.class).toXmlElement(doc);
		if (entries != null)
		{
		 doc.appendChild(entries);
		}
		httpResponse = new HttpResponseStructure(doc);
	   }
	   catch (Exception e)
	   {
		httpResponse = new HttpResponseStructure(HttpResponseStructure.HTTP_RETURN_INTERNAL_SERVER_ERROR);
		LogHolder.log(LogLevel.ERR, LogType.NET, e);
	   }
	   return httpResponse;
	  }*/

	/**
	 * This method is called, when we receive data from another infoservice with the lastest java version
	 * for a specific vendor.
	 *
	 * @param a_postData The data we have received.
	 *
	 * @return The HTTP response for the client.
	 */
	private HttpResponseStructure postLatestJavaVersions(byte[] a_postData)
	{
		HttpResponseStructure httpResponse = new HttpResponseStructure(HttpResponseStructure.HTTP_RETURN_OK);
		try
		{
			LogHolder.log(LogLevel.DEBUG, LogType.NET,
						  "Latest Java versions received. XML: " + (new String(a_postData)));

			Element node = (Element) (XMLUtil.getFirstChildByName(XMLUtil.toXMLDocument(
				a_postData), JavaVersionDBEntry.XML_ELEMENT_NAME));
			/* verify the signature */
			if (SignatureVerifier.getInstance().verifyXml(node,
				SignatureVerifier.DOCUMENT_CLASS_UPDATE) == true)
			{
				Database.getInstance(JavaVersionDBEntry.class).update(new JavaVersionDBEntry(node));
				httpResponse = new HttpResponseStructure(HttpResponseStructure.HTTP_RETURN_OK);
			}
			else
			{
				LogHolder.log(LogLevel.WARNING, LogType.NET,
							  "Signature check failed for Java version entry! XML: " +
							  XMLUtil.toString(node));
				httpResponse = new HttpResponseStructure(HttpResponseStructure.
					HTTP_RETURN_INTERNAL_SERVER_ERROR);
			}
		}
		catch (Exception e)
		{
			LogHolder.log(LogLevel.ERR, LogType.NET, e);
			httpResponse = new HttpResponseStructure(HttpResponseStructure.HTTP_RETURN_BAD_REQUEST);
		}
		return httpResponse;
	}

	private String getHumanStatusHeader()
	{
		return "<HTML>\n" +
		"  <HEAD>\n" +
		"    <TITLE>JAP - InfoService - Cascade Status</TITLE>\n" +
		"    <STYLE TYPE=\"text/css\">\n" +
		"      <!--\n" +
		"        h1 {color:blue; text-align:center;}\n" +
		"        b,h3,h4,h5 {font-weight:bold; color:maroon;}\n" +
		"        body {margin-top:0px; margin-left:5px; margin-width:0px; margin-height:0px; background-color:white; color:black;}\n" +
		"        h1,h2,h3,h4,h5,p,address,ol,ul,tr,td,th,blockquote,body,.smalltext,.leftcol {font-family:geneva,arial,helvetica,sans-serif;}\n" +
		"        p,address,ol,ul,tr,td,th,blockquote {font-size:11pt;}\n" +
		"        .leftcol,.smalltext {font-size: 10px;}\n" +
		"        h1 {font-size:17px;}\n" +
		"        h2 {font-size:16px;}\n" +
		"        h3 {font-size:15px;}\n" +
		"        h4 {font-size:14px;}\n" +
		"        h5 {font-size:13px;}\n" +
		"        address {font-style:normal;}\n" +
		"        hr {color:#cccccc;}\n" +
		"        h2,.leftcol {font-weight:bold; color:#006699;}\n" +
		"        a:link {color:#006699; font-weight:bold; text-decoration:none;}\n" +
		"        a:visited {color:#666666; font-weight:bold; text-decoration:none;}\n" +
		"        a:active {color:#006699; font-weight:bold; text-decoration:none;}\n" +
		"        a:hover {color:#006699; font-weight:bold; text-decoration:underline;}\n" +
		"        th {color:white; background:#006699; font-weight:bold; text-align:left;}\n" +
		"        td.name {border-bottom-style:solid; border-bottom-width:1pt; border-color:#006699; background:#eeeeff;}\n" +
		"        td.status {border-bottom-style:solid; border-bottom-width:1pt; border-color:#006699;}\n" +
		"      -->\n" +
		"    </STYLE>\n" +
		"    <META HTTP-EQUIV=\"refresh\" CONTENT=\"25\">\n" +
		"  </HEAD>\n" +
		"  <BODY BGCOLOR=\"#FFFFFF\">\n" +
		"    <P ALIGN=\"right\">" + (new Date()).toString() + "</P>\n" +
		"    <H2>InfoService Status (" + Configuration.getInstance().getID() + ")</H2>\n" +
		"    <P>InfoService Name: " + Configuration.getInstance().getOwnName() + "<BR></P>\n";
	}
	
	private String getHumanStatusFooter()
	{
		return 
		"    <P>Infoservice [" + Constants.INFOSERVICE_VERSION + "] Startup Time: " +
			Configuration.getInstance().getStartupTime() +
			"</P>\n" +
			"    <HR noShade SIZE=\"1\">\n" +
			"    <ADDRESS>&copy; 2000 - 2008 The JAP Team - JonDos GmbH</ADDRESS>\n" +
			"  </BODY>\n" +
			"</HTML>\n";
	}
	
	private HttpResponseStructure humanGetPerfStatus()
	{
		HttpResponseStructure httpResponse;
		try
		{
			String htmlData = getHumanStatusHeader();
			
			htmlData += "<a href=\"/status\">Go to Server Status</a><br /><br />";
			
			if(Configuration.getInstance().isPerfEnabled() && InfoService.getPerfMeter() != null)
			{
				htmlData += "    <table style=\"align: left\" border=\"0\" width=\"30%\"><tr><th colspan=\"2\">Performance Monitoring Enabled</th></tr>\n" + 
				"<tr><td class=\"name\">Proxy host</td><td class=\"status\">" + Configuration.getInstance().getPerformanceMeterConfig()[0] + "</td></tr>" +
				"<tr><td class=\"name\">Proxy port</td><td class=\"status\">" + Configuration.getInstance().getPerformanceMeterConfig()[1] + "<td></tr>" +
				"<tr><td class=\"name\">Datasize</td><td class=\"status\">" + JAPUtil.formatBytesValueWithUnit(((Integer)Configuration.getInstance().getPerformanceMeterConfig()[2]).intValue()) + "<td></tr>" +
				"<tr><td class=\"name\">Major interval</td><td class=\"status\">" + Configuration.getInstance().getPerformanceMeterConfig()[3] + " ms<td></tr>" +
				"<tr><td class=\"name\">Requests per interval</td><td class=\"status\">" + Configuration.getInstance().getPerformanceMeterConfig()[4] + "<td></tr>" +
				"<tr><td class=\"name\">Account directory</td><td class=\"status\">" + Configuration.getInstance().getPerfAccountDirectory() + "<td></tr>" +
				"<tr><td class=\"name\">Last successful update</td><td class=\"status\">" + (InfoService.getPerfMeter().getLastSuccessfulUpdate() == 0 ? "(never)" : new Date(InfoService.getPerfMeter().getLastSuccessfulUpdate()).toString()) + "</td></tr>" +
				"<tr><td class=\"name\">Current time</td><td class=\"status\">" + new Date().toString() + "</td></tr>" +
				"<tr><td class=\"name\">Next update attempt</td><td class=\"status\">" + (InfoService.getPerfMeter().getNextUpdate() == 0 ? "(unknown)" : new Date(InfoService.getPerfMeter().getNextUpdate()).toString()) + "</td></tr>" +
				"<tr><td class=\"name\">Last run total updates</td><td class=\"status\">" + InfoService.getPerfMeter().getLastTotalUpdates() + "</td></tr>" +
				"<tr><td class=\"name\">Last Cascade updated</td><td class=\"status\">" + InfoService.getPerfMeter().getLastCascadeUpdated() + "</td></tr>" +
				"</table><br />" +
				"<table style=\"align: left\" border=\"0\" width=\"30%\">" +
				"<tr><td class=\"name\">Accumulated Total Traffic</td><td class=\"status\">" + JAPUtil.formatBytesValueWithUnit(InfoService.getPerfMeter().getBytesRecvd()) + "</td></tr>" +
				"</table><br />";
				
				Vector vPIs = Database.getInstance(PaymentInstanceDBEntry.class).getEntryList();
				
				for(int i = 0; i < vPIs.size(); i++)
				{
					PaymentInstanceDBEntry pi = (PaymentInstanceDBEntry) vPIs.elementAt(i);
					
					htmlData += "<h2>" + pi.getName() + "</h2><table style=\"align: left\" border=\"0\" width=\"30%\">" +
					"<tr><td class=\"name\">Estimated PayTraffic per Day</td><td class=\"status\">" + JAPUtil.formatBytesValueWithUnit(InfoService.getPerfMeter().calculatePayTrafficPerDay(pi.getId())) + "</td></tr>" +
					"<tr><td class=\"name\">Remaining PayCredit</td><td class=\"status\">" + JAPUtil.formatBytesValueWithUnit(InfoService.getPerfMeter().getRemainingCredit(pi.getId())) + "</td></tr>" +				
					"<tr><td class=\"name\">Estimated Pay End Time</td><td class=\"status\">" + (InfoService.getPerfMeter().calculateRemainingPayTime(pi.getId()) == 0 ? "(unknown)" : new Date(InfoService.getPerfMeter().calculateRemainingPayTime(pi.getId())).toString()) + "</td></tr>" +
					"</table><br />";
				}
				
				if(vPIs.size() == 0)
				{
					htmlData += "Waiting for PaymentInstance data...<br /><br />";
				}
				
				htmlData += "    <table style=\"align: left\" border=\"0\" width=\"30%\">" +
				"<tr><th>Account File</th><th>Last Modified</th></tr>\n";
				
				Hashtable usedFiles = InfoService.getPerfMeter().getUsedAccountFiles();
				
				Enumeration keys = usedFiles.keys();
				while (keys.hasMoreElements())
				{
					File file = (File) keys.nextElement();
					
					htmlData += "<tr><td class=\"name\">" + file.getName() + "</td><td class=\"status\">" + new Date(file.lastModified()) + "</td></tr>";
				}
				
				htmlData += "</table><br />";
			}
			else
			{
				htmlData += "Performance Monitoring disabled.";
			}
			
			htmlData += getHumanStatusFooter();
			
			/* send content */
			httpResponse = new HttpResponseStructure(HttpResponseStructure.HTTP_TYPE_TEXT_HTML,
				HttpResponseStructure.HTTP_ENCODING_PLAIN, htmlData);
		}
		catch (Exception e)
		{
			httpResponse = new HttpResponseStructure(HttpResponseStructure.HTTP_RETURN_INTERNAL_SERVER_ERROR);
			LogHolder.log(LogLevel.ERR, LogType.MISC, e);
		}
		return httpResponse;
	}
	
	/**
	 * Sends a generated HTML file with all status entrys to the client. This function is not used
	 * by the JAP client. It's intended to use with a webbrowser to see the status of all cascades.
	 *
	 * @return The HTTP response for the client.
	 */
	private HttpResponseStructure humanGetStatus()
	{
		/* this is only the default, if something is going wrong */
		HttpResponseStructure httpResponse;
		try
		{
				String htmlData = getHumanStatusHeader();
				
				htmlData +="    <TABLE BORDER=\"0\">\n" +
				"      <COLGROUP>\n" +
				"        <COL WIDTH=\"20%\">\n" +
				"        <COL WIDTH=\"15%\">\n" +
				"        <COL WIDTH=\"10%\">\n" +
				"        <COL WIDTH=\"2%\">\n" +
				"        <COL WIDTH=\"10%\">\n" +
				"        <COL WIDTH=\"5%\">\n" +				
				"        <COL WIDTH=\"17%\">\n" +
				"        <COL WIDTH=\"3%\">\n" +				
				"        <COL WIDTH=\"20%\">\n" +
				"      </COLGROUP>\n" +
				"      <TR>\n" +
				"        <TH>Cascade Name</TH>\n" +
				"        <TH>Cascade ID</TH>\n" +
				"        <TH>Active Users</TH>\n" +
				"        <TH>Current Risk</TH>\n" +
				"        <TH>Traffic Situation</TH>\n" +
				"        <TH>Avg Delay</TH>\n" +
				"        <TH>Avg Speed</TH>\n" +
				"        <TH>Mixed Packets</TH>\n" +
				"        <TH>Last Notification</TH>\n" +
				"      </TR>\n";
			/* get all status entries from database */
			Enumeration enumer = Database.getInstance(StatusInfo.class).getEntrySnapshotAsEnumeration();
			StatusInfo info;
			while (enumer.hasMoreElements())
			{
				info = (StatusInfo) (enumer.nextElement());
				/* get the HTML table line */
				htmlData = htmlData + "      " + (info).getHtmlTableLine() +
					"\n";
			}
			htmlData = htmlData + "    </TABLE><BR>";
			
			if(Configuration.getInstance().isPerfEnabled() && InfoService.getPerfMeter() != null)
			{
				htmlData += "<a href=\"/perfstatus\">Performance Monitoring enabled</a>";				
			}
			
			htmlData += "<BR><BR><BR>\n" +
			ISRuntimeStatistics.getAsHTML();
			
			htmlData += getHumanStatusFooter();
			
			/* send content */
			httpResponse = new HttpResponseStructure(HttpResponseStructure.HTTP_TYPE_TEXT_HTML,
				HttpResponseStructure.HTTP_ENCODING_PLAIN, htmlData);
		}
		catch (Exception e)
		{
			httpResponse = new HttpResponseStructure(HttpResponseStructure.HTTP_RETURN_INTERNAL_SERVER_ERROR);
			LogHolder.log(LogLevel.ERR, LogType.MISC, e);
		}
		return httpResponse;
	}

	/**
	 * Sends the complete list of all known mixes to the client. This command is not used by
	 * the JAP client. It's just a comfort function to see all currently working mixes.
	 *
	 * @return The HTTP response for the client.
	 */
	private HttpResponseStructure fetchAllMixes()
	{
		/* this is only the default, if something is going wrong */
		HttpResponseStructure httpResponse;
		try
		{
			/* create xml document */
			Document doc = XMLUtil.createDocument();
			/* create the Mixes element */
			Element mixesNode = doc.createElement("Mixes");
			/* append the nodes of all mixes we know */
			Enumeration knownMixes = Database.getInstance(MixInfo.class).getEntrySnapshotAsEnumeration();
			Element mixNode;
			MixInfo mixInfo;
			while (knownMixes.hasMoreElements())
			{
				/* import the mix node in this document */
				mixInfo = (MixInfo) knownMixes.nextElement();
				mixNode = mixInfo.getXmlStructure();
				if (mixNode == null)
				{
					LogHolder.log(LogLevel.EMERG, LogType.MISC, 
							"Mix node XML is null for Mix " + mixInfo.getId() + 
							" (" + mixInfo.getName() + ")!");
					continue;
				}
				mixNode = (Element) XMLUtil.importNode(doc, mixNode, true);
				mixesNode.appendChild(mixNode);
			}
			SignatureCreator.getInstance().signXml(SignatureVerifier.DOCUMENT_CLASS_INFOSERVICE, mixesNode);
			doc.appendChild(mixesNode);
			/* send the XML document to the client */
			httpResponse = new HttpResponseStructure(doc);
		}
		catch (Exception e)
		{
			httpResponse = new HttpResponseStructure(HttpResponseStructure.HTTP_RETURN_INTERNAL_SERVER_ERROR);
			LogHolder.log(LogLevel.ERR, LogType.MISC, e);
		}
		return httpResponse;
	}

	/**
	 * Constructs an XML structure containing a list of all free mixes we know. Those mixes were
	 * announced to us via the '/configure' method and are currently not assigned to any cascade.
	 *
	 * @return The HTTP response for the client.
	 */
	private HttpResponseStructure fetchAvailableMixes()
	{
		/* this is only the default, if something is going wrong */
		HttpResponseStructure httpResponse;
		try
		{
			/* create xml document */
			Document doc = XMLUtil.createDocument();
			/* create the Mixes element */
			Element mixesNode = doc.createElement("Mixes");
			/* append the nodes of all free mixes we know */
			Enumeration knownMixes = Database.getInstance(MixInfo.class).getEntrySnapshotAsEnumeration();
			while (knownMixes.hasMoreElements())
			{
				/* import the mix node in this document */
				MixInfo mixEntry = (MixInfo) (knownMixes.nextElement());
				if (mixEntry.isFreeMix())
				{
					Element mixNode = (Element) (XMLUtil.importNode(doc, mixEntry.getXmlStructure(), true));
					mixesNode.appendChild(mixNode);
				}
			}
			doc.appendChild(mixesNode);
			/* send the XML document to the client */
			httpResponse = new HttpResponseStructure(doc);
		}
		catch (Exception e)
		{
			httpResponse = new HttpResponseStructure(HttpResponseStructure.HTTP_RETURN_INTERNAL_SERVER_ERROR);
			LogHolder.log(LogLevel.ERR, LogType.MISC, e);
		}
		return httpResponse;
	}
	
	/**
	 * Sends the XML encoded mix cascade entry the ID given by cascadeId to the client.
	 *
	 * @param a_supportedEncodings defines the encoding supported by the client (deflate, gzip,...)
	 * @param a_cascadeId The ID of the requested mix cascade.
	 * 
	 * @return The HTTP response for the client.
	 */
	private HttpResponseStructure getCascadeInfo(int a_supportedEncodings, String a_cascadeId)
	{
		HttpResponseStructure httpResponse;
		try
		{
			MixCascade mixCascadeEntry =
				(MixCascade) (Database.getInstance(MixCascade.class).getEntryById(a_cascadeId));
			if (mixCascadeEntry == null)
			{
				/* we don't have a mixcascade with the given id */
				httpResponse = new HttpResponseStructure(HttpResponseStructure.HTTP_RETURN_NOT_FOUND);
			}
			else
			{
				/* send XML-Document */
				if ( (a_supportedEncodings & HttpResponseStructure.HTTP_ENCODING_ZLIB) > 0)
				{
					httpResponse = new HttpResponseStructure(
						HttpResponseStructure.HTTP_TYPE_TEXT_XML, HttpResponseStructure.HTTP_ENCODING_ZLIB,
						mixCascadeEntry.getCompressedData());
				}
				else
				{
					httpResponse = new HttpResponseStructure(
						HttpResponseStructure.HTTP_TYPE_TEXT_XML, HttpResponseStructure.HTTP_ENCODING_PLAIN,
						XMLUtil.toString(mixCascadeEntry.getXmlStructure()));
				}
			}
		}
		catch (Exception e)
		{
			httpResponse =
				new HttpResponseStructure(HttpResponseStructure.HTTP_RETURN_INTERNAL_SERVER_ERROR);
			LogHolder.log(LogLevel.ERR, LogType.MISC, e);
		}
		return httpResponse;
	}

	private HttpResponseStructure getInfoServiceInfo(String a_infoserviceId)
	{
		HttpResponseStructure httpResponse;
		try
		{
			InfoServiceDBEntry infoserviceEntry =
				(InfoServiceDBEntry) (Database.getInstance(InfoServiceDBEntry.class).getEntryById(
					a_infoserviceId));
			if (infoserviceEntry == null)
			{
				/* we don't have a mixcascade with the given id */
				httpResponse = new HttpResponseStructure(HttpResponseStructure.HTTP_RETURN_NOT_FOUND);
			}
			else
			{
				/* send XML-Document */
				httpResponse = new HttpResponseStructure(
					HttpResponseStructure.HTTP_TYPE_TEXT_XML, HttpResponseStructure.HTTP_ENCODING_PLAIN,
					XMLUtil.toString(infoserviceEntry.getXmlStructure()));
			}
		}
		catch (Exception e)
		{
			httpResponse = new HttpResponseStructure(HttpResponseStructure.HTTP_RETURN_INTERNAL_SERVER_ERROR);
			LogHolder.log(LogLevel.ERR, LogType.MISC, e);
		}
		return httpResponse;
	}

	private HttpResponseStructure echoIP(InetAddress a_sourceAddress)
	{
		HttpResponseStructure httpResponse;
		Document docEchoIP = XMLUtil.createDocument();
		Node nodeEchoIP = docEchoIP.createElement("EchoIP");
		Node nodeIP = docEchoIP.createElement("IP");

		docEchoIP.appendChild(nodeEchoIP);
		nodeEchoIP.appendChild(nodeIP);
		SignatureCreator.getInstance().signXml(SignatureVerifier.DOCUMENT_CLASS_INFOSERVICE, nodeEchoIP);
		XMLUtil.setValue(nodeIP, a_sourceAddress.getHostAddress());
		httpResponse = new HttpResponseStructure(HttpResponseStructure.HTTP_TYPE_TEXT_XML,
												 HttpResponseStructure.HTTP_ENCODING_PLAIN,
												 XMLUtil.toString(docEchoIP));

		return httpResponse;
	}

	/**
	 * Sends the complete list of all known tor nodes to the client. This command is used by the
	 * JAP clients with tor integration. If we don't have a current tor nodes list, we return -1
	 * and the client will get an http error. So the client will ask another infoservice.
	 *
	 * @param a_supportedEncodings defines the encoding supported by the client (deflate, gzip,...)
	 *
	 * @return The HTTP response for the client.
	 */
	private HttpResponseStructure getTorNodesList(int a_supportedEncodings)
	{
		ISRuntimeStatistics.ms_lNrOfGetTorNodesRequests++;
		/* this is only the default, if we don't have a TOR list */
		HttpResponseStructure httpResponse = null;
		byte[] torNodesList = null;
		int encoding;
		if ( (a_supportedEncodings & HttpResponseStructure.HTTP_ENCODING_ZLIB) > 0)
		{
			encoding = HttpResponseStructure.HTTP_ENCODING_ZLIB;
			torNodesList = TorDirectoryAgent.getInstance().getCompressedTorNodesList();
		}
		else
		{
			encoding = HttpResponseStructure.HTTP_ENCODING_PLAIN;
			torNodesList = TorDirectoryAgent.getInstance().getTorNodesList();
		}
		if (torNodesList != null)
		{
			try
			{
				/* create xml document */
				httpResponse = new HttpResponseStructure(HttpResponseStructure.HTTP_TYPE_TEXT_PLAIN,
					encoding, torNodesList);
			}
			catch (Exception e)
			{
				LogHolder.log(LogLevel.ERR, LogType.MISC, e);
				httpResponse = new HttpResponseStructure(HttpResponseStructure.
					HTTP_RETURN_INTERNAL_SERVER_ERROR);
			}
		}
		else
		{
			httpResponse = new HttpResponseStructure(HttpResponseStructure.
				HTTP_RETURN_INTERNAL_SERVER_ERROR);
		}

		return httpResponse;
	}

	/**
	 * Sends the complete list of all known mixminion nodes to the client. This command is used by the
	 * JAP clients with mixminion integration. If we don't have a current mixminion nodes list, we return -1
	 * and the client will get an http error. So the client will ask another infoservice.
	 *
	 * @return The HTTP response for the client.
	 */
	private HttpResponseStructure getMixminionNodesList()
	{
		/* this is only the default, if we don't have a TOR list */
		HttpResponseStructure httpResponse = null;
		byte[] mixminionNodesList = null;
		mixminionNodesList = MixminionDirectoryAgent.getInstance().getMixminionNodesList();
		if (mixminionNodesList != null)
		{
			try
			{
				httpResponse = new HttpResponseStructure(HttpResponseStructure.HTTP_TYPE_TEXT_PLAIN,
					HttpResponseStructure.HTTP_ENCODING_GZIP, mixminionNodesList);
			}
			catch (Exception e)
			{
				LogHolder.log(LogLevel.ERR, LogType.MISC, e);
				httpResponse = new HttpResponseStructure(HttpResponseStructure.
					HTTP_RETURN_INTERNAL_SERVER_ERROR);
			}
		}
		else
		{
			httpResponse = new HttpResponseStructure(HttpResponseStructure.
				HTTP_RETURN_INTERNAL_SERVER_ERROR);
		}
		return httpResponse;
	}

	/**
	 * Adds a new JAP forwarder to the database of JAP forwarders. But first we verify the
	 * connection, if this is successful we add the entry and send the forwarder entry ID back
	 * to the forwarder. So he knows under which ID he can renew the entry.
	 *
	 * @param a_postData The data we have received.
	 * @param a_sourceAddress The internet address where the request was coming from. We use this
	 *                        for checking the connection to the forwarder.
	 *
	 * @return The HTTP response for the client.
	 */
	private HttpResponseStructure addJapForwarder(byte[] a_postData, InetAddress a_sourceAddress)
	{
		/* this is only the default, if we don't have a primary forwarder list */
		HttpResponseStructure httpResponse = new HttpResponseStructure(HttpResponseStructure.
			HTTP_RETURN_BAD_REQUEST);
		String answer = JapForwardingTools.addForwarder(a_postData, a_sourceAddress);
		if (answer != null)
		{
			httpResponse = new HttpResponseStructure(HttpResponseStructure.HTTP_TYPE_TEXT_XML,
				HttpResponseStructure.HTTP_ENCODING_PLAIN, answer);
		}
		return httpResponse;
	}

	/**
	 * Renews the entry of a JAP forwarder in the database of JAP forwarders. We write back some
	 * status information, whether it was successful.
	 *
	 * @param a_postData The data we have received.
	 *
	 * @return The HTTP response for the client.
	 */
	private HttpResponseStructure renewJapForwarder(byte[] a_postData)
	{
		/* this is only the default, if we don't have a primary forwarder list */
		HttpResponseStructure httpResponse = new HttpResponseStructure(HttpResponseStructure.
			HTTP_RETURN_BAD_REQUEST);
		String answer = JapForwardingTools.renewForwarder(a_postData);
		if (answer != null)
		{
			httpResponse = new HttpResponseStructure(HttpResponseStructure.HTTP_TYPE_TEXT_XML,
				HttpResponseStructure.HTTP_ENCODING_PLAIN, answer);
		}
		return httpResponse;
	}

	/**
	 * Gets a forwarder entry (encoded with a captcha) from the JAP forwarder database. If we have
	 * such a database, but there is no data in it, we send back an answer with the error
	 * description. If this infoservice doesn't have a primary forwarder list, we aks all known
	 * infoservices with such a list for an entry, until we get an entry from one of them or we
	 * asked all and no one has a JAP forwarder entry. In this case we send also an answer with
	 * the error description back to the client.
	 *
	 * @return The HTTP response for the client.
	 */
	private HttpResponseStructure getJapForwarder()
	{
		/* this is only the default, if something is going wrong */
		HttpResponseStructure httpResponse = new HttpResponseStructure(HttpResponseStructure.
			HTTP_RETURN_INTERNAL_SERVER_ERROR);
		String answer = JapForwardingTools.getForwarder();
		if (answer != null)
		{
			httpResponse = new HttpResponseStructure(HttpResponseStructure.HTTP_TYPE_TEXT_XML,
				HttpResponseStructure.HTTP_ENCODING_PLAIN, answer);
		}
		return httpResponse;
	}

	/**
	 * This method is called, when we receive data from another infoservice with the minimum
	 * required JAP client version.
	 *
	 * @param a_postData The data we have received.
	 *
	 * @return The HTTP response for the client.
	 */
	private HttpResponseStructure japPostCurrentJapVersion(byte[] a_postData)
	{
		HttpResponseStructure httpResponse = new HttpResponseStructure(HttpResponseStructure.HTTP_RETURN_OK);
		try
		{
			LogHolder.log(LogLevel.DEBUG, LogType.NET,
						  "JAPMinVersion received: XML: " + (new String(a_postData)));
			Element japNode = (Element) (XMLUtil.getFirstChildByName(XMLUtil.toXMLDocument(a_postData),
				JAPMinVersion.getXmlElementName()));
			/* verify the signature */
			if (SignatureVerifier.getInstance().verifyXml(japNode, SignatureVerifier.DOCUMENT_CLASS_UPDATE) == true)
			{
				JAPMinVersion minVersionEntry = new JAPMinVersion(japNode);
				Database.getInstance(JAPMinVersion.class).update(minVersionEntry);
			}
			else
			{
				LogHolder.log(LogLevel.WARNING, LogType.NET,
							  "Signature check failed for JAPMinVersion entry! XML: " +
							  (new String(a_postData)));
				httpResponse = new HttpResponseStructure(HttpResponseStructure.
					HTTP_RETURN_INTERNAL_SERVER_ERROR);
			}
		}
		catch (Exception e)
		{
			LogHolder.log(LogLevel.ERR, LogType.NET, e);
			httpResponse = new HttpResponseStructure(HttpResponseStructure.HTTP_RETURN_BAD_REQUEST);
		}
		return httpResponse;
	}

	/**
	 * Sends the version number of the current minimum required JAP client software as an XML
	 * structure.
	 * Note: This respects the update propability as specifed in the configuration file.
	 *
	 * @return The HTTP response for the client.
	 */
	private HttpResponseStructure japGetCurrentJapVersion()
	{
		ISRuntimeStatistics.ms_lNrOfGetMinJapVersion++;
		/* this is only the default, if we don't know the current JAP version */
		HttpResponseStructure httpResponse = null;
		JAPMinVersion oldMinVersion = Configuration.getInstance().getJapMinVersionOld();
		double updatePropability = Configuration.getInstance().getJapUpdatePropability();
		if (oldMinVersion != null && updatePropability < 1.0 && m_Random.nextDouble() >= updatePropability)
		{
			httpResponse = new HttpResponseStructure(HttpResponseStructure.HTTP_TYPE_TEXT_XML,
				HttpResponseStructure.HTTP_ENCODING_PLAIN, oldMinVersion.getPostData());
		}
		else
		{
			JAPMinVersion minVersionEntry = (JAPMinVersion) (Database.getInstance(JAPMinVersion.class).
				getEntryById("JAPMinVersion"));
			if (minVersionEntry != null)
			{
				httpResponse = new HttpResponseStructure(HttpResponseStructure.HTTP_TYPE_TEXT_XML,
					HttpResponseStructure.HTTP_ENCODING_PLAIN, minVersionEntry.getPostData());
			}
			else
			{
				httpResponse = new HttpResponseStructure(HttpResponseStructure.
					HTTP_RETURN_NOT_FOUND);
			}
		}
		return httpResponse;
	}

	/**
	 * This method is called, when we receive data from another infoservice with the current
	 * japRelease.jnlp or japDevelopment.jnlp Java WebStart files.
	 *
	 * @param a_fileName The filename of the JNLP file (full path starting with / + filename).
	 * @param a_postData The data we have received.
	 *
	 * @return The HTTP response for the client.
	 */
	private HttpResponseStructure postJnlpFile(String a_fileName, byte[] a_postData)
	{
		HttpResponseStructure httpResponse = new HttpResponseStructure(HttpResponseStructure.HTTP_RETURN_OK);
		try
		{
			LogHolder.log(LogLevel.DEBUG, LogType.NET,
						  "JNLP file received (" + a_fileName + "): XML: " + (new String(a_postData)));
			Element jnlpNode = (Element) (XMLUtil.getFirstChildByName(XMLUtil.toXMLDocument(a_postData),
				JAPVersionInfo.getXmlElementName()));
			/* verify the signature */
			if (SignatureVerifier.getInstance().verifyXml(jnlpNode, SignatureVerifier.DOCUMENT_CLASS_UPDATE) == true)
			{
				JAPVersionInfo jnlpEntry = null;
				if (a_fileName.equals("/japRelease.jnlp"))
				{
					jnlpEntry = new JAPVersionInfo(jnlpNode, JAPVersionInfo.JAP_RELEASE_VERSION);
				}
				else if (a_fileName.equals("/japDevelopment.jnlp"))
				{
					jnlpEntry = new JAPVersionInfo(jnlpNode, JAPVersionInfo.JAP_DEVELOPMENT_VERSION);
				}
				else
				{
					throw (new Exception("InfoServiceCommands: postJnlpFile: Invalid filename specified (" +
										 a_fileName + ")."));
				}
				Database.getInstance(JAPVersionInfo.class).update(jnlpEntry);
			}
			else
			{
				LogHolder.log(LogLevel.WARNING, LogType.NET,
							  "Signature check failed for JNLP file! XML: " + (new String(a_postData)));
				httpResponse = new HttpResponseStructure(HttpResponseStructure.
					HTTP_RETURN_INTERNAL_SERVER_ERROR);
			}
		}
		catch (Exception e)
		{
			LogHolder.log(LogLevel.ERR, LogType.NET, e);
			httpResponse = new HttpResponseStructure(HttpResponseStructure.HTTP_RETURN_BAD_REQUEST);
		}
		return httpResponse;
	}

	/**
	 * Sends the JNLP files for the JAP development or release version to the JAP client or any
	 * other system which makes a Java WebStart request.
	 *
	 * @param a_fileName The filename of the requested JNLP file (full path starting with / +
	 *                   filename).
	 * @param a_httpMethod Describes the HTTP method (can be GET or HEAD, see constants in
	 *                     Constants.java). Java WebStart requests firstly only the header without
	 *                     the content and the asks a second time for the whole thing (header +
	 *                     content).
	 *
	 * @return The HTTP response for the client.
	 */
	private HttpResponseStructure getJnlpFile(String a_fileName, int a_httpMethod)
	{
		/* this is only the default, if we cannot find the requested JNLP file */
		HttpResponseStructure httpResponse = new HttpResponseStructure(HttpResponseStructure.
			HTTP_RETURN_NOT_FOUND);
		JAPVersionInfo jnlpFile = (JAPVersionInfo) (Database.getInstance(JAPVersionInfo.class).getEntryById(
			a_fileName));
		if (jnlpFile != null)
		{
			if (a_httpMethod == Constants.REQUEST_METHOD_GET)
			{
				httpResponse = new HttpResponseStructure(HttpResponseStructure.HTTP_TYPE_APPLICATION_JNLP,
					HttpResponseStructure.HTTP_ENCODING_PLAIN,
					XMLUtil.toString(jnlpFile.getXmlStructure()));
			}
			else if (a_httpMethod == Constants.REQUEST_METHOD_HEAD)
			{
				httpResponse = new HttpResponseStructure(HttpResponseStructure.HTTP_TYPE_APPLICATION_JNLP,
					HttpResponseStructure.HTTP_ENCODING_PLAIN,
					XMLUtil.toString(jnlpFile.getXmlStructure()), true);
			}
			else
			{
				httpResponse = new HttpResponseStructure(HttpResponseStructure.HTTP_RETURN_BAD_REQUEST);
			}
		}
		return httpResponse;
	}

	/**
	 * This function sends the addresses of
	 * the proxy servers at the end of the cascades as plain text to the client. The info about
	 * the proxies comes from the configuration property file and from the information given
	 * by Last Mixes.
	 *
	 * @return The HTTP response for the client.
	 */
	private HttpResponseStructure getProxyAddresses()
	{
		/* this is only the default, if we don't know the proxy addresses */
		HttpResponseStructure httpResponse = new HttpResponseStructure(HttpResponseStructure.
			HTTP_RETURN_NOT_FOUND);
		String strConfiguredProxies = Configuration.getInstance().getProxyAddresses();
		String strReportedProxies = VisibleProxyAddresses.getVisibleAddresses();
		if (strConfiguredProxies == null)
		{
			if (strReportedProxies != null)
			{
				httpResponse = new HttpResponseStructure(HttpResponseStructure.HTTP_TYPE_TEXT_PLAIN,
					HttpResponseStructure.HTTP_ENCODING_PLAIN,
					strReportedProxies);
			}

		}
		else
		{
			if (strReportedProxies != null)
			{
				strConfiguredProxies += " " + strReportedProxies;
			}
			httpResponse = new HttpResponseStructure(HttpResponseStructure.HTTP_TYPE_TEXT_PLAIN,
				HttpResponseStructure.HTTP_ENCODING_PLAIN,
				strConfiguredProxies);
		}
		return httpResponse;
	}

	/**
	 * This is the handler for processing the InfoService commands.
	 *
	 * @param method The HTTP method used within the request from the client. See the REQUEST_METHOD
	 *               constants in anon.infoservice.Constants.
	 * @param a_supportedEncodings The HTTP encodings supported by the client. See the HTTP_ENCODING
	 * constants in HttpResonseStructure.
	 * @param command The URL requested from the client within the HTTP request. Normally this
	 *                should be an absolute path with a filename.
	 * @param postData The HTTP content data (maybe of size 0), if the request was an HTTP POST. If
	 *                 the HTTP method was not POST, this value is always null.
	 * @param a_sourceAddress The internet address from where we have received the request. It is
	 *                        the address of the other end of the socket connection, so maybe it is
	 *                        only the address of a proxy.
	 *
	 * @return The response to send back to the client. This value is null, if the request cannot
	 *         be handled by this implementation (maybe because of an invalid command, ...).
	 */
	public HttpResponseStructure processCommand(int method, int a_supportedEncodings,
												String command, byte[] postData, InetAddress a_sourceAddress)
	{
		if (method == Constants.REQUEST_METHOD_POST)
		{
			ISRuntimeStatistics.ms_lNrOfPosts++;
		}
		else if (method == Constants.REQUEST_METHOD_GET)
		{
			ISRuntimeStatistics.ms_lNrOfGets++;
		}
		else
		{
			ISRuntimeStatistics.ms_lNrOfOtherMethod++;
		}

		HttpResponseStructure httpResponse = null;
		if ( (command.startsWith("/mixcascadestatus/")) && (method == Constants.REQUEST_METHOD_GET))
		{
			/* JAP or someone else wants to get information about the status of the cascade with the
			 * given ID
			 * Full command: GET /mixcascadestatus/cascadeid
			 */
			String cascadeId = command.substring(18);
			httpResponse = japGetCascadeStatus(cascadeId);
		}
		else if (command.equals("/infoservice") && (method == Constants.REQUEST_METHOD_POST))
		{
			/** message from another infoservice (can be forwared by an infoservice), which includes
			 * information about that infoservice
			 */
			httpResponse = infoServerPostHelo(postData);
		}
		else if (command.startsWith("/infoservice/") && (method == Constants.REQUEST_METHOD_GET))
		{
			/* get information about the InfoService with the given ID (it's the same information as
			 * /infoservices but there you get information about all known infoservices)
			 * Full command: GET /infoservice/infoserviceId
			 */
			String infoserviceId = command.substring(13);
			httpResponse = getInfoServiceInfo(infoserviceId);
		}
		else if ( (command.equals("/infoservices")) && (method == Constants.REQUEST_METHOD_GET))
		{
			ISRuntimeStatistics.ms_lNrOfGetInfoservicesRequests++;
			/* JAP or someone else wants to get information about all infoservices we know */
			httpResponse = m_isResponseGetter.fetchResponse(a_supportedEncodings, false);
		}
		else if ( (command.equals("/infoserviceserials")) && (method == Constants.REQUEST_METHOD_GET))
		{
			ISRuntimeStatistics.ms_lNrOfGetInfoserviceserialsRequests++;
			/* JAP or someone else wants to get information about all infoservices we know */
			httpResponse = m_isResponseGetter.fetchResponse(a_supportedEncodings, true);
		}
		else if ( (command.equals("/cascade")) && (method == Constants.REQUEST_METHOD_POST))
		{
			/* message from the first mix of a cascade (can be forwarded by an infoservice), which
			 * includes information about the cascade, or from other IS
			 */
//			httpResponse = cascadePostHelo(postData, a_supportedEncodings);
			httpResponse = m_dynamicExtension.cascadePostHelo(postData, a_supportedEncodings);
		}
		else if ( (command.equals("/cascadeserials")) && (method == Constants.REQUEST_METHOD_GET))
		{
			ISRuntimeStatistics.ms_lNrOfGetCascadeserialsRequests++;
			/* JAP or someone else wants to get information about all cascade serial numbers we know */
			httpResponse = m_cascadeResponseGetter.fetchResponse(a_supportedEncodings, true);
		}
		else if ( (command.equals("/cascades")) && (method == Constants.REQUEST_METHOD_GET))
		{
			ISRuntimeStatistics.ms_lNrOfGetCascadesRequests++;
			/* JAP or someone else wants to get information about all cascades we know */
			httpResponse = m_cascadeResponseGetter.fetchResponse(a_supportedEncodings, false);
		}
		else if( (command.startsWith("/performanceinfo") && (method == Constants.REQUEST_METHOD_GET)))
		{
			ISRuntimeStatistics.ms_lNrOfPerformanceInfoRequests++;
			httpResponse = m_performanceResponseGetter.fetchResponse(a_supportedEncodings, false);
		}
		else if ( (command.equals("/helo")) && (method == Constants.REQUEST_METHOD_POST))
		{
			/* message from a mix (can be forwarded by an infoservice), which includes information
			 * about that mix
			 */
			httpResponse = mixPostHelo(postData);
		}
		else if ( (command.equals("/configure")) && (method == Constants.REQUEST_METHOD_POST))
		{
			/* message from a mix requesting configuration */
			httpResponse = mixPostConfigure(postData);
		}
		else if ( (command.startsWith("/mixinfo/")) && (method == Constants.REQUEST_METHOD_GET))
		{
			ISRuntimeStatistics.ms_lNrOfGetMixinfoRequests++;
			/* JAP or someone else wants to get information about the mix with the given ID
			 * Full command: GET /mixinfo/mixid
			 */
			String mixId = command.substring(9);
			httpResponse = japGetMix(mixId);
		}
		else if ( (command.equals("/feedback")) && (method == Constants.REQUEST_METHOD_POST))
		{
			/* message from the first mix of a cascade (can be forwarded by an infoservice), which
			 * includes status information (traffic) of that cascade
			 */
			httpResponse = cascadePostStatus(postData);
		}
		else if ( (command.equals("/status")) && (method == Constants.REQUEST_METHOD_GET))
		{
			/* get the status (traffic) information about all cascades for human view as html file */
			ISRuntimeStatistics.ms_lNrOfGetStatus++;
			httpResponse = humanGetStatus();
		}
		else if ( (command.equals("/perfstatus")) && (method == Constants.REQUEST_METHOD_GET))
		{
			/* get the status information about the performance monitoring for human view as html file */
			httpResponse = humanGetPerfStatus();
		}		
		else if ( (command.equals("/mixes")) && (method == Constants.REQUEST_METHOD_GET))
		{
			/* get information about all mixes (mixes of all cascades) */
			httpResponse = fetchAllMixes();
		}
		else if ( (command.equals("/availablemixes")) && (method == Constants.REQUEST_METHOD_GET))
		{
			/* get information about all mixes (mixes of all cascades) */
			httpResponse = fetchAvailableMixes();
		}
		else if ( (command.equals(MessageDBEntry.HTTP_REQUEST_STRING)) &&
				 (method == Constants.REQUEST_METHOD_GET))
		{
			httpResponse = m_messageResponseGetter.fetchResponse(a_supportedEncodings, false);
		}
		else if ( (command.equals(MessageDBEntry.HTTP_SERIALS_REQUEST_STRING)) &&
				 (method == Constants.REQUEST_METHOD_GET))
		{
			httpResponse = m_messageResponseGetter.fetchResponse(a_supportedEncodings, true);
		}
		else if ( (command.equals(MessageDBEntry.POST_FILE)) && (method == Constants.REQUEST_METHOD_POST))
		{
			httpResponse = messagePost(postData, a_supportedEncodings);
		}
		else if ( (command.startsWith("/cascadeinfo/")) && (method == Constants.REQUEST_METHOD_GET))
		{
			ISRuntimeStatistics.ms_lNrOfGetCascadeinfoRequests++;
			/* get information about the cascade with the given ID (it's the same information as
			 * /cascades but there you get information about all known cascades)
			 * Full command: GET /cascadeinfo/cascadeid
			 */
			String cascadeId = command.substring(13);
			httpResponse = getCascadeInfo(a_supportedEncodings, cascadeId);
		}
		else if ( (command.equals("/tornodes")) && (method == Constants.REQUEST_METHOD_GET))
		{
			/* get the list with all known tor nodes */
			httpResponse = getTorNodesList(a_supportedEncodings);
		}
		else if ( (command.equals("/mixminionnodes")) && (method == Constants.REQUEST_METHOD_GET))
		{
			/* get the list with all known mixminion nodes in an XML structure */
			httpResponse = getMixminionNodesList();
		}
		else if ( (command.equals("/addforwarder")) && (method == Constants.REQUEST_METHOD_POST))
		{
			ISRuntimeStatistics.ms_lNrOfGetForwarding++;
			/* adds a new JAP forwarder to the database of known forwarders */
			httpResponse = addJapForwarder(postData, a_sourceAddress);
		}
		else if ( (command.equals("/renewforwarder")) && (method == Constants.REQUEST_METHOD_POST))
		{
			ISRuntimeStatistics.ms_lNrOfGetForwarding++;
			/* renews a JAP forwarder in the database of known forwarders */
			httpResponse = renewJapForwarder(postData);
		}
		else if ( (command.equals("/getforwarder")) && (method == Constants.REQUEST_METHOD_GET))
		{
			ISRuntimeStatistics.ms_lNrOfGetForwarding++;
			/* get a captcha with information about a JAP forwarder */
			httpResponse = getJapForwarder();
		}
		else if (command.equals(JavaVersionDBEntry.HTTP_REQUEST_STRING))
		{
			if (method == Constants.REQUEST_METHOD_GET)
			{
				httpResponse = m_javaVersionResponseGetter.fetchResponse(a_supportedEncodings, false);
				//httpResponse = getLatestJavaVersions();
			}
			else if (method == Constants.REQUEST_METHOD_POST)
			{
				httpResponse = postLatestJavaVersions(postData);
			}
		}
		else if (command.equals(JavaVersionDBEntry.HTTP_SERIALS_REQUEST_STRING))
		{
			httpResponse = m_javaVersionResponseGetter.fetchResponse(a_supportedEncodings, true);
		}
		else if ( (command.equals("/currentjapversion")) && (method == Constants.REQUEST_METHOD_POST))
		{
			/* message from another infoservice about the minimal needed JAP version */
			httpResponse = japPostCurrentJapVersion(postData);
		}
		else if ( (command.equals("/currentjapversion")) && (method == Constants.REQUEST_METHOD_GET))
		{
			/* get the current version of the client software */
			httpResponse = japGetCurrentJapVersion();
		}
		else if ( ( (command.equals("/japRelease.jnlp")) || (command.equals("/japDevelopment.jnlp"))) &&
				 (method == Constants.REQUEST_METHOD_POST))
		{
			/* message from another infoservice with information about new JAP software */
			httpResponse = postJnlpFile(command, postData);
		}
		else if ( ( (command.equals("/japRelease.jnlp")) || (command.equals("/japDevelopment.jnlp"))) &&
				 ( (method == Constants.REQUEST_METHOD_GET) || (method == Constants.REQUEST_METHOD_HEAD)))
		{
			// request for JNLP File (WebStart or Update Request
			httpResponse = getJnlpFile(command, method);
		}
		else if ( (command.equals("/proxyAddresses")) && (method == Constants.REQUEST_METHOD_GET))
		{
			/* returns the addresses from the proxies at the end of the cascades, only
			 * for compatibility with some old scripts (written before world war II)
			 */
			/** @todo remove it */
			httpResponse = getProxyAddresses();
		}
		else if (command.equals("/echoip") && (method == Constants.REQUEST_METHOD_GET ||
											   method == Constants.REQUEST_METHOD_HEAD))
		{
			// just echo the clients ip adresse - for mix autoconfig resons
			httpResponse = echoIP(a_sourceAddress);
		}
		else if ( (command.equals("/paymentinstance")) && (method == Constants.REQUEST_METHOD_POST))
		{
			/* message from a payment instance or another infoservice (can be forwared by an infoservice), which includes
			 * information about that payment instance
			 */
			ISRuntimeStatistics.ms_lNrOfGetPaymentRequests++;
			httpResponse = paymentInstancePostHelo(postData);
		}
		else if ( (command.equals("/paymentinstances")) && (method == Constants.REQUEST_METHOD_GET))
		{
			/* JAP or someone else wants to get information about all payment instacnes we know */
			ISRuntimeStatistics.ms_lNrOfGetPaymentRequests++;
			httpResponse = japFetchPaymentInstances();

		}
		else if ( (command.startsWith("/paymentinstance/")) && (method == Constants.REQUEST_METHOD_GET))
		{
			/* JAP or someone else wants to get information about a special payment instance */
			ISRuntimeStatistics.ms_lNrOfGetPaymentRequests++;
			String piID = command.substring(17);
			httpResponse = japFetchPaymentInstanceInfo(piID);

		}
		// LERNGRUPPE
		else if (command.startsWith("/connectivity") && (method == Constants.REQUEST_METHOD_POST))
		{
			httpResponse = m_dynamicExtension.mixPostConnectivityTest(a_sourceAddress, postData);
		}
		else if (command.startsWith("/dynacasade") && (method == Constants.REQUEST_METHOD_POST))
		{
			httpResponse = m_dynamicExtension.lastMixPostDynaCascade(postData);
		}
		else if ( (command.startsWith("/newcascadeinformationavailable/"))
				 && (method == Constants.REQUEST_METHOD_GET))
		{
			String piID = command.substring(32);
			httpResponse = m_dynamicExtension.isNewCascadeAvailable(piID);
		}
		else if ( (command.startsWith("/reconfigure/"))
				 && (method == Constants.REQUEST_METHOD_GET))
		{
			String piID = command.substring(13);
			httpResponse = m_dynamicExtension.reconfigureMix(piID);
		}
		else if (command.startsWith("/agreement") && (method == Constants.REQUEST_METHOD_POST))
		{

			httpResponse = m_agreementAdapter.handleMessage(postData);

		}
		else if (command.startsWith("/startagreement")
				 && (method == Constants.REQUEST_METHOD_GET))
		{
			m_agreementAdapter.startProtocolByOperator();
			httpResponse = new HttpResponseStructure(HttpResponseStructure.HTTP_RETURN_OK);
		}
		else if (command.startsWith("/virtualcascades")
				 && (method == Constants.REQUEST_METHOD_GET))
		{
			httpResponse = m_dynamicExtension.virtualCascadeStatus();
		}
		else
		{
			ISRuntimeStatistics.ms_lNrOfUnknownRequests++;
		}

		return httpResponse;
	}

}
