/*
 Copyright (c) 2000 - 2006, The JAP-Team
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
package anon.util;

import java.util.Vector;

import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;

/**
 * The job queue is meant for threads that are executed very often and do the same things, e.g.
 * updating the screen with different values. At all times, only one job in the queue is executed,
 * and the queue allows no more than two jobs in total (one running and one waiting). If more than two
 * jobs are added, any job in the queue that is currently not running is removed and the new job is
 * added. It will then be executed right after the running job has finished, or it is removed if
 * another job is added meanwhile.
 *
 * @author Rolf Wendolsky
 */
public class JobQueue
{
	private Vector m_jobs;
	private Vector m_jobThreads;

	private Thread m_threadQueue;
	private boolean m_bInterrupted = false;

	private Job m_currentJob;
	private Thread m_currentJobThread;

	/**
	 * Creates and starts the job queue.
	 * @param a_name name of the queue
	 */
	public JobQueue(String a_name)
	{
		m_jobs = new Vector();
		m_jobThreads = new Vector();
		m_threadQueue = new Thread(new Runnable()
		{
			public void run()
			{
				synchronized (Thread.currentThread())
				{
					while (!Thread.currentThread().isInterrupted() && !m_bInterrupted)
					{
						try
						{
							Thread.currentThread().wait();
						}
						catch (InterruptedException ex)
						{
						}
						if (Thread.currentThread().isInterrupted())
						{
							Thread.currentThread().notifyAll();
							break;
						}

						// There is a new job!
						if (m_jobs.size() > 0 &&
							m_currentJob == m_jobs.firstElement() &&
							m_currentJobThread.isAlive())
						{
							// a job is currently running; 
							if (((Job) m_jobs.lastElement()).isInterrupting())
							{
								// interrupt the running job 
								while (m_currentJobThread.isAlive())
								{
									m_currentJobThread.interrupt();
									try 
									{
										Thread.currentThread().wait(100);
									} 
									catch (InterruptedException e) 
									{
										break;
									}
								}
							}
							else 
							{
								// wait for the end/notify of the running job
								continue;
							}
						}
						else if (m_jobs.size() > 0)
						{
							// no job is running; remove all jobs that are outdated
							while (m_jobs.size() > 1)
							{
								m_jobs.removeElementAt(0);
								m_jobThreads.removeElementAt(0);
							}

							// start the newest job
							m_currentJob = (Job) m_jobs.elementAt(0);
							m_currentJobThread = (Thread) m_jobThreads.elementAt(0);
							m_currentJobThread.start();
						}
					}
					// stop all threads
					while (m_jobs.size() > 0)
					{
						if (m_currentJob == m_jobs.firstElement())
						{
							m_currentJobThread.interrupt();

							try
							{
								Thread.currentThread().wait(500);
							}
							catch (InterruptedException ex1)
							{
							}
						}
						else
						{
							m_jobs.removeAllElements();
							m_jobThreads.removeAllElements();
						}
					}
				}
			}
		}, a_name);
		m_threadQueue.setDaemon(true);
		m_threadQueue.start();
	}

	/**
	 * A job that may be added to the job queue.
	 */
	public static abstract class Job implements Runnable
	{
		private boolean m_bMayBeSkippedIfDuplicate;
		private JobQueue m_queue;

		/**
		 * Creates a new job.
		 * @param a_bMayBeSkippedIfDuplicate if this thread is skipped if another thread with the same
		 * value already is in the queue
		 */
		public Job(boolean a_bMayBeSkippedIfDuplicate)
		{
			m_bMayBeSkippedIfDuplicate = a_bMayBeSkippedIfDuplicate;
		}

		/**
		 * Creates a new job for that mayBeSkippedIfDuplicate() returns false.
		 */
		public Job()
		{
			this(false);
		}

		/**
		 * Replaces the run method as the normal run method is implemented by Job itself. This method
		 * holds the things the job should do.
		 */
		public abstract void runJob();

		public final void run()
		{
			runJob();
			if (m_queue != null)
			{
				m_queue.removeJob(this, true);
			}
		}

