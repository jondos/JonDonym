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
package jap.forward;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import anon.util.JAPMessages;
import anon.util.XMLUtil;
import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;
import jap.*;
import gui.*;

/**
 * This is the implementation of a structure, which stores the paramters of a connection class,
 * like ISDN, DSL, ...
 */
public class JAPRoutingConnectionClass
{

	/**
	 * Stores an identifier for this connection class. See the constants in
	 * JAPRoutingConnectionClassSelector.
	 *
	 * @see JAPRoutingConnectionClassSelector
	 */
	private int m_connectionClassIdentifier;

	/**
	 * Stores the name of the connection class. This name is displayed in the GUI, so it should be
	 * an identifier for JAPMessages.
	 */
	private String m_connectionClassName;

	/**
	 * Stores the maximal possible bandwidth for this connection class in bytes/sec.
	 */
	private int m_maximumBandwidth;

	/**
	 * Stores the percentage of the maximum bandwidth of the connection which shall be used for
	 * forwarding.
	 */
	private int m_relativeBandwidth;

	/**
	 * Creates a new JAPRoutingConnectionClass structure. The number is depending on
	 * a_currentBandwidth set to the maximum possible value according to
	 * JAPConstants.ROUTING_BANDWIDTH_PER_USER.
	 *
	 * @param a_connectionClassIdentifier The identifier for this connection class. See the
	 *                                    constants in JAPRoutingConnectionClassSelector.
	 * @param a_connectionClassName The name for the connection class, which is displayed in the
	 *                              GUI. So it should be an identifier for JAPMessages.
	 * @param a_maximumBandwidth The maximum possible bandwidth for this connection class in
	 *                           bytes/sec.
	 * @param a_currentBandwidth The bandwidth which can be used for the forwarding server with
	 *                           this connection class.
	 */
	public JAPRoutingConnectionClass(int a_connectionClassIdentifier, String a_connectionClassName,
									 int a_maximumBandwidth, int a_relativeBandwidth)
	{
		m_connectionClassIdentifier = a_connectionClassIdentifier;
		m_connectionClassName = a_connectionClassName;
		m_maximumBandwidth = a_maximumBandwidth;
		setRelativeBandwidth(a_relativeBandwidth);
	}

	/**
	 * Returns the identifier for this connection class. See the constants in
	 * JAPRoutingConnectionClassSelector.
	 *
	 * @return The identifier of this connection class.
	 *
	 * @see JAPRoutingConnectionClassSelector
	 */
	public int getIdentifier()
	{
		return m_connectionClassIdentifier;
	}

	/**
	 * Returns the maximum bandwidth of this connection class.
	 *
	 * @return The maximum bandwidth of this connection class.
	 */
	public int getMaximumBandwidth()
	{
		return m_maximumBandwidth;
	}

	/**
	 * Changes the maximum bandwidth of this connection class. This is only possible for the
	 * user-defined connection class. Any other connection class will ignore a call of this method.
	 *
	 * @param a_maximumBandwidth The new maximum bandwidth (in bytes/sec), for this connection
	 *                           class.
	 */
	public void setMaximumBandwidth(int a_maximumBandwidth)
	{
		if (m_connectionClassIdentifier == JAPRoutingConnectionClassSelector.CONNECTION_CLASS_USER)
		{
			synchronized (this)
			{
				m_maximumBandwidth = a_maximumBandwidth;
				/* set the useable bandwidth to the smallest possible value above the current one */
				setRelativeBandwidth(getRelativeBandwidth());
			}
		}
	}

	/**
	 * Returns the current maximum bandwidth, which can be used for the forwarding server with this
	 * connection class (= relative bandwidth * maximum bandwidth).
	 *
	 * @return The current maximum bandwidth in bytes/sec, which can be used for the forwarding
	 *         server with this connection class.
	 */
	public int getCurrentBandwidth()
	{
		int currentAbsoluteBandwidth = 0;
		synchronized (this)
		{
			currentAbsoluteBandwidth = (m_maximumBandwidth * m_relativeBandwidth) / 100;
		}
		return currentAbsoluteBandwidth;
	}

