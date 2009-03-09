/*
 Copyright (c) 2000 - 2003, The JAP-Team
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
package anon.infoservice;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import anon.crypto.CertPath;
import anon.crypto.JAPCertificate;
import anon.crypto.MultiCertPath;
import anon.crypto.X509DistinguishedName;
import anon.crypto.X509SubjectAlternativeName;
import anon.util.Util;
import anon.util.XMLUtil;

import java.util.Enumeration;
import java.util.Vector;
import anon.crypto.AbstractX509Extension;
import java.net.URL;

import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;

/**
 * Holds the information about the operator of a service.
 */
public class ServiceOperator extends AbstractDatabaseEntry
{
	public static final String XML_ELEMENT_NAME = "Operator";
	public static final String XML_ELEMENT_ORGANISATION = "Organisation";
	public static final String XML_ELEMENT_COUNTRYCODE = "CountryCode";
	public static final String XML_ELEMENT_URL = "URL"; 
	public static final String XML_ELEMENT_ORG_UNIT = "OrganisationalUnit";
	public static final String XML_ELEMENT_EMAIL = "EMail";
	public static final String XML_ELEMENT_EMAIL_SPAMSAFE = "Liame";
	
	private static final String AT_SUBSTITUTE = "([at]";
	private static final String DOT_SUBSTITUTE = "([dot]";
	private static final boolean SPAM_SAFE = true;
	
	/**
	 * This is the name of the operator or organization.
	 */
	private String m_strOrganization;

	private String m_strOrgUnit;

	/**
	 * This is the URL of the operators home page.
	 */
	private String m_strUrl;

	/**
	 * This is the EMail address of the operator.
	 */
	private String m_strEmail;
	
	private String m_countryCode;

	/**
	 * The last update time.
	 */
	private long m_lastUpdate;
	
	private String m_strID;

	/**
	 * The operators certificate
	 */
	private MultiCertPath m_certPath;

	/** operator address information as a certificate independent extension */ 
	private OperatorAddress address;
	
	/**
	 * Creates a ServiceOperator just by his Certificate
	 * @param operatorCertificate the opeartors certificate
	 */
	public ServiceOperator(JAPCertificate operatorCertificate)
	{
		
		super(Long.MAX_VALUE);
		X509DistinguishedName subject = operatorCertificate.getSubject();
		m_certPath = null;
		/* get the organization name */
		m_strOrganization = subject.getOrganisation();
		if(m_strOrganization == null || m_strOrganization.trim().length() == 0)
		{
			// if no organization is given, use the common name
			m_strOrganization = subject.getCommonName();
		}
		m_countryCode = subject.getCountryCode();
		m_strOrgUnit = subject.getOrganisationalUnit();
			
		/* get the e-mail address */
		m_strEmail = subject.getE_EmailAddress();
		if (m_strEmail == null || m_strEmail.trim().length() == 0)
		{
		   m_strEmail = subject.getEmailAddress();
		}
			
		// get the URL
		AbstractX509Extension extension =
			operatorCertificate.getExtensions().getExtension(X509SubjectAlternativeName.IDENTIFIER);
		if (extension != null && extension instanceof X509SubjectAlternativeName)
		{
			X509SubjectAlternativeName alternativeName = (X509SubjectAlternativeName) extension;
			Vector tags = alternativeName.getTags();
			Vector values = alternativeName.getValues();
			if (tags.size() == values.size())
			{
				for (int i = 0; i < tags.size(); i++)
				{
					if (tags.elementAt(i).equals(X509SubjectAlternativeName.TAG_URL))
					{
						try
						{
							m_strUrl = new URL(values.elementAt(i).toString()).toString();
						}
						catch (Exception a_e)
						{
							// ignore
						}
						break;
					}
				}
			}
		}
		m_strID = operatorCertificate.getSubjectKeyIdentifierConcatenated();			
	    if (m_strID == null)
		{
			LogHolder.log(LogLevel.ALERT, LogType.DB, "Could not create ID for ServiceOperator entry!");
			m_strID = "";
		}
	    address = null;
	}
	
