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

import org.w3c.dom.Element;
import org.w3c.dom.Node;

import anon.crypto.JAPCertificate;
import anon.crypto.X509DistinguishedName;
import anon.crypto.X509SubjectAlternativeName;
import anon.util.XMLUtil;
import java.util.Vector;
import anon.crypto.AbstractX509Extension;
import java.net.URL;

/**
 * Holds the information about the operator of a service.
 */
public class ServiceOperator
{
	private static final String XML_ELEM_EMAIL = "EMail";

	/**
	 * This is the name of the operator or organisation.
	 */
	private String organisation;

	/**
	 * This is the URL of the operators homepage.
	 */
	private String url;

	private String m_strEmail;

	/**
	 * Creates a new ServiceOperator from XML description (Operator node).
	 *
	 * @param operatorNode The Operator node from an XML document.
	 */
	public ServiceOperator(Node operatorNode, JAPCertificate operatorCertificate)
	{
		Node node;
		X509DistinguishedName subject;

	    if(operatorCertificate != null)
		{
			subject = operatorCertificate.getSubject();
			/* get the organisation name */
			organisation = subject.getOrganisation();
			if(organisation == null || organisation.trim().length() == 0)
			{
				// if no organisation is given, use the common name
				organisation = subject.getCommonName();
			}

			/* get the e-mail adress */
			m_strEmail = subject.getE_EmailAddress();
			if(m_strEmail == null || m_strEmail.trim().length() == 0)
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
								url = new URL(values.elementAt(i).toString()).toString();
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

		/* check if the the information from the cert is valid (not null oder empty)
		 * and take the information from the XML-Structure if not
		 */
		if(organisation == null || organisation.trim().length() == 0)
		{
			node = XMLUtil.getFirstChildByName(operatorNode, "Organisation");
		    organisation = XMLUtil.parseValue(node, null);
		}

		if(m_strEmail == null || m_strEmail.trim().length() == 0 ||
		   !X509SubjectAlternativeName.isValidEMail(m_strEmail))
		{
			node = XMLUtil.getFirstChildByName(operatorNode, XML_ELEM_EMAIL);
		    m_strEmail = XMLUtil.parseValue(node, null);
		}

	    /* get the homepage url */
		if (url == null)
		{
			node = XMLUtil.getFirstChildByName(operatorNode, "URL");
			url = XMLUtil.parseValue(node, null);
		}
	}

	public String getEMail()
	{
		return m_strEmail;
	}

	/**
	 * Returns the name of the operator or organisation.
	 *
	 * @return The name of the operator or organisation.
	 */
	public String getOrganisation()
	{
		return organisation;
	}

	/**
	 * Returns the URL of the operators homepage.
	 *
	 * @return The URL of the operators homepage.
	 */
	public String getUrl()
	{
		return url;
	}

}
