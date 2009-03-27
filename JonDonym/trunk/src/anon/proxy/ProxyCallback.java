package anon.proxy;

public interface ProxyCallback 
{
	/** indicates that a packet should be not proceesed any further */
	public final static int STATUS_FINISHED = 0;
	/** indicates that packet processing should stop immediately and to delay packet sending until enuogh
	 * data for processing is collected
	 */
	public final static int STATUS_DELAY = 1;
	/** indicates that a packet can be proceesed any further by other registered proxy callbacks */
	public final static int STATUS_PROCESSABLE = 2;
	//processing failure should be indicated by raising exception
	//rather than by defining a status code.
	
	
	/*public byte[] handleUpstreamChunk(AnonProxyRequest anonRequest, int offset, byte[] chunk, int len) throws ChunkNotProcessableException;
	
	public byte[] handleDownstreamChunk(AnonProxyRequest anonRequest, int offset, byte[] chunk, int len) throws ChunkNotProcessableException;*/
	
	public int handleUpstreamChunk(AnonProxyRequest anonRequest, ProxyCallbackBuffer buffer) throws ProxyCallbackNotProcessableException;
	
	public int handleDownstreamChunk(AnonProxyRequest anonRequest, ProxyCallbackBuffer buffer) throws ProxyCallbackNotProcessableException;
	
	public void closeRequest(AnonProxyRequest anonRequest);
}
