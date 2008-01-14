/*
 Copyright (c) 2000 - 2006, The JAP-Team
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

import gui.dialog.JAPDialog;
import java.awt.Component;
import gui.JAPMessages;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import anon.pay.xml.XMLPaymentSettings;
import javax.swing.BoxLayout;
import java.awt.event.ActionListener;
import java.awt.Event;
import java.awt.event.ActionEvent;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import jap.JAPUtil;
import java.sql.Timestamp;
import anon.pay.PayAccount;
import logging.LogLevel;
import logging.LogHolder;
import anon.pay.PaymentInstanceDBEntry;
import anon.pay.BIConnection;
import anon.pay.PayAccountsFile;
import jap.JAPModel;
import logging.LogType;
import anon.pay.xml.XMLErrorMessage;
import javax.swing.Box;
import java.awt.Insets;
import jap.AbstractJAPConfModule;
import java.awt.Color;

/**
 * Dialog that is shown when the user clicks the "buy flatrate" button on AccountSettingsPanel
 * shows the current modalities of the flatrate (duration, price etc from table paymentsettings)
 * as well as the user's balance, and allows the user to buy a flatrate if funds are sufficient
 *
 * @deprecated : outdated, since this dialog can't show multiple volume plans
 * Choosing a plan has been integrated into the regular charge wizard instead
 *
 * @author Elmar Schraml

 */
public class FlatrateDialog extends JAPDialog implements ActionListener
{
	private static final String MSG_HEADING = "heading";
	private static final String MSG_BALANCE_LABEL = "balance_label";
	private static final String MSG_FLAT_HEADER = "flat_header";
	private static final String MSG_PRICE_LABEL = "price_label";
	private static final String MSG_VALID = "valid";
	private static final String MSG_VOLUME_LABEL = "volume_label";
	private static final String MSG_INSUFFICIENT_FUNDS = "insufficient_funds";
	private static final String MSG_UNLIMITED = "unlimited";
	private static final String MSG_BUY = "buy";
	private static final String MSG_CANCEL = "cancel";
	private static final String MSG_ERROR_CONNECTION = "error_connection";
	private static final String MSG_ERROR_INSUFFICIENT_FUNDS = "error_insufficient_funds";
	private static final String MSG_ERROR_FLATRATE_REFUSED = "error_flatrate_refused";
	private static final String MSG_ERROR_ALREADY_ACTIVE = "error_already_active";
	private static final String MSG_ERROR_NO_SETTINGS = "error_no_settings";
	private static final String MSG_FLAT_BOUGHT = "flat_bought";

	private AbstractJAPConfModule m_parent;

	private JButton m_btnBuy;
	private JButton m_btnCancel;
	private JLabel m_lHeading;
	private JLabel m_lBalanceLabel ;
	private JLabel m_lBalance;
	private JLabel m_lFlatHeader ;
	private JLabel m_lPriceLabel;
	private JLabel m_lPrice;
	private JLabel m_lValidtimeLabel;
	private JLabel m_lValidtime;
	private JLabel m_lVolumeLabel;
	private JLabel m_lVolume;
	private JLabel m_lInsufficientFunds ;

	private JPanel buttonPanel;
	private JPanel flatPanel;

	private String m_strBalance;
	private String m_strPrice;
	private String m_strDuration;
	private String m_strUnit;

	private XMLPaymentSettings paymentSettings;
	private boolean isFlatAffordable = true;
	private PayAccount m_account;

