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

/**
 * This class encapsulates a connection to the Payment Instance, performs authentication
 * and contains other methods for interaction with the Payment Instance.
 *
 * @author Grischan Glaenzel, Bastian Voigt, Tobias Bayer
 */
import java.io.IOException;
import java.net.Socket;
import java.util.Date;
import java.util.Enumeration;
import java.util.Vector;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import HTTPClient.ForbiddenIOException;
import anon.crypto.ByteSignature;
import anon.crypto.IMyPrivateKey;
import anon.crypto.XMLSignature;
import anon.crypto.tinytls.TinyTLS;
import anon.infoservice.IMutableProxyInterface;
import anon.infoservice.ImmutableProxyInterface;
import anon.infoservice.ListenerInterface;
import anon.pay.xml.XMLAccountCertificate;
import anon.pay.xml.XMLAccountInfo;
import anon.pay.xml.XMLBalance;
import anon.pay.xml.XMLChallenge;
import anon.pay.xml.XMLErrorMessage;
import anon.pay.xml.XMLJapPublicKey;
import anon.pay.xml.XMLPassivePayment;
import anon.pay.xml.XMLPaymentOptions;
import anon.pay.xml.XMLResponse;
import anon.pay.xml.XMLTransCert;
import anon.pay.xml.XMLTransactionOverview;
import anon.util.XMLUtil;
import anon.util.captcha.ICaptchaSender;
import anon.util.captcha.IImageEncodedCaptcha;
import anon.util.captcha.ZipBinaryImageCaptchaClient;
import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;
import anon.infoservice.IMutableProxyInterface;
import anon.pay.xml.XMLPaymentSettings;
import anon.pay.xml.XMLVolumePlans;
import anon.util.IXMLEncodable;
import anon.pay.xml.XMLGenericText;
import anon.pay.xml.XMLGenericStrings;

public class BIConnection implements ICaptchaSender
{
	public static final int TIMEOUT_DEFAULT = 30000;
	public static final int TIMEOUT_MAX = 100000;

	public static final String XML_ATTR_CONNECTION_TIMEOUT = "timeout";

	private static int ms_connectionTimeout = TIMEOUT_DEFAULT;

	private PaymentInstanceDBEntry m_theBI;

	private Socket m_socket;
	private HttpClient m_httpClient;

	private Vector m_biConnectionListeners;

	private byte[] m_captchaSolution;

	private boolean m_bSendNewCaptcha;
	private boolean m_bFirstCaptcha = true;

	IMutableProxyInterface m_proxyInterface = null;

	/**
	 * Constructor
	 *
	 * @param BI the BI to which we connect
	 */
	public BIConnection(PaymentInstanceDBEntry theBI)
	{
		m_theBI = theBI;
		m_biConnectionListeners = new Vector();
	}

	public static void setConnectionTimeout(int a_timeout)
	{
		if (a_timeout >= 0)
		{
			if (a_timeout > TIMEOUT_MAX)
			{
				ms_connectionTimeout = TIMEOUT_MAX;
			}
			else
			{
				ms_connectionTimeout = a_timeout;
			}
		}
		else
		{
			ms_connectionTimeout = 0;
		}
	}

	public static int getConnectionTimeout()
	{
		return ms_connectionTimeout;
	}

	/**
	 * Connects to the Payment Instance via TCP and inits the HttpClient.
	 *
	 * @throws IOException if an error occured while connection
	 * @throws ForbiddenIOException if it is assumed that the local provider forbids the connection
	 */
	public void connect(IMutableProxyInterface a_proxyInterface) throws IOException
	{
		IOException exception = new IOException("No valid proxy available");

		if (a_proxyInterface == null)
		{
			throw exception;
		}

		m_proxyInterface = a_proxyInterface;
		IMutableProxyInterface.IProxyInterfaceGetter proxyInterfaceGetter;
		boolean bAnonProxy = false;

		for (int i = 0; (i < 2) && !Thread.currentThread().isInterrupted(); i++)
		{
			if (i == 1)
			{
				bAnonProxy = true;
			}

			proxyInterfaceGetter = a_proxyInterface.getProxyInterface(bAnonProxy);
			if (proxyInterfaceGetter == null)
			{
				continue;
			}

			try
			{
				//Try to connect to BI...
				connect_internal(proxyInterfaceGetter.getProxyInterface());
				return;
			}
			catch (IOException a_t)
			{
				//Could not connect to BI
				exception = a_t;
			}
		}

		throw exception;
	}

