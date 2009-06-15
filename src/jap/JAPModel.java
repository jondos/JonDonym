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
package jap;

import gui.GUIUtils;
import gui.JAPDll;
import gui.dialog.JAPDialog;
import gui.help.AbstractHelpFileStorageManager;
import gui.help.IHelpModel;
import gui.help.LocalHelpFileStorageManager;
import jap.forward.JAPRoutingSettings;

import java.awt.Dimension;
import java.awt.Point;
import java.io.File;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.Observable;
import java.util.Vector;
import java.util.Hashtable;

import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;

import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;
import platform.AbstractOS;
import anon.client.TrustModel;
import anon.crypto.JAPCertificate;
import anon.infoservice.IMutableProxyInterface;
import anon.infoservice.IProxyInterfaceGetter;
import anon.infoservice.IServiceContextContainer;
import anon.infoservice.ImmutableProxyInterface;
import anon.infoservice.ProxyInterface;
import anon.mixminion.mmrdescription.MMRList;
import anon.util.ClassUtil;
import anon.util.JAPMessages;
import anon.util.RecursiveFileTool;
import anon.util.ResourceLoader;
import anon.util.Util;

/* This is the Model of All. It's a singleton!*/
public final class JAPModel extends Observable implements IHelpModel, IServiceContextContainer
{
	public static final String MACOSX_LIB_NEEDS_UPDATE = "macOSXLibNeedsUpdate";
	public static final String DLL_VERSION_UPDATE = "dllVersionUpdate";
	public static final String DLL_VERSION_WARNING_BELOW = "dllWarningVersion";
	
	public static final int CONNECTION_ALLOW_ANONYMOUS = 0;
	public static final int CONNECTION_FORCE_ANONYMOUS = 1;
	public static final int CONNECTION_BLOCK_ANONYMOUS = 2;	

	public static final String XML_ANONYMIZED_HTTP_HEADERS = "anonymizedHttpHeaders";
	public static final String XML_REMIND_OPTIONAL_UPDATE = "remindOptionalUpdate";
	public static final String XML_REMIND_JAVA_UPDATE = "remindJavaUpdate";
	public static final String XML_RESTRICT_CASCADE_AUTO_CHANGE = "restrictCascadeAutoChange";
	public static final String XML_ASK_FOR_NON_ANONYMOUS_SURFING = "askForUnprotectedSurfing";
	public static final String XML_ATTR_ACTIVATED = "activated";
	public static final String XML_FONT_SIZE = "fontSize";
	public static final String XML_CONFIG_WINDOW = "ConfigWindow";
	public static final String XML_SIZE = "Size";
	public static final String XML_ICONIFIED_WINDOW = "IconifiedWindow";
	public static final String XML_ATTR_ICONIFIED_ON_TOP = "alwaysOnTop";
	public static final String XML_HELP_WINDOW = "HelpWindow";
	public static final String XML_ATTR_WIDTH = "width";
	public static final String XML_ATTR_HEIGHT = "height";
	public static final String XML_ATTR_SAVE = "save";

	public static final String AUTO_CHANGE_NO_RESTRICTION = "none";
	public static final String AUTO_CHANGE_RESTRICT_TO_PAY = "pay";
	public static final String AUTO_CHANGE_RESTRICT = "restrict";
	
	public static final String NO_HELP_STORAGE_MANAGER = "help_internal";

	public static final int MAX_FONT_SIZE = 3;

	// observer messages
	public static final Integer CHANGED_INFOSERVICE_AUTO_UPDATE = new Integer(0);
	public static final Integer CHANGED_ALLOW_INFOSERVICE_DIRECT_CONNECTION = new Integer(1);
	public static final Integer CHANGED_ALLOW_UPDATE_DIRECT_CONNECTION = new Integer(2);
	public static final Integer CHANGED_NOTIFY_JAP_UPDATES = new Integer(3);
	public static final Integer CHANGED_NOTIFY_JAVA_UPDATES = new Integer(4);
	public static final Integer CHANGED_AUTO_CONNECT = new Integer(5);
	public static final Integer CHANGED_AUTO_RECONNECT = new Integer(6);
	public static final Integer CHANGED_CASCADE_AUTO_CHANGE = new Integer(7);
	public static final Integer CHANGED_ASK_FOR_NON_ANONYMOUS = new Integer(8);
	public static final Integer CHANGED_HELP_PATH = new Integer(9);
	public static final Integer CHANGED_DLL_UPDATE = new Integer(10);
	public static final Integer CHANGED_MACOSX_LIBRARY_UPDATE = new Integer(11);
	public static final Integer CHANGED_ANONYMIZED_HTTP_HEADERS = new Integer(12);
	public static final Integer CHANGED_CONTEXT = new Integer(13);
	
	private static final String[] MSG_CONNECTION_ANONYMOUS = new String[] {
		JAPModel.class.getName() + "_anonymousConnectionAllow", 
		JAPModel.class.getName() + "_anonymousConnectionForce", 
		JAPModel.class.getName() + "_anonymousConnectionBlock"
	};

	private static final int DIRECT_CONNECTION_INFOSERVICE = 0;
	private static final int DIRECT_CONNECTION_PAYMENT = 1;
	private static final int DIRECT_CONNECTION_UPDATE = 2;

	private int m_HttpListenerPortNumber = JAPConstants.DEFAULT_PORT_NUMBER; // port number of HTTP  listener
	private boolean m_bHttpListenerIsLocal = JAPConstants.DEFAULT_LISTENER_IS_LOCAL; // indicates whether listeners serve for localhost only or not
	private ProxyInterface m_proxyInterface = null;
	private ProxyInterface m_proxyAnon;
	private final Object SYNC_ANON_PROXY = new Object();
	private IMutableProxyInterface m_mutableProxyInterface;
	private boolean m_bAutoConnect; // autoconnect after program start
	private boolean m_bAutoReConnect; // autoReconnects after loosing connection to mix

	private int m_iDummyTrafficIntervall = -1; // indicates what Dummy Traffic should be generated or not

	private boolean m_bSmallDisplay = false;
	private boolean m_bInfoServiceDisabled = JAPConstants.DEFAULT_INFOSERVICE_DISABLED;
	private boolean m_bMinimizeOnStartup = JAPConstants.DEFAULT_MINIMIZE_ON_STARTUP; // true if program will start minimized
	private boolean m_bMoveToSystrayOnStartup = JAPConstants.DEFAULT_MOVE_TO_SYSTRAY_ON_STARTUP; // true if program will start in the systray
	private int m_iDefaultView = JAPConstants.DEFAULT_VIEW; //which view we should start?

	private boolean m_bSaveMainWindowPosition;
	private boolean m_bSaveConfigWindowPosition;
	private boolean m_bSaveIconifiedWindowPosition;
	private boolean m_bSaveHelpWindowPosition;
	private Point m_OldMainWindowLocation = null;
	private Point m_iconifiedWindowLocation = null;
	private Point m_configWindowLocation = null;
	private Point m_helpWindowLocation = null;

	private boolean m_bGoodByMessageNeverRemind = false; // indicates if Warning message before exit has been deactivated forever

	private int m_iPaymentAnonymousConnectionSetting;
	private int m_iInfoServiceAnonymousConnectionSetting;
	private int m_iUpdateAnonymousConnectionSetting;

	private boolean m_bAskForAnyNonAnonymousRequest;

	private boolean m_bRemindOptionalUpdate;
	private boolean m_bRemindJavaUpdate;

	private boolean m_bTorActivated;
	private boolean m_bMixMinionActivated;

	private boolean m_bChooseCascasdeConnectionAutomatically;
	private boolean m_bChooseCascasdeAutomaticallyOnStartup;

