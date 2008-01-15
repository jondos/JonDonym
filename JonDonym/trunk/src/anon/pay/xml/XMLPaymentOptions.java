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

import java.util.Vector;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import anon.util.IXMLEncodable;
import anon.util.XMLUtil;
import java.util.Enumeration;
import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;
import org.w3c.dom.Node;

/**
 * This class represents a XMLPaymentOptions structure.
 * @author Tobias Bayer, Elmar Schraml
 */
public class XMLPaymentOptions implements IXMLEncodable
{
	private Vector m_currencies = new Vector();
	private Vector m_paymentOptions = new Vector(); //holds XMLPaymentOption objects
	private String m_acceptedCreditCards;

	public XMLPaymentOptions(String xml) throws Exception
	{
		Document doc = XMLUtil.toXMLDocument(xml);
		setValues(doc.getDocumentElement());
	}

	public XMLPaymentOptions()
	{
	}

	public XMLPaymentOptions(Element xml) throws Exception
	{
		setValues(xml);
	}

	public Element toXmlElement(Document a_doc)
	{
		Element elemRoot = a_doc.createElement("PaymentOptions");
		elemRoot.setAttribute("version", "1.0");

		Element elem;

		for (int i = 0; i < m_currencies.size(); i++)
		{
			elem = a_doc.createElement("Currency");
			elem.appendChild(a_doc.createTextNode( (String) m_currencies.elementAt(i)));
			elemRoot.appendChild(elem);

		}

		for (int i = 0; i < m_paymentOptions.size(); i++)
		{
			try
			{
				XMLPaymentOption anOption = (XMLPaymentOption) m_paymentOptions.elementAt(i);
				elem = anOption.toXmlElement(a_doc);
				elemRoot.appendChild(elem);
			}
			catch (ClassCastException e)
			{
				//System.out.println(e.getMessage()); //just to check, message is normally null
			}
		}

		elem = a_doc.createElement("AcceptedCards");
		elem.appendChild(a_doc.createTextNode(m_acceptedCreditCards));
		elemRoot.appendChild(elem);

		return elemRoot;
	}

	private void setValues(Element elemRoot) throws Exception
	{
		if (!elemRoot.getTagName().equals("PaymentOptions"))
		{
			throw new Exception("XMLPaymentOptions wrong XML structure");
		}

		NodeList currencies = elemRoot.getElementsByTagName("Currency");
		for (int i = 0; i < currencies.getLength(); i++)
		{
			m_currencies.addElement(currencies.item(i).getFirstChild().getNodeValue());
		}

		NodeList options = elemRoot.getElementsByTagName("PaymentOption");
		XMLPaymentOption anOption;
		for (int i = 0; i < options.getLength(); i++)
		{
			anOption = new XMLPaymentOption((Element) options.item(i));
			m_paymentOptions.addElement(anOption);
		}

		Node node = XMLUtil.getFirstChildByName(elemRoot, "AcceptedCards");
		m_acceptedCreditCards = XMLUtil.parseValue(node, "");
	}

	public XMLPaymentOptions(Document document) throws Exception
	{
		setValues(document.getDocumentElement());
	}

	public void addOption(XMLPaymentOption a_option)
	{
		m_paymentOptions.addElement(a_option);
	}

	public void addCurrency(String a_currency)
	{
		m_currencies.addElement(a_currency);
	}

	public synchronized Enumeration getOptionHeadings(String a_language)
	{
		Vector optionHeadings = new Vector();
		this.setSortingLanguage(a_language);
		//Collections.sort(options, this); //Java 1.2 only, unfortunately
        sortVector();
		Vector options = (Vector) m_paymentOptions.clone();
		for (int i = 0; i < options.size(); i++)
		{
			try
			{
				XMLPaymentOption option = (XMLPaymentOption) options.elementAt(i);
				optionHeadings.addElement(option.getHeading(a_language));
			}
			catch (Exception e)
			{
				LogHolder.log(LogLevel.EXCEPTION, LogType.PAY,
							  "Could not get payment option heading: " + e.getMessage());
			}
		}
		return optionHeadings.elements();
	}


	public Vector getAllOptions(){
		   return getAllOptionsSortedByRank("en");
	}

	public synchronized Vector getAllOptionsSortedByRank(String a_lang)
	{
		//set this as Comparator -> uses XMLPaymentOptions.compare() -> enables us to use a paramater to set which language's ranks to sort by
		this.setSortingLanguage(a_lang);
		//Collections.sort(sortedOptions,this); //Java 1.2 only, unfortunately
		sortVector();
		Vector sortedOptions = (Vector) m_paymentOptions.clone(); //so the member Vector remains unchanged the Getter
		return sortedOptions;
	}

