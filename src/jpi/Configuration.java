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
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.Properties;
import anon.crypto.IMyPrivateKey;
import anon.crypto.JAPCertificate;
import anon.crypto.PKCS12;
import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;
import anon.pay.xml.XMLPaymentOptions;
import anon.pay.xml.XMLPaymentOption;
import java.util.Enumeration;
import java.util.Vector;
import java.util.StringTokenizer;
import anon.infoservice.ListenerInterface;
import anon.crypto.X509SubjectKeyIdentifier;
import anon.pay.xml.XMLPaymentSettings;
import java.util.Hashtable;
import anon.pay.xml.XMLVolumePlans;
import anon.pay.xml.XMLVolumePlan;
import jpi.db.DBSupplier;

/**
 * Loads and stores all configuration data, keys
 * and certificates.
 */
public class Configuration
{
	/** Versionsnummer --> Please update if you change anything*/
	public static final String BEZAHLINSTANZ_VERSION = "BI.02.026";

	public static IMyPrivateKey getPrivateKey()
	{
		return m_privateKey;
	}

	private static Hashtable m_settingsInDb;

	/** private Constructor! */
	private Configuration()
	{}

	private static Configuration ms_Instance;

	/** return the one and only Configuration */
	public static Configuration getInstance()
	{
		if (ms_Instance == null)
		{
			ms_Instance = new Configuration();
		}
		return ms_Instance;
	}

	/** holds the public x509 certificate */
	private static JAPCertificate m_ownX509Certificate;

	/** returns the public x509 certificate */
	public static JAPCertificate getOwnCertificate()
	{
		return m_ownX509Certificate;
	}

	/** holds the JPI ID */
	private static String m_ID;

	/** returns the JPI ID */
	public static String getBiID()
	{
		return m_ID;
	}

	/** holds the JPI Name */
	private static String m_Name;

	/** returns the JPI Name */
	public static String getBiName()
	{
		if (m_Name == null)
		{
			return "Payment Instance " + m_ID;
		}
		return m_Name;
	}

	/** holds the database hostname */
	private static String m_dbHostname;

	/** returns the database hostname */
	public static String getDatabaseHost()
	{
		return m_dbHostname;
	}

	/** holds the database name */
	private static String m_dbDatabaseName;

	/** returns the database name */
	public static String getDatabaseName()
	{
		return m_dbDatabaseName;
	}

	/** holds the database username */
	private static String m_dbUsername;

	/** returns the database username */
	public static String getDatabaseUserName()
	{
		return m_dbUsername;
	}

	/** holds the database password */
	private static String m_dbPassword;

	/** returns the database password */
	public static String getDatabasePassword()
	{
		return m_dbPassword;
	}

	/** holds the database portnumber */
	private static int m_dbPort;

	/** returns the database portnumber */
	public static int getDatabasePort()
	{
		return m_dbPort;
	}

	/** Holds listener interfaces of infoservices */
	private static Vector ms_isListenerInterfaces = new Vector();

	/** Returns listener interfaces for infoservices */
	public static Enumeration getInfoservices()
	{
		return ms_isListenerInterfaces.elements();
	}

	/** holds the port where the JPI should listen for AI connections */
	//Elmar: m_AIPort is never set!? (ListenerInterface holding the AI connection contains the port, too)
	private static int m_AIPort;

	/** returns the port where the JPI should listen for AI connections */
	public static int getAIPort()
	{
		return m_AIPort;
	}

	/** holds maximum concurrent connections for accounting instances*/
	private static int ms_aiConnections;

	/** returns maximum concurrent connections for accounting instances*/
	public static int getMaxAiConnections()
	{
		return ms_aiConnections;
	}

	/** holds maximum concurrent connections for japs (per interface!)*/
	private static int ms_japConnections;

	/** holds maximum concurrent connections for japs (per interface!)*/
	public static int getMaxJapConnections()
	{
		return ms_japConnections;
	}

	/** Holds listener interfaces for jap connections*/
	private static Vector ms_japListenerInterfaces = new Vector();

