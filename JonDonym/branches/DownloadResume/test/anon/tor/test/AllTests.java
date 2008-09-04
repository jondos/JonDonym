package anon.tor.test;

import junit.framework.Test;
import junit.framework.TestSuite;

public class AllTests 
{
	public static void main(String[] args)
	{
		junit.swingui.TestRunner.run(AllTests.class);
	}
	
	public static Test suite()
	{
		TestSuite suite = new TestSuite(AllTests.class.getName());
		suite.addTest(anon.tor.ordescription.test.AllTests.suite());
		suite.addTestSuite(TorTest.class);
		return suite;
	}
}
