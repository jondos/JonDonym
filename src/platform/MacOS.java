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
package platform;

import java.io.File;
import anon.util.ClassUtil;

import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;



/**
 * This class is instantiated by AbstractOS if the current OS is MacOS or MacOSX
 */
public class MacOS extends AbstractOS
{
	public static final String OS_NAME = "Mac OS";
	final static String BUNDLE_CONTENTS = "Contents"+File.separator;
	final static String BUNDLE_MAC_OS_EXECUTABLES = BUNDLE_CONTENTS+"MacOS"+File.separator;
	final static String BUNDLE_PROPERTY_FILE_NAME = "Info.plist";
	final static String BUNDLE_EXECUTABLE_PROPERTY_KEY = "CFBundleExecutable";
	
	//private HashMap m_bundleProperties = null;
	private String m_bundlePath = null;
	
	public MacOS() throws Exception
	{
		if (System.getProperty("mrj.version") == null)
		{
			throw new Exception("Operating system is not "+ OS_NAME);
		}
		//m_bundleProperties = new HashMap();
		setBundlePath();
		//loadBundleProperties();
		
		
	}

	/*
	public boolean openURL(URL a_url)
	{
		//MRJFileUtils.openURL(a_url.toString());
		return openLink(a_url.toString());
	}*/
	
	
	protected boolean openLink(String a_link)
	{
		try
		{
			Runtime.getRuntime().exec("open " + a_link);
			return true;
		}
		catch (Exception ex)
		{
			LogHolder.log(LogLevel.ERR, LogType.MISC,
						  "Cannot open '" + a_link + "' in MacOS default program.");
		}

		return false;
	}

	public String getConfigPath()
	{
		//Return path in users's home/Library/Preferences
		if (System.getProperty("os.name").equalsIgnoreCase(OS_NAME))
		{
			return System.getProperty("user.home", ".") +"/";
		}
		else
		{
			return System.getProperty("user.home", "") + "/Library/Preferences/";
		}
	}
	
	/* returns absolute path to application bundle or null,
	 * if JAP is not executed as application bundle
	 */
	public void setBundlePath()
	{
		File classParentFile = ClassUtil.getClassDirectory(this.getClass());
		if(classParentFile != null)
		{
			String path = classParentFile.getPath();
			if(path != null)
			{
				// remove file: prefix
				if(!(path.startsWith(File.separator)))
				{
					int s_index = path.indexOf("/");
					path = (s_index != -1) ? path.substring(s_index) : path;
				}
				int index_path = path.indexOf(BUNDLE_CONTENTS);
				if(index_path != -1)
				{
					/* JAP is started as an  application bundle */
					m_bundlePath = path.substring(0, index_path-1);
					return;
				}
			}
		}
		m_bundlePath = null;
	}
	
	public String getBundlePath()
	{
		return m_bundlePath;
	}
	
	public boolean isBundle() {
		return (m_bundlePath != null);
	}
	
	/* load the bundle properties specified in the Info.plist property file */
	/*protected void loadBundleProperties()
	{
		if(isBundle())
		{
			try 
			{
				File bundlePropertyFile = new File(new URI("file://"+m_bundlePath+File.separator+
						BUNDLE_CONTENTS+BUNDLE_PROPERTY_FILE_NAME));
				
				Document bundlePropertyDoc = XMLUtil.readXMLDocument(bundlePropertyFile);
				if(bundlePropertyDoc != null)
				{
					NodeList bundlePropertyDicts = bundlePropertyDoc.getElementsByTagName("dict");
					if(bundlePropertyDicts != null)
					{
						int bprop_length = bundlePropertyDicts.getLength();
						for(int index = 0; index < bprop_length; index++)
						{
							Node dictNode = bundlePropertyDicts.item(index);
							if(dictNode.hasChildNodes())
							{
								NodeList dictChildNodes = dictNode.getChildNodes();
								int nrChildNodes = dictChildNodes.getLength();
								String keyName = null;
								for (int i = 0; i < nrChildNodes; i++) {
									Node dictChildeNode = dictChildNodes.item(i);
									if(dictChildeNode.getNodeName().equals("key"))
									{
										keyName = dictChildeNode.getTextContent();
									} 
									else if(dictChildeNode.getNodeName().equals("string"))
									{
										if(keyName != null)
										{
											m_bundleProperties.put(keyName, dictChildeNode.getTextContent());
											keyName = null;
										}
									}
								}
							}
						}
					}
				}
			} 
			catch (IOException ioe) 
			{
				LogHolder.log(LogLevel.ERR, LogType.MISC,
						"Cannot open bundle property file: "+BUNDLE_PROPERTY_FILE_NAME+", cause:", ioe);
			} 
			catch (XMLParseException xpe) 
			{
				LogHolder.log(LogLevel.ERR, LogType.MISC,
						"Cannot parse bundle property file: "+BUNDLE_PROPERTY_FILE_NAME+", cause:", xpe);
			} catch (URISyntaxException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}*/
	
	/* returns absolute path to application bundle executable
	 * which is the stub to execute the jar file
	 * returns null, if JAP is not executed as an application bundle
	 */
	public String getBundleExecutablePath()
	{
		/*if(!isBundle())
		{
			return null;
		}
		String bundleExecutable = 
			(String) m_bundleProperties.get(BUNDLE_EXECUTABLE_PROPERTY_KEY);
		if (bundleExecutable == null)
		{
			return null;
		}
		return getBundlePath()+File.separator+BUNDLE_MAC_OS_EXECUTABLES+bundleExecutable;*/
		return null;
	}
	
	
	
}
