/*
 Copyright (c) 2000-2006, The JAP-Team
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

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Locale;

import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JRadioButton;
import javax.swing.JTextPane;
import javax.swing.border.Border;

import gui.GUIUtils;
import gui.JAPHelpContext;
import gui.JAPMessages;
import gui.JTextComponentToClipboardCopier;
import gui.LanguageMapper;
import gui.dialog.DialogContentPane;
import gui.dialog.DialogContentPaneOptions;
import gui.dialog.JAPDialog;
import gui.dialog.SimpleWizardContentPane;
import gui.help.JAPHelp;
import logging.LogType;
import platform.AbstractOS;

/**
 * This is some kind of installation and configuration assistant that helps the unexperienced
 * user to get the most out of JAP/JonDo.
 *
 * @author Rolf Wendolsky
 */
public class ConfigAssistant extends JAPDialog
{
	private static final String BROWSER_JONDOFOX = "JonDoFox";
	private static final String BROWSER_FIREFOX = "Mozilla Firefox";

	private static final String MSG_WELCOME = ConfigAssistant.class.getName() + "_welcome";
	private static final String MSG_HELP = ConfigAssistant.class.getName() + "_help";
	private static final String MSG_ANON_HP = ConfigAssistant.class.getName() + "_anonHP";
	private static final String MSG_TITLE = ConfigAssistant.class.getName() + "_title";
	private static final String MSG_FINISHED = ConfigAssistant.class.getName() + "_finished";
	private static final String MSG_FINISHED_ANONTEST = 
		ConfigAssistant.class.getName() + "_menuFinishAnontest";
	private static final String MSG_FINISHED_TROUBLESHOOTING = 
		ConfigAssistant.class.getName() + "_menuFinishTroubleshooting";
	
	private static final String MSG_BROWSER_CONF = ConfigAssistant.class.getName() + "_browserConf";
	private static final String MSG_RECOMMENDED = ConfigAssistant.class.getName() + "_recommended";
	private static final String MSG_OTHER_BROWSERS = ConfigAssistant.class.getName() + "_otherBrowsers";
	private static final String MSG_CLICK_TO_VIEW_HELP = ConfigAssistant.class.getName() +
		"_clickToViewHelp";
	private static final String MSG_BROWSER_TEST = ConfigAssistant.class.getName() + "_browserTest";
	private static final String MSG_MAKE_SELECTION = ConfigAssistant.class.getName() + "_makeSelection";

	private static final String MSG_ERROR_NO_WARNING = ConfigAssistant.class.getName() + "_errorNoWarning";
	private static final String MSG_EXPLAIN_NO_WARNING = ConfigAssistant.class.getName() + "_explainNoWarning";
	private static final String MSG_EXPLAIN_NO_DIRECT_CONNECTION = ConfigAssistant.class.getName() +
		"_explainNoDirectConnection";
	private static final String MSG_EXPLAIN_FIREWALL = ConfigAssistant.class.getName() +
	"_explainFirewall";
	private static final String MSG_ERROR_WARNING_NO_SURFING = ConfigAssistant.class.getName() +
		"_errorWarningNoSurfing";
	private static final String MSG_SUCCESS_WARNING = ConfigAssistant.class.getName() + "_successWarning";
	private static final String MSG_REALLY_CLOSE = ConfigAssistant.class.getName() + "_reallyClose";
	private static final String MSG_DEACTIVATE_ACTIVE = ConfigAssistant.class.getName() +
		"_deactivateActiveContent";
	private static final String MSG_ANON_TEST = ConfigAssistant.class.getName() + "_anonTest";
	private static final String MSG_ERROR_NO_SERVICE_AVAILABLE = ConfigAssistant.class.getName() +
		"_errorNoServiceAvailable";
	private static final String MSG_ERROR_NO_CONNECTION = ConfigAssistant.class.getName() +
		"_errorNoConnection";
	private static final String MSG_ERROR_CONNECTION_SLOW = ConfigAssistant.class.getName() +
		"_errorConnectionSlow";
	private static final String MSG_ERROR_NO_SURFING = ConfigAssistant.class.getName() +
		"_errorNoSurfing";
	private static final String MSG_SUCCESS_CONNECTION = ConfigAssistant.class.getName() +
		"_successConnection";
	private static final String MSG_EXPLAIN_NO_CONNECTION = ConfigAssistant.class.getName() +
		"_explainNoConnection";
	private static final String MSG_EXPLAIN_BAD_CONNECTION = ConfigAssistant.class.getName() +
		"_explainBadConnection";
	private static final String MSG_EXPLAIN_CHOOSE_OTHER_SERVICE = ConfigAssistant.class.getName() +
	"_explainChooseOtherService";	
	private static final String MSG_EXPLAIN_NO_SERVICE_AVAILABLE = ConfigAssistant.class.getName() +
		"_explainNoServiceAvailable";
	private static final String MSG_ERROR_WARNING_IN_BROWSER = ConfigAssistant.class.getName() +
		"_errorWarningInBrowser";
	private static final String MSG_EXPLAIN_WARNING_IN_BROWSER = ConfigAssistant.class.getName() +
		"_explainWarningInBrowser";
	private static final String MSG_SELECT_VIEW = ConfigAssistant.class.getName() +
		"_selectView";
	private static final String MSG_SET_NEW_VIEW = ConfigAssistant.class.getName() + "_setNewView";
	private static final String MSG_SET_NEW_LANGUAGE = ConfigAssistant.class.getName() +
		"_setNewLanguage";
	private static final String MSG_EXPLAIN_RESTART = ConfigAssistant.class.getName() +
	"_explainRestart";
	