	/**
	 * Returns the maximum number of simultaneous connections, the forwarding server can handle
	 * with the current bandwidth. The value depends on JAPConstants.ROUTING_BANDWIDTH_PER_USER.
	 *
	 * @return The maximum number of simultaneous forwarded connections, the server can handle with
	 *         the current bandwidth.
	 */
	public int getMaxSimultaneousConnections()
	{
		return (getCurrentBandwidth() / JAPConstants.ROUTING_BANDWIDTH_PER_USER);
	}

	/**
	 * Returns the percentage of the maximum bandwidth which shall be used for forwarding.
	 *
	 * @return The relative connection bandwidth (in %) useable for forwarding.
	 */
	public int getRelativeBandwidth()
	{
		return m_relativeBandwidth;
	}

	/**
	 * Changes the percentage of the maximum bandwidth which shall be used for forwarding. The new
	 * value must not be smaller than the value returned by getMinimumRelativeBandwidth() (if it is
	 * smaller, the value returned by getMinimumRelativeBandwidth() is set as the new relative
	 * bandwidth).
	 *
	 * @param a_relativeBandwidth The relative connection bandwidth (in %) useable for forwarding.
	 */
	public void setRelativeBandwidth(int a_relativeBandwidth)
	{
		synchronized (this)
		{
			if (a_relativeBandwidth > getMinimumRelativeBandwidth())
			{
				m_relativeBandwidth = a_relativeBandwidth;
			}
			else
			{
				/* less than the minimum bandwidth value is not possible */
				m_relativeBandwidth = getMinimumRelativeBandwidth();
			}
		}
	}

	/**
	 * Returns the minimum relative forwarding bandwidth. This is the percentage of the maximum
	 * bandwidth necessary to forward at least one connection, see
	 * JAPConstants.ROUTING_BANDWIDTH_PER_USER).
	 *
	 * @return The minimum possible relative connection bandwidth (in %).
	 */
	public int getMinimumRelativeBandwidth()
	{
		int maximumBandwidth = m_maximumBandwidth;
		/* the +(maximumBandwidth-1) is only for rounding up the result */
		return ( (JAPConstants.ROUTING_BANDWIDTH_PER_USER * 100) + (maximumBandwidth - 1)) / maximumBandwidth;
	}

	/**
	 * Returns the name of this connection class. If it is an identifier for JAPMessages, the String
	 * after resolving the identifier is returned.
	 *
	 * @return The name of this connection class, which can be used in the GUI.
	 */
	public String toString()
	{
		return JAPMessages.getString(m_connectionClassName);
	}

	/**
	 * Returns the settings for this connection class (bandwidth settings) for storage within an XML
	 * document.
	 *
	 * @param a_doc The context document for the connection class settings.
	 *
	 * @return An XML node (ConnectionClass) with all settings of this connection class.
	 */
	public synchronized Element getSettingsAsXml(Document a_doc)
	{
		Element connectionClassNode = a_doc.createElement("ConnectionClass");
		Element classIdentifierNode = a_doc.createElement("ClassIdentifier");
		Element maximumBandwidthNode = a_doc.createElement("MaximumBandwidth");
		Element relativeBandwidthNode = a_doc.createElement("RelativeBandwidth");
		XMLUtil.setValue(classIdentifierNode, getIdentifier());
		//synchronized (this) Deadly for JDK 1.1.8
		{
			XMLUtil.setValue(maximumBandwidthNode, getMaximumBandwidth());
			XMLUtil.setValue(relativeBandwidthNode, getRelativeBandwidth());
		}
		connectionClassNode.appendChild(classIdentifierNode);
		connectionClassNode.appendChild(maximumBandwidthNode);
		connectionClassNode.appendChild(relativeBandwidthNode);
		return connectionClassNode;
	}

