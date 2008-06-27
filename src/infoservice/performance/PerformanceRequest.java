package infoservice.performance;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import anon.crypto.SignatureCreator;
import anon.crypto.SignatureVerifier;
import anon.infoservice.Database;
import anon.infoservice.InfoServiceDBEntry;
import anon.util.IXMLEncodable;
import anon.util.XMLParseException;
import anon.util.XMLUtil;

public class PerformanceRequest implements IXMLEncodable 
{
	public static final String XML_ELEMENT_NAME = "PerformanceRequest";
	public static final String XML_ELEMENT_TOKEN_ID = "TokenId";
	public static final String XML_ELEMENT_DATASIZE = "DataSize";
	
	private String m_tokenId;
	private int m_dataSize;
		
	public PerformanceRequest(Element a_node) throws XMLParseException
	{
		XMLUtil.assertNodeName(a_node, XML_ELEMENT_NAME);
		
		if(!SignatureVerifier.getInstance().verifyXml(a_node, SignatureVerifier.DOCUMENT_CLASS_INFOSERVICE))
		{
			throw new XMLParseException("Could not verify XML Document.");
		}
		
		Node nodeToken = XMLUtil.getFirstChildByName(a_node, XML_ELEMENT_TOKEN_ID);
		
		XMLUtil.assertNotNull(nodeToken);
		
		m_tokenId = XMLUtil.parseValue(nodeToken, "");
		
		// check against available tokens
		PerformanceToken token = (PerformanceToken) Database.getInstance(PerformanceToken.class).getEntryById(m_tokenId);
		
		if(token == null)
		{
			throw new XMLParseException("Request did not specify a valid token.");
		}
		
		Node nodeDataSize = XMLUtil.getFirstChildByName(a_node, XML_ELEMENT_DATASIZE);
		
		XMLUtil.assertNotNull(nodeDataSize);
		
		m_dataSize = XMLUtil.parseValue(nodeDataSize, -1);		
	}
	
	public PerformanceRequest(String a_tokenId, int a_dataSize)
	{
		m_tokenId = a_tokenId;
		m_dataSize = a_dataSize;
	}
	
	public Element toXmlElement(Document a_doc) 
	{
		Element elem = a_doc.createElement(XML_ELEMENT_NAME);
		
		Element elemTokenId = a_doc.createElement(XML_ELEMENT_TOKEN_ID);
		XMLUtil.setValue(elemTokenId, m_tokenId);
		
		Element elemDataSize = a_doc.createElement(XML_ELEMENT_DATASIZE);
		XMLUtil.setValue(elemDataSize, m_dataSize);
		
		elem.appendChild(elemTokenId);
		elem.appendChild(elemDataSize);
		
		return elem;
	}
	
	public String getTokenId()
	{
		return m_tokenId;
	}
	
	public int getDataSize()
	{
		return m_dataSize;
	}
}
