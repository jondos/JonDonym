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
package jap.forward;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.Enumeration;
import java.util.Observable;
import java.util.Vector;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import anon.infoservice.HTTPConnectionFactory;
import anon.infoservice.InfoServiceDBEntry;
import anon.infoservice.ListenerInterface;
import anon.infoservice.MixCascade;
import anon.infoservice.ProxyInterface;
import anon.shared.ProxyConnection;
import anon.transport.address.AddressMappingException;
import anon.transport.address.Endpoint;
import anon.transport.address.IAddress;
import anon.transport.address.MalformedURNException;
import anon.transport.address.SkypeAddress;
import anon.transport.address.TcpIpAddress;
import anon.transport.connection.ConnectionException;
import anon.transport.connection.IStreamConnection;
import anon.util.XMLUtil;
import anon.util.IXMLEncodable;
import forward.ForwardUtils;
import forward.LocalAddress;
import forward.LocalForwarder;
import forward.client.ClientForwardException;
import forward.client.DefaultClientProtocolHandler;
import forward.client.ForwardConnectionDescriptor;
import forward.server.ForwardSchedulerStatistics;
import forward.server.ForwardServerManager;
import forward.server.ServerSocketPropagandist;
import forward.server.SkypeServerManager;
import jap.JAPConstants;
import jap.JAPController;
import jap.JAPModel;
import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;
import anon.proxy.AnonProxy;

/**
 * This class stores all routing settings. Observers of this class are notified, if the settings
 * are changed. They will get an instance of JapRoutingMessage with the detailed message of the
 * notification.
 */
final public class JAPRoutingSettings extends Observable implements IXMLEncodable
{

	/**
	 * This Class holds all supported Ways to establish 
	 * an forwarding Connection.
	 * <p>
	 * It's also acts as an registry, for mapping the different
	 * transport Modes to the corresponding Transportidentifier.   
	 */
	public static final class TransportMode {
		
		/** Constant for Transport over Skype */
		public final static TransportMode SKYPE = new TransportMode(SkypeAddress.TRANSPORT_IDENTIFIER);
		
		/** Constant for direkt Transport over TCP/IP */
		public final static TransportMode TCPIP = new TransportMode(TcpIpAddress.TRANSPORT_IDENTIFIER);
		
		/** 
		 * Constant for Forwarding to ourself.
		 * Client and Server is the same JAP Instance
		 */
		public final static TransportMode LOCAL = new TransportMode(LocalAddress.TRANSPORT_IDENTIFIER);
		
		/** Constant for an Unknown Transportmode. (Fallback) */
		public final static TransportMode UNKNOWN = new TransportMode("");
		
		/** 
		 * The Identifier for an Transportmode.
		 * <p>
		 * This String is always intern.
		 * @see String#intern()
		 */ 
		private String m_identifier;
		
		/** New TransportMode for given identifier */
		private TransportMode(String a_identifier) {
			m_identifier = a_identifier.intern();
		}
		
		/**
		 * Gets the Identifier of an TransportMode.
		 * @return The Identifer as an intern String.
		 * 
		 * @see String#intern()
		 */
		public String getIdentifier(){
			return m_identifier;
		}
		
		/**
		 * Trys to map an given Identifier to an TransportMode.
		 * 
		 * @return The corresponding TransportMode to an Identifier or {@link #UNKNOWN}
		 * if none could be found.
		 */
		public static TransportMode getByIdentifier(String a_identifier){
			a_identifier = a_identifier.intern();
			if (a_identifier == SKYPE.getIdentifier()) return SKYPE;
			if (a_identifier == TCPIP.getIdentifier()) return TCPIP;
			if (a_identifier == LOCAL.getIdentifier()) return LOCAL;
			return UNKNOWN;
		}
	}
	
	
	
	
	
	/**
	 * In this mode routing is disabled.
	 */
	public static final int ROUTING_MODE_DISABLED = 0;

	/**
	 * In this mode, we are a client and use a forwarder to connect to the JAP servers.
	 */
	public static final int ROUTING_MODE_CLIENT = 1;

	/**
	 * This is the mode, when we are a server and provide forwarding for other clients.
	 */
	public static final int ROUTING_MODE_SERVER = 2;

	/**
	 * This value means, that there were no known infoservices to register the local forwarding
	 * server.
	 */
	public static final int REGISTRATION_NO_INFOSERVICES = 1;

	/**
	 * This value means, that there occured unknown errors while the registration of the local
	 * forwarding server at every infoservice.
	 */
	public static final int REGISTRATION_UNKNOWN_ERRORS = 2;

	/**
	 * This value means, that we could not reach any infoservice while the registration of the
	 * local forwarding server.
	 */
	public static final int REGISTRATION_INFOSERVICE_ERRORS = 3;

	/**
	 * This value means, that no infoservice could verify the local forwarding server.
	 */
	public static final int REGISTRATION_VERIFY_ERRORS = 4;

	/**
	 * This value means, that the registration process was interrupted.
	 */
	public static final int REGISTRATION_INTERRUPTED = 5;

	/**
	 * This value means, that we have registrated the local forwarding server successful at least
	 * at one infoservice.
	 */
	public static final int REGISTRATION_SUCCESS = 0;
	
	/**
	 * The default Application Name to use, when working in Server Mode over Skype.
	 */
	public static final String DEFAULT_APP_NAME = "jap";

	/**
	 * This stores the current routing mode. See the constants in this class.
	 */
	private int m_routingMode;

	/**
	 * Stores the port, where the local forwarding server will listen for clients.
	 */
	private int m_serverPort;

	/**
	 * Stores the ID of the current ServerManager which manages the server socket of the forwarding
	 * server. If the forwarding server is not running, this value is always null.
	 */
	private Object m_currentServerManagerId;

	/**
	 * Stores the current bandwidth, which is provided for forwarding.
	 */
	private int m_bandwidth;

	/**
	 * Stores the maximum number of simultaneously forwarded connections.
	 */
	private int m_connections;

	/**
	 * Stores the forwarded client connection.
	 */
	private IStreamConnection m_forwardedConnection;

	
	/**
	 * Stores the hostname/IP of the current forwarder.
	 */
	//private String m_forwarderHost;

	
	/**
	 * Stores the port of the current forwarder.
	 */
	//private int m_forwarderPort;
	

	/**
	 * Stores, whether connections to the infoservice needs also forwarding. Also the instance
	 * fetching the information about a forwarder should use this to decide, whether it is possible
	 * to obtain the information about the forwarder directly from the InfoServices or the
	 * mail-gateway has to be used in order to obtain that information.
	 */
	private boolean m_forwardInfoService;

	/**
	 * Stores whether JAP shall connect to a forwarder when starting the anonymity mode. This value
	 * doesn't have any meaning within JAPRoutingSettings, so it is still possible to enable the
	 * client forwarding mode, if this value is false. Anybody who is starting the anonymity mode
	 * should check this value and do the necessary things.
	 */
	private boolean m_connectViaForwarder;

	/**
	 * Needed for synchronization if we open a new forwarded connection and have to shutdown an old
	 * one.
	 */
	private boolean m_waitForShutdownCall;

	/**
	 * Stores the protocol handling instance for the forwarded client connection.
	 */
	private DefaultClientProtocolHandler m_protocolHandler;

	/**
	 * Stores the minimum dummy traffic rate, needed by the forwarded on the current forwarded
	 * client connection.
	 */
	private int m_maxDummyTrafficInterval;

