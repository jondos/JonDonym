package jap;

import gui.JAPMessages;
import gui.dialog.JAPDialog;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Locale;
import java.util.zip.ZipFile;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import platform.AbstractOS;

import anon.util.ClassUtil;
import anon.util.RecursiveCopyTool;
import anon.util.XMLParseException;
import anon.util.XMLUtil;
import anon.util.ZLibTools;
import anon.util.ZipArchiver;

import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;

/**
 * Handling of the JonDo help files
 * @author Simon Pecher
 *
 */
public final class JAPHelpController {
	

	public final static String HELP_VERSION_NODE = "jondohelp";
	public final static String HELP_VERSION_ATTRIBUTE = "version";
	
	private static JAPHelpController helpController = null;
	
	public static final String HELP_FOLDER = "help/";
	public static final String HELP_VERSION_FILE = "jondohelp.xml";
	public static final String HELP_START = "index.html";
	private final File VERSION_INFO;
	
	JAPModel model = null;
	
	private JAPHelpController(JAPModel japModel)
	{
		if(japModel == null)
		{
			throw new NullPointerException("JAPModel is null: cannot instantiate HelpController");
		}
		model = japModel;
		VERSION_INFO = new File(model.getHelpPath()+File.separator+
								HELP_FOLDER+HELP_VERSION_FILE);
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
	
	/**
	 * installs the JonDo help externally out the JonDo Jarfile in the specified external destination folder. If no folder 
	 * was specified the help is installed in the same directory where the JAR file is situated. If JonDo is not executed 
	 * from a Jar-Archive the installation process aborts. This routine also performs an installation if there is already 
	 * a help version installed which does not match the JonDo version number (even if the help version is more recent than 
	 * the JonDo version)
	 */
	public void installHelp()
	{
		if(!model.isHelpPathDefined())
		{
			JAPController.getInstance().getView().askForHelpInstallationPath(IJAPMainView.WITH_DIALOG);
			if(!model.isHelpPathDefined())
			{
				boolean stdInstall = 
					JAPDialog.showYesNoDialog((JAPNewView)JAPController.getInstance().getView(), 
											"Could not install in that folder. Do you want to install int help std dir?");
				if(stdInstall == false)
				{
					LogHolder.log(LogLevel.ERR, LogType.MISC, "help was not installed");
					//TODO: Show dialog ?
					return;
				}
												
			}
		}
		
		File helpFolder = getHelpFolder();
		if(helpFolder == null)
		{
			LogHolder.log(LogLevel.ERR, LogType.MISC, "Fatal: Destination folder is null: Aborting help installation");
		}
		/**/
		
		if(helpFolder.exists())
		{	
			String versionString = getHelpVersion();
			
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
		ZipFile japArchive = ClassUtil.getJarFile();
		if(japArchive == null)
		{
			LogHolder.log(LogLevel.WARNING, LogType.MISC, "Not running a jar file: Installing help is not necessary");
			return;
		}
		final JAPHelpProgressDialog hpd = ((JAPNewView)JAPController.getInstance().getView()).displayInstallProgress();
		ZipArchiver archiver = new ZipArchiver(japArchive);
		archiver.addObserver(hpd);
		
		new Thread(
			new Runnable()
			{
				public void run()
				{
					hpd.setVisible();	
				}
			}).start();
		
		boolean installationSuccessful = archiver.extractArchive(HELP_FOLDER, model.getHelpPath());
		if(installationSuccessful)
		{
			createHelpVersionDoc();
		}
		/*if(japArchive != null)
		{
			try 
			{
				japArchive.close();
			} 
			catch (IOException e) {
				LogHolder.log(LogLevel.ERR, LogType.MISC, "Error: Could not close Jar-Archive");
			}
		}*/
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
		
		try
		{
			XMLUtil.write(helpVersionDoc, VERSION_INFO);
		} 
		catch (IOException ioe)
		{
			LogHolder.log(LogLevel.WARNING, LogType.MISC, "Could not write help version due to an I/O error: ", ioe);
		}
	}
	
	/**
	 * Opens the start page of the JonDo help
	 * @return true if the start page could be found, false otherwise
	 */
	public boolean openHelp()
	{
		String helpPath = model.getHelpPath();
		if(helpPath == null)
		{ 
			return false;
		}
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
				new URL("file://"+helpPath+
					 File.separator+
					 HELP_FOLDER+
					 startPath+
					 HELP_START);
			AbstractOS.getInstance().openURL(helpURL);	
		} 
		catch (MalformedURLException e)
		{
			LogHolder.log(LogLevel.WARNING, LogType.MISC, "Malformed URL Excpetion: ", e);
			return false;
		}
		return true;
		
	}
	
	/**
	 * checks if there is a help installed in the specified external help path
	 * @return true if a help folder exists in the user defined help path. If no help 
	 * 			path is specified by the user the default path (the folder where the 
	 * 			Jarfile is situated) is checked.
	 */
	public boolean isHelpInstalled()
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
	public String getHelpVersion()
	{
		try 
		{
			Document doc = XMLUtil.readXMLDocument(VERSION_INFO);
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
	 * conveinience function that returns a file reference to the help root folder
	 * @return a file reference to the help root folder or null if helpPath is null
	 */ 
	public File getHelpFolder()
	{
		String helpPath = model.getHelpPath();
		if(helpPath == null)
		{
			return null;
		}
		return new File(model.getHelpPath()+File.separator+HELP_FOLDER);
	}
	
	public static JAPHelpController getInstance()
	{
		if(helpController == null)
		{
			helpController = new JAPHelpController(JAPModel.getInstance());
		}
		return helpController;
	}
}
