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
/* Hint: This file may be only a copy of the original file which is always in the JAP source tree!
 * If you change something - do not forget to add the changes also to the JAP source tree!
 */
package anon.infoservice;

import java.lang.reflect.Constructor;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Observable;
import java.util.Observer;
import java.util.Vector;

import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;
import anon.crypto.MyRandom;
import anon.util.Util;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import anon.util.IXMLEncodable;
import anon.util.XMLUtil;

/**
 * This class is the generic implementation of a database. It is used by the database
 * implementations for the different services.
 * It is also a registry for all databases used in the context of this application. Instances of
 * this class are observable. Observers of the instances are notified, if an entry is added,
 * renewed or removed from the database or if the whole database is cleared. The observers will
 * always get a DatabaseMessage object as the argument of the update() method. The DatabaseMessage
 * will identify the reason of the notification, see the DatabaseMessage class for more
 * information.
 */
public final class Database extends Observable implements IXMLEncodable
{
	
	private static String XML_ALL_DB_NAME = "InfoServiceDB";
	/**
	 * The registered databases.
	 */
	private static Hashtable ms_databases = new Hashtable();

	/**
	 * The distributor that forwards new database entries.
	 */
	private static IDistributor ms_distributor;

	private static boolean ms_bShutdown = false;

	/**
	 * The DatabaseEntry class for that this Database is registered.
	 * The Database can only hold instances of this class.
	 */
	private Class m_DatabaseEntryClass;

	private Thread m_dbThread;
	private final Object SYNC_THREAD = new Object();

	/**
	 * Stores services we know.
	 */
	private Hashtable m_serviceDatabase;

	/**
	 * Chronological order (in relation to timeouts) of all objects in the database.
	 */
	private Vector m_timeoutList;
	
	private volatile boolean m_bStopThread = false;

	/**
	 * Registers a distributor that forwards new database entries.
	 * @param a_distributor a distributor that forwards new database entries
	 */
	public static void registerDistributor(IDistributor a_distributor)
	{
		ms_distributor = a_distributor;
	}

	/**
	 * Registers a Database object. If a Database was previously registered for the same
	 * DatabaseEntry class, the method does nothing and returns the previously registered Database.
	 * Otherwise, the given Database is returned.
	 * This method is used for testing purposes and should not be removed.
	 * @param a_Database the registered Database
	 * @return the actually registered Database instance for the specified DatabaseEntry class
	 */
	private static Database registerInstance(Database a_Database)
	{
		Database database = (Database) ms_databases.get(a_Database.getEntryClass());

		if (database == null && a_Database != null)
		{
			ms_databases.put(a_Database.getEntryClass(), a_Database);
			database = a_Database;
		}

		return database;
	}

	/**
	 * Unregisters the Database object that contains instances of the specified DatabaseEntry class.
	 * This method is used for testing purposes and should not be removed.
	 * @param a_DatabaseEntryClass the DatabaseEntry class for that the corresponding Database
	 *                             is unregistered
	 * @return the Database instance for the specified DatabaseEntry class that was unregistered
	 *         or null if  no corresponding Database could be found
	 */
	private static Database unregisterInstance(Class a_DatabaseEntryClass)
	{
		return (Database) ms_databases.remove(a_DatabaseEntryClass);
	}

	/**
	 * Unregisters all Database instances
	 * This method is used for testing purposes and should not be removed.
	 */
	private static void unregisterInstances()
	{
		ms_databases.clear();
	}

	/**
	 * Gets the Database for the specified database entries. Creates the Database
	 * if it does not exist already.
	 * @param a_DatabaseEntryClass the DatabaseEntry class for that the method returns
	 * the corresponding Database object
	 * @return the Database object that contains DatabaseEntries of the specified type
	 * @exception IllegalArgumentException if the argument is no valid DatabaseEntry class
	 */
	public static Database getInstance(Class a_DatabaseEntryClass) throws IllegalArgumentException
	{
		Database database = null;
		synchronized (Database.class)
		{
			database = (Database) ms_databases.get(a_DatabaseEntryClass);
			if (database == null)
			{
				database = new Database(a_DatabaseEntryClass);
				if (!ms_bShutdown)
				{
					ms_databases.put(a_DatabaseEntryClass, database);
				}
			}
		}
		return database;
	}

