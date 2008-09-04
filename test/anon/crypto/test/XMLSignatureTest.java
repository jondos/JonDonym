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
import java.util.GregorianCalendar;
import java.util.Vector;

import org.w3c.dom.Document;
import anon.crypto.JAPCertificate;
import anon.crypto.PKCS12;
import anon.crypto.Validity;
import anon.crypto.XMLSignature;
import anon.crypto.X509DistinguishedName;
import anon.util.XMLUtil;
import anon.util.test.DummyXMLEncodable;
import junitx.framework.extension.XtendedPrivateTestCase;

/**
 * These are the tests for the XMLSignature class.
 * @todo implement pessimistic tests (exceptions and false inputs)
 * @author Rolf Wendolsky
 */
public class XMLSignatureTest extends XtendedPrivateTestCase
{
	private SecureRandom m_random;

	public XMLSignatureTest(String a_name)
	{
		super(a_name);
		m_random = new SecureRandom();
	}

	public void testSignAndVerifyDSA() throws Exception
	{
		m_random.setSeed(89603428);
		testSignAndVerify(new DSATestKeyPairGenerator(m_random));
	}


	/**
	 * Tests if XML nodes can successfully be signed and verified with the RSA algorithm.
	 * @throws Exception if an error occurs
	 * @todo Signing and verifying with RSA does not work yet; {@link anon.crypto.MyRSASignature}
	 * encoding and decoding methods!
	 */
	public void _testSignAndVerifyRSA() throws Exception
	{
		m_random.setSeed(85922773);
		testSignAndVerify(new RSATestKeyPairGenerator(m_random));
	}

	/**
	 * Tests if XML nodes can successfully be signed and verified with the Dummy algorithm.
	 * @throws Exception if an error occurs
	 */
	public void testSignAndVerifyDummy() throws Exception
	{
		m_random.setSeed(47257895);
		testSignAndVerify(new DummyTestKeyPairGenerator(m_random));
	}

	/**
	 * Test if DSA certificates can be added and removed correctly from/to the signature.
	 * @exception Exception if an error occurs
	 */
	public void testManageCertificatesDSA() throws Exception
	{
		m_random.setSeed(9590722);
		testManageCertificates(new DSATestKeyPairGenerator(m_random));
	}

	/**
	 * Test if RSA certificates can be added and removed correctly from/to the signature.
	 * @exception Exception if an error occurs
	 * @todo Signing and verifying with RSA does not work yet; {@link anon.crypto.MyRSASignature}
	 * encoding and decoding methods!
	 */
	public void _testManageCertificatesRSA() throws Exception
	{
		m_random.setSeed(726619);
		testManageCertificates(new RSATestKeyPairGenerator(m_random));
	}

	/**
	 * Test if Dummy certificates can be added and removed correctly from/to the signature.
	 * @exception Exception if an error occurs
	 */
	public void testManageCertificatesDummy() throws Exception
	{
		m_random.setSeed(1298302);
		testManageCertificates(new DummyTestKeyPairGenerator(m_random));
	}

	/**
	 * Test if DSA certificates can be added and removed correctly from/to the signature.
	 * @exception Exception if an error occurs
	 */
	public void testAppendRemoveDSA() throws Exception
	{
		m_random.setSeed(7522939);
		testAppendRemove(new DSATestKeyPairGenerator(m_random));
	}

	/**
	 * Test if RSA certificates can be added and removed correctly from/to the signature.
	 * @exception Exception if an error occurs
	 * @todo Signing and verifying with RSA does not work yet; {@link anon.crypto.MyRSASignature}
	 * encoding and decoding methods!
	 */
	public void _testAppendRemoveRSA() throws Exception
	{
		m_random.setSeed(8928933);
		testAppendRemove(new RSATestKeyPairGenerator(m_random));
	}

	/**
	 * Test if Dummy certificates can be added and removed correctly from/to the signature.
	 * @exception Exception if an error occurs
	 */
	public void testAppendRemoveDummy() throws Exception
	{
		m_random.setSeed(90259932);
		testAppendRemove(new DummyTestKeyPairGenerator(m_random));
	}

