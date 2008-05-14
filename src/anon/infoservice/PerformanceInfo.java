package anon.infoservice;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import java.util.Hashtable;
import java.util.Vector;
import java.util.Enumeration;

import anon.util.XMLParseException;
import anon.util.XMLUtil;
import anon.util.IXMLEncodable;
import anon.util.XMLParseException;
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
 * @see PerformanceInfo#getAverageEntry(String)
 * 
 * @author Christian Banse
 */
public class PerformanceInfo extends AbstractDatabaseEntry implements IXMLEncodable
{
	private static final double PERFORMANCE_INFO_MIN_PERCENTAGE_OF_VALID_ENTRIES = 2.0/3.0;
	private static final double PERFORMANCE_INFO_MAX_STRAY = 0.7;

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
	
	/**
	 * All PerformanceEntry objects measured by the info service
	 */
	private Hashtable m_entries = new Hashtable();
	
	public static final String XML_ATTR_ID = "id";
	public static final String XML_ELEMENT_NAME = "PerformanceInfo";
	public static final String XML_ELEMENT_CONTAINER_NAME = "PerformanceInfoList";
	
	/*
	 * Timeout set to 6 hours because it's better to have out-dated 
	 * performance information than no performance information at all ;-)
	 */
	public static final int PERFORMANCE_INFO_TTL = 1000*60*60*6;
	
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
		
		m_id = XMLUtil.parseAttribute(a_info, XML_ATTR_ID, "");
		
		if(m_id == "")
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
	 * should ONLY be used inside getAverageEntry!
	 * 
	 * @see PerformanceInfo#getAverageEntry(String)
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
	 * Info Services and returns a new average PerformanceEntry for the
	 * given cascade.
	 * 
	 * @param a_cascadeId Id of the cascade
	 * 
	 * @return PerformanceEntry with average values for the given cascade
	 */
	public static PerformanceEntry getAverageEntry(String a_cascadeId)
	{
		// loop through all PerformanceInfo objects (from the different Info Services)
		// and calculate the average
		
		Vector vec = Database.getInstance(PerformanceInfo.class).getEntryList();
		PerformanceEntry avgEntry = new PerformanceEntry(a_cascadeId, -1);
		
		Vector v = new Vector();
		
		for (int i = 0; i < vec.size(); i++)
		{
			PerformanceEntry entry = ((PerformanceInfo) vec.elementAt(i)).getEntry(a_cascadeId);
			if (entry != null)
			{
				v.addElement(entry);
			}
		}
		
		if(v.size() == 0)
		{
			return avgEntry;
		}
		
		long avgSpeed = 0;
		long avgDelay = 0;
		for(int j = 0; j < v.size(); j++)
		{
			avgSpeed += ((PerformanceEntry) v.elementAt(j)).getAverageSpeed();
			avgDelay += ((PerformanceEntry) v.elementAt(j)).getAverageDelay();
		}
		avgSpeed /= v.size();
		avgDelay /= v.size();
		
		Vector vToCheck = (Vector) v.clone();
		Vector vResult = new Vector();
		Vector vDeleted = new Vector();
		double stray = PERFORMANCE_INFO_MAX_STRAY;
		
		// loop through all entries to eliminate stray entries
		// if we deleted too many entries, re-add deleted entries
		do
		{
			stray = eliminateStrayEntries(vToCheck, vDeleted, avgSpeed, avgDelay, stray);
			// add the entries that passed the test to the result vector
			for (Enumeration e = vToCheck.elements(); e.hasMoreElements(); )
			{
				vResult.addElement(e.nextElement() ); //.addAll would be faster, but is post-JDK 1.1.8
			}
			//vResult.addAll(vToCheck);
			// only check the deleted entries next round
			vToCheck = vDeleted;
			// reset the deleted entries vector
			vDeleted = new Vector();
		}
		while ((double)vResult.size() / v.size() < PERFORMANCE_INFO_MIN_PERCENTAGE_OF_VALID_ENTRIES);
		
		if (vResult.size() == 0)
		{
			return avgEntry;
		}
		
		avgSpeed = 0;
		avgDelay = 0;
		for(int j = 0; j < vResult.size(); j++)
		{
			if(((PerformanceEntry) vResult.elementAt(j)).getAverageSpeed() != 0)
			{
				avgSpeed += ((PerformanceEntry) vResult.elementAt(j)).getAverageSpeed();
			}
			
			if(((PerformanceEntry) vResult.elementAt(j)).getAverageSpeed() != 0)
			{
				avgDelay += ((PerformanceEntry) vResult.elementAt(j)).getAverageDelay();
			}
		}
		avgSpeed /= vResult.size();
		avgDelay /= vResult.size();
		
		avgEntry.setAverageSpeed(avgSpeed);
		avgEntry.setAverageDelay(avgDelay);
		
		return avgEntry;
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
	public static double eliminateStrayEntries(Vector a_vec, Vector r_vecDeleted, long a_avgSpeed, long a_avgDelay, double a_maxStray)
	{
		LogHolder.log(LogLevel.DEBUG, LogType.MISC, "Looking for entries with stray >" + a_maxStray);
		double nextStray = Double.MAX_VALUE;
		
		for(int k = 0; k < a_vec.size(); k++)
		{
			PerformanceEntry entry = ((PerformanceEntry) a_vec.elementAt(k));
			
			double straySpeed = (double) Math.abs(a_avgSpeed - entry.getAverageSpeed()) / (double) a_avgSpeed;
			double strayDelay = (double) Math.abs(a_avgDelay - entry.getAverageDelay()) / (double) a_avgDelay;
			if(straySpeed > a_maxStray)
			{
				LogHolder.log(LogLevel.DEBUG, LogType.MISC, "Ignoring performance entry with speed " + entry.getAverageSpeed());
				
				r_vecDeleted.addElement(entry);
				
				a_vec.removeElementAt(k);
				k--;
				
				if(straySpeed < nextStray)
				{
					nextStray = straySpeed;
				}
				
				continue;
			}
			
			if(strayDelay > a_maxStray)
			{
				LogHolder.log(LogLevel.DEBUG, LogType.MISC, "Ignoring performance entry with delay " + entry.getAverageDelay());
				
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
	}
}