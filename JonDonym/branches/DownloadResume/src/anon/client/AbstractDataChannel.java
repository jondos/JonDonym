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

import java.io.IOException;

import anon.client.crypto.MixCipherChain;
import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;


/**
 * @author Stefan Lieske
 */
public abstract class AbstractDataChannel extends AbstractChannel {

  private final static short FLAG_CHANNEL_DUMMY = 0x0010;


  private MixCipherChain m_mixCipherChain;

  private AbstractDataChain m_parentDataChain;

  private InternalChannelMessageQueue m_channelMessageQueue;


  public AbstractDataChannel(int a_channelId, Multiplexer a_parentMultiplexer, AbstractDataChain a_parentDataChain, MixCipherChain a_mixCipherChain) {
    super(a_channelId, a_parentMultiplexer);
    m_parentDataChain = a_parentDataChain;
    m_mixCipherChain = a_mixCipherChain;
    m_channelMessageQueue = new InternalChannelMessageQueue();
  }


  public InternalChannelMessageQueue getChannelMessageQueue() {
    return m_channelMessageQueue;
  }

  public void processReceivedPacket(MixPacket a_mixPacket) {
    /* do decryption */
    m_mixCipherChain.decryptPacket(a_mixPacket.getPayloadData());
    /* catch all dummies (shouldn't be used any more in the future) */
    if ((a_mixPacket.getChannelFlags() & FLAG_CHANNEL_DUMMY) == FLAG_CHANNEL_DUMMY) {
      /* we haven't ordered any dummies -> normally we shouldn't receive any
       */
      LogHolder.log(LogLevel.INFO, LogType.NET, "AbstractDataChannel: processReceivedPacket(): Catched an unexpected dummy-paket on channel '" + Integer.toString(a_mixPacket.getChannelId()) + "'.");
    }
    else {
      /* further handling is done by the channel-implementation */
      handleReceivedPacket(a_mixPacket);
    }
  }

  public int getNextPacketRecommandedOutputBlocksize() {
    int recommandedSize = 0;
    synchronized (m_mixCipherChain) {
      recommandedSize = MixPacket.getPayloadSize() - m_mixCipherChain.getNextPacketEncryptionOverhead();
    }
    return recommandedSize;
  }


  protected void createAndSendMixPacket(DataChainSendOrderStructure a_order, short a_channelFlags) {
    MixPacket packet = createEmptyMixPacket();
    packet.setChannelFlags(a_channelFlags);
    synchronized (m_mixCipherChain) {
      a_order.setChannelCell(new byte[packet.getPayloadData().length - m_mixCipherChain.getNextPacketEncryptionOverhead()]);
      /* initalize the channel cell with payload from the mixpacket (payload
       * should be initialized with random bytes -> channel cell will be
       * initialized with random bytes) -> we take the payload from the end of
       * the mixpacket for initialization
       */
      System.arraycopy(packet.getPayloadData(), packet.getPayloadData().length - a_order.getChannelCell().length, a_order.getChannelCell(), 0, a_order.getChannelCell().length);
      if (a_order.getOrderData() != null) {
        /* messages without order-data are channel-internal and needs no interaction with
         * the parent chain
         */
        m_parentDataChain.createPacketPayload(a_order);
      }
      byte[] encryptedPacketPayload = m_mixCipherChain.encryptPacket(a_order.getChannelCell(), packet.getPayloadData().length, packet.getSendCallbackHandlers());
      System.arraycopy(encryptedPacketPayload, 0, packet.getPayloadData(), 0, packet.getPayloadData().length);
      try {
        sendPacket(packet);
      }
      catch (IOException e) {
        a_order.setThrownException(e);
      }
    }
    a_order.processingDone();
  }


  public abstract boolean processSendOrder(DataChainSendOrderStructure a_order);

  public abstract void organizeChannelClose() throws IOException;


  protected abstract void handleReceivedPacket(MixPacket a_mixPacket);

}
