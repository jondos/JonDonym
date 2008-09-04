package misc;

import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Random;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.OutputStream;
import java.io.InputStream;

import anon.util.Util;

public class GenerateUpload implements Runnable
{
	private static InputStream in;
	private static byte[] randomData;
	public static void main(String[] args) throws UnknownHostException, IOException
	{
		randomData=new byte[10000000];
		Random rand=new Random();
		//rand.nextBytes(randomData);
		for(int i=0;i<randomData.length;i++)
			randomData[i]=(byte)i;
		Socket s=new Socket("127.0.0.1",4001);
		OutputStream out=s.getOutputStream();
		in=s.getInputStream();
		Thread t=new Thread(new GenerateUpload());
		t.start();
		out.write("CONNECT 127.0.0.1:7 HTTP/1.0\n\r\n\r".getBytes()); //Note: Connects to the ECHO service through the cascade / proxy
		long l=0;
		int currentPos=0;
		while(true)
		{
			out.write(randomData,currentPos,10000);
			currentPos+=10000;
			currentPos%=10000000;
			l+=10000;
			if((l%1000000)==0)
				System.out.println("Sent: "+l/1000000+" MBytes");
			try {
				Thread.sleep(10);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

	}

	public void run()
	{
		try{
			//LineNumberReader inr=new LineNumberReader(new InputStreamReader(in));
			//System.out.println(inr.readLine());
			//String s=inr.readLine();
			//s=inr.readLine();
			
		byte[] buff=new byte[10000];
		long l=0;
		long reported=1000000;
		int currentPos=0;
		int h=in.read(buff,0,39);//skip proxy response headers
		h=in.read(buff,0,1); // Strange thing here: we got (sometimes?) and extra chr(13) after the HTTP response headers - why ???
		while(true)
		{
			int r=Math.min(buff.length, randomData.length-currentPos); 
			r=in.read(buff,0,r);
			if(!Util.arraysEqual(buff, 0, randomData, currentPos, r))
			{
			System.out.println("REad error!");
			for (int u=0;u<randomData.length-1;u++)
			{
				if(buff[0]==randomData[u]&&buff[1]==randomData[u+1])
					{System.out.println(u);break;}
			}
			}
			l+=r;
			currentPos+=r;
			currentPos%=10000000;
			if(l>=reported)
			{
				System.out.println("Read: "+l/1000000.0+" MBytes");
				reported+=1000000;
			}
		}
		}catch(Exception e)
		{
			System.out.println("Read failed!");
		}
	}
}
