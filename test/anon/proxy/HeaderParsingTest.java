package anon.proxy;

import java.util.Random;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class HeaderParsingTest extends TestCase 
{	
	final static int CHUNK_SIZE = 1000;
	final static int PREFIX_SIZE = 1000;
	
	
	
	private static class TestConfig
	{	
		static Random random = new Random(System.currentTimeMillis());
		byte[] prefix = null;
		byte[] chunk = null;
		int result = -1;
		String name = null;
		
		/* creates new TestConfig which by default is negative (no header ending -> result -1 ) */
		TestConfig()
		{
			/* Initialize random byte arrays */
			chunk = new byte[CHUNK_SIZE];
			prefix = new byte[PREFIX_SIZE];
			
			random.nextBytes(prefix);
			for (int i = 0; i < prefix.length; i++) 
			{
				if(prefix[i] == 10 ||
				   prefix[i] == 13 )
				{
					prefix[i] = 0;
				}
			}
			
			random.nextBytes(chunk);
			for (int i = 0; i < chunk.length; i++) 
			{
				if(chunk[i] == 10 ||
				   chunk[i] == 13 )
				{
					chunk[i] = 0;
				}
			}
			result = -1;
			name = "Negative result (-1)";
		}
		
		TestConfig(String prefixString, String chunkString, int result)
		{
			this.prefix = prefixString == null ? null : prefixString.getBytes();
			this.chunk = chunkString.getBytes();
			this.result = result;
			name = "Customized test, result: "+result;
		}
		
		static TestConfig[] getAllSplitTestConfigs()
		{
			TestConfig[] splitConfigs = 
				new TestConfig[HTTPProxyCallback.HTTP_HEADER_END_BYTES.length-1];
			for(int i = 0; i < splitConfigs.length; i++)
			{
				splitConfigs[i] = getSplitTestConfig(i);
			}
			return splitConfigs;
		}
		
		static TestConfig getSplitTestConfig(int splitIndex)
		{
			if(splitIndex < 0 || splitIndex >= HTTPProxyCallback.HTTP_HEADER_END_BYTES.length-1 )
			{
				throw new IllegalArgumentException("splitIndex must be in between 0 and "+
						(HTTPProxyCallback.HTTP_HEADER_END_BYTES.length-2));
			}
			TestConfig config = new TestConfig();
			
			int bytesToPrefix = splitIndex+1;
			int j = 0;
			for(int i = (config.prefix.length - bytesToPrefix); i < config.prefix.length; i++ )
			{
				config.prefix[i] = HTTPProxyCallback.HTTP_HEADER_END_BYTES[j++];
			}
			
			int k = 0;
			for( k = 0; j < HTTPProxyCallback.HTTP_HEADER_END_BYTES.length; k++ )
			{
				config.chunk[k] = HTTPProxyCallback.HTTP_HEADER_END_BYTES[j++];
			}
			config.result = k;
			config.name="Split at position "+splitIndex+", result: "+config.result;
			
			return config;
		}
		
		static TestConfig getNonSplitTestConfig(int pos)
		{
			TestConfig config = new TestConfig();
			if(pos < 0 || 
				pos > (config.chunk.length-HTTPProxyCallback.HTTP_HEADER_END_BYTES.length) )
			{
				throw new IllegalArgumentException("pos must be in between 0 and "+
						(config.chunk.length-HTTPProxyCallback.HTTP_HEADER_END_BYTES.length));
			}
			
			for(int i = 0; i < HTTPProxyCallback.HTTP_HEADER_END_BYTES.length; i++)
			{
				config.chunk[pos+i] = HTTPProxyCallback.HTTP_HEADER_END_BYTES[i];
			}
			config.result = pos + HTTPProxyCallback.HTTP_HEADER_END_BYTES.length;
			config.name="No split, result: "+config.result;
			return config;
		}
		
		static TestConfig getNegativeTestConfig()
		{
			return new TestConfig();
		}
		
		static TestConfig getRandomTestConfig()
		{
			//TestConfig config = null; new TestConfig();
			
			boolean split = random.nextBoolean();
			if(split)
			{
				return getSplitTestConfig(
						random.nextInt(HTTPProxyCallback.HTTP_HEADER_END_BYTES.length-1));
			}
			else
			{
				return getNonSplitTestConfig(
						random.nextInt(CHUNK_SIZE-HTTPProxyCallback.HTTP_HEADER_END_BYTES.length));
			}
		}
	}
	
	byte[] prefix = null;
	byte[] chunk = null;
	int result = -1;
	
	public HeaderParsingTest(TestConfig config) 
	{
		super(config.name);
		prefix = config.prefix;
		chunk = config.chunk;
		result = config.result;
	}

	protected void runTest()
	{
		assertEquals(result, HTTPProxyCallback.indexOfHTTPHeaderEnd(prefix,chunk));
	}
	
	public static Test suite()
	{
		TestSuite suite = new TestSuite("AllParsingTests");
		TestConfig[] splits = TestConfig.getAllSplitTestConfigs();
		for (int i = 0; i < splits.length; i++) 
		{
			suite.addTest(new HeaderParsingTest(splits[i]));
		}
		for (int i = 0; i <= (CHUNK_SIZE-HTTPProxyCallback.HTTP_HEADER_END_BYTES.length); i++) 
		{
			suite.addTest(new HeaderParsingTest(TestConfig.getNonSplitTestConfig(i)));
		}
		
		for (int i = 0; i < 10; i++) 
		{
			suite.addTest(new HeaderParsingTest(TestConfig.getNegativeTestConfig()));
		}
		
		for (int i = 0; i < 100; i++) 
		{
			suite.addTest(new HeaderParsingTest(TestConfig.getRandomTestConfig()));
		}
		
		suite.addTest(new HeaderParsingTest(new TestConfig("blbla\r", "\n\r\nusw", 3)));
		suite.addTest(new HeaderParsingTest(new TestConfig("blbla\r\n", "\n\r\nusw", -1)));
		suite.addTest(new HeaderParsingTest(new TestConfig("blbla\r\n", "\r\nusw", 2)));
		suite.addTest(new HeaderParsingTest(new TestConfig("blbla\r\n\r", "\r\nusw", -1)));
		suite.addTest(new HeaderParsingTest(new TestConfig("blbla\r\n\r", "\nusw", 1)));
		suite.addTest(new HeaderParsingTest(new TestConfig("bla", "i\r\n\r\nusw", 5)));
		suite.addTest(new HeaderParsingTest(new TestConfig(null, "i\r\n\r\nusw", 5)));
		
		return suite;
	}
}
