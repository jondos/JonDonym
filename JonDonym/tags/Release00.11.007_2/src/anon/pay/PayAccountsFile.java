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
package anon.pay;

import java.io.IOException;
import java.sql.Timestamp;
import java.util.Enumeration;
import java.util.Observable;
import java.util.Vector;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import anon.ErrorCodes;
import anon.crypto.AsymmetricCryptoKeyPair;
import anon.crypto.DSAKeyPool;
import anon.infoservice.Database;
import anon.infoservice.IMutableProxyInterface;
import anon.infoservice.MixCascade;
import anon.pay.xml.XMLAccountCertificate;
import anon.pay.xml.XMLBalance;
import anon.pay.xml.XMLErrorMessage;
import anon.pay.xml.XMLGenericText;
import anon.pay.xml.XMLJapPublicKey;
import anon.util.IMiscPasswordReader;
import anon.util.IXMLEncodable;
import anon.util.XMLParseException;
import anon.util.XMLUtil;
import anon.util.captcha.ICaptchaSender;
import anon.util.captcha.IImageEncodedCaptcha;
import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;


import anon.pay.xml.XMLAccountInfo;


/**
 * This class encapsulates a collection of accounts. One of the accounts in the collection
 * is always active, except when the collection is empty.
 *
 * GUI classes can register a IPaymentListener with this class to be informed about all
 * payment specific events.
 *
 * The class can be initialized from an
 * XML structure and can also save all internal information in an XML structure before
 * shutdown.
 * For saving the accounts information, the following XML structure is used:
 * <pre>
 * &lt;PayAccountsFile version="1.0"&gt;
 *    &lt;MainAccountNumber&gt;123465&lt;/MainAccountNumber&gt;
 *    &lt;Accounts&gt;
 *       &lt;Account version="1.0"&gt;
 * 		    &lt;AccountCertificate&gt;...&lt;/AccountCertificate&gt; // Kontozertiufkat von der BI unterschrieben
 * 		    &lt;RSAPrivateKey&gt;...&lt;/RSAPrivateKey&gt; //der geheime Schl?ssel zum Zugriff auf das Konto
 * 		    &lt;TransferCertificates&gt; //offenen Transaktionsummern
 * 			    ....
 * 		    &lt;/TransferCertifcates&gt;
 * 		    &lt;AccountInfo&gt;...&lt;/AccountInfo&gt; //Kontostand (siehe XMLAccountInfo)
 *       &lt;/Account&gt;
 *        .
 *        .
 *        .
 *    &lt;/Accounts&gt;
 * &lt;/PayAccountsFile&gt;
 * </pre>
 *
 * @author Bastian Voigt, Tobias Bayer
 * @version 1.0
 */
public class PayAccountsFile extends Observable implements IXMLEncodable, IBIConnectionListener, IMessageListener
{
	public static final String XML_ELEMENT_NAME = "PayAccounts";

	public static final Integer CHANGED_AUTO_UPDATE = new Integer(0);

	private static final String XML_ATTR_IGNORE_AI_ERRORS = "ignoreAIErrorMessages";
	private static final String XML_ATTR_ENABLE_BALANCE_AUTO_UPDATE = "autoUpdateBalance";

	private boolean m_bIsInitialized = false;
	private boolean m_bIgnoreAIAccountErrorMessages = false;
	private boolean m_bEnableBalanceAutoUpdate = true;


	/** contains a vector of PayAccount objects, one for each account */
	private Vector m_Accounts = new Vector();

	/** the active account */
	private PayAccount m_ActiveAccount = null;

	/** the one and only accountsfile */
	private static PayAccountsFile ms_AccountsFile = null;

	private Vector m_paymentListeners = new Vector();

	private Vector m_messageListeners = new Vector();

	private MyAccountListener m_MyAccountListener = new MyAccountListener();
	
	private DSAKeyPool m_keyPool;

