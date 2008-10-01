package misc;

import java.net.ServerSocket;
import anon.proxy.AnonProxy;
import anon.infoservice.MixCascade;
import anon.infoservice.SimpleMixCascadeContainer;
import logging.LogHolder;
import logging.SystemErrLog;
import logging.LogType;
import logging.LogLevel;
import anon.crypto.SignatureVerifier;
import anon.infoservice.IProxyInterfaceGetter;
import anon.infoservice.ProxyInterface;
import anon.infoservice.IMutableProxyInterface;
import anon.infoservice.ImmutableProxyInterface;
import anon.util.IPasswordReader;
public class AnonProxyTest
{
	public static void main(String[] args)
	{
		try
		{


			//just to ensure that we see some debug messages...
			SystemErrLog log=new SystemErrLog();
			log.setLogType(LogType.ALL);
			log.setLogLevel(LogLevel.DEBUG);
			LogHolder.setLogInstance(new SystemErrLog());
			ServerSocket ss = new ServerSocket(4005);
			IPasswordReader pass=new IPasswordReader()
			{
				public String readPassword(ImmutableProxyInterface a_proxyInterface)
				{
					return "654321";
				}
			};
			final ProxyInterface p=new ProxyInterface("141.76.45.36",3128,ProxyInterface.PROTOCOL_TYPE_HTTP,"sk13",pass,true,true);
			IMutableProxyInterface mutableProxyInterface = new IMutableProxyInterface()
			{
				public IProxyInterfaceGetter getProxyInterface(boolean a_bAnonInterface)
				{
					return new IProxyInterfaceGetter()
					{
						public ImmutableProxyInterface getProxyInterface()
						{
								return p;

						}
					};
				}
		};
			AnonProxy theProxy = new AnonProxy(ss, mutableProxyInterface, mutableProxyInterface);

			//we need to disbale certificate checks (better: set valid root certifcates for productive environments!)
			SignatureVerifier.getInstance().setCheckSignatures(false);
			//InfoServiceHolder ih=InfoServiceHolder.getInstance();
			//ih.setPreferredInfoService(new InfoServiceDBEntry("infoservice.inf.tu-dresden.de",80));
			//Object o=ih.getInfoServices();
			theProxy.start(new SimpleMixCascadeContainer(
						 new MixCascade(null, null, "mix.inf.tu-dresden.de", 6544)));
			synchronized(theProxy)
				{
					theProxy.wait();
				}
		}
		catch (Exception e)
		{
			System.out.println(e);
		}
	}
}
