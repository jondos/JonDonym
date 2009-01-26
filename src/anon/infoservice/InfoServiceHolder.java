/*
 Copyright (c) 2000 - 2005, The JAP-Team
 All rights reserved.
 Redistribution and use in source and binary forms, with or without modification,
 are permitted provided that the following conditions are met:

  - Redistributions of source code must retain the above copyright notice,
 this list of conditions and the following disclaimer.

  - Redistributions in binary form must reproduce the above copyright notice,
 this list of conditions and the following disclaimer in the documentation and/or
 other materials provided with the distribution.

  - Neither the name of the University of Technology Dresden, Germany nor the names of its contributors
 may be used to endorse or promote products derived from this software without specific
 prior written permission.


 THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS ``AS IS'' AND ANY EXPRESS
 OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY
 AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE REGENTS OR CONTRIBUTORS
 BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA,
 OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER
 IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
 OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE
 */
package anon.infoservice;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Observable;
import java.util.Random;
import java.util.Vector;
import anon.util.ThreadPool;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import anon.crypto.SignatureVerifier;
import anon.pay.PaymentInstanceDBEntry;
import anon.util.ClassUtil;
import anon.util.IXMLEncodable;
import anon.util.Util;
import anon.util.XMLParseException;
import anon.util.XMLUtil;
import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;

/**
 * This class holds the instances of the InfoService class for the JAP client and is a singleton.
 * The instance of this class is observable and will send a notification with an
 * InfoServiceHolderMessage, if the preferred InfoService or the InfoService management policy
 * were changed.
 */
public class InfoServiceHolder extends Observable implements IXMLEncodable
{
	/**
	 * Stores the name of the root node of the XML settings for this class.
	 */
	public static final String XML_ELEMENT_NAME = "InfoserviceManagement";
	public static final String XML_ELEM_CHANGE_INFO_SERVICES = "ChangeInfoService";
	public static final int MAXIMUM_OF_ASKED_INFO_SERVICES = 10;
	public static final int DEFAULT_OF_ASKED_INFO_SERVICES = 4;

	/**
	 * Function number for fetchInformation() - getMixCascades().
	 */
	private static final int GET_MIXCASCADES = 1;

	/**
	 * Function number for fetchInformation() - getInfoServices().
	 */
	private static final int GET_INFOSERVICES = 2;

	/**
	 * Function number for fetchInformation() - getMixInfo().
	 */
	private static final int GET_MIXINFO = 3;

	/**
	 * Function number for fetchInformation() - getStatusInfo().
	 */
	private static final int GET_STATUSINFO = 4;

	/**
	 * Function number for fetchInformation() - getNewVersionNumber().
	 */
	private static final int GET_NEWVERSIONNUMBER = 5;

	/**
	 * Function number for fetchInformation() - getJAPVersionInfo().
	 */
	private static final int GET_JAPVERSIONINFO = 6;

	/**
	 * Function number for fetchInformation() - getTorNodesList().
	 */
	private static final int GET_TORNODESLIST = 7;

	/**
	 * Function number for fetchInformation() - getForwarder().
	 */
	private static final int GET_FORWARDER = 8;

	/**
	 * Function number for fetchInformation() - getPaymentInstances().
	 */
	private static final int GET_PAYMENT_INSTANCES = 9;

	/**
	 * Function number for fetchInformation() - getPaymentInstance().
	 */
	private static final int GET_PAYMENT_INSTANCE = 10;

	/**
	 * Function number for fetchInformation() - getMixminionNodesList().
	 */
	private static final int GET_MIXMINIONNODESLIST = 11;

	private static final int GET_CASCADEINFO = 12;

	private static final int GET_LATEST_JAVA = 13;

	private static final int GET_INFOSERVICE_SERIALS = 14;
	private static final int GET_MIXCASCADE_SERIALS = 15;
	private static final int GET_MESSAGES = 16;
	private static final int GET_LATEST_JAVA_SERIALS = 17;
	private static final int GET_MESSAGE_SERIALS = 18;

	private static final int GET_STATUSINFO_TIMEOUT = 19;
	private static final int GET_PERFORMANCE_INFO = 20;
	
	private static final int GET_TC_FRAMEWORK = 21;
	
	private static final int GET_TCS = 22;
	private static final int GET_TC_SERIALS = 23;
	
	private static final int GET_EXIT_ADDRESSES = 24;
	
	/**
	 * Function number for fetchInformation() - getMixInfo().
	 */
	private static final int GET_MIXINFOS = 25;

	/**
	 * This defines, whether there is an automatic change of infoservice after failure as default.
	 */
	public static final boolean DEFAULT_INFOSERVICE_CHANGES = true;

	private static final String XML_ATTR_ASKED_INFO_SERVICES = "askedInfoservices";

	/**
	 * Stores the instance of InfoServiceHolder (Singleton).
	 */
	private static InfoServiceHolder ms_infoServiceHolderInstance = null;

	/**
	 * Allows only 3 concurrent update operations.
	 */
	private ThreadPool m_poolFetchInformation = new ThreadPool("Fetch Information Thread Pool", 10, Thread.MIN_PRIORITY);

	/**
	 * Stores the preferred InfoService. This InfoService is asked first for every information.
	 */
	private InfoServiceDBEntry m_preferredInfoService;

	/**
	 * Stores, whether there is an automatic change of infoservice after failure. If this value is
	 * set to false, only the preferred infoservice is used.
	 */
	private boolean m_changeInfoServices;

