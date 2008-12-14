/*
 Copyright (c) 2000 - 2007, The JAP-Team
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
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import anon.util.IXMLEncodable;
import anon.util.XMLUtil;

/**
 * XML representation of the data associated with a particular volume plan,
 * similar to a payment option
 *
 * Example:
 *
 * <VolumePlan>
 *    <Name>Small</Name>
 *    <Price>200</Price>   <!-- in eurocent -->
 *    <VolumeLimited>True</VolumeLimited>
 *    <VolumeKbytes>2000000</VolumeKbytes>
 *    <DurationLimited>True</DurationLimited>
 *    <Duration unit="months">1</Duration>  <!-- possible values: days, month(s), week(s)
 * </VolumePlan>
 *
 * @author Elmar Schraml
 */
public class XMLVolumePlan implements IXMLEncodable
{
	public static final String XML_ELEMENT_NAME = "VolumePlan";

	private Document m_docTheVolumePlan;
	private String m_name; //the internal, unique name of the plan, e.g. "Small_jan07" - prior to JAP 00.08.107 (before the introduction of displayname, also the one to be displayed
	private String m_displayName; //the name to be shown to the customer, e.g. "Small"
	private int m_price;
	private boolean m_volumeLimited;
	private boolean m_durationLimited;
	private long m_volumeKbytes;
	private int m_duration;
	private String m_durationUnit;

	public XMLVolumePlan()
	{
	}

	public XMLVolumePlan(String xml) throws Exception
	{
		ByteArrayInputStream in = new ByteArrayInputStream(xml.getBytes());
		Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(in);
		setValues(doc.getDocumentElement());
		m_docTheVolumePlan = doc;
	}

	public XMLVolumePlan(Element elemVolumePlan) throws Exception
	{
		setValues(elemVolumePlan);
		m_docTheVolumePlan = XMLUtil.createDocument();
		m_docTheVolumePlan.appendChild(XMLUtil.importNode(m_docTheVolumePlan, elemVolumePlan, true));
	}

	public XMLVolumePlan(Document doc) throws Exception
	{
		setValues(doc.getDocumentElement());
		m_docTheVolumePlan = doc;
	}

	/**
	 * duration and volume will both be unlimited
	 */
	public XMLVolumePlan(String a_name, String a_displayName, int a_price)
	{
		m_name = a_name;
		m_price = a_price;
		m_displayName = a_displayName;
		m_docTheVolumePlan = XMLUtil.createDocument();
		m_docTheVolumePlan.appendChild(internal_toXmlElement(m_docTheVolumePlan));
	}

	/**
	 * duration will be unlimited, volumeLimited is automatically set to true
	 */
	public XMLVolumePlan(String a_name, String a_displayName, int a_price, long a_volumeLimit)
	{
		m_name = a_name;
		m_displayName = a_displayName;
		m_price = a_price;
		m_volumeKbytes = a_volumeLimit;
		m_volumeLimited = true;
		m_durationLimited = false;
		m_docTheVolumePlan = XMLUtil.createDocument();
		m_docTheVolumePlan.appendChild(internal_toXmlElement(m_docTheVolumePlan));
}

	/**
	 * volume will be unlimited, durationLimited is automatically set to true
	 */
	public XMLVolumePlan(String a_name, String a_displayName, int a_price, int a_duration, String a_durationUnit)
	{
		m_name = a_name;
		m_displayName = a_displayName;
		m_price = a_price;
		m_volumeLimited = false;
		m_durationLimited = true;
		m_duration = a_duration;
		m_durationUnit = a_durationUnit;
		m_docTheVolumePlan = XMLUtil.createDocument();
		m_docTheVolumePlan.appendChild(internal_toXmlElement(m_docTheVolumePlan));
	}

	/**
	 * regular volume plan: both volume and duration are limited
	 */
	public XMLVolumePlan(String a_name, String a_displayName, int a_price, int a_duration, String a_durationUnit, long a_volumeLimit)
	{
		m_name = a_name;
		m_displayName = a_displayName;
		m_price = a_price;
		m_durationLimited = true;
		m_duration = a_duration;
		m_durationUnit = a_durationUnit;
		m_volumeKbytes = a_volumeLimit;
		m_volumeLimited = true;
		m_docTheVolumePlan = XMLUtil.createDocument();
		m_docTheVolumePlan.appendChild(internal_toXmlElement(m_docTheVolumePlan));
	}

