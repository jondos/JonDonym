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

import jap.JAPModel;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.net.URL;
import java.util.Locale;
import java.util.Vector;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.text.Document;

import org.omg.CORBA._PolicyStub;

import anon.util.ResourceLoader;
import gui.JAPHelpContext;
import gui.JAPMessages;
import gui.JAPHelpContext.IHelpContext;

import gui.dialog.JAPDialog;
import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;
import platform.AbstractOS;


public abstract class JAPHelp
{
	public static final String INDEX_CONTEXT = "index";

	public static final String IMG_HELP = JAPHelp.class.getName() + "_help.gif";

	// messages
	public static final String MSG_HELP_BUTTON = JAPHelp.class.getName() + ("_helpButton");
	public static final String MSG_HELP_MENU_ITEM = JAPHelp.class.getName() + ("_helpMenuItem");
	public static final String MSG_CLOSE_BUTTON = JAPHelp.class.getName() + ("_closeButton");
	public static final String MSG_HELP_WINDOW = JAPHelp.class.getName() + ("_helpWindow");
	public static final String MSG_HELP_PATH = JAPHelp.class.getName() + ("_helpPath");
	public static final String MSG_LANGUAGE_CODE = JAPHelp.class.getName() + ("_languageCode");
	public static final String MSG_ERROR_EXT_URL = JAPHelp.class.getName() + ("_errorExtURL");

	// images
	public static final String IMG_HOME = JAPHelp.class.getName() + "_home.gif";
	public static final String IMG_PREVIOUS = JAPHelp.class.getName() + ("_previous.gif");
	public static final String IMG_NEXT = JAPHelp.class.getName() + ("_next.gif");

	/*public String m_helpPath = " ";
	public LanguageMapper m_language = new LanguageMapper();
	public JComboBox m_comBoxLanguage;
	public HtmlPane m_htmlpaneTheHelpPane;

	public JButton m_closeButton;
	public JButton m_backButton;
	public JButton m_forwardButton;
	public JButton m_homeButton;

	private boolean m_initializing;*/
	private IHelpContext m_helpContext;

	protected static JAPHelp ms_theJAPHelp = null;

	/*private JAPHelp(Frame parent, IExternalURLCaller a_urlCaller, IExternalEMailCaller a_emailCaller)
	{
		
	}*/

	/**
	 * Creates and initialises a new global help object with the given frame as parent frame.
	 * @param a_parent the parent frame of the help object
	 * @param a_urlCaller the caller that is used to open external URLs (may be null)
	 * @param m_emailCaller the caller that is used to open E_Mail applications with a given address
	 */
	public static void init(Frame a_parent,
							IExternalURLCaller a_urlCaller, IExternalEMailCaller a_emailCaller)
	{
		if (ms_theJAPHelp == null)
		{
			ms_theJAPHelp = JAPHelpFactory.createJAPhelp(a_parent, a_urlCaller, a_emailCaller);
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
		/*JMenuItem helpButton = new JMenuItem(JAPMessages.getString(MSG_HELP_MENU_ITEM));
		helpButton.addActionListener(new HelpContextActionListener(a_helpContext));
		return helpButton;*/
		return null;
	}

	public abstract void loadCurrentContext();
	
	public abstract void setVisible(boolean a_bVisible);
	
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

	/*private static String getHelpPath(int a_languageIndex)
	{
		String strMessage = MSG_HELP_PATH + String.valueOf(a_languageIndex);
		String strHelpPath = JAPMessages.getString(strMessage);

		if (strHelpPath.equals(strMessage) || strHelpPath.trim().length() == 0)
		{
			return JAPMessages.getString(MSG_HELP_PATH);
		}

		return strHelpPath;
	}*/
	private static class JAPHelpFactory
	{
		private static JAPHelp createJAPhelp(Frame a_parent,
											IExternalURLCaller a_urlCaller, IExternalEMailCaller a_emailCaller)
		{
			if(JAPModel.getInstance().isExternalHelpInstallationPossible())
			{
				LogHolder.log(LogLevel.WARNING, LogType.GUI, "Creating external help viewer.");
				return new JAPExternalHelpViewer(a_parent, a_urlCaller, a_emailCaller);
			}
			else
			{
				LogHolder.log(LogLevel.WARNING, LogType.GUI, "Creating internal help viewer.");
				JAPInternalHelpViewer internalViewer = 
					new JAPInternalHelpViewer(a_parent, a_urlCaller, a_emailCaller);
				return internalViewer.getHelp();
			}
		}
	}
}
