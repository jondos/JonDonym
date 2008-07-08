package jap;

import gui.JAPMessages;
import gui.dialog.JAPDialog;
import gui.dialog.JAPDialog.ProgressObservableAdapter;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Locale;
import java.util.Observable;
import java.util.Observer;
import java.util.zip.ZipFile;

import javax.swing.JProgressBar;

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
import anon.util.ZipArchiver.ZipEvent;

import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;

/**
 * Handling of the JonDo help files
 * @author Simon Pecher
 *
 */
public final class JAPHelpController implements Observer {
	

	public final static String HELP_VERSION_NODE = "jondohelp";
	public final static String HELP_VERSION_ATTRIBUTE = "version";
	
	private static JAPHelpController helpController = null;
	
	public static final String HELP_FOLDER = "help"+File.separator;
	public static final String HELP_VERSION_FILE = "jondohelp.xml";
	public static final String HELP_START = "index.html";
	
	private static Thread asynchHelpFileInstallThread = null;
	
	JAPModel model = null;
	
	private JAPHelpController(JAPModel japModel)
	{
		if(japModel == null)
		{
			throw new NullPointerException("JAPModel is null: cannot instantiate HelpController");
		}
		model = japModel;
		model.addObserver(this);
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
		File helpFolder = getHelpFolder();
		if(helpFolder == null)
		{
			LogHolder.log(LogLevel.ERR, LogType.MISC, "Fatal: Destination folder is null: Aborting help installation");
		}
		
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
		ProgressObservableAdapter adapter = new HelpInstallProgressAdapter();
		ZipArchiver archiver = new ZipArchiver(japArchive);
		archiver.addObserver(adapter);
		((JAPNewView)JAPController.getInstance().getView()).displayInstallProgress(adapter);
				
		boolean installationSuccessful = archiver.extractArchive(HELP_FOLDER, model.getHelpPath());
		if(installationSuccessful)
		{
			createHelpVersionDoc();
		}
		else
		{
			LogHolder.log(LogLevel.ERR, LogType.MISC, "Extracting help files was not succesful.");
		}
		if(japArchive != null)
		{
			try 
			{
				japArchive.close();
			} 
			catch (IOException e) {
				LogHolder.log(LogLevel.ERR, LogType.MISC, "Could not close Jar-Archive");
			}
		}
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
		
		File versionInfoFile = new File(model.getHelpPath()+File.separator+
				HELP_FOLDER+HELP_VERSION_FILE);
		try
		{
			XMLUtil.write(helpVersionDoc, versionInfoFile);
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
	
	public void removeOldHelp(String parentPath)
	{
		if(parentPath == null)
		{
			return;
		}
		File helpFolder = new File(parentPath+File.separator+HELP_FOLDER);
		File helpVersionFile = new File(parentPath+File.separator+HELP_FOLDER+HELP_VERSION_FILE);
		
		if(!helpFolder.exists() || !helpVersionFile.exists())
		{
			LogHolder.log(LogLevel.INFO, LogType.MISC, "No old help found in "+helpFolder.getPath());
			return;
		}
		/* Make sure that there will be never the wrong directory as parameter!!! */
		RecursiveCopyTool.deleteRecursion(helpFolder);
		LogHolder.log(LogLevel.INFO, LogType.MISC, "removed old help from "+parentPath);
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
			File versionInfoFile = new File(model.getHelpPath()+File.separator+
					HELP_FOLDER+HELP_VERSION_FILE);
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

	public void update(Observable o, Object arg) 
	{
		if( (o instanceof JAPModel) && (arg instanceof String) )
		{
			if(arg != null)
			{
				removeOldHelp((String) arg);
				if(model.isHelpPathDefined())
				{
					synchronized(JAPHelpController.class)
					{
						if((asynchHelpFileInstallThread != null))
						{
							while(asynchHelpFileInstallThread.isAlive())
							{
								try 
								{
									/* It is necessary to wait: when the model is changed the corresponding
									 * changes need to be performed to maintain a valid state.
									 */
									System.out.println("Waiting for previous install thread");
									JAPHelpController.class.wait();
								} 
								catch (InterruptedException e) 
								{}
							}
						}
						asynchHelpFileInstallThread =
							new Thread(
									new Runnable()
									{
										public void run()
										{
											installHelp();
											synchronized(JAPHelpController.class)
											{
												JAPHelpController.class.notifyAll();
											}
										}
									});
						asynchHelpFileInstallThread.start();
					}
				}
			}
		}
	}

	public static synchronized Thread getAsynchHelpFileInstallThread() {
		return asynchHelpFileInstallThread;
	}
	
	public class HelpInstallProgressAdapter extends ProgressObservableAdapter
	{
		
		long m_totalSizeExceedingInt = ZipEvent.UNDEFINED;
		
		public void updateProgress(Observable observable, Object arg,
				JProgressBar progressBar) {
			
			if( !(arg instanceof ZipEvent) )
			{
				return;
			}
			ZipEvent ze = (ZipEvent) arg;
			if(ze.isTotalSizeEvent())
			{
				progressBar.setMinimum(0);
				long totalByteCount = ze.getTotalByteCount();
				if(totalByteCount > Integer.MAX_VALUE)
				{
					m_totalSizeExceedingInt = totalByteCount;
					progressBar.setMaximum(Integer.MAX_VALUE);
				}
				else
				{
					m_totalSizeExceedingInt = ZipEvent.UNDEFINED;
					progressBar.setMaximum((int) totalByteCount);
				}	
			}
			else
			{
				long byteCount = ze.getByteCount();
				String entryName = ze.getZipEntryName();
				if(byteCount != ZipEvent.UNDEFINED)
				{
					if(m_totalSizeExceedingInt != ZipEvent.UNDEFINED)
					{
						double byteCountRatio =
							((double) byteCount) / ((double) m_totalSizeExceedingInt);
						progressBar.setValue((int) (byteCountRatio*Integer.MAX_VALUE));
					}
					else
					{
						progressBar.setValue((int) byteCount);
					}
				}
				/*if(entryName != null)
				{
					m_progressLabel.setText(JAPMessages.getString(MSG_HELP_INSTALL_EXTRACTING)+entryName);
				}
				progressBar.repaint();
				m_progressLabel.repaint();*/
			}
		}
		
	}	
}
