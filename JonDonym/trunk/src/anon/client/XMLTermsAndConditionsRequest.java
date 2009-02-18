/*
Copyright (c) 2008 The JAP-Team, JonDos GmbH

All rights reserved.

Redistribution and use in source and binary forms, with or without modification, 
are permitted provided that the following conditions are met:

    * Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
    * Redistributions in binary form must reproduce the above copyright notice,
       this list of conditions and the following disclaimer in the documentation and/or
       other materials provided with the distribution.
    * Neither the name of the University of Technology Dresden, Germany, nor the name of
       the JonDos GmbH, nor the names of their contributors may be used to endorse or
       promote products derived from this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
"AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE REGENTS OR
CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package anon.client;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import anon.util.IXMLEncodable;
import anon.util.XMLUtil;

/**
 * XML structure sent to the mix during login to request lacking TermsAndConditions items
 * which are required to be accepted before connecting to the corresponding cascade. 
 * @author Simon Pecher
 */
public class XMLTermsAndConditionsRequest implements IXMLEncodable 
{

	public static final String XML_ELEMENT_NAME = "Resources";
	public static final String XML_ELEMENT_CONTAINER_NAME = "TermsAndConditionsRequest";
	public final static String XML_ATTR_LOCALE = "locale";
	
	public final static String XML_ELEMENT_REQ_TRANSLATION = "Translation"; 
	public final static String XML_ELEMENT_RESOURCE_TEMPLATE = "Template"; 
	public final static String XML_ELEMENT_RESOURCE_CUSTOMIZED_SECT = "CustomizedSections"; 
	
	private Vector requestedTemplates = null;
	private Hashtable requestedItems = null;
	private Hashtable resourceRootRefs = null;
	
	public XMLTermsAndConditionsRequest()
	{
		requestedTemplates = new Vector();
		requestedItems = new Hashtable();
		resourceRootRefs = new Hashtable();
	}
	
	public void addTemplateRequest(String opSki, String langCode, String templateRefID)
	{
		if(!requestedTemplates.contains(templateRefID))
		{
			requestedTemplates.addElement(templateRefID);
			addResourceRequest(XML_ELEMENT_RESOURCE_TEMPLATE, opSki, langCode);
		}
	}
	
	public void addCustomizedSectionsRequest(String opSki, String langCode)
	{
		addResourceRequest(XML_ELEMENT_RESOURCE_CUSTOMIZED_SECT, opSki, langCode);
	}
	
	private void addResourceRequest(String resourceType, String opSki, String langCode)
	{
		TCRequestKey reqKey = new TCRequestKey(opSki, langCode);
		//DOMElementWrapper reqRoot = getResourceRootReference(opSki);
		
		TCRequestValue reqValue = (TCRequestValue) requestedItems.get(reqKey);
		if(reqValue == null)
		{
			reqValue = new TCRequestValue();
			requestedItems.put(reqKey, reqValue);
		}
		reqValue.addResourceRequest(resourceType);
	}
	
	public boolean hasResourceRequests()
	{
		return !requestedItems.isEmpty();
	}
	
	public Element toXmlElement(Document a_doc) 
	{
		
		Enumeration allReqs = requestedItems.keys();
		if(!allReqs.hasMoreElements())
		{
			return null;
		}
		
		Element requestRoot = a_doc.createElement(XML_ELEMENT_CONTAINER_NAME);
		
		a_doc.appendChild(requestRoot);
		Element currRequestElement = null;
		Element currTranslationElement = null;
		Enumeration currRequestItems = null;
		
		TCRequestKey opski = null;
		
		while (allReqs.hasMoreElements()) 
		{
			opski = (TCRequestKey) allReqs.nextElement();
			currRequestElement = (Element) resourceRootRefs.get(opski.getOpSki());
			
			if(currRequestElement == null)
			{
				currRequestElement = a_doc.createElement(XML_ELEMENT_NAME);
				XMLUtil.setAttribute(currRequestElement, XML_ATTR_ID, opski.getOpSki());
				resourceRootRefs.put(opski.getOpSki(), currRequestElement);
			}
		
			currRequestItems = ((TCRequestValue)requestedItems.get(opski)).getAllResourceRequests();
			if(currRequestItems.hasMoreElements())
			{
				currTranslationElement = a_doc.createElement(XML_ELEMENT_REQ_TRANSLATION);
				XMLUtil.setAttribute(currTranslationElement, XML_ATTR_LOCALE, opski.getLangCode());
				currRequestElement.appendChild(currTranslationElement);
			}
			while (currRequestItems.hasMoreElements()) 
			{
				currTranslationElement.appendChild(a_doc.createElement((String) currRequestItems.nextElement()));
			}
			requestRoot.appendChild(currRequestElement);
		}
		return requestRoot;
	}
	
	/**
	 * simple class to build a key with language and opSki
	 * to map a TC resource request 
	 */
	private static class TCRequestKey
	{
		String opSki = null;
		String langCode = null;
		
		private TCRequestKey(String opSki, String langCode)
		{
			this.opSki = opSki;
			this.langCode = langCode;
		}
		
		public String toString()
		{
			return (opSki+langCode);
		}
		
		public int hashCode()
		{
			return toString().hashCode();
		}
		
		public boolean equals(Object anotherKey)
		{
			return ((TCRequestKey)anotherKey).toString().equals(toString());
		}

		public String getLangCode() 
		{
			return langCode;
		}

		public String getOpSki() 
		{
			return opSki;
		}
	}
	
	/**
	 * a corresponding value wrapper class for the resource hashtable
	 */
	private static class TCRequestValue
	{
		Vector requestEntries;
		
		private TCRequestValue()
		{
			
			requestEntries = new Vector();
		}
		
		private void addResourceRequest(String resourceType)
		{
			if(!requestEntries.contains(resourceType))
			{
				requestEntries.addElement(resourceType);
			}
		}
		
		private Enumeration getAllResourceRequests()
		{
			return requestEntries.elements();
		}	
	}
}
