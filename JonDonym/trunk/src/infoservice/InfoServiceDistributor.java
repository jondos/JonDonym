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
import java.util.Vector;

import HTTPClient.HTTPConnection;
import HTTPClient.HTTPResponse;

import anon.infoservice.Database;
import anon.infoservice.HTTPConnectionFactory;
import anon.infoservice.IDistributable;
import anon.infoservice.IDistributor;
import anon.infoservice.InfoServiceDBEntry;
import anon.infoservice.ListenerInterface;
import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;

/**
 * This is the implementation of the forwarding code. Every information which to
 * send to the other infoservices is processed by this class.
 */
public class InfoServiceDistributor implements IDistributor
{
	private static final long TIMEOUT_QUEUE = 50000; // time after that to
	// look for jobs; for
	// debugging only
	private static final int CONNECTION_TIMEOUT = 10000;
	private static final int BLOCKING_FACTOR = 5;

	/**
	 * Stores the instance of InfoServiceDatabase (Singleton).
	 */
	private static InfoServiceDistributor ms_isdInstance = null;

	/**
	 * Stores all jobs we have to send to the other infoservices. This is the
	 * default job queue, which means, that every entry is sent to all known and
	 * running infoservices stored in the InfoServiceDatabase.
	 */
	private Vector m_defaultJobQueue;

	/**
	 * Stores all jobs we have to send to the initial neighbours of our
	 * infoservice. This job queue should only be used for the propaganda of the
	 * own infoservice entry.
	 */
	private Vector m_initialNeighboursJobQueue;

	/**
	 * Returns the instance of InfoServiceDistributor (Singleton). If there is
	 * no instance, there is a new one created. Also the included threads are
	 * started.
	 * 
	 * @return The InfoServiceDistributor instance.
	 */
	public static InfoServiceDistributor getInstance()
	{
		synchronized (InfoServiceDistributor.class)
		{
			if (ms_isdInstance == null)
			{
				ms_isdInstance = new InfoServiceDistributor();
			}
		}
		return ms_isdInstance;
	}

