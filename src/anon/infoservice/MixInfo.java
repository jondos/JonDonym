/*
 Copyright (c) 2000 - 2005, The JAP-Team
 All rights reserved.
 Redistribution and use in source and binary forms, with or without modification,
 are permitted provided that the following conditions are met:

  - Redistributions of source code must retain the above copyright notice,
 this list of conditions and the following disclaimer.

  - Redistributions in binary form must reproduce the above copyright notice,
 this list of conditions and the following disclaimer in the documentation and/or
 other materials provided with the distribution.

  - Neither the name of the University of Technology Dresden, Germany nor the names of its contributors
 may be used to endorse or promote products derived from this software without specific
 prior written permission.


 THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS ``AS IS'' AND ANY EXPRESS
 OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY
 AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE REGENTS OR CONTRIBUTORS
 BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA,
 OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER
 IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
 OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE
 */
package anon.infoservice;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import anon.crypto.CertPath;
import anon.crypto.JAPCertificate;
import anon.crypto.SignatureVerifier;
import anon.crypto.XMLSignature;
import anon.util.XMLParseException;
import anon.util.XMLUtil;
import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;
import anon.crypto.IVerifyable;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Date;
import java.util.Vector;

import anon.pay.xml.XMLPriceCertificate;
import anon.pay.AIControlChannel;

/**
 * Holds the information of one single mix.
 */
public class MixInfo extends AbstractDistributableCertifiedDatabaseEntry implements IVerifyable
{
	public static final String DEFAULT_NAME = "AN.ON Mix";

	public static final String XML_ELEMENT_CONTAINER_NAME = "Mixes";
	public static final String XML_ELEMENT_NAME = "Mix";


    /* LERNGRUPPE: Mix types */
    public static final int FIRST_MIX = 0;
    public static final int MIDDLE_MIX = 1;
    public static final int LAST_MIX = 2;

    /**
     * LERNGRUPPE
     * This is the type of the mix
     */
    private int m_type;

    /**
     * LERNGRUPPE
     * Indicates if this mix is available for dynamic cascades
     */
    private boolean m_dynamic = false;

	private boolean m_bSocks = false;
	
	/**
	 * Stores all exit IP addresses of this mix.
	 */
	private final Vector m_vecVisibleAdresses = new Vector();

	private final Vector m_vecListenerAdresses = new Vector();
	
  /**
   * This is the ID of the mix.
   */
  private String m_mixId;

  /**
   * Time (see System.currentTimeMillis()) when the mix has sent this HELO message.
   */
  private long m_lastUpdate;

  private long m_serial;

  /**
   * The name of the mix.
   */
  private String m_name;

  /**
   * Some information about the location of the mix.
   */
  private ServiceLocation m_mixLocation;

  /**
   * Some information about the operator of the mix.
   */
  private ServiceOperator m_mixOperator;

  /**
   * Some information about the used mix software.
   */
  private ServiceSoftware m_mixSoftware;

  /**
   * Stores whether the mix is waiting for a cascade assignment. This value is only true, if the
   * mix is sending configure requests instead of HELO messages and if it is not already assigned
   * to a cascade. This value is only meaningful within the context of the infoservice.
   */
  private boolean m_freeMix;

  /**
   * Stores the XML structure for this mix.
   */
  private Element m_xmlStructure;

  /**
   * Stores the certificate for this mix.
   * The certificate is not set (null) if the MixInfo-Object is in the InfoService
   */
  private JAPCertificate m_mixCertificate;

  private JAPCertificate m_operatorCertificate;

  /**
   * Stores the certPath for this mix.
   * The CertPath is not set (null) if the MixInfo-Object is in the InfoService
   */
  private CertPath m_mixCertPath;

  /**
   *  The price certificate for the Mix
   */
  private XMLPriceCertificate m_priceCert;

  /**
   * Amount of bytes that the JAP has to prepay with this Cascade
   */
  private long m_prepaidInterval;

  /**
   * Stores the signature element for this mix.
   * The CertPath is not set (null) if the MixInfo-Object is in the InfoService
   */
  private XMLSignature m_mixSignature;

  /**
   * If this MixInfo has been recevied directly from a cascade connection.
   */
  private boolean m_bFromCascade;
  
  /**
   * True, if this Mix has a performance server.
   */
  private boolean m_bPerformanceServer;

  /**
	* The host of the performance server.
	*/
  private String m_strPerformanceServerHost;
	
