package anon.infoservice;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import anon.util.XMLParseException;
import anon.util.XMLUtil;

import java.util.Hashtable;
import java.util.Vector;

import anon.util.XMLParseException;

public class PerformanceInfo extends AbstractDatabaseEntry 
{
	private long m_lastUpdate;
	private long m_serial;
	
	private String m_id;
	
	private Hashtable m_entries = new Hashtable();
	
	public static final String XML_ATTR_ID = "id";
	public static final String XML_ELEMENT_NAME = "PerformanceInfo";	
	
	public PerformanceInfo(Element a_info) throws XMLParseException
	{
		super(System.currentTimeMillis() + 1000*60*60);

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

	public PerformanceEntry getEntry(String a_id)
	{
		return (PerformanceEntry) m_entries.get(a_id);
	}
	
	public static PerformanceEntry getAverageEntry(String a_cascadeId)
	{
		// loop through all PerformanceInfo objects (from the different Infoservices)
		// and calculate the average
		
		Vector vec = Database.getInstance(PerformanceInfo.class).getEntryList();
		PerformanceEntry avgEntry = new PerformanceEntry(a_cascadeId, -1);
		
		for(int i = 0; i < vec.size(); i++)
		{
			PerformanceEntry entry = ((PerformanceInfo) vec.elementAt(i)).getEntry(a_cascadeId);
			if(entry != null)
			{
				avgEntry.updateSpeed(entry.getAverageSpeed());
				avgEntry.updateDelay(entry.getAverageDelay());
			}
		}
		
		return avgEntry;
	}

}
