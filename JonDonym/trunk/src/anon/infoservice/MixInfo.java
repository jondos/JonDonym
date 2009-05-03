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

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import anon.crypto.CertPath;
import anon.crypto.JAPCertificate;
import anon.crypto.MultiCertPath;
import anon.crypto.SignatureVerifier;
import anon.crypto.XMLSignature;
import anon.terms.TermsAndConditionsMixInfo;
import anon.util.IXMLEncodable;
import anon.util.XMLParseException;
import anon.util.XMLUtil;
import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;
import anon.crypto.IVerifyable;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Date;
import java.util.Hashtable;
import java.util.Vector;

import anon.pay.xml.XMLPriceCertificate;
import anon.pay.AIControlChannel;

/**
 * Holds the information of one single mix.
 */
public class MixInfo extends AbstractDistributableCertifiedDatabaseEntry implements IVerifyable, Database.IWebInfo
{
	public static final String NAME_TYPE_MIX = "Mix";
	public static final String NAME_TYPE_OPERATOR = "Operator";
	
	//public static final String DEFAULT_NAME = "Mix";
	
	public static final String XML_ELEMENT_CONTAINER_NAME = "Mixes";
	public static final String XML_ELEMENT_NAME = "Mix";
	public static final String XML_ELEMENT_MIX_NAME = "Name";
	public final static String XML_ATTRIBUTE_NAME_FOR_CASCADE = "forCascade";
	
	public static final String XML_ELEMENT_WEBINFO_CONTAINER = "MixWebInfos";
	public static final String INFOSERVICE_COMMAND_WEBINFOS = "/mixwebinfos";
	public static final String INFOSERVICE_COMMAND_WEBINFO = "/mixwebinfo/";
	
	private static final String XML_ELEMENT_WEBINFO = "MixWebInfo";
	private static final String XML_ELEM_SERVER_MONITORING = "ServerMonitoring";
	
	private static final String XML_ATTR_PAYMENT = "payment";
	

    /* LERNGRUPPE: Mix types */
    public static final int FIRST_MIX = 0;
    public static final int MIDDLE_MIX = 1;
    public static final int LAST_MIX = 2;

    /**
     * LERNGRUPPE
     * This is the type of the mix
     */
    private int m_type;
    
    private DataRetentionInformation m_drInfo;
    
    private boolean m_bPayment = false;

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
	
	private final Vector m_vecListenerInterfaces = new Vector();
	
	private final Vector m_vecListenerMonitoring = new Vector();
	
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
   * The name to contribute to the cascade name .
   */
  private String m_nameFragmentForCascade;
  
  private boolean m_bUseCascadeNameFragment = false;
  
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
   * Stores the certPath for this mix.
   * The CertPath is not set (null) if the MixInfo-Object is in the InfoService
   */
  private MultiCertPath m_mixCertPath;

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
  
  private TermsAndConditionsMixInfo m_mixTnCInfo = null;
  
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

  public MixInfo(MultiCertPath a_certPath)
  {
	  super(Long.MAX_VALUE);
	  if (a_certPath == null)
	  {
		  throw new IllegalArgumentException("No Mix cert path!");
	  }
	  
	  Vector certificates = new Vector();
	  Vector certPaths = a_certPath.getPaths();
	  for (int i = 0; i < certPaths.size(); i++)
	  {
		  certificates.addElement(((CertPath)certPaths.elementAt(i)).getFirstCertificate());
	  }
	  m_mixId = JAPCertificate.calculateXORofSKIs(certificates);
	  
	  m_name = a_certPath.getSubject().getCommonName();
	  if (m_name == null)
	  {
		  m_name = "Mix";
	  }
	  m_type = -1;
	  m_bFromCascade = true;
	  m_mixCertPath = a_certPath;
	  //m_mixCertificate = a_certPath.getEndEntity();
	  //m_operatorCertificate = a_certPath.getSecondCertificate();
	  m_lastUpdate = 0;
	  m_serial = 0;
	  CertPath path = a_certPath.getPath();
	  m_mixLocation = new ServiceLocation(null, path.getFirstCertificate());
	  m_mixOperator = new ServiceOperator(null, a_certPath, 0);
	  m_freeMix = false;
	  m_prepaidInterval = AIControlChannel.MAX_PREPAID_INTERVAL;
  }

