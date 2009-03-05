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
import java.util.Vector;

import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import anon.util.IXMLEncodable;
import anon.util.XMLParseException;
import anon.util.XMLUtil;

/**
 * Stores a certification path with all included certificates.
 * @author Robert Hirschberger
 */
public class CertPath implements IXMLEncodable
{
	public static final String XML_ELEMENT_NAME = "CertPath";
	public static final String XML_ATTR_CLASS = "rootCertificateClass";
	public static final String XML_ATTR_TYPE = "certificateType";
	
	public static final int NO_ERRORS								= 0x0;
	public static final int ERROR_VERIFICATION 						= 0x1;
	public static final int ERROR_VALIDITY							= 0x2;
	public static final int ERROR_REVOCATION   						= 0x3;
	public static final int ERROR_UNKNOWN_CRITICAL_EXTENSION 		= 0x4;
	public static final int ERROR_BASIC_CONSTRAINTS_IS_CA			= 0x5;
	public static final int ERROR_BASIC_CONSTRAINTS_IS_NO_CA		= 0x6;
	public static final int ERROR_BASIC_CONSTRAINTS_PATH_TOO_LONG	= 0x7;
	public static final int ERROR_KEY_USAGE							= 0x8;
	
	private static final int VERIFICATION_INTERVAL = 3 * 60 * 1000;
	
	/** the certificate class of the certs that may verify this CertPath */
	private int m_documentType;
	
	/** the included certificates */
	private Vector m_certificates;
	/** true if the last cert is a root cert */
	private boolean m_rootFound;
	/** true if the CertPath has valid format (not timely valid!!) */
	private boolean m_valid;
	/** inicates if the CertPath was verified within the last VERIFICATION_INTERVAL */
	private boolean m_verified;
	/** time when the CertPath was verified for the last time */
	private long m_verificationTime;
	
	private int m_pathError;
	private int m_errorPosition;

	/**
	 * Creates a new CertPath Object from a given Certificate
	 * @param firstCert The first certificate of the path
	 *                  (it will be on the lowest level of the cert hierarchy)
	 */
	private CertPath(JAPCertificate a_firstCert, int a_documentType)
	{
		m_certificates = new Vector();
		m_documentType = a_documentType;
		m_verificationTime = 0;
		m_verified = false;
		m_pathError = NO_ERRORS;
		m_errorPosition = -1;
		appendCertificate(a_firstCert);
		m_rootFound = false;
	}

	protected CertPath(Element a_elemCertPath) throws XMLParseException
	{
		if (a_elemCertPath == null || !a_elemCertPath.getNodeName().equals(XML_ELEMENT_NAME))
		{
			throw new XMLParseException(XMLParseException.ROOT_TAG, XML_ELEMENT_NAME);
		}

		XMLUtil.parseAttribute(a_elemCertPath, XML_ATTR_TYPE, -1);
		if (m_documentType == -1)
		{
			// only for compatibility < 00.10.074
			m_documentType = getDocumentTypeFromRootCertType(XMLUtil.parseAttribute(a_elemCertPath, XML_ATTR_CLASS, -1));
		}

		NodeList listCerts = a_elemCertPath.getElementsByTagName(JAPCertificate.XML_ELEMENT_NAME);
		if (listCerts.getLength() == 0)
		{
			throw new XMLParseException("No certificates found!");
		}
		m_certificates = new Vector(listCerts.getLength());
		for (int i = 0; i < listCerts.getLength(); i++)
		{
			m_certificates.addElement(JAPCertificate.getInstance(listCerts.item(i)));
		}
		if (m_documentType == SignatureVerifier.DOCUMENT_CLASS_NONE) 
		{ //rootCertPaths do not need validation
			m_valid = true;
		}
		else
		{ 
			m_valid = buildAndValidate(null);
		}
	}
	
	public static CertPath getRootInstance(JAPCertificate a_rootCert)
	{
		//TODO check extensions!
		CertPath rootCertPath = new CertPath(a_rootCert, JAPCertificate.CERTIFICATE_TYPE_ROOT);
		rootCertPath.m_valid = true;
		return rootCertPath;
	}
	
