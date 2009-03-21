/*
 Copyright (c) 2000 - 2005, The JAP-Team
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

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JTextField;
import javax.swing.WindowConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;

import anon.infoservice.ImmutableListenerInterface;
import anon.infoservice.ListenerInterface;
import anon.infoservice.ProxyInterface;
import anon.util.JAPMessages;
import gui.JAPHtmlMultiLineLabel;
import gui.JAPJIntField;
import gui.dialog.JAPDialog;
import jap.forward.JAPRoutingMessage;
import jap.forward.JAPRoutingSettings;
import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;

/**
 * This is the configuration GUI for the JAP forwarding client component.
 */
public class JAPConfNetwork extends AbstractJAPConfModule
{
	private static final String MSG_LISTENER_CHANGED = JAPConfNetwork.class.getName() + "_listenerChanged";
	private static final String MSG_ACCESS_TO_JAP = JAPConfNetwork.class.getName() + "_accessToJAP";
	public static final String MSG_SLOW_ANTI_CENSORSHIP = JAPConfNetwork.class.getName() + "_slowAntiCensorship";
	private static final String MSG_SLOW_ANTI_CENSORSHIP_Q = JAPConfNetwork.class.getName() + "_slowAntiCensorshipQuestion";

	//private JAPHtmlMultiLineLabel m_descLabel;


	private JAPJIntField m_tfListenerPortNumber;
	private JCheckBox m_cbListenerIsLocal;
	private JLabel m_labelPortnumber1;

	private TitledBorder m_borderSettingsListener;

	private JCheckBox m_cbProxy;
	private JCheckBox m_settingsForwardingClientConfigNeedForwarderBox;
	private JAPJIntField m_tfProxyPortNumber;
	private JTextField m_tfProxyHost;
	private JComboBox m_comboProxyType;
	private JCheckBox m_cbProxyAuthentication;
	private JTextField m_tfProxyAuthenticationUserID;
	private JLabel m_labelProxyHost, m_labelProxyPort, m_labelProxyType, m_labelProxyAuthUserID;


	/**
	 * Constructor for JAPConfForwardingClient. We do some initialization here.
	 */
	public JAPConfNetwork()
	{
		super(new JAPConfNetworkSavePoint());
	}
	
	protected boolean initObservers()
	{
		if (super.initObservers())
		{
			synchronized(LOCK_OBSERVABLE)
			{
				Observer clientSettingsObserver = new Observer()
				{
					/**
					 * This is the observer implementation. If the client settings were changed, we update the
					 * checkboxes.
					 *
					 * @param a_notifier The observed Object. This should always be JAPRoutingSettings or the
					 *                   module internal message system at the moment.
					 * @param a_message The reason of the notification. This should always be a JAPRoutingMessage
					 *                  or null at the moment.
					 */
					public void update(Observable a_notifier, Object a_message)
					{
						try
						{
							if (a_notifier == JAPModel.getInstance().getRoutingSettings())
							{
								if ( ( (JAPRoutingMessage) (a_message)).getMessageCode() ==
									JAPRoutingMessage.CLIENT_SETTINGS_CHANGED)
								{
									/* the client settings were changed -> update the state of the checkboxes, maybe
									 * also make them invisible, if they are not needed
									 */
									if (JAPModel.getInstance().getRoutingSettings().isConnectViaForwarder())
									{
										m_settingsForwardingClientConfigNeedForwarderBox.setSelected(true);
									}
									else
									{
										m_settingsForwardingClientConfigNeedForwarderBox.setSelected(false);
									}
								}
							}
						}
						catch (Exception e)
						{
							/* should not happen */
							LogHolder.log(LogLevel.EXCEPTION, LogType.GUI, e);
						}
					}
				};
			
				JAPModel.getInstance().getRoutingSettings().addObserver(clientSettingsObserver);
				/* tricky: initialize the checkboxes by calling the observer */
				clientSettingsObserver.update(JAPModel.getInstance().getRoutingSettings(),
											  new JAPRoutingMessage(JAPRoutingMessage.CLIENT_SETTINGS_CHANGED));
				return true;
			}
		}
		return false;
	}