	/**
	 * Tests if signatures can be successfully verified by using collected certificates and if
	 * certificates are collected successfully. (for DSA algorithm)
	 * @throws Exception if an error occurs
	 */
	public void _testVerifyWithCollectedCertificatesDSA() throws Exception
	{
		m_random.setSeed(5729391);
		testVerifyWithCollectedCertificates(new DSATestKeyPairGenerator(m_random));
	}

	/**
	 * Tests if signatures can be successfully verified by using collected certificates and if
	 * certificates are collected successfully. (for RSA algorithm)
	 * @throws Exception if an error occurs
	 * @todo Signing and verifying with RSA does not work yet; {@link anon.crypto.MyRSASignature}
	 */
	public void _testVerifyWithCollectedCertificatesRSA() throws Exception
	{
		m_random.setSeed(8598228);
		testVerifyWithCollectedCertificates(new RSATestKeyPairGenerator(m_random));
	}

	/**
	 * Tests if signatures can be successfully verified by using collected certificates and if
	 * certificates are collected successfully. (for Dummy algorithm)
	 * @throws Exception if an error occurs
	 */
	public void _testVerifyWithCollectedCertificatesDummy() throws Exception
	{
		m_random.setSeed(367140);
		testVerifyWithCollectedCertificates(new DummyTestKeyPairGenerator(m_random));
	}


	/**
	 * Test if certificates can be added and removed correctly from/to the signature.
	 * @param a_keyGen AbstractTestKeyPairGenerator
	 * @exception Exception if an error occurs
	 */
	private	void testManageCertificates(AbstractTestKeyPairGenerator a_keyGen)
		throws Exception
	{
		Document doc;
		PKCS12 pkcs12Certificate;
		PKCS12 otherPkcs12Certificate;
		XMLSignature signature;
		JAPCertificate x509Certificate;
		Vector certificates;


		doc = XMLUtil.toXMLDocument(new DummyXMLEncodable());

		// create a private certificate
		pkcs12Certificate = new PKCS12(
				  new X509DistinguishedName("CN=private"),
				  a_keyGen.createKeyPair(), new Validity(new GregorianCalendar(), 0));

		// create some X509 certificates
		certificates = new Vector();
		for (int i = 0; i < 5; i++)
		{
			certificates.addElement(new PKCS12(new X509DistinguishedName("CN=Owner:" + i),
											   a_keyGen.createKeyPair(),
											   new Validity(new GregorianCalendar(), 0)
											   ).getX509Certificate());
		}

		// sign the document with a private key (no certificate is appended)
		signature = XMLSignature.sign(doc, pkcs12Certificate.getPrivateKey());
		assertEquals(0, signature.countCertificates());

		// sign the document with a PKCS12 cert (the corresponding X509 certificate is appended)
		signature = XMLSignature.sign(doc, pkcs12Certificate);
		assertEquals(1, signature.countCertificates());
		assertTrue(signature.containsCertificate(pkcs12Certificate.getX509Certificate()));
		assertEquals(pkcs12Certificate.getX509Certificate(),
					 (JAPCertificate)signature.getCertificates().elementAt(0));

		// append other certificates; these certificates are not suitable to verify the signature
		signature.addCertificate((JAPCertificate)certificates.elementAt(0));
		assertEquals(1, signature.countCertificates());

		// verify the signature and test if the certificate is contained in it
		assertNotNull(XMLSignature.verify(doc, pkcs12Certificate.getX509Certificate()));
		signature = XMLSignature.verify(doc, pkcs12Certificate.getX509Certificate().getPublicKey());
		assertEquals(1, signature.countCertificates());
		assertEquals(pkcs12Certificate.getX509Certificate().getId(),
					 ((JAPCertificate)signature.getCertificates().elementAt(0)).getId());
		assertTrue(signature.containsCertificate(pkcs12Certificate.getX509Certificate()));



		/*
		 * Create a new private certificate; sign the document with this certificate (but do not
		 * add the certificate) and then sign this certificate with the previous private
		 * certificate.
		 */
		otherPkcs12Certificate = new PKCS12(new X509DistinguishedName(
				  "CN=private2"), a_keyGen.createKeyPair(),
											new Validity(new GregorianCalendar(), 0));
		otherPkcs12Certificate.sign(pkcs12Certificate);
		XMLSignature.removeSignatureFrom(doc);
		signature = XMLSignature.sign(doc, otherPkcs12Certificate.getPrivateKey());
		assertEquals(0, signature.countCertificates());

		// verifying succeeds for the signing certificate, but not for the other one
		assertNotNull(XMLSignature.verify(doc, otherPkcs12Certificate.getX509Certificate()));
		assertNull(XMLSignature.verify(doc, pkcs12Certificate.getX509Certificate()));

		// now the non-signing certificate is added; nothing changes
		assertFalse(signature.addCertificate(pkcs12Certificate.getX509Certificate()));
		assertFalse(signature.addCertificate(pkcs12Certificate.getX509Certificate()));
		assertEquals(0, signature.countCertificates());
		assertNotNull(XMLSignature.verify(doc, otherPkcs12Certificate.getX509Certificate()));
		assertNull(XMLSignature.verify(doc, pkcs12Certificate.getX509Certificate()));

		// now add the signing certificate; verification can be done by both certificates now!
		signature.addCertificate(otherPkcs12Certificate.getX509Certificate());
		assertEquals(1, signature.countCertificates());
		assertNotNull(XMLSignature.verify(doc, otherPkcs12Certificate.getX509Certificate()));
		assertNotNull(XMLSignature.verify(doc, pkcs12Certificate.getX509Certificate()));


		// create an other private certificate; verifying fails for this certificate
		pkcs12Certificate = new PKCS12(new X509DistinguishedName(
				  "CN=private3"), a_keyGen.createKeyPair(),
									   new Validity(new GregorianCalendar(), 0));
		assertNull(XMLSignature.verify(doc, pkcs12Certificate.getX509Certificate()));

		// use this certificate to sign the signing certificate
		x509Certificate = otherPkcs12Certificate.getX509Certificate().sign(pkcs12Certificate);

		// add this certificate to the signature
		assertEquals(1, signature.countCertificates());
		signature.addCertificate(x509Certificate);
		assertEquals(2, signature.countCertificates());

		// verify the signature (indirectly) with the new private certificate
		assertNotNull(XMLSignature.verify(doc, pkcs12Certificate.getX509Certificate()));
		assertNotNull(XMLSignature.verify(doc, pkcs12Certificate.getX509Certificate()));


		// ***  write this structure to file ***
		writeXMLOutputToFile(signature);
		writeXMLOutputToFile(doc, 1);


		// test clear certificates
		signature.clearCertificates();
		assertEquals(0, signature.countCertificates());
		assertNull(XMLSignature.verify(doc, pkcs12Certificate.getX509Certificate()));

		// add some certificates and remove one
		signature.addCertificate(otherPkcs12Certificate.getX509Certificate());
		signature.addCertificate(x509Certificate);
		signature.removeCertificate(x509Certificate);
		assertEquals(1, signature.countCertificates());
		signature.removeCertificate(x509Certificate);
		assertEquals(1, signature.countCertificates());
		assertTrue(signature.containsCertificate(otherPkcs12Certificate.getX509Certificate()));
	}

