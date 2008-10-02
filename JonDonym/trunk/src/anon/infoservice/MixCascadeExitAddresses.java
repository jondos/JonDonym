package anon.infoservice;

import java.net.InetAddress;
import java.util.Hashtable;
import java.util.Enumeration;

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
	/**
	 * The time-to-live of the exit address.
	 */
	public static final int EXIT_ADDRESS_TTL = 1000*60*60*24;
	
	/**
	 * The entry's container XML element name.
	 */
	public static final String XML_ELEMENT_CONTAINER_NAME = "ExitAddressesList";

	/**
	 * The XML element name.
	 */
	public static final String XML_ELEMENT_NAME = "ExitAddresses";
	
	/**
	 * The exit address XML element name.
	 */
	public static final String XML_ELEMENT_ADDRESS_NAME = "ExitAddress";
	
	/**
	 * The last update XML attribute name.
	 */
	public static final String XML_ATTR_LAST_UPDATE = "lastUpdate";
	
	/**
	 * The payment XML attribute name.
	 */
	public static final String XML_ATTR_PAYMENT = "payment";
	
	/**
	 * The time of the last update.
	 */
	private long m_lastUpdate;
	
	/**
	 * The serial of the database entry.
	 */
	private long m_serial;
	
	/**
	 * The cascade id.
	 */
	private String m_strCascadeId = null;
	
	/**
	 * Specifies whether this is a paycascade or not
	 */
	private boolean m_bPayment = false;
	
	/**
	 * The list of addresses.
	 */
	private Hashtable m_tblAddresses = new Hashtable();
	
	/**
	 * Constructs a new <code>MixCascadeExitAddress</code> object from a given 
	 * mix cascade id.
	 * 
	 * @param a_strCascadeId The mix cascade id.
	 */
	public MixCascadeExitAddresses(MixCascade a_cascade)
	{
		super(System.currentTimeMillis() + EXIT_ADDRESS_TTL);
		
		m_strCascadeId = a_cascade.getId();
		m_bPayment = a_cascade.isPayment();
		
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
	
	/**
	 * Adds an address to the list.
	 * 
	 * @param a_addr The address to add.
	 * @return True, if the list has changed, false otherwise.
	 */
	public boolean addInetAddress(InetAddress a_addr)
	{
		boolean bChanged = false;
		
		// enumerate through all entries and remove obsolete ones
		Enumeration e = m_tblAddresses.keys();
		
		while(e.hasMoreElements())
		{
			Long timestamp = (Long) e.nextElement();
			
			// exit address expired, remove it
			if(timestamp.longValue() < System.currentTimeMillis() - EXIT_ADDRESS_TTL)
			{
				m_tblAddresses.remove(timestamp);
				bChanged = true;
			}
		}
		
		// add the address if it isn't already in the list
		if(!m_tblAddresses.contains(a_addr))
		{
			m_tblAddresses.put(new Long(System.currentTimeMillis()), a_addr);
			bChanged = true;
		}
		
		return bChanged;
	}
	
	public Element toXmlElement(Document a_doc)
	{
		Element elem = a_doc.createElement(XML_ELEMENT_NAME);
		XMLUtil.setAttribute(elem, XML_ATTR_ID, getId());
		XMLUtil.setAttribute(elem, XML_ATTR_PAYMENT, m_bPayment);
		
		Enumeration e = m_tblAddresses.keys();
		
		while(e.hasMoreElements())
		{
			Long timestamp = (Long) e.nextElement();
			InetAddress addr = (InetAddress) m_tblAddresses.get(timestamp); 
			
			Element el = a_doc.createElement(XML_ELEMENT_ADDRESS_NAME);
			XMLUtil.setAttribute(el, XML_ATTR_LAST_UPDATE, timestamp.longValue());
			XMLUtil.setValue(el, addr.getHostAddress());
			elem.appendChild(el);
		}
		
		return elem;
	}
}
