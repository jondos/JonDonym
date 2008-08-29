/*
	Copyright (c) The JAP-Team, JonDos GmbH
	All rights reserved.
	Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:
	Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
	Redistributions in binary form must reproduce the above copyright notice,
	 this list of conditions and the following disclaimer in the documentation and/or
	 other materials provided with the distribution.
	Neither the name of the University of Technology Dresden, Germany, nor the name of
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
package jap;

import gui.help.AbstractHelpFileStorageManager;

import java.io.File;
import java.io.IOException;
import java.util.Observable;
import java.util.zip.ZipFile;

import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import platform.AbstractOS;

import anon.util.ClassUtil;
import anon.util.RecursiveCopyTool;
import anon.util.XMLParseException;
import anon.util.XMLUtil;
import anon.util.ZipArchiver;

/**
 * Handling of the JonDo help files
 * @author Simon Pecher
 *
 */
public final class JARHelpFileStorageManager extends AbstractHelpFileStorageManager  {
	

	public final static String HELP_VERSION_NODE = "jondohelp";
	public final static String HELP_VERSION_ATTRIBUTE = "version";
	
	
	public static final String HELP_VERSION_FILE = "jondohelp.xml";
	
	
	private String m_helpPath;	
	private ZipArchiver m_archiver;
	
	public JARHelpFileStorageManager()
	{
		ZipFile japArchive = ClassUtil.getJarFile();
		if(japArchive == null)
		{
			//TODO: Throw something
			//throw new
		}
		m_archiver = new ZipArchiver(japArchive);				
	}
	/**
	 * sets the specified path for external installation of the help files
	 * performs no validityCheck, because it is only called by
	 * handleHelpPathChanged, which is invkoed by the JAPModel, which itself 
	 * performs a validity check
	 */
	private void setHelpPath(String helpPath)
	{
		this.m_helpPath = helpPath;
	}
	
	/**
	 * checks whether the help version number matches the JonDo version number
	 * @return true if the stored help version number is not exactly the same 
	 * 		as the JonDo version number or if the number could not be retrieved
	 *         (if no help is available)
	 */
	public boolean helpVersionMismatch()
	{
		String versionString = 
			getHelpVersion(m_helpPath +  File.separator + HELP_FOLDER);
		if (versionString == null)
		{
			return true;
		}
		return !JAPConstants.aktVersion.equals(versionString);
	}
	
	
	public boolean handleHelpPathChanged(String oldHelpPath, String newHelpPath, boolean a_bIgnoreExistingHelpDir)
	{
		boolean installationSuccessful = true;
		setHelpPath(newHelpPath);
		if(oldHelpPath != null)
		{
			removeOldHelp(oldHelpPath, false);
		}
		if(newHelpPath != null)
		{
			installationSuccessful = installHelp(a_bIgnoreExistingHelpDir);
		}
		return installationSuccessful;
	}
	
