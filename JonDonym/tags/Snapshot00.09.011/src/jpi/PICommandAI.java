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
package jpi;

import java.util.Random;

import anon.pay.xml.XMLCloseAck;
import anon.pay.xml.XMLEasyCC;
import anon.pay.xml.XMLErrorMessage;
import anon.pay.xml.XMLJapPublicKey;
import anon.util.IXMLEncodable;
import anon.util.XMLUtil;
import jpi.db.DBInterface;
import jpi.db.DBSupplier;
import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;
import java.util.Vector;
import anon.pay.xml.XMLPriceCertificate;
import java.util.Enumeration;
import java.util.*;
import anon.util.EmailHandler;
import anon.crypto.JAPCertificate;
import anon.crypto.X509SubjectKeyIdentifier;
import java.sql.Timestamp;
import anon.crypto.XMLSignature;
import org.w3c.dom.Node;
import anon.util.XMLParseException;

/**
 * This class implements the high-level communication with the AI.
 * At the moment "settling" of costconfirmations is only possible.
 *
 * @author Bastian Voigt
 * @version 1.0
 * @todo implement challenge-response
 */
public class PICommandAI implements PICommand
{
	private int m_iState;

	/** takes the challenge we sent to the AI in order to check it later */
	private byte[] m_arbChallenge;

	/** @todo use one system-wide random source */
	private Random m_SecureRandom;

	/** interface to the database */
	private DBInterface m_Database;

	/** the name of the AI we are talking to */
	//private String m_aiName;

	static final int INIT = 0;
	static final int CHALLENGE_SENT = 1;
	static final int AUTHENTICATION_OK = 2;
	static final int AUTHENTICATION_BAD = 3;

	//if a non-pay Mix in a pay cascade has alreadey been emailed about it,
	//we store its subjectkeyidentifier here to avoid sending another email with each new CC that is settled
	private static final Vector notifiedMixes = new Vector();

	/**
	 * Erzeugt und initialisiert ein {@link PICommandAI} Objekt.
	 */
	public PICommandAI()
	{
		init();
		m_SecureRandom = new Random(System.currentTimeMillis());
	}

	void init()
	{
		m_iState = INIT;
		m_arbChallenge = null;
	}

	public PIAnswer next(PIRequest request)
	{
		PIAnswer reply = null;
		try
		{
			m_Database = DBSupplier.getDataBase();
		}
		catch (Exception e)
		{
			LogHolder.log(LogLevel.ALERT, LogType.PAY, "settle(): could not get Database connection!");
			return PIAnswer.getErrorAnswer(XMLErrorMessage.ERR_INTERNAL_SERVER_ERROR);
		}

		if (request.method.equals("GET") &&
			request.url.equals("/close"))
		{
			return new PIAnswer(PIAnswer.TYPE_CLOSE, new XMLCloseAck());
		}

		switch (m_iState)
		{
			case INIT:

				/*				// authentication for a known account
					if (request.method.equals("POST") && request.url.equals("/authenticate"))
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
					  LogHolder.log(LogLevel.EXCEPTION, LogType.PAY,e);
					  reply = PIAnswer.getErrorAnswer(XMLErrorMessage.ERR_INTERNAL_SERVER_ERROR);
					 }
					}
					break;

				   case CHALLENGE_SENT:
					break;

				   case AUTHENTICATION_BAD:
				 reply = new PIAnswer(PIAnswer.TYPE_CLOSE, new XMLErrorMessage(XMLErrorMessage.ERR_BAD_REQUEST));
					break;

				   case AUTHENTICATION_OK:*/
				if (request.method.equals("POST") && request.url.equals("/settle"))
				{
					LogHolder.log(LogLevel.DEBUG, LogType.MISC, "got settle request");
					reply = new PIAnswer(PIAnswer.TYPE_CLOSE, settle(request.data));
					break;
				}

				/*else if (request.method.equals("POST") &&
				 request.url.equals("/update"))
				   {
				 piAnswer = new PIAnswer(200,PIAnswer.TYPE_ACCOUNT_SNAPSHOT,
				 getAccountSnapshots(request.data));
				 break;
				   }*/

				/*					else if (request.method.equals("POST") &&
				   request.url.equals("/payoff"))
				  {
				   piAnswer = new PIAnswer(200,PIAnswer.TYPE_PAYOFF,
				   payoff(request.data));
				   break;
				  }
				 */
				/*					else if (request.method.equals("POST") &&
				   request.url.equals("/confirm"))
				  {
				   piAnswer = new PIAnswer(200,PIAnswer.TYPE_CONFIRM,
				   confirm(request.data));
				   break;
				  }
				 */
				reply = new PIAnswer(PIAnswer.TYPE_CLOSE,
									 new XMLErrorMessage(XMLErrorMessage.ERR_BAD_REQUEST,
					"I cannot understand your request"));
				break;

		}
		return reply;
	}