	private int m_nrAskedInfoServices = DEFAULT_OF_ASKED_INFO_SERVICES;

	/**
	 * This creates a new instance of InfoServiceHolder. This is only used for setting some
	 * values. Use InfoServiceHolder.getInstance() for getting an instance of this class.
	 */
	private InfoServiceHolder()
	{
		m_preferredInfoService = null;
		m_changeInfoServices = DEFAULT_INFOSERVICE_CHANGES;
	}

	/**
	 * Returns the instance of InfoServiceHolder (Singleton). If there is no instance,
	 * there is a new one created.
	 *
	 * @return The InfoServiceHolder instance.
	 */
	public static InfoServiceHolder getInstance()
	{
		synchronized (InfoServiceHolder.class)
		{
			if (ms_infoServiceHolderInstance == null)
			{
				ms_infoServiceHolderInstance = new InfoServiceHolder();
			}
		}
		return ms_infoServiceHolderInstance;
	}

	public void shutdown()
	{
		m_poolFetchInformation.shutdown();

	}

	/**
	 * Returns the name of the XML node used to store all settings of the InfoServiceHolder
	 * instance. This name can be used to find the XML node within a document when the settings
	 * shall be loaded.
	 *
	 * @return The name of the XML node created when storing the settings.
	 */
	public static String getXmlSettingsRootNodeName()
	{
		return XML_ELEMENT_NAME;
	}

	/**
	 * Sets the preferred InfoService. This InfoService is used every time we need data from an
	 * InfoService until there is an connection error. If we can't get a connection to any of the
	 * interfaces of this InfoService or if we get no or wrong data from this InfoService it is
	 * changed automatically.
	 *
	 * @param a_preferredInfoService The preferred InfoService.
	 */
	public synchronized void setPreferredInfoService(InfoServiceDBEntry a_preferredInfoService)
	{
		if (a_preferredInfoService != null)
		{
			/* also if m_preferredInfoService.equals(a_preferredInfoService), there is the possibility
			 * that some values of the infoservice, like listener interfaces or the name have been
			 * changed, so we always update the internal stored preferred infoservice
			 */
			m_preferredInfoService = a_preferredInfoService;
			setChanged();
			notifyObservers(new InfoServiceHolderMessage(InfoServiceHolderMessage.
				PREFERRED_INFOSERVICE_CHANGED, m_preferredInfoService));

			LogHolder.log(LogLevel.INFO, LogType.NET,
						  "Preferred InfoService is now: " + m_preferredInfoService.getName());
		}
	}

	/**
	 * Returns the preferred InfoService. This InfoService is used every time we need data from an
	 * InfoService until there is an connection error. If we can't get a connection to any of the
	 * interfaces of this InfoService or if we get no or wrong data from this InfoService it is
	 * changed automatically.
	 *
	 * @return The preferred InfoService or null, if no preferred InfoService is set.
	 */
	public InfoServiceDBEntry getPreferredInfoService()
	{
		return m_preferredInfoService;
	}

	public int getNumberOfAskedInfoServices()
	{
		return m_nrAskedInfoServices;
	}

	public void setNumberOfAskedInfoServices(int a_nrAskedInfoServices)
	{
		if (a_nrAskedInfoServices < 1)
		{
			m_nrAskedInfoServices = 1;
		}
		else if (a_nrAskedInfoServices > MAXIMUM_OF_ASKED_INFO_SERVICES)
		{
			a_nrAskedInfoServices = MAXIMUM_OF_ASKED_INFO_SERVICES;
		}
		else
		{
			m_nrAskedInfoServices = a_nrAskedInfoServices;
		}
	}

	/**
	 * Sets, whether there is an automatic change of infoservice after failure. If this value is
	 * set to false, only the preferred infoservice is used.
	 *
	 * @param a_changeInfoServices Whether there are automatic changes of the infoservice.
	 */
	public void setChangeInfoServices(boolean a_changeInfoServices)
	{
		synchronized (this)
		{
			if (m_changeInfoServices != a_changeInfoServices)
			{
				m_changeInfoServices = a_changeInfoServices;
				setChanged();
				notifyObservers(new InfoServiceHolderMessage(InfoServiceHolderMessage.
					INFOSERVICE_MANAGEMENT_CHANGED, new Boolean(m_changeInfoServices)));
			}
		}
	}

	/**
	 * Returns, whether there is an automatic change of infoservice after failure. If this value is
	 * set to false, only the preferred infoservice is used for requests.
	 *
	 * @return Whether there are automatic changes of the infoservice.
	 */
	public boolean isChangeInfoServices()
	{
		boolean r_changeInfoServices = true;
		synchronized (this)
		{
			r_changeInfoServices = m_changeInfoServices;
		}
		return r_changeInfoServices;
	}