	private void connect_internal(ImmutableProxyInterface a_proxy) throws IOException
	{
		boolean bForbidden = false;

		TinyTLS tls = null;
		ListenerInterface li = null;
		boolean connected = false;

		Enumeration listeners = m_theBI.getListenerInterfaces();
		while (listeners.hasMoreElements())
		{
			li = (ListenerInterface) listeners.nextElement();
			LogHolder.log(LogLevel.DEBUG, LogType.PAY,
						  "Trying to connect to Payment Instance at " + li.getHost() + ":" +
						  li.getPort() + ".");
			try
			{
				if (a_proxy == null)
				{
					tls = new TinyTLS(li.getHost(), li.getPort());
				}
				else
				{
					LogHolder.log(LogLevel.INFO, LogType.PAY, "Using proxy at " + a_proxy.getHost() +
								  ":" + a_proxy.getPort());
					tls = new TinyTLS(li.getHost(), li.getPort(), a_proxy);
				}
				m_socket = tls;
				tls.setSoTimeout(ms_connectionTimeout);
				tls.setRootKey(m_theBI.getCertificate().getPublicKey());
				tls.startHandshake();

				m_httpClient = new HttpClient(m_socket);
				connected = true;
				break;
			}
			catch (Exception e)
			{
				if (m_httpClient != null)
				{
					try
					{
						m_httpClient.close();
					}
					catch (Exception ex)
					{
						LogHolder.log(LogLevel.ERR, LogType.NET, ex);
					}
				}
				else if (m_socket != null)
				{
					try
					{
						m_socket.close();
					}
					catch (IOException a_e)
					{
						LogHolder.log(LogLevel.ERR, LogType.NET, a_e);
					}
				}

				// try to recognize if the provider forbids the connection
				if (e instanceof ForbiddenIOException)
				{
					bForbidden = true;
				}

				if (listeners.hasMoreElements())
				{
					LogHolder.log(LogLevel.ERR, LogType.PAY,
								  "Could not connect to Payment Instance at " + li.getHost() + ":" +
								  li.getPort() + ". Trying next interface...", e);
				}
				else
				{
					LogHolder.log(LogLevel.EXCEPTION, LogType.PAY,
								  "Could not connect to Payment Instance at " + li.getHost() + ":" +
								  li.getPort() + ". No more interfaces left.", e);
				}
			}
		}
		if (!connected)
		{
			String error = "Could not connect to Payment Instance";
			if (bForbidden)
			{
				throw new ForbiddenIOException(error);
			}
			else
			{
				throw new IOException(error);
			}
		}
		else
		{
			LogHolder.log(LogLevel.INFO, LogType.PAY,
						  "Connected to Payment Instance at " + li.getHost() + ":" +
						  li.getPort() + ".", true);
		}

	}

	/**
	 * Closes the connection.
	 *
	 * @throws IOException
	 */
	public void disconnect() throws Exception
	{
		m_httpClient.close();
	}

	/**
	 * Fetches a transfer certificate from the BI.
	 * @return XMLTransCert the transfer certificate
	 */
	public XMLTransCert charge(XMLGenericStrings parameters) throws Exception
	{
		m_httpClient.writeRequest("POST", "charge", XMLUtil.toString(parameters.toXmlElement(XMLUtil.createDocument())));
		Document doc = m_httpClient.readAnswer();

		XMLTransCert cert = new XMLTransCert(doc);
		//debug
		//System.out.println(XMLUtil.toString(XMLUtil.toXMLDocument(cert)));


		if (!XMLSignature.verifyFast(doc, m_theBI.getCertificate().getPublicKey()))
		{
			throw new Exception("The BI's signature under the transfer certificate is invalid");
		}


		cert.setReceivedDate(new Date());
		return cert;
	}

	public XMLErrorMessage buyFlatrate(long accountnumber) throws Exception
	{
		m_httpClient.writeRequest("POST", "buyflat", (new Long(accountnumber)).toString());
		Document doc = m_httpClient.readAnswer();
		XMLErrorMessage messageReturned = new XMLErrorMessage(doc);
		return messageReturned;
	}

