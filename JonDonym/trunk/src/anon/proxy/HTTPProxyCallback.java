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

import anon.infoservice.HttpResponseStructure;
import anon.util.JAPMessages;

import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;

/**
 * Provides functionality for parsing and storing the headers of a
 * HTTP-Connection. Invoked by the ProxyCallbackHandler framework.
 * HTTPConnectionListeners can be registered to examine and modify 
 * the corresponding HTTP messages before they are
 * transmitted further. Also Listeners can be notified about how much
 * payload was transmitted via the corresponding HTTPConnection. 
 * @author Simon Pecher
 */
public class HTTPProxyCallback implements ProxyCallback
{
	private final static boolean FIRE_EVENT = true;
	
	final static int MESSAGE_TYPE_REQUEST = 0;
	final static int MESSAGE_TYPE_RESPONSE = 1;
	
	final static String CRLF = "\r\n";
	final static String HTTP_HEADER_END = CRLF+CRLF; //end of http message headers
	final static byte[] HTTP_HEADER_END_BYTES = {13, 10, 13, 10};
	
	final static String HTTP_HEADER_DELIM = ": ";
	final static String HTTP_START_LINE_KEY = "start-line";
	final static String HTTP_VERSION_PREFIX = "HTTP/";
	final static byte[] HTTP_VERSION_PREFIX_BYTES = HTTP_VERSION_PREFIX.getBytes();
	
	final static String[] HTTP_REQUEST_METHODS = { "GET", "POST", "CONNECT", "HEAD" , "PUT", "OPTIONS", "DELETE", "TRACE"};
	final static byte[][] HTTP_REQUEST_METHODS_BYTES;
	
	final static String MSG_INVALID_LINETERM_REQUEST ="httpFilter.invalidlineterm.request";
	final static String MSG_INVALID_LINETERM_RESPONSE ="httpFilter.invalidlineterm.response";
	
	static 
	{
		HTTP_REQUEST_METHODS_BYTES = new byte[HTTP_REQUEST_METHODS.length][];
		for (int i = 0; i < HTTP_REQUEST_METHODS.length; i++) 
		{
			byte [] currentMethodBytes = HTTP_REQUEST_METHODS[i].getBytes();
			HTTP_REQUEST_METHODS_BYTES[i] = currentMethodBytes;
		}
	}
	
