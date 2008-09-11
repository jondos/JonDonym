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

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.security.SignatureException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import anon.util.IXMLEncodable;
import anon.util.XMLUtil;
import anon.crypto.SignatureVerifier;
import anon.crypto.XMLSignature;
import anon.crypto.CertPath;
import anon.crypto.IVerifyable;
import anon.crypto.JAPCertificate;
import anon.crypto.X509SubjectKeyIdentifier;
import anon.util.XMLParseException;

/**
 * Holds the information of a mixcascade status.
 */
public final class StatusInfo extends AbstractDatabaseEntry implements IDistributable, IVerifyable, IXMLEncodable
{
	public static final String XML_ELEMENT_NAME = "MixCascadeStatus";
	public static final String XML_ELEMENT_CONTAINER_NAME = "MixCascadeStatusList";

	public static final int ANON_LEVEL_MIN = 0;
	public static final int ANON_LEVEL_LOW = 2;
	public static final int ANON_LEVEL_FAIR = 3;
	public static final int ANON_LEVEL_HIGH = 8;
	public static final int ANON_LEVEL_MAX = 10;

	/**
	 * This is the ID of the mixcascade to which this status belongs.
	 */
	private String m_mixCascadeId;

	/**
	 * Time (see System.currentTimeMillis()) when the mixcascade (first mix) has sent this status
	 * message.
	 */
	private long m_lastUpdate;

	/**
	 * Stores the number of active users in the corresponding mixcascade.
	 */
	private int m_nrOfActiveUsers;

	/**
	 * Stores the current risk for using this mix cascade. This is a value between 0 an 100 and it
	 * is calculated by the mixcascade in contrast to the anonlevel, which is calculated by the JAP
	 * client.
	 */
	private int m_currentRisk;

	/**
	 * Stores the current traffic situation for the mixcascade.
	 */
	private int m_trafficSituation;

	/**
	 * This is the number of packets, which are mixed through the cascade since their startup.
	 */
	private long m_mixedPackets;

	/**
	 * This is the calculated anonymity level (from number of active users, current traffic
	 * and cascade length). It is a value between 0 and 5.
	 */
	private int m_anonLevel;

	/**
	 * Stores the XML description which we forward to other infoservices (the same as we have
	 * received). This XML description is also used by recent versions of the JAP client
	 * (>= 00.02.016) when fetching the status info. We are using a string representation here
	 * because it is much faster if we don't need to process the XML tree everytime a client
	 * requests the current status.
	 */
	private String m_statusXmlData;
	
	/**
	 * Stores the XML description as byte-array for fast processing
	 */
	private byte[] m_statusXmlDataBytes;

	/**
	 * The signature of the StatusInfo
	 */
	private XMLSignature m_signature;
	
	/**
	 * The certificate path
	 */
	private CertPath m_certPath;
	
	/**
	 * The associated certificate
	 */
	private JAPCertificate m_certificate;

	/**
	 * Returns a new StatusInfo with dummy values (everything is set to -1). The LastUpdate time is
	 * set to the current system time. This function is used every time, we can't get the StatusInfo
	 * from the infoservice of when a new MixCascade is constructed. This method is only used within
	 * the context of the JAP client.
	 *
	 * @param a_mixCascadeId The ID of the MixCascade the StatusInfo belongs to.
	 *
	 * @return The new dummy StatusInfo.
	 */
	public static StatusInfo createDummyStatusInfo(String a_mixCascadeId)
	{
		return (new StatusInfo(a_mixCascadeId, -1, -1, -1, -1, -1));
	}

	/**
	 * Returns the name of the XML element corresponding to this class ("MixCascadeStatus").
	 *
	 * @return The name of the XML element corresponding to this class.
	 */
	public static String getXmlElementName()
	{
		return XML_ELEMENT_NAME;
	}

	/**
	 * Creates a new StatusInfo from XML description (MixCascadeStatus node). There is no anonymity
	 * level calculated for the new status entry -> getAnonLevel() will return -1. This constructor
	 * should only be called within the context of the infoservice.
	 *
	 * @param a_statusNode The MixCascadeStatus node from an XML document.
	 */
	public StatusInfo(Element a_statusNode) throws Exception
	{
		this(a_statusNode, null);
	}
	
	public StatusInfo(Element a_statusNode, MixCascade a_cascade) throws Exception
	{
		this(a_statusNode, a_cascade, -1);
	}