		/**
		 * Optional message for a log entry that is generated if a new job was added and this value is not
		 * null.
		 * @return optional message for a log entry that is generated if a new job was added
		 */
		public String getAddedJobLogMessage()
		{
			return null;
		}

		public boolean isInterrupting()
		{
			return false;
		}
		
		/**
		 * If a new thread is added to the queue and mayBeSkippedIfDuplicate() returns true, it is skipped if
	     * another thread with the same value already is in the queue.
		 * @return if this thread is skipped if another thread with the same value already is in the queue
		 */
		public final boolean isSkippedIfDuplicate()
		{
			return m_bMayBeSkippedIfDuplicate;
		}
	}

	/**
	 * Adds a new job to the queue that is run as soon as there are no other running threads
	 * left in the queue. If for a job mayBeSkippedIfDuplicate() returns true, it is skipped if
	 * another thread with the same value already is in the queue.
	 * @param a_anonJob a Job
	 */
	public void addJob(final Job a_anonJob)
	{
		Thread jobThread;
		Job job;

		if (a_anonJob == null)
		{
			return;
		}

		if (!a_anonJob.isSkippedIfDuplicate() && m_bInterrupted)
		{
			// only jobs that may be skipped are allowed when thread is interrupted
			return;
		}

		if (a_anonJob.m_queue != null)
		{
			// this job already is in a queue
			return;
		}

		synchronized (m_threadQueue)
		{
			if (a_anonJob.isSkippedIfDuplicate() && m_jobs.contains(a_anonJob))
			{
				// do not accept duplicate jobs
				return;
			}

			if (m_jobs.size() > 0)
			{
				/* check whether this is job is different to the last one */
				Job lastJob = (Job) (m_jobs.lastElement());
				if (lastJob.isSkippedIfDuplicate() && a_anonJob.isSkippedIfDuplicate())
				{
					/* It's the same as the last job and may be skipped; skip it! */
					return;
				}
				else 
				{
					// remove all jobs that are not currently running
					while (m_jobs.size() > 0)
					{
						job = (Job)m_jobs.lastElement();
						if (job == m_currentJob && m_currentJobThread.isAlive())
						{
							// do not delete a running job; finish it and then start this new job
							break;
						}
						removeJob(job, false);
					}
				}
			}
			
			jobThread = new Thread(a_anonJob);
			jobThread.setDaemon(true);
			a_anonJob.m_queue = this;
			m_jobs.addElement(a_anonJob);
			m_jobThreads.addElement(jobThread);
			m_threadQueue.notify();

			String logMessage = a_anonJob.getAddedJobLogMessage();
			if (logMessage != null)
			{
				LogHolder.log(LogLevel.DEBUG, LogType.MISC, logMessage);
			}
		}
	}

	/**
	 * Stops the queue once and for all and interrupts all running threads. After calling
	 * this method, it will not accept any new threads.
	 */
	public void stop()
	{
		while (m_threadQueue.isAlive())
		{
			m_threadQueue.interrupt();
			synchronized (m_threadQueue)
			{
				m_bInterrupted = true;
				m_threadQueue.notifyAll();
				m_threadQueue.interrupt();
			}
			try
			{
				m_threadQueue.join(500);
			}
			catch (InterruptedException a_e)
			{
				// ignore
			}
		}
	}

	/**
	 * Must (!!) be called at the end of a job thread to remove this job from the queue and to tell the
	 * queue that the job was removed, so that a new job may be started.
	 * @param a_anonJob a Job to be removed
	 */
	private void removeJob(final Job a_anonJob, boolean a_bNotifyQueue)
	{
		if (a_anonJob == null)
		{
			return;
		}
		synchronized (m_threadQueue)
		{
			int index = m_jobs.indexOf(a_anonJob);

			if (index >= 0)
			{
				((Thread)m_jobThreads.elementAt(index)).interrupt();
				m_jobs.removeElementAt(index);
				m_jobThreads.removeElementAt(index);
				if (a_bNotifyQueue)
				{
					m_threadQueue.notify();
				}
			}
		}
	}
}
