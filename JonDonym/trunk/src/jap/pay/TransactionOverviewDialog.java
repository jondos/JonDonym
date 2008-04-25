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

import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;

import anon.pay.BIConnection;
import anon.pay.PayAccount;
import anon.pay.PaymentInstanceDBEntry;
import anon.pay.xml.XMLErrorMessage;
import anon.pay.xml.XMLPassivePayment;
import anon.pay.xml.XMLPaymentOption;
import anon.pay.xml.XMLPaymentOptions;
import anon.pay.xml.XMLTransCert;
import anon.pay.xml.XMLTransactionOverview;
import anon.util.IXMLEncodable;
import gui.GUIUtils;
import gui.JAPMessages;
import gui.dialog.JAPDialog;
import gui.dialog.WorkerContentPane;
import jap.JAPConstants;
import jap.JAPModel;
import jap.JAPUtil;
import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;

/** This dialog shows an overview of transaction numbers for an account
 *
 *  @author Tobias Bayer, Elmar Schraml
 */
public class TransactionOverviewDialog extends JAPDialog implements ActionListener
{
	/** Messages */
	private static final String MSG_OK_BUTTON = TransactionOverviewDialog.class.
		getName() + "_ok_button";
	private static final String MSG_DETAILSBUTTON = TransactionOverviewDialog.class.
		getName() + "_detailsbutton";
	private static final String MSG_RELOADBUTTON = TransactionOverviewDialog.class.
		getName() + "_reloadbutton";
	private static final String MSG_CANCELBUTTON = TransactionOverviewDialog.class.
		getName() + "_cancelbutton";
	private static final String MSG_FETCHING = TransactionOverviewDialog.class.
		getName() + "_fetching";
	private static final String MSG_TAN = TransactionOverviewDialog.class.
		getName() + "_tan";
	private static final String MSG_AMOUNT = TransactionOverviewDialog.class.
		getName() + "_amount";
	private static final String MSG_STATUS = TransactionOverviewDialog.class.
		getName() + "_status";
	private static final String MSG_TRANSACTION_DATE = TransactionOverviewDialog.class.
		getName() + "_transaction_date";
	public static final String MSG_DETAILS_FAILED = TransactionOverviewDialog.class.getName() + "_details_failed";
	public static final String MSG_FETCHING_TAN = TransactionOverviewDialog.class.getName() + "_fetchingTAN";
	private static final String MSG_ACCOUNTNUMBER = TransactionOverviewDialog.class.getName() + "_accountnumber";
	private static final String MSG_VOLUMEPLAN = TransactionOverviewDialog.class.getName() + "_volumeplan";
	private static final String MSG_PAYMENTMETHOD = TransactionOverviewDialog.class.getName() + "_paymentmethod";
	private static final String MSG_USEDSTATUS = TransactionOverviewDialog.class.getName() + "_usedstatus";
	private static final String MSG_OPENSTATUS = TransactionOverviewDialog.class.getName() + "_openstatus";
	private static final String MSG_EXPIREDSTATUS = TransactionOverviewDialog.class.getName() + "_expiredstatus";
	private static final String MSG_PAYMENT_COMPLETED = TransactionOverviewDialog.class.getName() + "_paymentcompleted";
	private static final String MSG_PAYMENT_EXPIRED = TransactionOverviewDialog.class.getName() + "_paymentexpired";
	private static final String MSG_NO_OPEN_TRANSFERS = TransactionOverviewDialog.class.getName() + "_noopentransfers";

	private JTable m_transactionsTable;
	private JButton m_okButton, m_reloadButton, m_detailsButton;

	private JLabel m_fetchingLabel;
	private AccountSettingsPanel m_parent;

	private Vector m_accounts; //contains the PayAccount Objects for which transactions are shown (could be only one, if only for the current account)


