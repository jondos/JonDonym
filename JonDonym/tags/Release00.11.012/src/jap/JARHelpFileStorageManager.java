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
import anon.util.RecursiveFileTool;
import anon.util.XMLParseException;
import anon.util.XMLUtil;
import anon.util.ZipArchiver;

/**
 * Handling of the JonDo help files
 * @author Simon Pecher
 *
 */
public final class JARHelpFileStorageManager extends AbstractHelpFileStorageManager  
{
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
	 * handleHelpPathChanged, which is invoked by the JAPModel, which itself 
	 * performs a validity check
	 */
	private void setHelpPath(String helpPath)
	{
		m_helpPath = helpPath;
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
		
		if(a_absolutePath != null)
		{
			File hpFile = new File(a_absolutePath);
			
			if (a_absolutePath.indexOf(JAPConstants.APPLICATION_CONFIG_DIR_NAME) >= 0)
			{
				// this definitely is a JonDo application folder
				a_bIgnoreExistingHelpDir = true;
			}
			
			if (hpFile.exists())
			{
				if(hpFile.isDirectory())
				{
					strPath = a_absolutePath;
					while ((index = strPath.toLowerCase().lastIndexOf(HELP_FOLDER.toLowerCase())) >= 0)
					{
						if (!new File(strPath.substring(0, index + HELP_FOLDER.length())).exists())
						{
							LogHolder.log(LogLevel.EMERG, LogType.MISC, 
									"Existing help directory was not found!");
						}
						//if (getHelpVersion(m_helpPath + File.separator + HELP_FOLDER) != null)
						if (getHelpVersion(strPath.substring(0, index + HELP_FOLDER.length())) != null)
						{
							return HELP_NESTED;
						}
						strPath = strPath.substring(0, index);
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
					
					
					if (!helpDir.exists())
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
							new File(helpDir.getPath() + File.separator +  HELP_VERSION_FILE);
						if(a_bIgnoreExistingHelpDir || jondoHelpFileVersion.exists())
						{
							try
							{
								if (//helpDir.setLastModified(System.currentTimeMillis()) &&
									!helpDir.canWrite())
								{
									return HELP_INVALID_NOWRITE;	
								}
							}
							catch (SecurityException a_e)
							{
								LogHolder.log(LogLevel.EXCEPTION, LogType.MISC, a_e);
								return HELP_INVALID_NOWRITE;	
							}
							
							try
							{
								if (!helpDir.canRead() || helpDir.list() == null)
								{
									return HELP_INVALID_NOREAD;	
								}
							}
							catch (SecurityException a_e)
							{
								LogHolder.log(LogLevel.EXCEPTION, LogType.MISC, a_e);
								return HELP_INVALID_NOREAD;	
							}								
							
							return HELP_JONDO_EXISTS;
						}
						else
						{
							LogHolder.log(LogLevel.WARNING, LogType.GUI, 
									"Found help directory without this version file: " +
									jondoHelpFileVersion);
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
	
	public boolean extractHelpFiles(String a_extractionPath)
	{
		return extractHelpFiles(a_extractionPath, true);
	}
	
	private boolean extractHelpFiles(String a_extractionPath, boolean bAcceptHelpFolderInPath)
	{
		if (a_extractionPath == null)
		{
			LogHolder.log(LogLevel.ERR, LogType.MISC, 
					"Invalid directory for help extraction: " + a_extractionPath);
			return false;
		}
		
		boolean installationSuccessful = 
			m_archiver.extractArchive(HELP_FOLDER + "/", a_extractionPath);
		if(installationSuccessful)
		{
			createHelpVersionDoc(a_extractionPath);
			return true;
		}
		else
		{
			LogHolder.log(LogLevel.ERR, LogType.MISC, "Extracting help files was not succesful.");
			return false;
		}
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
		
		return extractHelpFiles(m_helpPath, false);
	}
	
	/**
	 * creates an XML document containing the version of the JonDo help which has to match the actual JonDo version
	 */
	private static void createHelpVersionDoc(String a_extractionPath)
	{
		Document helpVersionDoc = XMLUtil.createDocument();
		Element helpVersionNode = helpVersionDoc.createElement(HELP_VERSION_NODE);
		XMLUtil.setAttribute(helpVersionNode, HELP_VERSION_ATTRIBUTE, JAPConstants.aktVersion);
		helpVersionDoc.appendChild(helpVersionNode);
		
		File versionInfoFile = new File(a_extractionPath+File.separator+ HELP_FOLDER + File.separator +HELP_VERSION_FILE);
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
		
		try
		{
			if(!helpFolder.exists() || 
				(!a_bIgnoreExistingHelpDir && !helpVersionFile.exists() &&
						helpFolder.list().length > 0))
			{
				LogHolder.log(LogLevel.INFO, LogType.MISC, "No old help found in " + helpFolder.getPath());
				return true;
			}
		}
		catch (SecurityException a_e)
		{
			LogHolder.log(LogLevel.INFO, LogType.MISC, "No old help found in " + helpFolder.getPath(), a_e);
			return false;
		}
		
		/* Make sure that there will be never the wrong directory as parameter!!! */
		if (!RecursiveFileTool.deleteRecursion(helpFolder))
		{
			// try again
			LogHolder.log(LogLevel.WARNING, LogType.MISC, 
					"Failed to delete old help at first try - try again!");
			RecursiveFileTool.deleteRecursion(helpFolder);
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
		if (helpFolder == null)
		{
			return false;
		}
		if (helpFolder.exists())
		{
			return true;
		}
		LogHolder.log(LogLevel.WARNING, LogType.GUI, "Checked for help folder " + helpFolder +
				" but it did not exist");
		return false;
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
	
	public boolean ensureMostRecentVersion(String helpPath) 
	{
		setHelpPath(helpPath);
		if(helpVersionMismatch() || !isHelpInstalled())
		{
			if (m_helpPath != null && m_helpPath.indexOf(JAPConstants.APPLICATION_CONFIG_DIR_NAME) >= 0)
			{
				// this is definitely a JonDo application folder
				return installHelp(true);
			}
			return installHelp(false);
		}
		return true;
	}
	public boolean helpInstallationExists(String helpPath) {
		
		setHelpPath(helpPath);
		return isHelpInstalled();
	}
}