	public String helpPathValidityCheck(String a_absolutePath, boolean a_bIgnoreExistingHelpDir) 
	{
		String strPath;
		int index;
		File fileTmp;
		
		if(a_absolutePath != null)
		{
			File hpFile = new File(a_absolutePath);
			
			if (a_absolutePath.indexOf(JAPConstants.APPLICATION_NAME) >= 0)
			{
				// this is definitely a JonDo application folder
				a_bIgnoreExistingHelpDir = true;
			}
			
			if(hpFile.exists())
			{
				if(hpFile.isDirectory())
				{
					strPath = a_absolutePath;
					while ((index = strPath.indexOf(HELP_FOLDER)) > 0)
					{
						fileTmp = new File(strPath.substring(0, index) + HELP_FOLDER);
						if (!fileTmp.exists())
						{
							LogHolder.log(LogLevel.EMERG, LogType.MISC, 
									"Existing help directory was not found!");
						}
						if (getHelpVersion(m_helpPath + 
								File.separator + HELP_FOLDER) != null)
						{
							return HELP_NESTED;
						}
						strPath = strPath.substring(index, strPath.length());
					}
					
					// check for virtual directories					
					strPath = AbstractOS.getInstance().getenv("ALLUSERSPROFILE");
					if (strPath != null && hpFile.getPath().indexOf(strPath) >= 0)
					{
						return HELP_VIRTUAL;
					}					
					strPath = AbstractOS.getInstance().getenv("PROGRAMFILES");
					if (strPath != null && hpFile.getPath().indexOf(strPath) >= 0)
					{
						return HELP_VIRTUAL;
					}
					strPath = AbstractOS.getInstance().getenv("SYSTEMROOT");
					if (strPath != null && hpFile.getPath().indexOf(strPath) >= 0)
					{
						return HELP_VIRTUAL;
					}
					strPath = AbstractOS.getInstance().getenv("PROGRAMDATA");
					if (strPath != null && hpFile.getPath().indexOf(strPath) >= 0)
					{
						return HELP_VIRTUAL;
					}
					
						
					File helpDir = new File(hpFile.getPath() + File.separator +
							HELP_FOLDER + File.separator);
					
					
					if(!helpDir.exists())
					{
						try
						{							
							if (helpDir.mkdir())
							{
								helpDir.delete();
							}
							else
							{
								return HELP_INVALID_NOWRITE;
							}
						}
						catch (SecurityException a_e)
						{
							return HELP_INVALID_NOWRITE;
						}
						return HELP_VALID;
					}
					else
					{
						//help directory already exists in specified folder
						File jondoHelpFileVersion =
							new File(hpFile.getPath()+
									File.separator+
									HELP_FOLDER+
									File.separator+
									HELP_VERSION_FILE);
						if(a_bIgnoreExistingHelpDir || jondoHelpFileVersion.exists())
						{
							return HELP_JONDO_EXISTS;
						}
						else
						{
							return HELP_DIR_EXISTS;
						}
					}
					
				}
				else
				{
					//path is not a directory
					return HELP_NO_DIR;
				}
			}
			else
			{
				//no such directory
				return HELP_INVALID_PATH_NOT_EXISTS;
			}
		}
		else
		{
			//null path specified
			return HELP_INVALID_NULL;
		}
	}
	
	public Observable getStorageObservable()
	{
		return m_archiver;
	}
	
	/**
	 * installs the JonDo help externally out the JonDo Jarfile in the specified external destination folder. 
	 * This routine overrides already installed JonDo help files.
	 */
	private boolean installHelp(boolean a_bIgnoreExistingHelpDir)
	{
		File helpFolder = getHelpFolder();
		if(helpFolder == null)
		{
			LogHolder.log(LogLevel.EMERG, LogType.MISC, "Destination folder is null: Aborting help installation");
			return false;
		}
		if(m_archiver == null)
		{
			LogHolder.log(LogLevel.EMERG, LogType.MISC, "JARStorageManager does only work when started from a Jar file");
			return false;
		}
		//override old help
		if(helpFolder.exists())
		{	
			if (helpVersionMismatch())
			{
				removeOldHelp(m_helpPath, a_bIgnoreExistingHelpDir);
				if(helpFolder.exists())
				{
					LogHolder.log(LogLevel.EMERG, LogType.MISC, "Could not delete old help directory!");
					return false;
				}
			}
			else
			{
				LogHolder.log(LogLevel.NOTICE, LogType.MISC, "Previous help installation restored.");
				return true;
			}
		}
			
		boolean installationSuccessful = m_archiver.extractArchive(HELP_FOLDER + "/", m_helpPath);
		if(installationSuccessful)
		{
			createHelpVersionDoc();
		}
		else
		{
			LogHolder.log(LogLevel.ERR, LogType.MISC, "Extracting help files was not succesful.");
			return false;
		}
		
		return installationSuccessful;
	}
	
