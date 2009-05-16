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

import java.io.IOException;
import java.io.StringReader;
import java.util.StringTokenizer;
import java.util.Vector;

import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import anon.terms.TCComponent;
import anon.util.IXMLEncodable;
import anon.util.XMLParseException;
import anon.util.XMLUtil;

public class Paragraph extends TCComponent implements IXMLEncodable
{
	public static String XML_ELEMENT_CONTAINER_NAME = "Section";
	public static String XML_ELEMENT_NAME = "Paragraph";
	
	private Vector elementNodes = null;
	private boolean hasElementNodes = false;
	
	public Paragraph()
	{
		content = new Vector();
		elementNodes = new Vector(3);
		hasElementNodes = false;
	}
	
	public Paragraph(double id)
	{
		this();
		setId(id);
	}
	
	public Paragraph(Node root) throws XMLParseException
	{
		this();
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
			throw new XMLParseException("Attribute "+XML_ATTR_ID+" missing: "+XMLUtil.toString(root));
		}
		setContent(rootElement.getChildNodes());

	}
	
	public void replaceElementNodes(NodeList nodes)
	{
		Element former = null;
		Element substitute = null;
		NodeList elementDeepList = null;
		Element deepElement = null;
		Node deepElementParent = null;
		for (int i = 0; i < nodes.getLength(); i++) 
		{
			substitute = (Element) nodes.item(i);
			for (int j = 0; j < elementNodes.size(); j++) 
			{
				former = (Element) elementNodes.elementAt(j);
				if(former.getTagName().equals(substitute.getTagName()))
				{
					elementNodes.removeElementAt(j);
					elementNodes.insertElementAt(substitute, j);
					int contentIndex = contentNodes().indexOf(former);
					contentNodes().removeElementAt(contentIndex);
					contentNodes().insertElementAt(substitute, contentIndex);
				}
				else
				{
					elementDeepList = former.getElementsByTagName(substitute.getTagName());
					for (int k = 0; k < elementDeepList.getLength(); k++) 
					{
						deepElement = (Element) elementDeepList.item(k);
						//ParentNode cannot be null.
						try
						{
							deepElement.getParentNode().replaceChild(
									XMLUtil.importNode(deepElement.getParentNode().getOwnerDocument(), substitute, true), deepElement);
						}
						catch (XMLParseException a_e)
						{
							LogHolder.log(LogLevel.EMERG, LogType.MISC, a_e);
						}
					}
				}
			}
		}
	}
	
	public boolean hasElementNodes()
	{
		return hasElementNodes;
	}
	
	public void setContent(Object o)
	{	
		NodeList nl = null;
		
		if(o != null)
		{	
			if(!(o instanceof NodeList) )
			{
				if(o instanceof Node[])
				{
					nl = toNodeList((Node[]) o);
				}
				else
				{
					//try to make a NodeList out the object content.
					StringBuffer contentBuffer = new StringBuffer();
					
					contentBuffer.append("<?xml version=\"1.0\"?><temp>");
					contentBuffer.append(o);
					contentBuffer.append("</temp>");	
					try 
					{
						Document tempDoc = XMLUtil.readXMLDocument(new StringReader(contentBuffer.toString()));
						nl = (tempDoc.getDocumentElement() != null) ? tempDoc.getDocumentElement().getChildNodes() : null;
					} 
					catch (IOException e) 
					{
						LogHolder.log(LogLevel.DEBUG, LogType.MISC, "Cannot set content, reason: "+e.getMessage());
						return;
					} 
					catch (XMLParseException e) 
					{
						LogHolder.log(LogLevel.DEBUG, LogType.MISC, "Cannot set content, reason: "+e.getMessage());
						return;
					}
				}
			}
			else
			{
				nl = (NodeList) o;
			}
		}
		
		elementNodes.removeAllElements();
		contentNodes().removeAllElements();
		hasElementNodes = false;
		
		if(nl != null)
		{
			for (int i = 0; i < nl.getLength(); i++) 
			{
				Node n = nl.item(i).cloneNode(true);
				
				if(n.getNodeType() == Node.ELEMENT_NODE)
				{
					elementNodes.addElement(n);
					hasElementNodes = true;
				}
				contentNodes().addElement(n);
			}	
		}
	}
	
	public Object getContent()
	{
		final Node[] contentNodesArray =
			new Node[contentNodes().size()];
		
		for (int i = 0; i < contentNodesArray.length; i++) 
		{
			contentNodesArray[i] = (Node) contentNodes().elementAt(i);
		}
		
		return new NodeList()
		{
			public int getLength() 
			{
				return contentNodesArray.length;
			}

			public Node item(int index) 
			{
				return contentNodesArray[index];
			}
		};
	}
	
	public void setContentBold()
	{
		NodeList contentNodeList = (NodeList) getContent();
		Document tempDoc = XMLUtil.createDocument();
		Node boldElement = null;
		
		elementNodes.removeAllElements();
		contentNodes().removeAllElements();
		
		for (int i = 0; i < contentNodeList.getLength(); i++) 
		{
			if( (contentNodeList.item(i).getNodeType() == Node.ELEMENT_NODE) &&
				((Element) contentNodeList.item(i)).getTagName().toLowerCase().equals("b"))
			{
				//don't enclose an already existing <b>-Tag.
				boldElement = contentNodeList.item(i);
			}
			else if( (contentNodeList.item(i).getNodeType() != Node.TEXT_NODE) ||
					( (contentNodeList.item(i).getNodeValue() != null) &&
					  !(contentNodeList.item(i).getNodeValue().trim().equals(""))) )
			{
				
				boldElement = tempDoc.createElement("b"); 
				boldElement.appendChild(tempDoc.importNode(contentNodeList.item(i), true));
			}
			else
			{
				boldElement = null;
			}
			
			if(boldElement != null)
			{
				contentNodes().addElement(boldElement);
				elementNodes.addElement(boldElement);
			}
		}
		hasElementNodes = (elementNodes.size() > 0);
	}
	
	private Vector contentNodes()
	{
		return (Vector) content;
	}
	
	public boolean hasContent()
	{
		return super.hasContent() && (contentNodes().size() > 0);
	}
	
	public Element toXmlElement(Document ownerDoc) 
	{
		return toXmlElement(ownerDoc, false);
	}
	
	public Element toXmlElement(Document ownerDoc, boolean outputEmpty) 
	{
		if(id < 0 || ((contentNodes().size() == 0) && !outputEmpty) ) return null;
		Element rootElement = ownerDoc.createElement(XML_ELEMENT_NAME);
		rootElement.setAttribute(XML_ATTR_ID, ""+this.id);
		for (int i = 0; i < contentNodes().size(); i++) 
		{
			try
			{
				rootElement.appendChild(XMLUtil.importNode(ownerDoc, (Node) contentNodes().elementAt(i), true));
			}
			catch (XMLParseException a_e)
			{
				LogHolder.log(LogLevel.EMERG, LogType.MISC, a_e);
			}
		}
		return rootElement;
	}
	
	public Object clone()
	{
		Paragraph paragraph = new Paragraph();
		paragraph.setId(id);
		paragraph.setContent(getContent());
		return paragraph;
	}
	
	public String toString()
	{
		StringBuffer buff = new StringBuffer();
		String currentNodeContent = null;
		StringTokenizer contentTokenizer = null;
		for (int i = 0; i < contentNodes().size(); i++) 
		{
			currentNodeContent = XMLUtil.toString((Node) contentNodes().elementAt(i));
			contentTokenizer = new StringTokenizer(currentNodeContent, "\n");
			while (contentTokenizer.hasMoreTokens()) 
			{
				buff.append(contentTokenizer.nextToken().trim());
				buff.append("\n");
			}
		}
		return buff.toString().trim();
	}
	
	public static NodeList toNodeList(Node[] nodeArray)
	{
		final Node[] fNodeArray = nodeArray;
		return new NodeList()
		{
			public int getLength() 
			{
				return fNodeArray.length;
			}

			public Node item(int index) 
			{
				return fNodeArray[index];
			}
		};
	}
}
