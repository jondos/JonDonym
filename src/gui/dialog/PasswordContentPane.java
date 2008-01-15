/*
 Copyright (c) 2000-2006, The JAP-Team
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

import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import javax.swing.JLabel;
import javax.swing.JPasswordField;
import javax.swing.JPopupMenu;
import javax.swing.JMenuItem;

import gui.JAPMessages;
import logging.LogType;
import anon.util.IMiscPasswordReader;
import anon.util.Util;
import gui.GUIUtils;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.Clipboard;
import java.awt.Toolkit;


public class PasswordContentPane extends DialogContentPane implements IMiscPasswordReader,
	DialogContentPane.IWizardSuitable
{
	/** Enter a new password and a confirmation. */
	public static final int PASSWORD_NEW = 1;
	/** Enter a single password */
	public static final int PASSWORD_ENTER = 2;
	/** Replace an old password by a new one. Enter the old password, the new password and a confirmation. */
	public static final int PASSWORD_CHANGE = 3;
	public static final int NO_MINIMUM_LENGTH = 0;

	public static final String MSG_ENTER_LBL = PasswordContentPane.class.getName() + "_enterPasswordLabel";
	private static final int FIELD_LENGTH = 15;
	private static final String MSG_TOO_SHORT = PasswordContentPane.class.getName() + "_tooShort";
	private static final String MSG_CAPS_LOCK_PRESSED =
		PasswordContentPane.class.getName() + "_pressedCapsLock";

	private static final String MSG_WRONG_PASSWORD = PasswordContentPane.class.getName() + "_wrongPassword";
	private static final String MSG_ENTER_PASSWORD_TITLE = PasswordContentPane.class.getName() + "_title";
	private static final String MSG_CONFIRM_LBL = PasswordContentPane.class.getName() + "_confirmPasswordLabel";
	private static final String MSG_ENTER_OLD_LBL = PasswordContentPane.class.getName() + "_enterOldPasswordLabel";

	private static final String MSG_ENTER_NEW_LBL = PasswordContentPane.class.getName() + "_enterNewPasswordLabel";
	private static final String MSG_PASSWORDS_DONT_MATCH =
		PasswordContentPane.class.getName() + "_passwordsDontMatch";
	private static final String MSG_INSERT_FROM_CLIP =
		PasswordContentPane.class.getName() + "_insertFromClipboard";

	private JPasswordField m_textOldPasswd, m_textNewPasswd, m_textConfirmPasswd;
	private char[] m_passwd = null;
	private char[] m_oldPasswd = null;
	private int m_type;
	private int m_minLength;
	private JLabel m_lblNew1;
	private JLabel m_lblNew2;
	private JLabel m_lblOld;
	private JPopupMenu m_popup = new JPopupMenu();
	private JPasswordField m_currentPopup;

	public PasswordContentPane(JAPDialog a_parentDialog, int a_type, String a_strMessage, int a_minLength)
	{
		this(a_parentDialog, null, a_type, a_strMessage, a_minLength);
	}

	public PasswordContentPane(JAPDialog a_parentDialog, int a_type, String a_strMessage)
	{
		this(a_parentDialog, null, a_type, a_strMessage, NO_MINIMUM_LENGTH);
	}

	public PasswordContentPane(JAPDialog a_parentDialog, DialogContentPane a_previousContentPane,
							   int a_type, String a_strMessage)
	{
		this(a_parentDialog, a_previousContentPane, a_type, a_strMessage, NO_MINIMUM_LENGTH);
	}

	public PasswordContentPane(JAPDialog a_parentDialog, DialogContentPane a_previousContentPane,
							   int a_type, String a_strMessage, int a_minLength)
	{
		super(a_parentDialog, (a_strMessage != null) ? a_strMessage : "",
			  new Layout(JAPMessages.getString(MSG_ENTER_PASSWORD_TITLE), MESSAGE_TYPE_QUESTION),
			  new Options(OPTION_TYPE_OK_CANCEL, a_previousContentPane));
		setDefaultButtonOperation(BUTTON_OPERATION_WIZARD);

		if (a_type < PASSWORD_NEW || a_type > PASSWORD_CHANGE)
		{
			throw new IllegalArgumentException("Unknown type!");
		}
		m_type = a_type;
		if (a_minLength < NO_MINIMUM_LENGTH)
		{
			a_minLength = NO_MINIMUM_LENGTH;
		}
		m_minLength = a_minLength;

		GridBagLayout layout = new GridBagLayout();
		GridBagConstraints c = new GridBagConstraints();
		JMenuItem itemInsertPassword = new JMenuItem(JAPMessages.getString(MSG_INSERT_FROM_CLIP));
		MouseAdapter popupListener = new MouseAdapter()
		{
			public void mouseClicked(MouseEvent a_event)
			{
				if (GUIUtils.isMouseButton(a_event, MouseEvent.BUTTON2_MASK) ||
					GUIUtils.isMouseButton(a_event, MouseEvent.BUTTON3_MASK))
				{
					m_currentPopup = (JPasswordField)a_event.getComponent();
					m_popup.show(a_event.getComponent(), a_event.getX(), a_event.getY());
				}
			}
		};


		getContentPane().setLayout(layout);
		c.anchor = GridBagConstraints.WEST;
		c.insets = new Insets(10, 10, 10, 10);
		c.gridx = 0;
		c.gridy = 0;

		m_popup = new JPopupMenu();
		itemInsertPassword.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent a_event)
			{
				Clipboard clip = GUIUtils.getSystemClipboard();
				Transferable data = clip.getContents(this);
				if (data != null && data.isDataFlavorSupported(DataFlavor.stringFlavor))
				{
					try
					{
						m_currentPopup.setText( (String) data.getTransferData(DataFlavor.stringFlavor));
					}
					catch (Exception a_e)
					{
						// ignore it
					}
				}
			}
		});
		m_popup.add(itemInsertPassword);


		if (a_type == PASSWORD_CHANGE)
		{
			m_lblOld = new JLabel(getOldPasswordLabel());
			layout.setConstraints(m_lblOld, c);
			getContentPane().add(m_lblOld);
			m_textOldPasswd = new JPasswordField(FIELD_LENGTH);
			m_textOldPasswd.setEchoChar('*');
			m_textOldPasswd.addMouseListener(popupListener);
			c.gridx = 1;
			c.weightx = 1;
			c.fill = GridBagConstraints.HORIZONTAL;
			layout.setConstraints(m_textOldPasswd, c);
			getContentPane().add(m_textOldPasswd);
		}
		if (a_type == PASSWORD_CHANGE || a_type == PASSWORD_NEW)
		{
			m_lblNew1 = new JLabel(getNewPasswordLabel());
			c.gridx = 0;
			c.gridy++;
			c.weightx = 0;
			c.fill = GridBagConstraints.NONE;
			getContentPane().add(m_lblNew1, c);
			m_textNewPasswd = new JPasswordField(FIELD_LENGTH);
			m_textNewPasswd.setEchoChar('*');
			m_textNewPasswd.addMouseListener(popupListener);
			c.fill = GridBagConstraints.HORIZONTAL;
			c.gridx = 1;
			c.weightx = 1;
			layout.setConstraints(m_textNewPasswd, c);
			getContentPane().add(m_textNewPasswd);
		}
		if (a_type == PASSWORD_ENTER)
		{
			m_lblNew2 = new JLabel(JAPMessages.getString(MSG_ENTER_LBL));
		}
		else
		{
			m_lblNew2 = new JLabel(JAPMessages.getString(MSG_CONFIRM_LBL));
		}
		c.fill = GridBagConstraints.NONE;
		c.gridx = 0;
		c.gridy++;
		c.weightx = 0;
		getContentPane().add(m_lblNew2, c);
		m_textConfirmPasswd = new JPasswordField(FIELD_LENGTH);
		m_textConfirmPasswd.setEchoChar('*');
		m_textConfirmPasswd.addMouseListener(popupListener);

		c.gridx = 1;
		c.weightx = 1;
		c.fill = GridBagConstraints.HORIZONTAL;
		layout.setConstraints(m_textConfirmPasswd, c);
		getContentPane().add(m_textConfirmPasswd);

		CapsLockAdapter keyAdapter = new CapsLockAdapter();
		m_textConfirmPasswd.addKeyListener(keyAdapter);
		if (m_textNewPasswd != null)
		{
			m_textNewPasswd.addKeyListener(keyAdapter);
		}
		if (m_textOldPasswd != null)
		{
			m_textOldPasswd.addKeyListener(keyAdapter);
		}

		addComponentListener(new SetFocusComponentAdapter());
	}

	/**
	 * Updates the content pane and shows the dialog. You will have to define the size of the
	 * dialog before, for example by a pack() operation. getValue() will return RETURN_VALUE_CLOSED,
	 * RETURN_VALUE_CANCEL or RETURN_VALUE_OK after this call.
	 * @param a_message a message (optional); if given, a_message.toString() will be shown to the user
	 * @return the password entered by the user or null if the user canceled
	 */
	public String readPassword(Object a_message)
	{
		CheckError[] errors;

		errors = updateDialog();
		if (errors != null && errors.length > 0)
		{
			return null;
		}
		setButtonValue(RETURN_VALUE_CLOSED);
		showDialog();

		if (a_message != null)
		{
			printStatusMessage(a_message.toString());
		}

		if (getButtonValue() != RETURN_VALUE_OK || getPassword() == null)
		{
			return null;
		}
		return new String(getPassword());
	}

	/**
	 * Returns the password that the user has entered.
	 * @return null if getButtonValue() is not OK; otherwise, the password that the user has entered
	 */
	public char[] getPassword()
	{
		if (getButtonValue() != RETURN_VALUE_OK)
		{
			return null;
		}

		if (m_passwd == null)
		{
			return new char[]{0};
		}

		return m_passwd;
	}

	/**
	 * Returns false in order to set the focus on the first available password field.
	 * @return false
	 */
	public boolean isAutomaticFocusSettingEnabled()
	{
		return false;
	}

	public String getNewPasswordLabel()
	{
		return JAPMessages.getString(MSG_ENTER_NEW_LBL);
	}

	public String getOldPasswordLabel()
	{
		return JAPMessages.getString(MSG_ENTER_OLD_LBL);
	}

	/**
	 * Needed if an old password is changed. This method returns the old password.
	 * @return null if uninitialised; otherwise, the old password
	 */
	public char[] getOldPassword()
	{
		if (!hasValidValue())
		{
			return null;
		}
		if (m_oldPasswd == null)
		{
			return new char[]{0};
		}

		return m_oldPasswd;
	}

	/**
	 * Implement this method for the types PASSWORD_ENTER or PASSWORD_CHANGE. It returns the password
	 * that the entered password is compared with. The dialog will only continue if th user enters the
	 * right password or if he closes the dialog.
	 * @return the password that the entered password is compared with; if it returns null, no comparation
	 * is done and the dialog continues whatever the user entered
	 */
	public char[] getComparedPassword()
	{
		return null;
	}

	/**
	 * Saves the user input.
	 * @return CheckError[]
	 */
	public CheckError[] checkYesOK()
	{
		CheckError[] errors = new CheckError[0];

		if (m_type == PASSWORD_NEW || m_type == PASSWORD_CHANGE)
		{
			if (m_minLength > NO_MINIMUM_LENGTH &&
				(m_textNewPasswd.getPassword() == null ||
				 m_textNewPasswd.getPassword().length < m_minLength))
			{
				errors = new CheckError[]{
					new CheckError(JAPMessages.getString(MSG_TOO_SHORT, new Integer(m_minLength)),
					LogType.GUI)
				{
					public void doErrorAction()
					{
						m_lblNew1.setForeground(Color.red);
					}

					public void undoErrorAction()
					{
						m_lblNew1.setForeground(new JLabel().getForeground());
					}
				}};
			}

			if (!Util.arraysEqual(m_textConfirmPasswd.getPassword(), m_textNewPasswd.getPassword()))
			{
				if (errors.length == 1)
				{
					errors = new CheckError[]{null, errors[0]};
				}
				else
				{
					errors = new CheckError[1];
				}

				errors[0] = new CheckError(JAPMessages.getString(MSG_PASSWORDS_DONT_MATCH), LogType.GUI)
				{
					public void doErrorAction()
					{
						m_lblNew1.setForeground(Color.red);
						m_lblNew2.setForeground(Color.red);
					}

					public void undoErrorAction()
					{
						m_lblNew1.setForeground(new JLabel().getForeground());
						m_lblNew2.setForeground(new JLabel().getForeground());
					}
				};
			}
			else
			{
				m_passwd = m_textNewPasswd.getPassword();
			}
		}
		else if (m_type == PASSWORD_ENTER)
		{
			m_passwd = m_textConfirmPasswd.getPassword();
		}

		if (m_type == PASSWORD_CHANGE)
		{
			if (getComparedPassword() != null &&
				!Util.arraysEqual(getComparedPassword(), m_textOldPasswd.getPassword()))
			{
				if (errors.length == 1)
				{
					errors = new CheckError[]{null, errors[0]};
				}
				else if (errors.length == 2)
				{
					errors = new CheckError[]{null, errors[0], errors[1]};
				}
				else
				{
					errors = new CheckError[1];
				}
				{
					errors[0] = new CheckError(JAPMessages.getString(MSG_WRONG_PASSWORD), LogType.GUI)
					{
						public void doErrorAction()
						{
							m_lblOld.setForeground(Color.red);
						}

						public void undoErrorAction()
						{
							m_lblOld.setForeground(new JLabel().getForeground());
						}
					};
				}
			}
			else
			{
				m_oldPasswd = m_textOldPasswd.getPassword();
			}
		}

		return errors;
	}

	/**
	 * Shows a warning when the caps lock key is pressed while entering passwords.
	 * This does only work for JDKs >= 1.3
	 */
	private class CapsLockAdapter extends KeyAdapter
	{
		private int m_messageID = 0;

		public void keyPressed(KeyEvent a_event)
		{
			boolean isCapsLockPressed = false;
			try
			{
				isCapsLockPressed = ((Boolean)Toolkit.class.getMethod("getLockingKeyState", new Class[]
					{int.class}).invoke(getContentPane().getToolkit(),
										new Object[]{new Integer(KeyEvent.VK_CAPS_LOCK)})).booleanValue();
			}
			catch (Exception ex)
			{
			}


			if (isCapsLockPressed)
			{
				m_messageID =
					printErrorStatusMessage(JAPMessages.getString(MSG_CAPS_LOCK_PRESSED), LogType.GUI);
			}
			else
			{
				clearStatusMessage(m_messageID);
			}
		}
	}

	private class SetFocusComponentAdapter extends ComponentAdapter
	{
		public void componentShown(ComponentEvent a_event)
		{
			if (m_type == PASSWORD_CHANGE)
			{
				m_textOldPasswd.requestFocus();
			}
			else if (m_type == PASSWORD_NEW)
			{
				m_textNewPasswd.requestFocus();
			}
			else
			{
				m_textConfirmPasswd.requestDefaultFocus();
			}
		}
	}
}