	/**
	 * Creates a new instance of an InfoServiceDistributor. Some initialization
	 * is done here. Also the internal queue processing threads are started.
	 */
	private InfoServiceDistributor()
	{
		m_defaultJobQueue = new Vector();
		m_initialNeighboursJobQueue = new Vector();
		/* start the job queue processing threads */
		Thread defaultJobQueueThread = new Thread(new Runnable()
		{
			/**
			 * This is the thread for processing the default distribution job
			 * queue. Every job is sent to all known and working neighbour
			 * infoservices, which are stored in the InfoServiceDatabase.
			 */
			public void run()
			{
				while (true)
				{
					IDistributable currentJob = null;
					synchronized (m_defaultJobQueue)
					{
						if (m_defaultJobQueue.size() > 0)
						{
							currentJob = (IDistributable) (m_defaultJobQueue.firstElement());
							m_defaultJobQueue.removeElementAt(0);
						}
						else
						{
							/*
							 * if there are no more jobs in the queue, sleep
							 * until there are new ones
							 */
							try
							{
								m_defaultJobQueue.wait();
								LogHolder.log(LogLevel.DEBUG, LogType.NET,
										"There is something to do. Wake up...");
							}
							catch (InterruptedException e)
							{
							}
						}
					}
					if (currentJob != null)
					{
						final IDistributable job = currentJob;
						/*
						 * new Thread(new Runnable() { public void run() {
						 */
						InfoServiceDBEntry entry;
						LogHolder.log(LogLevel.DEBUG, LogType.NET, "Forward entry " + job.getId()
								+ " to all running neighbour infoservices.");
						Enumeration runningNeighbourInfoServices = getNeighbourList().elements();
						while (runningNeighbourInfoServices.hasMoreElements())
						{
							entry = (InfoServiceDBEntry) (runningNeighbourInfoServices
									.nextElement());
							if (!sendToInfoService(entry, job))
							{
								LogHolder.log(LogLevel.ERR, LogType.NET, "Could not send entry "
										+ job + " to InfoService " + entry + "!");
							}
						}
						/*
						 * } }, "Distribute single entry").start();
						 */
					}
				}
			}
		});
		defaultJobQueueThread.setDaemon(true);
		defaultJobQueueThread.setName("InfoServiceDistributor - DefaultQueue JOb Thread");
		defaultJobQueueThread.start();
		Thread initialNeighboursJobQueueThread = new Thread(new Runnable()
		{
			/**
			 * This is the thread for processing the initial neighbour
			 * distribution job queue. Every job is sent to all interfaces
			 * specified as initial neighbours of our infoservice. It should
			 * only be used for the propaganda of the own infoservice entry.
			 */
			public void run()
			{
				IDistributable currentJob;
				while (true)
				{
					synchronized (m_initialNeighboursJobQueue)
					{
						if (m_initialNeighboursJobQueue.size() > 0)
						{
							currentJob = (IDistributable) (m_initialNeighboursJobQueue
									.firstElement());
							m_initialNeighboursJobQueue.removeElementAt(0);
						}
						else
						{
							currentJob = null;
							/*
							 * if there are no more jobs in the queue, sleep
							 * until there are new ones
							 */
							try
							{
								m_initialNeighboursJobQueue.wait(TIMEOUT_QUEUE);
								LogHolder.log(LogLevel.DEBUG, LogType.NET,
										"There may be something to do. Wake up...");
							}
							catch (InterruptedException e)
							{
							}
						}
					}
					if (currentJob != null)
					{
						final IDistributable job = currentJob;

						/*
						 * new Thread(new Runnable() { public void run() {
						 */
						ListenerInterface listener;
						LogHolder.log(LogLevel.DEBUG, LogType.NET, "Forward entry " + job.getId()
								+ " to initial neighbour infoservices.");
						Enumeration enumInitialNeighbours = Configuration.getInstance()
								.getInitialNeighbourInfoServices().elements();
						Vector vecInitialNeighbours = new Vector();
						Vector neighbourList = getNeighbourList();
						Vector listeners;
						InfoServiceDBEntry entry;
						ListenerInterface currentNeigbourInterface;
						neighbours: while (enumInitialNeighbours.hasMoreElements())
						{
							currentNeigbourInterface = (ListenerInterface) enumInitialNeighbours
									.nextElement();
							for (int i = 0; i < neighbourList.size(); i++)
							{
								entry = (InfoServiceDBEntry) neighbourList.elementAt(i);
								listeners = entry.getListenerInterfaces();
								for (int j = 0; j < listeners.size(); j++)
								{
									if (((ListenerInterface) listeners.elementAt(j)).isValid()
											&& ((ListenerInterface) listeners.elementAt(j))
													.equals(currentNeigbourInterface))
									{
										// we already have this interface in the
										// database
										continue neighbours;
									}
								}
							}
							// this interface is not in the database
							vecInitialNeighbours.addElement(currentNeigbourInterface);
						}

						enumInitialNeighbours = vecInitialNeighbours.elements();
						while (enumInitialNeighbours.hasMoreElements())
						{
							listener = (ListenerInterface) enumInitialNeighbours.nextElement();
							/*
							 * we have only the interfaces of the initial
							 * neighbours
							 */
							if (!sendToInterface(listener, job))
							{
								LogHolder.log(LogLevel.ERR, LogType.NET, "Could not send entry "
										+ job + " to initial neighbour InfoService: " + listener
										+ "!");
							}
						}

						Enumeration runningNeighbourInfoServices = neighbourList.elements();
						while (runningNeighbourInfoServices.hasMoreElements())
						{
							entry = (InfoServiceDBEntry) (runningNeighbourInfoServices
									.nextElement());
							if (!sendToInfoService(entry, job))
							{
								LogHolder.log(LogLevel.ERR, LogType.NET, "Could not send entry "
										+ job + " to neighbour InfoService " + entry + "!");
							}
						}
						/*
						 * } }, "Distribute single initial neighbour
						 * entry").start();
						 */
					}
				}
			}
		});
		initialNeighboursJobQueueThread.setDaemon(true);
		initialNeighboursJobQueueThread
				.setName("InfoServiceDistributor - Initial NeighboursQueue Job Thread");
		initialNeighboursJobQueueThread.start();
	}

	/**
	 * Adds a new job to the default job queue. So it is forwarded to all known
	 * and running neighbour infoservices, stored in the InfoServiceDatabase.
	 * 
	 * @param a_newJob
	 *            The information to forward.
	 */
	public void addJob(IDistributable a_newJob)
	{
		synchronized (m_defaultJobQueue)
		{
			m_defaultJobQueue.addElement(a_newJob);
			LogHolder.log(LogLevel.DEBUG, LogType.NET, "Added Job '" + a_newJob.getId()
					+ "' to the distribution queue. Now there are "
					+ Integer.toString(m_defaultJobQueue.size()) + " jobs waiting in the queue.");
			/*
			 * notify (and awake) the distribution thread for the default job
			 * queue, if it is waiting on the job queue
			 */
			m_defaultJobQueue.notify();
		}
	}

	/**
	 * Adds a new job to the initial neighbours job queue. So it is forwarded to
	 * all initial neighbour infoservices (specified in the config file). This
	 * should only be used for the propaganda of the own infoservice entry.
	 * 
	 * @param a_newJob
	 *            The information to forward.
	 */
	public void addJobToInititalNeighboursQueue(IDistributable a_newJob)
	{
		synchronized (m_initialNeighboursJobQueue)
		{
			m_initialNeighboursJobQueue.addElement(a_newJob);
			/*
			 * notify (and awake) the distribution thread for the initial
			 * neighbours job queue, if it is waiting on the job queue
			 */
			m_initialNeighboursJobQueue.notify();
		}
	}

