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
import org.bouncycastle.crypto.modes.GCMBlockCipher;
import org.bouncycastle.crypto.params.AEADParameters;
import org.bouncycastle.crypto.params.KeyParameter;

public class ControlChannelCipher
{
	GCMBlockCipher m_sentEngine;
	GCMBlockCipher m_recvEngine;

	long m_EncMsgCounter;
	long m_DecMsgCounter;

	byte[] m_sentKey;
	byte[] m_recvKey;

	public ControlChannelCipher()
	{
		m_sentEngine =  new GCMBlockCipher(new AESFastEngine());
		m_recvEngine =  new GCMBlockCipher(new AESFastEngine());
		m_EncMsgCounter=0;
		m_DecMsgCounter=0;
	}


	synchronized public int setSentKey(byte[] key, int offset, int len)
	{
		try
		{
			m_sentKey = new byte[16];
			System.arraycopy(key, offset, m_sentKey, 0, 16);
			m_EncMsgCounter=0;
			return 0;
		}
		catch (Exception e)
		{
			return -1;
		}
	}

	synchronized public int setRecvKey(byte[] key, int offset, int len)
		{
			try
			{
				m_recvKey = new byte[16];
				System.arraycopy(key, offset, m_recvKey, 0, 16);
				m_DecMsgCounter=0;
				return 0;
			}
			catch (Exception e)
			{
				return -1;
			}
		}


  private byte[] createIV(long counter) {
    byte[] iv = new byte[12];
    for (int i = 0; i < 8; i++) iv[i] = 0;
    iv[8] = (byte) (counter >> 24 & 0x00ff);
    iv[9] = (byte) (counter >> 16 & 0x00ff);
    iv[10] = (byte) (counter >> 8 & 0x00ff);
    iv[11] = (byte) (counter & 0x00ff);
    return iv;
}
	public void encryptGCM1(byte[] from, int ifrom, byte[] to, int ito, int len) throws Exception
	{
		byte[] iv=createIV(m_EncMsgCounter);
    m_EncMsgCounter++;
		m_sentEngine.init(true, new AEADParameters(new KeyParameter(m_sentKey), 128,iv, null));
		int outlen=m_sentEngine.processBytes(from, ifrom, len, to, ito);
		m_sentEngine.doFinal(to, ito+outlen);
	}

	public void decryptGCM2(byte[] from, int ifrom, byte[] to, int ito, int len) throws Exception
		{
			byte[] iv=createIV(m_DecMsgCounter);
	    m_DecMsgCounter++;
			m_recvEngine.init(false, new AEADParameters(new KeyParameter(m_recvKey), 128,iv, null));
			int declen=m_recvEngine.processBytes(from, ifrom, len, to, ito);
			m_recvEngine.doFinal(to, ito+declen);				
		}


	public int getEncryptedOutputSize(int inputlength)
		{
			return inputlength+16;
		}


	public int getDecryptedOutputSize(int enclength)
		{
			return enclength-16;
		}

}
