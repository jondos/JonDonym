package anon.infoservice;

import java.util.Date;

import org.w3c.dom.Element;

public interface TermsAndConditionsTranslation 
{
	public String getTemplateReferenceId();
	
	public String getLocale();
	
	public boolean isDefaultTranslation();
	
	public Element getTranslationElement();
	
	public ServiceOperator getOperator();
	
	public Date getDate();
}
