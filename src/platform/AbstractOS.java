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
package platform;

import jap.JAPConstants;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Locale;
import java.util.Vector;
import java.util.jar.JarFile;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import anon.util.ClassUtil;
import anon.util.RecursiveCopyTool;
import anon.util.XMLParseException;
import anon.util.XMLUtil;
import anon.util.ZLibTools;

import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;
import gui.JAPMessages;
import gui.JAPHelp.IExternalURLCaller;
import gui.JAPHelp.IExternalEMailCaller;
import gui.dialog.JAPDialog;


/**
 * This abstract class provides access to OS-specific implementations of certain
 * functions. It tries to instantiate an OS-specific class by determining on which
 * operating system JAP is currently running.
 */
public abstract class AbstractOS implements IExternalURLCaller, IExternalEMailCaller
{
	public static final String URL_MAIL_TO = "mailto:";

	/**
	 * Make sure that the default OS is the last OS in the array.
	 */
	private static Class[] REGISTERED_PLATFORM_CLASSES =
		{
		LinuxOS.class, WindowsOS.class, MacOS.class, UnknownOS.class};

	private static final String[] BROWSERLIST =
		{
		"firefox", "iexplore", "explorer", "mozilla", "konqueror", "mozilla-firefox", "opera"
	};

	/**
	 * The instanciated operation system class.
	 * (no, ms_operating system does not mean only Microsoft OS are supported... ;-))
	 */
	private static AbstractOS ms_operatingSystem;

	private IURLErrorNotifier m_notifier;
	private IURLOpener m_URLOpener;

	private static File ms_tmpDir;

	public final static String HELP_VERSION_NODE = "jondohelp";
	public final static String HELP_VERSION_ATTRIBUTE = "version";
	
	static
	{
		// Needs to be done according to the JDK because java.io.tmpdir
		// seems to return wrong values on some Linux and Solaris systems.
		String tmpDir = System.getProperty("java.io.tmpdir");
		if(tmpDir.compareTo("/var/tmp/") == 0)
			tmpDir = "/tmp/";

		// Assure that the tmpDir has a trailing File.seperator
		if(tmpDir.lastIndexOf(File.pathSeparator) != (tmpDir.length() - 1))
			tmpDir = tmpDir + File.separator;

		ms_tmpDir = new File(tmpDir);
	}

	public static interface IURLOpener
	{
		boolean openURL(URL a_url);
	}

	public static interface IURLErrorNotifier
	{
		void checkNotify(URL a_url);
	}

	/**
	 * Instantiates an OS-specific class. If no specific class is found, the default OS
	 * (which is a dummy implementation) is instanciated.
	 * @return the instanciated operating system class
	 */
	public static final AbstractOS getInstance()
	{
		for (int i = 0; ms_operatingSystem == null && i < REGISTERED_PLATFORM_CLASSES.length; i++)
		{
			try
			{
				ms_operatingSystem =
					(AbstractOS) REGISTERED_PLATFORM_CLASSES[i].newInstance();
			}
			catch (Exception a_e)
			{
				LogHolder.log(LogLevel.DEBUG, LogType.MISC,
							  "Cannot instantiate class " + REGISTERED_PLATFORM_CLASSES[i] +
							  ". Trying to instanciate another platform class.");
			}
			if (ms_operatingSystem != null)
			{
				ms_operatingSystem.m_notifier = new IURLErrorNotifier()
				{
					public void checkNotify(URL a_url)
					{
						// do nothing
					}
				};
			}
		}

		return ms_operatingSystem;
	}

	public void init(IURLErrorNotifier a_notifier, IURLOpener a_URLOpener)
	{
		if (a_notifier != null)
		{
			m_notifier = a_notifier;
		}
		if (a_URLOpener != null)
		{
			m_URLOpener = a_URLOpener;
		}
	}

	public JAPDialog.ILinkedInformation createURLLink(final URL a_url, final String a_optionalText)
	{
		return createURLLink(a_url, a_optionalText, null);
	}

	public JAPDialog.ILinkedInformation createURLLink(final URL a_url, final String a_optionalText,
		String a_helpContext)
	{
		if (a_url == null)
		{
			return null;
		}

		JAPDialog.ILinkedInformation link = new JAPDialog.LinkedHelpContext(a_helpContext)
		{
			public int getType()
			{
				return JAPDialog.ILinkedInformation.TYPE_LINK;
			}
			public void clicked(boolean a_bState)
			{
				openURL(a_url);
			}
			public String getMessage()
			{
				if (a_optionalText == null || a_optionalText.trim().length() == 0)
				{
					return a_url.toString();
				}
				else
				{
					return a_optionalText;
				}
			}
		};

		return link;
	}

