package anon.proxy;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.StringTokenizer;
import java.util.Vector;

import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;


public abstract class HTTPProxyCallback implements ProxyCallback
{

	protected Hashtable connectionHTTPHeaders = null; //saves the HTTP Header
	private Hashtable headerParts = null;
	
	final static int HEADER_TYPE_REQUEST = 0;
	final static int HEADER_TYPE_RESPONSE = 1;
	
	final static String CRLF = "\r\n";
	final static String HTTP_HEADER_END = CRLF+CRLF; //end of http message headers
	final static String HTTP_HEADER_DELIM = ": ";
	final static String HTTP_START_LINE_KEY = "start-line";
	final static String HTTP_VERSION_PREFIX = "HTTP/";
	final static String[] HTTP_REQUEST_METHODS = {"OPTIONS", "GET", "HEAD", "POST", "PUT", "DELETE", "TRACE", "CONNECT"};
	
	public final String HTTP_CONTENT_LENGTH = "Content-Length";
	
	public final String HTTP_HOST = "Host";
	public final String HTTP_USER_AGENT = "User-Agent";
	public final String HTTP_ACCEPT = "Accept";
	public final String HTTP_ACCEPT_LANGUAGE = "Accept-Language";
	public final String HTTP_ACCEPT_ENCODING =  "Accept-Encoding";
	public final String HTTP_ACCEPT_CHARSET = "Accept-Charset";
	public final String HTTP_KEEP_ALIVE = "Keep-Alive";
	public final String HTTP_PROXY_CONNECTION = "Proxy-Connection";
	public final String HTTP_REFERER = "Referer";
	public final String HTTP_CACHE_CONTROL = "Cache-Control";
	public final String HTTP_PRAGMA = "Pragma";
	public final String HTTP_IE_UA_CPU = "UA-CPU";	
	

	public HTTPProxyCallback()
	{
		connectionHTTPHeaders = new Hashtable();
		headerParts = new Hashtable();
		
	}
	
	public byte[] handleUpstreamChunk(AnonProxyRequest anonRequest, byte[] chunk, int len)
	{
		
		int content = (int) getLengthOfPayloadBytes(chunk, len);
		if(content != len)
		{
			extractHeaderParts(anonRequest, chunk, HEADER_TYPE_REQUEST);
			HTTPConnectionHeader connHeader;
			synchronized(connectionHTTPHeaders)
			{
				connHeader = (HTTPConnectionHeader) connectionHTTPHeaders.get(anonRequest);
			}
			if(connHeader != null)
			{
				String request_line = connHeader.getRequestHeader(HTTP_START_LINE_KEY);
				boolean performMods = (request_line == null) ? false : !request_line.startsWith("CONNECT");
				if(performMods)
				{
					handleRequest(connHeader);
					byte[] newHeaders = connHeader.dumpRequestHeader();
					byte[] newChunk = new byte[newHeaders.length+content];
					System.arraycopy(newHeaders, 0, newChunk, 0, newHeaders.length);
					System.arraycopy(chunk, len-content, newChunk, newHeaders.length, content);
					return newChunk;
				}
			}
		}
		return chunk;
		
	}
	
	public byte[] handleDownstreamChunk(AnonProxyRequest anonRequest, byte[] chunk, int len)
	{
		int content = (int) getLengthOfPayloadBytes(chunk, len);
		if(content != len)
		{
			extractHeaderParts(anonRequest, chunk, HEADER_TYPE_RESPONSE);
			HTTPConnectionHeader connHeader;
			synchronized(connectionHTTPHeaders)
			{
				connHeader = (HTTPConnectionHeader) connectionHTTPHeaders.get(anonRequest);
			}
			if(connHeader != null)
			{
				String request_line = connHeader.getRequestHeader(HTTP_START_LINE_KEY);
				boolean performMods = (request_line == null) ? false : !request_line.startsWith("CONNECT");
				if(performMods)
				{
					handleResponse(connHeader);
					
					byte[] newHeaders = connHeader.dumpResponseHeader();
					byte[] newChunk = new byte[newHeaders.length+content];
					System.arraycopy(newHeaders, 0, newChunk, 0, newHeaders.length);
					System.arraycopy(chunk, len-content, newChunk, newHeaders.length, content);
					
					return newChunk;
				}
			}
		}
		return chunk;
	}
	
	public abstract void handleRequest(HTTPConnectionHeader connHeader);
	
	public abstract void handleResponse(HTTPConnectionHeader connHeader);
	
