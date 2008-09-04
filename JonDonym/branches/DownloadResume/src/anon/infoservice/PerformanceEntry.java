package anon.infoservice;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Calendar;
import java.util.Vector;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.Document;

import anon.util.Util.LongSortAsc;
import anon.util.Util.LongSortDesc;
import anon.util.Util;
import anon.util.XMLUtil;
import anon.util.XMLParseException;
import anon.util.IXMLEncodable;

public class PerformanceEntry extends AbstractDatabaseEntry implements IXMLEncodable
{
	public static final String XML_ELEMENT_CONTAINER_NAME = "PerformanceInfo";
	public static final String XML_ELEMENT_NAME = "PerformanceEntry";	
	
	public static final long LAST_TEST_DATA_TTL = 20 * 60 * 1000;
	
	public static final int SPEED = 0;
	public static final int DELAY = 1;
	public static final int USERS = 2;
	
	public static final long[][] BOUNDARIES = new long[][] { 
		{ 0, 100, 250, 500, 750, 1000, 1500, 2000 },
		{ 500, 750, 1000, 2000, 2500, 3000, 4000, Long.MAX_VALUE} };

	private static final String XML_ATTR_ID = "id";
	private static final String XML_ELEMENT_CURRENT_HOURLY_DATA = "Data";

	private static final String[] ATTRIBUTES = new String[]{ "Speed", "Delay", "Users" };
	
	private String m_strCascadeId;
	private Calendar m_current = Calendar.getInstance();
	
	private long m_lastUpdate;
	private long m_serial;
	
	private long m_lastTestTime;
	
	private PerformanceAttributeEntry[][][] m_entries =
		new PerformanceAttributeEntry[ATTRIBUTES.length][8][24];
	
	private PerformanceAttributeFloatingTimeEntry[] m_floatingTimeEntries;
	
	private long m_lastTestAverage[] = new long[3];
	
	private static final int PERFORMANCE_ENTRY_TTL = 1000*60*60; // 1 hour
	
	public PerformanceEntry(String a_strCascadeId)
	{
		this(a_strCascadeId, true);
	}
	
	public PerformanceEntry(String a_strCascadeId, boolean a_bInfoService)
	{	
		super(Long.MAX_VALUE);
		
		m_strCascadeId = a_strCascadeId;
		
		m_lastUpdate = System.currentTimeMillis();
		m_serial = System.currentTimeMillis();
		
		m_floatingTimeEntries = 
			new PerformanceAttributeFloatingTimeEntry[] { 
				new PerformanceAttributeFloatingTimeEntry(SPEED, a_bInfoService),
				new PerformanceAttributeFloatingTimeEntry(DELAY, a_bInfoService),
				new PerformanceAttributeFloatingTimeEntry(USERS, a_bInfoService) };
	}
	
	public PerformanceEntry(Element a_entry) throws XMLParseException
	{
		super(System.currentTimeMillis() + PERFORMANCE_ENTRY_TTL);
		
		m_floatingTimeEntries = new PerformanceAttributeFloatingTimeEntry[ATTRIBUTES.length];
		
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
		m_floatingTimeEntries[DELAY] = new PerformanceAttributeFloatingTimeEntry(DELAY, elemDelay);
		
		Node elemSpeed = XMLUtil.getFirstChildByName(elemCurrentData, ATTRIBUTES[SPEED]);
		m_floatingTimeEntries[SPEED] = new PerformanceAttributeFloatingTimeEntry(SPEED, elemSpeed);
		
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
	
	/**
	 * Imports a value into the entry array.
	 * 
	 * @param a_attribute The performance attribute (speed/delay, etc.)
	 * @param a_timestamp The time stamp of the value
	 * @param a_value The value itself
	 */
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
		
		entry.addValue(a_timestamp, a_value);
		m_floatingTimeEntries[a_attribute].addValue(a_timestamp, a_value);
		
		m_lastUpdate = a_timestamp;
		
		// check if entries are obsolete
		for(int i = 0; i < 24; i++)
		{
			if(m_entries[a_attribute][day][i] == null)
			{
				continue;
			}
			
			if(System.currentTimeMillis() - 
					m_entries[a_attribute][day][i].getDayTimestamp() >= 7 * 24 * 60 * 60 * 1000)
			{
				m_entries[a_attribute][day][i] = null;
			}
		}
	}
	