	/**
	 * Fetches an account statement (balance cert. + costconfirmations)
	 * from the BI.
	 * @return the statement in XMLAccountInfo format
	 * @throws IOException
	 */
	public XMLAccountInfo getAccountInfo() throws Exception
	{
		XMLAccountInfo info = null;
		m_httpClient.writeRequest("GET", "balance", null);
		Document doc = m_httpClient.readAnswer();
		info = new XMLAccountInfo(doc);
		XMLBalance bal = info.getBalance();
		if (XMLSignature.verify(XMLUtil.toXMLDocument(bal), m_theBI.getCertificate()) == null)
		{
			throw new Exception("The BI's signature under the balance certificate is Invalid!");
		}
		return info;
	}

	/**
	 * Fetches payment options.
	 * @return XMLPaymentOptions
	 * @throws Exception
	 */
	public XMLPaymentOptions getPaymentOptions() throws Exception
	{
		XMLPaymentOptions options;
		m_httpClient.writeRequest("GET", "paymentoptions", null);
		Document doc = m_httpClient.readAnswer();
		options = new XMLPaymentOptions(doc);
		return options;
	}

	public XMLVolumePlans getVolumePlans() throws Exception
	{
		XMLVolumePlans plans;
		m_httpClient.writeRequest("GET", "volumeplans", null);
		Document doc = m_httpClient.readAnswer();
		plans = new XMLVolumePlans(doc);
		return plans;
	}

	public XMLGenericText getTerms(String lang) throws Exception
	{

		m_httpClient.writeRequest("POST","terms",lang);
		Document doc = m_httpClient.readAnswer();
		XMLGenericText terms;
		try
		{
			terms = new XMLGenericText(doc);
		} catch (Exception e) // e.g. JPI returned an ErrorMessage instead of GenericText
		{
			return null;
		}
		return terms;
	}

	public XMLGenericText getCancellationPolicy(String lang) throws Exception
	{

		m_httpClient.writeRequest("POST","cancellationpolicy",lang);
		Document doc = m_httpClient.readAnswer();
		XMLGenericText policy;
		try
		{
			policy = new XMLGenericText(doc);
		} catch (Exception e) // e.g. JPI returned an ErrorMessage instead of GenericText
		{
			return null;
		}
		return policy;
	}


	public XMLPaymentSettings getPaymentSettings() throws Exception
	{
		m_httpClient.writeRequest("GET", "paymentsettings",null);
		Document doc = m_httpClient.readAnswer();
		return new XMLPaymentSettings(doc);
	}

	/** performs challenge-response authentication */
	public void authenticate(XMLAccountCertificate accountCert, IMyPrivateKey a_privateKey) throws Exception
	{
		String StrAccountCert = XMLUtil.toString(XMLUtil.toXMLDocument(accountCert));
		m_httpClient.writeRequest("POST", "authenticate", StrAccountCert);
		Document doc = m_httpClient.readAnswer();
		String tagname = doc.getDocumentElement().getTagName();
		if (tagname.equals(XMLChallenge.XML_ELEMENT_NAME))
		{
			XMLChallenge xmlchallenge = new XMLChallenge(doc);
			byte[] challenge = xmlchallenge.getChallengeForSigning();
			byte[] response = ByteSignature.sign(challenge, a_privateKey);
			XMLResponse xmlResponse = new XMLResponse(response);
			String strResponse = XMLUtil.toString(XMLUtil.toXMLDocument(xmlResponse));
			m_httpClient.writeRequest("POST", "response", strResponse);
			doc = m_httpClient.readAnswer();
			XMLErrorMessage message = new XMLErrorMessage(doc);
			if (message.getErrorCode() >= 0 && message.getErrorCode() != XMLErrorMessage.ERR_OK)
			{
				throw message;
			}
		}
		else if (tagname.equals(XMLErrorMessage.XML_ELEMENT_NAME))
		{
			/** @todo handle errormessage properly */
			throw new Exception("The BI sent an errormessage: " +
								new XMLErrorMessage(doc).getErrorDescription());
		}
	}

