/*
 Copyright (c) 2000 - 2004, The JAP-Team
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

/* Hint: This file may be only a copy of the original file which is always in the JAP source tree!
 * If you change something - do not forget to add the changes also to the JAP source tree!
 */

package anon.infoservice;

import java.util.StringTokenizer;
import java.util.Vector;

import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import anon.util.IXMLEncodable;
import anon.util.XMLParseException;
import anon.util.XMLUtil;

/**
 * Saves the information about a network server.
 */
public class ListenerInterface implements ImmutableListenerInterface, IXMLEncodable
{
	public static final int PORT_MIN_VALUE = 1;
	public static final int PORT_MAX_VALUE = 65535;

	public static final String XML_ELEMENT_NAME = "ListenerInterface";
	public static final String XML_ELEMENT_CONTAINER_NAME = "ListenerInterfaces";
	
	public static final String XML_ATTR_HIDDEN = "hidden";
	public static final String XML_ATTR_VIRTUAL = "virtual";
	
	public static final String XML_ELEM_HOST = "Host";
	public static final String XML_ELEM_PORT = "Port";
	public static final String XML_ELEM_FILE = "File";
	

	private long m_endOfBlocking = 0;

	/**
	 * This is the host of this interface (hostname or IP). It can be NULL indicating,
	 * that the hostname is unspecified. The user has to decide how to handle this (e.g.
	 * if this means 'any' IP of any interface or if whis means 'only localhost' etc.
	 */
	private String m_strHostname;

	/**
	 * This is the representation of the port of the ListenerInterface. Might be -1 to
	 * indicate that the port is unspecified.
	 */
	private int m_iInetPort;

	/**
	 * This describes the protocol type.
	 */
	private int m_iProtocolType;

	/**
	 * This describes, whether we can reach this interface or not. If we can't get a connection
	 * to this interface, we set it to false and so there are no more connection trys. The
	 * interface still remains in the database because others may get a connection to this
	 * interface (causes can be firewalls, another network, ...). This is only meaningful for
	 * non-local (remote) interfaces.
	 */
	private boolean m_bUseInterface = false;
	
	private boolean m_bVirtual = false;	
	private boolean m_bHidden = false;

	/**
	 * Creates a new ListenerInterface from XML description (ListenerInterface node).
	 * @todo remove the ip host
	 * @param listenerInterfaceNode The ListenerInterface node from an XML document.
	 * @exception XMLParseException if an error in the xml structure occurs
	 */
	public ListenerInterface(Element listenerInterfaceNode) throws XMLParseException
	{
		String strHostname;
		
		m_bHidden = XMLUtil.parseAttribute(listenerInterfaceNode, XML_ATTR_HIDDEN, false);
		m_bVirtual = XMLUtil.parseAttribute(listenerInterfaceNode, XML_ATTR_VIRTUAL, false);
		if (m_bVirtual && m_bHidden)
		{
			m_bHidden = false;
			m_bVirtual = false;
		}
		
		//Try Type and NetworkProtocol for backwards compatibility...
		String strProtocol = XMLUtil.parseValue(XMLUtil.getFirstChildByName(listenerInterfaceNode, "Type"), null);
		if (strProtocol == null)
		{
			strProtocol = XMLUtil.parseValue(XMLUtil.getFirstChildByName(listenerInterfaceNode,
				"NetworkProtocol"), null);
		}
		setProtocol(strProtocol);

		if (getProtocol() == PROTOCOL_TYPE_RAW_UNIX)
		{
			strHostname = XMLUtil.parseValue(XMLUtil.getFirstChildByName(listenerInterfaceNode, XML_ELEM_FILE), null);
		}
		else
		{
			Node hostNode = XMLUtil.getFirstChildByName(listenerInterfaceNode, XML_ELEM_HOST);
			Node ipNode = XMLUtil.getFirstChildByName(listenerInterfaceNode, "IP");
			if (hostNode == null && ipNode == null)
			{
				throw new XMLParseException("Host,IP", "Neither Host nor IP are given.");
			}
			//The value give in Host supersedes the one given by IP
			strHostname = XMLUtil.parseValue(hostNode, null);
			if (!isValidHostname(strHostname))
			{
				strHostname = XMLUtil.parseValue(ipNode, null);
				if (!isValidIP(strHostname))
				{
					throw new XMLParseException("Host, IP", "Invalid Host and IP.");
				}
			}
		}

		setHostname(strHostname);		
		setPort(XMLUtil.parseValue(XMLUtil.getFirstChildByName(listenerInterfaceNode, XML_ELEM_PORT), -1));

		setUseInterface(true);
	}

