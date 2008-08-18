package jap.pay;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

import anon.pay.xml.XMLPassivePayment;
import gui.JAPHtmlMultiLineLabel;
import gui.JAPMessages;
import gui.dialog.JAPDialog;
import jap.JAPUtil;
import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;

/**
 * Shows detailed info for a passive payment transaction
 * invoked by clicking
 *
 * @author Elmar Schraml
 *
 */

public class PassivePaymentDetails extends JAPDialog implements ActionListener
{
	private static final String MSG_HEADING = PassivePaymentDetails.class.getName() + "_heading";
	private static final String MSG_TITLE = PassivePaymentDetails.class.getName() + "_title";
	private static final String MSG_CLOSEBUTTON = PassivePaymentDetails.class.getName() + "_closebutton";
	private static final String MSG_UNKNOWN_PAYMENT = PassivePaymentDetails.class.getName() + "_unknownpayment";
	private static final String MSG_NOT_SHOWN = PassivePaymentDetails.class.getName() + "_notshown";
	private static final String MSG_PAID_BY = PassivePaymentDetails.class.getName() + "_paidby";

	//credit card details - these are not used directly, but needed to dynamically get the field names
	private static final String MSG_CREDITCARDWORD =     PassivePaymentDetails.class.getName() + "_creditcardword";
	private static final String MSG_CREDITCARDTYPE = PassivePaymentDetails.class.getName() + "_creditcardtype";
	private static final String MSG_NUMBER = PassivePaymentDetails.class.getName() + "_number";
	private static final String MSG_OWNER = PassivePaymentDetails.class.getName() + "_owner";
	private static final String MSG_VALID = PassivePaymentDetails.class.getName() + "_valid";
	private static final String MSG_CHECKNUMBER = PassivePaymentDetails.class.getName() + "_checknumber";

	//general transaction data
	private static final String MSG_AMOUNT = PassivePaymentDetails.class.getName() + "_amount";
	private static final String MSG_TRANSFERNUMBER = PassivePaymentDetails.class.getName() + "_transfernumber";
	private static final String MSG_ACCOUNTNUMBER = PassivePaymentDetails.class.getName() + "_accountnumber";

	private GridBagConstraints m_c;
	private JButton m_closeButton;

	public PassivePaymentDetails(JAPDialog a_parent, XMLPassivePayment paymentToShow, long transfernumber, long accountnumber)
	{
		super(a_parent, JAPMessages.getString(MSG_TITLE));

		try
		{
			setDefaultCloseOperation(DISPOSE_ON_CLOSE);
			buildDialog(paymentToShow, transfernumber, accountnumber);
			setResizable(false);
			pack();
			setVisible(true);
		}
		catch (Exception e)
		{
			LogHolder.log(LogLevel.EXCEPTION, LogType.PAY,"Could not create PassivePaymentDetails: ", e);
		}

	}


	private void buildDialog(XMLPassivePayment pp, long transfernumber, long accountnumber)
	{
		m_c = new GridBagConstraints();
		m_c.anchor = GridBagConstraints.NORTH;
		m_c.insets = new Insets(10, 30, 10, 30);
		m_c.gridx = 0;
		m_c.gridy = 0;
		m_c.weighty = 0;
		m_c.weightx = 0;
		getContentPane().setLayout(new GridBagLayout());

		JAPHtmlMultiLineLabel heading = new JAPHtmlMultiLineLabel("<h3>" + JAPMessages.getString(MSG_HEADING) + "</h3");
		getContentPane().add(heading,m_c);
		m_c.gridy++;

		//show general transaction data

		JPanel transactionDetailsPanel = buildTransactionDetailsPanel(accountnumber, transfernumber, pp.getAmount() );
		getContentPane().add(transactionDetailsPanel,m_c);
		m_c.gridy++;

		//adds payment details that are dependent on the payment type
		JPanel paymentDetailsPanel = buildPaymentDetailsPanel(pp);
		getContentPane().add(paymentDetailsPanel,m_c);
		m_c.gridy++;

		m_closeButton = new JButton(JAPMessages.getString(MSG_CLOSEBUTTON));
		m_closeButton.addActionListener(this);
		m_c.gridy++;
		getContentPane().add(m_closeButton,m_c);

	}

