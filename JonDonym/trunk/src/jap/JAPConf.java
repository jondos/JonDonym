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

import java.io.File;
import java.io.IOException;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Observable;
import java.util.Observer;
import java.util.Vector;

import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.WindowListener;
import java.awt.event.WindowEvent;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;

import platform.AbstractOS;

import anon.infoservice.MixCascade;
import gui.GUIUtils;
import gui.JAPMultilineLabel;
import gui.dialog.JAPDialog;
import gui.help.JAPHelp;

import jap.forward.JAPConfForwardingServer;
import jap.forward.JAPConfForwardingState;
import jap.pay.AccountSettingsPanel;
import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;
import java.awt.Dimension;
import anon.pay.PayAccount;
import anon.util.JAPMessages;

final public class JAPConf extends JAPDialog implements ActionListener, WindowListener
{

	/** Messages */
	public static final String MSG_READ_PANEL_HELP = JAPConf.class.getName() + "_readPanelHelp";
	private static final String MSG_DETAILLEVEL = JAPConf.class.getName() + "_detaillevel";
	private static final String MSG_BTN_SAVE = JAPConf.class.getName() + "_btnSave";
	private static final String MSG_ASK_RESET_DEFAULTS = JAPConf.class.getName() + "_askResetDefaults";
	private static final String MSG_NEED_RESTART = JAPConf.class.getName() + "_needRestart";

	//final static public String PORT_TAB = "PORT_TAB";
	final static public String NETWORK_TAB = "NETWORK_TAB";
	final static public String UI_TAB = "UI_TAB";
	final static public String UPDATE_TAB = "UPDATE_TAB";
	final static public String PROXY_TAB = "PROXY_TAB";
	final static public String INFOSERVICE_TAB = "INFOSERVICE_TAB";
	final static public String ANON_TAB = "ANON_TAB";
	final static public String ANON_SERVICES_TAB = "SERVICES_TAB";
	final static public String ANON_TRUST_TAB = "ANON_TRUST_TAB";
	final static public String CERT_TAB = "CERT_TAB";
	final static public String TOR_TAB = "TOR_TAB";
	final static public String DEBUG_TAB = "DEBUG_TAB";
	final static public String PAYMENT_TAB = "PAYMENT_TAB";
	final static public String HTTP_FILTER_TAB = "HTTP_FILTER_TAB";

	/**
	 * This constant is a symbolic name for accessing the forwarding client configuration tab.
	 */
	final static public String FORWARDING_CLIENT_TAB = "FORWARDING_CLIENT_TAB";

	/**
	 * This constant is a symbolic name for accessing the forwarding server configuration tab.
	 */
	final static public String FORWARDING_SERVER_TAB = "FORWARDING_SERVER_TAB";

	/**
	 * This constant is a symbolic name for accessing the forwarding state tab.
	 */
	final static public String FORWARDING_STATE_TAB = "FORWARDING_STATE_TAB";

	private static JAPConf ms_JapConfInstance = null;

	private JAPController m_Controller;


	private JCheckBox[] m_cbLogTypes;
	private JCheckBox m_cbShowDebugConsole, m_cbDebugToFile;
	private JTextField m_tfDebugFileName;
	private JButton m_bttnDebugFileNameSearch;
	private JAPMultilineLabel m_labelConfDebugLevel, m_labelConfDebugTypes;

	private JSlider m_sliderDebugLevel;
	private JSlider m_sliderDebugDetailLevel;

	private JPanel m_pMisc;
	private JButton m_bttnDefaultConfig, m_bttnCancel, m_bttnHelp;

	//private Font m_fontControls;

	private boolean m_bWithPayment = false;
	private boolean m_bIsSimpleView;
	private Vector m_vecConfigChangesNeedRestart = new Vector();

	private JAPConfModuleSystem m_moduleSystem;
	private JAPConfServices m_confServices;
	private AbstractJAPMainView m_parentView;
	private AccountSettingsPanel m_accountSettings;
	private JAPConfUI m_confUI;

	public static JAPConf getInstance()
	{
		return ms_JapConfInstance;
	}

