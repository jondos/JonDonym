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
package anon.crypto.tinytls;

/** This is the TLS plaintext record*/
final public class TLSPlaintextRecord extends AbstractTLSRecord
{
	public final static int CONTENTTYPE_HANDSHAKE = 22;

	public final static int HEADER_LENGTH = 5;

	public final static int MAX_PAYLOAD_SIZE = 0x4000;//max 2^14 bytes for a TLS.plaintext record

	private int m_nextHandshakeRecordOffset;

	/**
	 * Constructor
	 *
	 */
	public TLSPlaintextRecord()
	{
		m_Header = new byte[HEADER_LENGTH];
		m_Header[1] = TinyTLS.PROTOCOLVERSION[0];
		m_Header[2] = TinyTLS.PROTOCOLVERSION[1];
		m_Data = new byte[MAX_PAYLOAD_SIZE]; //max 2^14 bytes for a TLS.plaintext record
		m_dataLen = 0;
		m_Type = 0;
		m_nextHandshakeRecordOffset = 0;
	}

	public void clean()
	{
		m_dataLen = 0;
		m_Type = 0;
		m_nextHandshakeRecordOffset = 0;
	}

	public int getHeaderLength()
	{
		return HEADER_LENGTH;
	}

	public int getMaxPayloadSize()
	{
		return MAX_PAYLOAD_SIZE;
	}

	/**
	 * sets the length of the tls record
	 * @param len
	 * length
	 */
	public void setLength(int len)
	{
		m_dataLen = len;
		m_Header[3] = (byte) ( (len >> 8) & 0x00FF);
		m_Header[4] = (byte) ( (len) & 0x00FF);
	}

	/** Retruns true if this record contains in the payload some more handshake records.*/
	public boolean hasMoreHandshakeRecords()
	{
		return (m_Type == CONTENTTYPE_HANDSHAKE) && (m_nextHandshakeRecordOffset < m_dataLen);
	}

	/** Retruns the next handshake record from inside the payload*/
	public TLSHandshakeRecord getNextHandshakeRecord()
	{
		TLSHandshakeRecord handshake = new TLSHandshakeRecord(m_Data, m_nextHandshakeRecordOffset);
		m_nextHandshakeRecordOffset += handshake.getLength() + TLSHandshakeRecord.HEADER_LENGTH;
		return handshake;
	}

}
