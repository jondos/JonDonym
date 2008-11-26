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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Vector;
import java.util.Locale;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.TimeZone;

import anon.util.XMLParseException;

import anon.crypto.JAPCertificate;
import anon.crypto.PKCS12;
import anon.crypto.X509SubjectKeyIdentifier;
import anon.crypto.SignatureCreator;
import anon.crypto.SignatureVerifier;
import anon.infoservice.Constants;
import anon.infoservice.ListenerInterface;
import anon.infoservice.Database;
import anon.infoservice.TermsAndConditionsFramework;
import infoservice.tor.TorDirectoryAgent;
import infoservice.tor.TorDirectoryServer;
import infoservice.tor.TorDirectoryServerUrl;
import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;
import infoservice.tor.MixminionDirectoryAgent;
import anon.infoservice.JavaVersionDBEntry;
import anon.infoservice.MessageDBEntry;
import anon.infoservice.InfoServiceDBEntry;
import infoservice.dynamic.DynamicConfiguration;
import anon.infoservice.JAPMinVersion;
import org.w3c.dom.Document;
import anon.util.XMLUtil;

final public class Configuration
{
	/**
	 * Stores the instance of Configuration (Singleton).
	 */
	private static Configuration configurationInstance = null;

	/**
	 * Returns the instance of Configuration (Singleton).
	 * @return The Configuration instance.
	 */
	public static Configuration getInstance()
	{
		return configurationInstance;
	}

	/**
	 * Stores the ListenerInterfaces of all neighbour infoservices declared in the config file.
	 */
	private Vector m_initialNeighbourInfoServices;

	/**
	 * Stores the ListenerInterfaces of all interfaces our infoservice is bound to.
	 */
	private Vector m_hardwareListenerList;

	/**
	 * Stores the ListenerInterfaces of all interfaces our infoservice propagates to others.
	 */
	private Vector m_virtualListenerList;

	/**
	 * Stores all hosts (virtual and hardware) the infoservice ist listening on
	 */
	private Vector m_hostList;
	
	/**
	 * Stores the name of our infoservice. The name is just for comfort reasons and has no
	 * importance.
	 */
	private String m_strOwnName;

	/**
	 * The InfoService ID. It is the SubjectPublicKeyIdentifier of the certificate.
	 */
	private String m_strID;

	/**
	 * Maximum size in bytes of HTTP POST data. We will not accept the post of longer messages.
	 * Longer messages will be thrown away.
	 */
	private int m_iMaxPostContentLength;

	/**
	 * This stores, whether status statistics are enabled (true) or not (false).
	 */
	private boolean m_bStatusStatisticsEnabled;

	/**
	 * Stores the interval (in ms, see System.currentTimeMillis()) between two status statistics.
	 */
	private long m_lStatusStatisticsInterval;

	/**
	 * Stores the directory, where to write the status statistics files. The name must end with a
	 * slash or backslash (WINDOWS).
	 */
	private String m_strStatusStatisticsLogDir;

	/**
	 * Stores the addresses of the proxy servers at the end of the cascades. This is only for
	 * compatibility and will be removed soon.
	 */
	private String m_strProxyAddresses;

	/**
	 * Stores the date format information for HTTP headers.
	 */
	private static SimpleDateFormat ms_httpDateFormat;

	/**
	 * Stores, whether we are the "root" of the JAP update information (JNLP-Files + JAPMinVersion).
	 */
	private boolean m_bRootOfUpdateInformation;

	/**
	 * Stores where the japRelease.jnlp is located in the local file system (path + filename).
	 */
	private String m_strJapReleaseJnlpFile;

	/**
	 * Stores where the japDevelopment.jnlp is located in the local file system (path + filename).
	 */
	private String m_strJapDevelopmentJnlpFile;

	/**
	 * Stores where the information about latest Java versions is located in the local file system
	 * (path + filename).
	 */
	private File m_strJavaLatestVersionFile;

	private File m_messageFile;

	/**
	 * Stores where the japMinVersion.xml is located in the local file system (path + filename).
	 */
	private String m_strJapMinVersionFile;

	/**
	 * Stores where the old JAP min version.
	 */
	private JAPMinVersion m_japMinVersionOld;

	/**
	 * Stores the propability with which the japMinVersion is returned instead of japMinVersionOld (e.g. a forced updated is done).
	 */
	private double m_dJapUpdatePropability;

	/**
	 * Stores the startup time of the infoservice (time when the configuration instance was
	 * created).
	 */
	private Date m_startupTime;

	/**
	 * Stores whether this infoservice should hold a list with JAP forwarders (true) or not (false).
	 */
	private boolean m_holdForwarderList;

	/** Stores how many concurrent connections the InfoService can handle
	 *
	 */
	private int m_NrOfThreads;
	
	/**
	 * Stores if the performance monitoring is enabled
	 */
	private boolean m_bPerfEnabled;
	
	/**
	 * If InfoService is in passive (listen/poll) mode.
	 */
	private boolean m_bPassive;
	
	private String m_lastPassword = "";