	/**
	 * Returns a Vector of InfoServices with all known infoservices (including the preferred
	 * infoservice), which have a forwarder list.
	 *
	 * @return The Vector of all known infoservices with a forwarder list, maybe this Vector is
	 *         empty.
	 */
	public Vector getInfoservicesWithForwarderList()
	{
		Vector primaryInfoServices = new Vector();
		/* check the preferred infoservice */
		InfoServiceDBEntry currentPreferredInfoService = getPreferredInfoService();
		if (currentPreferredInfoService.hasPrimaryForwarderList() == true)
		{
			primaryInfoServices.addElement(currentPreferredInfoService);
		}
		Enumeration infoservices = Database.getInstance(InfoServiceDBEntry.class).getEntryList().elements();
		while (infoservices.hasMoreElements())
		{
			InfoServiceDBEntry currentInfoService = (InfoServiceDBEntry) (infoservices.nextElement());
			if (currentInfoService.hasPrimaryForwarderList())
			{
				if (currentInfoService.getId().equals(currentPreferredInfoService.getId()) == false)
				{
					/* we have already the preferred infoservice in the list -> only add other infoservices */
					primaryInfoServices.addElement(currentInfoService);
				}
			}
		}
		return primaryInfoServices;
	}

	private class InformationFetcher implements Runnable
	{
		private int functionNumber;
		private Vector arguments;
		private Object m_result;

		public InformationFetcher(int a_functionNumber, Vector a_arguments)
		{
			functionNumber = a_functionNumber;
			arguments = a_arguments;
		}

		public Object getResult()
		{
			return m_result;
		}