	/**
	 * At this time, the implementation supports only one single BI. In the future
	 * a feature should be added to have support for multiple BIs, so that the
	 * AccountCertificate also contains a BIName. The infoservice should then
	 * publish information about the known BIs and also which MixCascade works
	 * with which BI.
	 *
	 * However, at the moment there is only one static BI which is used for all
	 * cascades and all accounts. This is the reason why we have this field in
	 * the singleton class.
	 */
	//private BI m_theBI;

	// singleton!
	private PayAccountsFile()
	{
		m_keyPool = new DSAKeyPool();
		m_keyPool.start();
	}

	/**
	 * returns the one and only accountsfile.
	 * Note: If {@link init(BI, Element)} was not yet called,
	 * you get an empty instance which is not really useful.
	 */
	public static PayAccountsFile getInstance()
	{
		if (ms_AccountsFile == null)
		{
			ms_AccountsFile = new PayAccountsFile();
		}
		return ms_AccountsFile;
	}
	
	public AsymmetricCryptoKeyPair createAccountKeyPair()
	{
		return m_keyPool.popKeyPair();
	}

	/**
	 * Defined if error messages from the AI should be ignored. The connection mightbe closed, but the
	 * listeners will not get informed about the problem.
	 * @param a_bIgnore boolean
	 */
	public void setIgnoreAIAccountError(boolean a_bIgnore)
	{
		m_bIgnoreAIAccountErrorMessages = a_bIgnore;
	}

	/**
	 * Returns if account balances are automatically updated.
	 * @return if account balances are automatically updated
	 */
	public boolean isBalanceAutoUpdateEnabled()
	{
		return m_bEnableBalanceAutoUpdate;
	}

	public void setBalanceAutoUpdateEnabled(boolean a_bEnable)
	{
		synchronized (this)
		{
			if (m_bEnableBalanceAutoUpdate != a_bEnable)
			{
				m_bEnableBalanceAutoUpdate = a_bEnable;
				setChanged();
			}
			notifyObservers(CHANGED_AUTO_UPDATE);
		}
	}

	/**
	 * Returns if error messages from the AI should be ignored.
	 * @return boolean
	 */
	public boolean isAIAccountErrorIgnored()
	{
		return m_bIgnoreAIAccountErrorMessages;
	}

	/**
	 * Performs the initialization.
	 * @param a_passwordReader  a password reader for encrypted account files; message: AccountNumber
	 * @return boolean succeeded?
	 */
	public static boolean init(Element elemAccountsFile, IMiscPasswordReader a_passwordReader,
							   boolean a_bForceAIErrors)
	{
		if (ms_AccountsFile == null)
		{
			ms_AccountsFile = new PayAccountsFile();
		}
		//ms_AccountsFile.m_theBI = theBI;
		if (elemAccountsFile != null && elemAccountsFile.getNodeName().equals(XML_ELEMENT_NAME))
		{
			if (a_bForceAIErrors)
			{
				ms_AccountsFile.m_bIgnoreAIAccountErrorMessages = false;
			}
			else
			{
				ms_AccountsFile.m_bIgnoreAIAccountErrorMessages =
					XMLUtil.parseAttribute(elemAccountsFile, XML_ATTR_IGNORE_AI_ERRORS, false);
			}
			ms_AccountsFile.m_bEnableBalanceAutoUpdate =
				XMLUtil.parseAttribute(elemAccountsFile, XML_ATTR_ENABLE_BALANCE_AUTO_UPDATE, true);
			Element elemActiveAccount = (Element) XMLUtil.getFirstChildByName(elemAccountsFile,
				"ActiveAccountNumber");
			long activeAccountNumber = Long.parseLong(XMLUtil.parseValue(elemActiveAccount, "0"));

			Element elemAccounts = (Element) XMLUtil.getFirstChildByName(elemAccountsFile, "Accounts");
			Element elemAccount = (Element) XMLUtil.getFirstChildByName(elemAccounts, PayAccount.XML_ELEMENT_NAME);
			while (elemAccount != null)
			{
				try
				{
					PayAccount theAccount = new PayAccount(elemAccount, a_passwordReader);

					ms_AccountsFile.addAccount(theAccount);
					//do NOT explicitly add PayAccountsFile as MessageListener - addAccount() already includes that, would duplicate all messages to have two Listeners
					//theAccount.addMessageListener(PayAccountsFile.getInstance());


					//load messages from the balances we already have (future messages are only displayed if different from existing ones)
					XMLBalance existingBalance = theAccount.getAccountInfo().getBalance();
					PayMessage existingMessage = existingBalance.getMessage();
					if (existingMessage != null && !existingMessage.getShortMessage().equals(""))
					{
						ms_AccountsFile.messageReceived(existingMessage);
					}

					elemAccount = (Element) elemAccount.getNextSibling();
				}
				catch (Exception e)
				{
					return false;
				}
			}

			// find activeAccount
			if (activeAccountNumber > 0)
			{
				Enumeration e = ms_AccountsFile.m_Accounts.elements();
				while (e.hasMoreElements())
				{
					PayAccount current = (PayAccount) e.nextElement();
					if (current.getAccountNumber() == activeAccountNumber)
					{
						try
						{
							ms_AccountsFile.setActiveAccount(current);
						}
						catch (Exception ex)
						{
						}
						break ;
					}
				}
			}
		}
		ms_AccountsFile.m_bIsInitialized = true;
		return true;
	}

