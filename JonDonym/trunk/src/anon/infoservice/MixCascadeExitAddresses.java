package anon.infoservice;

import java.net.InetAddress;
import java.util.Vector;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import anon.util.IXMLEncodable;
import anon.util.XMLUtil;

/**
 * Stores all exit addresses of one <code>MixCasacde</code>.
 * 
 * @author Christian Banse
 */
public class MixCascadeExitAddresses extends AbstractDatabaseEntry implements IXMLEncodable
{
	public static final int EXIT_ADDRESS_TTL = 1000*60*60*24;
	
	/**
	 * The entry's container XML element name.
	 */
	public static final String XML_ELEMENT_CONTAINER_NAME = "ExitAddressesList";

	/**
	 * The XML element name.
	 */
	public static final String XML_ELEMENT_NAME = "ExitAddresses";
	
	public static final String XML_ELEMENT_ADDRESS_NAME = "ExitAddress";
	
	private long m_lastUpdate;
	private long m_serial;
	
	private String m_strCascadeId = null;
	
	private Vector m_vecAddr = new Vector();
	
	/**
	 * Constructs a new <code>MixCascadeExitAddress</code> object from a given 
	 * mix cascade id.
	 * 
	 * @param a_strCascadeId The mix cascade id.
	 */
	public MixCascadeExitAddresses(String a_strCascadeId)
	{
		super(System.currentTimeMillis() + EXIT_ADDRESS_TTL);
		
		m_strCascadeId = a_strCascadeId;
		
		m_lastUpdate = System.currentTimeMillis();
		m_serial = System.currentTimeMillis();
	}
	
	public String getId() 
	{
		return m_strCascadeId;
	}
	
	public long getLastUpdate() 
	{
		return m_lastUpdate;
	}
	
	public long getVersionNumber() 
	{
		return m_serial;
	}
	
	public boolean addInetAddress(InetAddress a_addr)
	{
		if(!m_vecAddr.contains(a_addr))
		{
			m_vecAddr.add(a_addr);
			
			return true;
		}
		
		return false;
	}
	
	public Element toXmlElement(Document a_doc)
	{
		Element elem = a_doc.createElement(XML_ELEMENT_NAME);
		XMLUtil.setAttribute(elem, XML_ATTR_ID, getId());
		
		for(int i = 0; i < m_vecAddr.size(); i++)
		{
			Element e = a_doc.createElement(XML_ELEMENT_ADDRESS_NAME);
			XMLUtil.setValue(e, ((InetAddress) m_vecAddr.elementAt(i)).getHostAddress());
			elem.appendChild(e);
		}		
		
		return elem;
	}
}