	public final static String HTTP_CONTENT_LENGTH = "Content-Length";
	public final static String HTTP_CONTENT_ENCODING = "Content-Encoding";
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
			return a_header.dumpRequestHeaders();
		}
	};
	private static final IHTTPHelper DOWNSTREAM_HELPER = new IHTTPHelper()
	{
		public byte[] dumpHeader(HTTPProxyCallback a_callback, HTTPConnectionHeader a_header, AnonProxyRequest anonRequest)
		{
			//a_callback.handleResponse(a_header);
			a_callback.fireResponseHeadersReceived(a_callback.getEvent(anonRequest));
			return a_header.dumpResponseHeaders();
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
	
	public int handleUpstreamChunk(AnonProxyRequest anonRequest, ProxyCallbackBuffer buffer)  throws ProxyCallbackNotProcessableException
	{
		return handleStreamChunk(anonRequest, buffer, MESSAGE_TYPE_REQUEST, UPSTREAM_HELPER);
		//return chunk;
	}
	
	public int handleDownstreamChunk(AnonProxyRequest anonRequest, ProxyCallbackBuffer buffer) throws ProxyCallbackNotProcessableException
	{
		/* set noResponseExptected when a response to a request was already 
		 * parsed and no further requests were sent so we don't expect
		 * to receive a response either. 
		 * 
		 * This flag should avoid confusion when received byte-content contains the
		 * same aligned byte sequence that would identify it as a response.
		 */
		boolean noResponseExpected = false;
		synchronized(this)
		{
			HTTPConnectionHeader connHeader =
				(HTTPConnectionHeader) m_connectionHTTPHeaders.get(anonRequest);
			noResponseExpected = (connHeader == null) ? true : !connHeader.isResponseExpected();
		}
		return noResponseExpected ? STATUS_PROCESSABLE : handleStreamChunk(anonRequest,  buffer, MESSAGE_TYPE_RESPONSE, DOWNSTREAM_HELPER);
					
		//return chunk;
	}

	private int handleStreamChunk(AnonProxyRequest anonRequest, ProxyCallbackBuffer buffer, 
			int a_messageType, IHTTPHelper a_helper) throws ProxyCallbackNotProcessableException
	{
		int startOffset = buffer.getModificationStartOffset();
		int endOffset = buffer.getModificationEndOffset();
		int length = (endOffset - startOffset) + 1;
		byte[] chunk = buffer.getChunk();
		/* Does only work with a valid AnonProxy reference */
		if(anonRequest == null)
		{
			throw new NullPointerException("AnonProxyRequest must not be null!");
		}
		int headerEndLength = HTTP_HEADER_END.length();
		int headerEndIndex = -1;
		//preceding bytes of that chunk possibly containing line termination bytes
		byte[] prefixBytes = null;
		
		Hashtable unfinishedMessages =
			(a_messageType == MESSAGE_TYPE_REQUEST) ? 
				m_unfinishedRequests : m_unfinishedResponses;
		String unfinishedHeaderPart = null;
		
		Hashtable byteCounter = 
			(a_messageType == MESSAGE_TYPE_REQUEST) ? 
					m_upstreamBytes : m_downstreamBytes;
		/* check if header parsing has already started but hasn't finished yet for this AnonRequest */
		synchronized(this)
		{
			unfinishedHeaderPart = (String) unfinishedMessages.get(anonRequest);
		}
		
		if(unfinishedHeaderPart != null)
		{
			//take care, as there may be some HTTP header ending bytes in the previous extracted part
			if(unfinishedHeaderPart.length() > (HTTP_HEADER_END_BYTES.length-1) )
			{
				prefixBytes = 
					unfinishedHeaderPart.substring(
						unfinishedHeaderPart.length() - (HTTP_HEADER_END_BYTES.length-1)).getBytes();
			}
			else
			{
				prefixBytes = unfinishedHeaderPart.getBytes();
			}
		}
		
		/* get the index of the first char after the CRLFCRLF end delimiter of the http headers */		
		headerEndIndex = indexOfHTTPHeaderEnd(prefixBytes, chunk, startOffset, endOffset);
		
		/* this specifies how many bytes of the chunk may contain header data 
		 * (if the chunk contain header data at all)
		 */
		//int endLen = (headerEndIndex == -1) ? length : Math.min(headerEndIndex, length);
		int endLen = (headerEndIndex == -1) ? length : (headerEndIndex - startOffset);
		int contentBytes = length;
		String chunkData = null;
		
		/* only if this chunk contains http headers, we need to turn it into a string */
		if( (unfinishedHeaderPart != null) || hasAlignedHTTPStartLine(chunk, startOffset, endLen, a_messageType) )
		{
			contentBytes = length - endLen;
			chunkData = ( (unfinishedHeaderPart == null) ? "" : unfinishedHeaderPart) + new String(chunk, startOffset, endLen );
			
			boolean finished = extractHeaderParts(anonRequest, chunkData, a_messageType);

			if(!finished)
			{
				/* if header parsing hasn't finished yet:
				 * chunk can be delivered with delay by returning null for now and
				 * the rest later on.
				 */
				return STATUS_DELAY;
			}
			HTTPConnectionHeader connHeader = null;
			synchronized (this)
			{
				connHeader = (HTTPConnectionHeader) m_connectionHTTPHeaders.get(anonRequest);
			}
						
			if (connHeader != null && connHeader.getRequestLine() != null)
			{
				byte[] newHeaders = a_helper.dumpHeader(this, connHeader, anonRequest);
				if (getEvent(anonRequest).isConfirmationNeeded())
				{
					// TODO check whether we are allowed to perform this request
				}
				
				countContentBytes(anonRequest, contentBytes, byteCounter, FIRE_EVENT);
				
				int contentDataStart = newHeaders.length + startOffset;
				int trailingDataStart = contentDataStart+contentBytes;
				byte[] newChunk = new byte[trailingDataStart + buffer.getTrailingDataLength()];
				
				buffer.copyLeadingData(newChunk);
				System.arraycopy(newHeaders, 0, newChunk, startOffset, newHeaders.length);
	 			System.arraycopy(chunk, (endOffset+1-contentBytes), newChunk, contentDataStart, contentBytes);
				buffer.copyTrailingData(newChunk, trailingDataStart);
				
				buffer.setChunk(newChunk);
				buffer.setModificationStartOffset(contentDataStart);
				buffer.setModificationEndOffset(trailingDataStart-1);
				return STATUS_PROCESSABLE;
			}
		}
		countContentBytes(anonRequest, contentBytes, byteCounter, FIRE_EVENT);
		return STATUS_PROCESSABLE;		
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
		throws ProxyCallbackNotProcessableException
	{
		if(anonRequest == null)
		{
			throw new NullPointerException("AnonProxyRequest must not be null!");
		}
		HTTPConnectionHeader connHeader = null;
	
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
		
		//if(hasAlignedHTTPStartLine(chunkData, messageType))
		//{
			
			Hashtable unfinishedMessages = 
				(messageType == MESSAGE_TYPE_REQUEST) ? 
					m_unfinishedRequests : m_unfinishedResponses;
			
			int off_headers_end = chunkData.indexOf(HTTP_HEADER_END);
			String headerFragment = ( off_headers_end == -1 ) ? chunkData : 
				chunkData.substring(0,  off_headers_end);
			
			if(!checkValidity(headerFragment))
			{
				String errorMsgKey = null;
				if(messageType == MESSAGE_TYPE_REQUEST)
				{
					connHeader.setRequestFinished(true);
					errorMsgKey = MSG_INVALID_LINETERM_REQUEST;
				}
				else if (messageType == MESSAGE_TYPE_RESPONSE)
				{
					connHeader.setResponseFinished(true);
					errorMsgKey = MSG_INVALID_LINETERM_RESPONSE;
				}
				
				unfinishedMessages.remove(anonRequest);
				
				throw new HTTPHeaderParseException(
						HttpResponseStructure.HTTP_RETURN_BAD_REQUEST,
						messageType, 
						JAPMessages.getString(errorMsgKey));
			}
			
			
			if((off_headers_end != -1))
			{
				//Because it is assumed that the chunk is aligned: the HTTP message starts at index 0
				parseHTTPHeader(headerFragment, connHeader, messageType);
				if(messageType == MESSAGE_TYPE_REQUEST)
				{
					connHeader.setRequestFinished(true);
					/* response for that request expected. */
					connHeader.setResponseExpected(true);
				}
				else if (messageType == MESSAGE_TYPE_RESPONSE)
				{
					connHeader.setResponseFinished(true);
					/* next response expected when new request is parsed. */
					connHeader.setResponseExpected(false);
				}
				unfinishedMessages.remove(anonRequest);
				return true;
			}
			else 
			{
				unfinishedMessages.put(anonRequest, chunkData);
				return false;
			}
		//}
		//return false;
	}
	
	/**
	 * checks whether the HTTP message headers fragment specified by headerData
	 * contains invalid line terminations, i.e. all terminations other
	 * than CRLF
	 * @param headerData the fragment of the HTTP headers of a HTTP message
	 * @return false if the fragment contains invalid line terminations, true otherwise.
	 */
	public static boolean checkValidity(String headerData)
	{
		int currentCRIndex = -1;
		int currentLFIndex = -1;
		
		//a CR that stands without a trailing LF
		boolean onlyCR = false;
		//a LF that stands without a leading CR
		boolean onlyLF = false;
		boolean endsWithCR = false;
		boolean noMoreLineTerminations = false;
		boolean indexExceeds = (Math.max((currentCRIndex+1), (currentLFIndex+1)) >= headerData.length());
		
		while(!indexExceeds )
		{
			currentCRIndex = headerData.indexOf('\r', (currentCRIndex+1));
			currentLFIndex = headerData.indexOf('\n', (currentLFIndex+1));
		
			noMoreLineTerminations = ( (currentCRIndex == -1) && (currentLFIndex == -1));
			
			if(noMoreLineTerminations)
			{
				break;
			}
			
			onlyLF = (currentLFIndex != -1) && ( (currentCRIndex == -1) || (currentCRIndex != (currentLFIndex-1)) );
			if(onlyLF)
			{
				break;
			}
			
			onlyCR = (currentCRIndex != -1) && ( (currentLFIndex == -1) || (currentCRIndex != (currentLFIndex-1)) );
			
			endsWithCR = (currentCRIndex == (headerData.length() - 1));
			if(endsWithCR)
			{
				break;
			}
			indexExceeds = (Math.max((currentCRIndex+1), (currentLFIndex+1)) >= headerData.length());
		}
		return !onlyLF && ( !onlyCR || endsWithCR );
	}
	
	private boolean hasAlignedHTTPStartLine(String chunkData, int messageType)
	{
		return (messageType == MESSAGE_TYPE_REQUEST) ? isRequest(chunkData) : isResponse(chunkData);
	}
	
	private boolean hasAlignedHTTPStartLine(byte[] chunk, int off, int len, int messageType)
	{
		return (messageType == MESSAGE_TYPE_REQUEST) ? isRequest(chunk, off, len) : isResponse(chunk, off, len);
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
	
	/**
	 * this method makes a bytewise comparison so we don't have 
	 * to turn the whole chunk into a string for header detection.
	 * @param chunk
	 * @param len
	 * @return
	 */
	private boolean isRequest(byte[] chunk, int off, int len)
	{
		boolean match = true;
		for (int i = 0; i < HTTP_REQUEST_METHODS_BYTES.length; i++) {
			//the bytes encoding of the header data is the same as the default
			//Java byte encoding since we don't have any special characters 
			//in the header data
			int compLen = Math.min(len, HTTP_REQUEST_METHODS_BYTES[i].length);
			for (int j = off; j < compLen; j++) 
			{
				if(chunk[j] != HTTP_REQUEST_METHODS_BYTES[i][j])
				{
					match = false;
					break;
				}
			}
			if(!match)
			{
				match = true;
				continue;
			}
			return true;
		}
		return false;
	}
	
	private boolean isResponse(String chunkData)
	{
		return chunkData.startsWith(HTTP_VERSION_PREFIX);
	}
	
	private boolean isResponse(byte[] chunk, int off, int len)
	{
		int compLen = Math.min(len, HTTP_VERSION_PREFIX_BYTES.length);
		for (int i = off; i < compLen; i++) 
		{
			if(chunk[i] != HTTP_VERSION_PREFIX_BYTES[i])
			{
				return false;
			}
		}
		return true;
	}
	
	/*protected long getLengthOfPayloadBytes(byte[] chunkData, int l)
	{	
		//int off_firstline= chunkData.indexOf(HTTP_VERSION_PREFIX);
		int off_headers_end = indexOfHTTPHeaderEnd(chunkData);
		//if( (off_firstline != -1) )
		//{
		if(off_headers_end == -1)
		{
			return 0l;
		}
		
		return (long) (chunkData.length() - (off_headers_end+HTTP_HEADER_END.length())); 
		//}
		return (long) chunkData.length();
	}*/
	
	/**
	 * Returns the index of the first char after the first occurence of the CRLFCRLF termination
	 * in the array 'chunk' or -1 if such a termination sequence was not found.
	 */
	public static int indexOfHTTPHeaderEnd(byte[] chunk, int startIndex, int endIndex)
	{
		boolean match = false;
		for (int i = startIndex; i <= (endIndex-(HTTP_HEADER_END_BYTES.length-1)); i++) 
		{
			match = true;
			for (int j = 0; j < HTTP_HEADER_END_BYTES.length; j++) 
			{
				if(chunk[i+j] != HTTP_HEADER_END_BYTES[j])
				{
					match = false;
					break;
				}
			}
			if(match) return (i+HTTP_HEADER_END_BYTES.length);
		}
		return -1;
	}
	
	/**
	 * Returns the index of the first char after the first occurence of the CRLFCRLF termination
	 * in the array 'chunk' or -1 if such a termination sequence was not found. Also considers that 
	 * some of the line termination characters are stored at the end of the array 'prefix' which
	 * are followed by the corresponding characters in 'chunk' to complete the CRLFCRLF
	 * sequence. The index returned refers to the array 'chunk'.
	 */
	public static int indexOfHTTPHeaderEnd(byte[] prefix, byte[] chunk, 
			int chunkStartIndex, int chunkEndIndex)
	{
		if(prefix != null)
		{
			boolean match = false;
			int prefixLength = prefix.length;
			int startIndex = (prefixLength >= (HTTP_HEADER_END_BYTES.length - 1) ) ?
					prefixLength - (HTTP_HEADER_END_BYTES.length - 1) : 0;
			if((chunkEndIndex+1) + prefixLength < HTTP_HEADER_END_BYTES.length)
			{
				return -1;
			}
			for(int i = startIndex; i < prefixLength; i++)
			{
				
				//This index determines how many of the chunk bytes have to be compared
				int chunkCheckLength = 
					HTTP_HEADER_END_BYTES.length - (prefixLength - i);
				if(chunkCheckLength > (chunkEndIndex - chunkStartIndex + 1))
				{
					//not enough chunk bytes available for check
					return -1;
				}
				
				match = true;
				int j = 0;
				/* Part 1 check prefix bytes to match pattern */
				for (; ((i+j) < prefixLength); j++) 
				{
					if(prefix[i+j] != HTTP_HEADER_END_BYTES[j])
					{
						
						match = false;
						break;
					}
				}
				if(match) 
				{
					/* Part 2 continue pattern check with the chunk array. starting at the specified
					 * chunk start offset
					 */
					int k = chunkStartIndex;
					for(; (k < chunkCheckLength || j < HTTP_HEADER_END_BYTES.length); k++)
					{
						if(chunk[k] != HTTP_HEADER_END_BYTES[j++])
						{
							match = false;
							break;
						}
					}
					if(match) return k;
				}
			}
		}
		return indexOfHTTPHeaderEnd(chunk, chunkStartIndex, chunkEndIndex);
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
		String values = null;
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
			if(delim == -1)
			{
				delim = header.indexOf("\n\n");
			}
			if(delim != -1)
			{
				key = header.substring(0, delim).trim();
				if(delim+1 < header.length())
				{
					values = header.substring(delim+1).trim();
				}
				if( (key != null) && (values != null) )
				{
					if(headerType == MESSAGE_TYPE_REQUEST)
					{
						connHeader.setRequestHeader(key, values);
					}
					else if (headerType == MESSAGE_TYPE_RESPONSE)
					{
						connHeader.setResponseHeader(key, values);
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
		for (Enumeration enumeration = m_httpConnectionListeners.elements(); enumeration.hasMoreElements();)
		{
			HTTPConnectionListener listener = (HTTPConnectionListener) enumeration.nextElement();
			if (listener != null)
			{
				listener.requestHeadersReceived(event);
			}
		}
	}
	
	public synchronized void fireResponseHeadersReceived(HTTPConnectionEvent event)
	{
		for (Enumeration enumeration = m_httpConnectionListeners.elements(); enumeration.hasMoreElements();)
		{
			HTTPConnectionListener listener = (HTTPConnectionListener) enumeration.nextElement();
			if( listener != null)
			{
				listener.responseHeadersReceived(event);
			}
		}
	}
	
	public synchronized void fireDownstreamContentBytesReceived(HTTPConnectionEvent event)
	{
		for (Enumeration enumeration = m_httpConnectionListeners.elements(); enumeration.hasMoreElements();)
		{
			HTTPConnectionListener listener = (HTTPConnectionListener) enumeration.nextElement();
			if (listener != null)
			{
				listener.downstreamContentBytesReceived(event);
			}
		}
	}
	
	public synchronized void fireUpstreamContentBytesReceived(HTTPConnectionEvent event)
	{
		for (Enumeration enumeration = m_httpConnectionListeners.elements(); enumeration.hasMoreElements();)
		{
			HTTPConnectionListener listener = (HTTPConnectionListener) enumeration.nextElement();
			if (listener != null)
			{
				listener.upstreamContentBytesReceived(event);
			}
		}
	}
	
	public synchronized void closeRequest(AnonProxyRequest anonRequest)
	{
		HTTPConnectionHeader connHeader = (HTTPConnectionHeader) 
		m_connectionHTTPHeaders.get(anonRequest);
	
		if( (connHeader != null) )
		{
			connHeader.clearRequest();
			connHeader.clearResponse();
			m_upstreamBytes.remove(anonRequest);
			m_downstreamBytes.remove(anonRequest);
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
		
		private boolean responseExpected = false;
		
		private synchronized boolean isResponseExpected() 
		{
			return responseExpected;
		}

		private synchronized void setResponseExpected(boolean responseExpected) 
		{
			this.responseExpected = responseExpected;
		}

		public synchronized boolean isResponseFinished() 
		{
			return responseFinished;
		}

		private synchronized void setResponseFinished(boolean responseFinished) 
		{
			this.responseFinished = responseFinished;
		}

		private synchronized boolean isRequestFinished() 
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
		
		public synchronized String getRequestLine()
		{
			return getStartLine(reqHeaders);
		}
		
		public synchronized String getResponseLine()
		{
			return getStartLine(resHeaders);
		}
		
		public synchronized String[] getRequestHeader(String header)
		{
			return getHeader(reqHeaders, header);
		}
		
		public synchronized String[] getResponseHeader(String header)
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
		
		/* private util-function area. All of these functions are not thread safe and are only to be accessed  
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
			if (valueContainer == null || valueContainer.size() == 0)
			{
				LogHolder.log(LogLevel.ERR, LogType.NET, "Invalid request because it contains no startline");
				return null;
			}
		
			if(valueContainer.size() > 1)
			{
				String errOutput = "";
				for (int i = 0; i < valueContainer.size(); i++) 
				{
					errOutput+= valueContainer.elementAt(i) + "\n";
				}
				LogHolder.log(LogLevel.ERR, LogType.NET, 
						"This HTTP message seems to be invalid, because it has multiple start lines:\n"
					+errOutput);
			}
			return (String)valueContainer.elementAt(0);
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
		
		private byte[] dumpRequestHeaders()
		{
			return dumpHeaders(reqHeaders, reqHeaderOrder);
		}
		
		private byte[] dumpResponseHeaders()
		{
			return dumpHeaders(resHeaders, resHeaderOrder);
		}
		
		private byte[] dumpHeaders(Hashtable headerMap, Vector headerOrder)
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
			if (LogHolder.isLogged(LogLevel.INFO, LogType.NET))
			{
				LogHolder.log(LogLevel.INFO, LogType.NET, Thread.currentThread().getName()+": header dump:\n"+allHeaders);
			}
			return allHeaders.getBytes();
		}
	}
}
