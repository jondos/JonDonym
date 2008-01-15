/*
 Copyright (c) 2000-2006, The JAP-Team
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
import java.util.Calendar;
import java.util.Enumeration;
import java.util.Hashtable;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import anon.util.IXMLEncodable;
import anon.util.XMLUtil;

/**
 * XMl representation of the payment setting (flatrate price, duration etc)
 * simple wrapper around the data of table paymentsettings,
 * cotains a Hashtable whose keys and values correspond to
 * the name and curvalue columns of table paymentsettings
 *
 * @author Elmar Schraml
 */
public class XMLPaymentSettings implements IXMLEncodable
{
	private Hashtable m_paymentSettings = new Hashtable();

	private Document m_docTheSettings = null;

	public XMLPaymentSettings(String xml) throws Exception
	{
		ByteArrayInputStream in = new ByteArrayInputStream(xml.getBytes());
		Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(in);
		setValues(doc.getDocumentElement());
	}

	public XMLPaymentSettings()
	{
	}

	public XMLPaymentSettings(Hashtable allSettings)
	{
		m_paymentSettings = allSettings;
		m_docTheSettings = XMLUtil.createDocument();
		internal_toXmlElement(m_docTheSettings);
	}

	public XMLPaymentSettings(Element xml) throws Exception
	{
		setValues(xml);
	}

	public XMLPaymentSettings(Document document) throws Exception
	{
		setValues(document.getDocumentElement());
	}

	public void addSetting(String name, String value)
	{
		m_paymentSettings.put(name, value);
	}

	/**
	 * getSettingValue
	 *
	 * @param name String: name of the settings as used in configfile/ database
	 * @return String value of the settings, or null if it's not set
	 */
	public String getSettingValue(String name)
	{
		return (String) m_paymentSettings.get(name);
	}

	/**
	 * getEndDate: utility method that returns the enddate of a flat purchased now
	 * (i.e. Date() + flat_duration * flat_duration_unit
	 *
	 * @return Calendar: set to the time of expiration of a flatrate bought right now
	 */
	public Calendar getEndDate()
	{
		Calendar now = Calendar.getInstance();
		String unit = this.getSettingValue("FlatrateDurationUnit");
		if (unit.equalsIgnoreCase("day") || unit.equalsIgnoreCase("days"))
		{
			int day = now.get(Calendar.DAY_OF_YEAR);
			int duration = Integer.parseInt(this.getSettingValue("FlatrateDuration"));
			now.set(Calendar.DAY_OF_YEAR, (day + duration) % now.getMaximum(Calendar.DAY_OF_YEAR));
		}
		else if (unit.equalsIgnoreCase("week") || unit.equalsIgnoreCase("weeks"))
		{
			int week = now.get(Calendar.WEEK_OF_YEAR);
			int duration = Integer.parseInt(this.getSettingValue("FlatrateDuration"));
			now.set(Calendar.WEEK_OF_YEAR, (week + duration) % now.getMaximum(Calendar.WEEK_OF_YEAR));
		}
		else if (unit.equalsIgnoreCase("month") || unit.equalsIgnoreCase("months"))
		{
			int month = now.get(Calendar.MONTH);
			int duration = Integer.parseInt(this.getSettingValue("FlatrateDuration"));
			now.set(Calendar.MONTH, (month + duration) % now.getMaximum(Calendar.MONTH));
		}
		else if (unit.equalsIgnoreCase("year") || unit.equalsIgnoreCase("years"))
		{
			int year = now.get(Calendar.YEAR);
			int duration = Integer.parseInt(this.getSettingValue("FlatrateDuration"));
			now.set(Calendar.YEAR, year + duration);
		}
		return now;
	}

	public Enumeration getSettingNames()
	{
		return m_paymentSettings.keys();
	}

	private Element internal_toXmlElement(Document a_doc)
	{
		Element elemRoot = a_doc.createElement("PaymentSettings");
		elemRoot.setAttribute("version", "1.0");
		a_doc.appendChild(elemRoot);

		Element elem;
		String name;
		String value;
		for (Enumeration settingNames = m_paymentSettings.keys(); settingNames.hasMoreElements(); )
		{
			name = (String) settingNames.nextElement();
			value = (String) m_paymentSettings.get(name);
			elem = a_doc.createElement("Setting");
			XMLUtil.setAttribute(elem, "name", name);
			XMLUtil.setValue(elem, value);
			elemRoot.appendChild(elem);
		}

		return elemRoot;
	}

	private void setValues(Element elemRoot) throws Exception
	{
		if (!elemRoot.getTagName().equals("PaymentSettings") ||
			!elemRoot.getAttribute("version").equals("1.0"))
		{
			throw new Exception("wrong XML format");
		}
		String name;
		String value;
		Element curElem;
		//get array of Settings elements
		NodeList settingElements = elemRoot.getElementsByTagName("Setting");

		//loop
		for (int i = 0; i < settingElements.getLength(); i++)
		{
			curElem = (Element) settingElements.item(i);
			value = XMLUtil.parseValue(curElem, null);
			name = XMLUtil.parseAttribute(curElem, "name", null);
			m_paymentSettings.put(name, value);
		}
	}

	public Element toXmlElement(Document a_doc)
	{
		try
		{
			return (Element) XMLUtil.importNode(a_doc, m_docTheSettings.getDocumentElement(), true);
		}
		catch (Exception e)
		{
			return null;
		}
	}

}