	public final boolean openEMail(String a_mailto)
	{
		if (a_mailto == null)
		{
			return false;
		}
		if (!a_mailto.startsWith(URL_MAIL_TO))
		{
			return openLink(URL_MAIL_TO + a_mailto);
		}
		else
		{
			return openLink(a_mailto);
		}
	}

	public final boolean openURL(URL a_url)
	{
		boolean success = false;
		if (a_url == null)
		{
			return false;
		}

		String[] browser = BROWSERLIST;
		String url = getAsString(a_url);

		m_notifier.checkNotify(a_url);

		if (m_URLOpener != null)
		{
			success = m_URLOpener.openURL(a_url);
		}
		if (!success)
		{
			success = openLink(url);
		}
		if (!success)
		{
			for (int i = 0; i < browser.length; i++)
			{
				try
				{
					Runtime.getRuntime().exec(new String[]{browser[i], url});
					success = true;
					break;
				}
				catch (Exception ex)
				{
				}
			}
		}

		if (!success)
		{
			LogHolder.log(LogLevel.ERR, LogType.MISC, "Cannot open URL in browser");
		}
		return success;
	}

	/**
	 * Implementations must return a valid path to the config file.
	 */
	public abstract String getConfigPath();

	protected abstract boolean openLink(String a_link);

	protected String getAsString(URL a_url)
	{
		if (a_url == null)
		{
			return null;
		}
		return a_url.toString();
	}

