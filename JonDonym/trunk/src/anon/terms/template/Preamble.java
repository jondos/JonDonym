package anon.terms.template;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import anon.client.TrustModel.PremiumAttribute;
import anon.infoservice.OperatorAddress;
import anon.infoservice.ServiceOperator;
import anon.util.XMLParseException;
import anon.util.XMLUtil;

public class Preamble 
{
	public static String XML_ELEMENT_NAME = "Preamble";
	public static String XML_ELEMENT_LEADING_TEXT = "LeadingText";
	public static String XML_ELEMENT_TRAILING_TEXT = "TrailingText";
	
	private String leadingText = null;
	private ServiceOperator operator = null; 
	private OperatorAddress operatorAddress = null;
	private String trailingText = null;
	
	public Preamble()
	{
	}
	

	public Preamble(Node rootNode) throws XMLParseException
	{
		Element preambleRoot = null;
		if(rootNode.getNodeType() == Node.DOCUMENT_NODE)
		{
			preambleRoot = ((Document) rootNode).getDocumentElement();
		}
		else if(rootNode.getNodeType() == Node.ELEMENT_NODE)
		{
			preambleRoot = (Element) rootNode;
		}
		else
		{
			throw new XMLParseException("Invalid node type");
		}
		
		Element leadingTextElement = 
			(Element) XMLUtil.getFirstChildByName(preambleRoot, XML_ELEMENT_LEADING_TEXT);
		Element trailingTextElement = 
			(Element) XMLUtil.getFirstChildByName(preambleRoot, XML_ELEMENT_TRAILING_TEXT);
		
		
		
		leadingText = (leadingTextElement != null) ? XMLUtil.parseValue(leadingTextElement, (String) null) : null;
		trailingText = (trailingTextElement != null) ? XMLUtil.parseValue(trailingTextElement, (String) null) : null;
	}
	
	public String getLeadingText() 
	{
		return leadingText;
	}
	public void setLeadingText(String leadingText) 
	{
		this.leadingText = leadingText;
	}
	public ServiceOperator getOperator() 
	{
		return operator;
	}
	public void setOperator(ServiceOperator operator) 
	{
		this.operator = operator;
	}
	
	public OperatorAddress getOperatorAddress() 
	{
		return operatorAddress;
	}

	public void setOperatorAddress(OperatorAddress operatorAddress) 
	{
		this.operatorAddress = operatorAddress;
	}

	public String getTrailingText() 
	{
		return trailingText;
	}
	public void setTrailingText(String trailingText) 
	{
		this.trailingText = trailingText;
	}
	
	public Element toXmlElement(Document ownerDoc)
	{
		Element preambleRoot = ownerDoc.createElement(XML_ELEMENT_NAME);
		Element leadingTextElement = ownerDoc.createElement(XML_ELEMENT_LEADING_TEXT);
		Element trailingTextElement = ownerDoc.createElement(XML_ELEMENT_TRAILING_TEXT);
		
		Element operatorElement = (operator != null) ? 
				operator.toXMLElement(ownerDoc, operatorAddress, false) : ownerDoc.createElement(ServiceOperator.XML_ELEMENT_NAME);
		XMLUtil.setValue(leadingTextElement, (leadingText != null) ? leadingText : "");
		XMLUtil.setValue(trailingTextElement, (trailingText != null) ? trailingText : "");
		
		preambleRoot.appendChild(leadingTextElement);
		preambleRoot.appendChild(operatorElement);
		preambleRoot.appendChild(trailingTextElement);	
		
		return preambleRoot;
	}
}
