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
import java.util.Hashtable;
import java.util.Random;
import java.util.Calendar;
import java.util.Observer;
import java.util.Observable;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import HTTPClient.HTTPConnection;

import jap.pay.AccountUpdater;
import anon.ErrorCodes;
import anon.client.AnonClient;
import anon.client.DummyTrafficControlChannel;
import anon.crypto.SignatureVerifier;
import anon.infoservice.DatabaseMessage;
import anon.infoservice.ListenerInterface;
import anon.infoservice.MixCascade;
import anon.infoservice.PerformanceEntry;
import anon.infoservice.SimpleMixCascadeContainer;
import anon.infoservice.Database;
import anon.infoservice.InfoServiceDBEntry;
import anon.infoservice.StatusInfo;
import anon.pay.PayAccount;
import anon.pay.PayAccountsFile;
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
 * to the performance server of each known cascade. The transport is doing by setting up a local
 * <code>AnonProxy</code>
 * The delay (the time between sending the first byte and receiving the first byte of the response)
 * and the throughput (the data rate of the response in bytes
 * per millisecond) are measured and set to the corresponding cascade.
 * 
 *
 * @author Christian Banse
 */
public class PerformanceMeter implements Runnable, Observer
{	
	private Object SYNC_METER = new Object();
	private Object SYNC_SOCKET = new Object();
	private Object SYNC_TEST = new Object();
	
	private String m_proxyHost;
	private int m_proxyPort;
	private int m_dataSize;
	private int m_majorInterval;
	private int m_requestsPerInterval;
	private int m_maxWaitForTest;
	private int m_minPackets;
	
	private AnonProxy m_proxy;
	private char[] m_recvBuff;
	
	private Configuration m_infoServiceConfig = null;
	
	private long m_lastUpdate = 0;
	private long m_nextUpdate = 0;
	private int m_lastTotalUpdates = 0;
	private long m_lastUpdateRuntime = 0;
	
	private String m_lastCascadeUpdated = "(none)";
	private long m_lBytesRecvd;
	
	public static final int PERFORMANCE_ENTRY_TTL = 1000*60*60;
	public static final String PERFORMANCE_LOG_FILE = "performance_"; 
	
	private PayAccountsFile m_payAccountsFile;
	
	private Socket m_meterSocket;

	private Hashtable m_usedAccountFiles = new Hashtable();
	private AccountUpdater m_accUpdater = null;
	
	private Random ms_rnd = new Random();
	
	private FileOutputStream m_stream = null;
	
	private int m_currentWeek;
	private Calendar m_cal = Calendar.getInstance();

	public PerformanceMeter(AccountUpdater updater)
	{
		init();
		
		m_accUpdater = updater;
		m_lBytesRecvd = 0;
	}
	
	public void init() 
	{
		m_infoServiceConfig = Configuration.getInstance(); 
		if(m_infoServiceConfig == null)
		{
			//@todo: throw something. Assert InfoServiceConfig is not null
		}
		
		Object[] a_config = m_infoServiceConfig.getPerformanceMeterConfig();
		
		m_proxyHost = (String) a_config[0];
		m_proxyPort = ((Integer) a_config[1]).intValue();
		m_dataSize = ((Integer) a_config[2]).intValue();
		m_majorInterval = ((Integer) a_config[3]).intValue();
		m_requestsPerInterval = ((Integer) a_config[4]).intValue();
		m_maxWaitForTest = ((Integer) a_config[5]).intValue();
		m_minPackets = ((Integer) a_config[6]).intValue();
		AnonClient.setLoginTimeout(m_maxWaitForTest);
		//AnonClient.setLoginTimeout(LOGIN_TIMEOUT);
		
		m_cal.setTimeInMillis(System.currentTimeMillis());
		m_currentWeek = m_cal.get(Calendar.WEEK_OF_YEAR);
		
		readOldPerformanceData(m_currentWeek);
		if(m_cal.get(Calendar.DAY_OF_WEEK) != 6)
		{
			readOldPerformanceData(m_currentWeek - 1);
		}
		
		try
		{
			m_stream = new FileOutputStream(PERFORMANCE_LOG_FILE + 
				
			m_cal.get(Calendar.YEAR) + "_" + m_currentWeek + ".log", true);
		}
		catch(FileNotFoundException ex)
		{
			LogHolder.log(LogLevel.WARNING, LogType.NET, "Could not open "+ PERFORMANCE_LOG_FILE + ".");
		}
		
		Database.getInstance(MixCascade.class).addObserver(this);
	}
	
