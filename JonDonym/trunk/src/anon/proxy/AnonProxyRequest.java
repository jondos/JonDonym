/*
 Copyright (c) 2000, The JAP-Team
 All rights reserved.
 Redistribution and use in source and binary forms, with or without modification,
 are permitted provided that the following conditions are met:

 - Redistributions of source code must retain the above copyright notice,
 this list of conditions and the following disclaimer.

 - Redistributions in binary form must reproduce the above copyright notice,
 this list of conditions and the following disclaimer in the documentation and/or
 other materials provided with the distribution.

 - Neither the name of the University of Technology Dresden, Germany nor the names of its contributors
 may be used to endorse or promote products derived from this software without specific
 prior written permission.


 THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS ``AS IS'' AND ANY EXPRESS
 OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY
 AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE REGENTS OR CONTRIBUTORS
 BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA,
 OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER
 IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
 OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE
 */
package anon.proxy;

import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.io.IOException;
import java.net.Socket;

import anon.AnonChannel;
import anon.NotConnectedToMixException;
import anon.TooMuchDataForPacketException;
import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;

public final class AnonProxyRequest implements Runnable
{
	private static int ms_nrOfRequests = 0;

	private static final long TIMEOUT_RECONNECT = 60000;
	private static final int CHUNK_SIZE = 1000;
	
	private static int ms_currentRequest;

	private InputStream m_InChannel;

	private OutputStream m_OutChannel;

	private InputStream m_InSocket;

	private OutputStream m_OutSocket;

	private Socket m_clientSocket;

	private Thread m_threadResponse;

	private Thread m_threadRequest;

	private AnonChannel m_Channel;

	private AnonProxy m_Proxy;

	private volatile boolean m_bRequestIsAlive;

	private int m_iProtocol;

	private Object m_syncObject;
	
	private ProxyCallbackHandler m_callbackHandler = null;
	
	/* indicates that the JonDo has to perform the contentEncoding
	 * itself. May happen if, due to the header replacement, the JonDo
	 * specifies content encodings which the client does not support.
	 */
	private String[] contentEncodings;
	private boolean internalEncodingRequired = false;
	
	AnonProxyRequest(AnonProxy proxy, Socket clientSocket, Object a_syncObject, ProxyCallbackHandler callbackHandler) throws IOException
	{
			m_Proxy = proxy;
			m_clientSocket = clientSocket;
			m_syncObject = a_syncObject;
			m_clientSocket.setSoTimeout(0); // just to ensure that threads will
			// stop - really no timeout ? We had 1000...
			m_InSocket = clientSocket.getInputStream();
			m_OutSocket = clientSocket.getOutputStream();
			m_threadRequest = new Thread(this, "JAP - AnonProxy Request "+Integer.toString(ms_currentRequest));
			ms_currentRequest++;
			m_callbackHandler = callbackHandler;
			
			m_threadRequest.setDaemon(true);
			m_threadRequest.start();
	}

	public static int getNrOfRequests()
	{
		return ms_nrOfRequests;
	}
	
