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
package jap.pay.wizardnew;

import java.util.Enumeration;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.GregorianCalendar;

import java.awt.Component;
import java.awt.Container;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import anon.pay.xml.XMLPassivePayment;
import anon.pay.xml.XMLPaymentOption;
import anon.pay.xml.XMLPaymentOptions;
import anon.pay.xml.XMLTransCert;
import anon.util.Util;
import gui.JAPMessages;
import gui.dialog.DialogContentPane;
import gui.dialog.DialogContentPane.IWizardSuitable;
import gui.dialog.DialogContentPaneOptions;
import gui.dialog.JAPDialog;
import gui.dialog.WorkerContentPane;
import logging.LogType;
import gui.JAPJIntField;
import java.util.Date;
import gui.GUIUtils;
import anon.util.Base64;
import jap.JAPController;

public class PassivePaymentPane extends DialogContentPane implements IWizardSuitable
{
	/** Messages */
	private static final String MSG_ENTER = PassivePaymentPane.class.
		getName() + "_enter";
	private static final String MSG_ERRALLFIELDS = PassivePaymentPane.class.
		getName() + "_errallfields";
	private static final String MSG_CARDCOMPANY = PassivePaymentPane.class.
		getName() + "_cardcompany";
	private static final String MSG_CARDOWNER = PassivePaymentPane.class.
		getName() + "_cardowner";
	private static final String MSG_CARDVALIDITY = PassivePaymentPane.class.
		getName() + "_cardvalidity";
	private static final String MSG_CARDNUMBER = PassivePaymentPane.class.
		getName() + "_cardnumber";
	private static final String MSG_CARDCHECKNUMBER = PassivePaymentPane.class.
		getName() + "_cardchecknumber";

	/** Images */
	public static final String IMG_CREDITCARDSECURITY = PassivePaymentPane.class.
		getName() + "_creditcardsecurity.gif";

	public static final String[] creditCardDataKeys = { "creditcardtype","number","owner","valid","checknumber" };

	private Container m_rootPanel;
	private GridBagConstraints m_c;
	private String m_language;
	private Vector m_inputFields;
	private XMLPaymentOption m_selectedOption;
	private XMLPaymentOptions m_paymentOptions;

	/** Fields for special credit card form */
	private JComboBox m_cbCompany, m_cbMonth, m_cbYear;
	private JTextField m_tfCardOwner;
	private JAPJIntField m_tfCardNumber1, m_tfCardNumber2, m_tfCardNumber3, m_tfCardNumber4,
		m_tfCardCheckNumber;

	public PassivePaymentPane(final JAPDialog a_parentDialog, DialogContentPane a_previousContentPane)
	{
		super(a_parentDialog, "Dummy Text<br>Dummy Text<br>DummyText",
			  new Layout(JAPMessages.getString(MSG_ENTER), MESSAGE_TYPE_PLAIN),
			  new DialogContentPaneOptions(OPTION_TYPE_OK_CANCEL, a_previousContentPane));
		setDefaultButtonOperation(ON_CLICK_DISPOSE_DIALOG | ON_YESOK_SHOW_NEXT_CONTENT |
								  ON_NO_SHOW_PREVIOUS_CONTENT);
		m_language = JAPMessages.getLocale().getLanguage();
		m_rootPanel = this.getContentPane();
		m_c = new GridBagConstraints();
		m_rootPanel.setLayout(new GridBagLayout());
		m_c = new GridBagConstraints();
		m_c.gridx = 0;
		m_c.gridy = 0;
		m_c.weightx = 0;
		m_c.weightx = 0;
		m_c.insets = new Insets(5, 5, 5, 5);
		m_c.anchor = GridBagConstraints.NORTHWEST;
		m_c.fill = GridBagConstraints.NONE;

		//Add some dummy labels for dialog sizing
		/*
		for (int i = 0; i < 12; i++)
		{
			m_rootPanel.add(new JLabel("..................................................."),
							m_c);
			m_c.gridy++;
		}*/
	}

