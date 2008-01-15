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

import java.util.Enumeration;
import java.util.Vector;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import anon.util.IXMLEncodable;
import anon.util.Util;
import anon.util.XMLUtil;
import java.util.Hashtable;


/**
 * This class represents a payment option sent by the Payment Instance.
 * @author Tobias Bayer, Elmar Schraml
 */
public class XMLPaymentOption implements IXMLEncodable
{
	public static final int MAX_CLICKS_UNLIMITED = Integer.MAX_VALUE;

	public static final String OPTION_ACTIVE = "active";
	public static final String OPTION_PASSIVE = "passive";
	public static final String OPTION_MIXED = "mixed";

	public static final String EXTRA_TEXT = "text";
	public static final String EXTRA_LINK = "link";
	public static final String EXTRA_PHONE = "phone";

	private static final String XML_ATTR_MAXCLICKS = "maxclicks";
	private static final String EXCEPTION_WRONG_XML_STRUCTURE = "XMLPaymentOption wrong XML structure";

	//stores short code Strings for alle languages for which at least one entry has been set
	private static Vector m_languages = new Vector();

	/** Option name */
	private String m_name;

	/** Option type (active|passive)*/
	private String m_type;

	private int m_maxClicks = MAX_CLICKS_UNLIMITED;

    /** percentage of a user's payment that we have to pay to the payment option's provider*/
	private int m_markup;

	/** Generic option? should be true, unless you plan to have JAP provide a special input form for this option*/
	private boolean m_generic;

	/** This vector takes String[2] arrays while the first element is the heading
	 * and the second element is the language identifier. E.g.: {"Money Transfer", "en"}
	 */
	private Vector m_headings = new Vector();

	/** Same explanation as m_headings*/
	private Vector m_detailedInfos = new Vector();

	/**
	 * key: language, value: rank as Integer > 0, lower number for rank means higher priority
	 */
	private Hashtable m_ranks = new Hashtable();

	/** Content: String[2], [0] is a message telling the user about the delay until his account is credited after payment,
	 * [1] the language identifier, e.g. {"2-3 business days", "en"}
	 */
	private Vector m_paymentDelays = new Vector();

	/** This vector takes String[3] arrays. First element: Extra payment info like account number.
	 * Second element: type. Third element: Language.*/
	private Vector m_extraInfos = new Vector();

	/**
	 * This vector takes input fields. First element: reference, second element: label, third element: language
	 */
	private Vector m_inputFields = new Vector();

	/** A link to an image */
	private String m_imageLink;

	/** JAP version since when the option does work (for non-generic options) */
	private String m_minJapVersion;

	public XMLPaymentOption(String xml) throws Exception
	{
        Document doc = XMLUtil.toXMLDocument(xml);
		setValues(doc.getDocumentElement());
	}

	public XMLPaymentOption()
	{
	}

	public XMLPaymentOption(String a_name, String a_type, boolean a_generic)
	{
		m_name = a_name;
		m_type = a_type;
		m_generic = a_generic;
	}

	public XMLPaymentOption(String a_name, String a_type, boolean a_generic, String a_japVersion)
	{
		m_name = a_name;
		m_type = a_type;
		m_generic = a_generic;
		m_minJapVersion = a_japVersion;
	}

	public XMLPaymentOption(String a_name, String a_type, boolean a_generic, String a_japVersion, int a_markup, int a_maxClicks)
	{
		m_name = a_name;
		m_type = a_type;
		m_generic = a_generic;
		m_minJapVersion = a_japVersion;
		m_markup = a_markup;
		if (a_maxClicks < 0)
		{
			a_maxClicks = 0;
		}
		m_maxClicks = a_maxClicks;
	}


	public XMLPaymentOption(String a_name, String a_type)
	{
		m_name = a_name;
		m_type = a_type;
		m_generic = true;
	}

	public void addHeading(String a_heading, String a_language)
	{
		m_headings.addElement(new String[]
							  {a_heading, a_language});
		addLanguage(a_language);
	}

	public void addDetailedInfo(String a_info, String a_language)
	{
		m_detailedInfos.addElement(new String[]
								   {a_info, a_language});
		addLanguage(a_language);
	}

