package anon.proxy;

import anon.proxy.HTTPProxyCallback.HTTPConnectionHeader;

public final class JonDoFoxHeader implements HTTPConnectionListener {

	public final String JONDOFOX_USER_AGENT = "Mozilla/5.0 Gecko/20070713 Firefox/2.0.0.0";
	public final String JONDOFOX_LANGUAGE = "en";
	public final String JONDOFOX_CHARSET = "utf-8,*";
	public final String JONDOFOX_CONTENT_TYPES = "*/*";
	public final String JONDOFOX_ENCODING = "gzip,deflate";
	
	public void handleRequest(HTTPConnectionEvent event)
	{
	}

	public void downstreamContentBytesReceived(HTTPConnectionEvent event) 
	{
	}


	public void requestHeadersReceived(HTTPConnectionEvent event) 
	{
		if(event == null)
		{
			return;
		}
		HTTPConnectionHeader connHeader = event.getConnectionHeader();
		if(connHeader != null)
		{
			String domain = connHeader.getRequestLine();
			if(domain != null)
			{
				int afterMethod = domain.indexOf(" ");
				if(afterMethod != -1)
				{
					domain = domain.substring(afterMethod+1);
					int slashix = domain.indexOf("//");
					if(slashix != -1)
					{
						slashix = domain.indexOf('/', slashix+2);
						if(slashix != -1)
						{
							domain = domain.substring(0,slashix+1);
							connHeader.replaceRequestHeader(HTTPProxyCallback.HTTP_REFERER, domain);
						}
					}
				}
			}
			
			connHeader.replaceRequestHeader(HTTPProxyCallback.HTTP_USER_AGENT, JONDOFOX_USER_AGENT);
			connHeader.replaceRequestHeader(HTTPProxyCallback.HTTP_ACCEPT_LANGUAGE, JONDOFOX_LANGUAGE);
			connHeader.replaceRequestHeader(HTTPProxyCallback.HTTP_ACCEPT_CHARSET, JONDOFOX_CHARSET);
			connHeader.replaceRequestHeader(HTTPProxyCallback.HTTP_ACCEPT, JONDOFOX_CONTENT_TYPES);
			//connHeader.replaceRequestHeader(HTTPProxyCallback.HTTP_ACCEPT_ENCODING, JONDOFOX_ENCODING);
			//connHeader.removeRequestHeader(HTTPProxyCallback.HTTP_KEEP_ALIVE);
			//connHeader.replaceRequestHeader(HTTPProxyCallback.HTTP_PROXY_CONNECTION, "close");
			connHeader.removeRequestHeader(HTTPProxyCallback.HTTP_IE_UA_CPU);
		}
	}


	public void responseHeadersReceived(HTTPConnectionEvent event) 
	{
	}


	public void upstreamContentBytesReceived(HTTPConnectionEvent event) 
	{
	}

}
