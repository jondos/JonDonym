package anon.util;

/**
 * An object implementing this interface reads passwords
 * from a user interface.
 * @author Bastian Voigt
 */
public interface IMiscPasswordReader
{
	/**
	 * Reads a password from a user interface.
	 * Note: This method should return NULL only when the user pressed cancel. When an
	 * empty password was supplied, an empty but non-null string should be returned!!
	 *
	 * @param message a message that the user interface can display
	 * @return a password
	 */
	public String readPassword(Object message);
}
