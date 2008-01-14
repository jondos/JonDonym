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
package jap;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import javax.swing.JCheckBox;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeListener;
import javax.swing.event.ChangeEvent;
import javax.swing.JRadioButton;
import javax.swing.ButtonGroup;
import javax.swing.JPanel;
import javax.swing.JComboBox;
import javax.swing.JButton;
import javax.swing.JSlider;
import javax.swing.SwingConstants;

import gui.JAPMessages;
import gui.GUIUtils;
import gui.JAPMultilineLabel;
import javax.swing.JList;
import javax.swing.DefaultListModel;
import javax.swing.JTextArea;
import javax.swing.ListSelectionModel;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import java.util.Dictionary;
import java.util.Observable;
import java.util.Hashtable;

final class JAPConfTrust extends AbstractJAPConfModule
{
	public static final String MSG_TITLE = JAPConfTrust.class.getName() + "_title";
	public static final String MSG_TITLE_LONG = JAPConfTrust.class.getName() + "_titleLong";
	public static final String MSG_PAYMENT = JAPConfTrust.class.getName() + "_payment";
	public static final String MSG_EXPIRED_CERTS = JAPConfTrust.class.getName() + "_expiredCerts";

	public static final String MSG_TRUST_NONE = JAPConfTrust.class.getName() + "_trustNone";
	public static final String MSG_TRUST_LITTLE = JAPConfTrust.class.getName() + "_trustLittle";
	public static final String MSG_TRUST_HIGH = JAPConfTrust.class.getName() + "_trustHigh";
	public static final String MSG_TRUST_EXCLUSIVE = JAPConfTrust.class.getName() + "_trustExclusive";

	private static final int TRUST_NONE = 0;
	private static final int TRUST_LITTLE = 1;
	private static final int TRUST_NORMAL = 2;
	private static final int TRUST_HIGH = 3;
	private static final int TRUST_EXCLUSIVE = 4;

	private JComboBox m_comboTrustPay;
	private JComboBox m_comboTrustExpiredCerts;


	protected JAPConfTrust()
	{
		super(null);
	}

	public String getTabTitle()
	{
		return JAPMessages.getString(MSG_TITLE);
	}

	protected void onUpdateValues()
	{
		m_comboTrustPay.setSelectedIndex(
			  TrustModel.getCurrentTrustModel().getTrustPay());
		m_comboTrustExpiredCerts.setSelectedIndex(
			  TrustModel.getCurrentTrustModel().getTrustExpiredCerts());
	}

	protected boolean onOkPressed()
	{
		TrustModel.getCurrentTrustModel().setTrustPay(m_comboTrustPay.getSelectedIndex());
		TrustModel.getCurrentTrustModel().setTrustExpiredCerts(
			  m_comboTrustExpiredCerts.getSelectedIndex());

		return true;
	}

	public void recreateRootPanel()
	{
		GridBagConstraints constraints;
		JPanel panelRoot = getRootPanel();


		panelRoot.removeAll();
		panelRoot.setBorder(new TitledBorder(JAPMessages.getString(MSG_TITLE_LONG)));
		panelRoot.setLayout(new GridBagLayout());
		constraints = new GridBagConstraints();
		constraints.gridx = 0;
		constraints.gridy = 0;
		constraints.insets = new Insets(10, 10, 0, 10);
		constraints.anchor = GridBagConstraints.NORTHWEST;
		constraints.fill = GridBagConstraints.BOTH;
		panelRoot.add(new JLabel(JAPMessages.getString(MSG_PAYMENT) + ":"), constraints);
		m_comboTrustPay = new JComboBox(
			  new Object[]{JAPMessages.getString(MSG_TRUST_NONE), JAPMessages.getString(MSG_TRUST_LITTLE),
			  JAPMessages.getString(MSG_TRUST_HIGH), JAPMessages.getString(MSG_TRUST_EXCLUSIVE)});
		constraints.gridx = 1;
		panelRoot.add(m_comboTrustPay, constraints);
		/*
		m_comboTrustPay.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent a_event)
			{
				System.out.println(m_comboTrustPay.isVisible());
				JAPModel.getInstance().getTrustModel().showTrustWarning(m_comboTrustPay);
			}
		});*/


		constraints.gridy++;
		constraints.gridx = 0;
		panelRoot.add(new JLabel(JAPMessages.getString(MSG_EXPIRED_CERTS) + ":"), constraints);
		m_comboTrustExpiredCerts = new JComboBox(
			  new Object[]{JAPMessages.getString(MSG_TRUST_NONE), JAPMessages.getString(MSG_TRUST_LITTLE),
			  JAPMessages.getString(MSG_TRUST_HIGH)});
		constraints.gridx = 1;
		panelRoot.add(m_comboTrustExpiredCerts, constraints);
		/*
		m_comboTrustExpiredCerts.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent a_event)
			{
				JAPModel.getInstance().getTrustModel().showTrustWarning(m_comboTrustExpiredCerts);
			}
		});*/



		constraints.gridy++;
		constraints.gridx = 2;
		constraints.weightx = 1;
		constraints.weighty = 1;
		panelRoot.add(new JLabel(), constraints);
	}

	public void onResetToDefaultsPressed()
	{
		m_comboTrustPay.setSelectedIndex(
			  TrustModel.getCurrentTrustModel().TRUST_DEFAULT);
		m_comboTrustExpiredCerts.setSelectedIndex(
			  TrustModel.getCurrentTrustModel().TRUST_DEFAULT);
	}

	public String getHelpContext()
	{
		return getClass().getName();
	}

	protected void onRootPanelShown()
	{

	}
}
