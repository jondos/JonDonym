package anon.pay;

import org.w3c.dom.Document;

import anon.pay.xml.XMLAccountInfo;
import anon.pay.xml.XMLBalance;

import anon.util.XMLUtil;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * Tests if the balance values of a PayAccount are consistent
 * If something is changed within the representation
 * of XMLBalance or PayAccount, please run this test again.
 * @author Simon Pecher
 *
 */
public class BalanceTest extends TestCase 
{
	static String[] payAccountFiles = new String[]
	{
		"testAccount.acc", "08082008.acc", "771911871620.acc", "154641585060.acc",
		"314220514247.acc", "878617592328.acc", "671940293645.acc"
	};
	
	PayAccount testAccount = null;
	String accountFileName = null;
	
	public BalanceTest(String accountFileName)
	{
		super();
		this.accountFileName = accountFileName;
	}


	public BalanceTest(String name, String accountFileName)
	{
		super(name);
		this.accountFileName = accountFileName;
	}
	
	protected void setUp() throws Exception
	{	
		testAccount = getPayAccount(accountFileName);
	}
	
	protected void runTest() throws Throwable
	{
		assertNotNull(testAccount);
		XMLBalance balance = testAccount.getBalance();
		assertNotNull(balance);
		XMLAccountInfo accountInfo = testAccount.getAccountInfo();
		assertNotNull(accountInfo);
		
		long totalBytes = balance.getVolumeKBytesLeft()*1000 + balance.getSpent();
		long currentCredit = testAccount.getCurrentCredit();
		long currentSpent = testAccount.getCurrentSpent();
		long sumOfAllCCTransBytes = accountInfo.getAllCCsTransferredBytes();
		
		assertEquals(totalBytes, currentCredit*1000+currentSpent);
		assertTrue(currentCredit >= 0); /* credit must not be less than 0 */
		assertTrue(currentCredit <= totalBytes); /* credit must not exceed totalBytes */
		/* credit must be the minimum of the BI certified credit (XMLBalance) and the difference of totalbytes
		 * and the sum of "transfered bytes of all cost confirmations (which has Kbyte precision in contrast to all other
		 * balance properties, aargh!) 
		 */
		assertEquals(currentCredit, 
				Math.min( ((sumOfAllCCTransBytes > totalBytes) ? 0 : (totalBytes - sumOfAllCCTransBytes)/1000), 
						(balance.getVolumeKBytesLeft()) ));
		
		//TODO: check if the self calculated credit does not differ too much from the PI-certified balance
		
	}
	
	static PayAccount getPayAccount(String name) throws Exception
	{
		Document payAccountDoc = XMLUtil.readXMLDocument(
				BalanceTest.class.getResourceAsStream("res/"+name));
		return new PayAccount(payAccountDoc.getDocumentElement(), null);
	}
	
	public static Test suite()
	{
		TestSuite suite = new TestSuite(BalanceTest.class.getName());
		for (int i = 0; i < payAccountFiles.length; i++) 
		{
			suite.addTest( 
				new BalanceTest("Balance Test for Account '"+payAccountFiles[i]+"'", payAccountFiles[i]));
		}
		return suite;
	}
	
}
