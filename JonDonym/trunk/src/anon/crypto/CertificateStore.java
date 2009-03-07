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

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Observable;
import java.util.Vector;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import anon.util.XMLUtil;
import anon.util.IXMLEncodable;
import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;

/*** Implementation remark: The locking order (to avoid deadlocks) in this class is:
 * 1. this
 * 2. m_trustedCertificates
 *
 * DO NEVER EVER synchronize on m_trustedCertificates IF YOU DO NOT OWN A LOCK on this!
 */

public class CertificateStore extends Observable implements IXMLEncodable
{

	/**
	 * Stores the name of the root node of the XML settings for this class.
	 */
	public static final String XML_ELEMENT_NAME = "TrustedCertificates";

	private Hashtable m_trustedCertificates;

	private Hashtable m_lockTable;

	private int m_lockIdPointer;

	public static String getXmlSettingsRootNodeName()
	{
		return XML_ELEMENT_NAME;
	}

	/**
	 * Creates a new certificate store.
	 */
	public CertificateStore()
	{
		m_trustedCertificates = new Hashtable();
		m_lockTable = new Hashtable();
		m_lockIdPointer = 0;
	}

	public Vector getAllCertificates()
	{
		Vector returnedCertificates = new Vector();
		synchronized (this)
		{
			synchronized (m_trustedCertificates)
			{
				Enumeration allCertificates = m_trustedCertificates.elements();
				while (allCertificates.hasMoreElements())
				{
					returnedCertificates.addElement( ( (CertificateContainer) (allCertificates.nextElement())).
						getInfoStructure());
				}
			}
		}
		return returnedCertificates;
	}

	public Vector getUnavailableCertificatesByType(int a_certificateType)
	{
		Vector returnedCertificates = new Vector();
		synchronized (m_trustedCertificates)
		{
			Enumeration allCertificates = m_trustedCertificates.elements();
			while (allCertificates.hasMoreElements())
			{
				CertificateContainer currentCertificateContainer = (CertificateContainer) (allCertificates.
					nextElement());
				if (currentCertificateContainer.getCertificateType() == a_certificateType &&
					!currentCertificateContainer.isAvailable())
				{
					/* return only disabled certificates of the specified type */
					returnedCertificates.addElement(currentCertificateContainer.getInfoStructure());
				}
			}
		}
		return returnedCertificates;
	}

	public CertificateInfoStructure getCertificateInfoStructure(JAPCertificate a_certificate,
		int a_certificateType)
	{
		CertificateContainer container = (CertificateContainer) m_trustedCertificates.get(
				  getCertificateId(a_certificate, a_certificateType));
		if(container != null)
		{
			return container.getInfoStructure();
		}
		return null;
	}


	public CertificateInfoStructure getCertificateInfoStructure(JAPCertificate a_certificate)
	{
		synchronized (m_trustedCertificates)
		{
			Enumeration allCertificates = m_trustedCertificates.elements();
			while (allCertificates.hasMoreElements())
			{
				CertificateContainer currentCertificateContainer = (CertificateContainer) (allCertificates.
					nextElement());
				if (currentCertificateContainer.getCertificate().equals(a_certificate))
				{
					return currentCertificateContainer.getInfoStructure();
				}
			}
		}
		return null;
	}

	public Vector getAvailableCertificatesByType(int a_certificateType)
	{
		Vector returnedCertificates = new Vector();
		synchronized (m_trustedCertificates)
		{
			Enumeration allCertificates = m_trustedCertificates.elements();
			while (allCertificates.hasMoreElements())
			{
				CertificateContainer currentCertificateContainer = (CertificateContainer) (allCertificates.
					nextElement());
				if ( (currentCertificateContainer.getCertificateType() == a_certificateType) &&
					currentCertificateContainer.isAvailable())
				{
					/* return only enabled certificates of the specified type */
					returnedCertificates.addElement(currentCertificateContainer.getInfoStructure());
				}
			}
		}
		return returnedCertificates;
	}

