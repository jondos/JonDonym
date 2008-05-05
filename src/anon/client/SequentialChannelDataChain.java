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
import java.io.IOException;
import java.io.InterruptedIOException;
import java.util.Vector;

import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;


/** 
 * @author Stefan Lieske
 */
public class SequentialChannelDataChain extends AbstractDataChain {

  private static final int CHAIN_ID_LENGTH = 8;

  /* downstream flags */
  private static final short FLAG_UNKNOWN_CHAIN_ID = (short)0x2000;
  private static final short FLAG_CONNECTION_ERROR = (short)0x8000;

  /* upstream flags */
  private static final short FLAG_NEW_CHAIN        = (short)0x2000;
  private static final short FLAG_FAST_RESPONSE    = (short)0x8000;
  
  /* flags for both directions */
  private static final short FLAG_STREAM_CLOSED    = (short)0x4000;

  
  private Vector m_associatedChannels;
  
  private boolean m_firstDownstreamPacket;
  
  private volatile byte[] m_chainId;
  
  private int m_maximumOutputBlocksize;
  
  private Object m_sendSynchronization;
    
  private volatile boolean m_chainClosed;
  
  private long m_chainTimeout;
    
  
  private class InvalidChainCellException extends Exception {
    
    public InvalidChainCellException(String a_message) {
      super(a_message);
    }
    
  }
  
  private class ChainCell {

    private static final short DATALENGTH_MASK = 0x03FF;
    
    
    private byte[] m_payloadData;
    
    private byte[] m_receivedChainId;
    
    private boolean m_unknownChainIdFlagSet;
    
    private boolean m_connectionErrorFlagSet;
    
    private boolean m_downstreamClosedFlagSet;
    
    
    public ChainCell(byte[] a_rawData) throws InvalidChainCellException {
      if (a_rawData.length < 2) {
        throw (new InvalidChainCellException("SequentialChannelDataChain: ChainCell: Constructor: Length of chaincell must be at least 2 bytes."));
      }
      short lengthAndFlagsField = 0;
      try {
        DataInputStream rawDataStream = new DataInputStream(new ByteArrayInputStream(a_rawData, 0, 2));
        lengthAndFlagsField = rawDataStream.readShort();
      }
      catch (IOException e) {
        /* should never occur */
      }
      short flags = (short)(lengthAndFlagsField & (~DATALENGTH_MASK));
      if ((flags & FLAG_UNKNOWN_CHAIN_ID) == FLAG_UNKNOWN_CHAIN_ID) {
        m_unknownChainIdFlagSet = true;
      }
      else {
        m_unknownChainIdFlagSet = false;
      }
      if ((flags & FLAG_CONNECTION_ERROR) == FLAG_CONNECTION_ERROR) {
        m_connectionErrorFlagSet = true;
      }
      else {
        m_connectionErrorFlagSet = false;
      }
      if ((flags & FLAG_STREAM_CLOSED) == FLAG_STREAM_CLOSED) {
        m_downstreamClosedFlagSet = true;
      }
      else {
        m_downstreamClosedFlagSet = false;
      }
      int dataOffset = 2;
      int dataLength = lengthAndFlagsField & DATALENGTH_MASK;
      if (m_firstDownstreamPacket) {
        if (a_rawData.length < dataOffset + CHAIN_ID_LENGTH + dataLength) {
          throw (new InvalidChainCellException("SequentialChannelDataChain: ChainCell: Constructor: First downstream chaincell must contain Chain-ID."));          
        }
        m_receivedChainId = new byte[CHAIN_ID_LENGTH];
        System.arraycopy(a_rawData, dataOffset, m_receivedChainId, 0, CHAIN_ID_LENGTH);
        dataOffset = dataOffset + CHAIN_ID_LENGTH;
        m_firstDownstreamPacket = false;
      }
      else {
        if (dataOffset + dataLength > a_rawData.length) {
          throw (new InvalidChainCellException("SequentialChannelDataChain: ChainCell: Constructor: Chaincell has invalid length-field."));        
        }
        m_receivedChainId = null;
      }
      m_payloadData = new byte[dataLength];
      System.arraycopy(a_rawData, dataOffset, m_payloadData, 0, dataLength);
    }
    
    
    public byte[] getPayloadData() {
      return m_payloadData;
    }

    public byte[] getReceivedChainId() {
      return m_receivedChainId;
    }
    
    public boolean isUnknownChainIdFlagSet() {
      return m_unknownChainIdFlagSet;
    }
    