	/**
	 * Tests if signatures can be successfully verified by using collected certificates and if
	 * certificates are collected successfully.
	 * @param a_keyGen AbstractTestKeyPairGenerator
	 * @throws Exception if an error occurs
	 */
	private void testVerifyWithCollectedCertificates(AbstractTestKeyPairGenerator a_keyGen)
		throws Exception
	{
		PKCS12 trustedCertificateGenerator;
		PKCS12 signerOne;
		PKCS12 signerTwo;
		Document doc;
		SecureRandom random;
		XMLSignature signature;
		Vector collectedCertificates;
		Vector trustedCertificates;

		random = new SecureRandom();
		random.setSeed(6839293);

		trustedCertificateGenerator = new PKCS12(new X509DistinguishedName(
				  "CN=trustedCertGen"), a_keyGen.createKeyPair(),
												 new Validity(new GregorianCalendar(), 0));

		signerOne = new PKCS12(new X509DistinguishedName("CN=SignerOne"),
							   a_keyGen.createKeyPair(), new Validity(new GregorianCalendar(), 0));
		signerOne.sign(trustedCertificateGenerator);


		signerTwo = new PKCS12(new X509DistinguishedName("CN=SignerTwo"),
							   a_keyGen.createKeyPair(), new Validity(new GregorianCalendar(), 0));
		signerTwo.sign(trustedCertificateGenerator);


		// initialise the certificate stores
		trustedCertificates = new Vector();
		trustedCertificates.addElement(trustedCertificateGenerator.getX509Certificate());
		collectedCertificates = new Vector();

		/*
		 * Signer one and signer two sign XML documents and append their certificate
		 * that has been signed by the trusted certificate generator. These certificates
		 * are trusted and will be appended to the collected certificates.
		 */
		doc = XMLUtil.toXMLDocument(new DummyXMLEncodable(random));
		XMLSignature.sign(doc, signerOne);

		assertTrue(XMLSignature.getVerified(doc, trustedCertificates, collectedCertificates, false).isVerified());
		assertEquals(1, collectedCertificates.size());

		// signer two does not append any certificate; validation is not possible
		doc = XMLUtil.toXMLDocument(new DummyXMLEncodable(random));
		XMLSignature.sign(doc, signerTwo.getPrivateKey());
		assertFalse(XMLSignature.getVerified(doc, trustedCertificates, collectedCertificates, false).isVerified());

		// now signer two appends his certificate, too
		doc = XMLUtil.toXMLDocument(new DummyXMLEncodable(random));
		XMLSignature.sign(doc, signerTwo);
		assertTrue(XMLSignature.getVerified(doc, trustedCertificates, collectedCertificates, false).isVerified());
		assertEquals(2, collectedCertificates.size());


		/*
		 * Now the two signers do not append their certificates any more. The signatures
		 * can still be verified, as the certificates have been collected before.
		 */
		doc = XMLUtil.toXMLDocument(new DummyXMLEncodable(random));
		signature = XMLSignature.sign(doc, signerOne.getPrivateKey());
		assertEquals(0, signature.countCertificates());
		assertTrue(XMLSignature.getVerified(doc, trustedCertificates, collectedCertificates, false).isVerified());

		doc = XMLUtil.toXMLDocument(new DummyXMLEncodable(random));
		signature = XMLSignature.sign(doc, signerTwo.getPrivateKey());
		assertEquals(0, signature.countCertificates());
		assertTrue(XMLSignature.getVerified(doc, trustedCertificates, collectedCertificates, false).isVerified());
	}

