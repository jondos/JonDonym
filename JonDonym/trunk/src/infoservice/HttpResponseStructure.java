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
package infoservice;

import java.io.UnsupportedEncodingException;
import java.util.Date;

import org.w3c.dom.Document;

import anon.infoservice.HTTPConnectionFactory;
import anon.util.XMLUtil;
import anon.util.MyStringBuilder;

/**
 * This class stores the HTTP response for the requests which reaches the InfoService. Every
 * response will have an HTTP header, which prevents proxies to store the response in their
 * cache.
 */
public final class HttpResponseStructure
{

	/**
	 * This constant is used, when HTTP OK (200) shall be returned.
	 */
	public static final int HTTP_RETURN_OK = 200;

	/**
	 * This constant is used, when HTTP BAD REQUEST (400) shall be returned.
	 */
	public static final int HTTP_RETURN_BAD_REQUEST = 400;

	/**
	 * This constant is used, when HTTP NOT FOUND (404) shall be returned.
	 */
	public static final int HTTP_RETURN_NOT_FOUND = 404;

	/**
	 * This constant is used, when HTTP INTERNAL SERVER ERROR (500) shall be returned.
	 */
	public static final int HTTP_RETURN_INTERNAL_SERVER_ERROR = 500;

	/**
	 * This constant is used, when the content part is plain text.
	 */
	public static final int HTTP_TYPE_TEXT_PLAIN = 0;

	/**
	 * This constant is used, when the content part is in the HTML format.
	 */
	public static final int HTTP_TYPE_TEXT_HTML = 1;

	/**
	 * This constant is used, when the content part is in the XML format.
	 */
	public static final int HTTP_TYPE_TEXT_XML = 2;

	/**
	 * This constant is used, when the content part is a JNLP file.
	 */
	public static final int HTTP_TYPE_APPLICATION_JNLP = 10;

	public static final int HTTP_ENCODING_PLAIN = HTTPConnectionFactory.HTTP_ENCODING_PLAIN;
	public static final int HTTP_ENCODING_ZLIB = HTTPConnectionFactory.HTTP_ENCODING_ZLIB;
	public static final int HTTP_ENCODING_GZIP = HTTPConnectionFactory.HTTP_ENCODING_GZIP;

	/**
	 * This constant is used, when no content type shall be specified in the HTTP header.
	 * This is only for internal use.
	 */
	private static final int HTTP_TYPE_NO_TYPE = -1;

	private static final String HTTP_11_STRING = "HTTP/1.1 ";
	private static final String HTTP_CRLF_STRING = "\r\n";

	private static final String HTTP_RETURN_OK_STRING = "200 OK";
	private static final String HTTP_RETURN_BAD_REQUEST_STRING = "400 Bad Request";
	private static final String HTTP_RETURN_NOT_FOUND_STRING = "404 Not Found";
	private static final String HTTP_RETURN_INTERNAL_SERVER_ERROR_STRING = "500 Internal Server Error";

	private static final String HTTP_HEADER_TYPE_STRING = "Content-type: ";
	private static final String HTTP_HEADER_ENCODING_STRING = "Content-Encoding: ";
	private static final String HTTP_HEADER_LENGTH_STRING = "Content-length: ";
	private static final String HTTP_HEADER_DATE_STRING = "Date: ";
	private static final String HTTP_HEADER_EXPIRES_STRING = "Expires: ";
	private static final String HTTP_HEADER_CACHE_CONTROL_STRING = "Cache-Control: ";
	private static final String HTTP_HEADER_PRAGMA_STRING = "Pragma: ";
	private final static String HTTP_HEADER_CACHE_CONTROL_STRINGS=HTTP_HEADER_CACHE_CONTROL_STRING+
			"no-cache"+HTTP_CRLF_STRING+HTTP_HEADER_PRAGMA_STRING+"no-cache"+HTTP_CRLF_STRING;

	//private static final String HTTP_ENCODING_PLAIN_STRING = "plain";
	private static final String HTTP_ENCODING_ZLIB_STRING = HTTPConnectionFactory.HTTP_ENCODING_ZLIB_STRING;
	private static final String HTTP_ENCODING_GZIP_STRING = HTTPConnectionFactory.HTTP_ENCODING_GZIP_STRING;

