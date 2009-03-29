/*
 Copyright (c) 2006, The JAP-Team
 All rights reserved.
 Redistribution and use in source and binary forms, with or without modification,
 are permitted provided that the following conditions are met:

  - Redistributions of source code must retain the above copyright notice,
 this list of conditions and the following disclaimer.

  - Redistributions in bisnary form must reproduce the above copyright notice,
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
package anon.util;

import java.util.Observable;
import java.util.Observer;

import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;

/**
 * Updates the local database. This may be done automatically (by a background thread) and manually
 * by method call. The automatic update is only done if this is allowed by the model.
 * @author Rolf Wendolsky
 */
public abstract class Updater implements Observer
{
	private static final long MIN_WAITING_TIME_MS = 20000;

	private IUpdateInterval m_updateInterval;
	private Thread m_updateThread;
	private boolean m_bAutoUpdateChanged = false;
	private boolean m_bInitialRun = true;
	// patch for JDK 1.1.8 and JView
	private boolean m_interrupted = false;
	private boolean m_bUpdating = false;
	private Object UPDATE_SYNC = new Object();
	private ObservableInfo m_observable;

	/**
	 * Initialises and starts the database update thread.
	 */
	public Updater(IUpdateInterval a_updateInterval, ObservableInfo a_observable)
	{
		if (a_updateInterval == null)
		{
			throw new IllegalArgumentException("No update interval specified!");
		}
		else if (a_observable == null)
		{
			throw new IllegalArgumentException("No ObservableInfo specified!");
		}
		/*if (a_updateInterval.getUpdateInterval() <= 1000)
		{
			throw new IllegalArgumentException(
						 "Database update interval of " + a_updateInterval + " is too short!");
		}*/
		m_observable = a_observable;
		m_updateInterval = a_updateInterval;
		init();
	}

	public static abstract class ObservableInfo
	{
		private Observable m_observable;

		public ObservableInfo(Observable a_observable)
		{
			if (a_observable == null)
			{
				throw new IllegalArgumentException("No Observable specified!");
			}
			m_observable = a_observable;
		}

		public void notifyAdditionalObserversOnUpdate(Class a_updatedClass)
		{
			
		}
		
		public boolean updateImmediately()
		{
			return false;
		}
		
		public final Observable getObservable()
		{
			return m_observable;
		}
		public abstract Integer getUpdateChanged();
		public abstract boolean isUpdateDisabled();
	}



	protected static class DynamicUpdateInterval implements IUpdateInterval
	{
		private long m_updateInterval;

		public DynamicUpdateInterval(long a_updateInterval)
		{
			setUpdateInterval(a_updateInterval);
		}

		public void setUpdateInterval(long a_updateInterval)
		{
			m_updateInterval = a_updateInterval;
		}

		public long getUpdateInterval()
		{
			return m_updateInterval;
		}
	}

	/**
	 * May be used to re-initialise the thread after stopping it.
	 */
	private final void init()
	{
		//stop();
		m_observable.getObservable().addObserver(this);
		m_updateThread = new Thread(new Runnable()
		{
			public void run()
			{
				long lastUpdate = System.currentTimeMillis();
				long waitingTime;
				LogHolder.log(LogLevel.INFO, LogType.THREAD,
							  getUpdatedClassName() + "update thread started.");
				while (!Thread.currentThread().isInterrupted() && !m_interrupted)
				{
					synchronized (Thread.currentThread())
					{
						m_bAutoUpdateChanged = true; // this is important to switch waiting times
						while (m_bAutoUpdateChanged)
						{
							m_bAutoUpdateChanged = false; // normally, this should be false after first call
							try
							{
								Thread.currentThread().notify();
								if (m_observable.isUpdateDisabled() || m_bInitialRun)
								{
									Thread.currentThread().wait();
								}
								else
								{
									waitingTime = Math.max(m_updateInterval.getUpdateInterval() -
										(System.currentTimeMillis() - lastUpdate), MIN_WAITING_TIME_MS);
									LogHolder.log(LogLevel.NOTICE, LogType.THREAD,
										"Update waiting time for " + getUpdatedClass().getName() +
										": " + waitingTime);
									Thread.currentThread().wait(waitingTime);
								}
							}
							catch (InterruptedException a_e)
							{
								Thread.currentThread().notifyAll();
								break;
							}
							if (Thread.currentThread().isInterrupted())
							{
								Thread.currentThread().notifyAll();
								break;
							}
							// patch for JDK 1.1.8 and JView
							if (m_interrupted)
							{
								break;
							}
						}
					}

					if (!Thread.currentThread().isInterrupted() && !m_interrupted && !isUpdatePaused())
					{
						LogHolder.log(LogLevel.INFO, LogType.THREAD,
									  "Updating " + getUpdatedClassName() + "list.");
						lastUpdate = System.currentTimeMillis();
						updateInternal();
					}
				}
				LogHolder.log(LogLevel.INFO, LogType.THREAD,
							  getUpdatedClassName() + "update thread stopped.");
			}
		}, getUpdatedClassName() + "Update Thread");
		m_updateThread.setPriority(Thread.MIN_PRIORITY);
		m_updateThread.setDaemon(true);
		m_updateThread.start();
	}
	
