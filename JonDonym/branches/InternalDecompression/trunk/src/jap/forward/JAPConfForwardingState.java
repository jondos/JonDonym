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
package jap.forward;

import java.text.NumberFormat;
import java.util.Observable;
import java.util.Observer;
import java.util.Vector;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.border.TitledBorder;


import anon.transport.address.AddressParameter;
import anon.transport.address.IAddress;
import anon.util.JAPMessages;
import gui.JAPHtmlMultiLineLabel;
import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;

import jap.*;
import gui.*;

/**
 * This is the configuration GUI for the JAP forwarding state component.
 */
public class JAPConfForwardingState extends AbstractJAPConfModule
{

	/**
	 * This is the internal message system of this module.
	 */
	private MessageSystem m_messageSystem;

	/**
	 * Constructor for JAPConfForwardingState. We do some initialization here.
	 */
	public JAPConfForwardingState()
	{
		/* we change nothing -> we don't need a savepoint */
		super(null);
	}

	/**
	 * @todo write help
	 * @return String
	 */
	public String getHelpContext()
	{
		return null;
	}

	/**
	 * Creates the forwarding state root panel with all child components.
	 */
	public void recreateRootPanel()
	{
		synchronized (this)
		{
			if (m_messageSystem == null)
			{
				/* create a new object for sending internal messages */
				m_messageSystem = new MessageSystem();
			}
		}

		JPanel rootPanel = getRootPanel();

		synchronized (this)
		{
			/* clear the whole root panel */
			rootPanel.removeAll();
			/* notify the observers of the message system that we recreate the root panel */
			m_messageSystem.sendMessage();
			/* recreate all parts of the forwarding state dialog */
			JPanel statePanel = createForwardingStatePanel();

			GridBagLayout rootPanelLayout = new GridBagLayout();
			rootPanel.setLayout(rootPanelLayout);

			GridBagConstraints rootPanelConstraints = new GridBagConstraints();
			rootPanelConstraints.anchor = GridBagConstraints.NORTHWEST;
			rootPanelConstraints.fill = GridBagConstraints.BOTH;
			rootPanelConstraints.weightx = 1.0;
			rootPanelConstraints.weighty = 1.0;

			rootPanelConstraints.gridx = 0;
			rootPanelConstraints.gridy = 0;
			rootPanelLayout.setConstraints(statePanel, rootPanelConstraints);
			rootPanel.add(statePanel);
		}
	}

	/**
	 * Returns the title for the forwarding state component within the configuration tree.
	 *
	 * @return The title for the forwarding state leaf within the tree.
	 */
	public String getTabTitle()
	{
		return JAPMessages.getString("confTreeForwardingStateLeaf");
	}

