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
package jap.pay;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.SecureRandom;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Enumeration;
import java.util.GregorianCalendar;
import java.util.Hashtable;
import java.util.Locale;
import java.util.Observable;
import java.util.Observer;
import java.util.Vector;

import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTabbedPane;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.border.TitledBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.filechooser.FileFilter;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import HTTPClient.ForbiddenIOException;
import anon.crypto.DSAKeyPair;
import anon.crypto.XMLEncryption;
import anon.infoservice.IMutableProxyInterface;
import anon.infoservice.MixCascade;
import anon.pay.BIConnection;
import anon.pay.IPaymentListener;
import anon.pay.PayAccount;
import anon.pay.PayAccountsFile;
import anon.pay.PaymentInstanceDBEntry;
import anon.pay.xml.XMLAccountInfo;
import anon.pay.xml.XMLBalance;
import anon.pay.xml.XMLErrorMessage;
import anon.pay.xml.XMLGenericStrings;
import anon.pay.xml.XMLGenericText;
import anon.pay.xml.XMLPassivePayment;
import anon.pay.xml.XMLPaymentOption;
import anon.pay.xml.XMLPaymentOptions;
import anon.pay.xml.XMLTransCert;
import anon.pay.xml.XMLTransactionOverview;
import anon.pay.xml.XMLVolumePlan;
import anon.pay.xml.XMLVolumePlans;
import anon.util.SingleStringPasswordReader;
import anon.util.XMLUtil;
import anon.util.captcha.ICaptchaSender;
import anon.util.captcha.IImageEncodedCaptcha;
import gui.GUIUtils;
import gui.JAPMessages;
import gui.dialog.CaptchaContentPane;
import gui.dialog.DialogContentPane;
import gui.dialog.DialogContentPane.Options;
import gui.dialog.IDialogOptions;
import gui.dialog.JAPDialog;
import gui.dialog.PasswordContentPane;
import gui.dialog.SimpleWizardContentPane;
import gui.dialog.WorkerContentPane;
import jap.AbstractJAPConfModule;
import jap.JAPConfInfoService;
import jap.JAPConstants;
import jap.JAPController;
import jap.JAPControllerMessage;
import jap.JAPModel;
import jap.JAPUtil;
import jap.pay.wizardnew.CancellationPolicyPane;
import jap.pay.wizardnew.JpiSelectionPane;
import jap.pay.wizardnew.MethodSelectionPane;
import jap.pay.wizardnew.PassivePaymentPane;
import jap.pay.wizardnew.PaymentInfoPane;
import jap.pay.wizardnew.TermsAndConditionsPane;
import jap.pay.wizardnew.VolumePlanSelectionPane;
import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;

/**
 * The Jap Conf Module (Settings Tab Page) for the Accounts and payment Management
 * also contains the setup for the account creation/charging wizard
 *
 * @author Bastian Voigt, Tobias Bayer, Elmar Schraml
 * @version 1.0
 */
