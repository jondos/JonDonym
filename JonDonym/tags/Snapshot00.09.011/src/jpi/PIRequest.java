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
package jpi;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.StringTokenizer;

/**
 * Data container for a http request.
 * Used by PIServer, PIUserHttpServer, PayPalHelper
 *
 * @author Andreas Mueller
 */
public class PIRequest
{
	public String method;
	public String url;
	public String getData; //params in the URL of a GET request (NOT an accessor for POST data!)
	public int contentLength;
	public byte[] data;

	// cache for getAttribute()
	private Hashtable m_attribs = null;

	public PIRequest()
	{
		method = null;
		url = null;
		contentLength = -1;
		data = null;
	}

	/** stores the request POST parameters names and values in an internal hashtable
	 * @author Bastian Voigt
	 * @date 04 July 2003
	 */
	private boolean buildHashtable()
	{
		if (method.equalsIgnoreCase("POST"))
		{
			if (data.length == 0)
			{
				return false;
			}
			m_attribs = new Hashtable(30);
			StringTokenizer tok = new StringTokenizer(new String(data), "&");
			String temp = null, a_name = null, a_val = null;
			int index = 0;
			while (tok.hasMoreTokens())
			{
				temp = tok.nextToken();
				index = temp.indexOf('=');
				a_name = temp.substring(0, index);
				a_val = temp.substring(index + 1, temp.length());
				m_attribs.put(a_name, a_val);
			}
			return true;
		}
		else if (method.equalsIgnoreCase("GET"))
		{
			if (getData.length() == 0)
			{
				return false;
			}
			m_attribs = new Hashtable(30);
			StringTokenizer tok = new StringTokenizer(getData, "&");
			String temp = null, a_name = null, a_val = null;
			int index = 0;
			while (tok.hasMoreTokens())
			{
				temp = tok.nextToken();
				index = temp.indexOf('=');
				a_name = temp.substring(0, index);
				a_val = temp.substring(index + 1, temp.length());
				m_attribs.put(a_name, a_val);
			}
			return true;
		}
		else
		{
			return false;
		}
	}

	/** Gets a parameter by name.
	 *
	 * If method is POST, the parameters are read from the request's
	 * data section.  If method is GET, the parameters are read from
	 * getData (everything in the URL that comes after the '?'
	 * character
	 *
	 * Fixme: error handling
	 * @param name Attribute's name
	 * @return Attribute value or null if the attribute does not exist
	 * @author Bastian Voigt
	 * @date 01 July 2003
	 */
	public String getParameter(String name)
	{
		if (m_attribs == null)
		{
			if (!buildHashtable())
			{
				return null;
			}
		}
		return (String) m_attribs.get(name);
	}

	/** Returns an enumeration of all parameter names
	 * Works only if method is POST.
	 * @author Bastian Voigt
	 * @date 04 July 2003
	 */
	public Enumeration getParameterNames()
	{
		if (m_attribs == null)
		{
			if (!buildHashtable())
			{
				return null;
			}
		}
		return m_attribs.keys();
	}

}
