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

import java.io.IOException;
import java.net.ServerSocket;

import java.security.SecureRandom;
import java.security.SignatureException;
import java.sql.Timestamp;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Observable;
import java.util.StringTokenizer;
import java.util.Vector;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import anon.crypto.Util;
import anon.pay.BIConnection;
import anon.pay.PayAccount;
import anon.pay.PayAccountsFile;
import anon.pay.PaymentInstanceDBEntry;
import anon.pay.PayAccountsFile.AccountAlreadyExistingException;
import anon.pay.xml.XMLGenericStrings;
import anon.pay.xml.XMLPassivePayment;
import anon.pay.xml.XMLTransCert;
import anon.proxy.AnonProxy;
import anon.infoservice.AbstractDatabaseEntry;
import anon.infoservice.Database;
import anon.infoservice.IDistributable;
import anon.infoservice.IDistributor;
import anon.infoservice.InfoServiceDBEntry;
import anon.infoservice.InfoServiceHolder;
import anon.infoservice.MixCascade;
import anon.infoservice.update.AbstractMixCascadeUpdater;
import anon.infoservice.update.InfoServiceUpdater;
import anon.infoservice.update.PaymentInstanceUpdater;
import anon.infoservice.update.PerformanceInfoUpdater;
import logging.AbstractLog4jLog;
import logging.Log;
import logging.LogHolder;
import logging.LogType;
import logging.LogLevel;
import logging.SystemErrLog;
import anon.client.AbstractAutoSwitchedMixCascadeContainer;
import anon.client.DummyTrafficControlChannel;
import anon.client.ITermsAndConditionsContainer;
import anon.client.TrustException;
import anon.client.TrustModel;
import anon.client.TrustModel.NumberOfMixesAttribute;
import anon.client.TrustModel.TrustAttribute;
import anon.client.crypto.KeyPool;
import anon.crypto.JAPCertificate;
import anon.crypto.SignatureVerifier;
import anon.util.ClassUtil;
import anon.util.Configuration;
import anon.util.IMiscPasswordReader;
import anon.util.JAPMessages;
import anon.util.XMLParseException;
import anon.util.XMLUtil;
import anon.util.Updater.ObservableInfo;

/**
 * 
 * TODO documentation
 * TODO forwarding server support
 * TODO memory and performance tuning
 * TODO some more functionality tests
 * @author Rolf Wendolsky
 */
public class Controller
{
	public static final int LOG_DETAIL_LEVEL_LOWEST = LogHolder.DETAIL_LEVEL_LOWEST;
	public static final int LOG_DETAIL_LEVEL_LOWER = LogHolder.DETAIL_LEVEL_LOWER;
	public static final int LOG_DETAIL_LEVEL_HIGH = LogHolder.DETAIL_LEVEL_HIGH;
	public static final int LOG_DETAIL_LEVEL_HIGHEST = LogHolder.DETAIL_LEVEL_HIGHEST;
	
	private static final String VERSION = "00.00.009";
	private static final String XML_ROOT_NODE = "ConsoleController";
	
	private static final String MESSAGES = "JAPMessages";
	
	private static final String XML_ATTR_LOG_DETAIL = "logDetail";
	private static final String XML_ATTR_LOG_LEVEL = "logLevel";
	
	private static final String PI_JONDOS = "ECD365B98453B316B210B55546731F52DA445F40";
	//private static final String PI_TEST = "3ADE1713CAFA6470FADCC3395415F8950C42CD2E";
	
	private static final long TIMEOUT_RECHARGE = 1000 * 60 * 60 * 24 * 14; // 14 days
	
	private static final String MSG_NO_CHARGED_ACCOUNT = Controller.class.getName() + "_noChargedAccount";
	private static final String MSG_DEFAULT_TRUST_MODEL = Controller.class.getName() + "_trustModelDefault";
	
	private static InfoServiceUpdater ms_isUpdater;
	private static MixCascadeUpdater ms_cascadeUpdater;
	private static PaymentInstanceUpdater ms_paymentUpdater;
	private static PerformanceInfoUpdater ms_perfUpdater;
	private static ServerSocket ms_socketListener;
	private static AnonProxy ms_jondonymProxy;
	private static AutoSwitchedMixCascadeContainer ms_serviceContainer;
	private static String ms_currentPIID = PI_JONDOS;
	private static Configuration ms_configuration;
	private static Log ms_logger;
	private static PayAccount ms_currentlyCreatedAccount;
	private static int m_iInitLogLevel = LogLevel.WARNING;
	
