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

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

import gui.dialog.JAPDialog;

public  class BasicWizardPage extends JPanel implements WizardPage
	{
		private String  m_strTitle;
		private JLabel m_labelTitle;
		private ImageIcon m_Icon;
		private JLabel m_labelIcon;
		protected JPanel m_panelComponents;
		private String message;

		public BasicWizardPage()
			{
				GridBagLayout gridBag = new GridBagLayout();
				setLayout(gridBag);
				GridBagConstraints c = new GridBagConstraints();
				m_labelTitle=new JLabel();
				m_labelIcon=new JLabel();
				m_panelComponents=new JPanel();
				//Add Icon
				c.fill = GridBagConstraints.NONE;
				c.anchor=GridBagConstraints.WEST;
				c.gridheight=2;
				c.gridwidth=1;
				c.gridx=0;
				c.gridy=0;
				c.insets=new Insets(10,10,10,10);
				c.weightx=0;
				c.weighty=1;
				gridBag.setConstraints(m_labelIcon,c);
				add(m_labelIcon);
				//Title
				c.gridheight=1;
				c.gridx=1;
				c.insets=new Insets(10,10,10,10);
				c.weightx=1.0;
				c.weighty=0;
				gridBag.setConstraints(m_labelTitle,c);
				add(m_labelTitle);
				//Main Part
				c.fill=GridBagConstraints.BOTH;
				c.gridy=1;
				c.weightx=1.0;
				c.weighty=1.0;
				c.insets=new Insets(0,0,10,10);
				gridBag.setConstraints(m_panelComponents,c);
				add(m_panelComponents);
			}



	public void setPageTitle(String title)
		{
			m_strTitle=title;
			m_labelTitle.setText(title);
		}

	public void deactivated(WizardHost host)
		{
			//setVisible(false);
		}


	public void setIcon(ImageIcon icon)
		{
			m_Icon=icon;
			m_labelIcon.setIcon(icon);
		}

	public void activated(WizardHost host)
		{
			//setVisible(true);
		}

	public JComponent getPageComponent(/*WizardHost host*/)
		{
			return this;
		}

	public boolean checkPage()
		{
			return false;
		}

	public void showInformationDialog(String message)
	{
		JAPDialog.showMessageDialog(this, message);
	}

	public ImageIcon getIcon()
		{
			return m_Icon;
		}

}
