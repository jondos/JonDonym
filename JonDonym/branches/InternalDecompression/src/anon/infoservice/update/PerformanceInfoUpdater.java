package anon.infoservice.update;

import java.util.Hashtable;

import anon.infoservice.PerformanceInfo;
import anon.infoservice.InfoServiceHolder;

public class PerformanceInfoUpdater extends AbstractDatabaseUpdater
{
	private static final long UPDATE_INTERVAL = 1000 * 60 * 4; // 4 minutes
	private static final long MIN_UPDATE_INTERVAL_MS = 20000l;
	
	
	public PerformanceInfoUpdater(ObservableInfo a_observableInfo)
	{
		super(new DynamicUpdateInterval(UPDATE_INTERVAL), a_observableInfo);
	}
	
	public PerformanceInfoUpdater(long interval, ObservableInfo a_observableInfo)
	{
		super(interval, a_observableInfo);
	}
	
	protected Hashtable getEntrySerials() 
	{
		return new Hashtable();
	}
	
	protected Hashtable getUpdatedEntries(Hashtable toUpdate) 
	{
		Hashtable hashtable = InfoServiceHolder.getInstance().getPerformanceInfos();
		if (getUpdateInterval() instanceof DynamicUpdateInterval)
		{
			if (hashtable == null)
			{
				((DynamicUpdateInterval)getUpdateInterval()).setUpdateInterval(MIN_UPDATE_INTERVAL_MS);
			}
			else
			{
				((DynamicUpdateInterval)getUpdateInterval()).setUpdateInterval(UPDATE_INTERVAL);
			}
		}

		return hashtable;
	}

	public Class getUpdatedClass() 
	{
		return PerformanceInfo.class;
	}
}
