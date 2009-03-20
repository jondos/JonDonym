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

import java.util.Vector;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;

import anon.crypto.SignatureVerifier;
import anon.infoservice.Database;
import anon.infoservice.MixCascade;
import anon.infoservice.MixInfo;
import anon.infoservice.PerformanceInfo;
import anon.infoservice.StatusInfo;
import anon.util.JAPMessages;

/**
 * This is only a wrapper class for the configuration classes under the services node. It is only
 * used for forwarding the events and creating the tabbed pane. Attention: The createSavePoint()
 * call and the setFontSetting() call are not forwarded. This should be no problem because at the
 * moment none of the service modules uses savepoints and setFontSetting() is never called.
 */
public class JAPConfServices extends AbstractJAPConfModule
{

	/**
	 * Stores the module for the AN.ON tab.
	 */
	private JAPConfAnon m_anonModule;

	/**
	 * Stores the module for the TOR tab.
	 */
	private JAPConfTor m_torModule;

	/**
	 * Stores the module for the Mixminion tab.
	 */
	private JAPConfMixminion m_mixminionModule;

	/**
	 * Stores the module for the general tab.
	 */
	private JAPConfAnonGeneral m_anonGeneralModule;
	
	private JAPConfTC m_tcModule;

	/** Stores the tabbed pane  **/
	private JTabbedPane m_tabsAnon;
	private Vector m_tabbedModules;

	/**
	 * Constructor for JAPConfServices. We do some initialization here.
	 */
	public JAPConfServices()
	{
		super(null);
	}
	
	protected boolean initObservers()
	{
		if (super.initObservers())
		{
			synchronized (LOCK_OBSERVABLE)
			{
				m_anonModule.initObservers();
				m_torModule.initObservers();
				m_mixminionModule.initObservers();
				m_anonGeneralModule.initObservers();
				m_tcModule.initObservers();
			}
			return true;
		}
		return false;
	}

	/**
	 * Creates the services configuration root panel with all child components (tabbed pane
	 * for the services and the service panes itself).
	 */
	public synchronized void recreateRootPanel()
	{
		JPanel rootPanel = getRootPanel();

		/* we cannot access the varibales directly because this method is called also by the parent
		 * constructor -> maybe we have to initialize the variables first
		 */
		JAPConfAnon anonModule = getAnonModule();
		JAPConfTor torModule = getTorModule();
		JAPConfMixminion mixminionModule = getMixminionModule();
		JAPConfAnonGeneral anonGeneralModule = getAnonGeneralModule();
		JAPConfTC tcModule = getTCModule();

		synchronized (this)
		{
			/* clear the whole root panel */
			rootPanel.removeAll();
			/* call the handler on the service modules */
			/*anonModule.recreateRootPanel();
			torModule.recreateRootPanel();
			mixminionModule.recreateRootPanel();
			anonGeneralModule.recreateRootPanel();*/
			/* rebuild the services panel */

			m_tabsAnon = new JTabbedPane();
			m_tabbedModules = new Vector();


			GridBagLayout rootPanelLayout = new GridBagLayout();
			rootPanel.setLayout(rootPanelLayout);
			GridBagConstraints rootPanelConstraints = createTabbedRootPanelContraints();

			if (JAPModel.getDefaultView() != JAPConstants.VIEW_SIMPLIFIED)
			{
				m_tabsAnon.addTab(anonModule.getTabTitle(), anonModule.getRootPanel());
				m_tabbedModules.addElement(anonModule);
				//uncomment to enable the T&C tabs.
				//m_tabsAnon.addTab(tcModule.getTabTitle(), tcModule.getRootPanel());
				//m_tabbedModules.addElement(tcModule);
				if (!JAPConstants.m_bReleasedVersion)
				{
					m_tabsAnon.addTab(torModule.getTabTitle(), torModule.getRootPanel());
					m_tabbedModules.addElement(torModule);
					m_tabsAnon.addTab(mixminionModule.getTabTitle(), mixminionModule.getRootPanel());
					m_tabbedModules.addElement(mixminionModule);
				}
				m_tabsAnon.addTab(anonGeneralModule.getTabTitle(), anonGeneralModule.getRootPanel());
				m_tabbedModules.addElement(anonGeneralModule);
				rootPanel.add(m_tabsAnon, rootPanelConstraints);
			}
			else
			{
				rootPanelConstraints.weightx = 0;
				rootPanelConstraints.weighty = 0;
				rootPanel.add(anonModule.getRootPanel(), rootPanelConstraints);
				rootPanelConstraints.weightx = 1;
				rootPanelConstraints.weighty = 1;
				rootPanel.add(new JLabel(), rootPanelConstraints);
			}
		}
	}

	/**
	 * Returns the title for the services configuration within the configuration tree.
	 *
	 * @return The title for the services configuration leaf within the tree.
	 */
	public String getTabTitle()
	{
		return JAPMessages.getString("ngAnonymitaet");
	}

