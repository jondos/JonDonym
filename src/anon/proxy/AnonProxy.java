/*
 Copyright (c) 2000, The JAP-Team
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
package anon.proxy;


import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.Vector;

import anon.AnonChannel;
import anon.AnonService;
import anon.AnonServiceEventListener;
import anon.AnonServiceFactory;
import anon.ErrorCodes;
import anon.NotConnectedToMixException;
import anon.client.AnonClient;
import anon.client.DummyTrafficControlChannel;
import anon.client.ITermsAndConditionsContainer;
import anon.infoservice.MixCascade;
import anon.infoservice.AbstractMixCascadeContainer;
import anon.mixminion.MixminionServiceDescription;
import anon.tor.TorAnonServerDescription;
import anon.transport.connection.IStreamConnection;
import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;
import anon.AnonServerDescription;
import anon.pay.IAIEventListener;
import anon.infoservice.IMutableProxyInterface;
import anon.util.IMessages;
import anon.util.ObjectQueue;
import java.security.SignatureException;

import anon.client.TrustException;

/**
 * This calls implements a proxy one can use for convienient access to the
 * provided anonymous communication primitives. Below you find an example which
 * creates a proxy which uses the AN.ON mix cascade for anonymous web surfing.
 * {@code AnonProxy theProxy=new AnonProxy(serverSocket,null);
 * theProxy.setMixCascade(new MixCascade(null, null, hostNameOfMixCascade,
 * portNumberOfMixCascade)); theProxy.start(); }
 */

final public class AnonProxy implements Runnable, AnonServiceEventListener
{
	public static final int UNLIMITED_REQUESTS = Integer.MAX_VALUE;
	public static final int MIN_REQUESTS = 5;

	public static final int E_BIND = -2;

	public final static int E_MIX_PROTOCOL_NOT_SUPPORTED = ErrorCodes.E_PROTOCOL_NOT_SUPPORTED;

	public final static int E_SIGNATURE_CHECK_FIRSTMIX_FAILED = ErrorCodes.E_SIGNATURE_CHECK_FIRSTMIX_FAILED;

	public final static int E_SIGNATURE_CHECK_OTHERMIX_FAILED = ErrorCodes.E_SIGNATURE_CHECK_OTHERMIX_FAILED;

	private static final int RECONNECT_INTERVAL = 5000;

	private int m_maxRequests = UNLIMITED_REQUESTS;

	private AnonService m_Anon;

	private AnonService m_Tor;

	private AnonService m_Mixminion;

	private Vector m_anonServiceListener;

	private Thread threadRun;

	private ServerSocket m_socketListener;

	private IMutableProxyInterface m_proxyInterface =
		new IMutableProxyInterface.DummyMutableProxyInterface();

	private IProxyListener m_ProxyListener;

	private volatile int m_numChannels = 0;

	private boolean m_bReconnecting = false;
	private final Object THREAD_SYNC = new Object();
	private final Object SHUTDOWN_SYNC = new Object();
	private boolean bShuttingDown = false;

	private ProxyCallbackHandler m_callbackHandler = new ProxyCallbackHandler();
	private HTTPProxyCallback m_httpProxyCallback = null;
	private JonDoFoxHeader m_jfxHeader = null;
	private HTTPConnectionWatch m_connectionWatch = null;
	
	/**
	 * Stores the MixCascade we are connected to.
	 */
	private AbstractMixCascadeContainer m_currentMixCascade = new DummyMixCascadeContainer();

	/**
	 * Stores the Tor params.
	 */
	private TorAnonServerDescription m_currentTorParams;

	/**
	 * Stores the Mixminion params.
	 */
	private MixminionServiceDescription m_currentMixminionParams;

	/**
	 * Stores, whether we use a forwarded connection (already active, when
	 * AnonProxy is created) or not.
	 */
	private boolean m_forwardedConnection;

	/**
	 * Stores the maximum dummy traffic interval in milliseconds -> we need dummy
	 * traffic with at least that rate. If this value is -1, there is no need for
	 * dummy traffic on a forwarded connection on the server side. This value is
	 * only meaningful, if m_forwardedConnection is true.
	 */
	private int m_maxDummyTrafficInterval = DummyTrafficControlChannel.DT_MAX_INTERVAL_MS;

