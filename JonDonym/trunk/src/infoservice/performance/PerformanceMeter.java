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
import java.io.OutputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Iterator;
import java.net.InetAddress;
import java.net.Socket;
import java.net.ServerSocket;
import java.net.UnknownHostException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;
import anon.infoservice.MixCascade;
import anon.infoservice.MixInfo;
import anon.infoservice.PerformanceEntry;
import anon.infoservice.SimpleMixCascadeContainer;
import anon.infoservice.Database;
import anon.pay.PayAccount;
import anon.pay.PayAccountsFile;
import anon.proxy.AnonProxy;
import anon.util.IMiscPasswordReader;
import anon.util.XMLParseException;
import anon.util.XMLUtil;
import infoservice.Configuration;
import java.math.BigInteger;

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
	
	private AnonProxy proxy;
	private char[] m_recvBuff;
	
	private Configuration m_infoServiceConfig = null;
	
	private long m_lastUpdate;
	private String m_lastCascadeUpdated = "(none)";
	private long m_lKiloBytesRecvd;
	
	public static final int PERFORMANCE_SERVER_TIMEOUT = 5000;
	public static final int PERFORMANCE_ENTRY_TTL = 1000*60*60;
	public static final int MINOR_INTERVAL = 1000;
	
	public PerformanceMeter(Object[] a_config)
	{
		m_proxyHost = (String) a_config[0];
		m_proxyPort = ((Integer) a_config[1]).intValue();
		m_dataSize = ((Integer) a_config[2]).intValue();
		m_majorInterval = ((Integer) a_config[3]).intValue();
		m_requestsPerInterval = ((Integer) a_config[4]).intValue();
		m_infoServiceConfig = Configuration.getInstance(); 
		if(m_infoServiceConfig == null)
		{
			//@todo: throw something. Assert InfoServiceConfig is not null
		}
		
		m_lKiloBytesRecvd = 0;
	}
	
	public void run() 
	{
		try
		{
			proxy = new AnonProxy(new ServerSocket(m_proxyPort, -1, InetAddress.getByName(m_proxyHost)), null, null);
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
		
		/* @todo: first effort to get infoservice connected with PayCascades */
		/* read in an account file, as it is exported and password protected by JAP */
		Document payAccountXMLFile = null;
		boolean accountLoaded = false;
		String payAccountFileName = 
			m_infoServiceConfig.getPerfAccountFile();
		if(payAccountFileName != null)
		{
			try 
			{
				payAccountXMLFile = XMLUtil.readXMLDocument(new File(payAccountFileName));
				Element payAccountElem = (Element) XMLUtil.getFirstChildByName(payAccountXMLFile.getDocumentElement(), "Account");
				if(payAccountElem != null)
				{
					PayAccount payAccount = null;
					payAccount = new PayAccount(payAccountElem,new PerformanceAccountPasswordReader());
					PayAccountsFile payAccountsFile = PayAccountsFile.getInstance();
					
					if(payAccountsFile != null)
					{
						payAccountsFile.addAccount(payAccount);
						payAccountsFile.setActiveAccount(payAccount.getAccountNumber());
						accountLoaded = true;
					}
				}
			}
			catch (IOException e) 
			{
				LogHolder.log(LogLevel.WARNING, LogType.PAY, 
					"Cannot read account file "+payAccountFileName+" for performance monitoring.");
			} 
			catch (XMLParseException e) 
			{
				LogHolder.log(LogLevel.WARNING, LogType.PAY, 
					"Cannot parse account file "+payAccountFileName+" for performance monitoring.");
			}
			catch (Exception e) 
			{
				LogHolder.log(LogLevel.WARNING, LogType.PAY, 
						"An error occured while accessing the accountfile: "+payAccountFileName+
						", cause: ",e);
			}
		}
		
		if(!accountLoaded)
		{
			System.out.println("Error loading account file");
			LogHolder.log(LogLevel.WARNING, LogType.PAY, 
					"Loading of accountfile: "+payAccountFileName+
					" failed. Infoservice cannot perform performance check for pay cascades.");
		}

		while(true)
		{
			Iterator knownMixCascades = Database.getInstance(MixCascade.class).getEntryList().iterator();

			while(knownMixCascades.hasNext()) 
			{
				MixCascade cascade = (MixCascade) knownMixCascades.next();
				if(cascade.hasPerformanceServer())
				{
					performTest(cascade);
				}
			}
			
    		try 
    		{
    			LogHolder.log(LogLevel.DEBUG, LogType.NET, "Sleeping for " + m_majorInterval);
    			Thread.sleep(m_majorInterval);
    		} 
    		catch (InterruptedException e)
    		{
    			//@todo: ignore or terminate ?
    		}
		}
	}

	/**
	 * Performs a performance test on the given MixCascade using the parameters
	 * of m_minorInterval and m_requestsPerInterval
	 * 
	 * @param a_cascade The MixCascade that should be tested
	 * 
	 * @return true if the test was successful, false otherwise
	 */
	private boolean performTest(MixCascade a_cascade) 
	{
		if(a_cascade == null)
		{
			return false;
		}
	

		PerformanceEntry entry = new PerformanceEntry(a_cascade.getId(), System.currentTimeMillis() + m_majorInterval + PERFORMANCE_ENTRY_TTL);
		
		m_recvBuff = new char[m_dataSize];
		
		/*if((proxy.getMixCascade() != a_cascade))
		{*/
			proxy.start(new SimpleMixCascadeContainer(a_cascade));
			
			synchronized(proxy)
			{
				try
				{
					// @todo: doesn't work
					proxy.wait(1000);
				}
				catch(InterruptedException ex)
				{
				
				}
			}
		//}
		
		if(!proxy.isConnected())
		{
			LogHolder.log(LogLevel.INFO, LogType.NET, "Could not start performance test. Connection to cascade " + a_cascade.getName() + " failed.");
			return false;
		}
		
		LogHolder.log(LogLevel.INFO, LogType.NET, "Starting performance test on cascade " + a_cascade.getName() + " with " + m_requestsPerInterval + " requests and " + MINOR_INTERVAL + " ms interval.");
		
		for(int i = 0; i < m_requestsPerInterval; i++)
		{
        	try 
        	{
        		long delay;
        		long speed;
					
		       	Socket s = new Socket(m_proxyHost, m_proxyPort);
		       	s.setSoTimeout(PERFORMANCE_SERVER_TIMEOUT);
		       	
		       	OutputStream stream = s.getOutputStream();

		       	BufferedReader reader = new BufferedReader(new InputStreamReader(s.getInputStream()));
		       	HTTPResponse resp;
		       	
		       	MixInfo lastMix = a_cascade.getMixInfo(a_cascade.getNumberOfMixes() - 1);
		       	
		       	LogHolder.log(LogLevel.INFO, LogType.NET, "Connecting to Performance Server at " + lastMix.getPerformanceServerHost() + ":" + lastMix.getPerformanceServerPort() + " through the mixcascade "+ a_cascade.getListenerInterface(0).getHost() +".");
		       	
		       	stream.write(("CONNECT " + lastMix.getPerformanceServerHost()  + ":" + lastMix.getPerformanceServerPort() +" HTTP/1.0\r\n\r\n").getBytes());
		       	
		       	// read HTTP header from Anon Proxy
		       	if(((resp = parseHTTPHeader(reader)) == null) || resp.m_statusCode != 200)
		       	{
		       		LogHolder.log(LogLevel.INFO, LogType.NET, "Connection to Performance Server failed." + (resp != null ? " Status Code: " + resp.m_statusCode : ""));
		       		s.close();
		       		
		       		// TODO: try it twice?
		        	break;
		        }
		        
		        String data = 
		        	"<SendDummyDataRequest dataLength=\"" + m_dataSize +"\">" +
		        	"	<InfoService id=\"" + m_infoServiceConfig.getID() + "\">" +
		        	"	</InfoService>" +
		        	"</SendDummyDataRequest>";
		        
				stream.write("POST /senddummydata\r\n".getBytes());
				stream.write(("Content-Length: " + data.getBytes().length +"\r\n\r\n").getBytes());
				stream.write(data.getBytes());
					
				// read first byte for delay
		        long transferInitiatedTime = System.currentTimeMillis();
		        	
		        reader.mark(2);
		        reader.read();
		        	
		        long responseStartTime = System.currentTimeMillis();
		        
		        reader.reset();
		        
		        // read HTTP header from PerformanceServer
		        if(((resp = parseHTTPHeader(reader)) == null) || resp.m_statusCode != 200)
		        {
		        	LogHolder.log(LogLevel.INFO, LogType.NET, "Request to Performance Server failed." + (resp != null ? " Status Code: " + resp.m_statusCode : ""));
		        	s.close();
		        	// TODO: try it twice?
		        	break;
		        }
		        
		        if(resp.m_length != m_dataSize)
		        {
        			LogHolder.log(LogLevel.INFO, LogType.NET, "Performance Meter could not verify incoming package. Specified invalid Content-Length " + resp.m_length + " of " + m_dataSize + " bytes.");
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
        			LogHolder.log(LogLevel.INFO, LogType.NET, "Performance Meter could not verify incoming package. Recieved " + bytesRead + " of " + m_dataSize + " bytes.");
        			s.close();
        			continue;
        		}
        		
        		// delay in ms
        		delay = responseStartTime - transferInitiatedTime;
        		
        		// speed in kbit/sec
        		speed = (long) (m_dataSize * 8 / (responseEndTime - responseStartTime));
        		
        		LogHolder.log(LogLevel.INFO, LogType.NET, "Verified incoming package. Delay: " + delay + " ms - Speed: " + speed + " kbit/sec.");
        		
        		entry.updateDelay(delay, m_requestsPerInterval);
        		entry.updateSpeed(speed, m_requestsPerInterval);

        		m_lKiloBytesRecvd += bytesRead / 1024;
        		
        		if(m_lKiloBytesRecvd < 0) 
        			m_lKiloBytesRecvd = 0;
        		
		       	s.close();
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
    			
    		}
		}
		
    	LogHolder.log(LogLevel.INFO, LogType.NET, "Performance test for cascade " + a_cascade.getName() + " done. Avg Delay: " + entry.getAverageDelay() + " ms; Avg Throughput: " + entry.getAverageSpeed() + " kb/sec");
		
    	Database.getInstance(PerformanceEntry.class).update(entry);
		proxy.stop();
		
		m_lastUpdate = System.currentTimeMillis();
		m_lastCascadeUpdated = a_cascade.getName();
    	
		return true;
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
	
	public long getLastUpdate()
	{
		return m_lastUpdate;
	}
	
	public String getLastCascadeUpdated()
	{
		return m_lastCascadeUpdated;
	}
	
	public long getKiloBytesRecvd()
	{
		return m_lKiloBytesRecvd;
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

