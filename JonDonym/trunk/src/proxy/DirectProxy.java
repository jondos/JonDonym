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
import jap.JAPModel;
import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;
import anon.AnonServiceEventListener;
import java.net.ConnectException;
import anon.AnonChannel;
import anon.infoservice.ImmutableProxyInterface;
import anon.ErrorCodes;
import anon.AnonServerDescription;
import anon.AnonServiceFactory;
import anon.infoservice.MixCascade;
import anon.IServiceContainer;
import anon.infoservice.IMutableProxyInterface;
import anon.util.JAPMessages;

final public class DirectProxy implements Runnable, AnonService
{
	private static final String GENERAL_RULE = "*";
	
	private static AllowProxyConnectionCallback ms_callback;

	private AnonService m_tor;
	private ServerSocket m_socketListener;
	private final Object THREAD_SYNC = new Object();
	private volatile Thread threadRunLoop;

	public DirectProxy(ServerSocket s)
	{
		m_socketListener = s;
	}

	public static void setAllowUnprotectedConnectionCallback(AllowProxyConnectionCallback a_callback)
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


	public static abstract class AllowProxyConnectionCallback
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
			public boolean isClosed()
			{
				return false;
			}
			
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
			if (threadRunLoop != null && threadRunLoop.isAlive())
			{
				return true;
			}
			