	/**
	 * Creates a new AnonProxy. This proxy uses as default only the AN.ON service.
	 * If you also want to use TOR and Mixminion you have to enable them by
	 * calling setTorParams() and setMixmininoParams().
	 *
	 * @see setTorParams()
	 * @see setMixminionParams()
	 *
	 * @param a_listener
	 *          A ServerSocket, where the AnonProxy listens for new requests (e.g.
	 *          from a web browser).
	 */
	public AnonProxy(ServerSocket a_listener)
	{
		this (a_listener, null, null);
	}

	/**
	 * Creates a new AnonProxy. This proxy uses as default only the AN.ON service.
	 * If you also want to use TOR and Mixminion you have to enable them by
	 * calling setTorParams() and setMixmininoParams().
	 *
	 * @see setTorParams()
	 * @see setMixminionParams()
	 *
	 * @param a_listener
	 *          A ServerSocket, where the AnonProxy listens for new requests (e.g.
	 *          from a web browser).
	 * @param a_proxyInterface
	 *          describes a proxy the AnonProxy should use to establish
	 *          connections to the anon servers (e.g. if you are behind some
	 *          firewall etc.)
	 */
	public AnonProxy(ServerSocket a_listener, IMutableProxyInterface a_proxyInterface,
					 IMutableProxyInterface a_paymentProxyInterface)
	{
		if (a_listener == null)
		{
			throw new IllegalArgumentException("Socket listener is null!");
		}

		m_socketListener = a_listener;
		if (a_proxyInterface != null)
		{
			m_proxyInterface = a_proxyInterface;
		}
		// HTTP
		m_Anon = AnonServiceFactory.getAnonServiceInstance(AnonServiceFactory.SERVICE_ANON);
		m_Anon.setProxy(m_proxyInterface);
		( (AnonClient) m_Anon).setPaymentProxy(a_paymentProxyInterface);
		setDummyTraffic(DummyTrafficControlChannel.DT_DISABLE);
		m_forwardedConnection = false;
		m_anonServiceListener = new Vector();
		m_Anon.removeEventListeners();
		m_Anon.addEventListener(this);
		// SOCKS\uFFFD
	}

	/**
	 * Creates a new AnonProxy with an already active mix connection.
	 *
	 * @param a_listener
	 *          A ServerSocket, where the AnonProxy listens for new requests (e.g.
	 *          from a web browser).
	 * @param a_proxyConnection
	 *          An already open connection to a mix (but not initialized, like
	 *          keys exchanged, ...).
	 * @param a_maxDummyTrafficInterval
	 *          The minimum dummy traffic rate the connection needs. The value is
	 *          the maximum dummy traffic interval in milliseconds. Any call of
	 *          setDummyTraffic(), will respect this maximum interval value ->
	 *          bigger values set with setDummyTraffic (especially -1) result in
	 *          that maximum dummy traffic interval value. If this value is -1,
	 *          there is no need for dummy traffic on that connection on the
	 *          server side.
	 */
	public AnonProxy(ServerSocket a_listener, IStreamConnection a_proxyConnection,
					 int a_maxDummyTrafficInterval)
	{
		if (a_listener == null)
		{
			throw new IllegalArgumentException("Socket listener is null!");
		}
		m_socketListener = a_listener;
		m_Anon = new AnonClient(a_proxyConnection); 
		m_forwardedConnection = true;
		m_maxDummyTrafficInterval = a_maxDummyTrafficInterval;
		setDummyTraffic(a_maxDummyTrafficInterval);
		m_anonServiceListener = new Vector();
		m_Anon.removeEventListeners();
		m_Anon.addEventListener(this);
	}
	
	public void setHTTPHeaderProcessingEnabled(boolean enable, IMessages a_messages)
	{
		if(enable)
		{
			if(m_callbackHandler == null)
			{
				LogHolder.log(LogLevel.WARNING, LogType.NET, "No ProxyCallbackHandler activated: cannot process HTTP headers.");
				return;
			}
			if(m_httpProxyCallback == null)
			{
				m_httpProxyCallback = new HTTPProxyCallback(a_messages);
			}
			m_callbackHandler.registerProxyCallback(m_httpProxyCallback);
		}
		else
		{
			if(m_httpProxyCallback != null)
			{
				m_callbackHandler.removeCallback(m_httpProxyCallback);
			}
			m_httpProxyCallback = null;
		}
	}
	