	/**
	 * Creates a new ListenerInterface from a hostname / IP address and a port. The
	 * protocol is set to HTTP/TCP.
	 *
	 * @param a_hostname The hostname or the IP address of this interface.
	 * @param a_port The port of this interface (1 <= port <= 65535).
	 * @exception IllegalArgumentException if an illegal host name or port was given
	 */
	public ListenerInterface(String a_hostname, int a_port) throws IllegalArgumentException
	{
		this(a_hostname, a_port, PROTOCOL_TYPE_HTTP);
	}

	/**
	 * Creates a new ListenerInterface from a String which is of the form
	 * hostname:port. (The hostname can also be omitted) The
	 * protocol is set to HTTP/TCP.
	 *
	 * @param a_strValue The string containing hostname and port.
	 * @exception IllegalArgumentException if an illegal host name or port was given
	 */
	public ListenerInterface(String a_strValue) throws IllegalArgumentException
	{
		if (a_strValue == null)
		{
			throw new IllegalArgumentException("Argument given to ListenInterface constructor is NULL!");
		}
		int delimiter = a_strValue.indexOf(":");
		int listenPort = -1;
		String listenHost = null;
			try
			{
				listenPort =
					Integer.parseInt(a_strValue.substring(delimiter + 1, a_strValue.length()));
			}
			catch (Exception a_e)
			{
				LogHolder.log(LogLevel.WARNING, LogType.MISC, "Could not parse listener port: ", a_e);
			}

		if (delimiter > 0)
		{
			// host and port are given
			listenHost = a_strValue.substring(0, delimiter);
		}
		setHostname(listenHost);
		setPort(listenPort);
		setProtocol(ListenerInterface.PROTOCOL_TYPE_HTTP);
		setUseInterface(true);
	}

	/**
	 * Creates a new ListenerInterface from a hostname / IP address, a port and a protocol
	 * information.
	 *
	 * @param a_hostname The hostname or the IP address of this interface.
	 * @param a_port The port of this interface (1 <= port <= 65535).
	 * @param a_protocol The protocol information. Invalid protocols are replaced by http.
	 * @exception IllegalArgumentException if an illegal host name, port or protocol was given
	 */
	public ListenerInterface(String a_hostname, int a_port, int a_protocol) throws IllegalArgumentException
	{
		setHostname(a_hostname);
		setPort(a_port);
		setProtocol(a_protocol);
		setUseInterface(true);
	}

	/**
	 * Returns if the given port is valid.
	 * @param a_port a port number
	 * @return true if the given port is valid; false otherwise
	 */
	public static boolean isValidPort(int a_port)
	{
		if ( (a_port < PORT_MIN_VALUE) || (a_port > PORT_MAX_VALUE))
		{
			return false;
		}
		return true;
	}

	/**
	 * Returns if the given protocol is valid web protocol and can be recognized by
	 * recognizeProtocol().
	 * @param a_protocol a web protocol
	 * @see #recognizeProtocol(String)
	 * @return true if the given protocol is valid web protocol; false otherwise
	 */
	public static boolean isValidProtocol(String a_protocol)
	{
		return recognizeProtocol(a_protocol) != PROTOCOL_TYPE_UNKNOWN;
	}

	/**
	 * Returns if the given protocol is valid web protocol and can be recognized by
	 * recognizeProtocol().
	 * @param a_protocol a web protocol
	 * @see #recognizeProtocol(String)
	 * @return true if the given protocol is valid web protocol; false otherwise
	 */
	public static boolean isValidProtocol(int a_protocol)
	{
		return recognizeProtocol(a_protocol) != PROTOCOL_TYPE_UNKNOWN;
	}

	/**
	 * Returns if the given host name is valid.
	 * @param a_hostname a host name
	 * @return true if the given host name is valid; false otherwise
	 */
	public static boolean isValidHostname(String a_hostname)
	{
		return ( (a_hostname != null) && a_hostname.length() > 0);
	}

