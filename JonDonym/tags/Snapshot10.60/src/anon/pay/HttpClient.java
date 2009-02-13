/*
 Copyright (c) 2000, The JAP-Team
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
package anon.pay;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;

import org.w3c.dom.Document;

import anon.pay.xml.XMLDescription;
import java.io.BufferedOutputStream;
import anon.util.XMLUtil;

final public class HttpClient
{
	private BufferedReader m_reader;
	private BufferedOutputStream m_OS;
	private Socket m_socket;

	/**
	 * Klasse zum Senden von Http-Request und empfangen der Antwort.
	 *
	 * @param socket Socket, \uFFFDber dem die Http-Verbindung l\uFFFDuft.
	 * @throws IOException
	 */
	public HttpClient(Socket socket) throws IOException
	{
		m_socket = socket;
		m_reader = new BufferedReader(new InputStreamReader(m_socket.getInputStream()));
		m_OS = new BufferedOutputStream(m_socket.getOutputStream(),4096);
	}
	
	/**
	 * Schlie\uFFFDt die Http-Verbindung.
	 *
	 * @throws IOException
	 */
	public void close() throws Exception
	{
		writeRequest("GET", "close", null);
		readAnswer();
		//m_dataOS.close();
		//m_dataIS.close();
		/*SK13 removed because not Java 1.1. */
		//	m_socket.shutdownInput();
		//  m_socket.shutdownOutput();
		if(m_socket != null)
		{
			m_socket.close();
		}
	}

	/**
	 * Sendet Http-Request.
	 *
	 * @param method Http-Methode (GET / POST)
	 * @param url URL
	 * @param data Im Body zu \uFFFDbermittelnde Daten
	 */
	public void writeRequest(String method, String url, String data) throws IOException
	{
		m_OS.write((method + " /" + url + " HTTP/1.1\r\n").getBytes());
		if (method.equals("POST"))
		{
			m_OS.write(("Content-Length: " + data.length() + "\r\n").getBytes());
			m_OS.write("\r\n".getBytes());
			m_OS.write(data.getBytes());
		}
		else
		{
			m_OS.write("\r\n".getBytes());
		}
		m_OS.flush();
	}

	/**
	 * Einlesen der Http-Antwort.
	 *
	 * @return Die im Body der Antwort enthaltenen Daten als XMLDocument
	 * @throws IOException
	 */
	public Document readAnswer() throws Exception
	{
		int contentLength = -1;
		char[] data = null;
		int index;
		String line = m_reader.readLine(); 
				
		if(line == null) {
			throw new IOException("No answer received");
		}
		if ( (index = line.indexOf(" ")) == -1)
		{
			throw new IOException("Wrong Header");
		}
		line = line.substring(index + 1);
		if ( (index = line.indexOf(" ")) == -1)
		{
			throw new IOException("Wrong Header");
		}
		String Status = line.substring(0, index);
		String statusString = line.substring(index + 1);
		
		line = m_reader.readLine();
		while (line != null)
		{
			if(line.equals(""))
			{
				break;
			}
			
			if ( (index = line.indexOf(" ")) == -1)
			{
				throw new IOException("Wrong Header: "+line);
			}
			String headerField = line.substring(0, index);
			String headerValue = line.substring(index + 1).trim();
			if (headerField.equalsIgnoreCase("Content-length:"))
			{
				try 
				{
					contentLength = Integer.parseInt(headerValue);
				}
				catch (NumberFormatException nfe)
				{
					throw new IOException("Error: received invalid value for header Content-length: "+headerValue);
				}
			}
			line = m_reader.readLine();
		}
				
		if (contentLength > 0)
		{
			/** @todo Check if needed!
			if (contentLength > 10000)
			{
				throw new IOException("Communication Error");
			}
		 */

			data = new char[contentLength];

			int pos = 0;
			int ret = 0;
			do
			{
				ret = m_reader.read(data, pos, contentLength - pos);
				if (ret == -1)
				{
					break;
				}
				pos += ret;
			}
			while (pos < contentLength);
		}
		if (!Status.equals("200"))
		{
			if (Status.equals("409"))
			{
				String descstr;
				try
				{
					XMLDescription desc = new XMLDescription(data); 
					descstr = desc.getDescription();
				}
				catch (Exception e)
				{
					descstr = "Unkown Error";
				}

				throw new IOException(descstr);
			}
			throw new IOException(statusString);
		}
		return XMLUtil.toXMLDocument(data);
	}
}
