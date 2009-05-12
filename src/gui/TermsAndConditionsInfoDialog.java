package gui;

import jap.JAPConf;
import jap.JAPController;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Vector;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTable;

import logging.LogType;

import anon.infoservice.ServiceOperator;
import anon.terms.TermsAndConditions;
import anon.util.JAPMessages;

import gui.dialog.DialogContentPane;
import gui.dialog.JAPDialog;

public class TermsAndConditionsInfoDialog extends JAPDialog implements TermsAndCondtionsTableController, ActionListener
{
	public static String MSG_DIALOG_TEXT = TermsAndConditionsInfoDialog.class.getName()+"_dialogText";
	public static String MSG_DIALOG_TITLE = TermsAndConditionsInfoDialog.class.getName()+"_dialogTitle";
	
	private TermsAndConditionsOperatorTable operatorTable = null;
	
	private JButton okButtton = null;
	private JButton cancelButton = null;
	
	public TermsAndConditionsInfoDialog(Component parent, Vector operators, String serviceName) 
	{
		super(parent, JAPMessages.getString(MSG_DIALOG_TITLE));
		setDefaultCloseOperation(HIDE_ON_CLOSE);
		
		DialogContentPane dialogContentPane = 
			new DialogContentPane(this, JAPMessages.getString(MSG_DIALOG_TEXT, serviceName));
		Container contentPane = dialogContentPane.getContentPane();
		contentPane.setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		
		c.gridx = 0;
		c.gridy = 0;
		c.insets = new Insets(3,3,3,3);
		c.gridheight = 1;
		c.gridwidth = GridBagConstraints.REMAINDER;
		c.weightx = 1.0;
		c.weighty = 1.0;
		c.fill = GridBagConstraints.BOTH;
		
		operatorTable = new TermsAndConditionsOperatorTable(operators);
		operatorTable.setController(this);
		operatorTable.setPreferredSize(new Dimension(400,300));
		//tableModel = new InfoDialogTableModel(operators);
		//operatorTable.setDefaultRenderer(ServiceOperator.class, new OperatorsCellRenderer());
		//operatorTable.setModel(tableModel);
		
		okButtton = new JButton(JAPMessages.getString(JAPMessages.getString(DialogContentPane.MSG_OK)));
		cancelButton = new JButton(JAPMessages.getString(JAPMessages.getString(DialogContentPane.MSG_CANCEL)));
		
		okButtton.addActionListener(this);
		cancelButton.addActionListener(this);
		
		JPanel buttonPanel = new JPanel();
		buttonPanel.add(okButtton);
		buttonPanel.add(cancelButton);
		
		contentPane.add(operatorTable, c);
		c.gridy++;
		c.weightx = 0.0;
		c.weighty = 0.0;
		contentPane.add(buttonPanel, c);
		dialogContentPane.updateDialog();
		pack();
	}
	
	public boolean areAllAccepted()
	{
		Vector v = operatorTable.getOperators();
		for(int i=0; i < v.size(); i++)
		{
			TermsAndConditions t = TermsAndConditions.getTermsAndConditions((ServiceOperator) v.elementAt(i));
			if( (t == null) || !t.isAccepted()) return false;
		}
		return true;
	}

	public boolean handleOperatorAction(ServiceOperator operator, boolean accepted) 
	{
		TermsAndConditions terms = TermsAndConditions.getTermsAndConditions(operator);
		if(terms != null)
		{
			TermsAndConditionsDialog dlg = 
				new TermsAndConditionsDialog(JAPController.getInstance().getCurrentView(), accepted, terms);
			if(!dlg.hasError())
			{
				dlg.setVisible(true);
				return dlg.getReturnValues().hasAccepted();
			}
		}
		return accepted;
	}

	public void handleSelectLineAction(ServiceOperator operator) 
	{
		//none
	}

	public void actionPerformed(ActionEvent e) 
	{
		if(e.getSource() == okButtton)
		{
			Vector[] allHandledTerms = new Vector[]
      		{
      			operatorTable.getTermsAccepted(),
      			operatorTable.getTermsRejected()
      		};
      		TermsAndConditions terms = null;
      		boolean accept = false;
      		boolean errorDialogShown = false;
      		
      		for(int j=0; j < allHandledTerms.length; j++)
      		{
      			accept = (j==0);
      			if(allHandledTerms[j] == null) continue;
  				for(int i = 0; i < allHandledTerms[j].size(); i++)
  				{
  					terms = (TermsAndConditions) allHandledTerms[j].elementAt(i);
  					if(terms != null) terms.setAccepted(accept);
  				}
      		}
		}
		dispose();
	}
}
