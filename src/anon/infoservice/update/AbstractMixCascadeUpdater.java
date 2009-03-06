/*
 Copyright (c) 2006, The JAP-Team
 All rights reserved.
 Redistribution and use in source and binary forms, with or without modification,
 are permitted provided that the following conditions are met:

  - Redistributions of source code must retain the above copyright notice,
 this list of conditions and the following disclaimer.

  - Redistributions in bisnary form must reproduce the above copyright notice,
 this list of conditions and the following disclaimer in the documentation and/or
 other materials provided with the distribution.

  - Neither the name of the University of Technology Dresden, Germany nor the names of its contributors
 may be used to endorse or promote products derived from this software without specific
 prior written permission.


 THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS ``AS IS'' AND ANY EXPRESS
 OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY
 AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE REGENTS OR CONTRIBUTORS
 BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA,
 OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER
 IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
 OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE
 */
package anon.infoservice.update;

import jap.JAPController;

import java.util.Hashtable;
import java.util.Vector;

import anon.client.TrustModel;
import anon.infoservice.AbstractDatabaseEntry;
import anon.infoservice.Database;
import anon.infoservice.InfoServiceHolder;
import anon.infoservice.MixCascade;
import anon.infoservice.MixInfo;
import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;
import java.util.Enumeration;
import anon.infoservice.StatusInfo;
import anon.infoservice.update.AbstractDatabaseUpdater;

/**
 * Updates the list of available MixCascades.
 * @author Rolf Wendolsky
 */
public abstract class AbstractMixCascadeUpdater extends AbstractDatabaseUpdater
{
	private static final long UPDATE_INTERVAL_MS = 4 * 60000l; //every four minutes
	private static final long MIN_UPDATE_INTERVAL_MS = 30000l;
	private boolean m_bDoMixInfoCleanup = true;

	public AbstractMixCascadeUpdater(ObservableInfo a_observableInfo)
	{
		super(new DynamicUpdateInterval(UPDATE_INTERVAL_MS), a_observableInfo);
	}

	public AbstractMixCascadeUpdater(long interval, boolean a_bDoMixInfoCleanup, ObservableInfo a_observableInfo)
	{
		super(interval, a_observableInfo);
		m_bDoMixInfoCleanup = a_bDoMixInfoCleanup;
	}
	
	protected abstract AbstractDatabaseEntry getPreferredEntry();
	
	protected abstract void setPreferredEntry(AbstractDatabaseEntry a_preferredEntry);
	
	
	public final Class getUpdatedClass()
	{
		return MixCascade.class;
	}

	/**
	 * Removes all MixInfo entries that exist without a cascade.
	 */
	protected final boolean doCleanup(Hashtable a_newEntries)
	{
		boolean bUpdated = super.doCleanup(a_newEntries);
		MixCascade preferredEntry = (MixCascade)getPreferredEntry();
		
		if (m_bDoMixInfoCleanup)
		{
			LogHolder.log(LogLevel.DEBUG, LogType.MISC, "Do MixInfo database cleanup.");
	
			Vector mixes = Database.getInstance(MixInfo.class).getEntryList();
			Vector cascades = Database.getInstance(MixCascade.class).getEntryList();
			// do not remove mixes of current cascade
			if (preferredEntry != null)
			{
				cascades.addElement(preferredEntry);
			}
			MixInfo currentMix;
			Vector currentCascadeMixes;
	
			loop:
			for (int i = 0; i < mixes.size(); i++)
			{
				currentMix = (MixInfo)mixes.elementAt(i);
				if (Database.getInstance(MixCascade.class).getEntryById(currentMix.getId()) != null ||
					(preferredEntry != null && preferredEntry.getMixIds().elementAt(0).equals(currentMix.getId())))
				{
					continue;
				}
				for (int j = 0; j < cascades.size(); j++)
				{
					currentCascadeMixes = ((MixCascade)cascades.elementAt(j)).getMixIds();
					for (int k = 1; k < currentCascadeMixes.size(); k++)
					{
						if (currentCascadeMixes.elementAt(k).equals(currentMix.getId()))
						{
							continue loop;
						}
					}
				}
				Database.getInstance(MixInfo.class).remove(currentMix);
				LogHolder.log(LogLevel.NOTICE, LogType.MISC, "Cleaned MixInfo DB entry: " + currentMix.getId());
			}
		}

		return bUpdated;
	}

	protected final Hashtable getEntrySerials()
	{
		Hashtable result = 
			InfoServiceHolder.getInstance().getMixCascadeSerials(TrustModel.getContext());
		if (getUpdateInterval() instanceof DynamicUpdateInterval)
		{
			if (result == null)
			{
				((DynamicUpdateInterval)getUpdateInterval()).setUpdateInterval(MIN_UPDATE_INTERVAL_MS);
			}
			else
			{
				((DynamicUpdateInterval)getUpdateInterval()).setUpdateInterval(UPDATE_INTERVAL_MS);
			}
		}
		
		return result;
	}

	protected Hashtable getUpdatedEntries(Hashtable a_entriesToUpdate)
	{
		Hashtable updatedCascades;
		Hashtable cascadesInDB = Database.getInstance(MixCascade.class).getEntryHash();
		Enumeration enumCascades = cascadesInDB.elements();
		MixCascade currentCascade;
		
		// fetch the current status entries first for a visual speed-up
		updatedCascades = new Hashtable();
		while (enumCascades.hasMoreElements())
		{			
			currentCascade = (MixCascade)enumCascades.nextElement();
			if (fetchCurrentStatus(currentCascade))
			{
				updatedCascades.put(currentCascade.getId(), currentCascade);
			}
		}
		cascadesInDB = updatedCascades;
		
		updatedCascades = getUpdatedEntries_internal(a_entriesToUpdate);
		
		enumCascades = updatedCascades.elements();
		while (enumCascades.hasMoreElements())
		{
			currentCascade = (MixCascade)enumCascades.nextElement();
			if (!cascadesInDB.contains(currentCascade))
			{
				fetchCurrentStatus(currentCascade);
			}
		}
		
		return updatedCascades;
	}
	
	private final boolean fetchCurrentStatus(MixCascade a_cascade)
	{
		StatusInfo info = null;
		if (!a_cascade.isUserDefined())
		{
			info = a_cascade.fetchCurrentStatus(UPDATE_INTERVAL_MS * KEEP_ENTRY_FACTOR);
			return Database.getInstance(StatusInfo.class).update(info);
		}
		return info != null;
	}

	
	
	protected final Hashtable getUpdatedEntries_internal(Hashtable a_entriesToUpdate)
	{
		Hashtable result;
		if (a_entriesToUpdate == null)
		{
			result = InfoServiceHolder.getInstance().getMixCascades(TrustModel.getContext());
		}
		else if (a_entriesToUpdate.size() == 0)
		{
			result = new Hashtable();
		}
		else
		{
			Hashtable returnedEntries = new Hashtable(a_entriesToUpdate.size());
			Enumeration enumEntriesToUpdate = a_entriesToUpdate.keys();
			MixCascade cascade;
			while (enumEntriesToUpdate.hasMoreElements())
			{
				cascade = InfoServiceHolder.getInstance().getMixCascadeInfo(
								(String)enumEntriesToUpdate.nextElement());
				if (cascade != null)
				{
					returnedEntries.put(cascade.getId(), cascade);
				}
			}
			result = returnedEntries;
		}
		return result;
	}
}