	public static void restoreFromXML(Document xmlAllDBs, Class[] classesToRestore)
	{
		
		if( (xmlAllDBs == null) || (classesToRestore == null) )
		{
			return;
		}
		
		Element allDBRoot = xmlAllDBs.getDocumentElement();
		if(allDBRoot == null)
		{
			return;
		}
		if(!allDBRoot.getNodeName().equals(XML_ALL_DB_NAME))
		{
			return;
		}
		
		Database currentDB = null;
		for (int i = 0; i < classesToRestore.length; i++) 
		{
			currentDB = getInstance(classesToRestore[i]);
			if(currentDB != null)
			{
				currentDB.loadFromXml(allDBRoot);
			}
		}
	}
	
	public static Document dumpToXML(Class[] classesToDump)
	{
		if(classesToDump == null)
		{
			return null;
		}
		
		Document doc = XMLUtil.createDocument();
		Element root = doc.createElement(XML_ALL_DB_NAME);
		Database currentDB = null;
		Element currentDBRoot = null;
		
		synchronized (Database.class)
		{
			for (int i = 0; i < classesToDump.length; i++) 
			{		
				currentDB = getInstance(classesToDump[i]);
				currentDBRoot = currentDB.toXmlElement(doc);
				if(currentDBRoot != null)
				{
					root.appendChild(currentDBRoot);
				}
			}
		}
		doc.appendChild(root);
		return doc;
	}
	
	public static void shutdownDatabases()
	{
		synchronized (Database.class)
		{
			ms_bShutdown = true;
			Enumeration databases = ms_databases.elements();
			Database currentDB;
			while (databases.hasMoreElements())
			{
				currentDB = (Database) databases.nextElement();
				synchronized (currentDB.SYNC_THREAD)
				{
					currentDB.stopThread();
				}
			}
			ms_databases.clear();
		}
	}


	/**
	 * Creates a new instance of a Database.
	 * @param a_DatabaseEntryClass the DatabaseEntry class for that this Database is registered
	 * @exception IllegalArgumentException if the argument is no valid DatabaseEntry class
	 */
	private Database(Class a_DatabaseEntryClass) throws IllegalArgumentException
	{
		if (a_DatabaseEntryClass == null)
		{
			throw new NullPointerException("Invalid database class!");
		}
		
		if (!AbstractDatabaseEntry.class.isAssignableFrom(a_DatabaseEntryClass))
		{
			throw new IllegalArgumentException(
				"There is no Database that can store entries of type " +
				a_DatabaseEntryClass.getName() + "!");
		}

		m_DatabaseEntryClass = a_DatabaseEntryClass;
		m_serviceDatabase = new Hashtable();
		m_timeoutList = new Vector();
		
		//startThread();
	}
	
	private void startThread()
	{	
		synchronized (SYNC_THREAD)
		{
			if (ms_bShutdown || (!m_bStopThread && m_dbThread != null && m_dbThread.isAlive()))
			{
				return;
			}
			
			while (m_dbThread != null && m_bStopThread && m_dbThread.isAlive())
			{
				LogHolder.log(LogLevel.ERR, LogType.DB, "Shutting down old database thread before starting new one (" + 
						m_DatabaseEntryClass.toString() + ")");
				// should not happen
				m_dbThread.interrupt();
				Thread.yield();
			}
			
			m_bStopThread = false;
			m_dbThread = new Thread(new TimeoutThread(), 
					"Database Thread: " + m_DatabaseEntryClass.toString());
			m_dbThread.setDaemon(true);
			m_dbThread.start();
		}
	}
	
	private void stopThread()
	{	
		synchronized (SYNC_THREAD)
		{
			m_bStopThread = true;
			while (m_dbThread != null && m_dbThread.isAlive())
			{
				LogHolder.log(LogLevel.INFO, LogType.DB, 
						"Shutting down db thread for class: " + m_DatabaseEntryClass.toString());
				m_dbThread.interrupt();
				synchronized (m_serviceDatabase)
				{
					m_serviceDatabase.notify();
				}
				Thread.yield();
			}
		}
	}

