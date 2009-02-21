/*
Copyright (c) 2008 The JAP-Team, JonDos GmbH

All rights reserved.

Redistribution and use in source and binary forms, with or without modification, 
are permitted provided that the following conditions are met:

    * Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
    * Redistributions in binary form must reproduce the above copyright notice,
       this list of conditions and the following disclaimer in the documentation and/or
       other materials provided with the distribution.
    * Neither the name of the University of Technology Dresden, Germany, nor the name of
       the JonDos GmbH, nor the names of their contributors may be used to endorse or
       promote products derived from this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
"AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE REGENTS OR
CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package anon.infoservice;

import java.security.SignatureException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Locale;

import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import anon.crypto.MultiCertPath;
import anon.crypto.SignatureVerifier;
import anon.crypto.XMLSignature;
import anon.util.IXMLEncodable;
import anon.util.XMLParseException;
import anon.util.XMLUtil;

/**
 * This is the container for the operator specific sections of Terms and Conditions 
 * with all its translations. Any translation refers to a Terms and Conditions template which 
 * serves as a 'frame' in which the operator specific sections are displayed. 
 * This enables reusability of very common terms which are needed by all operators.
 * operator.
 * 
 * A terms and conditions container is referenced by the subject key identifier of its operator.
 * The translations are stored in the context of this object and are referenced by the two letter
 * country code.
 *  
 * A Terms and conditions container is either empty or must at least 
 * provide the default translation. An empty container cannot be 
 * transferred into an XML-DOM-structure.
 */
public class TermsAndConditions implements IXMLEncodable
{
	public static final String XML_ATTR_ACCEPTED = "accepted";
	public static final String XML_ATTR_DATE = "date";
	
	public final static String XML_ELEMENT_CONTAINER_NAME = "TermsAndConditionsList";
	public final static String XML_ELEMENT_NAME = "TermsAndConditions";
	public final static String XML_ELEMENT_TRANSLATION_NAME = Translation.XML_ELEMENT_NAME;
	
	private static final String XML_ATTR_LOCALE = "locale";
	private static final String XML_ATTR_DEFAULT_LOCALE = "default";
	private static final String XML_ATTR_REFERENCE_ID = "referenceId";

	private static final String DATE_FORMAT = "yyyyMMdd"; 
	
	//private String m_strId;
	private ServiceOperator operator;
	
	private Date m_date;
	
	private Hashtable translations;
	private Translation defaultTranslation = null;
	
	private boolean accepted;
	private boolean read;
	
	private final static Hashtable tcHashtable = new Hashtable();
	
	/**
	 * creates an empty Terms And Condition object for the specified id and validation date
	 * which serves as a contianer for the different translations.
	 * @throws ParseException 
	 */
	public TermsAndConditions(ServiceOperator operator, String date) throws ParseException
	{
		if(operator == null)
		{
			throw new NullPointerException("Operator of terms and conditions must not be null!");
		}
		this.operator = operator;
		
		if(date == null)
		{
			throw new NullPointerException("Date of terms and conditions must not be null!");
		}
		
		m_date = new SimpleDateFormat(DATE_FORMAT).parse(date);
		if(m_date == null)
		{
			throw new IllegalArgumentException("Date has not the valid format "+DATE_FORMAT);
		}
		translations = new Hashtable();
		accepted = false;
		read = false;
	}