	private static Thread ms_starterThread;
	private static RunnableStarter ms_starter;
	private static final Object SYNC_STARTER = new Object();
	private static boolean m_bShuttingDown = false;
	
	private static final String DEFAULT_INFOSERVICE_NAMES[] =
		new String[]{"880D9306B90EC8309178376B43AC26652CE52B74",
		"8FF9236BD03A12391D939219310597C830F3943A",
		"1E47E65976C6F7868047B6E9A06654B8AFF36A38",
		"AE116ECB775FF127C02DF96F5466AECAF86B93A9"};
	//new String[]{"1AF4734DD3AA5BD1A8A4A2EDACAD825C711E1770"};
	private static final String DEFAULT_INFOSERVICE_HOSTNAMES[] =
		new String[]{"infoservice.inf.tu-dresden.de", "87.230.56.74", "78.129.146.44", "72.55.137.241"};
	//new String[]{"87.230.20.187"};

	private static final int DEFAULT_INFOSERVICE_PORT_NUMBERS[][] =
		{{80}, {80}, {80}, {80}};	
	
	
	public static synchronized void init(final Logger a_logger, Configuration a_configuration) throws Exception
	{		
		if (ms_isUpdater != null)
		{
			return;
		}		
	
		if (a_logger == null)
		{
			ms_logger = new SystemErrLog();
		}
		else
		{
			ms_logger = new AbstractLog4jLog()
			{
				  protected Logger getLogger()
					{
						return a_logger;
					}
			};
		}
		
   		LogHolder.setLogInstance(ms_logger);
   		ms_logger.setLogType(LogType.ALL);
   		ms_logger.setLogLevel(m_iInitLogLevel);
   		
   		LogHolder.log(LogLevel.ALERT, LogType.MISC, "Initialising " + ClassUtil.getClassNameStatic() + " version " + VERSION + "...");
   		
   		JAPMessages.init(MESSAGES);
   		
   		LogHolder.log(LogLevel.NOTICE, LogType.CRYPTO, "Initialising random number generator...");
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
		try
		{
			secureRandomThread.join();
		}
		catch (InterruptedException a_e)
		{
			LogHolder.log(LogLevel.ERR, LogType.CRYPTO, "Interrupted while initialising random number generator!");
		}
		LogHolder.log(LogLevel.NOTICE, LogType.CRYPTO, "Random number generator initialised.");
   		
		LogHolder.log(LogLevel.NOTICE, LogType.CRYPTO, "Initialising general settings...");
		ClassUtil.enableFindSubclasses(false); // This would otherwise start non-daemon AWT threads, blow up memory and prevent closing the app.
		XMLUtil.setStorageMode(XMLUtil.STORAGE_MODE_AGRESSIVE); // Store as few XML data as possible for memory optimization.
		SignatureVerifier.getInstance().setCheckSignatures(true);
		
		Util.addDefaultCertificates("acceptedInfoServiceCAs/", JAPCertificate.CERTIFICATE_TYPE_ROOT_INFOSERVICE);
		Util.addDefaultCertificates("acceptedMixCAs/", JAPCertificate.CERTIFICATE_TYPE_ROOT_MIX);
		Util.addDefaultCertificates("acceptedPIs/", JAPCertificate.CERTIFICATE_TYPE_PAYMENT);
		LogHolder.log(LogLevel.NOTICE, LogType.CRYPTO, "General settings initialised.");
     
		LogHolder.log(LogLevel.NOTICE, LogType.CRYPTO, "Initialising database updaters...");
		// simulate database distributor and suppress distributor warnings
		Database.registerDistributor(new IDistributor()
		{
			public void addJob(IDistributable a_distributable)
			{
			}
		});
    
		InfoServiceDBEntry[] defaultInfoService = anon.util.Util.createDefaultInfoServices(
				DEFAULT_INFOSERVICE_NAMES, DEFAULT_INFOSERVICE_HOSTNAMES, DEFAULT_INFOSERVICE_PORT_NUMBERS);
		for (int i = 0; i < defaultInfoService.length; i++)
		{
			Database.getInstance(InfoServiceDBEntry.class).update(defaultInfoService[i]);
		}
		InfoServiceHolder.getInstance().setPreferredInfoService(defaultInfoService[0]);
		
		ObservableInfo a_observableInfo = new ObservableInfo(new Observable())
		{
			public Integer getUpdateChanged()
			{
				return new Integer(0);
			}
			public boolean isUpdateDisabled()
			{
				return false;
			}
		};		
		ms_isUpdater = new InfoServiceUpdater(a_observableInfo);
		ms_cascadeUpdater = new MixCascadeUpdater(a_observableInfo);
		ms_paymentUpdater = new PaymentInstanceUpdater(a_observableInfo);
		ms_perfUpdater = new PerformanceInfoUpdater(a_observableInfo);
		
		ms_isUpdater.start(true);
        ms_paymentUpdater.start(true);
        ms_cascadeUpdater.start(true);
        ms_perfUpdater.start(true);
		LogHolder.log(LogLevel.NOTICE, LogType.CRYPTO, "Database updaters initialised.");
		
		LogHolder.log(LogLevel.NOTICE, LogType.CRYPTO, "Initialising trust filters...");
		// predefine trust model; force premium services if charged account is available, but do not use them if not
		TrustModel modelDynamicPremium = new TrustModel(MSG_DEFAULT_TRUST_MODEL, TrustModel.FIRST_UNRESERVED_MODEL_ID);
		modelDynamicPremium.setAttribute(ForcePremiumIfAccountAvailableAttribute.class, TrustModel.TRUST_IF_TRUE);
		modelDynamicPremium.setAttribute(NumberOfMixesAttribute.class, TrustModel.TRUST_IF_AT_LEAST, 3);
		TrustModel.addTrustModel(modelDynamicPremium);
		TrustModel.setCurrentTrustModel(modelDynamicPremium);
		LogHolder.log(LogLevel.NOTICE, LogType.CRYPTO, "Trust filters initialised.");
		
		ms_configuration = a_configuration;
		if (ms_configuration == null)
		{
			LogHolder.log(LogLevel.WARNING, LogType.MISC, "No configuration object available!");
		}
		else
		{
			String conf = ms_configuration.read();
			if (conf != null)
			{
				LogHolder.log(LogLevel.NOTICE, LogType.CRYPTO, "Parsing configuration...");
				Element root = XMLUtil.toXMLDocument(conf).getDocumentElement();
				Element elem;
				XMLUtil.removeComments(root);
				XMLUtil.assertNodeName(root, XML_ROOT_NODE);
				
				LogHolder.setDetailLevel(XMLUtil.parseAttribute(root, XML_ATTR_LOG_DETAIL, LogHolder.getDetailLevel()));
				ms_logger.setLogLevel(XMLUtil.parseAttribute(root, XML_ATTR_LOG_LEVEL, ms_logger.getLogLevel()));
				
				elem = (Element)XMLUtil.getFirstChildByName(root, PayAccountsFile.XML_ELEMENT_NAME);
				if (elem != null)
				{
					LogHolder.log(LogLevel.NOTICE, LogType.CRYPTO, "Initialising pay accounts from configuration...");
					PayAccountsFile.init(elem, null, false);
					LogHolder.log(LogLevel.NOTICE, LogType.CRYPTO, "Pay accounts initialised from configuration.");
				}
				elem = (Element)XMLUtil.getFirstChildByName(root, TrustModel.XML_ELEMENT_CONTAINER_NAME);
				if (elem != null)
				{
					LogHolder.log(LogLevel.NOTICE, LogType.CRYPTO, "Initialising trust filters from configuration...");
					TrustModel.fromXmlElement(elem);
					if (TrustModel.getCurrentTrustModel() == TrustModel.getTrustModelUserDefined())
					{
						TrustModel.setCurrentTrustModel(TrustModel.getTrustModelDefault());
					}
					LogHolder.log(LogLevel.NOTICE, LogType.CRYPTO, "Trust filters initialised from configuration.");
				}
				LogHolder.log(LogLevel.NOTICE, LogType.CRYPTO, "Configuration parsed completely.");
			}
			else
			{
				LogHolder.log(LogLevel.WARNING, LogType.MISC, "Configuration is empty!");
			}
		}
	}
	
	
	/**
	 * Returns the number of log levels. Each log level is represented by an integer
	 * value from 0 to getLogLevelCount() - 1.
	 * @return
	 */
	public static int getLogLevelCount()
	{
		return LogLevel.getLevelCount();
	}
	