	private void readOldPerformanceData(int week) 
	{
		int year = m_cal.get(Calendar.YEAR);
		
		if(week < 0)
		{
			year--;
		}
		
		try
		{
			FileInputStream stream = new FileInputStream(PERFORMANCE_LOG_FILE + 
					year + "_" + week + ".log");
			
			BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
			String line = null;
			
			while((line = reader.readLine()) != null)
			{
				int firstTab = line.indexOf('\t');
				int secondTab = line.indexOf('\t', firstTab + 1);
				int thirdTab = line.indexOf('\t', secondTab + 1);
				int fourthTab = line.indexOf('\t', thirdTab + 1);
				
				if(firstTab != -1 && secondTab != -1 && thirdTab != -1)
				{
					long timestamp = Long.parseLong(line.substring(0, firstTab));
					String id = line.substring(firstTab + 1, secondTab);
					long delay = Long.parseLong(line.substring(secondTab + 1, thirdTab));
					
					// old format without users
					long speed = 0;
					long users = -1;
					if(fourthTab == -1)
					{
						speed = Long.parseLong(line.substring(thirdTab + 1));
					}
					else
					{
						speed = Long.parseLong(line.substring(thirdTab + 1, fourthTab));
						users = Long.parseLong(line.substring(fourthTab +1));
					}
						
					PerformanceEntry entry = (PerformanceEntry) Database.getInstance(PerformanceEntry.class).getEntryById(id);
					
					if(entry == null)
					{
						entry = new PerformanceEntry(id);
					}
					
					entry.importValue(PerformanceEntry.DELAY, timestamp, delay);
					entry.importValue(PerformanceEntry.SPEED, timestamp, speed);
					
					if(users != -1)
					{
						entry.importValue(PerformanceEntry.USERS, timestamp, users);
					}
					
					Database.getInstance(PerformanceEntry.class).update(entry);
				}
			}
		}
		catch(IOException ex)
		{
			LogHolder.log(LogLevel.WARNING, LogType.NET, "Could not read "+ PERFORMANCE_LOG_FILE + ". No previous performanace date for this week found.");
		}
		
		LogHolder.log(LogLevel.WARNING, LogType.NET, "Added previous performance data for week" + week);
	}
	
