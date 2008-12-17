package anon.infoservice;

import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.StringTokenizer;
import java.util.Locale;

import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;

import org.w3c.dom.Element;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import anon.crypto.CertPath;
import anon.crypto.JAPCertificate;
import anon.crypto.MultiCertPath;
import anon.crypto.SignatureVerifier;
import anon.crypto.X509SubjectKeyIdentifier;
import anon.crypto.XMLSignature;
import anon.util.XMLParseException;
import anon.util.XMLUtil;

public class TermsAndConditions extends AbstractDistributableCertifiedDatabaseEntry 
{
	private Element m_xmlData;
	private Document m_doc;
	
	public static int TERMS_AND_CONDITIONS_TTL = 1000*60*60*24;
	
	public static final String XML_ATTR_TIME_ACCEPTED = "timeAccepted";
	public static final String XML_ATTR_DATE = "date";
	
	public static String XML_ELEMENT_CONTAINER_NAME = "TermsAndConditionsList";
	public static String XML_ELEMENT_NAME = "TermsAndConditions";
	
	private static final String XML_ATTR_LOCALE = "locale";
	private static final String XML_ATTR_REFERENCE_ID = "referenceId";

	public static String HTTP_REQUEST_STRING = "/tcs";
	public static String HTTP_SERIALS_REQUEST_STRING = "/tcserials";
	
	public static String POST_FILE = "/tc";
	
	public String m_strId;
	public String m_ski;
	
	public String m_referenceId;
	
	public String m_locale;
	public Date m_date;
	
	public long m_lastUpdate;
	public long m_serial;

	private XMLSignature m_signature = null;

	private MultiCertPath m_certPath = null;
	
	public TermsAndConditions(Element a_elem) throws XMLParseException
	{
		this(XMLUtil.createDocumentFromElement(a_elem), true);
	}
	
	public TermsAndConditions(Document a_doc, boolean a_bJAPContext) throws XMLParseException
	{
		super(a_bJAPContext ? Long.MAX_VALUE : System.currentTimeMillis() + TERMS_AND_CONDITIONS_TTL);
		
		m_doc = a_doc;
		m_xmlData = a_doc.getDocumentElement();
		
		m_serial = XMLUtil.parseAttribute(m_xmlData, XML_ATTR_SERIAL, -1);
		m_strId = XMLUtil.parseAttribute(m_xmlData, XML_ATTR_ID, null);
		m_referenceId = XMLUtil.parseAttribute(m_xmlData, XML_ATTR_REFERENCE_ID, "");
		
		ParsePosition p = new ParsePosition(0);
		SimpleDateFormat df = new SimpleDateFormat("yyyyMMdd");
		m_date = df.parse(XMLUtil.parseAttribute(m_xmlData, XML_ATTR_DATE, null), p);
		
		StringTokenizer token = new StringTokenizer(m_strId, "_");
		
		int tokens = token.countTokens();
		
		if (tokens >= 1)
		{
			// extract the ski
			m_ski = token.nextToken();
		}
		else
		{
			m_ski = null;
		}
		
		if(tokens >= 2)
		{
			// extract the locale
			m_locale = token.nextToken();
		}
		else
		{
			m_locale = "en";			
		}
		
		m_lastUpdate = XMLUtil.parseAttribute(m_xmlData, XML_ATTR_LAST_UPDATE, -1L);
		
		// verify the signature
		m_signature = SignatureVerifier.getInstance().getVerifiedXml(m_xmlData,
			SignatureVerifier.DOCUMENT_CLASS_MIX);
		if (m_signature != null)
		{
			m_certPath = m_signature.getMultiCertPath();
		}
		
		if (!checkId())
		{
			throw new XMLParseException(XMLParseException.ROOT_TAG, "Malformed id for TermsAndConditons object: " + m_strId);
		}
	}
	
	public boolean checkId()
	{
		if(m_signature == null)
		{
			LogHolder.log(LogLevel.INFO,LogType.CRYPTO,"AbstractDistributableCertifiedDatabaseEntry::checkId() -- signature is NULL!");
			return false;
		}
		return  (m_ski != null) && m_ski.equals(m_signature.getXORofSKIs());
	}


	public String getId() 
	{
		return m_strId;
	}

	public String getReferenceId()
	{
		return m_referenceId;
	}
	
	public String getSKI()
	{
		return m_ski;
	}
	
	public String getLocale()
	{
		return m_locale;
	}
	
	public long getLastUpdate()
	{
		return m_lastUpdate;
	}
	
	public long getVersionNumber()
	{
		return m_serial;
	}
	
	public Date getDate()
	{
		return m_date;
	}
	
	public Element getXmlStructure()
	{
		return m_xmlData;
	}
	
	public String getPostFile()
	{
		return POST_FILE;
	}
	
	public Document getDocument()
	{
		return m_doc;
	}
	
	public boolean isVerified()
	{
		if (m_signature != null)
		{
			return m_signature.isVerified();
		}
		return false;
	}

	public boolean isValid()
	{
		if (m_certPath != null)
		{
			return m_certPath.isValid(new Date());
		}
		return false;
	}
	
	public static TermsAndConditions getById(String a_id, Locale a_locale)
	{
		String lang = a_locale.getLanguage();
		Database db = Database.getInstance(TermsAndConditions.class);
		
		if(lang.equals("en"))
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
	}
	
	public boolean equals(Object a_object)
	{
		boolean objectEquals = false;
		if (a_object != null)
		{
			if (a_object instanceof TermsAndConditions)
			{
				objectEquals = this.getId().equals( ( (TermsAndConditions) a_object).getId());
			}
		}
		return objectEquals;
	}
	
	public int hashCode()
	{
		return (getId().hashCode());
	}

	public XMLSignature getSignature()
	{
		return m_signature;
	}

	public MultiCertPath getCertPath()
	{
		return m_certPath;
	}
}
