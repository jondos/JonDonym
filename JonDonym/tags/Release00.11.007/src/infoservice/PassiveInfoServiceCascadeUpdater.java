package infoservice;


import java.util.Hashtable;

import anon.infoservice.AbstractDatabaseEntry;
import anon.infoservice.update.AbstractMixCascadeUpdater;

public class PassiveInfoServiceCascadeUpdater extends AbstractMixCascadeUpdater 
{
	private AbstractDatabaseEntry m_preferred;
	public PassiveInfoServiceCascadeUpdater(ObservableInfo a_observableInfo) 
	{
		super(Long.MAX_VALUE, false, a_observableInfo);
	}
	
	protected AbstractDatabaseEntry getPreferredEntry()
	{
		return m_preferred;
	}
	
	protected void setPreferredEntry(AbstractDatabaseEntry a_entry)
	{
		m_preferred = a_entry;
	}
	
	protected Hashtable getUpdatedEntries(Hashtable a_entriesToUpdate)
	{
		return getUpdatedEntries_internal(a_entriesToUpdate);
	}

}
