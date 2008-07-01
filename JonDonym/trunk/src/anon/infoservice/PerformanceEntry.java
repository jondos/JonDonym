package anon.infoservice;

import java.util.Vector;
import java.util.Calendar;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.Document;

import anon.util.XMLUtil;
import anon.util.XMLParseException;
import anon.util.IXMLEncodable;

public class PerformanceEntry extends AbstractDatabaseEntry implements IXMLEncodable
{
	private static final String XML_ATTR_HOUR = "hour";
	private static final String XML_ATTR_DAY = "day";
	public static final String XML_ATTR_ID = "id";
	
	private static final String XML_ELEMENT_CURRENT_DATA = "CurrentData";
	private static final String XML_ELEMENT_HOURLY_DATA = "HourlyData";
	private static final String XML_ELEMENT_DAILY_DATA = "DailyData";
	private static final String XML_ELEMENT_WEEKLY_DATA = "WeeklyData";
	
	private String m_strCascadeId;
	private Calendar m_cal = Calendar.getInstance();
	
	private long m_lastUpdate;
	private long m_serial;
	
	/*private long m_lDelay;
	private long m_lSpeed;
	
	private long[] m_aDelays;
	private long[] m_aSpeeds;*/
	
	private PerformanceAttributeEntry[][] m_speed = new PerformanceAttributeEntry[7][24];
	private PerformanceAttributeEntry[][] m_delay = new PerformanceAttributeEntry[7][24];
	
	public static final String XML_ELEMENT_CONTAINER = "PerformanceInfo";
	
	public static final int PERFORMANCE_ENTRY_TTL = 1000*60*60; // 1 hour
	
