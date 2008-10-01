package jap.pay;

import gui.dialog.IReturnRunnable;
import anon.pay.PayAccount;

interface IReturnAccountRunnable extends IReturnRunnable
{
	public PayAccount getAccount();
}