	/**
	 * @param a_firstCert
	 * @param a_documentType
	 * @param a_pathCertificates
	 * @return
	 */
	public static CertPath getInstance(JAPCertificate a_firstCert, int a_documentType, Vector a_pathCertificates)
	{
		Vector pathCerts;
		
		if(a_firstCert == null)
		{
			return null;
		}
		//check cached CertPaths here!
		CertificateInfoStructure cachedPath = null;
		cachedPath = SignatureVerifier.getInstance().getVerificationCertificateStore()
												.getCertificateInfoStructure(a_firstCert, getCertType(a_documentType));
		
		if (cachedPath != null && cachedPath.getCertPath().m_valid)
		{
			//check if the cached Path is valid and if not 
			//check if we may be able to build a valid one now
			if(cachedPath.getCertPath().checkValidity(new Date())
					|| !isPossiblyValid(a_firstCert, a_pathCertificates))
			{
				return cachedPath.getCertPath();
			}
		}

		//build and validate a new CertPath
		CertPath certPath = new CertPath(a_firstCert, a_documentType);
		
		pathCerts = (Vector) a_pathCertificates.clone();
		certPath.m_valid = certPath.buildAndValidate(pathCerts);
		
		if (!certPath.m_valid) //we built an invalid path 
		{
			if(cachedPath != null) //return the cached path if there is one (it is also invalid) 
			{
				return cachedPath.getCertPath();
			}
		}
		
		//store new Path
		SignatureVerifier.getInstance().getVerificationCertificateStore().addCertificateWithVerification(certPath, getCertType(a_documentType), false);
		
		return certPath; 
	}
	
	
	/**
	 * Checks if it may be possible to build a (timely) valid CertPath from the
	 * given certificates. To return <code>true</code> the first cert has to be 
	 * valid an at least one of the path certs, too.
	 * @param a_firstCert the certificate to verify
	 * @param a_pathCertificates the possible path certificates
	 * @return <code>true</code> if it is generally possible to build a (timely)
	 * 		   valid CertPath
	 */
	private static boolean isPossiblyValid(JAPCertificate a_firstCert,
			Vector a_pathCertificates)
	{
		Enumeration certs;
		JAPCertificate nextCert;
		
		if(a_firstCert.getValidity().isValid(new Date()))
		{
			certs = a_pathCertificates.elements();
			while(certs.hasMoreElements())
			{
				nextCert = (JAPCertificate) certs.nextElement();
				if(nextCert.getValidity().isValid(new Date()))
				{
					return true;
				}
			}
		}
		return false;
	}

	private boolean buildAndValidate(Vector a_pathCertificates)
	{	
		Enumeration certificates;
		JAPCertificate current = null, issuer;
		int pathPosition = 0;
		
		//build the CertPath though name- and key-chaining
		build(a_pathCertificates);
		
		synchronized (m_certificates)
		{
			certificates = m_certificates.elements();
						
			if(certificates.hasMoreElements())
			{
				current = (JAPCertificate) certificates.nextElement();
				do
				{
					issuer = null;
					if(certificates.hasMoreElements())
					{
						issuer = (JAPCertificate) certificates.nextElement();
					}
					
					m_pathError = validate(current, pathPosition, issuer);
					if(m_pathError != NO_ERRORS)
					{
						m_errorPosition = pathPosition;
						if(m_pathError == ERROR_VERIFICATION 
								|| m_pathError == ERROR_REVOCATION 
								|| m_pathError == ERROR_UNKNOWN_CRITICAL_EXTENSION)
						{
							return false;
						}
						else
						{
							//all other errors are non-critical for the moment
						}
					}
					current = issuer;
					pathPosition++;
				} 
				while(current != null);
			}	
			return true;
		}
	}
	
	private void build(Vector a_pathCertificates)
	{	
		JAPCertificate pathCertificate = null;
		
		if(a_pathCertificates != null)
		{
			pathCertificate = doNameAndKeyChaining(getLastCertificate(), a_pathCertificates);
		}
		
		while(pathCertificate != null)
		{
			appendCertificate(pathCertificate);
			pathCertificate = doNameAndKeyChaining(pathCertificate, a_pathCertificates);
		}
		
		findVerifier();
	}
	
