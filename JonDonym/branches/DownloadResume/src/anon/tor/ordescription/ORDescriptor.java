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

import java.io.LineNumberReader;
import java.util.StringTokenizer;
import java.util.Vector;

import anon.crypto.MyRSAPublicKey;
import anon.crypto.MyRSASignature;
import anon.util.Base64;
import anon.tor.util.Base16;

import org.bouncycastle.crypto.digests.SHA1Digest;

public class ORDescriptor
{
	private String m_address;
	private String m_name;
	private String m_fingerprint;
	private boolean m_hibernate;
	private int m_port;
	private int m_portDir;
	private int m_uptime;
	private String m_strSoftware;
	private String m_published;
	private String m_hash;
	private ORAcl m_acl;
	private boolean m_bIsExitNode;
	private MyRSAPublicKey m_onionkey;
	private MyRSAPublicKey m_signingkey;
	private Vector family;

	/**
	 * Constructor
	 * @param address
	 * address of the onion router
	 * @param name
	 * name for the onion router
	 * @param port
	 * port
	 * @param strSoftware
	 * version of the onion router software
	 */
	public ORDescriptor(String address, String name, int port, String strSoftware)
	{
		this.m_address = address;
		this.m_name = name;
		this.m_port = port;
		m_portDir = -1;
		m_strSoftware = strSoftware;
		m_acl = new ORAcl();
		m_bIsExitNode = false;
		m_uptime = 0;
		m_hibernate = false;
		family = null;
	}

	public void setPublished(String published)
	{
		m_published = published;
	}
	
	public String getPublished()
	{
		return m_published;
	}
	
	public void setFingerprint(String fp)
	{
		m_fingerprint = fp;
	}
	
	public String getFingerprint()
	{
		return m_fingerprint;
	}
	
	public void setHash(String hash)
	{
		m_hash = hash;
	}
	
	public String getHash()
	{
		return m_hash;
	}
	
	public void setUptime(int uptime)
	{
		m_uptime = uptime;
	}
	
	public int getUptime()
	{
		return m_uptime;
	}
	
	public Vector getFamily()
	{
		return family;
	}
	
	public void setHibernate(boolean hibernate)
	{
		m_hibernate = hibernate;
	}
	
	public boolean getHibernate()
	{
		return m_hibernate;
	}
	
	/**
	 * sets this server as exit node or not
	 * @param bIsExitNode
	 *
	 */
	public void setExitNode(boolean bIsExitNode)
	{
		m_bIsExitNode = bIsExitNode;
	}

	public void setFamily(Vector fam)
	{
		family = fam;
	}

	/**
	 * returns if this server is an exit node
	 * @return
	 */
	public boolean isExitNode()
	{
		return m_bIsExitNode;
	}

	/**
	 * sets the ACL for this onion router
	 * @param acl
	 * ACL
	 */
	public void setAcl(ORAcl acl)
	{
		m_acl = acl;
	}

	/**
	 * gets the ACL for this onion router
	 * @return
	 * ACL
	 */
	public ORAcl getAcl()
	{
		return m_acl;
	}

	/**
	 * sets the onionkey for this OR
	 * @param onionkey
	 * onionkey
	 * @return
	 * true if the key is a rsa key
	 */
	public boolean setOnionKey(byte[] onionkey)
	{
		m_onionkey = MyRSAPublicKey.getInstance(onionkey);
		return m_onionkey != null;
	}

	/**
	 * gets the onionkey
	 * @return
	 * onionkey
	 */
	public MyRSAPublicKey getOnionKey()
	{
		return this.m_onionkey;
	}

	/**
	 * sets the signing key
	 * @param signingkey
	 * signing key
	 * @return
	 * true if the key is a RSA key
	 */
	public boolean setSigningKey(byte[] signingkey)
	{
		m_signingkey = MyRSAPublicKey.getInstance(signingkey);
		return m_signingkey != null;
	}

