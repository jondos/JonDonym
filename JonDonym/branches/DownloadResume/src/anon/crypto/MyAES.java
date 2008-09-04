/*
 Copyright (c) 2000 - 2004, The JAP-Team
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

/* Hint: This file may be only a copy of the original file which is always in the JAP source tree!
 * If you change something - do not forget to add the changes also to the JAP source tree!
 */

package anon.crypto;

import org.bouncycastle.crypto.engines.AESFastEngine;
import org.bouncycastle.crypto.params.KeyParameter;

/**
 * This class is a wrapper for doing AES encryption stuff.
 */
public class MyAES
{

	/**
	 * Stores the used AES encryption algorithm.
	 */
	private AESFastEngine m_AES;

	/** Same variables for CTR mode*/
	private byte[] m_arCounter;
	private byte[] m_arCounterOut;
	private int m_posCTR;
	/**
	 * Creates a new instance of Aes.
	 */
	public MyAES()
	{
		m_AES = new AESFastEngine();
		m_arCounter = null;
		m_arCounterOut = null;
		m_posCTR = 0;
	}

	/**
	 * Initialises instance of Aes for encryption or decryption. The size of the key must be 16 bytes (128 bit),
	 * 24 bytes (192 bit) or 32 bytes (256 bit). If the key size doesn't fit, an exception is
	 * thrown.
	 *
	 * @param a_aesKey The 128 bit or 192 bit or 256 bit AES key.
	 */
	public synchronized void init(boolean bEncrypt, byte[] a_aesKey) throws Exception
	{
		init(bEncrypt,a_aesKey,0,a_aesKey.length);

	}

    /**
     * Initialises instance of Aes for encryption or decryption. The size of the key must be 16 bytes (128 bit),
     * 24 bytes (192 bit) or 32 bytes (256 bit). If the key size doesn't fit, an exception is
     * thrown.
     *
     * @param a_aesKey The 128 bit or 192 bit or 256 bit AES key.
     */
    public synchronized void init(boolean bEncrypt, byte[] a_aesKey,int off,int len) throws Exception
    {
            m_AES.init(bEncrypt, new KeyParameter(a_aesKey,off,len));
            m_arCounter = null;
            m_arCounterOut = null;
            m_posCTR = 0;

    }
	/**
	 * Encrypts/Decrypts one single plain data block and returns the cipher data block. The blocksize is
	 * always 16 bytes (128 bit). If the plain data block is shorter than 16 bytes, an exception
	 * is thrown, if it is longer, only the first 16 bytes are encrypted and returned in the cipher
	 * block.
	 *
	 * @param a_plainData The plain data block.
	 *
	 *
	 */
	public synchronized void processBlockECB(byte[] a_InData, byte[] a_OutData) throws Exception
	{
		m_AES.processBlock(a_InData, 0, a_OutData, 0);
	}

	/**
	 * Encrypts/Decrypts one single plain data block and returns the cipher data block. The blocksize is
	 * always 16 bytes (128 bit). If the plain data block is shorter than 16 bytes, an exception
	 * is thrown, if it is longer, only the first 16 bytes are encrypted and returned in the cipher
	 * block.
	 *
	 * @param a_plainData The plain data block.
	 *
	 * @return The cipher data block. The length is always 16 bytes.
	 */
	public synchronized byte[] processBlockECB(byte[] a_plainData) throws Exception
	{
		byte[] cipherBlock = new byte[16];
		m_AES.processBlock(a_plainData, 0, cipherBlock, 0);
		return cipherBlock;
	}

	public void processBytesCTR(byte[] in, int inOff, byte[] out, int outOff, int len) throws
		Exception
	{
		if (m_arCounterOut == null)
		{
			m_arCounterOut = new byte[16];
			m_posCTR = 0;
			m_arCounter = new byte[16];
		}
		while (len > 0)
		{

			if (m_posCTR == 0)
			{
				processBlockECB(m_arCounter, m_arCounterOut);
			}

			while (m_posCTR < m_arCounterOut.length)
			{
				out[outOff] = (byte) (m_arCounterOut[m_posCTR] ^ in[inOff]);
				outOff++;
				inOff++;
				len--;
				m_posCTR++;
				if (len == 0)
				{
					return;
				}
			}
			m_posCTR = 0;
			//
			// XOR the counterOut with the plaintext producing the cipher text
			//

			int carry = 1;

			for (int i = m_arCounter.length - 1; i >= 0; i--)
			{
				int x = (m_arCounter[i] & 0xff) + carry;

				if (x > 0xff)
				{
					carry = 1;
				}
				else
				{
					carry = 0;
				}

				m_arCounter[i] = (byte) x;
			}
		}
	}

}