	public void showGenericForm()
	{
		m_rootPanel.removeAll();
		m_rootPanel = this.getContentPane();
		m_c = new GridBagConstraints();
		m_rootPanel.setLayout(new GridBagLayout());
		m_c = new GridBagConstraints();
		m_c.gridx = 0;
		m_c.gridy = 0;
		m_c.weightx = 0;
		m_c.weightx = 0;
		m_c.insets = new Insets(5, 5, 5, 5);
		m_c.anchor = GridBagConstraints.NORTHWEST;
		m_c.fill = GridBagConstraints.NONE;
		m_c.gridwidth = 1;
		setText(m_selectedOption.getDetailedInfo(m_language));
		JLabel label;

		JTextField textField = null;
		JComboBox comboBox = null;

		m_inputFields = new Vector();
		Vector inputFields = m_selectedOption.getInputFields();

		for (int i = 0; i < inputFields.size(); i++)
		{
			String[] field = (String[]) inputFields.elementAt(i);

			if (field[2].equalsIgnoreCase(m_language))
			{
				label = new JLabel("<html>" + field[1] + "</html>");
				//If the input field asks for credit card type we use a combobox
				//that displays all accepted cards instead of a simple text field
				if (field[0].equalsIgnoreCase("creditcardtype"))
				{
					String acceptedCards = m_paymentOptions.getAcceptedCreditCards();
					StringTokenizer st = new StringTokenizer(acceptedCards, ",");
					comboBox = new JComboBox();
					comboBox.setName(field[0]);
					while (st.hasMoreTokens())
					{
						comboBox.addItem(st.nextToken());
					}
					m_inputFields.addElement(comboBox);
					m_rootPanel.add(label, m_c);
					m_c.gridx++;
					m_rootPanel.add(comboBox, m_c);
				}
				else
				{
					textField = new JTextField(15);
					textField.setName(field[0]);
					m_inputFields.addElement(textField);
					m_rootPanel.add(label, m_c);
					m_c.gridx++;
					m_rootPanel.add(textField, m_c);
				}

				m_c.gridy++;
				m_c.gridx = 0;
			}
		}
	}

	public XMLPassivePayment getEnteredInfo()
	{
		if (m_selectedOption.isGeneric())
		{
			return getEnteredGenericInfo();
		}
		if (m_selectedOption.getName().equalsIgnoreCase("creditcard"))
		{
			return getEnteredCreditCardInfo();
		}

		return null;
	}

	/**
	 * getEnteredCreditCardInfo
	 *
	 * @return XMLPassivePayment
	 */
	private XMLPassivePayment getEnteredCreditCardInfo()
	{
		/** Construct PassivePayment object */
		XMLTransCert transCert = null;
		DialogContentPane somePreviousPane = getPreviousContentPane();
		//get transaction certificate
	    //this elaborate structure is necessary since there are several WorkerContentPanes in the wizard returning different kinds of objects as value
		while (transCert == null)
		{
			if (somePreviousPane instanceof WorkerContentPane)
			{
				WorkerContentPane wcp = (WorkerContentPane) somePreviousPane;
				Object value = wcp.getValue();
				if (value instanceof XMLTransCert)
				{
					transCert = (XMLTransCert) value;
				}
				else
				{
					somePreviousPane = somePreviousPane.getPreviousContentPane();
				}
			}
			else
			{
				somePreviousPane = somePreviousPane.getPreviousContentPane();
			}
		}
		//get amount and currency from VolumePlanSelectionPane (endless loop in none found!)
		somePreviousPane = getPreviousContentPane();
		while (! (somePreviousPane instanceof VolumePlanSelectionPane) )
		{
			somePreviousPane = somePreviousPane.getPreviousContentPane();
		}
		VolumePlanSelectionPane vpsp = (VolumePlanSelectionPane) somePreviousPane;
		String amount = vpsp.getAmount();
		String currency = vpsp.getCurrency();
		XMLPassivePayment pp = new XMLPassivePayment();
		pp.setTransferNumber(transCert.getTransferNumber());

		pp.setAmount(Long.parseLong(amount)); //because pp expects whole euros as double, but we deal with eurocents

		pp.setCurrency(currency);
		pp.setPaymentName(m_selectedOption.getName());

		pp.addData("creditcardtype", (String) m_cbCompany.getSelectedItem());
		pp.addData("number",
				   m_tfCardNumber1.getText() + m_tfCardNumber2.getText() + m_tfCardNumber3.getText() +
				   m_tfCardNumber4.getText());
		pp.addData("owner",Base64.encode(m_tfCardOwner.getText().getBytes(), false));
		pp.addData("valid", (String) m_cbMonth.getSelectedItem() + "/" + (String) m_cbYear.getSelectedItem());
		pp.addData("checknumber", m_tfCardCheckNumber.getText());

		return pp;
	}

