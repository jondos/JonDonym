package anon.infoservice;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import java.util.Hashtable;
import java.util.Vector;

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
		
		/*
		 * TODO: this obviously needs some improvement, this is just a first draft
		 */
		long speed = 0;
		long delay = 0;
		for(int j = 0; j < v.size(); j++)
		{
			speed += ((PerformanceEntry) v.get(j)).getAverageSpeed();
			delay += ((PerformanceEntry) v.get(j)).getAverageDelay();
		}
		speed /= v.size();
		delay /= v.size();
		
		for(int k = 0; k < v.size(); k++)
		{
			double straySpeed = Math.abs(speed - ((PerformanceEntry) v.get(k)).getAverageSpeed()) / speed;
			double strayDelay = Math.abs(delay - ((PerformanceEntry) v.get(k)).getAverageDelay()) / delay;
			if(straySpeed > 0.6)
			{
				LogHolder.log(LogLevel.INFO, LogType.MISC, "Ignoring performance entry with speed " + ((PerformanceEntry) v.get(k)).getAverageSpeed());
				((PerformanceEntry) v.get(k)).setAverageSpeed(0);
			}
			
			if(strayDelay > 0.6)
			{
				LogHolder.log(LogLevel.INFO, LogType.MISC, "Ignoring performance entry with delay " + ((PerformanceEntry) v.get(k)).getAverageDelay());
				((PerformanceEntry) v.get(k)).setAverageDelay(0);
			}
		}
		
		if(v.size() == 0)
		{
			return avgEntry;
		}
		
		speed = 0;
		delay = 0;
		for(int j = 0; j < v.size(); j++)
		{
			if(((PerformanceEntry) v.get(j)).getAverageSpeed() != 0)
			{
				speed += ((PerformanceEntry) v.get(j)).getAverageSpeed();
			}
			
			if(((PerformanceEntry) v.get(j)).getAverageSpeed() != 0)
			{
				delay += ((PerformanceEntry) v.get(j)).getAverageDelay();
			}
		}
		speed /= v.size();
		delay /= v.size();
		
		avgEntry.setAverageSpeed(speed);
		avgEntry.setAverageDelay(delay);
		
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
}