	/**
	 * Reads the version string out of the help version XML file
	 * The help version number corresponds to the version number of the JonDo
	 * 
	 * @param versionFile the XML file where the help version is specified.
	 * @return the help version number as string, or null if no version string was found in versionFile
	 */
	public static String getHelpVersion(File versionFile)
	{
		try 
		{
			Document doc = XMLUtil.readXMLDocument(versionFile);
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
	 * returns the path to the folder which contains the Jarfile or null if JonDo is not executed from a Jar file
	 * @return the path to the folder which contains the Jarfile or null if JonDo is not executed from a Jar file
	 */
	public String getDefaultHelpPath()
	{
		File classParentFile = ClassUtil.getClassDirectory(this.getClass());
		if(classParentFile!= null)
		{
			String path = classParentFile.getPath();
			int p_index = path.indexOf("JAP.jar");
			return (p_index == -1) ? path : path.substring(0, p_index);
		}
		return null;
	}
	
	/**
	 * returns a handle to the help parent folder based on the default help parent folder
	 * @return a handle to the help parent folder based on the default help parent folder
	 */
	public File getDefaultHelpFolder()
	{
		return new File(getDefaultHelpPath()+File.separator+
				JAPConstants.HELP_FOLDER);
	}
	
	/**
	 * checks if there is a help installed in the specified external help path
	 * @return true if a help folder exists in the user defined help path. If no help 
	 * 			path is specified by the user the default path (the folder where the 
	 * 			Jarfile is situated) is checked.
	 */
	public boolean isHelpInstalled()
	{
		return getDefaultHelpFolder().exists();
	}
	
	public String getHelpPath()
	{
		//TODO: can be specified by user
		return getDefaultHelpPath();
	}
	
	public File getHelpFolder()
	{
		//TODO: can be specified by user
		return getDefaultHelpFolder();
	}
	
	/**
	 * checks whether the help version number matches the JonDo version number
	 * @return true if and only if the stored help version number is exactly the same 
	 * 			as the JonDo version number
	 */
	public boolean helpVersionMismatch()
	{
		File versionFile = 
			new File(getDefaultHelpPath()+File.separator+
				JAPConstants.HELP_FOLDER+
				JAPConstants.HELP_VERSION_FILE);
		String versionString = getHelpVersion(versionFile);
		return !JAPConstants.aktVersion.equals(versionString);
	}
	
	/**
	 * installs the JonDo help externally out the JonDo Jarfile in the specified external destination folder. If no folder 
	 * was specified the help is installed in the same directory where the JAR file is situated. If JonDo is not executed 
	 * from a Jar-Archive the installation process aborts. This routibne also performs an installation if there is already 
	 * a help version installed which does not match the JonDo version number (even if the help version is more recent than 
	 * the JonDo version)
	 */
	public void installHelp()
	{
		File helpFolder = getHelpFolder();
		File versionFile = 
			new File(getDefaultHelpPath()+File.separator+
				JAPConstants.HELP_FOLDER+
				JAPConstants.HELP_VERSION_FILE);
		
		if(helpFolder.exists())
		{	
			String versionString = getHelpVersion(versionFile);
			
			if(!helpVersionMismatch())
			{
				LogHolder.log(LogLevel.WARNING, LogType.MISC, "Help is already installed!");
				return;
			}
			else
			{
				LogHolder.log(LogLevel.DEBUG, LogType.MISC, "Removing old help");
				RecursiveCopyTool.deleteRecursion(helpFolder);
			}
		}
		// We can go on extracting the help from the JarFile if necessary
		JarFile japArchive = getJarFile();
		if(japArchive == null)
		{
			LogHolder.log(LogLevel.WARNING, LogType.MISC, "Not running a jar file: Installing help is not necessary");
			return;
		}
		boolean installationSuccessful = ZLibTools.extractArchive(japArchive, JAPConstants.HELP_FOLDER, getHelpPath());
		if(installationSuccessful)
		{
			Document helpVersionDoc = createHelpVersionDoc();
			try
			{
				XMLUtil.write(helpVersionDoc, versionFile);
			} 
			catch (IOException ioe)
			{
				LogHolder.log(LogLevel.WARNING, LogType.MISC, "Could not write help version due to an I/O error: ", ioe);
			}
		}
	}
	
	/**
	 * creates an XML document containing the version of the JonDo help which has to match the actual JonDo version
	 * @return an XML document containing the version of the JonDo help
	 */
	private Document createHelpVersionDoc()
	{
		Document helpVersionDoc = XMLUtil.createDocument();
		Element helpVersionNode = helpVersionDoc.createElement(HELP_VERSION_NODE);
		XMLUtil.setAttribute(helpVersionNode, HELP_VERSION_ATTRIBUTE, JAPConstants.aktVersion);
		helpVersionDoc.appendChild(helpVersionNode);
		return helpVersionDoc;
	}
	
	/**
	 * Opens the start page of the JonDo help
	 * @return true if the start page could be found, false otherwise
	 */
	public boolean openHelp()
	{
		if(!isHelpInstalled() || helpVersionMismatch())
		{
			installHelp();
		}
		Locale loc = JAPMessages.getLocale();
		String startPath =
			loc.toString().equalsIgnoreCase("de") ? "de/help/" : "en/help/";
		if(!isHelpInstalled()) 
		{	
			return false;
		}
		try
		{
			URL helpURL = 
				new URL("file://"+getHelpPath()+
					 File.separator+
					 JAPConstants.HELP_FOLDER+
					 startPath+
					 JAPConstants.HELP_START);
			openURL(helpURL);	
		} 
		catch (MalformedURLException e)
		{
			LogHolder.log(LogLevel.WARNING, LogType.MISC, "Malformed URL Excpetion: ", e);
			return false;
		}
		return true;
		
	}
	
	/**
	 * returns a handle to the JAP.jar or null if JAP is not started as jar-file
	 * @returns a handle to the JAP.jar or null if JAP is not started as jar-file
	 */
	public JarFile getJarFile()
	{
		File classParentFile = ClassUtil.getClassDirectory(this.getClass());
		if(classParentFile != null)
		{
			if(classParentFile.getPath().endsWith(".jar"))
				try
				{
					return new JarFile(classParentFile);
				} 
				catch (IOException ioe)
				{
					LogHolder.log(LogLevel.ERR, LogType.MISC, "An I/O error occured while opening the JAR file: ", ioe);
				}
		}
		return null;
	}
	
	/**
	 * Returns a vector of all running VMs. This only works on the Sun VM
	 * @return a vector of all running Virtual Machines
	 */
	public Vector getActiveVMs()
	{
		Vector r_vms = new Vector();
		int id = 0;

		if(!ms_tmpDir.isDirectory())
			return r_vms;

		// Loop through all directories that match the filter
		String[] dirs = ms_tmpDir.list(new FilenameFilter()
		{
			public boolean accept(File a_dir, String a_name)
			{
				return a_name.startsWith("hsperfdata_");
			}
		});
		if(dirs == null) return r_vms;

		for(int i = 0; i < dirs.length; i++)
		{
			File dir = new File(ms_tmpDir + File.separator + dirs[i]);
			if(!dir.isDirectory())
				continue;

			// Loop through all files in the directory. Each file represents one VM
			String[] files = dir.list();

			if(files != null)
			{
				for(int j = 0; j < files.length; j++)
				{
					File file = new File(dir + File.separator + files[j]);
					if(file.isFile() && file.canRead())
					{
						try
						{
							if((id = Integer.parseInt(file.getName())) != 0)
								r_vms.addElement(new VMPerfDataFile(id));
						}
						catch(NumberFormatException e) { continue; }
					}
				}
			}
		}

		return r_vms;
	}
}
