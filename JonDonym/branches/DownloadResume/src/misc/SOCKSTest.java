package misc;

import anon.proxy.AnonProxy;
import java.net.ServerSocket;
import java.io.IOException;
import anon.infoservice.MixCascade;
import anon.infoservice.SimpleMixCascadeContainer;
import anon.tor.TorAnonServerDescription;
import logging.LogHolder;
import logging.SystemErrLog;
import logging.LogLevel;
import logging.LogType;
import anon.crypto.SignatureVerifier;
import anon.infoservice.HTTPConnectionFactory;
import HTTPClient.HTTPConnection;
import anon.infoservice.ListenerInterface;
import anon.infoservice.ProxyInterface;
import HTTPClient.HTTPResponse;
import HTTPClient.ModuleException;
import java.util.Random;
import jap.JAPDebug;
import anon.infoservice.InfoServiceHolder;
import anon.infoservice.InfoServiceDBEntry;
import anon.client.AnonClient;

public final class SOCKSTest
{
	private final class SocksHttpConnection implements Runnable
	{
		private String m_strHost;
		private int m_Port;
		private String m_strFile;
		SocksHttpConnection(String host,int port,String file)
		{
			m_strHost=host;
			m_Port=port;
			m_strFile=file;
			Thread t=new Thread(this);
			t.setDaemon(true);
			t.start();
		}
		public void run()
		{
			try{
			HTTPConnection conn=HTTPConnectionFactory.getInstance().createHTTPConnection(new ListenerInterface(m_strHost,m_Port),
				new ProxyInterface("127.0.0.1",4007,ProxyInterface.PROTOCOL_TYPE_SOCKS,null));
			conn.setTimeout(60000);
			HTTPResponse resp=conn.Get(m_strFile);
			if(resp.getStatusCode()!=200)
			{
				LogHolder.log(LogLevel.WARNING,LogType.TOR,"Error getting Web page!");
			}
			else
				LogHolder.log(LogLevel.WARNING,LogType.TOR,"Successfull getting Web page!");

			}
			catch(Exception e)
			{
				e.printStackTrace();
				LogHolder.log(LogLevel.WARNING,LogType.TOR,"Exception getting Web page! - "+e.getMessage());
		}
		}
	}
	public static void main(String[] args) throws IOException, Exception
	{
		SOCKSTest t=new SOCKSTest();
		t.doIt();
	}
	public void doIt() throws IOException, Exception
		{
		JAPDebug log=JAPDebug.getInstance();
		log.setLogLevel(LogLevel.DEBUG);
		log.setLogType(LogType.TOR);
		LogHolder.setLogInstance(log);
		LogHolder.setDetailLevel(LogHolder.DETAIL_LEVEL_LOWEST);
		LogHolder.log(LogLevel.DEBUG,LogType.TOR,"Start stress testr!");
		ServerSocket listener=new ServerSocket(4007);
		InfoServiceHolder.getInstance().setPreferredInfoService(new InfoServiceDBEntry("infoservice.inf.tu-dresden.de",80));
		AnonClient.setLoginTimeout(40000);
		AnonProxy proxy=new AnonProxy(listener);
		TorAnonServerDescription td=new TorAnonServerDescription("141.76.45.45",9030,true);
	//	TorAnonServerDescription td=new TorAnonServerDescription(true,true);
		td.setMaxConnectionsPerRoute(100);
		td.setMaxRouteLen(2);
		proxy.setTorParams(td);
		SignatureVerifier.getInstance().setCheckSignatures(false);
		proxy.start(new SimpleMixCascadeContainer(new MixCascade("mix.inf.tu-dresden.de",6544)));
		proxy.setDummyTraffic(60000);
		Random rnd=new Random();
		Thread.sleep(20000);
		LogHolder.log(LogLevel.DEBUG,LogType.TOR,"Stress test building connection");
		for(int i=0;i<1000;i++)
		{
		//new SocksHttpConnection("www.google.com",80,"/index.html");
		new SocksHttpConnection("anon.inf.tu-dresden.de",80,"/bl.gif");
		Thread.sleep(/*rnd.nextInt(1000)+200*/450);
		}
	}



}
