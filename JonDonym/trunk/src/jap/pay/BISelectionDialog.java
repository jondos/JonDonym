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
package jap.pay;

import java.awt.FlowLayout;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import anon.pay.PaymentInstanceDBEntry;
import gui.JAPMessages;
import gui.dialog.JAPDialog;
import java.util.Vector;
import java.util.Enumeration;
import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;
import anon.pay.PayAccountsFile;

/** This dialog fetches all known Payment Instances from the InfoService and lets
 *  the user select one.
 *
 *  @author Tobias Bayer
 */
public class BISelectionDialog extends JAPDialog implements ActionListener, ListSelectionListener
{
	private JList m_biList;
	private JButton m_okButton;
	private JButton m_cancelButton;
	private JLabel m_biHost;
	private JLabel m_biPort;

	private PaymentInstanceDBEntry m_selectedBI;

	public BISelectionDialog(Component a_owner)
	{
		super(a_owner, JAPMessages.getString("biSelectionDialog"), true);
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		jbInit();
		setSize(500, 400);
		setVisible(true);
	}

	private void jbInit()
	{
		JPanel panel1 = new JPanel(new GridBagLayout());
		JPanel bttnPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		GridBagConstraints c = new GridBagConstraints();
		c.anchor = GridBagConstraints.NORTHWEST;
		c.fill = GridBagConstraints.NONE;
		c.insets = new Insets(5, 5, 5, 5);

		//The BI list
		m_biList = new JList();
		m_biList.addListSelectionListener(this);

		c.gridx = 0;
		c.gridy = 0;
		c.gridwidth = 3;
		c.gridheight = 5;
		c.weighty = 1;
		c.fill = GridBagConstraints.BOTH;
		panel1.add(m_biList, c);
		c.weightx = 0;
		c.weighty = 0;
		c.gridwidth = 1;
		c.gridheight = 1;
		c.fill = GridBagConstraints.NONE;

		//The information about the selected BI
		c.gridx = 3;
		panel1.add(new JLabel(JAPMessages.getString("infoAboutBI")), c);

		c.gridy++;
		panel1.add(new JLabel(JAPMessages.getString("biInfoHost")), c);

		m_biHost = new JLabel();
		c.gridx++;
		panel1.add(m_biHost, c);

		c.gridx--;
		c.gridy++;
		panel1.add(new JLabel(JAPMessages.getString("biInfoPort")), c);

		m_biPort = new JLabel();
		c.gridx++;
		panel1.add(m_biPort, c);

		//The Cancel button
		m_cancelButton = new JButton(JAPMessages.getString("bttnCancel"));
		m_cancelButton.addActionListener(this);
		bttnPanel.add(m_cancelButton);

		//The Ok button
		m_okButton = new JButton(JAPMessages.getString("bttnOk"));
		m_okButton.addActionListener(this);
		bttnPanel.add(m_okButton);

		//Add the button panel
		c.gridy = 5;
		c.gridx = 0;
		c.weightx = 1;
		c.gridwidth = 5;
		c.anchor = GridBagConstraints.SOUTHEAST;
		panel1.add(bttnPanel, c);

		getContentPane().add(panel1);

		//Fetch information about available Payment Instances from the InfoService
		DefaultListModel model = new DefaultListModel();
		model.addElement(JAPMessages.getString("loadingBIInfo1"));
		model.addElement(JAPMessages.getString("loadingBIInfo2"));
		m_biList.setModel(model);
		m_biList.setEnabled(false);

		Runnable fillList = new Runnable()
		{
			public void run()
			{
				DefaultListModel listModel = new DefaultListModel();

				try
				{
					Vector paymentInstances = PayAccountsFile.getInstance().getPaymentInstances();
					Enumeration en = paymentInstances.elements();
					while (en.hasMoreElements())
					{
						listModel.addElement(((PaymentInstanceDBEntry) en.nextElement()));
					}
				}
				catch (Exception e)
				{
					LogHolder.log(LogLevel.EXCEPTION, LogType.PAY, e.getMessage());
				}
				m_biList.setEnabled(true);
				m_biList.setModel(listModel);
			}
		};

		Thread t = new Thread(fillList);
		t.setDaemon(true);
		t.start();
	}

	public PaymentInstanceDBEntry getSelectedBI()
	{
		return m_selectedBI;
	}

	public void actionPerformed(ActionEvent a_e)
	{
		if (a_e.getSource() == m_okButton)
		{
			m_selectedBI = (PaymentInstanceDBEntry) m_biList.getSelectedValue();
			dispose();
		}
		else if (a_e.getSource() == m_cancelButton)
		{
			dispose();
		}
	}

	public void valueChanged(ListSelectionEvent e)
	{
		if (e.getSource() == m_biList)
		{
			/*m_biHost.setText( ( (BI) m_biList.getSelectedValue()).getHostName());
			m_biPort.setText(String.valueOf( ( (BI) m_biList.getSelectedValue()).getPortNumber()));*/
		}
	}
}
