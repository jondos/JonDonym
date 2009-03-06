package infoservice;

import jap.MixCascadeUpdater;

import java.util.Hashtable;

public class PassiveInfoServiceCascadeUpdater extends MixCascadeUpdater {

	public PassiveInfoServiceCascadeUpdater(long interval, ObservableInfo a_observableInfo) 
	{
		super(interval, false, a_observableInfo);
	}

	public PassiveInfoServiceCascadeUpdater(ObservableInfo a_observableInfo) 
	{
		super(a_observableInfo);
	}
	
	protected Hashtable getUpdatedEntries(Hashtable a_entriesToUpdate)
	{
		return getUpdatedEntries_internal(a_entriesToUpdate);
	}

}
