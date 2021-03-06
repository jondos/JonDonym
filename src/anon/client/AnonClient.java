/*
 * Copyright (c) 2006, The JAP-Team
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *   - Redistributions of source code must retain the above copyright notice,
 *     this list of conditions and the following disclaimer.
 *
 *   - Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer in the
 *     documentation and/or other materials provided with the distribution.
 *
 *   - Neither the name of the University of Technology Dresden, Germany nor
 *     the names of its contributors may be used to endorse or promote
 *     products derived from this software without specific prior written
 *     permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package anon.client;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.ConnectException;
import java.net.Socket;
import java.security.SecureRandom;
import java.security.SignatureException;
import java.util.Enumeration;
import java.util.Observable;
import java.util.Observer;
import java.util.Vector;

import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;
import HTTPClient.HTTPConnection;
import HTTPClient.ThreadInterruptedIOException;
import anon.AnonChannel;
import anon.AnonServerDescription;
import anon.AnonService;
import anon.AnonServiceEventListener;
import anon.ErrorCodes;
import anon.IServiceContainer;
import anon.NotConnectedToMixException;
import anon.client.replay.ReplayControlChannel;
import anon.client.replay.TimestampUpdater;
import anon.infoservice.HTTPConnectionFactory;
import anon.infoservice.IMutableProxyInterface;
import anon.infoservice.ImmutableProxyInterface;
import anon.infoservice.MixCascade;
import anon.pay.AIControlChannel;
import anon.pay.IAIEventListener;
import anon.pay.PayAccount;
import anon.pay.PayAccountsFile;
import anon.terms.TermsAndConditionConfirmation;
import anon.terms.TermsAndConditionsReadException;
import anon.terms.TermsAndConditionsResponseHandler;
import anon.transport.connection.ConnectionException;
import anon.transport.connection.IStreamConnection;
import anon.transport.connection.SocketConnection;
import anon.util.JobQueue;
import anon.util.XMLParseException;
/**
 * @author Stefan Lieske
 */
public class AnonClient implements AnonService, Observer, DataChainErrorListener
{
	private static boolean ENABLE_CONTROL_CHANNEL_TEST=false; 
	
	public static final int DEFAULT_LOGIN_TIMEOUT = 30000;
	private static final int FAST_LOGIN_TIMEOUT = 4000; // try the fast timeout first
	private static final int CONNECT_TIMEOUT = 8000;

	private static int m_loginTimeout = DEFAULT_LOGIN_TIMEOUT;
	private static int m_loginTimeoutFastAvailable;

	private Multiplexer m_multiplexer;

	private JobQueue m_queuePacketCount;

	private IMutableProxyInterface m_proxyInterface;

	private Object m_internalSynchronization;

	private IServiceContainer m_serviceContainer;

	private Thread m_threadInitialise;
	private Object SYNC_SHUTDOWN = new Object();

	private Object m_internalSynchronizationForSocket;

	private Object m_internalSynchronizationForDummyTraffic;

	private SocketHandler m_socketHandler;

	private Vector m_eventListeners;

	private PacketCounter m_packetCounter;

	private DummyTrafficControlChannel m_dummyTrafficControlChannel;

	private int m_dummyTrafficInterval = DummyTrafficControlChannel.DT_MAX_INTERVAL_MS;

	private KeyExchangeManager m_keyExchangeManager;

	private IStreamConnection m_streamConnection;

	private boolean m_connected;
	
	private IAIEventListener m_aiEventListener;
	
	static 
	{
		resetInternalLoginTimeout();				
	}

	public AnonClient()
	{
		m_socketHandler = null;
		m_multiplexer = null;
		m_packetCounter = null;
		m_dummyTrafficControlChannel = null;
		m_dummyTrafficInterval = -1;
		m_keyExchangeManager = null;
		m_streamConnection = null;
		m_internalSynchronization = new Object();
		m_internalSynchronizationForSocket = new Object();
		m_internalSynchronizationForDummyTraffic = new Object();
		m_eventListeners = new Vector();
		m_connected = false;
		m_proxyInterface = new IMutableProxyInterface.DummyMutableProxyInterface();
		m_queuePacketCount = new JobQueue("AnonClient Packet count updater");
	}