  /**
   * The port of the performance server.
   */
  private int m_iPerformanceServerPort;
  
  /**
   * Creates a new MixInfo from XML description (Mix node). The state of the mix will be set to
   * non-free (only meaningful within the context of the infoservice).
   *
   * @param a_mixNode The Mix node from an XML document.
   */
  public MixInfo(Element a_mixNode) throws XMLParseException {
	  this (a_mixNode, 0);
  }

  /**
   * Creates a new MixInfo from XML description (Mix node). The state of the mix will be set to
   * non-free (only meaningful within the context of the infoservice).
   *
   * @param a_mixNode The Mix node from an XML document.
   * @param a_expireTime forces a specific expire time; takes default expire time if <= 0
   */
  public MixInfo(Element a_mixNode, long a_expireTime) throws XMLParseException
  {
	  this(a_mixNode, a_expireTime, false);
  }


  public MixInfo(String a_mixID, CertPath a_certPath)
  {
	  super(Long.MAX_VALUE);
	  if (a_mixID == null)
	  {
		  throw new IllegalArgumentException("No Mix ID!");
	  }
	  m_mixId = a_mixID;
	  m_name = a_mixID;
	  m_type = -1;
	  m_bFromCascade = true;
	  m_mixCertPath = a_certPath;
	  m_mixCertificate = a_certPath.getFirstCertificate();
	  m_operatorCertificate = a_certPath.getSecondCertificate();
	  m_lastUpdate = 0;
	  m_serial = 0;
	  m_mixLocation = new ServiceLocation(null, m_mixCertificate);
	  m_mixOperator = new ServiceOperator(null, m_mixCertPath.getSecondCertificate(), 0);
	  m_freeMix = false;
	  m_prepaidInterval = AIControlChannel.MAX_PREPAID_INTERVAL;
	  m_bPerformanceServer = false;
	  m_strPerformanceServerHost = "";
	  m_iPerformanceServerPort = -1;
  }

  public MixInfo(String a_mixID, CertPath a_certPath, XMLPriceCertificate a_priceCert, long a_prepaidInterval)
  {
	  super(Long.MAX_VALUE);
	  m_mixId = a_mixID;
	  m_name = a_mixID;
	  m_type = -1;
	  m_bFromCascade = true;
	  m_mixCertPath = a_certPath;
	  m_mixCertificate = a_certPath.getFirstCertificate();
	  m_operatorCertificate = a_certPath.getSecondCertificate();
	  m_lastUpdate = 0;
	  m_serial = 0;
	  m_mixLocation = new ServiceLocation(null, m_mixCertificate);
	  m_mixOperator = new ServiceOperator(null, m_mixCertPath.getSecondCertificate(), 0);
	  m_freeMix = false;
	  //
	  m_priceCert = a_priceCert;
	  m_prepaidInterval = a_prepaidInterval;
	  m_bPerformanceServer = false;
	  m_strPerformanceServerHost = "";
	  m_iPerformanceServerPort = -1;	  
  }

