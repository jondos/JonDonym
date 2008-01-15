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

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.Socket;
import java.net.URL;
import java.net.URLConnection;
import java.util.Enumeration;

import jpi.PIRequest;
import jpi.db.DBInterface;
import jpi.db.DBSupplier;
import jpi.util.HttpServer;
import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;
import HTTPClient.Util;
import jpi.Configuration;

/**
 * This class verifies received PayPal IPNs and updates the database.
 *
 * @author Tobias Bayer
 */

public class IncomingPayPalConnection implements Runnable
{
	private Socket m_socket;
	private DataInputStream m_inStream;
	private DataOutputStream m_outStream;

	public IncomingPayPalConnection(Socket a_socket)
	{
		try
		{
			m_socket = a_socket;
			m_inStream = new DataInputStream(a_socket.getInputStream());
			m_outStream = new DataOutputStream(a_socket.getOutputStream());
		}
		catch (Exception e)
		{
			LogHolder.log(LogLevel.EXCEPTION, LogType.PAY, "IncomingPayPalConnection could not initialize.");
		}
	}

	public void run()
	{
		try
		{
			HttpServer server = new HttpServer(m_inStream, m_outStream);

			PIRequest request;
			while (true)
			{
				try
				{
					//Get POST Request from Paypal
					request = server.parseRequest();

					if (request == null)
					{
						break;
					}
					LogHolder.log(LogLevel.INFO, LogType.PAY, "Received IPN");

					Enumeration names = request.getParameterNames();

					while (names.hasMoreElements())
					{
						String name = (String) names.nextElement();
						LogHolder.log(LogLevel.DEBUG, LogType.PAY,
									  name + " = " + request.getParameter(name));
					}

					//Post back to PayPal
					URL url;
					URLConnection urlConn;
					DataOutputStream printout;
					DataInputStream input;
					url = new URL("https://www.sandbox.paypal.com/cgi-bin/webscr");
					urlConn = url.openConnection();
					urlConn.setDoInput(true);
					urlConn.setDoOutput(true);
					urlConn.setUseCaches(false);
					printout = new DataOutputStream(urlConn.getOutputStream());
					String content = "cmd=_notify-validate";
					names = request.getParameterNames();

					while (names.hasMoreElements())
					{
						String name = (String) names.nextElement();
						content += "&" + name + "=" + request.getParameter(name);

					}
					LogHolder.log(LogLevel.DEBUG, LogType.PAY, "Posting back to PayPal: " + content);
					printout.writeBytes(content);
					printout.flush();
					printout.close();
					// Get response data.
					input = new DataInputStream(urlConn.getInputStream());
					String str;
					String response = "";
					while (null != ( (str = input.readLine())))
					{
						response += str;
					}
					input.close();

					if (response.indexOf("VERIFIED") != -1)
					{
						LogHolder.log(LogLevel.INFO, LogType.PAY,
									  "Payment verified by PayPal, checking payment data");
						if (request.getParameter("payment_status").indexOf("Completed") != -1)
						{
							LogHolder.log(LogLevel.INFO, LogType.PAY, "Payment is completed");
							/**@todo check mc_currency*/
							//Charge account
							long transfernum = Long.parseLong(request.getParameter("item_name"));
							double amount = Double.parseDouble(request.getParameter("mc_gross"));
							amount *= 100;

							/*
							//account is charged with money directly, so no conversion to bytes necessary here
							double ratePerMB = Configuration.getRatePerMB();
							amount = 1000*1000 * (amount / ratePerMB);
							*/
							DBInterface db = DBSupplier.getDataBase();
							LogHolder.log(LogLevel.INFO, LogType.PAY,
										  "Charging account with  " + (long) amount + " kBytes");
							db.chargeAccount(transfernum, (long) amount);
						}

					}

				}
				catch (Exception e)
				{
					LogHolder.log(LogLevel.EXCEPTION, LogType.PAY, e);
					break;
				}
			}
		}
		catch (Exception e)
		{
			LogHolder.log(LogLevel.EXCEPTION, LogType.PAY, "IncomingPayPalConnection accept loop exception.");
		}

	}
}
