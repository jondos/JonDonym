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

import java.io.IOException;
import java.security.SignatureException;
import java.util.Enumeration;
import java.util.Observable;
import java.util.Vector;

import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import anon.infoservice.Database;
import anon.infoservice.ServiceOperator;
import anon.infoservice.TermsAndConditions;
import anon.infoservice.TermsAndConditionsFramework;
import anon.util.XMLParseException;
import anon.util.XMLUtil;

/**
 * Extracts the requested resources the mix sends back
 * and stores them.
 * 
 * A mix tc response looks like this:
 * <TermsAndConditionsResponse>
 * 		<Resources id="(operator subject key identifier)" locale="de">
 * 			<CustomizedSections>
 * 				<TCTranslation locale="de" referenceId="(template_id)_(lang)_(date)">
 *					...
 *       		</TCTranslation>
 *			</CustomizedSections>
 *			<Template>
 *			...
 *			</Template>
 *		</Resources>
 *		...
 *	</TermsAndConditionsResponse>
 */
public class TermsAndConditionsResponseHandler extends Observable
{
	public static final String XML_ELEMENT_INVALID_REQUEST_NAME = "InvalidTermsAndConditionsRequest";
	public static final String XML_ELEMENT_RESPONSE_NAME = "TermsAndConditionsResponse";
	
	public void handleXMLResourceResponse(Document answerDoc, TermsAndConditionsRequest request) throws XMLParseException, IOException, IllegalTCRequestPostConditionException
	{
		if(answerDoc.getDocumentElement().getTagName().equals(XML_ELEMENT_INVALID_REQUEST_NAME))
		{
			throw new IOException("Error: Mix reported invalid TC request");
		}
		if(!answerDoc.getDocumentElement().getTagName().equals(XML_ELEMENT_RESPONSE_NAME))
		{
			throw new XMLParseException("No TC response.");
		}
		Node currentResourceParentNode = XMLUtil.getFirstChildByName(answerDoc.getDocumentElement(), 
				TermsAndConditionsRequest.XML_ELEMENT_NAME);
		Node currentResourceNode = null;
		String currentID = "";
		while(currentResourceParentNode != null)
		{
			currentID = XMLUtil.parseAttribute(currentResourceParentNode, TermsAndConditionsRequest.XML_ATTR_ID, "");
			if(currentID.equals("")  )
			{
				throw new XMLParseException("invalid attributes: id not set");
			}
			
			currentResourceNode = XMLUtil.getFirstChildByName(currentResourceParentNode, 
					TermsAndConditionsRequest.XML_ELEMENT_RESOURCE_TEMPLATE);
			while(currentResourceNode != null)
			{
				TermsAndConditionsFramework fr = new TermsAndConditionsFramework((Element)currentResourceNode.getFirstChild());
				Database db = Database.getInstance(TermsAndConditionsFramework.class);
				db.update(fr);
				currentResourceNode = (Element) XMLUtil.getNextSiblingByName(currentResourceNode, 
						TermsAndConditionsRequest.XML_ELEMENT_RESOURCE_TEMPLATE);
			}
			
			currentResourceNode = XMLUtil.getFirstChildByName(currentResourceParentNode, 
					TermsAndConditionsRequest.XML_ELEMENT_RESOURCE_CUSTOMIZED_SECT);
			while(currentResourceNode != null)
			{
				ServiceOperator operator = ((ServiceOperator) Database.getInstance(ServiceOperator.class).getEntryById(currentID.toUpperCase()));
				if(operator == null)
				{
					throw new XMLParseException("invalid id "+currentID+": no operator found with this subject key identifier");
				}
				
				TermsAndConditions tc = TermsAndConditions.getTermsAndConditions(operator);
				if(tc == null)
				{
					throw new IllegalStateException("a tc container for operator "+operator.getOrganization()+" must exist but does not!");
				}
				try 
				{
					tc.addTranslation(((Element) XMLUtil.getFirstChildByName(currentResourceNode, TermsAndConditions.XML_ELEMENT_TRANSLATION_NAME)));
				} 
				catch (SignatureException e) 
				{
					LogHolder.log(LogLevel.ERR, LogType.MISC, "Signature validition error while receiving mix tc answer: ", e);
					//leave error handling to request.checkRequestPostCondition() after all resources are received.
				}
				
				currentResourceNode = (Element) XMLUtil.getNextSiblingByName(currentResourceNode, 
						TermsAndConditionsRequest.XML_ELEMENT_RESOURCE_CUSTOMIZED_SECT);
			}
			
			currentResourceParentNode = XMLUtil.getNextSiblingByName(currentResourceParentNode, 
					TermsAndConditionsRequest.XML_ELEMENT_NAME);
		}
		request.checkRequestPostCondition();
		
		setChanged();
		notifyObservers();
	}
	
	static class TermsAndConditionsReadException extends Exception
	{
		Vector tcsTosShow = new Vector();
		
		void addTermsAndConditonsToRead(TermsAndConditions tc)
		{
			tcsTosShow.addElement(tc);
		}
		
		Enumeration getTermsTermsAndConditonsToRead()
		{
			return tcsTosShow.elements();
		}
	}
}
