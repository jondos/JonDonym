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
package anon.terms.template;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.text.DateFormat;
import java.util.Date;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Vector;

import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import anon.client.TrustModel.PremiumAttribute;
import anon.crypto.MultiCertPath;
import anon.crypto.SignatureCreator;
import anon.crypto.SignatureVerifier;
import anon.crypto.XMLSignature;
import anon.infoservice.AbstractDistributableCertifiedDatabaseEntry;
import anon.infoservice.Database;
import anon.infoservice.InfoServiceHolder;
import anon.infoservice.OperatorAddress;
import anon.infoservice.ServiceOperator;
import anon.terms.TCComponent;
import anon.terms.TCComposite;
import anon.terms.TermsAndConditions;
import anon.terms.TermsAndConditionsTranslation;
import anon.util.XMLParseException;
import anon.util.XMLUtil;

public class TermsAndConditionsTemplate extends AbstractDistributableCertifiedDatabaseEntry
{
	private static final String XML_ATTR_DATE = "date";
	private static final String XML_ATTR_LOCALE = "locale";
	private static final String XML_ATTR_NAME = "name";
	private static final String XML_ATTR_TYPE = "type";
	
	private static final String XML_ELEMENT_OPERATOR_COUNTRY = "OperatorCountry";
	private static final String XML_ELEMENT_SIGNATURE = "Sig";
	private static final String XML_ELEMENT_DATE = "Date";
	
	public static final String INFOSERVICE_PATH = "/tctemplate/";
	public static final String INFOSERVICE_CONTAINER_PATH = "/tctemplates";
	public static final String INFOSERVICE_SERIALS_PATH = "/tctemplateserials";
	
	public final static String[] REPLACEMENT_ELEMENT_NAMES = 
		new String[]
		{
			TermsAndConditionsTranslation.XML_ELEMENT_PRIVACY_POLICY, 
			TermsAndConditionsTranslation.XML_ELEMENT_LEGAL_OPINIONS, 
			TermsAndConditionsTranslation.XML_ELEMENT_OPERATIONAL_AGREEMENT,
        };
	
	private static final String XSLT_PATH = "res/tac.xslt";
	
	public static String TERMS_AND_CONDITIONS_TYPE_COMMON_LAW = "CommonLaw";
	public static String TERMS_AND_CONDITIONS_TYPE_GERMAN_LAW = "GermanLaw";
	public static String TERMS_AND_CONDITIONS_TYPE_GENERAL_LAW = "GeneralLaw";
	
	public static String XML_ELEMENT_CONTAINER_NAME = "TermsAndConditionsTemplates";
	public static String XML_ELEMENT_NAME = "TermsAndConditionsTemplate";
	
	public String m_strId = null;
	public Locale m_locale = null;
	public String m_type = null;
	
	public long m_lastUpdate;
	public long m_date;
	
	public Document signedDocument = null;
	
	private XMLSignature m_signature = null;

	private MultiCertPath m_certPath = null;
	
	private String name = "";
	private Preamble preamble = null;
	private TCComposite sections = new TCComposite();
	
	public TermsAndConditionsTemplate(Node rootNode) throws XMLParseException
	{
		super(Long.MAX_VALUE);
		Element templateRoot = null;
		if(rootNode.getNodeType() == Node.DOCUMENT_NODE)
		{
			templateRoot = ((Document) rootNode).getDocumentElement();
		}
		else if(rootNode.getNodeType() == Node.ELEMENT_NODE)
		{
			templateRoot = (Element) rootNode;
		}
		else
		{
			throw new XMLParseException("Invalid node type");
		}
		
		name = XMLUtil.parseAttribute(templateRoot, XML_ATTR_NAME, "");
		m_date = XMLUtil.parseAttribute(templateRoot, XML_ATTR_DATE, -1);
		m_locale = new Locale(XMLUtil.parseAttribute(templateRoot, XML_ATTR_LOCALE, Locale.ENGLISH.toString()), "");
		m_type = XMLUtil.parseAttribute(templateRoot, XML_ATTR_TYPE, TERMS_AND_CONDITIONS_TYPE_COMMON_LAW);
		m_strId = m_type + "_" + m_locale + "_" + m_date;
		m_lastUpdate = System.currentTimeMillis();
		m_signature = SignatureVerifier.getInstance().getVerifiedXml(templateRoot,
			SignatureVerifier.DOCUMENT_CLASS_INFOSERVICE);
		
		if (m_signature != null)
		{
			m_certPath = m_signature.getMultiCertPath();
			signedDocument = 
				(rootNode.getNodeType() == Node.DOCUMENT_NODE) ? (Document) rootNode : templateRoot.getOwnerDocument();	
		}
		
		// get the defined sections of this template
		NodeList nl = templateRoot.getElementsByTagName(Section.XML_ELEMENT_NAME);
		for (int i = 0; i < nl.getLength(); i++) 
		{
			sections.addTCComponent(new Section(nl.item(i)));
		}
		Node preambleNode = XMLUtil.getFirstChildByName(templateRoot, Preamble.XML_ELEMENT_NAME);
		// get the preamble of this template
		preamble = (preambleNode != null) ? new Preamble(preambleNode) : new Preamble();
	}
	
