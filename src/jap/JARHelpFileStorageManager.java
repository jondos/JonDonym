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

import gui.JAPMessages;
import gui.dialog.JAPDialog;
import gui.dialog.JAPDialog.ProgressObservableAdapter;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Locale;
import java.util.Observable;
import java.util.Observer;
import java.util.zip.ZipFile;

import javax.swing.JProgressBar;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import platform.AbstractOS;

import anon.util.ClassUtil;
import anon.util.ProgressCapsule;
import anon.util.RecursiveCopyTool;
import anon.util.XMLParseException;
import anon.util.XMLUtil;
import anon.util.ZLibTools;
import anon.util.ZipArchiver;
import anon.util.ZipArchiver.ZipEvent;

import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;

/**
 * Handling of the JonDo help files
 * @author Simon Pecher
 *
 */
public final class JARHelpFileStorageManager implements HelpFileStorageManager  {
	

	public final static String HELP_VERSION_NODE = "jondohelp";
	public final static String HELP_VERSION_ATTRIBUTE = "version";
	
	private static JARHelpFileStorageManager helpController = null;
	
	
	public static final String HELP_VERSION_FILE = "jondohelp.xml";
	
	
	private static Thread asynchHelpFileInstallThread = null;
	private String helpPath;
	private ZipArchiver archiver;
	
	public JARHelpFileStorageManager()
	{
		ZipFile japArchive = ClassUtil.getJarFile();
		if(japArchive == null)
		{
			//TODO: Throw something
			//throw new
		}
		archiver = new ZipArchiver(japArchive);
	}
	/**
	 * sets the specified path for external installation of the help files
	 * performs no validityCheck, because it is only called by
	 * handleHelpPathChanged, which is invkoed by the JAPModel, which itself 
	 * performs a validity check
	 */
	private void setHelpPath(String helpPath)
	{
		this.helpPath = helpPath;
	}
	
	/**
	 * checks whether the help version number matches the JonDo version number
	 * @return true if and only if the stored help version number is exactly the same 
	 * 			as the JonDo version number
	 */
	public boolean helpVersionMismatch()
	{
		String versionString = getHelpVersion();
		return !JAPConstants.aktVersion.equals(versionString);
	}
	
	
	public boolean handleHelpPathChanged(String oldHelpPath, String newHelpPath)
	{
		boolean installationSuccessful = true;
		setHelpPath(newHelpPath);
		if(oldHelpPath != null)
		{
			removeOldHelp(oldHelpPath);
		}
		if(newHelpPath != null)
		{
			installationSuccessful = installHelp();
		}
		return installationSuccessful;
	}
	
	public String helpPathValidityCheck(String absolutePath) 
	{
		if(absolutePath != null)
		{
			File hpFile = new File(absolutePath);
			if(hpFile.exists())
			{
				if(hpFile.isDirectory())
				{
					if(hpFile.canWrite())
					{
						File anotherFileWithSameName =
							new File(hpFile.getPath()+
									File.separator+
									JAPConstants.HELP_FOLDER);
						
						if(!anotherFileWithSameName.exists())
						{
							return HELP_VALID;
						}
						else
						{
							//help directory already exists in specified folder
							File jondoHelpFileVersion =
								new File(hpFile.getPath()+
										File.separator+
										JAPConstants.HELP_FOLDER+
										File.separator+
										HELP_VERSION_FILE);
							if(jondoHelpFileVersion.exists())
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
						//we have no write access to that file
						return HELP_INVALID_NOWRITE;
					}
				}
				else
				{
					//path is not a directory
					return "TODO: Error message key";
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
		return archiver;
	}
	
	/**
	 * installs the JonDo help externally out the JonDo Jarfile in the specified external destination folder. 
	 * This routine overrides already installed JonDo help files.
	 */
	private boolean installHelp()
	{
		File helpFolder = getHelpFolder();
		if(helpFolder == null)
		{
			LogHolder.log(LogLevel.ERR, LogType.MISC, "Fatal: Destination folder is null: Aborting help installation");
			return false;
		}
		if(archiver == null)
		{
			LogHolder.log(LogLevel.ERR, LogType.MISC, "Fatal: JARStorageManager does only work when started from a Jar file");
			return false;
		}
		//override old help
		if(helpFolder.exists())
		{	
			removeOldHelp(helpPath);
			if(helpFolder.exists())
			{
				return false;
			}
		}
			
		boolean installationSuccessful = archiver.extractArchive(JAPConstants.HELP_FOLDER, helpPath);
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
	 * @return an XML document containing the version of the JonDo help
	 */
	private void createHelpVersionDoc()
	{
		Document helpVersionDoc = XMLUtil.createDocument();
		Element helpVersionNode = helpVersionDoc.createElement(HELP_VERSION_NODE);
		XMLUtil.setAttribute(helpVersionNode, HELP_VERSION_ATTRIBUTE, JAPConstants.aktVersion);
		helpVersionDoc.appendChild(helpVersionNode);
		
		File versionInfoFile = 
			new File(helpPath+File.separator+
					JAPConstants.HELP_FOLDER+HELP_VERSION_FILE);
		try
		{
			XMLUtil.write(helpVersionDoc, versionInfoFile);
		} 
		catch (IOException ioe)
		{
			LogHolder.log(LogLevel.WARNING, LogType.MISC, "Could not write help version due to an I/O error: ", ioe);
		}
	}
	
	private void removeOldHelp(String parentPath)
	{
		if(parentPath == null)
		{
			return;
		}
		File helpFolder = new File(parentPath+File.separator+JAPConstants.HELP_FOLDER);
		File helpVersionFile = new File(parentPath+File.separator+JAPConstants.HELP_FOLDER+HELP_VERSION_FILE);
		
		if(!helpFolder.exists() || !helpVersionFile.exists())
		{
			LogHolder.log(LogLevel.INFO, LogType.MISC, "No old help found in "+helpFolder.getPath());
			return;
		}
		/* Make sure that there will be never the wrong directory as parameter!!! */
		RecursiveCopyTool.deleteRecursion(helpFolder);
		LogHolder.log(LogLevel.DEBUG, LogType.MISC, "removed old help from "+parentPath);
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
	private String getHelpVersion()
	{
		try 
		{
			File versionInfoFile = new File(helpPath+File.separator+
					JAPConstants.HELP_FOLDER+HELP_VERSION_FILE);
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
		if(helpPath == null)
		{
			return null;
		}
		return new File(helpPath+File.separator+JAPConstants.HELP_FOLDER);
	}
	
	public void ensureMostRecentVersion(String helpPath) 
	{
		setHelpPath(helpPath);
		if(helpVersionMismatch() || !isHelpInstalled())
		{
			installHelp();
		}
	}
	public boolean helpInstallationExists(String helpPath) {
		
		setHelpPath(helpPath);
		return isHelpInstalled();
	}
}
