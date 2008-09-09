package anon.proxy;

import java.util.Enumeration;
import java.util.Vector;

/**
 * 
 * @author Simon Pecher
 * Experimental framework class to filter or modify data before it is
 * transferred via the gateway. Customized Callback-Objects can 
 * be registered. 
 */
public class ProxyCallbackHandler 
{
	private Vector callbacks = null;
	
	public ProxyCallbackHandler()
	{
		callbacks = new Vector();
	}
	
	public synchronized byte[] deliverUpstreamChunk(AnonProxyRequest anonRequest, byte[] chunk, int len)
	{
		for (Enumeration enumeration = callbacks.elements(); enumeration.hasMoreElements();) 
		{
			ProxyCallback callback = (ProxyCallback) enumeration.nextElement();
			chunk = callback.handleUpstreamChunk(anonRequest, chunk, len);
		}
		return chunk;
	}
	
	public synchronized byte[] deliverDownstreamChunk(AnonProxyRequest anonRequest, byte[] chunk, int len)
	{
		for (Enumeration enumeration = callbacks.elements(); enumeration.hasMoreElements();) 
		{
			ProxyCallback callback = (ProxyCallback) enumeration.nextElement();
			chunk = callback.handleDownstreamChunk(anonRequest, chunk, len);
		}
		return chunk;
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
