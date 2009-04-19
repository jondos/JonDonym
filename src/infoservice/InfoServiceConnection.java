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
package infoservice;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.Enumeration;
import java.util.StringTokenizer;
import java.util.Vector;

import anon.infoservice.Constants;
import anon.infoservice.HTTPConnectionFactory;
import anon.infoservice.HttpResponseStructure;
import anon.infoservice.InfoServiceDBEntry;
import anon.util.TimedOutputStream;
import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;
import java.io.*;

/**
 * This is a simple implementation of an HTTP server. This implementation doesn't support the most
 * HTTP/1.1 stuff (like persistent connections or special encodings). But parsing an HTTP/1.1
 * request is working.
 */
final public class InfoServiceConnection implements Runnable
{

	private static final int RESPONSE_CHUNK_SIZE = 5000;
	
	private static boolean ms_bLogAnonlibVersion = false;

	/**
	 * Stores the socket which is connected to the client we got the request from.
	 */
	private Socket m_socket;

	private InputStream m_inputStream;
	/**
	 * Stores the ID of the connection which is used in the log-output for identifying the current
	 * connection.
	 */
	private int m_connectionId;

	/**
	 * Stores the implementation which is used for processing the received HTTP request and creating
	 * the HTTP response which is sent back to the client.
	 */
	private JWSInternalCommands m_serverImplementation;

	/**
	 * This stores the currently number of bytes which can be read from the InputStream until the
	 * data limit is exhausted.
	 */
	private int m_byteLimit;

	//ByteArrayOutputStream used on different places of this class.
	private ByteArrayOutputStream m_tmpByteArrayOut;
	/**
	 * Creates a new instance of InfoServiceConnection for handling the received data as an HTTP
	 * request.
	 *
	 * @param a_socket The socket which is connected to the client. We read the data from this
	 *                 socket, parse it as an HTTP request, process the request and send back the
	 *                 HTTP response to this socket.
	 * @param a_connectionId The connection ID which is used for identifying the log outputs of this
	 *                       new instance of InfoServiceConnection.
	 * @param a_serverImplementation The implementation which is used for processing the HTTP
	 *                               request and creating the HTTP response which is sent back to
	 *                               the client.
	 */
	public InfoServiceConnection(Socket a_socket, int a_connectionId,
								 JWSInternalCommands a_serverImplementation)
	{
		m_socket = a_socket;
		m_connectionId = a_connectionId;
		m_serverImplementation = a_serverImplementation;
		m_tmpByteArrayOut = new ByteArrayOutputStream(512);
	}

