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

import org.w3c.dom.Node;

import anon.crypto.JAPCertificate;
import anon.crypto.X509DistinguishedName;
import anon.crypto.X509SubjectAlternativeName;
import anon.crypto.X509SubjectKeyIdentifier;
import anon.util.XMLUtil;
import java.util.Vector;
import anon.crypto.AbstractX509Extension;
import java.net.URL;

/**
 * Holds the information about the operator of a service.
 */
public class ServiceOperator extends AbstractDatabaseEntry
{
	private static final String XML_ELEM_EMAIL = "EMail";
	
	/**
	 * This is the name of the operator or organization.
	 */
	private String m_strOrganization;

	/**
	 * This is the URL of the operators home page.
	 */
	private String m_strUrl;

	/**
	 * This is the EMail address of the operator.
	 */
	private String m_strEmail;
	
	/**
	 * The last update time.
	 */
	private long m_lastUpdate;

	/**
	 * The operators certificate
	 */
	private JAPCertificate m_certificate;
	
	/**
	 * The XML data.
	 */
	private Node m_node;

	/**
	 * Creates a new ServiceOperator an operator certificate or
	 * from an XML description (Operator node).
	 *
	 * @param a_node The operator node from an XML document.
	 * @param a_certificate The operator certificate
	 * @param a_lastUpdate Last update time.
	 */
	public ServiceOperator(Node a_node, JAPCertificate a_certificate, long a_lastUpdate)
	{
		super(Long.MAX_VALUE);

		Node node;
		X509DistinguishedName subject;

		m_node = a_node;
		m_certificate = a_certificate;
		m_lastUpdate = a_lastUpdate;

	    if(a_certificate != null)
		{
			subject = a_certificate.getSubject();
			/* get the organization name */
			m_strOrganization = subject.getOrganisation();
			if(m_strOrganization == null || m_strOrganization.trim().length() == 0)
			{
				// if no organization is given, use the common name
				m_strOrganization = subject.getCommonName();
			}

			/* get the e-mail address */
			m_strEmail = subject.getE_EmailAddress();
			if(m_strEmail == null || m_strEmail.trim().length() == 0)
			{
			   m_strEmail = subject.getEmailAddress();
			}

			// get the URL
			AbstractX509Extension extension =
				a_certificate.getExtensions().getExtension(X509SubjectAlternativeName.IDENTIFIER);
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
		}

	    /**
	     * @todo remove - Backwards compatibility only
	     *
	     * older mixes seem to send no operator certificate
	     *
	     * check if the the information from the cert is valid (not null oder empty)
		 * and take the information from the XML-Structure if not
	     */
		if(m_strOrganization == null || m_strOrganization.trim().length() == 0)
		{
			node = XMLUtil.getFirstChildByName(a_node, "Organisation");
		    m_strOrganization = XMLUtil.parseValue(node, null);
		}

		if(m_strEmail == null || m_strEmail.trim().length() == 0 ||
		   !X509SubjectAlternativeName.isValidEMail(m_strEmail))
		{
			node = XMLUtil.getFirstChildByName(a_node, XML_ELEM_EMAIL);
		    m_strEmail = XMLUtil.parseValue(node, null);
		}
		if (m_strUrl == null)
		{
			node = XMLUtil.getFirstChildByName(a_node, "URL");
			m_strUrl = XMLUtil.parseValue(node, null);
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
		if(m_certificate != null)
		{
			X509SubjectKeyIdentifier id = ((X509SubjectKeyIdentifier)m_certificate.getExtensions().getExtension(X509SubjectKeyIdentifier.IDENTIFIER));
			return id != null ? id.getValue() : m_certificate.getId();
		}

		return "";
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

	/**
	 * Returns the name of the operator or organization.
	 *
	 * @return The name of the operator or organization.
	 */
	public String getOrganization()
	{
		return m_strOrganization;
	}

	/**
	 * Returns the operator certificate.
	 *
	 * @return The operator certificate.
	 */
	public JAPCertificate getCertificate()
	{
		return m_certificate;
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
	
	/**
	 * Returns the XML data.
	 */
	public Node getXML()
	{
		return m_node;
	}
	
	public boolean equals(Object a_obj)
	{
		if(a_obj == null || m_certificate == null) return false;

		ServiceOperator op = (ServiceOperator) a_obj;
		return m_certificate.equals(op.m_certificate);
	}
}
