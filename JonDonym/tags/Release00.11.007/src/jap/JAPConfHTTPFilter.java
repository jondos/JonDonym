/*
 Copyright (c) 2000 - 2004, The JAP-Team
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
package jap;

import java.util.Observable;
import java.util.Observer;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JRadioButton;
import javax.swing.JPanel;
import javax.swing.border.TitledBorder;

import anon.util.JAPMessages;


/**
 * This is the configuration GUI for the cert.
 */

final class JAPConfHTTPFilter extends AbstractJAPConfModule implements Observer, ActionListener
{
	private TitledBorder m_borderCert;

	private JRadioButton m_btnFilterOn;
	private JRadioButton m_btnFilterOff;
	
	private JCheckBox m_boxUserAgent;
	private JCheckBox m_boxLanguage;
	private JCheckBox m_boxEncoding;
	private JCheckBox m_boxFileTypes;

	public JAPConfHTTPFilter()
	{
		super(null);
	}

	/**
	 * Creates the cert root panel with all child-panels.
	 */
	public void recreateRootPanel()
	{
		JPanel panelRoot = getRootPanel();

		/* clear the whole root panel */
		panelRoot.removeAll();

		m_borderCert = new TitledBorder(JAPMessages.getString("confHTTPFilterTab"));
		panelRoot.setBorder(m_borderCert);
		panelRoot.setLayout(new GridBagLayout());
		
		GridBagConstraints c = new GridBagConstraints();
		c.insets = new Insets(0, 0, 0, 0);
		c.gridx = 0;
		c.gridy = 0;
		c.weightx = 1;
		c.weighty = 0;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.anchor = GridBagConstraints.NORTHWEST;
		
		m_btnFilterOn = new JRadioButton("jap.JAPConfHTTPFilter_filterOn");
		m_btnFilterOn.setSelected(true);
		m_btnFilterOn.addActionListener(this);
		panelRoot.add(m_btnFilterOn, c);
		
		c.insets = new Insets(0, 20, 0, 0);
		c.gridy++;
		m_boxUserAgent = new JCheckBox("jap.JAPConfHTTPFilter_userAgent");
		m_boxUserAgent.setSelected(true);
		panelRoot.add(m_boxUserAgent, c);
		
		c.gridy++;
		m_boxLanguage = new JCheckBox("jap.JAPConfHTTPFilter_language");
		m_boxLanguage.setSelected(true);
		panelRoot.add(m_boxLanguage, c);
		
		c.gridy++;
		m_boxEncoding = new JCheckBox("jap.JAPConfHTTPFilter_encoding");
		m_boxEncoding.setSelected(true);
		panelRoot.add(m_boxEncoding, c);

		c.gridy++;
		m_boxFileTypes = new JCheckBox("jap.JAPConfHTTPFilter_fileTypes");
		m_boxFileTypes.setSelected(true);
		panelRoot.add(m_boxFileTypes, c);
		
		c.insets = new Insets(0, 0, 0, 0);
		c.gridy++;
		c.weighty = 1;
		m_btnFilterOff = new JRadioButton("jap.JAPConfHTTPFilter_filterOff");
		m_btnFilterOff.addActionListener(this);
		panelRoot.add(m_btnFilterOff, c);
		
		ButtonGroup group = new ButtonGroup();
		group.add(m_btnFilterOn);
		group.add(m_btnFilterOff);
	}

	/**
	 * Returns the title for the cert configuration tab.
	 *
	 * @return The title for the cert configuration tab.
	 */
	public String getTabTitle()
	{
		return JAPMessages.getString("confHTTPFilterTab");
	}
	
	public void actionPerformed(ActionEvent a_event)
	{
		if(a_event.getSource() == m_btnFilterOff)
		{
			m_boxUserAgent.setEnabled(false);
			m_boxLanguage.setEnabled(false);
			m_boxEncoding.setEnabled(false);
			m_boxFileTypes.setEnabled(false);
		} 
		else if(a_event.getSource() == m_btnFilterOn)
		{
			m_boxUserAgent.setEnabled(true);
			m_boxLanguage.setEnabled(true);
			m_boxEncoding.setEnabled(true);
			m_boxFileTypes.setEnabled(true);
		}
		
	}

	public void update(Observable a_notifier, Object a_message)
	{
		
	}

	protected void onUpdateValues()
	{

	}

	protected boolean onOkPressed()
	{
		return true;
	}

	protected void onResetToDefaultsPressed()
	{
	
	}

	public String getHelpContext()
	{
		return "httpFilter";
	}
}