	/**
	 * Stores a list of ServerSocketPropagandists, which are currently running.
	 */
	private Vector m_runningPropagandists;

	/**
	 * Stores the instance of the startPropaganda Thread, if there is a currently running instance
	 * (else this value is null).
	 */
	private Thread m_startPropagandaThread;

	/**
	 * Stores the instance of the connection class selector. This is only needed for some more
	 * comfort, to make it easier for a user running a forwarding server to choose the correct
	 * values for the bandwidth.
	 */
	private JAPRoutingConnectionClassSelector m_connectionClassSelector;

	/**
	 * Stores a structure, which contains information about the infoservices to register at
	 * forwarding server startup.
	 */
	private JAPRoutingRegistrationInfoServices m_registrationInfoServicesStore;

	/**
	 * Stores, whether the propaganda for the local forwarding server is running (true) or not
	 * (false).
	 */
	private boolean m_propagandaStarted;

	/**
	 * Stores the structure, which manages the currently useable mixcascades. So the forwarding
	 * clients will always get an up-to-date list of usebale mixcascades.
	 */
	private JAPRoutingUseableMixCascades m_useableMixCascadesStore;

	/**
	 * Stores the instance of JAPForwardingServerStatisticsListener, which fetches the statistics
	 * from the running forwarding server.
	 */
	private JAPRoutingServerStatisticsListener m_serverStatisticsListener;

	/**
	 * Stores the instance of JAPRoutingRegistrationStatusObserver, which holds some information
	 * about the current registration status at the infoservices for the local forwarding server.
	 */
	private JAPRoutingRegistrationStatusObserver m_registrationStatusObserver;

	
	/**
	 * Stores the Address of the forwarding Server.
	 * This Address define the Transport Mode to use and all the necessary Informations
	 * to determinate the unique Endpoint based on this Medium of Transport.
	 */
	private IAddress m_forwadingAddress;

	/**
	 * When acting as Server over Skype, this Field defines the Application Name
	 * where we listen for incoming Requests.
	 */
	private String m_appName;
	
	/**
	 * This Field determines the Transport to use by selecting one of the Constants
	 * of {@link #SUPORTED_TRANSPORTMODES}.
	 * 
	 * When acting as Server, this Field defines the Medium of Transport where we want
	 * to listen for incoming Request.
	 * When  acting as Client, it has no use at the moment, as the InfoService only 
	 * propagate TCP/IP Forwarding Server and directly provided Addresses already
	 * determinate the Transport Mode. 
	 */
	private TransportMode m_transportMode;
	
	/**
	 * Stores the Address of an forwarding Server, directly provided by the User.
	 */
	private IAddress m_userForwarder;

	
	private final static String DEFAULT_SKYPE_ADDRESS_URN="urn:endpoint:skype:user(japforwarder):application("+DEFAULT_APP_NAME+")";
	/**
	 * This creates a new instance of JAPRoutingSettings. We are doing some initialization here.
	 */
	public JAPRoutingSettings()
	{
		m_routingMode = ROUTING_MODE_DISABLED;
		/* random value for the forwarding server port between 1025 and 65535 */
		m_serverPort = ( (int) (Math.round(Math.abs(Math.random() *
			( (double) 65535 - (double) 1025 + (double) 1))))) + 1025;
		m_appName = DEFAULT_APP_NAME;
		/* set default values for bandwidth, ... */
		m_connectionClassSelector = new JAPRoutingConnectionClassSelector();
		JAPRoutingConnectionClass currentConnectionClass = m_connectionClassSelector.
			getCurrentConnectionClass();
		m_bandwidth = currentConnectionClass.getCurrentBandwidth();
		m_connections = m_bandwidth / JAPConstants.ROUTING_BANDWIDTH_PER_USER;
		m_forwardedConnection = null;
		m_forwardInfoService = false;
		m_connectViaForwarder = false;
		//m_forwarderHost = null;
		//m_forwarderPort = -1;
		m_forwadingAddress = null;
		m_waitForShutdownCall = false;
		m_protocolHandler = null;
		m_maxDummyTrafficInterval = -1;
		m_runningPropagandists = new Vector();
		m_startPropagandaThread = null;
		m_propagandaStarted = false;
		m_currentServerManagerId = null;
//		m_transportMode = TransportMode.SKYPE;
		m_transportMode = TransportMode.TCPIP;
		//m_userForwarder = new SkypeAddress("japforwarder",DEFAULT_APP_NAME);
		m_registrationInfoServicesStore = new JAPRoutingRegistrationInfoServices();
		/* add the registration infoservices store to the observers of JAPRoutingSettings */
		addObserver(m_registrationInfoServicesStore);
		m_useableMixCascadesStore = new JAPRoutingUseableMixCascades();
		/* add the useable mixcascades store to the observers of JAPRoutingSettings */
		addObserver(m_useableMixCascadesStore);
		m_serverStatisticsListener = new JAPRoutingServerStatisticsListener();
		/* add the statistics listener to the observers of JAPRoutingSettings */
		addObserver(m_serverStatisticsListener);
		m_registrationStatusObserver = new JAPRoutingRegistrationStatusObserver();
		/* add the registration status observer to the observers of JAPRoutingSettings */
		addObserver(m_registrationStatusObserver);
	}

	/**
	 * Returns the current routing mode, see the constants in this class.
	 *
	 * @return The current routing mode.
	 */
	public int getRoutingMode()
	{
		return m_routingMode;
	}