	/**
	 * Registers a new account using the specified keypair.
	 * Checks the signature and the public key of the accountCertificate
	 * that is received.
	 *
	 * @param pubKey public key
	 * @param privKey private key
	 * @return XMLAccountCertificate the certificate issued by the BI
	 * @throws Exception if an error occurs or the signature or public key is wrong
	 */
	public XMLAccountCertificate registerNewAccount(XMLJapPublicKey pubKey, IMyPrivateKey a_privateKey) throws Exception
	{
		Document doc;
		byte[] challenge = null;

		m_bSendNewCaptcha = true;
		while (m_bSendNewCaptcha)
		{
			if (!m_bFirstCaptcha)
			{
				try
				{
					this.disconnect();
				}
				catch (Exception e)
				{

					LogHolder.log(LogLevel.INFO, LogType.PAY,
								  "Not connected to payment instance while trying to disconnect");
				}
				this.connect(m_proxyInterface);
			}
			// send our public key
			m_httpClient.writeRequest(
				"POST", "register",
				XMLUtil.toString(XMLUtil.toXMLDocument(pubKey))
				);
			doc = m_httpClient.readAnswer();

			try
			{
				challenge = new XMLChallenge(doc.getDocumentElement()).getChallengeForSigning();
				m_bSendNewCaptcha = false;
				break;
			}
			catch (Exception a_e)
			{
				LogHolder.log(LogLevel.WARNING, LogType.PAY,
							  "No challenge sent directly while registering account, trying capchta...");
			}
			//Answer document should contain a captcha, let the user solve it and extract the XMLChallenge
			IImageEncodedCaptcha captcha = new ZipBinaryImageCaptchaClient(doc.getDocumentElement());
			m_bSendNewCaptcha = false;
			fireGotCaptcha(captcha);
		}

		if (m_captchaSolution != null)
		{
			/** Cut off everything beyond the last ">" to extract only the XML challenge
			 *  without the cipher padding.
			 */
			String challengeString = new String(m_captchaSolution);
			int pos = challengeString.lastIndexOf(">");
			challengeString = challengeString.substring(0, pos + 1);
			int pos1 = challengeString.indexOf(">") + 1;
			int pos2 = challengeString.lastIndexOf("<");
			challengeString = challengeString.substring(pos1, pos2);
			challengeString = "<DontPanic>" + challengeString + "</DontPanic>";
			challenge = challengeString.getBytes();
		}
		else if (challenge == null)
		{
			throw new Exception("CAPTCHA");
		}

		// perform challenge-response authentication
		XMLAccountCertificate xmlCert = null;
		byte[] response = ByteSignature.sign(challenge, a_privateKey);
		XMLResponse xmlResponse = new XMLResponse(response);
		String strResponse = XMLUtil.toString(XMLUtil.toXMLDocument(xmlResponse));
		m_httpClient.writeRequest("POST", "response", strResponse);
		doc = m_httpClient.readAnswer();
		// check signature
		if (!XMLSignature.verifyFast(doc, m_theBI.getCertificate().getPublicKey()))
		{
			throw new Exception("AccountCertificate: Wrong signature!");
		}
		xmlCert = new XMLAccountCertificate(doc.getDocumentElement());
		if (!xmlCert.getPublicKey().equals(pubKey.getPublicKey()))
		{
			throw new Exception(
				"The JPI is evil (sent a valid certificate, but with a wrong publickey)");
		}
		return xmlCert;

	}

	/**
	 * Gets the payment options the PI provides.
	 * @return XMLPaymentOptions
	 * @throws Exception
	 */
	public XMLPaymentOptions fetchPaymentOptions() throws Exception
	{
		m_httpClient.writeRequest("GET", "paymentoptions", null);
		Document doc = m_httpClient.readAnswer();
		XMLPaymentOptions paymentoptions = new XMLPaymentOptions(doc.getDocumentElement());
		return paymentoptions;
	}

	public IXMLEncodable fetchPaymentData(String transfernumber) throws Exception
	{
		IXMLEncodable paymentData;
		m_httpClient.writeRequest("POST", "paymentdata",transfernumber);
		Document doc = m_httpClient.readAnswer();
		if (doc == null) //should never happen, BI return XMLErrorMessage at worst
		{
			return null;
		}
		if (doc.getDocumentElement().getTagName().equalsIgnoreCase(XMLPassivePayment.XML_ELEMENT_NAME) )
		{
			paymentData = new XMLPassivePayment(doc.getDocumentElement());
		} else
		{
			paymentData = new XMLErrorMessage(doc.getDocumentElement());
        }
		return paymentData;
	}