	// creation from file
	public TermsAndConditionsTemplate(File a_file) throws XMLParseException, IOException
	{
		this(XMLUtil.readXMLDocument(a_file));
	}
	
	public Document createTCDocument(TermsAndConditionsTranslation tcTranslation)
	{
		Document tcDocument = XMLUtil.createDocument();
		Element tcRootElement = tcDocument.createElement(XML_ELEMENT_NAME);
		
		Element cityElement = tcDocument.createElement(OperatorAddress.NODE_NAME_CITY);
		Element venueElement = tcDocument.createElement(OperatorAddress.NODE_NAME_VENUE);
		Element dateElement = tcDocument.createElement(XML_ELEMENT_DATE);
		
		ServiceOperator operator = null;
		OperatorAddress address = null;
		
		Element signatureElement = tcDocument.createElement(XML_ELEMENT_SIGNATURE);
		signatureElement.appendChild(cityElement);
		signatureElement.appendChild(dateElement);
		
		TCComposite copiedSections = getSections();
		TCComponent[] allSections = null;
		
		tcRootElement.setAttribute(XML_ATTR_NAME, name);
		tcDocument.appendChild(tcRootElement);
		
		if(tcTranslation != null)
		{
			operator = tcTranslation.getOperator();
			address = tcTranslation.getOperatorAddress();
			
			Locale operatorLocale = new Locale(operator.getCountryCode(), operator.getCountryCode());
			Locale tcLocale = new Locale(tcTranslation.getLocale(), "", "");
			
			Element operatorElement = operator.toXMLElement(tcDocument, address, false);
			Element operatorCountryElement = tcDocument.createElement(XML_ELEMENT_OPERATOR_COUNTRY);
			
			XMLUtil.setValue(cityElement, 
					(address != null) ? tcTranslation.getOperatorAddress().getCity() : "");
			XMLUtil.setValue(venueElement,
					(address != null) ? tcTranslation.getOperatorAddress().getVenue() : "");
			XMLUtil.setValue(dateElement,
					DateFormat.getDateInstance(DateFormat.MEDIUM, tcLocale).format(tcTranslation.getDate()));
			XMLUtil.setValue(operatorCountryElement, operatorLocale.getDisplayCountry(tcLocale));
			
			//add/replace the customized sections/paragraphs
			TCComponent[] translationSections = tcTranslation.getSections().getTCComponents();
			Section currentTranslationSection = null;
			Section currentSection = null;
			TCComponent[] currentParagraphs = null;
			for (int i = 0; i < translationSections.length; i++) 
			{
				currentTranslationSection = (Section) translationSections[i];
				currentSection = (Section) copiedSections.getTCComponent(currentTranslationSection.getId());
				//if section has no content simply replace the section
				if( !currentTranslationSection.hasContent() || 
					(currentSection == null) )
				{
					copiedSections.addTCComponent(translationSections[i]);
				}
				else
				{
					if(currentTranslationSection.getContent() != null)
					{
						currentSection.setContent(currentTranslationSection.getContent());
					}
					currentParagraphs = currentTranslationSection.getTCComponents();
					//... otherwise descend into paragraphs level to do the replacement
					for (int j = 0; j < currentParagraphs.length; j++)
					{
						currentSection.addTCComponent(currentParagraphs[j]);
					}
				}
			}
	
			//prepare List with all nodes to be replaced in the sections and paragraphs.
			String[] replaceValues = new String[]
	        {
					tcTranslation.getPrivacyPolicyUrl(),		
					tcTranslation.getLegalOpinionsUrl(),
					tcTranslation.getOperationalAgreementUrl(),
	        };
			
			final Vector replaceNodes = new Vector();
			Element currentReplaceElement = null;
			for (int i = 0; i < replaceValues.length; i++) 
			{
				currentReplaceElement = tcDocument.createElement(REPLACEMENT_ELEMENT_NAMES[i]);
				currentReplaceElement.appendChild(tcDocument.createTextNode(replaceValues[i]));
				replaceNodes.addElement(currentReplaceElement);
			}
			replaceNodes.addElement(operatorElement);
			replaceNodes.addElement(operatorCountryElement);
			replaceNodes.addElement(venueElement);
			
			NodeList replaceNodeList = new NodeList()
			{
				public int getLength() 
				{
					return replaceNodes.size();
				}
	
				public Node item(int index) 
				{
					return (Node) replaceNodes.elementAt(index);
				}	
			};
	
			//replace DOMElements in the sections and paragraphs.
			allSections = copiedSections.getTCComponents();
			for (int i = 0; i < allSections.length; i++) 
			{
				((Section) allSections[i]).replaceElementNodes(replaceNodeList);
			}
		}
		//create DOMDocument for output
		preamble.setOperator(operator);
		preamble.setOperatorAddress(address);
		tcRootElement.appendChild(preamble.toXmlElement(tcDocument));
		
		Element sectionsElement = tcDocument.createElement(Section.XML_ELEMENT_CONTAINER_NAME);
		
		allSections = copiedSections.getTCComponents();
		Element sectionElement = null;
		for (int i = 0; i < allSections.length; i++) 
		{
			sectionElement = ((Section) allSections[i]).toXmlElement(tcDocument);
			if(sectionElement != null)
			{
				sectionsElement.appendChild(sectionElement);
			}
		}
		tcRootElement.appendChild(sectionsElement);
		tcRootElement.appendChild(signatureElement);
		return tcDocument;
	}
	
