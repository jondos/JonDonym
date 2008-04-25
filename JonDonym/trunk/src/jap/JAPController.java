/*
 Copyright (c) 2000 - 2006, The JAP-Team
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
package jap;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.URL;
import java.security.SignatureException;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Locale;
import java.util.Observable;
import java.util.Observer;
import java.util.Random;
import java.util.Vector;
import java.io.BufferedReader;
import java.io.FileReader;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Point;
import java.awt.Window;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;
import javax.swing.UnsupportedLookAndFeelException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;
import anon.AnonServerDescription;
import anon.AnonServiceEventAdapter;
import anon.AnonServiceEventListener;
import anon.ErrorCodes;
import anon.client.AnonClient;
import anon.client.ITrustModel;
import anon.crypto.JAPCertificate;
import anon.crypto.SignatureVerifier;
import anon.infoservice.AbstractMixCascadeContainer;
import anon.infoservice.BlacklistedCascadeIDEntry;
import anon.infoservice.CascadeIDEntry;
import anon.infoservice.Database;
import anon.infoservice.PerformanceInfo;
import anon.infoservice.DatabaseMessage;
import anon.infoservice.DeletedMessageIDDBEntry;
import anon.infoservice.HTTPConnectionFactory;
import anon.infoservice.IDistributable;
import anon.infoservice.IDistributor;
import anon.infoservice.InfoServiceDBEntry;
import anon.infoservice.InfoServiceHolder;
import anon.infoservice.JAPMinVersion;
import anon.infoservice.JAPVersionInfo;
import anon.infoservice.ListenerInterface;
import anon.infoservice.MixCascade;
import anon.infoservice.MixInfo;
import anon.infoservice.StatusInfo;
import anon.infoservice.PreviouslyKnownCascadeIDEntry;
import anon.infoservice.ProxyInterface;
import anon.mixminion.MixminionServiceDescription;
import anon.mixminion.mmrdescription.MMRList;
import anon.pay.BIConnection;
import anon.pay.IAIEventListener;
import anon.pay.PayAccount;
import anon.pay.PayAccountsFile;
import anon.pay.PaymentInstanceDBEntry;
import anon.proxy.AnonProxy;
import anon.proxy.IProxyListener;
import anon.tor.TorAnonServerDescription;
import anon.util.Base64;
import anon.util.ClassUtil;
import anon.util.IMiscPasswordReader;
import anon.util.IPasswordReader;
import anon.util.JobQueue;
import anon.util.ResourceLoader;
import anon.util.XMLUtil;
import forward.server.ForwardServerManager;
import gui.GUIUtils;
import gui.JAPMessages;
import gui.dialog.DialogContentPane;
import gui.dialog.JAPDialog;
import gui.dialog.JAPDialog.LinkedCheckBox;
import gui.dialog.PasswordContentPane;
import jap.forward.JAPRoutingEstablishForwardedConnectionDialog;
import jap.forward.JAPRoutingMessage;
import jap.forward.JAPRoutingSettings;
import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;
import platform.AbstractOS;
import platform.MacOS;
import proxy.DirectProxy;
import proxy.DirectProxy.AllowUnprotectedConnectionCallback;
import update.JAPUpdateWizard;
import jap.pay.AccountUpdater;
import anon.infoservice.ClickedMessageIDDBEntry;

/* This is the Controller of All. It's a Singleton!*/
public final class JAPController extends Observable implements IProxyListener, Observer,
	AnonServiceEventListener, IAIEventListener
{
	/** Messages */
	public static final String MSG_ERROR_SAVING_CONFIG = JAPController.class.getName() +
		"_errorSavingConfig";
	private static final String MSG_DIALOG_ACCOUNT_PASSWORD = JAPController.class.
		getName() + "_dialog_account_password";
	private static final String MSG_ACCOUNT_PASSWORD = JAPController.class.
		getName() + "_account_password";
	private static final String MSG_ENCRYPTACCOUNT = JAPController.class.
		getName() + "_encryptaccount";
	private static final String MSG_ENCRYPTACCOUNTTITLE = JAPController.class.
		getName() + "_encryptaccounttitle";
	private static final String MSG_ACCPASSWORDTITLE = JAPController.class.
		getName() + "_accpasswordtitle";
	private static final String MSG_ACCPASSWORD = JAPController.class.
		getName() + "_accpassword";
	private static final String MSG_ACCPASSWORDENTERTITLE = JAPController.class.
		getName() + "_accpasswordentertitle";
	private static final String MSG_ACCPASSWORDENTER = JAPController.class.
		getName() + "_accpasswordenter";
	private static final String MSG_LOSEACCOUNTDATA = JAPController.class.
		getName() + "_loseaccountdata";
	private static final String MSG_REPEAT_ENTER_ACCOUNT_PASSWORD = JAPController.class.getName() +
		"_repeatEnterAccountPassword";
	private static final String MSG_DISABLE_GOODBYE = JAPController.class.getName() +
		"_disableGoodByMessage";
	private static final String MSG_NEW_OPTIONAL_VERSION = JAPController.class.getName() +
		"_newOptionalVersion";
	private static final String MSG_CASCADE_NOT_TRUSTED = JAPController.class.getName() +
		"_cascadeNotTrusted";

	private static final String MSG_ALLOWUNPROTECTED = JAPController.class.getName() + "_allowunprotected";
	public static final String MSG_IS_NOT_ALLOWED = JAPController.class.getName() + "_isNotAllowed";
	public static final String MSG_ASK_SWITCH = JAPController.class.getName() + "_askForSwitchOnError";
	public static final String MSG_ASK_RECONNECT = JAPController.class.getName() + "_askForReconnectOnError";
	public static final String MSG_ASK_AUTO_CONNECT = JAPController.class.getName() + "_reallyAutoConnect";
	public static final String MSG_FINISHING = JAPController.class.getName() + "_finishing";
	public static final String MSG_SAVING_CONFIG = JAPController.class.getName() + "_savingConfig";
	public static final String MSG_CLOSING_DIALOGS = JAPController.class.getName() + "_closingDialogs";
	public static final String MSG_FINISHING_IS_UPDATES = JAPController.class.getName() + "_finishISUpdates";
	public static final String MSG_FINISHING_ANON = JAPController.class.getName() + "_finishAnon";
	public static final String MSG_WAITING_IS = JAPController.class.getName() + "_waitingIS";
	public static final String MSG_WAITING_ANON = JAPController.class.getName() + "_waitingAnon";
	public static final String MSG_STOPPING_PROXY = JAPController.class.getName() + "_stoppingProxy";
	public static final String MSG_STOPPING_LISTENER = JAPController.class.getName() + "_stoppingListener";
	public static final String MSG_RESTARTING = JAPController.class.getName() + "_restarting";
	public static final String MSG_FINISH_FORWARDING_SERVER = JAPController.class.getName() +
		"_finishForwardingServer";
	public static final String MSG_VERSION_RELEASE = JAPController.class.getName() + "_versionRelease";
	public static final String MSG_VERSION_DEVELOPER = JAPController.class.getName() + "_versionDeveloper";
	public static final String MSG_ASK_WHICH_VERSION = JAPController.class.getName() + "_askWhichVersion";
	private static final String MSG_CASCADE_NOT_PARSABLE =
		JAPController.class.getName() + "_cascadeNotParsable";
	public static final String MSG_PAYMENT_DAMAGED = JAPController.class.getName() + "_paymentDamaged";
	public static final String MSG_ACCOUNT_NOT_SAVED = JAPController.class.getName() + "_accountNotSaved";


	private static final String XML_ELEM_LOOK_AND_FEEL = "LookAndFeel";
	private static final String XML_ELEM_LOOK_AND_FEELS = "LookAndFeels";
	private static final String XML_ATTR_LOOK_AND_FEEL = "current";
	private static final String XML_ALLOW_NON_ANONYMOUS_CONNECTION = "AllowDirectConnection";
	private static final String XML_ALLOW_NON_ANONYMOUS_UPDATE = "AllowDirectUpdate";
	private static final String XML_ATTR_AUTO_CHOOSE_CASCADES = "AutoSwitchCascades";
	private static final String XML_ATTR_AUTO_CHOOSE_CASCADES_ON_STARTUP = "autoSwitchCascadesOnStartup";
	private static final String XML_ATTR_SHOW_CONFIG_ASSISTANT = "showConfigAssistant";
	private static final String XML_ATTR_LOGIN_TIMEOUT = "loginTimeout";
	private static final String XML_ATTR_INFOSERVICE_CONNECT_TIMEOUT = "isConnectionTimeout";
	private static final String XML_ATTR_ASK_SAVE_PAYMENT = "askIfNotSaved";
	private static final String XML_ATTR_SHOW_SPLASH_SCREEN = "ShowSplashScreen";

	// store classpath as it may not be created successfully after update
	private final String CLASS_PATH = ClassUtil.getClassPath().trim();

	private final Object PROXY_SYNC = new Object();

	private String m_commandLineArgs = "";
	private Process m_portableFirefoxProcess = null;
	boolean m_firstPortableFFStart = false;
	/**
	 * Stores all MixCascades we know (information comes from infoservice or was entered by a user).
	 * This list may or may not include the current active MixCascade.
	 */
	//private Vector m_vectorMixCascadeDatabase = null;

	private boolean m_bShutdown = false;
	private Vector m_programExitListeners = new Vector();

	private boolean m_bShowConfigAssistant = false;
	private boolean m_bAssistantClicked = false;

	private JobQueue m_anonJobQueue;

	private JobQueue queueFetchAccountInfo;
	private long m_lastBalanceUpdateMS = 0;
	private long m_lastBalanceUpdateBytes = 0;
	/** How many milliseconds to wait before requesting a new account statement */
	private static final long ACCOUNT_UPDATE_INTERVAL_MS = 60000;



	/**
	 * Stores the active MixCascade.
	 */
	private MixCascade m_currentMixCascade = null;

	private ServerSocket m_socketHTTPListener = null; // listener object for HTTP

	private DirectProxy m_proxyDirect = null; // service object for direct access (bypass anon service)
	private AnonProxy m_proxyAnon = null; // service object for anon access

	private AccountUpdater m_AccountUpdater;
	private InfoServiceUpdater m_InfoServiceUpdater;
	private PaymentInstanceUpdater m_paymentInstanceUpdater;
	private MixCascadeUpdater m_MixCascadeUpdater;
	private MinVersionUpdater m_minVersionUpdater;
	private JavaVersionUpdater m_javaVersionUpdater;
	private MessageUpdater m_messageUpdater;
	private PerformanceInfoUpdater m_perfInfoUpdater;
	
	private Object LOCK_VERSION_UPDATE = new Object();
	private boolean m_bShowingVersionUpdate = false;

	private boolean m_bAskAutoConnect = false;

	private boolean isRunningHTTPListener = false; // true if a HTTP listener is running

	private boolean mbActCntMessageNotRemind = false; // indicates if Warning message in setAnonMode has been deactivated for the session
	private boolean mbActCntMessageNeverRemind = false; // indicates if Warning message in setAnonMode has been deactivated forever
	private boolean mbDoNotAbuseReminder = false; // indicates if new warning message in setAnonMode (containing Do no abuse) has been shown
	private boolean m_bForwarderNotExplain = false; //indicates if the warning message about forwarding should be shown
	private boolean m_bPayCascadeNoAsk = false;

	private boolean m_bAskSavePayment;
	private boolean m_bPresentationMode = false;
	private boolean m_bPortableJava = false;
	private boolean m_bPortable = false;

	private long m_nrOfBytesWWW = 0;
	private long m_nrOfBytesOther = 0;

	private IJAPMainView m_View = null;
	private boolean m_bMainView = true;
	private Object SYNC_VIEW = new Object();
	private static JAPController m_Controller = null;
	private static JAPModel m_Model = null;
	private static JAPFeedback m_feedback = null;
	private Vector observerVector = null;
	private Vector m_anonServiceListener;
	private IPasswordReader m_passwordReader;
	private Object m_finishSync = new Object();
	private ISplashResponse m_finishSplash;

	private DirectProxy.AllowUnprotectedConnectionCallback m_proxyCallback;

	/** Holds the MsgID of the status message after the forwarding server was started.*/
	private int m_iStatusPanelMsgIdForwarderServerStatus;

	private JAPController()
	{
		m_Model = JAPModel.getInstance();
	}
	
	public void start() {
		// simulate database distributor
		Database.registerDistributor(new IDistributor()
		{
			public void addJob(IDistributable a_distributable)
			{
			}
		});

		// set recover timeout
		InfoServiceDBEntry.setJVMNetworkErrorHandling(new Runnable()
		{
			public void run()
			{
				JAPController.getInstance().goodBye(false);
			}
		},
			JAPConstants.TIME_RESTART_AFTER_SOCKET_ERROR);

		// initialise IS update threads
		m_feedback = new JAPFeedback();
		m_AccountUpdater = new AccountUpdater();
		m_InfoServiceUpdater = new InfoServiceUpdater();
		m_paymentInstanceUpdater = new PaymentInstanceUpdater();
		m_MixCascadeUpdater = new MixCascadeUpdater();
		m_minVersionUpdater = new MinVersionUpdater();
		m_javaVersionUpdater = new JavaVersionUpdater();
		m_messageUpdater = new MessageUpdater();
		m_perfInfoUpdater = new PerformanceInfoUpdater();

		m_anonJobQueue = new JobQueue("Anon mode job queue");
		m_Model.setAnonConnectionChecker(new AnonConnectionChecker());
		InfoServiceDBEntry.setMutableProxyInterface(m_Model.getInfoServiceProxyInterface());

		queueFetchAccountInfo = new JobQueue("FetchAccountInfoJobQueue");

		// Create observer object
		observerVector = new Vector();
		// create service listener object
		m_anonServiceListener = new Vector();

		// initialise HTTP proxy
		if (!JAPModel.isSmallDisplay())
		{
			m_proxyCallback = new DirectProxy.AllowUnprotectedConnectionCallback()
			{
				public DirectProxy.AllowUnprotectedConnectionCallback.Answer callback(
								DirectProxy.RequestInfo a_requestInfo
					)
				{
					if (JAPModel.getInstance().isNonAnonymousSurfingDenied() ||
						JAPController.getInstance().getView() == null)
					{
						return new Answer(false, false);
					}

					boolean bShowHtmlWarning;
					JAPDialog.LinkedCheckBox cb = new JAPDialog.LinkedCheckBox(
									   JAPMessages.getString(JAPDialog.LinkedCheckBox.MSG_REMEMBER_ANSWER), false,
									   MSG_ALLOWUNPROTECTED)
					{
						public boolean isOnTop()
						{
							return true;
						}
					};
					bShowHtmlWarning = ! (JAPDialog.showYesNoDialog(
									   JAPController.getInstance().getViewWindow(),
									   JAPMessages.getString(MSG_ALLOWUNPROTECTED), a_requestInfo.getURI()
									   + (a_requestInfo.getPort() != 80 ? ":" + a_requestInfo.getPort() : ""), cb));
					if (bShowHtmlWarning && cb.getState())
					{
						// user has chosen to never allow non anonymous websurfing
						JAPModel.getInstance().denyNonAnonymousSurfing(true);
						// do not remember as this may be switched in the control panel
						return new Answer(!bShowHtmlWarning, false);
					}
					return new Answer(!bShowHtmlWarning, cb.getState());
				}
			};

			DirectProxy.setAllowUnprotectedConnectionCallback(m_proxyCallback);
		}
		/* set a default mixcascade */
		try
		{
			Vector listeners = new Vector();
			for (int j = 0; j < JAPConstants.DEFAULT_ANON_HOSTS.length; j++)
			{
				for (int i = 0; i < JAPConstants.DEFAULT_ANON_PORT_NUMBERS.length; i++)
				{
					listeners.addElement(new ListenerInterface(JAPConstants.DEFAULT_ANON_HOSTS[j],
						JAPConstants.DEFAULT_ANON_PORT_NUMBERS[i],
						ListenerInterface.PROTOCOL_TYPE_RAW_TCP));
				}
			}
			Vector mixIDs = new Vector(JAPConstants.DEFAULT_ANON_MIX_IDs.length);
			for (int i = 0; i < JAPConstants.DEFAULT_ANON_MIX_IDs.length; i++)
			{
				mixIDs.addElement(JAPConstants.DEFAULT_ANON_MIX_IDs[i]);
			}
			m_currentMixCascade = new MixCascade(JAPMessages.getString(JAPConstants.DEFAULT_ANON_NAME),
												 JAPConstants.DEFAULT_ANON_MIX_IDs[0], mixIDs, listeners,
												 //System.currentTimeMillis() + Constants.TIMEOUT_MIXCASCADE);
												 System.currentTimeMillis());
			m_currentMixCascade.setUserDefined(false, null);
			m_currentMixCascade.showAsTrusted(true);
			Database.getInstance(CascadeIDEntry.class).update(new CascadeIDEntry(m_currentMixCascade));
			Database.getInstance(PreviouslyKnownCascadeIDEntry.class).update(
						 new PreviouslyKnownCascadeIDEntry(m_currentMixCascade));
		}
		catch (Exception e)
		{
			LogHolder.log(LogLevel.EMERG, LogType.NET, e);
		}
		/* set a default infoservice */
		try
		{
			InfoServiceDBEntry[] defaultInfoService = JAPController.createDefaultInfoServices();
			for (int i = 0; i < defaultInfoService.length; i++)
			{
				Database.getInstance(InfoServiceDBEntry.class).update(defaultInfoService[i]);
			}
			InfoServiceHolder.getInstance().setPreferredInfoService(defaultInfoService[0]);
		}
		catch (Exception e)
		{
			LogHolder.log(LogLevel.EMERG, LogType.NET,
						  "JAPController: Constructor - default info service: " + e.getMessage());
		}
		/* set some default values for infoservice communication */
		setInfoServiceDisabled(JAPConstants.DEFAULT_INFOSERVICE_DISABLED);

		addDefaultCertificates();
		SignatureVerifier.getInstance().setCheckSignatures(JAPConstants.DEFAULT_CERT_CHECK_ENABLED);

		HTTPConnectionFactory.getInstance().setTimeout(JAPConstants.DEFAULT_INFOSERVICE_TIMEOUT);

		m_proxyDirect = null;
		m_proxyAnon = null;
		//m_proxySocks = null;

		m_passwordReader = new JAPFirewallPasswdDlg();

		/* we want to observe some objects */
		JAPModel.getInstance().getRoutingSettings().addObserver(this);
		JAPModel.getInstance().getRoutingSettings().getServerStatisticsListener().addObserver(this);
		JAPModel.getInstance().getRoutingSettings().getRegistrationStatusObserver().addObserver(this);
		m_iStatusPanelMsgIdForwarderServerStatus = -1;
	}

	/** Creates the Controller - as Singleton.
	 *  @return The one and only JAPController
	 */
	public static JAPController getInstance()
	{
		if (m_Controller == null)
		{
			m_Controller = new JAPController();
		}
		return m_Controller;
	}

	public static interface ProgramExitListener
	{
		public void programExiting();
	}


	public class AnonConnectionChecker
	{
		public boolean checkAnonConnected()
		{
			return isAnonConnected();
		}
	}

	public void addProgramExitListener(ProgramExitListener a_listener)
	{
		if (a_listener != null && !m_programExitListeners.contains(a_listener))
		{
			m_programExitListeners.addElement(a_listener);
		}
	}

	public void setPresentationMode(boolean a_bPresentationMode)
	{
		m_bPresentationMode = a_bPresentationMode;
	}

	public void setPortableJava(boolean a_bPortable)
	{
		m_bPortableJava = a_bPortable;
	}

	public boolean hasPortableJava()
	{
		return m_bPortableJava;
	}

	public void setPortableMode(boolean a_bPortable)
	{
		m_bPortable = a_bPortable;
	}

	public boolean isPortableMode()
	{
		return m_bPortable;
	}

	public void setCommandLineArgs(String a_cmdArgs)
	{
		if (a_cmdArgs != null)
		{
			m_commandLineArgs = a_cmdArgs;
		}
	}

	/**
	 * Returns the password reader.
	 * @return the password reader
	 */
	public IPasswordReader getPasswordReader()
	{
		return m_passwordReader;
	}

	//---------------------------------------------------------------------
	public void initialRun(String a_listenerHost, int a_listenerPort)
	{
		LogHolder.log(LogLevel.INFO, LogType.MISC, "Initial run of JAP...");

		// start update threads and prevent waining for locks by using a thread
		Database.getInstance(JAPMinVersion.class).addObserver(this);
		Thread run = new Thread(new Runnable()
		{
			public void run()
			{
				m_feedback.start(false);
				if (JAPModel.getInstance().isInfoServiceDisabled())
				{
					m_InfoServiceUpdater.start(false);
					m_paymentInstanceUpdater.start(false);
					m_MixCascadeUpdater.start(false);
					m_minVersionUpdater.start(false);
					m_javaVersionUpdater.start(false);
					m_messageUpdater.start(false);
					m_perfInfoUpdater.start(false);
				}
				else
				{
					if (!m_InfoServiceUpdater.isFirstUpdateDone())
					{
						m_InfoServiceUpdater.update();
					}
					if (!m_paymentInstanceUpdater.isFirstUpdateDone())
					{
						m_paymentInstanceUpdater.update();
					}
					if (!m_MixCascadeUpdater.isFirstUpdateDone())
					{
						m_MixCascadeUpdater.update();
					}
					if (!m_minVersionUpdater.isFirstUpdateDone())
					{
						m_minVersionUpdater.update();
					}
					if (!m_javaVersionUpdater.isFirstUpdateDone())
					{
						m_javaVersionUpdater.update();
					}
					if (!m_messageUpdater.isFirstUpdateDone())
					{
						m_messageUpdater.update();
					}
					if (!m_perfInfoUpdater.isFirstUpdateDone())
					{
						m_perfInfoUpdater.update();
					}
				}

				m_AccountUpdater.start(false);
			}
		});
		run.setDaemon(true);
		run.start();


		// start http listener object
		/* if (JAPModel.isTorEnabled())
		 {
		   startSOCKSListener();
		 }*/
		if (!startHTTPListener(a_listenerHost, a_listenerPort))
		{ // start was not sucessful
			Object[] args =
				{
				new Integer(a_listenerPort <= 0 ? JAPModel.getHttpListenerPortNumber() : a_listenerPort)};
			// output error message
			JAPDialog.showErrorDialog(
				getViewWindow(), JAPMessages.getString("errorListenerPort", args) +
				"<br><br>" +
				JAPMessages.getString(JAPConf.MSG_READ_PANEL_HELP, new Object[]
									  {
									  JAPMessages.getString("confButton"),
									  JAPMessages.getString("confListenerTab")}), LogType.NET,
				new JAPDialog.LinkedHelpContext("portlistener")
			{
				public boolean isOnTop()
				{
					return true;
				}
			});

			setAnonMode(false);
			m_View.disableSetAnonMode();
			notifyJAPObservers();
		}
		else if (!SignatureVerifier.getInstance().isCheckSignatures())
		{
			setAnonMode(false);
			JAPDialog.showWarningDialog(
				getViewWindow(),
				JAPMessages.getString(JAPConfCert.MSG_NO_CHECK_WARNING),
				new JAPDialog.LinkedHelpContext("cert")
			{
				public boolean isOnTop()
				{
					return true;
				}
			});
		}
		else
		{
			new Thread(new Runnable()
			{
				public void run()
				{
					if (JAPController.getInstance().isConfigAssistantShown() &&
						!(JAPDialog.isConsoleOnly()||JAPModel.isSmallDisplay()) &&
						!isPortableMode())
					{
						showInstallationAssistant();
					}
				}
			}).start();

			if (m_bAskAutoConnect)
			{
				if (JAPDialog.showYesNoDialog(getViewWindow(), JAPMessages.getString(MSG_ASK_AUTO_CONNECT),
					new JAPDialog.LinkedHelpContext("services_general")))
				{
					JAPModel.getInstance().setAutoConnect(true);
				}
				else
				{
					JAPModel.getInstance().setAutoConnect(false);
				}
			}

			// listener has started correctly
			// do initial setting of anonMode
			if (JAPModel.isAutoConnect() &&
				JAPModel.getInstance().getRoutingSettings().isConnectViaForwarder())
			{
				/* show the connect via forwarder dialog -> the dialog will do the remaining things */
				new JAPRoutingEstablishForwardedConnectionDialog(getViewWindow());
				notifyObservers();
			}
			else
			{
				setAnonMode(JAPModel.isAutoConnect());
			}
		}
	}

	public boolean isAskSavePayment()
	{
		return m_bAskSavePayment;
	}

	public void setAskSavePayment(boolean a_bAsk)
	{
		synchronized (this)
		{
			if (m_bAskSavePayment != a_bAsk)
			{
				m_bAskSavePayment = a_bAsk;
				setChanged();
				notifyObservers(new JAPControllerMessage(JAPControllerMessage.ASK_SAVE_PAYMENT_CHANGED));
			}
		}
	}


	public boolean isShuttingDown()
	{
		return m_bShutdown;
	}

	//---------------------------------------------------------------------
	/** Loads the Configuration.
	 * First tries to read a config file provided on the command line.
	 * If none is provided, it will look in the operating system specific locations
	 * for configuration files (e.g. Library/Preferences on Mac OS X or hidden
	 * in the user's home on Linux).
	 * If there are no config files in these locations, the method will look
	 * in the user's home directory and in the installation path of JAP
	 * (the last two locations are checked for compatibility reasons and are deprecated).
	 *
	 * The configuration is a XML-File with the following structure:
	 *  <JAP
	 *    version="0.25"                     // version of the xml struct (DTD) used for saving the configuration
	 *    portNumber=""                     // Listener-Portnumber
	 *    portNumberSocks=""                // Listener-Portnumber for SOCKS
	 *    supportSocks=""                   // Will we support SOCKS ?
	 *    listenerIsLocal="true"/"false"    // Listener lauscht nur an localhost ?
	 *    proxyMode="true"/"false"          // Using a HTTP-Proxy??
	 *    proxyType="SOCKS"/"HTTP"          // which kind of proxy
	 *    proxyHostName="..."               // the Hostname of the Proxy
	 *    proxyPortNumber="..."             // port number of the Proxy
	 *    proxyAuthorization="true"/"false" // Need authorization to acces the proxy ?
	 *    porxyAuthUserID="..."             // UserId for the Proxy if Auth is neccessary
	 *    infoServiceDisabled="true/false"  // disable use of InfoService
	 *    infoServiceTimeout="..."          // timeout (sec) for infoservice and update communication (since config version 0.5)
	 *    autoConnect="true"/"false"    // should we start the anon service immedialy after programm launch ?
	 *    autoReConnect="true"/"false"    // should we automatically reconnect to mix if connection was lost ?
	 *    DummyTrafficIntervall=".."    //Time of inactivity in milli seconds after which a dummy is send
	 *    minimizedStartup="true"/"false" // should we start minimized ???
	 *    neverRemindActiveContent="true"/"false" // should we remind the user about active content ?
	 *    neverAskPayment="true"/"false" // should we remind the user about payment for cascades ?
	 *    Locale="LOCALE_IDENTIFIER" (two letter iso 639 code) //the Language for the UI to use
	 *    LookAndFeel="..."             //the LookAndFeel class name
	 *  >
	 * <MixCascades>								//info about known MixCascades (since version 0.16)
	 *	<MixCascade>...</MixCascade>
	 * </MixCascades>							   //at the moment only user defined cascades are stored
	 * <MixCascade id=.." userDefined="true/false">  //info about the used AnonServer (since version 0.1) [equal to the general MixCascade struct]
	 *												//Attr "userDefined" since Version 0.12
	 * 												//if true this cascade information was handcrafted by the user
	 * 												//otherwise it comes from the InfoService
	 *   <Name>..</Name>
	 *   <Network>
	 *     <ListenerInterfaces>
	 *       <ListenerInterface> ... </ListenerInterface>
	 *     </ListenerInterfaces>
	 *   </Network>
	 * </MixCascade>
	 * <GUI> //since version 0.2 --> store the position and size of JAP on the Desktop
	 *    <MainWindow> //for the Main Window
	 *       <SetOnStartup>"true/false"</SetOnStartup> //remember Position ?
	 *       <Location x=".." y=".."> //Location of the upper left corner
	 *       <Size dx=".." dy=.."> //Size of the Main window
	 *       <DefaultView>Normal|Simplified</DefaultView> //Which view of JAP to show? (since version 0.11); default: Normal
	 * 		 <MoveToSystray>"true"/"false"</MoveToSystray> //After start move JAP into the systray? (since version 0.11); default: false
	 *     </MainWindow>
	 * </GUI>
	 * <Debug>                          //info about debug output
	 *    <Level>..</Level>              // the amount of output (0 means less.. 7 means max)
	 *    <Detail>..</Detail>          // the detail level of the log output, sinver version 0.21
	 *    <Type                          // which type of messages should be logged
	 *      GUI="true"/"false"          // messages related to the user interface
	 *      NET="true"/"false"          // messages related to the network
	 *      THREAD="true"/"false"        // messages related to threads
	 *      MISC="true"/"false"          // all the others
	 *    >
	 *    </Type>
	 *    <Output>...                      //the kind of Output, at the moment only: if NodeValue==Console --> Console
	 *       <File>...                    //if given, log to the given File (since version 0.14)
	 *       </File>
	 *    </Output>
	 *
	 * </Debug>
	 * <SignatureVerification>                                   // since version 0.18
	 *   <CheckSignatures>true</CheckSignatures>                 // whether signature verification of received XML data is enabled or disabled
	 *   <TrustedCertificates>                                   // list of all certificates to uses for signature verification
	 *     <CertificateContainer>
	 *       <CertificateType>1</CertificateType>                              // the type of the stored certificate (until it's stored within the certificate itself), see JAPCertificate.java
	 *       <CertificateNeedsVerification>true<CertificateNeedsVerification>  // whether the certificate has to be verified against an active root certificate from the certificat store in order to get activated itself
	 *       <CertificateEnabled>true<CertificateEnabled>                      // whether the certificate is enabled (available for signature verification) or not
	 *       <CertificateData>
	 *         <X509Certificate>...</X509Certificate>                          // the certificate data, see JAPCertificate.java
	 *       </CertificateData>
	 *     </CertificateContainer>
	 *     ...
	 *   </TrustedCertificates>
	 * </SignatureVerification>
	 * <InfoServiceManagement>                                    // since config version 0.19
	 *   <InfoServices>                                           // info about all known infoservices
	 *   <InfoService id="...">...</InfoService>                // the same format as from infoservice, without signature, if expired, it is removed from infoservice list
	 *   <InfoService id="...">...</InfoService>
	 * </InfoServices>
	 *   <PreferredInfoService>                                   // info about the preferred infoservice, only one infoservice is supported here
	 *   <InfoService id="...">...</InfoService>                // the same format as from infoservice, without signature, expire time does not matter
	 * </PreferedInfoService>
	 *   <ChangeInfoServices>true<ChangeInfoServices>             // whether it is tried to change the infoservice automatically after failure
	 * </InfoServiceManagement>
	 * <Tor>    //  Tor related seetings (since Version 0.6)
	 * 	 <MaxConnectionsPerRoute>...</MaxConnectionsPerRoute>(since Vresion 0.8) //How many connections are allowed before a new circuit is created
	 * 	 <RouteLen min=" " max=" "/>(since Vresion 0.9) //How long should a route be
	 *   <PreCreateAnonRoutes>True/False</PreCreateAnonRoutes> //Should the routes be created in advance?
	 *   <DirectoryServer useNoneDefault="true|false" Hostname="..." Port="..." /> //since 0.25 Use none default Tor directory server
	 * </Tor>
	 * <Mixminion>    //  Mixminion related seetings (since Version 0.22)
	 * 	 <RouteLen>...</RouteLen> //How long should a route be
	 * 	 <MixminionREPLYMail>xxx@yyy.xyz</MixminionREPLYMail> //destination of the replyblock route
	 * 	 <MixminionPasswordHash>as String Base64</MixminionPasswordHash> //Hash of the user pw
	 *   <KeyRing>String ASCII-Armor and Base64</KeyRing> //Keyring with usersecrets
	 * </Mixminion>
	 * <Payment //Since version 0.7
	 *    biHost="..."                      // BI's Hostname
	 *    biPort="..."                      // BI's portnumber
	 * >
	 *   <EncryptedData>  // Account data encrypted with password
	 *      <Accounts>
	 *        <Account>.....</Account>
	 *        <Account>.....</Account>
	 *      </Accounts>
	 *   </EncryptedData>
	 * </Payment>
	 * <JapForwardingSettings>                                   // since version 0.10, if WITH_BLOCKINGRESISTANCE is enabled
	 *   <ForwardingServer>
	 *     <ServerPort>12345</ServerPort>                        // the port number, where the forwarding server is listening
	 *     <ServerRunning>false</ServerRunning>                  // whether the forwarding server shall be started, when JAP is starting
	 *     <ConnectionClassSettings>
	 *       <ConnectionClasses>                                 // list of all connection classes including settings
	 *         <ConnectionClass>                                 // a single connection class entry
	 *           <ClassIdentifier>0</ClassIdentifier>            // the identifier of the connection class
	 *           <MaximumBandwidth>10000</MaximumBandwidth>      // the maximum bandwidth (bytes/sec) the class provides
	 *           <RelativeBandwidth>50</RelativeBandwidth>       // since version 0.17, the percentage of the bandwidth useable for forwarding
	 *         </ConnectionClass>
	 *         ...
	 *       </ConnectionClasses>
	 *       <CurrentConnectionClass>0</CurrentConnectionClass>  // the currently selected connection class (identifier)
	 *     </ConnectionClassSettings>
	 *     <InfoServiceRegistrationSettings>
	 *       <UseAllPrimaryInfoServices>false</UseAllPrimaryInfoServices>  // whether to registrate the local forwarding server at all infoservices with a forwarder list
	 *       <RegistrationInfoServices>                                    // a list of InfoServices, where the local forwarding server shall be registrated on startup
	 *         <InfoService>...</InfoService>
	 *         ...
	 *       </RegistrationInfoServices>
	 *     </InfoServiceRegistrationSettings>
	 *     <AllowedMixCascadesSettings>
	 *       <AllowAllAvailableMixCascades>true</AllowAllAvailableMixCascades>  // whether the clients of the local forwarding server are allowed to use all running mixcascades
	 *       <AllowedMixCascades>                                               // a list of MixCascades, where the the clients are allowed to connect to
	 *         <MixCascade>...<MixCascade>
	 *         ...
	 *       </AllowedMixCascades>
	 *     </AllowedMixCascadesSettings>
	 *   </ForwardingServer>
	 *   <ForwardingClient>                                      // since version 0.15
	 *     <ConnectViaForwarder>false</ConnectViaForwarder>      // whether a forwarder is needed to contact the mixcascades when enabling the anonymous mode
	 *     <ForwardInfoService>false</ForwardInfoService>        // whether an InfoService can be reached or also the InfoService needs forwarding
	 *   </ForwardingClient>
	 * </JapForwardingSettings>
	 *  </JAP>
	 *  @param a_strJapConfFile - file containing the Configuration. If null $(user.home)/jap.conf or ./jap.conf is used.
	 *  @param loadPay does this JAP support Payment ?
	 */
	public synchronized void loadConfigFile(String a_strJapConfFile, boolean loadPay,
											final ISplashResponse a_splash)
	{
		// @todo: remove since we already looked for the confing file in preLoadConfigFile
		boolean success = lookForConfigFile(a_strJapConfFile);
		
		if (a_strJapConfFile != null)
		{
			/* always try to use the config file specified on the command-line for storing the
			 * configuration
			 */
			JAPModel.getInstance().setConfigFile(a_strJapConfFile);
		}
		else
		{
			if (!success)
			{
				/* no config file was specified on the command line and the default config files don't
				 * exist -> store the configuration in the OS-specific directory
				 */
				JAPModel.getInstance().setConfigFile(AbstractOS.getInstance().getConfigPath() +
						JAPConstants.XMLCONFFN);
			}
		}
		Document doc = null;
		
		if (success)
		{
			try
			{
				doc = XMLUtil.readXMLDocument(new File(JAPModel.getInstance().getConfigFile()));
			}
			catch (Exception a_e)
			{
				LogHolder.log(LogLevel.NOTICE, LogType.MISC, "Error while loading the configuration file!");
			}
		}

		if (doc == null)
		{
			doc = XMLUtil.createDocument();
		}

		//if (success)
		{
			try
			{
				Element root = doc.getDocumentElement();
				XMLUtil.removeComments(root);

				//Load Locale-Settings
				String strLocale =
					XMLUtil.parseAttribute(root, JAPConstants.CONFIG_LOCALE, JAPMessages.getLocale().getLanguage());
				JAPMessages.init(new Locale(strLocale, ""), JAPConstants.MESSAGESFN);

				//
				setDefaultView(JAPConstants.VIEW_NORMAL);

				//Loading debug settings
				Element elemDebug = (Element) XMLUtil.getFirstChildByName(root, JAPConstants.CONFIG_DEBUG);
				if (elemDebug != null)
				{
					try
					{
						Element elemLevel = (Element) XMLUtil.getFirstChildByName(elemDebug,
							JAPConstants.CONFIG_LEVEL);
						JAPDebug.getInstance().setLogLevel(XMLUtil.parseValue(
							elemLevel, JAPDebug.getInstance().getLogLevel()));

						Element elemLogDetail = (Element) XMLUtil.getFirstChildByName(elemDebug,
							JAPConstants.CONFIG_LOG_DETAIL);
						LogHolder.setDetailLevel(
							XMLUtil.parseValue(elemLogDetail, LogHolder.getDetailLevel()));

						Element elemType = (Element) XMLUtil.getFirstChildByName(elemDebug,
							JAPConstants.CONFIG_TYPE);
						if (elemType != null)
						{
							int debugtype = LogType.NUL;
							int[] logTypes = LogType.getAvailableLogTypes();
							for (int j = 0; j < logTypes.length; j++)
							{
								if (XMLUtil.parseAttribute(elemType, LogType.getLogTypeName(logTypes[j]), true))
								{
									debugtype |= logTypes[j];
								}
							}
							JAPDebug.getInstance().setLogType(debugtype);
						}
						Node elemOutput = XMLUtil.getFirstChildByName(elemDebug, JAPConstants.CONFIG_OUTPUT);
						if (elemOutput != null)
						{
							String strConsole = XMLUtil.parseValue(elemOutput, "");
							if (strConsole != null && getView() != null)
							{
								strConsole.trim();
								JAPDebug.showConsole(strConsole.equalsIgnoreCase(JAPConstants.CONFIG_CONSOLE),
									getViewWindow());
							}
							Node elemFile = XMLUtil.getLastChildByName(elemOutput, JAPConstants.CONFIG_FILE);
							JAPDebug.setLogToFile(XMLUtil.parseValue(elemFile, null));
						}
					}
					catch (Exception ex)
					{
						LogHolder.log(LogLevel.INFO, LogType.MISC,
									  " Error loading Debug Settings.");
					}
				}


				String strVersion = XMLUtil.parseAttribute(root, JAPConstants.CONFIG_VERSION, null);
	            m_Model.setDLLupdate(XMLUtil.parseAttribute(root, m_Model.DLL_VERSION_UPDATE, false));




				JAPModel.getInstance().allowUpdateViaDirectConnection(
								XMLUtil.parseAttribute(root, XML_ALLOW_NON_ANONYMOUS_UPDATE,
					JAPConstants.DEFAULT_ALLOW_UPDATE_NON_ANONYMOUS_CONNECTION));
				JAPModel.getInstance().setReminderForOptionalUpdate(
								XMLUtil.parseAttribute(root, JAPModel.XML_REMIND_OPTIONAL_UPDATE,
					JAPConstants.REMIND_OPTIONAL_UPDATE));
				JAPModel.getInstance().setReminderForJavaUpdate(
								XMLUtil.parseAttribute(root, JAPModel.XML_REMIND_JAVA_UPDATE,
					JAPConstants.REMIND_JAVA_UPDATE));
				if (!m_bShowConfigAssistant)
				{
					// show the config assistant only if JAP forced this at the last start
					m_bShowConfigAssistant =
						XMLUtil.parseAttribute(root, XML_ATTR_SHOW_CONFIG_ASSISTANT, false);
				}
				AnonClient.setLoginTimeout(XMLUtil.parseAttribute(root, XML_ATTR_LOGIN_TIMEOUT,
					AnonClient.DEFAULT_LOGIN_TIMEOUT));
				InfoServiceDBEntry.setConnectionTimeout(XMLUtil.parseAttribute(root,
					XML_ATTR_INFOSERVICE_CONNECT_TIMEOUT,
					InfoServiceDBEntry.DEFAULT_GET_XML_CONNECTION_TIMEOUT));



				JAPModel.getInstance().setCascadeAutoSwitch(
								XMLUtil.parseAttribute(root, XML_ATTR_AUTO_CHOOSE_CASCADES, true));
				JAPModel.getInstance().setAutoChooseCascadeOnStartup(
								XMLUtil.parseAttribute(root, XML_ATTR_AUTO_CHOOSE_CASCADES_ON_STARTUP, true));
				JAPModel.getInstance().denyNonAnonymousSurfing(
								XMLUtil.parseAttribute(root, JAPModel.XML_DENY_NON_ANONYMOUS_SURFING, false));





	            m_Model.setHttpListenerPortNumber(XMLUtil.parseAttribute(root,
					JAPConstants.CONFIG_PORT_NUMBER,
					JAPModel.getHttpListenerPortNumber()));
				JAPModel.getInstance().setHttpListenerIsLocal(XMLUtil.parseAttribute(root,
					JAPConstants.CONFIG_LISTENER_IS_LOCAL, true));


				//port = XMLUtil.parseAttribute(root, "portNumberSocks",
				//  JAPModel.getSocksListenerPortNumber());
				//setSocksPortNumber(port);
				//setUseSocksPort(JAPUtil.parseNodeBoolean(n.getNamedItem("supportSocks"),false));
				//setUseProxy(JAPUtil.parseNodeBoolean(n.getNamedItem("proxyMode"),false));
				// load settings for the reminder message in setAnonMode
				try
				{
					mbActCntMessageNeverRemind = XMLUtil.parseAttribute(root,
						JAPConstants.CONFIG_NEVER_REMIND_ACTIVE_CONTENT, false);
					mbDoNotAbuseReminder =
						XMLUtil.parseAttribute(root, JAPConstants.CONFIG_DO_NOT_ABUSE_REMINDER, false);
					if (mbActCntMessageNeverRemind && mbDoNotAbuseReminder)
					{
						mbActCntMessageNotRemind = true;

					}
					// load settings for the reminder message before goodBye
					m_Model.setNeverRemindGoodbye(
						XMLUtil.parseAttribute(root, JAPConstants.CONFIG_NEVER_REMIND_GOODBYE,
											   !JAPConstants.DEFAULT_WARN_ON_CLOSE));
					m_bForwarderNotExplain =
						XMLUtil.parseAttribute(root, JAPConstants.CONFIG_NEVER_EXPLAIN_FORWARD, false);
					m_bPayCascadeNoAsk =
						XMLUtil.parseAttribute(root, JAPConstants.CONFIG_NEVER_ASK_PAYMENT, false);

				}
				catch (Exception ex)
				{
					LogHolder.log(LogLevel.INFO, LogType.MISC,
								  "Error loading reminder message ins setAnonMode.");
				}
				/* infoservice configuration options */
				boolean b = XMLUtil.parseAttribute(root, JAPConstants.CONFIG_INFOSERVICE_DISABLED,
					JAPModel.isInfoServiceDisabled());
				setInfoServiceDisabled(b);
				int i = XMLUtil.parseAttribute(root, JAPConstants.CONFIG_INFOSERVICE_TIMEOUT, 10);
				try
				{ //i = 5; /** @todo temp */
					if ( (i >= 1) && (i <= 60))
					{
						HTTPConnectionFactory.getInstance().setTimeout(i);
					}
				}
				catch (Exception e)
				{
					LogHolder.log(LogLevel.INFO, LogType.MISC, "Error loading InfoService timeout.");
				}

				// load settings for proxy
				ProxyInterface proxyInterface = null;

				try
				{
					String proxyType = XMLUtil.parseAttribute(root, JAPConstants.CONFIG_PROXY_TYPE,
															  ListenerInterface.PROTOCOL_STR_TYPE_HTTP);
					if (proxyType.equalsIgnoreCase("HTTP"))
					{
						proxyType = ListenerInterface.PROTOCOL_STR_TYPE_HTTP;
					}
					else if (proxyType.equalsIgnoreCase("SOCKS"))
					{
						proxyType = ListenerInterface.PROTOCOL_STR_TYPE_SOCKS;
					}
					JAPModel.getInstance().setUseProxyAuthentication(
					   XMLUtil.parseAttribute(root, JAPConstants.CONFIG_PROXY_AUTHORIZATION, false));
					proxyInterface = new ProxyInterface(
						XMLUtil.parseAttribute(root, JAPConstants.CONFIG_PROXY_HOST_NAME, null),
						XMLUtil.parseAttribute(root, JAPConstants.CONFIG_PROXY_PORT_NUMBER, -1),
						proxyType,
						XMLUtil.parseAttribute(root, JAPConstants.CONFIG_PROXY_AUTH_USER_ID, null),
						getPasswordReader(),
						JAPModel.getInstance().isProxyAuthenticationUsed(),
						XMLUtil.parseAttribute(root, JAPConstants.CONFIG_PROXY_MODE, false));
				}
				catch (Exception a_e)
				{
					LogHolder.log(LogLevel.NOTICE, LogType.NET, "Could not load proxy settings!", a_e);
				}

				// check if something has changed
				changeProxyInterface(proxyInterface,
									 XMLUtil.parseAttribute(
					root,JAPConstants.CONFIG_PROXY_AUTHORIZATION, false),
									 JAPController.getInstance().getViewWindow());

				setDummyTraffic(XMLUtil.parseAttribute(root, JAPConstants.CONFIG_DUMMY_TRAFFIC_INTERVALL,
					JAPConfAnonGeneral.DEFAULT_DUMMY_TRAFFIC_INTERVAL_SECONDS));
				if (strVersion == null || strVersion.compareTo("0.24") < 0)
				{
					JAPModel.getInstance().setAutoConnect(
									   XMLUtil.parseAttribute(root, "autoConnect", true));
					// if auto-connect is not chosen, ask the user what to do
					m_bAskAutoConnect =  !JAPModel.getInstance().isAutoConnect();
				}
				else
				{
					JAPModel.getInstance().setAutoConnect(
						XMLUtil.parseAttribute(root, JAPConstants.CONFIG_AUTO_CONNECT, true));
				}
				m_Model.setAutoReConnect(
								XMLUtil.parseAttribute(root, JAPConstants.CONFIG_AUTO_RECONNECT, true));;
				m_Model.setMinimizeOnStartup(
					XMLUtil.parseAttribute(root, JAPConstants.CONFIG_MINIMIZED_STARTUP, false));

				//Database.getInstance(MixCascade.class).update(getCurrentMixCascade());


				/* load the signature verification settings */
				try
				{
					Element signatureVerificationNode = (Element) (XMLUtil.getFirstChildByName(root,
						SignatureVerifier.getXmlSettingsRootNodeName()));
					if (signatureVerificationNode != null)
					{
						Hashtable blockedCerts = new Hashtable();
						blockedCerts.put(new Integer(JAPCertificate.CERTIFICATE_TYPE_PAYMENT),
										 new Integer(JAPCertificate.CERTIFICATE_TYPE_PAYMENT));
						SignatureVerifier.getInstance().loadSettingsFromXml(signatureVerificationNode,
							blockedCerts);
					}
					else
					{
						throw (new Exception("No SignatureVerification node found. Using default settings for signature verification."));
					}
				}
				catch (Exception e)
				{
					LogHolder.log(LogLevel.ERR, LogType.MISC, e);
				}

				/* load the list of blacklisted cascades */
				Database.getInstance(BlacklistedCascadeIDEntry.class).loadFromXml(
								(Element) XMLUtil.getFirstChildByName(root,
					BlacklistedCascadeIDEntry.XML_ELEMENT_CONTAINER_NAME));
				// do not blacklist the loaded cascades automatically
				boolean bAutoBlacklist = XMLUtil.parseAttribute(XMLUtil.getFirstChildByName(root,
					BlacklistedCascadeIDEntry.XML_ELEMENT_CONTAINER_NAME),
					BlacklistedCascadeIDEntry.XML_ATTR_AUTO_BLACKLIST_NEW_CASCADES,
					BlacklistedCascadeIDEntry.DEFAULT_AUTO_BLACKLIST);
				BlacklistedCascadeIDEntry.putNewCascadesInBlacklist(false);


				/* try to load information about cascades */
				Node nodeCascades = XMLUtil.getFirstChildByName(root, MixCascade.XML_ELEMENT_CONTAINER_NAME);
				MixCascade currentCascade;
				if (nodeCascades != null)
				{
					Node nodeCascade = nodeCascades.getFirstChild();
					while (nodeCascade != null)
					{
						if (nodeCascade.getNodeName().equals(MixCascade.XML_ELEMENT_NAME))
						{
							try
							{
								currentCascade = new MixCascade( (Element) nodeCascade, Long.MAX_VALUE);
								try
								{
									Database.getInstance(MixCascade.class).update(currentCascade);
								}
								catch (Exception e)
								{}
								/* register loaded cascades as known cascades */
								Database.getInstance(CascadeIDEntry.class).update(
									new CascadeIDEntry(currentCascade));
							}
							catch (Exception a_e)
							{
							}
						}
						nodeCascade = nodeCascade.getNextSibling();
					}
				}
				BlacklistedCascadeIDEntry.putNewCascadesInBlacklist(bAutoBlacklist);

				/* load the list of known cascades */
				Database.getInstance(CascadeIDEntry.class).loadFromXml(
								(Element) XMLUtil.getFirstChildByName(root,
					CascadeIDEntry.XML_ELEMENT_CONTAINER_NAME));

				/* for the blacklist: */
				Database.getInstance(PreviouslyKnownCascadeIDEntry.class).loadFromXml(
								(Element) XMLUtil.getFirstChildByName(root,
					PreviouslyKnownCascadeIDEntry.XML_ELEMENT_CONTAINER_NAME));


				/** @todo add Tor in a better way */
	/**			Database.getInstance(MixCascade.class).update(
								new MixCascade("Tor - Onion Routing", "Tor", "myhost.de", 1234));*/

				/* try to load information about user defined mixes */
				Node nodeMixes = XMLUtil.getFirstChildByName(root, MixInfo.XML_ELEMENT_CONTAINER_NAME);
				if (nodeMixes != null)
				{
					Node nodeMix = nodeMixes.getFirstChild();
					while (nodeMix != null)
					{
						if (nodeMix.getNodeName().equals(MixInfo.XML_ELEMENT_NAME))
						{
							try
							{
								Database.getInstance(MixInfo.class).update(
									new MixInfo((Element)nodeMix, Long.MAX_VALUE, false));
							}
							catch (Exception e)
							{
								try
								{
									Database.getInstance(MixInfo.class).update(
										new MixInfo((Element) nodeMix, Long.MAX_VALUE, true));
								}
								catch (Exception a_e)
								{
									LogHolder.log(LogLevel.ERR, LogType.MISC,
												  "Illegal MixInfo object in configuration.", a_e);
								}
							}
						}
						nodeMix = nodeMix.getNextSibling();
					}
				}
				
				/* load trust models */
				TrustModel.fromXmlElement(
								(Element)XMLUtil.getFirstChildByName(root,
					TrustModel.XML_ELEMENT_CONTAINER_NAME));
				
				// load the stored statusinfos
				Database.getInstance(StatusInfo.class).loadFromXml(
						(Element) XMLUtil.getFirstChildByName(root, StatusInfo.XML_ELEMENT_CONTAINER_NAME));
				
				// load stored performanceinfo
				Database.getInstance(PerformanceInfo.class).loadFromXml(
						(Element) XMLUtil.getFirstChildByName(root, PerformanceInfo.XML_ELEMENT_CONTAINER_NAME));
				
				// load deleted messages
				Database.getInstance(DeletedMessageIDDBEntry.class).loadFromXml(
								(Element) XMLUtil.getFirstChildByName(root,
					DeletedMessageIDDBEntry.XML_ELEMENT_CONTAINER_NAME));

				// load clicked messages
				Database.getInstance(ClickedMessageIDDBEntry.class).loadFromXml(
					(Element) XMLUtil.getFirstChildByName(root,
					ClickedMessageIDDBEntry.XML_ELEMENT_CONTAINER_NAME));



				if (!JAPModel.isSmallDisplay() && !JAPDialog.isConsoleOnly())
				{
					JAPModel.getInstance().updateSystemLookAndFeels();
				}
				// read the current L&F (old XML tag for compatibility reasons)
				String lf = XMLUtil.parseAttribute(root, JAPConstants.CONFIG_LOOK_AND_FEEL, null);
				if (lf == null)
				{
					Node elemLookAndFeels = XMLUtil.getFirstChildByName(root, XML_ELEM_LOOK_AND_FEELS);
					// read the current L&F (if old tag is not available)
					lf = XMLUtil.parseAttribute(
						elemLookAndFeels, XML_ATTR_LOOK_AND_FEEL, JAPConstants.CONFIG_UNKNOWN);

					if (elemLookAndFeels != null)
					{
						NodeList lnfs =
							( (Element) elemLookAndFeels).getElementsByTagName(XML_ELEM_LOOK_AND_FEEL);
						File currentFile;
						for (int j = 0; j < lnfs.getLength(); j++)
						{
							try
							{
								currentFile = new File(XMLUtil.parseValue(lnfs.item(j), null));
								//Load look-and-feel settings (not changed if SmmallDisplay!)
								try
								{
									if (JAPModel.isSmallDisplay() || JAPDialog.isConsoleOnly() ||
										GUIUtils.registerLookAndFeelClasses(currentFile).size() > 0)
									{
										// this is a valie L&F-file
										JAPModel.getInstance().addLookAndFeelFile(currentFile);
									}
								}
								catch (IllegalAccessException a_e)
								{
									// this is an old java version; do not drop the file reference
									JAPModel.getInstance().addLookAndFeelFile(currentFile);
								}
							}
							catch (Exception a_e)
							{
								LogHolder.log(
									LogLevel.ERR, LogType.MISC, "Error while parsing Look&Feels!");
								continue;
							}
						}
					}
				}

				if (!JAPModel.isSmallDisplay() && !JAPDialog.isConsoleOnly())
				{
					LookAndFeelInfo[] lfi = UIManager.getInstalledLookAndFeels();
					for (i = 0; i < lfi.length; i++)
					{
						if (lfi[i].getName().equals(lf) || lfi[i].getClassName().equals(lf))
						{
							try
							{
								UIManager.setLookAndFeel(lfi[i].getClassName());
							}
							catch (Throwable lfe)
							{
							try
								{
									UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
								}
								catch (UnsupportedLookAndFeelException ex)
								{
								}
								catch (IllegalAccessException ex)
								{
								}
								catch (InstantiationException ex)
								{
								}
								catch (ClassNotFoundException ex)
								{
								}
								LogHolder.log(LogLevel.WARNING, LogType.GUI,
											  "Exception while setting look-and-feel '" +
											  lfi[i].getClassName() + "'");
							}
							break ;
						}
					}
					JAPModel.getInstance().setLookAndFeel(UIManager.getLookAndFeel().getClass().getName());
				}


				//Loading GUI Setting
				Element elemGUI = (Element) XMLUtil.getFirstChildByName(root, JAPConstants.CONFIG_GUI);
				JAPModel.getInstance().setFontSize(XMLUtil.parseAttribute(
									   elemGUI, JAPModel.XML_FONT_SIZE, JAPModel.getInstance().getFontSize()));
				JAPDialog.setOptimizedFormat(XMLUtil.parseAttribute(
								elemGUI, JAPDialog.XML_ATTR_OPTIMIZED_FORMAT, JAPDialog.getOptimizedFormat()));
				/** @todo for backwards compatibility; remove */
				JAPModel.getInstance().setFontSize(XMLUtil.parseAttribute(
									   root, JAPModel.XML_FONT_SIZE, JAPModel.getInstance().getFontSize()));
				JAPDialog.setOptimizedFormat(XMLUtil.parseAttribute(
								root, JAPDialog.XML_ATTR_OPTIMIZED_FORMAT, JAPDialog.getOptimizedFormat()));
				Point location;
				Point defaultPoint = new Point(0,0);

				Node nodeWindow = XMLUtil.getFirstChildByName(elemGUI, JAPModel.XML_CONFIG_WINDOW);
				Node nodeSize = XMLUtil.getFirstChildByName(nodeWindow, JAPModel.XML_SIZE);
				if (!JAPDialog.isConsoleOnly())
				{
					Dimension defaultSize = new Dimension();
					Dimension size = parseWindowSize(nodeWindow, defaultSize,
						JAPConstants.DEFAULT_SAVE_CONFIG_WINDOW_SIZE, false);
					JAPModel.getInstance().setSaveConfigWindowSize(size != null);

					if (size == null)
					{
						size = parseWindowSize(nodeWindow, defaultSize,
											   JAPConstants.DEFAULT_SAVE_CONFIG_WINDOW_SIZE, true);
					}
					if (size != defaultSize)
					{
						JAPModel.getInstance().setConfigSize(size);
					}

				}
				location = parseWindowLocation(nodeWindow, defaultPoint,
											   JAPConstants.DEFAULT_SAVE_CONFIG_WINDOW_POSITION);
				JAPModel.getInstance().setSaveConfigWindowPosition(location != null);
				if (location != defaultPoint)
				{
					JAPModel.getInstance().setConfigWindowLocation(location);
				}

				nodeWindow = XMLUtil.getFirstChildByName(elemGUI, JAPModel.XML_ICONIFIED_WINDOW);
				JAPModel.getInstance().setMiniViewOnTop(
								XMLUtil.parseAttribute(nodeWindow, JAPModel.XML_ATTR_ICONIFIED_ON_TOP, true));
				nodeSize = XMLUtil.getFirstChildByName(nodeWindow, JAPModel.XML_SIZE);
				if (!JAPDialog.isConsoleOnly())
				{
					JAPModel.getInstance().setIconifiedSize(new Dimension(
						XMLUtil.parseAttribute(nodeSize, JAPModel.XML_ATTR_WIDTH, 0),
						XMLUtil.parseAttribute(nodeSize, JAPModel.XML_ATTR_HEIGHT, 0)));
				}
				location = parseWindowLocation(nodeWindow, defaultPoint,
											   JAPConstants.DEFAULT_SAVE_MINI_WINDOW_POSITION);
				JAPModel.getInstance().setSaveIconifiedWindowPosition(location != null);
				if (location != defaultPoint)
				{
					JAPModel.getInstance().setIconifiedWindowLocation(location);
				}

				nodeWindow = XMLUtil.getFirstChildByName(elemGUI, JAPModel.XML_HELP_WINDOW);
				location = parseWindowLocation(nodeWindow, defaultPoint,
											   JAPConstants.DEFAULT_SAVE_HELP_WINDOW_POSITION);
				JAPModel.getInstance().setSaveHelpWindowPosition(location != null);
				if (location != defaultPoint)
				{
					JAPModel.getInstance().setHelpWindowLocation(location);
				}
				if (!JAPDialog.isConsoleOnly())
				{
					Dimension defaultSize = new Dimension();
					Dimension size = parseWindowSize(nodeWindow, defaultSize,
						JAPConstants.DEFAULT_SAVE_HELP_WINDOW_SIZE, false);
					JAPModel.getInstance().setSaveHelpWindowSize(size != null);
					if (size != defaultSize)
					{
						JAPModel.getInstance().setHelpWindowSize(size);
					}
				}

				Element elemMainWindow = (Element) XMLUtil.getFirstChildByName(elemGUI,
					JAPConstants.CONFIG_MAIN_WINDOW);
				location = parseWindowLocation(elemMainWindow, defaultPoint,
											   JAPConstants.DEFAULT_SAVE_MAIN_WINDOW_POSITION);
				JAPModel.getInstance().setSaveMainWindowPosition(location != null);
				if (location != defaultPoint)
				{
					JAPModel.getInstance().setMainWindowLocation(location);
				}

				Element tmp = (Element) XMLUtil.getFirstChildByName(elemMainWindow,
					JAPConstants.CONFIG_MOVE_TO_SYSTRAY);
				b = XMLUtil.parseValue(tmp, false);
				setMoveToSystrayOnStartup(b);
				/*if (b)
				  { ///todo: move to systray
				 if (m_View != null)
				 {
				  b=m_View.hideWindowInTaskbar();
				 }
				  }
				  if(!b)
				  {
				 m_View.setVisible(true);
				   m_View.toFront();


				  }*/
				
				tmp = (Element) XMLUtil.getFirstChildByName(elemMainWindow, JAPConstants.CONFIG_START_PORTABLE_FIREFOX);
				JAPModel.getInstance().setStartPortableFirefox(XMLUtil.parseValue(tmp, true));
				
				tmp = (Element) XMLUtil.getFirstChildByName(elemMainWindow,
					JAPConstants.CONFIG_DEFAULT_VIEW);
				String strDefaultView = XMLUtil.parseValue(tmp, JAPConstants.CONFIG_NORMAL);
				if (strDefaultView.equals(JAPConstants.CONFIG_SIMPLIFIED))
				{
					setDefaultView(JAPConstants.VIEW_SIMPLIFIED);
				}

				/* load the infoservice management settings */
				try
				{
					Element infoserviceManagementNode = (Element) (XMLUtil.getFirstChildByName(root,
						InfoServiceHolder.getXmlSettingsRootNodeName()));
					JAPModel.getInstance().allowInfoServiceViaDirectConnection(
					   XMLUtil.parseAttribute(infoserviceManagementNode,
											  XML_ALLOW_NON_ANONYMOUS_CONNECTION,
											  JAPConstants.DEFAULT_ALLOW_INFOSERVICE_NON_ANONYMOUS_CONNECTION));
					if (infoserviceManagementNode != null)
					{
						InfoServiceHolder.getInstance().loadSettingsFromXml(
											  infoserviceManagementNode, JAPConstants.m_bReleasedVersion);
					}
					else
					{
						throw (new Exception("No InfoServiceManagement node found. Using default settings for infoservice management in InfoServiceHolder."));
					}
				}
				catch (Exception e)
				{
					LogHolder.log(LogLevel.ERR, LogType.MISC, e);
				}

				/* load Payment settings */
				try
				{
					if (loadPay)
					{
						Element elemPay = (Element) XMLUtil.getFirstChildByName(root,
							JAPConstants.CONFIG_PAYMENT);
						JAPModel.getInstance().allowPaymentViaDirectConnection(
											  XMLUtil.parseAttribute(elemPay, XML_ALLOW_NON_ANONYMOUS_CONNECTION,
							JAPConstants.DEFAULT_ALLOW_PAYMENT_NON_ANONYMOUS_CONNECTION));
						m_bAskSavePayment = XMLUtil.parseAttribute(elemPay, XML_ATTR_ASK_SAVE_PAYMENT, true);
						BIConnection.setConnectionTimeout(XMLUtil.parseAttribute(elemPay,
							BIConnection.XML_ATTR_CONNECTION_TIMEOUT,
							BIConnection.TIMEOUT_DEFAULT));


						Element elemAccounts = (Element) XMLUtil.getFirstChildByName(elemPay,
							PayAccountsFile.XML_ELEMENT_NAME);

						//Load known Payment instances
						Node nodePaymentInstances = XMLUtil.getFirstChildByName(elemPay,
							PaymentInstanceDBEntry.XML_ELEMENT_CONTAINER_NAME);
						PaymentInstanceDBEntry piEntry;
						if (nodePaymentInstances != null)
						{
							Node nodePI = nodePaymentInstances.getFirstChild();
							while (nodePI != null)
							{
								if (nodePI.getNodeName().equals(PaymentInstanceDBEntry.XML_ELEMENT_NAME))
								{
									try
									{
										piEntry = new PaymentInstanceDBEntry( (Element) nodePI, Long.MAX_VALUE);
										if (piEntry.isValid() && piEntry.isVerified())
										{
											Database.getInstance(PaymentInstanceDBEntry.class).update(piEntry);
										}
									}
									catch (Exception a_e)
									{
										LogHolder.log(LogLevel.ERR, LogType.MISC, a_e);
									}
								}
								nodePI = nodePI.getNextSibling();
							}
						}


						/** @todo implement password reader for console */
						IMiscPasswordReader passwordReader;
						final Hashtable cachedPasswords = new Hashtable();
						final Hashtable completedAccounts = new Hashtable();
						JAPDialog tempDialog = null;

						if (JAPDialog.isConsoleOnly())
						{
							passwordReader = new IMiscPasswordReader()
							{
								public String readPassword(Object a_message)
								{
									return null;
								}
							};
						}
						else
						{
							final JAPDialog.LinkedInformationAdapter onTopAdapter =
								new JAPDialog.LinkedInformationAdapter()
							{
								public boolean isOnTop()
								{
									return true;
								}
							};
							Component background;
							if (a_splash instanceof Component)
							{
								background = (Component)a_splash;
							}
							else
							{
								background = new Frame();
							}
							final JAPDialog dialog = new JAPDialog(background,
								"JAP: " + JAPMessages.getString(MSG_ACCPASSWORDENTERTITLE), true);
							dialog.setResizable(false);
							/** @todo does only work with java 1.5+ as the dll is not loaded at this time */
							dialog.setAlwaysOnTop(true);
							tempDialog = dialog;
							dialog.setDefaultCloseOperation(JAPDialog.HIDE_ON_CLOSE);
							PasswordContentPane temp = new PasswordContentPane(
								dialog, PasswordContentPane.PASSWORD_ENTER,
								JAPMessages.getString(
									MSG_ACCPASSWORDENTER, new Long(Long.MAX_VALUE)));
							temp.updateDialog();
							dialog.pack();

							passwordReader = new IMiscPasswordReader()
							{
								private Vector passwordsToTry = new Vector();

								public String readPassword(Object a_message)
								{
									PasswordContentPane panePassword;
									String password;
									panePassword = new PasswordContentPane(
										dialog, PasswordContentPane.PASSWORD_ENTER,
										JAPMessages.getString(MSG_ACCPASSWORDENTER, a_message));
									panePassword.setDefaultButtonOperation(PasswordContentPane.
																		   ON_CLICK_HIDE_DIALOG);
									if (passwordsToTry == null)
									{
										return null;
									}

									if (!completedAccounts.containsKey(a_message))
									{
										passwordsToTry.removeAllElements();
									}

									if (cachedPasswords.size() == 0 ||
										(completedAccounts.containsKey(a_message) &&
										((Boolean)completedAccounts.get(a_message)).booleanValue()))
									{
										while (true)
										{
											password = panePassword.readPassword(null);
											if (password == null)
											{
												if (JAPDialog.showYesNoDialog(
													(Component)a_splash,
													JAPMessages.getString(MSG_LOSEACCOUNTDATA),
													onTopAdapter))
												{
													// user clicked cancel
													passwordsToTry = null;
													// do not use the password from this account
													//cachedPasswords.remove(a_message);
													break;
												}
												else
												{
													continue;
												}
											}
											else
											{
												break;
											}
										}
										if (password != null)
										{
											cachedPasswords.put(password, password);
											completedAccounts.put(a_message, new Boolean(true));
										}
									}
									else
									{
										if (passwordsToTry.size() == 0)
										{
											Enumeration enumCachedPasswordKeys = cachedPasswords.elements();
											while (enumCachedPasswordKeys.hasMoreElements())
											{
												passwordsToTry.addElement(enumCachedPasswordKeys.
													nextElement());
											}
											// start using cached paasswords for this account
											completedAccounts.put(a_message, new Boolean(false));
										}
										password = (String) passwordsToTry.elementAt(passwordsToTry.size() -
											1);
										passwordsToTry.removeElementAt(passwordsToTry.size() - 1);

										if (passwordsToTry.size() == 0)
										{
											// all cached passwords have been used so far
											completedAccounts.put(a_message, new Boolean(true));
										}

									}
									return password;
								}
							};
						}
						PayAccountsFile.init(elemAccounts, passwordReader, JAPConstants.m_bReleasedVersion);
						if (tempDialog != null)
						{
							tempDialog.dispose();
						}
						if (cachedPasswords.size() > 0)
						{
							// choose any password from the working ones
							setPaymentPassword((String)cachedPasswords.elements().nextElement());
						}
					}
				}
				catch (Exception e)
				{
					LogHolder.log(LogLevel.ALERT, LogType.PAY, "Error loading Payment configuration.", e);
					if (JAPDialog.isConsoleOnly())
					{
						LogHolder.log(LogLevel.ALERT,  LogType.PAY, "Exiting...");
						System.exit(1);
					}
					else
					{
						if (JAPDialog.showConfirmDialog(new Frame(), JAPMessages.getString(MSG_PAYMENT_DAMAGED),
							JAPDialog.OPTION_TYPE_YES_NO, JAPDialog.MESSAGE_TYPE_ERROR,
							new JAPDialog.LinkedInformationAdapter()
						{
							public boolean isOnTop()
							{
								return true;
							}
						}) != JAPDialog.RETURN_VALUE_YES)
						{
							System.exit(1);
						}
					}
				}

				/*loading Tor settings*/
				try
				{
					Element elemTor = (Element) XMLUtil.getFirstChildByName(root, JAPConstants.CONFIG_TOR);
					if (JAPConstants.m_bReleasedVersion)
					{
						JAPModel.getInstance().setTorActivated(false);
					}
					else
					{
						JAPModel.getInstance().setTorActivated(
							XMLUtil.parseAttribute(elemTor, JAPModel.XML_ATTR_ACTIVATED, false));
					}
					Element elem = (Element) XMLUtil.getFirstChildByName(elemTor,
						JAPConstants.CONFIG_MAX_CONNECTIONS_PER_ROUTE);
					setTorMaxConnectionsPerRoute(XMLUtil.parseValue(elem,
						JAPModel.getTorMaxConnectionsPerRoute()));
					elem = (Element) XMLUtil.getFirstChildByName(elemTor, JAPConstants.CONFIG_ROUTE_LEN);
					int min, max;
					min = XMLUtil.parseAttribute(elem, JAPConstants.CONFIG_MIN,
												 JAPModel.getTorMinRouteLen());
					max = XMLUtil.parseAttribute(elem, JAPConstants.CONFIG_MAX,
												 JAPModel.getTorMaxRouteLen());
					setTorRouteLen(min, max);
					elem = (Element) XMLUtil.getFirstChildByName(elemTor,
						JAPConstants.CONFIG_TOR_PRECREATE_ANON_ROUTES);
					setPreCreateAnonRoutes(XMLUtil.parseValue(elem, JAPModel.isPreCreateAnonRoutesEnabled()));
					elem = (Element) XMLUtil.getFirstChildByName(elemTor,
						JAPConstants.CONFIG_TOR_DIR_SERVER);
					setTorUseNoneDefaultDirServer(XMLUtil.parseAttribute(elem,JAPConstants.CONFIG_XML_ATTR_TOR_NONE_DEFAULT_DIR_SERVER,
						JAPModel.isTorNoneDefaultDirServerEnabled()));
				}
				catch (Exception ex)
				{
					LogHolder.log(LogLevel.ERR, LogType.MISC,
								  "Error loading Tor configuration.", ex);
				}

				/*loading Mixminion settings*/
				try
				{
					Element elemMixminion = (Element) XMLUtil.getFirstChildByName(root,
						JAPConstants.CONFIG_Mixminion);
					if (JAPConstants.m_bReleasedVersion)
					{
						JAPModel.getInstance().setMixMinionActivated(false);
					}
					else
					{
						JAPModel.getInstance().setMixMinionActivated(
											  XMLUtil.parseAttribute(elemMixminion, JAPModel.XML_ATTR_ACTIVATED, false));
					}
					Element elemMM = (Element) XMLUtil.getFirstChildByName(elemMixminion,
						JAPConstants.CONFIG_ROUTE_LEN);
					int routeLen = XMLUtil.parseValue(elemMM, JAPModel.getMixminionRouteLen());
					JAPModel.getInstance().setMixminionRouteLen(routeLen);

					Element elemMMMail = (Element) XMLUtil.getFirstChildByName(elemMixminion,
							JAPConstants.CONFIG_MIXMINION_REPLY_MAIL);
					String emaddress = XMLUtil.parseAttribute(elemMMMail,"MixminionSender", "");
					if (strVersion == null || strVersion.compareTo(JAPConstants.CURRENT_CONFIG_VERSION) < 0)
					{
						/** @todo remove this fix for old config in a later version */
						if (emaddress.equals("none"))
						{
							emaddress = "";
						}
					}
					JAPModel.getInstance().setMixminionMyEMail(emaddress);

					Element elemMMPwHash = (Element) XMLUtil.getFirstChildByName(elemMixminion,
							JAPConstants.CONFIG_MIXMINION_PASSWORD_HASH);
					String pwhash = XMLUtil.parseValue(elemMMPwHash,null);
					if (pwhash != null)
					{
						JAPModel.getInstance().setMixinionPasswordHash(Base64.decode(pwhash));
					}
					Element elemMMKeyring = (Element) XMLUtil.getFirstChildByName(elemMixminion,
							JAPConstants.CONFIG_MIXMINION_KEYRING);
					String keyring = XMLUtil.parseValue(elemMMKeyring,"");
					JAPModel.getInstance().setMixminionKeyring(keyring);

				}
				catch (Exception ex)
				{
					LogHolder.log(LogLevel.ERR, LogType.MISC,
								  "Error loading Mixminion configuration.", ex);
				}

				/* read the settings of the JAP forwarding system */
				Element japForwardingSettingsNode = (Element) (XMLUtil.getFirstChildByName(root,
					JAPConstants.CONFIG_JAP_FORWARDING_SETTINGS));
				if (japForwardingSettingsNode != null)
				{
					JAPModel.getInstance().getRoutingSettings().loadSettingsFromXml(
						japForwardingSettingsNode);
				}
				else
				{
					LogHolder.log(LogLevel.ERR, LogType.MISC, "No JapForwardingSettings node found. Using default settings for forwarding.");
				}


				if (JAPModel.getInstance().isCascadeAutoSwitched() &&
					JAPModel.getInstance().isCascadeAutoChosenOnStartup())
				{
					// choose a random initial cascade
					AutoSwitchedMixCascadeContainer cascadeSwitcher =
						new AutoSwitchedMixCascadeContainer(true);
					setCurrentMixCascade(cascadeSwitcher.getNextMixCascade());
				}
				else
				{
					/* try to get the info from the MixCascade node */
					Element mixCascadeNode = (Element) XMLUtil.getFirstChildByName(root,
						MixCascade.XML_ELEMENT_NAME);
					try
					{
						m_currentMixCascade = new MixCascade( (Element) mixCascadeNode, Long.MAX_VALUE);
					}
					catch (Exception e)
					{
						m_currentMixCascade = new AutoSwitchedMixCascadeContainer().getNextMixCascade();
					}
				}
				/** make the default cascade known (so that it is not marked as new on first JAP start) */
				Database.getInstance(MixCascade.class).update(m_currentMixCascade);
				Database.getInstance(CascadeIDEntry.class).update(
								new CascadeIDEntry(m_currentMixCascade));
			}
			catch (Exception e)
			{
				LogHolder.log(LogLevel.INFO, LogType.MISC,
							  "JAPModel:Error loading configuration! " + e.toString());
			}
		} //end if f!=null
		// fire event
		notifyJAPObservers();
	}
	
	public Process getPortableFirefoxProcess() {
		return m_portableFirefoxProcess;
	}

	public void preLoadConfigFile(String a_strJapConfFile) 
	{
		if(lookForConfigFile(a_strJapConfFile))
		{
			try
			{
				BufferedReader br = new BufferedReader(new FileReader(m_Model.getConfigFile()));
				
				// skip the <?xml part
				br.readLine();
				Document doc = XMLUtil.toXMLDocument(br.readLine() + "</JAP>");
				
				m_Model.setShowSplashScreen(XMLUtil.parseAttribute(doc, XML_ATTR_SHOW_SPLASH_SCREEN, true));
			}
			catch(Exception ex)
			{
				LogHolder.log(LogLevel.EXCEPTION, LogType.MISC, "Unable to pre-load config file " + m_Model.getConfigFile() + ".");
			}
		}
	}

	public boolean lookForConfigFile(String a_strJapConfFile) {
		boolean success = false;
		if (a_strJapConfFile != null)
		{
			/* try the config file from the command line */
			success = this.loadConfigFileCommandLine(a_strJapConfFile);
		}
		if (!success)
		{
			/* no config file found -> try to use the config file in the OS-specific location */
			success = this.loadConfigFileOSdependent();
		}
		if (!success)
		{
			/* no config file found -> try to use the config file in the home directory of the user */
			success = this.loadConfigFileHome();
		}
		if (!success)
		{
			/* no config file found -> try to use the config file in the current directory */
			success = this.loadConfigFileCurrentDir();
		}
		if (!success)
		{
			/* no config file at any position->use OS-specific path for storing a new one*/
			m_Model.setConfigFile(AbstractOS.getInstance().getConfigPath() +
												 JAPConstants.XMLCONFFN);

			/* As this is the first JAp start, show the config assistant */
			m_bShowConfigAssistant = true;
		}
		return success;
	}

	public boolean startPortableFirefox(String[] cmds) 
	{
		if(m_portableFirefoxProcess != null)
		{
			boolean execSuccessful = false;
			try
			{
				int ffExitValue = m_portableFirefoxProcess.exitValue();
				LogHolder.log(LogLevel.INFO, LogType.MISC,
					"previous portable firefox process exited "+
					((ffExitValue == 0) ? "normally " : "anormally ")+
					"(exit value "+ffExitValue+").");
			}
			catch(IllegalThreadStateException itse)
			{
				LogHolder.log(LogLevel.WARNING, LogType.MISC,
				"Portable Firefox process is still running!");
				return false;
			}
		}
		
		try
		{
			m_portableFirefoxProcess = Runtime.getRuntime().exec(cmds);
			return true;
		} 
		catch (SecurityException se)
		{
			LogHolder.log(LogLevel.WARNING, LogType.MISC,
					"You are not allowed to lauch portable firefox: ", se);
			return false;
		}
		catch (IOException ioe3) 
		{
			LogHolder.log(LogLevel.WARNING, LogType.MISC,
			"Error occured while launching portable firefox with command "+cmds[0]+": ",ioe3);
			// open dialog and allow user to specify the firefox command
			if(m_View instanceof JAPNewView)
			{
				((JAPNewView) m_View).showChooseFirefoxPathDialog();
			}			
		}
		catch (NullPointerException npe) 
		{
			LogHolder.log(LogLevel.WARNING, LogType.MISC,
			"Launching portable firefox failed because the firefox command is null");
			// open dialog and allow user to specify the firefox command
			if(m_View instanceof JAPNewView)
			{
				((JAPNewView) m_View).showChooseFirefoxPathDialog();
			}			
		}
		catch (ArrayIndexOutOfBoundsException aioobe) 
		{
			LogHolder.log(LogLevel.WARNING, LogType.MISC,
			"Launching portable firefox failed because the firefox command array is empty");
		}

		return false;
	}
	
	/**
	 * Tries to load the config file provided in the command line
	 * @return FileInputStream
	 */
	private boolean loadConfigFileCommandLine(String a_configFile)
	{
		LogHolder.log(LogLevel.INFO, LogType.MISC,
					  "JAPController: loadConfigFile: Trying to load configuration from: " + a_configFile);
		try
		{
			FileInputStream f = new FileInputStream(a_configFile);
			/* if we are successful, use this config file also for storing the configuration */
			JAPModel.getInstance().setConfigFile(a_configFile);
			try
			{
				f.close();
			}
			catch (Exception e1)
			{}
			return true;
		}
		catch (Exception e)
		{
			LogHolder.log(LogLevel.ERR, LogType.MISC,
						  "Configuration file \"" + a_configFile +  "\" not found.");
			return false;
		}
	}

	private Dimension parseWindowSize(Node a_node, Dimension a_default, boolean a_bDefaultSave,
									  boolean bForceLoadingofSize)
	{
		Element tmp = (Element) XMLUtil.getFirstChildByName(a_node, JAPModel.XML_SIZE);
		boolean bSave;
		Dimension p = new Dimension();

		bSave = XMLUtil.parseAttribute(tmp, JAPModel.XML_ATTR_SAVE, a_bDefaultSave);
		p.width = XMLUtil.parseAttribute(tmp, JAPModel.XML_ATTR_WIDTH, 0);
		p.height = XMLUtil.parseAttribute(tmp, JAPModel.XML_ATTR_HEIGHT, 0);

		if (p.width <= 0 || p.height <= 0 || (!bSave && !bForceLoadingofSize))
		{
			if (bSave)
			{
				return a_default;
			}
			else
			{
				return null;
			}
		}

		return p;
	}


	private Point parseWindowLocation(Node a_node, Point a_default, boolean a_bSaveDefault)
	{
		Element tmp = (Element)XMLUtil.getFirstChildByName(a_node, JAPConstants.CONFIG_LOCATION);
		boolean bSave;

		bSave = XMLUtil.parseAttribute(tmp, JAPModel.XML_ATTR_SAVE, a_bSaveDefault);
		if (tmp == null || tmp.getAttribute(JAPConstants.CONFIG_X) == null ||
			tmp.getAttribute(JAPConstants.CONFIG_X).trim().length() == 0 ||
			tmp.getAttribute(JAPConstants.CONFIG_Y) == null  ||
			tmp.getAttribute(JAPConstants.CONFIG_Y).trim().length() == 0 || !bSave)
		{
			if (bSave)
			{
				return a_default;
			}
			else
			{
				return null;
			}
		}

		Point p = new Point();
		p.x = XMLUtil.parseAttribute(tmp, JAPConstants.CONFIG_X, 0);
		p.y = XMLUtil.parseAttribute(tmp, JAPConstants.CONFIG_Y, 0);
		return p;
	}

	/**
	 * Tries to load a config file in OS-depended locations
	 * @return boolean
	 */
	private boolean loadConfigFileOSdependent()
	{
		String japConfFile = AbstractOS.getInstance().getConfigPath() + JAPConstants.XMLCONFFN;
		LogHolder.log(LogLevel.INFO, LogType.MISC,
					  "Trying to load configuration from: " + japConfFile);
		try
		{
			FileInputStream f = new FileInputStream(japConfFile);
			/* if we are successful, use this config file also for storing the configuration */
			JAPModel.getInstance().setConfigFile(japConfFile);
			try
			{
				f.close();
			}
			catch (Exception e1)
			{}
			return true;
		}
		catch (Exception e)
		{
			LogHolder.log(LogLevel.ERR, LogType.MISC,
						  "JAPController: loadConfigFileOSdependent: Configuration file \"" + japConfFile +
						  "\" not found.");
			return false;
		}
	}

	/**
	 * Tries to load a config file from the user's home directory
	 * @return boolean
	 */
	private boolean loadConfigFileHome()
	{
		String japConfFile = System.getProperty("user.home", "") + File.separator + JAPConstants.XMLCONFFN;
		LogHolder.log(LogLevel.INFO, LogType.MISC,
					  "JAPController: loadConfigFile: Trying to load configuration from: " + japConfFile);
		try
		{
			FileInputStream f = new FileInputStream(japConfFile);
			/* if we are successful, use this config file also for storing the configuration */
			JAPModel.getInstance().setConfigFile(japConfFile);
			try
			{
				f.close();
			}
			catch (Exception e1)
			{}
			return true;
		}
		catch (Exception e)
		{
			LogHolder.log(LogLevel.ERR, LogType.MISC,
						  "JAPController: loadConfigFile: Configuration file \"" + japConfFile +
						  "\" not found.");
			return false;
		}

	}

	/**
	 * Tries to load a config file in the current directory
	 * @return boolean
	 */
	private boolean loadConfigFileCurrentDir()
	{
		String japConfFile = JAPConstants.XMLCONFFN;
		LogHolder.log(LogLevel.INFO, LogType.MISC,
					  "JAPController: loadConfigFile: Trying to load configuration from: " + japConfFile);
		try
		{
			FileInputStream f = new FileInputStream(japConfFile);
			/* if we are successful, use this config file also for storing the configuration */
			JAPModel.getInstance().setConfigFile(japConfFile);
			try
			{
				f.close();
			}
			catch (Exception e1)
			{}
			return true;
		}
		catch (Exception e)
		{
			LogHolder.log(LogLevel.ERR, LogType.MISC,
						  "JAPController: loadConfigFile: Configuration file \"" + japConfFile +
						  "\" not found.");
			return false;
		}

	}


	/**
	 * Tries to restart the JAP<br>
	 * 1.) Try to find "java.home" and start JAP with the java.exe in this path
	 * 2.) Try to find out if 'java' or 'jview' was used
	 */
	private void restartJAP()
	{
		// restart command
		MacOS macOS = (AbstractOS.getInstance() instanceof MacOS) ?
							(MacOS) AbstractOS.getInstance() : null;
		String strRestartCommand = "";
		String JapMainClass = (macOS != null ) ?
									"JAPMacintosh" : "JAP";
		
		//what is used: sun.java or JView?
		String strJavaVendor = System.getProperty("java.vendor");
		LogHolder.log(LogLevel.INFO, LogType.ALL, "Java vendor: " + strJavaVendor);

		String javaExe = null;
		String pathToJava = null;
		if (strJavaVendor.toLowerCase().indexOf("microsoft") != -1)
		{
			System.out.println("Java vendor :"+strJavaVendor.toLowerCase());
			pathToJava = System.getProperty("com.ms.sysdir") + File.separator;
			javaExe = "jview /cp";
		}
		else
		{
			pathToJava = System.getProperty("java.home") + File.separator + "bin" + File.separator;
			javaExe = "javaw -cp"; // for windows
		}
		strRestartCommand = pathToJava + javaExe + " \"" + CLASS_PATH + "\" " 
		+ JapMainClass + m_commandLineArgs;
		
		boolean isMacOSBundle = (macOS != null) ? macOS.isBundle() : false;
		
	    try
		{
	    	if(!isMacOSBundle)
	    	{
	    		Runtime.getRuntime().exec(strRestartCommand);	
	    	}
	    	else
	    	{
	    		String[] cmdArray = {"open", "-n", macOS.getBundlePath()};
	    		Runtime.getRuntime().exec(cmdArray);
	    	}
	    	LogHolder.log(LogLevel.INFO, LogType.ALL, "JAP restart command: " + strRestartCommand);	
		}
		catch (Exception ex)
		{
			javaExe = "java -cp"; // Linux/UNIX
			
			strRestartCommand = pathToJava + javaExe + " \"" + CLASS_PATH + "\" "+ JapMainClass + 
				m_commandLineArgs;

			LogHolder.log(LogLevel.INFO, LogType.ALL, "JAP restart command: " + strRestartCommand);
			try
			{
				Runtime.getRuntime().exec(strRestartCommand);
			}
			catch (Exception a_e)
			{
				LogHolder.log(LogLevel.EXCEPTION, LogType.ALL, "Error auto-restart JAP: " + ex);
			}
		}
    	
	}

	/**
	 * Changes the common proxy.
	 * @param a_proxyInterface a proxy interface
	 * @param a_bUseAuth indicates whether porxy authentication should be used
	 */
	public synchronized void changeProxyInterface(ProxyInterface a_proxyInterface, boolean a_bUseAuth,
		Component a_parent)
	{
		if (a_proxyInterface != null &&
			(m_Model.getProxyInterface() == null ||
			 !m_Model.getProxyInterface().equals(a_proxyInterface)))
		{
			// change settings
			m_Model.setProxyListener(a_proxyInterface);

			applyProxySettingsToInfoService(a_bUseAuth);
			applyProxySettingsToAnonService(a_parent);

			notifyJAPObservers();
		}
	}

	public boolean saveConfigFile()
	{
		boolean error = false;
		LogHolder.log(LogLevel.INFO, LogType.MISC, "Try saving configuration.");
		try
		{
			Document sb = getConfigurationAsXmlString();
			if (sb == null)
			{
				LogHolder.log(LogLevel.ERR, LogType.MISC,
							  "Could not transform the configuration to a string.");
				error = true;
			}
			else
			{
				/* JAPModel.getModel().getConfigFile() should always point to a valid configuration file */
				FileOutputStream f = new FileOutputStream(JAPModel.getInstance().getConfigFile());
				//XMLUtil.formatHumanReadable(doc);
				//return XMLUtil.toString(doc);
				XMLUtil.write(sb, f);
				//((XmlDocument)doc).write(f);

				//f.write(sb.getBytes());
				//f.flush();
				f.close();
			}
		}
		catch (Throwable e)
		{
			error = true;
		}
		return error;
	}

	private void addWindowLocationToConf(Element a_parentElement, Point a_location)
	{
		if (a_parentElement != null)
		{
			Element tmp =
				a_parentElement.getOwnerDocument().createElement(JAPConstants.CONFIG_LOCATION);

			a_parentElement.appendChild(tmp);
			XMLUtil.setAttribute(tmp, JAPModel.XML_ATTR_SAVE, a_location != null);

			if (a_location != null)
			{
				XMLUtil.setAttribute(tmp, JAPConstants.CONFIG_X, Integer.toString(a_location.x));
				XMLUtil.setAttribute(tmp, JAPConstants.CONFIG_Y, Integer.toString(a_location.y));
			}
		}
	}

	private void addWindowSizeToConf(Element a_parentElement, Dimension a_size, boolean a_bSaveSize)
	{
		if (a_parentElement != null)
		{
			Element tmp =
				a_parentElement.getOwnerDocument().createElement(JAPModel.XML_SIZE);

			a_parentElement.appendChild(tmp);
			XMLUtil.setAttribute(tmp, JAPModel.XML_ATTR_SAVE, a_size != null && a_bSaveSize);

			if (a_size != null)
			{
				XMLUtil.setAttribute(tmp, JAPModel.XML_ATTR_WIDTH, Integer.toString(a_size.width));
				XMLUtil.setAttribute(tmp, JAPModel.XML_ATTR_HEIGHT, Integer.toString(a_size.height));
			}
		}
	}


	private Document getConfigurationAsXmlString()
	{
		// Save config to xml file
		// Achtung!! Fehler im Sun-XML --> NULL-Attribute koennen hinzugefuegt werden,
		// beim Abspeichern gibt es dann aber einen Fehler!
		try
		{
			Document doc = XMLUtil.createDocument();
			Element e = doc.createElement("JAP");
			doc.appendChild(e);
			
			XMLUtil.setAttribute(e, JAPConstants.CONFIG_VERSION, JAPConstants.CURRENT_CONFIG_VERSION);
			XMLUtil.setAttribute(e, JAPModel.DLL_VERSION_UPDATE, m_Model.getDLLupdate());

			XMLUtil.setAttribute(e, XML_ALLOW_NON_ANONYMOUS_UPDATE,
								 JAPModel.getInstance().isUpdateViaDirectConnectionAllowed());
			XMLUtil.setAttribute(e, JAPModel.XML_REMIND_OPTIONAL_UPDATE,
								 JAPModel.getInstance().isReminderForOptionalUpdateActivated());
			XMLUtil.setAttribute(e, JAPModel.XML_REMIND_JAVA_UPDATE,
								 JAPModel.getInstance().isReminderForJavaUpdateActivated());
			XMLUtil.setAttribute(e, XML_ATTR_AUTO_CHOOSE_CASCADES,
								 JAPModel.getInstance().isCascadeAutoSwitched());
			XMLUtil.setAttribute(e, XML_ATTR_AUTO_CHOOSE_CASCADES_ON_STARTUP,
								 JAPModel.getInstance().isCascadeAutoChosenOnStartup());
			XMLUtil.setAttribute(e, JAPModel.XML_DENY_NON_ANONYMOUS_SURFING,
								 JAPModel.getInstance().isNonAnonymousSurfingDenied());
			XMLUtil.setAttribute(e, XML_ATTR_SHOW_CONFIG_ASSISTANT, m_bShowConfigAssistant);
			XMLUtil.setAttribute(e, XML_ATTR_SHOW_SPLASH_SCREEN, m_Model.getShowSplashScreen());

			XMLUtil.setAttribute(e, XML_ATTR_LOGIN_TIMEOUT, AnonClient.getLoginTimeout());
			XMLUtil.setAttribute(e, XML_ATTR_INFOSERVICE_CONNECT_TIMEOUT,
								 InfoServiceDBEntry.getConnectionTimeout());


			try
			{
				PayAccountsFile accounts = PayAccountsFile.getInstance();
				if (accounts != null)
				{
					Element elemPayment = doc.createElement(JAPConstants.CONFIG_PAYMENT);
					XMLUtil.setAttribute(elemPayment, XML_ALLOW_NON_ANONYMOUS_CONNECTION,
										 JAPModel.getInstance().isPaymentViaDirectConnectionAllowed());
					XMLUtil.setAttribute(elemPayment, BIConnection.XML_ATTR_CONNECTION_TIMEOUT,
										 BIConnection.getConnectionTimeout());
					XMLUtil.setAttribute(elemPayment, XML_ATTR_ASK_SAVE_PAYMENT, m_bAskSavePayment);
					e.appendChild(elemPayment);

					elemPayment.appendChild(Database.getInstance(PaymentInstanceDBEntry.class).toXmlElement(
									   doc, PaymentInstanceDBEntry.XML_ELEMENT_CONTAINER_NAME));

					elemPayment.appendChild(accounts.toXmlElement(doc, getPaymentPassword()));
				}
			}
			catch (Exception ex)
			{
				LogHolder.log(LogLevel.EXCEPTION, LogType.MISC, "Error saving payment configuration", ex);
				return null;
			}

			//
			XMLUtil.setAttribute(e, JAPConstants.CONFIG_PORT_NUMBER, JAPModel.getHttpListenerPortNumber());
			//XMLUtil.setAttribute(e,"portNumberSocks", Integer.toString(JAPModel.getSocksListenerPortNumber()));
			//XMLUtil.setAttribute(e,"supportSocks",(getUseSocksPort()?"true":"false"));
			XMLUtil.setAttribute(e, JAPConstants.CONFIG_LISTENER_IS_LOCAL, JAPModel.isHttpListenerLocal());
			ProxyInterface proxyInterface = m_Model.getProxyInterface();
			boolean bUseProxy = proxyInterface != null && proxyInterface.isValid();
			XMLUtil.setAttribute(e, JAPConstants.CONFIG_PROXY_MODE, bUseProxy);
			if (proxyInterface != null)
			{
				XMLUtil.setAttribute(e, JAPConstants.CONFIG_PROXY_TYPE,
									 m_Model.getProxyInterface().getProtocolAsString().toUpperCase());
				XMLUtil.setAttribute(e, JAPConstants.CONFIG_PROXY_HOST_NAME,
									 m_Model.getProxyInterface().getHost());
				XMLUtil.setAttribute(e, JAPConstants.CONFIG_PROXY_PORT_NUMBER,
									 m_Model.getProxyInterface().getPort());
				XMLUtil.setAttribute(e, JAPConstants.CONFIG_PROXY_AUTHORIZATION,
									 m_Model.getProxyInterface().isAuthenticationUsed());
				XMLUtil.setAttribute(e, JAPConstants.CONFIG_PROXY_AUTH_USER_ID,
									 m_Model.getProxyInterface().getAuthenticationUserID());
			}
			/* infoservice configuration options */
			XMLUtil.setAttribute(e, JAPConstants.CONFIG_INFOSERVICE_DISABLED, JAPModel.isInfoServiceDisabled());
			XMLUtil.setAttribute(e, JAPConstants.CONFIG_INFOSERVICE_TIMEOUT,
								 HTTPConnectionFactory.getInstance().getTimeout());

			XMLUtil.setAttribute(e, JAPConstants.CONFIG_DUMMY_TRAFFIC_INTERVALL,
								 JAPModel.getDummyTraffic());
			XMLUtil.setAttribute(e, JAPConstants.CONFIG_AUTO_CONNECT, JAPModel.isAutoConnect());
			XMLUtil.setAttribute(e, JAPConstants.CONFIG_AUTO_RECONNECT, JAPModel.isAutomaticallyReconnected());
			XMLUtil.setAttribute(e, JAPConstants.CONFIG_MINIMIZED_STARTUP, JAPModel.getMinimizeOnStartup());
			XMLUtil.setAttribute(e, JAPConstants.CONFIG_NEVER_REMIND_ACTIVE_CONTENT,
								 mbActCntMessageNeverRemind);
			XMLUtil.setAttribute(e, JAPConstants.CONFIG_NEVER_EXPLAIN_FORWARD, m_bForwarderNotExplain);
			XMLUtil.setAttribute(e, JAPConstants.CONFIG_NEVER_ASK_PAYMENT, m_bPayCascadeNoAsk);
			XMLUtil.setAttribute(e, JAPConstants.CONFIG_DO_NOT_ABUSE_REMINDER, mbDoNotAbuseReminder);
			XMLUtil.setAttribute(e, JAPConstants.CONFIG_NEVER_REMIND_GOODBYE,
								 JAPModel.getInstance().isNeverRemindGoodbye());

			XMLUtil.setAttribute(e, JAPConstants.CONFIG_LOCALE, JAPMessages.getLocale().getLanguage());
			Element elemLookAndFeels = doc.createElement(XML_ELEM_LOOK_AND_FEELS);
			XMLUtil.setAttribute(elemLookAndFeels, XML_ATTR_LOOK_AND_FEEL,
								 JAPModel.getInstance().getLookAndFeel());
			e.appendChild(elemLookAndFeels);
			Vector vecUIFiles = JAPModel.getInstance().getLookAndFeelFiles();
			Element elemLookAndFeel;
			for (int i = 0; i < vecUIFiles.size(); i++)
			{
				elemLookAndFeel = doc.createElement(XML_ELEM_LOOK_AND_FEEL);
				XMLUtil.setValue(elemLookAndFeel, ((File)vecUIFiles.elementAt(i)).getAbsolutePath());
				elemLookAndFeels.appendChild(elemLookAndFeel);
			}

			/* store trust models */
			e.appendChild(TrustModel.toXmlElement(doc, TrustModel.XML_ELEMENT_CONTAINER_NAME));

			/*stores MixCascades*/
			Element elemCascades = doc.createElement(MixCascade.XML_ELEMENT_CONTAINER_NAME);
			e.appendChild(elemCascades);
			Enumeration enumer = Database.getInstance(MixCascade.class).getEntrySnapshotAsEnumeration();
			while (enumer.hasMoreElements())
			{
				elemCascades.appendChild(((MixCascade) enumer.nextElement()).toXmlElement(doc));
			}

			/* stores known cascades */
			e.appendChild(Database.getInstance(CascadeIDEntry.class).toXmlElement(doc));
			
			// store status info
			e.appendChild(Database.getInstance(StatusInfo.class).toXmlElement(doc));
			
			// known for the blacklist
			e.appendChild(Database.getInstance(PreviouslyKnownCascadeIDEntry.class).toXmlElement(doc));

			/* stores blacklisted cascades */
			Element elemBlacklist = Database.getInstance(BlacklistedCascadeIDEntry.class).toXmlElement(doc);
			XMLUtil.setAttribute(elemBlacklist,
								 BlacklistedCascadeIDEntry.XML_ATTR_AUTO_BLACKLIST_NEW_CASCADES,
								 BlacklistedCascadeIDEntry.areNewCascadesInBlacklist());
			e.appendChild(elemBlacklist);



			/*stores mixes */
			Element elemMixes = doc.createElement(MixInfo.XML_ELEMENT_CONTAINER_NAME);
			e.appendChild(elemMixes);
			Enumeration enumerMixes = Database.getInstance(MixInfo.class).getEntrySnapshotAsEnumeration();
			while (enumerMixes.hasMoreElements())
			{
				Element element = ((MixInfo) enumerMixes.nextElement()).toXmlElement(doc);
				if (element != null) // do not write MixInfos of first mixes derived from cascade
				{
					elemMixes.appendChild(element);
				}
			}
			/* store the current MixCascade */
			MixCascade defaultMixCascade = getCurrentMixCascade();
			if (defaultMixCascade != null)
			{
				Element elem = defaultMixCascade.toXmlElement(doc);
				e.appendChild(elem);
			}

			// TODO: Is this really the best way?
			// store performanceinfo objects
			e.appendChild(Database.getInstance(PerformanceInfo.class).toXmlElement(doc));
			
			// store deleted messages
			e.appendChild(Database.getInstance(DeletedMessageIDDBEntry.class).toXmlElement(doc));

			// store clicked messages
			e.appendChild(Database.getInstance(ClickedMessageIDDBEntry.class).toXmlElement(doc));

			// adding GUI-Element
			Element elemGUI = doc.createElement(JAPConstants.CONFIG_GUI);
			e.appendChild(elemGUI);

			XMLUtil.setAttribute(elemGUI, JAPModel.XML_FONT_SIZE, JAPModel.getInstance().getFontSize());
			XMLUtil.setAttribute(elemGUI, JAPDialog.XML_ATTR_OPTIMIZED_FORMAT, JAPDialog.getOptimizedFormat());



			if (m_View instanceof AbstractJAPMainView)
			{
				((AbstractJAPMainView)m_View).saveWindowPositions();
			}

			Element elemWindow, elemSize;

			elemWindow = doc.createElement(JAPModel.XML_CONFIG_WINDOW);
			addWindowLocationToConf(elemWindow, JAPModel.getInstance().getConfigWindowLocation());
			addWindowSizeToConf(elemWindow, JAPModel.getInstance().getConfigSize(),
								JAPModel.getInstance().isConfigWindowSizeSaved());
			elemGUI.appendChild(elemWindow);


			elemWindow = doc.createElement(JAPModel.XML_ICONIFIED_WINDOW);
			XMLUtil.setAttribute(
					elemWindow, JAPModel.XML_ATTR_ICONIFIED_ON_TOP,
					JAPModel.getInstance().isMiniViewOnTop());
			if (JAPModel.getInstance().getIconifiedSize() != null)
			{
				elemSize = doc.createElement(JAPModel.XML_SIZE);
				XMLUtil.setAttribute(
					elemSize, JAPModel.XML_ATTR_WIDTH, JAPModel.getInstance().getIconifiedSize().width);
				XMLUtil.setAttribute(
					elemSize, JAPModel.XML_ATTR_HEIGHT, JAPModel.getInstance().getIconifiedSize().height);
				elemWindow.appendChild(elemSize);
			}
			addWindowLocationToConf(elemWindow, JAPModel.getInstance().getIconifiedWindowLocation());
			elemGUI.appendChild(elemWindow);

			elemWindow = doc.createElement(JAPModel.XML_HELP_WINDOW);
			addWindowLocationToConf(elemWindow, JAPModel.getInstance().getHelpWindowLocation());
			addWindowSizeToConf(elemWindow, JAPModel.getInstance().getHelpWindowSize(),
								JAPModel.getInstance().isHelpWindowSizeSaved());
			elemGUI.appendChild(elemWindow);


			Element elemMainWindow = doc.createElement(JAPConstants.CONFIG_MAIN_WINDOW);
			elemGUI.appendChild(elemMainWindow);
			addWindowLocationToConf(elemMainWindow, JAPModel.getInstance().getMainWindowLocation());

			if (JAPModel.getMoveToSystrayOnStartup())
			{
				Element tmp = doc.createElement(JAPConstants.CONFIG_MOVE_TO_SYSTRAY);
				XMLUtil.setValue(tmp, true);
				elemMainWindow.appendChild(tmp);
			}
			if(!JAPModel.getInstance().getStartPortableFirefox())
			{
				Element tmp = doc.createElement(JAPConstants.CONFIG_START_PORTABLE_FIREFOX);
				XMLUtil.setValue(tmp, false);
				elemMainWindow.appendChild(tmp);
			}
			if (JAPModel.getDefaultView() == JAPConstants.VIEW_SIMPLIFIED)
			{
				Element tmp = doc.createElement(JAPConstants.CONFIG_DEFAULT_VIEW);
				XMLUtil.setValue(tmp, JAPConstants.CONFIG_SIMPLIFIED);
				elemMainWindow.appendChild(tmp);
			}
			// adding Debug-Element
			Element elemDebug = doc.createElement(JAPConstants.CONFIG_DEBUG);
			e.appendChild(elemDebug);
			Element tmp = doc.createElement(JAPConstants.CONFIG_LEVEL);
			Text txt = doc.createTextNode(Integer.toString(JAPDebug.getInstance().getLogLevel()));
			tmp.appendChild(txt);
			elemDebug.appendChild(tmp);
			tmp = doc.createElement(JAPConstants.CONFIG_LOG_DETAIL);
			XMLUtil.setValue(tmp, LogHolder.getDetailLevel());
			elemDebug.appendChild(tmp);
			tmp = doc.createElement(JAPConstants.CONFIG_TYPE);
			int debugtype = JAPDebug.getInstance().getLogType();
			int[] availableLogTypes = LogType.getAvailableLogTypes();
			for (int i = 1; i < availableLogTypes.length; i++)
			{
				XMLUtil.setAttribute(tmp, LogType.getLogTypeName(availableLogTypes[i]),
									 ( (debugtype & availableLogTypes[i]) != 0));
			}

			elemDebug.appendChild(tmp);
			if (JAPDebug.isShowConsole() || JAPDebug.isLogToFile())
			{
				tmp = doc.createElement(JAPConstants.CONFIG_OUTPUT);
				elemDebug.appendChild(tmp);
				if (JAPDebug.isShowConsole())
				{
					XMLUtil.setValue(tmp, JAPConstants.CONFIG_CONSOLE);
				}
				if (JAPDebug.isLogToFile())
				{
					Element elemFile = doc.createElement(JAPConstants.CONFIG_FILE);
					tmp.appendChild(elemFile);
					XMLUtil.setValue(elemFile, JAPDebug.getLogFilename());
				}
			}

			/* adding signature verification settings */
			e.appendChild(SignatureVerifier.getInstance().toXmlElement(doc));

			/* adding infoservice settings */
			Element elemIS = InfoServiceHolder.getInstance().toXmlElement(doc);
			XMLUtil.setAttribute(elemIS, XML_ALLOW_NON_ANONYMOUS_CONNECTION,
								 JAPModel.getInstance().isInfoServiceViaDirectConnectionAllowed());
			e.appendChild(elemIS);


			/** add tor*/
			Element elemTor = doc.createElement(JAPConstants.CONFIG_TOR);
			XMLUtil.setAttribute(elemTor, JAPModel.XML_ATTR_ACTIVATED, JAPModel.getInstance().isTorActivated());
			Element elem = doc.createElement(JAPConstants.CONFIG_MAX_CONNECTIONS_PER_ROUTE);
			XMLUtil.setValue(elem, JAPModel.getTorMaxConnectionsPerRoute());
			elemTor.appendChild(elem);
			elem = doc.createElement(JAPConstants.CONFIG_ROUTE_LEN);
			XMLUtil.setAttribute(elem, JAPConstants.CONFIG_MIN, JAPModel.getTorMinRouteLen());
			XMLUtil.setAttribute(elem, JAPConstants.CONFIG_MAX, JAPModel.getTorMaxRouteLen());
			elemTor.appendChild(elem);
			elem = doc.createElement(JAPConstants.CONFIG_TOR_PRECREATE_ANON_ROUTES);
			XMLUtil.setValue(elem, JAPModel.isPreCreateAnonRoutesEnabled());
			elemTor.appendChild(elem);
			elem = doc.createElement(JAPConstants.CONFIG_TOR_DIR_SERVER);
			XMLUtil.setAttribute(elem,JAPConstants.CONFIG_XML_ATTR_TOR_NONE_DEFAULT_DIR_SERVER, JAPModel.isTorNoneDefaultDirServerEnabled());
			elemTor.appendChild(elem);

			e.appendChild(elemTor);

			/** add mixminion*/
			try{
			Element elemMixminion = doc.createElement(JAPConstants.CONFIG_Mixminion);
			XMLUtil.setAttribute(elemMixminion, JAPModel.XML_ATTR_ACTIVATED,
								 JAPModel.getInstance().isMixMinionActivated());
			Element elemMM = doc.createElement(JAPConstants.CONFIG_ROUTE_LEN);
			XMLUtil.setValue(elemMM, JAPModel.getMixminionRouteLen());

			Element elemMMMail = doc.createElement(JAPConstants.CONFIG_MIXMINION_REPLY_MAIL);
			XMLUtil.setAttribute(elemMMMail,"MixminionSender", JAPModel.getMixminionMyEMail());
			Element elemMMPwHash = doc.createElement(JAPConstants.CONFIG_MIXMINION_PASSWORD_HASH);
			XMLUtil.setValue(elemMMPwHash, Base64.encodeBytes(JAPModel.getMixMinionPasswordHash()));
			Element elemMMKeyring = doc.createElement(JAPConstants.CONFIG_MIXMINION_KEYRING);
			XMLUtil.setValue(elemMMKeyring, JAPModel.getMixminionKeyring());

			//end
			elemMixminion.appendChild(elemMM);
			elemMixminion.appendChild(elemMMMail);
			elemMixminion.appendChild(elemMMPwHash);
			elemMixminion.appendChild(elemMMKeyring);
			e.appendChild(elemMixminion);
			}
			catch(Exception em)
			{
				LogHolder.log(LogLevel.EXCEPTION, LogType.MISC,
							  "Error in savin Mixminion settings -- ignoring...", em);
			}
			e.appendChild(JAPModel.getInstance().getRoutingSettings().toXmlElement(doc));

			return doc;
		}
		catch (Throwable ex)
		{
			LogHolder.log(LogLevel.EXCEPTION, LogType.MISC, ex);
		}
		return null;
	}


	//---------------------------------------------------------------------
	public void setMinimizeOnStartup(boolean b)
	{
		synchronized (this)
		{
			m_Model.setMinimizeOnStartup(b);
		}
	}

	public void setMoveToSystrayOnStartup(boolean b)
	{
		synchronized (this)
		{
			m_Model.setMoveToSystrayOnStartup(b);
		}
	}

	public void setDefaultView(int defaultView)
	{
		synchronized (this)
		{
			m_Model.setDefaultView(defaultView);
		}
	}

	public MixCascade switchToNextMixCascade()
	{
		MixCascade cascade = new AutoSwitchedMixCascadeContainer(true).getNextMixCascade();
		setCurrentMixCascade(cascade);
		return cascade;
	}

	/**
	 * Changes the active MixCascade.
	 *
	 * @param newMixCascade The MixCascade which is activated.
	 */
	public void setCurrentMixCascade(MixCascade newMixCascade)
	{
		if (newMixCascade == null)
		{
			return;
		}

		if (!m_currentMixCascade.equals(newMixCascade))
		{
			synchronized (this)
			{
				/* we need consistent states */
				if ( (getAnonMode()) && (m_currentMixCascade != null))
				{
					/* we are running in anonymity mode */
					//setAnonMode(false);
					m_currentMixCascade = newMixCascade;
					connecting(m_currentMixCascade);
					LogHolder.log(LogLevel.DEBUG, LogType.MISC,
								  "MixCascade changed while in anonymity mode.");
					setAnonMode(true);
				}
				else
				{
					m_currentMixCascade = newMixCascade;
				}
			}
			notifyJAPObservers();
		}
		else
		{
			m_currentMixCascade = newMixCascade;
		}

	}

	/**
	 * Returns the active MixCascade.
	 *
	 * @return The active MixCascade.
	 */
	public MixCascade getCurrentMixCascade()
	{
		return m_currentMixCascade;
	}

	public void applyProxySettingsToInfoService(boolean a_bUseAuth)
	{
		if (m_Model.getProxyInterface() != null && m_Model.getProxyInterface().isValid())
		{
			HTTPConnectionFactory.getInstance().setNewProxySettings(m_Model.getProxyInterface(), a_bUseAuth);
		}
		else
		{
			//no Proxy should be used....
			HTTPConnectionFactory.getInstance().setNewProxySettings(null, false);
		}
	}

	private void applyProxySettingsToAnonService(Component a_parent)
	{
		if (JAPModel.getInstance().getProxyInterface() != null &&
			JAPModel.getInstance().getProxyInterface().isValid() && getAnonMode())
		{
			// anon service is running
			JAPDialog.Options options = new JAPDialog.Options(JAPDialog.OPTION_TYPE_YES_NO)
			{
				public String getYesOKText()
				{
					return JAPMessages.getString("reconnect");
				}

				public String getNoText()
				{
					return JAPMessages.getString("later");
				}
			};

			int ret = JAPDialog.showConfirmDialog(a_parent,
				JAPMessages.getString("reconnectAfterProxyChangeMsg"),
				JAPMessages.getString("reconnectAfterProxyChangeTitle"),
				options, JAPDialog.MESSAGE_TYPE_WARNING, null, null);
			if (ret == JAPDialog.RETURN_VALUE_YES)
			{
				// reconnect
				setAnonMode(false);
				setAnonMode(true);
			}
		}
	}

	public static String getFirewallAuthPasswd_()
	{
		/*
		   if (JAPModel.getUseFirewallAuthorization())
		   {
		 if (JAPModel.getFirewallAuthPasswd() == null)
		 {
		  m_Model.setFirewallAuthPasswd(JAPFirewallPasswdDlg.getPasswd());
		 }
		 return JAPModel.getFirewallAuthPasswd();
		   }
		   else
		   {
		 return null;
		   }*/
		return null;
	}

	public void setInfoServiceDisabled(boolean b)
	{
		m_Model.setInfoServiceDisabled(b);
		synchronized (this)
		{
			setChanged();
			notifyObservers(new JAPControllerMessage(JAPControllerMessage.INFOSERVICE_POLICY_CHANGED));
		}
	}

	public static void setPreCreateAnonRoutes(boolean b)
	{
		m_Model.setPreCreateAnonRoutes(b);
	}

	public static void setTorUseNoneDefaultDirServer(boolean b)
	{
		m_Model.setTorUseNoneDefaultDirServer(b);
	}

	//---------------------------------------------------------------------

	/* public void setSocksPortNumber(int p)
	 {
	   m_Model.setSocksListenerPortNumber(p);
	 }*/

	//---------------------------------------------------------------------
	//---------------------------------------------------------------------
	//---------------------------------------------------------------------
	private final class SetAnonModeAsync extends JobQueue.Job
	{

		private boolean m_startServer;

		public SetAnonModeAsync(boolean a_startServer)
		{
			super(!a_startServer);
			m_startServer = a_startServer;
		}

		public String getAddedJobLogMessage()
		{
			return "Added a job for changing the anonymity mode to '" +
				(new Boolean(isStartServerJob())).toString() + "' to the job queue.";
		}

		public boolean isStartServerJob()
		{
			return m_startServer;
		}

		public void runJob()
		{
			if (!Thread.currentThread().isInterrupted())
			{
				/* job was not canceled -> we have to do it */
				try
				{
					setServerMode(m_startServer);
				}
				catch (Throwable a_e)
				{
					LogHolder.log(LogLevel.EXCEPTION, LogType.NET,
								  "Error while setting server mode to " + m_startServer + "!", a_e);
				}

				LogHolder.log(LogLevel.DEBUG, LogType.MISC,
							  "Job for changing the anonymity mode to '" +
							  (new Boolean(m_startServer)).toString() + "' was executed.");
			}
		}

		/**
		 * @param anonModeSelected true, if anonymity should be started; false otherwise
		 * @param a_bRetryOnConnectionError if in case of a connection error it is retried to
		 * establish the connection
		 */
		private synchronized void setServerMode(boolean anonModeSelected)
		{
			int msgIdConnect = 0;
			if (!anonModeSelected)
			{
				// this is needed to interrupt the connection process
				if (m_proxyDirect == null)
				{
					msgIdConnect = m_View.addStatusMsg(JAPMessages.getString(
						"setAnonModeSplashDisconnect"),
						JAPDialog.MESSAGE_TYPE_INFORMATION, false);
				}
				try
				{
					m_proxyAnon.stop();
				}
				catch (NullPointerException a_e)
				{
					// ignore
				}
				if (msgIdConnect != 0)
				{
					m_View.removeStatusMsg(msgIdConnect);
				}
			}
			synchronized (PROXY_SYNC)
			{
				boolean canStartService = true;
				AutoSwitchedMixCascadeContainer cascadeContainer;

				//setAnonMode--> async!!
				LogHolder.log(LogLevel.DEBUG, LogType.MISC, "setAnonMode(" + anonModeSelected + ")");
				if (anonModeSelected &&
					!(m_proxyAnon != null && m_proxyAnon.getMixCascade().equals(getCurrentMixCascade())))
				{
					//start Anon Mode

					// true if the cascade is switched without starting the direct proxy
					boolean bSwitchCascade = (m_proxyAnon != null);
					int ret;

					if (getViewWindow() != null)
					{
						//getViewWindow().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
					}
					msgIdConnect = getView().addStatusMsg(JAPMessages.getString("setAnonModeSplashConnect"),
						JAPDialog.MESSAGE_TYPE_INFORMATION, false);

					// starting MUX --> Success ???
					boolean bForwardedConnection = false;
					if (JAPModel.getInstance().getRoutingSettings().getRoutingMode() ==
						JAPRoutingSettings.ROUTING_MODE_CLIENT)
					{
						/* we use a forwarded connection */
						bForwardedConnection = true;
						m_proxyAnon = JAPModel.getInstance().getRoutingSettings().getAnonProxyInstance(
											  m_socketHTTPListener);
					}
					else
					{
						/* we use a direct connection */
						if (!bSwitchCascade)
						{
							m_proxyAnon = new AnonProxy(
								m_socketHTTPListener, JAPModel.getInstance().getMutableProxyInterface(),
								JAPModel.getInstance().getPaymentProxyInterface());
						}
					}
					if (!JAPModel.isInfoServiceDisabled())
					{
						m_feedback.updateAsync();
					}
					m_proxyAnon.addEventListener(JAPController.getInstance());

					//m_proxyAnon.setMixCascade(new SimpleMixCascadeContainer(
					//			   m_Controller.getCurrentMixCascade()));
					cascadeContainer = new AutoSwitchedMixCascadeContainer();
					if (!bSwitchCascade)
					{
						if (JAPModel.getInstance().isTorActivated() && !bForwardedConnection &&
							!JAPConstants.m_bReleasedVersion)
						{
							TorAnonServerDescription td = new TorAnonServerDescription(!JAPModel.isTorNoneDefaultDirServerEnabled(),
								JAPModel.isPreCreateAnonRoutesEnabled());
							td.setTorDirServer("141.76.45.45",9030);
							td.setMaxRouteLen(JAPModel.getTorMaxRouteLen());
							td.setMinRouteLen(JAPModel.getTorMinRouteLen());
							td.setMaxConnectionsPerRoute(JAPModel.getTorMaxConnectionsPerRoute());
							m_proxyAnon.setTorParams(td);
						}
						else
						{
							m_proxyAnon.setTorParams(null);
						}
						if (JAPModel.getInstance().isMixMinionActivated() && !bForwardedConnection &&
							!JAPConstants.m_bReleasedVersion)
						{
							m_proxyAnon.setMixminionParams(new MixminionServiceDescription(JAPModel.
								getMixminionRouteLen(), JAPModel.getMixminionMyEMail()));
						}
						else
						{
							m_proxyAnon.setMixminionParams(null);
						}
						m_proxyAnon.setProxyListener(m_Controller);
						m_proxyAnon.setDummyTraffic(JAPModel.getDummyTraffic());

						// -> we can try to start anonymity
						if (m_proxyDirect != null)
						{
							m_proxyDirect.shutdown(true);
						}
						m_proxyDirect = null;

						LogHolder.log(LogLevel.DEBUG, LogType.NET, "Try to start AN.ON service...");
					}
					ret = m_proxyAnon.start(cascadeContainer);


					JAPDialog.LinkedInformationAdapter onTopAdapter =
						new JAPDialog.LinkedInformationAdapter()
					{
						public boolean isOnTop()
						{
							return true;
						}
					};


					if (ret == AnonProxy.E_BIND)
					{
						canStartService = false;
						m_proxyAnon.stop();
						m_proxyAnon = null;

						Object[] args =
							{
							new Integer(JAPModel.getHttpListenerPortNumber())};
						JAPDialog.showErrorDialog(
							getViewWindow(), JAPMessages.getString("errorListenerPort", args) +
							"<br><br>" +
							JAPMessages.getString(JAPConf.MSG_READ_PANEL_HELP, new Object[]
												  {
												  JAPMessages.getString("confButton"),
												  JAPMessages.getString("confListenerTab")})
							, LogType.NET, new JAPDialog.LinkedHelpContext("portlistener")
							  {
								  public boolean isOnTop()
								  {
									  return true;
								  }
							  });
						JAPController.getInstance().getView().disableSetAnonMode();
					}
					else if ((ret == AnonProxy.E_MIX_PROTOCOL_NOT_SUPPORTED ||
							 ret == AnonProxy.E_SIGNATURE_CHECK_FIRSTMIX_FAILED ||
							 ret == AnonProxy.E_SIGNATURE_CHECK_OTHERMIX_FAILED ||
							 ret == ErrorCodes.E_NOT_TRUSTED ||
							 ret == ErrorCodes.E_NOT_PARSABLE) &&
							!(cascadeContainer.isReconnectedAutomatically() &&
								cascadeContainer.isServiceAutoSwitched()))
					{
						String strMessage;

						canStartService = false;
						m_proxyAnon.stop();
						m_proxyAnon = null;

						if (ret == AnonProxy.E_MIX_PROTOCOL_NOT_SUPPORTED)
						{
							strMessage = JAPMessages.getString("errorMixProtocolNotSupported");
						}
						else if (ret == AnonProxy.E_SIGNATURE_CHECK_FIRSTMIX_FAILED)
						{
							strMessage =
								JAPMessages.getString("errorMixFirstMixSigCheckFailed");
						}
						else if (ret == AnonProxy.E_SIGNATURE_CHECK_OTHERMIX_FAILED)
						{
							strMessage = JAPMessages.getString("errorMixOtherMixSigCheckFailed");
						}
						else if (ret == ErrorCodes.E_NOT_TRUSTED)
						{
							strMessage =  JAPMessages.getString(MSG_CASCADE_NOT_TRUSTED);
						}
						else // if (ret == ErrorCodes.E_NOT_PARSABLE)
						{
							strMessage = JAPMessages.getString(MSG_CASCADE_NOT_PARSABLE);
						}
						strMessage += "<br><br>" + JAPMessages.getString(MSG_ASK_SWITCH);
						if (JAPDialog.showConfirmDialog(getViewWindow(),
							strMessage, JAPDialog.OPTION_TYPE_YES_NO,
							JAPDialog.MESSAGE_TYPE_ERROR,
							onTopAdapter) == JAPDialog.RETURN_VALUE_YES)
						{
							JAPModel.getInstance().setAutoReConnect(true);
							JAPModel.getInstance().setCascadeAutoSwitch(true);
						}
						else
						{
							getView().doClickOnCascadeChooser();
						}
					}
					else if (ret == ErrorCodes.E_SUCCESS ||
							 (ret != ErrorCodes.E_INTERRUPTED &&
							  cascadeContainer.isReconnectedAutomatically()))
					{
						final AnonProxy proxyAnon = m_proxyAnon;
						AnonServiceEventAdapter adapter = new AnonServiceEventAdapter()
						{
							boolean bWaitingForConnection = true;
							public synchronized void connectionEstablished(
								AnonServerDescription a_serverDescription)
							{
								if (bWaitingForConnection)
								{
									try
									{
										proxyAnon.addAIListener(JAPController.getInstance());
									}
									catch (Exception a_e)
									{
										// do nothing
									}
									JAPController.getInstance().removeEventListener(this);
									bWaitingForConnection = false;
								}
							}
						};

						if (ret == ErrorCodes.E_SUCCESS)
						{
							LogHolder.log(LogLevel.DEBUG, LogType.NET, "AN.ON service started successfully");
							adapter.connectionEstablished(proxyAnon.getMixCascade());

							if (!mbActCntMessageNotRemind && !JAPModel.isSmallDisplay() &&
								!m_bShowConfigAssistant && !getInstance().isPortableMode())
							{
								SwingUtilities.invokeLater(new Runnable()
								{
									public void run()
									{
										JAPDialog.LinkedCheckBox checkBox =
											new JAPDialog.LinkedCheckBox(false, "noactive");
										JAPDialog.showWarningDialog(getViewWindow(),
											JAPMessages.getString("disableActCntMessage"),
											JAPMessages.getString("disableActCntMessageTitle"),
											checkBox);
										// show a Reminder message that active contents should be disabled

										mbActCntMessageNeverRemind = checkBox.getState();
										mbDoNotAbuseReminder = checkBox.getState();
										if (mbActCntMessageNeverRemind)
										{
											mbActCntMessageNotRemind = true;
										}
									}
								});
							}
						}
						else
						{
							JAPController.getInstance().addEventListener(adapter);
							LogHolder.log(LogLevel.INFO, LogType.NET,
										  "AN.ON service not connected. Trying reconnect...");
						}

						// update feedback thread
						if (!JAPModel.isInfoServiceDisabled())
						{
							m_feedback.updateAsync();
						}
					}
					// ootte
					else
					{
						canStartService = false;
						m_proxyAnon.stop();
						m_proxyAnon = null;
						if (!JAPModel.isSmallDisplay() && ret != ErrorCodes.E_INTERRUPTED)
						{
							LogHolder.log(LogLevel.ERR, LogType.NET,
										  "Error starting AN.ON service! - ErrorCode: " +
										  Integer.toString(ret));
							if (JAPDialog.showConfirmDialog(getViewWindow(),
								JAPMessages.getString("errorConnectingFirstMix") + "<br><br>" +
								JAPMessages.getString(MSG_ASK_RECONNECT),
								JAPMessages.getString("errorConnectingFirstMixTitle"),
								JAPDialog.OPTION_TYPE_YES_NO,
								JAPDialog.MESSAGE_TYPE_ERROR, onTopAdapter)
								== JAPDialog.RETURN_VALUE_YES)
							{
								JAPModel.getInstance().setAutoReConnect(true);
							}
							//else
							{
								getView().doClickOnCascadeChooser();
							}
						}
					}
					if (getViewWindow() != null)
					{
						//getViewWindow().setCursor(Cursor.getDefaultCursor());
					}
					onTopAdapter = null;
					notifyJAPObservers();
					//splash.abort();
					m_View.removeStatusMsg(msgIdConnect);
					if (!canStartService)
					{
						// test if cascade has been switched during the error
						if (ret != ErrorCodes.E_INTERRUPTED ||
							(ret == ErrorCodes.E_INTERRUPTED && cascadeContainer.getInitialCascade().equals(
								JAPController.getInstance().getCurrentMixCascade())))
						{
							setAnonMode(false);
						}
					}

					if (canStartService && !JAPModel.isInfoServiceDisabled())
					{
						MixCascade cascade = null;
						try
						{
							cascade = m_proxyAnon.getMixCascade();
						}
						catch (NullPointerException a_e)
						{
						}
						if (!JAPModel.getInstance().isInfoServiceDisabled() &&
							cascade != null && !cascade.isUserDefined())
						{
							if (cascade.isFromCascade() ||
								Database.getInstance(MixCascade.class).getEntryById(cascade.getId()) == null)
							{
								// We have received a hint that this cascade has changed!
								Database.getInstance(MixCascade.class).update(
									InfoServiceHolder.getInstance().getMixCascadeInfo(cascade.getId()));
							}
						}
					}
				}
				else if ( (m_proxyDirect == null) && (!anonModeSelected))
				{
					AnonProxy proxyAnon = m_proxyAnon;
					if (proxyAnon != null)
					{
						msgIdConnect = m_View.addStatusMsg(JAPMessages.getString(
											  "setAnonModeSplashDisconnect"),
							JAPDialog.MESSAGE_TYPE_INFORMATION, false);
						proxyAnon.stop();
						m_View.removeStatusMsg(msgIdConnect);
					}

					synchronized (m_finishSync)
					{
						m_proxyAnon = null;
						m_finishSync.notifyAll();
					}

					if (!isShuttingDown()) // do not restart proxy during shutdown
					{
						m_proxyDirect = new DirectProxy(m_socketHTTPListener);
						// ignore all connections to terminate all remaining connections from JAP threads
						m_proxyDirect.setAllowUnprotectedConnectionCallback(null);
						m_proxyDirect.startService();
						try
						{
							Thread.sleep(300);
						}
						catch (InterruptedException ex)
						{
							// ignore
						}
						// reactivate the callback (now all remaining JAP connections should be dead)
						m_proxyDirect.setAllowUnprotectedConnectionCallback(m_proxyCallback);
					}
					/* notify the forwarding system after! m_proxyAnon is set to null */
					JAPModel.getInstance().getRoutingSettings().anonConnectionClosed();

					notifyJAPObservers();
				}
			}
		}

	} //end of class SetAnonModeAsync

	//---------------------------------------------------------------------
	//---------------------------------------------------------------------
	//---------------------------------------------------------------------
	public boolean getAnonMode()
	{
		return m_proxyAnon != null;
	}

	/**
	 * Indicactes if the config assistant should be shown on JAP start.
	 * @return if the config assistant should be shown on JAP start
	 */
	public boolean isConfigAssistantShown()
	{
		return m_bShowConfigAssistant;
	}

	public void setConfigAssistantShown()
	{
		m_bShowConfigAssistant = false;
	}

	public boolean isAnonConnected()
	{
		// don't think this needs to be synhronized; would cause deadlocks if it was...
		AnonProxy proxy = m_proxyAnon;
		if (proxy == null)
		{
			return false;
		}
		MixCascade currentCascade = getCurrentMixCascade();
		MixCascade proxyCascade = proxy.getMixCascade();

		return proxy != null && proxy.isConnected() && currentCascade != null && proxyCascade != null &&
			proxyCascade.equals(currentCascade);

	}

	public void stopAnonModeWait()
	{
		synchronized (m_Controller.m_finishSync)
		{
			while (m_Controller.getAnonMode() || m_Controller.isAnonConnected())
			{
				m_Controller.setAnonMode(false);
				LogHolder.log(LogLevel.NOTICE, LogType.THREAD, "Waiting for finish of AN.ON connection...");
				try
				{
					m_Controller.m_finishSync.wait(500);
				}
				catch (InterruptedException a_e)
				{
				}
			}
		}
}

	public void setAnonMode(final boolean a_anonModeSelected)
	{
		if (!m_bShutdown || !a_anonModeSelected)
		{
			m_anonJobQueue.addJob(new SetAnonModeAsync(a_anonModeSelected));
		}
	}

	/**
	 * This will do all necessary things in order to enable the anonymous mode. The method decides
	 * whether to establish the connection via a forwarder or direct to the selected anonymity
	 * service.
	 * Attention: Maybe it is necessary to show a dialog in order to get the information about a
	 *            forwarder. Thus only the Java-AWT event dispatch thread should call this method.
	 *            Any other caller will produce a freeze, if the connect-to-forwarder dialog
	 *            appears.
	 *
	 * @param a_parentComponent The parent component over which the connect to forwarder dialog (if
	 *                          necessary) is centered.
	 */
	public void startAnonymousMode(Component a_parentComponent)
	{
		/* decide whether to establish a forwarded connection or not */
		if (JAPModel.getInstance().getRoutingSettings().isConnectViaForwarder())
		{
			/* show the connect via forwarder dialog -> the dialog will do the remaining things */
			new JAPRoutingEstablishForwardedConnectionDialog(a_parentComponent);
			/* maybe connection to forwarder failed -> notify the observers, because the view maybe
			 * still shows the anonymity mode enabled
			 */
			notifyJAPObservers();
		}
		else
		{
			/* simply enable the anonymous mode */
			//if (m_currentMixCascade.isCertified())
			{
				setAnonMode(true);
			}
			//else
			{
				/** @todo ask if user wants to connect nevertheless!! */
			}
		}
	}

	public void setDummyTraffic(int msIntervall)
	{
		m_Model.setDummyTraffic(msIntervall);
		ForwardServerManager.getInstance().setDummyTrafficInterval(msIntervall);
		if (m_proxyAnon != null)
		{
			m_proxyAnon.setDummyTraffic(msIntervall);
		}
	}

	public static void setTorMaxConnectionsPerRoute(int i)
	{
		m_Model.setTorMaxConnectionsPerRoute(i);
	}

	public static void setTorRouteLen(int min, int max)
	{
		m_Model.setTorMaxRouteLen(max);
		m_Model.setTorMinRouteLen(min);
	}
	public static void setMixminionPassword(String p)
	{
		m_Model.setMixMinionPassword(p);
	}
	public static void setMixminionPasswordHash(byte[] h)
	{
		m_Model.setMixinionPasswordHash(h);
	}
	public static void resetMixminionPassword()
	{
		m_Model.resetMixMinionKeyringandPw();
	}
	public static void setMixminionKeyring(String kr)
	{
		m_Model.setMixminionKeyring(kr);
	}
	public static void setMixminionMessages(Vector m)
	{
		m_Model.setMixminionMessages(m);
	}
	public static void setMixminionMMRList(MMRList m)
	{
		m_Model.setMixminionMMRList(m);
	}
	public static void setMixminionFragments(Vector f)
	{
		m_Model.setMixminionFragments(f);
	}


	//---------------------------------------------------------------------
	private ServerSocket intern_startListener(int port, String host)
	{
		LogHolder.log(LogLevel.DEBUG, LogType.MISC, "JAPModel:startListener on port: " + port);
		ServerSocket s = null;
		for (int i = 0; i < 10; i++) //HAck for Mac!!
		{
			try
			{
				if (host == null && JAPModel.isHttpListenerLocal())
				{
					host = "127.0.0.1";
				}
				if (host != null)
				{
					//InetAddress[] a=InetAddress.getAllByName("localhost");
					InetAddress[] a = InetAddress.getAllByName(host);
					LogHolder.log(LogLevel.NOTICE, LogType.NET, "Try binding Listener on host: " + a[0]);
					s = new ServerSocket(port, 50, a[0]);
				}
				else
				{
					s = new ServerSocket(port);
				}
				LogHolder.log(LogLevel.NOTICE, LogType.NET, "Started listener on port " + port + ".");
				/*
				try
				{
					s.setSoTimeout(2000);
				}
				catch (Exception e1)
				{
					LogHolder.log(LogLevel.DEBUG, LogType.NET,
								  "Could not set listener accept timeout: Exception: " +
								  e1.getMessage());
				}*/
				break ;
			}
			catch (Exception e)
			{
				LogHolder.log(LogLevel.NOTICE, LogType.NET, e);
				s = null;
			}
		}
		return s;
	}

	public boolean startHTTPListener(String a_listenerHost, int a_listenerPort)
	{
		if (!isRunningHTTPListener)
		{
			LogHolder.log(LogLevel.DEBUG, LogType.NET, "Start HTTP Listener");
			m_socketHTTPListener = intern_startListener(
						 a_listenerPort <= 0 ? JAPModel.getHttpListenerPortNumber() : a_listenerPort,
						 a_listenerHost);
			isRunningHTTPListener = true;
		}
		return m_socketHTTPListener != null;
	}

	public void showInstallationAssistant()
	{
		if (m_bAssistantClicked)
		{
			return;
		}
		m_bAssistantClicked = true;

		final JAPDialog configAssistant = new ConfigAssistant(getViewWindow());

		configAssistant.addWindowListener(new WindowAdapter()
		{
			public void windowClosed(WindowEvent a_event)
			{
				configAssistant.removeWindowListener(this);
				//configAssistant.removeComponentListener(componentAdapter);
				m_bAssistantClicked = false;
				getViewWindow().setVisible(true);
			}
		});

		configAssistant.setVisible(true);
	}

/*
	private void stopHTTPListener()
	{
		LogHolder.log(LogLevel.DEBUG, LogType.MISC, "JAPModel:stopListener");
		if (isRunningHTTPListener)
		{
			setAnonMode(false);
			try
			{
				m_socketHTTPListener.close();
			}
			catch (Exception e)
			{}
			;
			m_socketHTTPListener = null;
			isRunningHTTPListener = false;
		}
	}*/

	/* private boolean startSOCKSListener()
	 {
	   LogHolder.log(LogLevel.DEBUG, LogType.MISC, "JAPModel:start SOCKS Listener");
	   if (isRunningSOCKSListener == false)
	   {
	  m_socketSOCKSListener = intern_startListener(JAPModel.getSocksListenerPortNumber(),
	 JAPModel.getHttpListenerIsLocal());
	  if (m_socketSOCKSListener != null)
	  {
	 isRunningSOCKSListener = true;
	  }
	   }
	   return isRunningSOCKSListener;
	 }
	 */
	//---------------------------------------------------------------------

	/** This (and only this) is the final exit procedure of JAP!
	 * It shows a reminder to reset the proxy configurations and saves the current configuration.
	 *	@param bDoNotRestart false if JAP should be restarted; true otherwise
	 */
	public static void goodBye(final boolean bDoNotRestart)
	{
		Thread stopThread = new Thread(new Runnable()
		{
			public void run()
			{
				int returnValue;
				JAPDialog.LinkedCheckBox checkBox;
				if (!JAPModel.getInstance().isNeverRemindGoodbye() && bDoNotRestart &&
					!getInstance().isPortableMode())
				{
					// show a Reminder message that active contents should be disabled
					checkBox = new JAPDialog.LinkedCheckBox(false)
					{
						public boolean isOnTop()
						{
							return true;
						}
					};
					returnValue = JAPDialog.showConfirmDialog(getInstance().getViewWindow(),
						JAPMessages.getString(MSG_DISABLE_GOODBYE),
						JAPDialog.OPTION_TYPE_OK_CANCEL, JAPDialog.MESSAGE_TYPE_INFORMATION, checkBox);
					if (returnValue == JAPDialog.RETURN_VALUE_OK)
					{
						JAPModel.getInstance().setNeverRemindGoodbye(checkBox.getState());
					}
				}
				else
				{
					returnValue = JAPDialog.RETURN_VALUE_OK;
				}

				if (returnValue == JAPDialog.RETURN_VALUE_OK &&
					getInstance().getViewWindow() != null && getInstance().m_bAskSavePayment && bDoNotRestart)
				{
					// we are in GUI mode
					Enumeration enumAccounts = PayAccountsFile.getInstance().getAccounts();
					PayAccount account;
					while (enumAccounts.hasMoreElements())
					{
						account = (PayAccount) enumAccounts.nextElement();
						if (!account.isBackupDone())
						{
							JAPDialog.LinkedCheckBox checkbox =
								new JAPDialog.LinkedCheckBox(false, "payment_account")
							{
								public boolean isOnTop()
								{
									return true;
								}
							};

							if (!JAPDialog.showYesNoDialog(getInstance().getViewWindow(),
								JAPMessages.getString(MSG_ACCOUNT_NOT_SAVED), checkbox))
							{
								// skip closing JAP
								getInstance().setAskSavePayment(!checkbox.getState());
								final PayAccount tempAcount = account;
								new Thread(new Runnable()
								{
									public void run()
									{
										getInstance().getView().showConfigDialog(
											JAPConf.PAYMENT_TAB, new Long(tempAcount.getAccountNumber()));
									}
								}).start();

								return;
							}
							getInstance().setAskSavePayment(!checkbox.getState());
							break;
						}
					}
				}


				if (returnValue == JAPDialog.RETURN_VALUE_OK || JAPDialog.isConsoleOnly())
				{
					if (getInstance().getViewWindow() != null)
					{
						getInstance().getViewWindow().setEnabled(false);
						JAPViewIconified viewiconified=getInstance().m_View.getViewIconified();
						if(viewiconified!=null)
							viewiconified.setEnabled(false);
					}

					getInstance().m_finishSplash.setText(JAPMessages.getString(MSG_SAVING_CONFIG));
					if (getInstance().m_finishSplash instanceof JAPSplash)
					{
						if (getInstance().getViewWindow() instanceof AbstractJAPMainView &&
							getInstance().getViewWindow().isVisible())
						{
							GUIUtils.centerOnWindow( (JAPSplash) getInstance().m_finishSplash,
								(AbstractJAPMainView) m_Controller.m_View);
						}
						else
						{
							( (JAPSplash) getInstance().m_finishSplash).centerOnScreen();
						}
						((JAPSplash)getInstance().m_finishSplash).setVisible(true);
					}

					Window parent = getInstance().getViewWindow();
					if (getInstance().m_finishSplash instanceof JAPSplash)
					{
						parent = (JAPSplash) getInstance().m_finishSplash;
					}

					Vector exitListeners = (Vector)getInstance().m_programExitListeners.clone();
					for (int i = 0; i < exitListeners.size(); i++)
					{
						((ProgramExitListener)exitListeners.elementAt(i)).programExiting();
					}

					boolean error = m_Controller.saveConfigFile();
					if (error && bDoNotRestart)
					{
						JAPDialog.showErrorDialog(parent, JAPMessages.getString(MSG_ERROR_SAVING_CONFIG,
							JAPModel.getInstance().getConfigFile() ), LogType.MISC);
					}

					// disallow new connections
					JAPModel.getInstance().setAutoReConnect(false);
					JAPModel.getInstance().setCascadeAutoSwitch(false);

					getInstance().m_finishSplash.setText(JAPMessages.getString(MSG_CLOSING_DIALOGS));
					JAPDialog.setConsoleOnly(true); // do not show any dialogs now

					if (!bDoNotRestart)
					{
						GUIUtils.setLoadImages(false);
					}
					m_Controller.m_bShutdown = true;
					// disallow InfoService traffic
					JAPModel.getInstance().setInfoServiceDisabled(true);
					Thread finishIS = new Thread(new Runnable()
					{
						public void run()
						{
							LogHolder.log(LogLevel.NOTICE, LogType.THREAD,
										  "Stopping InfoService auto-update threads...");
							getInstance().m_finishSplash.setText(
								JAPMessages.getString(MSG_FINISHING_IS_UPDATES));
							m_Controller.m_feedback.stop();
							m_Controller.m_AccountUpdater.stop();
							m_Controller.m_MixCascadeUpdater.stop();
							m_Controller.m_InfoServiceUpdater.stop();
							m_Controller.m_paymentInstanceUpdater.stop();
							m_Controller.m_minVersionUpdater.stop();
							m_Controller.m_javaVersionUpdater.stop();
							m_Controller.m_messageUpdater.stop();
							m_Controller.m_perfInfoUpdater.stop();
						}
					}, "Finish IS threads");
					finishIS.start();

					// do not show direct connection warning dialog in this state; ignore all direct conns
					m_Controller.m_proxyCallback = null;
					DirectProxy.setAllowUnprotectedConnectionCallback(null);

					Thread finishAnon = new Thread(new Runnable()
					{
						public void run()
						{
							try
							{
								getInstance().m_finishSplash.setText(
									JAPMessages.getString(MSG_FINISHING_ANON));
								m_Controller.setAnonMode(false);
								// Wait until anon mode is disabled");
								m_Controller.stopAnonModeWait();

								//Wait until all Jobs are finished....
								LogHolder.log(LogLevel.NOTICE, LogType.THREAD, "Finishing all AN.ON jobs...");
								m_Controller.m_anonJobQueue.stop();
								m_Controller.queueFetchAccountInfo.stop();
							}
							catch (Throwable a_e)
							{
								LogHolder.log(LogLevel.EMERG, LogType.MISC, a_e);
							}
						}
					}, "Finish anon thread");
					finishAnon.start();

					if (JAPModel.getInstance().getRoutingSettings().getRoutingMode() ==
						JAPRoutingSettings.ROUTING_MODE_SERVER)
					{
						getInstance().m_finishSplash.setText(
						  JAPMessages.getString(MSG_FINISH_FORWARDING_SERVER));
						getInstance().enableForwardingServer(false);
					}


					while (finishIS.isAlive() || finishAnon.isAlive())
					{
						try
						{
							if (finishIS.isAlive())
							{
								getInstance().m_finishSplash.setText(JAPMessages.getString(MSG_WAITING_IS));
							}
							if (finishAnon.isAlive())
							{
								getInstance().m_finishSplash.setText(JAPMessages.getString(MSG_WAITING_ANON));
							}
							finishIS.join();
							finishAnon.join();
						}
						catch (InterruptedException ex)
						{
						}
					}

					try
					{
						LogHolder.log(LogLevel.NOTICE, LogType.THREAD, "Shutting down direct proxy...");
						getInstance().m_finishSplash.setText(JAPMessages.getString(MSG_STOPPING_PROXY));
						DirectProxy proxy = m_Controller.m_proxyDirect;
						if (proxy != null)
						{
							proxy.shutdown(true);
						}
						LogHolder.log(LogLevel.NOTICE, LogType.THREAD, "Shutting down direct proxy - Done!");
					}
					catch (Exception a_e)
					{
						LogHolder.log(LogLevel.DEBUG, LogType.THREAD, "Shutting down direct proxy - exception",a_e);
						// ignore
					}
					try
					{
						getInstance().m_finishSplash.setText(JAPMessages.getString(MSG_STOPPING_LISTENER));
						m_Controller.m_socketHTTPListener.close();
					}
					catch (Exception a_e)
					{
					}

					getInstance().m_finishSplash.setText(JAPMessages.getString(MSG_FINISHING));
					LogHolder.log(LogLevel.NOTICE, LogType.NET,
								  "Interrupting all network communication threads...");
					System.getProperties().put( "socksProxyPort", "0");
					System.getProperties().put( "socksProxyHost" ,"localhost");
					// do not show any dialogs in this state
					getInstance().switchViewWindow(true);
					if (getInstance().getViewWindow() != null)
					{
						getInstance().getViewWindow().dispose();
					}
					if (getInstance().m_finishSplash instanceof JAPSplash)
					{
						((JAPSplash)m_Controller.m_finishSplash).dispose();
					}
					LogHolder.log(LogLevel.INFO, LogType.GUI, "View has been disposed. Finishing...");
					if ( !bDoNotRestart ) {
						getInstance().m_finishSplash.setText(JAPMessages.getString(MSG_RESTARTING));
						LogHolder.log(LogLevel.INFO, LogType.ALL, "Try to restart JAP...");
						m_Controller.restartJAP();
					}
					System.exit(0);
				}
			}
		});
		if (!JAPDialog.isConsoleOnly() && SwingUtilities.isEventDispatchThread())
		{
			stopThread.start();
		}
		else
		{
			stopThread.run();
		}
	}

	/** Shows the About dialog
	 */
	public static void aboutJAP()
	{
		try
		{
			if (getInstance().m_bPresentationMode)
			{
				new JAPAbout(getInstance().getViewWindow());
			}
			else
			{
				new JAPAboutNew(getInstance().getViewWindow()).setVisible(true);
			}
		}
		catch (Throwable t)
		{
			LogHolder.log(LogLevel.EXCEPTION, LogType.GUI, t);
		}
	}

	/**
	 * Updates the list of known InfoServices.
	 * @param a_bDoOnlyIfNotYetUpdated only updates the infoservices if not at least one successful
	 * update has been done yet
	 * @return true if the update was successful; false otherwise
	 */
	public boolean updateInfoServices(boolean a_bDoOnlyIfNotYetUpdated)
	{
		if (a_bDoOnlyIfNotYetUpdated && m_InfoServiceUpdater.isFirstUpdateDone())
		{
			return true;
		}
		return m_InfoServiceUpdater.update();
	}

	/**
	 * Get all available mixcascades from the infoservice and store it in the database.
	 * @param bShowError should an Error Message be displayed if something goes wrong ?
	 * @param a_bDoOnlyIfNotYetUpdated only updates the cascades if not at least one successful update
	 * has been done yet
	 */
	public void fetchMixCascades(boolean bShowError, Component a_view, boolean a_bDoOnlyIfNotYetUpdated)
	{
		if (a_bDoOnlyIfNotYetUpdated && m_MixCascadeUpdater.isFirstUpdateDone())
		{
			return;
		}

		LogHolder.log(LogLevel.INFO, LogType.MISC, "Trying to fetch mixcascades from infoservice.");
		while (!m_MixCascadeUpdater.update())
		{
			LogHolder.log(LogLevel.ERR, LogType.NET, "No connection to infoservices.");
			if (!JAPModel.isSmallDisplay() &&
				(bShowError || Database.getInstance(MixCascade.class).getNumberOfEntries() == 0))
			{
				if (!JAPModel.getInstance().isInfoServiceViaDirectConnectionAllowed() && !isAnonConnected())
				{
					int returnValue =
						JAPDialog.showConfirmDialog(a_view, JAPMessages.getString(MSG_IS_NOT_ALLOWED),
						JAPDialog.OPTION_TYPE_YES_NO, JAPDialog.MESSAGE_TYPE_ERROR);
					if (returnValue == JAPDialog.RETURN_VALUE_YES)
					{
						JAPModel.getInstance().allowInfoServiceViaDirectConnection(true);
						updateInfoServices(false);
						continue;
					}
				}
				else
				{
					JAPDialog.showErrorDialog(a_view, JAPMessages.getString("errorConnectingInfoService"),
											  LogType.NET);
				}
			}
			break;
		}
	}

	/**
	 * Performs the Versioncheck.
	 *
	 * @return  0, if the local JAP version is up to date.
	 *         -1, if version check says that anonymity mode should not be enabled. Reasons can be:
	 *             new version found, version check failed.
	 *          1, if no version check could be done
	 */
	private int versionCheck(String a_minVersion, boolean a_bForced)
	{
		String versionType;
		boolean recommendToSwitchToRelease = false;
		if (a_bForced)
		{
			versionType = "mandatory";
		}
		else
		{
			versionType = "optional";
		}

		LogHolder.log(LogLevel.NOTICE, LogType.MISC,
					  "Checking if new " + versionType + " version of JAP is available...");

		JAPVersionInfo vi = null;
		JAPVersionInfo viRelease = null;
		String updateVersionNumber = null;


		Database.getInstance(JAPVersionInfo.class).update(
			  InfoServiceHolder.getInstance().getJAPVersionInfo(JAPVersionInfo.JAP_RELEASE_VERSION));
		Database.getInstance(JAPVersionInfo.class).update(
			  InfoServiceHolder.getInstance().getJAPVersionInfo(JAPVersionInfo.JAP_DEVELOPMENT_VERSION));

		vi = (JAPVersionInfo)Database.getInstance(JAPVersionInfo.class).getEntryById(
			  JAPVersionInfo.ID_RELEASE);
		if (!JAPConstants.m_bReleasedVersion)
		{
			viRelease = vi;
			vi = (JAPVersionInfo) Database.getInstance(JAPVersionInfo.class).getEntryById(
				JAPVersionInfo.ID_DEVELOPMENT);
		}

		if (vi == null)
		{
			LogHolder.log(LogLevel.ERR, LogType.MISC,
						  "Could not get the current JAP version from infoservice.");
			return 1;
		}
		if (viRelease != null && viRelease.getJapVersion() != null && vi.getJapVersion() != null &&
			//Util.convertVersionStringToNumber(viRelease.getJapVersion()) + 2 >= //patch
			//Util.convertVersionStringToNumber(vi.getJapVersion()) &&  // patch
			//viRelease.getJapVersion().compareTo(JAPConstants.aktVersion) > 0) // patch
			viRelease.getJapVersion().equals(vi.getJapVersion()))
		{
			// developer and release version are equal; recommend to switch to release
			recommendToSwitchToRelease = true;
		}

		/*
		if (a_bForced)
		{
			updateVersionNumber = a_minVersion;
		}*/

		if (updateVersionNumber == null)
		{
			updateVersionNumber = vi.getJapVersion();
		}

		if (updateVersionNumber == null)
		{
			/* can't get the current version number from the infoservices. Ignore this,
			 * as this is not a problem!
			 */
			LogHolder.log(LogLevel.ERR, LogType.MISC,
						  "Could not get the current JAP version number from infoservice.");
			/*
				JAPDialog.showErrorDialog(m_View, JAPMessages.getString("errorConnectingInfoService"),
					LogType.NET);*/
			//notifyJAPObservers();
			return 1;
		}

		if (!a_bForced && !recommendToSwitchToRelease)
		{
			if (updateVersionNumber.compareTo(JAPConstants.aktVersion) <= 0 || isConfigAssistantShown()
				|| !JAPModel.getInstance().isReminderForOptionalUpdateActivated())
			{
				// no update needed; do not show dialog
				return 0;
			}
		}

		updateVersionNumber = updateVersionNumber.trim();
		LogHolder.log(LogLevel.DEBUG, LogType.MISC, "Local version: " + JAPConstants.aktVersion);
		if (updateVersionNumber.compareTo(JAPConstants.aktVersion) <= 0)
		{
			/* the local JAP version is up to date -> exit */
			return 0;
		}
		/* local version is not up to date, new version is available -> ask the user whether to
		 * download the new version or not
		 */
		String message;
		JAPDialog.ILinkedInformation checkbox = null;
		String dev = ")";
		if (!JAPConstants.m_bReleasedVersion && !recommendToSwitchToRelease)
		{
			dev = "-dev)";
		}
		if (a_bForced)
		{
			message = JAPMessages.getString("newVersionAvailable", updateVersionNumber + dev);
			checkbox = new JAPDialog.LinkedInformationAdapter()
			{
				public boolean isOnTop()
				{
					return true;
				}
			};
		}
		else
		{
			message = JAPMessages.getString(MSG_NEW_OPTIONAL_VERSION, updateVersionNumber + dev);
			checkbox = new JAPDialog.LinkedCheckBox(false);
		}
		JAPDialog.Options options;
		if (recommendToSwitchToRelease)
		{
			message += "<br><br>" + JAPMessages.getString(MSG_ASK_WHICH_VERSION);
			options = new JAPDialog.Options(JAPDialog.OPTION_TYPE_YES_NO_CANCEL)
			{
				public String getYesOKText()
				{
					return JAPMessages.getString(MSG_VERSION_DEVELOPER);
				}

				public String getNoText()
				{
					return JAPMessages.getString(MSG_VERSION_RELEASE);
				}
			};
		}
		else
		{
			options = new JAPDialog.Options(JAPDialog.OPTION_TYPE_OK_CANCEL)
			{
				public String getYesOKText()
				{
					return JAPMessages.getString(DialogContentPane.MSG_YES);
				}

				public String getCancelText()
				{
					return JAPMessages.getString(DialogContentPane.MSG_NO);
				}
			};
		}
		int bAnswer = JAPDialog.showConfirmDialog(getViewWindow(), message,
												  JAPMessages.getString("newVersionAvailableTitle"),
												  options, JAPDialog.MESSAGE_TYPE_QUESTION, checkbox);
		if (checkbox instanceof JAPDialog.LinkedCheckBox)
		{
			JAPModel.getInstance().setReminderForOptionalUpdate(
						 !((JAPDialog.LinkedCheckBox)checkbox).getState());
		}
		if (bAnswer == JAPDialog.RETURN_VALUE_YES || bAnswer == JAPDialog.RETURN_VALUE_NO)
		{
			if (bAnswer == JAPDialog.RETURN_VALUE_NO)
			{
				vi = viRelease;
			}
			/* User has selected to download new version of JAP -> Download, Alert, exit program */
			//store current configuration first
			saveConfigFile();
			JAPUpdateWizard wz = new JAPUpdateWizard(vi, getViewWindow());
			/* we got the JAPVersionInfo from the infoservice */
			/* Assumption: If we are here, the download failed for some resaons, otherwise the
			 * program would quit
			 */
			//TODO: Do this in a better way!!
			if (wz.getStatus() == JAPUpdateWizard.UPDATESTATUS_ERROR)
			{
				/* Download failed -> alert, and reset anon mode to false */
				LogHolder.log(LogLevel.ERR, LogType.MISC, "Some update problem.");
				JAPDialog.showErrorDialog(getViewWindow(),
										  JAPMessages.getString("downloadFailed") +
										  JAPMessages.getString("infoURL"), LogType.MISC);
				if (a_bForced)
				{
					notifyJAPObservers();
				}
				/* update failed -> exit */
				return -1;
			}
			/* should never be reached, because if update was successful, the JAPUpdateWizard closes
			 * JAP
			 */
			return 0;
		}
		else
		{
			/* User has selected not to download -> Alert, we should'nt start the system due to
			 * possible compatibility problems
			 */
			if (a_bForced)
			{

				JAPDialog.showWarningDialog(getViewWindow(), JAPMessages.getString("youShouldUpdate",
					JAPMessages.getString(JAPNewView.MSG_UPDATE)),
											JAPUtil.createDialogBrowserLink(JAPMessages.getString("infoURL")));
				//notifyJAPObservers();
				return -1;
			}
			return 0;
		}

		/* this line should never be reached */
	}

	//---------------------------------------------------------------------
	public void setView(IJAPMainView v, boolean a_bAllowSplash)
	{
		synchronized (SYNC_VIEW)
		{
			m_View = v;
			if (m_View instanceof Frame && a_bAllowSplash)
			{
				m_finishSplash = new JAPSplash((Frame)m_View, JAPMessages.getString(MSG_FINISHING));
			}
			else
			{
				m_finishSplash = new ConsoleSplash();
			}
		}
	}

	public void switchViewWindow(boolean a_bMainView)
	{
		synchronized (SYNC_VIEW)
		{
			m_bMainView = a_bMainView;
		}
	}

	public Window getViewWindow()
	{
		synchronized (SYNC_VIEW)
		{
			if (m_View instanceof Window)
			{
				if (m_bMainView)
				{
					return (Window) m_View;
				}
				else
				{
					return m_View.getViewIconified();
				}
			}
			return null;
		}
	}

	public IJAPMainView getView()
	{
		return m_View;
	}

	public void removeEventListener(AnonServiceEventListener a_listener)
	{
		m_anonServiceListener.removeElement(a_listener);
	}

	public void addEventListener(AnonServiceEventListener a_listener)
	{
		synchronized (m_anonServiceListener)
		{
			Enumeration e = m_anonServiceListener.elements();
			while (e.hasMoreElements())
			{
				if (a_listener.equals(e.nextElement()))
				{
					return;
				}
			}
			m_anonServiceListener.addElement(a_listener);
		}
	}



	//---------------------------------------------------------------------
	public void addJAPObserver(JAPObserver o)
	{
		observerVector.addElement(o);
	}

	public void notifyJAPObservers()
	{
		LogHolder.log(LogLevel.DEBUG, LogType.MISC, "JAPModel:notifyJAPObservers()");
		synchronized (observerVector)
		{
			try
			{
				Enumeration enumer = observerVector.elements();
				int i = 0;
				while (enumer.hasMoreElements())
				{
					JAPObserver listener = (JAPObserver) enumer.nextElement();
					LogHolder.log(LogLevel.DEBUG, LogType.MISC, "JAPModel:notifyJAPObservers: " + i);
					listener.updateValues(false);
					i++;
				}
			}
			catch (Throwable t)
			{
				LogHolder.log(LogLevel.EMERG, LogType.MISC,
							  "JAPModel:notifyJAPObservers - critical exception: " + t.getMessage());
			}
		}
		LogHolder.log(LogLevel.DEBUG, LogType.MISC, "JAPModel:notifyJAPObservers()-ended");
	}

	//---------------------------------------------------------------------
	public synchronized void channelsChanged(int channels)
	{
		//nrOfChannels = channels;
		Enumeration enumer = observerVector.elements();
		while (enumer.hasMoreElements())
		{
			JAPObserver listener = (JAPObserver) enumer.nextElement();
			listener.channelsChanged(channels);
		}
	}

	public synchronized void transferedBytes(long bytes, int protocolType)
	{
		long b;
		if (protocolType == IProxyListener.PROTOCOL_WWW)
		{
			m_nrOfBytesWWW += bytes;
			b = m_nrOfBytesWWW;
		}
		else if (protocolType == IProxyListener.PROTOCOL_OTHER)
		{
			m_nrOfBytesOther += bytes;
			b = m_nrOfBytesOther;
		}
		else
		{
			return;
		}

		Enumeration enumer = observerVector.elements();
		while (enumer.hasMoreElements())
		{
			JAPObserver listener = (JAPObserver) enumer.nextElement();
			listener.transferedBytes(b, protocolType);
		}
	}

	/**
	 * This is the observer implementation. At the moment only the routing system is observed.
	 * It's just for comfort reasons, so there is no need to registrate the JAPView at all
	 * observable objects. We collect all messages here and send them to the view. But it's also
	 * possible to registrate directly at the observed objects. So every developer can decide,
	 * whether to use the common JAP notification system or the specific ones. Also keep in mind,
	 * that maybe not all messages are forwarded to the common notification system (like statistic messages).
	 *
	 * @param a_notifier The observed Object (various forwarding related objects).
	 * @param a_message The reason of the notification, e.g. a JAPRoutingMessage.
	 *
	 */
	public void update(Observable a_notifier, Object a_message)
	{
		try
		{
			if (a_notifier == JAPModel.getInstance().getRoutingSettings())
			{
				/* message is from JAPRoutingSettings */
				if ( ( (JAPRoutingMessage) (a_message)).getMessageCode() ==
					JAPRoutingMessage.ROUTING_MODE_CHANGED)
				{
					/* routing mode was changed -> notify the observers of JAPController */
					notifyJAPObservers();
				}
				if ( ( (JAPRoutingMessage) (a_message)).getMessageCode() ==
					JAPRoutingMessage.CLIENT_SETTINGS_CHANGED)
				{
					/* the forwarding-client settings were changed -> notify the observers of JAPController */
					notifyJAPObservers();
				}

			}
			else if (a_notifier == JAPModel.getInstance().getRoutingSettings().getRegistrationStatusObserver())
			{
				/* message is from JAPRoutingRegistrationStatusObserver */
				if ( ( (JAPRoutingMessage) (a_message)).getMessageCode() ==
					JAPRoutingMessage.REGISTRATION_STATUS_CHANGED)
				{
					/* the registration status of the local forwarding server has changed */
					notifyJAPObservers();
				}
			}
			else if (a_notifier == Database.getInstance(JAPMinVersion.class) && a_message != null &&
					 ((DatabaseMessage)a_message).getMessageData() instanceof JAPMinVersion)
			{
				JAPMinVersion version = (JAPMinVersion)((DatabaseMessage)a_message).getMessageData();
				final String versionNumber = version.getJapSoftware().getVersion().trim();
				final boolean bForce = (versionNumber.compareTo(JAPConstants.aktVersion) > 0);

				/*if (!bForce && !JAPModel.getInstance().isReminderForOptionalUpdateActivated())
				{
					return;
				}*/

				new Thread(new Runnable()
				{
					public void run()
					{

						synchronized (LOCK_VERSION_UPDATE)
						{
							if (m_bShowingVersionUpdate)
							{
								return;
							}
							m_bShowingVersionUpdate = true;
						}

						try
						{
							versionCheck(versionNumber, bForce);
						}
						catch (Throwable a_e)
						{
							LogHolder.log(LogLevel.EXCEPTION, LogType.MISC, a_e);
						}

						synchronized (LOCK_VERSION_UPDATE)
						{
							m_bShowingVersionUpdate = false;
						}
					}
				}).start();
			}
		}
		catch (Exception e)
		{
			/* should not happen, but better than throwing a runtime exception */
			LogHolder.log(LogLevel.EXCEPTION, LogType.THREAD, e);
		}
	}

	/**
	 * Enables or disables the forwarding server.
	 * Attention: If there is an active forwarding client running, nothing is done and this method
	 * returns always false. Run a forwarding server and a client at the same time is not supported.
	 * This method returns always immedailly and the real job is done in a background thread.
	 * @param a_activate True, if there server shall be activated or false, if it shall be disabled.
	 *
	 */
	public synchronized void enableForwardingServer(boolean a_activate)
	{
		if (!m_bForwarderNotExplain && a_activate)
		{
			/* show a message box with the explanation of the forwarding stuff */
			JAPDialog.LinkedCheckBox checkbox = new JAPDialog.LinkedCheckBox(false)
			{
				public boolean isOnTop()
				{
					return true;
				}
			};

			JAPDialog.showMessageDialog(getViewWindow(), JAPMessages.getString("forwardingExplainMessage"),
										JAPMessages.getString("forwardingExplainMessageTitle"), checkbox);

			m_bForwarderNotExplain = checkbox.getState();
		}
		if (m_iStatusPanelMsgIdForwarderServerStatus != -1)
		{
			/* remove old forwarding server messages from the status bar */
			m_View.removeStatusMsg(m_iStatusPanelMsgIdForwarderServerStatus);
			m_iStatusPanelMsgIdForwarderServerStatus = -1;
		}
		if (JAPModel.getInstance().getRoutingSettings().getRoutingMode() !=
			JAPRoutingSettings.ROUTING_MODE_CLIENT)
		{
			/* don't allow to interrupt the client forwarding mode */
			if (a_activate)
			{
				/* start the server */
				if (JAPModel.getInstance().getRoutingSettings().setRoutingMode(JAPRoutingSettings.
					ROUTING_MODE_SERVER) == true)
				{
					/* starting the server was successful -> start propaganda with blocking in a separate
					 * thread
					 */
					Thread startPropagandaThread = new Thread(new Runnable()
					{
						public void run()
						{
							try
							{
								int msgId = getView().addStatusMsg(JAPMessages.getString(
									"controllerStatusMsgRoutingStartServer"),
									JAPDialog.MESSAGE_TYPE_INFORMATION, false);
								int registrationStatus = JAPModel.getInstance().getRoutingSettings().
									startPropaganda(true);
								getView().removeStatusMsg(msgId);
								/* if there occured an error while registration, show a message box */
								switch (registrationStatus)
								{
									case JAPRoutingSettings.REGISTRATION_NO_INFOSERVICES:
									{
										JAPDialog.showErrorDialog(getViewWindow(),
											JAPMessages.getString(
												"settingsRoutingServerRegistrationEmptyListError"),
											LogType.MISC);
										break;
									}
									case JAPRoutingSettings.REGISTRATION_UNKNOWN_ERRORS:
									{
										JAPDialog.showErrorDialog(getViewWindow(),
											JAPMessages.getString(
											"settingsRoutingServerRegistrationUnknownError"),
											LogType.MISC);
										break;
									}
									case JAPRoutingSettings.REGISTRATION_INFOSERVICE_ERRORS:
									{
										JAPDialog.showErrorDialog(getViewWindow(),
											JAPMessages.getString(
												"settingsRoutingServerRegistrationInfoservicesError"),
											LogType.MISC);
										break;
									}
									case JAPRoutingSettings.REGISTRATION_VERIFY_ERRORS:
									{
										JAPDialog.showErrorDialog(getViewWindow(),
											JAPMessages.getString(
												"settingsRoutingServerRegistrationVerificationError"),
											LogType.MISC);
										break;
									}
									case JAPRoutingSettings.REGISTRATION_SUCCESS:
									{
										/* show a success message in the status bar */
										m_iStatusPanelMsgIdForwarderServerStatus = m_View.addStatusMsg(
											JAPMessages.getString(
											"controllerStatusMsgRoutingStartServerSuccess"),
											JAPDialog.MESSAGE_TYPE_INFORMATION, true);
									}
								}
							}
							catch (Exception a_e)
							{
								LogHolder.log(LogLevel.EXCEPTION, LogType.THREAD, a_e);
							}
						}
					});
					startPropagandaThread.setDaemon(true);
					startPropagandaThread.start();
				}
				else
				{
					/* opening the server port was not successful -> show an error message */
					m_iStatusPanelMsgIdForwarderServerStatus = getView().addStatusMsg(JAPMessages.getString(
						"controllerStatusMsgRoutingStartServerError"), JAPDialog.MESSAGE_TYPE_ERROR, true);
					JAPDialog.showErrorDialog(getViewWindow(),
											  JAPMessages.getString("settingsRoutingStartServerError"),
											  LogType.MISC);

				}
			}
			else
			{
				/* stop the server -> the following call will stop all forwarding server activities
				 * immediately
				 */
				JAPModel.getInstance().getRoutingSettings().setRoutingMode(JAPRoutingSettings.
					ROUTING_MODE_DISABLED);
				m_iStatusPanelMsgIdForwarderServerStatus = getView().addStatusMsg(JAPMessages.getString(
					"controllerStatusMsgRoutingServerStopped"), JAPDialog.MESSAGE_TYPE_INFORMATION, true);
			}
		}
	}

	public static InfoServiceDBEntry[] createDefaultInfoServices() throws Exception
	{
		Vector listeners;
		InfoServiceDBEntry[] entries = new InfoServiceDBEntry[JAPConstants.DEFAULT_INFOSERVICE_NAMES.length];
		for (int i = 0; i < entries.length; i++)
		{
			listeners = new Vector(JAPConstants.DEFAULT_INFOSERVICE_PORT_NUMBERS[i].length);
			for (int j = 0; j < JAPConstants.DEFAULT_INFOSERVICE_PORT_NUMBERS[i].length; j++)
			{
				listeners.addElement(new ListenerInterface(JAPConstants.DEFAULT_INFOSERVICE_HOSTNAMES[i],
					JAPConstants.DEFAULT_INFOSERVICE_PORT_NUMBERS[i][j]));
			}
			entries[i] = new InfoServiceDBEntry(
						 JAPConstants.DEFAULT_INFOSERVICE_NAMES[i],
						 JAPConstants.DEFAULT_INFOSERVICE_NAMES[i], listeners, false, true, 0, 0);
		}

		return entries;
	}


	private static void addDefaultCertificates(String a_certspath, String[] a_singleCerts, int a_type)
	{
		JAPCertificate defaultRootCert = null;

		for (int i = 0; i < a_singleCerts.length; i++)
		{
			if (a_singleCerts[i] != null &&
				(!JAPConstants.m_bReleasedVersion ||
				 !a_singleCerts[i].endsWith(".dev")))
			{
				defaultRootCert = JAPCertificate.getInstance(ResourceLoader.loadResource(
					JAPConstants.CERTSPATH + a_certspath + a_singleCerts[i]));
				if (defaultRootCert == null)
				{
					continue;
				}
				SignatureVerifier.getInstance().getVerificationCertificateStore().
					addCertificateWithoutVerification(defaultRootCert, a_type, true, true);
			}
		}
		String strBlockCert = null;
		if (JAPConstants.m_bReleasedVersion)
		{
			strBlockCert = ".dev";
		}
		Enumeration certificates =
			JAPCertificate.getInstance(JAPConstants.CERTSPATH + a_certspath, true, strBlockCert).elements();
		while (certificates.hasMoreElements())
		{
			defaultRootCert = (JAPCertificate) certificates.nextElement();
			SignatureVerifier.getInstance().getVerificationCertificateStore().
				addCertificateWithoutVerification(defaultRootCert, a_type, true, true);
		}
		/* no elements were found */
		if (defaultRootCert == null)
		{
			LogHolder.log(LogLevel.ERR, LogType.MISC,
						  "Error loading certificates of type '" + a_type + "'.");
		}
	}


	/** load the default certificates */
	public static void addDefaultCertificates()
	{
		addDefaultCertificates(JAPConstants.MIX_CERTSPATH, JAPConstants.MIX_ROOT_CERTS,
							   JAPCertificate.CERTIFICATE_TYPE_ROOT_MIX);

		addDefaultCertificates(JAPConstants.INFOSERVICE_CERTSPATH, JAPConstants.INFOSERVICE_ROOT_CERTS,
							   JAPCertificate.CERTIFICATE_TYPE_ROOT_INFOSERVICE);

		addDefaultCertificates(JAPConstants.PAYMENT_ROOT_CERTSPATH, JAPConstants.PAYMENT_ROOT_CERTS,
							   JAPCertificate.CERTIFICATE_TYPE_ROOT_PAYMENT);
		addDefaultCertificates(JAPConstants.PAYMENT_DEFAULT_CERTSPATH, JAPConstants.PI_CERTS,
							   JAPCertificate.CERTIFICATE_TYPE_PAYMENT);

		JAPCertificate updateMessagesCert = JAPCertificate.getInstance(ResourceLoader.loadResource(
			  JAPConstants.CERTSPATH + JAPConstants.CERT_JAPINFOSERVICEMESSAGES));
		if (updateMessagesCert != null)
		{
			SignatureVerifier.getInstance().getVerificationCertificateStore().
				addCertificateWithoutVerification(updateMessagesCert, JAPCertificate.CERTIFICATE_TYPE_UPDATE, true, true);
			//SignatureVerifier.getInstance().getVerificationCertificateStore().
				//addCertificateWithoutVerification(updateMessagesCert, JAPCertificate.CERTIFICATE_TYPE_ROOT_UPDATE, true, true);
		}
		else
		{
			LogHolder.log(LogLevel.ERR, LogType.MISC,
						  "Error loading default update messages certificate.");
		}
	}

	public void connecting(AnonServerDescription a_serverDescription)
	{
		if (a_serverDescription instanceof MixCascade &&
			!m_currentMixCascade.equals(a_serverDescription))
		{
			m_currentMixCascade = (MixCascade)a_serverDescription;
			setChanged();
			notifyObservers(new JAPControllerMessage(JAPControllerMessage.CURRENT_MIXCASCADE_CHANGED));
			notifyJAPObservers();
		}
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

	public void connectionEstablished(AnonServerDescription a_serverDescription)
	{
		Thread run = new Thread(new Runnable()
		{
			public void run()
			{
				if (!JAPModel.getInstance().isInfoServiceDisabled())
				{
					m_feedback.update();
				}
			}
		});
		run.setDaemon(true);
		run.start();

		synchronized (m_anonServiceListener)
		{
			Enumeration e = m_anonServiceListener.elements();
			while (e.hasMoreElements())
			{
				( (AnonServiceEventListener) e.nextElement()).connectionEstablished(
								a_serverDescription);
			}
		}
		m_lastBalanceUpdateBytes = 0;
		transferedBytes(0, IProxyListener.PROTOCOL_WWW);
		transferedBytes(0, IProxyListener.PROTOCOL_OTHER);
		
		if(isPortableMode() && m_Model.getStartPortableFirefox())
		{
			if(!m_firstPortableFFStart)
			{
				LogHolder.log(LogLevel.DEBUG, LogType.MISC, "First browser start");
				m_firstPortableFFStart = true;
				/*@todo: should better get the browser command from JAPModel */
				startPortableFirefox(m_View.getBrowserCommand());
			}
		}
	}

	public void dataChainErrorSignaled()
	{
		connectionError();
		synchronized (m_anonServiceListener)
		{
			Enumeration e = m_anonServiceListener.elements();
			while (e.hasMoreElements())
			{
				( (AnonServiceEventListener) e.nextElement()).dataChainErrorSignaled();
			}
		}
	}

	public void disconnected()
	{
		synchronized (m_finishSync)
		{
			synchronized (PROXY_SYNC)
			{
				if (m_proxyAnon != null)
				{
					// leads to deadlock
					//m_proxyAnon.stop();
				}
				m_proxyAnon = null;
				m_nrOfBytesWWW = 0;
				m_nrOfBytesOther = 0;
				transferedBytes(0, IProxyListener.PROTOCOL_WWW);
				transferedBytes(0, IProxyListener.PROTOCOL_OTHER);
			}
			synchronized (m_anonServiceListener)
			{
				Enumeration e = m_anonServiceListener.elements();
				while (e.hasMoreElements())
				{
					( (AnonServiceEventListener) e.nextElement()).disconnected();
				}
			}

			m_finishSync.notifyAll();
		}
	}

	public void connectionError()
	{
		LogHolder.log(LogLevel.ERR, LogType.NET, "JAPController received connectionError");
		if (!m_Model.isAutomaticallyReconnected())
		{
			this.setAnonMode(false);
		}

		synchronized (m_anonServiceListener)
		{
			Enumeration e = m_anonServiceListener.elements();
			while (e.hasMoreElements())
			{
				( (AnonServiceEventListener) e.nextElement()).connectionError();
			}
		}
	}

	public void unrealisticBytes(long a_bytes)
	{
		JAPDialog.LinkedInformationAdapter adapter =
			new JAPDialog.LinkedInformationAdapter()
		{
			public boolean isOnTop()
			{
				return true;
			}
		};

		boolean choice = JAPDialog.showYesNoDialog(
			getViewWindow(),
			JAPMessages.getString("unrealBytesDesc") + "<p>" +
			JAPMessages.getString("unrealBytesDifference") + " " + JAPUtil.formatBytesValueWithUnit(a_bytes),
			JAPMessages.getString("unrealBytesTitle"),adapter
			);

		if (!choice)
		{
			this.setAnonMode(false);
		}
	}

	/**
	 * Gets the password for payment data encryption
	 * @return String
	 */
	public String getPaymentPassword()
	{
		return JAPModel.getInstance().getPaymentPassword();
	}

	/**
	 * Sets the password for payment data encryption
	 * @param a_password Strign
	 */
	public void setPaymentPassword(String a_password)
	{
		JAPModel.getInstance().setPaymentPassword(a_password);
	}

	public void packetMixed(final long a_totalBytes)
	{
		JobQueue.Job job = new JobQueue.Job(true)
		{
			public void runJob()
			{
				PayAccount currentAccount = PayAccountsFile.getInstance().getActiveAccount();
				MixCascade cascade = JAPController.this.getCurrentMixCascade();
				if (currentAccount == null || !cascade.isPayment())
				{
					return;
				}

				if (System.currentTimeMillis() - ACCOUNT_UPDATE_INTERVAL_MS > m_lastBalanceUpdateMS ||
					a_totalBytes  - (cascade.getPrepaidInterval() / 2) >
					m_lastBalanceUpdateBytes)
				{
					// fetch new balance
					try
					{
						currentAccount.fetchAccountInfo(JAPModel.getInstance().getPaymentProxyInterface(), false);
					}
					catch (Exception ex)
					{
						if (!isShuttingDown())
						{
							LogHolder.log(LogLevel.WARNING, LogType.PAY, ex);
						}
					}

					m_lastBalanceUpdateMS = System.currentTimeMillis();
					m_lastBalanceUpdateBytes = a_totalBytes;
				}
			}
		};
		queueFetchAccountInfo.addJob(job);



		synchronized (m_anonServiceListener)
		{
			Enumeration e = m_anonServiceListener.elements();
			while (e.hasMoreElements())
			{
				( (AnonServiceEventListener) e.nextElement()).packetMixed(a_totalBytes);
			}
		}
	}

	public boolean getDontAskPayment()
	{
		return true;
		//return m_bPayCascadeNoAsk;
	}

	public void setDontAskPayment(boolean a_payCascadeNoAsk)
	{
		m_bPayCascadeNoAsk = a_payCascadeNoAsk;
	}

	/**
	 * This class returns a new random cascade from all currently available cascades every time
	 * getNextCascade() is called. If all available cascades have been returned once, this class starts
	 * again by choosing the random cascades from all available ones.
	 * @author Rolf Wendolsky
	 */
	private class AutoSwitchedMixCascadeContainer extends AbstractMixCascadeContainer
	{
		private Hashtable m_alreadyTriedCascades;
		private Random m_random;
		private MixCascade m_initialCascade;
		private MixCascade m_currentCascade;
		private boolean m_bKeepCurrentCascade;
		private boolean m_bSkipInitialCascade;

		public AutoSwitchedMixCascadeContainer(boolean a_bSkipInitialCascade)
		{
			m_bSkipInitialCascade = a_bSkipInitialCascade;
			m_alreadyTriedCascades = new Hashtable();
			m_random = new Random(System.currentTimeMillis());
			m_random.nextInt();
			m_initialCascade = JAPController.getInstance().getCurrentMixCascade();
			m_bKeepCurrentCascade = false;
		}

		public AutoSwitchedMixCascadeContainer()
		{
			this(false);
		}
		public MixCascade getInitialCascade()
		{
			return m_initialCascade;
		}

		public MixCascade getNextMixCascade()
		{
			synchronized (m_alreadyTriedCascades)
			{
				if (!JAPModel.getInstance().isCascadeAutoSwitched())
				{
					m_alreadyTriedCascades.clear();
					m_bKeepCurrentCascade = false;
					if (m_currentCascade == null)
					{
						m_currentCascade = m_initialCascade;
					}
				}
				else if (m_bKeepCurrentCascade)
				{
					// do not check if this cascade has been used before
					m_bKeepCurrentCascade = false;
					if (m_currentCascade == null)
					{
						m_currentCascade = m_initialCascade;
					}
					if (m_currentCascade != null)
					{
						m_alreadyTriedCascades.put(m_currentCascade.getId(), m_currentCascade);
					}
				}
				else if (m_bSkipInitialCascade || m_initialCascade == null ||
						 m_alreadyTriedCascades.containsKey(m_initialCascade.getId()))
				{
					MixCascade currentCascade = null;
					Vector availableCascades;
					boolean forward = true;

					availableCascades = Database.getInstance(MixCascade.class).getEntryList();
					if (availableCascades.size() > 0)
					{
						int chosenCascadeIndex = m_random.nextInt();
						if (chosenCascadeIndex < 0)
						{
							// only positive numbers are allowed
							chosenCascadeIndex *= -1;
							// move backward
							forward = false;
						}

						// chose an index from the vector
						chosenCascadeIndex %= availableCascades.size();
						/* Go through all indices until a suitable MixCascade is found or the original index
						 * is reached.
						 */
						int i;
						for (i = 0; i < availableCascades.size(); i++)
						{
							currentCascade = (MixCascade) availableCascades.elementAt(chosenCascadeIndex);
							// this is the logic that decides whether to use a cascade or not
							if (!m_alreadyTriedCascades.containsKey(currentCascade.getId()))
							{
								m_alreadyTriedCascades.put(currentCascade.getId(), currentCascade);
								if (isSuitableCascade(currentCascade))
								{
									// found a suitable cascade
									break;
								}
							}
							if (forward)
							{
								chosenCascadeIndex = (chosenCascadeIndex + 1) % availableCascades.size();
							}
							else
							{
								chosenCascadeIndex -= 1;
								if (chosenCascadeIndex < 0)
								{
									chosenCascadeIndex = availableCascades.size() - 1;
								}
							}
						}
						if (i == availableCascades.size())
						{
							// no suitable cascade was found
							if (m_alreadyTriedCascades.size() == 0)
							{
								/** @todo Perhaps we should insert a timeout here? */
							}
							currentCascade = null;
						}
					}
					else if (m_initialCascade == null)
					{
						// no cascade is available
						return null;
					}
					if (currentCascade == null)
					{
						m_bSkipInitialCascade = false; // this is not the first call
						m_alreadyTriedCascades.clear();
						currentCascade = getNextMixCascade();
						if (currentCascade == null && m_initialCascade != null)
						{
							// fallback if there are really no cascades; take the initial cascade
							currentCascade = m_initialCascade;
							m_alreadyTriedCascades.put(m_initialCascade.getId(), m_initialCascade);
						}
					}
					m_currentCascade = currentCascade;
				}
				else
				{
					m_alreadyTriedCascades.put(m_initialCascade.getId(), m_initialCascade);
					m_currentCascade = m_initialCascade;
				}

				if (m_bSkipInitialCascade)
				{
					m_initialCascade = m_currentCascade;
				}
				// this only happens for the first call
				m_bSkipInitialCascade = false;
			}

			return m_currentCascade;
		}

		public boolean isServiceAutoSwitched()
		{
			return JAPModel.getInstance().isCascadeAutoSwitched();
		}
		public boolean isReconnectedAutomatically()
		{
			return JAPModel.getInstance().isAutomaticallyReconnected();
		}

		private boolean isSuitableCascade(MixCascade a_cascade)
		{
			if (a_cascade == null)
			{
				return false;
			}

			if (a_cascade.isPayment() && isConfigAssistantShown() && !isPortableMode() &&
				!TrustModel.getCurrentTrustModel().isPaymentForced())
			{
				// do not connect to payment for new users
				return false;
			}

			if (m_initialCascade != null && m_bSkipInitialCascade && a_cascade.equals(m_initialCascade))
			{
				return false;
			}

			/*
			 * Cascade is not suitable if payment and the warning dialog is shown or no account is available
			 * Otherwise the user would have to answer a dialog which is not good for automatic connections.
			 */
			/*
			return isTrusted(a_cascade) && !(a_cascade.isPayment() &&
					 ( !JAPController.getInstance().getDontAskPayment() ||
					  PayAccountsFile.getInstance().getNumAccounts() == 0 ||
					  PayAccountsFile.getInstance().getActiveAccount() == null ||
					  PayAccountsFile.getInstance().getActiveAccount().getBalance().getCredit() == 0));*/
			return isTrusted(a_cascade);


		}
		public MixCascade getCurrentMixCascade()
		{
			return m_currentCascade;
		}

		public void keepCurrentService(boolean a_bKeepCurrentCascade)
		{
			synchronized (m_alreadyTriedCascades)
			{
				m_bKeepCurrentCascade = a_bKeepCurrentCascade;
			}
		}

		public void checkTrust(MixCascade a_cascade) throws TrustException, SignatureException
		{
			TrustModel.getCurrentTrustModel().checkTrust(a_cascade);
		}
	}
}
