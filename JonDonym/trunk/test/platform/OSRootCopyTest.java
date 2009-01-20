package platform;

import java.io.File;

import platform.AbstractOS.AbstractRetryCopyProcess;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/*
 * right now a very primitive Testcase serving only Mac OS purposes
 * TODO: Feel free to extend
 */
public class OSRootCopyTest extends TestCase 
{
	
	static final File[] srcFiles = 
		new File[]
        {
			new File("/Applications/JAP.app/Contents/Resources/Java/JAP.jar")
        };
	
	static final File[] destDirs = 
		new File[]
        {
			new File("/usr/bin")
        };
	
	AbstractOS testOs = null;
	
	File src = null;
	File destDir = null;
	AbstractRetryCopyProcess retryChecker = null;
	
	public OSRootCopyTest(File src, File destDir, AbstractRetryCopyProcess retryChecker) 
	{
		this.src = src;
		this.destDir = destDir;
		this.retryChecker = retryChecker;
	}
	
	public static Test suite() 
	{
		TestSuite suite = new TestSuite();
		for (int i = 0; i < srcFiles.length; i++) 
		{
			suite.addTest(new OSRootCopyTest(srcFiles[i], destDirs[i], null));
		}
		return suite;
		
	}

	
	protected void setUp() throws Exception 
	{
		testOs = AbstractOS.getInstance();
		assertNotNull(testOs);
		assertNotNull(src);
		assertNotNull(destDir);
	}


	protected void runTest() throws Throwable 
	{
		assertTrue(testOs.copyAsRoot(src, destDir, retryChecker));
	}
	

}
