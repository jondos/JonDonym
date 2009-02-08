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

/**
 * This class is the main payment view on JAP's main gui window
 *
 * @author Bastian Voigt, Tobias Bayer, Elmar Schraml
 * @version 1.0
 */
import java.sql.Timestamp;

import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

import anon.ErrorCodes;
import anon.infoservice.Database;
import anon.infoservice.MixCascade;
import anon.pay.AIControlChannel;
import anon.pay.PayAccount;
import anon.pay.PayAccountsFile;
import anon.pay.PaymentInstanceDBEntry;
import anon.pay.xml.XMLBalance;
import anon.pay.xml.XMLErrorMessage;
import anon.util.JobQueue;
import anon.util.captcha.ICaptchaSender;
import anon.util.captcha.IImageEncodedCaptcha;
import gui.FlippingPanel;
import gui.GUIUtils;
import gui.JAPMessages;
import gui.JAPProgressBar;
import gui.dialog.JAPDialog;
import jap.JAPConf;
import jap.JAPConstants;
import jap.JAPController;
import jap.JAPModel;
import jap.JAPNewView;
import jap.JAPUtil;
import jap.TrustModel;
import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;

public class PaymentMainPanel extends FlippingPanel
{
	public static final long WARNING_AMOUNT = 25 * 1000; // 25 MB (db stores in Kbyte!)
	public static final long WARNING_TIME = 1000 * 60 * 60 * 24 * 7; // seven days
	public static final long FULL_AMOUNT = WARNING_AMOUNT * 4; // 25 MB (db stores in Kbyte!)	

	/** Messages */
	private static final String MSG_TITLE =
		AccountSettingsPanel.MSG_ACCOUNT_FLAT_VOLUME;
		//PaymentMainPanel.class.getName() + "_title";
	private static final String MSG_LASTUPDATE = PaymentMainPanel.class.getName() +
		"_lastupdate";
	private static final String MSG_PAYMENTNOTACTIVE = PaymentMainPanel.class.getName() +
		"_paymentnotactive";
	private static final String MSG_NEARLYEMPTY_CREATE_ACCOUNT = PaymentMainPanel.class.getName() +
		"_nearlyEmptyCreateAccount";
	private static final String MSG_NEARLYEXPIRED_CREATE_ACCOUNT = PaymentMainPanel.class.getName() +
		"_nearlyExpiredCreateAccount";
	private static final String MSG_SESSIONSPENT = PaymentMainPanel.class.getName() +
		"_sessionspent";
	private static final String MSG_TOTALSPENT = PaymentMainPanel.class.getName() +
		"_totalspent";
	private static final String MSG_NO_ACTIVE_ACCOUNT = PaymentMainPanel.class.getName() +
		"_noActiveAccount";
	private static final String MSG_ENABLE_AUTO_SWITCH = PaymentMainPanel.class.getName() +
		"_enableAutoSwitch";
	private static final String MSG_WITH_COSTS = PaymentMainPanel.class.getName() +
	"_withCosts";
	private static final String MSG_CHOOSE_FREE_SERVICES_ONLY = PaymentMainPanel.class.getName() +
	"_chooseFreeServicesOnly";
	
	private static final String MSG_EXPERIMENTAL = PaymentMainPanel.class.getName() +
		"_experimental";
	private static final String MSG_TITLE_FLAT = PaymentMainPanel.class.getName() + "_title_flat";
	private static final String MSG_VALID_UNTIL =
		//PaymentMainPanel.class.getName() + "_valid_until";
		AccountSettingsPanel.MSG_ACCOUNT_VALID;
	private static final String MSG_EURO_BALANCE = PaymentMainPanel.class.getName() + "_euro_balance";
	private static final String MSG_NO_FLATRATE = PaymentMainPanel.class.getName() + "_no_flatrate";
	private static final String MSG_WANNA_CHARGE = PaymentMainPanel.class.getName() + "_wannaCharge";
	private static final String MSG_TT_CREATE_ACCOUNT = PaymentMainPanel.class.getName() + "_ttCreateAccount";
	private static final String MSG_FREE_OF_CHARGE = PaymentMainPanel.class.getName() + "_freeOfCharge";
	private static final String MSG_OPEN_TRANSACTION = PaymentMainPanel.class.getName() + "_openTransaction";
	private static final String MSG_CREATE_ACCOUNT_QUESTION = PaymentMainPanel.class.getName() + "_createAccountQuestion";




	private static final String[] MSG_PAYMENT_ERRORS = {"_xmlSuccess", "_xmlErrorInternal",
		"_xmlErrorWrongFormat", "_xmlErrorWrongData", "_xmlErrorKeyNotFound", "_xmlErrorBadSignature",
	"_xmlErrorBadRequest", "_xmlErrorNoAccountCert", "_xmlErrorNoBalance", "_xmlErrorNoConfirmation",
	"_accountempty", "_xmlErrorCascadeLength", "_xmlErrorDatabase", "_xmlErrorInsufficientBalance",
	"_xmlErrorNoFlatrateOffered", "_xmlErrorInvalidCode", "_xmlErrorInvalidCC", "_xmlErrorInvalidPriceCerts",
	"_xmlErrorMultipleLogin", "_xmlErrorNoRecordFound", "_xmlErrorPartialSuccess", "_xmlErrorAccountBlocked"};

