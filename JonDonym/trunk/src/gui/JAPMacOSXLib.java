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

import gui.help.JAPHelp;
import jap.JAPConf;
import jap.JAPController;
import jap.SystrayPopupMenu;
import jap.TrustModel;
import anon.infoservice.Database;
import anon.infoservice.MixCascade;

import java.awt.EventQueue;
import java.util.Vector;

import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JSeparator;

/**
 * Utility class for the JAPMacOSX library
 *
 * @author Christian Banse
 */
public class JAPMacOSXLib
{
	private static final String MSG_SETTINGS= SystrayPopupMenu.class.getName() + "_settings";
	private static final String MSG_ANONYMITY_MODE = SystrayPopupMenu.class.getName() + "_anonymityMode";
	private static final String MSG_SHOW_DETAILS = SystrayPopupMenu.class.getName() + "_showDetails";
	
	private JAPMacOSXLib()
	{
		
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
					// action command is probably a cascade id
					MixCascade cascade = (MixCascade) Database.getInstance(MixCascade.class).getEntryById(cmd);
					
					if(cascade != null)
					{
						// TODO: switch trustmodels
						JAPController.getInstance().setCurrentMixCascade(cascade);
					}
				}
			}
		});
	}
	
	public static void init()
	{
		try
		{
			System.loadLibrary("JAPMacOSX");
			nativeInit();
			nativeInitDockMenu();
		}
		catch(Exception ex)
		{
			ex.printStackTrace();
		}
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
				JMenu sub = new JMenu(model.getName());
				
				Vector cascades = Database.getInstance(MixCascade.class).getEntryList();
				for(int j = 0; j < cascades.size(); j++)
				{
					MixCascade cascade = (MixCascade) cascades.elementAt(j);
					
					if(!model.isTrusted(cascade))
					{
						continue;
					}
					
					item = new JMenuItem(cascade.getName());
					item.setActionCommand(cascade.getId());
					sub.add(item);
				}
				
				menu.add(sub);
			}
		}
		
		return menu;
	}
	
	private static native void nativeInit();
	private static native void nativeInitDockMenu();
	//public static native void setMenu(JMenu menu);
}