	/**
	 * Processes a collection of cost confirmations, stores them in the Database
	 * and updates the account credit for affected accounts.
	 *
	 * @param data byte[] the collection of CostConfirmations in XML format
	 * @return XMLErrorMessage an xml structure indicating success or failure
	 */
	private IXMLEncodable settle(byte[] data)
	{
		XMLEasyCC cc = null;
		try
		{
			cc = new XMLEasyCC(data);
			LogHolder.log(LogLevel.DEBUG, LogType.MISC,
						  "Parsed incoming CC: " + XMLUtil.toString(XMLUtil.toXMLDocument(cc)));
		}
		catch (Exception ex)
		{
			LogHolder.log(LogLevel.EXCEPTION, LogType.PAY, ex);
			return new XMLErrorMessage(XMLErrorMessage.ERR_WRONG_FORMAT,
									   "could not parse CC:" + ex.getMessage());
		}
		LogHolder.log(LogLevel.DEBUG, LogType.MISC, "settle request for cc #"+cc.getId() );

		// check CC signature
		XMLJapPublicKey keyParser = null;
		try
		{
			keyParser = new XMLJapPublicKey(m_Database.getXmlPublicKey(cc.getAccountNumber()));
		}
		catch (Exception ex2)
		{
			LogHolder.log(LogLevel.DEBUG, LogType.MISC, "settle request: Could not parse key");
			LogHolder.log(LogLevel.EXCEPTION, LogType.PAY, ex2);
			return new XMLErrorMessage(XMLErrorMessage.ERR_KEY_NOT_FOUND,
									   "Could not find a key for account " + cc.getAccountNumber() +
									   " in my DB");
		}
		LogHolder.log(LogLevel.DEBUG, LogType.MISC,
					  "Parsed Key: " + XMLUtil.toString(XMLUtil.toXMLDocument(keyParser.getPublicKey())));
		try
		{
			if (!cc.verify(keyParser.getPublicKey()))
			{
				// sig was bad
				LogHolder.log(LogLevel.DEBUG, LogType.MISC, "settle request: Bad signature");
				return new XMLErrorMessage(XMLErrorMessage.ERR_BAD_SIGNATURE);
			}
		}
		catch (Exception e)
		{
			LogHolder.log(LogLevel.ALERT, LogType.PAY, "settle(): Error while verifying signature");
			LogHolder.log(LogLevel.EXCEPTION, LogType.PAY, e);
			return new XMLErrorMessage(XMLErrorMessage.ERR_INTERNAL_SERVER_ERROR,
									   "Error while verifying signature");
		}
		//check price certificates
		//get price cert hashes from cc
		Hashtable priceCertElements = cc.getPriceCertElements(); //getPriceCertElements = Hashtable including mix ids, vs. getPriceCerts = Enumeration of only the pc hashes
		Vector ccPriceCerts = new Vector();
		String oneCertHash;
		XMLPriceCertificate completeCert;
		//get corresponding price certs from database
		for (Enumeration e = priceCertElements.elements(); e.hasMoreElements(); )
		{
			oneCertHash = (String) e.nextElement();
			//cut off trailing newline if necessary
			oneCertHash = XMLUtil.stripNewlineFromHash(oneCertHash);
			completeCert = m_Database.getPriceCertForHash(oneCertHash);
			ccPriceCerts.add(completeCert);
		}

		//check no of price certs (=number of mixes in cascade) < max cascade length
		//there might be more (non-pay) Mixes in the Cascade, but the PI only cares about pay mixes, since those are the ones it has to pay
		if (ccPriceCerts.size() > Integer.parseInt(Configuration.getMaxCascadeLength()) )
		{
			LogHolder.log(LogLevel.DEBUG, LogType.PAY, "payment instance refuses to pay for cost confirmation, cascade is too long");
			return new XMLErrorMessage(XMLErrorMessage.ERR_CASCADE_LENGTH,"No of price certs exceeds maximum cascade length of the pi");
		}

		//check whether cascade is all free or all pay mixes
		Vector freeMixes = new Vector();
		boolean isAllFree = true;
		String curId;
		Object curCert;
		XMLPriceCertificate fullCert = null;
		for (Enumeration e = priceCertElements.keys(); e.hasMoreElements(); )
		{
			curId = (String) e.nextElement();
			curCert = priceCertElements.get(curId);
			if (curCert instanceof String) //just to be sure, should always be a String, either "none" or a pc hash
			{
				curCert = XMLUtil.stripNewlineFromHash((String) curCert);
				if ( curCert.equals("none") )
				{
					//we found a non-pay mix
					freeMixes.add(curId);
				} else
				{
					//only the hash, need to get full XMLPriceCertificate object from db
					fullCert = m_Database.getPriceCertForHash((String)curCert);
					if (fullCert != null)
					{
						isAllFree = false; //we found a pay Mix
					}
				}
			}
		}
		if (isAllFree == false && freeMixes.size() > 0)
		{
			//for mixes in mixed pay/non-pay cascade, see if it's already been notified, and if not email its operator
			Enumeration enumer = freeMixes.elements();
			String freeMixId;
			while (enumer.hasMoreElements())
			{
				freeMixId = (String) enumer.nextElement();
				if (!notifiedMixes.contains(freeMixId))
				{
					String operatorEmailAddress = "foo@op.com";
					JAPCertificate opCert = m_Database.getOperatorOfMix(freeMixId);
					//get operator email from operator Cert
					operatorEmailAddress = opCert.getAnyEmailAddress();
					if (operatorEmailAddress != null) //will be null if no email address found in cert
					{
						EmailHandler.sendEmail(operatorEmailAddress, EmailHandler.JPI_ADDRESS,
											   EmailHandler.SUBJECT_MIXEDCASCADE,
											   EmailHandler.BODY_MIXEDCASCADE);
						notifiedMixes.add(freeMixId);
					}
				}
			}
		}
	//check if price certs are current (no newer price cert for same mix in database)
	//if price certs don't need to be signed, skip this check
	    if (!isAllFree && Configuration.isSignatureOnPriceRequired().booleanValue())
		{
			String curMixId;
			XMLPriceCertificate aCert;
			Vector priceCertsOfMix;
			for (Enumeration en = ccPriceCerts.elements(); en.hasMoreElements(); )
			{
				aCert = (XMLPriceCertificate) en.nextElement();
				curMixId = aCert.getSubjectKeyIdentifier();
				//try to find a newer price cert for same mix in db
				priceCertsOfMix = m_Database.getPriceCertsForMix(curMixId);
				if (priceCertsOfMix == null)
				{
					//no price certs matching mix found in db
					//should never happen, since we got the price cert whose subjectkeyidentifier is used in the query just a few lines ago
					LogHolder.log(LogLevel.DEBUG, LogType.PAY, "price cert vanished from database?");
					return new XMLErrorMessage(XMLErrorMessage.ERR_DATABASE_ERROR,"Price cert used in the query vanished from database");
				}
				if (priceCertsOfMix.size() ==1)
				{
					//the price cert we got from the cc is the only one in the database for the mix
					//for good measure, check for matching dates
					java.sql.Timestamp ccTime = aCert.getSignatureTime();
					java.sql.Timestamp dbTime = ( (XMLPriceCertificate) priceCertsOfMix.elementAt(0)).getSignatureTime();
					if (!ccTime.equals(dbTime) ){
						LogHolder.log(LogLevel.DEBUG, LogType.PAY, "mismatch between price certificates in db and cc");
						return new XMLErrorMessage(XMLErrorMessage.ERR_DATABASE_ERROR,"mismatch between price certificates in db and cc");
					}
				}
				if (priceCertsOfMix.size() > 1)
				{
					//several price certs for the same mix in database, check if the one in the CC is the newest
					java.sql.Timestamp ccTime = aCert.getSignatureTime();
					java.sql.Timestamp latestPriceCertTime = null;
					//loop through CCs from db, and find the latest date
					XMLPriceCertificate oneCert;
					java.sql.Timestamp oneTime;
					for (Enumeration e = priceCertsOfMix.elements(); e.hasMoreElements(); )
					{
						oneCert = (XMLPriceCertificate) e.nextElement();
   						oneTime = oneCert.getSignatureTime();
							if (oneTime != null) //cert is signed (assumption: no signatureTime set if not signed)
							{
								if (latestPriceCertTime == null || oneTime.after(latestPriceCertTime))
								{
									latestPriceCertTime = oneTime;
								}
							}

					}
					//refuse to settle CC if the price certs contained in it are not the most current ones
					//(which means that whenever prices are changed, the CCs that are signed but yet unsettled become worthless
					//but should only be the CCs of a minute or so, not more than a few cents)
					if (latestPriceCertTime.after(ccTime) )
					{
						LogHolder.log(LogLevel.DEBUG, LogType.PAY,"price certs of the CC are outdated, PI refuses to settle");
						return new XMLErrorMessage(XMLErrorMessage.ERR_DATABASE_ERROR,"price certs of the CC are outdated, PI refuses to settle");
					}
				}
			}
		}
		//everything OK, PI will settle the CC
		LogHolder.log(LogLevel.ERR, LogType.PAY, "PICommandAI:381:starting to settle CC");
		try
		{
			//check if a CC for this account at this cascade already is in database
			XMLEasyCC oldCC = m_Database.getCC(cc.getAccountNumber(), cc.getCascadeID() );
			LogHolder.log(LogLevel.DEBUG, LogType.MISC, "settle request: Now storing CC in database");
			//need two seperate Enumerations, since we need to pass it to two different methods
			//(same Enumeration object will be empty after transversing it once)
			Enumeration mixesToUpdate = cc.getMixIds();
			Enumeration mixesWithStats = cc.getMixIds();
			long newTraffic = 0;
			if (oldCC == null)
			{
				m_Database.insertCC(cc);
				newTraffic = cc.getTransferredBytes();
			}
			else if (oldCC.getTransferredBytes() < cc.getTransferredBytes())
			{
				m_Database.updateCC(cc);
				newTraffic = cc.getTransferredBytes() - oldCC.getTransferredBytes();
			}
			else
			{
				return new XMLErrorMessage(XMLErrorMessage.ERR_INVALID_CC,"outdated cost confirmation, possible doublespending");
			}
		    //charge user account
			if (! m_Database.debitAccount(cc.getAccountNumber(),cc.getPriceCertElements(), newTraffic ) )
			{
				return new XMLErrorMessage(XMLErrorMessage.ERR_ACCOUNT_EMPTY);
			}

			//do not pay out for PriceCerts that are not signed (if configured)
			Hashtable acceptedPriceCerts = new Hashtable(); //key: ski of mix, value: hash of mixcert
			if (Configuration.isSignatureOnPriceRequired().booleanValue() )
			{
				XMLPriceCertificate certToCheck;
				//iterate over certs, and filter out the unsigned ones
				for (Enumeration e = ccPriceCerts.elements(); e.hasMoreElements(); )
				{
					certToCheck = (XMLPriceCertificate) e.nextElement();
					if (isValid(certToCheck))
					{
						String mixId = certToCheck.getSubjectKeyIdentifier();
						String certHash = (String) priceCertElements.get(mixId);
						acceptedPriceCerts.put(mixId,certHash);
					} else
					{
						continue; //skip this cert
					}
				}
			}
			else
			{
				acceptedPriceCerts = cc.getPriceCertElements();
			}

			//pay Mixes
			if (! m_Database.creditMixes(acceptedPriceCerts,newTraffic) )
			{
				return new XMLErrorMessage(XMLErrorMessage.ERR_DATABASE_ERROR);
			}

			//log stats, but only if set in Configuration
			Boolean debug = Configuration.isLogPaymentStatsEnabled();
			if (Configuration.isLogPaymentStatsEnabled().booleanValue() )
			{
				m_Database.writeMixStats(mixesWithStats, newTraffic);
				m_Database.writeMixTraffic(mixesToUpdate, newTraffic);
				String cascadeId = cc.getCascadeID();
				m_Database.writeJapTraffic(newTraffic,cascadeId,cc.getAccountNumber());
			}
			//logically, writeMixStats() and writeMixTraffic() could be combined
			//but we can only iterate over an Enumeration once, and updating two tables in the same iteration would be messy
			//(Enumeration doesnt support clone, and Enumeration is what we get from XMLEasyCC)

			return new XMLErrorMessage(XMLErrorMessage.ERR_OK);

		}
		catch (Exception ex3)
		{
			LogHolder.log(LogLevel.EXCEPTION, LogType.PAY, ex3);
			return new XMLErrorMessage(XMLErrorMessage.ERR_INTERNAL_SERVER_ERROR);
		}
	}

