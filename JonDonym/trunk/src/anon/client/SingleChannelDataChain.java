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
import java.util.Vector;

/**
 * @author Stefan Lieske
 */
public class SingleChannelDataChain extends AbstractDataChain
{

	private static final short FLAG_FLOW_CONTROL = (short) 0x8000;

	private int m_chainType;

	private boolean m_supportFlowControl;

	private AbstractDataChannel m_associatedChannel;

	private boolean m_firstUpstreamPacket;

	private class InvalidChainCellException extends Exception
	{

		public InvalidChainCellException(String a_message)
		{
			super(a_message);
		}

	}

	private class ChainCell
	{

		private static final short DATALENGTH_MASK = 0x03FF;

		private static final short FLAG_CONNECTION_ERROR = (short) 0x4000;

		private byte[] m_payloadData;

		private boolean m_flowControlFlagSet;

		private boolean m_connectionErrorFlagSet;

		public ChainCell(byte[] a_rawData) throws InvalidChainCellException
		{
			if (a_rawData.length < 3)
			{
				throw (new InvalidChainCellException(
					"SingleChannelDataChain: ChainCell: Constructor: Length of ChainCell must be at least 3 bytes."));
			}
			short lengthAndFlagsField = 0;
			try
			{
				DataInputStream rawDataStream = new DataInputStream(new ByteArrayInputStream(a_rawData, 0, 2));
				lengthAndFlagsField = rawDataStream.readShort();
			}
			catch (IOException e)
			{
				/* should never occur */
			}
			m_flowControlFlagSet = false;
			m_connectionErrorFlagSet = false;
			short flags = (short) (lengthAndFlagsField & (~DATALENGTH_MASK));
			if (m_supportFlowControl)
			{
				if ( (flags & FLAG_FLOW_CONTROL) == FLAG_FLOW_CONTROL)
				{
					m_flowControlFlagSet = true;
				}
			}
			if ( (flags & FLAG_CONNECTION_ERROR) == FLAG_CONNECTION_ERROR)
			{
				m_connectionErrorFlagSet = true;
			}
			int dataLength = lengthAndFlagsField & DATALENGTH_MASK;
			/* data is starting at byte 3 (0 and 1 are length and flags, 2 is the type and can
			 * be ignored)
			 */
			int dataOffset = 3;
			if (dataOffset + dataLength > a_rawData.length)
			{
				throw (new InvalidChainCellException(
					"SingleChannelDataChain: ChainCell: Constructor: ChainCell has invalid length-field."));
			}
			m_payloadData = new byte[dataLength];
			System.arraycopy(a_rawData, dataOffset, m_payloadData, 0, dataLength);
		}

		public byte[] getPayloadData()
		{
			return m_payloadData;
		}

		public boolean isFlowControlFlagSet()
		{
			return m_flowControlFlagSet;
		}

		public boolean isConnectionErrorFlagSet()
		{
			return m_connectionErrorFlagSet;
		}
	}

	public SingleChannelDataChain(IDataChannelCreator a_channelCreator,
								  DataChainErrorListener a_errorListener, int a_chainType,
								  boolean a_supportFlowControl)
	{
		super(a_channelCreator, a_errorListener);
		m_chainType = a_chainType;
		m_supportFlowControl = a_supportFlowControl;
		/* create the channel */
		m_associatedChannel = createDataChannel();
		/* observe the channel-message queue */
		m_associatedChannel.getChannelMessageQueue().addObserver(this);
		m_firstUpstreamPacket = true;
	}

	public int getOutputBlockSize()
	{
		int channelPacketSize = 0;
		synchronized (m_associatedChannel)
		{
			channelPacketSize = m_associatedChannel.getNextPacketRecommandedOutputBlocksize();
		}
		return Math.max(0, channelPacketSize - 3);
	}