	private void findVerifier()
	{
		JAPCertificate trustAnchor;
		Vector rootCertificates = SignatureVerifier.getInstance().getVerificationCertificateStore().
										getAvailableCertificatesByType(getRootCertType(m_documentType));
		
		trustAnchor = doNameAndKeyChaining(this.getLastCertificate(), rootCertificates);
		if(trustAnchor == null)
		{
			rootCertificates = SignatureVerifier.getInstance().getVerificationCertificateStore().
									getUnavailableCertificatesByType(getRootCertType(m_documentType));
			trustAnchor = doNameAndKeyChaining(this.getLastCertificate(), rootCertificates);
		}
		if (trustAnchor != null)
		{
			m_rootFound = true;
			this.appendCertificate(trustAnchor);
		}
	}

	/**
	 * Tries to find a possible verifier for the given cert from the given Vector
	 * of certs by comparing the cert's subject with the issuer of the possible verifiers.
	 * If the cert contains an AuthorityKeyIdentifier Extension it will also be 
	 * compared with the SubjectKeyIdentifier of the possible verifiers
	 * @param a_cert the cert to find the issuer for
	 * @param a_possibleIssuers a vector of certs to search fot the issuer
	 * @return the possible issuer or <code>null</code> if there was none
	 */
	private static JAPCertificate doNameAndKeyChaining(JAPCertificate a_cert, Vector a_possibleIssuers)
	{
		JAPCertificate issuer;
		JAPCertificate sameIssuer = null;
		Enumeration issuers = a_possibleIssuers.elements();
		
		while(issuers.hasMoreElements())
		{
			Object obj = issuers.nextElement();
			
			if(obj instanceof JAPCertificate)
			{
				issuer = (JAPCertificate) obj;
			}
			else
			{
				issuer = ((CertificateInfoStructure) obj).getCertificate();
			}
			
			//Name Chaining
			if (a_cert.getIssuer() != null && issuer.getSubject() != null)
			{
				if (a_cert.getIssuer().equals(issuer.getSubject()))
				{
					X509AuthorityKeyIdentifier aki = (X509AuthorityKeyIdentifier) a_cert.getExtensions().
														getExtension(X509AuthorityKeyIdentifier.IDENTIFIER);
					
					//Key Chaining (only if an aki extensions is available)
					if (aki != null)
					{
						if(!aki.getValue().equals(issuer.getSubjectKeyIdentifier()))
						{ //key identifiers do not match -> obviously this is the wrong issuer
							continue;
						}
					}
					if (a_cert.equals(issuer))
					{
						sameIssuer = issuer; //maybe this cert is self-signed
					}
					else
					{
						return issuer;
					}
				}
			}
		}
		return sameIssuer;
	}
	
	private int validate(JAPCertificate a_cert, int a_position, JAPCertificate a_issuer)
	{
		//check Signature
		if(a_issuer != null && !a_cert.verify(a_issuer))
		{
			return ERROR_VERIFICATION;
		}
		//check validity
		if(!a_cert.getValidity().isValid(new Date()))
		{
			return ERROR_VALIDITY;
		}
		//check revocation
		if(a_cert.isRevoked())
		{
			return ERROR_REVOCATION;
		}
		//check if there is an unknown critial extension in the cert
		if(a_cert.getExtensions().hasUnknownCriticalExtensions())
		{
			return ERROR_UNKNOWN_CRITICAL_EXTENSION;
		}
		//check BasicConstraints extension (only if available)
		X509BasicConstraints basicConstraints = 
			(X509BasicConstraints) a_cert.getExtensions().getExtension(X509BasicConstraints.IDENTIFIER);
		if(basicConstraints != null)
		{
			if(basicConstraints.isCA())
			{
				if(a_position == 0)
				{	//end entity certificates are not issued by a CA!
					return ERROR_BASIC_CONSTRAINTS_IS_CA;
				}
				int maxPathLength = basicConstraints.getPathLengthConstraint();
				if(maxPathLength != -1 && maxPathLength < a_position)
				{	//path length is too short for current position in path
					return ERROR_BASIC_CONSTRAINTS_PATH_TOO_LONG;
				}
			}
			else
			{
				if(a_position > 0)
				{
					return ERROR_BASIC_CONSTRAINTS_IS_NO_CA;
				}
			}
		}
		//check KeyUsage extension (only if available)
		X509KeyUsage keyUsage = 
			(X509KeyUsage) a_cert.getExtensions().getExtension(X509KeyUsage.IDENTIFIER);
		if(keyUsage != null)
		{
			//TODO usages ok like this?
			if(a_position == 0)
			{
				if(!keyUsage.allowsDigitalSignature())
				{
					return ERROR_KEY_USAGE;
				}
			}
			else
			{
				if(!keyUsage.allowsDigitalSignature() 
						|| !keyUsage.allowsKeyCertSign())
				{
					return ERROR_KEY_USAGE;
				}
			}
		}
		return NO_ERRORS;
	}


