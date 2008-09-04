/*
Copyright (c) 2004, The JAP-Team
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

package anon.mixminion.mmrdescription;

import java.io.IOException;
import java.io.LineNumberReader;
import java.io.StringReader;
import java.util.Vector;

import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;

import HTTPClient.HTTPConnection;
import HTTPClient.HTTPResponse;


/**
 *
 * @author Stefan Roenisch
 */
public class ServerStats {

	private String m_Server;
	private int m_Port;

	/**
	 * Constructor
	 *
	 */
	public ServerStats()
	//FIXME Do this within the Infoservice!!!
	//http://privacy.outel.org/minion/nlist.txt
	{
		m_Server = "privacy.outel.org";
		m_Port = 80;
	}

	/**
	 * method to get a List with those of the mixminion routers who are down
	 * @return a Vector with Strings, names of the unreachable routers
	 * @throws IOException
	 */
	public Vector getWhoIsDown() throws IOException
	{
		String list = null;
		Vector ret = new Vector();
		try
		{

			HTTPConnection http = new HTTPConnection(m_Server, m_Port);
			HTTPResponse resp = http.Get("/minion/nlist.txt");

			if (resp.getStatusCode() != 200)
			{
				LogHolder.log(LogLevel.DEBUG, LogType.MISC,
						  "There was a problem with fetching the Statistics of the Mixminion-network. ");
				return ret;
			}
			list = resp.getText();
		}
		catch (Throwable t)
		{
			return ret;
		}

		LineNumberReader reader = new LineNumberReader(new StringReader(list));
		String aktLine = reader.readLine();
		reader.readLine();reader.readLine();reader.readLine();reader.readLine();
		aktLine = reader.readLine();
		while (aktLine.length()>5) {


//			#	0 to 5 minutes
//			*	5 to 60 minutes
//			+	1 to 4 hours
//			-	4 to 24 hours
//			.	1 to 2 days
//			_	2 or more days

			char status = aktLine.charAt(26);
			if ((status == ' ') || (status == '.') || (status == '_') || (status == '-'))    {

				String name = aktLine.substring(0,15);
				ret.addElement(name.trim());

			}
			aktLine = reader.readLine();
		}
		return ret;

	}
}