	public void addRank(int a_rank, String a_language)
	{
		m_ranks.put(a_language, new Integer(a_rank));
		//NO addLanguage here, since a rank is not necesary for localization in a language, can always default to the rank for english, or none at all
	}

	public void addPaymentDelay(String a_delayString, String a_language)
	{
		m_paymentDelays.addElement(new String[] {a_delayString, a_language} );
		addLanguage(a_language);
	}

	public void addExtraInfo(String a_info, String a_type, String a_language)
	{
		m_extraInfos.addElement(new String[]
								{a_info, a_type, a_language});
		addLanguage(a_language);
	}

	public void addInputField(String a_reference, String a_label, String a_language)
	{
		m_inputFields.addElement(new String[]
								 {a_reference, a_label, a_language});
		addLanguage(a_language);
	}

	public void setImageLink(String a_link)
	{
		m_imageLink = a_link;
	}

	public XMLPaymentOption(Element xml) throws Exception
	{
		setValues(xml);
	}

	public XMLPaymentOption(Document document) throws Exception
	{
		setValues(document.getDocumentElement());
	}

	public Element toXmlElement(Document a_doc)
	{
		Element elem;
		Element elemRoot = a_doc.createElement("PaymentOption");
		elemRoot.setAttribute("name", m_name);
		elemRoot.setAttribute("type", m_type);
		elemRoot.setAttribute("generic", String.valueOf(m_generic));
		elemRoot.setAttribute("japversion", m_minJapVersion);
		XMLUtil.setAttribute(elemRoot, XML_ATTR_MAXCLICKS, m_maxClicks);

		elem = a_doc.createElement("Markup");
		XMLUtil.setValue(elem,m_markup);
		elemRoot.appendChild(elem);


		//Add headings
		for (int i = 0; i < m_headings.size(); i++)
		{
			String[] heading = (String[]) m_headings.elementAt(i);
			elem = a_doc.createElement("Heading");
			elem.setAttribute("lang", heading[1]);
			elem.appendChild(a_doc.createTextNode(heading[0]));
			elemRoot.appendChild(elem);
		}
		//Add detailed information
		for (int i = 0; i < m_detailedInfos.size(); i++)
		{
			String[] detailed = (String[]) m_detailedInfos.elementAt(i);
			elem = a_doc.createElement("DetailedInfo");
			elem.setAttribute("lang", detailed[1]);
			elem.appendChild(a_doc.createTextNode(detailed[0]));
			elemRoot.appendChild(elem);
		}
		//Add ranks
		Enumeration allRankLanguages = m_ranks.keys();
		while (allRankLanguages.hasMoreElements() )
		{
			String curLang = (String) allRankLanguages.nextElement();
			Integer curRank = (Integer) m_ranks.get(curLang);
			elem = a_doc.createElement("Rank");
			elem.setAttribute("lang",curLang);
			elem.appendChild(a_doc.createTextNode(curRank.toString() ) );
			elemRoot.appendChild(elem);
		}

		//Add payment delay Infos
		for (int i = 0; i < m_paymentDelays.size(); i++)
		{
			String[] delay = (String[]) m_paymentDelays.elementAt(i);
			elem = a_doc.createElement("PaymentDelay");
			elem.setAttribute("lang", delay[1]);
			elem.appendChild(a_doc.createTextNode(delay[0]));
			elemRoot.appendChild(elem);
		}


		//Add extra information
		for (int i = 0; i < m_extraInfos.size(); i++)
		{
			String[] extra = (String[]) m_extraInfos.elementAt(i);
			elem = a_doc.createElement("ExtraInfo");
			elem.setAttribute("type", extra[1]);
			if (extra[2] != null)
			{
				elem.setAttribute("lang", extra[2]);
			}
			elem.appendChild(a_doc.createTextNode(extra[0]));
			elemRoot.appendChild(elem);
		}

		//Add image link
		if (m_imageLink != null)
		{
			elem = a_doc.createElement("ImageLink");
			elem.appendChild(a_doc.createTextNode(m_imageLink));
			elemRoot.appendChild(elem);
		}

		//Add input fields
		for (int i = 0; i < m_inputFields.size(); i++)
		{
			String[] input = (String[]) m_inputFields.elementAt(i);
			elem = a_doc.createElement("input");
			elem.setAttribute("ref", input[0]);
			Element elem2 = a_doc.createElement("label");
			elem.appendChild(elem2);
			if (input[2] != null)
			{
				elem2.setAttribute("lang", input[2]);
			}
			elem2.appendChild(a_doc.createTextNode(input[1]));
			elemRoot.appendChild(elem);
		}

		return elemRoot;
	}

