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

import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import anon.util.IXMLEncodable;
import anon.util.XMLParseException;
import anon.util.XMLUtil;

/**
 * Stores a certification path with all included certificates.
 * @see gui.CertDetailsDialog, anon.crypto.XMLSignature
 * @author Robert Hirschberger
 */
public class CertPath implements IXMLEncodable
{
	public static final String XML_ELEMENT_NAME = "CertPath";
	public static final String XML_ATTR_CLASS = "rootCertificateClass";

	public static final int ROOT_CERT = 0;
	
	/** the certificate class of the rootCerts that may verify this  CertPath */
	private int m_rootCertificateClass;
	/** the included certificates */
	private Vector m_certificates;
	/** true if the CertPath has valid format, that means */
	private boolean m_valid;

	/**
	 * Creates a new CertPath Object from a given Certificate
	 * @param firstCert The first certifiacte of the path
	 *                  (it will be on the lowest Level of the cert hierarchy)
	 * @todo make me private and add rootCertType param!
	 */
	private CertPath(JAPCertificate a_firstCert, int a_rootCertType)
	{
		m_certificates = new Vector();
		m_rootCertificateClass = a_rootCertType;
		appendCertificate(a_firstCert);
	}

	protected CertPath(Element a_elemCertPath) throws XMLParseException
	{
		if (a_elemCertPath == null || !a_elemCertPath.getNodeName().equals(XML_ELEMENT_NAME))
		{
			throw new XMLParseException(XMLParseException.ROOT_TAG, XML_ELEMENT_NAME);
		}

		m_rootCertificateClass = XMLUtil.parseAttribute(a_elemCertPath, XML_ATTR_CLASS, -1);

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
		if(m_rootCertificateClass == ROOT_CERT) 
		{ //rootCertPaths do not need validation
			m_valid = true;
		}
		else
		{ 
			m_valid = validate();
		}
	}
	
	protected static CertPath getRootInstance(JAPCertificate a_rootCert)
	{
		//TODO check extensions!
		CertPath rootCertPath = new CertPath(a_rootCert, ROOT_CERT);
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
		//check cached CertPaths here!
		try {
		CertificateInfoStructure cachedPath = SignatureVerifier.getInstance().getVerificationCertificateStore()
												.getCertificateInfoStructure(a_firstCert, getCertType(a_documentType));
		if(cachedPath != null)
		{
			//cachedPath.getCertificate();
			return cachedPath.getCertPath();
		}
		}catch (Exception e)
		{
			e.printStackTrace();
		}
		
		//build and validate a new CertPath
		CertPath certPath = new CertPath(a_firstCert, getRootCertType(a_documentType));
		if(a_pathCertificates != null)
		{
			certPath.m_valid = certPath.buildAndValidate(a_pathCertificates);
			if(!certPath.m_valid)
			{
				//TODO do what? return null?
				/*Maybe build again without validation (to get the most likley path)
				 * and store that this path is invalid (maybe with reason an position)*/
			}
		}
		else
		{
			
		}
		
		//store new Path
		SignatureVerifier.getInstance().getVerificationCertificateStore().addCertificateWithVerification(certPath, getCertType(a_documentType), false);
		
		return certPath; 
	}
	
	private boolean buildAndValidate(Vector a_pathCertificates)
	{
		build(a_pathCertificates);
		if(!validate())
		{
			if(a_pathCertificates.size() > 1)
			{
				JAPCertificate lastCert = getLastCertificate();
				
				removeLastCertificate();
				a_pathCertificates.remove(lastCert);
				
				return buildAndValidate(a_pathCertificates);
			}
			else 
			{
				return false;
			}
		}
		return true;
	}
	
	private void build(Vector a_pathCertificates)
	{
		JAPCertificate lastCertificate = this.getLastCertificate();		
		JAPCertificate pathCertificate = doNameAndKeyChaining(lastCertificate, a_pathCertificates);
		
		while(pathCertificate != null)
		{
			this.appendCertificate(pathCertificate);
			pathCertificate = doNameAndKeyChaining(pathCertificate, a_pathCertificates);
		}
		
		/* we do not add a root certificate into the path... TODO or should we?
		 * 
		pathCertificate = doNameAndKeyChaining(getLastCertificate(), a_rootCertificates);
		if(pathCertificate != null)
		{
			
		}*/
	}
	