	public int addCertificateWithVerification(CertPath a_certificate, int a_certificateType,
											  boolean a_onlyHardRemovable)
	{
		int lockId = -1;
		if ( (a_certificateType == JAPCertificate.CERTIFICATE_TYPE_MIX) ||
			(a_certificateType == JAPCertificate.CERTIFICATE_TYPE_INFOSERVICE))
		{
			/* only mix and infoservice certificates can be verified at the moment */
			boolean bChanged = false;
			synchronized (m_trustedCertificates)
			{
				int rootType = JAPCertificate.CERTIFICATE_TYPE_ROOT_MIX;
				if (a_certificateType == JAPCertificate.CERTIFICATE_TYPE_INFOSERVICE)
				{
					rootType = JAPCertificate.CERTIFICATE_TYPE_ROOT_INFOSERVICE;
				}
				if (!m_trustedCertificates.containsKey(getCertificateId(a_certificate.getFirstCertificate(),
					a_certificateType)))
				{
					/* the certificate isn't already in this certificate store */
					CertificateContainer certificateContainer =
						new CertificateContainer(a_certificate, a_certificateType, true);
					m_trustedCertificates.put(getCertificateId(a_certificate.getFirstCertificate(),
						a_certificateType), certificateContainer);
					/* verify the new certificate against all enabled root certificates */
					Enumeration rootCertificates = getAvailableCertificatesByType(rootType).elements();
					boolean verificationSuccessful = false;
					while (rootCertificates.hasMoreElements() && (!verificationSuccessful))
					{
						/* try to verify the certificate against the next root certificate */
						JAPCertificate currentRootCertificate = ( (CertificateInfoStructure) (
							rootCertificates.nextElement())).getCertificate();
						verificationSuccessful = a_certificate.isVerifier(currentRootCertificate);
						if (verificationSuccessful)
						{
							/* we have found the parent certificate */
							certificateContainer.setParentCertificate(currentRootCertificate);
						}
					}
					/* we have added one certificate */
					bChanged = true;
				}
				if (!a_onlyHardRemovable)
				{
					/* add a lock to the lock table */
					lockId = getNextAvailableLockId();
					m_lockTable.put(new Integer(lockId), getCertificateId(a_certificate.getFirstCertificate(),
						a_certificateType));
					/* also add the new lock to the lock list of the certificate */
					( (CertificateContainer) (m_trustedCertificates.get(getCertificateId(
									   a_certificate.getFirstCertificate(),
						a_certificateType)))).getLockList().
						addElement(new Integer(lockId));
				}
				else
				{
					/* enable the only hard removable flag */
					( (CertificateContainer) (m_trustedCertificates.get(getCertificateId(
									   a_certificate.getFirstCertificate(),
						a_certificateType)))).enableOnlyHardRemovable();
				}
				/* notify the observers, only meaningful, if setChanged() was called */
			}
			if (bChanged)
			{
				setChanged();
				notifyObservers(new Integer(a_certificateType));
			}
		}
		return lockId;
	}

	public synchronized int addCertificateWithoutVerification(JAPCertificate a_certificate, int a_certificateType,
												 boolean a_onlyHardRemovable, boolean a_bNotRemovable)
	{
		return addCertificateWithoutVerification(CertPath.getRootInstance(a_certificate), a_certificateType,
												 a_onlyHardRemovable, a_bNotRemovable);
	}