	private boolean m_bMiniViewOnTop;
	private String m_strLookAndFeel;
	private Vector m_vecLookAndFeels = new Vector();
	private LookAndFeelInfo[] m_systemLookAndFeels;
	private Object LOOK_AND_FEEL_SYNC = new Object();
	
	private boolean m_bShowDialogFormat = false;
	
	private boolean m_bAnonymizedHttpHeaders = JAPConstants.ANONYMIZED_HTTP_HEADERS;

	private String m_context = CONTEXT_JONDONYM;
	
	private String m_strDistributorMode = JAPConstants.PROGRAM_NAME_JAP_JONDO;
	
	private String m_strRelativeBrowserPath;
	
	private int m_fontSize = 0;

	private GUIUtils.IIconResizer m_resizer = new GUIUtils.IIconResizer()
		{
			public double getResizeFactor()
			{
				return 1.0 + getFontSize() * 0.1;
			}
	};

	private static JAPModel ms_TheModel = null;

	//private boolean m_bCertCheckDisabled = true;

	private JAPCertificate m_certJAPCodeSigning = null;

	private int m_TorMaxConnectionsPerRoute = JAPConstants.DEFAULT_TOR_MAX_CONNECTIONS_PER_ROUTE;
	private int m_TorMaxRouteLen = JAPConstants.DEFAULT_TOR_MAX_ROUTE_LEN;
	private int m_TorMinRouteLen = JAPConstants.DEFAULT_TOR_MIN_ROUTE_LEN;
	private boolean m_bTorUseNoneDefaultDirServer=JAPConstants.DEFAULT_TOR_USE_NONE_DEFAULT_DIR_SERVER;

	private int m_mixminionRouteLen = JAPConstants.DEFAULT_MIXMINION_ROUTE_LEN;
	private String m_mixminionMyEMail = JAPConstants.DEFAULT_MIXMINION_EMAIL;
	//fixme sr
	private String m_mixminionPassword = null;
	private byte[] m_mixminionPasswordHash = null;
	private String m_mixminionKeyring = "";
	private Vector m_mixminionMessages = null;
	private MMRList m_mixminionRouters = null;
	private Vector m_mixminionFragments = null;
	private boolean m_bPreCreateAnonRoutes = JAPConstants.DEFAULT_TOR_PRECREATE_ROUTES;
	private boolean m_bUseProxyAuthentication = false;
	private JAPController.AnonConnectionChecker m_connectionChecker;

	private boolean m_bShowSplashScreen = true;
	private boolean m_bShowSplashDisabled = false;
	
	private boolean m_bStartPortableFirefox = true;
	
	private String m_helpPath = null;
	private boolean m_bPortableHelp = false;
	
	private Dimension m_iconifiedSize;
	private Dimension m_configSize;
	private Dimension m_helpSize;
	private boolean m_bSaveHelpSize;
	private boolean m_bSaveConfigSize;

	/**
	 * Stores the instance with the routing settings.
	 */
	private JAPRoutingSettings m_routingSettings;

	/**
	 * Stores the path and the name of the config file.
	 */
	private String m_configFileName;

	/**
	 * Stores whether the forwarding state module shall be visible within the configuration
	 * dialog.
	 */
	private boolean m_forwardingStateModuleVisible;

	/**
	 * Stores the password for account data encryption
	 */
	private String m_paymentPassword;

	/** Boolen value which describes if a dll update is necessary */
	private String m_bDllUpdatePath;
	private long m_noWarningForDllVersionBelow = 0;
	
	private boolean m_bMacOSXLibraryUpdateAtStartupNeeded = false;

	private BigInteger m_iDialogVersion=new BigInteger("-1");

	private AbstractHelpFileStorageManager m_helpFileStorageManager;
	
	private Hashtable m_acceptedTCs = new Hashtable();
	
	private JAPModel()
	{
		try
		{
			m_certJAPCodeSigning = JAPCertificate.getInstance(
				ResourceLoader.loadResource(JAPConstants.CERTSPATH + JAPConstants.CERT_JAPCODESIGNING));
		}
		catch (Throwable t)
		{
			m_certJAPCodeSigning = null;
		}
		m_routingSettings = new JAPRoutingSettings();
		m_configFileName = null;
		m_forwardingStateModuleVisible = false;
		m_mutableProxyInterface = new IMutableProxyInterface()
		{
			public IProxyInterfaceGetter getProxyInterface(boolean a_bAnonInterface)
			{
				return new IProxyInterfaceGetter()
				{
					public ImmutableProxyInterface getProxyInterface()
					{
						ProxyInterface proxy = m_proxyInterface;
						if (proxy != null && proxy.isValid())
						{
							return proxy;
						}
						return null;
					}
				};
			}
		};
		
		if (ClassUtil.getJarFile() == null)
		{
//			build environment; use the browser to open the local help files
			m_helpFileStorageManager =	
				new LocalHelpFileStorageManager(JAPConstants.APPLICATION_CONFIG_DIR_NAME);
		}
		else
		{			
			m_helpFileStorageManager = new JARHelpFileStorageManager();
		}		
	}

	// m_Locale=Locale.getDefault();

	/** Creates the Model - as Singleton.
	 *  @return The one and only JAPModel
	 */
	public static JAPModel getInstance()
	{
		if (ms_TheModel == null)
		{
			ms_TheModel = new JAPModel();
		}

		return ms_TheModel;
	}
	
	public String getPortableBrowserpath()
	{
		return m_strRelativeBrowserPath;
	}
	
	public void setPortableBrowserpath(String a_strRelativeBrowserPath)
	{
		if (a_strRelativeBrowserPath == null || a_strRelativeBrowserPath.trim().length() <= 0)
		{
			m_strRelativeBrowserPath = null;
		}
		else
		{
			m_strRelativeBrowserPath = a_strRelativeBrowserPath;
		}
	}

	public static String[] getMsgConnectionAnonymous()
	{
		return MSG_CONNECTION_ANONYMOUS;
	}
	
	public ProxyInterface getProxyInterface()
	{
		return m_proxyInterface;
	}

	public IMutableProxyInterface getMutableProxyInterface()
	{
		return m_mutableProxyInterface;
	}

	void setProxyListener(ProxyInterface a_proxyInterface)
	{
		m_proxyInterface = a_proxyInterface;
	}

	void setAutoConnect(boolean b)
	{
		synchronized (this)
		{
			if (m_bAutoConnect != b)
			{
				m_bAutoConnect = b;
				setChanged();
			}
			notifyObservers(CHANGED_AUTO_CONNECT);
		}
	}

	public static boolean isAutoConnect()
	{
		return ms_TheModel.m_bAutoConnect;
	}

	public void setAutoReConnect(boolean b)
	{
		synchronized (this)
		{
			if (m_bAutoReConnect != b)
			{
				m_bAutoReConnect = b;
				setChanged();
			}
			notifyObservers(CHANGED_AUTO_RECONNECT);
		}
	}

	public static boolean isAutomaticallyReconnected()
	{
		return ms_TheModel.m_bAutoReConnect;
	}

	public void setLookAndFeel(String a_strLookAndFeel)
	{
		m_strLookAndFeel = a_strLookAndFeel;
	}

	/**
	 * Returns a Vector with all files that are registerd to contain LookAndFeel classes.
	 * @return a Vector with all files that are registerd to contain LookAndFeel classes
	 */
	public Vector getLookAndFeelFiles()
	{
		return (Vector)m_vecLookAndFeels.clone();
	}

	public boolean addLookAndFeelFile(File a_file)
	{
		if (a_file != null)
		{
			synchronized (m_vecLookAndFeels)
			{
				if (!m_vecLookAndFeels.contains(a_file))
				{
					m_vecLookAndFeels.addElement(a_file);
					return true;
				}
			}
		}
		return false;
	}

