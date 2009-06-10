package anon.pay;

import anon.pay.xml.XMLEasyCC;
import anon.pay.xml.XMLEasyCCTest;
import junit.framework.Test;
import junit.framework.TestSuite;

public class AllTests 
{
	public static void main(String[] a_Args)
	{
		junit.swingui.TestRunner.run(AllTests.class);
	}
	
	public static Test suite()
	{
		TestSuite suite = new TestSuite(AllTests.class.getName());
		suite.addTest(BalanceTest.suite());
		suite.addTest(XMLEasyCCTest.suite());
		return suite;
	}
}