	public FlatrateDialog(Component parentComponent, AbstractJAPConfModule calledFrom, String title, boolean modal, PayAccount activeAccount)
	{
		super(parentComponent,title,modal);
		m_parent = calledFrom;

		//get account info (in private vars, so we can use it in methods without passing the PayAccount around)
		m_account = activeAccount;

		//refuse to buy new flatrate if one is already active
		if (m_account.isFlatrateActive()  )
		{
			long remainingBytes = m_account.getBalance().getVolumeBytesLeft();
			if (remainingBytes > PayAccount.MAX_KBYTES_COUNTING_AS_EMPTY )
			{
				JAPDialog.showMessageDialog(this, getString(MSG_ERROR_ALREADY_ACTIVE));
				return;
			}
		}

		//get paymentsettings
		paymentSettings = getPaymentSettings();
		if (paymentSettings == null)
		{
			JAPDialog.showMessageDialog(this,getString(MSG_ERROR_NO_SETTINGS));
			return;
		}



		//check if account balance is sufficient to buy flat
		String flatPriceString = paymentSettings.getSettingValue("FlatratePrice");
		long flatPrice = Long.parseLong(flatPriceString);
		long balance = m_account.getBalance().getCredit();
		isFlatAffordable = balance >= flatPrice;

		//build dialog elements
		m_lHeading = new JLabel("<html><b>"+getString(MSG_HEADING)+"</b></html>");
		if (isFlatAffordable)
		{
			m_lInsufficientFunds = new JLabel("");
		} else
		{
			m_lInsufficientFunds = new JLabel(getString(MSG_INSUFFICIENT_FUNDS));
		}
		flatPanel = buildFlatPanel(m_account);
		buttonPanel = buildButtonPanel();

		//layout dialog
		JPanel mainPanel = new JPanel();
		mainPanel.setLayout(new GridBagLayout());
		GridBagConstraints gbc = new GridBagConstraints();

		gbc.gridx = 0;
		gbc.insets = new Insets(5,5,5,5);
		gbc.anchor = GridBagConstraints.NORTH;

		gbc.gridy = 0;
		mainPanel.add(m_lHeading,gbc);

		gbc.gridy = 1;
		mainPanel.add(m_lInsufficientFunds,gbc);
		m_lInsufficientFunds.setForeground(Color.red);

		gbc.gridy = 2;
		mainPanel.add(flatPanel,gbc);

		gbc.gridy = 3;
		mainPanel.add(buttonPanel,gbc);

		this.setContentPane(mainPanel);
		this.pack();
		this.setVisible(true);

	}

	private JPanel buildButtonPanel()
	{
		buttonPanel = new JPanel(); //default Flow Layout
		m_btnBuy = new JButton(getString(MSG_BUY));
		m_btnBuy.addActionListener(this);
		m_btnCancel = new JButton(getString(MSG_CANCEL));
		m_btnCancel.addActionListener(this);
		buttonPanel.add(m_btnCancel);
		buttonPanel.add(Box.createHorizontalStrut(10));
		buttonPanel.add(m_btnBuy);

		return buttonPanel;
	}

	private JPanel buildFlatPanel(PayAccount account)
	{
		JPanel flatPanel = new JPanel();
		GridBagLayout gbl = new GridBagLayout();
		flatPanel.setLayout(gbl);
		GridBagConstraints gbc = new GridBagConstraints();
		gbc.insets = new Insets(5,5,5,5);
		gbc.anchor = GridBagConstraints.NORTHWEST;

		m_lBalanceLabel = new JLabel(getString(MSG_BALANCE_LABEL));
		gbc.gridx = 0;
		gbc.gridy = 0;
		gbl.setConstraints(m_lBalanceLabel,gbc);
		flatPanel.add(m_lBalanceLabel);

		String balance = JAPUtil.formatEuroCentValue(account.getBalance().getCredit());
		m_lBalance = new JLabel(balance);
		gbc.gridx = 1;
		gbc.gridy = 0;
		gbl.setConstraints(m_lBalance,gbc);
		flatPanel.add(m_lBalance);



		m_lFlatHeader = new JLabel(getString(MSG_FLAT_HEADER));
		gbc.gridx = 0;
		gbc.gridy = 1;
		gbc.gridwidth = 1;
		gbc.insets = new Insets(15,5,5,5);
		gbl.setConstraints(m_lFlatHeader,gbc);
		flatPanel.add(m_lFlatHeader);

		m_lPriceLabel = new JLabel(getString(MSG_PRICE_LABEL));
		gbc.gridx = 0;
		gbc.gridy = 2;
		gbc.insets = new Insets(5,5,5,5);
		gbl.setConstraints(m_lPriceLabel,gbc);
		flatPanel.add(m_lPriceLabel);

		m_lPrice = new JLabel(JAPUtil.formatEuroCentValue(Long.parseLong(paymentSettings.getSettingValue("FlatratePrice"))));
		gbc.gridx = 1;
		gbc.gridy = 2;
		gbl.setConstraints(m_lPrice,gbc);
		flatPanel.add(m_lPrice);

		m_lValidtimeLabel = new JLabel(getString(MSG_VALID));
		gbc.gridx = 0;
		gbc.gridy = 3;
		gbl.setConstraints(m_lValidtimeLabel,gbc);
		flatPanel.add(m_lValidtimeLabel);


		if (paymentSettings.getSettingValue("DurationLimited").equalsIgnoreCase("true") )
		{
			//duration units are not localized, so we show the enddate instead
			Timestamp endTime = new Timestamp(paymentSettings.getEndDate().getTime().getTime());
			String lang = JAPMessages.getLocale().getLanguage();
			String enddate = JAPUtil.formatTimestamp(endTime, false, lang);
			m_lValidtime = new JLabel(enddate);
		} else {
			m_lValidtime = new JLabel(getString(MSG_UNLIMITED));
		}
		gbc.gridx = 1;
		gbc.gridy = 3;
		gbl.setConstraints(m_lValidtime,gbc);
		flatPanel.add(m_lValidtime);

		m_lVolumeLabel = new JLabel(getString(MSG_VOLUME_LABEL));
		gbc.gridx = 0;
		gbc.gridy = 4;
		gbl.setConstraints(m_lVolumeLabel,gbc);
		flatPanel.add(m_lVolumeLabel);

		if (paymentSettings.getSettingValue("VolumeLimited").equals("true") )
		{
			m_lVolume = new JLabel(JAPUtil.formatBytesValueWithUnit(Long.parseLong(paymentSettings.getSettingValue("VolumeAmount"))*1000));
		}
		else {
			m_lVolume = new JLabel(getString(MSG_UNLIMITED));
		}
		gbc.gridx = 1;
		gbc.gridy = 4;
		gbl.setConstraints(m_lVolume,gbc);
		flatPanel.add(m_lVolume);

		return flatPanel;
	}

