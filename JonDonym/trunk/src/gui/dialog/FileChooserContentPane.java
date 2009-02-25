/*
 Copyright (c) 2000 - 2006, The JAP-Team
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
package gui.dialog;

import gui.GUIUtils;
import gui.JAPMessages;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JTextField;

import logging.LogType;

/**
 * This is a generic content pane that implements the IWizardSuitable interface
 */
public class FileChooserContentPane extends DialogContentPane implements DialogContentPane.IWizardSuitable
{
	private static final String MSG_CHOOSE_FILE = 
		FileChooserContentPane.class.getName() + "_errorChooseFile"; 
	private static final String MSG_CHOOSE_DIR = 
		FileChooserContentPane.class.getName() + "_errorChooseDirectory"; 
	
	private JTextField pathField;
	private JFileChooser chooser;
	private int m_fileSelectionMode;
	
	public FileChooserContentPane(final JAPDialog dialog, String a_strText,  
			  String a_defaultValue, final int fileSelectionMode)
	{
		this(dialog, a_strText, new Layout(""), a_defaultValue, fileSelectionMode);
	}
	
	public FileChooserContentPane(final JAPDialog dialog, String a_strText, Layout a_layout, 
								  String a_defaultValue, final int fileSelectionMode)
	{
		super(dialog, a_strText, a_layout, 
				new DialogContentPaneOptions(IDialogOptions.OPTION_TYPE_OK_CANCEL));
		
		JButton pathChooseButton = new JButton(JAPMessages.getString("bttnChoose"));
		
		pathField = new JTextField(15);
		pathField.setEditable(false);
		
		if (a_defaultValue != null)
		{
			pathField.setText(a_defaultValue);
		}

		chooser = new JFileChooser();
		chooser.setFileSelectionMode(fileSelectionMode);
		m_fileSelectionMode = chooser.getFileSelectionMode();
		
		getContentPane().add(pathField);
		getContentPane().add(pathChooseButton);
		
		
		ActionListener chooseListener = 
			new ActionListener()
			{
				public void actionPerformed(ActionEvent aev)
				{
					File fileCurrent = getFile();
					if (fileCurrent != null && fileCurrent.isDirectory())
					{
						chooser.setCurrentDirectory(fileCurrent);
					}						
					
					if(GUIUtils.showMonitoredFileChooser(chooser, dialog.getContentPane()) == JFileChooser.APPROVE_OPTION)
					{
						File f = chooser.getSelectedFile();
						if(f != null)
						{
							pathField.setText(f.getPath());
						}
					}
				}
			};

		pathChooseButton.addActionListener(chooseListener);
	}

	public File getFile()
	{
		String strpath = pathField.getText();
		if (strpath != null)
		{
			strpath = strpath.trim();
		}
		if (strpath.length() > 0)
		{
			return new File(strpath);
		}
		return null;
	}
	
	public CheckError[] checkYesOK()
	{
		String strMessage;
		
		if (m_fileSelectionMode == JFileChooser.DIRECTORIES_ONLY)
		{
			strMessage = JAPMessages.getString(MSG_CHOOSE_DIR);
		}
		else
		{
			strMessage = JAPMessages.getString(MSG_CHOOSE_FILE);
		}
		
		File file = getFile();
		if (file == null  || 
			(m_fileSelectionMode == JFileChooser.DIRECTORIES_ONLY && !file.isDirectory()) ||
			(m_fileSelectionMode == JFileChooser.FILES_ONLY && file.isDirectory()))
		{
			return new CheckError[]{new CheckError(strMessage, LogType.GUI)};			
		}
		return null;
	}
	

}
