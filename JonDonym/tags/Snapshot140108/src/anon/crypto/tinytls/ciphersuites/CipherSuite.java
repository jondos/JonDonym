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
 * Created on Mar 16, 2004
 *
 */
package anon.crypto.tinytls.ciphersuites;

import java.math.BigInteger;

import anon.crypto.JAPCertificate;
import anon.crypto.tinytls.TLSException;
import anon.crypto.tinytls.TLSPlaintextRecord;
import anon.crypto.tinytls.keyexchange.Key_Exchange;
import anon.util.ByteArrayUtil;
import org.bouncycastle.crypto.macs.HMac;
import org.bouncycastle.crypto.digests.SHA1Digest;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.crypto.modes.CBCBlockCipher;
import anon.crypto.MyRandom;

/**
 * @author stefan
 *
 * A abstract ciphersuite that can used TinyTLS
 * this is the parent class for all ciphersuites
 */
public abstract class CipherSuite
{

	private byte[] m_ciphersuitecode;
	protected String m_ciphersuitename = "Name not set";
	private Key_Exchange m_keyexchangealgorithm = null;
	private JAPCertificate m_servercertificate = null;
	protected CBCBlockCipher m_decryptcipher;
	protected CBCBlockCipher m_encryptcipher;
	private HMac m_hmacInput = new HMac(new SHA1Digest());
	private HMac m_hmacOutput = new HMac(new SHA1Digest());
	private MyRandom m_Random;
	/**
	 * writesequenznumber for packages
	 */
	protected long m_writesequenznumber;
	/**
	 * readsequenznumber for packages
	 */
	protected long m_readsequenznumber;
	/**
	 * client write key
	 */
	protected byte[] m_clientwritekey = null;
	/**
	 * client write mac secret
	 */
	protected byte[] m_clientmacsecret = null;
	/**
	 * client write IV, only used for block ciphers
	 */
	protected byte[] m_clientwriteIV = null;
	/**
	 * server write key
	 */
	protected byte[] m_serverwritekey = null;
	/**
	 * server write mac secret
	 */
	protected byte[] m_servermacsecret = null;
	/**
	 * server write IV, only used for block ciphers
	 */
	protected byte[] m_serverwriteIV = null;

	/**
	 * Constructor for a ciphersuite
	 * @param code Code of the ciphersuite (see RFC2246)
	 * @throws TLSException
	 */
	public CipherSuite(byte[] code) throws TLSException
	{
		if (code.length != 2)
		{
			throw new TLSException("wrong CipherSuiteCode ");
		}
		m_ciphersuitecode = code;
		m_writesequenznumber = 0;
		m_readsequenznumber = 0;
		m_Random = new MyRandom();
		}

	/**
	 * sets the key exchange algorithm
	 * @param ke Key Exchange Algorithm
	 */
	protected void setKeyExchangeAlgorithm(Key_Exchange ke)
	{
		m_keyexchangealgorithm = ke;
	}

	/**
	 * gets the key exchange algorithm that is used
	 * @return
	 * key exchange algorithm
	 */
	public Key_Exchange getKeyExchangeAlgorithm()
	{
		return m_keyexchangealgorithm;
	}

	/**
	 * set the Server Certificate
	 * @param cert server certificate
	 */
	public void setServerCertificate(JAPCertificate cert)
	{
		this.m_servercertificate = cert;
	}

	/**
	 * returns the code of a ciphersuite (see RFC2246)
	 * @return ciphersuitecode
	 */
	public byte[] getCipherSuiteCode()
	{
		return this.m_ciphersuitecode;
	}

	/**
	 * processes the client key exchange
	 * @param dh_y
	 * diffie hellman parameter
	 */
	public void processClientKeyExchange(BigInteger dh_y)
	{
		this.m_keyexchangealgorithm.processClientKeyExchange(dh_y);
		calculateKeys(this.m_keyexchangealgorithm.calculateKeys(), false);
		m_hmacInput.init(new KeyParameter(m_servermacsecret));
		m_hmacOutput.init(new KeyParameter(m_clientmacsecret));
	}

