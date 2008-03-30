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
package anon.pay;

import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import anon.crypto.ByteSignature;

import anon.client.ChannelTable;
import anon.client.Multiplexer;
import anon.client.PacketCounter;
import anon.client.XmlControlChannel;
import anon.pay.xml.XMLAiLoginConfirmation;
import anon.pay.xml.XMLChallenge;
import anon.pay.xml.XMLEasyCC;
import anon.pay.xml.XMLErrorMessage;
import anon.pay.xml.XMLPayRequest;
import anon.pay.xml.XMLResponse;
import anon.pay.xml.XMLPriceCertificate;
import anon.util.XMLUtil;
import java.util.Vector;
import java.util.Enumeration;
import anon.infoservice.IMutableProxyInterface;
import anon.IServiceContainer;
import anon.infoservice.MixCascade;
import java.util.Hashtable;
import java.sql.Timestamp;

/**
 * This control channel is used for communication with the AI
 * (AccountingInstance or Abrechnungsinstanz in German) which lives in the first
 * mix. The AI sends a request when it wants a cost confirmation from us. This
 * thread waits for incoming requests and sends the requested confirmations to
 * the AI.
 *
 * @author Bastian Voigt, Tobias Bayer, Rolf Wendolsky
 * @version 1.0
 */
public class AIControlChannel extends XmlControlChannel
{
	public static final long MAX_PREPAID_INTERVAL = 3000000; // 3MB
	public static final long MIN_PREPAID_INTERVAL = 5000; // 500 kb
	public static final long AI_LOGIN_TIMEOUT = 120000; // 2 minutes
	private static final long NO_CHARGED_ACCOUNT_UPDATE = 1000 * 60 * 5; // 5 minutes

  //codes for AI events that can be fired
  private static final int EVENT_UNREAL = 1;



  private static long m_totalBytes = 0;

  private boolean m_bPrepaidReceived = false;
  private long m_prepaidBytes = 0;

  private Vector m_aiListeners = new Vector();

  private IMutableProxyInterface m_proxys;

  private PacketCounter m_packetCounter;

  private MixCascade m_connectedCascade;

  private XMLEasyCC m_initialCC;

  private Vector m_aiLoginSyncObject;

  private volatile boolean m_synchronizedAILogin;

  public AIControlChannel(Multiplexer a_multiplexer, IMutableProxyInterface a_proxy,
						  PacketCounter a_packetCounter, IServiceContainer a_serviceContainer,
						  MixCascade a_connectedCascade) {
    super(ChannelTable.CONTROL_CHANNEL_ID_PAY, a_multiplexer, a_serviceContainer);
    m_proxys = a_proxy;
    m_packetCounter = a_packetCounter;
	m_connectedCascade = a_connectedCascade;
	m_aiLoginSyncObject = new Vector(1);
	m_synchronizedAILogin =  true;
  }

  public void addAIListener(IAIEventListener a_aiListener) {
    if (!m_aiListeners.contains(a_aiListener)) {
      m_aiListeners.addElement(a_aiListener);
    }
  }

