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

package jap;

import gui.AWTUpdateQueue;
import gui.JAPHelpContext;

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.util.Observable;
import java.util.Observer;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.event.AncestorEvent;
import javax.swing.event.AncestorListener;

import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;

/**
 * This is the generic implementation for a JAP configuration module.
 */
public abstract class AbstractJAPConfModule implements JAPHelpContext.IHelpContext
{
	private final AWTUpdateQueue AWT_UPDATE_QUEUE = new AWTUpdateQueue(new Runnable()
	{
		public void run()
		{
			// synchronize with pack() of config dialog so that update and pack events do not overlap
			//synchronized (JAPConf.getInstance())
			{
				try
				{
					onUpdateValues();
					m_rootPanel.validate();
				}
				catch (Throwable a_e)
				{
					LogHolder.log(LogLevel.ALERT, LogType.GUI, a_e);
				}
			}
		}
	});

	/**
	 * This stores the root panel of this configuration tab. All elements of the configuration
	 * tab are placed on this panel (or subpanels).
	 */
	private JPanel m_rootPanel;

	/**
	 * The savepoint for this module. It is needed for restoring the old configuration, if the user
	 * presses "Cancel" or the default configuration, if the user presses "Reset to defaults".
	 */
	protected IJAPConfSavePoint m_savePoint;
	
	private boolean m_bObserversInitialised = false;
	protected final Object LOCK_OBSERVABLE = new Object();

	/**
	 * Helper class for creating a onRootPanelShown call when the root panel (the whole configuration
	 * tab of this module) is coming to foreground.
	 */
	private class RootPanelAncestorListener implements AncestorListener
	{
		/**
		 * This method is called when the root panel is set to visible. This only happens if the whole
		 * configuration tab of this module is set to visible.
		 *
		 * @param event The fired AncestorEvent.
		 */
		public void ancestorAdded(AncestorEvent event)
		{
			if (event.getAncestor() == getRootPanel())
			{
				if (getRootPanel().isVisible())
				{
					onRootPanelShown();
				}
			}
		}

		/**
		 * This method is called when the root panel is moved. This only happens if the whole
		 * configuration tab of this module is moved.
		 *
		 * @param event The fired AncestorEvent.
		 */
		public void ancestorMoved(AncestorEvent event)
		{
		}

		/**
		 * This method is called when the root panel is set to invisible. This only happens if the
		 * whole configuration tab of this module is set to invisible.
		 *
		 * @param event The fired AncestorEvent.
		 */
		public void ancestorRemoved(AncestorEvent event)
		{
		}
	}

	/**
	 * This is the constructor of AbstractJAPConfModule. It will be called by every child of this
	 * class. Every child have to do an super(a_moduleSavePoint) in the first line of its own
	 * constructor, where a_moduleSavePoint is the savepoint for this module.
	 *
	 * @param a_moduleSavePoint The savepoint for this module. If you supply null, there is no
	 *                          possibility for creating savepoints for this module. Also the
	 *                          default configuration for this module cannot be restored auto-
	 *                          matically.
	 */
	protected AbstractJAPConfModule(IJAPConfSavePoint a_moduleSavePoint)
	{
		m_rootPanel = new JPanel();
		m_rootPanel.addAncestorListener(new RootPanelAncestorListener());
		m_savePoint = a_moduleSavePoint;
		recreateRootPanel();
		FontSizeObserver observer = new FontSizeObserver();
		JAPModel.getInstance().addObserver(observer);
		fontSizeChanged(new JAPModel.FontResize(JAPModel.getInstance().getFontSize(),
												JAPModel.getInstance().getFontSize()),
						observer.getDummyLabel());
	}

	/**
	 * Creates constraints for an equal alignment of a root panel with tabs.
	 * @return GridBagConstraints
	 */
	public static GridBagConstraints createTabbedRootPanelContraints()
	{
		GridBagConstraints rootPanelConstraints = new GridBagConstraints();
		rootPanelConstraints.anchor = GridBagConstraints.NORTHWEST;
		rootPanelConstraints.fill = GridBagConstraints.BOTH;
		rootPanelConstraints.weightx = 1.0;
		rootPanelConstraints.weighty = 1.0;

		rootPanelConstraints.gridx = 0;
		rootPanelConstraints.gridy = 0;
		return rootPanelConstraints;
	}

	/**
	 * This method must be implemented by the children of AbstractJAPConfModule and returns
	 * the title for this configuration tab.
	 *
	 * @return The title for this configuration tab.
	 */
	public abstract String getTabTitle();

	/**
	 * This method must be implemented by the children of AbstractJAPConfModule. It is called every
	 * time the root panel needs to be (re)created (e.g. the language has changed). This method is
	 * also called by the constructor of AbstractJAPConfModule after creating the root panel.
	 */
	public abstract void recreateRootPanel();