	/**
	 * This is the Thread implementation for reading, parsing, processing the request and sending
	 * the response. In every case, the socket is closed after finishing this method.
	 */
	public void run()
	{
		try
		{
			//LogHolder.log(LogLevel.DEBUG, LogType.NET,
			//			  "InfoServiceConnection (" + Integer.toString(m_connectionId) +
			//			  "): Handle connection from: " + m_socket.getInetAddress().getHostAddress() + ":" +
			//			  m_socket.getPort());
			try
			{
				m_socket.setSoTimeout(Constants.COMMUNICATION_TIMEOUT);
			}
			catch (Exception e)
			{
				LogHolder.log(LogLevel.WARNING, LogType.NET,
							  "InfoServiceConnection (" + Integer.toString(m_connectionId) +
							  "): Cannot set socket timeout: " + e.toString());
			}
			try
			{
				m_inputStream = m_socket.getInputStream();
			}
			catch (Exception e)
			{
				LogHolder.log(LogLevel.ERR, LogType.NET,
							  "InfoServiceConnection (" + Integer.toString(m_connectionId) +
							  "): Error while accessing the socket streams: " + e.toString());
				try
				{
					m_socket.close();
				}
				catch (Exception e1)
				{
				}
				return ;
			}
			int internalRequestMethodCode = 0;
			String requestMethod = null;
			String requestUrl = null;
			byte[] postData = null;
			int supportedEncodings = HttpResponseStructure.HTTP_ENCODING_PLAIN;

			try
			{
				/* first line is the Request-Line with the format:
				 * METHOD <space> REQUEST-URI <space> HTTP-VERSION <CRLF>
				 * Attention: <CRLF> is removed from readRequestLine()
				 */
				initReader( /*streamFromClient,*/Constants.MAX_REQUEST_HEADER_SIZE);
				String requestLine;
				try
				{
					requestLine = readRequestLine();
				}
				catch (SocketException a_e)
				{
					LogHolder.log(LogLevel.NOTICE, LogType.NET, "No client request received. " + a_e.getMessage());						
					requestLine = null;
				}
				catch (SocketTimeoutException a_e)
				{
					LogHolder.log(LogLevel.WARNING, LogType.NET, "Client request timed out. " + a_e.getMessage());
					requestLine = null;
				}
				if (requestLine == null)
				{
					closeSockets();
					return;
				}
				//LogHolder.log(LogLevel.DEBUG, LogType.NET,
				//			  "InfoServiceConnection (" + Integer.toString(m_connectionId) +
				//			  "): Client: " + m_socket.getInetAddress() +  " Request line: " + requestLine);
				StringTokenizer requestLineTokenizer = new StringTokenizer(requestLine, " ");
				/*if (requestLineTokenizer.countTokens() != 3)
				   {
				 throw (new Exception("Invalid HTTP request line: " + requestLine));
				   }*/
				requestMethod = requestLineTokenizer.nextToken();
				requestUrl = requestLineTokenizer.nextToken();
				if (requestMethod.equals("POST"))
				{
					internalRequestMethodCode = Constants.REQUEST_METHOD_POST;
				}
				else if (requestMethod.equals("GET"))
				{
					internalRequestMethodCode = Constants.REQUEST_METHOD_GET;
				}
				else if (requestMethod.equals("HEAD"))
				{
					internalRequestMethodCode = Constants.REQUEST_METHOD_GET;
				}
				else
				{
					closeSockets();
					return;
				}

				int contentLength = 0;
				/* now process the HTTP request header */
				Vector v = readHeader();
				if (v == null)
				{
					closeSockets();
					return;
				}
				Enumeration headerLines = v.elements();
				boolean bStatisticsHeaderFound = !headerLines.hasMoreElements(); // ignore requests with no headers
				while (headerLines.hasMoreElements())
				{
					String currentHeaderLine = (String) (headerLines.nextElement());
					//LogHolder.log(LogLevel.DEBUG, LogType.NET,
					//			  "InfoServiceConnection (" + Integer.toString(m_connectionId) +
					//			  "): Processing header line: " + currentHeaderLine);
					/* everything until the first ':' is the field-name, everything after the first ':'
					 * belongs to the field-value
					 */
					int fieldDelimiterPos = currentHeaderLine.indexOf(":");
					if (fieldDelimiterPos < 0)
					{
						closeSockets();
						return;
					}
					String currentHeaderFieldName = currentHeaderLine.substring(0, fieldDelimiterPos);
					/* leading or trailing whitspaces can be removed from a field value */
					String currentHeaderFieldValue = currentHeaderLine.substring(fieldDelimiterPos + 1).
						trim();
					if (currentHeaderFieldName.equalsIgnoreCase("Content-Length"))
					{
						try
						{
							contentLength = Integer.parseInt(currentHeaderFieldValue);
							//LogHolder.log(LogLevel.DEBUG, LogType.NET,
							//			  "InfoServiceConnection (" + Integer.toString(m_connectionId) +
							//			  "): Read 'Content-Length: " + Integer.toString(contentLength) +
							//			  "' from header.");
						}
						catch (Exception e)
						{
							closeSockets();
							return;
						}
					}

					if (currentHeaderFieldName.toLowerCase().startsWith(InfoServiceDBEntry.HEADER_STATISTICS.toLowerCase()))
					{
						if (!ms_bLogAnonlibVersion && 
							currentHeaderFieldName.equals("statistics-anonlib-version"))
						{
							ms_bLogAnonlibVersion = true;
						}
						ISRuntimeStatistics.putClientVersion(currentHeaderFieldName, currentHeaderFieldValue);
						bStatisticsHeaderFound = true;
					}
					
					if ((currentHeaderFieldName.equalsIgnoreCase("Accept-Encoding") &&
						(internalRequestMethodCode == Constants.REQUEST_METHOD_GET ||
						 internalRequestMethodCode == Constants.REQUEST_METHOD_HEAD)) ||
						(currentHeaderFieldName.equalsIgnoreCase("Content-Encoding") &&
						 internalRequestMethodCode == Constants.REQUEST_METHOD_POST)) // for post
					{
						if (currentHeaderFieldValue != null &&
							currentHeaderFieldValue.indexOf(
								HTTPConnectionFactory.HTTP_ENCODING_ZLIB_STRING) >= 0)
						{
							supportedEncodings = HttpResponseStructure.HTTP_ENCODING_ZLIB;
						}
					}
				}
				
				if (ms_bLogAnonlibVersion && 
					internalRequestMethodCode == Constants.REQUEST_METHOD_GET &&
					!bStatisticsHeaderFound)
					
				{
					ISRuntimeStatistics.putClientVersion("statistics-anonlib-version",
							"unknown");
				}

				/* read the POST data, if it is a POST request */
				if ( (internalRequestMethodCode == Constants.REQUEST_METHOD_POST) && (contentLength >= 0))
				{
					/* the volume of post data should be limited -> check the limit */
					if (contentLength > Configuration.getInstance().getMaxPostContentLength())
					{
						throw (new Exception(
							"POST: Content is longer than allowed maximum POST content length."));
					}
					ByteArrayOutputStream postDataRead = new ByteArrayOutputStream(contentLength);
					int currentPos = 0;
					while (currentPos < contentLength)
					{
						int byteRead = m_inputStream.read();
						if (byteRead == -1)
						{
							throw (new Exception(
								"POST: Content was shorter than specified in the header. Content length from header: " +
								Integer.toString(contentLength) + " Real content length: " +
								Integer.toString(currentPos)));
						}
						currentPos++;
						postDataRead.write(byteRead);
					}
					postData = postDataRead.toByteArray();
					//LogHolder.log(LogLevel.DEBUG, LogType.NET,
					//			  "InfoServiceConnection (" + Integer.toString(m_connectionId) +
					//		  "): Post-Data received for request: " + requestUrl + ": " +
					//			  postDataRead.toString());
				}
			}
			catch (Exception e)
			{
				LogHolder.log(LogLevel.ERR, LogType.NET,
				"InfoServiceConnection (" + Integer.toString(m_connectionId) +
//				", " + m_socket.getInetAddress() +
				") - has an Error -",e);
				closeSockets();
				return;
			}

			/* request parsing done -> process the request, if there was no error */
			HttpResponseStructure response;
			try
			{
				response = m_serverImplementation.processCommand(internalRequestMethodCode,
					supportedEncodings, requestUrl, postData, m_socket.getInetAddress());
				
				if (response == null)
				{
					LogHolder.log(LogLevel.WARNING, LogType.NET,
								  "InfoServiceConnection (" + Integer.toString(m_connectionId) +
//								  ", " + m_socket.getInetAddress() +
								  "): Response could not be generated: Request: " + requestMethod +
								  " " + requestUrl);
					response = new HttpResponseStructure(HttpResponseStructure.HTTP_RETURN_NOT_FOUND);
				}
			}
			catch (Exception a_e)
			{
				LogHolder.log(LogLevel.EMERG, LogType.NET, a_e);
				response = new HttpResponseStructure(HttpResponseStructure.HTTP_RETURN_INTERNAL_SERVER_ERROR);
			}
			
			/* send our response back to the client */
			try
			{
				//LogHolder.log(LogLevel.DEBUG, LogType.NET,
				//			  "InfoServiceConnection (" + Integer.toString(m_connectionId) +
				//			  "): Response for request: " + requestUrl + ": " +
				//			  (new String(response.getResponseData())));
				TimedOutputStream streamToClient = new TimedOutputStream(m_socket.getOutputStream(),
					Constants.COMMUNICATION_TIMEOUT);

				byte[] theResponse = response.getResponseData();
				int index = 0;
				int len = theResponse.length;
				int transferLen = 0;
				//we send the data batch to the client in smaller chunks in order
				//to avoid unwanted timeouts for large messages and slow connections
				while (true)
				{
					if (len > RESPONSE_CHUNK_SIZE)
					{
						transferLen = RESPONSE_CHUNK_SIZE;
					}
					else
					{
						if (len > 0)
						{
							transferLen = len;
						}
						else
						{
							break;
						}
					}
											
					try
					{
						streamToClient.write(theResponse, index, transferLen);
						streamToClient.flush();
					}
					catch (SocketException a_e)
					{
						LogHolder.log(LogLevel.WARNING, LogType.NET, 
								"Client closed our response. " + a_e.getMessage());
					}					
					catch (InterruptedIOException a_e)
					{
						LogHolder.log(LogLevel.WARNING, LogType.NET, 
								"Response to client timed out. " + a_e.getMessage());
					}					
					index += transferLen;
					len -= transferLen;
				}
			}
			catch (Exception e)
			{
				LogHolder.log(LogLevel.ERR, LogType.NET,
				  "InfoServiceConnection (" + Integer.toString(m_connectionId) +
//				  ", " + m_socket.getInetAddress() +
				  "): Error while sending the response to the client." , e);
			}
		}
		catch (Throwable t)
		{
			LogHolder.log(LogLevel.EXCEPTION, LogType.NET, "Caught an unexpected connection error!", t);
		}

		/* try to close everything */
		closeSockets();
	}

