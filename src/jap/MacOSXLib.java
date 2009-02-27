/*
Copyright (c) 2008 The JAP-Team, JonDos GmbH

All rights reserved.

Redistribution and use in source and binary forms, with or without modification, 
are permitted provided that the following conditions are met:

    * Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
    * Redistributions in binary form must reproduce the above copyright notice,
       this list of conditions and the following disclaimer in the documentation and/or
       other materials provided with the distribution.
    * Neither the name of the University of Technology Dresden, Germany, nor the name of
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

import java.awt.Window;
import java.io.File;
import java.io.FileOutputStream;
import java.util.StringTokenizer;
import java.util.Vector;

import javax.swing.SwingUtilities;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JSeparator;

import gui.JAPHelpContext;
import gui.JAPMessages;
import gui.dialog.JAPDialog;
import gui.help.JAPHelp;
import anon.infoservice.Database;
import anon.infoservice.MixCascade;
import anon.util.ClassUtil;
import anon.util.ResourceLoader;
import anon.util.Util;
import logging.LogHolder;
import logging.LogType;
import logging.LogLevel;

/**
 * Utility class for the JAPMacOSX library
 *
 * @author Christian Banse
 */
public class MacOSXLib
{
	public static final String JAP_MACOSX_LIB_REQUIRED_VERSION = "00.00.005";
	public static final String JAP_MACOSX_LIB = "MacOSX";
	public static final String JAP_MACOSX_LIB_FILENAME = "lib" + JAP_MACOSX_LIB + ".jnilib";
	private static final String JAP_MACOSX_LIB_OLD_FILENAME = JAP_MACOSX_LIB_FILENAME + ".old";
	public static final String JAP_MACOSX_LIB_REQUIRED_VERSION_FILENAME = JAP_MACOSX_LIB_FILENAME + "." + JAP_MACOSX_LIB_REQUIRED_VERSION;
	
	private static final String MSG_MACOSX_LIB_UPDATE = MacOSXLib.class.getName() + "_macOSXLibUpdate";
	private static final String UPDATE_PATH;
	
	private static final String MSG_SETTINGS = SystrayPopupMenu.class.getName() + "_settings";
	private static final String MSG_ANONYMITY_MODE = SystrayPopupMenu.class.getName() + "_anonymityMode";
	private static final String MSG_SHOW_DETAILS = SystrayPopupMenu.class.getName() + "_showDetails";
	
	private static boolean ms_bLibraryLoaded = false;
	
	private MacOSXLib()
	{
		
	}
	
	static
	{
		File japdir = ClassUtil.getClassDirectory(MacOSXLib.class);
		if (japdir == null)
		{
			String strUpdatePath = null;
			// the update method might not work; maybe this is Java Webstart?
			try
			{
				strUpdatePath = System.getProperty("user.dir", (String)null);
			}
			catch (Throwable a_e)
			{
				a_e.printStackTrace();
			}
			UPDATE_PATH = strUpdatePath;
		}
		else
		{
			UPDATE_PATH = japdir.getParent();
		}
	}
	
	public static void dockMenuCallback(String a_command)
	{
		final String cmd = a_command;
		
		SwingUtilities.invokeLater(new Runnable() 
		{
			public void run() 
			{
				if(cmd.equals(MSG_ANONYMITY_MODE))
				{
					if(JAPController.getInstance().getAnonMode())
					{
						JAPController.getInstance().setAnonMode(false);
					}
					else
					{
						JAPController.getInstance().setAnonMode(true);
					}
				}
				else if(cmd.equals(MSG_SHOW_DETAILS))
				{
					JAPController.getInstance().showConfigDialog(JAPConf.ANON_TAB,
							JAPController.getInstance().getCurrentMixCascade());					
				}
				else if(cmd.equals(MSG_SETTINGS))
				{
					JAPController.getInstance().showConfigDialog();
				}
				else if(cmd.equals(JAPHelp.MSG_HELP_MENU_ITEM))
				{
					JAPHelp help = JAPHelp.getInstance();
					help.setContext(JAPHelpContext.createHelpContext("index", 
							JAPController.getInstance().getViewWindow()));
					help.loadCurrentContext();
				}
				else
				{
					// try to split the action command in trustmodel and cascade id
					StringTokenizer tokenizer = new StringTokenizer(cmd,",");
					if(tokenizer.countTokens() == 2)
					{
						long model = Long.parseLong(tokenizer.nextToken());
						
						TrustModel.setCurrentTrustModel(model);
						
						MixCascade cascade = (MixCascade) Database.getInstance(MixCascade.class).getEntryById(tokenizer.nextToken());
						
						if(cascade != null)
						{
							JAPController.getInstance().setCurrentMixCascade(cascade);
						}
					}
				}
			}
		});
	}
	
	public static void init()
	{
		// library needs update at startup, we'll deal with the error handling inside
		// checkLibVersion()
		if(getUpdatePath() != null && JAPModel.getInstance().isMacOSXLibraryUpdateAtStartupNeeded())
		{
			update();
		}
		
		load();
		
		checkLibVersion();
		
		if(ms_bLibraryLoaded)
		{
			nativeInit();
			nativeInitDockMenu();
		}
	}
	