	/*	private byte[] getAccountSnapshots(byte[] data) throws RequestException
	 { //TODO make it work again...
	  int begin;
	  int end;
	  int firstIndex;
	  int lastIndex;
	  int tmpIndex;
	  StringBuffer snapshots = new StringBuffer();

	  String balances = new String(data);
	  System.out.println("::" + balances);

	  if ( ( (begin = balances.indexOf("<Balances>")) == -1) ||
	   ( (end = balances.indexOf("</Balances>")) == -1) ||
	   (begin >= end))
	  {
	   throw new RequestException(409, XML_WRONG_CONFIRMATIONS);
	  }

	  firstIndex = begin + 10;
	  while (true)
	  {
	   if ( ( (tmpIndex = balances.indexOf(XMLBalance.docStartTag, firstIndex)) != -1) &&
	 ( (lastIndex = balances.indexOf(XMLBalance.docEndTag, tmpIndex)) != -1))
	   {
	 lastIndex = lastIndex + XMLBalance.docEndTag.length();
	 try
	 {
	  XMLBalance xmlbal = new XMLBalance(balances.substring(tmpIndex, lastIndex));
	  long accountNumber = xmlbal.getAccountNumber();

	  AccountSnapshot ksa = m_Database.getAccountSnapshot(accountNumber, m_aiName);

	  // create new XML account snapshot structure
	  XMLAccountSnapshot xmlksa =
	   new XMLAccountSnapshot(m_aiName,
	  accountNumber, ksa.creditMax,
	  ksa.credit, ksa.costs,
	  new java.sql.Timestamp(System.currentTimeMillis())
	  );

	  // sign it
	  Document dom = xmlksa.getDomDocument();
	  JAPSignature japSig = Configuration.getSigningInstance();
	  japSig.signXmlDoc(dom);
	  String signed = XMLUtil.XMLDocumentToString(dom);

	  // and add it to the answer
	  snapshots.append(signed);

	 }
	 catch (Exception e)
	 {

	 }
	 firstIndex = lastIndex;
	   }
	   else
	   {
	 break;
	   }
	  }

	  return (XML_HEAD + "<AccountSnapshots>" + snapshots.toString() +
	 "</AccountSnapshots>").getBytes();

	 return null;
	 }
	 */
	/*	private byte[] payoff(byte[] data) throws RequestException
	 {
	  int begin;
	  int end;
	  int firstIndex;
	  int lastIndex;
	  int tmpIndex;
	  long sum = 0;

	  StringBuffer answer = new StringBuffer();
	  String accounts = new String(data);
	  System.out.println("::" + accounts);

	  if ( ( (begin = accounts.indexOf("<AccountNumbers>")) == -1) ||
	   ( (end = accounts.indexOf("</AccountNumbers>")) == -1) ||
	   (begin >= end))
	  {
	   throw new RequestException(409, XML_WRONG_CONFIRMATIONS);
	  }

	  firstIndex = begin + 16;
	  while (true)
	  {
	   if ( ( (tmpIndex = accounts.indexOf("<AccountNumber>", firstIndex)) != -1) &&
	 ( (lastIndex = accounts.indexOf("</AccountNumber>", tmpIndex)) != -1))
	   {
	 firstIndex = tmpIndex + 15;
	 try
	 {
	  long accountNumber = Long.parseLong(accounts.substring(firstIndex, lastIndex));
	  long costs = m_Database.getCosts(accountNumber, m_aiName);
	  if (costs > 0)
	  {
	   long payCosts = m_Database.getPayCosts(accountNumber, m_aiName);
	   if (costs <= payCosts)
	   {
	 continue;
	   }
	   sum = sum + costs - payCosts;
	  }
	 }
	 catch (Exception e)
	 {

	 }
	 firstIndex = lastIndex;
	   }
	   else
	   {
	 break;
	   }
	  }

	  return (XML_HEAD + "<Amount>" + sum + "</Amount>").getBytes();
	 }
	 */
	/*	private byte[] confirm(byte[] data)
	 {
	  return null;
	 }

	 private int countTicks(byte[] begin, byte[] last) throws RequestException
	 {
	  int maxTickCounter = 1000;
	  MessageDigest md;
	  try
	  {
	   md = MessageDigest.getInstance("SHA");
	  }
	  catch (Exception e)
	  {
	   throw new RequestException(500);
	  }
	  int tickCounter;
	  byte[] tmp;
	  for (tickCounter = 0; tickCounter < maxTickCounter + 1; tickCounter++)
	  {
	   tmp = md.digest(begin);
	   if (Arrays.equals(tmp, last))
	   {
	 break;
	   }
	   begin = tmp;
	  }
	  if (tickCounter == maxTickCounter)
	  {
	   return 0;
	  }
	  else
	  {
	   return tickCounter + 1;
	  }
	 }*/