	/**
	 * Changes the routing mode. This method also does everything necessary to change the routing
	 * mode, like starting or shutting down the forwarding server, infoservice registrations or a
	 * forwarded connection. Attention: The infoservice registration services are not started
	 * automatically with the server. You have to call startPropaganda() explicitly.
	 *
	 * @param a_routingMode The new routing mode. See the constants in this class.
	 *
	 * @return True, if the change of the routing mode was successful, else false.
	 */
	public boolean setRoutingMode(int a_routingMode)
	{
		boolean success = false;
		boolean notifyObservers = false;
		synchronized (this)
		{
			if (a_routingMode != m_routingMode)
			{
				if (m_routingMode == ROUTING_MODE_SERVER)
				{
					/* server was running, shut it down */
					ForwardServerManager.getInstance().shutdownForwarding();
					/* stop the propaganda */
					stopPropaganda();
					/* remove the server manager id */
					m_currentServerManagerId = null;
				}
				if (m_routingMode == ROUTING_MODE_CLIENT)
				{
					if (getForwardInfoService() == true)
					{
						/* restore the original proxy settings for the infoservice */
						JAPController.getInstance().applyProxySettingsToInfoService(JAPModel.getInstance().isProxyAuthenticationUsed());
					}
					JAPController.getInstance().setAnonMode(false);
					
					// if we used localForwarder also remove the server 
					// local is not an official supported Transport Mode and only used for testing
					if (m_forwadingAddress.getTransportIdentifier().equals("local"))
						LocalForwarder.unregisterLocalForwarder();
					
					/* client was running, close the connection */
					try {
						m_forwardedConnection.close();
					} catch (IOException e) {
						// to nothing, maybe a warning
					}
					m_forwardedConnection = null;
					m_protocolHandler = null;
				}
				m_routingMode = a_routingMode;
				if (a_routingMode == ROUTING_MODE_SERVER)
				{
					/* the server shall be started */
					ForwardServerManager.getInstance().startForwarding();
					ForwardServerManager.getInstance().setNetBandwidth(getBandwidth());
					ForwardServerManager.getInstance().setMaximumNumberOfConnections(getAllowedConnections());
					/*
					m_currentServerManagerId = null;
					if (getServerPort() != 443)
					{
						m_currentServerManagerId = ForwardServerManager.getInstance().addListenSocket(443);
					}
					if (m_currentServerManagerId == null)
					{
						LogHolder.log(LogLevel.ERR, LogType.NET,
									  "Could not start forwarding server on port 443! Trying port " +
							getServerPort() + "...");*/
					
					// should never happen
					
					
					// Multiple Transport Modes
					if(m_transportMode!=null){
					if(m_transportMode.equals(TransportMode.TCPIP))
								{
						m_currentServerManagerId = ForwardServerManager.getInstance().addListenSocket(getServerPort());
								}
					else if(m_transportMode.equals(TransportMode.SKYPE)){
						m_currentServerManagerId = ForwardServerManager.getInstance().addServerManager(
								new SkypeServerManager(getApplicationName()));}
					}
					

					if (m_currentServerManagerId == null)
					{
						/* error while binding the socket -> shutdown the server */
						ForwardServerManager.getInstance().shutdownForwarding();
						m_routingMode = ROUTING_MODE_DISABLED;
					}
					else
					{
						/* everything ok */
						success = true;
					}
				}
				if (a_routingMode == ROUTING_MODE_CLIENT)
				{
					/* close an existing anon connection, if there is one */
					if (JAPController.getInstance().getAnonMode() == true)
					{
						m_waitForShutdownCall = true;
						JAPController.getInstance().setAnonMode(false);
						try
						{
							this.wait();
						}
						catch (Exception e)
						{
						}
						/* the shutdown from the existing connection is done */
						m_waitForShutdownCall = false;
					}
					// if we use local Forwarder, we first need to start the server 
					// local is not an official supported Transport Mode and only used for testing
					if (m_forwadingAddress.getTransportIdentifier().equals("local"))
						LocalForwarder.registerLocalForwarder(getBandwidth());
					
					/* try to connect to a forwarder */
					m_forwardedConnection = ForwardUtils.getInstance().createForwardingConnection(m_forwadingAddress);
					if (m_forwardedConnection != null)
					{
						/* update the infoservice proxy settings, if it needs forwarding too */
						updateInfoServiceProxySettings();
						m_protocolHandler = new DefaultClientProtocolHandler(m_forwardedConnection);
						success = true;
					}
					else
					{
						/* there was a connection error */
						m_routingMode = ROUTING_MODE_DISABLED;
					}
				}
				if (a_routingMode == ROUTING_MODE_DISABLED)
				{
					/* nothing to do */
					success = true;
				}
				/* the routing mode was changed (maybe without success), notify observers */
				notifyObservers = true;
			}
			else
			{
				/* nothing to change */
				success = true;
			}
			if (notifyObservers == true)
			{
				setChanged();
				notifyObservers(new JAPRoutingMessage(JAPRoutingMessage.ROUTING_MODE_CHANGED));
			}
		}
		return success;
	}

	/**
	 * Returns the currently configured forwarding server port.
	 *
	 * @return The currently configured forwarding server port.
	 */
	public int getServerPort()
	{
		return m_serverPort;
	}
	
	/**
	 * Returns the currently configured Application Name for a forwarding
	 * Server listing over Skype.
	 */
	public String getApplicationName(){
		return m_appName;
	}
	
	/**
	 * Sets the Application Name to use when listening for request over Skype.
	 * <p>
	 * The Method takes care of the currently running Server and tries to change it,
	 * when we already using Skype.
	 * 
	 * @param a_value The new Application Name to use
	 * @return true if the changing was successful. Otherwise false.
	 */
	public boolean setApplicationName(String a_value){
		synchronized (this) {
			
			// is there a change
			if (m_appName.equals(a_value)) return true;
			
			// are we currently server and are we using Skype
			if ((m_routingMode != ROUTING_MODE_SERVER) ||
						(m_transportMode != TransportMode.SKYPE)){
					m_appName = a_value;
					return true;
			}
			
			// OK now comes the hard part
			Object newServerID = ForwardServerManager.getInstance().addServerManager(new SkypeServerManager(a_value));
			
			// Setting the new appName was not successful.
			if (newServerID == null) return false;
			ForwardServerManager.getInstance().removeServerManager(m_currentServerManagerId);
			m_currentServerManagerId = newServerID;
			m_appName = a_value;
			return true;
		}
	}

	/**
	 * Configures the forwarding server port. If the forwarding server is currently running, we will
	 * open the new port and the old one is closed. If there is an error while setting the new port,
	 * nothing is changed, the old port keeps active. Attention: Any active client connection are not
	 * influenced by the change of the forwarding port. They keep connected to the old port even
	 * though the old port is closed. If the forwarding server isn't running at the moment, only the
	 * configuration for the server port is changed.
	 *
	 * @param a_serverPort The port number for the forwarding server. The value must be between 1 and
	 *                     65535.
	 *
	 * @return True, if the change of the port was successful or false, if not.
	 */
	public boolean setServerPort(int a_serverPort)
	{
		boolean success = false;
		if ( (a_serverPort >= 1) && (a_serverPort <= 65535))
		{
			synchronized (this)
			{
				if (m_serverPort != a_serverPort)
				{
					if (m_routingMode != ROUTING_MODE_SERVER)
					{
						/* server is not running, so simply change the port value */
						m_serverPort = a_serverPort;
						success = true;
					}
					else
					{
						/* the server is running and the port differs from the old one , try to open the new
						 * port
						 */
						Object newServerManagerId = ForwardServerManager.getInstance().addListenSocket(
							a_serverPort);
						if (newServerManagerId != null)
						{
							/* opening the new port was successful -> close the old one and update the values */
							ForwardServerManager.getInstance().removeServerManager(m_currentServerManagerId);
							m_serverPort = a_serverPort;
							m_currentServerManagerId = newServerManagerId;
							success = true;
							/* if the propaganda is running, restart it (the old one is stopped automatically) */
							if (m_propagandaStarted == true)
							{
								startPropaganda(false);
							}
						}
					}
					if (success == true)
					{
						/* the server port was changed successfully -> notify the observers about it */
						setChanged();
						notifyObservers(new JAPRoutingMessage(JAPRoutingMessage.SERVER_PORT_CHANGED));
					}
				}
				else
				{
					/* noting to change */
					success = true;
				}
			}
		}
		return success;
	}

	/**
	 * Returns the current bandwidth, which is provided for forwarding.
	 *
	 * @return The current bandwidth for forwarding in bytes/sec.
	 */
	public int getBandwidth()
	{
		return m_bandwidth;
	}

	/**
	 * Changes the bandwidth, which is provided for forwarding. Also the allowed connection number
	 * is altered, if necessary (more connections than the bandwidth can support).
	 *
	 * @param a_bandwidth The bandwidth for forwarding in bytes/sec.
	 */
	public void setBandwidth(int a_bandwidth)
	{
		synchronized (this)
		{
			m_bandwidth = a_bandwidth;
			ForwardServerManager.getInstance().setNetBandwidth(a_bandwidth);
			/* call setAllowedConnections with the current allowed connection number, that will alter the
			 * allowed connection number, if necessary
			 */
			setAllowedConnections(getAllowedConnections());
		}
	}

