package anon.proxy;

import java.util.Enumeration;
import java.util.Iterator;
import java.util.Vector;

/**
 * 
 * @author Simon Pecher
 * A Framework class to filter or modify before it is transferred via the gateway.
 * Customized Callback-Objects can be registered. 
 */
public class ProxyCallbackHandler 
{
	private Vector callbacks = null;
	
	public ProxyCallbackHandler()
	{
		callbacks = new Vector();
	}
	
	public synchronized byte[] deliverUpstreamChunk(byte[] chunk, int len)
	{
		//byte[] newChunk = new byte[len];
		//System.arraycopy(chunk, 0, newChunk, 0, len);
		for (Enumeration enumeration = callbacks.elements(); enumeration.hasMoreElements();) 
		{
			ProxyCallback callback = (ProxyCallback) enumeration.nextElement();
			//newChunk = callback.handleUpstreamChunk(newChunk);
			chunk = callback.handleUpstreamChunk(chunk, len);
			
		}
		return chunk;
	}
	
	public synchronized byte[] deliverDownstreamChunk(byte[] chunk, int len)
	{
		//byte[] newChunk = new byte[len];
		//System.arraycopy(chunk, 0, newChunk, 0, len);
		
		for (Enumeration enumeration = callbacks.elements(); enumeration.hasMoreElements();) 
		{
			ProxyCallback callback = (ProxyCallback) enumeration.nextElement();
			//newChunk = callback.handleDownstreamChunk(newChunk);
			chunk = callback.handleDownstreamChunk(chunk, len);
		}
		return chunk;
	}
	
	public void finishStream()
	{
		System.out.println("Stream regularly finished");
	}
	
	public synchronized void registerProxyCallback(ProxyCallback callback)
	{
		callbacks.addElement(callback);
	}
	
	public synchronized void removeCallback(ProxyCallback callback)
	{
		callbacks.removeElement(callback);
	}
}