	private static final String HTTP_TYPE_APPLICATION_JNLP_STRING = "application/x-java-jnlp-file";
	//private static final String HTTP_TYPE_APPLICATION_ZLIB_STRING = "application/x-compress";
	private static final String HTTP_TYPE_TEXT_PLAIN_STRING = "text/plain";
	private static final String HTTP_TYPE_TEXT_HTML_STRING = "text/html";
	private static final String HTTP_TYPE_TEXT_XML_STRING = "text/xml";

	private static final String HTML_NOT_FOUND = "<HTML><TITLE>404 File Not Found</TITLE><H1>404 File Not Found</H1><P>File not found on this server.</P></HTML>";
	private static final String HTML_BAD_REQUEST = "<HTML><TITLE>400 Bad Request</TITLE><H1>400 Bad Request</H1><P>Your request has been rejected by the server.</P></HTML>";
	private static final String HTML_INTERNAL_SERVER_ERROR = "<HTML><TITLE>500 Internal Server Error</TITLE><H1>500 Internal Server Error</H1><P>Error while processing the request on the server.</P></HTML>";

	/**
	 * Stores the whole HTTP response, including HTTP code, header and data.
	 */
	private byte[] m_httpReturnData;

	/**
	 * Creates a new HTTP response, which only consists of the return code specified and sometimes a
	 * describing HTML message.
	 *
	 * @a_returnCode The HTTP return code, see the HTTP_RETURN constants in this class.
	 */
	public HttpResponseStructure(int a_returnCode)
	{
		if (a_returnCode == HTTP_RETURN_OK)
		{
			m_httpReturnData = createHttpMessage(HTTP_RETURN_OK, HTTP_TYPE_NO_TYPE, HTTP_ENCODING_PLAIN, null, false);
		}
		else if (a_returnCode == HTTP_RETURN_BAD_REQUEST)
		{
			m_httpReturnData = createHttpMessage(HTTP_RETURN_BAD_REQUEST, HTTP_TYPE_TEXT_HTML,
												 HTTP_ENCODING_PLAIN,
												 HTML_BAD_REQUEST.getBytes(), false);
		}
		else if (a_returnCode == HTTP_RETURN_NOT_FOUND)
		{
			m_httpReturnData = createHttpMessage(HTTP_RETURN_NOT_FOUND, HTTP_TYPE_TEXT_HTML,
												 HTTP_ENCODING_PLAIN,
												 HTML_NOT_FOUND.getBytes(), false);
		}
		else
		{
			/* any other error code is treated as internal server error */
			m_httpReturnData = createHttpMessage(HTTP_RETURN_INTERNAL_SERVER_ERROR, HTTP_TYPE_TEXT_HTML,
												 HTTP_ENCODING_PLAIN, HTML_INTERNAL_SERVER_ERROR.getBytes(), false);
		}
	}

	/**
	 * Creates a new HTTP response with HTTP return code OK (200) and the specified XML data in the
	 * content part.
	 *
	 * @param a_xmlDocument The XML data for the body of the HTTP response.
	 */
	public HttpResponseStructure(Document a_xmlDocument)
	{
		this(a_xmlDocument, HTTP_ENCODING_PLAIN);
	}

	/**
	 * Creates a new HTTP response with HTTP return code OK (200) and the specified XML data in the
	 * content part.
	 *
	 * @param a_xmlDocument The XML data for the body of the HTTP response.
	 */
	public HttpResponseStructure(Document a_xmlDocument, int a_supportedEncodings)
	{
		String xmlString = XMLUtil.toString(a_xmlDocument);
		if (xmlString == null)
		{
			m_httpReturnData = createHttpMessage(HTTP_RETURN_INTERNAL_SERVER_ERROR, HTTP_TYPE_TEXT_HTML,
												 HTTP_ENCODING_PLAIN, HTML_INTERNAL_SERVER_ERROR.getBytes(), false);
		}
		else
		{
			try
			{
				// try UTF8
				m_httpReturnData = createHttpMessage(HTTP_RETURN_OK, HTTP_TYPE_TEXT_XML,
												 a_supportedEncodings, xmlString.getBytes("UTF8"), false);
			}
			catch(UnsupportedEncodingException ex)
			{
				m_httpReturnData = createHttpMessage(HTTP_RETURN_OK, HTTP_TYPE_TEXT_XML,
						 a_supportedEncodings, xmlString.getBytes(), false);
				
			}
		}
	}