	/**
	 * Creates a new ServiceOperator an operator certificate or
	 * from an XML description (Operator node).
	 *
	 * @param a_node The operator node from an XML document.
	 * @param a_certificate The operator certificate
	 * @param a_lastUpdate Last update time.
	 */
	public ServiceOperator(Node a_node, MultiCertPath a_certPath, long a_lastUpdate)
	{
		super(Long.MAX_VALUE);

		X509DistinguishedName subject;
		CertPath path;

		m_certPath = a_certPath;
		m_lastUpdate = a_lastUpdate;

	    if (m_certPath != null && (path = m_certPath.getPath()) != null &&
	    	path.getSecondCertificate() != null	)
		{
			subject = path.getSecondCertificate().getSubject();
			/* get the organization name */
			m_strOrganization = subject.getOrganisation();
			if(m_strOrganization == null || m_strOrganization.trim().length() == 0)
			{
				// if no organization is given, use the common name
				m_strOrganization = subject.getCommonName();
			}
			m_countryCode = subject.getCountryCode();
			m_strOrgUnit = subject.getOrganisationalUnit();
			
			/* get the e-mail address */
			m_strEmail = subject.getE_EmailAddress();
			if (m_strEmail == null || m_strEmail.trim().length() == 0)
			{
			   m_strEmail = subject.getEmailAddress();
			}
			
			// get the URL
			AbstractX509Extension extension =
				path.getSecondCertificate().getExtensions().getExtension(X509SubjectAlternativeName.IDENTIFIER);
			if (extension != null && extension instanceof X509SubjectAlternativeName)
			{
				X509SubjectAlternativeName alternativeName = (X509SubjectAlternativeName) extension;
				Vector tags = alternativeName.getTags();
				Vector values = alternativeName.getValues();
				if (tags.size() == values.size())
				{
					for (int i = 0; i < tags.size(); i++)
					{
						if (tags.elementAt(i).equals(X509SubjectAlternativeName.TAG_URL))
						{
							try
							{
								m_strUrl = new URL(values.elementAt(i).toString()).toString();
							}
							catch (Exception a_e)
							{
								// ignore
							}
							break;
						}
					}
				}
			}
			
			/** Create ID */
			Vector vecCertPaths = m_certPath.getPaths();
			Vector vecCertificates = new Vector();
			JAPCertificate certTemp;
			for (int i = 0; i < vecCertPaths.size(); i++)
			{
				certTemp = ((CertPath)vecCertPaths.elementAt(i)).getSecondCertificate();
				if (certTemp != null)
				{
					vecCertificates.addElement(certTemp);
				}
			}

			m_strID = JAPCertificate.calculateXORofSKIs(vecCertificates);			
		}
	    
	    if (m_strID == null)
		{
			LogHolder.log(LogLevel.ALERT, LogType.DB, "Could not create ID for ServiceOperator entry!");
			m_strID = "";
		}
	}

	/**
	 * Returns version number which is used to determine the more recent infoservice entry, if two
	 * entries are compared (higher version number -> more recent entry).
	 *
	 * @return The version number for this entry.
	 */
	public long getVersionNumber()
	{
		return m_lastUpdate;
	}

	/**
	 * Returns the time in milliseconds when this db entry was created from the origin instance.
	 *
	 * @return the time in milliseconds when this db entry was created from the origin instance
	 */
	public long getLastUpdate()
	{
		return m_lastUpdate;
	}

	/**
	 * Returns a unique ID for a database entry.
	 *
	 * @return The ID of this database entry.
	 */
	public String getId()
	{
		return m_strID;
	}

	/**
	 * Return the EMail address of the operator.
	 *
	 * @return The EMail address of the operator.
	 */
	public String getEMail()
	{
		return m_strEmail;
	}
	
	
	public String getEMailSpamSafe()
	{
		if(m_strEmail != null)
		{
			m_strEmail = Util.replaceAll(m_strEmail, "@", AT_SUBSTITUTE);
			m_strEmail = Util.replaceAll(m_strEmail, ".", DOT_SUBSTITUTE);	
		}
		return m_strEmail;
	}

