/*
 Copyright (c) 2000 - 2005, The JAP-Team
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
package anon.crypto;

import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;

/**
 * This class contains all basic signature operations.
 * @author Rolf Wendolsky
 */
public final class ByteSignature
{
	/**
	 * This class works without being initialised and is completely static.
	 * Therefore, the constructor is not needed and private.
	 */
	private ByteSignature()
	{
	}

	/**
	 * Verifies the signature for a message.
	 * @param a_message a message
	 * @param a_signature the signature to verify
	 * @param a_keyPair a key pair for verification of the signature
	 * @return true if the signature is valid; false otherwise
	 */
	public static boolean verify(byte[] a_message, byte[] a_signature, AsymmetricCryptoKeyPair a_keyPair)
	{
		return verify(a_message, a_signature, a_keyPair.getPublic());
	}

	/**
	 * Verifies the signature for a message.
	 * @param a_message a message
	 * @param a_signature the signature to verify
	 * @param a_publicKey a public key for verification of the signature
	 * @return true if the signature is valid; false otherwise
	 */
	public static boolean verify(byte[] a_message, byte[] a_signature, IMyPublicKey a_publicKey)
	{
		if (a_publicKey == null)
		{
			LogHolder.log(LogLevel.DEBUG, LogType.CRYPTO, "key==null!");
			return false;
		}

		//synchronized (a_publicKey.getSignatureAlgorithm())
		{
			//LogHolder.log(LogLevel.DEBUG, LogType.CRYPTO,
			//	"Try to verify a message...");
			ISignatureVerificationAlgorithm algorithm = a_publicKey.getSignatureAlgorithm();
			if (algorithm != null)
			{
				//LogHolder.log(LogLevel.DEBUG, LogType.CRYPTO,
				//			  "Try to verify a message - using algorithm: " + algorithm.toString());
			}
			else
			{
				LogHolder.log(LogLevel.EXCEPTION, LogType.CRYPTO,
							  "Try to verify a message - unknown algorithm!");
				return false;
			}
			return algorithm.verify(a_message, a_signature);
		}
	}

	/**
	 * Signs a message.
	 * @param a_message the message to sign
	 * @param a_keyPair a key pair for signing
	 * @return the signature or null if no signature could be created
	 */
	public static byte[] sign(byte[] a_message, AsymmetricCryptoKeyPair a_keyPair)
	{
		return sign(a_message, a_keyPair.getPrivate());

	}

	/**
	 * Signs a message.
	 * @param a_message the message to sign
	 * @param a_privateKey a private key for signing
	 * @return the signature or null if no signature could be created
	 */
	public static byte[] sign(byte[] a_message, IMyPrivateKey a_privateKey)
	{
		if (a_privateKey == null)
		{
			return null;
		}
		// synchronization is needed so that no one else can reinit the signature algorithm
		//synchronized (a_privateKey.getSignatureAlgorithm())
		{
			return a_privateKey.getSignatureAlgorithm().sign(a_message);
		}
	}

	/**
	 * Creates, from a given byte array, a readable byte string of the form AA:C3:02:21:...
	 * @param a_bytes a byte array
	 * @return a readable byte string of the form AA:C3:02:21:...
	 */
	public static String toHexString(byte[] a_bytes)
	{
		String currentValue;
		String value = "";

		if (a_bytes == null || a_bytes.length == 0)
		{
			return value;
		}

		for (int i = 0; i < a_bytes.length; i++)
		{
			currentValue = Integer.toHexString(255 & a_bytes[i]).toUpperCase();
			if (currentValue.length() == 1)
			{
				currentValue = "0" + currentValue;
			}

			value += currentValue;
			if (i + 1 < a_bytes.length)
			{
				value += ":";
			}
		}

		return value;
	}
}