	public AnonClient(IStreamConnection a_theConnection)
	{
		this();
		m_streamConnection = a_theConnection;
	}

	private static interface StatusThread extends Runnable
	{
		public int getStatus();
	}

	public int initialize(final AnonServerDescription a_mixCascade,
						  final IServiceContainer a_serviceContainer, 
						  final TermsAndConditionConfirmation termsConfirmation)
	{
		if (! (a_mixCascade instanceof MixCascade))
		{
			return ErrorCodes.E_INVALID_SERVICE;
		}
		final MixCascade mixCascade = (MixCascade) a_mixCascade;

		m_serviceContainer = a_serviceContainer;
		
		StatusThread run = new StatusThread()
		{
			int status;

			public int getStatus()
			{
				return status;
			}

			public void run()
			{
				synchronized (m_internalSynchronization)
				{
					if (isConnected())
					{
						LogHolder.log(LogLevel.ERR, LogType.NET,
									  "AnonClient was already connected when connecting!");
						/*
						shutdown();
						if (isConnected())
						{*/
							status = ErrorCodes.E_ALREADY_CONNECTED;
							synchronized (m_threadInitialise)
							{
								m_threadInitialise.notifyAll();
							}
							return;
						//}
					}

					IStreamConnection connectionToMixCascade = null;
					if (m_streamConnection != null)
					{
						connectionToMixCascade = m_streamConnection;
						m_streamConnection = null;
					}
					else
					{
						try
						{
							connectionToMixCascade = connectMixCascade(mixCascade,
								m_proxyInterface.getProxyInterface(false).getProxyInterface());
						}
						catch (InterruptedIOException a_e)
						{
							status = ErrorCodes.E_INTERRUPTED;
							synchronized (m_threadInitialise)
							{
								m_threadInitialise.notifyAll();
							}
							return;
						}
					}
					if (connectionToMixCascade == null)
					{
						status = ErrorCodes.E_CONNECT;
						synchronized (m_threadInitialise)
						{
							m_threadInitialise.notifyAll();
						}
						return;
					}
					status = initializeProtocol(connectionToMixCascade, a_mixCascade, 
							a_serviceContainer, termsConfirmation);
					synchronized (m_threadInitialise)
					{
						m_threadInitialise.notifyAll();
					}
					return;
				}
			}
		};
		synchronized (SYNC_SHUTDOWN)
		{
			m_threadInitialise = new Thread(run);
		}
		m_threadInitialise.start();
		try
		{
			m_threadInitialise.join();
		}
		catch (InterruptedException ex)
		{
			synchronized (m_threadInitialise)
			{
				while (m_threadInitialise.isAlive())
				{
					m_threadInitialise.interrupt();
					try
					{
						m_threadInitialise.wait(500);
					}
					catch (InterruptedException ex1)
					{
					}
				}
			}
			return ErrorCodes.E_INTERRUPTED;
		}

		return run.getStatus();
	}

	public static void setLoginTimeout(int a_loginTimeoutMS)
	{
		if (a_loginTimeoutMS >= 1000)
		{
			m_loginTimeout = a_loginTimeoutMS;
		}
	}
	
	private static void resetInternalLoginTimeout()
	{			
		int maxLoginTimeoutFastAvailable = DEFAULT_LOGIN_TIMEOUT / 1000;
		
		m_loginTimeoutFastAvailable = 
			Math.max(m_loginTimeout / 1000, m_loginTimeout / FAST_LOGIN_TIMEOUT);
				
		if (m_loginTimeoutFastAvailable > maxLoginTimeoutFastAvailable)
		{
			m_loginTimeoutFastAvailable = maxLoginTimeoutFastAvailable;
		}
	}
	
