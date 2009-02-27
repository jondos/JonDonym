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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.security.SecureRandom;
import java.util.Hashtable;
import java.util.Locale;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.zip.ZipFile;

import java.awt.Frame;
import java.awt.Window;

import anon.client.crypto.KeyPool;
import anon.infoservice.ListenerInterface;
import anon.infoservice.MixCascade;
import anon.util.ClassUtil;
import gui.GUIUtils;
import gui.JAPAWTMsgBox;
import gui.JAPDll;
import gui.JAPMessages;
import gui.dialog.JAPDialog;
import gui.help.AbstractHelpFileStorageManager;
import jap.AbstractJAPMainView;
import jap.ConsoleJAPMainView;
import jap.ConsoleSplash;
import jap.IJAPMainView;
import jap.ISplashResponse;
import jap.JAPConf;
import jap.JAPConstants;
import jap.JAPController;
import jap.JAPDebug;
import jap.JAPModel;
import jap.JAPNewView;
import jap.JAPSplash;
import jap.JAPViewIconified;
import jap.MacOSXLib;
import logging.FileLog;
import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;
import logging.SystemErrLog;
import platform.AbstractOS;
import platform.WindowsOS;
import platform.MacOS;

/** This is the main class of the JAP project. It starts everything. It can be inherited by another
 *  class that wants to initialize platform depending features, e.g. see
 *  <A HREF="JAPMacintosh.html">JAPMacintosh.html</A>
 *  as an example.
 */
public class JAP
{
	private static final String MSG_ERROR_NEED_NEWER_JAVA = "errorNeedNewerJava";
	private static final String MSG_ERROR_JONDO_ALREADY_RUNNING = "errorAlreadyRunning";
	private static final String MSG_ERROR_JONDO_ALREADY_RUNNING_WIN = "errorAlreadyRunningWin";
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
	private static final String MSG_EXPLAIN_NO_FIREFOX_FOUND = 
		JAP.class.getName() + "_explainNoFirefoxFound";
	private static final String MSG_USE_DEFAULT_BROWSER = 
		JAP.class.getName() + "_useDefaultBrowser";
	private static final String MSG_CONFIGURE_BROWSER = 
		JAP.class.getName() + "_configureBrowser";
	
	private static final String MSG_UNINSTALLING = JAP.class.getName() + "_uninstalling";
	
	private static final String OPTION_CONTEXT = "--context";
	
	private JAPController m_controller;
	
	Hashtable m_arstrCmdnLnArgs = null;
	String[] m_temp = null;

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
					//System.out.println(argv[i]+": "+argv[i + 1]);
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
		boolean bUninstall = false;		
		String listenHost = null;
		int listenPort = 0;
		MixCascade commandlineCascade = null;
		
		/* system wide socks settings should not not apply to JAP */
		System.getProperties().remove("socksProxyHost");
		System.getProperties().remove("socksProxyPort");
		
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
		