	/**
	 * Creates a new HTTP response with HTTP return code OK (200) and the content type and content
	 * data specified.
	 *
	 * @param a_httpDataType The content type of the data, see the HTTP_TYPE constants in this
	 *                       class.
	 * @param a_httpData The content data for the HTTP response.
	 */
	public HttpResponseStructure(int a_httpDataType, int a_httpEncoding, String a_httpData)
	{
		try
		{
			// try UTF8
			m_httpReturnData = createHttpMessage(HTTP_RETURN_OK, a_httpDataType, a_httpEncoding,
									a_httpData.getBytes("UTF-8"), false);
		}
		catch(UnsupportedEncodingException ex)
		{
			m_httpReturnData = createHttpMessage(HTTP_RETURN_OK, a_httpDataType, a_httpEncoding,
					a_httpData.getBytes(), false);
		}
	}

	/**
	 * Creates a new HTTP response with HTTP return code OK (200) and the content type and content
	 * data specified.
	 *
	 * @param a_httpDataType The content type of the data, see the HTTP_TYPE constants in this
	 *                       class.
	 * @param a_httpData The content data for the HTTP response.
	 */
	public HttpResponseStructure(int a_httpDataType, int a_httpEncoding, byte[] a_httpData)
	{
		m_httpReturnData = createHttpMessage(HTTP_RETURN_OK, a_httpDataType, a_httpEncoding, a_httpData, false);
	}

	/**
	 * Creates a new HTTP response with HTTP return code OK (200) and the content type and content
	 * data specified. It is possible to include only the HTTP header (needed as response to the
	 * HTTP HEAD command) for the specified data in the response, but not the data itself.
	 *
	 * @param a_httpDataType The content type of the data, see the HTTP_TYPE constants in this
	 *                       class.
	 * @param a_httpData The content data for the HTTP response.
	 * @param a_onlyHeader If this is true, only the matching HTTP header (including content type
	 *                     and content length) is included in the response, but not the data
	 *                     itself. It is needed as response for the HTTP HEAD command. If this
	 *                     parameter is set to false, the full response including header and
	 *                     data is created.
	 */
	public HttpResponseStructure(int a_httpDataType, int a_httpEncoding, String a_httpData,
								 boolean a_onlyHeader)
	{
		// TODO: this might need UTF-8 support
		m_httpReturnData = createHttpMessage(HTTP_RETURN_OK, a_httpDataType, a_httpEncoding,
											 a_httpData.getBytes(), a_onlyHeader);
	}

	/**
	 * Returns the data of this HTTP response.
	 *
	 * @return The HTTP response data stored in this instance.
	 */
	public byte[] getResponseData()
	{
		return m_httpReturnData;
	}

