package anon.util.test;

import anon.util.Util;
import junit.framework.Test;
import junit.framework.TestSuite;
import junitx.framework.extension.XtendedPrivateTestCase;

public class StringReplaceTest extends XtendedPrivateTestCase 
{
	private static String[] SPECIAL_CHARS = new String[] {"&", "<", ">"};
    private static String[] ENTITIES = new String[] {"&amp;", "&lt;", "&gt;"};
	
	public StringReplaceTest() 
	{
		super("replaceTest");	
	}

	public void replaceTest() throws Exception
	{
		String s="&&amp;<Halloder>&amp;&";
		for (int i = 0; i < SPECIAL_CHARS.length; i++)
		{
			s = Util.replaceAll(s, SPECIAL_CHARS[i], ENTITIES[i], 
				(SPECIAL_CHARS[i].equals("&") ? ENTITIES : null));
		}
		assertEquals("&amp;&amp;&lt;Halloder&gt;&amp;&amp;", s);
	}
	
	public static Test suite()
	{
		TestSuite suite = new TestSuite();
		suite.addTest(new StringReplaceTest());
		return suite;
	}
}
