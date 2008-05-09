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
package anon.client.crypto;

import org.bouncycastle.crypto.engines.AESFastEngine;
import org.bouncycastle.crypto.params.KeyParameter;

public class SymCipher
{
	AESFastEngine aesEngine;

	byte[] iv = null;

	byte[] iv2 = null;

	byte[] m_aesKey;

	public SymCipher()
	{
		aesEngine = new AESFastEngine();
		m_aesKey = null;
		iv = new byte[16];
		for (int i = 0; i < 16; i++)
		{
			iv[i] = 0;
		}
		iv2 = new byte[16];
		for (int i = 0; i < 16; i++)
		{
			iv2[i] = 0;
		}
	}

	synchronized public int setEncryptionKeyAES(byte[] key)
	{
		return setEncryptionKeyAES(key, 0, 16);
	}

	/**
	 * 
	 * @param key
	 *            byte[]
	 * @param offset
	 *            int
	 * @param len
	 *            int if len==16 --> only the key is set; if len==32 --> key and IV is set
	 * @return int
	 */
	synchronized public int setEncryptionKeyAES(byte[] key, int offset, int len)
	{
		try
		{
			m_aesKey = new byte[16];
			System.arraycopy(key, offset, m_aesKey, 0, 16);
			aesEngine.init(true, new KeyParameter(m_aesKey));
			if (len == 16)
			{
				for (int i = 0; i < 16; i++)
				{
					iv[i] = 0;
					iv2[i] = 0;
				}
			}
			else
			{
				for (int i = 0; i < 16; i++)
				{
					iv[i] = key[i + 16 + offset];
					iv2[i] = key[i + 16 + offset];
				}
			}

			return 0;
		}
		catch (Exception e)
		{
			m_aesKey = null;

			return -1;
		}
	}

	/**
	 * Returns the currently used key for encryption.
	 * 
	 * @return The current key used for encryption or null, if no key is set.
	 */
	public byte[] getKey()
	{
		return m_aesKey;
	}

	synchronized public void setIV2(byte[] buff)
	{
		for (int i = 0; i < 16; i++)
		{
			iv2[i] = buff[i];
		}
	}

	public int encryptAES(byte[] from, int ifrom, byte[] to, int ito, int len)
	{
		len = ifrom + len;
		while (ifrom < len - 15)
		{
			synchronized (aesEngine)
			{
				aesEngine.processBlock(iv, 0, iv, 0);
			}
			to[ito++] = (byte) (from[ifrom++] ^ iv[0]);
			to[ito++] = (byte) (from[ifrom++] ^ iv[1]);
			to[ito++] = (byte) (from[ifrom++] ^ iv[2]);
			to[ito++] = (byte) (from[ifrom++] ^ iv[3]);
			to[ito++] = (byte) (from[ifrom++] ^ iv[4]);
			to[ito++] = (byte) (from[ifrom++] ^ iv[5]);
			to[ito++] = (byte) (from[ifrom++] ^ iv[6]);
			to[ito++] = (byte) (from[ifrom++] ^ iv[7]);
			to[ito++] = (byte) (from[ifrom++] ^ iv[8]);
			to[ito++] = (byte) (from[ifrom++] ^ iv[9]);
			to[ito++] = (byte) (from[ifrom++] ^ iv[10]);
			to[ito++] = (byte) (from[ifrom++] ^ iv[11]);
			to[ito++] = (byte) (from[ifrom++] ^ iv[12]);
			to[ito++] = (byte) (from[ifrom++] ^ iv[13]);
			to[ito++] = (byte) (from[ifrom++] ^ iv[14]);
			to[ito++] = (byte) (from[ifrom++] ^ iv[15]);
		}
		if (ifrom < len)
		{
			synchronized (aesEngine)
			{
				aesEngine.processBlock(iv, 0, iv, 0);
			}
			len -= ifrom;
			for (int k = 0; k < len; k++)
			{
				to[ito++] = (byte) (from[ifrom++] ^ iv[k]);
			}
		}
		return 0;
	}

	public int encryptAES2(byte[] buff)
	{
		int i = 0;
		int len = buff.length;
		while (i < len - 15)
		{
			synchronized (aesEngine)
			{
				aesEngine.processBlock(iv2, 0, iv2, 0);
			}
			buff[i++] ^= iv2[0];
			buff[i++] ^= iv2[1];
			buff[i++] ^= iv2[2];
			buff[i++] ^= iv2[3];
			buff[i++] ^= iv2[4];
			buff[i++] ^= iv2[5];
			buff[i++] ^= iv2[6];
			buff[i++] ^= iv2[7];
			buff[i++] ^= iv2[8];
			buff[i++] ^= iv2[9];
			buff[i++] ^= iv2[10];
			buff[i++] ^= iv2[11];
			buff[i++] ^= iv2[12];
			buff[i++] ^= iv2[13];
			buff[i++] ^= iv2[14];
			buff[i++] ^= iv2[15];
		}
		if (i < len)
		{
			synchronized (aesEngine)
			{
				aesEngine.processBlock(iv2, 0, iv2, 0);
			}
			len -= i;
			for (int k = 0; k < len; k++)
			{
				buff[i++] ^= iv2[k];
			}
		}
		return 0;
	}

}