	/**
	 * Processes the 'OK' pressed event on all service modules.
	 *
	 * @return True, if all service modules returned true or false, if one module returned a veto
	 *         to this event.
	 */
	protected boolean onOkPressed()
	{
		/* forward the event to all service modules */
		boolean resultValue1 = m_anonModule.okPressed();
		boolean resultValue2 = m_torModule.okPressed();
		boolean resultValue3 = m_mixminionModule.okPressed();
		boolean resultValue4 = m_anonGeneralModule.okPressed();
		boolean resultValue5 = m_tcModule.okPressed();
		return (resultValue1 && resultValue2 && resultValue3 && resultValue4 && resultValue5);
	}

	/**
	 * Processes the 'Cancel' pressed event on all service modules.
	 */
	protected void onCancelPressed()
	{
		/* forward the event to all service modules */
		m_anonModule.cancelPressed();
		m_torModule.cancelPressed();
		m_mixminionModule.cancelPressed();
		m_anonGeneralModule.cancelPressed();
		m_tcModule.cancelPressed();
	}

	public String getHelpContext()
	{
		if (JAPModel.getDefaultView() != JAPConstants.VIEW_SIMPLIFIED)
		{
			AbstractJAPConfModule module =
				( (AbstractJAPConfModule) m_tabbedModules.elementAt(m_tabsAnon.getSelectedIndex()));

			return module.getHelpContext();
		}
		else
		{
			return m_anonModule.getHelpContext();
		}
	}

	protected void onRootPanelShown()
	{
		if (JAPModel.getDefaultView() != JAPConstants.VIEW_SIMPLIFIED)
		{
			( (AbstractJAPConfModule) m_tabbedModules.elementAt(m_tabsAnon.getSelectedIndex())).
				onRootPanelShown();
		}
		else
		{
			m_anonModule.onRootPanelShown();
		}
	}

	/**
	 * Processes the 'Reset to defaults' pressed event on all service modules.
	 */
	protected void onResetToDefaultsPressed()
	{
		/* forward the event to all service modules */
		m_anonModule.resetToDefaultsPressed();
		m_torModule.resetToDefaultsPressed();
		m_mixminionModule.resetToDefaultsPressed();
		m_anonGeneralModule.resetToDefaultsPressed();
		m_tcModule.resetToDefaultsPressed();
	}

	/**
	 * Processes the update values event on all service modules.
	 */
	protected void onUpdateValues()
	{
		//synchronized (JAPConf.getInstance())
		{
			/* forward the event to all service modules */
			m_anonModule.updateValues(false);
			m_torModule.updateValues(false);
			m_mixminionModule.updateValues(false);
			m_anonGeneralModule.updateValues(false);
			m_tcModule.updateValues(false);
		}
	}

	/**
	 * Returns the AN.ON module. The module is created, if necessary.
	 *
	 * @return The AN.ON configuration module.
	 */
	private JAPConfAnon getAnonModule()
	{
		synchronized (this)
		{
			if (m_anonModule == null)
			{
				m_anonModule = new JAPConfAnon(null);
			}
		}
		return m_anonModule;
	}

	/**
	 * Returns the TOR module. The module is created, if necessary.
	 *
	 * @return The TOR configuration module.
	 */
	private JAPConfTor getTorModule()
	{
		synchronized (this)
		{
			if (m_torModule == null)
			{
				m_torModule = new JAPConfTor();
			}
		}
		return m_torModule;
	}

	/**
	 * Returns the Mixminion module. The module is created, if necessary.
	 *
	 * @return The Mixminion configuration module.
	 */
	private JAPConfMixminion getMixminionModule()
	{
		synchronized (this)
		{
			if (m_mixminionModule == null)
			{
				m_mixminionModule = new JAPConfMixminion();
			}
		}
		return m_mixminionModule;
	}

	/**
	 * Returns the general configuration module. The module is created, if necessary.
	 *
	 * @return The general configuration module.
	 */
	private JAPConfAnonGeneral getAnonGeneralModule()
	{
		synchronized (this)
		{
			if (m_anonGeneralModule == null)
			{
				m_anonGeneralModule = new JAPConfAnonGeneral(null);
			}
		}
		return m_anonGeneralModule;
	}

	private JAPConfTC getTCModule()
	{
		synchronized (this)
		{
			if (m_tcModule == null)
			{
				m_tcModule = new JAPConfTC(null, JAPController.getInstance());
			}
		}
		return m_tcModule;
	}
	
	public synchronized void selectAnonTab(MixCascade a_cascade, boolean a_bShowFilter)
	{
		if (JAPModel.getDefaultView() != JAPConstants.VIEW_SIMPLIFIED)
		{
			m_tabsAnon.setSelectedIndex(0);
		}
		m_anonModule.setSelectedCascade(a_cascade);
		
		if(a_bShowFilter)
		{
			m_anonModule.showFilter();
		}
	}
}