	static
	{
		for (int i = 0; i < MSG_PAYMENT_ERRORS.length; i++)
		{
			MSG_PAYMENT_ERRORS[i] = PaymentMainPanel.class.getName() + MSG_PAYMENT_ERRORS[i];
		}
	}

	/**
	 * Icons for the account icon display
	 */
	private ImageIcon[] m_accountIcons;

	/** shows the current balance state */
	private JAPProgressBar m_BalanceProgressBar;
	private JAPProgressBar m_BalanceSmallProgressBar;

	/** shows the current balance as text */
	private JLabel m_BalanceText, m_BalanceTextSmall;

	private JobQueue m_queueUpdate;

	/** show the date of the last balance update */
	private JLabel m_dateLabel;

	/** the main jap window */
	private JAPNewView m_view;

	/** Listens to payment events */
	private MyPaymentListener m_MyPaymentListener = new MyPaymentListener();

	/** has user been notified about nearly empty accout? */
	private boolean m_notifiedEmpty = false;

	private boolean m_bShowingError = false;

	private JLabel m_labelTotalSpent;
	private JLabel m_labelSessionSpent;
	private JLabel m_labelTitle;
	private JLabel m_labelTitleSmall;
	private JLabel m_labelTotalSpentHeader;
	private JLabel m_labelSessionSpentHeader;

	private JLabel m_labelValidUntilHeader;
	private JLabel m_labelValidUntil;
	//private JLabel m_labelBalanceInEurosHeader;
	//private JLabel m_labelBalanceInEuros;

	private long m_spentThisSession;

