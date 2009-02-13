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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.security.InvalidKeyException;
import java.security.Key;

import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;

import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.ASN1Null;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.DERObjectIdentifier;
import org.bouncycastle.asn1.DEROutputStream;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.asn1.x509.DigestInfo;
import org.bouncycastle.asn1.x509.X509ObjectIdentifiers;
import org.bouncycastle.crypto.digests.SHA1Digest;
import org.bouncycastle.crypto.encodings.PKCS1Encoding;
import org.bouncycastle.crypto.engines.RSAEngine;

/*** SHA1withRSA Signature as described in RFC 2437*/
public final class MyRSASignature implements IMySignature
{
	private static final AlgorithmIdentifier ms_identifier =
		new AlgorithmIdentifier(new DERObjectIdentifier("1.2.840.113549.1.1.5"));

	private PKCS1Encoding m_SignatureAlgorithm;
	/**
	 * The key with that this algorithm has been initialised.
	 */
	private Key m_initKey;
	private SHA1Digest m_Digest;
	private final static AlgorithmIdentifier ms_AlgID=
		new AlgorithmIdentifier(X509ObjectIdentifiers.id_SHA1,null);

	public MyRSASignature()
	{
		m_SignatureAlgorithm = new PKCS1Encoding(new RSAEngine());
		m_Digest=new SHA1Digest();
	}

	synchronized public void initVerify(IMyPublicKey k) throws InvalidKeyException
	{
		//if (m_initKey == null || m_initKey != k)
		//{
			m_SignatureAlgorithm.init(false, ( (MyRSAPublicKey) k).getParams());
			m_initKey = k;
		//}
	}

	synchronized public void initSign(IMyPrivateKey k) throws InvalidKeyException
	{
		//if (m_initKey == null || m_initKey != k)
		//{
			m_SignatureAlgorithm.init(true, ( (MyRSAPrivateKey) k).getParams());
			m_initKey = k;
		//}
	}

	synchronized public boolean verify(byte[] a_message, byte[] a_signature)
	{
		return verify(a_message, 0, a_message.length, a_signature, 0, a_signature.length);
	}

	synchronized public boolean verify(byte[] message, int message_offset, int message_len,
									   byte[] sig, int signature_offset, int signature_len)
	{
		try
		{
			m_Digest.reset();
			m_Digest.update(message, message_offset, message_len);
			byte[] hash = new byte[m_Digest.getDigestSize()];
			m_Digest.doFinal(hash, 0);

			byte[] decryptedSig = m_SignatureAlgorithm.processBlock(sig, signature_offset, signature_len);
			ByteArrayInputStream bIn = new ByteArrayInputStream(decryptedSig);
			ASN1InputStream dIn = new ASN1InputStream(bIn);


			DigestInfo digInfo = new DigestInfo( (ASN1Sequence) dIn.readObject());

			if (!digInfo.getAlgorithmId().getObjectId().equals(ms_AlgID.getObjectId()))
			{
				return false;
			}

			Object o = digInfo.getAlgorithmId().getParameters();
			if (o != null && ! (o instanceof ASN1Null))
			{
				return false;
			}

			byte[] sigHash = digInfo.getDigest();

			if (hash.length != sigHash.length)
			{
				return false;
			}

			for (int i = 0; i < hash.length; i++)
			{
				if (sigHash[i] != hash[i])
				{
					return false;
				}
			}

			return true;
		}
		catch (Throwable e)
		{
			LogHolder.log(LogLevel.DEBUG, LogType.CRYPTO, "Signature algorithm does not match!");
			return false;
		}
	}


	/** Verifyes a signature for a given hash*/
	synchronized public boolean verifyPlain(byte[] hash, byte[] sig)
	{
		try
		{
			byte[] sigHash = m_SignatureAlgorithm.processBlock(sig, 0, sig.length);

			if (hash.length != sigHash.length)
			{
				return false;
			}

			for (int i = 0; i < hash.length; i++)
			{
				if (sigHash[i] != hash[i])
				{
					return false;
				}
			}

			return true;
		}
		catch (Exception e)
		{
			return false;
		}
	}

	public synchronized byte[] sign(byte[] bytesToSign)
	{
		try
		{
			byte[]  hash = new byte[m_Digest.getDigestSize()];
			m_Digest.reset();
			m_Digest.update(bytesToSign,0,bytesToSign.length);
			m_Digest.doFinal(hash, 0);

			ByteArrayOutputStream   bOut = new ByteArrayOutputStream();
			DEROutputStream         dOut = new DEROutputStream(bOut);
			DigestInfo              dInfo = new DigestInfo(ms_AlgID, hash);
	  	  	dOut.writeObject(dInfo);
	  		byte[]  bytes= bOut.toByteArray();

			return m_SignatureAlgorithm.processBlock(bytes, 0, bytes.length);
		}
		catch (Throwable t)
		{
			return null;
		}
	}

	/** Only does the signature calculation assuming that the input already is a hash*/
	public synchronized byte[] signPlain(byte[] hash)
	{
		try
		{

			return m_SignatureAlgorithm.processBlock(hash, 0, hash.length);
		}
		catch (Throwable t)
		{
			return null;
		}
	}

	/**
	 * Returns the algorithm identifier (RSA with SHA1).
	 * @return the algorithm identifier (RSA with SHA1)
	 */
	public AlgorithmIdentifier getIdentifier()
	{
		return ms_identifier;
	}

	/**
	 * Encodes a signature in a way it meets the W3C standard for RSA XML signature values.
	 * Note: No further encoding has to be done!
	 * @see http://tools.ietf.org/html/rfc2437#section-8.1
	 * @param a_signature an non-encoded signature
	 * @return the encoded signature in PKCS1 format or null if an error occured
	 * @see http://www.w3.org/TR/xmldsig-core/#sec-PKCS1
	 */
	public byte[] encodeForXMLSignature(byte[] a_signature)
	{
		return a_signature;
	}

	/**
	 * Tries to decode a signature in a way as it would meet the W3C standard for RSA XML
	 * signature values.
	 * Note: No further decoding has to be done!
	 * @see http://tools.ietf.org/html/rfc2437#section-8.1
	 * @param a_encodedSignature an encoded signature in PKCS1 format
	 * @return the decoded signature or null if an error occured
	 * @see http://www.w3.org/TR/xmldsig-core/#sec-PKCS1
	 */
	public byte[] decodeForXMLSignature(byte[] a_encodedSignature)
	{
		return a_encodedSignature;
	}

	/**
	 * Returns http://www.w3.org/2000/09/xmldsig#rsa-sha1.
	 * @return http://www.w3.org/2000/09/xmldsig#rsa-sha1
	 */
	public String getXMLSignatureAlgorithmReference()
	{
		return "http://www.w3.org/2000/09/xmldsig#rsa-sha1";
	}
}
