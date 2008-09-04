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

import anon.ErrorCodes;
import anon.IServiceContainer;

/**
 * @author Stefan Lieske
 */
public abstract class StreamedControlChannel extends AbstractControlChannel
{
	private byte[] m_messageBuffer;

	private int m_currentIndex;

	private byte[] m_lengthBuffer;

	public StreamedControlChannel(int a_channelId, Multiplexer a_multiplexer,
								  IServiceContainer a_serviceContainer)
	{
		super(a_channelId, a_multiplexer, a_serviceContainer);
		m_messageBuffer = new byte[0];
		m_currentIndex = -2;
		m_lengthBuffer = new byte[2];
	}

	public int sendByteMessage(byte[] a_message)
	{
		if (a_message.length > 0xFFFF)
		{
			return ErrorCodes.E_SPACE;
		}
		ByteArrayOutputStream outputBuffer = new ByteArrayOutputStream();
		DataOutputStream dataOutputBuffer = new DataOutputStream(outputBuffer);
		try
		{
			dataOutputBuffer.writeShort(a_message.length);
			dataOutputBuffer.flush();
			outputBuffer.write(a_message);
			outputBuffer.flush();
		}
		catch (IOException e)
		{
			/* cannot happen */
		}
		return sendRawMessage(outputBuffer.toByteArray());
	}

	protected void processPacketData(byte[] a_packetData)
	{
		int currentPacketIndex = 0;
		while (currentPacketIndex < a_packetData.length)
		{
			if (m_currentIndex < 0)
			{
				/* read the length of the next message */
				int lengthBytesToRead = Math.min( -m_currentIndex, a_packetData.length - currentPacketIndex);
				System.arraycopy(a_packetData, currentPacketIndex, m_lengthBuffer,
								 m_lengthBuffer.length + m_currentIndex, lengthBytesToRead);
				m_currentIndex = m_currentIndex + lengthBytesToRead;
				currentPacketIndex = currentPacketIndex + lengthBytesToRead;
				if (m_currentIndex == 0)
				{
					/* we've got the length -> create the buffer for the message data */
					try
					{
						m_messageBuffer = new byte[ (new DataInputStream(new ByteArrayInputStream(
							m_lengthBuffer))).readUnsignedShort()];
					}
					catch (IOException e)
					{
						/* cannot happen */
					}
				}
			}
			if ( (m_currentIndex >= 0) && (m_currentIndex < m_messageBuffer.length))
			{
				/* we've got the length -> read the message data */
				int messageBytesToRead = Math.min(m_messageBuffer.length - m_currentIndex,
												  a_packetData.length - currentPacketIndex);
				System.arraycopy(a_packetData, currentPacketIndex, m_messageBuffer, m_currentIndex,
								 messageBytesToRead);
				m_currentIndex = m_currentIndex + messageBytesToRead;
				currentPacketIndex = currentPacketIndex + messageBytesToRead;
			}
			if (m_currentIndex == m_messageBuffer.length)
			{
				/* we've read a whole message -> process it and prepare to read the next one */
				processMessage(m_messageBuffer); /** @todo react on unrecoverable errors */
				m_messageBuffer = new byte[0];
				m_currentIndex = -2;
			}
		}
	}

	protected abstract void processMessage(byte[] a_message);
}
