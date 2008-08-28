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
import jap.SystrayPopupMenu;
import jap.TrustModel;
import anon.infoservice.Database;
import anon.infoservice.MixCascade;

import java.util.Vector;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
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
	
	public static void dockMenuCallback()
	{
		/*EventQueue.invokeLater(new Runnable() {
				public void run() {
					osl.openSheetFinished(new SheetEvent(filenames));
				}
			});*/
		System.out.println("dock menu item clicked");
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
		
		item = new JMenuItem(JAPMessages.getString(MSG_ANONYMITY_MODE));
		menu.add(item);
		
		item = new JMenuItem(JAPMessages.getString(MSG_SHOW_DETAILS));
		menu.add(item);
		
		menu.add(new JSeparator());
		
		item = new JMenuItem(JAPMessages.getString(MSG_SETTINGS));
		menu.add(item);
		
		item = new JMenuItem(JAPMessages.getString(JAPHelp.MSG_HELP_MENU_ITEM));
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