  /**
   * Creates a new MixInfo from XML description (Mix node). The state of the mix will be set to
   * non-free (only meaningful within the context of the infoservice).
   *
   * @param a_mixNode The Mix node from an XML document.
   * @param a_expireTime forces a specific expire time; takes default expire time if <= 0
   * @param a_bFromCascade if this is a MixInfo node directly received from a cascade (it is stripped)
   * if true, the last update value is set to 0
   */
  public MixInfo(Element a_mixNode, long a_expireTime, boolean a_bFromCascade)
	  throws XMLParseException
  {
	  /* use always the timeout for the infoservice context, because the JAP client currently does
	   * not have a database of mixcascade entries -> no timeout for the JAP client necessary
	   */
	  super(a_expireTime <= 0 ? System.currentTimeMillis() + Constants.TIMEOUT_MIX : a_expireTime);
	  m_bFromCascade = a_bFromCascade;
	  /* get the ID */
	  m_mixId = XMLUtil.parseAttribute(a_mixNode, "id", null);
	  if (m_mixId == null)
	  {
		  throw (new XMLParseException(XMLParseException.NODE_NULL_TAG, "id"));
	  }

	  /* try to get the certificate and CertPath from the Signature node */
	  try
	  {
		  m_mixSignature = SignatureVerifier.getInstance().getVerifiedXml(a_mixNode,
			  SignatureVerifier.DOCUMENT_CLASS_MIX);

		  if (m_mixSignature != null)
		  {
			  m_mixCertPath = m_mixSignature.getCertPath();
			  if (m_mixCertPath != null && m_mixCertPath.getFirstCertificate() != null)
			  {
				  m_mixCertificate = m_mixCertPath.getFirstCertificate();
				  m_operatorCertificate = m_mixCertPath.getSecondCertificate();
			  }
			  else
			  {
				  LogHolder.log(LogLevel.DEBUG, LogType.MISC,
								"No appended certificates in the MixCascade structure.");
			  }
		  }
		  else
		  {
			  LogHolder.log(LogLevel.DEBUG, LogType.MISC,
							"No signature node found while looking for MixCascade certificate.");
		  }
	  }
	  catch (Exception e)
	  {
		  LogHolder.log(LogLevel.ERR, LogType.MISC,
						"Error while looking for appended certificates in the MixInfo structure: " +
						e.toString());
	  }

	  if (!checkId())
	  {
		  throw new XMLParseException(XMLParseException.ROOT_TAG, "Malformed Mix ID: " + m_mixId);
	  }

	  m_name = XMLUtil.parseValue(XMLUtil.getFirstChildByName(a_mixNode, "Name"), DEFAULT_NAME);

	  m_bSocks = XMLUtil.parseAttribute(
		   XMLUtil.getFirstChildByName(a_mixNode, "Proxies"), "socks5Support", false);

	  Node operatorNode = XMLUtil.getFirstChildByName(a_mixNode, "Operator");
	  Node locationNode = XMLUtil.getFirstChildByName(a_mixNode, "Location");
	  Node lastUpdateNode = XMLUtil.getFirstChildByName(a_mixNode, "LastUpdate");
	  Node softwareNode = XMLUtil.getFirstChildByName(a_mixNode, "Software");
	  Node prepaidIntervalNode = XMLUtil.getFirstChildByName(a_mixNode, "PrepaidIntervalKbytes");

	  Node priceCertNode = XMLUtil.getFirstChildByName(a_mixNode, XMLPriceCertificate.XML_ELEMENT_NAME);
	  if (priceCertNode != null)
	  {
		  m_priceCert = new XMLPriceCertificate((Element)priceCertNode);
		  if (!m_priceCert.getSubjectKeyIdentifier().equals(getId()))
		  {
			  String message = "SKI in price certificate differs from Mix ID! SKI: $"
				  + m_priceCert.getSubjectKeyIdentifier() + "$ MixID: $" + getId() + "$";
			  LogHolder.log(LogLevel.ERR, LogType.PAY, message);
		  }
	  }

	  parseVisibleAdresses(a_mixNode);
	  parseListenerAdresses(a_mixNode);
	  
	  if (!a_bFromCascade) //info from cascade does not contain these infos, so no use parsing them
	  {
		  /* Parse the MixType */
		  Node typeNode =  XMLUtil.getFirstChildByName(a_mixNode, "MixType");
		  if (typeNode == null)
		  {
			  throw new XMLParseException("MixType");
		  }
		  m_type = parseMixType(typeNode.getFirstChild().getNodeValue());

		  /* Parse dynamic property */
		  m_dynamic = XMLUtil.parseValue(XMLUtil.getFirstChildByName(a_mixNode, "Dynamic"), false);

		  /* get the software information */

		  if (softwareNode == null)
		  {
			  throw (new XMLParseException("Software", m_mixId));
		  }

		  /* get LastUpdate information */

		  if (lastUpdateNode == null)
		  {
			  throw (new XMLParseException("LastUpdate", m_mixId));
		  }
		  m_lastUpdate = XMLUtil.parseValue(lastUpdateNode, 0L);

	  }
	  else
	  {
		  m_lastUpdate = System.currentTimeMillis() - Constants.TIMEOUT_MIX;
	  }
	  //no Exception if the node is null, since it's okay not to set it
	  //we'll just use 5 MB as a safe default
	  m_prepaidInterval = XMLUtil.parseValue(prepaidIntervalNode, AIControlChannel.MAX_PREPAID_INTERVAL / 1000) * 1000;

	  m_serial = XMLUtil.parseValue(lastUpdateNode, 0L);

	  if (softwareNode != null)
	  {
		  m_mixSoftware = new ServiceSoftware(softwareNode);
	  }

	  m_mixLocation = new ServiceLocation(locationNode, m_mixCertificate);
	  //get the Operator Certificate from the CertPath
	  if (m_mixCertPath != null)
	  {
		  m_mixOperator = new ServiceOperator(operatorNode, m_mixCertPath.getSecondCertificate(), m_lastUpdate);
	  }
	  else
	  {
		  m_mixOperator = new ServiceOperator(operatorNode, null, m_lastUpdate);
	  }
	  
	  m_strPerformanceServerHost = "";
	  m_iPerformanceServerPort = -1;
	  m_bPerformanceServer = false;
	  Node perfNode = XMLUtil.getFirstChildByName(a_mixNode, "PerformanceServer");
	  if(perfNode != null)
	  {
		  Node perfHostNode  = XMLUtil.getFirstChildByName(perfNode, "Host");
		  Node perfHostPort = XMLUtil.getFirstChildByName(perfNode, "Port");
		  
		  if(perfHostNode != null && perfHostPort != null)
		  {
			  m_strPerformanceServerHost = XMLUtil.parseValue(perfHostNode, "localhost");
			  m_iPerformanceServerPort = XMLUtil.parseValue(perfHostPort, 7777);
			  m_bPerformanceServer = true;
		  }
	  }

	  /*
	   * Store the Service Operator if
	   * - it doesn't exist yet or
	   * - the old certificate is null or
	   * - the certificate is newer than the old entry
	   * but only if the organization name is != null
	   */
	  ServiceOperator currentSO =
		  (ServiceOperator)Database.getInstance(ServiceOperator.class).getEntryById(m_mixOperator.getId());
	  if ( m_mixOperator.getOrganization() != null && ((currentSO == null || currentSO.getCertificate() == null) ||
		  (m_mixOperator.getCertificate() != null &&
		   m_mixOperator.getCertificate().getValidity().getValidTo().after(
			   currentSO.getCertificate().getValidity().getValidTo()))))
	  {
		  Database.getInstance(ServiceOperator.class).update(m_mixOperator);
	  }



	  /* as default no mix is free, only if we receive a configuration request from the mix and it
	   * it is not already assigned to a cascade, this mix will be free
	   */
	  m_freeMix = false;
	  m_xmlStructure = a_mixNode;


  }
  