	 //takes the hash value of a price certificate (e.g. from a cost confirmation)
	 //and returns the corresponding complete cc from the database
	 private XMLPriceCertificate getPriceCertForHash(String hashValue)
	 {
		 //it is probably more efficient to let the database handle the comparions of all price certs,so we just pass the hashvalue through to it
		 XMLPriceCertificate thePriceCert= m_Database.getPriceCertForHash(hashValue);
		 if (thePriceCert != null)
		 {
			 return thePriceCert;
		 }
		 else
		 {
			 LogHolder.log(LogLevel.DEBUG, LogType.PAY, "BI knows no price certificate corresponding to the hash value "+hashValue);
			 return null;		 }
	 }

	private boolean isValid(XMLPriceCertificate aPriceCert)
	{
		Timestamp sigTime = aPriceCert.getSignatureTime();

		//signed at all?
		if (sigTime == null )
		{
			return false;
		}
		//valid signature time?
		Timestamp now = new Timestamp( new Date().getTime());
		if (sigTime.after(now) )
		{
			return false;
		}

		//valid signature?
		try
		{
			JAPCertificate biCert = Configuration.getOwnCertificate();
			XMLSignature.verify(aPriceCert.getDocument(), biCert);
		}
		catch (XMLParseException ex)
		{
			//if we cant even parse the signature xml, the sig is certainly not valid
			return false;
		}
		return true;
	}
}
