package platform.signal;

import java.util.Vector;
import java.util.Observable;

public class SignalHandler extends Observable implements sun.misc.SignalHandler
{
	public void addSignal(String a_signal)
	{
		sun.misc.Signal.handle(new sun.misc.Signal(a_signal), this);
	}
	
	public void handle(sun.misc.Signal a_sig)
	{
		setChanged();
		notifyObservers(a_sig);
	}
}