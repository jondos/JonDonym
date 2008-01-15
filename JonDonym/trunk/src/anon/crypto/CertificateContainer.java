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

import java.util.Vector;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import anon.util.IXMLEncodable;
import anon.util.XMLUtil;

/**
 * This class stores additional settings for every certificate stored within a CertificateStore.
 * Any instance of this class should be visible only within the parent CertificateStore it belongs
 * to.
 */
public class CertificateContainer implements IXMLEncodable
{
	/**
	 * Stores the name of the root node of the XML settings for this class.
	 */
	public static final String XML_ELEMENT_NAME = "CertificateContainer";

	private static final String XML_ATTR_REMOVABLE ="removable";




	/**
	 * Stores the parent certificate (the certificate against which verification of this certificate
	 * was successful) of this certificate. This value is only not null, if this certificate needs
	 * verification and the parent certificate is activated within the same certificate store.
	 */
	private JAPCertificate m_parentCertificate;

	private CertPath m_certPath;

	/**
	 * Stores the certificate type of this certificate. See the CERTIFICATE_TYPE constants within
	 * JAPCertificate.
	 */
	private int m_certificateType;

	/**
	 * Stores, whether this certificate is enabled within the certificate store.
	 */
	private boolean m_enabled;

	/**
	 * Stores, whether this certificate needs verification by a parent certificate in order to get
	 * activated within the certificate store.
	 */
	private boolean m_certificateNeedsVerification;

	/**
	 * Stores, whether the certificate can only be removed from the certificate store by calling the
	 * removeCertificate() method. If this value is false, the certificate will be removed also from
	 * the certificate store automatically, if there are no more active locks on the certificate
	 * (removeCertificateLock() was called for all locks on the certificate). Also the certificate
	 * will be persistent (included in the XML structure created by the certificate store) only, if
	 * this value is true (persistence wouldn't make sense for automatically removable
	 * certificates).
	 */
	private boolean m_onlyHardRemovable;

	/** This certifcate is NOT removable - used at the moment for some default certs*/
	private boolean m_bNotRemovable = false;

	/**
	 * Stores all locks on this certificate. Attention: This list can also include locks, if this
	 * certificate cannot be removed automatically from the certificate store.
	 */
	private Vector m_lockList;

	/**
	 * Creates a new instance of CertificateContainer. Only instances of CertificateStore should
	 * call this constructor.
	 *
	 * @param a_certificate The certificate for which the container is built.
	 * @param a_certificateType The type of the certificate.
	 * @param a_certificateNeedsVerification Whether this certificate is only valid within the
	 *                                       certificate store, if it can be verified against an
	 *                                       active root certificate from the store.
	 */
	public CertificateContainer(JAPCertificate a_certificate, int a_certificateType,
								boolean a_certificateNeedsVerification)
	{
		this(new CertPath(a_certificate), a_certificateType, a_certificateNeedsVerification);
	}


	/**
	 * Creates a new instance of CertificateContainer. Only instances of CertificateStore should
	 * call this constructor.
	 *
	 * @param a_certificate The certificate for which the container is built.
	 * @param a_certificateType The type of the certificate.
	 * @param a_certificateNeedsVerification Whether this certificate is only valid within the
	 *                                       certificate store, if it can be verified against an
	 *                                       active root certificate from the store.
	 */
	public CertificateContainer(CertPath a_certPath, int a_certificateType,
								boolean a_certificateNeedsVerification)
	{
		if (a_certPath == null || a_certPath.getFirstCertificate() == null)
		{
			throw new IllegalArgumentException("Invalid cert path!");
		}
		m_certPath = a_certPath;
		m_certificateType = a_certificateType;
		m_certificateNeedsVerification = a_certificateNeedsVerification;
		m_enabled = true;
		m_parentCertificate = null;
		m_onlyHardRemovable = false;
		m_bNotRemovable = false;
		m_lockList = new Vector();
	}

