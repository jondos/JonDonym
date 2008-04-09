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

import java.io.IOException;
import java.io.OutputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.Socket;
import java.net.ServerSocket;

import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;
import anon.infoservice.MixCascade;
import anon.infoservice.SimpleMixCascadeContainer;
import anon.proxy.AnonProxy;

/**
 * A simple performance meter for Mix cascades.<br>
 * The meter runs as a thread inside the <code>InfoService</code> and periodically sends requests
 * to the ECHO port of each known cascade's exit point. The delay (the time between sending the first byte
 * and receiving the first byte of the response) and the throughput (the data rate of the response in bytes
 * per millisecond) are measured and set to the corresponding cascade.
 * @see anon.infoservice.MixCascade#setDelay(long)
 * @see anon.infoservice.MixCascade#setThroughput(double)
 *
 * @author oliver
 *
 */
public class PerformanceMeter implements Runnable 
{
	private int m_majorInterval;
	private int m_minorInterval;
	private int m_requestsPerMinorInterval;
	
	private int m_dataSize;
	
	private AnonProxy proxy;

	private char[] m_recvBuff;
	
	private String m_proxyHost;
	private int m_proxyPort;
	/**
	 * @param a_testDataSize
	 * @param a_MinorInterval
	 * @param a_requestsPerMinorInterval
	 */
	public PerformanceMeter(int a_testDataSize, int a_majorInterval, int a_minorInterval, 
			int a_requestsPerMinorInterval, String a_proxyHost, int a_proxyPort) 
	{
		m_majorInterval = a_majorInterval;
		m_minorInterval = a_minorInterval;
		m_requestsPerMinorInterval = a_requestsPerMinorInterval;
		m_dataSize = a_testDataSize;
		
		m_proxyHost = a_proxyHost;
		m_proxyPort = a_proxyPort;
	}

	/* (non-Javadoc)
	 * @see java.lang.Runnable#run()
	 */
	public void run() 
	{
		try
		{
			proxy = new AnonProxy(new ServerSocket(m_proxyPort, -1, InetAddress.getByName(m_proxyHost)), null, null);
			
			synchronized(proxy)
			{
				proxy.wait(1000);
			}
		
			MixCascade cascade = new MixCascade(null, null, "yuna", 6544);
		
			while(true)
			{
				performTest(cascade);
			
	    		try 
	    		{
	    			Thread.sleep(m_majorInterval);
	    		} catch (InterruptedException e) 
	    		{
	    			
	    		}
			}
		}
		
		catch(Exception ex) 
		{
			ex.printStackTrace();
		}
		

		
		
	}

	private boolean performTest(MixCascade cascade) 
	{
		if(cascade == null)
		{
			return false;
		}
		
		m_recvBuff = new char[m_dataSize];
		
		proxy.start(new SimpleMixCascadeContainer(cascade));
		
		synchronized(proxy)
		{
			try
			{
				// TODO: doesn't work
				proxy.wait(1000);
			}
			catch(InterruptedException ex)
			{
				
			}
		}
		
		if(!proxy.isConnected())
		{
			return false;
		}
		
		LogHolder.log(LogLevel.INFO, LogType.NET, "Starting performance test on cascade" + cascade.getName() + " with " + m_requestsPerMinorInterval + " requests and " + m_minorInterval + " ms interval.");
		
		for(int i = 0; i < m_requestsPerMinorInterval; i++)
		{
        	try 
        	{
        		long delay;
		        double throughput;
					
		       	Socket s = new Socket(m_proxyHost, m_proxyPort);
		       	s.setSoTimeout(5000);
		       	OutputStream stream = s.getOutputStream();
		       	stream.write("CONNECT 127.0.0.1:7777 HTTP/1.0\r\n\r\n".getBytes());
		       	BufferedReader reader = new BufferedReader(new InputStreamReader(s.getInputStream()));
		       	HTTPResponse resp;     	
		        	
		       	// read HTTP header from Anon Proxy
		       	if(((resp = parseHTTPHeader(reader)) == null) || resp.m_statusCode != 200)
		       	{
		       		LogHolder.log(LogLevel.INFO, LogType.NET, "Connection to Performance Server failed." + (resp != null ? " Status Code: " + resp.m_statusCode : ""));
		       		s.close();
		       
		       		// TODO: try it twice?
		        	break;
		        }
		        	
		        String data = "<GenerateDummyDataRequest dataLength=\"" + m_dataSize +"\"></GenerateDummyDataRequest>\r\n";
		        	
				stream.write("POST /generatedummydata\r\n".getBytes());
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
        			LogHolder.log(LogLevel.DEBUG, LogType.NET, "Performance Meter could not verify incoming package. Specified invalid Content-Length " + resp.m_length + " of " + m_dataSize + " bytes.");
        			s.close();
        			continue;
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

		        	//System.out.println(" - Recieved " + recvd + "bytes... " + bytesRead + " in total (from " + (m_dataSize) + ")");
		        }
		        
		        long responseEndTime = System.currentTimeMillis();
		        	
        		if(bytesRead != m_dataSize)
        		{
        			LogHolder.log(LogLevel.DEBUG, LogType.NET, "Performance Meter could not verify incoming package. Recieved " + bytesRead + " of " + m_dataSize + " bytes.");
        			s.close();
        			continue;
        		}
        			
        		delay = responseStartTime - transferInitiatedTime;
        		throughput = (double) m_dataSize / (responseEndTime - responseStartTime);
        			
        		System.out.println("delay:" + delay);
		        System.out.println("thru:" + throughput);		        	

			    cascade.setDelay(delay);
				cascade.setThroughput(throughput);
				
		       	s.close();
        	}
        	catch(Exception e) 
        	{
	        	LogHolder.log(LogLevel.EXCEPTION, LogType.NET, e);
	        	e.printStackTrace();
	        }
        	
    		try 
    		{
    			Thread.sleep(m_minorInterval);
    		} catch (InterruptedException e) 
    		{
    			
    		}
		}
		
    	LogHolder.log(LogLevel.INFO, LogType.NET, "Performance test for cascade " + cascade.getName() + " done. Avg Delay: " + cascade.getAverageDelay() + " ms - Avg Throughput: " + cascade.getAverageThroughput() + " kb/sec");
		
		return true;
	}
		
	public HTTPResponse parseHTTPHeader(BufferedReader reader) throws IOException, NumberFormatException
	{	
		String line;
		HTTPResponse r = new HTTPResponse();
		int i = 0;
		
		do
		{
			line = reader.readLine();
			if(i == 0 && !line.startsWith("HTTP")) return null;
			
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
	
	private class HTTPResponse
	{
		int m_statusCode = -1;
		int m_length = 0;
	}
}