	/**
	 * Thrown if an account with same account number was already existing when trying to add it.
	 */
	public static class AccountAlreadyExistingException extends Exception
	{
	}

	/**
	 * constructs the xml structure
	 *
	 * @return Element
	 */
	public Element toXmlElement(Document a_doc)
	{
		return this.toXmlElement(a_doc, null);
	}

	public Element toXmlElement(Document a_doc, String a_password)
	{
		try
			{
				Element elemAccount;
				Element elemAccountsFile = a_doc.createElement(XML_ELEMENT_NAME);

				elemAccountsFile.setAttribute(XML_ATTR_VERSION, "1.0");
				XMLUtil.setAttribute(elemAccountsFile, XML_ATTR_IGNORE_AI_ERRORS,
									 m_bIgnoreAIAccountErrorMessages);
				XMLUtil.setAttribute(elemAccountsFile, XML_ATTR_ENABLE_BALANCE_AUTO_UPDATE,
									 m_bEnableBalanceAutoUpdate);


				Element elem = a_doc.createElement("ActiveAccountNumber");
				XMLUtil.setValue(elem, Long.toString(getActiveAccountNumber()));
				elemAccountsFile.appendChild(elem);

				elem = a_doc.createElement("Accounts");
				elemAccountsFile.appendChild(elem);

				synchronized (this)
				{
					for (int i = 0; i < m_Accounts.size(); i++)
					{
						PayAccount account = (PayAccount) m_Accounts.elementAt(i);
						elemAccount = account.toXmlElement(a_doc, a_password);
						elem.appendChild(elemAccount);
					}
				}
				return elemAccountsFile;
			}
			catch (Exception ex)
			{
				LogHolder.log(LogLevel.EXCEPTION, LogType.PAY,
							  "Exception while creating PayAccountsFile XML: " + ex);
				return null;
			}
	}

	public boolean hasActiveAccount()
	{
		return m_ActiveAccount != null;
	}

	public PayAccount getActiveAccount()
	{
		return m_ActiveAccount;
	}