	/**
	 * Tests if signatures can be successfully added to and removed from XML documents.
	 * @param a_keyGen AbstractTestKeyPairGenerator
	 * @exception Exception if an error occurs
	 */
	private void testAppendRemove(AbstractTestKeyPairGenerator a_keyGen)
		throws Exception
	{
		Document doc;
		PKCS12 pkcs12Certificate;
		DummyXMLEncodable dummy;
		XMLSignature signature;

		pkcs12Certificate = new PKCS12(new X509DistinguishedName(
				  "CN=CertOwner"), a_keyGen.createKeyPair(),
									   new Validity(new GregorianCalendar(), 0));


		doc = XMLUtil.toXMLDocument(new DummyXMLEncodable());

		// try to remove a signature from a signed document
		signature = XMLSignature.sign(doc, pkcs12Certificate);
		XMLSignature.verify(doc, pkcs12Certificate.getX509Certificate());
		assertTrue(XMLSignature.removeSignatureFrom(doc));
		assertFalse(XMLSignature.removeSignatureFrom(doc));
		assertNull(XMLSignature.verify(doc, pkcs12Certificate.getX509Certificate()));

		// now append this signature again
		assertTrue(signature.appendSignatureTo(doc));
		assertNotNull(XMLSignature.verify(doc, pkcs12Certificate.getX509Certificate()));

		// remove the signature, modify the document, and try to append the signature (fails)
		assertTrue(XMLSignature.removeSignatureFrom(doc));
		XMLUtil.setAttribute(doc.getDocumentElement(), "modified", true);
		assertFalse(signature.appendSignatureTo(doc));

		// recreate the document and append the signature
		doc = XMLUtil.toXMLDocument(new DummyXMLEncodable());
		assertTrue(signature.appendSignatureTo(doc));
		assertNotNull(XMLSignature.verify(doc, pkcs12Certificate.getX509Certificate()));

		// recreate a modified version of the document and append the signature (fails)
		dummy = new DummyXMLEncodable();
		dummy.setValueLong(dummy.getValueLong() + 12);
		doc = XMLUtil.toXMLDocument(dummy);
		assertFalse(signature.appendSignatureTo(doc));
		assertNull(XMLSignature.verify(doc, pkcs12Certificate.getX509Certificate()));

		dummy = new DummyXMLEncodable();
		dummy.setID(dummy.getID() + "gr");
		doc = XMLUtil.toXMLDocument(dummy);
		assertFalse(signature.appendSignatureTo(doc));
		assertNull(XMLSignature.verify(doc, pkcs12Certificate.getX509Certificate()));
	}

