/*
 Copyright (c) 2000 - 2006, The JAP-Team
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
package anon.client;

import java.security.SignatureException;
import anon.client.TrustException;
import anon.crypto.SignatureVerifier;

import java.util.Date;
import java.util.Observable;

import anon.infoservice.MixCascade;
import anon.infoservice.MixInfo;
import anon.util.JAPMessages;


/**
 * A trust model that only checks if a previously done signature verification was successful.
 *
 * @author Rolf Wendolsky
 */
public class BasicTrustModel extends Observable implements ITrustModel
{
	public BasicTrustModel()
	{
	}

	public void checkTrust(MixCascade a_cascade) throws TrustException, SignatureException
	{
		// test if all mixes have verified certificates.
		MixInfo info;
		int countUnverified = 0;
		SignatureException exception = null;
		for (int i = 0; i < a_cascade.getNumberOfMixes(); i++)
		{
			info = a_cascade.getMixInfo(i);
			if ((info == null && SignatureVerifier.getInstance().isCheckSignatures()) || !info.isVerified())
			{
				countUnverified++;
				if (exception == null)
				{
					exception = new SignatureException(JAPMessages.getString("invalidSignature") + " (Mix " + (i+1) + ")");
				}
			}
		}
		if (exception != null)
		{
			if (countUnverified > 1 || a_cascade.getNumberOfOperatorsShown() == 1 || a_cascade.getNumberOfMixes() <= 1)
			{
				throw new SignatureException(JAPMessages.getString("invalidSignature"));
			}
			else
			{
				throw exception;
			}
		}
		
		if (a_cascade == null || (!a_cascade.isUserDefined() && !a_cascade.isVerified()))
		{
			throw (new SignatureException(JAPMessages.getString("invalidSignature")));
		}
		else if (SignatureVerifier.getInstance().isCheckSignatures())
		{
			
			// check whether at least one of the certificates and at least the certificate of the first or last Mix are valid
			for (int i = 0; i < a_cascade.getNumberOfMixes(); i++)
			{
				if (a_cascade.getMixInfo(i) != null && a_cascade.getMixInfo(i).getCertPath() != null &&
					a_cascade.getMixInfo(i).getCertPath().isValid(new Date()) && (i == 0 || i == a_cascade.getNumberOfMixes() - 1))
				{
					return;
				}
			}
			
			throw (new SignatureException(JAPMessages.getString("invalidSignature")));
		}
	}

	/**
	 * Does a call on checkTrust() after checking the isShownAsTrusted() attribute of the given cascade.
	 * Should be called by GUI methods only, not for checking the trust to make a connection!
	 * @param a_cascade MixCascade
	 * @return boolean
	 */
	public final boolean isTrusted(MixCascade a_cascade)
	{
		if (a_cascade == null)
		{
			return false;
		}
		if (a_cascade != null && a_cascade.isShownAsTrusted())
		{
			return true;
		}
		try
		{
			checkTrust(a_cascade);
			return true;
		}
		catch (TrustException a_e)
		{
			return false;
		}
		catch (SignatureException a_e)
		{
			return false;
		}
	}

	public final boolean isTrusted(MixCascade a_cascade, StringBuffer buff)
	{
		if (a_cascade != null && a_cascade.isShownAsTrusted())
		{
			return true;
		}
		try
		{
			checkTrust(a_cascade);
			return true;
		}
		catch (TrustException a_e)
		{
			buff.append(a_e.getMessage());
			return false;
		}
		catch (SignatureException a_e)
		{
			buff.append(a_e.getMessage());
			return false;
		}
	}
}