	public boolean removeLookAndFeelFile(File a_file)
	{
		return m_vecLookAndFeels.removeElement(a_file);
	}


	public String getLookAndFeel()
	{
		return m_strLookAndFeel;
	}

	public boolean isTorActivated()
	{
		return m_bTorActivated;
	}

	public void setTorActivated(boolean a_bActivate)
	{
		m_bTorActivated = a_bActivate;
	}

	public void setMixMinionActivated(boolean a_bActivate)
	{
		m_bMixMinionActivated = a_bActivate;
	}

	public boolean isMixMinionActivated()
	{
		return m_bMixMinionActivated;
	}

	protected void setMinimizeOnStartup(boolean b)
	{
		m_bMinimizeOnStartup = b;
	}

	public static boolean getMinimizeOnStartup()
	{
		return ms_TheModel.m_bMinimizeOnStartup;
	}

	protected void setMoveToSystrayOnStartup(boolean b)
	{
		m_bMoveToSystrayOnStartup = b;
	}

	public static boolean getMoveToSystrayOnStartup()
	{
		return ms_TheModel.m_bMoveToSystrayOnStartup;
	}

	protected void setDefaultView(int view)
	{
		m_iDefaultView = view;
	}

	public static int getDefaultView()
	{
		return ms_TheModel.m_iDefaultView;
	}

	protected void setSaveMainWindowPosition(boolean b)
	{
		m_bSaveMainWindowPosition = b;
	}

	public void setSaveConfigWindowPosition(boolean a_bSave)
	{
		m_bSaveConfigWindowPosition = a_bSave;
	}

	public void setSaveIconifiedWindowPosition(boolean a_bSave)
	{
		m_bSaveIconifiedWindowPosition = a_bSave;
	}

	public void setSaveHelpWindowPosition(boolean a_bSave)
	{
		m_bSaveHelpWindowPosition = a_bSave;
	}


	public void updateSystemLookAndFeels()
	{
		synchronized (LOOK_AND_FEEL_SYNC)
		{
			m_systemLookAndFeels = UIManager.getInstalledLookAndFeels();
		}
	}

	public boolean isSystemLookAndFeel(String a_LAFclassName)
	{
		synchronized (LOOK_AND_FEEL_SYNC)
		{
			if (m_systemLookAndFeels == null || a_LAFclassName == null)
			{
				return false;
			}
			for (int i = 0; i < m_systemLookAndFeels.length; i++)
			{
				if (m_systemLookAndFeels[i] == null)
				{
					continue;
				}
				if (m_systemLookAndFeels[i].getClassName().equals(a_LAFclassName))
				{
					return true;
				}
			}
		}
		return false;
	}

	public boolean isIconifiedWindowLocationSaved()
	{
		return m_bSaveIconifiedWindowPosition;
	}

	public void setIconifiedWindowLocation(Point a_location)
	{
		m_iconifiedWindowLocation = a_location;
	}

	public Point getIconifiedWindowLocation()
	{
		if (isIconifiedWindowLocationSaved())
		{
			return m_iconifiedWindowLocation;
		}
		return null;
	}

	public boolean isHelpWindowLocationSaved()
	{
		return m_bSaveHelpWindowPosition;
	}

	public void setHelpWindowLocation(Point a_location)
	{
		m_helpWindowLocation = a_location;
	}

	public Point getHelpWindowLocation()
	{
		if (isHelpWindowLocationSaved())
		{
			return m_helpWindowLocation;
		}
		return null;
	}


	public boolean isConfigWindowLocationSaved()
	{
		return m_bSaveConfigWindowPosition;
	}

	public void setConfigWindowLocation(Point a_location)
	{
		m_configWindowLocation = a_location;
	}

	public Point getConfigWindowLocation()
	{
		if (isConfigWindowLocationSaved())
		{
			return m_configWindowLocation;
		}
		return null;
	}


	public static boolean isMainWindowLocationSaved()
	{
		return ms_TheModel.m_bSaveMainWindowPosition;
	}

	protected void setMainWindowLocation(Point location)
	{
		m_OldMainWindowLocation = location;
	}

	public static Point getMainWindowLocation()
	{
		if (isMainWindowLocationSaved())
		{
			return ms_TheModel.m_OldMainWindowLocation;
		}
		return null;
	}

	/*
//---------------------------------------------------------------------
	 public Locale getLocale() {
	  return m_Locale;
	 }
//---------------------------------------------------------------------
//---------------------------------------------------------------------
	 */
	/**
	 * Show the options to alter the dialog format
	 * @return boolean
	 */
	public boolean isDialogFormatShown()
	{
		return m_bShowDialogFormat;
	}

	public void setDialogFormatShown(boolean a_bShow)
	{
		m_bShowDialogFormat = a_bShow;
	}


	protected void setDummyTraffic(int msIntervall)
	{
		m_iDummyTrafficIntervall = msIntervall;
	}

	public static int getDummyTraffic()
	{
		return ms_TheModel.m_iDummyTrafficIntervall;
	}

	protected void setHttpListenerPortNumber(int p)
	{
		m_HttpListenerPortNumber = p;
	}

	public void setAnonConnectionChecker(JAPController.AnonConnectionChecker a_connectionChecker)
	{
		m_connectionChecker = a_connectionChecker;
	}

	public boolean isReminderForOptionalUpdateActivated()
	{
		return m_bRemindOptionalUpdate;
	}

	public void setReminderForOptionalUpdate(boolean a_bRemind)
	{
		synchronized (this)
		{
			if (m_bRemindOptionalUpdate != a_bRemind)
			{
				m_bRemindOptionalUpdate = a_bRemind;
				setChanged();
			}
			notifyObservers(CHANGED_NOTIFY_JAP_UPDATES);
		}

	}

	public boolean isReminderForJavaUpdateActivated()
	{
		return m_bRemindJavaUpdate;
	}

	public void setReminderForJavaUpdate(boolean a_bRemind)
	{
		synchronized (this)
		{
			if (m_bRemindJavaUpdate != a_bRemind)
			{
				m_bRemindJavaUpdate = a_bRemind;
				setChanged();
			}
			notifyObservers(CHANGED_NOTIFY_JAVA_UPDATES);
		}
	}

	public void setCascadeAutoSwitch(boolean a_bChooseCascasdeConnectionAutomatically)
	{
		synchronized (this)
		{
			if (m_bChooseCascasdeConnectionAutomatically != a_bChooseCascasdeConnectionAutomatically)
			{
				m_bChooseCascasdeConnectionAutomatically = a_bChooseCascasdeConnectionAutomatically;
				setChanged();
			}
			notifyObservers(CHANGED_CASCADE_AUTO_CHANGE);
		}
	}

	public boolean isCascadeAutoSwitched()
	{
		return m_bChooseCascasdeConnectionAutomatically;
	}

	public void setAutoChooseCascadeOnStartup(boolean a_bChooseCascasdeAutomaticallyOnStartup)
	{
		m_bChooseCascasdeAutomaticallyOnStartup = a_bChooseCascasdeAutomaticallyOnStartup;
	}

	public boolean isCascadeAutoChosenOnStartup()
	{
		return m_bChooseCascasdeAutomaticallyOnStartup;
	}

	public boolean isAnonConnected()
	{
		return m_connectionChecker.checkAnonConnected();
	}

	public boolean isAskForAnyNonAnonymousRequest()
	{
		return m_bAskForAnyNonAnonymousRequest;
	}

	public void setAskForAnyNonAnonymousRequest(boolean a_bAskForAnyNonAnonymousRequest)
	{
		synchronized (this)
		{
			if (m_bAskForAnyNonAnonymousRequest != a_bAskForAnyNonAnonymousRequest)
			{
				m_bAskForAnyNonAnonymousRequest = a_bAskForAnyNonAnonymousRequest;
				setChanged();
			}
			notifyObservers(CHANGED_ASK_FOR_NON_ANONYMOUS);
		}
	}

