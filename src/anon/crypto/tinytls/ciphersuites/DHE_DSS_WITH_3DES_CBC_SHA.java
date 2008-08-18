/*
 Copyright (c) 2004, The JAP-Team
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
package anon.crypto.tinytls.ciphersuites;

import org.bouncycastle.crypto.engines.DESedeEngine;
import org.bouncycastle.crypto.modes.CBCBlockCipher;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.crypto.params.ParametersWithIV;
import anon.crypto.tinytls.TLSException;
import anon.crypto.tinytls.keyexchange.DHE_DSS_Key_Exchange;
import anon.util.ByteArrayUtil;

public class DHE_DSS_WITH_3DES_CBC_SHA extends CipherSuite
{

	public DHE_DSS_WITH_3DES_CBC_SHA() throws TLSException
	{
		super(new byte[]
			  {0x00, 0x13});
		this.m_ciphersuitename = "TLS_DHE_DSS_WITH_3DES_CBC_SHA";
		this.setKeyExchangeAlgorithm(new DHE_DSS_Key_Exchange());
	}

	protected void calculateKeys(byte[] keys, boolean forclient)
	{
		this.m_clientwritekey = ByteArrayUtil.copy(keys, 40, 24);
		this.m_serverwritekey = ByteArrayUtil.copy(keys, 64, 24);
		this.m_clientwriteIV = ByteArrayUtil.copy(keys, 88, 8);
		this.m_serverwriteIV = ByteArrayUtil.copy(keys, 96, 8);
		if (forclient)
		{
			this.m_clientmacsecret = ByteArrayUtil.copy(keys, 0, 20);
			this.m_servermacsecret = ByteArrayUtil.copy(keys, 20, 20);
			this.m_encryptcipher = new CBCBlockCipher(new DESedeEngine());
			this.m_encryptcipher.init(true,
									  new ParametersWithIV(new KeyParameter(this.m_clientwritekey), this.m_clientwriteIV));
			this.m_decryptcipher = new CBCBlockCipher(new DESedeEngine());
			this.m_decryptcipher.init(false,
									  new ParametersWithIV(new KeyParameter(this.m_serverwritekey), this.m_serverwriteIV));
		}
		else
		{
			this.m_servermacsecret = ByteArrayUtil.copy(keys, 0, 20);
			this.m_clientmacsecret = ByteArrayUtil.copy(keys, 20, 20);
			this.m_encryptcipher = new CBCBlockCipher(new DESedeEngine());
			this.m_encryptcipher.init(true,
									  new ParametersWithIV(new KeyParameter(this.m_serverwritekey), this.m_serverwriteIV));
			this.m_decryptcipher = new CBCBlockCipher(new DESedeEngine());
			this.m_decryptcipher.init(false,
									  new ParametersWithIV(new KeyParameter(this.m_clientwritekey), this.m_clientwriteIV));
		}
	}

}
