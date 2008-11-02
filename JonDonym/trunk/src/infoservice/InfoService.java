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
package infoservice;

import java.io.FileInputStream;
import java.util.Enumeration;
import java.util.Properties;
import java.util.Locale;
import java.util.Observer;
import java.util.Observable;
import platform.signal.SignalHandler;

import infoservice.performance.PerformanceMeter;
import gui.JAPMessages;
import jap.pay.AccountUpdater;
import jap.JAPModel;
import jap.JAPController;
import jap.MixCascadeUpdater;
import anon.infoservice.Constants;
import anon.infoservice.HTTPConnectionFactory;
import anon.infoservice.Database;
import anon.infoservice.ListenerInterface;
import anon.infoservice.TermsAndConditionsFramework;
import anon.util.ThreadPool;
import anon.util.TimedOutputStream;
import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;

public class InfoService implements Observer
{

	protected JWSInternalCommands oicHandler;

	private static int m_connectionCounter;
	private static PerformanceMeter ms_perfMeter;
	private static AccountUpdater ms_accountUpdater;
	//private static MixCascadeUpdater ms_cascadeUpdater;
	//private static MixCascadeUpdater ms_cascadeUpdater;
	
	private String m_configFileName;
	
	protected ThreadPool m_ThreadPool;

	public static void main(String[] argv)
	{
		String fn = null;
		if (argv.length >= 1)
		{
			fn = argv[0].trim();
		}
		if (fn != null)
		{
			if (fn.equalsIgnoreCase("--generatekey"))
			{
				int i = 1;
				String isName=null;
				String passwd=null;
				try
				{
					while (i < argv.length)
					{
						String arg = argv[i].trim();
						if (arg.equals("--name"))
						{
							i++;
							isName = argv[i].trim();
						}
						else if (arg.equals("--passwd"))
						{
							i++;
							passwd = argv[i].trim();
						}
						i++;
					}
				}
				catch (Throwable t)
				{
				}
				InfoService.generateKeyPair(isName, passwd);
				System.exit(0);
			}
			else if (fn.equalsIgnoreCase("--version"))
			{
				System.out.println("InfoService version: " + Constants.INFOSERVICE_VERSION);
				System.exit(0);
			}
		}

		// start the InfoService
		try
		{
			InfoService s1 = new InfoService(fn);
						
//			 configure the performance meter
			if(Configuration.getInstance().isPerfEnabled())
			{				
				LogHolder.log(LogLevel.NOTICE, LogType.NET, "Starting Performance Meter...");
				
				ms_accountUpdater = new AccountUpdater();		
				ms_accountUpdater.start(false);				
				ms_perfMeter = new PerformanceMeter(ms_accountUpdater);
				Thread perfMeterThread = new Thread(InfoService.ms_perfMeter);			
				perfMeterThread.start();
			}
			else
			{
				InfoService.ms_perfMeter = null;
			}
			
			if(Configuration.getInstance().isPassive())
			{
				
			}
			
			// start info service server
			s1.startServer();
			
			SignalHandler handler = new SignalHandler();
			handler.addObserver(s1);
			handler.addSignal("HUP");
			handler.addSignal("TERM");
			
			JAPMessages.setLocale(Locale.ENGLISH);			

			System.out.println("InfoService is running!");
			
			JAPModel model = JAPModel.getInstance();
			model.allowPaymentViaDirectConnection(true);
			model.allowUpdateViaDirectConnection(true);
			model.setAnonConnectionChecker(JAPController.getInstance().new AnonConnectionChecker());
			model.setAutoReConnect(true);
			model.setCascadeAutoSwitch(false);
			
			Thread tacLoader = new Thread()
			{
				public void run()
				{
					while(true)
					{
						TermsAndConditionsFramework.loadFromDirectory(
							Configuration.getInstance().getTermsAndConditionsDir());
						
						try
						{
							Thread.sleep(TermsAndConditionsFramework.TERMS_AND_CONDITIONS_UPDATE_INTERVAL);	
						}
						catch(InterruptedException ex)
						{
							break;
						}
					}
				}
			};
			
			tacLoader.start();
		}
		catch (Exception e)
		{
			System.out.println("Cannot start InfoService...");
			e.printStackTrace();
			System.exit(1);
		}
	}
	
