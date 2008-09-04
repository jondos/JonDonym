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

import java.util.Vector;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;

import anon.mixminion.PasswordManager;
import anon.mixminion.mmrdescription.MMRDescription;
import anon.mixminion.mmrdescription.MMRList;
import anon.mixminion.mmrdescription.InfoServiceMMRListFetcher;
import anon.mixminion.mmrdescription.PlainMMRListFetcher;

import gui.JAPMessages;
import gui.dialog.JAPDialog;
import logging.LogType;
import gui.GUIUtils;
import javax.swing.JCheckBox;
import javax.swing.event.ChangeListener;
import javax.swing.event.ChangeEvent;
import java.awt.Color;
import java.util.Dictionary;

final class JAPConfMixminion extends AbstractJAPConfModule implements ActionListener
{
	private JCheckBox m_cbxActive;
	private JTable m_tableRouters;
	private JSlider m_sliderPathLen;
	private JButton m_bttnFetchRouters, m_bttnChangePW, m_bttnResetKeyring;
	private JLabel m_labelAvailableRouters, m_lblPathLen, m_lblEMail, m_lblKeyring;
	private JScrollPane m_scrollPane;
	private JPanel m_panelEMail, m_panelPreferences;
	private TitledBorder m_borderPreferences;
	private TitledBorder m_borderEMail;
	private JTextField m_email;

	long m_lastUpdate;

	private class MyJTable extends JTable
	{
		/**
		 * serial version UID
		 */
		private static final long serialVersionUID = 1L;
		
		public MyJTable(DefaultTableModel m)
		{
			super(m);
		}
		public boolean isCellEditable(int i, int j)
		{
			return false;
		}
	};


	public JAPConfMixminion()
	{
		super(null);
	}

