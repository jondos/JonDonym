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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.DEROutputStream;
import org.bouncycastle.crypto.digests.MD5Digest;
import org.bouncycastle.crypto.digests.SHA1Digest;
import anon.util.Base64;
import anon.util.ResourceLoader;


/**
 * A class for creating and verifying PKCS10 Certification requests. They are used
 * to request certification for an X509 certificate from a certificate authority.
 *
 * <PRE>
 *  CertificationRequest ::= SEQUENCE {
 * 	  certificationRequestInfo CertificationRequestInfo,
 *	  signatureAlgorithm SignatureAlgorithmIdentifier,
 *	  signature Signature }
 *
 *  SignatureAlgorithmIdentifier ::= AlgorithmIdentifier
 *  Signature ::= BIT STRING
 *
 *
 *  CertificationRequestInfo ::= SEQUENCE {
 *	 version Version,
 *	 subject Name,
 *	 subjectPublicKeyInfo SubjectPublicKeyInfo,
 *	 attributes [0] IMPLICIT Attributes }
 *
 *  Version ::= INTEGER
 *  Attributes ::= SET OF Attribute
 *
 *
 *  Attribute ::= SEQUENCE {
 *    type    ATTRIBUTE.&id,
 *    values  SET SIZE(1..MAX) OF ATTRIBUTE.&Type
 *  }
 * </PRE>
 *
 * @author Rolf Wendolsky
 * @see http://www.faqs.org/rfcs/rfc2314.html
 */
public final class PKCS10CertificationRequest
{
	/** The file extension that should be used if a request is written to a file. */
	public static final String FILE_EXTENSION = ".p10";

	private static final String BASE64_TAG = "CERTIFICATE REQUEST";
	private static final String BASE64_ALTERNATIVE_TAG = "NEW CERTIFICATE REQUEST";

	private CertificationRequest m_certificationRequest;
	private String m_sha1Fingerprint;
	private String m_md5Fingerprint;

	/**
	 * Creates a PKCS10 Certification Request from input stream.
	 * @param a_inputStream an input stream representing a PKCS10 Certification Request
	 * @throws IOException if an I/O error occurs
	 */
	public PKCS10CertificationRequest(InputStream a_inputStream) throws IOException
	{
		this(ResourceLoader.getStreamAsBytes(a_inputStream));
	}

	/**
	 * Creates a PKCS10 Certification Request from an array of bytes.
	 * @param a_bytes an array of bytes representing a PKCS10 Certification Request
	 */
	public PKCS10CertificationRequest(byte[] a_bytes)
	{
		ASN1Sequence certificateRequest = JAPCertificate.toASN1Sequence(a_bytes, null);

		m_certificationRequest = new CertificationRequest(certificateRequest);
		createFingerprints();
	}

	/**
	 * Creates a new PKCS10 Certification Request.
	 * @param a_subject an X509 distinguished name
	 * @param a_keyPair a cryptographic key pair used to sign the request
	 * @param a_extensions a set of attributes for this request
	 */
	public PKCS10CertificationRequest(X509DistinguishedName a_subject,
									  AsymmetricCryptoKeyPair a_keyPair,
									  X509Extensions a_extensions)
	{
		CertificationRequestInfo certificationRequestInfo =
			new CertificationRequestInfo(a_subject, a_keyPair.getPublic(), a_extensions);

		m_certificationRequest = new CertificationRequest(certificationRequestInfo, a_keyPair);
		createFingerprints();
	}

	/**
	 * Creates a new PKCS10 Certification Request from a private certificate.
	 * @param a_privateCertificate a private certificate
	 */
	public PKCS10CertificationRequest(PKCS12 a_privateCertificate)
	{
		this(a_privateCertificate.getSubject(), a_privateCertificate.getKeyPair(),
			 a_privateCertificate.getExtensions());
	}

	/**
	 * Writes the request to an output stream. The data is DER encoded and may be additionally
	 * Bas64 encoded. The Base64 encoding is needed to be compatible the OpenSSL PEM format.
	 * @param a_out OutputStream
	 * @param a_Base64Encoded boolean
	 * @throws IOException
	 */
	public void toOutputStream(OutputStream a_out, boolean a_Base64Encoded) throws IOException
	{
		a_out.write(toByteArray(a_Base64Encoded));
	}