	public Element toXmlElement(Document a_doc)
	{
		if (a_doc == null)
		{
			return null;
		}

		Element elemCertPath = a_doc.createElement(XML_ELEMENT_NAME);
		XMLUtil.setAttribute(elemCertPath, XML_ATTR_TYPE, m_documentType);
		synchronized (m_certificates)
		{
			Enumeration enumCerts = m_certificates.elements();
			while (enumCerts.hasMoreElements())
			{
				elemCertPath.appendChild(((JAPCertificate)enumCerts.nextElement()).toXmlElement(a_doc));
			}
		}
		return elemCertPath;
	}

	/**
	 * Adds a certificate to next higher level of this CertPath,
	 * if the cert is not already included
	 * @param a_certificate the certificate to add
	 */
	private void appendCertificate(JAPCertificate a_certificate)
	{
		synchronized (m_certificates)
		{
			if (!m_certificates.contains(a_certificate))
			{
				m_certificates.addElement(a_certificate);
			}
		}
	}
	
	private void removeLastCertificate()
	{
		synchronized (m_certificates)
		{
			if (m_certificates.size() > 1)
			{
				m_certificates.removeElementAt(m_certificates.size()-1);
			}
		}
	}

	/**
	 * Returns the top level certificate (it is the one that was last added)
	 * @return the last added certificate
	 */
	public JAPCertificate getLastCertificate()
	{
		synchronized (m_certificates)
		{
			if (m_certificates.size() > 0)
			{
				return (JAPCertificate) m_certificates.lastElement();
			}
			return null;
		}
	}

	/**
	 * Returns the certificate from the lowest Level of this CertPath (the one
	 * that was added at first). If this CertPath is from a Mix this would be
	 * the Mix Certificate.
	 * @return the first added certificate
	 */
	public JAPCertificate getFirstCertificate()
	{
		synchronized (m_certificates)
		{
			if (m_certificates.size() > 0)
			{
				return (JAPCertificate) m_certificates.firstElement();
			}
			return null;
		}
	}

	/**
	 * Returns the certificate from the second lowest Level of this CertPath
	 * (the one that was added at Second).
	 * If this CertPath is from a Mix this would be the Operator Certificate.
	 * @return the second added certificate
	 */
	public JAPCertificate getSecondCertificate()
	{
		synchronized (m_certificates)
		{
			if (m_certificates.size() <= 1)
			{
				return null;
			}
			return (JAPCertificate) m_certificates.elementAt(1);
		}
	}

