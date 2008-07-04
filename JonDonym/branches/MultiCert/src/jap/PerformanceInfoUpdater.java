package jap;

import java.util.Enumeration;
import java.util.Hashtable;

import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;
import anon.infoservice.PerformanceInfo;
import anon.infoservice.AbstractDatabaseEntry;
import anon.infoservice.Database;
import anon.infoservice.InfoServiceHolder;
import anon.infoservice.MixCascade;

public class PerformanceInfoUpdater extends AbstractDatabaseUpdater 
{
	private static final long UPDATE_INTERVAL = 1000 * 60 * 5; // 5 minutes
	
	public PerformanceInfoUpdater()
	{
		super(new DynamicUpdateInterval(UPDATE_INTERVAL));
	}
	
	protected Hashtable getEntrySerials() 
	{
		return new Hashtable();
	}
	
	protected Hashtable getUpdatedEntries(Hashtable toUpdate) 
	{
		Hashtable hashtable = InfoServiceHolder.getInstance().getPerformanceInfos();
		if (hashtable == null)
		{
			return new Hashtable();
		}

		return hashtable;
	}

	public Class getUpdatedClass() 
	{
		return PerformanceInfo.class;
	}
}
