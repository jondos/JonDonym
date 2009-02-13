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
import java.util.Vector;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.sun.org.apache.xalan.internal.xsltc.runtime.Hashtable;

import anon.infoservice.Database;
import anon.infoservice.TermsAndConditions;
import anon.infoservice.TermsAndConditionsFramework;
import anon.util.IXMLEncodable;
import anon.util.XMLParseException;
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
	
	public final static String XML_ELEMENT_RESOURCE_TEMPLATE = "Template"; 
	public final static String XML_ELEMENT_RESOURCE_CUSTOMIZED_SECT = "CustomizedSections"; 
	
	private Vector requestedTemplates = null;
	private Hashtable requestedItems = null;
	
	public XMLTermsAndConditionsRequest()
	{
		requestedTemplates = new Vector();
		requestedItems = new Hashtable();
	}
	
	public void addTemplateRequest(String opski, String langCode, String templateRefID)
	{
		if(!requestedTemplates.contains(templateRefID))
		{
			requestedTemplates.addElement(templateRefID);
			RequestEntry entry = (RequestEntry) requestedItems.get(opski);
			if(entry == null)
			{
				entry = new RequestEntry(langCode);
				requestedItems.put(opski, entry);
			}
			entry.items.add(XML_ELEMENT_RESOURCE_TEMPLATE);
		}
	}
	
	public void addCustomizedSectionsRequest(String opski, String langCode)
	{
		RequestEntry entry = (RequestEntry) requestedItems.get(opski);
		if(entry == null)
		{
			entry = new RequestEntry(langCode);
			requestedItems.put(opski, entry);
		}
		entry.items.add(XML_ELEMENT_RESOURCE_CUSTOMIZED_SECT);
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
		RequestEntry currReqEntry = null;
		Enumeration currRequestItems = null;
		
		while (allReqs.hasMoreElements()) 
		{
			String opski = (String) allReqs.nextElement();
			currRequestElement = a_doc.createElement(XML_ELEMENT_NAME);
			currReqEntry = (RequestEntry) requestedItems.get(opski);
			XMLUtil.setAttribute(currRequestElement, XML_ATTR_ID, opski);
			XMLUtil.setAttribute(currRequestElement, XML_ATTR_LOCALE, currReqEntry.langCode);
			currRequestItems = currReqEntry.items.elements();
			while (currRequestItems.hasMoreElements()) 
			{
				currRequestElement.appendChild(a_doc.createElement((String) currRequestItems.nextElement()));
			}
			requestRoot.appendChild(currRequestElement);
		}
		return requestRoot;
	}
	
	private static class RequestEntry
	{
		String langCode = null;
		Vector items = null;
		
		private RequestEntry(String langCode)
		{
			this.langCode = langCode.trim().toLowerCase();
			items = new Vector();
		}
	}
}
