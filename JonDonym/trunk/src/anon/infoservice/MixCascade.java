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
package anon.infoservice;

import java.net.InetAddress;
import java.util.Enumeration;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.Date;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import anon.AnonServerDescription;
import anon.crypto.MultiCertPath;
import anon.crypto.SignatureVerifier;
import anon.crypto.X509DistinguishedName;
import anon.crypto.XMLSignature;
import anon.crypto.CertPath;
import anon.util.XMLParseException;
import anon.util.XMLUtil;
import anon.util.ZLibTools;
import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;
import anon.crypto.IVerifyable;
import java.util.Hashtable;
import anon.pay.AIControlChannel;

/**
 * Holds the information for a mixcascade.
 */
public class MixCascade extends AbstractDistributableCertifiedDatabaseEntry
	implements AnonServerDescription, IVerifyable, IServiceContextContainer, Database.IWebInfo
{
	public static final String SUPPORTED_PAYMENT_PROTOCOL_VERSION = "2.0";
	
	public static final int DISTRIBUTION_MIN = 0;
	public static final int DISTRIBUTION_MAX = 6;

	public static final String XML_ELEMENT_NAME = "MixCascade";
	public static final String XML_ELEMENT_CONTAINER_NAME = "MixCascades";

	private static final String XML_ATTR_USER_DEFINED = "userDefined";
	private static final String XML_ATTR_STUDY = "study";
	private static final String XML_ATTR_MAX_USERS = "maxUsers";
	private static final String XML_ATTR_PAYMENT = "payment";
	
	/* Constants for the JonDonym service status */
	public static final String XML_ELEMENT_WEBINFO_CONTAINER = "CascadeWebInfos";
	public static final String XML_ELEMENT_WEBINFO = "CascadeWebInfo";
	public static final String XML_ELEMENT_WEBINFO_CASCADE_NAME = "CascadeName";
	public static final String XML_ELEMENT_WEBINFO_NAME = "Name";
	public static final String XML_ELEMENT_WEBINFO_COMPOSED_NAME = "ComposedName";
	public static final String XML_ELEMENT_WEBINFO_CURR_USERS = "CurrentUsers";
	public static final String XML_ATTR_WEBINFO_MIX_COUNTRY = "mixCountry";
	public static final String XML_ATTR_WEBINFO_MIX_POSITION = "mixPosition";
	public static final String XML_ATTR_WEBINFO_OP_COUNTRY = "operatorCountry";
	
	public static final String INFOSERVICE_COMMAND_WEBINFOS = "/cascadewebinfos";
	public static final String INFOSERVICE_COMMAND_WEBINFO = "/cascadewebinfo/";
	
	//private static final String XML_ELEM_RSA_KEY_VALUE = "RSAKeyValue";

	private boolean m_bDefaultVerified = false;

	private boolean m_bImplicitTrust = false;

	private boolean m_bSock5Support = false;
	
	private boolean m_bDataRetention = false;

	/**
	 * This is the ID of the mixcascade.
	 */
	private String m_mixCascadeId;

	/**
	 * Time (see System.currentTimeMillis()) when the mixcascade (first mix) has sent this HELO
	 * message.
	 */
	private long m_lastUpdate;

	/**
	 * The name of the mixcascade.
	 */
	private String m_strName;
	
	private Vector m_decomposedCascadeName;

	/**
	 * Holds the information about the interfaces (IP, Port) the mixcascade (first mix) is listening
	 * on.
	 */
	private Vector m_listenerInterfaces;

	/**
	 * Holds IDs of all mixes in the cascade.
	 */
	private Vector m_mixIds;

	private String m_strMixIds;
	private String m_piid = "";

	private MixInfo[] m_mixInfos;
	private String m_strMixNames;
	private int m_nrPriceCerts = 0;

	private Vector m_mixNodes;

	private long m_serial;

	/**
	 * Stores the XML structure for this mixcascade.
	 * @todo remove the plain xml structure storage if new JAP version is released
	 */
	private Element m_xmlStructure;
	private byte[] m_compressedXmlStructure;

	private XMLSignature m_signature;
	private MultiCertPath m_certPath;

	private int m_nrCountries = 0;
	private int m_nrOperators = 0;
	private int m_nrOperatorsCountForDistribution = 0;
	private int m_nrOperatorsShown = 0;
	private int m_distributionPoints = 0;
	private boolean[] m_mixCertVerifiedAndValid;
	private Object SYNC_OPERATORS_AND_COUNTRIES = new Object();

	/**
	 * True, if this MixCascade is user defined, false if the Information comes from the
	 * InfoService. This value is only meaningful within the context of the JAP client.
	 */
	private boolean m_userDefined;
	
	/**
	 * If this Cascade participates in a study.
	 */
	private boolean m_bStudy = false;
	
	/**
	 * Is greater zero if user number is restricted.
	 */
	private int m_maxUsers = 0;

	/**
	 * True, if this MixCascade is a payment cascade.
	 */
	private boolean m_isPayment;
	
	private long m_prepaidInterval = AIControlChannel.MAX_PREPAID_INTERVAL;

	private String m_mixProtocolVersion;

	private String m_paymentProtocolVersion;

	private Hashtable m_priceCertificateHashes = new Hashtable();
	private Vector m_priceCertificates = new Vector();

	/**
	 * If this MixCascade has been received directly from a cascade connection.
	 */
	private boolean m_bFromCascade;
	private String m_context;

	
	/**
	 * Creates a new MixCascade from XML description (MixCascade node).
	 *
	 * @param a_bCompressedMixCascadeNode The MixCascade node from a compressed XML document.
	 */
	public MixCascade(byte[] a_bCompressedMixCascadeNode)
		throws XMLParseException
	{
		this(a_bCompressedMixCascadeNode, null, 0, null);
	}

	/**
	 * Creates a new MixCascade from XML description (MixCascade node).
	 *
	 * @param a_mixCascadeNode The MixCascade node from an XML document.
	 */
	public MixCascade(Element a_mixCascadeNode) throws XMLParseException
	{
		this(null, a_mixCascadeNode, 0, null);
	}

	/**
	 * Creates a new MixCascade from XML description (MixCascade node).
	 *
	 * @param a_mixCascadeNode The MixCascade node from an XML document.
	 * @param a_expireTime forces a specific expire time; takes default expire time if <= 0
	 */
	public MixCascade(Element a_mixCascadeNode, long a_expireTime)
		throws XMLParseException
	{
		this(null, a_mixCascadeNode, a_expireTime, null);
	}

	/**
	 * Creates a new MixCascade from XML description (MixCascade node).
	 *
	 * @param a_mixCascadeNode The MixCascade node from an XML document.
	 * @param a_expireTime forces a specific expire time; takes default expire time if <= 0
	 * @param a_mixIDFromCascade if this is a MixCascade node directly received from a cascade
	 * (it is stripped) it gets this mix id; otherwise it must be null
	 */
	public MixCascade(Element a_mixCascadeNode, long a_expireTime, String a_mixIDFromCascade)
		throws XMLParseException
	{
		this(null, a_mixCascadeNode, a_expireTime, a_mixIDFromCascade);
	}

	/**
	 * Creates a new MixCascade from XML description (MixCascade node).
	 *
	 * @param a_mixCascadeNode The MixCascade node from an XML document.
	 * @param a_expireTime forces a specific expire time; takes default expire time if <= 0
	 * @param a_mixIDFromCascade if this is a MixCascade node directly received from a cascade
	 * (it is stripped) it gets this mix id; otherwise it must be null
	 */
	private MixCascade(byte[] a_compressedMixCascadeNode, Element a_mixCascadeNode,
					   long a_expireTime, String a_mixIDFromCascade)
		throws XMLParseException
	{
		/* use always the timeout for the infoservice context, because the JAP client currently does
		 * not have a database of mixcascade entries -> no timeout for the JAP client necessary
		 */
		super(a_expireTime <= 0 ? (System.currentTimeMillis() + Constants.TIMEOUT_MIXCASCADE) : a_expireTime);
		m_bFromCascade = a_mixIDFromCascade != null;

		if (a_mixCascadeNode == null && a_compressedMixCascadeNode == null)
		{
			throw new XMLParseException(XMLParseException.NODE_NULL_TAG);
		}
		if (a_mixCascadeNode == null)
		{
			a_mixCascadeNode = (Element) (XMLUtil.getFirstChildByName(
						 XMLUtil.toXMLDocument(ZLibTools.decompress(a_compressedMixCascadeNode)),
						 MixCascade.XML_ELEMENT_NAME));
		}

		/* try to get the certificate from the Signature node */
		try
		{
			m_signature = SignatureVerifier.getInstance().getVerifiedXml(a_mixCascadeNode,
				SignatureVerifier.DOCUMENT_CLASS_MIX);
			if (m_signature != null)
			{
				m_certPath = m_signature.getMultiCertPath();
			}
			else
			{
				LogHolder.log(LogLevel.DEBUG, LogType.MISC,
							  "No signature node found while looking for MixCascade certificate.");
			}
		}
		catch (Exception e)
		{
			LogHolder.log(LogLevel.ERR, LogType.MISC,
						  "Error while looking for appended certificates in the MixCascade structure: " +
						  e.toString());
		}

		/* get the information, whether this mixcascade was user-defined within the JAP client */
		m_userDefined = XMLUtil.parseAttribute(a_mixCascadeNode, XML_ATTR_USER_DEFINED, false);
		
		m_bStudy = XMLUtil.parseAttribute(a_mixCascadeNode, XML_ATTR_STUDY, false);
		
		m_context = XMLUtil.parseAttribute(a_mixCascadeNode, XML_ATTR_CONTEXT, CONTEXT_JONDONYM);
		m_maxUsers = XMLUtil.parseAttribute(a_mixCascadeNode, XML_ATTR_MAX_USERS, 0);
		m_maxUsers = Math.min(m_maxUsers, 9999); // 10000 is seen as unlimited

		/* get the ID */
		if (a_mixCascadeNode == null || !a_mixCascadeNode.getNodeName().equals(XML_ELEMENT_NAME))
		{
			throw new XMLParseException(XML_ELEMENT_NAME);
		}
		m_mixCascadeId = XMLUtil.parseAttribute(a_mixCascadeNode, "id", null);
		if (m_mixCascadeId == null) //get id of first mix, if cascade id is not set
		{
			Node nodeMix =
				XMLUtil.getFirstChildByName(XMLUtil.getFirstChildByName(a_mixCascadeNode, "Mixes"), "Mix");

			m_mixCascadeId = XMLUtil.parseAttribute(nodeMix, "id", a_mixIDFromCascade);
		}
		if (!checkId())
		{
			throw new XMLParseException(XMLParseException.ROOT_TAG, "Malformed Mix-Cascade ID: " + m_mixCascadeId);
		}

		m_mixProtocolVersion =
			XMLUtil.parseValue(XMLUtil.getFirstChildByName(a_mixCascadeNode, "MixProtocolVersion"), null);
		if (m_mixProtocolVersion != null)
		{
			m_mixProtocolVersion = m_mixProtocolVersion.trim();
		}
		/* get payment info */
		Node payNode = XMLUtil.getFirstChildByName(a_mixCascadeNode, "Payment");
		m_isPayment = XMLUtil.parseAttribute(payNode, "required", false);
		m_paymentProtocolVersion = XMLUtil.parseAttribute(payNode, XML_ATTR_VERSION,
			SUPPORTED_PAYMENT_PROTOCOL_VERSION);
		
		m_prepaidInterval = XMLUtil.parseAttribute(payNode, "prepaidInterval", AIControlChannel.MAX_PREPAID_INTERVAL + 1);
		m_piid = XMLUtil.parseAttribute(payNode, "piid", "");

		if (!m_bFromCascade)
		{
			/* get the listener interfaces */
			NodeList networkNodes = a_mixCascadeNode.getElementsByTagName("Network");
			if (networkNodes.getLength() == 0)
			{
				throw new XMLParseException("Network");
			}
			Element networkNode = (Element) (networkNodes.item(0));
			NodeList listenerInterfacesNodes = networkNode.getElementsByTagName(ListenerInterface.XML_ELEMENT_CONTAINER_NAME);
			if (listenerInterfacesNodes.getLength() == 0)
			{
				throw new XMLParseException("ListenerInterfaces");
			}
			Element listenerInterfacesNode = (Element) (listenerInterfacesNodes.item(0));
			NodeList listenerInterfaceNodes = listenerInterfacesNode.getElementsByTagName(ListenerInterface.XML_ELEMENT_NAME);
			if (listenerInterfaceNodes.getLength() == 0)
			{
				throw new XMLParseException("ListenerInterface");
			}
			m_listenerInterfaces = new Vector();
			for (int i = 0; i < listenerInterfaceNodes.getLength(); i++)
			{
				Element listenerInterfaceNode = (Element) (listenerInterfaceNodes.item(i));
				m_listenerInterfaces.addElement(new ListenerInterface(listenerInterfaceNode));
			}
		}
		/* get the IDs of all mixes in the cascade */
		NodeList mixesNodes = a_mixCascadeNode.getElementsByTagName("Mixes");
		if (mixesNodes.getLength() == 0)
		{
			throw new XMLParseException("Mixes");
		}
		Element mixesNode = (Element) (mixesNodes.item(0));
		int nrOfMixes = Integer.parseInt(mixesNode.getAttribute("count"));
		NodeList mixNodes = mixesNode.getElementsByTagName("Mix");
		if ( (mixNodes.getLength() == 0) || (nrOfMixes != mixNodes.getLength()))
		{
			throw (new XMLParseException("Mix"));
		}
		m_mixIds = new Vector();
		m_mixNodes = new Vector();
		for (int i = 0; i < mixNodes.getLength(); i++)
		{
			Element mixNode = (Element) (mixNodes.item(i));
			m_mixIds.addElement(mixNode.getAttribute("id"));
			if (i == 0 && !isUserDefined() && !m_mixIds.lastElement().equals(m_mixCascadeId))
			{
				// The cascade has another id as the first mix!
				throw new XMLParseException(XMLParseException.ROOT_TAG,
											"Cascade ID not ID of first mix: " + m_mixCascadeId);
			}
			m_mixNodes.addElement(mixNode);
		}
		m_mixInfos = new MixInfo[mixNodes.getLength()];
		boolean bNullMixInfo = false;
		int countDataRetentionMixes = 0;
		for (int i = 0; i < mixNodes.getLength(); i++)
		{
			try
			{
				m_mixInfos[i] = new MixInfo((Element) mixNodes.item(i), a_expireTime, true);
				if (i + 1 == mixNodes.getLength())
				{
					m_bSock5Support = m_mixInfos[i].isSocks5Supported();
				}
				if (m_mixInfos[i].getPriceCertificate() != null)
				{
					if (i == 0)
					{
						m_piid = m_mixInfos[i].getPriceCertificate().getBiID();
					}
					if (i == 0 && m_prepaidInterval > AIControlChannel.MAX_PREPAID_INTERVAL)
					{
						m_prepaidInterval = m_mixInfos[i].getPrepaidInterval();
					}

					m_priceCertificates.addElement(m_mixInfos[i].getPriceCertificate());
					m_priceCertificateHashes.put(new MixPosition(i, m_mixInfos[i].getId()), // position in cascade
												 m_mixInfos[i].getPriceCertificate().getHashValue());
					m_nrPriceCerts++;
				}
				if (m_mixInfos[i].getDataRetentionURL("en") != null)
				{
					countDataRetentionMixes++;
				}
			}
			catch (XMLParseException a_e)
			{
				bNullMixInfo = true;
				m_mixInfos[i] = null;
			}
		}
		if (countDataRetentionMixes == mixNodes.getLength() || 
			(countDataRetentionMixes > 0 && bNullMixInfo))
		{
			m_bDataRetention = true;
		}
		
		/* get the name */
		m_strName = XMLUtil.parseValue(XMLUtil.getFirstChildByName(a_mixCascadeNode, "Name"), null);
		//@todo: rather use this for setting m_strName: generateNameFromMixNames()
		//(when the mix providers support it)
		if (m_strName == null && !m_bFromCascade)
		{
			generateNameFromMixNames();
			//throw (new XMLParseException("Name"));	
		}
		
		if (a_expireTime == 0 && m_mixInfos.length > 0)
		{
			// this seems to be InfoService context
			Vector visibleAddresses = m_mixInfos[m_mixInfos.length - 1].getVisibleAddresses();
			if (visibleAddresses.size() == 0)
			{
				// take the last mix IP addresses
				visibleAddresses = m_mixInfos[m_mixInfos.length - 1].getListenerAddresses();
			}
			for (int i = 0; i < visibleAddresses.size(); i++)
			{
				InetAddress addr = (InetAddress)visibleAddresses.elementAt(i);
				MixCascadeExitAddresses.addInetAddress(getId(), addr);
			}
		}
		
		if (m_mixCascadeId == null)
		{
			m_mixCascadeId = (String)m_mixIds.elementAt(0);
		}
		Node lastUpdateNode = XMLUtil.getFirstChildByName(a_mixCascadeNode, "LastUpdate");
		m_lastUpdate = XMLUtil.parseValue(lastUpdateNode,
										  System.currentTimeMillis() - Constants.TIMEOUT_MIXCASCADE);
		if (!m_bFromCascade)
		{
			if (lastUpdateNode == null)
			{
				throw new XMLParseException("LastUpdate");
			}

			m_serial = XMLUtil.parseAttribute(a_mixCascadeNode, XML_ATTR_SERIAL, m_lastUpdate);
		}
		else
		{
			m_serial = XMLUtil.parseAttribute(a_mixCascadeNode, XML_ATTR_SERIAL, Long.MIN_VALUE);
		}

		/* store the xml structure */
		if (a_compressedMixCascadeNode != null)
		{
			m_compressedXmlStructure = a_compressedMixCascadeNode;
		}
		else
		{
			m_compressedXmlStructure = ZLibTools.compress(XMLSignature.toCanonical(a_mixCascadeNode));
		}
		m_xmlStructure = a_mixCascadeNode;

		// restore the id if needed
		if (m_bFromCascade && a_mixIDFromCascade.trim().length() > 0)
		{
			m_mixCascadeId = a_mixIDFromCascade;
		}

		createMixIDString();
		calculateOperatorsAndCountries();

		// JAP will not pay more than the predefined maximum!
		if (isPayment())
		{
			if (m_piid == null)
			{
				throw new XMLParseException("Payment instance id is null on paid cascade!");
			}
			
			if (m_prepaidInterval > AIControlChannel.MAX_PREPAID_INTERVAL)
			{
				LogHolder.log(LogLevel.WARNING, LogType.PAY, "Prepaid interval of cascade " + getId() +
					"is too high: " + m_prepaidInterval);
			}
			else if (m_prepaidInterval < AIControlChannel.MIN_PREPAID_INTERVAL)
			{
				LogHolder.log(LogLevel.WARNING, LogType.PAY, "Prepaid interval of cascade " + getId() +
					"is too low: " + m_prepaidInterval);
			}
		}
		m_prepaidInterval = Math.min(m_prepaidInterval, AIControlChannel.MAX_PREPAID_INTERVAL);
		m_prepaidInterval = Math.max(m_prepaidInterval, AIControlChannel.MIN_PREPAID_INTERVAL);
	}

	/**
	 * Creates a new MixCascade from the hostName / IP and the port. The hostName and port are
	 * directly used for creating the ListenerInterface for this MixCascade. ID and the name
	 * are set to a generic value derived from the name and the port. The lastUpdate time is the
	 * current system time. One mixId is created, it is the same as the mixCascadeId. The current
	 * status is set to dummy value. Cause the infoservice does not know this mixCascadeId and the
	 * created mixId, you will never get a StatusInfo or a MixInfo other than the dummy one.
	 *
	 * @param a_hostName The hostname or IP address the mixcascade (first mix) is listening on.
	 * @param a_port The port the mixcascade (first mix) is listening on.
	 */
	public MixCascade(String a_hostName, int a_port) throws Exception
	{
		this(null, null, a_hostName, a_port);
	}

	/**
	 * Creates a new MixCascade from the hostName / IP and the port. The hostName and port are
	 * directly used for creating the ListenerInterface for this MixCascade. If ID and the name
	 * are not provided, than they are set to a generic value derived from the name and the port.
	 * The lastUpdate time is the current system time. One mixId is created, it is the same as the
	 * mixCascadeId. The current status is set to dummy value. Cause the infoservice does not know
	 * this mixCascadeId and the created mixId, you will never get a StatusInfo or a MixInfo other
	 * than the dummy one.
	 *
	 * @param a_name A human readable name of this cascade, which could be display on the UI. If
	 *               this value is null the name will be constructed from hostName and port.
	 * @param a_id The ID of this cascade. If null than it will be constructed from hostName and
	 *             port.
	 * @param a_hostName The hostname or IP address the mixcascade (first mix) is listening on.
	 * @param a_port The port the mixcascade (first mix) is listening on.
	 */
	public MixCascade(String a_name, String a_id, String a_hostName, int a_port) throws Exception
	{
		this(a_name, a_id,
			 new ListenerInterface(a_hostName, a_port, ListenerInterface.PROTOCOL_TYPE_RAW_TCP).toVector());
	}

	public MixCascade(String a_name, String a_id, Vector a_listenerInterfaces) throws Exception
	{
		this(a_name, a_id, null, a_listenerInterfaces);
	}

	public MixCascade(String a_name, String a_id, Vector a_mixIDs, Vector a_listenerInterfaces) throws Exception
	{
		this (a_name, a_id, a_mixIDs, a_listenerInterfaces, Long.MAX_VALUE);
	}

	public MixCascade(String a_name, String a_id, Vector a_mixIDs, Vector a_listenerInterfaces,
					  long a_timeout) throws Exception
	{
		super(a_timeout);
		ListenerInterface listener = (ListenerInterface) a_listenerInterfaces.elementAt(0);
		String strHostName = listener.getHost();
		String strPort = Integer.toString(listener.getPort());
		/* set a unique ID */
		if ( (a_id == null) || (a_id.length() == 0))
		{
			m_mixCascadeId = "(user)" + strHostName + "%3A" + strPort;
		}
		else
		{
			m_mixCascadeId = a_id;
		}
		/* set a name */
		if (a_name != null)
		{
			m_strName = a_name;
		}
		else
		{
			m_strName = strHostName + ":" + strPort;
		}
		m_listenerInterfaces = a_listenerInterfaces;
		/* set the lastUpdate time */
		m_lastUpdate = System.currentTimeMillis();
		/* create the mixIds and set one with the same ID as the mixcascade itself */
		m_mixNodes = new Vector();
		if (a_mixIDs == null || a_mixIDs.size() == 0)
		{
			m_mixIds = new Vector();
			m_mixIds.addElement(m_mixCascadeId);
		}
		else
		{
			m_mixIds = (Vector)a_mixIDs.clone();
		}
		m_mixInfos = new MixInfo[m_mixIds.size()];
		for (int i = 0; i < m_mixInfos.length; i++)
		{
			m_mixInfos[i] = null;
		}

		/* some more values */
		m_userDefined = true;
		m_bDefaultVerified = true;
		m_xmlStructure = generateXmlRepresentation();
		m_compressedXmlStructure = ZLibTools.compress(XMLSignature.toCanonical(m_xmlStructure));
		createMixIDString();
		calculateOperatorsAndCountries();
	}


	/**
	 * Returns whether a given cascade has another number of mixes or mixes with other IDs than this one.
	 * @param a_cascade MixCascade
	 * @return if both cascades contain the same mix IDs (and are therefore identical); false otherwise
	 */
	public boolean compareMixIDs(MixCascade a_cascade)
	{
		if (a_cascade == null)
		{
			return false;
		}

		return a_cascade.getMixIDsAsString().equals(getMixIDsAsString());
	}

	/**
	 * Returns the ID of the mixcascade.
	 *
	 * @return The ID of this mixcascade.
	 */
	public String getId()
	{
		return m_mixCascadeId;
	}

	/**
	 * Returns the cascade protocol version, but only if this cascade entry was received directly from a
	 * first mix.
	 * @return String
	 */
	public String getMixProtocolVersion()
	{
		return m_mixProtocolVersion;
	}

	public String getPaymentProtocolVersion()
	{
		return m_paymentProtocolVersion;
	}

	public String getPIID()
	{
		return m_piid;
	}

	public long getPrepaidInterval()
	{
		return m_prepaidInterval;
	}

	/**
	 * Returns if this MixCascade has been recevied directly from a cascade connection.
	 * @return if this MixCascade has been recevied directly from a cascade connection
	 */
	public boolean isFromCascade()
	{
		return m_bFromCascade;
	}

	/**
	 * Returns the time when this MixCascade entry was created by the origin mixcascade (or by the
	 * JAP client if it is a user-defined entry).
	 *
	 * @return A version number which is used to determine the more recent MixCascade entry, if two
	 *         entries are compared (higher version number -> more recent entry).
	 */
	public long getVersionNumber()
	{
		return m_serial;
	}

	public long getLastUpdate()
	{
		return m_lastUpdate;
	}

	/**
	 * Gets (and sets) the concatenated names of the Mixes in this Cascade.
	 * @todo can't getName() be used instead of that?
	 * @return
	 */
	public String getMixNames()
	{
		synchronized (m_mixInfos)
		{
			if (m_strMixNames == null)
			{
				m_strMixNames = "";
				for (int i = 0; i < m_mixInfos.length; i++)
				{
					if (m_mixInfos[i] == null)
					{						
						continue;
					}
					if (m_strMixNames.length() > 0)
					{
						m_strMixNames += "-";
					}
					m_strMixNames += m_mixInfos[i].getNameFragmentForCascade(); //m_mixInfos[i].getName();					
				}
				if (m_strMixNames.length() == 0)
				{
					m_strMixNames = m_strName;
				}
				else if (!m_strName.equals(m_strMixNames))
				{
					m_strMixNames = m_strName + "|" + m_strMixNames; 
				}
			}			
		}
		return m_strMixNames;
	}
	
	/* this function generates a cascadeName form the namefragments of the corresponding
	 * mixes. (but only namefragments of mixes with different operators will appear)
	 * this overwrites the existing cascadename
	 */
	private void generateNameFromMixNames()
	{
		//no null checks necessary because NO NullPointers must occur! 
		if (m_decomposedCascadeName == null)
		{
			m_decomposedCascadeName = new Vector();
		}
		else
		{
			m_decomposedCascadeName.removeAllElements();
		}
		
		Vector operators = new Vector();
		ServiceOperator currentOp = null;
		String currentNameFragment = null;
		m_strName = "";
		
		/* special case: If the operator of the first and the last mix are the same
		 * only this operator is displayed. 
		 */
		if(m_mixInfos[0].getServiceOperator().equals(m_mixInfos[m_mixInfos.length-1].getServiceOperator()))
		{
			//@todo: should we better use only the provider name in this case? 
			currentNameFragment = m_mixInfos[0].getNameFragmentForCascade();
			m_decomposedCascadeName.addElement(currentNameFragment);
			m_strName = currentNameFragment;
			return;
		}
		
		for (int i = 0; i < m_mixInfos.length; i++) 
		{
			currentOp = m_mixInfos[i].getServiceOperator();
			if(! operators.contains(m_mixInfos[i].getServiceOperator()))
			{
				currentNameFragment = m_mixInfos[i].getNameFragmentForCascade();
				m_strName += (i == (m_mixInfos.length-1)) ? currentNameFragment : (currentNameFragment+"-");	
				operators.addElement(currentOp);
				m_decomposedCascadeName.addElement(currentNameFragment);
			}
		}
	}
	
	/**
	 * Returns the name of the mixcascade.
	 *
	 * @return The name of this mixcascade.
	 */
	public String getName()
	{
		getDecomposedCascadeName(); /*@todo: remove */
		return m_strName;
	}

	/**
	 * Tells if this Cascade has a maximum number of users.
	 * @return > 0 if user number is restricted; 0 otherwise
	 */
	public int getMaxUsers()
	{
		return m_maxUsers;
	}
	
	/**
	 * Returns a String representation for this MixCascade object. It's just the name of the
	 * mixcascade.
	 *
	 * @return The name of this mixcascade.
	 */
	public String toString()
	{
		return getName();
	}

	/**
	 * @todo use generateNameFromMixNames when the operator short name is certified.
	 * and this method only to return the container containg the name fragments
	 * @return
	 */
	public Vector getDecomposedCascadeName()
	{
		if (m_strName == null)
		{
			return null;
		}
		synchronized (m_strName)
		{
			if (m_decomposedCascadeName == null)
			{			
				m_decomposedCascadeName = new Vector();
				
				if (isUserDefined() || m_mixInfos.length == 0)
				{
					m_decomposedCascadeName.addElement(m_strName);
					return m_decomposedCascadeName;
				}					
				
				StringTokenizer tokenizer = new StringTokenizer(m_strName,"-");
				StringTokenizer tempTokenizer;
				String token;
				
				if (tokenizer.countTokens() == getNumberOfMixes())
				{
					while (tokenizer.hasMoreTokens())
					{
						token = tokenizer.nextToken().trim();
						tempTokenizer = new StringTokenizer(token);
						if (!tempTokenizer.hasMoreTokens())
						{
//							cannot further decompose this name
							break;
						}
						token = tempTokenizer.nextToken().trim();						
						
						if (token.length() == 0)
						{
//							cannot further decompose this name
							break;
						}
						if (token.length() > 15)
						{
							token = token.substring(0, 15);
						}
						m_decomposedCascadeName.addElement(token);
					}
				}
				
				if (m_decomposedCascadeName.size() == 0)
				{
					// cannot decompose this name
					m_decomposedCascadeName.addElement(m_strName);
				}
				/* special case: If the operator of the first and the last mix are the same
				 * only this operator is displayed. 
				 */
				else if (m_mixInfos[0] == null || m_mixInfos[0].getServiceOperator() == null ||
						m_mixInfos[m_mixInfos.length-1] == null ||
						m_mixInfos[m_mixInfos.length-1].getServiceOperator() == null ||
						m_mixInfos[0].getServiceOperator().equals(m_mixInfos[m_mixInfos.length-1].getServiceOperator()))
				{
					m_strName = (String) m_decomposedCascadeName.elementAt(0);
					m_decomposedCascadeName.removeAllElements();
					m_decomposedCascadeName.addElement(m_strName);
				}
				else
				{
					Vector ops = new Vector();
					ServiceOperator currentOp = null;
					m_strName = "";
					
					for (int i = 0; (i < m_decomposedCascadeName.size() ) && (i < m_mixInfos.length); i++)
					{
						currentOp = m_mixInfos[i].getServiceOperator();
						if (currentOp == null)
						{
							m_strName = (String)m_decomposedCascadeName.elementAt(0);
							break;
						}
						if (!ops.contains(currentOp))
						{
							ops.addElement(currentOp);
							m_strName += m_strName.equals("") ? "" : "-";
							m_strName += m_decomposedCascadeName.elementAt(i);
						}
					}
				}
			}
		}
		return m_decomposedCascadeName;
	}
	
	/**
	 * Compares this object to another one. This method returns only true, if the other object is
	 * also a MixCascade and has the same ID as this MixCascade.
	 *
	 * @param a_object The object with which to compare.
	 *
	 * @return True, if the object with which to compare is also a MixCascade which has the same ID
	 *         as this instance. In any other case, false is returned.
	 */
	public boolean equals(Object a_object)
	{
		boolean objectEquals = false;
		if (a_object != null)
		{
			if (a_object instanceof MixCascade)
			{
				objectEquals = this.getId().equals( ( (MixCascade) a_object).getId());
			}
		}
		return objectEquals;
	}

	public boolean checkId()
	{
		return m_userDefined || super.checkId();
	}

	/**
	 * Returns a hashcode for this instance of MixCascade. The hashcode is calculated from the ID,
	 * so if two instances of MixCascade have the same ID, they will have the same hashcode.
	 *
	 * @return The hashcode for this MixCascade.
	 */
	public int hashCode()
	{
		return (getId().hashCode());
	}

	/**
	 * Returns the number of interfaces (IP, Port) the mixcascade (first mix) is listening on.
	 *
	 * @return The number of listener interfaces.
	 */
	public int getNumberOfListenerInterfaces()
	{
		if (m_listenerInterfaces != null)
		{
			return m_listenerInterfaces.size();
		}
		return 0;
	}

	/**
	 * Returns the ListenerInterface with the number i from the list of all listener interfaces
	 * (count starts with 0). If there is no ListenerInterface with this number, null is returned.
	 *
	 * @param i The number of the ListenerInterface.
	 *
	 * @return The ListenerInterface with the number i from the list of all listener interfaces of
	 * this MixCascade.
	 */
	public ListenerInterface getListenerInterface(int i)
	{
		ListenerInterface returnedListener = null;
		if (i >= 0)
		{
			if (i < getNumberOfListenerInterfaces())
			{
				returnedListener = (ListenerInterface) (m_listenerInterfaces.elementAt(i));
			}
		}
		return returnedListener;
	}
	
	public Vector getHosts()
	{
		Vector hosts = new Vector();
		
		for(int i = 0; i < getNumberOfListenerInterfaces(); i++)
		{
			String host = ((ListenerInterface) m_listenerInterfaces.elementAt(0)).getHost();
			
			if(!hosts.contains(host))
			{
				hosts.addElement(host);
			}
		}
		
		return hosts;
	}

	/**
	 * Returns the number of mixes in the cascade.
	 *
	 * @return the number of mixes in the cascade
	 */
	public int getNumberOfMixes()
	{
		return m_mixIds.size();
	}

	//Elmar: not sure what this does, seems like m_strMixIds is all the elements of the Vector of MixIds concatenated
	//if you need the subjectkeyidentifiers of the mixes of the cascade, use getMixIds()
	public String getMixIDsAsString()
	{
		return m_strMixIds;
	}

	/**
	 * Returns the MixInfo object (if available) of the mix in the specified position in the cascade.
	 * @param a_mixNumber a mix position from 0 to getNumberOfMixes() - 1
	 * @return the MixInfo object for the specified mix or null if it was not found in this cascade
	 */
	public MixInfo getMixInfo(int a_mixNumber)
	{
		if (a_mixNumber < 0 || a_mixNumber >= getNumberOfMixes())
		{
			return null;
		}
		return m_mixInfos[a_mixNumber];
	}

	/**
	 * Returns the MixInfo object (if available) of the mix with the specified id if this mix is part of
	 * this cascade.
	 * @param a_mixId a Mix id
	 * @return the MixInfo object for the specified mix or null if it was not found
	 */
	public MixInfo getMixInfo(String a_mixId)
	{
		if (a_mixId == null)
		{
			return null;
		}

		for (int i = 0; i < m_mixIds.size(); i++)
		{
			if (m_mixIds.elementAt(i).equals(a_mixId))
			{
				return m_mixInfos[i];
			}
		}

		return null;
	}

	/**
	 * Returns the Mix ID of the mix with the specified position in the cascade.
	 * @param a_mixNumber  a mix position from 0 to getNumberOfMixes() - 1
	 * @return the Mix ID of the mix with the specified position in the cascade or null if the specified
	 * mix is not present in this cascade
	 */
	public String getMixId(int a_mixNumber)
	{
		if (a_mixNumber < 0 || a_mixNumber >= getNumberOfMixes())
		{
			return null;
		}
		return m_mixIds.elementAt(a_mixNumber).toString();
	}

	/**
	 * Returns the IDs of all mixes in the cascade.
	 *
	 * @return A snapshot of the list with all mix IDs within the cascade.
	 */
	public Vector getMixIds()
	{
		//Elmar: why do we bother to copy everything into a new Vector instead of just returning m_mixIds ?
		return (Vector)m_mixIds.clone();
	}

	/**
	 * Returns whether this MixCascade entry was generated by a user within the JAP client (true) or
	 * was generated by the original mixcascade itself (false).
	 *
	 * @return Whether this MixCascade entry is user-defined.
	 */
	public boolean isUserDefined()
	{
		return m_userDefined;
	}

	public boolean isSocks5Supported()
	{
		return m_bSock5Support;
	}

	/**
	 * May be set by this application to show this service as trusted. TrustModel.isTrusted will then
	 * return true, but TrustModel.checkTrust will still check the correct trust.
	 * @param a_bImplicitTrust if this service is shown as trusted in this application
	 */
	public void showAsTrusted(boolean a_bImplicitTrust)
	{
		m_bImplicitTrust = a_bImplicitTrust;
	}

	public boolean isShownAsTrusted()
	{
		return m_bImplicitTrust;
	}

	public void setUserDefined(boolean a_bUserDefined, MixCascade a_oldMixCascade) throws XMLParseException
	{
		m_userDefined = a_bUserDefined;
		if (m_userDefined && a_oldMixCascade != null && a_oldMixCascade.getId().equals(getId()))
		{
			m_strName = a_oldMixCascade.m_strName;
			m_decomposedCascadeName = new Vector();
			m_decomposedCascadeName.addElement(m_strName);
			
			m_listenerInterfaces = a_oldMixCascade.m_listenerInterfaces;
			/* set the lastUpdate time */
			m_lastUpdate = System.currentTimeMillis();
		}
		m_xmlStructure = generateXmlRepresentation();
		m_compressedXmlStructure = ZLibTools.compress(XMLSignature.toCanonical(m_xmlStructure));
		calculateOperatorsAndCountries();
	}

	public StatusInfo fetchCurrentStatus()
	{
		return fetchCurrentStatus(-1);
	}

	/**
	 * Fetches the current status of the mixcascade from the InfoService. The StatusInfo is
	 * available by calling getCurrentStatus().
	 */
	public StatusInfo fetchCurrentStatus(long a_timeout)
	{
		synchronized (this)
		{
			String id = getMixId(0);
			if (id == null)
			{
				// the cascade id should be the same as the the id of the first mix, but ok...
				id = getId();
			}
			StatusInfo statusInfo;
			if (a_timeout <= 0)
			{
				statusInfo = InfoServiceHolder.getInstance().getStatusInfo(this);
			}
			else
			{
				statusInfo = InfoServiceHolder.getInstance().getStatusInfo(this, a_timeout);
			}

			return statusInfo;
		}
	}

	/**
	 * Returns the current status of this mixcascade. If there is no status available at the
	 * infoservice, a dummy StatusInfo (every value = -1) is returned. The current status is every
	 * time updated, when fetchCurrentStatus() is called.
	 *
	 * @return The current status of the mixcascade.
	 */
	public StatusInfo getCurrentStatus()
	{
		StatusInfo statusInfo = (StatusInfo)Database.getInstance(StatusInfo.class).getEntryById(getId());
		if (statusInfo == null)
		{
			statusInfo = StatusInfo.createDummyStatusInfo(getId());
		}
		return statusInfo;
	}

	/**
	 * This returns the filename (InfoService command), where this MixCascade entry is posted at
	 * other InfoServices. It's always '/cascade'.
	 *
	 * @return The filename where the information about this MixCascade entry is posted at other
	 *         InfoServices when this entry is forwarded.
	 */
	public String getPostFile()
	{
		return "/cascade";
	}

	public int getPostEncoding()
	{
		return HTTPConnectionFactory.HTTP_ENCODING_ZLIB;
	}

	/**
	 * This returns the data posted when this MixCascade information is forwarded to other
	 * infoservices. It's the XML structure of this MixCascade as we received it.
	 *
	 * @return The data posted to other infoservices when this entry is forwarded.
	 */
	public byte[] getPostData()
	{
		return m_compressedXmlStructure;
	}

	public byte[] getCompressedData()
	{
		return m_compressedXmlStructure;
	}

	/**
	 * Returns the XML structure for this MixCascade entry.
	 *
	 * @return The XML node for this MixCascade (MixCascade node).
	 */
	public Element getXmlStructure()
	{
		return m_xmlStructure;
	}
	
	public Hashtable getPriceCertificateHashes()
	{
		return (Hashtable)m_priceCertificateHashes.clone();
	}

	/**
	 * getPriceCertificates: utility method,
	 * equivalent to getting the MixInfo for all Mixes, and calling getPriceCertificate on each one
	 *
	 * @return Vector containing one XMLPriceCertificate per Mix of the cascade
	 */
	public Vector getPriceCertificates()
	{
		return (Vector)m_priceCertificates.clone();
	}

	public int getNrOfPriceCerts()
	{
		return m_nrPriceCerts;
	}
	
	public XMLSignature getSignature()
	{
		return m_signature;
	}

	public MultiCertPath getCertPath()
	{
		return m_certPath;
	}

	public boolean isVerified()
	{
		if (m_signature != null)
		{
			return m_signature.isVerified();
		}
		return m_bDefaultVerified;
	}

	public boolean isValid()
	{
		if (m_certPath != null)
		{
			return m_certPath.isValid(new Date());
		}
		return false;
	}
	
	public boolean isDataRetentionActive()
	{
		return m_bDataRetention;
	}
	
	public boolean isActiveStudy()
	{
		return m_bStudy||m_userDefined;
	}

	/**
	 * Returns the number of really independent operators in this cascade.
	 * @return the number of really independent operators in this cascade
	 */
	public int getNumberOfOperators()
	{
		calculateOperatorsAndCountries();
		return m_nrOperators;
	}
	
	/**
	 * Returns the same as getNumberOfOperators(), is all operator and mix certificates
	 * are valid. If one or more of them are expired, this method will ignore it and
	 * count them as "full" operators. It should therefore not be used for security
	 * purposes (filters etc.) but for presentation only.
	 * @return
	 */
	public int getNumberOfOperatorsShown()
	{
		calculateOperatorsAndCountries();
		return m_nrOperatorsShown;
	}

	/**
	 * Returns the number of Mixes in the cascade that differ from others in the cascade in their
	 * location and in the location of their operator.
	 * @return int
	 */
	public int getNumberOfCountries()
	{
		calculateOperatorsAndCountries();
		return m_nrCountries;
	}
	
	/**
	 * Returns a number that tells how distributed this service is. The higher, the better.
	 * Numbers range from 0 to 6, where 0 is an unknown state.
	 * @return a number that tells how distributed this service is
	 */
	public int getDistribution()
	{
		calculateOperatorsAndCountries();
		return m_distributionPoints;
	}
	
	private void calculateOperatorsAndCountries()
	{
		synchronized (SYNC_OPERATORS_AND_COUNTRIES)
		{
			boolean bNeedsRecalculation;
			// check the certificates of the Mixes
			Hashtable operators = new Hashtable();
			//Hashtable operatorCountries = new Hashtable();
			Hashtable countries = new Hashtable();
			//Hashtable mixCountries = new Hashtable();
			Hashtable mixIDs = new Hashtable();
			X509DistinguishedName currentName;
			String mixCountryCode, operatorCountryCode;
			boolean bStatus;
			
			if (bNeedsRecalculation = (m_mixCertVerifiedAndValid == null))
			{
				m_mixCertVerifiedAndValid = new boolean[getNumberOfMixes()];
				for (int i = 0; i < m_mixCertVerifiedAndValid.length; i++)
				{
					m_mixCertVerifiedAndValid[i] = false;
				}
			}
			
			for (int i = 0; i < getNumberOfMixes(); i++)
			{
				bStatus = getMixInfo(i) != null && getMixInfo(i).getCertPath() != null && 
					getMixInfo(i).getCertPath().isVerified() && 
					getMixInfo(i).getCertPath().isValid(new Date());
				if (m_mixCertVerifiedAndValid[i] != bStatus)
				{
					// Trust has changed since last calculation, recalculation is needed!
					bNeedsRecalculation = true;
				}
				m_mixCertVerifiedAndValid[i] = bStatus;
			}
			
			if (!bNeedsRecalculation)
			{
				return;
			}
		
			m_nrOperatorsCountForDistribution = 0;
			m_nrOperators = 0;
			m_nrOperatorsShown = 0;
			m_nrCountries = 0;
			for (int i = 0; i < getNumberOfMixes(); i++)
			{
				if (getMixInfo(i) == null || getMixInfo(i).getCertPath() == null)
				{
					continue;
				}
				currentName = getMixInfo(i).getCertPath().getIssuer();
				if (currentName != null && currentName.getOrganisation() != null &&
					!operators.contains(currentName.getOrganisation())					
					&& !mixIDs.contains(getMixInfo(i).getId()))
				{
					// this Mix seems to be operated by an organization independent from others in the cascade
		
					// country bonus
					operatorCountryCode = currentName.getCountryCode();
					mixCountryCode = getMixInfo(i).getCertPath().getSubject().getCountryCode();
					if (operatorCountryCode != null && mixCountryCode != null &&
						//!operatorCountries.containsKey(operatorCountryCode) &&
						//!mixCountries.containsKey(mixCountryCode))
						!countries.contains(mixCountryCode) &&
						!countries.contains(operatorCountryCode))
					{
						// operator and Mix are located in different countries than the others in the cascade
						if (m_mixCertVerifiedAndValid[i])
						{
							if (i <= 1 || (i + 1 == getNumberOfMixes()))
							{
								// do not count more than one middle Mix to get a maximum of 6 distribution points
								m_nrCountries++;
							}
						}
					}
					
					if (m_mixCertVerifiedAndValid[i])
					{
						if (operatorCountryCode != null)
						{
							//operatorCountries.put(operatorCountryCode, operatorCountryCode);
							countries.put(operatorCountryCode, operatorCountryCode);
						}
						if (mixCountryCode != null)
						{
							//mixCountries.put(mixCountryCode, mixCountryCode);
							countries.put(mixCountryCode, mixCountryCode);
						}
					}
		
					// operator bonus
					operators.put(currentName.getOrganisation(), currentName.getOrganisation());
					mixIDs.put(getMixInfo(i).getId(), getMixInfo(i).getId());
					if (m_mixCertVerifiedAndValid[i])
					{
						m_nrOperators++;
						if (i <= 1 || (i + 1 == getNumberOfMixes()))
						{
							// do not count more than one middle Mix to get a maximum of 6 distribution points
							m_nrOperatorsCountForDistribution++;
						}
					}
					m_nrOperatorsShown++;
				}
				else
				{
					// This Cascade has at least two operators which are the same. Assume only one operator at maximum!
					if (m_nrOperators <= 0)
					{
						for (int j = 0; j < getNumberOfMixes(); j++)
						{
							if (m_mixCertVerifiedAndValid[j])
							{
								m_nrOperators = 1;
								m_nrOperatorsCountForDistribution = 1;
								break;
							}
						}
					}
					else
					{
						m_nrOperators = 1;
						m_nrOperatorsCountForDistribution = 1;
					}
					m_nrOperatorsShown = 1;
					m_nrCountries = Math.min(m_nrOperatorsCountForDistribution, 1);
					break;
				}
			}
			// calculate distribution points
			if (m_nrOperatorsCountForDistribution == 2 && m_nrCountries == 2)
			{
				m_distributionPoints = 3;
			}
			else if (m_nrOperatorsCountForDistribution == 2 && m_nrCountries == 1)
			{
				m_distributionPoints = 2;
			}
			else if (m_nrOperatorsCountForDistribution == 1)
			{
				m_distributionPoints = 1;
			}
			else
			{
				m_distributionPoints = m_nrOperatorsCountForDistribution + m_nrCountries;
			}
			
			// Test is trust has changed meanwhile and recalculate distribution if it has.
			calculateOperatorsAndCountries();
		}
	}

	/**
	 * Creates an XML node without signature for this MixCascade.
	 *
	 * @return The MixCascade XML node.
	 */
	private Element generateXmlRepresentation()
	{
		Document doc = XMLUtil.createDocument();
		Element mixCascadeNode = doc.createElement(XML_ELEMENT_NAME);
		XMLUtil.setAttribute(mixCascadeNode, "id", getId());
		/* Create the child nodes of MixCascade (Name, Network, Mixes, LastUpdate) */
		if (m_isPayment)
		{
			Element elemPay = doc.createElement("Payment");
			XMLUtil.setAttribute(elemPay, "required", m_isPayment);
			XMLUtil.setAttribute(elemPay, XML_ATTR_VERSION, m_paymentProtocolVersion);
			mixCascadeNode.appendChild(elemPay);
		}

		Element nameNode = doc.createElement("Name");
		XMLUtil.setValue(nameNode, getName());
		Element networkNode = doc.createElement("Network");
		Element listenerInterfacesNode = doc.createElement("ListenerInterfaces");
		for (int i = 0; i < getNumberOfListenerInterfaces(); i++)
		{
			ListenerInterface currentListenerInterface = getListenerInterface(i);
			Element currentListenerInterfaceNode = currentListenerInterface.toXmlElement(doc);
			listenerInterfacesNode.appendChild(currentListenerInterfaceNode);
		}
		networkNode.appendChild(listenerInterfacesNode);
		Element mixesNode = doc.createElement("Mixes");
		XMLUtil.setAttribute(mixesNode, "count", getNumberOfMixes());
		Enumeration allMixIds = m_mixIds.elements();
		for (int i = 0; allMixIds.hasMoreElements(); i++)
		{
			if (m_mixNodes.size() > i)
			{
				allMixIds.nextElement(); // skip
				try
				{
					mixesNode.appendChild(XMLUtil.importNode(doc, (Node) m_mixNodes.elementAt(i), true));
				}
				catch (XMLParseException a_e)
				{
					LogHolder.log(LogLevel.EXCEPTION, LogType.MISC, "Could not import node " +
								  ( (Node) m_mixNodes.elementAt(i)).getNodeName() + "!");
				}
			}
			else
			{
				Element mixNode = doc.createElement("Mix");
				XMLUtil.setAttribute(mixNode, "id", (String) (allMixIds.nextElement()));
				mixesNode.appendChild(mixNode);
			}

		}
		Element lastUpdateNode = doc.createElement("LastUpdate");
		XMLUtil.setValue(lastUpdateNode, getLastUpdate());
		mixCascadeNode.appendChild(nameNode);
		mixCascadeNode.appendChild(networkNode);
		mixCascadeNode.appendChild(mixesNode);
		mixCascadeNode.appendChild(lastUpdateNode);
		if (isUserDefined())
		{
			XMLUtil.setAttribute(mixCascadeNode, XML_ATTR_USER_DEFINED, true);
			if (m_signature != null)
			{
				Element[] signatureElements = m_signature.getXMLElements(doc);
								
				for(int i=0; i < signatureElements.length; i++)
				{
					mixCascadeNode.appendChild(signatureElements[i]);
				}
				
			}
		}

		return mixCascadeNode;
	}

	public boolean isPaymentProtocolSupported()
	{
		return !m_isPayment ||
			(m_isPayment && getPaymentProtocolVersion().equals(SUPPORTED_PAYMENT_PROTOCOL_VERSION));
	}

	public boolean isPayment()
	{
		return m_isPayment;
	}

	private void createMixIDString()
	{
		m_strMixIds = "";
		for (int i = 0; i < m_mixIds.size(); i++)
		{
			m_strMixIds += m_mixIds.elementAt(i);
		}
	}

	public String getContext() 
	{
		return m_context;
	}
	
	public Element getWebInfo(Document webInfoDoc)
	{
		if (webInfoDoc == null)
		{
			return null;
		}
		
		Vector decomposedNames = getDecomposedCascadeName();
		Vector namesToAdd = new Vector(decomposedNames.size());
		String tempName = null;
		for (int i = 0; i < decomposedNames.size(); i++) 
		{
			tempName = (String) decomposedNames.elementAt(i);
			if(tempName == null || namesToAdd.contains(tempName))
			{
				tempName = "";
			}
			namesToAdd.insertElementAt(tempName, i);
		}
		
		Element rootElement = webInfoDoc.createElement(XML_ELEMENT_WEBINFO);
		XMLUtil.setAttribute(rootElement, XML_ATTR_PAYMENT, isPayment());
		XMLUtil.setAttribute(rootElement, XML_ATTR_ID, getId());
		if (getMaxUsers() > 0)
		{
			XMLUtil.setAttribute(rootElement, XML_ATTR_MAX_USERS, getMaxUsers());
		}
		if (getContext() != null)
		{
			XMLUtil.setAttribute(rootElement, XML_ATTR_CONTEXT, getContext());
		}
		Element cascadeName = XMLUtil.createChildElement(rootElement, XML_ELEMENT_WEBINFO_CASCADE_NAME);
		Element mixList = webInfoDoc.createElement(MixInfo.XML_ELEMENT_CONTAINER_NAME);
		
		XMLUtil.createChildElementWithValue(rootElement, 
				XML_ELEMENT_WEBINFO_CURR_USERS, 
				(""+getCurrentStatus().getNrOfActiveUsers()));
		
		PerformanceEntry perfEntry = 
			(PerformanceEntry) Database.getInstance(PerformanceEntry.class).getEntryById(getId());
		
		if(perfEntry != null)
		{
			rootElement.appendChild(perfEntry.toXmlElement(webInfoDoc));
		}
		
		Element currentNameElement = null;
		String currentDecomposedNameComponent = null;
		MixInfo currentMixInfo = null;
		ServiceOperator currentMixOperator = null;
		ServiceLocation currentMixLocation = null;
		Element currentMixElement = null;
		Element currentMixOperatorElement = null;
		Element currentMixLocationElement = null;
		String currentMixName = null;
		rootElement.appendChild(mixList);
		for (int i = 0; i < getNumberOfMixes(); i++) 
		{
			currentMixInfo = getMixInfo(i);
			if(currentMixInfo != null)
			{
				if(currentMixInfo.getCertPath() == null)
				{
					/* no valid document can be returned */
					return null;
				}
				CertPath path = currentMixInfo.getCertPath().getPath();
				currentMixOperator = new ServiceOperator(null, currentMixInfo.getCertPath(), 0l);
				currentMixLocation = new ServiceLocation(null, path.getFirstCertificate());
				currentMixName = currentMixInfo.getName();
				
				if(currentMixOperator == null || 
				   currentMixLocation == null)
				{
					/* no valid document can be returned */
					return null;
				}
				
				/* set the cascade name components */
				currentDecomposedNameComponent = null;
				if( i < namesToAdd.size() )
				{
					currentDecomposedNameComponent = (String) namesToAdd.elementAt(i);
					if(currentDecomposedNameComponent != null && 
						currentDecomposedNameComponent.equals(""))
					{
						currentDecomposedNameComponent = null;
					}
				}
				
				if(currentDecomposedNameComponent != null)
				{
					currentNameElement = 
						XMLUtil.createChildElementWithValue(cascadeName, 
								XML_ELEMENT_WEBINFO_NAME, 
								currentDecomposedNameComponent);
				
					if (currentMixLocation.getCountryCode() != null )
					{
						XMLUtil.setAttribute(currentNameElement, XML_ATTR_WEBINFO_MIX_COUNTRY, currentMixLocation.getCountryCode());
					}
					
					if (currentMixOperator.getCountryCode() != null )
					{
						XMLUtil.setAttribute(currentNameElement, XML_ATTR_WEBINFO_OP_COUNTRY, currentMixOperator.getCountryCode());
					}
					XMLUtil.setAttribute(currentNameElement, XML_ATTR_WEBINFO_MIX_POSITION, i);
				}
				
				/* now set the current Mix Attributes */ 
				currentMixElement = XMLUtil.createChildElement(mixList, MixInfo.XML_ELEMENT_NAME);
				XMLUtil.setAttribute(currentMixElement, XML_ATTR_ID, currentMixInfo.getId());
				
				if(currentMixName != null)
				{
					XMLUtil.createChildElementWithValue(currentMixElement, XML_ELEMENT_WEBINFO_NAME,
							currentMixName);
				}
				
				currentMixOperatorElement = currentMixOperator.toXMLElement(webInfoDoc);
				currentMixLocationElement = currentMixLocation.toXMLElement(webInfoDoc);
				
				if(currentMixOperatorElement != null)
				{
					currentMixElement.appendChild(currentMixOperatorElement);
				}
				
				if(currentMixLocationElement != null)
				{
					currentMixElement.appendChild(currentMixLocationElement);
				}
			}
		}	
		
		currentNameElement = 
			XMLUtil.createChildElementWithValue(cascadeName, 
					XML_ELEMENT_WEBINFO_COMPOSED_NAME, getName());
		
		Element listenerInterfaces = 
			XMLUtil.createChildElement(rootElement, ListenerInterface.XML_ELEMENT_CONTAINER_NAME);
		ListenerInterface listenerInterface;
		Element elemInterface;
		Hashtable hashInterfaceHosts = new Hashtable();
		for (int i = 0; i < getNumberOfListenerInterfaces(); i++)
		{
			listenerInterface = getListenerInterface(i);
			if (listenerInterface.isHidden())
			{
				continue;
			}
			if (hashInterfaceHosts.containsKey(listenerInterface.getHost()))
			{
				elemInterface = (Element)hashInterfaceHosts.get(listenerInterface.getHost());
			}
			else
			{
				elemInterface = XMLUtil.createChildElement(listenerInterfaces, 
						ListenerInterface.XML_ELEMENT_NAME);
				XMLUtil.setAttribute(elemInterface, "Host", listenerInterface.getHost());
				hashInterfaceHosts.put(listenerInterface.getHost(), elemInterface);
			}
			if (listenerInterface.getProtocol() != ListenerInterface.PROTOCOL_TYPE_RAW_UNIX)
			{
				XMLUtil.createChildElementWithValue(elemInterface, "Port", "" + listenerInterface.getPort());
			}
		}
		
		return rootElement;
	}
}
