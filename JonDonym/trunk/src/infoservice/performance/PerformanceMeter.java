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

import jap.pay.AccountUpdater;

import java.io.File;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Vector;
import java.net.InetAddress;
import java.net.Socket;
import java.net.ServerSocket;
import java.net.UnknownHostException;
import java.util.Hashtable;
import java.util.Random;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;
import HTTPClient.HTTPConnection;
import anon.ErrorCodes;
import anon.client.DummyTrafficControlChannel;
import anon.crypto.SignatureVerifier;
import anon.infoservice.ListenerInterface;
import anon.infoservice.MixCascade;
import anon.infoservice.PerformanceEntry;
import anon.infoservice.SimpleMixCascadeContainer;
import anon.infoservice.Database;
import anon.infoservice.InfoServiceDBEntry;
import anon.pay.PayAccount;
import anon.pay.PayAccountsFile;
import anon.proxy.AnonProxy;
import anon.util.IMiscPasswordReader;
import anon.util.XMLParseException;
import anon.util.XMLUtil;
import infoservice.Configuration;
import infoservice.HttpResponseStructure;

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
 * @see anon.infoservice.MixCascade#setDelay(long)
 * @see anon.infoservice.MixCascade#setThroughput(double)
 *
 * @author cbanse, oliver
 */
public class PerformanceMeter implements Runnable 
{	
	private String m_proxyHost;
	private int m_proxyPort;
	private int m_dataSize;
	private int m_majorInterval;
	private int m_requestsPerInterval;
	private int m_maxWaitForTest;
	
	private AnonProxy m_proxy;
	private char[] m_recvBuff;
	
	private Configuration m_infoServiceConfig = null;
	
	private long m_lastUpdate = 0;
	private long m_nextUpdate = 0;
	private int m_lastTotalUpdates = 0;
	private long m_lastUpdateRuntime = 0;
	
	private String m_lastCascadeUpdated = "(none)";
	private long m_lBytesRecvd;
	
	public static final int PERFORMANCE_SERVER_TIMEOUT = 60000;
	public static final int PERFORMANCE_ENTRY_TTL = 1000*60*60;
	public static final int MINOR_INTERVAL = 1000;	
	private PayAccountsFile m_payAccountsFile;

	private Hashtable m_usedAccountFiles = new Hashtable();
	private AccountUpdater m_accUpdater = null;
	
