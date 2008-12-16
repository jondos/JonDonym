package jap;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Calendar;
import java.util.Date;
import java.util.Enumeration;
import java.util.GregorianCalendar;
import java.util.Hashtable;
import java.util.StringTokenizer;
import java.util.Vector;

import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;

import anon.infoservice.Database;
import anon.infoservice.InfoServiceHolder;
import anon.infoservice.MixCascade;
import anon.infoservice.PerformanceEntry;
import anon.infoservice.PerformanceInfo;
import anon.infoservice.PerformanceEntry.PerformanceAttributeEntry;
import anon.infoservice.PerformanceEntry.StabilityAttributes;

public class PassiveInfoServiceMainUpdater extends AbstractDatabaseUpdater
{
	public static final String PERFORMANCE_LOG_FILE = "performancePassive_"; 
	
	PerformanceInfoUpdater m_performanceInfoUpdater = null;
	PassiveInfoServiceCascadeUpdater m_cascadeUpdater = null;
	MixInfoUpdater m_mixUpdater = null;
	PaymentInstanceUpdater piUpdater = null;
	
	/**
	 * Stream to the performance log file.
	 */
	private FileOutputStream m_stream = null;
	
	/**
	 * The current week.
	 */
	private int m_currentWeek;
	
	/**
	 * A <code>Calendar</code> object for various date calculations.
	 */
	private final Calendar m_cal = Calendar.getInstance();
	
	
	public PassiveInfoServiceMainUpdater(long interval, boolean a_bFetchPerformanceData) 
	throws IOException
	{
		super(interval);
		
		m_cascadeUpdater = new PassiveInfoServiceCascadeUpdater(Long.MAX_VALUE);
		m_mixUpdater = new MixInfoUpdater();
		piUpdater = new PaymentInstanceUpdater(Long.MAX_VALUE);
		
		if (a_bFetchPerformanceData)
		{
			m_performanceInfoUpdater = new PerformanceInfoUpdater(Long.MAX_VALUE);
		
			// set calendar to current time
			m_cal.setTime(new Date(System.currentTimeMillis()));
	
			
			// set the current week
			m_currentWeek = m_cal.get(Calendar.WEEK_OF_YEAR);
			
			// if we're on the last day of the week (saturday) we already have enough
			// performance data for the last 7 days.
			if(m_cal.get(Calendar.DAY_OF_WEEK) != Calendar.SATURDAY)
			{
				// if it's not Saturday -> import last week, too
				readOldPerformanceData(m_currentWeek - 1);
			}
			
			// import old performance date from this week
			readOldPerformanceData(m_currentWeek);
			
			try
			{
				// open the stream for the performance log file
				m_stream = new FileOutputStream(PERFORMANCE_LOG_FILE + 				
						m_cal.get(Calendar.YEAR) + "_" + m_currentWeek + ".log", true);
			}
			catch(IOException ex)
			{
				LogHolder.log(LogLevel.WARNING, LogType.NET, "Could not open "+ PERFORMANCE_LOG_FILE + ".");
				throw ex;
			}
			
			/*
			 * Update current performance entries from performance info cache.
			 */
			Enumeration cascades = Database.getInstance(MixCascade.class).getEntrySnapshotAsEnumeration();
			while(cascades.hasMoreElements())
			{
				getUpdatedEntry(((MixCascade) cascades.nextElement()).getId());
			}
		}
	}
	
	public PassiveInfoServiceMainUpdater(boolean a_bFetchPerformanceData) throws IOException
	{
		this(Long.MAX_VALUE, a_bFetchPerformanceData);
	}
	
	protected boolean doCleanup(Hashtable a_newEntries)
	{
		if (m_performanceInfoUpdater != null)
		{
			return super.doCleanup(a_newEntries);
		}
		return false;
	}
	
