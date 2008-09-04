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
package anon.crypto.tinytls.keyexchange;

import java.math.BigInteger;
import java.security.SecureRandom;

import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;

import org.bouncycastle.crypto.AsymmetricCipherKeyPair;
import org.bouncycastle.crypto.agreement.DHBasicAgreement;
import org.bouncycastle.crypto.generators.DHKeyPairGenerator;
import org.bouncycastle.crypto.params.DHKeyGenerationParameters;
import org.bouncycastle.crypto.params.DHParameters;
import org.bouncycastle.crypto.params.DHPrivateKeyParameters;
import org.bouncycastle.crypto.params.DHPublicKeyParameters;

import anon.crypto.IMyPrivateKey;
import anon.crypto.JAPCertificate;
import anon.crypto.MyDSAPrivateKey;
import anon.crypto.MyDSAPublicKey;
import anon.crypto.MyDSASignature;
import anon.crypto.tinytls.TLSException;
import anon.crypto.tinytls.util.PRF;
import anon.crypto.tinytls.util.hash;
import anon.util.ByteArrayUtil;

public class DHE_DSS_Key_Exchange extends Key_Exchange
{

	//maximum length for keymaterial (3DES_EDE_CBC_SHA)
	//see RFC2246 for more information
	private final static int MAXKEYMATERIALLENGTH = 104;

	private final static byte[] CLIENTFINISHEDLABEL = ("client finished").getBytes();

	private final static byte[] SERVERFINISHEDLABEL = ("server finished").getBytes();

	private final static byte[] KEYEXPANSION = ("key expansion").getBytes();

	private final static byte[] MASTERSECRET = ("master secret").getBytes();

	private final static BigInteger SAFEPRIME = new BigInteger(
		"00FFFFFFFFFFFFFFFFC90FDAA22168C234C4C6628B80DC1CD129024E08" +
		"8A67CC74020BBEA63B139B22514A08798E3404DDEF9519B3CD3A431B" +
		"302B0A6DF25F14374FE1356D6D51C245E485B576625E7EC6F44C42E9" +
		"A637ED6B0BFF5CB6F406B7EDEE386BFB5A899FA5AE9F24117C4B1FE6" +
		"49286651ECE65381FFFFFFFFFFFFFFFF", 16);
	private final static DHParameters DH_PARAMS = new DHParameters(SAFEPRIME, new BigInteger("2"));

	private DHParameters m_dhparams;
	private DHPublicKeyParameters m_dhserverpub;
	private byte[] m_premastersecret;
	private byte[] m_mastersecret;
	private byte[] m_clientrandom;
	private byte[] m_serverrandom;

	private DHBasicAgreement m_dhe = null;

	public byte[] generateServerKeyExchange(IMyPrivateKey key, byte[] clientrandom, byte[] serverrandom) throws
		TLSException
	{
		if (! (key instanceof MyDSAPrivateKey))
		{
			throw new TLSException("wrong key type (cannot cast to MyDSAPrivateKey)");
		}
		MyDSAPrivateKey dsakey = (MyDSAPrivateKey) key;
		this.m_clientrandom = clientrandom;
		this.m_serverrandom = serverrandom;
		DHKeyGenerationParameters params = new DHKeyGenerationParameters(new SecureRandom(), DH_PARAMS);
		DHKeyPairGenerator kpGen = new DHKeyPairGenerator();
		kpGen.init(params);
		AsymmetricCipherKeyPair pair = kpGen.generateKeyPair();
		DHPublicKeyParameters dhpub = (DHPublicKeyParameters) pair.getPublic();
		DHPrivateKeyParameters dhpriv = (DHPrivateKeyParameters) pair.getPrivate();
		m_dhe = new DHBasicAgreement();
		m_dhe.init(dhpriv);

		byte[] dh_p = dhpub.getParameters().getP().toByteArray();
		dh_p = ByteArrayUtil.conc(ByteArrayUtil.inttobyte(dh_p.length, 2), dh_p);
		byte[] dh_g = dhpub.getParameters().getG().toByteArray();
		dh_g = ByteArrayUtil.conc(ByteArrayUtil.inttobyte(dh_g.length, 2), dh_g);
		byte[] dh_y = dhpub.getY().toByteArray();
		dh_y = ByteArrayUtil.conc(ByteArrayUtil.inttobyte(dh_y.length, 2), dh_y);

		byte[] message = ByteArrayUtil.conc(dh_p, dh_g, dh_y);
		//byte[] signature = hash.sha(clientrandom, serverrandom, message);
		byte[] signature = ByteArrayUtil.conc(clientrandom, serverrandom, message);

		MyDSASignature sig = new MyDSASignature();
		try
		{
			sig.initSign(dsakey);
		}
		catch (Exception ex)
		{
			throw new TLSException("wrong key type (cannot init signature algorithm ("+ex.getMessage()+"))");
		}

		byte[] signature2 = sig.sign(signature);

		message=ByteArrayUtil.conc(message, ByteArrayUtil.inttobyte(signature2.length, 2), signature2);

		return message;
	}