		SystemErrLog templog = new SystemErrLog();
		LogHolder.setLogInstance(templog);
		templog.setLogType(LogType.ALL);
		templog.setLogLevel(LogLevel.WARNING);		
		
		
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
			System.out.println("--uninstall, -u              Delete all configuration and help files.");
			System.out.println("--cascade {[host][:port][:id]}    Connects to the specified Mix-Cascade.");
			System.out.println("--portable [path_to_browser] Tell JonDo that it runs in a portable environment.");
			System.out.println("--portable-jre               Tell JonDo that it runs with a portable JRE.");
			System.out.println("--portable-help-path         Path of external html help files for portable use.");
			System.out.println("--config, -c {Filename}:     Force JonDo to use a specific configuration file.");
			System.out.println("--context {Context}:         Start JonDo with a specific service provider context.");
			System.exit(0);
		}

		if (isArgumentSet("-console") || isArgumentSet("--console"))
		{
			bConsoleOnly = true;
		}
	
		if (isArgumentSet("--uninstall") || isArgumentSet("-u"))
		{
			bUninstall = true;
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
					JAPMessages.init(JAPConstants.MESSAGESFN);
					// multiple instances of JAP have been started, what to do?
					String errorString = 
						JAPMessages.getString(MSG_ERROR_JONDO_ALREADY_RUNNING) +
						((AbstractOS.getInstance() instanceof WindowsOS) ? 
						"\n" + JAPMessages.getString(MSG_ERROR_JONDO_ALREADY_RUNNING_WIN) : "");
					
					if(bConsoleOnly) 
					{
						System.out.println(errorString);
					}
					else
					{
						JAPDialog.ILinkedInformation test = new JAPDialog.LinkedInformationAdapter() {
							public boolean isOnTop() 
							{
								return true;
							}
							
							public boolean isApplicationModalityForced() 
							{
								return true;
							}
						};
						JAPDialog.showErrorDialog(null, errorString, LogType.MISC, test);
					System.exit(0);
					}
				}
			}
		}
		
		// init controller and preload config
		m_controller = JAPController.getInstance();
		
		// Set path to Firefox for portable JAP
		final boolean bPortable = isArgumentSet("--portable");
		m_controller.setPortableMode(bPortable);
		String context =  getArgumentValue(OPTION_CONTEXT);
		if (context != null)
		{
			JAPModel.getInstance().setContext(context);
		}
		
		String configFileName = null;
		boolean bSetPortableConfig = false;
		/* check, whether there is the -config parameter, which means the we use user defined config
		 * file
		 */
		if ((configFileName = getArgumentValue("--config")) == null)
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
				bSetPortableConfig = true;
			}
		}

		if (configFileName != null)
		{
			LogHolder.log(LogLevel.NOTICE, LogType.MISC, "Loading config file '" + configFileName + "'.");
		}
		
		String strErrorFileNotFound = null;
		try 
		{
			m_controller.preLoadConfigFile(configFileName);
		} 
		catch (FileNotFoundException a_e) 
		{
			LogHolder.log(LogLevel.ALERT, LogType.MISC, a_e);
			if (!bSetPortableConfig)
			{
				strErrorFileNotFound = "File not found: " + a_e.getMessage();
			}
		}
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
		
		if (strErrorFileNotFound != null)
		{
			splash.setText(strErrorFileNotFound);
			try 
			{
				Thread.sleep(5000);
			} 
			catch (InterruptedException e) 
			{
				// ignore
			}
			System.exit(-1);
		}

		// Init Messages....
		if (!JAPMessages.isInitialised())
		{
			JAPMessages.init(JAPConstants.MESSAGESFN);
		}

		if (!bConsoleOnly && !bUninstall)
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

		
		if (bUninstall)
		{
			int exitCode = 0;
			splash.setText(JAPMessages.getString(MSG_UNINSTALLING));
			try 
			{
				m_controller.uninstall(configFileName);
			} 
			catch (IOException a_e) 
			{
				LogHolder.log(LogLevel.ALERT, LogType.MISC, a_e);
				exitCode = -1;
			}
			
			if (splash instanceof JAPSplash)
			{
				((JAPSplash)splash).setVisible(false);
			}
			System.exit(exitCode);
		}
		
		
		splash.setText(JAPMessages.getString(MSG_INIT_RANDOM));
		// initialize secure random generators
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
				//cascade is given as host[:port][:id]
				StringTokenizer st=new StringTokenizer(tmpStr,":");
				String host=null;
				String id=null;
				int port=6544;
				if(st.hasMoreTokens())
				{
						host=st.nextToken();
				}
				if(st.hasMoreTokens())
					{
							port=Integer.parseInt(st.nextToken());
					}
				if(st.hasMoreTokens())
					{
							id=st.nextToken();
					}
				commandlineCascade=new MixCascade("Commandline Cascade",id,host,port);
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
		
		//if (m_temp != null)
		{
			String[] cmdArgs = m_temp;
			
			if (m_temp == null || (!isArgumentSet("--allow-multiple") && !isArgumentSet("-a")))
			{
				if (m_temp == null)
				{
					cmdArgs = new String[1];
				}
				else
				{
					cmdArgs = new String[m_temp.length + 1];
					System.arraycopy(m_temp, 0, cmdArgs, 0, m_temp.length);
				}
				cmdArgs[cmdArgs.length - 1] = "-a"; // important for automatic restart; it might block otherwise
			}
			
			m_controller.initCommandLineArgs(cmdArgs);
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

		final String BROWSER_CMD = buildPortableFFCommand(splash);
		AbstractOS.getInstance().init(new AbstractOS.IURLErrorNotifier()
		{
			public void checkNotify(URL a_url)
			{
				/**
				 * Allow non-anonymous surfing for https payment pages.
				 */
				/*
				if (a_url != null && !m_controller.getAnonMode() &&
					JAPModel.getInstance().isNonAnonymousSurfingDenied() &&
					a_url.toString().startsWith("https"))
				{
					JAPModel.getInstance().denyNonAnonymousSurfing(false);
				}*/
			}
		},new AbstractOS.AbstractURLOpener()
		{									
			public boolean openURL(URL a_url, String a_browerCommand)
			{			
				if (a_url == null || !m_controller.isPortableMode())
				{
					// no valid url or no portable installation
					return false;
				}
				
				if (!super.openURL(a_url, a_browerCommand))
				{
					if (a_browerCommand != null && 
						(getBrowserCommand() == null || !a_browerCommand.equals(getBrowserCommand())))
					{
						return false;
					}
					
					JAPDialog.LinkedInformationAdapter adapter = 
						new JAPDialog.LinkedInformationAdapter()
					{
						public boolean isApplicationModalityForced()
						{
							return true;
						}

						public boolean isOnTop()
						{
							return true;
						}
					};
					int answer = JAPDialog.showConfirmDialog(JAPController.getInstance().getCurrentView(),
							JAPMessages.getString(MSG_EXPLAIN_NO_FIREFOX_FOUND), 
							new JAPDialog.Options(JAPDialog.OPTION_TYPE_YES_NO_CANCEL)
					{
						public String getYesOKText()
						{
							return JAPMessages.getString(MSG_USE_DEFAULT_BROWSER);
						}
						
						public String getNoText()
						{
							return JAPMessages.getString(MSG_CONFIGURE_BROWSER);
						}
					}, JAPDialog.MESSAGE_TYPE_WARNING, adapter);
					
					if (answer == JAPDialog.RETURN_VALUE_OK)
					{
						return false; // try to open with system default browser												
					}
					else if (answer == JAPDialog.RETURN_VALUE_NO)
					{
						JAPController.getInstance().showConfigDialog(JAPConf.UI_TAB, this);
					}
				}
				return true;
			}
			
			public URL getDefaultURL()
			{
				return JAPModel.getInstance().getHelpURL();
			}
			
			public String getBrowserPath()
			{
				return getArgumentValue("--portable");
			}
			
			public String getBrowserCommand()
			{
				return BROWSER_CMD;
			}
		});
		
		JAPModel.getInstance().setForwardingStateModuleVisible(forwardingStateVisible);
		// load settings from config file
		splash.setText(JAPMessages.getString(MSG_LOADING_SETTINGS));
		m_controller.loadConfigFile(configFileName, splash);
	
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
		
		

		splash.setText(JAPMessages.getString(MSG_INIT_DLL));
		// set the current splash temporarily in case the controller shuts down during dll update
		m_controller.setView(null, splash);		
		Window window = null;
		if (splash instanceof Window)
		{
			window = (Window)splash;
		}
		JAPDll.init(isArgumentSet(JAPDll.START_PARAMETER_ADMIN),
				getArgumentValue(JAPDll.START_PARAMETER_ADMIN), window);		
		
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
			view = new JAPNewView(JAPConstants.TITLE, m_controller);

			// Create the main frame
			view.create(true);
			//view.setWindowIcon();
			// Switch Debug Console Parent to MainView
			//JAPDebug.setConsoleParent( (JAPNewView) view);
		}
		else
		{
			view = new ConsoleJAPMainView();
		}
		// Add observer
		m_controller.addJAPObserver(view);
		m_controller.addEventListener(view);
		// Register the Main view where they are needed
		if (splash instanceof JAPSplash)
		{			
			m_controller.setView(view, new JAPSplash((Frame)view, 
					JAPMessages.getString(JAPController.MSG_FINISHING)));
		}
		else
		{
			m_controller.setView(view, new ConsoleSplash());
		}
		

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

		//set cascade if given on command line
		if (commandlineCascade != null)
		{
			try
			{
				m_controller.setCurrentMixCascade(commandlineCascade);
			}
			catch (Throwable t)
			{
				LogHolder.log(LogLevel.EXCEPTION, LogType.MISC,
					"Could not set Cascade specified on the Command line! Ignoring information given and continue...");
			}
		}
		
		// initially start services
		m_controller.initialRun(listenHost, listenPort);



		// show alternative view (console, http server,...);
		if (bConsoleOnly)
		{
			view.setVisible(true);
		}
		
		// for some reason this needs to be done here, otherwise 
		// it won't work
		if(AbstractOS.getInstance() instanceof MacOS)
		{
			MacOSXLib.init();
			
			/*final SystrayPopupMenu systray = new SystrayPopupMenu(
					new SystrayPopupMenu.MainWindowListener()
				{
					public void onShowMainWindow()
					{
						// do nothing
					}

					public void onShowSettings(String card, Object a_value)
					{
						
					}

					public void onShowHelp()
					{

					}
				});
			
			javax.swing.JPopupMenu popup = systray.getPopup();
			
			javax.swing.JMenu menu = new javax.swing.JMenu();
			
			for(int i = 0; i < popup.getComponentCount(); i++)
			{
				java.awt.Component c = popup.getComponent(i);
				if(c instanceof javax.swing.JMenuItem)
				{
					menu.add(c);
				}
			}
			
			JAPMacOSXLib.setMenu(menu);*/
		}
	}

	private String getArgumentValue(String a_argument)
	{
		String value = (String) m_arstrCmdnLnArgs.get(a_argument);
		if (value != null && value.trim().length() == 0)
		{
			value = null;
		}

		return value;
	}

	private boolean isArgumentSet(String a_argument)
	{
		return m_arstrCmdnLnArgs.containsKey(a_argument);
	}
	
	private String buildPortableFFCommand(ISplashResponse a_splash)
	{
		String pFFExecutable;
		String pFFHelpPath = null;

		if (!isArgumentSet("--portable") )
		{
			return null;
		}
		
		//check if portable is set
		pFFExecutable = JAPModel.getInstance().getPortableBrowserpath();
		if (pFFExecutable == null)
		{
			pFFExecutable = getArgumentValue("--portable");
			JAPModel.getInstance().setPortableBrowserpath(pFFExecutable);
		}
		if (pFFExecutable != null)
		{
			pFFExecutable = AbstractOS.createBrowserCommand(pFFExecutable);
		}
					
		if (isArgumentSet("--portable-help-path"))
		{
			pFFHelpPath = getArgumentValue("--portable-help-path");
		}
		
		if (pFFHelpPath == null && isArgumentSet("--jar-path"))
		{				
			int index;
			String jarpath = getArgumentValue("--jar-path");
			String pFFHelpPathTmp;
			
			try
			{
				if (m_temp != null && m_temp.length > 0 && jarpath != null)
				{					
					pFFHelpPathTmp = m_temp[0];					
					index = pFFHelpPathTmp.indexOf(jarpath);
					if (index > 0)
					{	
						pFFHelpPathTmp = pFFHelpPathTmp.substring(0, index);
						String[] dirs = new File(pFFHelpPathTmp).list();						
						for (int i = 0; i < dirs.length; i++)
						{
							if (dirs[i].toUpperCase().equals(AbstractHelpFileStorageManager.HELP_FOLDER.toUpperCase()))
							{
								// found a help folder in the jarpath; assume that it is the right one...
								pFFHelpPath = pFFHelpPathTmp;
								break;
							}
						}
					}									
				}
			}
			catch (Exception a_e)
			{
				LogHolder.log(LogLevel.EXCEPTION, LogType.MISC, a_e);
			}
		}
		
//			if no path was found, set the help directory to the path where the JAP.jar is located
		if (pFFHelpPath == null)
		{
			ZipFile jar = ClassUtil.getJarFile();
			if (jar != null)
			{
				pFFHelpPath = new File(jar.getName()).getParent();
			}
			else if (!JAPModel.getInstance().isHelpPathChangeable())
			{
				// maybe this is a test environment with local class files, but no jar file
				pFFHelpPath = ClassUtil.getClassDirectory(this.getClass()).getParent();
			}
		}

		if (pFFHelpPath != null)
		{
			String messageText = a_splash.getText();
			a_splash.setText(JAPMessages.getString(JAPController.MSG_UPDATING_HELP));
			JAPModel.getInstance().setHelpPath(new File(pFFHelpPath), true);
			a_splash.setText(messageText);				
		}
		
		return pFFExecutable;
	}
	
	public static void main(String[] argv)
	{
		// do NOT change anything in main!
		JAP jap = new JAP(argv);
		jap.startJAP();
	}
}