  public MixInfo(String a_mixID, MultiCertPath a_certPath, XMLPriceCertificate a_priceCert, long a_prepaidInterval)
  {
	  super(Long.MAX_VALUE);
	  m_mixId = a_mixID;
	  m_name = a_mixID;
	  m_type = -1;
	  m_bFromCascade = true;
	  m_mixCertPath = a_certPath;
	  //m_mixCertificate = a_certPath.getFirstCertificate();
	  //m_operatorCertificate = a_certPath.getSecondCertificate();
	  m_lastUpdate = 0;
	  m_serial = 0;
	  CertPath path = a_certPath.getPath();
	  m_mixLocation = new ServiceLocation(null, path.getFirstCertificate());
	  m_mixOperator = new ServiceOperator(null, a_certPath, 0);
	  m_freeMix = false;
	  //
	  m_priceCert = a_priceCert;
	  if (m_priceCert != null)
	  {
		  m_bPayment = true;
	  }
	  m_prepaidInterval = a_prepaidInterval;
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
			  m_mixCertPath = m_mixSignature.getMultiCertPath();
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

	  if (a_expireTime < Long.MAX_VALUE) // only do this in InfoService context. Otherwise, we might get unwanted DNS requests.
	  {
		  parseVisibleAdresses(a_mixNode);
		  parseListenerAdresses(a_mixNode);
	  }

	  Element listenerInterfacesNode = 
		  (Element)XMLUtil.getFirstChildByName(a_mixNode, ListenerInterface.XML_ELEMENT_CONTAINER_NAME);	  
	  XMLUtil.assertNotNull(listenerInterfacesNode);
      NodeList listenerInterfaceNodes = listenerInterfacesNode.getElementsByTagName(ListenerInterface.XML_ELEMENT_NAME);
      if (listenerInterfaceNodes.getLength() == 0)
      {
          throw (new XMLParseException("First Mix has no ListenerInterfaces in its XML structure."));
      }
      for (int i = 0; i < listenerInterfaceNodes.getLength(); i++)
      {
    	  m_vecListenerInterfaces.addElement(new ListenerInterface((Element)listenerInterfaceNodes.item(i)));
      }
     
      Element elemMonitoring = (Element)XMLUtil.getFirstChildByName(a_mixNode, XML_ELEM_SERVER_MONITORING);
      if (elemMonitoring != null)
      {
    	  listenerInterfacesNode = 
    		  (Element)XMLUtil.getFirstChildByName(elemMonitoring, ListenerInterface.XML_ELEMENT_CONTAINER_NAME);	  
    	  if (listenerInterfacesNode != null)
    	  {
	          listenerInterfaceNodes = listenerInterfacesNode.getElementsByTagName(ListenerInterface.XML_ELEMENT_NAME);
	          for (int i = 0; i < listenerInterfaceNodes.getLength(); i++)
	          {
	        	  m_vecListenerMonitoring.addElement(new ListenerInterface((Element)listenerInterfaceNodes.item(i)));
	          }
    	  }
    	  else
    	  {
    		  // alternative listener description
    		  String host = XMLUtil.parseValue(XMLUtil.getFirstChildByName(elemMonitoring, ListenerInterface.XML_ELEM_HOST), null);
    		  int port = XMLUtil.parseValue(XMLUtil.getFirstChildByName(elemMonitoring, ListenerInterface.XML_ELEM_PORT), -1);
    		  if (host != null && port >= 0)
    		  {
    			  m_vecListenerMonitoring.addElement(new ListenerInterface(host, port));
    		  }
    	  }
      }
	  
	  if (!a_bFromCascade) //info from cascade does not contain these infos, so no use parsing them
	  {
		  /* Parse the MixType */
		  Node typeNode =  XMLUtil.getFirstChildByName(a_mixNode, "MixType");
		  XMLUtil.assertNotNull(typeNode);
	
		  m_type = parseMixType(XMLUtil.parseValue(typeNode, null));
		  m_bPayment = XMLUtil.parseAttribute(typeNode, XML_ATTR_PAYMENT, false);

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

	  Node tncInfoRoot = XMLUtil.getFirstChildByName(a_mixNode, TermsAndConditionsMixInfo.TNC_MIX_INFO_ROOT);
	  m_mixTnCInfo = (tncInfoRoot != null) ? new TermsAndConditionsMixInfo(tncInfoRoot) : null;
	  
	  if (softwareNode != null)
	  {
		  m_mixSoftware = new ServiceSoftware(softwareNode);
	  }
	  CertPath path = m_mixCertPath.getPath();
	  
	  //get the Mix and Operator Certificate from the CertPath
	  if (path != null)
	  {
		  m_mixLocation = new ServiceLocation(locationNode, path.getFirstCertificate());
		  m_mixOperator = new ServiceOperator(operatorNode, m_mixCertPath, m_lastUpdate);	
	  }
	  else
	  {
		  m_mixLocation = new ServiceLocation(locationNode, null);
		  m_mixOperator = new ServiceOperator(operatorNode, null, m_lastUpdate);
	  }
	  
	  
	  Node nodeDR = XMLUtil.getFirstChildByName(a_mixNode, DataRetentionInformation.XML_ELEMENT_NAME);
	  if (nodeDR != null)
	  {
		  // TODO patch for currently missing tag... Remove if no longer needed, that means when the mixes send it!
		  if (m_mixOperator.getOrganization() != null)
		  {
			  if (m_mixOperator.getOrganization().indexOf("JAP-Team") >= 0 ||
					  m_mixOperator.getOrganization().indexOf("Independent Centre") >= 0)
			  {				  
				  if (XMLUtil.getFirstChildByName(nodeDR, DataRetentionInformation.XML_ELEMENT_DESCRIPTION) == null)
				  {
					  nodeDR = XMLUtil.importNode(XMLUtil.createDocument(), nodeDR, true);
					  
					  Element elemDesc = nodeDR.getOwnerDocument().createElement(
							  DataRetentionInformation.XML_ELEMENT_DESCRIPTION);
					  XMLUtil.setAttribute(elemDesc, IXMLEncodable.XML_ATTR_LANGUAGE, "en");
					  Element elemURL = nodeDR.getOwnerDocument().createElement(
							  DataRetentionInformation.XML_ELEMENT_URL);
					 
					  XMLUtil.setValue(elemURL, "http://anon.inf.tu-dresden.de/dataretention_en.html");
					  elemDesc.appendChild(elemURL);
					  nodeDR.appendChild(elemDesc);
					  
					  elemDesc = nodeDR.getOwnerDocument().createElement(
							  DataRetentionInformation.XML_ELEMENT_DESCRIPTION);
					  XMLUtil.setAttribute(elemDesc, IXMLEncodable.XML_ATTR_LANGUAGE, "de");
					  elemURL = nodeDR.getOwnerDocument().createElement(
							  DataRetentionInformation.XML_ELEMENT_URL);
					  XMLUtil.setValue(elemURL, "http://anon.inf.tu-dresden.de/dataretention_de.html");
					  elemDesc.appendChild(elemURL);
					  nodeDR.appendChild(elemDesc);
				  }
			  }
		  }
		  
		  m_drInfo = new DataRetentionInformation((Element)nodeDR);
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
	  if (m_mixOperator.getOrganization() != null && ((currentSO == null || m_mixOperator.getOrganization() == null)) ||
		  currentSO.getCertPath() == null)
	  {
		  Database.getInstance(ServiceOperator.class).update(m_mixOperator);
	  }
	  else if (m_mixOperator.getCertPath() != null)		   
	  {
		  Vector vecNewCertPaths = m_mixOperator.getCertPath().getPaths();
		  Vector vecOldCertPaths = currentSO.getCertPath().getPaths();
		  JAPCertificate certNew, certCurrent;
		  
		  if (vecNewCertPaths.size() < vecOldCertPaths.size())
		  {
			  LogHolder.log(LogLevel.ALERT, LogType.DB, 
					  "Illegal DB object state: ServiceOperator object have same ID but different cert path lengths!");
			  Database.getInstance(ServiceOperator.class).update(m_mixOperator);
		  }
		  else
		  {		  
			  /* Look whether any of the "new" paths has a longer validity as the current paths */ 
			  for (int i = 0; i < vecNewCertPaths.size(); i++)
			  {
				  certNew = ((CertPath)vecNewCertPaths.elementAt(i)).getSecondCertificate();
				  certCurrent =  ((CertPath)vecOldCertPaths.elementAt(i)).getSecondCertificate();
				  if (certNew == null)
				  {
					  break;
				  }
				  else if (certCurrent == null || (certNew.getValidity().getValidTo().after(
						   certCurrent.getValidity().getValidTo())))
				  {
					  Database.getInstance(ServiceOperator.class).update(m_mixOperator);
					  break;
				  }
			  }
		  }
	  }



	  /* as default no mix is free, only if we receive a configuration request from the mix and it
	   * it is not already assigned to a cascade, this mix will be free
	   */
	  m_freeMix = false;
	  m_xmlStructure = a_mixNode;

	  Node nameNode = XMLUtil.getFirstChildByName(a_mixNode, XML_ELEMENT_MIX_NAME);
	  
	  m_name = XMLUtil.parseValue(nameNode, null);
	  
	  String nameType = XMLUtil.parseAttribute(nameNode, XML_ATTRIBUTE_NAME_FOR_CASCADE, "" );
	  
	  /*
	  if (m_name.equals("Euklid"))
	  {
		  nameType = NAME_TYPE_OPERATOR;
	  }*/
	  
	  if (nameType.equals(NAME_TYPE_OPERATOR) && (m_mixOperator != null))
	  {
		  m_nameFragmentForCascade = (m_mixOperator != null) ? m_mixOperator.getCommonName() : null;
		  m_bUseCascadeNameFragment = true;
	  }
	  else if (nameType.equals(NAME_TYPE_MIX) && (m_mixLocation != null) )
	  {
		  m_nameFragmentForCascade = m_mixLocation.getCommonName();
		  m_bUseCascadeNameFragment = true;
	  }
	  
	  if (m_nameFragmentForCascade != null && m_nameFragmentForCascade.equals("AN.ON Operator Certificate"))
	  {
		  if (m_mixLocation != null && m_mixLocation.getCommonName() != null && 
				  !m_mixLocation.getCommonName().startsWith("<Mix id="))
		  {
			  m_nameFragmentForCascade = m_mixLocation.getCommonName();
		  }
		  else 
		  {
			  m_nameFragmentForCascade = null;
		  }
	  }
	  
	  if (m_nameFragmentForCascade == null || m_nameFragmentForCascade.startsWith("<Mix id="))
	  {
		  if (m_name != null)
		  {
			  m_nameFragmentForCascade = m_name;
		  }
		  else
		  {
			  LogHolder.log(LogLevel.WARNING, LogType.MISC, 
					  "Could not set cascade name fragment for Mix!");
			  m_nameFragmentForCascade = "Unknown Mix";
		  }
	  }

	  
	  if (m_name == null)
	  {
		  if (m_mixLocation != null && m_mixLocation.getCommonName() != null && 
				  !m_mixLocation.getCommonName().startsWith("<Mix id="))
		  {
			  m_name = m_mixLocation.getCommonName();
		  }
		  else
		  {
			  m_name = m_nameFragmentForCascade;
		  }
	  }

	  //System.out.println("'" + m_nameFragmentForCascade + "'");
  }
  
  private void parseListenerAdresses(Node nodeMix) 
  {
	  parseVisibleAdresses(nodeMix, ListenerInterface.XML_ELEMENT_CONTAINER_NAME, 
			  ListenerInterface.XML_ELEMENT_NAME, m_vecListenerAdresses);
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
  
	public boolean isPersistanceDeletionAllowed()
	{
		return XMLUtil.getStorageMode() == XMLUtil.STORAGE_MODE_AGRESSIVE;
	}
	
	public void deletePersistence()
	{
		if (isPersistanceDeletionAllowed())
		{
			m_mixSignature = null;
			m_xmlStructure = null;
		}
	}
  

  public Vector getVisibleAddresses()
  {
	  return (Vector)m_vecVisibleAdresses.clone();
  }
  
  public Vector getListenerAddresses()
  {
	  return (Vector)m_vecListenerAdresses.clone();
  }
  
  public Vector getListenerInterfaces()
  {
	  return (Vector)m_vecListenerInterfaces.clone();
  }
  
  public Vector getMonitoringListenerInterfaces()
  {
	  return (Vector)m_vecListenerMonitoring.clone();
  }
  
  /**
   * Returns the ID of the mix.
   *
   * @return The ID of this mix.
   */
  public String getId() 
  {
    return m_mixId;
  }

  public boolean isSocks5Supported()
  {
	  return m_bSocks;
  }

  /**
   * Returns if this MixInfo has been received directly from a cascade connection.
   * @return if this MixInfo has been received directly from a cascade connection
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
  public String getName() 
  {
    return m_name;
  }
  
	public DataRetentionInformation getDataRetentionInformation()
	{
		return m_drInfo;
	}

  public boolean isVerified()
  {
	  if (m_mixCertPath != null)
	  {
		  return m_mixCertPath.isVerified();
	  }
	  return false;
  }

  public boolean isValid()
  {
	  if (m_mixCertPath != null)
	  {
		  return m_mixCertPath.isValid(new Date());
	  }
	  return false;
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
  public MultiCertPath getCertPath()
  {
	  return m_mixCertPath;
  }
  
  public XMLSignature getSignature()
  {
	  return m_mixSignature;
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
  public Element getXmlStructure() 
  {
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

  public boolean isPayment()
  {
	  return m_bPayment;
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
  public String getFirstHostName() throws Exception 
  {
	  ListenerInterface listenInterface;
	  for (int i = 0; i < m_vecListenerInterfaces.size(); i++)
	  {
		  listenInterface = (ListenerInterface)m_vecListenerInterfaces.elementAt(i);
		  if (!listenInterface.isHidden())
		  {
			  return listenInterface.getHost();
		  }
	  }
	   
	  return "";
  }

  /**
   * LERNGRUPPE
   * Extracts the port from  first listenerinterface.
   * @return host
   * @throws Exception
   */
   public int getFirstPort() throws Exception 
   {
	   ListenerInterface listenInterface;
	   for (int i = 0; i < m_vecListenerInterfaces.size(); i++)
	   {
		   listenInterface = (ListenerInterface)m_vecListenerInterfaces.elementAt(i);
		   if (!listenInterface.isHidden())
		   {
			   return listenInterface.getPort();
		   }
	   }
	   
	   return -1;
   }

   public boolean isCascadaNameFragmentUsed()
   {
	   return m_bUseCascadeNameFragment;
   }
   
	public String getNameFragmentForCascade()
	{
		return m_nameFragmentForCascade;
	}
	
	/*
	public void setNameFragmentForCascade(String fragmentForCascade) 
	{
		m_nameFragmentForCascade = fragmentForCascade;
	}*/
	
	public TermsAndConditionsMixInfo getTermsAndConditionMixInfo()
	{
		return m_mixTnCInfo;
	}
	
	public Element getWebInfo(Document webInfoDoc)
	{
		if (webInfoDoc == null)
		{
			return null;
		}
		
		Element rootElement = webInfoDoc.createElement(XML_ELEMENT_WEBINFO);
		XMLUtil.setAttribute(rootElement, XML_ATTR_PAYMENT, isPayment());
		XMLUtil.setAttribute(rootElement, XML_ATTR_ID, getId());
		/*
		if (getContext() != null)
		{
			XMLUtil.setAttribute(rootElement, XML_ATTR_CONTEXT, getContext());
		}*/
		
		Element currentMixOperatorElement = null;
		Element currentMixLocationElement = null;
		
		if(getCertPath() == null)
		{
			/* no valid document can be returned */
			return null;
		}

		
		XMLUtil.createChildElementWithValue(rootElement, XML_ELEMENT_MIX_NAME, getName());
		
		CertPath path = getCertPath().getPath();
		currentMixOperatorElement = new ServiceOperator(null, getCertPath(), 0l).toXMLElement(webInfoDoc);
		currentMixLocationElement = new ServiceLocation(null, path.getFirstCertificate()).toXMLElement(webInfoDoc);
		
		if(currentMixOperatorElement != null)
		{
			rootElement.appendChild(currentMixOperatorElement);
		}
		
		if(currentMixLocationElement != null)
		{
			rootElement.appendChild(currentMixLocationElement);
		}
		
		
		appendListenerInterfaces(rootElement, m_vecListenerInterfaces);
		if (m_vecListenerMonitoring.size() > 0)
		{
			appendListenerInterfaces(XMLUtil.createChildElement(rootElement, XML_ELEM_SERVER_MONITORING), m_vecListenerMonitoring);
		}
		
		
		rootElement.appendChild(m_mixCertPath.toXmlElement(webInfoDoc));
		
		return rootElement;
	}
	
	private void appendListenerInterfaces(Element a_rootElement, Vector a_listenerInterfaces)
	{
		Element listenerInterfaces = 
			XMLUtil.createChildElement(a_rootElement, ListenerInterface.XML_ELEMENT_CONTAINER_NAME);
		ListenerInterface listenerInterface;
		Element elemInterface;
		Hashtable hashInterfaceHosts = new Hashtable();
		Hashtable hashHiddenInterfaceHosts = new Hashtable();
		Hashtable hashVirtualInterfaceHosts = new Hashtable();
		for (int i = 0; i < a_listenerInterfaces.size(); i++)
		{
			listenerInterface = (ListenerInterface)a_listenerInterfaces.elementAt(i);
			if (listenerInterface.isHidden() && hashHiddenInterfaceHosts.containsKey(listenerInterface.getHost()))
			{
				elemInterface = (Element)hashHiddenInterfaceHosts.get(listenerInterface.getHost());
			}
			else if (listenerInterface.isVirtual() && hashVirtualInterfaceHosts.containsKey(listenerInterface.getHost()))
			{
				elemInterface = (Element)hashVirtualInterfaceHosts.get(listenerInterface.getHost());
			}
			else if (hashInterfaceHosts.containsKey(listenerInterface.getHost()))
			{
				elemInterface = (Element)hashInterfaceHosts.get(listenerInterface.getHost());
			}
			else
			{
				elemInterface = XMLUtil.createChildElement(listenerInterfaces, 
						ListenerInterface.XML_ELEMENT_NAME);
				if (listenerInterface.isVirtual())
				{
					XMLUtil.setAttribute(elemInterface, ListenerInterface.XML_ATTR_VIRTUAL, listenerInterface.isVirtual());
					hashVirtualInterfaceHosts.put(listenerInterface.getHost(), elemInterface);
				}
				else if (listenerInterface.isHidden())
				{
					XMLUtil.setAttribute(elemInterface, ListenerInterface.XML_ATTR_HIDDEN, listenerInterface.isHidden());
					hashHiddenInterfaceHosts.put(listenerInterface.getHost(), elemInterface);
				}
				else
				{
					hashInterfaceHosts.put(listenerInterface.getHost(), elemInterface);
				}
				XMLUtil.setAttribute(elemInterface, ListenerInterface.XML_ELEM_HOST, listenerInterface.getHost());				
			}
			
			if (listenerInterface.getProtocol() != ListenerInterface.PROTOCOL_TYPE_RAW_UNIX)
			{
				XMLUtil.createChildElementWithValue(elemInterface, 
						ListenerInterface.XML_ELEM_PORT, "" + listenerInterface.getPort());
			}
		}
	}
}