	public JAPConf(AbstractJAPMainView frmParent, boolean loadPay)
	{
		super(frmParent, JAPMessages.getString("settingsDialog"), true);
		m_parentView = frmParent;
		setDefaultCloseOperation(HIDE_ON_CLOSE);
		m_bWithPayment = loadPay;
		m_bIsSimpleView = (JAPModel.getDefaultView() == JAPConstants.VIEW_SIMPLIFIED);
		/* set the instance pointer */
		ms_JapConfInstance = this;
		m_Controller = JAPController.getInstance();
		JPanel pContainer = new JPanel();
		GridBagLayout gbl = new GridBagLayout();
		pContainer.setLayout(gbl);

		//m_pFirewall = buildProxyPanel();
		m_pMisc = buildMiscPanel();

		m_moduleSystem = new JAPConfModuleSystem();
		DefaultMutableTreeNode rootNode = m_moduleSystem.getConfigurationTreeRootNode();
		m_confUI = new JAPConfUI();
		m_moduleSystem.addConfigurationModule(rootNode, m_confUI, UI_TAB);
		if (m_bWithPayment)
		{
			m_accountSettings = new AccountSettingsPanel();
			m_moduleSystem.addConfigurationModule(rootNode, m_accountSettings, PAYMENT_TAB);
		}
		if (!m_bIsSimpleView)
		{
			m_moduleSystem.addConfigurationModule(rootNode, new JAPConfUpdate(), UPDATE_TAB);
		}

		//DefaultMutableTreeNode nodeNet = m_moduleSystem.addComponent(rootNode, null, "ngTreeNetwork", null,
			//null);
		//m_moduleSystem.addComponent(rootNode, m_pPort, "ngTreeNetwork", NETWORK_TAB, "network");
		//m_moduleSystem.addComponent(nodeNet, m_pFirewall, "confProxyTab", PROXY_TAB, "proxy");
		m_moduleSystem.addConfigurationModule(rootNode, new JAPConfNetwork(), NETWORK_TAB);


		m_confServices = new JAPConfServices();
		
		DefaultMutableTreeNode nodeAnon =
			m_moduleSystem.addComponent(rootNode, null, "ngTreeAnonService", null, null);
		if (!m_bIsSimpleView)
		{
			m_moduleSystem.addConfigurationModule(nodeAnon, m_confServices, ANON_SERVICES_TAB);
			m_moduleSystem.addConfigurationModule(nodeAnon, new JAPConfInfoService(), INFOSERVICE_TAB);			
			//m_moduleSystem.addConfigurationModule(nodeAnon, new JAPConfTrust(), ANON_TRUST_TAB);
			m_moduleSystem.addConfigurationModule(nodeAnon, new JAPConfForwardingServer(),
												  FORWARDING_SERVER_TAB);
			m_moduleSystem.addConfigurationModule(nodeAnon, new JAPConfCert(), CERT_TAB);
			
			// will be added later, just a template for now
			//m_moduleSystem.addConfigurationModule(nodeAnon, new JAPConfHTTPFilter(), HTTP_FILTER_TAB);
			DefaultMutableTreeNode debugNode =
				m_moduleSystem.addComponent(rootNode, m_pMisc, "ngTreeDebugging", DEBUG_TAB, "debugging");
			if (JAPModel.getInstance().isForwardingStateModuleVisible())
			{
				m_moduleSystem.addConfigurationModule(debugNode, new JAPConfForwardingState(),
					FORWARDING_STATE_TAB);
			}
		}
		else
		{
			//DefaultMutableTreeNode dummy = 
				//m_moduleSystem.addConfigurationModule(rootNode, m_confServices, ANON_SERVICES_TAB);
			//m_moduleSystem.getConfigurationTree().expandPath(new TreePath(dummy.getPath()));
			m_moduleSystem.addConfigurationModule(nodeAnon, m_confServices, ANON_SERVICES_TAB);
		}
		m_moduleSystem.getConfigurationTree().expandPath(new TreePath(nodeAnon.getPath()));
		
		//JAPExtension.addOptOut(m_moduleSystem);

		
		//m_moduleSystem.getConfigurationTree().expandPath(new TreePath(nodeNet.getPath()));

	//	m_moduleSystem.getConfigurationTree().setSelectionRow(0);
		/* after finishing building the tree, it is important to update the tree size */
/*		
		m_moduleSystem.getConfigurationTree().setMinimumSize(m_moduleSystem.getConfigurationTree().
			getPreferredSize());*/

		JPanel buttonPanel = new JPanel();
		buttonPanel.setLayout(new FlowLayout(FlowLayout.RIGHT));
		m_bttnHelp = new JButton(JAPMessages.getString(JAPHelp.MSG_HELP_BUTTON));
		//m_bttnHelp = new JButton(GUIUtils.loadImageIcon(JAPHelp.IMG_HELP, true));
		buttonPanel.add(m_bttnHelp);
		m_bttnHelp.addActionListener(this);

		m_bttnDefaultConfig = new JButton(JAPMessages.getString("bttnDefaultConfig"));
		final JAPDialog view = this;
		m_bttnDefaultConfig.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				if (JAPDialog.showConfirmDialog(view, JAPMessages.getString(MSG_ASK_RESET_DEFAULTS),
												JAPDialog.OPTION_TYPE_OK_CANCEL,
												JAPDialog.MESSAGE_TYPE_WARNING)
					== JAPDialog.RETURN_VALUE_OK)
				{
					resetToDefault();
				}
			}
		});
		if (!JAPModel.isSmallDisplay())
		{
			buttonPanel.add(m_bttnDefaultConfig);
		}
		m_bttnCancel = new JButton(JAPMessages.getString("cancelButton"));
		m_bttnCancel.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				cancelPressed();
			}
		});
		buttonPanel.add(m_bttnCancel);
		JButton bttnSave = new JButton(JAPMessages.getString(MSG_BTN_SAVE));
		bttnSave.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				okPressed(false);
			}
		});

		buttonPanel.add(bttnSave);

		JButton ok = new JButton(JAPMessages.getString("okButton"));
		ok.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				okPressed(true);
			}
		});
		//ok.setFont(m_fontControls);
		buttonPanel.add(ok);
		buttonPanel.add(new JLabel("   "));
		getRootPane().setDefaultButton(ok);

		JPanel moduleSystemPanel = m_moduleSystem.getRootPanel();

		GridBagLayout configPanelLayout = new GridBagLayout();
		pContainer.setLayout(configPanelLayout);

		GridBagConstraints configPanelConstraints = new GridBagConstraints();
		configPanelConstraints.anchor = GridBagConstraints.NORTHWEST;
		configPanelConstraints.fill = GridBagConstraints.BOTH;
		configPanelConstraints.weightx = 1.0;
		configPanelConstraints.weighty = 1.0;
		configPanelConstraints.gridx = 0;
		configPanelConstraints.gridy = 0;
		configPanelLayout.setConstraints(moduleSystemPanel, configPanelConstraints);
		pContainer.add(moduleSystemPanel);

		configPanelConstraints.weighty = 0.0;
		configPanelConstraints.gridx = 0;
		configPanelConstraints.gridy = 1;
		configPanelConstraints.insets = new Insets(10, 10, 10, 10);
		configPanelLayout.setConstraints(buttonPanel, configPanelConstraints);
		pContainer.add(buttonPanel);

		setContentPane(pContainer);
		updateValues();
		// largest tab to front
		//m_moduleSystem.selectNode(NETWORK_TAB); // Temp solution: UI is the largest!!!
		
		// select the last module (this may help against vanishing module entries after faulty pack operations)
		if (JAPModel.getDefaultView() == JAPConstants.VIEW_SIMPLIFIED)
		{
			m_moduleSystem.selectNode(ANON_SERVICES_TAB);
		}
		else
		{
			m_moduleSystem.selectNode(DEBUG_TAB);
		}
		
		if (JAPModel.isSmallDisplay())
		{
			setSize(240, 300);
			setLocation(0, 0);
		}
		else
		{
			if (JAPModel.getInstance().isConfigWindowSizeSaved() &&
				JAPModel.getInstance().getConfigSize() != null)
			{
				setSize(JAPModel.getInstance().getConfigSize());
			}
			else
			{
				doPack();
			}
		}
		m_confUI.afterPack();
		
		// do not make this panel smaller than needed by the menu
		m_moduleSystem.getConfigurationTree().setMinimumSize(m_moduleSystem.getConfigurationTree().
			getPreferredSize());
		
		m_moduleSystem.selectNode(UI_TAB);
		restoreLocation(JAPModel.getInstance().getConfigWindowLocation());
		//setDockable(true);
		this.addWindowListener(this);

		m_moduleSystem.initObservers();
		
		JAPModel.getInstance().addObserver(new Observer()
		{
			public void update(Observable a_observable, final Object a_message)
			{
				if (a_message instanceof JAPModel.FontResize)
				{
					Runnable run = new Runnable()
					{
						public void run()
						{
							// font changed
							SwingUtilities.updateComponentTreeUI(getContentPane());
							//m_bttnHelp.setIcon(GUIUtils.loadImageIcon(JAPHelp.IMG_HELP, true));
						}
					};
					if (SwingUtilities.isEventDispatchThread())
					{
						run.run();
					}
					else
					{
						SwingUtilities.invokeLater(run);
					}

				}
			}
		});
	}

	public void windowClosed(WindowEvent e)
	{

	}

	public void windowClosing(WindowEvent e)
	{
		cancelPressed();
	}

	public void windowDeactivated(WindowEvent e)
	{

	}

	public void windowActivated(WindowEvent e)
	{

	}

	public void windowDeiconified(WindowEvent e)
	{

	}

	public void windowIconified(WindowEvent e)
	{

	}

	public void windowOpened(WindowEvent e)
	{

	}

	protected synchronized void doPack()
	{
		// synchronize with updateValues of AbstractJAPConfModul AWT thread so that updates and pack events do not overlap
		boolean bError = false;
		//boolean bRetry = true;
		boolean bRetry = false;
		
		
		try 
		{
			SwingUtilities.invokeAndWait(new Runnable()
			{
				public void run()
				{
					m_moduleSystem.revalidate();
				}
			});
		} 
		catch (Exception a_e) 
		{
			LogHolder.log(LogLevel.ERR, LogType.GUI, a_e);
		} 
		
		while (!bError)
		{
			pack();
			if (getSize().width < getSize().height)
			{
				LogHolder.log(LogLevel.ERR, LogType.GUI,
							  "Could not pack config properly. Width is smaller than height! " +
							  "Width:" + getSize().width + " Height:" + getSize().height);
				bError = true;
				/*
				try 
				{
					wait(2000);
					continue;
				} 
				catch (InterruptedException e) 
				{
					bError = true;
				}*/
			}
			else if (getSize().width > getScreenBounds().width ||
					 getSize().height > getScreenBounds().height)
			{
				LogHolder.log(LogLevel.ERR, LogType.GUI, "Packed config view with illegal size! " +
							  getSize());
	
				bError = true;
			}
			else
			{
				JAPModel.getInstance().setConfigSize(getSize());
			}
			
			if (bError)
			{	
				m_moduleSystem.revalidate();
				if (bRetry)
				{
					bError = false;
					bRetry = false;
					continue;
				}
				
				if (JAPModel.getInstance().getConfigSize() != null &&
						JAPModel.getInstance().getConfigSize().width > 0 &&
						JAPModel.getInstance().getConfigSize().height > 0)
				{
					setSize(JAPModel.getInstance().getConfigSize());
				}
				else
				{
					// default size for MacOS
					setSize(new Dimension(786, 545));
				}
				LogHolder.log(LogLevel.ERR, LogType.GUI, "Setting default config size to " + getSize());
			}
			break;
		}
		
	}

	public static abstract class AbstractRestartNeedingConfigChange
	{
		public abstract String getName();
		public abstract void doChange();
		public void doCancel()
		{

		}
		public String getMessage()
		{
			return "";
		}
	}

	/**
	 * This method to show the Dialog We need it for
	 * creating the module savepoints. After this, we call the parent setVisible(true) method.
	 */
	public void setVisible(boolean a_bVisible)
	{
		/* every time the configuration is set to visible, we need to create the savepoints for the
		 * modules for the case that 'Cancel' is pressed later
		 */
		if (a_bVisible)
		{
			m_parentView.getViewIconified().switchBackToMainView();
			m_moduleSystem.createSavePoints();
		}
		/* call the original method */
		super.setVisible(a_bVisible);
	}

	public void actionPerformed(ActionEvent e)
	{
		if (e.getSource() == m_bttnHelp)
		{
			JAPHelp.getInstance().setContext(m_moduleSystem);
			JAPHelp.getInstance().loadCurrentContext();
		}
	}

	private JPanel buildMiscPanel()
	{
		JPanel p = new JPanel(new GridBagLayout());
		p.setBorder(new TitledBorder("Debugging"));
		GridBagConstraints c = new GridBagConstraints();
		c.anchor = GridBagConstraints.NORTHWEST;
		c.insets = new Insets(5, 5, 5, 5);
		JPanel panelLogTypes = new JPanel(new GridLayout(0, 1));
		m_cbLogTypes = new JCheckBox[LogType.getNumberOfLogTypes()];
		int[] availableLogTypes = LogType.getAvailableLogTypes();
		for (int i = 0; i < m_cbLogTypes.length; i++)
		{
			m_cbLogTypes[i] = new JCheckBox(LogType.getLogTypeName(availableLogTypes[i]));
			if (i > 0)
			{
				panelLogTypes.add(m_cbLogTypes[i]);
			}
		}

		m_labelConfDebugTypes = new JAPMultilineLabel(JAPMessages.getString("ConfDebugTypes"));
		p.add(m_labelConfDebugTypes, c);
		c.gridy = 1;
		p.add(panelLogTypes, c);
		c.gridy = 2;
		c.gridwidth = 5;
		c.weightx = 1;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.insets = new Insets(0, 5, 0, 5);
		p.add(new JSeparator(), c);
		m_cbShowDebugConsole = new JCheckBox(JAPMessages.getString("ConfDebugShowConsole"));
		m_cbShowDebugConsole.setSelected(JAPDebug.isShowConsole());
		JAPDebug.getInstance().addObserver(new Observer()
		{
			public void update(Observable a_observable, final Object a_message)
			{
				m_cbShowDebugConsole.setSelected(false);
			}
		});
		m_cbShowDebugConsole.addItemListener(new ItemListener()
		{
			public void itemStateChanged(ItemEvent e)
			{
				JAPDebug.showConsole(e.getStateChange() == ItemEvent.SELECTED,
									 JAPController.getInstance().getViewWindow());
			}
		});
		c.gridy = 3;
		c.weighty = 0;
		c.insets = new Insets(5, 5, 5, 5);
		p.add(m_cbShowDebugConsole, c);

		m_cbDebugToFile = new JCheckBox(JAPMessages.getString("ConfDebugFile"));
		m_cbDebugToFile.addItemListener(new ItemListener()
		{
			public void itemStateChanged(ItemEvent e)
			{
				boolean b = m_cbDebugToFile.isSelected();
				m_bttnDebugFileNameSearch.setEnabled(b);
				m_tfDebugFileName.setEnabled(b);
			}
		});

		c.gridy = 4;
		c.weighty = 0;
		p.add(m_cbDebugToFile, c);
		JPanel panelDebugFileName = new JPanel(new GridBagLayout());
		GridBagConstraints c1 = new GridBagConstraints();
		m_tfDebugFileName = new JTextField(20);
		c1.weightx = 1;
		c1.insets = new Insets(0, 5, 0, 5);
		c1.fill = GridBagConstraints.HORIZONTAL;
		panelDebugFileName.add(m_tfDebugFileName, c1);
		m_bttnDebugFileNameSearch = new JButton(JAPMessages.getString("ConfDebugFileNameSearch"));
		m_bttnDebugFileNameSearch.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				JFileChooser fileChooser = new JFileChooser();
				String strCurrentFile = ms_JapConfInstance.m_tfDebugFileName.getText().trim();
				if (!strCurrentFile.equals(""))
				{
					try
					{
						fileChooser.setCurrentDirectory(new File(new File(strCurrentFile).getParent()));
					}
					catch (Exception e1)
					{
						strCurrentFile = "";
					}
				}
				if (JAPController.getInstance().isPortableMode() && strCurrentFile.equals(""))
				{
					strCurrentFile = AbstractOS.getInstance().getProperty("user.dir");
					if (strCurrentFile != null)
					{
						fileChooser.setCurrentDirectory(new File(strCurrentFile));
					}
				}
				int ret = GUIUtils.showMonitoredFileChooser(fileChooser, ms_JapConfInstance.getContentPane());
				if (ret == JFileChooser.APPROVE_OPTION)
				{
					try
					{
						if (JAPController.getInstance().isPortableMode())
						{
							m_tfDebugFileName.setText(AbstractOS.toRelativePath(
									fileChooser.getSelectedFile().getCanonicalPath()));
						}
						else
						{
							m_tfDebugFileName.setText(
									fileChooser.getSelectedFile().getCanonicalPath());
						}
					}
					catch (IOException ex)
					{
					}
				}
			}
		});
		c1.gridx = 1;
		c1.weightx = 0;
		panelDebugFileName.add(m_bttnDebugFileNameSearch, c1);
		c.gridy = 5;
		c.weighty = 1;
		c.fill = GridBagConstraints.HORIZONTAL;
		p.add(panelDebugFileName, c);

		JPanel panelDebugLevels = new JPanel();
		m_sliderDebugLevel = new JSlider(SwingConstants.VERTICAL, 0, 7, 0);
		m_sliderDebugLevel.addChangeListener(new ChangeListener()
		{
			public void stateChanged(ChangeEvent e)
			{
				Dictionary d = m_sliderDebugLevel.getLabelTable();
				for (int i = 0; i < LogLevel.getLevelCount(); i++)
				{
					( (JLabel) d.get(new Integer(i))).setEnabled(i <= m_sliderDebugLevel.getValue());
				}
			}
		});

		Hashtable ht = new Hashtable(LogLevel.getLevelCount(), 1.0f);
		for (int i = 0; i < LogLevel.getLevelCount(); i++)
		{
			ht.put(new Integer(i), new JLabel(" " + LogLevel.getLevelName(i)));
		}
		m_sliderDebugLevel.setLabelTable(ht);
		m_sliderDebugLevel.setPaintLabels(true);
		m_sliderDebugLevel.setMajorTickSpacing(1);
		m_sliderDebugLevel.setMinorTickSpacing(1);
		m_sliderDebugLevel.setSnapToTicks(true);
		m_sliderDebugLevel.setPaintTrack(true);
		m_sliderDebugLevel.setPaintTicks(false);
		panelDebugLevels.add(m_sliderDebugLevel);

		c.gridheight = 2;
		c.gridwidth = 1;
		c.insets = new Insets(0, 10, 0, 10);
		c.gridx = 1;
		c.gridy = 0;
		c.weightx = 0;
		c.weighty = 0;
		c.fill = GridBagConstraints.VERTICAL;
		p.add(new JSeparator(SwingConstants.VERTICAL), c);
		c.gridheight = 1;
		c.gridwidth = 1;
		c.gridx = 2;
		c.gridy = 0;
		c.fill = GridBagConstraints.NONE;
		c.insets = new Insets(5, 5, 5, 5);
		m_labelConfDebugLevel = new JAPMultilineLabel(JAPMessages.getString("ConfDebugLevels"));
		p.add(m_labelConfDebugLevel, c);
		c.gridy = 1;
		c.weightx = 1;
		p.add(panelDebugLevels, c);

		JPanel panelDebugDetailLevel = new JPanel();
		m_sliderDebugDetailLevel = new JSlider(SwingConstants.VERTICAL, LogHolder.DETAIL_LEVEL_LOWEST,
											   LogHolder.DETAIL_LEVEL_HIGHEST, LogHolder.getDetailLevel());
		/*m_sliderDebugDetailLevel.addChangeListener(new ChangeListener()
		{
			public void stateChanged(ChangeEvent e)
			{
				LogHolder.setDetailLevel(m_sliderDebugDetailLevel.getValue());
			}
		});*/
		m_sliderDebugDetailLevel.setPaintTicks(false);
		m_sliderDebugDetailLevel.setPaintLabels(true);
		m_sliderDebugDetailLevel.setMajorTickSpacing(1);
		m_sliderDebugDetailLevel.setMinorTickSpacing(1);
		m_sliderDebugDetailLevel.setSnapToTicks(true);
		m_sliderDebugDetailLevel.setPaintTrack(true);

		panelDebugDetailLevel.add(m_sliderDebugDetailLevel);

		c.gridheight = 2;
		c.gridwidth = 1;
		c.insets = new Insets(0, 10, 0, 10);
		c.gridx = 3;
		c.gridy = 0;
		c.weightx = 0;
		c.weighty = 0;
		c.fill = GridBagConstraints.VERTICAL;
		p.add(new JSeparator(SwingConstants.VERTICAL), c);

		c.gridheight = 1;
		c.gridwidth = 1;
		c.gridx = 4;
		c.gridy = 0;
		c.fill = GridBagConstraints.NONE;
		c.insets = new Insets(5, 5, 5, 5);
		m_labelConfDebugLevel = new JAPMultilineLabel(JAPMessages.getString(MSG_DETAILLEVEL));
		p.add(m_labelConfDebugLevel, c);

		c.gridy = 1;
		c.weightx = 1;
		p.add(panelDebugDetailLevel, c);

		return p;
	}

	void cancelPressed()
	{
		m_vecConfigChangesNeedRestart.removeAllElements();
		m_moduleSystem.processCancelPressedEvent();
		setVisible(false);
	}

	/** Checks if all input in all files make sense. Displays InfoBoxes about what is wrong.
	 * @return true if all is ok
	 *					false otherwise
	 */
	private boolean checkValues()
	{
		return true;
	}

	/** Resets the Configuration to the Default values*/
	private void resetToDefault()
	{
		m_vecConfigChangesNeedRestart.removeAllElements();
		m_moduleSystem.processResetToDefaultsPressedEvent();
		m_cbShowDebugConsole.setSelected(false);
		m_sliderDebugLevel.setValue(LogLevel.WARNING);
		for (int i = 0; i < m_cbLogTypes.length; i++)
		{
			m_cbLogTypes[i].setSelected(true);
		}
		m_sliderDebugDetailLevel.setValue(LogHolder.DETAIL_LEVEL_HIGHEST);
		m_cbDebugToFile.setSelected(false);
	}

	private void onOkPressed()
	{
		/*
		System.out.println(m_moduleSystem.getConfigurationTree().getComponentCount());
		System.out.println(m_moduleSystem.getConfigurationTreeRootNode().getChildCount());		
		m_moduleSystem.getConfigurationTree().repaint();*/
		
		
		
		// Misc settings
		int[] availableLogTypes = LogType.getAvailableLogTypes();
		int logType = LogType.NUL;
		for (int i = 0; i < m_cbLogTypes.length; i++)
		{
			logType |= (m_cbLogTypes[i].isSelected() ? availableLogTypes[i] : LogType.NUL);
		}

		JAPDebug.getInstance().setLogType(logType);
		JAPDebug.getInstance().setLogLevel(m_sliderDebugLevel.getValue());
		LogHolder.setDetailLevel(m_sliderDebugDetailLevel.getValue());
		String strFilename = m_tfDebugFileName.getText().trim();
		if (!m_cbDebugToFile.isSelected())
		{
			strFilename = null;
		}
		JAPDebug.setLogToFile(strFilename);
	}

	private void okPressed(final boolean a_bCloseConfiguration)
	{
		if (!checkValues())
		{
			return;
		}
		m_vecConfigChangesNeedRestart.removeAllElements();
		if (m_moduleSystem.processOkPressedEvent() == false)
		{
			m_vecConfigChangesNeedRestart.removeAllElements();
			return;
		}
		onOkPressed();
		resetAutomaticLocation(JAPModel.getInstance().isConfigWindowLocationSaved());

		if (m_vecConfigChangesNeedRestart.size() > 0)
		{
			String strChanges = "<ul>";
			AbstractRestartNeedingConfigChange change;
			for (int i = 0; i < m_vecConfigChangesNeedRestart.size(); i++)
			{
				change = (AbstractRestartNeedingConfigChange)m_vecConfigChangesNeedRestart.elementAt(i);
				strChanges += "<li>" + change.getName();
				if (change.getMessage() != null && change.getMessage().trim().length() > 0)
				{
					strChanges += "<br>" + change.getMessage();
				}
				strChanges += "</li>";

			}
			strChanges += "</ul>";

			if (JAPDialog.showYesNoDialog(this, JAPMessages.getString(MSG_NEED_RESTART, strChanges)))
			{
				for (int i = 0; i < m_vecConfigChangesNeedRestart.size(); i++)
				{
					((AbstractRestartNeedingConfigChange)m_vecConfigChangesNeedRestart.elementAt(i)).doChange();
				}
			}
			else
			{
				for (int i = 0; i < m_vecConfigChangesNeedRestart.size(); i++)
				{
					((AbstractRestartNeedingConfigChange)m_vecConfigChangesNeedRestart.elementAt(i)).doCancel();
				}
				m_vecConfigChangesNeedRestart.removeAllElements();
				return;
			}
		}


		// We are in event dispatch thread!!
		Thread run = new Thread(new Runnable()
		{
			public void run()
			{
				// save configuration
				m_Controller.saveConfigFile();

				if (a_bCloseConfiguration && !isRestartNeeded())
				{
					setVisible(false);
				}
				// force notifying the observers set the right server name
				//m_Controller.notifyJAPObservers();

				if (isRestartNeeded())
				{
					JAPController.goodBye(false);
				}
			}
		});
		run.setDaemon(true);
		run.start();

		// ... manual settings stuff finished
	}

	

	/**
	 * Brings the specified card of the tabbed pane of the configuration window to the foreground.
	 * If there is no card with the specified symbolic name, nothing is done (current foreground
	 * card is not changed).
	 *
	 * @param a_selectedCard The card to bring to the foreground. See the TAB constants in this
	 *                       class.
	 */
	public void selectCard(String a_strSelectedCard, final Object a_value)
	{
		if (a_strSelectedCard != null)
		{
			if (a_strSelectedCard.equals(UI_TAB))
			{
				m_moduleSystem.selectNode(UI_TAB);
				new Thread(new Runnable()
				{
					public void run()
					{
						m_confUI.chooseBrowserPath();
					}
				}).start();
			}
			else if (a_strSelectedCard.equals(ANON_TAB))
			{
				m_moduleSystem.selectNode(ANON_SERVICES_TAB);
				if (a_value instanceof MixCascade)
				{
					m_confServices.selectAnonTab( (MixCascade) a_value, false);
				}
				else if(a_value instanceof Boolean)
				{
					m_confServices.selectAnonTab(null, ((Boolean) a_value).booleanValue());
				}
				else
				{
					m_confServices.selectAnonTab(null, false);
				}
			}
			else if (a_strSelectedCard.equals(PAYMENT_TAB))
			{
				m_moduleSystem.selectNode(PAYMENT_TAB);

				if (a_value != null)
				{
					new Thread(new Runnable()
					{
						public void run()
						{
							if (a_value instanceof Boolean && ((Boolean)a_value).booleanValue())
							{
								// create new account
								m_accountSettings.doCreateAccount(null);
							}
							else if (a_value instanceof String)
							{
								// insert biid
								m_accountSettings.doCreateAccount((String)a_value);
							}
							else if (a_value instanceof PayAccount)
							{
								// charge existing account
								//m_accountSettings.doChargeAccount((PayAccount)a_value);

								//show transaction for existing account
								m_accountSettings.showOpenTransaction((PayAccount)a_value);
							}
							else if (a_value instanceof Boolean && !((Boolean)a_value).booleanValue())
							{
								m_accountSettings.backupAccount();
							}
						}
					}).start();

				}
			}
			else
			{
				m_moduleSystem.selectNode(a_strSelectedCard);
			}
		}
	}

	/** Updates the shown Values from the Model.*/
	private synchronized void updateValues()
	{
		m_moduleSystem.processUpdateValuesEvent(true);
		/*		if (loadPay)
		  {
		   ( (pay.view.PayView) m_pKonto).userPanel.valuesChanged();
		  }*/
		// misc tab
		m_cbShowDebugConsole.setSelected(JAPDebug.isShowConsole());
		int[] availableLogTypes = LogType.getAvailableLogTypes();
		for (int i = 0; i < m_cbLogTypes.length; i++)
		{
			m_cbLogTypes[i].setSelected(
				( ( (JAPDebug.getInstance().getLogType() & availableLogTypes[i]) != 0) ?
				 true : false));
		}
		m_sliderDebugLevel.setValue(JAPDebug.getInstance().getLogLevel());
		m_sliderDebugDetailLevel.setValue(LogHolder.getDetailLevel());
		boolean b = JAPDebug.isLogToFile();
		m_tfDebugFileName.setEnabled(b);
		m_bttnDebugFileNameSearch.setEnabled(b);
		m_cbDebugToFile.setSelected(b);
		if (b)
		{
			m_tfDebugFileName.setText(JAPDebug.getLogFilename());
		}
		//validate(); //lead to deadlock on startup...
	}

	protected void addNeedRestart(AbstractRestartNeedingConfigChange a_change)
	{
		if (a_change != null)
		{
			m_vecConfigChangesNeedRestart.addElement(a_change);
		}
	}

	private boolean isRestartNeeded()
	{
		return m_vecConfigChangesNeedRestart.size() > 0;
	}
}