	public int getPaymentAnonymousConnectionSetting()
	{
		return m_iPaymentAnonymousConnectionSetting;
	}

	public int getUpdateAnonymousConnectionSetting()
	{
		return m_iUpdateAnonymousConnectionSetting;
	}

	public void setUpdateAnonymousConnectionSetting(int a_iUpdateAnonymousConnectionSetting)
	{
		synchronized (this)
		{
			if (m_iUpdateAnonymousConnectionSetting != a_iUpdateAnonymousConnectionSetting)
			{
				m_iUpdateAnonymousConnectionSetting = a_iUpdateAnonymousConnectionSetting;
				setChanged();
			}
			notifyObservers(CHANGED_ALLOW_UPDATE_DIRECT_CONNECTION);
		}
	}


	public int getInfoServiceAnonymousConnectionSetting()
	{
		return m_iInfoServiceAnonymousConnectionSetting;
	}

	public void setInfoServiceAnonymousConnectionSetting(int a_iInfoServiceAnonymousConnectionSetting)
	{
		synchronized (this)
		{
			if (m_iInfoServiceAnonymousConnectionSetting != a_iInfoServiceAnonymousConnectionSetting)
			{
				m_iInfoServiceAnonymousConnectionSetting = a_iInfoServiceAnonymousConnectionSetting;
				setChanged();
			}
			notifyObservers(CHANGED_ALLOW_INFOSERVICE_DIRECT_CONNECTION);
		}
	}

	public void setPaymentAnonymousConnectionSetting(int a_iPaymentAnonymousConnectionSetting)
	{
		m_iPaymentAnonymousConnectionSetting = a_iPaymentAnonymousConnectionSetting;
	}

	public IMutableProxyInterface getInfoServiceProxyInterface()
	{
		return new IMutableProxyInterface()
		{
			public IProxyInterfaceGetter getProxyInterface(boolean a_bAnonInterface)
			{
				return JAPModel.getInstance().getProxyInterface(
								DIRECT_CONNECTION_INFOSERVICE, a_bAnonInterface);
			}
		};
	}

	public IMutableProxyInterface getPaymentProxyInterface()
	{
		return new IMutableProxyInterface()
		{
			public IProxyInterfaceGetter getProxyInterface(boolean a_bAnonInterface)
			{
				return JAPModel.getInstance().getProxyInterface(DIRECT_CONNECTION_PAYMENT, a_bAnonInterface);
			}
		};
	}

	public IMutableProxyInterface getUpdateProxyInterface()
	{
		return new IMutableProxyInterface()
		{
			public IProxyInterfaceGetter getProxyInterface(boolean a_bAnonInterface)
			{
				return JAPModel.getInstance().getProxyInterface(DIRECT_CONNECTION_UPDATE, a_bAnonInterface);
			}
		};
	}

	public ImmutableProxyInterface getTorProxyInterface()
	{
		return new ProxyInterface("localhost", getHttpListenerPortNumber(),
								  ProxyInterface.PROTOCOL_TYPE_SOCKS, null);
	}

	public static int getHttpListenerPortNumber()
	{
		return ms_TheModel.m_HttpListenerPortNumber;
	}

	/* protected void setSocksListenerPortNumber(int p)
	 {
	   m_SOCKSListenerPortnumber = p;
	 }

	 public static int getSocksListenerPortNumber()
	 {
	   return ms_TheModel.m_SOCKSListenerPortnumber;
	 }
	 */
	protected void setHttpListenerIsLocal(boolean b)
	{
		m_bHttpListenerIsLocal = b;
	}

	public static boolean isHttpListenerLocal()
	{
		return ms_TheModel.m_bHttpListenerIsLocal;
	}

	public void setSmallDisplay(boolean b)
	{
		m_bSmallDisplay = b;
	}

	public static boolean isSmallDisplay()
	{
		return ms_TheModel.m_bSmallDisplay;
	}

	public boolean isNeverRemindGoodbye()
	{
		return m_bGoodByMessageNeverRemind;
	}

	public void setNeverRemindGoodbye(boolean a_bGoodByMessageNeverRemind)
	{
		m_bGoodByMessageNeverRemind = a_bGoodByMessageNeverRemind;
	}

	protected void setInfoServiceDisabled(boolean b)
	{
		synchronized (this)
		{
			if (m_bInfoServiceDisabled != b)
			{
				m_bInfoServiceDisabled = b;
				setChanged();
			}
			notifyObservers(CHANGED_INFOSERVICE_AUTO_UPDATE);
		}

	}

	public static boolean isInfoServiceDisabled()
	{
		return ms_TheModel.m_bInfoServiceDisabled;
	}

	public boolean isMiniViewOnTop()
	{
		return m_bMiniViewOnTop;
	}

	public void setMiniViewOnTop(boolean a_bMiniViewOnTop)
	{
		m_bMiniViewOnTop = a_bMiniViewOnTop;
	}

	public GUIUtils.IIconResizer getIconResizer()
	{
		return m_resizer;
	}

	/**
	 * Returns the relative font size as integer from 0 to MAX_FONT_SIZE. The real font size
	 * is calculated as 100% + getFontSize() * 10%.
	 * @return the relative font size as integer from 0 to MAX_FONT_SIZE
	 */
	public int getFontSize()
	{
		return m_fontSize;
	}

	public boolean setFontSize(int a_fontSize)
	{
		if (a_fontSize < 0)
		{
			a_fontSize = 0;
		}
		else if (a_fontSize > MAX_FONT_SIZE)
		{
			a_fontSize = MAX_FONT_SIZE;
		}
		if (m_fontSize != a_fontSize)
		{
			synchronized (this)
			{
				FontResize resize = new FontResize(m_fontSize, a_fontSize);
				if (!JAPDialog.isConsoleOnly())
				{
					GUIUtils.resizeAllFonts(1.0f / (1.0f + 0.1f * resize.getOldSize()));
					GUIUtils.resizeAllFonts(1.0f + 0.1f * resize.getNewSize());
				}
				m_fontSize = a_fontSize;
				setChanged();
				notifyObservers(resize);
			}
			return true; // font size changed
		}
		return false;
	}

