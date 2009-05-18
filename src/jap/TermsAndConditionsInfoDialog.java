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

import gui.TermsAndConditionsDialog;
import gui.TermsAndConditionsDialog.TermsAndConditonsDialogReturnValues;
import gui.dialog.DialogContentPane;
import gui.dialog.JAPDialog;

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
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.ScrollPaneConstants;

import anon.infoservice.ServiceOperator;
import anon.terms.TermsAndConditions;
import anon.util.JAPMessages;

/**
 * This dialog shows up when the user connects to a cascade and needs
 * to confirm terms and conditions of at least one of its operators.
 * The dialog gives an overview of the corresponding operators and 
 * shows the terms when clicking on a specific operator.
 * @author Simon Pecher
 *
 */
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
		
		Container contentPane = getContentPane();
		contentPane.setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		//contentPane.setPreferredSize(new Dimension(450,200)); // use pack(); this method is only available in java >=1.5
		c.gridx = 0;
		c.gridy = 0;
		c.insets = new Insets(5,5,5,5);
		c.gridheight = 1;
		c.gridwidth = GridBagConstraints.REMAINDER;
		c.fill = GridBagConstraints.BOTH;
	
		JTextArea dialogText = new JTextArea(JAPMessages.getString(MSG_DIALOG_TEXT, serviceName));
		dialogText.setText(JAPMessages.getString(MSG_DIALOG_TEXT, serviceName));
		dialogText.setEditable(false);
		dialogText.setBackground(contentPane.getBackground());
		dialogText.setLineWrap(true);
		dialogText.setWrapStyleWord(true);
		dialogText.setSelectionColor(contentPane.getBackground());
		dialogText.setSelectedTextColor(dialogText.getForeground());
		
		contentPane.add(dialogText, c);
		c.gridwidth = GridBagConstraints.REMAINDER;
		operatorTable = new TermsAndConditionsOperatorTable(operators);
		operatorTable.setController(this);
		
		JScrollPane scroll = new JScrollPane(operatorTable);
		scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		scroll.setPreferredSize(new Dimension(400, 120));
		okButtton = new JButton(JAPMessages.getString(DialogContentPane.MSG_OK));
		cancelButton = new JButton(JAPMessages.getString(DialogContentPane.MSG_CANCEL));
		
		okButtton.addActionListener(this);
		cancelButton.addActionListener(this);
		
		JPanel buttonPanel = new JPanel();
		buttonPanel.add(okButtton);
		buttonPanel.add(cancelButton);
		
		c.gridy++;
		c.weightx = 1.0;
		c.weighty = 1.0;
		contentPane.add(scroll, c);
		
		c.gridy++;
		c.weightx = 0.0;
		c.weighty = 0.0;
		contentPane.add(buttonPanel, c);
		okButtton.setEnabled(!operatorTable.areTermsRejected());
		pack();
	}
	
	//This methods checks if the stored terms are accepted.
	//It does not refer to the internal table model.
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

	//show the terms of the selected operator in a new display dialog
	public boolean handleOperatorAction(ServiceOperator operator, boolean accepted) 
	{
		
		TermsAndConditions terms = TermsAndConditions.getTermsAndConditions(operator);
		//terms == null must never happen
		//if(terms != null)
		//{
			TermsAndConditionsDialog dialog = 
				new TermsAndConditionsDialog(JAPController.getInstance().getCurrentView(), accepted, terms);
			dialog.setVisible(true);
			TermsAndConditonsDialogReturnValues dialogResult = dialog.getReturnValues(); 
			
			return dialogResult.isCancelled() ? accepted : dialogResult.isAccepted();
		//}
		
	}

	public void handleSelectLineAction(ServiceOperator operator) {}

	public void actionPerformed(ActionEvent e) 
	{	
		if(e.getSource() == okButtton)
		{
			commitActions();
		}
		dispose();
	}

	//commit the accept/reject actions to the stored terms and conditions
	public void commitActions()
	{
		Vector[] allHandledTerms = new Vector[]
		{
			operatorTable.getTermsAccepted(),
			operatorTable.getTermsRejected()
		};
		TermsAndConditions terms = null;
		boolean accept = false;
		
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
	
	public void handleAcceptAction(ServiceOperator operator, boolean accept) 
	{
		okButtton.setEnabled(!operatorTable.areTermsRejected());
	}

}