	/**
	 * Gets a XMLPaymentOption object for the provided heading
	 * @param a_heading String
	 * @param a_language String
	 * @return XMLPaymentOption
	 */
	public XMLPaymentOption getOption(String a_heading, String a_language)
	{
		for (int i = 0; i < m_paymentOptions.size(); i++)
		{
			try
			{
				XMLPaymentOption option = (XMLPaymentOption) m_paymentOptions.elementAt(i);
				String heading = option.getHeading(a_language);
				if (heading.equalsIgnoreCase(a_heading))
				{
					return option;
				}

			}
			catch (Exception e)
			{
				LogHolder.log(LogLevel.EXCEPTION, LogType.PAY,
							  "Could not get payment option for heading: " + a_heading + " in language " +
							  a_language);
			}

		}
		return null;
	}

	public Vector getCurrencies()
	{
		return (Vector) m_currencies.clone();
	}

	public void setAcceptedCreditCards(String a_acceptedCreditCards)
	{
		m_acceptedCreditCards = a_acceptedCreditCards;
	}

	public String getAcceptedCreditCards()
	{
		return m_acceptedCreditCards;
	}
	/**
	 *  --- not usable directly vie implementing Comparator due to Comparator being Java version 1.2+ only ---
	 *
	 * used to compare 2 XMLPaymentOptions by rank
	 * will return -1 (i.e. smaller than) if option1's rank is lower than option2's rank,
	 * thereby placing option1 earlier in a sorted collection
	 *
	 * Ranks come from the JPI's database table paymentoptionranks,
	 * and can be set per language.
	 *
	 * By default, the ranks for English will be returned; to sort by the ranks for another language,
	 * setSortingLanguage(<2-letter-language code>) before using an XMLPaymentOptions object as a Comparator.
	 * If ranks have not been set for both options in the given language, the comparison will again fall back to comparison by the english ranks.
	 *
	 * @param o1 Object, @param o2 Object:
	 * both have to be XMLPaymentOptions, otherwise a ClassCastException will be thrown
	 * @return int
	 */
	public int compare(Object object1, Object object2)
	{
		XMLPaymentOption optionOne;
		XMLPaymentOption optionTwo;
		//see if casting to XMLPaymentOption works
		try
		{
			if (object1 == null || object2 == null)
			{
				throw new Exception("can not compare null objects");
			}
			optionOne = (XMLPaymentOption) object1;
			optionTwo = (XMLPaymentOption) object2;
		} catch (Exception e)
		{
			throw new ClassCastException("could not compare payment options, incompatible objects?" + e);
		}
		//get sortingLanguage, if it has been set, and both options support it, otherwise fall back to english
		String sortingLang = m_sortingLanguage;
		if (sortingLang == null || optionOne.getRank(sortingLang) == null || optionTwo.getRank(sortingLang) == null)
		{
			sortingLang = "en";
		}
	    //get ranks for sorting Language, and compare by ranks
		Integer rankOne = optionOne.getRank(sortingLang);
		Integer rankTwo = optionTwo.getRank(sortingLang);
		if (rankOne == null || rankTwo == null)
		{
			//should not be possible to happen, but no big deal, we'll just rank them equal
			return 0;
		}
		else
		{
			if (rankOne.intValue() < rankTwo.intValue())
			{
				return -1;
			}
			else if (rankOne.intValue() > rankTwo.intValue())
			{
				return 1;
			}
			return 0;
			//return rankOne.compareTo(rankTwo);
		}

	}

	private String m_sortingLanguage = null;
	public void setSortingLanguage(String a_lang)
	{
		m_sortingLanguage = a_lang;
	}

	/**
	 * replacement for calling Collections.sort() on the Vector
	 * not the world's most efficient sort, but for a bunch of options it'll do
	 */
	private void sortVector()
	{
		Vector optionsToSort = (Vector) m_paymentOptions.clone();
		Vector sortedOptions = new Vector();
		for (Enumeration allOptions = optionsToSort.elements(); allOptions.hasMoreElements(); )
		{
			XMLPaymentOption newOption = (XMLPaymentOption) allOptions.nextElement();

			//put it in new Vector, at the index of the first element with a higher rank
			boolean wasInserted = false;
			for (int i = 0; i < sortedOptions.size(); i++)
			{
				XMLPaymentOption curOption = (XMLPaymentOption) sortedOptions.elementAt(i);
				if ( compare(newOption,curOption) < 0) //new option's rank is smaller, i.e. we've gone past all options with a smaller rank
				{
					sortedOptions.insertElementAt(newOption,i); //curOption will be pushed towards the end of the Vector
					wasInserted = true;
					break;
				}
			}
			//no object with higher rank in Vector yet? insert at the end
			if (!wasInserted)
			{
				sortedOptions.addElement(newOption);
			}
		}
		m_paymentOptions = sortedOptions;
	}
}