	private class TimeoutThread implements Runnable
	{
		/**
		 * This is the garbage collector for the database. If an entry becomes
		 * outdated, it will be automatically removed from the database.
		 */
		public void run()
		{
			LogHolder.log(LogLevel.INFO, LogType.DB, 
					"Starting timeout database thread for class " + m_DatabaseEntryClass.toString() + ".");
			while (!m_bStopThread && !ms_bShutdown && !Thread.currentThread().isInterrupted())
			{
				boolean moreOldEntrys = true;
				synchronized (m_serviceDatabase)
				{
					/* we need exclusive access to the database */
					while (!m_bStopThread && !ms_bShutdown && !Thread.currentThread().isInterrupted() &&
							(m_timeoutList.size() > 0) && (moreOldEntrys))
					{
						AbstractDatabaseEntry entry = (AbstractDatabaseEntry) m_serviceDatabase.get(m_timeoutList.
							firstElement());
						if (System.currentTimeMillis() >= entry.getExpireTime())
						{
							/* we remove the old entry now, because it has reached the expire time */
							LogHolder.log(LogLevel.INFO, LogType.MISC,
										  "DatabaseEntry (" + entry.getClass().getName() + ")" +
										  entry.getId() + " has reached the expire time and is removed.");
							m_serviceDatabase.remove(entry.getId());
							m_timeoutList.removeElementAt(0);
							/* notify the observers about the removal */
							setChanged();
							notifyObservers(new DatabaseMessage(DatabaseMessage.ENTRY_REMOVED, entry));
						}
						else
						{
							/* the oldest entry in the database
							 * has not reached expire time now, so there are no more old entries
							 */
							moreOldEntrys = false;
						}
					}
					if (m_bStopThread || ms_bShutdown || Thread.currentThread().isInterrupted())
					{
						return;
					}
				}
				synchronized (m_serviceDatabase)
				{
					/* we need the database in a consistent state */
					long sleepTime = 0;
					if (m_timeoutList.size() > 0)
					{
						/* get time until next timeout */
						sleepTime = ( (AbstractDatabaseEntry) (m_serviceDatabase.get(m_timeoutList.firstElement()))).
							getExpireTime() - System.currentTimeMillis();
					}
					if (sleepTime > 0)
					{
						/* there is nothing to do now -> wait until next expire time */
						try
						{
							m_serviceDatabase.wait(sleepTime);
							LogHolder.log(LogLevel.DEBUG, LogType.MISC,
										  "One entry could be expired. Wake up...");
						}
						catch (InterruptedException e)
						{
							if (m_bStopThread || ms_bShutdown || Thread.currentThread().isInterrupted())
							{
								return;
							}
						}
					}
					if (m_timeoutList.size() == 0)
					{
						/* there are no entries in the database, wait until there are some */
						try
						{
							m_serviceDatabase.wait();
							LogHolder.log(LogLevel.DEBUG, LogType.MISC,
										  "First entry in the database. Look when it expires. Wake up...");
						}
						catch (InterruptedException e)
						{
							if (m_bStopThread || ms_bShutdown || Thread.currentThread().isInterrupted())
							{
								return;
							}
						}
					}
				}
			}
		}
	}

	/**
	 * Updates an entry in the database. If the entry is an unknown or if it is newer then the
	 * one stored in the database for this service, the new entry is stored in the database and
	 * forwarded to all neighbour infoservices.
	 *
	 * @param newEntry The database entry to update.
	 * @exception IllegalArgumentException if the database entry is not of the type the Database
	 * can store
	 * @return if the database has been changed
	 */
	public boolean update(AbstractDatabaseEntry newEntry) throws IllegalArgumentException
	{
		return update(newEntry, true);
	}