	public void createPacketPayload(DataChainSendOrderStructure a_order)
	{
		if (a_order.getOrderData() != null)
		{
			int dataLength = Math.min(a_order.getOrderData().length, a_order.getChannelCell().length - 3);
			if (m_supportFlowControl)
			{
				/* check whether we shall send a flow-control flag */
				if (a_order.getAdditionalProtocolData() instanceof Boolean)
				{
					if ( ( (Boolean) (a_order.getAdditionalProtocolData())).booleanValue())
					{
						/* we shall send a packet with flow-control flag */
						dataLength = dataLength | FLAG_FLOW_CONTROL;
					}
				}
			}
			ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
			DataOutputStream dataStream = new DataOutputStream(byteStream);
			try
			{
				dataStream.writeShort(dataLength);
				dataStream.flush();
				if (m_firstUpstreamPacket)
				{
					byteStream.write(m_chainType);
					m_firstUpstreamPacket = false;
				}
				else
				{
					byteStream.write(0);
				}
				byteStream.write(a_order.getOrderData(), 0, dataLength);
				byteStream.flush();
			}
			catch (IOException e)
			{
				/* cannot happen */
			}
			System.arraycopy(byteStream.toByteArray(), 0, a_order.getChannelCell(), 0,
							 byteStream.toByteArray().length);
			a_order.setProcessedBytes(dataLength);
		}
	}

	public void run()
	{
		Vector messageQueuesNotifications = getMessageQueuesNotificationsList();
		try
		{
			while (!Thread.interrupted())
			{
				InternalChannelMessage currentMessage = null;
				InternalChannelMessageQueue messageQueue = null;
				synchronized (messageQueuesNotifications)
				{
					while (messageQueuesNotifications.size() == 0)
					{
						messageQueuesNotifications.wait();
					}
					messageQueue = (InternalChannelMessageQueue) (messageQueuesNotifications.firstElement());
					currentMessage = messageQueue.getFirstMessage();
					/* we will process the message immediately -> remove it from the queue */
					messageQueue.removeFirstMessage();
					/* remove also the notification */
					messageQueuesNotifications.removeElementAt(0);
				}
				switch (currentMessage.getMessageCode())
				{
					case InternalChannelMessage.CODE_PACKET_RECEIVED:
					{
						try
						{
							ChainCell dataCell = new ChainCell(currentMessage.getMessageData());
							if (m_supportFlowControl && dataCell.isFlowControlFlagSet())
							{
								/* send a response-packet with the flow-control flag */
								DataChainSendOrderStructure order = new DataChainSendOrderStructure(new byte[
									0]);
								order.setAdditionalProtocolData(new Boolean(true));
								orderPacket(order);
							}
							/* add data to the datastream */
							addInputStreamQueueEntry(new DataChainInputStreamQueueEntry(
								DataChainInputStreamQueueEntry.TYPE_DATA_AVAILABLE, dataCell.getPayloadData()));
							if (dataCell.isConnectionErrorFlagSet())
							{
								addInputStreamQueueEntry(new DataChainInputStreamQueueEntry(new IOException(
									"SingleChannelDataChain: run(): Last mix signaled connection error.")));
								propagateConnectionError();
							}
						}
						catch (InvalidChainCellException e)
						{
							addInputStreamQueueEntry(new DataChainInputStreamQueueEntry(new IOException(e.
								toString())));
						}
						break ;
					}
					case InternalChannelMessage.CODE_CHANNEL_CLOSED:
					{
						addInputStreamQueueEntry(new DataChainInputStreamQueueEntry(
							DataChainInputStreamQueueEntry.TYPE_STREAM_END, null));
						/* stop observing the message-queue of the channel */
						messageQueue.deleteObserver(this);
						/* interrupt this thread because the only channel of the DataChain was
						 * closed
						 */
						Thread.currentThread().interrupt();
						break;
					}
					case InternalChannelMessage.CODE_CHANNEL_EXCEPTION:
					{
						addInputStreamQueueEntry(new DataChainInputStreamQueueEntry(new IOException(
							"SingleChannelDataChain: run(): Channel signaled an exception - closing chain.")));
						break;
					}
				}
			}
		}
		catch (InterruptedException e)
		{
		}

	}

	protected void orderPacket(DataChainSendOrderStructure a_order)
	{
		synchronized (m_associatedChannel)
		{
			m_associatedChannel.processSendOrder(a_order);
		}
	}

	protected void outputStreamClosed() throws IOException
	{
		/* close the whole data-chain */
		close();
	}

	protected void closeDataChain()
	{
		synchronized (m_associatedChannel)
		{
			try
			{
				m_associatedChannel.organizeChannelClose();
			}
			catch (IOException e)
			{
				/* channel is locally closed in every case -> IOException doesn't matter */
			}
		}
	}

}