	public void setActiveAccount(PayAccount a_account)
	{
		PayAccount account = null;
		
		if (a_account != null)
		{
			// look whether this account is in the database
			account = getAccount(a_account.getAccountNumber(), a_account.getPIID());
		}
		
		if (account != null && account.getPrivateKey() != null)
		{
			m_ActiveAccount = account;
			synchronized (m_paymentListeners)
			{
				Enumeration enumListeners = m_paymentListeners.elements();
				while (enumListeners.hasMoreElements())
				{
					( (IPaymentListener) enumListeners.nextElement()).accountActivated(m_ActiveAccount);
				}
			}
		}
		else if (account == null)
		{
			m_ActiveAccount = null;
			synchronized (m_paymentListeners)
			{
				Enumeration enumListeners = m_paymentListeners.elements();
				while (enumListeners.hasMoreElements())
				{
					( (IPaymentListener) enumListeners.nextElement()).accountActivated(m_ActiveAccount);
				}
			}
		}
	}

	public long getActiveAccountNumber()
	{
		PayAccount activeAccount = m_ActiveAccount;
		if (activeAccount != null)
		{
			return activeAccount.getAccountNumber();
		}
		else
		{
			return -1;
		}
	}

	/**
	 * Liefert PayAccount zur angegebenen Kontonummer.
	 *
	 * @param accountNumber Kontonummer
	 * @return {@link PayAccount} oder null, wenn kein Konto unter der angebenen
	 * Kontonummer vorhanden ist
	 */
	public synchronized PayAccount getAccount(long accountNumber, String a_piid)
	{
		PayAccount tmp = null;
		Enumeration enumer = m_Accounts.elements();
		while (enumer.hasMoreElements())
		{
			tmp = (PayAccount) enumer.nextElement();
			if (tmp.getAccountNumber() == accountNumber && 
					((a_piid == tmp.getPIID() || 
					(a_piid != null && tmp.getPIID() != null && a_piid.equals(tmp.getPIID())))))
			{
				break;
			}
			else
			{
				tmp = null;
			}
		}

		return tmp;
	}

	/**
	 * Deletes the account from the accountsfile and saves the file to disk.
	 * If the deleted account was the active account, the first remaining account
	 * will become the active account.
	 *
	 * @param accountNumber account number
	 * @throws Exception Wenn ein Fehler bei Dateizugriff auftrat
	 */
	public void deleteAccount(PayAccount a_account)
	{
		if (a_account == null)
		{
			return;
		}
		
		PayAccount accountToDelete = null;
		synchronized (this)
		{
			accountToDelete = getAccount(a_account.getAccountNumber(), a_account.getPIID());

			if (accountToDelete != null)
			{
				for (int i = 0; i < m_Accounts.size(); i++)
				{
					accountToDelete = (PayAccount) m_Accounts.elementAt(i);
					if (accountToDelete.getAccountNumber() == a_account.getAccountNumber())
					{
						m_Accounts.removeElementAt(i);
						break;
					}
				}
				//if deleted account had a message, remove it
				//because a deleted account will not be update any more -> JapNewView would not realize the message is gone
				if (accountToDelete.getBalance() != null)
				{
					PayMessage oldMessage = accountToDelete.getBalance().getMessage();
					if (oldMessage != null && !oldMessage.getShortMessage().equals("") )
					{
						fireMessageRemoved(oldMessage);
					}
				}

	            //get a new active account, if possible
				if (getActiveAccount() == accountToDelete)
				{
					if (m_Accounts.size() > 0)
					{
						setActiveAccount((PayAccount)m_Accounts.elementAt(0));
					}
					else
					{
						setActiveAccount(null);
					}
				}
			}
		}

		if (accountToDelete != null)
		{
			synchronized (m_paymentListeners)
			{
				Enumeration enumListeners = m_paymentListeners.elements();
				while (enumListeners.hasMoreElements())
				{
					( (IPaymentListener) enumListeners.nextElement()).accountRemoved(accountToDelete);
				}
			}
		}
	}

	/**
	 * Liefert alle Kontennummern der Kontendatei.
	 *
	 * @return Enumeration von Long-Werten
	 */
	/*	public Enumeration getAccountNumbers()
	 {
	  PayAccount tmpAccount;
	  Vector tmp = new Vector();
	  Enumeration enumer = m_Accounts.elements();
	  while (enumer.hasMoreElements())
	  {
	   tmpAccount = (PayAccount) enumer.nextElement();
	   tmp.addElement(new Long(tmpAccount.getAccountNumber()));
	  }
	  return tmp.elements();
	 }*/