	public static String getLogLevelName(int a_level)
	{
		return LogLevel.getLevelName(a_level);
	}
	
	public static synchronized int getLogLevel()
	{
		if (ms_logger != null)
		{
			return ms_logger.getLogLevel();
		}
		else
		{
			return m_iInitLogLevel;
		}
	}
	
	public static synchronized void setLogLevel(int a_level)
	{
		if (ms_logger != null)
		{
			ms_logger.setLogLevel(a_level);
		}
		else
		{
			m_iInitLogLevel = a_level;
		}
	}
	
	public static String getLogDetailName(int a_detail)
	{
		return LogHolder.getDetailLevelName(a_detail);
	}
	
	public static void setLocale(Locale a_locale)
	{
		JAPMessages.init(a_locale, MESSAGES);
	}
	
	public static Locale getLocale()
	{
		return JAPMessages.getLocale();
	}
	
	public static int getLogDetail()
	{
		return LogHolder.getDetailLevel();
	}
	
	public static int getLogDetailCount()
	{
		return LogHolder.getDetailLevelCount();
	}
	
	public static boolean setLogDetail(int a_logDetail)
	{
		return LogHolder.setDetailLevel(a_logDetail);
	}
	
	public static void stop()
	{
		if (m_bShuttingDown)
		{
			return;
		}
		
		synchronized (SYNC_STARTER)
		{
			if (ms_socketListener == null)
			{
				return;
			}
			
			m_bShuttingDown = true;
			
			while (ms_starterThread != null && !ms_starter.isFinished())
			{
				LogHolder.log(LogLevel.NOTICE, LogType.THREAD, "Interrupting startup thread...");
				ms_starterThread.interrupt();
				try 
				{
					SYNC_STARTER.wait(200);
				} 
				catch (InterruptedException e) 
				{
					LogHolder.log(LogLevel.EXCEPTION, LogType.THREAD, e);
				}
			}
			
			if (ms_jondonymProxy != null)
			{
				ms_jondonymProxy.stop();
			}
			if (ms_socketListener != null)
			{
				try 
				{
					ms_socketListener.close();
				} 
				catch (IOException a_e) 
				{
					LogHolder.log(LogLevel.EXCEPTION, LogType.NET, a_e);
				}
				ms_socketListener = null;
			}
			
			m_bShuttingDown = false;
		}
	}
	