	/**
	 * Creates a new instance of CertificateContainer. Only instances of CertificateStore should
	 * call this constructor. Certificates loaded via this constructor cannot be removed
	 * automatically from the certifcate store and will be persistent (isOnlyHardRemovable() will
	 * return true).
	 *
	 * @param a_certificateContainerNode The XML node to load the settings from
	 *                                   ('CertificateContainer' node). Such a node can be created
	 *                                   by calling toXmlElement().
	 */
	public CertificateContainer(Element a_certificateContainerNode) throws Exception
	{
		/* parse the whole CertificateContainer XML structure */
		Element certificateTypeNode = (Element) (XMLUtil.getFirstChildByName(a_certificateContainerNode,
			"CertificateType"));
		if (certificateTypeNode == null)
		{
			throw (new Exception("No CertificateType node found."));
		}
		/* CertificateType node found -> get the value */
		m_certificateType = XMLUtil.parseValue(certificateTypeNode, -1);
		if (m_certificateType == -1)
		{
			throw (new Exception("Invalid CertificateType value."));
		}
		Element certificateNeedsVerificationNode = (Element) (XMLUtil.getFirstChildByName(
			a_certificateContainerNode, "CertificateNeedsVerification"));
		if (certificateNeedsVerificationNode == null)
		{
			throw (new Exception("No CertificateNeedsVerification node found."));
		}
		/* CertificateNeedsVerification node found -> get the value */
		m_certificateNeedsVerification = XMLUtil.parseValue(certificateNeedsVerificationNode, true);
		Element certificateEnabledNode = (Element) (XMLUtil.getFirstChildByName(a_certificateContainerNode,
			"CertificateEnabled"));
		if (certificateEnabledNode == null)
		{
			throw (new Exception("No CertificateEnabled node found."));
		}
		/* CertificateEnabled node found -> get the value */
		m_enabled = XMLUtil.parseValue(certificateEnabledNode, false);
		Element certificateDataNode = (Element) (XMLUtil.getFirstChildByName(a_certificateContainerNode,
			"CertificateData"));
		if (certificateDataNode == null)
		{
			throw (new Exception("No CertificateData node found."));
		}
		/* CertificateData node found -> get the certificate */
		JAPCertificate certificate =
			JAPCertificate.getInstance(XMLUtil.getFirstChildByName(certificateDataNode,
			JAPCertificate.XML_ELEMENT_NAME));
		if (certificate == null)
		{
			m_certPath = new CertPath((Element)XMLUtil.getFirstChildByName(
						 certificateDataNode, CertPath.XML_ELEMENT_NAME));
			//throw (new Exception("Invalid CertificateData value. Cannot get the certificate."));
		}
		else
		{
			// for compatibility with older versions of this class
			m_certPath = new CertPath(certificate);
		}

		m_parentCertificate = null;
		/* only hard removable certificates can be persistent */
		m_onlyHardRemovable = true;
		// should also be true, but for deleting old entries we do this here...
		m_bNotRemovable = !XMLUtil.parseAttribute(a_certificateContainerNode, XML_ATTR_REMOVABLE, false);

		m_lockList = new Vector();
	}

	/**
	 * Returns the corresponding certificate for this CertificateContainer.
	 *
	 * @return The certificate where the settings stored within this container are belonging to.
	 */
	public JAPCertificate getCertificate()
	{
		return m_certPath.getFirstCertificate();
	}

	public CertPath getCertPath()
	{
		return m_certPath;
	}

	/**
	 * Changes the parent certificate (the certificate against which verification of this
	 * certificate was successful) of this certificate. This value will be null, if the certificate
	 * store doesn't contain a matching parent certificate (or if that certificate is not active).
	 * This value is not meaningful, if this certificate doesn't need verification.
	 *
	 * @param a_parentCertificate The parent certificate of this certificate (maybe null).
	 */
	public void setParentCertificate(JAPCertificate a_parentCertificate)
	{
		m_parentCertificate = a_parentCertificate;
	}

