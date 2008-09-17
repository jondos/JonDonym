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

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.StringTokenizer;
import java.util.Vector;

import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;

/**
 * Provides functionality for parsing and storing the headers of a
 * HTTP-Connection. Invoked by the ProxyCallbackHandler framework.
 * Inheriting from this class and implementing the abstract handler functions
 * allows examining and modifying of rthe correspondende HTTP messages before it is
 * transmitted further.
 * @author Simon Pecher
 */
public class HTTPProxyCallback implements ProxyCallback
{
	private final static boolean FIRE_EVENT = true;
	private final static boolean DONT_FIRE_EVENT = !FIRE_EVENT;
	
	final static int MESSAGE_TYPE_REQUEST = 0;
	final static int MESSAGE_TYPE_RESPONSE = 1;
	
	final static String CRLF = "\r\n";
	final static String HTTP_HEADER_END = CRLF+CRLF; //end of http message headers
	final static String HTTP_HEADER_DELIM = ": ";
	final static String HTTP_START_LINE_KEY = "start-line";
	final static String HTTP_VERSION_PREFIX = "HTTP/";
	final static String[] HTTP_REQUEST_METHODS = { "GET", "POST", "CONNECT", "HEAD" , "PUT", "OPTIONS", "DELETE", "TRACE"};
	
	public final static String HTTP_CONTENT_LENGTH = "Content-Length";
	public final static String HTTP_HOST = "Host";
	public final static String HTTP_USER_AGENT = "User-Agent";
	public final static String HTTP_ACCEPT = "Accept";
	public final static String HTTP_ACCEPT_LANGUAGE = "Accept-Language";
	public final static String HTTP_ACCEPT_ENCODING =  "Accept-Encoding";
	public final static String HTTP_ACCEPT_CHARSET = "Accept-Charset";
	public final static String HTTP_KEEP_ALIVE = "Keep-Alive";
	public final static String HTTP_PROXY_CONNECTION = "Proxy-Connection";
	public final static String HTTP_REFERER = "Referer";
	public final static String HTTP_CACHE_CONTROL = "Cache-Control";
	public final static String HTTP_PRAGMA = "Pragma";
	public final static String HTTP_RANGE = "Range";
	public final static String HTTP_IE_UA_CPU = "UA-CPU";	
	
	/** Container for the Headers of a whole HTTP Connection
	 * including Request and Response. 
	 */
	private Hashtable m_connectionHTTPHeaders = null;
	/** request messages whose parsing hasn't finished yet */
	private Hashtable m_unfinishedRequests = null;
	/** response messages whose parsing hasn't finished yet */
	private Hashtable m_unfinishedResponses = null;
	
	private Hashtable m_downstreamBytes = null;
	
	private Hashtable m_upstreamBytes = null;
	
	private Vector m_httpConnectionListeners = null;
	
	private static final IHTTPHelper UPSTREAM_HELPER = new IHTTPHelper()
	{
		public byte[] dumpHeader(HTTPProxyCallback a_callback, HTTPConnectionHeader a_header, AnonProxyRequest anonRequest)
		{
			a_callback.fireRequestHeadersReceived(a_callback.getEvent(anonRequest));
			return a_header.dumpRequestHeader();
		}
	};
	private static final IHTTPHelper DOWNSTREAM_HELPER = new IHTTPHelper()
	{
		public byte[] dumpHeader(HTTPProxyCallback a_callback, HTTPConnectionHeader a_header, AnonProxyRequest anonRequest)
		{
			//a_callback.handleResponse(a_header);
			a_callback.fireResponseHeadersReceived(a_callback.getEvent(anonRequest));
			return a_header.dumpResponseHeader();
		}
	};
	
	public HTTPProxyCallback()
	{
		m_connectionHTTPHeaders = new Hashtable();
		m_unfinishedRequests = new Hashtable();
		m_unfinishedResponses = new Hashtable();
		m_downstreamBytes = new Hashtable();
		m_upstreamBytes = new Hashtable();
		m_httpConnectionListeners = new Vector();
	}
	
	public byte[] handleUpstreamChunk(AnonProxyRequest anonRequest, byte[] chunk, int len)
	{
		return handleStreamChunk(anonRequest, chunk, len, MESSAGE_TYPE_REQUEST, UPSTREAM_HELPER);
		//return chunk;
	}
	
