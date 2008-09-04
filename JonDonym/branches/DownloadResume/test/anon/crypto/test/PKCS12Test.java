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
package anon.crypto.test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.security.SecureRandom;
import java.util.GregorianCalendar;
import anon.crypto.PKCS12;
import anon.crypto.Validity;
import anon.crypto.X509DistinguishedName;
import anon.crypto.X509Extensions;
import junitx.framework.extension.XtendedPrivateTestCase;


public class PKCS12Test extends XtendedPrivateTestCase
{
	private SecureRandom m_random;

	public PKCS12Test(String a_name)
	{
		super(a_name);
		m_random = new SecureRandom();
	}

	/**
	 * Test for the DSA algorithm if certificates can be saved and reloaded correctly.
	 * @exception Exception if an error occurs
	 */
	public void testSaveAndLoadDSA() throws Exception
	{
		m_random.setSeed(484721941);
		testSaveAndLoad(new DSATestKeyPairGenerator(m_random));
	}

	/**
	 * Test for the RSA algorithm if certificates can be saved and reloaded correctly.
	 * @exception Exception if an error occurs
	 */
	public void testSaveAndLoadRSA() throws Exception
	{
		m_random.setSeed(982728351);
		testSaveAndLoad(new RSATestKeyPairGenerator(m_random));
	}

	/**
	 * Test for the Dummy algorithm if certificates can be saved and reloaded correctly.
	 * @exception Exception if an error occurs
	 */
	public void testSaveAndLoadDummy() throws Exception
	{
		m_random.setSeed(578581951);
		testSaveAndLoad(new DummyTestKeyPairGenerator(m_random));
	}


	/**
	 * Test if certificates can be saved and reloaded correctly.
	 * @param a_keyPairGenerator a key pair generator
	 * @throws Exception if an error occurs
	 */
	private void testSaveAndLoad(AbstractTestKeyPairGenerator a_keyPairGenerator)
		throws Exception
	{
		PKCS12 privateCertificate;
		String alias;


		// create a private certificate
		alias = "DummyOwner";

		privateCertificate = new PKCS12(
				  new X509DistinguishedName("CN=" + alias),
				  a_keyPairGenerator.createKeyPair(), new Validity(new GregorianCalendar(), 1));

		assertEquals(alias, privateCertificate.getAlias());
		assertEquals(alias, privateCertificate.getSubject().getCommonName());

		testSaveAndLoad(privateCertificate, (char[])null);
		testSaveAndLoad(privateCertificate, new char[0]);
		testSaveAndLoad(privateCertificate, "My long password!!!".toCharArray());
	}

	/**
	 * Test if certificates can be saved and reloaded correctly with different passwords.
	 * @param a_privateCertificate a private certificate
	 * @param a_password a password
	 * @throws Exception if an error occurs
	 */
	private void testSaveAndLoad(PKCS12 a_privateCertificate, char[] a_password)
	throws Exception
	{
		ByteArrayInputStream in;
		ByteArrayOutputStream out;
		PKCS12 loadedPrivateCertificate;

		// store the certificate
		out = new ByteArrayOutputStream();
		a_privateCertificate.store(out, a_password);

		// reload the certificate
		in = new ByteArrayInputStream(out.toByteArray());
		loadedPrivateCertificate = PKCS12.getInstance(in, a_password);

		assertEquals(a_privateCertificate.getX509Certificate(),
					 loadedPrivateCertificate.getX509Certificate());

		assertEquals(a_privateCertificate.getPublicKey(),
					 loadedPrivateCertificate.getPublicKey());
	}

}
