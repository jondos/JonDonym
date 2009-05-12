package jap;

import gui.JAPHyperlinkAdapter;
import gui.TermsAndConditionsOperatorTable;
import gui.TermsAndCondtionsTableController;
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
import anon.client.ITermsAndConditionsContainer;
import anon.infoservice.Database;
import anon.infoservice.ServiceOperator;
import anon.terms.TermsAndConditions;
import anon.util.JAPMessages;

public class JAPConfTC extends AbstractJAPConfModule implements Observer, TermsAndCondtionsTableController
{
	private static final String MSG_TAB_TITLE = JAPConfTC.class.getName() + "_tabTitle";
	private static final String MSG_ERR_REJECT_IMPOSSIBLE = JAPConfTC.class.getName() + "_errRejectImpossible";
	
	private TermsAndConditionsOperatorTable m_tblOperators;
	private JEditorPane m_termsPane;
	private JScrollPane m_scrollingTerms;
	private ITermsAndConditionsContainer m_tcc;
	
	protected JAPConfTC(IJAPConfSavePoint savePoint, ITermsAndConditionsContainer tcc)
	{
		super(null);
		if (tcc == null)
		{
			throw new NullPointerException();
		}
		m_tcc = tcc;
	}
	
	/*protected boolean initObservers()
	{
		if (super.initObservers())
		{
			synchronized(LOCK_OBSERVABLE)
			{
				//m_tcc.getTermsAndConditionsResponseHandler().addObserver(this);
				return true;
			}
		}
		return false;
	}*/
	
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
		
		m_tblOperators = new TermsAndConditionsOperatorTable();
		m_tblOperators.setController(this);
	
		//m_tblOperators.getColumnModel().getColumn(OperatorsTableModel.ACCEPTED_COL).setMinWidth(4);
		//m_tblOperators.getColumnModel().getColumn(OperatorsTableModel.ACCEPTED_COL).setPreferredWidth(4);
		JScrollPane scroll;

		scroll = new JScrollPane(m_tblOperators);
		scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		//scroll.setMinimumSize(new Dimension(100, 100));
		//scroll.setPreferredSize(preferredSize);
		
		root.add(scroll, c);
		
		c.gridy++;
		
		m_termsPane = new JEditorPane("text/html", "");
		m_termsPane.addHyperlinkListener(new JAPHyperlinkAdapter());
		m_termsPane.setEditable(false);
		m_scrollingTerms = new JScrollPane();
		m_scrollingTerms.setViewport(new UpperLeftStartViewport());
		m_scrollingTerms.getViewport().add(m_termsPane);
		/**@todo make this dynamic */
		
		m_scrollingTerms.setPreferredSize(new Dimension(400,200));
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
						if(!accept && !JAPController.getInstance().isTCRejectingPossible(terms))
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
	}

	//public void handleAcceptAction(TermsAndConditions terms, boolean accept) 
	//{
		/*if(!accept && !JAPController.getInstance().isTCRejectingPossible(terms))
		{
			JAPDialog.showErrorDialog(JAPConf.getInstance(), 
			JAPMessages.getString(MSG_ERR_REJECT_IMPOSSIBLE, terms.getOperator().getOrganization()), LogType.MISC);
		}
		else
		{
			terms.setAccepted(accept);
		}
		terms.setRead(true);*/
	//}

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
}