	public String toString()
	{
		StringBuffer buff = new StringBuffer(2048);
		buff.append("Configuration for JAP Version ");
		buff.append(JAPConstants.aktVersion);
		buff.append("\n");
		String s = JAPDll.getDllVersion();
		if (s != null)
		{
			buff.append("Using JAPDll Version: ");
			buff.append(s);
			buff.append("\n");
		}
		s=JAPDll.getDllFileName();
		if(s!=null)
		{
			buff.append("Using JAPDll File: ");
			buff.append(s);
			buff.append("\n");
		}
		buff.append("Config path: ");
		buff.append(getConfigFile());
		buff.append("\n");
		buff.append("Help path: ");
		buff.append(getHelpPath());
		buff.append("\n");
		if (m_bDllUpdatePath != null)
		{
			buff.append("DLL update path: ");
			buff.append(m_bDllUpdatePath);
			buff.append("\n");
		}
		
		buff.append("Command line arguments: '");
		for (int i = 0; i < JAPController.getInstance().getCommandlineArgs().length; i++)
		{
			buff.append(JAPController.getInstance().getCommandlineArgs()[i]);
			if (i < JAPController.getInstance().getCommandlineArgs().length - 1)
			{
				buff.append(" ");
			}
		}
		buff.append("'\n");
		
		buff.append("HttpListenerPortNumber: ");
		buff.append(m_HttpListenerPortNumber);
		buff.append("\n");
		buff.append("HttpListenerIsLocal: ");
		buff.append(m_bHttpListenerIsLocal);
		buff.append("\n");
		buff.append("UseFirewall: ");
		boolean bFirewall = m_proxyInterface != null && m_proxyInterface.isValid();
		buff.append(bFirewall);
		buff.append("\n");
		if (bFirewall)
		{
			buff.append("FirewallType: ");
			buff.append(m_proxyInterface.getProtocol());
			buff.append("\n");
			buff.append("FirewallHost: ");
			buff.append(m_proxyInterface.getHost());
			buff.append("\n");
			buff.append("FirewallPort: ");
			buff.append(m_proxyInterface.getPort());
			buff.append("\n");
		}
		buff.append("AutoConnect: ");
		buff.append(m_bAutoConnect);
		buff.append("\n");
		buff.append("AutoReConnect: ");
		buff.append(m_bAutoReConnect);
		buff.append("\n");

		/*  private boolean m_bUseFirewallAuthentication   = false; //indicates whether JAP should use a UserID/Password to authenticat to the proxy
		 private String  m_FirewallAuthenticationUserID = null;  //userid for authentication
		 private String  m_FirewallAuthenticationPasswd = null;  // password --> will never be saved...
		 private boolean m_bAutoConnect                 = false; // autoconnect after program start
		 private boolean m_bMinimizeOnStartup
		 */
		/*     e.setAttribute("portNumber",Integer.toString(portNumber));
		  //e.setAttribute("portNumberSocks",Integer.toString(portSocksListener));
		  //e.setAttribute("supportSocks",(getUseSocksPort()?"true":"false"));
		  e.setAttribute("listenerIsLocal",(mblistenerIsLocal?"true":"false"));
		  e.setAttribute("proxyMode",(mbUseProxy?"true":"false"));
		  e.setAttribute("proxyHostName",((proxyHostName==null)?"":proxyHostName));
		  e.setAttribute("proxyPortNumber",Integer.toString(proxyPortNumber));
		  e.setAttribute("proxyAuthorization",(mb_UseProxyAuthentication?"true":"false"));
		 e.setAttribute("proxyAuthUserID",((m_ProxyAuthenticationUserID==null)?"":m_ProxyAuthenticationUserID));
		  e.setAttribute("infoServiceHostName",((infoServiceHostName==null)?"":infoServiceHostName));
		  e.setAttribute("infoServicePortNumber",Integer.toString(infoServicePortNumber));
		  AnonServerDBEntry e1 = model.getAnonServer();
		  e.setAttribute("anonserviceName",((e1.getName()==null)?"":e1.getName()));
		  e.setAttribute("anonHostName",   ((e1.getHost()==null)?"":e1.getHost()));
		  e.setAttribute("anonHostIP",   ((e1.getIP()==null)?"":e1.getIP()));
		  e.setAttribute("anonPortNumber",   Integer.toString(e1.getPort()));
		  e.setAttribute("anonSSLPortNumber",Integer.toString(e1.getSSLPort()));
		  e.setAttribute("autoConnect",(autoConnect?"true":"false"));
		  e.setAttribute("minimizedStartup",(mbMinimizeOnStartup?"true":"false"));
		  e.setAttribute("neverRemindActiveContent",(mbActCntMessageNeverRemind?"true":"false"));
		  e.setAttribute("doNotAbuseReminder",(mbDoNotAbuseReminder?"true":"false"));
		  e.setAttribute("neverRemindGoodBye",(mbGoodByMessageNeverRemind?"true":"false"));
		  e.setAttribute("Locale",m_Locale.getLanguage());
		  e.setAttribute("LookAndFeel",UIManager.getLookAndFeel().getName());
		  // adding Debug-Element
		  Element elemDebug=doc.createElement("Debug");
		  e.appendChild(elemDebug);
		  Element tmp=doc.createElement("Level");
		  Text txt=doc.createTextNode(Integer.toString(JAPDebug.getDebugLevel()));
		  tmp.appendChild(txt);
		  elemDebug.appendChild(tmp);
		  tmp=doc.createElement("Type");
		  int debugtype=JAPDebug.getDebugType();
		  tmp.setAttribute("GUI",(debugtype&JAPDebug.GUI)!=0?"true":"false");
		  tmp.setAttribute("NET",(debugtype&JAPDebug.NET)!=0?"true":"false");
		  tmp.setAttribute("THREAD",(debugtype&JAPDebug.THREAD)!=0?"true":"false");
		  tmp.setAttribute("MISC",(debugtype&JAPDebug.MISC)!=0?"true":"false");
		  elemDebug.appendChild(tmp);
		  if(JAPDebug.isShowConsole()){
		 tmp=doc.createElement("Output");
		 txt=doc.createTextNode("Console");
		 tmp.appendChild(txt);
		 elemDebug.appendChild(tmp);
		  }
		  return JAPUtil.XMLDocumentToString(doc);
		  //((XmlDocument)doc).write(f);
		 }
		 catch(Exception ex) {
		  JAPDebug.out(JAPDebug.EXCEPTION,JAPDebug.MISC,"JAPModel:save() Exception: "+ex.getMessage());
		  //ex.printStackTrace();
		 }
		 */
		return buff.toString();
	}

	public static boolean isPreCreateAnonRoutesEnabled()
	{
		return ms_TheModel.m_bPreCreateAnonRoutes;
	}

	void setPreCreateAnonRoutes(boolean b)
	{
		m_bPreCreateAnonRoutes = b;
	}

	public static JAPCertificate getJAPCodeSigningCert()
	{
		return ms_TheModel.m_certJAPCodeSigning;
	}

	/**
	 * Changes the filename of the used config file.
	 *
	 * @param a_configFileName The filename (including path) of the used configuration file.
	 */
	public void setConfigFile(String a_configFileName)
	{
		m_configFileName = a_configFileName;
	}

	public void setIconifiedSize(Dimension a_size)
	{
		m_iconifiedSize = a_size;
	}

	public Dimension getIconifiedSize()
	{
		return m_iconifiedSize;
	}

	public void setHelpWindowSize(Dimension a_size)
	{
		m_helpSize = a_size;
	}

	public Dimension getHelpWindowSize()
	{
		return m_helpSize;
	}

	public boolean isHelpWindowSizeSaved()
	{
		return m_bSaveHelpSize;
	}

	public void setSaveHelpWindowSize(boolean a_bSave)
	{
		m_bSaveHelpSize = a_bSave;
	}

	public void setSaveConfigWindowSize(boolean a_bSave)
	{
		m_bSaveConfigSize = a_bSave;
	}

	public boolean isConfigWindowSizeSaved()
	{
		return m_bSaveConfigSize;
	}



	public void setConfigSize(Dimension a_size)
	{
		m_configSize = a_size;
	}

	public Dimension getConfigSize()
	{
		return m_configSize;
	}

	/**
	 * Returns the filename of the used config file.
	 *
	 * @return The filename (including path) of the used configuration file.
	 */
	public String getConfigFile()
	{
		return m_configFileName;
	}

	/**
	 * This method returns the instance of JAPRoutingSettings, where all routing settings are
	 * stored in. Changes of the routing settings are directly done on the returned instance.
	 * @see JAPRoutingSettings
	 *
	 * @return The routing settings.
	 */
	public JAPRoutingSettings getRoutingSettings()
	{
		return m_routingSettings;
	}

	/**
	 * Sets whether the forwarding state module shall be visible within the configuration
	 * dialog.
	 *
	 * @param a_moduleVisible True, if the forwarding state module shall be visible, false
	 *                        otherwise.
	 */
	public void setForwardingStateModuleVisible(boolean a_moduleVisible)
	{
		m_forwardingStateModuleVisible = a_moduleVisible;
	}