	public void processServerKeyExchange(byte[] bytes, int bytes_offset, int bytes_len,
										 byte[] clientrandom, byte[] serverrandom,
										 JAPCertificate servercertificate) throws TLSException
	{
		this.m_clientrandom = clientrandom;
		this.m_serverrandom = serverrandom;
		BigInteger dh_p=null;
		BigInteger dh_g=null;
		BigInteger dh_ys=null;
		int start_of_server_params=bytes_offset;
		byte[] dummy=null;
		int length = ( (bytes[bytes_offset] & 0xFF) << 8) | (bytes[bytes_offset+1] & 0xFF);
		bytes_offset += 2;
		dummy = ByteArrayUtil.copy(bytes, bytes_offset, length);
		bytes_offset += length;
		dh_p = new BigInteger(1, dummy);
		LogHolder.log(LogLevel.DEBUG, LogType.MISC, "[SERVER_KEY_EXCHANGE] DH_P = " + dh_p.toString());

		length = ( (bytes[bytes_offset] & 0xFF) << 8) | (bytes[bytes_offset+1] & 0xFF);
		bytes_offset += 2;
		dummy = ByteArrayUtil.copy(bytes, bytes_offset, length);
		bytes_offset += length;
		dh_g = new BigInteger(1, dummy);
		LogHolder.log(LogLevel.DEBUG, LogType.MISC, "[SERVER_KEY_EXCHANGE] DH_G = " + dh_g.toString());

		length = ( (bytes[bytes_offset] & 0xFF) << 8) | (bytes[bytes_offset+1] & 0xFF);
		bytes_offset += 2;
		dummy = ByteArrayUtil.copy(bytes, bytes_offset, length);
		bytes_offset += length;
		dh_ys = new BigInteger(1, dummy);
		LogHolder.log(LogLevel.DEBUG, LogType.MISC, "[SERVER_KEY_EXCHANGE] DH_Ys = " + dh_ys.toString());

		this.m_dhparams = new DHParameters(dh_p, dh_g);
		this.m_dhserverpub = new DHPublicKeyParameters(dh_ys, this.m_dhparams);

		//-----------------------------------------


		byte[] serverparams = ByteArrayUtil.copy(bytes, start_of_server_params,bytes_offset-start_of_server_params);

		byte[] expectedSignature = /*hash.sha*/ ByteArrayUtil.conc(clientrandom, serverrandom, serverparams);

		length = ( (bytes[bytes_offset] & 0xFF) << 8) | (bytes[bytes_offset+1] & 0xFF);
		bytes_offset += 2;

		MyDSAPublicKey dsakey;
		MyDSASignature sig = new MyDSASignature();
		if (servercertificate.getPublicKey() instanceof MyDSAPublicKey)
		{
			dsakey = (MyDSAPublicKey) servercertificate.getPublicKey();
		}
		else
		{
			throw new TLSException("cannot decode certificate");
		}
		try
		{
			sig.initVerify(dsakey);
		}
		catch (Exception ex)
		{
		}

		if (!sig.verify(expectedSignature, 0,expectedSignature.length,
						bytes,  bytes_offset, length))
		{
			LogHolder.log(LogLevel.DEBUG, LogType.MISC, "[SERVER_KEY_EXCHANGE] Signature wrong");
			throw new TLSException("wrong Signature",2,21);
		}
		else
		{
			LogHolder.log(LogLevel.DEBUG, LogType.MISC, "[SERVER_KEY_EXCHANGE] Signature ok");
		}
	}