	private static int getInternalLoginTimeout(IServiceContainer a_serviceContainer)
	{
		if (a_serviceContainer != null && m_loginTimeoutFastAvailable > 0 &&
				a_serviceContainer.isReconnectedAutomatically() && 
				a_serviceContainer.isServiceAutoSwitched())
		{
			m_loginTimeoutFastAvailable--;
			return FAST_LOGIN_TIMEOUT;
		}
		
		return m_loginTimeout;
	}

	public static int getLoginTimeout()
	{
		return m_loginTimeout;
	}

	public int setProxy(IMutableProxyInterface a_proxyInterface)
	{
		synchronized (m_internalSynchronization)
		{
			if (a_proxyInterface == null)
			{
				m_proxyInterface = new IMutableProxyInterface.DummyMutableProxyInterface();
			}
			else
			{
				m_proxyInterface = a_proxyInterface;
			}
		}
		return ErrorCodes.E_SUCCESS;
	}

	public void shutdown(boolean a_bResetTransferredBytes)
	{	
		synchronized (m_internalSynchronizationForSocket)
		{
			if (m_socketHandler != null)
			{
				m_socketHandler.deleteObservers();
			}
		}
		
		synchronized (m_internalSynchronization)
		{
			if (m_multiplexer != null)
			{
				m_multiplexer.close();
			}
		}
		
		synchronized (m_internalSynchronizationForSocket)
		{
			if (m_socketHandler != null)
			{
				m_socketHandler.closeSocket();
				m_socketHandler = null;
			}
		}
		synchronized (SYNC_SHUTDOWN)
		{
			if (m_threadInitialise != null)
			{
				synchronized (m_threadInitialise)
				{
					while (m_threadInitialise.isAlive())
					{
						m_threadInitialise.interrupt();
						try 
						{
							m_threadInitialise.wait(100);
						} 
						catch (InterruptedException e) 
						{
							break;
						}
					}
				}
			}
		}
		synchronized (m_internalSynchronization)
		{
			if (m_multiplexer != null)
			{
				m_multiplexer.deleteObservers();
			}
			m_multiplexer = null;
			m_connected = false;

			synchronized (m_internalSynchronizationForDummyTraffic)
			{
				if (m_dummyTrafficControlChannel != null)
				{
					m_dummyTrafficControlChannel.stop();
					m_dummyTrafficControlChannel = null;
				}
			}
			
			if (m_packetCounter != null)
			{
				m_packetCounter.deleteObserver(this);
				if (a_bResetTransferredBytes)
				{
					m_packetCounter = null;
				}
			}
			if (m_keyExchangeManager != null)
			{
				m_keyExchangeManager.removeCertificateLock();
				m_keyExchangeManager = null;
			}
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
		}
	}

	public boolean isConnected()
	{
		return m_connected;
	}

	public AnonChannel createChannel(int a_type) throws ConnectException
	{
		Multiplexer multiplexer = null;
		KeyExchangeManager keyExchangeManager = null;
		synchronized (m_internalSynchronization)
		{
			if (m_multiplexer == null)
			{
				/* not connected */
				throw (new NotConnectedToMixException(
					"AnonClient: createChannel(): The AN.ON client is currently not connected to a mixcascade."));
			}
			/* we don't want to block everything, if chain-creation is waiting
			 * for free resources -> only get the pointers to the necessary objects
			 * within the synchronized part
			 */
			multiplexer = m_multiplexer;
			keyExchangeManager = m_keyExchangeManager;
		}
		FixedRatioChannelsDescription channelsDescription = keyExchangeManager.
			getFixedRatioChannelsDescription();
		if (channelsDescription == null)
		{
			/* old protocol with unlimited channels */
			return (new SingleChannelDataChain(multiplexer.getChannelTable(), this, a_type,
											   keyExchangeManager.isChainProtocolWithFlowControl(),keyExchangeManager.getUpstreamSendMe(),
											   keyExchangeManager.getDownstreamSendMe(),
											   keyExchangeManager.isProtocolWithEnhancedChannelEncryption()));
		}
		return (new TypeFilterDataChain(new SequentialChannelDataChain(multiplexer.getChannelTable(), this,
			channelsDescription.getChainTimeout()), a_type));
	}