	private Random ms_rnd = new Random();

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
	}
	
	public void run() 
	{
		Thread performTestThread;
		try
		{
			m_proxy = new AnonProxy(new ServerSocket(m_proxyPort, -1, InetAddress.getByName(m_proxyHost)), null, null);
			//m_proxy.setDummyTraffic(DummyTrafficControlChannel.DT_DISABLE);
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
			m_lastUpdateRuntime = 0;
			m_nextUpdate = updateBegin + m_majorInterval;
						
			Enumeration knownMixCascades = Database.getInstance(MixCascade.class).getEntryList().elements();
			m_lastTotalUpdates = 0;
			while(knownMixCascades.hasMoreElements()) 
			{
				final MixCascade cascade = (MixCascade) knownMixCascades.nextElement();

				loadAccountFiles();
				m_accUpdater.update();
				performTestThread = new Thread(new Runnable()
				{
					public void run()
					{
						try
						{					
							if (performTest(cascade))
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
					
					if (iWait >= 5)
					{			
						if (iWait == 5)
						{
							LogHolder.log(LogLevel.EMERG, LogType.THREAD, "Problems finishing meter thread!");
						}
						if (iWait > 5)
						{
							LogHolder.log(LogLevel.EMERG, LogType.THREAD, 
									"Using deprecated stop routine to finish meter thread!");
							performTestThread.stop();
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
			if (m_lastTotalUpdates > 0)
			{
				m_lastUpdateRuntime = System.currentTimeMillis() - updateBegin;
			}
			
    		try 
    		{
    			long sleepFor = m_nextUpdate - System.currentTimeMillis();
    			if (sleepFor > 0)
    			{
    				LogHolder.log(LogLevel.DEBUG, LogType.NET, "Sleeping for " + sleepFor + "ms.");
    				Thread.sleep(sleepFor);
    			}
    		}
    		catch (InterruptedException e)
    		{
    			//break;
    		}
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
		
		// skip cascades on the same host as the infoservice
		if (a_cascade == null || !isPerftestAllowed(a_cascade))
		{
			return false;
		}
		
		PerformanceEntry entry = new PerformanceEntry(a_cascade.getId(), System.currentTimeMillis() + m_majorInterval + PERFORMANCE_ENTRY_TTL);
		
		m_recvBuff = new char[m_dataSize];				
		
		for (int i = 0; i < m_requestsPerInterval && !Thread.currentThread().isInterrupted() &&
		     (errorCode = m_proxy.start(new SimpleMixCascadeContainer(a_cascade))) == ErrorCodes.E_CONNECT;
			 i++)
		{
			// try to recover from this error
			Thread.yield();			
		}
		
		if (Thread.currentThread().isInterrupted())
		{
			return false;
		}
		
		if (errorCode != ErrorCodes.E_SUCCESS || !m_proxy.isConnected())
		{
			// interrupted or any other not recoverable error 
			LogHolder.log(LogLevel.WARNING, LogType.NET, "Could not start performance test. Connection to cascade " + a_cascade.getName() + " failed.");
			return false;
		}					
		
		LogHolder.log(LogLevel.WARNING, LogType.NET, "Starting performance test on cascade " + a_cascade.getName() + " with " + m_requestsPerInterval + " requests and " + MINOR_INTERVAL + " ms interval.");
		
		for(int i = 0; i < m_requestsPerInterval && !Thread.currentThread().isInterrupted() &&
			m_proxy.isConnected(); i++)
		{
        	try 
        	{
        		long delay;
        		long speed;
				
		       	
		       	ListenerInterface iface = chooseRandomInfoService();
		       	if(iface == null)
		       	{
		       		LogHolder.log(LogLevel.WARNING, LogType.NET, "Could not find any info services that are running a performance server.");
		       		return false;
		       	}
		       	
		       	String host = iface.getHost();
		       	int port = iface.getPort();
        		
		       	// request token from info service directly
		       	PerformanceTokenRequest tokenRequest = new PerformanceTokenRequest(Configuration.getInstance().getID());
		       	Document doc = XMLUtil.toSignedXMLDocument(tokenRequest, SignatureVerifier.DOCUMENT_CLASS_INFOSERVICE);
		       	String xml = XMLUtil.toString(doc);
		       	
		       	LogHolder.log(LogLevel.WARNING, LogType.NET, "Requesting performance token");
		       	
		       	HTTPConnection conn = new HTTPConnection(host, port);
		       	HTTPClient.HTTPResponse httpResponse = conn.Post("/requestperformancetoken", xml);
		       	
		       	if(httpResponse.getStatusCode() != 200 || 
			       Thread.currentThread().isInterrupted())
		       	{
		        	LogHolder.log(LogLevel.WARNING, LogType.NET, "Request to Performance Server failed. Status Code: " + httpResponse.getStatusCode());
		        	break;
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
		        	return false;
		        }
		       	
		       	LogHolder.log(LogLevel.WARNING, LogType.NET, "Trying to reach infoservice random data page at " + host + ":" + port + " through the mixcascade "+ a_cascade.getListenerInterface(0).getHost() +".");
		       	
		       	Socket s = new Socket(m_proxyHost, m_proxyPort);
		       	s.setSoTimeout(PERFORMANCE_SERVER_TIMEOUT);
		       	
		       	OutputStream stream = s.getOutputStream();
		       	
		       	BufferedReader reader = new BufferedReader(new InputStreamReader(s.getInputStream()));
		       	
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
		        reader.read();
		        
		        long responseStartTime = System.currentTimeMillis();
		        LogHolder.log(LogLevel.WARNING, LogType.NET, "Downloading bytes for performance test...");
		        reader.reset();
		        
		        HTTPResponse resp = null;
		        
		        // read HTTP header from PerformanceServer
		        if(((resp = parseHTTPHeader(reader)) == null) || resp.m_statusCode != 200)
		        {
		        	LogHolder.log(LogLevel.WARNING, LogType.NET, "Request to Performance Server failed." + (resp != null ? " Status Code: " + resp.m_statusCode : ""));
		        	s.close();
		        	// TODO: try it twice?
		        	break;
		        }
		        
		        if(resp.m_length != m_dataSize)
		        {
        			LogHolder.log(LogLevel.WARNING, LogType.NET, "Performance Meter could not verify incoming package. Specified invalid Content-Length " + resp.m_length + " of " + m_dataSize + " bytes.");
        			s.close();
        			continue;
		        }
		        
		        int bytesRead = 0;
		        int recvd = 0;
		        int toRead = resp.m_length;
		        
		        while(bytesRead < m_dataSize) 
		        {
		        	try
		        	{
		        		recvd = reader.read(m_recvBuff, bytesRead, toRead);
		        	}
		        	catch(Exception ex)
		        	{
		        		continue;
		        	}
		        	if(recvd == -1) break;
		        	bytesRead += recvd;
		        	toRead -= recvd;
		        }
		        
		        long responseEndTime = System.currentTimeMillis();
		        
        		if(bytesRead != m_dataSize)
        		{
        			LogHolder.log(LogLevel.WARNING, LogType.NET, "Performance Meter could not verify incoming package. Recieved " + bytesRead + " of " + m_dataSize + " bytes.");
        			s.close();
        			continue;
        		}
        		
        		// delay in ms
        		delay = responseStartTime - transferInitiatedTime;
        		
        		// speed in bit/sec;
        		speed = (m_dataSize * 8) / (responseEndTime - responseStartTime);
        		
        		LogHolder.log(LogLevel.WARNING, LogType.NET, "Verified incoming package. Delay: " + delay + " ms - Speed: " + speed + " kbit/sec.");
        		
        		entry.updateDelay(delay, m_requestsPerInterval);
        		entry.updateSpeed(speed, m_requestsPerInterval);        		        		        		
        		m_lBytesRecvd += bytesRead;        		        		
        		bUpdated = true;
        		
		       	s.close();
        	}
        	catch (InterruptedIOException a_e)
        	{
        		LogHolder.log(LogLevel.WARNING, LogType.NET, a_e);
        		break;
        	}
        	catch(Exception e)
        	{
	        	LogHolder.log(LogLevel.EXCEPTION, LogType.NET, e);
	        }
        	
    		try 
    		{
    			Thread.sleep(MINOR_INTERVAL);
    		} 
    		catch (InterruptedException e) 
    		{
    			break;
    		}
		}
		
    	LogHolder.log(LogLevel.WARNING, LogType.NET, "Performance test for cascade " + a_cascade.getName() + " done. Avg Delay: " + entry.getAverageDelay() + " ms; Avg Throughput: " + entry.getAverageSpeed() + " kb/sec");
		
    	Database.getInstance(PerformanceEntry.class).update(entry);
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
	
	public ListenerInterface chooseRandomInfoService()
	{
		Vector knownIS = Database.getInstance(InfoServiceDBEntry.class).getEntryList();
		
		while(!knownIS.isEmpty())
		{
			int i = ms_rnd.nextInt(knownIS.size());
		
			InfoServiceDBEntry entry = (InfoServiceDBEntry) knownIS.elementAt(i);
			if(entry.isPerfServerEnabled() && !entry.getListenerInterfaces().isEmpty())
			{
				return (ListenerInterface) entry.getListenerInterfaces().elementAt(0); 
			}
			else
			{
				knownIS.remove(entry);
			}
		}
		
		return null;
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

