package anon.tor.test;

import java.net.ConnectException;
import java.io.IOException;

import junitx.framework.PrivateTestCase;
import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;

import anon.AnonChannel;
import anon.tor.Tor;
import anon.tor.TorAnonServerDescription;
import anon.infoservice.ListenerInterface;

public class TorTest extends PrivateTestCase
{
	Tor tor;

	public TorTest(String s)
	{
		super(s);
	}

	public void setUp()
	{
		tor = Tor.getInstance();
		tor.initialize(new TorAnonServerDescription(), null, null);
	}

	public void tearDown()
	{
		tor.shutdown(true);
	}

	public void testDNS()
	{
		String ip = null;

		ip = tor.resolveDNS("a.root-servers.net");
		assertTrue(ListenerInterface.isValidIP(ip));

		ip = tor.resolveDNS("www.google.com");
		assertTrue(ListenerInterface.isValidIP(ip));

		ip = tor.resolveDNS("tor.eff.org");
		assertTrue(ListenerInterface.isValidIP(ip));

		ip = tor.resolveDNS("zeit.de");
		assertTrue(ListenerInterface.isValidIP(ip));
	}

	public void testChannel() throws ConnectException, IOException
	{
		AnonChannel channel = tor.createChannel("www.google.com", 80);
		channel.getOutputStream().write("GET /index.html HTTP/1.0\n\r\n\r".getBytes());

		byte[] b = new byte[200];
		int i = channel.getInputStream().read(b,0,128);
		assertTrue(i > 0);
	}
}
