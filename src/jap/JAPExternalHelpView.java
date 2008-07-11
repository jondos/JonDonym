package jap;

import gui.JAPMessages;
import gui.dialog.JAPDialog;

import java.awt.Component;
import java.io.File;
import java.net.URL;
import java.util.Observable;

import javax.swing.JFileChooser;


import logging.LogType;
import platform.AbstractOS;

public class JAPExternalHelpView 
{
	Component m_parent = null;
	
	/**
	 * TODO: Perhaps do it better with a chained list of DialogPanes?
	 * @param parent
	 */
	public JAPExternalHelpView(Component parent)
	{
		m_parent = parent;
	}
	
	public boolean displayHelp()
	{
		final JAPModel model = JAPModel.getInstance();
	
		/* If no external help path is specified and no help is installed: 
		 * open dialog to ask the user
		 */
		if(!model.isHelpPathDefined() )
		{
			final File f = 
				JAPDialog.showFileChooseDialog(m_parent, 
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
				return false;
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
							JAPDialog.showProgressDialog(m_parent, JAPMessages.getString(JAPNewView.MSG_HELP_INSTALL), 
									JAPMessages.getString(JAPNewView.MSG_HELP_INSTALL_PROGRESS), null, 
									null, helpFileStorageObservable);
					}
					//When we set the path: the file storage manager of the JAPModel does the rest (if the path is valid) */
					model.setHelpPath(f);
				}
			}
			else
			{
				JAPDialog.showErrorDialog(m_parent, JAPMessages.getString(JAPNewView.MSG_HELP_INSTALL_FAILED)+
						JAPMessages.getString(pathValidation), LogType.MISC);
				return false;
			}
		}
		URL helpURL = model.getHelpURL();
		if(helpURL != null)
		{
			return AbstractOS.getInstance().openURL(helpURL);	
		}
		return false;
	}	
}
