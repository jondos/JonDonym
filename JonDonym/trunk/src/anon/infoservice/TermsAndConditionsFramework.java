package anon.infoservice;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import jap.JAPController;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Date;
import java.util.Locale;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.Transformer;

import anon.crypto.CertPath;
import anon.crypto.JAPCertificate;
import anon.crypto.SignatureCreator;
import anon.crypto.SignatureVerifier;
import anon.crypto.XMLSignature;
import anon.util.XMLParseException;
import anon.util.XMLUtil;
import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;

public class TermsAndConditionsFramework extends AbstractDistributableCertifiedDatabaseEntry
{
	private static final String XML_ELEMENT_OPERATIONAL_AGREEMENT_URL = "OperationalAgreementUrl";

	private static final String XML_ELEMENT_LEGAL_OPINIONS_URL = "LegalOpinionsUrl";

	private static final String XML_ELEMENT_SECTION = "Section";
	
	private static final String XML_ATTR_ID = "id";
	
	private static final String XML_ATTR_DATE = "date";
	
	private static final String XML_ATTR_LOCALE = "locale";
	
	private static final String XML_ATTR_TYPE = "type";
	
	private static final String XML_ELEMENT_PARAGRAPH = "Paragraph";
	
	private static final String XML_ELEMENT_PRIVACY_POLICY_URL = "PrivacyPolicyUrl";
	
	private static final String XML_ELEMENT_OPERATOR = "Operator";
	private static final String XML_ELEMENT_OPERATOR_NAME = "Name";
	private static final String XML_ELEMENT_OPERATOR_STREET = "Street";
	private static final String XML_ELEMENT_OPERATOR_POSTAL_CODE = "PostalCode";
	private static final String XML_ELEMENT_OPERATOR_CITY = "City";
	private static final String XML_ELEMENT_OPERATOR_COUNTRY = "OperatorCountry";
	private static final String XML_ELEMENT_OPERATOR_VAT = "VAT";
	private static final String XML_ELEMENT_OPERATOR_FAX = "Fax";
	private static final String XML_ELEMENT_OPERATOR_EMAIL = "eMail";
	
	public static int TERMS_AND_CONDITIONS_TTL = 1000*60*60*24;
	
	public static int TERMS_AND_CONDITIONS_UPDATE_INTERVAL = 1000*60*60;
	
	public static String TERMS_AND_CONDITIONS_TYPE_COMMON_LAW = "CommonLaw";
	public static String TERMS_AND_CONDITIONS_TYPE_GERMAN_LAW = "GermanLaw";
	public static String TERMS_AND_CONDITIONS_TYPE_GENERAL_LAW = "GeneralLaw";
	
	public static String XML_ELEMENT_CONTAINER_NAME = "TermsAndConditionsFrameworks";
	public static String XML_ELEMENT_NAME = "TermsAndConditionsFramework";
	
	public String m_strId = null;
	public Locale m_locale = null;
	public String m_type = null;
	
	public long m_lastUpdate;
	public long m_date;
	
	public Document m_doc;
	public Element m_xmlData;
	
	private JAPCertificate m_certificate = null;

	private XMLSignature m_signature = null;

	private CertPath m_certPath = null;
	
