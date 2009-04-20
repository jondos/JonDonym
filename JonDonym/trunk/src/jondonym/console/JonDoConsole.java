/*
 Copyright (c) 2009, The JAP-Team, JonDos GmbH
 All rights reserved.
 Redistribution and use in source and binary forms, with or without modification,
 are permitted provided that the following conditions are met:

  - Redistributions of source code must retain the above copyright notice,
 this list of conditions and the following disclaimer.

  - Redistributions in binary form must reproduce the above copyright notice,
 this list of conditions and the following disclaimer in the documentation and/or
 other materials provided with the distribution.

  - Neither the name of the University of Technology Dresden, Germany, nor the name of
 the JonDos GmbH, nor the names of their contributors may be used to endorse or
 promote products derived from this software without specific prior written permission.


 THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS ``AS IS'' AND ANY EXPRESS
 OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY
 AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE REGENTS OR CONTRIBUTORS
 BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA,
 OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER
 IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
 OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE
 */
package jondonym.console;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;

import java.util.Hashtable;
import java.util.Vector;

import anon.pay.PaymentInstanceDBEntry;
import anon.client.TrustModel;
import anon.infoservice.Database;
import anon.infoservice.MixCascade;
import anon.util.Configuration;
import anon.util.CountryMapper;
import anon.util.IMiscPasswordReader;
import anon.util.Util;
import anon.util.XMLUtil;
import logging.LogHolder;
import logging.LogType;
import logging.LogLevel;

/**
 * This is a small console test application for the Controller interface. It may be used for
 * testing new interface functions and for debugging.
 * @author Rolf Wendolsky
 */
public class JonDoConsole
{
	public static void main(String[] args)
	{
		MixCascade defaultCascade = null;
		Configuration configuration = new LocalFileConfiguration();
		
		// TODO put a real command line parser here...
		if (args != null && args.length > 0)
		{
			if (args[0].equals("--noConfiguration"))
			{
				configuration = null;
			}
			else
			{
				// interpret the first two arguments as hostname and port
				try 
				{
					defaultCascade = new MixCascade(args[0], Integer.parseInt(args[1]));
				}
				catch (Exception e)
				{
					e.printStackTrace();
					System.out.println("Could not parse input as [hostname] and [port] of a cascade!");
					return;
				}
			}
		}
		
		try
		{
			Controller.setLogDetail(Controller.LOG_DETAIL_LEVEL_HIGHEST);
			//MixCascadeInfo.initToStringAsHTML(createDummyISO2CountryHashtable()); //, "/dummyUserImage.gif", "/dummySecurityImage.jpg");
			Controller.init(null, configuration);
		}
		catch (Exception a_e)
		{
			a_e.printStackTrace();
			//LogHolder.log(LogLevel.EXCEPTION, LogType.MISC, a_e);
			return;
		}
		
		if (defaultCascade != null)
		{
			TrustModel.setCurrentTrustModel(TrustModel.TRUST_MODEL_USER_DEFINED);
		}
		
		System.out.println("Initialising...");
		
		// the cascade argument is here only for debugging; use Controller.start() by default
		if (!Controller.start(defaultCascade) || 
			!Controller.isRunning())
		{
			System.out.println("Could not start controller! Exiting...");
			return;
		}
		
		run();
	}
	
	private static String readLine()
	{
		String entered = null;
		while (entered == null)
		{
			try
			{
				entered = new BufferedReader(new InputStreamReader(System.in)).readLine();
			}
			catch(Throwable t)
			{
			}
			if (entered == null)
			{
				//Hm something is strange... do not simply continue but wait some time
				//BTW: That are situations when this could happen?
				// One is if JAP is run on VNC based X11 server
				try
				{
					Thread.sleep(1000);
				}
				catch (InterruptedException e)
				{
				}
			}
		}
		return entered;
	}
	