	public static synchronized void switchCascade()
	{
		if (ms_jondonymProxy != null && ms_serviceContainer != null)
		{
			ms_serviceContainer.setCurrentCascade(ms_serviceContainer.getNextRandomCascade());
			ms_jondonymProxy.stop();
			ms_jondonymProxy.start(ms_serviceContainer);
		}
	}
	
	public static synchronized void switchCascade(MixCascade a_cascade)
	{
		if (a_cascade == null)
		{
			return;
		}
		if (ms_jondonymProxy != null && ms_serviceContainer != null)
		{
			ms_serviceContainer.setCurrentCascade(a_cascade);
			ms_jondonymProxy.stop();
			ms_jondonymProxy.start(ms_serviceContainer);
		}
	}
	
	public static synchronized void setCascadeAutoSwitched(boolean a_bAutoSwitch)
	{
		if (ms_serviceContainer != null)
		{
			ms_serviceContainer.setServiceAutoSwitched(a_bAutoSwitch);
		}
	}
	
	public static synchronized boolean isCascadeAutoSwitched()
	{
		if (ms_serviceContainer != null)
		{
			return ms_serviceContainer.isServiceAutoSwitched();
		}
		return true;
	}
	
	public static synchronized Vector getAvailableCascades()
	{
		Vector vecCascades =  Database.getInstance(MixCascade.class).getEntryList();
		Vector vecAvailable = new Vector();
		for (int i = 0; i < vecCascades.size(); i++)
		{
			if (TrustModel.getCurrentTrustModel().isTrusted((MixCascade)vecCascades.elementAt(i)))
			{
				vecAvailable.addElement(vecCascades.elementAt(i));
			}
		}
		return vecAvailable;
	}
	
	public static synchronized MixCascade getCurrentCascade()
	{
		MixCascade cascade = null;
		if (ms_jondonymProxy != null)
		{
			cascade = ms_jondonymProxy.getMixCascade();
		}
		
		if (cascade == null)
		{
			try
			{
				cascade = new MixCascade("-", "-", "0.0.0.0", 6544);
			}
			catch (Exception a_e)
			{
				LogHolder.log(LogLevel.EMERG, LogType.MISC, a_e);
			}
		}
		return cascade;
	}
	
	public static synchronized boolean isRunning()
	{
		return ms_socketListener != null;
	}
	
	public static synchronized boolean isConnected()
	{
		return isRunning() && ms_jondonymProxy != null && ms_jondonymProxy.isConnected();
	}
	
