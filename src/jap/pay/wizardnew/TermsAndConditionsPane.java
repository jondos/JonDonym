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

import gui.dialog.DialogContentPane;
import java.awt.GridBagConstraints;
import gui.dialog.WorkerContentPane;
import gui.dialog.JAPDialog;
import java.awt.GridBagLayout;
import java.awt.Insets;
import gui.JAPMessages;
import java.awt.Container;
import logging.LogType;
import javax.swing.JCheckBox;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import anon.pay.xml.XMLGenericText;
import gui.dialog.DialogContentPane.IWizardSuitable;
import gui.JapHtmlPane;
import java.awt.Dimension;
import javax.swing.event.HyperlinkListener;
import javax.swing.event.HyperlinkEvent;
import java.net.URL;
import platform.AbstractOS;

/**
 * Shows the terms and conditions as gotten from the JPI.
 * User has to agree to them in order to proceed
 *
 * Part of the wizard for charging an account
 *
 * @author Elmar Schraml
 */
public class TermsAndConditionsPane extends DialogContentPane implements IWizardSuitable, HyperlinkListener
{
	public static final String MSG_HEADING = TermsAndConditionsPane.class.getName() + "_heading";
	private static final String MSG_TERMS = TermsAndConditionsPane.class.getName() + "_terms";
    private static final String MSG_ERROR_HAVE_TO_ACCEPT = TermsAndConditionsPane.class.getName() + "_havetoaccept";
	private static final String MSG_NO_TERMS_FOUND = TermsAndConditionsPane.class.getName() + "_notermsfound";
	private static final String MSG_I_ACCEPT = TermsAndConditionsPane.class.getName() + "_iaccept";

	private WorkerContentPane m_fetchTermsPane;
	private GridBagConstraints m_c = new GridBagConstraints();
	private Container m_rootPanel;
	private JCheckBox m_accepted;
	private JEditorPane m_termsPane;
	private JScrollPane m_scrollingTerms;
	private boolean m_bCheckAccept;
	//private JapHtmlPane m_termsPane;

	public TermsAndConditionsPane(JAPDialog a_parentDialog, WorkerContentPane a_previousContentPane,
		boolean a_bCheckAccept)
	{
		super(a_parentDialog,
			  new Layout(JAPMessages.getString(MSG_HEADING), MESSAGE_TYPE_PLAIN),
			  new Options(OPTION_TYPE_OK_CANCEL, a_previousContentPane));
		setDefaultButtonOperation(ON_CLICK_DISPOSE_DIALOG | ON_YESOK_SHOW_NEXT_CONTENT |
								  ON_NO_SHOW_PREVIOUS_CONTENT);

		m_bCheckAccept = a_bCheckAccept;
		m_fetchTermsPane = a_previousContentPane;
		m_rootPanel = this.getContentPane();
		m_c = new GridBagConstraints();
		m_rootPanel.setLayout(new GridBagLayout());
		m_c.gridx = 0;
		m_c.gridy = 0;
		m_c.weightx = 1.0;
		m_c.weighty = 1.0;
		m_c.insets = new Insets(5, 5, 5, 5);
		m_c.anchor = m_c.NORTHWEST;
		m_c.fill = m_c.BOTH;

		String termsHtml = JAPMessages.getString(MSG_NO_TERMS_FOUND);
		m_termsPane = new JEditorPane("text/html",termsHtml);
		m_termsPane.setEditable(false);
		m_termsPane.addHyperlinkListener(this);
		m_scrollingTerms = new JScrollPane(m_termsPane);
		/**@todo make this dynamic */
		m_scrollingTerms.setPreferredSize(new Dimension(400,200));
		m_rootPanel.add(m_scrollingTerms, m_c);

		String acceptTerms = JAPMessages.getString(MSG_I_ACCEPT);
		m_accepted = new JCheckBox(acceptTerms);
		m_c.weightx = 0.0;
		m_c.weighty = 0.0;
		m_c.gridy++;
		m_c.fill = m_c.BOTH;
		if (m_bCheckAccept)
		{
			m_rootPanel.add(m_accepted, m_c);
		}
		//getButtonCancel().setVisible(false);
	}

	public boolean isTermsAccepted()
	{
		return m_accepted.isSelected();
	}


	private void showTerms()
	{
		//Get fetched terms
		WorkerContentPane p = m_fetchTermsPane;
		Object value = p.getValue();
		String termsHtml;
		if (value == null) //getting terms from the JPI failed
		{
			termsHtml = JAPMessages.getString(MSG_NO_TERMS_FOUND);
		} else
		{
			XMLGenericText theTerms = (XMLGenericText) value;
			termsHtml = theTerms.getText();
		}
		m_termsPane.setText(termsHtml);
		m_scrollingTerms.revalidate();
	}

	public CheckError[] checkYesOK()
	{
		CheckError[] errors = super.checkYesOK();
		if (m_bCheckAccept)
		{
			if ( (errors == null || errors.length == 0) && !isTermsAccepted())
			{
				errors = new CheckError[]
					{
					new CheckError(JAPMessages.getString(MSG_ERROR_HAVE_TO_ACCEPT), LogType.GUI)};
			}
		}
		return errors;

	}

	public void resetSelection(){
		m_accepted.setSelected(false);
	}

    public CheckError[] checkUpdate()
	{
		showTerms();
		resetSelection();
		return null;
	}

	public void hyperlinkUpdate(HyperlinkEvent e)
	{
		if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED )
		{
			URL urlToOpen = e.getURL();
			if (urlToOpen.getProtocol().startsWith("mailto") )
			{
				AbstractOS.getInstance().openEMail(urlToOpen.toString());
			}
			else
			{
				AbstractOS.getInstance().openURL(urlToOpen);
			}
		}
	}

}