	/**
	 * Creates the forwarding server state panel. This panel should only be visible, when the
	 * forwarding server is running.
	 *
	 * @return The state panel which is shown, if the forwarding server is running.
	 */
	private JPanel createForwardingServerStatePanel()
	{
		/* get the NumberFormat instance for formating the bandwidth (double) */
		final NumberFormat bandwidthFormat = NumberFormat.getInstance();
		bandwidthFormat.setMinimumFractionDigits(1);
		bandwidthFormat.setMaximumFractionDigits(1);
		bandwidthFormat.setMinimumIntegerDigits(1);

		/* get the NumberFormat instance for formating the other numbers (integer) */
		final NumberFormat integerFormat = NumberFormat.getInstance();
		bandwidthFormat.setMinimumIntegerDigits(1);

		final JAPHtmlMultiLineLabel settingsRoutingServerStatusLabel = new JAPHtmlMultiLineLabel("");
		final JLabel settingsRoutingServerStatusRegistrationErrorLabel = new JLabel();
		//settingsRoutingServerStatusRegistrationErrorLabel.setFont(getFontSetting());

		final JLabel settingsRoutingServerStatusStatisticsBandwidthLabel = new JLabel();
		//settingsRoutingServerStatusStatisticsBandwidthLabel.setFont(getFontSetting());
		final JLabel settingsRoutingServerStatusStatisticsForwardedBytesLabel = new JLabel();
		//settingsRoutingServerStatusStatisticsForwardedBytesLabel.setFont(getFontSetting());
		JLabel settingsRoutingServerStatusStatisticsConnectionsLabel = new JLabel(JAPMessages.getString(
			"settingsRoutingServerStatusStatisticsConnectionsLabel"));
		//settingsRoutingServerStatusStatisticsConnectionsLabel.setFont(getFontSetting());
		final JLabel settingsRoutingServerStatusStatisticsCurrentConnectionsLabel = new JLabel();
		//settingsRoutingServerStatusStatisticsCurrentConnectionsLabel.setFont(getFontSetting());
		final JLabel settingsRoutingServerStatusStatisticsAcceptedConnectionsLabel = new JLabel();
		//settingsRoutingServerStatusStatisticsAcceptedConnectionsLabel.setFont(getFontSetting());
		final JLabel settingsRoutingServerStatusStatisticsRejectedConnectionsLabel = new JLabel();
		//settingsRoutingServerStatusStatisticsRejectedConnectionsLabel.setFont(getFontSetting());

		JLabel settingsRoutingServerStatusInfoServiceRegistrationsLabel = new JLabel(JAPMessages.getString(
			"settingsRoutingServerStatusInfoServiceRegistrationsLabel"));
		//settingsRoutingServerStatusInfoServiceRegistrationsLabel.setFont(getFontSetting());

		final JAPRoutingInfoServiceRegistrationTableModel infoServiceRegistrationTableModel = new
			JAPRoutingInfoServiceRegistrationTableModel();
		JTable infoServiceRegistrationTable = new JTable(infoServiceRegistrationTableModel);
		//infoServiceRegistrationTable.setFont(getFontSetting());
		infoServiceRegistrationTable.getColumnModel().getColumn(1).setMaxWidth(125);
		infoServiceRegistrationTable.getColumnModel().getColumn(1).setPreferredWidth(125);
		infoServiceRegistrationTable.setEnabled(false);
		//infoServiceRegistrationTable.getTableHeader().setFont(getFontSetting());
		infoServiceRegistrationTable.getTableHeader().setResizingAllowed(false);
		infoServiceRegistrationTable.getTableHeader().setReorderingAllowed(false);
		JScrollPane infoServiceRegistrationTableScrollPane = new JScrollPane(infoServiceRegistrationTable);
		infoServiceRegistrationTableScrollPane.setPreferredSize(new Dimension(
			infoServiceRegistrationTableScrollPane.getPreferredSize().width, 50));

		Observer serverStatusObserver = new Observer()
		{
			/**
			 * This is the observer implementation. If there are new server statistics available, new
			 * propganda instances started or the server status has been changed, the components on the
			 * server status panel are updated. If the panel is recreated (message via the module
			 * internal message system), the observer removes itself from all observed objects.
			 *
			 * @param a_notifier The observed Object. This should always be
			 *                   JAPRoutingServerStatisticsListener, JAPRoutingSettings or the module
			 *                   internal message system at the moment.
			 * @param a_message The reason of the notification. This should always be a JAPRoutingMessage
			 *                  or null at the moment.
			 */
			public void update(Observable a_notifier, Object a_message)
			{
				try
				{
					if (a_notifier == JAPModel.getInstance().getRoutingSettings().getServerStatisticsListener())
					{
						if ( ( (JAPRoutingMessage) (a_message)).getMessageCode() ==
							JAPRoutingMessage.SERVER_STATISTICS_UPDATED)
						{
							/* statistics might have been changed */
							settingsRoutingServerStatusStatisticsBandwidthLabel.setText(JAPMessages.getString(
								"settingsRoutingServerStatusStatisticsBandwidthLabelPart1") + " " +
								bandwidthFormat.format( ( (double) (JAPModel.getInstance().getRoutingSettings().
								getServerStatisticsListener().getCurrentBandwidthUsage())) / (double) 1024) +
								" " +
								JAPMessages.getString("settingsRoutingServerStatusStatisticsBandwidthLabelPart2"));
							settingsRoutingServerStatusStatisticsForwardedBytesLabel.setText(JAPMessages.
								getString("settingsRoutingServerStatusStatisticsForwardedBytesLabel") + " " +
								integerFormat.format(JAPModel.getInstance().getRoutingSettings().
								getServerStatisticsListener().getTransferedBytes()));
							settingsRoutingServerStatusStatisticsCurrentConnectionsLabel.setText(JAPMessages.
								getString("settingsRoutingServerStatusStatisticsCurrentConnectionsLabel") +
								" " +
								integerFormat.format(JAPModel.getInstance().getRoutingSettings().getServerStatisticsListener().
								getCurrentlyForwardedConnections()));
							settingsRoutingServerStatusStatisticsAcceptedConnectionsLabel.setText(JAPMessages.
								getString("settingsRoutingServerStatusStatisticsAcceptedConnectionsLabel") +
								" " +
								integerFormat.format(JAPModel.getInstance().getRoutingSettings().getServerStatisticsListener().
								getAcceptedConnections()));
							settingsRoutingServerStatusStatisticsRejectedConnectionsLabel.setText(JAPMessages.
								getString("settingsRoutingServerStatusStatisticsRejectedConnectionsLabel") +
								" " +
								integerFormat.format(JAPModel.getInstance().getRoutingSettings().getServerStatisticsListener().
								getRejectedConnections()));
						}
					}
					if (a_notifier ==
						JAPModel.getInstance().getRoutingSettings().getRegistrationStatusObserver())
					{
						if ( ( (JAPRoutingMessage) (a_message)).getMessageCode() ==
							JAPRoutingMessage.REGISTRATION_STATUS_CHANGED)
						{
							/* update the server state label and the reason of error, if necessary */
							int currentRegistrationState = JAPModel.getInstance().getRoutingSettings().
								getRegistrationStatusObserver().getCurrentState();
							int currentErrorCode = JAPModel.getInstance().getRoutingSettings().
								getRegistrationStatusObserver().getCurrentErrorCode();
							if (currentRegistrationState ==
								JAPRoutingRegistrationStatusObserver.STATE_DISABLED)
							{
								settingsRoutingServerStatusLabel.setText(
						JAPMessages.getString("settingsRoutingServerStatusLabelStateRegistrationDisabled"));
							}
							else if (currentRegistrationState ==
									 JAPRoutingRegistrationStatusObserver.STATE_INITIAL_REGISTRATION)
							{
								settingsRoutingServerStatusLabel.setText(
						JAPMessages.getString("settingsRoutingServerStatusLabelStateRegistrationInitiated"));
							}
							else if (currentRegistrationState ==
									 JAPRoutingRegistrationStatusObserver.STATE_NO_REGISTRATION)
							{
								settingsRoutingServerStatusLabel.setText(
							JAPMessages.getString("settingsRoutingServerStatusLabelStateRegistrationFailed"));
							}
							else if (currentRegistrationState ==
									 JAPRoutingRegistrationStatusObserver.STATE_SUCCESSFUL_REGISTRATION)
							{
								settingsRoutingServerStatusLabel.setText(
						JAPMessages.getString("settingsRoutingServerStatusLabelStateRegistrationSuccessful"));
							}
							if (currentErrorCode == JAPRoutingRegistrationStatusObserver.ERROR_NO_ERROR)
							{
								settingsRoutingServerStatusRegistrationErrorLabel.setText(" ");
							}
							else if (currentErrorCode ==
									 JAPRoutingRegistrationStatusObserver.ERROR_NO_KNOWN_PRIMARY_INFOSERVICES)
							{
								settingsRoutingServerStatusRegistrationErrorLabel.setText(
			JAPMessages.getString("settingsRoutingServerStatusRegistrationErrorLabelNoKnownInfoServices"));
							}
							else if (currentErrorCode ==
									 JAPRoutingRegistrationStatusObserver.ERROR_INFOSERVICE_CONNECT_ERROR)
							{
								settingsRoutingServerStatusRegistrationErrorLabel.setText(
									JAPMessages.getString("settingsRoutingServerStatusRegistrationErrorLabelConnectionFailed"));
							}
							else if (currentErrorCode ==
									 JAPRoutingRegistrationStatusObserver.ERROR_VERIFICATION_ERROR)
							{
								settingsRoutingServerStatusRegistrationErrorLabel.setText(
									JAPMessages.getString("settingsRoutingServerStatusRegistrationErrorLabelVerificationFailed"));
							}
							else if (currentErrorCode ==
									 JAPRoutingRegistrationStatusObserver.ERROR_UNKNOWN_ERROR)
							{
								settingsRoutingServerStatusRegistrationErrorLabel.setText(
									JAPMessages.getString("settingsRoutingServerStatusRegistrationErrorLabelUnknownReason"));
							}
						}
					}
					if (a_notifier == JAPModel.getInstance().getRoutingSettings())
					{
						if ( ( (JAPRoutingMessage) (a_message)).getMessageCode() ==
							JAPRoutingMessage.PROPAGANDA_INSTANCES_ADDED)
						{
							/* update the propagandists in the infoservice registration table */
							infoServiceRegistrationTableModel.updatePropagandaInstancesList( (Vector) ( ( (
								JAPRoutingMessage) a_message).getMessageData()));
						}
					}
					if (a_notifier == m_messageSystem)
					{
						/* the root panel was recreated -> stop observing and remove ourself from the observed
						 * objects
						 */
						JAPModel.getInstance().getRoutingSettings().getServerStatisticsListener().
							deleteObserver(this);
						JAPModel.getInstance().getRoutingSettings().deleteObserver(this);
						m_messageSystem.deleteObserver(this);
						/* also stop observing of the propaganda instances from the
						 * InfoServiceRegistrationTableModel
						 */
						infoServiceRegistrationTableModel.clearPropagandaInstancesTable();
					}
				}
				catch (Exception e)
				{
					/* should not happen */
					LogHolder.log(LogLevel.EXCEPTION, LogType.GUI, e);
				}
			}
		};
		/* registrate the observer also at the internal message system */
		m_messageSystem.addObserver(serverStatusObserver);
		JAPModel.getInstance().getRoutingSettings().getServerStatisticsListener().addObserver(
			serverStatusObserver);
		JAPModel.getInstance().getRoutingSettings().getRegistrationStatusObserver().addObserver(
			serverStatusObserver);
		JAPModel.getInstance().getRoutingSettings().addObserver(serverStatusObserver);
		/* tricky: initialize the labels by calling the observer (with all possible messages) */
		serverStatusObserver.update(JAPModel.getInstance().getRoutingSettings().getServerStatisticsListener(),
									new JAPRoutingMessage(JAPRoutingMessage.SERVER_STATISTICS_UPDATED));
		serverStatusObserver.update(JAPModel.getInstance().getRoutingSettings().getRegistrationStatusObserver(),
									new JAPRoutingMessage(JAPRoutingMessage.REGISTRATION_STATUS_CHANGED));
		/* initialize the propaganda instances table */
		infoServiceRegistrationTableModel.updatePropagandaInstancesList(JAPModel.getInstance().
			getRoutingSettings().getRunningPropagandaInstances());

		JPanel serverStatusPanel = new JPanel();

		TitledBorder settingsRoutingServerStatusBorder = new TitledBorder(
			  JAPMessages.getString("settingsRoutingServerStatusBorder"));
		//settingsRoutingServerStatusBorder.setTitleFont(getFontSetting());
		serverStatusPanel.setBorder(settingsRoutingServerStatusBorder);

		GridBagLayout serverStatusPanelLayout = new GridBagLayout();
		serverStatusPanel.setLayout(serverStatusPanelLayout);

		GridBagConstraints serverStatusPanelConstraints = new GridBagConstraints();
		serverStatusPanelConstraints.anchor = GridBagConstraints.NORTHWEST;
		serverStatusPanelConstraints.fill = GridBagConstraints.NONE;
		serverStatusPanelConstraints.weightx = 1.0;
		serverStatusPanelConstraints.weighty = 0.0;
		serverStatusPanelConstraints.gridx = 0;
		serverStatusPanelConstraints.gridy = 0;
		serverStatusPanelConstraints.insets = new Insets(5, 5, 5, 5);
		serverStatusPanelLayout.setConstraints(settingsRoutingServerStatusLabel, serverStatusPanelConstraints);
		serverStatusPanel.add(settingsRoutingServerStatusLabel);

		serverStatusPanelConstraints.gridx = 0;
		serverStatusPanelConstraints.gridy = 1;
		serverStatusPanelConstraints.insets = new Insets(0, 5, 20, 5);
		serverStatusPanelLayout.setConstraints(settingsRoutingServerStatusRegistrationErrorLabel,
											   serverStatusPanelConstraints);
		serverStatusPanel.add(settingsRoutingServerStatusRegistrationErrorLabel);

		serverStatusPanelConstraints.gridx = 0;
		serverStatusPanelConstraints.gridy = 2;
		serverStatusPanelConstraints.insets = new Insets(0, 5, 0, 5);
		serverStatusPanelLayout.setConstraints(settingsRoutingServerStatusInfoServiceRegistrationsLabel,
											   serverStatusPanelConstraints);
		serverStatusPanel.add(settingsRoutingServerStatusInfoServiceRegistrationsLabel);

		serverStatusPanelConstraints.fill = GridBagConstraints.BOTH;
		serverStatusPanelConstraints.weighty = 1.0;
		serverStatusPanelConstraints.gridx = 0;
		serverStatusPanelConstraints.gridy = 3;
		serverStatusPanelConstraints.insets = new Insets(0, 5, 5, 5);
		serverStatusPanelLayout.setConstraints(infoServiceRegistrationTableScrollPane,
											   serverStatusPanelConstraints);
		serverStatusPanel.add(infoServiceRegistrationTableScrollPane);

		JPanel serverStatusStatisticsPanel = new JPanel();

		TitledBorder settingsRoutingServerStatusStatisticsBorder = new TitledBorder(
			  JAPMessages.getString("settingsRoutingServerStatusStatisticsBorder"));
		//settingsRoutingServerStatusStatisticsBorder.setTitleFont(getFontSetting());
		serverStatusStatisticsPanel.setBorder(settingsRoutingServerStatusStatisticsBorder);

		GridBagLayout serverStatusStatisticsPanelLayout = new GridBagLayout();
		serverStatusStatisticsPanel.setLayout(serverStatusStatisticsPanelLayout);

		GridBagConstraints serverStatusStatisticsPanelConstraints = new GridBagConstraints();
		serverStatusStatisticsPanelConstraints.anchor = GridBagConstraints.NORTHWEST;
		serverStatusStatisticsPanelConstraints.fill = GridBagConstraints.NONE;
		serverStatusStatisticsPanelConstraints.weightx = 1.0;
		serverStatusStatisticsPanelConstraints.weighty = 0.0;
		serverStatusStatisticsPanelConstraints.gridx = 0;
		serverStatusStatisticsPanelConstraints.gridy = 0;
		serverStatusStatisticsPanelConstraints.gridwidth = 4;
		serverStatusStatisticsPanelConstraints.insets = new Insets(5, 5, 10, 5);
		serverStatusStatisticsPanelLayout.setConstraints(settingsRoutingServerStatusStatisticsBandwidthLabel,
			serverStatusStatisticsPanelConstraints);
		serverStatusStatisticsPanel.add(settingsRoutingServerStatusStatisticsBandwidthLabel);

		serverStatusStatisticsPanelConstraints.gridx = 0;
		serverStatusStatisticsPanelConstraints.gridy = 1;
		serverStatusStatisticsPanelConstraints.insets = new Insets(0, 5, 10, 5);
		serverStatusStatisticsPanelLayout.setConstraints(
			settingsRoutingServerStatusStatisticsForwardedBytesLabel, serverStatusStatisticsPanelConstraints);
		serverStatusStatisticsPanel.add(settingsRoutingServerStatusStatisticsForwardedBytesLabel);

		serverStatusStatisticsPanelConstraints.gridx = 0;
		serverStatusStatisticsPanelConstraints.gridy = 2;
		serverStatusStatisticsPanelConstraints.weighty = 1.0;
		serverStatusStatisticsPanelConstraints.weightx = 0.0;
		serverStatusStatisticsPanelConstraints.gridwidth = 1;
		serverStatusStatisticsPanelConstraints.insets = new Insets(0, 5, 5, 15);
		serverStatusStatisticsPanelLayout.setConstraints(
			settingsRoutingServerStatusStatisticsConnectionsLabel, serverStatusStatisticsPanelConstraints);
		serverStatusStatisticsPanel.add(settingsRoutingServerStatusStatisticsConnectionsLabel);

		serverStatusStatisticsPanelConstraints.gridx = 1;
		serverStatusStatisticsPanelConstraints.gridy = 2;
		serverStatusStatisticsPanelConstraints.insets = new Insets(0, 0, 5, 15);
		serverStatusStatisticsPanelLayout.setConstraints(
			settingsRoutingServerStatusStatisticsCurrentConnectionsLabel,
			serverStatusStatisticsPanelConstraints);
		serverStatusStatisticsPanel.add(settingsRoutingServerStatusStatisticsCurrentConnectionsLabel);

		serverStatusStatisticsPanelConstraints.gridx = 2;
		serverStatusStatisticsPanelConstraints.gridy = 2;
		serverStatusStatisticsPanelConstraints.insets = new Insets(0, 0, 5, 15);
		serverStatusStatisticsPanelLayout.setConstraints(
			settingsRoutingServerStatusStatisticsAcceptedConnectionsLabel,
			serverStatusStatisticsPanelConstraints);
		serverStatusStatisticsPanel.add(settingsRoutingServerStatusStatisticsAcceptedConnectionsLabel);

		serverStatusStatisticsPanelConstraints.gridx = 3;
		serverStatusStatisticsPanelConstraints.gridy = 2;
		serverStatusStatisticsPanelConstraints.weightx = 1.0;
		serverStatusStatisticsPanelConstraints.insets = new Insets(0, 0, 5, 5);
		serverStatusStatisticsPanelLayout.setConstraints(
			settingsRoutingServerStatusStatisticsRejectedConnectionsLabel,
			serverStatusStatisticsPanelConstraints);
		serverStatusStatisticsPanel.add(settingsRoutingServerStatusStatisticsRejectedConnectionsLabel);

		JPanel serverStatusAllPanel = new JPanel();

		GridBagLayout serverStatusAllPanelLayout = new GridBagLayout();
		serverStatusAllPanel.setLayout(serverStatusAllPanelLayout);

		GridBagConstraints serverStatusAllPanelConstraints = new GridBagConstraints();
		serverStatusAllPanelConstraints.anchor = GridBagConstraints.NORTHWEST;
		serverStatusAllPanelConstraints.fill = GridBagConstraints.BOTH;
		serverStatusAllPanelConstraints.weightx = 1.0;
		serverStatusAllPanelConstraints.weighty = 1.0;
		serverStatusAllPanelConstraints.gridx = 0;
		serverStatusAllPanelConstraints.gridy = 0;
		serverStatusAllPanelLayout.setConstraints(serverStatusPanel, serverStatusAllPanelConstraints);
		serverStatusAllPanel.add(serverStatusPanel);

		serverStatusAllPanelConstraints.weighty = 0.0;
		serverStatusAllPanelConstraints.gridx = 0;
		serverStatusAllPanelConstraints.gridy = 1;
		serverStatusAllPanelLayout.setConstraints(serverStatusStatisticsPanel,
												  serverStatusAllPanelConstraints);
		serverStatusAllPanel.add(serverStatusStatisticsPanel);

		return serverStatusAllPanel;
	}