	/**
	 * Returns the maximum number of clients, which can be forwarded with the current forwarding
	 * bandwidth. This number depends on the bandwidth per user constant in JAPConstants.The number
	 * of allowed forwarding connections can be smaller than this value.
	 * @see JAPConstants.ROUTING_BANDWIDTH_PER_USER
	 *
	 * @return The maximum number of connections which can be forwarded with the current bandwidth.
	 */
	public int getBandwidthMaxConnections()
	{
		return (getBandwidth() / JAPConstants.ROUTING_BANDWIDTH_PER_USER);
	}

	/**
	 * Returns the allowed number of simultaneously forwarded connections.
	 *
	 * @return The allowed number of forwarded connections.
	 */
	public int getAllowedConnections()
	{
		return m_connections;
	}

	/**
	 * Changes the allowed number of simultaneously forwarded connections. If the new value is
	 * bigger than the maximum number of possible connections because of the bandwidth limit, the
	 * number is set to the maximum number of possible connections.
	 * @see getBandwidthMaxConnections()
	 *
	 * @param a_connections The new allowed number of forwarded connections.
	 */
	public void setAllowedConnections(int a_connections)
	{
		synchronized (this)
		{
			if (a_connections > getBandwidthMaxConnections())
			{
				a_connections = getBandwidthMaxConnections();
			}
			m_connections = a_connections;
			ForwardServerManager.getInstance().setMaximumNumberOfConnections(a_connections);
		}
	}

	/**
	 * This method sets new settings for the proxy server. All new connections created by the
	 * routing server or the routing client after the call of this method will use them. Connections
	 * which already exist are not influenced by that call. The default after creating the instance
	 * of JAPRoutingSettings is to use no proxy for all new connections.
	 *
	 * @param a_proxyInterface the proxy interface
	 */
	public void setNewProxySettings(ProxyInterface a_proxyInterface)
	{
		ForwardUtils.getInstance().setProxySettings(a_proxyInterface);
	}

	/**
	 * Changes the forwarder for the client routing mode. If the routing mode is changed to the
	 * client routing mode, we use this settings to connect to the forwarder. This settings can
	 * only be changed, if the routing mode is not ROUTING_MODE_CLIENT, so this is only possible
	 * when there is no connection to a forwarder.
	 *
	 * @param a_forwarderHost The hostname/IP of a forwarder.
	 * @param a_forwarderPort The port of a forwarder.
	 */
	public void setForwarder(String a_forwarderHost, int a_forwarderPort)
	{
		setForwarderAddress(new TcpIpAddress(a_forwarderHost,a_forwarderPort));
	}
	

	/**
	 * Changes the forwarder for the client routing mode. If the routing mode is changed to the
	 * client routing mode, we use this settings to connect to the forwarder. This settings can
	 * only be changed, if the routing mode is not ROUTING_MODE_CLIENT, so this is only possible
	 * when there is no connection to a forwarder.
	 *
	 *@param a_address The Address of the Forwarder
	 */
	public void setForwarderAddress(IAddress a_address){
		synchronized (this)
		{
			if (m_routingMode != ROUTING_MODE_CLIENT)
			{
				m_forwadingAddress = a_address;
			}
		}
		
	}

	/**
	 * Returns the Address of the current forwarder. If there is no
	 * forwarder set (or an invalid one), null is returned.
	 *
	 * @return The Address of the current Forwarder.
	 */
	public IAddress getForwarderAddress()
	{
		synchronized (this)
		{
			try
			{
				return m_forwadingAddress;
			}
			catch (Exception e)
			{
				/* forwarder not set */
			}
		}
		return null;
		
	}
	
	/**
	 * Get the currently Address of an forwarding Server, as provided by the User
	 * 
	 * @return The Address to an forwarding Server or null when no one was declared.
	 */
	public IAddress getUserProvidetForwarder(){
		return m_userForwarder;
	}
	
	/**
	 * Sets the Address of an forwarding Server to prefer when using Client Routing Mode.
	 * 
	 * @param a_uri The URI of the Forwarding Server or null when no spezial Forwader
	 * should be used

	 */
	public void setUserProvidetForwarder(String a_uri) throws ConnectionException{
		if (a_uri == null) {
			m_userForwarder = null;
			return;
		}
		Endpoint endpoint = new Endpoint(a_uri);		
		// should never happen, but we are careful
		if (endpoint == null) throw new MalformedURNException("Unable to parse URN");
		IAddress result = null;	
		
		TransportMode requestedMode = TransportMode.getByIdentifier(endpoint.getTransportIdentifier());
		
		if (requestedMode == TransportMode.UNKNOWN)
			throw new AddressMappingException("Transportmode is not Supported");
		if (requestedMode == TransportMode.SKYPE)
			result =  new SkypeAddress(endpoint);
		else if (requestedMode == TransportMode.TCPIP)
			result =  new TcpIpAddress(endpoint);
		/*
		// local forwarding is not official supported 
		if (identifier.equals("local"))
			result = new LocalAddress();
		*/
		m_userForwarder = result;
	}
	
	/**
	 * Returns the currently used Transport Mode i.e. the
	 * Medium to use for transporting the Data. 
	 */
	public TransportMode getTransportMode(){
		return m_transportMode;
	}
	
	/**
	 * Sets the Transport Mode i.e the Medium to use for transporting Data.
	 * <p>
	 * The Method will take care of the currently Routing Mode while updating the value.
	 * This means that when acting as Server, we will try to add a new Listener for the selected
	 * Transport Mode and when successful, remove the old one.
	 * <p>
	 * When in Client Mode, there no changes, as the Transport mode is determinate by the Address.
	 * 
	 * @return True when changing the Transport Mode was successful. Otherwise false.
	 */
	public boolean setTransportMode(TransportMode a_value){
		synchronized (this) {
			// we do not allow UNKNOWN
			if (a_value == TransportMode.UNKNOWN) return false;
			// are there changes
			if (a_value == m_transportMode) return true;
			if (m_routingMode == ROUTING_MODE_DISABLED || m_routingMode == ROUTING_MODE_CLIENT){
				// as we ignore this setting in client mode, it's easy
				m_transportMode = a_value;
				///@todo make skype address configurable
				if(m_transportMode.equals(TransportMode.SKYPE))
					{
						try
							{
								setUserProvidetForwarder(DEFAULT_SKYPE_ADDRESS_URN);
							}
						catch (ConnectionException e)
							{
							}
					}
				return true;
			}
			if (m_routingMode == ROUTING_MODE_SERVER){
				// try to start the new server
				Object newServerID = null;
				
				if (a_value == TransportMode.TCPIP)
					newServerID = ForwardServerManager.getInstance().addListenSocket(m_serverPort);
				
				else if (a_value == TransportMode.SKYPE)
					newServerID = ForwardServerManager.getInstance().addServerManager(
							new SkypeServerManager(m_appName));				
				
				if (newServerID == null) return false;
				// remove the old one
				ForwardServerManager.getInstance().removeServerManager(m_currentServerManagerId);
				// save changes
				m_currentServerManagerId = newServerID;
				m_transportMode = a_value;
				return true;
			}
			// unknown routing mode
			return false;
		}
	}

