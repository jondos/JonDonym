package anon.infoservice;

import java.text.NumberFormat;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Calendar;
import java.util.Vector;

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
	private static final String XML_ATTR_ID = "id";
	private static final String XML_ATTR_TIME = "time";
	
	private static final String XML_ELEMENT_LAST_TEST = "LastTest";
	private static final String XML_ELEMENT_CURRENT_HOURLY_DATA = "CurrentHourlyData";
	private static final String XML_ELEMENT_HOURLY_DATA = "HourlyData";
	private static final String XML_ELEMENT_DAILY_DATA = "DailyData";
	private static final String XML_ELEMENT_WEEKLY_DATA = "WeeklyData";
		
	public static final long LAST_TEST_DATA_TTL = 20 * 60 * 1000;
	
	private String m_strCascadeId;
	private Calendar m_cal = Calendar.getInstance();
	
	private long m_lastUpdate;
	private long m_serial;
	
	private long m_lastTestTime;
	
	private PerformanceAttributeEntry[][] m_speed = new PerformanceAttributeEntry[7][24];
	private PerformanceAttributeEntry[][] m_delay = new PerformanceAttributeEntry[7][24];
	
	private long m_lastTestAverageSpeed = 0;
	private long m_lastTestAverageDelay = 0;
		
	public static final String XML_ELEMENT_CONTAINER_NAME = "PerformanceInfo";
	
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
		
		Node elemCurrentData = XMLUtil.getFirstChildByName(a_entry, XML_ELEMENT_CURRENT_HOURLY_DATA);
		if(elemCurrentData == null)
		{
			throw new XMLParseException(XML_ELEMENT_NAME + ": Could not find node " + XML_ELEMENT_CURRENT_HOURLY_DATA);
		}
		
		m_cal.setTimeInMillis(System.currentTimeMillis());
		
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
	
	public long getLastTestTime()
	{
		return m_lastTestTime;
	}
	
	public void addValue(String a_attributeName, PerformanceAttributeEntry[][] a_entries, long a_timestamp, long a_value)
	{
		PerformanceAttributeEntry entry = null;
		
		Calendar cal = Calendar.getInstance();
		cal.setTimeInMillis(a_timestamp);
		
		int dayOfWeek = cal.get(Calendar.DAY_OF_WEEK);
		int hour = cal.get(Calendar.HOUR_OF_DAY);
		
		entry = a_entries[dayOfWeek][hour];
		if(entry == null)
		{
			a_entries[dayOfWeek][hour] = entry = new PerformanceAttributeEntry(a_attributeName);
		}

		entry.addValue(a_timestamp, a_value);
		
		m_lastUpdate = a_timestamp;
	}
	
	public long addData(String a_attributeName, PerformanceAttributeEntry[][] a_entries, Hashtable a_data) 
	{
		PerformanceAttributeEntry entry = null;
		
		// this _SHOULD_ never become true
		if(a_data.isEmpty())
		{
			return -1;
		}
		
		long lAverageFromLastTest = -1;
		
		Long timestamp = null;
		Enumeration e = a_data.keys();
			
		int values = 0;
		
		while(e.hasMoreElements())
		{
			timestamp = (Long) e.nextElement();
			long value = ((Long) a_data.get(timestamp)).longValue();
			
			Calendar cal = Calendar.getInstance();
			cal.setTimeInMillis(timestamp.longValue());
			
			int dayOfWeek = cal.get(Calendar.DAY_OF_WEEK);
			int hour = cal.get(Calendar.HOUR_OF_DAY);
			
			entry = a_entries[dayOfWeek][hour];
			if(entry == null)
			{
				a_entries[dayOfWeek][hour] = entry = new PerformanceAttributeEntry(a_attributeName); 
			}
			
			entry.addValue(timestamp.longValue(), value);
			
			if(value > 0)
			{
				lAverageFromLastTest += value;
				values++;
			}
		}
		
		if(values > 0)
		{
			lAverageFromLastTest /= values;
		}
		
		m_lastTestTime = timestamp.longValue();
		m_lastUpdate = timestamp.longValue();
				
		return lAverageFromLastTest;
	}
	
	public long addSpeedData(Hashtable a_data) 
	{
		return m_lastTestAverageSpeed = addData(PerformanceAttributeEntry.PERFORMANCE_ATTRIBUTE_SPEED, m_speed, a_data);
	}
		
	public long addDelayData(Hashtable a_data)
	{
		return m_lastTestAverageDelay = addData(PerformanceAttributeEntry.PERFORMANCE_ATTRIBUTE_DELAY, m_delay, a_data);
	}
	
	public void addSpeedValue(long a_timestamp, long a_value)
	{
		addValue(PerformanceAttributeEntry.PERFORMANCE_ATTRIBUTE_SPEED, m_speed, a_timestamp, a_value);
	}
	
	public void addDelayValue(long a_timestamp, long a_value)
	{
		addValue(PerformanceAttributeEntry.PERFORMANCE_ATTRIBUTE_DELAY, m_delay, a_timestamp, a_value);
	}
	
	public long getDelayFromLastTest()
	{
		return m_lastTestAverageDelay;
	}
	
	public long getSpeedFromLastTest()
	{
		return m_lastTestAverageSpeed;
	}
	
	public PerformanceAttributeEntry getCurrentSpeedEntry()
	{
		m_cal.setTimeInMillis(System.currentTimeMillis());
		
		int dayOfWeek = m_cal.get(Calendar.DAY_OF_WEEK);
		int hour = m_cal.get(Calendar.HOUR_OF_DAY);
		
		return m_speed[dayOfWeek][hour];
	}
	
	public PerformanceAttributeEntry getCurrentDelayEntry()
	{
		m_cal.setTimeInMillis(System.currentTimeMillis());
		
		int dayOfWeek = m_cal.get(Calendar.DAY_OF_WEEK);
		int hour = m_cal.get(Calendar.HOUR_OF_DAY);
		
		return m_delay[dayOfWeek][hour];
	}
	
	public void overrideDailyAverageSpeed(long a_lSpeed)
	{
		m_cal.setTimeInMillis(System.currentTimeMillis());
		
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
		m_cal.setTimeInMillis(System.currentTimeMillis());
		
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
		m_cal.setTimeInMillis(System.currentTimeMillis());
		
		int dayOfWeek = m_cal.get(Calendar.DAY_OF_WEEK);
		int hour = m_cal.get(Calendar.HOUR_OF_DAY);
		
		if(m_speed[dayOfWeek][hour] == null)
		{
			return 0;
		}
		else
		{
			return m_speed[dayOfWeek][hour].getAverageValue();
		}
	}
	
	public long getAverageDelay()
	{
		m_cal.setTimeInMillis(System.currentTimeMillis());
		
		int dayOfWeek = m_cal.get(Calendar.DAY_OF_WEEK);
		int hour = m_cal.get(Calendar.HOUR_OF_DAY);
		
		if(m_delay[dayOfWeek][hour] == null)
		{
			return 0;
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
	
	public String delayToHTML()
	{
		return toHTML(m_delay, "ms");
	}
	
	public String speedToHTML()
	{
		return toHTML(m_speed, "kbit/sec");
	}
	
	public String toHTML(PerformanceAttributeEntry[][] a_entries, String a_unit)
	{
		MixCascade cascade = (MixCascade) Database.getInstance(MixCascade.class).getEntryById(m_strCascadeId);
		
		String htmlData = 
				(cascade != null ? cascade.getName() : "") +
				"<h2>" + m_strCascadeId + "</h2>" +
				"<table width=\"60%\">" +
				"<tr>" +
				"<th width=\"16%\">Hour</th>" +
				"<th>Average</th>" +
				"<th>Min</th>" +
				"<th>Max</th>" +
				"<th>Std. Deviation</th><th>Last Test</th><th>Errors</th></tr>";
		
		m_cal.setTimeInMillis(System.currentTimeMillis());
		
		int dayOfWeek = m_cal.get(Calendar.DAY_OF_WEEK);
		
		for(int hour = 0; hour < 24; hour++)
		{
			htmlData += "<tr>" +
					"<td CLASS=\"name\">" + hour + ":00 - " + ((hour + 1) % 24) + ":00</td>";
			
			PerformanceAttributeEntry entry = a_entries[dayOfWeek][hour];
			
			Calendar calLastTest = Calendar.getInstance();
			calLastTest.setTimeInMillis(m_lastTestTime);
			
			if(entry == null)
			{
				htmlData += "<td colspan=\"6\" align=\"center\">No data available</td>";
			}
			else
			{
				htmlData += "<td>" + entry.getAverageValue() + " " + a_unit + "</td>" +
					"<td>" + entry.getMinValue() + " " + a_unit + "</td>" +
					"<td>" + entry.getMaxValue() + " " + a_unit + "</td>" +
					"<td>" + NumberFormat.getInstance(Constants.LOCAL_FORMAT).format(entry.getStdDeviation()) + " " + a_unit + "</td>";
				
				if(hour == calLastTest.get(Calendar.HOUR_OF_DAY) && a_entries == m_speed)
				{
					htmlData += "<td>" + m_lastTestAverageSpeed + " " + a_unit + "</td>";
				}
				else if(hour == calLastTest.get(Calendar.HOUR_OF_DAY) && a_entries == m_delay)
				{
					htmlData += "<td>" + m_lastTestAverageDelay + " " + a_unit + "</td>";
				}
				else if(hour == m_cal.get(Calendar.HOUR_OF_DAY))
				{
					htmlData += "<td>No test since IS startup</td>";
				}
				else
				{
					htmlData += "<td></td>";
				}

				double errorPercentage = 0;
				
				if(entry.getValueSize() != 0)
				{
					errorPercentage = (double) entry.getErrors() / entry.getValueSize() * 100.0;
				}
				
				htmlData += "<td>" + entry.getErrors() + " / " + entry.getValueSize() + " (" + NumberFormat.getInstance(Constants.LOCAL_FORMAT).format(errorPercentage) +" %)</td>";				
			}
			
			htmlData += "</tr>";
		}
		
		htmlData += "</table>";
		
		return htmlData;
	}
	
	public Element toXmlElement(Document a_doc)
	{
		Element elem = a_doc.createElement(XML_ELEMENT_NAME);
		XMLUtil.setAttribute(elem, XML_ATTR_ID, getId());
		
		Element elemLast = a_doc.createElement(XML_ELEMENT_LAST_TEST);
		XMLUtil.setAttribute(elemLast, PerformanceAttributeEntry.PERFORMANCE_ATTRIBUTE_DELAY, m_lastTestAverageDelay);
		XMLUtil.setAttribute(elemLast, PerformanceAttributeEntry.PERFORMANCE_ATTRIBUTE_SPEED, m_lastTestAverageSpeed);
		XMLUtil.setAttribute(elemLast, XML_ATTR_TIME, m_lastTestTime);
		
		Element elemCurrent = a_doc.createElement(XML_ELEMENT_CURRENT_HOURLY_DATA);
		
		// TODO: no available data for this hour -> use another hour?
		if(getCurrentDelayEntry() != null)
		{
			Element elemDelay = getCurrentDelayEntry().toXmlElement(a_doc);
			elemCurrent.appendChild(elemDelay);
		}
		
		if(getCurrentSpeedEntry() != null)
		{
			Element elemSpeed = getCurrentSpeedEntry().toXmlElement(a_doc);
			elemCurrent.appendChild(elemSpeed);
		}
		
		/*if(a_bDisplayWeeklyData)
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
		}*/
		
		elem.appendChild(elemLast);
		elem.appendChild(elemCurrent);
		
		return elem;
	}

	class PerformanceAttributeEntry implements IXMLEncodable
	{
		public static final String PERFORMANCE_ATTRIBUTE_DELAY = "Delay";
		public static final String PERFORMANCE_ATTRIBUTE_SPEED = "Speed";
		
		public static final String XML_ATTR_MIN = "min";
		public static final String XML_ATTR_MAX = "max";
		public static final String XML_ATTR_AVERAGE = "average";
		public static final String XML_ATTR_STD_DEVIATION = "stdDeviaton";
		
		public static final String XML_ELEMENT_VALUES = "Values";
		public static final String XML_ELEMENT_VALUE = "Value";
		
		private String m_name;
		
		private long m_lMaxValue = 0;
		private long m_lMinValue = 0;
		private long m_lAverageValue = 0;
		private double m_lStdDeviation = 0;
		
		private Hashtable m_Values = new Hashtable();
		
		private int m_iErrors = 0;
		
		public PerformanceAttributeEntry(String a_name)
		{
			m_name = a_name;
		}
		
		public PerformanceAttributeEntry(Node a_node)
		{
			m_name = XMLUtil.parseValue(a_node, "UnknownAttribute");
			
			m_lMinValue = XMLUtil.parseAttribute(a_node, XML_ATTR_MIN, 0);
			m_lMaxValue = XMLUtil.parseAttribute(a_node, XML_ATTR_MAX, 0);
			m_lAverageValue = XMLUtil.parseAttribute(a_node, XML_ATTR_AVERAGE, 0);
			m_lStdDeviation = XMLUtil.parseAttribute(a_node, XML_ATTR_STD_DEVIATION, 0);
		}
		
		public void addValue(long a_lTimeStamp, long a_lValue)
		{
			if(a_lValue < 0)
			{
				m_iErrors++;
				
				// we have at least one error and no values
				if(m_Values.size() == 0)
				{
					m_lAverageValue = -1;
					m_lMinValue = -1;
					m_lMaxValue = -1;
					m_lStdDeviation = -1;
				}
				return;
			}
			
			m_Values.put(new Long(a_lTimeStamp), new Long(a_lValue));
			
			long lValues = 0;
			Enumeration e = m_Values.elements();
			while(e.hasMoreElements())
			{
				lValues += ((Long) e.nextElement()).longValue();
			}
			
			m_lAverageValue = lValues / m_Values.size();
			
			// mean squared error
			double mseValue = 0;
			e = m_Values.elements();
			while(e.hasMoreElements())
			{
				mseValue += Math.pow(((Long) e.nextElement()).longValue() - m_lAverageValue, 2);
			}
			
			mseValue /= m_Values.size();
			
			// standard deviation
			m_lStdDeviation = Math.sqrt(mseValue);
			
			if(m_lMinValue == 0 || m_lMinValue == -1)
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
		
		public double getStdDeviation()
		{
			return m_lStdDeviation;
		}
		
		public int getErrors()
		{
			return m_iErrors;
		}
		
		public int getValueSize()
		{
			return m_Values.size() + m_iErrors;
		}
		
		public String getName()
		{
			return m_name;
		}
		
		public void overrideAverageValue(long a_lValue)
		{
			m_lAverageValue = a_lValue;
		}
		
		public Element toXmlElement(Document a_doc)
		{
			Element elem = a_doc.createElement(getName());
			
			XMLUtil.setAttribute(elem, XML_ATTR_MIN, m_lMinValue);
			XMLUtil.setAttribute(elem, XML_ATTR_MAX, m_lMaxValue);
			XMLUtil.setAttribute(elem, XML_ATTR_AVERAGE, m_lAverageValue);
			XMLUtil.setAttribute(elem, XML_ATTR_STD_DEVIATION, m_lStdDeviation);
			
			return elem;
		}
	}
}
