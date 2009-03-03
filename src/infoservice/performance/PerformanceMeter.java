/*
 Copyright (c) 2008 The JAP-Team, JonDos GmbH

 All rights reserved.

 Redistribution and use in source and binary forms, with or without modification, 
 are permitted provided that the following conditions are met:

    * Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
    * Redistributions in binary form must reproduce the above copyright notice,
      this list of conditions and the following disclaimer in the documentation and/or
      other materials provided with the distribution.
    * Neither the name of the University of Technology Dresden, Germany, nor the name of
      the JonDos GmbH, nor the names of their contributors may be used to endorse or
      promote products derived from this software without specific prior written permission.

 THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE REGENTS OR
 CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package infoservice.performance;

import java.io.File;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.FileNotFoundException;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Vector;
import java.net.InetAddress;
import java.net.Socket;
import java.net.ServerSocket;
import java.net.UnknownHostException;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Hashtable;
import java.util.Random;
import java.util.Calendar;
import java.util.Observer;
import java.util.Observable;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import HTTPClient.HTTPConnection;
import HTTPClient.HTTPResponse;

import jap.pay.AccountUpdater;
import anon.ErrorCodes;
import anon.client.AnonClient;
import anon.client.DummyTrafficControlChannel;
import anon.crypto.SignatureVerifier;
import anon.infoservice.DatabaseMessage;
import anon.infoservice.ListenerInterface;
import anon.infoservice.MixCascade;
import anon.infoservice.MixCascadeExitAddresses;
import anon.infoservice.PerformanceEntry;
import anon.infoservice.SimpleMixCascadeContainer;
import anon.infoservice.Database;
import anon.infoservice.InfoServiceDBEntry;
import anon.infoservice.StatusInfo;
import anon.pay.PayAccount;
import anon.pay.PayAccountsFile;
import anon.pay.PaymentInstanceDBEntry;
import anon.proxy.AnonProxy;
import anon.util.IMiscPasswordReader;
import anon.util.XMLParseException;
import anon.util.XMLUtil;
import infoservice.Configuration;
import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;

/**
 * A simple performance meter for Mix cascades.<br>
 * 
 * The meter runs as a thread inside the <code>InfoService</code> and periodically sends requests
 * to another <code>InfoService</code> through a <code>MixCascade</code>. The transport is done
 * by a local <code>AnonProxy</code>
 * The delay (the time between sending the first byte and receiving the first byte of the response)
 * and the throughput (the data rate of the response in bytes
 * per millisecond) are measured and set to the corresponding cascade.
 *
 * @author Christian Banse
 */
public class PerformanceMeter implements Runnable, Observer
{	
	/**
	 * The time-to-live for a <code>PerformanceEntry</code>.
	 */
	public static final int PERFORMANCE_ENTRY_TTL = 1000*60*60;
	
	/**
	 * The suffix for performance log files.
	 */
	public static final String PERFORMANCE_LOG_FILE = "performance_"; 	
	
	/**
	 * Lock for the <code>PerformanceMeter</code>.
	 */
	private Object SYNC_METER = new Object();
	
	/**
	 * Lock for the socket connection to the <code>AnonProxy</code>.
	 */
	private Object SYNC_SOCKET = new Object();
	
	/**
	 * Lock for the individual test of a mix cascade.
	 */
	private Object SYNC_TEST = new Object();
	
	/**
	 * Protects certain commands from being interrupted.
	 */
	private Object SYNC_INTERRUPT = new Object();
	
	/**
	 * The size of data to be requested.
	 */
	private int m_dataSize;
	
	/**
	 * Interval in milliseconds between the tests.
	 */
	private int m_majorInterval;
	
	/**
	 * The number of requests per interval.
	 */
	private int m_requestsPerInterval;
	
	/**
	 * The test timeout.
	 */
	private int m_maxWaitForTest;
	
	/**
	 * The local <code>AnonProxy</code>.
	 */
	private AnonProxy m_proxy;
	
	/**
	 * Socket connection to the local <code>AnonProxy</code>.
	 */
	private Socket m_meterSocket;
	
	/**
	 * The host of the local <code>AnonProxy</code>
	 */
	private String m_proxyHost;
	
	/**
	 * The port of the local <code>AnonProxy</code>
	 */
	private int m_proxyPort;
	
	/**
	 * The info service configuration.
	 */
	private Configuration m_infoServiceConfig = null;
	
	/**
	 * Time of the last update.
	 */
	private long m_lastUpdate = 0;
	
	/**
	 * Estimated time of the next update. 
	 */
	private long m_nextUpdate = 0;
	
	private boolean m_bUpdate = false;
	
	/**
	 * The number of total successful updates.
	 */
	private int m_lastTotalUpdates = 0;
	
	/**
	 * The duration of the last update.
	 */
	private long m_lastUpdateRuntime = 0;
	
	/**
	 * The name of the last updated mix cascade.
	 */
	private String m_lastCascadeUpdated = "(none)";
	
	/**
	 * Total amount of bytes received from mix cascades.
	 */
	private long m_lBytesRecvd;

	/**
	 * The pay account manager.
	 */
	private PayAccountsFile m_payAccountsFile;
	
	/**
	 * A hashtable that stores the used account files.
	 */
	private Hashtable m_usedAccountFiles = new Hashtable();
	
	/**
	 * An account updater that refreshes the credit balance on the pay accounts.
	 */
	private AccountUpdater m_accUpdater = null;
	
	/**
	 * The random number generator.
	 */
	private Random ms_rnd = new Random();
	
	/**
	 * Stream to the performance log file.
	 */
	private FileOutputStream m_stream = null;
	
	/**
	 * The current week.
	 */
	private int m_currentWeek;
	
	/**
	 * A <code>Calendar</code> object for various date calculations.
	 */
	private Calendar m_cal = Calendar.getInstance();
	
	private InfoServiceDBEntry m_ownInfoService;

	/**
	 * Constructs a new <code>PerformanceMeter</code> with a given <code>AccountUpdater</code>.
	 * 
	 * @param updater An account updater for the pay accounts.
	 */
	public PerformanceMeter(AccountUpdater updater) throws IOException
	{
		init();
		
		m_accUpdater = updater;
		m_lBytesRecvd = 0;
	}
	
