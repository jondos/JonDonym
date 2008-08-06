package anon.infoservice;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
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
	private static final String XML_ATTR_ID = "id";
	private static final String XML_ATTR_TIME = "time";
	
	private static final String XML_ELEMENT_LAST_TEST = "LastTest";
	private static final String XML_ELEMENT_CURRENT_HOURLY_DATA = "CurrentHourlyData";

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
	
	private PerformanceAttributeCurrentEntry[] m_currentEntries = 
		new PerformanceAttributeCurrentEntry[] { 
			new PerformanceAttributeCurrentEntry(SPEED),
			new PerformanceAttributeCurrentEntry(DELAY),
			new PerformanceAttributeCurrentEntry(USERS) };
	
	private long m_lastTestAverage[] = new long[3];
	
	public static final int SPEED = 0;
	public static final int DELAY = 1;
	public static final int USERS = 2;
	
	public static final String[] ATTRIBUTES = new String[]{ "Speed", "Delay", "Users" };
	
	private static final int PERFORMANCE_ENTRY_TTL = 1000*60*60; // 1 hour
	
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
		
		m_current.setTime(new Date(System.currentTimeMillis()));
		
		Node elemDelay = XMLUtil.getFirstChildByName(elemCurrentData, ATTRIBUTES[DELAY]);
		/*
		if(elemDelay == null)
		{
			System.out.println(XMLUtil.toString(a_entry));
			throw new XMLParseException(XML_ELEMENT_NAME + ": Could not find node " + ATTRIBUTES[DELAY]);
		}*/
		m_currentEntries[DELAY] = new PerformanceAttributeCurrentEntry(DELAY, elemDelay);
		
		Node elemSpeed = XMLUtil.getFirstChildByName(elemCurrentData, ATTRIBUTES[SPEED]);
		/*
		if(elemSpeed == null)
		{
			throw new XMLParseException(XML_ELEMENT_NAME + ": Could not find node " + ATTRIBUTES[SPEED]);
		}*/
		m_currentEntries[SPEED] = new PerformanceAttributeCurrentEntry(SPEED, elemSpeed);
		
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
		cal.setTime(new Date(a_timestamp));
		
		int day = cal.get(Calendar.DAY_OF_WEEK);
		int hour = cal.get(Calendar.HOUR_OF_DAY);
		
		entry = m_entries[a_attribute][day][hour];
		if(entry == null)
		{
			m_entries[a_attribute][day][hour] = entry = new PerformanceAttributeEntry();
		}
		else
		{
			// check if entry is obsolete
			if(System.currentTimeMillis() - 
				getDayTimestamp(a_attribute, day) > 7 * 24 * 60 * 60 * 1000)
			{
				m_entries[a_attribute][day] = new PerformanceAttributeEntry[24];
				m_entries[a_attribute][day][hour] = entry = new PerformanceAttributeEntry();
			}
		}
		
		entry.addValue(a_timestamp, a_value);
		m_currentEntries[a_attribute].addValue(a_timestamp, a_value);
		
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
			cal.setTime(new Date(timestamp.longValue()));
			
			int day = cal.get(Calendar.DAY_OF_WEEK);
			int hour = cal.get(Calendar.HOUR_OF_DAY);
			
			entry = m_entries[a_attribute][day][hour];
			if(entry == null)
			{
				m_entries[a_attribute][day][hour] = entry = new PerformanceAttributeEntry();
			}
			else
			{
				// check if entry is obsolete
				if(System.currentTimeMillis() - 
					getDayTimestamp(a_attribute, day) > 7 * 24 * 60 * 60 * 1000)
				{
					m_entries[a_attribute][day] = new PerformanceAttributeEntry[24];
					m_entries[a_attribute][day][hour] = entry = new PerformanceAttributeEntry();
				}
			}	
			
			entry.addValue(timestamp.longValue(), value);
			
			m_currentEntries[a_attribute].addValue(timestamp.longValue(), value);
			
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
		m_current.setTime(new Date(System.currentTimeMillis()));
		int hour = m_current.get(Calendar.HOUR_OF_DAY);
		int dayOfWeek = m_current.get(Calendar.DAY_OF_WEEK);
		
		return m_entries[SPEED][dayOfWeek][hour];
	}
	
	public PerformanceAttributeEntry getCurrentDelayEntry()
	{
		m_current.setTime(new Date(System.currentTimeMillis()));
		int hour = m_current.get(Calendar.HOUR_OF_DAY);
		int dayOfWeek = m_current.get(Calendar.DAY_OF_WEEK);
		
		return m_entries[DELAY][dayOfWeek][hour];
	}
	
	public void overrideXMLAverage(int a_attribute, long a_lValue)
	{
		m_currentEntries[a_attribute].overrideXMLAverage(a_lValue);
	}
	
	public void overrideXMLStdDeviation(int a_attribute, double a_dValue)
	{
		m_currentEntries[a_attribute].overrideXMLStdDeviation(a_dValue);
	}
	
	public long getAverage(int a_attribute)
	{
		return m_currentEntries[a_attribute].getAverage();
	}
	
	public long getXMLAverage(int a_attribute)
	{
		return m_currentEntries[a_attribute].getXMLAverage();
	}
	
	public double getXMLStdDeviation(int a_attribute)
	{
		return m_currentEntries[a_attribute].getXMLStdDeviation();
	}
	
	public String delayToHTML(int day)
	{
		return toHTML(DELAY, "ms", day);
	}
	
	public String speedToHTML(int day)
	{
		return toHTML(SPEED, "kbit/s", day);
	}
	
	public String usersToHTML(int day)
	{
		return toHTML(USERS, "", day);
	}
	
	private long getDayTimestamp(int a_attribute, int a_dayOfWeek)
	{
		long timestamp = -1;
		
		for(int i = 0; i < 24; i++)
		{
			if(m_entries[a_attribute][a_dayOfWeek][i] == null)
			{
				continue;
			}
			
			timestamp = m_entries[a_attribute][a_dayOfWeek][i].getDayTimestamp();
			
			if(timestamp != -1)
			{
				break;
			}
		}
		
		return timestamp;
	}
	
	public String toHTML(int a_attribute, String a_unit, int a_selectedDay)
	{
		MixCascade cascade = (MixCascade) Database.getInstance(MixCascade.class).getEntryById(m_strCascadeId);
		
		String htmlData = 
				(cascade != null ? cascade.getName() : "") +
				"<h2>" + m_strCascadeId + "</h2>";
		
		m_current.setTime(new Date(System.currentTimeMillis()));
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
				htmlData += "<a href=\"/values/" + ATTRIBUTES[a_attribute].toLowerCase() + "/" + m_strCascadeId + "/" + cal.get(Calendar.DAY_OF_WEEK) + "\">" + 
				(cal.get(Calendar.DAY_OF_WEEK) == dayOfWeek ? "Today</a> " : date +"</a> | ");
			}
			
			cal.add(Calendar.DAY_OF_WEEK, 1);
		}
		
		htmlData += "<br /><br />" +
				"<table width=\"100%\">" +
				"<tr>" +
				"<th width=\"16%\">Hour</th>" +
				"<th>Average</th>" +
				"<th>Min</th>" +
				"<th>Max</th>" +
				"<th>Std. Deviation</th>" +
				//(a_selectedDay == dayOfWeek ? "<th>Last Test</th>" : "")+
				"<th>% Std. Deviation</th>" +
				"<th>Errors</th></tr>";

		long dayTimestamp = getDayTimestamp(a_attribute, a_selectedDay);
		
		for(int hour = 0; hour < 24; hour++)
		{
			htmlData += "<tr>" +
					"<td CLASS=\"name\">" + hour + ":00 - " + ((hour + 1) % 24) + ":00</td>";
			
			PerformanceAttributeEntry entry = m_entries[a_attribute][a_selectedDay][hour];
			
			Calendar calLastTest = Calendar.getInstance();
			calLastTest.setTime(new Date(m_lastTestTime));
			
			if(entry == null || System.currentTimeMillis() - dayTimestamp > 7 * 24 * 60 * 60 * 1000 )
			{
				htmlData += "<td colspan=\"6\" align=\"center\">No data available</td>";
			}
			else
			{
				NumberFormat format = NumberFormat.getInstance(Constants.LOCAL_FORMAT);
				format.setMaximumFractionDigits(2);
				format.setMinimumFractionDigits(2);
				
				htmlData += "<td>" + entry.getAverageValue() + " " + a_unit + "</td>" +
					"<td>" + entry.getMinValue() + " " + a_unit + "</td>" +
					"<td>" + entry.getMaxValue() + " " + a_unit + "</td>" +
					"<td>" + format.format(entry.getStdDeviation()) + " " + a_unit + "</td>";
				
				/*
				if(a_selectedDay == dayOfWeek)
				{
					if(hour == calLastTest.get(Calendar.HOUR_OF_DAY) && a_attribute == SPEED)
					{
						htmlData += "<td>" + m_lastTestAverage[SPEED] + " " + a_unit + "</td>";
					}
					else if(hour == calLastTest.get(Calendar.HOUR_OF_DAY) && a_attribute == DELAY)
					{
						htmlData += "<td>" + m_lastTestAverage[DELAY] + " " + a_unit + "</td>";
					}
					else if(hour == calLastTest.get(Calendar.HOUR_OF_DAY) && a_attribute == USERS)
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
				}*/
				
				htmlData += "<td>" + format.format(100.0 * entry.getStdDeviation() / (double)entry.getAverageValue()) + "%</td>";
				
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
		
		if(getCurrentDelayEntry() != null)
		{
			Element elemDelay = m_currentEntries[DELAY].toXmlElement(a_doc);
			elemCurrent.appendChild(elemDelay);
		}
		
		if(getCurrentSpeedEntry() != null)
		{
			Element elemSpeed = m_currentEntries[SPEED].toXmlElement(a_doc);
			elemCurrent.appendChild(elemSpeed);
		}
		
		elem.appendChild(elemLast);
		elem.appendChild(elemCurrent);
		
		return elem;
	}
	
	class PerformanceAttributeCurrentEntry implements IXMLEncodable
	{
		public static final String XML_ATTR_MIN = "min";
		public static final String XML_ATTR_MAX = "max";
		public static final String XML_ATTR_AVERAGE = "average";
		public static final String XML_ATTR_STD_DEVIATION = "stdDeviaton";
		
		public static final String XML_ELEMENT_VALUES = "Values";
		public static final String XML_ELEMENT_VALUE = "Value";
		
		public static final long DEFAULT_TIMEFRAME = 60 * 60 * 1000; // 60 minutes
		
		public long m_timeFrame;
		public int m_attribute;
		public long m_lastUpdate;
		
		private Hashtable m_Values = new Hashtable();
		
		public long m_lXMLAverageValue = -1;
		public double m_lXMLStdDeviation = 0;
		
		public PerformanceAttributeCurrentEntry(int a_attribute)
		{
			m_timeFrame = DEFAULT_TIMEFRAME;
			m_attribute = a_attribute;
		}
		
		public PerformanceAttributeCurrentEntry(int a_attribute, long a_timeFrame)
		{
			m_timeFrame = a_timeFrame;
			m_attribute = a_attribute;
		}
		
		public PerformanceAttributeCurrentEntry(int a_attribute, Node a_node)
		{
			m_timeFrame = DEFAULT_TIMEFRAME;
			m_attribute = a_attribute;
			
			m_lXMLAverageValue = XMLUtil.parseAttribute(a_node, XML_ATTR_AVERAGE, -1);
			m_lXMLStdDeviation = XMLUtil.parseAttribute(a_node, XML_ATTR_STD_DEVIATION, 0.0d);
		}
		
		public void addValue(long a_lTimeStamp, long a_lValue)
		{
			if(System.currentTimeMillis() - a_lTimeStamp > m_timeFrame)
			{
				return;
			}
			
			Long timestamp;
			
			Enumeration e = m_Values.keys();
			
			m_Values.put(new Long(a_lTimeStamp), new Long(a_lValue));
			
			while(e.hasMoreElements())
			{
				timestamp = (Long) e.nextElement();
				if(System.currentTimeMillis() - timestamp.longValue() > m_timeFrame)
				{
					m_Values.remove(timestamp);
				}
			}
		}
		
		public void overrideXMLAverage(long a_lValue)
		{
			m_lXMLAverageValue = a_lValue;
		}
		
		public void overrideXMLStdDeviation(double a_dValue)
		{
			m_lXMLStdDeviation = a_dValue;
		}
		
		public long getXMLAverage()
		{
			return m_lXMLAverageValue;
		}
		
		public double getXMLStdDeviation()
		{
			return m_lXMLStdDeviation;
		}
		
		public long getAverage()
		{
			long values = 0;
			long value = 0;
			long lAverageValue = 0;
			long errors = 0;
			Long timestamp;
			
			Enumeration e = m_Values.keys();
			
			while(e.hasMoreElements())
			{
				timestamp = (Long) e.nextElement();
				if(System.currentTimeMillis() - timestamp.longValue() > m_timeFrame)
				{
					continue;
				}
				
				value = ((Long) m_Values.get(timestamp)).longValue();
				
				if(value < 0)
				{
					errors++;
				}
				else
				{
					values++;
					lAverageValue += value;
				}
			}
			
			// only errors
			if(errors > 0 && values == 0)
			{
				return -1;
			}
			
			if(values == 0)
			{
				return 0;
			}
			else
			{
				return lAverageValue / values;
			}
		}
		
		public double getStdDeviation()
		{
			long values = 0;
			long value = 0;
			long errors = 0;
			long mseValue = 0;
			Long timestamp;
			
			Enumeration e = m_Values.keys();
			
			while(e.hasMoreElements())
			{
				timestamp = (Long) e.nextElement();
				if(System.currentTimeMillis() - timestamp.longValue() > m_timeFrame)
				{
					continue;
				}
				
				value = ((Long) m_Values.get(timestamp)).longValue();
				
				if(value < 0)
				{
					errors++;
				}
				else
				{
					values++;
					mseValue += Math.pow(value - getAverage(), 2);
				}
			}
			
			if(errors > 0 && values == 0)
			{
				return -1;
			}
			
			if(values == 0)
			{
				return 0;
			}
			else
			{
				mseValue /= m_Values.size();
				// standard deviation
				return Math.sqrt(mseValue);
			}
		}
		
		public Element toXmlElement(Document a_doc)
		{
			Element elem = a_doc.createElement(ATTRIBUTES[m_attribute]);
			
			XMLUtil.setAttribute(elem, XML_ATTR_AVERAGE, getAverage());
			XMLUtil.setAttribute(elem, XML_ATTR_STD_DEVIATION, getStdDeviation());
			
			return elem;
		}
	}

	class PerformanceAttributeEntry
	{
		private long m_lMaxValue = -1;
		private long m_lMinValue = -1;
		private long m_lAverageValue = -1;
		private double m_lStdDeviation = 0.0;
		
		private long m_lastUpdate = -1;
		
		private Hashtable m_Values = new Hashtable();
		
		private int m_iErrors = 0;
		
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
		
		public long getDayTimestamp()
		{
			Calendar cal = Calendar.getInstance();
			cal.setTime(new Date(m_lastUpdate));
			
			return m_lastUpdate - cal.get(Calendar.HOUR_OF_DAY) * 60 * 60 * 1000 -
				cal.get(Calendar.MINUTE) * 60 * 1000 - cal.get(Calendar.SECOND) * 1000 -
				cal.get(Calendar.MILLISECOND); 
		}
	}
}
