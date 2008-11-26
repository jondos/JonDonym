/*
 Copyright (c) 2008 The JAP-Team, JonDos GmbH

 All rights reserved.

 Redistribution and use in source and binary forms, with or without modification, 
 are permitted provided that the following conditions are met:

    * Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
    * Redistributions in binary form must reproduce the above copyright notice,
      this list of conditions and the following disclaimer in the documentation and/or
      other materials provided with the distribution.
    * Neither the name of the University of Technology Dresden, Germany, nor the name of
      the JonDos GmbH, nor the names of their contributors may be used to endorse or
      promote products derived from this software without specific prior written permission.

 THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE REGENTS OR
 CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package anon.infoservice;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Calendar;
import java.util.Vector;

import logging.LogHolder;
import logging.LogType;
import logging.LogLevel;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.Document;

import anon.util.Util.IntegerSortAsc;
import anon.util.Util.IntegerSortDesc;
import anon.util.Util.LongSortAsc;
import anon.util.Util.LongSortDesc;
import anon.util.Util;
import anon.util.XMLUtil;
import anon.util.XMLParseException;
import anon.util.IXMLEncodable;

/**
 * A <code>PerformanceEntry</code> stores various performance-related data 
 * such as average speed or delay about a certain <code>MixCascade</code>.
 * 
 * @author Christian Banse
 */
public class PerformanceEntry extends AbstractDatabaseEntry implements IXMLEncodable
{
	public static final long WEEK_SEVEN_DAYS_TIMEOUT = 7 * 24 * 60 * 60 * 1000l;
	
	/**
	 * Remove the worst x% results.
	 */
	private static final double BOUND_ROUNDING = 0.15d;
	
	/**
	 * The entry's container XML element name.
	 */
	public static final String XML_ELEMENT_CONTAINER_NAME = "PerformanceInfo";

	/**
	 * The XML element name.
	 */
	public static final String XML_ELEMENT_NAME = "PerformanceEntry";
	
	/**
	 * The data XML element name.
	 */
	private static final String XML_ELEMENT_DATA = "Data";

	/**
	 * The cascade id attribute name.
	 */
	private static final String XML_ATTR_ID = "id";
	
	/**
	 * Time-to-live of the last-test-data display.
	 */
	public static final long LAST_TEST_DATA_TTL = 20 * 60 * 1000;
	
	/**
	 * Time-to-live of the performance entries.
	 */
	private static final int PERFORMANCE_ENTRY_TTL = 1000*60*60;
	
	/**
	 * Speed attribute.
	 */
	public static final int SPEED = 0;
	
	/**
	 * Delay attribute.
	 */
	public static final int DELAY = 1;
	
	/**
	 * Current users attribute.
	 */
	public static final int USERS = 2;
	
	/**
	 * Current users attribute.
	 */
	public static final int PACKETS = 3;

	/**
	 * Text representation of the performance attributes.
	 */
	private static final String[] ATTRIBUTES = new String[]{ "Speed", "Delay", "Users", "Packets"};
	
	/**
	 * The boundaries used to calculate the speed and delay bounds.
	 */
	public static final int[][] BOUNDARIES = new int[][] { 
		{ 0, 50, 100, 200, 300, 400, 500, 750, 1000, 1500 },
		{ 500, 750, 1000, 2000, 2500, 3000, 4000, 8000, Integer.MAX_VALUE },
		{ 0 },
		{ 0 }};
	
	/**
	 * The id of the associated cascade.
	 */
	private String m_strCascadeId;
	
	/**
	 * A calendar used for date calculations.
	 */
	private Calendar m_current = Calendar.getInstance();
	
	/**
	 * The time of the last update.
	 */
	private long m_lastUpdate;
	
	/**
	 * The serial of the database entry.
	 */
	private long m_serial;
	
	private boolean m_bPassive;
	
	/**
	 * The time of the last performed test.
	 */
	private long m_lastTestTime;
	
	private StabilityAttributes m_stabilityAttributes;
	
	/**
	 * The attribute entries.
	 */
	private PerformanceAttributeEntry[][][] m_entries =
		new PerformanceAttributeEntry[ATTRIBUTES.length][8][24];
	
	/**
	 * The floating time entry. This structure holds all attribute entries
	 * that are not older than a given time (usually 1 hour).
	 */
	private PerformanceAttributeFloatingTimeEntry[] m_floatingTimeEntries;
	
	/**
	 * The average entries of the last test.
	 */
	private int m_lastTestAverage[] = new int[4];
	
	/**
	 * Constructs a new <code>PerformanceEntry</code> for the given <code>MixCascade</code>.
	 * 
	 * @param a_strCascadeId The id of the mix cascade.
	 */
	public PerformanceEntry(String a_strCascadeId)
	{
		this(a_strCascadeId, false);
	}
	
	/**
	 * Constructs a new <code>PerformanceEntry</code> for the given <code>MixCascade</code>.
	 * 
	 * @param a_strCascadeId The id of the mix cascade.
	 * @param a_bInfoService Specifies if the caller of this method is an <code>InfoService</code> or the JAP client.
	 */
	public PerformanceEntry(String a_strCascadeId, boolean a_bPassive)
	{	
		super(Long.MAX_VALUE);
		
		m_strCascadeId = a_strCascadeId;
		
		m_lastUpdate = System.currentTimeMillis();
		m_serial = System.currentTimeMillis();
		m_bPassive = a_bPassive;
		
		m_floatingTimeEntries = 
			new PerformanceAttributeFloatingTimeEntry[] { 
				new PerformanceAttributeFloatingTimeEntry(SPEED, !a_bPassive),
				new PerformanceAttributeFloatingTimeEntry(DELAY, !a_bPassive),
				new PerformanceAttributeFloatingTimeEntry(USERS, !a_bPassive),
				new PerformanceAttributeFloatingTimeEntry(PACKETS, !a_bPassive)};
	}
	
