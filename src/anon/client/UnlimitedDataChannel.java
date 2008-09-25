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

import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;
import anon.client.crypto.MixCipherChain;


/**
 * @author Stefan Lieske
 */
public class UnlimitedDataChannel extends AbstractDataChannel {

  private static final short FLAG_CHANNEL_DATA  = 0x0000;

  private static final short FLAG_CHANNEL_CLOSE = 0x0001;

  private static final short FLAG_CHANNEL_OPEN  = 0x0008;


  private Object m_internalSynchronization;

  private boolean m_channelOpened;


  public UnlimitedDataChannel(int a_channelId, Multiplexer a_parentMultiplexer, AbstractDataChain a_parentDataChain, MixCipherChain a_mixCipherChain) {
    super(a_channelId, a_parentMultiplexer, a_parentDataChain, a_mixCipherChain);
    m_internalSynchronization = new Object();
    m_channelOpened = false;
  }


  public void organizeChannelClose() {
    synchronized (m_internalSynchronization) {
      if (m_channelOpened) {
        /* send only a close-message, if a open-message was sent */
        DataChainSendOrderStructure closeOrder = new DataChainSendOrderStructure(null);
        createAndSendMixPacket(closeOrder, FLAG_CHANNEL_CLOSE);
        synchronized (closeOrder.getSynchronizationObject()) {
          if (!closeOrder.isProcessingDone()) {
            try {
              closeOrder.getSynchronizationObject().wait();
            }
            catch (InterruptedException e) {
            }
          }
        }
      }
      /* delete the channel from the channel-table -> all further send-calls will
       * fail with an IOException
       */
      deleteChannel();
      /* send a close-channel message via the message-queue */
      getChannelMessageQueue().addChannelMessage(new InternalChannelMessage(InternalChannelMessage.CODE_CHANNEL_CLOSED, null));
    }
  }

  public boolean processSendOrder(DataChainSendOrderStructure a_order) {
    synchronized (m_internalSynchronization) {
      if (!m_channelOpened) {
        createAndSendMixPacket(a_order, FLAG_CHANNEL_OPEN);
        m_channelOpened = true;
      }
      else {
        createAndSendMixPacket(a_order, FLAG_CHANNEL_DATA);
      }
    }
    return true;
  }

  public void multiplexerClosed() 
  {
    LogHolder.log(LogLevel.ERR, LogType.NET, "UnlimitedDataChannel: multiplexerClosed(): Multiplexer closed while channel was still active.");
    /* send an exception and a channel-close message */
    getChannelMessageQueue().addChannelMessage(new InternalChannelMessage(InternalChannelMessage.CODE_CHANNEL_EXCEPTION, null));
    getChannelMessageQueue().addChannelMessage(new InternalChannelMessage(InternalChannelMessage.CODE_CHANNEL_CLOSED, null));
  }


  protected void handleReceivedPacket(MixPacket a_mixPacket) {
    if ((a_mixPacket.getChannelFlags() & FLAG_CHANNEL_CLOSE) == FLAG_CHANNEL_CLOSE) {
      /* send a close-channel message via the message-queue */
      getChannelMessageQueue().addChannelMessage(new InternalChannelMessage(InternalChannelMessage.CODE_CHANNEL_CLOSED, a_mixPacket.getPayloadData()));
      /* if close bit is set -> close the channel */
      deleteChannel();
    }
    else {
      /* it's a data-message */
      getChannelMessageQueue().addChannelMessage(new InternalChannelMessage(InternalChannelMessage.CODE_PACKET_RECEIVED, a_mixPacket.getPayloadData()));
    }
  }

}