		public void run()
		{
			InfoServiceDBEntry currentInfoService = null;
			Random random = new Random(System.currentTimeMillis());
			int askInfoServices = 1;
			currentInfoService = getPreferredInfoService();
			Vector infoServiceList = null;
			if (m_changeInfoServices)
			{
				/* get the whole infoservice list */
				infoServiceList = Database.getInstance(InfoServiceDBEntry.class).getEntryList();
				
				Vector copyList = (Vector)infoServiceList.clone();
				InfoServiceDBEntry isTemp;
				
				for (int i = 0; i <  copyList.size(); i++)
				{
					isTemp = (InfoServiceDBEntry)copyList.elementAt(i);
					if ((isTemp.getCertPath() != null && (!isTemp.getCertPath().isVerified()) ||
						(SignatureVerifier.getInstance().isCheckSignatures() &&
								SignatureVerifier.getInstance().isCheckSignatures(
									SignatureVerifier.DOCUMENT_CLASS_INFOSERVICE) && !isTemp.isValid())))
					{
						infoServiceList.removeElement(isTemp);
					}
				}
			}
			else
			{
				/* use an empty list -> only preferred infoservice is used */
				infoServiceList = new Vector();
				if (currentInfoService != null)
				{
					infoServiceList.addElement(currentInfoService);
				}
			}

			Object result = null;
			/**
			 * @todo This is a first hack for the fact that not only one IS should be asked but
			 * a lot of IS...
			 */
			if (functionNumber == GET_INFOSERVICES || functionNumber == GET_MIXCASCADES
				|| functionNumber == GET_MIXINFOS 
				|| functionNumber == GET_INFOSERVICE_SERIALS || functionNumber == GET_MIXCASCADE_SERIALS ||
				functionNumber == GET_CASCADEINFO || functionNumber == GET_LATEST_JAVA_SERIALS ||
				functionNumber == GET_LATEST_JAVA || functionNumber == GET_MESSAGES ||
				functionNumber == GET_MESSAGE_SERIALS || functionNumber == GET_PAYMENT_INSTANCES ||
				functionNumber == GET_PERFORMANCE_INFO || 
				functionNumber == GET_TCS || functionNumber == GET_TC_SERIALS ||
				functionNumber == GET_EXIT_ADDRESSES)
			{
				result = new Hashtable();
				//if (functionNumber == GET_CASCADEINFO)
				{
					// example: enter number of asked IS here, or keep default of 1
				}
				//else
				{
					// try up to a certain maximum of InfoServices
					if (MAXIMUM_OF_ASKED_INFO_SERVICES == m_nrAskedInfoServices)
					{
						// ask all InfoServices
						askInfoServices = infoServiceList.size() + 1;
					}
					else
					{
						askInfoServices = m_nrAskedInfoServices;
					}
				}
			}

			if (functionNumber == GET_STATUSINFO || functionNumber == GET_STATUSINFO_TIMEOUT)
			{
				/*
				 * For performance reasons the requests must be distributed equally over all InfoServices.
				 * Therefore the default InfoService is ignored.
				 */
				currentInfoService = null;
			}
			while (((infoServiceList.size() > 0) || (currentInfoService != null)) &&
				   !Thread.currentThread().isInterrupted())
			{
				if (currentInfoService == null)
				{
					/* randomly take a new one from the list */
					currentInfoService = (InfoServiceDBEntry) (infoServiceList.elementAt(
						Math.abs(random.nextInt()) % infoServiceList.size()));
				}

				if ((currentInfoService.getCertPath() != null && !currentInfoService.getCertPath().isVerified()))// ||
					//(SignatureVerifier.getInstance().isCheckSignatures() && !currentInfoService.isValid()))
				{
					LogHolder.log(LogLevel.NOTICE, LogType.NET,
								  "Skipped non-verifyable InfoService: " + currentInfoService.getName(), true);
					infoServiceList.removeElement(currentInfoService);
					currentInfoService = null;
					continue;
				}
				
				//if (functionNumber == GET_LATEST_JAVA_SERIALS)
				{
					LogHolder.log(LogLevel.NOTICE, LogType.NET,
								  "Trying InfoService: " + currentInfoService.getName(), true);
				}
				try
				{
					Hashtable tempHashtable = null;

					/* try to get the information from currentInfoService */
					if (functionNumber == GET_MIXCASCADES)
					{
						tempHashtable = currentInfoService.getMixCascades();
						if(arguments != null)
						{
							/* a service context (service environment) is specified 
							 * remove all elements which does not match our context 
							 */
							String context = (String) arguments.firstElement();
							filterServiceContext(tempHashtable, context);
						}
					}
					else if (functionNumber == GET_INFOSERVICES)
					{
						tempHashtable = currentInfoService.getInfoServices();
					}
					else if (functionNumber == GET_MIXINFOS)
					{
						tempHashtable = currentInfoService.getMixes(true);
					}
					else if (functionNumber == GET_MIXINFO)
					{
						result = currentInfoService.getMixInfo( (String) (arguments.elementAt(0)));
					}
					else if (functionNumber == GET_LATEST_JAVA)
					{
						tempHashtable = currentInfoService.getLatestJava();
					}
					else if (functionNumber == GET_LATEST_JAVA_SERIALS)
					{
						tempHashtable = currentInfoService.getLatestJavaSerials();
					}
					else if (functionNumber == GET_TC_FRAMEWORK)
					{
						result = currentInfoService.getTCFramework((String) (arguments.elementAt(0)));
					}
					else if (functionNumber == GET_TCS)
					{
						tempHashtable = currentInfoService.getTermsAndConditions();
					}
					else if (functionNumber == GET_TC_SERIALS)
					{
						tempHashtable = currentInfoService.getTermsAndConditionSerials();
					}
					else if (functionNumber == GET_PERFORMANCE_INFO)
					{
						AbstractDatabaseEntry dbEntry = currentInfoService.getPerformanceInfo();
						tempHashtable = new Hashtable();
						if (dbEntry != null)
						{
							tempHashtable.put(dbEntry.getId(), dbEntry);
						}
					}
					else if (functionNumber == GET_MESSAGES)
					{
						tempHashtable = currentInfoService.getMessages();
					}
					else if (functionNumber == GET_MESSAGE_SERIALS)
					{
						tempHashtable = currentInfoService.getMessageSerials();
					}
					else if (functionNumber == GET_STATUSINFO)
					{
						result = currentInfoService.getStatusInfo( (MixCascade) arguments.elementAt(0));
					}
					else if (functionNumber == GET_STATUSINFO_TIMEOUT)
					{
						result = currentInfoService.getStatusInfo( (MixCascade) arguments.elementAt(0),
							( (Long) arguments.elementAt(1)).longValue());
					}
					else if (functionNumber == GET_MIXCASCADE_SERIALS)
					{
						tempHashtable = currentInfoService.getMixCascadeSerials();
						if(arguments != null)
						{
							/* a service context (service environment) is specified 
							 * remove all elements which does not match our context 
							 */
							String context = (String) arguments.firstElement();
							filterServiceContext(tempHashtable, context);
						}
					}
					else if (functionNumber == GET_INFOSERVICE_SERIALS)
					{
						tempHashtable = currentInfoService.getInfoServiceSerials();
					}
					else if (functionNumber == GET_NEWVERSIONNUMBER)
					{
						result = currentInfoService.getNewVersionNumber();
					}
					else if (functionNumber == GET_JAPVERSIONINFO)
					{
						result = currentInfoService.getJAPVersionInfo( ( (Integer) (arguments.elementAt(0))).
							intValue());
					}
					else if (functionNumber == GET_TORNODESLIST)
					{
						result = currentInfoService.getTorNodesList();
					}
					else if (functionNumber == GET_MIXMINIONNODESLIST)
					{
						result = currentInfoService.getMixminionNodesList();
					}
					else if (functionNumber == GET_FORWARDER)
					{
						result = currentInfoService.getForwarder();
					}
					else if (functionNumber == GET_PAYMENT_INSTANCES)
					{
						tempHashtable = currentInfoService.getPaymentInstances();
					}
					else if (functionNumber == GET_PAYMENT_INSTANCE)
					{
						result = currentInfoService.getPaymentInstance( (String) arguments.firstElement());
					}
					else if (functionNumber == GET_EXIT_ADDRESSES)
					{
						// TODO ask more than one infoservice
						result = currentInfoService.getExitAddresses();
					}
					else if (functionNumber == GET_CASCADEINFO)
					{
						AbstractDatabaseEntry dbEntry =
							currentInfoService.getMixCascadeInfo( (String) arguments.firstElement());
						tempHashtable = new Hashtable();
						if (dbEntry != null)
						{
							tempHashtable.put(dbEntry.getId(), dbEntry);
						}
					}

					if ( (tempHashtable == null && result == null) ||
						(tempHashtable != null && tempHashtable.size() == 0))
					{
						LogHolder.log(LogLevel.INFO, LogType.NET,
									  "IS " + currentInfoService.getName() + " did not have the requested info!");
						infoServiceList.removeElement(currentInfoService);
						currentInfoService = null;
						continue;
					}
					else if (tempHashtable != null)
					{
						Enumeration newEntries = ( (Hashtable) tempHashtable).elements();
						AbstractDatabaseEntry currentEntry;
						AbstractDatabaseEntry hashedEntry;
						AbstractDistributableDatabaseEntry.SerialDBEntry currentSerialEntry, hashedSerialEntry;
						while (newEntries.hasMoreElements())
						{
							currentEntry = (AbstractDatabaseEntry) newEntries.nextElement();
							if ( ( (Hashtable) result).containsKey(currentEntry.getId()))
							{
								hashedEntry =
									(AbstractDatabaseEntry) ( (Hashtable) result).get(currentEntry.getId());

								if (currentEntry instanceof AbstractDistributableDatabaseEntry.SerialDBEntry &&
									hashedEntry instanceof AbstractDistributableDatabaseEntry.SerialDBEntry)
								{
									currentSerialEntry =
										(AbstractDistributableDatabaseEntry.SerialDBEntry) currentEntry;
									hashedSerialEntry =
										(AbstractDistributableDatabaseEntry.SerialDBEntry) hashedEntry;
									if (currentSerialEntry.getVersionNumber() !=
										hashedSerialEntry.getVersionNumber())
									{
										LogHolder.log(LogLevel.WARNING, LogType.NET,
													  "InfoServices report different serial numbers for " +
													  currentSerialEntry.getId() + "!");
										/**
										 * Alert: Two or more InfoServices report different version numbers.
										 * This could be a try to keep the caller from updating this entry.
										 * Mark this serial entry, so that the caller knows he must update this
										 * entry.
										 */
										currentSerialEntry = new AbstractDistributableDatabaseEntry.SerialDBEntry(
											currentSerialEntry.getId(), 0, Long.MAX_VALUE, // force update of hash
											currentSerialEntry.isVerified(), currentSerialEntry.isValid(), 
											currentSerialEntry.getContext());
									}

									if (currentSerialEntry.isVerified() != hashedSerialEntry.isVerified())
									{
										LogHolder.log(LogLevel.WARNING, LogType.NET,
													  "InfoServices report different verification status for " +
													  ClassUtil.getShortClassName(currentEntry.getClass()) + 
													  " with id " +
													  currentSerialEntry.getId() + "!");
										/**
										 * This may only be used for filtering if allInfoServices think this entry
										 * is unverified.
										 * If at least one IS reports it as verified, it must not be filtered.
										 */
										currentSerialEntry = new AbstractDistributableDatabaseEntry.SerialDBEntry(
											currentSerialEntry.getId(), currentSerialEntry.getVersionNumber(),
											Long.MAX_VALUE, true, currentSerialEntry.isValid(), 
											currentSerialEntry.getContext());
									}

									if (currentSerialEntry.isValid() != hashedSerialEntry.isValid())
									{
										LogHolder.log(LogLevel.WARNING, LogType.NET,
													  "InfoServices report different validity status for " +
													  currentSerialEntry.getId() + "!");
										/**
										 * This may only be used for filtering if allInfoServices think this entry
										 * is invalid.
										 * If at least one IS reports it as valid, it must not be filtered.
										 */
										currentSerialEntry = new AbstractDistributableDatabaseEntry.SerialDBEntry(
											currentSerialEntry.getId(), currentSerialEntry.getVersionNumber(),
											Long.MAX_VALUE, currentSerialEntry.isVerified(), true, 
											currentSerialEntry.getContext());
									}
									currentEntry = currentSerialEntry;
								}

								if (hashedEntry.getLastUpdate() > currentEntry.getLastUpdate())
								{
									continue;
								}
							}
							( (Hashtable) result).put(currentEntry.getId(), currentEntry);
						}

						askInfoServices--;
						if (askInfoServices == 0)
						{
							break;
						}
						infoServiceList.removeElement(currentInfoService);
						currentInfoService = null;
						continue;
					}
					break;
				}
				catch (Exception e)
				{
					LogHolder.log(LogLevel.INFO, LogType.NET,
								  "Contacting IS " + currentInfoService.getName() + " produced an error!", e);
					/* if there was an error, remove currentInfoService from the list and try another
					 * infoservice
					 */
					infoServiceList.removeElement(currentInfoService);
					currentInfoService = null;
				}
			}

			if (result != null && (! (result instanceof Hashtable) || ( (Hashtable) result).size() > 0))
			{
				if (functionNumber == GET_CASCADEINFO)
				{
					result = ( (Hashtable) result).elements().nextElement();
				}
				m_result = result;
				return;
			}

			LogHolder.log(LogLevel.ERR, LogType.NET,
						  "No InfoService with the needed information (" + functionNumber + ") available.",
						  true);
			m_result = null;
		}
	}