	/* TODO: this also enables the experimental ConnectionWatch
	 * has to be handled by another enable method
	 */
	public void setJonDoFoxHeaderEnabled(boolean enable)
	{
		if(enable)
		{
			if( m_callbackHandler == null)
			{
				LogHolder.log(LogLevel.WARNING, LogType.NET, "No Callbackhandler activated: cannot activate JonDoFox headers.");
				return;
			}
			if(m_httpProxyCallback == null)
			{
				LogHolder.log(LogLevel.WARNING, LogType.NET, "No HTTPProxyCallback activated: cannot activate JonDoFox headers.");
				return;
			}
			
			if(m_jfxHeader == null)
			{
				m_jfxHeader = new JonDoFoxHeader();
			}
			m_httpProxyCallback.addHTTPConnectionListener(m_jfxHeader);
			m_callbackHandler.registerProxyCallback(m_httpProxyCallback);
		}
		else
		{
			if (m_httpProxyCallback != null )
			{
				if(m_jfxHeader != null)
				{
					m_httpProxyCallback.removeHTTPConnectionListener(m_jfxHeader);
					m_jfxHeader = null;
				}
			}
		}
	}
	
	public void setConnnectionWatchEnabled(boolean enabled)
	{
		if(enabled)
		{
			if( m_callbackHandler == null)
			{
				LogHolder.log(LogLevel.WARNING, LogType.NET, "No Callbackhandler activated: cannot enable ConnectionWatch.");
				return;
			}
			if(m_httpProxyCallback == null)
			{
				LogHolder.log(LogLevel.WARNING, LogType.NET, "No HTTPProxyCallback activated: cannot enable ConnectionWatch.");
				return;
			}
			if(m_connectionWatch != null)
			{
					
			}
			m_httpProxyCallback.removeHTTPConnectionListener(m_connectionWatch);
			m_connectionWatch = new HTTPConnectionWatch();
			m_httpProxyCallback.addHTTPConnectionListener(m_connectionWatch);
			m_callbackHandler.registerProxyCallback(m_httpProxyCallback);
		}
		else
		{
			if (m_httpProxyCallback != null )
			{
				if(m_connectionWatch != null)
				{
					m_httpProxyCallback.removeHTTPConnectionListener(m_connectionWatch);
					m_connectionWatch = null;
				}
			}
		}
	}
	
	/**
	 * Sets a new MixCascade.
	 *
	 * @param newMixCascade
	 *          The new MixCascade we are connected to.
	 */
	/*private void setMixCascade(AbstractMixCascadeContainer newMixCascade)
	{
		if (newMixCascade == null)
		{
			m_currentMixCascade = new DummyMixCascadeContainer();
		}
		else
		{
			m_currentMixCascade = new EncapsulatedMixCascadeContainer(newMixCascade);
		}
		// m_AICom.setAnonServer(newMixCascade);
	}
	*/

	/** Returns the current Mix cascade */
	public MixCascade getMixCascade()
	{
		try
		{
			return m_currentMixCascade.getCurrentMixCascade();
		}
		catch (NullPointerException a_e)
		{
			return null;
		}
	}

	/**
	 * Sets the parameter for TOR (anonymous SOCKS). If NULL TOR proxy is
	 * disabled.
	 *
	 * @param newTorParams
	 *          The new parameters for TOR.
	 * @see TorAnonServerDescription
	 */
	public void setTorParams(TorAnonServerDescription newTorParams)
	{
		m_currentTorParams = newTorParams;
	}

	public TorAnonServerDescription getTorParams()
	{
		return m_currentTorParams;
	}

