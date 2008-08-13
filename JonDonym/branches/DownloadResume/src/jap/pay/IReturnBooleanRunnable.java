package jap.pay;

import gui.dialog.IReturnRunnable;
import gui.dialog.WorkerContentPane;

interface IReturnBooleanRunnable extends IReturnRunnable
{
	public boolean isTrue();
}