	/**
	 * Constructs a new <code>PerformanceEntry</code> from its XML representation.
	 * 
	 * @param a_entry The XML data.
	 * @throws XMLParseException If the XML data is invalid.
	 */
	public PerformanceEntry(Element a_entry) throws XMLParseException
	{
		super(System.currentTimeMillis() + PERFORMANCE_ENTRY_TTL);
		
		// create a new floating time entry
		m_floatingTimeEntries = new PerformanceAttributeFloatingTimeEntry[ATTRIBUTES.length];
		
		XMLUtil.assertNodeName(a_entry, XML_ELEMENT_NAME);
		
		m_strCascadeId = XMLUtil.parseAttribute(a_entry, XML_ATTR_ID, "");
		
		if(m_strCascadeId == "")
		{
			throw new XMLParseException(XML_ELEMENT_NAME + ": invalid id");
		}
		
		Node elemCurrentData = XMLUtil.getFirstChildByName(a_entry, XML_ELEMENT_DATA);
		if(elemCurrentData == null)
		{
			throw new XMLParseException(XML_ELEMENT_NAME + ": Could not find node " + XML_ELEMENT_DATA);
		}
		
		m_current.setTime(new Date(System.currentTimeMillis()));
		
		// fill the floating time entry with the XML data
		Node elemDelay = XMLUtil.getFirstChildByName(elemCurrentData, ATTRIBUTES[DELAY]);
		m_floatingTimeEntries[DELAY] = new PerformanceAttributeFloatingTimeEntry(DELAY, elemDelay);
		
		// fill the floating time entry with the XML data
		Node elemSpeed = XMLUtil.getFirstChildByName(elemCurrentData, ATTRIBUTES[SPEED]);
		m_floatingTimeEntries[SPEED] = new PerformanceAttributeFloatingTimeEntry(SPEED, elemSpeed);
		
		Node elemStability = 
			XMLUtil.getFirstChildByName(elemCurrentData, StabilityAttributes.XML_ELEMENT_NAME);
		if (elemStability != null)
		{
			m_stabilityAttributes = new StabilityAttributes((Element)elemStability);
		}
		else
		{
			m_stabilityAttributes = new StabilityAttributes(0, 0, 0, 0);
		}
		
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
	
	/**
	 * Returns the time of the last test.
	 * 
	 * @return The time of the last test.
	 */
	public long getLastTestTime()
	{
		return m_lastTestTime;
	}
	
	/**
	 * Imports a value into the entry array.
	 * 
	 * @param a_attribute The performance attribute.
	 * @param a_timestamp The time stamp of the value.
	 * @param a_value The value itself.
	 */
	public PerformanceAttributeEntry importValue(int a_attribute, long a_timestamp, int a_value)
	{
		return addPerformanceAttributeEntry(a_attribute, a_timestamp, a_value, true);
	}
	
	public PerformanceEntry update(PerformanceEntry a_entry)
	{
		boolean bUpdated = false;
		
		if (!m_bPassive)
		{
			// not allowed
			return null;
		}
		
		for (int i = 0; i < ATTRIBUTES.length; i++)
		{
			setBound(i, a_entry.getBound(i));
			setBestBound(i, a_entry.getBestBound(i));
			if ((i == DELAY && getBound(i) > 0) || (i != DELAY && getBound(i) >= 0))
			{
				bUpdated = true;
			}
		}
		
		setStabilityAttributes(a_entry.getStabilityAttributes());
		
		if (bUpdated)
		{
			m_lastUpdate = System.currentTimeMillis();
		}
		
		return this;
	}
	
	public Vector updateHourlyPerformanceAttributeEntries(long a_timestamp)
	{
		if (!m_bPassive)
		{
			// not allowed
			return null;
		}
		
		Vector entries = new Vector();
		PerformanceAttributeEntry entry;
		int bound;
		
		for (int i = 0; i < ATTRIBUTES.length; i++)
		{			
			bound = getBound(i);
			if (i == SPEED && bound == Integer.MAX_VALUE)
			{
				bound = -1;
			}
			else if (i == DELAY && bound == 0)
			{
				bound = -1;
			}
			
			entry = addPerformanceAttributeEntry(i, a_timestamp, bound, false);
			if (entry != null)
			{
				entries.addElement(entry);
			}
		}
		
		return entries;
	}
	
	private PerformanceAttributeEntry addPerformanceAttributeEntry(
			int a_attribute, long a_timestamp, int a_value, boolean a_bImport)
	{
		PerformanceAttributeEntry entry = null;
		PerformanceAttributeEntry previousEntry;
		
		// if time stamp is older than 7 days, ignore it
		if (System.currentTimeMillis() - a_timestamp >= WEEK_SEVEN_DAYS_TIMEOUT)
		{
			return null;
		}
		
		if (a_timestamp > System.currentTimeMillis())
		{
			LogHolder.log(LogLevel.WARNING, LogType.MISC, 
					"Performance timestamp has future value and is ignored: " + 
					a_timestamp + " , current: " + System.currentTimeMillis());
			return null;
		}
		
		Calendar cal = Calendar.getInstance();
		cal.setTime(new Date(a_timestamp));
		
		int day = cal.get(Calendar.DAY_OF_WEEK);
		int hour = cal.get(Calendar.HOUR_OF_DAY);
		
		if (m_bPassive)
		{
			// passive mode
			if (hour > 0)
			{
				hour -= 1;
			}
			else if (day == Calendar.SUNDAY)
			{
				day = Calendar.SATURDAY;
				hour = 23;
			}
			else
			{
				day -= 1;
				hour = 23;
			}
		}

		// check if entries are obsolete
		for(int i = hour; i < 24; i++)
		{
			if (m_entries[a_attribute][day][i] == null)
			{
				continue;
			}
			
			// we've found at least one value in the entry that is older than 7 days
			if (System.currentTimeMillis() - 
					m_entries[a_attribute][day][i].getDayTimestamp() >= WEEK_SEVEN_DAYS_TIMEOUT)
			{
				// invalidate the whole entry
				m_entries[a_attribute][day][i] = null;
			}
		}
		
		// look for the attribute entry
		entry = m_entries[a_attribute][day][hour];
		// create one if necessary
		if (entry == null)
		{
			m_entries[a_attribute][day][hour] = entry = new PerformanceAttributeEntry(a_attribute, m_bPassive);
		}
		else if (m_bPassive)
		{
			// only one entry at a time is allowed per hour
			return null;
		}
		
		if (hour > 0)
		{
			previousEntry = m_entries[a_attribute][day][hour-1];
		}
		else if (day == Calendar.SUNDAY)
		{
			previousEntry = m_entries[a_attribute][Calendar.SATURDAY][23];
		}
		else
		{
			previousEntry = m_entries[a_attribute][day-1][23];
		}
		
		// add the value to the entry
		entry.addValue(a_timestamp, a_value, previousEntry);
		
		if (a_bImport)
		{
			if (!m_bPassive)
			{
				// add the value to the floating time entry
				m_floatingTimeEntries[a_attribute].addValue(a_timestamp, a_value);
			}
		}
		
		return entry;
	}
	
	/**
	 * Adds a hashtable of values into the entry array.
	 * 
	 * @param a_attribute The performance attribute.
	 * @param a_data The data hashtable.
	 * @return The average value of the hashtable.
	 */
	public int addData(int a_attribute, Hashtable a_data) 
	{	
		// this _SHOULD_ never become true
		if(a_data.isEmpty())
		{
			LogHolder.log(LogLevel.ALERT, LogType.MISC, "Empty performance data!");
			return -1;
		}
		
		int lAverageFromLastTest = 0;
		
		long timestamp;
		long lastTestTimestamp = -1;
		long lastUpdateTimestamp = -1;
		Enumeration enumKeys = a_data.keys();
		Vector vecKeys = new Vector();
		while (enumKeys.hasMoreElements())
		{
			vecKeys.addElement(enumKeys.nextElement());
		}
		Util.sort(vecKeys, new LongSortAsc());
		enumKeys = vecKeys.elements();
		
		int values = 0;
		
		// enumerate through the data hashtable
		while(enumKeys.hasMoreElements())
		{
			timestamp = ((Long) enumKeys.nextElement()).longValue();
			int value = ((Integer) a_data.get(new Long(timestamp))).intValue();
			
			if (addPerformanceAttributeEntry(a_attribute, timestamp, value, false) == null)
			{
				continue;
			}
			
			lastUpdateTimestamp = timestamp;
			
			// add the value to the floating time entry
			m_floatingTimeEntries[a_attribute].addValue(timestamp, value);
			
			// if the value is valid, consider it for the average value
			if(value > 0)
			{
				if (value < Integer.MAX_VALUE)
				{
					if (lAverageFromLastTest < 0)
					{
						lAverageFromLastTest = 0;
					}
					lAverageFromLastTest += value;
					values++;
					lastTestTimestamp = timestamp;
				}
			}
			else if (values == 0)
			{
				// this was an error
				if (lAverageFromLastTest == 0)
				{
					lAverageFromLastTest = -1;
				}
				lastTestTimestamp = timestamp;
			}
		}
		
		// calculate the average value from the last test
		if (values > 0)
		{
			lAverageFromLastTest /= values;
		}
		
		if (lastTestTimestamp >= 0)
		{
			m_lastTestTime = lastTestTimestamp;
			m_lastTestAverage[a_attribute] = lAverageFromLastTest;
		}
		
		if (lastUpdateTimestamp >= 0)
		{
			m_lastUpdate = lastUpdateTimestamp;
		}
		
		return lAverageFromLastTest;
	}

	/**
	 * Returns the average value from the last performed test.
	 * 
	 * @param a_attribute The performance attribute.
	 * @return The average value from the last performed test.
	 */
	public int getLastTestAverage(int a_attribute)
	{
		return m_lastTestAverage[a_attribute];
	}
	
	public void setStabilityAttributes(StabilityAttributes a_attributes)
	{
		m_stabilityAttributes = a_attributes;
	}
	
	/**
	 * Sets the bound value. This should only be used by 
	 * the PerformanceInfo class in the JAP client.
	 * 
	 * @see anon.infoservice.PerformanceInfo#getLowestCommonBoundEntry(String)
	 * 
	 * @param a_attribute The performance attribute.
	 * @param a_lValue The bound value.
	 */
	public void setBound(int a_attribute, int a_lValue)
	{
		m_floatingTimeEntries[a_attribute].setBound(a_lValue);
	}
	
	/**
	 * Sets the best bound value. This should only be used by 
	 * the PerformanceInfo class in the JAP client.
	 * 
	 * @see anon.infoservice.PerformanceInfo#getLowestCommonBoundEntry(String)
	 * 
	 * @param a_attribute The performance attribute.
	 * @param a_lValue The best bound value.
	 */
	public void setBestBound(int a_attribute, int a_lValue)
	{
		m_floatingTimeEntries[a_attribute].setBestBound(a_lValue);
	}
	
	/**
	 * Calculates (if used by the info service) and returns the
	 * bound value of the given attribute.
	 * 
	 * @param a_attribute The performance attribute.
	 * @return The bound value of the given attribute.
	 */
	public int getBound(int a_attribute)
	{
		return m_floatingTimeEntries[a_attribute].getBound();
	}
	
	/**
	 * Returns errors resets, unknown requests and total requests for floating entries.
	 * @return
	 */
	public StabilityAttributes getStabilityAttributes()
	{
		if (m_stabilityAttributes != null)
		{
			return m_stabilityAttributes;
		}
		
		StabilityAttributes attributes;
		synchronized (m_floatingTimeEntries[PACKETS].m_Values)
		{
			synchronized (m_floatingTimeEntries[SPEED].m_Values)
			{
				attributes = new StabilityAttributes(m_floatingTimeEntries[PACKETS].m_Values.size(), 
						m_floatingTimeEntries[SPEED].m_iUnknown, // speed values are the worst 
						m_floatingTimeEntries[SPEED].m_iErrors, // speed values are the worst
						m_floatingTimeEntries[PACKETS].m_iResets);
			}
		}
		return attributes;
	}
	
	/**
	 * Calculates (if used by the info service) and returns the
	 * best bound value of the given attribute.
	 * 
	 * @param a_attribute The performance attribute.
	 * @return The best bound value of the given attribute.
	 */
	public int getBestBound(int a_attribute)
	{
		return m_floatingTimeEntries[a_attribute].getBestBound();
	}
	
	/**
	 * Returns the average value of the give attribute.
	 * 
	 * @param a_attribute The performance attribute.
	 * @return The average value of given attribute.
	 */
	public int getAverage(int a_attribute)
	{
		return m_floatingTimeEntries[a_attribute].getAverage();
	}
	
	/**
	 * Returns the delay values of a specified day as HTML table.
	 * 
	 * @param day The day.
	 * @return The HTML table.
	 */
	public String delayToHTML(int day)
	{
		return toHTML(DELAY, "ms", day);
	}
	
	/**
	 * Returns the speed values of a specified day as HTML table.
	 * 
	 * @param day The day.
	 * @return The HTML table.
	 */
	public String speedToHTML(int day)
	{
		return toHTML(SPEED, "kbit/s", day);
	}
	
	/**
	 * Returns the users values of a specified day as HTML table.
	 * 
	 * @param day The day.
	 * @return The HTML table.
	 */
	public String usersToHTML(int day)
	{
		return toHTML(USERS, "", day);
	}
	
	/**
	 * Determines the day timestamp of the oldest entry of specified 
	 * performance attribute.
	 * 
	 * @param a_attribute The performance attribute.
	 * @param a_dayOfWeek The day of the week.
	 * @return The day timestamp.
	 */
	private long getDayTimestamp(int a_attribute, int a_dayOfWeek)
	{
		long timestamp = -1;
		
		// loop through all hours of the day
		for(int i = 0; i < 24; i++)
		{
			if(m_entries[a_attribute][a_dayOfWeek][i] == null)
			{
				continue;
			}
			
			// get the day timestamp of current hour - this actually makes sense ;-)
			timestamp = m_entries[a_attribute][a_dayOfWeek][i].getDayTimestamp();
			
			if(timestamp != -1)
			{
				break;
			}
		}
		
		return timestamp;
	}
	
	/**
	 * Constructs a HTML table of the specified performance attribute
	 * on the a certain day. 
	 * 
	 * @param a_attribute The performance attribute.
	 * @param a_unit The unit of the attribute.
	 * @param a_selectedDay The day.
	 * @return The HTML table.
	 */
	private String toHTML(int a_attribute, String a_unit, int a_selectedDay)
	{
		MixCascade cascade = (MixCascade) Database.getInstance(MixCascade.class).getEntryById(m_strCascadeId);
		
		String htmlData = (cascade != null ? cascade.getName() : "") + "<h2>" + m_strCascadeId + "</h2>";
		
		m_current.setTime(new Date(System.currentTimeMillis()));
		int dayOfWeek = m_current.get(Calendar.DAY_OF_WEEK);
		
		Calendar cal = Calendar.getInstance();
		cal.add(Calendar.DAY_OF_WEEK, -6);
		
		for (int i = 1; i <= 7; i++)
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
				"<th>Bound</th>" +
				"<th>% Std. Deviation</th>" +
				"<th>Err/Try/Total</th>" +
				"<th>Resets</th></tr>";

		//long dayTimestamp = getDayTimestamp(a_attribute, a_selectedDay);		
		
		//LogHolder.log(LogLevel.WARNING, LogType.MISC, "GOOD: Performance day: " + a_selectedDay + " timestamp: " + dayTimestamp + " ID: " + cascade.getId());
		
		for (int hour = 0; hour < 24; hour++)
		{
			htmlData += "<tr>" +
					"<td CLASS=\"name\">" + hour + ":00 - " + ((hour + 1) % 24) + ":00</td>";
			
			PerformanceAttributeEntry entry = m_entries[a_attribute][a_selectedDay][hour];
			long dayTimestamp = 0;
			if (entry != null)
			{
				dayTimestamp = entry.getDayTimestamp();
			}
			
			if (entry == null || System.currentTimeMillis() - dayTimestamp >= WEEK_SEVEN_DAYS_TIMEOUT)
			{
				/*
				if (entry != null)
				{
					LogHolder.log(LogLevel.WARNING, LogType.MISC, "BAD: Performance day: " + a_selectedDay + " timestamp: " + 
							dayTimestamp + (cascade != null ?  " ID: " + cascade.getId() : ""));
				}*/
				
				htmlData += "<td colspan=\"7\" align=\"center\">No data available</td>";
			}
			else
			{
				NumberFormat format = NumberFormat.getInstance(Constants.LOCAL_FORMAT);
				format.setMaximumFractionDigits(2);
				format.setMinimumFractionDigits(2);
				
				htmlData += "<td>" + entry.getAverageValue() + " " + a_unit + "</td>" +
					"<td>" + entry.getMinValue() + " " + a_unit + "</td>" +
					"<td>" + entry.getMaxValue() + " " + a_unit + "</td>" +
					"<td>";
				
				int bound = (entry == null ? -1 : entry.getBound());
			
				if (a_attribute == DELAY)
				{
					if(bound == Integer.MAX_VALUE)
					{
						htmlData += "> " + PerformanceEntry.BOUNDARIES[PerformanceEntry.DELAY][PerformanceEntry.BOUNDARIES[PerformanceEntry.DELAY].length - 2];
					}
					else if (bound <= 0)
					{
						htmlData += "?";
					}
					else
					{
						htmlData += bound;
					}
				}
				else if (a_attribute == SPEED)
				{
					if(bound == 0)
					{
						htmlData += "< " + PerformanceEntry.BOUNDARIES[PerformanceEntry.SPEED][1];
					}
					else if (bound < 0 || bound == Integer.MAX_VALUE)
					{
						htmlData += "?";
					}
					else
					{
						htmlData += bound;
					}
				}
				
				htmlData += " " + a_unit + "</td>";
								
				if(entry == null || entry.getStdDeviation() == -1 || entry.getAverageValue() == 0)
				{
					htmlData += "<td></td>";
				}
				else
				{
					htmlData += "<td>" + format.format(100.0 * entry.getStdDeviation() / (double)entry.getAverageValue()) + " %</td>";
				}
				
				double errorPercentage = 0;
				double tryPercentage = 0;
				
				if(entry != null && entry.getValueSize() != 0)
				{
					errorPercentage = (double) entry.getErrors() / entry.getValueSize() * 100.0;
					tryPercentage = (double) entry.getUnknown() / entry.getValueSize() * 100.0;
				}
				
				htmlData += "<td>" + entry.getErrors() + " / " + entry.getUnknown()  + " / " + entry.getValueSize() + " (" + NumberFormat.getInstance(Constants.LOCAL_FORMAT).format(errorPercentage) +" % / " + 
					NumberFormat.getInstance(Constants.LOCAL_FORMAT).format(tryPercentage) + " %)</td>";		
				
				entry = m_entries[PACKETS][a_selectedDay][hour];
				if (entry != null && entry.getResets() > 0)
				{
					htmlData += "<td>" + entry.getResets() + "</td>";									
				}
				else
				{
					htmlData += "<td></td>";
				}
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
		
		Element elemCurrent = a_doc.createElement(XML_ELEMENT_DATA);
		
		Element elemDelay = m_floatingTimeEntries[DELAY].toXmlElement(a_doc);
		elemCurrent.appendChild(elemDelay);
		
		Element elemSpeed = m_floatingTimeEntries[SPEED].toXmlElement(a_doc);
		elemCurrent.appendChild(elemSpeed);
		
		Element elemStability = getStabilityAttributes().toXmlElement(a_doc);
		elemCurrent.appendChild(elemStability);
		
		elem.appendChild(elemCurrent);
		
		return elem;
	}
	
	/**
	 * The floating time entry. This structure holds all attribute entries
	 * that are not older than a given time (usually 1 hour).
	 * 
	 * @author Christian Banse
	 */
	private static class PerformanceAttributeFloatingTimeEntry implements IXMLEncodable
	{
		/**
		 * The time frame of this floating time entry.
		 */
		public static final long DEFAULT_TIMEFRAME = 60 * 60 * 1000; // 60 minutes
		
		/**
		 * The containers XML element name.
		 */
		public static final String XML_ELEMENT_VALUES = "Values";
		
		/**
		 * The XML element name.
		 */
		public static final String XML_ELEMENT_VALUE = "Value";
		
		/**
		 * The best bound value XML attribute name.
		 */
		public static final String XML_ATTR_BEST = "best";
		
		/**
		 * The bound value XML attribute name.
		 */
		public static final String XML_ATTR_BOUND = "bound";
		
		/**
		 * The last value that has been added. This is only useful for PACKETS.
		 */
		private int m_iLastValue = -1;
		private long m_iLastTimestamp = -1;
		
		/**
		 * The performance attribute.
		 */
		public int m_attribute;
		
		/**
		 * The time of the last update.
		 */
		public long m_lastUpdate;
		
		/**
		 * The values.
		 */
		private Hashtable m_Values = new Hashtable();
		
		/**
		 * The bound value. This will only be set if this object
		 * is constructed from XML (only in the JAP client). The 
		 * info service calculates the bound value on the fly using
		 * {@link #getBound(boolean)}
		 */
		private int m_lBoundValue = -1;
		
		/**
		 * The best bound value. This will only be set if this object
		 * is constructed from XML (only in the JAP client). The 
		 * info service calculates the best bound value on the fly using
		 * {@link #getBound(boolean)}
		 */
		private int m_lBestBoundValue = -1;
				
		private int m_iResets = 0;
		private int m_iErrors = 0;
		private int m_iUnknown = 0;
		
		/**
		 * True, if the object is created by the info service 
		 * or the JAP client. Determines whether the bound value
		 * is retrieved from the stored value or calculated on the fly.
		 */
		private boolean m_bInfoService; 
		
		/**
		 * Constructs a new <code>PerformanceAttributeFloatingTimeEntry</code>.
		 * 
		 * @param a_attribute The performance attribute.
		 * @param a_bInfoService Specifies if the caller of this method is an <code>InfoService</code> or the JAP client.
		 */
		public PerformanceAttributeFloatingTimeEntry(int a_attribute, boolean a_bInfoService)
		{
			m_attribute = a_attribute;
			m_bInfoService = a_bInfoService;
		}
		
		/**
		 * Constructs a new <code>PerformanceAttributeFloatingTimeEntry</code> from XML data.
		 * 
		 * @param a_attribute The performance attribute.
		 * @param a_node The XML node.
		 */
		public PerformanceAttributeFloatingTimeEntry(int a_attribute, Node a_node)
		{
			m_attribute = a_attribute;
			// data comes from the XML data so this method is called by the JAP client
			m_bInfoService = false;
			
			long lBoundValue = XMLUtil.parseAttribute(a_node, XML_ATTR_BOUND, -1l);
			if (lBoundValue > Integer.MAX_VALUE)
			{
				m_lBoundValue = Integer.MAX_VALUE;
			}
			else
			{
				m_lBoundValue = (int) lBoundValue;
			}
			
			long lBestBoundValue = XMLUtil.parseAttribute(a_node, XML_ATTR_BEST, -2l);
			if(lBestBoundValue == -2)
			{
				if(a_attribute == SPEED)
				{
					m_lBestBoundValue = Integer.MAX_VALUE;
				}
				else
				{
					m_lBestBoundValue = 0;
				}
			}
			else
			{
				if (lBestBoundValue > Integer.MAX_VALUE)
				{	
					m_lBestBoundValue = Integer.MAX_VALUE;
				}
				else
				{
					m_lBestBoundValue = (int) lBestBoundValue;
				}
			}
		}
		
		/**
		 * Adds a value to the floating time entry.
		 * 
		 * @param a_lTimeStamp The timestamp of the value.
		 * @param a_lValue The value.
		 */
		public void addValue(long a_lTimeStamp, int a_lValue)
		{
			if (System.currentTimeMillis() - a_lTimeStamp > DEFAULT_TIMEFRAME)
			{
				return;
			}
			
			Long timestamp;
			
			synchronized (m_Values)
			{
				Enumeration e;
				Vector keysToDelete = new Vector();
				boolean bReset = false;
												
				if (a_lValue < 0 || a_lValue == Integer.MAX_VALUE)
				{
					if (a_lValue < 0)
					{
						m_iErrors++;
					}	
					else if (a_lValue == Integer.MAX_VALUE)
					{
						m_iUnknown++;
					}
				}
				else if (m_attribute == PACKETS)
				{				
					if (m_iLastTimestamp < a_lTimeStamp)
					{
						if (a_lValue < m_iLastValue)
						{
							// this service has been restarted since the last test
							m_iResets++;
							bReset = true;
						}
						m_iLastValue = a_lValue;
						m_iLastTimestamp = a_lTimeStamp;
					}
					else
					{
						LogHolder.log(LogLevel.WARNING, LogType.MISC, 
								"Unordered timestamps for floating PACKETS. " +
								"Timestamp new: " + a_lTimeStamp + " Timestamp old: " + m_iLastTimestamp +
								" Value: " + a_lValue);
					}
					if (bReset)
					{
						a_lValue = 1; // this is a reset; the concrete packets are not important
					}
					else
					{
						a_lValue = 0;
					}
				}
				
				m_Values.put(new Long(a_lTimeStamp), new Integer(a_lValue));
				
				
				// loop through all values
				e = m_Values.keys();
				while (e.hasMoreElements())
				{
					timestamp = (Long) e.nextElement();
					// value is too old, remove it
					if (System.currentTimeMillis() - timestamp.longValue() > DEFAULT_TIMEFRAME)
					{
						keysToDelete.addElement(timestamp);						
					}
				}
				for (int i = 0; i < keysToDelete.size(); i++)
				{
					// do not delete from enumeration, as this might give weird results!
					a_lValue = ((Integer)m_Values.get(keysToDelete.elementAt(i))).intValue();
					if (a_lValue < 0 || a_lValue == Integer.MAX_VALUE)
					{
						if (a_lValue < 0)
						{
							m_iErrors--;
						}	
						else if (a_lValue == Integer.MAX_VALUE)
						{
							m_iUnknown--;
						}
					}
					else if (m_attribute == PACKETS && a_lValue == 1)
					{
						m_iResets--;
					}
					m_Values.remove(keysToDelete.elementAt(i));
				}
				keysToDelete.removeAllElements();
			}
		}
		
		/**
		 * Sets the bound value. Only allowed by the client.
		 * 
		 * @param a_lValue The value.
		 */
		public void setBound(int a_lValue)
		{
			// only allowed by the client
			if(!m_bInfoService)
			{
				m_lBoundValue = a_lValue;
			}
		}
		
		/**
		 * Sets the best bound value. Only allowed by the client.
		 * 
		 * @param a_lValue The value.
		 */
		public void setBestBound(int a_lValue)
		{
			// only allowed by the client
			if(!m_bInfoService)
			{
				m_lBestBoundValue = a_lValue;
			}
		}
		
		/**
		 * Returns the bound value. If it is invoked by the client 
		 * the stored m_lBoundValue is returned otherwise the bound
		 * value is calculated from the values in the entry.
		 * 
		 * When invoked by an active InfoService, the bound is calculated
		 * be removing the BOUND_ROUNDING per cent (%) worst results. The
		 * remaining worst result is then compared to BOUNDARIES. The
		 * best value from BOUNDARIES, that is equal to or worse than
		 * the real worst value after removing the others is the bound.
		 * 
		 * In this form, the bound reacts quite fast on bad values, but 
		 * recovers very slow if the problems are gone and good values
		 * show up again. It is therefore 'tuned' as follows:
		 * 
		 * Look for the time of the last value that was equal to or worse than
		 * the next best bound compared to the bound calculated as above. 
		 * If there are at least BOUND_ROUNDING per cent (%) valid values 
		 * (errors and tries are not counted) left afterwards, calculate the 
		 * bound again but remove this and all previous values from the 
		 * calculation. This should honor the fact that some services quickly 
		 * recover from bad performance.
		 * 
		 * @return The bound value.
		 */
		public int getBound()
		{
			// if it is invoked by the client, just return the stored bound value
			if (!m_bInfoService)
			{
				return m_lBoundValue;
			}
			
			Integer value;
			int nextBound;
			Vector vecTimestamps = new Vector();
			Hashtable hashValues = (Hashtable)m_Values.clone();
			int bound = calculateBound(hashValues, vecTimestamps);
			int limit = (int) Math.floor((double)vecTimestamps.size() * BOUND_ROUNDING);
	
			
			Util.sort(vecTimestamps, new LongSortDesc()); // start with the latest timestamp
			for (int i = 0; i < vecTimestamps.size(); i++)
			{
				value = ((Integer)hashValues.get(vecTimestamps.elementAt(i))).intValue();
				if (value < 0 || value == Integer.MAX_VALUE)
				{
					// Do not count errors and tries.
					limit++;
					continue;
				}
				
				// find the next better bound
				nextBound = bound;
				if (m_attribute == DELAY)
				{
					for (int j = BOUNDARIES[m_attribute].length - 1; j >= 0; j--)
					{
						if (BOUNDARIES[m_attribute][j] == bound)
						{
							if (j > 0)
							{
								nextBound = BOUNDARIES[m_attribute][j - 1];
							}
								
							break;
						}
					}
				}
				else
				{
					for (int j = 0; j < BOUNDARIES[m_attribute].length; j++)
					{
						if (BOUNDARIES[m_attribute][j] == bound)
						{
							if (j + 1 < BOUNDARIES[m_attribute].length)
							{
								nextBound = BOUNDARIES[m_attribute][j + 1];
							}
							break;
						}
					}
				}
				
				if (nextBound != bound &&
					((m_attribute == SPEED && value <= nextBound) || (m_attribute == DELAY && value >= nextBound)))
				{
					// We have found the last value that supersedes this bound. Now check the limit!
					if (i >= limit)
					{	
						//LogHolder.log(LogLevel.WARNING, LogType.MISC, "Limit: " + limit +  " Old bound: " +
							//	bound + " Next bound: " + nextBound);
						
						// remove all values worse than or equal to the current bound 
						for (int j = i; j < vecTimestamps.size(); j++)
						{
							hashValues.remove(vecTimestamps.elementAt(j));
						}
						
						// recalculate the bound
						vecTimestamps.removeAllElements();
						bound = calculateBound(hashValues, vecTimestamps);
						
						//LogHolder.log(LogLevel.WARNING, LogType.MISC, "i: " + i +  " new bound: " + bound);
					}
					break;
				}
			}
			
			return bound;
		}
		
		/**
		 * Returns the bound value. If it is invoked by the client 
		 * the stored m_lBoundValue is returned otherwise the bound
		 * value is calculated from the values in the entry.
		 * 
		 * When invoked by an active InfoService, the bound is calculated
		 * be removing the BOUND_ROUNDING per cent (%) worst results. The
		 * remaining worst result is then compared to BOUNDARIES. The
		 * best value from BOUNDARIES, that is equal to or worse than
		 * the real worst value after removing the others is the bound.
		 * 
		 * @param a_bLow Low or high bound.
		 * @return The bound value.
		 */
		private int calculateBound(Hashtable a_hashValues, Vector a_timestamps)
		{
			int values = 0;
			long errors = 0;
			Long timestamp;
			
			Vector vecValues = new Vector();
			Enumeration e = a_hashValues.keys();
			
			while(e.hasMoreElements())
			{
				timestamp = (Long) e.nextElement();				
				// value is too old, remove it
				if(System.currentTimeMillis() - timestamp.longValue() > DEFAULT_TIMEFRAME)
				{
					continue;
				}
				
				a_timestamps.addElement(timestamp);

				// get the value
				Integer value = ((Integer) a_hashValues.get(timestamp));
				if(value.intValue() < 0)
				{
					// value is an error
					errors++;
				}
				else if (value.intValue() == Integer.MAX_VALUE)
				{
					// value was not measured (try)
				}
				else
				{
					values++;
					vecValues.addElement(value);
				}
			}
			
			
			// we don't seem to have any values
			if (values == 0)
			{
				// but we have errors
				if(errors > 0)
				{
					return -1;
				}
				else if (m_attribute == DELAY)
				{
					return 0;
				}
				else 
				{
					return Integer.MAX_VALUE;
				}
			}
			
			
			if (m_attribute == DELAY)
			{
				Util.sort(vecValues, new IntegerSortDesc());				
			}
			else
			{
				Util.sort(vecValues, new IntegerSortAsc());
			}
			
			int limit = (int) Math.floor((double)vecValues.size() * BOUND_ROUNDING);
			
			for (int i = 0; i < limit; i++)
			{
				vecValues.removeElementAt(0);
			}
			
			if (vecValues.size() > 0)
			{
				int value = ((Integer) vecValues.elementAt(0)).intValue();
			
				return getBoundFromValue(value);
			}
			
			return -1;
		}
		
		/**
		 * Returns the best bound value. If it is invoked by the client 
		 * the stored m_lBoundValue is returned otherwise the bound
		 * value is calculated from the values in the entry.
		 * 
		 * @return The best bound value.
		 */
		public int getBestBound()
		{
			// if it is invoked by the client, just return the stored bound value
			if(!m_bInfoService)
			{
				return m_lBestBoundValue;
			}
			
			int values = 0;
			long errors = 0;
			Long timestamp;
			
			int bestValue;
			
			if (m_attribute == SPEED)
			{
				bestValue = Integer.MAX_VALUE;
			}
			else
			{
				bestValue = 0;
			}
			
			
			synchronized (m_Values)
			{
				Enumeration e = m_Values.keys();
				
				while(e.hasMoreElements())
				{
					timestamp = (Long) e.nextElement();
					// value is too old, remove it
					if(System.currentTimeMillis() - timestamp.longValue() > DEFAULT_TIMEFRAME)
					{
						continue;
					}

					// get the value
					Integer value = ((Integer) m_Values.get(timestamp));
					if(value.intValue() < 0)
					{
						// value is an error
						errors++;
					}
					else if (value.intValue() == Integer.MAX_VALUE)
					{
						// TODO
					}
					else
					{
						values++;
						
						if (m_attribute == SPEED)
						{
							if(value.intValue() < bestValue)
							{
								bestValue = value.intValue();
							}
						}
						else
						{
							if(value.intValue() > bestValue)
							{
								bestValue = value.intValue();
							}
						}
						
					}
				}
			}
			
			// we don't seem to have any values
			if (values == 0)
			{
				// but we have errors
				if(errors > 0)
				{
					return -1;
				}
				else if (m_attribute == SPEED)
				{
					return 0;
				}
				else 
				{
					return Integer.MAX_VALUE;
				}				
			}
			else
			{
				return getBoundFromValue(bestValue);
			}
		}
		
		private int getBoundFromValue(int value) 
		{
			if (m_attribute == DELAY)
			{
				for (int i = 0; i < BOUNDARIES[m_attribute].length; i++)
				{
					if (value <= BOUNDARIES[m_attribute][i])
					{
						return BOUNDARIES[m_attribute][i];
					}
				}
				
				return BOUNDARIES[m_attribute][BOUNDARIES[m_attribute].length - 1];
			}
			else
			{
				for (int i = BOUNDARIES[m_attribute].length -1 ; i >= 0; i--)
				{
					if (value >= BOUNDARIES[m_attribute][i])
					{
						return BOUNDARIES[m_attribute][i];
					}
				}
			}
			
			
			return BOUNDARIES[m_attribute][0];
		}
		
		/**
		 * Calculates and return the average value. 
		 * 
		 * @return The average value.
		 */
		public int getAverage()
		{
			// this method should only be invoked by the InfoService
			if(!m_bInfoService)
			{
				return -1;
			}
			
			int values = 0;
			int value = 0;
			int lAverageValue = 0;
			long errors = 0;
			Long timestamp;
			
			synchronized (m_Values)
			{
				Enumeration e = m_Values.keys();
				
				while(e.hasMoreElements())
				{
					timestamp = (Long) e.nextElement();
					if(System.currentTimeMillis() - timestamp.longValue() > DEFAULT_TIMEFRAME)
					{
						continue;
					}
					
					value = ((Integer) m_Values.get(timestamp)).intValue();
					
					if(value < 0)
					{
						errors++;
					}
					else if (value == Integer.MAX_VALUE)
					{
						// TODO
					}
					else
					{
						values++;
						lAverageValue += value;
					}
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
		
		/**
		 * Calculates and returns the standard deviation.
		 * 
		 * @return The standard deviation.
		 */
		public double getStdDeviation()
		{
			// this method should only be invoked by the InfoService
			if(!m_bInfoService)
			{
				return -1;
			}
			
			int values = 0;
			int value = 0;
			long errors = 0;
			long mseValue = 0;
			Long timestamp;
			
			synchronized (m_Values)
			{
				Enumeration e = m_Values.keys();
				
				while(e.hasMoreElements())
				{
					timestamp = (Long) e.nextElement();
					if(System.currentTimeMillis() - timestamp.longValue() > DEFAULT_TIMEFRAME)
					{
						continue;
					}
					
					value = ((Integer) m_Values.get(timestamp)).intValue();
					
					if(value < 0)
					{
						errors++;
					}
					else if (value == Integer.MAX_VALUE)
					{
						// TODO
					}
					else
					{
						values++;
						mseValue += Math.pow(value - getAverage(), 2);
					}
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
				mseValue /= values;
				// standard deviation
				return Math.sqrt(mseValue);
			}
		}
		
		public Element toXmlElement(Document a_doc)
		{
			Element elem = a_doc.createElement(ATTRIBUTES[m_attribute]);
			
			XMLUtil.setAttribute(elem, XML_ATTR_BOUND, getBound());
			XMLUtil.setAttribute(elem, XML_ATTR_BEST, getBestBound());
			
			return elem;
		}
	}

	/**
	 * An entry that holds values of performance attributes.
	 * 
	 * @author Christian Banse
	 */
	public static class PerformanceAttributeEntry
	{
		/**
		 * The last value added. This is only useful for PACKETS.
		 */
		private int m_iLastValue = -1;
		private long m_iLastTimestamp = -1;
		
		/**
		 * The max value.
		 */
		private int m_lMaxValue = -1;
		
		/**
		 * The min value.
		 */
		private int m_iMinValue = -1;
		
		/**
		 * The average value.
		 */
		private int m_lAverageValue = -1;
		
		/**
		 * The bound value.
		 */
		private int m_lBound = -1;
		
		/**
		 * The standard deviation.
		 */
		private double m_lStdDeviation = 0.0;
		
		/**
		 * The time of the last update.
		 */
		private long m_lastUpdate = -1;
		
		/**
		 * The values.
		 */
		private Hashtable m_Values = new Hashtable();		
		
		/**
		 * The amount of errors occurred.
		 */
		private int m_iErrors = 0;
		
		private int m_iResets = 0;
		
		private int m_iUnknown = 0;
		
		private int m_iSuccess = 0;
		
		/**
		 * The performance attribute.
		 */
		private int m_attribute;
		
		/**
		 * True, if the object is created by the info service 
		 * or the JAP client. Determines whether the bound value
		 * is retrieved from the stored value or calculated on the fly.
		 */
		private boolean m_bPassive; 
		
		private PerformanceAttributeEntry(int a_attribute, boolean a_bPassive)
		{
			m_attribute = a_attribute;
			m_bPassive = a_bPassive;
		}
		
		/**
		 * Adds a value to the entry.
		 * 
		 * @param a_lTimeStamp The timestamp.
		 * @param a_iValue The value.
		 */
		private void addValue(long a_lTimeStamp, int a_iValue, PerformanceAttributeEntry a_previousEntry)
		{
			if (System.currentTimeMillis() - a_lTimeStamp >= WEEK_SEVEN_DAYS_TIMEOUT)
			{
				// ignore
				return;
			}
			
			m_lastUpdate = a_lTimeStamp;						
			
			if (a_iValue < 0 || (!m_bPassive && a_iValue == Integer.MAX_VALUE))
			{
				if (a_iValue < 0)
				{
					m_iErrors++;
					if (a_iValue < -1)
					{
						LogHolder.log(LogLevel.WARNING, LogType.MISC, 
								"Got negative performance value (" + a_iValue + ") for timestamp " + a_lTimeStamp + ".");
					}
				}	
				else if (a_iValue == Integer.MAX_VALUE)
				{
					m_iUnknown++;
				}
				
				// we have at least one error and no values
				if(m_Values.size() == 0)
				{
					m_lAverageValue = -1;
					m_iMinValue = -1;
					m_lMaxValue = -1;
					m_lStdDeviation = -1.0;
					m_lBound = -1;
				}
				return;
			}						
			
			m_Values.put(new Long(a_lTimeStamp), new Integer(a_iValue));
			m_iSuccess++;
			if (m_attribute == PACKETS)
			{
				if (m_iLastTimestamp < 0 && a_previousEntry != null)
				{
					m_iLastTimestamp = a_previousEntry.m_iLastTimestamp;
					m_iLastValue = a_previousEntry.m_iLastValue;
				}
				
				if (m_iLastTimestamp < a_lTimeStamp)
				{
					if (a_iValue < m_iLastValue)
					{
						// this service has been restarted since the last test
						m_iResets++;				
					}
					m_iLastValue = a_iValue;
					m_iLastTimestamp = a_lTimeStamp;
				}
				else
				{
					LogHolder.log(LogLevel.WARNING, LogType.MISC, 
							"Unordered timestamps for hourly attribute " + m_attribute + "." +
							"Timestamp new: " + a_lTimeStamp + " Timestamp old: " + m_iLastTimestamp +
							" Value: " + a_iValue);
				}
			}
			
			int lValues = 0;
			double mseValue = 0; // mean squared error
			Enumeration e;
			synchronized (m_Values)
			{
				e = m_Values.elements();
				while(e.hasMoreElements())
				{
					lValues += ((Integer) e.nextElement()).intValue();
				}
				
				m_lAverageValue = lValues / m_Values.size();
				
				e = m_Values.elements();
				while(e.hasMoreElements())
				{
					mseValue += Math.pow(((Integer) e.nextElement()).intValue() - m_lAverageValue, 2);
				}
				
				mseValue /= m_Values.size();
			}
			
			// standard deviation
			m_lStdDeviation = Math.sqrt(mseValue);
			if (mseValue < 0)
			{
				LogHolder.log(LogLevel.EMERG, LogType.MISC, 
						"Negative mean square error! " + mseValue);
			}
			if (a_iValue < 0)
			{
				LogHolder.log(LogLevel.ERR, LogType.MISC, "Negative attribute value! " + a_iValue);
			}
			
			if (m_iMinValue == 0 || m_iMinValue == -1)
			{
				m_iMinValue = a_iValue;
			}
			else
			{
				m_iMinValue = Math.min(m_iMinValue, a_iValue);
			}
			
			m_lMaxValue = Math.max(m_lMaxValue, a_iValue);
			
			Vector vec = new Vector();
			synchronized (m_Values)
			{
				e = m_Values.elements();
				while(e.hasMoreElements())
				{
					Integer value = (Integer) e.nextElement();
					
					if (value.intValue() >= 0)
					{
						vec.addElement(value);
					}
				}
			}
			
			if (m_attribute == SPEED)
			{
				Util.sort(vec, new IntegerSortAsc());
			}
			else
			{
				Util.sort(vec, new IntegerSortDesc());
			}
			
			int limit = (int) Math.floor((double)vec.size() * BOUND_ROUNDING);
			
			for (int i = 0; i < limit && vec.size() > 1; i++)
			{
				vec.removeElementAt(0);
			}
			
			if (vec.size() > 0)
			{
				int value = ((Integer) vec.elementAt(0)).intValue();
				
				if (m_attribute == SPEED)
				{
					for(int i = BOUNDARIES[m_attribute].length -1 ; i >= 0; i--)
					{
						if(value >= BOUNDARIES[m_attribute][i])
						{
							m_lBound = BOUNDARIES[m_attribute][i];
							return;
						}
					}
				}
				else
				{
					for (int i = 0; i < BOUNDARIES[m_attribute].length; i++)
					{
						if (value <= BOUNDARIES[m_attribute][i])
						{
							m_lBound = BOUNDARIES[m_attribute][i];
							return;
						}
					}
					
					m_lBound = BOUNDARIES[m_attribute][BOUNDARIES[m_attribute].length - 1];
					return;
				}
				
				m_lBound = BOUNDARIES[m_attribute][0];
				return;
			}
			else
			{
				m_lBound = -1;
			}
		}
		
		/**
		 * Returns the average value.
		 * 
		 * @return The average value.
		 */
		public int getAverageValue()
		{
			return m_lAverageValue;
		}
		
		/**
		 * Returns the min value.
		 * 
		 * @return The min value.
		 */
		public int getMinValue()
		{	
			return m_iMinValue;
		}
		
		/**
		 * Returns the max value.
		 * 
		 * @return The max value.
		 */
		public int getMaxValue()
		{
			return m_lMaxValue;
		}
		
		/**
		 * Returns the bound value.
		 * 
		 * @return The bound value.
		 */
		public int getBound()
		{
			return m_lBound;
		}
		
		/**
		 * Returns the standard deviation.
		 * 
		 * @return The standard deviation.
		 */
		public double getStdDeviation()
		{
			return m_lStdDeviation;
		}
		
		public void setErrors(int a_errors)
		{
			m_iErrors = a_errors;
		}
		
		/**
		 * Returns the amount of errors.
		 * 
		 * @return The amounts of errors.
		 */
		public int getErrors()
		{
			return m_iErrors;
		}
		
		public void setResets(int a_resets)
		{
			m_iResets = a_resets;
		}
		
		/**
		 * Only useful for PACKETS attribute. Tells how often the service has been reset/restarted.
		 * @return how often the service has been reset/restarted
		 */
		public int getResets()
		{
			return m_iResets;
		}
		
		/**
		 * The amount of attempts that should have been made but were not.
		 * @return
		 */
		public int getUnknown()
		{
			return m_iUnknown;
		}
		
		public void setUnknown(int a_unknown)
		{
			m_iUnknown = a_unknown;
		}
		
		public void setSuccess(int a_iSuccess)
		{
			m_iSuccess = a_iSuccess;
		}
		
		public int getSuccess()
		{
			//return m_Values.size();
			return m_iSuccess;
		}
		
		/**
		 * Returns the amount of values and errors.
		 * 
		 * @return The amount of values and errors.
		 */
		public int getValueSize()
		{
			return getSuccess() + m_iErrors + m_iUnknown;
		}
		
		/**
		 * Returns the day timestamp of the entry.
		 * 
		 * @return The day timestamp.
		 */
		public long getDayTimestamp()
		{
			Calendar cal = Calendar.getInstance();
			cal.setTime(new Date(m_lastUpdate));
			
			return m_lastUpdate - cal.get(Calendar.HOUR_OF_DAY) * 60 * 60 * 1000 -
				cal.get(Calendar.MINUTE) * 60 * 1000 - cal.get(Calendar.SECOND) * 1000 -
				cal.get(Calendar.MILLISECOND); 
		}
	}
	
	public static class StabilityAttributes
	{		
		public static final String XML_ELEMENT_NAME = "Stability";
		private static final String XML_ATTR_TOTAL = "total";
		private static final String XML_ATTR_UNKNOWN = "unknown";
		private static final String XML_ATTR_ERRORS = "errors";
		private static final String XML_ATTR_RESETS = "resets";
		private static final String XML_ATTR_BOUND_UNKNOWN = "boundUnknown";
		private static final String XML_ATTR_BOUND_ERRORS = "boundErrors";
		private static final String XML_ATTR_BOUND_RESETS = "boundResets";
		
		private static final double BOUND = 5.0;
		
		private int m_iSize;
		private int m_iErrors;
		private int m_iResets;
		private int m_iUnknown;
		
		private int m_boundUnknown;
		private int m_boundErrors;
		private int m_boundResets;
		
		private StabilityAttributes(Element a_entry) throws XMLParseException
		{
			XMLUtil.assertNodeName(a_entry, XML_ELEMENT_NAME);
			
			m_iSize = XMLUtil.parseAttribute(a_entry, XML_ATTR_TOTAL, 0);
			m_iUnknown = XMLUtil.parseAttribute(a_entry, XML_ATTR_UNKNOWN, 0);
			m_iErrors = XMLUtil.parseAttribute(a_entry, XML_ATTR_ERRORS, 0);
			m_iResets = XMLUtil.parseAttribute(a_entry, XML_ATTR_RESETS, 0);
			m_boundUnknown = XMLUtil.parseAttribute(a_entry, XML_ATTR_BOUND_UNKNOWN, 0);
			m_boundErrors = XMLUtil.parseAttribute(a_entry, XML_ATTR_BOUND_ERRORS, 0);
			m_iResets = XMLUtil.parseAttribute(a_entry, XML_ATTR_BOUND_RESETS, 0);			
		}
		
		public StabilityAttributes(int a_iSize, int a_iUnknown, int a_iErrors, int a_iResets)
		{
			double percentageOne, percentageTwo;
			
			m_iSize = a_iSize;
			m_iUnknown = a_iUnknown;
			m_iErrors = a_iErrors;
			m_iResets = a_iResets;
			
			if (a_iSize == 0)
			{
				m_boundUnknown = 0;
				m_boundErrors = 0;
				m_boundResets = 0;
				return;
			}
			
			// calculate percentage bounds
			percentageOne = (100.0d * (double)m_iUnknown) / (double)m_iSize;
			percentageTwo = (100.0d * (double)a_iErrors) / (double)m_iSize;

			// upper bounds for percentage
			m_boundUnknown = (int)Math.ceil(percentageOne / BOUND) * (int)BOUND;
			m_boundErrors = (int)Math.ceil(percentageTwo / BOUND) * (int)BOUND;
			m_boundResets = (int)Math.ceil(100.0d * (double)a_iResets / (double)a_iSize / BOUND) * (int)BOUND;
		}
		
		/**
		 * Returns the amount of errors.
		 * 
		 * @return The amounts of errors.
		 */
		public int getBoundErrors()
		{
			return m_boundErrors;
		}
		
		/**
		 * Only useful for PACKETS attribute. Tells how often the service has been reset/restarted.
		 * @return how often the service has been reset/restarted
		 */
		public int getBoundResets()
		{
			return m_boundResets;
		}
		
		/**
		 * The amount of attempts that should have been made but were not.
		 * @return
		 */
		public int getBoundUnknown()
		{
			return m_boundUnknown;
		}
		
		/**
		 * Returns the amount of values and errors.
		 * 
		 * @return The amount of values and errors.
		 */
		public int getValueSize()
		{
			return m_iSize;
		}
		
		public Element toXmlElement(Document a_doc)
		{
			Element elem = a_doc.createElement(XML_ELEMENT_NAME);
			
			XMLUtil.setAttribute(elem, XML_ATTR_TOTAL, m_iSize);
			XMLUtil.setAttribute(elem, XML_ATTR_UNKNOWN, m_iUnknown);
			XMLUtil.setAttribute(elem, XML_ATTR_ERRORS, m_iErrors);
			XMLUtil.setAttribute(elem, XML_ATTR_RESETS, m_iResets);
			
			XMLUtil.setAttribute(elem, XML_ATTR_BOUND_UNKNOWN, m_boundUnknown);
			XMLUtil.setAttribute(elem, XML_ATTR_BOUND_ERRORS, m_boundErrors);
			XMLUtil.setAttribute(elem, XML_ATTR_BOUND_RESETS, m_boundResets);
			
			return elem;
		}
	}
}