	/**
	 * Adds a hashtable of values into the entry array.
	 * 
	 * @param a_attribute The performance attribute
	 * @param a_data The data hashtable
	 * @return The average value of the hashtable
	 */
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
			
			if(System.currentTimeMillis() - timestamp.longValue() >= 7 * 24 * 60 * 60 * 1000)
			{
				continue;
			}
			
			Calendar cal = Calendar.getInstance();
			cal.setTime(new Date(timestamp.longValue()));
			
			int day = cal.get(Calendar.DAY_OF_WEEK);
			int hour = cal.get(Calendar.HOUR_OF_DAY);

			// check if entries are obsolete
			for(int i = hour; i < 24; i++)
			{
				if(m_entries[a_attribute][day][i] == null)
				{
					continue;
				}
				
				if(System.currentTimeMillis() - 
						m_entries[a_attribute][day][i].getDayTimestamp() > 7 * 24 * 60 * 60 * 1000)
				{
					m_entries[a_attribute][day][i] = null;
				}
			}
			
			entry = m_entries[a_attribute][day][hour];
			if(entry == null)
			{
				m_entries[a_attribute][day][hour] = entry = new PerformanceAttributeEntry();
			}
			
			entry.addValue(timestamp.longValue(), value);
			
			m_floatingTimeEntries[a_attribute].addValue(timestamp.longValue(), value);
			
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

	/**
	 * Returns the average value from the last performed test.
	 * 
	 * @param a_attribute The performance attribute
	 * @return The average value from the last performed test
	 */
	public long getLastTestAverage(int a_attribute)
	{
		return m_lastTestAverage[a_attribute];
	}
	
	/**
	 * Sets the bound value. This should only be used by 
	 * the PerformanceInfo class in the JAP client.
	 * 
	 * @see anon.infoservice.PerformanceInfo#getLowestCommonBoundEntry(String)
	 * 
	 * @param a_attribute The performance attribute
	 * @param a_lValue The bound value
	 */
	public void setBound(int a_attribute, long a_lValue)
	{
		m_floatingTimeEntries[a_attribute].setBound(a_lValue);
	}
	
	/**
	 * Calculates (if used by the info service) and returns the
	 * bound value of the given attribute.
	 * 
	 * @param a_attribute The performance attribute
	 * @return The bound value of the given attribute
	 */
	public long getBound(int a_attribute)
	{
		if(a_attribute == SPEED)
		{
			return m_floatingTimeEntries[a_attribute].getBound(true);
		}
		else if(a_attribute == DELAY)
		{
			return m_floatingTimeEntries[a_attribute].getBound(false);
		}
		else
		{
			return 0;
		}
	}
	