	private void closeSockets()
	{
		m_serverImplementation = null;
		m_tmpByteArrayOut = null;

		try
		{
			if(!m_socket.isClosed())
			{
				m_socket.getOutputStream().close();
			}
		}
		catch (IOException a_e)
		{
			/* if we get an error here, normally there was already one -> log only for debug reasons */
			LogHolder.log(LogLevel.DEBUG, LogType.NET,
						  "InfoServiceConnection (" + Integer.toString(m_connectionId) +
//						  ", " + m_socket.getInetAddress() +
						  "): Error while closing output stream to client!", a_e);
		}

		try
		{
			m_inputStream.close();
		}
		catch (Exception e)
		{
			/* if we get an error here, normally there was already one -> log only for debug reasons */
			LogHolder.log(LogLevel.DEBUG, LogType.NET,
						  "InfoServiceConnection (" + Integer.toString(m_connectionId) +
//						  ", " + m_socket.getInetAddress() +
						  "): Error while closing input stream from client!", e);
		}
		m_inputStream = null;
		try
		{
			m_socket.close();
		}
		catch (Exception e)
		{
			/* if we get an error here, normally there was already one -> log only for debug reasons */
			LogHolder.log(LogLevel.DEBUG, LogType.NET,
						  "InfoServiceConnection (" + Integer.toString(m_connectionId) +
//						  ", " + m_socket.getInetAddress() +
						  "): Error while closing connection!", e);
		}
		m_socket = null;
	}