	/**
	 * Initializes the performance meter.
	 */
	public void init() throws IOException
	{
		m_infoServiceConfig = Configuration.getInstance(); 
		if(m_infoServiceConfig == null)
		{
			// TODO throw something. Assert InfoServiceConfig is not null
		}
		
		// get the performance meter config from the infoservice config
		Object[] a_config = m_infoServiceConfig.getPerformanceMeterConfig();
		
		m_proxyHost = (String) a_config[0];
		m_proxyPort = ((Integer) a_config[1]).intValue();
		m_dataSize = ((Integer) a_config[2]).intValue();
		m_majorInterval = ((Integer) a_config[3]).intValue();
		m_requestsPerInterval = ((Integer) a_config[4]).intValue();
		m_maxWaitForTest = ((Integer) a_config[5]).intValue();
		
		// set anon client timeout
		AnonClient.setLoginTimeout(m_maxWaitForTest);

		// set calendar to current time
		m_cal.setTime(new Date(System.currentTimeMillis()));
		
		// set the current week
		m_currentWeek = m_cal.get(Calendar.WEEK_OF_YEAR);
		
		if(m_cal.get(Calendar.DAY_OF_WEEK) != Calendar.SATURDAY)
		{
			// if it's not saturday -> import last week, too
			readOldPerformanceData(m_currentWeek - 1);
		}
		
		// import old performance date from this week
		readOldPerformanceData(m_currentWeek);
		// if we're on the last day of the week (saturday) we already have enough
		// performance data for the last 7 days.
		
		try
		{
			// open the stream for the performance log file
			m_stream = new FileOutputStream(PERFORMANCE_LOG_FILE + 				
					m_cal.get(Calendar.YEAR) + "_" + m_currentWeek + ".log", true);
		}
		catch(FileNotFoundException ex)
		{
			LogHolder.log(LogLevel.WARNING, LogType.NET, "Could not open "+ PERFORMANCE_LOG_FILE + ".");
		}
		
		// add ourself as observer to the MixCascade database so we knew when new MixCascades show up
		Database.getInstance(MixCascade.class).addObserver(this);
		
		
		// create our local anon proxy
	
		try
		{
			m_proxy = new AnonProxy(new ServerSocket(m_proxyPort, -1, InetAddress.getByName(m_proxyHost)), null, null);
		}
		catch (IOException a_e)
		{
			LogHolder.log(LogLevel.EMERG, LogType.NET, "Could not start AnonProxy for performance meter on port " + m_proxyPort + "!");
			throw a_e;
		}
		m_proxy.setDummyTraffic(DummyTrafficControlChannel.DT_MAX_INTERVAL_MS);
		
		// disable header processing and jondofox headers. these used to cause problems with the test.
		m_proxy.setHTTPHeaderProcessingEnabled(false);
		m_proxy.setJonDoFoxHeaderEnabled(false);
		
		m_ownInfoService =
			new InfoServiceDBEntry(Configuration.getInstance().getOwnName(),
								   Configuration.getInstance().getID(),
								   Configuration.getInstance().getVirtualListeners(), Configuration.getInstance().holdForwarderList(), false,
								   System.currentTimeMillis(), 0, Configuration.getInstance().isPerfServerEnabled());
	}
	
	/**
	 * Imports old performance date from the hard disk.
	 * 
	 * @param week The week we want to import.
	 */
	private void readOldPerformanceData(int week) 
	{
		int year = m_cal.get(Calendar.YEAR);
		
		if (week == 0)
		{
			year--;
			week = new GregorianCalendar(year, Calendar.DECEMBER, 31).get(Calendar.WEEK_OF_YEAR);
		}
		
		try
		{
			// open the stream
			FileInputStream stream = new FileInputStream(PERFORMANCE_LOG_FILE + 
					year + "_" + week + ".log");
			
			BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
			String line = null;
			
			// read until EOF
			while((line = reader.readLine()) != null)
			{
				// the performance data is separated by tabs so look for the tabs in our file
				int firstTab = line.indexOf('\t');
				int secondTab = line.indexOf('\t', firstTab + 1);
				int thirdTab = line.indexOf('\t', secondTab + 1);
				int fourthTab = line.indexOf('\t', thirdTab + 1);
				int fifthTab = line.indexOf('\t', fourthTab + 1);
				int sixthTab = line.indexOf('\t', fifthTab + 1);
				
				// we have found at list 3 tabs (timestamp, cascade id, speed, delay)
				if(firstTab != -1 && secondTab != -1 && thirdTab != -1)
				{
					// extract the timestamp
					long timestamp = Long.parseLong(line.substring(0, firstTab));
					
					// then the mix cascade id
					String id = line.substring(firstTab + 1, secondTab);
					
					// then the delay
					long lDelay = Integer.parseInt(line.substring(secondTab + 1, thirdTab));
					int delay = Integer.MAX_VALUE;
					if (lDelay < Integer.MAX_VALUE)
					{
						delay = (int)lDelay;
					}
					

					long lSpeed;
					int speed = Integer.MAX_VALUE;
					
					// default users to -1.  used if we are import from the old format
					int users = -1;
					
					int packets = -1;
					
					// we might be importing performance data without the users entry (old format)
					if(fourthTab == -1)
					{
						lSpeed = Integer.parseInt(line.substring(thirdTab + 1));
					}
					else
					{
						// this is the new format, speed and then users
						lSpeed = Integer.parseInt(line.substring(thirdTab + 1, fourthTab));
						if (fifthTab != -1 && sixthTab != -1)							
						{					
							// format with packets
							String strAddr = line.substring(sixthTab + 1);
							if (!strAddr.equals("0.0.0.0"))
							{
								InetAddress addr = InetAddress.getByName(strAddr);
								/*
								LogHolder.log(LogLevel.WARNING, LogType.NET, 
				        				"Adding IP address from file: " + addr.getHostAddress() + " from " + line.substring(sixthTab + 1));*/
								MixCascadeExitAddresses.addInetAddress(id, addr);
							}
							packets = Integer.parseInt(line.substring(fifthTab +1, sixthTab));
							users = Integer.parseInt(line.substring(fourthTab +1, fifthTab));
						}
						else if (fifthTab != -1)
						{
							users = Integer.parseInt(line.substring(fourthTab +1, fifthTab));
						}
						else
						{
							users = Integer.parseInt(line.substring(fourthTab +1));
						}
					}
					
					
					if (lSpeed < Integer.MAX_VALUE)
					{
						speed = (int)lSpeed;
					}
					
					// look for an existing performance entry
					PerformanceEntry entry = (PerformanceEntry) Database.getInstance(PerformanceEntry.class).getEntryById(id);
					
					// create one if necessary
					if (entry == null)
					{
						entry = new PerformanceEntry(id);
					}
					
					// import the extracted value into the performance entry
					entry.importValue(PerformanceEntry.DELAY, timestamp, delay);
					entry.importValue(PerformanceEntry.SPEED, timestamp, speed);
					
					// only import users if we have a valid value
					if (users != -1)
					{
						entry.importValue(PerformanceEntry.USERS, timestamp, users);
					}
					
					if (packets != -1)
					{
						entry.importValue(PerformanceEntry.PACKETS, timestamp, packets);
					}
					
					Database.getInstance(PerformanceEntry.class).update(entry);
				}
			}
		}
		catch(IOException ex)
		{
			LogHolder.log(LogLevel.WARNING, LogType.NET, "No previous performance data for this week found: " + ex.getMessage());
		}
		
		LogHolder.log(LogLevel.NOTICE, LogType.NET, "Added previous performance data for week" + week);
	}
	