    public boolean isDownstreamClosedFlagSet() {
      return m_downstreamClosedFlagSet;
    }
    
    public boolean isConnectionErrorFlagSet() {
      return m_connectionErrorFlagSet;
    }
    
  }
  
  
  private class SendOrderProtocolData {
    
    private DataChainChannelListEntry m_channelEntry;
    
    private boolean m_sendUpstreamClose;
    
    private boolean m_enforceFastResponse;
    
    
    public SendOrderProtocolData(DataChainChannelListEntry a_channelEntry, boolean a_sendUpstreamClose, boolean a_enforceFastResponse) {
      m_channelEntry = a_channelEntry;
      m_sendUpstreamClose = a_sendUpstreamClose;
      m_enforceFastResponse = a_enforceFastResponse;
    }
    
    
    public DataChainChannelListEntry getChannelEntry() {
      return m_channelEntry;
    }
    
    public boolean sendUpstreamClose() {
      return m_sendUpstreamClose;
    }
    
    public boolean enforceFastResponse() {
      return m_enforceFastResponse;
    }
    
  }
  
  public SequentialChannelDataChain(IDataChannelCreator a_channelCreator, DataChainErrorListener a_errorListener, long a_chainTimeout) {
    super(a_channelCreator, a_errorListener);
    m_associatedChannels = new Vector();
    m_sendSynchronization = new Object();
    m_firstDownstreamPacket = true;
    m_chainClosed = false;
    m_chainTimeout = a_chainTimeout;
    /* create a first channel for calculating the preferred output blocksize */
    AbstractDataChannel dummyChannel = createDataChannel();
    int channelBlocksize = dummyChannel.getNextPacketRecommandedOutputBlocksize();
    try {
      dummyChannel.organizeChannelClose();
    }
    catch (IOException e) {
      /* should not happen */
    }
    /* calculate the maximum blocksize we are able to handle -> we loose 2
     * bytes for length+flags but need one byte more for the decision whether
     * the fast-response flag should be set or not (the type of the data-chain
     * is part of the data and so not taken into consideration here, also this
     * calculates the maximum blocksize, for sequel-packets we would also
     * loose 8 bytes for the chain ID)
     */
    m_maximumOutputBlocksize = channelBlocksize - 2 + 1;
  }
  
  
  public int getOutputBlockSize() {
    return m_maximumOutputBlocksize;
  }

  public void createPacketPayload(DataChainSendOrderStructure a_order) {
    if (a_order.getOrderData() != null) {
      SendOrderProtocolData protocolData = (SendOrderProtocolData)(a_order.getAdditionalProtocolData());
      int dataLength = 0;
      boolean firstSequelChannelPacket = false;
      if ((protocolData.getChannelEntry().getProcessedUpstreamPackets() == 0) && (m_chainId != null)) { 
        /* first packet of a sequel-channel */
        dataLength = Math.min(a_order.getOrderData().length, a_order.getChannelCell().length - 2 - CHAIN_ID_LENGTH);
        firstSequelChannelPacket = true;
        LogHolder.log(LogLevel.DEBUG, LogType.NET, "SequentialChannelDataChain: createPacketPayload(): Resuming existent chain.");
      }
      else {
        dataLength = Math.min(a_order.getOrderData().length, a_order.getChannelCell().length - 2);        
      }
      int lengthAndFlags = dataLength;
      if ((a_order.getOrderData().length > dataLength) || (protocolData.enforceFastResponse())) {
        lengthAndFlags = lengthAndFlags | FLAG_FAST_RESPONSE;
      }
      if (protocolData.sendUpstreamClose()) {
        lengthAndFlags = lengthAndFlags | FLAG_STREAM_CLOSED;
        LogHolder.log(LogLevel.DEBUG, LogType.NET, "SequentialChannelDataChain: createPacketPayload(): Sending STREAM_CLOSE.");
      }
      if ((protocolData.getChannelEntry().getProcessedUpstreamPackets() == 0) && (!firstSequelChannelPacket)) {
        lengthAndFlags = lengthAndFlags | FLAG_NEW_CHAIN;        
        LogHolder.log(LogLevel.DEBUG, LogType.NET, "SequentialChannelDataChain: createPacketPayload(): Sending NEW_CHAIN.");
      }
      protocolData.getChannelEntry().incProcessedUpstreamPackets();
      ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
      DataOutputStream dataStream = new DataOutputStream(byteStream);
      try {
        dataStream.writeShort(lengthAndFlags);
        dataStream.flush();
        if (firstSequelChannelPacket) {
          byteStream.write(m_chainId);
        }
        byteStream.write(a_order.getOrderData(), 0, dataLength);
        byteStream.flush();
      }
      catch (IOException e) {
        /* cannot happen */
      }
      System.arraycopy(byteStream.toByteArray(), 0, a_order.getChannelCell(), 0, byteStream.toByteArray().length);
      a_order.setProcessedBytes(dataLength);
    }
  }