  /**
   * proccessXMLMessage - this is called when a new request is coming in.
   *
   * @param docMsg
   *          Document
   */
  public void processXmlMessage(Document docMsg)
  {
	  Element elemRoot = docMsg.getDocumentElement();
	  String tagName = elemRoot.getTagName();
	  try
	  {
		  if (tagName.equals(XMLPayRequest.XML_ELEMENT_NAME))
		  {
			  XMLPayRequest theRequest = new XMLPayRequest(elemRoot);
			  processPayRequest(theRequest);
		  }
		  else if (tagName.equals(XMLAiLoginConfirmation.XML_ELEMENT_NAME))
		  {
			  XMLAiLoginConfirmation loginConfirm =
				  new XMLAiLoginConfirmation(elemRoot);
			  /*LogHolder.log(LogLevel.EXCEPTION, LogType.PAY,
						"Thread "+ Thread.currentThread().getName()+" getting AI Login Confirmation with code "
						+loginConfirm.getCode()+" and message "+loginConfirm.getMessage());*/
			  int code = loginConfirm.getCode();

			  synchronized(m_aiLoginSyncObject)
			  {
				  if(loginConfirm.getCode()==XMLErrorMessage.ERR_OK)
				  {
					  m_aiLoginSyncObject.addElement(new Object());
				  }
				  m_aiLoginSyncObject.notifyAll();
			  }
		  }
		  else if (tagName.equals(XMLErrorMessage.XML_ELEMENT_NAME))
		  {

			  XMLErrorMessage error = new XMLErrorMessage(elemRoot);
			  LogHolder.log(LogLevel.EXCEPTION, LogType.PAY,
						"processing AI ErrorMessage "+error.getErrorCode()+": "+error.getMessage());
			  if (error.getErrorCode() ==  XMLErrorMessage.ERR_ACCOUNT_EMPTY)
			  {
				  // find an account that is not empty - if possible...

				  /*getServiceContainer().keepCurrentService(false); // reconnect to another cascade if possible
				  processErrorMessage(new XMLErrorMessage(elemRoot));*/


				  updateBalance(PayAccountsFile.getInstance().getActiveAccount(), false); // show that account is empty
				  PayAccount currentAccount = PayAccountsFile.getInstance().getAlternativeNonEmptyAccount(
								  m_connectedCascade.getPIID());

				  if (currentAccount != null)
				  {
					  PayAccountsFile.getInstance().setActiveAccount(currentAccount);
				  }
				  else
				  {
					  getServiceContainer().keepCurrentService(false); // reconnect to another cascade if possible
					  processErrorMessage(new XMLErrorMessage(elemRoot));
				  }

			  }
			  else
			  {
				  getServiceContainer().keepCurrentService(false); // reconnect to another cascade if possible
				  processErrorMessage(new XMLErrorMessage(elemRoot));
			  }

		  }
		  else if (tagName.equals(XMLChallenge.XML_ELEMENT_NAME))
		  {
			  processChallenge(new XMLChallenge(elemRoot));
		  }
		  else if (tagName.equals(XMLEasyCC.getXMLElementName()))
		  {
			  processInitialCC(new XMLEasyCC(elemRoot));
		  }
		  else
		  {
			  LogHolder.log(LogLevel.WARNING, LogType.PAY,
							"Received unknown payment control channel message '" + tagName + "'");
		  }
	  }
	  catch (Exception ex)
	  {
		  LogHolder.log(LogLevel.EXCEPTION, LogType.PAY, ex);
		  getServiceContainer().keepCurrentService(false); // reconnect to another cascade if possible
		  PayAccountsFile.getInstance().signalAccountError(
			  new XMLErrorMessage(XMLErrorMessage.ERR_BAD_REQUEST,
								  ex.getClass().getName() + ": " + ex.getMessage()));
	  }
  }

  /**
   * processChallenge
   *
   * @param xMLChallenge
   *          XMLChallenge
   */
  private synchronized void processChallenge(XMLChallenge chal) throws Exception
  {
	  byte[] arbChal = chal.getChallengeForSigning();

	  LogHolder.log(LogLevel.NOTICE, LogType.PAY, "Received " + chal.getPrepaidBytes() + " prepaid bytes.");

	  PayAccount acc = PayAccountsFile.getInstance().getActiveAccount();
	  if (acc == null)
	  {
		  throw new Exception("Received Challenge from AI but ActiveAccount not set!");
	  }
	  if (!m_bPrepaidReceived && chal.getPrepaidBytes() > 0)
	  {
		  m_prepaidBytes = chal.getPrepaidBytes();
		   m_bPrepaidReceived = true;
		  // ignore prepaid bytes smaller than zero
		  acc.updateCurrentBytes(chal.getPrepaidBytes() * ( -1)); // remove transferred bytes
	  }

	  byte[] arbSig = ByteSignature.sign(arbChal, acc.getPrivateKey());
	  XMLResponse response = new XMLResponse(arbSig);
	  this.sendXmlMessage(XMLUtil.toXMLDocument(response));
  }

  /**
   * processErrorMessage
   *
   * @param msg
   *          XMLErrorMessage
   */
  private void processErrorMessage(XMLErrorMessage msg) {
	  PayAccountsFile.getInstance().signalAccountError(msg);
  }