	public static String getActivePaymentInstanceID()
	{
		return ms_currentPIID;
	}
	
	public static void  setActivePaymentInstanceID(String a_piid)
	{
		if (a_piid != null && a_piid.trim().length() > 0 && new StringTokenizer(a_piid).countTokens() == 1)
		{
			ms_currentPIID = a_piid;
		}
	}
	
	private static PayAccount createAccount() throws Exception
	{
		PaymentInstanceDBEntry entry = PayAccountsFile.getInstance().getBI(ms_currentPIID);
		
		if (entry == null)
		{
			return null;
		}
		return PayAccountsFile.getInstance().createAccount(entry, null);
	}
	
	private static boolean activateCouponCode(String a_code, PayAccount a_account, boolean a_bPreCheckOnly) throws Exception
	{	
		boolean bValid = true;
		
		if (a_account == null)
		{
			throw new NullPointerException("No account given!");
		}
		
		if ((a_code = PayAccount.checkCouponCode(a_code)) == null)
		{
			return false;
		}
		
		BIConnection piConn = null;
		if (a_bPreCheckOnly)
		{
			bValid = false;
			try
			{
				piConn = new BIConnection(a_account.getBI());
				piConn.connect();
				piConn.authenticate(a_account.getAccountCertificate(),a_account.getPrivateKey());
				bValid = piConn.checkCouponCode(a_code);
				piConn.disconnect();
			}
			catch (Exception a_e)
			{
				if (piConn != null)
				{
					try
					{
						piConn.disconnect();
					}
					catch (Exception a_e2)
					{
						//ignore
					}
				}
				throw a_e;
			}
			
			// do not really use this code; we only checked it
			return bValid;
		}
		
		XMLGenericStrings requestData = new XMLGenericStrings();
		requestData.addEntry("plan", "Coupon");
		requestData.addEntry("method", "Coupon");
		requestData.addEntry("amount", "0");
		
		String lang = JAPMessages.getLocale().getLanguage();
		requestData.addEntry("language",lang);
		XMLTransCert transCert = a_account.charge(requestData);
		if (transCert == null)
		{
			return false;
		}
		
		 XMLPassivePayment paymentToSend = new XMLPassivePayment();
		 //if coupon was used, get its code and put it into an XMLPassivePayment
		
		 paymentToSend.addData(XMLPassivePayment.KEY_COUPONCODE, a_code);
		 paymentToSend.setPaymentName("Coupon");
		 long accNum = a_account.getAccountNumber();
		 paymentToSend.addData(XMLPassivePayment.KEY_ACCOUNTNUMBER, new Long(accNum).toString());
		 long tan = transCert.getTransferNumber();
		 paymentToSend.addData(XMLPassivePayment.KEY_TRANSFERNUMBER, new Long(tan).toString());
		 
		 try
		 {
			 piConn = new BIConnection(a_account.getBI());
			 piConn.connect();
			 piConn.authenticate(a_account.getAccountCertificate(), a_account.getPrivateKey());
			 if (!piConn.sendPassivePayment(paymentToSend))
			 {
				 bValid = false;
			 }
			 piConn.disconnect();
		 }
		 catch (Exception e)
		 {
			 if (piConn != null)
			 {
				 try
				 {
					 piConn.disconnect();
				 }
				 catch (Exception ex)
				 {
				 }
			 }
			 throw e;
		 } 
		 if (!bValid)
		 {
			 throw new Exception("Coupon code was not accepted when charging at the payment instance!");
		 }
		
		return true;
	}
	
	/**
	 * 
	 * @param a_code
	 * @return
	 * @throws Exception if the account could not be created
	 */
	public static synchronized boolean validateCoupon(String a_code) throws Exception
	{
		Exception ex = null;
		if (ms_currentlyCreatedAccount == null)
		{
			try
			{
				ms_currentlyCreatedAccount = createAccount();
			}
			catch (Exception a_e)
			{
				ex = a_e;
			}
			if (ms_currentlyCreatedAccount == null || ex != null)
			{
				LogHolder.log(LogLevel.EMERG, LogType.PAY, 
						"Could not create account for PI " + Controller.getActivePaymentInstanceID() + "!");
				if (ex != null)
				{
					throw ex;
				}
				return false;
			}
			else
			{
				saveConfiguration();
			}
		}
		try
		{
			return activateCouponCode(a_code, ms_currentlyCreatedAccount, true);
		}
		catch (Exception a_e)
		{
			LogHolder.log(LogLevel.EXCEPTION, LogType.PAY, a_e);
			return false;
		}
	}
	
