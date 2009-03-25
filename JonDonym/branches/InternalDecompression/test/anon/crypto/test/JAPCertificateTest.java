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
import java.util.Vector;
import java.util.Calendar;
import java.math.BigInteger;

import anon.crypto.AsymmetricCryptoKeyPair;
import anon.crypto.JAPCertificate;
import anon.crypto.PKCS12;
import anon.crypto.Validity;
import anon.crypto.X509DistinguishedName;
import anon.crypto.X509SubjectKeyIdentifier;
import anon.crypto.X509Extensions;
import anon.crypto.X509SubjectKeyIdentifier;
import anon.crypto.X509IssuerAlternativeName;
import anon.crypto.X509SubjectAlternativeName;
import junitx.framework.extension.XtendedPrivateTestCase;


public class JAPCertificateTest extends XtendedPrivateTestCase
{
	private SecureRandom m_random;

	public JAPCertificateTest(String a_name)
	{
		super(a_name);
		m_random = new SecureRandom();
	}

	/**
	 * Test for the DSA algorithm if extensions can be added and correctly re-read.
	 * @exception Exception if an error occurs
	 */
	public void testExtensionsDSA() throws Exception
	{
		//TODO: test does not validate: either fix test or fix the function
		m_random.setSeed(158943225);
		testExtensions(new DSATestKeyPairGenerator(m_random));
	}

	/**
	 * Test for the RSA algorithm ifextensions can be added and correctly re-read.
	 * @exception Exception if an error occurs
	 */
	public void testExtensionsRSA() throws Exception
	{
		//TODO: test does not validate: either fix test or fix the function
		m_random.setSeed(355582912);
		testExtensions(new RSATestKeyPairGenerator(m_random));
	}

	/**
	 * Test for the Dummy algorithm if extensions can be added and correctly re-read.
	 * @exception Exception if an error occurs
	 */
	public void testExtensionsDummy() throws Exception
	{
		//TODO: test does not validate: either fix test or fix the function
		m_random.setSeed(692981264);
		testExtensions(new DummyTestKeyPairGenerator(m_random));
	}


	/**
	 * Test if extensions can be added and correctly re-read.
	 * @param a_keyPairGenerator a key pair generator
	 * @throws Exception if an error occurs
	 */
	private void testExtensions(AbstractTestKeyPairGenerator a_keyPairGenerator)
		throws Exception
	{
		PKCS12 privateCertificate;
		String ski_one, ski_two;
		X509Extensions extensions;
		Vector vecExtensions;
		Calendar calendar;
		X509SubjectAlternativeName san;
		X509IssuerAlternativeName ian;
		String mail = "my@mail.de";
		String ip = "132.199.134.2";


		// create a private certificate
		privateCertificate = new PKCS12(
				  new X509DistinguishedName("CN=DummyOwner"),
				  a_keyPairGenerator.createKeyPair(), new Validity(new GregorianCalendar(), 1));

		// self-sign certificate with new extensions
		vecExtensions = new Vector();
		vecExtensions.addElement(new X509SubjectKeyIdentifier(privateCertificate.getPublicKey()));
		vecExtensions.addElement(new X509SubjectAlternativeName(
				  mail, X509SubjectAlternativeName.TAG_EMAIL));
		vecExtensions.addElement(new X509IssuerAlternativeName(
				  ip, X509IssuerAlternativeName.TAG_IP));
		extensions = new X509Extensions(vecExtensions);
		calendar = new GregorianCalendar(2002, 2, 2);
		privateCertificate.sign(privateCertificate,
			new Validity(calendar, -1), extensions, new BigInteger("35321"));

		// test if the extensions are set correctly
		extensions = privateCertificate.getExtensions();
		assertEquals(3, extensions.getSize());
		ski_one = new X509SubjectKeyIdentifier(privateCertificate.getPublicKey()).getValue();
		ski_two = ((X509SubjectKeyIdentifier)extensions.getExtension(
				  X509SubjectKeyIdentifier.IDENTIFIER)).getValue();
		assertEquals(ski_one, ski_two);

		san = (X509SubjectAlternativeName)extensions.getExtension(
				  X509SubjectAlternativeName.IDENTIFIER);
		assertEquals(mail,san.getValues().elementAt(0));
		assertEquals(X509SubjectAlternativeName.TAG_EMAIL, san.getTags().elementAt(0));

		ian =(X509IssuerAlternativeName)extensions.getExtension(
				  X509IssuerAlternativeName.IDENTIFIER);
		assertEquals(ip, ian.getValues().elementAt(0));


		// test if the other data is set correctly
		assertEquals(calendar.getTime(),
					 privateCertificate.getX509Certificate().getValidity().getValidFrom());
		assertEquals(new BigInteger("35321"),
				  privateCertificate.getX509Certificate().getSerialNumber());
	}