	protected  ObservableInfo getObservableInfo() 
	{
		return m_observable;
	}

	public void update(Observable a_observable, Object a_argument)
	{
		if (!(a_argument instanceof Integer) ||
			!((Integer)a_argument).equals(m_observable.getUpdateChanged()))
		{
			return;
		}
		
		final Updater updater = this;
		if (!m_observable.isUpdateDisabled())
		{
			new Thread(new Runnable()
			{
				public void run()
				{
					if (m_observable.updateImmediately())
					{
						updater.update(false);
					}
					else
					{
						updater.start(false);
					}
				}
			}).start();
		}
	}

	/**
	 * Starts the thread if it has not already started or has been stopped before.
	 */
	public final void start(boolean a_bSynchronized)
	{
		synchronized (UPDATE_SYNC)
		{
			if (m_bUpdating)
			{
				return;
			}
			m_bUpdating = true;
		}

		synchronized (this)
		{
			synchronized (m_updateThread)
			{
				m_bAutoUpdateChanged = true;
				m_bInitialRun = false;
				m_updateThread.notifyAll();
				if (a_bSynchronized)
				{
					try
					{
						m_updateThread.wait();
					}
					catch (InterruptedException ex)
					{
					}
				}
			}
		}
		synchronized (UPDATE_SYNC)
		{
			m_bUpdating = false;
		}
	}

	/**
	 * Force a synchronized update of the known database entries.
	 * @return true if the update was successful, false otherwise
	 */
	public final boolean update()
	{
		return update(true);
	}

	/**
	 * Force an update of the known database entries. The current thread does not wait until it is done.
	 * @return true if the update was successful, false otherwise
	 */
	public final void updateAsync()
	{
		Thread run = new Thread(new Runnable()
		{
			public void run()
			{
				update(false);
			}
		});
		run.setDaemon(true);
		run.start();
	}

	/**
	 * Force an update of the known database entries.
	 * @param a_bSynchronized true if the current thread should wait until the update is done; false otherwise
	 * @return true if the update was successful, false otherwise
	 */
	private final boolean update(boolean a_bSynchronized)
	{
		if (m_bInitialRun)
		{
			start(true);
		}

		synchronized (this)
		{
			synchronized (m_updateThread)
			{
				m_bAutoUpdateChanged = false;
				m_updateThread.notifyAll();
				if (a_bSynchronized)
				{
					try
					{
						m_updateThread.wait();
					}
					catch (InterruptedException a_e)
					{
						return false;
					}
					return wasUpdateSuccessful();
				}
				return true;
			}
		}
	}

	/**
	 * Stops the update thread. No further updates are possible.
	 */
	public final void stop()
	{
		m_observable.getObservable().deleteObserver(this);
		if (m_updateThread == null)
		{
			// no initialising has been done until now
			return;
		}

		while (m_updateThread.isAlive())
		{
			m_updateThread.interrupt();
			synchronized (m_updateThread)
			{
				m_bAutoUpdateChanged = false;
				m_bInitialRun = false;
				m_interrupted = true;
				m_updateThread.notifyAll();
				m_updateThread.interrupt();
			}
			try
			{
				m_updateThread.join(500);
			}
			catch (InterruptedException a_e)
			{
				// ignore
			}
		}
	}

	public final IUpdateInterval getUpdateInterval()
	{
		return m_updateInterval;
	}

	public abstract Class getUpdatedClass();

	protected abstract boolean wasUpdateSuccessful();

	protected static final class ConstantUpdateInterval implements IUpdateInterval
	{
		private long m_updateInterval;
		public ConstantUpdateInterval(long a_updateInterval)
		{
			m_updateInterval = a_updateInterval;
		}

		public long getUpdateInterval()
		{
			return m_updateInterval;
		}
	}

	protected static interface IUpdateInterval
	{
		long getUpdateInterval();
	}

	public abstract boolean isFirstUpdateDone();
	
	/**
	 * Does the update and should tell if it was successful or not.
	 */
	protected abstract void updateInternal();

	/**
	 * May be overwritten if an update is currently no wanted.
	 * @return boolean
	 */
	protected boolean isUpdatePaused()
	{
		return false;
	}

	protected final String getUpdatedClassName()
	{
		return ClassUtil.getShortClassName(getUpdatedClass()) + " ";
	}
}
