package gui.help;

import gui.JAPMessages;
import gui.JAPHelpContext.IHelpContext;
import gui.dialog.JAPDialog;

import jap.HelpFileStorageManager;
import jap.JAPConf;
import jap.JAPModel;
import jap.JAPNewView;

import java.awt.Component;
import java.awt.Frame;
import java.io.File;
import java.net.URL;
import java.util.Observable;

import javax.swing.JFileChooser;
import javax.swing.RootPaneContainer;


import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;
import platform.AbstractOS;

public final class JAPExternalHelpViewer extends JAPHelp
{
	private Component m_parent = null;
	private IExternalURLCaller m_urlCaller = null;
	private IExternalEMailCaller m_emailCaller = null;
	
	private JAPHelp m_alternativeHelp = null;
	
	JAPExternalHelpViewer(Frame a_parent, IExternalURLCaller a_urlCaller, IExternalEMailCaller a_emailCaller)
	{
		m_parent = a_parent;
		m_urlCaller = a_urlCaller;
		m_emailCaller = a_emailCaller;
		//m_alternativeHelp = 
		//	new JAPInternalHelpViewer(a_parent, a_urlCaller, a_emailCaller).getHelp();
	}
	
	public void setVisible(boolean a_bVisible)
	{
		final JAPModel model = JAPModel.getInstance();
		IHelpContext context = getHelpContext();
		if(getHelpContext() == null)
		{
			LogHolder.log(LogLevel.ERR, LogType.GUI, "Cannot show help externally: No help context specified");
			return;
		}
		RootPaneContainer container = context.getDisplayContext();
		
		if(container == null)
		{
			LogHolder.log(LogLevel.ERR, LogType.GUI, "Cannot show help externally: No display context specified");
			return;
		}
		/* If no external help path is specified and no help is installed: 
		 * open dialog to ask the user
		 */
		if(!model.isHelpPathDefined() )
		{
			final File f = 
				JAPDialog.showFileChooseDialog(container, 
							JAPMessages.getString(JAPNewView.MSG_HELP_INSTALL),
							JAPMessages.getString(JAPNewView.MSG_HELP_PATH_CHOICE),
							model.getHelpPath(),
							JFileChooser.DIRECTORIES_ONLY);
			
			boolean pathChosen = (f != null);
			String pathValidation = pathChosen ? 
					JAPModel.getInstance().helpPathValidityCheck(f) : HelpFileStorageManager.HELP_INVALID_NULL;
				
			boolean pathValid = pathValidation.equals(HelpFileStorageManager.HELP_VALID) ||
							 	pathValidation.equals(HelpFileStorageManager.HELP_JONDO_EXISTS);
			
			if(!pathChosen)
			{
				return; //false;
			}
			
			if(pathChosen && pathValid)
			{
				if(!model.isHelpPathDefined() || !f.getPath().equals(model.getHelpPath()))
				{
					Observable helpFileStorageObservable = model.getHelpFileStorageObservable();
					/* observe the model while it changes during installation. (if we can). */
					if(helpFileStorageObservable != null)
					{		
						JAPDialog hd =
							JAPDialog.showProgressDialog(container, JAPMessages.getString(JAPNewView.MSG_HELP_INSTALL), 
									JAPMessages.getString(JAPNewView.MSG_HELP_INSTALL_PROGRESS), null, 
									null, helpFileStorageObservable);
					}
					//When we set the path: the file storage manager of the JAPModel does the rest (if the path is valid) */
					model.setHelpPath(f);
					//Not really happy about that:
					JAPConf.getInstance().updateValues();
				}
			}
			else
			{
				JAPDialog.showErrorDialog(container, JAPMessages.getString(JAPNewView.MSG_HELP_INSTALL_FAILED)+
						JAPMessages.getString(pathValidation), LogType.MISC);
				return; //false;
			}
		}
		URL helpURL = model.getHelpURL(context.getHelpContext()+".html");
		if(helpURL != null)
		{
			AbstractOS.getInstance().openURL(helpURL);	
		}
	}	

	protected JAPDialog getOwnDialog()
	{
		return null;
	}
	
	public void loadCurrentContext()
	{
		if(getHelpContext() != null)
		{
			if(getHelpContext().getDisplayContext() != null)
			{
				setVisible(true);
			}
		}
	}
}