	/**
	 * The main meter thread.
	 */
	public void run() 
	{
		boolean bFirstTestDone = false;
		// load pay account files and update them to show the correct credit balance.
		loadAccountFiles();
		m_accUpdater.update();
		
		while(true)
		{			
			long updateBegin = System.currentTimeMillis();
			int intRandom;
			
			m_lastUpdateRuntime = 0;
			m_nextUpdate = updateBegin + m_majorInterval;
			
			// retrieve all currently known mix cascades
			Vector knownMixCascades;
			synchronized(SYNC_TEST)
			{
				knownMixCascades = Database.getInstance(MixCascade.class).getEntryList();		
			}
			
			m_lastTotalUpdates = 0;
			
			// loop through all mix cascade that need to be tested
			while(knownMixCascades.size() > 0) 
			{
				synchronized(SYNC_TEST)
				{
					LogHolder.log(LogLevel.INFO, LogType.THREAD, "Cascades left to test: " + knownMixCascades.size());

					// randomly choose the next mix cascade					
					intRandom = Math.abs(ms_rnd.nextInt()) % knownMixCascades.size();
					final MixCascade cascade = (MixCascade) knownMixCascades.elementAt(intRandom);		
					
					// remove the cascade from the to-be-tested-vector
					knownMixCascades.removeElementAt(intRandom);
				
					// start the test
					startTest(cascade);
					
					// update the last update time
					if (m_lastTotalUpdates > 0)
					{
						bFirstTestDone = true;
						m_lastUpdateRuntime = System.currentTimeMillis() - updateBegin;
					}
				}
			}
			
			synchronized (SYNC_METER)
			{
				// all tests in the current interval are done, wait for the specified time
				if (!m_bUpdate)
				{
					if (!bFirstTestDone)
					{
						m_nextUpdate = System.currentTimeMillis() + 30000; // wait 30 seconds fo the next try
					}
					
					long sleepFor = m_nextUpdate - System.currentTimeMillis();    	
					
					if (sleepFor > 0)
					{
						LogHolder.log(LogLevel.INFO, LogType.NET, "Performance thread sleeping for " + sleepFor + " ms.");
			    		try 
			    		{	    			    			
		    				SYNC_METER.wait(sleepFor);	    				
			    		}
			    		catch (InterruptedException e)
			    		{
			    			//break;
			    		}	    		
		    		}
				}
				m_bUpdate = false;
			}
		}
	}
	
	/**
	 * Starts a test with giving mix cascade. The test itself is done within another thread
	 * that is being created. Otherwise the main thread might get a dead-lock.
	 *  
	 * @param a_cascade The mix cascade that needs to be tested.
	 */
	private void startTest(final MixCascade a_cascade) 
	{
		Thread performTestThread;
		
		// update the account files again
		loadAccountFiles();
		m_accUpdater.update();
		
		// check if we have the payment instance and a valid account file
		if (a_cascade.isPayment() && a_cascade.getPIID() != null)
		{			
			// check account file for validity
			PayAccount account = PayAccountsFile.getInstance().getChargedAccount(
					a_cascade.getPIID(), null);
			if (account == null)
			{
				LogHolder.log(LogLevel.WARNING, LogType.PAY, 
						"Could not start test because no valid account was available for PI " + a_cascade.getPIID() + "!");
//				 this test is useless
				return;
			}
		}
		
		// create the test thread
		performTestThread = new Thread(new Runnable()
		{
			public void run()
			{
				// the actual testing is done inside performTest()
				if (performTest(a_cascade) > 0)
				{
					m_lastTotalUpdates++;
				}										
			}
		});
		
		// start the thread
		performTestThread.start();
		try
		{
			performTestThread.join(m_maxWaitForTest);						
		}
		catch (InterruptedException e)
		{
			// test is finished
		}
						
		// interrupt the test if it is not finished yet
		if (m_proxy.isConnected())
		{
			m_proxy.stop();
		}
		try
		{
			performTestThread.join(500);						
		}
		catch (InterruptedException e)
		{
			// test is finished
		}
		
		// for some reason the test thread is still alive, try various methods to stop it
		int iWait = 0;
		while (performTestThread.isAlive())
		{							
			iWait++;			
			// try to interrupt it
			synchronized (SYNC_INTERRUPT)
			{
				performTestThread.interrupt();
			}
		
			if (iWait > 5)
			{	
				closeMeterSocket();
				if (iWait > 20)
				{
					LogHolder.log(LogLevel.EMERG, LogType.THREAD, 
						"Using deprecated stop method to finish meter thread!");
					synchronized (SYNC_INTERRUPT)
					{
						performTestThread.stop();
					}
				}
				else if (iWait > 5)
				{
				LogHolder.log(LogLevel.EMERG, LogType.THREAD, 
					"Problems finishing meter thread!");
				}

				try
				{
					performTestThread.join(1000);
				}
				catch (InterruptedException e)
				{	
				}
			}
			else
			{
				try
				{
					performTestThread.join(500);
				}
				catch (InterruptedException e)
				{								
				}
			}
		}
	}

	/**
	 * Force an update if thread is waiting.
	 */
	public void update()
	{
		synchronized (SYNC_METER)
		{
			m_bUpdate = true;
			m_nextUpdate = System.currentTimeMillis();
			SYNC_METER.notify();
		}
	}
	
