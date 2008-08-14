package infoservice.performance;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import anon.crypto.SignatureVerifier;
import anon.infoservice.Database;
import anon.infoservice.InfoServiceDBEntry;
import anon.util.IXMLEncodable;
import anon.util.XMLParseException;
import anon.util.XMLUtil;

public class PerformanceTokenRequest implements IXMLEncodable 
{
	public static final String XML_ELEMENT_NAME = "PerformanceTokenRequest";
	public static final String XML_ELEMENT_INFOSERVICE_ID = "InfoServiceId";

	private String m_infoServiceId;
	
	public PerformanceTokenRequest(Element a_node) throws XMLParseException
	{
		XMLUtil.assertNodeName(a_node, XML_ELEMENT_NAME);
		
		if(!SignatureVerifier.getInstance().verifyXml(a_node, SignatureVerifier.DOCUMENT_CLASS_INFOSERVICE))
		{
			throw new XMLParseException("Could not verify XML Document.");
		}
		
		Node isId = XMLUtil.getFirstChildByName(a_node, XML_ELEMENT_INFOSERVICE_ID);
		
		XMLUtil.assertNotNull(isId);
		
		m_infoServiceId = XMLUtil.parseValue(isId, "");
		
		// check against available infoservices
		InfoServiceDBEntry infoService = (InfoServiceDBEntry) Database.getInstance(InfoServiceDBEntry.class).getEntryById(m_infoServiceId);
		
		if(infoService == null)
		{
			throw new XMLParseException("Request did not specify a valid infoservice id.");
		}
	}
	
	public PerformanceTokenRequest(String a_infoServiceId)
	{
		m_infoServiceId = a_infoServiceId;
	}
	
	public Element toXmlElement(Document a_doc) 
	{
		Element elem = a_doc.createElement(XML_ELEMENT_NAME);
		
		Element elemISId = a_doc.createElement(XML_ELEMENT_INFOSERVICE_ID);
		XMLUtil.setValue(elemISId, m_infoServiceId);
		
		elem.appendChild(elemISId);

		return elem;
	}
	
	public String getInfoServiceId()
	{
		return m_infoServiceId;
	}
}
