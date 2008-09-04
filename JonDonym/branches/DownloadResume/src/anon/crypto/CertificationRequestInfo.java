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

import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.ASN1Set;
import org.bouncycastle.asn1.DERSet;
import org.bouncycastle.asn1.DERObject;
import org.bouncycastle.asn1.DERInteger;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.asn1.DERTaggedObject;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.asn1.x509.X509Name;

/**
 * This class is used to hold the information that is needed to create a PKCS10 Certification
 * request. It is for internal use only.
 *
 * <pre>
 * CertificationRequestInfo ::= SEQUENCE {
 *	 version Version,
 *	 subject Name,
 *	 subjectPublicKeyInfo SubjectPublicKeyInfo,
 *	 attributes [0] IMPLICIT Attributes }
 *
 *  Version ::= INTEGER
 *
 *  Attributes ::= SET OF Attribute
 * </pre>
 * @author Rolf Wendolsky
 * @see http://www.faqs.org/rfcs/rfc2314.html
 */
final class CertificationRequestInfo extends DERSequence
{
	private IMyPublicKey m_publicKey;
	private X509DistinguishedName m_subject;
	private X509Extensions m_extensions;

	/**
	 * Creates a new CertificationRequestInfo from a distinguished name (DN), a public key
	 * and X509 V3 extensions.
	 * @param a_subject an X509DistinguishedName (DN)
	 * @param a_publicKey a public key
	 * @param a_extensions X509 V3 extensions (may be null)
	 */
	public CertificationRequestInfo(X509DistinguishedName a_subject, IMyPublicKey a_publicKey,
									X509Extensions a_extensions)
	{
		super(createRequestInfo(new DERInteger(0), a_subject.getX509Name(),
								a_publicKey.getAsSubjectPublicKeyInfo(), a_extensions));
		m_subject = a_subject;
		try
		{
			m_publicKey = AsymmetricCryptoKeyPair.createPublicKey(
						 a_publicKey.getAsSubjectPublicKeyInfo());
		}
		catch (Exception a_e)
		{
			throw new RuntimeException("Could not create public key: " + a_e.getMessage());
		}
		m_extensions = a_extensions;
	}

	/**
	 * Creates a CertificationRequestInfo from a BouncyCastle ASN1Sequence. For internal use only.
	 * @param a_sequence ASN1Sequence
	 */
	CertificationRequestInfo(ASN1Sequence a_sequence)
	{
		super(createRequestInfo(a_sequence));
		try
		{
			m_publicKey = AsymmetricCryptoKeyPair.createPublicKey(
				SubjectPublicKeyInfo.getInstance(a_sequence.getObjectAt(2)));
		}
		catch (Exception a_e)
		{
			throw new RuntimeException("Could not create public key: " + a_e.getMessage());
		}
		m_subject = new X509DistinguishedName(X509Name.getInstance(getObjectAt(1)));
		DERObject temp = ((DERTaggedObject)getObjectAt(3)).getDERObject();
		if (temp instanceof DERSet)
		{
			m_extensions = new X509Extensions( (DERSet) (temp));
		}
		else
		{
			m_extensions = new X509Extensions(new DERSet());
		}
	}

	/**
	 * Returns the public key used in this request.
	 * @return the public key used in this request
	 */
	public IMyPublicKey getPublicKey()
	{
		return m_publicKey;
	}

	/**
	 * Returns the extensions contained in this request info.
	 * @return the extensions contained in this request info
	 */
	public X509Extensions getExtensions()
	{
		return m_extensions;
	}

	/**
	 * Get the X509 name that has been provided for this certification request.
	 * @return the X509 name that has been provided for this certification request
	 */
	public X509DistinguishedName getX509DistinguishedName()
	{
		return m_subject;
	}

	private static ASN1EncodableVector createRequestInfo(DERInteger a_version, X509Name a_subject,
		SubjectPublicKeyInfo a_subjectPublicKeyInfo, X509Extensions a_extensions)
	{
		ASN1EncodableVector  certificateRequestInfo = new ASN1EncodableVector();

		certificateRequestInfo.add(a_version);
		certificateRequestInfo.add(a_subject);
		certificateRequestInfo.add(a_subjectPublicKeyInfo);

		if (a_extensions != null)
		{
			certificateRequestInfo.add(
						 new DERTaggedObject(false, 0, a_extensions.getBCExtensions()));
		}

		return certificateRequestInfo;
	}

	private static ASN1EncodableVector createRequestInfo(ASN1Sequence  a_sequence)
	{
		DERInteger version = (DERInteger)a_sequence.getObjectAt(0);
		X509Name x509Name = X509Name.getInstance(a_sequence.getObjectAt(1));
		SubjectPublicKeyInfo subjectPublicKeyInfo =
			SubjectPublicKeyInfo.getInstance(a_sequence.getObjectAt(2));
		X509Extensions extensions = null;

		if (a_sequence.size() > 3)
		{
			extensions = new X509Extensions((DERSet)ASN1Set.getInstance(
										(DERTaggedObject)a_sequence.getObjectAt(3), false));
		}

		return createRequestInfo(version, x509Name, subjectPublicKeyInfo, extensions);
	}
}

