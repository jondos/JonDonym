package anon.terms;
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
import java.util.Date;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import anon.infoservice.OperatorAddress;
import anon.infoservice.ServiceOperator;

public interface TermsAndConditionsTranslation 
{
	public static final String XML_ELEMENT_NAME = "TCTranslation";
	public static final String XML_ELEMENT_CONTAINER_NAME = TermsAndConditions.XML_ELEMENT_NAME;
	
	public final static String XML_ELEMENT_PRIVACY_POLICY = "PrivacyPolicyUrl";
	public final static String XML_ELEMENT_LEGAL_OPINIONS = "LegalOpinionsUrl";
	public final static String XML_ELEMENT_OPERATIONAL_AGREEMENT = "OperationalAgreementUrl";
	
	public static final String XML_ATTR_LOCALE = "locale";
	public static final String XML_ATTR_DEFAULT_LOCALE = "default";
	public static final String XML_ATTR_REFERENCE_ID = "referenceId";
	
	public final static String PROPERTY_NAME_PRIVACY_POLICY = "privacyPolicyUrl";
	public final static String PROPERTY_NAME_LEGAL_OPINIONS = "legalOpinionsUrl";
	public final static String PROPERTY_NAME_OPERATIONAL_AGREEMENT = "operationalAgreementUrl";
	public final static String PROPERTY_NAME_TEMPLATE_REFERENCE_ID = "templateReferenceId";
	
	public void setTemplateReferenceId(String templateReferenceId);
	public String getTemplateReferenceId();
	
	public String getLocale();
	
	public void setDefaultTranslation(boolean defaultTranslation);
	public boolean isDefaultTranslation();
	
	public Element getTranslationElement();
	
	public Element createXMLOutput(Document a_doc);
	
	public ServiceOperator getOperator();
	
	public Date getDate();
	
	public void setOperatorAddress(OperatorAddress operatorAddress);
	public OperatorAddress getOperatorAddress();
	
	public String getPrivacyPolicyUrl();
	public void setPrivacyPolicyUrl(String privacyPolicyUrl);

	public String getLegalOpinionsUrl();
	public void setLegalOpinionsUrl(String legalOpinionsUrl);

	public String getOperationalAgreementUrl();
	public void setOperationalAgreementUrl(String operationalAgreementUrl);
	
	public void setSections(TCComposite sections);
	public TCComposite getSections();
	
	public TermsAndConditionsTranslation duplicateWithImports(Element xmlImport);
}
