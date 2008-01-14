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
package anon.mixminion;

/**@todo Temporary removed - needs to be rewritten.. */
//import gui.GUIUtils;
//import gui.dialog.JAPDialog;
//import gui.dialog.PasswordContentPane;
//import jap.AbstractJAPConfModule;
//import jap.JAPController;
//import jap.JAPModel;
import anon.mixminion.message.Keyring;
import anon.mixminion.message.MixMinionCryptoUtil;
import anon.util.ByteArrayUtil;

/**
 * @author Stefan Roenisch
 *
 */
public class PasswordManager
{

	public PasswordManager()
	{
	}

	/**
	 * returns the password, if no one is set, opens dialog for new
	 * pw, else a enter password dialog
	 * @return
	 */
	public String getPassword()
	{
		/**@todo Temporary removed - needs to be rewritten.. */
		/*
		if (JAPModel.getMixMinionPassword() != null)
		{
			return JAPModel.getMixMinionPassword();
		}
		else
		{

			if (JAPModel.getMixMinionPasswordHash() != null)
			{
				return enterPassword();
			}
			else
			{
				return setNewPassword();
			}
		}
	  */
		/*
		 * if noch ne gesetzt setNewPassword()
		 * else
		 * 	if schonmal eingegeben getActualPassword()
		 * 	else enterPassword()
		 *
		 */
		return null;
	}

	/**
	 * opens a dialog for a new password, saves it and returns it
	 * @return
	 */
	/**@todo Temporary removed - needs to be rewritten.. */
/*
	private String setNewPassword()
	{
		//GUIUtils.getParentWindow(this.getRootPanel()).show();
		String pw = null;
		JAPDialog d = new JAPDialog(JAPController.getInstance().getViewWindow(),
									"password", true);
		PasswordContentPane p;
		p = new PasswordContentPane(d, PasswordContentPane.PASSWORD_NEW, "");
		p.updateDialog();
		d.pack();
		d.setLocationCenteredOnParent();
		d.setVisible(true);
		d.requestFocus();

		if (p.getButtonValue() != PasswordContentPane.RETURN_VALUE_CANCEL &&
			p.getButtonValue() != PasswordContentPane.RETURN_VALUE_CLOSED)
		{
			pw = new String(p.getPassword());
			JAPController.setMixminionPassword(pw);
			byte[] hash = MixMinionCryptoUtil.hash(pw.getBytes());
			JAPController.setMixminionPasswordHash(hash);
		}
		return pw;

	}
	*/
	/**
	 * opens a dialog for enter the existing pw
	 * @return
	 */
	/**@todo Temporary removed - needs to be rewritten.. */
/*
	private String enterPassword()
	{
		String pw = null;
		while (true)
		{

			JAPDialog d = new JAPDialog(JAPController.getInstance().getViewWindow(),
										"password", true);

			PasswordContentPane p = new PasswordContentPane(d,
				PasswordContentPane.PASSWORD_ENTER, "");
			p.updateDialog();
			d.pack();
			d.setLocationCenteredOnParent();
			d.setVisible(true);
			d.toFront();
			d.requestFocus();

			if (p.getButtonValue() != PasswordContentPane.RETURN_VALUE_CANCEL &&
				p.getButtonValue() != PasswordContentPane.RETURN_VALUE_CLOSED)
			{
				pw = new String(p.getPassword());
			}
			if (pw == null)
			{
				break;
			}
			try
			{
				byte[] acthash = MixMinionCryptoUtil.hash(pw.getBytes());
				byte[] pwhash = JAPModel.getMixMinionPasswordHash();
				if (ByteArrayUtil.equal(acthash, pwhash))
				{
					JAPController.setMixminionPassword(pw);
					return pw;
				}
				else
				{
					continue;
				}

			}
			catch (Exception ex)
			{
				pw = null;
				continue;
			}
			//break ;
		}
		return null;

	}
*/
	/**
	 * opens a change pw dialog
	 * @return
	 */
	/**@todo Temporary removed - needs to be rewritten.. */
	public boolean changePassword()
	{
		return false;
		/*
		//GUIUtils.getParentWindow(this.getRootPanel()).show();
		String pwold = null;
		String pwnew = null;
		//probably there is no password set-->set new
		if (JAPModel.getMixMinionPasswordHash() == null)
		{
			setNewPassword();
			return true;
		}
		else
		{
			//there is already a password set
			while (true)
			{
				JAPDialog d = new JAPDialog(JAPController.getInstance().getViewWindow(),
											"password", true);
				PasswordContentPane p = new PasswordContentPane(d,
					PasswordContentPane.PASSWORD_CHANGE, "");
				p.updateDialog();
				d.pack();
				d.setLocationCenteredOnParent();
				d.setVisible(true);
				d.requestFocus();

				if (p.getButtonValue() != PasswordContentPane.RETURN_VALUE_CANCEL &&
					p.getButtonValue() != PasswordContentPane.RETURN_VALUE_CLOSED)
				{
					pwold = new String(p.getOldPassword());
					pwnew = new String(p.getPassword());
				}
				if (pwnew == null)
				{
					break;
				}
				try
				{
					byte[] acthash = MixMinionCryptoUtil.hash(pwold.getBytes());
					byte[] pwhash = JAPModel.getMixMinionPasswordHash();
					if (ByteArrayUtil.equal(acthash, pwhash))
					{
						//change Keyring pw
						Keyring k = new Keyring(pwold);
						k.changeKeyringPW(pwnew);
						//setController values
						JAPController.setMixminionPassword(pwnew);
						JAPController.setMixminionPasswordHash(MixMinionCryptoUtil.hash(pwnew.getBytes()));
						return true;
					}
					else
					{
						continue;
					}

				}
				catch (Exception ex)
				{
					pwold = null;
					pwnew = null;
					continue;
				}

			}
		}

		return false;*/
	}
}
