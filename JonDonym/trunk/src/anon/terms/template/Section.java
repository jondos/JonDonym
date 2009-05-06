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

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import anon.terms.TCComponent;
import anon.terms.TCComposite;
import anon.util.IXMLEncodable;
import anon.util.XMLParseException;
import anon.util.XMLUtil;

public class Section extends TCComposite implements IXMLEncodable
{
	public static String XML_ELEMENT_CONTAINER_NAME = "Sections";
	public static String XML_ELEMENT_NAME = "Section";
	
	public static String XML_ATTR_NAME = "name";
	
	public Section()
	{
		super();
	}
	
	public Section(double id, Object content)
	{
		super(id, content); 
	}
	
	public Section(Node root) throws XMLParseException
	{
		Element rootElement = null;
		if(root.getNodeType() == Node.DOCUMENT_NODE)
		{
			rootElement = ((Document) root).getDocumentElement();
		}
		else if (root.getNodeType() == Node.ELEMENT_NODE)
		{
			rootElement = (Element) root;
		}
		else
		{
			throw new XMLParseException("Invalid node type.");
		}
		if(!rootElement.getTagName().equals(XML_ELEMENT_NAME))
		{
			throw new XMLParseException("Invalid Tag name: "+rootElement.getTagName());
		}
		this.id = XMLUtil.parseAttribute(rootElement, XML_ATTR_ID, (double) -1);
		if(this.id < 0)
		{
			throw new XMLParseException("Attribute "+XML_ATTR_ID+" of "+XMLUtil.parseAttribute(rootElement, XML_ATTR_NAME, "")+" missing");
		}
		Element paragraphElement = 
			(Element) XMLUtil.getFirstChildByName(rootElement, Paragraph.XML_ELEMENT_NAME);
		setContent(XMLUtil.parseAttribute(rootElement, XML_ATTR_NAME, null));
		while (paragraphElement != null) 
		{
			addTCComponent(new Paragraph(paragraphElement));
			paragraphElement = 
				(Element) XMLUtil.getNextSiblingByName(paragraphElement, Paragraph.XML_ELEMENT_NAME);
		}
	}
	
	public void replaceElementNodes(NodeList nodes)
	{
		TCComponent[] allParagraphs = getTCComponents();
		Paragraph currentParagraph = null;
		for (int i = 0; i < allParagraphs.length; i++) 
		{
			currentParagraph = (Paragraph) allParagraphs[i];
			if(currentParagraph.hasElementNodes())
			{
				currentParagraph.replaceElementNodes(nodes);
			}
		}
	}
	
	/*public boolean hasContent()
	{
		return ((getContent() != null) ? !getContent().equals("") : false) || (getTCComponentCount() > 0);
	}*/
	
	public Element toXmlElement(Document ownerDoc)
	{
		return toXmlElement(ownerDoc, false);
	}
	
	public Element toXmlElement(Document ownerDoc, boolean outputEmpty) 
	{
		if( (getId() < 0) || (!hasContent() && !outputEmpty) ) 
		{
			return null;
		}
		Element rootElement = ownerDoc.createElement(XML_ELEMENT_NAME);
		if(getContent() != null)
		{
			rootElement.setAttribute(XML_ATTR_NAME, getContent().toString());
		}
		rootElement.setAttribute(XML_ATTR_ID, ""+getId());
		
		TCComponent[] allParagraphs = getTCComponents();
		Paragraph currentParagraph = null;
		Element currentParagraphElement = null;
		for (int i = 0; i < allParagraphs.length; i++) 
		{
			currentParagraph = (Paragraph) allParagraphs[i];
			currentParagraphElement = currentParagraph.toXmlElement(ownerDoc, outputEmpty);
			if(currentParagraphElement != null)
			{
				rootElement.appendChild(currentParagraphElement);
			}
		}
		return rootElement;
	}
}
