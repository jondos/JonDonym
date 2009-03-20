package jap.pay;

import anon.pay.PayAccount;
import anon.util.IReturnRunnable;

interface IReturnAccountRunnable extends IReturnRunnable
{
	public PayAccount getAccount();
}