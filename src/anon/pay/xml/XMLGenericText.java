package anon.pay.xml;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import anon.util.IXMLEncodable;
import anon.util.XMLUtil;

/**
 * Wraps a String in an IXMLEncodable
 * Can be used to send simple text over the biconnection
 *
 * Text can be plain text (which will be put into the top-level element)
 * or HTML/XML (whose top-level element will be a child of the top-level element)
 *
 * HTML does not have to be a webpage, i.e. does not have to be wrapped in <html><body> tags,
 * but does have to be valid XML (e.g. closed tags),
 * and has to start and end with a tag (i.e. "this is <b>important</b>data" would NOT be okay),
 * otherwise it will be treated as plain text.
 *
 * This class will typically be used if you already have XML or HTML,
 * but need to put it into an IXMLEncodable so PICommand will accept it
 *
 * <GenericText type="plaintext">
 *     blablablabla blabla bla
 * </XMLGenericText>
 *
 * <GenericText type="xml">
 *     <h3>Some Headline</h3>
 * </GenericText>
 *
 * @todo: treat xml/html as real xml (currently just wrapped as plaintext)
 *
 * @author Elmar Schraml
 */
public class XMLGenericText implements IXMLEncodable
{
	public static final int TYPE_PLAINTEXT = 1;
	public static final int TYPE_XML = 2;

	private String m_text;
	private Document m_docTheText;
	public static final String XML_ELEMENT_NAME = "GenericText";

	public XMLGenericText()
	{
		m_text = "";
		m_docTheText = XMLUtil.createDocument();
		m_docTheText.appendChild(internal_toXmlElement(m_docTheText));
	}

	public XMLGenericText(String a_text)
	{
		m_text = a_text;
		m_docTheText = XMLUtil.createDocument();
		m_docTheText.appendChild(internal_toXmlElement(m_docTheText));
	}

	public String getText()
	{
		return m_text;
	}

	public XMLGenericText(Element xml) throws Exception
	{
		setValues(xml);
		m_docTheText = XMLUtil.createDocument();
		m_docTheText.appendChild(XMLUtil.importNode(m_docTheText, xml, true));
	}

	public XMLGenericText(Document doc) throws Exception
	{
		setValues(doc.getDocumentElement());
		m_docTheText = doc;
	}



	/**
	 * Return an element that can be appended to the document.
	 *
	 * @param a_doc a document
	 * @return the interface as xml element
	 * @todo Implement this anon.util.IXMLEncodable method
	 */
	public Element toXmlElement(Document a_doc)
	{
		try
		{
			return (Element) XMLUtil.importNode(a_doc, m_docTheText.getDocumentElement(), true);
		}
		catch (Exception e)
		{
			return null;
		}

	}

	private Element internal_toXmlElement(Document a_doc)
	{
		Element elemRoot = a_doc.createElement(XML_ELEMENT_NAME);
		XMLUtil.setValue(elemRoot, m_text);
		return elemRoot;
	}



	private void setValues(Element xml) throws Exception
	{
		String rootTagName = xml.getTagName();
		if (!rootTagName.equals(XML_ELEMENT_NAME))
		{
			throw new Exception("XMLGenericText: cannot parse, wrong xml format!");
		}

		Element elem;

		// parse text
		m_text = XMLUtil.parseValue(xml, "");
		//might very well be empty
	}


}