	/**
	 * Returns the parent certificate (the certificate against which verification of this
	 * certificate was successful) of this certificate. This value will be null, if the certificate
	 * store doesn't contain a matching parent certificate (or if that certificate is not active).
	 * This value is not meaningful, if this certificate doesn't need verification.
	 *
	 * @return a_parentCertificate The parent certificate of this certificate (maybe null).
	 */
	public JAPCertificate getParentCertificate()
	{
		return m_parentCertificate;
	}



	/**
	 * Returns the certificate type of this certificate. See the CERTIFICATE_TYPE constants within
	 * JAPCertificate.
	 *
	 * @return The certificate type of this certificate.
	 */
	public int getCertificateType()
	{
		return m_certificateType;
	}

	/**
	 * Returns, whether this certificate needs verification by a parent certificate in order to get
	 * activated within the certificate store.
	 *
	 * @return Whether this certificate needs to be verified against a root certificate in order to
	 *         get activated within the certificate store.
	 */
	public boolean getCertificateNeedsVerification()
	{
		return m_certificateNeedsVerification;
	}

	/**
	 * Returns whether this certificate is activated within the certificate store. Only activated
	 * certificates should be used to verify signatures and other certificates. A certificate is
	 * activated, if it is enabled and can be verified against a root certificate from the
	 * certificate store (only if the certificate requires verification, see
	 * getCertificateNeedsVerification() ).
	 *
	 * @return Whether this certificate is active within the certificate store.
	 */
	public boolean isAvailable()
	{
		boolean returnValue = false;
		synchronized (this)
		{
			returnValue = ((!m_certificateNeedsVerification) || (m_parentCertificate != null)) && m_enabled;
		}
		return returnValue;
	}

	/**
	 * Stores, whether this certificate is enabled within the certificate store.
	 *
	 * @return Whether this certificate is enabled within the certificate store.
	 */
	public boolean isEnabled()
	{
		return m_enabled;
	}

	/**
	 * Changes the value which stores, whether this certificate is enabled within the certificate
	 * store.
	 *
	 * @param a_enabled Whether this certificate shall be enabled within the certificate store.
	 */
	public void setEnabled(boolean a_enabled)
	{
		m_enabled = a_enabled;
	}

	/**
	 * Disables the possibility of removing the certificate automatically from the certificate
	 * store. Then the certificate can only be removed from the store by calling
	 * removeCertificate(). Without calling this method, the certificate will be removed also from
	 * the certificate store automatically, if there are no more active locks on the certificate
	 * (removeCertificateLock() was called for all locks on the certificate). Also the certificate
	 * will be persistent (included in the XML structure created by the certificate store) any more,
	 * if this method is called (persistence wouldn't make sense for automatically removable
	 * certificates).
	 */
	public void enableOnlyHardRemovable()
	{
		m_onlyHardRemovable = true;
	}

	/**
	 * Returns, whether the certificate can only be removed from the certificate store by calling
	 * the removeCertificate() method. If this value is false, the certificate will be removed also
	 * from the certificate store automatically, if there are no more active locks on the
	 * certificate (removeCertificateLock() was called for all locks on the certificate). Also the
	 * certificate will be persistent (included in the XML structure created by the certificate
	 * store) only, if this value is true (persistence wouldn't make sense for automatically
	 * removable certificates) and if it is removable (otherwise it is expected to be reloaded on hte
	 * next start).
	 *
	 * @return Whether the certificate can only be removed from the certificate store by calling
	 *         removeCertificate() and also whether the certificate will be persistent.
	 */
	public boolean isOnlyHardRemovable()
	{
		return m_onlyHardRemovable;
	}

	/** This certifcate is not removeable - this is a workaround for default certificates*/
	public void enableNotRemovable()
	{
		m_bNotRemovable = true;
	}