	private static final String PROXIES[] = {"HTTP(S)", "SSL/FTP"};

	private static final String IMG_ARROW = "arrow46.gif";
	private static final String IMG_HELP_BUTTON = ConfigAssistant.class.getName() + "_en_help.gif";
	private static final String IMG_SERVICES = ConfigAssistant.class.getName() + "_services.gif";

	private JTextPane[] m_lblHostnames = new JTextPane[PROXIES.length];
	private JTextPane[] m_lblPorts = new JTextPane[PROXIES.length];

	private JRadioButton m_radioNoWarning, m_radioSuccessWarning,
		m_radioErrorWarningNoSurfing, m_radioWarningInBrowser;
	private ButtonGroup m_groupWarning;
	private JRadioButton m_radioNoConnection, m_radioConnectionSlow, m_noSurfing, m_ConnectionOK,
		m_radioNoServiceAvailable;
	private ButtonGroup m_groupAnon;
	private JRadioButton m_radioSimpleView, m_radioAdvancedView;
	private ButtonGroup m_groupView;
	private boolean m_bFinished = false;

	public ConfigAssistant(Component a_parentComponent)
	{
		super(a_parentComponent, JAPMessages.getString(MSG_TITLE), false);
		init();
	}

	public ConfigAssistant(JAPDialog a_parentDialog)
	{
		super(a_parentDialog, JAPMessages.getString(MSG_TITLE), false);
		init();
	}