  /**
   * process a XMLPayRequest message, which might request a XMLAccountCertificate,
   * request a XMLBalance, or contain a XMLEasyCC which the AI asks the JAP to sign
   *
   * @param request
   *          XMLPayRequest
   */
  private synchronized void processPayRequest(XMLPayRequest request) {


	//if requested, send account certificate
	if (request.isAccountRequest())
	{
		if (!sendAccountCert())
		{
			LogHolder.log(LogLevel.ALERT, LogType.PAY, "Could not send account certificate!");
		}
	}

	//if sent, process cost confirmation
    XMLEasyCC cc = request.getCC();
    if (cc != null) {
      try {
		  processCcToSign(cc);
      }
      catch (Exception ex1) {
        // the account stated by the AI does not exist or is not currently
        // active
        // @todo handle this exception
        LogHolder.log(LogLevel.ERR, LogType.PAY, ex1);
      }
    }
  }

  private void updateBalance(final PayAccount currentAccount, boolean a_bSynchronous)
  {
	  Runnable runUpdate;
	  if (currentAccount == null)
	  {
		  return;
	  }

	  runUpdate = new Runnable()
	  {
		  public void run()
		  {
			  try
			  {
				  currentAccount.fetchAccountInfo(m_proxys, false);
			  }
			  catch (Exception ex)
			  {
				  LogHolder.log(LogLevel.DEBUG, LogType.PAY, ex);
			  }
		  }
	  };

	  if (a_bSynchronous)
	  {
		  LogHolder.log(LogLevel.DEBUG, LogType.PAY, "Fetching new Balance from BI.");
		  runUpdate.run();
	  }
	  else
	  {
		  LogHolder.log(LogLevel.DEBUG, LogType.PAY, "Fetching new Balance from BI asynchronously.");
		  Thread t = new Thread(runUpdate);
		  t.setDaemon(true);
		  t.start();

	  }
  }

