package anon.pay.xml;

import java.util.Hashtable;
import java.util.Properties;

import junit.framework.Test;
import junit.framework.TestSuite;
import junitx.framework.extension.XtendedPrivateTestCase;

import org.w3c.dom.Document;

import anon.util.XMLUtil;

/**
 * This compares the correctness of the price certificate hash concatenation
 * @author simon
 *
 */
public class XMLEasyCCTest extends XtendedPrivateTestCase
{	
	private final static int NR_OF_CCS = 13;
	private final static String PROPS_EXPECTED_RESULTS_OLD_FORMAT = 
		"expectedOldFormat.properties";
	private final static String PROPS_EXPECTED_RESULTS_NEW_FORMAT = 
		"expectedNewFormat.properties";
	private final static String CONCAT_METHOD_KEY = 
		"createConcatenatedPriceCertHashes_"+Hashtable.class.getName()+"_boolean";
	
	public XMLEasyCCTest(String name)
	{
		super(name);
	}

	public void testHashNewFormat() throws Exception
	{
		doHashTestRun(true);
	}
	
	public void testHashOldFormat() throws Exception
	{
		doHashTestRun(false);
	}
	
	private void doHashTestRun(boolean newFormat) throws Exception
	{
		Properties expectedResults = new Properties();
		expectedResults.load(this.getClass().getResourceAsStream("res/"+
				(newFormat ? PROPS_EXPECTED_RESULTS_NEW_FORMAT : 
							PROPS_EXPECTED_RESULTS_OLD_FORMAT) ));
		
		String currentCC = null;
		for (int i = 1; i <= NR_OF_CCS; i++)
		{
			currentCC = "CC"+i;
			Document doc = 
				XMLUtil.readXMLDocument(
						this.getClass().getResourceAsStream("res/"+currentCC+".xml"));
			
			
			XMLEasyCC testProxy = new XMLEasyCC(doc.getDocumentElement());
			
			Object concatenatedHashResult = 
				invokeWithKey(testProxy, CONCAT_METHOD_KEY , 
					new Object[]{testProxy.getPriceCertHashes(), new Boolean(newFormat)});
			
			assertEquals("Check of "+currentCC, 
					expectedResults.get(currentCC), 
					concatenatedHashResult);
		}
	}
	
	public static Test suite()
	{
		TestSuite suite = new TestSuite(XMLEasyCC.class.getSimpleName());
		suite.addTest(new XMLEasyCCTest("testHashNewFormat"));
		suite.addTest(new XMLEasyCCTest("testHashOldFormat"));
		return suite;
	}
}
