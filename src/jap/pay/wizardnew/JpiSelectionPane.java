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
import gui.dialog.DialogContentPaneOptions;
import gui.dialog.WorkerContentPane;
import gui.dialog.JAPDialog;
import anon.pay.PaymentInstanceDBEntry;
import anon.util.JAPMessages;

import javax.swing.ButtonGroup;
import java.awt.GridBagConstraints;
import java.awt.Container;
import java.util.Vector;
import logging.LogType;
import javax.swing.JRadioButton;
import java.awt.event.ActionEvent;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.Enumeration;
import java.util.Hashtable;
import anon.infoservice.ListenerInterface;
import java.awt.event.ActionListener;
import gui.dialog.DialogContentPane.IWizardSuitable;

/**
 * shown as the first panel when creating an account
 * shows a list of JPIs, and allows the user to choose one to connect to
 *
 * @author Elmar Schraml
 */
public class JpiSelectionPane extends DialogContentPane implements ActionListener, IWizardSuitable
{
	private static final String MSG_CHOOSEAJPI = JpiSelectionPane.class.getName() + "_chooseajpi";
	private static final String MSG_CHOOSEAJPI_TITLE = JpiSelectionPane.class.getName() + "_titleChooseajpi";
	/*private static final String MSG_JPIS_FOUND = JpiSelectionPane.class.getName() + "_jpis_found";
	private static final String MSG_NAME = JpiSelectionPane.class.getName() + "_name";
	private static final String MSG_ADDRESS = JpiSelectionPane.class.getName() + "_address";*/
	private static final String MSG_HAVE_TO_CHOOSE = JpiSelectionPane.class.getName() + "_havetochoose";

	private WorkerContentPane m_fetchJPIPane;
	private PaymentInstanceDBEntry m_selectedJpi;
	private Vector m_allJpis; //gotten from the previous pane
	private Hashtable m_Jpis; //same as the vector, but with the id as key (so we can select one according to the radio button selected)
	private ButtonGroup m_rbGroup;
	private GridBagConstraints m_c = new GridBagConstraints();
	private Container m_rootPanel;

	public JpiSelectionPane(JAPDialog a_parentDialog, WorkerContentPane a_previousContentPane, String a_jpiId)
	{
		super(a_parentDialog, JAPMessages.getString(MSG_CHOOSEAJPI),
		  new Layout(JAPMessages.getString(MSG_CHOOSEAJPI_TITLE), MESSAGE_TYPE_PLAIN),
		  new DialogContentPaneOptions(OPTION_TYPE_OK_CANCEL, a_previousContentPane));
      	setDefaultButtonOperation(ON_CLICK_DISPOSE_DIALOG | ON_YESOK_SHOW_NEXT_CONTENT |
							  ON_NO_SHOW_PREVIOUS_CONTENT);

	    m_fetchJPIPane = a_previousContentPane;
		 // size > 1: we have several JPIs -> Pane will ask user to select one, so don't set a selectedJpi yet here

	    //do NOT show dummy entries here, leads to strange NullPointerExceptions
		//and is not necessary, unless you expect this Pane to be the biggest Pane of the dialog

		//this.getContentPane().setVisible(false) does NOT work to suppress the dialog, we're using a dummy pane before this one instead

    }


	public void actionPerformed(ActionEvent e)
	{
		if (e.getSource() instanceof JRadioButton)
		{
			JRadioButton clickedButton = (JRadioButton) e.getSource();
			String selectedJpiId = clickedButton.getName();
			PaymentInstanceDBEntry selectedJpi = (PaymentInstanceDBEntry) m_Jpis.get(selectedJpiId);
			m_selectedJpi = selectedJpi;
		}
	}

	private void addJpi(PaymentInstanceDBEntry a_Jpi)
	{

		m_c.insets = new Insets(0, 5, 0, 5);
		m_c.gridy++;

		String curId = a_Jpi.getId();
		String curName = a_Jpi.getName();
		//if jpi listens on more than one host:port interfaces, we will ignore all but the first
		ListenerInterface curInterface = (ListenerInterface) a_Jpi.getListenerInterfaces().nextElement();
        String listenerString = curInterface.getHost() + " : " + curInterface.getPort();

		m_c.gridx = 0;
		JRadioButton rb = new JRadioButton(curName + " , " + listenerString); //the Jpi's name is what is shown to the user
		rb.setName(curId); //but the JRadioButton's name is the ID (= subjectkeyidentifier) of the jpi (since names are not necessarily unique)
		rb.addActionListener(this);
		m_rbGroup.add(rb);
		m_rootPanel.add(rb, m_c);

    }



	public CheckError[] checkYesOK()
	{
		CheckError[] errors = super.checkYesOK();
		if ((errors == null || errors.length == 0) && m_rbGroup.getSelection() == null)
		{
			errors = new CheckError[]{
				new CheckError(JAPMessages.getString(MSG_HAVE_TO_CHOOSE), LogType.GUI)};
		}

		return errors;
	}




	public CheckError[] checkUpdate()
	{
		//set layout
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

		m_allJpis = (Vector) m_fetchJPIPane.getValue();
		showPaymentInstances();
		m_rootPanel.setVisible(true);

		resetSelection();
		return null;
	}

	public void showPaymentInstances()
	{

		//convert Vector to Hashtable, with id as key
		PaymentInstanceDBEntry curJpi;
		String curName;
		m_Jpis = new Hashtable();
		for (Enumeration e = m_allJpis.elements(); e.hasMoreElements(); )
		{
			curJpi = (PaymentInstanceDBEntry) e.nextElement();
			curName = curJpi.getId();
			m_Jpis.put(curName,curJpi);
		}


		m_rootPanel.removeAll();
		m_c = new GridBagConstraints();
		m_c.gridx = 0;
		m_c.gridy = 0;
		m_c.weightx = 0;
		m_c.weightx = 0;
		m_c.insets = new Insets(5, 5, 5, 5);
		m_c.anchor = GridBagConstraints.NORTHWEST;
		m_c.fill = GridBagConstraints.NONE;


		//show plans
		m_c.gridy++;
		for (Enumeration theJpis = m_allJpis.elements(); theJpis.hasMoreElements(); )
		{
			addJpi( (PaymentInstanceDBEntry) theJpis.nextElement() );
		}




	}

	public void resetSelection()
	{
		m_selectedJpi = null;
	}

	public PaymentInstanceDBEntry getSelectedPaymentInstance()
	{
		m_allJpis = (Vector) m_fetchJPIPane.getValue();
		if (m_allJpis == null || m_allJpis.size() < 1)
		{
			m_selectedJpi = null;
		}
		else if (m_allJpis.size() == 1)
		{
			m_selectedJpi = (PaymentInstanceDBEntry) m_allJpis.elementAt(0);
		}
		return m_selectedJpi;
	}


}
