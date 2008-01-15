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
package gui.dialog;

import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.PlainDocument;

import anon.pay.IPaymentListener;
import anon.pay.PayAccount;
import anon.pay.xml.XMLErrorMessage;
import anon.util.captcha.IImageEncodedCaptcha;
import anon.util.captcha.ICaptchaSender;
import gui.JAPMessages;
import logging.LogType;
import anon.infoservice.MixCascade;

/**
 * This class displays a dialog for solving a captcha.
 *
 * @author Tobias Bayer
 */
public class CaptchaContentPane extends DialogContentPane implements
	DialogContentPane.IWizardSuitable, IPaymentListener
{
	/** Messages*/
	private static final String MSG_TITLE = CaptchaContentPane.class.getName() +
		"_title";
	private static final String MSG_SOLVE = CaptchaContentPane.class.getName() +
		"_solve";
	private static final String MSG_WRONGCHARNUM = CaptchaContentPane.class.getName() +
		"_wrongcharnum";
	private static final String MSG_CAPTCHAERROR = CaptchaContentPane.class.getName() +
		"_captchaerror";

	private JTextField m_tfSolution;
	private byte[] m_solution;
	private IImageEncodedCaptcha m_captcha;
	private String m_beginsWith;
	private JLabel m_imageLabel;
	private Object m_syncObject;
	private ICaptchaSender m_captchaSource;

	public CaptchaContentPane(JAPDialog a_parentDialog, DialogContentPane a_previousContentPane)
	{
		super(a_parentDialog, JAPMessages.getString(MSG_SOLVE),
			  new Layout(JAPMessages.getString(MSG_TITLE), MESSAGE_TYPE_PLAIN),
			  new Options(OPTION_TYPE_OK_CANCEL, a_previousContentPane));
		setDefaultButtonOperation(ON_CLICK_DISPOSE_DIALOG | ON_YESOK_SHOW_PREVIOUS_CONTENT |
								  ON_NO_SHOW_PREVIOUS_CONTENT);

		Container rootPanel = this.getContentPane();
		GridBagConstraints c = new GridBagConstraints();
		rootPanel.setLayout(new GridBagLayout());
		c.anchor = GridBagConstraints.CENTER;
		c.weightx = 0;
		c.weighty = 0;
		c.gridwidth = 2;
		c.insets = new Insets(5, 5, 5, 5);
		c.fill = GridBagConstraints.NONE;
		c.gridy = 0;
		c.gridx = 0;

		m_imageLabel = new JLabel();
		m_imageLabel.setPreferredSize(new Dimension(300, 100));
		rootPanel.add(m_imageLabel, c);

		c.gridy++;
		m_tfSolution = new JTextField();
		c.fill = GridBagConstraints.HORIZONTAL;
		c.weighty = 1;
		c.weightx = 1;
		rootPanel.add(m_tfSolution, c);
	}

	public CheckError[] checkNo()
	{
		m_captchaSource.getNewCaptcha();
		synchronized (m_syncObject)
		{
			m_syncObject.notifyAll();
		}

		return null;
	}

	public CheckError[] checkYesOK()
	{
		CheckError[] errors = new CheckError[1];

		if (m_captcha.getCharacterNumber() == m_tfSolution.getText().length())
		{
			try
			{
				m_solution = m_captcha.solveCaptcha(m_tfSolution.getText().trim(), m_beginsWith.getBytes());
				if (m_solution != null)
				{
					m_captchaSource.setCaptchaSolution(m_solution);
					synchronized (m_syncObject)
					{
						m_syncObject.notifyAll();
					}
				}
				else
				{
					errors[0] = new CheckError(JAPMessages.getString(MSG_CAPTCHAERROR), LogType.PAY);
				}
			}
			catch (Exception e)
			{
				errors[0] = new CheckError(null, LogType.PAY, e);
			}
		}
		else
		{
			errors[0] = new CheckError(JAPMessages.getString(MSG_WRONGCHARNUM,
				new Integer(m_captcha.getCharacterNumber())), LogType.PAY);
		}

		if (errors[0] == null)
		{
			return null;
		}

		return errors;
	}

	public CheckError[] checkCancel()
	{
		synchronized (m_syncObject)
		{
			m_syncObject.notifyAll();
		}

		return null;
	}

	private void setCaptcha(IImageEncodedCaptcha a_captcha, String a_beginsWith)
	{
		m_beginsWith = a_beginsWith;
		m_captcha = a_captcha;
		m_imageLabel.setIcon(new ImageIcon(m_captcha.getImage()));
		MyDocument doc = new MyDocument();
		doc.setCaptcha(m_captcha);
		m_tfSolution.setDocument(doc);
	}

	public byte[] getSolution()
	{
		return m_solution;
	}

	public void gotCaptcha(ICaptchaSender a_source, IImageEncodedCaptcha a_captcha)
	{
		getPreviousContentPane().getButtonCancel().setEnabled(true);
		setCaptcha(a_captcha, "<Don");
		m_captchaSource = a_source;
		SwingUtilities.invokeLater(new Runnable()
		{
			public void run()
			{
				updateDialog();
				m_tfSolution.requestFocus();
			}
		});

		m_syncObject = new Object();
		synchronized (m_syncObject)
		{
			try
			{
				m_syncObject.wait();
			}
			catch (InterruptedException e)
			{
				/** @todo Handle exception */
			}
		}
	}

	public boolean isSkippedAsNextContentPane()
	{
		return true;
	}

	public boolean isSkippedAsPreviousContentPane()
	{
		return true;
	}

	public boolean accountCertRequested(MixCascade a_connectedCascade)
	{
		return true;
	}

	public void accountError(XMLErrorMessage msg, boolean a_bIgnore)
	{
	}

	public void accountActivated(PayAccount acc)
	{
	}

	public void accountRemoved(PayAccount acc)
	{
	}

	public void accountAdded(PayAccount acc)
	{
	}

	public void creditChanged(PayAccount acc)
	{
	}

	private class MyDocument extends PlainDocument
	{
		private IImageEncodedCaptcha m_captcha;

		public void setCaptcha(IImageEncodedCaptcha a_captcha)
		{
			m_captcha = a_captcha;
		}

		public void insertString(int a_position, String a_stringToInsert,
								 AttributeSet a_attributes) throws BadLocationException
		{
			if (getLength() + a_stringToInsert.length() <= m_captcha.getCharacterNumber())
			{
				/* the new text fits in the box */
				boolean invalidCharacters = false;
				int i = 0;
				while ( (i < a_stringToInsert.length()) && (invalidCharacters == false))
				{
					if (m_captcha.getCharacterSet().indexOf(a_stringToInsert.toUpperCase().
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

	}
}
