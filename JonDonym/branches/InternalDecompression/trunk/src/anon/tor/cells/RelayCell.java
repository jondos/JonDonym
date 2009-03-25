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
package anon.tor.cells;

import org.bouncycastle.crypto.digests.SHA1Digest;

import anon.util.ByteArrayUtil;
import anon.crypto.MyAES;

/**
 * @author stefan
 *
 */
public class RelayCell extends Cell
{

	public final static byte RELAY_BEGIN = 1;
	public final static byte RELAY_DATA = 2;
	public final static byte RELAY_END = 3;
	public final static byte RELAY_CONNECTED = 4;
	public final static byte RELAY_SENDME = 5;
	public final static byte RELAY_EXTEND = 6;
	public final static byte RELAY_EXTENDED = 7;
	public final static byte RELAY_TRUNCATE = 8;
	public final static byte RELAY_TRUNCATED = 9;
	public final static byte RELAY_DROP = 10;
	public final static byte RELAY_RESOLVE = 11;
	public final static byte RELAY_RESOLVED = 12;

	private byte m_relayCommand;
	private Integer m_streamID;
	private boolean m_digestGenerated;

	/**
	 * Constructor for a relay cell
	 */
	public RelayCell()
	{
		super(3);
		m_streamID=new Integer(0);
	}

	/**
	 * Constructor for a relay cell
	 *
	 * @param circID
	 * circID
	 */
	public RelayCell(int circID)
	{
		super(3, circID);
		m_streamID=new Integer(0);
		this.m_digestGenerated = false;
	}

	/**
	 * Constructor for a relay cell
	 *
	 * @param circID
	 * circID
	 * @param payload
	 * payload
	 * @param offset
	 * offset
	 */
	public RelayCell(int circID, byte[] payload, int offset)
	{
		super(3, circID, payload, offset);
		m_relayCommand = payload[0];
		m_streamID =new Integer(( (payload[3] & 0xFF) << 8) | (payload[4] & 0xFF));
		m_digestGenerated = false;
	}

	/**
	 * Constructor
	 * @param circID
	 * circID
	 * @param relaycommand
	 * relaycommand
	 * @param streamid
	 * streamID
	 * @param data
	 * data
	 */
	public RelayCell(int circID, byte relaycommand, int streamid, byte[] data)
	{
		super(3, circID, createPayload(relaycommand, streamid, data));
		this.m_relayCommand = relaycommand;
		this.m_streamID = new Integer(streamid);
		this.m_digestGenerated = false;
	}

	/**
	 * gets the command of the relay cell
	 * @return
	 * command
	 */
	public byte getRelayCommand()
	{
		return this.m_relayCommand;
	}

	/**
	 * gets the streamID of the relay cell
	 * @return
	 * streamID
	 */
	public Integer getStreamID()
	{
		return m_streamID;
	}

	/**
	 * generates the digest for the relay cell
	 * @param digest
	 * digest
	 */
	public void generateDigest(SHA1Digest digest)
	{
		if (!this.m_digestGenerated)
		{
			digest.update(this.m_payload, 0, this.m_payload.length);
			SHA1Digest sha = new SHA1Digest(digest);
			byte[] d = new byte[sha.getDigestSize()];
			sha.doFinal(d, 0);
			for (int i = 0; i < 4; i++)
			{
				this.m_payload[i + 5] = d[i];
			}
			this.m_digestGenerated = true;
		}
	}

	/**
	 * checks if the digest of the relay cell is correct
	 * @param digest
	 * digest
	 * @throws Exception
	 * if an error occurs
	 */
	public void checkDigest(SHA1Digest digest) throws Exception
	{
		digest.update(this.m_payload, 0, 5);
		digest.update(new byte[4], 0, 4);
		digest.update(this.m_payload, 9, this.m_payload.length - 9);
		SHA1Digest sha = new SHA1Digest(digest);
		byte[] d = new byte[sha.getDigestSize()];
		sha.doFinal(d, 0);
		for (int i = 0; i < 4; i++)
		{
			if (this.m_payload[i + 5] != d[i])
			{
				throw new Exception("Wrong Digest detected");
			}
		}
		this.m_digestGenerated = true;
	}

	/**
	 * encode the cell
	 * @param engine
	 * a cipher
	 */
	public void doCryptography(MyAES engine) throws Exception
	{
		byte[] data = new byte[m_payload.length];
		engine.processBytesCTR(m_payload, 0, data, 0, CELL_PAYLOAD_SIZE);
		m_payload = data;
		m_relayCommand = m_payload[0];
		m_streamID = new Integer(( (m_payload[3] & 0xFF) << 8) | (m_payload[4] & 0xFF));
	}

	private static byte[] createPayload(byte relaycommand, int streamid, byte[] data)
	{
		byte[] b = new byte[]
			{
			relaycommand, 0x00, 0x00, }; //relaycommand + 2bytes Recognized
		b = ByteArrayUtil.conc(b, ByteArrayUtil.inttobyte(streamid, 2), new byte[4]); //streamid + 4bytes digest
		if (data == null)
		{
			data = new byte[498];
		}
		if (data.length < 499)
		{
			b = ByteArrayUtil.conc(b, ByteArrayUtil.inttobyte(data.length, 2), data); //length + data
		}
		else //copy only the first 498 bytes into the payload and forget the rest
		{
			b = ByteArrayUtil.conc(b, ByteArrayUtil.inttobyte(498, 2), ByteArrayUtil.copy(data, 0, 498)); //length+ data
		}
		return b;
	}

	public byte[] getCellData()
	{
		if (m_digestGenerated)
		{
			return super.getCellData();
		}
		else
		{
			return null;
		}
	}

	/**
	 * gets the payload of the relay cell
	 * @return
	 * payload
	 */
	public byte[] getRelayPayload()
	{
		int len = (m_payload[9] & 0x00FF);
		len <<= 8;
		len |= (m_payload[10] & 0x00FF);
		return ByteArrayUtil.copy(m_payload, 11, len);
	}
}
