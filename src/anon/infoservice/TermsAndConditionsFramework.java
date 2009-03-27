package anon.infoservice;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.text.DateFormat;
import java.util.Date;
import java.util.Enumeration;
import java.util.Locale;

import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.Transformer;

import anon.crypto.MultiCertPath;
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
	private static final String XML_ELEMENT_VENUE = "Venue";

	//private static final String XML_ELEMENT_CI = "Location";

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
	// do not delete: dynamic reference!
	private static final String XML_ELEMENT_OPERATOR_COUNTRY = "OperatorCountry";
	private static final String XML_ELEMENT_OPERATOR_CITY = "City";
	
	/*private static final String XML_ELEMENT_OPERATOR_NAME = "Name";
	private static final String XML_ELEMENT_OPERATOR_STREET = "Street";
	private static final String XML_ELEMENT_OPERATOR_POSTAL_CODE = "PostalCode";
	
	private static final String XML_ELEMENT_OPERATOR_VAT = "VAT";
	private static final String XML_ELEMENT_OPERATOR_FAX = "Fax";
	private static final String XML_ELEMENT_OPERATOR_EMAIL = "eMail";*/
	
	private static final String XSLT_PATH = "res/tac.xslt";
	
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
	
	public Document m_docWorkingCopy;
	public Element m_xmlData;

	private XMLSignature m_signature = null;

	private MultiCertPath m_certPath = null;
	
	public TermsAndConditionsFramework(Element a_elem) throws XMLParseException
	{
		this(a_elem, true);
	}
	
	// creation from xml structure
	public TermsAndConditionsFramework(Element a_elem, boolean a_bJAPContext) throws XMLParseException
	{
		super(a_bJAPContext ? Long.MAX_VALUE : System.currentTimeMillis() + TERMS_AND_CONDITIONS_TTL);
		
		m_xmlData = a_elem;
		
		// create a working copy of this framework
		m_docWorkingCopy = XMLUtil.createDocument();
		Node node = XMLUtil.importNode(m_docWorkingCopy, m_xmlData, true);
		m_docWorkingCopy.appendChild(node);
		
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
			m_certPath = m_signature.getMultiCertPath();
		}
	}
	
	// infoservice-creation from file
	public TermsAndConditionsFramework(File a_file) throws XMLParseException, IOException
	{
		super(System.currentTimeMillis() + TERMS_AND_CONDITIONS_TTL);
		
		Document doc = XMLUtil.readXMLDocument(a_file);
		
		m_xmlData = doc.getDocumentElement();
		
		// create a working copy of this framework
		m_docWorkingCopy = XMLUtil.createDocument();
		Node node = XMLUtil.importNode(m_docWorkingCopy, m_xmlData, true);
		m_docWorkingCopy.appendChild(node);
		
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
			m_certPath = m_signature.getMultiCertPath();
		}
	}
	
	public void importData(	TermsAndConditionsTranslation tcTranslation )
	{
		
		//Document doc = a_data.getDocument();
		try
		{
			// find the ServiceOperator object to our T&C
			ServiceOperator op = tcTranslation.getOperator();
			
			if(op == null)
			{
				//Must never happen!
				throw new NullPointerException("The operator of a tc translation cannot be null. This must never happen. Please report!");
			}
			
			// create the operator node
			Element tcTranslationElement = tcTranslation.getTranslationElement();
			if(tcTranslationElement == null)
			{
				throw new XMLParseException("Translation node must not be null. Mix violates T&C protocol.");
			}
			
			Element operator = operator = op.toXMLElement(XMLUtil.createDocument(), tcTranslation.getOperatorAddress(), false);
			if(operator == null)
			{
				throw new XMLParseException("Operator must not be null.");
			}
			
			
			// get country from country code
			Locale loc = new Locale(op.getCountryCode(), op.getCountryCode());
			Locale tcLoc = new Locale(tcTranslation.getLocale(), "", "");
			
			Element country = m_docWorkingCopy.createElement(XML_ELEMENT_OPERATOR_COUNTRY);
			XMLUtil.setValue(country, loc.getDisplayCountry(tcLoc));
			//operator.appendChild(country);
			
			//appendChildNodeFromTC(a_data, operator, XML_ELEMENT_OPERATOR_STREET);
			//appendChildNodeFromTC(a_data, operator, XML_ELEMENT_OPERATOR_POSTAL_CODE);
			//appendChildNodeFromTC(a_data, operator, XML_ELEMENT_OPERATOR_CITY);
			//appendChildNodeFromTC(a_data, operator, XML_ELEMENT_OPERATOR_VAT);
			//appendChildNodeFromTC(a_data, operator, XML_ELEMENT_OPERATOR_FAX);
			//appendChildNodeFromTC(a_data, operator, XML_ELEMENT_OPERATOR_EMAIL); 
			
			replaceNode(operator, XML_ELEMENT_OPERATOR);
			Element replacedOpNode = (Element)
				XMLUtil.getFirstChildByNameUsingDeepSearch(m_docWorkingCopy.getDocumentElement(), XML_ELEMENT_OPERATOR);
			//XMLUtil.createChildElementWithValue(replacedOpNode, XML_ELEMENT_OPERATOR_NAME, op.getOrganization());
			//XMLUtil.createChildElementWithValue(replacedOpNode, XML_ELEMENT_OPERATOR_EMAIL, op.getEMail());
			
			replaceNode(country, XML_ELEMENT_OPERATOR_COUNTRY);
			
			// replace PrivacyPolicyUrl
			//replaceNodeFromTC(tcTranslationElement, XML_ELEMENT_PRIVACY_POLICY_URL);
			
			String[] replaceElements = new String[]
			{
					XML_ELEMENT_PRIVACY_POLICY_URL, 
					XML_ELEMENT_LEGAL_OPINIONS_URL, 
					XML_ELEMENT_OPERATIONAL_AGREEMENT_URL,
					XML_ELEMENT_OPERATOR_CITY,
					XML_ELEMENT_VENUE,
					"Date"
			};
			
			String[] replaceValues = new String[]
            {
					tcTranslation.getPrivacyPolicyUrl(),
					tcTranslation.getLegalOpinionsUrl(),
					tcTranslation.getOperationalAgreementUrl(),
					tcTranslation.getOperatorAddress().getCity(),
					tcTranslation.getOperatorAddress().getVenue(),
					DateFormat.getDateInstance(DateFormat.MEDIUM, tcLoc).format(tcTranslation.getDate())
            };
			
			Element currentUrlElement = null;
			NodeList currentNl = null; 
			for (int i = 0; i < replaceValues.length; i++) 
			{
				currentNl = m_docWorkingCopy.getElementsByTagName(replaceElements[i]);
				for(int j = 0; j < currentNl.getLength(); j++)
				{
					currentUrlElement =
						(Element) currentNl.item(j);
					if( (currentUrlElement != null) && 
						(replaceValues[i] != null) )
					{
						//a workaround for the setTextContent which replaces an already existing TextNode.
						if(currentUrlElement.hasChildNodes())
						{
							NodeList nl = currentUrlElement.getChildNodes();
							for (int k = 0; k < nl.getLength(); k++) 
							{
								currentUrlElement.removeChild(nl.item(k));
							}
						}
						XMLUtil.setValue(currentUrlElement, replaceValues[i]);
					}
				}
			}
			// replace LegalOpinionsUrl
			//replaceNodeFromTC(tcTranslationElement, XML_ELEMENT_LEGAL_OPINIONS_URL);
			
			// replace OperationalAgreementUrl
			//replaceNodeFromTC(tcTranslationElement, XML_ELEMENT_OPERATIONAL_AGREEMENT_URL);
			
			// replace Location
			//replaceNodeFromTC(operator, XML_ELEMENT_OPERATOR_CITY);
			
			// replace Venue
			//replaceNodeFromTC(operator, XML_ELEMENT_VENUE);
			
			// ExtendedOperatorCountry
			//replaceNodeFromTC(tcTranslationElement, "ExtendedOperatorCountry");
			
			// loop through all Paragraph nodes in our import document
			NodeList paragraphs = tcTranslation.getTranslationElement().getElementsByTagName(XML_ELEMENT_PARAGRAPH);
			for(int i = 0; i < paragraphs.getLength(); i++)
			{
				Node importParagraph = XMLUtil.importNode(m_docWorkingCopy, paragraphs.item(i), true);
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
		}
		catch(Exception ex)
		{
			ex.printStackTrace();
		}
	}

	/*private void appendChildNodeFromTC(TermsAndConditions a_tc, Element a_elem, String a_nodeName) 
	{
		Node node = importNodeFromTC(a_tc, a_nodeName);
		if(node != null)
		{
			a_elem.appendChild(node);
		}
	}*/
	
	private void replaceNodeFromTC(Element tcTranslationElement, String a_nodeName) throws XMLParseException
	{
		Node node = importNodeFromTC(tcTranslationElement, a_nodeName);
		if(node != null)
		{
			replaceNode(node, a_nodeName);
		}
	}
	
	private Node importNodeFromTC(Element tcTranslationElement, String a_nodeName)
	{
		// look for node
		Node node = XMLUtil.getFirstChildByNameUsingDeepSearch(tcTranslationElement, a_nodeName);
		if(node != null)
		{
			return node;
		}
		
		return null;
	}
	
	private Node findSectionById(String a_id)
	{
		NodeList list = m_docWorkingCopy.getElementsByTagName(XML_ELEMENT_SECTION);
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
	
	private Node findParagraphById(String a_id)
	{
		NodeList list = m_docWorkingCopy.getElementsByTagName(XML_ELEMENT_PARAGRAPH);
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
	
	private void replaceNode(Node a_src, String a_elementToReplace) throws XMLParseException
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
		
		// import it in to our document only if it is not already the owner
		Document srcOwnerDoc = node.getOwnerDocument();
		if((srcOwnerDoc == null) || !m_docWorkingCopy.equals(srcOwnerDoc) )
		{
			node = XMLUtil.importNode(m_docWorkingCopy, node, true);
		}
		// replace all nodes in the original document with it
		NodeList list = m_docWorkingCopy.getElementsByTagName(a_elementToReplace);
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
			//File xsltFile = new File("tac.xslt");
			//File output = new File("output.html");
			//FileOutputStream stream = new FileOutputStream(output);
			
			Source xmlSource = new DOMSource(m_docWorkingCopy);
			
			Source xsltSource = new StreamSource(this.getClass().getResourceAsStream(XSLT_PATH));
			
			TransformerFactory factory = TransformerFactory.newInstance();
			Transformer transformer = factory.newTransformer(xsltSource);
			
			//transformer.transform(xmlSource, new StreamResult(stream));
			//stream.close();
			
			StringWriter writer = new StringWriter();
			transformer.transform(xmlSource, new StreamResult(writer));
			writer.close();
			
			String s = writer.toString();
			
			// otherwise inserted elements such as Venue or OperatorCountry 
			// will have additional whitespace after them. 
			// TODO: find a better way to deal with this
			s = anon.util.Util.replaceAll(s, "\n", "");
			s = anon.util.Util.replaceAll(s, "\r", "");
			
			// this is needed on some older java versions (mainly 1.5)
			// otherwise br's will not be displayed correctly
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
			return m_certPath.isValid(new Date());
		}
		return false;
	}
	
	public static synchronized void store(Element root)
	{
		Element current = (Element) XMLUtil.getFirstChildByName(root, XML_ELEMENT_NAME);
		while(current != null)
		{
			try
			{
				Database.getInstance(TermsAndConditionsFramework.class).update(
						new TermsAndConditionsFramework(current));
				current = (Element) XMLUtil.getNextSiblingByName(current, XML_ELEMENT_NAME);
			}
			catch(XMLParseException xpe)
			{
				LogHolder.log(LogLevel.ERR, LogType.MISC, 
					"one tc templates could not be stored in the DB");
			}
		}
	}
	
	public static synchronized Enumeration getAllStoredRefIDs()
	{
		final Enumeration e = Database.getInstance(TermsAndConditionsFramework.class).getEntryList().elements();
		return new Enumeration()
		{
			public boolean hasMoreElements() 
			{
				return e.hasMoreElements();
			}

			public Object nextElement() 
			{
				return ((TermsAndConditionsFramework)e.nextElement()).getId();
			}
			
		};
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
	
	public boolean equals(Object a_object)
	{
		boolean objectEquals = false;
		if (a_object != null)
		{
			if (a_object instanceof TermsAndConditionsFramework)
			{
				objectEquals = this.getId().equals( ( (TermsAndConditionsFramework) a_object).getId());
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
		// TODO Auto-generated method stub
		return m_certPath;
	}
}
