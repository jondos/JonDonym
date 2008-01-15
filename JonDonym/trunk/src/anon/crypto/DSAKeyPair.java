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

import java.security.SecureRandom;

import org.bouncycastle.crypto.AsymmetricCipherKeyPair;
import org.bouncycastle.crypto.generators.DSAKeyPairGenerator;
import org.bouncycastle.crypto.generators.DSAParametersGenerator;
import org.bouncycastle.crypto.params.DSAKeyGenerationParameters;
import org.bouncycastle.crypto.params.DSAPrivateKeyParameters;

/**
 * This class creates key pairs for the DSA algorithm.
 * @author Rolf Wendolsky
 */
public class DSAKeyPair extends AsymmetricCryptoKeyPair
{
	/**
	 * Creates a new DSA key pair.
	 *
	 * @param a_privateKey a private key
	 */
	public DSAKeyPair(MyDSAPrivateKey a_privateKey)
	{
		super(a_privateKey);
	}

	/**
	 * This creates a new DSA key pair.
	 *
	 * @param a_secureRandom A random number as initialization for the key generator.
	 * @param a_keyLength The length of the key in bits. For the current implementation of
	 *                    bouncycastle it must be a number between 256 and 1024 which is a multiple
	 *                    of 64.
	 * @param a_certainty Measure of robustness of prime. For FIPS 186-2 compliance this
	 *                             value should be at least 80.
	 * @return a key pair or null if no key pair could be created with these parameters
	 */
	public static DSAKeyPair getInstance(SecureRandom a_secureRandom, int a_keyLength, int a_certainty)
	{
		DSAKeyPair keyPair;

		DSAParametersGenerator dsaParametersGenerator = new DSAParametersGenerator();
		dsaParametersGenerator.init(a_keyLength, a_certainty, a_secureRandom);
		DSAKeyPairGenerator dsaKeyPairGenerator = new DSAKeyPairGenerator();
		dsaKeyPairGenerator.init(new DSAKeyGenerationParameters(a_secureRandom,
			dsaParametersGenerator.generateParameters()));
		AsymmetricCipherKeyPair asymmetricCipherKeyPair = dsaKeyPairGenerator.generateKeyPair();

		try
		{
			keyPair = new DSAKeyPair(
				new MyDSAPrivateKey((DSAPrivateKeyParameters) asymmetricCipherKeyPair.getPrivate()));
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
}
