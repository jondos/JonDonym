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

import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.DERBitString;
import org.bouncycastle.asn1.DEROutputStream;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;

/**
 * A class for creating and verifying PKCS10 Certification requests. They are used
 * to request certification for an X509 certificate from a certificate authority.
 * This class is for internal use only.
 *
 * <PRE>
 *  CertificationRequest ::= SEQUENCE {
 * 	  certificationRequestInfo CertificationRequestInfo,
 *	  signatureAlgorithm SignatureAlgorithmIdentifier,
 *	  signature Signature }
 *
 *  SignatureAlgorithmIdentifier ::= AlgorithmIdentifier
 *  Signature ::= BIT STRING
 *  }
 * </PRE>
 * @author Rolf Wendolsky
 * @see http://www.faqs.org/rfcs/rfc2314.html
 */
final class CertificationRequest extends DERSequence
{
	private CertificationRequestInfo m_certificationRequestInfo;
	private DERBitString m_signature;


	public CertificationRequest(CertificationRequestInfo a_certificationRequestInfo,
								AsymmetricCryptoKeyPair a_keyPair)
	{
		super(createRequest(a_certificationRequestInfo,
							a_keyPair.getPrivate().getSignatureAlgorithm().getIdentifier(),
							new DERBitString(
								 ByteSignature.sign(
									DERtoBytes(a_certificationRequestInfo), a_keyPair))));
		m_certificationRequestInfo = a_certificationRequestInfo;
		m_signature = new DERBitString(
								 ByteSignature.sign(
									DERtoBytes(a_certificationRequestInfo), a_keyPair));
	}

	CertificationRequest(ASN1Sequence a_sequence)
	{
		super(createRequest((DERSequence)a_sequence.getObjectAt(0),
							AlgorithmIdentifier.getInstance(a_sequence.getObjectAt(1)),
							(DERBitString)a_sequence.getObjectAt(2)));

		m_certificationRequestInfo =
			new CertificationRequestInfo((DERSequence)a_sequence.getObjectAt(0));
		m_signature = (DERBitString)a_sequence.getObjectAt(2);
	}

	/**
	 * Returns the public key used in this request.
	 * @return the public key used in this request
	 */
	public IMyPublicKey getPublicKey()
	{
		return m_certificationRequestInfo.getPublicKey();
	}

	/**
	 * Returns the Certification Request Info of this request.
	 * @return the Certification Request Info of this request
	 */
	public CertificationRequestInfo getCertificationRequestInfo()
	{
		return m_certificationRequestInfo;
	}

	/**
	 * Verifies the signature of this request with the public key included.
	 * @return true if the signature of this request is valid; false otherwise
	 */
	public boolean verify()
	{
		return ByteSignature.verify(DERtoBytes(m_certificationRequestInfo), m_signature.getBytes(),
									getPublicKey());
	}

	private static ASN1EncodableVector createRequest(DERSequence a_certificationRequestInfo,
		AlgorithmIdentifier a_algorithmIdentifier, DERBitString a_signature)
	{
		ASN1EncodableVector  certificateRequest = new ASN1EncodableVector();

		certificateRequest.add(a_certificationRequestInfo);
		certificateRequest.add(a_algorithmIdentifier);
		certificateRequest.add(a_signature);

		return certificateRequest;
	}

	private static byte[] DERtoBytes(Object a_object)
	{
		ByteArrayOutputStream DERasBytes = new ByteArrayOutputStream();

		try
		{
			new DEROutputStream(DERasBytes).writeObject(a_object);
		}
		catch (IOException a_e)
		{
			throw new RuntimeException("Could not write DER data to bytes.");
		}

		return DERasBytes.toByteArray();
	}
}
