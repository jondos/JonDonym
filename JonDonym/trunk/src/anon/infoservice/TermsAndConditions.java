package anon.infoservice;

import jap.TermsAndConditionsUpdater;
import jap.JAPController;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.Locale;
import java.util.StringTokenizer;

import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;

import org.w3c.dom.Element;
import org.w3c.dom.Document;

import anon.crypto.CertPath;
import anon.crypto.JAPCertificate;
import anon.crypto.SignatureCreator;
import anon.crypto.SignatureVerifier;
import anon.crypto.XMLSignature;
import anon.util.XMLParseException;
import anon.util.XMLUtil;

public class TermsAndConditions extends AbstractDistributableCertifiedDatabaseEntry 
{
	private Element m_xmlData;
	private Document m_doc;
	
	public static int TERMS_AND_CONDITIONS_TTL = 1000*60*60*24;
	
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
	
	public long m_lastUpdate;
	public long m_serial;
	
	private JAPCertificate m_certificate = null;

	private XMLSignature m_signature = null;

	private CertPath m_certPath = null;
	
	public TermsAndConditions(Document a_doc)
	{
		super(System.currentTimeMillis() + TERMS_AND_CONDITIONS_TTL);
		
		m_doc = a_doc;
		m_xmlData = a_doc.getDocumentElement();
		
		m_serial = XMLUtil.parseAttribute(m_xmlData, XML_ATTR_SERIAL, -1);
		m_strId = XMLUtil.parseAttribute(m_xmlData, XML_ATTR_ID, "");
		m_referenceId = XMLUtil.parseAttribute(m_xmlData, XML_ATTR_REFERENCE_ID, "");
		
		StringTokenizer token = new StringTokenizer(m_strId, "_");
		if(token.countTokens() >= 2)
		{
			// extract the locale
			m_locale = token.nextToken();
			
			// extract the ski
			m_ski = token.nextToken();
		}
		else
		{
			m_locale = "en";
			m_ski = null;
		}
		
		m_lastUpdate = XMLUtil.parseAttribute(m_xmlData, XML_ATTR_LAST_UPDATE, -1L);
		
		// verify the signature
		m_signature = SignatureVerifier.getInstance().getVerifiedXml(m_xmlData,
			SignatureVerifier.DOCUMENT_CLASS_MIX);
		if (m_signature != null)
		{
			m_certPath = m_signature.getCertPath();
			if (m_certPath != null)
			{
				m_certificate = m_certPath.getFirstCertificate();
			}
		}
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
			return m_certPath.checkValidity(new Date());
		}
		return false;
	}
	
	public JAPCertificate getCertificate()
	{
		return m_certificate;
	}
	
	public static TermsAndConditions getById(String a_id)
	{
		// first look if it's in our database
		TermsAndConditions tc = (TermsAndConditions) Database.getInstance(TermsAndConditions.class).getEntryById(a_id);
		
		if(tc != null)
		{
			return tc;
		}
		
		// not found, force an update and try again
		JAPController.getInstance().getTermsUpdater().update();
		
		// return the entry if found, otherwise null
		return (TermsAndConditions) Database.getInstance(TermsAndConditions.class).getEntryById(a_id);
	}
}
