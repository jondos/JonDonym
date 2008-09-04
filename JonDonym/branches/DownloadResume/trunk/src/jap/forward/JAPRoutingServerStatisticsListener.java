/*
 Copyright (c) 2000 - 2004, The JAP-Team
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
package jap.forward;

import java.util.Observable;
import java.util.Observer;

import forward.server.ForwardSchedulerStatistics;

import jap.*;

/**
 * This class is the implementation for the forwarding server statistics update. Every class
 * interested in the forwarding server statistics should add itself to the observers of this
 * class. It gets an update message with JAPRoutingMessage.SERVER_STATISTICS_UPDATED, every time
 * some values might have changed. So it can ask for the instance of this class for the new
 * values. This class is also available, if the server is not running (but fetching of new
 * statistics is only done, when the forwarding server is running).
 */
public class JAPRoutingServerStatisticsListener extends Observable implements Observer, Runnable
{

	/**
	 * This is the update interval for the server statistics panel in milliseconds.
	 */
	private static final long SERVER_STATISTICS_UPDATE_INTERVAL = (long) 1000;

	/**
	 * Stores the number of rejected connections.
	 */
	private int m_rejectedConnections;

	/**
	 * Stores the number of accepted connections.
	 */
	private int m_acceptedConnections;

	/**
	 * Stores the number of currently forwarded connections.
	 */
	private int m_currentlyForwardedConnections;

	/**
	 * Stores the number of transfered bytes.
	 */
	private long m_transferedBytes;

	/**
	 * Stores the current bandwidth usage in bytes/sec.
	 */
	private int m_currentBandwidthUsage;

	/**
	 * Stores the current statistics instance from where we obtain the data.
	 */
	private ForwardSchedulerStatistics m_currentStatisticsInstance;

	/**
	 * Stores the current instance of the statistics fetching thread (if we are in server mode).
	 */
	private Thread m_statisticsThread;

	/**
	 * Creates a new instance of JAPForwardingServerStatisticsListener. It's required, that the
	 * server isn't running at the moment of creation. Also the new instance must be added to the
	 * observers of JAPRoutingSettings after creation.
	 */
	public JAPRoutingServerStatisticsListener()
	{
		m_rejectedConnections = 0;
		m_acceptedConnections = 0;
		m_currentlyForwardedConnections = 0;
		m_transferedBytes = 0;
		m_currentBandwidthUsage = 0;
		m_currentStatisticsInstance = null;
		m_statisticsThread = null;
	}

	/**
	 * Returns the number of rejected connections. If there is no forwarding server running at the
	 * moment, 0 is returned.
	 *
	 * @return The number of rejected connections.
	 */
	public int getRejectedConnections()
	{
		return m_rejectedConnections;
	}

	/**
	 * Returns the number of accepted connections. If there is no forwarding server running at the
	 * moment, 0 is returned.
	 *
	 * @return The number of accepted connections.
	 */
	public int getAcceptedConnections()
	{
		return m_acceptedConnections;
	}

	/**
	 * Returns the number of currently forwarded connections. If there is no forwarding server
	 * running at the moment, 0 is returned.
	 *
	 * @return The number of currently forwarded connections.
	 */
	public int getCurrentlyForwardedConnections()
	{
		return m_currentlyForwardedConnections;
	}

	/**
	 * Returns the current bandwidth usage. If there is no forwarding server running at the moment,
	 * 0 is returned.
	 *
	 * @return The current bandwidth usage in bytes/sec.
	 */
	public int getCurrentBandwidthUsage()
	{
		return m_currentBandwidthUsage;
	}

	/**
	 * Returns the total number of transfered bytes in the current forwarding server session. If
	 * there is no forwarding server running at the moment, 0 is returned.
	 *
	 * @return The total number of transfered bytes.
	 */
	public long getTransferedBytes()
	{
		return m_transferedBytes;
	}

	/**
	 * This is the observer implementation. If the routing mode is changed to server mode in
	 * JAPRoutingSettings, we fetch the new statistics instance of the server and also notify
	 * the observers of this instance of JAPForwardingServerStatisticsListener periodically
	 * about new values. If the forwarding server was shutted down, we stop the periodical
	 * notification of the observers of this instance (one last notification after the shutdown
	 * is sent).
	 *
	 * @param a_notifier The observed Object. This should always be JAPRoutingSettings at the moment.
	 * @param a_message The reason of the notification. This should always be a JAPRoutingMessage
	 *                  at the moment.
	 */
	public void update(Observable a_notifier, Object a_message)
	{
		try
		{
			if (a_notifier == JAPModel.getInstance().getRoutingSettings())
			{
				/* message is from JAPRoutingSettings */
				if ( ( (JAPRoutingMessage) (a_message)).getMessageCode() ==
					JAPRoutingMessage.ROUTING_MODE_CHANGED)
				{
					if (JAPModel.getInstance().getRoutingSettings().getRoutingMode() ==
						JAPRoutingSettings.ROUTING_MODE_SERVER)
					{
						startStatistics();
					}
					else
					{
						stopStatistics();
					}
				}
			}
		}
		catch (Exception e)
		{
		}
	}