	/**
	 * Sets the parameter for Mixminion (anonymous remailer). If NULL Mixminion
	 * proxy is disabled.
	 *
	 * @param newMixminionParams
	 *          The new parameters for Mixminion. If NULL the Mixminion proxy is
	 *          disabled.
	 * @see MixminionServiceDescription
	 */
	public void setMixminionParams(MixminionServiceDescription newMixminionParams)
	{
		m_currentMixminionParams = newMixminionParams;
	}

	public MixminionServiceDescription getMixminionParams()
	{
		return m_currentMixminionParams;
	}

	public void setMaxConcurrentRequests(int a_maxRequests)
	{
		if (a_maxRequests > MIN_REQUESTS)
		{
			m_maxRequests = a_maxRequests;
		}
	}

	public int getMaxConcurrentRequests()
	{
		return m_maxRequests;
	}

	/**
	 * Changes the dummy traffic interval on the connection to the server. This
	 * method respects dummy traffic restrictions on a forwarded connection. If
	 * there is a minimum dummy traffic rate needed by the server, the dummy
	 * traffic interval gets never bigger than that needed rate on a forwarded
	 * connection (especially a interval value of -1 is ignored).
	 *
	 * @param a_interval
	 *          The interval for dummy traffic on the connection to the server in
	 *          milliseconds.
	 */
	public void setDummyTraffic(int a_interval)
	{
		try
		{
			if ( (!m_forwardedConnection) || 
				 (m_maxDummyTrafficInterval < 0) ||
				 (a_interval == DummyTrafficControlChannel.DT_DISABLE) )
			{
				/* no dummy traffic restrictions */
				( (AnonClient) m_Anon).setDummyTraffic(a_interval);
			}
			else
			{
				/* there are dummy traffic restrictions */
				if (a_interval >= 0)
				{
					/* take the smaller interval */
					( (AnonClient) m_Anon).setDummyTraffic(Math.min(a_interval, m_maxDummyTrafficInterval));
				}
				else
				{
					/*
					 * we need dummy traffic with a minimum rate -> can't disable dummy
					 * traffic
					 */
					( (AnonClient) m_Anon).setDummyTraffic(m_maxDummyTrafficInterval);
				}
			}
		}
		catch (ClassCastException a_e)
		{
			// this is a direct proxy!
		}
	}

	public void stop()
	{
		synchronized (SHUTDOWN_SYNC)
		//synchronized (THREAD_SYNC)
		{
			if (threadRun == null)
			{
				disconnected();
				return;
			}

			bShuttingDown = true;

			m_Anon.shutdown(true);
			if (m_Tor != null)
			{
				m_Tor.shutdown(true);
			}
			if (m_Mixminion != null)
			{
				m_Mixminion.shutdown(true);
			}

			int i = 0;
			while (threadRun.isAlive())
			{
				try
				{
					threadRun.interrupt();
					threadRun.join(1000);
					if (i > 3)
					{
						threadRun.stop();
					}
					i++;
				}
				catch (InterruptedException e)
				{
				}
			}

			m_Tor = null;
			m_Mixminion = null;
			threadRun = null;

			packetMixed(0);
			/*
			synchronized (m_eventListeners)
				{
					final Enumeration eventListenersList = m_eventListeners.elements();
					Thread notificationThread = new Thread(new Runnable()
					{
						public void run()
						{
							while (eventListenersList.hasMoreElements())
							{
								( (AnonServiceEventListener) (eventListenersList.nextElement())).packetMixed(0);
							}
						}
					}, "AnonClient: Zero PacketMixed notification (after shutdown)");
					notificationThread.setDaemon(true);
					notificationThread.start();
			}*/


			disconnected();

			bShuttingDown = false;
		}


		/*
		 synchronized (THREAD_SYNC)
		 {
		  THREAD_SYNC.notify();
		 }*/

	}

	private class OpenSocketRequester implements Runnable
	{
		private ObjectQueue m_socketQueue = new ObjectQueue();
		private AnonProxy m_proxy;
		private Object m_syncObject;
		private boolean m_bIsClosed = false;

		public OpenSocketRequester(AnonProxy a_proxy, Object a_syncObject)
		{
			m_proxy = a_proxy;
			m_syncObject = a_syncObject;
		}