	/**
	 * Changes whether connections to the infoservice needs forwarding too. Also the instance
	 * fetching the information about a forwarder uses this to decide, whether it is possible to
	 * obtain the information about the forwarder directly from the InfoServices or the mail-gateway
	 * has to be used in order to obtain that information.
	 *
	 * @param a_forwardInfoService True, if connections to the infoservice must be forwarded and the
	 *                             mail-gateway is used in order to obtain the forwarder-address,
	 *                             else false (no InfoService forwarding and direct requests to the
	 *                             InfoService when fetching a forwarder address).
	 */
	public void setForwardInfoService(boolean a_forwardInfoService)
	{
		synchronized (this)
		{
			if (m_forwardInfoService != a_forwardInfoService)
			{
				m_forwardInfoService = a_forwardInfoService;
				if ( (a_forwardInfoService == true) && (getRoutingMode() == ROUTING_MODE_CLIENT))
				{
					/* apply the proxy settings directly for the infoservice */
					updateInfoServiceProxySettings();
				}
				if ( (a_forwardInfoService == false) && (getRoutingMode() == ROUTING_MODE_CLIENT))
				{
					/* restore the original proxy settings for the infoservice */
					JAPController.getInstance().applyProxySettingsToInfoService(JAPModel.getInstance().isProxyAuthenticationUsed());
				}
				setChanged();
				notifyObservers(new JAPRoutingMessage(JAPRoutingMessage.CLIENT_SETTINGS_CHANGED));
			}
		}
	}

	/**
	 * Returns whether connections to the infoservice needs forwarding too. Also the instance
	 * fetching the information about a forwarder should use this to decide, whether it is possible
	 * to obtain the information about the forwarder directly from the InfoServices or the
	 * mail-gateway has to be used in order to obtain that information.
	 *
	 * @return True, if connections to the infoservice must be forwarded and the mail-gateway is
	 *         used in order to obtain the forwarder-address, else false (no InfoService forwarding
	 *         and direct requests to the InfoService when fetching a forwarder address).
	 */
	public boolean getForwardInfoService()
	{
		//return m_forwardInfoService;
		return false; // this is done automatically if no info is received
	}

	/**
	 * Sets whether JAP shall connect to a forwarder when starting the anonymity mode. This value
	 * doesn't have any meaning within JAPRoutingSettings, so it is still possible to enable the
	 * client forwarding mode, if this value is false. Anybody who is starting the anonymity mode
	 * should check this value and do the necessary things.
	 *
	 * @param a_connectViaForwarder True, if new anonymous connections shall use a forwarder, false
	 *                              otherwise.
	 */

	public void setConnectViaForwarder(boolean a_connectViaForwarder)
	{
		synchronized (this)
		{
			if (m_connectViaForwarder != a_connectViaForwarder)
			{
				m_connectViaForwarder = a_connectViaForwarder;
				setChanged();
				notifyObservers(new JAPRoutingMessage(JAPRoutingMessage.CLIENT_SETTINGS_CHANGED));
			}
		}
	}

	/**
	 * Returns whether JAP shall connect to a forwarder when starting the anonymity mode. This value
	 * doesn't have any meaning within JAPRoutingSettings, so it is still possible to enable the
	 * client forwarding mode, if this value is false. Anybody who is starting the anonymity mode
	 * should check this value and do the necessary things.
	 *
	 * @return True, if new anonymous connections shall use a forwarder, false otherwise.
	 */
	public boolean isConnectViaForwarder()
	{
		return m_connectViaForwarder;
	}

	/**
	 * This method updates the local proxy for the infoservice, if we are in client routing mode
	 * and the infoservice needs to be forwarded. This method must be called, if the old HTTP
	 * listener port is closed and a new one is opened. At the moment, we don't need this method
	 * because changing the HTTP listener requires a restart of JAP.
	 */
	public void httpListenerPortChanged()
	{
		synchronized (this)
		{
			if ( (getForwardInfoService() == true) && (getRoutingMode() == ROUTING_MODE_CLIENT))
			{
				/* update the infoservice proxy settings -> use the new local HTTP listener port */
				updateInfoServiceProxySettings();
			}
		}
	}

	/**
	 * This method is always called, when a anon connection is closed. If it is a forwarded
	 * connection, we update the internal status like the routing mode.
	 */
	public void anonConnectionClosed()
	{
		synchronized (this)
		{
			if (getRoutingMode() == ROUTING_MODE_CLIENT)
			{
				/* we have to do something */
				if (m_waitForShutdownCall == true)
				{
					/* this is the shutdown of an old anon connection before the start of a new forwarded
					 * connection -> don't do anything except notifying the startup thread for the new
					 * forwarded connection
					 */
					this.notify();
				}
				else
				{
					/* this is the shutdown of an existing forwarded connection */
					setRoutingMode(ROUTING_MODE_DISABLED);
				}
			}
		}
	}

	/**
	 * Returns the anon proxy for the forwarded client connection.
	 *
	 * @param a_listener The ServerSocket listening for incoming requests e.g. the from the web
	 *                   browser.
	 *
	 * @return The anon proxy for the forwarded connection or null, if we are not in the client
	 *         routing mode.
	 */
	public AnonProxy getAnonProxyInstance(ServerSocket a_listener)
	{
		AnonProxy anonProxy = null;
		synchronized (this)
		{
			if (getRoutingMode() == ROUTING_MODE_CLIENT)
			{
				anonProxy = new AnonProxy(a_listener, m_forwardedConnection, m_maxDummyTrafficInterval);
			}
		}
		return anonProxy;
	}

	/**
	 * Returns the connection offer from the forwarder. This method must be called exactly once,
	 * after the connection to the forwarder is created. If there was an error or we are not in
	 * client routing mode, an exception is thrown.
	 *
	 * @return The connection descriptor with the connection offer from the forwarder.
	 */
	public ForwardConnectionDescriptor getConnectionDescriptor() throws
		ClientForwardException
	{
		ForwardConnectionDescriptor connectionDescriptor = null;
		DefaultClientProtocolHandler protocolHandler = null;
		synchronized (this)
		{
			if (getRoutingMode() == ROUTING_MODE_CLIENT)
			{
				protocolHandler = m_protocolHandler;
			}
		}
		if (protocolHandler != null)
		{
			/* we are in client routing mode */
			try
			{
				connectionDescriptor = protocolHandler.getConnectionDescriptor();
			}
			catch (ClientForwardException e)
			{
				/* there was an exception, shutdown routing */
				setRoutingMode(ROUTING_MODE_DISABLED);
				throw (e);
			}
		}
		else
		{
			/* throw an exception, because we are not in client routing mode */
			throw (new ClientForwardException(ClientForwardException.ERROR_UNKNOWN_ERROR,
											  "JAPRoutingSettings: getConnectionDescriptor: Not in client routing mode."));
		}
		synchronized (this)
		{
			/* store the needed dummy traffic interval */
			m_maxDummyTrafficInterval = connectionDescriptor.getMinDummyTrafficInterval();
		}
		return connectionDescriptor;
	}