	/**
	 * This method loads some settings for this connection class from a prior created XML structure.
	 * But the identifier and the maximum bandwidth (both belong to the characteristics of this
	 * connection class) are normally not changed, but checked, whether they match to this
	 * connection class. Only the user-defined connection class will change the maximum bandwidth
	 * value to the loaded setting. If there is an error while loading the settings, it is still
	 * tried to load as much settings as possible.
	 *
	 * @param a_connectionClassNode The ConnectionClass XML node, which was created by the
	 *                              getSettingsAsXml() method.
	 *
	 * @return True, if there was no error while loading the settings and false, if there was one.
	 */
	public boolean loadSettingsFromXml(Element a_connectionClassNode)
	{
		/* store, whether there were some errors while loading the settings */
		boolean noError = true;
		synchronized (this)
		{
			try
			{
				/* check, whether the class identifier matches to the connection class */
				if (XMLUtil.parseValue(XMLUtil.getFirstChildByName(a_connectionClassNode, "ClassIdentifier"),
									   m_connectionClassIdentifier + 1) != m_connectionClassIdentifier)
				{
					throw (new Exception(
						"JAPRoutingConnectionClass: loadSettingsFromXml: The class identifer doesn't match to this class (class: " +
						Integer.toString(m_connectionClassIdentifier) + ")."));
				}
				if (m_connectionClassIdentifier == JAPRoutingConnectionClassSelector.CONNECTION_CLASS_USER)
				{
					/* user-defined connection class -> load the maximum bandwidth setting */
					int maximumBandwidth = XMLUtil.parseValue(XMLUtil.getFirstChildByName(
						a_connectionClassNode, "MaximumBandwidth"), -1);
					if (maximumBandwidth >= JAPConstants.ROUTING_BANDWIDTH_PER_USER)
					{
						/* maximum bandwidth needs to be at least the same as required for one forwarded
						 * connection
						 */
						m_maximumBandwidth = maximumBandwidth;
						/* set the useable bandwidth to 50% or to the smallest possible value above 50% */
						setRelativeBandwidth(50);
					}
					else
					{
						throw (new Exception(
							"JAPRoutingConnectionClass: loadSettingsFromXml: Invalid maximum bandwidth value (class: " +
							Integer.toString(m_connectionClassIdentifier) + ")."));
					}
				}
				else
				{
					/* pre-defined connection class -> check whether the maximum bandwidth setting matches */
					if (XMLUtil.parseValue(XMLUtil.getFirstChildByName(a_connectionClassNode,
						"MaximumBandwidth"), m_maximumBandwidth + 1) != m_maximumBandwidth)
					{
						throw (new Exception(
							"JAPRoutingConnectionClass: loadSettingsFromXml: The maximum bandwidth doesn't match to this class (class: " +
							Integer.toString(m_connectionClassIdentifier) + ")."));
					}
				}
			}
			catch (Exception e)
			{
				LogHolder.log(LogLevel.ERR, LogType.NET,
					"JAPRoutingConnectionClass: loadSettingsFromXml: Loading the settings for this connection class failed: " +
							  e.toString());
				noError = false;
			}
			if (noError = true)
			{
				/* only load the settings, if everything is ok */
				Element relativeBandwidthNode = (Element) (XMLUtil.getFirstChildByName(a_connectionClassNode,
					"RelativeBandwidth"));
				if (relativeBandwidthNode == null)
				{
					LogHolder.log(LogLevel.ERR, LogType.MISC,
						"JAPRoutingConnectionClass: loadSettingsFromXml: Error in XML structure (RelativeBandwidth node for class " +
								  Integer.toString(m_connectionClassIdentifier) + "): Using default value.");
					noError = false;
				}
				else
				{
					int relativeBandwidth = XMLUtil.parseValue(relativeBandwidthNode, -1);
					if (relativeBandwidth < getMinimumRelativeBandwidth())
					{
						LogHolder.log(LogLevel.ERR, LogType.MISC,
							"JAPRoutingConnectionClass: loadSettingsFromXml: Invalid relative bandwidth value for class " +
									  Integer.toString(m_connectionClassIdentifier) +
									  ": Using default value.");
						noError = false;
					}
					else
					{
						setRelativeBandwidth(relativeBandwidth);
					}
				}
			}
		}
		return noError;
	}

}