		public void pushSocket(Socket clientSocket)
		{
			synchronized (m_socketQueue)
			{
				m_socketQueue.push(clientSocket);
				m_socketQueue.notify();
			}
		}
		public void close()
		{
			m_bIsClosed = true;
			synchronized (m_socketQueue)
			{
				m_socketQueue.notify();
			}


		}

		public void run()
		{
			while (!Thread.currentThread().isInterrupted() && !m_bIsClosed)
			{
				if (m_socketQueue.getSize() > 0 && AnonProxyRequest.getNrOfRequests() < m_maxRequests)
				{
					try
					{
						new AnonProxyRequest(m_proxy, (Socket)m_socketQueue.pop(), m_syncObject, m_callbackHandler);
					}
					catch (Exception e)
					{
						LogHolder.log(LogLevel.ERR, LogType.NET, e);
					}
				}
				else
				{
					try
					{
						synchronized (m_socketQueue)
						{
							if (AnonProxyRequest.getNrOfRequests() >= m_maxRequests)
							{
								m_socketQueue.wait(100);
							}
							else
							{
								m_socketQueue.wait();
							}
						}
					}
					catch (InterruptedException ex)
					{
						break;
					}
				}
			}
			LogHolder.log(LogLevel.INFO, LogType.NET, "Open socket thread stopped.");
		}
	}


	public void run()
	{
		Thread socketThread;
		OpenSocketRequester requester;
		int oldTimeOut = 0;
		LogHolder.log(LogLevel.DEBUG, LogType.NET, "AnonProxy is running as Thread");

		try
		{
			oldTimeOut = m_socketListener.getSoTimeout();
		}
		catch (Exception e)
		{
		}
		try
		{
			m_socketListener.setSoTimeout(2000);
		}
		catch (Exception e1)
		{
			LogHolder.log(LogLevel.DEBUG, LogType.NET, "Could not set accept time out!", e1);
		}
		requester = new OpenSocketRequester(this, THREAD_SYNC);
		socketThread = new Thread(requester);
		socketThread.start();

		try
		{
			while (!Thread.currentThread().isInterrupted())
			{
				Socket socket = null;
				try
				{
					socket = m_socketListener.accept();
				}
				catch (InterruptedIOException e)
				{
					continue;
				}
				try
				{
					socket.setSoTimeout(0); // ensure that socket is blocking!
					requester.pushSocket(socket);
				}
				catch (SocketException soex)
				{
					socket = null;
					LogHolder.log(LogLevel.ERR, LogType.NET,
								  "Could not set non-Blocking mode for Channel-Socket!", soex);
					continue;
				}


			}
		}
		catch (Exception e)
		{
			LogHolder.log(LogLevel.ERR, LogType.NET, e);
		}
		try
		{
			m_socketListener.setSoTimeout(oldTimeOut);
		}
		catch (Exception e4)
		{
		}
		socketThread.interrupt();
		requester.close();

		try
		{
			socketThread.join();
		}
		catch (InterruptedException ex)
		{
		}

		LogHolder.log(LogLevel.INFO, LogType.NET, "JAPAnonProxyServer stopped.");
	}

	AnonChannel createChannel(int type) throws NotConnectedToMixException, Exception
	{
		if (type == AnonChannel.SOCKS)
		{
			if (m_Tor != null)
			{
				return m_Tor.createChannel(AnonChannel.SOCKS);
			}
			else if (getMixCascade().isSocks5Supported())
			{
				return m_Anon.createChannel(AnonChannel.SOCKS);
			}
			else
			{
				LogHolder.log(LogLevel.ERR, LogType.NET,
							  "Received SOCKS request, but no SOCKS server is available.");
			}
		}
		else if (type == AnonChannel.HTTP)
		{
			return m_Anon.createChannel(AnonChannel.HTTP);
		}
		else if (type == AnonChannel.SMTP)
		{
			if (m_Mixminion != null)
			{
				return m_Mixminion.createChannel(AnonChannel.SMTP);
			}
		}
		return null;
	}

