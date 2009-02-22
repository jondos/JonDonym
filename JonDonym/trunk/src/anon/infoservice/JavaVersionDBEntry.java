/*
 Copyright (c) 2000 - 2006, The JAP-Team
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

import java.net.URL;
import java.util.Enumeration;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import anon.util.XMLParseException;
import anon.util.XMLUtil;


/**
 * Stores information about the latest java version of a specific vendor.
 * @author Rolf Wendolsky
 */
public class JavaVersionDBEntry  extends AbstractDistributableDatabaseEntry
{
	public static final String CURRENT_JAVA_VENDOR = System.getProperty("java.vendor");
	public static final String CURRENT_JAVA_VERSION = System.getProperty("java.version");

	public static final String HTTP_REQUEST_STRING = "/currentjavaversion";
	public static final String HTTP_SERIALS_REQUEST_STRING = "/currentjavaversionSerials";

	public static final String PROPERTY_NAME = "jreVersionsFileName";

	public static final String VENDOR_ID_SUN_JAVA = "Sun";
	public static final String VENDOR_ID_BLACKDOWN_JAVA = "Blackdown";

	public static final String XML_ELEMENT_NAME = "JavaVersion";
	public static final String XML_ELEMENT_CONTAINER_NAME = "JavaVersionInfos";

	private static final String OS_NAME = System.getProperty("os.name", "");

	private static final String XML_ATTR_VENDOR = "vendor";
	private static final String XML_ATTR_OPERATING_SYSTEM = "os";
	private static final String XML_ELEM_VERSION = "LatestVersion";
	private static final String XML_ATTR_VERSION_NAME = "name";
	private static final String XML_ELEM_DOWNLOAD_URL = "DownloadURL";
	private static final String XML_ELEM_VENDOR_LONG = "VendorLongName";
	private static final String XML_ELEM_LAST_UPDATE = "LastUpdate";

	private static final String[] VENDOR_IDS = {VENDOR_ID_SUN_JAVA, VENDOR_ID_BLACKDOWN_JAVA};

	/**
	 * This should be an infinite timeout (1000 years are infinite enough).
	 */
	private static final long TIMEOUT = Long.MAX_VALUE;


	private long m_lastUpdate;
	private String m_latestVersion;
	private String m_vendor;
	private URL m_downloadURL;
	private String m_vendorLongName;
	private String m_versionName;

	/**
	 * Stores the XML representation of this DBEntry.
	 */
	private Element m_xmlDescription;

/*
	public JavaVersionDBEntry(String a_vendorId, String a_latestVersion, String a_latestVersionName, URL a_downloadURL,
							  String a_vendorLongName) throws IllegalArgumentException
	{
		super(System.currentTimeMillis() + TIMEOUT);

		Element elemTemp;
		Document doc;

		if (a_latestVersion == null)
		{
			throw new IllegalArgumentException("Version is null!");
		}

		if (!checkVendor(a_vendorId))
		{
			throw new IllegalArgumentException("Unknown vendor!");
		}
		if (a_downloadURL == null)
		{
			throw new IllegalArgumentException("Download URL is null!");
		}

		m_latestVersion = a_latestVersion;
		m_versionName = a_latestVersionName;
		m_vendor = a_vendorId;
		m_downloadURL = a_downloadURL;
		m_vendorLongName = a_vendorLongName;

		doc = XMLUtil.createDocument();
		m_xmlDescription = doc.createElement(XML_ELEMENT_NAME);
		XMLUtil.setAttribute(m_xmlDescription, XML_ATTR_VENDOR, m_vendor);
		elemTemp = doc.createElement(XML_ELEM_VERSION);
		XMLUtil.setValue(elemTemp, m_vendor);
		m_xmlDescription.appendChild(elemTemp);

		if (m_vendorLongName != null)
		{
			elemTemp = doc.createElement(XML_ELEM_VENDOR_LONG);
			XMLUtil.setValue(elemTemp, m_vendorLongName);
			m_xmlDescription.appendChild(elemTemp);
		}

		elemTemp = doc.createElement(XML_ELEM_DOWNLOAD_URL);
		XMLUtil.setValue(elemTemp, m_downloadURL.toString());
		m_xmlDescription.appendChild(elemTemp);

		elemTemp = doc.createElement(XML_ELEM_LAST_UPDATE);
		XMLUtil.setValue(elemTemp, m_lastUpdate);
		m_xmlDescription.appendChild(elemTemp);
	}
*/
	public JavaVersionDBEntry(Element a_xmlElement) throws XMLParseException
	{
		super(TIMEOUT);
		Node currentElement;
		NodeList nodes;
		String strTemp;

		if (a_xmlElement == null || !a_xmlElement.getNodeName().equals(XML_ELEMENT_NAME))
		{
			throw new XMLParseException(XMLParseException.ROOT_TAG);
		}
		m_vendor = XMLUtil.parseAttribute(a_xmlElement, XML_ATTR_VENDOR, null);
		if (!checkVendor(m_vendor))
		{
			throw new XMLParseException(XML_ELEMENT_NAME, "Unknown vendor!");
		}

		nodes = a_xmlElement.getElementsByTagName(XML_ELEM_VERSION);
		for (int i = 0; i < nodes.getLength(); i++)
		{
			strTemp = XMLUtil.parseAttribute(nodes.item(i), XML_ATTR_OPERATING_SYSTEM, "");
			if ((m_latestVersion == null && strTemp.length() == 0) || OS_NAME.indexOf(strTemp) >= 0)
			{
				try
				{
					m_latestVersion = XMLUtil.parseValue(nodes.item(i), null);
					m_versionName = XMLUtil.parseAttribute(nodes.item(i), XML_ATTR_VERSION_NAME, null);
				}
				catch (Exception a_e)
				{
				}
			}
		}
		if (m_latestVersion == null)
		{
			throw new XMLParseException(XML_ELEM_VERSION);
		}

		currentElement = XMLUtil.getFirstChildByName(a_xmlElement, XML_ELEM_LAST_UPDATE);
		m_lastUpdate = XMLUtil.parseValue(currentElement, -1L);
		if (m_lastUpdate == -1)
		{
			m_lastUpdate = System.currentTimeMillis();
		}

		nodes = a_xmlElement.getElementsByTagName(XML_ELEM_DOWNLOAD_URL);
		for (int i = 0; i < nodes.getLength(); i++)
		{
			strTemp = XMLUtil.parseAttribute(nodes.item(i), XML_ATTR_OPERATING_SYSTEM, "");
			if ((m_downloadURL == null && strTemp.length() == 0) || OS_NAME.indexOf(strTemp) >= 0)
			{
				try
				{
					m_downloadURL = new URL(XMLUtil.parseValue(nodes.item(i), null));
				}
				catch (Exception a_e)
				{
				}
			}
		}
		if (m_downloadURL == null)
		{
			throw new XMLParseException(XML_ELEM_DOWNLOAD_URL);
		}


		currentElement = XMLUtil.getFirstChildByName(a_xmlElement, XML_ELEM_VENDOR_LONG);
		try
		{
			m_vendorLongName = XMLUtil.parseValue(currentElement, null);
		}
		catch (Exception a_e)
		{
			// ignore
		}

		m_xmlDescription = a_xmlElement;
	}