	/**
	 * This method must be called exactly once, after we have received the the connection offer
	 * from the forwarder. If the call of this method doesn't throw an exception, everything is
	 * ready for starting the anonymous connection. This method throws an exception, if there is
	 * something wrong while sending our decision to the forwarder. At the moment this method
	 * must be called within the forwarder dummy traffic interval, because dummy traffic is not
	 * implemented within the used protocol -> dummy traffic is available after starting the JAP
	 * AnonProxy on the forwarded connection.
	 *
	 * @param a_mixCascade The mixcascade from the connection offer we want to use.
	 */
	public void selectMixCascade(MixCascade a_mixCascade) throws ClientForwardException
	{
		DefaultClientProtocolHandler protocolHandler = null;
		synchronized (this)
		{
			if (getRoutingMode() == ROUTING_MODE_CLIENT)
			{
				protocolHandler = m_protocolHandler;
			}
		}
		if (protocolHandler != null)
		{
			/* we are in client routing mode */
			try
			{
				protocolHandler.selectMixCascade(a_mixCascade);
			}
			catch (ClientForwardException e)
			{
				/* there was an exception, shutdown routing */
				setRoutingMode(ROUTING_MODE_DISABLED);
				throw (e);
			}
		}
		else
		{
			/* throw an exception, because we are not in client routing mode */
			throw (new ClientForwardException(ClientForwardException.ERROR_UNKNOWN_ERROR,
											  "JAPRoutingSettings: selectMixCascade: Not in client routing mode."));
		}
	}

	/**
	 * Returns the statistics instance of the forwarding scheduler. If we are not in server
	 * routing mode, null is returned.
	 *
	 * @return The statistics instance of the scheduler or null, if no scheduler is running.
	 */
	public ForwardSchedulerStatistics getSchedulerStatistics()
	{
		return ForwardServerManager.getInstance().getSchedulerStatistics();
	}

	/**
	 * Returns the number of currently forwarded connections. If we are not in server routing mode,
	 * 0 is returned.
	 *
	 * @return The number of currently forwarded connections.
	 */
	public int getCurrentlyForwardedConnections()
	{
		return ForwardServerManager.getInstance().getCurrentlyForwardedConnections();
	}

	/**
	 * Starts the propaganda instances, which register the local forwarding server at the specified
	 * infoservices. Any running propaganda instances are stopped. If we are not in the server
	 * routing mode, nothing is done.
	 *
	 * @param a_blocking Whether to wait until all propaganda instances are started and have tried
	 *                   to connect to the infoservice (true) or return immediately (false).
	 *
	 * @return The status of the registration process. See the REGISTRATION constants in this class.
	 *         If we are not in blocking mode (a_blocking == false), always 0 is returned.
	 */
	public int startPropaganda(boolean a_blocking)
	{
		/* create a lock for synchronizing the startPropaganda thread with the current one */
		final JAPRoutingSettingsPropagandaThreadLock masterThreadLock = new
			JAPRoutingSettingsPropagandaThreadLock();
		synchronized (this)
		{
			if (m_routingMode == ROUTING_MODE_SERVER)
			{
				/* we have to be in the server routing mode */
				/* stop the running propaganda instances */
				stopPropaganda();
				final Vector infoServiceList = getRegistrationInfoServicesStore().
					getRegistrationInfoServicesForStartup();
				final Vector currentPropagandists = new Vector();
				m_runningPropagandists = currentPropagandists;
				m_startPropagandaThread = new Thread(new Runnable()
				{
					public void run()
					{
						/* this is not synchronized with JAPRoutingSettings */
						/* notify the observers, that we are starting the propaganda */
						setChanged();
						notifyObservers(new JAPRoutingMessage(JAPRoutingMessage.START_PROPAGANDA_BEGIN, null));
						Enumeration infoServices = infoServiceList.elements();
						boolean stopRegistration = false;
						while ( (infoServices.hasMoreElements()) && !stopRegistration)
						{
							ServerSocketPropagandist currentPropagandist = new ServerSocketPropagandist(
								m_serverPort, (InfoServiceDBEntry) (infoServices.nextElement()));
							synchronized (JAPModel.getInstance().getRoutingSettings())
							{
								stopRegistration = Thread.interrupted();
								if (stopRegistration)
								{
									/* we were interrupted -> all propagandists except the current on were stopped
									 * -> stop the current one
									 */
									currentPropagandist.stopPropaganda();
									masterThreadLock.registrationWasInterrupted();
								}
								else
								{
									/* we were not interrupted -> go on */
									currentPropagandists.addElement(currentPropagandist);
									masterThreadLock.updateRegistrationStatus(currentPropagandist);
									/* notify the observers, that there is a new propagandist */
									setChanged();
									notifyObservers(new JAPRoutingMessage(JAPRoutingMessage.
										PROPAGANDA_INSTANCES_ADDED, currentPropagandists.clone()));
								}
							}
						}
						synchronized (JAPModel.getInstance().getRoutingSettings())
						{
							/* remove the pointer to this thread, because we are at the end -> interrupting makes
							 * no sense any more
							 */
							m_startPropagandaThread = null;
							if ( (Thread.interrupted() == false) && (stopRegistration == false))
							{
								/* we can notify the observers, that propaganda was started successfully */
								setChanged();
								notifyObservers(new JAPRoutingMessage(JAPRoutingMessage.
									START_PROPAGANDA_READY, currentPropagandists.clone()));
							}
						}
						/* we are at the end -> notify the master thread, if it is waiting at the lock */
						synchronized (masterThreadLock)
						{
							masterThreadLock.propagandaThreadIsReady();
							masterThreadLock.notify();
						}
					}
				});
				m_startPropagandaThread.setDaemon(true);
				m_propagandaStarted = true;
				m_startPropagandaThread.start();
			}
			else
			{
				/* we are not in server routing mode -> we are ready because nothing was to do */
				masterThreadLock.propagandaThreadIsReady();
			}
		}
		int registrationStatus = 0;
		synchronized (masterThreadLock)
		{
			if (a_blocking == true)
			{
				/* wait for the startPropaganda Thread */
				if (masterThreadLock.isPropagandaThreadReady() == false)
				{
					/* wait only, if it is not already at the end */
					try
					{
						masterThreadLock.wait();
						registrationStatus = masterThreadLock.getRegistrationStatus();
					}
					catch (InterruptedException e)
					{
					}
				}
			}
		}
		return registrationStatus;
	}

	/**
	 * Stops all running propaganda instances, which register the local forwarder at the specified
	 * infoservices.
	 */
	public void stopPropaganda()
	{
		synchronized (this)
		{
			if (m_startPropagandaThread != null)
			{
				/* there is a thread starting new propagandist instances */
				try
				{
					/* interrupt the running startPropaganda Thread */
					m_startPropagandaThread.interrupt();
				}
				catch (Exception e)
				{
					/* should not happen */
				}
			}
			/* stop all running propagandists */
			while (m_runningPropagandists.size() > 0)
			{
				( (ServerSocketPropagandist) (m_runningPropagandists.firstElement())).stopPropaganda();
				m_runningPropagandists.removeElementAt(0);
			}
			m_propagandaStarted = false;
			/* notify the observers */
			setChanged();
			notifyObservers(new JAPRoutingMessage(JAPRoutingMessage.STOP_PROPAGANDA_CALLED));
		}
	}

