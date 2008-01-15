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
package proxy;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.PushbackInputStream;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.Hashtable;

import anon.infoservice.ProxyInterface;
import anon.AnonService;
import gui.JAPMessages;
import jap.JAPModel;
import jap.JAPUtil;
import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;
import anon.AnonServiceEventListener;
import java.net.ConnectException;
import anon.AnonChannel;
import anon.AnonService;
import anon.infoservice.ImmutableProxyInterface;
import anon.ErrorCodes;
import anon.AnonServerDescription;
import anon.tor.TorAnonServerDescription;
import anon.AnonServiceFactory;
import anon.infoservice.MixCascade;
import anon.IServiceContainer;
import anon.infoservice.IMutableProxyInterface;

final public class DirectProxy implements Runnable, AnonService
{
	private static final int REMEMBER_NOTHING = 0;
	private static final int REMEMBER_WARNING = 1;
	private static final int REMEMBER_NO_WARNING = 2;
	private static final int TEMPORARY_REMEMBER_TIME = 5000;

	private static AllowUnprotectedConnectionCallback ms_callback;

	private AnonService m_tor;
	private ServerSocket m_socketListener;
	private final Object THREAD_SYNC = new Object();
	private volatile Thread threadRunLoop;

	public DirectProxy(ServerSocket s)
	{
		m_socketListener = s;
	}

	public static void setAllowUnprotectedConnectionCallback(AllowUnprotectedConnectionCallback a_callback)
	{
		ms_callback = a_callback;
	}

	public static class RequestInfo
	{
		private String m_strURI;
		private String m_strMethod;
		private int m_port;

		protected RequestInfo(String a_strURI, String a_strMethod, int a_port)
		{
			m_strURI = a_strURI;
			m_strMethod = a_strMethod;
			m_port = a_port;
		}

		public String getURI()
		{
			return m_strURI;
		}

		public String getMethod()
		{
			return m_strMethod;
		}

		public int getPort()
		{
			return m_port;
		}
	}


	public static abstract class AllowUnprotectedConnectionCallback
	{
		public static class Answer
		{
			private boolean m_bRemembered;
			private boolean m_bAllow;

			public Answer(boolean a_bAllow, boolean a_bRemembered)
			{
				m_bAllow = a_bAllow;
				m_bRemembered = a_bRemembered;
			}

			public boolean isRemembered()
			{
				return m_bRemembered;
			}

			public boolean isAllowed()
			{
				return m_bAllow;
			}
		}

		public abstract Answer callback(RequestInfo a_requestInfo);
	}

	public AnonChannel createChannel(int a_type) throws ConnectException
	{
		return new AnonChannel()
		{
			public void close()
			{

			}

			public OutputStream getOutputStream()
			{
				return null;
			}

			public InputStream getInputStream()
			{
				return null;
			}

			public int getOutputBlockSize()
			{
				return 1;
			}
		};
	}

	public void addEventListener(AnonServiceEventListener l)
	{
	}

	public void removeEventListeners()
	{
	}

	public void removeEventListener(AnonServiceEventListener l)
	{
	}

	public int setProxy(IMutableProxyInterface a_proxyInterface)
	{
		return ErrorCodes.E_SUCCESS;
	}

	public boolean isConnected()
	{
		synchronized (THREAD_SYNC)
		{
			if (m_tor != null)
			{
				return m_tor.isConnected() && (threadRunLoop != null);
			}
			else
			{
				return threadRunLoop != null;
			}
		}
	}

	public int initialize(AnonServerDescription a_mixCascade, IServiceContainer a_serviceContainer)
	{
		if (! (a_mixCascade instanceof MixCascade) || a_mixCascade == null)
		{
			return ErrorCodes.E_INVALID_SERVICE;
		}
		m_tor = AnonServiceFactory.getAnonServiceInstance(AnonServiceFactory.SERVICE_TOR);
		//startService();
		return ErrorCodes.E_SUCCESS;
	}

	public synchronized boolean startService()
	{
		if (m_socketListener == null)
		{
			return false;
		}
		synchronized (THREAD_SYNC)
		{
			//stopService();
			//threadRunLoop.join();
			threadRunLoop = new Thread(this, "JAP - Direct Proxy");
			threadRunLoop.setDaemon(true);
			threadRunLoop.start();
			return true;
		}
	}

