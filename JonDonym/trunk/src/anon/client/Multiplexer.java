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

import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.IOException;
import java.io.OutputStream;
import java.security.SecureRandom;
import java.util.Enumeration;
import java.util.Observable;
import java.util.Vector;

import anon.client.crypto.SymCipher;
import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;

/**
 * @author Stefan Lieske
 */
public class Multiplexer extends Observable implements Runnable
{

	private Vector m_sendJobQueue;
	private Vector m_controlMessageQueue;

	private Object m_waitQueueObject;
	
	private ChannelTable m_channelTable;

	private InputStream m_inputStream;

	private OutputStream m_outputStream;

	private SymCipher m_inputStreamCipher;

	private SymCipher m_outputStreamCipher;

	private Object m_internalEventSynchronization;

	public Multiplexer(InputStream a_inputStream, OutputStream a_outputStream,
					   KeyExchangeManager a_keyExchangeManager, SecureRandom a_channelIdGenerator)
	{
		m_internalEventSynchronization = new Object();
		m_sendJobQueue = new Vector();
		m_controlMessageQueue = new Vector();
		m_waitQueueObject = new Object();
		
		m_channelTable = new ChannelTable(new DefaultDataChannelFactory(a_keyExchangeManager, this),
										  a_channelIdGenerator);
		m_inputStream = a_inputStream;
		m_inputStreamCipher = a_keyExchangeManager.getMultiplexerInputStreamCipher();
		m_outputStream = a_outputStream;
		m_outputStreamCipher = a_keyExchangeManager.getMultiplexerOutputStreamCipher();
		Thread downstreamThread = new Thread(this, "Multiplexer: Receive-Thread");
		downstreamThread.setDaemon(true);
		downstreamThread.start();
	}

	public void sendPacket(MixPacket a_mixPacket) throws IOException
	{
		Object ownSynchronizationObject = new Object();
		boolean coChPacket = m_channelTable.isControlChannelId(a_mixPacket.getChannelId());
		Vector waitQueue = coChPacket ? m_controlMessageQueue : m_sendJobQueue;;
		
		synchronized (ownSynchronizationObject)
		{
			boolean waitForAccess = false;
						
			synchronized (m_waitQueueObject)
			{
				waitQueue.addElement(ownSynchronizationObject);
				
				if(!coChPacket)
				{
					if((m_controlMessageQueue.size() > 1) 
					|| (m_sendJobQueue.size() > 1)	)
					{
						/* data channel packets have to wait until 
						 * control channel packets are processed
						 */
						waitForAccess = true;
					}
				}
				else
				{
					if (m_controlMessageQueue.size() > 1)
					{
						/* control channel packets have higher priority and are processed 
						 * before any data traffic can be transmitted so cost confirmations 
						 * can be delivered at time in case of data congestion.
						 */
						waitForAccess = true;
						LogHolder.log(LogLevel.WARNING, LogType.NET,
								  "Control channel congestion");
					}
				}
			}
			if (waitForAccess)
			{
				try
				{
					ownSynchronizationObject.wait();
				}
				catch (InterruptedException e)
				{
					/* stop waiting, if we get interrupted and remove ourself from the send-queue */
					Object nextLockObject = null;
					synchronized (m_waitQueueObject)
					{
						//if (m_sendJobQueue.indexOf(ownSynchronizationObject) == 0)
						if (waitQueue.indexOf(ownSynchronizationObject) == 0)
						{
							/*control channel messages have higher priority */
							if ( (coChPacket && (m_controlMessageQueue.size() > 1)) ||		
								 (!coChPacket && (m_controlMessageQueue.size() > 0)) )
							{
								/* first wake up control channel packets */
								nextLockObject = m_controlMessageQueue.elementAt(1);
							}
							else 
							{
								/* just in this moment we should get notified -> notify the next waiting
								 * thread, if there is one
								 */
								if ( (!coChPacket && (m_sendJobQueue.size() > 1)) ||		
									 (coChPacket && (m_sendJobQueue.size() > 0)) )
								{
									/* there are more threads waiting to send packets */
									nextLockObject = m_sendJobQueue.elementAt(1);
								}
							}
						}
						waitQueue.removeElement(ownSynchronizationObject);
					}
					if (nextLockObject != null)
					{
						synchronized (nextLockObject)
						{
							/* wake up the next of the waiting threads */
							nextLockObject.notify();
						}
					}
					/* now we have cleaned up everything -> throw the origin InterruptedException
					 * as an InterruptedIOException
					 */
					throw (new InterruptedIOException(e.toString()));
				}
			}
		}
		
		/* first call all SendCallbackHandlers to finalize the packet */
		Enumeration sendCallbackHandlers = a_mixPacket.getSendCallbackHandlers().elements();
		while (sendCallbackHandlers.hasMoreElements())
		{
			( (ISendCallbackHandler) (sendCallbackHandlers.nextElement())).finalizePacket(a_mixPacket);
		}
		byte[] packetData = a_mixPacket.getRawPacket();
		/* do stream-encoding */
		if (m_outputStreamCipher != null)
		{
			m_outputStreamCipher.encryptAES(packetData, 0, packetData, 0, 16);
		}
		/* now we can send the packet */
		try
		{
			m_outputStream.write(packetData);
			m_outputStream.flush();
			synchronized (m_internalEventSynchronization)
			{
				setChanged();
				if (coChPacket)
				{
					/* we've sent a packet on a control channel */
					notifyObservers(new PacketProcessedEvent(PacketProcessedEvent.CODE_CONTROL_PACKET_SENT));
				}
				else
				{
					/* we've sent a packet on a data channel */
					notifyObservers(new PacketProcessedEvent(PacketProcessedEvent.CODE_DATA_PACKET_SENT));
				}
			}
		}
		finally
		{
			/* always wake up the next waiting thread */
			Object nextLockObject = null;
			
			synchronized (m_waitQueueObject)
			{
				/* remove our lock-object from the job-queue */
				waitQueue.removeElementAt(0);
				
				/* first handle control channel packets that have higher priority */
				if (m_controlMessageQueue.size() > 0)
				{
					/* there are more threads waiting to send packets */
					nextLockObject = m_controlMessageQueue.firstElement();
				}
				else
				{
					/* if there no control channel packets to be processed wake
					 * up other Threads for data transmission
					 */
					if (m_sendJobQueue.size() > 0)
					{
						/* there are more threads waiting to send packets */
						nextLockObject = m_sendJobQueue.firstElement();
					}
				}
			}
			if (nextLockObject != null)
			{
				synchronized (nextLockObject)
				{
					/* wake up the next of the waiting threads */
					nextLockObject.notify();
				}
			}
		}
	}

