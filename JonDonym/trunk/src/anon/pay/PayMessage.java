package anon.pay;

import java.net.URL;
import java.net.*;
import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;

/**
 * Data class for holding a message
 * that is set for an account via the PIG,
 * stored in the JPI database, and shown by the JAP as a status message
 *
 * Sent as a member variable in an XMLBalance
 * (Though this class could easily be retrofit to implement IXMLEncodable if necessary)
 *
 *
 * @author Elmar Schraml
 */
public class PayMessage
{
	private String m_shortMessage = null;
	private String m_messageText = null;
	private URL m_messageLink = null;

	/**
	 * builds a Message that only consists of a text that is short enough to be fully shown in one line,
	 * messageText and messageLink will be null
	 *
	 * @param a_shortMessage String: it probably doesn't make sense to build a Message object with a shortMessage of null,
	 *                               but if you need to you can
	 */
	public PayMessage(String a_shortMessage)
	{
		m_shortMessage = a_shortMessage;
	}

	/**
	 * @param a_shortMessage String: message that is short enough to be shown in one line. Can be null, but probably shouldn't be.
	 * @param a_messageText String: longer message text that will be displayed in its own dialog, but only upon click. Can be null.
	 * @param a_messageLink String: a link associated with the message (i.e. message "Contact us" -> click -> open email. Can be null.
	 */
	public PayMessage(String a_shortMessage, String a_messageText, URL a_messageLink)
	{
		m_shortMessage = a_shortMessage;
		m_messageText = a_messageText;
		m_messageLink = a_messageLink;
	}

	/**
	 *  PayMessages are equal if shortMesage, messageText and messagelink are all equal
	 *  (e.g. change one character in the URL -> considered a different message)
	 *
	 *  the messageLinks are compared as Strings, not URLs, so different hostnames pointing to the same IP will be considered UNequal
	 */
	public boolean equals(Object anotherObject)
	{
		if (! (anotherObject instanceof PayMessage) )
		{
			return false;
		}
		PayMessage otherMessage = (PayMessage) anotherObject;
		boolean shortMessageEqual = m_shortMessage.equals(otherMessage.getShortMessage());
		boolean messageTextEqual = m_messageText.equals(otherMessage.getMessageText());
		//compare Strings of URLs instead of URL objects, to avoid unnecessary hostname lookup
		String thisLink = m_messageLink.toString();
		String otherLink = otherMessage.toString();
		boolean linkEqual = thisLink.equalsIgnoreCase(otherLink);

		if (shortMessageEqual && messageTextEqual && linkEqual)
		{
			return true;
		}
		else
		{
			return false;
		}

	}

	public void setShortMessage(String a_shortMessage)
	{
		this.m_shortMessage = a_shortMessage;
	}

	public String getShortMessage()
	{
		return m_shortMessage;
	}

	public void setMessageText(String a_messageText)
	{
		this.m_messageText = a_messageText;
	}

	public String getMessageText()
	{
		return m_messageText;
	}

	/**
	 * sets the internal URL from a string supplied
	 * If a_messageLink is not a valid link, messageLink will be set to null
	 * (If you want to deal with the exception yourself, you'd just do a new URL(String) yourself
	 *  and call setMessageLink(URL) )
	 * @param a_messageLink String
	 */
	public void setMessageLink(String a_messageLink)
	{
		try
		{
			this.m_messageLink = new URL(a_messageLink);
		}
		catch (MalformedURLException ex)
		{
			LogHolder.log(LogLevel.DEBUG, LogType.PAY,"Could not get valid URL from the given String, messageLink will be null");
		}
	}

	public void setMessageLink(URL a_messageLink)
	{
		this.m_messageLink = a_messageLink;
	}

	public URL getMessageLink()
	{
		return m_messageLink;
	}
}
