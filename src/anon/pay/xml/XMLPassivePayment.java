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

import java.util.Enumeration;
import java.util.Hashtable;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import anon.util.IXMLEncodable;
import anon.util.XMLParseException;
import anon.util.XMLUtil;

/**
 * This class is used by JAP to send information necessary to process a
 * passive payment to the payment instance.
 *
 * <PassivePayment version="1.0">
 *    <TransferNumber>123456789</TransferNumber>
 *    <PaymentName>CreditCard</PaymentType>
 *    <Amount>3</Amount>
 *    <Currency>EUR</Currency>
 *    <PaymentData ref="owner">Tobias Bayer</PaymentData>
 *    <PaymentData ref="company">VISA</PaymentData>
 *    <PaymentData ref="number">0987654321</PaymentData>
 *    ...
 * </PassivePayment>
 *
 *
 * @author Tobias Bayer, Elmar Schraml
 */
public class XMLPassivePayment implements IXMLEncodable
{
	public static final String XML_ELEMENT_NAME = "PassivePayment";
	private static final String XML_DOCUMENT_VERSION = "1.0";
	private static final String VERSION = "version";
	//keys for element/attribute name
	private static final String TRANSFER_NUM = "TransferNumber";
	private static final String AMOUNT = "Amount";
	private static final String CURRENCY = "Currency";
	private static final String CHARGED = "Charged";
	private static final String PAYMENT_DATA = "PaymentData";
	private static final String REF = "ref";
	private static final String PAYMENT_NAME = "PaymentName";

	private Hashtable m_paymentData = new Hashtable();
	private long m_transferNumber;
	private String m_currency;
	private long m_centAmount;
	private String m_paymentName;
	private boolean m_charged;

	//keys for payment data - use these for addData() / getPaymentData()
	public static final String KEY_COUPONCODE = "code";
	public static final String KEY_ACCOUNTNUMBER = "accountnumber";
	public static final String KEY_TRANSFERNUMBER = "transfernumber";
	public static final String KEY_VOLUMEPLAN = "volumeplan";
	public static final String KEY_MERCHANT_ID = "merchant_id"; //Paysafecard-specific merchant id
	public static final String KEY_TRANSACTION_ID = "transaction_id"; //Paysafecard-specific, NOT equal to the AN.ON- transfer number

	/**
	 * Constructor
	 */
	public XMLPassivePayment()
	{}

	/**
	 * Constructor. Creates object from an XML string.
	 * @param xml String
	 * @throws Exception
	 */
	public XMLPassivePayment(String xml) throws XMLParseException
	{
		setValues(XMLUtil.toXMLDocument(xml).getDocumentElement());
	}
	
	public XMLPassivePayment(char[] xml) throws XMLParseException
	{
		this(new String(xml));
	}
	
	/**
	 * Constructor. Creates object from an XML bytearray .
	 * @param xml byte[]
	 * @throws Exception
	 */
	public XMLPassivePayment(byte[] xml) throws XMLParseException
	{
		setValues(XMLUtil.toXMLDocument(xml).getDocumentElement());
	}

	/**
	 * Constructor. Creates object from an XML document.
	 * @param doc Document
	 * @throws Exception
	 */
	public XMLPassivePayment(Document doc) throws XMLParseException
	{
		setValues(doc.getDocumentElement());
	}

	/**
	 * Constructor. Creates object from an XML element.
	 * @param element Element
	 * @throws Exception
	 */
	public XMLPassivePayment(Element element) throws XMLParseException
	{
		setValues(element);
	}

	/**
	 * Sets the member values from an XML element.
	 * @param elemRoot Element
	 * @throws Exception
	 */
	private void setValues(Element elemRoot) throws XMLParseException
	{
		String ref;
		String value;

		if (!elemRoot.getTagName().equals(XML_ELEMENT_NAME) ||
			!elemRoot.getAttribute(VERSION).equals(XML_DOCUMENT_VERSION))
		{
			throw new XMLParseException("PassivePayment wrong format or wrong version number");
		}

		m_paymentData = new Hashtable();

		NodeList nodesData = elemRoot.getElementsByTagName(PAYMENT_DATA);
		for (int i = 0; i < nodesData.getLength(); i++)
		{
			ref = XMLUtil.parseAttribute(nodesData.item(i), REF, null);
			value = XMLUtil.parseValue(nodesData.item(i), null);
			m_paymentData.put(ref, value);
		}

		m_transferNumber = XMLUtil.parseValue(XMLUtil.getFirstChildByName(elemRoot, TRANSFER_NUM), (long) 0);
		m_centAmount = XMLUtil.parseValue(XMLUtil.getFirstChildByName(elemRoot, AMOUNT), (long) 0);
		m_currency = XMLUtil.parseValue(XMLUtil.getFirstChildByName(elemRoot, CURRENCY), null);
		m_paymentName = XMLUtil.parseValue(XMLUtil.getFirstChildByName(elemRoot, PAYMENT_NAME), null);
		m_charged = XMLUtil.parseValue(XMLUtil.getFirstChildByName(elemRoot, CHARGED), false);

	}