	private void init()
	{
		final Locale locale = JAPMessages.getLocale();
		final JAPDialog thisDialog = this;
		JLabel tempLabel;
		Insets insets = new Insets(0, 0, 0, 5);
		ImageIcon wizardIcon = GUIUtils.loadImageIcon("install.gif");
		GridBagConstraints constraints;

		DialogContentPane.Layout layout = new DialogContentPane.Layout(wizardIcon);
		JLabel lblImage;
		Border border;
		JComponent contentPane;
		border = BorderFactory.createRaisedBevelBorder();
		//border = BorderFactory.createLoweredBevelBorder();

		DialogContentPane paneWelcome = new SimpleWizardContentPane(
			  this, JAPMessages.getString(MSG_WELCOME), layout, null);
		contentPane = paneWelcome.getContentPane();


		constraints = new GridBagConstraints();
		contentPane.setLayout(new GridBagLayout());
		constraints.gridx = 0;
		constraints.gridy = 0;
		constraints.fill = GridBagConstraints.HORIZONTAL;
		constraints.weightx = 0;
		constraints.anchor = GridBagConstraints.WEST;
		constraints.insets = new Insets(0, 5, 0, 0);
		tempLabel = new JLabel("line");
		tempLabel.setForeground(contentPane.getBackground());
		contentPane.add(tempLabel, constraints);
		constraints.gridy++;
		tempLabel = new JLabel(JAPMessages.getString("settingsLanguage"));
		contentPane.add(tempLabel, constraints);
		final JComboBox comboLang = new JComboBox();
		String[] languages = JAPConstants.getSupportedLanguages();
		for (int i = 0; i < languages.length; i++)
		{
			comboLang.addItem(new LanguageMapper(languages[i], new Locale(languages[i], "")));
		}
		comboLang.setSelectedItem(new LanguageMapper(JAPMessages.getLocale().getLanguage()));
		constraints.gridx++;
		constraints.insets = new Insets(0, 10, 0, 0);
		contentPane.add(comboLang, constraints);
		constraints.gridx++;
		constraints.weightx = 1;
		contentPane.add(new JLabel(), constraints);

		
		final DialogContentPane paneView = new SimpleWizardContentPane(
				  this, JAPMessages.getString(MSG_SELECT_VIEW), layout,
				  new DialogContentPaneOptions(paneWelcome))
		{
			public CheckError[] checkUpdate()
			{
				JAPMessages.init(((LanguageMapper)comboLang.getSelectedItem()).getLocale(),
								 JAPConstants.MESSAGESFN);
				getButtonCancel().setText(JAPMessages.getString(DialogContentPane.MSG_CANCEL));
				getButtonNo().setText(JAPMessages.getString(DialogContentPane.MSG_PREVIOUS));
				getButtonYesOK().setText(JAPMessages.getString(DialogContentPane.MSG_NEXT));
				setText(JAPMessages.getString(MSG_SELECT_VIEW));
				
				m_radioSimpleView.setText(JAPMessages.getString("ngSettingsViewSimplified"));
				m_radioAdvancedView.setText(JAPMessages.getString("ngSettingsViewNormal"));
				return super.checkUpdate();
			}
		
			public CheckError[] checkYesOK()
			{
				CheckError[] errors = super.checkYesOK();
				if (m_groupView.getSelection() == null)
				{
					return new CheckError[]{new CheckError(
					   JAPMessages.getString(MSG_MAKE_SELECTION), LogType.GUI)};
				}

				return errors;
			}
		};
		
		contentPane = paneView.getContentPane();
		contentPane.setLayout(new GridBagLayout());
		constraints = new GridBagConstraints();
		constraints.gridx = 0;
		constraints.gridy = 0;
		constraints.anchor = GridBagConstraints.WEST;
		m_radioSimpleView = new JRadioButton(JAPMessages.getString("ngSettingsViewSimplified"));
		m_radioAdvancedView = new JRadioButton(JAPMessages.getString("ngSettingsViewNormal"));
		if (!JAPController.getInstance().isConfigAssistantShown() || JAPModel.getDefaultView() != JAPConstants.DEFAULT_VIEW)
		{
			// this is not the first start of JAP
			if (JAPModel.getDefaultView() == JAPConstants.VIEW_NORMAL)
			{
				m_radioAdvancedView.setSelected(true);
			}
			else
			{
				m_radioSimpleView.setSelected(true);
			}
		}
		m_groupView = new ButtonGroup();
		m_groupView.add(m_radioSimpleView);
		m_groupView.add(m_radioAdvancedView);
		contentPane.add(m_radioSimpleView, constraints);
		constraints.gridy++;
		contentPane.add(m_radioAdvancedView, constraints);
		
		
		final DialogContentPane paneRestart = new SimpleWizardContentPane(
			  this, JAPMessages.getString(MSG_SET_NEW_LANGUAGE) + "<br><br>" +
			  JAPMessages.getString(MSG_SET_NEW_VIEW, "ngSettingsViewNormal") + "<br><br>" + 
			  JAPMessages.getString(MSG_EXPLAIN_RESTART),
			  layout, new DialogContentPaneOptions(paneView))
		{
			public CheckError[] checkUpdate()
			{
				String strText = ""; 
				
				JAPMessages.init(((LanguageMapper)comboLang.getSelectedItem()).getLocale(),
								 JAPConstants.MESSAGESFN);
				getButtonCancel().setText(JAPMessages.getString(DialogContentPane.MSG_CANCEL));
				getButtonNo().setText(JAPMessages.getString(DialogContentPane.MSG_PREVIOUS));
				
				if (!((LanguageMapper)comboLang.getSelectedItem()).getLocale().equals(locale))
				{
					strText = JAPMessages.getString(MSG_SET_NEW_LANGUAGE);
				}
				if ((m_radioSimpleView.isSelected() && JAPModel.getDefaultView() == JAPConstants.VIEW_NORMAL) ||
					(m_radioAdvancedView.isSelected() && JAPModel.getDefaultView() == JAPConstants.VIEW_SIMPLIFIED))
				{
					String strView;
					if (m_radioSimpleView.isSelected())
					{
						strView = JAPMessages.getString("ngSettingsViewSimplified");
					}
					else
					{
						strView = JAPMessages.getString("ngSettingsViewNormal");
					}
					strText += " " + JAPMessages.getString(MSG_SET_NEW_VIEW, strView);
				}
				setText(strText + "<br><br>" + JAPMessages.getString(MSG_EXPLAIN_RESTART));
				
				return super.checkUpdate();
			}

			public boolean isSkippedAsNextContentPane()
			{
				return ((LanguageMapper)comboLang.getSelectedItem()).getLocale().equals(locale) &&
					((m_radioSimpleView.isSelected() && JAPModel.getDefaultView() == JAPConstants.VIEW_SIMPLIFIED) ||
					(m_radioAdvancedView.isSelected() && JAPModel.getDefaultView() == JAPConstants.VIEW_NORMAL));
			}

			public boolean isSkippedAsPreviousContentPane()
			{
				return true;
			}
		};
		paneRestart.addComponentListener(new ComponentAdapter()
		{
			public void componentShown(ComponentEvent a_event)
			{
				// reset locale now after the finish button has been updated
				JAPMessages.init(locale, JAPConstants.MESSAGESFN);
			}
		});

		
		
		
			
		
		DialogContentPane paneBrowserConf = new SimpleWizardContentPane(
				  this, JAPMessages.getString(MSG_BROWSER_CONF), layout, new DialogContentPaneOptions(paneRestart))
		{
			public CheckError[] checkUpdate()
			{
				for (int i = 0; i < m_lblPorts.length; i++)
				{
					m_lblPorts[i].setText("" + JAPModel.getHttpListenerPortNumber());
				}

				return super.checkUpdate();
			}
			public boolean isMoveForwardAllowed()
			{
				return paneRestart.isSkippedAsNextContentPane();
			}
		};
		contentPane = paneBrowserConf.getContentPane();
		contentPane.setLayout(new GridBagLayout());
		constraints = new GridBagConstraints();
		constraints.gridx = 1;
		constraints.gridy = 0;
		constraints.anchor = GridBagConstraints.WEST;
		constraints.insets = insets;

		JTextComponentToClipboardCopier textCopier = new JTextComponentToClipboardCopier(false);


		constraints.gridy = 0;
		constraints.fill = GridBagConstraints.HORIZONTAL;
		for (int i = 0; i < PROXIES.length; i++)
		{
			constraints.gridy++;
			addProxyInfo(contentPane, constraints, PROXIES[i]);
			constraints.gridy++;
			constraints.gridx = 5;
			m_lblHostnames[i] = GUIUtils.createSelectableAndResizeableLabel(contentPane);
			m_lblHostnames[i].setText("localhost");
			textCopier.registerTextComponent(m_lblHostnames[i]);
			m_lblHostnames[i].setBackground(Color.white);
			contentPane.add(m_lblHostnames[i], constraints);

			constraints.gridx++;
			tempLabel = new JLabel(":");
			contentPane.add(tempLabel, constraints);

			constraints.gridx++;
			m_lblPorts[i]= GUIUtils.createSelectableAndResizeableLabel(contentPane);
			m_lblPorts[i].setText("" + 65535);
			textCopier.registerTextComponent(m_lblPorts[i]);
			m_lblPorts[i].setBackground(Color.white);
			contentPane.add(m_lblPorts[i], constraints);
			constraints.gridy++;
		}

		constraints.gridy = 0;
		addBrowserInstallationInfo(contentPane, constraints, BROWSER_JONDOFOX, "jondofox", true);
		addBrowserInstallationInfo(contentPane, constraints, BROWSER_FIREFOX, "jondofox", false);
		addBrowserInstallationInfo(contentPane, constraints,
								   JAPMessages.getString(MSG_OTHER_BROWSERS), "browser", false);


		DialogContentPane paneBrowserTest = new SimpleWizardContentPane(
			  this, JAPMessages.getString(MSG_BROWSER_TEST,
										  JAPMessages.getString(
			JAPDialog.LinkedCheckBox.MSG_REMEMBER_ANSWER)), layout,
			  new DialogContentPaneOptions(paneBrowserConf))
		{
			public CheckError[] checkYesOK()
			{
				CheckError[] errors = super.checkYesOK();
				if (m_groupWarning.getSelection() == null)
				{
					return new CheckError[]{new CheckError(
					   JAPMessages.getString(MSG_MAKE_SELECTION), LogType.GUI)};
				}

				return errors;
			}
			public boolean isSkippedAsPreviousContentPane()
			{
				return m_groupWarning.getSelection() != null &&
					(m_radioNoWarning.isSelected());
			}
		};
		contentPane = paneBrowserTest.getContentPane();
		contentPane.setLayout(new GridBagLayout());
		constraints = new GridBagConstraints();
		constraints.gridx = 0;
		constraints.gridy = 0;
		constraints.anchor = GridBagConstraints.WEST;
		m_radioNoWarning = new JRadioButton(JAPMessages.getString(MSG_ERROR_NO_WARNING));
		contentPane.add(m_radioNoWarning, constraints);
		m_radioErrorWarningNoSurfing = new JRadioButton(JAPMessages.getString(MSG_ERROR_WARNING_NO_SURFING));
		constraints.gridy++;
		contentPane.add(m_radioErrorWarningNoSurfing, constraints);
		m_radioWarningInBrowser = new JRadioButton(JAPMessages.getString(MSG_ERROR_WARNING_IN_BROWSER));
		constraints.gridy++;
		contentPane.add(m_radioWarningInBrowser, constraints);
		m_radioSuccessWarning = new JRadioButton(JAPMessages.getString(MSG_SUCCESS_WARNING));
		m_radioSuccessWarning.setForeground(new Color(0, 160, 0));
		constraints.gridy++;
		contentPane.add(m_radioSuccessWarning, constraints);
		m_groupWarning = new ButtonGroup();
		m_groupWarning.add(m_radioNoWarning);
		m_groupWarning.add(m_radioErrorWarningNoSurfing);
		m_groupWarning.add(m_radioWarningInBrowser);
		m_groupWarning.add(m_radioSuccessWarning);


		DialogContentPane paneBrowserTestNoWarning = new SimpleWizardContentPane(
			  this, JAPMessages.getString(MSG_EXPLAIN_NO_WARNING), layout,
			  new DialogContentPaneOptions(paneBrowserTest))
		{
			public boolean isSkippedAsNextContentPane()
			{
				return m_groupWarning.getSelection() != null &&
					!(m_radioNoWarning.isSelected());
			}
			public boolean isSkippedAsPreviousContentPane()
			{
				return true;
			}
		};
		paneBrowserTestNoWarning.setDefaultButtonOperation(
			  DialogContentPane.ON_YESOK_SHOW_PREVIOUS_CONTENT |
			  DialogContentPane.ON_CANCEL_DISPOSE_DIALOG |
			  DialogContentPane.ON_NO_SHOW_PREVIOUS_CONTENT);


		DialogContentPane paneExplainWarningInBrowser = new SimpleWizardContentPane(
			this, JAPMessages.getString(MSG_EXPLAIN_WARNING_IN_BROWSER, new Object[]
										{
										JAPMessages.getString("ngBttnAnonDetails"),
										JAPMessages.getString("settingsInfoServiceConfigAdvancedSettingsTabTitle"),
										JAPMessages.getString(
		  JAPConfAnonGeneral.MSG_DENY_NON_ANONYMOUS_SURFING)}), layout,
			new DialogContentPaneOptions(paneBrowserTestNoWarning))
		{

			public boolean isSkippedAsNextContentPane()
			{
				return m_groupWarning.getSelection() != null && !m_radioWarningInBrowser.isSelected();
			}

			public boolean isSkippedAsPreviousContentPane()
			{
				return true;
			}
		};
		paneExplainWarningInBrowser.setDefaultButtonOperation(
			DialogContentPane.ON_YESOK_SHOW_PREVIOUS_CONTENT |
			DialogContentPane.ON_CANCEL_DISPOSE_DIALOG |
			DialogContentPane.ON_NO_SHOW_PREVIOUS_CONTENT);


		DialogContentPane paneExplainNoDirectConnection = new SimpleWizardContentPane(
			  this, JAPMessages.getString(MSG_EXPLAIN_NO_DIRECT_CONNECTION)  + " " +
			  JAPMessages.getString(JAPConf.MSG_READ_PANEL_HELP, new Object[]{
									JAPMessages.getString("confButton"),
									JAPMessages.getString("ngTreeNetwork")}) + "<br><br>" +
									JAPMessages.getString(MSG_EXPLAIN_FIREWALL), layout,
			  new DialogContentPaneOptions(paneExplainWarningInBrowser))
		{

			public boolean isSkippedAsNextContentPane()
			{
				return m_groupWarning.getSelection() != null && !m_radioErrorWarningNoSurfing.isSelected();
			}

			public boolean isSkippedAsPreviousContentPane()
			{
				return true;
			}
		};
		paneExplainNoDirectConnection.setDefaultButtonOperation(
			DialogContentPane.ON_YESOK_SHOW_PREVIOUS_CONTENT |
			DialogContentPane.ON_CANCEL_DISPOSE_DIALOG |
			DialogContentPane.ON_NO_SHOW_PREVIOUS_CONTENT);



		DialogContentPane paneAnonTest = new SimpleWizardContentPane(
			  this, JAPMessages.getString(MSG_ANON_TEST), layout,
			  new DialogContentPaneOptions(paneExplainNoDirectConnection))
		{
			public CheckError[] checkYesOK()
			{
				CheckError[] errors = super.checkYesOK();
				if (m_groupAnon.getSelection() == null)
				{
					return new CheckError[]{new CheckError(
					   JAPMessages.getString(MSG_MAKE_SELECTION), LogType.GUI)};
				}

				return errors;
			}
		};
		contentPane = paneAnonTest.getContentPane();
		contentPane.setLayout(new GridBagLayout());
		constraints = new GridBagConstraints();
		constraints.gridx = 0;
		constraints.gridy = 0;
		constraints.anchor = GridBagConstraints.WEST;
		m_radioNoServiceAvailable = new JRadioButton(JAPMessages.getString(MSG_ERROR_NO_SERVICE_AVAILABLE));
		contentPane.add(m_radioNoServiceAvailable, constraints);
		m_radioNoConnection = new JRadioButton(JAPMessages.getString(MSG_ERROR_NO_CONNECTION));
		constraints.gridy++;
		contentPane.add(m_radioNoConnection, constraints);
		m_noSurfing = new JRadioButton(JAPMessages.getString(MSG_ERROR_NO_SURFING));
		constraints.gridy++;
		contentPane.add(m_noSurfing, constraints);
		m_radioConnectionSlow =
			new JRadioButton(JAPMessages.getString(MSG_ERROR_CONNECTION_SLOW));
		constraints.gridy++;
		contentPane.add(m_radioConnectionSlow, constraints);
		m_ConnectionOK = new JRadioButton(JAPMessages.getString(MSG_SUCCESS_CONNECTION));
		m_ConnectionOK.setForeground(new Color(0, 160, 0));
		constraints.gridy++;
		contentPane.add(m_ConnectionOK, constraints);

		m_groupAnon = new ButtonGroup();
		m_groupAnon.add(m_radioNoServiceAvailable);
		m_groupAnon.add(m_radioNoConnection);
		m_groupAnon.add(m_noSurfing);
		m_groupAnon.add(m_radioConnectionSlow);
		m_groupAnon.add(m_ConnectionOK);

		DialogContentPane paneExplainNoServiceAvailable = new SimpleWizardContentPane(
			  this, JAPMessages.getString(MSG_EXPLAIN_FIREWALL) + "<br><br>" +
			  JAPMessages.getString(MSG_EXPLAIN_NO_SERVICE_AVAILABLE, new Object[]{
										  JAPMessages.getString("ngBttnAnonDetails"),
										  JAPMessages.getString("ngAnonGeneralPanelTitle"),
										  JAPMessages.getString(JAPConfAnonGeneral.MSG_CONNECTION_TIMEOUT),
										  JAPMessages.getString("ngSettingsViewNormal"),
										  JAPMessages.getString("confButton"),
										  JAPMessages.getString("ngTreeNetwork")}) 
										  ,
			  layout, new DialogContentPaneOptions(paneAnonTest))
		{
			public boolean isSkippedAsNextContentPane()
			{
				return m_groupAnon.getSelection() != null && !m_radioNoServiceAvailable.isSelected();
			}

			public boolean isSkippedAsPreviousContentPane()
			{
				return true;
			}
		};
		paneExplainNoServiceAvailable.setDefaultButtonOperation(
			  DialogContentPane.ON_YESOK_SHOW_PREVIOUS_CONTENT |
			  DialogContentPane.ON_CANCEL_DISPOSE_DIALOG |
			  DialogContentPane.ON_NO_SHOW_PREVIOUS_CONTENT);



		DialogContentPane paneExplainNoConnection = new SimpleWizardContentPane(
			  this, JAPMessages.getString(MSG_EXPLAIN_NO_CONNECTION) + " " + 
			  JAPMessages.getString(MSG_EXPLAIN_CHOOSE_OTHER_SERVICE, new String[]{
					  JAPMessages.getString(JAPNewView.MSG_SERVICE_NAME),
					  JAPMessages.getString("ngAnonymitaet"), JAPMessages.getString("ngAnonOn") 
			  }), layout,
			  new DialogContentPaneOptions(paneExplainNoServiceAvailable))
		{
			public CheckError[] checkUpdate()
			{
				m_radioNoServiceAvailable.setVisible(true);
				return super.checkUpdate();
			}

			public boolean isSkippedAsNextContentPane()
			{
				return m_groupAnon.getSelection() != null && !m_radioNoConnection.isSelected();
			}

			public boolean isSkippedAsPreviousContentPane()
			{
				return true;
			}
		};
		paneExplainNoConnection.setDefaultButtonOperation(
			  DialogContentPane.ON_YESOK_SHOW_PREVIOUS_CONTENT |
			  DialogContentPane.ON_CANCEL_DISPOSE_DIALOG |
			  DialogContentPane.ON_NO_SHOW_PREVIOUS_CONTENT);
		lblImage = new JLabel(GUIUtils.loadImageIcon(IMG_SERVICES));
		lblImage.setBorder(border);
		paneExplainNoConnection.getContentPane().add(lblImage);


		DialogContentPane paneExplainBadConnection = new SimpleWizardContentPane(
			  this, JAPMessages.getString(MSG_EXPLAIN_BAD_CONNECTION) + " " +
			  JAPMessages.getString(MSG_EXPLAIN_CHOOSE_OTHER_SERVICE, new String[]{
					  JAPMessages.getString(JAPNewView.MSG_SERVICE_NAME),
					  JAPMessages.getString("ngAnonymitaet"), JAPMessages.getString("ngAnonOn") 
			  }), layout,
			  new DialogContentPaneOptions(paneExplainNoConnection))
		{

			public boolean isSkippedAsNextContentPane()
			{
				return m_groupAnon.getSelection() != null &&
					! (m_noSurfing.isSelected() || m_radioConnectionSlow.isSelected());
			}

			public boolean isSkippedAsPreviousContentPane()
			{
				return true;
			}
		};
		paneExplainBadConnection.setDefaultButtonOperation(
			DialogContentPane.ON_YESOK_SHOW_PREVIOUS_CONTENT |
			DialogContentPane.ON_CANCEL_DISPOSE_DIALOG |
			DialogContentPane.ON_NO_SHOW_PREVIOUS_CONTENT);
		lblImage = new JLabel(GUIUtils.loadImageIcon(IMG_SERVICES));
		lblImage.setBorder(border);
		paneExplainBadConnection.getContentPane().add(lblImage);


		final DialogContentPane paneFinish = new SimpleWizardContentPane(
			this, JAPMessages.getString(MSG_FINISHED), layout,
			new DialogContentPaneOptions(paneExplainBadConnection));
		contentPane = paneFinish.getContentPane();
		contentPane.setLayout(new GridBagLayout());
		constraints = new GridBagConstraints();
		constraints.gridx = 0;
		constraints.gridy = -1;
		addBrowserInstallationInfo(contentPane, constraints, 
				JAPMessages.getString(MSG_FINISHED_ANONTEST), "security_test", false);
		addBrowserInstallationInfo(contentPane, constraints, BROWSER_JONDOFOX, "jondofox", false);
		addBrowserInstallationInfo(contentPane, constraints, 
				JAPMessages.getString(MSG_FINISHED_TROUBLESHOOTING), "trouble", false);

		paneFinish.getButtonCancel().setVisible(false);


		// prevent premature closing of the wizard
		setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
		addWindowListener(new WindowAdapter()
		{
			public void windowClosing(WindowEvent a_event)
			{
				boolean bClose = true;
				if (!paneFinish.isVisible())
				{
					bClose = (JAPDialog.showConfirmDialog(thisDialog, JAPMessages.getString(MSG_REALLY_CLOSE),
						OPTION_TYPE_OK_CANCEL, MESSAGE_TYPE_QUESTION) == RETURN_VALUE_OK);
				}
				else
				{
					m_bFinished = true;
					//paneFinish.setButtonValue(DialogContentPane.DEFAULT_BUTTON_OK); // does not work...
				}
				if (bClose)
				{
					dispose();
				}
			}
			public void windowClosed(WindowEvent a_event)
			{
				if (paneRestart.getButtonValue() == DialogContentPane.RETURN_VALUE_OK)
				{
					JAPMessages.setLocale(((LanguageMapper)comboLang.getSelectedItem()).getLocale());
					if (m_radioSimpleView.isSelected())
					{
						JAPModel.getInstance().setDefaultView(JAPConstants.VIEW_SIMPLIFIED);
					}
					else if (m_radioAdvancedView.isSelected())
					{
						JAPModel.getInstance().setDefaultView(JAPConstants.VIEW_NORMAL);
					}
					
					JAPController.goodBye(false);
				}
				else
				{
					JAPController.getInstance().setConfigAssistantShown();
				}
			}
		});
		doClosingOnContentPaneCancel(true);

		DialogContentPane.updateDialogOptimalSized(paneWelcome);
		setResizable(false);
		m_radioNoServiceAvailable.setVisible(false);
	}

