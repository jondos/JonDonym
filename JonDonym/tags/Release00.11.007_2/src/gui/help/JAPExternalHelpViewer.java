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
package gui.help;

import gui.JAPHelpContext;
import gui.JAPHelpContext.IHelpContext;
import gui.dialog.FileChooserContentPane;
import gui.dialog.JAPDialog;
import gui.dialog.SimpleWizardContentPane;
import gui.dialog.WorkerContentPane;

import java.awt.Component;
import java.awt.Container;
import java.awt.Frame;
import java.net.URL;

import javax.swing.JFileChooser;

import platform.AbstractOS;

import anon.util.JAPMessages;
import anon.util.ProgressCapsule;


import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;

public final class JAPExternalHelpViewer extends JAPHelp
{
	public static final String MSG_HELP_INSTALL = 
		JAPExternalHelpViewer.class.getName()+ "_helpInstall";
	public final static String MSG_HELP_INSTALL_PROGRESS = "helpInstallProgress";
	public static final String MSG_HELP_INSTALL_FAILED = 
		JAPExternalHelpViewer.class.getName() + "_helpInstallFailed";
	private static final String MSG_HELP_PATH_CHOICE = 
		JAPExternalHelpViewer.class.getName() + "_helpPathChoice";		
	private static final String MSG_HELP_INTERNAL = 
		JAPExternalHelpViewer.class.getName() + "_helpInstallOpenInternal";		
	private static final String MSG_HELP_INSTALL_SUCCESS = 
		JAPExternalHelpViewer.class.getName() + "_helpInstallSucceded";	

	private Object SYNC_INSTALL = new Object();
	private boolean m_bInstallationDialogShown = false;
	
	private JAPHelp m_alternativeHelp = null;
	private IHelpModel m_helpModel;
	private long m_timeLastSetVisible = 0;
	
	JAPExternalHelpViewer(Frame a_parent, IHelpModel a_helpModel)
	{
		m_helpModel = a_helpModel;
		m_alternativeHelp = new JAPInternalHelpViewer(a_parent).getHelp();
	}
	
	public void setVisible(boolean a_bVisible)
	{
		if (System.currentTimeMillis() - m_timeLastSetVisible < 1000l)
		{
			// maybe opening the browser does not work... show the internal help instead
			m_alternativeHelp.setContext(JAPHelpContext.INDEX_CONTEXT);
			m_alternativeHelp.setVisible(a_bVisible);
			return;
		}
		m_timeLastSetVisible = System.currentTimeMillis();
		
		IHelpContext context = getHelpContext();
		if(getHelpContext() == null)
		{
			LogHolder.log(LogLevel.ERR, LogType.GUI, "Cannot show help externally: No help context specified");
			m_alternativeHelp.setContext(JAPHelpContext.INDEX_CONTEXT);
			m_alternativeHelp.setVisible(a_bVisible);
			return;
		}
		Component container = context.getHelpExtractionDisplayContext();
		
		/* If no external help path is specified and no help is installed: 
		 * open dialog to ask the user
		 */
		if(!m_helpModel.isHelpPathDefined() )
		{
			if(container == null)
			{
				LogHolder.log(LogLevel.ERR, LogType.GUI, 
						"Cannot show help externally: No display context specified");
				m_alternativeHelp.setContext(getHelpContext());
				m_alternativeHelp.setVisible(a_bVisible);
				return;
			}
			
			if (m_bInstallationDialogShown)
			{
				LogHolder.log(LogLevel.WARNING, LogType.GUI, 
						"Help installation dialog is already being shown. " +
						"Cannot display help files!");
				return;
			}
			
			synchronized (SYNC_INSTALL)
			{
				m_bInstallationDialogShown = true;		
				boolean bDialogShown = false;
				if (!m_helpModel.isHelpPathDefined() && 
					(!m_helpModel.isHelpPathChangeable() || 
						!(bDialogShown = showInstallDialog(container))))
				{			
					m_bInstallationDialogShown = false;
					
					LogHolder.log(LogLevel.ERR, LogType.GUI, 
						"Cannot show help externally: Help installation failed " +
						"(changeable: " + m_helpModel.isHelpPathChangeable() + 
						" showDialog: " + bDialogShown + ")");
					m_alternativeHelp.setContext(getHelpContext());
					m_alternativeHelp.setVisible(a_bVisible);
					return;
				}
				
				m_bInstallationDialogShown = false;	
			}
		}		
		
		URL helpURL = m_helpModel.getHelpURL(context.getHelpContext() + ".html");
		boolean bBrowserAvailable = true;
		if(helpURL == null || !(bBrowserAvailable = AbstractOS.getInstance().openURL(helpURL)))				
		{
			if (container != null && showInstallDialog(container) &&
				(helpURL = m_helpModel.getHelpURL(context.getHelpContext() + ".html")) != null)
			{
				AbstractOS.getInstance().openURL(helpURL);
			}
			else
			{
				bBrowserAvailable = false;
			}
		}
		if (!bBrowserAvailable)
		{
			LogHolder.log(LogLevel.ERR, LogType.GUI, 
			"Error while trying to show context '" + context.getHelpContext() + 
			"' in external help!");
			m_alternativeHelp.setContext(getHelpContext());
			m_alternativeHelp.setVisible(a_bVisible);
		}
	}	
	