	public synchronized int addCertificateWithoutVerification(CertPath a_certPath, int a_certificateType,
												 boolean a_onlyHardRemovable, boolean a_bNotRemovable)
	{
		int lockId = -1;
		boolean bChanged = false;

		if (a_certPath == null)
		{
			return lockId;
		}
		synchronized (m_trustedCertificates)
		{
			if (!m_trustedCertificates.containsKey(getCertificateId(a_certPath.getFirstCertificate(),
				a_certificateType)))
			{
				CertificateContainer newCertificateContainer = new CertificateContainer(a_certPath,
					a_certificateType, false);
				m_trustedCertificates.put(getCertificateId(a_certPath.getFirstCertificate(),
					a_certificateType), newCertificateContainer);
				if (a_certificateType == JAPCertificate.CERTIFICATE_TYPE_ROOT_MIX ||
					a_certificateType == JAPCertificate.CERTIFICATE_TYPE_ROOT_INFOSERVICE)
				{
					/* maybe with the new certificate we can activate some other certificates already stored
					 * in this certificate store
					 */
					activateAllDependentCertificates(a_certPath.getFirstCertificate());
				}
				/* we have added one certificate */
				bChanged = true;
			}
			if (!a_onlyHardRemovable)
			{
				/* add a lock to the lock table */
				lockId = getNextAvailableLockId();
				m_lockTable.put(new Integer(lockId), getCertificateId(a_certPath.getFirstCertificate(),
					a_certificateType));
				/* also add the new lock to the lock list of the certificate */
				( (CertificateContainer) (m_trustedCertificates.get(
								getCertificateId(a_certPath.getFirstCertificate(),
					a_certificateType)))).getLockList().
					addElement(new Integer(lockId));
			}
			else
			{
				/* enable the only hard removable flag */
				( (CertificateContainer) (m_trustedCertificates.get(getCertificateId(
								a_certPath.getFirstCertificate(),
								a_certificateType)))).enableOnlyHardRemovable();
			}
			if (a_bNotRemovable)
			{
				( (CertificateContainer) (m_trustedCertificates.get(
								getCertificateId(a_certPath.getFirstCertificate(),
					a_certificateType)))).enableNotRemovable();
			}
		}
		/* notify the observers, only meaningful, if setChanged() was called */
		if (bChanged)
		{
			setChanged();
			notifyObservers(new Integer(a_certificateType));
		}
		return lockId;
	}

	public synchronized void removeCertificateLock(int a_lockId)
	{
		synchronized (m_trustedCertificates)
		{
			CertificateContainer lockedCertificate = null;
			try
			{
				lockedCertificate = (CertificateContainer) (m_trustedCertificates.get(m_lockTable.get(new
					Integer(a_lockId))));
			}
			catch (Exception e)
			{
				/* can only happen, if a wrong lock id was specified, locks marked as invalid are not
				 * throwing this exception
				 */
				LogHolder.log(LogLevel.ERR, LogType.MISC,
							  "Error while removing certificate lock. There is no lock with ID " +
							  Integer.toString(a_lockId) + ".");
			}
			if (lockedCertificate != null)
			{
				/* it is not an invalid lock -> remove the lock from the certificate */
				lockedCertificate.getLockList().removeElement(new Integer(a_lockId));
				if (!lockedCertificate.isOnlyHardRemovable())
				{
					/* check whether it was the last lock on this certificate */
					if (lockedCertificate.getLockList().size() == 0)
					{
						/* no more locks -> we can remove the certificate */
						removeCertificate(lockedCertificate.getInfoStructure());
					}
				}
			}
			/* remove the lock from the lock table */
			m_lockTable.remove(new Integer(a_lockId));
		}
	}

	public synchronized void removeCertificate(CertificateInfoStructure a_certificateStructure)
	{
		CertificateContainer certificateToRemove = null;
		synchronized (m_trustedCertificates)
		{
			certificateToRemove = (CertificateContainer) (m_trustedCertificates.get(
				getCertificateId(a_certificateStructure.getCertificate(),
								 a_certificateStructure.getCertificateType())));
			if (certificateToRemove != null)
			{
				/* the hashtable contains the specified certificate */
				if (certificateToRemove.getCertificateType() == JAPCertificate.CERTIFICATE_TYPE_ROOT_MIX ||
					certificateToRemove.getCertificateType() ==
					JAPCertificate.CERTIFICATE_TYPE_ROOT_INFOSERVICE)
				{
					/* deactivate all certificates which depend on the specified certificate */
					deactivateAllDependentCertificates(certificateToRemove.getCertificate());
				}
				/* mark all active locks on that certificate as invalid (set the value to an empty string) */
				Enumeration activeLocks = certificateToRemove.getLockList().elements();
				while (activeLocks.hasMoreElements())
				{
					m_lockTable.put(activeLocks.nextElement(), "");
				}
				/* remove the specified certificate */
				m_trustedCertificates.remove(getCertificateId(a_certificateStructure.getCertificate(),
					a_certificateStructure.getCertificateType()));
			}
		}
		if (certificateToRemove != null)
		{
			/* we have removed one certificate -> notify the observers */
			setChanged();
			notifyObservers(new Integer(certificateToRemove.getCertificateType()));
		}
	}