  public void run() {
    try {
      DataChainChannelListEntry currentChannel = null;
      while (!Thread.interrupted()) {
        if (currentChannel == null) {
          synchronized (m_associatedChannels) {
            if ((m_associatedChannels.size() == 0) && (!m_firstDownstreamPacket)) {
              /* no channel open, but stream is not closed -> create a dummy-
               * channel so mix can send responses even if we doesn't have
               * anything to send
               */
              Thread chainKeepAliveThread = new Thread(new Runnable() {
                public void run() {
                  DataChainSendOrderStructure dummyOrder = new DataChainSendOrderStructure(new byte[0]);
                  orderPacketInternal(dummyOrder, false, true);
                }
              }, "SequentialChannelDataChain: Datachain keep-alive thread");
              chainKeepAliveThread.setDaemon(true);
              chainKeepAliveThread.start();
            }
            while (m_associatedChannels.size() == 0) {
              m_associatedChannels.wait();
            }
            currentChannel = (DataChainChannelListEntry)(m_associatedChannels.firstElement());
          }
        }
        InternalChannelMessage currentMessage = currentChannel.getChannel().getChannelMessageQueue().waitForNextMessage();
        currentChannel.getChannel().getChannelMessageQueue().removeFirstMessage();
        switch (currentMessage.getMessageCode()) {
          case InternalChannelMessage.CODE_PACKET_RECEIVED: {
            ChainCell dataCell = null;
            try {
              dataCell = new ChainCell(currentMessage.getMessageData());
            }
            catch (InvalidChainCellException e) {
              addInputStreamQueueEntry(new DataChainInputStreamQueueEntry(new IOException(e.toString())));
              Thread.currentThread().interrupt();
            }
            if (dataCell != null) {
              if (dataCell.getReceivedChainId() != null) {
                /* was the first packet -> we have received a chain-id */
                m_chainId = dataCell.getReceivedChainId();
              }
              if (dataCell.getPayloadData().length > 0) {
                /* add data to the datastream */
                LogHolder.log(LogLevel.DEBUG, LogType.NET, "SequentialChannelDataChain: run(): Data received.");
                addInputStreamQueueEntry(new DataChainInputStreamQueueEntry(DataChainInputStreamQueueEntry.TYPE_DATA_AVAILABLE, dataCell.getPayloadData()));
              }
              if (dataCell.isUnknownChainIdFlagSet()) {
                LogHolder.log(LogLevel.ERR, LogType.NET, "SequentialChannelDataChain: run(): Last mix signaled unknown chain ID.");
                addInputStreamQueueEntry(new DataChainInputStreamQueueEntry(new IOException("SequentialChannelDataChain: run(): Last mix signaled unknown chain ID.")));                
              }
              if (dataCell.isDownstreamClosedFlagSet()) {
                LogHolder.log(LogLevel.DEBUG, LogType.NET, "SequentialChannelDataChain: run(): Received downstream-close flag.");
                Thread.currentThread().interrupt();
              }
              else {
                synchronized (currentChannel) {
                  currentChannel.incProcessedDownstreamPackets();
                  /* notify all waiting send-threads about the received packet */
                  currentChannel.notify();
                }
              }
            }
            break;
          }
          case InternalChannelMessage.CODE_CHANNEL_CLOSED: {
	    	  ChainCell dataCell = null;	    	  
	          try {
        	  if (currentMessage.getMessageData() != null)
				{
		            dataCell = new ChainCell(currentMessage.getMessageData());
		            if (dataCell.getPayloadData().length == 0 && 
						dataCell.isConnectionErrorFlagSet()) 
		            {
		              LogHolder.log(LogLevel.ERR, LogType.NET, "SequentialChannelDataChain: run(): Last mix signaled a connection-error.");
		              addInputStreamQueueEntry(new DataChainInputStreamQueueEntry(new IOException("SequentialChannelDataChain: run(): Last mix signaled a connection-error.")));
		              propagateConnectionError();
		            }
				}
	          }
	          catch (InvalidChainCellException e) {
	            addInputStreamQueueEntry(new DataChainInputStreamQueueEntry(new IOException(e.toString())));	           
	          }
            
            if (currentChannel.getProcessedDownstreamPackets() == 0) {
              LogHolder.log(LogLevel.ERR, LogType.NET, "SequentialChannelDataChain: run(): Last mix sent CHANNEL_CLOSE immediately without data-packets.");
              /* should never occur that no packets and also no exception is received */
              Thread.currentThread().interrupt();
            }
            else {
              synchronized (m_associatedChannels) {
                m_associatedChannels.removeElementAt(0);
              }
              currentChannel = null;
            }
            break;
          }
          case InternalChannelMessage.CODE_CHANNEL_EXCEPTION: {
            addInputStreamQueueEntry(new DataChainInputStreamQueueEntry(new IOException("SingleChannelDataChain: run(): Channel signaled an exception - closing chain.")));
            synchronized (currentChannel) {
              currentChannel.notify();
            }
            Thread.currentThread().interrupt();
            break;
          }
        }
      }
    }
    catch (InterruptedException e) {
    }
    m_chainClosed = true;
    addInputStreamQueueEntry(new DataChainInputStreamQueueEntry(DataChainInputStreamQueueEntry.TYPE_STREAM_END, null));
    /* notify waiting threads */
    synchronized (m_associatedChannels) {
      while (m_associatedChannels.size() > 0) {
        DataChainChannelListEntry currentChannel = (DataChainChannelListEntry)(m_associatedChannels.firstElement());
        synchronized (currentChannel) {
          currentChannel.notify();
        }
        m_associatedChannels.removeElementAt(0);
      }
    }
  }

  
  protected void orderPacket(DataChainSendOrderStructure a_order) {
    orderPacketInternal(a_order, false, false);
  }
  
  
  private void orderPacketInternal(DataChainSendOrderStructure a_order, boolean a_sendUpstreamClose, boolean a_enforceFastResponse) {
    synchronized (m_sendSynchronization) {
      if (!m_chainClosed) {
        DataChainChannelListEntry lastChannel = null;
        synchronized (m_associatedChannels) {
          if (m_associatedChannels.size() > 0) {
            lastChannel = (DataChainChannelListEntry)(m_associatedChannels.lastElement());      
          }
        }
        boolean packetProcessed = false;
        if (lastChannel != null) {
          /* try to send the packet via the last known channel */
          a_order.setAdditionalProtocolData(new SendOrderProtocolData(lastChannel, a_sendUpstreamClose, a_enforceFastResponse));
          packetProcessed = lastChannel.getChannel().processSendOrder(a_order);
        }
        if (!packetProcessed) {
          if (lastChannel != null) {
            synchronized (lastChannel) {
              if (lastChannel.getProcessedDownstreamPackets() == 0) {
                /* we have to wait until we've received the first downstream-packet */
                try {
                  lastChannel.wait();
                }
                catch (InterruptedException e) {
                  a_order.setThrownException(new InterruptedIOException("SequentialChannelDataChain: orderPacketInternal(): Waiting for available channel was interrupted: " + e.toString()));
                  a_order.processingDone();
                  return;
                }
                if (lastChannel.getProcessedDownstreamPackets() == 0) {
                  a_order.setThrownException(new IOException("SequentialChannelDataChain: orderPacketInternal(): Chain already closed."));
                  a_order.processingDone();                
                }
              }
            }
          }
          /* we can create a sequel-channel */
          DataChainChannelListEntry sequelChannelEntry = new DataChainChannelListEntry(createDataChannel());
          synchronized (m_associatedChannels) {
            m_associatedChannels.addElement(sequelChannelEntry);
            m_associatedChannels.notifyAll();
          }
          a_order.setAdditionalProtocolData(new SendOrderProtocolData(sequelChannelEntry, a_sendUpstreamClose, a_enforceFastResponse));
          sequelChannelEntry.getChannel().processSendOrder(a_order);
        }
      }
      else {
        a_order.setThrownException(new IOException("SequentialChannelDataChain: orderPacketInternal(): Chain already closed."));
        a_order.processingDone();                      
      }
    }
  }
 
  protected void outputStreamClosed() throws IOException {
    /* close the whole data-chain */
    close();
  }

  protected void closeDataChain() {
    synchronized (m_sendSynchronization) {
      if (!m_chainClosed) {
        /* send a chain-close to the last mix */
        orderPacketInternal(new DataChainSendOrderStructure(new byte[0]), true, false);
        m_chainClosed = true;
        interruptDownstreamThread();
      }
    }
  }
  
}
