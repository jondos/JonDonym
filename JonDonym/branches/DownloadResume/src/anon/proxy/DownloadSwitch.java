package anon.proxy;

import jap.JAPController;

import java.util.Hashtable;

import anon.AnonServerDescription;
import anon.AnonServiceEventListener;

public class DownloadSwitch extends HTTPProxyCallback implements AnonServiceEventListener {

	private Hashtable downstreamBytes = new Hashtable();
	
	public DownloadSwitch()
	{
		super();
	}
	
	public byte[] handleDownstreamChunk(byte[] chunk, int len) 
	{
		super.handleDownstreamChunk(chunk, len);
		long contentBytes = getLengthOfPayloadBytes(chunk, len);
		Long dsCurrentBytes = (Long) downstreamBytes.get(Thread.currentThread());
		long dsCurrentValue = 0;
		if(dsCurrentBytes != null)
		{
			dsCurrentValue = dsCurrentBytes.longValue();
		}
		dsCurrentValue += contentBytes;
		downstreamBytes.put(Thread.currentThread(), new Long(dsCurrentValue));
		//System.out.println("rx: "+dsCurrentValue);
		/*if(contentBytes < len)
		{
			String response = new String(dumpHeader());
			System.out.println("response: "+response);
		}*/
		/*String cl = getConnectionHeader(Thread.currentThread(), "Content-Length");
		if(cl != null)
		{
				System.out.println("Content-Length: "+cl);
		}*/
		return chunk;
	}

	/*public byte[] handleUpstreamChunk(byte[] chunk, int len) 
	{
		super.handleUpstreamChunk(chunk, len);
		String request = getConnectionHeader(Thread.currentThread(), "Proxy-Connection");
		if(request != null)
		{
			System.out.println("Request: "+request);
		}
		return chunk;
	}*/

	public void modifyRequestHeaders()
	{
		
		removeConnectionHeader(Thread.currentThread(), HTTP_KEEP_ALIVE);
		setConnectionHeader(Thread.currentThread(), HTTP_PROXY_CONNECTION, "close");
			
			/*else
			{
				System.out.println("Connect.");
			}*/
			
			//setConnectionHeader(Thread.currentThread(), HTTP_ACCEPT_LANGUAGE, "franz");
			//setConnectionHeader(Thread.currentThread(), HTTP_USER_AGENT, "Firefox");
			//removeConnectionHeader(Thread.currentThread(), HTTP_REFERER);

	}
	
	public void connecting(AnonServerDescription description) 
	{	
	}

	public void connectionError() {
		// TODO Auto-generated method stub
		
	}

	public void connectionEstablished(AnonServerDescription description) {
		// TODO Auto-generated method stub
		
	}

	public void dataChainErrorSignaled() {
		// TODO Auto-generated method stub
		
	}

	public void disconnected() 
	{
		System.out.println("Download Switch: disconnected");
	}

	public void packetMixed(long bytes) {
		// TODO Auto-generated method stub
		
	}

}