	/**
	 * Returns whether the user should recharge.
	 * @return whether the user should recharge
	 */
	public static synchronized boolean shouldRecharge()
	{
		if (getCurrentCredit(System.currentTimeMillis() + TIMEOUT_RECHARGE) == 0 ||
			getCurrentCredit() <= 500000000)
		{
			return true;
		}
		return false;
	}
	
	/**
	 * Returns the current credit in bytes.
	 * @return
	 */
	public static synchronized long getCurrentCredit()
	{
		return getCurrentCredit(System.currentTimeMillis());
	}
	
	
	private static synchronized long getCurrentCredit(long a_time)
	{
		long credits = 0;
		Vector accounts = PayAccountsFile.getInstance().getAccounts(getActivePaymentInstanceID());
		for (int i = 0; i < accounts.size(); i++)
		{
			if (((PayAccount)accounts.elementAt(i)).isCharged(new Timestamp(a_time)))
			{
				credits += ((PayAccount)accounts.elementAt(i)).getCurrentCredit() * 1000; // recalculate value to bytes
			}
		}
		return credits;
	}
	
	/**
	 * Call this method always before validation of the coupon code with "null" as
	 * argument. Then execute validateCoupon until the user gives up or the coupon code works.
	 * Then you may call this method again. If it succeeds, no further validation is 
	 * needed, as the account has been activated successfully.
	 * @param a_code
	 * @return
	 * @throws Exception
	 */
	public static synchronized boolean activateCoupon(String a_code) throws Exception
	{
		boolean bRet;
	
		if (ms_currentlyCreatedAccount == null)
		{
			return false;
		}
		
		PayAccount account = ms_currentlyCreatedAccount;
		ms_currentlyCreatedAccount = null;
		bRet = activateCouponCode(a_code, account, false);
		
		if (bRet)
		{
			try
			{
				account.fetchAccountInfo(true);
			}
			catch (Exception a_e)
			{
				LogHolder.log(LogLevel.EMERG, LogType.PAY, "A problem occured while updating your charged account!", a_e);
			}
			
			LogHolder.log(LogLevel.NOTICE, LogType.PAY, "You have created an account with " + account.getCurrentCredit() + " kbytes!");
			checkActiveAccount(account);
		}
		else
		{
			// this account seems to be unusable; delete it
			PayAccountsFile.getInstance().deleteAccount(account);
		}
		
		return bRet;
	}
	
	private static void checkActiveAccount(PayAccount a_account)
	{
		if (a_account == null)
		{
			return;
		}
		PayAccount accountActive = PayAccountsFile.getInstance().getActiveAccount();
		if (accountActive == null || accountActive.getCurrentCredit() == 0  ||
			(!accountActive.getPIID().equals(getActivePaymentInstanceID()) && 
				a_account.getPIID().equals(getActivePaymentInstanceID())))
		{
			PayAccountsFile.getInstance().setActiveAccount(a_account);
		}
	}
	
	/**
	 * Tells the program to save the configuration. Note that a configuration is automatically loaded, but might not
	 * be saved unless this method is called from outside this class.
	 * @throws Exception if an error occurs while saving the configuration
	 */
	public static synchronized void saveConfiguration() throws Exception
	{
		if (ms_configuration != null)
		{
			// delete accounts that are clearly unusable or already completely used
			Enumeration enumAccounts = PayAccountsFile.getInstance().getAccounts();
			PayAccount currentAccount;
			while (enumAccounts.hasMoreElements())
			{
				currentAccount = (PayAccount)enumAccounts.nextElement();
				if (currentAccount.getBalance() != null && currentAccount.getBalance().getSpent() > 0 && 
						currentAccount.getCurrentCredit() == 0)
				{
					PayAccountsFile.getInstance().deleteAccount(currentAccount);
				}
			}
			
			Document doc = XMLUtil.createDocument();
			Element root = doc.createElement(XML_ROOT_NODE);
			XMLUtil.setAttribute(root, XML_ATTR_LOG_DETAIL, LogHolder.getDetailLevel());
			XMLUtil.setAttribute(root, XML_ATTR_LOG_LEVEL, ms_logger.getLogLevel());
			
			doc.appendChild(root);
			doc.getDocumentElement().appendChild(PayAccountsFile.getInstance().toXmlElement(doc));
			doc.getDocumentElement().appendChild(TrustModel.toXmlElement(doc, TrustModel.XML_ELEMENT_CONTAINER_NAME));
			ms_configuration.write(XMLUtil.toString(doc));
		}
	}
	