	private static void load()
	{
		try
		{
			System.loadLibrary(JAP_MACOSX_LIB);
			ms_bLibraryLoaded = true;
		}
		catch(Throwable t)
		{
			LogHolder.log(LogLevel.EXCEPTION, LogType.GUI, "Could not initialise MacOSXLib", t);
			ms_bLibraryLoaded = false;
		}
	}
	
	public static void checkLibVersion()
	{
		boolean bUpdateNeeded = false;
		String version = null;
		
		if(ms_bLibraryLoaded)
		{
			try
			{
				version = getLibVersion();
				LogHolder.log(LogLevel.INFO, LogType.GUI, "Existing " + JAP_MACOSX_LIB_FILENAME + " version: " + version);
			}
			catch(Throwable t)
			{
				LogHolder.log(LogLevel.INFO, LogType.GUI, JAP_MACOSX_LIB_FILENAME + " does not support version check. Update needed.");
				bUpdateNeeded = true;
			}
		}
		else
		{
			LogHolder.log(LogLevel.INFO, LogType.GUI, JAP_MACOSX_LIB_FILENAME + " does not exist or failed to load. Update needed.");
			bUpdateNeeded = true;
		}
		
		LogHolder.log(LogLevel.INFO, LogType.GUI, "Required " + JAP_MACOSX_LIB_FILENAME + " version: " + JAP_MACOSX_LIB_REQUIRED_VERSION);
		
		if(version != null &&
			version.compareTo(JAP_MACOSX_LIB_REQUIRED_VERSION) < 0)
		{
			bUpdateNeeded = true;
		}

		// we already tried an update at startup and it failed
		if(bUpdateNeeded && JAPModel.getInstance().isMacOSXLibraryUpdateAtStartupNeeded())
		{
			// give up
			LogHolder.log(LogLevel.INFO, LogType.GUI, "Update failed twice. Giving up.");			
			JAPModel.getInstance().setMacOSXLibraryUpdateAtStartupNeeded(false);
			JAPController.getInstance().saveConfigFile();
			return;
		}
		
		if(bUpdateNeeded)
		{
			LogHolder.log(LogLevel.INFO, LogType.GUI, "Trying to fetch " + JAP_MACOSX_LIB_REQUIRED_VERSION_FILENAME + " from JAP.jar.");
			if(ResourceLoader.getResourceURL(JAP_MACOSX_LIB_REQUIRED_VERSION_FILENAME) != null &&
			   getUpdatePath() != null)
			{
				if(update())
				{
					// update successful, don't need to update at startup
					JAPModel.getInstance().setMacOSXLibraryUpdateAtStartupNeeded(false);
					JAPController.getInstance().saveConfigFile();
					
					// try to load the new library
					// this probably fails every time, but ah well maybe we're lucky
					// the only time this won't fail is if there was no old library
					load();
					try
					{
						version = getLibVersion();
					}
					catch(Throwable t)
					{
						version = null;
					}
					
					// the new library was loaded - whoa! how did that happen?!
					if(version != null &&
						version.compareTo(JAP_MACOSX_LIB_REQUIRED_VERSION) >= 0)
					{
						LogHolder.log(LogLevel.INFO, LogType.GUI, JAP_MACOSX_LIB_FILENAME + " successfully updated to version " + JAP_MACOSX_LIB_REQUIRED_VERSION + ".");
						return;
					}
					else
					{
						// this is usually the case... update successful 
						// but we need a restart for the new library to load
						LogHolder.log(LogLevel.INFO, LogType.GUI, JAP_MACOSX_LIB_FILENAME + " successfully updated to version " + JAP_MACOSX_LIB_REQUIRED_VERSION + ". Restart needed.");
						informUserAboutJapRestart();
					}
				}
				
				// update failed, try again at next startup
				LogHolder.log(LogLevel.INFO, LogType.GUI, "Update failed, trying to restart JAP to retry update.");
				JAPModel.getInstance().setMacOSXLibraryUpdateAtStartupNeeded(true);
				JAPController.getInstance().saveConfigFile();
				informUserAboutJapRestart();
			}
			else
			{
				LogHolder.log(LogLevel.INFO, LogType.GUI, "Required version not available in JAP.jar. Update aborted.");
				return;
			}
		}
		else
		{
			if (JAPModel.getInstance().isMacOSXLibraryUpdateAtStartupNeeded())
			{
				JAPModel.getInstance().setMacOSXLibraryUpdateAtStartupNeeded(false);
				JAPController.getInstance().saveConfigFile();
			}
		}
	}
	
