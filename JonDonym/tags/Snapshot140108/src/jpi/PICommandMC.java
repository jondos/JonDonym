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
import anon.pay.xml.XMLMixAccountBalance;
import anon.crypto.XMLSignature;
import anon.pay.xml.XMLBankAccount;
import anon.pay.xml.XMLPriceCertificate;
import anon.pay.xml.XMLPriceCertificateList;
import java.util.Vector;
import anon.pay.xml.XMLTransferRequest;
import anon.pay.xml.XMLCloseAck;
import anon.util.XMLUtil;
import anon.pay.xml.XMLEmail;
import anon.util.EmailHandler;

/**
 * Handles communication of the PI with the MixConfig tool
 * (cf. PICommandAI and PICommandUser for the corresponding classes for communication with AI and JAP)
 * @author Elmar Schraml
 */
public class PICommandMC implements PICommand
{

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
		//start of giant if-else handling the various requests
		if (request.method.equals("GET") && request.url.equals("/test") )
		{
			//for testing purposes only
			LogHolder.log(LogLevel.DEBUG, LogType.PAY, "PI was contacted with a Get request");
			return new PIAnswer(PIAnswer.TYPE_ERROR, new XMLErrorMessage(XMLErrorMessage.ERR_OK) );
		}
		else if (request.method.equals("POST") && request.url.equals("/getbalance") )
		{
			reply = new PIAnswer(PIAnswer.TYPE_OPERATORBALANCE, getOperatorBalance(new String(request.data) ) );
		}
		else if (request.method.equals("POST") && request.url.equals("/getbankinfo") )
		{
			reply = new PIAnswer(PIAnswer.TYPE_BANKACCOUNT, getBankAccount(new String(request.data) ) );
		}
		else if (request.method.equals("POST") && request.url.equals("/getpricecerts") )
		{
			LogHolder.log(LogLevel.DEBUG, LogType.PAY, "PICommandMC handling /getpricecerts request");
			reply = new PIAnswer(PIAnswer.TYPE_PRICECERTS, getPriceCertificates(new String(request.data) ) );
		}
		else if (request.method.equals("POST") && request.url.equals("/getpricecertsformix") )
		{
			LogHolder.log(LogLevel.DEBUG, LogType.PAY, "PICommandMC handling /getpricecertsformix request");
			reply = new PIAnswer(PIAnswer.TYPE_PRICECERTS, getPriceCertificatesForMix(new String(request.data) ) );
		}
		else if (request.method.equals("POST") && request.url.equals("/setbankinfo") )
		{
			XMLErrorMessage result = storeBankInfo(request.data);
			reply = new PIAnswer(PIAnswer.TYPE_BANKACCOUNT, result);
			//return error message
		}
		else if (request.method.equals("POST") && request.url.equals("/transferrequest") )
		{
			reply = new PIAnswer(PIAnswer.TYPE_CONFIRM, handleTransferRequest(request.data));
		}
		else if (request.method.equals("POST") && request.url.equals("/submitpricecert") )
		{
			reply = new PIAnswer(PIAnswer.TYPE_CONFIRM, storePriceCert(request.data));
		}
		else if (request.method.equals("POST") && request.url.equals("/deletepricecert") )
		{
			reply = new PIAnswer(PIAnswer.TYPE_CONFIRM, deletePriceCert(request.data));
		}
		else if (request.method.equals("POST") && request.url.equals("/sendemail") )
		{
			reply = new PIAnswer(PIAnswer.TYPE_CONFIRM, sendEmail(request.data));
		}