	/**
	 * Sets the certificate class for the root certificates that can verify this
	 * Cert Path. This Method is usually called by the getVerifiedXml()-from the
	 * SignatureVerifier.
	 * It translates the document class from the SignatureVerifier to the
	 * certificate class from JAPCertificate
	 * @see also anon.crypto.SignatureVerifier.getVerifiedXml()
	 * @param a_documentClass a document class from the SignatureVerifier
	 */
	private static int getRootCertType(int a_documentClass)
	{
		switch (a_documentClass)
		{
			case SignatureVerifier.DOCUMENT_CLASS_MIX:
				return JAPCertificate.CERTIFICATE_TYPE_ROOT_MIX;
			case SignatureVerifier.DOCUMENT_CLASS_INFOSERVICE:
				return JAPCertificate.CERTIFICATE_TYPE_ROOT_INFOSERVICE;
			case SignatureVerifier.DOCUMENT_CLASS_UPDATE:
				return JAPCertificate.CERTIFICATE_TYPE_ROOT_UPDATE;
			case SignatureVerifier.DOCUMENT_CLASS_PAYMENT:
				return JAPCertificate.CERTIFICATE_TYPE_ROOT_PAYMENT;
			case SignatureVerifier.DOCUMENT_CLASS_NONE:
				return JAPCertificate.CERTIFICATE_TYPE_ROOT;
			default:
				return -1;
		}
	}
	
	/**
	 * Only for backwards compatibility. Maybe removed after JonDo 00.10.074 is released.
	 * @param a_rootCertType
	 * @return
	 */
	private static int getDocumentTypeFromRootCertType(int a_rootCertType)
	{
		switch (a_rootCertType)
		{
			case JAPCertificate.CERTIFICATE_TYPE_ROOT_MIX:
				return SignatureVerifier.DOCUMENT_CLASS_MIX;
			case JAPCertificate.CERTIFICATE_TYPE_ROOT_INFOSERVICE:
				return SignatureVerifier.DOCUMENT_CLASS_INFOSERVICE;
			case JAPCertificate.CERTIFICATE_TYPE_ROOT_UPDATE:
				return SignatureVerifier.DOCUMENT_CLASS_UPDATE;
			case JAPCertificate.CERTIFICATE_TYPE_ROOT_PAYMENT:
				return SignatureVerifier.DOCUMENT_CLASS_PAYMENT;
			case JAPCertificate.CERTIFICATE_TYPE_ROOT:
				return SignatureVerifier.DOCUMENT_CLASS_NONE;
			default:
				return -1;
		}
	}
	
	private static int getCertType(int a_documentClass)
	{
		switch (a_documentClass)
		{
			case SignatureVerifier.DOCUMENT_CLASS_MIX:
				return JAPCertificate.CERTIFICATE_TYPE_MIX;
			case SignatureVerifier.DOCUMENT_CLASS_INFOSERVICE:
				return JAPCertificate.CERTIFICATE_TYPE_INFOSERVICE;
			case SignatureVerifier.DOCUMENT_CLASS_UPDATE:
				return JAPCertificate.CERTIFICATE_TYPE_UPDATE;
			case SignatureVerifier.DOCUMENT_CLASS_PAYMENT:
				return JAPCertificate.CERTIFICATE_TYPE_PAYMENT;
			case SignatureVerifier.DOCUMENT_CLASS_NONE:
				return JAPCertificate.CERTIFICATE_TYPE_ROOT;
			default:
				return -1;
		}
	}

	/**
	 * Tries to find a verifying root certificate for the top level cert.
	 * After that we get the CertificateInfoStructure for this cert from the
	 * SignatureVerifier.
	 * @see also anon.crypto.SignatureVerifier.getCertificateInfoStructure()
	 * @param checkValidity shall the validity be checked?
	 * @return the CertificateInfoStructure for the verifing certificate,
	 *         null if there is none.
	 */
	/*public CertificateInfoStructure getVerifier(boolean alsoDeactivated)
	{
		JAPCertificate lastCert, verifier;
		CertificateInfoStructure rootInfoStruct;
		Vector rootCertificates;
		
		lastCert = this.getLastCertificate();
		rootCertificates = SignatureVerifier.getInstance().getVerificationCertificateStore().
										getAvailableCertificatesByType(m_rootCertificateClass);
		
		
		while(rootCertificates.size() != 0)
		{
			verifier = doNameAndKeyChaining(lastCert, rootCertificates);
			if(verifier != null && lastCert.verify(verifier))
			{
				return verifier;
			}
			
			//we found a verifier that had the right name but did not verify the path 
			// -> remove it from rootCerts and try agian
			for(int i=0; i<rootCertificates.size(); i++)
			{
				rootInfoStruct = (CertificateInfoStructure) rootCertificates.get(i);
				if(rootInfoStruct.getCertificate().equals(verifier))
				{
					if(!rootCertificates.removeElement(rootInfoStruct))
					{ //something's wrong - return false to avoid an endless loop
						m_verified = false;
						return false;
					}
					break;
				}
			}
		}
	}
	m_verified = false;
	return false;
	}}*/

