package jap;

import java.util.Enumeration;
import java.util.Hashtable;

import anon.infoservice.InfoServiceHolder;
import anon.infoservice.MixCascade;
import anon.infoservice.StatusInfo;

public class PassiveInfoServiceStatusUpdater extends AbstractDatabaseUpdater 
{
	private static final long UPDATE_INTERVAL_MS = 7000l;
	
	public PassiveInfoServiceStatusUpdater() 
	{
		super(UPDATE_INTERVAL_MS);
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