	//synchronized
	void reconnect()
	{
		synchronized (THREAD_SYNC)
		{
			if (m_Anon.isConnected() || bShuttingDown || Thread.currentThread().isInterrupted())
			{
				return;
			}

			if (!m_currentMixCascade.isReconnectedAutomatically())
			{
				stop();
				THREAD_SYNC.notifyAll(); // maybe setting has changed meanwhile
				return;
			}
			if (m_bReconnecting)
			{
				// there already is a reconnect thread
				return;
			}
			m_bReconnecting = true;

			while (threadRun != null && m_currentMixCascade.isReconnectedAutomatically() &&
				   !m_Anon.isConnected() && !Thread.currentThread().isInterrupted())
			{
				LogHolder.log(LogLevel.ERR, LogType.NET, "Try reconnect to AN.ON service");
				int ret = m_Anon.initialize(m_currentMixCascade.getNextMixCascade(), m_currentMixCascade);
				if (ret == ErrorCodes.E_SUCCESS)
				{
					m_currentMixCascade.keepCurrentService(true);
					break;
				}
				try
				{
					THREAD_SYNC.wait(RECONNECT_INTERVAL);
				}
				catch (InterruptedException ex)
				{
					break;
				}
			}
			m_bReconnecting = false;

			if ( (threadRun == null || !isConnected()) &&
				!m_currentMixCascade.isReconnectedAutomatically())
			{
				stop();
				THREAD_SYNC.notifyAll(); // maybe setting has changed meanwhile
				return;
			}

			return;
		}
	}

	public void setProxyListener(IProxyListener l)
	{
		m_ProxyListener = l;
	}

	public int start(AbstractMixCascadeContainer a_newMixCascade)
	{
		synchronized (THREAD_SYNC)
		{
			boolean bConnectionError = false;
			AbstractMixCascadeContainer newMixCascade;
			boolean bSwitch = false;
			if (a_newMixCascade == null)
			{
				newMixCascade = new DummyMixCascadeContainer();
			}
			else
			{
				newMixCascade = new EncapsulatedMixCascadeContainer(a_newMixCascade);
			}

			if (getMixCascade() != newMixCascade.getCurrentMixCascade() && threadRun != null)
			{
				bSwitch = true;
				THREAD_SYNC.notifyAll();
				synchronized (SHUTDOWN_SYNC)
				{
					m_Anon.shutdown(false);

					int i = 0;
					while (threadRun.isAlive())
					{
						try
						{
							threadRun.interrupt();
							threadRun.join(1000);
							if (i > 3)
							{
								threadRun.stop();
							}
							i++;
						}
						catch (InterruptedException e)
						{
						}
					}
				}
			}
			else if (threadRun == null)
			{
				m_Anon.shutdown(true); // reset transferred bytes
			}

			m_currentMixCascade = newMixCascade;

			/*
			  if (cascade.getId().equals("Tor"))
			  {
			 LogHolder.log(LogLevel.NOTICE, LogType.NET, "Using Tor as anon service!");
			 m_Anon = new DirectProxy(m_socketListener);
			  }
			  else
			  {
			 m_Anon = AnonServiceFactory.getAnonServiceInstance(AnonServiceFactory.SERVICE_ANON);
			   }*/

			LogHolder.log(LogLevel.DEBUG, LogType.NET, "Try to initialize AN.ON");
			m_numChannels = 0;
			int ret = m_Anon.initialize(m_currentMixCascade.getNextMixCascade(), m_currentMixCascade);

			if (ret != ErrorCodes.E_SUCCESS)
			{
				if (ret == ErrorCodes.E_INTERRUPTED || !m_currentMixCascade.isReconnectedAutomatically() ||
					(!m_currentMixCascade.isServiceAutoSwitched() &&
					 // these errors cannot be 'healed'
					 (ret == E_SIGNATURE_CHECK_FIRSTMIX_FAILED ||
					  ret == E_SIGNATURE_CHECK_OTHERMIX_FAILED || ret == E_MIX_PROTOCOL_NOT_SUPPORTED ||
					  ret == ErrorCodes.E_NOT_PARSABLE)))
				{
					return ret;
				}
				else
				{
					bConnectionError = true;
				}
			}
			else
			{
				m_currentMixCascade.keepCurrentService(true);
			}
			LogHolder.log(LogLevel.DEBUG, LogType.NET, "AN.ON initialized");

			if (bSwitch)
			{
				synchronized (SHUTDOWN_SYNC)
				{
					if (threadRun == null)
					{
						LogHolder.log(LogLevel.NOTICE, LogType.NET, "Noticed shutdown. Stopping AN.ON...");
						Thread.currentThread().interrupt();
						return ErrorCodes.E_INTERRUPTED;
					}
				}
			}
			else
			{
				if (m_currentTorParams != null)
				{
					m_Tor = AnonServiceFactory.getAnonServiceInstance(AnonServiceFactory.SERVICE_TOR);
					m_Tor.setProxy(m_proxyInterface);
					m_Tor.initialize(m_currentTorParams, null);
					LogHolder.log(LogLevel.DEBUG, LogType.NET, "Tor initialized");
				}
				if (m_currentMixminionParams != null)
				{
					m_Mixminion = AnonServiceFactory.getAnonServiceInstance(AnonServiceFactory.
						SERVICE_MIXMINION);
					m_Mixminion.setProxy(m_proxyInterface);
					m_Mixminion.initialize(m_currentMixminionParams, null);
					LogHolder.log(LogLevel.DEBUG, LogType.NET, "Mixminion initialized");
				}
			}

			threadRun = new Thread(this, "JAP - AnonProxy");
			threadRun.setDaemon(true);
			threadRun.start();

			if (bConnectionError)
			{
				connectionError();
				return ErrorCodes.E_CONNECT;
			}
			return ErrorCodes.E_SUCCESS;
		}
	}