	/**
	 * Test if certificates can be verified with the DSA algorithm.
	 * @exception Exception if an error occurs
	 */
	public void testVerifyCertificateDSA() throws Exception
	{
		m_random.setSeed(692859929);
		testVerifyCertificate(new DSATestKeyPairGenerator(m_random));
	}

	/**
	 * Test if certificates can be verified with the RSA algorithm.
	 * @exception Exception if an error occurs
	 */
	public void testVerifyCertificateRSA() throws Exception
	{
		m_random.setSeed(47989202);
		testVerifyCertificate(new RSATestKeyPairGenerator(m_random));
	}

	/**
	 * Test if certificates can be verified with the Dummy algorithm.
	 * @exception Exception if an error occurs
	 */
	public void testVerifyCertificateDummy() throws Exception
	{
		m_random.setSeed(38959105);
		testVerifyCertificate(new DummyTestKeyPairGenerator(m_random));
	}

	private void testVerifyCertificate(AbstractTestKeyPairGenerator a_keyPairGenerator)
		throws Exception
	{
		AsymmetricCryptoKeyPair keyPair;
		JAPCertificate certificate = null;
		Vector certificateStore = new Vector();
		Vector pkcs12Certificates = new Vector();
		PKCS12 signingCertificate, signingCertificate2;
		ByteArrayOutputStream testOutput;
		ByteArrayInputStream testInput;

		// generate some certificates
		for (int i = 0; i < 5; i++)
		{
			keyPair = a_keyPairGenerator.createKeyPair();
			signingCertificate = new PKCS12(new X509DistinguishedName("CN=DummyOwner" + i),
											keyPair, new Validity(new GregorianCalendar(), 1));

			// test if we can store and load PKCS12 certificates to/from an output stream
			testOutput = new ByteArrayOutputStream();
			signingCertificate.store(testOutput, new char[0]);
			testInput = new ByteArrayInputStream(testOutput.toByteArray());
			signingCertificate = PKCS12.getInstance(testInput, new char[0]);

			certificate = signingCertificate.getX509Certificate();
			//certificate.setEnabled(true); // X509 certs derived from PKCS12 must be enabled by default!!
			certificateStore.addElement(certificate);
			pkcs12Certificates.addElement(signingCertificate);
		}

		// use the last certificate for testing
		assertTrue(certificateStore.removeElement(certificate));

		// this certificate cannot be verified
		assertFalse(certificate.verify(certificateStore.elements()));

		// sign the certificate with the first pkcs12 certificate
		signingCertificate = (PKCS12)pkcs12Certificates.elementAt(0);
		certificate = certificate.sign(signingCertificate);

		// the certificate can be verified!
		assertTrue(certificate.verify(certificateStore.elements()));

		// put the signing certificate at the end of the list; the certificate can still be verified
		certificateStore.removeElement(signingCertificate.getX509Certificate());
		certificateStore.addElement(signingCertificate.getX509Certificate());
		assertTrue(certificate.verify(certificateStore.elements()));

		// the signing certificate is removed completely; now the certificate cannot be verified!
		certificateStore.removeElement(signingCertificate.getX509Certificate());
		assertFalse(certificate.verify(certificateStore.elements()));

		// sign with two other pkcs12 certificates and verify the signature
		signingCertificate = (PKCS12)pkcs12Certificates.elementAt(3);
		certificate = certificate.sign(signingCertificate);
		signingCertificate2 = (PKCS12)pkcs12Certificates.elementAt(2);
		certificate = certificate.sign(signingCertificate2);
		assertTrue(certificate.verify(certificateStore.elements()));

		// remove the first of the signing certificates (never mind; its signature has been overwritten)
		certificateStore.removeElement(signingCertificate.getX509Certificate());
		assertTrue(certificate.verify(certificateStore.elements()));
	}
}
