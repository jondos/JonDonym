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

import anon.infoservice.ServiceOperator;
import anon.infoservice.TermsAndConditions;
import anon.infoservice.TermsAndConditionsFramework;
import anon.util.IXMLEncodable;
import anon.util.XMLUtil;

/**
 * XML structure sent to the mix during login to request lacking TermsAndConditions items
 * which are required to be accepted before connecting to the corresponding cascade.
 * 
 *  Resource requests are added by the corresponding add-methods
 *  An example for a resulting XML request document:
 *  
 *  <TermsAndConditionsRequest>
 *		<Resources id="(operator subject key identifier)">
 *			<Translation locale="en">
 *				<CustomizedSections/>
 *				<Template/>
 *			</Translation>
 *			<Translation locale="de">
 *				<CustomizedSections/>
 *			</Translation>
 *			...
 *		</Resources>
 *		<Resources id="(operator subject key identifier)">
 *			<Translation locale="de">
 *				<CustomizedSections/>
 *				<Template/>
 *			</Translation>
 *			...
 *		</Resources>
 *	</TermsAndConditionsRequest>
 */
public class TermsAndConditionsRequest implements IXMLEncodable 
{

	public static final String XML_ELEMENT_NAME = "Resources";
	public static final String XML_ELEMENT_CONTAINER_NAME = "TermsAndConditionsRequest";
	public final static String XML_ATTR_LOCALE = "locale";
	
	public final static String XML_ELEMENT_REQ_TRANSLATION = "Translation"; 
	public final static String XML_ELEMENT_RESOURCE_TEMPLATE = "Template"; 
	public final static String XML_ELEMENT_RESOURCE_CUSTOMIZED_SECT = "CustomizedSections"; 
	
	public final static String XML_MSG_TC_INTERRUPT = "TermsAndConditionsInterrupt";
	public final static String XML_MSG_TC_CONFIRM = "TermsAndConditionsConfirm";
	
	/** to remember which templates are already requested. Needed to 
	 * ensure that the same template is only requested once.
	 */
	private Vector requestedTemplates = null;
	/**
	 * stores the requested T&C resources. the key that maps to the entries
	 * is built by the operator subject key identifier and the language.
	 */
	private Hashtable requestedItems = null;
	/**
	 * stores the resource root elements for the operators.
	 * needed when the XML document is created.
	 */
	private Hashtable resourceRootElements = null;
	
	public TermsAndConditionsRequest()
	{
		requestedTemplates = new Vector();
		requestedItems = new Hashtable();
		resourceRootElements = new Hashtable();
	}
	
	/**
	 * adds a template request for the given operator and language
	 * the templateRefid needs to be specified to avoid 
	 * multiple requests of the same template. (the same template can be used 
	 * by multiple operators)
	 */
	public void addTemplateRequest(ServiceOperator operator, String langCode, String templateRefID)
	{
		if(!requestedTemplates.contains(templateRefID))
		{
			requestedTemplates.addElement(templateRefID);
			addResourceRequest(XML_ELEMENT_RESOURCE_TEMPLATE, operator, langCode);
		}
	}
	
	/**
	 * adds a request for the individual T&C sections of the given operator in the
	 * the specified language.
	 */
	public void addCustomizedSectionsRequest(ServiceOperator operator, String langCode)
	{
		addResourceRequest(XML_ELEMENT_RESOURCE_CUSTOMIZED_SECT, operator, langCode);
	}
	
	/**
	 * private util function for adding a generic resource request.
	 */
	private void addResourceRequest(String resourceType, ServiceOperator operator, String langCode)
	{
		TCRequestKey reqKey = new TCRequestKey(operator, langCode);
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
		
		TCRequestKey currTCReqKey = null;
		
		while (allReqs.hasMoreElements()) 
		{
			currTCReqKey = (TCRequestKey) allReqs.nextElement();
			currRequestElement = (Element) resourceRootElements.get(currTCReqKey.getOperator());
			
			if(currRequestElement == null)
			{
				currRequestElement = a_doc.createElement(XML_ELEMENT_NAME);
				XMLUtil.setAttribute(currRequestElement, XML_ATTR_ID, currTCReqKey.getOperator().getId());
				resourceRootElements.put(currTCReqKey.getOperator(), currRequestElement);
			}
		
			currRequestItems = ((TCRequestValue)requestedItems.get(currTCReqKey)).getAllResourceRequests();
			if(currRequestItems.hasMoreElements())
			{
				currTranslationElement = a_doc.createElement(XML_ELEMENT_REQ_TRANSLATION);
				XMLUtil.setAttribute(currTranslationElement, XML_ATTR_LOCALE, currTCReqKey.getLangCode());
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
	 * simple class to build a key with language and 
	 * the operator subject key identifier.
	 * to map a TC resource request 
	 */
	private static class TCRequestKey
	{
		ServiceOperator operator = null;
		String langCode = null;
		
		private TCRequestKey(ServiceOperator operator, String langCode)
		{
			this.operator = operator;
			this.langCode = langCode;
		}
		
		public String toString()
		{
			return (operator.getId()+langCode);
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

		public ServiceOperator getOperator() 
		{
			return operator;
		}
	}
	
	/**
	 * To be called after the mix response was handled.
	 * checks if the mix really has sent all requested resources.
	 * This method performs the necessary cleanups when the the terms and conditions container are
	 * not in a consistent state. 
	 * @throws IllegalRequestPostConditionException if not all resources were sent
	 * or the container are in an inconsistent state (e.g because of invalid signatures)
	 */
	public void checkRequestPostCondition() throws IllegalTCRequestPostConditionException
	{
		IllegalTCRequestPostConditionException irpce = 
			new IllegalTCRequestPostConditionException();
		
		Enumeration allResourceRequests = requestedItems.keys();
		TCRequestKey currentReqKey = null;
		TermsAndConditions currentTnCs = null;
		while (allResourceRequests.hasMoreElements()) 
		{
			currentReqKey = (TCRequestKey) allResourceRequests.nextElement();
			currentTnCs = TermsAndConditions.getTermsAndConditions(currentReqKey.getOperator());
			
			if(currentTnCs != null)
			{
				if(!currentTnCs.hasTranslation(currentReqKey.getLangCode()))
				{
					irpce.addErrorMessage("Requested Translation ["+currentReqKey.getLangCode()+"] was not loaded for terms and conditions of operator "+
							currentReqKey.getOperator().getOrganization());
				}
				else
				{
					String templateRefid = currentTnCs.getTemplateReferenceId(currentReqKey.getLangCode());
					if(TermsAndConditionsFramework.getById(templateRefid, false) == null)
					{
						irpce.addErrorMessage("Template '"+templateRefid+"' for translation ["+
								currentReqKey.getLangCode()+"] of terms and conditions of operator "+
								currentReqKey.getOperator().getOrganization()+" was not loaded.");
					}
				}
				if(!currentTnCs.hasDefaultTranslation())
				{
					irpce.addErrorMessage("No default translation for terms and conditions of operator "+
							currentReqKey.getOperator().getOrganization()+" were loaded.");
					//in this case remove the terms and conditions
					TermsAndConditions.removeTermsAndConditions(currentReqKey.getOperator());
				}
			}
		}
		if(irpce.hasErrorMessages()) throw irpce;
	}
	
	/**
	 * a corresponding value wrapper class for the resource items hashtable
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
