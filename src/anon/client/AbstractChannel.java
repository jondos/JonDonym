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
import anon.IServiceContainer;


/**
 * @author Stefan Lieske
 */
public abstract class AbstractChannel {

  private int m_channelId;

  private Multiplexer m_parentMultiplexer;

  private volatile boolean m_channelOpen;

  private Object m_internalSynchronization;


  public AbstractChannel(int a_channelId, Multiplexer a_parentMultiplexer) {
    m_channelId = a_channelId;
    m_parentMultiplexer = a_parentMultiplexer;
    m_channelOpen = true;
    m_internalSynchronization = new Object();
  }


  public MixPacket createEmptyMixPacket() {
    return (new MixPacket(m_channelId));
  }

  public void sendPacket(MixPacket a_mixPacket) throws IOException {
    synchronized (m_internalSynchronization) {
      if (m_channelOpen) {
        /* only send the packet, if the channel is still open - otherwise there is the
         * possibility that it is sent on another channel which got the same ID in the
         * meantime
         */
        m_parentMultiplexer.sendPacket(a_mixPacket);
      }
      else {
        throw (new ChannelClosedException("AbstractChannel: sendPacket(): The channel is already closed."));
      }
    }
  }

  public void deleteChannel() {
    /* Attention: It can take some time until we get the synchronization monitor, if a
     * packet is currently in the send-queue of the multiplexer.
     */
    synchronized (m_internalSynchronization) {
      if (m_channelOpen) {
        /* ensure that removeChannel() is called only once (in the other case we maybe
         * remove a new channel which got the same ID in the meantime)
         */
        m_parentMultiplexer.getChannelTable().removeChannel(m_channelId);
        m_channelOpen = false;
      }
    }
  }

  /**
   * This method is called on every channel in the channel-table after the
   * multiplexer is closed. Children of AbstractChannel should overwrite this
   * method. It's not necessary to delete the channel from the channel-table
   * but maybe there are some other tasks to do.
   */
  public void multiplexerClosed() {
  }


  public abstract void processReceivedPacket(MixPacket a_mixPacket);

}
