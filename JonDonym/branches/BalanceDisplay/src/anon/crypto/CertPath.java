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
import java.util.Vector;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import anon.util.IXMLEncodable;
import anon.util.XMLParseException;
import anon.util.XMLUtil;

/**
 * Stores a certification path with all included certificates.
 * Keep in mind, that this CertPath adds a new Certificate to the BEGINNING
 * of the Vector. That means the first Certificate is at the end of the Vector.
 * @see also gui.CertDetailsDialog, anon.crypto.XMLSignature
 * @author Robert Hirschberger
 */
public class CertPath implements IXMLEncodable
{
	public static final String XML_ELEMENT_NAME = "CertPath";
	public static final String XML_ATTR_CLASS = "rootCertificateClass";

	/** the number of certificates included in this CertPath */
	private int m_certCount;
	/** the certificate class of the rootCerts that may verify this  CertPath */
	private int m_rootCertificateClass;
	/** the included certificates */
	private Vector m_certificates;

	/**
	 * Creates a new CertPath Object from a given Certificate
	 * @param firstCert The first certifiacte of the path
	 *                  (it will be on the lowest Level of the cert hierarchy)
	 */
	protected CertPath(JAPCertificate firstCert)
	{
		m_certificates = new Vector();

		if (firstCert == null)
		{
			m_certCount = -1;
			return;
		}

		add(firstCert);
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
	protected void add(JAPCertificate a_certificate)
	{
		synchronized (m_certificates)
		{
			if (!m_certificates.contains(a_certificate))
			{
				m_certificates.insertElementAt(a_certificate, 0);
				m_certCount++;
			}
		}
	}

	/**
	 * Removes the specified certificate from this CertPath
	 * @param a_certificate the certificate to remove
	 */
	protected void remove(JAPCertificate a_certificate)
	{
		synchronized (m_certificates)
		{
			m_certificates.removeElement(a_certificate);
			m_certCount--;
		}
	}

	/**
	 * Removes all certificates except the one on the lowest level
	 * of this certPath
	 */
	protected void removeAllButLast()
	{
		synchronized (m_certificates)
		{
			JAPCertificate firstCertificate = this.getFirstCertificate();
			m_certificates.removeAllElements();
			if (firstCertificate != null)
			{
				m_certCount = 0;
			}
			else
			{
				m_certCount = -1;
			}
			this.add(firstCertificate);
		}
	}

	/**
	 * Returns the top level certificate (it is the one that was last added)
	 * @return the last added certificate
	 */
	protected JAPCertificate getLatestAddedCertificate()
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
				return (JAPCertificate) m_certificates.lastElement();
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
			//return the Certificate at Index size-2 (size-1 would be the last one)
			return (JAPCertificate) m_certificates.elementAt(m_certificates.size() - 2);
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
	protected void setDocType(int a_documentClass)
	{
		switch (a_documentClass)
		{
			case SignatureVerifier.DOCUMENT_CLASS_MIX:
				m_rootCertificateClass = JAPCertificate.CERTIFICATE_TYPE_ROOT_MIX;
				break;
			case SignatureVerifier.DOCUMENT_CLASS_INFOSERVICE:
				m_rootCertificateClass = JAPCertificate.CERTIFICATE_TYPE_ROOT_INFOSERVICE;
				break;
			case SignatureVerifier.DOCUMENT_CLASS_UPDATE:
				m_rootCertificateClass = JAPCertificate.CERTIFICATE_TYPE_UPDATE;
				break;
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
		JAPCertificate lastCertificate = (JAPCertificate)this.getLatestAddedCertificate();
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
		return getLatestAddedCertificate().verify(a_certificate);
	}

	/**
	 * Tries to verify the top level certificate in this CertPath against the root certificates.
	 * If this last certificate can be verified the whole CertPath is verified, because we only
	 * generate valid CertPaths
	 * @return true if the CertPath could be verified
	 */
	public boolean verify()
	{
		Enumeration rootCertificates = SignatureVerifier.getInstance().getVerificationCertificateStore().
			getAvailableCertificatesByType(m_rootCertificateClass).elements();
		JAPCertificate verifier = getLatestAddedCertificate();
		if (verifier != null)
		{
			while (rootCertificates.hasMoreElements())
			{
				if (verifier.verify(
					( (CertificateInfoStructure) rootCertificates.nextElement()).getCertificate()))
				{
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Returns the number of certificates in this CertPath
	 * @return the number of certificates in this CertPath
	 */
	public int getCertCount()
	{
		return m_certCount;
	}

	/**
	 * Creates an Enumeration of CertificateInfoStructures of the included certs.
	 * The first element of this Enumeration is the verifier of this CertPath if
	 * there is one. The isEnabled() field of the CIS is used to mark if the certs
	 * are verified.
	 * @return an Enumeration of CertificateInfoStructures of the included certs plus
	 *         the verifier as first element if there is one.
	 */
	public Enumeration getCertificates()
	{
		synchronized (m_certificates)
		{
			boolean verifierFound = false;
			Enumeration certificates = m_certificates.elements();
			Vector certificateIS = new Vector();
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
			if (certificates.hasMoreElements())
			{
				//Mark the first CIS, if there is no verifier for the CertsPath
				JAPCertificate cert = (JAPCertificate) certificates.nextElement();
				certificateIS.addElement(new CertificateInfoStructure(cert,  null, 1,
					verifierFound, false, false, false));
				while (certificates.hasMoreElements())
				{
					cert = (JAPCertificate) certificates.nextElement();
					certificateIS.addElement(new CertificateInfoStructure(cert, null, 1, true, false, false, false));
				}
			}
			return certificateIS.elements();
		}
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
			String certPath = new String("Certification Path (" + getCertCount() + "):");
			String tabs = new String();
			Enumeration certificates = m_certificates.elements();

			while (certificates.hasMoreElements())
			{
				tabs += "\t";
				certPath += "\n" + tabs +
					( (JAPCertificate) certificates.nextElement()).getSubject().getCommonName();
			}
			return certPath;
		}
	}
}
