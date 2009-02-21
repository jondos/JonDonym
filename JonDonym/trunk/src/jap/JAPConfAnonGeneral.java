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

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import javax.swing.JCheckBox;
import javax.swing.border.TitledBorder;
import javax.swing.JPanel;
import javax.swing.JComboBox;
import javax.swing.JSlider;
import javax.swing.SwingConstants;

import gui.JAPMessages;
import javax.swing.JLabel;
import java.util.Dictionary;
import java.util.Observable;
import java.util.Hashtable;

import anon.client.AnonClient;
import anon.infoservice.BlacklistedCascadeIDEntry;
import anon.client.DummyTrafficControlChannel;
import java.util.Observer;

public final class JAPConfAnonGeneral extends AbstractJAPConfModule implements Observer
{
	public static final String MSG_CONNECTION_TIMEOUT =
		JAPConfAnonGeneral.class.getName() + "_loginTimeout";

	public static final String MSG_DENY_NON_ANONYMOUS_SURFING = JAPConfAnonGeneral.class.getName() +
		"_denyNonAnonymousSurfing";
	public static final String MSG_ANONYMIZED_HTTP_HEADERS = JAPConfAnonGeneral.class.getName() +
		"_anonymizedHttpHeaders";
	private static final String MSG_AUTO_CHOOSE_CASCADES = JAPConfAnonGeneral.class.getName() +
		"_autoChooseCascades";
	private static final String MSG_RESTRICT_AUTO_CHOOSE = JAPConfAnonGeneral.class.getName() +
		"_RestrictAutoChoosing";
	private static final String MSG_DO_NOT_RESTRICT_AUTO_CHOOSE = JAPConfAnonGeneral.class.getName() +
		"_doNotRestrictAutoChoosing";
	private static final String MSG_RESTRICT_AUTO_CHOOSE_PAY = JAPConfAnonGeneral.class.getName() +
		"_restrictAutoChoosingPay";
	private static final String MSG_KNOWN_CASCADES = JAPConfAnonGeneral.class.getName() +
		"_knownCascades";
	private static final String MSG_ALLOWED_CASCADES = JAPConfAnonGeneral.class.getName() +
		"_allowedCascades";
	private static final String MSG_AUTO_CHOOSE_ON_START = JAPConfAnonGeneral.class.getName() +
		"_autoChooseOnStart";
	private static final String MSG_TITLE_ASSIGN_SERVICES = JAPConfAnonGeneral.class.getName() +
		"_titleAssignServices";
	private static final String MSG_EXPLAIN_ASSIGN_SERVICES = JAPConfAnonGeneral.class.getName() +
		"_explainAssignServices";
	private static final String MSG_EXPLAIN_ASSIGN_SERVICES_BETA = JAPConfAnonGeneral.class.getName() +
		"_explainAssignServicesBeta";
	private static final String MSG_SERVICE_HTTP = JAPConfAnonGeneral.class.getName() + "_serviceHttp";
	private static final String MSG_SERVICE_FTP = JAPConfAnonGeneral.class.getName() + "_serviceFtp";
	private static final String MSG_SERVICE_EMAIL = JAPConfAnonGeneral.class.getName() + "_serviceEMail";
	private static final String MSG_SERVICE_SOCKS = JAPConfAnonGeneral.class.getName() + "_serviceSocks";
	private static final String MSG_PASSIVE_FTP = JAPConfAnonGeneral.class.getName() + "_passiveFTP";
	private static final String MSG_TOOLTIP_SERVICE_DEACTIVATED = JAPConfAnonGeneral.class.getName() +
		"_tooltipServiceDeactivated";
	private static final String MSG_EVERY_SECONDS = JAPConfAnonGeneral.class.getName() + "_everySeconds";
	private static final String MSG_LBL_WHITELIST = JAPConfAnonGeneral.class.getName() + "_autoBlacklist";
	private static final String MSG_AUTO_CHOOSE_ON_STARTUP =
		JAPConfAnonGeneral.class.getName() + "_autoChooseOnStartup";


	private static final String IMG_ARROW_RIGHT = JAPConfAnonGeneral.class.getName() + "_arrowRight.gif";
	private static final String IMG_ARROW_LEFT = JAPConfAnonGeneral.class.getName() + "_arrowLeft.gif";

	private static final int DT_INTERVAL_STEPLENGTH = 2;
	private static final int DT_INTERVAL_STEPS =
		DummyTrafficControlChannel.DT_MAX_INTERVAL_MS / DT_INTERVAL_STEPLENGTH / 1000;
	private static final int DT_INTERVAL_DEFAULT = 10;
	private static final int DT_INTERVAL_MIN_STEP = 6;

