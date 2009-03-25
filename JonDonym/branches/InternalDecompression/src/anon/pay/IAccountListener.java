package anon.pay;

/**
 * Event listener for one account
 * @author Bastian Voigt
 * @version 1.0
 */
public interface IAccountListener
{
	/**
	 * the state of the account changed
	 */
	void accountChanged(PayAccount acc);
}
