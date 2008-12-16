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

import anon.client.crypto.MixCipherChain;
import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;


/**
 * @author Stefan Lieske
 */
public class SimulatedLimitedDataChannel extends AbstractDataChannel implements Runnable {

  private static final short FLAG_CHANNEL_DATA  = 0x0000;

  private static final short FLAG_CHANNEL_CLOSE = 0x0001;

  private static final short FLAG_CHANNEL_OPEN  = 0x0008;


  private Object m_internalSynchronization;

  private boolean m_channelOpened;

  private int m_downstreamPackets;

  private long m_channelTimeout;

  private int m_receivedPackets;

  Thread m_timeoutSupervisionThread;

  Object m_timeoutSynchronization;

  private volatile boolean m_channelClosed;


  public SimulatedLimitedDataChannel(int a_channelId, Multiplexer a_parentMultiplexer, AbstractDataChain a_parentDataChain, MixCipherChain a_mixCipherChain, int a_downstreamPackets, long a_channelTimeout) {
    super(a_channelId, a_parentMultiplexer, a_parentDataChain, a_mixCipherChain);
    m_internalSynchronization = new Object();
    m_channelOpened = false;
    m_downstreamPackets = a_downstreamPackets;
    m_channelTimeout = a_channelTimeout;
    m_receivedPackets = 0;
    m_timeoutSupervisionThread = null;
    m_timeoutSynchronization = new Object();
    m_channelClosed = false;
  }


  public void organizeChannelClose() {
    synchronized (m_internalSynchronization) {
      if (!m_channelOpened) {
        /* we can only do something, if we never sent a message via the
         * channel -> in the other case we have to wait until we receive the
         * CHANNEL_CLOSE message
         */
        deleteChannel();
        /* send a close-channel message via the message-queue */
        getChannelMessageQueue().addChannelMessage(new InternalChannelMessage(InternalChannelMessage.CODE_CHANNEL_CLOSED, null));
      }
    }
  }

  public boolean processSendOrder(DataChainSendOrderStructure a_order) {
    synchronized (m_internalSynchronization) {
      if (!m_channelOpened) {
        /* prepare to send the first packet on this channel */
        synchronized (m_timeoutSynchronization) {
          /* start the timeout-thread */
          m_timeoutSupervisionThread = new Thread(this, "SimulatedLimitedDataChannel: Channel-timeout supervisor thread");
          m_timeoutSupervisionThread.setDaemon(true);
          m_timeoutSupervisionThread.start();
        }
        createAndSendMixPacket(a_order, FLAG_CHANNEL_OPEN);
        m_channelOpened = true;
        return true;
      }
      else {
        /* currently only one upstream-packet is allowed for every channel */
        return false;
      }
    }
  }

  public void multiplexerClosed() {
    synchronized (m_timeoutSynchronization) {
      if (!m_channelClosed) {
        LogHolder.log(LogLevel.ERR, LogType.NET, "SimulatedLimitedDataChannel: multiplexerClosed(): Multiplexer closed before channel has received all packets.");
        getChannelMessageQueue().addChannelMessage(new InternalChannelMessage(InternalChannelMessage.CODE_CHANNEL_EXCEPTION, null));
        m_channelClosed = true;
        m_timeoutSynchronization.notify();
      }
    }
  }


  protected void handleReceivedPacket(MixPacket a_mixPacket) {
    m_receivedPackets++;
    synchronized (m_timeoutSynchronization) {
      if (!m_channelClosed) {
        /* only do something, if the channel isn't already closed */
        if ((a_mixPacket.getChannelFlags() & FLAG_CHANNEL_CLOSE) == FLAG_CHANNEL_CLOSE) {
          if (m_receivedPackets < m_downstreamPackets) {
            /* there are some packets missing -> send an exception via the
             * message-queue
             */
            LogHolder.log(LogLevel.ALERT, LogType.NET, "SimulatedLimitedDataChannel: handleReceivedPacket(): Some packets are missing on channel.");
            getChannelMessageQueue().addChannelMessage(new InternalChannelMessage(InternalChannelMessage.CODE_CHANNEL_EXCEPTION, null));
          }
          /* if close bit is set -> close the channel via the timeout-thread */
          m_channelClosed = true;
          m_timeoutSynchronization.notify();
        }
        else {
          /* it's a data-message */
          if (m_receivedPackets >= m_downstreamPackets) {
            /* we have received more packets than allowed -> send an exception via
             * the message queue
             */
            LogHolder.log(LogLevel.ALERT, LogType.NET, "SimulatedLimitedDataChannel: handleReceivedPacket(): More packets on channel received than allowed.");
            getChannelMessageQueue().addChannelMessage(new InternalChannelMessage(InternalChannelMessage.CODE_CHANNEL_EXCEPTION, null));
            /* close the channel via the timeout-thread */
            m_channelClosed = true;
            m_timeoutSynchronization.notify();
          }
          else {
            getChannelMessageQueue().addChannelMessage(new InternalChannelMessage(InternalChannelMessage.CODE_PACKET_RECEIVED, a_mixPacket.getPayloadData()));
          }
        }
      }
    }
  }

  public void run() {
    synchronized (m_timeoutSynchronization) {
      try {
        m_timeoutSynchronization.wait(m_channelTimeout);
      }
      catch (InterruptedException e) {
      }
      if (!m_channelClosed) {
        /* timeout occured -> send an exception */
        LogHolder.log(LogLevel.ALERT, LogType.NET, "SimulatedLimitedDataChannel: run(): Channel-timeout occured.");
        getChannelMessageQueue().addChannelMessage(new InternalChannelMessage(InternalChannelMessage.CODE_CHANNEL_EXCEPTION, null));
      }
      /* in every case send a close-message and delete the channel from the channel-table */
      getChannelMessageQueue().addChannelMessage(new InternalChannelMessage(InternalChannelMessage.CODE_CHANNEL_CLOSED, null));
    }
    deleteChannel();
  }

}
