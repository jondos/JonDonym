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
import java.util.Vector;

import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.DERInteger;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.asn1.x509.TBSCertList;
import org.bouncycastle.asn1.x509.Time;
import org.bouncycastle.asn1.x509.X509CertificateStructure;
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
	private Vector m_extVector;
	private X509Extensions m_extensions;
	
	private JAPCertificate m_cert;
	
	public RevokedCertificate(X509CertificateStructure a_cert, Date a_revocationDate)
	{
		this(JAPCertificate.getInstance(a_cert), a_revocationDate);
	}
	
	public RevokedCertificate(JAPCertificate a_cert, Date a_revocationDate)
	{
		m_cert = a_cert;
		m_revocationDate =  a_revocationDate;
		
		m_serial =  checkSerial(a_cert.getSerialNumber());
		m_extVector = new Vector();
	}
	
	public RevokedCertificate(BigInteger a_serial, Date a_revocationDate)
	{
		m_serial =  checkSerial(a_serial);
		m_revocationDate =  a_revocationDate;
		m_extVector = new Vector();
	}
	
	protected RevokedCertificate(TBSCertList.CRLEntry a_crlEntry)
	{
		m_serial = a_crlEntry.getUserCertificate().getPositiveValue();
		m_revocationDate = a_crlEntry.getRevocationDate().getDate();
		m_extensions = new X509Extensions(a_crlEntry.getExtensions());
	}
	
	private BigInteger checkSerial(BigInteger a_serial)
	{
		if(a_serial.equals(BigInteger.ZERO) || a_serial.equals(BigInteger.ONE))
		{
			if(m_cert != null) 
			{
				return createPseudoSerial(m_cert.toByteArray());
			}
		}
		return a_serial;
	}
	
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
        
        if (!m_extVector.isEmpty())
        {
            v.add(new X509Extensions(m_extVector).getBCX509Extensions());
        }
        
        return new DERSequence(v);
	}

	public void addCertificateIssuerExtension()
	{
		//TODO check if already existing
		m_extVector.add(new X509CertificateIssuer(m_cert.getIssuer()));
	}
	
	public X509Extensions getExtensions()
	{
		if(m_extensions != null)
		{
			return m_extensions;
		}
		return new X509Extensions(m_extVector);
	}
}
