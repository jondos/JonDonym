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
package anon.tor;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.net.InetAddress;
import java.security.SecureRandom;

import org.bouncycastle.asn1.DEROutputStream;
import org.bouncycastle.crypto.CryptoException;
import org.bouncycastle.crypto.InvalidCipherTextException;
import org.bouncycastle.crypto.agreement.DHBasicAgreement;
import org.bouncycastle.crypto.digests.SHA1Digest;
import org.bouncycastle.crypto.generators.DHKeyPairGenerator;
import org.bouncycastle.crypto.params.DHKeyGenerationParameters;
import org.bouncycastle.crypto.params.DHParameters;
import org.bouncycastle.crypto.params.DHPrivateKeyParameters;
import org.bouncycastle.crypto.params.DHPublicKeyParameters;

import anon.crypto.MyRSAPublicKey;
import anon.crypto.MyAES;
import anon.crypto.MyRSA;
import anon.tor.cells.Cell;
import anon.tor.cells.CreateCell;
import anon.tor.cells.RelayCell;

import anon.tor.ordescription.ORDescriptor;
import anon.crypto.tinytls.util.hash;
import anon.util.ByteArrayUtil;
import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;
import org.bouncycastle.crypto.AsymmetricCipherKeyPair;

/**
 * @author stefan
 *
 */
public class OnionRouter
{

	private final static BigInteger SAFEPRIME = new BigInteger(
		"00FFFFFFFFFFFFFFFFC90FDAA22168C234C4C6628B80DC1CD129024E08" +
		"8A67CC74020BBEA63B139B22514A08798E3404DDEF9519B3CD3A431B" +
		"302B0A6DF25F14374FE1356D6D51C245E485B576625E7EC6F44C42E9" +
		"A637ED6B0BFF5CB6F406B7EDEE386BFB5A899FA5AE9F24117C4B1FE6" +
		"49286651ECE65381FFFFFFFFFFFFFFFF", 16);

	//lower boundary for key : 2^24
	private final static BigInteger MINKEY = new BigInteger(new byte[]
		{1, 0, 0, 0});

	//upper boundary for key : p - 2^24
	private final static BigInteger MAXKEY = SAFEPRIME.subtract(MINKEY);

	private final static DHParameters DH_PARAMS = new DHParameters(SAFEPRIME, new BigInteger("2"));

	private ORDescriptor m_description;
	private DHBasicAgreement m_dhe;
	private MyAES m_encryptionEngine;
	private MyAES m_decryptionEngine;
	private OnionRouter m_nextOR;
	private int m_circID;
	private SHA1Digest m_digestDf;
	private SHA1Digest m_digestDb;
	private boolean m_extended;

	/**
	 * Constructor
	 * @param circID
	 * circID of the circuit where it is used
	 * @param description
	 * ORDescription of the onionrouter
	 * @throws IOException
	 */
	public OnionRouter(int circID, ORDescriptor description) throws IOException
	{
		this.m_description = description;
		this.m_circID = circID;
		this.m_nextOR = null;
		this.m_extended = false;
	}

	/**
	 * returns a description of this router
	 * @return
	 * ORDescription
	 */
	public ORDescriptor getDescription()
	{
		return this.m_description;
	}

	/**
	 * encrypts a RelayCell
	 * @param cell
	 * unencrypted cell
	 * @return
	 * encrypted cell
	 */
	public synchronized RelayCell encryptCell(RelayCell cell) throws Exception
	{
		if (m_nextOR != null)
		{
			cell = m_nextOR.encryptCell(cell);
		}
		else
		{
			cell.generateDigest(m_digestDf);
		}
		cell.doCryptography(m_encryptionEngine);
		return cell;
	}

	/**
	 * decrypts a RelayCell
	 * @param cell
	 * encrypted cell
	 * @return
	 * decrypted cell
	 * @throws Exception
	 */
	public synchronized RelayCell decryptCell(RelayCell cell) throws Exception
	{
		RelayCell c = cell;
		c.doCryptography(this.m_decryptionEngine);
		if (m_nextOR != null && m_extended)
		{
			c = m_nextOR.decryptCell(c);
		}
		else
		{
			c.checkDigest(m_digestDb);
		}
		return c;
	}

