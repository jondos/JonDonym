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
package anon.crypto.test;

import java.security.InvalidKeyException;
import java.security.MessageDigest;

import org.bouncycastle.asn1.DERObjectIdentifier;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;

import anon.crypto.IMyPrivateKey;
import anon.crypto.IMyPublicKey;
import anon.crypto.IMySignature;
import anon.util.Util;


public class DummySignatureAlgorithm implements IMySignature
{
	private static final AlgorithmIdentifier ms_identifier =
		new AlgorithmIdentifier(new DERObjectIdentifier("0.0.0.0.0.0.0"));

	boolean m_bVerify = false;
	boolean m_bSign = false;
	long m_key;

	/**
	 * Initialises the algorithm for verifying. This must be done before doing the verify operation.
	 * @param a_publicKey a public key
	 * @throws InvalidKeyException if the key is invalid
	 */
	public void initVerify(IMyPublicKey a_publicKey) throws InvalidKeyException
	{
		if (a_publicKey == null || !(a_publicKey instanceof DummyPublicKey))
		{
			throw new InvalidKeyException();
		}

		m_bVerify = true;
		m_bSign = false;
		m_key = ((DummyPublicKey)a_publicKey).getKeyValue();
	}

	/**
	 * Initialises the algorithm for signing. This must be done before doing the sign operation.
	 * @param a_privateKey a private key
	 * @throws InvalidKeyException if the key is invalid
	 */
	public void initSign(IMyPrivateKey a_privateKey) throws InvalidKeyException
	{
		if (a_privateKey == null || !(a_privateKey instanceof DummyPrivateKey))
		{
			throw new InvalidKeyException();
		}

		m_bVerify = false;
		m_bSign = true;
		m_key = ((DummyPrivateKey)a_privateKey).getKeyValue();
	}

	/**
	 * Tests if the signature of a specified message is valid.
	 * @param a_message a message
	 * @param message_offset start of message
	 * @param message_len length of message
	 * @param a_signature a signature
	 * @param signature_offset start of signature
	 * @param signature_len length of signature
	 * @return true if the signature of a specified message is valid; false otherwiese
	 */
	 public boolean verify(byte[] a_message, int message_offset,int message_len,
						   byte[] a_signature,int signature_offset,int signature_len)
	 {
		 boolean bValid;
		 if (message_offset < 0 || signature_offset < 0 || message_len < 0 || signature_len < 0 ||
			 message_len + message_offset > a_message.length ||
			 signature_len + signature_offset > a_signature.length)
		 {
			 return false;
		 }

		 byte[] message = new byte[message_len];
		 byte[] signature = new byte[signature_len];

		 System.arraycopy(a_message, message_offset, message, 0, message_len);
		 System.arraycopy(a_signature, signature_offset, signature, 0, signature_len);

		 if (!m_bVerify)
		 {
			 return false;
		 }

		 m_bSign = true;
		 bValid = Util.arraysEqual(sign(message), signature);
		 m_bSign = false;

		return bValid;
}

	/**
	 * Tests if the signature of a specified message is valid.
	 * @param a_message a message
	 * @param a_signature a signature
	 * @return true if the signature of a specified message is valid; false otherwiese
	 */
	public boolean verify(byte[] a_message, byte[] a_signature)
	{
		return verify(a_message, 0, a_message.length, a_signature, 0, a_signature.length);
	}

	/**
	 * Signs a message and returns the signature.
	 * @param a_message a message
	 * @return the signature that was created
	 */
	public byte[] sign(byte[] a_message)
	{
		MessageDigest md5;
		byte[] signature;
		String message;

		if (!m_bSign)
		{
			return null;
		}

		message = new String(a_message);
		message += m_key;

		try
		{
			md5 = MessageDigest.getInstance("MD5");
			signature = md5.digest(message.getBytes());
		}
		catch(Exception a_e)
		{
			signature = null;
		}

		return signature;
	}

	/**
	 * Returns the algorithm identifier (dummy).
	 * @return the algorithm identifier (dummy)
	 */
	public AlgorithmIdentifier getIdentifier() {
		return ms_identifier;
	}

	/**
	 * This is a dummy implementation. It returns the unmodified signature that was given
	 * as argument.
	 * @param a_signature a signature
	 * @return the unmodified signature that was given as argument
	 */
	public byte[] encodeForXMLSignature(byte[] a_signature)
	{
		return a_signature;
	}

	/**
	 * This is a dummy implementation. It returns the unmodified signature that was given
	 * as argument.
	 * signature values.
	 * @param a_signature a signature
	 * @return the unmodified signature that was given as argument
	 */
	public byte[] decodeForXMLSignature(byte[] a_signature)
	{
		return a_signature;
	}

	/**
	 * This is a dummy implementation that returns "Dummy".
	 * @return "Dummy"
	 */
	public String getXMLSignatureAlgorithmReference()
	{
		return "Dummy";
	}

}
