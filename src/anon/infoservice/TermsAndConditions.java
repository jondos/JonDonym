package anon.infoservice;

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
	
	private String m_strId;
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
	public TermsAndConditions(String id, String date) throws ParseException
	{
		if(id == null)
		{
			throw new IllegalArgumentException("ID of terms and conditions must not be null!");
		}
		m_strId = id.trim().toLowerCase();
		
		if(date == null)
		{
			throw new IllegalArgumentException("Date must not be null!");
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

	public TermsAndConditions(Element termsAndConditionRoot) throws XMLParseException, ParseException
	{
		m_strId = XMLUtil.parseAttribute(termsAndConditionRoot, XML_ATTR_ID, null);
		if(m_strId == null)
		{
			throw new XMLParseException("attribute 'id' of TermsAndConditions must not be null!");
		}
		m_strId = m_strId.trim().toLowerCase();
		
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
		while(currentTranslation != null)
		{
			addTranslation(new Translation(currentTranslation));
			currentTranslation = 
				(Element) XMLUtil.getNextSiblingByName(currentTranslation, 
									Translation.XML_ELEMENT_NAME);
		}
		if(!hasTranslations())
		{
			throw new XMLParseException("TC of operator "+m_strId+" is invalid because it has no translations");
		}
		read = termsAndConditionRoot.hasAttribute(XML_ATTR_ACCEPTED);
		accepted = read ? Boolean.parseBoolean(termsAndConditionRoot.getAttribute(XML_ATTR_ACCEPTED)) : false;
	}
	
	public String getDateString()
	{
		return new SimpleDateFormat(DATE_FORMAT).format(m_date);
	}
	
	public synchronized void addTranslation(Element translationRoot) throws XMLParseException
	{
		addTranslation(new Translation(translationRoot));
	}
	
	
	public synchronized void addTranslation(Translation t)
	{
		if(!t.isVerified())
		{
			LogHolder.log(LogLevel.ERR, LogType.MISC,"Translation ["+t.getLocale()+"] of "+m_strId+" is not verified" +
					" and could not be added.");
			return;
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
	
	public String getId() 
	{
		return m_strId;
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
		tcHashtable.put(tc.getId().toLowerCase(), tc);
	}
	
	public static TermsAndConditions getById(String a_id)
	{
		return (TermsAndConditions) tcHashtable.get(a_id.toLowerCase());
	}
	
	public static void removeTermsAndConditions(TermsAndConditions tc)
	{
		tcHashtable.remove(tc.getId());
	}
	
	public static void removeTermsAndConditions(String opSki)
	{
		tcHashtable.remove(opSki);
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
				LogHolder.log(LogLevel.WARNING, LogType.MISC, "Could not parse the TC node:\n"+XMLUtil.toString(currentTCNode));
			} 
			catch (ParseException pe) 
			{
				LogHolder.log(LogLevel.WARNING, LogType.MISC, "Could not parse the TC node due to invalid date format:\n"+XMLUtil.toString(currentTCNode));
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
			throw new IllegalStateException("BUG: T&C document "+m_strId+
					" cannot be created while no translations are already loaded.");
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
			return null;
		}
		fr.importData(translation);
		return fr.transform();
	}
	
	public boolean equals(Object a_object)
	{
		return this.getId().equals( ( (TermsAndConditions) a_object).getId());
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
		if(!hasTranslations())
		{
			return null;
		}
		Element tcRoot = a_doc.createElement(XML_ELEMENT_NAME);
		XMLUtil.setAttribute(tcRoot, XML_ATTR_ID, m_strId);
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
			if (signature != null)
			{
				return signature.isVerified();
			}
			return false;
		}

		public boolean isValid()
		{
			if (certPath != null)
			{
				return certPath.isValid(new Date());
			}
			return false;
		}
		
		public boolean equals(Object obj) 
		{
			return this.locale.equals(((Translation) obj).locale);
		}

		public Element toXmlElement(Document a_doc) 
		{
			if(a_doc.equals(translationElement.getOwnerDocument()))
			{
				return translationElement;
			}
			else
			{
				return (Element) a_doc.importNode(translationElement.cloneNode(true), true);
			}
		}
		
		public String getId() 
		{
			return TermsAndConditions.this.getId();
		}
		
		public Date getDate() 
		{
			return TermsAndConditions.this.getDate();
		}
	}
}
