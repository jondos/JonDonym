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

import java.text.DateFormat;
import java.util.Date;
import java.util.Hashtable;
import java.util.Vector;

import java.awt.Dimension;
import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.table.DefaultTableModel;

import anon.tor.ordescription.InfoServiceORListFetcher;
import anon.tor.ordescription.ORList;
import gui.GUIUtils;
import gui.JAPMessages;
import gui.JAPJIntField;
import gui.dialog.JAPDialog;
import logging.LogType;
import java.util.Dictionary;
import anon.tor.ordescription.ORDescriptor;
import javax.swing.JTextField;
import anon.tor.ordescription.PlainORListFetcher;

final class JAPConfTor extends AbstractJAPConfModule implements ActionListener
{
	public static final String MSG_ACTIVATE = JAPConfTor.class.getName() + "_activate";

	private static final int MIN_CON_PER_PATH = 1;
	private static final int MAX_CON_PER_PATH = 5;

	private JCheckBox m_cbxActive;
	private JTable m_tableRouters;
	private JSlider m_sliderMaxPathLen, m_sliderMinPathLen, m_sliderConnectionsPerPath;
	private JButton m_bttnFetchRouters;
	private JLabel m_labelAvailableRouters;
	private JCheckBox m_cbPreCreateRoutes;
	private JCheckBox m_cbNoDefaultTorServer;
	private JTextField m_tfTorDirServerHostName;
	private JAPJIntField m_jintfieldTorDirServerPort;
	private JLabel m_lblMaxPathLen, m_lblMinPathLen, m_lblPathSwitchTime;
	private JScrollPane m_scrollPane;
	private JPanel m_panelSlider;
	private TitledBorder m_border;
	private DateFormat ms_dateFormat = DateFormat.getDateTimeInstance(DateFormat.MEDIUM,
		DateFormat.SHORT);
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

