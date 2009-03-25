/*
 Copyright (c) 2009, The JAP-Team, JonDos GmbH
 All rights reserved.
 Redistribution and use in source and binary forms, with or without modification,
 are permitted provided that the following conditions are met:

  - Redistributions of source code must retain the above copyright notice,
 this list of conditions and the following disclaimer.

  - Redistributions in binary form must reproduce the above copyright notice,
 this list of conditions and the following disclaimer in the documentation and/or
 other materials provided with the distribution.

  - Neither the name of the University of Technology Dresden, Germany, nor the name of
 the JonDos GmbH, nor the names of their contributors may be used to endorse or
 promote products derived from this software without specific prior written permission.


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

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import anon.util.IXMLEncodable;
import anon.util.XMLParseException;
import anon.util.XMLUtil;
import anon.util.XMLDuration;


/**
 * Holds information about the data retention policy of a mix or cascade.
 * 
 *     <!--Data retention related information. this element is given, if the Mix supports data retention-->
>>>     <LoggedElements>
>>>       <!--Describes which elements are logged-->
>>>       <InputTime>TRUE|FALSE</InputTime> <!--If TRUE, the time of packet arrival is logged. 
                                                If FALSE, only the time of connection to service is logged.-->
>>>       <OutputTime>TRUE|FALSE</OutputTime> <!--If TRUE, the time of packet sending / connection establishment is logged.
                                                  If false, only the time of disconnection from service is logged.-->
>>>       <InputChannelID>TRUE|FALSE</InputChannelID>  <!--If TRUE, the channel id of incoming packet is logged-->
>>>       <OutputChannelID>TRUE|FALSE</OutputChannelID> <!--If TRUE, the channel id of outgoing packet is logged-->
>>>       <InputSourceIPAddress>TRUE|FALSE</InputSourceIPAddress><!--If TRUE, the source IP address of incoming connection is logged-->
>>>       <InputSourceIPPort>TRUE|FALSE</InputSourceIPPort> <!--If TRUE, the source IP port of incoming connection is logged-->
>>>       <OutputSourceIPAddress>TRUE|FALSE</OutputSourceIPAddress> <!--If TRUE, the source IP address of incoming connection is logged-->
>>>       <OutputSourceIPPort>TRUE|FALSE</OutputSourceIPPort>    <!--If TRUE, the source IP port of outgoing connection is logged-->
>>>       <OutputTargetIPAddress>TRUE|FALSE</OutputIPAddress> <!-- If TRUE, the IP address of the contacted server is logged. -->
>>>       <OutputTargetDomain>TRUE|FALSE</OutputDomain> <!-- If TRUE, the Domain name of the requested web site is logged. -->
>>>     </LoggedElements>
>>>     <RetentionPeriod>
>>>       <!-- The period of time the logs are retained;
>>>       given as xs:duration (see: http://www.w3.org/TR/xmlschema-2/#duration)
>>>       -->
>>>     </RetentionPeriod>
>>>     <Description lang="LANGID"><!--Contains information about data retention, the information is specific for Languages/Countries specified in LANGID (according to XML standard) -->
>>>       <URL></URL><!--URL of a Web-Site with information about data retention-->
>>>     </Description>
 * 
 * 
 */
public class DataRetentionInformation
{
	public static final String XML_ELEMENT_NAME = "DataRetention";
	
	public static final String XML_ELEMENT_LOGGED_ELEMENTS = "LoggedElements";
	public static final String XML_ELEMENT_RETENTION_PERIOD = "RetentionPeriod";
	public static final String XML_ELEMENT_DESCRIPTION = "Description";
	public static final String XML_ELEMENT_URL = "URL";
	
	public static final int NOTHING = 1;
	public static final int INPUT_TIME = 2;
	public static final int OUTPUT_TIME = 4;
	public static final int INPUT_CHANNEL_ID = 8;
	public static final int OUTPUT_CHANNEL_ID = 16;
	public static final int INPUT_SOURCE_IP_ADDRESS = 32;
	public static final int INPUT_SOURCE_IP_PORT = 64;
	public static final int OUTPUT_SOURCE_IP_ADDRESS = 128;
	public static final int OUTPUT_SOURCE_IP_PORT = 256;
	public static final int OUTPUT_TARGET_IP_ADDRESS = 512;
	public static final int OUTPUT_TARGET_DOMAIN = 1024;
	
