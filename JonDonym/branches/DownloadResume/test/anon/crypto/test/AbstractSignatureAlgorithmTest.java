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

import junitx.framework.extension.XtendedPrivateTestCase;

import anon.crypto.IMySignature;
import anon.crypto.IMyPrivateKey;
import anon.crypto.IMyPublicKey;
import anon.util.Util;


/**
 * Tests if the signature algorithm performs all basic operations correctly.
 * @author Rolf Wendolsky
 */
public abstract class AbstractSignatureAlgorithmTest extends XtendedPrivateTestCase
{
	public static final int NUMBER_OF_KEYS = 2;

	private IMySignature m_algorithm;
	private IMyPublicKey[] m_publicKeys;
	private IMyPrivateKey[] m_privateKeys;

	/**
	 * Creates a new test case.
	 * @param a_name name of the test case
	 */
	public AbstractSignatureAlgorithmTest(String a_name)
	{
		super(a_name);

		// create the key arrays
		m_publicKeys = new IMyPublicKey[NUMBER_OF_KEYS];
		m_privateKeys = new IMyPrivateKey[NUMBER_OF_KEYS];
	}

	/**
	 * This method initialises the keys and the signature algorithm.
	 */
	protected abstract void setUp();

	/**
	 * Tests if the correct signature algorithm is returned by the keys.
	 */
	public void testCreateSignatureAlgorithm()
	{
		for (int i = 0; i < NUMBER_OF_KEYS; i++)
		{
			assertTrue("Public key " + i + " has wrong algorithm!", m_algorithm.getClass().isAssignableFrom(
				getPublicKeys()[i].getSignatureAlgorithm().getClass()));

            assertTrue("Private key " + i + " has wrong algorithm!", m_algorithm.getClass().isAssignableFrom(
				getPrivateKeys()[i].getSignatureAlgorithm().getClass()));
		}
	}

	/**
	 * Tests if a message can be signed correctly.
	 * @exception Exception if an error occurs
	 */
	public void testSign() throws Exception
	{
		byte[] message1, message2;
		byte[] signature1, signature2;

		message1 = "My message.".getBytes();
		message2 = "An other message. \nSign it!".getBytes();

		// the algorithm must be initialised before signing is possible
		assertNull(m_algorithm.sign(message1));

		// the algorithm must be initialised for signing, not for verifying
		m_algorithm.initVerify(getPublicKeys()[0]);
		assertNull(m_algorithm.sign(message1));

		// signing should be possible now
		m_algorithm.initSign(getPrivateKeys()[0]);
		signature1 = m_algorithm.sign(message1);
		assertNotNull(signature1);

		// the signatures for different keys and/or messages must be different
		signature2 = m_algorithm.sign(message2);
		assertNotNull(signature2);
		assertTrue(!Util.arraysEqual(signature1, signature2));

		m_algorithm.initSign(getPrivateKeys()[1]);
		signature2 = m_algorithm.sign(message1);
		assertNotNull(signature2);
		assertTrue(!Util.arraysEqual(signature1, signature2));
		signature2 = m_algorithm.sign(message2);
		assertNotNull(signature2);
		assertTrue(!Util.arraysEqual(signature1, signature2));
	}

	/**
	 * Tests if signatures can be created an verified correctly.
	 * @exception Exception if an error occurs
	 */
	public void testSignAndVerify() throws Exception
	{
		byte[] message1, message2, signature1, signature2;

		message1 = "A very important message.".getBytes();
		message2 = "You should know this is my message!\nKeep it in mind.".getBytes();

		// sign the messages with a key
		m_algorithm.initSign(getPrivateKeys()[1]);
		signature1 = m_algorithm.sign(message1);
		signature2 = m_algorithm.sign(message2);
		assertNotNull(signature1);
		assertNotNull(signature2);

		// the algorithm must be initialised before verifying is possible; all verifications will fail
		assertTrue(!m_algorithm.verify(message1, signature1));
		assertTrue(!m_algorithm.verify(message2, signature2));
		assertTrue(!m_algorithm.verify(message1, signature2));
		assertTrue(!m_algorithm.verify(message2, signature1));

		// initialise the algorithm with the wrong key;  all verifications will fail
		m_algorithm.initVerify(getPublicKeys()[0]);
		assertTrue(!m_algorithm.verify(message1, signature1));
		assertTrue(!m_algorithm.verify(message2, signature2));
		assertTrue(!m_algorithm.verify(message1, signature2));
		assertTrue(!m_algorithm.verify(message2, signature1));

		// initialise the algorithm correctly and verify the signatures
		m_algorithm.initVerify(getPublicKeys()[1]);
		assertTrue(m_algorithm.verify(message1, signature1));
		assertTrue(m_algorithm.verify(message2, signature2));
		assertTrue(!m_algorithm.verify(message1, signature2));
		assertTrue(!m_algorithm.verify(message2, signature1));
	}

	protected IMyPublicKey[] getPublicKeys()
	{
		return m_publicKeys;
	}

	protected IMyPrivateKey[] getPrivateKeys()
	{
		return m_privateKeys;
	}

	protected IMySignature getSignatureAlgorithm()
	{
		return m_algorithm;
	}

	protected void setSignatureAlgorithm(IMySignature a_algorithm)
	{
		m_algorithm = a_algorithm;
	}
}
