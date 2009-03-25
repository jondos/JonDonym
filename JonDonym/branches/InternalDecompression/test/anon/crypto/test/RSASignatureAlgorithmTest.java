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

import java.math.BigInteger;
import java.security.SecureRandom;

import anon.crypto.AsymmetricCryptoKeyPair;
import anon.crypto.MyRSASignature;
import anon.crypto.RSAKeyPair;

/**
 * These are the tests for the RSA signature algorithm.
 * @author Rolf Wendolsky
 */
public class RSASignatureAlgorithmTest extends AbstractSignatureAlgorithmTest
{
	/**
	 * Creates a new test case.
	 * @param a_name the name of the test case
	 */
	public RSASignatureAlgorithmTest(String a_name)
	{
		super(a_name);
	}

	/**
	 * This method initialises the keys and the signature algorithm.
	 */
	protected void setUp()
	{
		SecureRandom random = new SecureRandom();
		MyRSASignature algorithm = new MyRSASignature();
		AsymmetricCryptoKeyPair keyPair;

		random.setSeed(932365624);

		// initialise the algorithm
		setSignatureAlgorithm(algorithm);

		// initialise the keys
		for (int i = 0; i < NUMBER_OF_KEYS; i++)
		{
			keyPair = RSAKeyPair.getInstance(BigInteger.valueOf(0x11), random, 512, 0);
			getPrivateKeys()[i] = keyPair.getPrivate();
			getPublicKeys()[i] = keyPair.getPublic();

		}
	}
}