	public static final int DEFAULT_DUMMY_TRAFFIC_INTERVAL_SECONDS =
		DT_INTERVAL_STEPLENGTH * DT_INTERVAL_DEFAULT * 1000;

	private static final Integer[] LOGIN_TIMEOUTS =
		new Integer[]{new Integer(5), new Integer(10), new Integer(15), new Integer(20), new Integer(25),
		new Integer(30), new Integer(40), new Integer(50), new Integer(60)};

	private JCheckBox m_cbDenyNonAnonymousSurfing;
	private JCheckBox m_cbAnonymizedHttpHeaders;
	//private JCheckBox m_cbDummyTraffic;
	private JCheckBox m_cbAutoConnect;
	private JCheckBox m_cbAutoReConnect;
	private JCheckBox m_cbAutoBlacklist;
	private JCheckBox m_cbAutoChooseCascades;
	private JCheckBox m_cbAutoChooseCascadesOnStartup;
	private JSlider m_sliderDummyTrafficIntervall;
	private JAPController m_Controller;
	//private JComboBox[] m_comboServices;
	private JComboBox m_comboTimeout;


	protected JAPConfAnonGeneral(IJAPConfSavePoint savePoint)
	{
		super(null);
		
		m_Controller = JAPController.getInstance();
	}
	
	protected boolean initObservers()
	{
		if (super.initObservers())
		{
			synchronized(LOCK_OBSERVABLE)
			{
				JAPModel.getInstance().addObserver(this);
				return true;
			}
		}
		return false;
	}

	public String getTabTitle()
	{
		return JAPMessages.getString("settingsInfoServiceConfigAdvancedSettingsTabTitle");
	}

	public void update(Observable a_notifier, Object a_message)
	{
		if (a_message != null)
		{
			if (a_message.equals(JAPModel.CHANGED_AUTO_RECONNECT))
			{
				m_cbAutoReConnect.setSelected(JAPModel.isAutomaticallyReconnected());
			}
			else if (a_message.equals(JAPModel.CHANGED_CASCADE_AUTO_CHANGE))
			{
				m_cbAutoChooseCascades.setSelected(
								JAPModel.getInstance().isCascadeAutoSwitched());
			}
			else if (a_message.equals(JAPModel.CHANGED_AUTO_CONNECT))
			{
				m_cbAutoConnect.setSelected(JAPModel.isAutoConnect());
			}
			else if (a_message.equals(JAPModel.CHANGED_ASK_FOR_NON_ANONYMOUS))
			{
				m_cbDenyNonAnonymousSurfing.setSelected(JAPModel.getInstance().isAskForAnyNonAnonymousRequest());
			}
			else if(a_message.equals(JAPModel.CHANGED_ANONYMIZED_HTTP_HEADERS))
			{
				m_cbAnonymizedHttpHeaders.setSelected(JAPModel.getInstance().isAnonymizedHttpHeaders());
			}
		}
	}

	protected void onUpdateValues()
	{
		int iTmp = JAPModel.getDummyTraffic();
		//m_cbDummyTraffic.setSelected(iTmp > -1);
		if (iTmp > -1)
		{
			int seconds = iTmp / 1000;
			if (seconds < DT_INTERVAL_STEPLENGTH)
			{
				seconds = DT_INTERVAL_STEPLENGTH;
			}
			else if (seconds > DT_INTERVAL_STEPLENGTH * DT_INTERVAL_STEPS)
			{
				seconds = DT_INTERVAL_STEPLENGTH * DT_INTERVAL_STEPS;
			}
			m_sliderDummyTrafficIntervall.setValue(seconds);
		}
		m_sliderDummyTrafficIntervall.setEnabled(iTmp > -1);
		Dictionary d = m_sliderDummyTrafficIntervall.getLabelTable();
		for (int i = 1; i <= DT_INTERVAL_STEPS; i++)
		{
			( (JLabel) d.get(new Integer(i * DT_INTERVAL_STEPLENGTH))).setEnabled(
						 m_sliderDummyTrafficIntervall.isEnabled());
		}
		m_cbDenyNonAnonymousSurfing.setSelected(JAPModel.getInstance().isAskForAnyNonAnonymousRequest());
		m_cbAnonymizedHttpHeaders.setSelected(JAPModel.getInstance().isAnonymizedHttpHeaders());
		m_cbAutoConnect.setSelected(JAPModel.isAutoConnect());
		m_cbAutoReConnect.setSelected(JAPModel.isAutomaticallyReconnected());
		m_cbAutoBlacklist.setSelected(BlacklistedCascadeIDEntry.areNewCascadesInBlacklist());
		m_cbAutoChooseCascades.setSelected(JAPModel.getInstance().isCascadeAutoSwitched());
		m_cbAutoChooseCascadesOnStartup.setSelected(JAPModel.getInstance().isCascadeAutoChosenOnStartup());
		m_cbAutoChooseCascadesOnStartup.setEnabled(m_cbAutoChooseCascades.isSelected());

		/*m_comboServices[2].setEnabled(JAPModel.getInstance().isMixMinionActivated());
		m_comboServices[3].setEnabled(JAPModel.getInstance().isTorActivated());*/

		setLoginTimeout(AnonClient.getLoginTimeout());
	}


