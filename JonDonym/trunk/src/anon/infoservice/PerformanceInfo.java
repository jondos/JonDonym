package anon.infoservice;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.util.Hashtable;
import java.util.Vector;

import anon.crypto.CertPath;
import anon.crypto.JAPCertificate;
import anon.crypto.SignatureVerifier;
import anon.crypto.XMLSignature;
import anon.util.Util;
import anon.util.Util.IntegerSortAsc;
import anon.util.Util.IntegerSortDesc;
import anon.util.XMLParseException;
import anon.util.XMLUtil;
import anon.util.IXMLEncodable;
import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;

/**
 * A PerformanceInfo object holds all PerformanceEntry objects retrieved 
 * by a certain Info Service through the /performanceinfo command.
 * 
 * It is stored in the JAP client database using the id of the Info Service.
 * 
 * To access the data the static method getAverageEntry should be used
 * rather accessing the database directly! This way it is ensured that the entry
 * is derived from all available Info Services.
 * 
 * @see PerformanceInfo#getLowestCommonBoundEntry(String)
 * 
 * @author Christian Banse
 */
public class PerformanceInfo extends AbstractCertifiedDatabaseEntry implements IXMLEncodable
{
	private static final double PERFORMANCE_INFO_MIN_PERCENTAGE_OF_VALID_ENTRIES = 2.0/3.0;

	/**
	 * Last Update time of the database entry
	 */
	private long m_lastUpdate;
	
	/**
	 * Serial number of the database entry
	 */
	private long m_serial;
	
	/**
	 * The id of the entry - should be a Info Service Id
	 */
	private String m_id;
	
	/**
	 * Stored XML data for toXmlElement()
	 */
	private Element m_xmlData;
	
	private JAPCertificate m_isCertificate;
	
	/**
	 * All PerformanceEntry objects measured by the info service
	 */
	private Hashtable m_entries = new Hashtable();
	
	public static final String XML_ATTR_ID = "id";
	public static final String XML_ELEMENT_NAME = "PerformanceInfo";
	public static final String XML_ELEMENT_CONTAINER_NAME = "PerformanceInfoList";
	
	/*
	 * Timeout set to 6 days because it's better to have out-dated 
	 * performance information than no performance information at all ;-)
	 */
	public static final int PERFORMANCE_INFO_TTL = 1000*60*60*24*6;
	
	/**
	 * Creates a new PerformanceInfo object from an XML element which is usually
	 * either retrieved from the Info Service or stored in the configuration file 
	 * and loaded at startup.
	 * 
	 * @param a_info The XML data
	 * 
	 * @throws XMLParseException
	 */
	public PerformanceInfo(Element a_info) throws XMLParseException
	{
		super(System.currentTimeMillis() + PERFORMANCE_INFO_TTL);
		
		if (a_info == null)
		{
			throw new XMLParseException("Could not parse PerformanceInfo. Invalid document element.");
		}
		
		NodeList list = a_info.getElementsByTagName(PerformanceEntry.XML_ELEMENT_NAME);
				
		/* try to get the certificate from the Signature node */
		try
		{
			XMLSignature signature = SignatureVerifier.getInstance().getVerifiedXml(a_info,
				SignatureVerifier.DOCUMENT_CLASS_INFOSERVICE);
			if (signature != null && signature.isVerified())
			{
				CertPath certPath = signature.getCertPath();
				if (certPath != null && certPath.getFirstCertificate() != null)
				{
					m_isCertificate = certPath.getFirstCertificate();
				}
			}
		}
		catch (Exception e)
		{
			LogHolder.log(LogLevel.ERR, LogType.MISC, e);
		}
		
		m_id = XMLUtil.parseAttribute(a_info, XML_ATTR_ID, "");
		
		if(!checkId())
		{
			throw new XMLParseException(XML_ELEMENT_NAME + ": invalid id");
		}
		
		for(int i = 0; i < list.getLength(); i++)
		{
			PerformanceEntry entry = new PerformanceEntry((Element)list.item(i));
			m_entries.put(entry.getId(), entry);
		}
		
		// TODO for security reasons, do not interpret structures without last update!
		m_lastUpdate = XMLUtil.parseAttribute(a_info, XML_ATTR_LAST_UPDATE, System.currentTimeMillis());  
		m_serial = m_lastUpdate;
		m_xmlData = a_info;
	}
	
