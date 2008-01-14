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
package gui.dialog;

import java.awt.BorderLayout;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import javax.swing.JLabel;

import gui.GUIUtils;

/**
 * This is a dialog that executes a given Thread or Runnable if it is shown on screen. It has an optional
 * cancel button. A click on the dialog close button or on the cancel button will interrupt the Thread.
 * The Thread may watch for this event but does not need to. The behaviour if the Thread
 * is interrupted (isInterrupted() returns true) and has finished is the same as if a user clicks the
 * cancel button. You may therefore control this behaviour by defining another default button operation
 * for the cancel button and by overwriting the isInterrupted() method of your Thread.
 * If the Thread is not interrupted and has finished, the next content pane is shown. If there is no next
 * content pane, the dialog is closed according to the ON_CLICK button operation. By default, it is disposed.
 * <P>Warning: The Thread should not call dispose() on the dialog, as this may lead to a deadlock or
 * an Exception!</P>
 * @todo Modify the class so that a status bar (0-100%) can be shown, too.
 * @author Rolf Wendolsky
 */
public class WorkerContentPane extends DialogContentPane implements
	DialogContentPane.IWizardSuitableNoWizardButtons
{
	/** @todo rename the image according to coding standards */
	public static final String IMG_BUSY = "busy.gif";

	public static final String MSG_PLEASE_WAIT = WorkerContentPane.class.getName() + "_pleaseWait";

	public static final String DOTS = "...";

	private Thread m_workerThread;
	private Runnable m_workerRunnable;
	private Thread m_internalThread;
	private boolean m_bInterruptThreadSafe = true;

	public WorkerContentPane(JAPDialog a_parentDialog, String a_strText, Runnable a_workerRunnable)
	{
		this(a_parentDialog, a_strText, "", null, a_workerRunnable);
	}

	public WorkerContentPane(JAPDialog a_parentDialog, String a_strText, String a_strTitle,
							 Runnable a_workerRunnable)
	{
		this(a_parentDialog, a_strText, a_strTitle, null, a_workerRunnable);
	}

	public WorkerContentPane(JAPDialog a_parentDialog, String a_strText,
							 DialogContentPane a_previousContentPane, Runnable a_workerRunnable)
	{
		this(a_parentDialog, a_strText, "", a_previousContentPane, a_workerRunnable);
	}

	public WorkerContentPane(JAPDialog a_parentDialog, String a_strText, String a_strTitle,
							 DialogContentPane a_previousContentPane, Runnable a_workerRunnable)
	{
		super(a_parentDialog, a_strText, new Layout(a_strTitle),
			  new Options(OPTION_TYPE_CANCEL, a_previousContentPane));
		setDefaultButtonOperation(ON_CLICK_DISPOSE_DIALOG);
		m_workerRunnable = a_workerRunnable;

		getContentPane().setLayout(new BorderLayout());
		getContentPane().add(new JLabel(GUIUtils.loadImageIcon(IMG_BUSY, true)), BorderLayout.CENTER);

		addComponentListener(new WorkerComponentListener());
	}

	/**
	 * Returns true if the content pane only does a close or move operation if the thread has stopped.
	 * Otherwise, the close or move operation is performed if the thread has been interrupted by closing the
	 * dialog window or clicking on the cancel button. If you want this, the calling thread is recommended to
	 * call <CODE> joinThread() </CODE> to safely wait for the end of the thread before starting a new one.
	 * Remember that a call to updateDialog() of this content pane gives an undefined result if the previously
	 * startet thread has not stopped yet.
	 * @return true if the content pane only does a close or move operation if thetThread has stopped.
	 * Otherwise, the close or move operation is performed if the thread has been interrupted by closing the
	 * dialog window or clicking on the cancel button
	 */
	public final boolean isInterruptThreadSafe()
	{
		return m_bInterruptThreadSafe;
	}

	/**
	 * Defines this content pane should interrupt the wrapped thread safe or not if the user closed the parent
	 * dialog or clicked cancel.
	 * @param a_bInterruptThreadSafe true if the content pane only does a close or move operation if the
	 * thread has stopped. Otherwise, the close or move operation is performed if the thread has been
	 * interrupted by closing the dialog window or clicking on the cancel button. If you want this, the
	 * calling thread is recommended to call <CODE> joinThread() </CODE> to safely wait for the end of the
	 * thread before starting a new one. Remember that a call to updateDialog() of this content pane
	 * gives an undefined result if the previously started thread has not stopped yet.
	 */
	public final void setInterruptThreadSafe(boolean a_bInterruptThreadSafe)
	{
		m_bInterruptThreadSafe = a_bInterruptThreadSafe;
	}

	/**
	 * The caller waits until the thread has stopped. This is only needed if isInterruptThreadSafe()
	 * returns false and the user closes the dialog or clicks cancel, otherwise this is done automatically.
	 * Remember that a new thread started by this content pane is blocked until the old thread has stopped
	 * and, if startet, it will overwrite the getValue() result of the old thread.
	 */
	public final void joinThread()
	{
		try
		{
			if (m_workerThread != null)
			{
				m_workerThread.join();
			}
		}
		catch (InterruptedException a_e)
		{
		}
	}

	/**
	 * Returns if the content pane is ready to start a new thread. If it is not ready, no thread will
	 * be started when the content pane shows up. Subclasses may override this method to hide and show
	 * this content pane while a thread is running. Please note that this method should always return
	 * <CODE> true </CODE> after the thread has stopped.
	 * @return if the content pane is ready to start a new thread; true by default
	 */
	public boolean isReady()
	{
		return true;
	}

	/**
	 * Returns true by default, that means this worker content pane is skipped if a move from the next
	 * content pane to this one is done.
	 * @return true
	 */
	public boolean isSkippedAsPreviousContentPane()
	{
		return true;
	}

	/**
	 * Interrupts the Thread.
	 * @return CheckError[]
	 */
	public CheckError[] checkCancel()
	{
		interruptWorkerThread();
		return null;
	}

	public synchronized void dispose()
	{
		super.dispose();
		setInterruptThreadSafe(false);
		interruptWorkerThread();
		//m_workerThread = null;
		//m_workerRunnable = null;
		m_internalThread = null;
	}


	/**
	 * Interrupts the thread.
	 */
	public void interruptWorkerThread()
	{
		if (m_workerThread != null)
		{
			if (m_workerThread.isAlive() && !m_workerThread.isInterrupted())
			{
				m_workerThread.interrupt();
			}

			if (isInterruptThreadSafe())
			{
				joinThread();
			}
		}
	}

	/**
	 * A wrapper for the worker runnable.
	 */
	private class InternalThread extends Thread
	{
		private Runnable m_runnable;
		private boolean m_bInterrupted = false;

		public InternalThread(Runnable a_runnable)
		{
			super(a_runnable, "WorkerContentPane - InternalThread");
			m_runnable = a_runnable;
		}
		public void run()
		{
			m_runnable.run();
			m_bInterrupted = isInterrupted();
		}
		public boolean isInterrupted()
		{
			return super.isInterrupted() ||  m_bInterrupted;
		}
	}

	private class WorkerComponentListener extends ComponentAdapter implements Runnable
	{
		public void componentHidden(ComponentEvent a_event)
		{
			if (isReady())
			{
				interruptWorkerThread();
			}
		}

		/**
		 * Starts the internal Thread if the content pane becomes visible.
		 * If the Thread starts, the return value is reset to RETURN_VALUE_UNINITIALIZED. If the
		 * Thread is finished without interruption, the return value is set to RETURN_VALUE_OK.
		 * @param a_event ComponentEvent
		 */
		public void componentShown(ComponentEvent a_event)
		{
			if (isVisible() && isReady())
			{
				m_internalThread = new Thread(this,"WorkerContentPane - componentShown()");
				m_internalThread.setDaemon(true);
				m_internalThread.start();
			}
		}

		public synchronized void run()
		{
			setButtonValue(RETURN_VALUE_UNINITIALIZED);
			m_workerThread = new InternalThread(m_workerRunnable);
			m_workerThread.setPriority(Thread.MIN_PRIORITY);
			try
			{
				// give the GUI some extra time to show up
				Thread.sleep(200);
			}
			catch (InterruptedException a_e)
			{
				interruptWorkerThread();
				m_workerThread = null;
				moveToPreviousContentPane();
				notifyAll();
				return;
			}
			//m_workerThread.setDaemon(true);
			m_workerThread.start();


			try
			{
				m_workerThread.join();
			}
			catch (InterruptedException a_e)
			{
				interruptWorkerThread();
			}


			if (m_workerThread.isInterrupted() || getButtonValue() == RETURN_VALUE_CANCEL ||
				getButtonValue() == RETURN_VALUE_CLOSED)
			{
				// don't do anything if the cancel (or any other) button was clicked
				if (getButtonValue() == RETURN_VALUE_UNINITIALIZED)
				{
					if ( (getDefaultButtonOperation() &
						  (ON_CLICK_DISPOSE_DIALOG | ON_CANCEL_DISPOSE_DIALOG)) > 0)
					{
						closeDialog(true);
					}
					else if ((getDefaultButtonOperation() &
							  (ON_CLICK_HIDE_DIALOG | ON_CANCEL_HIDE_DIALOG)) > 0)
					{
						closeDialog(false);
					}
					else if ((getDefaultButtonOperation() &
							  (ON_CLICK_SHOW_PREVIOUS_CONTENT | ON_CANCEL_SHOW_PREVIOUS_CONTENT)) > 0)
					{
						moveToPreviousContentPane();
					}
					else if ((getDefaultButtonOperation() &
							  (ON_CLICK_SHOW_NEXT_CONTENT | ON_CANCEL_SHOW_NEXT_CONTENT)) > 0)
					{
						moveToNextContentPane();
					}
				}
			}
			else
			{
				interruptWorkerThread();
				setButtonValue(RETURN_VALUE_OK);
				moveToNextContentPane();
			}

			m_workerThread = null;
			notifyAll();
		}
	}

	/**
	 * If an instance of IReturnRunnable is run, this method returns the result of IReturnRunnable.getValue().
	 * Otherwise, <code>null</code> is returned.
	 * @return the result of IReturnRunnable.getValue()
	 */
	public Object getValue()
	{
		if (m_workerRunnable instanceof IReturnRunnable)
		{
			return ( (IReturnRunnable) m_workerRunnable).getValue();
		}
		else
		{
			return null;
		}
	}

	/**
	 * Implement this interface if you want your runnable object to return some kind of value.
	 */
	public static interface IReturnRunnable extends Runnable
	{
		public Object getValue();
	}

}