	/** Removes all but the not removable certs from the store*/
	public void removeAllCertificates()
	{
		synchronized (m_trustedCertificates)
		{
			/* mark all active locks as invalid (set the value to an empty string) */
			Enumeration activeLocks = m_lockTable.keys();
			while (activeLocks.hasMoreElements())
			{
				m_lockTable.put(activeLocks.nextElement(), "");
			}
			if (m_trustedCertificates.size() > 0)
			{
				/* remove all certificates */
				Enumeration it = m_trustedCertificates.keys();
				while (it.hasMoreElements())
				{
					Object key = it.nextElement();
					CertificateContainer certcontainer = (CertificateContainer) m_trustedCertificates.get(key);
					if (!certcontainer.isNotRemovable())
					{
						m_trustedCertificates.remove(key);
					}
				}
				/* we have removed some certificates */
				setChanged();
			}
		}
		/* notify the observers, only meaningful, if setChanged() was called */
		notifyObservers();
	}
	
	public synchronized void reset()
	{
		synchronized (m_trustedCertificates)
		{
			CertificateContainer container;
			Enumeration certs = m_trustedCertificates.elements();
			while (certs.hasMoreElements())
			{
				container = (CertificateContainer)certs.nextElement();
				container.setEnabled(container.isEnabled());
			}
		}
	}

	public synchronized void setEnabled(CertificateInfoStructure a_certificateStructure, boolean a_enabled)
	{
		synchronized (m_trustedCertificates)
		{
			CertificateContainer specifiedCertificate = (CertificateContainer) (m_trustedCertificates.get(
				getCertificateId(a_certificateStructure.getCertificate(),
								 a_certificateStructure.getCertificateType())));
			if (specifiedCertificate != null)
			{
				/* the certificate exists -> change the state if necessary */
				if (specifiedCertificate.isEnabled() != a_enabled)
				{
					/* we have to change the state */
					specifiedCertificate.setEnabled(a_enabled);
					if (specifiedCertificate.getCertificateType() == JAPCertificate.CERTIFICATE_TYPE_ROOT_MIX ||
						specifiedCertificate.getCertificateType() ==
						JAPCertificate.CERTIFICATE_TYPE_ROOT_INFOSERVICE)
					{
						/* update the dependent certificates */
						if (a_enabled)
						{
							activateAllDependentCertificates(specifiedCertificate.getCertificate());
						}
						else
						{
							deactivateAllDependentCertificates(specifiedCertificate.getCertificate());
						}
					}
					/* we have changed the state of a certificate */
					setChanged();
				}
			}
			/* notify the observers, only meaningful, if setChanged() was called */
		}
		notifyObservers();
	}

	public Element toXmlElement(Document a_doc)
	{
		Element trustedCertificatesNode = a_doc.createElement(XML_ELEMENT_NAME);
		synchronized (m_trustedCertificates)
		{
			Enumeration allCertificates = m_trustedCertificates.elements();
			while (allCertificates.hasMoreElements())
			{
				CertificateContainer currentCertificateContainer = (CertificateContainer) (allCertificates.
					nextElement());
				if (currentCertificateContainer.isOnlyHardRemovable() &&
					!currentCertificateContainer.isNotRemovable())
				{
					trustedCertificatesNode.appendChild(currentCertificateContainer.toXmlElement(a_doc));
				}
			}
		}
		return trustedCertificatesNode;
	}