	private JPanel buildTransactionDetailsPanel(long accountnumber, long transfernumber, long amount)
	{
		JPanel transactionPanel = new JPanel();
		BoxLayout verticalBoxLayout = new BoxLayout(transactionPanel,BoxLayout.Y_AXIS);
		transactionPanel.setLayout(verticalBoxLayout);

		String accNum = new Long(accountnumber).toString();
		String accountDesignation = JAPMessages.getString(MSG_ACCOUNTNUMBER);
		JLabel accountLabel = new JLabel(accountDesignation + ": " + accNum);
		transactionPanel.add(accountLabel);


		String tan = (new Long(transfernumber)).toString();
		String tanDesignation = JAPMessages.getString(MSG_TRANSFERNUMBER);
		JLabel tanLabel = new JLabel(tanDesignation + ": " + tan);
		transactionPanel.add(tanLabel,m_c);


		String amountString = JAPUtil.formatEuroCentValue(amount);
		String amountDesignation = JAPMessages.getString(MSG_AMOUNT);
		JLabel amountLabel = new JLabel(amountDesignation + ": " + amountString);
		transactionPanel.add(amountLabel, m_c);

		transactionPanel.add(Box.createRigidArea(new Dimension(0,10)));
		return transactionPanel;
	}

	private JPanel buildPaymentDetailsPanel(XMLPassivePayment pp)
	{
		JPanel paymentPanel = new JPanel();
		BoxLayout verticalBoxLayout = new BoxLayout(paymentPanel,BoxLayout.Y_AXIS);
		paymentPanel.setLayout(verticalBoxLayout);

		String paymentOptionUsed = pp.getPaymentName();

		/******* customize the layout here depending on the payment option used
		 *       available data depends on the payment type, so we can't just use a generic layout
		 *       .getPaymentName() return the non-localized, internal name of a payment option,
		 *       as saved in column "name" of database table "paymentoptions"
		 */
		if (paymentOptionUsed.equalsIgnoreCase("CreditCard") )
		{
		    //for security reasons, we do NOT store credit card data,
			//so we have nothing to show here
			//show empty/error panel
			JLabel errorLabel = new JLabel(JAPMessages.getString(MSG_NOT_SHOWN));
			paymentPanel.add(errorLabel);


			/* this shows all recorded data as entered in the passivePaymentPane
			 *

			String paidby = JAPMessages.getString(MSG_PAID_BY);
			String creditCard = JAPMessages.getString(MSG_CREDITCARDWORD);
			JLabel paymentType = new JLabel(paidby + " " + creditCard);
			paymentPanel.add(paymentType);
			paymentPanel.add(Box.createRigidArea(new Dimension(0,20)));


			String[] keys = PassivePaymentPane.creditCardDataKeys;
			String curKey;
			String curIdentifier;
			String curData;
			JLabel curLine;
		 	for (int i = 0; i < keys.length; i++)
			{
				 curKey = keys[i];
				 String messageName = PassivePaymentDetails.class.getName() + "_" + new String(curKey).toLowerCase();
				 curIdentifier = JAPMessages.getString(messageName); //e.g. key "number" will be labeled with "MSG_NUMBER"
				 curData = pp.getPaymentData(curKey);
			     curLine = new JLabel(curIdentifier + ": " + curData);
				 paymentPanel.add(curLine);
				 paymentPanel.add(Box.createRigidArea(new Dimension(0,10)));
			}
		  */

		}
		else if (paymentOptionUsed.equalsIgnoreCase("Paysafecard") )
		{
			//no data to show (psc payments are stored in table paysafecardpayments,
			//not passivepayments, need to change the calling method in TransactionOverviewDialog
			//if you want to prived paysafecard data, too
		}
		else
		{
			//show empty/error panel
			JLabel errorLabel = new JLabel(JAPMessages.getString(MSG_UNKNOWN_PAYMENT));
			paymentPanel.add(errorLabel);
		}

		paymentPanel.add(Box.createRigidArea(new Dimension(0,10)));
		return paymentPanel;
	}

	public void actionPerformed(ActionEvent e)
	{
		if (e.getSource() == m_closeButton)
		{
			this.setVisible(false);
		}
	}

}

