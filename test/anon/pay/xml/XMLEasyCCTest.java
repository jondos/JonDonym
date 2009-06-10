package anon.pay.xml;

import java.util.Properties;

import junit.framework.Test;
import junit.framework.TestSuite;
import junitx.framework.extension.XtendedPrivateTestCase;

import org.w3c.dom.Document;

import anon.util.XMLUtil;

/**
 * This compares the correctness of the price cert hash concatenation
 * @author simon
 *
 */
public class XMLEasyCCTest extends XtendedPrivateTestCase
{	
	final static int NR_OF_CCS = 13;
	
	public XMLEasyCCTest()
	{
		super("testHash");
	}
	
	public XMLEasyCCTest(String name)
	{
		super(name);
	}

	public void testHash() throws Exception
	{
		Properties expectedResults = new Properties();
		expectedResults.load(this.getClass().getResourceAsStream("res/expected.properties"));
		
		String currentCC = null;
		for (int i = 1; i <= NR_OF_CCS; i++)
		{
			currentCC = "CC"+i;
			Document doc = XMLUtil.readXMLDocument(this.getClass().getResourceAsStream("res/"+currentCC+".xml"));
			XMLEasyCC easyCC = new XMLEasyCC(doc.getDocumentElement());
			assertEquals("Check of "+currentCC, 
					expectedResults.get(currentCC), 
					easyCC.getConcatenatedPriceCertHashes());
		}
	}
	
	public static Test suite()
	{
		TestSuite suite = new TestSuite();
		suite.addTest(new XMLEasyCCTest());
		return suite;
	}
}
