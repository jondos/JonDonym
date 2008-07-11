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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Date;
import java.util.Enumeration;
import java.util.Vector;

import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;

import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.DERBitString;
import org.bouncycastle.asn1.DEREncodableVector;
import org.bouncycastle.asn1.DEROutputStream;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.asn1.x509.CertificateList;
import org.bouncycastle.asn1.x509.TBSCertList;
import org.bouncycastle.asn1.x509.Time;
import org.bouncycastle.asn1.x509.V2TBSCertListGenerator;
import org.bouncycastle.asn1.x509.X509Name;

/**
 * This Class implements Certificate Revocation Lists (CRLs) as specified by RFC 5280.
 * @author Robert Hirschberger
 * @see http://tools.ietf.org/html/rfc5280
 */
public class CertificateRevocationList
{	
	private CertificateList m_crl;
	private Date m_thisUpdate;
	private Date m_nextUpdate;
	private X509DistinguishedName m_issuer;
	private X509Extensions m_extensions;
	
	private static Class[] CRL_EXTENSIONS = 
		{X509AuthorityKeyIdentifier.class, X509IssuerAlternativeName.class,
			X509IssuingDistributionPoint.class};
	
	public CertificateRevocationList(
			PKCS12 a_issuerCertificate,
			Vector a_certList,
			Date a_nextUpdate,
			X509Extensions a_extensions)
	{
		this(new CRLGenerator(a_issuerCertificate.getSubject().getX509Name(), 
				a_certList, a_nextUpdate, a_extensions).sign(a_issuerCertificate));
	}
		
	public CertificateRevocationList(CertificateList a_crl)
	{
		m_crl = a_crl;
		m_issuer = new X509DistinguishedName(m_crl.getIssuer());
		m_extensions = new X509Extensions(m_crl.getTBSCertList().getExtensions());
		m_thisUpdate = m_crl.getThisUpdate().getDate();
		if(m_crl.getNextUpdate() != null)
		{
			m_nextUpdate = m_crl.getNextUpdate().getDate();
		}
	}
	
	public static CertificateRevocationList getInstance(byte[] a_rawCRL)
	{
		if (a_rawCRL == null || a_rawCRL.length == 0)
		{
			return null;
		}

		try
		{
			ByteArrayInputStream bis = new ByteArrayInputStream(a_rawCRL);
			ASN1InputStream ais = new ASN1InputStream(bis);
			DERSequence sequence = (DERSequence)ais.readObject();
			return new CertificateRevocationList(new CertificateList(sequence));
		}
		catch(Exception e)
		{
			e.printStackTrace();
			return null;
		}
	}
	
	public CertificateList getCRL()
	{
		return m_crl;
	}
	
	public X509DistinguishedName getIssuer()
	{
		return m_issuer;
	}
	
	public Date getThisUpdate()
	{
		return m_thisUpdate;
	}
	
	public X509Extensions getExtensions()
	{
		return m_extensions;
	}
	
	public Vector getRevokedCertificates()
	{
		Vector v = new Vector();
		TBSCertList.CRLEntry[]  crlEntries = m_crl.getRevokedCertificates();
		for(int i=0; i<crlEntries.length; i++)
		{
			v.addElement(new RevokedCertificate(crlEntries[i]));
		}
		return v;
	}
	
	public byte[] toByteArray()
	{
		ByteArrayOutputStream bos = new ByteArrayOutputStream();

		try
		{
			new DEROutputStream(bos).writeObject(this.m_crl);
		}
		catch(Exception e)
		{
			//should be impossible
		}
		return bos.toByteArray();
	}
			
	/**
	 * This class is used to generate, sign and modify CRLs.
	 * @author Robert Hirschberger
	 */
	private static final class CRLGenerator extends V2TBSCertListGenerator
	{
		public CRLGenerator(
				X509Name a_issuer,
				Vector a_certList,
				Date a_nextUpdate,
				X509Extensions a_extensions)
		{
			setIssuer(a_issuer);
			setThisUpdate(new Time(new Date()));
			if(a_nextUpdate != null)
			{
				setNextUpdate(new Time(a_nextUpdate));
			}
			setExtensions(a_extensions.getBCX509Extensions());
			if(a_certList != null)
			{
				Enumeration certificates = a_certList.elements();
				while(certificates.hasMoreElements())
				{
					JAPCertificate currentCertificate = (JAPCertificate)certificates.nextElement();
					RevokedCertificate revCert = new RevokedCertificate(currentCertificate, new Date());
					if(!currentCertificate.getIssuer().equals(a_issuer))
					{
						revCert.addCertificateIssuerExtension();
					}
					addCRLEntry(revCert.toASN1Sequence());
				}
			}
		}
		
		public CertificateList sign(PKCS12 a_privateIssuerCertificate)
		{
			return sign(a_privateIssuerCertificate.getPrivateKey());
		}

		public CertificateList sign(IMyPrivateKey a_privateKey)
		{
			try
			{
				TBSCertList tbsList;
				DEREncodableVector seqv;
				ByteArrayOutputStream bOut;
				byte[] signature;
				
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