	public JAPCertificate getCertificate()
	{
		return m_isCertificate;
	}
	
	public boolean isVerified()
	{
		return m_isCertificate != null;
	}
	
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
		return m_serial;
	}

	/**
	 * Returns a performance entry for the giving Cascade. This method
	 * should ONLY be used inside getLowestCommonBoundEntry!
	 * 
	 * @see PerformanceInfo#getLowestCommonBoundEntry(String)
	 * 
	 * @param a_id The cascade id
	 * 
	 * @return The performance entry of the given cascade
	 */
	private PerformanceEntry getEntry(String a_id)
	{
		return (PerformanceEntry) m_entries.get(a_id);
	}
	
	/**
	 * Loops through all PerformanceInfo objects from the different 
	 * Info Services and returns a new PerformanceEntry for the
	 * given cascade with the lowest common boundary
	 * 
	 * @param a_cascadeId Id of the cascade
	 * 
	 * @return PerformanceEntry with lowest common boundary values for the given cascade
	 */
	public static PerformanceEntry getLowestCommonBoundEntry(String a_cascadeId)
	{
		// loop through all PerformanceInfo objects (from the different Info Services)
		PerformanceEntry perfEntry = new PerformanceEntry(a_cascadeId, true);
		PerformanceEntry.StabilityAttributes attributes;
		
		Vector vPerfEntries = new Vector();
		Vector vSpeedBoundaries = new Vector();
		Vector vSpeedNotRecoveredBoundaries = new Vector();
		Vector vSpeedBestBoundaries = new Vector();
		Vector vDelayBoundaries = new Vector();
		Vector vDelayNotRecoveredBoundaries = new Vector();
		Vector vDelayBestBoundaries = new Vector();
		Vector vErrorBoundaries = new Vector();
		Vector vUnknownBoundaries = new Vector();
		Vector vResetsBoundaries = new Vector();
		Integer value;
		
		Vector vInfoServices = Database.getInstance(PerformanceInfo.class).getEntryList();
		for (int i = 0; i < vInfoServices.size(); i++)
		{
			PerformanceEntry entry = ((PerformanceInfo) vInfoServices.elementAt(i)).getEntry(a_cascadeId);
			if (entry != null)			{
				vPerfEntries.addElement(entry);
				// extract the bound value for speed
				value = new Integer(entry.getBound(PerformanceEntry.SPEED).getBound());
				if (value.intValue() != Integer.MAX_VALUE && value.intValue() >= 0)
				{
					vSpeedBoundaries.addElement(value);
				}				
				value = new Integer(entry.getBound(PerformanceEntry.SPEED).getNotRecoveredBound());
				if (value.intValue() != Integer.MAX_VALUE && value.intValue() >= 0)
				{
					vSpeedNotRecoveredBoundaries.addElement(value);
				}
				
				// extract the bound value for delay
				value = new Integer(entry.getBound(PerformanceEntry.DELAY).getBound());
				if (value.intValue() > 0)
				{
					vDelayBoundaries.addElement(value);
				}
				value = new Integer(entry.getBound(PerformanceEntry.DELAY).getNotRecoveredBound());
				if (value.intValue() > 0)
				{
					vDelayNotRecoveredBoundaries.addElement(value);
				}
				
				// extract the best bound value for speed
				value = new Integer(entry.getBestBound(PerformanceEntry.SPEED));
				if (value.intValue() != Integer.MAX_VALUE && value.intValue() >= 0)
				{
					vSpeedBestBoundaries.addElement(value);
				}
				
				// extract the best delay value for delay
				value = new Integer(entry.getBestBound(PerformanceEntry.DELAY));
				if (value.intValue() > 0)
				{
					vDelayBestBoundaries.addElement(value);
				}
				
				attributes = entry.getStabilityAttributes();
				if (attributes.getValueSize() > 0)
				{
					vErrorBoundaries.addElement(new Integer(attributes.getBoundErrors()));
					vUnknownBoundaries.addElement(new Integer(attributes.getBoundUnknown()));
					vResetsBoundaries.addElement(new Integer(attributes.getBoundResets()));
				}
			}
		}
		
		vInfoServices.removeAllElements();
		vInfoServices = null; // for garbage collection
		
		if (vPerfEntries.size() == 0)
		{
			perfEntry.setBound(PerformanceEntry.SPEED, new PerformanceEntry.Bound(Integer.MAX_VALUE, Integer.MAX_VALUE));
			perfEntry.setBestBound(PerformanceEntry.SPEED, Integer.MAX_VALUE);
			perfEntry.setBound(PerformanceEntry.DELAY, new PerformanceEntry.Bound(0,0));
			perfEntry.setBestBound(PerformanceEntry.DELAY, 0);
			perfEntry.setStabilityAttributes(new PerformanceEntry.StabilityAttributes(0, 0, 0, 0));
			return perfEntry;
		}
		
		Util.sort(vSpeedBoundaries, new IntegerSortDesc());
		Util.sort(vSpeedNotRecoveredBoundaries, new IntegerSortDesc());
		Util.sort(vSpeedBestBoundaries, new IntegerSortDesc());
		Util.sort(vDelayBoundaries, new IntegerSortAsc());
		Util.sort(vDelayNotRecoveredBoundaries, new IntegerSortAsc());
		Util.sort(vDelayBestBoundaries, new IntegerSortAsc());
		Util.sort(vErrorBoundaries, new IntegerSortAsc());
		Util.sort(vUnknownBoundaries, new IntegerSortAsc());
		Util.sort(vResetsBoundaries, new IntegerSortAsc());
		
		attributes = new PerformanceEntry.StabilityAttributes(100, 
				getMajorityBoundFromSortedBounds(vUnknownBoundaries, 0),
				getMajorityBoundFromSortedBounds(vErrorBoundaries, 0),
				getMajorityBoundFromSortedBounds(vResetsBoundaries, 0));
				
		perfEntry.setBound(PerformanceEntry.SPEED, 
				new PerformanceEntry.Bound(getMajorityBoundFromSortedBounds(vSpeedBoundaries, Integer.MAX_VALUE),
						getMajorityBoundFromSortedBounds(vSpeedNotRecoveredBoundaries, Integer.MAX_VALUE)));
		perfEntry.setBestBound(PerformanceEntry.SPEED, getMajorityBoundFromSortedBounds(vSpeedBestBoundaries, Integer.MAX_VALUE));
		perfEntry.setBound(PerformanceEntry.DELAY, 
				new PerformanceEntry.Bound(getMajorityBoundFromSortedBounds(vDelayBoundaries, 0),
						getMajorityBoundFromSortedBounds(vDelayNotRecoveredBoundaries, 0)));
		perfEntry.setBestBound(PerformanceEntry.DELAY, getMajorityBoundFromSortedBounds(vDelayBestBoundaries, 0));
		perfEntry.setStabilityAttributes(attributes);
		
		return perfEntry;
	}

	private static int getMajorityBoundFromSortedBounds(Vector a_vecSortedIntegers, int a_default)
	{
		// bound error
		int bound = a_default;
		for(int i = 0; i< a_vecSortedIntegers.size(); i++)
		{
			bound = ((Integer) a_vecSortedIntegers.elementAt(i)).intValue();
			if ((double)(i+1) / (double)a_vecSortedIntegers.size() >= PERFORMANCE_INFO_MIN_PERCENTAGE_OF_VALID_ENTRIES)
			{
				// found the bound!
				break;
			}
		}
		return bound;
	}
	
	/**
	 * Returns an XML Node of the current PerformanceInfo using the stored XML data
	 * 
	 * @return the XML node
	 */
	public Element toXmlElement(Document a_doc)
	{
		try
		{
			return (Element) XMLUtil.importNode(a_doc, m_xmlData, true);
	
		}
		catch(XMLParseException ex)
		{
			LogHolder.log(LogLevel.EXCEPTION, LogType.NET, "Could not store PerformanceInfo to XML element", ex);
			
			return null;
		}
	}
}