	public void addEventListener(AnonServiceEventListener a_eventListener)
	{
		synchronized (m_eventListeners)
		{
			m_eventListeners.addElement(a_eventListener);
		}

	}

	public void removeEventListeners()
	{
		m_eventListeners.removeAllElements();
	}

	public void removeEventListener(AnonServiceEventListener a_eventListener)
	{
		synchronized (m_eventListeners)
		{
			m_eventListeners.removeElement(a_eventListener);
		}
	}

	private void reconnect(final Object a_argument)
	{
		/* shutdown everything */
		new Thread(new Runnable()
		{
			/** @todo We use a thread to avoid deadlocks - is this really the best solution? */
			public void run()
			{
				shutdown(!m_serviceContainer.isReconnectedAutomatically());
				synchronized (m_eventListeners)
				{
					final Enumeration eventListenersList = m_eventListeners.elements();
					Thread notificationThread = new Thread(new Runnable()
					{
						public void run()
						{
							if (a_argument != null && a_argument instanceof Exception)
							{
								LogHolder.log(LogLevel.INFO, LogType.NET, (Exception)a_argument);
							}
							while (eventListenersList.hasMoreElements())
							{
								( (AnonServiceEventListener) (eventListenersList.nextElement())).
									connectionError();
							}
						}
					}, "ConnectionError notification");
					notificationThread.setDaemon(true);
					notificationThread.start();
				}
			}
		}).start();
	}
	
	public void update(Observable a_object, final Object a_argument)
	{
		if ( (a_object == m_socketHandler) && (a_argument instanceof IOException))
		{
			reconnect(a_argument);
		}
		else if (a_object == m_packetCounter)
		{
			JobQueue.Job notificationThread = new JobQueue.Job(true)
			{
				public void runJob()
				{
					Vector eventListeners = m_eventListeners;
					PacketCounter packetCounter = m_packetCounter;
					if (eventListeners != null && packetCounter != null)
					{
						synchronized (eventListeners)
						{
							Enumeration eventListenersList = eventListeners.elements();
							while (eventListenersList.hasMoreElements())
							{
								( (AnonServiceEventListener) (eventListenersList.nextElement())).packetMixed(
									packetCounter.getProcessedPackets() * (long) (MixPacket.getPacketSize()));
							}
						}
					}
				}
			};
			m_queuePacketCount.addJob(notificationThread);
		}
	}

	/**
	 * @todo signals proxy connection error without recovery
	 */
	public void dataChainErrorSignaled()
	{
		synchronized (m_eventListeners)
		{
			final Enumeration eventListenersList = m_eventListeners.elements();
			Thread notificationThread = new Thread(new Runnable()
			{
				public void run()
				{
					while (eventListenersList.hasMoreElements())
					{
						( (AnonServiceEventListener) (eventListenersList.nextElement())).
							dataChainErrorSignaled();
					}
				}
			}, "AnonClient: DataChainErrorSignaled notification");
			notificationThread.setDaemon(true);
			notificationThread.start();
		}
	}

	public void setDummyTraffic(int a_interval)
	{
		synchronized (m_internalSynchronizationForDummyTraffic)
		{
			m_dummyTrafficInterval = a_interval;
			if (m_dummyTrafficControlChannel != null)
			{
				m_dummyTrafficControlChannel.setDummyTrafficInterval(a_interval);
			}
		}
	}

