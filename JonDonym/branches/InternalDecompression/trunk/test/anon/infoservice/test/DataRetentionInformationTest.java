/*
 Copyright (c) 2009, The JAP-Team, JonDos GmbH
 All rights reserved.
 Redistribution and use in source and binary forms, with or without modification,
 are permitted provided that the following conditions are met:

  - Redistributions of source code must retain the above copyright notice,
 this list of conditions and the following disclaimer.

  - Redistributions in binary form must reproduce the above copyright notice,
 this list of conditions and the following disclaimer in the documentation and/or
 other materials provided with the distribution.

  - Neither the name of the University of Technology Dresden, Germany, nor the name of
 the JonDos GmbH, nor the names of their contributors may be used to endorse or
 promote products derived from this software without specific prior written permission.


 THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS ``AS IS'' AND ANY EXPRESS
 OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY
 AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE REGENTS OR CONTRIBUTORS
 BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA,
 OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER
 IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
 OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE
 */
package anon.infoservice.test;

/**
 * These are the tests for the class XMLUtil.
 */
import java.util.Vector;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import junitx.framework.extension.XtendedPrivateTestCase;

import anon.util.XMLDuration;
import anon.util.XMLParseException;
import anon.util.XMLUtil;
import anon.infoservice.DataRetentionInformation;

/**
 * @author Rolf Wendolsky
 */
public class DataRetentionInformationTest extends XtendedPrivateTestCase
{
	private DataRetentionInformation m_privateDrInfo;
	
	public DataRetentionInformationTest(String a_strName)
	{
		super(a_strName);
	}
	protected void setUp() throws Exception
	{
		Document doc = XMLUtil.createDocument();
		Element elem;
		doc.appendChild(doc.createElement(DataRetentionInformation.XML_ELEMENT_NAME));
		elem = doc.createElement(DataRetentionInformation.XML_ELEMENT_RETENTION_PERIOD);
		XMLUtil.setValue(elem, "P1M"); // value does not matter, but must not be empty
		doc.getDocumentElement().appendChild(elem);
		
		m_privateDrInfo = new DataRetentionInformation(doc.getDocumentElement());		
	}
	


	public void testCreation() throws Exception
	{
		DataRetentionInformation drInfo;
		Document doc;
		Element elem;
		
		doc = XMLUtil.createDocument();
		doc.appendChild(doc.createElement(DataRetentionInformation.XML_ELEMENT_NAME));
		elem = doc.createElement(DataRetentionInformation.XML_ELEMENT_RETENTION_PERIOD);
		XMLUtil.setValue(elem, "P5M"); // value does not matter, but must not be empty
		doc.getDocumentElement().appendChild(elem);
		
		drInfo = new DataRetentionInformation(doc.getDocumentElement());	
	}
	

	public void testCascadeInfo() throws Exception
	{
		DataRetentionInformation drInfo;
		
		drInfo =  getCasdadeDataRetentionInformation(new Vector());
		
	}
	
	private DataRetentionInformation getCasdadeDataRetentionInformation(Vector a_vecDRInfo) throws Exception
	{
		return (DataRetentionInformation)invoke(m_privateDrInfo, "getCascadeDataRetentionInformation", 
				new Object[]{a_vecDRInfo});
	}
	


}
