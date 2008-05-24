package anon.infoservice;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.Document;

import anon.util.XMLUtil;
import anon.util.XMLParseException;
import anon.util.IXMLEncodable;

public class PerformanceEntry extends AbstractDatabaseEntry implements IXMLEncodable
{
	private String m_strCascadeId;
	
	private long m_lastUpdate;
	private long m_serial;
	
	private long m_lDelay;
	private long m_lSpeed;
	
	private long[] m_aDelays;
	private long[] m_aSpeeds;
	
	public static final String XML_ELEMENT_CONTAINER_NAME = "PerformanceInfo";
	
	public static final int PERFORMANCE_ENTRY_TTL = 1000*60*60; // 1 hour
	
	public static final String XML_ELEMENT_NAME = "PerformanceEntry";	
	public static final String XML_ELEMENT_AVG_DELAY = "avgDelay";
	public static final String XML_ELEMENT_AVG_SPEED = "avgSpeed";
	
	public static final String XML_ATTR_ID = "id";
	
	public PerformanceEntry(String a_strCascadeId, long a_lExpireTime)
	{
		super(a_lExpireTime);
		
		m_strCascadeId = a_strCascadeId;
		
		m_lastUpdate = System.currentTimeMillis();
		m_serial = System.currentTimeMillis();
		m_lDelay = -1;
		m_lSpeed = -1;
	}
	
	public PerformanceEntry(Element a_entry) throws XMLParseException
	{
		super(System.currentTimeMillis() + PERFORMANCE_ENTRY_TTL);
		
		XMLUtil.assertNodeName(a_entry, XML_ELEMENT_NAME);
		
		m_strCascadeId = XMLUtil.parseAttribute(a_entry, XML_ATTR_ID, "");
		
		if(m_strCascadeId == "")
		{
			throw new XMLParseException(XML_ELEMENT_NAME + ": invalid id");
		}
		
		Node elemDelay = XMLUtil.getFirstChildByName(a_entry, XML_ELEMENT_AVG_DELAY);
		if(elemDelay == null)
		{
			throw new XMLParseException(XML_ELEMENT_NAME + ": Could not find node " + XML_ELEMENT_AVG_DELAY);
		}
		m_lDelay = XMLUtil.parseValue(elemDelay, -1);
		
		Node elemSpeed = XMLUtil.getFirstChildByName(a_entry, XML_ELEMENT_AVG_SPEED);
		if(elemSpeed == null)
		{
			throw new XMLParseException(XML_ELEMENT_NAME + ": Could not find node " + XML_ELEMENT_AVG_SPEED);
		}
		m_lSpeed = XMLUtil.parseValue(elemSpeed, -1);
		
		m_lastUpdate = System.currentTimeMillis();
		m_serial = System.currentTimeMillis();
	}
	
	public String getId()
	{
		return m_strCascadeId;
	}

	public long getLastUpdate()
	{
		return m_lastUpdate;
	}

	public long getVersionNumber() 
	{
		return m_serial;
	}
	
	public void updateDelay(long a_lDelay, int maxEntries) 
	{
		if (a_lDelay <= 0)
		{
			return;
		}
		
		if(m_aDelays == null)
		{
			m_aDelays = new long[maxEntries];
		}
		m_lDelay = 0;
		int numEntries = 1;
		for(int i = 1; i < m_aDelays.length; i++)
		{
			if(m_aDelays[i] != 0) 
			{
				numEntries++;
			}
			m_lDelay += (m_aDelays[i-1] = m_aDelays[i]);
		}
		m_aDelays[m_aDelays.length-1] = a_lDelay;
		m_lDelay = (m_lDelay + a_lDelay) / numEntries;
		
		m_lastUpdate = System.currentTimeMillis();
	}
	
	public void updateSpeed(long a_iSpeed, int maxEntries) 
	{
		if (a_iSpeed <= 0)
		{
			return;
		}
		
		if(m_aSpeeds == null)
		{
			m_aSpeeds = new long[maxEntries];
		}
		m_lSpeed = 0;
		int numEntries = 1;
		for(int i=1; i < m_aSpeeds.length; i++)
		{
			if(m_aSpeeds[i] != 0) 
			{
				numEntries++;
			}
			m_lSpeed += (m_aSpeeds[i-1] = m_aSpeeds[i]);
		}
		m_aSpeeds[m_aSpeeds.length-1] = a_iSpeed;
		m_lSpeed = (m_lSpeed + a_iSpeed) / numEntries;
		
		m_lastUpdate = System.currentTimeMillis();
	}
	
	public void setAverageSpeed(long a_lSpeed)
	{
		m_lSpeed = a_lSpeed;
	}
	
	public void setAverageDelay(long a_lDelay)
	{
		m_lDelay = a_lDelay;
	}
	
	public long getAverageSpeed()
	{
		return m_lSpeed;
	}
	
	public long getAverageDelay()
	{
		return m_lDelay;
	}
	
	public boolean isInvalid()
	{
		return (m_lSpeed == -1) || (m_lSpeed == 0) || (m_lDelay == 0) || (m_lDelay == -1);
	}
	
	public Element toXmlElement(Document a_doc)
	{
		Element elem = a_doc.createElement(XML_ELEMENT_NAME);
		XMLUtil.setAttribute(elem, XML_ATTR_ID, getId());
		
		Element elemDelay = a_doc.createElement(XML_ELEMENT_AVG_DELAY);
		XMLUtil.setValue(elemDelay, m_lDelay);
		
		Element elemSpeed = a_doc.createElement(XML_ELEMENT_AVG_SPEED);
		XMLUtil.setValue(elemSpeed, m_lSpeed);
		
		elem.appendChild(elemDelay);
		elem.appendChild(elemSpeed);
		
		return elem;
	}
}
