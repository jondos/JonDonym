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
package anon.pay.xml;

import java.io.ByteArrayInputStream;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;
import java.sql.Timestamp;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import anon.util.IXMLEncodable;
import anon.util.XMLUtil;
import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;

/**
 * This class is used by JAP to ask the Payment Instance about
 * used transaction numbers. JAP has to send the structure without the
 * attributed "used" to the Payment Instance.
 * The PI will then fill in the attributes according to its database entries.
 *
 * <TransactionOverview version="1.0">
 *    <TransferNumber used="false">232343455</TransferNumber>
 *    <TransferNumber used="true" date="234358734908" amount="13242424" accountnumber="123456789">435675747</TransferNumber>
 * </TransactionOverview>
 *
 *
 * @author Tobias Bayer, Elmar Schraml
 * @todo: Elmar: refactored to use Hashtable instead of String[], but still pretty awfully ugly - should be scrapped in favor of a Vector of XMLTransCert (cf. XMLPaymentOptions)
 */
public class XMLTransactionOverview implements IXMLEncodable
{
	public static final Object XML_ELEMENT_NAME = "TransactionOverview";

	private Vector m_transactions = new Vector(); //Vector<Hashtable> with keys as defined in the string constants below
	private String m_language; //language of the JAP that requests the Transaction overview (since some data, e.g. payment method name, need to be localized)

	//data for a single transaction is stored in a hashtable, define which keys exist here
	public static final String KEY_ACCOUNTNUMBER = "accountnumber";
	public static final String KEY_TAN = "tan";
	public static final String KEY_DATE = "date";
	public static final String KEY_CREATIONDATE = "created_on";
	public static final String KEY_AMOUNT = "amount";
	public static final String KEY_VOLUMEPLAN = "volumeplan";
	public static final String KEY_PAYMENTMETHOD = "paymentmethod";
	public static final String KEY_USED = "used";

	public XMLTransactionOverview(String a_language)
	{
		m_language = a_language;
	}
	
	public XMLTransactionOverview(char[] xml) throws Exception
	{
		Document doc = XMLUtil.toXMLDocument(xml);
		setValues(doc.getDocumentElement());
	}
	
	public XMLTransactionOverview(byte[] xml) throws Exception
	{
		Document doc = XMLUtil.toXMLDocument(xml);
		setValues(doc.getDocumentElement());
	}

	public XMLTransactionOverview(Document doc) throws Exception
	{
		setValues(doc.getDocumentElement());
	}

	public XMLTransactionOverview(Element element) throws Exception
	{
		setValues(element);
	}

	public int size()
	{
		return m_transactions.size();
	}

	private void setValues(Element elemRoot) throws Exception
	{
		m_transactions = new Vector();
		if (!elemRoot.getTagName().equals(XML_ELEMENT_NAME) ||
			!elemRoot.getAttribute("version").equals("1.1"))
		{
			throw new Exception("TransactionOverview wrong format or wrong version number");
		}

	    m_language = elemRoot.getAttribute("language");

		NodeList nodesTans = elemRoot.getElementsByTagName("TransferNumber");

		for (int i = 0; i < nodesTans.getLength(); i++)
		{
			Hashtable transactionData = new Hashtable();

			Element curTanElem = (Element) nodesTans.item(i);
			String tan = curTanElem.getFirstChild().getNodeValue();
			tan = XMLUtil.parseValue(curTanElem,"");

			transactionData.put(KEY_TAN,tan);

			String used;
			if (  curTanElem.getAttribute("used") != null)
			{
				used = curTanElem.getAttribute("used");
			}
			else
			{
				used = "false";
			}
			transactionData.put(KEY_USED,used);

			String creationDate;
			if ( curTanElem.getAttribute(KEY_CREATIONDATE) != null)
			{
				creationDate = curTanElem.getAttribute(KEY_CREATIONDATE);
			}
			else
			{
				creationDate = "0";
			}
			transactionData.put(KEY_CREATIONDATE, creationDate);

			String date;
			if ( curTanElem.getAttribute("date") != null)
			{
				date = curTanElem.getAttribute("date");
			}
			else
			{
				date = "0";
			}
			transactionData.put(KEY_DATE, date);


			String amount;
			if ( curTanElem.getAttribute("amount") != null)
			{
				amount = curTanElem.getAttribute("amount");
			}
			else
			{
				amount = "0";
			}
			transactionData.put(KEY_AMOUNT,amount);

			String accountnumber;
			if ( curTanElem.getAttribute("accountnumber") != null)
				{
					accountnumber = curTanElem.getAttribute("accountnumber");
				}
				else
				{
					accountnumber = "";
			}
			transactionData.put(KEY_ACCOUNTNUMBER, accountnumber);

			String volumeplan;
			if ( curTanElem.getAttribute("volumeplan") != null)
			{
				volumeplan = curTanElem.getAttribute("volumeplan");
			}
			else
			{
				volumeplan = "";
			}
			transactionData.put(KEY_VOLUMEPLAN, volumeplan);

			String paymentmethod;
			if ( curTanElem.getAttribute("paymentmethod") != null)
			{
				paymentmethod = curTanElem.getAttribute("paymentmethod");
			}
			else
			{
				paymentmethod = "";
			}
			transactionData.put(KEY_PAYMENTMETHOD, paymentmethod);

			m_transactions.addElement(transactionData);
		}

	}