	protected synchronized void decNumChannels()
	{
		m_numChannels--;
		if (m_ProxyListener != null)
		{
			m_ProxyListener.channelsChanged(m_numChannels);
		}
	}

	protected synchronized void incNumChannels()
	{
		m_numChannels++;
		if (m_ProxyListener != null)
		{
			m_ProxyListener.channelsChanged(m_numChannels);
		}
	}

	protected synchronized void transferredBytes(long bytes, int protocolType)
	{
		if (m_ProxyListener != null)
		{
			m_ProxyListener.transferedBytes(bytes, protocolType);
		}
	}

	private void fireDisconnected()
	{
		synchronized (m_anonServiceListener)
		{
			Enumeration e = m_anonServiceListener.elements();
			while (e.hasMoreElements())
			{
				( (AnonServiceEventListener) e.nextElement()).disconnected();
			}
		}
	}

	private void fireConnecting(AnonServerDescription a_serverDescription)
	{
		synchronized (m_anonServiceListener)
		{
			Enumeration e = m_anonServiceListener.elements();
			while (e.hasMoreElements())
			{
				( (AnonServiceEventListener) e.nextElement()).connecting(
					a_serverDescription);
			}
		}
	}

	private void fireConnectionEstablished(AnonServerDescription a_serverDescription)
	{
		synchronized (m_anonServiceListener)
		{
			Enumeration e = m_anonServiceListener.elements();
			while (e.hasMoreElements())
			{
				( (AnonServiceEventListener) e.nextElement()).connectionEstablished(
					a_serverDescription);
			}
		}
	}

	private void fireConnectionError()
	{
		synchronized (m_anonServiceListener)
		{
			Enumeration e = m_anonServiceListener.elements();
			while (e.hasMoreElements())
			{
				( (AnonServiceEventListener) e.nextElement()).connectionError();
			}
		}
	}

	public void connecting(AnonServerDescription a_serverDescription)
	{
		LogHolder.log(LogLevel.INFO, LogType.NET, "AnonProxy received connecting.");
		fireConnecting(a_serverDescription);
	}

	public void connectionEstablished(AnonServerDescription a_serverDescription)
	{
		LogHolder.log(LogLevel.ALERT, LogType.NET,
					  "AnonProxy received connectionEstablished to '" + a_serverDescription + "'.");
		fireConnectionEstablished(a_serverDescription);
	}

	public void disconnected()
	{
		LogHolder.log(LogLevel.ALERT, LogType.NET, "AnonProxy was disconnected.");
		fireDisconnected();
	}

	public void connectionError()
	{
		LogHolder.log(LogLevel.ERR, LogType.NET, "AnonProxy received connectionError", true);
		fireConnectionError();
		new Thread(new Runnable()
		{
			public void run()
			{
				reconnect();
			}
		}, "Connection error reconnect thead").start();
	}