	/** Stores 7 configuration values for cascade performance monitoring.
	 * <ul>
	 *  <li>The local proxy host</li>
	 * 	<li>The local proxy port</li>
	 * 	<li>The size of the random test data block in bytes;</li>
	 * 	<li>The interval between measurement blocks in milliseconds</li>
	 *  <li>Requests per interval</li>
	 *  <li>Test timeout</li>
	 *  <li>Min amount of packets a cascade needs to be tested</li>
	 * </ul>
	 */
	private final Object[] m_aPerfMeterConf = new Object[7];
	
	private boolean m_bPerfServerEnabled;
	
	private File m_strPerfAccountDirectory = null;
	
	private String m_strPerfAccountPassword = null;
	
	private Vector m_perfBlackList = new Vector();
	private Vector m_perfWhiteList = new Vector();
	
	public final static String IS_PROP_NAME_PERFORMANCE_MONITORING = "perf";
	public final static String IS_PROP_NAME_PERFACCOUNT = 
		IS_PROP_NAME_PERFORMANCE_MONITORING +".account";
	
	public final static String IS_PROP_NAME_BLACKLIST =
		IS_PROP_NAME_PERFORMANCE_MONITORING+".blackList";
	
	public final static String IS_PROP_NAME_WHITELIST =
		IS_PROP_NAME_PERFORMANCE_MONITORING+".whiteList";
	public final static String PROP_MAX_TEST_TIME =
		IS_PROP_NAME_PERFORMANCE_MONITORING+".maxTestTime";
	
	public final static String IS_PROP_NAME_PERFACCOUNT_DIRECTORY =
		IS_PROP_NAME_PERFACCOUNT+".directory";
	public final static String IS_PROP_VALUE_PERFACCOUNT_DIRECTORY = "accounts";
	
	public final static String IS_PROP_NAME_PERFACCOUNT_PASSWORD = 
		IS_PROP_NAME_PERFACCOUNT+".passw";
	public final static String IS_PROP_VALUE_PERFACCOUNT_PASSWORD = null;
	
	public final static String IS_PROP_NAME_PERF_SERVER =
		IS_PROP_NAME_PERFORMANCE_MONITORING + ".server";
	public final static String IS_PROP_VALUE_PERF_SERVER = "true";
	
	public final static String IS_PROP_VALUE_TERMS_AND_CONDITIONS_DIR = "terms";
	
