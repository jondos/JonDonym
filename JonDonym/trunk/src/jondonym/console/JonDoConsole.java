package jondonym.console;

import jap.JAPController;
import jap.JAPUtil;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;

import java.util.Observable;
import java.util.Vector;

import org.apache.log4j.Logger;

import anon.proxy.AnonProxy;
import anon.infoservice.AbstractDatabaseEntry;
import anon.infoservice.Database;
import anon.infoservice.IDistributable;
import anon.infoservice.IDistributor;
import anon.infoservice.InfoServiceDBEntry;
import anon.infoservice.InfoServiceHolder;
import anon.infoservice.MixCascade;
import anon.infoservice.update.AbstractMixCascadeUpdater;
import anon.infoservice.update.InfoServiceUpdater;
import anon.infoservice.update.PaymentInstanceUpdater;
import logging.AbstractLog4jLog;
import logging.Log;
import logging.LogHolder;
import logging.LogType;
import logging.LogLevel;
import logging.SystemErrLog;
import anon.client.AbstractAutoSwitchedMixCascadeContainer;
import anon.client.DummyTrafficControlChannel;
import anon.client.ITermsAndConditionsContainer;
import anon.crypto.JAPCertificate;
import anon.crypto.SignatureVerifier;
import anon.util.ClassUtil;
import anon.util.Configuration;
import anon.util.XMLUtil;
import anon.util.Updater.ObservableInfo;

public class JonDoConsole
{
	private static InfoServiceUpdater ms_isUpdater;
	private static MixCascadeUpdater ms_cascadeUpdater;
	private static PaymentInstanceUpdater ms_paymentUpdater;
	private static ServerSocket ms_socketListener;
	private static AnonProxy ms_jondonymProxy;
	private static AutoSwitchedMixCascadeContainer ms_serviceContainer;
	
	
	public static synchronized void init(final Logger a_logger, Configuration a_configuration) throws Exception
	{		
		if (ms_isUpdater != null)
		{
			return;
		}
		
		Log templog;
		if (a_logger == null)
		{
			templog = new SystemErrLog();
		}
		else
		{
			templog = new AbstractLog4jLog()
			{
				  protected Logger getLogger()
					{
						return a_logger;
					}
			};
		}
		
   		LogHolder.setLogInstance(templog);
   		templog.setLogType(LogType.ALL);
   		templog.setLogLevel(LogLevel.WARNING);
   		
   		LogHolder.log(LogLevel.ALERT, LogType.MISC, "Initialising " + ClassUtil.getClassNameStatic() + " version 0.3");
   		
		ClassUtil.enableFindSubclasses(false); // This would otherwise start non-daemon AWT threads, blow up memory and prevent closing the app.
		XMLUtil.setStorageMode(XMLUtil.STORAGE_MODE_AGRESSIVE); // Store as few XML data as possible for memory optimization.
		SignatureVerifier.getInstance().setCheckSignatures(true);
		
		 
		JAPUtil.addDefaultCertificates("acceptedInfoServiceCAs/", new String[] {"japinfoserviceroot.cer", "InfoService_CA.cer"}, JAPCertificate.CERTIFICATE_TYPE_ROOT_INFOSERVICE);
		JAPUtil.addDefaultCertificates("acceptedMixCAs/", new String[] {"japmixroot.cer", "Operator_CA.cer", //"Test_CA.cer.dev", 
				"gpf_jondonym_ca.cer"}, JAPCertificate.CERTIFICATE_TYPE_ROOT_MIX);
		JAPUtil.addDefaultCertificates("acceptedPIs/", new String[]{//"bi.cer.dev", 
				"Payment_Instance.cer"}, JAPCertificate.CERTIFICATE_TYPE_PAYMENT);
     
		 // simulate database distributor and suppress distributor warnings
		Database.registerDistributor(new IDistributor()
		{
			public void addJob(IDistributable a_distributable)
			{
			}
		});
    
		InfoServiceDBEntry[] defaultInfoService = JAPController.createDefaultInfoServices();
		for (int i = 0; i < defaultInfoService.length; i++)
		{
			Database.getInstance(InfoServiceDBEntry.class).update(defaultInfoService[i]);
		}
		InfoServiceHolder.getInstance().setPreferredInfoService(defaultInfoService[0]);
		
		ObservableInfo a_observableInfo = new ObservableInfo(new Observable())
		{
			public Integer getUpdateChanged()
			{
				return new Integer(0);
			}
			public boolean isUpdateDisabled()
			{
				return false;
			}
		};
		
		ms_isUpdater = new InfoServiceUpdater(a_observableInfo);
		ms_cascadeUpdater = new MixCascadeUpdater(a_observableInfo);
		ms_paymentUpdater = new PaymentInstanceUpdater(a_observableInfo);
	}
	