	/**
	 * Creates a new StatusInfo from XML description (MixCascadeStatus node).
	 *
	 * @param a_statusNode The MixCascadeStatus node from an XML document.
	 * @param a_mixCascadeLength The number of mixes in the mixcascade. We need this for
	 *                           calculating the anonymity level. If this value is smaller than 0,
	 *                           no anonymity level is calculated and getAnonLevel() will return
	 *                           -1.
	 * @param a_timeout	A timeout.                     
	 */
	public StatusInfo(Element a_statusNode, MixCascade a_cascade, long a_timeout) throws Exception
	{
		/* use always the timeout for the infoservice context, because the JAP client currently does
		 * not have a database of status entries -> no timeout for the JAP client necessary
		 */
		super(System.currentTimeMillis() + (a_timeout <= 0 ? Constants.TIMEOUT_STATUS : a_timeout));

		if (a_statusNode == null)
		{
			throw new XMLParseException(XMLParseException.NODE_NULL_TAG);
		}
		if (!a_statusNode.getNodeName().equals(XML_ELEMENT_NAME))
		{
			throw new XMLParseException(XMLParseException.ROOT_TAG);
		}
		
		/* get all the attributes of MixCascadeStatus */
		m_mixCascadeId = a_statusNode.getAttribute("id");
	
		/* Temporarily add the MixCascade certificate to the certificate store when loading from the XML file */		
		int certificateLock = -1;
		if(a_cascade == null)
		{
			a_cascade = (MixCascade) Database.getInstance(MixCascade.class).getEntryById(m_mixCascadeId);
			
			if (a_cascade != null && a_cascade.getCertPath() != null && a_cascade.getCertPath().getFirstCertificate() != null && a_cascade.getCertPath().verify())
			{
				/* add the cascade certificate to the certificate store */
				certificateLock = SignatureVerifier.getInstance().getVerificationCertificateStore().
					addCertificateWithoutVerification(a_cascade.getCertPath(),
					JAPCertificate.CERTIFICATE_TYPE_MIX, false, false);
			}
		}		

		// verify the signature
		try
		{
			m_signature = SignatureVerifier.getInstance().getVerifiedXml(a_statusNode,
				SignatureVerifier.DOCUMENT_CLASS_MIX);
			if (m_signature != null)
			{
				m_certPath = m_signature.getCertPath();
				if (m_certPath != null)
				{
					m_certificate = m_certPath.getFirstCertificate();
				}
			}
		}
		catch (Exception e)
		{
		}

		//The following is a workaround because if signature check is disabled, then also
		//the certificate sent with the POST HELO message are not stored....
		//we should change this....
		if(SignatureVerifier.getInstance().isCheckSignatures()&&
			SignatureVerifier.getInstance().isCheckSignatures(SignatureVerifier.DOCUMENT_CLASS_MIX))
			{
				if (m_certificate == null)
				{
					throw new SignatureException(
						 "There is no known certificate to verify the StatusInfo signature of Mix with ID: " +
						 m_mixCascadeId);
				}
				if (!checkId())
				{
					throw new XMLParseException(XMLParseException.ROOT_TAG, "Malformed Status-Entry for Mix ID: " + m_mixCascadeId);
				}
			}
		
		/* remove the lock on the certificate (if there is any) */
		if (certificateLock != -1)
		{
			SignatureVerifier.getInstance().getVerificationCertificateStore().removeCertificateLock(
				certificateLock);
		}
		
		/* get the values */
		m_currentRisk = Integer.parseInt(a_statusNode.getAttribute("currentRisk"));
		m_mixedPackets = Long.parseLong(a_statusNode.getAttribute("mixedPackets"));
		m_nrOfActiveUsers = Integer.parseInt(a_statusNode.getAttribute("nrOfActiveUsers"));
		m_trafficSituation = Integer.parseInt(a_statusNode.getAttribute("trafficSituation"));
		m_lastUpdate = Long.parseLong(a_statusNode.getAttribute("LastUpdate"));
		
		/* calculate the anonymity level */
		m_anonLevel = -1;
		if (a_cascade != null && a_cascade.getNumberOfMixes() > 0 &&
			getNrOfActiveUsers() >= 0 && getTrafficSituation() >= 0)
		{
			double userFactor = Math.min( ( (double) getNrOfActiveUsers()) / 500.0, 1.0);
			//double trafficFactor = Math.min( ( (double) getTrafficSituation()) / 100.0, 1.0);

			double countryFactor = Math.max(Math.min((double)a_cascade.getNumberOfCountries(), 3.0), 1.0);
			double operatorFactor = Math.max(Math.min((double)a_cascade.getNumberOfOperators(), 3.0), 1.0);



			/* get the integer part of the product -> 0 <= anonLevel <= 10 */
			m_anonLevel = (int) (userFactor * (countryFactor + operatorFactor) +
								  // "good" cascades always get a 40% minimum
								(countryFactor - 1.0 + operatorFactor - 1.0));

			// do not supersede the maximum or minimum anonymity level
			m_anonLevel = Math.min(m_anonLevel, ANON_LEVEL_MAX);
			m_anonLevel = Math.max(m_anonLevel, ANON_LEVEL_MIN);

		}
		m_statusXmlData = XMLUtil.toString(a_statusNode);
		m_statusXmlDataBytes=m_statusXmlData.getBytes();
	}

