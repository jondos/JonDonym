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
package jap;

import anon.infoservice.ImmutableProxyInterface;
import anon.util.IPasswordReader;
import gui.JAPMessages;
import gui.dialog.DialogContentPane;
import gui.dialog.JAPDialog;
import gui.dialog.PasswordContentPane;

/**
 * This class shows a dialog window and reads a password from user input.
 */
final class JAPFirewallPasswdDlg implements IPasswordReader
{
	public String readPassword(ImmutableProxyInterface a_proxyInterface)
	{
		JAPDialog dialog = new JAPDialog(JAPController.getInstance().getViewWindow(),
										 JAPMessages.getString("passwdDlgTitle"), true);
		dialog.setAlwaysOnTop(true);
		PasswordContentPane panePasswd =
			new PasswordContentPane(dialog, PasswordContentPane.PASSWORD_ENTER,
									JAPMessages.getString("passwdDlgInput") + "<br><br>" +
									JAPMessages.getString("passwdDlgHost") + ": " +
									a_proxyInterface.getHost() + "<br>" +
									JAPMessages.getString("passwdDlgPort") + ": " +
									a_proxyInterface.getPort() + "<br>" +
									JAPMessages.getString("passwdDlgUserID") + ": " +
									a_proxyInterface.getAuthenticationUserID())
		{
			public CheckError[] checkCancel()
			{
				return super.checkCancel();
			}
		};

		panePasswd.updateDialog();
		dialog.pack();
		dialog.setResizable(false);
		dialog.setVisible(true);

		if (panePasswd.getButtonValue() != PasswordContentPane.RETURN_VALUE_OK ||
			panePasswd.getPassword() == null)
		{
			return null;
		}
		else
		{
			return new String(panePasswd.getPassword());
		}
	}
}
