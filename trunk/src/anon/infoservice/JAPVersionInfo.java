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

import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.text.ParseException;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import anon.util.XMLUtil;

/**
 * This stores the version information about the current JAP release or development version.
 */
public class JAPVersionInfo extends AbstractDistributableDatabaseEntry
{
	public static final String ID_DEVELOPMENT = "/japDevelopment.jnlp";
	public static final String ID_RELEASE = "/japRelease.jnlp";

  /**
   * Describes a JAP release version.
   */
  public final static int JAP_RELEASE_VERSION = 1;

  /**
   * Describes a JAP development version.
   */
  public final static int JAP_DEVELOPMENT_VERSION = 2;


  /**
   * The timeout for JAP version information in the database. This should be an infinite timeout
   * (1000 years are infinite enough).
   */
  private static final long DATABASE_TIMEOUT = 1000 * 365 * 24 * 3600 * 1000L;


  /**
   * Stores whether this is the version info for the development version or the release version.
   * See the constants in this class.
   */
  private int m_versionInfoType;

  /**
   * Stores the version number of the described JAP version.
   */
  private String m_version;

  /**
   * Stores release date of the described JAP version.
   */
  private Date m_releaseDate;

  /**
   * Stores the filename of the where the corresponding JAP software is available on the server.
   */
  private String m_jarFileName;

  /**
   * Stores the URL of the server, where the corresponding JAP software can be downloaded from.
   */
  private URL m_codeBase;

  /**
   * Time (see System.currentTimeMillis()) when the root-of-update information infoservice has
   * propagated this version info.
   */
  private long m_lastUpdate;

  /**
   * Stores the XML structure of this JAPVersionInfo.
   */
  private Element m_xmlStructure;


  /**
   * Returns the name of the XML root element used by this class.
   *
   * @return The name of the XML root element used by this class ('jnlp').
   */
  public static String getXmlElementName()
  {
    return "jnlp";
  }


  /**
   * Creates a new JAP version info out of a JNLP file.
   *
   * @param a_jnlpRootNode The root node of the JNLP document.
   * @param a_versionInfoType The type of the JAPVersionInfo (release / development), see the
   *                          constants in this class.
   */
  public JAPVersionInfo(Element a_jnlpRootNode, int a_versionInfoType) throws Exception
  {
    super(System.currentTimeMillis() + DATABASE_TIMEOUT);
    m_versionInfoType = a_versionInfoType;
    /* parse the document */
    m_version = XMLUtil.parseAttribute(a_jnlpRootNode, "version", "");
    try {
      String strDate = a_jnlpRootNode.getAttribute("releaseDate") + " GMT";
	  try
	  {
		  m_releaseDate = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss z").parse(strDate);
	  }
	  catch (ParseException a_e)
	  {
		  m_releaseDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z").parse(strDate);
	  }
    }
    catch (Exception e) {
      m_releaseDate = null;
    }
    m_codeBase = new URL(a_jnlpRootNode.getAttribute("codebase"));
    NodeList nlResources = a_jnlpRootNode.getElementsByTagName("resources");
    NodeList nlJars = ( (Element) nlResources.item(0)).getElementsByTagName("jar");
    for (int i = 0; i < nlJars.getLength(); i++) {
      try {
        Element elemJar = (Element) nlJars.item(i);
        String part = elemJar.getAttribute("part");
        if (part.equals("jap")) {
          m_jarFileName = elemJar.getAttribute("href");
        }
      }
      catch (Exception e) {
      }
    }
    m_lastUpdate = XMLUtil.parseValue(XMLUtil.getFirstChildByName(a_jnlpRootNode, "LastUpdate"), -1L);
    if (m_lastUpdate == -1) {
		/**@todo FixMe Removed because new Is does not work..*/
		m_lastUpdate=System.currentTimeMillis();
      //throw (new Exception("JAPVersionInfo: Constructor: No LastUpdate node found."));
    }
    m_xmlStructure = a_jnlpRootNode;
  }




  /**
   * Returns the ID of this version information. It's the filename where this version info is
   * available on the infoservice ('/japRelease.jnlp' or '/japDevelopment.jnlp' depending on the
   * type of this version info)
   *
   * @return The ID of this version info.
   */
  public String getId()
  {
    String versionInfoId = ID_RELEASE;
    if (m_versionInfoType == JAP_DEVELOPMENT_VERSION)
	{
      versionInfoId = ID_DEVELOPMENT;
    }
    return versionInfoId;
  }



  /**
   * Returns the time when this version information was created by the root-of-update-information
   * infoservice. That infoservice will update and propagate the version info periodically even
   * if nothing has changed. Attention: The version number returned here is only necessary for
   * message exchange between the infoservices and is completely independent from the version
   * number of the JAP version described in this version info. Don't mix up those two version
   * numbers.
   *
   * @return A version number which is used to determine the more recent version info entry, if
   *         two entries are compared (higher version number -> more recent entry).
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
   * Returns the JAP software version described by this version info structure.
   *
   * @return The JAP software version described in this JAPVersionInfo.
   */
  public String getJapVersion()
  {
    return m_version;
  }

  /**
   * Returns the release date of the JAP software described in this version info structure.
   *
   * @return The release date of the JAP software described in this JAPVersionInfo.
   */
  public Date getDate()
  {
    return m_releaseDate;
  }

  /**
   * Returns the URL of the server, where the corresponding JAP software can be downloaded from.
   *
   * @return The URL of the download server for the JAP software described in this version info
   *         structure.
   */
  public URL getCodeBase()
  {
    return m_codeBase;
  }

  /**
   * Returns the filename of the JAP file on the download server.
   *
   * @return The filename where the JAP software described in this JAPVersionInfo is available on
   *         the download server.
   */
  public String getJAPJarFileName()
  {
    return m_jarFileName;
  }

  /**
   * This returns the filename (InfoService command), where this JAPVersionInfo entry is posted at
   * other InfoServices. Depending on the type of this version info, it's '/japRelease.jnlp' or
   * 'japDevelopment.jnlp'. This method returns the same value as the getId() method of this
   * instance.
   *
   * @return The filename where this version information is posted at other infoservices when this
   *         entry is forwarded.
   */
  public String getPostFile()
  {
    return getId();
  }

  /**
   * Returns the XML structure for this version info entry.
   *
   * @return The XML node of this JAPVersionInfo entry (jnlp node).
   */
  public Element getXmlStructure()
  {
    return m_xmlStructure;
  }

}