	/**
	 * Loads the pay account files from the account directory.
	 * 
	 * @return <code>true</code> if successful, <code>false</code> otherwise.
	 */
	private boolean loadAccountFiles() 
	{
		LogHolder.log(LogLevel.INFO, LogType.PAY, "Looking for new account files");
		Document payAccountXMLFile = null;
		PayAccount oldAccount;
		File file;
		
		File accountDir = m_infoServiceConfig.getPerfAccountDirectory();

		if(accountDir == null)
		{
			return false;
		}
		
		try
		{
			String[] files = accountDir.list();
			
			if(files == null)
			{
				return false;
			}
			
			/* Loop through all files in the directory to find an account file we haven't used yet */
			for (int i = 0; i < files.length; i++)
			{
				try
				{
					file = new File(accountDir.getAbsolutePath() + File.separator + files[i]);					
					oldAccount = (PayAccount) m_usedAccountFiles.get(file);
					
					/* skip files that already used and have the same modify date */
					if(oldAccount != null && oldAccount.getBackupTime() == file.lastModified())
					{
						continue;
					}
					
					LogHolder.log(LogLevel.NOTICE, LogType.PAY, "Trying to add " + file.getName());
					payAccountXMLFile = XMLUtil.readXMLDocument(file);
					Element payAccountElem = (Element) XMLUtil.getFirstChildByName(payAccountXMLFile.getDocumentElement(), "Account");
					if(payAccountElem != null)
					{
						PayAccount payAccount = new PayAccount(payAccountElem,new PerformanceAccountPasswordReader());
						
						m_payAccountsFile = PayAccountsFile.getInstance();
						if(m_payAccountsFile != null)
						{
							LogHolder.log(LogLevel.INFO, LogType.PAY, "Added account file " + file.getName() + ".");
							m_payAccountsFile.addAccount(payAccount);
							m_payAccountsFile.setActiveAccount(payAccount);
							payAccount.setBackupDone(file.lastModified());
							m_usedAccountFiles.put(file, payAccount);
						}
					}										
				}
				catch (IOException e)
				{
					LogHolder.log(LogLevel.WARNING, LogType.PAY, 
						"Cannot read account file " + files[i] +" for performance monitoring.", e);
				} 
				catch (XMLParseException e) 
				{
					LogHolder.log(LogLevel.WARNING, LogType.PAY, 
						"Cannot parse account file " + files[i] +" for performance monitoring.");
				}
			}
		}
		catch (Exception e) 
		{
			LogHolder.log(LogLevel.WARNING, LogType.PAY, 
					"An error occured while processing the accountfiles, cause: ", e);
			
			return false;
		}

		return true;
	}

	/**
	 * Determines whether a performance test is allowed for the giving mix cascade or not.
	 * 
	 * @param a_cascade The mix cascade to be tested.
	 * @return <code>false</code> if the cascade is in a blacklist or on the same host as the <code>InfoService</code>, <code>true</code> otherwise.
	 */
	private boolean isPerftestAllowed(MixCascade a_cascade)
	{
		Vector cascadeHosts = a_cascade.getHosts();
		Vector isHosts = m_infoServiceConfig.getHostList();
		
		Vector blackList = m_infoServiceConfig.getPerfBlackList();
		Vector whiteList = m_infoServiceConfig.getPerfWhiteList();
		
		for(int i = 0; i < cascadeHosts.size(); i++)
		{
			String host = (String) cascadeHosts.elementAt(i);
			
			if (blackList.contains(host))
			{
				return false;
			}
			
			if (isHosts.contains(host) && !whiteList.contains(host))
			{
				return false;
			}
		}
		return true;
	}
	
	/**
	 * Closes the socket to the <code>AnonProxy</code>.
	 */
	private void closeMeterSocket()
	{
		synchronized (SYNC_SOCKET)
		{
			if (m_meterSocket != null)
			{
				try
				{
					m_meterSocket.close();
					m_meterSocket = null;
				}
				catch (IOException e)
				{
					LogHolder.log(LogLevel.WARNING, LogType.NET, e);
				}
			}
		}
	}
	
	/**
	 * Performs a performance test on the given <code>MixCascade</code>.
	 * 
	 * @param a_cascade The MixCascade that should be tested.
	 * 
	 * @return the number of performed tests
	 */
	private int performTest(MixCascade a_cascade)
	{
		int iUpdates = 0;
		int dataSize = m_dataSize;
		int requestsPerInterval = m_requestsPerInterval;
		int errorCode = ErrorCodes.E_UNKNOWN; 		
		Hashtable hashBadInfoServices = new Hashtable();
		Hashtable vDelay = new Hashtable();
		Hashtable vSpeed = new Hashtable();
		Hashtable vUsers = new Hashtable();
		Hashtable vPackets = new Hashtable();
		
		// skip cascades on the same host as the infoservice
		// and skip blacklisted cascades
		if (a_cascade == null || !isPerftestAllowed(a_cascade))
		{
			return iUpdates;
		}
		
		// try to fetch the current cascade status
		a_cascade.fetchCurrentStatus();
		
		// look for a performance entry for this cascade
		PerformanceEntry entry = (PerformanceEntry) Database.getInstance(PerformanceEntry.class).getEntryById(a_cascade.getId());
		
		// create one if necessary
		if(entry == null)
		{
			entry = new PerformanceEntry(a_cascade.getId());
		}		
		
		while(!Thread.currentThread().isInterrupted())
		{			
			// connect to the mix cascade
		    errorCode = m_proxy.start(new SimpleMixCascadeContainer(a_cascade));
		    if (errorCode == ErrorCodes.E_CONNECT || errorCode == ErrorCodes.E_UNKNOWN)
		    {
		    	//	try to recover from this error; maybe a temporary problem
				try
				{
					Thread.sleep(500);
				}
				catch (InterruptedException e)
				{
					errorCode = ErrorCodes.E_INTERRUPTED;
					break;
				}
		    }
		    else
		    {
		    	if (errorCode != ErrorCodes.E_SUCCESS)
		    	{
		    		LogHolder.log(LogLevel.WARNING, LogType.NET, 
		    				"Error connecting to cascade " + a_cascade.getMixNames() + ": " + errorCode);
		    	}
		    	break;
		    }
		}
		
		if (errorCode != ErrorCodes.E_SUCCESS || !m_proxy.isConnected())
		{
			// interrupted or any other not recoverable error 
			LogHolder.log(LogLevel.WARNING, LogType.NET, "Could not start performance test. Connection to cascade " + a_cascade.getMixNames() + " failed.");
			//return iUpdates;
		}
		else
		{
				
			LogHolder.log(LogLevel.NOTICE, LogType.NET, "Starting performance test on cascade " + a_cascade.getMixNames() + " with " + requestsPerInterval + " requests and " + m_maxWaitForTest + " ms timeout.");
			
			// hashtable that holds the delay, speed and users value from this test			
			
	//		 allocate the recv buff
			char[] recvBuff = new char[dataSize];	
			for (int i = 0; i < requestsPerInterval && !Thread.currentThread().isInterrupted() && m_proxy.isConnected(); i++)
			{
				try
				{
					if (performSingleTest(a_cascade, vDelay, vSpeed, vUsers, vPackets, hashBadInfoServices, recvBuff))
					{
						iUpdates++;
					}
				}
				catch (InterruptedException a_e)
				{
					break;
				}
				catch (InfoServiceException a_e)
				{
					LogHolder.log(LogLevel.WARNING, LogType.NET, a_e);
					requestsPerInterval = iUpdates; // do not log this as error; the InfoService failed!
					break;
				}
			}
		}
		
		// add the data hashtable to the PerformanceEntry 
		synchronized (SYNC_INTERRUPT)
		{
			int lastDelay;
			int lastSpeed;
			int lastUsers;
			int lastPackets;
			long timestamp = System.currentTimeMillis();
			
			if (iUpdates > 0)
			{
				m_lastUpdate = timestamp;
				m_lastCascadeUpdated = a_cascade.getMixNames();
			}
			
			while (iUpdates < requestsPerInterval)
			{
		    	// timestamp at which the test data was retrieved
				timestamp += 1;
				iUpdates++;
				
				// put the values into the hashtable
				vDelay.put(new Long(timestamp), new Integer(Integer.MAX_VALUE));
				vSpeed.put(new Long(timestamp), new Integer(Integer.MAX_VALUE));
				
				StatusInfo info = a_cascade.getCurrentStatus();	    
				int packets = Integer.MAX_VALUE;
	    		if(info != null)
	    		{
	    			// and store the amount of users	   
	    			long lPackets = info.getMixedPackets();
	    			if (lPackets >= Integer.MAX_VALUE)
	    			{
	    				packets = Integer.MAX_VALUE - 1;
	    			}
	    			else if (lPackets >= 0)
	    			{
	    				packets = (int)lPackets;
	    			}
	    		}
				
				vUsers.put(new Long(timestamp), new Integer(info.getNrOfActiveUsers()));
				vPackets.put(new Long(timestamp), new Integer(packets));
				
				logPerftestData(timestamp, a_cascade, Integer.MAX_VALUE, Integer.MAX_VALUE, 
						info.getNrOfActiveUsers(), packets, null);				
			}
			
			while (timestamp - System.currentTimeMillis() > 0)
			{
				try 
				{
					//wait until we have reached the current time
					Thread.sleep(50);
					Thread.yield();
				} 
				catch (InterruptedException e) 
				{
					// should never happen
					LogHolder.log(LogLevel.EMERG, LogType.THREAD, e);
				}
			}
			
			lastDelay = entry.addData(PerformanceEntry.DELAY, vDelay);
			lastSpeed = entry.addData(PerformanceEntry.SPEED, vSpeed);
			lastUsers = entry.addData(PerformanceEntry.USERS, vUsers);
			lastPackets = entry.addData(PerformanceEntry.PACKETS, vPackets);
		
			Database.getInstance(PerformanceEntry.class).update(entry);
		
	    	LogHolder.log(LogLevel.INFO, LogType.NET, "Performance test for cascade " + a_cascade.getMixNames() + " done. Last Delay: " + lastDelay + 
	    			" ms; Last Throughput: " + lastSpeed + " kb/s; Last Users:" + lastUsers + "; Last Packets: " + lastPackets);
			
	    	if (m_proxy.isConnected())
			{
	    		m_proxy.stop();
			}						
		}
    	
		return iUpdates;
	}
	