	/**
	 * processCcToSign: to be called by processPayRequest
	 * (only the initial, old CC is sent as a naked XMLEasyCC,
	 * new CCs to sign are sent inside a XMLPayRequest)
	 *
	 */
	private synchronized void processCcToSign(XMLEasyCC cc) throws Exception
	{
		PayAccount currentAccount = PayAccountsFile.getInstance().getActiveAccount();
		long transferedBytes;
		XMLEasyCC myLastCC;

		//check CC for proper account number
		if ( (currentAccount == null) || (currentAccount.getAccountNumber() != cc.getAccountNumber()))
		{
			throw new Exception("Received CC with wrong accountnumber");
		}

		/*
		if (!m_bInitialCCSent)
		{
			LogHolder.log(LogLevel.WARNING, LogType.PAY, "CC requested before inital CC was sent!");
		}*/


	  // long transferedBytes = currentAccount.updateCurrentBytes(m_packetCounter);
	  currentAccount.updateCurrentBytes(m_packetCounter);
	  //LogHolder.log(LogLevel.DEBUG, LogType.PAY, "AI requests to sign " + transferedBytes + " transferred bytes");

	//System.out.println("PC Hash looked: " + cc.getConcatenatedPriceCertHashes());
	  myLastCC = currentAccount.getAccountInfo().getCC(cc.getConcatenatedPriceCertHashes());
	  //System.out.println("toSign: " + cc.getConcatenatedPriceCertHashes());
	  long confirmedBytes = 0;
	  if (myLastCC != null)
	  {
		  if (m_initialCC == null)
		  {
			  // this seems to be the first connection to the cascade
			  LogHolder.log(LogLevel.WARNING, LogType.PAY,
							"No initial CC available! The Mix might have lost its CC.");
			  if (m_prepaidBytes > 0)
			  {
				  // remove any prepaid bytes received before
				  currentAccount.updateCurrentBytes(m_prepaidBytes);
			  }
		  }
		  else
		  {
			  confirmedBytes = myLastCC.getTransferredBytes();
			  LogHolder.log(LogLevel.DEBUG, LogType.PAY, "Transferred bytes of last CC: " + confirmedBytes);
		  }
	  }

	  //System.out.println("To confirm1: " + cc.getTransferredBytes() + " Transferred1: " + transferedBytes);


	  transferedBytes = currentAccount.getCurrentBytes();  // maybe transferred bytes have changed above...
	  m_totalBytes = transferedBytes;

	  // check if bytes asked for in CC match bytes transferred
	  long newPrepaidBytes = //(cc.getTransferredBytes() - confirmedBytes) + m_prepaidBytes - transferedBytes;
		  cc.getTransferredBytes() - transferedBytes;

	  long diff = newPrepaidBytes - m_connectedCascade.getPrepaidInterval();

	  /*
	  if (transferedBytes < confirmedBytes)
	  {
		  LogHolder.log(LogLevel.ALERT, LogType.PAY, "Less transferred than confirmed bytes! "+
			  "Transferred: " + transferedBytes + " Confirmed: " + confirmedBytes);
	  }*/

	  if (diff > 0)
	  {
		  // this may happen with fast downloads; the bytes are counted in the Mix before JAP receives them
		  LogHolder.log(LogLevel.WARNING, LogType.PAY,
						"Illegal number of prepaid bytes for signing. Difference/Spent/CC/PrevCC: " +
						diff + "/" + transferedBytes + "/" + cc.getTransferredBytes() + "/" + confirmedBytes);

		  if (transferedBytes < 0)
		  {
			  LogHolder.log(LogLevel.WARNING, LogType.PAY, "The mix might have lost a CC. " +
							"Resetting transferred bytes to zero for now...");
			  currentAccount.updateCurrentBytes(transferedBytes * (-1));
			  transferedBytes = currentAccount.getCurrentBytes();
		  }
		  else if (cc.getTransferredBytes() < confirmedBytes)
		  {
			  LogHolder.log(LogLevel.WARNING, LogType.PAY,
							"Requested less than confirmed before! Maybe a CC did get lost!");
			  //this.fireAIEvent(EVENT_UNREAL, diff); // do not show this problem to the user...
		  }
	  }

	  cc.setTransferredBytes(transferedBytes + m_connectedCascade.getPrepaidInterval());


	  //get pricecerts and check against hashes in CC
	  //get price certs from connected cascade
	  /**
	   * We don't even bother to check the hashes in the CC,
	   * the JAP just fills in the ones he knows from the Cascade
	   */
	  if (cc.getTransferredBytes() > confirmedBytes)
	  {
		 // cc.setTransferredBytes(-1l);

		  cc.setPriceCerts(m_connectedCascade.getPriceCertificateHashes());
		  cc.setPIID(currentAccount.getAccountCertificate().getPIID());
		  cc.setCascadeID( m_connectedCascade.getId());
		  cc.sign(currentAccount.getPrivateKey());
		  if (currentAccount.addCostConfirmation(cc) <= 0)
		  {
			  LogHolder.log(LogLevel.WARNING, LogType.PAY, "Added old cost confirmation!");
		  }
	  }
	  else if (myLastCC != null && m_initialCC != null)
	  {
		  // resend the last valid CC
		 // System.out.println("old CC: " + cc.getConcatenatedPriceCertHashes());
		  cc = myLastCC;
	  }
	  else
	  {
		   LogHolder.log(LogLevel.EMERG, LogType.PAY, "Creating zero CC!!");
		  // send a zero CC
		  cc.setTransferredBytes(0);
		  cc.setPriceCerts(m_connectedCascade.getPriceCertificateHashes());
		  cc.setPIID(currentAccount.getAccountCertificate().getPIID());
		  cc.setCascadeID( m_connectedCascade.getId());
		  cc.sign(currentAccount.getPrivateKey());
		  currentAccount.addCostConfirmation(cc);
	  }
	  //System.out.println("Processed: " + cc.getTransferredBytes());

	  //System.out.println("To confirm2: " + cc.getTransferredBytes() + " Transferred2: " + transferedBytes);

	  if (m_initialCC == null)
	  {
		  // this seems to be the first connection to the cascade
		  LogHolder.log(LogLevel.WARNING, LogType.PAY, "Setting initial CC to current CC...");
		  m_initialCC = cc;
	  }

	  //System.out.println(cc.getTransferredBytes());

	  sendXmlMessage(XMLUtil.toXMLDocument(cc));
  }


