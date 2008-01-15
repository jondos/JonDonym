/*
 Copyright (c) 2000 - 2004, The JAP-Team
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

package jpi.helper;

import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;

/**
 * This class starts a server, which listens for PayPal
 * Instant Payment Notifications (IPN).
 *
 * @author Tobias Bayer
 */
public class PayPalHelper implements Runnable
{
	private int m_listenPort;
	private ServerSocket m_serverSocket;

	public PayPalHelper(int a_listenPort)
	{
		m_listenPort = a_listenPort;
	}

	public void run()
	{
		try
		{
			LogHolder.log(LogLevel.DEBUG, LogType.PAY,
						  "PayPalHelper starting up on port " + m_listenPort);
			m_serverSocket = new ServerSocket(m_listenPort);
		}
		catch (Exception e)
		{
			LogHolder.log(LogLevel.EXCEPTION, LogType.PAY, e);
			return;
		}

		Socket acceptedSocket;
		IncomingPayPalConnection ippc;
		while (true)
		{
			LogHolder.log(LogLevel.DEBUG, LogType.PAY, "PayPalHelper: Waiting for IPN");
			try
			{
				acceptedSocket = m_serverSocket.accept();
				InetAddress remote = acceptedSocket.getInetAddress();
				String strRemote = "unknown";
				if (remote != null)
				{
					strRemote = remote.getHostAddress();
				}
				LogHolder.log(LogLevel.DEBUG, LogType.PAY,
							  "PayPalHelper: connection from: " + strRemote);

				ippc = new IncomingPayPalConnection(acceptedSocket);
				new Thread(ippc).start();
			}
			catch (Exception e)
			{
				LogHolder.log(LogLevel.ALERT, LogType.PAY,
							  "PayPalHelper accept loop exception: " + e.getMessage());
				LogHolder.log(LogLevel.EXCEPTION, LogType.PAY, e);
				try
				{
					Thread.sleep(10000);
				}
				catch (InterruptedException ex)
				{
				}
			}
		}

	}

}
