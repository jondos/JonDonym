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
import anon.pay.xml.XMLGenericStrings;
import anon.pay.xml.XMLPassivePayment;
import anon.pay.xml.XMLTransCert;
import anon.proxy.AnonProxy;
import anon.infoservice.AbstractDatabaseEntry;
import anon.infoservice.Database;
import anon.infoservice.IDistributable;
import anon.infoservice.IDistributor;
import anon.infoservice.IMutableProxyInterface;
import anon.infoservice.InfoServiceDBEntry;
import anon.infoservice.InfoServiceHolder;
import anon.infoservice.MixCascade;
import anon.infoservice.update.AbstractMixCascadeUpdater;
import anon.infoservice.update.InfoServiceUpdater;
import anon.infoservice.update.PaymentInstanceUpdater;
import logging.AbstractLog4jLog;
import logging.Log;
import logging.LogHolder;
import logging.LogType;
import logging.LogLevel;
import logging.SystemErrLog;
import anon.client.AbstractAutoSwitchedMixCascadeContainer;
import anon.client.DummyTrafficControlChannel;
import anon.client.ITermsAndConditionsContainer;
import anon.client.TrustModel;
import anon.crypto.JAPCertificate;
import anon.crypto.SignatureVerifier;
import anon.util.ClassUtil;
import anon.util.Configuration;
import anon.util.JAPMessages;
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
	private static final String VERSION = "00.00.004";
	private static final String XML_ROOT_NODE = "ConsoleController";
	
	private static final String PI_JONDOS = "ECD365B98453B316B210B55546731F52DA445F40";
	//private static final String PI_TEST = "3ADE1713CAFA6470FADCC3395415F8950C42CD2E";
	
	private static InfoServiceUpdater ms_isUpdater;
	private static MixCascadeUpdater ms_cascadeUpdater;
	private static PaymentInstanceUpdater ms_paymentUpdater;
	private static ServerSocket ms_socketListener;
	private static AnonProxy ms_jondonymProxy;
	private static AutoSwitchedMixCascadeContainer ms_serviceContainer;
	private static String ms_currentPIID = PI_JONDOS;
	private static Configuration ms_configuration;
	
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
		
		Log templog;
		if (a_logger == null)
		{
			templog = new SystemErrLog();
		}
		else
		{
			templog = new AbstractLog4jLog()
			{
				  protected Logger getLogger()
					{
						return a_logger;
					}
			};
		}
		
   		LogHolder.setLogInstance(templog);
   		templog.setLogType(LogType.ALL);
   		templog.setLogLevel(LogLevel.WARNING);
   		
   		LogHolder.log(LogLevel.ALERT, LogType.MISC, "Initialising " + ClassUtil.getClassNameStatic() + " version " + VERSION + "...");
   		
   		JAPMessages.init("JAPMessages");
   		
		ClassUtil.enableFindSubclasses(false); // This would otherwise start non-daemon AWT threads, blow up memory and prevent closing the app.
		XMLUtil.setStorageMode(XMLUtil.STORAGE_MODE_AGRESSIVE); // Store as few XML data as possible for memory optimization.
		SignatureVerifier.getInstance().setCheckSignatures(true);
		
		Util.addDefaultCertificates("acceptedInfoServiceCAs/", JAPCertificate.CERTIFICATE_TYPE_ROOT_INFOSERVICE);
		Util.addDefaultCertificates("acceptedMixCAs/", JAPCertificate.CERTIFICATE_TYPE_ROOT_MIX);
		Util.addDefaultCertificates("acceptedPIs/", JAPCertificate.CERTIFICATE_TYPE_PAYMENT);
     
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
				Element root = XMLUtil.toXMLDocument(conf).getDocumentElement();
				Element elem;
				XMLUtil.removeComments(root);
				XMLUtil.assertNodeName(root, XML_ROOT_NODE);
				elem = (Element)XMLUtil.getFirstChildByName(root, PayAccountsFile.XML_ELEMENT_NAME);
				if (elem != null)
				{
					PayAccountsFile.init(elem, null, false);
				}
				elem = (Element)XMLUtil.getFirstChildByName(root, TrustModel.XML_ELEMENT_CONTAINER_NAME);
				if (elem != null)
				{
					TrustModel.fromXmlElement(elem);
					if (TrustModel.getCurrentTrustModel() == TrustModel.getTrustModelUserDefined())
					{
						TrustModel.setCurrentTrustModel(TrustModel.getTrustModelDefault());
					}
				}
			}
			else
			{
				LogHolder.log(LogLevel.WARNING, LogType.MISC, "Configuration is empty!");
			}
		}
	}
	
	public static synchronized void stop()
	{
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
	}
	
	public static synchronized void switchCascade()
	{
		if (ms_jondonymProxy != null && ms_serviceContainer != null)
		{
			ms_serviceContainer.setCurrentCascade(ms_serviceContainer.getNextCascade());
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
		if (ms_jondonymProxy == null)
		{
			return null;
		}
		return ms_jondonymProxy.getMixCascade();
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
	
	public static PayAccount createAccount() throws Exception
	{
		PaymentInstanceDBEntry entry = PayAccountsFile.getInstance().getBI(ms_currentPIID);
		
		if (entry == null)
		{
			return null;
		}
		return PayAccountsFile.getInstance().createAccount(entry, 
				new IMutableProxyInterface.DummyMutableProxyInterface(), null);
	}
	
	public static boolean activateCouponCode(String a_code, PayAccount a_account, boolean a_bPreCheck) throws Exception
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
		if (a_bPreCheck)
		{
			bValid = false;
			try
			{
				piConn = new BIConnection(a_account.getBI());
				piConn.connect(new IMutableProxyInterface.DummyMutableProxyInterface());
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
		XMLTransCert transCert = a_account.charge(new IMutableProxyInterface.DummyMutableProxyInterface(), requestData);
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
			 piConn.connect(new IMutableProxyInterface.DummyMutableProxyInterface());
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
	 * Tells the program to save the configuration. Note that a configuration is automatically loaded, butt will not
	 * be save unless this method is called from outside this class.
	 * @throws Exception if an error occurs while saving the configuration
	 */
	public static synchronized void saveConfiguration() throws Exception
	{
		if (ms_configuration != null)
		{
			Document doc = XMLUtil.createDocument();
			doc.appendChild(doc.createElement(XML_ROOT_NODE));
			doc.getDocumentElement().appendChild(PayAccountsFile.getInstance().toXmlElement(doc));
			doc.getDocumentElement().appendChild(TrustModel.toXmlElement(doc, TrustModel.XML_ELEMENT_CONTAINER_NAME));
			ms_configuration.write(XMLUtil.toString(doc));
		}
	}
	
	public static synchronized void start()
	{
		start(null);
	}
	
	protected static synchronized void start(MixCascade a_cascade)
	{
		if (ms_socketListener != null)
		{
			return;
		}
		
		try
		{
			ms_socketListener = new ServerSocket(4001);
			
	        ms_isUpdater.start(true);
	        ms_isUpdater.update();
	        ms_paymentUpdater.start(true);
	        ms_paymentUpdater.update();
	        ms_cascadeUpdater.start(true);
	        ms_cascadeUpdater.update();
	         
	        MixCascade cascade;
	        if (a_cascade != null)
	        {
	        	cascade = a_cascade;
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
	        	// cascade = new MixCascade("mix.inf.tu-dresden.de", 6544);
	        	cascade = new MixCascade("none", 6544);
	        }
	        
	        ms_jondonymProxy = new AnonProxy(ms_socketListener, null,null);
	        ms_jondonymProxy.setDummyTraffic(DummyTrafficControlChannel.DT_MAX_INTERVAL_MS);
	        ms_serviceContainer = new AutoSwitchedMixCascadeContainer(cascade);
	        ms_jondonymProxy.start(ms_serviceContainer);
		}
		catch (Exception e)
		{
			LogHolder.log(LogLevel.EXCEPTION, LogType.MISC, e);
			stop();
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
}