  private void parseListenerAdresses(Node nodeMix) 
  {
	  parseVisibleAdresses(nodeMix, "ListenerInterfaces", "ListenerInterface", m_vecListenerAdresses);
  }
  private void parseVisibleAdresses(Node nodeMix)
  {
		Node nodeTmp = XMLUtil.getFirstChildByName(nodeMix, "Proxies");
		if (nodeTmp == null)
		{
			return;
		}
		nodeTmp = XMLUtil.getFirstChildByName(nodeTmp, "Proxy");
		while (nodeTmp != null)
		{
			if (nodeTmp.getNodeName().equals("Proxy"))
			{
				parseVisibleAdresses(nodeTmp, "VisibleAddresses", "VisibleAddress", m_vecVisibleAdresses);
			}
			nodeTmp=nodeTmp.getNextSibling();
		}
  }
  
  private void parseVisibleAdresses(Node nodeMix, String a_containerName, String a_nodeName, Vector a_storage)
  {
		Node nodeVisibleAddresses = XMLUtil.getFirstChildByName(nodeMix, a_containerName);
		Node nodeVisibleAddress = XMLUtil.getFirstChildByName(nodeVisibleAddresses, a_nodeName);
		while (nodeVisibleAddress != null)
		{
			if (nodeVisibleAddress.getNodeName().equals(a_nodeName))
			{
				Node nodeHost = XMLUtil.getFirstChildByName(nodeVisibleAddress, "Host");
				String strHost = XMLUtil.parseValue(nodeHost, null);
				if (strHost != null)
				{
					try
					{							
						InetAddress address = InetAddress.getByName(strHost);
						if (MixCascadeExitAddresses.isValidAddress(address) && 
							!a_storage.contains(address))
						{
							a_storage.addElement(address);
						}												
					}
					catch (UnknownHostException a_e)
					{
						LogHolder.log(LogLevel.INFO, LogType.NET, a_e);
					}
					catch (Exception e)
					{
						LogHolder.log(LogLevel.EXCEPTION, LogType.NET, e);
					}
				}
			}
			nodeVisibleAddress = nodeVisibleAddress.getNextSibling();
		}
  }

