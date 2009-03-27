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
/*
 * Created on Mar 25, 2004
 *
 */
package anon.crypto.tinytls.util;

import org.bouncycastle.crypto.Digest;
import org.bouncycastle.crypto.macs.HMac;
import org.bouncycastle.crypto.params.KeyParameter;
import anon.util.ByteArrayUtil;

/**
 * @author stefan
 *
 * a P_Hash function as described in RFC2246
 */
public class P_Hash
{

	private byte[] m_secret;
	private byte[] m_seed;
	private Digest m_digest;

	/**
	 * Constructor
	 * @param secret a secret
	 * @param seed a seed
	 * @param digest a digest
	 */
	public P_Hash(byte[] secret, byte[] seed, Digest digest)
	{
		this.m_secret = secret;
		this.m_seed = seed;
		this.m_digest = digest;
	}

	/**
	 * returns a hash with a variabel length
	 * @param length length of the hash
	 * @return hash
	 */
	public byte[] getHash(int length)
	{
		byte[] a;
		byte[] b = null;
		byte[] c;
		int counter = 0;
		HMac hm = new HMac(this.m_digest);
		hm.reset();
		hm.init(new KeyParameter(this.m_secret));
		hm.update(this.m_seed, 0, this.m_seed.length);
		a = new byte[hm.getMacSize()];
		hm.doFinal(a, 0);

		do
		{
			//HMAC_HASH(secret,a+seed)
			hm.reset();
			hm.init(new KeyParameter(this.m_secret));
			hm.update(ByteArrayUtil.conc(a, this.m_seed), 0, a.length + this.m_seed.length);
			c = new byte[hm.getMacSize()];
			hm.doFinal(c, 0);
			if (b == null)
			{
				b = c;
			}
			else
			{
				b = ByteArrayUtil.conc(b, c);
			}

			//compute next a
			hm.reset();
			hm.init(new KeyParameter(this.m_secret));
			hm.update(a, 0, a.length);
			a = new byte[hm.getMacSize()];
			hm.doFinal(a, 0);
		}
		while (b.length < length);

		return ByteArrayUtil.copy(b, 0, length);
	}

}
