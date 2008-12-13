package anon.proxy;

public interface ProxyCallback 
{
	public byte[] handleUpstreamChunk(AnonProxyRequest anonRequest, byte[] chunk, int len) throws ChunkNotProcessableException;
	
	public byte[] handleDownstreamChunk(AnonProxyRequest anonRequest, byte[] chunk, int len) throws ChunkNotProcessableException;
	
	public void closeRequest(AnonProxyRequest anonRequest);
}
