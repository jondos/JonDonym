/*
Copyright (c) 2000, The JAP-Team
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
package gui.wizard;

import gui.JAPMessages;
import jap.JAPUtil;

import java.awt.CardLayout;
import java.awt.Container;
import java.awt.Dialog;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import gui.GUIUtils;
import gui.dialog.JAPDialog;
import javax.swing.RootPaneContainer;

// this shall become the browser/wizardhost providing class ...
public class BasicWizardHost implements WizardHost,ActionListener
	{

		private JAPDialog m_Dialog;
		private JButton m_bttnOk;
		private JButton m_bttnCancel;
		private JButton m_bttnFinish;
		private JButton m_bttnBack;
		private JButton m_bttnNext;
		private JButton m_bttnHelp;
		private JPanel	m_panelPages;
		private CardLayout m_cardlayoutPages;
		private Wizard m_Wizard;

		private final static String COMMAND_NEXT="NEXT";
		private final static String COMMAND_BACK="BACK";
		private final static String COMMAND_CANCEL="CANCEL";
		private final static String COMMAND_FINISH="FINISH";
		private final static String COMMAND_HELP="HELP";

		public BasicWizardHost(JAPDialog a_dialog, Wizard a_wizard)
		{
			this((Object)a_dialog, a_wizard);
		}

		public BasicWizardHost(Container a_dialog, Wizard a_wizard)
		{
			this((Object)a_dialog, a_wizard);
		}


		private BasicWizardHost(Object parent,Wizard wizard)
		{

				m_Wizard=wizard;
				//m_currentPage=null;

				if (parent instanceof JAPDialog)
				{
					m_Dialog = new JAPDialog((JAPDialog)parent, wizard.getWizardTitle(), true);
				}
				else
				{
					m_Dialog = new JAPDialog((Container)parent, wizard.getWizardTitle(), true);
				}
				GridBagLayout gridBag= new GridBagLayout();
				GridBagConstraints c = new GridBagConstraints();

				m_Dialog.getContentPane().setLayout(gridBag);

				GridBagLayout gridBagPanel=new GridBagLayout();
				GridBagConstraints cPanel = new GridBagConstraints();
				JPanel panel = new JPanel();
				panel.setLayout(gridBagPanel);

				m_bttnBack=new JButton(JAPMessages.getString("updateM_bttnBack"));
				m_bttnBack.setActionCommand(COMMAND_BACK);
				m_bttnBack.addActionListener(this);
				m_bttnNext=new JButton(JAPMessages.getString("updateM_bttnNext"));
				m_bttnNext.setActionCommand(COMMAND_NEXT);
				m_bttnNext.addActionListener(this);
				m_bttnHelp=new JButton(JAPMessages.getString("updateM_bttnHelp"));
				m_bttnCancel=new JButton(JAPMessages.getString("updateM_bttnCancel"));
				m_bttnCancel.setActionCommand(COMMAND_CANCEL);
				m_bttnCancel.addActionListener(this);
				m_bttnFinish=new JButton(JAPMessages.getString("updateM_bttnFinish"));
				m_bttnFinish.setActionCommand(COMMAND_FINISH);
				m_bttnFinish.addActionListener(this);

				JSeparator separator = new JSeparator(); //the Line...
				separator.setVisible(true);

				m_cardlayoutPages=new CardLayout();
				m_panelPages=new JPanel(m_cardlayoutPages);

				cPanel.gridx = 0;
				cPanel.gridy = 0;
				cPanel.fill=GridBagConstraints.NONE;
				cPanel.anchor = GridBagConstraints.WEST;
				cPanel.weightx = 0;
				cPanel.weighty = 1.0;
				cPanel.insets = new Insets(10,10,10,10);
				panel.add(m_bttnHelp,cPanel);

				JLabel l=new JLabel("");
				cPanel.weightx=1.0;
				cPanel.gridx=1;
				cPanel.fill=GridBagConstraints.HORIZONTAL;
				panel.add(l,cPanel);

				cPanel.weightx=0;
				cPanel.fill=GridBagConstraints.NONE;
				cPanel.gridx=2;
				cPanel.insets = new Insets(10,10,10,20);
				panel.add(m_bttnCancel,cPanel);

				cPanel.gridx=3;
				cPanel.insets = new Insets(10,2,10,2);
				panel.add(m_bttnBack,cPanel);
				cPanel.gridx=4;
				panel.add(m_bttnNext,cPanel);
				cPanel.gridx=5;
				cPanel.insets = new Insets(10,20,10,10);
				panel.add(m_bttnFinish,cPanel);

				c.gridx = 0;
				c.gridy = 0;
				c.gridwidth = 1;
				c.gridheight=1;
				c.fill=GridBagConstraints.BOTH;
				c.anchor = GridBagConstraints.NORTHWEST;
				c.insets = new Insets(10,10,10,10);
				c.weightx = 1.0;
				c.weighty = 1.0;
				gridBag.setConstraints(m_panelPages, c);
				m_Dialog.getContentPane().add(m_panelPages);

				c.gridy = 1;
				c.fill=GridBagConstraints.HORIZONTAL;
				c.weighty=0;
				c.insets = new Insets(0,10,0,10);
				gridBag.setConstraints(separator,c);
				m_Dialog.getContentPane().add(separator);

				c.gridy = 2;
				c.insets = new Insets(0,0,0,0);
				c.fill = GridBagConstraints.HORIZONTAL;
				gridBag.setConstraints(panel,c);
				m_Dialog.getContentPane().add(panel);
		}

	public void addWizardPage(int index,WizardPage wizardPage)
		{
			m_panelPages.add(wizardPage.getPageComponent(),Integer.toString(index));
		}

	public void showWizardPage(int index)
		{
			if(index==0)
				{
					m_cardlayoutPages.first(m_panelPages);
					m_Dialog.pack();
					//GUIUtils.centerOnScreen(m_Dialog);
					m_Dialog.setVisible(true);
				}
			else
				{
					m_cardlayoutPages.show(m_panelPages,Integer.toString(index));
					m_Dialog.pack();
				}
		}

	public JAPDialog getDialogParent()
		{
			return m_Dialog;
		}
 public void setHelpEnabled(boolean enabled)
	{
		m_bttnHelp.setEnabled(enabled);
	}

 public void setNextEnabled(boolean enabled)
	{
		m_bttnNext.setEnabled(enabled);
	}

	public void setBackEnabled(boolean b)
	 {
			m_bttnBack.setEnabled(b);
	 }

	public void setCancelEnabled(boolean b)
		{
			m_bttnCancel.setEnabled(b);
		}

	public void setFinishEnabled(boolean b)
		{
			m_bttnFinish.setEnabled(b);
		}



	public void actionPerformed(ActionEvent e)
		{
			String command=e.getActionCommand();
			if(command.equals(COMMAND_NEXT))
				{
					 m_Wizard.next(/*m_currentPage,this*/);
				}
			else if(command.equals(COMMAND_BACK))
				{
					m_Wizard.back(/*m_currentPage,this*/);
				}
			else if(command.equals(COMMAND_CANCEL))
				{
					m_Wizard.wizardCompleted();
					m_Dialog.dispose();
				}
				else if(command.equals(COMMAND_FINISH))
				{
					m_Wizard.finish(/*m_currentPage,this*/);

				}
				else if(command.equals(COMMAND_HELP))
				{
				//todo show help-dialog
				}
		}
}