	/**
	 * fetches several infos needed for the passive InfoService:
	 * 1. PerformanceInfos (to calculate the PerformanceEntries)
	 * 2. MixCascades
	 * 3. ExitAddresses (depends on MixCascades)
	 * 4. finally PerformanceEntries are calculated and returned.
	 */
	protected Hashtable getUpdatedEntries(Hashtable toUpdate)
	{					
		/* 1. PerformanceInfos Database update (to calculate the PerformanceEntries) */
		if (m_performanceInfoUpdater != null)
		{
			m_performanceInfoUpdater.update();
		}
		
		piUpdater.update();
		
		/* 2. MixCascades Database update */
		m_cascadeUpdater.update();
		/* 3. Exit addresses Database update */
		InfoServiceHolder.getInstance().getExitAddresses();
				
		m_mixUpdater.updateAsync();
		
		Hashtable performanceEntries = new Hashtable();
		if (m_performanceInfoUpdater != null)
		{
			/* now calculate lowest bounds of the performance entries */
			Enumeration enumCurrentPerEntries = Database.getInstance(PerformanceEntry.class).getEntrySnapshotAsEnumeration();
			Enumeration cascades = Database.getInstance(MixCascade.class).getEntrySnapshotAsEnumeration();
			
			String currentCascadeId;
			PerformanceEntry curPerformanceEntry;
			
			while(cascades.hasMoreElements())
			{
				currentCascadeId = ((MixCascade) cascades.nextElement()).getId();
				performanceEntries.put(currentCascadeId, getUpdatedEntry(currentCascadeId));
			}
			
			while (enumCurrentPerEntries.hasMoreElements())
			{
				curPerformanceEntry = (PerformanceEntry)enumCurrentPerEntries.nextElement();
				if (performanceEntries.containsKey(curPerformanceEntry.getId()))
				{
					// OK, has been updated
					continue;
				}
				
				// this entry needs an update but its cascade is not available; if it is too old, it will be deleted
				if (System.currentTimeMillis() - curPerformanceEntry.getLastUpdate() < PerformanceEntry.WEEK_SEVEN_DAYS_TIMEOUT)
				{
					performanceEntries.put(curPerformanceEntry.getId(), getUpdatedEntry(curPerformanceEntry.getId()));
				}
			}
		}
		
		return performanceEntries;
	}
	
	private PerformanceEntry getUpdatedEntry(String a_cascadeID)
	{
		PerformanceEntry dbPerformanceEntry = null;
		Vector attributeEntriesToUpdate;
		PerformanceEntry.PerformanceAttributeEntry attributeEntry;
		StabilityAttributes stabilityAttributeEntry;
		PerformanceEntry currentPerformanceEntry = PerformanceInfo.getLowestCommonBoundEntry(a_cascadeID);
		long timestamp = System.currentTimeMillis();
		
		synchronized (Database.getInstance(PerformanceEntry.class))
		{
			dbPerformanceEntry = 
				(PerformanceEntry)Database.getInstance(
						PerformanceEntry.class).getEntryById(a_cascadeID);
			if (dbPerformanceEntry == null)
			{							
				Database.getInstance(PerformanceEntry.class).update(currentPerformanceEntry);
			}
			else
			{
				currentPerformanceEntry = dbPerformanceEntry.update(currentPerformanceEntry);							
			}
		}					
		
		attributeEntriesToUpdate = 
			currentPerformanceEntry.updateHourlyPerformanceAttributeEntries(timestamp);
		for (int i = 0; i < attributeEntriesToUpdate.size(); i++)
		{
			stabilityAttributeEntry = 
				currentPerformanceEntry.getStabilityAttributes();
			attributeEntry = 
				(PerformanceEntry.PerformanceAttributeEntry)attributeEntriesToUpdate.elementAt(i);
			attributeEntry.setErrors(stabilityAttributeEntry.getBoundErrors());
			attributeEntry.setUnknown(stabilityAttributeEntry.getBoundUnknown());
			attributeEntry.setResets(stabilityAttributeEntry.getBoundResets());
			attributeEntry.setSuccess(
					stabilityAttributeEntry.getValueSize() - 
					stabilityAttributeEntry.getBoundErrors() -
					stabilityAttributeEntry.getBoundUnknown());
		}
		
		if (attributeEntriesToUpdate.size() > 0)
		{
			logPerftestData(timestamp, currentPerformanceEntry);
		}
		
		return currentPerformanceEntry;
	}
	