  public boolean sendAccountCert()
  {

	  String message = null;
	  Vector priceCerts = m_connectedCascade.getPriceCertificates();
	  Vector mixIDs = m_connectedCascade.getMixIds();
	  String mixID;
	  XMLPriceCertificate priceCert;
	  Timestamp now = new Timestamp(System.currentTimeMillis());
	  PayAccount activeAccount = PayAccountsFile.getInstance().getActiveAccount();

	  if (activeAccount == null || !activeAccount.isCharged(now) || (activeAccount.getBI() == null) ||
		  !activeAccount.getBI().getId().equals(m_connectedCascade.getPIID()))
	  {
		  PayAccount currentAccount = null;
		  PayAccount openTransactionAccount = null;
		  if (activeAccount != null && activeAccount.getSpent() == 0) // spent means account has been used
		  {
			  openTransactionAccount = PayAccountsFile.getInstance().getActiveAccount();
		  }

		  Vector accounts = PayAccountsFile.getInstance().getAccounts(m_connectedCascade.getPIID());
		  if (accounts.size() > 0)
		  {
			  for (int i = 0; i < accounts.size(); i++)
			  {
				  currentAccount = (PayAccount) accounts.elementAt(i);
				  if (currentAccount.isCharged(now))
				  {
					  break;
				  }
				  else if (openTransactionAccount == null && currentAccount.getSpent() == 0)
				  {
					  openTransactionAccount = currentAccount;
				  }
				  currentAccount = null;
			  }
			  if (currentAccount != null)
			  {
				  PayAccountsFile.getInstance().setActiveAccount(currentAccount);
			  }
			  else if (openTransactionAccount != null)
			  {
				  PayAccountsFile.getInstance().setActiveAccount(openTransactionAccount);
			  }
			  // check if the account is charged; if not, try to get the latest balance from each account
			  if (PayAccountsFile.getInstance().getActiveAccount() == null ||
				  !PayAccountsFile.getInstance().getActiveAccount().isCharged(now) &&
				  accounts.size() > 0)
			  {
				  LogHolder.log(LogLevel.WARNING, LogType.PAY,
								"No charged account is available for connecting. Trying to update balances...");
				  for (int i = 0; i < accounts.size(); i++)
				  {
					  currentAccount = (PayAccount) accounts.elementAt(i);
					  if (currentAccount.getBalance() == null ||
						  currentAccount.getBalance().getTimestamp().getTime() <
						  (now.getTime() - NO_CHARGED_ACCOUNT_UPDATE))
					  {
						  // update the account if the timestamp is quite old
						  updateBalance(currentAccount, true);
						  if (currentAccount.isCharged(now))
						  {
							  PayAccountsFile.getInstance().setActiveAccount(currentAccount);
							  break;
						  }
					  }
				  }
			  }
		  }
	  }

	  if (!PayAccountsFile.getInstance().signalAccountRequest(m_connectedCascade) ||
		  PayAccountsFile.getInstance().getActiveAccount() == null)
	  {
		  return false;
	  }


	  if (priceCerts.size() != mixIDs.size())
	  {
		  message = "Not all Mixes in cascade " + m_connectedCascade.getId() + " have price certs! " +
		  "PriceCerts/MixIDs:" + priceCerts.size() + "/" + mixIDs.size();
	  }
	  else
	  {
		  for (int i = 0; i < mixIDs.size(); i++)
		  {
			  priceCert = ( (XMLPriceCertificate) priceCerts.elementAt(i));
			  mixID = (String) mixIDs.elementAt(i);
			  if (!priceCert.verify(PayAccountsFile.getInstance().getActiveAccount().getBI()))
			  {
				  message = "Price certificate of cascade " + m_connectedCascade.getId() + " for mix " +
					  mixID + " cannot be verified!";
				  break;
			  }

			  if (!priceCert.getSubjectKeyIdentifier().equals(mixID))
			  {
				  message = "SKI in price certificate of cascade " + m_connectedCascade.getId() +
					  " differs from Mix ID! SKI:" + priceCert.getSubjectKeyIdentifier() + " MixID: " + mixID;
				  break;
			  }
		  }
	  }

	  if (message != null)
	  {
		  LogHolder.log(LogLevel.ERR, LogType.PAY, message);

		  getServiceContainer().keepCurrentService(false); // reconnect to another cascade if possible
		  PayAccountsFile.getInstance().signalAccountError(
				  new XMLErrorMessage(XMLErrorMessage.ERR_INVALID_PRICE_CERTS, message));


		  return false;
	  }

	  PayAccountsFile.getInstance().getActiveAccount().resetCurrentBytes();
	  sendXmlMessage(XMLUtil.toXMLDocument(PayAccountsFile.getInstance().getActiveAccount().getAccountCertificate()));
	  /*
	   * new ai login procedure: wait until all messages are
	   * exchanged or until login is timed out
	   */
	  boolean aiLoginSuccess = true;
	  synchronized(m_aiLoginSyncObject)
	  {
		  /* Only if the new synchronized AI login procedure is suppported by the first mix
		   * (version >= 00.07.20) we wait until the mix confirms a successful/unsuccessful login or
		   * the connection timed out. Otherwise for backward compatibility reasons
		   * we still perform the old asynchronous login procedure.
		   */
		  if(m_synchronizedAILogin)
		  {
			  LogHolder.log(LogLevel.INFO, LogType.PAY, "Performing new synchronous AI login");
			  try {
				m_aiLoginSyncObject.wait(AI_LOGIN_TIMEOUT);
			  }
			  catch (InterruptedException e) {
				/* This happens when a user pushes the anonymity off button before
				 * the synchronous login hasn't finished. Therefore just leave. false will be returned
				 * automatically.
				 */
			  }
			  //LogHolder.log(LogLevel.ALERT, LogType.PAY, m_aiLoginSyncObject);
			  aiLoginSuccess = m_aiLoginSyncObject.size() != 0;
			  m_aiLoginSyncObject.removeAllElements();
		  }
		  else
		  {
			  LogHolder.log(LogLevel.WARNING, LogType.PAY, "Old Mix version does not support synchronous AI login! Performing old asynchronous login procedure");
		  }
	  }
	  return aiLoginSuccess;
  }