	/**
	 * Returns if the given IP address is valid.
	 * @param a_ipAddress an IP address
	 * @return true if the given IP address is valid; false otherwise
	 */
	public static boolean isValidIP(String a_ipAddress)
	{
		StringTokenizer tokenizer;

		if ( (a_ipAddress == null) || (a_ipAddress.indexOf('-') != -1))
		{
			return false;
		}

		tokenizer = new StringTokenizer(a_ipAddress, ".");

		try
		{
			// test if the IP could be IPv4 or IPv6
			if ( (tokenizer.countTokens() != 4) && (tokenizer.countTokens() != 16))
			{
				throw new NumberFormatException();
			}

			while (tokenizer.hasMoreTokens())
			{
				if (new Integer(tokenizer.nextToken()).intValue() > 255)
				{
					throw new NumberFormatException();
				}
			}
		}
		catch (NumberFormatException a_e)
		{
			return false;
		}

		return true;
	}

	/**
	 * Gets the protocol of this ListenerInterface.
	 * @return the protocol of this ListenerInterface
	 */
	public int getProtocol()
	{
		return m_iProtocolType;
	}

	/**
	 * Gets the protocol of this ListenerInterface as String.
	 * @return the protocol of this ListenerInterface
	 */
	public String getProtocolAsString()
	{
		switch (m_iProtocolType)
		{
			case PROTOCOL_TYPE_RAW_TCP:
				return PROTOCOL_STR_TYPE_RAW_TCP;
			case PROTOCOL_TYPE_SOCKS:
				return PROTOCOL_STR_TYPE_SOCKS;
			case PROTOCOL_TYPE_HTTPS:
				return PROTOCOL_STR_TYPE_HTTPS;
			case PROTOCOL_TYPE_HTTP:
				return PROTOCOL_STR_TYPE_HTTP;
			case PROTOCOL_TYPE_RAW_UNIX:
				return PROTOCOL_STR_TYPE_RAW_UNIX;
			default:
				return PROTOCOL_STR_TYPE_UNKNOWN;
		}
	}

	/**
	 * Get the host (hostname or IP) of this interface as a String.
	 *
	 * @return The host of this interface.
	 */
	public String getHost()
	{
		return m_strHostname;
	}

	/**
	 * Get the port of this interface.
	 * @return The port of this interface.
	 */
	public int getPort()
	{
		return m_iInetPort;
	}

	/**
	 * Tests if two ListenerInterface instances are equal.
	 * @param a_listenerInterface a ListenerInterface
	 * @return true if the two ListenerInterface instances are equal; false otherwise
	 */
	public boolean equals(ListenerInterface a_listenerInterface)
	{

		if (getHost() == null)
		{
			if (a_listenerInterface.getHost() != null)
			{
				return false;
			}
		}
		else
		{
			if (!getHost().equals(a_listenerInterface.getHost()))
			{
				return false;
			}
		}

		if (getPort() != a_listenerInterface.getPort() ||
			getProtocol() != a_listenerInterface.getProtocol())
		{
			return false;
		}
		return true;
	}

	/**
	 * Creates an XML node without signature for this ListenerInterface..
	 * @param a_doc The XML document, which is the environment for the created XML node.
	 * @return The ListenerInterface XML node.
	 */
	public Element toXmlElement(Document a_doc)
	{
		return toXmlElementInternal(a_doc, XML_ELEMENT_NAME);
	}

	/**
	 * Sets if this interface is used or not. If it is not used, further connection
	 * retries are prevented for a predefined amount of time
	 * @param a_bUseInterface true if this interface is used; false otherwise
	 */
	public void setUseInterface(boolean a_bUseInterface)
	{
		m_bUseInterface = a_bUseInterface;
		if (a_bUseInterface)
		{
			m_endOfBlocking = 0;
		}
	}

	/**
	 * Blocks this interface for a specified time. This is useful if the service is temporarily not
	 * reachable.
	 * @param a_blockingTime long
	 */
	public void blockInterface(long a_blockingTime)
	{
		m_endOfBlocking = System.currentTimeMillis() + a_blockingTime;
	}

	/**
	 * Get the validity of this interface. If it is not valid, further connection
	 * retries are prevented.
	 * @return Whether this interface is valid or not.
	 */
	public boolean isValid()
	{
		return isValidPort(getPort()) && isValidHostname(getHost()) && m_bUseInterface &&
			(m_endOfBlocking < System.currentTimeMillis());
	}
	
	public boolean isVirtual()
	{
		return m_bVirtual;
	}
	
