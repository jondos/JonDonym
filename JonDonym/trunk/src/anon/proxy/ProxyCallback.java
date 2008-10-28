package anon.proxy;

public interface ProxyCallback 
{
	public byte[] handleUpstreamChunk(AnonProxyRequest anonRequest, byte[] chunk, int len);
	
	public byte[] handleDownstreamChunk(AnonProxyRequest anonRequest, byte[] chunk, int len);
	
	public void closeRequest(AnonProxyRequest anonRequest);
}