	/**
	 * Constructs a StatusInfo out of the single values. The creation time (last update) is set to
	 * the current system time.
	 *
	 * @param a_mixCascadeId The ID of the mixcascade this StatusInfo belongs to.
	 * @param a_nrOfActiveUsers The number of active users in the cascade.
	 * @param a_currentRisk The risk calculated by the cascade (between 0 and 100).
	 * @param a_trafficSituation The amount of traffic in the cascade.
	 * @param a_mixedPackets The number of packets the cascade has mixed since startup.
	 * @param a_anonLevel The anonymity level calculated by the JAP client (between 0 and 5).
	 */
	private StatusInfo(String a_mixCascadeId, int a_nrOfActiveUsers, int a_currentRisk,
					   int a_trafficSituation, long a_mixedPackets, int a_anonLevel)
	{
		/* use always the timeout for the infoservice context, because the JAP client currently does
		 * not have a database of status entries -> no timeout for the JAP client necessary
		 */
		super(System.currentTimeMillis() + Constants.TIMEOUT_STATUS);
		m_mixCascadeId = a_mixCascadeId;
		m_lastUpdate = System.currentTimeMillis();
		m_nrOfActiveUsers = a_nrOfActiveUsers;
		m_currentRisk = a_currentRisk;
		m_trafficSituation = a_trafficSituation;
		m_mixedPackets = a_mixedPackets;
		m_anonLevel = a_anonLevel;
		m_statusXmlData = XMLUtil.toString(generateXmlRepresentation());
		m_statusXmlDataBytes=m_statusXmlData.getBytes();
	}

	/**
	 * Returns the mixcascade ID of this status.
	 *
	 * @return The mixcascade ID of this status.
	 */
	public String getId()
	{
		return m_mixCascadeId;
	}

	/**
	 * Returns the time (see System.currentTimeMillis()), when the mixcascade has sent this
	 * StatusInfo to an InfoService.
	 *
	 * @return The send time of this StatusInfo from the mixcascade.
	 *
	 */
	public long getLastUpdate()
	{
		return m_lastUpdate;
	}

	/**
	 * Returns the time when this StatusInfo was created by the origin mixcascade (or by the JAP
	 * client if it is a dummy entry).
	 *
	 * @return A version number which is used to determine the more recent status entry, if two
	 *         entries are compared (higher version number -> more recent entry).
	 */
	public long getVersionNumber()
	{
		return getLastUpdate();
	}

	/**
	 * Returns the number of active users in the corresponding mixcascade.
	 *
	 * @return The number of active users in the corresponding mixcascade.
	 */
	public int getNrOfActiveUsers()
	{
		return m_nrOfActiveUsers;
	}

	/**
	 * Returns the current risk for using this mix cascade. This is a value between 0 an 100 and it
	 * is calculated by the mixcascade in contrast to the anonlevel, which is calculated by the JAP
	 * client.
	 *
	 * @return The current risk for the mixcascade.
	 */
	public int getCurrentRisk()
	{
		return m_currentRisk;
	}

	/**
	 * Returns the current traffic situation for the mixcascade.
	 *
	 * @return The current traffic situation for the mixcascade.
	 */
	public int getTrafficSituation()
	{
		return m_trafficSituation;
	}

	/**
	 * Returns the number of packets, which are mixed through the cascade since their startup.
	 *
	 * @return The number of mixed packets.
	 */
	public long getMixedPackets()
	{
		return m_mixedPackets;
	}

	/**
	 * Returns the calculated anonymity level (from number of active users, current traffic
	 * and cascade length). It is a value between ANON_LEVEL_MIN and ANON_LEVEL_MAX. If it is < 0,
	 * the anony level is unknown.
	 *
	 * @return The current anonymity level.
	 */
	public int getAnonLevel()
	{
		return m_anonLevel;
	}

