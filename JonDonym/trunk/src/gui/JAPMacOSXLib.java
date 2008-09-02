/*
 Copyright (c) 2000 - 2008, The JAP-Team
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
package gui;

import java.awt.EventQueue;
import java.io.File;
import java.util.StringTokenizer;
import java.util.Vector;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JSeparator;

import gui.help.JAPHelp;
import jap.JAPConf;
import jap.JAPController;
import jap.JAPModel;
import jap.SystrayPopupMenu;
import jap.TrustModel;
import anon.infoservice.Database;
import anon.infoservice.MixCascade;
import anon.util.ClassUtil;
import anon.util.ResourceLoader;
import logging.LogHolder;
import logging.LogType;
import logging.LogLevel;

/**
 * Utility class for the JAPMacOSX library
 *
 * @author Christian Banse
 */
public class JAPMacOSXLib
{
	public static final String JAP_MACOSX_LIB_REQUIRED_VERSION = "00.00.002";
	public static final String JAP_MACOSX_LIB_FILENAME = "libJAPMacOSX.jnilib";
	public static final String JAP_MACOSX_LIB_REQUIRED_VERSION_FILENAME = JAP_MACOSX_LIB_FILENAME + "." + JAP_MACOSX_LIB_REQUIRED_VERSION;
	
	private static final String UPDATE_PATH;
	
	private static final String MSG_SETTINGS = SystrayPopupMenu.class.getName() + "_settings";
	private static final String MSG_ANONYMITY_MODE = SystrayPopupMenu.class.getName() + "_anonymityMode";
	private static final String MSG_SHOW_DETAILS = SystrayPopupMenu.class.getName() + "_showDetails";
	
	private JAPMacOSXLib()
	{
		
	}
	
	static
	{
		File japdir = ClassUtil.getClassDirectory(JAPMacOSXLib.class);
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
		
		EventQueue.invokeLater(new Runnable() 
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
					JAPController.getInstance().getView().showConfigDialog(JAPConf.ANON_TAB,
							JAPController.getInstance().getCurrentMixCascade());					
				}
				else if(cmd.equals(MSG_SETTINGS))
				{
					JAPController.getInstance().getView().showConfigDialog(null, null);
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
		if(!load())
		{
			return;
		}
		
		nativeInit();
		nativeInitDockMenu();
		
		System.out.println(getLibVersion());
	}
	
	private static boolean load()
	{
		try
		{
			System.loadLibrary("JAPMacOSX");
		}
		catch(Throwable t)
		{
			LogHolder.log(LogLevel.EXCEPTION, LogType.GUI, "Could not initialise JAPMacOSXLib", t);
			t.printStackTrace();
			
			return false;
		}
		
		return true;
	}
	
	public static void checkLibVersion()
	{
		LogHolder.log(LogLevel.INFO, LogType.GUI, "Existing " + JAP_MACOSX_LIB_FILENAME + " version: " + getLibVersion());
		LogHolder.log(LogLevel.INFO, LogType.GUI, "Required " + JAP_MACOSX_LIB_FILENAME + " version: " + JAP_MACOSX_LIB_REQUIRED_VERSION);
		
		// checks, if the MacOSX library must (and can) be extracted from the jar-file.
		if (getLibVersion() != null && // != null means that there is a loaded library
			getLibVersion().compareTo(JAP_MACOSX_LIB_REQUIRED_VERSION) < 0 &&
			ResourceLoader.getResourceURL(JAP_MACOSX_LIB_REQUIRED_VERSION_FILENAME) != null && // null means there is no new library available
			getUpdatePath() != null)
		{
			System.out.println("needs update + update possible");
		}
		else
		{
			// version status OK
			// OR no library loaded
			// OR no new library in jar-file
			if (JAPModel.getInstance().isMacOSXLibraryUpdated())
			{
				JAPModel.getInstance().setMacOSXLibraryUpdate(false);
				JAPController.getInstance().saveConfigFile();
			}
		}
	}
	
	/**
	 * Returns the path where the dll should be stored into.
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