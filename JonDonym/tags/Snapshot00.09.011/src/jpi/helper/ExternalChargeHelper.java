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
import jpi.Configuration;
import anon.crypto.tinytls.TinyTLSServer;
import anon.crypto.MyDSAPrivateKey;
import jpi.util.HttpServer;
import java.io.DataOutputStream;
import java.io.DataInputStream;
import jpi.PIRequest;
import anon.pay.xml.XMLExternalChargeRequest;
import jpi.db.DBSupplier;
import jpi.db.DBInterface;
import java.util.Enumeration;
import anon.crypto.JAPCertificate;
import jpi.PIAnswer;
import anon.pay.xml.XMLErrorMessage;
import org.bouncycastle.crypto.digests.MD5Digest;
import anon.crypto.ByteSignature;

/**
 * This class starts a server, which listens for PayPal
 * Instant Payment Notifications (IPN).
 *
 * @author Tobias Bayer
 */
public class ExternalChargeHelper implements Runnable
{
	private int m_listenPort;
	private ServerSocket m_serverSocket;

	public ExternalChargeHelper(int a_listenPort)
	{
		m_listenPort = a_listenPort;
	}

	public void run()
	{
		try
		{
			LogHolder.log(LogLevel.DEBUG, LogType.PAY,
						  "ExternalChargeHelper listening on port " + m_listenPort);
			TinyTLSServer tlssock = new TinyTLSServer(m_listenPort);
			JAPCertificate cert = Configuration.getOwnCertificate();
			if (cert == null)
			{
				System.out.println("Own cert is NULL!");
			}

			MyDSAPrivateKey key = (MyDSAPrivateKey) Configuration.getPrivateKey();
			if (key == null)
			{
				System.out.println("Private key is NULL!");
			}

			tlssock.setDSSParameters(Configuration.getOwnCertificate(),
									 (MyDSAPrivateKey) Configuration.getPrivateKey());
			m_serverSocket = tlssock;

		}
		catch (Exception e)
		{
			LogHolder.log(LogLevel.EXCEPTION, LogType.PAY, e);
			return;
		}

		Socket acceptedSocket;
		ExternalCharge externalCharge;
		while (true)
		{
			LogHolder.log(LogLevel.DEBUG, LogType.PAY, "ExternalChargeHelper: Waiting for charge request...");
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
							  "ExternalChargeHelper: connection from: " + strRemote);

				externalCharge = new ExternalCharge(acceptedSocket);
				Thread t=new Thread(externalCharge,"ExternalChargeHelper");
				t.setDaemon(true);
				t.start();
			}
			catch (Exception e)
			{
				LogHolder.log(LogLevel.ALERT, LogType.PAY,
							  "ExternalChargeHelper accept loop exception: " + e.getMessage());
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

	private class ExternalCharge implements Runnable
	{
		private DataInputStream m_inStream;
		private DataOutputStream m_outStream;

		public ExternalCharge(Socket a_socket)
		{
			try
			{
				m_inStream = new DataInputStream(a_socket.getInputStream());
				m_outStream = new DataOutputStream(a_socket.getOutputStream());
			}
			catch (Exception e)
			{
				LogHolder.log(LogLevel.EXCEPTION, LogType.PAY,
							  "ExternalCharge could not initialize.");
			}
		}

		public void run()
		{
			try
			{
				HttpServer server = new HttpServer(m_inStream, m_outStream);

				PIRequest request;

				try
				{
					//Get POST Request from external client
					request = server.parseRequest();
					LogHolder.log(LogLevel.INFO, LogType.PAY, "Received Charge Request");
					XMLExternalChargeRequest chargeReq = new XMLExternalChargeRequest(request.data);

					String password = chargeReq.getPassword();
					MD5Digest md5 = new MD5Digest();
					md5.update(password.getBytes(), 0, password.getBytes().length);
					byte[] hash = new byte[md5.getDigestSize()];

					md5.doFinal(hash, 0);
					String hpassword = ByteSignature.toHexString(hash);

					if (hpassword.equalsIgnoreCase(Configuration.getExternalChargePassword()))
					{
						//Charge
						Enumeration chargeLines = chargeReq.getChargeLines();
						while (chargeLines.hasMoreElements())
						{
							String[] chargeLine = (String[]) chargeLines.nextElement();
							long transfernum = Long.parseLong(chargeLine[0]);
							double amount = Double.parseDouble(chargeLine[2]);
							amount *= 100;
							/*

							double ratePerMB = Configuration.getRatePerMB();
							amount = 1000 * 1000 * (amount / ratePerMB);
							*/
							DBInterface db = DBSupplier.getDataBase();

							LogHolder.log(LogLevel.INFO, LogType.PAY,
										  "Charging account with  " + (long) amount + " kBytes");
							db.chargeAccount(transfernum, (long) amount);
						}
						PIAnswer reply = new PIAnswer(PIAnswer.TYPE_CONFIRM,
							new XMLErrorMessage(XMLErrorMessage.ERR_OK));
						server.writeAnswer(200, reply.getContent());
					}
					else
					{
						LogHolder.log(LogLevel.ERR, LogType.PAY, "Wrong password for external charging!");
						PIAnswer reply = new PIAnswer(PIAnswer.TYPE_CONFIRM,
							new XMLErrorMessage(XMLErrorMessage.ERR_INTERNAL_SERVER_ERROR));
						server.writeAnswer(200, reply.getContent());
					}

				}
				catch (Exception e)
				{
					e.printStackTrace();

				}
			}

			catch (Exception e)
			{
				e.printStackTrace();
			}
		}
	}
}