	public void run() 
	{
		Thread performTestThread;
		try
		{
			m_proxy = new AnonProxy(new ServerSocket(m_proxyPort, -1, InetAddress.getByName(m_proxyHost)), null, null);
			m_proxy.setDummyTraffic(DummyTrafficControlChannel.DT_MAX_INTERVAL_MS);
			m_proxy.setJonDoFoxHeaderEnabled(false);
		} 
		catch (UnknownHostException e1) 
		{
			LogHolder.log(LogLevel.EXCEPTION, LogType.NET, 
					"An error occured while setting up performance monitoring, cause: ", e1);
			return;
		} 
		catch (IOException e1) 
		{
			LogHolder.log(LogLevel.EXCEPTION, LogType.NET, 
					"An I/O error occured while setting up performance monitoring, cause: ", e1);
			return;
		}

		loadAccountFiles();
		m_accUpdater.update();
		
		while(true)
		{
			long updateBegin = System.currentTimeMillis();
			int intRandom;
			Random random = new Random();
			
			m_lastUpdateRuntime = 0;
			m_nextUpdate = updateBegin + m_majorInterval;
			
			Vector knownMixCascades;
			synchronized(SYNC_TEST)
			{
				knownMixCascades = Database.getInstance(MixCascade.class).getEntryList();		
			}
			
			m_lastTotalUpdates = 0;
			while(knownMixCascades.size() > 0) 
			{
				synchronized(SYNC_TEST)
				{
					LogHolder.log(LogLevel.WARNING, LogType.THREAD, "Cascades left to test: " + knownMixCascades.size());
					intRandom = Math.abs(random.nextInt()) % knownMixCascades.size();
					final MixCascade cascade = (MixCascade) knownMixCascades.elementAt(intRandom);		
					knownMixCascades.removeElementAt(intRandom);
				
					startTest(cascade);
				
					if (m_lastTotalUpdates > 0)
					{
						m_lastUpdateRuntime = System.currentTimeMillis() - updateBegin;
					}
				}
			}			
			
			synchronized (SYNC_METER)
			{
				long sleepFor = m_nextUpdate - System.currentTimeMillis();    			
				if (sleepFor > 0)
				{
					LogHolder.log(LogLevel.DEBUG, LogType.NET, "Sleeping for " + sleepFor + "ms.");
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
		}
	}

	private void startTest(final MixCascade a_cascade) 
	{
		Thread performTestThread;
		loadAccountFiles();
		m_accUpdater.update();
		performTestThread = new Thread(new Runnable()
		{
			public void run()
			{
				try
				{					
					if (performTest(a_cascade))
					{
						m_lastTotalUpdates++;
					}
				}
				catch (InterruptedException a_e)
				{
				}
			}
		});	
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
		int iWait = 0;
		while (performTestThread.isAlive())
		{	
			iWait++;
			performTestThread.interrupt();
		
			if (iWait > 5)
			{	
				closeMeterSocket();
				if (iWait > 20)
				{
					LogHolder.log(LogLevel.EMERG, LogType.THREAD, 
						"Using deprecated stop method to finish meter thread!");
					performTestThread.stop();
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
			m_nextUpdate = System.currentTimeMillis();
			SYNC_METER.notify();
		}
	}
	
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
		
		/* Loop through all files in the directory to find an account file we haven't used yet */
		try
		{
			String[] files = accountDir.list();
			
			if(files == null)
			{
				return false;
			}
			
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
						PayAccount payAccount = null;
						//LogHolder.log(LogLevel.INFO, LogType.PAY, "Trying to decode password encrypted file");
						payAccount = new PayAccount(payAccountElem,new PerformanceAccountPasswordReader());
						m_payAccountsFile = PayAccountsFile.getInstance();
						if(m_payAccountsFile != null)
						{
							LogHolder.log(LogLevel.INFO, LogType.PAY, "Added account file " + file.getName() + ".");
							m_payAccountsFile.addAccount(payAccount);
							m_payAccountsFile.setActiveAccount(payAccount.getAccountNumber());
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

	private boolean isPerftestAllowed(MixCascade a_cascade)
	{
		Vector cascadeHosts = a_cascade.getHosts();
		Vector isHosts = m_infoServiceConfig.getHostList();
		
		Vector blackList = m_infoServiceConfig.getPerfBlackList();
		Vector whiteList = m_infoServiceConfig.getPerfWhiteList();
		
		for(int i = 0; i < cascadeHosts.size(); i++)
		{
			String host = (String) cascadeHosts.elementAt(i);
			
			if(blackList.contains(host))
			{
				return false;
			}
			
			if(isHosts.contains(host) && !whiteList.contains(host))
			{
				return false;
			}
		}
		return true;
	}
	
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
	 * Performs a performance test on the given MixCascade using the parameters
	 * of m_majorInterval and m_requestsPerInterval
	 * 
	 * @param a_cascade The MixCascade that should be tested
	 * 
	 * @return true if the test was successful, false otherwise
	 */
	private boolean performTest(MixCascade a_cascade) throws InterruptedException
	{
		boolean bUpdated = false;
		int errorCode = ErrorCodes.E_UNKNOWN; 
		boolean bRetry = true;
		Hashtable hashBadInfoServices = new Hashtable();
		HTTPClient.HTTPResponse httpResponse;
		Document doc;
		String host;
		int port;
		String xml;
		
		// skip cascades on the same host as the infoservice
		if (a_cascade == null || !isPerftestAllowed(a_cascade))
		{
			return false;
		}
		
		a_cascade.fetchCurrentStatus();
		if(a_cascade.getCurrentStatus() == null || a_cascade.getCurrentStatus().getMixedPackets() < m_minPackets)
		{
			return false;
		}
		
		PerformanceEntry entry = (PerformanceEntry) Database.getInstance(PerformanceEntry.class).getEntryById(a_cascade.getId());
		
		if(entry == null)
		{
			entry = new PerformanceEntry(a_cascade.getId());
		}
		
		m_recvBuff = new char[m_dataSize];				
		
		while(!Thread.currentThread().isInterrupted())
		{			
		    errorCode = m_proxy.start(new SimpleMixCascadeContainer(a_cascade));
		    if (errorCode == ErrorCodes.E_CONNECT || errorCode == ErrorCodes.E_UNKNOWN)
		    {
		    	//	try to recover from this error; maybe a temporary problem
				Thread.sleep(2000);
		    }
		    else
		    {
		    	if (errorCode != ErrorCodes.E_SUCCESS)
		    	{
		    		LogHolder.log(LogLevel.WARNING, LogType.NET, 
		    				"Error connecting to cascade " + a_cascade.getName() + ": " + errorCode);
		    	}
		    	break;
		    }
		}
		
		if (errorCode != ErrorCodes.E_SUCCESS || !m_proxy.isConnected())
		{
			// interrupted or any other not recoverable error 
			LogHolder.log(LogLevel.WARNING, LogType.NET, "Could not start performance test. Connection to cascade " + a_cascade.getName() + " failed.");
			return bUpdated;
		}					
		
		LogHolder.log(LogLevel.WARNING, LogType.NET, "Starting performance test on cascade " + a_cascade.getName() + " with " + m_requestsPerInterval + " requests and " + m_maxWaitForTest + " ms timeout.");
		
		Hashtable vDelay = new Hashtable();
		Hashtable vSpeed = new Hashtable();
		Hashtable vUsers = new Hashtable();
		
		for(int i = 0; i < m_requestsPerInterval && !Thread.currentThread().isInterrupted() &&
			m_proxy.isConnected(); i++)
		{
    		long delay = -1;
    		long speed = -1;
    		long users = -1;
    		long timestamp;
    		
			try 
        	{
        		OutputStream stream;
        		BufferedReader reader;
		       	
		       	InfoServiceDBEntry infoservice;
		       	
	    		a_cascade.fetchCurrentStatus(m_maxWaitForTest);
	    		StatusInfo info = a_cascade.getCurrentStatus();
	    		
	    		if(info != null)
	    		{
	    			users = info.getNrOfActiveUsers();
	    		}
		       	
		       	while (true)
		       	{
		       		infoservice = chooseRandomInfoService(hashBadInfoServices);
			       	if(infoservice == null)
			       	{
			       		LogHolder.log(LogLevel.WARNING, LogType.NET, "Could not find any info services that are running a performance server.");
			       		return bUpdated;
			       	}
			        
			       	host = ((ListenerInterface)infoservice.getListenerInterfaces().elementAt(0)).getHost();
			       	port = ((ListenerInterface)infoservice.getListenerInterfaces().elementAt(0)).getPort();
	        		
			       	// request token from info service directly
			       	PerformanceTokenRequest tokenRequest = new PerformanceTokenRequest(Configuration.getInstance().getID());
			       	doc = XMLUtil.toSignedXMLDocument(tokenRequest, SignatureVerifier.DOCUMENT_CLASS_INFOSERVICE);
			       	xml = XMLUtil.toString(doc);
			       	
			       	LogHolder.log(LogLevel.WARNING, LogType.NET, "Requesting performance token");
			       	
			       	HTTPConnection conn = new HTTPConnection(host, port);
			       	httpResponse = conn.Post("/requestperformancetoken", xml);
			       	
			       	if(httpResponse.getStatusCode() != 200 || Thread.currentThread().isInterrupted())
			       	{			       		
			        	LogHolder.log(LogLevel.WARNING, LogType.NET, 
			        			"Token request to performance server failed. Status Code: " + httpResponse.getStatusCode());
			        	httpResponse = null;
			        	if (!Thread.currentThread().isInterrupted())
			        	{
			        		hashBadInfoServices.put(infoservice.getId(), infoservice);			        		
			        		continue;
			        	}
			       	}
			       	break;
		       	}
		       	
		       	if (httpResponse == null)
		       	{
		       		throw new Exception("Error while reading from infoservice");
		       	}
		       	
		       	PerformanceToken token = null;
		        try
		        {
		        	doc = XMLUtil.toXMLDocument(httpResponse.getData());
		        	token = new PerformanceToken(doc.getDocumentElement());
		        	
		        	LogHolder.log(LogLevel.WARNING, LogType.NET, "Received Token " + token.getId() + ".");
		        }
		        catch(XMLParseException ex)
		        {
		        	LogHolder.log(LogLevel.WARNING, LogType.NET, "Error while parsing performance token: " + ex.getMessage());
		        	return bUpdated;
		        }
		       	
		       	LogHolder.log(LogLevel.WARNING, LogType.NET, "Trying to reach infoservice random data page at " + host + ":" + port + " through the mixcascade "+ a_cascade.getListenerInterface(0).getHost() +".");
		       	
		       	synchronized (SYNC_SOCKET)
		       	{
		       		m_meterSocket = new Socket(m_proxyHost, m_proxyPort);	
		       		m_meterSocket.setSoTimeout(m_maxWaitForTest);			       	
			       	stream = m_meterSocket.getOutputStream();
			       	reader = new BufferedReader(new InputStreamReader(m_meterSocket.getInputStream()));
		       	}		       			       			       	
		       	
		       	PerformanceRequest perfRequest = new PerformanceRequest(token.getId(), m_dataSize);
		       	doc = XMLUtil.toSignedXMLDocument(perfRequest, SignatureVerifier.DOCUMENT_CLASS_INFOSERVICE);
		       	xml = XMLUtil.toString(doc);
		       	
		       	LogHolder.log(LogLevel.WARNING, LogType.NET, "Requesting performance data");
		       	
		       	stream.write(("POST http://" + host + ":" + port + "/requestperformance HTTP/1.0\r\n").getBytes());
		       	stream.write(("Content-Length: " + xml.length() + "\r\n\r\n").getBytes());
		       	stream.write((xml + "\r\n").getBytes());
		       	
				// read first byte for delay
		        long transferInitiatedTime = System.currentTimeMillis();
		        	
		        LogHolder.log(LogLevel.WARNING, LogType.NET, "Reading first byte for performance test...");
		        reader.mark(2);
		        if (reader.read() < 0)
		        {
		        	closeMeterSocket();
		        	throw new Exception("Error while reading from socket");
		        }
		        long responseStartTime = System.currentTimeMillis();
		        
		        //	delay in ms
        		delay = responseStartTime - transferInitiatedTime;
		        
		        LogHolder.log(LogLevel.WARNING, LogType.NET, "Downloading bytes for performance test...");
		        reader.reset();
		        
		        HTTPResponse resp = null;
		        
		        // read HTTP header from PerformanceServer
		        if(((resp = parseHTTPHeader(reader)) == null) || resp.m_statusCode != 200)
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
		        		errorCode = m_proxy.start(new SimpleMixCascadeContainer(a_cascade));
		    		    if (errorCode == ErrorCodes.E_SUCCESS && m_proxy.isConnected())
		    		    {
		    		    	bRetry = true;
		    		    }
		        	}		        	
		        	throw new Exception("Error while reading from mix cascade");  	
		        }
		        LogHolder.log(LogLevel.WARNING, LogType.NET, "Performance meter parsed server header.");
		        
		        if(resp.m_length != m_dataSize)
		        {
        			LogHolder.log(LogLevel.WARNING, LogType.NET, "Performance Meter could not verify incoming package. Specified invalid Content-Length " + resp.m_length + " of " + m_dataSize + " bytes.");
        			closeMeterSocket();
        			throw new Exception("Invalid Packet-Length");
		        }
		        
		        int bytesRead = 0;
		        int recvd = 0;
		        int toRead = resp.m_length;
		        
		        while(bytesRead < m_dataSize) 
		        {
		        	recvd = reader.read(m_recvBuff, bytesRead, toRead);

		        	if(recvd == -1) break;
		        	bytesRead += recvd;
		        	toRead -= recvd;
		        }
		        
		        long responseEndTime = System.currentTimeMillis();
		        
        		if(bytesRead != m_dataSize)
        		{
        			LogHolder.log(LogLevel.WARNING, LogType.NET, "Performance Meter could not get all requested bytes. Received " + bytesRead + " of " + m_dataSize + " bytes.");
        			if (bytesRead < (1000))
        			{
        				// less than 1 kb was received; not enough for testing download performance
        				bytesRead = 0;
        			}
        		}        		        		
        		
        		// speed in bit/s;
        		speed = (bytesRead * 8) / (responseEndTime - responseStartTime);
        		
        		LogHolder.log(LogLevel.WARNING, LogType.NET, "Verified incoming package. Delay: " + delay + " ms - Speed: " + speed + " kbit/s.");
        		
        		m_lBytesRecvd += bytesRead;        		        		
        		bUpdated = true;
        		
        		closeMeterSocket();
        	}
        	catch (InterruptedIOException a_e)
        	{
        		LogHolder.log(LogLevel.WARNING, LogType.NET, a_e);
        	}
        	catch(Exception e)
        	{
	        	LogHolder.log(LogLevel.EXCEPTION, LogType.NET, e);
	        }
        	
    		timestamp = System.currentTimeMillis();
    		
    		vDelay.put(new Long(timestamp), new Long(delay));
    		vSpeed.put(new Long(timestamp), new Long(speed));
    		vUsers.put(new Long(timestamp), new Long(users));
    		
    		try
    		{
    			m_cal.setTimeInMillis(System.currentTimeMillis());
    			if(m_cal.get(Calendar.WEEK_OF_YEAR) != m_currentWeek)
    			{
    				m_currentWeek = m_cal.get(Calendar.WEEK_OF_YEAR);
    				
    				// open a new stream
    				m_stream.close();
    				m_stream = new FileOutputStream(PERFORMANCE_LOG_FILE + 
    						m_cal.get(Calendar.YEAR) + "_" + m_currentWeek + ".log", true);
    			}
    			
    			m_stream.write((timestamp + "\t" + a_cascade.getId() + "\t" + delay + "\t" + speed + "\t" + users + "\n").getBytes());
    		}
    		catch(IOException ex)
    		{
    			LogHolder.log(LogLevel.EXCEPTION, LogType.NET, ex);
    		}
		}
		
		long lastDelay = entry.addData(PerformanceEntry.DELAY, vDelay);
		long lastSpeed = entry.addData(PerformanceEntry.SPEED, vSpeed);
		long lastUsers = entry.addData(PerformanceEntry.USERS, vUsers);
		
		Database.getInstance(PerformanceEntry.class).update(entry);
		
    	LogHolder.log(LogLevel.WARNING, LogType.NET, "Performance test for cascade " + a_cascade.getName() + " done. Last Delay: " + lastDelay + " ms; Last Throughput: " + lastSpeed + " kb/s; Last Users:" + lastUsers);
		
    	if (m_proxy.isConnected())
		{
    		m_proxy.stop();
		}
		
		if (bUpdated)
		{
			m_lastUpdate = System.currentTimeMillis();
			m_lastCascadeUpdated = a_cascade.getName();
		}
    	
		return bUpdated;
	}
		
	public HTTPResponse parseHTTPHeader(BufferedReader a_reader) throws IOException, NumberFormatException
	{	
		String line;
		HTTPResponse r = new HTTPResponse();
		int i = 0;
		
		do
		{
			line = a_reader.readLine();
			if(line == null || (i == 0 && !line.startsWith("HTTP"))) return null;
			
			if(line.startsWith("HTTP"))
			{
				int c = line.indexOf(" ");
				if(c == -1) return null;
				r.m_statusCode = Integer.parseInt(line.substring(c + 1, c + 4));
			}
			
			if(line.startsWith("Content-Length: "))
			{
				r.m_length = Integer.parseInt(line.substring(16));
			}
			
			i++;
		} while(line.length() > 0);
		
		return r;
	}
	
	public int getLastTotalUpdates()
	{
		return m_lastTotalUpdates;
	}
	
	public long getLastUpdateRuntime()
	{
		return m_lastUpdateRuntime;
	}
	
	public long getLastSuccessfulUpdate()
	{
		return m_lastUpdate;
	}
	
	public long getNextUpdate()
	{
		return m_nextUpdate;
	}
	
	public String getLastCascadeUpdated()
	{
		return m_lastCascadeUpdated;
	}
	
	public long getBytesRecvd()
	{
		return m_lBytesRecvd;
	}
	
	public Hashtable getUsedAccountFiles()
	{
		return m_usedAccountFiles;
	}
	
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

	private long calculatePayTrafficPerTest(String a_piid) 
	{
		int payCascades = 0;
		long trafficPerTest = 0;
		
		Iterator cascades = Database.getInstance(MixCascade.class).getEntryList().iterator();
		
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
		
		trafficPerTest = payCascades * m_requestsPerInterval * m_dataSize;
		return trafficPerTest;
	}
	
	public long calculatePayTrafficPerDay(String a_piid)
	{
		int testsPerDay = (3600 * 24 * 1000) / (m_majorInterval);
		
		return calculatePayTrafficPerTest(a_piid) * testsPerDay;
	}
	
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
				credit += account.getBalance().getCredit() * 1000;
			}
		}
		
		return credit;
	}
	
	public InfoServiceDBEntry chooseRandomInfoService(Hashtable a_badInfoServices)
	{
		if (!a_badInfoServices.contains(Configuration.getInstance().getID()))
		{
			// always return this InfoService if connection to it is possible
			return (InfoServiceDBEntry)Database.getInstance(
					InfoServiceDBEntry.class).getEntryById(
							Configuration.getInstance().getID());
		}
		
		Vector knownIS = Database.getInstance(InfoServiceDBEntry.class).getEntryList();
		
		while(!knownIS.isEmpty())
		{
			int i = ms_rnd.nextInt(knownIS.size());
		
			InfoServiceDBEntry entry = (InfoServiceDBEntry) knownIS.elementAt(i);
			if (a_badInfoServices.get(entry.getId()) == null &&
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
		if(a_message != null && a_message instanceof DatabaseMessage)
		{
			DatabaseMessage msg = (DatabaseMessage) a_message;
			
			if(msg.getMessageCode() == DatabaseMessage.ENTRY_ADDED)
			{
				try
				{
					MixCascade cascade = (MixCascade) msg.getMessageData();
					
					PerformanceEntry entry = (PerformanceEntry) Database.getInstance(PerformanceEntry.class).getEntryById(cascade.getId());
					
					if(entry == null)
					{
						// new cascade, let's start our performance test immediately
						LogHolder.log(LogLevel.INFO, LogType.MISC, "Found new cascade, starting performance test immediately.");
						synchronized(SYNC_TEST)
						{
							startTest(cascade);
						}
					}
				}
				catch(Exception ex)
				{
					LogHolder.log(LogLevel.EXCEPTION, LogType.MISC, "Error while starting performance test for new cascade: ", ex);
				}
			}
		}
	}
		
	private class HTTPResponse
	{
		int m_statusCode = -1;
		int m_length = 0;
	}
	
	private final class PerformanceAccountPasswordReader implements IMiscPasswordReader
	{
		public String readPassword(Object a_message)
		{
			return m_infoServiceConfig.getPerfAccountPassword();
		}
	}

}