	private boolean performSingleTest(MixCascade a_cascade, Hashtable a_vDelay,
			Hashtable a_vSpeed, Hashtable a_vUsers, Hashtable a_vPackets, 
			Hashtable a_hashBadInfoServices, char[] a_recvBuff) 
		throws InterruptedException, InfoServiceException
	{		
		HTTPResponse httpResponse;
		
		Document doc;
		String host;
		int port;
		String xml;
		
		int delay = -1;
		int speed = -1;
		int users = Integer.MAX_VALUE;
		int packets = Integer.MAX_VALUE;
		long lPackets;
		long timestamp;
		InetAddress addr = null;
		
		boolean bUpdated = false;
		boolean bRetry = true;
		
		try 
    	{
    		OutputStream stream;
    		BufferedReader reader;
	       	
	       	InfoServiceDBEntry infoservice = null;
	       	
	       	// fetch cascade status
    		a_cascade.fetchCurrentStatus(m_maxWaitForTest);
    		StatusInfo info = a_cascade.getCurrentStatus();
    		
    		if(info != null)
    		{
    			// and store the amount of users
    			users = info.getNrOfActiveUsers();
    			lPackets = info.getMixedPackets();
    			if (lPackets >= Integer.MAX_VALUE)
    			{
    				packets = Integer.MAX_VALUE - 1;
    			}
    			else if (lPackets >= 0)
    			{
    				packets = (int)lPackets;
    			}
    		}
	       	
    		// loop until we have a valid token from an info service
	       	while (true)
	       	{
	       		// choose a random info service for our performance test
	       		infoservice = chooseRandomInfoService(a_hashBadInfoServices);
		       	if (infoservice == null)
		       	{		       		
		       		throw new InfoServiceException("Could not find any info services that are running a performance server.");
		       	}
		        
		       	// get the host and port from the chosen info service
		       	host = ((ListenerInterface)infoservice.getListenerInterfaces().elementAt(0)).getHost();
		       	port = ((ListenerInterface)infoservice.getListenerInterfaces().elementAt(0)).getPort();
        		
		       	// request token from info service directly
		       	PerformanceTokenRequest tokenRequest = new PerformanceTokenRequest(Configuration.getInstance().getID());
		       	doc = XMLUtil.toSignedXMLDocument(tokenRequest, SignatureVerifier.DOCUMENT_CLASS_INFOSERVICE);
		       	xml = XMLUtil.toString(doc);
		       	
		       	LogHolder.log(LogLevel.NOTICE, LogType.NET, "Requesting performance token");
		       	
		       	// open HTTP connection
		       	HTTPConnection conn = new HTTPConnection(host, port);
		       	httpResponse = conn.Post("/requestperformancetoken", xml);
		       	
		       	if(httpResponse.getStatusCode() != 200 || Thread.currentThread().isInterrupted())
		       	{			       		
		        	LogHolder.log(LogLevel.WARNING, LogType.NET, 
		        			"Token request to performance server failed. Status Code: " + httpResponse.getStatusCode());
		        	httpResponse = null;
		        	if (!Thread.currentThread().isInterrupted())
		        	{
		        		a_hashBadInfoServices.put(infoservice.getId(), infoservice);			        		
		        		continue;
		        	}
		       	}
		       	break;
	       	}
	       	
	       	if (httpResponse == null)
	       	{
	       		throw new InfoServiceException("Error while reading from infoservice");
	       	}
	       	
	       	// fetch the token from the response data
	       	PerformanceToken token = null;
	        try
	        {
	        	doc = XMLUtil.toXMLDocument(httpResponse.getData());
	        	token = new PerformanceToken(doc.getDocumentElement());
	        	
	        	LogHolder.log(LogLevel.NOTICE, LogType.NET, "Received Token " + token.getId() + ".");
	        }
	        catch(XMLParseException ex)
	        {
	        	LogHolder.log(LogLevel.ERR, LogType.NET, "Error while parsing performance token!", ex);
	        	return bUpdated;
	        }
	       	
	       	LogHolder.log(LogLevel.NOTICE, LogType.NET, "Trying to reach infoservice random data page at " + host + ":" + port + " through the mixcascade "+ a_cascade.getListenerInterface(0).getHost() +".");
	       	
	       	// open the socket connection to the AnonProxy
	       	synchronized (SYNC_SOCKET)
	       	{
	       		m_meterSocket = new Socket(m_proxyHost, m_proxyPort);	
	       		m_meterSocket.setSoTimeout(m_maxWaitForTest);			       	
		       	stream = m_meterSocket.getOutputStream();
		       	reader = new BufferedReader(new InputStreamReader(m_meterSocket.getInputStream()));
	       	}
	       	
	       	// send a PerformanceRequest to the info service while using the anonproxy
	       	PerformanceRequest perfRequest = new PerformanceRequest(token.getId(), a_recvBuff.length);
	       	doc = XMLUtil.toSignedXMLDocument(perfRequest, SignatureVerifier.DOCUMENT_CLASS_INFOSERVICE);
	       	xml = XMLUtil.toString(doc);
	       	
	       	LogHolder.log(LogLevel.NOTICE, LogType.NET, "Requesting performance data");
	       	
	       	stream.write(("POST http://" + host + ":" + port + "/requestperformance HTTP/1.0\r\n").getBytes());
	       	stream.write(("Content-Length: " + xml.length() + "\r\n\r\n").getBytes());
	       	stream.write((xml + "\r\n").getBytes());
	       	
			// read first byte for delay
	        long transferInitiatedTime = System.currentTimeMillis();
	        
	        LogHolder.log(LogLevel.INFO, LogType.NET, "Reading first byte for performance test...");
	        
	        // set a mark at the beginning
	        reader.mark(2);
	        if (reader.read() < 0)
	        {
	        	closeMeterSocket();
	        	throw new Exception("Error while reading from socket");
	        }
	        long responseStartTime = System.currentTimeMillis();
	        
	        //	delay in ms
	        if (responseStartTime - transferInitiatedTime >= Integer.MAX_VALUE)
	        {
	        	delay = Integer.MAX_VALUE - 1;
	        }
	        else
	        {
	        	delay = (int)(responseStartTime - transferInitiatedTime);
	        }
	        
	        LogHolder.log(LogLevel.INFO, LogType.NET, "Downloading bytes for performance test...");
	        
	        // reset the mark
	        reader.reset();
	        
	        HTTPResponseHeader resp = null;
	        
	        // read HTTP header from PerformanceServer
	        if (((resp = parseHTTPHeader(reader)) == null) || resp.m_statusCode != 200)
	        {       
	        	LogHolder.log(LogLevel.WARNING, LogType.NET, "Request to Performance Server failed." + (resp != null ? " Status Code: " + resp.m_statusCode : ""));
	        	closeMeterSocket();
	        	if (bRetry)
	        	{
	        		bRetry = false;
	        		if (m_proxy.isConnected())
	        		{
	        			m_proxy.stop();
	        		}
	  
	    		    if ( m_proxy.start(new SimpleMixCascadeContainer(a_cascade)) == 
	    		    	ErrorCodes.E_SUCCESS && m_proxy.isConnected())
	    		    {
	    		    	bRetry = true;
	    		    }
	        	}
	        	throw new Exception("Error while reading from mix cascade");  	
	        }
	        LogHolder.log(LogLevel.DEBUG, LogType.NET, "Performance meter parsed server header.");
	        
	        // check if the content length is supposed to be the same as the requested data size
	        if(resp.m_contentLength != a_recvBuff.length)
	        {
    			LogHolder.log(LogLevel.ERR, LogType.NET, "Performance Meter could not verify incoming package. Specified invalid Content-Length " + resp.m_contentLength + " of " + a_recvBuff.length + " bytes.");
    			closeMeterSocket();
    			throw new Exception("Invalid Packet-Length");
	        }
	        
	        int bytesRead = 0;
	        int recvd = 0;
	        int toRead = resp.m_contentLength;
	        
	        // read the whole packet
	        while(bytesRead < a_recvBuff.length) 
	        {
	        	recvd = reader.read(a_recvBuff, bytesRead, toRead);
	        	
	        	if(recvd == -1) break;
	        	bytesRead += recvd;
	        	toRead -= recvd;
	        }
	        
	        long responseEndTime = System.currentTimeMillis();
	        
	        
	        byte[] ip;
	        int ipSize = 0;
	        
	        if((byte) a_recvBuff[0] == PerformanceRequestHandler.MAGIC_BYTES_IPV4[0] &&
	           (byte) a_recvBuff[1] == PerformanceRequestHandler.MAGIC_BYTES_IPV4[1] &&
	           (byte) a_recvBuff[2] == PerformanceRequestHandler.MAGIC_BYTES_IPV4[2] &&
	           (byte) a_recvBuff[3] == PerformanceRequestHandler.MAGIC_BYTES_IPV4[3])
	        {
	        	ipSize = 4;
	        }
	        else if((byte) a_recvBuff[0] == PerformanceRequestHandler.MAGIC_BYTES_IPV6[0] &&
	           (byte) a_recvBuff[1] == PerformanceRequestHandler.MAGIC_BYTES_IPV6[1] &&
	           (byte) a_recvBuff[2] == PerformanceRequestHandler.MAGIC_BYTES_IPV6[2] &&
	           (byte) a_recvBuff[3] == PerformanceRequestHandler.MAGIC_BYTES_IPV6[3])
	        {
	        	ipSize = 16;
	        }
	        
	        if(ipSize > 0)
	        {
	        	ip = new byte[ipSize];
	        	for (int i = 0, j = ipSize; i < ipSize; i++, j++)
	        	{
	        		ip[i] = (byte) a_recvBuff[4 + i];
	        		if (a_recvBuff[4 + j] == 1)
	        		{
	        			ip[i] = (byte)(ip[i] ^ (byte)128);
	        		}
	        	}
	        
	        	try
	        	{
	        		addr = InetAddress.getByAddress(ip);
	        		/*LogHolder.log(LogLevel.WARNING, LogType.NET, 
	        				"Adding IP address: " + addr.getHostAddress());*/
	        		MixCascadeExitAddresses.addInetAddress(a_cascade.getId(), addr);	        		
	        	}
	        	catch(Exception ex)
	        	{
	        		LogHolder.log(LogLevel.WARNING, LogType.NET, "Could not parse IP address", ex);
	        	}
	        }
	        
	        // could not read all data
    		if(bytesRead != a_recvBuff.length)
    		{
    			LogHolder.log(LogLevel.WARNING, LogType.NET, "Performance Meter could not get all requested bytes. Received " + bytesRead + " of " + a_recvBuff.length + " bytes.");    			
    		}
    		
    		bytesRead += resp.m_headerBytes;
    		if (bytesRead < (100000))
			{
				// less than 100 kb was received; not enough for testing download performance
    			LogHolder.log(LogLevel.WARNING, LogType.NET, "Too few bytes for measuring speed: " + bytesRead + " bytes.");
				bytesRead = 0;
			}
    		
    		// speed in bit/s;
    		long lSpeed = (bytesRead * 8) / (responseEndTime - responseStartTime);
    		if (lSpeed <= 0 || lSpeed > Integer.MAX_VALUE)
    		{
    			speed = Integer.MAX_VALUE;
    		}
    		else
    		{
    			speed = (int)lSpeed;
    		}
    		
    		LogHolder.log(LogLevel.INFO, LogType.NET, "Verified incoming package. Delay: " + delay + " ms - Speed: " + speed + " kbit/s.");
    		
    		m_lBytesRecvd += bytesRead;        		        		
    		bUpdated = true;    		
    	}
    	catch (InterruptedIOException a_e)
    	{
    		LogHolder.log(LogLevel.WARNING, LogType.NET, a_e);
    	}
    	catch (InterruptedException a_e)
    	{
    		closeMeterSocket();
    		throw a_e;
    	}
    	catch (InfoServiceException a_e)
    	{
    		closeMeterSocket();
    		throw a_e;
    	}
    	catch(Exception e)
    	{
        	LogHolder.log(LogLevel.EXCEPTION, LogType.NET, e);
        }
    	closeMeterSocket();
    	
    	synchronized (SYNC_INTERRUPT)
		{
	    	// timestamp at which the test data was retrieved
			timestamp = System.currentTimeMillis();
			
			// put the values into the hashtable
			a_vDelay.put(new Long(timestamp), new Integer(delay));
			a_vSpeed.put(new Long(timestamp), new Integer(speed));
			a_vUsers.put(new Long(timestamp), new Integer(users));
			a_vPackets.put(new Long(timestamp), new Integer(packets));
			
			
			logPerftestData(timestamp, a_cascade, delay, speed, users, packets, addr);
		}
		
		return bUpdated;
	}
	
