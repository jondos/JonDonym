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
package anon.mixminion;


/**@todo Temporary removed - needs to be rewritten.. */
//import jap.JAPModel;

import java.net.ConnectException;

import anon.AnonChannel;
import anon.AnonServerDescription;
import anon.AnonService;
import anon.AnonServiceEventListener;
import anon.IServiceContainer;
import anon.infoservice.IMutableProxyInterface;

/** This class implements the Mixminion anonymity service, which can be used to sent anonymous e-mail
 *
 */
public class Mixminion implements AnonService
{
	private static MixminionServiceDescription m_serviceDescription;
	//maximal mixminion routers, that are used
	public final static int MAX_ROUTE_LEN = 10;

	///minimal mixminion routers, that are used
	public final static int MIN_ROUTE_LEN = 2;

	private static Mixminion ms_theMixminionInstance = null;
	private IMutableProxyInterface m_proxyInterface;

	private Mixminion()
	{
	}

	public int initialize(AnonServerDescription anonServer, IServiceContainer a_serviceContainer)
	{
		m_serviceDescription = (MixminionServiceDescription)anonServer;
		return 0;
	}

	/**
	 * sets RouteLength
	 *
	 * @param len
	 * route length
	 */
	public void setRouteLen(int len)
	{
		if ( (len >= MIN_ROUTE_LEN) && (len <= MAX_ROUTE_LEN))
		{
			m_serviceDescription.setRouteLen(len);
		}
	}

	public static int getRouteLen()
	{
		return m_serviceDescription.getRouteLen();
	}

	public static String getMyEMail()
	{
		return m_serviceDescription.getMyEmail();
	}

	public int setProxy(IMutableProxyInterface a_Proxy)
	{
		m_proxyInterface=a_Proxy;
		return 0;
	}

	public IMutableProxyInterface getProxy()
	{
		return m_proxyInterface;
	}

	public void shutdown(boolean a_bResetTransferredBytes)
	{
	}

	public boolean isConnected()
	{
		return false;
	}

	/**
	 * creates a SMTP channel which sents e-mail through the mixminion-network
	 * @param type
	 * channeltype - only AnonChannel.SMTP is supported at the moment
	 * @return
	 * a channel
	 * @throws IOException
	 */
	public AnonChannel createChannel(int type) throws ConnectException
	{
		if (type != AnonChannel.SMTP)
		{
			return null;
		}
		try
		{
			/**@todo Temporary removed - needs to be rewritten.. */
			/*if (JAPModel.getMixminionMessages() != null) {
				return new MixminionPOPChannel();
			}
			else
			{*/
				return new MixminionSMTPChannel();
			//}

		}
		catch (Exception e)
		{
			throw new ConnectException("Could not create a Mixminion-Channel: " + e.getMessage());
		}
	}

	/** Always returns NULL as normal TCP/IP channels are not supported at the moment
	 */
	public AnonChannel createChannel(String host, int port) throws ConnectException
	{
		return null;
	}

	public void addEventListener(AnonServiceEventListener l)
	{
	}

	public void removeEventListener(AnonServiceEventListener l)
	{
	}

	public void removeEventListeners()
	{
	}


	/**
	 * Returns a Instance of Mixminion
	 * @return a Instance of Mixminion
	 */
	public static Mixminion getInstance()
	{
		if (ms_theMixminionInstance == null)
		{
			ms_theMixminionInstance = new Mixminion();
		}
		return ms_theMixminionInstance;
	}

}