	public void run()
	{
		int remember = REMEMBER_NOTHING;
		Hashtable rememberedDomains = new Hashtable();
		boolean bShowHtmlWarning = true;
		Runnable doIt;
		RequestInfo requestInfo;

		try
		{
			m_socketListener.setSoTimeout(2000);
		}
		catch (Exception e1)
		{
			LogHolder.log(LogLevel.DEBUG, LogType.NET, "Could not set accept time out!", e1);
		}

		while (!Thread.currentThread().isInterrupted())
		{
			Socket socket = null;
			try
			{
				socket = m_socketListener.accept();
			}
			catch (InterruptedIOException e1)
			{
				Thread.yield();
				continue;
			}
			catch (SocketException e2)
			{
				LogHolder.log(LogLevel.ERR, LogType.NET, "Accept socket exception: " + e2);
				break;
			}
			catch (IOException a_e)
			{
				LogHolder.log(LogLevel.EXCEPTION, LogType.NET, "Socket could not accept!" + a_e);
				break;
			}

			try
			{
				socket.setSoTimeout(0); //Ensure socket is in Blocking Mode
			}
			catch (SocketException soex)
			{
				LogHolder.log(LogLevel.ERR, LogType.NET,
							  "Could not set socket to blocking mode! Excpetion: " + soex);
				socket = null;
				continue;
			}

			PushbackInputStream clientInputStream = null;

			try
			{
				clientInputStream = new PushbackInputStream(socket.getInputStream(), 200);
			}
			catch (IOException ex)
			{
			}

			if (remember == REMEMBER_NOTHING)
			{
				requestInfo = DirectProxyConnection.getURI(clientInputStream, 200);
				if (rememberedDomains.containsKey(requestInfo.getURI()))
				{
					bShowHtmlWarning = false;
				}
				else
				{
					AllowUnprotectedConnectionCallback.Answer answer;
					AllowUnprotectedConnectionCallback callback = ms_callback;
					if (callback != null)
					{
						answer = callback.callback(requestInfo);
					}
					else
					{
						answer = new AllowUnprotectedConnectionCallback.Answer(false, false);
					}
					bShowHtmlWarning = !answer.isAllowed();

					if (answer.isRemembered())
					{
						if (bShowHtmlWarning)
						{
							remember = REMEMBER_WARNING;
						}
						else
						{
							remember = REMEMBER_NO_WARNING;
						}
						rememberedDomains.clear();
					}
					else if (answer.isAllowed())
					{
						rememberedDomains.put(requestInfo.getURI(), requestInfo);
					}
				}
			}


			if (!bShowHtmlWarning && !JAPModel.isSmallDisplay())
			{
				if (getProxyInterface() != null && getProxyInterface().isValid() &&
					getProxyInterface().getProtocol() == ProxyInterface.PROTOCOL_TYPE_HTTP)
				{
					doIt = new DirectConViaHTTPProxy(socket, clientInputStream);
				}
				else
				{
					doIt = new DirectProxyConnection(socket, clientInputStream, this);
				}
				Thread thread = new Thread(doIt);
				thread.start();
			}
			else
			{
				Thread thread = new Thread(new SendAnonWarning(socket, clientInputStream));
				thread.start();
			}

		}
		LogHolder.log(LogLevel.INFO, LogType.NET, "Direct Proxy Server stopped.");

	}

	public synchronized void shutdown(boolean a_bResetTransferredBytes)
	{
		synchronized (THREAD_SYNC)
		{
			if (threadRunLoop == null)
			{
				return;
			}
			int i = 0;
			while (threadRunLoop.isAlive())
			{
				threadRunLoop.interrupt();
				Thread.yield();
				try
				{
					threadRunLoop.join(1000);
				}
				catch (InterruptedException e)
				{
					//LogHolder.log(LogLevel.ERR, LogType.NET, "Direct Proxy Server could not be stopped!!!");
				}
				if (i > 3)
				{
					try
					{
						threadRunLoop.stop();
					}
					catch (Exception e)
					{
					}
				}
				i++;
			}
			threadRunLoop = null;
		}
	}

