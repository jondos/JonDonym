package anon.pay.xml;

import java.net.MalformedURLException;
import java.net.URL;
import java.sql.Timestamp;

import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;
import org.w3c.dom.Document;
import org.w3c.dom.Element;


import anon.crypto.IMyPrivateKey;
import anon.crypto.XMLSignature;
import anon.pay.PayMessage;
import anon.util.Base64;
import anon.util.IXMLEncodable;
import anon.util.XMLParseException;
import anon.util.XMLUtil;

/**
 * This class holds a balance certificate. Can be converted to and from
 * XML
 *
 * @todo find a better internal representation for the signature
 */
public class XMLBalance implements IXMLEncodable
{
	private static final String DEFAULT_RATE_ENDDATE = "3000-01-01 00:00:00.00000000";
	
	/** the number of the account that this balance object belongs to*/ 
	private long m_lAccountNumber;
	
	private java.sql.Timestamp m_Timestamp;
	
	/** bytes that are not consumed if there are any will expire after that date */
	private java.sql.Timestamp m_ValidTime;
	
	/** the amount of money which the user has paid for the corresponding volume plan */
	private long m_lDeposit;
	
	/** 
	 * the amount of byte which are already consumed for the 
	 * corresponding account 
	 */
	private long m_lSpent;
	
	private java.sql.Timestamp m_flatEnddate;
	
	/**  the kbytes that are still left to spend for the corresponding account */
	private long m_volumeKBytesleft;
	
	/** */
	//private int m_balance;
	
	private String m_message;
	
	private String m_messageText;
	
	private URL m_messageLink;

	private Document m_docTheBalance = null;

	public XMLBalance(long accountNumber,
					  long deposit, long spent,
					  Timestamp timestamp,
					  Timestamp validTime,
					  //int balance,
					  long volumeBytesleft,
					  Timestamp flatEnddate,
					  IMyPrivateKey signKey)
	{
		m_lDeposit = deposit;
		m_lSpent = spent;
		m_Timestamp = timestamp;
		if (m_Timestamp == null)
		{
			m_Timestamp = new Timestamp(System.currentTimeMillis());
		}
		m_ValidTime = validTime;
		if (m_ValidTime == null)
		{
			m_ValidTime = Timestamp.valueOf(DEFAULT_RATE_ENDDATE);
		}
		m_lAccountNumber = accountNumber;
		//m_balance = balance;
		m_volumeKBytesleft = volumeBytesleft;
		m_flatEnddate = flatEnddate;
		if (m_flatEnddate == null)
		{
			m_flatEnddate = Timestamp.valueOf(DEFAULT_RATE_ENDDATE);
		}
		m_docTheBalance = XMLUtil.createDocument();
		m_docTheBalance.appendChild(internal_toXmlElement(m_docTheBalance));
		if (signKey != null) //might very well be null, when created by Database (which doesnt have access to the private key)
		{
			sign(signKey);
		}
	}

	public void sign(IMyPrivateKey signKey) //is public so we can create it first, and call sign later
	{
		try
		{
			XMLSignature.sign(m_docTheBalance, signKey);
		} catch (XMLParseException e)
		{
			LogHolder.log(LogLevel.DEBUG, LogType.PAY, "Could not sign XMLBalance");
		}
	}

	public void setMessage(PayMessage a_message)
	{
		if (a_message == null)
		{
			m_message = null;
			m_messageLink = null;
			m_messageText = null;
		}
		else
		{
			m_message = a_message.getShortMessage();
			m_messageLink = a_message.getMessageLink();
			m_messageText = a_message.getMessageText();
		}
		//need to recreate the internal do to include the message
		m_docTheBalance = XMLUtil.createDocument();
		m_docTheBalance.appendChild(internal_toXmlElement(m_docTheBalance));
	}



	public XMLBalance(Document doc) throws Exception
	{
		setValues(doc.getDocumentElement());
		m_docTheBalance = doc;
	}

	public XMLBalance(String xmlDoc) throws Exception
	{
		Document doc = XMLUtil.toXMLDocument(xmlDoc);
		setValues(doc.getDocumentElement());
		m_docTheBalance = doc;
	}

	public XMLBalance(Element elemBalance) throws Exception
	{
		setValues(elemBalance);
		m_docTheBalance = XMLUtil.createDocument();
		m_docTheBalance.appendChild(XMLUtil.importNode(m_docTheBalance, elemBalance, true));
	}

