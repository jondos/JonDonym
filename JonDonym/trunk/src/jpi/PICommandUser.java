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
/**********************************************************************
 This file is part of the Java Payment instance (JPI)
 Anonymity and Privacy on the Internet

 http://anon.inf.tu-dresden.de

 **********************************************************************/

package jpi;

import java.util.Enumeration;
import java.util.Random;

import anon.crypto.IMyPublicKey;
import anon.crypto.XMLSignature;
import anon.crypto.ByteSignature;
import anon.pay.xml.XMLAccountCertificate;
import anon.pay.xml.XMLAccountInfo;
import anon.pay.xml.XMLBalance;
import anon.pay.xml.XMLChallenge;
import anon.pay.xml.XMLCloseAck;
import anon.pay.xml.XMLEasyCC;
import anon.pay.xml.XMLErrorMessage;
import anon.pay.xml.XMLJapPublicKey;
import anon.pay.xml.XMLPassivePayment;
import anon.pay.xml.XMLPaymentOptions;
import anon.pay.xml.XMLResponse;
import anon.pay.xml.XMLTransCert;
import anon.pay.xml.XMLTransactionOverview;
import anon.util.IXMLEncodable;
import anon.util.XMLUtil;
import jpi.db.Balance;
import jpi.db.DBInterface;
import jpi.db.DBSupplier;
import jpi.util.XMLCaptcha;
import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;
import java.util.Vector;
import jpi.helper.DummyCreditCardHelper;
import org.w3c.dom.Document;
import anon.util.XMLParseException;
import anon.util.Base64;
import jpi.helper.PaysafecardHandler;
import anon.pay.xml.XMLVolumePlans;
import anon.pay.xml.XMLVolumePlanPurchase;
import anon.pay.xml.XMLVolumePlan;

/**
 * This class contains the functionality for talking to a JAP. For
 * each JAP connecting to us, one {@link PICommandUser} instance is
 * created to answer this user's requests
 */
public class PICommandUser implements PICommand
{
	private int m_iCurrentState;
	private byte[] m_arbChallenge;
	private Random m_SecureRandom;
	private DBInterface m_Database;

	private XMLAccountCertificate m_accountCertificate;
	private IMyPublicKey m_publicKey;

	// constants for the connection state
	static final int STATE_INIT = 0; // connection just initiated
	static final int STATE_REG_SEND = 10; // ?
	static final int STATE_REG_CHA_SENT = 12; // waiting for response to challenge from user
	static final int STATE_AUTH_SEND = 20; // ?
	static final int STATE_AUTH_CHA_SENT = 22; // waiting for response to challenge from user
	static final int STATE_AUTH_OK = 24; // login procedure done, auth ok

	private static final int CAPTCHA_KEY_BITS = 48;
	private static final int EXTRA_KEY_BITS = 16;

	/**
	 * Erzeugt und initialisiert ein {@link PICommandUser} Objekt.
	 *
	 */
	public PICommandUser()
	{
		init();
		m_SecureRandom = new Random(System.currentTimeMillis());
	}

	void init()
	{
		m_iCurrentState = STATE_INIT;
		m_arbChallenge = null;
		m_publicKey = null;
		m_accountCertificate = null;
	}