	/**
	 * Updates an entry in the database. If the entry is an unknown or if it is newer then the
	 * one stored in the database for this service, the new entry is stored in the database and
	 * forwarded to all neighbour infoservices.
	 *
	 * @param newEntry The database entry to update.
	 * @param a_bDistribute distribute to other InfoServices if distributor object is set; should be default
	 * @exception IllegalArgumentException if the database entry is not of the type the Database
	 * can store
	 * @return if the database has been changed
	 */
	public boolean update(AbstractDatabaseEntry newEntry, boolean a_bDistribute)
		throws IllegalArgumentException
	{
		if (newEntry == null)
		{
			return false;
		}

		if (!m_DatabaseEntryClass.isAssignableFrom(newEntry.getClass()))
		{
			throw new IllegalArgumentException(
				"Database cannot store entries of type " + newEntry.getClass().getName() + "!");
		}
		boolean addEntry = false;
		AbstractDatabaseEntry oldEntry = null;
		boolean bStopThread = false;
		boolean bStartThread = false;
		
		synchronized (SYNC_THREAD)
		{
			synchronized (m_serviceDatabase)
			{
				/* we need exclusive access to the database */
				oldEntry = (AbstractDatabaseEntry) (m_serviceDatabase.get(newEntry.getId()));
				// check if this is an unknown entry, or if the entry is newer than the one we have stored
				addEntry = newEntry.isNewerThan(oldEntry);
				//if(addEntry && oldEntry != null) m_timeoutList.removeElement(oldEntry.getId());
	
				if (addEntry)
				{
					// test first if the element has not yet expired
					if (newEntry.getExpireTime() <= System.currentTimeMillis())
					{
						LogHolder.log(LogLevel.INFO, LogType.NET, "Received an expired db entry: '" +
									  newEntry.getId() + "' (" +  m_DatabaseEntryClass.toString() + 
									  "). It was dropped immediatly.");
						AbstractDatabaseEntry removedEntry =
							(AbstractDatabaseEntry)m_serviceDatabase.remove(newEntry.getId());
						if (removedEntry != null)
						{
							/* There was an entry with a lower version number in the database, which was not
							 * expired yet??? No matter why, now it was removed -> notify the observers.
							 */
							setChanged();
							notifyObservers(new DatabaseMessage(DatabaseMessage.ENTRY_REMOVED, removedEntry));
							return true;
						}
						return false;
					}
					// remove any old entry with the same id from the timeout list
					while (m_timeoutList.removeElement(newEntry.getId()));
	
					// add the entry to the database
					m_serviceDatabase.put(newEntry.getId(), newEntry);
	
					/* update the timeoutList */
					boolean timeoutEntryInserted = false;
					int i = 0;
					while (!timeoutEntryInserted)
					{
						if (i < m_timeoutList.size())
						{
							if ( ( (AbstractDatabaseEntry) (m_serviceDatabase.get(
								m_timeoutList.elementAt(i)))).getExpireTime() >=
								newEntry.getExpireTime())
							{
								m_timeoutList.insertElementAt(newEntry.getId(), i);
								timeoutEntryInserted = true;
							}
						}
						else
						{
							/* we are at the last position in the list -> add entry at the end */
							m_timeoutList.addElement(newEntry.getId());
							timeoutEntryInserted = true;
						}
						i++;
					}
					if (i == 1)
					{
						/* entry at the first expire position added */
						if (newEntry.getExpireTime() == Long.MAX_VALUE)
						{
							// this entry will never expire -> cleanup thread is not needed any more
							bStopThread = true;
						}
						else
						{
							/* -> notify the cleanup thread */
							bStartThread = true; // activate if needed	
							m_serviceDatabase.notify();
						}
						
					}
					LogHolder.log(LogLevel.DEBUG, LogType.MISC,
								  "Added / updated entry '" + newEntry.getId() + "' in the " +
								  m_DatabaseEntryClass.getName() + " database. Now there are " +
								  Integer.toString(m_serviceDatabase.size()) +
								  " entries stored in this database. The new entry has position " +
								  Integer.toString(i) + "/" + Integer.toString(m_timeoutList.size()) +
								  " in the database-timeout list.");
					if (newEntry instanceof IDistributable && a_bDistribute)
					{
						// forward new entries
						if (ms_distributor != null)
						{
							ms_distributor.addJob( (IDistributable) newEntry);
						}
						else
						{
							LogHolder.log(LogLevel.WARNING, LogType.MISC,
										  "No distributor specified - cannot distribute database entries!");
						}
					}
				}
			}
			if (bStopThread)
			{
				stopThread();
			}
			else if (bStartThread)
			{
				startThread();
			}
		}
		if (addEntry)
		{	
			/* there was an entry added or renewed in the database -> notify the observers */
			setChanged();
			if (oldEntry == null)
			{
				/* it was really a new entry */
				notifyObservers(new DatabaseMessage(DatabaseMessage.ENTRY_ADDED, newEntry));
			}
			else
			{
				/* there was already an entry with the same ID -> the entry was renewed */
				notifyObservers(new DatabaseMessage(DatabaseMessage.ENTRY_RENEWED, newEntry));
			}
			return true;
		}
		return false;
	}

	/**
	 * Returns the DatabaseEntry class for that this Database is registered.
	 * @return the DatabaseEntry class for that this Database is registered
	 */
	public Class getEntryClass()
	{
		return m_DatabaseEntryClass;
	}