	public byte[] handleDownstreamChunk(AnonProxyRequest anonRequest, byte[] chunk, int len)
	{
		return handleStreamChunk(anonRequest, chunk, len, MESSAGE_TYPE_RESPONSE, DOWNSTREAM_HELPER);
		//return chunk;
	}

	private byte[] handleStreamChunk(AnonProxyRequest anonRequest, byte[] chunk, int len, 
			int a_messageType, IHTTPHelper a_helper)
	{
		
		/* Does only work with a valid AnonProxy reference */
		if(anonRequest == null)
		{
			throw new NullPointerException("AnonProxyRequest must not be null!");
		}
		Hashtable unfinishedMessages =
			(a_messageType == MESSAGE_TYPE_REQUEST) ? 
				m_unfinishedRequests : m_unfinishedResponses;
		String unfinishedHeaderPart = null;
		
		Hashtable byteCounter = 
			(a_messageType == MESSAGE_TYPE_REQUEST) ? 
					m_upstreamBytes : m_downstreamBytes;
		/* check if header parsing already started but hasn't finished yet for this AnonRequest */
		synchronized(this)
		{
			unfinishedHeaderPart = (String) unfinishedMessages.get(anonRequest);
		}
		String chunkData = new String(chunk, 0, len);
		chunkData = ((unfinishedHeaderPart != null) ? unfinishedHeaderPart : "") + chunkData;
		
		int contentBytes = (int) getLengthOfPayloadBytes(chunkData);
		
		
		
		if( hasAlignedHTTPStartLine(chunkData, a_messageType) )
		{
			boolean finished = extractHeaderParts(anonRequest, chunkData, a_messageType);
			if(!finished)
			{
				/* if header parsing hasn't finished yet:
				 * chunk can be delivered with delay by returning null for now and
				 * the rest later on.
				 */
				return null;
			}
			HTTPConnectionHeader connHeader;
			synchronized (this)
			{
				connHeader = (HTTPConnectionHeader) m_connectionHTTPHeaders.get(anonRequest);
			}
						
			if(connHeader != null)
			{
				String request_line = connHeader.getRequestLine();
				
				boolean performMods = (request_line == null) ? false : !request_line.startsWith("CONNECT");
				if(performMods)
				{
					/*if(a_helper == UPSTREAM_HELPER)
					{
						String[] alreadyDownloadedBytes = null;
						synchronized (this)
						{
							alreadyDownloadedBytes = connHeader.getRequestHeader(HTTP_RANGE);
						}
						
						if(alreadyDownloadedBytes != null)
						{
							int tix = alreadyDownloadedBytes[0].indexOf("=");
							int tix2 = alreadyDownloadedBytes[0].indexOf("-");
							if( (tix != -1) && (tix2 != -1) )
							{
								String alreadyDownloadedStr = alreadyDownloadedBytes[0].substring(tix+1, tix2);
								System.out.println("Already downloaded: " +alreadyDownloadedStr);
								long downloadedBytes = Long.parseLong(alreadyDownloadedStr);
								m_downstreamBytes.put(anonRequest, new Long(alreadyDownloadedStr));
							}
						}
					}*/
					
					byte[] newHeaders = a_helper.dumpHeader(this, connHeader, anonRequest);
					countContentBytes(anonRequest, contentBytes, byteCounter, FIRE_EVENT);
					byte[] newChunk = new byte[newHeaders.length+contentBytes];
					System.arraycopy(newHeaders, 0, newChunk, 0, newHeaders.length);
					System.arraycopy(chunk, len-contentBytes, newChunk, newHeaders.length, contentBytes);
					return newChunk;
				}				
			}
		}
		countContentBytes(anonRequest, contentBytes, byteCounter, FIRE_EVENT);
		return chunk;		
	}
	
	
	private interface IHTTPHelper
	{
		byte[] dumpHeader(HTTPProxyCallback a_callback, HTTPConnectionHeader a_header, AnonProxyRequest anonRequest);
	}
	