	/**
	 * gets the signing key
	 * @return
	 * signing key
	 */
	public MyRSAPublicKey getSigningKey()
	{
		return this.m_signingkey;
	}

	/**
	 * gets the address of the OR
	 * @return
	 * address
	 */
	public String getAddress()
	{
		return this.m_address;
	}
	/**
	 * gets the name of the OR
	 * @return
	 * name
	 */
	public String getName()
	{
		return this.m_name;
	}

	/**
	 * sets the port of the directory server
	 * @param port
	 * port
	 */
	public void setDirPort(int port)
	{
		m_portDir = port;
	}

	/**
	 * gets the port
	 * @return
	 * port
	 */
	public int getPort()
	{
		return m_port;
	}

	/**
	 * gets the port of the directory server
	 * @return
	 * port
	 */
	public int getDirPort()
	{
		return m_portDir;
	}

	/**
	 * gets the software version of this OR
	 * @return
	 * software version
	 */
	public String getSoftware()
	{
		return m_strSoftware;
	}

	/**
	 * test if two OR's are identical
	 * returns also true, if the routers are in the same family
	 * @param or
	 * OR
	 * @return
	 */
	public boolean isSimilar(Object onionrouter)
	{
		if (onionrouter != null)
		{
			if(onionrouter instanceof ORDescriptor)
			{
				ORDescriptor or = (ORDescriptor)onionrouter;
				
				if( m_address.equals(or.getAddress()) &&	
					m_name.equals(or.getName()) &&
				   (m_port == or.getPort()))
				{
					return true;
				}
				//routers in the same family are also equal
				else if(or.family != null && family != null)
				{
					if((or.family.contains(m_name)&&family.contains(or.getName())))
					{
						return true;
					}
				}
			}
		}
		
		return false;
	}

