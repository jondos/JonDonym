package infoservice;

import java.io.File;
import java.io.IOException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import anon.infoservice.InfoServiceHolder;
import anon.util.Updater;
import anon.util.XMLParseException;
import anon.util.XMLUtil;
import jap.InfoServiceUpdater;
import jap.JAPController;
import jap.JAPModel;
import jap.PerformanceInfoUpdater;
import jap.PassiveInfoServiceCascadeUpdater;
import jap.PassiveInfoServiceMainUpdater;
import jap.PassiveInfoServiceStatusUpdater;
import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;

/**
 * A wrapper class for InfoService that encapsules Updater threads 
 * normally used by JonDo. 
 * This module is used by InfoServices in passive mode.
 * In this mode the InfoService does not propagate itself to other InfoServices and does not push 
 * its entries towards other InfoServices. Instead it requests  
 * status, InfoService, cascade and performance entries from other InfoServices.
 * 
 * @author Simon Pecher
 *
 */
public class PassiveInfoServiceInitializer 
{
	private final static String LOAD_ERROR = "Could not load the cachefile for the passive InfoService";
	private final static String PARSE_ERROR = "Could not parse the cachefile for the passive InfoService";
	
	public static String CACHE_FILE_NAME = "cache.xml";
	
	private static InfoServiceUpdater infoServiceUpdater = null; /** Handler of infoservice entries */
	private static PassiveInfoServiceStatusUpdater statusUpdater = null; /** Handler of status entries */
	private static PassiveInfoServiceCascadeUpdater cascadeUpdater = null; /** Handler of mixcascade entries */
	private static PassiveInfoServiceGlobalUpdater globalUpdater = null;
	
	private final static int GLOBAL_UPDATE_INTERVAL = 60000;
	
	public static synchronized void init()
	{
		Document doc = null;
		try 
		{
			doc = XMLUtil.readXMLDocument(new File(CACHE_FILE_NAME));
			Element infoServiceRoot = null;
			if(doc != null)
			{
				infoServiceRoot = doc.getDocumentElement();
				if(infoServiceRoot != null)
				{
					try 
					{
						InfoServiceHolder.getInstance().loadSettingsFromXml(infoServiceRoot, false);
					} 
					catch (Exception e) 
					{
						LogHolder.log(LogLevel.ERR, LogType.MISC, PARSE_ERROR, e);
					}
				}
			}
		} 
		catch (IOException e) 
		{
			LogHolder.log(LogLevel.INFO, LogType.MISC, LOAD_ERROR, e);
			
		} 
		catch (XMLParseException e) 
		{
			LogHolder.log(LogLevel.INFO, LogType.MISC, PARSE_ERROR, e);
			
		}
		infoServiceUpdater = new InfoServiceUpdater(); /** Handler of infoservice entries */
		statusUpdater = new PassiveInfoServiceStatusUpdater(); /** Handler of status entries */
		globalUpdater = new PassiveInfoServiceGlobalUpdater();
		
		infoServiceUpdater.start(false);
		statusUpdater.start(false);
		globalUpdater.start();
	}
	
	public static synchronized void shutdown()
	{
		globalUpdater.interrupt();
	}
	
	private static void cacheDB()
	{
		System.out.println("TODO: cache the DB");
		/*Document doc = XMLUtil.createDocument();
		InfoServiceHolder.getInstance().toXmlElement(doc);
		
		File cacheFile = new File(PassiveInfoServiceInitializer.CACHE_FILE_NAME);
		try 
		{
			XMLUtil.write(doc, cacheFile);
		} 
		catch (IOException e) 
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}*/
	}
	
	/**
	 * invokes tghe Main updater and after that caches the Database
	 * this makes sure that the most recent data is cached.
	 */
	static class PassiveInfoServiceGlobalUpdater extends Thread
	{
		private static PassiveInfoServiceMainUpdater mainUpdater = null;
		
		PassiveInfoServiceGlobalUpdater()
		{
			mainUpdater = new PassiveInfoServiceMainUpdater();
		}
		
		public void run()
		{
			try
			{
			while(true)
			{
				mainUpdater.update();
				cacheDB();
				Thread.sleep(GLOBAL_UPDATE_INTERVAL);
			}
			}
			catch(InterruptedException ie)
			{
				//Global Updater is finished!
			}
			
		}
	}
}
