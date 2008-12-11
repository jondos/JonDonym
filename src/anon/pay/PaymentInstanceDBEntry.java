/*
 Copyright (c) 2000 - 2004, The JAP-Team
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

package anon.pay;

import java.util.Enumeration;
import java.util.Vector;
import java.util.Random;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import anon.crypto.JAPCertificate;
import anon.infoservice.AbstractDistributableDatabaseEntry;
import anon.infoservice.Constants;
import anon.infoservice.ListenerInterface;
import anon.infoservice.ServiceSoftware;
import anon.util.XMLParseException;
import anon.util.XMLUtil;
import anon.crypto.X509SubjectKeyIdentifier;
import anon.crypto.SignatureVerifier;
import anon.crypto.XMLSignature;
import anon.infoservice.AbstractDistributableCertifiedDatabaseEntry;
import java.util.Date;
import anon.crypto.CertPath;
import anon.crypto.IVerifyable;
import logging.LogHolder;
import logging.LogLevel;
import anon.crypto.SignatureCreator;
import logging.LogType;

/** Holds the information of a payment instance for storing in the InfoService.*/
public class PaymentInstanceDBEntry extends AbstractDistributableCertifiedDatabaseEntry
		implements IVerifyable
{
	public static final String XML_ELEMENT_NAME = "PaymentInstance";
	public static final String XML_ELEMENT_CONTAINER_NAME = "PaymentInstances";

	private static final String XML_ELEM_NAME = "Name";
	private static final String XML_ELEM_CERT = "Certificate";
	private static final String XML_ELEM_NET = "Network";


	/**
	 * This is the ID of this payment instance.
	 */
	private String m_strPaymentInstanceId;

	/**
	 * Stores the XML representation of this PaymentInstanceDBEntry.
	 */
	private Element m_xmlDescription;

	private XMLSignature m_signature;
	private CertPath m_certPath;

	/**
	 * Stores the time when this payment instance entry was created by the origin payment instance.
	 *  This value is used to determine the more recent
	 * payment instance entry, if two entries are compared (higher version number -> more recent entry).
	 */
	private long m_creationTimeStamp;

	private long m_serialNumber;

	private Vector m_listenerInterfaces;
	private String m_name;

	private JAPCertificate m_cert; //= JAPCertificate.getInstance("certificates/bi.cer");

	public PaymentInstanceDBEntry(Element elemRoot) throws XMLParseException
	{
		this(elemRoot, 0);
	}

	/** Creates a PaymentInstanceDBEntry which represents a payment instance.*/
	public PaymentInstanceDBEntry(Element elemRoot, long a_expireTime) throws XMLParseException
	{
		super(a_expireTime == 0 ?
			  System.currentTimeMillis() + Constants.TIMEOUT_PAYMENT_INSTANCE : a_expireTime);
		XMLUtil.assertNotNull(elemRoot);
		String name;

		/* store the XML representation */
		m_xmlDescription = elemRoot;

		name = XMLUtil.parseValue(XMLUtil.getFirstChildByName(elemRoot, XML_ELEM_NAME), null);
		if (name == null) // || !name.equals(m_name))
		{
			throw new XMLParseException(XML_ELEM_NAME);
		}

		m_signature = SignatureVerifier.getInstance().getVerifiedXml(elemRoot,
			SignatureVerifier.DOCUMENT_CLASS_PAYMENT);
		if (m_signature != null)
		{
			m_certPath = m_signature.getCertPath();
			if (m_certPath != null)
			{
				m_cert = m_certPath.getFirstCertificate();
			}
		}

		/* get the ID */
		m_strPaymentInstanceId = elemRoot.getAttribute(XML_ATTR_ID);
		if(!checkId() ) {
			throw new XMLParseException(elemRoot.getNodeName(),"Invalid Payment-Instance ID: " + m_strPaymentInstanceId );
		}
			
		m_name = XMLUtil.parseValue(XMLUtil.getFirstChildByName(elemRoot, XML_ELEM_NAME), "");

		/* get the creation timestamp */
		m_creationTimeStamp = XMLUtil.parseValue(XMLUtil.getFirstChildByName(elemRoot, XML_LAST_UPDATE), -1L);
		if (m_creationTimeStamp == -1)
		{
			throw new XMLParseException(XML_LAST_UPDATE);
		}

		m_serialNumber = XMLUtil.parseAttribute(elemRoot, XML_ATTR_SERIAL, m_creationTimeStamp);



		Node listenerInterfacesNode = XMLUtil.getFirstChildByName(
			  XMLUtil.getFirstChildByName(elemRoot, XML_ELEM_NET), ListenerInterface.XML_ELEMENT_CONTAINER_NAME);
		XMLUtil.assertNotNull(listenerInterfacesNode);


		NodeList listenerInterfaceNodes =
			((Element)listenerInterfacesNode).getElementsByTagName(ListenerInterface.XML_ELEMENT_NAME);
		if (listenerInterfaceNodes.getLength() == 0)
		{
			throw (new XMLParseException(ListenerInterface.XML_ELEMENT_NAME));
		}
		m_listenerInterfaces = new Vector();
		for (int i = 0; i < listenerInterfaceNodes.getLength(); i++)
		{
			m_listenerInterfaces.addElement(new ListenerInterface((Element) (listenerInterfaceNodes.item(i))));
		}
	}


	public PaymentInstanceDBEntry(String a_id, String a_name,
								  JAPCertificate a_cert, /** @todo remove this when new JAP available */
								  Enumeration a_listeners,
								  String software_version, long creationTime, long a_serialNumber)
	{
		super(System.currentTimeMillis() + Constants.TIMEOUT_PAYMENT_INSTANCE);
		m_strPaymentInstanceId = a_id;
		m_creationTimeStamp = creationTime;
		m_serialNumber = a_serialNumber;
		m_cert = a_cert;
		m_name = a_name;

		Document doc = XMLUtil.createDocument();
		Element elemRoot = doc.createElement(XML_ELEMENT_NAME);
		doc.appendChild(elemRoot);
		XMLUtil.setAttribute(elemRoot, XML_ATTR_ID, m_strPaymentInstanceId);
		XMLUtil.setAttribute(elemRoot, XML_ATTR_SERIAL, m_serialNumber);

		Element elemName = doc.createElement(XML_ELEM_NAME);
		XMLUtil.setValue(elemName, m_name);

		elemRoot.appendChild(elemName);
		ServiceSoftware software = new ServiceSoftware(software_version);
		elemRoot.appendChild(software.toXmlElement(doc));
		Element elemNet = doc.createElement(XML_ELEM_NET);
		elemRoot.appendChild(elemNet);
		Element elemListeners = doc.createElement(ListenerInterface.XML_ELEMENT_CONTAINER_NAME);
		elemNet.appendChild(elemListeners);
		while (a_listeners.hasMoreElements())
		{
			ListenerInterface li = (ListenerInterface) a_listeners.nextElement();
			elemListeners.appendChild(li.toXmlElement(doc));
		}
		Element elemLastUpdate = doc.createElement(XML_LAST_UPDATE);
		XMLUtil.setValue(elemLastUpdate, m_creationTimeStamp);
		elemRoot.appendChild(elemLastUpdate);
	    if (m_cert != null)
		{
			Element elemCert = doc.createElement(XML_ELEM_CERT);
			elemRoot.appendChild(elemCert);
			elemCert.appendChild(m_cert.toXmlElement(doc));

			m_signature = SignatureCreator.getInstance().getSignedXml(
				SignatureVerifier.DOCUMENT_CLASS_PAYMENT, elemRoot);
			if (m_signature != null)
			{
				m_certPath = m_signature.getCertPath();
			}

			if (m_certPath == null || m_certPath.getFirstCertificate() == null)
			{
				LogHolder.log(LogLevel.EXCEPTION, LogType.MISC, "Document could not be signed!");
			}
			else
			{
				m_cert = m_certPath.getFirstCertificate();
			}
		}
		else if (m_certPath != null)
		{
			m_cert = m_certPath.getFirstCertificate();
		}

		m_xmlDescription = elemRoot;
	}

	public boolean isVerified()
	{
		if (m_signature != null)
		{
			return m_signature.isVerified();
		}
		return false;
	}

	public boolean isValid()
	{
		if (m_certPath != null)
		{
			return m_certPath.checkValidity(new Date());
		}
		return false;
	}

	public CertPath getCertPath()
	{
		return m_certPath;
	}

	public String toString()
	{
		return getName();
	}

	public String getId()
	{
		return m_strPaymentInstanceId;
	}
	
	public boolean equals(Object a_paymentInstance)
	{
		PaymentInstanceDBEntry paymentInstance;
		
		if (!(a_paymentInstance instanceof PaymentInstanceDBEntry) || a_paymentInstance == null)
		{
			return false;
		}
		
		paymentInstance = (PaymentInstanceDBEntry)a_paymentInstance;
		
		if (paymentInstance.getId() == getId() || paymentInstance.getId().equals(getId()))
		{
			return true;
		}
		
		return false;
	}
	
	public int hashCode()
	{
		if (m_strPaymentInstanceId == null)
		{
			return 0;
		}
		return m_strPaymentInstanceId.hashCode();
	}
	

	public String getName()
	{
		return m_name;
	}

	/**
	 * Returns the listener interfaces of this PI in a random order.
	 * @return the listener interfaces of this PI in a random order
	 */
	public Enumeration getListenerInterfaces()
	{
		Random random = new Random();
		int currentIndex;
		Vector interfaces = (Vector)m_listenerInterfaces.clone();
		Vector interfacesReturned = new Vector();
		while (interfaces.size() > 0)
		{
			currentIndex = Math.abs(random.nextInt() % interfaces.size());
			interfacesReturned.addElement(interfaces.elementAt(currentIndex));
			interfaces.removeElementAt(currentIndex);
		}

		return interfacesReturned.elements();
	}

	public JAPCertificate getCertificate()
	{
		return m_cert;
	}

	/**
	 * Returns the time when this payment instance entry was created by the origin payment instance.
	 *
	 * @return A version number which is used to determine the more recent payment instance entry, if two
	 *         entries are compared (higher version number -> more recent entry).
	 */
	public long getVersionNumber()
	{
		return m_serialNumber;
	}

	public long getLastUpdate()
	{
		return m_creationTimeStamp;
	}

	/**
	 * This returns the filename (InfoService command), where this PaymentInstanceDBEntry is posted at
	 * other InfoServices. It's always '/paymentinstance'.
	 *
	 * @return The filename where the information about this PaymentInstanceDBEntry is posted at other
	 *         InfoServices when this entry is forwarded.
	 */
	public String getPostFile()
	{
		return "/paymentinstance";
	}

	public Element getXmlStructure()
	{
		return m_xmlDescription;
	}



}