	/* 
	 * 	extract headers from data chunk
	 * returns false if header could not be extracted
	 */
	private boolean extractHeaderParts(AnonProxyRequest anonRequest, byte[] chunk, int headerType)
	{
		// assumes, that the complete header fits into a whole chunk and the chunk is aligned.
		//Works in almost every case, but should work with every chunk size.
		
		if(anonRequest == null)
		{
			return false;
		}
		
		HTTPConnectionHeader connHeader = null;
		
		synchronized(connectionHTTPHeaders)
		{
			connHeader = (HTTPConnectionHeader) 
				connectionHTTPHeaders.get(anonRequest);
			
			if( (connHeader != null) && connHeader.isFinished() )
			{
				//This is an old one: replace it with a new one
				connectionHTTPHeaders.remove(anonRequest);
				connHeader = null;
			}
			
			if ( connHeader == null )
			{
				connHeader = new HTTPConnectionHeader();
				connectionHTTPHeaders.put(anonRequest, connHeader);
			}
		}
		String chunkData = new String(chunk);
		
		int off_firstline= chunkData.indexOf(HTTP_VERSION_PREFIX);
		int off_headers_end = chunkData.indexOf(HTTP_HEADER_END);
		if( (off_firstline != -1)  && (off_headers_end != -1) )
		{
			//Because it is assumed that the chunk is aligned: the HTTP message starts at index 0
			parseHTTPHeader(chunkData.substring(0, off_headers_end), connHeader, headerType);
			//Also the whole chunk must fit in a single chunk: after parsing the 
			//response headers we are finished.
			if(headerType == HEADER_TYPE_RESPONSE)
			{
				connHeader.setFinished(true);
			}
			return true;
		}
		return false;
		
		/*
		String chunkPart = new String(chunk);
		
		// already started extracting the headers but not finished yet
		String currentHeaderPart = (String) headerParts.get(Thread.currentThread());
		if(currentHeaderPart != null)
		{
			
		}
		//case: we don't have started extracting the HTTP headers
		else
		{
			int http_headers_start = chunkPart.indexOf(HTTP_VERSION_PREFIX);
			if(http_headers_start == -1)
			{
				for (int i = 0; i < HTTP_REQUEST_METHODS.length; i++) 
				{
					http_headers_start = chunkPart.indexOf(HTTP_REQUEST_METHODS[i]);
					if(http_headers_start != -1)
					{
						break;
					}
				}
			}
			//In this case we receive the first chunk
			if(http_headers_start == -1)
			{
				return false;
			}
		}*/
	}
	
	protected long getLengthOfPayloadBytes(byte[] chunk, int len)
	{
		String chunkData = new String(chunk);
		
		int off_firstline= chunkData.indexOf(HTTP_VERSION_PREFIX);
		int off_headers_end = chunkData.indexOf(HTTP_HEADER_END);
		if( (off_firstline != -1) )
		{
			if(off_headers_end == -1)
			{
				return 0l;
			}
			return (long) (len - (off_headers_end+HTTP_HEADER_END.length())); 
		}
		return (long) len;
	}
	
	private static void parseHTTPHeader(String headerData, HTTPConnectionHeader connHeader, int headerType)
	{
		StringTokenizer lineTokenizer = new StringTokenizer(headerData,CRLF);
		if(lineTokenizer.countTokens() == 0)
		{
			return;
		}
		String header = null;
		String key = null;
		String value = null;
		if(headerType == HEADER_TYPE_REQUEST)
		{
			connHeader.setRequestHeader(HTTP_START_LINE_KEY, lineTokenizer.nextToken());
		}
		else if (headerType == HEADER_TYPE_RESPONSE)
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
					if(headerType == HEADER_TYPE_REQUEST)
					{
						connHeader.setRequestHeader(key, value);
					}
					else if (headerType == HEADER_TYPE_RESPONSE)
					{
						connHeader.setResponseHeader(key, value);
					}
				}
			}
		}
	}
	
	protected final class HTTPConnectionHeader
	{
		private Hashtable reqHeaders = new Hashtable();
		private Hashtable resHeaders = new Hashtable();
		
		private Vector reqHeaderOrder = new Vector();
		private Vector resHeaderOrder = new Vector();
		
		private boolean finished = false;
		
		public synchronized boolean isFinished() 
		{
			return finished;
		}

		public synchronized void setFinished(boolean finished) 
		{
			this.finished = finished;
		}

		protected synchronized void setRequestHeader(String header, String value)
		{
			setHeader(reqHeaders, reqHeaderOrder, header, value);
		}
		
		protected synchronized void setResponseHeader(String header, String value)
		{
			setHeader(resHeaders, resHeaderOrder, header, value);
		}
		
		protected synchronized String getRequestHeader(String header)
		{
			return getHeader(reqHeaders, header);
		}
		
		protected synchronized String getResponseHeader(String header)
		{
			return getHeader(resHeaders, header);
		}
		
		protected synchronized String removeRequestHeader(String header)
		{
			return removeHeader(reqHeaders, reqHeaderOrder, header);
		}
		
		protected synchronized String removeResponseHeader(String header)
		{
			return removeHeader(resHeaders, resHeaderOrder, header);
		}
		
		private void setHeader(Hashtable headerMap, Vector headerOrder, String header, String value)
		{
			if(headerMap.get(header) == null)
			{
				headerOrder.addElement(header);
			}
			headerMap.put(header, value);
		}
		
		private String getHeader(Hashtable headerMap, String header)
		{
			return (String) headerMap.get(header);
		}
		
		private String removeHeader(Hashtable headerMap, Vector headerOrder, String header)
		{
			headerOrder.removeElement(header);
			return (String) headerMap.remove(header);
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
			String value = null;
			for(Enumeration enumeration = headerOrder.elements(); enumeration.hasMoreElements(); )
			{
				header = (String) enumeration.nextElement();
				value = (String) headerMap.get(header);
				if(value != null)
				{
					allHeaders += (header.equals(HTTP_START_LINE_KEY)) ? 
							value+CRLF : header+": "+value+CRLF;
				}
			}
			allHeaders += CRLF;
			LogHolder.log(LogLevel.INFO, LogType.NET, "header dump:\n"+allHeaders);
			return allHeaders.getBytes();
		}
		
	}
}
