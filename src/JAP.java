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

/** Project Web Mixes: JAP
 *
 *  The class JAP can be inherited by another class
 *  in order to implement system specific stuff, e.g.
 *  on a Macintosh to register the MRJ Handler.
 *
 */
import java.io.File;
import java.net.URL;
import java.security.SecureRandom;
import java.util.Hashtable;
import java.util.Locale;
import java.util.Vector;

import java.awt.Frame;

import anon.client.crypto.KeyPool;
import anon.infoservice.ListenerInterface;
import anon.infoservice.MixCascade;
import anon.util.ClassUtil;
import gui.GUIUtils;
import gui.JAPAWTMsgBox;
import gui.JAPDll;
import gui.JAPMessages;
import gui.dialog.JAPDialog;
import jap.AbstractJAPMainView;
import jap.ConsoleJAPMainView;
import jap.ConsoleSplash;
import jap.IJAPMainView;
import jap.ISplashResponse;
import jap.JAPConstants;
import jap.JAPController;
import jap.JAPDebug;
import jap.JAPModel;
import jap.JAPNewView;
import jap.JAPSplash;
import jap.JAPViewIconified;
import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;
import platform.AbstractOS;

/** This is the main class of the JAP project. It starts everything. It can be inherited by another
 *  class that wants to initialize platform dependend features, e.g. see
 *  <A HREF="JAPMacintosh.html">JAPMacintosh.html</A>
 *  as an example.
 */
public class JAP
{
	private static final String MSG_ERROR_NEED_NEWER_JAVA = "errorNeedNewerJava";
	private static final String MSG_GNU_NOT_COMPATIBLE = JAP.class.getName() + "_gnuNotCompatible";
	private static final String MSG_LOADING_INTERNATIONALISATION = JAP.class.getName() +
		"_loadingInternationalisation";
	private static final String MSG_LOADING_SETTINGS = JAP.class.getName() +
		"_loadingSettings";
	private static final String MSG_STARTING_CONTROLLER = JAP.class.getName() +
		"_startingController";
	private static final String MSG_INIT_DLL = JAP.class.getName() + "_initLibrary";
	private static final String MSG_INIT_VIEW = JAP.class.getName() + "_initView";
	private static final String MSG_INIT_ICON_VIEW = JAP.class.getName() + "_initIconView";
	private static final String MSG_INIT_RANDOM = JAP.class.getName() + "_initRandom";
	private static final String MSG_FINISH_RANDOM = JAP.class.getName() + "_finishRandom";
	private static final String MSG_START_LISTENER = JAP.class.getName() + "_startListener";
	
	private JAPController m_controller;
	
	Hashtable m_arstrCmdnLnArgs = null;
	String[] m_temp = null;
	String[] m_firefoxCommand; //holds command to re-open firefox, to be parsed from args and passed to JAPNewView

	public JAP()
	{
	}

	/** Constructor for the JAP object.
	 * @param argv The commandline arguments.
	 */
	JAP(String[] argv)
	{
		m_temp = argv;
		if (argv != null)
		{
			if (argv.length > 0)
			{
				m_arstrCmdnLnArgs = new Hashtable(argv.length);
			}
			else
			{
				m_arstrCmdnLnArgs = new Hashtable();
			}
			for (int i = 0; i < argv.length; i++)
			{
				if (i + 1 < argv.length && !argv[i + 1].startsWith("-"))
				{
					// this option has an argument
					m_arstrCmdnLnArgs.put(argv[i], argv[i + 1]);
				}
				else
				{
					m_arstrCmdnLnArgs.put(argv[i], "");
				}
			}
		}
		else
		{
			m_arstrCmdnLnArgs = new Hashtable();
		}
	}

