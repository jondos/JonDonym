/*
 Copyright (c) 2000 - 2004, The JAP-Team
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

/**
 * This exception is thrown if an error occurs while parsing an XML structure.
 * @author Wendolsky
 */
public class XMLParseException extends Exception
{
	/**
	 * A constant that means that the document to parse has a wrong root tag.
	 */
	public static final String ROOT_TAG = "##__root__##";

	/**
	 * A constant that means that a node to parse was null.
	 */
	public static final String NODE_NULL_TAG = "##__null__##";

	/**
	 * Creates a new exception.
	 * @param a_strTagName the name of the tag where the exception occured
	 * @param a_strMessage an additional message for a detailed description of this exception
	 */
	public XMLParseException(String a_strTagName, String a_strMessage)
	{
		super(parseTagName(a_strTagName) + getMessage(a_strMessage));
	}

	/**
	 * Creates a new exception.
	 * @param a_strTagName the name of the tag where the exception occured
	 */
	public XMLParseException(String a_strTagName)
	{
		this(a_strTagName, null);
	}

	private static String getMessage(String a_strMessage)
	{
		if (a_strMessage == null)
		{
			return "";
		}

		return a_strMessage;
	}

	/**
	 * Creates an error message from the given tag name.
	 * @param a_strTagName a tag name to parse
	 * @return the parsed tag name
	 */
	private static String parseTagName(String a_strTagName)
	{
		String strParseError = "Error while parsing XML ";

		if (a_strTagName == null)
		{
			strParseError = "";
		}
		else if (a_strTagName.equals(ROOT_TAG))
		{
			strParseError += "document root! ";
		}
		else if (a_strTagName.endsWith(NODE_NULL_TAG))
		{
			strParseError += "- node is null! ";
		}
		else
		{
			strParseError += "node '" + a_strTagName + "'! ";
		}

		return strParseError;
	}
}
