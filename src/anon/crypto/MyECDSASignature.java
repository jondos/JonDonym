package anon.crypto;
import java.io.IOException;
import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.Key;

import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;

import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.asn1.x9.X9ObjectIdentifiers;
import org.bouncycastle.crypto.digests.SHA1Digest;
import org.bouncycastle.crypto.signers.ECDSASigner;



public final class MyECDSASignature implements IMySignature
{
	private static final AlgorithmIdentifier ms_identifier = 
		new AlgorithmIdentifier(X9ObjectIdentifiers.ecdsa_with_SHA1);
	
	SHA1Digest m_digest;
	ECDSASigner m_signatureAlgorithm;
	
	private Key m_initKey;
	
	public MyECDSASignature()
	{
		m_digest = new SHA1Digest();
		m_signatureAlgorithm = new ECDSASigner();
	}
	
	public byte[] encodeForXMLSignature(byte[] a_signature)
	{
		int l = ((MyECPrivateKey) m_initKey).getPrivateParams().getParameters().getN().toByteArray().length;
		byte rLength = a_signature[3];
		byte sLength = a_signature[3 + rLength + 2];
		
		byte[] rsBuff = new byte[2*l];
		for (int i = 0; i < 2*l; i++)
		{
			/* be sure that it is zero */
			rsBuff[i] = 0;
		}
		System.arraycopy(a_signature, 4 , rsBuff, l - rLength, rLength);
		
		System.arraycopy(a_signature, 4 + rLength + 2 , rsBuff, 2*l - sLength, sLength);

		return rsBuff;
	}
	
	public byte[] decodeForXMLSignature(byte[] a_encodedSignature)
	{
		byte[] result;
		
		int l = ((MyECPublicKey) m_initKey).getPublicParams().getParameters().getN().toByteArray().length;
		if(a_encodedSignature.length != 2*l)
		{
			return null;
		}
		byte[] r_raw = new byte[l];
		byte[] s_raw = new byte[l];
		System.arraycopy(a_encodedSignature, 0, r_raw, 0, l);
		System.arraycopy(a_encodedSignature, l, s_raw, 0, l);
		try
		{
			result = MyDSASignature.derEncode(new BigInteger(r_raw), new BigInteger(s_raw));
		} 
		catch (IOException e)
		{
			result = null;
		}
		
		return result;
	}
	
	/**
	 * @return http://www.w3.org/2001/04/xmldsig-more#ecdsa-sha1
	 */
	public String getXMLSignatureAlgorithmReference()
	{
		return "http://www.w3.org/2001/04/xmldsig-more#ecdsa-sha1";
	}

	synchronized public void initSign(IMyPrivateKey k) throws InvalidKeyException
	{
		try
		{
			MyECPrivateKey key = (MyECPrivateKey) k;
			m_signatureAlgorithm.init(true, key.getPrivateParams());
			m_initKey = k;
		}
		catch (Exception ex2)
		{
			throw new InvalidKeyException(
				"MyECDSASignautre - initVerify - dont know how to handle the given key");
		}

	}

	synchronized public void initVerify(IMyPublicKey k) throws InvalidKeyException
	{
		try
		{
			MyECPublicKey key = (MyECPublicKey) k;
			m_signatureAlgorithm.init(false, key.getPublicParams());
			m_digest.reset();
			m_initKey = k;
		}
		catch (Exception ex2)
		{
			throw new InvalidKeyException(
				"MyECDSASignautre - initVerify - dont know how to handle the given key");
		}
	}

	synchronized public byte[] sign(byte[] bytesToSign)
	{
		try
		{
			m_digest.reset();
			m_digest.update(bytesToSign,0,bytesToSign.length);
			byte hash[] = new byte[m_digest.getDigestSize()];
			m_digest.doFinal(hash,0);
			BigInteger rs[] = m_signatureAlgorithm.generateSignature(hash);
			return MyDSASignature.derEncode(rs[0], rs[1]);
		}
		catch (Throwable t)
		{
			return null;
		}
	}
	
	synchronized public boolean verify(byte[] a_message, int message_offset, int message_len,
			byte[] a_signature, int signature_offset, int signature_len)
	{
		try
		{
			m_digest.reset();
			m_digest.update(a_message,message_offset,message_len);
			byte[]hash=new byte[m_digest.getDigestSize()];
			m_digest.doFinal(hash,0);
			BigInteger rs[]= MyDSASignature.derDecode(a_signature,signature_offset,signature_len);
			return m_signatureAlgorithm.verifySignature(hash,rs[0],rs[1]);
		}
		catch (Throwable e)
		{
			LogHolder.log(LogLevel.DEBUG, LogType.CRYPTO, "Signature algorithm does not match!");
			return false;
		}
	}

	synchronized public boolean verify(byte[] a_message, byte[] a_signature)
	{
		return verify(a_message, 0, a_message.length, a_signature, 0, a_signature.length);
	}

	public AlgorithmIdentifier getIdentifier()
	{
		return ms_identifier;
	}
}
