package misc;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;

import anon.AnonChannel;
import anon.AnonService;
import anon.AnonServiceFactory;
class XMLRPCTestClient
{
	public static void main(String[] args)
		{
			try{
			AnonService anonService=AnonServiceFactory.create(InetAddress.getLocalHost().getHostAddress(),8889);
			AnonChannel c=anonService.createChannel(AnonChannel.HTTP);
			InputStream in=c.getInputStream();
			OutputStream out=c.getOutputStream();
			out.write("GET HTTP://anon.inf.tu-dresden.de/index.html HTTP/1.1\n\n".getBytes());
			out.flush();
			int b;
			byte[] buff=new byte[100];
			while((b=in.read(buff))>0)
				{
					System.out.print(new String(buff,0,b));
				}
				}
				catch(Exception e)
					{
						e.printStackTrace();
					}
		}
}