	public void run()
	{
		ms_nrOfRequests++;
		m_bRequestIsAlive = true;
		AnonChannel newChannel = null;
		// Check for type
		int firstByte = 0;
		try
		{
			firstByte = m_InSocket.read();
		}
		catch (InterruptedIOException ex)
		{ // no request received so fare - assume
			// SMTP, where we have to sent something
			// first
			try
			{
				newChannel = m_Proxy.createChannel(AnonChannel.SMTP);
				m_iProtocol = IProxyListener.PROTOCOL_OTHER;
				if (newChannel == null)
				{
					closeRequest();
					return;
				}
			}
			catch (Throwable to)
			{
				LogHolder.log(LogLevel.ERR, LogType.NET,
					"AnonProxyRequest - something was wrong with seting up a new SMTP channel -- Exception: " +
							  to);
				closeRequest();
				return;
			}
		}
		catch (Throwable t)
		{
			closeRequest();
			return;
		}
		if (newChannel == null) // no SMTP - maybe HTTP or SOCKS
		{
			firstByte &= 0x00FF;
			for (;!Thread.currentThread().isInterrupted(); )
			{
				try
				{
					newChannel = null;
					if (firstByte == 4 || firstByte == 5) // SOCKS
					{
						newChannel = m_Proxy.createChannel(AnonChannel.SOCKS);
						m_iProtocol = IProxyListener.PROTOCOL_OTHER;
					}
					else
					{
						newChannel = m_Proxy.createChannel(AnonChannel.HTTP);
						m_iProtocol = IProxyListener.PROTOCOL_WWW;
					}
					break;
				}
				catch (NotConnectedToMixException ec)
				{
					LogHolder.log(LogLevel.ERR, LogType.NET, "AnonProxyRequest - Connection to Mix lost");

					Thread timeoutThread = new Thread(new Runnable()
					{
						public void run()
						{
							m_Proxy.reconnect();
						}
					}, "Request reconnect thread");
					timeoutThread.start();
					// set timeout for this request
					long currentTime = System.currentTimeMillis();
					try
					{

						timeoutThread.join(TIMEOUT_RECONNECT);
					}
					catch (InterruptedException ex1)
					{
						Thread.currentThread().interrupt();
					}

					boolean success = true;
					synchronized (m_syncObject) // synchronize with connection thread
					{
						if (!m_Proxy.isConnected() && !Thread.currentThread().isInterrupted())
						{
							long remainder = currentTime + TIMEOUT_RECONNECT - System.currentTimeMillis();
							if (remainder > 0)
							{
								try
								{
									m_syncObject.wait(remainder);
								}
								catch (InterruptedException ex2)
								{
									Thread.currentThread().interrupt();
									break;
								}
							}
						}

						if (!m_Proxy.isConnected())
						{
							success = false;
						}
					}

					if (!success)
					{
						// reconnect failed
						LogHolder.log(LogLevel.ERR, LogType.NET,
									  "Requests terminated due to loss of connection to service!");
						closeRequest();
						return;
					}
				}
				catch (Exception e)
				{
					LogHolder.log(LogLevel.ERR, LogType.NET,
								  "AnonProxyRequest - something was wrong with seting up a new channel Exception: " + e);
					closeRequest();
					return;
				}
			}
			if (newChannel == null)
			{
				closeRequest();
				return;
			}
		}
		int len = 0;
		int aktPos = 0;
		if (firstByte != 0) // only SOCKS and HTTP will read the first byte - but
		// not SMTP!
		{
			aktPos = 1;
		}
		byte[] buff = null;
		ProxyCallbackBuffer pcBuffer = null;
		try
		{
			m_InChannel = newChannel.getInputStream();
			m_OutChannel = newChannel.getOutputStream();
			m_Channel = newChannel;

			m_threadResponse = new Thread(new Response(), "JAP - AnonProxy Response for "+Thread.currentThread().getName());
			m_threadResponse.start();

			buff = new byte[1900];
			buff[0] = (byte) firstByte;
		}
		catch (Throwable t)
		{
			closeRequest();
			return;
		}
		m_Proxy.incNumChannels();
		try
		{
			for (; ; )
			{
				try
				{
					len = Math.min(m_Channel.getOutputBlockSize(), 1900);
					len -= aktPos;
					len = m_InSocket.read(buff, aktPos, len);
					len += aktPos;
				}
				catch (InterruptedIOException ioe)
				{
					aktPos = aktPos + ioe.bytesTransferred;
					continue;
				}
				if (len <= 0)
				{
					break;
				}
				try
				{
					if(m_callbackHandler != null)
					{
						pcBuffer =
							new ProxyCallbackBuffer(buff, 0, len);
						try 
						{
							m_callbackHandler.deliverUpstream(this, pcBuffer);
						} 
						catch (ProxyCallbackDelayException e) 
						{
							aktPos = 0;
							continue;
						}
						m_OutChannel.write(pcBuffer.getChunk(), 0, 
								pcBuffer.getPayloadLength());
					}
					else
					{
						m_OutChannel.write(buff, 0, len);
					}
					/* everything was OK */
					aktPos = 0;
				}
				catch (TooMuchDataForPacketException e)
				{
					/* The implementation could not send all bytes.
					 * Reason: AnonChannel.getOutputBlockSize() is sometimes only a hint
					 * about the estimated size, the available size can be smaller (also
					 * some implementations need to know, whether there are more bytes
					 * available -> they request more bytes than they can send to find
					 * it out).
					 * Solution: Send the remaining bytes again in the next write-call.
					 */
					if(m_callbackHandler != null)
					{
						/*callback processing has most likely caused this exception
						 * and needs another handling.
						 */
						sendRemainingBytesRecursion(pcBuffer, e.getBytesSent(), m_OutChannel);
						//Postcondition: aktPos is always 0, because we sent the whole chunk away; 
						aktPos = 0;
					}
					else
					{
						byte[] tempBuff = new byte[buff.length - e.getBytesSent()];
						System.arraycopy(buff, e.getBytesSent(), tempBuff, 0, tempBuff.length);
						System.arraycopy(tempBuff, 0, buff, 0, tempBuff.length);
						aktPos = tempBuff.length;
					}
				}
				// LogHolder.log(LogLevel.DEBUG,LogType.NET,"Channel
				// "+Integer.toString(m_Channel.hashCode())+" Request Len: "+re+" Read:
				// "+len);
				m_Proxy.transferredBytes(len - aktPos, m_iProtocol);
				Thread.yield();
			}
		}
		catch (IOException e)
		{
			LogHolder.log(LogLevel.DEBUG,LogType.NET,"Exception in AnonProxyRequest - upstream loop.", e );
		}
		catch ( ProxyCallbackNotProcessableException cnpe)
		{
			try 
			{
				m_OutSocket.write(cnpe.getErrorResponse());
			} 
			catch (IOException e) 
			{
			}
			LogHolder.log(LogLevel.ERR,LogType.NET,"chunk could not be processed. Terminating",  cnpe );
		}
		closeRequest();
		m_Proxy.decNumChannels();
	}