	public boolean isHidden()
	{
		return m_bHidden;
	}

	/**
	 * Returns a String equal to getHost(). If getHost() is an IP, we try to find the hostname
	 * and add it in brackets. If getHost() is a hostname, we try to find the IP and add
	 * it in brackets. If we can't resolve getHost() (IP or hostname), only getHost() without
	 * the additional information is returned.
	 *
	 * @return The host of this interface with additional information.
	 */
	/*public String getHostAndIp()
	  {
	 String hostAndIp = m_strHostname;
	 try
	 {
	  InetAddress interfaceAddress = InetAddress.getByName(m_strHostname);
	  if (isValidIP(m_strHostname))
	  {
	   // inetHost is an IP, try to add the hostname
	   String hostName = interfaceAddress.getHostName();
	   if ( (!hostName.equals(m_strHostname)) && (isValidHostname(hostName)))
	   {
	 // we got the hostname via DNS, add it
	 hostAndIp = hostAndIp + " (" + hostName + ")";
	   }
	  }
	  else
	  {
	   // inetHost is a hostname, add the IP
	   hostAndIp = hostAndIp + " (" + interfaceAddress.getHostAddress() + ")";
	  }
	 }
	 catch (java.net.UnknownHostException e)
	 {
	  // can't resolve inetHost, maybe we are behind a proxy, return only inetHost
	 }
	 return hostAndIp;
	  }
	 */


	/**
	 * Transforms a given protocol into a valid protocol if recognized.
	 * @param a_protocol a protocol
	 * @return the protocol (my be transformed in some way) or PROTOCOL_TYPE_UNKNOWN if not recognized
	 */
	protected static int recognizeProtocol(String a_protocol)
	{
		int iProtocol = PROTOCOL_TYPE_UNKNOWN;

		if (a_protocol != null)
		{
			if (a_protocol.equalsIgnoreCase(PROTOCOL_STR_TYPE_HTTP))
			{
				iProtocol = PROTOCOL_TYPE_HTTP;
			}
			else if (a_protocol.equalsIgnoreCase(PROTOCOL_STR_TYPE_HTTPS))
			{
				iProtocol = PROTOCOL_TYPE_HTTPS;
			}
			else if (a_protocol.equalsIgnoreCase(PROTOCOL_STR_TYPE_SOCKS))
			{
				iProtocol = PROTOCOL_TYPE_SOCKS;
			}
			else if (a_protocol.equalsIgnoreCase(PROTOCOL_STR_TYPE_RAW_TCP))
			{
				iProtocol = PROTOCOL_TYPE_RAW_TCP;
			}
			else if (a_protocol.equalsIgnoreCase(PROTOCOL_STR_TYPE_RAW_UNIX))
			{
				iProtocol = PROTOCOL_TYPE_RAW_UNIX;
			}
		}

		return iProtocol;
	}

	/**
	 * Transforms a given protocol into a valid protocol if recognized.
	 * @param a_protocol a protocol
	 * @return the protocol (my be transformed in some way) or PROTOCOL_TYPE_UNKNOWN if not recognized
	 */
	protected static int recognizeProtocol(int a_protocol)
	{
		if (a_protocol == PROTOCOL_TYPE_HTTP ||
			a_protocol == PROTOCOL_TYPE_HTTPS ||
			a_protocol == PROTOCOL_TYPE_RAW_TCP ||
			a_protocol == PROTOCOL_TYPE_SOCKS)
		{
			return a_protocol;
		}
		return PROTOCOL_TYPE_UNKNOWN;
	}

	/**
	 * Sets the protocol. If it is invalid, it is set to PROTOCOL_TYPE_RAW_TCP.
	 * @param a_protocol a protocol
	 */
	public void setProtocol(String a_protocol)
	{
		if (!isValidProtocol(a_protocol))
		{
			if (isValidHostname(getHost()))
			{
				LogHolder.log(LogLevel.NOTICE, LogType.NET, "Host " + getHost() + " has listener with " +
							  "invalid protocol '" + a_protocol + "'!");
			}
			m_iProtocolType = PROTOCOL_TYPE_RAW_TCP;
		}
		else
		{
			m_iProtocolType = recognizeProtocol(a_protocol);
		}
	}