	/** Holds listener interface (only one!) for accounting instance connections*/
	private static ListenerInterface ms_aiListenerInterface;

	/** Holds listener interface for connections from MixConfig tools (only one for now, why only one for AI, but several for JAP?*/
	private static ListenerInterface ms_mcListenerInterface;

	/** Holds the private key */
	private static IMyPrivateKey m_privateKey;

	/** Returns listener interfaces for jap connections*/
	public static Enumeration getJapListenerInterfaces()
	{
		return ms_japListenerInterfaces.elements();
	}

	/** Returns listener interface (only one!) for accounting instance connections*/
	public static ListenerInterface getAiListenerInterface()
	{
		return ms_aiListenerInterface;
	}


	/** Returns listener interface (only one!) for accounting instance connections*/
	public static ListenerInterface getMCListenerInterface()
	{
		return ms_mcListenerInterface;
	}

	private static int ms_mcMaxConnections;

	/** returns maximum concurrent connections for accounting instances*/
	public static int getMaxMCConnections()
	{
		return ms_mcMaxConnections;
	}

	private static int ms_mpMaxConnections;
	public static int getMaxMPConnections()
	{
		return ms_mpMaxConnections;
	}

	private static ListenerInterface ms_mpListenerInterface;
	public static ListenerInterface getMPListenerInterface()
	{
		return ms_mpListenerInterface;
	}


	private static String ms_MaxCascadeLength;
	public static String getMaxCascadeLength()
	{
		return ms_MaxCascadeLength;
	}

	//should payment statistic be logged?
	private static boolean mb_logPaymentStatsEnabled;
	public static Boolean isLogPaymentStatsEnabled()
	{
		return new Boolean(mb_logPaymentStatsEnabled);
	}

	//**************payment settings for flatrate ******************

	private static boolean mb_flatEnabled;
	//returns Object instead of primitive var for purposes of Reflection
	public static Boolean isFlatEnabled()
	{
		return new Boolean(mb_flatEnabled);
	}

	private static boolean mb_volumeLimited;
	//returns Object instead of primitive var for purposes of Reflection
	public static Boolean isVolumeLimited()
	{
		return new Boolean(mb_volumeLimited);
	}

	private static long ml_volumeAmount;
	//returns Object instead of primitive var for purposes of Reflection
	public static Long getVolumeAmount()
	{
		return new Long(ml_volumeAmount);
	}

	private static boolean mb_durationLimited;
	//returns Object instead of primitive var for purposes of Reflection
	public static Boolean isDurationLimited()
	{
		return new Boolean(mb_durationLimited);
	}

	private static int mi_flatrateDuration;
	//returns Integer instead of int for purposes of Reflection
	public static Integer  getFlatrateDuration()
	{
		return new Integer(mi_flatrateDuration);
	}

	private static String ms_flatrateDurationUnit;
	public static String getFlatrateDurationUnit()
	{
		return ms_flatrateDurationUnit;
	}

	private static int mi_flatratePrice;
	//returns Integer instead of int for purposes of Reflection
	public static Integer getFlatratePrice()
	{
		return new Integer(mi_flatratePrice);
	}

	//***************  paysafecard configuration ****************

	private static String ms_merchantId ;
	public static String getMerchantId()
	{
		return ms_merchantId ;
	}

	private static String ms_merchantName ;
	public static String getMerchantName()
	{
		return ms_merchantName ;
	}

	private static String ms_clientCert ;
	public static String getClientCert()
	{
		return ms_clientCert ;
	}

	private static String ms_clientCertPassword ;
	public static String getClientCertPassword()
	{
		return ms_clientCertPassword ;
	}

	private static String ms_businessType ;
	public static String getBusinessType()
	{
		return ms_businessType ;
	}

	private static String ms_okUrl ;
	public static String getOkUrl()
	{
		return ms_okUrl ;
	}

	private static String ms_nokUrl ;
	public static String getNokUrl()
	{
		return ms_nokUrl ;
	}

