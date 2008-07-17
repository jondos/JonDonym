package anon.infoservice;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Enumeration;
import java.util.Hashtable;
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
	private static final String XML_ATTR_ID = "id";
	private static final String XML_ATTR_TIME = "time";
	
	private static final String XML_ELEMENT_LAST_TEST = "LastTest";
	private static final String XML_ELEMENT_CURRENT_HOURLY_DATA = "CurrentHourlyData";
	private static final String XML_ELEMENT_HOURLY_DATA = "HourlyData";
	private static final String XML_ELEMENT_DAILY_DATA = "DailyData";
	private static final String XML_ELEMENT_WEEKLY_DATA = "WeeklyData";

	public static final String XML_ELEMENT_CONTAINER_NAME = "PerformanceInfo";
	public static final String XML_ELEMENT_NAME = "PerformanceEntry";	
	
	public static final long LAST_TEST_DATA_TTL = 20 * 60 * 1000;
	
	private String m_strCascadeId;
	private Calendar m_current = Calendar.getInstance();
	
	private long m_lastUpdate;
	private long m_serial;
	
	private long m_lastTestTime;
	
	private PerformanceAttributeEntry[][][] m_entries =
		new PerformanceAttributeEntry[ATTRIBUTES.length][8][24];
	
	private long m_lastTestAverage[] = new long[3];
	
	public static final int SPEED = 0;
	public static final int DELAY = 1;
	public static final int USERS = 2;
	
	public static final String[] ATTRIBUTES = new String[]{ "Speed", "Delay", "Users" };
	
	public static final int PERFORMANCE_ENTRY_TTL = 1000*60*60; // 1 hour
	
	public PerformanceEntry(String a_strCascadeId)
	{	
		super(Long.MAX_VALUE);
		
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
		
		m_current.setTimeInMillis(System.currentTimeMillis());
		
		int dayOfWeek = m_current.get(Calendar.DAY_OF_WEEK);
		int hour = m_current.get(Calendar.HOUR_OF_DAY);
		
		Node elemDelay = XMLUtil.getFirstChildByName(elemCurrentData, ATTRIBUTES[DELAY]);
		if(elemDelay == null)
		{
			throw new XMLParseException(XML_ELEMENT_NAME + ": Could not find node " + ATTRIBUTES[DELAY]);
		}
		m_entries[DELAY][dayOfWeek][hour] = new PerformanceAttributeEntry(elemDelay);
		
		Node elemSpeed = XMLUtil.getFirstChildByName(elemCurrentData, ATTRIBUTES[SPEED]);
		if(elemSpeed == null)
		{
			throw new XMLParseException(XML_ELEMENT_NAME + ": Could not find node " + ATTRIBUTES[SPEED]);
		}
		m_entries[SPEED][dayOfWeek][hour] = new PerformanceAttributeEntry(elemSpeed);
		
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
	
	public void importValue(int a_attribute, long a_timestamp, long a_value)
	{
		if(System.currentTimeMillis() - a_timestamp > 7 * 24 * 60 * 60 * 1000 ||
			a_timestamp > System.currentTimeMillis())
		{
			return;
		}
		
		PerformanceAttributeEntry entry = null;
		
		Calendar cal = Calendar.getInstance();
		cal.setTimeInMillis(a_timestamp);
		
		int day = cal.get(Calendar.DAY_OF_WEEK);
		int hour = cal.get(Calendar.HOUR_OF_DAY);
		
		entry = m_entries[a_attribute][day][hour];
		if(entry == null)
		{
			m_entries[a_attribute][day][hour] = entry = new PerformanceAttributeEntry(a_attribute);
		}
		else
		{
			// check if entry is obsolete
			if(System.currentTimeMillis() - 
				m_entries[a_attribute][day][hour].getDayTimestamp() > 7 * 24 * 60 * 60 * 1000)
			{
				m_entries[a_attribute][day][hour] = entry = new PerformanceAttributeEntry(a_attribute);
			}
		}
		
		entry.addValue(a_timestamp, a_value);
		
		m_lastUpdate = a_timestamp;
	}
	
	public long addData(int a_attribute, Hashtable a_data) 
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
			
			if(System.currentTimeMillis() - timestamp.longValue() > 7 * 24 * 60 * 60 * 1000)
			{
				continue;
			}
			
			Calendar cal = Calendar.getInstance();
			cal.setTimeInMillis(timestamp.longValue());
			
			int day = cal.get(Calendar.DAY_OF_WEEK);
			int hour = cal.get(Calendar.HOUR_OF_DAY);
			
			entry = m_entries[a_attribute][day][hour];
			if(entry == null)
			{
				m_entries[a_attribute][day][hour] = entry = new PerformanceAttributeEntry(a_attribute);
			}
			else
			{
				// check if entry is obsolete
				if(System.currentTimeMillis() - 
					m_entries[a_attribute][day][hour].getDayTimestamp() > 7 * 24 * 60 * 60 * 1000)
				{
					m_entries[a_attribute][day][hour] = entry = new PerformanceAttributeEntry(a_attribute);
				}
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
		m_lastTestAverage[a_attribute] = lAverageFromLastTest;
		m_lastUpdate = timestamp.longValue();
		
		return lAverageFromLastTest;
	}

	public long getLastTestAverage(int a_attribute)
	{
		return m_lastTestAverage[a_attribute];
	}
	
	public PerformanceAttributeEntry getCurrentSpeedEntry()
	{
		m_current.setTimeInMillis(System.currentTimeMillis());
		int hour = m_current.get(Calendar.HOUR_OF_DAY);
		int dayOfWeek = m_current.get(Calendar.DAY_OF_WEEK);
		
		return m_entries[SPEED][dayOfWeek][hour];
	}
	
	public PerformanceAttributeEntry getCurrentDelayEntry()
	{
		m_current.setTimeInMillis(System.currentTimeMillis());
		int hour = m_current.get(Calendar.HOUR_OF_DAY);
		int dayOfWeek = m_current.get(Calendar.DAY_OF_WEEK);
		
		return m_entries[DELAY][dayOfWeek][hour];
	}
	
	public void overrideDailyAverage(int a_attribute, long a_lValue)
	{
		m_current.setTimeInMillis(System.currentTimeMillis());
		int hour = m_current.get(Calendar.HOUR_OF_DAY);
		int dayOfWeek = m_current.get(Calendar.DAY_OF_WEEK);
		
		PerformanceAttributeEntry entry = null;
		
		if(m_entries[a_attribute][dayOfWeek][hour] == null)
		{
			m_entries[a_attribute][dayOfWeek][hour] = entry = new PerformanceAttributeEntry(a_attribute);
		}
		
		entry.overrideAverageValue(a_lValue);
	}
	
	public long getAverage(int a_attribute)
	{
		m_current.setTimeInMillis(System.currentTimeMillis());
		int hour = m_current.get(Calendar.HOUR_OF_DAY);
		int dayOfWeek = m_current.get(Calendar.DAY_OF_WEEK);
		
		if(m_entries[a_attribute][dayOfWeek][hour] == null)
		{
			return 0;
		}
		else
		{
			return m_entries[a_attribute][dayOfWeek][hour].getAverageValue();
		}
	}
	
	public boolean isInvalid()
	{
		return (getAverage(SPEED) == -1 || getAverage(DELAY) == -1);
	}
	
	public String delayToHTML(int day)
	{
		return toHTML(m_entries[DELAY], "ms", day);
	}
	
	public String speedToHTML(int day)
	{
		return toHTML(m_entries[SPEED], "kbits/sec", day);
	}
	
	public String usersToHTML(int day)
	{
		return toHTML(m_entries[USERS], "", day);
	}
	
	private long getDayTimestamp(PerformanceAttributeEntry[][] a_entries, int a_dayOfWeek)
	{
		long timestamp = -1;
		
		for(int i = 0; i < 24; i++)
		{
			if(a_entries[a_dayOfWeek][i] == null)
			{
				continue;
			}
			
			timestamp = a_entries[a_dayOfWeek][i].getDayTimestamp();
			
			if(timestamp != -1)
			{
				break;
			}
		}
		
		return timestamp;
	}
	
	public String toHTML(PerformanceAttributeEntry[][] a_entries, String a_unit, int a_selectedDay)
	{
		MixCascade cascade = (MixCascade) Database.getInstance(MixCascade.class).getEntryById(m_strCascadeId);
		
		String htmlData = 
				(cascade != null ? cascade.getName() : "") +
				"<h2>" + m_strCascadeId + "</h2>";
		
		// TODO: improve
		String e = "";
		if(a_entries == m_entries[SPEED])
		{
			e = "speed";
		}
		else if(a_entries == m_entries[DELAY])
		{
			e = "delay";
		}
		else if(a_entries == m_entries[USERS])
		{
			e = "users";
		}
		
		m_current.setTimeInMillis(System.currentTimeMillis());
		int dayOfWeek = m_current.get(Calendar.DAY_OF_WEEK);
		
		Calendar cal = Calendar.getInstance();
		cal.add(Calendar.DAY_OF_WEEK, -6);
		
		for(int i = 1; i <= 7; i++)
		{
			SimpleDateFormat df = new SimpleDateFormat("E yyyy-MM-dd");
			String date = df.format(cal.getTime());
			
			if(cal.get(Calendar.DAY_OF_WEEK) == a_selectedDay)
			{
				htmlData += "<b> " + (cal.get(Calendar.DAY_OF_WEEK) == dayOfWeek ? "Today</b> " : date + "</b> | "); 
			}
			else
			{
				htmlData += "<a href=\"/values/" + e + "/" + m_strCascadeId + "/" + cal.get(Calendar.DAY_OF_WEEK) + "\">" + 
				(cal.get(Calendar.DAY_OF_WEEK) == dayOfWeek ? "Today</a> " : date +"</a> | ");
			}
			
			cal.add(Calendar.DAY_OF_WEEK, 1);
		}
		
		htmlData += "<br /><br />" +
				"<table width=\"60%\">" +
				"<tr>" +
				"<th width=\"16%\">Hour</th>" +
				"<th>Average</th>" +
				"<th>Min</th>" +
				"<th>Max</th>" +
				"<th>Std. Deviation</th>" +
				(a_selectedDay == dayOfWeek ? "<th>Last Test</th>" : "")+
				"<th>Errors</th></tr>";

		long dayTimestamp = getDayTimestamp(a_entries, a_selectedDay);
		
		for(int hour = 0; hour < 24; hour++)
		{
			htmlData += "<tr>" +
					"<td CLASS=\"name\">" + hour + ":00 - " + ((hour + 1) % 24) + ":00</td>";
			
			PerformanceAttributeEntry entry = a_entries[a_selectedDay][hour];
			
			Calendar calLastTest = Calendar.getInstance();
			calLastTest.setTimeInMillis(m_lastTestTime);
			
			if(entry == null || System.currentTimeMillis() - dayTimestamp > 7 * 24 * 60 * 60 * 1000 )
			{
				htmlData += "<td colspan=\"6\" align=\"center\">No data available</td>";
			}
			else
			{
				htmlData += "<td>" + entry.getAverageValue() + " " + a_unit + "</td>" +
					"<td>" + entry.getMinValue() + " " + a_unit + "</td>" +
					"<td>" + entry.getMaxValue() + " " + a_unit + "</td>" +
					"<td>" + NumberFormat.getInstance(Constants.LOCAL_FORMAT).format(entry.getStdDeviation()) + " " + a_unit + "</td>";
				
				if(a_selectedDay == dayOfWeek)
				{
					if(hour == calLastTest.get(Calendar.HOUR_OF_DAY) && a_entries == m_entries[SPEED])
					{
						htmlData += "<td>" + m_lastTestAverage[SPEED] + " " + a_unit + "</td>";
					}
					else if(hour == calLastTest.get(Calendar.HOUR_OF_DAY) && a_entries == m_entries[DELAY])
					{
						htmlData += "<td>" + m_lastTestAverage[DELAY] + " " + a_unit + "</td>";
					}
					else if(hour == calLastTest.get(Calendar.HOUR_OF_DAY) && a_entries == m_entries[USERS])
					{
						htmlData += "<td>" + m_lastTestAverage[USERS] + " " + a_unit + "</td>";
					}
					else if(hour == m_current.get(Calendar.HOUR_OF_DAY))
					{
						htmlData += "<td>No test since IS startup</td>";
					}
					else
					{
						htmlData += "<td></td>";
					}
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
		
		for(int i = 0; i < ATTRIBUTES.length; i++)
		{
			XMLUtil.setAttribute(elemLast, ATTRIBUTES[i], m_lastTestAverage[i]);
		}
		
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
					Element e = m_entries[ATTRIBUTE_SPEED][i][j] != null ? m_entries[ATTRIBUTE_SPEED][i][j].toXmlElement(a_doc) : null;
					Element f = m_entries[ATTRIBUTE_DELAY][i][j] != null ? m_entries[ATTRIBUTE_DELAY][i][j].toXmlElement(a_doc) : null;
					
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
		public static final String XML_ATTR_MIN = "min";
		public static final String XML_ATTR_MAX = "max";
		public static final String XML_ATTR_AVERAGE = "average";
		public static final String XML_ATTR_STD_DEVIATION = "stdDeviaton";
		
		public static final String XML_ELEMENT_VALUES = "Values";
		public static final String XML_ELEMENT_VALUE = "Value";
		
		private int m_attribute = -1;
		
		private long m_lMaxValue = 0;
		private long m_lMinValue = 0;
		private long m_lAverageValue = 0;
		private double m_lStdDeviation = 0;
		
		private long m_lastUpdate = -1;
		
		private Hashtable m_Values = new Hashtable();
		
		private int m_iErrors = 0;
		
		public PerformanceAttributeEntry(int a_attribute)
		{
			m_attribute = a_attribute;
		}
		
		public PerformanceAttributeEntry(Node a_node)
		{
			m_lMinValue = XMLUtil.parseAttribute(a_node, XML_ATTR_MIN, 0);
			m_lMaxValue = XMLUtil.parseAttribute(a_node, XML_ATTR_MAX, 0);
			m_lAverageValue = XMLUtil.parseAttribute(a_node, XML_ATTR_AVERAGE, 0);
			m_lStdDeviation = XMLUtil.parseAttribute(a_node, XML_ATTR_STD_DEVIATION, 0);
		}
		
		public void addValue(long a_lTimeStamp, long a_lValue)
		{
			m_lastUpdate = a_lTimeStamp;
			
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
		
		public void overrideAverageValue(long a_lValue)
		{
			m_lAverageValue = a_lValue;
		}
		
		public long getDayTimestamp()
		{
			Calendar cal = Calendar.getInstance();
			cal.setTimeInMillis(m_lastUpdate);
				
			return m_lastUpdate - cal.get(Calendar.HOUR_OF_DAY) * 60 * 60 * 1000 -
				cal.get(Calendar.MINUTE) * 60 * 1000 - cal.get(Calendar.SECOND) * 1000 -
				cal.get(Calendar.MILLISECOND); 
		}
		
		public Element toXmlElement(Document a_doc)
		{
			Element elem = a_doc.createElement(ATTRIBUTES[m_attribute]);
			
			XMLUtil.setAttribute(elem, XML_ATTR_MIN, m_lMinValue);
			XMLUtil.setAttribute(elem, XML_ATTR_MAX, m_lMaxValue);
			XMLUtil.setAttribute(elem, XML_ATTR_AVERAGE, m_lAverageValue);
			XMLUtil.setAttribute(elem, XML_ATTR_STD_DEVIATION, m_lStdDeviation);
			
			return elem;
		}
	}
}