	private IStreamConnection connectMixCascade(final MixCascade a_mixCascade, ImmutableProxyInterface a_proxyInterface) throws
		InterruptedIOException
	{
		LogHolder.log(LogLevel.DEBUG, LogType.NET,
					  "Trying to connect to MixCascade '" + a_mixCascade.toString() + "'...");

		Thread notificationThread = new Thread(new Runnable()
		{
			public void run()
			{
				synchronized (m_eventListeners)
				{
					Enumeration eventListenersList = m_eventListeners.elements();
					while (eventListenersList.hasMoreElements())
					{
						( (AnonServiceEventListener) (eventListenersList.nextElement())).
							connecting(a_mixCascade);
					}
				}
			}
		}, "AnonClient: Connecting notification");
		notificationThread.setDaemon(true);
		notificationThread.start();

		Socket connectedSocket = null;
		HTTPConnection connection;
		int i = 0;
		while ( (i < a_mixCascade.getNumberOfListenerInterfaces()) && (connectedSocket == null) &&
			   (!Thread.currentThread().isInterrupted()))
		{
			/* try out all interfaces of the mixcascade until we have a connection */
			try
			{
				connection = HTTPConnectionFactory.getInstance().createHTTPConnection(a_mixCascade.
					getListenerInterface(i), a_proxyInterface);
				connection.setTimeout(CONNECT_TIMEOUT);
				connectedSocket = connection.Connect();
				/*connectedSocket = new Socket(a_mixCascade.
						getListenerInterface(i).getHost(), a_mixCascade.
						getListenerInterface(i).getPort());*/
			}
			catch (InterruptedIOException e)
			{
				if (e instanceof ThreadInterruptedIOException)
				{
					/* Thread.interrupt() was called while connection-establishment
					 * -> stop all activities
					 */
					LogHolder.log(LogLevel.NOTICE, LogType.NET,
								  "Interrupted while connecting to MixCascade '" + a_mixCascade.toString() +
								  "'.");
					throw e;
				}
				LogHolder.log(LogLevel.ERR, LogType.NET,
							  "Timeout while connecting to MixCascade " + a_mixCascade.toString() +
							  " via " + a_mixCascade.getListenerInterface(i).toString() + "!", e);

			}
			catch (Exception e)
			{
				LogHolder.log(LogLevel.ERR, LogType.NET,
							  "Could not connect to MixCascade " + a_mixCascade.toString() +
							  " via " + a_mixCascade.getListenerInterface(i).toString() + "!", e);
			}
			i++;
		}
		if (connectedSocket != null)
		{
			LogHolder.log(LogLevel.DEBUG, LogType.NET,
						  "Connection to MixCascade '" +
						  a_mixCascade.toString() + "' successfully established - starting key-exchange...");
			return new SocketConnection(connectedSocket);
		}
		else
		{
			LogHolder.log(LogLevel.ERR, LogType.NET,
						  "Failed to connect to MixCascade '" + a_mixCascade.toString() + "'.");
			return null;
		}
	}

