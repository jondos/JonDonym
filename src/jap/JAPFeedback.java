/*
 Copyright (c) 2000 - 2004, The JAP-Team
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

import java.util.Hashtable;

import anon.infoservice.StatusInfo;
import anon.infoservice.update.AbstractDatabaseUpdater;

final class JAPFeedback extends AbstractDatabaseUpdater
{
	public static final long UPDATE_INTERVAL_MS = 70000l;
	private static final long MIN_UPDATE_INTERVAL_MS = 20000l;

	private DynamicUpdateInterval m_updateInterval;

	public JAPFeedback()
	{
		super(new DynamicUpdateInterval(UPDATE_INTERVAL_MS), new ObservableInfo(
				JAPController.getInstance().getObservableInfo().getObservable())
		{
			public Integer getUpdateChanged()
			{
				return JAPController.getInstance().getObservableInfo().getUpdateChanged();
			}
			public boolean isUpdateDisabled()
			{
				return JAPController.getInstance().getObservableInfo().isUpdateDisabled();
			}
			
			public void notifyAdditionalObserversOnUpdate(Class a_updatedClass)
			{
				JAPController.getInstance().getObservableInfo().notifyAdditionalObserversOnUpdate(a_updatedClass);
			}
			
			public boolean updateImmediately()
			{
				return true;
			}
		});
		m_updateInterval = (DynamicUpdateInterval)getUpdateInterval();
	}

	public Class getUpdatedClass()
	{
		return StatusInfo.class;
	}

	protected boolean doCleanup(Hashtable a_newEntries)
	{
		// no cleanup is needed as the status info has a short timeout
		return false;
	}

	protected boolean isUpdatePaused()
	{
		return !JAPController.getInstance().getAnonMode() ||
			JAPController.getInstance().getCurrentMixCascade().isUserDefined();
	}

	protected Hashtable getUpdatedEntries(Hashtable a_dummy)
	{
		StatusInfo info = JAPController.getInstance().getCurrentMixCascade().fetchCurrentStatus();
		Hashtable hashtable = new Hashtable();
		if (info != null)
		{
			if (info.getExpireTime() <= (System.currentTimeMillis() + UPDATE_INTERVAL_MS))
			{
				m_updateInterval.setUpdateInterval(MIN_UPDATE_INTERVAL_MS);
			}
			else if (info.getExpireTime() <=
					 (System.currentTimeMillis() + (1.5d * UPDATE_INTERVAL_MS)))
			{
				m_updateInterval.setUpdateInterval(Math.max(UPDATE_INTERVAL_MS / 2, MIN_UPDATE_INTERVAL_MS));
			}
			else
			{
				m_updateInterval.setUpdateInterval(UPDATE_INTERVAL_MS);
			}
			hashtable.put(info.getId(), info);
		}
		else
		{
			m_updateInterval.setUpdateInterval(MIN_UPDATE_INTERVAL_MS);
		}
		return hashtable;
	}

	protected Hashtable getEntrySerials()
	{
		return new Hashtable();
	}
}
