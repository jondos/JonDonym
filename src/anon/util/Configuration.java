package anon.util;

public interface Configuration 
{
	public String read() throws Exception;
	public void write(String a_configurationContent) throws Exception;
}
