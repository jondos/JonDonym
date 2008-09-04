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
package infoservice.tor;

import java.net.URL;

import HTTPClient.HTTPConnection;
import anon.infoservice.HTTPConnectionFactory;
import anon.infoservice.ListenerInterface;
import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;

/**
 * This class is responsible for fetching the information about the active tor nodes. This class
 * is a singleton.
 */
public class MixminionDirectoryAgent implements Runnable
{

	/**
	 * The filename where we can find the TOR nodes file on a TOR directory server.
	 */
	private static final String DEFAULT_DIRECTORY_FILE = "/Directory.gz";

	/**
	 * Stores the instance of MixminionDirectoryAgent (singleton).
	 */
	private static MixminionDirectoryAgent ms_mdaInstance;

	private URL m_urlDirectoryServer = null;
	/**
	 * Stores the XML container with the current tor nodes list. If we don't have a topical list,
	 * this value is null.
	 */
	private byte[] m_currentMixminionNodesList;

	/**
	 * Stores the cycle time (in milliseconds) for updating the tor nodes list. Until the update
	 * thread is started, this value is -1.
	 */
	private long m_updateInterval;

	/**
	 * Returns the instance of MixminionDirectoryAgent (singleton). If there is no instance, a new one is
	 * created.
	 *
	 * @return The MixminionDirectoryAgent instance.
	 */
	public static MixminionDirectoryAgent getInstance()
	{
		if (ms_mdaInstance == null)
		{
			ms_mdaInstance = new MixminionDirectoryAgent();
		}
		return ms_mdaInstance;
	}

	/**
	 * Creates a new instance of MixminionDirectoryAgent. We do some initialization here.
	 */
	private MixminionDirectoryAgent()
	{
		m_currentMixminionNodesList = null;
		m_updateInterval = -1;
	}

	public void addDirectoryServer(URL directoryServer)
	{
		m_urlDirectoryServer = directoryServer;
	}

	/**
	 * This starts the internal mixminion nodes list update thread. You have to call this method exactly
	 * once after the creation of this MixminionDirectoryAgent. After the update thread is started once,
	 * later calls of this method are ignored.
	 *
	 * @param a_updateInterval The cycle time in milliseconds for fetching the current list of the
	 *                         tor nodes.
	 */
	public void startUpdateThread(long a_updateInterval)
	{
		synchronized (this)
		{
			/* we need exclusive access */
			if ( (m_updateInterval == -1) && (a_updateInterval > 0))
			{
				m_updateInterval = a_updateInterval;
				/* start the internal thread */
				Thread fetchThread = new Thread(this, "MixminionDirectoryAgent Update Thread");
				fetchThread.setPriority(Thread.MIN_PRIORITY);
				fetchThread.setDaemon(true);
				fetchThread.start();
			}
		}
	}

	/**
	 * Returns the ZLib commpresed current Mixminion nodes list. If we don't have a topical list,
	 * the returned value is null.
	 * The xml-struct is as follows:
	 * The following does not exist at the moment...
	 *  * <MixminionNodesList>
	 *  ... <!-- the original mixminion nodes list -->
	*
	 *   <ReliabilityNodesList generated="..."> <!-- generated is the dat, when the list was originaly generated
	 *                                               in seconds since the epoch
	 *                                            -->
	 *     <MixminionNode id=".."> <!-- id ist he unique name (aka nickname) of this Mixminion node
	 *        <UptimeHistory>
	 *          <Uptime>
	 *             <!-- one char as symbol for the uptime according to:
	 *                  0	0 to .1
                        1	.1 to .2
                        2	.2 to .3
                        3	.3 to .4
                        4	.4 to .5
                        5	.5 to .6
                        6	.6 to .7
                        7	.7 to .8
                        8	.8 to .9
                        9	.9 to 1, but less than 1
                        +	1
                        ?	no pings that day
                  -->
	 *          <Uptime>
	 *        </UptimeHistory>
	 *     <MixminionNode>
	 *   <ReliabilityNodesList>
	 * </MixminionNodesList>
	 *
	 * @return The ZLib compressed current mixminion nodes list or null, if we don't have a topical
	 *         list.
	 */
	public byte[] getMixminionNodesList()
	{
		return m_currentMixminionNodesList;
	}

	/**
	 * This is the implementation of the mixminion nodes list update thread.
	 */
	public void run()
	{
		while (true)
		{
			LogHolder.log(LogLevel.INFO, LogType.NET,
						  "MixminionDirectoryAgent: run: Try to fetch the mixminion nodes list from the known mixminion directory servers.");

			try
			{
				HTTPConnection con = HTTPConnectionFactory.getInstance().createHTTPConnection(
					new ListenerInterface(m_urlDirectoryServer.getHost(),
										  m_urlDirectoryServer.getPort()));
				con.removeModule(Class.forName("HTTPClient.ContentEncodingModule"));
				m_currentMixminionNodesList = con.Get(m_urlDirectoryServer.getFile()).getData();

/*				String mixminionNodesListInformation = Base64.encode(mixminionNodesListCompressedData, false);
				//create the MixminionNodesList XML structure for the clients
				mixminionNodesListNode = XMLUtil.createDocument().createElement("MixminionNodesList");
				mixminionNodesListNode.setAttribute("xml:space", "preserve");
				XMLUtil.setValue(mixminionNodesListNode, mixminionNodesListInformation);
				//getReliabilityList();
*/			}
			catch (Exception e)
			{
				LogHolder.log(LogLevel.ERR, LogType.MISC,
							  "MixminionDirectoryAgent: run: Error while creating the XML structure with the Mixminion nodes list: " +
							  e.toString());
				m_currentMixminionNodesList = null;
			}

			if (m_currentMixminionNodesList == null)
			{
				LogHolder.log(LogLevel.ERR, LogType.NET,
							  "MixminionDirectoryAgent: run: Could not fetch the mixminion nodes list from the known tor directory servers.");
			}
			else
			{
				LogHolder.log(LogLevel.DEBUG, LogType.NET,
							  "MixminionDirectoryAgent: run: Fetched the list of mixminion nodes successfully.");
			}
			try
			{
				Thread.sleep(m_updateInterval);
			}
			catch (Exception e)
			{
			}
		}
	}

}