	public void run()
	{
		try
		{
			while (true)
			{
				/* read packet from stream */
				MixPacket receivedPacket = new MixPacket(m_inputStream, m_inputStreamCipher);
				AbstractChannel channel = m_channelTable.getChannel(receivedPacket.getChannelId());
				if (channel != null)
				{
					synchronized (m_internalEventSynchronization)
					{
						setChanged();
						if (m_channelTable.isControlChannelId(receivedPacket.getChannelId()))
						{
							/* we've received a packet on a control channel */
							notifyObservers(new PacketProcessedEvent(PacketProcessedEvent.
								CODE_CONTROL_PACKET_RECEIVED));
						}
						else
						{
							/* we've received a packet on a data channel */
							notifyObservers(new PacketProcessedEvent(PacketProcessedEvent.
								CODE_DATA_PACKET_RECEIVED));
						}
					}
					channel.processReceivedPacket(receivedPacket);
				}
				else
				{
					/* we don't know a channel with the specified ID - maybe it's already closed */
					LogHolder.log(LogLevel.INFO, LogType.NET,
								  "Received a packet for unknown channel '" +
								  Integer.toString(receivedPacket.getChannelId()) + "'.");
					synchronized (m_internalEventSynchronization)
					{
						setChanged();
						if (m_channelTable.isControlChannelId(receivedPacket.getChannelId()))
						{
							/* we've discarded a control channel packet */
							notifyObservers(new PacketProcessedEvent(PacketProcessedEvent.
								CODE_CONTROL_PACKET_DISCARDED));
						}
						else
						{
							/* we've discarded a data channel packet */
							notifyObservers(new PacketProcessedEvent(PacketProcessedEvent.
								CODE_DATA_PACKET_DISCARDED));
						}
					}
				}
			}
		}
		catch (IOException e)
		{
			/* end of input stream handling */
			LogHolder.log(LogLevel.WARNING, LogType.NET, Thread.currentThread().getName()+": was terminated by IOException: "+
					e.getClass().getSimpleName());
		}
		/* close the channel-table (notifies also all open channels) */
		m_channelTable.closeChannelTable();
	}

	public ChannelTable getChannelTable()
	{
		return m_channelTable;
	}

}
