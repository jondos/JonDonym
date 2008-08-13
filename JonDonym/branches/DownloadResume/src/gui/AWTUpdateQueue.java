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
package gui;

import javax.swing.SwingUtilities;

import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;

/**
 * Queues update operations for the AWT Event Thread. There are never more than two
 * operations in the queue, as additional operations will be skipped immediatly.
 * The queue should therefore contain only update operations for the same GUI elements.
 *
 * @author Rolf Wendolsky
 */
public class AWTUpdateQueue
{
	private Runnable m_awtRunnable;
	private int m_jobs;
	private Object JOB_LOCK = new Object();
	private Object UPDATE_LOCK = new Object();

	public AWTUpdateQueue(Runnable a_awtRunnable)
	{
		m_awtRunnable = a_awtRunnable;
		m_jobs = 0;
	}

	public void update(boolean a_bSync)
	{
		synchronized (JOB_LOCK)
		{
			if (m_jobs >= 2 && !a_bSync)
			{
				// queue is full, skip update
				return;
			}
			m_jobs++;
		}

		Thread run = new Thread(new Runnable()
		{
			public void run()
			{
				doUpdateQueue();
			}
		});
		run.setDaemon(true);
		run.start();
		if (a_bSync)
		{
			try
			{
				run.join();
			}
			catch (InterruptedException a_e)
			{
				LogHolder.log(LogLevel.ERR, LogType.GUI, a_e);
			}
		}
	}

	private void doUpdateQueue()
	{
		Runnable run = new Runnable()
		{
			public void run()
			{
				m_awtRunnable.run();
				m_jobs--;
			}
		};

		synchronized (UPDATE_LOCK)
		{
			try
			{
				SwingUtilities.invokeAndWait(run);
			}
			catch (Exception a_e)
			{
				LogHolder.log(LogLevel.EXCEPTION, LogType.GUI, a_e);
			}
		}
	}















}