	public static JavaVersionDBEntry getNewJavaVersion()
	{
		Enumeration versions = Database.getInstance(JavaVersionDBEntry.class).getEntrySnapshotAsEnumeration();
		JavaVersionDBEntry currentVersion;
		while (versions.hasMoreElements())
		{
			currentVersion =  (JavaVersionDBEntry)versions.nextElement();
			if (isJavaTooOld(currentVersion))
			{
				return currentVersion;
			}
		}
		//	Database.getInstance(JavaVersionDBEntry.class).getEntryById(m_newJavaVersion.getId())
		return null;
	}


	public static boolean isJavaTooOld(JavaVersionDBEntry a_entry)
	{
		if (a_entry == null || CURRENT_JAVA_VENDOR == null)
		{
			return false;
		}

		String vendor = a_entry.getVendor().toLowerCase();
		String currentVendor = CURRENT_JAVA_VENDOR.toLowerCase();
		if ((currentVendor.indexOf("microsoft") >= 0) ||
			(currentVendor.indexOf(vendor) >= 0 && (CURRENT_JAVA_VERSION == null ||
			 CURRENT_JAVA_VERSION.compareTo(a_entry.getJREVersion()) < 0)))
		{
			return true;
		}

		return false;
	}


	/**
	 * Returns the URL where this Java version is available.
	 * @return the URL where this Java version is available
	 */
	public URL getDownloadURL()
	{
		return m_downloadURL;
	}

	public Element getXmlStructure()
	{
		return m_xmlDescription;
	}

	/**
	 * Returns the latest known version number of the JRE from this vendor.
	 * @return String
	 */
	public String getJREVersion()
	{
		return m_latestVersion;
	}

	/**
	 * Returns the public human readable name for this JRE version.
	 * @return the public human readable name for this JRE version
	 */
	public String getJREVersionName()
	{
		return m_versionName;
	}

	/**
	 * Returns version number which is used to determine the more recent infoservice entry, if two
	 * entries are compared (higher version number -> more recent entry).
	 *
	 * @return The version number for this entry.
	 */
	public long getVersionNumber()
	{
		return m_lastUpdate;
	}

	public long getLastUpdate()
	{
		return m_lastUpdate;
	}

	/**
	 * Returns the vendor ID.
	 * @return the vendor ID
	 */
	public String getVendor()
	{
		return m_vendor;
	}

	/**
	 * Returns a more detailed vendor name.
	 * @return a more detailed vendor name
	 */
	public String getVendorLongName()
	{
		if (m_vendorLongName == null || m_vendorLongName.trim().length() == 0)
		{
			return m_vendor;
		}
		return m_vendorLongName;
	}

	/**
	 * Returns the vendor ID.
	 * @return the vendor ID
	 */
	public String getId()
	{
		return m_vendor;
	}

	/**
	 * Returns the HTTP_REQUEST_STRING.
	 * @return the HTTP_REQUEST_STRING
	 */
	public String getPostFile()
	{
		return HTTP_REQUEST_STRING;
	}

	private static boolean checkVendor(String a_vendorId)
	{
		if (a_vendorId == null)
		{
			return false;
		}

		for (int i = 0; i < VENDOR_IDS.length; i++)
		{
			if (VENDOR_IDS[i].equals(a_vendorId))
			{
				return true;
			}
		}
		return false;
	}
}
