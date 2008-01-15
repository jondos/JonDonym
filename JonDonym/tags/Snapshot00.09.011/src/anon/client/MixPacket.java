/*
 * Copyright (c) 2006, The JAP-Team
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *   - Redistributions of source code must retain the above copyright notice,
 *     this list of conditions and the following disclaimer.
 *
 *   - Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer in the
 *     documentation and/or other materials provided with the distribution.
 *
 *   - Neither the name of the University of Technology Dresden, Germany nor
 *     the names of its contributors may be used to endorse or promote
 *     products derived from this software without specific prior written
 *     permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package anon.client;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.Vector;

import anon.client.crypto.SymCipher;


/** 
 * @author Stefan Lieske
 */
public class MixPacket {
  
  private static final int PACKET_SIZE = 998;
  
  private static SecureRandom ms_secureRandom;
  
    
  private int m_channelId;
  
  private short m_channelFlags;
  
  private byte[] m_payloadData;
  
  private Vector m_sendCallbackHandlers;
  
  
  public static int getPacketSize() {
    return PACKET_SIZE;
  }
  
  public static int getPayloadSize() {
    return PACKET_SIZE - 6;
  }

  
  static {
    /* initialize the random-bytes generator */
    ms_secureRandom = new SecureRandom();
  }
  
  
  public MixPacket(InputStream a_inputStream, SymCipher a_inputStreamCipher) throws IOException {
    m_sendCallbackHandlers = new Vector();
    /* read the packet from the origin stream */
    byte[] rawPacket = new byte[PACKET_SIZE];
    DataInputStream sourceStream = new DataInputStream(a_inputStream);
    sourceStream.readFully(rawPacket);
    /* do stream-decryption */
    if (a_inputStreamCipher != null) {
      a_inputStreamCipher.encryptAES(rawPacket, 0, rawPacket, 0, 16);
    }
    /* read channel-id and channel-flags from the packet */
    DataInputStream packetDataStream = new DataInputStream(new ByteArrayInputStream(rawPacket, 0, 6));
    m_channelId = packetDataStream.readInt();
    m_channelFlags = packetDataStream.readShort();
    m_payloadData = new byte[rawPacket.length - 6];
    System.arraycopy(rawPacket, 6, m_payloadData, 0, rawPacket.length - 6);
  }
  

  public MixPacket(int a_channelId) {
    m_sendCallbackHandlers = new Vector();
    m_channelId = a_channelId;
    m_channelFlags = 0;
    m_payloadData = new byte[PACKET_SIZE - 6];
    /* initialize payload with random bytes */
    ms_secureRandom.nextBytes(m_payloadData);
  }
  
  
  public int getChannelId() {
    return m_channelId;
  }
  
  public short getChannelFlags() {
    return m_channelFlags;
  }
  
  public void setChannelFlags(short a_channelFlags) {
    m_channelFlags = a_channelFlags;
  }
  
  public byte[] getPayloadData() {
    return m_payloadData;
  }
  
  public byte[] getRawPacket() {
    ByteArrayOutputStream packetHeaderStream = new ByteArrayOutputStream();
    DataOutputStream packetHeaderDataStream = new DataOutputStream(packetHeaderStream);
    try {
      packetHeaderDataStream.writeInt(m_channelId);
      packetHeaderDataStream.writeShort(m_channelFlags);
      packetHeaderDataStream.flush();
    }
    catch (IOException e) {
      /* cannot occur */
    }
    byte[] rawPacket = new byte[PACKET_SIZE];
    byte[] packetHeader = packetHeaderStream.toByteArray();
    System.arraycopy(packetHeader, 0, rawPacket, 0, packetHeader.length);
    System.arraycopy(m_payloadData, 0, rawPacket, packetHeader.length, m_payloadData.length);
    return rawPacket;
  }
  
  public Vector getSendCallbackHandlers() {
    return m_sendCallbackHandlers;
  }
  
}