	/**
	 * Returns the name of the operator or organization.
	 *
	 * @return The name of the operator or organization.
	 */
	public String getOrganization()
	{
		return m_strOrganization;
	}
	
	public String getOrganizationUnit() 
	{
		return m_strOrgUnit;
	}

	public MultiCertPath getCertPath()
	{
		return m_certPath;
	}
	
	/**
	 * Returns the operator certificate. 
	 * TODO return the first certificate that is found; instead, only the cert path should be used to
	 * get the operator data, as this may also return an unverified certificate...
	 * @return The operator certificate.
	 */
	public JAPCertificate getCertificate()
	{
		JAPCertificate cert;
		if (m_certPath == null || m_certPath.getPath() == null || 
			(cert = m_certPath.getPath().getSecondCertificate()) == null)
		{
			return null;
		}
		return cert;
	}

	/**
	 * Returns the URL of the operators home page.
	 *
	 * @return The URL of the operators home page.
	 */
	public String getUrl()
	{
		return m_strUrl;
	}
	
	public String getCountryCode() 
	{
		return m_countryCode;
	}

	public Element toXMLElement(Document ownerDocument)
	{
		return toXMLElement(ownerDocument, SPAM_SAFE);
	}
	
	public boolean hasTermsAndConditions()
	{
		return TermsAndConditions.getTermsAndConditions(this) != null;
	}
	
	
	/* creates a DOM-Tree with the data which will be owned by
	 * ownerDocument but not appended to it.
	 * if spamSafe is true than the Email-Tag as well as the content are 
	 * modified in a way to make it harder for Spam-parsers to evaluate the
	 * email-address. 
	 */
	public Element toXMLElement(Document ownerDocument, boolean spamSafe)
	{
		if(ownerDocument == null)
		{
			return null;
		}
		Element mixOperatorElement = ownerDocument.createElement(XML_ELEMENT_NAME);
		
		if( m_strOrganization != null )
		{
			XMLUtil.createChildElementWithValue(mixOperatorElement, 
					XML_ELEMENT_ORGANISATION, 
					Util.filterXMLChars(m_strOrganization));
		}
		if( m_strUrl != null )
		{
			XMLUtil.createChildElementWithValue(mixOperatorElement, 
					XML_ELEMENT_URL, 
					Util.filterXMLChars(m_strUrl));
		}
		if( m_countryCode != null )
		{
			XMLUtil.createChildElementWithValue(mixOperatorElement, 
					XML_ELEMENT_COUNTRYCODE, 
					Util.filterXMLChars(m_countryCode));
		}
		if( m_strOrgUnit != null )
		{
			XMLUtil.createChildElementWithValue(mixOperatorElement, 
					XML_ELEMENT_ORG_UNIT, 
					Util.filterXMLChars(m_strOrgUnit));
		}
		if( m_strEmail != null )
		{
			XMLUtil.createChildElementWithValue(mixOperatorElement, 
					spamSafe ? XML_ELEMENT_EMAIL_SPAMSAFE : XML_ELEMENT_EMAIL, 
					spamSafe ? Util.filterXMLChars(getEMailSpamSafe()) : Util.filterXMLChars(getEMail()));
		}
		
		if (address != null)
		{
			Enumeration e = address.getAddressAsNodeList(ownerDocument);
			while (e.hasMoreElements()) 
			{
				mixOperatorElement.appendChild((Element) e.nextElement());
			}
		}
		
		return mixOperatorElement;
	}
	
	public int hashCode()
	{
		return getId().hashCode();
	}
	
	public boolean equals(Object a_obj)
	{
		if(a_obj == null || !(a_obj instanceof ServiceOperator)) return false;

		ServiceOperator op = (ServiceOperator) a_obj;
		return getId().equals(op.getId());
	}

	public OperatorAddress getAddress() 
	{
		return address;
	}

	public void setAddress(OperatorAddress address) 
	{
		this.address = address;
	}
}