	private static String ms_confirmUrl ;
	public static String getConfirmUrl()
	{
		return ms_confirmUrl ;
	}

	private static int mi_dispositionTimeout;
	public static Integer getDispositionTimeout()
	{
		return new Integer(mi_dispositionTimeout);
	}

	private static int mi_cleanupInterval;
	public static Integer getCleanupInterval()
	{
		return new Integer(mi_cleanupInterval);
	}

	private static int mi_logExpiration;
	public static Integer getLogExpiration()
	{
		return new Integer(mi_logExpiration);
	}


	/************* micropayment configuration ***************/

	private static boolean mb_isMicropaymentEnabled;
	public static Boolean isMicropaymentEnabled()
	{
		return new Boolean(mb_isMicropaymentEnabled);
	}


	private static boolean mb_isSignatureOnPriceRequired;
	public static Boolean isSignatureOnPriceRequired()
	{
		return new Boolean(mb_isSignatureOnPriceRequired);
	}

	/** Holds threshold for logging to stderr */
	private static int m_LogStderrThreshold;

	/** Returns threshold for logging to stderr */
	public static int getLogStderrThreshold()
	{
		return m_LogStderrThreshold;
	}

	/** Holds threshold for logging to log file */
	private static int m_LogFileThreshold;

	/** Returns threshold for logging to log file */
	public static int getLogFileThreshold()
	{
		return m_LogFileThreshold;
	}

	/** Holds log file name*/
	private static String m_LogFileName = null;

	/** Returns log file name*/
	public static String getLogFileName()
	{
		return m_LogFileName;
	}

	/** Holds the payment options  - only filled from file */
	private static XMLPaymentOptions ms_paymentOptions = new XMLPaymentOptions();
	/** Holds the volumeplans  - only filled from file */
	private static XMLVolumePlans ms_volumePlans = new XMLVolumePlans();

	/** Returns the payment options as stored in the config file */
	/* retained for compatibility, and to initially load the config from the file into the database
	 * whenever PaymentOptions are used, you should call getPaymentOptions() without arguments instead
	 * to get the most current values from the database
	 */
	public static XMLPaymentOptions getPaymentOptions(boolean fromFile)
	{
		return ms_paymentOptions;
	}

	public static XMLPaymentOptions getPaymentOptions(){
		try
		{
			return DBSupplier.getDataBase().getPaymentOptionsFromDb();
		} catch (Exception e)
		{
			LogHolder.log(LogLevel.DEBUG, LogType.PAY, "Could not get payment options from the database," +
			"returning payments options from config file, you may or may not want this");
			return ms_paymentOptions;
		}
	}

	/**
	 *
	 * @param fromFile boolean: just a marker parameter, value doesnt matter
	 * @return XMLVolumePlans: as stored in the config file
	 */
	public static XMLVolumePlans getVolumePlans(boolean fromFile)
	{
		return ms_volumePlans;
	}

	public static XMLVolumePlans getVolumePlans(){
		try
		{
			return DBSupplier.getDataBase().getVolumePlans();
		}
		catch (Exception e)
		{
			LogHolder.log(LogLevel.DEBUG, LogType.PAY, "Could not get volume plans from the database," +
						  "returning volume plans from config file, you may or may not want this");
			return ms_volumePlans;
		}
	}

	/** Holds the credit card helper class name. The class is named *CreditCardHelper.
	 * The configuration file must provide "*" (e.g. Dummy)
	 */
	private static String ms_creditCardHelper;

	/** Returns the credit card helper class name*/
	public static String getCreditCardHelper()
	{
		return ms_creditCardHelper;
	}

	private static String ms_acceptedCreditCards;

	public static String getAcceptedCreditCards()
	{
		return ms_acceptedCreditCards;
	}

	/**Holds the port where the JPI receives PayPal IPNs */
	private static String ms_payPalport;

	/**Returns the port where the JPI receives PayPal IPNs */
	public static String getPayPalPort()
	{
		return ms_payPalport;
	}


	/** Holds the rate per megabyte (in cents)
	 *  not in use anymore after the introduction of price certificates */

