package anon.infoservice;

import java.util.Date;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

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
	
	public TermsAndConditionsTranslation duplicateWithImports(Element xmlImport);
}
