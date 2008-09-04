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
package anon.tor.ordescription;

import java.net.InetAddress;
import java.util.StringTokenizer;
import java.util.Vector;

import anon.infoservice.ListenerInterface;

public class ORAcl
{
	private Vector m_Constraints;
	
	private class AclElement
	{
		byte[] arAdrWithMask;
		byte[] arAdrMask;
		int portLow;
		int portHigh;
		boolean bIsAccept;

		public AclElement(boolean baccept, String a, String m, int l, int h) throws Exception
		{
			InetAddress ia = InetAddress.getByName(a);
			arAdrWithMask = ia.getAddress();
			ia = InetAddress.getByName(m);
			arAdrMask = ia.getAddress();
			
			for (int i = 0; i < 4; i++)
			{
				arAdrWithMask[i] &= arAdrMask[i];
			}
			
			portLow = l;
			portHigh = h;
			bIsAccept = baccept;
		}

		public boolean isContained(String adr, int port) throws Exception
		{
			if (port < portLow || port > portHigh)
			{
				return false;
			}
			
			if (adr != null)
			{
				InetAddress ia = InetAddress.getByName(adr);
				byte[] arIP = ia.getAddress();
				for (int i = 0; i < 4; i++)
				{
					if ( (arIP[i] & arAdrMask[i]) != this.arAdrWithMask[i])
					{
						return false;
					}
				}
			}
			
			return true;
		}

		public boolean isAccept()
		{
			return bIsAccept;
		}

	}

	/**
	 * Constructor
	 *
	 */
	public ORAcl()
	{
		m_Constraints = new Vector();
	}

	/**
	 * add a acl condition
	 * @param acl
	 * acl
	 * @throws Exception
	 */
	public void add(String acl) throws Exception
	{
		StringTokenizer st = new StringTokenizer(acl);
		String s = st.nextToken();
		boolean bAccept = false;
		
		if (s.equals("accept"))
		{
			bAccept = true;
		}
		
		s = st.nextToken();
		st = new StringTokenizer(s, ":");
		String a = st.nextToken();
		String ports = st.nextToken();
		int l = 0xFFFF;
		int h = 0;
		
		if (ports.equals("*"))
		{
			l = 0;
			h = 0xFFFF;
		}
		else
		{
			st = new StringTokenizer(ports, "-");
			l = Integer.parseInt(st.nextToken());
			if (st.hasMoreTokens())
			{
				h = Integer.parseInt(st.nextToken());
			}
			else
			{
				h = l;
			}
		}
		
		String adr = null;
		String mask = null;
		if (a.equals("*"))
		{
			adr = "0.0.0.0";
			mask = "0.0.0.0";
		}
		else
		{
			st = new StringTokenizer(a, "/");
			adr = st.nextToken();
			if (st.hasMoreElements())
			{
				mask = st.nextToken();
				try
				{
					int intMask = Integer.parseInt(mask);
					if (intMask >= 0)
					{
						// alternative netmask description found
						mask = "";
						for (int i = 0; i < 4; i++)
						{
							if (intMask >= 8)
							{
								mask += 255;
							}
							else if (intMask == 0)
							{
								mask += 0;
							}
							else
							{
								mask += (255 - ((int)Math.pow(2, 8-intMask) - 1));
							}
							intMask = Math.max(0, intMask - 8);
							if (i != 3)
							{
								mask += ".";
							}
						}
					}
				}
				catch (NumberFormatException a_e)
				{
				}
			}
			else
			{
				mask = "255.255.255.255";
			}
		}
		m_Constraints.addElement(new AclElement(bAccept, adr, mask, l, h));
	}

	/** Checks if a nummeric ip and port is allowed
	 * @param adr
	 * address
	 * @param port
	 * port
	 */
	public boolean isAllowed(String adr, int port)
	{
		if (!ListenerInterface.isValidIP(adr))
		{
			return false;
		}
		
		try
		{
			for (int i = 0; i < m_Constraints.size(); i++)
			{
				AclElement acl = (AclElement) m_Constraints.elementAt(i);
				if (acl.isContained(adr, port))
				{
					return acl.isAccept();
				}
			}
		}
		catch (Exception e)
		{
		}
		
		return false;
	}

	/** Checks if a port is allowed ignoring the ip
	 * @param port
	 * port
	 */
	public boolean isAllowed(int port)
	{
		try
		{
			for (int i = 0; i < m_Constraints.size(); i++)
			{
				AclElement acl = (AclElement) m_Constraints.elementAt(i);
				if (acl.isContained(null, port))
				{
					return acl.isAccept();
				}
			}
		}
		catch (Exception e)
		{
		}
		
		return false;
	}

}