	protected boolean onOkPressed()
	{
		int dummyTraffic;
		//if (m_cbDummyTraffic.isSelected())
		{
			dummyTraffic = m_sliderDummyTrafficIntervall.getValue() * 1000;
		}/*
		else
		{
			dummyTraffic = - 1;
			// Listener settings
		}*/
		/*
		 * Set DT asynchronous; otherwise, the Event Thread is locked while the AnonClient connects
		 */
		final int dtAsync = dummyTraffic;
		new Thread(new Runnable()
		{
			public void run()
			{
				m_Controller.setDummyTraffic(dtAsync);
			}
		}).start();

		// Anonservice settings
		JAPModel.getInstance().setAskForAnyNonAnonymousRequest(m_cbDenyNonAnonymousSurfing.isSelected());
		BlacklistedCascadeIDEntry.putNewCascadesInBlacklist(m_cbAutoBlacklist.isSelected());
		JAPModel.getInstance().setAutoConnect(m_cbAutoConnect.isSelected());
		JAPModel.getInstance().setAutoReConnect(m_cbAutoReConnect.isSelected());
		JAPModel.getInstance().setCascadeAutoSwitch(m_cbAutoChooseCascades.isSelected());
		JAPModel.getInstance().setAutoChooseCascadeOnStartup(m_cbAutoChooseCascadesOnStartup.isSelected());
		JAPModel.getInstance().setAnonymizedHttpHeaders(m_cbAnonymizedHttpHeaders.isSelected());

		AnonClient.setLoginTimeout(((Integer)m_comboTimeout.getSelectedItem()).intValue() * 1000);

		return true;
	}