	/**
	 * Sets the protocol. If it is invalid, it is set to PROTOCOL_TYPE_RAW_TCP.
	 * @param a_protocol a protocol
	 */
	public void setProtocol(int a_protocol)
	{
		if (!isValidProtocol(a_protocol))
		{
			if (isValidHostname(getHost()))
			{
				LogHolder.log(LogLevel.NOTICE, LogType.NET, "Host " + getHost() + " has listener with " +
							  "invalid protocol '" + a_protocol + "'!");
			}
			m_iProtocolType = PROTOCOL_TYPE_RAW_TCP;
		}
		else
		{
			m_iProtocolType = recognizeProtocol(a_protocol);
		}
	}

	/**
	 * Sets the port number.
	 * @param a_port a port number
	 */
	public void setPort(int a_port)
	{
		if (!isValidPort(a_port))
		{
			m_iInetPort = -1;
		}
		else
		{
			m_iInetPort = a_port;
		}
	}

	/**
	 * Sets the host name.
	 * @param a_strHostname a host name
	 * @todo This method seems to be bugy!
	 */
	public void setHostname(String a_strHostname)
	{
		if (!isValidHostname(a_strHostname))
		{
			//throw (new IllegalArgumentException("ListenerInterface: Host is invalid."));
			String s=a_strHostname;
			if (a_strHostname == null)
			{
				s = "NULL";
			}
			LogHolder.log(LogLevel.NOTICE, LogType.NET, "Invalid host name: '" + s + "'");
		}
		m_strHostname = a_strHostname;
	}

	/**
	 * Creates a Vector of listeners with only the current listener.
	 * @return a Vector of listeners with only the current listener
	 */
	public Vector toVector()
	{
		Vector listeners = new Vector();
		listeners.addElement(this);

		return listeners;
	}

	/**
	 * Creates an XML node without signature for this ListenerInterface.
	 *
	 * @todo Remove the parts that contruct the tag <IP> and the ipnode respectively.
	 *       They are used by InfoService:MixCascadeDBEntry
	 *       and are only needed for compatibility with JAP < 00.02.034.
	 *
	 * @param doc The XML document, which is the environment for the created XML node.
	 * @param a_strXmlElementName the name of the xml element to create
	 * @return The ListenerInterface XML node.
	 */
	protected Element toXmlElementInternal(Document doc, String a_strXmlElementName)
	{
		Element listenerInterfaceNode = doc.createElement(a_strXmlElementName);
		/* Create the child nodes of ListenerInterface (Type, Port, Host) */
		Element typeNode = doc.createElement("Type");
		Element hostNode;
		Element portNode = null;
		XMLUtil.setValue(typeNode, getProtocolAsString());
		if (getProtocol() == PROTOCOL_TYPE_RAW_UNIX)
		{
			hostNode = doc.createElement(XML_ELEM_FILE);
			XMLUtil.setValue(hostNode, m_strHostname);
		}
		else
		{
			portNode = doc.createElement(XML_ELEM_PORT);
			XMLUtil.setValue(portNode, m_iInetPort);
			hostNode = doc.createElement(XML_ELEM_HOST);
			XMLUtil.setValue(hostNode, m_strHostname);
		}
		/*String ipString = null;
		   try
		   {
		 InetAddress interfaceAddress = InetAddress.getByName(m_strHostname);
		 ipString = interfaceAddress.getHostAddress();
		   }
		   catch (Exception e)
		   {
		 // maybe inetHost is a hostname and no IP, but this solution is better than nothing
		 ipString = m_strHostname;
		   }
		   Element ipNode = doc.createElement("IP");
		   ipNode.appendChild(doc.createTextNode(ipString));
		 */
		if (m_bHidden)
		{
			XMLUtil.setAttribute(listenerInterfaceNode, XML_ATTR_HIDDEN, m_bHidden);
		}
		else if (m_bVirtual)
		{
			XMLUtil.setAttribute(listenerInterfaceNode, XML_ATTR_VIRTUAL, m_bVirtual);
		}

		listenerInterfaceNode.appendChild(typeNode);
		if (portNode != null)
		{
			listenerInterfaceNode.appendChild(portNode);
		}
		listenerInterfaceNode.appendChild(hostNode);
		//listenerInterfaceNode.appendChild(ipNode);
		return listenerInterfaceNode;
	}

	public String toString()
	{
		return "ListenerInterface (Protocol: " + getProtocolAsString() + ")- Host: " + getHost() + " Port: " +
			getPort();
	}

}