  /**
   * LERNGRUPPE
   * Parse the given textual MixType to our constants
   * @param nodeValue The textual MixType (FirstMix, MiddleMix, LastMix)
   * @return FIRST_MIX, MIDDLE_MIX or LAST_MIX
   * @throws XMLParseException
   */
  private int parseMixType(String nodeValue) throws XMLParseException
  {
      if("FirstMix".equals(nodeValue))
          return FIRST_MIX;

      if("MiddleMix".equals(nodeValue))
          return MIDDLE_MIX;

      if("LastMix".equals(nodeValue))
          return LAST_MIX;
      throw new XMLParseException("MixType", "Unkonwn type: " + nodeValue);
  }

  public Vector getVisibleAddresses()
  {
	  return (Vector)m_vecVisibleAdresses.clone();
  }
  
  public Vector getListenerAddresses()
  {
	  return (Vector)m_vecListenerAdresses.clone();
  }
  
  /**
   * Returns the ID of the mix.
   *
   * @return The ID of this mix.
   */
  public String getId() {
    return m_mixId;
  }

  public boolean isSocks5Supported()
  {
	  return m_bSocks;
  }

  /**
   * Returns if this MixInfo has been recevied directly from a cascade connection.
   * @return if this MixInfo has been recevied directly from a cascade connection
   */
  public boolean isFromCascade()
  {
	  return m_bFromCascade;
  }

  /**
   * Returns the time (see System.currentTimeMillis()), when the mix has sent this MixInfo to an
   * infoservice.
   *
   * @return The send time of this MixInfo from the mix.
   *
   */
  public long getLastUpdate() {
    return m_lastUpdate;
  }

  /**
   * Returns the time when this mix entry was created by the origin mix.
   *
   * @return A version number which is used to determine the more recent mix entry, if two
   *         entries are compared (higher version number -> more recent entry); 0 if no version number
   *         was found in this MixInfo object
   */
  public long getVersionNumber() {
    return m_serial;
  }

  /**
   * Returns the name of the mix.
   *
   * @return The name of this mix.
   */
  public String getName() {
    return m_name;
  }

  public boolean isVerified()
  {
	  if (m_mixSignature != null)
	  {
		  return m_mixSignature.isVerified();
	  }
	  return false;
  }

  public boolean isValid()
  {
	  if (m_mixCertPath != null)
	  {
		  return m_mixCertPath.checkValidity(new Date());
	  }
	  return false;
  }

  /**
   * Returns the certificate of the mix
   * For MixInfo-Objects in the InfoService the certificate is null
   * @return the certificate of the mix
   */
  public JAPCertificate getCertificate()
  {
	  return m_mixCertificate;
  }

  public JAPCertificate getOperatorCertificate()
  {
	  return m_operatorCertificate;
  }

  public XMLPriceCertificate getPriceCertificate()
  {
      return m_priceCert;
  }

  public long getPrepaidInterval()
  {
	  return m_prepaidInterval;
  }

  public void setPriceCertificate(XMLPriceCertificate newPriceCert)
  {
	  m_priceCert = newPriceCert;
  }

  /**
   * Returns the CertPath of the mix
   * For MixInfo-Objects in the InfoService the CertPath is null
   * @return the CertPath of the mix
   */
  public CertPath getCertPath()
  {
	  return m_mixCertPath;
  }
  /**
   * Returns the location of the mix.
   *
   * @return The location information for this mix.
   */
  public ServiceLocation getServiceLocation() {
    return m_mixLocation;
  }

  /**
   * Returns information about the operator of this mix.
   *
   * @return The operator information for this mix.
   */
  public ServiceOperator getServiceOperator() {
    return m_mixOperator;
  }

  /**
   * Returns information about the used software in this mix.
   *
   * @return The software information for this mix.
   */
  public ServiceSoftware getServiceSoftware() {
    return m_mixSoftware;
  }

  /**
   * Returns whether the mix is waiting for a cascade assignment. This value is only true, if the
   * mix is sending configure requests instead of HELO messages and if it is not already assigned
   * to a cascade. The returned value is only meaningful within the context of the infoservice.
   *
   * @return Whether this mix is currently free and can be assigned to a mixcascade.
   */
  public boolean isFreeMix() {
    return m_freeMix;
  }

