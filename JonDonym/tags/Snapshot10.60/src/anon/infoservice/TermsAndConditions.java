package anon.infoservice;

import java.text.DateFormat;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Enumeration;
import java.util.StringTokenizer;
import java.util.Locale;

import javax.swing.text.DateFormatter;

import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;

import org.w3c.dom.Element;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import com.sun.org.apache.xalan.internal.xsltc.runtime.Hashtable;

import anon.crypto.CertPath;
import anon.crypto.JAPCertificate;
import anon.crypto.MultiCertPath;
import anon.crypto.SignatureVerifier;
import anon.crypto.X509SubjectKeyIdentifier;
import anon.crypto.XMLSignature;
import anon.util.IXMLEncodable;
import anon.util.Util;
import anon.util.XMLParseException;
import anon.util.XMLUtil;

public class TermsAndConditions implements IXMLEncodable
{
	//private Element m_xmlData;
	//private Document m_doc;
	
	//public static int TERMS_AND_CONDITIONS_TTL = 1000*60*60*24;
	
	public static final String XML_ATTR_TIME_ACCEPTED = "timeAccepted";
	public static final String XML_ATTR_DATE = "date";
	
	public final static String XML_ELEMENT_CONTAINER_NAME = "TermsAndConditionsList";
	public final static String XML_ELEMENT_NAME = "TermsAndConditions";
	public final static String XML_ELEMENT_TRANSLATION_NAME = Translation.XML_ELEMENT_NAME;
	
	//public final static String LOCAL_TC_DEFAULT_LANG = "en";
	
	private static final String XML_ATTR_LOCALE = "locale";
	private static final String XML_ATTR_DEFAULT_LOCALE = "default";
	private static final String XML_ATTR_REFERENCE_ID = "referenceId";

	private static final String DATE_FORMAT = "yyyyMMdd"; 
	
	/*public static String HTTP_REQUEST_STRING = "/tcs";
	public static String HTTP_SERIALS_REQUEST_STRING = "/tcserials";
	
	public static String POST_FILE = "/tc";*/
	
	private String m_strId;
	//public String m_ski;
	
	//public String m_referenceId;
	
	//public String m_locale;
	private Date m_date;
	
	private Hashtable translations;
	private Translation defaultTranslation = null;
	/*public long m_lastUpdate;
	public long m_serial;*/

	//private XMLSignature m_signature = null;

	//private MultiCertPath m_certPath = null;
	
	private final static Hashtable tcHashtable = new Hashtable();
	
	/**
	 * creates an empty Terms And Condtion object for the specified id and valid date
	 * which contains no translations.
	 */
	public TermsAndConditions(String id, String date) throws IllegalArgumentException
	{
		if(id == null)
		{
			throw new IllegalArgumentException("ID of terms and conditions must not be null!");
		}
		m_strId = id.toLowerCase();
		
		if(date == null)
		{
			throw new IllegalArgumentException("Date must not be null!");
		}
		
		ParsePosition p = new ParsePosition(0);
		SimpleDateFormat df = new SimpleDateFormat(DATE_FORMAT);
		m_date = df.parse(date, p);
		if(m_date == null)
		{
			throw new IllegalArgumentException("Date has not the valid format "+DATE_FORMAT);
		}
		translations = new Hashtable();
	}
	
	/*public TermsAndConditions(Element a_elem) throws XMLParseException
	{
		this(XMLUtil.createDocumentFromElement(a_elem), true);
	}*/
	
