package anon.proxy;

import java.util.StringTokenizer;

import anon.proxy.HTTPProxyCallback.HTTPConnectionHeader;

public final class JonDoFoxHeader implements HTTPConnectionListener {

	final static String HTTP_ENCODING_GZIP = "gzip";
	final static String HTTP_ENCODING_DEFLATE = "deflate";
	
	public final static String JONDOFOX_USER_AGENT = "Mozilla/5.0 Gecko/20070713 Firefox/2.0.0.0";
	public final static String JONDOFOX_USER_AGENT_NEW = "Mozilla/5.0 (en-US; rv:1.9.0.7) Gecko/2009021910 Firefox/3.0.7";
		
	public final static String JONDOFOX_LANGUAGE = "en";
	public final static String JONDOFOX_LANGUAGE_NEW = "en-US";
	public final static String JONDOFOX_CHARSET = "utf-8,*";
	public final static String JONDOFOX_CONTENT_TYPES = "*/*";
	public final static String JONDOFOX_ENCODING = HTTP_ENCODING_GZIP+","+HTTP_ENCODING_DEFLATE;
	
	public void handleRequest(HTTPConnectionEvent event)
	{
	}

	public void downstreamContentBytesReceived(HTTPConnectionEvent event) 
	{
	}


	public void requestHeadersReceived(HTTPConnectionEvent event) 
	{
		if (event == null)
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
			
			if (event.getConnectionHeader().getRequestLine().startsWith("CONNECT"))
			{
				// this is a CONNECT tunneling command - ask the user what to do now
				String[] uaHeader = connHeader.getRequestHeader(HTTPProxyCallback.HTTP_USER_AGENT);
				if (uaHeader == null || uaHeader.length != 1 || uaHeader[0] == null || 
					(!uaHeader[0].equals(JONDOFOX_USER_AGENT) && !uaHeader[0].equals(JONDOFOX_USER_AGENT_NEW)))
				{
					event.setNeedsConfirmation(true);
				}
			}
			
			connHeader.replaceRequestHeader(HTTPProxyCallback.HTTP_USER_AGENT, JONDOFOX_USER_AGENT);
			if (!event.getConnectionHeader().getRequestLine().startsWith("CONNECT"))
			{
				connHeader.replaceRequestHeader(HTTPProxyCallback.HTTP_ACCEPT_LANGUAGE, JONDOFOX_LANGUAGE);
				connHeader.replaceRequestHeader(HTTPProxyCallback.HTTP_ACCEPT_CHARSET, JONDOFOX_CHARSET);
				connHeader.replaceRequestHeader(HTTPProxyCallback.HTTP_ACCEPT, JONDOFOX_CONTENT_TYPES);
				String[] clientSupportedencodings = connHeader.getRequestHeader(HTTPProxyCallback.HTTP_ACCEPT_ENCODING);
				event.getAnonRequest().setInternalEncodingRequired(
						detectInternaEncodingRequired(clientSupportedencodings));
				//will be determined when the server response is received
				event.getAnonRequest().setContentEncodings(null);
			
				connHeader.replaceRequestHeader(HTTPProxyCallback.HTTP_ACCEPT_ENCODING, JONDOFOX_ENCODING);
				//connHeader.removeRequestHeader(HTTPProxyCallback.HTTP_KEEP_ALIVE);
				//connHeader.replaceRequestHeader(HTTPProxyCallback.HTTP_PROXY_CONNECTION, "close");
				connHeader.removeRequestHeader(HTTPProxyCallback.HTTP_IE_UA_CPU);
			}
		}
	}


	public void responseHeadersReceived(HTTPConnectionEvent event) 
	{
		AnonProxyRequest req = event.getAnonRequest();
		HTTPConnectionHeader connHeader = event.getConnectionHeader();
		if( req.isInternalEncodingRequired() )
		{
			String[] contentEncodings = connHeader.getResponseHeader(HTTPProxyCallback.HTTP_CONTENT_ENCODING);
			if(contentEncodings != null)
			{
				req.setContentEncodings(contentEncodings);
				connHeader.removeResponseHeader(HTTPProxyCallback.HTTP_CONTENT_ENCODING);
				//Also remove Content-Length, because this header cannot be changed 
				//when the length of the uncompressed content is determined.
				connHeader.removeResponseHeader(HTTPProxyCallback.HTTP_CONTENT_LENGTH);
			}
			else
			{
				req.setInternalEncodingRequired(false);
			}
		}
	}

	private static boolean detectInternaEncodingRequired(String[] clientSupportedEncodings)
	{
		boolean gzipFound = false;
		boolean zlibFound = false;
		if(clientSupportedEncodings != null)
		{
			StringTokenizer valueTokenizer = null;
			for (int i = 0; i < clientSupportedEncodings.length; i++) 
			{
				valueTokenizer = new StringTokenizer(clientSupportedEncodings[i], ",");
				String token = null;
				while (valueTokenizer.hasMoreTokens()) {
					token = valueTokenizer.nextToken().trim();
					if(!gzipFound)
					{
						gzipFound = token.equals(HTTP_ENCODING_GZIP);
					}
					if(!zlibFound)
					{
						zlibFound = token.trim().equals(HTTP_ENCODING_DEFLATE);
					}
				}
			}
		}
		return !gzipFound || !zlibFound;
	}

	public void upstreamContentBytesReceived(HTTPConnectionEvent event) 
	{
	}

}
