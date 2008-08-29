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

import java.math.BigInteger;
import java.security.SecureRandom;

import org.bouncycastle.crypto.AsymmetricCipherKeyPair;
import org.bouncycastle.crypto.generators.RSAKeyPairGenerator;
import org.bouncycastle.crypto.params.RSAKeyGenerationParameters;
import org.bouncycastle.crypto.params.RSAPrivateCrtKeyParameters;

/**
 * This class creates key pairs for the RSA algorithm.
 * @author Rolf Wendolsky
 */
public class RSAKeyPair extends AsymmetricCryptoKeyPair
{
	public static final int KEY_LENGTH_2048 = 2048;

	/**
	 * Creates a new rsa key pair.
	 * @param a_privateKey an rsa private key
	 */
	public RSAKeyPair(MyRSAPrivateKey a_privateKey)
	{
		super(a_privateKey);
	}

	/**
	 * Creates a new rsa key pair.
	 * @param a_publicExponent the public encyption exponent; a small integer denoted e,
	 *                         often a prime close to a power of 2, for example
	 *                         3, 5, 7, 17, 257, or 65537.
	 * @param a_random a random number generator
	 * @param a_strength The bit-length of n = p*q.
	 * @param a_certainty The certainty, that the generated numbers are prime.
	 * @return a key pair or null if no key pair could be created with these parameters
	 */
	public static RSAKeyPair getInstance(BigInteger a_publicExponent, SecureRandom a_random,
										 int a_strength, int a_certainty)
	{
		RSAKeyPair keyPair;

		RSAKeyPairGenerator pGen = new RSAKeyPairGenerator();
		RSAKeyGenerationParameters genParam = new RSAKeyGenerationParameters(
			a_publicExponent, a_random, a_strength, a_certainty);
		pGen.init(genParam);
		AsymmetricCipherKeyPair pair = pGen.generateKeyPair();
		try
		{
			keyPair = new RSAKeyPair(new MyRSAPrivateKey((RSAPrivateCrtKeyParameters) pair.getPrivate()));
		}
		catch (Exception a_e)
		{
			keyPair = null;
		}

		if (!isValidKeyPair(keyPair))
		{
			return null;
		}

		return keyPair;
	}

	/**
	 * Creates a new rsa key pair with a public exponent of 65537.
	 * @param a_random a random number generator
	 * @param a_strength The bit-length of n = p*q.
	 * @param a_certainty The certainty, that the generated numbers are prime.
	 * @return a key pair or null if no key pair could be created with these parameters
	 */

	public static RSAKeyPair getInstance(SecureRandom a_random, int a_strength, int a_certainty)
	{
		return getInstance(new BigInteger("65537"), a_random, a_strength, a_certainty);
	}
}