	private synchronized long countContentBytes(AnonProxyRequest anonRequest, int contentBytes, Hashtable contentBytesCount, boolean fire)
	{
		long dsCurrentValue = 0;
		
		Long dsCurrentBytes = (Long) contentBytesCount.remove(anonRequest);	
		if(dsCurrentBytes != null)
		{
			dsCurrentValue = dsCurrentBytes.longValue();
		}
		dsCurrentValue += contentBytes;
		contentBytesCount.put(anonRequest, new Long(dsCurrentValue));
		
		/* use occasion to fire event, when we have a brand new byteCount value */
		if(fire == FIRE_EVENT)
		{
			HTTPConnectionEvent event = new HTTPConnectionEvent();
			event.setAnonRequest(anonRequest);
			event.setConnectionHeader((HTTPConnectionHeader)m_connectionHTTPHeaders.get(anonRequest));
			
			if(contentBytesCount == m_downstreamBytes)
			{
				event.setUpStreamContentBytes(getUpStreamContentBytes(anonRequest));
				event.setDownStreamContentBytes(dsCurrentValue);
				fireDownstreamContentBytesReceived(event);
			}
			else if(contentBytesCount == m_upstreamBytes)
			{
				event.setDownStreamContentBytes(getDownStreamContentBytes(anonRequest));
				event.setUpStreamContentBytes(dsCurrentValue);
				fireUpstreamContentBytesReceived(event);
			}
		}
		return dsCurrentValue;
	}
	
	public synchronized long getUpStreamContentBytes(AnonProxyRequest anonRequest)
	{
		return getContentBytes(anonRequest, m_upstreamBytes);
	}
	
	public synchronized long getDownStreamContentBytes(AnonProxyRequest anonRequest)
	{
		return getContentBytes(anonRequest, m_downstreamBytes);
	}
	
	private long getContentBytes(AnonProxyRequest anonRequest, Hashtable contentByteCount)
	{
		if(contentByteCount == null)
		{
			throw new NullPointerException("Bug: No count table specified for getContentBytes");
		}
		Long contentBytes = (Long) contentByteCount.get(anonRequest);
		return (contentBytes == null) ? 0l : contentBytes.longValue();
	}
	
	private synchronized HTTPConnectionEvent getEvent(AnonProxyRequest anonRequest)
	{
		long upStreamBytes = getUpStreamContentBytes(anonRequest);
		long downStreamBytes = getDownStreamContentBytes(anonRequest);
		HTTPConnectionHeader connHeader = (HTTPConnectionHeader) m_connectionHTTPHeaders.get(anonRequest);
		return new HTTPConnectionEvent(connHeader, upStreamBytes, downStreamBytes, anonRequest);
	}
	
	
	
	/* 
	 * 	extract headers from data chunk
	 * returns false if header could not be extracted
	 */
	private synchronized boolean extractHeaderParts(AnonProxyRequest anonRequest, String chunkData, int messageType)
	{
		// assumes, that the chunk is aligned.
		//Works in almost every case.
		
		if(anonRequest == null)
		{
			throw new NullPointerException("AnonProxyRequest must not be null!");
		}
		HTTPConnectionHeader connHeader = null;
		
		//synchronized(m_connectionHTTPHeaders)
		//{
			connHeader = (HTTPConnectionHeader) 
				m_connectionHTTPHeaders.get(anonRequest);
			
			if( (connHeader != null) )
			{
				/* old http messages already delivered by this AnonProxyRequest-Thread can be removed */ 
				if((messageType == MESSAGE_TYPE_REQUEST) && connHeader.isRequestFinished())
				{
					connHeader.clearRequest();
					m_upstreamBytes.remove(anonRequest);
				}
				else if ((messageType == MESSAGE_TYPE_RESPONSE) && connHeader.isResponseFinished())
				{
					connHeader.clearResponse();
					m_downstreamBytes.remove(anonRequest);
				}
			}
			
			if ( connHeader == null )
			{
				connHeader = new HTTPConnectionHeader();
				m_connectionHTTPHeaders.put(anonRequest, connHeader);
			}
		//}
		
		if(hasAlignedHTTPStartLine(chunkData, messageType))
		{
			Hashtable unfinishedMessages = 
				(messageType == MESSAGE_TYPE_REQUEST) ? 
					m_unfinishedRequests : m_unfinishedResponses;
			int off_headers_end = chunkData.indexOf(HTTP_HEADER_END);

			if((off_headers_end != -1))
			{
				//Because it is assumed that the chunk is aligned: the HTTP message starts at index 0
				parseHTTPHeader(chunkData.substring(0, off_headers_end), connHeader, messageType);
				if(messageType == MESSAGE_TYPE_REQUEST)
				{
					connHeader.setRequestFinished(true);
				}
				else if (messageType == MESSAGE_TYPE_RESPONSE)
				{
					connHeader.setResponseFinished(true);
				}
				unfinishedMessages.remove(anonRequest);
				return true;
			}
			else 
			{
				unfinishedMessages.put(anonRequest, chunkData);
				return false;
			}
		}
		return false;
	}
	
