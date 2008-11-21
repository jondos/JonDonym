package jap;

import java.util.Enumeration;
import java.util.Hashtable;

import anon.infoservice.Database;
import anon.infoservice.InfoServiceHolder;
import anon.infoservice.MixCascade;
import anon.infoservice.PerformanceEntry;
import anon.infoservice.PerformanceInfo;

public class PassiveInfoServiceMainUpdater extends AbstractDatabaseUpdater
{
	
	PerformanceInfoUpdater m_performanceInfoUpdater = null;
	PassiveInfoServiceCascadeUpdater m_cascadeUpdater = null;
	
	public PassiveInfoServiceMainUpdater(long interval) 
	{
		super(interval);
		m_performanceInfoUpdater = new PerformanceInfoUpdater(Long.MAX_VALUE);
		m_cascadeUpdater = new PassiveInfoServiceCascadeUpdater(Long.MAX_VALUE);
	}
	
	public PassiveInfoServiceMainUpdater() 
	{
		this(Long.MAX_VALUE);
	}
	
	/**
	 * fetches several Infos needed for the passive InfoService:
	 * 1. PerformanceInfos (to calculate the PerformanceEntries)
	 * 2. MixCascades
	 * 3. ExitAddresses (depends on MixCascades)
	 * 4. finally PerformanceEntries are calulated and returned.
	 */
	protected Hashtable getUpdatedEntries(Hashtable toUpdate)
	{	
		/* 1. PerformanceInfos Database update (to calculate the PerformanceEntries) */
		m_performanceInfoUpdater.update();
		/* 2. MixCascades Database update */
		m_cascadeUpdater.update();
		/* 3. Exit addresses Database update */
		InfoServiceHolder.getInstance().getExitAddresses();
		/* now calculate lowest bounds of the performance entries */
		Hashtable cascadeInfos = null;
		Database cascadeDB = Database.getInstance(MixCascade.class);
		cascadeInfos = cascadeDB.getEntryHash();
		
		if(cascadeInfos != null)
		{
			
			Enumeration cascades = cascadeInfos.elements();
			
			if(cascades != null)
			{
				Hashtable performanceEntries = new Hashtable();
				String currentCascadeId = null;
				PerformanceEntry currentPerformanceEntry = null;
				
				while(cascades.hasMoreElements())
				{
					currentCascadeId = ((MixCascade) cascades.nextElement()).getId();
					currentPerformanceEntry = PerformanceInfo.getLowestCommonBoundEntry(currentCascadeId);
					if(currentPerformanceEntry != null)
					{
						performanceEntries.put(currentCascadeId, currentPerformanceEntry);
					}
				}
				return performanceEntries;
			}
			
		}
		return null;
	}
	
	public Class getUpdatedClass() 
	{
		return PerformanceEntry.class;
	}


	
	protected Hashtable getEntrySerials() 
	{
		return new Hashtable();
	}

}
