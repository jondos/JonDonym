/*
 Copyright (c) 2000 - 2004, The JAP-Team
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
package jpi.util;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import anon.pay.xml.XMLErrorMessage;
import anon.util.IXMLEncodable;
import anon.util.XMLUtil;
import jpi.PIRequest;
import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;
import java.io.OutputStream;
import java.io.BufferedOutputStream;

/**
 * Simple & small http server
 *
 * @author Andreas Mueller, Bastian Voigt
 */
final public class HttpServer
{
	private final static int MAX_LINE_LENGTH = 100;
	private DataInputStream m_dataIS;
	private DataOutputStream m_dataOS;
//	private Socket m_socket;
	private ErrorCodeMap m_errors;


	public HttpServer(DataInputStream in, OutputStream out) throws IOException
	{
//		m_socket = socket;
		m_dataIS = in;
		m_dataOS = new DataOutputStream(new BufferedOutputStream(out,4096));

		m_errors = ErrorCodeMap.getInstance();
	}

	/**
	 * Closes the connection, not including the socket...
	 * @todo rethink & rewrite
	 *
	 * @throws IOException
	 */
	public void close() throws IOException
	{
		m_dataIS.close();
		m_dataOS.close();
/*		m_socket.shutdownInput();
		m_socket.shutdownOutput();
		m_socket.close();*/
	}

	/**
	 * Reads and parses a http request.
	 * Blocks until a complete request arrives on our socket.
	 *
	 * Modifications by Bastian Voigt:
	 * <ul>
	 * <li>Parsing of form data sent via GET method added
	 * </ul>
	 *
	 * @return {@link PIRequest} object
	 * @throws Exception
	 */
	public PIRequest parseRequest() throws Exception
	{
		PIRequest request = new PIRequest();
		int index, index2;

		// read request method (GET, POST, ...)
		String line = readLine();
		if ( (index = line.indexOf(" ")) == -1)
		{
			writeError(400); //syntax error
			LogHolder.log(LogLevel.DEBUG, LogType.PAY, "HttpServer.parseRequest Fehler 1");
			return null;
		}
		request.method = line.substring(0, index);
		line = line.substring(index + 1);

		// read parameters for get requests
		// if( (request.method.equalsIgnoreCase("GET")) && (index=line.indexOf("?"))>0 ) {

		// }

		// read relative url and form data sent via GET method
		if ( (index = line.indexOf(" ")) == -1)
		{
			writeError(400); // syntax error
			LogHolder.log(LogLevel.DEBUG, LogType.PAY, "HttpServer.parseRequest Fehler 2");
			return null;
		}
		if ( (request.method.equalsIgnoreCase("GET")) && ( (index2 = line.indexOf("?")) > 0))
		{
			request.url = line.substring(0, index2);
			request.getData = line.substring(index2 + 1, index);
		}
		else
		{
			request.url = line.substring(0, index);
		}
		line = line.substring(index + 1);

		// read http protocol version
		if (! (line.equals("HTTP/1.1") || line.equals("HTTP/1.0")))
		{
			writeError(505);
			LogHolder.log(LogLevel.DEBUG, LogType.PAY, "HttpServer.parseRequest Fehler 2.5");
			return null;
		}

		// read header lines, search for content length
		while ( (line = readLine()).length() != 0)
		{
			if ( (index = line.indexOf(" ")) == -1)
			{
				writeError(400);
				LogHolder.log(LogLevel.DEBUG, LogType.PAY, "HttpServer.parseRequest Fehler 3");
				return null;
			}

			String headerField = line.substring(0, index);
			String headerValue = line.substring(index + 1).trim();
			if (headerField.equalsIgnoreCase("Content-length:"))
			{
				request.contentLength = Integer.parseInt(headerValue);
			}
		}

		// if method is post read request data
		if (request.method.equalsIgnoreCase("POST"))
		{
			if (request.contentLength < 0)
			{
				writeError(411);
				LogHolder.log(LogLevel.DEBUG, LogType.PAY, "HttpServer.parseRequest Fehler 4");
				return null;
			}
			if (request.contentLength > 10000)
			{
				writeError(413);
				LogHolder.log(LogLevel.DEBUG, LogType.PAY, "HttpServer.parseRequest Fehler 5");
				return null;
			}

			byte[] data = new byte[request.contentLength];

			int pos = 0;
			int ret = 0;
			do
			{
				ret = m_dataIS.read(data, pos, request.contentLength - pos);
				if (ret == -1)
				{
					break;
				}
				pos += ret;
			}
			while (pos < request.contentLength);
			request.data = data;
		}
		return request;
	}

	/**
	 * Sendet eine Http-Antwort.
	 *
	 * @param answer Antwort
	 * @throws IOException
	 */
	public void writeAnswer(int statuscode, IXMLEncodable answer) throws IOException
	{
		m_dataOS.writeBytes("HTTP/1.1 " + statuscode + " " + m_errors.getDescription(statuscode) +
							"\r\n");
		String content= XMLUtil.toString(XMLUtil.toXMLDocument(answer));
		if (content != null)
		{
			m_dataOS.writeBytes("Content-Type: text/xml\r\n");
			m_dataOS.writeBytes("Content-Length: " + content.length() + "\r\n");
			m_dataOS.writeBytes("\r\n");
			m_dataOS.writeBytes(content);
		}
		else
		{
			m_dataOS.writeBytes("\r\n");
		}
		m_dataOS.flush();
	}

	public void writeAnswer(int statuscode, String answer) throws IOException
	{
		m_dataOS.writeBytes("HTTP/1.1 " + statuscode + " " + m_errors.getDescription(statuscode) +
							"\r\n");
		String content= answer;
		if (content != null)
		{
			m_dataOS.writeBytes("Content-Type: text/html\r\n");
			m_dataOS.writeBytes("Content-Length: " + content.length() + "\r\n");
			m_dataOS.writeBytes("\r\n");
			m_dataOS.writeBytes(content);
		}
		else
		{
			m_dataOS.writeBytes("\r\n");
		}
		m_dataOS.flush();
	}

	/**
	 * Sends a HTTP error code with the appropriate description and a XMLErrorMessage
	 * structure as content.
	 *
	 * @param code Fehlercode
	 * @throws IOException
	 */
	public void writeError(int code) throws IOException
	{
		writeAnswer(code, new XMLErrorMessage(code, "HTTP error: "+ErrorCodeMap.getDescription(code)));
	}

	private String readLine() throws Exception
	{
		StringBuffer buff = new StringBuffer(256);
		int count = 0;
		int byteRead = m_dataIS.readByte();
		while (byteRead != 10 && byteRead != -1)
		{
			if (byteRead != 13)
			{
				count++;
				if (count > MAX_LINE_LENGTH)
				{
					writeError(400);
					throw new IOException();
				}

				buff.append( (char) byteRead);
			}
			byteRead = m_dataIS.read();
		}
		return buff.toString();
	}
}