	public byte[] calculateServerFinished(byte[] handshakemessages)
	{
		PRF prf = new PRF(this.m_mastersecret, SERVERFINISHEDLABEL,
						  ByteArrayUtil.conc(hash.md5(handshakemessages), hash.sha(handshakemessages)));
		return prf.calculate(12);
	}

	public void processServerFinished(byte[] b, int len, byte[] handshakemessages) throws TLSException
	{
		PRF prf = new PRF(this.m_mastersecret, SERVERFINISHEDLABEL,
						  ByteArrayUtil.conc(hash.md5(handshakemessages), hash.sha(handshakemessages)));
		byte[] c = prf.calculate(12);
		if (b[0] == 20 && b[1] == 0 && b[2] == 0 && b[3] == 12)
		{
			for (int i = 0; i < c.length; i++)
			{
				if (c[i] != b[i + 4])
				{
					throw new TLSException("wrong Server Finished message recieved", 2, 20);
				}
			}
			return;
		}
		throw new TLSException("wrong Server Finished message recieved", 2, 10);
	}

	public void processClientKeyExchange(BigInteger dh_y)
	{
		DHPublicKeyParameters dhclientpub = new DHPublicKeyParameters(dh_y, DH_PARAMS);
		this.m_premastersecret = m_dhe.calculateAgreement(dhclientpub).toByteArray();
		if (this.m_premastersecret[0] == 0)
		{
			this.m_premastersecret = ByteArrayUtil.copy(this.m_premastersecret, 1,
				this.m_premastersecret.length - 1);
		}
		PRF prf = new PRF(this.m_premastersecret, MASTERSECRET,
						  ByteArrayUtil.conc(this.m_clientrandom, this.m_serverrandom));
		this.m_mastersecret = prf.calculate(48);
		this.m_premastersecret = null;
	}

	public byte[] calculateClientKeyExchange() throws TLSException
	{
		DHKeyGenerationParameters params = new DHKeyGenerationParameters(new SecureRandom(), this.m_dhparams);
		DHKeyPairGenerator kpGen = new DHKeyPairGenerator();
		kpGen.init(params);

		AsymmetricCipherKeyPair pair = kpGen.generateKeyPair();
		DHPublicKeyParameters dhpub = (DHPublicKeyParameters) pair.getPublic();
		DHPrivateKeyParameters dhpriv = (DHPrivateKeyParameters) pair.getPrivate();

		DHBasicAgreement dha = new DHBasicAgreement();
		dha.init(dhpriv);
		this.m_premastersecret = dha.calculateAgreement(this.m_dhserverpub).toByteArray();
		if (this.m_premastersecret[0] == 0)
		{
			this.m_premastersecret = ByteArrayUtil.copy(this.m_premastersecret, 1,
				this.m_premastersecret.length - 1);
		}
		PRF prf = new PRF(this.m_premastersecret, MASTERSECRET,
						  ByteArrayUtil.conc(this.m_clientrandom, this.m_serverrandom));
		this.m_mastersecret = prf.calculate(48);
		this.m_premastersecret = null;
		return dhpub.getY().toByteArray();
	}

	public void processClientFinished(byte[] verify_data, byte[] handshakemessages) throws TLSException
	{
		PRF prf = new PRF(this.m_mastersecret, CLIENTFINISHEDLABEL,
						  ByteArrayUtil.conc(hash.md5(handshakemessages), hash.sha(handshakemessages)));

	}

	public byte[] calculateClientFinished(byte[] handshakemessages) throws TLSException
	{
		PRF prf = new PRF(this.m_mastersecret, CLIENTFINISHEDLABEL,
						  ByteArrayUtil.conc(hash.md5(handshakemessages), hash.sha(handshakemessages)));
		return prf.calculate(12);
	}

	public byte[] calculateKeys()
	{
		PRF prf = new PRF(this.m_mastersecret, KEYEXPANSION,
						  ByteArrayUtil.conc(this.m_serverrandom, this.m_clientrandom));
		return prf.calculate(MAXKEYMATERIALLENGTH);
	}

}
