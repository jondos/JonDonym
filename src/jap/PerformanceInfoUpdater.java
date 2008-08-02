package jap;

import java.util.Hashtable;

import anon.infoservice.PerformanceInfo;
import anon.infoservice.InfoServiceHolder;

public class PerformanceInfoUpdater extends AbstractDatabaseUpdater 
{
	private static final long UPDATE_INTERVAL = 1000 * 60 * 5; // 5 minutes
	private static final long MIN_UPDATE_INTERVAL_MS = 20000l;
	
	
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
			((DynamicUpdateInterval)getUpdateInterval()).setUpdateInterval(MIN_UPDATE_INTERVAL_MS);
			return new Hashtable();
		}
		else
		{
			((DynamicUpdateInterval)getUpdateInterval()).setUpdateInterval(UPDATE_INTERVAL);
		}

		return hashtable;
	}

	public Class getUpdatedClass() 
	{
		return PerformanceInfo.class;
	}
}