	public void recreateRootPanel()
	{
		JPanel panelRoot = getRootPanel();

		// clear the whole root panel
		panelRoot.removeAll();
		GridBagLayout l = new GridBagLayout();
		GridBagConstraints c = new GridBagConstraints();
		c.insets = new Insets(5, 5, 5, 5);
		c.anchor = GridBagConstraints.NORTHWEST;
		panelRoot.setLayout(l);
		c.gridwidth = 5;
		c.fill = GridBagConstraints.BOTH;

		c.gridx = 0;
		c.gridy = 1;
		m_cbxActive = new JCheckBox(JAPMessages.getString(JAPConfTor.MSG_ACTIVATE), true);
		m_cbxActive.addChangeListener(new ChangeListener()
		{
			public void stateChanged(ChangeEvent a_event)
			{
				m_labelAvailableRouters.setEnabled(m_cbxActive.isSelected());
				m_tableRouters.setEnabled(m_cbxActive.isSelected());
				m_bttnFetchRouters.setEnabled(m_cbxActive.isSelected());
				m_bttnChangePW.setEnabled(m_cbxActive.isSelected());
				m_bttnResetKeyring.setEnabled(m_cbxActive.isSelected());
				m_lblKeyring.setEnabled(m_cbxActive.isSelected());
				m_email.setEnabled(m_cbxActive.isSelected());
				m_sliderPathLen.setEnabled(m_cbxActive.isSelected());
				m_lblPathLen.setEnabled(m_cbxActive.isSelected());
				m_lblEMail.setEnabled(m_cbxActive.isSelected());
				m_scrollPane.setEnabled(m_cbxActive.isSelected());
				m_borderEMail = new TitledBorder(m_borderEMail.getTitle());
				m_borderPreferences = new TitledBorder(m_borderPreferences.getTitle());
				if (m_cbxActive.isSelected())
				{
					m_bttnFetchRouters.setDisabledIcon(GUIUtils.loadImageIcon(JAPConstants.
						IMAGE_RELOAD_DISABLED, true, false));
				}
				else
				{
					m_borderEMail.setTitleColor(Color.gray);
					m_borderPreferences.setTitleColor(Color.gray);
					m_bttnFetchRouters.setDisabledIcon(
						GUIUtils.loadImageIcon(JAPConstants.IMAGE_RELOAD_ROLLOVER, true, false));
				}
				m_panelEMail.setBorder(m_borderEMail);
				m_panelPreferences.setBorder(m_borderPreferences);

				Dictionary d = m_sliderPathLen.getLabelTable();
				for (int i = JAPConstants.MIXMINION_MIN_ROUTE_LEN; i <= JAPConstants.MIXMINION_MAX_ROUTE_LEN; i++)
				{
					( (JLabel) d.get(new Integer(i))).setEnabled(m_sliderPathLen.isEnabled());
				}
			}
		});
		panelRoot.add(m_cbxActive, c);

		c.weightx = 1;
		c.weighty = 1;
		c.gridx = 0;
		c.gridy = 2;
		GridBagLayout g2 = new GridBagLayout();
		GridBagConstraints c2 = new GridBagConstraints();
		JPanel p = new JPanel(g2);
		m_labelAvailableRouters = new JLabel(JAPMessages.getString("mixminionBorderAvailableRouters") + ":");
		c2.fill = GridBagConstraints.HORIZONTAL;
		c2.weightx = 1;
		c2.weighty = 0;
		p.add(m_labelAvailableRouters, c2);

		DefaultTableModel model = new DefaultTableModel();
		model.addColumn(JAPMessages.getString("mixminionRouterName"));
		model.addColumn(JAPMessages.getString("mixminionRouterAdr"));
		model.addColumn(JAPMessages.getString("mixminionRouterPort"));
		model.addColumn(JAPMessages.getString("mixminionRouterSoftware"));
		model.setNumRows(10);
		m_tableRouters = new MyJTable(model);
		m_tableRouters.setPreferredScrollableViewportSize(new Dimension(70, m_tableRouters.getRowHeight() * 5));
		m_tableRouters.setCellSelectionEnabled(false);
		m_tableRouters.setColumnSelectionAllowed(false);
		m_tableRouters.setRowSelectionAllowed(true);
		m_tableRouters.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		m_scrollPane = new JScrollPane(m_tableRouters);
		m_scrollPane.setAutoscrolls(true);
		c2.fill = GridBagConstraints.BOTH;
		c2.gridy = 1;
		c2.weightx = 1;
		c2.weighty = 1;
		c2.gridwidth = 2;
		p.add(m_scrollPane, c2);
		m_bttnFetchRouters = new JButton(JAPMessages.getString("mixminionBttnFetchRouters"));
		m_bttnFetchRouters.setIcon(GUIUtils.loadImageIcon(JAPConstants.IMAGE_RELOAD, true, false));
		m_bttnFetchRouters.setDisabledIcon(GUIUtils.loadImageIcon(JAPConstants.IMAGE_RELOAD_DISABLED, true, false));
		m_bttnFetchRouters.setPressedIcon(GUIUtils.loadImageIcon(JAPConstants.IMAGE_RELOAD_ROLLOVER, true, false));

		m_bttnFetchRouters.setActionCommand("fetchRouters");
		m_bttnFetchRouters.addActionListener(this);
		c2.fill = GridBagConstraints.NONE;
		c2.weighty = 0;
		c2.gridy = 0;
		c2.gridwidth = 1;
		c2.gridx = 1;
		c2.anchor = GridBagConstraints.EAST;
		c2.insets = new Insets(5, 5, 5, 0);

		p.add(m_bttnFetchRouters, c2);
		panelRoot.add(p, c);

		p = new JPanel(new GridBagLayout());
		GridBagConstraints c3 = new GridBagConstraints();
		c3.anchor = GridBagConstraints.NORTHWEST;
		c3.insets = new Insets(2, 5, 2, 5);
		c3.fill = GridBagConstraints.NONE;
		m_lblPathLen = new JLabel(JAPMessages.getString("mixminionPrefPathLen"));
		p.add(m_lblPathLen, c3);
		m_sliderPathLen = new JSlider();
		m_sliderPathLen.setPaintLabels(true);
		m_sliderPathLen.setPaintTicks(true);
		m_sliderPathLen.setMajorTickSpacing(1);
		m_sliderPathLen.setSnapToTicks(true);
		m_sliderPathLen.setMinimum(JAPConstants.MIXMINION_MIN_ROUTE_LEN);
		m_sliderPathLen.setMaximum(JAPConstants.MIXMINION_MAX_ROUTE_LEN);
		c3.gridx = 1;
		c3.fill = GridBagConstraints.HORIZONTAL;
		p.add(m_sliderPathLen, c3);

		m_borderPreferences = new TitledBorder(JAPMessages.getString("mixminionBorderPreferences"));
		p.setBorder(m_borderPreferences);
		m_panelPreferences = p;
		c.gridy = 3;
		c.weighty = 0;
		c.fill = GridBagConstraints.HORIZONTAL;
		panelRoot.add(p, c);

		p= new JPanel(new GridBagLayout());

		m_lblEMail = new JLabel(JAPMessages.getString("mixminionEMail"));
		c3.gridx = 0;
		c3.gridy = 0;
		p.add(m_lblEMail, c3);
		m_email = new JTextField();
		c3.gridx = 1;
		//c3.fill = GridBagConstraints.HORIZONTAL
		c3.gridwidth = GridBagConstraints.REMAINDER;
		p.add(m_email, c3);
		m_lblKeyring = new JLabel(JAPMessages.getString("mixminionKeyring") + ":");
		//c3.fill = GridBagConstraints.HORIZONTAL;
		c3.weightx = 1;
		c3.gridwidth = GridBagConstraints.RELATIVE;
		c3.gridx = 0;
		c3.gridy = 1;
		p.add(m_lblKeyring, c3);
		m_bttnChangePW = new JButton(JAPMessages.getString("mixminionBttnChangePassword"));
		m_bttnChangePW.setActionCommand("changePW");
		m_bttnChangePW.addActionListener(this);
		c3.gridx = 1;
		p.add(m_bttnChangePW, c3);
		m_bttnResetKeyring = new JButton(JAPMessages.getString("mixminionBttnResetKeyring"));
		m_bttnResetKeyring.setActionCommand("resetKeyring");
		m_bttnResetKeyring.addActionListener(this);
		c3.gridx = 2;
		p.add(m_bttnResetKeyring,c3);

		m_borderEMail = new TitledBorder(JAPMessages.getString("mixminionEMailSettings"));
		p.setBorder(m_borderEMail);
		m_panelEMail = p;
		c.gridy = 4;
		c.weighty = 0;
		c.fill = GridBagConstraints.HORIZONTAL;
		panelRoot.add(p, c);
		//end

		m_lastUpdate = 0;
	}

