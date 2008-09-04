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

import java.security.SecureRandom;
import java.util.Enumeration;
import java.util.Hashtable;

import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;


/** 
 * @author Stefan Lieske
 */
public class ChannelTable implements IDataChannelCreator {

  public static final int CONTROL_CHANNEL_ID_PAY = 2;
  
  public static final int CONTROL_CHANNEL_ID_REPLAY = 3;
  
  public static final int CONTROL_CHANNEL_ID_DUMMY = 4;
  
  
  private static final int MAX_OPEN_DATACHANNELS = 50;
  
  /**
   * The minimum Channel-ID in the reserved area for ControlChannels
   * (those Channel-IDs are not available for DataChannels).
   * Attention: ChannelID 0 is also not available for ControlChannels
   * (it is used mix-internal for filling the pool with dummies, if
   * no real packet is received).
   */
  private static final int MIN_RESERVED_CHANNEL_ID = 0;
  
  /**
   * The maximum Channel-ID in the reserved area for ControlChannels
   * (those Channel-IDs are not available for DataChannels).
   */
  private static final int MAX_RESERVED_CHANNEL_ID = 255;
  
  
  private IDataChannelFactory m_dataChannelFactory;
  
  private Hashtable m_channelTable;

  private volatile int m_availableDataChannels;
  
  private SecureRandom m_channelIdGenerator;
  
  private volatile boolean m_tableClosed;
  
  
  public ChannelTable(IDataChannelFactory a_dataChannelFactory, SecureRandom a_channelIdGenerator) {
    m_dataChannelFactory = a_dataChannelFactory;
    m_channelTable = new Hashtable();
    m_availableDataChannels = MAX_OPEN_DATACHANNELS;
    m_channelIdGenerator = a_channelIdGenerator;
    m_tableClosed = false;
  }
  
  
  public AbstractChannel getChannel(int a_channelId) {
    AbstractChannel returnedChannel = null;
    synchronized (m_channelTable) {
      returnedChannel = (AbstractChannel)(m_channelTable.get(new Integer(a_channelId)));      
    }
    return returnedChannel;
  }
  
  public void removeChannel(int a_channelId) {
    synchronized (m_channelTable) {
      if (!m_tableClosed) {
        AbstractChannel removedChannel = (AbstractChannel)(m_channelTable.remove(new Integer(a_channelId)));
        if (removedChannel instanceof AbstractDataChannel) {
          m_availableDataChannels++;
          /* notify waiting threads about the available channel */
          m_channelTable.notifyAll();
        }
        if (removedChannel != null) {
          LogHolder.log(LogLevel.DEBUG, LogType.NET, "ChannelTable: removeChannel(): Removed channel with ID '" + Integer.toString(a_channelId) + "' from table.");
        }
      }
    }
  }
  
  public void registerControlChannel(int a_channelId, AbstractControlChannel a_controlChannel) {
    synchronized (m_channelTable) {
      if (!m_tableClosed) {
        m_channelTable.put(new Integer(a_channelId), a_controlChannel);
        LogHolder.log(LogLevel.DEBUG, LogType.NET, "ChannelTable: registerControlChannel(): Registered ControlChannel with ID '" + Integer.toString(a_channelId) + "'.");
      }
      else {
        a_controlChannel.multiplexerClosed();
      }
    }
  }
  
  public boolean isControlChannelId(int a_channelId) {
    /* it's a little bit nasty because channel-ID 0 is reserved but not a
     * controlchannel-ID, so we can't use MIN_RESERVED_CHANNEL_ID
     */
    return ((a_channelId > 0) && (a_channelId <= MAX_RESERVED_CHANNEL_ID));
  }
  
  public AbstractDataChannel createDataChannel(AbstractDataChain a_parentDataChain) {
    AbstractDataChannel createdChannel = null;
    synchronized (m_channelTable) {
      try {
        while ((m_availableDataChannels <= 0) && (!m_tableClosed)) {
          m_channelTable.wait();
        }
        if (!m_tableClosed) {
          int channelId = getFreeChannelId();
          createdChannel = m_dataChannelFactory.createDataChannel(channelId, a_parentDataChain);
          m_channelTable.put(new Integer(channelId), createdChannel);
          m_availableDataChannels--;
          LogHolder.log(LogLevel.DEBUG, LogType.NET, "ChannelTable: createDataChannel(): Created DataChannel with ID '" + Integer.toString(channelId) + "'.");
        }
        else {
          /* create a dummy-channel and call multiplexerClosed() immediately */
          createdChannel = m_dataChannelFactory.createDataChannel(0, a_parentDataChain);
          createdChannel.multiplexerClosed();
        }
      }
      catch (InterruptedException e) {
        /* do nothing */
      }     
    }
    return createdChannel;
  }
  
  public void closeChannelTable() {
    synchronized (m_channelTable) {
      m_tableClosed = true;
      Enumeration channels = m_channelTable.elements();
      while (channels.hasMoreElements()) {
        AbstractChannel currentChannel = (AbstractChannel)(channels.nextElement());
        currentChannel.multiplexerClosed();
      }
      m_channelTable.clear();
      m_availableDataChannels = MAX_OPEN_DATACHANNELS;
      LogHolder.log(LogLevel.DEBUG, LogType.NET, "ChannelTable: closeChannelTable(): Removed all channels from table.");
      /* maybe there are some threads waiting because of the data-channel
       * limit -> notify them
       */
      m_channelTable.notifyAll();
    }
  }
  
  
  /**
   * Returns an available DataChannel-ID. The returned value is an ID which is
   * not in the reserved area for control-channels and also not already in use
   * by a datachannel. The returned ID can be negative because the mix simply
   * expects a 32 bit value (unsigned). Synchronization with the channel-table
   * has to be done by the caller.
   * 
   * @return An available DataChannel-ID (maybe negative).
   */
  private int getFreeChannelId() {
    int channelId = 0;
    do {
      channelId = m_channelIdGenerator.nextInt();
    } while (((channelId >= MIN_RESERVED_CHANNEL_ID) && (channelId <= MAX_RESERVED_CHANNEL_ID)) || (getChannel(channelId) != null));
    /* now the Channel-ID is not in the reserved area and also not already in
     * the channel-table
     */
    return channelId;
  }
  
}
