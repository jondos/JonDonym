/*
 Copyright (c) 2000 - 2005, The JAP-Team
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

import java.util.Hashtable;
import org.w3c.dom.Node;
import anon.util.XMLUtil;
import java.util.Enumeration;

/** This class stores information about all outgoing proxy addresses of all
 * cascades this InfoService knows about.
 * This is just a helper class (or a quick hack - thatever you like to call it).
 */

class VisibleProxyAddresses
{
	private static Hashtable ms_hashtableVisibleAddresses = new Hashtable();
	private static String ms_strPreComputedVisibleAddresses = null;

	/** Calculates the visible proxy addresses string from the addresses stored in the hashtable*/
	private static synchronized void precomputeVisibleAddressesString()
	{
		if (ms_hashtableVisibleAddresses.size() == 0)
		{
			ms_strPreComputedVisibleAddresses = null;
			return;
		}

		String tmp = "";
		Enumeration enumer = ms_hashtableVisibleAddresses.elements();
		while (enumer.hasMoreElements())
		{
			tmp += (String) enumer.nextElement() + " ";
		}
		ms_strPreComputedVisibleAddresses = tmp;
	}

	/** Adds the addresses store in the DOM_Node <Mix>*/
	static void addAddresses(Node nodeMix)
	{
		if (nodeMix == null)
		{
			return;
		}
		Node nodeTmp = XMLUtil.getFirstChildByName(nodeMix, "Proxies");
		if (nodeTmp == null)
		{
			return;
		}
		nodeTmp = XMLUtil.getFirstChildByName(nodeTmp, "Proxy");
		while (nodeTmp != null)
		{
			if (nodeTmp.getNodeName().equals("Proxy"))
			{
				Node nodeVisibleAddresses = XMLUtil.getFirstChildByName(nodeTmp, "VisibleAddresses");
				Node nodeVisibleAddress = XMLUtil.getFirstChildByName(nodeVisibleAddresses, "VisibleAddress");
				while (nodeVisibleAddress != null)
				{
					if (nodeVisibleAddress.getNodeName().equals("VisibleAddress"))
					{
						Node nodeHost = XMLUtil.getFirstChildByName(nodeVisibleAddress, "Host");
						String strHost = XMLUtil.parseValue(nodeHost, null);
						if (strHost != null)
						{
							synchronized (ms_hashtableVisibleAddresses)
							{
								ms_hashtableVisibleAddresses.put(strHost, strHost);
							}
						}
					}
					nodeVisibleAddress = nodeVisibleAddress.getNextSibling();
				}
			}
			nodeTmp=nodeTmp.getNextSibling();
		}
		precomputeVisibleAddressesString();
	}

	static String getVisibleAddresses()
	{
		return ms_strPreComputedVisibleAddresses;
	}
}