	private static double ms_ratePerMB;


	/** Returns the rate per megabyte (in cents)
	 *  not in use anymore after the introduction of price certificates */
	public static double getRatePerMB()
	{
		return ms_ratePerMB;
	}

	/** Holds password for external charging */
	private static String ms_externalChargePassword;

	/** Returns password for external charging */
	public static String getExternalChargePassword()
	{
		return ms_externalChargePassword;
	}

	/**Holds the port for external charging */
	private static String ms_externalChargePort;

	/**Returns the port for external charging */
	public static String getExternalChargePort()
	{
		return ms_externalChargePort;
	}

	/** Path to keyfile */
	public static String ms_keyFile;
	/** Returns the path to the keyfile */
	public static String getKeyFile()
	{
		return ms_keyFile;
	}

	/** Keyfile password */
	public static String ms_keyFilePassword;


	/** Returns the keyfile password */
	public static String getKeyFilePassword()
	{
		return ms_keyFilePassword;
	}

	/**
	 * Load configuration from properties file,
	 * initialize keys and certificates,
	 * and ask the user for all missing passwords
	 */
	public static boolean init(String configFileName)
	{

		// Load Properties file
		FileInputStream in;
		Properties props = new Properties();
		try
		{
			in = new FileInputStream(configFileName);
			props.load(in);
			in.close();
		}
		catch (java.io.IOException e)
		{
			LogHolder.log(LogLevel.ALERT, LogType.PAY,
						  "Could not read config file " + configFileName);
			LogHolder.log(LogLevel.EXCEPTION, LogType.PAY, e);
			return false; // Panic!
		}


		// parse Name (the name of the BI)
		m_Name = props.getProperty("name");

		// parse network configuration
		try
		{
			String japlisteners = props.getProperty("japlisteners");
			StringTokenizer st = new StringTokenizer(japlisteners, ",");
			while (st.hasMoreTokens())
			{
				String listener = st.nextToken();
				StringTokenizer st2 = new StringTokenizer(listener, ":");
				ListenerInterface l = new ListenerInterface(st2.nextToken(), Integer.parseInt(st2.nextToken()));
				ms_japListenerInterfaces.addElement(l);
			}

			String ailistener = props.getProperty("ailistener");
			st = new StringTokenizer(ailistener, ":");
			while (st.hasMoreTokens())
			{
				ms_aiListenerInterface = new ListenerInterface(st.nextToken(), Integer.parseInt(st.nextToken()));
			}

	        String mcListener = props.getProperty("mclistener");
			st = new StringTokenizer(mcListener, ":");
			while (st.hasMoreTokens() )
	        {
				ms_mcListenerInterface = new ListenerInterface(st.nextToken(), Integer.parseInt(st.nextToken()) );
			}

			String mpListener = props.getProperty("mplistener");
			st = new StringTokenizer(mpListener, ":");
			while (st.hasMoreTokens() )
			{
				ms_mpListenerInterface = new ListenerInterface(st.nextToken(), Integer.parseInt(st.nextToken()) );
			}

		}
		catch (Exception e)
		{
			LogHolder.log(LogLevel.ERR, LogType.PAY,
						  "ailistener, japlisteners and mclistener in configfile '" +
						  configFileName +
						  "' must be specified and must be in this format: host:port(,host2:port2...). ailistener and mclistener may only contain one interface"
				);
			LogHolder.log(LogLevel.EXCEPTION, LogType.PAY, e);
			return false;
		}

		// parse Logger Configuration
		m_LogFileName = props.getProperty("logfilename");
		try
		{
			m_LogStderrThreshold = Integer.parseInt(props.getProperty(
				"logstderrthreshold"));
			m_LogFileThreshold = Integer.parseInt(props.getProperty(
				"logfilethreshold"));
		}
		catch (NumberFormatException e)
		{
			m_LogStderrThreshold = 1;
			m_LogFileThreshold = 2;
		}

		//parse maximum cascade length
		ms_MaxCascadeLength = props.getProperty("maxcascadelength");

		// parse database configuration
		m_dbHostname = props.getProperty("dbhost");
		m_dbPassword = props.getProperty("dbpassword");
		try
		{
			m_dbPort = Integer.parseInt(props.getProperty("dbport"));
		}
		catch (NumberFormatException e)
		{
			LogHolder.log(LogLevel.ERR, LogType.PAY,
						  "dbport in configfile '" + configFileName +
						  "' should be a NUMBER!");
			return false;
		}
		m_dbUsername = props.getProperty("dbusername");
		m_dbDatabaseName = props.getProperty("dbname");

		// If db password was not specified, ask the user
		if (m_dbPassword == null || m_dbPassword.equals(""))
		{
			System.out.println(
				"Please enter the password for connecting to the\n" +
				"PostgreSQL server at " + m_dbHostname + ":" + m_dbPort);
			System.out.print("Password: ");
			BufferedReader passwordReader = new BufferedReader(
				new InputStreamReader(System.in)
				);
			try
			{
				m_dbPassword = passwordReader.readLine();
			}
			catch (java.io.IOException e)
			{
				LogHolder.log(LogLevel.ERR, LogType.PAY,
							  "Error reading password from stdin.. strange!");
				LogHolder.log(LogLevel.EXCEPTION, LogType.PAY, e);
			}
		}

		// initialize private signing key
		String password = props.getProperty("keyfilepassword");
		String keyFileName = props.getProperty("keyfile");

		// If the keyfile password was not specified, ask the user
		if (password == null || m_dbPassword.equals(""))
		{
			System.out.println("Please enter the password for decrypting the\n" +
							   "PKCS12 private key file " + keyFileName);
			System.out.print("Password: ");
			BufferedReader passwordReader = new BufferedReader(
				new InputStreamReader(System.in)
				);
			try
			{
				password = passwordReader.readLine();
			}
			catch (java.io.IOException e)
			{
				LogHolder.log(LogLevel.ERR, LogType.PAY,
							  "Error reading password from stdin.. strange!");
				LogHolder.log(LogLevel.EXCEPTION, LogType.PAY, e);
			}
		}

		try
		{
			LogHolder.log(LogLevel.DEBUG, LogType.PAY,
						  "Trying to load PKCS12 file " + keyFileName +
						  " with password '" + password +
						  "'.");
			PKCS12 ownPkcs12 = PKCS12.getInstance(
				new FileInputStream(keyFileName),
				password.toCharArray()
				);
			ms_keyFile = keyFileName;
			ms_keyFilePassword = password;
			m_privateKey = ownPkcs12.getPrivateKey();
			/* get the public certificate */
			m_ownX509Certificate = JAPCertificate.getInstance(ownPkcs12.
				getX509Certificate());
		}
		catch (Exception e)
		{
			LogHolder.log(LogLevel.ALERT, LogType.PAY,
						  "Error loading private key file " + keyFileName);
			LogHolder.log(LogLevel.EXCEPTION, LogType.PAY, e);
			return false;
		}

		// parse ID (the unique BI-Id)
		//m_ID = props.getProperty("id");
		//changed to use the certificate's subjectkeyidentifier instead of an arbitrary String as id
		m_ID = new X509SubjectKeyIdentifier(m_ownX509Certificate.getPublicKey()).getValueWithoutColon();


		// parse infoservice configuration
		try
		{
			String islisteners = props.getProperty("infoservices");
			StringTokenizer st = new StringTokenizer(islisteners, ",");
			while (st.hasMoreTokens())
			{
				String listener = st.nextToken();
				StringTokenizer st2 = new StringTokenizer(listener, ":");
				ListenerInterface l = new ListenerInterface(st2.nextToken(), Integer.parseInt(st2.nextToken()),
					ListenerInterface.PROTOCOL_TYPE_HTTP);
				ms_isListenerInterfaces.addElement(l);
			}
		}
		catch (Exception e)
		{
			LogHolder.log(LogLevel.ERR, LogType.PAY,
						  "infoservices in configfile '" +
						  configFileName +
						  "' must be specified and must be in this format: host:port(,host2:port2...)."
				);
			LogHolder.log(LogLevel.EXCEPTION, LogType.PAY, e);
			return false;
		}

		//parse payment settings
		//for backwards compatibility only, use plan<X> instead
		mb_flatEnabled = Boolean.getBoolean(props.getProperty("flat_enabled","true") );
		mb_durationLimited = Boolean.getBoolean(props.getProperty("duration_limited","true") );
		mb_volumeLimited = Boolean.getBoolean(props.getProperty("volume_limited","true") );
		mi_flatrateDuration = Integer.parseInt(props.getProperty("flatrate_duration","2") );
		ms_flatrateDurationUnit = props.getProperty("flatrate_duration_unit","months");
		ml_volumeAmount = Long.parseLong(props.getProperty("volume_amount","4000000") );
		mi_flatratePrice  = Integer.parseInt(props.getProperty("flatrate_price","500") );

		//parse paysafecard settings
		ms_merchantId = props.getProperty("merchantid","1010000456");
		ms_merchantName = props.getProperty("merchantname","An.ON");
		ms_clientCert = props.getProperty("clientcert","CMS_1010000456_001_uni-regensburg.pem");
		ms_clientCertPassword = props.getProperty("clientcertpassword","df7HN834bk");
		ms_okUrl = props.getProperty("okurl","http://anon.inf.tu-dresden.de/psc_okay.html");
		ms_nokUrl = props.getProperty("nokurl","http://anon.inf.tu-dresden.de/psc_failed.html");
		ms_businessType = props.getProperty("businesstype","I");
		ms_confirmUrl = props.getProperty("confirm_url","https://customer.test.at.paysafecard.com/psccustomer/GetCustomerPanelServlet");
		mi_dispositionTimeout = Integer.parseInt(props.getProperty("disposition_timeout","60"));
		mi_cleanupInterval = Integer.parseInt(props.getProperty("cleanup_interval","10"));
		mi_logExpiration = Integer.parseInt(props.getProperty("log_expiration","7"));

		//parse micropayment settings
		mb_isMicropaymentEnabled = Boolean.getBoolean(props.getProperty("enable_micropayment","false"));


		//parse payment statistics settings
		String configValue = props.getProperty("log_payment_stats_enabled","true"); //debug
		mb_logPaymentStatsEnabled = (new Boolean(configValue)).booleanValue();
		ms_mpMaxConnections = Integer.parseInt(props.getProperty("mpconnections","10"));

		//parse payment options
		//parse currencies
		int i = 1;
		while (true)
		{
			String currency = props.getProperty("currency" + i);
			if (currency == null)
			{
				break;
			}
			ms_paymentOptions.addCurrency(currency.toUpperCase());

			i++;
		}

		//parse accepted credit cards
		ms_acceptedCreditCards = props.getProperty("acceptedcards","");
		ms_paymentOptions.setAcceptedCreditCards(ms_acceptedCreditCards);

		//parse options
		i = 1;
		while (true)
		{
			String name = props.getProperty("option" + i + "name");
			if (name == null)
			{
				break;
			}
			String type = props.getProperty("option" + i + "type");
			if (type == null)
			{
				break;
			}
			String generic = props.getProperty("option" + i + "generic");
			if (generic == null)
			{
				generic = "true";
			}

			String japversion = props.getProperty("option" + i + "japversion");
			if (japversion == null)
			{
				japversion = "00.00.000";
			}

			XMLPaymentOption option = new XMLPaymentOption(name, type, Boolean.valueOf(generic).booleanValue(),
				japversion);
/*			String japversion = props.getProperty("option" + i + "japversion");
			if (japversion == null)
			{
				japversion = Util.VERSION_FORMAT;
			}

			XMLPaymentOption option = new XMLPaymentOption(name, type, Boolean.valueOf(generic).booleanValue(), japversion);
*/
			//Add headings
			String heading;
			String headingLang;
			int j = 1;
			while (true)
			{
				heading = props.getProperty("option" + i + "heading" + j);
				headingLang = props.getProperty("option" + i + "headinglang" + j);
				if (heading == null || headingLang == null)
				{
					break;
				}
				option.addHeading(heading, headingLang);
				j++;
			}

			//Add detailed infos
			String info;
			String infoLang;
			j = 1;
			while (true)
			{
				info = props.getProperty("option" + i + "detailedinfo" + j);
				infoLang = props.getProperty("option" + i + "detailedinfolang" + j);
				if (info == null || infoLang == null)
				{
					break;
				}
				option.addDetailedInfo(info, infoLang);
				j++;
			}

			//Add extra infos
			String extra;
			String extraLang;
			String extraType;
			j = 1;
			while (true)
			{
				extra = props.getProperty("option" + i + "extrainfo" + j);
				extraLang = props.getProperty("option" + i + "extrainfolang" + j);
				extraType = props.getProperty("option" + i + "extrainfotype" + j);
				if (extra == null || extraLang == null || extraType == null)
				{
					break;
				}
				option.addExtraInfo(extra, extraType, extraLang);
				j++;
			}

			//Add input fields
			String input;
			String inputLang;
			String inputRef;
			j = 1;
			while (true)
			{
				input = props.getProperty("option" + i + "input" + j);
				inputLang = props.getProperty("option" + i + "inputlang" + j);
				inputRef = props.getProperty("option" + i + "inputref" + j);
				if (input == null || inputLang == null || inputRef == null)
				{
					break;
				}
				option.addInputField(inputRef, input, inputLang);
				j++;
			}

			ms_paymentOptions.addOption(option);
			i++;
		}

		//parse volume plans
		i = 1;
		while (true)
		{
			String name = props.getProperty("plan"+i+"name");
			if (name == null) //no further volume plans exist
			{
				break;
			}
			//Assumption: if another option<X>name exists, all the other params will be set for this <X>, too
			int price = Integer.parseInt(props.getProperty("plan"+i+"price"));
			boolean durationLimited = Boolean.valueOf(props.getProperty("plan"+i+"durationlimited")).booleanValue();
			boolean volumeLimited = Boolean.valueOf(props.getProperty("plan"+i+"volumelimited")).booleanValue();
			int volumekbytes = Integer.parseInt(props.getProperty("plan"+i+"volumekbytes"));
			int duration = Integer.parseInt(props.getProperty("plan"+i+"duration"));
			String durationunit = props.getProperty("plan"+i+"durationunit");

			XMLVolumePlan curPlan = new XMLVolumePlan(name,price,durationLimited,volumeLimited,duration,durationunit,volumekbytes);
			ms_volumePlans.addVolumePlan(curPlan);
			i++;
		}

		// Parse the CreditCardHelper classname
		ms_creditCardHelper = props.getProperty("creditcardhelper", "Dummy");

		//Parse the PayPal IPN port
		ms_payPalport = props.getProperty("paypalport", "9999");

		//Parse rate per MB
		ms_ratePerMB = Double.parseDouble(props.getProperty("ratepermb", "1.0"));

		//Parse external charge password
		ms_externalChargePassword = props.getProperty("chargepw", "");

		//Parse external charge port
		ms_externalChargePort = props.getProperty("chargeport", "9950");

		//Parse max connections
		ms_aiConnections = Integer.parseInt(props.getProperty("aiconnections", "5"));
		ms_japConnections = Integer.parseInt(props.getProperty("japconnections", "25"));
		ms_mcMaxConnections = Integer.parseInt(props.getProperty("mcconnections","10"));


		//get those settings that are stored in the database's paymentsettings table from the database
		/*** not necessary, since updater thread is started, too ****** /
		DBInterface dbConn = null;
		try
		{
			dbConn = DBSupplier.getDataBase();
			updateSettings(dbConn.getPaymentSettings() );
		}
		catch (Exception ex)
		{
			LogHolder.log(LogLevel.ERR, LogType.PAY, "Configuration.init() could not access database to get payment settings");
		}
		*******/
		return true;
	}