	public void actionPerformed(ActionEvent e)
	{
		if (e.getSource() == m_btnBuy )
		{
			buyFlatrate();
		} else if (e.getSource() == m_btnCancel)
		{
			this.setVisible(false);
		}
	}

	private void buyFlatrate()
	{
		//refuse if balance is too low
		if ( ! isFlatAffordable )
		{
			JAPDialog.showMessageDialog(this,getString(MSG_INSUFFICIENT_FUNDS));
			return;
		}
		//buy flatrate
		XMLErrorMessage reply = null;
		try
		{
			PaymentInstanceDBEntry pi = m_account.getBI();
			BIConnection piConn = new BIConnection(pi);
			piConn.connect(JAPModel.getInstance().getPaymentProxyInterface());
			piConn.authenticate(m_account.getAccountCertificate(),m_account.getPrivateKey());
			reply = piConn.buyFlatrate(m_account.getAccountNumber());
			piConn.disconnect();
		}
		catch (Exception e)
		{
				LogHolder.log(LogLevel.EXCEPTION, LogType.NET,"Error buying flatrate: " + e.getMessage());
				JAPDialog.showMessageDialog(this, getString(MSG_ERROR_CONNECTION));
		}
		//check reply
		if (reply == null)
		{
			JAPDialog.showMessageDialog(this,getString(MSG_ERROR_CONNECTION));
		}

		if (reply.getErrorCode() == XMLErrorMessage.ERR_OK)
		{
			JAPDialog.showMessageDialog(this,getString(MSG_FLAT_BOUGHT));
			this.setVisible(false);
			try
			{
				AccountSettingsPanel parentPanel = (AccountSettingsPanel) m_parent;
				parentPanel.updateAccountShown();
			}
			catch (Exception e)
			{
				//flatrate dialog was called from some other dialog
				//perfectly acceptable, do nothing
			}

		}
		else
		{
			JAPDialog.showMessageDialog(this,getString(MSG_ERROR_FLATRATE_REFUSED));
		}



	}

	private XMLPaymentSettings getPaymentSettings()
	{
		XMLPaymentSettings theSettings = null;
		try
		{
			PaymentInstanceDBEntry pi = m_account.getBI();
			BIConnection piConn = new BIConnection(pi);
			piConn.connect(JAPModel.getInstance().getPaymentProxyInterface());
			piConn.authenticate(m_account.getAccountCertificate(),m_account.getPrivateKey());
			theSettings = piConn.getPaymentSettings();
			piConn.disconnect();
		}
		catch (Exception e)
		{
				LogHolder.log(LogLevel.EXCEPTION, LogType.NET,"Error getting payment settings: " + e.getMessage());
		}
		return theSettings;
	}

	private static String getString(String name)
	{
		return JAPMessages.getString(FlatrateDialog.class.getName() + "_" + name);
    }
}
