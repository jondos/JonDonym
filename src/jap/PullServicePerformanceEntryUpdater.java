package jap;

import java.util.Enumeration;
import java.util.Hashtable;

import anon.infoservice.Database;
import anon.infoservice.MixCascade;
import anon.infoservice.PerformanceEntry;
import anon.infoservice.PerformanceInfo;

public class PullServicePerformanceEntryUpdater extends MixCascadeUpdater
{

	public PullServicePerformanceEntryUpdater() 
	{
		super();
	}
	
	protected Hashtable getUpdatedEntries(Hashtable toUpdate)
	{
		Hashtable cascadeInfos = null;
		Database cascadeDB = Database.getInstance(MixCascade.class);
		if(cascadeDB == null || cascadeDB.getNumberOfEntries() == 0)
		{
			cascadeInfos = super.getUpdatedEntries(toUpdate);
		}
		else
		{
			cascadeInfos = cascadeDB.getEntryHash();
		}
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

}
