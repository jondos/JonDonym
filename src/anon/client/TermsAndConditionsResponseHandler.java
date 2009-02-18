package anon.client;

import java.io.IOException;
import java.util.Enumeration;
import java.util.Observable;
import java.util.Vector;

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
	public static final String XML_ELEMENT_INVALID_REQUEST_NAME = "InvalidTermsAndConditionsRequest";
	public static final String XML_ELEMENT_RESPONSE_NAME = "TermsAndConditionsResponse";
	
	public void handleXMLResourceResponse(Document answerDoc) throws XMLParseException, IOException
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
				XMLTermsAndConditionsRequest.XML_ELEMENT_NAME);
		Node currentResourceNode = null;
		String currentID = "";
		//String currentLang = "";
		while(currentResourceParentNode != null)
		{
			currentID = XMLUtil.parseAttribute(currentResourceParentNode, XMLTermsAndConditionsRequest.XML_ATTR_ID, "");
			//currentLang = XMLUtil.parseAttribute(currentResourceParentNode, XMLTermsAndConditionsRequest.XML_ATTR_LOCALE, "");
			if(currentID.equals("")  )
			{
				throw new XMLParseException("invalid attributes: id not set");
			}
			
			currentResourceNode = XMLUtil.getFirstChildByName(currentResourceParentNode, 
					XMLTermsAndConditionsRequest.XML_ELEMENT_RESOURCE_TEMPLATE);
			while(currentResourceNode != null)
			{
				TermsAndConditionsFramework fr = new TermsAndConditionsFramework((Element)currentResourceNode.getFirstChild());
				Database db = Database.getInstance(TermsAndConditionsFramework.class);
				db.update(fr);
				currentResourceNode = (Element) XMLUtil.getNextSiblingByName(currentResourceNode, 
						XMLTermsAndConditionsRequest.XML_ELEMENT_RESOURCE_TEMPLATE);
			}
			
			currentResourceNode = XMLUtil.getFirstChildByName(currentResourceParentNode, 
					XMLTermsAndConditionsRequest.XML_ELEMENT_RESOURCE_CUSTOMIZED_SECT);
			while(currentResourceNode != null)
			{
				TermsAndConditions tc = TermsAndConditions.getById(currentID);
				tc.addTranslation(((Element) XMLUtil.getFirstChildByName(currentResourceNode, TermsAndConditions.XML_ELEMENT_TRANSLATION_NAME)));
				currentResourceNode = (Element) XMLUtil.getNextSiblingByName(currentResourceNode, 
						XMLTermsAndConditionsRequest.XML_ELEMENT_RESOURCE_CUSTOMIZED_SECT);
			}
			
			currentResourceParentNode = XMLUtil.getNextSiblingByName(currentResourceParentNode, 
					XMLTermsAndConditionsRequest.XML_ELEMENT_NAME);
		}
		setChanged();
		notifyObservers();
	}
	
	static class TermsAndConditionsReadException extends Exception
	{
		Vector tcsTosShow = new Vector();
		
		void addTermsAndConditonsToRead(TermsAndConditions tc)
		{
			tcsTosShow.add(tc);
		}
		
		Enumeration getTermsTermsAndConditonsToRead()
		{
			return tcsTosShow.elements();
		}
	}
}
