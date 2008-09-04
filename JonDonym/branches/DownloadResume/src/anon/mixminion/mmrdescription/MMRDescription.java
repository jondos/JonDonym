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

import java.io.LineNumberReader;

import anon.crypto.MyRSAPublicKey;
import anon.util.Base64;
import anon.util.ByteArrayUtil;
import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;
import java.security.SecureRandom;
import java.text.SimpleDateFormat;

import anon.mixminion.message.ExitInformation;
import anon.mixminion.message.RoutingInformation;

public class MMRDescription
{
	private String m_address;
	private String m_name;
	private int m_port;
	private MyRSAPublicKey m_IdentityKey;
	private MyRSAPublicKey m_PacketKey;
	private byte[] m_digest;
	private byte[] m_keydigest;
	private boolean m_isExitNode;
	private boolean m_allowsFragmened;
	private String m_software;
	private SimpleDateFormat m_published;
	private static String m_time; //testing
	/**
	 *
	 * @param address
	 * @param name
	 * @param port
	 * @param digest
	 * @param keydigest
	 * @param exit
	 */
	public MMRDescription(String address, String name, int port, byte[] digest,
						  byte[] keydigest, boolean exit, boolean fragmented, String software, SimpleDateFormat published)
	{
		this.m_address = address;
		this.m_name = name;
		this.m_port = port;
		this.m_digest = digest;
		this.m_keydigest = keydigest;
		this.m_isExitNode = exit;
		this.m_allowsFragmened = fragmented;
		this.m_software = software;
		this.m_published = published;
	}

	/**
	 * sets the IdentityKey for this MMR
	 * @param IdentityKey
	 * IdentityKey
	 * @return
	 * true if the key is a rsa key
	 */
	public boolean setIdentityKey(byte[] identitykey)
	{
		m_IdentityKey = MyRSAPublicKey.getInstance(identitykey);
		return m_IdentityKey != null;
	}

	/**
	 * gets the IdentityKey
	 * @return
	 * IdentityKey
	 */
	public MyRSAPublicKey getIdentityKey()
	{
		return this.m_IdentityKey;
	}
	/**
	 * 
	 * @return
	 */
	public SimpleDateFormat getPublished() {
		return this.m_published;
	}

	/**
	 * sets the Packet key
	 * @param packetkey
	 * packetKey
	 * @return
	 * true if the packetKey is a rsa key
	 */
	public boolean setPacketKey(byte[] packetKey)
	{
		m_PacketKey = MyRSAPublicKey.getInstance(packetKey);
		return m_PacketKey != null;
	}

	/**
	 * gets the signing key
	 * @return
	 * signing key
	 */
	public MyRSAPublicKey getPacketKey()
	{
		return this.m_PacketKey;
	}

	/**
	 * gets the digest
	 * @return
	 * digest
	 */
	public byte[] getDigest()
	{
		return this.m_digest;
	}

	/**
	 * gets the keydigest
	 * @return
	 * digest
	 */
	public byte[] getKeyDigest()
	{
		return this.m_keydigest;
	}

//	/**
//	 * sets this server as exit node or not
//	 * @param bm_isExitNode
//	 *
//	 */
//	public void setExitNode(boolean bm_isExitNode)
//	{
//		m_isExitNode = bm_isExitNode;
//	}

	/**
	 * returns if this server is an exit node
	 * @return
	 */
	public boolean isExitNode()
	{
		return m_isExitNode;
	}

	public boolean allowsFragmented()
	{
		return m_allowsFragmened;
	}

	/**
	 * gets the address of the MMR
	 * @return
	 * address
	 */
	public String getAddress()
	{
		return this.m_address;
	}