	/**
	 * Returns an enumeration of all accounts
	 *
	 * @return Enumeration of {@link PayAccount}
	 */
	public Enumeration getAccounts()
	{
		return ((Vector)m_Accounts .clone()).elements();
	}

	/**
	* iterates over all accounts, and whenever there is a message it is sent to all MessageListeners
	* reason for existence: JapNewView needs to load all messages at start-up
	* (PayAccountsFile does already fire all message when it loads the accounts from the file, but that possibly happens before a MessageListener is
	* initialized and added as a listener to PayAccountsFile)
	*/
	public static void fireKnownMessages()
	{
		Enumeration allAccounts = getInstance().getAccounts();
		while (allAccounts.hasMoreElements() )
		{
			PayAccount theAccount = (PayAccount) allAccounts.nextElement();
			XMLAccountInfo info = theAccount.getAccountInfo();
			if (info != null)
			{
				XMLBalance existingBalance = theAccount.getAccountInfo().getBalance();
				PayMessage existingMessage = existingBalance.getMessage();
				if (existingMessage != null && !existingMessage.getShortMessage().equals(""))
				{
					ms_AccountsFile.fireMessageReceived(existingMessage);
				}
			}
		}
	}

	public synchronized PayAccount getAlternativeChargedAccount(String a_piid)
	{	
		return getChargedAccount(a_piid, getActiveAccount());
	}
	
	public synchronized PayAccount getChargedAccount(String a_piid)
	{
		return getChargedAccount(a_piid, null);
	}
	
	public synchronized PayAccount getChargedAccount(String a_piid, PayAccount a_excludeAccount)
	{
		Vector accounts = PayAccountsFile.getInstance().getAccounts(a_piid);
		Timestamp now = new Timestamp(System.currentTimeMillis());
		PayAccount currentAccount = null;
		
		if (a_excludeAccount != null && !accounts.contains(a_excludeAccount))
		{
			// good, we can choose the first account we will find
			a_excludeAccount = null;
		}

		if (accounts.size() > 0)
		{
			for (int i = 0; i < accounts.size(); i++)
			{
				currentAccount = (PayAccount) accounts.elementAt(i);
				if (currentAccount.isCharged(now) &&
					(a_excludeAccount == null || a_excludeAccount != currentAccount))
				{
					break;
				}
				currentAccount = null;
			}
		}
		return currentAccount;
	}


	public synchronized Vector getAccounts(String a_piid)
	{
		Vector vecAccounts = new Vector();
		Enumeration enumer = m_Accounts.elements();
		PayAccount currentAccount;
		PaymentInstanceDBEntry pi;

		if (a_piid != null && a_piid.trim().length() > 0)
		{
			while (enumer.hasMoreElements())
			{
				currentAccount = (PayAccount) enumer.nextElement();
				pi = currentAccount.getBI();
				if (pi == null)
				{
					// this may happen sometimes... but it should not!
					LogHolder.log(LogLevel.ERR, LogType.PAY,
								  "Payment instance for account nr. " +
								  currentAccount.getAccountNumber() +
								  " not found!");
					continue;
				}
				if (pi.getId().equals(a_piid))
				{
					vecAccounts.addElement(currentAccount);
				}
			}
		}

		return vecAccounts;
	}