	protected ImmutableProxyInterface getProxyInterface()
	{
		if (m_tor != null)
		{
			return JAPModel.getInstance().getTorProxyInterface();
		}
		else
		{
			return JAPModel.getInstance().getProxyInterface();
		}
	}

	/**
	 *  This class is used to inform the user that he tries to
	 *  send requests although anonymity mode is off.
	 */
	private final class SendAnonWarning implements Runnable
	{
		private Socket s;
		private SimpleDateFormat dateFormatHTTP;
		private InputStream m_clientInputStream;

		public SendAnonWarning(Socket s, InputStream a_clientInputStream)
		{
			this.s = s;
			m_clientInputStream = a_clientInputStream;
			dateFormatHTTP = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.US);
			dateFormatHTTP.setTimeZone(TimeZone.getTimeZone("GMT"));
		}

		public void run()
		{
			try
			{
				// read something so that the browser realises everything is OK
				if (m_clientInputStream != null)
				{
					m_clientInputStream.read();
				}
				else
				{
					s.getInputStream().read();
				}
			}
			catch (IOException a_e)
			{
				// ignored
			}

			try
			{
				String date = dateFormatHTTP.format(new Date());
				BufferedWriter toClient = new BufferedWriter(new OutputStreamWriter(s.getOutputStream()));
				toClient.write("HTTP/1.1 200 OK\r\n");
				toClient.write("Content-type: text/html\r\n");
				toClient.write("Expires: " + date + "\r\n");
				toClient.write("Date: " + date + "\r\n");
				toClient.write("Pragma: no-cache\r\n");
				toClient.write("Cache-Control: no-cache\r\n\r\n");
				toClient.write("<HTML><TITLE>JAP</TITLE>\n");
				toClient.write("<PRE>" + date + "</PRE>\n");
				toClient.write(JAPMessages.getString("htmlAnonModeOff"));
				toClient.write("</HTML>\n");
				toClient.flush();
				toClient.close();
				s.close();
			}
			catch (Exception e)
			{
				LogHolder.log(LogLevel.EXCEPTION, LogType.NET, "JAPFeedbackConnection: Exception: " + e);
			}
		}
	}

	/**
	 *  This class is used to transfer requests via the selected proxy
	 */
	private final class DirectConViaHTTPProxy implements Runnable
	{
		private Socket m_clientSocket;
		private InputStream m_clientInputStream;

		public DirectConViaHTTPProxy(Socket s, InputStream a_clientInputStream)
		{
			m_clientSocket = s;
			m_clientInputStream = a_clientInputStream;
		}

		public void run()
		{
			try
			{
				// open stream from client
				InputStream inputStream;
				if (m_clientInputStream != null)
				{
					inputStream = m_clientInputStream;
				}
				else
				{
					inputStream = m_clientSocket.getInputStream();
				}

				// create Socket to Server
				Socket serverSocket = new Socket(getProxyInterface().getHost(),
												 getProxyInterface().getPort());
				// Response from server is transfered to client in a sepatate thread
				DirectProxyResponse pr = new DirectProxyResponse(serverSocket.getInputStream(),
					m_clientSocket.getOutputStream());
				Thread prt = new Thread(pr, "JAP - DirectProxyResponse");
				prt.start();
				// create stream --> server
				OutputStream outputStream = serverSocket.getOutputStream();

				// Transfer data client --> server
				//first check if we use authorization for the proxy
				if (getProxyInterface().isAuthenticationUsed())
				{ //we need to insert an authorization line...
					//read first line and after this insert the authorization
					String str = DirectProxyConnection.readLine(inputStream);
					str += "\r\n";
					outputStream.write(str.getBytes());
					str = getProxyInterface().getProxyAuthorizationHeaderAsString();
					outputStream.write(str.getBytes());
					outputStream.flush();
				}
				byte[] buff = new byte[1000];
				int len;
				while ( (len = inputStream.read(buff)) != -1)
				{
					if (len > 0)
					{
						outputStream.write(buff, 0, len);
						outputStream.flush();
					}
				}
				prt.join();
				outputStream.close();
				inputStream.close();
				serverSocket.close();
			}
			catch (IOException ioe)
			{
			}
			catch (Exception e)
			{
				LogHolder.log(LogLevel.EXCEPTION, LogType.NET, "JAPDirectConViaProxy: Exception: " + e);
			}
		}
	}
}