	private void setValues(Element elemRoot) throws Exception
	{
		if (!elemRoot.getTagName().equals("Balance") ||
			!elemRoot.getAttribute("version").equals("1.0"))
		{
			throw new Exception("Balance wrong XML format");
		}

		Element elem = (Element) XMLUtil.getFirstChildByName(elemRoot, "AccountNumber");
		String str = XMLUtil.parseValue(elem, (String)null);
		m_lAccountNumber = Long.parseLong(str);

		elem = (Element) XMLUtil.getFirstChildByName(elemRoot, "Deposit");
		str = XMLUtil.parseValue(elem, (String)null);
		m_lDeposit = Long.parseLong(str);

		elem = (Element) XMLUtil.getFirstChildByName(elemRoot, "Spent");
		str = XMLUtil.parseValue(elem, (String)null);
		m_lSpent = Long.parseLong(str);

		elem = (Element) XMLUtil.getFirstChildByName(elemRoot, "BalanceInCent");
		str = XMLUtil.parseValue(elem, "0");
		//m_balance = Math.max(0, Integer.parseInt(str));

		elem = (Element) XMLUtil.getFirstChildByName(elemRoot, "FlatrateEnddate");
		str = XMLUtil.parseValue(elem, DEFAULT_RATE_ENDDATE);
		m_flatEnddate = java.sql.Timestamp.valueOf(str);

		elem = (Element) XMLUtil.getFirstChildByName(elemRoot, "VolumeBytesLeft");
		m_volumeKBytesleft = XMLUtil.parseValue(elem, 0);

		elem = (Element) XMLUtil.getFirstChildByName(elemRoot, "Timestamp");
		str = XMLUtil.parseValue(elem, (String)null);
		if (m_Timestamp != null)
		{
			m_Timestamp = java.sql.Timestamp.valueOf(str);
		}
		else
		{
			m_Timestamp = new Timestamp(System.currentTimeMillis());
		}
		elem = (Element) XMLUtil.getFirstChildByName(elemRoot, "Validtime");
		str = XMLUtil.parseValue(elem, DEFAULT_RATE_ENDDATE);
		m_ValidTime = java.sql.Timestamp.valueOf(str);

	    elem = (Element) XMLUtil.getFirstChildByName(elemRoot, "Message");
		if (elem == null)
		{
			; // no message existsx for this account, that's OK
		}
		else
		{
			boolean isBase64 = XMLUtil.parseAttribute(elem, "encoded",false);
			if (isBase64)
			{
				try
				{
					str = XMLUtil.parseValue(elem, "");
					if (!str.equals("") )
					{
						m_message = Base64.decodeToString(str);
					}
					else
					{
						m_message = "";
					}
				}
				catch (Exception e)
				{
					LogHolder.log(LogLevel.DEBUG, LogType.PAY,
								  "Error while reading message: " + e + ", message (Base64) was" + str +
								  "decoded message was" + m_message);
				}
			}
			else
			{
				m_message = XMLUtil.parseValue(elem,"");
			}
		}
		elem = (Element) XMLUtil.getFirstChildByName(elemRoot, "MessageLink");
		if (elem == null)
		{
			; // no message link exists for this account, that's OK
		}
		else
		{
			str = XMLUtil.parseValue(elem,"");
			if (!str.equals(""))
			{
				try
				{
					m_messageLink = new URL(str);
				} catch (MalformedURLException mue)
				{
					LogHolder.log(LogLevel.DEBUG, LogType.PAY, "Could not get URL from messagelink string: " + str + ", reason: " + mue);
				}
			}
		}
		elem = (Element) XMLUtil.getFirstChildByName(elemRoot, "MessageText");
		if (elem == null)
		{
			; // no message text exists for this account, that's OK
		}
		else
		{
			boolean isBase64 = XMLUtil.parseAttribute(elem, "encoded",false);
			if (isBase64)
			{
				try
				{
					str = XMLUtil.parseValue(elem, "");
					if (!str.equals(""))
					{
						m_messageText = Base64.decodeToString(str);
					} else
					{
						m_messageText = "";
					}
				}
				catch (Exception e)
				{
					LogHolder.log(LogLevel.DEBUG, LogType.PAY,
								  "Error while reading message: " + e + ", message (Base64) was" + str +
								  "decoded message was" + m_message);
				}
			}
			else
			{
				m_messageText = XMLUtil.parseValue(elem,"");
			}
		}

	}

