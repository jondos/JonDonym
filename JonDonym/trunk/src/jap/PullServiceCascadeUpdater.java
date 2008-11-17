package jap;

import java.util.Hashtable;

public class PullServiceCascadeUpdater extends MixCascadeUpdater {

	public PullServiceCascadeUpdater() 
	{
		super();
	}
	
	protected Hashtable getUpdatedEntries(Hashtable a_entriesToUpdate)
	{
		return getUpdatedEntries_internal(a_entriesToUpdate);
	}

}
