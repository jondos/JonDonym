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
package anon.infoservice;

import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;
import anon.crypto.JAPCertificate;
import anon.crypto.MultiCertPath;
import anon.crypto.X509SubjectKeyIdentifier;
import anon.crypto.XMLSignature;

/**
 *
 *
 * @author Rolf Wendolsky
 */
public abstract class AbstractDistributableCertifiedDatabaseEntry extends AbstractDistributableDatabaseEntry
	implements ICertifiedDatabaseEntry
{
	public AbstractDistributableCertifiedDatabaseEntry(long a_expireTime)
	{
		super(a_expireTime);
	}
	
	public abstract XMLSignature getSignature();

	/**
	 * Returns if this entry has been verified with a certificate chain.
	 * @return if this entry has been verified with a certificate chain
	 */
	public abstract boolean isVerified();

	/**
	 * Checks if the ID is valid.
	 * @return boolean
	 */
	public boolean checkId()
	{
		XMLSignature signature = getSignature();
		if(signature == null)
		{
			LogHolder.log(LogLevel.INFO,LogType.CRYPTO,"AbstractDistributableCertifiedDatabaseEntry.checkId() -- Signature is NULL!");
			return false;
		}
		return (getId() != null) && 
				getId().equalsIgnoreCase(signature.getXORofSKIs());
	}

}