	public void actionPerformed(ActionEvent e)
	{
		if (e.getSource() == m_cbProxyAuthentication)
		{
			if (m_cbProxyAuthentication.isSelected())
			{
				JAPModel.getInstance().setUseProxyAuthentication(true);
			}
			else
			{
				JAPModel.getInstance().setUseProxyAuthentication(false);
			}
		}
	}

	public void onResetToDefaultsPressed()
	{
		m_tfListenerPortNumber.setInt(JAPConstants.DEFAULT_PORT_NUMBER);
		m_cbListenerIsLocal.setSelected(JAPConstants.DEFAULT_LISTENER_IS_LOCAL);

		m_cbProxy.setSelected(false);
	}

	/**
	 * Creates the forwarding client root panel with all child components.
	 */
	public void recreateRootPanel()
	{
		JPanel rootPanel = getRootPanel();

		synchronized (this)
		{
			/* clear the whole root panel */
			rootPanel.removeAll();

			/* recreate all parts of the forwarding client configuration dialog */
			JPanel clientPanel = createForwardingClientConfigPanel();

			GridBagLayout rootPanelLayout = new GridBagLayout();
			rootPanel.setLayout(rootPanelLayout);

			GridBagConstraints rootPanelConstraints = new GridBagConstraints();
			rootPanelConstraints.anchor = GridBagConstraints.NORTHWEST;
			rootPanelConstraints.fill = GridBagConstraints.BOTH;

			rootPanelConstraints.gridx = 0;
			rootPanelConstraints.gridy = 0;
			rootPanelConstraints.weightx = 1.0;
			rootPanelConstraints.weighty = 0.0;
			rootPanelLayout.setConstraints(clientPanel, rootPanelConstraints);
			rootPanel.add(clientPanel);

			rootPanelConstraints.fill = GridBagConstraints.HORIZONTAL;
			rootPanelConstraints.weighty = 0.0;
			rootPanelConstraints.gridy++;

			rootPanel.add(buildPortPanel(), rootPanelConstraints);
			rootPanelConstraints.gridy++;
			rootPanelConstraints.weighty = 1.0;
			rootPanelConstraints.fill = GridBagConstraints.BOTH;
			rootPanel.add(buildProxyPanel(), rootPanelConstraints);
		}
	}

	/**
	 * Returns the title for the forwarding client configuration within the configuration tree.
	 *
	 * @return The title for the forwarding client configuration leaf within the tree.
	 */
	public String getTabTitle()
	{
		return JAPMessages.getString("ngTreeNetwork");
	}