			shutdown(true);
			threadRunLoop = new Thread(this, "JAP - Direct Proxy");
			threadRunLoop.setDaemon(true);
			threadRunLoop.start();
			return true;
		}
	}

	public void run()
	{
		Hashtable rememberedDomains = new Hashtable();
		RequestInfo requestInfo;
		RememberedRequestRight requestRight;

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

			requestInfo = DirectProxyConnection.getURI(clientInputStream, 200);
			requestRight = (RememberedRequestRight)rememberedDomains.get(GENERAL_RULE); // if a rule for all requests is set
			if (requestRight == null && !JAPModel.getInstance().isAskForAnyNonAnonymousRequest())
			{
				rememberedDomains.clear();
			}
			if (requestRight != null && //(requestRight.isTimedOut() || JAPModel.getInstance().isAskForAnyNonAnonymousRequest()))
				requestRight.isTimedOut())
			{
				rememberedDomains.remove(GENERAL_RULE);
				requestRight = null;
			}
			if (requestRight == null)
			{
				requestRight = (RememberedRequestRight)rememberedDomains.get(requestInfo.getURI());
			}
			if (requestRight != null && requestRight.isTimedOut())
			{
				rememberedDomains.remove(requestInfo.getURI());
				requestRight = null;
			}
			if (requestRight == null)
			{
				AllowProxyConnectionCallback.Answer answer;
				AllowProxyConnectionCallback callback = ms_callback;
				if (callback != null)
				{
					answer = callback.callback(requestInfo);
				}
				else
				{
					answer = new AllowProxyConnectionCallback.Answer(false, false);
				}
				
				
				if (JAPModel.getInstance().isAskForAnyNonAnonymousRequest() && !answer.isRemembered())
				{
					requestRight = new RememberedRequestRight(requestInfo.getURI(), 
							!answer.isAllowed(), false);
					rememberedDomains.put(requestInfo.getURI(), requestRight);
				}
				else
				{
					requestRight = new RememberedRequestRight(GENERAL_RULE, 
							!answer.isAllowed(), false);
					rememberedDomains.clear();
					rememberedDomains.put(GENERAL_RULE, requestRight);					
				}
			}

			if (!requestRight.isWarningShown() && !JAPModel.isSmallDisplay())
			{
				if (getProxyInterface() != null && getProxyInterface().isValid() &&
					getProxyInterface().getProtocol() == ProxyInterface.PROTOCOL_TYPE_HTTP)
				{
					new Thread(new DirectConViaHTTPProxy(socket, clientInputStream)).start();
				}
				else
				{
					new DirectProxyConnection(socket, clientInputStream, this);
				}
			}
			else
			{
				Thread thread = new Thread(new SendAnonWarning(socket, clientInputStream, 
						(rememberedDomains.size() > 0) ? requestRight: null));
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
	private static final class SendAnonWarning implements Runnable
	{
		private static final String MSG_BLOCKED = 
			SendAnonWarning.class.getName() + "_blocked";
		private static final String MSG_BLOCKED_ALL = 
			SendAnonWarning.class.getName() + "_blockedAll";
		private static final String MSG_BLOCKED_DOMAIN = 
			SendAnonWarning.class.getName() + "_blockedDomain";
		private static final String MSG_COUNTDOWN = 
			SendAnonWarning.class.getName() + "_countdown";	
		private static final String MSG_RELOAD = 
			SendAnonWarning.class.getName() + "_reload";	
		private static final String MSG_BLOCKED_PERMANENTLY = 
			SendAnonWarning.class.getName() + "_blockedPermanently";	
		private static final String MSG_ANON_MODE_OFF = 
			SendAnonWarning.class.getName() + "_htmlAnonModeOff";
		
		
		private Socket s;
		private SimpleDateFormat dateFormatHTTP;
		private InputStream m_clientInputStream;
		private RememberedRequestRight m_requestRight;

		public SendAnonWarning(Socket s, InputStream a_clientInputStream, 
				RememberedRequestRight a_requestRight)
		{
			this.s = s;
			m_requestRight = a_requestRight;
			m_clientInputStream = a_clientInputStream;
			dateFormatHTTP = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.US);
			dateFormatHTTP.setTimeZone(TimeZone.getTimeZone("GMT"));
		}

		public void run()
		{
			long countDown;
			String[] addedMessage;
			String blockedMessage;
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
				if (m_requestRight == null)
				{
					toClient.write(JAPMessages.getString(MSG_ANON_MODE_OFF,
							JAPMessages.getString(
									jap.JAPConfAnonGeneral.MSG_DENY_NON_ANONYMOUS_SURFING)));
				}
				else
				{
					countDown = m_requestRight.getCountDown();
					
					if (m_requestRight.getURI().equals(GENERAL_RULE))
					{
						blockedMessage = JAPMessages.getString(MSG_BLOCKED_ALL);
					}
					else
					{
						blockedMessage = JAPMessages.getString(MSG_BLOCKED_DOMAIN, 
								"<code>" + m_requestRight.getURI() + "</code>");
					}
					
					if (countDown == Long.MAX_VALUE)
					{
						addedMessage = new String[]{blockedMessage, 
								JAPMessages.getString(MSG_BLOCKED_PERMANENTLY) + 
								"<BR>" + JAPMessages.getString(MSG_RELOAD)};
					}
					else if (countDown / 1000 == 0)
					{
						addedMessage = new String[]{blockedMessage, 
								JAPMessages.getString(MSG_RELOAD)};
					}
					else 
					{
						addedMessage = new String[]{blockedMessage, 
								JAPMessages.getString(MSG_COUNTDOWN, 
										new String[]{"" + (countDown / 1000),
											"<BR>" + JAPMessages.getString(MSG_RELOAD)})};
					}
					toClient.write(JAPMessages.getString(MSG_BLOCKED, addedMessage));
				}
				toClient.write("</HTML>\n");
				toClient.flush();
				toClient.close();
				s.close();
			}
			catch (Exception e)
			{
				LogHolder.log(LogLevel.EXCEPTION, LogType.NET, e);
			}
		}
	}

	private final class RememberedRequestRight
	{		
		public static final boolean REMEMBER_WARNING = true;
		public static final boolean REMEMBER_NO_WARNING = false;		
		public static final boolean SET_TIMEOUT = true;
		public static final boolean SET_UNLIMITED = false;
	
		private static final long TEMPORARY_REMEMBER_TIME = 60000l;
		private static final long TEMPORARY_REMEMBER_TIME_NO_WARNING = 300000l;
		
		private long m_timeRemembered;
		private boolean m_bWarn;
		private String m_URI;
		
		public RememberedRequestRight(String a_URI, boolean a_bWarn, boolean a_bTimeout)
		{
			m_URI = a_URI;
			if (a_bTimeout)
			{
				if (a_bWarn)
				{
					m_timeRemembered = System.currentTimeMillis() + TEMPORARY_REMEMBER_TIME;
				}
				else
				{
					m_timeRemembered = System.currentTimeMillis() + TEMPORARY_REMEMBER_TIME_NO_WARNING;
				}
			}
			else
			{
				m_timeRemembered = Long.MAX_VALUE;
			}
			
			m_bWarn = a_bWarn;
		}
		
		public String getURI()
		{
			return m_URI;
		}
		
		public boolean isWarningShown()
		{
			return m_bWarn;
		}
		
		public long getCountDown()
		{
			if (m_timeRemembered == Long.MAX_VALUE)
			{
				return Long.MAX_VALUE;
			}
			
			long countDown = m_timeRemembered - System.currentTimeMillis();
			if (countDown < 0)
			{
				countDown = 0;
			}
			return countDown;
		}
		
		public boolean isTimedOut()
		{
			return m_timeRemembered < System.currentTimeMillis();
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
				// Response from server is transfered to client in a separate thread
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