	/**
	 * Removes an entry from the database.
	 *
	 * @param a_entryID The ID of the entry to remove. If it is not in the database, nothing is done.
	 * @return if the database has been changed
	 */
	public boolean remove(String a_entryID)
	{
		if (a_entryID != null)
		{
			AbstractDatabaseEntry removedEntry;
			boolean bStopThread = false;
			boolean bStartThread = false;
			synchronized (SYNC_THREAD)
			{
				synchronized (m_serviceDatabase)
				{
					/* we need exclusive access to the database */
					removedEntry = (AbstractDatabaseEntry) m_serviceDatabase.remove(a_entryID);
					if (removedEntry != null)
					{
						m_timeoutList.removeElement(a_entryID);
						if (m_timeoutList.size() > 0 && 
							((AbstractDatabaseEntry)m_serviceDatabase.get(m_timeoutList.elementAt(0))).getExpireTime() ==
								Long.MAX_VALUE)
						{
							bStopThread = true;
						}
						else
						{
							bStartThread = true;
						}
					}
				}
				
				if (bStartThread)
				{
					startThread();
				}
				else if (bStopThread)
				{
					stopThread();
				}
			}
			
			if (removedEntry != null)
			{
				/* an entry was removed -> notify the observers */
				setChanged();
				notifyObservers(new DatabaseMessage(DatabaseMessage.ENTRY_REMOVED, removedEntry));
				return true;
			}
		}
		return false;
	}

	/**
	 * Removes an entry from the database.
	 *
	 * @param a_deleteEntry The entry to remove. If it is not in the database, nothing is done.
	 * @return if the database has been changed
	 */
	public boolean remove(AbstractDatabaseEntry a_deleteEntry)
	{
		if (a_deleteEntry != null && m_DatabaseEntryClass.isAssignableFrom(a_deleteEntry.getClass()))
		{
			return remove(a_deleteEntry.getId());
		}
		return false;
	}

	/**
	 * Removes all entries from the database.
	 */
	public void removeAll()
	{
		synchronized (SYNC_THREAD)
		{
			synchronized (m_serviceDatabase)
			{
				/* we need exclusive access to the database */
				m_serviceDatabase.clear();
				m_timeoutList.removeAllElements();
			}
			stopThread();
		}
		/* database was cleared -> notify the observers */
		setChanged();
		notifyObservers(new DatabaseMessage(DatabaseMessage.ALL_ENTRIES_REMOVED));
	}

	/**
	 * Adds all database entries that are subnodes of the given element to the database.
	 * The class must have a constructor with a single argument, a org.w3c.dom.Element, so that this
	 * is successful.
	 *
	 * @param a_dbNode The xml node that contains db entries.
	 * @return number of updated entries
	 */
	public int loadFromXml(Element a_dbNode)
	{
		return loadFromXml(a_dbNode, false);
	}
	
	/**
	 * Adds all database entries that are subnodes of the given element to the database.
	 * The class must have a constructor with a single argument, a org.w3c.dom.Element, so that this
	 * is successful.
	 *
	 * @param a_dbNode The xml node that contains db entries.
	 * @param a_signatureDocumentClass if set to a value greater -1, the document is verified
	 * against certificates of the given class before getting loaded
	 * @return number of updated entries
	 */
	public int loadFromXml(Element a_dbNode, boolean a_bVerify)
	{
		int updatedEntries = 0;
		String xmlElementName = XMLUtil.getXmlElementName(m_DatabaseEntryClass);
		if (a_dbNode == null || xmlElementName == null)
		{
			return updatedEntries;
		}
		NodeList dbNodes = a_dbNode.getElementsByTagName(xmlElementName);
		Constructor constructor = null;
		AbstractDatabaseEntry instance;
		
		try
		{
			constructor = m_DatabaseEntryClass.getConstructor(
					new Class[]{Element.class, long.class});
			// first try to find constructor with timeout and set timeout unlimited					
		}
		catch (Exception a_e)
		{
			// no such constructor
			LogHolder.log(LogLevel.NOTICE, LogType.DB, 
					"No timeout constructor for " + m_DatabaseEntryClass + " available.");
		}
		
		for (int i = 0; i < dbNodes.getLength(); i++)
		{
			/* add all children to the database */			
			try
			{
				if (constructor == null)
				{
					instance = (AbstractDatabaseEntry)m_DatabaseEntryClass.getConstructor(
							new Class[]{Element.class}).newInstance(new Object[]{dbNodes.item(i)});
				}
				else
				{
					instance = (AbstractDatabaseEntry)constructor.newInstance(
							new Object[]{dbNodes.item(i), new Long(Long.MAX_VALUE)});
				}
				
				if (a_bVerify && instance instanceof ICertifiedDatabaseEntry &&
					!((ICertifiedDatabaseEntry) instance).isVerified())
				{
					// not verified
					LogHolder.log(LogLevel.WARNING, LogType.MISC, 
							"XML entry " + dbNodes.item(i).getNodeName() + " for ID " +
							instance.getId() + " could not be verified while being loaded!");
					continue;
				}
				update(instance);
				updatedEntries++;
			}
			catch (Exception e)
			{
				LogHolder.log(LogLevel.WARNING, LogType.MISC, "Could not load db entry from XML!", e);
				/* if there was an error, it does not matter */
			}
		}
		return updatedEntries;
	}

