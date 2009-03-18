/*
 Copyright (c) 2000-2007, The JAP-Team
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

import java.util.Vector;

import java.awt.Container;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.ButtonGroup;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import anon.pay.BIConnection;
import anon.pay.PayAccount;
import anon.pay.PaymentInstanceDBEntry;
import anon.pay.xml.XMLVolumePlan;
import anon.pay.xml.XMLVolumePlans;
import anon.util.JAPMessages;
import anon.util.Util;
import gui.GUIUtils;
import gui.JapCouponField;
import gui.dialog.DialogContentPane;
import gui.dialog.DialogContentPane.IWizardSuitable;
import gui.dialog.DialogContentPaneOptions;
import gui.dialog.JAPDialog;
import gui.dialog.WorkerContentPane;
import jap.JAPModel;
import jap.JAPUtil;
import jap.pay.AccountSettingsPanel.AccountCreationPane;
import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;
import javax.swing.BorderFactory;

/**
 * Shows all available volume plans (as sent by the jpi in the form of a XMLVolumePlans object),
 * and allows the user to pick one.
 * Part of the wizard for charging an account.
 *
 * @author Elmar Schraml
 */
 public class VolumePlanSelectionPane extends DialogContentPane implements  IWizardSuitable, ActionListener, DocumentListener
{
	/* Messages */
	private static final String MSG_PRICE = VolumePlanSelectionPane.class.getName() + "_price";
	private static final String MSG_HEADING = VolumePlanSelectionPane.class.getName() + "_heading";
	private static final String MSG_VOLUME = VolumePlanSelectionPane.class.getName() + "_volume";
	private static final String MSG_UNLIMITED = VolumePlanSelectionPane.class.getName() + "_unlimited";
	private static final String MSG_ERROR_NO_PLAN_CHOSEN = VolumePlanSelectionPane.class.getName() + "_errorNoPlanChosen";
	private static final String MSG_VALIDUNTIL = VolumePlanSelectionPane.class.getName() + "_validuntil";
	//private static final String MSG_VOLUMEPLAN = VolumePlanSelectionPane.class.getName() + "_volumeplan";
	private static final String MSG_CHOOSEAPLAN = VolumePlanSelectionPane.class.getName() + "_chooseaplan";
	private static final String MSG_ENTER_COUPON = VolumePlanSelectionPane.class.getName() + "_entercouponcode";
	private static final String MSG_PLAN_OR_COUPON = VolumePlanSelectionPane.class.getName() + "_planorcoupon";
	private static final String MSG_INVALID_COUPON = VolumePlanSelectionPane.class.getName() + "_invalidcoupon";
	private static final String MSG_COUPON_INCOMPLETE = VolumePlanSelectionPane.class.getName() + "_couponincomplete";

	private XMLVolumePlans m_allPlans;
	private XMLVolumePlan m_selectedPlan;
	private JapCouponField m_coupon1;
	private JapCouponField m_coupon2;
	private JapCouponField m_coupon3;
	private JapCouponField m_coupon4;
	private GridBagConstraints m_c = new GridBagConstraints();
	private Container m_rootPanel;
	private ButtonGroup m_rbGroup;
	private JRadioButton m_couponButton;

	private WorkerContentPane m_fetchPlansPane;
	private boolean m_isNewAccount;
	private boolean m_isCouponUsed;
	private boolean m_hasBeenShown = false;

	public VolumePlanSelectionPane(JAPDialog a_parentDialog, WorkerContentPane a_previousContentPane, boolean a_newAccount)
	{
		super(a_parentDialog, JAPMessages.getString(MSG_CHOOSEAPLAN),
			  new Layout(JAPMessages.getString(MSG_HEADING), MESSAGE_TYPE_PLAIN),
			  new DialogContentPaneOptions(OPTION_TYPE_OK_CANCEL, a_previousContentPane));
		setDefaultButtonOperation(ON_CLICK_DISPOSE_DIALOG | ON_YESOK_SHOW_NEXT_CONTENT |
								  ON_NO_SHOW_PREVIOUS_CONTENT);

		m_fetchPlansPane = a_previousContentPane;
		m_isNewAccount = a_newAccount;

		m_rbGroup = new ButtonGroup();
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



		//show some dummy plans
		//(real plans are shown by showVolumePlans(), called by checkUpdate()
		// (heaven knows why immediately showing them here in the constructor doesnt work)

		for (int i = 0; i < 10; i++)
		{
			XMLVolumePlan dummyPlan =
				new XMLVolumePlan("dummy", "Dummy        for sizing",100,2,"months",2000000);
			addPlan(dummyPlan);
		}
	}

