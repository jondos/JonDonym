/*
 Copyright (c) 2000 - 2008, The JAP-Team
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
import java.math.BigInteger;
import java.util.Date;
import java.util.Enumeration;
import java.util.Vector;

import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;

import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.DERBitString;
import org.bouncycastle.asn1.DEREncodableVector;
import org.bouncycastle.asn1.DERInteger;
import org.bouncycastle.asn1.DEROutputStream;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.asn1.DERUTCTime;
import org.bouncycastle.asn1.x509.CertificateList;
import org.bouncycastle.asn1.x509.TBSCertList;
import org.bouncycastle.asn1.x509.Time;
import org.bouncycastle.asn1.x509.V2TBSCertListGenerator;
import org.bouncycastle.asn1.x509.X509Name;
import org.bouncycastle.crypto.digests.GeneralDigest;
import org.bouncycastle.crypto.digests.SHA1Digest;

public class CertificateRevocationList
{	
	private CertificateList m_crl;
	
	private static Class[] CRL_EXTENSIONS = 
		{X509AuthorityKeyIdentifier.class, X509IssuerAlternativeName.class,
			X509IssuingDistributionPoint.class};
	
	private static Class[] CRL_ENTRY_EXTENSIONS = 
		{X509CertificateIssuer.class};
	
	public CertificateRevocationList(
			PKCS12 a_issuerCertificate,
			Vector a_certList,
			X509Extensions a_extensions)
	{
		m_crl = new CRLGenerator(a_certList, a_extensions).sign(a_issuerCertificate);
	}
	
	public CertificateList getCRL()
	{
		return m_crl;
	}
		
	public static BigInteger createPseudoSerial(byte[] a_rawCert)
	{
		GeneralDigest digest = new SHA1Digest();
		byte[] value = new byte[digest.getDigestSize()];
		
		digest.update(a_rawCert, 0, a_rawCert.length);
		digest.doFinal(value, 0);
		
		return new BigInteger(value).abs();		
	}
	
	private static final class CRLGenerator extends V2TBSCertListGenerator
	{
		public CRLGenerator(
				Vector a_certList,
				X509Extensions a_extensions)
		{
			setThisUpdate(new DERUTCTime(new Date()));
			setExtensions(a_extensions.getBCX509Extensions());
			if(a_certList != null)
			{
				Enumeration certificates = a_certList.elements();
				while(certificates.hasMoreElements())
				{
					JAPCertificate currentCertificate = (JAPCertificate)certificates.nextElement();
					X509Extensions extensions = new X509Extensions(new X509CertificateIssuer(currentCertificate.getIssuer()));
					addCRLEntry(new DERInteger(createPseudoSerial(currentCertificate.toByteArray())), 
							new Time(new Date()), extensions.getBCX509Extensions());
				}
			}
		}
		
		public CertificateList sign(PKCS12 a_privateIssuerCertificate)
		{
			return sign(a_privateIssuerCertificate.getSubject().getX509Name(), a_privateIssuerCertificate.getPrivateKey());
		}

		public CertificateList sign(
				X509Name a_issuer,
				IMyPrivateKey a_privateKey)
		{
			try
			{
				TBSCertList tbsList;
				DEREncodableVector seqv;
				ByteArrayOutputStream bOut;
				byte[] signature;
				
				setIssuer(a_issuer);
				setSignature(a_privateKey.getSignatureAlgorithm().getIdentifier());

				/* generate signature */
				bOut = new ByteArrayOutputStream();
				tbsList = generateTBSCertList();
				(new DEROutputStream(bOut)).writeObject(tbsList);
				signature = ByteSignature.sign(bOut.toByteArray(), a_privateKey);

				/* construct crl */
				seqv = new ASN1EncodableVector();
				seqv.add(tbsList);
				seqv.add(a_privateKey.getSignatureAlgorithm().getIdentifier());
				seqv.add(new DERBitString(signature));

				return new CertificateList(new DERSequence(seqv));
			}
			catch (Throwable t)
			{
				LogHolder.log(LogLevel.EXCEPTION, LogType.MISC, t);
				return null;
			}
		}
	}
}