	/**
	 * Creates the routing client state panel. This panel should only be visible, when the
	 * forwarding client is running.
	 *
	 * @return The state panel which is shown, if the forwarding client is running.
	 */
	private JPanel createForwardingClientStatePanel()
	{
		JPanel clientStatusPanel = new JPanel();

		JLabel settingsRoutingClientStatusClientRunningLabel = new JLabel(
			  JAPMessages.getString("settingsRoutingClientStatusClientRunningLabel"));
		//settingsRoutingClientStatusClientRunningLabel.setFont(getFontSetting());
		JLabel settingsRoutingClientStatusConnectedViaLabel = new JLabel(
			  JAPMessages.getString("settingsRoutingClientStatusConnectedViaLabel"));
		//settingsRoutingClientStatusConnectedViaLabel.setFont(getFontSetting());
		final JLabel settingsRoutingClientStatusForwarderInformationLabel = new JLabel();
		//settingsRoutingClientStatusForwarderInformationLabel.setFont(getFontSetting());

		Observer clientStatusObserver = new Observer()
		{
			/**
			 * This is the observer implementation. If we get connected via a new forwarding server, the
			 * client status with the information about the forwarder is updated. If the panel is
			 * recreated (message via the module internal message system), the observer removes itself
			 * from all observed objects.
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
							JAPRoutingMessage.ROUTING_MODE_CHANGED)
						{
							if (JAPModel.getInstance().getRoutingSettings().getRoutingMode() ==
								JAPRoutingSettings.ROUTING_MODE_CLIENT)
							{
								/* we are connected to a new client -> update the forwarder information label */
								IAddress currentForwarderAddress = JAPModel.getInstance().
									getRoutingSettings().getForwarderAddress();
								if (currentForwarderAddress != null)
								{
									AddressParameter[] paramters = currentForwarderAddress.getAllParameters();
									//TODO: check ob Laenge mindestens 2 und evtl. auf Reihenfolge achten
									// ideal extra Texte fuer unterschiedliche Addresstypen.
									settingsRoutingClientStatusForwarderInformationLabel.setText(JAPMessages.
										getString("settingsRoutingClientStatusForwarderInformationLabelPart1") +
										" " + paramters[0].getValue() + "    " +
										JAPMessages.getString("settingsRoutingClientStatusForwarderInformationLabelPart2") +
										" " + paramters[1].getValue());
								}
								else
								{
									/* should never occur */
									settingsRoutingClientStatusForwarderInformationLabel.setText(
										JAPMessages.getString("settingsRoutingClientStatusForwarderInformationLabelInvalid"));
								}
							}
						}
					}
					if (a_notifier == m_messageSystem)
					{
						/* the root panel was recreated -> stop observing and remove ourself from the observed
						 * objects
						 */
						JAPModel.getInstance().getRoutingSettings().deleteObserver(this);
						m_messageSystem.deleteObserver(this);
					}
				}
				catch (Exception e)
				{
					/* should not happen */
					LogHolder.log(LogLevel.EXCEPTION, LogType.GUI, e);
				}
			}
		};
		/* registrate the observer also at the internal message system */
		m_messageSystem.addObserver(clientStatusObserver);
		JAPModel.getInstance().getRoutingSettings().addObserver(clientStatusObserver);
		/* tricky: initialize the label (if necessary) by calling the observer */
		clientStatusObserver.update(JAPModel.getInstance().getRoutingSettings(),
									new JAPRoutingMessage(JAPRoutingMessage.ROUTING_MODE_CHANGED));

		TitledBorder settingsRoutingClientStatusBorder = new TitledBorder(
			  JAPMessages.getString("settingsRoutingClientStatusBorder"));
		//settingsRoutingClientStatusBorder.setTitleFont(getFontSetting());
		clientStatusPanel.setBorder(settingsRoutingClientStatusBorder);

		GridBagLayout clientStatusPanelLayout = new GridBagLayout();
		clientStatusPanel.setLayout(clientStatusPanelLayout);

		GridBagConstraints clientStatusPanelConstraints = new GridBagConstraints();
		clientStatusPanelConstraints.anchor = GridBagConstraints.NORTHWEST;
		clientStatusPanelConstraints.fill = GridBagConstraints.NONE;
		clientStatusPanelConstraints.weightx = 1.0;
		clientStatusPanelConstraints.weighty = 0.0;
		clientStatusPanelConstraints.gridx = 0;
		clientStatusPanelConstraints.gridy = 0;
		clientStatusPanelConstraints.insets = new Insets(5, 5, 10, 5);
		clientStatusPanelLayout.setConstraints(settingsRoutingClientStatusClientRunningLabel,
											   clientStatusPanelConstraints);
		clientStatusPanel.add(settingsRoutingClientStatusClientRunningLabel);

		clientStatusPanelConstraints.gridx = 0;
		clientStatusPanelConstraints.gridy = 1;
		clientStatusPanelConstraints.insets = new Insets(0, 5, 2, 5);
		clientStatusPanelLayout.setConstraints(settingsRoutingClientStatusConnectedViaLabel,
											   clientStatusPanelConstraints);
		clientStatusPanel.add(settingsRoutingClientStatusConnectedViaLabel);

		clientStatusPanelConstraints.gridx = 0;
		clientStatusPanelConstraints.gridy = 2;
		clientStatusPanelConstraints.weighty = 1.0;
		clientStatusPanelConstraints.insets = new Insets(0, 15, 5, 5);
		clientStatusPanelLayout.setConstraints(settingsRoutingClientStatusForwarderInformationLabel,
											   clientStatusPanelConstraints);
		clientStatusPanel.add(settingsRoutingClientStatusForwarderInformationLabel);

		return clientStatusPanel;
	}

	/**
	 * Creates the state panel, for the case of a disabled forwarding server and client. This panel
	 * should only be visible, if we are in ROUTING_MODE_DISABLED.
	 *
	 * @return The state panel which is shown, if neither the forwarding client nor the forwarding
	 *         server is running.
	 */
	private JPanel createForwardingDisabledStatePanel()
	{
		JPanel disabledStatusPanel = new JPanel();

		JLabel settingsRoutingDisabledStatusNothingRunningLabel = new JLabel(
			  JAPMessages.getString("settingsRoutingDisabledStatusNothingRunningLabel"));
		//settingsRoutingDisabledStatusNothingRunningLabel.setFont(getFontSetting());

		TitledBorder settingsRoutingDisabledStatusBorder = new TitledBorder(
			  JAPMessages.getString("settingsRoutingDisabledStatusBorder"));
		//settingsRoutingDisabledStatusBorder.setTitleFont(getFontSetting());
		disabledStatusPanel.setBorder(settingsRoutingDisabledStatusBorder);

		GridBagLayout disabledStatusPanelLayout = new GridBagLayout();
		disabledStatusPanel.setLayout(disabledStatusPanelLayout);

		GridBagConstraints disabledStatusPanelConstraints = new GridBagConstraints();
		disabledStatusPanelConstraints.anchor = GridBagConstraints.NORTHWEST;
		disabledStatusPanelConstraints.fill = GridBagConstraints.NONE;
		disabledStatusPanelConstraints.weightx = 1.0;
		disabledStatusPanelConstraints.weighty = 1.0;
		disabledStatusPanelConstraints.gridx = 0;
		disabledStatusPanelConstraints.gridy = 0;
		disabledStatusPanelConstraints.insets = new Insets(5, 5, 5, 5);
		disabledStatusPanelLayout.setConstraints(settingsRoutingDisabledStatusNothingRunningLabel,
												 disabledStatusPanelConstraints);
		disabledStatusPanel.add(settingsRoutingDisabledStatusNothingRunningLabel);

		return disabledStatusPanel;
	}

	/**
	 * Creates the forwarding state panel, which switches between the server state panel, the client
	 * state panel and the forwarding-disabled state panel, if the forwarding mode is changed.
	 *
	 * @return The forwarding state panel.
	 */
	private JPanel createForwardingStatePanel()
	{
		JPanel statusPanel = new JPanel();

		/* create all needed panels */
		final JPanel serverStatusPanel = createForwardingServerStatePanel();
		final JPanel clientStatusPanel = createForwardingClientStatePanel();
		final JPanel disabledStatusPanel = createForwardingDisabledStatePanel();

		GridBagLayout statusPanelLayout = new GridBagLayout();
		statusPanel.setLayout(statusPanelLayout);

		/* the panels shall be at the same position, but always only one of them is visible according
		 * to the routing mode
		 */
		GridBagConstraints statusPanelConstraints = new GridBagConstraints();
		statusPanelConstraints.anchor = GridBagConstraints.NORTHWEST;
		statusPanelConstraints.fill = GridBagConstraints.BOTH;
		statusPanelConstraints.weightx = 1.0;
		statusPanelConstraints.weighty = 1.0;
		statusPanelConstraints.gridx = 0;
		statusPanelConstraints.gridy = 0;
		statusPanelLayout.setConstraints(serverStatusPanel, statusPanelConstraints);
		statusPanel.add(serverStatusPanel);
		statusPanelLayout.setConstraints(clientStatusPanel, statusPanelConstraints);
		statusPanel.add(clientStatusPanel);
		statusPanelLayout.setConstraints(disabledStatusPanel, statusPanelConstraints);
		statusPanel.add(disabledStatusPanel);

		/* set the preferred size of the status panel, before the currently not needed panels are
		 * made invisible -> all panels are taken into account when calculating the peferred size
		 */
		statusPanel.setPreferredSize(statusPanel.getPreferredSize());

		Observer forwardingModeObserver = new Observer()
		{
			/**
			 * This is the observer implementation. If the routing mode is changed, we display the
			 * correct status panel for the new routing mode. If the panel is recreated (message via
			 * the module internal message system), the observer removes itself from all observed
			 * objects.
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
							JAPRoutingMessage.ROUTING_MODE_CHANGED)
						{
							int newRoutingMode = JAPModel.getInstance().getRoutingSettings().getRoutingMode();
							if (newRoutingMode == JAPRoutingSettings.ROUTING_MODE_CLIENT)
							{
								serverStatusPanel.setVisible(false);
								disabledStatusPanel.setVisible(false);
								clientStatusPanel.setVisible(true);
							}
							if (newRoutingMode == JAPRoutingSettings.ROUTING_MODE_SERVER)
							{
								clientStatusPanel.setVisible(false);
								disabledStatusPanel.setVisible(false);
								serverStatusPanel.setVisible(true);
							}
							if (newRoutingMode == JAPRoutingSettings.ROUTING_MODE_DISABLED)
							{
								serverStatusPanel.setVisible(false);
								clientStatusPanel.setVisible(false);
								disabledStatusPanel.setVisible(true);
							}
						}
					}
					if (a_notifier == m_messageSystem)
					{
						/* the root panel was recreated -> stop observing and remove ourself from the observed
						 * objects
						 */
						JAPModel.getInstance().getRoutingSettings().deleteObserver(this);
						m_messageSystem.deleteObserver(this);
					}
				}
				catch (Exception e)
				{
					/* should not happen */
					LogHolder.log(LogLevel.EXCEPTION, LogType.GUI, e);
				}
			}
		};
		/* registrate the observer also at the internal message system */
		m_messageSystem.addObserver(forwardingModeObserver);
		JAPModel.getInstance().getRoutingSettings().addObserver(forwardingModeObserver);
		/* tricky: initialize the panel by calling the observer */
		forwardingModeObserver.update(JAPModel.getInstance().getRoutingSettings(),
									  new JAPRoutingMessage(JAPRoutingMessage.ROUTING_MODE_CHANGED));

		return statusPanel;
	}

}
