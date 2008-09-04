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
/*
 * Created on Jun 13, 2004
 */
package anon.tor;

import java.util.Vector;

import anon.tor.ordescription.ORDescriptor;
import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;

/**
 *
 */
public class FirstOnionRouterConnectionFactory
{

	private Vector m_firstOnionRouters;
	private Tor m_Tor;
	/**
	 * constructor
	 * @param a_Tor
	 * a tor instance
	 */
	public FirstOnionRouterConnectionFactory(Tor a_Tor)
	{
		m_firstOnionRouters = new Vector();
		m_Tor = a_Tor;
	}

	/**
	 * creates a FOR with the given description if it doesn't exist. else it returns a existing FOR
	 * @param d
	 * description of a FOR
	 * @return
	 * FirstOnionRouter
	 */
	public synchronized FirstOnionRouterConnection createFirstOnionRouterConnection(ORDescriptor d)
	{
		ORDescriptor ord;
		FirstOnionRouterConnection fOR = null;
		for (int i = 0; i < m_firstOnionRouters.size(); i++)
		{
			fOR = (FirstOnionRouterConnection) m_firstOnionRouters.elementAt(i);
			ord = fOR.getORDescription();
			if (ord.isSimilar(d))
			{
				if (!fOR.isClosed())
				{
					return fOR;
				}
				else
				{
					break;
				}
			}
			fOR = null;
		}
		if (fOR == null)
		{
			fOR = new FirstOnionRouterConnection(d, m_Tor);
		}
		try
		{
			fOR.connect();
		}
		catch (Exception ex)
		{
			LogHolder.log(LogLevel.EXCEPTION, LogType.TOR, "Error while connection to first OnionRouter");
			LogHolder.log(LogLevel.EXCEPTION, LogType.TOR, ex);
			return null;
		}
		m_firstOnionRouters.addElement(fOR);
		return fOR;
	}

	/** Closes all connections to all FirstOnionRouters */
	public synchronized void closeAll()
	{
		for (int i = 0; i < m_firstOnionRouters.size(); i++)
		{
			FirstOnionRouterConnection fOR = (FirstOnionRouterConnection) m_firstOnionRouters.elementAt(i);
			fOR.close();
		}
		m_firstOnionRouters.removeAllElements();
	}

}