	/**
	 * Fetches every information from the infoservices. If we can't get the information from the
	 * preferred infoservice, all other known infoservices are asked automatically until an
	 * infoservice has the information. If we can't get the information from any infoservice, an
	 * Exception is thrown.
	 *
	 * @param functionNumber Specifies the InfoService function to call. Look at the constants
	 *                       defined in this class.
	 * @param arguments If an InfoService function needs arguments, these are in here.
	 *
	 * @return The needed information.
	 */
	private Object fetchInformation(int functionNumber, Vector arguments)
	{
		InformationFetcher fetcher = new InformationFetcher(functionNumber, arguments);
		try
		{
			m_poolFetchInformation.addRequestAndWait(fetcher);
		}
		catch (InterruptedException ex)
		{
			Thread.currentThread().interrupt();
			LogHolder.log(LogLevel.ERR, LogType.THREAD, ex);
		}
		return fetcher.getResult();
	}

	/**
	 * Get a Vector of all mixcascades the preferred infoservice knows. If we can't get a the
	 * information from preferred infoservice, another known infoservice is asked. If we have gotten
	 * a list from one infoservice, we stop asking other infoservices, so information is not a
	 * cumulative list with information from more than one infoservice. If we can't get the
	 * information from any infoservice, null is returned.
	 *
	 * @return The Vector of mixcascades.
	 */
	public Hashtable getMixCascades()
	{
		return (Hashtable) (fetchInformation(GET_MIXCASCADES, null));
	}
	
