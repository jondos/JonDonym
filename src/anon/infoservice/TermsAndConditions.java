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

import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
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
import anon.util.JAPMessages;
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

	private static final String DATE_FORMAT = "yyyyMMdd"; 
	
	//private String m_strId;
	private ServiceOperator operator;
	
	private Date m_date;
	
	private Hashtable translations;
	private Translation defaultTl = null;
	
	private boolean accepted;
	private boolean read;
	
	private final static Hashtable tcHashtable = new Hashtable();
	
	public TermsAndConditions(ServiceOperator operator, String date) throws ParseException
	{
		this(operator, new SimpleDateFormat(DATE_FORMAT).parse(date));
	}
	/**
	 * Creates an empty Terms And Condition object for the specified id and validation date
	 * which serves as a container for the different translations.
	 * @throws ParseException 
	 */
	public TermsAndConditions(ServiceOperator operator, Date date) throws ParseException
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
		
		m_date = date;
		if(m_date == null)
		{
			throw new IllegalArgumentException("Date has not the valid format "+DATE_FORMAT);
		}
		translations = new Hashtable();
		accepted = false;
		read = false;
	}

	/**
	 * Creates a TermsAndConditions container from the given XML DOM element
	 * with all translation that are stored within this element.
	 * The ServiceOperator ID will be extracted from termsAndConditionsRoot
	 * @param termsAndConditionRoot the DOM Element from which the Container will be created
	 * @throws XMLParseException if termsAndConditionsRoot does not provide a valid operator SKI
	 * or the date attribute is missing
	 * @throws ParseException if the date is in a wrong format (must be .
	 * @throws SignatureException if the signature of a Terms and Conditions translation
	 * has no valid signature.
	 */
	public TermsAndConditions(Element termsAndConditionRoot) throws XMLParseException, ParseException, SignatureException
	{
		this(termsAndConditionRoot, null);
	}
	
	/**
	 * Creates a TermsAndConditions container from the given XML DOM element
	 * with all translation that are stored within this element.
	 * The ServiceOperator ID from op is used if it is not null. Otherwise the ID will be extracted from termsAndConditionsRoot
	 * @param termsAndConditionRoot the DOM Element from which the Container will be created
	 * @param op the Operator to whom these Terms And Conditions belong
	 * @throws XMLParseException if op is null and termsAndConditionsRoot does not provide a valid operator SKI
	 * or the date attribute is missing
	 * @throws ParseException if the date is in a wrong format.
	 * @throws SignatureException if the signature of a Terms and Conditions translation
	 * has no valid signature.
	 */
	public TermsAndConditions(Element termsAndConditionRoot, ServiceOperator operator) throws XMLParseException, ParseException, SignatureException
	{
		if(operator != null)
		{
			this.operator = operator;
		}
		else
		{
			String opSki = XMLUtil.parseAttribute(termsAndConditionRoot, XML_ATTR_ID, null);
			if(opSki == null)
			{
				throw new XMLParseException("attribute 'id' of TermsAndConditions must not be null!");
			}
			
			opSki = opSki.toUpperCase();
			
			this.operator = (ServiceOperator) Database.getInstance(ServiceOperator.class).getEntryById(opSki);
			if (this.operator == null)
			{
				throw new XMLParseException("invalid  id "+ opSki +": no operator found with this subject key identifier");
			}
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
		//TODO: enable signature check if working
		while (currentTranslation != null)
		{
			addTranslation(new Translation(currentTranslation), false);
			currentTranslation = 
				(Element) XMLUtil.getNextSiblingByName(currentTranslation, 
									Translation.XML_ELEMENT_NAME);
		}
		if (!hasTranslations())
		{
			throw new XMLParseException("TC of operator "+this.operator.getId()+" is invalid because it has no translations");
		}
		
		read = XMLUtil.parseAttribute(termsAndConditionRoot, XML_ATTR_ACCEPTED, null) != null;
		accepted = XMLUtil.parseAttribute(termsAndConditionRoot, XML_ATTR_ACCEPTED, false);
	}
	
	/**
	 * returns the date as String in the format 'yyyyMMdd' from when these T&Cs became valid
	 * @return the date in the format 'yyyyMMdd' from when these T&Cs became valid
	 */
	public String getDateString()
	{
		return new SimpleDateFormat(DATE_FORMAT).format(m_date);
	}
	
	/**
	 * adds a T&C translation which specified by the DOMElement translationRoot
	 * @param translationRoot the DOMELement form which the translation should be appended to the 
	 * T&Cs container
	 * @param withSignatureCheck specifies whether this translation should only appended if it contains a valid signature.
	 * @throws XMLParseException if the translation does not refer to a valid T&C template or the
	 * 'locale' attribute which specifies the language is not set.
	 * @throws SignatureException if translationRoot does not contain a valid signature
	 */
	public void addTranslation(Element translationRoot, boolean withSignatureCheck) throws XMLParseException, SignatureException
	{
		addTranslation(new Translation(translationRoot), withSignatureCheck);
	}
	
	/**
	 * adds a T&C translation which specified by the DOMElement translationRoot
	 * @param translationRoot the DOMELement form which the translation should be appended to the 
	 * T&Cs container
	 * @throws XMLParseException if the translation does not refer to a valid T&C template or the
	 * 'locale' attribute which specifies the language is not set.
	 * @throws SignatureException if translationRoot does not conatin a valid signature
	 */
	public void addTranslation(Element translationRoot) throws XMLParseException, SignatureException
	{
		//TODO: enable signature check if working
		addTranslation(new Translation(translationRoot), false);
	}
	
	public TermsAndConditionsTranslation removeTranslation(String locale)
	{
		return (TermsAndConditionsTranslation) translations.remove(locale.trim().toLowerCase());
	}
	
	public TermsAndConditionsTranslation removeTranslation(Locale locale)
	{
		return removeTranslation(locale.getLanguage());
	}
	
	public TermsAndConditionsTranslation initializeEmptyTranslation(String locale)
	{
		//TODO: should we add or not?
		Translation t = new Translation();
		t.setLocale(locale.trim().toLowerCase());
		try 
		{
			addTranslation(t, false);
		}
		catch (SignatureException e) 
		{
		}
		return t;
	}
	
	public TermsAndConditionsTranslation initializeEmptyTranslation(Locale locale)
	{
		return initializeEmptyTranslation(locale.getLanguage());
	}
	
	private synchronized void addTranslation(Translation t, boolean withSignatureCheck) throws SignatureException
	{
		if(withSignatureCheck)
		{
			if(!t.isVerified())
			{
				throw new SignatureException("Translation ["+t.getLocale()+"] of "+operator.getOrganization()+" is not verified");
			}
			if(!t.checkId())
			{
				throw new SignatureException("Translation ["+t.getLocale()+"] is not signed by its operator '"+
						operator.getOrganization()+"'");
			}
		}
		
		synchronized (this)
		{
			if(t.isDefaultTranslation())
			{
				defaultTl = t;
			}
		}
		translations.put(t.getLocale(), t);
	}
	
	/**
	 * returns the default translation of the T&C which is displayed if there
	 * is no translation available for current display language 
	 * @return the default T&C translation
	 */
	public synchronized TermsAndConditionsTranslation getDefaultTranslation()
	{
		return defaultTl;
	}
	
	/**
	 * returns the translation of the T&C specified by the corresponding locale object
	 * @param locale the locale refering to the desired translation language 
	 * @return the desired T&C translation or null if the translation does not exist.
	 */
	public TermsAndConditionsTranslation getTranslation(Locale locale)
	{
		return getTranslation(locale.getLanguage());
	}
	
	/**
	 * returns the translation of the T&C specified by the two letter language code
	 * @param the two letter code specifying the desired translation langugae 
	 * @return the desired T&C translation or null if the translation does not exist.
	 */
	public TermsAndConditionsTranslation getTranslation(String locale)
	{
		return (Translation) translations.get(locale.trim().toLowerCase());
	}
	
	/**
	 * returns all translations of this T&C container 
	 * @return all translations as (implementing interface TermsAndConditionsTranslation) 
	 * of this T&C container as an enumeration.
	 */
	public Enumeration getAllTranslations()
	{
		return translations.elements();
	}
	
	/**
	 * return the id of the template which is needed to render the translation specified
	 * by the two-letter language code
	 * @param locale
	 * @return
	 */
	public String getTemplateReferenceId(String locale)
	{
		Translation t = (Translation) translations.get(locale.trim().toLowerCase());
		return (t != null) ? t.getTemplateReferenceId() : null;
	}
	
	/**
	 * returns if this T&C container provides a translation specified by the two-letter language code
	 * @param locale the two letter-code of the language
	 * @return true if the specified translation exists, false otherwise
	 */
	public boolean hasTranslation(String locale)
	{
		return translations.containsKey(locale.trim().toLowerCase());
	}
	
	/**
	 * for checking if this T&C container provides a translation specified by the given locale object
	 * @param locale locale object referring to the corresponding language
	 * @return true if the specified translation exists, false otherwise
	 */
	public boolean hasTranslation(Locale locale)
	{
		return hasTranslation(locale.getLanguage());
	}
	
	/**
	 * for checking whether this T&C container has stored translations at all
	 * @return true if this T&C container provides at least one translation, false otherwise.
	 */
	public boolean hasTranslations()
	{
		return !translations.isEmpty();
	}
	
	/**
	 * for checking if a default translation is specified which must be true if this
	 * T&C container is not empty.
	 * @return true if this T&C container has a default translation false otherwise
	 */
	public synchronized boolean hasDefaultTranslation()
	{
		return defaultTl != null;
	}

	/**
	 * returns the ServiceOperator-DBEntry referring to the operator to whom these T&C belong.
	 * @return
	 */
	public ServiceOperator getOperator()
	{
		return operator;
	}
	
	public void setDate(Date date)
	{
		m_date = date;
	}
	
	/**
	 * return a date object which holds the date from when these T&Cs became valid
	 * @return  a date object which holds the date from when these T&Cs became valid.
	 */
	public Date getDate()
	{
		return m_date;
	}
	
	/**
	 * marks the T&Cs as accepted if 'accepted' is true or
	 * rejected otherwise
	 * @param accepted true stands for accept, false for reject
	 */
	public synchronized void setAccepted(boolean accepted)
	{
		this.accepted = accepted;
	}
	
	/**
	 * returns whether these T&C are accepted by the user
	 * this is true if and only if they were read and accepted.
	 * @return true if and only if the T&Cs were read and accepted, false otherwise
	 */
	public boolean isAccepted() 
	{
		return accepted;
	}
	
	/**
	 * for checking if these T&Cs were already read.
	 * @return true if T&Cs were read, false otherwise
	 */
	public boolean isRead() 
	{
		return read;
	}

	/**
	 * marks these T&Cs as read/unread
	 * @param read true means read, false unread
	 */
	public synchronized void setRead(boolean read) 
	{
		this.read = read;
	}
	
	/**
	 * returns whether these T&Cs wer rejected.
	 * this is true if and only if the T&Cs were read and and not accepted.
	 * @return true if and only if the T&Cs were read and and not accepted, false otherwise.
	 */
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
		TermsAndConditionsTranslation translation = getTranslation(language);
		if(translation == null)
		{
			translation = getDefaultTranslation();
		}
		//default translation must never be null
		
		TermsAndConditionsFramework displayTemplate = 
			TermsAndConditionsFramework.getById(translation.getTemplateReferenceId(), false);
		if(displayTemplate == null)
		{ 
			throw new NullPointerException("Associated template '"+translation.getTemplateReferenceId()+"' for" +
					" translation ["+translation.getLocale()+"] of terms and conditions for operator '"
					+operator.getOrganization()+"' not found.");
		}
		displayTemplate.importData(translation);
		return displayTemplate.transform();
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
	
	private Element xmlOut(Document doc, boolean signedTranslations) 
	{
		if(!hasTranslations() || !hasDefaultTranslation())
		{
			return null;
		}
		Element tcRoot = createTCRoot(doc);
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
			 tcRoot.appendChild(signedTranslations ?
					 ((Translation)allTranslations.nextElement()).toXmlElement(doc) : 
					 ((Translation)allTranslations.nextElement()).createXMLOutput(doc) );	
		}
		return tcRoot;
	}
	
	public Element createTCRoot(Document doc)
	{
		Element tcRoot = doc.createElement(XML_ELEMENT_NAME);
		XMLUtil.setAttribute(tcRoot, XML_ATTR_ID, operator.getId());
		XMLUtil.setAttribute(tcRoot, XML_ATTR_DATE, getDateString());
		return tcRoot;
	}
	
	public Element toXmlElement(Document doc) 
	{
		return xmlOut(doc, true);
	}
	
	public Element createXMLOutput(Document doc) 
	{
		return xmlOut(doc, false);
	}
	
	/**
	 * Class that represents a translation of the enclosing terms and conditions.
	 */
	private class Translation implements IXMLEncodable, TermsAndConditionsTranslation
	{
		private String templateReferenceId;
		private String locale;
		private boolean defaultTranslation;
		private Element translationElement;
		
		private String privacyPolicyUrl;
		private String legalOpinionsUrl;
		private String operationalAgreementUrl;
		
		private OperatorAddress operatorAddress;
		
		private XMLSignature signature = null;
		private MultiCertPath certPath = null;
		
		Translation(Element translationElement) throws XMLParseException
		{
			this(translationElement, true);
		}
		
		private Translation()
		{
			
		}
		
		private Translation(Element translationElement, boolean withAttributeCheck) throws XMLParseException
		{
			this.templateReferenceId = XMLUtil.parseAttribute(translationElement, XML_ATTR_REFERENCE_ID, "");
			
			if(withAttributeCheck && this.templateReferenceId.equals(""))
			{
				throw new XMLParseException("TC translation must refer to a valid TC template");
			}
			
			this.locale = XMLUtil.parseAttribute(translationElement, XML_ATTR_LOCALE, "");
			if(withAttributeCheck && this.locale.equals(""))
			{
				throw new XMLParseException("TC translation must set attribute 'locale'");
			}
			
			this.locale = this.locale.trim().toLowerCase();
			this.defaultTranslation = XMLUtil.parseAttribute(translationElement, XML_ATTR_DEFAULT_LOCALE, false);
	
			privacyPolicyUrl = XMLUtil.parseValue(XMLUtil.getFirstChildByName(translationElement, XML_ELEMENT_PRIVACY_POLICY), "");
			legalOpinionsUrl = XMLUtil.parseValue(XMLUtil.getFirstChildByName(translationElement, XML_ELEMENT_LEGAL_OPINIONS), "");
			operationalAgreementUrl= XMLUtil.parseValue(XMLUtil.getFirstChildByName(translationElement, XML_ELEMENT_OPERATIONAL_AGREEMENT), "");
			
			Element operatorElement = (Element) XMLUtil.getFirstChildByName(translationElement, ServiceOperator.XML_ELEMENT_NAME);
			if(operatorElement != null)
			{
				operatorAddress = new OperatorAddress(operatorElement);
			}
			else
			{
				operatorAddress = null;
			}
			//useful if this object is created from a signed XML structure.
			this.translationElement = translationElement;
			
			// verify the signature
			signature = SignatureVerifier.getInstance().getVerifiedXml(translationElement,
				SignatureVerifier.DOCUMENT_CLASS_MIX);
			if (signature != null)
			{
				certPath = signature.getMultiCertPath();
			}
		}
		
		public void setTemplateReferenceId(String templateReferenceId) 
		{
			this.templateReferenceId = templateReferenceId;
		}
		
		public String getTemplateReferenceId()
		{
			return templateReferenceId;
		}

		public void setLocale(String locale)
		{
			this.locale = locale;
		}
		
		public String getLocale()
		{
			return locale;
		}
		
		public boolean isDefaultTranslation()
		{
			return defaultTranslation;
		}
		
		public void setDefaultTranslation(boolean defaultTranslation)
		{
			this.defaultTranslation = defaultTranslation;
			if(defaultTranslation)
			{
				TermsAndConditions.this.defaultTl = this;
			}
		}
		
		public Element getTranslationElement()
		{
			return (Element) translationElement.cloneNode(true);
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
			if( (obj == null) || 
				!(obj instanceof TermsAndConditionsTranslation) ) return false;
			return this.locale.equals(((Translation) obj).locale);
		}
		
		/**
		 *  merely outputs the doc from which this translation was initialized.
		 *  If you want to output the current use the method createXMLOutput.
		 *  This method is to implement the IXMLEncodable interface and it should be used.
		 *  if this translation was created from a signed XML node.
		 */
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
		
		/**
		 * This method creates an xml Element according to the 
		 * current object state whereas
		 * toXMLElement outputs the xml represenation from which the object was ininitialized.
		 * this method does not append teh element to the specified document.
		 * @param doc the document which should own the newly created translation element
		 */
		public Element createXMLOutput(Document doc) 
		{
			Element root = doc.createElement(TermsAndConditionsTranslation.XML_ELEMENT_NAME);
			root.setAttribute(XML_ATTR_REFERENCE_ID, templateReferenceId);
			root.setAttribute(XML_ATTR_LOCALE, locale);
			if(defaultTranslation)
			{
				root.setAttribute(XML_ATTR_DEFAULT_LOCALE, "true");
			}
			if( (privacyPolicyUrl != null) && !privacyPolicyUrl.equals(""))
			{
				XMLUtil.createChildElementWithValue(root, XML_ELEMENT_PRIVACY_POLICY, privacyPolicyUrl);
			}
			if( (legalOpinionsUrl != null) && !legalOpinionsUrl.equals(""))
			{
				XMLUtil.createChildElementWithValue(root, XML_ELEMENT_LEGAL_OPINIONS, legalOpinionsUrl);
			}
			if( (operationalAgreementUrl != null) && !operationalAgreementUrl.equals(""))
			{
				XMLUtil.createChildElementWithValue(root, XML_ELEMENT_OPERATIONAL_AGREEMENT, operationalAgreementUrl);
			}
			if(operatorAddress != null)
			{
				Enumeration e = operatorAddress.getAddressAsNodeList(doc);
				Element operatorAddressRoot = null; 
				if(e.hasMoreElements())
				{
					operatorAddressRoot = doc.createElement(ServiceOperator.XML_ELEMENT_NAME);
					root.appendChild(operatorAddressRoot);
				}
				while (e.hasMoreElements()) 
				{
					operatorAddressRoot.appendChild((Element) e.nextElement());
				}
			}
			return root;
		}
		
		public void setOperatorAddress(OperatorAddress operatorAddress) 
		{
			this.operatorAddress = operatorAddress;
		}
		
		public OperatorAddress getOperatorAddress() 
		{
			return operatorAddress;
		}
		
		public String toString()
		{
			return new Locale(locale, "").getDisplayLanguage(JAPMessages.getLocale()) + (defaultTranslation ? " (default)" : "");
		}
		
		public Date getDate() 
		{
			return TermsAndConditions.this.getDate();
		}

		public ServiceOperator getOperator()
		{
			return TermsAndConditions.this.operator;
		}

		public String getPrivacyPolicyUrl() 
		{
			return privacyPolicyUrl;
		}

		public void setPrivacyPolicyUrl(String privacyPolicyUrl) 
		{
			this.privacyPolicyUrl = privacyPolicyUrl;
		}

		public String getLegalOpinionsUrl() 
		{
			return legalOpinionsUrl;
		}

		public void setLegalOpinionsUrl(String legalOpinionsUrl) 
		{
			this.legalOpinionsUrl = legalOpinionsUrl;
		}

		public String getOperationalAgreementUrl() 
		{
			return operationalAgreementUrl;
		}

		public void setOperationalAgreementUrl(String operationalAgreementUrl) 
		{
			this.operationalAgreementUrl = operationalAgreementUrl;
		}

		public TermsAndConditionsTranslation duplicateWithImports(
				Element xmlImports)
		{
			try 
			{
				Translation newTrans = new Translation(xmlImports, false);
				Object currentValue = null;
				Method currentGetter = null;
				Method currentSetter = null;
				
				PropertyDescriptor translationPDs[] =
					Introspector.getBeanInfo(this.getClass()).getPropertyDescriptors();
				for (int i = 0; i < translationPDs.length; i++) 
				{
					if( !translationPDs[i].getName().equals("operatorAddress") &&
						//!translationPDs[i].getName().equals("operator") &&
						translationPDs[i].getWriteMethod() != null)
					{
						currentGetter = translationPDs[i].getReadMethod();
						currentSetter = translationPDs[i].getWriteMethod();
						currentValue = currentGetter.invoke(this, null);
					
						if( (currentValue != null) && !currentValue.toString().equals(""))
						{
							currentSetter.invoke(
									newTrans, 
									new Object[]{currentValue});
						}
					}
				}
				
				if(newTrans.getOperator() == null)
				{
					newTrans.setOperatorAddress(operatorAddress);
				}
				else if(operatorAddress != null)
				{
					//now we update every address field on the newTrans address object
					//which is defined in the address object of the current translation
					PropertyDescriptor addressPDs[] =
						Introspector.getBeanInfo(OperatorAddress.class).getPropertyDescriptors();
					for (int i = 0; i < addressPDs.length; i++) 
					{
						if( addressPDs[i].getWriteMethod() != null )
						{
							currentGetter = addressPDs[i].getReadMethod();
							currentSetter = addressPDs[i].getWriteMethod();
							currentValue = currentGetter.invoke(this.operatorAddress, null);
							//update the field in the copied object.
							if( (currentValue != null) && !currentValue.toString().equals(""))
							{
								currentSetter.invoke(
										newTrans.operatorAddress, 
										new Object[]{currentValue});
							}
						}
					}
				}
				return newTrans;
			}
			catch (XMLParseException e) 
			{
			} 
			catch (IntrospectionException e) 
			{
			} catch (IllegalArgumentException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (InvocationTargetException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return null;
		}
	}
}
