package anon.pay.xml;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import anon.util.IXMLEncodable;
import anon.util.XMLUtil;
import java.util.Vector;
import org.w3c.dom.NodeList;
import java.util.Hashtable;
import org.w3c.dom.Node;
import java.util.Enumeration;

/**
 * Wraps a Hashtable of Strings in xml
 * Can be used to send simple text over the biconnection,
 * when you have more than one piece of data to send, but don't want to go to the trouble
 * of defining a specialized implementation of IXMLEncodable
 *
  * <GenericStrings>
 *     <Entry name="foo">some string value</Entry>
 *     <Entry name="bar">numbers work too, but are treated as Strings</Entry>
 *     <Entry name="baz">300</Entry>
 * </XMLGenericStrings>
 *
 * @author Elmar Schraml
 */
public class XMLGenericStrings implements IXMLEncodable
{
	private Document m_doc;
	public static String ms_strElemName = "GenericStrings";
	Hashtable m_strings;

	public XMLGenericStrings()
	{
		m_strings = new Hashtable();
		m_doc = XMLUtil.createDocument();
		m_doc.appendChild(internal_toXmlElement(m_doc));
	}

	public XMLGenericStrings(String xml) throws Exception
	{
		Document doc = XMLUtil.toXMLDocument(xml);
		Element rootElem = doc.getDocumentElement();
		setValues(rootElem);
	}



	/**
	 * just a convenience constructor, will add both strings as content
	 *
	 * @param a_text String
	 * @param another_text String
	 */
	public XMLGenericStrings(String key, String value)
	{
		m_strings = new Hashtable();
		m_strings.put(key,value);
		m_doc = XMLUtil.createDocument();
		m_doc.appendChild(internal_toXmlElement(m_doc));
	}

	public XMLGenericStrings(Hashtable content)
	{
		m_strings = content;
		m_doc = XMLUtil.createDocument();
		m_doc.appendChild(internal_toXmlElement(m_doc));
	}

	public void addEntry(String name, String value)
	{
		m_strings.put(name,value);
		m_doc = XMLUtil.createDocument();
		m_doc.appendChild(internal_toXmlElement(m_doc));
	}

	public Hashtable getStrings()
	{
		return (Hashtable) m_strings.clone();
	}

	public String getValue(String key)
	{
		return (String) m_strings.get(key);
	}

	public XMLGenericStrings(Element xml) throws Exception
	{
		setValues(xml);
		m_doc = XMLUtil.createDocument();
		m_doc.appendChild(XMLUtil.importNode(m_doc, xml, true));
	}

	public XMLGenericStrings(Document doc) throws Exception
	{
		setValues(doc.getDocumentElement());
		m_doc = doc;
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
			return (Element) XMLUtil.importNode(a_doc, m_doc.getDocumentElement(), true);
		}
		catch (Exception e)
		{
			return null;
		}

	}

	private Element internal_toXmlElement(Document a_doc)
	{
		Element elemRoot = a_doc.createElement(this.ms_strElemName);
		Element elem;

	    for (Enumeration allKeys = m_strings.keys(); allKeys.hasMoreElements(); )
		{
			String key = (String) allKeys.nextElement();
			String value = (String) m_strings.get(key);
			elem = a_doc.createElement("Entry");
			elem.setAttribute("name",key);
			elem.appendChild(a_doc.createTextNode(value));
			elemRoot.appendChild(elem);
		}

		return elemRoot;
	}



	private void setValues(Element xml) throws Exception
	{
		String rootTagName = xml.getTagName();
		if (!rootTagName.equals(this.ms_strElemName))
		{
			throw new Exception("XMLGenericStrings: cannot parse, wrong xml format!");
		}
	    NodeList entries = xml.getElementsByTagName("Entry");
		m_strings = new Hashtable();
		for (int i = 0; i < entries.getLength(); i++)
		{
			Node curNode = entries.item(i);
			String key = XMLUtil.parseAttribute(curNode,"name","");
			String value =	XMLUtil.parseValue(curNode,"");
			m_strings.put(key,value);
		}
	}


}