	private static final int[] FIELDS = 
	{INPUT_TIME, OUTPUT_TIME, INPUT_CHANNEL_ID, OUTPUT_CHANNEL_ID, 
		INPUT_SOURCE_IP_ADDRESS, INPUT_SOURCE_IP_PORT, OUTPUT_SOURCE_IP_ADDRESS,
		OUTPUT_SOURCE_IP_PORT, OUTPUT_TARGET_IP_ADDRESS, OUTPUT_TARGET_DOMAIN};
	
	private static final String[] FIELD_NAMES = 
	{"INPUT_TIME", "OUTPUT_TIME", "INPUT_CHANNEL_ID", "OUTPUT_CHANNEL_ID", 
		"INPUT_SOURCE_IP_ADDRESS", "INPUT_SOURCE_IP_PORT", "OUTPUT_SOURCE_IP_ADDRESS",
		"OUTPUT_SOURCE_IP_PORT", "OUTPUT_TARGET_IP_ADDRESS", "OUTPUT_TARGET_DOMAIN"};
	
	private XMLDuration m_duration;
	private Hashtable m_hashURLs = new Hashtable();
	private int m_loggedElements = NOTHING;
	
	public DataRetentionInformation(Element a_drNode) throws XMLParseException
	{
		Node nodeParent;
		
		XMLUtil.assertNodeName(a_drNode, XML_ELEMENT_NAME);
		nodeParent = XMLUtil.getFirstChildByName(a_drNode, XML_ELEMENT_LOGGED_ELEMENTS);
		
		m_loggedElements = 0;
		if (XMLUtil.parseValue(XMLUtil.getFirstChildByName(nodeParent, "InputTime"), false))
		{
			m_loggedElements += INPUT_TIME;
		}
		if (XMLUtil.parseValue(XMLUtil.getFirstChildByName(nodeParent, "OutputTime"), false))
		{
			m_loggedElements += OUTPUT_TIME;
		}
		if (XMLUtil.parseValue(XMLUtil.getFirstChildByName(nodeParent, "InputChannelID"), false))
		{
			m_loggedElements += INPUT_CHANNEL_ID;
		}
		if (XMLUtil.parseValue(XMLUtil.getFirstChildByName(nodeParent, "OutputChannelID"), false))
		{
			m_loggedElements += OUTPUT_CHANNEL_ID;
		}
		if (XMLUtil.parseValue(XMLUtil.getFirstChildByName(nodeParent, "InputSourceIPAddress"), false))
		{
			m_loggedElements += INPUT_SOURCE_IP_ADDRESS;
		}
		if (XMLUtil.parseValue(XMLUtil.getFirstChildByName(nodeParent, "InputSourceIPPort"), false))
		{
			m_loggedElements += INPUT_SOURCE_IP_PORT;
		}
		if (XMLUtil.parseValue(XMLUtil.getFirstChildByName(nodeParent, "OutputSourceIPAddress"), false))
		{
			m_loggedElements += OUTPUT_SOURCE_IP_ADDRESS;
		}
		if (XMLUtil.parseValue(XMLUtil.getFirstChildByName(nodeParent, "OutputSourceIPPort"), false))
		{
			m_loggedElements += OUTPUT_SOURCE_IP_PORT;
		}
		if (XMLUtil.parseValue(XMLUtil.getFirstChildByName(nodeParent, "OutputTargetIPAddress"), false))
		{
			m_loggedElements += OUTPUT_TARGET_IP_ADDRESS;
		}
		if (XMLUtil.parseValue(XMLUtil.getFirstChildByName(nodeParent, "OutputTargetDomain"), false))
		{
			m_loggedElements += OUTPUT_TARGET_DOMAIN;
		}
		if (m_loggedElements == 0)
		{
			m_loggedElements = NOTHING;
		}
		
		m_duration = new XMLDuration(XMLUtil.parseValue(XMLUtil.getFirstChildByName(a_drNode, 
				XML_ELEMENT_RETENTION_PERIOD), null));
		if (m_duration.getSign() < 0)
		{
			throw new XMLParseException("Negative retention duration is not allowed!");
		}
		
		NodeList listURLs = XMLUtil.getElementsByTagName(a_drNode, XML_ELEMENT_DESCRIPTION);
		if (listURLs != null)
		{
			for (int i = 0; i < listURLs.getLength(); i++)
			{
				try 
				{
					m_hashURLs.put(XMLUtil.parseAttribute(listURLs.item(i), IXMLEncodable.XML_ATTR_LANGUAGE, "en"), 
							new URL(XMLUtil.parseValue(XMLUtil.getFirstChildByName(listURLs.item(i), XML_ELEMENT_URL), null)));
				}
				catch (MalformedURLException e) 
				{
					throw new XMLParseException(e.getMessage());
				}
			}
		}
	}
	