	public TransactionOverviewDialog(AccountSettingsPanel a_parent, String title, boolean modal,
									 Vector a_accounts)
	{
		super(GUIUtils.getParentWindow(a_parent.getRootPanel()), title, modal);
		m_parent = a_parent;


		if (a_accounts.size() == 0) //no accounts/transactions to show -> no sense showing an empty dialog, quit with a message
		{
			JAPDialog.showMessageDialog(this,JAPMessages.getString(MSG_NO_OPEN_TRANSFERS));
		}
		else
		{
			try
			{
				m_accounts = a_accounts;
				setDefaultCloseOperation(DISPOSE_ON_CLOSE);
				buildDialog();
				setModal(true);
				setSize(700, 300);
				//pack(); //do not call pack() here, otherwise setSize() has no effect
				setVisible(true);
			}
			catch (Exception e)
			{
				LogHolder.log(LogLevel.EXCEPTION, LogType.PAY,
							  "Could not create TransactionOverviewDialog: " + e.getMessage());
			}
		}
	}

	private void buildDialog() throws Exception
	{
		JPanel rootPanel = new JPanel(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.anchor = c.NORTHWEST;
		c.insets = new Insets(5, 5, 5, 5);

		m_transactionsTable = new JTable();

		c.gridx = 0;
		c.gridy = 0;
		c.weighty = 1;
		c.fill = GridBagConstraints.BOTH;
		rootPanel.add(new JScrollPane(m_transactionsTable), c);
		c.weightx = 0;
		c.weighty = 0;
		c.fill = c.NONE;

		//The fetching label
		c.gridy++;
		m_fetchingLabel = new JLabel(JAPMessages.getString(MSG_FETCHING),
									 GUIUtils.loadImageIcon(JAPConstants.BUSYFN, true), JLabel.LEADING);
		m_fetchingLabel.setHorizontalTextPosition(JLabel.LEADING);
		rootPanel.add(m_fetchingLabel, c);

		//Add the button panel
		c.gridy = 5;
		c.gridx = 0;
		c.weightx = 1;
		c.anchor = c.SOUTHEAST;
		rootPanel.add(buildButtonPanel(), c);

		getContentPane().add(rootPanel);
		showTransactions();

	}

	private JPanel buildButtonPanel()
	{
		JPanel bttnPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));

		//The Details button
		m_detailsButton = new JButton(JAPMessages.getString(MSG_DETAILSBUTTON));
		m_detailsButton.addActionListener(this);
		bttnPanel.add(m_detailsButton);

		//The Reload button
		m_reloadButton = new JButton(JAPMessages.getString(MSG_RELOADBUTTON));
		m_reloadButton.addActionListener(this);
		bttnPanel.add(m_reloadButton);

		//The Ok button
		m_okButton = new JButton(JAPMessages.getString(MSG_CANCELBUTTON));
		m_okButton.addActionListener(this);
		bttnPanel.add(m_okButton);