	/**
	 * create cell
	 *
	 * this cell is needed to connect to the first OR.
	 * after the connection is established extendConnection is used
	 * @return
	 * createCell
	 * @throws Exception
	 */
	public CreateCell createConnection() throws Exception
	{
		CreateCell cell = new CreateCell(m_circID);
		cell.setPayload(createExtendOnionSkin(), 0);
		return cell;
	}

	/**
	 * checks the created cell if the answer was right
	 * @param cell
	 * createdcell
	 * @throws Exception
	 */
	public boolean checkCreatedCell(Cell cell)
	{
		try
		{
			checkExtendParameters(cell.getPayload(), 0, 148);
			return true;
		}
		catch (Throwable t)
		{
			return false;
		}
	}

	/**
	 * extends the connection to another OR
	 * @param address
	 * address of the OR
	 * @param port
	 * port
	 * @return
	 * RelayCell with the data that is needed to connect to the new OR
	 * @throws IOException
	 * @throws InvalidCipherTextException
	 */
	private RelayCell extendConnection(String address, int port) throws IOException,
		InvalidCipherTextException,Exception
	{
		RelayCell cell;

		byte[] payload = ByteArrayUtil.conc(InetAddress.getByName(address).getAddress(),
											ByteArrayUtil.inttobyte(port, 2), createExtendOnionSkin());

		MyRSAPublicKey key = m_description.getSigningKey();
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		DEROutputStream dout = new DEROutputStream(out);
		dout.writeObject(key.getAsSubjectPublicKeyInfo().getPublicKey());
		dout.flush();
		byte[] b = out.toByteArray();
		byte[] hash1 = hash.sha(b);

		payload = ByteArrayUtil.conc(payload, hash1);

		cell = new RelayCell(m_circID, RelayCell.RELAY_EXTEND, 0, payload);
		return cell;
	}

	/**
	 * extends the connction to another OR and encrypts the data
	 * @param description
	 * ORDescription
	 * @return
	 * cell that is needed to extend the connection
	 * @throws IOException
	 * @throws InvalidCipherTextException
	 */
	public RelayCell extendConnection(ORDescriptor description) throws IOException,
		InvalidCipherTextException,Exception
	{
		RelayCell cell;
		if (m_nextOR == null)
		{
			m_nextOR = new OnionRouter(m_circID, description);
			cell = m_nextOR.extendConnection(description.getAddress(), description.getPort());
			cell.generateDigest(m_digestDf);
		}
		else
		{
			cell = m_nextOR.extendConnection(description);
		}
		cell.doCryptography(m_encryptionEngine);
		return cell;
	}

	/**
	 * checks if the extendedcell has the right parameters and hash
	 * @param cell
	 * cell
	 * @throws Exception
	 */
	public boolean checkExtendedCell(RelayCell cell)
	{
		try
		{
			if (m_nextOR == null)
			{
				checkExtendParameters(cell.getPayload(), 11, 148);
				LogHolder.log(LogLevel.DEBUG, LogType.MISC, "[TOR] Circuit '" + m_circID + "' Extended");
				return true;
			}
			else
			{
				cell.doCryptography(m_decryptionEngine);
				if (!m_extended)
				{
					cell.checkDigest(m_digestDb);
					m_extended = m_nextOR.checkExtendedCell(cell);
					if (!m_extended)
					{
						m_nextOR = null;
					}
					return m_extended;
				}
				else
				{
					return m_nextOR.checkExtendedCell(cell);
				}
			}
		}
		catch (Exception e)
		{
			return false;
		}
	}