	public void recreateRootPanel()
	{
		JPanel panelRoot = getRootPanel();
		panelRoot.removeAll();

		m_cbDenyNonAnonymousSurfing = new JCheckBox(JAPMessages.getString(MSG_DENY_NON_ANONYMOUS_SURFING));
		m_cbAnonymizedHttpHeaders = new JCheckBox(JAPMessages.getString(MSG_ANONYMIZED_HTTP_HEADERS));
		m_cbAutoConnect = new JCheckBox(JAPMessages.getString("settingsautoConnectCheckBox"));
		m_cbAutoReConnect = new JCheckBox(JAPMessages.getString("settingsautoReConnectCheckBox"));
		m_cbAutoChooseCascades = new JCheckBox(JAPMessages.getString(MSG_AUTO_CHOOSE_CASCADES));
		m_cbAutoChooseCascadesOnStartup = new JCheckBox(JAPMessages.getString(MSG_AUTO_CHOOSE_ON_STARTUP));
		m_cbAutoBlacklist = new JCheckBox(JAPMessages.getString(MSG_LBL_WHITELIST));

		m_cbAutoChooseCascades.addItemListener(new ItemListener()
		{
			public void itemStateChanged(ItemEvent e)
			{
				m_cbAutoChooseCascadesOnStartup.setEnabled(e.getStateChange() == ItemEvent.SELECTED);
			}
		});


		//m_cbDummyTraffic = new JCheckBox(JAPMessages.getString("ngConfAnonGeneralSendDummy"));

		/** @todo implement this panel... */
		/*JPanel panelServices = new JPanel(new GridBagLayout());
		GridBagConstraints constrServices = new GridBagConstraints();
		panelServices.setBorder(new TitledBorder(panelServices.getBorder(),
												 JAPMessages.getString(MSG_TITLE_ASSIGN_SERVICES)));
		String[][][] services = {
			{ { JAPMessages.getString(MSG_SERVICE_HTTP) + " (HTTP/HTTPS)" }, { "AN.ON" } },
			{ { JAPMessages.getString(MSG_SERVICE_FTP) + " (" +
			JAPMessages.getString(MSG_PASSIVE_FTP) + ")" }, { "AN.ON" } },
			{ { JAPMessages.getString(MSG_SERVICE_EMAIL) + " (SMTP)" }, { "Mixminion" } },
			{ { JAPMessages.getString(MSG_SERVICE_SOCKS) + " (SOCKS)"} , { "Tor" } }
		};

		constrServices.weightx = 0.0;
		constrServices.weighty = 0.0;
		constrServices.gridwidth = 1;
		constrServices.gridy = 0;
		constrServices.anchor = GridBagConstraints.WEST;
		//constrServices.insets = new Insets(5, 5, 5, 5); // top,left,bottom,right
		constrServices.insets = new Insets(2, 5, 2, 5);
		m_comboServices = new JComboBox[services.length];
		for (int i = 0; i < services.length; i++)
		{
			constrServices.gridx = 0;
			constrServices.fill = GridBagConstraints.NONE;
			panelServices.add( new JLabel(services[i][0][0] + ":"), constrServices);
			constrServices.gridx = 1;

			constrServices.fill = GridBagConstraints.HORIZONTAL;
			m_comboServices[i] = new JComboBox(services[i][1]);
			panelServices.add(m_comboServices[i], constrServices);
			constrServices.gridy++;
		}
		constrServices.gridx = 2;
		constrServices.gridy = 0;
		constrServices.weightx = 1.0;
		constrServices.gridheight = 4;
		constrServices.insets = new Insets(0, 0, 0, 0);
		panelServices.add(new JLabel(), constrServices);
		constrServices.gridx = 3;
		constrServices.weightx = 0.0;
		constrServices.insets = new Insets(5, 5, 5, 5);
		panelServices.add(
			  new JAPMultilineLabel(JAPMessages.getString(MSG_EXPLAIN_ASSIGN_SERVICES_BETA)), constrServices);
		constrServices.insets = new Insets(0, 0, 0, 0);
		constrServices.gridx = 4;
		constrServices.weightx = 1.0;
		panelServices.add(new JLabel(), constrServices);*/


		panelRoot.setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 0;
		c.gridheight = 1;
		c.gridwidth = 4;
		c.insets = new Insets(10, 10, 0, 10);
		c.anchor = GridBagConstraints.NORTHWEST;
		c.fill = GridBagConstraints.BOTH;
		c.weightx = 1;
		c.weighty = 1;
		/*if (!JAPConstants.m_bReleasedVersion)
		{
			panelRoot.add(panelServices, c);
		}*/
		c.weighty = 0;
		c.gridy++;
		panelRoot.add(m_cbAnonymizedHttpHeaders, c);
		c.gridy++;
		panelRoot.add(m_cbDenyNonAnonymousSurfing, c);
		c.gridy++;		
		panelRoot.add(m_cbAutoConnect, c);
		c.gridy++;
		panelRoot.add(m_cbAutoReConnect, c);
		c.gridy++;

		c.gridwidth = 3;
		//c.insets = new Insets(insets.top, insets.left + 20, insets.bottom, insets.right);
		c.weightx = 0.0;
		c.insets = new Insets(10, 10, 0, 0);
		panelRoot.add(m_cbAutoChooseCascades, c);

		c.gridx++;
		c.gridx++;
		c.gridx++;
		c.weightx = 1;
		c.gridwidth = 1;
		c.insets = new Insets(10, 0, 0, 0);
		panelRoot.add(m_cbAutoChooseCascadesOnStartup, c);
		
		c.gridx = 0;
		c.gridy++;
		c.weightx = 0.0;
		c.gridwidth = 4;
		c.insets = new Insets(10, 10, 0, 10);
		panelRoot.add(m_cbAutoBlacklist, c);
				
		c.gridy++;				
		c.gridwidth = 2;
		//panelRoot.add(m_cbDummyTraffic, c);
		panelRoot.add(new JLabel(JAPMessages.getString("ngConfAnonGeneralSendDummy")), c);


		m_sliderDummyTrafficIntervall = new JSlider(SwingConstants.HORIZONTAL,
													DT_INTERVAL_MIN_STEP,
													DT_INTERVAL_STEPS * DT_INTERVAL_STEPLENGTH,
													DT_INTERVAL_DEFAULT * DT_INTERVAL_STEPLENGTH);
		Hashtable ht = new Hashtable(DT_INTERVAL_STEPS);
		for (int i = 1; i <= DT_INTERVAL_STEPS; i++)
		{
			ht.put(new Integer(i * DT_INTERVAL_STEPLENGTH), new JLabel((i * DT_INTERVAL_STEPLENGTH) + "s"));
		}
		m_sliderDummyTrafficIntervall.setLabelTable(ht);

		m_sliderDummyTrafficIntervall.setMajorTickSpacing(DT_INTERVAL_STEPLENGTH);
		m_sliderDummyTrafficIntervall.setMinorTickSpacing(1);
		m_sliderDummyTrafficIntervall.setPaintLabels(true);
		m_sliderDummyTrafficIntervall.setPaintTicks(true);
		m_sliderDummyTrafficIntervall.setSnapToTicks(true);
		c.gridx++;
		c.gridx++;
		c.gridwidth = 2;
		panelRoot.add(m_sliderDummyTrafficIntervall, c);

		c.gridy++;
		c.gridx = 0;
		c.gridwidth = 1;
		panelRoot.add(new JLabel(JAPMessages.getString(MSG_CONNECTION_TIMEOUT) + " (s):"), c);
		m_comboTimeout = new JComboBox(LOGIN_TIMEOUTS);
		c.fill = GridBagConstraints.NONE;
		c.gridx++;
		panelRoot.add(m_comboTimeout, c);


		c.gridy++;
		c.gridx = 0;
		c.weighty = 1.0;
		c.weightx = 1.0;
		c.gridwidth = 4;
		c.fill = GridBagConstraints.BOTH;
		panelRoot.add(new JLabel(), c);

		/*
		m_cbDummyTraffic.addItemListener(new ItemListener()
		{
			public void itemStateChanged(ItemEvent e)
			{
				m_sliderDummyTrafficIntervall.setEnabled(e.getStateChange() == ItemEvent.SELECTED);
				Dictionary d = m_sliderDummyTrafficIntervall.getLabelTable();
				for (int i = 1; i <= DT_INTERVAL_STEPS; i++)
				{
					( (JLabel) d.get(new Integer(i*DT_INTERVAL_STEPLENGTH))).setEnabled(e.getStateChange() ==
						ItemEvent.SELECTED);
				}
			}
		});*/
		updateValues(false);
	}

