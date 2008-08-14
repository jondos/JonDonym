/*
 Copyright (c) 2000 - 2005, The JAP-Team
 All rights reserved.
 Redistribution and use in source and binary forms, with or without modification,
 are permitted provided that the following conditions are met:

  - Redistributions of source code must retain the above copyright notice,
 this list of conditions and the following disclaimer.

  - Redistributions in binary form must reproduce the above copyright notice,
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
import java.util.Vector;

import anon.infoservice.Database;
import anon.infoservice.InfoServiceDBEntry;
import anon.infoservice.InfoServiceHolder;
import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;

/**
 * This is the implementation for the infoservice savepoint. It is needed for restoring an old or
 * the default configuration, if the user presses "Cancel" or "Reset to defaults".
 */
public class JAPConfInfoServiceSavePoint implements IJAPConfSavePoint
{

	/**
	 * The Vector of all known infoservices.
	 */
	private Vector m_knownInfoServices;

	/**
	 * The preferred infoservice.
	 */
	private InfoServiceDBEntry m_preferredInfoService;

	/**
	 * Whether automatic infoservice requests are disabled or not.
	 */
	private boolean m_automaticInfoServiceRequestsDisabled;

	/**
	 * Whether automatic changes of infoservice are enabled (if the default infoservice fails).
	 */
	private boolean m_automaticInfoServiceChanges;

	/**
	 * This method will store the current infoservice configuration in this savepoint.
	 */
	public void createSavePoint()
	{
		m_knownInfoServices = Database.getInstance(InfoServiceDBEntry.class).getEntryList();
		m_preferredInfoService = InfoServiceHolder.getInstance().getPreferredInfoService();
		m_automaticInfoServiceRequestsDisabled = JAPModel.isInfoServiceDisabled();
		m_automaticInfoServiceChanges = InfoServiceHolder.getInstance().isChangeInfoServices();
	}

	/**
	 * Restores the old infoservice configuration (stored with the last call of createSavePoint()).
	 */
	public void restoreSavePoint()
	{
		/* update the database of known infoservices */
		Enumeration infoServices = m_knownInfoServices.elements();
		while (infoServices.hasMoreElements())
		{
			InfoServiceDBEntry currentInfoService = (InfoServiceDBEntry) (infoServices.nextElement());
			Database.getInstance(InfoServiceDBEntry.class).update(currentInfoService);
		}
		/* now remove all infoservices which were not in the stored list */
		Enumeration allInfoServices = Database.getInstance(InfoServiceDBEntry.class).getEntryList().elements();
		while (allInfoServices.hasMoreElements())
		{
			InfoServiceDBEntry currentInfoService = (InfoServiceDBEntry) (allInfoServices.nextElement());
			if (m_knownInfoServices.contains(currentInfoService) == false)
			{
				/* the current infoservice is not in the stored list -> remove it from the database */
				Database.getInstance(InfoServiceDBEntry.class).remove(currentInfoService);
			}
		}
		InfoServiceHolder.getInstance().setPreferredInfoService(m_preferredInfoService);
		JAPController.getInstance().setInfoServiceDisabled(m_automaticInfoServiceRequestsDisabled);
		InfoServiceHolder.getInstance().setChangeInfoServices(m_automaticInfoServiceChanges);
	}

	/**
	 * Loads the default infoservice configuration.
	 */
	public void restoreDefaults()
	{
		/* remove all infoservices from database and set the default infoservice as preferred
		 * infoservice
		 */
		synchronized (InfoServiceHolder.getInstance())
		{
			Database.getInstance(InfoServiceDBEntry.class).removeAll();
			try
			{
				InfoServiceDBEntry[] defaultInfoService = JAPController.createDefaultInfoServices();
				for (int i = 0; i < defaultInfoService.length; i++)
				{
					Database.getInstance(InfoServiceDBEntry.class).update(defaultInfoService[i]);
				}
				InfoServiceHolder.getInstance().setPreferredInfoService(defaultInfoService[0]);
			}
			catch (Exception e)
			{
				/* should not happen, if it happens, we can't do anything */
				LogHolder.log(LogLevel.EXCEPTION, LogType.MISC, "Cannot create the default infoservice.");
			}
			JAPController.getInstance().setInfoServiceDisabled(JAPConstants.DEFAULT_INFOSERVICE_DISABLED);
			InfoServiceHolder.getInstance().setChangeInfoServices(InfoServiceHolder.DEFAULT_INFOSERVICE_CHANGES);
		}
	}

}
