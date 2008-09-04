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
 * Created on May 9, 2004
 */
package anon.tor;

import java.io.IOException;

import anon.util.ByteArrayUtil;
import anon.ErrorCodes;
import logging.LogHolder;
import logging.LogType;
import logging.LogLevel;
import java.util.Hashtable;

/**
 * @author stefan
 */
public class TorSocksChannel extends TorChannel
{

	private final static int SOCKS_WAIT_FOR_VERSION = 0;
	private final static int SOCKS5_WAIT_FOR_METHODS = 1;
	private final static int SOCKS5_WAIT_FOR_REQUEST = 2;
	private final static int SOCKS4_WAIT_FOR_REQUEST = 3;
	private final static int DATA_MODE = 4;

	private final static int SOCKS_5 = 5;
	private final static int SOCKS_4 = 4;
	private int m_state;
	private int m_version;
	private byte[] m_data; //buffer for socks protocol headers
	private Tor m_Tor;

	public TorSocksChannel(Tor tor) throws IOException
	{
		m_state = SOCKS_WAIT_FOR_VERSION;
		m_data = null;
		m_Tor = tor;
		LogHolder.log(LogLevel.DEBUG, LogType.TOR, "new TorSocksChannel() - object created.");
	}

	/** Called if some bytes should be send over this Sock channel
	 *
	 */
	//Depending on the current stat of the protocol we have to proccess protocols headers. After
	//this we can transparently forward the data
	protected void send(byte[] arg0, int len) throws IOException
	{
		switch (m_state)
		{
			case SOCKS_WAIT_FOR_VERSION:
			{ //initial state
				state_WaitForVersion(arg0, len);
				break;
			}

			case SOCKS5_WAIT_FOR_METHODS:
			{ //auth mehtods (version5)
				state_WaitForMethods(arg0, len);
				break;
			}
			case SOCKS5_WAIT_FOR_REQUEST:
			{ //v5 waiting for a request....
				state_WaitForRequest_Socks5(arg0, len);
				break;
			}
			case SOCKS4_WAIT_FOR_REQUEST:
			{ //v4 waiting for a request....
				//version number is already consumed...
				state_WaitForRequest_Socks4(arg0, len);
				break;
			}

			case DATA_MODE:
			{
				super.send(arg0, len);
				break;
			}
			default:
			{
				throw new IOException("illegal status");
			}
		}

	}

	private void state_WaitForVersion(byte[] arg0, int len) throws IOException
	{
		if (arg0 != null && len > 0)
		{
			m_data = ByteArrayUtil.conc(m_data, arg0, len);
		}
		if (m_data.length > 1)
		{
			//m_data[0]=Version (0x05||0x04)
			m_version = m_data[0];
			if (m_version != SOCKS_5 && m_version != SOCKS_4)
			{
				close();
				throw new IOException("Wrong Sock Protocol number");
			}
			m_data = ByteArrayUtil.copy(m_data, 1, m_data.length - 1);
			if (m_version == SOCKS_5)
			{
				m_state = SOCKS5_WAIT_FOR_METHODS;
			}
			else
			{
				m_state = SOCKS4_WAIT_FOR_REQUEST;
			}
			send(null, 0);
		}
	}

	private void state_WaitForMethods(byte[] arg0, int len) throws IOException
	{
		if (arg0 != null && len > 0)
		{
			m_data = ByteArrayUtil.conc(m_data, arg0, len);
		}
		if (m_data.length > 1)
		{
			//m_data[0]=Number of Methods
			//m_data[1-x]=Methods
			int nrOfMethods = (m_data[0] & 0xFF);
			int length = nrOfMethods + 1;
			if (m_data.length >= length)
			{
				boolean methodFound = false;
				byte[] socksAnswer = null;
				for (int i = 0; i < nrOfMethods; i++)
				{
					if (m_data[i + 1] == 0)
					{
						methodFound = true;
						socksAnswer = new byte[]
							{
							0x05, 0x00};
						m_state = SOCKS5_WAIT_FOR_REQUEST;
						break;
					}
				}
				if (!methodFound)
				{
					socksAnswer = new byte[]
						{
						0x05, (byte) 0xFF};
				}
				super.recv(socksAnswer, 0, socksAnswer.length);
				if (!methodFound)
				{
					//todo close this channel
					return;
				}
				m_data = ByteArrayUtil.copy(m_data, length, m_data.length - length);
				if (m_data.length > 0)
				{
					send(null, 0);
				}
			}

		}
	}

