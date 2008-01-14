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
package jpi;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Enumeration;
import java.util.Properties;

import anon.infoservice.ListenerInterface;
import anon.util.TimedOutputStream;
import jpi.db.DBSupplier;
import jpi.helper.ExternalChargeHelper;
import jpi.helper.PayPalHelper;
import logging.ChainedLog;
import logging.FileLog;
import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;
import logging.SystemErrLog;
import jpi.db.ConfigurationUpdater;

public class JPIMain
{
	public static void main(String argv[])
	{
		TimedOutputStream.init();
		try
		{
			/* for running in non-graphic environments, we need the awt headless support, it is only
			 * available since Java 1.4, so running the service in a non-graphic environment is
			 * only possible with Java versions since 1.4
			 */
			/* set the property in Java 1.0 compatible style */
			Properties systemProperties = System.getProperties();
			systemProperties.put("java.awt.headless", "true");
			System.setProperties(systemProperties);
		}
		catch (Throwable t)
		{
		}
		// wrong commandline arguments
		if ( (argv.length < 1) || (argv.length > 3))
		{
			usage();
		}

		// initialize logging
		SystemErrLog log1 = new SystemErrLog();
		LogHolder.setLogInstance(log1);
		log1.setLogType(LogType.ALL);
		//LogHolder.setDetailLevel(LogHolder.DETAIL_LEVEL_LOWEST);

		// read config file
		if (!Configuration.init(argv[0]))
		{
			LogHolder.log(LogLevel.ERR, LogType.PAY,
						  "JPIMain: Error loading configuration, I'm going to die now");
			System.exit(0);
		}
		if (Configuration.getLogFileName() != null)
		{
			FileLog log2 = new FileLog(Configuration.getLogFileName(), 1000000, 10);
			log2.setLogType(LogType.ALL);
			log2.setLogLevel(Configuration.getLogFileThreshold());
			ChainedLog l = new ChainedLog(log1, log2);
			LogHolder.setLogInstance(l);
		}
		log1.setLogLevel(Configuration.getLogStderrThreshold());

		// process command line args
		boolean newdb = false;
		if (argv.length == 2)
		{
			if (argv[1].equals("new"))
			{
				newdb = true;
			}
			else
			{
				usage();
			}
		}
		else if (argv.length == 3)
		{
			if (argv[1].equals("new"))
			{
				newdb = true;
			}
			else
			{
				usage();
			}
		}

		// initialize database connection
		try
		{
			LogHolder.log(LogLevel.INFO, LogType.PAY, "Connecting to database");
			DBSupplier.initDataBase(
				Configuration.getDatabaseHost(),
				Configuration.getDatabasePort(),
				Configuration.getDatabaseName(),
				Configuration.getDatabaseUserName(),
				Configuration.getDatabasePassword()
				);

			if (newdb)
			{ // drop and recreate all tables
				String input = "";
				System.out.print("This will drop ALL existing data! Continue? (y/n): ");
				try
				{
					BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
					input = in.readLine();
				}
				catch (Exception e)
				{
					LogHolder.log(LogLevel.EXCEPTION, LogType.PAY, "Error while reading from keyboard.");

				}
				if (input.equalsIgnoreCase("y"))
				{
					LogHolder.log(LogLevel.INFO, LogType.PAY, "JPIMain: Recreating database tables...");
					DBSupplier.getDataBase().dropTables();
					DBSupplier.getDataBase().createTables();
				}
			}
			//launch configuration updater thread
			ConfigurationUpdater configUpdater = new ConfigurationUpdater();
			configUpdater.start();

			// launch database maintenance thread
			DBSupplier.getDataBase().startCleanupThread();
		}
		catch (Exception e)
		{
			LogHolder.log(LogLevel.ERR, LogType.PAY, "Could not connect to PostgreSQL database server");
			LogHolder.log(LogLevel.EXCEPTION, LogType.PAY, e);
			System.exit(0);
		}

		// start PIServer(s) for JAP connections
		LogHolder.log(LogLevel.INFO, LogType.PAY, "JPIMain: Launching PIServer for JAP connections");
		Enumeration japListeners = Configuration.getJapListenerInterfaces();

		while (japListeners.hasMoreElements())
		{
			PIServer userServer = new PIServer(PIServer.SERVING_JAP, (ListenerInterface) japListeners.nextElement());
			Thread userThread = new Thread(userServer, userServer.toString());
			userThread.start();
		}

		// start PIServer for AI connections
		LogHolder.log(LogLevel.INFO, LogType.PAY, "JPIMain: Launching PIServer for AI connections on port ");
		PIServer aiServer = new PIServer(PIServer.SERVING_AI, Configuration.getAiListenerInterface());
		Thread aiThread = new Thread(aiServer, aiServer.toString());
		aiThread.start();

		//start MCServer for MixConfig connections
		LogHolder.log(LogLevel.INFO, LogType.PAY, "JPIMain: Launching MCServer for MixConfig connections");
		PIServer mcServer = new PIServer(PIServer.SERVING_MC,Configuration.getMCListenerInterface() );
		Thread mcThread = new Thread(mcServer,mcServer.toString() );
		mcThread.start();

		//start Micropayment server for Call2pay payments via micropayment.de (if enabled in config)
		LogHolder.log(LogLevel.INFO, LogType.PAY, "JPIMain: Launching MicropaymentServer for Call 2 pay connections");
		PIServer micropaymentServer = new PIServer(PIServer.SERVING_MICROPAYMENT,Configuration.getMPListenerInterface() );
		Thread mpThread = new Thread(micropaymentServer,micropaymentServer.toString() );
		mpThread.start();

		//start the credit card helper
		/*String strHelperClass = "jpi.helper." + Configuration.getCreditCardHelper() + "CreditCardHelper";
		 LogHolder.log(LogLevel.INFO, LogType.PAY, "JPIMain: Launching CreditCardHelper: " + strHelperClass);
		   try
		   {
		 Class helper = (Class) Class.forName(strHelperClass);
		 ICreditCardHelper helperObj = (ICreditCardHelper) helper.newInstance();
		 Thread helperThread = new Thread(helperObj);
		 helperThread.start();
		   }
		   catch (Exception e)
		   {
		 LogHolder.log(LogLevel.EXCEPTION, LogType.PAY,
			  "JPIMain: Could not load helper class: " + strHelperClass);
		   }*/

		// start InfoService thread for InfoService connections
		LogHolder.log(LogLevel.INFO, LogType.PAY,
					  "JPIMain: Launching InfoService thread for InfoService connections");
		InfoServiceThread infoServer = new InfoServiceThread();
		Thread infoServiceThread = new Thread(infoServer,"InfoService connections");
		infoServiceThread.start();
		LogHolder.log(LogLevel.INFO, LogType.PAY, "Initialization complete, JPIMain Thread terminating");

		// start PayPalHelper thread for IPN acceptance
		LogHolder.log(LogLevel.INFO, LogType.PAY, "JPIMain: Launching PayPalHelper thread");
		PayPalHelper payPal = new PayPalHelper(Integer.parseInt(Configuration.getPayPalPort()));
		Thread payPalThread = new Thread(payPal,"PayPalHelper");
		payPalThread.start();

		// start ExternalChargeHelper thread for charge request acceptance
		LogHolder.log(LogLevel.INFO, LogType.PAY, "JPIMain: Launching ExternalChargeHelper thread");
		ExternalChargeHelper external = new ExternalChargeHelper(Integer.parseInt(Configuration.
			getExternalChargePort()));
		Thread externalThread = new Thread(external,"ExternalChargeHelper");
		externalThread.start();

		LogHolder.log(LogLevel.INFO, LogType.PAY, "Initialization complete, JPIMain Thread terminating");
	}

	private static void usage()
	{
		System.out.println("Usage: java JPIMain <configfile> [new]");
		System.exit(0);
	}

	public static String getHTMLServerInfo()
	{
		return "<html><body>AN.ON Payment Instance Test Page</body></html>";
	}

}
