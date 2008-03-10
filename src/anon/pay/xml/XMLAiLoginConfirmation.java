package anon.pay.xml;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import anon.util.IXMLEncodable;
import anon.util.XMLParseException;

public class XMLAiLoginConfirmation implements IXMLEncodable {
	
	private int m_code;
	private String m_message;
	
	public static final Object XML_ELEMENT_NAME = "LoginConfirmation";
	
	public XMLAiLoginConfirmation(Element element) throws XMLParseException
	{
		m_code = -1;
		m_message = null;
		setValues(element);
	}
	
	private void setValues(Element elemRoot) throws XMLParseException
	{
		if (!elemRoot.getTagName().equals(XML_ELEMENT_NAME))
		{
			throw new XMLParseException("Login confirmation has wrong format or wrong version number");
		}
		String codeStr = elemRoot.getAttribute("code");
		if(codeStr.equals(""))
		{
			throw new XMLParseException("No confirmation code for login confirmation specified");
		}
		try 
		{
			m_code = Integer.parseInt(codeStr);
		}
		catch(NumberFormatException nfe)
		{
			throw new XMLParseException("Invalid login confirmation code specified");
		}
		m_message = elemRoot.getTextContent();
		if(m_message==null)
		{
			throw new XMLParseException("No login confirmation message specified");
		}
	}
	
	@Override
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
