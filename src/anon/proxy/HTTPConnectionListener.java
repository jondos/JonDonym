package anon.proxy;

import anon.proxy.HTTPProxyCallback.HTTPConnectionHeader;

public interface HTTPConnectionListener 
{
	public void requestHeadersReceived(HTTPConnectionEvent event);
	
	public void responseHeadersReceived(HTTPConnectionEvent event);
	
	public void upstreamContentBytesReceived(HTTPConnectionEvent event);
	
	public void downstreamContentBytesReceived(HTTPConnectionEvent event);
}