	public PaymentMainPanel(final JAPNewView view, final JLabel a_alignLabel)
	{
		super(view);
		m_view = view;
		m_queueUpdate = new JobQueue("Payment Panel Update");

		loadIcons();
		JPanel fullPanel = new JPanel();
		fullPanel.setLayout(new GridBagLayout());
		GridBagConstraints c1 = new GridBagConstraints();
		m_labelTitle = new JLabel(JAPMessages.getString(MSG_TITLE) + ":");
		c1.insets = new Insets(0, 5, 0, 0);
		c1.anchor = GridBagConstraints.WEST;
		c1.weightx = 0;
		c1.fill = GridBagConstraints.HORIZONTAL;
		fullPanel.add(m_labelTitle, c1);
		JComponent spacer = new JPanel();
		Dimension spacerDimension = new Dimension(m_labelTitle.getFontMetrics(m_labelTitle.getFont()).
												  charWidth('9') * 6, 1);
		spacer.setPreferredSize(spacerDimension);
		c1.insets = new Insets(0, 0, 0, 0);
		c1.gridx = 1;
		c1.fill = GridBagConstraints.NONE;
		c1.weightx = 1;
		fullPanel.add(spacer, c1);
		m_BalanceText = new JLabel(" ");
		m_BalanceText.setHorizontalAlignment(JLabel.RIGHT);
		c1.insets = new Insets(0, 5, 0, 0);
		c1.weightx = 0;
		c1.fill = GridBagConstraints.HORIZONTAL;
		c1.gridx = 2;
		fullPanel.add(m_BalanceText, c1);
		JLabel label = new JLabel("", SwingConstants.RIGHT)
		{
			public Dimension getPreferredSize()
			{
				return a_alignLabel.getPreferredSize();
			}
		};
		c1.weightx = 0;
		c1.fill = GridBagConstraints.HORIZONTAL;
		c1.gridx = 3;
		c1.insets = new Insets(0, 10, 0, 0);
		fullPanel.add(label, c1);
		m_BalanceProgressBar = new JAPProgressBar();
		m_BalanceProgressBar.setMinimum(0);
		m_BalanceProgressBar.setMaximum(5);
		m_BalanceProgressBar.setBorderPainted(false);
		c1.gridx = 4;
		c1.weightx = 0;
		c1.fill = GridBagConstraints.NONE;

		c1.insets = new Insets(0, 5, 0, 0);
		fullPanel.add(m_BalanceProgressBar, c1);

		//Elmar: suggestion for improvement:
		//since all rows follow the same format, extract to a method like addRow(labelName)

		//row for validtime of flatrate
		m_labelValidUntilHeader = new JLabel(JAPMessages.getString(MSG_VALID_UNTIL));
		c1.insets = new Insets(10, 20, 0, 0);
		c1.gridx = 0;
		c1.gridy = 1;
		c1.anchor = GridBagConstraints.WEST;
		c1.weightx = 0;
		fullPanel.add(m_labelValidUntilHeader, c1);
		spacer = new JPanel();
		spacer.setPreferredSize(spacerDimension);
		c1.gridx = 1;
		c1.insets = new Insets(0, 0, 0, 0);
		c1.weightx = 1;
		c1.fill = GridBagConstraints.NONE;
		fullPanel.add(spacer, c1);
		m_labelValidUntil = new JLabel(" ");
		m_labelValidUntil.setHorizontalAlignment(JLabel.RIGHT);
		c1.insets = new Insets(10, 5, 0, 0);
		c1.gridx = 2;
		c1.fill = GridBagConstraints.HORIZONTAL;
		c1.weightx = 0;
		fullPanel.add(m_labelValidUntil, c1);

		//row for bytes_left of flatrate
		/*
		m_labelBalanceInEurosHeader = new JLabel(JAPMessages.getString(MSG_EURO_BALANCE));
		c1.insets = new Insets(10, 20, 0, 0);
		c1.gridx = 0;
		c1.gridy = 2;
		c1.anchor = GridBagConstraints.WEST;
		c1.weightx = 0;
		fullPanel.add(m_labelBalanceInEurosHeader, c1);
		spacer = new JPanel();
		spacer.setPreferredSize(spacerDimension);
		c1.gridx = 1;
		c1.insets = new Insets(0, 0, 0, 0);
		c1.weightx = 1;
		c1.fill = GridBagConstraints.NONE;
		fullPanel.add(spacer, c1);
		m_labelBalanceInEuros = new JLabel(" ");
		m_labelBalanceInEuros.setHorizontalAlignment(JLabel.RIGHT);
		c1.insets = new Insets(10, 5, 0, 0);
		c1.gridx = 2;
		c1.fill = GridBagConstraints.HORIZONTAL;
		c1.weightx = 0;
		fullPanel.add(m_labelBalanceInEuros, c1);
	  */



		//row for bytes spent in this session
		m_labelSessionSpentHeader = new JLabel(JAPMessages.getString(MSG_SESSIONSPENT));
		m_labelSessionSpentHeader.setVisible(false);
		c1.insets = new Insets(10, 20, 0, 0);
		c1.gridx = 0;
		c1.gridy = 3;
		c1.anchor = GridBagConstraints.WEST;
		c1.weightx = 0;
		fullPanel.add(m_labelSessionSpentHeader, c1);
		spacer = new JPanel();
		spacer.setPreferredSize(spacerDimension);
		c1.gridx = 1;
		c1.insets = new Insets(0, 0, 0, 0);
		c1.weightx = 1;
		c1.fill = GridBagConstraints.NONE;
		fullPanel.add(spacer, c1);
		m_labelSessionSpent = new JLabel(" ");
		m_labelSessionSpent.setVisible(false);
		m_labelSessionSpent.setHorizontalAlignment(JLabel.RIGHT);
		c1.insets = new Insets(10, 5, 0, 0);
		c1.gridx = 2;
		c1.fill = GridBagConstraints.HORIZONTAL;
		c1.weightx = 0;
		fullPanel.add(m_labelSessionSpent, c1);

		//row for total spent
		m_labelTotalSpentHeader = new JLabel(JAPMessages.getString(MSG_TOTALSPENT));
		c1.insets = new Insets(10, 20, 0, 0);
		c1.gridx = 0;
		c1.gridy = 4;
		c1.anchor = GridBagConstraints.WEST;
		c1.weightx = 0;
		fullPanel.add(m_labelTotalSpentHeader, c1);
		spacer = new JPanel();
		spacer.setPreferredSize(spacerDimension);
		c1.gridx = 1;
		c1.insets = new Insets(0, 0, 0, 0);
		c1.weightx = 1;
		c1.fill = GridBagConstraints.NONE;
		fullPanel.add(spacer, c1);
		m_labelTotalSpent = new JLabel(" ");
		m_labelTotalSpent.setHorizontalAlignment(JLabel.RIGHT);
		c1.insets = new Insets(10, 5, 0, 0);
		c1.gridx = 2;
		c1.fill = GridBagConstraints.HORIZONTAL;
		c1.weightx = 0;
		fullPanel.add(m_labelTotalSpent, c1);

		//row for date of last update
		m_dateLabel = new JLabel(JAPMessages.getString(MSG_LASTUPDATE));
		m_dateLabel.setVisible(false);
		c1.insets = new Insets(10, 20, 0, 0);
		c1.gridx = 0;
		c1.gridy = 5;
		c1.anchor = GridBagConstraints.WEST;
		c1.weightx = 0;
		fullPanel.add(m_dateLabel, c1);
		spacer = new JPanel();
		spacer.setPreferredSize(spacerDimension);
		c1.gridx = 1;
		c1.insets = new Insets(0, 0, 0, 0);
		c1.weightx = 1;
		c1.fill = GridBagConstraints.NONE;
		fullPanel.add(spacer, c1);
		this.setFullPanel(fullPanel);

		//build small panel
		JPanel smallPanel = new JPanel();
		smallPanel.setLayout(new GridBagLayout());
		c1 = new GridBagConstraints();
		m_labelTitleSmall = new JLabel(JAPMessages.getString(MSG_TITLE) + ":");
		c1.insets = new Insets(0, 5, 0, 0);
		c1.anchor = GridBagConstraints.WEST;
		c1.weightx = 0;
		c1.fill = GridBagConstraints.HORIZONTAL;
		smallPanel.add(m_labelTitleSmall, c1);
		spacer = new JPanel();
		spacerDimension = new Dimension(label.getFontMetrics(label.getFont()).charWidth('9') * 6, 1);
		spacer.setPreferredSize(spacerDimension);
		c1.insets = new Insets(0, 0, 0, 0);
		c1.gridx = 1;
		c1.fill = GridBagConstraints.NONE;
		c1.weightx = 1;
		smallPanel.add(spacer, c1);
		c1.insets = new Insets(0, 5, 0, 0);
		c1.weightx = 0;
		c1.fill = GridBagConstraints.HORIZONTAL;
		c1.gridx = 2;
		m_BalanceTextSmall = new JLabel(" ");
		m_BalanceTextSmall.setHorizontalAlignment(JLabel.RIGHT);
		smallPanel.add(m_BalanceTextSmall, c1);
		label = new JLabel("", SwingConstants.RIGHT)
		{
			public Dimension getPreferredSize()
			{
				return a_alignLabel.getPreferredSize();
			}
		};
		c1.weightx = 0;
		c1.fill = GridBagConstraints.HORIZONTAL;
		c1.gridx = 3;
		c1.insets = new Insets(0, 10, 0, 0);
		smallPanel.add(label, c1);

		c1.gridx = 4;
		c1.weightx = 0;
		c1.fill = GridBagConstraints.NONE;
		c1.insets = new Insets(0, 5, 0, 0);
		m_BalanceSmallProgressBar = new JAPProgressBar();
		m_BalanceSmallProgressBar.setMinimum(0);
		m_BalanceSmallProgressBar.setMaximum(5);
		m_BalanceSmallProgressBar.setBorderPainted(false);

		smallPanel.add(m_BalanceSmallProgressBar, c1);
		this.setSmallPanel(smallPanel);

		MouseAdapter adapter = new MouseAdapter()
		{
			public void mouseClicked(MouseEvent a_event)
			{
				if (((JLabel)a_event.getSource()).getCursor() != Cursor.getDefaultCursor())
				{
					if (((JLabel)a_event.getSource()).getForeground() == Color.blue)
					{
						m_view.showConfigDialog(JAPConf.PAYMENT_TAB,
												PayAccountsFile.getInstance().getActiveAccount());
					}
					else
					{
						m_view.showConfigDialog(JAPConf.PAYMENT_TAB, new Boolean(true));
					}
				}
			}
		};

		m_BalanceTextSmall.addMouseListener(adapter);
		m_BalanceText.addMouseListener(adapter);


		PayAccountsFile.getInstance().addPaymentListener(m_MyPaymentListener);
		// do not show nearly empty dialog, as this would freeze the start sequence
		updateDisplay(PayAccountsFile.getInstance().getActiveAccount(), false);
	}