	public static void main(String[] args)
	{
		try
		{
			init(null, null);
			start();
			
			if (!isRunning())
			{
				return;
			}
			
			String entered = "";
			boolean bChoose = false;
			Vector cascades = null;
			int index;
			
			while (true)
			{
				if (entered != null && !bChoose)
				{
					System.out.println("Type 'choose' to choose another service, 'force' to prevent auto-switching or 'exit' to quit.");
				}
				entered = null;
				try
				{
					entered = new BufferedReader(new InputStreamReader(System.in)).readLine();
				}
				catch(Throwable t)
				{
				}
				if (entered == null)
				{
					//Hm something is strange... do not simply continue but wait some time
					//BTW: That are situations when this could happen?
					// One is if JAP is run on VNC based X11 server
					try
					{
						Thread.sleep(1000);
					}
					catch (InterruptedException e)
					{
					}
					continue;
				}
	
				if (bChoose)
				{
					bChoose = false;
					try
					{
						index = Integer.parseInt(entered) - 1;
						if (index >= 0 && index < cascades.size())
						{
							switchCascade((MixCascade)cascades.elementAt(index));
						}
						continue;
					}
					catch (NumberFormatException a_e)
					{
					}
					System.out.println("Sorry, this service is not available.");
				}
				else if (entered.equals("exit"))
				{
					stop();
					break;
				}
				else if (entered.equals("choose"))
				{
					cascades = getAvailableCascades();
					for (int i = 0; i < cascades.size(); i++)
					{
						System.out.println((i+1) + ". " + cascades.elementAt(i));
					}
					bChoose = true;
					
					continue;
				}
				else if (entered.equals("force"))
				{
					setCascadeAutoSwitched(!isCascadeAutoSwitched());
					System.out.println("Auto-switch is " + (isCascadeAutoSwitched() ? "ON" : "OFF") + ".");
				}
				else
				{
					System.out.println(ms_jondonymProxy.getMixCascade().getName() + " (" + (isConnected() ? "connected, " +
							"auto-switch:" + ((isCascadeAutoSwitched() ? "ON" : "OFF")) : "disconnected") + ")");
					  
		            System.out.println("Memory usage: " + 
		            		JAPUtil.formatBytesValueWithUnit(Runtime.getRuntime().totalMemory()) + ", Max VM memory: " +
	
							 JAPUtil.formatBytesValueWithUnit(Runtime.getRuntime().maxMemory()) + ", Free memory: " +
							 JAPUtil.formatBytesValueWithUnit(Runtime.getRuntime().freeMemory()));
				}
			}			
			
		}
		catch (Exception a_e)
		{
			LogHolder.log(LogLevel.EXCEPTION, LogType.MISC, a_e);
		}
	}
	
	public static synchronized void stop()
	{
		if (ms_jondonymProxy != null)
		{
			ms_jondonymProxy.stop();
		}
		if (ms_socketListener != null)
		{
			try 
			{
				ms_socketListener.close();
			} 
			catch (IOException a_e) 
			{
				LogHolder.log(LogLevel.EXCEPTION, LogType.NET, a_e);
			}
			ms_socketListener = null;
		}
	}
	