	private void state_WaitForRequest_Socks4(byte[] arg0, int len) throws IOException
	{
		if (arg0 != null && len > 0)
		{
			m_data = ByteArrayUtil.conc(m_data, arg0, len);
		}
		int requestType;
		if (m_data.length > 0)
		{
			requestType = m_data[0]; // 1 = connect 2= bind
		}
		else
		{
			return;
		}
		if (requestType != 1) //connect request type==1
		{
			///@todo: close etc.
			//command not supported
			byte[] socksAnswer = new byte[]
				{
				0x00, 91, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00};
			m_data = null;
			super.recv(socksAnswer, 0, socksAnswer.length);
			return;
		}
		if (m_data.length >= 8)
		{
			byte[] socksAnswer = null;
			int port = 0;
			String addr = null;
			int consumedBytes = 1;
			//IP V4
			addr = Integer.toString(m_data[3] & 0xFF) + "." +
				Integer.toString(m_data[4] & 0xFF) + "." +
				Integer.toString(m_data[5] & 0xFF) + "." +
				Integer.toString(m_data[6] & 0xFF);
			port = ( (m_data[1] & 0xFF) << 8) | (m_data[2] & 0xFF);
			consumedBytes += 6;
			//skip userid ('0' terminated string)
			int i = 7;
			while (i < m_data.length && m_data[i] != 0)
			{
				i++;
				consumedBytes++;
			}
			if (m_data[i] != 0)
			{
				return;
			}
			consumedBytes++;
			//check vor SOCKs 4a --> IP starts with "0.0.0" and Domain-Name follows
			if (addr.startsWith("0.0.0"))
			{
				i++;
				StringBuffer sb = new StringBuffer();
				while (i < m_data.length && m_data[i] != 0)
				{
					sb.append( (char) m_data[i]);
					i++;
					consumedBytes++;
				}
				if (m_data[i] != 0)
				{
					return;
				}
				consumedBytes++;
				addr = sb.toString();
			}
			boolean connected = false;
			setDoNotCloseChannelOnErrorDuringConnect(true);
			//	connect
			int tries = 0;
			Hashtable excludeCircuits=new Hashtable();
			while (!connected && tries < 3)
			{
				connected = true;
				Circuit circ=null;
				try
				{
					circ = m_Tor.getCircuitForDestination(addr, port,excludeCircuits);
					if (circ == null) //connection error
					{
						socksAnswer = new byte[]
							{
							0x00, 91, 0x00, 0, 0, 0, 0, 0};
						super.recv(socksAnswer, 0, socksAnswer.length);
						this.closedByPeer();
						return;
					}
					if (circ.connectChannel(this, addr, port) != ErrorCodes.E_SUCCESS)
					{
						connected = false;
						excludeCircuits.put(circ,circ);
					}
				}
				catch (IOException ex)
				{
					if(circ!=null)
						excludeCircuits.put(circ,circ);
					connected = false;
				}
				//init();
				tries++;
			}
			if (!connected)
			{
				socksAnswer = new byte[]
						{
						0x00, 91, 0x00, 0, 0, 0, 0, 0};
					super.recv(socksAnswer, 0, socksAnswer.length);
				closedByPeer();
				return;
			}

			socksAnswer = new byte[]
				{
				0x00, 90, 0x00, 0, 0, 0, 0, 0};
			super.recv(socksAnswer, 0, socksAnswer.length);
			m_data = ByteArrayUtil.copy(m_data, consumedBytes, m_data.length - consumedBytes);
			m_state = DATA_MODE;
			if (m_data.length > 0)
			{
				send(m_data, m_data.length);
				m_data = null;
			}
		}
	}