	private void logPerftestData(long a_timestamp, MixCascade a_cascade, int a_delay, 
			int a_speed, int a_users, int a_packets, InetAddress a_ip)
	{
		try
		{
			synchronized (SYNC_INTERRUPT)
			{
    			m_cal.setTimeInMillis(System.currentTimeMillis());
    			// check if we're still in the same week, if not open a new performance log file
    			if(m_cal.get(Calendar.WEEK_OF_YEAR) != m_currentWeek)
    			{
    				m_currentWeek = m_cal.get(Calendar.WEEK_OF_YEAR);
    				
    				// open a new stream
    				m_stream.close();
    				m_stream = new FileOutputStream(PERFORMANCE_LOG_FILE + 
    						m_cal.get(Calendar.YEAR) + "_" + m_currentWeek + ".log", true);
    			}
    			
    			m_stream.write((a_timestamp + "\t" + a_cascade.getId() + "\t" + a_delay + 
    					"\t" + a_speed + "\t" + a_users + "\t" + a_packets + "\t" +
    					(a_ip == null ? "0.0.0.0" : a_ip.getHostAddress()) + "\n").getBytes());
    			m_stream.flush();
			}
		}
		catch(IOException ex)
		{
			LogHolder.log(LogLevel.EXCEPTION, LogType.NET, ex);
		}
	}
	