	/**
	 * Returns whether the forwarding state module shall be visible within the configuration
	 * dialog.
	 *
	 * @return True, if the forwarding state module shall be visible, false otherwise.
	 */
	public boolean isForwardingStateModuleVisible()
	{
		return m_forwardingStateModuleVisible;
	}

	public static int getTorMaxConnectionsPerRoute()
	{
		return ms_TheModel.m_TorMaxConnectionsPerRoute;
	}

	protected void setTorMaxConnectionsPerRoute(int i)
	{
		m_TorMaxConnectionsPerRoute = i;
	}

	public static int getTorMaxRouteLen()
	{
		return ms_TheModel.m_TorMaxRouteLen;
	}

	protected void setTorMaxRouteLen(int i)
	{
		m_TorMaxRouteLen = i;
	}

	public static int getTorMinRouteLen()
	{
		return ms_TheModel.m_TorMinRouteLen;
	}

	protected void setTorMinRouteLen(int i)
	{
		m_TorMinRouteLen = i;
	}

	public static boolean isTorNoneDefaultDirServerEnabled()
	{
		return ms_TheModel.m_bTorUseNoneDefaultDirServer;
	}

	protected void setTorUseNoneDefaultDirServer(boolean b)
	{
		m_bTorUseNoneDefaultDirServer=b;
	}

	protected void setMixminionRouteLen(int i)
	{
		m_mixminionRouteLen = i;
	}

	public static int getMixminionRouteLen()
	{
		return ms_TheModel.m_mixminionRouteLen;
	}

	protected void setMixminionMyEMail(String address)
	{
		m_mixminionMyEMail = address;
	}

	public static String getMixminionMyEMail()
	{
		return ms_TheModel.m_mixminionMyEMail;
	}

	protected void setMixMinionPassword(String pw)
	{
		m_mixminionPassword = pw;
	}
	public static String getMixMinionPassword()
	{
		return ms_TheModel.m_mixminionPassword;
	}

	protected void setMixinionPasswordHash(byte[] hash)
	{
		m_mixminionPasswordHash = hash;
	}

	public static byte[] getMixMinionPasswordHash()
	{
		return ms_TheModel.m_mixminionPasswordHash;
	}
	protected void resetMixMinionKeyringandPw()
	{
		m_mixminionPasswordHash = null;
		m_mixminionPassword = null;
		m_mixminionKeyring = "";
	}
	protected void setMixminionMessages(Vector m)
	{
		m_mixminionMessages = m;
	}
	public static Vector getMixminionMessages()
	{
		return ms_TheModel.m_mixminionMessages;
	}

	protected void setMixminionKeyring(String kr)
	{
		m_mixminionKeyring = kr;
	}

	public static String getMixminionKeyring()
	{
		return ms_TheModel.m_mixminionKeyring;
	}

	protected void setMixminionMMRList(MMRList m)
	{
		m_mixminionRouters = m;
	}

	public static MMRList getMixminionMMRlist()
	{
		return ms_TheModel.m_mixminionRouters;
	}

	protected void setMixminionFragments(Vector f)
	{
		m_mixminionFragments = f;
	}

	public static Vector getMixminionFragments()
	{
		return ms_TheModel.m_mixminionFragments;
	}


	protected void setUseProxyAuthentication(boolean a_bUseAuth)
	{
		m_bUseProxyAuthentication = a_bUseAuth;
	}

	public boolean isProxyAuthenticationUsed()
	{
		return m_bUseProxyAuthentication;
	}

	public void setPaymentPassword(String a_password)
	{
		m_paymentPassword = a_password;
	}

	public String getPaymentPassword()
	{
		return m_paymentPassword;
	}

	public synchronized String getHelpPath()
	{
		return (m_helpPath != null || m_bPortableHelp) ?
				m_helpPath : AbstractOS.getInstance().getDefaultHelpPath(
						JAPConstants.APPLICATION_CONFIG_DIR_NAME);
		
	}
	
	public synchronized URL getHelpURL(String a_startDoc)
	{
		URL helpURL = null;
		if (a_startDoc != null && isHelpPathDefined() && 
			m_helpFileStorageManager.ensureMostRecentVersion(m_helpPath))
		{
			try 
			{
				if (new File(m_helpPath + File.separator + 
					m_helpFileStorageManager.getLocalisedHelpDir() + 
					File.separator + a_startDoc).exists())
				{
					helpURL = new URL("file://" + m_helpPath + "/" +
								m_helpFileStorageManager.getLocalisedHelpDir() 
								+ "/" + a_startDoc);
				}
			}
			catch (SecurityException a_e)
			{
				LogHolder.log(LogLevel.WARNING, LogType.MISC, a_e);
			}
			catch (MalformedURLException e) 
			{
				LogHolder.log(LogLevel.WARNING, LogType.MISC, e);
			}
		}
		return helpURL;
	}
	
	public URL getHelpURL()
	{
		return getHelpURL("index.html");
	}
	
	synchronized void initHelpPath(String helpPath)
	{
		String blockedPath;
		
		if (m_bPortableHelp)
		{
			return;
		}
		
		/** TODO remove after some months; created on 2008-08-17 */
		blockedPath = AbstractOS.getInstance().getenv("ALLUSERSPROFILE");
		if (blockedPath != null && helpPath != null && helpPath.startsWith(blockedPath))
		{
			if (helpPath.indexOf(JAPConstants.APPLICATION_CONFIG_DIR_NAME) >= 0)
			{
				RecursiveFileTool.deleteRecursion(new File(helpPath));
			}
			
			helpPath = null;
		}
		
		String initPathValidity = (helpPathValidityCheck(helpPath));
		
		if( initPathValidity.equals(AbstractHelpFileStorageManager.HELP_VALID) || 
			initPathValidity.equals(AbstractHelpFileStorageManager.HELP_JONDO_EXISTS) ||
			initPathValidity.equals(NO_HELP_STORAGE_MANAGER))
		{
			m_helpPath = helpPath;
		}
		else
		{		
			m_helpPath = m_helpFileStorageManager.getInitPath();
		}
	}
	
	public synchronized void setHelpPath(File hpFile)
	{
		setHelpPath(hpFile, false);
	}
	
