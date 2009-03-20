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

import anon.ErrorCodes;
import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;
import anon.IServiceContainer;
import anon.infoservice.MixCascade;

/**
 * @author Stefan Lieske
 */
public abstract class AbstractControlChannel extends AbstractChannel
{
	private IServiceContainer m_serviceContainer;

	public AbstractControlChannel(int a_channelId, Multiplexer a_parentMultiplexer,
								  IServiceContainer a_serviceContainer)
	{
		super(a_channelId, a_parentMultiplexer);
		m_serviceContainer = a_serviceContainer;
		if (m_serviceContainer  == null)
		{
			m_serviceContainer = new IServiceContainer()
			{
				public boolean isTrusted(MixCascade a_cascade)
				{
					// not important in this context
					return true;
				}
				public void checkTrust(MixCascade a_cascade) throws TrustException
				{
					// not important in this context
				}

				public void keepCurrentService(boolean a_bKeepCurrentService)
				{
				}

				public boolean isServiceAutoSwitched()
				{
					return false;
				}

				public  boolean isReconnectedAutomatically()
				{
					return false;
				}
				
				public ITermsAndConditionsContainer getTCContainer()
				{
					return null;
				}
			};
		}
		/* register the channel */
		a_parentMultiplexer.getChannelTable().registerControlChannel(a_channelId, this);
	}

	public int sendRawMessage(byte[] a_message)
	{
		try
		{
			int bytesToSend = a_message.length;
			do
			{
				/* we also send empty packages (maybe this is necessary for interaction) */
				MixPacket currentMixPacket = createEmptyMixPacket();
				int currentPacketLength = Math.min(bytesToSend, currentMixPacket.getPayloadData().length);
				currentMixPacket.setChannelFlags( (short) currentPacketLength);
				System.arraycopy(a_message, a_message.length - bytesToSend, currentMixPacket.getPayloadData(),
								 0, currentPacketLength);
				sendPacket(currentMixPacket);
				bytesToSend = bytesToSend - currentPacketLength;
			}
			while (bytesToSend > 0 && !Thread.currentThread().isInterrupted());
			return ErrorCodes.E_SUCCESS;
		}
		catch (IOException e)
		{
			return ErrorCodes.E_UNKNOWN;
		}
	}

	public void processReceivedPacket(MixPacket a_mixPacket)
	{
		int packetDataLength = a_mixPacket.getChannelFlags();
		if ( (packetDataLength > a_mixPacket.getPayloadData().length) || (packetDataLength < 0))
		{
			/* something is wrong here -> ignore the packet */
			LogHolder.log(LogLevel.ERR, LogType.NET,
						  "AbstractControlChannel: processReceivedPacket(): Invalid packet length.");
		}
		else
		{
			byte[] packetData = new byte[packetDataLength];
			System.arraycopy(a_mixPacket.getPayloadData(), 0, packetData, 0, packetDataLength);
			processPacketData(packetData);
		}
	}

	protected final IServiceContainer getServiceContainer()
	{
		return m_serviceContainer;
	}

	protected abstract void processPacketData(byte[] a_packetData);

}