	/** was meant to be used in a loop setting variables via reflection
	 *  unfortunately we have to do it by hand, since we can't access
	 *  private member varialbes via reflection

	 // add any option which you wish to be configurable in the database here
	// Format: .put("name_in_configfile","ms_memberVariable");
	// db always supplies Strings, will be parsed according to the type of the member variable
	 //
	private static void whichSettingsInDatabase()
	{
		m_settingsInDb = new Hashtable();
		//flatrate options
		m_settingsInDb.put("flat_enabled","");
		m_settingsInDb.put("duration_limited","");
		m_settingsInDb.put("flatrate_duration","");
		m_settingsInDb.put("flatrate_duration_unit","");
		m_settingsInDb.put("volume_limited","");
		m_settingsInDb.put("volume_amount","");
		m_settingsInDb.put("flatrate_price","");
		m_settingsInDb.put("is_signature_on_price_required","");
		//paysafecard options
		m_settingsInDb.put("","");

	}
    ***********/

   /**
	* sets the internal varialbes of Configuration to current values from the database
	*
	* @param allSettings XMLPaymentSettings: a collection of payment settings gotten from the database
	*/
   public static void updateSettings(XMLPaymentSettings allSettings)
	{
		String dbValue;
		/******** logging settings *****/
		dbValue = allSettings.getSettingValue("log_payment_stats_enabled");
		if (dbValue != null)
		{
			mb_logPaymentStatsEnabled = Boolean.getBoolean(dbValue);
		}

		/*** flatrate settings ***/
		dbValue = allSettings.getSettingValue("flat_enabled");
		if (dbValue != null)
		{
			mb_flatEnabled = Boolean.valueOf(dbValue).booleanValue();
		}

		dbValue = allSettings.getSettingValue("duration_limited");
		if (dbValue != null)
		{
			mb_durationLimited = Boolean.valueOf(dbValue).booleanValue();
		}

		dbValue = allSettings.getSettingValue("flatrate_duration");
		if (dbValue != null)
		{
			mi_flatrateDuration = Integer.parseInt(dbValue);
		}

		dbValue = allSettings.getSettingValue("flatrate_duration_unit");
		if (dbValue != null)
		{
			ms_flatrateDurationUnit = dbValue;
		}

		dbValue = allSettings.getSettingValue("volume_limited");
		if (dbValue != null)
		{
			mb_volumeLimited = Boolean.valueOf(dbValue).booleanValue();
		}

		dbValue = allSettings.getSettingValue("volume_amount");
		if (dbValue != null)
		{
			ml_volumeAmount = Long.parseLong(dbValue);
		}

		dbValue = allSettings.getSettingValue("flatrate_price");
		if (dbValue != null)
		{
			mi_flatratePrice = Integer.parseInt(dbValue);
		}

		dbValue = allSettings.getSettingValue("is_signature_on_price_required");
		if (dbValue != null)
		{
			mb_isSignatureOnPriceRequired = Boolean.valueOf(dbValue).booleanValue();
		}

		/********** paysafecard settings ***************/
		dbValue = allSettings.getSettingValue("okurl");
		if (dbValue != null)
		{
			ms_okUrl = dbValue;
		}

		dbValue = allSettings.getSettingValue("nokurl");
		if (dbValue != null)
		{
			ms_nokUrl = dbValue;
		}

		dbValue = allSettings.getSettingValue("confirm_url");
		if (dbValue != null)
		{
			ms_confirmUrl = dbValue;
		}

		dbValue = allSettings.getSettingValue("disposition_timeout");
		if (dbValue != null)
		{
			mi_dispositionTimeout = Integer.parseInt(dbValue);
		}

		dbValue = allSettings.getSettingValue("cleanup_interval");
		if (dbValue != null)
		{
			mi_cleanupInterval = Integer.parseInt(dbValue);
		}

		dbValue = allSettings.getSettingValue("log_expiration");
		if (dbValue != null)
		{
			mi_logExpiration = Integer.parseInt(dbValue);
		}


	}
}
