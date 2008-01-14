/*
 * Created on Jun 22, 2004
 */
package anon.tor.test;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

import anon.tor.TorChannel;

/**
 * @author stefan
 */
public class proxythread implements Runnable
{

	private OutputStream torout;
	private InputStream torin;
	private OutputStream out;
	private InputStream in;
	private Socket client;
	private Thread t;
	private TorChannel channel;

	public proxythread(Socket client, TorChannel channel) throws IOException
	{
		this.torin = channel.getInputStream();
		this.torout = channel.getOutputStream();
		this.in = client.getInputStream();
		this.out = client.getOutputStream();
		this.client = client;
		this.channel = channel;

	}

	public void start()
	{
		t = new Thread(this, "Tor proxy thread");
		t.start();
	}

	public void stop()
	{
		try
		{
			while (torin.available() > 0)
			{
				byte[] b = new byte[torin.available()];
				int len = torin.read(b);
				out.write(b, 0, len);
				out.flush();
			}
		}
		catch (Exception ex)
		{
		}
		channel.close();
		try
		{
			client.close();
		}
		catch (Exception ex)
		{
			System.out.println("Fehler beim schliessen des kanals");
		}
		System.out.println("kanal wird geschlossen");
		t.stop();
	}

	public void run()
	{
		while (true)
		{
			try
			{
				while (torin.available() > 0)
				{
					byte[] b = new byte[torin.available()];
					int len = torin.read(b);
					out.write(b, 0, len);
					out.flush();
				}
				while (in.available() > 0)
				{
					byte[] b = new byte[in.available()];
					int len = in.read(b);
					torout.write(b, 0, len);
					torout.flush();
				}
				if (channel.isClosedByPeer())
				{
					this.stop();
				}
				Thread.sleep(20);
			}
			catch (Exception ex)
			{
				System.out.println("Exception catched : " + ex.getLocalizedMessage());
				ex.printStackTrace();
				this.stop();
			}
		}

	}

}