	public static String exportAccounts(String a_password)
	{
		Document doc = XMLUtil.createDocument();
		doc.appendChild(PayAccountsFile.getInstance().toXmlElement(doc, a_password));
		return XMLUtil.toString(doc);
	}
	
	/**
	 * Optionally import without the possibility to enter a password.
	 * @param a_accountData
	 * @return
	 */
	public static boolean importAccounts(String a_accountData)
	{
		return importAccounts(a_accountData, null);
	}
	
	public static boolean importAccounts(String a_accountData, final char[] a_password)
	{
		boolean bImportSucceeded = importAccounts_internal(a_accountData, a_password);
		if (bImportSucceeded)
		{
			checkActiveAccount(PayAccountsFile.getInstance().getChargedAccount(getActivePaymentInstanceID()));
		}
		return bImportSucceeded;
	}
	
	private static boolean importAccounts_internal(String a_accountData, final char[] a_password)
	{
		Document doc;
		IMiscPasswordReader pwReader;
		
		try
		{
			doc = XMLUtil.toXMLDocument(a_accountData);
		}
		catch (XMLParseException a_e)
		{
			LogHolder.log(LogLevel.EXCEPTION, LogType.PAY, a_e);
			return false;
		}
		
		pwReader = new IMiscPasswordReader()
		{
			private boolean m_bTriedOnce = false;
			public synchronized String readPassword(Object message)
			{
				if (!m_bTriedOnce)
				{
					m_bTriedOnce = true;
					if (a_password == null)
					{
						return "";
					}
					return new String(a_password);
				}
				else
				{
					// send this password only once and then cancel
					return null;
				}
			}
		};
		
		if (doc.getDocumentElement().getNodeName().equals(PayAccount.XML_ELEMENT_NAME))
		{
			try 
			{
				PayAccountsFile.getInstance().addAccount(new PayAccount(doc.getDocumentElement(), pwReader));
				return true;
			} 
			catch (AccountAlreadyExistingException a_e) 
			{
				// just ignore
				LogHolder.log(LogLevel.ERR, LogType.PAY, a_e);
				return true;
				
			} 
			catch (Exception a_e) 
			{
				LogHolder.log(LogLevel.EXCEPTION, LogType.PAY, a_e);
				return false;
			}
		}
		else
		{
			try
			{
				return PayAccountsFile.getInstance().importAccounts(doc.getDocumentElement(), pwReader);
			}
			catch (Exception a_e)
			{
				LogHolder.log(LogLevel.EXCEPTION, LogType.PAY, a_e);
				return false;
			}
		}
	}
	
	private static class RunnableStarter implements Runnable
	{
		private MixCascade m_cascadeDefault;
		private boolean m_bFinished = false;
		
		public RunnableStarter(MixCascade a_cascade)
		{
			m_cascadeDefault = a_cascade;
		}
		public boolean isFinished()
		{
			return m_bFinished;
		}
		
		public void run()
		{
			try
			{
		        if (!ms_isUpdater.isFirstUpdateDone())
		        {
		        	LogHolder.log(LogLevel.NOTICE, LogType.NET, "Updating infoservices...");
		        	ms_isUpdater.update();
		        	LogHolder.log(LogLevel.NOTICE, LogType.NET, "Infoservices updated.");
		        }
		        if (!ms_paymentUpdater.isFirstUpdateDone())
		        {
		        	LogHolder.log(LogLevel.NOTICE, LogType.NET, "Updating payment instances...");
		        	ms_paymentUpdater.update();
		        	LogHolder.log(LogLevel.NOTICE, LogType.NET, "Payment instances updated.");
		        }
		        if (!ms_cascadeUpdater.isFirstUpdateDone())
		        {
		        	LogHolder.log(LogLevel.NOTICE, LogType.NET, "Updating Mix cascades...");
		        	ms_cascadeUpdater.update();
		        	LogHolder.log(LogLevel.NOTICE, LogType.NET, "Mix cascades updated.");
		        }
		        if (!ms_perfUpdater.isFirstUpdateDone())
		        {
		        	LogHolder.log(LogLevel.NOTICE, LogType.NET, "Updating performance data...");
		        	ms_perfUpdater.update();
		        	LogHolder.log(LogLevel.NOTICE, LogType.NET, "Performance data updated.");
		        }
		         
		        MixCascade cascade;
		        if (m_cascadeDefault != null)
		        {
		        	cascade = m_cascadeDefault;
		        }
		        else if (ms_jondonymProxy == null || ms_jondonymProxy.getMixCascade() == null)
		        {
			        cascade = (MixCascade)Database.getInstance(MixCascade.class).getRandomEntry();
		        }
		        else
		        {
		        	cascade = ms_jondonymProxy.getMixCascade();
		        }
		       
		        if (cascade == null)
		        {
		        	cascade = new MixCascade("-", "-", "0.0.0.0", 6544);
		        }
		        
		        ms_jondonymProxy = new AnonProxy(ms_socketListener, null);
		        ms_jondonymProxy.setHTTPHeaderProcessingEnabled(true);
		        ms_jondonymProxy.setJonDoFoxHeaderEnabled(true);
		        ms_jondonymProxy.setHTTPDecompressionEnabled(true);
		        ms_jondonymProxy.setDummyTraffic(DummyTrafficControlChannel.DT_MAX_INTERVAL_MS);
		        ms_serviceContainer = new AutoSwitchedMixCascadeContainer(cascade);
		        ms_jondonymProxy.start(ms_serviceContainer);
		        m_bFinished = true;
			}
			catch (Exception e)
			{
				LogHolder.log(LogLevel.EXCEPTION, LogType.MISC, e);
				m_bFinished = true;
				stop();
			}
			synchronized (SYNC_STARTER)
			{
				ms_starter = null;
				ms_starterThread = null;
			}
		}
	}
	
