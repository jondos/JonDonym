package anon.tor.ordescription.test;

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
		suite.addTestSuite(ORListTest.class);
		suite.addTestSuite(PlainORListFetcherTest.class);
		return suite;
	}
}