	public boolean isVerified()
	{
		if (m_signature != null)
		{
			return m_signature.isVerified();
		}
		return false;
	}

	public boolean isValid()
	{
		if (m_certPath != null)
		{
			return m_certPath.checkValidity(new Date());
		}
		return false;
	}

	public JAPCertificate getCertificate()
	{
		return m_certificate;
	}

	public CertPath getCertPath()
	{
		return m_certPath;
	}

	public boolean checkId()
	{
		return getCertificate() != null &&
			 getId().equals(new X509SubjectKeyIdentifier(
				 getCertificate().getPublicKey()).getValueWithoutColon());
	}

	/**
	 * This returns the filename (InfoService command), where this status entry is posted at other
	 * infoservices. It's always '/feedback'. This method is used within the context of the
	 * infoservice when this status entry is forwarded to other infoservices.
	 *
	 * @return The filename where the information about this StatusInfo is posted at other
	 *         InfoServices when this entry is forwarded.
	 */
	public String getPostFile()
	{
		return "/feedback";
	}

	/**
	 * This returns the data, which are posted to other InfoServices. It's the whole XML structure
	 * of this status entry.
	 *
	 * @return The data, which are posted to other InfoServices when this entry is forwarded.
	 */
	public byte[] getPostData()
	{
		return 	m_statusXmlDataBytes;
	}

	public int getPostEncoding()
	{
		return HTTPConnectionFactory.HTTP_ENCODING_PLAIN;
	}

	/**
	 * Returns the XML structure of this status entry as we received it.
	 *
	 * @return The original XML data of this status entry.
	 */
	public String getStatusXmlData()
	{
		return m_statusXmlData;
	}

	/**
	 * Returns a HTML table line with the data of this StatusDBEntry. This method is called within
	 * the context of the infoservice by InfoServiceCommands.humanGetStatus().
	 *
	 * @return A HTML table line with the data of this status entry.
	 */
	public String getHtmlTableLine()
	{
		String htmlTableLine = "<TR><TD CLASS=\"name\">";
		MixCascade ownMixCascade = (MixCascade) Database.getInstance(MixCascade.class).getEntryById(getId());
		PerformanceEntry perfEntry = 
			(PerformanceEntry) Database.getInstance(PerformanceEntry.class).getEntryById(getId());
		int maxUsers = 0;
		if (ownMixCascade != null)
		{
			htmlTableLine = htmlTableLine + ownMixCascade.getName();			
			maxUsers = ownMixCascade.getMaxUsers();
		}
		/* generate a String, which describes the traffic situation */
		String trafficString = " (n/a)";
		if (getTrafficSituation() >= 0)
		{
			trafficString = " (low)";
		}
		if (getTrafficSituation() > 30)
		{
			trafficString = " (medium)";
		}
		if (getTrafficSituation() > 60)
		{
			trafficString = " (high)";
		}
		htmlTableLine = htmlTableLine + "</TD><TD CLASS=\"name\">" + getId() +
			"</TD><TD CLASS=\"status\" ALIGN=\"right\"><a href=\"/values/users/" + getId() + "\">" + 
			Integer.toString(getNrOfActiveUsers()) + (maxUsers > 0 ? " / " + maxUsers : "") +
			//"</TD><TD CLASS=\"status\" ALIGN=\"right\">" + Integer.toString(getCurrentRisk()) +
			"</a></TD><TD CLASS=\"status\" ALIGN=\"center\">" + Integer.toString(getTrafficSituation()) +
			trafficString +
			"</TD><TD CLASS=\"status\" ALIGN=\"right\">" +
			"<a href=\"/values/delay/" + getId() + "\">" + 
			((perfEntry != null &&
					System.currentTimeMillis() - perfEntry.getLastTestTime() < PerformanceEntry.LAST_TEST_DATA_TTL &&
					perfEntry.getLastTestAverage(PerformanceEntry.DELAY) != 0) ? String.valueOf(perfEntry.getLastTestAverage(PerformanceEntry.DELAY)) : "?") +
			" (" + ((perfEntry != null && perfEntry.getAverage(PerformanceEntry.DELAY) != 0) ? String.valueOf(perfEntry.getAverage(PerformanceEntry.DELAY)) : "?") + ") " +
			"[";
		
			long delayBound;
			if (perfEntry == null)
			{
				delayBound = -1;
			}
			else
			{
				delayBound = perfEntry.getBound(PerformanceEntry.DELAY);
			}
		
			if(delayBound == Integer.MAX_VALUE)
			{
				htmlTableLine += ">" + PerformanceEntry.BOUNDARIES[PerformanceEntry.DELAY][PerformanceEntry.BOUNDARIES[PerformanceEntry.DELAY].length - 2];
			}
			else
			{
				htmlTableLine += delayBound;
			}
		
			htmlTableLine += "] ms</a>" +
			"</TD><TD CLASS=\"status\" ALIGN=\"right\">" +
			"<a href=\"/values/speed/" + getId() + "\">" + 
			((perfEntry != null  &&
					System.currentTimeMillis() - perfEntry.getLastTestTime() < PerformanceEntry.LAST_TEST_DATA_TTL &&
					perfEntry.getLastTestAverage(PerformanceEntry.SPEED) != 0) ? String.valueOf(perfEntry.getLastTestAverage(PerformanceEntry.SPEED)) : "?") + 
			" (" + ((perfEntry != null && perfEntry.getAverage(PerformanceEntry.SPEED) != 0) ? String.valueOf(perfEntry.getAverage(PerformanceEntry.SPEED)): "?") + ") " +
					"[";
			
			long speedBound;
			if (perfEntry == null)
			{
				speedBound = -1;
			}
			else
			{
				speedBound = perfEntry.getBound(PerformanceEntry.SPEED);
			}
			
			
			if(speedBound == 0)
			{
				htmlTableLine += "<" + PerformanceEntry.BOUNDARIES[PerformanceEntry.SPEED][1];
			}
			else
			{
				htmlTableLine += speedBound;
			}

			htmlTableLine += "] kbit/s</a>" +
			"</TD><TD CLASS=\"status\" ALIGN=\"right\">" +
			NumberFormat.getInstance(Constants.LOCAL_FORMAT).format(getMixedPackets()) +
			"</TD><TD CLASS=\"status\">" + 
			new SimpleDateFormat("HH:mm:ss").format(new Date(getLastUpdate())) +
			"</TD></TR>";
		return htmlTableLine;
	}