	/**
	 * creates the onion skin for a create or extend cell
	 * @return
	 * payload
	 * @throws IOException
	 * @throws InvalidCipherTextException
	 */
	private byte[] createExtendOnionSkin() throws IOException, InvalidCipherTextException,Exception
	{
		byte[] rsaBlock = new byte[86];
		byte[] key = new byte[16];

		//generate AES Key and Engine
		MyAES aes = new MyAES();
		SecureRandom random = new SecureRandom();
		random.nextBytes(key);
		//initialize aes-ctr. with keyparam and an IV=0
		aes.init(true, key);

		//generate DH Parameters and Agreement
		DHKeyGenerationParameters params = new DHKeyGenerationParameters(new SecureRandom(), DH_PARAMS);
		DHKeyPairGenerator kpGen = new DHKeyPairGenerator();
		kpGen.init(params);
		AsymmetricCipherKeyPair pair = kpGen.generateKeyPair();
		DHPublicKeyParameters dhpub = (DHPublicKeyParameters) pair.getPublic();
		DHPrivateKeyParameters dhpriv = (DHPrivateKeyParameters) pair.getPrivate();
		m_dhe = new DHBasicAgreement();
		m_dhe.init(dhpriv);

		byte[] dhpubY = dhpub.getY().toByteArray();
		int dhpubOffset = 0;
		if (dhpubY[0] == 0)
		{
			dhpubOffset = 1;
		}
		System.arraycopy(key, 0, rsaBlock, 0, 16);
		System.arraycopy(dhpubY, dhpubOffset, rsaBlock, 16, 70);

		MyRSA rsa = new MyRSA();
		rsa.init(m_description.getOnionKey());

		rsaBlock = rsa.processBlockOAEP(rsaBlock, 0, rsaBlock.length);

		byte[] result = new byte[186];
		System.arraycopy(rsaBlock, 0, result, 0, 128);
		//generate AES encrypted part
		aes.processBytesCTR(dhpubY, 70 + dhpubOffset, result, 128, 58);
		return result;
	}

	/**
	 * checks the parameters of a extend cell and calculate the secrets
	 * @param param
	 * parameters
	 * @param offset
	 * offset of the parameters
	 * @param len
	 * length of the parameters
	 * @throws Exception
	 */
	private void checkExtendParameters(byte[] param, int offset, int len) throws Exception
	{
		DHPublicKeyParameters dhserverpub;
		byte[] a = new byte[128];
		System.arraycopy(param, offset, a, 0, 128);
		dhserverpub = new DHPublicKeyParameters(new BigInteger(1, a), DH_PARAMS);
		BigInteger key = m_dhe.calculateAgreement(dhserverpub);
		byte[] agreement = key.toByteArray();
		byte[] buff = new byte[129];
		if (agreement[0] == 0)
		{
			System.arraycopy(agreement, 1, buff, 0, 128);
		}
		else
		{
			System.arraycopy(agreement, 0, buff, 0, 128);
		}

		byte[] kh = hash.sha(buff);
		for (int i = 0; i < kh.length; i++)
		{
			if (kh[i] != param[i + offset + 128])
			{
				throw new Exception("wrong derivative key");
			}
		}

		//test, if the calculated key is in range (to prevent attacks)
		if (key.compareTo(MINKEY) == -1 || key.compareTo(MAXKEY) == 1)
		{
			throw new CryptoException("Calculated DH-Key is not in allowed range (KEY:" + key.doubleValue() +
									  ")");
		}
		//test, if at least 16 bits are "0" and 16 bits are "1"
		if (key.bitCount() < 16 || (1024 - key.bitCount()) < 16)
		{
			throw new CryptoException("Calculated DH-Key is not valid. Not enough zeros ore ones");
		}

		buff[128] = 1;
		m_digestDf = new SHA1Digest();
		byte[] keydata = hash.sha(buff);
		m_digestDf.update(keydata, 0, 20);
		buff[128] = 2;
		m_digestDb = new SHA1Digest();
		keydata = hash.sha(buff);
		m_digestDb.update(keydata, 0, 20);
		buff[128] = 3;
		keydata = hash.sha(buff);
		m_encryptionEngine = new MyAES();
		m_encryptionEngine.init(true, keydata, 0, 16);
		byte[] keyKb = new byte[16];
		System.arraycopy(keydata, 16, keyKb, 0, 4);
		buff[128] = 4;
		keydata = hash.sha(buff);
		System.arraycopy(keydata, 0, keyKb, 4, 12);
		m_decryptionEngine = new MyAES();
		m_decryptionEngine.init(true, keyKb);
	}

}
