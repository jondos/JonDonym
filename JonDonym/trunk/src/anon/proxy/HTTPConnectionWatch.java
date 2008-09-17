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
package anon.proxy;

import java.util.Hashtable;

import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;

import anon.proxy.HTTPProxyCallback.HTTPConnectionHeader;

/**
 * still experimental code
 * @author Simon Pecher
 *
 */
public class HTTPConnectionWatch implements HTTPConnectionListener {

	Hashtable storedConnections = new Hashtable();
	
	public synchronized void downstreamContentBytesReceived(HTTPConnectionEvent event) 
	{
		if(event.getAnonRequest() == null)
		{
			throw new NullPointerException("HTTPConnectionEvent must belong to an AnonProxyRequest.");
		}
		
		ConnectionTableEntry entry = (ConnectionTableEntry) storedConnections.get(event.getAnonRequest());
		if(entry != null)
		{
			entry.setTransferedBytes(event.getDownStreamContentBytes());
			if (entry.getCountedBytes() == entry.getTotalBytes())
			{
				LogHolder.log(LogLevel.DEBUG, LogType.NET, "transmission for connection " + 
						entry.getConnectionHeader().getRequestLine()+ "successfully finished");
				storedConnections.remove(event.getAnonRequest());
			}
			//if(entry.getOffset()+entry.)
		}
		/*String infoString = "";
		infoString += event.getDownStreamContentBytes();
		if(event.getConnectionHeader() != null)
		{
			String[] contentLength = 
				event.getConnectionHeader().getResponseHeader(HTTPProxyCallback.HTTP_CONTENT_LENGTH); 
			if(contentLength != null)
			{
				
				infoString += 
					(contentLength.length > 0) ? " of "+contentLength[0] : "";
				long dsBytes = Long.parseLong(contentLength[0]);
				if(dsBytes == event.getDownStreamContentBytes())
				{
					System.err.println(Thread.currentThread().getName()+": completed transfer of "+dsBytes+" bytes for "+event.getConnectionHeader().getRequestLine());
				}
			}
			infoString += " bytes downloaded for "+event.getConnectionHeader().getRequestLine() +", "+ Thread.currentThread().getName();
		}
		System.out.println(infoString);*/
	}

	public void requestHeadersReceived(HTTPConnectionEvent event) 
	{
	}

	public synchronized void responseHeadersReceived(HTTPConnectionEvent event) 
	{
		if(event.getAnonRequest() == null)
		{
			throw new NullPointerException("HTTPConnectionEvent must belong to an AnonProxyRequest.");
		}
		
		HTTPConnectionHeader connectionHeader = event.getConnectionHeader();
		
		if(connectionHeader == null)
		{
			return;
		}
		String[] contentLength = 
			connectionHeader.getResponseHeader(HTTPProxyCallback.HTTP_CONTENT_LENGTH); 
			
		if(contentLength == null)
		{
			return;
		}
		if(contentLength.length <= 0)
		{
			return;
		}
		
		long totalBytes = 0l;
		long offset = 0l;
		
		/* get Content-Length */
		try 
		{
			totalBytes = Long.parseLong(contentLength[0]);
		}
		catch(NumberFormatException nfe)
		{
			LogHolder.log(LogLevel.INFO, LogType.NET, "Received bad Content-Length Header: "+contentLength[0]+
					"for request "+connectionHeader.getRequestLine());
			return;
		}
		
		String[] offsetVal = connectionHeader.getRequestHeader(HTTPProxyCallback.HTTP_RANGE);
		if(offsetVal != null)
		{
			if(offsetVal.length > 0)
			{
				int tix = offsetVal[0].indexOf("=");
				int tix2 = offsetVal[0].indexOf("-");
				if( (tix != -1) && (tix2 != -1) )
				{
					String offsetString = offsetVal[0].substring(tix+1, tix2);
					try
					{
						offset = Long.parseLong(offsetString);
						//System.out.println("Request with offset "+offset+" received");
					}
					catch(NumberFormatException nfe)
					{
						//just ignore if range bytes are invalid.
					}
				}
			}
		}
		if(totalBytes > 0)
		{
			ConnectionTableEntry entry = new ConnectionTableEntry(connectionHeader, offset, totalBytes, 0l);
			storedConnections.put(event.getAnonRequest(), entry);
		}
		else
		{
			LogHolder.log(LogLevel.DEBUG, LogType.NET, 
					"Connection "+connectionHeader.getRequestLine()+" cannot be watched.");
		}
	}
	
	//private void checkEvent
	
	public void upstreamContentBytesReceived(HTTPConnectionEvent event) 
	{
	}

	private class ConnectionTableEntry
	{
		/** if connection is already a resumed connection: offset contains the Value of the Range-Header */
		long offset;
		/** downstreamBytes transferred during this session */
		long transferedBytes;
		/** downstreambytes to be transferred, value of the Content-Length-Header */ 
		long totalBytes;
		/** the corresponding request and response headers of this connection */
		HTTPConnectionHeader connectionHeader;
		
		public ConnectionTableEntry(HTTPConnectionHeader connectionHeader,
				long offset, long totalBytes, long transferedBytes) 
		{

			this.connectionHeader = connectionHeader;
			this.offset = offset;
			this.totalBytes = totalBytes;
			this.transferedBytes = transferedBytes;
		}

		public ConnectionTableEntry() 
		{
		}

		private long getOffset()
		{
			return offset;
		}
		
		private void setOffset(long offset)
		{
			this.offset = offset;
		}
		
		private long getTransferedBytes()
		{
			return transferedBytes;
		}
		
		private void setTransferedBytes(long transferedBytes)
		{
			this.transferedBytes = transferedBytes;
		}
		
		private long getTotalBytes()
		{
			return totalBytes;
		}
		
		private void setTotalBytes(long totalBytes) 
		{
			this.totalBytes = totalBytes;
		}
		
		private long getCountedBytes()
		{
			return offset+transferedBytes;
		}
		
		private HTTPConnectionHeader getConnectionHeader()
		{
			return connectionHeader;
		}
		
		private void setConnectionHeader(HTTPConnectionHeader connectionHeader)
		{
			this.connectionHeader = connectionHeader;
		}
		
	}
}
