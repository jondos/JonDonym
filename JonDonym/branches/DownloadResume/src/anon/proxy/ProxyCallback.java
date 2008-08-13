package anon.proxy;

public interface ProxyCallback 
{
	byte[] handleUpstreamChunk(byte[] chunk, int len);
	
	byte[] handleDownstreamChunk(byte[] chunk, int len);
}