	/**
	 * Parses the incoming HTTP header from the info service.
	 * 
	 * @param a_reader The buffered reader which holds the data.
	 * @return A <code>HTTPResponseHeader</code> filled with corresponding entries.
	 * @throws IOException If a stream operation fails.
	 * @throws NumberFormatException If the transfered status code is not a number.
	 */
	public HTTPResponseHeader parseHTTPHeader(BufferedReader a_reader) throws IOException, NumberFormatException
	{	
		String line;
		HTTPResponseHeader r = new HTTPResponseHeader();
		int i = 0;
		
		do
		{
			line = a_reader.readLine();			
			if (line == null || (i == 0 && !line.startsWith("HTTP"))) return null;
			
			r.m_headerBytes += line.length() + 2; // add 2 bytes for \r\n
			
			if (line.startsWith("HTTP"))
			{
				int c = line.indexOf(" ");
				if(c == -1) return null;
				r.m_statusCode = Integer.parseInt(line.substring(c + 1, c + 4));
			}
			else if(line.startsWith("Content-Length: "))
			{
				r.m_contentLength = Integer.parseInt(line.substring(16));
			}
			
			i++;
		} while(line.length() > 0);
		
		return r;
	}
	
	/**
	 * Returns the number of updates since startup.
	 * 
	 * @return The number of udpates since startup.
	 */
	public int getLastTotalUpdates()
	{
		return m_lastTotalUpdates;
	}
	
	/**
	 * Returns the runtime of the last update.
	 * 
	 * @return Returns the runtime of the last update.
	 */
	public long getLastUpdateRuntime()
	{
		return m_lastUpdateRuntime;
	}
	
	/**
	 * Returns the time of the last successful update.
	 * 
	 * @return The time of the last successful update.
	 */
	public long getLastSuccessfulUpdate()
	{
		return m_lastUpdate;
	}
	
	/**
	 * Returns the estimated time of the next update. 
	 * 
	 * @return The estimated time of the next update.
	 */
	public long getNextUpdate()
	{
		return m_nextUpdate;
	}
	
	/**
	 * Returns the name of the last updated cascade.
	 * 
	 * @return The name of the last updated cascade.
	 */
	public String getLastCascadeUpdated()
	{
		return m_lastCascadeUpdated;
	}
	
	/**
	 * Returns the amount of bytes received since startup.
	 * 
	 * @return The amount of bytes received since startup.
	 */
	public long getBytesRecvd()
	{
		return m_lBytesRecvd;
	}
	