	public synchronized void setHelpPath(File hpFile, boolean a_bPortable)
	{	
		String strCheck;
		
		if (m_bPortableHelp && !a_bPortable)
		{
			return;
		}
		
		if (hpFile == null)
		{
			resetHelpPath();
		}
		else
		{
			hpFile = new File(hpFile.getAbsolutePath());
			if (a_bPortable)
			{
				m_bPortableHelp = true;
				if (hpFile.isFile())
				{
					/* This is for backwards compatibility with old portable
					 * launchers for Windows. The xml file check is disabled for this
					 * kind of installation. 
					 */				
					int index;
					if ((index = hpFile.getPath().toUpperCase().indexOf((
							AbstractHelpFileStorageManager.HELP_FOLDER + File.pathSeparator + "de" + 
							File.pathSeparator + AbstractHelpFileStorageManager.HELP_FOLDER).toUpperCase())) >= 0 ||
							(index = hpFile.getPath().toUpperCase().indexOf((
									AbstractHelpFileStorageManager.HELP_FOLDER + File.pathSeparator + "en" +
									File.pathSeparator + AbstractHelpFileStorageManager.HELP_FOLDER).toUpperCase())) >= 0)
					{
						if (index > 0)
						{
							hpFile = new File(hpFile.getPath().substring(0, index));
						}
						else
						{
							hpFile = null;
						}
					}
					else
					{
	//					get the parent directory as help path
						String tmp = hpFile.getParent();
						if (tmp != null)
						{
							hpFile = new File(tmp);
						}
						else
						{
							hpFile = null;
						}
					}	
				}
				
				if (hpFile != null && hpFile.isDirectory())										
				{				
					strCheck = m_helpFileStorageManager.helpPathValidityCheck(hpFile.getPath(), true);
					if (strCheck.equals(AbstractHelpFileStorageManager.HELP_VALID) ||
						strCheck.equals(AbstractHelpFileStorageManager.HELP_JONDO_EXISTS))
					{						
						//delete old help directory if it exists and create a new one
						if (m_helpFileStorageManager.handleHelpPathChanged(
								m_helpPath, hpFile.getPath(), true))
						{
							if (m_helpPath == null || !m_helpPath.equals(hpFile.getPath()))
							{
								m_helpPath = hpFile.getPath();
								setChanged();
							}							
						}
						else
						{							
							resetHelpPath();
							LogHolder.log(LogLevel.WARNING, LogType.GUI, 
								"Help path resetted because we could not change it.");
						}
					}
					else
					{	
						resetHelpPath();
						LogHolder.log(LogLevel.WARNING, LogType.GUI, 
							"Help path resetted because it was invalid.");
					}
				}
				else
				{
					resetHelpPath();
					LogHolder.log(LogLevel.WARNING, LogType.GUI, 
						"Help path resetted because it was no directory.");
				}
			}
			else
			{
				if (hpFile.getPath().toUpperCase().endsWith(AbstractHelpFileStorageManager.HELP_FOLDER.toUpperCase()) &&
					hpFile.getParent() != null)
				{					
					File file = new File(hpFile.getParent()); 
					if (file.isDirectory())
					{
						hpFile = file;
					}
				}				
				setHelpPath(hpFile.getPath());
			}
		}
		
		notifyObservers(CHANGED_HELP_PATH);		
	}
	
	private synchronized void setHelpPath(String newHelpPath)
	{
		String strCheck;
		
		if(newHelpPath == null)
		{
			resetHelpPath();
			return;
		}
		if(newHelpPath.equals(""))
		{
			resetHelpPath();
			return;
		}
		
		strCheck = helpPathValidityCheck(newHelpPath);
		if (strCheck.equals(AbstractHelpFileStorageManager.HELP_VALID) ||
			strCheck.equals(AbstractHelpFileStorageManager.HELP_JONDO_EXISTS))
		{
			String oldHelpPath = m_helpPath;
			boolean helpPathChanged =
						isHelpPathDefined() ? !m_helpPath.equals(newHelpPath) : true;
			
			if(helpPathChanged)
			{
				boolean storageLayerChanged = true;
				storageLayerChanged = m_helpFileStorageManager.handleHelpPathChanged(
						oldHelpPath, newHelpPath, false);
				
				if(storageLayerChanged)
				{					
					m_helpPath = newHelpPath;
					setChanged();
				}
			}
		}
	}
	
	public boolean extractHelpFiles(String a_extractionPath)
	{
		return m_helpFileStorageManager.extractHelpFiles(a_extractionPath);
	}
	
	protected synchronized void resetHelpPath()
	{
		String oldHelpPath = m_helpPath;
		
		if(oldHelpPath != null && !m_bPortableHelp)
		{
			m_helpFileStorageManager.handleHelpPathChanged(oldHelpPath, null, false);
			setChanged();
			m_helpPath = null;
		}	
	}
	
	/**
	 * performs a validity check whether the specified path is a valid 
	 * path for external installation of the help files.
	 * @param helpPath the path of the parent directory where the help files should be installed 
	 * @return a string that signifies a valid path or a key for a corresponding error message otherwise
	 */
	public synchronized String helpPathValidityCheck(String helpPath)
	{
		return m_helpFileStorageManager.helpPathValidityCheck(helpPath, false);		
	}
	
	/**
	 * performs a validity check whether the specified path is a valid 
	 * path for external installation of the help files.
	 * @param hpFile the parent directory where the help files should be installed 
	 * @return a string that signifies a valid path or a key for a corresponding error message otherwise
	 */
	public synchronized String helpPathValidityCheck(File hpFile)
	{
		if(hpFile == null)
		{
			return JAPMessages.getString(AbstractHelpFileStorageManager.HELP_INVALID_NULL);
		}
		return helpPathValidityCheck(hpFile.getPath());
	}
	
	public boolean isHelpPathChangeable()
	{
		if (m_helpFileStorageManager instanceof LocalHelpFileStorageManager)
		{
			return false;
		}
		if (m_bPortableHelp)
		{
			return false;
		}
		return true;
	}
	
	/**
	 * checks if a help Path is defined and a
	 * valid help file installation can be found there.
	 * @return true if and only if a help path not null is defined and 
	 * 			comprises a valid help file installation
	 */
	public synchronized boolean isHelpPathDefined()
	{			
		boolean helpPathExists = m_helpPath != null;
		
		boolean bInstallationExists;
		String strInstallationValid = null;
		
		/* if no storageManager is defined: don't check if installation exists */
		boolean helpInstallationExists = 
			(bInstallationExists = m_helpFileStorageManager.helpInstallationExists(m_helpPath)) &&
			((strInstallationValid = helpPathValidityCheck(m_helpPath)).equals(AbstractHelpFileStorageManager.HELP_JONDO_EXISTS));
		
		if (helpPathExists && !helpInstallationExists)
		{
			LogHolder.log(LogLevel.WARNING, LogType.MISC, "Help path " + m_helpPath + 
					" configured but no valid help could be found! Exists: " + bInstallationExists +
					" Valid: " + strInstallationValid);
			m_helpPath = null;
			setChanged();
		}
		
		if (!m_bPortableHelp && m_helpPath == null 
				&& m_helpFileStorageManager.helpInstallationExists(
				AbstractOS.getInstance().getDefaultHelpPath(JAPConstants.APPLICATION_CONFIG_DIR_NAME)) &&
				helpPathValidityCheck(AbstractOS.getInstance().getDefaultHelpPath(JAPConstants.APPLICATION_CONFIG_DIR_NAME)).equals(
						AbstractHelpFileStorageManager.HELP_JONDO_EXISTS))
		{
			m_helpPath = AbstractOS.getInstance().getDefaultHelpPath(JAPConstants.APPLICATION_CONFIG_DIR_NAME);
			helpInstallationExists = true;
			setChanged();
		}
		
		notifyObservers(CHANGED_HELP_PATH);
			
		return helpInstallationExists;							
	}
	
	public Observable getHelpFileStorageObservable()
	{
		return m_helpFileStorageManager.getStorageObservable();
	}
	
	public synchronized void setDLLupdate(String a_dllUpdatePath)
	{
		if (a_dllUpdatePath != null && 
			(m_bDllUpdatePath == null || !m_bDllUpdatePath.equals(a_dllUpdatePath)))
		{
			File file = new File(a_dllUpdatePath);
			if (file.exists() && file.isDirectory())
			{
				m_bDllUpdatePath = file.getAbsolutePath();
				setChanged();
			}			
		}
		else if (a_dllUpdatePath == null && m_bDllUpdatePath != null)
		{
			m_bDllUpdatePath = null;
			setChanged();
		}
		notifyObservers(CHANGED_DLL_UPDATE);
    }
	
	public synchronized void setMacOSXLibraryUpdateAtStartupNeeded(boolean a_update)
	{
		if (m_bMacOSXLibraryUpdateAtStartupNeeded != a_update)
		{
			m_bMacOSXLibraryUpdateAtStartupNeeded = a_update;
			setChanged();
		}
		notifyObservers(CHANGED_MACOSX_LIBRARY_UPDATE);
	}
	