	/**
	 * Creates the forwarding client configuration panel.
	 *
	 * @return The forwarding client config panel.
	 */
	private JPanel createForwardingClientConfigPanel()
	{
		final JPanel clientPanel = new JPanel();

		m_settingsForwardingClientConfigNeedForwarderBox =
			new JCheckBox(JAPMessages.getString("settingsForwardingClientConfigNeedForwarderBox"));
		//settingsForwardingClientConfigNeedForwarderBox.setFont(getFontSetting());
		m_settingsForwardingClientConfigNeedForwarderBox.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent event)
			{
				if (m_settingsForwardingClientConfigNeedForwarderBox.isSelected())
				{
					if (JAPDialog.showYesNoDialog(JAPConfNetwork.this.getRootPanel(),
												  JAPMessages.getString(MSG_SLOW_ANTI_CENSORSHIP) +
												  "<br><br>" +
												  JAPMessages.getString(MSG_SLOW_ANTI_CENSORSHIP_Q)))
					{

						/* we shall enable the connect-via-forwarder feature */
						if (JAPModel.getInstance().getRoutingSettings().getRoutingMode() ==
							JAPRoutingSettings.ROUTING_MODE_SERVER)
						{
							/* we have to shutdown the server first -> user has to confirm this */
							showForwardingClientConfirmServerShutdownDialog(clientPanel);
							/* maybe the user has canceled the shutdown -> update the selection state of the
							 * checkbox
							 */
							if (!JAPModel.getInstance().getRoutingSettings().isConnectViaForwarder())
							{
								m_settingsForwardingClientConfigNeedForwarderBox.setSelected(false);
							}
						}
						else
						{
							/* we can directly enable the connect-via-forwarder setting, because the forwarding
							 * server isn't running
							 */
							JAPModel.getInstance().getRoutingSettings().setConnectViaForwarder(true);
						}
					}
					else
					{
						m_settingsForwardingClientConfigNeedForwarderBox.setSelected(false);
					}
				}
				else
				{
					/* disable the connect-via-forwarder setting in JAPRoutingSettings */
					JAPModel.getInstance().getRoutingSettings().setConnectViaForwarder(false);
				}
			}
		});

		TitledBorder settingsForwardingClientConfigBorder = new TitledBorder(
			  JAPMessages.getString("settingsForwardingClientConfigBorder"));
		//settingsForwardingClientConfigBorder.setTitleFont(getFontSetting());
		clientPanel.setBorder(settingsForwardingClientConfigBorder);

		GridBagLayout clientPanelLayout = new GridBagLayout();
		clientPanel.setLayout(clientPanelLayout);

		GridBagConstraints clientPanelConstraints = new GridBagConstraints();
		clientPanelConstraints.anchor = GridBagConstraints.NORTHWEST;
		clientPanelConstraints.fill = GridBagConstraints.NONE;
		clientPanelConstraints.weightx = 1.0;
		/* we need at least a minimal value to prevent horinzontal centering if no other component
		 * is visible
		 */
		clientPanelConstraints.weighty = 0.0001;
		clientPanelConstraints.gridx = 0;
		clientPanelConstraints.gridy = 0;
		clientPanelConstraints.insets = new Insets(5, 5, 5, 5);
		clientPanelLayout.setConstraints(m_settingsForwardingClientConfigNeedForwarderBox,
										 clientPanelConstraints);
		clientPanel.add(m_settingsForwardingClientConfigNeedForwarderBox);

		clientPanelConstraints.insets = new Insets(0, 20, 5, 5);


		/*
		clientPanelConstraints.gridy++;
		clientPanelConstraints.weightx = 1.0;
		clientPanelConstraints.fill = GridBagConstraints.HORIZONTAL;
		clientPanel.add(new JSeparator(), clientPanelConstraints);
		clientPanelConstraints.gridy++;
		clientPanelConstraints.weighty = 1.0;

		m_descLabel =
			new JAPHtmlMultiLineLabel(JAPMessages.getString("forwardingClientDesc"));
		clientPanel.add(m_descLabel, clientPanelConstraints);*/
		return clientPanel;
	}

	/**
	 * Shows the forwarding server shutdown confirmation dialog. This dialog is necessary if the
	 * forwarding server is running when the connect-via-forwarder feature is enabled, because the
	 * components for starting/stopping the forwarding server will be disabled after that.
	 *
	 * @param a_parentComponent The component where the dialog will be centered over.
	 */
	private void showForwardingClientConfirmServerShutdownDialog(Component a_parentComponent)
	{
		final JAPDialog confirmDialog = new JAPDialog(a_parentComponent,
			JAPMessages.getString("settingsForwardingClientConfigConfirmServerShutdownDialogTitle"));
		confirmDialog.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
		JPanel confirmPanel = new JPanel();
		confirmDialog.getContentPane().add(confirmPanel);

		JAPHtmlMultiLineLabel settingsForwardingClientConfigConfirmServerShutdownLabel = new
			JAPHtmlMultiLineLabel(
				JAPMessages.getString("settingsForwardingClientConfigConfirmServerShutdownLabel"));

		JButton settingsForwardingClientConfigConfirmServerShutdownShutdownButton = new JButton(
			  JAPMessages.getString("settingsForwardingClientConfigConfirmServerShutdownShutdownButton"));
		//settingsForwardingClientConfigConfirmServerShutdownShutdownButton.setFont(getFontSetting());
		settingsForwardingClientConfigConfirmServerShutdownShutdownButton.addActionListener(new
			ActionListener()
		{
			public void actionPerformed(ActionEvent event)
			{
				/* if the shutdown button is pressed, shutdown the server and enable the
				 * connect-via-forwarder setting
				 */
				JAPModel.getInstance().getRoutingSettings().setRoutingMode(JAPRoutingSettings.
					ROUTING_MODE_DISABLED);
				JAPModel.getInstance().getRoutingSettings().setConnectViaForwarder(true);
				confirmDialog.dispose();
			}
		});

		JButton settingsForwardingClientConfigConfirmServerShutdownCancelButton = new JButton(
			  JAPMessages.getString("cancelButton"));
		//settingsForwardingClientConfigConfirmServerShutdownCancelButton.setFont(getFontSetting());
		settingsForwardingClientConfigConfirmServerShutdownCancelButton.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent event)
			{
				/* if the Cancel button is pressed, leave everything untouched */
				confirmDialog.dispose();
			}
		});

		GridBagLayout confirmPanelLayout = new GridBagLayout();
		confirmPanel.setLayout(confirmPanelLayout);

		GridBagConstraints confirmPanelConstraints = new GridBagConstraints();
		confirmPanelConstraints.anchor = GridBagConstraints.NORTH;
		confirmPanelConstraints.fill = GridBagConstraints.NONE;
		confirmPanelConstraints.weighty = 0.0;
		confirmPanelConstraints.weightx = 1.0;

		confirmPanelConstraints.gridx = 0;
		confirmPanelConstraints.gridy = 0;
		confirmPanelConstraints.gridwidth = 2;
		confirmPanelConstraints.insets = new Insets(10, 5, 20, 5);
		confirmPanelLayout.setConstraints(settingsForwardingClientConfigConfirmServerShutdownLabel,
										  confirmPanelConstraints);
		confirmPanel.add(settingsForwardingClientConfigConfirmServerShutdownLabel);

		confirmPanelConstraints.gridx = 0;
		confirmPanelConstraints.gridy = 1;
		confirmPanelConstraints.weighty = 1.0;
		confirmPanelConstraints.fill = GridBagConstraints.HORIZONTAL;
		confirmPanelConstraints.gridwidth = 1;
		confirmPanelConstraints.insets = new Insets(0, 5, 15, 5);
		confirmPanelLayout.setConstraints(settingsForwardingClientConfigConfirmServerShutdownShutdownButton,
										  confirmPanelConstraints);
		confirmPanel.add(settingsForwardingClientConfigConfirmServerShutdownShutdownButton);

		confirmPanelConstraints.gridx = 1;
		confirmPanelConstraints.gridy = 1;
		confirmPanelConstraints.insets = new Insets(0, 5, 15, 5);
		confirmPanelLayout.setConstraints(settingsForwardingClientConfigConfirmServerShutdownCancelButton,
										  confirmPanelConstraints);
		confirmPanel.add(settingsForwardingClientConfigConfirmServerShutdownCancelButton);

		confirmDialog.pack();
		confirmDialog.setVisible(true);
	}

	protected boolean onOkPressed()
	{
		String s = null;
		int i;

		//--------------
		//checking Listener Port Number
		try
		{
			i = Integer.parseInt(m_tfListenerPortNumber.getText().trim());
		}
		catch (Exception e)
		{
			i = -1;
		}
		if (!ProxyInterface.isValidPort(i))
		{
			JAPDialog.showErrorDialog(getRootPanel(), JAPMessages.getString("errorListenerPortWrong"),
									  LogType.MISC);
			return false;
		}

		//Checking Firewall Settings (Host + Port)
		if (m_cbProxy.isSelected())
		{
			s = m_tfProxyHost.getText().trim();
			if (s == null || s.equals(""))
			{
				JAPDialog.showErrorDialog(getRootPanel(),
										  JAPMessages.getString("errorFirewallHostNotNull"), LogType.MISC);
				JAPConf.getInstance().selectCard(JAPConf.NETWORK_TAB, null);
				return false;
			}
			try
			{
				i = Integer.parseInt(m_tfProxyPortNumber.getText().trim());
			}
			catch (Exception e)
			{
				i = -1;
			}
			if (!ProxyInterface.isValidPort(i))
			{
				JAPDialog.showErrorDialog(getRootPanel(),
										  JAPMessages.getString("errorFirewallServicePortWrong"),
										  LogType.MISC);
				JAPConf.getInstance().selectCard(JAPConf.NETWORK_TAB, null);
				return false;
			}
			if (m_cbProxyAuthentication.isSelected())
			{
				s = m_tfProxyAuthenticationUserID.getText().trim();
				if (s == null || s.equals(""))
				{
					JAPDialog.showErrorDialog(getRootPanel(),
											  JAPMessages.getString("errorFirewallAuthUserIDNotNull"),
											  LogType.MISC);
					JAPConf.getInstance().selectCard(JAPConf.NETWORK_TAB, null);
					return false;
				}
			}
		}


		if (JAPModel.getHttpListenerPortNumber() != m_tfListenerPortNumber.getInt())
		{
			JAPConf.getInstance().addNeedRestart(new JAPConf.AbstractRestartNeedingConfigChange()
			{
				public String getName()
				{
					return JAPMessages.getString("confListenerTab");
				}

				public String getMessage()
				{
					return JAPMessages.getString(MSG_LISTENER_CHANGED);
				}

				public void doChange()
				{
					JAPModel.getInstance().setHttpListenerPortNumber(m_tfListenerPortNumber.getInt());
				}
			});
		}
		if (JAPModel.isHttpListenerLocal() != m_cbListenerIsLocal.isSelected())
		{
			JAPConf.getInstance().addNeedRestart(new JAPConf.AbstractRestartNeedingConfigChange()
			{
				public String getName()
				{
					return JAPMessages.getString(JAPMessages.getString(MSG_ACCESS_TO_JAP));
				}

				public void doChange()
				{
					JAPModel.getInstance().setHttpListenerIsLocal(m_cbListenerIsLocal.isSelected());
				}
			});
		}

		//m_Controller.setSocksPortNumber(m_tfListenerPortNumberSocks.getInt());
		// Firewall settings
		int port = -1;
		try
		{
			port = Integer.parseInt(m_tfProxyPortNumber.getText().trim());
		}
		catch (Exception e)
		{}
		;
		int firewallType = ImmutableListenerInterface.PROTOCOL_TYPE_HTTP;
		if (m_comboProxyType.getSelectedIndex() == 1)
		{
			firewallType = ImmutableListenerInterface.PROTOCOL_TYPE_SOCKS;
		}
		JAPController.getInstance().changeProxyInterface(
			new ProxyInterface(m_tfProxyHost.getText().trim(),
							   port,
							   firewallType,
							   m_tfProxyAuthenticationUserID.getText().trim(),
							   JAPController.getInstance().getPasswordReader(),
							   m_cbProxyAuthentication.isSelected(),
							   m_cbProxy.isSelected()),
			m_cbProxyAuthentication.isSelected(), getRootPanel());

	  return true;
}

	protected void onUpdateValues()
	{
		//synchronized (JAPConf.getInstance())
		{
			//m_descLabel.setFont(new JLabel().getFont());
	
			// listener tab
			m_tfListenerPortNumber.setInt(JAPModel.getHttpListenerPortNumber());
			m_cbListenerIsLocal.setSelected(JAPModel.isHttpListenerLocal());
			//m_tfListenerPortNumberSocks.setInt(JAPModel.getSocksListenerPortNumber());
			//boolean bSocksVisible = JAPModel.isTorEnabled();
			//m_tfListenerPortNumberSocks.setVisible(bSocksVisible);
			//m_labelSocksPortNumber.setVisible(bSocksVisible);
			//m_cbListenerSocks.setSelected(m_Controller.getUseSocksPort());
			// firewall tab
			ProxyInterface proxyInterface = JAPModel.getInstance().getProxyInterface();
			boolean bEnableProxy = proxyInterface != null &&
				proxyInterface.isValid();
			m_cbProxy.setSelected(bEnableProxy);
			m_tfProxyHost.setEnabled(bEnableProxy);
			m_tfProxyPortNumber.setEnabled(bEnableProxy);
			m_comboProxyType.setEnabled(bEnableProxy);
			m_tfProxyAuthenticationUserID.setEnabled(bEnableProxy);
			m_labelProxyHost.setEnabled(bEnableProxy);
			m_labelProxyPort.setEnabled(bEnableProxy);
			m_labelProxyType.setEnabled(bEnableProxy);
			if (proxyInterface == null ||
				proxyInterface.getProtocol() ==
				ImmutableListenerInterface.PROTOCOL_TYPE_HTTP)
			{
				m_comboProxyType.setSelectedIndex(0);
			}
			else
			{
				m_comboProxyType.setSelectedIndex(1);
			}
			m_cbProxyAuthentication.setEnabled(bEnableProxy);
			if (proxyInterface != null)
			{
				m_tfProxyHost.setText(proxyInterface.getHost());
				m_tfProxyPortNumber.setText(String.valueOf(
					proxyInterface.getPort()));
				m_tfProxyAuthenticationUserID.setText(
					proxyInterface.getAuthenticationUserID());
				m_cbProxyAuthentication.setSelected(
					proxyInterface.isAuthenticationUsed());
			}
			m_labelProxyAuthUserID.setEnabled(m_cbProxyAuthentication.isSelected() & bEnableProxy);
			m_tfProxyAuthenticationUserID.setEnabled(m_cbProxyAuthentication.isSelected() & bEnableProxy);
			if (m_tfProxyPortNumber.getText().trim().equalsIgnoreCase("-1"))
			{
				m_tfProxyPortNumber.setText("");
			}
		}
	}

	JPanel buildPortPanel()
	{
		m_labelPortnumber1 = new JLabel(JAPMessages.getString("settingsPort"));
		//m_labelPortnumber1.setFont(m_fontControls);
		//m_labelPortnumber2.setFont(m_fontControls);
		m_tfListenerPortNumber = new JAPJIntField(ListenerInterface.PORT_MAX_VALUE);
		//m_tfListenerPortNumber.setFont(m_fontControls);
		/*
		   m_tfListenerPortNumber.addActionListener(new ActionListener()
		   {
		 public void actionPerformed(ActionEvent e)
		 {
		  okPressed(false);
		 }
		   });*/
		m_cbListenerIsLocal = new JCheckBox(JAPMessages.getString("settingsListenerCheckBox"));
		//m_cbListenerIsLocal.setFont(m_fontControls);
		// set Font in listenerCheckBox in same color as in portnumberLabel1
		m_cbListenerIsLocal.setForeground(m_labelPortnumber1.getForeground());

		//m_tfListenerPortNumberSocks.setEnabled(false);
	//}
		//m_tfListenerPortNumberSocks = new JAPJIntField();
		//m_tfListenerPortNumberSocks.setFont(m_fontControls);

		JPanel p = new JPanel();
		p.setLayout(new BorderLayout());
		m_borderSettingsListener = new TitledBorder(JAPMessages.getString("settingsListenerBorder"));
		//m_borderSettingsListener.setTitleFont(m_fontControls);
		p.setBorder(m_borderSettingsListener);
		JPanel p1 = new JPanel();
		GridBagLayout g = new GridBagLayout();
		p1.setLayout(g);
		p1.setBorder(new EmptyBorder(5, 10, 10, 10));
		GridBagConstraints c = new GridBagConstraints();
		c.anchor = GridBagConstraints.NORTHWEST;
		c.gridy = 0;
		c.gridx = 0;
		c.weightx = 0;
		c.fill = GridBagConstraints.NONE;
		g.setConstraints(m_tfListenerPortNumber, c);
		p1.add(m_tfListenerPortNumber);
		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridwidth = 1;
		c.gridx = 1;
		c.gridy = 0;
		c.weightx = 1;
		c.weighty = 0;
		Insets normInsets = new Insets(5, 5, 5, 5);
		c.insets = normInsets;
		g.setConstraints(m_labelPortnumber1, c);
		p1.add(m_labelPortnumber1);

		c.insets = normInsets;
		c.gridx = 0;
		c.gridy = 2;
		c.gridwidth = 2;
		c.insets = new Insets(10, 0, 0, 0);
		g.setConstraints(m_cbListenerIsLocal, c);
		p1.add(m_cbListenerIsLocal);

		p.add(p1, BorderLayout.NORTH);

		return p;
	}

	JPanel buildProxyPanel()
	{
		m_cbProxy = new JCheckBox(JAPMessages.getString("settingsProxyCheckBox"));
		//m_cbProxy.setFont(m_fontControls);
		m_comboProxyType = new JComboBox();
		//m_comboProxyType.setFont(m_fontControls);
		m_comboProxyType.addItem(JAPMessages.getString("settingsProxyTypeHTTP"));
		m_comboProxyType.addItem(JAPMessages.getString("settingsProxyTypeSOCKS"));
		m_tfProxyHost = new JTextField(20);
		//m_tfProxyHost.setFont(m_fontControls);
		m_tfProxyPortNumber = new JAPJIntField(ListenerInterface.PORT_MAX_VALUE);
		//m_tfProxyPortNumber.setFont(m_fontControls);
		ProxyInterface proxyInterface = JAPModel.getInstance().getProxyInterface();
		boolean bUseProxy = (proxyInterface != null && proxyInterface.isValid());
		m_tfProxyHost.setEnabled(bUseProxy);
		m_tfProxyPortNumber.setEnabled(bUseProxy);
		m_cbProxy.addItemListener(new ItemListener()
		{
			public void itemStateChanged(ItemEvent e)
			{
				boolean b = m_cbProxy.isSelected();
				m_comboProxyType.setEnabled(b);
				m_tfProxyHost.setEnabled(b);
				m_tfProxyPortNumber.setEnabled(b);
				m_cbProxyAuthentication.setEnabled(b);
				m_labelProxyHost.setEnabled(b);
				m_labelProxyPort.setEnabled(b);
				m_labelProxyType.setEnabled(b);
				m_labelProxyAuthUserID.setEnabled(m_cbProxyAuthentication.isSelected() & b);
				m_tfProxyAuthenticationUserID.setEnabled(m_cbProxyAuthentication.isSelected() & b);
			}

		});
			/*
		m_tfProxyHost.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				okPressed(false);
			}
		});*/
	 /*
		m_tfProxyPortNumber.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				okPressed(false);
			}
		});*/
		m_cbProxyAuthentication = new JCheckBox(JAPMessages.getString("settingsProxyAuthenticationCheckBox"));
		//m_cbProxyAuthentication.setFont(m_fontControls);
		m_tfProxyAuthenticationUserID = new JTextField(10);
		//m_tfProxyAuthenticationUserID.setFont(m_fontControls);
		m_cbProxyAuthentication.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				m_tfProxyAuthenticationUserID.setEnabled(m_cbProxyAuthentication.isSelected());
				m_labelProxyAuthUserID.setEnabled(m_cbProxyAuthentication.isSelected());
			}
		});
		m_labelProxyHost = new JLabel(JAPMessages.getString("settingsProxyHost"));
		//m_labelProxyHost.setFont(m_fontControls);
		m_labelProxyPort = new JLabel(JAPMessages.getString("settingsProxyPort"));
		//m_labelProxyPort.setFont(m_fontControls);
		m_labelProxyType = new JLabel(JAPMessages.getString("settingsProxyType"));
		//m_labelProxyType.setFont(m_fontControls);
		m_labelProxyAuthUserID = new JLabel(JAPMessages.getString("settingsProxyAuthUserID"));
		//m_labelProxyAuthUserID.setFont(m_fontControls);
		// set Font in m_cbProxy in same color as in proxyPortLabel
		m_cbProxy.setForeground(m_labelProxyPort.getForeground());
		m_cbProxyAuthentication.setForeground(m_labelProxyPort.getForeground());

		JPanel p = new JPanel();
		p.setLayout(new BorderLayout());
		TitledBorder border = new TitledBorder(JAPMessages.getString("settingsProxyBorder"));
		//border.setTitleFont(m_fontControls);
		p.setBorder(border);
		JPanel p1 = new JPanel();
		GridBagLayout g = new GridBagLayout();
		p1.setLayout(g);
		if (JAPModel.isSmallDisplay())
		{
			p1.setBorder(new EmptyBorder(1, 10, 1, 10));
		}
		else
		{
			p1.setBorder(new EmptyBorder(5, 10, 10, 10));
		}
		GridBagConstraints c = new GridBagConstraints();
		c.anchor = GridBagConstraints.NORTHWEST;
		c.fill = GridBagConstraints.NONE;
		c.gridwidth = 1;
		c.gridx = 0;
		c.gridy = 0;
		c.weightx = 1;
		c.weighty = 0;
		Insets normInsets;
		if (JAPModel.isSmallDisplay())
		{
			normInsets = new Insets(0, 0, 1, 0);
		}
		else
		{
			normInsets = new Insets(0, 0, 3, 0);
		}
		c.insets = normInsets;
		g.setConstraints(m_cbProxy, c);
		p1.add(m_cbProxy);
		c.gridy = 1;
		g.setConstraints(m_labelProxyType, c);
		c.gridy = 2;
		p1.add(m_labelProxyType);
		g.setConstraints(m_comboProxyType, c);
		c.gridy = 3;
		p1.add(m_comboProxyType);
		g.setConstraints(m_labelProxyHost, c);
		p1.add(m_labelProxyHost);
		c.gridy = 4;
		g.setConstraints(m_tfProxyHost, c);
		p1.add(m_tfProxyHost);
		c.gridy = 5;
		g.setConstraints(m_labelProxyPort, c);
		p1.add(m_labelProxyPort);
		c.gridy = 6;
		g.setConstraints(m_tfProxyPortNumber, c);
		p1.add(m_tfProxyPortNumber);
		JSeparator seperator = new JSeparator();
		c.gridy = 7;
		if (JAPModel.isSmallDisplay())
		{
			c.insets = new Insets(5, 0, 1, 0);
		}
		else
		{
			c.insets = new Insets(10, 0, 3, 0);
		}
		g.setConstraints(seperator, c);
		p1.add(seperator);
		c.insets = normInsets;
		c.gridy = 8;
		//c.insets=new Insets(10,0,0,0);
		g.setConstraints(m_cbProxyAuthentication, c);
		p1.add(m_cbProxyAuthentication);
		c.gridy = 9;
		g.setConstraints(m_labelProxyAuthUserID, c);
		p1.add(m_labelProxyAuthUserID);
		c.gridy = 10;
		g.setConstraints(m_tfProxyAuthenticationUserID, c);
		p1.add(m_tfProxyAuthenticationUserID);
		c.gridy = 11;
		p.add(p1, BorderLayout.NORTH);


		return p;
	}


	public String getHelpContext()
	{
		return "net";
	}

}
