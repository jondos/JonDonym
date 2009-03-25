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
	AESFastEngine m_aesEngine1;
	AESFastEngine m_aesEngine2;

	byte[] m_iv1 = null;

	byte[] m_iv2 = null;

	byte[] m_aesKeys;

	public SymCipher()
	{
		m_aesEngine1 = new AESFastEngine();
		m_aesEngine2 = new AESFastEngine();
		m_aesKeys = null;
		m_iv1 = new byte[16];
		for (int i = 0; i < 16; i++)
		{
			m_iv1[i] = 0;
		}
		m_iv2 = new byte[16];
		for (int i = 0; i < 16; i++)
		{
			m_iv2[i] = 0;
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
			m_aesKeys = new byte[16];
			System.arraycopy(key, offset, m_aesKeys, 0, 16);
			m_aesEngine1.init(true, new KeyParameter(m_aesKeys));
			m_aesEngine2.init(true, new KeyParameter(m_aesKeys));
			if (len == 16)
			{
				for (int i = 0; i < 16; i++)
				{
					m_iv1[i] = 0;
					m_iv2[i] = 0;
				}
			}
			else
			{
				for (int i = 0; i < 16; i++)
				{
					m_iv1[i] = key[i + 16 + offset];
					m_iv2[i] = key[i + 16 + offset];
				}
			}

			return 0;
		}
		catch (Exception e)
		{
			m_aesKeys = null;

			return -1;
		}
	}

	/** Set both keys to different value, if keys.length==2* KEY_SIZe, else set both keys to the same value
	 * 
	 * @param keys
	 *            byte[]
	 * @return int
	 */
	synchronized public int setEncryptionKeysAES(byte[] keys)
	{
		try
		{
			if(keys.length==16)
				{
				return setEncryptionKeyAES(keys);
				}
			else
				{
			m_aesKeys = new byte[32];
			System.arraycopy(keys, 0, m_aesKeys, 0, 32);
			m_aesEngine1.init(true, new KeyParameter(m_aesKeys,0,16));
			m_aesEngine2.init(true, new KeyParameter(m_aesKeys,16,16));
			for (int i = 0; i < 16; i++)
			{
				m_iv1[i] = 0;
				m_iv2[i] = 0;
			}

			return 0;
				}
		}
		catch (Exception e)
		{
			m_aesKeys = null;

			return -1;
		}
	}

	/**
	 * Returns the currently used key(s) for encryption. If both keys are set, than an array of 32 byte is returned, otherwise an array of 16 bytes
	 * 
	 * @return The current key(s) used for encryption or null, if no key is set.
	 */
	public byte[] getKeys()
	{
		return m_aesKeys;
	}

	synchronized public void setIV2(byte[] buff)
	{
		for (int i = 0; i < 16; i++)
		{
			m_iv2[i] = buff[i];
		}
	}

	public int encryptAES1(byte[] from, int ifrom, byte[] to, int ito, int len)
	{
		len = ifrom + len;
		while (ifrom < len - 15)
		{
			synchronized (m_aesEngine1)
			{
				m_aesEngine1.processBlock(m_iv1, 0, m_iv1, 0);
			}
			to[ito++] = (byte) (from[ifrom++] ^ m_iv1[0]);
			to[ito++] = (byte) (from[ifrom++] ^ m_iv1[1]);
			to[ito++] = (byte) (from[ifrom++] ^ m_iv1[2]);
			to[ito++] = (byte) (from[ifrom++] ^ m_iv1[3]);
			to[ito++] = (byte) (from[ifrom++] ^ m_iv1[4]);
			to[ito++] = (byte) (from[ifrom++] ^ m_iv1[5]);
			to[ito++] = (byte) (from[ifrom++] ^ m_iv1[6]);
			to[ito++] = (byte) (from[ifrom++] ^ m_iv1[7]);
			to[ito++] = (byte) (from[ifrom++] ^ m_iv1[8]);
			to[ito++] = (byte) (from[ifrom++] ^ m_iv1[9]);
			to[ito++] = (byte) (from[ifrom++] ^ m_iv1[10]);
			to[ito++] = (byte) (from[ifrom++] ^ m_iv1[11]);
			to[ito++] = (byte) (from[ifrom++] ^ m_iv1[12]);
			to[ito++] = (byte) (from[ifrom++] ^ m_iv1[13]);
			to[ito++] = (byte) (from[ifrom++] ^ m_iv1[14]);
			to[ito++] = (byte) (from[ifrom++] ^ m_iv1[15]);
		}
		if (ifrom < len)
		{
			synchronized (m_aesEngine1)
			{
				m_aesEngine1.processBlock(m_iv1, 0, m_iv1, 0);
			}
			len -= ifrom;
			for (int k = 0; k < len; k++)
			{
				to[ito++] = (byte) (from[ifrom++] ^ m_iv1[k]);
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
			synchronized (m_aesEngine2)
			{
				m_aesEngine2.processBlock(m_iv2, 0, m_iv2, 0);
			}
			buff[i++] ^= m_iv2[0];
			buff[i++] ^= m_iv2[1];
			buff[i++] ^= m_iv2[2];
			buff[i++] ^= m_iv2[3];
			buff[i++] ^= m_iv2[4];
			buff[i++] ^= m_iv2[5];
			buff[i++] ^= m_iv2[6];
			buff[i++] ^= m_iv2[7];
			buff[i++] ^= m_iv2[8];
			buff[i++] ^= m_iv2[9];
			buff[i++] ^= m_iv2[10];
			buff[i++] ^= m_iv2[11];
			buff[i++] ^= m_iv2[12];
			buff[i++] ^= m_iv2[13];
			buff[i++] ^= m_iv2[14];
			buff[i++] ^= m_iv2[15];
		}
		if (i < len)
		{
			synchronized (m_aesEngine2)
			{
				m_aesEngine2.processBlock(m_iv2, 0, m_iv2, 0);
			}
			len -= i;
			for (int k = 0; k < len; k++)
			{
				buff[i++] ^= m_iv2[k];
			}
		}
		return 0;
	}

}