	private int initializeProtocol(IStreamConnection a_connectionToMixCascade, final AnonServerDescription a_mixCascade,
								   final IServiceContainer a_serviceContainer, final TermsAndConditionConfirmation termsConfirmation)
	{
		synchronized (m_internalSynchronization)
		{
			try
			{
				try
				{
					/* limit timeouts while login procedure */
					a_connectionToMixCascade.setTimeout(
							getInternalLoginTimeout(a_serviceContainer));
				}
				catch (ConnectionException e)
				{
					/* ignore it */
				}
				
				synchronized (m_internalSynchronizationForSocket)
				{
					if (m_socketHandler != null)
					{
						m_socketHandler.deleteObservers();
					}
					m_socketHandler = new SocketHandler(a_connectionToMixCascade);
				}				

				final Vector exceptionCache = new Vector();
				Thread loginThread = new Thread(new Runnable()
				{
					public void run()
					{
						boolean tcRetry = true;
						int tctry = 0;
						try
						{
							while(tcRetry)
							{
								try
								{
									m_keyExchangeManager = new KeyExchangeManager(m_socketHandler.getInputStream(),
											m_socketHandler.getOutputStream(), (MixCascade) a_mixCascade, a_serviceContainer);
									tcRetry = false;
								}
								catch(TermsAndConditionsReadException tcie)
								{
									if(!termsConfirmation.confirmTermsAndConditions(tcie.getOperators(),
											tcie.getTermsTermsAndConditonsToRead()))
									{
										a_serviceContainer.keepCurrentService(false);
										throw new InterruptedException("Client rejected T&C after reading.");
									}
									
									tctry++;
									if(tctry > 1)
									{
										LogHolder.log(LogLevel.ERR, LogType.NET, 
												"Requesting t&cs after the first try is not allowed!");
										throw new InterruptedException("A second tc request must never be sent.");
									}
									
									m_socketHandler =
										new SocketHandler(
												connectMixCascade( (MixCascade) a_mixCascade,
														m_proxyInterface.getProxyInterface(false).getProxyInterface()));
									
								}
							}
						}
						catch (Exception a_e)
						{
							exceptionCache.addElement(a_e);
						}
					}
				}, "Login Thread");
				loginThread.start();
				try
				{
					// this trick is needed to interrupt the key exchange read operation
					loginThread.join();
				}
				catch (InterruptedException a_e)
				{
					throw a_e;
				}

				if (exceptionCache.size() > 0)
				{
					throw (Exception) exceptionCache.firstElement();
				}
			}
			catch (UnknownProtocolVersionException e)
			{
				LogHolder.log(LogLevel.ERR, LogType.NET, e);
				closeSocketHandler();
				return ErrorCodes.E_PROTOCOL_NOT_SUPPORTED;
			}
			catch (SignatureException a_e)
			{
				LogHolder.log(LogLevel.EXCEPTION, LogType.CRYPTO, a_e);
				closeSocketHandler();
				/** @todo Make this more transparent... */
				return ErrorCodes.E_SIGNATURE_CHECK_OTHERMIX_FAILED;
			}
			catch (InterruptedException a_e)
			{
				LogHolder.log(LogLevel.INFO, LogType.NET, a_e);
				closeSocketHandler();
				return ErrorCodes.E_INTERRUPTED;
			}
			catch (TrustException a_e)
			{
				LogHolder.log(LogLevel.INFO, LogType.NET, a_e);
				closeSocketHandler();
				return ErrorCodes.E_NOT_TRUSTED;
			}
			catch (XMLParseException a_e)
			{
				LogHolder.log(LogLevel.ERR, LogType.NET, a_e);
				closeSocketHandler();
				return ErrorCodes.E_NOT_PARSABLE;
			}
			catch (Exception e)
			{
				LogHolder.log(LogLevel.ERR, LogType.NET, e);
				closeSocketHandler();
				return ErrorCodes.E_UNKNOWN;
			}

			try
			{
				/* try to set infinite timeout */
				a_connectionToMixCascade.setTimeout(0);
			}
			catch (ConnectionException e)
			{
				/* ignore it */
			}

			m_multiplexer = new Multiplexer(m_socketHandler.getInputStream(),
											m_socketHandler.getOutputStream(),
											m_keyExchangeManager, new SecureRandom());
			m_socketHandler.addObserver(this);
			if (m_packetCounter != null)
			{
				// there has been a previous connection
				m_packetCounter = new PacketCounter(m_packetCounter.getProcessedPackets());
			}
			else
			{
				m_packetCounter = new PacketCounter();
			}

			m_multiplexer.addObserver(m_packetCounter);
			m_packetCounter.addObserver(this);
			synchronized (m_internalSynchronizationForDummyTraffic)
			{
				m_dummyTrafficControlChannel =
					new DummyTrafficControlChannel(m_multiplexer, a_serviceContainer);
				m_dummyTrafficControlChannel.setDummyTrafficInterval(m_dummyTrafficInterval);
			}
			//the Test Control Channel if used...
			if(ENABLE_CONTROL_CHANNEL_TEST)
				{
					TestControlChannel t=new TestControlChannel(m_multiplexer, a_serviceContainer);
					t.setMessageInterval(30000);
				}
			
			
			/* maybe we have to start some more services */
			int errorCode = finishInitialization(m_multiplexer, m_keyExchangeManager, 
												 m_packetCounter, a_connectionToMixCascade, a_serviceContainer,
												 m_keyExchangeManager.getConnectedCascade() );
			if (errorCode != ErrorCodes.E_SUCCESS)
			{
				shutdown(!a_serviceContainer.isReconnectedAutomatically());
				return errorCode;
			}
			
						
			//try
			{
				/* try to set infinite timeout */
				//a_connectedSocket.setSoTimeout(0);
			}
			//catch (SocketException e)
			{
				/* ignore it */
			}
			
			//if()
			//{
			connectionEstablished(a_mixCascade);
			//}
			return ErrorCodes.E_SUCCESS;
		}
	}
	