	/**
	 * If the entries of this database implement IXMLEncodable and has a proper value for the field
	 * XML_ELEMENT_CONTAINER_NAME, this database is transformed into an XML element.
	 * @param a_doc a Document
	 * @return the database as XML Element or null if transformation was not possible
	 */
	public Element toXmlElement(Document a_doc)
	{
		return toXmlElement(a_doc, XMLUtil.getXmlElementContainerName(m_DatabaseEntryClass));
	}

	/**
	 * Creates an XML node with all database entries, but only for those entries that implement
	 * IXMLEncodable.
	 *
	 * @param a_doc The XML document, which is the environment for the created XML node.
	 * @param a_xmlContainerName the name of the XML element that should contain the entries
	 *
	 * @return the newly created XML node.
	 */
	public Element toXmlElement(Document a_doc, String a_xmlContainerName)
	{
		Element element;

		if (a_doc == null || !IXMLEncodable.class.isAssignableFrom(m_DatabaseEntryClass) ||
			a_xmlContainerName == null || a_xmlContainerName.trim().length() == 0)
		{
			return null;
		}

		element = a_doc.createElement(a_xmlContainerName);
		synchronized (m_serviceDatabase)
		{
			Enumeration it = m_serviceDatabase.elements();
			while (it.hasMoreElements())
			{
				element.appendChild( ( (IXMLEncodable) (it.nextElement())).toXmlElement(a_doc));
			}
		}
		return element;
	}

	public Hashtable getEntryHash()
	{
		return (Hashtable)m_serviceDatabase.clone();
	}

	/**
	 * Returns a snapshot of all values in the serviceDatabase.
	 *
	 * @return A Vector with all values which are stored in the serviceDatabase.
	 */
	public Vector getEntryList()
	{
		Vector entryList = new Vector();
		synchronized (m_serviceDatabase)
		{
			/* get the actual values */
			Enumeration serviceDatabaseElements = m_serviceDatabase.elements();
			while (serviceDatabaseElements.hasMoreElements())
			{
				entryList.addElement(serviceDatabaseElements.nextElement());
			}
		}
		return entryList;
	}

	public Vector getSortedEntryList(Util.Comparable c)
	{
		Vector entryList = getEntryList();

		anon.util.Util.sort(entryList, c);

		return entryList;
	}

	/**
	 * Returns a snapshot of all entries in the Database as an Enumeration.
	 *
	 * @return a snapshot of all entries in the Database as an Enumeration
	 */
	public Enumeration getEntrySnapshotAsEnumeration()
	{
		synchronized (m_serviceDatabase)
		{
			return getEntryList().elements();
		}
	}

	/**
	 * Returns the number of DatabaseEntries in the Database.
	 * @return the number of DatabaseEntries in the Database
	 */
	public int getNumberOfEntries()
	{
		return m_serviceDatabase.size();
	}

	/**
	 * Returns the DatabaseEntry with the given ID. If there is no DatabaseEntry with this ID is
	 * in the database, null is returned.
	 *
	 * @param entryId The ID of the database entry.
	 * @return The entry with the specified ID or null, if there is no such entry.
	 */
	public AbstractDatabaseEntry getEntryById(String entryId)
	{
		if (entryId == null)
		{
			return null;
		}
		AbstractDatabaseEntry resultEntry = null;
		synchronized (m_serviceDatabase)
		{
			/* get the actual value */
			resultEntry = (AbstractDatabaseEntry) (m_serviceDatabase.get(entryId));
		}
		return resultEntry;
	}

