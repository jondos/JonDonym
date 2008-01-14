/*
 Copyright (c) 2000 - 2006, The JAP-Team
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
/**********************************************************************
 This file is part of the Java Payment instance (JPI)
 Anonymity and Privacy on the Internet

 http://anon.inf.tu-dresden.de

 **********************************************************************/


package jpi;

import jpi.db.DBInterface;
import anon.pay.xml.XMLErrorMessage;
import logging.LogHolder;
import logging.LogLevel;
import jpi.db.DBSupplier;
import logging.LogType;

/**
 * Listens for incoming connections from the micropayment.de CAll 2 pay system
 *
 * Sole purpose is to charge a user's account upon receiving a connection
 *
 * URL should be kept secret,
 * for additional security check the incoming connection's IP
 * (should be 195.159.183.235 (as of 18.12.2006) but subject to change)
 *
 *
 * @author Elmar Schraml
 */
public class PICommandMicropayment implements PICommand
{
	private static final String apiUrl = "/evunm8q"; //keep this secret from the users!!

	private DBInterface m_Database;


	public PIAnswer next(PIRequest request)
	{
		PIAnswer reply = null;

		// open the DB
		try
		{
			m_Database = DBSupplier.getDataBase();
		}
		catch (Exception e)
		{
			LogHolder.log(LogLevel.ALERT, LogType.PAY, "PICommandMC: could not get Database connection!");
			return new PIAnswer(
				PIAnswer.TYPE_CLOSE,
				new XMLErrorMessage(XMLErrorMessage.ERR_INTERNAL_SERVER_ERROR, "database problem")
				);
		}
		if (request.data != null && request.data.length > 0)
		{
			LogHolder.log(LogLevel.DEBUG, LogType.PAY, "request data: " + new String(request.data));
		}

		//handle requests
		if (request.url.equals(apiUrl) )
		{
			LogHolder.log(LogLevel.DEBUG, LogType.PAY, "payment received");
			reply = new PIAnswer(PIAnswer.TYPE_PASSIVE_PAYMENT, creditAccount(request) );
		}
		else
		{
			LogHolder.log(LogLevel.ERR, LogType.PAY, "invalid URL, possible effort to cheat!");
			reply = new PIAnswer(PIAnswer.TYPE_ERROR,new XMLErrorMessage(XMLErrorMessage.ERR_BAD_REQUEST));
		}
		//TODO: change to proper reply to micropayment.de server
		return reply;
	}

	private XMLErrorMessage creditAccount(PIRequest request)
	{
		//get transfernumber and amount from request url
		try
		{
			long transfernumber = Long.parseLong(request.getParameter("tan"));
			long amount = Long.parseLong(request.getParameter("amount"));
			//charge account
			m_Database.chargeAccount(transfernumber,amount);
		}
		catch (Exception e)
		{
			LogHolder.log(LogLevel.ERR, LogType.PAY, "error handling payment url");
			return new XMLErrorMessage(XMLErrorMessage.ERR_BAD_REQUEST);
		}
		return new XMLErrorMessage(XMLErrorMessage.ERR_OK);
	}

}