	private boolean showInstallDialog(Component a_container)
	{
		if (m_helpModel.getHelpPath() == null || !m_helpModel.isHelpPathChangeable())
		{
			return false;
		}
		
		final JAPDialog dialog = 
			new JAPDialog(a_container, JAPMessages.getString(MSG_HELP_INSTALL));
		final FileChooserContentPane fileChooser = 
			new FileChooserContentPane(dialog, JAPMessages.getString(MSG_HELP_PATH_CHOICE),
					m_helpModel.getHelpPath(), JFileChooser.DIRECTORIES_ONLY)
		{
			public CheckError[] checkYesOK()
			{
				CheckError[] errors = super.checkYesOK();
				
				if (errors != null && errors.length > 0)
				{
					return errors;
				}
				
				String pathValidation = m_helpModel.helpPathValidityCheck(getFile());
				
				if (!pathValidation.equals(AbstractHelpFileStorageManager.HELP_VALID) &&
					!pathValidation.equals(AbstractHelpFileStorageManager.HELP_JONDO_EXISTS))
				{
					errors = new CheckError[]{
							new CheckError(JAPMessages.getString(pathValidation), LogType.GUI)};
				}
				
				return errors;
			}
			
			public boolean isSkippedAsPreviousContentPane()
			{
				return true;
			}
		};					
			
		Runnable run = new Runnable()
		{
			public void run()
			{
//					When we set the path: the file storage manager of the JAPModel does the rest (if the path is valid) */
				m_helpModel.setHelpPath(fileChooser.getFile());
			}
		};
					
		final WorkerContentPane workerPane = 
			new WorkerContentPane(dialog, JAPMessages.getString(MSG_HELP_INSTALL_PROGRESS),
					fileChooser, run, m_helpModel.getHelpFileStorageObservable())
		{
			public boolean isSkippedAsNextContentPane()
			{
				return m_helpModel.isHelpPathDefined() && 
					fileChooser.getFile().getPath().equals(m_helpModel.getHelpPath());
			}
		};
		//workerPane.setInterruptThreadSafe(true);
					
		SimpleWizardContentPane finish = 
			new SimpleWizardContentPane(dialog, 
					JAPMessages.getString(MSG_HELP_INSTALL_SUCCESS), workerPane)
		{
			public CheckError[] checkUpdate()
			{
				if (workerPane.getProgressStatus() != ProgressCapsule.PROGRESS_FINISHED)
				{					
					dialog.setTitle(JAPMessages.getString(JAPDialog.MSG_TITLE_ERROR));
					setText("<font color='red'>" + 
							JAPMessages.getString(MSG_HELP_INSTALL_FAILED) +
							" " + JAPMessages.getString(MSG_HELP_INTERNAL) + "</font>");
				}
				return null;
			}
		};
		finish.getButtonCancel().setVisible(false);
		
		fileChooser.updateDialogOptimalSized();
		dialog.setResizable(false);
		dialog.setVisible(true);		
		
		if(workerPane.getProgressStatus() != ProgressCapsule.PROGRESS_FINISHED)
		{			
			return false;
		}
		return true;
	}

	protected JAPDialog getOwnDialog()
	{
		return null;
	}
	
	public void loadCurrentContext()
	{
		if(getHelpContext() != null)
		{
			if(getHelpContext().getHelpExtractionDisplayContext() != null)
			{
				setVisible(true);
			}
			else
			{
				LogHolder.log(LogLevel.ERR, LogType.GUI, "Cannot show help externally: No display context specified");
				m_alternativeHelp.setContext(getHelpContext());
				m_alternativeHelp.loadCurrentContext();
			}
		}
		else
		{
			LogHolder.log(LogLevel.ERR, LogType.GUI, "Cannot show help externally: No help context specified");
			m_alternativeHelp.setContext(JAPHelpContext.INDEX_CONTEXT);
			m_alternativeHelp.loadCurrentContext();
		}
	}
}
