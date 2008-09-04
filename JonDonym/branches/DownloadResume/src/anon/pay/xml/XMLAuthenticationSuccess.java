package anon.pay.xml;

/**
 * @deprecated use XMLErrorMessage instead with code ERR_OK
 * <p>\u00DCberschrift: </p>
 * <p>Beschreibung: </p>
 * <p>Copyright: Copyright (c) 2004</p>
 * <p>Organisation: </p>
 * @author not attributable
 * @version 1.0
 */
public class XMLAuthenticationSuccess
{
	final static private byte[] XML_AUTH_SUCCESS = ("<?xml version=\"1.0\" ?>" +
		"<Authentication>Success</Authentication>").getBytes();

	public static byte[] getXMLByteArray()
	{
		return XML_AUTH_SUCCESS;
	}
}