	private static void run()
	{
		String entered = "";
		boolean bChoose = false;
		boolean bCreate = false;
		Vector cascades = null;
		Vector vecPIs = null;
		boolean bPI = false;
		boolean bSwitchLogDetails = false;
		boolean bSwitchLogLevel = false;
		boolean bSwitchTrust = false;
		Vector vecTrustModels = null;
		String strTemp;
		boolean bTemp;
		boolean bImport = false;
		int index;
		
		while (true)
		{
			if (entered != null)
			{
				if (bChoose)
				{
					System.out.println("Please enter a cascade number to choose or 'break'.");
				}
				else if (bCreate)
				{
					System.out.println("Please enter a coupon code or 'break'.");
				}
				else if (bPI)
				{
					System.out.println("Please enter a payment instance number to switch or 'break'.");
				}
				else if (bSwitchTrust)
				{
					System.out.println("Please enter the number of the trust filter you wish to activate or 'break'.");
				}
				else if (bSwitchLogDetails || bSwitchLogLevel)
				{
					System.out.println("Please enter the desired logging mode number or 'break'.");
				}
				else if (bImport)
				{
					System.out.println("Please enter the absolute file path to the account file to be imported:");
				}
				else
				{
					System.out.println();
					System.out.println("Type <ENTER> or 'viewCascade' for more information,\n'choose', 'switch', 'start' or 'stop' for controlling services," +
							"\n'trust' to activate another trust filter," + " 'force' to prevent service auto-switching," +
									"\n'charge' to create an account," + " 'import' to import an account," + " 'pi' to switch to another payment instance," +
									"\n'logdetails' or 'loglevel' for altering the logging modes or 'exit' to quit.");
				}
			}
			
			entered = readLine();

			if (entered.equals("break") || entered.equals("exit"))
			{
				bChoose = false;
				bCreate = false;
				if (entered.equals("exit"))
				{
					Controller.stop();
					saveConfiguration();
					break;
				}
			}
			else if (bChoose)
			{
				bChoose = false;
				try
				{
					index = Integer.parseInt(entered) - 1;
					if (index >= 0 && index < cascades.size())
					{
						Controller.switchCascade((MixCascade)cascades.elementAt(index));
					}
					continue;
				}
				catch (NumberFormatException a_e)
				{
				}
				System.out.println("Sorry, this service is not available.");
			}
			else if (bSwitchLogDetails)
			{
				bSwitchLogDetails = false;
				bTemp = false;
				try
				{
					index = Integer.parseInt(entered) - 1;
					bTemp = Controller.setLogDetail(index);
				}
				catch (NumberFormatException a_e)
				{
				}
				if (!bTemp)
				{
					System.out.println("Sorry, there is no such detail level for logging.");
				}
				System.out.println("Log detail is now: '" + Controller.getLogDetailName(Controller.getLogDetail()) + "'");
			}
			else if (bSwitchLogLevel)
			{
				bSwitchLogLevel = false;
				try
				{
					index = Integer.parseInt(entered) - 1;
					if (index >= 0 && index < Controller.getLogLevelCount())
					{
						Controller.setLogLevel(index);
						System.out.println("Log level is now: '" + Controller.getLogLevelName(index) + "'");
						continue;
					}	
				}
				catch (NumberFormatException a_e)
				{
				}
				System.out.println("Sorry, there is no such log level.");				
			}
			else if (bSwitchTrust)
			{
				bSwitchTrust = false;
				try
				{
					index = Integer.parseInt(entered) - 1;
					if (index >= 0 && index < vecTrustModels.size())
					{
						TrustModel.setCurrentTrustModel((TrustModel)vecTrustModels.elementAt(index));
						System.out.println("Trust model is now: '" + ((TrustModel)vecTrustModels.elementAt(index)).getName() + "'");
						if (!TrustModel.getCurrentTrustModel().isTrusted(Controller.getCurrentCascade()) && Controller.isCascadeAutoSwitched())
						{
							Controller.switchCascade();
						}
						continue;
					}	
				}
				catch (NumberFormatException a_e)
				{
				}
				System.out.println("Sorry, there is no such trust model.");
			}
			else if (bImport)
			{
				boolean bImportSucceeded = false;
		
				File fileAccounts = new File(entered);
				String strData = null;
				try
				{
					strData = XMLUtil.toString(XMLUtil.readXMLDocument(fileAccounts));
				}
				catch (Exception a_e)
				{
					LogHolder.log(LogLevel.ERR, LogType.FILE, a_e);
				}
				if (strData != null)
				{
					IMiscPasswordReader pwReader = new IMiscPasswordReader()
					{
						public synchronized String readPassword(Object message)
						{
							String strPW;
							
							System.out.println("Please enter password for account " + message + " or 'break':");
							strPW = readLine();
							if (strPW.equals("break"))
							{
								return null;
							}
							
							return strPW;
						}
					};
					
					bImportSucceeded = Controller.importAccounts(strData, pwReader);
				}
				if (bImportSucceeded)
				{
					System.out.println("Import was successful!");
				}
				else
				{
					System.out.println("Could not import any account!");
				}
				
				bImport = false;
			}
			else if (bPI)
			{
				bPI = false;
				try
				{
					index = Integer.parseInt(entered) - 1;
					if (index >= 0 && index < vecPIs.size())
					{
						Controller.setActivePaymentInstanceID(((PaymentInstanceDBEntry)vecPIs.elementAt(index)).getId());
					}
					System.out.println("The active payment instance is now " + Controller.getActivePaymentInstanceID() + ".");
					continue;
				}
				catch (NumberFormatException a_e)
				{
				}
				System.out.println("Sorry, this payment instance is not available.");
			}			
			else if (bCreate)
			{
				try
				{
					if (Controller.validateCoupon(entered))
					{
						bCreate = false;
						System.out.println("Coupon code accepted, charging account...");
						if (Controller.activateCoupon(entered))
						{
							System.out.println("Account charged! We have a total of " + 
									Util.formatBytesValueWithUnit(Controller.getCurrentCredit()) + " now.");
						}
						else
						{
							System.out.println("Could not charge account with valid coupon code!");
						}
					}
					else
					{
						System.out.println("This does not seem to be a valid coupon code: '" + entered + "'");
					}
				}
				catch (Exception a_e)
				{
					LogHolder.log(LogLevel.EXCEPTION, LogType.PAY, a_e);
					System.out.println("A problem occured when validating your code!");
				}	
			}
			else if (entered.trim().equals("switch"))
			{
				Controller.switchCascade();
			}
			else if (entered.trim().equals("import"))
			{
				bImport = true;
			}
			else if (entered.trim().equals("start"))
			{
				Controller.start();
			}
			else if (entered.trim().equals("stop"))
			{
				Controller.stop();
			}
			else if (entered.trim().equals("viewCascade"))
			{
				System.out.println(Controller.getCurrentCascade().getName() + " | " + 
						new MixCascadeInfo(Controller.getCurrentCascade()).toString());
			}
			else if (entered.trim().equals("choose"))
			{
				cascades = Controller.getAvailableCascades();
				for (int i = 0; i < cascades.size(); i++)
				{
					if (Controller.getCurrentCascade().equals(cascades.elementAt(i)))
					{
						strTemp = "* ";
					}
					else
					{
						strTemp = "";
					}
					System.out.println(strTemp + (i+1) + ". " + cascades.elementAt(i));
				}
				bChoose = true;
				
				continue;
			}
			else if (entered.trim().equals("pi"))
			{
				vecPIs = Database.getInstance(PaymentInstanceDBEntry.class).getEntryList();
				for (int i = 0; i < vecPIs.size(); i++)
				{
					if (Controller.getActivePaymentInstanceID().equals(((PaymentInstanceDBEntry)vecPIs.elementAt(i)).getId()))
					{
						strTemp = "* ";
					}
					else
					{
						strTemp = "";
					}
					System.out.println(strTemp + (i+1) + ". " + ((PaymentInstanceDBEntry)vecPIs.elementAt(i)).getName() + " (" +
							((PaymentInstanceDBEntry)vecPIs.elementAt(i)).getId() + ")");
				}
				bPI = true;
				
				continue;
			}
			else if (entered.trim().equals("trust"))
			{
				vecTrustModels = TrustModel.getTrustModels();
				for (int i = 0; i < vecTrustModels.size(); i++)
				{
					if (TrustModel.getCurrentTrustModel().equals(vecTrustModels.elementAt(i)))
					{
						strTemp = "* ";
					}
					else
					{
						strTemp = "";
					}
					System.out.println(strTemp + (i+1) + ". " + ((TrustModel)vecTrustModels.elementAt(i)).getName());
				}
				bSwitchTrust = true;
				
				continue;
			}
			else if (entered.trim().equals("logdetails"))
			{
				for (int i = 0; i < Controller.getLogDetailCount(); i++)
				{
					if (Controller.getLogDetail() == i)
					{
						strTemp = "* ";
					}
					else
					{
						strTemp = "";
					}
					System.out.println(strTemp + (i+1) + ". " + Controller.getLogDetailName(i));
				}
				
				bSwitchLogDetails = true;
			}
			else if (entered.trim().equals("loglevel"))
			{
				for (int i = 0; i < Controller.getLogLevelCount(); i++)
				{
					if (Controller.getLogLevel() == i)
					{
						strTemp = "* ";
					}
					else
					{
						strTemp = "";
					}
					System.out.println(strTemp + (i+1) + ". " + Controller.getLogLevelName(i));
				}
				bSwitchLogLevel = true;
			}
			else if (entered.trim().equals("force"))
			{
				Controller.setCascadeAutoSwitched(!Controller.isCascadeAutoSwitched());
				System.out.println("Auto-switch is " + (Controller.isCascadeAutoSwitched() ? "ON" : "OFF") + ".");
			}
			else if (entered.trim().equals("charge"))
			{
				bCreate = true;
				try
				{
					Controller.activateCoupon(null); //reset payment if needed
				}
				catch (Exception a_e)
				{
					LogHolder.log(LogLevel.EXCEPTION, LogType.PAY, a_e);
					System.out.println("Resetting coupon methods failed!");
				}
			}
			else
			{
				String strRecharge = ")";
				if (Controller.getCurrentCredit() > 0 && Controller.shouldRecharge())
				{
					strRecharge = ", credits will run out soon)";
				}
				String balance = ", balance: " + Util.formatBytesValueWithUnit(Controller.getCurrentCredit()) + strRecharge;
				
				System.out.println(Controller.getCurrentCascade().getName() + " (" + (Controller.isConnected() ? "connected, " +
						"auto-switch:" + ((Controller.isCascadeAutoSwitched() ? "ON" : "OFF")) : "disconnected") + 
						", TrustModel:'" + TrustModel.getCurrentTrustModel().getName() + "'" + balance);
				  
	            System.out.print("Memory usage: " + 
	            		Util.formatBytesValueWithUnit(Runtime.getRuntime().totalMemory()) + ", Max VM memory: " +

						 Util.formatBytesValueWithUnit(Runtime.getRuntime().maxMemory()) + ", Free memory: " +
						 Util.formatBytesValueWithUnit(Runtime.getRuntime().freeMemory()));
			}
		}
	}
	