	public String transform(TermsAndConditionsTranslation translation)
	{
		try 
		{
			StringWriter writer = new StringWriter();
			transform(writer, translation);
			writer.close();
			String s = writer.toString();
			
			// otherwise inserted elements such as Venue or OperatorCountry 
			// will have additional whitespace after them. 
			// TODO: find a better way to deal with this
			//s = anon.util.Util.replaceAll(s, "\n", "");
			//s = anon.util.Util.replaceAll(s, "\r", "");
			
			// this is needed on some older java versions (mainly 1.5)
			// otherwise br's will not be displayed correctly
			return anon.util.Util.replaceAll(s, "<br/>", "<br>");
		} 
		catch (IOException e) 
		{
			LogHolder.log(LogLevel.ERR, LogType.MISC, "IOException caught while transforming terms and conditions.");
			return null;
		} 
		catch (TransformerException e)
		{
			LogHolder.log(LogLevel.ERR, LogType.MISC, "Could not transform terms and conditions.");
			e.printStackTrace();
			return null;
		}
	}
	
	public void transform(Writer writer, TermsAndConditionsTranslation translation) throws IOException, TransformerException
	{
		Source xmlSource = new DOMSource(createTCDocument(translation));
		Source xsltSource = new StreamSource(this.getClass().getResourceAsStream(XSLT_PATH));
		
		TransformerFactory factory = TransformerFactory.newInstance();
		Transformer transformer = factory.newTransformer(xsltSource);
	
		transformer.transform(xmlSource, new StreamResult(writer));
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
	
	public String getPostFile()
	{
		//TODO: rename
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
	
	public TCComposite getSections()
	{
		return (TCComposite) sections.clone();
	}
	
	public static synchronized void store(Element root)
	{
		Element current = (Element) XMLUtil.getFirstChildByName(root, XML_ELEMENT_NAME);
		while(current != null)
		{
			try
			{
				Database.getInstance(TermsAndConditionsTemplate.class).update(
						new TermsAndConditionsTemplate(current));
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
		final Enumeration e = Database.getInstance(TermsAndConditionsTemplate.class).getEntryList().elements();
		return new Enumeration()
		{
			public boolean hasMoreElements() 
			{
				return e.hasMoreElements();
			}

			public Object nextElement() 
			{
				return ((TermsAndConditionsTemplate)e.nextElement()).getId();
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
				TermsAndConditionsTemplate tac = new TermsAndConditionsTemplate(file);
				
				Database.getInstance(TermsAndConditionsTemplate.class).update(tac);
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
	
	public static TermsAndConditionsTemplate getById(String a_id, boolean a_bUpdateFromInfoService)
	{
		// first look if it's in our database
		TermsAndConditionsTemplate tc = (TermsAndConditionsTemplate) Database.getInstance(TermsAndConditionsTemplate.class).getEntryById(a_id);
		
		if(!a_bUpdateFromInfoService || tc != null)
		{
			return tc;
		}
		
		tc = InfoServiceHolder.getInstance().getTCFramework(a_id);
		Database.getInstance(TermsAndConditionsTemplate.class).update(tc);
		
		return tc;
	}
	
	public boolean equals(Object a_object)
	{
		boolean objectEquals = false;
		if (a_object != null)
		{
			if (a_object instanceof TermsAndConditionsTemplate)
			{
				objectEquals = this.getId().equals( ( (TermsAndConditionsTemplate) a_object).getId());
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
	
	public Document getDocument() 
	{
		return (signedDocument != null) ? signedDocument : createTCDocument(null);
	}

	public Document getSignedDocument()
	{
		return signedDocument;
	}
	
	public Element getXmlStructure() 
	{
		return getDocument().getDocumentElement();
	}
	
}