	/** Initializes and starts the JAP.
	 */
	public void startJAP()
	{
		final String msg =
			"JAP/JonDo must run with a 1.1.3 or higher version Java!\nYou will find more information at the JAP webpage!\nYour Java Version: ";
		String javaVersion = System.getProperty("java.version");
		String vendor = System.getProperty("java.vendor");
		String os = System.getProperty("os.name");
		String mrjVersion = System.getProperty("mrj.version");
		boolean bConsoleOnly = false;
		boolean loadPay = true;
		String listenHost = null;
		int listenPort = 0;
		ListenerInterface listenerCascade = null;

		if (isArgumentSet("--version") || isArgumentSet("-v"))
		{
			System.out.println("JAP/JonDo version: " + JAPConstants.aktVersion + "\n" +
							   "Java Vendor: " + vendor + "\n" +
							   "Java Version: " + javaVersion + "\n");
			System.exit(0);
		}

		if (!JAPConstants.m_bReleasedVersion)
		{
			System.out.println("Starting up JAP/JonDo version " + JAPConstants.aktVersion + ". (" + javaVersion +
							   "/" + vendor + "/" + os +
							   (mrjVersion != null ? "/" + mrjVersion : "") + ")");
		}
		//Macintosh Runtime for Java (MRJ) on Mac OS
		// Test (part 1) for right JVM
		if (javaVersion.compareTo("1.0.2") <= 0)
		{
			System.out.println(msg + javaVersion);
			System.exit(0);
		}

		if (isArgumentSet("--help") || isArgumentSet("-h"))
		{
			System.out.println("Usage:");
			System.out.println("--help, -h:                  Show this text.");
			System.out.println("--console:                   Start JAP/JonDo in console-only mode.");
			System.out.println("--allow-multiple, -a         Allow JAP to start multiple instances.");
			System.out.println("--minimized, -m:             Minimize JAP/JonDo on startup.");
			System.out.println("--version, -v:               Print version information.");
			System.out.println("--showDialogFormat           Show and set dialog format options.");
			System.out.println("--noSplash, -s               Suppress splash screen on startup.");
			System.out.println("--presenation, -p            Presentation mode (slight GUI changes).");
			System.out.println("--forwarder, -f {port}       Act as a forwarder on a specified port.");
			System.out.println("--listen, -l {[host][:port]} Listen on the specified interface.");
			System.out.println("--cascade {[host][:port]}    Connects to the specified Mix-Cascade.");
			System.out.println("--portable [path_to_browser] Tell JonDo that it runs in a portable environment.");
			System.out.println("--portable-browserprofile [profile] Path to the portable browser profile,");
			System.out.println("                             if not detected automatically.");
			System.out.println("--portable-jre               Tell JonDo that it runs with a portable JRE.");
			System.out.println("--portable-help-path         Path of external html help files for portable use.");
			System.out.println("--config, -c {Filename}:     Force JonDo to use a specific configuration file.");
			System.exit(0);
		}

		if (isArgumentSet("-console") || isArgumentSet("--console"))
		{
			bConsoleOnly = true;
		}

		if(!isArgumentSet("--allow-multiple") && !isArgumentSet("-a"))
		{
			// Try to detect running instances of JAP
			Vector activeVMs = AbstractOS.getInstance().getActiveVMs();
			Object vm;
			int numJAPInstances = 0;
			for(int i = 0; i < activeVMs.size(); i++)
			{
				vm = activeVMs.elementAt(i);
				if(vm == null || vm.toString() == null ) continue;
				if(vm.toString().equals("JAP") || vm.toString().equals("JAP.jar") || vm.toString().equals("JAPMacintosh")) numJAPInstances++;
				if(numJAPInstances > 1)
				{
					// multiple instances of JAP have been started, what to do?
					System.out.println("There is already an instance of JAP/JonDo running.");
					System.exit(0);
				}
			}
		}

		// Test (part 2) for right JVM....
		if (vendor.startsWith("Transvirtual"))
		{ // Kaffe

			//if (javaVersion.compareTo("1.0.5") <= 0)
			if (javaVersion.compareTo("1.3") <= 0)
			{
				JAPMessages.init(JAPConstants.MESSAGESFN);
				if (bConsoleOnly)
				{
					System.out.println(JAPMessages.getString(MSG_ERROR_NEED_NEWER_JAVA));
				}
				else
				{
					JAPAWTMsgBox.MsgBox(
						new Frame(),
						JAPMessages.getString(MSG_ERROR_NEED_NEWER_JAVA),
						JAPMessages.getString("error"));
				}
				System.exit(0);
			}
		}
		else if (vendor.toUpperCase().indexOf("FREE SOFTWARE FOUNDATION") >= 0)
		{
			JAPMessages.init(JAPConstants.MESSAGESFN);
			// latest version reported not to run: 1.4.2, Free Software Foundation Inc.
			System.out.println("\n" + JAPMessages.getString(MSG_GNU_NOT_COMPATIBLE) + "\n");
			//System.exit(0);
		}
		else
		{
			if (javaVersion.compareTo("1.0.2") <= 0)
			{
				System.out.println(msg + javaVersion);
				System.exit(0);
			}
			if (javaVersion.compareTo("1.1.2") <= 0)
			{
				JAPMessages.init(JAPConstants.MESSAGESFN);
				if (bConsoleOnly)
				{
					System.out.println(JAPMessages.getString(MSG_ERROR_NEED_NEWER_JAVA));
				}
				else
				{
					JAPAWTMsgBox.MsgBox(
						new Frame(),
						JAPMessages.getString(MSG_ERROR_NEED_NEWER_JAVA),
						JAPMessages.getString("error"));
				}
				System.exit(0);
			}
		}
		
		// init controller and preload config
		m_controller = JAPController.getInstance();
		
		// Set path to Firefox for portable JAP
		//String firepath="";
		String profilepath = "";
		boolean bPortable = isArgumentSet("--portable");
		m_controller.setPortableMode(bPortable);
		
		String configFileName = null;
		/* check, whether there is the -config parameter, which means the we use userdefined config
		 * file
		 */
		if ( (configFileName = getArgumentValue("--config")) == null)
		{
			configFileName = getArgumentValue("-c");
		}
		if (configFileName == null && bPortable)
		{
			// load and create the config file in the current directory by default
			File tempDir = ClassUtil.getClassDirectory(JAP.class);
			if (tempDir != null)
			{
				configFileName =
					ClassUtil.getClassDirectory(JAP.class).getParent() +
					File.separator + JAPConstants.XMLCONFFN;
			}
		}

		if (configFileName != null)
		{
			LogHolder.log(LogLevel.NOTICE, LogType.MISC, "Loading config file '" + configFileName + "'.");
		}
		
		m_controller.preLoadConfigFile(configFileName);
		// Show splash screen
		ISplashResponse splash;
		String splashText;
		Locale defaultLocale = Locale.getDefault();
		// splashText = JAPMessages.getString(MSG_LOADING_INTERNATIONALISATION);
		if (defaultLocale.getLanguage().equals("de"))
		{
			splashText = "Lade Internationalisierung";
		}
		else if (defaultLocale.getLanguage().equals("fr"))
		{
			splashText = "Chargement des param\u00e8tres d'internationalisation";
		}
		else if (defaultLocale.getLanguage().equals("cs"))
		{
			splashText = "Nahr\u00E1v\u00E1m internacionalizaci";
		}

		else
		{
			splashText = "Loading internationalisation";
		}

		if (bConsoleOnly)
		{
			JAPDialog.setConsoleOnly(true);
			splash = new ConsoleSplash();
			splash.setText(splashText);
		}
		else if (isArgumentSet("--noSplash") || isArgumentSet("-s") || !JAPModel.getInstance().getShowSplashScreen())
		{
			splash = new ConsoleSplash();
			splash.setText(splashText);
		}
		else
		{
			Frame hidden = new Frame();
			splash = new JAPSplash(hidden, splashText);
			( (JAPSplash) splash).centerOnScreen();
			( (JAPSplash) splash).setVisible(true);
			GUIUtils.setAlwaysOnTop( ( (JAPSplash) splash), true);
		}

		// Init Messages....
		if (!JAPMessages.isInitialised())
		{
			JAPMessages.init(JAPConstants.MESSAGESFN);
		}

		if (!bConsoleOnly)
		{
			JAPModel.getInstance().setDialogFormatShown(isArgumentSet("--showDialogFormat"));

			GUIUtils.setIconResizer(JAPModel.getInstance().getIconResizer());
			// Test for Swing
			try
			{
				Object o = new javax.swing.JLabel();
				o = null;
			}
			catch (NoClassDefFoundError e)
			{
				JAPAWTMsgBox.MsgBox(
					new Frame(),
					JAPMessages.getString("errorSwingNotInstalled"),
					JAPMessages.getString("error"));
				System.exit(0);
			}
		}
		// Create debugger object and set the LogHolder to JAPDebug
		LogHolder.setLogInstance(JAPDebug.getInstance());
		JAPDebug.getInstance().setLogType(LogType.ALL);
		JAPDebug.getInstance().setLogLevel(LogLevel.WARNING);

		splash.setText(JAPMessages.getString(MSG_INIT_RANDOM));
		// initialise secure random generators
		Thread secureRandomThread = new Thread(new Runnable()
		{
			public void run()
			{
				KeyPool.start();
				new SecureRandom().nextInt();
			}
		});
		secureRandomThread.setPriority(Thread.MIN_PRIORITY);
		secureRandomThread.start();

		//splash.setText(JAPMessages.getString(MSG_FINISH_RANDOM));
		try
		{
			secureRandomThread.join();
		}
		catch (InterruptedException a_e)
		{
			LogHolder.log(LogLevel.NOTICE, LogType.CRYPTO, a_e);
		}

		// Set the default Look-And-Feel
		if (!bConsoleOnly && !os.regionMatches(true, 0, "mac", 0, 3))
		{
			LogHolder.log(LogLevel.DEBUG, LogType.GUI, "Setting Cross Platform Look-And-Feel!");
			try
			{
				javax.swing.UIManager.setLookAndFeel(
					javax.swing.UIManager.getCrossPlatformLookAndFeelClassName());
			}
			catch (Exception e)
			{
				LogHolder.log(LogLevel.EXCEPTION, LogType.GUI,
							  "Exception while setting Cross Platform Look-And-Feel!");
			}
		}
		//deactivate socks proxy settings if given by the os
		/*		try
		  {
		   Properties p = System.getProperties();
		   boolean changed = false;
		   if (p.containsKey("socksProxyHost"))
		   {
		 System.out.println("Found sosckProxyHost");
		 p.remove("socksProxyHost");
		 changed = true;
		   }
		   if (p.containsKey("socksProxyPort"))
		   {
		 p.remove("socksProxyPort");
		 changed = true;
		   }
		   if (changed)
		   {

		 p.list(System.out);
		 System.setProperties(p);
		 //System.setProperty("socksProxyHost","hallo");
		 System.out.println("removed socks settings");
		 Socket.setSocketImplFactory(null);
		   }
		  }
		  catch (Throwable t)
		  {
		   t.printStackTrace();
		   LogHolder.log(
		 LogLevel.EXCEPTION,
		 LogType.NET,
		 "JAP:Exception while trying to deactivate SOCKS proxy settings: " + t.getMessage());
		  }
		 */

		// parse any given listener
		if (isArgumentSet("--listen") || isArgumentSet("-l"))
		{
			if ( (listenHost = getArgumentValue("--listen")) == null)
			{
				listenHost = getArgumentValue("-l");
			}
			if (listenHost != null)
			{
				try
				{
					ListenerInterface l = new ListenerInterface(listenHost);
					listenHost = l.getHost();
					listenPort = l.getPort();
				}
				catch (Throwable t)
				{
				}
			}
		}

		// parse any given Mix-Cascade
		if (isArgumentSet("--cascade"))
		{
			String tmpStr = getArgumentValue("--cascade");
			try
			{
				listenerCascade = new ListenerInterface(tmpStr);
			}
			catch (Throwable t)
			{
			}
		}



		// Create the controller object
		splash.setText(JAPMessages.getString(MSG_STARTING_CONTROLLER));
		m_controller.start();
		if (isArgumentSet("--presentation") || isArgumentSet("-p"))
		{
			m_controller.setPresentationMode(true);
		}
		String cmdArgs = "";
		if (m_temp != null)
		{
			for (int i = 0; i < m_temp.length; i++)
			{
				cmdArgs += " " + m_temp[i];
			}
			m_controller.setCommandLineArgs(cmdArgs);
		}

		

		if (isArgumentSet("--portable-jre"))
		{
			m_controller.setPortableJava(true);
		}

		/* check, whether there is the -forwarding_state parameter, which extends
		 * the configuration dialog
		 */
		boolean forwardingStateVisible = false;
		if (isArgumentSet("-forwarding_state"))
		{
			forwardingStateVisible = true;
		}

		JAPModel.getInstance().setForwardingStateModuleVisible(forwardingStateVisible);
		// load settings from config file
		splash.setText(JAPMessages.getString(MSG_LOADING_SETTINGS));
		m_controller.loadConfigFile(configFileName, loadPay, splash);
		// configure forwarding server
		String forwardingServerPort;
		if ( (forwardingServerPort = getArgumentValue("--forwarder")) == null)
		{
			forwardingServerPort = getArgumentValue("-f");
		}
		if (forwardingServerPort != null)
		{
			try
			{
				JAPModel.getInstance().getRoutingSettings().setServerPort(
					Integer.parseInt(forwardingServerPort));
			}
			catch (NumberFormatException a_e)
			{
				LogHolder.log(LogLevel.EXCEPTION, LogType.MISC, a_e);
			}
		}
		
		if(bPortable)
		{
			m_firefoxCommand = buildPortableFFCommand();
		}
		// keep this string unchangeable from "outside"
		final String[] firefoxCommand = m_firefoxCommand;
		AbstractOS.getInstance().init(new AbstractOS.IURLErrorNotifier()
		{
			boolean m_bReset = false;
			public void checkNotify(URL a_url)
			{
				if (a_url != null && !m_controller.getAnonMode() &&
					JAPModel.getInstance().isNonAnonymousSurfingDenied() &&
					a_url.toString().startsWith("https"))
				{
					m_bReset = true;
					JAPModel.getInstance().denyNonAnonymousSurfing(false);
				}
			}
		},new AbstractOS.IURLOpener()
		{
			public boolean openURL(URL a_url)
			{
				if (firefoxCommand == null || a_url == null)
				{
					// no path to portable browser was given; use default
					return false;
				}
				try
				{
					firefoxCommand[firefoxCommand.length-1] = a_url.toString();
					LogHolder.log(LogLevel.WARNING, LogType.GUI, firefoxCommand[0] + " " + a_url.toString());
					return m_controller.startPortableFirefox(firefoxCommand);
				}
				catch (Exception ex)
				{
					LogHolder.log(LogLevel.WARNING, LogType.GUI, "Error running applescript: ", ex);
				}
				return false;
			}
		});

		splash.setText(JAPMessages.getString(MSG_INIT_DLL));
		JAPDll.init();
		/*
		   if (splash instanceof JAPSplash)
		   {
		 hidden.setName(Double.toString(Math.random()));
		 hidden.setTitle(hidden.getName());
		 ( (JAPSplash) splash).setName(hidden.getName());

		 GUIUtils.setAlwaysOnTop( ( (JAPSplash) splash), true);
		   }*/
		// Output some information about the system
		LogHolder.log(LogLevel.INFO, LogType.MISC,
					  "Welcome! This is version " + JAPConstants.aktVersion + " of JAP.");
		LogHolder.log(LogLevel.INFO, LogType.MISC, "Java " + javaVersion + " running on " + os + ".");
		if (mrjVersion != null)
		{
			LogHolder.log(LogLevel.INFO, LogType.MISC, "MRJ Version is " + mrjVersion + ".");
		}

		splash.setText(JAPMessages.getString(MSG_INIT_VIEW));
		IJAPMainView view;
		if (!bConsoleOnly)
		{
			view = new JAPNewView(JAPConstants.TITLE, m_controller, m_firefoxCommand);

			// Create the main frame
			view.create(loadPay);
			//view.setWindowIcon();
			// Switch Debug Console Parent to MainView
			JAPDebug.setConsoleParent( (JAPNewView) view);
		}
		else
		{
			view = new ConsoleJAPMainView();
		}
		// Add observer
		m_controller.addJAPObserver(view);
		m_controller.addEventListener(view);
		// Register the Main view where they are needed
		m_controller.setView(view, splash instanceof JAPSplash);

		// Create the iconified view
		if (!bConsoleOnly)
		{
			splash.setText(JAPMessages.getString(MSG_INIT_ICON_VIEW));
			JAPViewIconified viewIconified;
			viewIconified = new JAPViewIconified( (AbstractJAPMainView) view);
			// Register the views where they are needed
			view.registerViewIconified(viewIconified);
		}

		// start forwarding server if requested
		if (isArgumentSet("--forwarder") || isArgumentSet("-f"))
		{
			m_controller.enableForwardingServer(true);
		}

		//Init Crypto...
		//		java.security.Security.addProvider(new cryptix.jce.provider.CryptixCrypto());
		// Show main frame and dispose splash screen
		//view.show();
		//view.setVisible(true);
		//view.toFront();
		boolean bSystray = JAPModel.getMoveToSystrayOnStartup();
		if (isArgumentSet("-minimized") || isArgumentSet("--minimized") || isArgumentSet("-m"))
		{
			bSystray = true;
		}

		/*
		   splash.setText(JAPMessages.getString(MSG_FINISH_RANDOM));
		   try
		   {
		 secureRandomThread.join();
		   }
		   catch (InterruptedException a_e)
		   {
		 LogHolder.log(LogLevel.NOTICE, LogType.CRYPTO, a_e);
		   }*/

		splash.setText(JAPMessages.getString(MSG_START_LISTENER));
		if (!m_controller.startHTTPListener(listenHost, listenPort))
		{
			view.disableSetAnonMode();
		}

		if (!bConsoleOnly)
		{
			AbstractJAPMainView frameView = (AbstractJAPMainView) view;
			if (bSystray)
			{
				/* The old JAPDll does return false even if hideWindowInTaskbar() succeeded - so we have to do
				 * this to circumvent the bug...
				 * @todo Remove if new DLL is deployed
				 */
				String s = JAPDll.getDllVersion();
				boolean bOldDll = false;
				if (s == null || s.compareTo("00.02.00") < 0)
				{
					frameView.setVisible(true);
					frameView.toFront();
					bOldDll = true;
				}
				if (!frameView.hideWindowInTaskbar() && !bOldDll)
				{
					frameView.setVisible(true);
					frameView.toFront();
				}
			}
			else if (JAPModel.getMinimizeOnStartup())
			{
				frameView.setVisible(true);
				frameView.showIconifiedView();
			}
			else
			{
				GUIUtils.setAlwaysOnTop(frameView, true);
				frameView.setVisible(true);
				frameView.toFront();
				GUIUtils.setAlwaysOnTop(frameView, false);
			}
			if (splash instanceof JAPSplash)
			{
				( (JAPSplash) splash).dispose();
			}
		}

		//WP: check japdll.dll version
		JAPDll.checkDllVersion(true);

		// initially start services
		m_controller.initialRun(listenHost, listenPort);

		//set cascade if given on command line
		if (listenerCascade != null)
		{
			try
			{
				m_controller.setCurrentMixCascade(new MixCascade("Commandline Cascade", null,
					listenerCascade.toVector()));
				m_controller.setAnonMode(true);
			}
			catch (Throwable t)
			{
				LogHolder.log(LogLevel.EXCEPTION, LogType.MISC,
					"Could not set Cascade specified on the Command line! Ignoring information given and continue...");
			}
		}

		// show alternative view (console, http server,...);
		if (bConsoleOnly)
		{
			view.setVisible(true);
		}
	}