	public static String translateBIError(XMLErrorMessage a_msg)
	{
		String error = ""; //JAPMessages.getString("aiErrorMessage"); // + "<br>";
		if (a_msg.getErrorCode() >= 0 && a_msg.getErrorCode() < MSG_PAYMENT_ERRORS.length)
		{
			error += JAPMessages.getString(MSG_PAYMENT_ERRORS[a_msg.getErrorCode()]);
		}
		else
		{
			error += a_msg.getErrorDescription();
		}
		return error;
	}

	public void stopUpdateQueue()
	{
		m_queueUpdate.stop();
	}

	/**
	 * This should be called by the changelistener whenever the state of the
	 * active account changes.
	 *
	 * @param activeAccount PayAccount
	 */
	private void updateDisplay(final PayAccount activeAccount, final boolean a_bWarnIfNearlyEmpty)
	{
		if (PayAccountsFile.getInstance().getActiveAccount() != activeAccount)
		{
			return;
		}

		JobQueue.Job job = new JobQueue.Job(true)
		{
			public void runJob()
			{
				// payment disabled
				if (activeAccount == null)
				{
					m_labelValidUntil.setText("");
					m_BalanceText.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
					m_BalanceText.setToolTipText(JAPMessages.getString(MSG_TT_CREATE_ACCOUNT));
					m_BalanceText.setText(JAPMessages.getString(MSG_PAYMENTNOTACTIVE));
					m_BalanceText.setForeground(m_labelValidUntil.getForeground());
					m_BalanceProgressBar.setValue(0);
					m_BalanceProgressBar.setEnabled(false);
					m_spentThisSession = 0;
					m_labelSessionSpent.setText("");
					m_labelTotalSpent.setText("");
				}
				// we got everything under control, situation normal
				else
				{
					m_BalanceText.setCursor(Cursor.getDefaultCursor());
					m_BalanceText.setToolTipText(null);

					XMLBalance balance = activeAccount.getBalance();
					Timestamp now = new Timestamp(System.currentTimeMillis());

					if (balance != null && activeAccount.isCharged(now))
					{
						m_BalanceProgressBar.setEnabled(false);
						//m_BalanceText.setText(JAPUtil.formatBytesValueWithUnit(balance.getVolumeKBytesLeft() * 1000));
						m_BalanceText.setText(JAPUtil.formatBytesValueWithUnit(activeAccount.getCurrentCredit() * 1000));
//							JAPUtil.MAX_FORMAT_KBYTES));
						m_BalanceText.setForeground(m_labelValidUntil.getForeground());
						m_labelValidUntil.setText(JAPUtil.formatTimestamp(balance.getFlatEnddate(), false,
							JAPMessages.getLocale().getLanguage()));
						//m_labelBalanceInEuros.setText(JAPUtil.formatEuroCentValue(balance.getBalance()));

						//long deposit = balance.getVolumeKBytesLeft() * 1000 + balance.getSpent();
						long deposit = FULL_AMOUNT * 1000;
						//long credit = balance.getVolumeKBytesLeft() * 1000;
						long credit = activeAccount.getCurrentCredit() * 1000;
						double percent = (double) credit / (double) deposit;
						if (percent > 0.83)
						{
							m_BalanceProgressBar.setValue(5);
						}
						else if (percent > 0.66)
						{
							m_BalanceProgressBar.setValue(4);
						}
						else if (percent > 0.49)
						{
							m_BalanceProgressBar.setValue(3);
						}
						else if (percent > 0.32)
						{
							m_BalanceProgressBar.setValue(2);
						}
						else if (credit > 0.15)
						{
							m_BalanceProgressBar.setValue(1);
						}
						else
						{
							m_BalanceProgressBar.setValue(0);
						}
						m_BalanceProgressBar.setEnabled(true);
					}
					else
					{
						m_BalanceProgressBar.setValue(0);
						m_BalanceProgressBar.setEnabled(false);

						if (balance == null)
						{
							m_labelValidUntil.setText("");
							m_BalanceText.setText(JAPUtil.formatBytesValueWithUnit(0));
							m_BalanceText.setForeground(m_labelValidUntil.getForeground());
						}
						else
						{
							Timestamp enddate = balance.getFlatEnddate();
							String endDateString = JAPUtil.formatTimestamp(enddate, false,
								JAPMessages.getLocale().getLanguage());
							boolean expired = false;

							m_BalanceText.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
							m_BalanceText.setToolTipText(JAPMessages.getString(MSG_TT_CREATE_ACCOUNT));

							//if (balance.getVolumeKBytesLeft() == 0)
							if (activeAccount.getCurrentCredit() == 0)
							{
								m_labelValidUntil.setText("");
							}
							else if (enddate != null && enddate.after(now))
							{
								m_labelValidUntil.setText(endDateString);
							}
							else
							{
								expired = true;
								m_labelValidUntil.setText(JAPMessages.getString(AccountSettingsPanel.MSG_EXPIRED));
							}

							//if (balance.getVolumeKBytesLeft() > 0 && expired)
							if (activeAccount.getCurrentBytes() > 0 && expired)
							{
								m_BalanceText.setText(JAPMessages.getString(AccountSettingsPanel.MSG_EXPIRED));
								m_BalanceText.setForeground(m_labelValidUntil.getForeground());
							}
							//else if (balance.getVolumeKBytesLeft() <= 0 && balance.getSpent() == 0 && !expired)
							else if (activeAccount.getCurrentBytes() <= 0 && 
									//activeAccount.getSpent() == 0 && 
									activeAccount.getCurrentSpent() == 0 && 
									!expired)
							{								
								if (activeAccount.getTransCerts().size() > 0 &&	!activeAccount.isTransactionExpired())
								{
									m_BalanceText.setText(JAPMessages.getString(AccountSettingsPanel.
											MSG_NO_TRANSACTION));
									m_BalanceText.setToolTipText(JAPMessages.getString(AccountSettingsPanel.
										MSG_SHOW_TRANSACTION_DETAILS));
									m_BalanceText.setForeground(Color.blue);
								}
								else
								{
									m_BalanceText.setText("");
									m_BalanceText.setToolTipText(null);
									m_BalanceText.setForeground(m_labelValidUntil.getForeground());
								}
							}
							else
							{
								m_BalanceText.setText(JAPMessages.getString(AccountSettingsPanel.MSG_NO_CREDIT));
								m_BalanceText.setForeground(m_labelValidUntil.getForeground());
							}
						}
					}
					//set rest of the panel
					m_spentThisSession = AIControlChannel.getBytes();

					m_labelSessionSpent.setText(JAPUtil.formatBytesValueWithUnit(m_spentThisSession));

					//m_labelTotalSpent.setText(JAPUtil.formatBytesValueWithUnit(activeAccount.getSpent()));
					m_labelTotalSpent.setText(JAPUtil.formatBytesValueWithUnit(activeAccount.getCurrentSpent()));
					// account is nearly empty

					if (a_bWarnIfNearlyEmpty && //a_bWarnIfNearlyEmpty means warnings are not to be suppressed
						activeAccount.getCurrentCredit() <= WARNING_AMOUNT && !m_notifiedEmpty &&
						activeAccount.isCharged(now) &&
						PayAccountsFile.getInstance().getAlternativeChargedAccount(
							JAPController.getInstance().getCurrentMixCascade().getPIID()) == null)
					{
						m_notifiedEmpty = true;
						// start a new thread to prevent freezing of GUI update
						new Thread(new Runnable()
						{
							public void run()
							{
								if (JAPDialog.showYesNoDialog(JAPController.getInstance().getViewWindow(),
									JAPMessages.getString(MSG_NEARLYEMPTY_CREATE_ACCOUNT)))
								{
									m_view.showConfigDialog(JAPConf.PAYMENT_TAB,
										JAPController.getInstance().getCurrentMixCascade().getPIID());
								}
							}
						}).start();
					}
					Timestamp warningTime = new Timestamp(System.currentTimeMillis() + WARNING_TIME);
					if (a_bWarnIfNearlyEmpty && //a_bWarnIfNearlyEmpty means warnings are not to be suppressed
						!m_notifiedEmpty && activeAccount.isCharged(now) && !activeAccount.isCharged(warningTime) &&
						PayAccountsFile.getInstance().getAlternativeChargedAccount(
							JAPController.getInstance().getCurrentMixCascade().getPIID()) == null)
					{
						m_notifiedEmpty = true;
						// start a new thread to prevent freezing of GUI update
						new Thread(new Runnable()
						{
							public void run()
							{
								if (JAPDialog.showYesNoDialog(JAPController.getInstance().getViewWindow(),
									JAPMessages.getString(MSG_NEARLYEXPIRED_CREATE_ACCOUNT)))
								{
									m_view.showConfigDialog(JAPConf.PAYMENT_TAB,
										JAPController.getInstance().getCurrentMixCascade().getPIID());
								}
							}
						}).start();
					}

				}

				m_BalanceTextSmall.setText(m_BalanceText.getText());
				m_BalanceTextSmall.setForeground(m_BalanceText.getForeground());
				m_BalanceTextSmall.setToolTipText(m_BalanceText.getToolTipText());
				m_BalanceTextSmall.setCursor(m_BalanceText.getCursor());
				m_BalanceSmallProgressBar.setValue(m_BalanceProgressBar.getValue());
				m_BalanceSmallProgressBar.setEnabled(m_BalanceProgressBar.isEnabled());
			}
		};

		m_queueUpdate.addJob(job);
	}

