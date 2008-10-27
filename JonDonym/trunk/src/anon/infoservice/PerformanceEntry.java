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

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.Document;

import anon.util.Util.IntegerSortAsc;
import anon.util.Util.IntegerSortDesc;
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
	 * Text representation of the performance attributes.
	 */
	private static final String[] ATTRIBUTES = new String[]{ "Speed", "Delay", "Users" };
	
	/**
	 * The boundaries used to calculate the speed and delay bounds.
	 */
	public static final int[][] BOUNDARIES = new int[][] { 
		{ 0, 50, 100, 200, 300, 400, 500, 750, 1000, 1500 },
		{ 500, 750, 1000, 2000, 2500, 3000, 4000, 8000, Integer.MAX_VALUE },
		{ 0 } };
	
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
	
	/**
	 * The time of the last performed test.
	 */
	private long m_lastTestTime;
	
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
	private int m_lastTestAverage[] = new int[3];
	
	/**
	 * Constructs a new <code>PerformanceEntry</code> for the given <code>MixCascade</code>.
	 * 
	 * @param a_strCascadeId The id of the mix cascade.
	 */
	public PerformanceEntry(String a_strCascadeId)
	{
		this(a_strCascadeId, true);
	}
	
	/**
	 * Constructs a new <code>PerformanceEntry</code> for the given <code>MixCascade</code>.
	 * 
	 * @param a_strCascadeId The id of the mix cascade.
	 * @param a_bInfoService Specifies if the caller of this method is an <code>InfoService</code> or the JAP client.
	 */
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
	public void importValue(int a_attribute, long a_timestamp, int a_value)
	{
		// check if entry is older than 7 days
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
		
		// look for the attribute entry
		entry = m_entries[a_attribute][day][hour];
		// create one if necessary
		if(entry == null)
		{
			m_entries[a_attribute][day][hour] = entry = new PerformanceAttributeEntry(a_attribute);
		}
		
		// add the value to the entry
		entry.addValue(a_timestamp, a_value);
		// add the value to the floating time entry
		m_floatingTimeEntries[a_attribute].addValue(a_timestamp, a_value);
		
		m_lastUpdate = a_timestamp;
		
		// check if entries are obsolete
		for(int i = 0; i < 24; i++)
		{
			if(m_entries[a_attribute][day][i] == null)
			{
				continue;
			}
			
			// we've found at least one value in the entry that is older than 7 days
			if(System.currentTimeMillis() - 
					m_entries[a_attribute][day][i].getDayTimestamp() >= 7 * 24 * 60 * 60 * 1000)
			{
				// invalidate the whole entry
				m_entries[a_attribute][day][i] = null;
			}
		}
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
		PerformanceAttributeEntry entry = null;
		
		// this _SHOULD_ never become true
		if(a_data.isEmpty())
		{
			return -1;
		}
		
		int lAverageFromLastTest = -1;
		
		Long timestamp = null;
		Enumeration e = a_data.keys();
		
		int values = 0;
		
		// enumerate through the data hashtable
		while(e.hasMoreElements())
		{
			// get the timestamp
			timestamp = (Long) e.nextElement();
			int value = ((Integer) a_data.get(timestamp)).intValue();
			
			// timestamp is older than 7 days, ignore it
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
				
				// we've found at least one value in the entry that is older than 7 days
				if(System.currentTimeMillis() - 
						m_entries[a_attribute][day][i].getDayTimestamp() > 7 * 24 * 60 * 60 * 1000)
				{
					// invalidate the whole entry
					m_entries[a_attribute][day][i] = null;
				}
			}
			
			// look for the attribute entry
			entry = m_entries[a_attribute][day][hour];
			// create one if necessary
			if(entry == null)
			{
				m_entries[a_attribute][day][hour] = entry = new PerformanceAttributeEntry(a_attribute);
			}
			
			// add the value
			entry.addValue(timestamp.longValue(), value);
			
			// add the value to the floating time entry
			m_floatingTimeEntries[a_attribute].addValue(timestamp.longValue(), value);
			
			// if the value is valid, consider it for the average value
			if(value > 0)
			{
				lAverageFromLastTest += value;
				values++;
			}
		}
		
		// calculate the average value from the last test
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
	 * @param a_attribute The performance attribute.
	 * @return The average value from the last performed test.
	 */
	public int getLastTestAverage(int a_attribute)
	{
		return m_lastTestAverage[a_attribute];
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
	 * Calculates (if used by the info service) and returns the
	 * best bound value of the given attribute.
	 * 
	 * @param a_attribute The performance attribute.
	 * @return The best bound value of the given attribute.
	 */
	public int getBestBound(int a_attribute)
	{
		if(a_attribute == SPEED)
		{
			return m_floatingTimeEntries[a_attribute].getBestBound(true);
		}
		else if(a_attribute == DELAY)
		{
			return m_floatingTimeEntries[a_attribute].getBestBound(false);
		}
		else
		{
			return 0;
		}
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
				"<th>Bound</th>" +
				"<th>% Std. Deviation</th>" +
				"<th>Errors</th></tr>";

		long dayTimestamp = getDayTimestamp(a_attribute, a_selectedDay);
		
		for (int hour = 0; hour < 24; hour++)
		{
			htmlData += "<tr>" +
					"<td CLASS=\"name\">" + hour + ":00 - " + ((hour + 1) % 24) + ":00</td>";
			
			PerformanceAttributeEntry entry = m_entries[a_attribute][a_selectedDay][hour];
			
			Calendar calLastTest = Calendar.getInstance();
			calLastTest.setTime(new Date(m_lastTestTime));
			
			if (entry == null || System.currentTimeMillis() - dayTimestamp > 7 * 24 * 60 * 60 * 1000 )
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
				else if(a_attribute == SPEED)
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
		
		Element elemCurrent = a_doc.createElement(XML_ELEMENT_DATA);
		
		Element elemDelay = m_floatingTimeEntries[DELAY].toXmlElement(a_doc);
		elemCurrent.appendChild(elemDelay);
		
		Element elemSpeed = m_floatingTimeEntries[SPEED].toXmlElement(a_doc);
		elemCurrent.appendChild(elemSpeed);
		
		elem.appendChild(elemCurrent);
		
		return elem;
	}
	
	/**
	 * The floating time entry. This structure holds all attribute entries
	 * that are not older than a given time (usually 1 hour).
	 * 
	 * @author Christian Banse
	 */
	class PerformanceAttributeFloatingTimeEntry implements IXMLEncodable
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
				Enumeration e = m_Values.keys();
				
				m_Values.put(new Long(a_lTimeStamp), new Integer(a_lValue));
				
				// loop through all values
				while (e.hasMoreElements())
				{
					timestamp = (Long) e.nextElement();
					// value is too old, remove it
					if (System.currentTimeMillis() - timestamp.longValue() > DEFAULT_TIMEFRAME)
					{
						m_Values.remove(timestamp);
					}
				}
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
		 * @param a_bLow Low or high bound.
		 * @return The bound value.
		 */
		public int getBound(boolean a_bLow)
		{
			// if it is invoked by the client, just return the stored bound value
			if(!m_bInfoService)
			{
				return m_lBoundValue;
			}
			
			int values = 0;
			long errors = 0;
			Long timestamp;
			
			Vector vec = new Vector();
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
					else
					{
						values++;
						vec.addElement(value);
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
				else if (a_bLow)
				{
					return Integer.MAX_VALUE;
				}
				else 
				{
					return 0;
				}
			}
			
			
			if(a_bLow)
			{
				Util.sort(vec, new IntegerSortAsc());
			}
			else
			{
				Util.sort(vec, new IntegerSortDesc());
			}
			
			int limit = (int) Math.floor((double)vec.size() * BOUND_ROUNDING);
			
			for (int i = 0; i < limit; i++)
			{
				vec.removeElementAt(0);
			}
			
			if (vec.size() > 0)
			{
				int value = ((Integer) vec.elementAt(0)).intValue();
			
				return calculateBoundary(a_bLow, value);
			}
			
			return -1;
		}
		
		/**
		 * Returns the best bound value. If it is invoked by the client 
		 * the stored m_lBoundValue is returned otherwise the bound
		 * value is calculated from the values in the entry.
		 * 
		 * @param a_bLow Low or high bound.
		 * @return The best bound value.
		 */
		public int getBestBound(boolean a_bLow)
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
			
			if(a_bLow)
			{
				bestValue = 0;
			}
			else
			{
				bestValue = Integer.MAX_VALUE;
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
					else
					{
						values++;
						if(a_bLow)
						{
							if(value.intValue() > bestValue)
							{
								bestValue = value.intValue();
							}
						}
						else
						{
							if(value.intValue() < bestValue)
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
				else if (a_bLow)
				{
					return Integer.MAX_VALUE;
				}
				else 
				{
					return 0;
				}
			}
			else
			{
				return calculateBoundary(a_bLow, bestValue);
			}
		}
		
		private int calculateBoundary(boolean a_bLow, int value) 
		{
			if (a_bLow)
			{
				for (int i = BOUNDARIES[m_attribute].length -1 ; i >= 0; i--)
				{
					if (value >= BOUNDARIES[m_attribute][i])
					{
						return BOUNDARIES[m_attribute][i];
					}
				}
			}
			else
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
			
			if(this.m_attribute == SPEED)
			{
				XMLUtil.setAttribute(elem, XML_ATTR_BOUND, getBound(true));
				XMLUtil.setAttribute(elem, XML_ATTR_BEST, getBestBound(true));
			}
			else if(this.m_attribute == DELAY)
			{
				XMLUtil.setAttribute(elem, XML_ATTR_BOUND, getBound(false));
				XMLUtil.setAttribute(elem, XML_ATTR_BEST, getBestBound(false));
			}
			
			return elem;
		}
	}

	/**
	 * An entry that holds values of performance attributes.
	 * 
	 * @author Christian Banse
	 */
	class PerformanceAttributeEntry
	{
		/**
		 * The max value.
		 */
		private int m_lMaxValue = -1;
		
		/**
		 * The min value.
		 */
		private int m_lMinValue = -1;
		
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
		
		/**
		 * The performance attribute.
		 */
		private int m_attribute;
		
		/**
		 * Constructs a new <code>PerformanceAttributeEntry</code>.
		 * 
		 * @param a_attribute The performance attribute.
		 */
		public PerformanceAttributeEntry(int a_attribute)
		{
			m_attribute = a_attribute;
		}
		
		/**
		 * Adds a value to the entry.
		 * 
		 * @param a_lTimeStamp The timestamp.
		 * @param a_lValue The value.
		 */
		public void addValue(long a_lTimeStamp, int a_lValue)
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
					m_lBound = -1;
				}
				return;
			}
			
			m_Values.put(new Long(a_lTimeStamp), new Integer(a_lValue));
			
			int lValues = 0;
			Enumeration e = m_Values.elements();
			while(e.hasMoreElements())
			{
				lValues += ((Integer) e.nextElement()).intValue();
			}
			
			m_lAverageValue = lValues / m_Values.size();
			
			// mean squared error
			double mseValue = 0;
			e = m_Values.elements();
			while(e.hasMoreElements())
			{
				mseValue += Math.pow(((Integer) e.nextElement()).intValue() - m_lAverageValue, 2);
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
			
			Vector vec = new Vector();
			synchronized (m_Values)
			{
				e = m_Values.keys();
				
				e = m_Values.elements();
				while(e.hasMoreElements())
				{
					Integer value = (Integer) e.nextElement();
					
					if(value.intValue() > 0)
					{
						vec.addElement(value);
					}
				}
			}
			
			if(m_attribute == SPEED)
			{
				Util.sort(vec, new IntegerSortAsc());
			}
			else
			{
				Util.sort(vec, new IntegerSortDesc());
			}
			
			int limit = (int) Math.floor((double)vec.size() * BOUND_ROUNDING);
			
			for (int i = 0; i < limit; i++)
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
			return m_lMinValue;
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
		
		/**
		 * Returns the amount of errors.
		 * 
		 * @return The amounts of errors.
		 */
		public int getErrors()
		{
			return m_iErrors;
		}
		
		/**
		 * Returns the amount of values and errors.
		 * 
		 * @return The amount of values and errors.
		 */
		public int getValueSize()
		{
			return m_Values.size() + m_iErrors;
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
}