	private boolean hasAlignedHTTPStartLine(String chunkData, int messageType)
	{
		return (messageType == MESSAGE_TYPE_REQUEST) ? isRequest(chunkData) : isResponse(chunkData);
	}
	
	private boolean isRequest(String chunkData)
	{
		for (int i = 0; i < HTTP_REQUEST_METHODS.length; i++) {
			if( chunkData.startsWith(HTTP_REQUEST_METHODS[i]) )
			{
				return true;
			}
		}
		return false;
	}
	
	private boolean isResponse(String chunkData)
	{
		return chunkData.startsWith(HTTP_VERSION_PREFIX);
	}
	
	protected long getLengthOfPayloadBytes(String chunkData)
	{	
		int off_firstline= chunkData.indexOf(HTTP_VERSION_PREFIX);
		int off_headers_end = chunkData.indexOf(HTTP_HEADER_END);
		if( (off_firstline != -1) )
		{
			if(off_headers_end == -1)
			{
				return 0l;
			}
			return (long) (chunkData.length() - (off_headers_end+HTTP_HEADER_END.length())); 
		}
		return (long) chunkData.length();
	}
	
	private synchronized void parseHTTPHeader(String headerData, HTTPConnectionHeader connHeader, int headerType)
	{
		StringTokenizer lineTokenizer = new StringTokenizer(headerData,CRLF);
		if(lineTokenizer.countTokens() == 0)
		{
			return;
		}
		String header = null;
		String key = null;
		String value = null;
		if(headerType == MESSAGE_TYPE_REQUEST)
		{
			connHeader.setRequestHeader(HTTP_START_LINE_KEY, lineTokenizer.nextToken());
		}
		else if (headerType == MESSAGE_TYPE_RESPONSE)
		{
			connHeader.setResponseHeader(HTTP_START_LINE_KEY, lineTokenizer.nextToken());
		}
		while(lineTokenizer.hasMoreTokens())
		{
			header = lineTokenizer.nextToken();
			int delim = header.indexOf(HTTP_HEADER_DELIM);
			if(delim != -1)
			{
				key = header.substring(0, delim).trim();
				if(delim+1 < header.length())
				{
					value = header.substring(delim+1).trim();
				}
				if( (key != null) && (value != null) )
				{
					if(headerType == MESSAGE_TYPE_REQUEST)
					{
						connHeader.setRequestHeader(key, value);
					}
					else if (headerType == MESSAGE_TYPE_RESPONSE)
					{
						connHeader.setResponseHeader(key, value);
					}
				}
			}
		}
	}
	
	public synchronized void addHTTPConnectionListener(HTTPConnectionListener listener)
	{
		if(! m_httpConnectionListeners.contains(listener) )
		{
			m_httpConnectionListeners.addElement(listener);
		}
	}
	
	public synchronized void removeHTTPConnectionListener(HTTPConnectionListener listener)
	{
		m_httpConnectionListeners.removeElement(listener);
	}
	
	public synchronized void removeAlllHTTPConnectionListeners()
	{
		m_httpConnectionListeners.removeAllElements();
	}
	
	public synchronized void fireRequestHeadersReceived(HTTPConnectionEvent event)
	{
		for(Enumeration enumeration = m_httpConnectionListeners.elements(); enumeration.hasMoreElements();)
		{
			HTTPConnectionListener listener = (HTTPConnectionListener) enumeration.nextElement();
			if(listener != null)
			{
				listener.requestHeadersReceived(event);
			}
		}
	}
	