	public XMLPassivePayment getEnteredGenericInfo()
	{
		/** Construct PassivePayment object */
		XMLTransCert transCert = null;
		DialogContentPane somePreviousPane = getPreviousContentPane();
		//get transaction certificate
		//this elaborate structure is necessary since there are several WorkerContentPanes in the wizard returning different kinds of objects as value
		while (transCert == null)
		{
			if (somePreviousPane instanceof WorkerContentPane)
			{
				WorkerContentPane wcp = (WorkerContentPane) somePreviousPane;
				Object value = wcp.getValue();
				if (value instanceof XMLTransCert)
				{
					transCert = (XMLTransCert) value;
				}
				else
				{
					somePreviousPane = somePreviousPane.getPreviousContentPane();
				}
			}
			else
			{
				somePreviousPane = somePreviousPane.getPreviousContentPane();
			}
		}
		//get amount and currency from VolumePlanSelectionPane (endless loop in none found!)
		somePreviousPane = getPreviousContentPane();
		while (! (somePreviousPane instanceof VolumePlanSelectionPane) )
		{
			somePreviousPane = somePreviousPane.getPreviousContentPane();
		}
		VolumePlanSelectionPane vpsp = (VolumePlanSelectionPane) somePreviousPane;
		String amount = vpsp.getAmount();
		String currency = vpsp.getCurrency();

		XMLPassivePayment pp = new XMLPassivePayment();
		pp.setTransferNumber(transCert.getTransferNumber());
		pp.setAmount(Long.parseLong(amount));
		pp.setCurrency(currency);
		pp.setPaymentName(m_selectedOption.getName());
		Enumeration fields = m_inputFields.elements();
		while (fields.hasMoreElements())
		{
			Component comp = (Component) fields.nextElement();
			JTextField curField;
			if (comp instanceof JTextField)
			{
				curField = (JTextField) comp;
				String name = curField.getName();
				String text = curField.getText();
				pp.addData( name, text);
			}
			else if (comp instanceof JComboBox)
			{
				pp.addData( ( (JComboBox) comp).getName(),
						   (String) ( (JComboBox) comp).getSelectedItem());
			}
		}
		return pp;
	}