	//defaults
	public void onResetToDefaultsPressed()
	{
		m_cbDenyNonAnonymousSurfing.setSelected(true);
		m_cbAnonymizedHttpHeaders.setSelected(JAPConstants.ANONYMIZED_HTTP_HEADERS);
		//m_cbDummyTraffic.setSelected(true);
		m_cbAutoBlacklist.setSelected(BlacklistedCascadeIDEntry.DEFAULT_AUTO_BLACKLIST);
		m_sliderDummyTrafficIntervall.setEnabled(true);
		m_sliderDummyTrafficIntervall.setValue(DT_INTERVAL_DEFAULT);
		m_cbAutoConnect.setSelected(true);
		m_cbAutoReConnect.setSelected(true);
		m_cbAutoChooseCascades.setSelected(true);
		m_cbAutoChooseCascadesOnStartup.setSelected(true);
		setLoginTimeout(AnonClient.DEFAULT_LOGIN_TIMEOUT);
	}

	public String getHelpContext()
	{
		return "services_general";
	}

	protected void onRootPanelShown()
	{
		/*m_comboServices[2].setEnabled(JAPModel.getInstance().isMixMinionActivated());
		m_comboServices[3].setEnabled(JAPModel.getInstance().isTorActivated());
		for (int i = 0; i < m_comboServices.length; i++)
		{
			if (m_comboServices[i].isEnabled())
			{
				m_comboServices[i].setToolTipText(null);
			}
			else
			{
				m_comboServices[i].setToolTipText(JAPMessages.getString(MSG_TOOLTIP_SERVICE_DEACTIVATED));
			}

		}*/
	}

	private void setLoginTimeout(int a_timeoutMS)
	{
		int timeout = a_timeoutMS / 1000;

		if (timeout >= ((Integer)m_comboTimeout.getItemAt(m_comboTimeout.getItemCount() - 1)).intValue())
		{
			m_comboTimeout.setSelectedIndex(m_comboTimeout.getItemCount() - 1);
			AnonClient.setLoginTimeout(((Integer)m_comboTimeout.getSelectedItem()).intValue() * 1000);
		}
		else if (timeout <=((Integer)m_comboTimeout.getItemAt(0)).intValue())
		{
			m_comboTimeout.setSelectedIndex(0);
			AnonClient.setLoginTimeout(((Integer)m_comboTimeout.getSelectedItem()).intValue() * 1000);
		}
		else
		{
			for (int i = 1; i < m_comboTimeout.getItemCount(); i++)
			{
				if (timeout <= ((Integer)m_comboTimeout.getItemAt(i)).intValue())
				{
					m_comboTimeout.setSelectedIndex(i);
					break;
				}
			}
		}
	}
}