	/**
	 * Asks the PI to fill an XMLTransactionOverview
	 * @param a_overview XMLTransactionOverview
	 * @return XMLTransactionOverview
	 * @throws Exception
	 */
	public XMLTransactionOverview fetchTransactionOverview(XMLTransactionOverview a_overview) throws
		Exception
	{
		String theOverview =  XMLUtil.toString(a_overview.toXmlElement(XMLUtil.createDocument()));
		m_httpClient.writeRequest("POST", "transactionoverview",theOverview);
		Document doc = m_httpClient.readAnswer();

	    //check if what came back is actually an XMLTransactionOverview, or rather an XMLErrorMessage
		Element rootElem = doc.getDocumentElement();
		if (rootElem.getTagName().equalsIgnoreCase(XMLErrorMessage.XML_ELEMENT_NAME) )
		{
			return null;
		}
		else
		{
			XMLTransactionOverview overview = new XMLTransactionOverview(doc.getDocumentElement());
			String theReturnedOverview = XMLUtil.toString(overview.toXmlElement(XMLUtil.createDocument()));
			return overview;
		}
	}

	/**
	 * Sends data the user has entered for a passive payment to the payment
	 * instance.
	 * @param a_passivePayment XMLPassivePayment
	 * @throws Exception
	 */
	public boolean sendPassivePayment(XMLPassivePayment a_passivePayment)
	{
		try
		{
			String passivePaymentString = XMLUtil.toString(a_passivePayment.toXmlElement(XMLUtil.createDocument()));
			m_httpClient.writeRequest("POST", "passivepayment",passivePaymentString);
			Document doc = m_httpClient.readAnswer();
			XMLErrorMessage err = new XMLErrorMessage(doc.getDocumentElement());
			if (err.getErrorCode() == XMLErrorMessage.ERR_OK)
			{
				return true;
			}
			else
			{
				return false;
			}
		}
		catch (Exception e)
		{
			LogHolder.log(LogLevel.EXCEPTION, LogType.PAY,
						  "Could not send PassivePayment to payment instance: " + e);
			return false;
		}
	}

	public boolean checkCouponCode(String couponCode)
	{
		try
		{
			m_httpClient.writeRequest("POST", "coupon",couponCode);
			Document doc = m_httpClient.readAnswer();
			XMLErrorMessage err = new XMLErrorMessage(doc.getDocumentElement());
			if (err.getErrorCode() == XMLErrorMessage.ERR_OK)
			{
				return true;
			}
			else
			{
				LogHolder.log(LogLevel.DEBUG, LogType.PAY, "User entered an invalid coupon, reply from jpi was: "+ err.getMessage() );
				return false;
			}
		}
		catch (Exception e)
		{
			LogHolder.log(LogLevel.EXCEPTION, LogType.PAY,"BIConnection.checkCouponCode: Could not check coupon validity due to: " + e + " so I'll return false");
			return false;
		}
	}

	/**
	 * Adds an IBIConnectionListener
	 * @param a_listener IBIConnectionListener
	 */
	public void addConnectionListener(IBIConnectionListener a_listener)
	{
		if (!m_biConnectionListeners.contains(a_listener))
		{
			m_biConnectionListeners.addElement(a_listener);
		}
	}

	/**
	 * Signals a received captcha to all registered IBICOnnectionListeners.
	 * @param a_captcha IImageEncodedCaptcha
	 */
	private void fireGotCaptcha(IImageEncodedCaptcha a_captcha)
	{
		for (int i = 0; i < m_biConnectionListeners.size(); i++)
		{
			( (IBIConnectionListener) m_biConnectionListeners.elementAt(i)).gotCaptcha(this, a_captcha);
		}
	}

	/**
	 * Sets the solution of a captcha for registering an account.
	 * @param a_solution byte[]
	 */
	public void setCaptchaSolution(byte[] a_solution)
	{
		m_captchaSolution = a_solution;
	}

	public void getNewCaptcha()
	{
		m_bSendNewCaptcha = true;
		m_bFirstCaptcha = false;
	}

}