	public CheckError[] checkYesOK()
	{
		CheckError error[] = new CheckError[1];
		if (m_selectedOption.getType().equals(XMLPaymentOption.OPTION_PASSIVE))
		{
			if (m_selectedOption.isGeneric())
			{
				Enumeration e = m_inputFields.elements();
				while (e.hasMoreElements())
				{
					Component c = (Component) e.nextElement();
					if (c instanceof JTextField)
					{
						JTextField tf = (JTextField) c;
						if (tf.getText() == null || tf.getText().trim().equals(""))
						{
							error[0] = new CheckError(JAPMessages.getString(MSG_ERRALLFIELDS), LogType.PAY);
							return error;
						}
					}
				}
			}
			else
			{
				boolean ok = true;
				if (m_tfCardCheckNumber.getText() == null || m_tfCardCheckNumber.getText().trim().equals(""))
				{
					ok = false;
				}
				if (m_tfCardNumber1.getText() == null || m_tfCardNumber1.getText().trim().equals(""))
				{
					ok = false;
				}
				if (m_tfCardNumber2.getText() == null || m_tfCardNumber2.getText().trim().equals(""))
				{
					ok = false;
				}
				if (m_tfCardNumber3.getText() == null || m_tfCardNumber3.getText().trim().equals(""))
				{
					ok = false;
				}
				if (m_tfCardNumber4.getText() == null || m_tfCardNumber4.getText().trim().equals(""))
				{
					ok = false;
				}
				if (m_tfCardOwner.getText() == null || m_tfCardOwner.getText().trim().equals(""))
				{
					ok = false;
				}

				if (!ok)
				{
					error[0] = new CheckError(JAPMessages.getString(MSG_ERRALLFIELDS), LogType.PAY);
					return error;
				}
				else
				{
					return null;
				}
			}
		}
		return null;
	}

	public CheckError[] checkUpdate()
	{
		showForm();
		return null;
	}

	public void showForm()
	{
		DialogContentPane somePreviousPane = getPreviousContentPane();
		while (! (somePreviousPane instanceof MethodSelectionPane) )
		{
			somePreviousPane = somePreviousPane.getPreviousContentPane();
		}
		MethodSelectionPane mpsp = (MethodSelectionPane) somePreviousPane;
		m_selectedOption = mpsp.getSelectedPaymentOption();

	    //this elaborate structure is necessary since there are several WorkerContentPanes in the wizard returning different kinds of objects as value
		m_paymentOptions = null;
	    while (m_paymentOptions == null)
		{
			if (somePreviousPane instanceof WorkerContentPane)
			{
				WorkerContentPane wcp = (WorkerContentPane) somePreviousPane;
				Object value = wcp.getValue();
				if (value instanceof XMLPaymentOptions)
				{
					m_paymentOptions = (XMLPaymentOptions) value;
				}
				else
				{
					somePreviousPane = somePreviousPane.getPreviousContentPane();
				}
			}
			else
			{
				somePreviousPane = somePreviousPane.getPreviousContentPane();
			}
		}

		if (m_selectedOption.isGeneric())
		{
			showGenericForm();
		}
		else
		{
			if (m_selectedOption.getName().equalsIgnoreCase("creditcard"))
			{
				showCreditCardForm();
			}
			/** @todo Show special forms for other payment names*/
		}
	}

