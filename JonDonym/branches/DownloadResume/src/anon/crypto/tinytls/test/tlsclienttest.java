package anon.crypto.tinytls.test;

import java.io.InputStream;

import anon.crypto.tinytls.TinyTLS;
import logging.LogHolder;
import logging.SystemErrLog;

public class tlsclienttest
{

	public static void main(String[] args) throws Exception
	{
		LogHolder.setLogInstance(new SystemErrLog());
		//Socket	tls = new Socket("localhost", 3456);
		TinyTLS tls = new TinyTLS("localhost", 3456);
//		TinyTLS tls = new TinyTLS("anon.inf.tu-dresden.de", 49876);
		tls.checkRootCertificate(false);
	//	tls.setRootKey(JAPCertificate.getInstance("testkey.cer").getPublicKey());
		tls.startHandshake();
//		OutputStream out=tls.getOutputStream();
//		out.write("GET /index.html HTTP/1.0\r\n\r\n".getBytes());
//		out.flush();
		InputStream in = tls.getInputStream();
//		TimedOutputStream.init();
//		OutputStream out=new TimedOutputStream(tls.getOutputStream(),20000);
//	while(in!=null)
		byte w[]=new byte[1000000];
		tls.setSoTimeout(1000);
//	out.write(w);
		int i;
		try
		{
			while ( (i = in.read()) > 0)
			{
				System.out.print( (char) i);
			}
		}
		catch (Exception e)
		{
		}
		tls.close();
	}
}
