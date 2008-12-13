package jap;

import java.util.Hashtable;

import anon.infoservice.InfoServiceHolder;
import anon.infoservice.MixInfo;

public class MixInfoUpdater extends AbstractDatabaseUpdater 
{
	public MixInfoUpdater() 
	{
		super(Long.MAX_VALUE);
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
