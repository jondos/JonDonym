/*
 Copyright (c) 2000 - 2004 The JAP-Team
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

package anon.infoservice;

import org.w3c.dom.Document;

/**
 * This class describes an HTTP request.
 */
final public class HttpRequestStructure
{

	/**
	 * This is the constant for the http get command.
	 */
	public static final int HTTP_COMMAND_GET = 0;

	/**
	 * This is the constant for the http post command.
	 */
	public static final int HTTP_COMMAND_POST = 1;

	/**
	 * The command of the http request this HttpCommandStructure is used for. See the constants in
	 * this class.
	 */
	private int m_httpCommand;

	/**
	 * The filename on the HTTP server which is requested.
	 */
	private String m_httpFileName;

	/**
	 * An XML document with data which is posted to the HTTP server. For the get method or for
	 * posting no data, this value is null.
	 */
	private Document m_postDocument;

	/**
	 * Creates a new HttpRequestStructure instance.
	 *
	 * @param a_httpCommand The command of the http request the new HttpCommandStructure is used
	 *                      for. See the constants in this class.
	 * @param a_httpFileName The filename on the HTTP server which is requested.
	 * @param a_postDocument An XML document with data which is posted to the HTTP server. For the
	 *                       get method or for posting no data, this value is null.
	 */
	private HttpRequestStructure(int a_httpCommand, String a_httpFileName, Document a_postDocument)
	{
		m_httpCommand = a_httpCommand;
		m_httpFileName = a_httpFileName;
		m_postDocument = a_postDocument;
	}

	/**
	 * Creates a new HTTP GET request.
	 *
	 * @param a_httpFileName The filename on the HTTP server which is requested.
	 *
	 * @return An HttpRequestStructure which encapsulates an HTTP get request.
	 */
	public static HttpRequestStructure createGetRequest(String a_httpFileName)
	{
		return (new HttpRequestStructure(HTTP_COMMAND_GET, a_httpFileName, null));
	}

	/**
	 * Creates a new HTTP POST request.
	 *
	 * @param a_httpFileName The filename on the HTTP server which is requested.
	 * @param a_postDocument An XML document which is posted to the HTTP server. If this is null,
	 *                       an empty post request is done.
	 *
	 * @return An HttpRequestStructure which encapsulates an HTTP post request.
	 */
	public static HttpRequestStructure createPostRequest(String a_httpFileName, Document a_postDocument)
	{
		return (new HttpRequestStructure(HTTP_COMMAND_POST, a_httpFileName, a_postDocument));
	}

	/**
	 * Returns the command of the http request this HttpCommandStructure is used for. See the
	 * constants in this class.
	 *
	 * @return The HTTP command of this request.
	 */
	public int getRequestCommand()
	{
		return m_httpCommand;
	}

	/**
	 * Returns the filename on the HTTP server which is requested.
	 *
	 * @return The requested filename.
	 */
	public String getRequestFileName()
	{
		return m_httpFileName;
	}

	/**
	 * Returns the data which shall be posted to the server. If this request is a GET request, this
	 * value is null. If it is a POST request, this is the XML document to post to the server, if
	 * it is null, an empty post shall be done.
	 *
	 * @return The XML document to post to the server.
	 */
	public Document getRequestPostDocument()
	{
		return m_postDocument;
	}

}