	public TermsAndConditions(Element termsAndConditionRoot) throws XMLParseException, ParseException, SignatureException
	{
		String opSki = XMLUtil.parseAttribute(termsAndConditionRoot, XML_ATTR_ID, null);
		if(opSki == null)
		{
			throw new XMLParseException("attribute 'id' of TermsAndConditions must not be null!");
		}
		opSki = opSki.toUpperCase();
		
		this.operator = (ServiceOperator) Database.getInstance(ServiceOperator.class).getEntryById(opSki);
		if (operator == null)
		{
			throw new XMLParseException("invalid  id "+ opSki +": no operator found with this subject key identifier");
		}
		
		String dateStr = XMLUtil.parseAttribute(termsAndConditionRoot, XML_ATTR_DATE, null);
		if(dateStr == null)
		{
			throw new XMLParseException("attribute 'date' must not be null!");
		}
		m_date = new SimpleDateFormat(DATE_FORMAT).parse(dateStr);
		translations = new Hashtable();
		
		Element currentTranslation = 
			(Element) XMLUtil.getFirstChildByName(termsAndConditionRoot, 
									Translation.XML_ELEMENT_NAME);
		while (currentTranslation != null)
		{
			addTranslation(new Translation(currentTranslation));
			currentTranslation = 
				(Element) XMLUtil.getNextSiblingByName(currentTranslation, 
									Translation.XML_ELEMENT_NAME);
		}
		if (!hasTranslations())
		{
			throw new XMLParseException("TC of operator "+opSki+" is invalid because it has no translations");
		}
		
		read = XMLUtil.parseAttribute(termsAndConditionRoot, XML_ATTR_ACCEPTED, null) != null;
		accepted = XMLUtil.parseAttribute(termsAndConditionRoot, XML_ATTR_ACCEPTED, false);
	}
	
	public String getDateString()
	{
		return new SimpleDateFormat(DATE_FORMAT).format(m_date);
	}
	
	public synchronized void addTranslation(Element translationRoot) throws XMLParseException, SignatureException
	{
		addTranslation(new Translation(translationRoot));
	}
	
	
	public synchronized void addTranslation(Translation t) throws SignatureException
	{
		//if(t.isDefaultLocale()) throw new SignatureException("Just a silly test");
		if(!t.isVerified())
		{
			throw new SignatureException("Translation ["+t.getLocale()+"] of "+operator.getOrganization()+" is not verified");
		}
		if(!t.checkId())
		{
			throw new SignatureException("Translation ["+t.getLocale()+"] is not signed by its operator '"+
					operator.getOrganization()+"'");
		}
		
		synchronized (this)
		{
			if(t.isDefaultLocale())
			{
				defaultTranslation = t;
			}
		}
		translations.put(t.getLocale(), t);
	}
	
	public synchronized Translation getDefaultTranslation()
	{
		return defaultTranslation;
	}
	
	public Translation getTranslation(Locale locale)
	{
		return getTranslation(locale.getLanguage());
	}
	
	public Translation getTranslation(String locale)
	{
		return (Translation) translations.get(locale.trim().toLowerCase());
	}
	
	public String getTemplateReferenceId(String locale)
	{
		Translation t = (Translation) translations.get(locale.trim().toLowerCase());
		return (t != null) ? t.getReferenceId() : null;
	}
	
	public boolean hasTranslation(String locale)
	{
		return translations.containsKey(locale.trim().toLowerCase());
	}
	
	public boolean hasTranslation(Locale locale)
	{
		return hasTranslation(locale.getLanguage());
	}
	
	public boolean hasTranslations()
	{
		return !translations.isEmpty();
	}
	
	public synchronized boolean hasDefaultTranslation()
	{
		return defaultTranslation != null;
	}

	public ServiceOperator getOperator()
	{
		return operator;
	}
	
	public Date getDate()
	{
		return m_date;
	}
	
	public synchronized void setAccepted(boolean accepted)
	{
		this.accepted = accepted;
	}
	
	public boolean isAccepted() 
	{
		return accepted;
	}
	
	public boolean isRead() 
	{
		return read;
	}

	public synchronized void setRead(boolean read) 
	{
		this.read = read;
	}
	
	public synchronized boolean isRejected() 
	{
		return read && !accepted;
	}
	
	public static void storeTermsAndConditions(TermsAndConditions tc)
	{
		tcHashtable.put(tc.operator, tc);
	}
	
	public static TermsAndConditions getTermsAndConditions(ServiceOperator operator)
	{
		return (TermsAndConditions) tcHashtable.get(operator);
	}
	
	public static void removeTermsAndConditions(TermsAndConditions tc)
	{
		tcHashtable.remove(tc.operator);
	}
	
	public static void removeTermsAndConditions(ServiceOperator operator)
	{
		tcHashtable.remove(operator);
	}
	