	/**
	 * calculate the client keys (see RFC2246 Client Key Exchange)
	 * @return client key exchange message
	 * @throws TLSException
	 */
	public byte[] calculateClientKeyExchange() throws TLSException
	{
		byte[] b = this.m_keyexchangealgorithm.calculateClientKeyExchange();
		calculateKeys(this.m_keyexchangealgorithm.calculateKeys(), true);
		m_hmacInput.init(new KeyParameter(m_servermacsecret));
		m_hmacOutput.init(new KeyParameter(m_clientmacsecret));
		return b;
	}

	/**
	 * validates the finishedmessage and throws a Exception if a error occure
	 * @param finishedmessage the message that have to be valideted
	 * @throws TLSException
	 */
	public void processServerFinished(TLSPlaintextRecord msg, byte[] handshakemessages) throws TLSException
	{
		decode(msg);
		m_keyexchangealgorithm.processServerFinished(msg.getData(), msg.getLength(), handshakemessages);
	}

	/**
	 * encodes a message with a symmetric key
	 * @param message message
	 * @return encoded message
	 */
	public void encode(TLSPlaintextRecord msg)
	{
		int dataLen=msg.getLength();
		byte[] dataBuff=msg.getData();
		byte[] header=msg.getHeader();
		m_hmacOutput.reset();
		m_hmacOutput.update(ByteArrayUtil.inttobyte(m_writesequenznumber, 8), 0, 8);
		m_writesequenznumber++;
		m_hmacOutput.update(header, 0,header.length);
		m_hmacOutput.update(dataBuff, 0, dataLen);
		m_hmacOutput.doFinal(dataBuff, dataLen);
		dataLen +=m_hmacOutput.getMacSize();
		//add padding as described in RFC2246 (6.2.3.2)
		int paddingsize = m_Random.nextInt(240);
		paddingsize = paddingsize +
			(m_encryptcipher.getBlockSize() -
			 ( (dataLen + 1 + paddingsize) % m_encryptcipher.getBlockSize()));
		for (int i = 0; i < paddingsize + 1; i++)
		{
			dataBuff[dataLen++] = (byte) paddingsize;
		}

		for (int i = 0; i < dataLen; i += m_encryptcipher.getBlockSize())
		{
			m_encryptcipher.processBlock(dataBuff, i, dataBuff, i);
		}
		msg.setLength(dataLen);
	}

	/**
	 * decodes a message with a symmetric key
	 * @param message message
	 * @return decoded message
	 */
	public void decode(TLSPlaintextRecord msg) throws TLSException
	{
		int dataLen=msg.getLength();
		byte[] dataBuff=msg.getData();
		if ( (dataLen % m_decryptcipher.getBlockSize()) != 0 ||
			dataLen < m_hmacInput.getMacSize())
		{
			throw new TLSException("wrong payload len!");
		}
		for (int i = 0; i < dataLen; i += m_decryptcipher.getBlockSize())
		{
			m_decryptcipher.processBlock(dataBuff, i,dataBuff, i);
		}
		//remove padding and mac
		int len = dataLen - m_hmacInput.getMacSize() - 1;
		byte paddingbyte=dataBuff[dataLen - 1];
		int paddinglength = paddingbyte&0x00FF; //padding
		if(paddinglength>dataLen-2)
		{
			throw new TLSException("wrong Padding len detected", 2, 51);

		}

		//check if we've recieved the right padding
		for (int i = dataLen - 1; i > dataLen - paddinglength - 2; i--)
		{
			if (dataBuff[i] != paddingbyte)
			{
				throw new TLSException("wrong Padding detected", 2, 51);
			}
		}

		len -= paddinglength;
		msg.setLength(len);

		m_hmacInput.reset();
		m_hmacInput.update(ByteArrayUtil.inttobyte(m_readsequenznumber, 8), 0, 8);
		m_readsequenznumber++;
		byte[] header=msg.getHeader();
		m_hmacInput.update(header, 0, header.length);
		m_hmacInput.update(dataBuff, 0, len);
		byte[] mac = new byte[m_hmacInput.getMacSize()];
		m_hmacInput.doFinal(mac, 0);

		if(!ByteArrayUtil.equal(dataBuff,len,mac,0,mac.length))
				throw new TLSException("Wrong MAC detected!!!", 2, 20);
	}

	/**
	 * calculate server and client write keys (see RFC2246 TLS Record Protocoll)
	 * @param keys array of bytes(see RFC how it is calculated)
	 */
	protected abstract void calculateKeys(byte[] keys, boolean forclient);

	public String toString()
	{
		return m_ciphersuitename;
	}

}