	/**
	 * This is the implementation of the forwarding server statistics fetch thread. Once per update
	 * interval the statistics are fetched and the observers of this class are notified, so they can
	 * obtain the new statistics values.
	 */
	public void run()
	{
		boolean interrupted = false;
		while (interrupted == false)
		{
			boolean somethingHasChanged = false;
			synchronized (this)
			{
				interrupted = Thread.interrupted();
				if (interrupted == false)
				{
					/* backup all values for determining, whether something has changed */
					int oldRejectedConnections = m_rejectedConnections;
					int oldAcceptedConnections = m_acceptedConnections;
					int oldForwardedConnections = m_currentlyForwardedConnections;
					long oldTransferedBytes = m_transferedBytes;
					int oldBandwidthUsage = m_currentBandwidthUsage;
					/* get the current values */
					m_rejectedConnections = m_currentStatisticsInstance.getRejectedConnections();
					m_acceptedConnections = m_currentStatisticsInstance.getAcceptedConnections();
					m_currentlyForwardedConnections = JAPModel.getInstance().getRoutingSettings().
						getCurrentlyForwardedConnections();
					m_transferedBytes = m_currentStatisticsInstance.getTransferedBytes();
					m_currentBandwidthUsage = m_currentStatisticsInstance.getCurrentBandwidthUsage();
					if ( (oldRejectedConnections != m_rejectedConnections) ||
						(oldAcceptedConnections != m_acceptedConnections) ||
						(oldForwardedConnections != m_currentlyForwardedConnections) ||
						(oldTransferedBytes != m_transferedBytes) ||
						(oldBandwidthUsage != m_currentBandwidthUsage))
					{
						somethingHasChanged = true;
					}
				}
			}
			if (interrupted == false)
			{
				if (somethingHasChanged == true)
				{
					/* notify the observers if something has changed */
					setChanged();
					notifyObservers(new JAPRoutingMessage(JAPRoutingMessage.SERVER_STATISTICS_UPDATED));
				}
				try
				{
					Thread.sleep(SERVER_STATISTICS_UPDATE_INTERVAL);
				}
				catch (InterruptedException e)
				{
					interrupted = true;
				}
			}
		}
	}

	/**
	 * This starts fetching the statistics from the forwarding server. First, all currently running
	 * fetch threads are stopped and then a new thread is started (if we are in server routing
	 * mode).
	 */
	private void startStatistics()
	{
		synchronized (this)
		{
			/* stop the running statistics thread, if there is one */
			stopStatistics();
			/* get the new statistics instance and start the statistics thread */
			m_currentStatisticsInstance = JAPModel.getInstance().getRoutingSettings().getSchedulerStatistics();
			if (m_currentStatisticsInstance != null)
			{
				m_statisticsThread = new Thread(this);
				m_statisticsThread.setDaemon(true);
				m_statisticsThread.start();
			}
		}
	}

	/**
	 * This stops the fetch thread of the forwarding server statistics. If there is no thread
	 * currently running, nothing is done.
	 */
	private void stopStatistics()
	{
		synchronized (this)
		{
			if (m_statisticsThread != null)
			{
				m_statisticsThread.interrupt();
				m_statisticsThread = null;
			}
			int oldRejectedConnections = m_rejectedConnections;
			int oldAcceptedConnections = m_acceptedConnections;
			int oldForwardedConnections = m_currentlyForwardedConnections;
			long oldTransferedBytes = m_transferedBytes;
			int oldBandwidthUsage = m_currentBandwidthUsage;
			m_currentStatisticsInstance = null;
			m_rejectedConnections = 0;
			m_acceptedConnections = 0;
			m_currentlyForwardedConnections = 0;
			m_transferedBytes = 0;
			m_currentBandwidthUsage = 0;
			if ( (oldRejectedConnections != m_rejectedConnections) ||
				(oldAcceptedConnections != m_acceptedConnections) ||
				(oldForwardedConnections != m_currentlyForwardedConnections) ||
				(oldTransferedBytes != m_transferedBytes) || (oldBandwidthUsage != m_currentBandwidthUsage))
			{
				/* something has changed -> notify the observers */
				setChanged();
				notifyObservers(new JAPRoutingMessage(JAPRoutingMessage.SERVER_STATISTICS_UPDATED));
			}
		}
	}

}
