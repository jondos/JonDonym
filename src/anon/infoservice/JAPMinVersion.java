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
package anon.infoservice;

import org.w3c.dom.Element;

import anon.util.XMLUtil;
import anon.util.XMLParseException;
import java.io.File;

/**
 * Holds the information about the currently minimum required JAP version to access the
 * mixcascades.
 */
public class JAPMinVersion extends AbstractDistributableDatabaseEntry
{
	public static final String DEFAULT_ID = "JAPMinVersion";

	/**
	 * The timeout for minimum JAP version information in the database. This should be an infinite
	 * timeout (1000 years are infinite enough).
	 */
	private static final long DATABASE_TIMEOUT = 1000 * 365 * 24 * 3600 * 1000L;

	/**
	 * Stores the included ServiceSoftware information.
	 */
	private ServiceSoftware m_japSoftware;

	/**
	 * Time (see System.currentTimeMillis()) when the root-of-update information infoservice has
	 * propagated this minimum version info.
	 */
	private long m_lastUpdate;

	/**
	 * Stores the XML structure of this JAPVersionInfo.
	 */
	private Element m_xmlStructure;

	/** The same as above as byte[]*/
	private byte[] m_bytesPostData;

	/**
	 * Returns the name of the XML root element used by this class.
	 *
	 * @return The name of the XML root element used by this class ('Jap').
	 */
	public static String getXmlElementName()
	{
		return "Jap";
	}

	/**
	 * Creates a new JAPMinVersion from XML description load from local filesystem.
	 *
	 * @param a_fileJapMinVersion The XML file containing the root node of the XML structure of this JAPMinVersion ('Jap' node).
	 */
	public JAPMinVersion(File a_fileRootNode) throws Exception
	{
		this(XMLUtil.readXMLDocument(a_fileRootNode).getDocumentElement());
	}

	/**
	 * Creates a new JAPMinVersion from XML description received from remote infoservice or local
	 * filesystem.
	 *
	 * @param a_japRootNode The root node of the XML structure of this JAPMinVersion ('Jap' node).
	 */
	public JAPMinVersion(Element a_japRootNode) throws Exception
	{
		super(System.currentTimeMillis() + DATABASE_TIMEOUT);
		Element softwareNode = (Element) (XMLUtil.getFirstChildByName(a_japRootNode,
			ServiceSoftware.getXmlElementName()));
		if (softwareNode == null)
		{
			throw (new Exception("JAPMinVersion: Constructor: Error in XML structure: No software node."));
		}
		m_japSoftware = new ServiceSoftware(softwareNode);

		String versionString = m_japSoftware.getVersion();
		if ( (versionString.charAt(2) != '.') || (versionString.charAt(5) != '.'))
		{
			throw new XMLParseException("Invalid version number format: " + versionString);
		}

		/** @todo FixMe: Removed because new IS does not work...*/
		m_lastUpdate = XMLUtil.parseValue(XMLUtil.getFirstChildByName(a_japRootNode, "LastUpdate"), -1L);
		if (m_lastUpdate == -1)
		{
			m_lastUpdate = System.currentTimeMillis();
			//throw (new Exception("JAPMinVersion: Constructor: No LastUpdate node found."));
		}
		m_xmlStructure = a_japRootNode;
		m_bytesPostData=super.getPostData();
	}

	/**
	 * Returns the ID for this JAPMinVersion instance. Because there is only one instance in the
	 * system, it's always 'JAPMinVersion'.
	 *
	 * @return The ID for this JAPMinVersion.
	 */
	public String getId()
	{
		return DEFAULT_ID;
	}

	public long getLastUpdate()
	{
		return m_lastUpdate;
	}

	/**
	 * Returns the time when this mimimum version information was created by the
	 * root-of-update-information infoservice. That infoservice will update and propagate the
	 * minimum version info periodically even if nothing has changed. Attention: The version number
	 * returned here is only necessary for  message exchange between the infoservices and is
	 * completely independent from the version number of the JAP version described in this minimum
	 * version info. Don't mix up those two version numbers.
	 *
	 * @return A version number which is used to determine the more recent minimum version info
	 *         entry, if two entries are compared (higher version number -> more recent entry).
	 */
	public long getVersionNumber()
	{
		return m_lastUpdate;
	}

	/**
	 * Returns the included ServiceSoftware information about the minimum required JAP software
	 * version.
	 *
	 * @return The software information about the minimum JAP software version.
	 */
	public ServiceSoftware getJapSoftware()
	{
		return m_japSoftware;
	}

	/**
	 * This returns the filename (InfoService command), where this JAP minimum required version
	 * information is posted at other InfoServices, It's always '/currentjapversion'.
	 *
	 * @return The filename where this JAPMinVersion entry is posted at other InfoServices when this
	 *         entry is forwarded.
	 */
	public String getPostFile()
	{
		return "/currentjapversion";
	}

	/**
	 * Returns the XML structure of this mimimum version info entry.
	 *
	 * @return The XML node of this JAPMinVersion entry ('Jap' node).
	 */
	public Element getXmlStructure()
	{
		return m_xmlStructure;
	}

	public byte[]getPostData()
	{
		return m_bytesPostData;
	}

}