	/**
	 * Creates a list of all known infoservices (from the InfoServiceDatabase),
	 * which are neighbours of our one.
	 * 
	 * @return List of all neighbour infoservices we know.
	 */
	private Vector getNeighbourList()
	{
		Vector targets = new Vector();
		Enumeration enumer = Database.getInstance(InfoServiceDBEntry.class)
				.getEntrySnapshotAsEnumeration();
		while (enumer.hasMoreElements())
		{
			InfoServiceDBEntry currentInfoService = (InfoServiceDBEntry) (enumer.nextElement());

			if (currentInfoService.isNeighbour() && !targets.contains(currentInfoService)
					&& !Configuration.getInstance().getID().equals(currentInfoService.getId()))
			{
				/*
				 * if currentInfoService is a neighbour of our one, put it in
				 * the target list
				 */
				targets.addElement(currentInfoService);
			}
		}
		return targets;
	}

	/**
	 * Sends the information about the DatabaseEntry to a remote infoservice.
	 * TODO: if an infoservice entry is updated, we loose information about
	 * invalid interfaces and so we try them again (very time intensive) -->
	 * better: take always the last working interface until it is not working
	 * any more
	 * 
	 * @param a_infoservice
	 *            The remote infoservice.
	 * @param a_information
	 *            The information to send to the infoservice.
	 * 
	 * @return Whether we could reach the remote infoservice or not.
	 */
	private boolean sendToInfoService(InfoServiceDBEntry a_infoservice, IDistributable a_information)
	{
		boolean connected = false;
		Enumeration enumer = a_infoservice.getListenerInterfaces().elements();
		while ((enumer.hasMoreElements()) && (connected == false))
		{
			ListenerInterface currentInterface = (ListenerInterface) (enumer.nextElement());
			if (currentInterface.isValid())
			{
				/*
				 * send only to valid interfaces (if we can't reach an
				 * interface, we set it to invalid)
				 */
				if (sendToInterface(currentInterface, a_information))
				{
					/*
					 * if connection is successful, we reached the remote
					 * infoservice, don't connect again
					 */
					connected = true;
				}
				else
				{
					/*
					 * If we can't reach the interface, make it invalid to
					 * prevent more connections to there. currentInterface is a
					 * direct reference to that stored in the database of all
					 * infoservices.
					 */
					currentInterface.blockInterface(CONNECTION_TIMEOUT * BLOCKING_FACTOR);
					LogHolder.log(LogLevel.ERR, LogType.NET, "Could not reach InfoService "
							+ a_infoservice.getId() + " at " + currentInterface.getHost() + ":"
							+ Integer.toString(currentInterface.getPort())
							+ " -> temporarily invalidate that interface.");
				}
			}
		}
		return connected;
	}

	/**
	 * Sends the information to listener (remote ListenerInterface).
	 * 
	 * @param a_listener
	 *            The representation of a host address and port to send to.
	 * @param a_information
	 *            The information to send to the listener interface.
	 * 
	 * @return Whether the connection was successful or not.
	 */
	private boolean sendToInterface(ListenerInterface a_listener, IDistributable a_information)
	{
		boolean connected = true;
		HTTPConnection connection = null;
		try
		{
			connection = HTTPConnectionFactory.getInstance().createHTTPConnection(a_listener,
					a_information.getPostEncoding(), false);
			connection.setTimeout(CONNECTION_TIMEOUT);
			/* post the information */
			HTTPResponse response = connection.Post(a_information.getPostFile(), a_information
					.getPostData());
			/* wait for the response with the status code */
			int statusCode = response.getStatusCode();
			LogHolder.log(LogLevel.DEBUG, LogType.NET, "Entry " + a_information.getId()
					+ " sent to: " + a_listener.getHost() + ":"
					+ Integer.toString(a_listener.getPort()) + a_information.getPostFile()
					+ " Result: " + Integer.toString(statusCode));
		}
		catch (Throwable e)
		{
			connected = false;
			LogHolder.log(LogLevel.ERR, LogType.NET, "Error while sending " + a_information.getId()
					+ " to: " + a_listener.getHost() + ":" + Integer.toString(a_listener.getPort())
					+ a_information.getPostFile(), e);
		}
		if (connection != null)
		{
			/*
			 * we have received a status code or an exception occured -> we can
			 * interrupt the connection because our post request has been
			 * processed or an exception occured -> we don't need the connection
			 * any more in both cases
			 */
			connection.stop();
		}
		return connected;
	}

}
