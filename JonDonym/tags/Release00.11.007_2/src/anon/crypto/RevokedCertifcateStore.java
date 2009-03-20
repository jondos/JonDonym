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

import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;

import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;

/**
 * This class holds all valid certificate revocations to check
 * if a certificate is revoked. Implemented as a singleton.
 * @author zenoxx
 */
public class RevokedCertifcateStore
{
	/** the instance of RevokedCertificateStore */
	private static RevokedCertifcateStore m_instance;
	
	/** the path from which to load the crls */
	private static final String CRL_PATH = "crls/";
	
	/** This table holds all revocations. 
	 *  Key is a concatenation of the cert's issuer an its serial 
	 *  Value is an instance of RevokedCertificate
	 */
	private Hashtable m_revokedCerts;
	
	/**
	 * Create the instance of RevokedCertificateStore. To fill the revocation table
	 * the default crls are loaded from a path
	 */
	private RevokedCertifcateStore()
	{
		CertificateRevocationList crl = null;
		m_revokedCerts = new Hashtable();
		Enumeration crls = CertificateRevocationList.getInstance(CRL_PATH, true, null).elements();
		
		while(crls.hasMoreElements())
		{
			crl = (CertificateRevocationList) crls.nextElement();
			//TODO if(crl.verify())
			{
				addRevocations(crl);
			}
		}
		// no crls found
		if(crl == null)
		{
			LogHolder.log(LogLevel.WARNING, LogType.CRYPTO, "Could not load default CRLs!");
		}
	}
	
	/**
	 * Adds the revocations of a single crl to the store.
	 * @param a_crl the crl to add the revocations from
	 */
	private void addRevocations(CertificateRevocationList a_crl)
	{
		RevokedCertificate revCert;
		X509DistinguishedName issuer;
		Enumeration revocations = a_crl.getRevokedCertificates().elements();
		boolean indirectCRL = a_crl.isIndirectCRL();
		
		synchronized(m_revokedCerts)
		{
			while(revocations.hasMoreElements())
			{
				revCert = (RevokedCertificate) revocations.nextElement();
				issuer = null;
				if(indirectCRL)
				{
					issuer = revCert.getCertificateIssuer();
				}
				if(issuer == null)
				{
					issuer = a_crl.getIssuer();
				}
				m_revokedCerts.put(issuer.toString() + revCert.getSerialNumber().toString(), revCert);
			}
		}
	}
	
	/**
    * Returns the instance of RevokedCertificateStore (Singleton). 
    * If there is no instance, there is a new one created.
    * @return the RevokedCertificateStore instance.
    */
	public static RevokedCertifcateStore getInstance()
	{
		synchronized (RevokedCertifcateStore.class)
		{
			if(m_instance == null)
			{
				m_instance = new RevokedCertifcateStore();
			}
			return m_instance;
		}
	}
	
	/** 
	 * Creates a concatenation of the certificate's issuer and its (pseudo-)serial.
	 * @param a_cert the certificate to create the value from
	 * @return the Key value to search the Hashtable
	 * @see RevokedCertificate.getUniqueSerial()
	 */
	private static String keyValue(JAPCertificate a_cert)
	{
		return (a_cert.getIssuer().toString() + RevokedCertificate.getUniqueSerial(a_cert).toString());
	}
	
	/**
	 * Checks if the given cert is revoked.
	 * @param a_cert the cert to check
	 * @return <code>true</code> if the store contains a revocation for the cert, 
	 *         <code>false</code> otherwise
	 */
	public boolean isCertificateRevoked(JAPCertificate a_cert)
	{
		synchronized (m_revokedCerts)
		{
			return m_revokedCerts.containsKey(keyValue(a_cert));
		}
	}
	
	/**
	 * Gets the revocation date for the specified cert, if and only if the
	 * cert is revoked.
	 * @param a_cert the cert to get the revocation Date from
	 * @return the revocation date of the cert or <code>null</code> if the store
	 *         contains no revocation for the cert 
	 */
	public Date getRevocationDate(JAPCertificate a_cert)
	{
		RevokedCertificate revCert;
		synchronized (m_revokedCerts)
		{
			if(isCertificateRevoked(a_cert))
			{
				revCert = (RevokedCertificate) m_revokedCerts.get(keyValue(a_cert));
				return revCert.getRevocationDate();
			}
			return null;
		}
	}
}