	/**
	 * full set of parameters, as stored in database
	 */
	public XMLVolumePlan(String a_name, String a_displayName, int a_price,boolean a_durationLimited, boolean a_volumeLimited, int a_duration, String a_durationUnit,
						 long a_volumeLimit)
	{
		m_name = a_name;
		m_displayName = a_displayName;
		m_price = a_price;
		m_durationLimited = a_durationLimited;
		m_duration = a_duration;
		m_durationUnit = a_durationUnit;
		m_volumeKbytes = a_volumeLimit;
		m_volumeLimited = a_volumeLimited;
		m_docTheVolumePlan = XMLUtil.createDocument();
		m_docTheVolumePlan.appendChild(internal_toXmlElement(m_docTheVolumePlan));
	}


	public String getName()
	{
		return m_name ;
    }

	public String getDisplayName()
	{
		if (m_displayName != null && !m_displayName.equals("") )
		{
			return m_displayName;
		}
		else
		{
			return m_name; //just in case no extra display name is set, should not happen
		}
	}

	public int getPrice()
	{
		return m_price ;
	}

	public boolean isVolumeLimited()
	{
		return m_volumeLimited ;
	}

	public boolean isDurationLimited()
	{
		return m_durationLimited ;
	}

	public int getDuration()
	{
		return m_duration ;
	}

	public String getDurationUnit()
	{
		return m_durationUnit ;
	}

	public int getDurationInDays()
	{
		//TODO, just a utility method
		return 0;
	}

	public long getVolumeKbytes()
	{
		return m_volumeKbytes ;
	}



	/**
	 * Return an element that can be appended to the document.
	 *
	 * @param a_doc a document
	 * @return the interface as xml element
	 * @todo Implement this anon.util.IXMLEncodable method
	 */
	public Element toXmlElement(Document a_doc)
	{
		try
		{
			return (Element) XMLUtil.importNode(a_doc, m_docTheVolumePlan.getDocumentElement(), true);
		}
		catch (Exception e)
		{
			return null;
		}
	}

	protected void setValues(Element elemRoot) throws Exception
	{
		if (!elemRoot.getTagName().equals("VolumePlan"))
		{
			throw new Exception("XMLVolumePlan: wrong XML structure");
		}

		Element elem;
		String str;

		elem = (Element) XMLUtil.getFirstChildByName(elemRoot, "Name");
		m_name = XMLUtil.parseValue(elem, (String)null);

		elem = (Element) XMLUtil.getFirstChildByName(elemRoot, "DisplayName");
		m_displayName = XMLUtil.parseValue(elem, (String)null);

		elem = (Element) XMLUtil.getFirstChildByName(elemRoot, "Price");
		str = XMLUtil.parseValue(elem, (String)null);
		m_price = Integer.parseInt(str);

		elem = (Element) XMLUtil.getFirstChildByName(elemRoot, "VolumeLimited");
		m_volumeLimited = XMLUtil.parseValue(elem, false);

		elem = (Element) XMLUtil.getFirstChildByName(elemRoot, "DurationLimited");
		m_durationLimited = XMLUtil.parseValue(elem, false);

		elem = (Element) XMLUtil.getFirstChildByName(elemRoot, "VolumeKbytes");
		str = XMLUtil.parseValue(elem, (String)null);
		m_volumeKbytes = Long.parseLong(str);

		elem = (Element) XMLUtil.getFirstChildByName(elemRoot, "Duration");
		str = XMLUtil.parseValue(elem, (String)null);
		m_duration = Integer.parseInt(str);
		m_durationUnit = XMLUtil.parseAttribute(elem,"unit","");

	}

	private Element internal_toXmlElement(Document a_doc)
	{
		Element elemRoot = a_doc.createElement("VolumePlan");
		Element elem;

		elem = a_doc.createElement("Name");
		XMLUtil.setValue(elem, m_name);
		elemRoot.appendChild(elem);

		elem = a_doc.createElement("DisplayName");
		XMLUtil.setValue(elem, m_displayName);
		elemRoot.appendChild(elem);

		elem = a_doc.createElement("Price");
		XMLUtil.setValue(elem, m_price);
		elemRoot.appendChild(elem);

		elem = a_doc.createElement("DurationLimited");
		XMLUtil.setValue(elem, m_durationLimited);
		elemRoot.appendChild(elem);

		elem = a_doc.createElement("VolumeLimited");
		XMLUtil.setValue(elem, m_volumeLimited);
		elemRoot.appendChild(elem);

		elem = a_doc.createElement("VolumeKbytes");
		XMLUtil.setValue(elem, m_volumeKbytes);
		elemRoot.appendChild(elem);

		elem = a_doc.createElement("Duration");
		XMLUtil.setValue(elem, m_duration);
		elem.setAttribute("unit",m_durationUnit);
		elemRoot.appendChild(elem);

		return elemRoot;
	}
}
