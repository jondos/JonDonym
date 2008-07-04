package anon.infoservice.test;

import anon.infoservice.ImmutableProxyInterface;
import anon.util.IPasswordReader;

/**
 * This class is a dummy implementation and for testing purposes only.
 * @author Wendolsky
 */
public class DummyPasswordReader implements IPasswordReader
{
	private String m_strPassword = "";
	private boolean m_bRead = false;

	/**
	 * Sets the password.
	 * @param a_strPassword a password
	 */
	public void setPassword(String a_strPassword)
	{
		m_strPassword = a_strPassword;
		m_bRead = false;
	}

	/**
	 * Gets the password.
	 * @param a_proxyInterface a proxy interface
	 * @return the password
	 */
	public String readPassword(ImmutableProxyInterface a_proxyInterface)
	{
		m_bRead = true;
		return m_strPassword;
	}

	/**
	 * Gets if the password was read.
	 * @return if the password was read
	 */
	public boolean isRead()
	{
		return m_bRead;
	}

}
