package anon.proxy;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.StringTokenizer;
import java.util.Vector;

import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;


public abstract class HTTPProxyCallback implements ProxyCallback
{

	private Hashtable connectionHTTPHeaders = null; //saves the HTTP Header
	private Hashtable headerParts = null;
	private Hashtable downstreamBytes = null;
	
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
	public final String HTTP_RANGE = "Range";
	

	public HTTPProxyCallback()
	{
		connectionHTTPHeaders = new Hashtable();
		headerParts = new Hashtable();
		downstreamBytes = new Hashtable();
	}
	
	public byte[] handleUpstreamChunk(AnonProxyRequest anonRequest, byte[] chunk, int len)
	{
		HTTPConnectionHeader connHeader;
		//This is for Download resume
		if(chunk == null)
		{
			synchronized(connectionHTTPHeaders)
			{
				connHeader = (HTTPConnectionHeader) connectionHTTPHeaders.get(anonRequest);
			}
			if(connHeader != null)
			{
				long downStreamBytes = getDownstreamBytes(anonRequest);
				if(downStreamBytes > 0)
				{
					connHeader.setRequestHeader(HTTP_RANGE, "bytes="+downStreamBytes+"-");
					return connHeader.dumpRequestHeader();
				}
				
			}
		}
		int content = (int) getLengthOfPayloadBytes(chunk, len);
		if(content != len)
		{
			extractHeaderParts(anonRequest, chunk, HEADER_TYPE_REQUEST);
			
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
		if(chunk == null)
		{
			return null;
		}
		int content = (int) getLengthOfPayloadBytes(chunk, len);
		countDownstreamBytes(anonRequest, content);
		
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
					byte[] newChunk = null;
					String response_line = connHeader.getResponseHeader(HTTP_START_LINE_KEY);
					if( response_line != null )
					{
						if(response_line.indexOf("206 Partial Content") != -1)
						{
							System.out.println("Appending partial content");
							newChunk = new byte[content];
							System.arraycopy(chunk, len-content, newChunk, 0, content);
							return newChunk;
						}
					}
					byte[] newHeaders = connHeader.dumpResponseHeader();
					newChunk = new byte[newHeaders.length+content];
					System.arraycopy(newHeaders, 0, newChunk, 0, newHeaders.length);
					System.arraycopy(chunk, len-content, newChunk, newHeaders.length, content);
					return newChunk;
				}
			}
		}
		/*String responseContentLength = getConnectionHeader(anonRequest).getResponseHeader(HTTP_CONTENT_LENGTH);
		String request_line = getConnectionHeader(anonRequest).getRequestHeader(HTTP_START_LINE_KEY);
		long dsCurrentValue = getDownstreamBytes(anonRequest);
		System.out.println("Request: '"+request_line+"': "+dsCurrentValue+" of "+
							(responseContentLength != null ? responseContentLength : "?")
							+ " transferred");*/
		
		return chunk;
	}
	
	public abstract void handleRequest(HTTPConnectionHeader connHeader);
	
	public abstract void handleResponse(HTTPConnectionHeader connHeader);
	
	final HTTPConnectionHeader getConnectionHeader(AnonProxyRequest anonRequest)
	{
		HTTPConnectionHeader ch = null;
		synchronized(connectionHTTPHeaders)
		{
			ch = (HTTPConnectionHeader) connectionHTTPHeaders.get(anonRequest);
		}
		return ch;
	}
	
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
		boolean removeDownstreamBytes = false;
		
		synchronized(connectionHTTPHeaders)
		{
			connHeader = (HTTPConnectionHeader) 
				connectionHTTPHeaders.get(anonRequest);
			
			if( (connHeader != null) && 
					connHeader.isFinished() && 
					(headerType == HEADER_TYPE_REQUEST) )
			{
				//This is an old one: replace it with a new one
				connectionHTTPHeaders.remove(anonRequest);
				connHeader = null;
				removeDownstreamBytes = true;
			}
			
			if ( connHeader == null )
			{
				connHeader = new HTTPConnectionHeader();
				connectionHTTPHeaders.put(anonRequest, connHeader);
			}
		}
		
		if(removeDownstreamBytes)
		{
			synchronized(downstreamBytes)
			{
				downstreamBytes.remove(anonRequest);
			}
		}
		
		String chunkData = null;
		
		int off_firstline= new String(chunk).indexOf(HTTP_VERSION_PREFIX);
		int off_headers_end = new String(chunk).indexOf(HTTP_HEADER_END);
		if( (off_firstline != -1)  && (off_headers_end != -1) )
		{
			//Because it is assumed that the chunk is aligned: the HTTP message starts at index 0
			parseHTTPHeader(new String(chunk, 0, off_headers_end), connHeader, headerType);
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
	private long countDownstreamBytes(AnonProxyRequest anonRequest, int contentBytes)
	{
		//long contentBytes = getLengthOfPayloadBytes(chunk, len);
		long dsCurrentValue = 0;
		synchronized(downstreamBytes)
		{
			Long dsCurrentBytes = (Long) downstreamBytes.remove(anonRequest);	
			if(dsCurrentBytes != null)
			{
				dsCurrentValue = dsCurrentBytes.longValue();
			}
			dsCurrentValue += contentBytes;
			downstreamBytes.put(anonRequest, new Long(dsCurrentValue));
		}
		return dsCurrentValue;
	}
	
	public long getDownstreamBytes(AnonProxyRequest anonRequest)
	{
		synchronized(downstreamBytes)
		{
			Long dsCurrentBytes = (Long) downstreamBytes.get(anonRequest);	
			if(dsCurrentBytes != null)
			{
				return dsCurrentBytes.longValue();
			}
		}
		return 0l;
	}
	
	protected long getLengthOfPayloadBytes(byte[] chunk, int len)
	{
		int off_firstline= new String(chunk).indexOf(HTTP_VERSION_PREFIX);
		int off_headers_end = new String(chunk).indexOf(HTTP_HEADER_END);
		if( (off_firstline != -1) )
		{
			if(off_headers_end == -1)
			{
				System.out.println("THIS SHOULD NEVER HAPPEN.");
				return 0l;
			}
			return (long) (len - (off_headers_end+HTTP_HEADER_END.length())); 
		}
		return (long) len;
	}
	
	private static void parseHTTPHeader(String headerData, HTTPConnectionHeader connHeader, int headerType)
	{
		//System.out.println(headerData+"\n");
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
		
		final public synchronized boolean isFinished() 
		{
			return finished;
		}

		final private synchronized void setFinished(boolean finished) 
		{
			this.finished = finished;
		}

		final protected synchronized void setRequestHeader(String header, String value)
		{
			setHeader(reqHeaders, reqHeaderOrder, header, value);
		}
		
		final protected synchronized void setResponseHeader(String header, String value)
		{
			setHeader(resHeaders, resHeaderOrder, header, value);
		}
		
		final protected synchronized String getRequestHeader(String header)
		{
			return getHeader(reqHeaders, header);
		}
		
		final protected synchronized String getResponseHeader(String header)
		{
			return getHeader(resHeaders, header);
		}
		
		final protected synchronized String removeRequestHeader(String header)
		{
			return removeHeader(reqHeaders, reqHeaderOrder, header);
		}
		
		final protected synchronized String removeResponseHeader(String header)
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
			headerOrder.remove(header);
			return (String) headerMap.remove(header);
		}
		
		final protected byte[] dumpRequestHeader()
		{
			return dumpHeader(reqHeaders, reqHeaderOrder);
		}
		
		final protected byte[] dumpResponseHeader()
		{
			return dumpHeader(resHeaders, resHeaderOrder);
		}
		
		final private byte[] dumpHeader(Hashtable headerMap, Vector headerOrder)
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