	public static Element getAllTermsAndConditionsAsXMLElement(Document ownerDoc)
	{
		Enumeration allTermsAndConditions = null;
		Element listRoot = ownerDoc.createElement(XML_ELEMENT_CONTAINER_NAME);
		allTermsAndConditions = tcHashtable.elements();
		TermsAndConditions currentTC = null;
		while (allTermsAndConditions.hasMoreElements()) 
		{
			currentTC = (TermsAndConditions) allTermsAndConditions.nextElement();
			if(currentTC.hasTranslations())
			{
				listRoot.appendChild(currentTC.toXmlElement(ownerDoc));
			}
		}
		return listRoot;
	}
	
	public static void loadTermsAndConditionsFromXMLElement(Element listRoot)
	{
		if(listRoot == null)
		{
			LogHolder.log(LogLevel.WARNING, LogType.MISC, "TC list root is null!");
			return;
		}
		Element currentTCNode = (Element) XMLUtil.getFirstChildByName(listRoot, XML_ELEMENT_NAME);
		while(currentTCNode != null)
		{
			try 
			{
				storeTermsAndConditions(new TermsAndConditions(currentTCNode));
			}
			catch(XMLParseException xpe)
			{
				LogHolder.log(LogLevel.WARNING, LogType.MISC, "XML error occured while parsing the TC node:", xpe);
			} 
			catch (ParseException pe) 
			{
				LogHolder.log(LogLevel.WARNING, LogType.MISC, "Could not parse the TC node:", pe);
			}
			catch (SignatureException se) 
			{
				LogHolder.log(LogLevel.WARNING, LogType.MISC, "Terms and Condition cannot be loaded due to a wrong signature:", se);
			}
			currentTCNode = (Element) XMLUtil.getNextSiblingByName(currentTCNode, XML_ELEMENT_NAME);
		}
	}
	
	public String getHTMLText(Locale locale)
	{
		return getHTMLText(locale.getLanguage());
	}
	
	/* if language is not supported get the defaultLanguage text */
	public String getHTMLText(String language)
	{
		if(!hasTranslations())
		{
			throw new IllegalStateException("T&C document "+operator.getId()+
					" cannot be created when no translations are loaded.");
		}
		Translation translation = getTranslation(language);
		if(translation == null)
		{
			translation = getDefaultTranslation();
		}
		//default translation must never be null
		TermsAndConditionsFramework fr = 
			TermsAndConditionsFramework.getById(translation.getReferenceId(), false);
		if(fr == null)
		{ 
			throw new NullPointerException("Associated template '"+translation.getReferenceId()+"' for" +
					" translation ["+translation.getLocale()+"] of terms and conditions for operator '"
					+operator.getOrganization()+"' not found.");
		}
		fr.importData(translation);
		return fr.transform();
	}
	
	public boolean equals(Object anotherTC)
	{
		return operator.equals( ( (TermsAndConditions) anotherTC).operator);
	}

	/*
	 compares the dates of this TermsAndCondition object
	 * with the one specified by o
	 * 
	 * (right now we can't implement
	 *  java.lang.Comparable due to java 1.1 restrictions. 
	 *  but perhaps someday we could so better implement this method).  
	 */
	public int compareTo(Object o)
	{
		TermsAndConditions otherTc = (TermsAndConditions) o;
		return m_date.equals(otherTc.getDate()) ? 0 : m_date.before(otherTc.getDate()) ? -1 : 1;
	}
	
	public boolean isMostRecent(String toWhichDate) throws ParseException
	{
		return isMostRecent(new SimpleDateFormat(DATE_FORMAT).parse(toWhichDate));
	}
	
	/**
	 * true if the date of the T&Cs are equal or more recent than 'toWhichDate' 
	 */
	public boolean isMostRecent(Date toWhichDate)
	{
		return (m_date.equals(toWhichDate) || m_date.after(toWhichDate));
	}
	