	public String getArgumentValue(String a_argument)
	{
		String value = (String) m_arstrCmdnLnArgs.get(a_argument);
		if (value != null && value.trim().length() == 0)
		{
			value = null;
		}

		return value;
	}

	public boolean isArgumentSet(String a_argument)
	{
		return m_arstrCmdnLnArgs.containsKey(a_argument);
	}
	
	public String[] buildPortableFFCommand()
	{
		String[] pFFCommand = null;
		String pFFExecutable = null;
		String pFFprofile = null;
		String pFFHelpPath = null;
		int args_len = 0;
		int argC = 0;
		
		if (!isArgumentSet("--portable") )
		{
			return null;
		}
		//--portable is set
		pFFExecutable = getArgumentValue("--portable");
		args_len++;
		if (pFFExecutable != null)
		{
			// portable configuration seems to be complete
			pFFExecutable = toAbsolutePath(pFFExecutable);
		}
		else
		{
			pFFExecutable = "";
		}
			
		/*if (isArgumentSet("--portable-browserprofile"))
		{
			pFFprofile = getArgumentValue("--portable-browserprofile");
			if(pFFprofile != null)
			{
				args_len += 2;
			}
		}*/
		if (isArgumentSet("--portable-help-path"))
		{
			pFFHelpPath = getArgumentValue("--portable-help-path");
		}
		// One argument is reserved for the URL
		args_len++;
		pFFCommand = new String[args_len];
		pFFCommand[argC] = pFFExecutable;
		argC++;
		/*if(pFFprofile != null)
		{
			pFFCommand[argC] = "-profile";
			pFFCommand[argC+1] = toAbsolutePath(pFFprofile);
			argC += 2;
		}*/
		Locale loc = JAPMessages.getLocale();
		if(pFFHelpPath != null)
		{
			pFFCommand[argC] = pFFHelpPath;
		}
		else
		{
			pFFCommand[argC] = "file://"+toAbsolutePath("help/en/help/index.html");
			if(loc != null)
			{
				if(loc.toString().equalsIgnoreCase("de"))
				{
					pFFCommand[argC] = "file://"+toAbsolutePath("help/de/help/index.html");
				}
			}
		}
		return pFFCommand;
	}
	
	public static String toAbsolutePath(String path)
	{
		if(path != null)
		{
			if(!(path.startsWith(File.separator)) && 
			   !((path.substring(1,3)).equals(":"+File.separator)) )
			{
				//path is relative
				return System.getProperty("user.dir") + File.separator + path;
				
			}
			else
			{
				//path is already absolute
				return path;
			}
		}
		return null;
	}
	
	public static void main(String[] argv)
	{
		// do NOT change anything in main!
		JAP jap = new JAP(argv);
		jap.startJAP();
	}
}
