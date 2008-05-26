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
package infoservice;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import anon.infoservice.AbstractDatabaseEntry;
import anon.infoservice.Constants;
import anon.infoservice.Database;
import anon.infoservice.InfoServiceDBEntry;
import anon.infoservice.MixCascade;
import anon.infoservice.MixInfo;
import anon.infoservice.InfoServiceIDEntry;
import anon.pay.PaymentInstanceDBEntry;
import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;

/**
 * This is the implementation of the announcment of our own infoservice. It generates
 * periodicly (default: 10 minutes) a new InfoServiceDBEntry for our own infoservices and
 * propagates it to all other infoservices (via InfoServiceDatabase.update()).
 */
public class InfoServicePropagandist implements Runnable
{
	private static long ms_serialNumber;

	/**
	 * Stores whether ther is already an instance of InfoServicePropagandist running or not.
	 */
	private static boolean alreadyRunning = false;

	/**
	 * Generates an instance of InfoServicePropagandist if there isn't already one running.
	 */
	public static void generateInfoServicePropagandist()
	{
		if (alreadyRunning == false)
		{
			alreadyRunning = true;
			ms_serialNumber = System.currentTimeMillis();
			InfoServicePropagandist propaganda = new InfoServicePropagandist();
			Thread propagandist = new Thread(propaganda, "Propaganda Thread");
			propagandist.start();
		}
	}

	/**
	 * This is the propaganda thread, which propagates our infoservice to all neighbour
	 * infoservices every Constants.ANNOUNCE_PERIOD (default: 10 minutes).
	 */
	public void run()
	{
		// fetch all available InfoServices and MixCascades from the neighbour InfoServices
		Hashtable hashEntries;
		Enumeration enumNeighbours;
		Enumeration enumTmp;
		AbstractDatabaseEntry tmpEntry;
		AbstractDatabaseEntry idEntry;
		boolean bInit = true;

		while (true)
		{
			Vector virtualListeners = Configuration.getInstance().getVirtualListeners();
			if (virtualListeners.size() > 0)
			{
				for (int i = 0; i < 4; i++)
				{
					if (i == 1 && !bInit)
					{
						// update MixCascades and Mixes at startup only
						break;
					}

					hashEntries = new Hashtable();
					enumNeighbours = Database.getInstance(InfoServiceDBEntry.class).
						getEntrySnapshotAsEnumeration();
					while (enumNeighbours.hasMoreElements())
					{
						try
						{
							tmpEntry = (AbstractDatabaseEntry)enumNeighbours.nextElement();
							if (tmpEntry.getId().equals(Configuration.getInstance().getID()))
							{
								continue;
							}

							if (i == 0)
							{
								enumTmp = ( (InfoServiceDBEntry) tmpEntry).getInfoServices(false).elements();
							}
							else if (i == 1)
							{								
								enumTmp = ( (InfoServiceDBEntry) tmpEntry).getPaymentInstances(false).elements();
								
							}
							else if (i == 2)
							{
								enumTmp = ( (InfoServiceDBEntry) tmpEntry).getMixCascades(false).elements();
							}
							else
							{
								enumTmp = ( (InfoServiceDBEntry) tmpEntry).getMixes(false).elements();
							}
						}
						catch (Exception a_e)
						{
							// ignore
							enumTmp = new Hashtable().elements();
						}
						while (enumTmp.hasMoreElements())
						{
							tmpEntry = (AbstractDatabaseEntry) enumTmp.nextElement();
							hashEntries.put(tmpEntry.getId(), tmpEntry);
						}
					}
					enumTmp = hashEntries.elements();
					while (enumTmp.hasMoreElements())
					{
						tmpEntry = (AbstractDatabaseEntry) enumTmp.nextElement();
						if (i == 0)
						{
							idEntry = Database.getInstance(InfoServiceIDEntry.class).getEntryById(
									tmpEntry.getId());

							if (!tmpEntry.getId().equals(Configuration.getInstance().getID()) &&
								(tmpEntry.isNewerThan(idEntry)))
							{
								Database.getInstance(InfoServiceIDEntry.class).update(
									new InfoServiceIDEntry((InfoServiceDBEntry)tmpEntry));
								Database.getInstance(InfoServiceDBEntry.class).update(tmpEntry, false);
							}
						}
						else if (i == 1)
						{
							LogHolder.log(LogLevel.NOTICE, LogType.MISC, "Updating payment instances...");
							if (Database.getInstance(PaymentInstanceDBEntry.class).update(tmpEntry, false))
							{
								LogHolder.log(LogLevel.NOTICE, LogType.MISC, "Payment instances updated!");
							}							
						}
						else if (i == 2)
						{
							Database.getInstance(MixCascade.class).update(tmpEntry, false);
						}
						else
						{
							Database.getInstance(MixInfo.class).update(tmpEntry, false);							
						}
					}
				}
				bInit = false;

				InfoServiceDBEntry generatedOwnEntry =
					new InfoServiceDBEntry(Configuration.getInstance().getOwnName(),
										   Configuration.getInstance().getID(),
										   virtualListeners, Configuration.getInstance().holdForwarderList(), false,
										   System.currentTimeMillis(), ms_serialNumber);
				/* put the own entry in the database -> it is forwarded automatically to all neighbour
				 * infoservices, which are also in the database
				 */
				if (!Database.getInstance(InfoServiceDBEntry.class).update(generatedOwnEntry))
				{
					LogHolder.log(LogLevel.ALERT, LogType.MISC, "Could not update own InfoService entry: " +
								  generatedOwnEntry.getName() + ":" + generatedOwnEntry.getId() + 
								  ":LastUpdate:" +
								  generatedOwnEntry.getLastUpdate() + 
								  ":VersionNumber:" + generatedOwnEntry.getVersionNumber());
					/*
					Enumeration entries =
						Database.getInstance(InfoServiceDBEntry.class).getEntrySnapshotAsEnumeration();
					InfoServiceDBEntry entry;
					while (entries.hasMoreElements())
					{
						entry = (InfoServiceDBEntry) entries.nextElement();
						LogHolder.log(LogLevel.ALERT, LogType.MISC,
									  entry.getName() + ":" + entry.getId() + ":LastUpdate:" +
									  entry.getLastUpdate() + ":VersionNumber:" + entry.getVersionNumber());
					}*/
					// reset serial number
					ms_serialNumber = System.currentTimeMillis();
				}

				/* send it also to all initial neighbour infoservices -> they will always find us, after
				 * they come up
				 */
				InfoServiceDistributor.getInstance().addJobToInititalNeighboursQueue(generatedOwnEntry);
				LogHolder.log(LogLevel.DEBUG, LogType.MISC,
							  "Updating and propagating own InfoServerDBEntry.");
			}
			else
			{
				/* we need a listener-interface */
				LogHolder.log(LogLevel.EMERG, LogType.MISC,
							  "There is no virtual listener interface configurated. Shutdown InfoService!");
				System.out.println(
					"There is no virtual listener interface configurated. Shutdown InfoService!");
				System.exit( -1);
			}
			/* sleep for one announce period and then announce again */
			try
			{
				Thread.sleep(Constants.ANNOUNCE_PERIOD);
			}
			catch (InterruptedException e)
			{
			}
		}
	}

}