	private static void sendRemainingBytesRecursion(ProxyCallbackBuffer pcBuffer, 
													int sentBytes, 
													OutputStream outputStream) throws IOException
	{
		byte[] tempBuff = new byte[pcBuffer.getPayloadLength() - sentBytes];
		System.arraycopy(pcBuffer.getChunk(), sentBytes, tempBuff, 0, tempBuff.length);
		System.arraycopy(tempBuff, 0, pcBuffer.getChunk(), 0, tempBuff.length);
		try
		{
			outputStream.write(tempBuff);
		}
		catch(TooMuchDataForPacketException e)
		{
			pcBuffer.setChunk(tempBuff);
			sendRemainingBytesRecursion(pcBuffer, e.getBytesSent(), outputStream);
		}
	}
	
	private synchronized void closeRequest()
	{
		if (m_bRequestIsAlive)
		{
			ms_nrOfRequests--;
			m_bRequestIsAlive = false;
		}
		try
		{
			if (m_Channel != null)
			{
				m_Channel.close();
			}
		}
		catch (Throwable t)
		{
		}
		try
		{
			m_InSocket.close();
		}
		catch (Throwable t)
		{
		}
		try
		{
			m_OutSocket.close();
		}
		catch (Throwable t)
		{
		}
		try
		{
			m_clientSocket.close();
		}
		catch (Throwable t)
		{
		}
		if(m_callbackHandler != null)
		{
			m_callbackHandler.closeRequest(this);
		}
	}

	public boolean isInternalEncodingRequired() 
	{
		return internalEncodingRequired;
	}

	public void setInternalEncodingRequired(boolean internalEncodingRequired) 
	{
		this.internalEncodingRequired = internalEncodingRequired;
	}

	public String[] getContentEncodings() 
	{
		return contentEncodings;
	}

	public void setContentEncodings(String[] contentEncodings) 
	{
		this.contentEncodings = contentEncodings;
	}

	final class Response implements Runnable
	{
		Response()
		{
		}

		public void run()
		{
			int len = 0;
			byte[] buff = new byte[2900];
			
			try 
			{	
				ProxyCallbackBuffer pcBuffer = null;
mainLoop:		do
				{
					len = m_InChannel.read(buff, 0, CHUNK_SIZE);
					if(len <= 0)
					{
						break;
					}
					int count = 0;
					for (; ; )
					{
						try
						{
							if(m_callbackHandler != null)
							{
								//  strange looking but tests if a 
								//  a proxy callback correctly
								//  processes chunk offsets
								//  byte[] nchunk = new byte[len+10];
//								for (int i = 0; i < 10; i++) 
//								{
//									nchunk[i] = (byte)'e';
//								}
//								System.arraycopy(buff, 0, nchunk, 10, len);
//								pcBuffer = new ProxyCallbackBuffer(nchunk, 10, len+10);
								
								pcBuffer = new ProxyCallbackBuffer(buff, 0, len);
								try 
								{
									m_callbackHandler.deliverDownstream(AnonProxyRequest.this, pcBuffer);
								} 
								catch (ProxyCallbackDelayException e) 
								{
									continue mainLoop;
								}
								m_OutSocket.write(pcBuffer.getChunk(), 0, pcBuffer.getPayloadLength());
								if(pcBuffer.getStatus() == ProxyCallback.STATUS_FINISHED)
								{
									break mainLoop;
								}
								//belongs to the above mentioned offset test
//								byte[] nnchunk = new byte[pcBuffer.getPayloadLength()-10];
//								System.arraycopy(pcBuffer.getChunk(), 10, nnchunk, 0, pcBuffer.getPayloadLength()-10);
//								m_OutSocket.write(nnchunk, 0, pcBuffer.getPayloadLength()-10);
							}
							else
							{
								m_OutSocket.write(buff, 0, len);
							}
							
							m_OutSocket.flush();
							break;
						}
						catch (InterruptedIOException ioe)
						{
							LogHolder.log(LogLevel.EMERG, LogType.NET,
										  "Should never be here: Timeout in sending to Browser!");
						}
						count++;
						if (count > 3)
						{
							throw new IOException("Could not send to Browser...");
						}
					}
					m_Proxy.transferredBytes(len, m_iProtocol);
					Thread.yield();
				}	while (len > 0 && !m_Channel.isClosed()); 
			}
			catch (IOException e)
			{
				LogHolder.log(LogLevel.ERR, LogType.NET, e);
			}
			catch (ProxyCallbackNotProcessableException cnpe)
			{
				LogHolder.log(LogLevel.ERR, LogType.NET, cnpe);
				try 
				{
					m_OutSocket.write(cnpe.getErrorResponse());
				} 
				catch (IOException e) 
				{
				}
			}
			try
			{
				m_clientSocket.close();
			}
			catch (IOException e)
			{
			}
			try
			{
				Thread.sleep(500);
			}
			catch (InterruptedException e)
			{
			}
			if (m_bRequestIsAlive)
			{
				m_threadRequest.interrupt();
			}
			buff = null;
		}
	}
}
