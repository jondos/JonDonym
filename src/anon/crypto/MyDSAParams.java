/*
 Copyright (c) 2000, The JAP-Team
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
package anon.crypto;

/* Hint: This file may be only a copy of the original file which is always in the JAP source tree!
 * If you change something - do not forget to add the changes also to the JAP source tree!
 */

import java.security.interfaces.DSAParams;
import java.security.interfaces.DSAPrivateKey;

import org.bouncycastle.asn1.x509.DSAParameter;
import org.bouncycastle.crypto.params.DSAParameters;

final public class MyDSAParams extends DSAParameters implements DSAParams
{
	public MyDSAParams()
	{
		super(null, null, null);
	}

	public MyDSAParams(DSAParams params)
	{
		super(params.getP(), params.getQ(), params.getG());
	}

	public MyDSAParams(DSAParameter params)
	{
		super(params.getP(), params.getQ(), params.getG());
	}

	public MyDSAParams(DSAParameters params)
	{
		super(params.getP(), params.getQ(), params.getG(), params.getValidationParameters());
	}

	public MyDSAParams(DSAPrivateKey key)
	{
		this(key.getParams());
	}

	public boolean equals(Object o)
	{
		if (o == null)
		{
			return false;
		}
		if (! (o instanceof DSAParams))
		{
			return false;
		}
		DSAParams p = (DSAParams) o;
		return p.getG().equals(this.getG()) && p.getP().equals(this.getP()) && p.getQ().equals(this.getQ());
	}
}
