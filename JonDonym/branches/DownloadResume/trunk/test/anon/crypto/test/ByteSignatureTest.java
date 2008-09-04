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

import java.security.SecureRandom;

import anon.crypto.AsymmetricCryptoKeyPair;
import anon.crypto.ByteSignature;
import anon.util.Util;
import junitx.framework.extension.XtendedPrivateTestCase;

/**
 * These are the tests for the ByteSignature class.
 * @author Rolf Wendolsky
 */
public class ByteSignatureTest extends XtendedPrivateTestCase
{
	private SecureRandom m_random;

	/**
	 * Creates a new test case.
	 * @param a_name the name of the test case
	 */
	public ByteSignatureTest(String a_name)
	{
		super(a_name);
	}

	/**
	 * Initialises the secure random generator.
	 */
	public void setUp()
	{
		m_random = new SecureRandom();
	}

	/**
	 * Tests if the sign and verify methods work correctly with the DSA algorithm.
	 * @throws Exception if an error occurs
	 */
	public void testSignAndVerifyDSA() throws Exception
	{
		m_random.setSeed(75982267);
		testSignAndVerify(new DSATestKeyPairGenerator(m_random));
	}

	/**
	 * Tests if the sign and verify methods work correctly with the RSA algorithm.
	 * @throws Exception if an error occurs
	 */
	public void testSignAndVerifyRSA() throws Exception
	{
		m_random.setSeed(849271881);
		testSignAndVerify(new RSATestKeyPairGenerator(m_random));
	}

	/**
	 * Tests if the sign and verify methods work correctly with the Dummy algorithm.
	 * @throws Exception if an error occurs
	 */
	public void testSignAndVerifyDummy() throws Exception
	{
		m_random.setSeed(48599858);
		testSignAndVerify(new DummyTestKeyPairGenerator(m_random));
	}


	/**
	 * Tests if the sign and verify methods work correctly with the specified key pairs.
	 * @param a_keyPairOne AsymmetricKeyPair
	 * @param a_keyPairTwo AsymmetricKeyPair
	 * @throws Exception if an error occurs
	 */
	private void testSignAndVerify(AbstractTestKeyPairGenerator a_keyPairGenerator)
		throws Exception
	{
		byte[] messageOne, messageTwo;

		// initialise messages
		messageOne = "I will by this item for 3500 credits.".getBytes();
		messageTwo = "O.K. lad. I`ll give it to you.".getBytes();

		testSignAndVerify(a_keyPairGenerator, messageOne, messageTwo);
		testSignAndVerify(a_keyPairGenerator, messageTwo, messageOne);
	}

	/**
	 * Tests if the sign and verify methods work correctly with the specified key pairs and messages.
	 * @param a_keyPairOne AsymmetricKeyPair
	 * @param a_keyPairTwo AsymmetricKeyPair
	 * @param a_messageOne a message to be signed and verified
	 * @param a_messageTwo an other message to be signed and verified
	 * @throws Exception if an error occurs
	 */
	private void testSignAndVerify(AbstractTestKeyPairGenerator a_keyPairGenerator,
								   byte[] a_messageOne, byte[] a_messageTwo)
		throws Exception
	{
		byte[] signatureOne, signatureTwo;
		AsymmetricCryptoKeyPair keyPairOne = a_keyPairGenerator.createKeyPair();
		AsymmetricCryptoKeyPair keyPairTwo = a_keyPairGenerator.createKeyPair();

		// sign message one with different keys
		signatureOne = ByteSignature.sign(a_messageOne, keyPairOne);
		signatureTwo = ByteSignature.sign(a_messageOne, keyPairTwo);
		assertNotNull(signatureOne);
		assertNotNull(signatureTwo);
		assertTrue(!Util.arraysEqual(signatureOne, signatureTwo));

		// verify the first message with the first signature and the first key pair
		assertTrue(ByteSignature.verify(a_messageOne, signatureOne, keyPairOne));

		// verify the first message with the first signature and the second key pair (failed)
		assertFalse(ByteSignature.verify(a_messageOne, signatureOne, keyPairTwo));

		// verify the first message with the second signature and the first key pair (failed)
		assertFalse(ByteSignature.verify(a_messageOne, signatureTwo, keyPairOne));

		// verify the first message with the second signature and the second key pair
		assertTrue(ByteSignature.verify(a_messageOne, signatureTwo, keyPairTwo));

		// verify the second message with all signatues and key pairs (failed)
		assertFalse(ByteSignature.verify(a_messageTwo, signatureTwo, keyPairTwo));
		assertFalse(ByteSignature.verify(a_messageTwo, signatureOne, keyPairTwo));
		assertFalse(ByteSignature.verify(a_messageTwo, signatureTwo, keyPairOne));
		assertFalse(ByteSignature.verify(a_messageTwo, signatureOne, keyPairOne));
	}
}
