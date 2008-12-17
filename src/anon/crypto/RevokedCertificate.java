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

import java.math.BigInteger;
import java.util.Date;

import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.DERInteger;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.asn1.x509.TBSCertList;
import org.bouncycastle.asn1.x509.Time;
import org.bouncycastle.crypto.digests.GeneralDigest;
import org.bouncycastle.crypto.digests.SHA1Digest;

/**
 * This Class implements a CRLEntry specified by RFC 5280
 * <pre>
 * revokedCertificates     SEQUENCE OF SEQUENCE  {
 *     userCertificate         CertificateSerialNumber,
 *     revocationDate          Time,
 *     crlEntryExtensions      Extensions OPTIONAL
 *                             -- if present, version MUST be v2
 * }  OPTIONAL
 * </pre>
 * @author Robert Hirschberger
 * @see http://tools.ietf.org/html/rfc5280
 */
public class RevokedCertificate
{
	/** allowed CRL Entry Extension */
	public static final Class[] CRL_ENTRY_EXTENSIONS = 
		{X509CertificateIssuer.class}; //TODO: not implemented: X509ReasonCode, X509InvalidityDate
	
	private BigInteger m_serial;
	private Date m_revocationDate;
	private X509Extensions m_extensions;
	
	public RevokedCertificate(JAPCertificate a_cert, Date a_revocationDate, X509Extensions a_extensions)
	{
		m_revocationDate =  a_revocationDate;
		m_serial =  getUniqueSerial(a_cert);
		m_extensions = a_extensions;
	}
	
	/*public RevokedCertificate(BigInteger a_serial, Date a_revocationDate)
	{
		m_serial =  checkSerial(a_serial);
		m_revocationDate =  a_revocationDate;
	}*/
	
	protected RevokedCertificate(TBSCertList.CRLEntry a_crlEntry)
	{
		m_serial = a_crlEntry.getUserCertificate().getPositiveValue();
		m_revocationDate = a_crlEntry.getRevocationDate().getDate();
		if(a_crlEntry.getExtensions() != null)
		{
			m_extensions = new X509Extensions(a_crlEntry.getExtensions());
		}
	}
	
	/**
	 * If the cert's serial is greater than 1 then the serial is returned,
	 * otherwise a pseudo-serial is created to uniquely identify the cert. 
	 * @param a_cert the cert to get the unique serial
	 * @return the serial of the cert of a pseudo-serial
	 */
	protected static BigInteger getUniqueSerial(JAPCertificate a_cert)
	{
		if(a_cert.getSerialNumber().equals(BigInteger.ZERO) || a_cert.getSerialNumber().equals(BigInteger.ONE))
		{
			return createPseudoSerial(a_cert.toByteArray());
		}
		return a_cert.getSerialNumber();
	}
	
	/**
	 * Creates a pseudo-serial for a cert from the SHA1-value of its raw data.
	 * @param a_rawCert
	 * @return a pseudo-serial
	 */
	private static BigInteger createPseudoSerial(byte[] a_rawCert)
	{
		GeneralDigest digest = new SHA1Digest();
		byte[] value = new byte[digest.getDigestSize()];
		
		digest.update(a_rawCert, 0, a_rawCert.length);
		digest.doFinal(value, 0);
		
		return new BigInteger(value).abs();
	}
		
	protected ASN1Sequence toASN1Sequence()
	{
		ASN1EncodableVector v = new ASN1EncodableVector();

        v.add(new DERInteger(m_serial));
        v.add(new Time(m_revocationDate));
        
        if (m_extensions != null)
        {
            v.add(m_extensions.getBCX509Extensions());
        }
        
        return new DERSequence(v);
	}
	
	public BigInteger getSerialNumber()
	{
		return m_serial;
	}
	
	/**
	 * Checks if this RevokedCertificate contains a X509CertificateIssuer CRLEntry-Extension.
	 * If so the contained distinguished name is returned, <code>null</code> otherwise
	 * @return the certificate's issuer or <code>null</code> if the cert's issuer is the same
	 *         as the crl's issuer
	 */
	public X509DistinguishedName getCertificateIssuer()
	{
		X509CertificateIssuer issuer;
		if(m_extensions != null)
		{
			issuer = (X509CertificateIssuer) m_extensions.getExtension(X509CertificateIssuer.IDENTIFIER);
			if(issuer != null)
			{
				return issuer.getDistinguishedName();
			}
		}
		return null;
	}
	
	public Date getRevocationDate()
	{
		return m_revocationDate;
	}
	
	public X509Extensions getExtensions()
	{
		return m_extensions;
	}
}