	/**
	 * Generates the appropriate answer for a http request. This is
	 * called by the {@link PIConnection} instance for each incoming
	 * request from a JAP.
	 *
	 * In the initial state, the user can authenticate or register a
	 * new account. After authentication is complete, the user can
	 * send one and only one @c /charge or @c /balance request. Then
	 * the connection is reset to initial state. Users can then
	 * authenticate again (e.g. using a different account num) or
	 * close the connection by sending a @c /close request.
	 *
	 * @return answer as {@link PIAnswer} object
	 */
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
			LogHolder.log(LogLevel.ALERT, LogType.PAY, "PICommandUser: could not get Database connection!");
			return new PIAnswer(
				PIAnswer.TYPE_CLOSE,
				new XMLErrorMessage(XMLErrorMessage.ERR_INTERNAL_SERVER_ERROR, "database problem")
				);
		}

		// send back a simple CloseAck. PIConnection will recognize
		// this special answer and will close the socket
		if (request.method.equals("GET") &&
			request.url.equals("/close"))
		{
			return new PIAnswer(PIAnswer.TYPE_CLOSE, new XMLCloseAck());
		}

		LogHolder.log(LogLevel.DEBUG, LogType.PAY,
					  "received " + request.method + " command from user: " + request.url);
		if (request.data != null && request.data.length > 0)
		{
			LogHolder.log(LogLevel.DEBUG, LogType.PAY, "request data: " + new String(request.data));
		}

		//getting

		switch (m_iCurrentState)
		{
			// Initial state: the ssl connection was just
			// established.  now JAP can either send a /register
			// request to register a new account or /auth to
			// authenticate with a public key that is known
			// already
			case STATE_INIT:

				// register a new account
				if (request.method.equals("POST") && request.url.equals("/register"))
				{
					XMLJapPublicKey keyParser = null;
					try
					{
						keyParser = new XMLJapPublicKey(request.data);
						m_publicKey = keyParser.getPublicKey();
					}
					catch (Exception e)
					{
						LogHolder.log(LogLevel.EXCEPTION, LogType.PAY, e);
						reply = PIAnswer.getErrorAnswer(XMLErrorMessage.ERR_BAD_REQUEST,
							"Error while generating challenge: " + e.getMessage());
					}
					try
					{
						reply = new PIAnswer(PIAnswer.TYPE_CAPTCHA_REQUEST, getCreationChallengeXML());
						//	reply = new PIAnswer(PIAnswer.TYPE_CHALLENGE_REQUEST, getChallengeXML());
						m_iCurrentState = STATE_REG_CHA_SENT;
					}
					catch (Exception e)
					{
						LogHolder.log(LogLevel.EXCEPTION, LogType.PAY, e);
						reply = PIAnswer.getErrorAnswer(XMLErrorMessage.ERR_INTERNAL_SERVER_ERROR);
					}
				}

				// authentication for a known account
				else if (request.method.equals("POST") && request.url.equals("/authenticate"))
				{
					try
					{
						testCertificate(request.data);
						reply = new PIAnswer(PIAnswer.TYPE_CHALLENGE_REQUEST,
											 getChallengeXML()); // send challenge for challenge-response authentication
						m_iCurrentState = STATE_AUTH_CHA_SENT;
					}
					catch (Exception e)
					{
						LogHolder.log(LogLevel.EXCEPTION, LogType.PAY, e);
						reply = PIAnswer.getErrorAnswer(XMLErrorMessage.ERR_INTERNAL_SERVER_ERROR);
					}
				}
				break;

				// A new account was created and we now got
				// the response to our challenge.
			case STATE_REG_CHA_SENT:
				if (request.method.equals("POST") && request.url.equals("/response"))
				{
					if (!verifyResponse(request.data))
					{
						reply = PIAnswer.getErrorAnswer(XMLErrorMessage.ERR_BAD_SIGNATURE,
							"Invalid response to challenge");
					}
					else
					{
						try
						{
							// Response was correct. Send back the signed certificate
							reply = new PIAnswer(PIAnswer.TYPE_ACCOUNT_CERTIFICATE,
												 generateAccountCertificate()
								);
							init(); // registration complete, goto state init again, user can now authenticate
						}
						catch (Exception e)
						{
							LogHolder.log(LogLevel.EXCEPTION, LogType.PAY, e);
							reply = PIAnswer.getErrorAnswer(XMLErrorMessage.ERR_INTERNAL_SERVER_ERROR);
						}
					}
				}
				break;

				// A user has authenticated with his certificate and
				// we now got the response to our challenge
			case STATE_AUTH_CHA_SENT:
				if (request.method.equals("POST") && request.url.equals("/response"))
				{
					if (!verifyResponse(request.data))
					{
						reply = PIAnswer.getErrorAnswer(XMLErrorMessage.ERR_BAD_SIGNATURE,
							"Invalid response to challenge");
					}
					else
					{
						reply = new PIAnswer(PIAnswer.TYPE_AUTHENTICATION_SUCCESS,
											 new XMLErrorMessage(XMLErrorMessage.ERR_OK));
						m_iCurrentState = STATE_AUTH_OK;
					}
				}
				break;

				// authenticaten is complete. The user can now send the real requests
			case STATE_AUTH_OK:

				// The user has requested a transfer number (TAN)
				if (request.method.equals("GET") && request.url.equals("/charge"))
				{
					try
					{
						reply = new PIAnswer(PIAnswer.TYPE_TRANSFER_CERTIFICATE,
											 getTransCert());
						// go to state init again, user can authenticate again with different account
						init();
					}
					catch (Exception e)
					{
						LogHolder.log(LogLevel.EXCEPTION, LogType.PAY, e);
						reply = PIAnswer.getErrorAnswer(XMLErrorMessage.ERR_INTERNAL_SERVER_ERROR);
					}
				}

				else if (request.method.equals("GET") && request.url.equals("/balance"))
				{
					try
					{
						reply = new PIAnswer(PIAnswer.TYPE_BALANCE, getBalance());
						// go to state init again, user can authenticate again with different account
						init();
					}
					catch (Exception ex)
					{
						LogHolder.log(LogLevel.EXCEPTION, LogType.PAY, ex);
						reply = PIAnswer.getErrorAnswer(XMLErrorMessage.ERR_INTERNAL_SERVER_ERROR);
					}
				}

				else if (request.method.equals("GET") && request.url.equals("/paymentoptions"))
				{
					try
					{
						reply = new PIAnswer(PIAnswer.TYPE_PAYMENT_OPTIONS, getPaymentOptions());
						// go to state init again, user can authenticate again with different account
						init();
					}
					catch (Exception ex)
					{
						LogHolder.log(LogLevel.EXCEPTION, LogType.PAY, ex);
						reply = PIAnswer.getErrorAnswer(XMLErrorMessage.ERR_INTERNAL_SERVER_ERROR);
					}
				}

				else if (request.method.equals("GET") && request.url.equals("/volumeplans"))
				{
					try
					{
						reply = new PIAnswer(PIAnswer.TYPE_VOLUME_PLANS, getVolumePlans());
						// go to state init again, user can authenticate again with different account
						init();
					}
					catch (Exception ex)
					{
						LogHolder.log(LogLevel.EXCEPTION, LogType.PAY, ex);
						reply = PIAnswer.getErrorAnswer(XMLErrorMessage.ERR_INTERNAL_SERVER_ERROR);
					}
				}


				else if (request.method.equals("GET") && request.url.equals("/paymentsettings"))
				{
					try
					{
						reply = new PIAnswer(PIAnswer.TYPE_PAYMENT_SETTINGS , getPaymentSettings());
						// go to state init again, user can authenticate again with different account
						init();
					}
					catch (Exception ex)
					{
						LogHolder.log(LogLevel.EXCEPTION, LogType.PAY, ex);
						reply = PIAnswer.getErrorAnswer(XMLErrorMessage.ERR_INTERNAL_SERVER_ERROR);
					}
				}


				else if (request.method.equals("GET") && request.url.equals("/paymentdata"))
				{
					try
					{
						reply = new PIAnswer(PIAnswer.TYPE_PASSIVE_PAYMENT, getPaymentData(request.data) );
						// go to state init again, user can authenticate again with different account
						init();
					}
					catch (Exception ex)
					{
						LogHolder.log(LogLevel.EXCEPTION, LogType.PAY, ex);
						reply = PIAnswer.getErrorAnswer(XMLErrorMessage.ERR_INTERNAL_SERVER_ERROR);
					}
				}


				else if (request.method.equals("POST") && request.url.equals("/transactionoverview"))
				{
					try
					{
						reply = new PIAnswer(PIAnswer.TYPE_TRANSACTION_OVERVIEW,
											 getTransactionOverview(request.data));
						// go to state init again, user can authenticate again with different account
						init();
					}
					catch (Exception ex)
					{
						LogHolder.log(LogLevel.EXCEPTION, LogType.PAY, ex);
						reply = PIAnswer.getErrorAnswer(XMLErrorMessage.ERR_INTERNAL_SERVER_ERROR);
					}
				}
				else if (request.method.equals("POST") && request.url.equals("/passivepayment"))
				{
					try
					{
						reply = new PIAnswer(PIAnswer.TYPE_PASSIVE_PAYMENT,
											 storePassivePayment(request.data));
						// go to state init again, user can authenticate again with different account
						init();
					}
					catch (Exception ex)
					{
						LogHolder.log(LogLevel.EXCEPTION, LogType.PAY, ex);
						reply = PIAnswer.getErrorAnswer(XMLErrorMessage.ERR_INTERNAL_SERVER_ERROR);
					}
				}
				else if (request.method.equals("POST") && request.url.equals("/buyflat"))
				{
					try
					{
						reply = new PIAnswer(PIAnswer.TYPE_FLATRATE, buyFlatrate(request.data));
						// go to state init again, user can authenticate again with different account
						init();
					}
					catch (Exception ex)
					{
						LogHolder.log(LogLevel.EXCEPTION, LogType.PAY, ex);
						reply = PIAnswer.getErrorAnswer(XMLErrorMessage.ERR_INTERNAL_SERVER_ERROR);
					}
				}
				break;
		}
		if (reply == null)
		{
			reply = PIAnswer.getErrorAnswer(XMLErrorMessage.ERR_BAD_REQUEST);
		}
		return reply;
	}

	/**
	 * Creates a XMLPaymentOptions structure from the config file
	 * @return IXMLEncodable
	 */
	private IXMLEncodable getPaymentOptions()
	{
		XMLPaymentOptions paymentOptions = Configuration.getPaymentOptions();

		return paymentOptions;
	}

	private IXMLEncodable getVolumePlans()
	{
		XMLVolumePlans volumePlans = Configuration.getVolumePlans();
		return volumePlans;
	}

	private IXMLEncodable getPaymentSettings()
	{
		return m_Database.getPaymentSettings();
	}

	private IXMLEncodable getPaymentData(byte[] data)
	{
		XMLPassivePayment paymentdata = null;
		//transfernumber is a long, but in the sql statement we'll need it as String again, no point in converting back and forth
		String transfernumber = new String(data);
		DBInterface db = null;
				try
				{
					db = DBSupplier.getDataBase();
				}
				catch (Exception e)
				{
					LogHolder.log(LogLevel.EXCEPTION, LogType.PAY,
								  "Could not connect to Database:" + e.getMessage());
				}
				if (db != null)
				{
					paymentdata = db.getPassivePaymentData(transfernumber);

				}


		return paymentdata;
	}

	/**
	 * Stores a PassivePayment object the user has sent to the database
	 * @param a_data byte[]
	 * @return IXMLEncodable
	 */
	private IXMLEncodable storePassivePayment(byte[] a_data)
	{
		XMLPassivePayment pp = null;
		IXMLEncodable result = null;
		try
		{
			pp = new XMLPassivePayment(a_data);
		}
		catch (Exception e)
		{
			LogHolder.log(LogLevel.EXCEPTION, LogType.PAY, "Could not parse XMLPassivePayment");
			result =  new XMLErrorMessage(XMLErrorMessage.ERR_WRONG_FORMAT);
		}
		//if payment is made by paysafecard,call different method to store in different table
		boolean isPaysafecard = pp.getPaymentName().equalsIgnoreCase("paysafecard");
		if (isPaysafecard)
		{
			return storePaysafecardPayment(pp);
		}

		//if payment is made by coupon, call specific method to redeem it
		if (pp.getPaymentName().equalsIgnoreCase("coupon"))
		{
			result = handleCoupon(pp);
		}

		//strings in passivepayment were XML-encoded, decode before handing it to the database
		String key;
		String curData;
		String decodedData;
		for (Enumeration allData = pp.getPaymentDataKeys(); allData.hasMoreElements(); )
		{
			key = (String) allData.nextElement();
			curData = pp.getPaymentData(key);
			//the manually added keys dont need to be decoded again //TODO: clean this up
			if (! (key.equals("accountnumber") || key.equals("volumeplan")) )
			{
				decodedData = Base64.decodeToString(curData);
				//pp's data is a hashtable ->setting data with the same key will overwrite the old data with the new decoded data
				pp.addData(key, decodedData);
			} else
			{
				pp.addData(key,curData);
			}
		}

		/** Store in database*/
		DBInterface db = null;
		try
		{
			db = DBSupplier.getDataBase();
			db.storePassivePayment(pp);
		}
		catch (Exception e)
		{
			LogHolder.log(LogLevel.EXCEPTION, LogType.PAY,
						  "Could not store passive payment data in database!");
			result =  new XMLErrorMessage(XMLErrorMessage.ERR_INTERNAL_SERVER_ERROR);

		}
		if (!DummyCreditCardHelper.getInstance().chargePending())
		{
			LogHolder.log(LogLevel.ERR, LogType.PAY,
						  "Could not directly charge pending payments!");
			result = new  XMLErrorMessage(XMLErrorMessage.ERR_INTERNAL_SERVER_ERROR);
		}

		if (result != null && ((XMLErrorMessage) result).getErrorCode() != XMLErrorMessage.ERR_OK ) //an error occured upon payment
		{
			return result; //finish by sending the corresponding XMLErrorMessage
		}
		else //payment was okay
		{
			//use it to buy the volume plan
			String planName = pp.getPaymentData("volumeplan");
			String accNumAsString = pp.getPaymentData("accountnumber");
			long accountNumber = Long.parseLong(accNumAsString);
			XMLVolumePlanPurchase newPlan = new XMLVolumePlanPurchase(accountNumber, planName);
			return buyVolumePlan(newPlan);
		}

	}

	/**
	 * storePaysafecardPayment
	 * not to be called directly from the request data,
	 * rather called by storePassivePayment if the XMLPassivepayment sent
	 * turns out ot contain a payment via Paysafecard
	 * (because it gets stored in a different database table,
	 * plus we need to contact the Paysafecard server to create a disposition)
	 *
	 * @param pp XMLPassivePayment
	 * @return IXMLEncodable
	 */
	private IXMLEncodable storePaysafecardPayment(XMLPassivePayment pp)
	{
		boolean allOk = true;
		if (! PaysafecardHandler.createDisposition(pp) )
		{
			allOk = false;
		}
		try
		{
			DBInterface db = DBSupplier.getDataBase();
			db.storePaysafecardPayment(pp);
		}
		catch (Exception e)
		{
			allOk = false;
		}
		if (allOk)
		{
			return new XMLErrorMessage(XMLErrorMessage.ERR_OK,"paysafecard payment was stored");
		}
		else
		{
			return new XMLErrorMessage(XMLErrorMessage.ERR_DATABASE_ERROR,"could not store psc payment");
		}
	}

	private IXMLEncodable handleCoupon(XMLPassivePayment pp)
	{
		//check coupon code for validity
		String code = pp.getPaymentData("code");
		boolean isValid;
		DBInterface db;
		try
		{
			db = DBSupplier.getDataBase();
			isValid = db.checkValidity(code);
		} catch (Exception e)
		{
			return new XMLErrorMessage(XMLErrorMessage.ERR_DATABASE_ERROR,"Database error, could not check validity");
		}
		if ( ! isValid )
		{
			return new XMLErrorMessage(XMLErrorMessage.ERR_INVALID_CODE,"Coupon code is not valid");
		}
		//credit account
		try
		{
			long tan = pp.getTransferNumber();
			db.redeemCoupon(code,tan);
		} catch (Exception e)
		{
			return new XMLErrorMessage(XMLErrorMessage.ERR_DATABASE_ERROR,"Redeeming the coupon failed");
		}
		return new XMLErrorMessage(XMLErrorMessage.ERR_OK, "Coupon was redeemed");
	}

	/**
	 * Fills the transaction overview XML structure with values from the database.
	 * @param a_data byte[]
	 * @return IXMLEncodable
	 */
	private IXMLEncodable getTransactionOverview(byte[] a_data)
		{
			XMLTransactionOverview overview = null;
			Vector tans = null;
			try
			{
				overview = new XMLTransactionOverview(a_data);
				tans = (Vector) overview.getTans().clone();
			}
			catch (Exception e)
			{
				LogHolder.log(LogLevel.EXCEPTION, LogType.PAY,
							  "Could not create XMLTransactionOverview from POST data:" + e.getMessage());
				return null;
			}

			for (int i=0;i<tans.size();i++)
			{
				String[] line = (String[]) tans.elementAt(i);
				//Get "used" and "date" attribute from database
				DBInterface db = null;
				try
				{
					db = DBSupplier.getDataBase();
				}
				catch (Exception e)
				{
					LogHolder.log(LogLevel.EXCEPTION, LogType.PAY,
								  "Could not connect to Database:" + e.getMessage());
				}
				if (db != null)
				{
					boolean used = db.isTanUsed(Long.parseLong(line[0]));
					long usedDate = db.getUsedDate(Long.parseLong(line[0]));
					long amount = db.getTransferAmount(Long.parseLong(line[0]));
					overview.setUsed(Long.parseLong(line[0]), used, usedDate, amount);
				}
			}
			return overview;
		}

	/**
	 * Generates a random challenge in {@link XMLChallenge} format.
	 * @return challenge XML data as array of bytes
	 */
	private IXMLEncodable getChallengeXML() throws Exception
	{
		m_arbChallenge = new byte[222];
		m_SecureRandom.nextBytes(m_arbChallenge);
		XMLChallenge xmlChallenge = new XMLChallenge(m_arbChallenge);
		String chStr = XMLUtil.toString(XMLUtil.toXMLDocument(xmlChallenge));
		m_arbChallenge = xmlChallenge.getChallengeForSigning();
		return xmlChallenge;
	}

	private IXMLEncodable getCreationChallengeXML() throws Exception
	{
		XMLChallenge xmlChallenge = (XMLChallenge) getChallengeXML();
		XMLCaptcha captcha = new XMLCaptcha(xmlChallenge.getChallengeForSigning(), CAPTCHA_KEY_BITS, EXTRA_KEY_BITS);
		return captcha;
	}

	/**
	 * Verifies the response from the challenge-response authentication
	 * @retval true if the response is correct
	 * @retval false if the response is invalid
	 */
	boolean verifyResponse(byte[] data)
	{
		try
		{
			XMLResponse response = new XMLResponse(new String(data));
			boolean b = ByteSignature.verify(m_arbChallenge, response.getResponse(), m_publicKey);
			return b;
		}
		catch (Exception e)
		{
			LogHolder.log(LogLevel.EXCEPTION, LogType.PAY, e);
			return false;
		}
	}

	/**
	 * generates a new
	 * certificate and stores it in the DB.
	 * @return certificate as {@link XMLAccountCertificate}
	 */
	private IXMLEncodable generateAccountCertificate() throws Exception
	{
		String strCert = null;
		long accountNum = m_Database.getNextAccountNumber();
		LogHolder.log(LogLevel.DEBUG, LogType.PAY, "generateCertificate(): new accountnr is: " + accountNum);
		java.sql.Timestamp creationTime = new java.sql.Timestamp(System.currentTimeMillis());

		XMLAccountCertificate xmlcert = new XMLAccountCertificate(
			m_publicKey, accountNum, creationTime,
			Configuration.getBiID());

		xmlcert.sign(Configuration.getPrivateKey());

		XMLJapPublicKey keyFormatter = new XMLJapPublicKey(m_publicKey);
		String strXmlKey = XMLUtil.toString(XMLUtil.toXMLDocument(keyFormatter));

		// debugging
		strCert = XMLUtil.toString(XMLUtil.toXMLDocument(xmlcert));
		m_Database.addAccount(accountNum, strXmlKey, creationTime,
							  strCert);
		return xmlcert;
	}

	/**
	 * Generates a tranfer certificate (TAN) as answer to a /charge
	 * request.  Stores the transfer number and the corresponding
	 * account no. in the db
	 * @param data request xml data, not used in this version
	 * @return transfer certificate xml data
	 * @todo Pruefen ob noch eine offen transaktion l\uFFFDuft --> wenn ja certificat nochmal senden
	 */
	private IXMLEncodable getTransCert() throws Exception
	{
		long accountNum = m_accountCertificate.getAccountNumber();
		Balance bal = m_Database.getBalance(accountNum);
		LogHolder.log(LogLevel.DEBUG, LogType.MISC, "Got balance. deposit=" + bal.deposit);

		// generate a transfer number (TAN)
		long transNumber = m_Database.getNextTransferNumber();
		LogHolder.log(LogLevel.DEBUG, LogType.MISC, "Transnumber=" + bal.deposit);

		// transfer cert is valid for 30 days
		long time = System.currentTimeMillis() +
			30l * 24l * 60l * 60l * 1000l;
		java.sql.Timestamp validto = new java.sql.Timestamp(time);

		// store transfer number in DB
		m_Database.storeTransferNumber(transNumber, accountNum, bal.deposit, validto);

		// generate xml tranfer cert and sign it
		XMLTransCert xmlcert =
			new XMLTransCert(accountNum, transNumber, bal.deposit, validto);
		xmlcert.sign(Configuration.getPrivateKey());
		return xmlcert;
	}

	/**
	 * generates a XMLAccountInfo certificate as response to a /balance request
	 */
	IXMLEncodable getBalance() throws Exception
	{
		long accountnumber = m_accountCertificate.getAccountNumber();

		XMLBalance xmlbal = m_Database.getXmlBalance(accountnumber);
		xmlbal.sign( Configuration.getPrivateKey() );
		XMLAccountInfo info = new XMLAccountInfo(xmlbal);
		Vector allCcs = m_Database.getCostConfirmations(accountnumber);
		for (Enumeration e = allCcs.elements(); e.hasMoreElements(); )
		{
			info.addCC( (XMLEasyCC) e.nextElement() );
		}

		return info;
	}

	/**
	 * buyFlatrate: deduct price of a flatrate from account, and set flatrate
	 * @deprecated : was used for a single flatrate, to buy one of several offered volume plans,
	 * use buyVolumePlan()
	 *
	 * @param data byte[] : the accountnumber
	 * @return IXMLEncodable: an XMLErrorMessage with codes ERR_OK, ERR_INSUFFICIENT_BALANCE or ERR_NO_FLATRATE_OFFERED
	 */
	IXMLEncodable buyFlatrate(byte[] data)
	{
		long accountnumber = Long.parseLong(new String(data));
		return m_Database.buyFlatrate(accountnumber);
	}

	IXMLEncodable buyVolumePlan(byte[] data)
	{
		XMLVolumePlanPurchase purchaseRequest;
		IXMLEncodable result;
		try
		{
			purchaseRequest = new XMLVolumePlanPurchase(new String(data));
			result = buyVolumePlan(purchaseRequest);
		}
		catch (Exception e)
		{
			LogHolder.log(LogLevel.ERR, LogType.PAY,
					  "Could not construct a XMLVolumePlanPurchase from the request data sent");
			result =  new XMLErrorMessage(XMLErrorMessage.ERR_BAD_REQUEST);
		}
		return result;
}

	IXMLEncodable buyVolumePlan(XMLVolumePlanPurchase a_Plan)
	{
		String planName = a_Plan.getPlanName();
		XMLVolumePlan purchasedPlan = m_Database.getVolumePlan(planName);
		long accountNumber = a_Plan.getAccountNumber();
		XMLErrorMessage result = m_Database.buyVolumePlan(accountNumber, purchasedPlan);
		if (result.getErrorCode() != XMLErrorMessage.ERR_OK)
		{
			LogHolder.log(LogLevel.ERR, LogType.PAY, "Database reported error when trying to buy volume plan");
			return new XMLErrorMessage(XMLErrorMessage.ERR_DATABASE_ERROR);
		}
		else
		{
			return new XMLErrorMessage(XMLErrorMessage.ERR_OK);
		}
	}

	/**
	 * Checks whether a certificate with this public key is contained in our DB.
	 * @todo real checking of signature and timestamp !!
	 */
	private boolean testCertificate(byte[] data) //throws Exception
	{
		Document xmlData;
		try
		{
			xmlData = XMLUtil.toXMLDocument(data);

			if (XMLSignature.verify(xmlData, Configuration.getOwnCertificate().getPublicKey()) == null)
			{
				LogHolder.log(LogLevel.INFO, LogType.PAY, "Wrong certificate (invalid signature)");
				// error: certificate not contained in our DB
				return false;
			}
		}
		catch (XMLParseException ex)
		{
			LogHolder.log(LogLevel.ALERT, LogType.PAY,
						  "Internal Error in PICommandUser.testCertificate(): Wrong xml structure!", ex);
			return false;
		}

		XMLAccountCertificate xmlcert = null;
		try
		{
			xmlcert = new XMLAccountCertificate(new String(data));
		}
		catch (Exception ex1)
		{
			LogHolder.log(LogLevel.EXCEPTION, LogType.PAY, ex1);
			return false;
		}
		m_accountCertificate = xmlcert;
		m_publicKey = xmlcert.getPublicKey();
		return true;
	}
}