	public JAPConfTor()
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
		m_cbxActive = new JCheckBox(JAPMessages.getString(MSG_ACTIVATE), true);
		m_cbxActive.addChangeListener(new ChangeListener()
		{
			public void stateChanged(ChangeEvent a_event)
			{
				m_labelAvailableRouters.setEnabled(m_cbxActive.isSelected());
				m_tableRouters.setEnabled(m_cbxActive.isSelected());
				m_bttnFetchRouters.setEnabled(m_cbxActive.isSelected());
				m_sliderMinPathLen.setEnabled(m_cbxActive.isSelected());
				m_sliderMaxPathLen.setEnabled(m_cbxActive.isSelected());
				m_sliderConnectionsPerPath.setEnabled(m_cbxActive.isSelected());
				m_cbPreCreateRoutes.setEnabled(m_cbxActive.isSelected());
				m_lblMinPathLen.setEnabled(m_cbxActive.isSelected());
				m_lblMaxPathLen.setEnabled(m_cbxActive.isSelected());
				m_scrollPane.setEnabled(m_cbxActive.isSelected());
				m_lblPathSwitchTime.setEnabled(m_cbxActive.isSelected());
				m_border = new TitledBorder(m_border.getTitle());
				if (m_cbxActive.isSelected())
				{
					m_bttnFetchRouters.setDisabledIcon(GUIUtils.loadImageIcon(JAPConstants.
						IMAGE_RELOAD_DISABLED, true, false));
				}
				else
				{
					m_border.setTitleColor(Color.gray);
					m_bttnFetchRouters.setDisabledIcon(
						GUIUtils.loadImageIcon(JAPConstants.IMAGE_RELOAD_ROLLOVER, true, false));
				}
				m_panelSlider.setBorder(m_border);

				Dictionary d;
				d = m_sliderMaxPathLen.getLabelTable();
				for (int i = JAPConstants.TOR_MIN_ROUTE_LEN; i <= JAPConstants.TOR_MAX_ROUTE_LEN; i++)
				{
					( (JLabel) d.get(new Integer(i))).setEnabled(m_sliderMaxPathLen.isEnabled());
				}
				d = m_sliderMinPathLen.getLabelTable();
				for (int i = JAPConstants.TOR_MIN_ROUTE_LEN; i <= JAPConstants.TOR_MAX_ROUTE_LEN; i++)
				{
					( (JLabel) d.get(new Integer(i))).setEnabled(m_sliderMinPathLen.isEnabled());
				}
				d = m_sliderConnectionsPerPath.getLabelTable();
				for (int i = MIN_CON_PER_PATH; i <= MAX_CON_PER_PATH; i++)
				{
					( (JLabel) d.get(new Integer(i))).setEnabled(m_sliderConnectionsPerPath.isEnabled());
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
		m_labelAvailableRouters = new JLabel(JAPMessages.getString("torBorderAvailableRouters") + ":");
		c2.fill = GridBagConstraints.HORIZONTAL;
		c2.weightx = 1;
		c2.weighty = 0;
		p.add(m_labelAvailableRouters, c2);

		DefaultTableModel model = new DefaultTableModel();
		model.addColumn(JAPMessages.getString("torRouterName"));
		model.addColumn(JAPMessages.getString("torRouterAdr"));
		model.addColumn(JAPMessages.getString("torRouterPort"));
		model.addColumn(JAPMessages.getString("torRouterSoftware"));
		model.setNumRows(3);
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
		m_bttnFetchRouters = new JButton(JAPMessages.getString("torBttnFetchRouters"));
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
		p.setBorder(new TitledBorder(JAPMessages.getString("torBorderTorDirServer")));
		GridBagConstraints c4 = new GridBagConstraints();
		m_cbNoDefaultTorServer = new JCheckBox(JAPMessages.getString("torCheckBoxNoDefaultDirServer"));
		p.add(m_cbNoDefaultTorServer, c4);
		c4.gridx = 1;
		p.add(new JLabel(JAPMessages.getString("torDirServerHostName")), c4);
		m_tfTorDirServerHostName = new JTextField();
		c4.gridx = 2;
		p.add(m_tfTorDirServerHostName, c4);
		m_jintfieldTorDirServerPort = new JAPJIntField(0x00FFFF);
		c4.gridx = 3;
		p.add(m_jintfieldTorDirServerPort, c4);
		c4.gridx = 4;
		p.add(new JLabel(JAPMessages.getString("torDirServerPort")), c4);

		c.gridy = 3;
		c.weighty = 0;
		c.fill = GridBagConstraints.HORIZONTAL;
		panelRoot.add(p, c);

		p = new JPanel(new GridBagLayout());
		GridBagConstraints c3 = new GridBagConstraints();
		c3.anchor = GridBagConstraints.NORTHWEST;
		c3.insets = new Insets(2, 5, 2, 5);
		c3.fill = GridBagConstraints.NONE;
		m_lblMinPathLen = new JLabel(JAPMessages.getString("torPrefMinPathLen"));
		p.add(m_lblMinPathLen, c3);
		m_sliderMinPathLen = new JSlider();
		m_sliderMinPathLen.setPaintLabels(true);
		m_sliderMinPathLen.setPaintTicks(true);
		m_sliderMinPathLen.setMajorTickSpacing(1);
		m_sliderMinPathLen.setSnapToTicks(true);
		m_sliderMinPathLen.setMinimum(JAPConstants.TOR_MIN_ROUTE_LEN);
		m_sliderMinPathLen.setMaximum(JAPConstants.TOR_MAX_ROUTE_LEN);
		m_sliderMinPathLen.addChangeListener(new ChangeListener()
		{
			public void stateChanged(ChangeEvent e)
			{
				if (m_sliderMaxPathLen.getValue() < m_sliderMinPathLen.getValue())
				{
					m_sliderMaxPathLen.setValue(m_sliderMinPathLen.getValue());
				}
			}
		});
		c3.gridx = 1;
		c3.fill = GridBagConstraints.HORIZONTAL;
		p.add(m_sliderMinPathLen, c3);
		c3.gridx = 0;
		c3.gridy = 1;
		c3.fill = GridBagConstraints.NONE;
		m_lblMaxPathLen = new JLabel(JAPMessages.getString("torPrefMaxPathLen"));
		p.add(m_lblMaxPathLen, c3);
		m_sliderMaxPathLen = new JSlider();
		m_sliderMaxPathLen.setMinimum(JAPConstants.TOR_MIN_ROUTE_LEN);
		m_sliderMaxPathLen.setMaximum(JAPConstants.TOR_MAX_ROUTE_LEN);
		m_sliderMaxPathLen.setPaintLabels(true);
		m_sliderMaxPathLen.setPaintTicks(true);
		m_sliderMaxPathLen.setMajorTickSpacing(1);
		m_sliderMaxPathLen.setMinorTickSpacing(1);
		m_sliderMaxPathLen.setSnapToTicks(true);
		m_sliderMaxPathLen.addChangeListener(new ChangeListener()
		{
			public void stateChanged(ChangeEvent e)
			{
				if (m_sliderMaxPathLen.getValue() < m_sliderMinPathLen.getValue())
				{
					m_sliderMinPathLen.setValue(m_sliderMaxPathLen.getValue());
				}
			}
		});
		c3.gridx = 1;
		c3.fill = GridBagConstraints.HORIZONTAL;
		p.add(m_sliderMaxPathLen, c3);
		c3.gridx = 0;
		c3.gridy = 2;
		c3.fill = GridBagConstraints.NONE;
		m_lblPathSwitchTime = new JLabel(JAPMessages.getString("torPrefPathSwitchTime"));
		p.add(m_lblPathSwitchTime, c3);
		m_sliderConnectionsPerPath = new JSlider();
		Hashtable sliderLabels = new Hashtable();
		sliderLabels.put(new Integer(1), new JLabel("10"));
		sliderLabels.put(new Integer(2), new JLabel("50"));
		sliderLabels.put(new Integer(3), new JLabel("100"));
		sliderLabels.put(new Integer(4), new JLabel("500"));
		sliderLabels.put(new Integer(5), new JLabel("1000"));
		m_sliderConnectionsPerPath.setLabelTable(sliderLabels);
		m_sliderConnectionsPerPath.setMinimum(1);
		m_sliderConnectionsPerPath.setMaximum(5);
		m_sliderConnectionsPerPath.setMajorTickSpacing(1);
		m_sliderConnectionsPerPath.setMinorTickSpacing(1);
		m_sliderConnectionsPerPath.setSnapToTicks(true);
		m_sliderConnectionsPerPath.setPaintLabels(true);
		m_sliderConnectionsPerPath.setPaintTicks(true);
		c3.gridx = 1;
		c3.weightx = 1;
		c3.fill = GridBagConstraints.HORIZONTAL;
		p.add(m_sliderConnectionsPerPath, c3);
		m_cbPreCreateRoutes = new JCheckBox(JAPMessages.getString("ngConfAnonGeneralPreCreateRoutes"));
		c3.gridy++;
		c3.gridx = 0;
		c3.gridwidth = 2;
		p.add(m_cbPreCreateRoutes, c3);
		m_border = new TitledBorder(JAPMessages.getString("torBorderPreferences"));
		p.setBorder(m_border);
		m_panelSlider = p;

		c.gridy = 4;
		c.weighty = 0;
		c.fill = GridBagConstraints.HORIZONTAL;
		panelRoot.add(p, c);
	}

	public String getTabTitle()
	{
		return "Tor";
	}

	/**
	 * actionPerformed
	 *
	 * @param actionEvent ActionEvent
	 */
	public void actionPerformed(ActionEvent actionEvent)
	{
		if (actionEvent.getActionCommand().equals("enableTor"))
		{
			updateGuiOutput();
		}
		else if (actionEvent.getActionCommand().equals("fetchRouters"))
		{
			fetchRoutersAsync(true);

		}
	}

	protected boolean onOkPressed()
	{
		JAPModel.getInstance().setTorActivated(m_cbxActive.isSelected());
		int i = m_sliderConnectionsPerPath.getValue();
		int[] ar =
			{
			10, 50, 100, 500, 1000};
		JAPController.setTorMaxConnectionsPerRoute(ar[i - 1]);
		JAPController.setTorRouteLen(m_sliderMinPathLen.getValue(), m_sliderMaxPathLen.getValue());
		JAPController.setPreCreateAnonRoutes(m_cbPreCreateRoutes.isSelected());
		JAPController.setTorUseNoneDefaultDirServer(m_cbNoDefaultTorServer.isSelected());
		return true;
	}

	protected void onUpdateValues()
	{
		//synchronized (JAPConf.getInstance())
		{
			updateGuiOutput();
		}
	}

	public String getHelpContext()
	{
		return "services_tor";
	}

	private void updateGuiOutput()
	{
		int i = JAPModel.getTorMaxConnectionsPerRoute();
		if (i < 25)
		{
			i = 1;
		}
		else if (i < 75)
		{
			i = 2;
		}
		else if (i < 250)
		{
			i = 3;
		}
		else if (i < 750)
		{
			i = 4;
		}
		else
		{
			i = 5;
		}
		m_sliderConnectionsPerPath.setValue(i);
		m_sliderMaxPathLen.setValue(JAPModel.getTorMaxRouteLen());
		m_sliderMinPathLen.setValue(JAPModel.getTorMinRouteLen());
		m_cbPreCreateRoutes.setSelected(JAPModel.isPreCreateAnonRoutesEnabled());
		m_cbxActive.setSelected(JAPModel.getInstance().isTorActivated());
		m_cbNoDefaultTorServer.setSelected(JAPModel.isTorNoneDefaultDirServerEnabled());
	}

	private void fetchRoutersAsync(final boolean bShowError)
	{
		m_bttnFetchRouters.setEnabled(false);
		Runnable doIt = new Runnable()
		{
			public void run()
			{
				ORList ol = null;
				if (JAPModel.isTorNoneDefaultDirServerEnabled())
				{
					ol = new ORList(new PlainORListFetcher("141.76.45.45", 9030));
				}
				else
				{
					ol = new ORList(new InfoServiceORListFetcher());
				}
				if (!ol.updateList())
				{
					if (bShowError)
					{
						JAPDialog.showErrorDialog(getRootPanel(),
												  JAPMessages.getString("torErrorFetchRouters"), LogType.MISC);
					}
					m_bttnFetchRouters.setEnabled(true);
					return;
				}
				DefaultTableModel m = (DefaultTableModel) m_tableRouters.getModel();
				Vector ors = ol.getList();
				m.setNumRows(ors.size());
				for (int i = 0; i < ors.size(); i++)
				{
					ORDescriptor ord = (ORDescriptor) ors.elementAt(i);
					m_tableRouters.setValueAt(ord.getName(), i, 0);
					m_tableRouters.setValueAt(ord.getAddress(), i, 1);
					m_tableRouters.setValueAt(new Integer(ord.getPort()), i, 2);
					m_tableRouters.setValueAt(ord.getSoftware(), i, 3);
					m_tableRouters.invalidate();
				}
				Date published = ol.getPublished();
				String strPublished = JAPMessages.getString("unknown");
				if (published != null)
				{
					strPublished = ms_dateFormat.format(published);
				}
				m_labelAvailableRouters.setText(JAPMessages.getString("torBorderAvailableRouters") + " (" +
												strPublished + "):");
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
		m_cbPreCreateRoutes.setSelected(JAPConstants.DEFAULT_TOR_PRECREATE_ROUTES);
		m_sliderMaxPathLen.setValue(JAPConstants.DEFAULT_TOR_MAX_ROUTE_LEN);
		m_sliderMinPathLen.setValue(JAPConstants.DEFAULT_TOR_MIN_ROUTE_LEN);
		m_sliderConnectionsPerPath.setValue(JAPConstants.DEFAULT_TOR_MAX_CONNECTIONS_PER_ROUTE);
		m_cbxActive.setSelected(false);
		m_cbNoDefaultTorServer.setSelected(JAPConstants.DEFAULT_TOR_USE_NONE_DEFAULT_DIR_SERVER);
	}
}
