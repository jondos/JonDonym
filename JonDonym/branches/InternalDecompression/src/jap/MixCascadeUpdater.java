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

import anon.infoservice.AbstractDatabaseEntry;
import anon.infoservice.MixCascade;
import anon.infoservice.update.AbstractMixCascadeUpdater;

/**
 * Updates the list of available MixCascades.
 * @author Rolf Wendolsky
 */
public class MixCascadeUpdater extends AbstractMixCascadeUpdater
{
	public MixCascadeUpdater(ObservableInfo a_observableInfo)
	{
		super(a_observableInfo);
	}

	public MixCascadeUpdater(long interval, boolean a_bDoMixInfoCleanup, ObservableInfo a_observableInfo)
	{
		super(interval, a_bDoMixInfoCleanup, a_observableInfo);
	}

	protected AbstractDatabaseEntry getPreferredEntry()
	{
		return JAPController.getInstance().getCurrentMixCascade();
	}

	protected void setPreferredEntry(AbstractDatabaseEntry a_preferredEntry)
	{
		if (a_preferredEntry instanceof MixCascade)
		{
			JAPController.getInstance().setCurrentMixCascade((MixCascade)a_preferredEntry);
		}
	}
}
