package platform.signal;

import java.util.Observable;

import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;

public class SignalHandler extends Observable implements sun.misc.SignalHandler
{
	public void addSignal(String a_signal)
	{
		try
		{
			sun.misc.Signal.handle(new sun.misc.Signal(a_signal), this);
		}
		catch (IllegalArgumentException a_e)
		{
			LogHolder.log(LogLevel.WARNING, LogType.MISC, 
					"Could not register signal " + a_signal + "!");
		}
	}
	
	public void handle(sun.misc.Signal a_sig)
	{
		setChanged();
		notifyObservers(a_sig);
	}
}