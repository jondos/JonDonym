package anon.proxy;

import java.util.Hashtable;
import java.util.StringTokenizer;


public abstract class HTTPProxyCallback implements ProxyCallback
{

	private Hashtable connectionHTTPHeaders = null; //saves the HTTP Header
	private Hashtable headerParts = null;
	
	final static String CRLF = "\r\n";
	final static String HTTP_HEADER_END = CRLF+CRLF; //RFC 2616: end of http message headers
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
	
	public final String[] HTTP_REQUEST_HEADER_ORDER = 
	{
			HTTP_START_LINE_KEY,			
			HTTP_HOST,
			HTTP_USER_AGENT,
			HTTP_ACCEPT,
			HTTP_ACCEPT_LANGUAGE,
			HTTP_ACCEPT_ENCODING,
			HTTP_ACCEPT_CHARSET,
			HTTP_KEEP_ALIVE,
			HTTP_PROXY_CONNECTION,
			HTTP_REFERER,
			HTTP_PRAGMA,
			HTTP_CACHE_CONTROL
	};
	
	public HTTPProxyCallback()
	{
		connectionHTTPHeaders = new Hashtable();
		headerParts = new Hashtable();
	}
	
	public byte[] handleUpstreamChunk(byte[] chunk, int len)
	{
		int content = (int) getLengthOfPayloadBytes(chunk, len);
		if(content != len)
		{
			extractHeaderParts(chunk);
			String request_line = getConnectionHeader(Thread.currentThread(), HTTP_START_LINE_KEY);
			
			boolean performMods = (request_line == null) ? false : !request_line.startsWith("CONNECT");
			if(performMods)
			{
				modifyRequestHeaders();
				byte[] newHeaders = dumpHeader();
				/*byte[] dHeaders = dumpHeader();
				byte[] newHeaders = new byte[len];
				System.arraycopy(chunk, 0, newHeaders, 0, len);
				for (int i = 0; i < newHeaders.length; i++) {
					if(newHeaders[i] != dHeaders[i])
					{
						System.out.println(" , index "+i+" nh char "+((int)newHeaders[i])+"("+(char)newHeaders[i]+")dh char "+((int)dHeaders[i])+
								"("+(char)dHeaders[i]+")");
					}
				}*/
				//System.out.println("dHeaders: "+new String(dHeaders));
				//System.out.println("new Headers: "+(new String(newHeaders)));
				byte[] newChunk = new byte[newHeaders.length+content];
				System.arraycopy(newHeaders, 0, newChunk, 0, newHeaders.length);
				System.arraycopy(chunk, len-content, newChunk, newHeaders.length, content);
				/*System.out.println("Chunk: "+new String(chunk));
				System.out.println("len: "+len+", content: "+content+"newHeadersLength: "+newHeaders.length);*/
				//System.out.println("new Chunk: "+(new String(newChunk)));
				return newChunk;
			}
			else
			{
				return chunk;
			}
			//return newHeaders;
		}
		
		byte[] newChunk = new byte[len];
		System.arraycopy(chunk, 0, newChunk, 0, len);
		return chunk;
		
	}
	
	public byte[] handleDownstreamChunk(byte[] chunk, int len)
	{
		extractHeaderParts(chunk);
		return chunk;
	}
	
	protected final synchronized String getConnectionHeader(Thread connectionThread, String headerKey)
	{
		Hashtable headers = (Hashtable) connectionHTTPHeaders.get(connectionThread);
		if (headers == null)
		{
			return null;	
		}
		return (String) headers.get(headerKey); 
	}

	protected final synchronized void setConnectionHeader(Thread connectionThread, String headerKey, String newValue)
	{
		if(headerKey != null)
		{
			Hashtable headers = (Hashtable) connectionHTTPHeaders.get(connectionThread);
			if (headers != null)
			{
				headers.put(headerKey, newValue);
			}
		}
	}
	
	protected final synchronized String removeConnectionHeader(Thread connectionThread, String headerKey)
	{
		if(headerKey != null)
		{
			Hashtable headers = (Hashtable) connectionHTTPHeaders.get(connectionThread);
			if (headers != null)
			{
				return (String) headers.remove(headerKey);
			}
		}
		return null;
	}
	
