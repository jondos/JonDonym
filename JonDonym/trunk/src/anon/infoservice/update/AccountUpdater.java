/*
Copyright (c) 2007, The JAP-Team, JonDos GmbH
All rights reserved.
Redistribution and use in source and binary forms, with or without modification,
are permitted provided that the following conditions are met:

 - Redistributions of source code must retain the above copyright notice,
this list of conditions and the following disclaimer.

 - Redistributions in binary form must reproduce the above copyright notice,
this list of conditions and the following disclaimer in the documentation and/or
other materials provided with the distribution.

 - Neither the name of the University of Technology Dresden, Germany nor the name of
the JonDos GmbH, nor the names of their contributors may be used to endorse or
promote products derived from this software without specific prior written permission.

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

import anon.util.Updater;
import anon.pay.xml.XMLAccountInfo;
import logging.LogLevel;
import logging.LogHolder;
import java.util.Enumeration;
import anon.pay.PayAccountsFile;
import anon.pay.PayAccount;
import logging.LogType;


public class AccountUpdater extends Updater
{
	private boolean m_successfulUpdate = false;
	private boolean m_bFirstUpdateDone = false;

	public AccountUpdater()
	{
		super(new IUpdateInterval()
		{
			public long getUpdateInterval()
			{
				return PayAccount.ACCOUNT_MAX_UPDATE_INTERVAL_MS;
			}
		}
		,new ObservableInfo(PayAccountsFile.getInstance())
		{
			public Integer getUpdateChanged()
			{
				return PayAccountsFile.CHANGED_AUTO_UPDATE;
			}

			public boolean isUpdateDisabled()
			{
				return !PayAccountsFile.getInstance().isBalanceAutoUpdateEnabled();
			}
			
			public boolean updateImmediately()
			{
				return true;
			}
		}
		);
	}

	/**
	 * getUpdatedClass
	 *
	 * @return Class
	 */
	public Class getUpdatedClass()
	{
		return XMLAccountInfo.class;
	}

	/**
	 * Does the update and should tell if it was successful or not.
	 *
	 */
	protected void updateInternal()
	{
		m_successfulUpdate = false;

		if (Thread.currentThread().isInterrupted())
		{
			// this thread is being stopped; ignore this error
			m_successfulUpdate = true;
			return;
		}

		Enumeration accounts = PayAccountsFile.getInstance().getAccounts();
		while (accounts.hasMoreElements() && !Thread.currentThread().isInterrupted())
		{
			PayAccount account = (PayAccount) accounts.nextElement();
			try
			{
				if (account.shouldUpdateAccountInfo())
				{
					LogHolder.log(LogLevel.DEBUG, LogType.PAY,
								  "Fetching statement for account: " + account.getAccountNumber());
					account.fetchAccountInfo(false);
					if (account.getAccountInfo() != null)
					{
						m_successfulUpdate = true;
						m_bFirstUpdateDone = true;
					}
				}
			}
			catch (Exception e)
			{
				LogHolder.log(LogLevel.ERR, LogType.PAY,
							  "Could not fetch statement for account: " + account.getAccountNumber(), e);
			}
		}

		if (Thread.currentThread().isInterrupted())
		{
			// this thread is being stopped; ignore this error
			m_successfulUpdate = true;
			return;
		}
	}

	public boolean isFirstUpdateDone()
	{
		return m_bFirstUpdateDone;
	}
	
	/**
	 * wasUpdateSuccessful
	 *
	 * @return boolean
	 */
	protected boolean wasUpdateSuccessful()
	{
		return m_successfulUpdate;
	}
}
