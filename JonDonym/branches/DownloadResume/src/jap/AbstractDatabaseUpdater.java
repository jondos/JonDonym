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
package jap;

import java.util.Enumeration;
import java.util.Observable;
import java.util.Hashtable;

import anon.util.Updater;
import anon.infoservice.AbstractDatabaseEntry;
import anon.infoservice.Database;
import anon.infoservice.MixCascade;
import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;

/**
 * Updates the local database. This may be done automatically (by a background thread) and manually
 * by method call. The automatic update is only done if this is allowed by the model.
 * @author Rolf Wendolsky
 */
public abstract class AbstractDatabaseUpdater extends Updater
{
	/**
	 * Keep all entries KEEP_ENTRY_FACTOR times of the update interval, even if the InfoService
	 * does not hold this object any longer. If the update interval is 5 minutes, for example,
	 * each entry read into the JAP database is kept for at least 15 minutes before it is deleted (if
	 * the database does not delete it before).
	 */
	public static final long KEEP_ENTRY_FACTOR = 3l;

	private boolean m_successfulUpdate = false;
	private boolean m_bFirstUpdateDone = false;

	/**
	 * Initialises and starts the database update thread.
	 */
	public AbstractDatabaseUpdater(long a_updateInterval)
	{
		this (new ConstantUpdateInterval(a_updateInterval));
	}
	
	/**
	 * Initialises and starts the database update thread.
	 */
	public AbstractDatabaseUpdater(IUpdateInterval a_updateInterval)
	{
		super(a_updateInterval, new ObservableInfo(JAPModel.getInstance())
		{
			public Integer getUpdateChanged()
			{
				return JAPModel.CHANGED_INFOSERVICE_AUTO_UPDATE;
			}
			public boolean isUpdateDisabled()
			{
				return JAPModel.getInstance().isInfoServiceDisabled();
			}
		});
	}

	/**
	 * Does the update. Subclasses may overwrite to, for example, synchronize the update with another object.
	 */
	protected void updateInternal()
	{
		Hashtable serials = getEntrySerials();
		Hashtable hashEntriesToUpdate;
		Hashtable hashEntriesKept;
		String key;
		AbstractDatabaseEntry entry;
		Enumeration enumSerials;

		if (Thread.currentThread().isInterrupted())
		{
			// this thread is being stopped; ignore this error
			m_successfulUpdate = true;
			return;
		}
		else if (serials == null)
		{
			LogHolder.log(LogLevel.ERR, LogType.THREAD, getUpdatedClassName() + "update failed!");
			m_successfulUpdate = false;
			return;
		}

		if (serials.size() > 0)
		{
			hashEntriesToUpdate = new Hashtable(serials.size());
			hashEntriesKept = new Hashtable(serials.size());
		}
		else
		{
			hashEntriesToUpdate = new Hashtable();
			hashEntriesKept = new Hashtable();
		}

		enumSerials = serials.keys();
		while (enumSerials.hasMoreElements())
		{
			key = (String)enumSerials.nextElement();
			entry = Database.getInstance(getUpdatedClass()).getEntryById(key);
			if (entry != null &&
				((AbstractDatabaseEntry)serials.get(key)).getVersionNumber() == entry.getVersionNumber())
			{
				// do not update this entry
				entry.resetCreationTime();
				hashEntriesKept.put(entry.getId(), entry);
			}
			else
			{
				// force update of this entry
				hashEntriesToUpdate.put(key, serials.get(key));
			}
		}

		if (Thread.currentThread().isInterrupted())
		{
			// this thread is being stopped; ignore this error
			m_successfulUpdate = true;
			return;
		}

		// get the entries
		Hashtable newEntries;
		if (hashEntriesKept.size() == 0)
		{
			newEntries = getUpdatedEntries(null);
		}
		else
		{
			newEntries = getUpdatedEntries(hashEntriesToUpdate);
		}

		// add the entries that have not changed
		if (newEntries != null)
		{
			Enumeration enumEntriesKept = hashEntriesKept.keys();
			while (enumEntriesKept.hasMoreElements())
			{
				key = (String) enumEntriesKept.nextElement();
				newEntries.put(key, hashEntriesKept.get(key));
			}
		}

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
				AbstractDatabaseEntry preferredEntry = getPreferredEntry();
				if (preferredEntry != null)
				{
					/* if the current entry is equal to the preferred entry,
					 * update the preferred entry, too
					 */
					if (preferredEntry.equals(currentEntry))
					{
						setPreferredEntry(currentEntry);
					}
				}
			}

			updated = doCleanup(newEntries) || updated;

			if ((getUpdatedClass() == MixCascade.class) && updated)
			{
				JAPController.getInstance().notifyJAPObservers();
			}
		}
	}

	protected boolean wasUpdateSuccessful()
	{
		return m_successfulUpdate;
	}


	/**
	 * Does some cleaup operations of the database. All old entries that were not updated by
	 * the new entries are removed. Subclasses may overwrite this method to suppress or alter this
	 * behaviour. This method is called by updateInternal().
	 * @param a_newEntries the list of new entries
	 * @return boolean
	 */
	protected boolean doCleanup(Hashtable a_newEntries)
	{
		boolean bUpdated = false;

		/* now remove all non user-defined infoservices, which were not updated, from the
		 * database of known infoservices
		 */
		Enumeration knownDBEntries = Database.getInstance(getUpdatedClass()).getEntryList().elements();
		while (knownDBEntries.hasMoreElements())
		{
			AbstractDatabaseEntry currentEntry = (AbstractDatabaseEntry) (knownDBEntries.nextElement());
			if (!currentEntry.isUserDefined() && !a_newEntries.contains(currentEntry) &&
				(currentEntry.getCreationTime() +  KEEP_ENTRY_FACTOR * getUpdateInterval().getUpdateInterval()) <
				System.currentTimeMillis())
			{
				/* the db entry was fetched from the Internet earlier, but it is not
				 * in the list fetched from the Internet this time
				 * -> remove that db entry from the database of known db entries, but only if it is
				 * too old (this should make the db more robust against IS errors)
				 */
				if (Database.getInstance(getUpdatedClass()).remove(currentEntry))
				{
					bUpdated = true;
				}
			}
		}
		return bUpdated;
	}

	public boolean isFirstUpdateDone()
	{
		return m_bFirstUpdateDone;
	}

	protected  AbstractDatabaseEntry getPreferredEntry()
	{
		return null;
	}
	protected void setPreferredEntry(AbstractDatabaseEntry a_preferredEntry)
	{
	}

	protected abstract Hashtable getEntrySerials();

	protected abstract Hashtable getUpdatedEntries(Hashtable a_entriesToUpdate);
}