	public static synchronized void switchCascade(MixCascade a_cascade)
	{
		if (a_cascade == null)
		{
			return;
		}
		if (ms_jondonymProxy != null && ms_serviceContainer != null)
		{
			ms_serviceContainer.setCurrentCascade(a_cascade);
			ms_jondonymProxy.stop();
			ms_jondonymProxy.start(ms_serviceContainer);
		}
	}
	
	public static synchronized void setCascadeAutoSwitched(boolean a_bAutoSwitch)
	{
		if (ms_serviceContainer != null)
		{
			ms_serviceContainer.setServiceAutoSwitched(a_bAutoSwitch);
		}
	}
	
	public static synchronized boolean isCascadeAutoSwitched()
	{
		if (ms_serviceContainer != null)
		{
			return ms_serviceContainer.isServiceAutoSwitched();
		}
		return true;
	}
	
	public static synchronized Vector getAvailableCascades()
	{
		return Database.getInstance(MixCascade.class).getEntryList();
	}
	
	public static synchronized MixCascade getCurrentCascade()
	{
		if (ms_jondonymProxy == null)
		{
			return null;
		}
		return ms_jondonymProxy.getMixCascade();
	}
	
	public static synchronized boolean isRunning()
	{
		return ms_socketListener != null;
	}
	
	public static synchronized boolean isConnected()
	{
		return isRunning() && ms_jondonymProxy != null && ms_jondonymProxy.isConnected();
	}
	
	public static synchronized void start()
	{
		if (ms_socketListener != null)
		{
			return;
		}
		
		try
		{
			ms_socketListener = new ServerSocket(4001);
			
	        ms_isUpdater.start(true);
	        ms_isUpdater.update();
	        ms_paymentUpdater.start(true);
	        ms_paymentUpdater.update();
	        ms_cascadeUpdater.start(true);
	        ms_cascadeUpdater.update();
	         
	        MixCascade cascade;
	        if (ms_jondonymProxy == null || ms_jondonymProxy.getMixCascade() == null)
	        {
		        cascade = (MixCascade)Database.getInstance(MixCascade.class).getRandomEntry();
	        }
	        else
	        {
	        	cascade = ms_jondonymProxy.getMixCascade();
	        }
	       
	        if (cascade == null)
	        {
	        	// cascade = new MixCascade("mix.inf.tu-dresden.de", 6544);
	        	cascade = new MixCascade("none", 6544);
	        }
	        
	        ms_jondonymProxy = new AnonProxy(ms_socketListener, null,null);
	        ms_jondonymProxy.setDummyTraffic(DummyTrafficControlChannel.DT_MAX_INTERVAL_MS);
	        ms_serviceContainer = new AutoSwitchedMixCascadeContainer(cascade);
	        ms_jondonymProxy.start(ms_serviceContainer);
		}
		catch (Exception e)
		{
			LogHolder.log(LogLevel.EXCEPTION, LogType.MISC, e);
			stop();
		}
	}
	
	
	private static class AutoSwitchedMixCascadeContainer extends AbstractAutoSwitchedMixCascadeContainer
	{
		private boolean m_bAutoSwitched = true;
		public AutoSwitchedMixCascadeContainer(MixCascade a_cascade)
		{
			super(false, a_cascade);
		}

		public boolean isPaidServiceAllowed()
		{
			return false;
		}
		
		public void setServiceAutoSwitched(boolean a_bAutoSwitched)
		{
			m_bAutoSwitched = a_bAutoSwitched;
		}
		
		public boolean isServiceAutoSwitched()
		{
			return m_bAutoSwitched;
		}
		
		public boolean isReconnectedAutomatically()
		{
			return true;
		}
		
		public ITermsAndConditionsContainer getTCContainer()
		{
			return null;
		}
	}	
	
	private static class MixCascadeUpdater extends AbstractMixCascadeUpdater
	{
		public MixCascadeUpdater(ObservableInfo a_observableInfo)
		{
			super(a_observableInfo);
		}

		protected AbstractDatabaseEntry getPreferredEntry()
		{
			return null; // set current cascade here!
		}

		protected void setPreferredEntry(AbstractDatabaseEntry a_preferredEntry)
		{
			// do nothing
		}
	}
}