	/**
	 * Returns a list of used pay account files.
	 * 
	 * @return A list of used pay account files.
	 */
	public Hashtable getUsedAccountFiles()
	{
		return m_usedAccountFiles;
	}
	
	/**
	 * Estimates how long the balance of all current loaded pay accounts 
	 * of one payment instance will last.
	 * 
	 * @param a_piid The payment instance.
	 * @return The time when the balance will reach zero.
	 */
	public long calculateRemainingPayTime(String a_piid)
	{
		long remainingTests;
		long trafficPerTest = calculatePayTrafficPerTest(a_piid);
		if(trafficPerTest == 0)
		{
			return 0;
		}
		
		remainingTests = getRemainingCredit(a_piid) / trafficPerTest;
		return System.currentTimeMillis() + (remainingTests * m_majorInterval);
	}

	/**
	 * Calculates how much traffic one batch of tests with all pay cascades will produce.
	 *  
	 * @param a_piid The payment instances of the pay cascade.
	 * @return The estimated traffic.
	 */
	private long calculatePayTrafficPerTest(String a_piid) 
	{
		int payCascades = 0;
		long trafficPerTest = 0;
		
		Iterator cascades = Database.getInstance(MixCascade.class).getEntryList().iterator();
		
		// determine the amount of pay cascades
		while(cascades.hasNext()) 
		{
			MixCascade cascade = (MixCascade) cascades.next();
			
			if(cascade.isPayment() && 
				cascade.getPIID() != null && cascade.getPIID().equals(a_piid) &&
				isPerftestAllowed(cascade))
			{
				payCascades++;
			}
		}
		
		// calculate the traffic per test
		trafficPerTest = payCascades * m_requestsPerInterval * m_dataSize;
		return trafficPerTest;
	}
	
	/**
	 * Estimates how much traffic all pay cascades will produce in one day.
	 * 
	 * @param a_piid The payment instance.
	 * @return The estimated traffic.
	 */
	public long calculatePayTrafficPerDay(String a_piid)
	{
		int testsPerDay = (3600 * 24 * 1000) / (m_majorInterval);
		
		return calculatePayTrafficPerTest(a_piid) * testsPerDay;
	}
	
	/**
	 * Calculates the remaining credit of all pay accounts of one payment instance.
	 * 
	 * @param a_piid The payment instance.
	 * @return The remaining credit.
	 */
	public long getRemainingCredit(String a_piid)
	{			
		if(a_piid == null || m_payAccountsFile == null)
		{
			return 0;
		}
		
		Enumeration accounts = m_payAccountsFile.getAccounts();
		long credit = 0;
		
		while(accounts.hasMoreElements())
		{
			PayAccount account = (PayAccount) accounts.nextElement();
			
			if(account.getBI() != null && account.getBI().getId().equals(a_piid))
			{
				//credit += account.getBalance().getVolumeKBytesLeft() * 1000;
				credit += account.getCurrentCredit() * 1000;
			}
		}
		
		return credit;
	}
	
	/**
	 * Randomly chooses one InfoService from the database.
	 * @param a_badInfoServices A hashtable of InfoServices that should not be chosen.
	 * @return A random InfoService.
	 */
	private InfoServiceDBEntry chooseRandomInfoService(Hashtable a_badInfoServices)
	{
		if (!a_badInfoServices.containsKey(Configuration.getInstance().getID()))
		{
			// always return this InfoService if connection to it is possible
			return m_ownInfoService;
			/*
			return (InfoServiceDBEntry)Database.getInstance(
					InfoServiceDBEntry.class).getEntryById(
							Configuration.getInstance().getID());*/
		}
		
		Vector knownIS = Database.getInstance(InfoServiceDBEntry.class).getEntryList();
		
		while(!knownIS.isEmpty())
		{
			int i = ms_rnd.nextInt(knownIS.size());
		
			InfoServiceDBEntry entry = (InfoServiceDBEntry) knownIS.elementAt(i);
			if (!a_badInfoServices.containsKey(entry.getId()) &&
				entry.isPerfServerEnabled() && !entry.getListenerInterfaces().isEmpty())
			{
				return entry; 
			}
			else
			{
				a_badInfoServices.put(entry.getId(), entry);
				knownIS.remove(entry);
			}
		}
		
		return null;
	}
	
	public void update(Observable a_observable, Object a_message)
	{
		// the observer message came from the databse
		if(a_message != null && a_message instanceof DatabaseMessage)
		{
			DatabaseMessage msg = (DatabaseMessage) a_message;
			
			// a new entry was added
			if(msg.getMessageCode() == DatabaseMessage.ENTRY_ADDED)
			{
				try
				{
					final MixCascade cascade = (MixCascade) msg.getMessageData();					
					final PerformanceEntry entry = (PerformanceEntry) Database.getInstance(PerformanceEntry.class).getEntryById(cascade.getId());
					
					// we don't have a PerformanceEntry for this cascade yet
					if (entry == null || 
						(System.currentTimeMillis() - entry.getLastUpdate()) >= m_majorInterval)
					{
						// let's start our performance test immediately
						new Thread(new Runnable()
						{
							public void run()
							{
								synchronized(SYNC_TEST)
								{
									if (entry == null || 
											(System.currentTimeMillis() - entry.getLastUpdate()) >= m_majorInterval)
									{
										LogHolder.log(LogLevel.INFO, LogType.MISC, 
												"Found new cascade, starting performance test immediately.");
										startTest(cascade);
									}
								}
							}
						}).start();
					}
				}
				catch(Exception ex)
				{
					LogHolder.log(LogLevel.EXCEPTION, LogType.MISC, "Error while starting performance test for new cascade: ", ex);
				}
			}
		}
	}
	
	/**
	 * Small helper class to store HTTP response header information.
	 * 
	 * @author Christian Banse
	 */
	private class HTTPResponseHeader
	{
		/**
		 * HTTP status code.
		 */
		int m_statusCode = -1;
		
		/**
		 * Length of the HTTP content.
		 */
		int m_contentLength = 0;
		
		int m_headerBytes = 0;
	}
	
	/**
	 * Small utility class to open pay account files with passwords
	 *  
	 * @author Christian Banse
	 */
	private final class PerformanceAccountPasswordReader implements IMiscPasswordReader
	{
		public String readPassword(Object a_message)
		{
			// the password must be specified in the InfoService config and it must be the
			// same for all pay account files.
			return m_infoServiceConfig.getPerfAccountPassword();
		}
	}
	
	private final class InfoServiceException extends Exception
	{
		public InfoServiceException(String a_message)
		{
			super(a_message);
		}
	}
}