  /**
   * Changes the state of this mix (whether it is free or not). If the mix is free, it will appear
   * in the list of free mixes. This mixes can be assigned to new cascades. If the specified value
   * is false, it will not appear in the list and cannot be assigned to new cascades. This value
   * is only meaningful within the context of the infoservice.
   *
   * @param a_freeMix Whether to treat this mix as free (true) or not (false).
   */
  public void setFreeMix(boolean a_freeMix) {
    m_freeMix = a_freeMix;
  }

  /**
   * This returns the filename (InfoService command), where this mix entry is posted at other
   * InfoServices. It's '/helo' if the mix is not treated as free of '/configure' if this mix
   * is currently free and needs to be assigned to a mixcascade.
   *
   * @return The filename where the information about this mix is posted at other infoservices
   *         when this entry is forwarded.
   */
  public String getPostFile() {
    String postFileName = "/helo";
    if (isFreeMix()) {
      postFileName = "/configure";
    }
    return postFileName;
  }



  /**
   * Returns the XML structure for this mix entry.
   *
   * @return The XML node for this mix entry (Mix node).
   */
  public Element getXmlStructure() {
    return m_xmlStructure;
  }


  /**
   * LERNGRUPPE
   * Returns the type of this mix
   * @return The type
   */
  public int getType()
  {
      return m_type;
  }

  /**
   * LERNGRUPPE
   * Returns the type of this mix
   * @return The type as  string
   */
  public String getTypeAsString()
  {
      switch(m_type) {
      case 0: return "First Mix";
      case 1: return "Middle Mix";
      case 2: return "Last Mix";
      default: return "Unknown type!";
      }
  }

  /**
   * LERNGRUPPE
   * Returns <code>true</code> if this mix is available for dynamic cascades,
   * <code>false</code> otherwise.
   * @return Returns <code>true</code> if this mix is available for dynamic cascades,
   * <code>false</code> otherwise.
   */
  public boolean isDynamic()
  {
    return m_dynamic;
  }


 /**
  * LERNGRUPPE
  * Extracts the host name from  first listenerinterface.
  * @return host
  * @throws Exception
  */
  public String getFirstHostName() throws Exception {

	  String result = "";
	  Element mix = this.getXmlStructure();
      NodeList listenerInterfacesNodes = mix.getElementsByTagName("ListenerInterfaces");
      if (listenerInterfacesNodes.getLength() == 0)
      {
          throw (new Exception("Mix has no ListenerInterfaces in its XML structure."));
      }
      Element listenerInterfacesNode = (Element) (listenerInterfacesNodes.item(0));
      NodeList listenerInterfaceNodes = listenerInterfacesNode
              .getElementsByTagName("ListenerInterface");
      if (listenerInterfaceNodes.getLength() == 0)
      {
          throw (new Exception("First Mix has no ListenerInterfaces in its XML structure."));
      }

      for (int i = 0; i < listenerInterfaceNodes.getLength(); i++)
      {
          Element listenerInterfaceNode = (Element) (listenerInterfaceNodes.item(i));
          result =   new ListenerInterface(listenerInterfaceNode).getHost();
          break;
      }
      return result;
  }

  /**
   * LERNGRUPPE
   * Extracts the port from  first listenerinterface.
   * @return host
   * @throws Exception
   */
   public int getFirstPort() throws Exception {

 	  int result = -1;
 	  Element mix = this.getXmlStructure();
       NodeList listenerInterfacesNodes = mix.getElementsByTagName("ListenerInterfaces");
       if (listenerInterfacesNodes.getLength() == 0)
       {
           throw (new Exception("Mix has no ListenerInterfaces in its XML structure."));
       }
       Element listenerInterfacesNode = (Element) (listenerInterfacesNodes.item(0));
       NodeList listenerInterfaceNodes = listenerInterfacesNode
               .getElementsByTagName("ListenerInterface");
       if (listenerInterfaceNodes.getLength() == 0)
       {
           throw (new Exception("First Mix has no ListenerInterfaces in its XML structure."));
       }

       for (int i = 0; i < listenerInterfaceNodes.getLength(); i++)
       {
           Element listenerInterfaceNode = (Element) (listenerInterfaceNodes.item(i));
           result =   new ListenerInterface(listenerInterfaceNode).getPort();
           break;
       }
       return result;
   }

	
	public boolean hasPerformanceServer()
	{
		return m_bPerformanceServer;
	}
	
	public String getPerformanceServerHost()
	{
		return m_strPerformanceServerHost;
	}
	
	public int getPerformanceServerPort()
	{
		return m_iPerformanceServerPort;
	}   
}