	/**
	 * Creates the whole HTTP response, including HTTP return code, HTTP header and the specified
	 * content.
	 *
	 * @param a_httpReturnCode The HTTP return code for the response, see the HTTP_RETURN constants
	 *                         in this class.
	 * @param a_httpDataType The content type, which shall be set in the HTTP header of the
	 *                       response. See the HTTP_TYPE constants in this class. If
	 *                       HTTP_TYPE_NO_TYPE is specified here, the content type will not be set
	 *                       in the HTTP header.
	 * @param a_httpData The content for the HTTP response. The data should be in the specified
	 *                   content type. If null is specified here, no content will be in the HTTP
	 *                   response (response will consist of HTTP return code and header only) and
	 *                   the content lenght field in the header is not set. In any other case, the
	 *                   content length will be the length of this data structure.
	 * @param a_onlyHeader If this is true, only the matching HTTP header (including content type
	 *                     and content length, if available) is included in the response, but not
	 *                     the content data itself. It is needed as response for the HTTP HEAD
	 *                     command. If this parameter is set to false, the full response including
	 *                     header and data is created.
	 *
	 * @return The created HTTP response.
	 */
	private byte[] createHttpMessage(int a_httpReturnCode, int a_httpDataType, int a_httpEncoding,
									 byte[] a_httpData, boolean a_onlyHeader)
	{
		MyStringBuilder httpHeader = new MyStringBuilder(2048);
		httpHeader.append(HTTP_11_STRING);
		/* set the return code */
		if (a_httpReturnCode == HTTP_RETURN_OK)
		{
			httpHeader.append(HTTP_RETURN_OK_STRING);
		}
		else if (a_httpReturnCode == HTTP_RETURN_BAD_REQUEST)
		{
			httpHeader.append(HTTP_RETURN_BAD_REQUEST_STRING);
		}
		else if (a_httpReturnCode == HTTP_RETURN_NOT_FOUND)
		{
			httpHeader.append(HTTP_RETURN_NOT_FOUND_STRING);
		}
		else if (a_httpReturnCode == HTTP_RETURN_INTERNAL_SERVER_ERROR)
		{
			httpHeader.append(HTTP_RETURN_INTERNAL_SERVER_ERROR_STRING);
		}
		httpHeader.append(HTTP_CRLF_STRING);
		/* set the data length header field */
		if (a_httpData != null)
		{
			httpHeader.append(HTTP_HEADER_LENGTH_STRING);
			httpHeader.append(a_httpData.length);
			httpHeader.append(HTTP_CRLF_STRING);
		}
		/* set the type header field */
		if (a_httpDataType != HTTP_TYPE_NO_TYPE)
		{
			httpHeader.append(HTTP_HEADER_TYPE_STRING);
			if (a_httpDataType == HTTP_TYPE_TEXT_PLAIN)
			{
				httpHeader.append(HTTP_TYPE_TEXT_PLAIN_STRING);
			}
			else if (a_httpDataType == HTTP_TYPE_TEXT_HTML)
			{
				httpHeader.append(HTTP_TYPE_TEXT_HTML_STRING);
			}
			else if (a_httpDataType == HTTP_TYPE_TEXT_XML)
			{
				httpHeader.append(HTTP_TYPE_TEXT_XML_STRING);
			}
			else if (a_httpDataType == HTTP_TYPE_APPLICATION_JNLP)
			{
				httpHeader.append(HTTP_TYPE_APPLICATION_JNLP_STRING);
			}
			httpHeader.append(HTTP_CRLF_STRING);
		}

		// set the encoding
		if (a_httpEncoding != HTTP_ENCODING_PLAIN)
		{
			httpHeader.append(HTTP_HEADER_ENCODING_STRING);
			if (a_httpEncoding == HTTP_ENCODING_ZLIB)
			{
				httpHeader.append(HTTP_ENCODING_ZLIB_STRING);
			}
			else if (a_httpEncoding == HTTP_ENCODING_GZIP)
			{
				httpHeader.append(HTTP_ENCODING_GZIP_STRING);
			}
			httpHeader.append(HTTP_CRLF_STRING);
		}

		/* set some more header fields */
		String currentDate = Configuration.getHttpDateFormat().format(new Date());
		httpHeader.append(HTTP_HEADER_EXPIRES_STRING);
		httpHeader.append(currentDate);
		httpHeader.append(HTTP_CRLF_STRING);
		httpHeader.append(HTTP_HEADER_DATE_STRING);
		httpHeader.append(currentDate);
		httpHeader.append(HTTP_CRLF_STRING);
		httpHeader.append(HTTP_HEADER_CACHE_CONTROL_STRINGS);

		httpHeader.append(HTTP_CRLF_STRING);
		byte[] createdHttpResponse = null;
		/* now add the data, if there are any, else return only the header */

		if ( (a_httpData != null) && (!a_onlyHeader))
		{
			byte[] headerData;

			// try UTF8
			try
			{
				 headerData = httpHeader.toString().getBytes("UTF8");
			}
			catch(UnsupportedEncodingException ex)
			{
				headerData = httpHeader.toString().getBytes();
			}
			createdHttpResponse = new byte[headerData.length + a_httpData.length];
			System.arraycopy(headerData, 0, createdHttpResponse, 0, headerData.length);
			System.arraycopy(a_httpData, 0, createdHttpResponse, headerData.length, a_httpData.length);
		}
		else
		{
			// try UTF8
			try
			{
				createdHttpResponse = httpHeader.toString().getBytes("UTF8");
			}
			catch(UnsupportedEncodingException ex)
			{
				createdHttpResponse = httpHeader.toString().getBytes();
			}
		}
		
		return createdHttpResponse;
		
	}

}