	/**
	 * creates an XML document containing the version of the JonDo help which has to match the actual JonDo version
	 */
	private void createHelpVersionDoc()
	{
		Document helpVersionDoc = XMLUtil.createDocument();
		Element helpVersionNode = helpVersionDoc.createElement(HELP_VERSION_NODE);
		XMLUtil.setAttribute(helpVersionNode, HELP_VERSION_ATTRIBUTE, JAPConstants.aktVersion);
		helpVersionDoc.appendChild(helpVersionNode);
		
		File versionInfoFile = new File(m_helpPath+File.separator+ HELP_FOLDER + File.separator +HELP_VERSION_FILE);
		try
		{
			XMLUtil.write(helpVersionDoc, versionInfoFile);
		} 
		catch (IOException ioe)
		{
			LogHolder.log(LogLevel.WARNING, LogType.MISC, "Could not write help version due to an I/O error: ", ioe);
		}
	}
	
	private boolean removeOldHelp(String parentPath, boolean a_bIgnoreExistingHelpDir)
	{
		if(parentPath == null)
		{
			return true;
		}
		File helpFolder = new File(parentPath+File.separator+HELP_FOLDER + File.separator);
		File helpVersionFile = 
			new File(parentPath+File.separator+HELP_FOLDER+File.separator+HELP_VERSION_FILE);
		
		if(!helpFolder.exists() || 
			(!a_bIgnoreExistingHelpDir && !helpVersionFile.exists()))
		{
			LogHolder.log(LogLevel.INFO, LogType.MISC, "No old help found in "+helpFolder.getPath());
			return true;
		}
		
		/* Make sure that there will be never the wrong directory as parameter!!! */
		if (!RecursiveCopyTool.deleteRecursion(helpFolder))
		{
			// try again
			LogHolder.log(LogLevel.WARNING, LogType.MISC, 
					"Failed to delete old help at first try - try again!");
			RecursiveCopyTool.deleteRecursion(helpFolder);
		}
		
		if (!helpFolder.exists())
		{
			LogHolder.log(LogLevel.DEBUG, LogType.MISC, "removed old help from "+parentPath);
			return true;
		}
		
		return false;
	}
	
	/**
	 * checks if there is a help installed in the specified external help path
	 * @return true if a help folder exists in the user defined help path. If no help 
	 * 			path is specified by the user the default path (the folder where the 
	 * 			Jarfile is situated) is checked.
	 */
	private boolean isHelpInstalled()
	{
		File helpFolder = getHelpFolder();
		if(helpFolder == null)
		{
			return false;
		}
		return helpFolder.exists();
	}
	
	/**
	 * Reads the version string out of the help version XML file
	 * The help version number corresponds to the version number of the JonDo
	 * 
	 * @param versionFile the XML file where the help version is specified.
	 * @return the help version number as string, or null if no version string was found in versionFile
	 */
	private String getHelpVersion(String a_helpFolder)
	{
		try 
		{
			File versionInfoFile = 
				new File(a_helpFolder +  File.separator + HELP_VERSION_FILE);
			if (!versionInfoFile.exists())
			{
				return null;
			}
			Document doc = XMLUtil.readXMLDocument(versionInfoFile);
			Node versionNode = XMLUtil.getFirstChildByName(doc, HELP_VERSION_NODE);
			String versionString = XMLUtil.parseAttribute(versionNode, HELP_VERSION_ATTRIBUTE, null);
			return versionString;
		} 
		catch (IOException ioe) 
		{
			LogHolder.log(LogLevel.ERR, LogType.MISC, "Error: an I/O error occured while parsing help version file: ", ioe);
		} 
		catch (XMLParseException xpe) 
		{
			LogHolder.log(LogLevel.ERR, LogType.MISC, "Error: help version file cannot be parsed: ", xpe);
		}
		return null;
	}
	
	/**
	 * convenience function that returns a file reference to the current help root folder 
	 * @return a file reference to the current help root folder or null if helpPath is null
	 */ 
	private File getHelpFolder()
	{
		if(m_helpPath == null)
		{
			return null;
		}
		return new File(m_helpPath+File.separator + HELP_FOLDER + File.separator);
	}
	
	public void ensureMostRecentVersion(String helpPath) 
	{
		setHelpPath(helpPath);
		if(helpVersionMismatch() || !isHelpInstalled())
		{
			installHelp(false);
		}
	}
	public boolean helpInstallationExists(String helpPath) {
		
		setHelpPath(helpPath);
		return isHelpInstalled();
	}
}