	private static void saveConfiguration()
	{
		try
		{
			Controller.saveConfiguration();
		}
		catch (Exception a_e)
		{
			LogHolder.log(LogLevel.EXCEPTION, LogType.FILE, a_e);
			System.out.println("Configuration was not saved!");
		}
	}
	
	/**
	 * We know it is an XML configuration, so we may use XML methods. This is not very nice and other people
	 * should not do this, as the configuration format might change (compression etc.). 
	 * Therefore this class is private and not for public use.
	 */
	private static class LocalFileConfiguration implements Configuration 
	{
		private final File CONFIGURATION = new File("jondoConsole.conf");
		
		public String read() throws Exception
		{
			if (CONFIGURATION.exists())
			{
				System.out.println("Loading configuration from " + CONFIGURATION.getAbsolutePath());
				// this is a fast-hacked implementation for reading the document; it assumes it is XML; you should not do that normally...
				return XMLUtil.toString(XMLUtil.readXMLDocument(CONFIGURATION));
			}
			return null;
		}
		public void write(String a_configurationContent) throws Exception
		{
			System.out.println("Writing configuration to " + CONFIGURATION.getAbsolutePath());
			// this is a fast-hacked implementation for writing the document; it assumes it is XML; you should not do that normally...
			XMLUtil.write(XMLUtil.toXMLDocument(a_configurationContent), CONFIGURATION);
		}
	}
	
	private static Hashtable createDummyISO2CountryHashtable()
	{
		Vector vecCountries = CountryMapper.getLocalisedCountries();
		Hashtable dummy = new Hashtable(vecCountries.size());
		String code;
		for (int i = 0; i < vecCountries.size(); i++)
		{
			code = ((CountryMapper)vecCountries.elementAt(i)).getISOCode();
			dummy.put(code, "/dummy/path/toImage_" + code + ".png");
		}
		return dummy;
	}
}
