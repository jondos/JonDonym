package anon.proxy;

/**
 * still experimental code
 * @author simon
 *
 */
public class HTTPConnectionWatch implements HTTPConnectionListener {

	
	
	public void downstreamContentBytesReceived(HTTPConnectionEvent event) 
	{
		/*if(event.getAnonRequest() == null)
		{
			throw new NullPointerException("HTTPConnectionEvent must belong to an AnonProxyRequest.");
		}
		
		String infoString = "";
		infoString += event.getDownStreamContentBytes();
		if(event.getConnectionHeader() != null)
		{
			String[] contentLength = 
				event.getConnectionHeader().getResponseHeader(HTTPProxyCallback.HTTP_CONTENT_LENGTH); 
			if(contentLength != null)
			{
				
				infoString += 
					(contentLength.length > 0) ? " of "+contentLength[0] : "";
				long dsBytes = Long.parseLong(contentLength[0]);
				if(dsBytes == event.getDownStreamContentBytes())
				{
					System.err.println(Thread.currentThread().getName()+": completed transfer of "+dsBytes+" bytes for "+event.getConnectionHeader().getRequestLine());
				}
			}
			infoString += " bytes downloaded for "+event.getConnectionHeader().getRequestLine() +", "+ Thread.currentThread().getName();
		}
		System.out.println(infoString);*/
	}

	public void requestHeadersReceived(HTTPConnectionEvent event) 
	{
	}

	public void responseHeadersReceived(HTTPConnectionEvent event) 
	{
		
	}

	public void upstreamContentBytesReceived(HTTPConnectionEvent event) 
	{
	}

}