	public XMLVolumePlan getSelectedVolumePlan()
	{
		return m_selectedPlan;
	}

	public String getEnteredCouponCode()
	{
		return getCouponString();
	}

	private void setCouponUsed(boolean curValue)
	{
		m_isCouponUsed = curValue;
    }

	public boolean isCouponUsed()
	{
		return m_isCouponUsed;
	}

	public boolean isCouponComplete()
	{
	   return PayAccount.checkCouponCode(getCouponString()) != null;
	}

	/**
	 * returns the amount (= price) of the currently selected plan
	 * for reasons of backwards compatibility (amount used to be a field of MethodSelectionPane)
	 * as a String
	 *
	 * @return String: e.g. "500" for 500 Eurocent
	 */
	public String getAmount()
	{
		int amount = m_selectedPlan.getPrice();
		Integer foo = new Integer(amount);
		String bar = foo.toString();
		return bar;
	}

	/**
	 * get Currency of the selected volume Plan
	 *
	 * @return String: currently always "EUR"
	 * @todo: handle this more gracefully, e.g. convert into currency of the user's country
	 */
	public String getCurrency()
	{
		return new String("EUR");
	}

	public void actionPerformed(ActionEvent e)
	{
		if (e.getSource() instanceof JRadioButton)
		{
			JRadioButton clickedButton = (JRadioButton) e.getSource();
			String name = clickedButton.getName();
			if (name.equals("coupon") )
			{
				m_selectedPlan = null;
				setCouponUsed(true);
			}
			else
			{
				m_selectedPlan = m_allPlans.getVolumePlan(name);
				//clear coupon fields if a regular (non-coupon) plan selected
				clearCouponFields();
				setCouponUsed(false);
			}
		}

	    //clearing regular plans if a coupon has been entered
		//is handled by insertUpdate() (method of interface DocumentListener)
	}

	public void insertUpdate(DocumentEvent e)
	{
		m_selectedPlan = null;
		m_couponButton.setSelected(true);
		setCouponUsed(true);

	}

	public void removeUpdate(DocumentEvent e)
	{
		;//do nothing, we only implement to fullfill DocumentListener
	}

	public void changedUpdate(DocumentEvent e)
	{
       ;//do nothing, we only implement to fullfill DocumentListener
	}


	private void addPlan(XMLVolumePlan aPlan)
	{
		m_c.insets = new Insets(0, 5, 0, 5);
		m_c.gridy++;

		String displayName = aPlan.getDisplayName();
		if (displayName == null || displayName.equals("") )
		{
			displayName = aPlan.getName();
		}
		String internalName = aPlan.getName();

		m_c.gridx = 0;
		JRadioButton rb = new JRadioButton(displayName);
		rb.setName(internalName); //we need the unique INTERNAL name to later get the selected plan out of XMLVolumePlans
		rb.addActionListener(this);
		m_rbGroup.add(rb);
		m_rootPanel.add(rb, m_c);

	    //all entries in the row after the radio button need a border of 6 pixels, otherwise the radio button will be too low (swing strangeness...)

		m_c.gridx++;
		JLabel priceLbl = new JLabel(JAPUtil.formatEuroCentValue(aPlan.getPrice()));
		priceLbl.setBorder(BorderFactory.createEmptyBorder(4,0,2,0));
		m_rootPanel.add(priceLbl, m_c);

		m_c.gridx++;
		JLabel planDurationLbl;
		if (aPlan.isDurationLimited() )
		{
			/*
				Timestamp endDate = JAPUtil.getEnddate(aPlan.getDuration(),aPlan.getDurationUnit() );
				String lang = JAPController.getLocale().getLanguage();
				m_rootPanel.add(new JLabel(JAPUtil.formatTimestamp(endDate,false,lang)), m_c);
			*/
			 planDurationLbl = new JLabel(JAPUtil.getDuration(aPlan.getDuration(), aPlan.getDurationUnit()));
		}
		else
		{
			planDurationLbl = new JLabel(JAPMessages.getString(MSG_UNLIMITED));
		}
		planDurationLbl.setBorder(BorderFactory.createEmptyBorder(4,0,2,0));
		m_rootPanel.add(planDurationLbl, m_c);

		m_c.gridx++;
		JLabel planVolumeLbl;
		if (aPlan.isVolumeLimited() )
		{
			planVolumeLbl = new JLabel(Util.formatBytesValueWithUnit(aPlan.getVolumeKbytes()*1000));
		}
		else
		{
			planVolumeLbl = new JLabel(JAPMessages.getString(MSG_UNLIMITED));
		}
		planVolumeLbl.setBorder(BorderFactory.createEmptyBorder(4,0,2,0));
		m_rootPanel.add(planVolumeLbl,m_c);
	}