	/**
	 * Returns the average value of the give attribute.
	 * 
	 * @param a_attribute The performance attribute
	 * @return The average value of given attribute
	 */
	public long getAverage(int a_attribute)
	{
		return m_floatingTimeEntries[a_attribute].getAverage();
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
				
				if(entry.getStdDeviation() == -1)
				{
					htmlData += "<td>0 %</td>";
				}
				else
				{
					htmlData += "<td>" + format.format(100.0 * entry.getStdDeviation() / (double)entry.getAverageValue()) + " %</td>";
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
		
		Element elemCurrent = a_doc.createElement(XML_ELEMENT_CURRENT_HOURLY_DATA);
		
		Element elemDelay = m_floatingTimeEntries[DELAY].toXmlElement(a_doc);
		elemCurrent.appendChild(elemDelay);
		
		Element elemSpeed = m_floatingTimeEntries[SPEED].toXmlElement(a_doc);
		elemCurrent.appendChild(elemSpeed);
		
		elem.appendChild(elemCurrent);
		
		return elem;
	}
	
	class PerformanceAttributeFloatingTimeEntry implements IXMLEncodable
	{
		public static final String XML_ATTR_MIN = "min";
		public static final String XML_ATTR_MAX = "max";
		public static final String XML_ATTR_BOUND = "bound";
		
		public static final String XML_ELEMENT_VALUES = "Values";
		public static final String XML_ELEMENT_VALUE = "Value";
		
		public static final long DEFAULT_TIMEFRAME = 60 * 60 * 1000; // 60 minutes
		
		public long m_timeFrame;
		public int m_attribute;
		public long m_lastUpdate;
		
		private Hashtable m_Values = new Hashtable();
		private long m_lBoundValue = -1;
		
		private boolean m_bInfoService; 
		
		public PerformanceAttributeFloatingTimeEntry(int a_attribute, boolean a_bInfoService)
		{
			m_timeFrame = DEFAULT_TIMEFRAME;
			m_attribute = a_attribute;
			m_bInfoService = a_bInfoService;
		}
		
		public PerformanceAttributeFloatingTimeEntry(int a_attribute, Node a_node)
		{
			m_timeFrame = DEFAULT_TIMEFRAME;
			m_attribute = a_attribute;
			m_bInfoService = false;
			
			m_lBoundValue = XMLUtil.parseAttribute(a_node, XML_ATTR_BOUND, -1l);
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
		
		public void setBound(long a_lValue)
		{
			// only allowed by the client
			if(!m_bInfoService)
			{
				m_lBoundValue = a_lValue;
			}
		}
		
		public long getBound(boolean a_bLow)
		{
			// if it is invoked by the client, just return the stored bound value
			if(!m_bInfoService)
			{
				return m_lBoundValue;
			}
			
			long values = 0;
			long errors = 0;
			Long timestamp;
			
			Vector vec = new Vector();
			synchronized (m_Values)
			{
				Enumeration e = m_Values.keys();
				
				while(e.hasMoreElements())
				{
					timestamp = (Long) e.nextElement();
					if(System.currentTimeMillis() - timestamp.longValue() > m_timeFrame)
					{
						continue;
					}
					
					Long value = ((Long) m_Values.get(timestamp));
					
					if(value.longValue() < 0)
					{
						errors++;
					}
					else
					{
						values++;
						vec.addElement(value);
					}
				}
			}
			
			if(values == 0 && errors > 0)
			{
				return -1;
			}
			else if(values == 0)
			{
				return 0;
			}
			
			if(a_bLow)
			{
				Util.sort(vec, new LongSortAsc());
			}
			else
			{
				Util.sort(vec, new LongSortDesc());
			}
			
			int limit = (int) Math.floor(vec.size() / 10);
			
			for(int i = 0; i < limit; i++)
			{
				vec.removeElementAt(i);
			}
			
			if(vec.elementAt(0) != null)
			{
				long value = ((Long) vec.elementAt(0)).longValue();
			
				if(a_bLow)
				{
					for(int i = BOUNDARIES[m_attribute].length -1 ; i >= 0; i--)
					{
						if(value >= BOUNDARIES[m_attribute][i])
						{
							return BOUNDARIES[m_attribute][i];
						}
					}
				}
				else
				{
					for(int i = 0; i < BOUNDARIES[m_attribute].length; i++)
					{
						if(value <= BOUNDARIES[m_attribute][i])
						{
							return BOUNDARIES[m_attribute][i];
						}
					}
					
					return BOUNDARIES[m_attribute][BOUNDARIES[m_attribute].length - 1];
				}
				
				return BOUNDARIES[m_attribute][0];
			}
			
			return -1;
		}
		
		public long getAverage()
		{
			// this method should only be invoked by the InfoService
			if(!m_bInfoService)
			{
				return -1;
			}
			
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
			// this method should only be invoked by the InfoService
			if(!m_bInfoService)
			{
				return -1;
			}
			
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
			
			if(this.m_attribute == SPEED)
			{
				XMLUtil.setAttribute(elem, XML_ATTR_BOUND, getBound(true));
			}
			else if(this.m_attribute == DELAY)
			{
				XMLUtil.setAttribute(elem, XML_ATTR_BOUND, getBound(false));
			}
			
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
