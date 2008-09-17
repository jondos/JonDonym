package anon.proxy;

import anon.proxy.HTTPProxyCallback.HTTPConnectionHeader;

public class HTTPConnectionEvent 
{
	private HTTPConnectionHeader connectionHeader;
	private volatile long upStreamContentBytes;
	private volatile long downStreamContentBytes;
	private AnonProxyRequest anonRequest;
	
	public HTTPConnectionEvent() 
	{
	}
	
	HTTPConnectionEvent(HTTPConnectionHeader connectionHeader, long upStreamContentBytes,
								long downStreamContentBytes, AnonProxyRequest anonRequest) 
	{
		this.connectionHeader = connectionHeader;
		this.upStreamContentBytes = upStreamContentBytes;
		this.downStreamContentBytes = downStreamContentBytes;
		this.anonRequest = anonRequest;
	}


	public HTTPConnectionHeader getConnectionHeader() 
	{
		return connectionHeader;
	}


	public void setConnectionHeader(HTTPConnectionHeader connectionHeader) 
	{
		this.connectionHeader = connectionHeader;
	}


	public long getUpStreamContentBytes() 
	{
		return upStreamContentBytes;
	}


	public void setUpStreamContentBytes(long upStreamContentBytes) 
	{
		this.upStreamContentBytes = upStreamContentBytes;
	}


	public long getDownStreamContentBytes()
	{
		return downStreamContentBytes;
	}


	public void setDownStreamContentBytes(long downStreamContentBytes) 
	{
		this.downStreamContentBytes = downStreamContentBytes;
	}


	public AnonProxyRequest getAnonRequest() 
	{
		return anonRequest;
	}


	public void setAnonRequest(AnonProxyRequest anonRequest) 
	{
		this.anonRequest = anonRequest;
	}
	
	
}