	/**
	 * This is a compatibility method for the creation of the CurrentStatus in the MixCascade
	 * XML structure for old JAP clients.
	 * @todo remove this method, only for compatibility with JAP client < 00.02.016
	 *
	 * @return The CurrentStatus node for this status entry.
	 */
	public Node generateMixCascadeCurrentStatus()
	{
		Document doc = XMLUtil.createDocument();
		/* create the CurrentStatus element */
		Element currentStatusNode = doc.createElement("CurrentStatus");
		/* create the attributes of the CurrentStatus node */
		currentStatusNode.setAttribute("CurrentRisk", Integer.toString(getCurrentRisk()));
		currentStatusNode.setAttribute("TrafficSituation", Integer.toString(getTrafficSituation()));
		currentStatusNode.setAttribute("ActiveUsers", Integer.toString(getNrOfActiveUsers()));
		currentStatusNode.setAttribute("MixedPackets", Long.toString(getMixedPackets()));
		currentStatusNode.setAttribute("LastUpdate", Long.toString(getLastUpdate()));
		return currentStatusNode;
	}

	/**
	 * Generates an XML representation for this StatusInfo entry.
	 *
	 * @return The generated XML representation for this StatusInfo.
	 */
	private Element generateXmlRepresentation()
	{
		Document doc = XMLUtil.createDocument();
		/* create the MixCascadeStatus element */
		Element mixCascadeStatusNode = doc.createElement("MixCascadeStatus");
		/* create the attributes of the MixCascadeStatus node */
		mixCascadeStatusNode.setAttribute("id", getId());
		mixCascadeStatusNode.setAttribute("currentRisk", Integer.toString(getCurrentRisk()));
		mixCascadeStatusNode.setAttribute("mixedPackets", Long.toString(getMixedPackets()));
		mixCascadeStatusNode.setAttribute("nrOfActiveUsers", Integer.toString(getNrOfActiveUsers()));
		mixCascadeStatusNode.setAttribute("trafficSituation", Integer.toString(getTrafficSituation()));
		mixCascadeStatusNode.setAttribute("LastUpdate", Long.toString(getLastUpdate()));
		return mixCascadeStatusNode;
	}
	
	/**
	 * Returns an XML Node of the current StatusInfo using the stored XML String
	 * 
	 * @return the XML node
	 */
	public Element toXmlElement(Document a_doc)
	{
		try 
		{
			return (Element) XMLUtil.importNode(a_doc, (Node) XMLUtil.toXMLDocument(m_statusXmlDataBytes).getDocumentElement(), true);
		}
		catch(XMLParseException ex)
		{
			return null;
		}
	}
}
