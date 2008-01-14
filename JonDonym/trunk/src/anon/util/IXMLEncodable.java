/*
 Copyright (c) 2000, The JAP-Team
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
package anon.util;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Objects of this type can be transformed into xml.
 * Classes that implement this interface should also define one or two constants that
 * give further description of about the constructed element. As they are static, these constants
 * cannot be part of this interface, but they are presented here in commented form.
 * PLEASE DO NOT DELETE THEM!
 * @author Wendolsky
 */
public interface IXMLEncodable
{
	/**
	 * The version attribute used for a lot of tags.
	 */
	public static final String XML_ATTR_VERSION = "version";

	/**
	 * The id attribute used for a lot of tags.
	 */
	public static final String XML_ATTR_ID = "id";

	public static final String FIELD_XML_ELEMENT_NAME = "XML_ELEMENT_NAME";
	public static final String FIELD_XML_ELEMENT_CONTAINER_NAME = "XML_ELEMENT_CONTAINER_NAME";

	/**
	 * The name of the XML element constructed by this class. Do not delete this comment!
	 */
	// public static final String XML_ELEMENT_NAME = "ElementName";

	/**
	 * The name of the XML container elemene for elements constructed by this class.
	 * Only needed if this element needs a special container element and cannot exist without
	 * it in an XML structure. Do not delete this comment!
     */
	// public static final String XML_ELEMENT_CONTAINER_NAME = "ElementContainerName";


	/**
	 * Return an element that can be appended to the document. This
	 * Method must not change the document in any way!
	 * @param a_doc a document
	 * @return the interface as xml element
   */
	public Element toXmlElement(Document a_doc);
}
