package infoservice;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.w3c.dom.Document;

import anon.infoservice.Database;
import anon.infoservice.InfoServiceDBEntry;
import anon.infoservice.InfoServiceHolder;
import anon.infoservice.MixCascade;
import anon.infoservice.MixCascadeExitAddresses;
import anon.infoservice.PerformanceInfo;
import anon.infoservice.StatusInfo;
import anon.util.XMLParseException;
import anon.util.XMLUtil;
import jap.InfoServiceUpdater;
import jap.PassiveInfoServiceMainUpdater;
import jap.PassiveInfoServiceStatusUpdater;
import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;

/**
 * A wrapper class for InfoService that encapsulates Updater threads 
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
	private final static String CACHE_ERROR = "DB caching failed.";
	
	/* contains all the classes that needs to be cached 
	 * must implement IXMLEncodable*/
	private final static Class[] CACHE_CLASSES = new Class[]
	{                    
		MixCascade.class, MixCascadeExitAddresses.class, PerformanceInfo.class, StatusInfo.class,
		InfoServiceDBEntry.class
	};
	
	public static String CACHE_FILE_NAME = "cache.xml";
	
	private static InfoServiceUpdater infoServiceUpdater = null; /** Handler of infoservice entries */
	private static PassiveInfoServiceStatusUpdater statusUpdater = null; /** Handler of status entries */
	private static PassiveInfoServiceGlobalUpdater globalUpdater = null;
	
	private final static int GLOBAL_UPDATE_INTERVAL = 60000;
	private static final long UPDATE_SYNC_INTERVAL = 4000;
	
	public static synchronized void init() throws FileNotFoundException
	{
		Document doc = null;
		try 
		{
			File fileCache = new File(CACHE_FILE_NAME);
			if (fileCache.exists())
			{
				doc = XMLUtil.readXMLDocument(new File(CACHE_FILE_NAME));
				if(doc != null)
				{
					Database.restoreFromXML(doc, CACHE_CLASSES);	
				}
			}
		} 
		catch (IOException e) 
		{
			LogHolder.log(LogLevel.INFO, LogType.MISC, LOAD_ERROR, e);
			e.printStackTrace();
			
		} 
		catch (XMLParseException e) 
		{
			LogHolder.log(LogLevel.INFO, LogType.MISC, PARSE_ERROR, e);
			e.printStackTrace();
		}
		
		/* activate querying of all InfoServices */
		InfoServiceHolder.getInstance().setNumberOfAskedInfoServices(InfoServiceHolder.MAXIMUM_OF_ASKED_INFO_SERVICES);
		
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
		Document doc = Database.dumpToXML(CACHE_CLASSES);
		
		if(doc != null)
		{
			File cacheFile = new File(PassiveInfoServiceInitializer.CACHE_FILE_NAME);
			try 
			{
				XMLUtil.write(doc, cacheFile);
			} 
			catch (IOException ioe) 
			{	
				LogHolder.log(LogLevel.INFO, LogType.MISC, CACHE_ERROR, ioe);
			}
		}
		else
		{
			LogHolder.log(LogLevel.INFO, LogType.MISC, CACHE_ERROR);
		}
	}
	
	/**
	 * invokes the Main updater and after that caches the Database
	 * this makes sure that the most recent data is cached.
	 */
	static class PassiveInfoServiceGlobalUpdater extends Thread
	{
		
		private static PassiveInfoServiceMainUpdater mainUpdater = null;
		
		PassiveInfoServiceGlobalUpdater() throws FileNotFoundException
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
					/* wait short amount of time for asynchronous information fetcher operations ...*/
					Thread.sleep(UPDATE_SYNC_INTERVAL); 
					/* ... so it is likely that all informations are fetched when caching the DB */
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
