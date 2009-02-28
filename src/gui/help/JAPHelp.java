/*
 Copyright (c) 2000-2006, The JAP-Team
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

package gui.help;

import java.net.URL;

import java.awt.Component;
import java.awt.Container;
import java.awt.Frame;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JButton;
import javax.swing.JMenuItem;

import gui.JAPMessages;
import gui.JAPHelpContext.IHelpContext;
import gui.dialog.JAPDialog;

import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;

public abstract class JAPHelp
{
	public static final String INDEX_CONTEXT = "index";

	public static final String IMG_HELP = JAPHelp.class.getName() + "_help.gif";

	// messages
	public static final String MSG_HELP_BUTTON = JAPHelp.class.getName() + ("_helpButton");
	public static final String MSG_HELP_MENU_ITEM = JAPHelp.class.getName() + ("_helpMenuItem");
	public static final String MSG_CLOSE_BUTTON = JAPHelp.class.getName() + ("_closeButton");
	public static final String MSG_HELP_WINDOW = JAPHelp.class.getName() + ("_helpWindow");	
	public static final String MSG_LANGUAGE_CODE = JAPHelp.class.getName() + ("_languageCode");
	public static final String MSG_ERROR_EXT_URL = JAPHelp.class.getName() + ("_errorExtURL");

	// images
	public static final String IMG_HOME = JAPHelp.class.getName() + "_home.gif";
	public static final String IMG_PREVIOUS = JAPHelp.class.getName() + ("_previous.gif");
	public static final String IMG_NEXT = JAPHelp.class.getName() + ("_next.gif");

	private IHelpContext m_helpContext;

	protected static JAPHelp ms_theJAPHelp = null;
	
	/**
	 * Creates and initialises a new global help object with the given frame as parent frame.
	 * @param a_parent the parent frame of the help object
	 * @param a_helpModel a help model; if set to null, only the internal help will be presented
	 */
	public static void init(Frame a_parent, IHelpModel a_helpModel)
	{
		if (ms_theJAPHelp == null)
		{
			ms_theJAPHelp = 
				JAPHelpFactory.createJAPhelp(a_parent, a_helpModel);
		}
	}

	/**
	 * Returns the current help instance.
	 * @return the current help instance
	 */
	public static JAPHelp getInstance()
	{
		return ms_theJAPHelp;
	}

	/**
	 * An instance of this interface is needed to open an E-Mail application with a given address.
	 */
	public static interface IExternalEMailCaller
	{
		/**
		 * Returns if the caller was able to open the URL in the browser
		 * @param a_email an E-Mail address
		 * @return if the caller was able to open the E-Mail address in the browser
		 */
		boolean openEMail(String a_email);
	}

	/**
	 * An instance of this interface is needed to open external URLs.
	 */
	public static interface IExternalURLCaller
	{
		/**
		 * Returns if the caller was able to open the URL in the browser
		 * @param a_url a URL
		 * @return if the caller was able to open the URL in the browser
		 */
		boolean openURL(URL a_url);
	}

	static final class HelpContextActionListener implements ActionListener
	{
		private IHelpContext m_helpContext;

		public HelpContextActionListener(IHelpContext a_helpContext)
		{
			m_helpContext = a_helpContext;
		}

		public void actionPerformed(ActionEvent a_event)
		{
			getInstance().setContext(m_helpContext);
			getInstance().loadCurrentContext();
			if(getHelpDialog() != null)
			{
				getHelpDialog().toFront();
				getHelpDialog().requestFocus();
			}
		}
	}
	
	/**
	 * Creates a button that opens the help window with the given context.
	 * @param a_helpContext a help context
	 * @return a button that opens the help window with the given context
	 */
	public final static JButton createHelpButton(IHelpContext a_helpContext)
	{
		//JButton helpButton = new JButton(GUIUtils.loadImageIcon(JAPHelp.IMG_HELP, true));
		JButton helpButton = new JButton(JAPMessages.getString(MSG_HELP_BUTTON));
		helpButton.setToolTipText(JAPMessages.getString(MSG_HELP_BUTTON));
		helpButton.addActionListener(new HelpContextActionListener(a_helpContext));
		return helpButton;
	}

	/**
	 * Creates a menu item that opens the help window with the given context.
	 * @param a_helpContext a help context
	 * @return a menu item that opens the help window with the given context
	 */
	public static JMenuItem createHelpMenuItem(IHelpContext a_helpContext)
	{
		JMenuItem helpButton = new JMenuItem(JAPMessages.getString(MSG_HELP_MENU_ITEM));
		helpButton.addActionListener(new HelpContextActionListener(a_helpContext));
		return helpButton;
	}

	public abstract void loadCurrentContext();
	
	public abstract void setVisible(boolean a_bVisible);
	
	public final void setContext(final String a_context, final Component a_parent) 
	{
		if (a_context == null)
		{
			return;
		}
		m_helpContext = new IHelpContext()
		{
			public String getHelpContext()
			{
				return a_context;
			}
			
			public Component getHelpExtractionDisplayContext()
			{
				return a_parent;
			}
		};
	}
	
	public final void setContext(IHelpContext context) 
	{
		m_helpContext = context;
	}
	
	/**
	 * Returns the context object
	 * @return JAPHelpContext
	 */
	public final IHelpContext getHelpContext()
	{
		return m_helpContext;
	}
	
	protected JAPDialog getOwnDialog()
	{
		return null;
	}
	
	public final static JAPDialog getHelpDialog()
	{
		if(ms_theJAPHelp == null)
		{
			return null;
		}
		return ms_theJAPHelp.getOwnDialog();
	}

	private static class JAPHelpFactory
	{
		private static JAPHelp createJAPhelp(Frame a_parent, IHelpModel a_helpModel)
		{
			if(a_helpModel != null)
			{
				LogHolder.log(LogLevel.DEBUG, LogType.GUI, "Creating external help viewer.");
				return new JAPExternalHelpViewer(a_parent, a_helpModel);
			}
			else
			{
				LogHolder.log(LogLevel.DEBUG, LogType.GUI, "Creating internal help viewer.");
				JAPInternalHelpViewer internalViewer = 
					new JAPInternalHelpViewer(a_parent);
				return internalViewer.getHelp();
			}
		}
	}
}