	public synchronized void addEventListener(AnonServiceEventListener l)
	{
		synchronized (m_anonServiceListener)
		{
			Enumeration e = m_anonServiceListener.elements();
			while (e.hasMoreElements())
			{
				if (l.equals(e.nextElement()))
				{
					return;
				}
			}
			m_anonServiceListener.addElement(l);
		}
	}

	public synchronized void removeEventListener(AnonServiceEventListener l)
	{
		m_anonServiceListener.removeElement(l);
	}

	public boolean isConnected()
	{
		AnonService service = m_Anon;
		return service != null && service.isConnected();
	}

	public void addAIListener(IAIEventListener a_aiListener)
	{
		/** @todo check if needed */
		//synchronized (THREAD_SYNC)
		{
			//if (m_Anon instanceof AnonClient)
			{
				try
				{
					(( (AnonClient) m_Anon).getPay()).getAIControlChannel().addAIListener(a_aiListener);
				}
				catch (ClassCastException a_e)
				{
					LogHolder.log(LogLevel.EMERG, LogType.NET, a_e);
				}
			}
		}
	}

	public void packetMixed(long a_totalBytes)
	{
		if (isConnected() || a_totalBytes == 0)
		{
			synchronized (m_anonServiceListener)
			{
				Enumeration e = m_anonServiceListener.elements();
				while (e.hasMoreElements())
				{
					( (AnonServiceEventListener) e.nextElement()).packetMixed(a_totalBytes);
				}
			}
		}
	}

	public void dataChainErrorSignaled()
	{
		LogHolder.log(LogLevel.ERR, LogType.NET, "Proxy has been nuked");
		m_currentMixCascade.keepCurrentService(false);
		m_Anon.shutdown(false);
		synchronized (m_anonServiceListener)
		{
			Enumeration e = m_anonServiceListener.elements();
			while (e.hasMoreElements())
			{
				( (AnonServiceEventListener) e.nextElement()).dataChainErrorSignaled();
			}
		}
		reconnect();
	}

	private class DummyMixCascadeContainer extends AbstractMixCascadeContainer
	{
		public MixCascade getNextMixCascade()
		{
			return null;
		}

		public MixCascade getCurrentMixCascade()
		{
			return null;
		}

		public void keepCurrentService(boolean a_bKeepCurrentCascade)
		{
		}

		public boolean isServiceAutoSwitched()
		{
			return false;
		}

		public boolean isReconnectedAutomatically()
		{
			return false;
		}
		
		public ITermsAndConditionsContainer getTCContainer()
		{
			return null;
		}
	}

	private class EncapsulatedMixCascadeContainer extends AbstractMixCascadeContainer
	{
		private AbstractMixCascadeContainer m_mixCascadeContainer;

		public EncapsulatedMixCascadeContainer(AbstractMixCascadeContainer a_mixCascadeContainer)
		{
			m_mixCascadeContainer = a_mixCascadeContainer;
		}

		public void checkTrust(MixCascade a_cascade) throws TrustException, SignatureException
		{
			m_mixCascadeContainer.checkTrust(a_cascade);
		}

		public MixCascade getNextMixCascade()
		{
			return m_mixCascadeContainer.getNextMixCascade();
		}

		public MixCascade getCurrentMixCascade()
		{
			return m_mixCascadeContainer.getCurrentMixCascade();
		}

		public void keepCurrentService(boolean a_bKeepCurrentCascade)
		{
			m_mixCascadeContainer.keepCurrentService(a_bKeepCurrentCascade);
		}

		public boolean isServiceAutoSwitched()
		{
			return m_mixCascadeContainer.isServiceAutoSwitched();
		}

		public boolean isReconnectedAutomatically()
		{
			/** @todo reconnect is not yet supported with forwarded connections */
			return!m_forwardedConnection && m_mixCascadeContainer.isReconnectedAutomatically();
		}
		
		public ITermsAndConditionsContainer getTCContainer()
		{
			return m_mixCascadeContainer.getTCContainer();
		}
	}
}