	public synchronized void fireResponseHeadersReceived(HTTPConnectionEvent event)
	{
		for(Enumeration enumeration = m_httpConnectionListeners.elements(); enumeration.hasMoreElements();)
		{
			HTTPConnectionListener listener = (HTTPConnectionListener) enumeration.nextElement();
			if(listener != null)
			{
				listener.responseHeadersReceived(event);
			}
		}
	}
	
	public synchronized void fireDownstreamContentBytesReceived(HTTPConnectionEvent event)
	{
		for(Enumeration enumeration = m_httpConnectionListeners.elements(); enumeration.hasMoreElements();)
		{
			HTTPConnectionListener listener = (HTTPConnectionListener) enumeration.nextElement();
			if(listener != null)
			{
				listener.downstreamContentBytesReceived(event);
			}
		}
	}
	
	public synchronized void fireUpstreamContentBytesReceived(HTTPConnectionEvent event)
	{
		for(Enumeration enumeration = m_httpConnectionListeners.elements(); enumeration.hasMoreElements();)
		{
			HTTPConnectionListener listener = (HTTPConnectionListener) enumeration.nextElement();
			if(listener != null)
			{
				listener.upstreamContentBytesReceived(event);
			}
		}
	}
	
	protected final class HTTPConnectionHeader
	{
		private Hashtable reqHeaders = new Hashtable();
		private Hashtable resHeaders = new Hashtable();
		
		private Vector reqHeaderOrder = new Vector();
		private Vector resHeaderOrder = new Vector();
		
		private boolean requestFinished = false;
		private boolean responseFinished = false;
		
		public synchronized boolean isResponseFinished() 
		{
			return responseFinished;
		}

		public synchronized void setResponseFinished(boolean responseFinished) 
		{
			this.responseFinished = responseFinished;
		}

		public synchronized boolean isRequestFinished() 
		{
			return requestFinished;
		}

		public synchronized void setRequestFinished(boolean finished) 
		{
			this.requestFinished = finished;
		}

		/*
		 * methods for checking or modifying HTTP message headers
		 */
		protected synchronized void setRequestHeader(String header, String value)
		{
			setHeader(reqHeaders, reqHeaderOrder, header, value);
		}
		
		protected synchronized void setResponseHeader(String header, String value)
		{
			setHeader(resHeaders, resHeaderOrder, header, value);
		}
		
		protected synchronized void replaceRequestHeader(String header, String value)
		{
			replaceHeader(reqHeaders, reqHeaderOrder, header, value);
		}
		
		protected synchronized void replaceResponseHeader(String header, String value)
		{
			replaceHeader(resHeaders, resHeaderOrder, header, value);
		}
		
		protected synchronized String getRequestLine()
		{
			return getStartLine(reqHeaders);
		}
		
		protected synchronized String getResponseLine()
		{
			return getStartLine(resHeaders);
		}
		
		protected synchronized String[] getRequestHeader(String header)
		{
			return getHeader(reqHeaders, header);
		}
		
		protected synchronized String[] getResponseHeader(String header)
		{
			return getHeader(resHeaders, header);
		}
		
		protected synchronized String[] removeRequestHeader(String header)
		{
			return removeHeader(reqHeaders, reqHeaderOrder, header);
		}
		
		protected synchronized String[] removeResponseHeader(String header)
		{
			return removeHeader(resHeaders, resHeaderOrder, header);
		}
		
		protected synchronized void clearRequest()
		{
			clearHeader(reqHeaders, reqHeaderOrder);
		}
		
		protected synchronized void clearResponse()
		{
			clearHeader(resHeaders, resHeaderOrder);
		}
		
		/* private util-fucntion area. All of these functions are not thread safe and are only to be accessed  
		 * synchronized by the actual ConnectionHeader object
		 */
		private void setHeader(Hashtable headerMap, Vector headerOrder, String header, String value)
		{
			Vector valueContainer = (Vector) headerMap.get(header.toLowerCase());
			if(valueContainer == null)
			{
				/* it's possible that a header was removed but is still in the order list. 
				 * because when a header is removed, it is not deleted from there.
				 * this is convenient for replacing headers but can cause side effects.
				 * (Removing and the setting a header will set the header in the place where 
				 * it first was). 
				 */
				boolean addToOrder = true;
				for(Enumeration enumeration = headerOrder.elements(); enumeration.hasMoreElements();)
				{
					String aktheader = (String) enumeration.nextElement();
					if(aktheader.equalsIgnoreCase(header))
					{
						addToOrder = false;
					}
				}
				if(addToOrder)
				{
					headerOrder.addElement(header);
				}
				valueContainer = new Vector();
			}
			valueContainer.addElement(value);
			headerMap.put(header.toLowerCase(), valueContainer);
		}
		private void replaceHeader(Hashtable headerMap, Vector headerOrder, String header, String value)
		{
			removeHeader(headerMap, headerOrder, header);
			setHeader(headerMap, headerOrder, header, value);
		}
		