	/**
	 * Adds a new account
	 *
	 * @param account new account
	 * @throws Exception If the same account was already added
	 */
	public synchronized void addAccount(PayAccount newAccount) throws AccountAlreadyExistingException
	{
		PayAccount tmp;
		boolean activeChanged = false;

		Enumeration enumer = m_Accounts.elements();
		while (enumer.hasMoreElements())
		{
			tmp = (PayAccount) enumer.nextElement();
			if (tmp.getAccountNumber() == newAccount.getAccountNumber())
			{
				throw new AccountAlreadyExistingException();
			}
		}
		newAccount.addAccountListener(m_MyAccountListener);
		newAccount.addMessageListener(this);
		m_Accounts.addElement(newAccount);

		if (m_ActiveAccount == null && newAccount.getPrivateKey() != null)
		{
			m_ActiveAccount = newAccount;
			activeChanged = true;
		}



		// notify paymen listeners
		synchronized (m_paymentListeners)
		{
			Enumeration enumListeners = m_paymentListeners.elements();
			IPaymentListener pl;
			while (enumListeners.hasMoreElements())
			{
				pl = (IPaymentListener) enumListeners.nextElement();
				pl.accountAdded(newAccount);
				if (activeChanged == true)
				{
					pl.accountActivated(newAccount);
				}
			}
			enumListeners = null;
			pl = null;
		}
	}

	/**
	 * getNumAccounts
	 *
	 * @return int
	 */
	public int getNumAccounts()
	{
		return m_Accounts.size();
	}

	/**
	 * getAccountAt
	 *
	 * @param rowIndex int
	 * @return PayAccount
	 */
	public synchronized PayAccount getAccountAt(int rowIndex)
	{
		return (PayAccount) m_Accounts.elementAt(rowIndex);
	}

	/**
	 * isInitialized
	 *
	 * @return boolean
	 */
	public boolean isInitialized()
	{
		return m_bIsInitialized;
	}

	public void addPaymentListener(IPaymentListener listener)
	{
		synchronized (m_paymentListeners)
		{
			if (listener != null)
			{
				m_paymentListeners.addElement(listener);
			}
		}
	}

	public void removePaymentListener(IPaymentListener a_listener)
	{
		synchronized (m_paymentListeners)
		{
			if (m_paymentListeners.contains(a_listener))
			{
				m_paymentListeners.removeElement(a_listener);
			}
		}
	}

	public void addMessageListener(IMessageListener listener)
	{
		synchronized (m_messageListeners)
		{
			if (listener != null)
			{
				m_messageListeners.addElement(listener);
			}
		}
	}

	private void fireMessageReceived(PayMessage message)
	{
		Enumeration enumListeners = ( (Vector) m_messageListeners.clone()).elements();
		while (enumListeners.hasMoreElements())
		{
			( (IMessageListener) enumListeners.nextElement()).messageReceived(message);
		}
	}

	private void fireMessageRemoved(PayMessage message)
	{
		Enumeration enumListeners = ( (Vector) m_messageListeners.clone()).elements();
		while (enumListeners.hasMoreElements())
		{
			( (IMessageListener) enumListeners.nextElement()).messageRemoved(message);
		}

	}


	/**
	 * getBI
	 *
	 * @return BI
	 */
	/*	public BI getBI()
	{
		return m_theBI;
	 }*/

	/**
	 * Listens to changes
	 * inside the accounts and forwards the events to our paymentListeners
	 */
	private class MyAccountListener implements IAccountListener
	{
		/**
		 * accountChanged
		 *
		 * @param acc PayAccount
		 */
		public void accountChanged(PayAccount acc)
		{
			// fire event
			Enumeration enumListeners;
			//synchronized (m_paymentListeners) // deadly for jdk 1.1.8...
			{
				/*
				 *  Clone the vector and leave synchronisation block as otherwise there
				 * would be a deadlock with fireChangeEvent in PayAccount:
				 * PayAccount.m_accountListeners:Vector,
				 * PayAccountsFile.m_paymentListeners:Vector
				 */
				enumListeners = ((Vector)m_paymentListeners.clone()).elements();
			}

			if (acc != null)
			{
				while (enumListeners.hasMoreElements())
				{
					( (IPaymentListener) enumListeners.nextElement()).creditChanged(acc);
				}
			}
		}
	}