	public void update(Observable a_ob, Object a_args)
	{
		if(a_args == null || a_args.toString() == null)
		{
			return;
		}
		
		String signal = a_args.toString();
		
		if(signal.equals("SIGHUP"))
		{
			System.out.println("Reloading configuration...");
			LogHolder.log(LogLevel.ALERT, LogType.ALL, "Caught SIGHUP. Reloading config...");
			
			try
			{
				loadConfig();
				ms_perfMeter.init();
			}
			catch(Exception ex)
			{
				System.out.println("Could not load configuration. Exiting...");
				LogHolder.log(LogLevel.ALERT, LogType.ALL, "Could not load configuration. Exiting...");
			}
		}
		
		if(signal.equals("SIGTERM"))
		{
			System.out.println("Exiting...");
			LogHolder.log(LogLevel.ALERT, LogType.ALL, "Caught SIGTERM. Exiting...");
			
			stopServer();
			
			System.exit(1);
		}
	}

	/**
	 * Generates a key pair for the infoservice
	 */
	private static void generateKeyPair(String isName, String passwd)
	{
		try
		{
			System.out.println("Start generating new KeyPair (this can take some minutes)...");
			KeyGenTest.generateKeys(isName, passwd);
			System.out.println("Finished generating new KeyPair!");
		}
		catch (Exception e)
		{
			System.out.println("Error generating KeyPair!");
			e.printStackTrace();
		}
	}
	
	private InfoService(String a_configFileName) throws Exception
	{
		m_configFileName = a_configFileName;
		
		loadConfig();
		
		m_connectionCounter = 0;
	}

	private void loadConfig() throws Exception 
	{
		Properties properties = new Properties();
		if (m_configFileName == null)
		{
			m_configFileName = Constants.DEFAULT_RESSOURCE_FILENAME;
		}
		try
		{
			properties.load(new FileInputStream(m_configFileName));
		}
		catch (Exception a_e)
		{
			System.out.println("Error reading configuration!");
			System.out.println(a_e.getMessage());
			System.exit(1);
		}
		new Configuration(properties);
	}

	private void startServer() throws Exception
	{
		HTTPConnectionFactory.getInstance().setTimeout(Constants.COMMUNICATION_TIMEOUT);
		/* initialize Distributor */
		InfoServiceDistributor.getInstance();
		Database.registerDistributor(InfoServiceDistributor.getInstance());
		/* initialize internal commands of InfoService */
		oicHandler = new InfoServiceCommands();
		/* initialize propagandist for our infoservice */
		if(!Configuration.getInstance().isPassive())
		{
			InfoServicePropagandist.generateInfoServicePropagandist(ms_perfMeter);
		}
		// start server
		LogHolder.log(LogLevel.EMERG, LogType.MISC, "InfoService -- Version " + Constants.INFOSERVICE_VERSION);
		LogHolder.log(LogLevel.EMERG, LogType.MISC, System.getProperty("java.version"));
		LogHolder.log(LogLevel.EMERG, LogType.MISC, System.getProperty("java.vendor"));
		LogHolder.log(LogLevel.EMERG, LogType.MISC, System.getProperty("java.home"));
		LogHolder.log(LogLevel.EMERG, LogType.MISC, System.getProperty("os.name"));
		LogHolder.log(LogLevel.EMERG, LogType.MISC, System.getProperty("os.arch"));
		LogHolder.log(LogLevel.EMERG, LogType.MISC, System.getProperty("os.version"));
		m_ThreadPool = new ThreadPool("ISConnection",
									  Configuration.getInstance().getNrOfConcurrentConnections());
		TimedOutputStream.init();
		Enumeration enumer = Configuration.getInstance().getHardwareListeners().elements();
		while (enumer.hasMoreElements())
		{
			InfoServiceServer server = new InfoServiceServer( (ListenerInterface) (enumer.nextElement()), this);
			Thread currentThread = new Thread(server, server.toString());
			currentThread.setDaemon(true);
			currentThread.start();
		}
	}
	
	private void stopServer()
	{
		// TODO: implement
	}

	protected static int getConnectionCounter()
	{
		return m_connectionCounter++;
	}

	protected static PerformanceMeter getPerfMeter()
	{
		return ms_perfMeter;
	}
}