	public Class getUpdatedClass() 
	{
		return PerformanceEntry.class;
	}


	
	protected Hashtable getEntrySerials() 
	{
		return new Hashtable();
	}
	
	
	/**
	 * Imports old performance date from the hard disk.
	 * 
	 * @param week The week we want to import.
	 */
	private void readOldPerformanceData(int week) 
	{
		int year = m_cal.get(Calendar.YEAR);
		
		if (week == 0)
		{
			year--;
			week = new GregorianCalendar(year, Calendar.DECEMBER, 31).get(Calendar.WEEK_OF_YEAR);
		}
		
		try
		{
			// open the stream
			FileInputStream stream = new FileInputStream(PERFORMANCE_LOG_FILE + 
					year + "_" + week + ".log");
			
			BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
			String line = null;
			StringTokenizer tokenizer;
			long timestamp = -1;
			int delay = -1, speed = -1, delayBest = -1, speedBest = -1;
			int errors = -1, unknown = -1 , resets = -1;
			String currentToken;
			String id = null;
			PerformanceAttributeEntry attrEntry;
			StabilityAttributes attrStability;
			
			// read until EOF
			while ((line = reader.readLine()) != null)
			{
				tokenizer = new StringTokenizer(line);
				int i = 0;
				for (; tokenizer.hasMoreTokens(); i++)
				{
					currentToken = tokenizer.nextToken();
					if (i == 0)
					{
						timestamp = Long.parseLong(currentToken);
					}
					else if (i == 1)
					{
						id = currentToken;
					}
					else if (i == 2)
					{
						delay = Integer.parseInt(currentToken);
					}
					else if (i == 3)
					{
						delayBest = Integer.parseInt(currentToken);
					}
					else if (i == 4)
					{
						speed = Integer.parseInt(currentToken);
					}
					else if (i == 5)
					{
						speedBest = Integer.parseInt(currentToken);
					}
					else if (i == 6)
					{
						errors = Integer.parseInt(currentToken);
					}
					else if (i == 7)
					{
						unknown = Integer.parseInt(currentToken);
					}
					else if (i == 8)
					{
						resets = Integer.parseInt(currentToken);
					}
					else
					{
						LogHolder.log(LogLevel.WARNING, LogType.MISC, 
								"Too many performance log tokens: " + i);
						break;
					}
				}
				if (i < 9)
				{
					LogHolder.log(LogLevel.EXCEPTION, LogType.MISC,  "No enough performance log tokens: " + i);
				}
				
				if (System.currentTimeMillis() - timestamp >= PerformanceEntry.WEEK_SEVEN_DAYS_TIMEOUT)
				{
					continue;
				}
					
				// look for an existing performance entry
				PerformanceEntry entry = (PerformanceEntry) Database.getInstance(PerformanceEntry.class).getEntryById(id);
				
				// create one if necessary
				if (entry == null)
				{
					entry = new PerformanceEntry(id, true);
				}
				
				
				attrStability = new StabilityAttributes(100, errors, unknown, resets);
				
				// import the extracted value into the performance entry
				if (delay == 0)
				{
					delay = -1;
				}
				attrEntry = entry.importValue(PerformanceEntry.DELAY, timestamp, delay);
				//entry.setStabilityAttributes(attrStability);
				//entry.setBound(PerformanceEntry.DELAY, delay);
				//entry.setBestBound(PerformanceEntry.DELAY, delayBest);
				if (attrEntry != null)
				{
					attrEntry.setErrors(errors);
					attrEntry.setUnknown(unknown);
					attrEntry.setResets(resets);
					attrEntry.setSuccess(100 - errors - unknown);
				}
				
				if (speed== Integer.MAX_VALUE)
				{
					speed = -1;
				}				
				attrEntry = entry.importValue(PerformanceEntry.SPEED, timestamp, speed);
				//entry.setStabilityAttributes(attrStability);
				//entry.setBound(PerformanceEntry.SPEED, speed);
				//entry.setBestBound(PerformanceEntry.SPEED, speedBest);
				if (attrEntry != null)
				{
					attrEntry.setErrors(errors);
					attrEntry.setUnknown(unknown);
					attrEntry.setResets(resets);
					attrEntry.setSuccess(100 - errors - unknown);
				}
				
				Database.getInstance(PerformanceEntry.class).update(entry);
				
			}
		}
		catch(IOException ex)
		{
			LogHolder.log(LogLevel.WARNING, LogType.NET, 
					"No previous performance data for this week found: " +
					ex.getMessage());
		}
		
		LogHolder.log(LogLevel.NOTICE, LogType.NET, "Added previous performance data for week" + week);
	}	
	
	private void logPerftestData(long a_timestamp, PerformanceEntry a_entry)
	{
		try
		{
			StabilityAttributes attributes;
			
			m_cal.setTime(new Date(System.currentTimeMillis()));
			// check if we're still in the same week, if not open a new performance log file
			if (m_cal.get(Calendar.WEEK_OF_YEAR) != m_currentWeek)
			{
				m_currentWeek = m_cal.get(Calendar.WEEK_OF_YEAR);
				
				// open a new stream
				m_stream.close();
				m_stream = new FileOutputStream(PERFORMANCE_LOG_FILE + 
						m_cal.get(Calendar.YEAR) + "_" + m_currentWeek + ".log", true);
			}
			
			attributes = a_entry.getStabilityAttributes();
			
			m_stream.write((a_timestamp + "\t" + a_entry.getId() + "\t" + 
					a_entry.getBound(PerformanceEntry.DELAY).getNotRecoveredBound() + "\t" +
					a_entry.getBestBound(PerformanceEntry.DELAY) + "\t" + 
					a_entry.getBound(PerformanceEntry.SPEED).getNotRecoveredBound() + "\t" +
					a_entry.getBestBound(PerformanceEntry.SPEED) + "\t" + attributes.getBoundErrors() + "\t" +
					attributes.getBoundUnknown() + "\t" + attributes.getBoundResets() + "\n").getBytes());
			m_stream.flush();
			
		}
		catch(IOException ex)
		{
			LogHolder.log(LogLevel.EXCEPTION, LogType.NET, ex);
		}
	}

}