	private void addCouponField()
	{
		m_c.gridy++;
		m_c.insets = new Insets(10, 5, 0, 5);
		m_c.gridx = 0;
		m_c.gridwidth = 4;

	    //headline and radio button
	    JPanel couponHeaderPanel = new JPanel(); //default flow layout
		m_couponButton = new JRadioButton("");
		m_couponButton.setName("coupon");
		m_couponButton.addActionListener(this);
		m_rbGroup.add(m_couponButton);
		couponHeaderPanel.add(m_couponButton);
		couponHeaderPanel.add(new JLabel(JAPMessages.getString(MSG_ENTER_COUPON)) );
		m_rootPanel.add(couponHeaderPanel,m_c);
		m_c.gridy++;
		m_c.gridx = 0;
		m_c.gridwidth = 4;

	    //text fields
	    JPanel couponPanel = new JPanel(); //default flow layout
	    m_coupon1 = new JapCouponField();
		m_coupon1.getDocument().addDocumentListener(this);
		couponPanel.add(m_coupon1);
		couponPanel.add(new JLabel(" - "));
		m_coupon2 = new JapCouponField();
		m_coupon1.setNextCouponField(m_coupon2);
		m_coupon2.getDocument().addDocumentListener(this);
		couponPanel.add(m_coupon2);
		couponPanel.add(new JLabel(" - "));
		m_coupon3 = new JapCouponField();
		m_coupon2.setNextCouponField(m_coupon3);
		m_coupon3.getDocument().addDocumentListener(this);
		couponPanel.add(m_coupon3);
		couponPanel.add(new JLabel(" - "));
		m_coupon4 = new JapCouponField();
		m_coupon3.setNextCouponField(m_coupon4);
		m_coupon4.getDocument().addDocumentListener(this);
		couponPanel.add(m_coupon4);
		m_rootPanel.add(couponPanel,m_c);

	}