	/**
	 * This method adds a new single propaganda instance for the forwarding server. It should only
	 * be called from JAPRoutingRegistrationInfoServices because the instance is only added to the
	 * temporarly propaganda instances list, which is only valid until stopPropaganda() is called.
	 * It is not added to the permanent list, which is stored in JAPRoutingRegistrationInfoServices.
	 * If the propaganda doesn't run at the moment, is stopped while the new propaganda instance is
	 * started or the server port is changed while the new propaganda instance is started, nothing
	 * is done. This method only starts a thread, which creates the propagandist and returns
	 * immediately.
	 *
	 * @param a_registrationInfoService The infoservice where the new propaganda instance tries to
	 *                                  registrate at.
	 */
	public void addPropagandaInstance(final InfoServiceDBEntry a_registrationInfoService)
	{
		Thread addPropagandistThread = new Thread(new Runnable()
		{
			public void run()
			{
				boolean startNewPropagandist = false;
				int serverPort = -1;
				synchronized (JAPModel.getInstance().getRoutingSettings())
				{
					/* start only, if the propaganda is running */
					startNewPropagandist = m_propagandaStarted;
					serverPort = m_serverPort;
				}
				ServerSocketPropagandist newPropagandist = null;
				if (startNewPropagandist == true)
				{
					newPropagandist = new ServerSocketPropagandist(serverPort, a_registrationInfoService);
					synchronized (JAPModel.getInstance().getRoutingSettings())
					{
						if ( (m_serverPort == serverPort) && (m_propagandaStarted == true))
						{
							/* add the new propagandist to the list and notify the observers */
							m_runningPropagandists.addElement(newPropagandist);
							setChanged();
							notifyObservers(new JAPRoutingMessage(JAPRoutingMessage.
								PROPAGANDA_INSTANCES_ADDED, m_runningPropagandists.clone()));
						}
						else
						{
							/* stop the new propagandist, because the environment was changed */
							newPropagandist.stopPropaganda();
						}
					}
				}
			}
		});
		addPropagandistThread.setDaemon(true);
		addPropagandistThread.start();
	}

	/**
	 * Returns a snapshot of the list of currently running propaganda instances, which registrate
	 * the local forwarding server at the infoservices. If there are no running propaganda instances
	 * (maybe because we are not in server routing mode), an empty Vector is returned.
	 *
	 * @return A Vector with all currently running propaganda instances.
	 */
	public Vector getRunningPropagandaInstances()
	{
		Vector resultValue = null;
		synchronized (this)
		{
			resultValue = (Vector) (m_runningPropagandists.clone());
		}
		return resultValue;
	}

	/**
	 * Returns the instance of the connection class selector of the forwarding server. This is
	 * only needed because of some comfort reasons. It makes it easier for a user to configure
	 * the correct values for the forwarding server bandwidth.
	 *
	 * @return The forwarding server connection class selector.
	 */
	public JAPRoutingConnectionClassSelector getConnectionClassSelector()
	{
		return m_connectionClassSelector;
	}

	/**
	 * Returns the structure, where the infoservices are stored, used for registration of the
	 * local forwarding server.
	 *
	 * @return The forwarding server registration infoservices store.
	 */
	public JAPRoutingRegistrationInfoServices getRegistrationInfoServicesStore()
	{
		return m_registrationInfoServicesStore;
	}

	/**
	 * Returns the structure, which stores the currently useable mixcascades for the client
	 * connections and updates the ForwardCascadeDatabase of the forwarding server.
	 *
	 * @return The structure, which manages the currently useable mixcascades for the forwarding
	 *         server.
	 */
	public JAPRoutingUseableMixCascades getUseableMixCascadesStore()
	{
		return m_useableMixCascadesStore;
	}

	/**
	 * Returns the current routing settings for storage within an XML document.
	 *
	 * @param a_doc The context document for the routing settings.
	 *
	 * @return An XML node (JapForwardingSettings) with all routing related settings.
	 */
	public Element toXmlElement(Document a_doc)
	{
		Element japForwardingSettingsNode = a_doc.createElement("JapForwardingSettings");
		Element forwardingServerNode = a_doc.createElement("ForwardingServer");
		Element serverPortNode = a_doc.createElement("ServerPort");
		Element serverRunningNode = a_doc.createElement("ServerRunning");
		synchronized (this)
		{
			XMLUtil.setValue(serverPortNode, getServerPort());
			if (getRoutingMode() == ROUTING_MODE_SERVER)
			{
				XMLUtil.setValue(serverRunningNode, true);
			}
			else
			{
				XMLUtil.setValue(serverRunningNode, false);
			}
		}
		forwardingServerNode.appendChild(serverPortNode);
		forwardingServerNode.appendChild(serverRunningNode);
		forwardingServerNode.appendChild(getConnectionClassSelector().getSettingsAsXml(a_doc));
		forwardingServerNode.appendChild(getRegistrationInfoServicesStore().getSettingsAsXml(a_doc));
		forwardingServerNode.appendChild(getUseableMixCascadesStore().getSettingsAsXml(a_doc));
		japForwardingSettingsNode.appendChild(forwardingServerNode);

		Element forwardingClientNode = a_doc.createElement("ForwardingClient");
		XMLUtil.setAttribute(forwardingClientNode, "type",getTransportMode().getIdentifier());
		Element connectViaForwarderNode = a_doc.createElement("ConnectViaForwarder");
		Element forwardInfoServiceNode = a_doc.createElement("ForwardInfoService");
		XMLUtil.setValue(connectViaForwarderNode, isConnectViaForwarder());
		XMLUtil.setValue(forwardInfoServiceNode, getForwardInfoService());
		forwardingClientNode.appendChild(connectViaForwarderNode);
		forwardingClientNode.appendChild(forwardInfoServiceNode);
		japForwardingSettingsNode.appendChild(forwardingClientNode);

		return japForwardingSettingsNode;
	}