	public TermsAndConditions(Element termsAndConditionRoot) throws XMLParseException
	{
		//super(a_bJAPContext ? Long.MAX_VALUE : System.currentTimeMillis() + TERMS_AND_CONDITIONS_TTL);
		
		//m_doc = a_doc;
		//Element xmlData = a_doc.getDocumentElement();
		
		//m_serial = XMLUtil.parseAttribute(m_xmlData, XML_ATTR_SERIAL, -1);
		m_strId = XMLUtil.parseAttribute(termsAndConditionRoot, XML_ATTR_ID, null);
		if(m_strId == null)
		{
			throw new XMLParseException("attribute 'id' of TermsAndConditions must not be null!");
		}
		m_strId = m_strId.toLowerCase();
		//m_referenceId = XMLUtil.parseAttribute(m_xmlData, XML_ATTR_REFERENCE_ID, "");
		
		ParsePosition p = new ParsePosition(0);
		SimpleDateFormat df = new SimpleDateFormat(DATE_FORMAT);
		String dateStr = XMLUtil.parseAttribute(termsAndConditionRoot, XML_ATTR_DATE, null);
		if(dateStr == null)
		{
			throw new XMLParseException("attribute 'date' must not be null!");
		}
		m_date = df.parse(dateStr, p);
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
		//StringTokenizer token = new StringTokenizer(m_strId, "_");
		
		//int tokens = token.countTokens();
		
		//if (tokens >= 1)
		//{
			// extract the ski
		//	m_ski = token.nextToken();
		//}
		//else
		//{
		//	m_ski = null;
		//}
		
		//if(tokens >= 2)
		//{
			// extract the locale
		//	m_locale = token.nextToken();
		//}
		//else
		//{
		//	m_locale = LOCAL_TC_DEFAULT_LANG;			
		//}
		
		//m_lastUpdate = XMLUtil.parseAttribute(m_xmlData, XML_ATTR_LAST_UPDATE, -1L);
		
		// verify the signature
		//m_signature = SignatureVerifier.getInstance().getVerifiedXml(m_xmlData,
		//	SignatureVerifier.DOCUMENT_CLASS_MIX);
		//if (m_signature != null)
		//{
		//	m_certPath = m_signature.getMultiCertPath();
		//}
		
		//if (!checkId())
		//{
		//	throw new XMLParseException(XMLParseException.ROOT_TAG, "Malformed id for TermsAndConditons object: " + m_strId);
		//}
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
					" and could be added.");
			return;
		}
		
		if(t.isDefaultLocale())
		{
			defaultTranslation = t;
		}
		translations.put(t.getLocale(), t);
	}
	
	public synchronized Translation getDefaultTranslation()
	{
		return defaultTranslation;
	}
	
	public synchronized Translation getTranslation(String locale)
	{
		return (Translation) translations.get(locale.trim().toLowerCase());
	}
	
	public synchronized boolean hasTranslation(String locale)
	{
		return translations.containsKey(locale.trim().toLowerCase());
	}
	
	public boolean hasTranslation(Locale locale)
	{
		return hasTranslation(locale.getDisplayLanguage());
	}
	
	/*public boolean checkId()
	{
		if(m_signature == null)
		{
			System.out.println(""i);
			LogHolder.log(LogLevel.INFO,LogType.CRYPTO,"AbstractDistributableCertifiedDatabaseEntry::checkId() -- signature is NULL!");
			return false;
		}
		String ski = m_signature.getMultiCertPath().getPath().getSecondCertificate().getSubjectKeyIdentifier();
		
		ski = (ski != null) ?  Util.replaceAll(ski, ":", "") : "";
		System.out.println("m_ski: "+m_ski+", ski: "+ski);
		return  (m_ski != null) && m_ski.equalsIgnoreCase(ski);
		//Not needed anymore: Terms And Cnditions are direclty transmitted form the mix
		
	}*/


	public String getId() 
	{
		return m_strId.toLowerCase();
	}

	/*public String getSKI()
	{
		return m_ski;
	}*/
	
	/*public long getLastUpdate()
	{
		return m_lastUpdate;
	}
	
	public long getVersionNumber()
	{
		return m_serial;
	}*/
	
	public Date getDate()
	{
		return m_date;
	}
	
	/*public Element getXmlStructure()
	{
		return m_xmlData;
	}*/
	
	/*public String getPostFile()
	{
		return POST_FILE;
	}*/
	
	/*public Document getDocument()
	{
		return m_doc;
	}*/
	
	/*public boolean isVerified()
	{
		if (m_signature != null)
		{
			return m_signature.isVerified();
		}
		return false;
	}*/

	/*public boolean isValid()
	{
		if (m_certPath != null)
		{
			return m_certPath.isValid(new Date());
		}
		return false;
	}*/
	
	public static synchronized void storeTermsAndConditions(TermsAndConditions tc)
	{
		tcHashtable.put(tc.getId().toLowerCase(), tc);
	}
	
	public static synchronized TermsAndConditions getById(String a_id)
	{
		return (TermsAndConditions) tcHashtable.get(a_id.toLowerCase());
	}
	
	/*public static synchronized Translation getTranslationById(String a_id, String language)
	{
		TermsAndConditions tc = (TermsAndConditions) tcHashtable.get(a_id);
		if(tcHashtable.get(a_id) != null)
		{
			return tc.getTranslation(language.trim().toLowerCase());
		}
		return null;
	}*/
	
	/*public static Translation getTranslationById(String a_id, Locale locale)
	{
		return getTranslationById( a_id, locale.getLanguage().trim().toLowerCase());
	}*/
	
	
	
	public String getHTMLText(Locale locale)
	{
		return getHTMLText(locale.getLanguage());
	}
	
	
	
	/* if lanugae is not supported get the defaultLanguage text */
	public String getHTMLText(String language)
	{
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
	
	/*public static TermsAndConditions getById(String a_id, String lang)
	{
		Database db = Database.getInstance(TermsAndConditions.class);
		
		if(lang.equals(LOCAL_TC_DEFAULT_LANG))
		{
			return (TermsAndConditions) db.getEntryById(a_id);
		}
		
		TermsAndConditions tc = (TermsAndConditions) db.getEntryById(a_id + "_" + lang);
		
		// localized version not found, try the English one
		if(tc == null)
		{
			return (TermsAndConditions) db.getEntryById(a_id); 
		}
		
		return tc;
	}*/
	
	public boolean equals(Object a_object)
	{
		return this.getId().equals( ( (TermsAndConditions) a_object).getId());
	}
	
	/*public int hashCode()
	{
		return (getId().toLowerCase().hashCode());
	}*/

	/*public XMLSignature getSignature()
	{
		return m_signature;
	}

	public MultiCertPath getCertPath()
	{
		return m_certPath;
	}*/

	/*
	 compares the dates of this TermsAndCondition object
	 * with the one specified by o
	 * 
	 * (right now we can't implement
	 *  java.lang.Comparable due to java 1.1 restrictions. 
	 *  but perhaps someday we could so better use this method).  
	 */
	public int compareTo(Object o)
	{
		TermsAndConditions otherTc = (TermsAndConditions) o;
		return m_date.equals(otherTc.getDate()) ? 0 : m_date.before(otherTc.getDate()) ? -1 : 1;
	}
	
	public Element toXmlElement(Document a_doc) 
	{
		Element tcRoot = a_doc.createElement(XML_ELEMENT_NAME);
		XMLUtil.setAttribute(tcRoot, XML_ATTR_ID, m_strId);
		XMLUtil.setAttribute(tcRoot, XML_ATTR_DATE, getDateString());
		Enumeration allTranslations = translations.elements();
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
		
		/*public int hashCode() 
		{
			return locale.hashCode();
		}*/

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