	private DataRetentionInformation()
	{
		m_duration = new XMLDuration();
	}
	
	public static DataRetentionInformation getCascadeDataRetentionInformation(MixCascade a_cascade)
	{
		if (a_cascade == null)
		{
			return null;
		}
		
		Vector vecDrInfo = new Vector();
		DataRetentionInformation drInfo;
		
		for (int i = 0; i < a_cascade.getNumberOfMixes(); i++)
		{
			if (a_cascade.getMixInfo(i) == null ||
				a_cascade.getMixInfo(i).getDataRetentionInformation() == null)
			{
				if (i == 0)
				{
					// the first mix does no data retention -> it is useless!
					return null;
				}
				drInfo = new DataRetentionInformation();
			}
			else
			{
				drInfo = a_cascade.getMixInfo(i).getDataRetentionInformation();
			}
			vecDrInfo.addElement(drInfo);
		}

		return getCascadeDataRetentionInformation(vecDrInfo);
	}
	
	
	private static DataRetentionInformation getCascadeDataRetentionInformation(Vector a_dataRetentionInformations)
	{
		DataRetentionInformation drInfo = new DataRetentionInformation();
		DataRetentionInformation currentDrInfo;
		
		if (a_dataRetentionInformations == null)
		{
			return drInfo;
		}
				
		Vector vec = (Vector)a_dataRetentionInformations.clone();
		
		if (vec.size() == 0)
		{
			return drInfo;
		}
		
		currentDrInfo = (DataRetentionInformation)a_dataRetentionInformations.elementAt(0);
		drInfo.m_loggedElements = currentDrInfo.getLoggedElementIDs();
		drInfo.m_duration = currentDrInfo.m_duration;
		drInfo.m_hashURLs = (Hashtable)currentDrInfo.m_hashURLs.clone();		
		
		
		for (int i = 1; i < a_dataRetentionInformations.size(); i++)
		{
			currentDrInfo = ((DataRetentionInformation)a_dataRetentionInformations.elementAt(i));
			
			// take the minimum common log infos
			if (i == a_dataRetentionInformations.size() - 1) // last mix
			{
				drInfo.m_loggedElements &= (currentDrInfo.getLoggedElementIDs() | OUTPUT_CHANNEL_ID); // implicit
			}
			else
			{
				drInfo.m_loggedElements &= currentDrInfo.getLoggedElementIDs();
			}
			
			// take the shortest common duration
			if (drInfo.m_duration.isLongerThan(currentDrInfo.m_duration))
			{				
				drInfo.m_duration = currentDrInfo.m_duration;
			}
			
			// keep the URLs if they are all the same
			if (drInfo.m_hashURLs.size() == currentDrInfo.m_hashURLs.size())
			{
				Enumeration enumURLKeys = currentDrInfo.m_hashURLs.keys();
				Object key;
				while (enumURLKeys.hasMoreElements())
				{
					key = enumURLKeys.nextElement();
					if (!drInfo.m_hashURLs.containsKey(key) || 
						!drInfo.m_hashURLs.get(key).equals(currentDrInfo.m_hashURLs.get(key)))
					{
						drInfo.m_hashURLs.clear();
						break;
					}
				}
			}
			else
			{
				drInfo.m_hashURLs.clear();
			}
		}
		
		if (drInfo.m_loggedElements == 0)
		{
			drInfo.m_loggedElements = NOTHING;
		}
		
		return drInfo;
	}
	
	public static int getLoggedElementsLength()
	{
		return FIELDS.length;
	}
	
	public static int getLoggedElementID(int a_fieldIndex)
	{
		if (a_fieldIndex < 0 || a_fieldIndex > FIELDS.length)
		{
			return -1;
		}
		return FIELDS[a_fieldIndex];
	}
	
	public static String getLoggedElementName(int a_fieldIndex)
	{
		if (a_fieldIndex < 0 || a_fieldIndex > FIELD_NAMES.length)
		{
			return null;
		}
		return FIELD_NAMES[a_fieldIndex];
	}
	
	
	public boolean isLogged(int a_elementIDs)
	{
		return ((a_elementIDs & m_loggedElements) == a_elementIDs);
	}
	
	public int getLoggedElementIDs()
	{
		return m_loggedElements;
	}
	
	public URL getURL(String a_language)
	{
		URL url = (URL)m_hashURLs.get(a_language);
		if (url == null)
		{
			url = (URL)m_hashURLs.get("en");
		}
		return url;
	}
	
	public XMLDuration getDuration()
	{
		return m_duration;
	}
}