	public boolean isNotRemovable()
	{
		return m_bNotRemovable;
	}

	/**
	 * Returns the list with all locks on this certificate. Attention: This list can also include
	 * locks, if this certificate cannot be removed automatically from the certificate store.
	 *
	 * @return The list with all locks on this certificate within the context of the certificate
	 *         store this certificate container belongs to.
	 */
	public Vector getLockList()
	{
		return m_lockList;
	}

	/**
	 * Creates an information structure with the settings stored within this certificate container.
	 * This structure can also be used outside the certificate store (while this container should
	 * not be used outside the certificate store because of the possibility of changing the settings
	 * to inconsistent states). The created structure can be used by an application to display the
	 * settings of the certificate. But if some of the settings shall be changed, it is necessary to
	 * call the corresponding method of the CertificateStore (if available).
	 *
	 * @return An information structure with the settings stored within this certificate container.
	 */
	public CertificateInfoStructure getInfoStructure()
	{
		return (new CertificateInfoStructure(m_certPath, m_parentCertificate, m_certificateType, m_enabled,
											 m_certificateNeedsVerification, m_onlyHardRemovable, m_bNotRemovable));
	}

	/**
	 * Creates an XML node with most of the settings stored within this certificate container. This
	 * node can be used later again to construct a new instance of CertificateContainer.
	 *
	 * @param a_doc The XML document, which is the environment for the created XML node.
	 *
	 * @return The XML node with the settings stored within this certificate container
	 *         ('CertificateContainer' node).
	 */
	public Element toXmlElement(Document a_doc)
	{
		Element certificateContainerNode = a_doc.createElement(XML_ELEMENT_NAME);
		synchronized (this)
		{
			XMLUtil.setAttribute(certificateContainerNode, XML_ATTR_REMOVABLE, !m_bNotRemovable);
			Element certificateTypeNode = a_doc.createElement("CertificateType");
			XMLUtil.setValue(certificateTypeNode, m_certificateType);
			Element certificateNeedsVerificationNode = a_doc.createElement("CertificateNeedsVerification");
			XMLUtil.setValue(certificateNeedsVerificationNode, m_certificateNeedsVerification);
			Element certificateEnabledNode = a_doc.createElement("CertificateEnabled");
			XMLUtil.setValue(certificateEnabledNode, m_enabled);
			Element certificateDataNode = a_doc.createElement("CertificateData");
			certificateDataNode.appendChild(m_certPath.toXmlElement(a_doc));
			certificateContainerNode.appendChild(certificateTypeNode);
			certificateContainerNode.appendChild(certificateNeedsVerificationNode);
			certificateContainerNode.appendChild(certificateEnabledNode);
			certificateContainerNode.appendChild(certificateDataNode);
		}
		return certificateContainerNode;
	}
	/**
	 * Note: Only checks if the certificate in the container is the same
	 * @see also JAPCertificate.equals()
	 */
	public boolean equals(Object a_certificateContainer)
	{
		if(this == a_certificateContainer)
		{
			return true;
		}

		if(a_certificateContainer == null || !(a_certificateContainer instanceof CertificateContainer))
		{
			return false;
		}
		//it is a certificateContainer, now compare the certificate ids.
		return m_certPath.getFirstCertificate().getId().equals(
			  ( (CertificateContainer) a_certificateContainer).getCertificate().getId());
	}

	/**
	 * Returns a unique id for this CertificateContainer.
	 * The Container has the same id as the included certifacte
	 * @return a unique id for this CertificateContainer
	 * @see also JAPCertificate.getId()
	 */
	public String getId()
	{
		return m_certPath.getFirstCertificate().getId();
	}

	/**
	 * The hash code is derived from the certificate`s id.
	 * @return the hash code of the certificate
	 * @see also JAPCertificate.hashCode()
	 */
	public int hashCode()
	{
		return m_certPath.getFirstCertificate().hashCode();
	}
}