	public Element toXmlElement(Document a_doc) 
	{
		if(!hasTranslations() || !hasDefaultTranslation())
		{
			return null;
		}
		Element tcRoot = a_doc.createElement(XML_ELEMENT_NAME);
		XMLUtil.setAttribute(tcRoot, XML_ATTR_ID, operator.getId());
		XMLUtil.setAttribute(tcRoot, XML_ATTR_DATE, getDateString());
		Enumeration allTranslations = null;
		synchronized (this)
		{
			if(read)
			{
				XMLUtil.setAttribute(tcRoot, XML_ATTR_ACCEPTED, accepted);
			}
			allTranslations = translations.elements();
		}
		
		while (allTranslations.hasMoreElements()) 
		{
			 tcRoot.appendChild(((Translation)allTranslations.nextElement()).toXmlElement(a_doc));	
		}
		return tcRoot;
	}
	
	/**
	 * Class that represents a translation of the enclosing terms and conditions.
	 */
	class Translation implements IXMLEncodable
	{
		public static final String XML_ELEMENT_NAME = "TCTranslation";
		public static final String XML_ELEMENT_CONTAINER_NAME = TermsAndConditions.XML_ELEMENT_NAME;
		
		private String referenceId;
		private String locale;
		private boolean defaultLocale;
		private Element translationElement;
		
		private XMLSignature signature = null;
		private MultiCertPath certPath = null;
		
		public Translation(Element translationElement) throws XMLParseException
		{
			this.referenceId = XMLUtil.parseAttribute(translationElement, XML_ATTR_REFERENCE_ID, "");
			if(this.referenceId.equals(""))
			{
				throw new XMLParseException("TC translation must refer to a valid TC template");
			}
			
			this.locale = XMLUtil.parseAttribute(translationElement, XML_ATTR_LOCALE, "");
			if(this.locale.equals(""))
			{
				throw new XMLParseException("TC translation must set attribute 'locale'");
			}
			
			this.locale = this.locale.trim().toLowerCase();
			this.defaultLocale = XMLUtil.parseAttribute(translationElement, XML_ATTR_DEFAULT_LOCALE, false);
			this.translationElement = translationElement;
			
			// verify the signature
			signature = SignatureVerifier.getInstance().getVerifiedXml(translationElement,
				SignatureVerifier.DOCUMENT_CLASS_MIX);
			if (signature != null)
			{
				certPath = signature.getMultiCertPath();
			}
		}
		
		public String getReferenceId()
		{
			return referenceId;
		}
		
		public String getLocale()
		{
			return locale;
		}
		
		public boolean isDefaultLocale()
		{
			return defaultLocale;
		}
		
		public Element getTranslationElement()
		{
			return translationElement;
		}

		public XMLSignature getSignature()
		{
			return signature;
		}

		public MultiCertPath getCertPath()
		{
			return certPath;
		}
		
		public boolean isVerified()
		{
			return (signature != null) ? signature.isVerified() : false;
		}

		public boolean isValid()
		{
			return (certPath != null) ? certPath.isValid(new Date()) : false;
		}
		
		public boolean checkId()
		{
			//REQUIREMENTS: 
			//1.There is still only one certification path for the signature of the TC translations 
			//  because only the operator ski of the first path found is checked.
			//2.The translation is signed with the mix certificate and checked against the ski 
			//  of the operator certificate
			return (certPath != null) ? 
					certPath.getPath().getSecondCertificate().getSubjectKeyIdentifierConcatenated().equals(getOperator().getId()): 
					false;
		}
		
		public boolean equals(Object obj) 
		{
			return this.locale.equals(((Translation) obj).locale);
		}

		public Element toXmlElement(Document a_doc) 
		{
			if (a_doc.equals(translationElement.getOwnerDocument()))
			{
				return translationElement;
			}
			else
			{
				try
				{
					return (Element) XMLUtil.importNode(a_doc, translationElement, true);
				}
				catch (XMLParseException a_e)
				{
					return null;
				}
			}
		}
		
		public ServiceOperator getOperator() 
		{
			return TermsAndConditions.this.operator;
		}
		
		public Date getDate() 
		{
			return TermsAndConditions.this.getDate();
		}
	}
}
