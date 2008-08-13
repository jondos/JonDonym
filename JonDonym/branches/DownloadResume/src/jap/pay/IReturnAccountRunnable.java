package jap.pay;

import gui.dialog.IReturnRunnable;
import gui.dialog.WorkerContentPane;
import anon.pay.PayAccount;

interface IReturnAccountRunnable extends IReturnRunnable
{
	public PayAccount getAccount();
}