	public Element toXmlElement(Document a_doc)
	{
		Element elemRoot = a_doc.createElement("TransactionOverview");
		elemRoot.setAttribute("version", "1.1");
		elemRoot.setAttribute("language",m_language);

		Element elem;
		Enumeration tans = m_transactions.elements();
		while (tans.hasMoreElements())
		{
			Hashtable curTransaction = (Hashtable) tans.nextElement();
			elem = a_doc.createElement("TransferNumber");

			String creationDate = (String) curTransaction.get(KEY_CREATIONDATE);
			creationDate = (creationDate == null)?"":creationDate;
			elem.setAttribute(KEY_CREATIONDATE, creationDate);

			String accountNumber = (String) curTransaction.get(KEY_ACCOUNTNUMBER);
			accountNumber = (accountNumber == null)?"":accountNumber;
			elem.setAttribute("accountnumber",accountNumber);

			String date = (String) curTransaction.get(KEY_DATE);
			date = (date == null)?"":date;
			elem.setAttribute("date", date);

			String amount = (String) curTransaction.get(KEY_AMOUNT);
			amount = (amount == null)?"": amount;
			elem.setAttribute("amount", amount);

			String volumePlan = (String) curTransaction.get(KEY_VOLUMEPLAN);
			volumePlan = (volumePlan == null)?"":volumePlan;
			elem.setAttribute("volumeplan",volumePlan);

			String paymentMethod = (String) curTransaction.get(KEY_PAYMENTMETHOD);
			paymentMethod = (paymentMethod==null)?"":paymentMethod;
			elem.setAttribute("paymentmethod",paymentMethod);

			String used = (String) curTransaction.get(KEY_USED);
			used = (used == null)?"":used;
			elem.setAttribute("used", used);

			String transferNumber = (String) curTransaction.get(KEY_TAN);
			elem.appendChild(a_doc.createTextNode(transferNumber));

			elemRoot.appendChild(elem);
		}
		return elemRoot;
	}

	/**
	 * Gets an vector of all transfer numbers
	 * @return Vector
	 */
	public Vector getTans()
	{
		return m_transactions;
	}

	public String getLanguage()
	{
		return m_language;
	}

	/**
	 * Returns if a specific transfer number is marked as "used".
	 * @param a_tan long
	 * @return boolean
	 */
	public boolean isUsed(long a_tan)
	{
		boolean used = false;
		Hashtable theTransaction  = getDataForTransaction(a_tan);
        if (theTransaction != null) //nothing to be done if no transaction found,will return false
		{
			String usedString = (String) theTransaction.get(KEY_USED);
			used = Boolean.valueOf(usedString).booleanValue();
		}
		return used;
	}

	/**
	 *
	 * @param a_transactionNumber long
	 * @return Hashtable containing all data associated with that transaction, null if no matching tan was found
	 */
	public Hashtable getDataForTransaction(long a_transactionNumber)
	{
		Hashtable matchingTransaction = null;
		for (Enumeration allTans = m_transactions.elements(); allTans.hasMoreElements(); )
		{
			Hashtable transactionData = (Hashtable) allTans.nextElement();
			String tan = (String) transactionData.get(KEY_TAN);
			try
			{
				long curTan = Long.parseLong(tan);
				if (curTan == a_transactionNumber)
				{
					matchingTransaction = transactionData;
					break;
				}
			}
			catch (NumberFormatException a_e)
			{
				LogHolder.log(LogLevel.ERR, LogType.PAY, a_e);
			}
		}
		return matchingTransaction;
	}

	/**
	 * Sets a specific tan to used or not used, and add all the data associated with the TAN
	 * @param a_tan long
	 * @param a_used boolean
	 * @param a_usedDate long
	 */
	public void setTransactionData(long a_tan, long a_creationDate, boolean a_used, long a_usedDate, long amount, long accountnumber, String volumePlan, String paymentMethod)
	{
	    //prevent null values (might very well happen for older accounts that didn't record that data, etc)
        String strAccountNumber;
		if (accountnumber == 0)
		{
			strAccountNumber = new String("");
		}
		else
		{
			strAccountNumber = (new Long(accountnumber)).toString();
		}
		String strAmount;
		if (amount == 0)
		{
			strAmount = new String("");
		}
		else
		{
			strAmount = (new Long(amount)).toString();
		}

	    String strCreationDate;
		if (a_creationDate == 0)
		{
			strCreationDate = new String("");
		}
		else
		{
			strCreationDate = (new Long(a_creationDate)).toString();
		}

		String strUsedDate;
		if (a_usedDate == 0)
		{
			strUsedDate = new String("");
		}
		else
		{
			strUsedDate = (new Long(a_usedDate)).toString();
		}
	    if (volumePlan == null)
		{
			volumePlan = new String("");
		}
		if (paymentMethod == null)
		{
			paymentMethod = new String("");
		}

		Hashtable affectedTransaction = getDataForTransaction(a_tan);
		affectedTransaction.put(KEY_USED,(new Boolean(a_used)).toString() );
		affectedTransaction.put(KEY_DATE, strUsedDate );
		affectedTransaction.put(KEY_CREATIONDATE, strCreationDate);
		affectedTransaction.put(KEY_ACCOUNTNUMBER, strAccountNumber );
		affectedTransaction.put(KEY_AMOUNT, strAmount );
		affectedTransaction.put(KEY_VOLUMEPLAN, volumePlan);
		affectedTransaction.put(KEY_PAYMENTMETHOD, paymentMethod);
	}

	/**
	 * Adds a transfer number and sets its state to "not used".
	 * @param a_tan long
	 */
	public void addTan(long a_tan)
	{
		Hashtable newTransaction = new Hashtable();
		newTransaction.put(KEY_TAN, (new Long(a_tan)).toString());
		m_transactions.addElement(newTransaction);
	}

}