	/**
	 * Checks the validity of all certificates in the path. If only one of the certificates is outdated,
	 * it returns false.
	 * @param a_date the date for which the validity of the path is tested
	 * @return if all certificates in the path are valid at the given time
	 */
	public boolean checkValidity(Date a_date)
	{
		if (a_date == null)
		{
			return false;
		}

		synchronized (m_certificates)
		{
			Enumeration enumCerts = m_certificates.elements();
			while (enumCerts.hasMoreElements())
			{
				if (!((JAPCertificate)enumCerts.nextElement()).getValidity().isValid(a_date))
				{
					return false;
				}
			}
			return true;
		}
	}

	protected boolean isVerifier(JAPCertificate a_certificate)
	{
		if(a_certificate == null)
		{
			return false;
		}
		if(!m_valid)
		{
			return false;
		}
		if (m_rootFound && a_certificate.equals(getLastCertificate()))
		{
			return true;
		}
		return getLastCertificate().verify(a_certificate);
	}

	/**
	 * Tries to verify the top level certificate in this CertPath against the root certificates.
	 * If this last certificate can be verified the whole CertPath is verified, because we only
	 * generate valid CertPaths
	 * @return true if the CertPath could be verified
	 */
	public synchronized boolean verify()
	{
		CertificateInfoStructure rootCert;
		Vector trustedCerts;
		
		long verificationDelta;
		
		if (m_documentType == SignatureVerifier.DOCUMENT_CLASS_NONE)
		{
			return true;
		}
		
		//calculate time since last verification
		verificationDelta = System.currentTimeMillis() - m_verificationTime;	
		if (verificationDelta < VERIFICATION_INTERVAL)
		{
			//System.out.println("Using cached verification result for " + this.getFirstCertificate().getSubject().getCommonName());
			return m_verified;
		}
		
		m_valid = buildAndValidate(null);
		m_verificationTime = System.currentTimeMillis();
		//System.out.println("Doing fresh verification of " + this.getFirstCertificate().getSubject().getCommonName() + "...");
			
		rootCert = SignatureVerifier.getInstance().getVerificationCertificateStore().getCertificateInfoStructure(getLastCertificate());
		if (m_rootFound)
		{
			if (rootCert != null && rootCert.getCertificateType() == getRootCertType(m_documentType))
			{
				if (rootCert.isAvailable() && m_valid)
				{
					m_verified = true;
					return true;
				}
			} 
			else //maybe root cert was deleted meanwhile?
			{
				if (rootCert != null && rootCert.getCertificateType() != getRootCertType(m_documentType))
				{
					LogHolder.log(LogLevel.ALERT, LogType.CRYPTO, "Verification root certificate found in wrong type path! " +
							"Cert doctype: " + rootCert.getCertificateType() + " Expected doc type: " + getRootCertType(m_documentType) + 
							(rootCert.getCertificate() != null ? " SKI:" + rootCert.getCertificate().getSubjectKeyIdentifier() : ""));
					m_verified = false;
					return false;
				}
				
				this.removeLastCertificate();
				m_rootFound = false;
				this.resetVerification();
				return verify();
			}
		}
		else
		{
			// try a verification directly against trusted certificates
			
			// this will only work for self signed certificates, so make a pre-check for faster validation
			trustedCerts = new Vector();
			trustedCerts.addElement(getLastCertificate());
			if (doNameAndKeyChaining(getLastCertificate(), trustedCerts) != null)
			{
				trustedCerts = 
					SignatureVerifier.getInstance().getVerificationCertificateStore().getAvailableCertificatesByType(
						getCertType(m_documentType));

				if (m_valid && doNameAndKeyChaining(getLastCertificate(), trustedCerts) != null)
				{
					m_verified = true;
					return true;
				}
			}
		}
		m_verified = false;
		return false;
	}

