/*
 Copyright (c) 2009, The JAP-Team, JonDos GmbH
 All rights reserved.
 Redistribution and use in source and binary forms, with or without modification,
 are permitted provided that the following conditions are met:

  - Redistributions of source code must retain the above copyright notice,
 this list of conditions and the following disclaimer.

  - Redistributions in binary form must reproduce the above copyright notice,
 this list of conditions and the following disclaimer in the documentation and/or
 other materials provided with the distribution.

  - Neither the name of the University of Technology Dresden, Germany, nor the name of
 the JonDos GmbH, nor the names of their contributors may be used to endorse or
 promote products derived from this software without specific prior written permission.


 THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS ``AS IS'' AND ANY EXPRESS
 OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY
 AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE REGENTS OR CONTRIBUTORS
 BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA,
 OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER
 IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
 OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE
 */
package infoservice;

import java.io.File;
import java.io.IOException;
import java.util.Observable;

import org.w3c.dom.Document;

import anon.infoservice.Database;
import anon.infoservice.InfoServiceDBEntry;
import anon.infoservice.InfoServiceHolder;
import anon.infoservice.MixCascade;
import anon.infoservice.MixCascadeExitAddresses;
import anon.infoservice.MixInfo;
import anon.infoservice.PerformanceInfo;
import anon.infoservice.StatusInfo;
import anon.infoservice.update.InfoServiceUpdater;
import anon.pay.PaymentInstanceDBEntry;
import anon.util.XMLParseException;
import anon.util.XMLUtil;
import anon.util.Updater.ObservableInfo;
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
		InfoServiceDBEntry.class, MixInfo.class, PaymentInstanceDBEntry.class
	};
	
	public static String CACHE_FILE_NAME = "cache.xml";
	
	private static InfoServiceUpdater infoServiceUpdater = null; /** Handler of infoservice entries */
	private static PassiveInfoServiceStatusUpdater statusUpdater = null; /** Handler of status entries */
	private static PassiveInfoServiceGlobalUpdater globalUpdater = null;
	
	private final static int GLOBAL_UPDATE_INTERVAL = 60000;
	private static final long UPDATE_SYNC_INTERVAL = 4000;
	
	public static synchronized void init() throws IOException
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
		
		ObservableInfo observableInfo = new ObservableInfo(new Observable())
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
		
		infoServiceUpdater = new InfoServiceUpdater(observableInfo); /** Handler of infoservice entries */
		statusUpdater = new PassiveInfoServiceStatusUpdater(observableInfo); /** Handler of status entries */
		globalUpdater = new PassiveInfoServiceGlobalUpdater(observableInfo);
		
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
	private static class PassiveInfoServiceGlobalUpdater extends Thread
	{
		
		private static PassiveInfoServiceMainUpdater mainUpdater = null;
		
		PassiveInfoServiceGlobalUpdater(ObservableInfo a_observableInfo) throws IOException
		{
			mainUpdater = new PassiveInfoServiceMainUpdater(
					!Configuration.getInstance().isPerfEnabled(), a_observableInfo);
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