		private String[] getHeader(Hashtable headerMap, String header)
		{
			Vector valueContainer = (Vector) headerMap.get(header.toLowerCase());
			return valuesToArray(valueContainer);
		}
		
		private String[] removeHeader(Hashtable headerMap, Vector headerOrder, String header)
		{
			/*for(Enumeration enumeration = headerOrder.elements(); enumeration.hasMoreElements();)
			{
				String aktheader = (String) enumeration.nextElement();
				if(aktheader.equalsIgnoreCase(header))
				{
					headerOrder.remove(aktheader);
				}
			}*/
			/* header is not removed from the order list: beware of side-effects */
			Vector valueContainer = (Vector) headerMap.remove(header.toLowerCase());
			return valuesToArray(valueContainer);
		}
		
		private void clearHeader(Hashtable headerMap, Vector headerOrder)
		{
			headerMap.clear();
			headerOrder.removeAllElements();
		}
		
		private String getStartLine(Hashtable headerMap)
		{
			Vector valueContainer = (Vector) headerMap.get(HTTP_START_LINE_KEY.toLowerCase());
			String[] startlineRet = valuesToArray(valueContainer);
			if (startlineRet == null)
			{
				LogHolder.log(LogLevel.ERR, LogType.NET, "Invalid request because it contains no startline");
				new Exception("DFg").printStackTrace();
				return null;
			}
			if(startlineRet.length > 1)
			{
				String errOutput = "";
				for (int i = 0; i < startlineRet.length; i++) 
				{
					errOutput+= startlineRet[i]+"\n";
				}
				LogHolder.log(LogLevel.ERR, LogType.NET, 
						"This HTTP message seems to be invalid, because it has multiple start lines:\n"
					+errOutput);
			}
			return startlineRet[0];
		}
		
		private String[] valuesToArray(Vector valueContainer)
		{
			if(valueContainer == null)
			{
				return null;
			}
			int valueCount = valueContainer.size();
			if(valueCount == 0)
			{
				return null;
			}
			String[] values = new String[valueCount];
			Enumeration enumeration = valueContainer.elements();
			for(int i = 0; enumeration.hasMoreElements(); i++)
			{
				values[i] = (String) enumeration.nextElement();
			}
			return values;
		}
		
		private byte[] dumpRequestHeader()
		{
			return dumpHeader(reqHeaders, reqHeaderOrder);
		}
		
		private byte[] dumpResponseHeader()
		{
			return dumpHeader(resHeaders, resHeaderOrder);
		}
		
		private byte[] dumpHeader(Hashtable headerMap, Vector headerOrder)
		{
			String allHeaders = "";
			String header = null;

			for(Enumeration enumeration = headerOrder.elements(); enumeration.hasMoreElements(); )
			{
				header = (String) enumeration.nextElement();
				if(header.equalsIgnoreCase(HTTP_START_LINE_KEY))
				{
					if(!allHeaders.equals(""))
					{
						LogHolder.log(LogLevel.ERR, LogType.NET, "HTTP startline set after Message-Header. " +
								"This is a Bug. please report this.");
						throw new  IllegalStateException("HTTP startline set after Message-Header. " +
								"This is a Bug. please report this.");
					}
					allHeaders += getStartLine(headerMap)+CRLF;
				}
				else
				{
					String[] values = getHeader(headerMap, header);
					if(values != null)
					{
						for (int i = 0; i < values.length; i++) 
						{
							allHeaders += header+": "+values[i]+CRLF;
						}
					}
				}
			}
			allHeaders += CRLF;
			LogHolder.log(LogLevel.INFO, LogType.NET, Thread.currentThread().getName()+": header dump:\n"+allHeaders);
			return allHeaders.getBytes();
		}
		
	}
}