	/**
	 * showCreditCardForm
	 */
	private void showCreditCardForm()
	{
		JAPJIntField.IIntFieldBounds bounds;

		m_rootPanel.removeAll();
		m_rootPanel = this.getContentPane();
		m_c = new GridBagConstraints();
		m_rootPanel.setLayout(new GridBagLayout());
		m_c.gridx = 0;
		m_c.gridy = 0;
		m_c.weightx = 0;
		m_c.weightx = 0;
		m_c.insets = new Insets(5, 5, 5, 5);
		m_c.anchor = GridBagConstraints.NORTHWEST;
		m_c.fill = GridBagConstraints.NONE;

		m_c.gridwidth = 1;

		String acceptedCards = m_paymentOptions.getAcceptedCreditCards();
		StringTokenizer st = new StringTokenizer(acceptedCards, ",");
		m_cbCompany = new JComboBox();
		while (st.hasMoreTokens())
		{
			m_cbCompany.addItem(st.nextToken());
		}
		m_rootPanel.add(m_cbCompany, m_c);

		bounds = new JAPJIntField.IntFieldUnlimitedZerosBounds(9999);
		m_c.gridy++;
		m_c.gridwidth = 1;
		m_c.fill = GridBagConstraints.HORIZONTAL;
		m_rootPanel.add(new JLabel(JAPMessages.getString(MSG_CARDNUMBER)), m_c);

		JPanel panelCardNumer = new JPanel(new GridBagLayout());
		m_c.gridwidth = 4;
		m_c.gridx++;
		m_c.weightx = 1.0;
		m_c.insets = new Insets(0, 5, 0, 5);
		m_rootPanel.add(panelCardNumer, m_c);

		GridBagConstraints constrCardNr = new GridBagConstraints();

		constrCardNr.gridx = 0;
		constrCardNr.gridy = 0;
		constrCardNr.fill = GridBagConstraints.HORIZONTAL;
		constrCardNr.weightx = 1.0;
		constrCardNr.insets = new Insets(5, 0, 5, 5);
		m_tfCardNumber1 = new JAPJIntField(bounds, true);
		panelCardNumer.add(m_tfCardNumber1, constrCardNr);
		//m_rootPanel.add(m_tfCardNumber1, m_c);

		constrCardNr.gridx++;
		constrCardNr.insets = new Insets(5, 5, 5, 5);
		m_tfCardNumber2 = new JAPJIntField(bounds, true);
		//m_rootPanel.add(m_tfCardNumber2, m_c);
		panelCardNumer.add(m_tfCardNumber2, constrCardNr);

		constrCardNr.gridx++;
		m_tfCardNumber3 = new JAPJIntField(bounds, true);
		//m_rootPanel.add(m_tfCardNumber3, m_c);
		panelCardNumer.add(m_tfCardNumber3, constrCardNr);

		constrCardNr.gridx++;
		constrCardNr.insets = new Insets(5, 5, 5, 0);
		m_tfCardNumber4 = new JAPJIntField(bounds, true);
		//m_rootPanel.add(m_tfCardNumber4, m_c);
		panelCardNumer.add(m_tfCardNumber4, constrCardNr);

		m_c.insets = new Insets(5, 5, 5, 5);
		m_c.weightx = 0;
		m_c.gridx = 0;
		m_c.gridy++;
		m_c.gridwidth = 1;
		m_rootPanel.add(new JLabel(JAPMessages.getString(MSG_CARDOWNER)), m_c);
		m_c.gridx++;
		m_c.gridwidth = 4;
		m_c.fill = GridBagConstraints.HORIZONTAL;
		m_tfCardOwner = new JTextField();
		m_rootPanel.add(m_tfCardOwner, m_c);



		m_c.weightx = 0;
		m_c.gridx = 0;
		m_c.gridy++;
		m_c.gridwidth = 1;
		//m_c.fill = GridBagConstraints.NONE;
		m_rootPanel.add(new JLabel(JAPMessages.getString(MSG_CARDVALIDITY)), m_c);
		m_c.gridx++;
		//m_c.fill = GridBagConstraints.NONE;
		m_cbMonth = new JComboBox();
		for (int i = 1; i < 13; i++)
		{
			m_cbMonth.addItem(String.valueOf(i));
		}
		m_rootPanel.add(m_cbMonth, m_c);

		m_c.gridx = 2;
		int thisYear = new GregorianCalendar().get(GregorianCalendar.YEAR);
		m_cbYear = new JComboBox();
		for (int i = 0; i < 21; i++)
		{
			m_cbYear.addItem(String.valueOf(thisYear + i));
		}
		m_rootPanel.add(m_cbYear, m_c);

		m_c.gridx++;
		m_c.gridwidth = 2;
		m_c.gridheight = 2;
		m_rootPanel.add(new JLabel(GUIUtils.loadImageIcon(IMG_CREDITCARDSECURITY, true)), m_c);


		m_c.gridx = 0;
		m_c.gridy++;
		m_c.gridwidth = 1;
		m_c.gridheight = 1;
		m_c.fill = GridBagConstraints.HORIZONTAL;
		m_rootPanel.add(new JLabel(JAPMessages.getString(MSG_CARDCHECKNUMBER)), m_c);
		m_c.gridx++;
		m_c.gridwidth = 1;
		m_tfCardCheckNumber = new JAPJIntField(bounds, true);
		m_rootPanel.add(m_tfCardCheckNumber, m_c);

		setText(m_selectedOption.getDetailedInfo(m_language));
	}
}