	/**Tries to parse an router specification according to the descriptor.
	 * @param reader
	 * reader
	 * @return
	 * parsed descriptor (or null on error)
	 */
	public static ORDescriptor parse(LineNumberReader reader)
	{
		try
		{
			StringBuffer router_doc = new StringBuffer();
			String ln = reader.readLine();
			router_doc.append(ln);
			router_doc.append("\n");
			boolean bIsExitNode = false;
			if (ln == null || ! ln.startsWith("router"))
			{
				return null;
			}
			
			StringTokenizer st = new StringTokenizer(ln);
			st.nextToken(); // skip "router"
			String nickname = st.nextToken();
			String adr = st.nextToken();
			String orport = st.nextToken();
			String socksport = st.nextToken();
			String dirport = st.nextToken();
			
			Vector fam = null;
			byte[] key = null;
			byte[] signingkey = null;
			ORAcl acl = new ORAcl();
			String strSoftware = "";
			String published   = "";
			String fp = "";
			boolean hibernate = false;
			
			for (;;)
			{
				ln = reader.readLine();
				if (ln == null)
					return null;
				router_doc.append(ln);
				router_doc.append("\n");
				if (ln == null)
				{
					return null;
				}
				
				// remove "opt" (optional) in front of line
				if (ln.startsWith("opt "))
				{
					ln = ln.substring(4);
				}
				
				if (ln.startsWith("platform"))
				{
					strSoftware = ln.substring(9);
				/*	st = new StringTokenizer(ln);
					st.nextToken(); // skip "platform"
					strSoftware = st.nextToken() + " " + st.nextToken();*/
				}
				else if (ln.startsWith("published"))
				{
					published = ln.substring(10);
				}
				else if (ln.startsWith("accept"))
				{
					acl.add(ln);
					bIsExitNode = true;
				}
				else if (ln.startsWith("reject"))
				{
					acl.add(ln);
				}
				else if (ln.startsWith("fingerprint"))
				{
					StringBuffer buff = new StringBuffer();
					st = new StringTokenizer(ln);
					st.nextToken(); // skip "fingerprint"
					
					while (st.hasMoreTokens())
					{
						buff.append(st.nextToken());
					}
					fp = buff.toString();
				}
				else if (ln.startsWith("hibernate"))
				{
					try
					{
						if (Integer.parseInt(ln.substring(10)) == 1)
						{
							hibernate = true;
						}
						else
						{
							hibernate = false;
						}
					}
					catch (Exception e)
					{
					}
				}
				else if (ln.startsWith("onion-key"))
				{
					StringBuffer buff = new StringBuffer();
					ln = reader.readLine(); // skip -----begin
					if (ln == null)
						return null;
					router_doc.append(ln);
					router_doc.append("\n");
					
					for (;;)
					{
						ln = reader.readLine();
						if (ln == null)
							return null;		
						router_doc.append(ln);
						router_doc.append("\n");
						if (ln.startsWith("-----END"))
						{
							key = Base64.decode(buff.toString());
							break;
						}
						buff.append(ln);
					}
				}
				else if (ln.startsWith("signing-key"))
				{
					StringBuffer buff = new StringBuffer();
					ln = reader.readLine(); //skip -----begin
					if (ln == null)
						return null;
					router_doc.append(ln);
					router_doc.append("\n");
					
					for (;;)
					{
						ln = reader.readLine();
						if (ln == null)
							return null;
						router_doc.append(ln);
						router_doc.append("\n");
						if (ln.startsWith("-----END"))
						{
							signingkey = Base64.decode(buff.toString());
							break;
						}
						buff.append(ln);
					}
				}
				else if (ln.startsWith("family"))
				{
					st = new StringTokenizer(ln);
					st.nextToken();
					fam = new Vector();
					while(st.hasMoreTokens())
					{
						fam.addElement(st.nextToken());
					}
				}
				else if (ln.startsWith("router-signature"))
				{
					StringBuffer buff = new StringBuffer();
					ln = reader.readLine(); // skip -----begin
					if (ln == null)
						return null;
					
					for (;;)
					{
						ln = reader.readLine(); // skip signature
						if (ln == null)
							return null;
						if (ln.startsWith("-----END"))
						{	
							/*if (! checkSignature(router_doc.toString().getBytes(),buff.toString().getBytes(),signingkey))
							{
								return null;
							}*/
							
							ORDescriptor ord = new ORDescriptor(adr, nickname, Integer.parseInt(orport),
								strSoftware);
							if (! ord.setOnionKey(key) || ! ord.setSigningKey(signingkey))
							{
								return null;
							}
							ord.setAcl(acl);
							ord.setExitNode(bIsExitNode);
							ord.setFamily(fam);
							ord.setPublished(published);
							ord.setFingerprint(fp);
							ord.setHibernate(hibernate);
							ord.setHash(calcHash(router_doc.toString()));
							
							try
							{
								ord.setDirPort(Integer.parseInt(dirport));
							}
							catch (Exception e)
							{
							}
							
							return ord;
						}
						buff.append(ln);
					}
				}
			}
		}
		catch (Throwable t)
		{
			t.printStackTrace();
		}
		return null;
	}

	public String toString()
	{
		return "ORRouter: " + this.m_name + " on " + this.m_address 
		     + ":" + this.m_port + " Software : " + this.m_strSoftware + " isExitNode:" + this.m_bIsExitNode;
	}
	
	private static String calcHash(String desc)
	{
		SHA1Digest dig = new SHA1Digest();
		byte[] in = desc.getBytes();
		byte[] out = new byte[dig.getDigestSize()];
		dig.update(in, 0, in.length);
		dig.doFinal(out, 0);
		return Base16.encode(out);
	}
	
	private static boolean checkSignature(byte[] document, byte[] signature, byte[] identity)
	{
		try
		{
			MyRSAPublicKey key = MyRSAPublicKey.getInstance(identity);
			MyRSASignature sign = new MyRSASignature();
			sign.initVerify(key);
			return sign.verify(document, signature);
		}
		catch (Throwable t)
		{
			//LogHolder.
			return false;
		}
	}
}