	//end of giant if-else handling requests
		if (reply == null)
		{
			reply = PIAnswer.getErrorAnswer(XMLErrorMessage.ERR_BAD_REQUEST);
		}
		return reply;
	} // of method next

	private XMLMixAccountBalance getOperatorBalance(String operatorCert)
	{
		//get balance (handled by SQL in DataBase)
		int balance = m_Database.getOperatorBalance(operatorCert);
		java.sql.Timestamp lastUpdate = m_Database.getLastBalanceUpdate(operatorCert);
		//build IXMLEncodable
		XMLMixAccountBalance accountbalance = new XMLMixAccountBalance(balance,lastUpdate);
		//sign
		accountbalance.sign(Configuration.getPrivateKey() );
		return accountbalance;
	}

	private XMLBankAccount getBankAccount(String operatorCert)
	{
		//get Account Info
		String[] data = m_Database.getBankAccount(operatorCert);
		String type = data[0];
		String details = data[1];
		//build and IXMLEncodable
		XMLBankAccount accountData = new XMLBankAccount(type, details);
		return accountData;
	}

	private XMLPriceCertificateList getPriceCertificates(String operatorCert)
	{
		Vector theCerts = m_Database.getPriceCerts(operatorCert);
		XMLPriceCertificateList theList = new XMLPriceCertificateList(theCerts);
		LogHolder.log(LogLevel.DEBUG, LogType.PAY, "the list after constructor:"+XMLUtil.toString(XMLUtil.toXMLDocument(theList)) );
		return theList;
	}

	private XMLPriceCertificateList getPriceCertificatesForMix(String mixId)
	{
		Vector theCerts = m_Database.getPriceCertsForMix(mixId);
		XMLPriceCertificateList theList = new XMLPriceCertificateList(theCerts);
		LogHolder.log(LogLevel.DEBUG, LogType.PAY, "the list after constructor:"+XMLUtil.toString(XMLUtil.toXMLDocument(theList)) );
		return theList;
	}



	private XMLErrorMessage storePriceCert(byte[] requestdata)
	{
		XMLPriceCertificate thePriceCert = null;
		boolean saveOk = true;
		try
		{
			thePriceCert = new XMLPriceCertificate(requestdata);
			m_Database.storePriceCert(thePriceCert);
		}
		catch (Exception ex)
		{
			saveOk = false;
			LogHolder.log(LogLevel.DEBUG, LogType.PAY, "could not store XMLPriceCertificate");
		}finally
		{
			return new XMLErrorMessage(saveOk?XMLErrorMessage.ERR_OK:XMLErrorMessage.ERR_DATABASE_ERROR);
		}
	}

	private XMLErrorMessage deletePriceCert(byte[] requestdata)
	{
		XMLPriceCertificate thePriceCert = null;
		boolean deleteOk = true;
		try
		{
			thePriceCert = new XMLPriceCertificate(requestdata);
			m_Database.deletePriceCert(thePriceCert);
		}
		catch (Exception ex)
		{
			deleteOk = false;
			LogHolder.log(LogLevel.DEBUG, LogType.PAY, "could not delete XMLPriceCertificate");
		}finally
		{
			return new XMLErrorMessage(deleteOk?XMLErrorMessage.ERR_OK:XMLErrorMessage.ERR_DATABASE_ERROR);
		}
	}


	private XMLErrorMessage storeBankInfo(byte[] requestdata)
	{
		XMLBankAccount theBankAccount = null;
		boolean saveOk = true;
		try
		{
			theBankAccount = new XMLBankAccount(requestdata);
			m_Database.storeBankAccount(theBankAccount);
		}
		catch (Exception ex)
		{
			LogHolder.log(LogLevel.DEBUG, LogType.PAY, "could not get XMLBankAccount from http request");
			saveOk = false;
		} finally
		{
			return new XMLErrorMessage(saveOk?XMLErrorMessage.ERR_OK:XMLErrorMessage.ERR_DATABASE_ERROR);
		}
	}

	private XMLErrorMessage sendEmail(byte[] requestdata)
	{
		XMLEmail theMessage = null;
		boolean sendOk = true;
		try
		{
			theMessage = new XMLEmail(requestdata);
			if (theMessage == null)
			{
				throw new Exception("could not get XMLEmail from the data sent");
			}
			String opInfo = m_Database.getOperatorInfo(theMessage.getSenderIdentification());
			String returnAddress = theMessage.getReplyAddress();
			String bodyText = theMessage.getBodyText()+"\n\n\nInformation added by payment instance: \n"+opInfo;
			EmailHandler.sendSupportEmail(returnAddress,bodyText);

		}
		catch (Exception ex)
		{
			LogHolder.log(LogLevel.DEBUG, LogType.PAY, "could not get XMLBankAccount from http request");
			sendOk = false;
		} finally
		{
			return new XMLErrorMessage(sendOk?XMLErrorMessage.ERR_OK:XMLErrorMessage.ERR_INTERNAL_SERVER_ERROR);
		}
	}

	private XMLErrorMessage handleTransferRequest(byte[] requestdata)
	{
		boolean saveOk = true;
		XMLTransferRequest theTransferRequest = null;
		try {
			theTransferRequest = new XMLTransferRequest(requestdata);
			m_Database.handleTransferRequest(theTransferRequest);
		} catch (Exception e)
		{
			LogHolder.log(LogLevel.DEBUG, LogType.PAY, "could not get XMLTransferRequest from http request");
			saveOk = false;
		} finally
		{
			return new XMLErrorMessage(saveOk?XMLErrorMessage.ERR_OK:XMLErrorMessage.ERR_DATABASE_ERROR);
		}

	}

}