	    return bttnPanel;
	}

	private void showTransactions()
	{
		m_reloadButton.setEnabled(false);
		m_fetchingLabel.setVisible(true);

		Runnable fillList = new Runnable()
		{
			public void run()
			{
				//get all transactions certs for all accounts
				Vector transCerts = new Vector();
				PayAccount curAccount;
				XMLTransactionOverview overview;
				for (Enumeration allAccounts = m_accounts.elements(); allAccounts.hasMoreElements(); )
				{
					curAccount = (PayAccount) allAccounts.nextElement();
					Vector transCertsOfAccount = curAccount.getTransCerts();
					for (Enumeration e = transCertsOfAccount.elements(); e.hasMoreElements(); )
					{
						transCerts.addElement(e.nextElement() ); //.addAll would be faster, but is post-JDK 1.1.8
					}

				}
				//put TAN for each transCert into an XMLTransacton Overview
				overview = new XMLTransactionOverview(JAPMessages.getLocale().getLanguage());
				for (int i = 0; i < transCerts.size(); i++)
				{
					XMLTransCert cert = (XMLTransCert) transCerts.elementAt(i);
					overview.addTan(cert.getTransferNumber());
				}


				try
				{
					curAccount = (PayAccount) m_accounts.elementAt(0); //just get the first account to find a BI to connect to
					BIConnection biConn = new BIConnection(curAccount.getBI());
					biConn.connect(JAPModel.getInstance().getPaymentProxyInterface());
					biConn.authenticate(curAccount.getAccountCertificate(), curAccount.getPrivateKey());
					overview = biConn.fetchTransactionOverview(overview);
					biConn.disconnect();
					if (overview == null)
					{
						throw new Exception("JPI returned error message rather than transaction overview");
					}
				}
				catch (Exception e)
				{
					LogHolder.log(LogLevel.EXCEPTION, LogType.PAY,
								  "Cannot connect to Payment Instance: " + e.getMessage());
					LogHolder.log(LogLevel.EXCEPTION, LogType.PAY, e);
					m_parent.showPIerror(getRootPane(), e);
					m_fetchingLabel.setVisible(false);
					setVisible(false);
					return; //do not bother to try and fill in table
				}

				MyTableModel tableModel = new MyTableModel(overview);
				m_transactionsTable.setEnabled(true);
				m_transactionsTable.setModel(tableModel);
				m_transactionsTable.addMouseListener(new MouseAdapter()
				{
					public void mouseClicked(MouseEvent e)
					{
						if (e.getClickCount() == 2)
						{
							showTransactionDetailsDialog();
						}
					}
				});



				m_okButton.setText(JAPMessages.getString(MSG_OK_BUTTON));
				m_fetchingLabel.setVisible(false);
				m_reloadButton.setEnabled(true);
			}
		};

		Thread t = new Thread(fillList,"TransactionOverviewDialog");
		t.setDaemon(true);
		t.start();

	}

	public void actionPerformed(ActionEvent e)
	{
		if (e.getSource() == m_okButton)
		{
			dispose();
		}
		else if (e.getSource() == m_reloadButton)
		{
			showTransactions();
		}
		else if (e.getSource() == m_detailsButton)
		{
            showTransactionDetailsDialog();
		}
	}

	public void showTransactionDetailsDialog()
	{
		try
		{
			//get transactionnumber of selected row in table
			int selectedRow = m_transactionsTable.getSelectedRow();
			//this will break if the layout/order of columns of the table is changed, is there a way to get to column by name/id/?
			final String transferNumber = (String) m_transactionsTable.getModel().getValueAt(selectedRow, 1); //1 for second column = transfernumber
			Object value = m_transactionsTable.getModel().getValueAt(selectedRow, 3);//fourth column
			long amount = ((TablecellAmount) value).getLongValue();
			String status = (String) m_transactionsTable.getModel().getValueAt(selectedRow,6);
			String planName = (String) m_transactionsTable.getModel().getValueAt(selectedRow,4);
			String paymentMethod = (String) m_transactionsTable.getModel().getValueAt(selectedRow,5);
			boolean isCompleted = false;
			boolean isExpired = false;
			if (status.equalsIgnoreCase(JAPMessages.getString(MSG_USEDSTATUS)) )
			{
				isCompleted = true;
			}
			else if (status.equalsIgnoreCase(JAPMessages.getString(MSG_EXPIREDSTATUS)) )
			{
				isExpired = true;
			}
			final PayAccount m_account = (PayAccount) m_accounts.elementAt(0); //just to get it to compile

			JAPDialog dialog =
				new JAPDialog(this, JAPMessages.getString(TransactionOverviewDialog.MSG_FETCHING_TAN));
			WorkerContentPane.IReturnRunnable run2 =
				new WorkerContentPane.IReturnRunnable()
			{
				Object xmlReply;
				public void run()
				{
					try
					{
						BIConnection biConn = new BIConnection(m_account.getBI());
						biConn.connect(JAPModel.getInstance().getPaymentProxyInterface());
						biConn.authenticate(m_account.getAccountCertificate(), m_account.getPrivateKey());

						biConn.connect(JAPModel.getInstance().
									   getPaymentProxyInterface());
						biConn.authenticate(m_account.getAccountCertificate(),
											m_account.getPrivateKey());

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
									  JAPMessages.getString(TransactionOverviewDialog.MSG_FETCHING_TAN), run2);

			pane2.updateDialog();
			dialog.pack();
			dialog.setVisible(true);


			if (run2.getValue() == null)
			{
				// interrupted
				return;
			}
			else if (run2.getValue() instanceof Exception &&
					 ! (run2.getValue() instanceof XMLErrorMessage))
			{
				throw (Exception) run2.getValue();
			}
			else if (! (run2.getValue() instanceof IXMLEncodable))
			{
				throw new Exception("Illegal return value!");
			}


			IXMLEncodable xmlReply = (IXMLEncodable)run2.getValue();


			//biConn will return XMLErrorMessage if payment is active (= no matching record in passivepayments)
			//(the transfers table alone does not associate a payment method or type with a transfernumber)
			if (xmlReply instanceof XMLErrorMessage)
			{
				XMLErrorMessage repliedMessage = (XMLErrorMessage) xmlReply;
				if (repliedMessage.getErrorCode() == XMLErrorMessage.ERR_NO_RECORD_FOUND )
				{
					if (isCompleted)
					{
					    JAPDialog.showMessageDialog(this,JAPMessages.getString(MSG_PAYMENT_COMPLETED));
					}
					else if (isExpired)
					{
						JAPDialog.showMessageDialog(this,JAPMessages.getString(MSG_PAYMENT_EXPIRED));
					}
					else
					{
						showActivePaymentDialog(this, transferNumber, amount, m_account, planName, paymentMethod);
					}
				}
				else
				{
					JAPDialog.showMessageDialog(this,JAPMessages.getString(MSG_DETAILS_FAILED));
				}
			} else
			{
				showPassivePaymentDialog(this, (XMLPassivePayment) xmlReply, Long.parseLong(transferNumber), m_account.getAccountNumber());
			}

		} catch (Exception e)
		{
			LogHolder.log(LogLevel.DEBUG, LogType.PAY, "could not get transaction details");
		}
	}

	public static void showActivePaymentDialog(JAPDialog a_parent, String transferNumber, long amount, PayAccount a_account, String planName, String a_paymentMethod)
	{
		String language = JAPMessages.getLocale().getLanguage();
		Vector optionsToShow = getLocalizedActivePaymentsData(language, a_account, a_paymentMethod);

	    ActivePaymentDetails apd = new ActivePaymentDetails(a_parent, optionsToShow, transferNumber,amount, planName);
	}

	/**
	 * getLocalizedActivePaymentsData
	 *
	 * @param lang: String, 2-letter language code, e.g. "en" for english
	 * @param a_paymentMethod: the internal name of the payment method that the user chose when creating the transaction
	 * @return Vector: of Hashtables (one per XMLPaymentOption)
	 *         entries in Hashtable: Strings for keys "name","heading","detailedInfo",
	 *                               Vector of Strings for key "extraInfos"
	 */
	private static Vector getLocalizedActivePaymentsData(String lang, PayAccount a_account, String a_paymentMethod)
	{
		Vector optionsToShow = new Vector();
		try
		{
			PaymentInstanceDBEntry theJPI = a_account.getBI();
			BIConnection biConn = new BIConnection(theJPI);
			biConn.connect(JAPModel.getInstance().getPaymentProxyInterface());
			biConn.authenticate(a_account.getAccountCertificate(), a_account.getPrivateKey());
			XMLPaymentOptions allOptions = biConn.fetchPaymentOptions();
			//optionsToShow: Vector of Hashtables, one per active option, containing strings in current language
			//in case of error, use english
			if (lang.equals("") )
			{
				lang = "en";
			}
			for (Enumeration options = allOptions.getAllOptions().elements();options.hasMoreElements(); )
			{
				XMLPaymentOption curOption = (XMLPaymentOption) options.nextElement();

				//skip passive payment options
				if (curOption.getType().equals("passive") )
				{
					continue; //we only need data for active and mixed payment options
				}

				Hashtable curOptionData = new Hashtable();
				curOptionData.put("name",curOption.getName());
				curOptionData.put("heading",curOption.getHeading(lang));
				curOptionData.put("detailedInfo",curOption.getDetailedInfo(lang));
				curOptionData.put("extraInfos",curOption.getLocalizedExtraInfoText(lang));
				optionsToShow.addElement(curOptionData);
			}
		} catch (Exception e)
		{
			LogHolder.log(LogLevel.DEBUG, LogType.PAY, "could not get payment options");
		}
		return optionsToShow;
	}

	public static void showPassivePaymentDialog(JAPDialog a_parent, XMLPassivePayment pp, long transfernumber, long accountnumber)
	{
		PassivePaymentDetails ppd = new PassivePaymentDetails(a_parent,pp, transfernumber, accountnumber);
	}

	/**
	 * Tabel model
	 */
	private class MyTableModel extends AbstractTableModel
	{
		private XMLTransactionOverview m_overview;

		public MyTableModel(XMLTransactionOverview a_overview)
		{
			super();
			m_overview = a_overview;
		}

		public int getColumnCount()
		{
			return 7;
		}

		public int getRowCount()
		{
			return m_overview.size();
		}

		public Class getColumnClass(int c)
		{
			switch (c)
			{
				case 0:
					return String.class; //accountnumber
				case 1:
					return String.class; //transaction number
				case 2:
					return Date.class;   //creation date
				case 3:
					return TablecellAmount.class; //amount
				case 5:
					return String.class; //volume plan
				case 6:
					return String.class; //payment method
			    case 7:
					return String.class;   //used
				default:
					return Object.class;
			}
		}

		public Object getValueAt(int rowIndex, int columnIndex)
		{
			Hashtable transactionData = (Hashtable) m_overview.getTans().elementAt(rowIndex);
			switch (columnIndex)
			{
				case 0:
					String accountNumber = (String) transactionData.get(XMLTransactionOverview.KEY_ACCOUNTNUMBER);
					if (accountNumber == null)
					{
						return new String("");
					}
					else
					{
						return accountNumber;
					}
				case 1:
					return transactionData.get(XMLTransactionOverview.KEY_TAN);
				case 2:
				   try
				   {
					   String dateAsString = (String) transactionData.get(XMLTransactionOverview.KEY_DATE);
					   long dateAsMillis = Long.parseLong(dateAsString);
					   return new Date(dateAsMillis);
				   }
				   catch (Exception e)
				   {
					   return null;
				   }
				case 3:
					try
					{
						String amountString = (String)transactionData.get(XMLTransactionOverview.KEY_AMOUNT);
						long amount = Long.parseLong(amountString);
						TablecellAmount theAmount = new TablecellAmount(amount);
						return theAmount;
					}
					catch (Exception e)
					{
						return new String("");
					}
				case 4:
					try
					{
						String volumePlan = (String) transactionData.get(XMLTransactionOverview.KEY_VOLUMEPLAN);
						return volumePlan;
					}
					catch (Exception e)
					{
						return new String("");
					}
				case 5:
					try
					{
						String paymentMethod = (String) transactionData.get(XMLTransactionOverview.KEY_PAYMENTMETHOD);
						if (paymentMethod.equalsIgnoreCase("null"))
						{
							return new String("");
						}
						else
						{
							return "<html>" + paymentMethod + "</html>";
						}
					}
					catch (Exception e)
					{
						return new String("");
					}
				case 6:
					//show as open or used, with localized String
					try
					{
						return transactionStatus(transactionData);
					}
					catch (Exception e)
					{
						return new String("");
					}
				default:
					return JAPMessages.getString("unknown");
			}
		}

	    private String transactionStatus(Hashtable a_transactionData)
		{
			String used = (String) a_transactionData.get(XMLTransactionOverview.KEY_USED);
			boolean isUsed = (new Boolean(used)).booleanValue();
			if (isUsed)
			{
				return JAPMessages.getString(MSG_USEDSTATUS);
			}
			else
			{
				return JAPMessages.getString(MSG_OPENSTATUS);
			}
		}

		public String getColumnName(int col)
		{
			switch (col)
			{
				case 0:
					return JAPMessages.getString(MSG_ACCOUNTNUMBER);
				case 1:
					return JAPMessages.getString(MSG_TAN);
				case 2:
					return JAPMessages.getString(MSG_TRANSACTION_DATE);
				case 3:
					return JAPMessages.getString(MSG_AMOUNT);
				case 4:
					return JAPMessages.getString(MSG_VOLUMEPLAN);
				case 5:
					return JAPMessages.getString(MSG_PAYMENTMETHOD);
			    case 6:
					return JAPMessages.getString(MSG_STATUS);
				default:
					return "---";
			}
		}

		public boolean isCellEditable(int col, int row)
		{
			return false;
		}
	}

	protected class TablecellAmount {

		long m_theAmount;
		public TablecellAmount(long amount)
		{
			m_theAmount = amount;
		}

		public String toString()
		{
			return JAPUtil.formatEuroCentValue(m_theAmount);
		}

		public long getLongValue()
		{
			return m_theAmount;
		}

	}
}