	/** 
	 * same as getMixCascades but a service context that the cascades must match can be specfied.
	 * If null is specified the method returns all service objects
	 * @param context service context that the returned cascades must match
	 * @return all cascades that match the specified service context.
	 */
	public Hashtable getMixCascades(String context)
	{
		if(context == null)
		{
			return getMixCascades();
		}
		Vector args = new Vector();
		args.addElement(context); 
		return (Hashtable) (fetchInformation(GET_MIXCASCADES, args));
	}
	
	public Hashtable getMixCascadeSerials()
	{
		return (Hashtable) (fetchInformation(GET_MIXCASCADE_SERIALS, null));
	}
	
	/** 
	 * same as getMixCascadesSerials but a service context that the serials must match can be specified.
	 * If null is specified the method returns all service serials
	 * @param context service context that the returned cascades must match
	 * @return all cascade serials that match the specified service context.
	 */
	public Hashtable getMixCascadeSerials(String context)
	{
		if(context == null)
		{
			return getMixCascadeSerials();
		}
		Vector args = new Vector();
		args.addElement(context); 
		return (Hashtable) (fetchInformation(GET_MIXCASCADE_SERIALS, args));
	}
	
	/**
	 * from preferred info service
	 * @return
	 */
	public TermsAndConditionsFramework getTCFramework(String a_id)
	{
		return (TermsAndConditionsFramework) (fetchInformation(GET_TC_FRAMEWORK, Util.toVector(a_id)));
	}
	
	public Hashtable getTermsAndConditions()
	{
		return (Hashtable) (fetchInformation(GET_TCS, null));
	}
	
	public Hashtable getTermsAndConditionsSerials()
	{
		return (Hashtable) (fetchInformation(GET_TC_SERIALS, null));
	}
	
	/*
	 * Retrieves the PerformanceInfo object of ALL inforservices!
	 */
	public Hashtable getPerformanceInfos()
	{
		return (Hashtable) (fetchInformation(GET_PERFORMANCE_INFO, null));
	}
	
	public void getExitAddresses()
	{
		fetchInformation(GET_EXIT_ADDRESSES, null);
	}

	/**
	 * Get a Vector of all payment instances the preferred infoservice knows. If we can't get a the
	 * information from preferred infoservice, another known infoservice is asked. If we have gotten
	 * a list from one infoservice, we stop asking other infoservices, so information is not a
	 * cumulative list with information from more than one infoservice. If we can't get the
	 * information from any infoservice, null is returned.
	 *
	 * @return The Vector of payment instances.
	 */
	public Hashtable getPaymentInstances()
	{
		return (Hashtable) (fetchInformation(GET_PAYMENT_INSTANCES, null));
	}

	/** Get information for a particular payment instance identified by a_piID
	 *
	 * @return Payment Instance information
	 */

	public PaymentInstanceDBEntry getPaymentInstance(String a_piID) throws Exception
	{
		return (PaymentInstanceDBEntry) (fetchInformation(GET_PAYMENT_INSTANCE, Util.toVector(a_piID)));
	}

	/**
	 * Get a Vector of all infoservices the preferred infoservice knows. If we can't get a the
	 * information from preferred infoservice, another known infoservice is asked. If we have gotten
	 * a list from one infoservice, we stop asking other infoservices, so information is not a
	 * cumulative list with information from more than one infoservice. If we can't get the
	 * information from any infoservice, null is returned.
	 *
	 * @return The Vector of infoservices.
	 */
	public Hashtable getInfoServices()
	{
		return (Hashtable) (fetchInformation(GET_INFOSERVICES, null));
	}

	public Hashtable getInfoServiceSerials()
	{
		return (Hashtable) (fetchInformation(GET_INFOSERVICE_SERIALS, null));
	}


	/**
	 * Get the MixInfo for the mix with the given ID. If we can't get a the information from
	 * preferred infoservice, another known infoservice is asked. If we can't get the information
	 * from any infoservice, null is returned. You should not call this method directly, better
	 * call the method in MixCascade to get the MixInfo.
	 *
	 * @param mixId The ID of the mix to get the MixInfo for.
	 *
	 * @return The MixInfo for the mix with the given ID.
	 */
	public MixInfo getMixInfo(String mixId)
	{
		return (MixInfo) (fetchInformation(GET_MIXINFO, Util.toVector(mixId)));
	}
	
	public Hashtable getMixInfos()
	{
		return (Hashtable)(fetchInformation(GET_MIXINFOS, null));
	}

	/**
	 * Get the StatusInfo for the mixcascade with the given ID. If we can't get a the information
	 * from preferred infoservice, another known infoservice is asked. If we can't get the
	 * information from any infoservice, null is returned. You should not call this method directly,
	 * better call the method in MixCascade to get the current status.
	 *
	 * @param cascadeId The ID of the mixcascade to get the StatusInfo for.
	 * @param cascadeLength The length of the mixcascade (number of mixes). We need this for
	 *                      calculating the AnonLevel in the StatusInfo.
	 *
	 * @return The current StatusInfo for the mixcascade with the given ID.
	 */
	public StatusInfo getStatusInfo(MixCascade a_cascade)
	{
		return (StatusInfo) (fetchInformation(GET_STATUSINFO, Util.toVector(a_cascade)));
	}