	private void addProxyInfo(JComponent a_component, GridBagConstraints a_constraints, String a_protocol)
	{
		JLabel tempLabel;
		a_constraints.gridx = 5;
		a_constraints.gridwidth = 1;
		a_constraints.anchor = GridBagConstraints.EAST;
		tempLabel = new JLabel(a_protocol + " Hostname");
		a_component.add(tempLabel, a_constraints);
		tempLabel = new JLabel(":");
		a_constraints.gridx++;
		a_component.add(tempLabel, a_constraints);
		tempLabel = new JLabel(a_protocol + " Port");
		a_constraints.gridx++;
		a_component.add(tempLabel, a_constraints);
}

	private void addBrowserInstallationInfo(JComponent a_component, GridBagConstraints a_constraints,
											String a_browserName, String a_helpContext, boolean a_bRecommended)
	{
		JLabel tempLabel;

		a_constraints.gridx = 0;
		a_constraints.gridy++;
		a_constraints.gridwidth = 1;
		a_constraints.anchor = GridBagConstraints.WEST;
		tempLabel = new JLabel(GUIUtils.loadImageIcon(IMG_ARROW));
		a_component.add(tempLabel, a_constraints);

		a_constraints.gridwidth = 4;
		a_constraints.gridx = 1;
		if (a_bRecommended)
		{
			tempLabel = new JLabel(a_browserName + " (" + JAPMessages.getString(MSG_RECOMMENDED) + ")");
		}
		else
		{
			tempLabel = new JLabel(a_browserName);
		}

		registerLink(tempLabel, a_helpContext, true);
		a_component.add(tempLabel, a_constraints);
		tempLabel = new JLabel();
		a_constraints.weightx = 1.0;
		a_constraints.fill = GridBagConstraints.HORIZONTAL;
		a_component.add(tempLabel, a_constraints);
		a_constraints.weightx = 0.0;

	}

	private void registerLink(JLabel a_label, final String a_context, final boolean a_bHelpContext)
	{
		a_label.setForeground(Color.blue);
		if (a_bHelpContext)
		{
			a_label.setToolTipText(JAPMessages.getString(MSG_CLICK_TO_VIEW_HELP));
		}
		else
		{
			a_label.setToolTipText(a_context);
		}
		a_label.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		a_label.addMouseListener(new MouseAdapter()
		{
			public void mouseClicked(MouseEvent e)
			{
				if (a_bHelpContext)
				{
					JAPHelp.getInstance().setContext(
							JAPHelpContext.createHelpContext(a_context,ConfigAssistant.this.getContentPane()));
					JAPHelp.getInstance().setVisible(true);
				}
				else
				{
					// interpret as URL
					try
					{
						AbstractOS.getInstance().openURL(new URL(a_context));
					}
					catch (MalformedURLException ex)
					{
						// ignore
					}
				}
			}
		});
	}

}