	public void loadSettingsFromXml(Element a_trustedCertificatesNode)
	{
		synchronized (m_trustedCertificates)
		{
			/* first remove all already stored certificates */
			removeAllCertificates();
			/* load the settings from the XML description */
			NodeList certificateContainerNodes = a_trustedCertificatesNode.getElementsByTagName(
				CertificateContainer.XML_ELEMENT_NAME);
			for (int i = 0; i < certificateContainerNodes.getLength(); i++)
			{
				Element certificateContainerNode = (Element) (certificateContainerNodes.item(i));
				try
				{
					CertificateContainer currentCertificateContainer = new CertificateContainer(
						certificateContainerNode);
					/* add the certificate to the list of trusted certificates */
					if (currentCertificateContainer.getCertificateNeedsVerification())
					{
						addCertificateWithVerification(currentCertificateContainer.getCertPath(),
							currentCertificateContainer.getCertificateType(), true);
					}
					else
					{
						addCertificateWithoutVerification(currentCertificateContainer.getCertPath(),
							currentCertificateContainer.getCertificateType(), true,
							currentCertificateContainer.isNotRemovable());
					}
					/* enable or disable the certificate */
					setEnabled(currentCertificateContainer.getInfoStructure(),
							   currentCertificateContainer.isEnabled());
				}
				catch (Exception e)
				{
					LogHolder.log(LogLevel.ERR, LogType.MISC,
								  "Error while loading a CertificateContainer. Skipping this entry. Error: " +
								  e.toString() + " - Invalid container was: " +
								  XMLUtil.toString(certificateContainerNode));
				}
			}
		}
	}

	private void activateAllDependentCertificates(JAPCertificate a_certificate)
	{
		synchronized (m_trustedCertificates)
		{
			Enumeration allCertificates = m_trustedCertificates.elements();
			while (allCertificates.hasMoreElements())
			{
				CertificateContainer currentCertificateContainer = (CertificateContainer) (allCertificates.
					nextElement());
				if (currentCertificateContainer.getCertificateNeedsVerification())
				{
					JAPCertificate parentCertificate = currentCertificateContainer.getParentCertificate();
					if (parentCertificate == null)
					{
						/* the current certificate needs verification but is not verified -> try to do it
						 * with the specified certificate
						 */
						if (currentCertificateContainer.getCertPath().isVerifier(a_certificate))
						{
							/* verification of the current certificate was successful */
							currentCertificateContainer.setParentCertificate(a_certificate);
							currentCertificateContainer.setEnabled(true);
						}
					}
				}
			}
		}
	}

	private void deactivateAllDependentCertificates(JAPCertificate a_certificate)
	{
		synchronized (m_trustedCertificates)
		{
			Enumeration allCertificates = m_trustedCertificates.elements();
			while (allCertificates.hasMoreElements())
			{
				CertificateContainer currentCertificateContainer = (CertificateContainer) (allCertificates.
					nextElement());
				if (currentCertificateContainer.getCertificateNeedsVerification())
				{
					JAPCertificate currentParentCertificate = currentCertificateContainer.
						getParentCertificate();
					if (currentParentCertificate != null)
					{
						if (currentParentCertificate.equals(a_certificate))
						{
							/* the current certificate depends on the specified certificate -> deactivate it */
							currentCertificateContainer.setParentCertificate(null);
							currentCertificateContainer.setEnabled(false);
						}
					}
				}
			}
		}
	}

	private int getNextAvailableLockId()
	{
		while (m_lockTable.containsKey(new Integer(m_lockIdPointer)) || (m_lockIdPointer == -1))
		{
			m_lockIdPointer++;
		}
		return m_lockIdPointer;
	}

	private String getCertificateId(JAPCertificate a_certificate, int a_certificateType)
	{
		return (a_certificate.getId() + Integer.toString(a_certificateType));
	}

}