	/**
	 * This returns the root panel for this configuration module. The children of
	 * AbstractJAPConfModule can use this method to insert elements on the root panel. This method
	 * is also called to connect the root panel with a JTabbedPane.
	 *
	 * @return The root panel of this configuration module.
	 */
	public final JPanel getRootPanel()
	{
		return m_rootPanel;
	}

	/**
	 * This will create a new savepoint with the current configuration. If the user later will
	 * press "Cancel", we can restore the current configuration. This method is called, every time,
	 * the configuration dialog is opened by the user.
	 */
	public final void createSavePoint()
	{
		if (m_savePoint != null)
		{
			m_savePoint.createSavePoint();
		}
	}

	/**
	 * This method is called every time the user presses the "OK" button. The onOkPressed() event
	 * handler is called by this method.
	 * @return true, if all values are ok and we can procceed; false otherwise
	 */
	public final boolean okPressed()
	{
		/* call the event handler */
		return onOkPressed();
	}

	/**
	 * May contain some logic that is executed when the font size changes. It should also
	 * contain all fixed font sizes, e.g. for JLabels.
	 * @param a_fontResize the resize factor
	 * @param a_dummyLabel a label to get the current JLabel font size from
	 */
	public void fontSizeChanged(JAPModel.FontResize a_fontResize, JLabel a_dummyLabel)
	{
	}

	/**
	 * This method is called every time the user presses the "Cancel" button. It contains the
	 * generic implementation for restoring the original configuration from the last savepoint. Also
	 * the onCancelPressed() event handler is called by this method.
	 */
	public final void cancelPressed()
	{
		if (m_savePoint != null)
		{
			m_savePoint.restoreSavePoint();
		}
		/* call the event handler */
		onCancelPressed();
	}

	/**
	 * This method is called every time the user presses the "Reset to defaults" button. It contains
	 * the generic implementation for restoring the default configuration from the savepoint. Also
	 * the onResetToDefaultsPressed() event handler is called by this method.
	 */
	public final void resetToDefaultsPressed()
	{
		if (m_savePoint != null)
		{
			m_savePoint.restoreDefaults();
		}
		/* call the event handler */
		onResetToDefaultsPressed();
	}

	/**
	 * This method is called, if something on the configuration data has changed and the module
	 * shall update its GUI. The update events are queued.
	 * @param a_bSync if the current thread should wait until the operation is performed
	 */
	public final void updateValues(boolean a_bSync)
	{
		if (this instanceof JAPConfAnon)
		{
		//	new Exception().printStackTrace();
		}
		/* call the event handler */
		AWT_UPDATE_QUEUE.update(a_bSync);
	}

	/**
	 * This method can be overwritten by the children of AbstractJAPConfModule. It is called
	 * every time the root panel comes to the foreground (is set to visible).
	 */
	protected void onRootPanelShown()
	{
	}

	/**
	 * This method can be overwritten by the children of AbstractJAPConfModule. It is called
	 * every time the user presses "OK" in the configuration dialog.
	 */
	protected boolean onOkPressed()
	{
		return true;
	}

	/**
	 * This method can be overwritten by the children of AbstractJAPConfModule. It is called
	 * every time the user presses "Cancel" in the configuration dialog after the restoring
	 * of the savepoint data (if there is a savepoint for this module).
	 */
	protected void onCancelPressed()
	{
	}

	/**
	 * This method can be overwritten by the children of AbstractJAPConfModule. It is called
	 * every time the user presses "Reset to defaults" in the configuration dialog after the
	 * restoring of the default configuration from the savepoint (if there is a savepoint for
	 * this module).
	 */
	protected void onResetToDefaultsPressed()
	{
	}

	/**
	 * This method can be overwritten by the children of AbstractJAPConfModule. It is called
	 * every time the values of the model have changed and must be reread by the module.
	 * SHOULD NOT BE CALLED DIRECTLY in subclasses!!
	 */
	protected void onUpdateValues()
	{
	}
	
	/**
	 * All observables that are observed by this object MUST be registered here.
	 * Subsequent calls of this method should not lead to additional registrations.
	 */
	protected boolean initObservers()
	{
		boolean bObserversInitialised = m_bObserversInitialised;
		m_bObserversInitialised = true;
		//System.out.println(ClassUtil.getShortClassName(getClass()));
		return !bObserversInitialised;
	}

	/**
	 * Observes changes of the font size.
	 */
	protected class FontSizeObserver implements Observer
	{
		private JLabel DUMMY_LABEL = new JLabel();

		public JLabel getDummyLabel()
		{
			return DUMMY_LABEL;
		}

		public void update(Observable a_observable, final Object a_message)
		{
			if (a_message instanceof JAPModel.FontResize && a_message != null)
			{
				SwingUtilities.updateComponentTreeUI(DUMMY_LABEL);
				fontSizeChanged((JAPModel.FontResize)a_message, DUMMY_LABEL);
			}
		}
	}

	public Component getHelpExtractionDisplayContext()
	{
		return JAPConf.getInstance().getContentPane();
	}
}