	public String getTabTitle()
	{
		return "Mixminion";
	}

	/**
	 * actionPerformed
	 *
	 * @param actionEvent ActionEvent
	 */
	public void actionPerformed(ActionEvent actionEvent)
	{
		if (actionEvent.getActionCommand().equals("enableMixminion"))
		{
			updateValues(false);
		}
		else if (actionEvent.getActionCommand().equals("fetchRouters"))
		{
			fetchRoutersAsync(true);

		}
		else if (actionEvent.getActionCommand().equals("changePW"))
		{
			PasswordManager pm = new PasswordManager();
			pm.changePassword();
			m_bttnResetKeyring.setEnabled(true);
		}
		else if (actionEvent.getActionCommand().equals("resetKeyring"))
		{
			JAPController.resetMixminionPassword();
			m_bttnResetKeyring.setEnabled(false);
		}
	}

	protected boolean onOkPressed()
	{
		JAPModel.getInstance().setMixminionRouteLen(m_sliderPathLen.getValue());
		JAPModel.getInstance().setMixminionMyEMail(m_email.getText());
		JAPModel.getInstance().setMixMinionActivated(m_cbxActive.isSelected());
		return true;
	}

	protected void onUpdateValues()
	{
		m_sliderPathLen.setValue(JAPModel.getMixminionRouteLen());
		m_email.setText(JAPModel.getMixminionMyEMail());
		m_cbxActive.setSelected(JAPModel.getInstance().isMixMinionActivated());
	}

	public String getHelpContext()
	{
		return "services_mixminion";
	}

	private void fetchRoutersAsync(final boolean bShowError)
	{
		m_bttnFetchRouters.setEnabled(false);
		Runnable doIt = new Runnable()
		{
			public void run()
			{
				MMRList mmrl = new MMRList(new InfoServiceMMRListFetcher());
				if (!mmrl.updateList()) mmrl = new MMRList(new PlainMMRListFetcher());
				if (!mmrl.updateList())
				{
					if (bShowError)
					{
						JAPDialog.showErrorDialog(getRootPanel(),JAPMessages.getString("mixminionErrorFetchRouters"),LogType.MISC);
					}
					m_bttnFetchRouters.setEnabled(true);
					return;
				}
				m_lastUpdate = System.currentTimeMillis();
				DefaultTableModel m = (DefaultTableModel) m_tableRouters.getModel();
				Vector mmrs = mmrl.getList();
				m.setNumRows(mmrs.size());
				for (int i = 0; i < mmrs.size(); i++)
				{
					MMRDescription mmrd = (MMRDescription) mmrs.elementAt(i);
					m_tableRouters.setValueAt(mmrd.getName(), i, 0);
					m_tableRouters.setValueAt(mmrd.getAddress(), i, 1);
					m_tableRouters.setValueAt(new Integer(mmrd.getPort()), i, 2);
					m_tableRouters.setValueAt(mmrd.getSoftwareVersion(), i, 3);
					m_tableRouters.invalidate();
				}

				m_labelAvailableRouters.setText(JAPMessages.getString("mixminionBorderAvailableRouters:"));
				m_labelAvailableRouters.invalidate();
				getRootPanel().validate();
				m_bttnFetchRouters.setEnabled(true);
			}
		};
		Thread t = new Thread(doIt);
		t.start();
	}

	public void onResetToDefaultsPressed()
	{
		JAPController.resetMixminionPassword();
		m_bttnResetKeyring.setEnabled(false);
		m_sliderPathLen.setValue(JAPConstants.DEFAULT_MIXMINION_ROUTE_LEN);
		m_email.setText(JAPConstants.DEFAULT_MIXMINION_EMAIL);
		m_cbxActive.setSelected(false);
	}
}