	protected void setValues(Element elemRoot) throws Exception
	{
		if (!elemRoot.getTagName().equals("PaymentOption"))
		{
			throw new Exception(EXCEPTION_WRONG_XML_STRUCTURE);
		}

		m_type = elemRoot.getAttribute("type");
		m_name = elemRoot.getAttribute("name");
		m_generic = XMLUtil.parseAttribute(elemRoot, "generic", true);
		m_minJapVersion = XMLUtil.parseAttribute(elemRoot, "japversion", Util.VERSION_FORMAT);
		m_maxClicks = XMLUtil.parseAttribute(elemRoot, XML_ATTR_MAXCLICKS, MAX_CLICKS_UNLIMITED);

		Node markupElem = XMLUtil.getFirstChildByName(elemRoot,"Markup");
		m_markup = XMLUtil.parseValue(markupElem,0);
        //parse headings
		NodeList nodesHeadings = elemRoot.getElementsByTagName("Heading");
		for (int i = 0; i < nodesHeadings.getLength(); i++)
		{
			String heading = XMLUtil.parseValue(nodesHeadings.item(i), null);
			String language = ( (Element) nodesHeadings.item(i)).getAttribute("lang");
			if (language == null || heading == null)
			{
				throw new Exception(EXCEPTION_WRONG_XML_STRUCTURE);
			}
			m_headings.addElement(new String[]
								  {heading, language});
		}
        //parse detailed info
		NodeList nodesDetailed = elemRoot.getElementsByTagName("DetailedInfo");
		for (int i = 0; i < nodesDetailed.getLength(); i++)
		{
			String info = XMLUtil.parseValue(nodesDetailed.item(i), null);
			String language = ( (Element) nodesDetailed.item(i)).getAttribute("lang");
			if (language == null || info == null)
			{
				throw new Exception(EXCEPTION_WRONG_XML_STRUCTURE);
			}

			m_detailedInfos.addElement(new String[]
									   {info, language});
		}
		//parse ranks
		NodeList rankNodes = elemRoot.getElementsByTagName("Rank");
		for (int i = 0; i < rankNodes.getLength(); i++ )
		{
			String language = ( (Element) rankNodes.item(i)).getAttribute("lang");
			int rank = XMLUtil.parseValue(rankNodes.item(i),Integer.MAX_VALUE); //if no rank given, send to end of list
			m_ranks.put(language,new Integer(rank));
		}

		//parse payment delays
		NodeList nodesDelay = elemRoot.getElementsByTagName("PaymentDelay");
		for (int i = 0; i < nodesDelay.getLength(); i++)
		{
			String delay = XMLUtil.parseValue(nodesDelay.item(i), "");
			String language = ( (Element) nodesDelay.item(i)).getAttribute("lang");
			if (language == null || delay == null)
			{
				throw new Exception(EXCEPTION_WRONG_XML_STRUCTURE);
			}

			m_paymentDelays.addElement(new String[]
									   {delay, language});
		}

		//parse extra infos
		NodeList nodesExtra = elemRoot.getElementsByTagName("ExtraInfo");
		for (int i = 0; i < nodesExtra.getLength(); i++)
		{
			String info = XMLUtil.parseValue(nodesExtra.item(i), null);
			String language = ( (Element) nodesExtra.item(i)).getAttribute("lang");
			String type = ( (Element) nodesExtra.item(i)).getAttribute("type");
			if (language == null || info == null || type == null)
			{
				throw new Exception(EXCEPTION_WRONG_XML_STRUCTURE);
			}
			m_extraInfos.addElement(new String[]
									{info, type, language});
		}

		NodeList nodesInput = elemRoot.getElementsByTagName("input");

		for (int i = 0; i < nodesInput.getLength(); i++)
		{
			String label = XMLUtil.parseValue(nodesInput.item(i).getFirstChild(), null);
			String lang = ( (Element) nodesInput.item(i).getFirstChild()).getAttribute("lang");
			String ref = ( (Element) nodesInput.item(i)).getAttribute("ref");
			if (lang == null || label == null || ref == null)
			{
				throw new Exception(EXCEPTION_WRONG_XML_STRUCTURE);
			}
			m_inputFields.addElement(new String[]
									 {ref, label, lang});
		}

		try
		{
			String imageLink = XMLUtil.parseValue(XMLUtil.getFirstChildByName(elemRoot, "ImageLink").
												  getFirstChild(), "0");
			if (!imageLink.equals("0"))
			{
				m_imageLink = imageLink;
			}
		}
		catch (Exception e)
		{
			m_imageLink = null;
		}
	}