	public StatusInfo getStatusInfo(MixCascade a_cascade, long a_timeout)
	{
		Vector args = new Vector();
		args.addElement(a_cascade);
		args.addElement(new Long(a_timeout));

		return (StatusInfo) (fetchInformation(GET_STATUSINFO_TIMEOUT, args));
	}


	/**
	 * Get the version String of the current JAP version from the infoservice. This function is
	 * called to check, whether updates of the JAP are available. If we can't get a the information
	 * from preferred infoservice, another known infoservice is asked. If we can't get the
	 * information from any infoservice, null is returned.
	 *
	 * @return The version String (fromat: nn.nn.nnn) of the current JAP version or null if the
	 * version information could not be retrieved
	 */
	public JAPMinVersion getNewVersionNumber()
	{
		return (JAPMinVersion) (fetchInformation(GET_NEWVERSIONNUMBER, null));
	}

	/**
	 * Returns all known latests Java versions as JavaVersionDBEntry. If we can't get the information
	 * from any infoservice, null is returned.
	 *
	 * @return all known latests Java versions as JavaVersionDBEntry
	 */
	public Hashtable getLatestJavaVersions()
	{
		return (Hashtable) fetchInformation(GET_LATEST_JAVA, null);
	}

	public Hashtable getLatestJavaVersionSerials()
	{
		return (Hashtable) fetchInformation(GET_LATEST_JAVA_SERIALS, null);
	}


	public Hashtable getMessages()
	{
		return (Hashtable) fetchInformation(GET_MESSAGES, null);
	}

	public Hashtable getMessageSerials()
	{
		return (Hashtable) fetchInformation(GET_MESSAGE_SERIALS, null);
	}

	/**
	 * Returns the JAPVersionInfo for the specified type. The JAPVersionInfo is generated from
	 * the JNLP files received from the infoservice. If we can't get a the information from
	 * preferred infoservice, another known infoservice is asked. If we can't get the information
	 * from any infoservice, null is returned.
	 *
	 * @param japVersionType Selects the JAPVersionInfo (release / development). Look at the
	 *                       Constants in JAPVersionInfo.
	 *
	 * @return The JAPVersionInfo of the specified type.
	 */
	public JAPVersionInfo getJAPVersionInfo(int japVersionType)
	{
		return (JAPVersionInfo) fetchInformation(
			  GET_JAPVERSIONINFO, Util.toVector(new Integer(japVersionType)));
	}

	/**
	 * Get the list with the tor nodes from the infoservice. If we can't get a the information from
	 * preferred infoservice, another known infoservice is asked. If we can't get the information
	 * from any infoservice, null is returned.
	 *
	 * @return The raw tor nodes list as it is distributed by the tor directory servers.
	 */
	public byte[] getTorNodesList()
	{
		return (byte[]) (fetchInformation(GET_TORNODESLIST, null));
	}

	public MixCascade getMixCascadeInfo(String a_cascadeID)
	{
		return (MixCascade) (fetchInformation(GET_CASCADEINFO, Util.toVector(a_cascadeID)));
	}

	/**
	 * Get the list with the mixminion nodes from the infoservice. If we can't get a the information from
	 * preferred infoservice, another known infoservice is asked. If we can't get the information
	 * from any infoservice, null is returned.
	 *
	 * @return The raw mixminion nodes list as it is distributed by the mixminion directory servers.
	 */
	public byte[] getMixminionNodesList()
	{
		return (byte[]) (fetchInformation(GET_MIXMINIONNODESLIST, null));
	}

	/**
	 * Downloads a forwarder entry from a infoservice. If that infoservice has no forwarder list,
	 * it will ask another infoservice with such a list and returns the answer to us. If we can't
	 * get the information from preferred infoservice, another known infoservice is asked. If we
	 * can't get the information from any infoservice, null is returned.
	 *
	 * @return The JapForwarder node of the answer of the infoservice's getforwarder command.
	 */
	public Element getForwarder()
	{
		return (Element) (fetchInformation(GET_FORWARDER, null));
	}

	/**
	 * Returns all settings (including the database of known infoservices) as an XML node.
	 *
	 * @param a_doc The parent document for the created XML node.
	 *
	 * @return The settings of this instance of InfoServiceHolder as an XML node.
	 */
	public Element toXmlElement(Document a_doc)
	{
		Element infoServiceManagementNode = a_doc.createElement(XML_ELEMENT_NAME);
		Element infoServicesNode = Database.getInstance(InfoServiceDBEntry.class).toXmlElement(
			  a_doc, InfoServiceDBEntry.XML_ELEMENT_CONTAINER_NAME);
		Element preferredInfoServiceNode = a_doc.createElement("PreferredInfoService");
		Element changeInfoServicesNode = a_doc.createElement(XML_ELEM_CHANGE_INFO_SERVICES);
		XMLUtil.setAttribute(infoServiceManagementNode, XML_ATTR_ASKED_INFO_SERVICES, m_nrAskedInfoServices);
		synchronized (this)
		{
			InfoServiceDBEntry preferredInfoService = getPreferredInfoService();
			if (preferredInfoService != null)
			{
				preferredInfoServiceNode.appendChild(preferredInfoService.toXmlElement(a_doc));
			}
			XMLUtil.setValue(changeInfoServicesNode, isChangeInfoServices());
		}
		infoServiceManagementNode.appendChild(infoServicesNode);
		infoServiceManagementNode.appendChild(preferredInfoServiceNode);
		infoServiceManagementNode.appendChild(changeInfoServicesNode);
		return infoServiceManagementNode;
	}

