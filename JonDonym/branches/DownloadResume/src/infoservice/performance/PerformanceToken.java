package infoservice.performance;

import java.util.Random;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.Document;

import anon.infoservice.AbstractDatabaseEntry;
import anon.infoservice.Database;
import anon.infoservice.InfoServiceDBEntry;
import anon.util.XMLParseException;
import anon.util.XMLUtil;
import anon.util.IXMLEncodable;
import anon.util.Base64;

public class PerformanceToken extends AbstractDatabaseEntry implements IXMLEncodable
{
	public static final int PERFORMANCE_TOKEN_TTL = 120*1000;
	public static final int PERFORMANCE_TOKEN_BASE_LENGTH = 16;
	
	public static final String XML_ELEMENT_NAME = "PerformanceToken";
	public static final String XML_ELEMENT_ID = "Id";
		
	private static Random ms_rnd = new Random();
	
	private String m_id;
	private long m_lastUpdate;
	
	public PerformanceToken() 
	{
		super(System.currentTimeMillis() + PERFORMANCE_TOKEN_TTL);
		
		m_lastUpdate = System.currentTimeMillis();
		
		byte[] token = new byte[PERFORMANCE_TOKEN_BASE_LENGTH];
		ms_rnd.nextBytes(token);
		m_id = Base64.encode(token, false) + Base64.encodeString(String.valueOf(System.currentTimeMillis()));
	}
	
	public PerformanceToken(Element a_node) throws XMLParseException
	{
		super(-1);
		m_lastUpdate = -1;
		
		XMLUtil.assertNodeName(a_node, XML_ELEMENT_NAME);
		
		Node token = XMLUtil.getFirstChildByName(a_node, XML_ELEMENT_ID);
		
		XMLUtil.assertNotNull(token);
		
		m_id = XMLUtil.parseValue(token, "");
	}
	
	public String getId() 
	{
		return m_id;
	}

	public long getLastUpdate() 
	{
		return m_lastUpdate;
	}

	public long getVersionNumber() 
	{
		return m_lastUpdate;
	}
	
	public Element toXmlElement(Document a_doc)
	{
		Element elem = a_doc.createElement(XML_ELEMENT_NAME);
		
		Element elemToken = a_doc.createElement(XML_ELEMENT_ID);
		XMLUtil.setValue(elemToken, m_id);
		
		elem.appendChild(elemToken);
		
		return elem;
	}
}
