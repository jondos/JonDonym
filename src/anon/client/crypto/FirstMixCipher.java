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
package anon.client.crypto;

import java.util.Vector;

import anon.client.ISendCallbackHandler;
import anon.client.MixPacket;


/** 
 * @author Stefan Lieske
 */
public class FirstMixCipher implements IMixCipher {

  private boolean m_firstEncryptionPacket;
    
  private SymCipher m_mixCipher;
  
  private SymCipher m_channelSymCipher;
  

  private class MixEncryptionHandler implements ISendCallbackHandler {
    
    private SymCipher m_mixStreamCipher;
    
    private int m_bytesToEncrypt;
    
    public MixEncryptionHandler(SymCipher a_mixStreamCipher, int a_bytesToEncrypt) {
      m_mixStreamCipher = a_mixStreamCipher;
      m_bytesToEncrypt = a_bytesToEncrypt;
    }
    
    public void finalizePacket(MixPacket a_mixPacket) {
      /* it's a little bit nasty because we have to know the position to encrypt in the
       * MixPacket (currently the first bytes of the payload data)
       */
      m_mixStreamCipher.encryptAES(a_mixPacket.getPayloadData(), 0, a_mixPacket.getPayloadData(), 0, m_bytesToEncrypt);
    }
  }
  
  
  public FirstMixCipher(SymCipher a_mixCipher, SymCipher a_channelSymCipher) {
    m_firstEncryptionPacket = true;
    m_mixCipher = a_mixCipher;
    m_channelSymCipher = a_channelSymCipher;
  }
  
  
  public byte[] encrypt(byte[] a_packet, int a_virtualPacketLength, Vector a_sendCallbackHandlers) {
    int alreadyEncryptedBytes = 0;
    int realPacketLength = a_packet.length;
    byte[] packet = null;
    byte[] encryptedPacket = null;
    if (m_firstEncryptionPacket) {
      /* add the symmetric encryption key of the channel-cipher to the packet */
      realPacketLength = realPacketLength + m_channelSymCipher.getKey().length;
      if (a_virtualPacketLength > realPacketLength) {
        /* we shall encrypt some more dummy bytes to keep the stream cipher synchronized */
        packet = new byte[a_virtualPacketLength];       
      }
      else {
        packet = new byte[realPacketLength];
      }
      System.arraycopy(m_channelSymCipher.getKey(), 0, packet, 0, m_channelSymCipher.getKey().length);
      System.arraycopy(a_packet, 0, packet, m_channelSymCipher.getKey().length, a_packet.length);
      encryptedPacket = new byte[packet.length];
      /* symmetric encryption on the first part (channel key) of the packet is done later
       * with the mix-cipher (because it depends on the position of the packet in the
       * mix-data-stream) -> for now copy only the plaintext and create a callback
       * handler for later encryption
       */
      System.arraycopy(packet, 0, encryptedPacket, 0, m_channelSymCipher.getKey().length);
      alreadyEncryptedBytes = m_channelSymCipher.getKey().length;
      a_sendCallbackHandlers.addElement(new MixEncryptionHandler(m_mixCipher, alreadyEncryptedBytes));
      m_firstEncryptionPacket = false;
    }
    else {
      /* do only symmetric encryption with the channel-cipher */
      if (a_virtualPacketLength > realPacketLength) {
        /* we shall encrypt some more dummy bytes to keep the stream cipher synchronized */
        packet = new byte[a_virtualPacketLength];       
      }
      else {
        packet = new byte[realPacketLength];
      }
      System.arraycopy(a_packet, 0, packet, 0, a_packet.length);
      encryptedPacket = new byte[packet.length];
    }
    /* do symmetric encryption for the channel */
    m_channelSymCipher.encryptAES(packet, alreadyEncryptedBytes, encryptedPacket, alreadyEncryptedBytes, packet.length - alreadyEncryptedBytes);
    if (realPacketLength < encryptedPacket.length) {
      /* cut off the dummy bytes */
      byte[] tempPacket = encryptedPacket;
      encryptedPacket = new byte[realPacketLength];
      System.arraycopy(tempPacket, 0, encryptedPacket, 0, realPacketLength);
    }   
    return encryptedPacket;
  }

  public void decrypt(byte[] a_packet) {
    m_channelSymCipher.encryptAES2(a_packet);
  }

  public int getNextPacketEncryptionOverhead() {
    int overhead = 0;
    if (m_firstEncryptionPacket) {
      overhead = m_channelSymCipher.getKey().length;
    }
    return overhead;
  }

}