	/**
	 * Tests if XML nodes can successfully be signed and verified with the same certificates
	 * that signed them.
	 * @param a_keyPairGenerator AbstractTestKeyPairGenerator
	 * @throws Exception
	 */
	private void testSignAndVerify(AbstractTestKeyPairGenerator a_keyPairGenerator) throws Exception
	{
		XMLSignature signature = null;
		PKCS12 pkcs12Certificate;
		JAPCertificate x509certificate;
		Document doc = null;

		// test with several keys (respective certificates)
		for (int i = 0; i < 3; i++)
		{
			doc = XMLUtil.toXMLDocument(new DummyXMLEncodable());

			// create a private certificate
			pkcs12Certificate = new PKCS12(new X509DistinguishedName("CN=ImportantOwner:" + i),
										   a_keyPairGenerator.createKeyPair(),
										   new Validity(new GregorianCalendar(), 0));

			// remove any previous signature
			XMLSignature.removeSignatureFrom(doc);
			assertNull(i + "", XMLSignature.getUnverified(doc));

			// sign and verify
			signature = XMLSignature.sign(doc, pkcs12Certificate);
			assertNotNull(i + "", signature);
			assertNotNull(i + "", XMLSignature.getUnverified(doc));
			signature = XMLSignature.verify(doc, pkcs12Certificate.getX509Certificate());
			assertNotNull(i + "", signature);

			// modify the document and try to verify it (fails)
			XMLUtil.setAttribute(doc.getDocumentElement(), "modified", true);
			assertNull(i + "", XMLSignature.verify(doc, pkcs12Certificate.getX509Certificate()));

			// remove any previous signature and verify (fails)
			XMLSignature.removeSignatureFrom(doc);
			assertNull(i + "", XMLSignature.getUnverified(doc));
			assertNull(i + "", XMLSignature.verify(doc, pkcs12Certificate.getX509Certificate()));

			// sign twice and verify
			signature = XMLSignature.sign(doc, pkcs12Certificate);
			assertNotNull(i + "", signature);
			assertNotNull(i + "", XMLSignature.getUnverified(doc));
			signature = XMLSignature.sign(doc, pkcs12Certificate); // sign twice
			assertNotNull(i + "", signature);
			assertNotNull(XMLSignature.getUnverified(doc));
			signature = XMLSignature.verify(doc, pkcs12Certificate.getX509Certificate());
			assertNotNull(i + "", signature);

			// sign with private key and verify with public key
			XMLSignature.removeSignatureFrom(doc);
			signature = XMLSignature.sign(doc, pkcs12Certificate.getPrivateKey());
			assertNotNull(i + "", XMLSignature.getUnverified(doc));
			signature =
				XMLSignature.verify(doc, pkcs12Certificate.getX509Certificate().getPublicKey());


			// take other random keys and test the signature; it must fail
			assertNull(i + "",
					   XMLSignature.verify(doc, a_keyPairGenerator.createKeyPair().getPublic()));
			x509certificate = new PKCS12(new X509DistinguishedName("CN=NewOwner:1"),
				a_keyPairGenerator.createKeyPair(),
				new Validity(new GregorianCalendar(), 0)).getX509Certificate();
			assertNull(i + "", XMLSignature.verify(doc, x509certificate));
			x509certificate = new PKCS12(new X509DistinguishedName("CN=NewOwner:2"),
				a_keyPairGenerator.createKeyPair(),
				new Validity(new GregorianCalendar(), 0)).getX509Certificate();
			assertNull(i + "", XMLSignature.verify(doc, x509certificate));
		}
	}

}