	public void setType(String a_type)
	{
		m_type = a_type;
	}

	/**
	 * getHeading
	 *
	 * @param a_langShort String
	 * @return String: heading int the specified language (if not found: returns English heading)
	 */
	public String getHeading(String a_langShort)
	{
		int debug = m_headings.size();
		for (int i = 0; i < m_headings.size(); i++)
		{
			String[] heading = (String[]) m_headings.elementAt(i);
			if (heading[1].equalsIgnoreCase(a_langShort))
			{
				return heading[0];
			}
		}
		if (a_langShort.equals("en") )
		{
			return null;
		}
		else
		{
			return getHeading("en");
		}
	}

	public String getDetailedInfo(String a_langShort)
	{
		for (int i = 0; i < m_detailedInfos.size(); i++)
		{
			String[] detailedInfo = (String[]) m_detailedInfos.elementAt(i);
			if (detailedInfo[1].equalsIgnoreCase(a_langShort))
			{
				return detailedInfo[0];
			}
		}
		if (a_langShort.equals("en") )
		{
			return null;
		}
		else
		{
			return getDetailedInfo("en");
		}
	}
	/**
	 *
	 * @param a_langShort String
	 * @return int: the option's rank for the given language, or NULL if not set
	 * (does NOT fall back to the rank for english, or default values - if no rank is set for this language, we want the comparator to notice,
		so it won't compare one option's proper rank to the second's option rank for English)
	 * special case: no rank set for english -> will return Integer.MAX_VALUE
	 */
	public Integer getRank(String a_langShort)
	{
		Integer rank = (Integer) m_ranks.get(a_langShort);
		if (rank == null && a_langShort.equalsIgnoreCase("en") )
		{
			rank = new Integer(Integer.MAX_VALUE);
		}
		return rank;
	}

	public String getPaymentDelay(String a_langShort)
	{
		for (int i = 0; i < m_paymentDelays.size(); i++)
		{
			String[] paymentDelay = (String[]) m_paymentDelays.elementAt(i);
			if (paymentDelay[1].equalsIgnoreCase(a_langShort))
			{
				return paymentDelay[0];
			}
		}
		if (! a_langShort.equalsIgnoreCase("en") )
		{
			return getPaymentDelay("en");
		}
		else
		{
			return null; //already was looking for english -> stop a potentially endless loop
		}
	}


	public String getExtraInfo(String a_langShort)
	{
		for (int i = 0; i < m_extraInfos.size(); i++)
		{
			String[] extraInfo = (String[]) m_extraInfos.elementAt(i);
			if (extraInfo[2].equalsIgnoreCase(a_langShort))
			{
				return extraInfo[0];
			}
		}
		//still there? try to fall back to English (NOT recursively, otherwise we'd have endless recursion if English info is missing, too)
		if (a_langShort.equals("en") )
		{
			return null; //no more languages to fall back to
		}
		else
		{
			return getExtraInfo("en");
		}
	}