	/**
	 * Returns a random entry from the database. If there are no entries in the database, null is
	 * returned.
	 *
	 * @return A random entry from the database or null, if the database is empty.
	 */
	public AbstractDatabaseEntry getRandomEntry()
	{
		AbstractDatabaseEntry resultEntry = null;
		synchronized (m_serviceDatabase)
		{
			/* all keys of the database are in the timeout list -> select a random key from there
			 * and get the associated entry from the database
			 */
			if (m_timeoutList.size() > 0)
			{
				try
				{
					String entryId =
						(String) m_timeoutList.elementAt(
							new MyRandom().nextInt(m_timeoutList.size()));
					resultEntry = (AbstractDatabaseEntry) (m_serviceDatabase.get(entryId));
				}
				catch (Exception e)
				{
					/* should never occur */
				}
			}
		}
		return resultEntry;
	}

	/**
	 * Adds an observer to this database. The observer will obtain an initial message including a
	 * snapshot of the current database (this message is also sent, if the observer was already
	 * observing the database).
	 *
	 * @param a_observer The observer to add to this database.
	 */
	public void addObserver(Observer a_observer)
	{
		synchronized (m_serviceDatabase)
		{
			/* add the observer -> because we have locked m_serviceDatabase, there will be no message
			 * sent to the observers, until we are done -> our message to the new observer will be the
			 * first
			 */
			super.addObserver(a_observer);
			/* send the initial message to the new observer */
			a_observer.update(this,
							  new DatabaseMessage(DatabaseMessage.INITIAL_OBSERVER_MESSAGE, getEntryList()));
		}
	}

	public boolean isEntryIdInTimeoutList(String a_entryId)
	{
		return m_timeoutList.contains(a_entryId);
	}

	public int getTimeoutListSize()
	{
		return m_timeoutList.size();
	}
	
	public Document getWebInfos(String a_ID)
	{
		return getWebInfos(getEntryClass(), a_ID);
	}
	
	public Document getWebInfos()
	{
		return getWebInfos(getEntryClass());
	}
	
	public static interface IWebInfo
	{
		public static final String FIELD_XML_ELEMENT_WEBINFO_CONTAINER = "XML_ELEMENT_WEBINFO_CONTAINER";
//		public static final String XML_ELEMENT_WEBINFO_CONTAINER;  /DO NOT REMOVE! THIS IS PART OF THE SPECIFICATION!
		Element getWebInfo(Document a_doc);
	}
	
	/**
	 * get WebInfos for an entry with the specified ID
	 */
	private static Document getWebInfos(Class a_webInfoClass, String a_ID)
	{
		if (!IWebInfo.class.isAssignableFrom(a_webInfoClass))
		{
			LogHolder.log(LogLevel.EMERG, LogType.DB, "Illegal class for web info: " + a_webInfoClass);
			return null;
		}
		
		Document webInfoDoc = XMLUtil.createDocument();
		IWebInfo webinfo = (IWebInfo)getInstance(a_webInfoClass).getEntryById(a_ID);
		Element webInfoElement =
			(webinfo == null) ? null : webinfo.getWebInfo(webInfoDoc);
		if(webInfoElement == null)
		{
			return null;
		}
		webInfoDoc.appendChild(webInfoElement);
		return webInfoDoc;
	}
	
	/**
	 * get WebInfos for all entries
	 */
	private static Document getWebInfos(Class a_webInfoClass)
	{
		if (!IWebInfo.class.isAssignableFrom(a_webInfoClass))
		{
			return null;
		}
		
		String nameContainer = 
			Util.getStaticFieldValue(a_webInfoClass, IWebInfo.FIELD_XML_ELEMENT_WEBINFO_CONTAINER);
		
		if (nameContainer == null)
		{
			return null;
		}
		
		Document allWebInfosDoc = XMLUtil.createDocument();
		Vector entries = getInstance(a_webInfoClass).getEntryList();
		IWebInfo webinfo = null;
		
		Element rootElement = allWebInfosDoc.createElement(nameContainer);
		Element listItem = null;
		allWebInfosDoc.appendChild(rootElement);
		
		for (int i = 0; i < entries.size(); i++) 
		{
			webinfo = (IWebInfo) entries.elementAt(i);
			listItem = webinfo.getWebInfo(allWebInfosDoc);
			if(listItem != null)
			{
				rootElement.appendChild(listItem);
			}
		}
		return allWebInfosDoc;
	}
}
