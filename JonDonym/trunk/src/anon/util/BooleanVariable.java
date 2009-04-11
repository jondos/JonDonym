package anon.util;

public class BooleanVariable 
{
	private boolean m_boolean;
	public BooleanVariable(boolean a_boolean)
	{
		m_boolean = a_boolean;
	}
	
	public void set(boolean a_boolean)
	{
		m_boolean = a_boolean;
	}
	
	public boolean get()
	{
		return m_boolean;
	}

}