	/**
	 * Reads the first line of an HTTP request (including the CRLF at the end of the line, so the
	 * next byte read from the underlying stream, is the first byte of the request header). The
	 * line is also parsed for illegal characters. If there are illegal characters found or the
	 * read limit is exhausted or there is an unexpected end of the stream, an exception is thrown.
	 * Also if there occured an exception while reading from the stream, this exception is thrown.
	 * The syntax of the request line (whether it is a valid HTTP request line) is not checked here.
	 *
	 * @param a_inputData The InfoServiceConnectionReader for reading the line (with a limit of
	 *                    maximally read bytes).
	 *
	 * @return The line read from the stream without the trailing CRLF.
	 */
	private String readRequestLine() throws Exception
	{
		m_tmpByteArrayOut.reset();
		boolean requestLineReadingDone = false;
		while (!requestLineReadingDone)
		{
			int byteRead = read();
			if (byteRead == -1)
			{
				return null;
			}
			/* check for illegal characters */
			if ( ( (byteRead < 32) && (byteRead != 13)) || (byteRead == 127))
			{
				return null;
			}
			if (byteRead == 13)
			{
				byteRead = read();
				if (byteRead != 10)
				{
					/* only complete <CRLF> is allowed */
					return null;
				}
				/* <CRLF> found -> end of line */
				requestLineReadingDone = true;
			}
			else
			{
				m_tmpByteArrayOut.write(byteRead);
			}
		}
		return m_tmpByteArrayOut.toString();
	}

	private void initReader(int limit)
	{
		m_byteLimit = limit;
	}