	/**
	 * Sets the payment method name
	 * @param a_paymentName String
	 */
	public void setPaymentName(String a_paymentName)
	{
		m_paymentName = a_paymentName;
	}

	/**
	 * Gets the payment method name
	 * @return String
	 */
	public String getPaymentName()
	{
		return m_paymentName;
	}

	/**
	 * Sets the payment amount
	 * @param a_amount long: money amount in cents
	 */
	public void setAmount(long a_amount)
	{
		m_centAmount = a_amount;
	}

	/**
	 * Sets the currency the user wants to pay in
	 * @param a_currency String
	 */
	public void setCurrency(String a_currency)
	{
		m_currency = a_currency;
	}

	public void setCharged(boolean charged)
	{
		m_charged = charged;
	}

	/**
	 * Sets the transfer number that belongs to this message
	 * @param a_transferNumber long
	 */
	public void setTransferNumber(long a_transferNumber)
	{
		m_transferNumber = a_transferNumber;
	}

	/**
	 * Adds a <PaymentData> element
	 * @param a_reference String The reference, of the input field
	 * @param a_data String The value teh user has entered in the input field
	 */
	public void addData(String a_reference, String a_data)
	{
		m_paymentData.put(a_reference, a_data);
	}

	/**
	 * Gets the payment amount
	 * @return double
	 */
	public long getAmount()
	{
		return m_centAmount;
	}

	/**
	 * Gets the transfer number that belongs to this message
	 * @return long
	 */
	public long getTransferNumber()
	{
		return m_transferNumber;
	}

	/**
	 * Gets the currency the user wants to pay in
	 * @return String
	 */
	public String getCurrency()
	{
		return m_currency;
	}

	public boolean isCharged()
	{
		return m_charged;
	}

	/**
	 * Gets all references that belong to this message
	 * @return Enumeration
	 */
	public Enumeration getReferences()
	{
		return m_paymentData.keys();
	}

	/**
	 * Gets the value of a <PaymentData> line
	 * @param a_key String The reference of the input field that should be retrieved
	 * @return String, or null if key was not found
	 */
	public String getPaymentData(String a_key)
	{
		return (String) m_paymentData.get(a_key);
	}

	/**
	 * Returns a string with all payment data fields.
	 * @return String
	 */
	public String getAllPaymentData()
	{
		String data = "";
		String key;
		Enumeration e = m_paymentData.keys();
		while (e.hasMoreElements())
		{
			key = (String) e.nextElement();
			data += key + " = " + (String) m_paymentData.get(key);
			if (e.hasMoreElements())
			{
				data += "\n";
			}
		}
		return data;
	}

	public Enumeration getPaymentDataKeys()
	{
		return m_paymentData.keys();
	}

	/**
	 * Produces an XML element from the member values
	 * @param a_doc Document
	 * @return Element
	 */
	public Element toXmlElement(Document a_doc)
	{
		String ref;
		Element elemRoot = a_doc.createElement(XML_ELEMENT_NAME);
		elemRoot.setAttribute(VERSION, XML_DOCUMENT_VERSION);

		Element elem;
		elem = a_doc.createElement(TRANSFER_NUM);
		XMLUtil.setValue(elem, m_transferNumber);
		elemRoot.appendChild(elem);

		elem = a_doc.createElement(PAYMENT_NAME);
		XMLUtil.setValue(elem, m_paymentName);
		elemRoot.appendChild(elem);

		elem = a_doc.createElement(AMOUNT);
		XMLUtil.setValue(elem, String.valueOf(m_centAmount));
		elemRoot.appendChild(elem);

		elem = a_doc.createElement(CURRENCY);
		XMLUtil.setValue(elem, m_currency);
		elemRoot.appendChild(elem);

		elem = a_doc.createElement(CHARGED);
		XMLUtil.setValue(elem, m_charged);
		elemRoot.appendChild(elem);

		Enumeration refs = m_paymentData.keys();
		while (refs.hasMoreElements())
		{
			ref = (String) refs.nextElement();
			elem = a_doc.createElement(PAYMENT_DATA);
			XMLUtil.setAttribute(elem, REF, ref);
			XMLUtil.setValue(elem, (String) m_paymentData.get(ref));
			elemRoot.appendChild(elem);
		}
		return elemRoot;
	}

}
