package anon.infoservice;

import org.w3c.dom.Element;
import org.w3c.dom.Document;

import anon.util.XMLUtil;
import anon.util.XMLParseException;
import anon.util.IXMLEncodable;

public class PerformanceEntry extends AbstractDatabaseEntry implements IXMLEncodable
{
	private String m_strCascadeId;
	private String m_strInfoServiceId;
	
	private long m_lastUpdate;
	private long m_serial;
	
	private long m_lDelay;
	private double m_dSpeed;
	
	private long[] m_aDelays = new long[3];
	private double[] m_aSpeeds = new double[3];
	
	public static final String XML_ELEMENT_NAME = "PerformanceEntry";
	public static final String XML_ELEMENT_CONTAINER_NAME = "PerformanceEntries";
	
	public static final String XML_ATTR_ID = "id";
	public static final String XML_ATTR_AVG_DELAY = "avgDelay";
	public static final String XML_ATTR_AVG_SPEED = "avgSpeed";
	public static final String XML_ATTR_EXPIRE_TIME = "expireTime";
	
	public PerformanceEntry(String a_strCascadeId, String a_strInfoServiceId, long a_lExpireTime)
	{
		super(a_lExpireTime);
		
		m_strCascadeId = a_strCascadeId;
		m_strInfoServiceId = a_strInfoServiceId;
		
		m_lastUpdate = System.currentTimeMillis();
		m_serial = System.currentTimeMillis();
		m_lDelay = -1;
		m_dSpeed = -1;
	}
	
	public PerformanceEntry(Element a_entry) throws XMLParseException
	{
		// TODO: client expire time
		super(System.currentTimeMillis() + 1000*60*24);
		
		XMLUtil.assertNodeName(a_entry, XML_ELEMENT_NAME);
		
		String id = XMLUtil.parseAttribute(a_entry, XML_ATTR_ID, ".");
		int i = id.indexOf(".");
		
		if(i < 0)
		{
			throw new XMLParseException(XML_ELEMENT_NAME + ": Could not parse id");
		}
		
		m_strCascadeId = id.substring(0, i);
		m_strInfoServiceId = id.substring(i);
		m_lDelay = XMLUtil.parseAttribute(a_entry, XML_ATTR_AVG_DELAY, -1);
		m_dSpeed = XMLUtil.parseAttribute(a_entry, XML_ATTR_AVG_SPEED, -1);
		
		m_lastUpdate = System.currentTimeMillis();
		m_serial = System.currentTimeMillis();
	}
	
	/**
	 * Use IS and cascade IDs since this entry depends on a specific
	 * cascade and the info service we're retrieving it from.
	 */
	public String getId()
	{
		return m_strCascadeId + "." + m_strInfoServiceId;
	}

	public long getLastUpdate()
	{
		return m_lastUpdate;
	}

	public long getVersionNumber() 
	{
		return m_serial;
	}
	
	public void updateDelay(long a_lDelay) 
	{
		if(m_aDelays == null)
			m_aDelays = new long[3]; // TODO: Make number of fixings configurable
		m_lDelay = 0;
		for(int i=1;i < m_aDelays.length;i++)
			m_lDelay += (m_aDelays[i-1] = m_aDelays[i]);
		m_aDelays[m_aDelays.length-1] = a_lDelay;
		m_lDelay = (m_lDelay + a_lDelay) / m_aDelays.length;
		
		m_lastUpdate = System.currentTimeMillis();
	}
	
	public void updateSpeed(double a_dSpeed) 
	{
		if(m_aSpeeds == null)
			m_aSpeeds = new double[3]; // TODO: Make number of fixings configurable
		m_dSpeed = 0.0;
		for(int i=1; i < m_aSpeeds.length; i++)
			m_dSpeed += (m_aSpeeds[i-1] = m_aSpeeds[i]);
		m_aSpeeds[m_aSpeeds.length-1] = a_dSpeed;
		m_dSpeed = (m_dSpeed + a_dSpeed) / m_aSpeeds.length;
		
		m_lastUpdate = System.currentTimeMillis();
	}

	public double getAverageSpeed()
	{
		return m_dSpeed;
	}
	
	public long getAverageDelay()
	{
		return m_lDelay;
	}
	
	public Element toXmlElement(Document a_doc)
	{
		Element elem = a_doc.createElement(XML_ELEMENT_NAME);
		XMLUtil.setAttribute(elem, XML_ATTR_ID, getId());
		XMLUtil.setAttribute(elem, XML_ATTR_AVG_DELAY, m_lDelay);
		XMLUtil.setAttribute(elem, XML_ATTR_AVG_SPEED, m_dSpeed);
		XMLUtil.setAttribute(elem, XML_ATTR_EXPIRE_TIME, getExpireTime());
		
		return elem;
	}
}