	/**
	 * Notifies us when the state of the active account changes, so we can update
	 * the display
	 *
	 * @version 1.0
	 */
	private class MyPaymentListener implements anon.pay.IPaymentListener
	{
		/**
		 * accountActivated
		 *
		 * @param acc PayAccount
		 */
		public void accountActivated(PayAccount acc)
		{
			updateDisplay(acc, true);
		}

		/**
		 * accountAdded
		 *
		 * @param acc PayAccount
		 */
		public void accountAdded(PayAccount acc)
		{
		}

		/**
		 * accountRemoved
		 *
		 * @param acc PayAccount
		 */
		public void accountRemoved(PayAccount acc)
		{
		}

		/**
		 * creditChanged
		 *
		 * @param acc PayAccount
		 */
		public void creditChanged(PayAccount acc)
		{
			updateDisplay(acc, true);
		}

		private String formatCascadeName(MixCascade a_cascade)
		{
			String strCascadeName;
			
			if (a_cascade == null || a_cascade.getName() == null)
			{
				strCascadeName = "";
			}
			else
			{
				strCascadeName = ", <b>" + a_cascade.getName() + "</b>,";
			}
			return strCascadeName;
		}
		
		private String formatOrganisation(String a_strOrganisation)
		{
			String strOrganisation;
			
			/** Fix this, when test BI has a new organisation... */
			a_strOrganisation = "JonDos GmbH";
			
			if (a_strOrganisation == null)
			{
				strOrganisation = "";
			}
			else
			{
				strOrganisation = "<b>" + a_strOrganisation + "</b>";
			}
			return strOrganisation;			
		}
		
