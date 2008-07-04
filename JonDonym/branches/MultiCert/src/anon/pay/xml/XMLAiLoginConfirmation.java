package anon.pay.xml;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.Element;

import anon.util.XMLUtil;
import anon.util.IXMLEncodable;
import anon.util.XMLParseException;

public class XMLAiLoginConfirmation implements IXMLEncodable {
	
	private int m_code;
	private String m_message;
	
	public static final String XML_ELEMENT_NAME = "LoginConfirmation";
	
	public XMLAiLoginConfirmation(Element element) throws XMLParseException
	{
		m_code = -1;
		m_message = null;
		setValues(element);
	}
	
	private void setValues(Element elemRoot) throws XMLParseException
	{
		XMLUtil.assertNodeName((Node) elemRoot, XML_ELEMENT_NAME);
		
		m_code = XMLUtil.parseAttribute(elemRoot, "code", -1);
		
		if(m_code == -1)
		{
			throw new XMLParseException("No or invalid confirmation code for login confirmation specified");
		}
		
		m_message = XMLUtil.parseValue((Node) elemRoot, null);
		
		if(m_message==null)
		{
			throw new XMLParseException("No login confirmation message specified");
		}
	}
	
	public Element toXmlElement(Document a_doc) {
		// TODO Auto-generated method stub
		return null;
	}

	public int getCode() {
		return m_code;
	}

	public String getMessage() {
		return m_message;
	}

}
