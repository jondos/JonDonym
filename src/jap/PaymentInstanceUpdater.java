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

import java.util.Hashtable;
import java.util.Vector;

import anon.infoservice.AbstractDatabaseEntry;
import anon.infoservice.Database;
import anon.infoservice.InfoServiceHolder;
import anon.pay.PaymentInstanceDBEntry;
import anon.pay.PayAccountsFile;
import logging.*;

/**
 * Updates the list of available MixCascades.
 * @author Rolf Wendolsky
 */
public class PaymentInstanceUpdater extends AbstractDatabaseUpdater
{
	private static final long UPDATE_INTERVAL_MS = 15 * 60000l; //every 15 minutes
	private static final long MIN_UPDATE_INTERVAL_MS = 60000l;

	public PaymentInstanceUpdater()
	{
		super(new DynamicUpdateInterval(UPDATE_INTERVAL_MS));
	}

	public Class getUpdatedClass()
	{
		return PaymentInstanceDBEntry.class;
	}

	protected Hashtable getEntrySerials()
	{
		return new Hashtable();
	}

	protected Hashtable getUpdatedEntries(Hashtable a_entriesToUpdate)
	{
		Hashtable pis = InfoServiceHolder.getInstance().getPaymentInstances();

		if (pis == null)
		{
			// no entries where found
			((DynamicUpdateInterval)getUpdateInterval()).setUpdateInterval(MIN_UPDATE_INTERVAL_MS);
		}
		else
		{
			((DynamicUpdateInterval)getUpdateInterval()).setUpdateInterval(UPDATE_INTERVAL_MS);
		}

		if (pis != null && pis.size() == 0)
		{
			// no payment instances found in InfoService; do not delete remaining pis!
			pis = null;
		}

		return pis;
	}
	
	protected boolean protectFromCleanup(AbstractDatabaseEntry a_currentEntry)
	{
		// do not delete any payment instances for that we still have an account
		if (PayAccountsFile.getInstance().getAccounts(a_currentEntry.getId()).size() > 0)
		{
			return true;
		}
		
		return false;
	}
}
