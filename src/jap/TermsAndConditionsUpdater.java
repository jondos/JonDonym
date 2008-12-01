package jap;

import java.util.Enumeration;
import java.util.Hashtable;

import anon.infoservice.TermsAndConditions;
import anon.infoservice.InfoServiceHolder;
import anon.infoservice.TermsAndConditionsFramework;

import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;

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
		
		Enumeration e = hashtable.elements();
		while(e.hasMoreElements())
		{
			TermsAndConditions tc = (TermsAndConditions) e.nextElement();
			TermsAndConditionsFramework frm = TermsAndConditionsFramework.getById(tc.getReferenceId());
			if(frm == null)
			{
				LogHolder.log(LogLevel.DEBUG, LogType.NET, "Droping T&C object " + tc.getId() + " because the t&c framework with the reference id " + tc.getReferenceId() + " could not be retrieved.");
				// remove the t&c
				hashtable.remove(e);
			}
		}
		
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
