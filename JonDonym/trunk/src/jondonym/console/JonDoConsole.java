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

import java.util.Enumeration;
import java.util.Vector;

import anon.pay.PayAccount;
import anon.pay.PayAccountsFile;
import anon.pay.PaymentInstanceDBEntry;
import anon.client.TrustModel;
import anon.infoservice.Database;
import anon.infoservice.MixCascade;
import anon.util.Configuration;
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
		
		// TODO put a command line parser here...
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
			Controller.init(null, configuration);
		}
		catch (Exception a_e)
		{
			a_e.printStackTrace();
			//LogHolder.log(LogLevel.EXCEPTION, LogType.MISC, a_e);
			return;
		}
		
		// TODO put a command line parser here...
		if (defaultCascade != null)
		{
			TrustModel.setCurrentTrustModel(TrustModel.TRUST_MODEL_USER_DEFINED);
		}
		
		Controller.start(defaultCascade); // the cascade argument is here only for debugging; use Controller.start() otherwise
		
		if (!Controller.isRunning())
		{
			System.out.println("Could not start controller! Exiting...");
			return;
		}
		
		run();
	}
	
	private static void run()
	{
		String entered = "";
		boolean bChoose = false;
		boolean bCreate = false;
		Vector cascades = null;
		Vector vecPIs = null;
		boolean bPI = false;
		PayAccount account = null;
		boolean bSwitchTrust = false;
		Vector vecTrustModels = null;
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
				else
				{
					System.out.println();
					System.out.println("Type <ENTER> for more information,\n'choose' to choose another service," +
							"\n'force' to prevent auto-switching,\n'charge' to create an account," + 
							"\n'pi' to switch to another payment instance," +
									"\n'trust' to activate another trust model, or\n'exit' to quit.");
				}
			}
			
			entered = null;
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
				continue;
			}

			if (entered.equals("break") || entered.equals("exit"))
			{
				boolean bSaved = false;
				PayAccountsFile.getInstance().deleteAccount(account);
				if (account != null)
				{
					// this account is deleted as it is of no further use and only consumes memory
					PayAccountsFile.getInstance().deleteAccount(account);
					bSaved = true;
					saveConfiguration();
				}
				account = null;
				bChoose = false;
				bCreate = false;
				if (entered.equals("exit"))
				{
					Controller.stop();
					if (!bSaved)
					{
						saveConfiguration();
					}
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
					}
					continue;
				}
				catch (NumberFormatException a_e)
				{
				}
				System.out.println("Sorry, there is no such trust model.");
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
					if (!Controller.activateCouponCode(entered, account, true))
					{
						System.out.println("This does not seem to be a valid coupon code: '" + entered + "'");
					}
					else
					{
						System.out.println("Coupon code accepted, charging account...");
						
						final String tEntered = entered;
						final PayAccount tAccount = account;
						account = null;
						bCreate = false;
						
						Thread useCode = new Thread(new Runnable()
						{
							public void run()
							{
								try
								{
									if (Controller.activateCouponCode(tEntered, tAccount, false))
									{
										try
										{
											tAccount.fetchAccountInfo(true);
										}
										catch (Exception a_e)
										{
											LogHolder.log(LogLevel.EXCEPTION, LogType.PAY, a_e);
											System.out.println("A problem occured while updating your charged account!");
										}
										
										System.out.println("Your have created an account with " + tAccount.getCurrentCredit() + " kbytes!");
										if (PayAccountsFile.getInstance().getActiveAccount() == null || 
											PayAccountsFile.getInstance().getActiveAccount().getCurrentCredit() == 0  ||
											!PayAccountsFile.getInstance().getActiveAccount().getPIID().equals(Controller.getActivePaymentInstanceID()))
										{
											PayAccountsFile.getInstance().setActiveAccount(tAccount);
										}
									}
								}
								catch (Exception a_e)
								{
									LogHolder.log(LogLevel.EXCEPTION, LogType.PAY, a_e);
									System.out.println("A problem occured when validating your code!");
								}
							}
						});
						useCode.setDaemon(false);
						useCode.start();
					}
				}
				catch (Exception a_e)
				{
					LogHolder.log(LogLevel.EXCEPTION, LogType.PAY, a_e);
					System.out.println("A problem occured when validating your code!");
				}	
			}
			else if (entered.equals("choose"))
			{
				cascades = Controller.getAvailableCascades();
				for (int i = 0; i < cascades.size(); i++)
				{
					System.out.println((i+1) + ". " + cascades.elementAt(i));
				}
				bChoose = true;
				
				continue;
			}
			else if (entered.equals("pi"))
			{
				vecPIs = Database.getInstance(PaymentInstanceDBEntry.class).getEntryList();
				for (int i = 0; i < vecPIs.size(); i++)
				{
					System.out.println((i+1) + ". " + ((PaymentInstanceDBEntry)vecPIs.elementAt(i)).getName() + " (" +
							((PaymentInstanceDBEntry)vecPIs.elementAt(i)).getId() + ")");
				}
				bPI = true;
				
				continue;
			}
			else  if (entered.equals("trust"))
			{
				vecTrustModels = TrustModel.getTrustModels();
				for (int i = 0; i < vecTrustModels.size(); i++)
				{
					System.out.println((i+1) + ". " + ((TrustModel)vecTrustModels.elementAt(i)).getName());
				}
				bSwitchTrust = true;
				
				continue;
			}
			else if (entered.equals("force"))
			{
				Controller.setCascadeAutoSwitched(!Controller.isCascadeAutoSwitched());
				System.out.println("Auto-switch is " + (Controller.isCascadeAutoSwitched() ? "ON" : "OFF") + ".");
			}
			else if (entered.equals("charge"))
			{
				bCreate = true;
				try
				{
					account = Controller.createAccount();
					saveConfiguration();
				}
				catch (Exception a_e)
				{
					LogHolder.log(LogLevel.EXCEPTION, LogType.PAY, a_e);
				}
				if (account == null)
				{
					bCreate = false;
					System.out.println("Could not create account for PI " + Controller.getActivePaymentInstanceID() + "!");
				}
			}
			else
			{
				String balance = ")";
				if (PayAccountsFile.getInstance().getActiveAccount() != null &&
					PayAccountsFile.getInstance().getActiveAccount().getBalance() != null)
				{
					balance = ", balance: " + 
						Util.formatBytesValueWithUnit(
								PayAccountsFile.getInstance().getActiveAccount().getCurrentCredit() * 1000) + ")";
				}
				
				System.out.println(Controller.getCurrentCascade().getName() + " (" + (Controller.isConnected() ? "connected, " +
						"auto-switch:" + ((Controller.isCascadeAutoSwitched() ? "ON" : "OFF")) : "disconnected") + 
						", TrustModel:'" + TrustModel.getCurrentTrustModel().getName() + "'" + balance);
				  
	            System.out.println("Memory usage: " + 
	            		Util.formatBytesValueWithUnit(Runtime.getRuntime().totalMemory()) + ", Max VM memory: " +

						 Util.formatBytesValueWithUnit(Runtime.getRuntime().maxMemory()) + ", Free memory: " +
						 Util.formatBytesValueWithUnit(Runtime.getRuntime().freeMemory()));
			}
		}
	}
	
	private static void saveConfiguration()
	{
		// delete all empty accounts (save the coupon codes if you would like to save them...
		Enumeration enumAccounts = PayAccountsFile.getInstance().getAccounts();
		PayAccount currentAccount;
		while (enumAccounts.hasMoreElements())
		{
			currentAccount = (PayAccount)enumAccounts.nextElement();
			if (currentAccount.getBalance() != null && currentAccount.getBalance().getSpent() > 0 && 
					currentAccount.getCurrentCredit() == 0)
			{
				// delete accounts that are clearly unusable or already completely used
				PayAccountsFile.getInstance().deleteAccount(currentAccount);
			}
		}
		
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
				return XMLUtil.toString(XMLUtil.readXMLDocument(CONFIGURATION));
			}
			return null;
		}
		public void write(String a_configurationContent) throws Exception
		{
			System.out.println("Writing configuration to " + CONFIGURATION.getAbsolutePath());
			XMLUtil.write(XMLUtil.toXMLDocument(a_configurationContent), CONFIGURATION);
		}
	}
}