	private void state_WaitForRequest_Socks5(byte[] arg0, int len) throws IOException
	{
		if (arg0 != null && len > 0)
		{
			m_data = ByteArrayUtil.conc(m_data, arg0, len);
		}
		if (m_data.length > 6)
		{
			byte[] socksAnswer = null;
			int port = 0;
			String addr = null;
			int requestType = m_data[1];
			int addrType = m_data[3];
			int consumedBytes = 0;
			if (requestType != 1) //connect request type==1
			{
				//todo: close etc.
				//command not supported
				socksAnswer = ByteArrayUtil.conc(new byte[]
												 {0x05, 0x07, 0x00}
												 , ByteArrayUtil.copy(this.m_data, 3, this.m_data.length - 3));
				m_data = null;
				super.recv(socksAnswer, 0, socksAnswer.length);
				return;
			}
			switch (addrType)
			{
				case 1: //IP V4
				{
					if (m_data.length > 9)
					{
						addr = Integer.toString(m_data[4] & 0xFF) + "." +
							Integer.toString(m_data[5] & 0xFF) + "." +
							Integer.toString(m_data[6] & 0xFF) + "." +
							Integer.toString(m_data[7] & 0xFF);
						port = ( (m_data[8] & 0xFF) << 8) | (m_data[9] & 0xFF);
						consumedBytes = 10;
					}
					break;
				}
				case 3: //Domain Name
				{
					int length = m_data[4] & 0xFF;
					if (m_data.length >= (7 + length))
					{
						addr = new String(m_data, 5, length);
						port = ( (m_data[5 + length] & 0xFF) << 8) | (m_data[6 + length] & 0xFF);
						consumedBytes = length + 7;
					}
					break;
				}
				default:
				{
					//addresstype not supportet
					socksAnswer = ByteArrayUtil.conc(new byte[]
						{0x05, 0x08, 0x00}
						, ByteArrayUtil.copy(m_data, 3, m_data.length - 3));
					super.recv(socksAnswer, 0, socksAnswer.length);
					m_data = null;
					//todo close
				}
			}

			if (addr != null) //we found an address
			{
				Hashtable excludeCircuits=new Hashtable();
				boolean bChannelCreated = false;
				setDoNotCloseChannelOnErrorDuringConnect(true);
				for (int tries = 0; tries < 3; tries++) //try 3 times to esatblish a channel trough the tor network
				{
					Circuit circ;
					circ = m_Tor.getCircuitForDestination(addr, port,excludeCircuits);
					if (circ == null) //circuit creation error --> because the circuit establishment
					//proceudre itselves tries 5 times we can give up here directly...
					{
						break;
					}
					//	connect
					if (circ.connectChannel(this, addr, port) == ErrorCodes.E_SUCCESS)
					{
						bChannelCreated = true;
						break;
					}
					excludeCircuits.put(circ,circ);
					//init();
				}
				if (!bChannelCreated)
				{ //ew were not able to establish a channel - give up and tell the peer
					socksAnswer = ByteArrayUtil.conc(new byte[]
						{0x05, 0x01, 0x00}
						, ByteArrayUtil.copy(m_data, 3, consumedBytes - 3));
					super.recv(socksAnswer, 0, socksAnswer.length);
					closedByPeer();
					return;
				}
				socksAnswer = ByteArrayUtil.conc(new byte[]
												 {0x05, 0x00, 0x00}
												 , ByteArrayUtil.copy(m_data, 3, consumedBytes - 3));
				super.recv(socksAnswer, 0, socksAnswer.length);
				m_data = ByteArrayUtil.copy(m_data, consumedBytes, m_data.length - consumedBytes);
				m_state = DATA_MODE;
				if (m_data.length > 0)
				{
					send(m_data, m_data.length);
					m_data = null;
				}
			}
		}
	}

}