	// creation from xml structure
	public TermsAndConditionsFramework(Document a_doc)
	{
		super(System.currentTimeMillis() + TERMS_AND_CONDITIONS_TTL);
		
		m_doc = a_doc;
		m_xmlData = a_doc.getDocumentElement();
		
		m_date = XMLUtil.parseAttribute(m_xmlData, XML_ATTR_DATE, -1);
		m_locale = new Locale(XMLUtil.parseAttribute(m_xmlData, XML_ATTR_LOCALE, Locale.ENGLISH.toString()), "");
		m_type = XMLUtil.parseAttribute(m_xmlData, XML_ATTR_TYPE, TERMS_AND_CONDITIONS_TYPE_COMMON_LAW);
		
		m_strId = m_type + "_" + m_locale + "_" + m_date;
		
		m_lastUpdate = XMLUtil.parseAttribute(m_xmlData, XML_ATTR_LAST_UPDATE, -1L);
		
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
	
	// infoservice-creation from file
	public TermsAndConditionsFramework(File a_file) throws XMLParseException, IOException
	{
		super(System.currentTimeMillis() + TERMS_AND_CONDITIONS_TTL);
		
		m_doc = XMLUtil.readXMLDocument(a_file);
		
		m_xmlData = m_doc.getDocumentElement();
		
		m_date = XMLUtil.parseAttribute(m_xmlData, XML_ATTR_DATE, -1);
		m_locale = new Locale(XMLUtil.parseAttribute(m_xmlData, XML_ATTR_LOCALE, Locale.ENGLISH.toString()), "");
		m_type = XMLUtil.parseAttribute(m_xmlData, XML_ATTR_TYPE, TERMS_AND_CONDITIONS_TYPE_COMMON_LAW);
		
		m_strId = m_type + "_" + m_locale + "_" + m_date;
		
		m_lastUpdate = System.currentTimeMillis();
		XMLUtil.setAttribute(m_xmlData, XML_ATTR_LAST_UPDATE, m_lastUpdate);
		XMLUtil.setAttribute(m_xmlData, XML_ATTR_ID, m_strId);
		
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
	
	public void importData(TermsAndConditions a_data)
	{
		Document doc = a_data.getDocument();
		
		try
		{
			// find the ServiceOperator object to our T&C
			ServiceOperator op = (ServiceOperator) Database.getInstance(ServiceOperator.class).getEntryById(a_data.getSKI());
			
			if(op == null)
			{
				return;
			}
			
			// create the operator node
			Element operator = doc.createElement(XML_ELEMENT_OPERATOR);
			
			XMLUtil.createChildElementWithValue(operator, XML_ELEMENT_OPERATOR_NAME, op.getOrganization());
			XMLUtil.createChildElementWithValue(operator, XML_ELEMENT_OPERATOR_EMAIL, op.getEMail());
			
			appendChildNodeFromTC(a_data, operator, XML_ELEMENT_OPERATOR_STREET);
			appendChildNodeFromTC(a_data, operator, XML_ELEMENT_OPERATOR_POSTAL_CODE);
			appendChildNodeFromTC(a_data, operator, XML_ELEMENT_OPERATOR_CITY);
			appendChildNodeFromTC(a_data, operator, XML_ELEMENT_OPERATOR_COUNTRY);
			appendChildNodeFromTC(a_data, operator, XML_ELEMENT_OPERATOR_VAT);
			appendChildNodeFromTC(a_data, operator, XML_ELEMENT_OPERATOR_FAX);
			appendChildNodeFromTC(a_data, operator, XML_ELEMENT_OPERATOR_EMAIL);
			
			replaceNode(operator, XML_ELEMENT_OPERATOR);
			
			// replace OperatorCountry
			replaceNode(importNodeFromTC(a_data, XML_ELEMENT_OPERATOR_COUNTRY), XML_ELEMENT_OPERATOR_COUNTRY);
			
			// replace PrivacyPolicyUrl
			replaceNode(doc.getDocumentElement(), XML_ELEMENT_PRIVACY_POLICY_URL);
			
			// replace LegalOpinionsUrl
			replaceNode(doc.getDocumentElement(), XML_ELEMENT_LEGAL_OPINIONS_URL);
			
			// replace OperationalAgreementUrl
			replaceNode(doc.getDocumentElement(), XML_ELEMENT_OPERATIONAL_AGREEMENT_URL);
			
			// replace Location
			replaceNode(importNodeFromTC(a_data, "Location"), "Location");

			// replace Venue
			replaceNode(importNodeFromTC(a_data, "Venue"), "Venue");

			Element date = doc.createElement("Date");
			XMLUtil.setValue(date, a_data.getVersionNumber());
			
			// replace Date
			replaceNode(date, "Date");
			
			// loop through all Paragraph nodes in our import document
			NodeList paragraphs = doc.getElementsByTagName(XML_ELEMENT_PARAGRAPH);
			for(int i = 0; i < paragraphs.getLength(); i++)
			{
				Node importParagraph = XMLUtil.importNode(m_doc, paragraphs.item(i), true);
				String id = XMLUtil.parseAttribute(importParagraph, XML_ATTR_ID, "-1");
				
				// try to find it in our original document
				Node para = findParagraphById(id);
				Node section = null;
				// insert it if the paragraph doesn't exist yet
				if(para == null)
				{
					// invalid id, skip
					if(id.length() < 2)
					{
						continue;
					}
					
					String sectionId = id.substring(0, 1);
					section = findSectionById(sectionId);
					
					// invalid section id, skip
					if(section == null)
					{
						continue;
					}
					
					section.appendChild(importParagraph);
				}
				// replace it otherwise
				else
				{
					section = para.getParentNode();
					section.replaceChild(importParagraph, para);
				}
			}
			
			System.out.println(XMLUtil.toString(m_doc));
		}
		catch(Exception ex)
		{
			ex.printStackTrace();
		}
	}

	private void appendChildNodeFromTC(TermsAndConditions a_tc, Element a_elem, String a_nodeName) 
	{
		Node node = importNodeFromTC(a_tc, a_nodeName);
		if(node != null)
		{
			a_elem.appendChild(node);
		}
	}
	
	private Node importNodeFromTC(TermsAndConditions a_tc, String a_nodeName)
	{
		// look for node
		Node node = XMLUtil.getFirstChildByName(a_tc.getDocument().getDocumentElement(), a_nodeName);
		if(node != null)
		{
			return node;
		}
		else if(!a_tc.getLocale().equals("en"))
		{
			// look in the English version of this T&C
			TermsAndConditions tcDefault = TermsAndConditions.getById("en_" + a_tc.getSKI());
			
			if(tcDefault != null)
			{
				node = XMLUtil.getFirstChildByName(tcDefault.getDocument().getDocumentElement(), a_nodeName);
				if(node != null)
				{
					try
					{
						return XMLUtil.importNode(a_tc.getDocument(), node, true);
					}
					catch (XMLParseException a_e)
					{
						LogHolder.log(LogLevel.EXCEPTION, LogType.PAY, a_e);
						return null;
					}
				}
			}
		}
		
		return null;
	}
	
	public Node findSectionById(String a_id)
	{
		NodeList list = m_doc.getElementsByTagName(XML_ELEMENT_SECTION);
		for(int i = 0; i < list.getLength(); i++)
		{
			Node node = list.item(i);
			if(XMLUtil.parseAttribute(node, XML_ATTR_ID, "-1").equals(a_id))
			{
				return node;
			}
		}
		
		return null;
	}
	
	public Node findParagraphById(String a_id)
	{
		NodeList list = m_doc.getElementsByTagName(XML_ELEMENT_PARAGRAPH);
		for(int i = 0; i < list.getLength(); i++)
		{
			Node node = list.item(i);
			if(XMLUtil.parseAttribute(node, XML_ATTR_ID, "-1").equals(a_id))
			{
				return node;
			}
		}
		
		return null;
	}
	
	public void replaceNode(Node a_src, String a_elementToReplace) throws XMLParseException
	{
		// find the node in the source document
		Node node = XMLUtil.getFirstChildByName(a_src, a_elementToReplace);
		
		if(node == null)
		{
			// try the node itself
			if(a_src.getNodeName() == null)
			{
				return;
			}
			else if(a_src.getNodeName().equals(a_elementToReplace))
			{
				node = a_src;
			}
			else
			{
				return;
			}
		}
		
		// import it in to our document
		node = XMLUtil.importNode(m_doc, node, true);
		
		// replace all nodes in the original document with it
		NodeList list = m_doc.getElementsByTagName(a_elementToReplace);
		for(int i = 0; i < list.getLength(); i++)
		{
			Node parent = list.item(i).getParentNode();
			parent.replaceChild(node.cloneNode(true), list.item(i));
		}
	}
	
	public String transform()
	{
		try
		{		
			File xsltFile = new File("tac.xslt");
			File output = new File("output.html");
			FileOutputStream stream = new FileOutputStream(output);
			
			Source xmlSource = new DOMSource(m_doc);
			
			Source xsltSource = new StreamSource(xsltFile);
			
			TransformerFactory factory = TransformerFactory.newInstance();
			Transformer transformer = factory.newTransformer(xsltSource);
			
			transformer.transform(xmlSource, new StreamResult(stream));
			stream.close();
			
			StringWriter writer = new StringWriter();
			transformer.transform(xmlSource, new StreamResult(writer));
			writer.close();
			
			String s = writer.toString();
			
			// this is needed on some older java versions (mainly 1.5)
			// otherwise breaks will not be displayed correctly
			return anon.util.Util.replaceAll(s, "<br/>", "<br>");
		}
		catch(Exception ex)
		{
			return null;
		}
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
		return m_date;
	}
	
	public Element getXmlStructure()
	{
		return m_xmlData;
	}
	
	public String getPostFile()
	{
		return "/posttcframework";
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
	
	public static TermsAndConditionsFramework getById(String a_id, boolean a_bUpdateFromInfoService)
	{
		// first look if it's in our database
		TermsAndConditionsFramework tc = (TermsAndConditionsFramework) Database.getInstance(TermsAndConditionsFramework.class).getEntryById(a_id);
		
		if(!a_bUpdateFromInfoService || tc != null)
		{
			return tc;
		}
		
		tc = InfoServiceHolder.getInstance().getTCFramework(a_id);
		Database.getInstance(TermsAndConditionsFramework.class).update(tc);
		
		return tc;
	}
}
