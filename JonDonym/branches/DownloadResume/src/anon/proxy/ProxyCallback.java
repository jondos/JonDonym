package anon.proxy;

public interface ProxyCallback 
{
	byte[] handleUpstreamChunk(AnonProxyRequest anonRequest, byte[] chunk, int len);
	
	byte[] handleDownstreamChunk(AnonProxyRequest anonRequest, byte[] chunk, int len);
}