	/**
	 * Reads one byte from the underlying InputStream. If the call is successful, the limit of bytes
	 * able to read from the stream is also decremented by 1. If the end of the stream is reached or
	 * there was an exception while reading from the stream, the byte limit is not decremented. In
	 * the case of a read exception, this exception is thrown. If the byte limit is exhausted, also
	 * an exception is thrown.
	 *
	 * @return The byte read from the stream or -1, if the end of the stream was reached.
	 */
	private int read() throws Exception
	{
		if (m_byteLimit < 1)
		{
			throw (new Exception("Cannot read more bytes, message size limit reached."));
		}
		int returnByte = -1;
		try
		{
			returnByte = m_inputStream.read();
			m_byteLimit--;
		}
		catch (Exception e)
		{
			throw (e);
		}
		//This does not seem to be usefull as -1 means that EOF was reached - so we can not read something anyway - can't we?
		//Therefore i comment this out...
		/*if (returnByte == -1)
		   {
		 // nothing was read -> re-increase the counter
		 m_byteLimit++;
		   }*/
		return returnByte;
	}

	/**
	 * Reads the whole header of an HTTP request (including the last CRLF signalizing the end of the
	 * header, so the next byte read from the stream would be the first of the HTTP content). The
	 * request line of the HTTP request should already be read from the stream. Folded header lines
	 * are concatenated to one header line (by removing the CRLF used for folding). Also all lines
	 * are parsed for illegal characters. If illegal characters are found or the read limit is
	 * exhausted or there is an unexpected end of the stream, an exception is thrown. Also if there
	 * occured an exception while reading from the stream, this exception is thrown. The syntax of
	 * the header lines (whether they are valid HTTP header lines) returned by this method is not
	 * checked here.
	 *
	 * @param a_inputData The InfoServiceConnectionReader where the HTTP header shall be read from
	 *                    (with a limit of maximally read bytes). The initially request line of the
	 *                    HTTP request should already be read from the underlying stream.
	 *
	 * @return A Vector of strings with the header lines (maybe empty, if there were no header
	 *         fields). The trailing CRLF is removed at every line. If a header line was folded, the
	 *         folding CRLF is removed (but not the SPACEs or TABs at the begin of the next line)
	 *         and the whole line is within one String stored. The empty line which signals the end
	 *         of the HTTP header is not included within the Vector.
	 */
	private Vector readHeader() throws Exception
	{
		m_tmpByteArrayOut.reset();
		Vector allHeaderLines = new Vector();
		boolean startOfHeader = true;
		boolean headerReadingDone = false;
		while (!headerReadingDone)
		{
			int byteRead = read();
			/* first check, whether it is the <CR> -> read the next bytes in this case */
			if (byteRead == 13)
			{
				byteRead = read();
				if (byteRead != 10)
				{
					/* only complete <CRLF> is allowed in the header */
					return null;
				}
				if (startOfHeader)
				{
					/* header started with <CRLF> -> no header -> stop reading */
					headerReadingDone = true;
				}
				else
				{
					/* we have a complete <CRLF>, but maybe it is only for folding a long header line ->
					 * if next line starts with <space> or <TAB>, it's only a folded header line -> according
					 * to the HTTP specification, it is no problem to remove <CRLF> in this case
					 */
					byteRead = read();
					if ( (byteRead != 9) && (byteRead != 32))
					{
						/* <CRLF> was end of the header line -> add the header line to the Vector of header
						 * lines (without trailing <CRLF>)
						 */
						allHeaderLines.addElement(m_tmpByteArrayOut.toString());
						m_tmpByteArrayOut.reset();
						/* maybe it was the last header line, then there is a second <CRLF> */
						if (byteRead == 13)
						{
							byteRead = read();
							if (byteRead != 10)
							{
								/* only complete <CRLF> is allowed in the header */
								return null;
							}
							/* found empty header line -> end of header -> stop reading */
							headerReadingDone = true;
						}
					}
				}
			}
			if (!headerReadingDone)
			{
				if (startOfHeader)
				{
					/* header not started with <CRLF> -> header is not empty */
					startOfHeader = false;
				}
				if (byteRead == -1)
				{
					return null;
				}
				/* check for illegal characters */
				if ( ( (byteRead < 32) && (byteRead != 9)) || (byteRead == 127))
				{
					return null;
				}
				/* valid character -> add it to the buffer */
				m_tmpByteArrayOut.write(byteRead);
			}
		}
		return allHeaderLines;
	}

}