	public PayAccount createAccount(PaymentInstanceDBEntry a_bi, XMLGenericText a_terms) throws Exception
	{
		AsymmetricCryptoKeyPair keyPair = createAccountKeyPair();
		if (keyPair == null)
		{
			return null;
		}
		return createAccount(a_bi, keyPair, a_terms);
	}
	
	/**
	 * Creates a new Account.
	 * Generates an RSA or DSA key pair and then registers a new account with the BI.
	 * This can take a while, so the user should be notified before calling this.
	 *
	 * At the moment, only DSA should be used, because RSA is not supported by the
	 * AI implementation
	 * @param a_keyPair RSA should not be used at the moment
	 *
	 */
	public PayAccount createAccount(PaymentInstanceDBEntry a_bi,
									AsymmetricCryptoKeyPair a_keyPair, XMLGenericText a_terms) 
		throws Exception
	{
		XMLJapPublicKey xmlKey = new XMLJapPublicKey(a_keyPair.getPublic());

		LogHolder.log(LogLevel.DEBUG, LogType.PAY,
					  "Attempting to create account at PI " + a_bi.getName());
		//System.out.println(a_bi.getId());
		BIConnection biConn = new BIConnection(a_bi);
		biConn.addConnectionListener(this);
		biConn.connect();
		XMLAccountCertificate cert = biConn.registerNewAccount(xmlKey, a_keyPair.getPrivate());
		biConn.disconnect();

		// add the new account to the accountsFile
		PayAccount newAccount = new PayAccount(cert, a_keyPair.getPrivate(), a_bi, a_terms);
		addAccount(newAccount);
		return newAccount;
	}

	/**
	 * signalAccountRequest
	 */
	public int signalAccountRequest(MixCascade a_connectedCascade)
	{
		int m_bSuccess = ErrorCodes.E_SUCCESS;
		synchronized (m_paymentListeners)
		{
			Enumeration enumListeners = m_paymentListeners.elements();
			while (enumListeners.hasMoreElements())
			{
				if ((m_bSuccess = 
					( (IPaymentListener) enumListeners.nextElement()).accountCertRequested(a_connectedCascade)) !=
						ErrorCodes.E_SUCCESS)
				{
					break;
				}
			}
		}
		return m_bSuccess;
	}

	/**
	 * signalAccountError
	 *
	 * @param msg XMLErrorMessage
	 */
	public void signalAccountError(XMLErrorMessage msg)
	{
		synchronized (m_paymentListeners)
		{
			IPaymentListener currentListener = null;
			Enumeration enumListeners = m_paymentListeners.elements();
			while (enumListeners.hasMoreElements())
			{
				currentListener = (IPaymentListener) enumListeners.nextElement();
				currentListener.accountError(msg, m_bIgnoreAIAccountErrorMessages);
			}
		}
	}



	public Vector getPaymentInstances()
	{
		return Database.getInstance(PaymentInstanceDBEntry.class).getEntryList();
	}

	public PaymentInstanceDBEntry getBI(String a_piID)
	{
		PaymentInstanceDBEntry theBI =
			(PaymentInstanceDBEntry)Database.getInstance(PaymentInstanceDBEntry.class).getEntryById(a_piID);
		return theBI;
	}

	/**
	 * This method is called whenever a captcha has been received from the
	 * Payment Instance.
	 * @param a_source Object
	 * @param a_captcha IImageEncodedCaptcha
	 */
	public void gotCaptcha(ICaptchaSender a_source, final IImageEncodedCaptcha a_captcha)
	{
		synchronized (m_paymentListeners)
		{
			Enumeration enumListeners = m_paymentListeners.elements();
			while (enumListeners.hasMoreElements())
			{
				( (IPaymentListener) enumListeners.nextElement()).gotCaptcha(a_source, a_captcha);
			}
		}
	}

	/**
	 * just passes through a received message to the PayAccountsFile's MessageListeners
	 */
	public void messageReceived(PayMessage message)
	{
		fireMessageReceived(message);
	}

	public void messageRemoved(PayMessage message)
	{
		fireMessageRemoved(message);
	}
}
