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

import org.bouncycastle.crypto.digests.MD5Digest;
import org.bouncycastle.crypto.digests.SHA1Digest;
import anon.util.ByteArrayUtil;

/**
 * @author stefan
 *a Pseudo Radnom Function as described in RFC2246
 */
public class PRF
{

	private byte[] m_secret;
	private byte[] m_seed;
	private byte[] m_label;
	/**
	 * Constructor for a Pseudo Random Function
	 * @param secret a secret
	 * @param label a label
	 * @param seed a seed
	 */
	public PRF(byte[] secret, byte[] label, byte[] seed)
	{
		this.m_secret = secret;
		this.m_seed = seed;
		this.m_label = label;
	}

	/**
	 * calculates the result of a pseudo random function
	 * @param length length of the result
	 * @return result of a PRF with variable length
	 */
	public byte[] calculate(int length)
	{
		byte[] a;
		byte[] b;
		byte[] c = new byte[length];
		int splitsize = this.m_secret.length / 2;
		if ( (splitsize * 2) < this.m_secret.length)
		{
			splitsize++;
		}
		byte[] s1 = ByteArrayUtil.copy(this.m_secret, 0, splitsize);
		byte[] s2 = ByteArrayUtil.copy(this.m_secret, this.m_secret.length - splitsize, splitsize);
		P_Hash phash = new P_Hash(s1, ByteArrayUtil.conc(this.m_label, this.m_seed), new MD5Digest());
		a = phash.getHash(length);
		phash = new P_Hash(s2, ByteArrayUtil.conc(this.m_label, this.m_seed), new SHA1Digest());
		b = phash.getHash(length);
		for (int i = 0; i < length; i++)
		{
			c[i] = (byte) ( (a[i] ^ b[i]) & 0xFF);
		}

		return c;
	}

}
