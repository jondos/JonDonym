/*
 * Copyright (c) 2006, The JAP-Team
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *   - Redistributions of source code must retain the above copyright notice,
 *     this list of conditions and the following disclaimer.
 *
 *   - Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer in the
 *     documentation and/or other materials provided with the distribution.
 *
 *   - Neither the name of the University of Technology Dresden, Germany nor
 *     the names of its contributors may be used to endorse or promote
 *     products derived from this software without specific prior written
 *     permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package anon.infoservice;

import anon.AnonServerDescription;
import anon.IServiceContainer;
import java.security.SignatureException;
import anon.client.ITrustModel;
import anon.client.BasicTrustModel;

/**
 * This class keeps and returns one or more objects of the class MixCascade.
 * @author Rolf Wendolsky
 */
public abstract class AbstractMixCascadeContainer extends BasicTrustModel implements IServiceContainer
{
	/**
	 * Must return a MixCascade and never null. The returned MixCascade may change
	 * with every call of this method.
	 * @return a MixCascade and never null
	 */
	public abstract MixCascade getNextMixCascade();

	/**
	 * Returns the last return value of getNextCascade().
	 * @return the last return value of getNextCascade() (may be null if no call has been
	 * done yet)
	 */
	public abstract MixCascade getCurrentMixCascade();

	public final AnonServerDescription getCurrentService()
	{
		return getCurrentMixCascade();
	}

	/**
	 * Optional method that should allow to return the last MixCascade (Service) that was returned
	 * by getNextService() also the next time this method is called.
	 * @param a_bKeepCurrentService allows or dissallows to keep the current cascade for the next call
	 */
	public abstract void keepCurrentService(boolean a_bKeepCurrentService);

	public abstract boolean isServiceAutoSwitched();

	public abstract boolean isReconnectedAutomatically();
}