  private void fireAIEvent(int a_eventType, long a_additionalInfo) {
    LogHolder.log(LogLevel.DEBUG, LogType.PAY, "Firing AI event");
    Enumeration e = m_aiListeners.elements();
    while (e.hasMoreElements()) {
      if (a_eventType == EVENT_UNREAL) {
        ((IAIEventListener)e.nextElement()).unrealisticBytes(a_additionalInfo);
      }
    }
  }

  public static long getBytes() {
    return m_totalBytes;
  }

	/**
	 * processInitialCC: last step of connecting to a pay cascade:
	 * take last CC as sent by AI as base value for future CCs
	 * Also, send a CC for (bytes in last CC + prepay interval of cascade)
	 * to avoid triggering the cascade's hardlimit by starting to transfer bytes without prepaying
	 *
	 * @param a_cc XMLEasyCC: the last CC that the JAP sent to this Cascade, as returned from the AI
	 */
	private synchronized void processInitialCC(XMLEasyCC a_cc) {
		PayAccount currentAccount = PayAccountsFile.getInstance().getActiveAccount();
		String msg = "AI has sent a INVALID last cost confirmation.";

		if (a_cc.verify(currentAccount.getPublicKey()))
		{
			try
			{
				//compare number
				if (a_cc.getNrOfPriceCerts() != m_connectedCascade.getNrOfPriceCerts())
				{
					LogHolder.log(LogLevel.EXCEPTION, LogType.PAY,
								  "number of price certificates in cost confirmation does not match " +
								  "number of price certs in cascade");
					getServiceContainer().keepCurrentService(false); // reconnect to another cascade if possible
					PayAccountsFile.getInstance().signalAccountError(
						new XMLErrorMessage(XMLErrorMessage.ERR_INVALID_PRICE_CERTS,
											"AI sent CC will illegal number of price certs" +
											a_cc.getNrOfPriceCerts()));
					return;
				}

				/**
				 * Check if the CC contains the correct price certs
				 */
				//get hashes from CC
				Hashtable hashPriceCertHashesInCC = a_cc.getPriceCertHashes();
				Enumeration priceCertHashesInCascade = m_connectedCascade.getPriceCertificateHashes().keys();
				Hashtable inCascade = m_connectedCascade.getPriceCertificateHashes();
				String curCascadeHash;
				String curCcHash;
				MixCascade.MixPosition ski;
				int i = 0;
				while (priceCertHashesInCascade.hasMoreElements()) //enough to use one enum in condition, since we already checked for equal size
				{
					ski = ((MixCascade.MixPosition)priceCertHashesInCascade.nextElement());
					curCascadeHash = (String)inCascade.get(ski);
					curCcHash = (String)hashPriceCertHashesInCC.get(ski);

					if (curCcHash == null || !curCascadeHash.equals(curCcHash))
					{
						String message = "AI sent CC with illegal price cert hash for mix " +
							(ski.getPosition() + 1) + " (" + (i + 1) + ")" +  "!";
						if (curCcHash == null)
						{
							message += " Price certificate for this Mix was not found in CC!";
						}
						LogHolder.log(LogLevel.WARNING, LogType.PAY, message);
						getServiceContainer().keepCurrentService(false); // reconnect to another cascade if possible
						PayAccountsFile.getInstance().signalAccountError(
							new XMLErrorMessage(XMLErrorMessage.ERR_INVALID_PRICE_CERTS, message));
						return;
					}
					hashPriceCertHashesInCC.remove(ski); // do not count duplicate price certs
					i++;
				}
				//System.out.println("Inital1 CC: " + a_cc.getConcatenatedPriceCertHashes());

				LogHolder.log(LogLevel.DEBUG, LogType.PAY,
							  "AI has sent a valid last cost confirmation. Adding it to account.");
				//no need to verify the price certificates of the last CC, since they might have changed since then
				//System.out.println("PC Hash stored: " + a_cc.getConcatenatedPriceCertHashes());

				if (m_initialCC == null)
				{
					currentAccount.updateCurrentBytes(a_cc.getTransferredBytes());
					m_initialCC = a_cc;
				}
				else
				{
					long diff = a_cc.getTransferredBytes() - m_initialCC.getTransferredBytes();
					LogHolder.log(LogLevel.WARNING, LogType.PAY,
								  "Updated initial CostConfirmation! Difference: " + diff);
					currentAccount.updateCurrentBytes(diff);
				}
				long confirmedbytes = a_cc.getTransferredBytes();

				if (currentAccount.addCostConfirmation(a_cc) < 0)
				{
					/*
					a_cc.setTransferredBytes(currentAccount.getAccountInfo().getCC(
									   a_cc.getConcatenatedPriceCertHashes()).getTransferredBytes());*/
					LogHolder.log(LogLevel.WARNING, LogType.PAY, "Received old cost confirmation!");
				}

				//System.out.println("Initial: " + a_cc.getTransferredBytes() + ":" + a_cc.getConcatenatedPriceCertHashes());
				//get Cascade's prepay interval

				long currentlyTransferedBytes = currentAccount.getCurrentBytes(); //currentAccount.updateCurrentBytes(m_packetCounter);
				long bytesToPay =
					m_connectedCascade.getPrepaidInterval() - (confirmedbytes - currentlyTransferedBytes);

				//System.out.println("Initial CC transfered bytes: " + bytesToPay + " Old transfered: " + currentlyTransferedBytes + " Prepaid: " + m_prepaidBytes);


				long oldBytes = a_cc.getTransferredBytes();
				XMLEasyCC newCC = new XMLEasyCC(a_cc);
				//send CC for up to <last CC + prepay interval> bytes
				if (bytesToPay > 0)
				{

					newCC.setTransferredBytes(confirmedbytes + bytesToPay);
				}
				else
				{
					newCC.setTransferredBytes(confirmedbytes);
				}
				//newCC.setTransferredBytes(1000000000l);
				newCC.sign(currentAccount.getPrivateKey());
				if (bytesToPay > 0 && currentAccount.addCostConfirmation(newCC) <= 0)
				{
					LogHolder.log(LogLevel.WARNING, LogType.PAY, "Sending old cost confirmation! " +
								  "Diff (ShoulBe)/Old/New:" + bytesToPay + "/" + oldBytes + "/" +
								  newCC.getTransferredBytes());
				}

				//System.out.println("Initial: " + newCC.getTransferredBytes());

				// always send the message to tell the Mix the CC was received
				sendXmlMessage(XMLUtil.toXMLDocument(newCC));

				return;
			}
			catch (Exception e)
			{
				LogHolder.log(LogLevel.EXCEPTION, LogType.PAY, msg, e);
			}
		}
		else
		{
			LogHolder.log(LogLevel.ERR, LogType.PAY, msg);
		}

		// an error occured; reconnect to another cascade if possible

		getServiceContainer().keepCurrentService(false);
		PayAccountsFile.getInstance().signalAccountError(
			new XMLErrorMessage(XMLErrorMessage.ERR_WRONG_DATA, msg));
	}

	public void multiplexerClosed()
	{
		synchronized(m_aiLoginSyncObject)
		{
			m_aiLoginSyncObject.notifyAll();
		}
	}

	public void setSynchronizedAILogin(boolean synchronizedAILogin)
	{
		synchronized(m_aiLoginSyncObject)
		{
			m_synchronizedAILogin = synchronizedAILogin;
		}
	}
}
