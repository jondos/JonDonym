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
	private static final long UPDATE_INTERVAL_MS_SHORT = 1000 * 60; // 1 minute
	
	public PerformanceInfoUpdater()
	{
		super(new DynamicUpdateInterval(UPDATE_INTERVAL_MS_SHORT));
	}
	
	@Override
	protected Hashtable getEntrySerials() 
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected Hashtable getUpdatedEntries(Hashtable toUpdate) 
	{
		Hashtable hashtable = InfoServiceHolder.getInstance().getPerformanceInfos();
		if (hashtable == null)
		{
			return new Hashtable();
		}

		return hashtable;
	}
	
	protected void updateInternal()
	{
		if (Thread.currentThread().isInterrupted())
		{
			// this thread is being stopped; ignore this error
			m_successfulUpdate = true;
			return;
		}
		
		Hashtable newEntries = getUpdatedEntries(null);
		if (Thread.currentThread().isInterrupted())
		{
			// this thread is being stopped; ignore this error
			m_successfulUpdate = true;
		}
		else if (newEntries == null)
		{
			LogHolder.log(LogLevel.ERR, LogType.THREAD, getUpdatedClassName() + "update failed!");
			m_successfulUpdate = false;

		}
		else
		{
			LogHolder.log(LogLevel.DEBUG, LogType.THREAD,
						  getUpdatedClassName() + "update was successful.");
			boolean updated = false;
			m_bFirstUpdateDone = true; // indicate that at least one update was successful
			m_successfulUpdate = true;
			/* we have successfully downloaded the requested database entries
			 * -> update the internal database
			 */
			Enumeration entries = newEntries.elements();
			while (entries.hasMoreElements())
			{

				AbstractDatabaseEntry currentEntry = (AbstractDatabaseEntry) (entries.nextElement());
				if (Database.getInstance(getUpdatedClass()).update(currentEntry))
				{
					updated = true;
				}
			}
			
			updated = doCleanup(newEntries) || updated;

			if ((getUpdatedClass() == MixCascade.class) && updated)
			{
				JAPController.getInstance().notifyJAPObservers();
			}
		}
	}

	@Override
	public Class getUpdatedClass() {
		// TODO Auto-generated method stub
		return PerformanceInfo.class;
	}

}