	public static final String XML_ELEMENT_NAME = "PerformanceEntry";	

	
	public PerformanceEntry(String a_strCascadeId, long a_lExpireTime)
	{
		super(a_lExpireTime);
		
		m_strCascadeId = a_strCascadeId;
		
		m_lastUpdate = System.currentTimeMillis();
		m_serial = System.currentTimeMillis();
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
		
		Node elemCurrentData = XMLUtil.getFirstChildByName(a_entry, XML_ELEMENT_CURRENT_DATA);
		if(elemCurrentData == null)
		{
			throw new XMLParseException(XML_ELEMENT_NAME + ": Could not find node " + XML_ELEMENT_CURRENT_DATA);
		}
		
		int dayOfWeek = m_cal.get(Calendar.DAY_OF_WEEK);
		int hour = m_cal.get(Calendar.HOUR_OF_DAY);
		
		Node elemDelay = XMLUtil.getFirstChildByName(elemCurrentData, PerformanceAttributeEntry.PERFORMANCE_ATTRIBUTE_DELAY);
		if(elemDelay == null)
		{
			throw new XMLParseException(XML_ELEMENT_NAME + ": Could not find node " + PerformanceAttributeEntry.PERFORMANCE_ATTRIBUTE_DELAY);
		}
		m_delay[dayOfWeek][hour] = new PerformanceAttributeEntry(elemDelay);
		
		Node elemSpeed = XMLUtil.getFirstChildByName(elemCurrentData, PerformanceAttributeEntry.PERFORMANCE_ATTRIBUTE_SPEED);
		if(elemSpeed == null)
		{
			throw new XMLParseException(XML_ELEMENT_NAME + ": Could not find node " + PerformanceAttributeEntry.PERFORMANCE_ATTRIBUTE_SPEED);
		}
		m_speed[dayOfWeek][hour] = new PerformanceAttributeEntry(elemSpeed);
		
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
	
	public void addDelay(long a_lDelay) 
	{
		PerformanceAttributeEntry entry = null;
		
		int dayOfWeek = m_cal.get(Calendar.DAY_OF_WEEK);
		int hour = m_cal.get(Calendar.HOUR_OF_DAY);
		
		entry = m_delay[dayOfWeek][hour];
		if(entry == null)
		{
			m_delay[dayOfWeek][hour] = entry = new PerformanceAttributeEntry(PerformanceAttributeEntry.PERFORMANCE_ATTRIBUTE_DELAY); 
		}
		
		entry.addValue(a_lDelay);
	}
	
	public void addSpeed(long a_lSpeed) 
	{
		PerformanceAttributeEntry entry = null;
		
		int dayOfWeek = m_cal.get(Calendar.DAY_OF_WEEK);
		int hour = m_cal.get(Calendar.HOUR_OF_DAY);
		
		entry = m_speed[dayOfWeek][hour];
		if(entry == null)
		{
			m_speed[dayOfWeek][hour] = entry = new PerformanceAttributeEntry(PerformanceAttributeEntry.PERFORMANCE_ATTRIBUTE_SPEED); 
		}
		
		entry.addValue(a_lSpeed);
	}
	
	public PerformanceAttributeEntry getCurrentSpeedEntry()
	{
		int dayOfWeek = m_cal.get(Calendar.DAY_OF_WEEK);
		int hour = m_cal.get(Calendar.HOUR_OF_DAY);
		
		return m_speed[dayOfWeek][hour];
	}
	
	public PerformanceAttributeEntry getCurrentDelayEntry()
	{
		int dayOfWeek = m_cal.get(Calendar.DAY_OF_WEEK);
		int hour = m_cal.get(Calendar.HOUR_OF_DAY);
		
		return m_delay[dayOfWeek][hour];		
	}
	
	public void overrideDailyAverageSpeed(long a_lSpeed)
	{
		PerformanceAttributeEntry entry = null;
		
		int dayOfWeek = m_cal.get(Calendar.DAY_OF_WEEK);
		int hour = m_cal.get(Calendar.HOUR_OF_DAY);
		
		if(m_speed[dayOfWeek][hour] == null)
		{
			m_speed[dayOfWeek][hour] = entry = new PerformanceAttributeEntry(PerformanceAttributeEntry.PERFORMANCE_ATTRIBUTE_SPEED);
		}
		
		entry.overrideAverageValue(a_lSpeed);
	}
	
	public void overrideDailyAverageDelay(long a_lDelay)
	{
		PerformanceAttributeEntry entry = null;
		
		int dayOfWeek = m_cal.get(Calendar.DAY_OF_WEEK);
		int hour = m_cal.get(Calendar.HOUR_OF_DAY);
		
		if(m_delay[dayOfWeek][hour] == null)
		{
			m_delay[dayOfWeek][hour] = entry = new PerformanceAttributeEntry(PerformanceAttributeEntry.PERFORMANCE_ATTRIBUTE_DELAY);
		}
		
		entry.overrideAverageValue(a_lDelay);
	}
	
	public long getAverageSpeed()
	{
		int dayOfWeek = m_cal.get(Calendar.DAY_OF_WEEK);
		int hour = m_cal.get(Calendar.HOUR_OF_DAY);
		
		if(m_speed[dayOfWeek][hour] == null)
		{
			return -1;
		}
		else
		{
			return m_speed[dayOfWeek][hour].getAverageValue();
		}
	}
	
	public long getAverageDelay()
	{
		int dayOfWeek = m_cal.get(Calendar.DAY_OF_WEEK);
		int hour = m_cal.get(Calendar.HOUR_OF_DAY);
		
		if(m_delay[dayOfWeek][hour] == null)
		{
			return -1;
		}
		else
		{
			return m_delay[dayOfWeek][hour].getAverageValue();
		}
	}
	
	public boolean isInvalid()
	{
		return (getAverageSpeed() == -1 || getAverageDelay() == -1);
		
	}
	
	public Element toXmlElement(Document a_doc)
	{
		return toXmlElement(a_doc, false);
	}
	
	public Element toXmlElement(Document a_doc, boolean a_bDisplayWeeklyData)
	{
		Element elem = a_doc.createElement(XML_ELEMENT_NAME);
		XMLUtil.setAttribute(elem, XML_ATTR_ID, getId());
		
		Element elemCurrent = a_doc.createElement(XML_ELEMENT_CURRENT_DATA);

		Element elemDelay = getCurrentSpeedEntry().toXmlElement(a_doc);
		Element elemSpeed = getCurrentDelayEntry().toXmlElement(a_doc);
		
		elemCurrent.appendChild(elemDelay);
		elemCurrent.appendChild(elemSpeed);
		
		if(a_bDisplayWeeklyData)
		{
			Element elemWeeklyData = a_doc.createElement(XML_ELEMENT_WEEKLY_DATA);
		
			for(int i = 0; i < 7; i++)
			{
				Element elemDailyData = a_doc.createElement(XML_ELEMENT_DAILY_DATA);
				XMLUtil.setAttribute(elemDailyData, XML_ATTR_DAY, i);
				elemWeeklyData.appendChild(elemDailyData);
			
				for(int j = 0; j < 24; j++)
				{
					Element elemHourlyData = a_doc.createElement(XML_ELEMENT_HOURLY_DATA);
					XMLUtil.setAttribute(elemHourlyData, XML_ATTR_HOUR, j);
					Element e = m_speed[i][j] != null ? m_speed[i][j].toXmlElement(a_doc) : null;
					Element f = m_delay[i][j] != null ? m_delay[i][j].toXmlElement(a_doc) : null;
				
					if(e != null)
					{
						elemHourlyData.appendChild(e);
					}
				
					if(f != null)
					{
						elemHourlyData.appendChild(f);
					}
				
					elemDailyData.appendChild(elemHourlyData);
				}
			}
			
			elem.appendChild(elemWeeklyData);
		}
		
		elem.appendChild(elemCurrent);
		
		return elem;
	}

	class PerformanceAttributeEntry implements IXMLEncodable
	{
		public static final String PERFORMANCE_ATTRIBUTE_DELAY = "Delay";
		public static final String PERFORMANCE_ATTRIBUTE_SPEED = "Speed";
		
		public static final String XML_ATTR_MIN = "Min";
		public static final String XML_ATTR_MAX = "Max";
		public static final String XML_ATTR_AVERAGE = "Average";
		
		public static final String XML_ELEMENT_VALUES = "Values";
		public static final String XML_ELEMENT_VALUE = "Value";
		
		private String m;
		
		private long m_lMaxValue;
		private long m_lMinValue;
		private long m_lAverageValue;
		
		private Vector m_Values = new Vector();
		
		public PerformanceAttributeEntry(String a)
		{
			m = a;
		}
		
		public PerformanceAttributeEntry(Node a_node)
		{
			m = XMLUtil.parseValue(a_node, "UnknownAttribute");
			
			m_lMinValue = XMLUtil.parseAttribute(a_node, XML_ATTR_MIN, 0);
			m_lMaxValue = XMLUtil.parseAttribute(a_node, XML_ATTR_MAX, 0);
			m_lAverageValue = XMLUtil.parseAttribute(a_node, XML_ATTR_AVERAGE, 0);
		}
		
		public void addValue(long a_lValue)
		{
			if (a_lValue <= 0)
			{
				return;
			}
			
			m_Values.add(new Long(a_lValue));
			
			long lValues = 0;
			for(int i = 0; i < m_Values.size(); i++)
			{
				lValues += ((Long) m_Values.elementAt(i)).longValue();
			}
			
			m_lAverageValue = lValues / m_Values.size();
			
			if(m_lMinValue == 0)
			{
				m_lMinValue = a_lValue;
			}
			else
			{
				m_lMinValue = Math.min(m_lMinValue, a_lValue);
			}
			
			m_lMaxValue = Math.max(m_lMaxValue, a_lValue);
			
			m_lastUpdate = System.currentTimeMillis();			
		}
		
		public long getAverageValue()
		{
			return m_lAverageValue;
		}
		
		public long getMinValue()
		{
			return m_lMinValue;
		}
		
		public long getMaxValue()
		{
			return m_lMaxValue;
		}
		
		public boolean isInvalid()
		{
			return m_lAverageValue == 0 || m_lAverageValue == -1;
		}
		
		public String getName()
		{
			return m;
		}
		
		public void overrideAverageValue(long a_lValue)
		{
			m_lAverageValue = a_lValue;
		}
		
		public Element toXmlElement(Document a_doc)
		{
			return toXmlElement(a_doc, false);
		}
		
		public Element toXmlElement(Document a_doc, boolean a_bDisplayValues)
		{
			Element elem = a_doc.createElement(getName());
			
			XMLUtil.setAttribute(elem, XML_ATTR_MIN, m_lMinValue);
			XMLUtil.setAttribute(elem, XML_ATTR_MAX, m_lMaxValue);
			XMLUtil.setAttribute(elem, XML_ATTR_AVERAGE, m_lAverageValue);
			
			if(a_bDisplayValues)
			{
				Element elemValues = a_doc.createElement(XML_ELEMENT_VALUES);
			
				for(int i = 0; i < m_Values.size(); i++)
				{
					Element elemValue = a_doc.createElement(XML_ELEMENT_VALUE);
					XMLUtil.setValue(elemValue, ((Long) m_Values.elementAt(i)).longValue());
					elemValues.appendChild(elemValue);
				}
			
				elem.appendChild(elemValues);
			}
			
			return elem;
		}
	}
}
