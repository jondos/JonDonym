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
import jap.PerformanceInfoUpdater;
import jap.PullServiceCascadeUpdater;
import jap.PullServicePerformanceEntryUpdater;
import jap.PullServiceStatusUpdater;
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
 * (in other a spooky hybrid of a JonDo and an InfoService : ) 
 * @author Simon Pecher
 *
 */
public class PullServiceInitializer 
{
	private final static String LOAD_ERROR = "Could not load the Infoservice file for the pull service";
	private final static String PARSE_ERROR = "Could not parse the Infoservice file for the pull service";
	private final static String INIT_INFOSERVICE_ERROR = "Could not initialize the InfoServices for the pull service";
	
	//the info requester classes
	final static Updater[] infoHUpdaters = 
		new Updater[]
	{
		new InfoServiceUpdater(), /** Handler of infoservice entries */
		new PullServiceStatusUpdater(), /** Handler of status entries */
		new PullServiceCascadeUpdater(), /** Handler of mixcascade entries */
		new PerformanceInfoUpdater(),
		new PullServicePerformanceEntryUpdater() /** Handler of performancedata entries */
	};
	
	public static void init()
	{
		//System.out.println("Loading default certificates");
		JAPController.addDefaultCertificates();
		//System.out.println("read in infoservices");
		Document doc = null;
		try 
		{
			//TODO: read this from the Infoservice property file
			doc = XMLUtil.readXMLDocument(new File("infoservices.xml"));
		} 
		catch (IOException e) 
		{
			LogHolder.log(LogLevel.ERR, LogType.MISC, LOAD_ERROR, e);
			throw new RuntimeException(e);
		} 
		catch (XMLParseException e) 
		{
			LogHolder.log(LogLevel.ERR, LogType.MISC, PARSE_ERROR, e);
			throw new RuntimeException(e);
		}
		if(doc == null)
		{
			LogHolder.log(LogLevel.ERR, LogType.MISC, LOAD_ERROR);
			throw new RuntimeException(LOAD_ERROR);
		}
		Element infoServiceRoot = doc.getDocumentElement();
		if(infoServiceRoot == null)
		{
			throw new RuntimeException(PARSE_ERROR);
		}
		try 
		{
			InfoServiceHolder.getInstance().loadSettingsFromXml(infoServiceRoot, false);
		} 
		catch (Exception e) //Only a generic Exception can be caught. The above method only "throws Exception".
		{
			LogHolder.log(LogLevel.ERR, LogType.MISC, INIT_INFOSERVICE_ERROR, e);
			throw new RuntimeException(e);
		}
		//System.out.println("initialize updaters");
		for (int i = 0; i < infoHUpdaters.length; i++) 
		{
			infoHUpdaters[i].start(true);
		}
	}
	
}