	public CheckError[] checkYesOK()
	{
		CheckError[] errors = super.checkYesOK();
		Vector allErrors = new Vector();

		//can't have both a coupon and a plan selected (should not be possible to happen, but check anyway)
		if (m_couponButton.isSelected() == false && isCouponUsed() )
		{
			allErrors.addElement(new CheckError(JAPMessages.getString(MSG_PLAN_OR_COUPON), LogType.GUI));
		}

	    //if no coupon was entered, a plan needs to be selected
		if ( m_rbGroup.getSelection() == null && !isCouponUsed() )
		{
			allErrors.addElement(new CheckError(JAPMessages.getString(MSG_ERROR_NO_PLAN_CHOSEN), LogType.GUI));
		}

	    //if coupon was entered, check locally (without contacting JPI) if it's even possibly valid
		if (isCouponUsed() && !isCouponComplete() )
		{
			allErrors.addElement(new CheckError(JAPMessages.getString(MSG_COUPON_INCOMPLETE), LogType.GUI));
		}


	    //if coupon was entered,check it for validity immediately
	    if (isCouponUsed() && isCouponComplete() && m_isNewAccount) //validity check for charging old accounts is not implemented, since charging old accounts is disabled
		{
			//go back lots of panes to reach the original jpi selection pane to get the jpi
			DialogContentPane somePreviousPane = m_fetchPlansPane;
			while ( !(somePreviousPane instanceof JpiSelectionPane) ) {
				somePreviousPane = somePreviousPane.getPreviousContentPane();
				//warning: will loop endlessly if no JpiSelectionPane to be found
			}
			JpiSelectionPane jpiPane = (JpiSelectionPane) somePreviousPane;
			PaymentInstanceDBEntry jpi = jpiPane.getSelectedPaymentInstance();

			//go back in the wizard to find the previously created account
			somePreviousPane = m_fetchPlansPane;
			while (! (somePreviousPane instanceof AccountCreationPane))
			{
				somePreviousPane = somePreviousPane.getPreviousContentPane();
				//warning: will loop endlessly if no AccountCreationPane to be found
			}
			AccountCreationPane accountPane = (AccountCreationPane) somePreviousPane;
			PayAccount account = (PayAccount) accountPane.getValue();

			boolean isValid = false;
			try
			{
				BIConnection piConn = new BIConnection(jpi);
				piConn.connect();
				piConn.authenticate(account.getAccountCertificate(),account.getPrivateKey());
				LogHolder.log(LogLevel.DEBUG, LogType.PAY, "Checking coupon code validity in VolumePlanSelectionPane");
				isValid = piConn.checkCouponCode(getEnteredCouponCode() );
				piConn.disconnect();
			}
			catch (Exception e)
			{
				if (!Thread.currentThread().isInterrupted())
				{
					LogHolder.log(LogLevel.EXCEPTION, LogType.NET,"Error while checking coupon validity: ", e);
					Thread.currentThread().interrupt();
				}
			}
			if (!isValid)
			{
				allErrors.addElement(new CheckError(JAPMessages.getString(MSG_INVALID_COUPON), LogType.GUI) );
			}
		}

	    //return errors, if there were any (needs to return null if no errors detected)
		if (allErrors.size() > 0)
		{
			//unfortunately, Vector.toArray() gives us Object[], we need CheckError[], so we have to iterate, then cast and copy single objects
			int nrOfErrors = allErrors.size();
			errors = new CheckError[nrOfErrors];
			for (int i = 0; i < nrOfErrors; i++)
			{
				Object curEntry = allErrors.elementAt(i);
				errors[i] = (CheckError) curEntry;
			}
		} else //explicitly return null, since <an empty Vector>.toArray will return an empty Array
		{
			errors = null;
		}
		return errors;
	}


	public CheckError[] checkUpdate()
	{
		if (!m_hasBeenShown)
		{
			m_hasBeenShown = true;
			showVolumePlans();
		}
		//resetSelection();
		return null;
	}

	public void showVolumePlans()
	{
		//Get fetched volume plans
		WorkerContentPane p = m_fetchPlansPane;
		Object value = p.getValue();
		XMLVolumePlans allPlans = (XMLVolumePlans) value;
		JLabel label;
		m_allPlans = allPlans;

		m_rootPanel.removeAll();
		m_c = new GridBagConstraints();
		m_c.gridx = 0;
		m_c.gridy = 0;
		m_c.weightx = 0;
		m_c.weightx = 0;
		m_c.insets = new Insets(5, 5, 5, 5);
		m_c.anchor = GridBagConstraints.NORTHWEST;
		m_c.fill = GridBagConstraints.NONE;


		m_c.gridx++;
		label = new JLabel(JAPMessages.getString(MSG_PRICE));
		GUIUtils.setFontStyle(label, Font.BOLD);
		m_rootPanel.add(label, m_c);
		m_c.gridx++;
		label = new JLabel(JAPMessages.getString(MSG_VALIDUNTIL));
		GUIUtils.setFontStyle(label, Font.BOLD);
		m_rootPanel.add(label, m_c);
		m_c.gridx++;
		label = new JLabel(JAPMessages.getString(MSG_VOLUME));
		GUIUtils.setFontStyle(label, Font.BOLD);
		m_rootPanel.add(label, m_c);

		//show plans
		m_rbGroup = new ButtonGroup();
		m_c.gridy++;
		for (int i = 0; i < m_allPlans.getNrOfPlans(); i++)
		{
			addPlan(m_allPlans.getVolumePlan(i));
		}

	    //show coupon field
		addCouponField();
	}

	public void resetSelection()
	{
		m_selectedPlan = null;
		clearCouponFields();
	}

	private void clearCouponFields(){
		m_coupon1.setText("");
		m_coupon2.setText("");
		m_coupon3.setText("");
		m_coupon4.setText("");
	}

	private String getCouponString()
	{
		return m_coupon1.getText() + m_coupon2.getText() + m_coupon3.getText() + m_coupon4.getText();
	}
}