public class AccountSettingsPanel extends AbstractJAPConfModule implements
	ListSelectionListener, Observer, IPaymentListener
{

	protected static final String MSG_ACCOUNT_FLAT_VOLUME = AccountSettingsPanel.class.
		getName() + "_account_flat_volume";

	protected static final String MSG_ACCOUNT_VALID = AccountSettingsPanel.class.
		getName() + "_account_valid";
	protected static final String MSG_PAYMENT_INSTANCE = AccountSettingsPanel.class.
		getName() + "_paymentInstance";

	protected static final String IMG_COINS_DISABLED = AccountSettingsPanel.class.getName() +
		"_coins-disabled.gif";

	/** Messages */
	private static final String MSG_BUTTON_TRANSACTIONS = AccountSettingsPanel.class.
		getName() + "_button_transactions";
	private static final String MSG_BUTTON_DELETE = AccountSettingsPanel.class.
		getName() + "_button_delete";
	private static final String MSG_BTN_CREATE = AccountSettingsPanel.class.getName() + "_btnCreate";
	private static final String MSG_BUTTON_EXPORT = AccountSettingsPanel.class.
		getName() + "_button_export";
	private static final String MSG_BUTTONRELOAD = AccountSettingsPanel.class.
		getName() + "_buttonreload";
	private static final String MSG_TRANSACTION_OVERVIEW_DIALOG = AccountSettingsPanel.class.
		getName() + "_transaction_overview_dialog";
	private static final String MSG_ACCOUNT_SPENT = AccountSettingsPanel.class.
		getName() + "_account_spent";
	private static final String MSG_ACCOUNT_DEPOSIT = AccountSettingsPanel.class.
		getName() + "_account_deposit";
	private static final String MSG_ACCOUNT_BALANCE = AccountSettingsPanel.class.
		getName() + "_account_balance";
	private static final String MSG_ACCOUNT_FLAT_ENDDATE = AccountSettingsPanel.class.
		getName() + "_account_flat_enddate";
	private static final String MSG_ACCOUNT_NOFLAT = AccountSettingsPanel.class.
		getName() + "_account_noflat";
	private static final String MSG_ACCOUNT_DETAILS = AccountSettingsPanel.class.
		getName() + "_account_details";
	private static final String MSG_ACCOUNT_CREATION_DATE = AccountSettingsPanel.class.
		getName() + "_account_creation_date";
	private static final String MSG_ACCOUNT_STATEMENT_DATE = AccountSettingsPanel.class.
		getName() + "_account_statement_date";
	private static final String MSG_BUTTON_CHARGE = AccountSettingsPanel.class.
		getName() + "_button_charge";
	private static final String MSG_BUTTON_BUYFLAT = AccountSettingsPanel.class.
		getName() + "_button_buyflat";
	private static final String MSG_FLATTITLE = AccountSettingsPanel.class.
		getName() + "_flat_title";
	private static final String MSG_BUTTON_SELECT = AccountSettingsPanel.class.
		getName() + "_button_select";
	private static final String MSG_BUTTON_CHANGE_PASSWORD = AccountSettingsPanel.class.
		getName() + "_button_change_password";
	private static final String MSG_ACCOUNT_INVALID = AccountSettingsPanel.class.
		getName() + "_account_invalid";
	private static final String MSG_ACCOUNTCREATE = AccountSettingsPanel.class.
		getName() + "_accountcreate";
	private static final String MSG_CREATEERROR = AccountSettingsPanel.class.getName() +
		"_createerror";
	private static final String MSG_DIRECT_CONNECTION_FORBIDDEN = AccountSettingsPanel.class.getName() +
		"_directConnectionForbidden";
	private static final String MSG_NO_ANONYMITY_POSSIBLY_BLOCKED = AccountSettingsPanel.class.getName() +
		"_noAnonymityPossiblyBlocked";
	private static final String MSG_ERROR_FORBIDDEN = AccountSettingsPanel.class.getName() +
		"_errorForbidden";
	private static final String MSG_GETACCOUNTSTATEMENT = AccountSettingsPanel.class.
		getName() + "_getaccountstatement";
	private static final String MSG_GETACCOUNTSTATEMENTTITLE = AccountSettingsPanel.class.
		getName() + "_getaccountstatementtitle";
	private static final String MSG_ACCOUNTCREATEDESC = AccountSettingsPanel.class.
		getName() + "_accountcreatedesc";
	private static final String MSG_ACCPASSWORDTITLE = AccountSettingsPanel.class.
		getName() + "_accpasswordtitle";
	private static final String MSG_EXPORTENCRYPT = AccountSettingsPanel.class.
		getName() + "_exportencrypt";
	private static final String MSG_ACCPASSWORD = AccountSettingsPanel.class.
		getName() + "_accpassword";
	private static final String MSG_OLDSTATEMENT = AccountSettingsPanel.class.
		getName() + "_oldstatement";
	private static final String MSG_EXPORTED = AccountSettingsPanel.class.
		getName() + "_exported";
	private static final String MSG_ENCRYPT_ACCOUNTS = AccountSettingsPanel.class.getName() +
		"_encryptAccounts";
	private static final String MSG_NOTEXPORTED = AccountSettingsPanel.class.
		getName() + "_notexported";
	private static final String MSG_CONNECTIONACTIVE = AccountSettingsPanel.class.
		getName() + "_connectionactive";
	private static final String MSG_CONNECTIONACTIVE_QUESTION = AccountSettingsPanel.class.
		getName() + "_connectionActiveQuestion";
	private static final String MSG_FETCHINGOPTIONS = AccountSettingsPanel.class.
		getName() + "_fetchingoptions";
	private static final String MSG_FETCHINGPLANS = AccountSettingsPanel.class.
		getName() + "_fetchingplans";
	private static final String MSG_FETCHINGTERMS = AccountSettingsPanel.class.
		getName() + "_fetchingterms";
	private static final String MSG_FETCHINGPOLICY = AccountSettingsPanel.class.
		getName() + "_fetchingpolicy";
	private static final String MSG_FETCHINGTAN = AccountSettingsPanel.class.
		getName() + "_fetchingtan";
	private static final String MSG_CHARGEWELCOME = AccountSettingsPanel.class.
		getName() + "_chargewelcome";
	private static final String MSG_CHARGETITLE = AccountSettingsPanel.class.
		getName() + "_chargetitle";
	private static final String MSG_SENDINGPASSIVE = AccountSettingsPanel.class.
		getName() + "_sendingpassive";
	private static final String MSG_SENTPASSIVE = AccountSettingsPanel.class.
		getName() + "_sentpassive";
	private static final String MSG_NOTSENTPASSIVE = AccountSettingsPanel.class.
		getName() + "_notsentpassive";
	private static final String MSG_NEWCAPTCHA = AccountSettingsPanel.class.
		getName() + "_newcaptcha";
	private static final String MSG_NEWCAPTCHAEASTEREGG = AccountSettingsPanel.class.
		getName() + "_newcaptchaEasterEgg";
	private static final String MSG_SHOW_PAYMENT_CONFIRM_DIALOG = AccountSettingsPanel.class.
		getName() + "_showPaymentConfirmDialog";
	private static final String MSG_TEST_PI_CONNECTION = AccountSettingsPanel.class.
		getName() + "_testingPIConnection";
	private static final String MSG_CREATE_KEY_PAIR = AccountSettingsPanel.class.getName() +
		"_creatingKeyPair";
	private static final String MSG_KEY_PAIR_CREATE_ERROR = AccountSettingsPanel.class.getName() +
		"_keyPairCreateError";
	private static final String MSG_FETCHING_BIS = AccountSettingsPanel.class.getName() +
		"_fetchingBIs";
	private static final String MSG_SAVE_CONFIG = AccountSettingsPanel.class.getName() +
		"_savingConfig";
	private static final String MSG_CREATED_ACCOUNT_NOT_SAVED = AccountSettingsPanel.class.getName() +
		"_createdAccountNotSaved";
	private static final String MSG_ACCOUNT_IMPORT_FAILED = AccountSettingsPanel.class.getName() +
		"_accountImportFailed";
	private static final String MSG_ACCOUNT_ALREADY_EXISTING = AccountSettingsPanel.class.getName() +
		"_accountAlreadyExisting";
	private static final String MSG_ALLOW_DIRECT_CONNECTION = AccountSettingsPanel.class.getName() +
		"_allowDirectConnection";
	private static final String MSG_BI_CONNECTION_LOST = AccountSettingsPanel.class.getName() +
		"_biConnectionLost";
	private static final String MSG_BUTTON_UNLOCK = AccountSettingsPanel.class.getName() +
		"_unlockAccount";
	private static final String MSG_BUTTON_ACTIVATE = AccountSettingsPanel.class.getName() +
		"_activateAccount";
	private static final String MSG_BUTTON_DEACTIVATE = AccountSettingsPanel.class.getName() +
		"_deactivateAccount";
	private static final String MSG_ERROR_DELETING = AccountSettingsPanel.class.getName() +
		"_errorDeletingAccount";
	private static final String MSG_ACCOUNT_DISABLED = AccountSettingsPanel.class.getName() +
		"_accountDisabled";
	private static final String MSG_GIVE_ACCOUNT_PASSWORD = AccountSettingsPanel.class.getName() +
		"_giveAccountPassword";
	private static final String MSG_ACTIVATION_SUCCESSFUL = AccountSettingsPanel.class.getName() +
		"_activationSuccessful";
	private static final String MSG_ACTIVATION_FAILED = AccountSettingsPanel.class.getName() +
		"_activationFailed";
	private static final String MSG_SHOW_AI_ERRORS = AccountSettingsPanel.class.getName() +
		"_showAIErrors";
	private static final String MSG_BALANCE_AUTO_UPDATE_ENABLED = AccountSettingsPanel.class.getName() +
		"_balanceAutoUpdateEnabled";
	private static final String MSG_NO_BACKUP = AccountSettingsPanel.class.getName() + "_noBackup";
	private static final String MSG_TOOL_TIP_NO_BACKUP =
		AccountSettingsPanel.class.getName() + "_toolTipNoBackup";
	private static final String MSG_TOOL_TIP_ACTIVATE =
		AccountSettingsPanel.class.getName() + "_toolTipActivate";
	private static final String MSG_TOOL_TIP_EXPIRED =
		AccountSettingsPanel.class.getName() + "_toolTipExpired";
	private static final String MSG_PASSWORD_EXPORT =
		AccountSettingsPanel.class.getName() + "_passwordExport";
	private static final String MSG_ASK_IF_NOT_SAVED =
		AccountSettingsPanel.class.getName() + "_askIfNotSaved";
	private static final String MSG_NEW_CAPTCHA_HINT =
		AccountSettingsPanel.class.getName() + "_newCaptchaHint";


	public static final String MSG_SHOW_TRANSACTION_DETAILS =
		AccountSettingsPanel.class.getName() + "_showTransactionDetails";
	public static final String MSG_NO_TRANSACTION =
		AccountSettingsPanel.class.getName() + "_noTransaction";
	public static final String MSG_EXPIRED =
		AccountSettingsPanel.class.getName() + "_expired";
	public static final String MSG_NO_CREDIT =
		AccountSettingsPanel.class.getName() + "_noCredit";
	private static final String MSG_TERMS_AND_COND_DESC =
		AccountSettingsPanel.class.getName() + "_termsAndConditionsDescription";
	private static final String MSG_TERMS_AND_COND =
		AccountSettingsPanel.class.getName() + "_termsAndConditions";
	private static final String MSG_TERMS_AND_COND_HINT =
		AccountSettingsPanel.class.getName() + "_termsAndConditionsHint";




	private static final String MSG_BACKUP_WARNING = AccountSettingsPanel.class.getName() + "_backupwarning";
	private static final String MSG_ACTIVE_COMPLETE = AccountSettingsPanel.class.getName() + "_activecomplete";
	private static final String MSG_MIXED_COMPLETE = AccountSettingsPanel.class.getName() + "_mixedcomplete";
	private static final String MSG_COUPON_SENT = AccountSettingsPanel.class.getName() + "_couponsent";
	private static final String MSG_COUPON_FAILED = AccountSettingsPanel.class.getName() + "_couponfailed";
    private static final String MSG_COUPON = AccountSettingsPanel.class.getName() + "_coupon";
	private static final String MSG_FILE_EXISTS = AccountSettingsPanel.class.getName() + "_fileExists";



	private static final Integer[] CONNECT_TIMEOUTS =
		new Integer[]
		{
		new Integer(10), new Integer(20), new Integer(30), new Integer(40), new Integer(50),
		new Integer(60), new Integer(80), new Integer(100)};

	private JButton m_btnCreateAccount;
	private JButton m_btnChargeAccount;
	private JButton m_btnDeleteAccount;
	private JButton m_btnExportAccount;
	private JButton m_btnImportAccount;
	private JButton m_btnTransactions;
	private JButton m_btnSelect;
	private JButton m_btnPassword;
	private JButton m_btnReload;
	private JButton m_btnActivate;
	private JCheckBox m_cbxShowPaymentConfirmation;
	private JCheckBox m_cbxAllowNonAnonymousConnection;
	private JCheckBox m_cbxShowAIErrors;
	private JCheckBox m_cbxBalanceAutoUpdateEnabled;
	private JCheckBox m_cbxAskIfNotSaved;

	private JLabel m_paymentInstance;
	private JLabel m_labelTermsAndConditions;
	private JLabel m_labelCreationDate;
	private JLabel m_labelStatementDate;
	private JLabel m_labelDeposit;
	private JLabel m_labelSpent;
	private JLabel m_labelValid;
	private JLabel m_labelVolume;
	private JLabel m_lblInactiveMessage, m_lblNoBackupMessage;
	private JProgressBar m_coinstack;
	private JList m_listAccounts;
	private JComboBox m_comboTimeout;
	private JPanel m_tabBasicSettings, m_tabAdvancedSettings;
	private boolean m_bReady = true;
	private boolean m_bDoNotCloseDialog = false;

	private MyActionListener myActionListener;

	/**The TabbedPane Component*/
	private JTabbedPane m_tabPane;

	public AccountSettingsPanel()
	{
		super(null);
		PayAccountsFile.getInstance().addPaymentListener(this);
		JAPController.getInstance().addObserver(this);
	}


	public boolean accountCertRequested(MixCascade a_connectedCascade)
	{
		return true;
	}


		public void accountError(XMLErrorMessage msg, boolean a_bIgnore)
		{
		}

		public void accountActivated(PayAccount acc)
		{
			updateAccountList();
		}

		public void accountRemoved(PayAccount acc)
		{
		}

		public void accountAdded(PayAccount acc)
		{
		}

		/**
		 * The credit changed for the given account.
		 * @param acc PayAccount
		 */
		public void creditChanged(PayAccount acc)
		{
		}


		public void gotCaptcha(ICaptchaSender a_source, IImageEncodedCaptcha a_captcha)
		{
		}


	public void fontSizeChanged(JAPModel.FontResize a_fontSize, JLabel a_dummyLabel)
	{
		m_coinstack.setUI(new CoinstackProgressBarUI(GUIUtils.loadImageIcon(JAPConstants.
			IMAGE_COIN_COINSTACK, true), 0, 8));
	}

	public void update(Observable a_observable, Object a_arg)
	{
		if (a_observable instanceof JAPController &&
			( (JAPControllerMessage) a_arg).getMessageCode() == JAPControllerMessage.ASK_SAVE_PAYMENT_CHANGED)
		{
			m_cbxAskIfNotSaved.setSelected(JAPController.getInstance().isAskSavePayment());
		}
	}

	/**
	 * getTabTitle
	 *
	 * @return String
	 */
	public String getTabTitle()
	{
		return JAPMessages.getString("ngPaymentTabTitle");
	}

	/**
	 * recreateRootPanel - recreates all GUI elements
	 */
	public void recreateRootPanel()
	{
		JPanel rootPanel = getRootPanel();
		/* clear the whole root panel */
		rootPanel.removeAll();
		if (JAPModel.getDefaultView() == JAPConstants.VIEW_SIMPLIFIED)
		{
			rootPanel.setBorder(new TitledBorder(JAPMessages.getString("ngPayment")));
		}
		myActionListener = new MyActionListener();

		/* insert all components in the root panel */
		m_tabPane = new JTabbedPane();
		//tabPane.setFont(getFontSetting());
		m_tabBasicSettings = createBasicSettingsTab();
		m_tabPane.insertTab(JAPMessages.getString("ngPseudonymAccounts"),
							null, m_tabBasicSettings, null, 0);
		m_tabAdvancedSettings = createAdvancedSettingsTab();
		GridBagLayout rootPanelLayout = new GridBagLayout();
		rootPanel.setLayout(rootPanelLayout);
		GridBagConstraints contraints = createTabbedRootPanelContraints();

		if (JAPModel.getDefaultView() != JAPConstants.VIEW_SIMPLIFIED)
		{
			m_tabPane.insertTab(JAPMessages.getString(
				"settingsInfoServiceConfigAdvancedSettingsTabTitle"), null, m_tabAdvancedSettings, null, 1);
			rootPanel.add(m_tabPane, contraints);
		}
		else
		{
			contraints.weightx = 0;
			contraints.weighty = 0;
			rootPanel.add(m_tabBasicSettings, contraints);
			contraints.weightx = 1;
			contraints.weighty = 1;
			rootPanel.add(new JLabel(), contraints);
		}
	}

	private JPanel createBasicSettingsTab()
	{
		JPanel rootPanel = new JPanel();

		rootPanel.setLayout(new GridBagLayout());

		m_listAccounts = new JList();
		m_listAccounts.setCellRenderer(new CustomRenderer());
		m_listAccounts.addListSelectionListener(this);
		m_listAccounts.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		//m_listAccounts.setPreferredSize(new Dimension(200, 200));
		m_listAccounts.addMouseListener(new MouseAdapter()
		{
			public void mouseClicked(MouseEvent e)
			{
				//Activate account on double click
				if (e.getClickCount() == 2)
				{
					doSelectAccount(getSelectedAccount());
				}
			}
		}
		);

		JPanel buttonsPanel = new JPanel(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.fill = c.HORIZONTAL;
		c.anchor = c.NORTHWEST;
		c.weightx = 0;
		c.weighty = 0;
		c.gridx = 0;
		c.gridy = 0;
		c.insets = new Insets(5, 5, 5, 5);

		m_btnCreateAccount = new JButton(JAPMessages.getString(MSG_BTN_CREATE));
		m_btnCreateAccount.addActionListener(myActionListener);
		buttonsPanel.add(m_btnCreateAccount, c);

		c.gridx++;
		m_btnTransactions = new JButton(JAPMessages.getString(MSG_BUTTON_TRANSACTIONS));
		m_btnTransactions.addActionListener(myActionListener);
		buttonsPanel.add(m_btnTransactions, c);

		c.gridx++;
		m_btnPassword = new JButton(JAPMessages.getString(MSG_BUTTON_CHANGE_PASSWORD));
		m_btnPassword.addActionListener(myActionListener);
		buttonsPanel.add(m_btnPassword, c);

		c.gridx++;
		c.weighty = 1;
		m_btnImportAccount = new JButton(JAPMessages.getString("ngImportAccount"));
		m_btnImportAccount.addActionListener(myActionListener);
		buttonsPanel.add(m_btnImportAccount, c);

		c = new GridBagConstraints();
		c.fill = c.BOTH;
		c.anchor = c.NORTHWEST;
		c.weightx = 2.0;
		c.weighty = 1.0;
		c.gridx = 0;
		c.gridy = 0;
		c.gridheight = 6;
		c.insets = new Insets(5, 5, 5, 5);
		JScrollPane scroller = new JScrollPane(m_listAccounts);
		scroller.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		//scroller.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
		rootPanel.add(scroller, c);

		c.gridx++;
		c.fill = c.NONE;
		c.gridheight = 1;
		c.weighty = 0.0;
		c.weightx = 0.0;
		rootPanel.add(new JLabel(JAPMessages.getString(MSG_ACCOUNT_CREATION_DATE)), c);
		c.gridx++;
		c.weightx = 1.0;
		m_labelCreationDate = new JLabel();
		rootPanel.add(m_labelCreationDate, c);

		/*
		c.gridx--;
		c.gridy++;
		c.weightx = 0.0;
		rootPanel.add(new JLabel(JAPMessages.getString(MSG_ACCOUNT_STATEMENT_DATE)), c);
		c.gridx++;*/
		m_labelStatementDate = new JLabel();
		/*
		c.weightx = 1.0;
		rootPanel.add(m_labelStatementDate, c);*/

		c.gridx--;
		c.gridy++;
		c.weightx = 0.0;
		rootPanel.add(new JLabel(JAPMessages.getString(MSG_ACCOUNT_VALID)), c);
		c.gridx++;
		c.weightx = 1.0;
		m_labelValid = new JLabel();
		rootPanel.add(m_labelValid, c);


		c.gridy++;
		c.gridx--;
		c.weightx = 0.0;
		rootPanel.add(new JLabel(JAPMessages.getString(MSG_PAYMENT_INSTANCE) + ":"), c);
		c.gridx++;
		c.weightx = 1.0;
		m_paymentInstance = new JLabel();
		rootPanel.add(m_paymentInstance, c);


		c.gridy++;
		c.gridx--;
		c.gridwidth = 2;
		//rootPanel.add(new JLabel(JAPMessages.getString(MSG_TERMS_AND_COND_DESC) + ":"), c);
		//c.gridy++;
		m_labelTermsAndConditions = new JLabel();
		m_labelTermsAndConditions.setToolTipText(JAPMessages.getString(MSG_TERMS_AND_COND_HINT));

		m_labelTermsAndConditions.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		m_labelTermsAndConditions.setForeground(Color.blue);
		m_labelTermsAndConditions.addMouseListener(myActionListener);
		rootPanel.add(m_labelTermsAndConditions, c);
		c.gridwidth = 1;


		c.gridy++;
		//c.gridx--;
		c.gridwidth = 2;
		c.weightx = 1.0;
		m_lblInactiveMessage = new JLabel();
		m_lblInactiveMessage.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		m_lblInactiveMessage.setForeground(Color.red);
		m_lblInactiveMessage.addMouseListener(new MouseAdapter()
		{
			public void mouseClicked(MouseEvent a_event)
			{
				m_btnActivate.doClick();
			}
		});
		rootPanel.add(m_lblInactiveMessage, c);

		c.gridy++;
		m_lblNoBackupMessage = new JLabel();
		m_lblNoBackupMessage.addMouseListener(new MouseAdapter()
		{
			public void mouseClicked(MouseEvent a_event)
			{
				m_btnExportAccount.doClick();
			}
		});
		m_lblNoBackupMessage.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		m_lblNoBackupMessage.setForeground(Color.red);
		rootPanel.add(m_lblNoBackupMessage, c);

		c.gridy++;
		c.weightx = 0;
		c.gridx = 0;
		c.gridwidth = 3;
		rootPanel.add(buttonsPanel, c);

		c.gridy++;
		JSeparator sep = new JSeparator(JSeparator.HORIZONTAL);
		sep.setPreferredSize(new Dimension(520, 10));
		rootPanel.add(sep, c);

		c.weightx = 1;
		c.weighty = 1;
		c.gridy++;
		c.fill = c.HORIZONTAL;
		rootPanel.add(this.createDetailsPanel(myActionListener), c);

		//updateAccountList(); //would possibly lead to deadlock with AWT-Thread when showing JAPConf
		enableDisableButtons();

		return rootPanel;
	}

	private JPanel createAdvancedSettingsTab()
	{
		JPanel panelAdvanced = new JPanel();

		m_cbxShowPaymentConfirmation = new JCheckBox(JAPMessages.getString(MSG_SHOW_PAYMENT_CONFIRM_DIALOG));
		m_cbxShowPaymentConfirmation.setVisible(false); // not needed any more
		GridBagLayout advancedPanelLayout = new GridBagLayout();
		panelAdvanced.setLayout(advancedPanelLayout);

		GridBagConstraints advancedPanelConstraints = new GridBagConstraints();
		advancedPanelConstraints.anchor = GridBagConstraints.NORTHWEST;
		advancedPanelConstraints.fill = GridBagConstraints.NONE;
		advancedPanelConstraints.weightx = 1.0;
		advancedPanelConstraints.gridwidth = 3;

		advancedPanelConstraints.gridx = 0;
		advancedPanelConstraints.gridy = 0;
		advancedPanelConstraints.insets = new Insets(5, 5, 10, 5);

		panelAdvanced.add(m_cbxShowPaymentConfirmation, advancedPanelConstraints);

		m_cbxAllowNonAnonymousConnection = new JCheckBox(JAPMessages.getString(MSG_ALLOW_DIRECT_CONNECTION));

		advancedPanelConstraints.gridy = 1;
		panelAdvanced.add(m_cbxAllowNonAnonymousConnection, advancedPanelConstraints);

		advancedPanelConstraints.gridy = 2;
		m_cbxShowAIErrors = new JCheckBox(JAPMessages.getString(MSG_SHOW_AI_ERRORS));

		if (JAPConstants.m_bReleasedVersion)
		{
			// this does not work in release version, as it is only meant for debugging purposes
			m_cbxShowAIErrors.setVisible(false);
		}
		panelAdvanced.add(m_cbxShowAIErrors, advancedPanelConstraints);

		advancedPanelConstraints.gridy = 3;
		m_cbxBalanceAutoUpdateEnabled = new JCheckBox(JAPMessages.getString(MSG_BALANCE_AUTO_UPDATE_ENABLED));

		panelAdvanced.add(m_cbxBalanceAutoUpdateEnabled, advancedPanelConstraints);

		advancedPanelConstraints.gridy++;
		m_cbxAskIfNotSaved = new JCheckBox(JAPMessages.getString(MSG_ASK_IF_NOT_SAVED));
		panelAdvanced.add(m_cbxAskIfNotSaved, advancedPanelConstraints);

		advancedPanelConstraints.weightx = 0.0;
		advancedPanelConstraints.gridy++;
		advancedPanelConstraints.gridx = 0;
		advancedPanelConstraints.gridwidth = 1;
		advancedPanelConstraints.anchor = GridBagConstraints.WEST;
		panelAdvanced.add(new JLabel(JAPMessages.getString(JAPConfInfoService.MSG_CONNECT_TIMEOUT) + " (s):"),
						  advancedPanelConstraints);
		m_comboTimeout = new JComboBox(CONNECT_TIMEOUTS);
		advancedPanelConstraints.fill = GridBagConstraints.NONE;
		advancedPanelConstraints.gridx++;

		panelAdvanced.add(m_comboTimeout, advancedPanelConstraints);

		advancedPanelConstraints.weightx = 1.0;
		advancedPanelConstraints.weighty = 1.0;
		advancedPanelConstraints.gridwidth = 3;
		advancedPanelConstraints.gridx = 0;
		advancedPanelConstraints.gridy++;
		panelAdvanced.add(new JLabel(), advancedPanelConstraints);

		updateValues(false);

		return panelAdvanced;
	}

	/**
	 * Creates a new lower view of the dialog for displaying account details.
	 * @return JPanel
	 */
	private JPanel createDetailsPanel(ActionListener a_actionListener)
	{
		JPanel p = new JPanel(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.fill = c.NONE;
		c.anchor = c.NORTHWEST;
		c.weightx = 0;
		c.weighty = 0;
		c.gridx = 0;
		c.gridy = 0;
		c.insets = new Insets(5, 5, 5, 5);
		c.gridwidth = 2;
		p.add(new JLabel(JAPMessages.getString(MSG_ACCOUNT_DETAILS)), c);

		c.gridwidth = 1;
		c.insets = new Insets(5, 10, 5, 5);
		c.gridy++;
		c.gridheight = 5;
		m_coinstack = new JProgressBar(0, 8);
		p.add(m_coinstack, c);

		c.gridheight = 1;
		c.gridx++;
		p.add(new JLabel(JAPMessages.getString(MSG_ACCOUNT_DEPOSIT)), c);
		c.gridx++;
		m_labelDeposit = new JLabel();
		p.add(m_labelDeposit, c);

		/*
		c.gridx--;
		c.gridy++;
		p.add(new JLabel(JAPMessages.getString(MSG_ACCOUNT_BALANCE) + ":"), c);
		c.gridx++;
		m_labelBalance = new JLabel();
		p.add(m_labelBalance, c);*/


		c.gridx--;
		c.gridy++;
		p.add(new JLabel(JAPMessages.getString(MSG_ACCOUNT_SPENT)), c);
		c.gridx++;
		m_labelSpent = new JLabel();
		p.add(m_labelSpent, c);

		/*
		c.gridx--;
		c.gridy++;
		p.add(new JLabel(JAPMessages.getString(MSG_ACCOUNT_FLAT_ENDDATE) + ":"), c);
		c.gridx++;
		m_labelEnddate = new JLabel();
		p.add(m_labelEnddate, c);*/

		c.gridx--;
		c.gridy++;
		p.add(new JLabel(JAPMessages.getString(MSG_ACCOUNT_FLAT_VOLUME) + ":"), c);
		c.gridx++;
		m_labelVolume = new JLabel();
		m_labelVolume.addMouseListener(myActionListener);
		p.add(m_labelVolume, c);

		c.gridy++;
		c.gridy++;

		JPanel buttonsPanel = new JPanel(new GridBagLayout());
		GridBagConstraints d = new GridBagConstraints();
		d.fill = c.HORIZONTAL;
		d.anchor = c.NORTHWEST;
		d.weightx = 0;
		d.weighty = 0;
		d.gridx = 0;
		d.gridy = 0;
		d.insets = new Insets(5, 5, 5, 5);

		/*
		  d.gridx++;
		  m_btnBuyFlat = new JButton(JAPMessages.getString(MSG_BUTTON_BUYFLAT));
		  m_btnBuyFlat.setEnabled(false);
		  m_btnBuyFlat.addActionListener(a_actionListener);
		  buttonsPanel.add(m_btnBuyFlat,d);
		 */

		if (JAPConstants.DEBUG)
		{
			m_btnChargeAccount = new JButton(JAPMessages.getString(MSG_BUTTON_CHARGE));
			m_btnChargeAccount.addActionListener(a_actionListener);
			buttonsPanel.add(m_btnChargeAccount, d);
			d.gridx++;
		}

		m_btnSelect = new JButton(JAPMessages.getString(MSG_BUTTON_ACTIVATE));
		m_btnSelect.addActionListener(a_actionListener);
		buttonsPanel.add(m_btnSelect, d);

		d.gridx++;
		m_btnReload = new JButton(JAPMessages.getString(MSG_BUTTONRELOAD));
		m_btnReload.addActionListener(a_actionListener);
		buttonsPanel.add(m_btnReload, d);

		d.gridx++;
		m_btnActivate = new JButton(JAPMessages.getString(MSG_BUTTON_UNLOCK));
		m_btnActivate.setVisible(false); //hide button
		m_btnActivate.addActionListener(a_actionListener);
		buttonsPanel.add(m_btnActivate, d);


		d.gridx++;
		m_btnDeleteAccount = new JButton(JAPMessages.getString(MSG_BUTTON_DELETE));
		m_btnDeleteAccount.addActionListener(a_actionListener);
		buttonsPanel.add(m_btnDeleteAccount, d);

		d.gridx++;
		d.weightx = 1;
		d.weighty = 1;
		m_btnExportAccount = new JButton(JAPMessages.getString(MSG_BUTTON_EXPORT));
		m_btnExportAccount.addActionListener(a_actionListener);
		buttonsPanel.add(m_btnExportAccount, d);



		c.anchor = c.NORTHWEST;
		c.weightx = 1;
		c.weighty = 1;
		c.gridx = 0;
		c.gridy++;
		c.gridwidth = 3;
		p.add(buttonsPanel, c);

		return p;
	}

	private void updateAccountList()
	{
		Runnable updateAccountThread = new Runnable()
		{
			public void run()
			{
				synchronized (m_listAccounts)
				{
					PayAccount account;
					int activeAccountIndex = -1;
					DefaultListModel listModel = new DefaultListModel();
					Enumeration accounts = PayAccountsFile.getInstance().getAccounts();
					int selectedItem = m_listAccounts.getSelectedIndex();

					for (int i = 0; accounts.hasMoreElements(); i++)
					{
						account = (PayAccount) accounts.nextElement();
						if (PayAccountsFile.getInstance().getActiveAccount() == account)
						{
							activeAccountIndex = i;
						}
						listModel.addElement(account);
					}

					m_listAccounts.setModel(listModel);
					m_listAccounts.revalidate();

					if (m_listAccounts.getModel().getSize() > 0)
					{
						if (selectedItem < 0)
						{
							if (activeAccountIndex >= 0)
							{
								selectedItem = activeAccountIndex;
							}
							else
							{
								selectedItem = 0;
							}
						}
						else if (selectedItem >= m_listAccounts.getModel().getSize())
						{
							selectedItem = m_listAccounts.getModel().getSize() - 1;
						}
						m_listAccounts.setSelectedIndex(selectedItem);
						m_listAccounts.scrollRectToVisible(m_listAccounts.getCellBounds(selectedItem,
							selectedItem));
					}
				}
			}
		};
		if (SwingUtilities.isEventDispatchThread())
		{
			updateAccountThread.run();
		}
		else
		{
			try
			{
				SwingUtilities.invokeAndWait(updateAccountThread);
			}
			catch (Exception a_e)
			{
				LogHolder.log(LogLevel.EXCEPTION, LogType.GUI, a_e);
			}
		}
	}

	private void enableDisableButtons()
	{
		//if (m_listAccounts.getModel().getSize() > 0)
		{
			boolean enable = (getSelectedAccount() != null && getSelectedAccount().getPrivateKey() != null);
			m_btnActivate.setEnabled(
				getSelectedAccount() != null && getSelectedAccount().getPrivateKey() == null);
			//m_btnChargeAccount.setEnabled(enable);
			//m_btnBuyFlat.setEnabled(enable);
			m_btnTransactions.setEnabled(enable);
			m_btnExportAccount.setEnabled(enable);
			m_btnReload.setEnabled(enable);
			m_btnSelect.setEnabled(getSelectedAccount() != null &&
								   getSelectedAccount() != PayAccountsFile.getInstance().getActiveAccount());
			m_btnDeleteAccount.setEnabled(getSelectedAccount() != null);
		}
		/*
		  else
		  {
		   m_btnActivate.setEnabled(false);
		   m_btnChargeAccount.setEnabled(false);
		   m_btnTransactions.setEnabled(false);
		   m_btnExportAccount.setEnabled(false);
		   m_btnReload.setEnabled(false);
		   m_btnSelect.setEnabled(false);
		   m_btnDeleteAccount.setEnabled(false);
		  }*/
	}

	/**
	 * Handler for the Button Clicks
	 * @author Bastian Voigt
	 * @version 1.0
	 */
	private class MyActionListener extends MouseAdapter implements ActionListener
	{
		private boolean m_bButtonClicked = false;

		public void mouseClicked(MouseEvent a_event)
		{
			doAction(a_event.getSource());
		}

		public void actionPerformed(final ActionEvent e)
		{
			doAction(e.getSource());
		}

		public void doAction(final Object source)
		{
			Thread clickThread = new Thread(new Runnable()
			{
				public void run()
				{
					//Object source =  e.getSource();
					if (source == m_btnCreateAccount)
					{
						doCreateAccount(null);
					}
					else if (JAPConstants.DEBUG && source == m_btnChargeAccount)
					{
						doChargeAccount(getSelectedAccount());
					}
					else if (source == m_btnDeleteAccount)
					{
						doDeleteAccount(getSelectedAccount());
					}
					else if (source == m_btnImportAccount)
					{
						doImportAccount();
					}
					else if (source == m_btnExportAccount)
					{
						doExportAccount(getSelectedAccount());
					}
					else if (source == m_btnTransactions)
					{
						doShowTransactions();
					}
					else if (source == m_btnSelect)
					{
						if (getSelectedAccount() != null && getSelectedAccount().getPrivateKey() == null)
						{
							doActivateAccount(getSelectedAccount());
						}
						if (getSelectedAccount() != null && getSelectedAccount().getPrivateKey() != null)
						{
							doSelectAccount(getSelectedAccount());
						}
					}
					else if (source == m_btnPassword)
					{
						doChangePassword();
					}
					else if (source == m_btnReload)
					{
						doGetStatement(getSelectedAccount());
					}
					else if (source == m_btnActivate)
					{
						doActivateAccount(getSelectedAccount());
					}
					else if (source == m_labelVolume)
					{
						if (m_labelVolume.getForeground() == Color.blue)
						{
							showOpenTransaction(getSelectedAccount());
						}
					}
					else if (source == m_labelTermsAndConditions)
					{
						showTermsAndConditions(getSelectedAccount());
					}
					m_bButtonClicked = false;
				}
			});
			if (!m_bButtonClicked)
			{
				m_bButtonClicked = true;
				clickThread.start();
			}
		}
	}

	/**
	 * Asks the user for a new payment password
	 */
	private void doChangePassword()
	{
		JAPDialog d = new JAPDialog(getRootPanel(),
									JAPMessages.getString(MSG_ACCPASSWORDTITLE), true);
		PasswordContentPane p;

		if (JAPController.getInstance().getPaymentPassword() != null)
		{
			p = new PasswordContentPane(d, PasswordContentPane.PASSWORD_CHANGE,
										JAPMessages.getString(MSG_ENCRYPT_ACCOUNTS))
			{
				public char[] getComparedPassword()
				{
					return JAPController.getInstance().getPaymentPassword().toCharArray();
				}
			};
		}
		else
		{
			p = new PasswordContentPane(d, PasswordContentPane.PASSWORD_NEW, "");
		}
		p.updateDialog();
		d.pack();
		d.setVisible(true);
		if (p.getButtonValue() != PasswordContentPane.RETURN_VALUE_CANCEL &&
			p.getButtonValue() != PasswordContentPane.RETURN_VALUE_CLOSED)
		{
			JAPController.getInstance().setPaymentPassword(new String(p.getPassword()));
		}
	}

	/**
	 * Shows transaction details for all accounts
	 * (if several JPIs are in use: for all those accounts from the same JPI as the currently selected account)
	 * @param a_account PayAccount
	 */
	private void doShowTransactions()
	{
		Vector a_accounts = new Vector();
		PayAccount anAccount;
	    Enumeration accounts = PayAccountsFile.getInstance().getAccounts();

	    PayAccount activeAccount = getSelectedAccount();
		PaymentInstanceDBEntry jpiOfActiveAccount = activeAccount.getBI();

		while (accounts.hasMoreElements() )
		{
			anAccount = (PayAccount) accounts.nextElement();
			/* not necessary, and pops up a progress bar for every single account
		//update account to be sure to include all transactions
		//(since BI will only fill in transacton details, but not provide data for new ones not yet contained in the PayAccount)
			doGetStatement(anAccount);
		    */
		    if (anAccount.isTransactionExpired() )
			{
				continue; //if the transaction is already expired, we don't want to give the user the chance to pay any more
			}

		    PaymentInstanceDBEntry jpiOfThisAccount = anAccount.getBI();
			//only add account to the list of those to get transaction details for
			//if the account's JPI matches the JPI of the currently active account
			if (jpiOfThisAccount != null && jpiOfActiveAccount != null) //jpiOfActiveAccount is sometimes null, no idea why
			{
				if (jpiOfThisAccount.getId().equalsIgnoreCase(jpiOfActiveAccount.getId()))
				{
					a_accounts.addElement(anAccount);
				}
			}
			else
			{
				LogHolder.log(LogLevel.EXCEPTION, LogType.PAY, "JPI is null! " +
					"Current account: " + jpiOfThisAccount + " " +
					"Active account: " + jpiOfActiveAccount + " " +
					"Current account ID: " + anAccount.getAccountNumber());
			}
		}

		TransactionOverviewDialog d = new TransactionOverviewDialog(this,
			JAPMessages.getString(MSG_TRANSACTION_OVERVIEW_DIALOG), true, a_accounts);

	}

	/**
	 * doShowDetails - shows account details in the details panel
	 */
	private void doShowDetails(PayAccount selectedAccount)
	{
		if (selectedAccount == null)
		{
			m_coinstack.setValue(0);
			m_labelCreationDate.setText("");
			m_labelStatementDate.setText("");
			m_labelDeposit.setText("");
			m_labelSpent.setText("");
			//m_labelBalance.setText("");
			//m_labelEnddate.setText("");
			m_labelVolume.setText("");
			m_labelValid.setText("");
			m_paymentInstance.setText("");
			m_lblInactiveMessage.setText("");
			m_lblNoBackupMessage.setText("");
			return;
		}

		/** If there is no account info or the account info is older than 24 hours,
		 * fetch a new statement from the Payment Instance.
		 */
		/*if (!selectedAccount.hasAccountInfo() ||
		 (selectedAccount.getAccountInfo().getBalance().getTimestamp().getTime() <
		  (System.currentTimeMillis() - 1000 * 60 * 60 * 24)))
		   {
		 doGetStatement(selectedAccount);
		   }*/
		if (selectedAccount.getPrivateKey() == null)
		{
			m_lblInactiveMessage.setText(JAPMessages.getString(MSG_ACCOUNT_DISABLED));
			m_lblInactiveMessage.setToolTipText(JAPMessages.getString(MSG_TOOL_TIP_ACTIVATE));
		}
		else
		{
			m_lblInactiveMessage.setText("");
			m_lblInactiveMessage.setToolTipText("");
		}
		if (!selectedAccount.isBackupDone())
		{
			m_lblNoBackupMessage.setText(JAPMessages.getString(MSG_NO_BACKUP));
			m_lblNoBackupMessage.setToolTipText(JAPMessages.getString(MSG_TOOL_TIP_NO_BACKUP));
		}
		else
		{
			m_lblNoBackupMessage.setText("");
			m_lblNoBackupMessage.setToolTipText("");
		}

		XMLAccountInfo accountInfo = selectedAccount.getAccountInfo();
		if (accountInfo != null)
		{
			XMLBalance balance = accountInfo.getBalance();

			PaymentInstanceDBEntry pi = selectedAccount.getBI();
			if (pi == null)
			{
				m_paymentInstance.setText("");
			}
			else
			{
				m_paymentInstance.setText(pi.getName());
			}

			Calendar termsDate = selectedAccount.getTermsDate();
			String strDate = "";
			if (termsDate != null)
			{
				strDate = "(" + new SimpleDateFormat("yyyy-MM-dd").format(termsDate.getTime()) + ")";
			}
			m_labelTermsAndConditions.setText(JAPMessages.getString(MSG_TERMS_AND_COND, strDate));

			m_labelCreationDate.setText(JAPUtil.formatTimestamp(selectedAccount.getCreationTime(), false,
				JAPMessages.getLocale().getLanguage()));
			if (balance == null)
			{
				m_labelStatementDate.setText("");
				m_labelDeposit.setText("");
				m_labelSpent.setText("");
				m_coinstack.setValue(0);
				m_labelVolume.setText("");
				m_labelValid.setText("");
			}
			else
			{
				m_labelStatementDate.setText(JAPUtil.formatTimestamp(balance.getTimestamp(), true,
					JAPMessages.getLocale().getLanguage()));
				m_labelDeposit.setText(JAPUtil.formatEuroCentValue(balance.getDeposit()));
				m_labelSpent.setText(JAPUtil.formatBytesValueWithUnit(balance.getSpent()));
				//m_labelBalance.setText(JAPUtil.formatEuroCentValue(balance.getBalance()));

				Locale curLocale = JAPMessages.getLocale();
				String curLang = curLocale.getLanguage();
				Timestamp flatEnddate = balance.getFlatEnddate();
				Timestamp now = new Timestamp(System.currentTimeMillis());

				boolean expired = false;
				if (balance.getCredit() == 0)
				{
					m_labelValid.setText("");
				}
				else if (flatEnddate != null && flatEnddate.after(now))
				{
					m_labelValid.setText(JAPUtil.formatTimestamp(flatEnddate, false, curLang));
				}
				else
				{
					expired = true;
					m_labelValid.setText(JAPUtil.formatTimestamp(flatEnddate, false, curLang) +
										 " (" + JAPMessages.getString(MSG_EXPIRED) + ")");
				}


				//m_labelEnddate.setText(JAPUtil.formatTimestamp(flatEnddate, false, curLang));
				if (balance.getCredit() > 0)
				{
					m_labelVolume.setText(((expired ? "(" : "") +
										   JAPUtil.formatBytesValueWithUnit(balance.getVolumeBytesLeft() * 1000) +
										   (expired ? ")" : "")));
					m_labelVolume.setForeground(m_labelValid.getForeground());
					m_labelVolume.setToolTipText(null);
					m_labelVolume.setCursor(Cursor.getDefaultCursor());
				}
				else if (balance.getSpent() == 0 && !expired)
				{
					m_labelVolume.setText(JAPMessages.getString(MSG_NO_TRANSACTION));
					if (selectedAccount.getTransCerts().size() > 0 && !selectedAccount.isTransactionExpired() )
					{
						m_labelVolume.setToolTipText(JAPMessages.getString(MSG_SHOW_TRANSACTION_DETAILS));
						m_labelVolume.setForeground(Color.blue);
						m_labelVolume.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
					}
					else
					{
						m_labelVolume.setToolTipText(null);
						m_labelVolume.setForeground(m_labelValid.getForeground());
						m_labelVolume.setCursor(Cursor.getDefaultCursor());
					}
				}
				else
				{
					m_labelVolume.setText(JAPMessages.getString(MSG_NO_CREDIT));
					m_labelVolume.setToolTipText(null);
					m_labelVolume.setForeground(m_labelValid.getForeground());
					m_labelVolume.setCursor(Cursor.getDefaultCursor());
				}
				/*
				   m_labelValid.setText(JAPUtil.formatTimestamp(balance.getValidTime(), true,
					JAPController.getInstance().getLocale().getLanguage()));
				   if (balance.getValidTime().before(new Date()))
				   {
					m_labelValid.setForeground(Color.red);
					m_labelValid.setToolTipText(JAPMessages.getString(MSG_TOOL_TIP_EXPIRED));
				   }
				   else
				   {
					m_labelValid.setForeground(new JLabel().getForeground());
					m_labelValid.setToolTipText("");
				   }*/

				if (expired)
				{
					m_coinstack.setValue(0);
				}
				else
				{
					//long dep = balance.getVolumeBytesLeft()*1000 + balance.getSpent();
					long deposit = PaymentMainPanel.FULL_AMOUNT * 1000;
					long credit = balance.getCredit() * 1000;
					double percent = (double) credit / (double) deposit;

					if (percent > 0.87)
					{
						m_coinstack.setValue(8);
					}
					else if (percent > 0.75)
					{
						m_coinstack.setValue(7);
					}
					else if (percent > 0.63)
					{
						m_coinstack.setValue(6);
					}
					else if (percent > 0.50)
					{
						m_coinstack.setValue(5);
					}
					else if (percent > 0.38)
					{
						m_coinstack.setValue(4);
					}
					else if (percent > 0.25)
					{
						m_coinstack.setValue(3);
					}
					else if (percent > 0.13)
					{
						m_coinstack.setValue(2);
					}
					else if (percent > 0.01)
					{
						m_coinstack.setValue(1);
					}
					else
					{
						m_coinstack.setValue(0);
					}
				}
			}
		}
		else
		{
			m_coinstack.setValue(0);
			m_labelCreationDate.setText("");
			m_labelStatementDate.setText("");
			m_labelDeposit.setText("");
			m_labelSpent.setText("");
			//m_labelBalance.setText("");
			m_labelValid.setText("");
			//m_labelEnddate.setText("");
			m_labelVolume.setText("");
		}
	}

	/**
	 * returns the selected (active) account
	 * @return PayAccount
	 */
	private PayAccount getSelectedAccount()
	{
		try
		{
			return (PayAccount) m_listAccounts.getSelectedValue();
		}
		catch (Exception a_e)
		{
			return null;
		}
	}

	public void showTermsAndConditions(final PayAccount a_account)
	{
		final JAPDialog d = new JAPDialog(getRootPanel(),
										  JAPMessages.getString(TermsAndConditionsPane.MSG_HEADING), true);
		d.setDefaultCloseOperation(JAPDialog.DISPOSE_ON_CLOSE);
		d.setResizable(false);

		//************ fetch AGBs (terms and conditions) *******//
		final WorkerContentPane fetchTermsPane = new WorkerContentPane(d,
			JAPMessages.getString(MSG_FETCHINGTERMS),
			new FetchTermsRunnable(d, a_account.getBI(), a_account.getTerms()));
		fetchTermsPane.setInterruptThreadSafe(false);

		TermsAndConditionsPane pane = new TermsAndConditionsPane(d, fetchTermsPane, false)
		{
			public CheckError[] checkUpdate()
			{
				if (a_account.getTerms() == null)
				{
					a_account.setTerms( (XMLGenericText) fetchTermsPane.getValue());
				}
				return super.checkUpdate();
			}
		};
		pane.getButtonCancel().setVisible(false);
		DialogContentPane.updateDialogOptimalSized(fetchTermsPane);
		d.addWindowListener(new WindowAdapter()
		{
			public void windowClosed(WindowEvent a_event)
			{
				updateAccountList();
			}
		});

		//d.setLocationCenteredOnOwner();
		d.setVisible(true);
	}


	public void doChargeAccount(final PayAccount selectedAccount)
	{

		if (selectedAccount == null)
		{
			return;
		}

		if (selectedAccount.getBalanceValidTime().before(new Date()))
		{
			JAPDialog.showMessageDialog(getRootPanel(), JAPMessages.getString(MSG_ACCOUNT_INVALID));
			return;
		}

		final JAPDialog d = new JAPDialog(getRootPanel(), JAPMessages.getString(MSG_CHARGETITLE), true);
		d.setDefaultCloseOperation(JAPDialog.DISPOSE_ON_CLOSE);
		d.setResizable(false);
		doChargeAccount(new FixedReturnAccountRunnable(selectedAccount), d, null, null, new Vector(),true);
		d.setLocationCenteredOnOwner();
		d.setVisible(true);
	}


	/**
	 * Charges the selected account
	 */
	private void doChargeAccount(final IReturnAccountRunnable a_accountCreationThread,
								 final JAPDialog a_parentDialog,
								 final DialogContentPane a_previousContentPane,
								 final IReturnBooleanRunnable a_booleanThread,
								 final Vector a_tan,
								 final boolean isNewAccount)
	{
		// JPI does NOT maintain connections state, so we have to connect and authenticate again for every single request,
		// could be changed to improve performance



		//*********** fetch volume plans ************************//

		WorkerContentPane.IReturnRunnable fetchPlans = new WorkerContentPane.IReturnRunnable()
		{
			private XMLVolumePlans m_volumePlans;
			public void run()
			{
				BIConnection piConn = null;
				try
				{
					PaymentInstanceDBEntry pi = a_accountCreationThread.getAccount().getBI();
					piConn = new BIConnection(pi);
					piConn.connect(JAPModel.getInstance().getPaymentProxyInterface());

					piConn.authenticate(a_accountCreationThread.getAccount().getAccountCertificate(),
										a_accountCreationThread.getAccount().getPrivateKey());
					LogHolder.log(LogLevel.DEBUG, LogType.PAY, "Fetching volume plans");
					m_volumePlans = piConn.getVolumePlans();
					piConn.disconnect();
				}
				catch (Exception e)
				{
					if ( piConn != null)
					{
						try
						{
							piConn.disconnect();
						}
						catch (Exception ex)
						{
						}
					}

					if (!Thread.currentThread().isInterrupted())
					{
						LogHolder.log(LogLevel.EXCEPTION, LogType.NET,
									  "Error fetching payment options: ", e);
						showPIerror(a_parentDialog.getContentPane(), e);
						Thread.currentThread().interrupt();
					}
				}
			}

			public Object getValue()
			{
				return m_volumePlans;
			}

		};

		final WorkerContentPane fetchPlansPane = new WorkerContentPane(a_parentDialog,
			JAPMessages.getString(MSG_FETCHINGPLANS), a_previousContentPane, fetchPlans)
		{
			public boolean isMoveForwardAllowed()
			{
				return a_parentDialog.isVisible() && a_booleanThread != null && !a_booleanThread.isTrue();
			}
		};
		fetchPlansPane.setInterruptThreadSafe(false);

		// ********* show available volume plans *************//

	   final VolumePlanSelectionPane planSelectionPane = new VolumePlanSelectionPane(a_parentDialog,
		   fetchPlansPane, isNewAccount);

		// ********** fetch payment options *********************//
	   WorkerContentPane.IReturnRunnable fetchOptions = new WorkerContentPane.IReturnRunnable()
	   {
		   private XMLPaymentOptions m_paymentOptions;
		   public void run()
		   {
			   BIConnection piConn = null;
			   try
			   {
				   PaymentInstanceDBEntry pi = a_accountCreationThread.getAccount().getBI();
				   piConn = new BIConnection(pi);

				   piConn.connect(JAPModel.getInstance().getPaymentProxyInterface());
				   piConn.authenticate(a_accountCreationThread.getAccount().getAccountCertificate(),
									   //  PayAccountsFile.getInstance().getActiveAccount().getAccountCertificate(),
									   a_accountCreationThread.getAccount().getPrivateKey());
				   LogHolder.log(LogLevel.DEBUG, LogType.PAY, "Fetching payment options");
				   m_paymentOptions = piConn.getPaymentOptions();
				   piConn.disconnect();
			   }
			   catch (Exception e)
			   {
				   if (piConn != null)
				   {
					   try
					   {
						   piConn.disconnect();
					   }
					   catch (Exception ex)
					   {
					   }
				   }
				   if (!Thread.currentThread().isInterrupted())
				   {
					   LogHolder.log(LogLevel.EXCEPTION, LogType.NET,
									 "Error fetching payment options: " + e.getMessage());
					   showPIerror(a_parentDialog.getContentPane(), e);
					   Thread.currentThread().interrupt();
				   }
			   }
		   }

		   public Object getValue()
		   {
			   return m_paymentOptions;
		   }
	   };

		final WorkerContentPane fetchOptionsPane = new WorkerContentPane(a_parentDialog,
			JAPMessages.getString(MSG_FETCHINGOPTIONS), planSelectionPane, fetchOptions)
		{
			public boolean isSkippedAsNextContentPane()
			{
				if ( planSelectionPane.isCouponUsed() )
				{
					LogHolder.log(LogLevel.DEBUG, LogType.PAY, "Coupon entered, skipping payment options pane");
					return true;
				}
				else //no coupon used = regular volume plan
				{

					return false;
				}
			}

		};
		fetchOptionsPane.setInterruptThreadSafe(false);

		// ************ show method selection pane *****************
		final MethodSelectionPane methodSelectionPane =	new MethodSelectionPane(a_parentDialog, fetchOptionsPane){
			public boolean isSkippedAsNextContentPane()
			{
				if (planSelectionPane.isCouponUsed())
				{
					LogHolder.log(LogLevel.DEBUG, LogType.PAY, "Coupon entered, skipping payment options pane");
					return true;
				}
				else //no coupon used = regular volume plan
				{

					return false;
				}
			}

		};

		/******** fetch transaction number ****************/
		final WorkerContentPane.IReturnRunnable fetchTan = new WorkerContentPane.IReturnRunnable()
		{
			private XMLTransCert m_transCert;
			public void run()
			{
				if (m_transCert == null) //Elmar: reason for this if-clause?
				{
					try
					{
						LogHolder.log(LogLevel.DEBUG, LogType.PAY,
									  "Fetching Transaction Certificate from Payment Instance");

						String planName;
						String paymentMethod;
						String amount;
						if (planSelectionPane.isCouponUsed() )
						{
							String localizedCoupon = JAPMessages.getString(MSG_COUPON);
							planName = localizedCoupon;
							paymentMethod = localizedCoupon;
							amount = "0";

						} else
						{
							planName = planSelectionPane.getSelectedVolumePlan().getName();
							paymentMethod = methodSelectionPane.getSelectedPaymentOption().getName();
							int intAmount = planSelectionPane.getSelectedVolumePlan().getPrice();
						    amount = (new Integer(intAmount)).toString();
						}

						XMLGenericStrings requestData = new XMLGenericStrings();
						requestData.addEntry("plan",planName);
						requestData.addEntry("method",paymentMethod);
						requestData.addEntry("amount",amount);
						//for paysafecard, JPI has to use language-specific URLs to show result
						String lang = JAPMessages.getLocale().getLanguage();
						requestData.addEntry("language",lang);
						PayAccount curAccount = a_accountCreationThread.getAccount();
						m_transCert = curAccount.charge(JAPModel.getInstance().getPaymentProxyInterface(), requestData);
						if (m_transCert != null)
						{
							a_tan.addElement(m_transCert);
						}
						// there is no way back to choose another rate
						methodSelectionPane.getButtonNo().setVisible(false);
					}
					catch (Exception e)
					{
						if (!Thread.currentThread().isInterrupted())
						{
							LogHolder.log(LogLevel.EXCEPTION, LogType.NET,
										  "Error fetching TransCert: ", e);
							showPIerror(a_parentDialog.getContentPane(), e);
							Thread.currentThread().interrupt();
						}
					}
				}
			}

			public Object getValue()
			{
				return m_transCert;
			}
		};

		final WorkerContentPane fetchTanPane =
			new WorkerContentPane(a_parentDialog, JAPMessages.getString(MSG_FETCHINGTAN),
								  methodSelectionPane, fetchTan)
		{
			public boolean isSkippedAsNextContentPane()
			{
				if (fetchTan.getValue() == null)
				{
					return false;
				}
				return true;
			}
		};
		fetchTanPane.setInterruptThreadSafe(false);

		//************* show Payment Info (except for passive payments) **************
		 final PaymentInfoPane paymentInfoPane = new PaymentInfoPane(a_parentDialog, fetchTanPane)
		 {
			 public boolean isSkippedAsNextContentPane()
			 {
				 if (planSelectionPane.isCouponUsed() )
				 {
					 return true;
				 }
				 if (methodSelectionPane.getSelectedPaymentOption().getType().equalsIgnoreCase(
					 XMLPaymentOption.OPTION_ACTIVE))
				 {
					 return false;
				 }
				 else if (methodSelectionPane.getSelectedPaymentOption().getType().equalsIgnoreCase(
					 XMLPaymentOption.OPTION_MIXED))
				 {
					 return false;
				 }
				 else
				 {
					 return true;
				 }
			 }

			 public boolean isSkippedAsPreviousContentPane()
			 {
				 return true;
			 }
		 };
		//paymentInfoPane.getButtonNo().setVisible(false);

		//************* let user enter passive payment data (for passive only) *************//
		final PassivePaymentPane passivePaymentPane = new PassivePaymentPane(a_parentDialog, paymentInfoPane)
		{

			public boolean isSkippedAsNextContentPane()
			{
				if (planSelectionPane.isCouponUsed())
				{
					return true;
				}
				if (methodSelectionPane.getSelectedPaymentOption().getType().equalsIgnoreCase(
					XMLPaymentOption.OPTION_ACTIVE))
				{
					return true;
				}
				else if (methodSelectionPane.getSelectedPaymentOption().getType().equalsIgnoreCase(
					XMLPaymentOption.OPTION_MIXED))
				{
					return true;
				}
				else
				{
					return false;
				}
			}
		};

		//********* send passive payment ************
		 WorkerContentPane.IReturnRunnable sendPassive = new WorkerContentPane.IReturnRunnable()
		 {
			 private Boolean m_successful = new Boolean(true);

			 public void run()
			 {
				 /** Post data to payment instance */
				 BIConnection biConn = new BIConnection(a_accountCreationThread.getAccount().getBI());
				 XMLPassivePayment paymentToSend = new XMLPassivePayment();
				 //if coupon was used, get its code and put it into an XMLPassivePayment
				 if (planSelectionPane.isCouponUsed() )
				 {
					 paymentToSend.addData(XMLPassivePayment.KEY_COUPONCODE,planSelectionPane.getEnteredCouponCode());
					 paymentToSend.setPaymentName("Coupon");
					 long accNum = a_accountCreationThread.getAccount().getAccountNumber();
					 paymentToSend.addData(XMLPassivePayment.KEY_ACCOUNTNUMBER, new Long(accNum).toString());
					 XMLTransCert transCert = (XMLTransCert) fetchTanPane.getValue();
					 long tan = transCert.getTransferNumber();
					 paymentToSend.addData(XMLPassivePayment.KEY_TRANSFERNUMBER, new Long(tan).toString());
					 //no other data needed, since details about the contents of the coupons were stored in the database when the coupon was created

				 } else if (methodSelectionPane.getSelectedPaymentOption().getType().equalsIgnoreCase(
					 XMLPaymentOption.OPTION_PASSIVE))
				 {
					 paymentToSend = passivePaymentPane.getEnteredInfo();
					 XMLVolumePlan selectedPlan = planSelectionPane.getSelectedVolumePlan();
					 String planName = selectedPlan.getName();
					 int planPrice = selectedPlan.getPrice();
					 paymentToSend.setAmount(planPrice);
					 paymentToSend.addData(XMLPassivePayment.KEY_VOLUMEPLAN, planName);
					 //just a crutch to avoid having to query the database for the TAN
					 long accNum = a_accountCreationThread.getAccount().getAccountNumber();
					 paymentToSend.addData(XMLPassivePayment.KEY_ACCOUNTNUMBER, new Long(accNum).toString());

				 }
				 else if (methodSelectionPane.getSelectedPaymentOption().getType().equalsIgnoreCase(
					 XMLPaymentOption.OPTION_MIXED))
				 {
					 XMLVolumePlan selectedPlan = planSelectionPane.getSelectedVolumePlan();
					 int planPrice = selectedPlan.getPrice();
					 paymentToSend.setAmount(planPrice);
					 paymentToSend.setCurrency("EUR");
					 String paymentName = methodSelectionPane.getSelectedPaymentOption().getName();
					 paymentToSend.setPaymentName(paymentName);
					 XMLTransCert newTan = paymentInfoPane.getTransCert();
					 paymentToSend.setTransferNumber(newTan.getTransferNumber());
				 }
				 try
				 {
					 biConn.connect(JAPModel.getInstance().getPaymentProxyInterface());
					 biConn.authenticate(a_accountCreationThread.getAccount().getAccountCertificate(),
										 a_accountCreationThread.getAccount().getPrivateKey());
					 if (!biConn.sendPassivePayment(paymentToSend))
					 {
						 m_successful = new Boolean(false);
					 }
					 biConn.disconnect();
				 }
				 catch (Exception e)
				 {
					 if (biConn != null)
					 {
						 try
						 {
							 biConn.disconnect();
						 }
						 catch (Exception ex)
						 {
						 }
					 }
					 m_successful = new Boolean(false);
					 if (!Thread.currentThread().isInterrupted())
					 {
						 LogHolder.log(LogLevel.EXCEPTION, LogType.PAY,
									   "Could not send PassivePayment to payment instance: " +
									   e.getMessage());
						 showPIerror(a_parentDialog.getContentPane(), e);
						 Thread.currentThread().interrupt();
					 }
				 }
			 }

			 public Object getValue()
			 {
				 return m_successful;
			 }
		 };

		final WorkerContentPane sendPassivePane = new WorkerContentPane(a_parentDialog,
			JAPMessages.getString(MSG_SENDINGPASSIVE), passivePaymentPane, sendPassive)
		{
			public boolean isSkippedAsNextContentPane()
			{
				if (planSelectionPane.isCouponUsed() )
				{
					return false;
				}
				if (methodSelectionPane.getSelectedPaymentOption().getType().equalsIgnoreCase(
					XMLPaymentOption.OPTION_ACTIVE))
				{
					return true;
				}
				else if (methodSelectionPane.getSelectedPaymentOption().getType().equalsIgnoreCase(
					XMLPaymentOption.OPTION_MIXED))
			    {
					return false;
					//we do NOT send the paysafecardpayment here any more, we do it already when getting the tan (to avoid the user accidently closing the wizard after confirming the payment but before the payment ist stored)
					//BUT we send the passivepayment to immediately trigger polling the disposition state/crediting the user account
				}
				else //passive
				{
					return false;
				}
			}
		};

		//************** show confirmation / result of payment  (dependent on method and outcome) ****************/
		final SimpleWizardContentPane sentPane = new SimpleWizardContentPane(a_parentDialog,
			JAPMessages.getString(MSG_SENTPASSIVE), null,
			new Options(createUpdateAccountPane(
				a_accountCreationThread, methodSelectionPane, a_parentDialog, sendPassivePane)))
		{
			public boolean isSkippedAsNextContentPane()
			{
				return false; //show for all methods, so everybody sees the warning to back up the account
				}

			public CheckError[] checkUpdate()
				{
				Vector messagesToShow = new Vector();
				String highlightMarkupStart = "<Font color='red'><b>";
				String highlightMarkupEnd = "</b></Font>";
				String backupWarning = highlightMarkupStart + JAPMessages.getString(MSG_BACKUP_WARNING) +
					highlightMarkupEnd;
				String passiveError = highlightMarkupStart + JAPMessages.getString(MSG_NOTSENTPASSIVE) +
					highlightMarkupEnd;
				String passiveOK = JAPMessages.getString(MSG_SENTPASSIVE);
				String couponOK = JAPMessages.getString(MSG_COUPON_SENT);
				String couponFailed = highlightMarkupStart + JAPMessages.getString(MSG_COUPON_FAILED) +
					highlightMarkupEnd; //shold not happne, since we check validity upon entering the coupon
				String activeComplete = JAPMessages.getString(MSG_ACTIVE_COMPLETE);
				String mixedComplete = JAPMessages.getString(MSG_MIXED_COMPLETE);

				String paymentType;
				//get info about the delay until paymen is credited
				String language = JAPMessages.getLocale().getLanguage();
				String paymentDelay = null;

				//determine type of payment option used, so we can later show apropriate info
				if (planSelectionPane.isCouponUsed() )
				{
					paymentType = "coupon";
                }
				else
				{
					paymentType = methodSelectionPane.getSelectedPaymentOption().getType();
					paymentDelay = methodSelectionPane.getSelectedPaymentOption().getPaymentDelay(language);
				}

			    //set strings to show according to payment type and success/failure
				if (paymentType.equalsIgnoreCase(XMLPaymentOption.OPTION_ACTIVE))
				{
                    messagesToShow.addElement(activeComplete);
					messagesToShow.addElement(paymentDelay);
					messagesToShow.addElement(backupWarning);
				}
				else if (paymentType.equalsIgnoreCase(XMLPaymentOption.OPTION_MIXED))
				{
                   messagesToShow.addElement(mixedComplete);
				   messagesToShow.addElement(paymentDelay);
				   messagesToShow.addElement(backupWarning);
			    }
				else if (paymentType.equals("coupon") )
				{
					boolean passivePaymentSucceeded = ((Boolean) sendPassivePane.getValue()).booleanValue();

					if (passivePaymentSucceeded)
					{
						messagesToShow.addElement(couponOK);
						//no paymentDelay message here, since coupons are credited immediately
						messagesToShow.addElement(backupWarning);
					}
					else
					{
						messagesToShow.addElement(couponFailed);
					}
				}
				else //non-coupon passive payment, e.g. credit card
				{
					boolean passivePaymentSucceeded = ( (Boolean) sendPassivePane.getValue()).booleanValue();
					if (passivePaymentSucceeded)
					{
						messagesToShow.addElement(passiveOK);
						messagesToShow.addElement(paymentDelay);
						messagesToShow.addElement(backupWarning);
					}
					else
					{
						messagesToShow.addElement(passiveError);
					}

				}
				//combine messages and set text
				String combinedString = "";
				String stringToAdd;
				for (Enumeration messages = messagesToShow.elements(); messages.hasMoreElements(); )
				{
					stringToAdd = (String) messages.nextElement();
					if (stringToAdd != null) //possible if e.g. no payment delay message set
					{
						combinedString += "<p>";
						combinedString += stringToAdd;
						combinedString += "</p><br>";
					}
				}
				setText(combinedString);
				return null;
				}
		};

		sentPane.getButtonCancel().setVisible(false);
		sentPane.getButtonNo().setVisible(false);

		//the very first pane to show to the user is set here (NOT automatically the first one in the method!)
		if (a_previousContentPane == null)
		{
			DialogContentPane.updateDialogOptimalSized(fetchPlansPane);
		}
	}

	//************ fetch AGBs (terms and conditions) *******//
	private final class FetchTermsRunnable implements WorkerContentPane.IReturnRunnable
	{
		private XMLGenericText m_termsAndConditions;
		private JAPDialog m_parentDialog;
		private JpiSelectionPane m_jpiPane;
		PaymentInstanceDBEntry m_jpi;

		public FetchTermsRunnable(JAPDialog a_parentDialog, JpiSelectionPane a_jpiPane)
		{
			m_parentDialog = a_parentDialog;
			m_jpiPane = a_jpiPane;
		}

		public FetchTermsRunnable(JAPDialog a_parentDialog, PaymentInstanceDBEntry a_jpi,
								  XMLGenericText a_termsAndConditions)
		{
			m_parentDialog = a_parentDialog;
			m_jpi = a_jpi;
			m_termsAndConditions = a_termsAndConditions;
		}


		public void run()
		{
			BIConnection piConn = null;
			try
			{
				if (m_termsAndConditions != null)
				{
					return;
				}

				PaymentInstanceDBEntry pi;
				if (m_jpiPane != null)
				{
					pi = m_jpiPane.getSelectedPaymentInstance();
				}
				else
				{
					pi = m_jpi;
				}
				piConn = new BIConnection(pi);
				piConn.connect(JAPModel.getInstance().getPaymentProxyInterface());
				//authentication is neither necessary nor possible (creating first account -> user does not yet have an account to authenticate with)
				LogHolder.log(LogLevel.DEBUG, LogType.PAY, "Fetching terms and conditions");

				String lang = JAPMessages.getLocale().getLanguage();
				m_termsAndConditions = piConn.getTerms(lang);
				piConn.disconnect();
			}
			catch (Exception e)
			{
				if (piConn != null)
				{
					try
					{
						piConn.disconnect();
					}
					catch (Exception ex)
					{
					}
				}

				if (!Thread.currentThread().isInterrupted())
				{
					LogHolder.log(LogLevel.EXCEPTION, LogType.NET,
								  "Error fetching terms and conditions: ", e);
					showPIerror(m_parentDialog.getContentPane(), e);
					Thread.currentThread().interrupt();
				}
			}
		}

		public Object getValue()
		{
			return m_termsAndConditions;
		}

	};

	public void showOpenTransaction(PayAccount a_account)
	{
		/** @todo Must be refactored completely!!!! Really ugly mad code... */ //Elmar: I second that opinion, but am afraid to touch it...
		final PayAccount selectedAccount = a_account;
		final Vector transCerts = selectedAccount.getTransCerts();
		if (selectedAccount != null && transCerts.size() > 0)
		{
			try
			{
				final long transferNumber =
					( (XMLTransCert) transCerts.elementAt(0)).getTransferNumber();
				final BIConnection biConn = new BIConnection(selectedAccount.getBI());

				JAPDialog dialog =
					new JAPDialog(AccountSettingsPanel.this.getRootPanel(),
								  JAPMessages.getString(TransactionOverviewDialog.MSG_FETCHING_TAN), true);
				WorkerContentPane.IReturnRunnable run =
					new WorkerContentPane.IReturnRunnable()
				{
					Object data;
					public void run()
					{
						try
						{
							XMLTransactionOverview overview;

							biConn.connect(JAPModel.getInstance().
										   getPaymentProxyInterface());
							biConn.authenticate(selectedAccount.getAccountCertificate(),
												selectedAccount.getPrivateKey());
							overview =
								new XMLTransactionOverview(JAPMessages.getLocale().
								getLanguage());
							overview.addTan(transferNumber);
							overview = biConn.fetchTransactionOverview(overview);
							biConn.disconnect();
							if (overview == null)
							{
								throw new Exception(
									"JPI returned error message rather than transaction overview");
							}
							data = overview.getDataForTransaction(transferNumber);

						}
						catch (Exception a_e)
						{
							if (biConn != null)
							{
								try
								{
									biConn.disconnect();
								}
								catch (Exception ex)
								{
								}
							}


							data = a_e;
						}
					}

					public Object getValue()
					{
						return data;
					}
				};
				WorkerContentPane pane =
					new WorkerContentPane(dialog,
										  JAPMessages.getString(TransactionOverviewDialog.MSG_FETCHING_TAN), run);
/*
				WorkerContentPane.IReturnRunnable run2 =
					new WorkerContentPane.IReturnRunnable()
				{
					Object xmlReply;
					public void run()
					{
						try
						{
							biConn.connect(JAPModel.getInstance().
										   getPaymentProxyInterface());
							biConn.authenticate(selectedAccount.getAccountCertificate(),
												selectedAccount.getPrivateKey());

							xmlReply = biConn.fetchPaymentData(
								new Long(transferNumber).toString());
						}
						catch (Exception a_e)
						{
							xmlReply = a_e;
						}
					}

					public Object getValue()
					{
						return xmlReply;
					}
				};

				WorkerContentPane pane2 =
					new WorkerContentPane(dialog,
										  JAPMessages.getString(TransactionOverviewDialog.MSG_FETCHING_TAN), pane, run2);
*/
				pane.updateDialog();
				dialog.pack();
				dialog.setVisible(true);

				if (run.getValue() == null)
				{
					// interrupted
					return;
				}
				else if (run.getValue() instanceof Exception)
				{
					throw (Exception) run.getValue();
				}
				else if (! (run.getValue() instanceof Hashtable))
				{
					throw new Exception("Illegal return value!");
				}
/*
				if (run2.getValue() == null)
				{
					// interrupted
					return;
				}
				if (run2.getValue() instanceof Exception &&
					! (run2.getValue() instanceof XMLErrorMessage))
				{
					throw (Exception) run2.getValue();
				}
				else if (! (run2.getValue() instanceof IXMLEncodable))
				{
					throw new Exception("Illegal return value!");
				}

				IXMLEncodable xmlReply = (IXMLEncodable) run2.getValue();*/

				long amount =
					Long.parseLong( (String) ( (Hashtable) run.getValue()).get(XMLTransactionOverview.
					KEY_AMOUNT));
				String planName = (String) ( (Hashtable) run.getValue()).get(XMLTransactionOverview.
					KEY_VOLUMEPLAN);
				String paymentMethod = (String) ( (Hashtable) run.getValue()).get(XMLTransactionOverview.KEY_PAYMENTMETHOD);


/*
				//biConn will return XMLErrorMessage if payment is active (= no matching record in passivepayments)
				//(the transfers table alone does not associate a payment method or type with a transfernumber)
				if (xmlReply instanceof XMLErrorMessage)
				{
					XMLErrorMessage repliedMessage = (XMLErrorMessage) xmlReply;
					if (repliedMessage.getErrorCode() ==
						XMLErrorMessage.ERR_NO_RECORD_FOUND)
					{
						TransactionOverviewDialog.showActivePaymentDialog(
							jap.JAPConf.getInstance(), new Long(transferNumber).toString(),
							amount, selectedAccount, planName);
					}
					else
					{
						JAPDialog.showMessageDialog(
							AccountSettingsPanel.this.getRootPanel(),
							//JAPMessages.getString(MSG_DETAILS_FAILED));
							JAPMessages.getString(TransactionOverviewDialog.MSG_DETAILS_FAILED));
					}
				}
				else
				{
					TransactionOverviewDialog.showPassivePaymentDialog(jap.JAPConf.getInstance(),
						(XMLPassivePayment) xmlReply,
						transferNumber, selectedAccount.getAccountNumber());
				}*/

				TransactionOverviewDialog.showActivePaymentDialog(
								jap.JAPConf.getInstance(), new Long(transferNumber).toString(),
								amount, selectedAccount, planName, paymentMethod);

			}
			catch (Exception e)
			{
				LogHolder.log(LogLevel.EXCEPTION, LogType.PAY,
							  "Cannot connect to Payment Instance!", e);
				showPIerror(jap.JAPConf.getInstance().getContentPane(), e);
			}
		}
	}

	/**
	 *
	 * @return boolean
	 */
	public void doCreateAccount(final String a_biid)
	{
		final JAPDialog a_parentDialog = new JAPDialog(getRootPanel(), JAPMessages.getString(MSG_ACCOUNTCREATE), true);
		a_parentDialog.setDefaultCloseOperation(JAPDialog.DO_NOTHING_ON_CLOSE);
		a_parentDialog.setResizable(false);

		/******************** get available JPIs   ********************/
		WorkerContentPane.IReturnRunnable fetchJpisThread = new WorkerContentPane.IReturnRunnable()
		{
				private Vector allJpis; //Vector<PaymentInstanceDBEntry>

			public void run()
			{
					if (a_biid != null) //specific jpi given (the correct jpi for the currently selected pay-mixcascade)
						{
						PaymentInstanceDBEntry m_selectedJpi = PayAccountsFile.getInstance().getBI(a_biid);
						allJpis = new Vector();
						allJpis.addElement(m_selectedJpi);
						}
					else //get jpis for which accounts exist
					{
						allJpis = PayAccountsFile.getInstance().getPaymentInstances();
					}
				}

	            public Object getValue()
				{
					return allJpis;
				}

			};
			final WorkerContentPane fetchJpisPane = new WorkerContentPane(a_parentDialog,
				"","" , fetchJpisThread)
				{
				public boolean isMoveForwardAllowed()
				{
					return a_parentDialog.isVisible() ;
				}
		};

		/******************* show JPI selection pane    *************/

		final JpiSelectionPane jpiPane = new JpiSelectionPane(a_parentDialog, fetchJpisPane,a_biid)
		{
			public boolean isSkippedAsNextContentPane()
			{
				return isSkippedAsContentPane();
			}

			public boolean isSkippedAsContentPane()
			{
				Vector allJpis = (Vector) fetchJpisPane.getValue();
				if ( (a_biid != null) || (allJpis.size() <= 1)) //specific bi given or only one is available
				{
					return true;
				}
				else
				{
					return false;
				}
			}

			public boolean isSkippedAsPreviousContentPane()
			{
				return isSkippedAsContentPane();
			}
		};

		/*********************** test connection to payment instance *****************/
		Runnable piTestThread = new Runnable()
		{
			public void run()
			{
				if (jpiPane.getSelectedPaymentInstance() == null)
				{
					Thread.currentThread().interrupt();
					return;
				}
				BIConnection biconn = null;
				try
				{
					//Check if payment instance is reachable
					biconn = new BIConnection( jpiPane.getSelectedPaymentInstance() );
					biconn.connect(JAPModel.getInstance().getPaymentProxyInterface());
					biconn.disconnect();
				}
				catch (Exception e)
				{
					if (biconn != null)
					{
						try
						{
							biconn.disconnect();
						}
						catch (Exception ex)
						{
						}
					}

					if (!Thread.currentThread().isInterrupted())
					{
						showPIerror(a_parentDialog.getContentPane(), e);
						Thread.currentThread().interrupt();
					}
				}
			}
		};


		WorkerContentPane PITestWorkerPane = new WorkerContentPane(a_parentDialog,
			JAPMessages.getString(MSG_TEST_PI_CONNECTION) + "...", jpiPane, piTestThread);
		PITestWorkerPane.setInterruptThreadSafe(false);


		//************ fetch AGBs (terms and conditions) *******//
		final WorkerContentPane fetchTermsPane = new WorkerContentPane(a_parentDialog,
			JAPMessages.getString(MSG_FETCHINGTERMS), jpiPane ,
			new FetchTermsRunnable(a_parentDialog, jpiPane))
		{
			public boolean isMoveForwardAllowed()
			{
				return a_parentDialog.isVisible() ;
			}
		};
		fetchTermsPane.setInterruptThreadSafe(false);

		//************ show terms and conditions, only moving forward if user confirms  ******//
		final TermsAndConditionsPane termsPane =
			new TermsAndConditionsPane(a_parentDialog, fetchTermsPane, true)
		{
			public boolean isSkippedAsPreviousContentPane()
			{
				return true;
			}
		};


	   //****************** fetch cancellation policy **********************//
	   WorkerContentPane.IReturnRunnable fetchPolicy = new WorkerContentPane.IReturnRunnable()
	   {
		   private XMLGenericText cancellationPolicy;
		   public void run()
		   {
			   BIConnection piConn = null;
			   try
			   {
				   PaymentInstanceDBEntry pi = jpiPane.getSelectedPaymentInstance();
				   piConn = new BIConnection(pi);
				   piConn.connect(JAPModel.getInstance().getPaymentProxyInterface());

				   //authentication is neither necessary nor possible (creating first account -> user does not yet have an account to authenticate with)
				   LogHolder.log(LogLevel.DEBUG, LogType.PAY, "Fetching cancellation policy");
				   String lang = JAPMessages.getLocale().getLanguage();
				   cancellationPolicy = piConn.getCancellationPolicy(lang);
				   piConn.disconnect();
			   }
			   catch (Exception e)
			   {
				   if (piConn != null)
				   {
					   try
					   {
						   piConn.disconnect();
					   }
					   catch (Exception ex)
					   {
					   }
				   }

				   if (!Thread.currentThread().isInterrupted())
				   {
					   LogHolder.log(LogLevel.EXCEPTION, LogType.NET,
									 "Error fetching cancellation policy: ", e);
					   showPIerror(a_parentDialog.getContentPane(), e);
					   Thread.currentThread().interrupt();
				   }
			   }
		   }

		   public Object getValue()
		   {
			   return cancellationPolicy;
		   }

	   };
	   final WorkerContentPane fetchPolicyPane = new WorkerContentPane(a_parentDialog,
		   JAPMessages.getString(MSG_FETCHINGPOLICY), termsPane , fetchPolicy)
	   {
		   public boolean isMoveForwardAllowed()
		   {
			   return a_parentDialog.isVisible() ;
		   }
	   };
		fetchTermsPane.setInterruptThreadSafe(false);

	   //************* show cancellation policy ***************************//
	   final CancellationPolicyPane policyPane = new CancellationPolicyPane(a_parentDialog, fetchPolicyPane)
	   {
		   public boolean isSkippedAsPreviousContentPane()
		   {
			   return true;
		   }
		};

	    /*************** create keypair ****************/
		final WorkerContentPane.IReturnRunnable keyCreationThread = new WorkerContentPane.IReturnRunnable()
		{
			private DSAKeyPair m_keyPair;

			public void run()
			{
				m_bDoNotCloseDialog = true;
				m_keyPair =
					DSAKeyPair.getInstance(new SecureRandom(), DSAKeyPair.KEY_LENGTH_1024, 20);
				if (m_keyPair == null)
				{
					JAPDialog.showErrorDialog(
						a_parentDialog, JAPMessages.getString(MSG_KEY_PAIR_CREATE_ERROR), LogType.PAY);
				}
				m_bDoNotCloseDialog = false;
			}

			public Object getValue()
			{
				return m_keyPair;
			}
		};
		final WorkerContentPane keyWorkerPane = new WorkerContentPane(
			a_parentDialog, JAPMessages.getString(MSG_CREATE_KEY_PAIR) + "...", policyPane, keyCreationThread);
		keyWorkerPane.getButtonCancel().setEnabled(false);

		m_bReady = true;

		/******************* create account **************/
		final IReturnAccountRunnable doIt = new IReturnAccountRunnable()
		{
			private PayAccount m_payAccount;
			private IOException m_connectionError;

			public void run()
			{
				m_bReady = false;
				while (!Thread.currentThread().isInterrupted())
				{
					try
					{
						IMutableProxyInterface payProxy = JAPModel.getInstance().getPaymentProxyInterface();
						DSAKeyPair accountKeys = (DSAKeyPair) keyWorkerPane.getValue();
						PaymentInstanceDBEntry jpi = jpiPane.getSelectedPaymentInstance();
						XMLGenericText agreedTerms = (XMLGenericText) fetchTermsPane.getValue();

						m_payAccount = PayAccountsFile.getInstance().createAccount(jpi,payProxy,accountKeys,agreedTerms);

						m_payAccount.fetchAccountInfo(JAPModel.getInstance().getPaymentProxyInterface(), true);
						break;
					}
					catch (IOException a_e)
					{
						m_connectionError = a_e;
					}
					catch (Exception ex)
					{
						if (!Thread.currentThread().isInterrupted() && ex.getMessage() != null &&
							!ex.getMessage().equals("CAPTCHA"))
						{
							//User has not pressed cancel and no io exception occured
							showPIerror(a_parentDialog.getContentPane(), ex);
						}
						else
						{
							LogHolder.log(LogLevel.WARNING, LogType.GUI, ex);
						}

						Thread.currentThread().interrupt();
						break;
					}
				}
				m_connectionError = null;
			}

			public PayAccount getAccount()
			{
				Object account = getValue();
				if (account instanceof PayAccount)
				{
					return (PayAccount) account;
				}
				return null;
			}

			public Object getValue()
			{
				if (m_connectionError != null)
				{
					return m_connectionError;
				}
				return m_payAccount;
			}
		};


		AccountSettingsPanel.AccountCreationPane accountCreatedPane = this.new AccountCreationPane(a_parentDialog, JAPMessages.getString(MSG_ACCOUNTCREATEDESC), keyWorkerPane, doIt);
		accountCreatedPane.setInterruptThreadSafe(false);

		final CaptchaContentPane captcha = new CaptchaContentPane(a_parentDialog, accountCreatedPane)
		{
			public void gotCaptcha(ICaptchaSender a_source, IImageEncodedCaptcha a_captcha)
			{
				if (keyCreationThread.getValue() != null)
				{
					// we might receive a capta from a previous request; ignore it!
					super.gotCaptcha(a_source, a_captcha);
				}
			}
		};
		GregorianCalendar calendar = new GregorianCalendar();
		if ( ( (calendar.get(GregorianCalendar.DAY_OF_MONTH) == 27 &&
				calendar.get(GregorianCalendar.MONTH) == GregorianCalendar.SEPTEMBER) ||
			  (calendar.get(GregorianCalendar.DAY_OF_MONTH) == 4 &&
			   calendar.get(GregorianCalendar.MONTH) == GregorianCalendar.NOVEMBER)))
		{
			captcha.getButtonNo().setText(JAPMessages.getString(MSG_NEWCAPTCHAEASTEREGG));
		}
		else
		{
			captcha.getButtonNo().setText(JAPMessages.getString(MSG_NEWCAPTCHA));
		}

		PayAccountsFile.getInstance().addPaymentListener(captcha);
		captcha.addComponentListener(new ComponentAdapter()
		{
			public void componentShown(ComponentEvent a_event)
			{
				try
				{
					if (doIt.getValue() instanceof IOException)
					{
						captcha.printErrorStatusMessage(
							JAPMessages.getString(MSG_BI_CONNECTION_LOST), LogType.NET);
					}
				}
				catch (Exception a_e)
				{

				}
				m_bDoNotCloseDialog = false;
			}
		});

		/******** ask for a password if the newly created account is the user's first account ***********/
		//final boolean bFirstPayAccount = PayAccountsFile.getInstance().getNumAccounts() == 0;
		final boolean bFirstPayAccount = false;
		PasswordContentPane pc = new PasswordContentPane(a_parentDialog, captcha,
			PasswordContentPane.PASSWORD_NEW,
			JAPMessages.getString(MSG_ACCPASSWORD))
		{
			public CheckError[] checkYesOK()
			{
				CheckError[] errors = super.checkYesOK();

				if (errors == null || errors.length == 0)
				{
					setButtonValue(RETURN_VALUE_OK);
					if (getPassword() != null)
					{
						JAPController.getInstance().setPaymentPassword(new String(getPassword()));
					}
					else
					{
						JAPController.getInstance().setPaymentPassword("");
					}
				}
				return errors;
			}

			public boolean isSkippedAsNextContentPane()
			{
				return a_parentDialog.isVisible() && !bFirstPayAccount;
			}

			public boolean isSkippedAsPreviousContentPane()
			{
				return a_parentDialog.isVisible() && !bFirstPayAccount;
			}
		};

		/*********** save account in configuration *********/
		final IReturnBooleanRunnable exportThread = new IReturnBooleanRunnable()
		{
			private Boolean m_bAccountSaved = new Boolean(false);

			public void run()
			{
				// save all accounts to the config file
				m_bDoNotCloseDialog = true;
				if (JAPController.getInstance().saveConfigFile())
				{
					// an error occured while saving the configuration
					JAPDialog.showErrorDialog(a_parentDialog, JAPMessages.getString(
						JAPController.MSG_ERROR_SAVING_CONFIG,
						JAPModel.getInstance().getConfigFile()), LogType.MISC);
					try
					{
						if (exportAccount(doIt.getAccount(), a_parentDialog.getContentPane(),
										  JAPController.getInstance().getPaymentPassword()))
						{
							m_bAccountSaved = new Boolean(true);
						}
						else
						{
							m_bAccountSaved = new Boolean(false);
						}
					}
					catch (Exception a_e)
					{
						LogHolder.log(LogLevel.EXCEPTION, LogType.MISC, a_e);
					}
				}
				else
				{
					m_bAccountSaved = new Boolean(true);
				}
				m_bDoNotCloseDialog = false;
			}

			public Object getValue()
			{
				if (!a_parentDialog.isVisible())
				{
					return new Boolean(true);
				}
				return m_bAccountSaved;
			}

			public boolean isTrue()
			{
				return ( (Boolean) getValue()).booleanValue();
			}
		};

		WorkerContentPane saveConfig =
			new WorkerContentPane(a_parentDialog, JAPMessages.getString(MSG_SAVE_CONFIG) + WorkerContentPane.DOTS,
								  pc, exportThread)
		{
			public boolean isMoveBackAllowed()
			{
				return false;
			}
		};
		saveConfig.getButtonCancel().setEnabled(false);

	    //********* show errors if any *********//
		DialogContentPane saveErrorPane = new SimpleWizardContentPane(
			a_parentDialog, "<Font color=\"red\">" + JAPMessages.getString(MSG_CREATED_ACCOUNT_NOT_SAVED) + "</Font>",
			new DialogContentPane.Layout("", DialogContentPane.MESSAGE_TYPE_ERROR),
			new DialogContentPane.Options(saveConfig))
		{
			public boolean isSkippedAsNextContentPane()
			{
				return ( (Boolean) exportThread.getValue()).booleanValue();
			}

			public boolean isSkippedAsPreviousContentPane()
			{
				return true;
			}
		};
		saveErrorPane.getButtonCancel().setVisible(false);

		final Vector vecTan = new Vector();
		a_parentDialog.addWindowListener(new WindowAdapter()
		{
			public void windowClosing(WindowEvent e)
			{
				if (!m_bDoNotCloseDialog)
				{
					if (captcha.isVisible())
					{
						captcha.setButtonValue(IDialogOptions.RETURN_VALUE_CLOSED);
						captcha.checkCancel();
					}
					a_parentDialog.dispose();
				}
			}

			public void windowClosed(WindowEvent a_event)
			{
				// vecTan must be filled if charging was ok
				PayAccountsFile.getInstance().removePaymentListener(captcha);
				Object value = doIt.getValue();
				if (vecTan.size() == 0 && value != null && value instanceof PayAccount)
				{
					PayAccountsFile.getInstance().deleteAccount(
						( (PayAccount) value).getAccountNumber());
				}

				updateAccountList();

				if (vecTan.size() != 0 && value != null && value instanceof PayAccount)
				{
					/** Select new account */
					m_listAccounts.setSelectedValue(value, true);
					PayAccount account = getSelectedAccount();
					if (account != null)
					{
						account.updated();
					}
					/*
						  if ( ( (Boolean) exportThread.getValue()).booleanValue())
						  {
					 doChargeAccount(doIt.getAccount());
						  }*/
				}
			}
		});

		m_bDoNotCloseDialog = false;
		//immediately charge account after creating it
		doChargeAccount(doIt, a_parentDialog, saveErrorPane, exportThread, vecTan,true);
		DialogContentPane.updateDialogOptimalSized(fetchJpisPane);
		captcha.setText(captcha.getText() + " " + JAPMessages.getString(MSG_NEW_CAPTCHA_HINT,
			JAPMessages.getString(MSG_NEWCAPTCHA)));
		a_parentDialog.setLocationCenteredOnOwner();
		a_parentDialog.setVisible(true);

	}

	/**
	 * doActivateAccount
	 *
	 * @param payAccount PayAccount
	 */
	private void doSelectAccount(PayAccount selectedAccount)
	{
		if (selectedAccount == null)
		{
			return;
		}

		if (JAPController.getInstance().getAnonMode() && !hasDisconnected())
		{
			return;
		}

		PayAccountsFile accounts = PayAccountsFile.getInstance();
		try
		{
			accounts.setActiveAccount(selectedAccount.getAccountNumber());
		}
		catch (Exception ex)
		{
			JAPDialog.showErrorDialog(GUIUtils.getParentWindow(this.getRootPanel()),
									  JAPMessages.getString("Could not select account!"), LogType.PAY, ex);
		}
	}

	private DialogContentPane createUpdateAccountPane(final IReturnAccountRunnable a_accountCreationThread,
		final MethodSelectionPane a_methodSelectionPane,
		final JAPDialog a_parentDialog, DialogContentPane a_previousContentPane)
	{
		Runnable t = new Runnable()
		{
			public void run()
			{
				try
				{
					a_accountCreationThread.getAccount().fetchAccountInfo(
						JAPModel.getInstance().getPaymentProxyInterface(), true);
					updateAccountList();
				}
				catch (Exception e)
				{
					if (!Thread.currentThread().isInterrupted())
					{
						showPIerror(a_parentDialog.getContentPane(), e);
						LogHolder.log(LogLevel.EXCEPTION, LogType.PAY, "Could not get account statement");
						Thread.currentThread().interrupt();
					}
				}
			}
		};
		WorkerContentPane worker = new WorkerContentPane(a_parentDialog,
			JAPMessages.getString(MSG_GETACCOUNTSTATEMENT), a_previousContentPane, t)
		{
			public boolean isSkippedAsNextContentPane()
			{
				if (a_methodSelectionPane == null ||
					a_methodSelectionPane.getSelectedPaymentOption() == null ||
					a_methodSelectionPane.getSelectedPaymentOption().getType().equalsIgnoreCase(
									   XMLPaymentOption.OPTION_PASSIVE) ||
					a_methodSelectionPane.getSelectedPaymentOption().getType().equalsIgnoreCase(
									   XMLPaymentOption.OPTION_MIXED) )
				{
					return false;
				}
				else
				{
					return true;
				}
			}

			public boolean isSkippedAsPreviousContentPane()
			{
				return isSkippedAsNextContentPane();
			}
		};
		worker.setInterruptThreadSafe(false);
		return worker;
	}

	public void updateAccountShown()
	{
		doGetStatement( (PayAccount) m_listAccounts.getSelectedValue());
	}

	/**
	 * doGetStatement - fetches an account statement
	 *
	 */
	private void doGetStatement(final PayAccount a_selectedAccount)
	{
		if (a_selectedAccount == null)
		{
			return;
		}
		JAPDialog busy = new JAPDialog(GUIUtils.getParentWindow(getRootPanel()),
									   JAPMessages.getString(MSG_GETACCOUNTSTATEMENTTITLE), true);
		DialogContentPane worker =
			createUpdateAccountPane(new FixedReturnAccountRunnable(a_selectedAccount), null, busy, null);
		worker.updateDialog();
		busy.pack();
		busy.setLocationCenteredOnOwner();
		busy.setVisible(true);
	}

	/**
	 * doExportAccount
	 *
	 * @param payAccount PayAccount
	 */
	private void doExportAccount(PayAccount selectedAccount)
	{
		if (selectedAccount == null)
		{
			return;
		}
		if (selectedAccount.getPrivateKey() != null)
		{
			JAPDialog d = new JAPDialog(GUIUtils.getParentWindow(this.getRootPanel()),
										JAPMessages.getString(MSG_ACCPASSWORDTITLE), true);

			PasswordContentPane p;

			if (JAPController.getInstance().getPaymentPassword() != null)
			{
				p = new PasswordContentPane(d, PasswordContentPane.PASSWORD_CHANGE,
											JAPMessages.getString(MSG_EXPORTENCRYPT))
				{
					public char[] getComparedPassword()
					{
						return JAPController.getInstance().getPaymentPassword().toCharArray();
					}

					public String getOldPasswordLabel()
					{
						return JAPMessages.getString(PasswordContentPane.MSG_ENTER_LBL);
					}

					public String getNewPasswordLabel()
					{
						return JAPMessages.getString(MSG_PASSWORD_EXPORT);
					}
				};
			}
			else
			{
				p = new PasswordContentPane(d, PasswordContentPane.PASSWORD_NEW,
											JAPMessages.getString(MSG_EXPORTENCRYPT))
				{
					public String getNewPasswordLabel()
					{
						return JAPMessages.getString(MSG_PASSWORD_EXPORT);
					}
				};
			}
			p.updateDialog();
			d.pack();
			d.setVisible(true);
			if (p.getButtonValue() == PasswordContentPane.RETURN_VALUE_OK)
			{
				if (exportAccount(selectedAccount, this.getRootPanel(), new String(p.getPassword())))
				{
					selectedAccount.setBackupDone(true);
					doShowDetails(selectedAccount);
				}
			}
		}
		else
		{
			// account is already encrypted, save it only
			if (exportAccount(selectedAccount, this.getRootPanel(), null))
			{
				selectedAccount.setBackupDone(true);
				doShowDetails(selectedAccount);
			}
		}
	}

	private boolean exportAccount(PayAccount selectedAccount, Component a_parent, String strPassword)
	{
		JFileChooser chooser = new JFileChooser();
		chooser.setSelectedFile(new File(selectedAccount.getAccountNumber() + MyFileFilter.ACCOUNT_EXTENSION));
		MyFileFilter filter = new MyFileFilter();
		chooser.setFileFilter(filter);
		while (true)
		{
			int returnVal = chooser.showSaveDialog(a_parent);
			if (returnVal == JFileChooser.APPROVE_OPTION)
			{
				try
				{
					File f = chooser.getSelectedFile();
					if (!f.getName().toLowerCase().endsWith(MyFileFilter.ACCOUNT_EXTENSION))
					{
						f = new File(f.getParent(), f.getName() + MyFileFilter.ACCOUNT_EXTENSION);
					}
					if (f.exists())
					{
						if (!JAPDialog.showYesNoDialog(GUIUtils.getParentWindow(this.getRootPanel()),
							JAPMessages.getString(MSG_FILE_EXISTS)))
						{
							continue;
						}
					}

					Document doc = XMLUtil.createDocument();
					Element elemRoot = doc.createElement("root");
					elemRoot.setAttribute("filetype", "JapAccountFile");
					elemRoot.setAttribute("version", "1.1");

					doc.appendChild(elemRoot);
					Element elemAccount = selectedAccount.toXmlElement(doc, strPassword);
					elemRoot.appendChild(elemAccount);
					/*
					  if (strPassword != null && strPassword.length() > 0)
					  {
					 XMLEncryption.encryptElement(elemAccount, strPassword);
					  }*/

					String strOutput = XMLUtil.toString(XMLUtil.formatHumanReadable(doc));
					FileOutputStream outStream = new FileOutputStream(f);
					outStream.write(strOutput.getBytes());
					outStream.close();
					JAPDialog.showMessageDialog(GUIUtils.getParentWindow(this.getRootPanel()),
												JAPMessages.getString(MSG_EXPORTED));
					return true;
				}
				catch (Exception e)
				{
					JAPDialog.showErrorDialog(GUIUtils.getParentWindow(a_parent),
											  JAPMessages.getString(MSG_NOTEXPORTED) + ": " + e, LogType.PAY);
				}
			}
			break;
		}
		return false;
	}

	/**
	 * Filefilter for the import function
	 *
	 * @author Bastian Voigt
	 * @version 1.0
	 */
	private static class MyFileFilter extends FileFilter
	{
		public static final String ACCOUNT_EXTENSION = ".acc";
		private final String ACCOUNT_DESCRIPTION = "JAP Accountfile (*" + ACCOUNT_EXTENSION + ")";

		private int filterType;

		public int getFilterType()
		{
			return filterType;
		}

		public boolean accept(File f)
		{
			return f.isDirectory() || f.getName().endsWith(ACCOUNT_EXTENSION);
		}

		public String getDescription()
		{
			return ACCOUNT_DESCRIPTION;
		}
	}

	/**
	 * doImportAccount - imports an account from a file
	 */
	private void doImportAccount()
	{
		PayAccount importedAccount = null;
		Element elemAccount = null;
		JFileChooser chooser = new JFileChooser();
		MyFileFilter filter = new MyFileFilter();
		chooser.setFileFilter(filter);
		int returnVal = chooser.showOpenDialog(getRootPanel());
		if (returnVal == JFileChooser.APPROVE_OPTION)
		{
			File f = chooser.getSelectedFile();
			try
			{
				Document doc = XMLUtil.readXMLDocument(f);
				XMLUtil.removeComments(doc);
				Element elemRoot = doc.getDocumentElement();
				elemAccount = (Element) XMLUtil.getFirstChildByName(elemRoot, PayAccount.XML_ELEMENT_NAME);

				// maybe it was encrypted; only for compatibility with old export format 1.0, remove!
				if (elemAccount == null)
				{
					Element elemCrypt =
						(Element) XMLUtil.getFirstChildByName(elemRoot, XMLEncryption.XML_ELEMENT_NAME);
					if (elemCrypt != null)
					{
						String strPassword = null;

						while (true)
						{
							JAPDialog d = new JAPDialog(GUIUtils.getParentWindow(this.getRootPanel()),
								JAPMessages.getString(MSG_ACCPASSWORDTITLE), true);
							PasswordContentPane p = new PasswordContentPane(d,
								PasswordContentPane.PASSWORD_ENTER, "");
							p.updateDialog();
							d.pack();
							d.setVisible(true);
							if (p.getButtonValue() != PasswordContentPane.RETURN_VALUE_CANCEL &&
								p.getButtonValue() != PasswordContentPane.RETURN_VALUE_CLOSED)
							{
								strPassword = new String(p.getPassword());
							}
							if (strPassword == null)
							{
								break;
							}
							try
							{
								elemAccount = XMLEncryption.decryptElement(elemCrypt, strPassword);
							}
							catch (Exception ex)
							{
								strPassword = null;
								continue;
							}
							break ;
						}
					}
				}
			}
			catch (Exception e)
			{
				JAPDialog.showErrorDialog(getRootPanel(),
										  JAPMessages.getString(MSG_ACCOUNT_IMPORT_FAILED), LogType.MISC, e);
			}
			try
			{
				if (elemAccount != null)
				{
					XMLUtil.removeComments(elemAccount);

					importedAccount = new PayAccount(elemAccount, null);
					importedAccount.setBackupDone(true); // we know there is a backup file...
					PayAccountsFile accounts = PayAccountsFile.getInstance();
					accounts.addAccount(importedAccount);
					doActivateAccount(importedAccount);
					updateAccountList();
					doGetStatement(importedAccount);
				}
			}
			catch (Exception ex)
			{
				String message = "";

				if (ex instanceof PayAccountsFile.AccountAlreadyExisting)
				{
					message = JAPMessages.getString(MSG_ACCOUNT_ALREADY_EXISTING);
				}
				JAPDialog.showErrorDialog(getRootPanel(),
										  JAPMessages.getString(MSG_ACCOUNT_IMPORT_FAILED) + message,
										  LogType.MISC, ex);
			}
		}
	}

	private void doActivateAccount(PayAccount a_selectedAccount)
	{
		if (a_selectedAccount != null)
		{
			Enumeration accounts;
			PayAccount currentAccount;
			JAPDialog dialog = new JAPDialog(getRootPanel(), JAPMessages.getString(MSG_ACCPASSWORDTITLE));
			dialog.setDefaultCloseOperation(JAPDialog.HIDE_ON_CLOSE);
			PasswordContentPane contentPane =
				new PasswordContentPane(dialog, PasswordContentPane.PASSWORD_ENTER,
										JAPMessages.getString(MSG_GIVE_ACCOUNT_PASSWORD));
			contentPane.setDefaultButtonOperation(DialogContentPane.ON_CLICK_HIDE_DIALOG);
			contentPane.updateDialog();
			dialog.pack();
			try
			{
				a_selectedAccount.decryptPrivateKey(contentPane);

				// try to decrypt all inactive accounts with this password
				try
				{
					accounts = PayAccountsFile.getInstance().getAccounts();
					while (accounts.hasMoreElements())
					{
						currentAccount = (PayAccount) accounts.nextElement();
						currentAccount.decryptPrivateKey(
							new SingleStringPasswordReader(contentPane.getPassword()));
					}
				}
				catch (Exception a_e)
				{
					LogHolder.log(LogLevel.EXCEPTION, LogType.GUI, a_e);
				}

				// set the active account if none exists
				if (PayAccountsFile.getInstance().getActiveAccount() == null)
				{
					if (a_selectedAccount.getPrivateKey() != null)
					{
						PayAccountsFile.getInstance().setActiveAccount(a_selectedAccount);
					}
					else
					{
						accounts = PayAccountsFile.getInstance().getAccounts();
						while (accounts.hasMoreElements())
						{
							currentAccount = (PayAccount) accounts.nextElement();
							if (currentAccount.getPrivateKey() != null)
							{
								PayAccountsFile.getInstance().setActiveAccount(currentAccount);
							}
						}
					}
				}

				doShowDetails(a_selectedAccount);
				enableDisableButtons();
				m_listAccounts.repaint();

				if (a_selectedAccount.getPrivateKey() != null)
				{
					JAPDialog.showMessageDialog(getRootPanel(),
												JAPMessages.getString(MSG_ACTIVATION_SUCCESSFUL));
				}
			}
			catch (Exception a_e)
			{
				JAPDialog.showErrorDialog(getRootPanel(),
										  JAPMessages.getString(MSG_ACTIVATION_FAILED), LogType.PAY, a_e);
			}
			dialog.dispose();
		}
	}

	private boolean hasDisconnected()
	{
		if (JAPDialog.RETURN_VALUE_OK ==
			JAPDialog.showConfirmDialog(GUIUtils.getParentWindow(getRootPanel()),
										JAPMessages.getString(MSG_CONNECTIONACTIVE_QUESTION),
										JAPMessages.getString(JAPDialog.MSG_TITLE_WARNING),
										new JAPDialog.Options(JAPDialog.OPTION_TYPE_OK_CANCEL)
		{
			public String getYesOKText()
			{
				return JAPMessages.getString(JAPDialog.MSG_PROCEED);
			}
		}, JAPDialog.MESSAGE_TYPE_WARNING, null))
		{
			JAPDialog dialog = new JAPDialog(GUIUtils.getParentWindow(getRootPanel()),
											 JAPMessages.getString(WorkerContentPane.MSG_PLEASE_WAIT) +
											 WorkerContentPane.DOTS);
			WorkerContentPane pane = new WorkerContentPane(dialog,
				JAPMessages.getString(WorkerContentPane.MSG_PLEASE_WAIT),
				new Runnable()
			{
				public void run()
				{
					JAPController.getInstance().stopAnonModeWait();
				}
			});
			pane.updateDialog();
			dialog.pack();
			dialog.setResizable(false);
			dialog.setVisible(true);
			return true;
		}
		else
		{
			return false;
		}
	}

	/**
	 * doDeleteAccount
	 *
	 * @param payAccount PayAccount
	 */
	private void doDeleteAccount(PayAccount selectedAccount)
	{
		if (selectedAccount == null)
		{
			return;
		}
		PayAccountsFile accounts = PayAccountsFile.getInstance();
		boolean reallyDelete = false;

		if (accounts.getActiveAccount() == selectedAccount && JAPController.getInstance().getAnonMode() &&
			!hasDisconnected())
		{
				return;
		}

		if (!selectedAccount.hasAccountInfo())
		{
			boolean yes = JAPDialog.showYesNoDialog(GUIUtils.getParentWindow(this.getRootPanel()),
				JAPMessages.getString("ngDeleteAccountStatement"));

			if (yes)
			{
				doGetStatement(selectedAccount);
			}
		}
		if (selectedAccount.hasAccountInfo())
		{
			XMLAccountInfo accInfo = selectedAccount.getAccountInfo();
			if (accInfo.getBalance().getTimestamp().getTime() <
				(System.currentTimeMillis() - 1000 * 60 * 60 * 24))
			{
				boolean yes = JAPDialog.showYesNoDialog(GUIUtils.getParentWindow(this.getRootPanel()),
					JAPMessages.getString(MSG_OLDSTATEMENT));

				if (yes)
				{
					doGetStatement(selectedAccount);
				}
			}

			if (accInfo.getBalance().getCredit() > 0)
			{
				boolean yes = JAPDialog.showYesNoDialog(GUIUtils.getParentWindow(this.getRootPanel()),
					JAPMessages.getString("ngDeleteAccountCreditLeft"));

				if (yes)
				{
					reallyDelete = true;
				}
			}
			else
			{
				boolean yes = JAPDialog.showYesNoDialog(GUIUtils.getParentWindow(this.getRootPanel()),
					JAPMessages.getString("ngReallyDeleteAccount"));

				if (yes)
				{
					reallyDelete = true;
				}
			}
		}
		else
		{
			boolean yes = JAPDialog.showYesNoDialog(GUIUtils.getParentWindow(this.getRootPanel()),
				JAPMessages.getString("ngReallyDeleteAccount"));

			if (yes)
			{
				reallyDelete = true;
			}
		}
		if (reallyDelete)
		{
			try
			{
				m_listAccounts.clearSelection();
				accounts.deleteAccount(selectedAccount.getAccountNumber());
				updateAccountList();
				doShowDetails(getSelectedAccount());
			}
			catch (Exception a_ex)
			{
				JAPDialog.showErrorDialog(GUIUtils.getParentWindow(getRootPanel()),
										  JAPMessages.getString(MSG_ERROR_DELETING), LogType.MISC, a_ex);
			}
		}
	}

	public String getHelpContext()
	{
		int index = 0;
		index = m_tabPane.getSelectedIndex();
		switch (index)
		{
			case 1:
				return "payment_extend";
			default:
				return "payment_account";
		}
	}

	/**
	 * This method can be overwritten by the children of AbstractJAPConfModule. It is called
	 * every time the root panel comes to the foreground (is set to visible).
	 */
	protected void onRootPanelShown()
	{
		updateAccountList();
	}

	/**
	 * This method can be overwritten by the children of AbstractJAPConfModule. It is called
	 * every time the user presses "OK" in the configuration dialog.
	 */
	protected boolean onOkPressed()
	{
		JAPController.getInstance().setDontAskPayment(!m_cbxShowPaymentConfirmation.isSelected());
		JAPModel.getInstance().allowPaymentViaDirectConnection(m_cbxAllowNonAnonymousConnection.isSelected());
		PayAccountsFile.getInstance().setIgnoreAIAccountError(!m_cbxShowAIErrors.isSelected());
		PayAccountsFile.getInstance().setBalanceAutoUpdateEnabled(m_cbxBalanceAutoUpdateEnabled.isSelected());
		JAPController.getInstance().setAskSavePayment(m_cbxAskIfNotSaved.isSelected());
		BIConnection.setConnectionTimeout( ( (Integer) m_comboTimeout.getSelectedItem()).intValue() * 1000);
		return true;
	}

	/**
	 * This method can be overwritten by the children of AbstractJAPConfModule. It is called
	 * every time the user presses "Cancel" in the configuration dialog after the restoring
	 * of the savepoint data (if there is a savepoint for this module).
	 */
	protected void onCancelPressed()
	{
		// it does not make sense to do anything here IMO...
	}

	/**
	 * This method can be overwritten by the children of AbstractJAPConfModule. It is called
	 * every time the user presses "Reset to defaults" in the configuration dialog after the
	 * restoring of the default configuration from the savepoint (if there is a savepoint for
	 * this module).
	 */
	protected void onResetToDefaultsPressed()
	{
		m_cbxShowPaymentConfirmation.setSelected(true);
		m_cbxAllowNonAnonymousConnection.setSelected(true);
		m_cbxShowAIErrors.setSelected(true);
		m_cbxAskIfNotSaved.setSelected(true);
		m_cbxBalanceAutoUpdateEnabled.setSelected(true);
		setConnectionTimeout(BIConnection.TIMEOUT_DEFAULT);
	}

	/**
	 * Fetches new (changed) account data from the PayAccountsFile
	 */
	protected void onUpdateValues()
	{
		m_cbxShowPaymentConfirmation.setSelected(!JAPController.getInstance().getDontAskPayment());
		m_cbxAllowNonAnonymousConnection.setSelected(
			JAPModel.getInstance().isPaymentViaDirectConnectionAllowed());
		m_cbxAskIfNotSaved.setSelected(JAPController.getInstance().isAskSavePayment());
		m_cbxShowAIErrors.setSelected(!PayAccountsFile.getInstance().isAIAccountErrorIgnored());
		m_cbxBalanceAutoUpdateEnabled.setSelected(PayAccountsFile.getInstance().isBalanceAutoUpdateEnabled());
		setConnectionTimeout(BIConnection.getConnectionTimeout());
		/*
		   PayAccountsFile accounts = PayAccountsFile.getInstance();
		   Enumeration enumAccounts = accounts.getAccounts();
		   while (enumAccounts.hasMoreElements())
		   {
		 PayAccount a = (PayAccount) enumAccounts.nextElement();
		   }*/
	}

	public void valueChanged(ListSelectionEvent e)
	{
		if (e.getSource() == m_listAccounts)
		{
			if (m_listAccounts.getModel().getSize() > 0)
			{
				doShowDetails(getSelectedAccount());
				enableDisableButtons();
			}
		}
	}

	public void showPIerror(Component a_parent, Exception a_e)
	{
		LogHolder.log(LogLevel.ERR, LogType.PAY, a_e);

		if (a_e instanceof XMLErrorMessage)
		{
			JAPDialog.showErrorDialog(a_parent,
									  PaymentMainPanel.translateBIError( (XMLErrorMessage) a_e), LogType.PAY);
		}
		else if (!JAPModel.getInstance().isAnonConnected() &&
				 !JAPModel.getInstance().isPaymentViaDirectConnectionAllowed())
		{
			int answer =
				JAPDialog.showConfirmDialog(a_parent,
											JAPMessages.getString(MSG_DIRECT_CONNECTION_FORBIDDEN),
											JAPDialog.OPTION_TYPE_YES_NO, JAPDialog.MESSAGE_TYPE_ERROR);
			if (answer == JAPDialog.RETURN_VALUE_YES)
			{
				m_cbxAllowNonAnonymousConnection.setSelected(true);
				JAPModel.getInstance().allowPaymentViaDirectConnection(true);
			}
		}
		else if (!JAPModel.getInstance().isAnonConnected())
		{
			JAPDialog.showErrorDialog(a_parent,
									  JAPMessages.getString(MSG_NO_ANONYMITY_POSSIBLY_BLOCKED), LogType.PAY);
		}
		else if (a_e instanceof ForbiddenIOException)
		{
			JAPDialog.showErrorDialog(a_parent, JAPMessages.getString(MSG_ERROR_FORBIDDEN), LogType.PAY);
		}
		else
		{
			JAPDialog.showErrorDialog(a_parent, JAPMessages.getString(MSG_CREATEERROR), LogType.PAY);
		}
	}

	class CustomRenderer extends DefaultListCellRenderer
	{
		public Component getListCellRendererComponent(JList list, Object value,
			int index, boolean isSelected, boolean cellHasFocus)
		{
			JLabel l;
			Component comp = super.getListCellRendererComponent(list, value,
				index, isSelected, cellHasFocus);
			if (comp instanceof JComponent && value != null && value instanceof PayAccount)
			{
				if ( ( (PayAccount) value).getPrivateKey() == null)
				{
					// encrypted account
					l = new JLabel(String.valueOf( ( (PayAccount) value).getAccountNumber()),
								   GUIUtils.loadImageIcon(AccountSettingsPanel.IMG_COINS_DISABLED, true),
								   LEFT);
				}
				else
				{
					l = new JLabel(String.valueOf( ( (PayAccount) value).getAccountNumber()),
								   GUIUtils.loadImageIcon(JAPConstants.IMAGE_COINS_FULL, true), LEFT);
				}
				if (isSelected)
				{
					l.setOpaque(true);
					l.setBackground(Color.lightGray);
				}
				Font f = l.getFont();
				if ( ( (PayAccount) value).equals(PayAccountsFile.getInstance().getActiveAccount()))
				{
					l.setFont(new Font(f.getName(), Font.BOLD, f.getSize()));
				}
				else
				{
					l.setFont(new Font(f.getName(), Font.PLAIN, f.getSize()));
				}
			}
			else
			{
				if (value != null)
				{
					l = new JLabel(value.toString());

				}
				else
				{
					l = new JLabel();
				}

			}

			return l;
		}
	}

	private static interface IReturnAccountRunnable extends WorkerContentPane.IReturnRunnable
	{
		public PayAccount getAccount();
	}

	private static interface IReturnBooleanRunnable extends WorkerContentPane.IReturnRunnable
	{
		public boolean isTrue();
	}

	private static final class FixedReturnAccountRunnable implements IReturnAccountRunnable
	{
		private PayAccount m_account;

		public FixedReturnAccountRunnable(PayAccount a_account)
		{
			m_account = a_account;
		}

		public Object getValue()
		{
			return m_account;
		}

		public PayAccount getAccount()
		{
			return m_account;
		}

		public void run()
		{}
	}

	private void setConnectionTimeout(int a_timeoutMS)
	{
		int timeout = a_timeoutMS / 1000;

		if (timeout >= ( (Integer) m_comboTimeout.getItemAt(m_comboTimeout.getItemCount() - 1)).intValue())
		{
			m_comboTimeout.setSelectedIndex(m_comboTimeout.getItemCount() - 1);
			BIConnection.setConnectionTimeout(
				( (Integer) m_comboTimeout.getSelectedItem()).intValue() * 1000);
		}
		else if (timeout <= ( (Integer) m_comboTimeout.getItemAt(0)).intValue())
		{
			m_comboTimeout.setSelectedIndex(0);
			BIConnection.setConnectionTimeout(
				( (Integer) m_comboTimeout.getSelectedItem()).intValue() * 1000);
		}
		else
		{
			for (int i = 1; i < m_comboTimeout.getItemCount(); i++)
			{
				if (timeout <= ( (Integer) m_comboTimeout.getItemAt(i)).intValue())
				{
					m_comboTimeout.setSelectedIndex(i);
					break;
				}
			}
		}
	}

	public class AccountCreationPane extends WorkerContentPane {
		public AccountCreationPane(JAPDialog a_parent, String a_text, WorkerContentPane a_pane, Runnable a_thread)
		{
			super(a_parent,a_text,a_pane,a_thread);
		}
		public boolean isReady()
		{
			return m_bReady;
		}

		public boolean isSkippedAsPreviousContentPane()
		{
			return false;
		}
	};

}