	/**
	 * gets the name of the MMR
	 * @return
	 * name
	 */
	public String getName()
	{
		return this.m_name;
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
	 * gets the Routing Informations of this MMR
	 * @return routingInformation Vector
	 * Vector with two byte[], first is the Routing Type, Second the Routing Information
	 */
	public RoutingInformation getRoutingInformation()
	{
		RoutingInformation ri=new RoutingInformation();
		ri.m_Type=RoutingInformation.TYPE_FORWARD_TO_HOST;
		ri.m_Content = ByteArrayUtil.conc(ByteArrayUtil.inttobyte(m_port, 2), m_keydigest, m_address.getBytes());
		return ri;
	}

	/**
	 * 
	 * @return m_software
	 */
	public String getSoftwareVersion(){
		return m_software;
	}
		
	/**
	 *
	 * @param email vector with strings max 8
	 * @return vector with routingtype, routinginformation
	 */
	public static ExitInformation getExitInformation(String[] email, byte[] decodinghandle)
	{
		ExitInformation exitInformation = new ExitInformation();
		byte[] arRand= null;
		if (decodinghandle == null) 
		{
		SecureRandom rand = new SecureRandom();
			arRand = new byte[20];
		rand.nextBytes(arRand);
        arRand[0] &= 0x7F; 
		}
		else 
		{
			arRand = decodinghandle; //when using a replyblock
		}

		//if no e-mail adress is specified return a vector with a drop and log error
		if (email.length < 1)
		{
			exitInformation.m_Type = ExitInformation.TYPE_DROP;
			exitInformation.m_Content = arRand;
			LogHolder.log(LogLevel.ERR, LogType.MISC,
						  "[Building ExitInformation]: no Recipients; Packet will be dropped! ");
			return exitInformation;
		}
		else
		{
			exitInformation.m_Type = ExitInformation.TYPE_SMTP;
			//byte[] zero = new byte[1];
			String mail = (String) email[0];
			arRand = ByteArrayUtil.conc(arRand, mail.getBytes());
			//TODO For the moment it seems like mixminion only supports one recipient 
//			for (int i = 1; i < email.length; i++)
//			{
//				if (i > 8)
//				{
//					LogHolder.log(LogLevel.ERR, LogType.MISC,
//								  "[Building ExitInformation]: more than 8 Recipients; 9+ will not receive ");
//					break;
//				}
//
//				mail = (String) email[i];
//				arRand = ByteArrayUtil.conc(arRand, zero, mail.getBytes());
//			}
			exitInformation.m_Content = arRand;
						
		}

		return exitInformation;
	}

	/**Tries to parse an router specification according to the desing document.
	 * @param reader
	 * reader
	 */
	public static MMRDescription parse(LineNumberReader reader)
	{
		try
		{
			reader.readLine(); // skip Descriptor-Version

			//Nickname
			String nickname = reader.readLine().substring(10);
			//Identity
			byte[] identity = Base64.decode(reader.readLine().substring(10));
			//Digest
			byte[] digest = Base64.decode(reader.readLine().substring(8));
			//Signature
			byte[] signature = Base64.decode(reader.readLine().substring(11));

			//published
			String pd = reader.readLine().substring(11,21);
			m_time = pd;
			SimpleDateFormat published = new SimpleDateFormat("yyyy-MM-dd");
			published.parse(pd);
						
			reader.readLine(); // skip Valid after
			reader.readLine(); // skip Valid until

			//Packet Key
			byte[] packetkey = Base64.decode(reader.readLine().substring(12));

			reader.readLine(); // skip Packet Versions
			String software = reader.readLine().substring(10); // Software
			reader.readLine(); // skip Secure-Configuration
			reader.readLine(); // skip Contact
			reader.readLine(); // skip Why Insecure
			reader.readLine(); // skip Comments
			reader.readLine(); // skip [incoming/MMTP]
			reader.readLine(); // skip Incoming Version
			reader.readLine(); // skip IP

			//Hostname
			String hostname = reader.readLine().substring(10);
			//Port
			String port = reader.readLine().substring(6);
			//some routers define no port, they will be dropped
			if (port.startsWith("gest"))
			{
				return null;
			}
			//Key-Digest
			byte[] keydigest = Base64.decode(reader.readLine().substring(12));

			reader.readLine(); // skip Incoming Protocols
			reader.readLine(); // skip [Outgoing/MMTP]
			reader.readLine(); // skip Outgoing Version
			reader.readLine(); // skip Outgoing Protocols

			//exitnode,mbox and/or fragmented delivery
			String temp = "";
			boolean exitNode = false;
			boolean mbox = false;
			boolean fragmented = false;
			for (; ; )
			{
				temp = reader.readLine();
				if (temp.startsWith("[Testing]"))
				{
					break;
				}
				if (temp.startsWith("[Delivery/SMTP]"))
				{
					exitNode = true;
				}
				if (temp.startsWith("[Delivery/MBOX]"))
				{
					mbox = true;
				}
				if (temp.startsWith("[Delivery/Fragmented"))
				{
					fragmented = true;
				}
			}

			//build the new MMRDescription
			MMRDescription mmrd = new MMRDescription(hostname, nickname, Integer.parseInt(port), digest,
				keydigest, exitNode, fragmented, software, published);

			if (!mmrd.setIdentityKey(identity) || !mmrd.setPacketKey(packetkey))
			{
				return null;
			}
			else
			{
				return mmrd;
			}
		}
		catch (Throwable t)
		{
			t.printStackTrace();
			return null;
		}

	}

	//only for testing purpose
	public String toString()
	{
		return "MMRRouter: " + this.m_name + " Exitnode:" + this.m_isExitNode + " FRAGS: "+ this.allowsFragmented()+"Published: " + m_time;
	}

}
