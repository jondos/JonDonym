package jap;

import java.util.Hashtable;

public class PassiveInfoServiceCascadeUpdater extends MixCascadeUpdater {

	public PassiveInfoServiceCascadeUpdater(long interval) 
	{
		super(interval, false);
	}

	public PassiveInfoServiceCascadeUpdater() 
	{
		super();
	}
	
	protected Hashtable getUpdatedEntries(Hashtable a_entriesToUpdate)
	{
		return getUpdatedEntries_internal(a_entriesToUpdate);
	}

}
