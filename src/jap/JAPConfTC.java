/*
Copyright (c) 2008 The JAP-Team, JonDos GmbH

All rights reserved.

Redistribution and use in source and binary forms, with or without modification, 
are permitted provided that the following conditions are met:

    * Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
    * Redistributions in binary form must reproduce the above copyright notice,
       this list of conditions and the following disclaimer in the documentation and/or
       other materials provided with the distribution.
    * Neither the name of the University of Technology Dresden, Germany, nor the name of
       the JonDos GmbH, nor the names of their contributors may be used to endorse or
       promote products derived from this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
"AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE REGENTS OR
CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package jap;

import gui.JAPHyperlinkAdapter;
import gui.UpperLeftStartViewport;
import gui.dialog.JAPDialog;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.Observable;
import java.util.Observer;
import java.util.Vector;

import javax.swing.JEditorPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;

import logging.LogType;
import anon.infoservice.Database;
import anon.infoservice.ServiceOperator;
import anon.terms.TermsAndConditions;
import anon.terms.TermsAndConditionsResponseHandler;
import anon.util.JAPMessages;

public class JAPConfTC extends AbstractJAPConfModule implements Observer, TermsAndCondtionsTableController
{
	private static final String MSG_TAB_TITLE = JAPConfTC.class.getName() + "_tabTitle";
	private static final String MSG_ERR_REJECT_IMPOSSIBLE = JAPConfTC.class.getName() + "_errRejectImpossible";
	
	private TermsAndConditionsOperatorTable m_tblOperators;
	private JEditorPane m_termsPane;
	private JScrollPane m_scrollingTerms;
	
	
	protected JAPConfTC(IJAPConfSavePoint savePoint)
	{
		super(null);
	}
	
	protected boolean initObservers()
	{
		if (super.initObservers())
		{
			synchronized(LOCK_OBSERVABLE)
			{
				TermsAndConditionsResponseHandler.get().addObserver(this);
				return true;
			}
		}
		return false;
	}
	
	public String getTabTitle() 
	{
		return JAPMessages.getString(MSG_TAB_TITLE);
	}
	
	public void recreateRootPanel() 
	{
		JPanel root = getRootPanel();
		root.setLayout(new GridBagLayout());
		
		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.HORIZONTAL;
		c.weightx = 1;
		c.weighty = 0.2;
		c.gridx = 0;
		c.gridy = 0;
		c.anchor = GridBagConstraints.NORTHWEST;
		c.fill = GridBagConstraints.BOTH;
		m_tblOperators = new TermsAndConditionsOperatorTable();
		m_tblOperators.setController(this);
	
		JScrollPane scroll;

		scroll = new JScrollPane(m_tblOperators);
		scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		//scroll.setPreferredSize(new Dimension(450, 60));
		scroll.setPreferredSize(m_tblOperators.getPreferredSize());
		root.add(scroll, c);
		
		c.gridy++;
		c.weighty = 0.8;
		m_termsPane = new JEditorPane("text/html", "");
		m_termsPane.addHyperlinkListener(new JAPHyperlinkAdapter());
		m_termsPane.setEditable(false);
		m_scrollingTerms = new JScrollPane();
		m_scrollingTerms.setViewport(new UpperLeftStartViewport());
		m_scrollingTerms.getViewport().add(m_termsPane);
		/**@todo make this dynamic */
		
		//m_scrollingTerms.setPreferredSize(new Dimension(400,200));
		root.add(m_scrollingTerms, c);
		
		root.validate();
	}

	public String getHelpContext() 
	{
		return "services_tc";
	}
	
	protected void onRootPanelShown()
	{
		m_tblOperators.setOperators(Database.getInstance(ServiceOperator.class).getEntryList());
	}
	
	protected boolean onOkPressed()
	{
		Vector[] allHandledTerms = new Vector[]
		{
				m_tblOperators.getTermsAccepted(),
				m_tblOperators.getTermsRejected()
		};
		TermsAndConditions terms = null;
		boolean accept = false;
		boolean errorDialogShown = false;
		
		for(int j=0; j < allHandledTerms.length; j++)
		{
			accept = (j==0);
			if(allHandledTerms[j] != null)
			{
				for(int i = 0; i < allHandledTerms[j].size(); i++)
				{
					terms = (TermsAndConditions) allHandledTerms[j].elementAt(i);
					if(terms != null)
					{
						if(!accept && !JAPController.getInstance().isOperatorOfConnectedMix(terms.getOperator()))
						{
							if(!errorDialogShown)
							{
								JAPDialog.showErrorDialog(JAPConf.getInstance(), 
										JAPMessages.getString(MSG_ERR_REJECT_IMPOSSIBLE, terms.getOperator().getOrganization()), LogType.MISC);
								errorDialogShown = true;
							}
						}
						else
						{
							terms.setAccepted(accept);
						}
					}
				}
			}
		}
		m_tblOperators.setOperators(Database.getInstance(ServiceOperator.class).getEntryList());
		return true;
	}
	
	protected void onCancelPressed()
	{
		m_tblOperators.setOperators(Database.getInstance(ServiceOperator.class).getEntryList());
	}
	
	public void update(Observable o, Object arg) 
	{
		onUpdateValues();
		getRootPanel().revalidate();
	}

	protected void onUpdateValues()
	{
		// is this update really needed?
		m_tblOperators.setOperators(Database.getInstance(ServiceOperator.class).getEntryList());
	}

	public boolean handleOperatorAction(ServiceOperator operator, boolean accepted) 
	{
		return accepted;
	}

	public void handleSelectLineAction(ServiceOperator operator) 
	{
		TermsAndConditions tc = TermsAndConditions.getTermsAndConditions(operator);
		if(tc == null)
		{
			return;
		}
		String tcHtmlText = tc.getHTMLText(JAPMessages.getLocale());
		m_termsPane.setText(tcHtmlText);
	}

	public void handleAcceptAction(ServiceOperator operator, boolean accept) 
	{
		if(!accept && !JAPController.getInstance().isOperatorOfConnectedMix(operator) )
		{
				JAPDialog.showErrorDialog(JAPConf.getInstance(), 
						JAPMessages.getString(MSG_ERR_REJECT_IMPOSSIBLE, operator.getOrganization()), LogType.MISC);
				throw new IllegalStateException(JAPMessages.getString(MSG_ERR_REJECT_IMPOSSIBLE, operator.getOrganization()));
		}
	}
}