	public synchronized void setAnonymizedHttpHeaders(boolean a_update)
	{
		if(m_bAnonymizedHttpHeaders != a_update)
		{
			m_bAnonymizedHttpHeaders = a_update;
			setChanged();
		}
		notifyObservers(CHANGED_ANONYMIZED_HTTP_HEADERS);
	}
	
	public boolean isAnonymizedHttpHeaders()
	{
		return m_bAnonymizedHttpHeaders;
	}
	
	public boolean isMacOSXLibraryUpdateAtStartupNeeded()
	{
		return m_bMacOSXLibraryUpdateAtStartupNeeded;
	}
	
	public String getDllUpdatePath() 
	{
		return m_bDllUpdatePath;
	}
	
	
	
	public synchronized void setDllWarning(boolean a_bWarn)
	{
		String version = JAPDll.JAP_DLL_REQUIRED_VERSION; 
			//JAPDll.getDllVersion();
		long newValue = m_noWarningForDllVersionBelow;
		if (a_bWarn)
		{
			newValue = 0;
		}
		else if (version != null)
		{
			newValue = Util.convertVersionStringToNumber(version);
		}
		
		if (m_noWarningForDllVersionBelow != newValue)
		{
			m_noWarningForDllVersionBelow = newValue;
			setChanged();
		}
		notifyObservers(CHANGED_DLL_UPDATE);
	}
	
	protected synchronized void setDllWarningVersion(long a_noWarningForDllVersionBelow)
	{
		if (m_noWarningForDllVersionBelow != a_noWarningForDllVersionBelow)
		{
			m_noWarningForDllVersionBelow = a_noWarningForDllVersionBelow;
			setChanged();
		}
		notifyObservers(CHANGED_DLL_UPDATE);
	}
	
	protected long getDLLWarningVersion()
	{
		return m_noWarningForDllVersionBelow;
	}
	
	public boolean isDLLWarningActive()
	{
		long currentVersion = Util.convertVersionStringToNumber(JAPDll.JAP_DLL_REQUIRED_VERSION);

		if (m_noWarningForDllVersionBelow == currentVersion)
		{
			return false;
		}		
		
		return true;
	}

	public void setShowSplashScreen(boolean a_bHide)
	{
		m_bShowSplashScreen = a_bHide;
	}
	
	public boolean getShowSplashScreen()
	{
		return m_bShowSplashScreen;
	}
	
	public void setShowSplashDisabled(boolean a_bDisabled)
	{
		m_bShowSplashDisabled = a_bDisabled;
	}
	
	public boolean getShowSplashDisabled()
	{
		return m_bShowSplashDisabled;
	}
	
	public void setStartPortableFirefox(boolean a_bStart)
	{
		m_bStartPortableFirefox = a_bStart;
	}
	
	public boolean getStartPortableFirefox()
	{
		return m_bStartPortableFirefox;
	}

	public static class FontResize
	{
		private int m_oldSize;
		private int m_newSize;

		public FontResize(int a_oldSize, int a_newSize)
		{
			m_oldSize = a_oldSize;
			m_newSize = a_newSize;
		}
		public int getOldSize()
		{
			return m_oldSize;
		}
		public int getNewSize()
		{
			return m_newSize;
		}
	}

	public boolean isShuttingDown()
	{
		return JAPController.getInstance().isShuttingDown();
	}

	private IProxyInterfaceGetter getProxyInterface(
		   int a_component, boolean a_bAnonInterface)
	{
		if (isShuttingDown())
		{
			return null;
		}

		IProxyInterfaceGetter proxyDirect, proxyAnon;
		proxyDirect = new IProxyInterfaceGetter()
		{
			public ImmutableProxyInterface getProxyInterface()
			{
				// try direct connection via proxy, if present
				return JAPModel.getInstance().getProxyInterface();
			}
		};
		proxyAnon = new IProxyInterfaceGetter()
		{
			public ImmutableProxyInterface getProxyInterface()
			{
				synchronized (SYNC_ANON_PROXY)
				{
					if (m_proxyAnon == null || m_proxyAnon.getPort() != getHttpListenerPortNumber())
					{
						InetAddress proxyListenerAddress = JAPController.getInstance().getListenerInetAddress();
						if(proxyListenerAddress != null)
						{
							String hostAddress = proxyListenerAddress.getHostAddress();
							if(proxyListenerAddress.getHostAddress().equals(JAPConstants.IN_ADDR_ANY_IPV4))
							{
								try 
								{
									hostAddress = InetAddress.getLocalHost().getHostAddress();
								} 
								catch (UnknownHostException e)
								{
									hostAddress = JAPConstants.IN_ADDR_LOOPBACK_IPV4;
								}
								
							}
							m_proxyAnon = new ProxyInterface(hostAddress , getHttpListenerPortNumber(), null);
						}
					}
				}
				return m_proxyAnon; // AN.ON
			}
		};

		//interfaces[3] = new ProxyInterface("localhost", getHttpListenerPortNumber(),
			//							   ProxyInterface.PROTOCOL_TYPE_SOCKS, null); // TOR
		if ((DIRECT_CONNECTION_PAYMENT == a_component && 
				m_iPaymentAnonymousConnectionSetting == CONNECTION_FORCE_ANONYMOUS) ||
			(DIRECT_CONNECTION_INFOSERVICE == a_component && 
				m_iInfoServiceAnonymousConnectionSetting == CONNECTION_FORCE_ANONYMOUS) ||
			(DIRECT_CONNECTION_UPDATE == a_component && 
				m_iUpdateAnonymousConnectionSetting == CONNECTION_FORCE_ANONYMOUS))
		{
			// force anonymous connections to BI and InfoService
			if (!m_connectionChecker.checkAnonConnected())
			{
				// no anonymous connection available... it is not possible to connect!
				return null;
			}
			// ok, there seems to be an anonymous channel
			if (a_bAnonInterface)
			{
				return proxyAnon;
			}
			// A direct proxy was requested; not allowed!
			return null;

		}
		else if (!m_connectionChecker.checkAnonConnected())
		{
			if (a_bAnonInterface)
			{
				// no anonymous connection is available
				return null;
			}
			return proxyDirect;
		}

		// both proxies are available
		if (a_bAnonInterface)
		{
			if ((DIRECT_CONNECTION_PAYMENT == a_component && 
					CONNECTION_BLOCK_ANONYMOUS == m_iPaymentAnonymousConnectionSetting) ||
				(DIRECT_CONNECTION_INFOSERVICE == a_component && 
				 CONNECTION_BLOCK_ANONYMOUS == m_iInfoServiceAnonymousConnectionSetting) ||
				 (DIRECT_CONNECTION_UPDATE == a_component && 
					CONNECTION_BLOCK_ANONYMOUS == m_iUpdateAnonymousConnectionSetting))
			{
				// Anonymous connection is not allowed!
				return null;
			}
			else
			{
				return proxyAnon;
			}
		}
		return proxyDirect;
	}

	public BigInteger getDialogVersion()
		{
			return m_iDialogVersion;
		}

		public void setDialogVersion(BigInteger dialogVersion)
		{
			m_iDialogVersion = dialogVersion;
		}
		
	public Hashtable getAcceptedTCs()
	{
		return m_acceptedTCs;
	}

	public String getContext() 
	{
		return m_context;
	}
	
	public String getProgramName()
	{
		return m_strDistributorMode;
	}
	
	public void setProgramName(String a_programName)
	{
		if (a_programName != null && 
			(a_programName.equals(JAPConstants.PROGRAM_NAME_JAP) || 
				a_programName.equals(JAPConstants.PROGRAM_NAME_JONDO)))
		{
			m_strDistributorMode = a_programName;
		}
	}
	
	public synchronized void setContext(String context) 
	{
		// did not work with an observer, I do not know why...
		TrustModel.updateContext(context);
		
	}
}
