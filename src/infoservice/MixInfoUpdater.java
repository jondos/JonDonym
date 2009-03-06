package infoservice;

import java.util.Hashtable;

import anon.infoservice.InfoServiceHolder;
import anon.infoservice.MixInfo;
import anon.infoservice.update.AbstractDatabaseUpdater;
import anon.util.Updater.ObservableInfo;

public class MixInfoUpdater extends AbstractDatabaseUpdater 
{
	public MixInfoUpdater(ObservableInfo a_observableInfo) 
	{
		super(Long.MAX_VALUE, a_observableInfo);
	}

	protected Hashtable getEntrySerials() 
	{
		return new Hashtable();
	}
	
	protected Hashtable getUpdatedEntries(Hashtable toUpdate) 
	{
		return InfoServiceHolder.getInstance().getMixInfos();
	}

	
	public Class getUpdatedClass() 
	{
		return MixInfo.class;
	}
}