	private Element internal_toXmlElement(Document a_doc)
	{
		Element elemRoot = a_doc.createElement("Balance");
		elemRoot.setAttribute("version", "1.0");

		Element elem = a_doc.createElement("AccountNumber");
		XMLUtil.setValue(elem, m_lAccountNumber);
		elemRoot.appendChild(elem);
		elem = a_doc.createElement("Deposit");
		XMLUtil.setValue(elem, m_lDeposit);
		elemRoot.appendChild(elem);
		elem = a_doc.createElement("Spent");
		XMLUtil.setValue(elem, m_lSpent);
		elemRoot.appendChild(elem);
		//elem = a_doc.createElement("BalanceInCent");
		//XMLUtil.setValue(elem,m_balance);
		//elemRoot.appendChild(elem);
		elem = a_doc.createElement("FlatrateEnddate");
		XMLUtil.setValue(elem, m_flatEnddate.toString() );
		elemRoot.appendChild(elem);
		elem = a_doc.createElement("VolumeBytesLeft");
		XMLUtil.setValue(elem, m_volumeKBytesleft);
		elemRoot.appendChild(elem);
		elem = a_doc.createElement("Timestamp");
		XMLUtil.setValue(elem, m_Timestamp.toString());
		elemRoot.appendChild(elem);
		elem = a_doc.createElement("Validtime");
		XMLUtil.setValue(elem, m_ValidTime.toString());
		elemRoot.appendChild(elem);

		elem = a_doc.createElement("Message");
		if (m_message != null)
		{
			String encodedMessage = Base64.encodeString(m_message);
			XMLUtil.setValue(elem, encodedMessage);
			XMLUtil.setAttribute(elem, "encoded",true);
		}
		elemRoot.appendChild(elem);

		elem = a_doc.createElement("MessageText");
		if (m_messageText != null)
		{
			String encodedMessageText = Base64.encodeString(m_messageText);
			XMLUtil.setAttribute(elem, "encoded", true);
			XMLUtil.setValue(elem, encodedMessageText);
		}
		elemRoot.appendChild(elem);

		elem = a_doc.createElement("MessageLink");
		if (m_messageLink != null)
		{
			XMLUtil.setValue(elem, m_messageLink.toString());
		}
		elemRoot.appendChild(elem);



		return elemRoot;
	}
	
	/**
	 * Returns the number the number of the account to which this balance belongs to
	 * @return number of the account that this balance belongs to
	 */
	public long getAccountNumber()
	{
		return m_lAccountNumber;
	}
	
	/**
	 * Returns the total amount of money spent by the user to buy the 
	 * corresponding volume plan.
	 * @return costs for the volume plan of the corresponding account
	 */
	public long getDeposit()
	{
		return m_lDeposit;
	}
	
	/**
	 * Returns the overall spent bytes for the corresponding account
	 * @return bytes spent overall
	 */
	public long getSpent()
	{
		return m_lSpent;
	}

	/*public int getBalance()
	{
		return m_balance;
	}*/

	/**
	 * getVolumeKBytesLeft: returns the current credit of the user
	 * Implementation depends on the payment system used
	 * formerly returned the difference between cumulative spent and deposit bytes
	 * now returns volume_bytesleft
	 * return value will be compared to jap.pay.PaymentMainPanel WARNING_AMOUNT
	 *
	 * @return long: currently volume_kbytesleft
	 */
	public long getVolumeKBytesLeft()
	{
		return m_volumeKBytesleft;
	}

	public java.sql.Timestamp getFlatEnddate()
	{
		return m_flatEnddate;
	}

	public java.sql.Timestamp getTimestamp()
	{
		return m_Timestamp;
	}
	
	/**
	 * Returns the Date after that unspent byte volume
	 * will expire 
	 * @return the expire date
	 */
	public java.sql.Timestamp getValidTime()
	{
		return m_ValidTime;
	}

	/**
	 *
	 * @return PayMessage: a PayMessage object if this Balance has a message associated with it
	 *  If the short message text is null or an empty String, getMessage() will return null
	 *   (even if a link or long text exists)
	 */
	public PayMessage getMessage()
	{
		if (m_message == null || m_message.equals("") )
		{
			return null;
		}
	    else
		{
			return new PayMessage(m_message, m_messageText, m_messageLink);
		}
	}



	public Element toXmlElement(Document a_doc)
	{
		try
		{
			return (Element) XMLUtil.importNode(a_doc, m_docTheBalance.getDocumentElement(), true);
		}
		catch (Exception e)
		{
			return null;
		}
	}

}
