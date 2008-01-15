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
import anon.infoservice.JavaVersionDBEntry;
import anon.infoservice.InfoServiceHolder;

/**
 * Updates the latest java version.
 * @author Rolf Wendolsky
 */
public class JavaVersionUpdater extends AbstractDatabaseUpdater
{
	private static final long UPDATE_INTERVAL_MS = 1000 * 60 * 60 * 12l ; // half a day (update twice per day)
	private static final long UPDATE_INTERVAL_MS_SHORT = 1000 * 60 * 10l; // 10 minutes

	public JavaVersionUpdater()
	{
		super(new DynamicUpdateInterval(UPDATE_INTERVAL_MS_SHORT));
	}

	public Class getUpdatedClass()
	{
		return JavaVersionDBEntry.class;
	}

	protected Hashtable getUpdatedEntries(Hashtable a_dummy)
	{
		Hashtable hashtable = InfoServiceHolder.getInstance().getLatestJavaVersions();
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
		//return new Hashtable();
		Hashtable hashtable = InfoServiceHolder.getInstance().getLatestJavaVersionSerials();
		if (hashtable == null)
		{
			((DynamicUpdateInterval)getUpdateInterval()).setUpdateInterval(UPDATE_INTERVAL_MS_SHORT);
			return new Hashtable();
		}
		((DynamicUpdateInterval)getUpdateInterval()).setUpdateInterval(UPDATE_INTERVAL_MS);
		return hashtable;
	}
}