	private static boolean update()
	{
		LogHolder.log(LogLevel.INFO, LogType.GUI, "Trying to update " + JAP_MACOSX_LIB_FILENAME + " to version " + JAP_MACOSX_LIB_REQUIRED_VERSION + ".");

		if (renameLib(JAP_MACOSX_LIB_FILENAME, JAP_MACOSX_LIB_OLD_FILENAME) && extractDLL(new File(getLibFileName())))
		{
			JAPModel.getInstance().setMacOSXLibraryUpdateAtStartupNeeded(false);
			JAPController.getInstance().saveConfigFile();
			
			return true;
		}
		else
		{
			renameLib(JAP_MACOSX_LIB_OLD_FILENAME, JAP_MACOSX_LIB_FILENAME);
			return false;
		}
	}
	
	/**
	 * Returns the path where the library should be stored into.
	 * @return
	 */
	private static String getUpdatePath()
	{
		String file = getLibFileName();
		if (file != null)
		{
			file = (new File(file)).getParent();
		}
		return file;
	}
	
	static public String getLibFileName()
	{
		if (UPDATE_PATH != null)
		{
			if (!UPDATE_PATH.endsWith(File.separator))
			{
				return UPDATE_PATH + File.separator + JAP_MACOSX_LIB_FILENAME;
			}
			else
			{
				return UPDATE_PATH + JAP_MACOSX_LIB_FILENAME;
			}			
		}
		return null;
	}
	
	private static boolean renameLib(String a_oldName, String a_newName)
	{
		try
		{
			File file = new File(getUpdatePath() + File.separator + a_oldName);
			if(file.exists())
			{
				file.renameTo(new File(getUpdatePath() + File.separator + a_newName));
				return true;
			}
			else 
			{
				// the file doesn't exist but return true so we can extract
				// the library file from the jar
				return true;
			}

		}
		catch (Exception e)
		{
			LogHolder.log(LogLevel.ERR, LogType.GUI, "Unable to copy " + getUpdatePath() + 
					File.separator + a_oldName + ".", e);
		}
		
		return false;
   }
	
	private static boolean extractDLL(File a_file)
	{
		LogHolder.log(LogLevel.DEBUG, LogType.GUI, "Extracting " + JAP_MACOSX_LIB_REQUIRED_VERSION_FILENAME + 
				" from jar-file to: " + a_file);

		try
		{
			Util.copyStream(
					ResourceLoader.loadResourceAsStream(JAP_MACOSX_LIB_REQUIRED_VERSION_FILENAME),
					new FileOutputStream(a_file));
			
			return true;
		}
		catch (Exception e)
		{
			LogHolder.log(LogLevel.EXCEPTION, LogType.MISC, e);
		}
		return false;
	}
	
	private static void informUserAboutJapRestart()
	{
		// inform the User about the necessary JAP restart
		JAPDialog.showMessageDialog(JAPController.getInstance().getCurrentView(),
									JAPMessages.getString(MSG_MACOSX_LIB_UPDATE));
		// close JAP
		JAPController.goodBye(false);
	}
	
	public static JMenu showDockMenu()
	{	
		JMenu menu = new JMenu();
		JMenuItem item;
		JCheckBoxMenuItem chk;
		
		chk = new JCheckBoxMenuItem(JAPMessages.getString(MSG_ANONYMITY_MODE));
		chk.setSelected(JAPController.getInstance().getAnonMode());
		chk.setActionCommand(MSG_ANONYMITY_MODE);
		menu.add(chk);
		
		item = new JMenuItem(JAPMessages.getString(MSG_SHOW_DETAILS));
		item.setActionCommand(MSG_SHOW_DETAILS);
		menu.add(item);
		
		menu.add(new JSeparator());
		
		item = new JMenuItem(JAPMessages.getString(MSG_SETTINGS));
		item.setActionCommand(MSG_SETTINGS);
		menu.add(item);
		
		item = new JMenuItem(JAPMessages.getString(JAPHelp.MSG_HELP_MENU_ITEM));
		item.setActionCommand(JAPHelp.MSG_HELP_MENU_ITEM);
		menu.add(item);
		
		menu.add(new JSeparator());
		
		Vector vec = TrustModel.getTrustModels();
		
		for(int i = 0; i < vec.size(); i++)
		{
			TrustModel model = (TrustModel) vec.elementAt(i);
			if(model.isAdded())
			{
				JMenu sub;
				
				if(model == TrustModel.getCurrentTrustModel())
				{
					sub = new JMenu(model.getName() + " (" + JAPMessages.getString("active") + ")");
				}
				else
				{
					sub = new JMenu(model.getName());
				}
				
				Vector cascades = Database.getInstance(MixCascade.class).getEntryList();
				for(int j = 0; j < cascades.size(); j++)
				{
					MixCascade cascade = (MixCascade) cascades.elementAt(j);
					
					if(!model.isTrusted(cascade))
					{
						continue;
					}
					
					item = new JMenuItem(cascade.getName());
					if(JAPController.getInstance().getCurrentMixCascade() == cascade)
					{
						item.setSelected(true);
					}
					item.setActionCommand(model.getId() + "," + cascade.getId());
					sub.add(item);
				}
				
				menu.add(sub);
			}
		}
		
		return menu;
	}
	
	private static native void nativeInit();
	private static native void nativeInitDockMenu();
	private static native String getLibVersion();
}