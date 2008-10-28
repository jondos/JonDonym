package jap;

import java.util.Hashtable;

import anon.infoservice.TermsAndConditions;
import anon.infoservice.InfoServiceHolder;

public class TermsAndConditionsUpdater extends AbstractDatabaseUpdater 
{
	private static final long UPDATE_INTERVAL_MS = 1000 * 60 * 60 * 1l ; // one hour
	private static final long UPDATE_INTERVAL_MS_SHORT = 1000 * 60 * 10l; // 10 minutes

	public TermsAndConditionsUpdater()
	{
		super(new DynamicUpdateInterval(UPDATE_INTERVAL_MS_SHORT));
	}
	
	public Class getUpdatedClass()
	{
		return TermsAndConditions.class;
	}
	
	protected Hashtable getUpdatedEntries(Hashtable a_dummy)
	{
		Hashtable hashtable = InfoServiceHolder.getInstance().getTermsAndConditions();
		if (hashtable == null)
		{
			((DynamicUpdateInterval)getUpdateInterval()).setUpdateInterval(UPDATE_INTERVAL_MS_SHORT);
			return new Hashtable();
		}
		((DynamicUpdateInterval)getUpdateInterval()).setUpdateInterval(UPDATE_INTERVAL_MS);
		return hashtable;
	}
	
	protected Hashtable getEntrySerials()
	{
		Hashtable hashtable = InfoServiceHolder.getInstance().getMessageSerials();
		if (hashtable == null)
		{
			((DynamicUpdateInterval)getUpdateInterval()).setUpdateInterval(UPDATE_INTERVAL_MS_SHORT);
			return new Hashtable();
		}
		((DynamicUpdateInterval)getUpdateInterval()).setUpdateInterval(UPDATE_INTERVAL_MS);
		return hashtable;
	}
}
