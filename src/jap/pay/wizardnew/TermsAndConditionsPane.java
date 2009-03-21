/*
 Copyright (c) 2000-2009, The JAP-Team
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
import jap.JAPHyperlinkAdapter;

import java.awt.GridBagConstraints;

import gui.dialog.DialogContentPaneOptions;
import gui.dialog.WorkerContentPane;
import gui.dialog.JAPDialog;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Container;
import logging.LogType;
import javax.swing.JCheckBox;
import javax.swing.JEditorPane;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;

import anon.pay.xml.XMLGenericText;
import anon.util.JAPMessages;
import gui.dialog.DialogContentPane.IWizardSuitable;
import java.awt.Dimension;

/**
 * Shows the terms and conditions as gotten from the JPI.
 * User has to agree to them in order to proceed
 *
 * Part of the wizard for charging an account
 *
 * @author Elmar Schraml
 */
public class TermsAndConditionsPane extends DialogContentPane implements IWizardSuitable /*, HyperlinkListener */
{
	public static final String MSG_HEADING = TermsAndConditionsPane.class.getName() + "_heading";
    private static final String MSG_ERROR_HAVE_TO_ACCEPT = TermsAndConditionsPane.class.getName() + "_havetoaccept";
	private static final String MSG_NO_TERMS_FOUND = TermsAndConditionsPane.class.getName() + "_notermsfound";
	private static final String MSG_I_ACCEPT = TermsAndConditionsPane.class.getName() + "_iaccept";
	
	private static final String MSG_CANCEL_HEADING = 
		TermsAndConditionsPane.class.getName() + "_cancellation_heading";
	private static final String MSG_CANCEL_ERROR_HAVE_TO_ACCEPT = 
		TermsAndConditionsPane.class.getName() + "_cancellation_havetoaccept";
	private static final String MSG_CANCEL_NO_POLICY_FOUND = 
		TermsAndConditionsPane.class.getName() + "_cancellation_nopolicyfound";
	private static final String MSG_CANCEL_I_ACCEPT = 
		TermsAndConditionsPane.class.getName() + "_cancellation_iaccept";

	private WorkerContentPane m_fetchTermsPane;
	private GridBagConstraints m_c = new GridBagConstraints();
	private Container m_rootPanel;
	private JCheckBox m_accepted;
	private JEditorPane m_termsPane;
	private JScrollPane m_scrollingTerms;
	private IMessages m_messages;

	public TermsAndConditionsPane(JAPDialog a_parentDialog, WorkerContentPane a_previousContentPane,
		boolean a_bShowCheckAccept, IMessages a_messages)
	{
		super(a_parentDialog,
			  new Layout(JAPMessages.getString(a_messages.getHeading()), MESSAGE_TYPE_PLAIN)
		{
			public boolean isCentered()
			{
				return false;
			}
		},new DialogContentPaneOptions(OPTION_TYPE_OK_CANCEL, a_previousContentPane));

		m_messages = a_messages;
		m_fetchTermsPane = a_previousContentPane;
		
		init(a_bShowCheckAccept);
	}
	
	public TermsAndConditionsPane(JAPDialog a_parentDialog, boolean a_bAccepted, IMessages a_messages)
	{
		super(a_parentDialog,
				new Layout(JAPMessages.getString(a_messages.getHeading()), MESSAGE_TYPE_PLAIN)
		{
			public boolean isCentered()
			{
				return false;
			}
		},new DialogContentPaneOptions(OPTION_TYPE_OK_CANCEL));
		m_messages = a_messages;
		
		init(true);
		
		m_accepted.setSelected(a_bAccepted);
	}
	
	public static final class TermsAndConditionsMessages implements IMessages
	{
		public String getHeading()
		{
			return MSG_HEADING;
		}
		public String getErrorHaveToAccept()
		{
			return MSG_ERROR_HAVE_TO_ACCEPT;
		}
		public String getNotFound()
		{
			return MSG_NO_TERMS_FOUND;
		}
		public String getIAccept()
		{
			return MSG_I_ACCEPT;
		}
	}
	