	/**
	 * Returns the number of certificates in this CertPath
	 * @return the number of certificates in this CertPath
	 */
	public int length()
	{
		return m_certificates.size();
	}

	/**
	 * Creates an Enumeration of CertificateInfoStructures of the included certs.
	 * The first element of this Enumeration is the verifier of this CertPath if
	 * there is one. The isEnabled() field of the CIS is used to mark if the certs
	 * are verified.
	 * @return an Enumeration of CertificateInfoStructures of the included certs plus
	 *         the verifier as first element if there is one.
	 *         
	 */
	/*public Hashtable getCertificates()
	{
		
		synchronized (m_certificates)
		{
			boolean verifierFound = false;
			Hashtable h = new Hashtable(m_certificates.size()+1);
			JAPCertificate cert;
			
			//try to find a verifier
			CertificateInfoStructure verifier = this.getVerifier(false);

			if (verifier != null)
			{
				verifierFound = true;
				//if the verifying certificate is not already in the CertPath we add it to the top level
				if (!m_certificates.contains(verifier.getCertificate())) //.equals(this.getLatestAddedCertificate()))
				{
					certificateIS.addElement(verifier);
				}
			}
			cert = this.getLastCertificate();
			//Mark the top level CIS, if there is no verifier for the CertPath
			certificateIS.addElement(new CertificateInfoStructure(cert,  null, 1,
				verifierFound, false, false, false));
			//if there is more than one cert in the path add those, too
			for(int i = m_certificates.size()-2; i >= 0; i--)
			{
				cert = (JAPCertificate)m_certificates.elementAt(i);
				certificateIS.addElement(new CertificateInfoStructure(cert, null, 1, true, false, false, false));
			}

			return certificateIS.elements();
		}
	}*/
		
	protected void resetVerification()
	{
		m_verificationTime = 0;
	}
	
	/**
	 * Creates a human readable List in String-Format using the CommonNames of
	 * the included certs. This is mainly used for debugging. To display a CertPath
	 * use a CertDetailsDialog and call the getCertificates()-Method
	 * @return a String representation of this CertPath object
	 */
	public String toString()
	{
		synchronized (m_certificates)
		{
			String certPath = new String("Certification Path (" + length() + "):");
			String tabs = new String();

			for(int i=m_certificates.size(); i>0; i--)
			{
				tabs += "\t";
				certPath += "\n" + tabs +
					( (JAPCertificate) m_certificates.elementAt(i-1)).getSubject().getCommonName();
			}
			return certPath;
		}
	}
	
	public CertPathInfo getPathInfo()
	{
		CertPathInfo info;
		JAPCertificate first = null, second = null, root = null;
		Vector subCAs = null;
		int len;
		boolean verified;
		
		synchronized (m_certificates)
		{
			verified = this.verify();
			len = length();
			first = getFirstCertificate();
			if(len > 1)
			{
				if(m_rootFound)
				{
					root = getLastCertificate();
					len--;
				}
			}
			if(len > 1)
			{
				second = getSecondCertificate();
			}
			if(len > 2)
			{
				subCAs = new Vector();
				for(int i=2; i<len; i++)
				{
					subCAs.addElement(m_certificates.elementAt(i));
				}
			}
		}
		
		info = new CertPathInfo(first, second, root, subCAs, 1);
		info.setVerified(verified);
		return info;
	}
	
	public boolean isValidPath()
	{
		return m_valid;
	}
	
	protected Vector getCertificates()
	{
		Vector certs = (Vector) m_certificates.clone();
		if (m_rootFound)
		{
			certs.removeElementAt(certs.size()-1);
		}
		return certs;
	}
	
	public int getErrorCode()
	{
		return m_pathError;
	}
	
	public int getErrorPosition()
	{
		return m_errorPosition;
	}
}