		/**
		 * accountCertRequested
		 *
		 * @param usingCurrentAccount boolean
		 */
		public int accountCertRequested(final MixCascade a_connectedCascade)
		{
			final PayAccountsFile payAccounts = PayAccountsFile.getInstance();
			int bSuccess = ErrorCodes.E_SUCCESS;
			PaymentInstanceDBEntry piEntry;
			String strOrganisation = null;

			final JAPDialog.LinkedCheckBox adapter =			
				new JAPDialog.LinkedCheckBox(JAPMessages.getString(MSG_CHOOSE_FREE_SERVICES_ONLY), 
						false, "premium")
			{
				public boolean isOnTop()
				{
					return true;
				}			
			};
			Runnable run = null;
			final String strMessage = ""; //JAPMessages.getString(MSG_FREE_OF_CHARGE) + "<br><br>";
						
			piEntry = (PaymentInstanceDBEntry)Database.getInstance(PaymentInstanceDBEntry.class).getEntryById(
					a_connectedCascade.getPIID());
			if (piEntry != null)
			{
				strOrganisation = piEntry.getOrganisation();
			}
			final String fstrOrganisation = strOrganisation;
			final MixCascade cascade = (MixCascade)Database.getInstance(MixCascade.class).getEntryById(a_connectedCascade.getId());

			final PayAccount account = payAccounts.getActiveAccount();
			if (payAccounts.getNumAccounts() == 0  ||
				(account != null && !account.getPIID().equals(a_connectedCascade.getPIID())))
			{
				JAPController.getInstance().setAnonMode(false);
				bSuccess = ErrorCodes.E_ACCOUNT_EMPTY;

				run = new Runnable()
				{
					public void run()
					{
						int optionType;
						JAPDialog.LinkedHelpContext helpAdapter;
						if (JAPModel.getInstance().isCascadeAutoSwitched() &&
							!TrustModel.getCurrentTrustModel().isPaymentForced())
						{
							optionType = JAPDialog.OPTION_TYPE_YES_NO_CANCEL;
							helpAdapter = adapter;
						}
						else
						{
							optionType = JAPDialog.OPTION_TYPE_OK_CANCEL;
							helpAdapter = new JAPDialog.LinkedHelpContext("premium")
							{
								public boolean isOnTop()
								{
									return true;
								}			
							};
						}

						int answer = JAPDialog.showConfirmDialog(
							JAPController.getInstance().getViewWindow(),
							strMessage + JAPMessages.getString(MSG_WITH_COSTS, formatCascadeName(cascade)) + " " + 
							JAPMessages.getString(MSG_CREATE_ACCOUNT_QUESTION),
							optionType, JAPDialog.MESSAGE_TYPE_QUESTION, helpAdapter);
						
						if (answer == JAPDialog.RETURN_VALUE_YES)
						{
							JAPController.getInstance().setAllowPaidServices(true);
							m_view.showConfigDialog(JAPConf.PAYMENT_TAB, a_connectedCascade.getPIID());
						}
						else if (answer == JAPDialog.RETURN_VALUE_NO)
						{
							if (adapter.getState())
							{
								// this user wants free services only... Choose the free trust model.
								TrustModel.forceFreeTrustModel();
							}
							JAPController.getInstance().setAllowPaidServices(false);
							JAPController.getInstance().switchToNextMixCascade();
							JAPController.getInstance().setAnonMode(true);							
						}
					}
				};
			}
			else
			{
				if (account == null)
				{
					JAPController.getInstance().setAnonMode(false);
					bSuccess = ErrorCodes.E_ACCOUNT_EMPTY;
					run = new Runnable()
					{
						public void run()
						{
							JAPDialog.showErrorDialog(JAPController.getInstance().getViewWindow(),
								strMessage + JAPMessages.getString(MSG_NO_ACTIVE_ACCOUNT), LogType.PAY, adapter);
							m_view.showConfigDialog(JAPConf.PAYMENT_TAB, null);
							if (adapter.getState())
							{
								// this user wants free services only... Choose the free trust model.
								TrustModel.forceFreeTrustModel();
							}
						}
					};
				}
				else if (!account.isCharged(new Timestamp(System.currentTimeMillis())))
				{
					JAPController.getInstance().setAnonMode(false);
					bSuccess = ErrorCodes.E_ACCOUNT_EMPTY;

					run = new Runnable()
					{
						public void run()
						{
							String message = strMessage + JAPMessages.getString(MSG_WITH_COSTS, 
									formatCascadeName(cascade)) + " ";
							boolean bOpenTransaction = false;
							//if (account.getBalance().getSpent() <= 0 && !account.hasExpired())
							if (account.getCurrentSpent() <= 0 && !account.hasExpired())
							{
								message += JAPMessages.getString(MSG_OPEN_TRANSACTION);
								bOpenTransaction = true;
							}
							else
							{
								message += JAPMessages.getString(MSG_CREATE_ACCOUNT_QUESTION);
							}
							JAPController.getInstance().setAnonMode(false);
							int optionType;
							JAPDialog.LinkedHelpContext helpAdapter;
							if (JAPModel.getInstance().isCascadeAutoSwitched() &&
								!TrustModel.getCurrentTrustModel().isPaymentForced())
							{
								helpAdapter = adapter;
								optionType = JAPDialog.OPTION_TYPE_YES_NO_CANCEL;
							}
							else
							{
								optionType = JAPDialog.OPTION_TYPE_OK_CANCEL;
								helpAdapter = new JAPDialog.LinkedHelpContext("premium")
								{
									public boolean isOnTop()
									{
										return true;
									}			
								};
							}
							int answer = JAPDialog.showConfirmDialog(
									JAPController.getInstance().getViewWindow(), message,
									optionType, JAPDialog.MESSAGE_TYPE_QUESTION, helpAdapter);
							if (answer == JAPDialog.RETURN_VALUE_YES)
							{
								JAPController.getInstance().setAllowPaidServices(true);
								if (bOpenTransaction)
								{
									m_view.showConfigDialog(JAPConf.PAYMENT_TAB, account);
								}
								else
								{
									m_view.showConfigDialog(JAPConf.PAYMENT_TAB, a_connectedCascade.getPIID());
								}
								//m_view.showConfigDialog(JAPConf.PAYMENT_TAB, new Boolean(true));
							}
							else if (answer == JAPDialog.RETURN_VALUE_NO)
							{
								if (adapter.getState())
								{
									// this user wants free services only... Choose the free trust model.
									TrustModel.forceFreeTrustModel();
								}
								JAPController.getInstance().setAllowPaidServices(false);
								JAPController.getInstance().switchToNextMixCascade();
								JAPController.getInstance().setAnonMode(true);
							}
						}
					};
				}
			}
			if (run != null)
			{
				if (JAPDialog.isConsoleOnly())
				{
					run.run();
				}
				else
				{
					SwingUtilities.invokeLater(run);
				}
			}
			return bSuccess;
			/*
			if (accounts.getActiveAccount() != null)
			{
				accounts.getActiveAccount().updated();
			}*/

		}