	/**
	 * Converts the request to a DER encoded byte array. The data may additionally be Base64
	 * encoded. The Base64 encoding is needed to be compatible the OpenSSL PEM format.
	 * @param a_Base64Encoded if the data should additionally be Base64 encoded.
	 * @return the request as a byte array
	 */
	public byte[] toByteArray(boolean a_Base64Encoded)
	{
		if (a_Base64Encoded)
		{
			ByteArrayOutputStream out = new ByteArrayOutputStream();

			try
			{
				out.write(Base64.createBeginTag(BASE64_TAG).getBytes());
				out.write(Base64.encode(getEncoded(), true).getBytes());
				out.write(Base64.createEndTag(BASE64_TAG).getBytes());
			}
			catch (IOException a_e)
			{
				throw new RuntimeException("Could not write encoded bytes to byte array: " +
										   a_e.getMessage());
			}

			return out.toByteArray();
		}
		else
		{
			return getEncoded();
		}
	}

	/**
	 * Verifies the signature of this request with the public key included.
	 * @return true if the signature of this request is valid; false otherwise
	 */
	public boolean verify()
	{
		return m_certificationRequest.verify();
	}

	/**
	 * Creates a new X509 certificate from this certification request. This method is usually used
	 * by a cerificate authority.
	 * @param a_signerCertificate PKCS12
	 * @param a_validity the validity of the certificate
	 * @param a_extensions some X509 extensions (may be null); you may take the extensions from
	 * this request after checking plausibility
	 * @param a_serialNumber the serial number for this certificate (may be null); usually a
	 * new serial number is created for each certificate created by the CA
	 * @return a new X509 certificate
	 */
	public JAPCertificate createX509Certificate(PKCS12 a_signerCertificate,
												Validity a_validity, X509Extensions a_extensions,
												BigInteger a_serialNumber)
	{
		return JAPCertificate.getInstance(getX509DistinguishedName(),
										  a_signerCertificate.getSubject(),
										  a_signerCertificate.getPrivateKey(),
										  a_signerCertificate.getPublicKey(),
										  a_validity, a_extensions, a_serialNumber);
	}

	/**
	 * Returns the public key used in this request.
	 * @return the public key used in this request
	 */
	public IMyPublicKey getPublicKey()
	{
		return m_certificationRequest.getPublicKey();
	}

	/**
	 * Gets a human readable SHA1 fingerprint for this request. This fingerprint may be
	 * compared by a user with an other request's fingerprint to proof their equality.
	 * @return a human readable SHA1 fingerprint for this request
	 */
	public String getSHA1Fingerprint()
	{
		return m_sha1Fingerprint;
	}

	/**
	 * Gets a human readable MD5 fingerprint for this request. This fingerprint may be
	 * compared by a user with an other request's fingerprint to proof their equality.
	 * @return a human readable MD5 fingerprint for this request
	 */
	public String getMD5Fingerprint()
	{
		return m_md5Fingerprint;
	}

	/**
	 * Get the X509 name that has been provided for this certification request.
	 * @return the X509 name that has been provided for this certification request
	 */
	public X509DistinguishedName getX509DistinguishedName()
	{
		return m_certificationRequest.getCertificationRequestInfo().getX509DistinguishedName();
	}

	/**
	 * Returns the extensions contained in this request.
	 * @return the extensions contained in this request
	 */
	public X509Extensions getExtensions()
	{
		return m_certificationRequest.getCertificationRequestInfo().getExtensions();
	}

	private void createFingerprints()
	{
		byte[] data = toByteArray(false);
		m_sha1Fingerprint = JAPCertificate.createFingerprint(new SHA1Digest(), data);
		m_md5Fingerprint = JAPCertificate.createFingerprint(new MD5Digest(), data);
	}

	/**
	 * Returns a DER encoded byte array representing this request.
	 * @return a DER encoded byte array representing this request
	 */
	private byte[] getEncoded()
	{
		ByteArrayOutputStream   bytesOut = new ByteArrayOutputStream();
		DEROutputStream derOut = new DEROutputStream(bytesOut);

		try
		{
			derOut.writeObject(m_certificationRequest);
		}
		catch (IOException e)
		{
			throw new RuntimeException(e.toString());
		}

		return bytesOut.toByteArray();
	}
}
