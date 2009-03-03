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
import anon.pay.xml.XMLGenericText;
import anon.util.JAPMessages;
import gui.dialog.DialogContentPane.IWizardSuitable;
import java.awt.Dimension;
import javax.swing.event.HyperlinkEvent;
import java.net.URL;
import platform.AbstractOS;
import javax.swing.event.HyperlinkListener;

/**
 * Shows the cancellation policy as gotten from the JPI.
 * User has to agree with it in order to proceed
 *
 * Very similar to TermsAndConditionsPane, but its own class due to different content size and layout
 *
 * Part of the wizard for charging an account
 *
 * @author Elmar Schraml
 */
public class CancellationPolicyPane extends DialogContentPane implements IWizardSuitable /*, HyperlinkListener */
{
	private static final String MSG_HEADING = CancellationPolicyPane.class.getName() + "_heading";
	private static final String MSG_ERROR_HAVE_TO_ACCEPT = CancellationPolicyPane.class.getName() + "_havetoaccept";
	private static final String MSG_NO_POLICY_FOUND = CancellationPolicyPane.class.getName() + "_nopolicyfound";
	private static final String MSG_I_ACCEPT = CancellationPolicyPane.class.getName() + "_iaccept";

	private WorkerContentPane m_fetchPolicyPane;
	private GridBagConstraints m_c = new GridBagConstraints();
	private Container m_rootPanel;
	private JCheckBox m_accepted;
	private JEditorPane m_policyPane;
	private JScrollPane m_scrollingPolicy;

	public CancellationPolicyPane(JAPDialog a_parentDialog, WorkerContentPane a_previousContentPane)
	{
		super(a_parentDialog,
			  new Layout(JAPMessages.getString(MSG_HEADING), MESSAGE_TYPE_PLAIN),
			  new DialogContentPaneOptions(OPTION_TYPE_OK_CANCEL, a_previousContentPane));
		setDefaultButtonOperation(ON_CLICK_DISPOSE_DIALOG | ON_YESOK_SHOW_NEXT_CONTENT |
								  ON_NO_SHOW_PREVIOUS_CONTENT);
		m_fetchPolicyPane = a_previousContentPane;
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

	    String policyHtml = JAPMessages.getString(MSG_NO_POLICY_FOUND);
		m_policyPane = new JEditorPane("text/html", policyHtml);
		m_policyPane.setEditable(false);
		m_policyPane.addHyperlinkListener(new JAPHyperlinkAdapter());
		m_scrollingPolicy = new JScrollPane(m_policyPane);
		/**@todo make this dynamic */
		m_scrollingPolicy.setPreferredSize(new Dimension(200,200));
		m_rootPanel.add(m_scrollingPolicy, m_c);

		m_c.gridy++;
		m_c.weightx = 0.0;
		m_c.weighty = 0.0;
		String acceptpolicy = JAPMessages.getString(MSG_I_ACCEPT);
		m_accepted = new JCheckBox(acceptpolicy);
		m_rootPanel.add(m_accepted, m_c);

	}

	public boolean isPolicyAccepted()
	{
		return m_accepted.isSelected();
	}


	private void showPolicy()
	{

		//Get fetched policy
		WorkerContentPane p = m_fetchPolicyPane;
		Object value = p.getValue();
		String policyHtml;
		if (value == null) //getting policy from the JPI failed
		{
			policyHtml = JAPMessages.getString(MSG_NO_POLICY_FOUND);
		} else
		{
			XMLGenericText thePolicy = (XMLGenericText) value;
			policyHtml = thePolicy.getText();
		}
		m_policyPane.setText(policyHtml);

	}

	public CheckError[] checkYesOK()
	{
		CheckError[] errors = super.checkYesOK();
		if ((errors == null || errors.length == 0) && !isPolicyAccepted() )
		{
			errors = new CheckError[]{
				new CheckError(JAPMessages.getString(MSG_ERROR_HAVE_TO_ACCEPT), LogType.GUI)};
		}

		return errors;

	}

	public void resetSelection(){
		m_accepted.setSelected(false);
	}

	public CheckError[] checkUpdate()
	{
		showPolicy();
		resetSelection();
		return null;
	}

	/*public void hyperlinkUpdate(HyperlinkEvent e)
	{
		if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED)
		{
			URL urlToOpen = e.getURL();
			if (urlToOpen.getProtocol().startsWith("mailto"))
			{
				AbstractOS.getInstance().openEMail(urlToOpen.toString());
			}
			else if (urlToOpen.getProtocol().startsWith("http"))
			{
				AbstractOS.getInstance().openURL(urlToOpen);
			}
		}
	}*/


}
