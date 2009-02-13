package anon.client;

import java.util.Observable;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import anon.infoservice.Database;
import anon.infoservice.TermsAndConditions;
import anon.infoservice.TermsAndConditionsFramework;
import anon.util.XMLParseException;
import anon.util.XMLUtil;



public class TermsAndConditionsResponseHandler extends Observable
{
	public static final String XML_ELEMENT_RESPONSE_NAME = "TermsAndConditionsResponse";
	
	public void handleXMLResourceResponse(Document answerDoc) throws XMLParseException
	{
		if(!answerDoc.getDocumentElement().getTagName().equals(XML_ELEMENT_RESPONSE_NAME))
		{
			throw new XMLParseException("No TC response.");
		}
		Node currentResourceParentNode = XMLUtil.getFirstChildByName(answerDoc.getDocumentElement(), 
				XMLTermsAndConditionsRequest.XML_ELEMENT_NAME);
		Node currentResourceNode = null;
		String currentID = "";
		String currentLang = "";
		while(currentResourceParentNode != null)
		{
			currentID = XMLUtil.parseAttribute(currentResourceParentNode, XMLTermsAndConditionsRequest.XML_ATTR_ID, "");
			currentLang = XMLUtil.parseAttribute(currentResourceParentNode, XMLTermsAndConditionsRequest.XML_ATTR_LOCALE, "");
			if(currentID.equals("") || currentLang.equals("") || currentLang.length() != 2 )
			{
				throw new XMLParseException("invalid attributes: "+currentID+"/"+currentLang);
			}
			
			currentResourceNode = XMLUtil.getFirstChildByName(currentResourceParentNode, 
					XMLTermsAndConditionsRequest.XML_ELEMENT_RESOURCE_TEMPLATE);
			if(currentResourceNode != null)
			{
				TermsAndConditionsFramework fr = new TermsAndConditionsFramework((Element)currentResourceNode.getFirstChild());
				Database db = Database.getInstance(TermsAndConditionsFramework.class);
				db.update(fr);
			}
			currentResourceNode = null;
			currentResourceNode = XMLUtil.getFirstChildByName(currentResourceParentNode, 
					XMLTermsAndConditionsRequest.XML_ELEMENT_RESOURCE_CUSTOMIZED_SECT);
			if(currentResourceNode != null)
			{
				TermsAndConditions tc = TermsAndConditions.getById(currentID);
				tc.addTranslation(((Element) XMLUtil.getFirstChildByName(currentResourceNode, TermsAndConditions.XML_ELEMENT_TRANSLATION_NAME)));
			}
			currentResourceNode = null;
			currentResourceParentNode = XMLUtil.getNextSiblingByName(currentResourceParentNode, 
					XMLTermsAndConditionsRequest.XML_ELEMENT_NAME);
		}
		setChanged();
		notifyObservers();
	}
	
	static class TCRequestException extends Exception
	{
		
	}
}
