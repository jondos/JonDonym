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

		if(a_info == null)
		{
			throw new XMLParseException("Could not parse PerformanceInfo. Invalid document element.");
		}
		
		NodeList list = a_info.getElementsByTagName("PerformanceEntry");
		
		
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
		
		m_lastUpdate = System.currentTimeMillis();
		m_serial = System.currentTimeMillis();
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
		PerformanceEntry perfEntry = new PerformanceEntry(a_cascadeId, false);
		
		Vector vPerfEntries = new Vector();
		Vector vSpeedBoundaries = new Vector();
		Vector vDelayBoundaries = new Vector();
		
		Vector vInfoServices = Database.getInstance(PerformanceInfo.class).getEntryList();
		for (int i = 0; i < vInfoServices.size(); i++)
		{
			PerformanceEntry entry = ((PerformanceInfo) vInfoServices.elementAt(i)).getEntry(a_cascadeId);
			if (entry != null)
			{
				vPerfEntries.addElement(entry);
				Integer value = new Integer(entry.getBound(PerformanceEntry.SPEED));
				
				if (!vSpeedBoundaries.contains(value))
				{
					vSpeedBoundaries.addElement(value);
				}
				
				value = new Integer(entry.getBound(PerformanceEntry.DELAY));
				
				if (!vDelayBoundaries.contains(value))
				{
					vDelayBoundaries.addElement(value);
				}
			}
		}
		
		Util.sort(vSpeedBoundaries, new IntegerSortDesc());
		Util.sort(vDelayBoundaries, new IntegerSortAsc());
		
		if(vPerfEntries.size() == 0)
		{
			return perfEntry;
		}
		
		int agreeing;
		int value = 0;
		
		for(int i = 0; i < vSpeedBoundaries.size(); i++)
		{
			agreeing = 0;
			int bound = ((Integer) vSpeedBoundaries.elementAt(i)).intValue();
			value = bound;
			
			for(int j = 0; j < vPerfEntries.size(); j++)
			{
				PerformanceEntry entry = (PerformanceEntry) vPerfEntries.elementAt(j);
				if(entry.getBound(PerformanceEntry.SPEED) >= bound)
				{
					agreeing++;
				}
			}
			
			if((double) agreeing / vPerfEntries.size() >= PERFORMANCE_INFO_MIN_PERCENTAGE_OF_VALID_ENTRIES)
			{
				break;
			}
		}
		perfEntry.setBound(PerformanceEntry.SPEED, value);
		
		for(int i = 0; i< vDelayBoundaries.size(); i++)
		{
			agreeing = 0;
			int bound = ((Integer) vDelayBoundaries.elementAt(i)).intValue();
			value = bound;
			
			for(int j = 0; j < vPerfEntries.size(); j++)
			{
				PerformanceEntry entry = (PerformanceEntry) vPerfEntries.elementAt(j);
				if(entry.getBound(PerformanceEntry.DELAY) <= bound)
				{
					agreeing++;
				}
			}
			
			if((double) agreeing / vPerfEntries.size() >= PERFORMANCE_INFO_MIN_PERCENTAGE_OF_VALID_ENTRIES)
			{
				break;
			}
		}
		perfEntry.setBound(PerformanceEntry.DELAY, value);
		
		return perfEntry;
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
	
	/**
	 * Eliminates all stray entries from the vector.
	 * 
	 * @param a_vec			The entries to check
	 * @param r_vecDeleted	A vector that will hold the deleted entries
	 * @param a_avgSpeed	The average speed of the vector
	 * @param a_avgDelay	The average delay of the vector
	 * @param a_maxStray	The maximum stray an entry is allowed to have
	 * 
	 * @return The minimum stray of the deleted entries
	 */
	/*public static double eliminateStrayEntries(Vector a_vec, Vector r_vecDeleted, long a_avgSpeed, long a_avgDelay, double a_maxStray)
	{
		LogHolder.log(LogLevel.DEBUG, LogType.MISC, "Looking for entries with stray >" + a_maxStray);
		double nextStray = Double.MAX_VALUE;
		
		for(int k = 0; k < a_vec.size(); k++)
		{
			PerformanceEntry entry = ((PerformanceEntry) a_vec.elementAt(k));
			
			double straySpeed = (double) Math.abs(a_avgSpeed - entry.getXMLAverage(PerformanceEntry.SPEED)) / (double) a_avgSpeed;
			double strayDelay = (double) Math.abs(a_avgDelay - entry.getXMLAverage(PerformanceEntry.DELAY)) / (double) a_avgDelay;
			if(entry.getXMLAverage(PerformanceEntry.SPEED) >= 0 && straySpeed > a_maxStray)
			{
				LogHolder.log(LogLevel.DEBUG, LogType.MISC, "Ignoring performance entry with speed " + entry.getAverage(PerformanceEntry.SPEED));
				
				r_vecDeleted.addElement(entry);
				
				a_vec.removeElementAt(k);
				k--;
				
				if(straySpeed < nextStray)
				{
					nextStray = straySpeed;
				}
				
				continue;
			}
			
			if(entry.getXMLAverage(PerformanceEntry.DELAY) >= 0 && strayDelay > a_maxStray)
			{
				LogHolder.log(LogLevel.DEBUG, LogType.MISC, "Ignoring performance entry with delay " + entry.getAverage(PerformanceEntry.DELAY));
				
				r_vecDeleted.addElement(entry);
				
				a_vec.removeElementAt(k);
				k--;
				
				if(strayDelay < nextStray)
				{
					nextStray = strayDelay;
				}
				
				continue;				
			}
		}
		
		return nextStray;
	}*/
}