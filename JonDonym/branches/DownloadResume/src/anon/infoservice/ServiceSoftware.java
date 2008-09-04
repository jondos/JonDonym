/*
 Copyright (c) 2000 - 2005, The JAP-Team
 All rights reserved.
 Redistribution and use in source and binary forms, with or without modification,
 are permitted provided that the following conditions are met:

  - Redistributions of source code must retain the above copyright notice,
 this list of conditions and the following disclaimer.

  - Redistributions in binary form must reproduce the above copyright notice,
 this list of conditions and the following disclaimer in the documentation and/or
 other materials provided with the distribution.

  - Neither the name of the University of Technology Dresden, Germany nor the names of its contributors
 may be used to endorse or promote products derived from this software without specific
 prior written permission.


 THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS ``AS IS'' AND ANY EXPRESS
 OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY
 AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE REGENTS OR CONTRIBUTORS
 BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA,
 OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER
 IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
 OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE
 */

/* Hint: This file may be only a copy of the original file which is always in the JAP source tree!
 * If you change something - do not forget to add the changes also to the JAP source tree!
 */

package anon.infoservice;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.Element;

import anon.util.IXMLEncodable;
import anon.util.XMLParseException;
import anon.util.XMLUtil;

/**
 * Holds the information about the used software of a service.
 */
public class ServiceSoftware implements IXMLEncodable {

  /**
   * This is the version of the used software.
   */
  private String m_strVersion;


  /**
   * Returns the name of the XML root element used by this class.
   *
   * @return The name of the XML root element used by this class ('Software').
   */
  public static String getXmlElementName() {
    return "Software";
  }


  /**
   * Creates a new ServiceSoftware from XML description.
   *
   * @param a_element The root node of the XML structure of this JAPMinVersion ('Software' node).
   *
   * @exception XMLParseException if an error occurs while parsing the xml structure
   */
  public ServiceSoftware(Node a_element) throws XMLParseException {
    /* get the version */
    m_strVersion = XMLUtil.parseValue(XMLUtil.getFirstChildByName(a_element, "Version"), null);
    if (m_strVersion == null) {
      throw new XMLParseException("Version");
    }
  }

  /**
   * Creates a new ServiceSoftware from the version information.
   *
   * @param a_strVersion The software version for this service.
   */
  public ServiceSoftware(String a_strVersion) {
    m_strVersion = a_strVersion;
  }


  /**
   * Creates an XML node without signature for this ServiceSoftware.
   *
   * @param doc The XML document, which is the environment for the created XML node.
   *
   * @return The Software XML node.
   */
  public Element toXmlElement(Document doc) {
    Element softwareNode = doc.createElement(getXmlElementName());
    /* Create the child of Software (Version) */
    Element versionNode = doc.createElement("Version");
    XMLUtil.setValue(versionNode, m_strVersion);
    softwareNode.appendChild(versionNode);
    return softwareNode;
  }

  /**
   * Returns the version of the used software.
   *
   * @return The version of the used software.
   */
  public String getVersion() {
    return m_strVersion;
  }

}
