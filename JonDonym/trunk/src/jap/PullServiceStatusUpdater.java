package jap;

import java.util.Enumeration;
import java.util.Hashtable;

import anon.infoservice.InfoServiceHolder;
import anon.infoservice.MixCascade;
import anon.infoservice.StatusInfo;

public class PullServiceStatusUpdater extends AbstractDatabaseUpdater 
{
	public static final long UPDATE_INTERVAL_MS = 7000l;
	
	private DynamicUpdateInterval m_updateInterval;
	
	public PullServiceStatusUpdater(long interval) 
	{
		super(interval);
	}

	public PullServiceStatusUpdater(IUpdateInterval interval) 
	{
		super(interval);
	}

	
	public PullServiceStatusUpdater() 
	{
		
		super(new DynamicUpdateInterval(UPDATE_INTERVAL_MS));
		m_updateInterval = (DynamicUpdateInterval)getUpdateInterval();
		
	}

	protected Hashtable getEntrySerials() 
	{
		return new Hashtable();
	}

	
	protected Hashtable getUpdatedEntries(Hashtable toUpdate) 
	{
		
		Hashtable t = InfoServiceHolder.getInstance().getMixCascades();
		Hashtable statusTable = new Hashtable();
		MixCascade currentCascade = null;
		StatusInfo currentInfo = null;
		if( t != null)
		{
			Enumeration entries = t.elements();
			if(entries != null)
			{
				while(entries.hasMoreElements())
				{
					currentCascade = (MixCascade) entries.nextElement();
					if(currentCascade != null)
					{
						currentInfo = currentCascade.fetchCurrentStatus();
						if(currentInfo != null)
						{
							statusTable.put(currentInfo.getId(), currentInfo);
						}
					}
				}
			}
		}
		/*end new*/
		m_updateInterval.setUpdateInterval(UPDATE_INTERVAL_MS);
		return statusTable;
	}

	
	public Class getUpdatedClass() 
	{
		return StatusInfo.class;
	}
	
	protected boolean doCleanup(Hashtable a_newEntries)
	{
		return false;
	}

}
