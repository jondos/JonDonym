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
package anon.infoservice;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import HTTPClient.Codecs;
import HTTPClient.NVPair;
import anon.util.IPasswordReader;
import anon.util.IXMLEncodable;
import anon.util.XMLParseException;
import anon.util.XMLUtil;

/**
 * This class is used to store information about a proxy connection.
 * @author Wendolsky
 */
public final class ProxyInterface extends ListenerInterface
	implements ImmutableProxyInterface, IXMLEncodable
{
	/**
	 * The name of the xml node that describes if authentication is used.
	 */
	private static String XML_USE_AUTHENTICATION = "UseAuthentication";

	/**
	 * The name of the xml node that describes if authentication is used.
	 */
	private static String XML_USE_PROXY = "UseProxy";


	/**
	 * The name of the xml node that stores the user id for authentication.
	 */
	private static String XML_AUTHENTICATION_USER_ID = "AuthenticationUserID";

	/**
	 * A password for http authentication.
	 */
	private String m_authenticationPassword = null;

	/**
	 * A user id for http authentication.
	 */
	private String m_authenticationUserID = null;

	/**
	 * If the authentication strings are used.
	 */
	private boolean m_bUseAuthentication = false;

	/**
	 * If the proxy is used or not.
	 */
	private boolean m_bUseInterface = true;

	/**
	 * The password reader instance.
	 */
	private IPasswordReader m_passwordReader;


	/**
	 * Authentication stuff
	 */
	private static final long AUTH_PASS_CANCEL_WAIT_TIME = 1000 * 6l; // 6 seconds
	private volatile long m_authPassLastCancelTime = 0;
	private boolean m_bAuthPassDialogShown = false;


	/**
	 * Creates a new ProxyInterface from XML description (ProxyInterface node).
	 * @param a_proxyInterfaceNode The ProxyInterface node from an XML document.
	 * @param a_passwordReader the password reader; this is allowed to be null,
	 *                         but then you won`t be able to use proxy authentication
	 * @exception XMLParseException if an error in the xml structure occurs
	 */
	public ProxyInterface(Element a_proxyInterfaceNode, IPasswordReader a_passwordReader)
		throws XMLParseException
	{
		super(a_proxyInterfaceNode);
		m_passwordReader = a_passwordReader;

		try
		{
			setAuthenticationUserID(XMLUtil.parseValue(
				XMLUtil.getFirstChildByName(a_proxyInterfaceNode, XML_AUTHENTICATION_USER_ID), null));
		}
		catch (IllegalStateException a_e)
		{
			// we have no password reader
			setAuthenticationUserID(null);
		}

		try
		{
			setUseAuthentication(Boolean.valueOf(XMLUtil.parseValue(
						 XMLUtil.getFirstChildByName(
								  a_proxyInterfaceNode, XML_USE_AUTHENTICATION), null)).booleanValue());
		}
		catch (IllegalStateException a_e)
		{
			// we have no password reader
			setUseAuthentication(false);
		}

		setUseInterface(Boolean.valueOf(XMLUtil.parseValue(
								 XMLUtil.getFirstChildByName(
									   a_proxyInterfaceNode, XML_USE_PROXY), null)).booleanValue());
	}

	/**
	 * Creates a new interface from a hostname / IP address and a port.
	 *
	 * @param a_hostname The hostname or the IP address of this interface.
	 * @param a_port The port of this interface.
	 * @param a_passwordReader the password reader; this is allowed to be null,
	 *                         but then you won`t be able to use proxy authentication
	 * @exception IllegalArgumentException if an illegal host name or port was given
	 */
	public ProxyInterface(String a_hostname, int a_port, IPasswordReader a_passwordReader)
		throws IllegalArgumentException
	{
		this(a_hostname, a_port, PROTOCOL_TYPE_HTTP, a_passwordReader);
	}

	/**
	 * Creates a new ListenerInterface from a hostname / IP address, a port and a protocol
	 * information.
	 *
	 * @param a_hostname The hostname or the IP address of this interface.
	 * @param a_port The port of this interface (1 <= port <= 65535).
	 * @param a_protocol The protocol information. Invalid protocols are replaced by http.
	 * @param a_passwordReader the password reader; this is allowed to be null,
	 *                         but then you won`t be able to use proxy authentication
	 * @exception IllegalArgumentException if an illegal host name, port or protocol was given
	 */
	public ProxyInterface(String a_hostname, int a_port, int a_protocol,
						  IPasswordReader a_passwordReader)
		throws IllegalArgumentException
	{
		this(a_hostname, a_port, a_protocol, null, a_passwordReader, false, true);
	}

	/**
	 * Creates a new interface for a proxy that needs basic http authentication.
	 *
	 * @param a_hostname The hostname or the IP address of this interface.
	 * @param a_port The port of this interface (1 <= port <= 65535).
	 * @param a_protocol The protocol information. Invalid protocols are replaced by http.
	 * @param a_authenticationUserID a user ID for authentication
	 * @param a_passwordReader the password reader; this is allowed to be null,
	 *                         but then you won`t be able to use proxy authentication
	 * @param a_bUseAuthentication true if the authentication strings are used; false otherwise
	 * @param a_bIsValid if the proxy should be used by now (true) or later (false)
	 * @exception IllegalArgumentException if an illegal host name, port or protocol was given
	 *                                     or if authentication should be used without password reader
	 */
	public ProxyInterface(String a_hostname, int a_port, String a_protocol,
						  String a_authenticationUserID, IPasswordReader a_passwordReader,
						  boolean a_bUseAuthentication, boolean a_bIsValid)
		throws IllegalArgumentException
	{
		this(a_hostname,a_port,ListenerInterface.recognizeProtocol(a_protocol),a_authenticationUserID,
			 a_passwordReader,a_bUseAuthentication, a_bIsValid);
	}

	/**
	 * Creates a new interface for a proxy that needs basic http authentication.
	 *
	 * @param a_hostname The hostname or the IP address of this interface.
	 * @param a_port The port of this interface (1 <= port <= 65535).
	 * @param a_protocol The protocol information. Invalid protocols are replaced by http.
	 * @param a_authenticationUserID a user ID for authentication
	 * @param a_passwordReader the password reader; this is allowed to be null,
	 *                         but then you won`t be able to use proxy authentication
	 * @param a_bUseAuthentication true if the authentication strings are used; false otherwise
	 * @param a_bIsValid if the proxy should be used by now (true) or later (false)
	 * @exception IllegalArgumentException if an illegal host name, port or protocol was given
	 *                                     or if authentication should be used without password reader
	 */
	public ProxyInterface(String a_hostname, int a_port, int a_protocol,
						  String a_authenticationUserID, IPasswordReader a_passwordReader,
						  boolean a_bUseAuthentication, boolean a_bIsValid)
		throws IllegalArgumentException
	{
		super(a_hostname, a_port, a_protocol);
		m_passwordReader = a_passwordReader;

		setAuthenticationUserID(a_authenticationUserID);
		setUseAuthentication(a_bUseAuthentication);
		setUseInterface(a_bIsValid);
	}

	/**
	 * Gets the name of the corresponding xml element.
	 * @return the name of the corresponding xml element
	 */
	public static String getXMLElementName()
	{
		return "ProxyInterface";
	}

	/**
	 * Gets if the given string is a valid user ID for authentication.
	 * @param a_authenticationUserID a String
	 * @todo put this in a new class HTTPPasswordAuthenticationProtocol!!
	 * @return true if the given string is a valid user ID for authentication; false otherwise
	 */
	public static boolean isValidUserID(String a_authenticationUserID)
	{
		return (a_authenticationUserID != null);
	}

	/**
	 * Gets if the authentication strings are used.
	 * @return true if the authentication strings are used; false otherwise
	 */
	public boolean isAuthenticationUsed()
	{
		return this.m_bUseAuthentication;
	}

	/**
	 * Sets if the authentication strings are used and reads a password form the password
	 * reader if necessary.
	 * @param a_bUseAuthentication true if the authentication strings should be used; false otherwise
	 * @return true if the authentication strings are used;
	 *         false otherwise or if no password reader is registered
	 * @exception IllegalStateException if authentication should be used, but it is not possible
	 */
	public boolean setUseAuthentication(boolean a_bUseAuthentication)
		throws IllegalStateException
	{
		if (!isAuthenticationUsed() && a_bUseAuthentication)
		{
			String exception = null;

			if (m_passwordReader == null)
			{
				exception =	"No password reader!";
			}
			if (getProtocol()!=PROTOCOL_TYPE_HTTP&&
				getProtocol()!=PROTOCOL_TYPE_SOCKS)
			{
				exception = "Wrong protocol type!";
			}
			if (!isValidUserID(this.getAuthenticationUserID()))
			{
				exception = "Invalid user ID!";
			}
			if (exception != null)
			{
				throw new IllegalStateException(": Cannot set proxy authentication! " + exception);
			}
		}

		m_bUseAuthentication = a_bUseAuthentication;

		return m_bUseAuthentication;
	}

	/**
	 * Gets the authentication password of this interface. If no password is set,
	 * it is read from the password reader.
	 * @return the authentication password of this interface
	 * @todo put this in a new class HTTPPasswordAuthenticationProtocol!!
	 * @exception IllegalStateException if no password reader is registered
	 */
	public String getAuthenticationPassword()
		throws IllegalStateException
	{
		if (m_passwordReader == null)
		{
			throw new IllegalStateException("No password reader!");
		}

		if (m_authPassLastCancelTime >= System.currentTimeMillis())
		{
			return m_authenticationPassword;
		}
		synchronized (this)
		{
			if (m_bAuthPassDialogShown)
			{
				return m_authenticationPassword;
			}
			m_bAuthPassDialogShown = true;
		}


		if (m_authenticationPassword == null || m_authenticationPassword.length() == 0)
		{
			m_authenticationPassword = m_passwordReader.readPassword(this);
			/*
			if (m_authenticationPassword == null)
			System.out.println("set and null");*/
		}
		if (m_authenticationPassword == null)
		{
			m_authPassLastCancelTime = System.currentTimeMillis() + AUTH_PASS_CANCEL_WAIT_TIME;
		}
		m_bAuthPassDialogShown = false;

		return m_authenticationPassword;
	}

	public void clearAuthenticationPassword()
	{
		/*
		new Exception().printStackTrace();
		System.out.println("clear: " + m_authenticationPassword);*/
		m_authenticationPassword = null;
	}

	/**
	 * Gets the authentication user ID of this interface.
	 * @todo put this in a new class HTTPPasswordAuthenticationProtocol!!
	 * @return the authentication user ID of this interface
	 */
	public String getAuthenticationUserID()
	{
		return m_authenticationUserID;
	}

	/**
	 * Sets the authentication user ID of this interface and resets the
	 * authentication password if the user id has changed.
	 * @todo put this in a new class HTTPPasswordAuthenticationProtocol!!
	 * @param a_authenticationUserID the authentication user ID of this interface
	 */
	public void setAuthenticationUserID(String a_authenticationUserID)
	{
		if (isValidUserID(a_authenticationUserID))
		{
			if (m_authenticationUserID == null || !m_authenticationUserID.equals(a_authenticationUserID))
			{
				m_authenticationUserID = a_authenticationUserID;
				// reset the password
				m_authenticationPassword = null;
			/*	new Exception().printStackTrace();
				System.out.println("reset");*/
				if (isAuthenticationUsed() && isValid())
				{
					getAuthenticationPassword();
				}
			}
		}
		else
		{
			m_authenticationUserID = null;
			m_authenticationPassword = null;
			setUseAuthentication(false);
		}
	}

	/**
	 * Gets the authorization header with the current user id and password as a String.
	 * @return the authorization header with the current user id and password as a String
	 * @todo put this in a new class HTTPPasswordAuthenticationProtocol!!
	 * @exception IllegalStateException if the authentication mode is not activated
	 */
	public String getProxyAuthorizationHeaderAsString()
			throws IllegalStateException
	{
		if (!isAuthenticationUsed())
		{
			throw new IllegalStateException("Authentication mode is not activated! Unknown state!");
		}

		return "Proxy-Authorization: Basic " +
			Codecs.base64Encode(getAuthenticationUserID() + ":" +
								getAuthenticationPassword()) + "\r\n";
	}

	/**
	 * Get the authorization header with the current user id and password.
	 * @return the authorization header with the current user id and password
	 * @todo put this in a new class HTTPPasswordAuthenticationProtocol!!
	  * @exception IllegalStateException if the authentication mode is not activated
	 */
	public NVPair getProxyAuthorizationHeader()
		throws IllegalStateException
	{
		if (!isAuthenticationUsed())
		{
			throw new IllegalStateException("Authentication mode is not activated! Unknown state!");
		}

		return new NVPair("Proxy-Authorization", "Basic " +
						  Codecs.base64Encode(getAuthenticationUserID() + ":" +
											  getAuthenticationPassword()));
	}

	/**
	 * Tests if two interface instances are equal. The authentication passwords are not compared!
	 * @param a_proxyInterface a ListenerInterface
	 * @return true if the two ListenerInterface instances are equal; false otherwise
	 */
	public boolean equals(ProxyInterface a_proxyInterface)
	{
		if (super.equals(a_proxyInterface) &&
			getAuthenticationUserID().equals(a_proxyInterface.getAuthenticationUserID()) &&
			isValid() == a_proxyInterface.isValid() &&
			isAuthenticationUsed() == a_proxyInterface.isAuthenticationUsed())
		{
			return true;
		}
		return false;
	}

	/**
	 * Creates an XML node without signature and password for this ProxyInterface.
	 * @param a_doc The XML document, which is the environment for the created XML node.
	 * @return The ProxyInterface XML node.
	 */
	public Element toXmlElement(Document a_doc)
	{
		Element element = toXmlElementInternal(a_doc, getXMLElementName());

		Element authUserID = a_doc.createElement(XML_AUTHENTICATION_USER_ID);
		authUserID.appendChild(a_doc.createTextNode(getAuthenticationUserID()));

		Element isValid = a_doc.createElement(XML_USE_PROXY);
		XMLUtil.setValue(isValid, isValid());

		Element useAuth = a_doc.createElement(XML_USE_AUTHENTICATION);
		XMLUtil.setValue(useAuth, isAuthenticationUsed());

		element.appendChild(authUserID);
		element.appendChild(useAuth);
		element.appendChild(isValid);

		return element;
	}

	/**
	 * Gets if the proxy is used or not.
	 * @return true if the proxy is used; false otherwise
	 */
	public boolean isValid()
	{
		return super.isValid() && m_bUseInterface;
	}

	/**
	 * Activates and deactivates the proxy.
	 * @param a_bUseInterface boolean
	 */
	public void setUseInterface(boolean a_bUseInterface)
	{
		super.setUseInterface(a_bUseInterface);
		m_bUseInterface = a_bUseInterface;

		if (isValid() && isAuthenticationUsed() &&
			m_passwordReader != null && m_authenticationPassword == null)
		{
			// read the password, if it is not done before
			getAuthenticationPassword();
		}
	}
}