	public Configuration(Properties a_properties) throws Exception
	{
		/* for running in non-graphic environments, we need the awt headless support, it is only
		 * available since Java 1.4, so running the infoservice in a non-graphic environment is
		 * only possible with Java versions since 1.4
		 */
		/* set the property in Java 1.0 compatible style */
		Properties systemProperties = System.getProperties();
		systemProperties.put("java.awt.headless", "true");
		System.setProperties(systemProperties);

		configurationInstance = this;
		m_startupTime = new Date();
		try
		{
			LogHolder.setLogInstance(new InfoServiceLog(a_properties));
			LogHolder.setDetailLevel(Integer.parseInt(a_properties.getProperty("messageDetailLevel", "0").
				trim()));
			m_strOwnName = a_properties.getProperty("ownname").trim();
			m_iMaxPostContentLength = Integer.parseInt(a_properties.getProperty("maxPOSTContentLength").trim());
			String strHardwareListeners = a_properties.getProperty("HardwareListeners").trim();
			String strVirtualListeners = a_properties.getProperty("VirtualListeners");
			
			StringTokenizer stHardware = new StringTokenizer(strHardwareListeners, ",");
			

			/* create a list of all interfaces we are listening on */
			m_hostList = new Vector();
			m_hardwareListenerList = new Vector();
			while (stHardware.hasMoreTokens())
			{
				ListenerInterface iface = new ListenerInterface(stHardware.nextToken());
				m_hardwareListenerList.addElement(iface);
				
				if(iface != null && !m_hostList.contains(iface.getHost()))
				{
					m_hostList.addElement(iface.getHost());
				}
			}
			
			if (strVirtualListeners != null)
			{
				m_virtualListenerList = new Vector();
				StringTokenizer stVirtual = new StringTokenizer(strVirtualListeners.trim(), ",");
				while (stVirtual.hasMoreTokens())
				{
					ListenerInterface iface = new ListenerInterface(stVirtual.nextToken());
					m_virtualListenerList.addElement(iface);
					
					if(iface != null && !m_hostList.contains(iface.getHost()))
					{
						m_hostList.addElement(iface.getHost());
					}
				}
			}
			else
			{
				m_virtualListenerList = (Vector)m_hardwareListenerList.clone();
			}

			/* only for compatibility */
			m_strProxyAddresses = a_properties.getProperty("proxyAddresses").trim();

			/* load the private key for signing our own infoservice messages */
			String privatePkcs12KeyFile = a_properties.getProperty("privateKeyFile");
			if ( (privatePkcs12KeyFile != null) && (!privatePkcs12KeyFile.trim().equals("")))
			{
				privatePkcs12KeyFile = privatePkcs12KeyFile.trim();
				PKCS12 infoServiceMessagesPrivateKey = null;
				try
				{
					do
					{
						infoServiceMessagesPrivateKey = loadPkcs12PrivateKey(privatePkcs12KeyFile,
							m_lastPassword);
						if (infoServiceMessagesPrivateKey == null)
						{
							/* file was found, but the private key could not be loaded -> maybe wrong password */
							System.out.println(
								"Cannot load private key! Enter password for private key from file: " +
								privatePkcs12KeyFile);
							System.out.print("Password: ");
							BufferedReader passwordReader = new BufferedReader(new InputStreamReader(System.
								in));
							m_lastPassword = passwordReader.readLine();
						}
					}
					while (infoServiceMessagesPrivateKey == null);
					/* we have loaded the private key for signing our own infoservice messages -> put it in
					 * the SignatureCreator
					 */
					SignatureCreator.getInstance().setSigningKey(SignatureVerifier.DOCUMENT_CLASS_INFOSERVICE,
						infoServiceMessagesPrivateKey);
					m_strID = new X509SubjectKeyIdentifier(
						infoServiceMessagesPrivateKey.getPublicKey()).getValueWithoutColon();
				}
				catch (FileNotFoundException e)
				{
					System.out.println("Cannot find the private key file: " + privatePkcs12KeyFile);
					System.out.println("Exiting...");
					throw (e);
				}
			}
			else
			{
				LogHolder.log(LogLevel.WARNING, LogType.MISC,
							  "No private key for signing the own infoservice entry specified. Unsigned messages will be sent.");
			}

			/* whether to check signatures or not (default is enabled signature verification) */
			SignatureVerifier.getInstance().setCheckSignatures(true);
			String checkSignatures = a_properties.getProperty("checkSignatures");
			if (checkSignatures != null)
			{
				if (checkSignatures.equalsIgnoreCase("false"))
				{
					SignatureVerifier.getInstance().setCheckSignatures(false);
					LogHolder.log(LogLevel.WARNING, LogType.MISC,
								  "Disabling signature verification for all documents.");
				}
			}
			//if (SignatureVerifier.getInstance().isCheckSignatures())
			{
				LogHolder.log(LogLevel.DEBUG, LogType.MISC, "Loading certificates...");												
				
				loadTrustedCertificateFiles(a_properties, "trustedRootCertificateFiles", 
						JAPCertificate.CERTIFICATE_TYPE_ROOT_MIX, "mix root", true);
				
				loadTrustedCertificateFiles(a_properties, "trustedInfoServiceRootCertificateFiles", 
						JAPCertificate.CERTIFICATE_TYPE_ROOT_INFOSERVICE, "infoservice root", true);
				
				loadTrustedCertificateFiles(a_properties, "trustedInfoServiceCertificateFiles", 
						JAPCertificate.CERTIFICATE_TYPE_INFOSERVICE, "infoservice", false);
				
				loadTrustedCertificateFiles(a_properties, "trustedMixCertificateFiles", 
						JAPCertificate.CERTIFICATE_TYPE_MIX, "mix", false);

				loadTrustedCertificateFiles(a_properties, "trustedUpdateCertificateFiles", 
						JAPCertificate.CERTIFICATE_TYPE_UPDATE, "update", true);
				
				loadTrustedCertificateFiles(a_properties, "trustedPICertificateFiles", 
						JAPCertificate.CERTIFICATE_TYPE_PAYMENT, "PI", true);						
				
				
				try
				{
					String b = a_properties.getProperty("checkInfoServiceSignatures").trim();
					if (b.equalsIgnoreCase("false"))
					{
						SignatureVerifier.getInstance().setCheckSignatures(
							SignatureVerifier.DOCUMENT_CLASS_INFOSERVICE, false);
					}
				}
				catch (Exception e)
				{
					LogHolder.log(LogLevel.WARNING, LogType.MISC,
								  "Could not read 'checkInfoServiceSignatures' setting - default to: " +
								  SignatureVerifier.getInstance().isCheckSignatures(
									  SignatureVerifier.DOCUMENT_CLASS_INFOSERVICE));
				}
				try
				{
					String b = a_properties.getProperty("checkMixSignatures").trim();
					if (b.equalsIgnoreCase("false"))
					{
						SignatureVerifier.getInstance().setCheckSignatures(
							SignatureVerifier.DOCUMENT_CLASS_MIX, false);
					}
				}
				catch (Exception e)
				{
					LogHolder.log(LogLevel.WARNING, LogType.MISC,
								  "Could not read 'checkMixSignatures' setting - default to: " +
								  SignatureVerifier.getInstance().isCheckSignatures(
									  SignatureVerifier.DOCUMENT_CLASS_MIX));
				}
				try
				{
					String b = a_properties.getProperty("checkUpdateSignatures").trim();
					if (b.equalsIgnoreCase("false"))
					{
						SignatureVerifier.getInstance().setCheckSignatures(
							SignatureVerifier.DOCUMENT_CLASS_UPDATE, false);
					}
				}
				catch (Exception e)
				{
					LogHolder.log(LogLevel.WARNING, LogType.MISC,
								  "Could not read 'checkUpdateSignatures' setting - default to: " +
								  SignatureVerifier.getInstance().isCheckSignatures(
									  SignatureVerifier.DOCUMENT_CLASS_UPDATE));
				}

				/* start the certificate manager, which manages the appended certificates of the
				 * MixCascade entries for verification of the StatusInfo entries
				 */
				new CertificateManager();
			}

			/* get the JAP update information persistence settings */
			m_bRootOfUpdateInformation = a_properties.getProperty("rootOfUpdateInformation").trim().
				equalsIgnoreCase("true");
			if (m_bRootOfUpdateInformation)
			{
				m_strJapReleaseJnlpFile = a_properties.getProperty("japReleaseFileName");
				if (m_strJapReleaseJnlpFile != null)
				{
					m_strJapReleaseJnlpFile.trim();
				}
				m_strJapDevelopmentJnlpFile = a_properties.getProperty("japDevelopmentFileName");
				if (m_strJapDevelopmentJnlpFile != null)
				{
					m_strJapDevelopmentJnlpFile.trim();
				}
				m_strJapMinVersionFile = a_properties.getProperty("japMinVersionFileName");
				if (m_strJapMinVersionFile != null)
				{
					m_strJapMinVersionFile.trim();
				}
				try
				{
					m_dJapUpdatePropability=Math.min(1.0,
						Double.parseDouble(a_properties.getProperty("japUpdatePropability").trim()));
					m_dJapUpdatePropability=Math.max(0.0,m_dJapUpdatePropability);
				}
				catch(Throwable t)
				{
					m_dJapUpdatePropability=1.0;
				}
				try
				{
					m_strJavaLatestVersionFile = new File(a_properties.getProperty(JavaVersionDBEntry.
						PROPERTY_NAME).trim());
				}
				catch (Exception a_e)
				{
					LogHolder.log(LogLevel.WARNING, LogType.MISC, "Could not load Java version information!");
				}

				try
				{
					m_messageFile = new File(a_properties.getProperty(MessageDBEntry.PROPERTY_NAME).trim());
				}
				catch (Exception a_e)
				{
					LogHolder.log(LogLevel.WARNING, LogType.MISC, "Could not load message information!");
				}

				/* load the private key for signing our own infoservice messages */
				String updatePkcs12KeyFile = a_properties.getProperty("updateInformationPrivateKey");
				if ( (updatePkcs12KeyFile != null) && (!updatePkcs12KeyFile.trim().equals("")))
				{
					updatePkcs12KeyFile = updatePkcs12KeyFile.trim();
					PKCS12 updateMessagesPrivateKey = null;
					try
					{
						String lastPassword = "";
						do
						{
							updateMessagesPrivateKey = loadPkcs12PrivateKey(updatePkcs12KeyFile, lastPassword);
							if (updateMessagesPrivateKey == null)
							{
								/* file was found, but the private key could not be loaded -> maybe wrong password */
								System.out.println(
									"Cannot load private key! Enter password for private key from file: " +
									updatePkcs12KeyFile);
								System.out.print("Password: ");
								BufferedReader passwordReader = new BufferedReader(new InputStreamReader(
									System.in));
								lastPassword = passwordReader.readLine();
							}
						}
						while (updateMessagesPrivateKey == null);
						/* we have loaded the private key for signing the update messages -> put it in the
						 * SignatureCreator
						 */
						SignatureCreator.getInstance().setSigningKey(SignatureVerifier.DOCUMENT_CLASS_UPDATE,
							updateMessagesPrivateKey);
					}
					catch (FileNotFoundException e)
					{
						System.out.println("Cannot find the private key file: " + updatePkcs12KeyFile);
						System.out.println("Exiting...");
						throw (e);
					}
				}
				else
				{
					LogHolder.log(LogLevel.WARNING, LogType.MISC,
								  "No private key for signing the update messages specified. Unsigned messages will be sent.");
				}
				//load and sign the old JAP version update information if they are given...
				String strJapMinVersionOldFile = a_properties.getProperty("japMinVersionFileNameOld");
				if (strJapMinVersionOldFile != null)
				{
					try{
						File fileJapMinVersionOld=new File(strJapMinVersionOldFile.trim());
						Document docMinVersion=XMLUtil.readXMLDocument(fileJapMinVersionOld);
						SignatureCreator.getInstance().signXml(SignatureVerifier.DOCUMENT_CLASS_UPDATE,docMinVersion);
						m_japMinVersionOld=new JAPMinVersion(docMinVersion.getDocumentElement());
					}
					catch(Throwable t)
					{
						m_japMinVersionOld=null;
					}
				}
			}
			else
			{
				m_strJapReleaseJnlpFile = null;
				m_strJapDevelopmentJnlpFile = null;
				m_strJapMinVersionFile = null;
			}

			/* Create the list of all neighbour infoservices. So we know, where to announce ourself at
			 * startup.
			 */
			StringTokenizer stNeighbours = new StringTokenizer(a_properties.getProperty("neighbours", "").
				trim(),
				",");
			m_initialNeighbourInfoServices = new Vector();
			while (stNeighbours.hasMoreTokens())
			{
				try
				{
					StringTokenizer stCurrentInterface = new StringTokenizer(stNeighbours.nextToken(), ":");
					String inetHost = stCurrentInterface.nextToken();
					int inetPort = Integer.parseInt(stCurrentInterface.nextToken());
					m_initialNeighbourInfoServices.addElement(new ListenerInterface(inetHost, inetPort));
				}
				catch (Exception e)
				{
					/* simply don't use this neighbour */
				}
			}
			// initialise the info service database to quickly send the own database entries
			InfoServiceDBEntry entry;
			for (int i = 0; i < m_initialNeighbourInfoServices.size(); i++)
			{
				entry =
					new InfoServiceDBEntry(null, null,
										   ( (ListenerInterface) m_initialNeighbourInfoServices.elementAt(i)).
										   toVector(), false, false, System.currentTimeMillis(), 0, false);
				entry.markAsBootstrap();

				//entry.setNeighbour(true);
				try
				{
					InfoServiceDBEntry.class.getMethod("setNeighbour", new Class[]
						{boolean.class}).invoke(
							entry, new Object[]
							{new Boolean(true)});
				}
				catch (Throwable a_e)
				{
					/** @todo don't know why, this leads to "NoSuchMethodError" (Z)V on some systems */
					LogHolder.log(LogLevel.EXCEPTION, LogType.MISC, a_e);
				}
				Database.getInstance(InfoServiceDBEntry.class).update(entry, false);
			}

			/* start the UpdateInformationHandler announce thread */
			UpdateInformationHandler.getInstance();

			/* get the settings for status statistics */
			m_bStatusStatisticsEnabled = a_properties.getProperty("statusStatistics").trim().equalsIgnoreCase(
				"enabled");
			/* set some default values */
			m_lStatusStatisticsInterval = 3600 * (long) (1000); // 1 hour
			m_strStatusStatisticsLogDir = ""; // log to the current directory
			if (m_bStatusStatisticsEnabled)
			{
				/* overwrite the default values */
				long tempInterval = Long.parseLong(a_properties.getProperty("statusStatisticsInterval").trim()) *
					60 *
					1000;
				if (tempInterval > 0)
				{
					/* set only to valid values */
					m_lStatusStatisticsInterval = tempInterval;
				}
				m_strStatusStatisticsLogDir = a_properties.getProperty("statusStatisticsLogDir").trim();
			}

			/* get the settings for fetching the tor nodes list */
			boolean fetchTorNodesList = a_properties.getProperty("fetchTorNodesList").trim().equalsIgnoreCase(
				"enabled");
			if (fetchTorNodesList)
			{
				/* set some default values */
				long fetchTorNodesListInterval = 600L * 1000L;
				/* overwrite the default values */
				long tempInterval = Long.parseLong(a_properties.getProperty("fetchTorNodesListInterval").trim()) *
					1000;
				if (tempInterval > 0)
				{
					/* set only to valid values */
					fetchTorNodesListInterval = tempInterval;
				}
				/* load the list of known tor directory servers */
				String torDirectoryServers = a_properties.getProperty("torDirectoryServers").trim();
				StringTokenizer stTorDirectoryServers = new StringTokenizer(torDirectoryServers, ",");
				while (stTorDirectoryServers.hasMoreTokens())
				{
					try
					{
						URL torDirectoryServer = new URL(stTorDirectoryServers.nextToken().trim());
						int torServerPort = torDirectoryServer.getPort();
						if (torServerPort == -1)
						{
							torServerPort = torDirectoryServer.getPort();
						}
						/* add the directory server with nearly infinite timeout (1000 years) */
						TorDirectoryAgent.getInstance().addTorDirectoryServer(new TorDirectoryServer(new
							TorDirectoryServerUrl(torDirectoryServer.getHost(), torServerPort,
												  torDirectoryServer.getFile()),
							1000L * 365L * 24L * 3600L * 1000L, true));
					}
					catch (Exception e)
					{
						/* don't add the directory server to the database, because there was an error */
					}
				}
				/* start the update tor nodes list thread -> if the fetchTorNodesList value was false,
				 * the thread is never started -> we will never fetch the list
				 */
				TorDirectoryAgent.getInstance().startUpdateThread(fetchTorNodesListInterval);
			}

			/* get the settings for fetching the tor nodes list */
			String str = a_properties.getProperty("fetchMixminionNodesList");
			boolean fetchMixminionNodesList = false;
			if (str != null && str.trim().equalsIgnoreCase("enabled"))
			{
				fetchMixminionNodesList = true;
			}
			if (fetchMixminionNodesList)
			{
				/* set some default values */
				long fetchMixminionNodesListInterval = 600L * 1000L;
				/* overwrite the default values */
				long tempInterval = Long.parseLong(a_properties.getProperty("fetchMixminionNodesListInterval").
					trim()) * 1000L;
				if (tempInterval > 0)
				{
					/* set only to valid values */
					fetchMixminionNodesListInterval = tempInterval;
				}
				/* load the list of known tor directory servers */
				String mixminionDirectoryServers = a_properties.getProperty("mixminionDirectoryServers").trim();
				StringTokenizer stMixminionDirectoryServers = new StringTokenizer(mixminionDirectoryServers,
					",");
				while (stMixminionDirectoryServers.hasMoreTokens())
				{
					try
					{
						URL mixminionDirectoryServer = new URL(stMixminionDirectoryServers.nextToken().trim());
						MixminionDirectoryAgent.getInstance().addDirectoryServer(mixminionDirectoryServer);
					}
					catch (Exception e)
					{
						/* don't add the directory server to the database, because there was an error */
					}
				}
				/* start the update tor nodes list thread -> if the fetchTorNodesList value was false,
				 * the thread is never started -> we will never fetch the list
				 */
				MixminionDirectoryAgent.getInstance().startUpdateThread(fetchMixminionNodesListInterval);
			}
			/* get the JAP forwarder list settings */
			m_holdForwarderList = false;
			try
			{
				m_holdForwarderList = a_properties.getProperty("primaryForwarderList").trim().
					equalsIgnoreCase(
						"enabled");
			}
			catch (Exception e)
			{
				LogHolder.log(LogLevel.WARNING, LogType.MISC,
							  "Could not read 'primaryForwarderList' setting - default to: " +
							  m_holdForwarderList);
			}
			/* do some more initialization stuff */
			ms_httpDateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.US);
			ms_httpDateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));

			m_NrOfThreads = Constants.MAX_NR_OF_CONCURRENT_CONNECTIONS;
			try
			{
				String b = a_properties.getProperty("maxNrOfConcurrentConnections").trim();
				m_NrOfThreads = Integer.parseInt(b);
			}
			catch (Exception e)
			{
				LogHolder.log(LogLevel.WARNING, LogType.MISC,
							  "Could not read 'maxNrOfConcurrentConnections' setting - default to: " +
							  m_NrOfThreads);
			}
			
			if (Boolean.valueOf(a_properties.getProperty("enableDynamicConfiguration", "false")).booleanValue())
			{
				try
				{
					DynamicConfiguration.getInstance().readConfiguration(a_properties);
				}
				catch (Exception e2)
				{
					System.err.println("Error reading the configuration information related to Dynamic Cascades");
					System.err.println("Exception: " + e2.toString());
				}
			}
			
			m_bPassive = Boolean.valueOf(a_properties.getProperty("modePassive", "false")).booleanValue();
			
			if (m_bPassive)
			{
				m_bPerfEnabled = false;
			}
			else
			{
				m_bPerfEnabled = Boolean.valueOf(a_properties.getProperty("perf", "true")).booleanValue();
			}
			
			if(m_bPerfEnabled)
			{
				String value = a_properties.getProperty("perf.proxyHost", "localhost");
				if(value != null)
					m_aPerfMeterConf[0] = value;
			
				value = a_properties.getProperty("perf.proxyPort", "4001");
				if(value != null)
					m_aPerfMeterConf[1] = Integer.valueOf(value);

				value = a_properties.getProperty("perf.dataSize", "250000");
				if(value != null)
				{
					m_aPerfMeterConf[2] = new Integer(Math.min(512*1024*2, Integer.parseInt(value)));
				}
				
				value = a_properties.getProperty("perf.majorInterval", "240000");
				if(value != null)
				{
					m_aPerfMeterConf[3] = new Integer(Math.max(60*1000, Integer.parseInt(value)));
				}
				
				value = a_properties.getProperty("perf.requestsPerInterval", "2");
				if(value != null)
				{
					m_aPerfMeterConf[4] = Integer.valueOf(value);
				}
				
				value = a_properties.getProperty("perf.maxWaitForSingleTest", "60000");
				if(value != null)
				{
					m_aPerfMeterConf[5] = new Integer(Math.max(5*1000, Integer.parseInt(value)));
				}
				
				m_strPerfAccountDirectory = 
					new File(a_properties.getProperty(IS_PROP_NAME_PERFACCOUNT_DIRECTORY, 
							 				 IS_PROP_VALUE_PERFACCOUNT_DIRECTORY));
				m_strPerfAccountPassword = 
					a_properties.getProperty(IS_PROP_NAME_PERFACCOUNT_PASSWORD, 
		 				 					 IS_PROP_VALUE_PERFACCOUNT_PASSWORD);
				
				//value = a_properties.getProperty(PROP_MAX_TEST_TIME, "60000");				
				//m_aPerfMeterConf[4] = Long.valueOf(value);
				
				
					
				
				String strPerfBlackList = a_properties.getProperty(IS_PROP_NAME_BLACKLIST, "").trim();
				String strPerfWhiteList = a_properties.getProperty(IS_PROP_NAME_WHITELIST, "").trim();
				
				StringTokenizer stBlack = new StringTokenizer(strPerfBlackList, ",");
				StringTokenizer stWhite = new StringTokenizer(strPerfWhiteList, ",");
				
				while (stBlack.hasMoreTokens())
				{
					m_perfBlackList.addElement(stBlack.nextToken());
				}
				
				while (stWhite.hasMoreTokens())
				{
					m_perfWhiteList.addElement(stWhite.nextToken());
				}
				
				m_bPerfServerEnabled = Boolean.valueOf(a_properties.getProperty(IS_PROP_NAME_PERF_SERVER,
						IS_PROP_VALUE_PERF_SERVER)).booleanValue();
			}
		}
		catch (Exception e)
		{
			System.err.println("Error reading configuration!");
			System.err.println("Exception: " + e.toString());
			throw e;
		}
	}

	/**
	 * Returns the list with ListenerInterfaces of all neighbour infoservices from
	 * the config file.
	 *
	 * @return Vector with ListenerInterfaces of initial neighbours.
	 */
	public Vector getInitialNeighbourInfoServices()
	{
		return m_initialNeighbourInfoServices;
	}

	/**
	 * Returns the ListenerInterfaces of all Interfaces our infoservice is
	 * locally bound to.
	 *
	 * @return the ListenerInterfaces of bound interfaces.
	 */
	public Vector getHardwareListeners()
	{
		return m_hardwareListenerList;
	}

	/** Returns how many concurrent connections this IS should handle*/
	public int getNrOfConcurrentConnections()
	{
		return m_NrOfThreads;
	}

	/**
	 * Returns the ListenerInterfaces of all Interfaces our infoservice
	 * propagates to others.
	 *
	 * @return the ListenerInterfaces of propagated interfaces.
	 */
	public Vector getVirtualListeners()
	{
		return m_virtualListenerList;
	}

	public String getID()
	{
		return m_strID;
	}

	/**
	 * Returns the name of our infoservice. The name is just for comfort reasons and has no
	 * importance.
	 *
	 * @return The name of our infoservice from config file.
	 */
	public String getOwnName()
	{
		return m_strOwnName;
	}

	/**
	 * Returns the maximum HTTP POST data size which will be accepted. We throw away longer POST
	 * messages.
	 *
	 * @return The maximum HTTP POST data size we accept.
	 */
	public int getMaxPostContentLength()
	{
		return m_iMaxPostContentLength;
	}

	/**
	 * This returns, whether status statistics are enabled (true) or not (false).
	 *
	 * @return True, if status statistics are enabled or false, if they are disabled.
	 */
	public boolean isStatusStatisticsEnabled()
	{
		return m_bStatusStatisticsEnabled;
	}

	/**
	 * Returns the interval (in ms, see System.currentTimeMillis()) between two status statistics.
	 * The value is only meaningful, if isStatusStatisticsEnabled() returns true.
	 *
	 * @return The status statistics interval.
	 */
	public long getStatusStatisticsInterval()
	{
		return m_lStatusStatisticsInterval;
	}

	/**
	 * Returns the directory where to log the status statistics. The directory name must end with
	 * a slash or backslash (WINDOWS). The value is only meaningful, if isStatusStatisticsEnabled()
	 * returns true.
	 *
	 * @return The directory, where to write the status statistics files.
	 */
	public String getStatusStatisticsLogDir()
	{
		return m_strStatusStatisticsLogDir;
	}

	/**
	 * Returns the addresses of the proxy servers at the end of the cascades. The info is from
	 * the proxyAddresses line of the properties file. This method is only for compatibility and
	 * will be removed soon.
	 *
	 * @return The addresses of the proxy servers.
	 */
	public String getProxyAddresses()
	{
		return m_strProxyAddresses;
	}

	/**
	 * Returns the HTTP-header date format information.
	 *
	 * @return The HTTP-header date format.
	 */
	public static SimpleDateFormat getHttpDateFormat()
	{
		return ms_httpDateFormat;
	}

	/**
	 * Returns, whether we are the root of the JAP update information (JNLP-Files + JAPMinVersion).
	 *
	 */
	public boolean isRootOfUpdateInformation()
	{
		return m_bRootOfUpdateInformation;
	}

	/**
	 * Returns where the japRelease.jnlp is located in the local file system (path + filename).
	 *
	 * @return The filename (maybe with path) of japRelease.jnlp.
	 */
	public String getJapReleaseJnlpFile()
	{
		return m_strJapReleaseJnlpFile;
	}

	/**
	 * Returns where the japDevelopment.jnlp is located in the local file system (path + filename).
	 *
	 * @return The filename (maybe with path) of japDevelopment.jnlp.
	 */
	public String getJapDevelopmentJnlpFile()
	{
		return m_strJapDevelopmentJnlpFile;
	}

	/**
	 * Returns where the file with Java latest version information is located in the local file system
	 * (path + filename).
	 *
	 * @return The filename (maybe with path) of java latest versions
	 */
	public File getJavaLatestVersionFile()
	{
		return m_strJavaLatestVersionFile;
	}

	public File getMessageFile()
	{
		return m_messageFile;
	}

	/**
	 * Returns where the file with JAP minimal version number is located in the local file system
	 * (path + filename).
	 *
	 * @return The filename (maybe with path) of japMinVersion.xml.
	 */
	public String getJapMinVersionFile()
	{
		return m_strJapMinVersionFile;
	}

	/**
	 * Returns where the JAP old minimal version number. This might be usefull in case of forced JAP updates.
	 *
	 * @return The old minimal version of JAP.
	 */
	public JAPMinVersion getJapMinVersionOld()
	{
		return m_japMinVersionOld;
	}

	/**
	 * Returns the update propability as specified in the configuration file.
	 * @return update propability
	 */
	public double getJapUpdatePropability()
	{
		return m_dJapUpdatePropability;
	}

	/**
	 * Returns the startup time of this infoservice.
	 *
	 * @return The time when this infoservices was started.
	 */
	public Date getStartupTime()
	{
		return m_startupTime;
	}

	/**
	 * Returns whether this infoservice holds a JAP forwarder list or not.
	 *
	 * @return True, if this infoservice has a primary forwarder list or false, if this infoservice
	 *         doesn't have a primary forwarder list and redirects all requests from blockees to
	 *         an infoservice with such a list (if we know one in the InfoserviceDatabase).
	 */
	public boolean holdForwarderList()
	{
		return m_holdForwarderList;
	}

	private void loadTrustedCertificateFiles(Properties a_properties, String a_strProperty, int a_certificateType,
			String a_strName, boolean bWarnIfNotAvailable)
	{
		String trustedCertFiles = a_properties.getProperty(a_strProperty);
		if ( (trustedCertFiles != null) && (!trustedCertFiles.trim().equals("")))
		{
			StringTokenizer stTrustedCertificates = new StringTokenizer(trustedCertFiles.
				trim(), ",");
			while (stTrustedCertificates.hasMoreTokens())
			{
				String currentCertificateFile = stTrustedCertificates.nextToken().trim();
				JAPCertificate currentCertificate = loadX509Certificate(currentCertificateFile);
				if (currentCertificate != null)
				{
					SignatureVerifier.getInstance().getVerificationCertificateStore().
						addCertificateWithoutVerification(currentCertificate, a_certificateType, true, false);
					LogHolder.log(LogLevel.DEBUG, LogType.MISC,
								  "Added the following file to the store of trusted " + 
								  a_strName + " certificates: " +
								  currentCertificateFile);
				}
				else
				{
					LogHolder.log(LogLevel.ERR, LogType.MISC,
								  "Error loading trusted " + a_strName + 
								  " certificate: " + currentCertificateFile);
				}
			}
		}
		else if (bWarnIfNotAvailable)
		{
			LogHolder.log(LogLevel.WARNING, LogType.MISC, "No trusted " + a_strName + " certificates specified.");
		}			
	}
	
	
	/**
	 * Loads a PKCS12 certificate from a file.
	 *
	 * @param a_pkcs12FileName The filename (with path) of the PKCS12 file.
	 * @param a_password The password for the PKCS12 file, if necessary. If no password is necessary,
	 *                   you can supply null or an empty string.
	 *
	 * @return The PKCS12 certificate structure including the private key or null, if the
	 *         certificate could not be loaded (invalid password or invalid data within the file).
	 *
	 * @throws FileNotFoundException If the file cannot be found in the filesystem.
	 */
	private PKCS12 loadPkcs12PrivateKey(String a_pkcs12FileName, String a_password) throws
		FileNotFoundException
	{
		PKCS12 loadedCertificate = null;
		if (a_password == null)
		{
			a_password = "";
		}
		try
		{
			loadedCertificate = PKCS12.getInstance(new FileInputStream(a_pkcs12FileName),
				a_password.toCharArray());
		}
		catch (FileNotFoundException fnfe)
		{
			/* we throw an exception, if the file was not found */
			throw (fnfe);
		}
		catch (Exception e)
		{
			/* do nothing and return null */
		}
		return loadedCertificate;
	}

	/**
	 * Loads a X509 certificate from a file.
	 *
	 * @param a_x509FileName The filename (with path) of the X509 file.
	 *
	 * @return The X509 certificate or null, if there was an error while loading the certificate
	 *         from the specified file.
	 */
	private JAPCertificate loadX509Certificate(String a_x509FileName)
	{
		return JAPCertificate.getInstance(new File(a_x509FileName));
	}
	
	/**
	 * Returns if this InfoService does not propagate any information to
	 * other InfoServices, but only passively stores some data (e.g. Mixes, Cascades, Status, Performance)
	 * collected from other InfoServices. This passively stored information is kept as long as a JonDo 
	 * client would do.
	 * @return
	 */
	public boolean isPassive()
	{
		return m_bPassive;
	}
	
	public Object[] getPerformanceMeterConfig() 
	{
		return m_aPerfMeterConf;
	}
	
	public boolean isPerfEnabled()
	{
		return m_bPerfEnabled;
	}
	
	public boolean isPerfServerEnabled()
	{
		return m_bPerfServerEnabled;
	}
	
	public File getPerfAccountDirectory()
	{
		return m_strPerfAccountDirectory;
	}
	
	public String getPerfAccountPassword()
	{
		return m_strPerfAccountPassword;
	}
	
	public Vector getPerfBlackList()
	{
		return m_perfBlackList;
	}
	
	public Vector getPerfWhiteList()
	{
		return m_perfWhiteList;
	}
	
	public Vector getHostList()
	{
		return m_hostList;
	}
	
	public File getTermsAndConditionsDir()
	{
		return new File(IS_PROP_VALUE_TERMS_AND_CONDITIONS_DIR);
	}

}