	/**
	 * getExtraInfos
	 *
	 * @return Vector: all Extrainfos, elements of the Vector are String[3] = { "info", "type", "lang"}
	 */
	public Vector getExtraInfos()
	{
		return (Vector) m_extraInfos.clone();
	}

	/**
	 * getLocalizedExtraInfoText
	 * like getExtraInfos(), except:
	 * - only returns "info", without type (so you can get Strings from the Vector directly instead of an Array)
	 * - only in the given language (so you don't have to iterate over the Vector to get one language's strings)
	 * (after Jap version 0.08.101, will fall back to english if no extra infos at all were found for the given language)
	 *
	 * Warning: extraInfos that do not exist for the given language will not be returned at all
	 * (ideally, we'd fall back to english, but the data structures used don't allow figuring out which
	 * extraInfos are equivalents in different languages)
	 *
	 * @param language 2-letter code for the expected language
	 * @return Vector: all Extrainfos for given language as String (empty Vector if nothing set for the given language)
	 *
	 */
	public Vector getLocalizedExtraInfoText(String language)
	{
		Vector allInfos = getExtraInfos();
		Vector localizedInfos = new Vector();
		//get Strings for the given language
		for (Enumeration infos = allInfos.elements(); infos.hasMoreElements(); )
		{
			String[] info = (String[]) infos.nextElement();
			if ( info[2].equals(language) )
			{
				localizedInfos.addElement(info[0]);
			}
		}
		// if not even a single String found for the given language, fall back to English
		if (localizedInfos.size() < 1)
		{
			localizedInfos = getLocalizedExtraInfoText("en");
		}
		return localizedInfos;
	}

	public String getType()
	{
		return m_type;
	}

	public String getName()
	{
		return m_name;
	}

	public String getExtraInfoType(String a_langShort)
	{
		for (int i = 0; i < m_extraInfos.size(); i++)
		{
			String[] extraInfo = (String[]) m_extraInfos.elementAt(i);
			if (extraInfo[2].equalsIgnoreCase(a_langShort))
			{
				return extraInfo[1];
			}
		}
		return getExtraInfoType("en");
	}

	public Vector getInputFields()
	{
		return (Vector) m_inputFields.clone();
	}

	public Vector getLanguages()
	{
		return (Vector) m_languages.clone(); //returning a cloned copy since we don't want any other class to change the contents
	}

	public boolean isGeneric()
	{
		return m_generic;
	}

	/**
	 * Decrements the max clicks counter and returns if clicks for this payments option are
	 * still allowed (only applicable on payment options with web link).
	 * @return boolean
	 */
	public boolean decrementMaxClicks()
	{
		if (m_maxClicks == MAX_CLICKS_UNLIMITED)
		{
			return true;
		}
		if (m_maxClicks > 0)
		{
			m_maxClicks--;
			return true;
		}
		return false;
	}

	public boolean isMaxClicksRestricted()
	{
		return m_maxClicks != MAX_CLICKS_UNLIMITED;
	}

	public int getMaxClicks()
	{
		return m_maxClicks;
	}

	public int getMarkup()
	{
		return m_markup;
	}

	public String getMinJapVersion()
	{
		return m_minJapVersion;
	}

	public boolean isNewer(XMLPaymentOption a_paymentOption)
	{
		if (m_minJapVersion == null)
		{
				return false;
		}
		if (a_paymentOption.getMinJapVersion() == null)
		{
			return true;
		}
		return (Util.convertVersionStringToNumber(m_minJapVersion) >
				Util.convertVersionStringToNumber(a_paymentOption.getMinJapVersion()));
	}

	public boolean worksWithJapVersion(String a_version)
	{
		if (m_minJapVersion != null)
		{
			if (Util.convertVersionStringToNumber(m_minJapVersion) >
				Util.convertVersionStringToNumber(a_version))
			{
				return false;
			}
		}
		return true;
	}

	private static void addLanguage(String lang)
	{
		//check whether lang already is in m_languages, if not add
		if (! m_languages.contains(lang) )
		{
			m_languages.addElement(lang);
		}
	}
};
