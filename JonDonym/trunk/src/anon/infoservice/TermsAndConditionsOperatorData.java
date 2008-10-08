package anon.infoservice;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.Locale;

import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

import anon.crypto.CertPath;
import anon.crypto.JAPCertificate;
import anon.crypto.SignatureCreator;
import anon.crypto.SignatureVerifier;
import anon.crypto.XMLSignature;
import anon.util.XMLParseException;
import anon.util.XMLUtil;

public class TermsAndConditionsOperatorData extends AbstractDistributableCertifiedDatabaseEntry 
{
	private Element m_xmlData;
	
	public static int TERMS_AND_CONDITIONS_TTL = 1000*60*60*24;
	
	private static final String XML_ATTR_LOCALE = "locale";

	public String m_strId;
	
	public Locale m_locale;
	
	public long m_lastUpdate;
	public long m_serial;
	
	private JAPCertificate m_certificate = null;

	private XMLSignature m_signature = null;

	private CertPath m_certPath = null;
	
	public TermsAndConditionsOperatorData()
	{
		super(0);
	}

	public String getId() 
	{
		return m_strId;
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
		return "/posttcopdata";
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
	
	public TermsAndConditionsOperatorData(File a_file) throws XMLParseException, IOException
	{
		super(System.currentTimeMillis() + TERMS_AND_CONDITIONS_TTL);
		
		m_xmlData = XMLUtil.readXMLDocument(a_file).getDocumentElement();
		
		m_serial = XMLUtil.parseAttribute(m_xmlData, XML_ATTR_SERIAL, -1);
		m_locale = new Locale(XMLUtil.parseAttribute(m_xmlData, XML_ATTR_LOCALE, Locale.ENGLISH.toString()));
		m_strId = XMLUtil.parseAttribute(m_xmlData, XML_ATTR_ID, "");
		
		m_lastUpdate = System.currentTimeMillis();
		
		SignatureCreator.getInstance().signXml(SignatureVerifier.DOCUMENT_CLASS_INFOSERVICE, m_xmlData);
		
		// verify the signature
		m_signature = SignatureVerifier.getInstance().getVerifiedXml(m_xmlData,
			SignatureVerifier.DOCUMENT_CLASS_INFOSERVICE);
		if (m_signature != null)
		{
			m_certPath = m_signature.getCertPath();
			if (m_certPath != null)
			{
				m_certificate = m_certPath.getFirstCertificate();
			}
		}
	}
	
	// THIS IS TEMPORARY!!! T&C should be read from the mix info
	public static void loadFromDirectory(File a_dir)
	{
		File file = null;
		
		if(a_dir == null)
		{
			return;
		}
		
		String[] files = a_dir.list();
			
		if(files == null)
		{
			return;
		}
			
		/* Loop through all files in the directory to find XML files */
		for (int i = 0; i < files.length; i++)
		{
			try
			{
				file = new File(a_dir.getAbsolutePath() + File.separator + files[i]);
				TermsAndConditionsFramework tac = new TermsAndConditionsFramework(file);
				 
				Database.getInstance(TermsAndConditionsFramework.class).update(tac);
			}
			catch(XMLParseException ex)
			{
				LogHolder.log(LogLevel.EXCEPTION, LogType.MISC, "XMLParseException while loading Terms & Conditions: ", ex);
			}
			catch(IOException ex)
			{
				LogHolder.log(LogLevel.EXCEPTION, LogType.MISC, "IOException while loading Terms & Conditions: ", ex);
			}
		}
	}
}