	/**
	 * 
	 * @param allHeaders all pevious headers terminated with CRLF
	 * @param newHeader type of new header to append
	 * @param newHaederValue value of new hedaer t6o appen
	 * @return String with appended header terminated with CRLF
	 */
	protected String appendHeader(String allHeaders, String newHeader, String newHeaderValue)
	{
		int allHeadersLength = allHeaders.length();
		if(allHeadersLength == 1)
		{
			allHeaders = "";
		}
		if (allHeaders != "")
		{
			if (!allHeaders.substring((allHeadersLength-2),(allHeadersLength)).equals(CRLF))
			{
				System.out.println("end|"+allHeaders.substring((allHeadersLength-2),(allHeadersLength-1))+"|_");
				allHeaders += CRLF;
			}
		}
		if(newHeader != null && newHeaderValue != null)
		{
			return allHeaders += newHeader.equals(HTTP_START_LINE_KEY) ? 
									(newHeaderValue +CRLF) : 
									(newHeader +": "+newHeaderValue + CRLF);
		}
		else
		{
			return allHeaders;
		}
	}
	
	protected String appendStoredHeader(String allHeaders, String newHeader)
	{
		if(newHeader != null)
		{
			return appendHeader(allHeaders, newHeader, getConnectionHeader(Thread.currentThread(), newHeader));
		}
		else
		{
			return allHeaders;
		}
	}
	
	protected byte[] dumpHeader()
	{
		String allHeaders = "";
		for (int i = 0; i < HTTP_REQUEST_HEADER_ORDER.length; i++) 
		{
			allHeaders = appendStoredHeader(allHeaders, HTTP_REQUEST_HEADER_ORDER[i]);
		}
		allHeaders += allHeaders.equals("") ? "" : CRLF;
		return allHeaders.getBytes();
	}
	
	public abstract void modifyRequestHeaders();
	
	/* 
	 * 	extract headers from data chunk
	 * returns false if header
	 */
	private boolean extractHeaderParts(byte[] chunk)
	{
		// first a quick and sloppy implementation which assumes, that
		//the complete header fits into a whole chunk and the chunk is aligned.
		//Works in almost every case, but should work with every chunk size
		//and without alignment.
		Thread proxyThread = Thread.currentThread();
		synchronized(connectionHTTPHeaders)
		{
			if (connectionHTTPHeaders.get(proxyThread) != null)
			{
				connectionHTTPHeaders.remove(proxyThread);
				//return true;
			}
		}
		String chunkData = null;
		
		int off_firstline= new String(chunk).indexOf(HTTP_VERSION_PREFIX);
		int off_headers_end = new String(chunk).indexOf(HTTP_HEADER_END);
		if( (off_firstline != -1)  && (off_headers_end != -1) )
		{
			//Because it is assumed that the chunk is aligned: the HTTP message starts at index 0
			Hashtable headers = getHTTPHeaders(new String(chunk, 0, off_headers_end));
			if (headers != null)
			{
				synchronized(connectionHTTPHeaders)
				{
					connectionHTTPHeaders.put(proxyThread, headers);
					return true;
				}
			}
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
		int off_firstline= new String(chunk).indexOf(HTTP_VERSION_PREFIX);
		int off_headers_end = new String(chunk).indexOf(HTTP_HEADER_END);
		if( (off_firstline != -1) )
		{
			if(off_firstline == -1)
			{
				return 0l;
			}
			return (long) (len - (off_headers_end+HTTP_HEADER_END.length())); 
		}
		return (long) len;
	}
	
	private static Hashtable getHTTPHeaders(String headerData)
	{
		//System.out.println(headerData+"\n");
		Hashtable headerMap = null;
		StringTokenizer lineTokenizer = new StringTokenizer(headerData,CRLF);
		if(lineTokenizer.countTokens() > 0)
		{
			headerMap = new Hashtable();
		}
		else
		{
			return null;
		}
		String header = null;
		String key = null;
		String value = null;
		headerMap.put(HTTP_START_LINE_KEY, lineTokenizer.nextToken());
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
					headerMap.put(key, value);
				}
			}
		}
		return headerMap;
	}
}