		/**
		 * accountError
		 *
		 * @param msg XMLErrorMessage
		 */
		public void accountError(XMLErrorMessage a_msg, boolean a_bIgnore)
		{
			PaymentInstanceDBEntry piEntry;
			String strOrganisation = null;
			
			if (a_msg.getErrorCode() <= XMLErrorMessage.ERR_OK || a_msg.getErrorCode() < 0)
			{
				// no error
				return;
			}

			final MixCascade cascade = JAPController.getInstance().getCurrentMixCascade();
			/* When "account empty" is signaled the connection should always be closed.
			 * But this should not be controlled by a GUI component.
			 */
			/*if (a_msg.getErrorCode() != XMLErrorMessage.ERR_ACCOUNT_EMPTY &&
				cascade.equals(JAPController.getInstance().switchToNextMixCascade()))
			{*/
				// there are no other cascades to switch to
				LogHolder.log(LogLevel.WARNING, LogType.NET, "There are no other cascades to choose!");
				for (int i = 0; i < 5 &&
					 (JAPController.getInstance().getAnonMode() ||
					  JAPController.getInstance().isAnonConnected()); i++)
				{
					// does not work well as of auto-reconnection
					JAPController.getInstance().setAnonMode(false);
					try
					{
						Thread.sleep(200);
					}
					catch (InterruptedException ex)
					{
						break;
					}
				}
			//}
			
			piEntry = (PaymentInstanceDBEntry)Database.getInstance(PaymentInstanceDBEntry.class).getEntryById(
					cascade.getPIID());
			if (piEntry != null)
			{
				strOrganisation = piEntry.getOrganisation();
			}
			final String fstrOrganisation = strOrganisation;

			if (!m_bShowingError && !a_bIgnore)
			{
				m_bShowingError = true;
				final XMLErrorMessage msg = a_msg;
				new Thread(new Runnable()
				{
					public void run()
					{
						String message = //JAPMessages.getString(MSG_FREE_OF_CHARGE) + "<br><br>" +
							translateBIError(msg);
						Component parent = PaymentMainPanel.this;
						final JAPDialog.LinkedCheckBox adapter =			
							new JAPDialog.LinkedCheckBox(JAPMessages.getString(MSG_CHOOSE_FREE_SERVICES_ONLY), 
									false, "premium")
						{
							public boolean isOnTop()
							{
								return true;
							}			
						};

						if (!GUIUtils.getParentWindow(parent).isVisible())
						{
							parent = JAPController.getInstance().getViewWindow();
						}

						if (msg.getErrorCode() == XMLErrorMessage.ERR_ACCOUNT_EMPTY)
						{
							message += "<br><br>" +
								JAPMessages.getString(MSG_WITH_COSTS, formatCascadeName(cascade)) + " " +
								JAPMessages.getString(MSG_CREATE_ACCOUNT_QUESTION);
							int optionType;
							if (JAPModel.getInstance().isCascadeAutoSwitched() &&
								!TrustModel.getCurrentTrustModel().isPaymentForced())
							{
								optionType = JAPDialog.OPTION_TYPE_YES_NO_CANCEL;
							}
							else
							{
								optionType = JAPDialog.OPTION_TYPE_OK_CANCEL;
							}
							int answer = JAPDialog.showConfirmDialog(parent, message,
								optionType, JAPDialog.MESSAGE_TYPE_QUESTION, adapter);							
							
							if (answer == JAPDialog.RETURN_VALUE_YES)
							{
								JAPController.getInstance().setAllowPaidServices(true);
								new Thread(new Runnable()
								{
									public void run()
									{
										if (cascade.isPayment())
										{
											m_view.showConfigDialog(JAPConf.PAYMENT_TAB, cascade.getPIID());
										}
										else
										{
											// huh, this should not happen...
											m_view.showConfigDialog(JAPConf.PAYMENT_TAB, new Boolean(true));
										}
									}
								}).start();																								
							}
							else if (answer == JAPDialog.RETURN_VALUE_NO)
							{
								if (adapter.getState())
								{
									// this user wants free services only... Choose the free trust model.
									TrustModel.forceFreeTrustModel();
								}
								JAPController.getInstance().setAllowPaidServices(false);
								JAPController.getInstance().switchToNextMixCascade();
								JAPController.getInstance().setAnonMode(true);
							}
						}
						else if (!JAPModel.getInstance().isCascadeAutoSwitched())
						{
							message += "<br><br>" + JAPMessages.getString(MSG_ENABLE_AUTO_SWITCH);
							
							if (JAPDialog.RETURN_VALUE_YES == 
								JAPDialog.showConfirmDialog(parent, message, JAPMessages.getString(JAPDialog.MSG_TITLE_WARNING), 
										JAPDialog.OPTION_TYPE_YES_NO,
										JAPDialog.MESSAGE_TYPE_WARNING,  adapter))
							{
								JAPModel.getInstance().setCascadeAutoSwitch(true);
							}
						}
						else
						{
							JAPDialog.showErrorDialog(parent, message, LogType.PAY, adapter);
						}
						m_bShowingError = false;
					}
				}).start();
			}
		}

		public void gotCaptcha(ICaptchaSender a_source, final IImageEncodedCaptcha a_captcha)
		{
		}
	}

	/**
	 * Loads some icons for the account display
	 */
	private void loadIcons()
	{
		// Load Images for Account Icon Display
		m_accountIcons = new ImageIcon[JAPConstants.ACCOUNTICONFNARRAY.length];
		for (int i = 0; i < JAPConstants.ACCOUNTICONFNARRAY.length; i++)
		{
			m_accountIcons[i] = GUIUtils.loadImageIcon(JAPConstants.ACCOUNTICONFNARRAY[i], false);
		}
	}
}
