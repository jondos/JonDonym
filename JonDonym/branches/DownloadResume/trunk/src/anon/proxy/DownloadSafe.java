package anon.proxy;

import java.util.Hashtable;

import sun.reflect.ReflectionFactory.GetReflectionFactoryAction;

import anon.AnonServerDescription;

public class DownloadSafe extends HTTPProxyCallback
{

	private volatile Hashtable downstreamBytes = new Hashtable();
	
	public DownloadSafe()
	{
		super();
	}
	
	
	
	public byte[] handleDownstreamChunk(AnonProxyRequest anonRequest, byte[] chunk, int len) 
	{
		long contentBytes = getLengthOfPayloadBytes(chunk, len);
		Long dsCurrentBytes = (Long) downstreamBytes.remove(anonRequest);
		long dsCurrentValue = 0;
		if(dsCurrentBytes != null)
		{
			dsCurrentValue = dsCurrentBytes.longValue();
		}
		dsCurrentValue += contentBytes;
		downstreamBytes.put(anonRequest, new Long(dsCurrentValue));
		
		/*byte[] newChunk = super.handleDownstreamChunk(anonRequest, chunk, len);
		
		HTTPConnectionHeader connHeader = getConnectionHeader(anonRequest);
		if(connHeader != null)
		{
			String request_line = connHeader.getRequestHeader(HTTP_START_LINE_KEY);
			String responseContentLength = connHeader.getResponseHeader(HTTP_CONTENT_LENGTH);
			if( responseContentLength != null)
			{
				System.out.println("Request: '"+request_line+"': "+dsCurrentValue+" of "+responseContentLength+ " transferred");
			}
		}
		System.out.println("check: "+new String(newChunk));*/
		return chunk;
	}
	
	public byte[] handleUpstreamChunk(AnonProxyRequest anonRequest, byte[] chunk, int len) 
	{
		/* If chunk is null: set up a download resume request*/
		/*if(chunk == null)
		{
			HTTPConnectionHeader connHeader = getConnectionHeader(anonRequest);
			//set resume header here
			return connHeader.dumpRequestHeader();
		}
		else */
		return chunk;//super.handleUpstreamChunk(anonRequest, chunk, len);
	}

	public void handleRequest(HTTPConnectionHeader connHeader)
	{	
	}
	
	public void handleResponse(HTTPConnectionHeader connHeader) 
	{
	}
}

