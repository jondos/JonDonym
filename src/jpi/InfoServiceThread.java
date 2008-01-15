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

import HTTPClient.HTTPConnection;
import anon.infoservice.HTTPConnectionFactory;
import anon.infoservice.ListenerInterface;
import anon.infoservice.PaymentInstanceDBEntry;
import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;
import java.util.Enumeration;

class InfoServiceThread implements Runnable
{
	public void run()
	{
		for (; ; )
		{
			Enumeration infoservices = Configuration.getInfoservices();
			while (infoservices.hasMoreElements())
			{
				ListenerInterface is = (ListenerInterface) infoservices.nextElement();
				try
				{
					HTTPConnectionFactory factory = HTTPConnectionFactory.getInstance();
					HTTPConnection con = factory.createHTTPConnection(is);
					PaymentInstanceDBEntry pi = new PaymentInstanceDBEntry(Configuration.getBiID(),
						Configuration.getBiName(), Configuration.getOwnCertificate(),
						Configuration.getJapListenerInterfaces(),
						Configuration.BEZAHLINSTANZ_VERSION,
						System.currentTimeMillis());
					con.Post(pi.getPostFile(), pi.getPostData());
				}
				catch (Throwable e)
				{
					LogHolder.log(LogLevel.DEBUG, LogType.NET,
								  "Could not send payment instance info to InfoService " + is.getHost() + ":" +
								  is.getPort());
				}
			}
			try
			{
				Thread.sleep(600000); //10 min
			}
			catch (Throwable t)
			{
			}

		}
	}
}