	public static final class CancellationPolicyMessages implements IMessages
	{
		public String getHeading()
		{
			return MSG_CANCEL_HEADING;
		}
		public String getErrorHaveToAccept()
		{
			return MSG_CANCEL_ERROR_HAVE_TO_ACCEPT;
		}
		public String getNotFound()
		{
			return MSG_CANCEL_NO_POLICY_FOUND;
		}
		public String getIAccept()
		{
			return MSG_CANCEL_I_ACCEPT;
		}
	}
	
	public static interface IMessages
	{
		public String getHeading();
		public String getErrorHaveToAccept();
		public String getNotFound();
		public String getIAccept();
	}

	private void init(boolean a_bShowCheckAccept) 
	{
		setDefaultButtonOperation(ON_CLICK_DISPOSE_DIALOG | ON_YESOK_SHOW_NEXT_CONTENT |
				  ON_NO_SHOW_PREVIOUS_CONTENT);
		
		m_rootPanel = this.getContentPane();
		m_c = new GridBagConstraints();
		m_rootPanel.setLayout(new GridBagLayout());
		m_c.gridx = 0;
		m_c.gridy = 0;
		m_c.weightx = 1.0;
		m_c.weighty = 1.0;
		m_c.insets = new Insets(5, 5, 5, 5);
		m_c.anchor = GridBagConstraints.NORTHWEST;
		m_c.fill = GridBagConstraints.BOTH;

		String termsHtml = JAPMessages.getString(m_messages.getNotFound());
		m_termsPane = new JEditorPane("text/html",termsHtml);
		m_termsPane.setEditable(false);
		m_termsPane.addHyperlinkListener(new JAPHyperlinkAdapter());
		m_scrollingTerms = new JScrollPane(m_termsPane);
		m_scrollingTerms.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		/**@todo make this dynamic */
		m_scrollingTerms.setPreferredSize(new Dimension(400,200));
		m_rootPanel.add(m_scrollingTerms, m_c);

		if (a_bShowCheckAccept)
		{
			String acceptTerms = JAPMessages.getString(m_messages.getIAccept());
			m_accepted = new JCheckBox(acceptTerms);
			m_c.weightx = 0.0;
			m_c.weighty = 0.0;
			m_c.gridy++;
			m_rootPanel.add(m_accepted, m_c);
		}
	}
	
	public boolean isTermsAccepted()
	{
		if (m_accepted != null)
		{
			return m_accepted.isSelected();
		}
		return true;
	}
	
	private void showTerms()
	{
		//Get fetched terms
		WorkerContentPane p = m_fetchTermsPane;
		Object value = p.getValue();
		String termsHtml;
		if (value == null) //getting terms from the JPI failed
		{
			termsHtml = JAPMessages.getString(m_messages.getNotFound());
		} 
		else
		{
			XMLGenericText theTerms = (XMLGenericText) value;
			termsHtml = theTerms.getText();
		}
		m_termsPane.setText(termsHtml);
		m_scrollingTerms.revalidate();
	}
	
	public void setText(String a_text)
	{
		m_termsPane.setText(a_text);
		m_scrollingTerms.revalidate();
	}

	public CheckError[] checkYesOK()
	{
		CheckError[] errors = super.checkYesOK();
		
		if ( (errors == null || errors.length == 0) && !isTermsAccepted())
		{
			errors = new CheckError[]
				{
				new CheckError(JAPMessages.getString(m_messages.getErrorHaveToAccept()), LogType.GUI)};
		}
		
		return errors;
	}

	public void resetSelection()
	{
		if (m_accepted != null)
		{
			m_accepted.setSelected(false);
		}
	}

    public CheckError[] checkUpdate()
	{
    	if (m_fetchTermsPane != null)
    	{
    		showTerms();
    	}
    	
		resetSelection();
		return null;
	}

	/*public void hyperlinkUpdate(HyperlinkEvent e)
	{
		System.out.println("click");
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
	}*/

}