	/**
	 * Restores the settings of this instance of InfoServiceHolder with the settings stored in the
	 * specified XML node.
	 *
	 * @param a_infoServiceManagementNode The XML node for loading the settings from. The name of
	 *                                    the needed XML node can be obtained by calling
	 *                                    getXmlSettingsRootNodeName().
	 * @param a_bForceISChange if automatic change if IS is forced
	 */
	public void loadSettingsFromXml(Element a_infoServiceManagementNode,
									boolean a_bForceISChange) throws Exception
	{
		setNumberOfAskedInfoServices(XMLUtil.parseAttribute(
			  a_infoServiceManagementNode, XML_ATTR_ASKED_INFO_SERVICES, DEFAULT_OF_ASKED_INFO_SERVICES));

		/* parse the whole InfoServiceManagement node */
		Element infoServicesNode = (Element) (XMLUtil.getFirstChildByName(a_infoServiceManagementNode,
			"InfoServices"));
		if (infoServicesNode == null)
		{
			throw (new Exception("No InfoServices node found."));
		}

		/* InfoServices node found -> load it into the database of known infoservices */
		Database.getInstance(InfoServiceDBEntry.class).loadFromXml(infoServicesNode);
		//InfoServiceDBEntry.loadFromXml(infoServicesNode, Database.getInstance(InfoServiceDBEntry.class));
		Element preferredInfoServiceNode = (Element) (XMLUtil.getFirstChildByName(a_infoServiceManagementNode,
			"PreferredInfoService"));
		if (preferredInfoServiceNode == null)
		{
			throw (new Exception("No PreferredInfoService node found."));
		}
		Element infoServiceNode = (Element) (XMLUtil.getFirstChildByName(preferredInfoServiceNode,
			"InfoService"));
		InfoServiceDBEntry preferredInfoService = null;
		if (infoServiceNode != null)
		{
			/* there is a preferred infoservice -> parse it */
			try
			{
				preferredInfoService = new InfoServiceDBEntry(infoServiceNode, Long.MAX_VALUE);
			}
			catch (XMLParseException a_e)
			{
			}
		}

		/* remove bootstrap entries is possible; at least three InfoServices have to be loaded, excluding default */
		Vector currentEntries = Database.getInstance(InfoServiceDBEntry.class).getEntryList();
		Vector bootstrapIDs = new Vector();
		int nrLoadedIS = 0;
		InfoServiceDBEntry entry;
		for (int i = 0; i < currentEntries.size(); i++)
		{
			entry = (InfoServiceDBEntry)currentEntries.elementAt(i);
			if (entry.isBootstrap())
			{
				bootstrapIDs.addElement(entry.getId());
			}
			else if (!entry.isUserDefined())
			{
				nrLoadedIS++;
			}
		}
		if (nrLoadedIS >= 3) // we need at least 3 InfoServices for some majority calculations
		{
			// remove all bootstrap entries
			for (int i = 0; i < bootstrapIDs.size(); i++)
			{
				Database.getInstance(InfoServiceDBEntry.class).remove(bootstrapIDs.elementAt(i).toString());
			}
		}
		
		
		synchronized (this)
		{
			/* we have collected all values -> set them */
			if (preferredInfoService != null)
			{
				setPreferredInfoService(preferredInfoService);
			}
			else if (getPreferredInfoService() == null)
			{
				setPreferredInfoService((InfoServiceDBEntry)Database.getInstance(InfoServiceDBEntry.class).getRandomEntry());
			}
			if (a_bForceISChange)
			{
				setChangeInfoServices(true);
			}
			else
			{
				Element changeInfoServicesNode =
					(Element) (XMLUtil.getFirstChildByName(a_infoServiceManagementNode,
					XML_ELEM_CHANGE_INFO_SERVICES));
				setChangeInfoServices(XMLUtil.parseValue(changeInfoServicesNode, isChangeInfoServices()));
			}
		}
	}
	
	/**
	 * helper function that filters service objects matching the specified
	 * service context. In case of a mismatch the service object will be removed
	 * from the specified serviceObjects table.
	 * @param serviceObjects table of service objects to be filtered
	 * @param context the service context that the service objects must match 
	 */
	private static void filterServiceContext(Hashtable serviceObjects, String context)
	{
		boolean removeEntry = false;
		if(context != null && serviceObjects != null)
		{
			String currentContext = null;
			try
			{
				for(Enumeration keys = serviceObjects.keys(); keys.hasMoreElements();)
				{
					Object currentKey = keys.nextElement();	
					IServiceContextContainer currentEntry = 
						(IServiceContextContainer) serviceObjects.get(currentKey);
					
					currentContext = currentEntry.getContext();
					removeEntry = (currentContext == null) ? 
									true : !currentContext.equals(context);
					if(removeEntry)
					{
						serviceObjects.remove(currentKey);
					}
				}
			}
			catch(ClassCastException cce)
			{
				LogHolder.log(LogLevel.ERR, LogType.MISC, "Wrong type for filter specified", cce);
			}
		}
	}

}