	/**
	 * This method loads all forwarding related settings from a prior created XML structure. If there
	 * is an error while loading the server settings, it is still tried to load as much settings as
	 * possible, but the server is never started in that case, because of security reasons.
	 *
	 * @param a_japForwardingSettingsNode The JapForwardingSettings XML node, which was created by
	 *                                    the getSettingsAsXml() method.
	 */
	public int loadSettingsFromXml(Element a_japForwardingSettingsNode)
	{
		/* this value stores, whether there are no errors -> starting the server is possible, so the
		 * server is never started, if there are any errors while reading the configuration, that's
		 * better for security
		 */
		boolean startServerIsPossible = true;
		/* get the ForwardingServer settings */
		Element forwardingServerNode = (Element) (XMLUtil.getFirstChildByName(a_japForwardingSettingsNode,
			"ForwardingServer"));
		if (forwardingServerNode == null)
		{
			LogHolder.log(LogLevel.ERR, LogType.MISC, "JAPRoutingSettings: loadSettingsFromXml: Error in XML structure (ForwardingServer node): Using default forwarding server settings.");
		}
		else
		{
			/* get the ServerPort */
			Element serverPortNode = (Element) (XMLUtil.getFirstChildByName(forwardingServerNode,
				"ServerPort"));
			if (serverPortNode == null)
			{
				LogHolder.log(LogLevel.ERR, LogType.MISC, "JAPRoutingSettings: loadSettingsFromXml: Error in XML structure (ServerPort node): Using default server port.");
				startServerIsPossible = false;
			}
			else
			{
				int serverPort = XMLUtil.parseValue(serverPortNode, -1);
				if (serverPort == -1)
				{
					LogHolder.log(LogLevel.ERR, LogType.MISC,
								  "JAPRoutingSettings: loadSettingsFromXml: Invalid server port in XML structure: Using default server port.");
					startServerIsPossible = false;
				}
				else
				{
					/* we have read a valid server port -> set it */
					if (setServerPort(serverPort) == false)
					{
						LogHolder.log(LogLevel.ERR, LogType.MISC,
									  "JAPRoutingSettings: loadSettingsFromXml: Error while setting the server port: Using default server port.");
						startServerIsPossible = false;
					}
				}
			}
			/* get the connection class settings */
			Element connectionClassSettingsNode = (Element) (XMLUtil.getFirstChildByName(forwardingServerNode,
				"ConnectionClassSettings"));
			if (connectionClassSettingsNode == null)
			{
				LogHolder.log(LogLevel.ERR, LogType.MISC, "JAPRoutingSettings: loadSettingsFromXml: Error in XML structure (ConnectionClassSettings node): Using default connection class settings.");
				startServerIsPossible = false;
			}
			else
			{
				/* load the connection class settings */
				if (getConnectionClassSelector().loadSettingsFromXml(connectionClassSettingsNode) == false)
				{
					startServerIsPossible = false;
				}
			}
			/* get the infoservice registration settings */
			Element infoserviceRegistrationSettingsNode = (Element) (XMLUtil.getFirstChildByName(
				forwardingServerNode, "InfoServiceRegistrationSettings"));
			if (infoserviceRegistrationSettingsNode == null)
			{
				LogHolder.log(LogLevel.ERR, LogType.MISC, "JAPRoutingSettings: loadSettingsFromXml: Error in XML structure (InfoServiceRegistrationSettings node): Using default infoservice registration settings.");
				startServerIsPossible = false;
			}
			else
			{
				/* load the infoservice registration settings */
				if (getRegistrationInfoServicesStore().loadSettingsFromXml(
					infoserviceRegistrationSettingsNode) == false)
				{
					startServerIsPossible = false;
				}
			}
			/* get the allowed mixcascades */
			Element allowedMixCascadesSettingsNode = (Element) (XMLUtil.getFirstChildByName(
				forwardingServerNode, "AllowedMixCascadesSettings"));
			if (allowedMixCascadesSettingsNode == null)
			{
				LogHolder.log(LogLevel.ERR, LogType.MISC, "JAPRoutingSettings: loadSettingsFromXml: Error in XML structure (AllowedMixCascadesSettings node): Using default forwarding mixcascade settings.");
				startServerIsPossible = false;
			}
			else
			{
				/* load the allowed mixcascades */
				if (getUseableMixCascadesStore().loadSettingsFromXml(allowedMixCascadesSettingsNode) == false)
				{
					startServerIsPossible = false;
				}
			}
			/* get the option, whether the server shall be started, this should be done after loading all
			 * other forwarding settings
			 */
			Element serverRunningNode = (Element) (XMLUtil.getFirstChildByName(forwardingServerNode,
				"ServerRunning"));
			if (serverRunningNode == null)
			{
				LogHolder.log(LogLevel.ERR, LogType.MISC,
							  "JAPRoutingSettings: loadSettingsFromXml: Error in XML structure (ServerRunning node): Server not started.");
			}
			else
			{
				/* read the start server setting */
				if (XMLUtil.parseValue(serverRunningNode, false) == true)
				{
					if (startServerIsPossible == true)
					{
						if (setRoutingMode(ROUTING_MODE_SERVER) == true)
						{
							/* start the propaganda */
							startPropaganda(false);
							LogHolder.log(LogLevel.INFO, LogType.MISC,
										  "JAPRoutingSettings: loadSettingsFromXml: According to the configuration, the forwarding server was started.");
						}
						else
						{
							LogHolder.log(LogLevel.ERR, LogType.MISC,
										  "JAPRoutingSettings: loadSettingsFromXml: Error while starting the forwarding server.");
						}
					}
					else
					{
						LogHolder.log(LogLevel.ERR, LogType.MISC, "JAPRoutingSettings: loadSettingsFromXml: Because of errors while loading the configuration, the forwarding server was not started.");
					}
				}
			}
		}
		/* get the forwarding client settings */
		Element forwardingClientNode = (Element) (XMLUtil.getFirstChildByName(a_japForwardingSettingsNode,
			"ForwardingClient"));
		if (forwardingClientNode == null)
		{
			LogHolder.log(LogLevel.ERR, LogType.MISC, "JAPRoutingSettings: loadSettingsFromXml: Error in XML structure (ForwardingClient node): Using default forwarding client settings.");
		}
		else
		{
			/* get the option, whether the anonymous connections shall use a forwarder */
			Element connectViaForwarderNode = (Element) (XMLUtil.getFirstChildByName(forwardingClientNode,
				"ConnectViaForwarder"));
			if (connectViaForwarderNode == null)
			{
				LogHolder.log(LogLevel.ERR, LogType.MISC, "JAPRoutingSettings: loadSettingsFromXml: Error in XML structure (ConnectViaForwarder node): Using default value when enabling anonymity mode.");
			}
			else
			{
				/* read the connect-via-forwarder client setting */
				String type=XMLUtil.parseAttribute(forwardingClientNode, "type", getTransportMode().getIdentifier());
				setTransportMode(TransportMode.getByIdentifier(type));
				setConnectViaForwarder(XMLUtil.parseValue(connectViaForwarderNode, false));
			}
			/* get the option, whether the InfoService can reached directly or not */
			Element forwardInfoServiceNode = (Element) (XMLUtil.getFirstChildByName(forwardingClientNode,
				"ForwardInfoService"));
			if (forwardInfoServiceNode == null)
			{
				LogHolder.log(LogLevel.ERR, LogType.MISC, "JAPRoutingSettings: loadSettingsFromXml: Error in XML structure (ForwardInfoService node): Using default value when creating a forwarded connection.");
			}
			else
			{
				/* read the forward InfoService client setting */
				setForwardInfoService(XMLUtil.parseValue(forwardInfoServiceNode, false));
			}
		}
		return 0;
	}

	/**
	 * Returns the statistics listener of the forwarding servers. So it's easy to get always the
	 * current server statistics.
	 *
	 * @return The statistics listener for the forwarding servers (this is always the same instance,
	 *         nevertheless a forwarding server is running or not).
	 */
	public JAPRoutingServerStatisticsListener getServerStatisticsListener()
	{
		return m_serverStatisticsListener;
	}

	/**
	 * Returns the instance of JAPRoutingRegistrationStatusObserver, which holds some information
	 * about the current registration status at the infoservices for the local forwarding server.
	 *
	 * @return The registration status observer for the infoservice registrations (this is always
	 *         the same instance, nevertheless a forwarding server is running or not).
	 */
	public JAPRoutingRegistrationStatusObserver getRegistrationStatusObserver()
	{
		return m_registrationStatusObserver;
	}

	/**
	 * If the infoservice needs forwarding, this changes the infoservice proxy settings to the
	 * JAP HTTP listener port (where JAP accept requests from browsers). So all infoservice requests
	 * are forwarded by JAP to the forwarder, from there through the mixcascade and then to the
	 * infoservice.
	 */
	private void updateInfoServiceProxySettings()
	{
		synchronized (this)
		{
			if (getForwardInfoService() == true)
			{
				/* change the proxy settings for the infoservice */
				HTTPConnectionFactory.getInstance().setNewProxySettings(
					new ProxyInterface("localhost",
									   JAPModel.getHttpListenerPortNumber(),
									   ProxyInterface.PROTOCOL_TYPE_HTTP, null),
									   JAPModel.getInstance().isProxyAuthenticationUsed());
			}
		}
	}
}