	public static boolean start()
	{
		return start(null);
	}
	
	protected static boolean start(MixCascade a_cascade)
	{
		if (m_bShuttingDown)
		{
			return false;
		}
		
		synchronized(SYNC_STARTER)
		{
			if (ms_starter != null || ms_socketListener != null)
			{
				// could not initialize startup thread as it is still starting up from another command call or we are already up
				return true;
			}
			
			try
			{
				ms_socketListener = new ServerSocket(4001);
			}
			catch (Exception e)
			{
				LogHolder.log(LogLevel.EXCEPTION, LogType.MISC, e);
				stop();
				return false;
			}
			
			ms_starter = new RunnableStarter(a_cascade);
			ms_starterThread = new Thread(ms_starter);
			ms_starterThread.start();
			return true;
		}
	}
	
	
	private static class AutoSwitchedMixCascadeContainer extends AbstractAutoSwitchedMixCascadeContainer
	{
		private boolean m_bAutoSwitched = true;
		public AutoSwitchedMixCascadeContainer(MixCascade a_cascade)
		{
			super(false, a_cascade);
		}

		public boolean isPaidServiceAllowed()
		{
			return false;
		}
		
		public void setServiceAutoSwitched(boolean a_bAutoSwitched)
		{
			m_bAutoSwitched = a_bAutoSwitched;
		}
		
		public boolean isServiceAutoSwitched()
		{
			return m_bAutoSwitched;
		}
		
		public boolean isReconnectedAutomatically()
		{
			return true;
		}
		
		public ITermsAndConditionsContainer getTCContainer()
		{
			return null;
		}
	}	
	
	private static class MixCascadeUpdater extends AbstractMixCascadeUpdater
	{
		public MixCascadeUpdater(ObservableInfo a_observableInfo)
		{
			super(a_observableInfo);
		}

		protected AbstractDatabaseEntry getPreferredEntry()
		{
			return null; // set current cascade here!
		}

		protected void setPreferredEntry(AbstractDatabaseEntry a_preferredEntry)
		{
			// do nothing
		}
	}
	
	public static class ForcePremiumIfAccountAvailableAttribute extends TrustAttribute
	{
		public ForcePremiumIfAccountAvailableAttribute(int a_trustCondition, Object a_conditionValue, boolean a_bIgnoreNoDataAvailable)
		{
			super(a_trustCondition, a_conditionValue, a_bIgnoreNoDataAvailable);
		}

		public void checkTrust(MixCascade a_cascade) throws TrustException, SignatureException
		{
			if (a_cascade.isPayment())
			{
				if (PayAccountsFile.getInstance().getChargedAccount(a_cascade.getPIID()) != null)
				{
					return;
				}
				else
				{
					throw new TrustException(MSG_NO_CHARGED_ACCOUNT);
				}
			}
			else if (PayAccountsFile.getInstance().getChargedAccount(getActivePaymentInstanceID()) != null)
			{
				throw new TrustException(TrustModel.MSG_EXCEPTION_FREE_CASCADE);
			}
		}
	};
}