	public void connectionEstablished(final AnonServerDescription a_serverDescription)
	{
		resetInternalLoginTimeout();
		
		Thread notificationThread = new Thread(new Runnable()
		{
			public void run()
			{
				synchronized (m_eventListeners)
				{
					Enumeration eventListenersList = m_eventListeners.elements();
					while (eventListenersList.hasMoreElements())
					{
						( (AnonServiceEventListener) (eventListenersList.nextElement())).
							connectionEstablished(a_serverDescription);
					}
				}
			}
		}, "AnonClient: ConnectionEstablished notification");
		notificationThread.setDaemon(true);
		notificationThread.start();

		LogHolder.log(LogLevel.INFO, LogType.NET,
					  "Connected to MixCascade '" + a_serverDescription.toString() + "'!");
		/* AnonClient successfully started */
		m_connected = true;
	}

	private int finishInitialization(Multiplexer a_multiplexer, KeyExchangeManager a_keyExchangeManager,
									PacketCounter a_packetCounter,
									 IStreamConnection a_connection, IServiceContainer a_serviceContainer,
									 MixCascade a_cascade)
	{
		if (a_keyExchangeManager.isProtocolWithTimestamp())
		{
			/* initialize replay-prevention */
			MixParameters[] mixesWithReplayDetection = a_keyExchangeManager.getMixParameters();
			if (a_keyExchangeManager.getFirstMixSymmetricCipher() != null)
			{
				/* the first mix doesn't need timestamps */
				mixesWithReplayDetection = new MixParameters[a_keyExchangeManager.getMixParameters().length -
					1];
				for (int i = 0; i < a_keyExchangeManager.getMixParameters().length - 1; i++)
				{
					mixesWithReplayDetection[i] = a_keyExchangeManager.getMixParameters()[i + 1];
				}
			}
			try
			{
				new TimestampUpdater(mixesWithReplayDetection,
					new ReplayControlChannel(a_multiplexer, a_serviceContainer));
			}
			catch (Exception e)
			{
				LogHolder.log(LogLevel.ERR, LogType.NET,
							  "Fetching of timestamps failed - closing connection.", e);
				return ErrorCodes.E_UNKNOWN;
			}
		}
		/* it seems that some payment things must be started even if payment is
		 * disabled
		 */
		AIControlChannel aiControlChannel =
			new AIControlChannel(a_multiplexer, a_packetCounter, a_serviceContainer, a_cascade);
		//aiControlChannel.addAIListener(a_aiListener)
		if (a_keyExchangeManager.isPaymentRequired())
		{	
			aiControlChannel.addAIListener(new IAIEventListener()
			{
				public void accountEmpty(PayAccount a_account, MixCascade a_cascade)
				{
					// close the connection due to an empty account; if set, reconnection will be done automatically
					reconnect(null);
				}
			});
			aiControlChannel.setAILoginTimeout(m_loginTimeout);
			return aiControlChannel.sendAccountCert();
		}
		return ErrorCodes.E_SUCCESS;
	}

	private void closeSocketHandler()
	{
		synchronized (m_internalSynchronizationForSocket)
		{
			if (m_socketHandler != null)
			{
				/* we have to check for null because the socket could be closed by another
				 * thread in the meantime
				 */
				m_socketHandler.closeSocket();
				m_socketHandler = null;
			}
		}
	}

}
