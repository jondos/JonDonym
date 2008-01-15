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

import anon.client.MixParameters;
import anon.client.replay.ReplayTimestamp;


/** 
 * @author Stefan Lieske
 */
public class DefaultMixCipher implements IMixCipher {

  private boolean m_firstEncryptionPacket;
  
  private MixParameters m_mixParameters;
  
  private SymCipher m_symCipher;

  
  public DefaultMixCipher(MixParameters a_mixParameters, SymCipher a_symCipher) {
    m_firstEncryptionPacket = true;
    m_mixParameters = a_mixParameters;
    m_symCipher = a_symCipher;
  }

  
  public byte[] encrypt(byte[] a_packet, int a_virtualPacketLength, Vector a_sendCallbackHandlers) {
    int alreadyEncryptedBytes = 0;
    int realPacketLength = a_packet.length;
    byte[] packet = null;
    byte[] encryptedPacket = null;
    if (m_firstEncryptionPacket) {
      /* add the symmetric encryption key to the packet */
      realPacketLength = realPacketLength + m_symCipher.getKey().length;
      if (a_virtualPacketLength > realPacketLength) {
        /* we shall encrypt some more dummy bytes to keep the stream cipher synchronized */
        packet = new byte[a_virtualPacketLength];       
      }
      else {
        packet = new byte[realPacketLength];
      }
      byte[] symmetricKey = m_symCipher.getKey();
      /* ensure that m < n for RSA -> symmetric key is the first part of the
       * message and should start with a 0-bit to be sure
       */
      symmetricKey[0] = (byte)(symmetricKey[0] & 0x7f);
      /* now check for replay-detection */
      ReplayTimestamp timestampStructure = m_mixParameters.getReplayTimestamp();
      if (timestampStructure != null) {
        /* mix supports replay-detection -> modify the key of the symmetric
         * cipher with the current timestamp (we assume that the packet is
         * sent immediately after encryption)
         */
        byte[] currentTimestamp = timestampStructure.getCurrentTimestamp();
        System.arraycopy(currentTimestamp, 0, symmetricKey, symmetricKey.length - currentTimestamp.length, currentTimestamp.length);
      }
      /* all modifications are done -> initialize the cipher again */
      m_symCipher.setEncryptionKeyAES(symmetricKey);
      System.arraycopy(m_symCipher.getKey(), 0, packet, 0, m_symCipher.getKey().length);
      System.arraycopy(a_packet, 0, packet, m_symCipher.getKey().length, a_packet.length);
      encryptedPacket = new byte[packet.length];
      /* do asymmetric encryption on the first part of the packet */
      alreadyEncryptedBytes = m_mixParameters.getMixCipher().encrypt(packet, 0, encryptedPacket, 0);
      m_firstEncryptionPacket = false;
    }
    else {
      /* do only symmetric encryption */
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
    /* do symmetric encryption */
    m_symCipher.encryptAES(packet, alreadyEncryptedBytes, encryptedPacket, alreadyEncryptedBytes, packet.length - alreadyEncryptedBytes);
    if (realPacketLength < encryptedPacket.length) {
      /* cut off the dummy bytes */
      byte[] tempPacket = encryptedPacket;
      encryptedPacket = new byte[realPacketLength];
      System.arraycopy(tempPacket, 0, encryptedPacket, 0, realPacketLength);
    }   
    return encryptedPacket;
  }

  public void decrypt(byte[] a_packet) {
    m_symCipher.encryptAES2(a_packet);
  }

  public int getNextPacketEncryptionOverhead() {
    int overhead = 0;
    if (m_firstEncryptionPacket) {
      overhead = m_symCipher.getKey().length;
    }
    return overhead;
  }

}
