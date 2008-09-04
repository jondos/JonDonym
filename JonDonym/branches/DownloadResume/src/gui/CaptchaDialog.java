/*
 Copyright (c) 2000, The JAP-Team
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
package gui;

import java.awt.Container;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JTextField;

import anon.util.captcha.IImageEncodedCaptcha;
import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.AttributeSet;
import javax.swing.text.PlainDocument;
import gui.dialog.JAPDialog;
import java.awt.Window;

/**
 * This class displays a dialog for solving a captcha.
 *
 * @author Tobias Bayer
 */
public class CaptchaDialog extends JAPDialog implements ActionListener
{
	/** Messages*/
	private static final String MSG_TITLE = CaptchaDialog.class.getName() +
		"_title";
	private static final String MSG_SOLVE = CaptchaDialog.class.getName() +
		"_solve";
	private static final String MSG_OK = CaptchaDialog.class.getName() +
		"_ok";
	private static final String MSG_CANCEL = CaptchaDialog.class.getName() +
		"_cancel";
	private static final String MSG_WRONGCHARNUM = CaptchaDialog.class.getName() +
		"_wrongcharnum";

	private JTextField m_tfSolution;
	private JButton m_btnOk;
	private JButton m_btnCancel;
	private byte[] m_solution;
	private IImageEncodedCaptcha m_captcha;
	private String m_beginsWith;

	public CaptchaDialog(IImageEncodedCaptcha a_captcha, String a_beginsWith, Window a_parent)
	{
		super(a_parent, JAPMessages.getString(MSG_TITLE), true);
		m_captcha = a_captcha;
		m_beginsWith = a_beginsWith;
		Container rootPanel = this.getContentPane();
		GridBagConstraints c = new GridBagConstraints();
		rootPanel.setLayout(new GridBagLayout());
		c.anchor = GridBagConstraints.NORTHWEST;
		c.weightx = 0;
		c.weighty = 0;
		c.gridwidth = 2;
		c.insets = new Insets(5, 5, 5, 5);
		c.fill = GridBagConstraints.NONE;
		c.gridy = 0;
		c.gridx = 0;

		JLabel image = new JLabel(new ImageIcon(a_captcha.getImage()));
		rootPanel.add(image, c);

		c.gridy++;
		JLabel l = new JLabel("<html>" + JAPMessages.getString(MSG_SOLVE) + "</html>");
		rootPanel.add(l, c);

		c.gridy++;
		final IImageEncodedCaptcha captcha = a_captcha;
		m_tfSolution = new JTextField(20)
		{
			protected Document createDefaultModel()
			{
				return (new PlainDocument()
				{
					public void insertString(int a_position, String a_stringToInsert,
											 AttributeSet a_attributes) throws BadLocationException
					{
						if (getLength() + a_stringToInsert.length() <= captcha.getCharacterNumber())
						{
							/* the new text fits in the box */
							boolean invalidCharacters = false;
							int i = 0;
							while ( (i < a_stringToInsert.length()) && (invalidCharacters == false))
							{
								if (captcha.getCharacterSet().indexOf(a_stringToInsert.toUpperCase().
									substring(i, i + 1)) <
									0)
								{
									/* we have found an invalid character */
									invalidCharacters = true;
								}
								i++;
							}
							if (invalidCharacters == false)
							{
								/* only insert strings, which fit in the box and have no invalid characters */
								super.insertString(a_position, a_stringToInsert.toUpperCase(), a_attributes);
							}
						}
					}
				});
			}
		};

		rootPanel.add(m_tfSolution, c);

		c.gridy++;
		c.gridwidth = 1;
		m_btnCancel = new JButton(JAPMessages.getString(MSG_CANCEL));
		m_btnCancel.addActionListener(this);
		rootPanel.add(m_btnCancel, c);

		c.gridx++;
		c.weighty = 1;
		c.weightx = 1;
		m_btnOk = new JButton(JAPMessages.getString(MSG_OK));
		m_btnOk.addActionListener(this);
		rootPanel.add(m_btnOk, c);

		pack();
		setLocationCenteredOnOwner();
		setVisible(true);
	}

	public void actionPerformed(ActionEvent a_e)
	{
		Object source = a_e.getSource();
		if (source == m_btnCancel)
		{
			dispose();
		}
		else if (source == m_btnOk)
		{
			if (m_captcha.getCharacterNumber() == m_tfSolution.getText().length())
			{
			try
			{
				m_solution = m_captcha.solveCaptcha(m_tfSolution.getText().trim(), m_beginsWith.getBytes());
				dispose();
			}
			catch (Exception e)
			{
				LogHolder.log(LogLevel.EXCEPTION, LogType.MISC, "Error solving captcha!");
				}
			}
			else
			{
				JAPDialog.showErrorDialog(this,
										  JAPMessages.getString(MSG_WRONGCHARNUM) + " " + m_captcha.getCharacterNumber()+".",
										  LogType.MISC);
			}
		}
	}

	public byte[] getSolution()
	{
		return m_solution;
	}
}
