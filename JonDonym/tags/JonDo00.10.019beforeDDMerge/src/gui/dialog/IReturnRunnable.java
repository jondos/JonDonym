package gui.dialog;

/**
 * Implement this interface if you want your runnable object to return some kind of value.
 */
public interface IReturnRunnable extends Runnable
{
	public Object getValue();
}