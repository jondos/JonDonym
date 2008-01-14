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
package anon;

import anon.client.AnonClient;
import anon.tor.Tor;
import anon.xmlrpc.client.AnonServiceImplProxy;
import anon.mixminion.Mixminion;

final public class AnonServiceFactory
{
	public static final String SERVICE_ANON = "AN.ON";
	public static final String SERVICE_TOR = "TOR";
	public static final String SERVICE_MIXMINION = "Mixminion";

	private static AnonService ms_AnonService = null;
	private AnonServiceFactory()
	{
	}

	/** Creates an AnonService of the given kind.
	 * At the moment the following values are defined:
	 * "AN.ON" -- creates an AnonService which uses the WebMix cascades
	 * "TOR" -- creates an AnonService which uses Tor nodes
	 * "Mixminion" -- creates an AnonService which uses Mixminion nodes
	 */

	public static AnonService getAnonServiceInstance(String kind)
	{
		if (kind == null)
		{
			return null;
		}
		if (kind.equals(SERVICE_ANON))
		{
			if (ms_AnonService == null)
			{
				ms_AnonService = new AnonClient();
			}
			return ms_AnonService; //AnonServiceImpl.create();
		}
		else if (kind.equals(SERVICE_TOR))
		{
			return Tor.getInstance();
		}
		else if (kind.equals(SERVICE_MIXMINION))
		{
			return Mixminion.getInstance();
		}
		return null;
	}

	//should be changed...
	public static AnonService create(String addr, int port)
	{
		try
		{
			return new AnonServiceImplProxy(addr, port);
		}
		catch (Exception e)
		{
			return null;
		}
	}

}
