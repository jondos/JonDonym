package anon.pay.xml;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import anon.util.IXMLEncodable;

public class XMLCloseAck implements IXMLEncodable
{
//	final static private byte[] XML_CLOSE_ACK = "<?xml version=\"1.0\" ?>\n<CloseAck/>".getBytes();

/*	public static byte[] getXMLByteArray()
	{
		return XML_CLOSE_ACK;
	}*/

	/**
	 * toXmlElement
	 *
	 * @param a_doc Document
	 * @return Element
	 */
	public Element toXmlElement(Document a_doc)
	{
		Element elem = a_doc.createElement("CloseAck");
		return elem;
	}
}