	private static JAPCertificate doNameAndKeyChaining(JAPCertificate a_cert, Vector a_possibleIssuers)
	{
		JAPCertificate issuer;
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
			
			if(a_cert.getIssuer().equals(issuer.getSubject()) && !a_cert.equals(issuer))
			{
				//TODO implement KeyChaining here like a_cert.getAKI.equals(issuer.getSKI())
				return issuer;
			}
		}
		return null;
	}
	
	private boolean validate()
	{
		Enumeration certificates = m_certificates.elements();
		JAPCertificate current = null, issuer;
		int pathPosition = 0;
		
		if(certificates.hasMoreElements())
		{
			current = (JAPCertificate) certificates.nextElement();
		}
		while(certificates.hasMoreElements())
		{
			pathPosition++;
			issuer = (JAPCertificate) certificates.nextElement();
				
			if(!current.verify(issuer))
			{
				return false;
			}
			if(!current.getValidity().isValid(new Date()))
			{
				return false;
			}
			//TODO implement me
			/*if(!current.isRevoked())
			{
				return false;
			}*/
			if(current.getExtensions().hasUnknownCriticalExtensions())
			{
				return false;
			}
			if(!checkExtensions(current, pathPosition))
			{
				//return false;
			}
			current = issuer;
		}
		return true;
	}

	private boolean checkExtensions(JAPCertificate a_cert, int a_position)
	{
		//check BasicConstraints
		X509BasicConstraints basicConstraints = 
			(X509BasicConstraints) a_cert.getExtensions().getExtension(X509BasicConstraints.IDENTIFIER);
		if(basicConstraints != null)
		{
			if(basicConstraints.isCA())
			{
				if(a_position == 1)
				{	//end entity certificates are not issued by a CA!
					//TODO return false;
				}
				int maxPathLength = basicConstraints.getPathLengthConstraint();
				if(maxPathLength != -1 && maxPathLength < a_position)
				{	//path length is too short for current position in path
					return false;
				}
			}
		}
		
		//check KeyUsage
		X509KeyUsage keyUsage = 
			(X509KeyUsage) a_cert.getExtensions().getExtension(X509KeyUsage.IDENTIFIER);
		if(keyUsage != null)
		{
			//TODO usages ok like this?
			if(a_position == 1)
			{
				if(!keyUsage.allowsKeyEncipherment()
						|| 	!keyUsage.allowsNonRepudiation())
				{
					//TODO return false;
				}
			}
			else
			{
				if(!keyUsage.allowsKeyCertSign())
				{
					return false;
				}
			}
		}
	
		return true;
	}

	public Element toXmlElement(Document a_doc)
	{
		if (a_doc == null)
		{
			return null;
		}

		Element elemCertPath = a_doc.createElement(XML_ELEMENT_NAME);
		XMLUtil.setAttribute(elemCertPath, XML_ATTR_CLASS, m_rootCertificateClass);
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
			if(m_certificates.size() > 1)
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
	private CertificateInfoStructure getVerifier(boolean checkValidity)
	{
		Vector rootCertificates = SignatureVerifier.getInstance().getVerificationCertificateStore().
			getAvailableCertificatesByType(m_rootCertificateClass);
		JAPCertificate lastCertificate = (JAPCertificate)this.getLastCertificate();
		if (lastCertificate == null)
		{
			return null;
		}
		CertificateInfoStructure verifyingCertificate = (CertificateInfoStructure) lastCertificate.
			getVerifier(rootCertificates.elements(), checkValidity);
		if (verifyingCertificate != null)
		{
			return verifyingCertificate; //SignatureVerifier.getInstance().getVerificationCertificateStore().getCertificateInfoStructure(verifyingCertificate);
		}
		//if there was no verifier in the available root certs, try to find a verifier in the unavailable ones
		rootCertificates = SignatureVerifier.getInstance().getVerificationCertificateStore().
			getUnavailableCertificatesByType(m_rootCertificateClass);
		verifyingCertificate = (CertificateInfoStructure) lastCertificate.getVerifier(rootCertificates.
			elements(), checkValidity);
		if (verifyingCertificate != null)
		{
			return verifyingCertificate; //SignatureVerifier.getInstance().getVerificationCertificateStore().getCertificateInfoStructure(verifyingCertificate);
		}
		return null;
	}

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

	public boolean verify(JAPCertificate a_certificate)
	{
		if (a_certificate == null)
		{
			return false;
		}
		if(!m_valid)
		{
			return false;
		}
		return getLastCertificate().verify(a_certificate);
	}

	/**
	 * Tries to verify the top level certificate in this CertPath against the root certificates.
	 * If this last certificate can be verified the whole CertPath is verified, because we only
	 * generate valid CertPaths
	 * @return true if the CertPath could be verified
	 */
	public boolean verify()
	{
		JAPCertificate lastCert, verifier;
		CertificateInfoStructure wrongVerifier;
		Vector rootCertificates;
		
		if(m_rootCertificateClass == ROOT_CERT)
		{
			return true;
		}
		
		rootCertificates = SignatureVerifier.getInstance().getVerificationCertificateStore().
			getAvailableCertificatesByType(m_rootCertificateClass);
		
		if(m_valid)
		{
			lastCert = this.getLastCertificate();
			while(rootCertificates.size() != 0)
			{
				verifier = doNameAndKeyChaining(lastCert, rootCertificates);
				if(verifier == null)
				{
					return false;
				}
				if(lastCert.verify(verifier))
				{
					return true;
				}
				try //remove the wrong verifier from the vector
				{
					wrongVerifier = SignatureVerifier.getInstance().getVerificationCertificateStore().
						getCertificateInfoStructure(verifier, m_rootCertificateClass);
					rootCertificates.removeElement(wrongVerifier);
				}
				catch(Exception e) //something's wrong - return false to avoid an endless loop
				{
					return false;
				}
			}
		}
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
	 *         @todo return Hashtable instead
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
}
