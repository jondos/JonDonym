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

import java.util.Enumeration;
import java.util.Observable;
import java.util.Observer;
import java.util.Vector;

import javax.swing.table.AbstractTableModel;

import anon.util.JAPMessages;

import forward.server.ServerSocketPropagandist;

/**
 * This is the implementation of the infoservice registration table data, showed in the server
 * status box. The status values in the table are updated automatically, when they have been
 * changed.
 */
public class JAPRoutingInfoServiceRegistrationTableModel extends AbstractTableModel implements Observer
{

	/**
	 * serial version UID
	 */
	private static final long serialVersionUID = 1L;
	/**
	 * This is the list of all known propaganda instances, displayed in the table.
	 */
	private Vector m_propagandaInstances;

	/**
	 * Creates a new instance of JAPRoutingInfoServiceRegistrationTableModel. We do only some
	 * initialization here.
	 */
	public JAPRoutingInfoServiceRegistrationTableModel()
	{
		m_propagandaInstances = new Vector();
	}

	/**
	 * Updates the list of all displayed propaganda instances. We add only new unknown instances
	 * here, because removing of the old ones is done automatically, when they are stopped.
	 *
	 * @param a_newPropagandaInstancesList A Vector with propaganda instances. The new ones are
	 *                                     added to the internal list.
	 */
	public void updatePropagandaInstancesList(Vector a_newPropagandaInstancesList)
	{
		Enumeration propagandists = a_newPropagandaInstancesList.elements();
		synchronized (m_propagandaInstances)
		{
			int addedRows = 0;
			while (propagandists.hasMoreElements())
			{
				/* removing old propaganda instances is not done here, because they are removed
				 * automatically, when they reach the status HALTED and notify us
				 */
				ServerSocketPropagandist currentPropagandist = (ServerSocketPropagandist) (propagandists.
					nextElement());
				if (m_propagandaInstances.contains(currentPropagandist) == false)
				{
					/* observe the added propagandist, no problem also, if we already observe this
					 * propagandist, then addObserver() does nothing
					 */
					currentPropagandist.addObserver(this);
					if (currentPropagandist.getCurrentState() != ServerSocketPropagandist.STATE_HALTED)
					{
						/* add only the new propagandists to the list of all known propaganda instances */
						m_propagandaInstances.addElement(currentPropagandist);
						addedRows++;
					}
					else
					{
						/* the propagandist was stopped in the meantime -> don't add it and stop observing */
						currentPropagandist.deleteObserver(this);
					}
				}
			}
			if (addedRows > 0)
			{
				/* update the table */
				fireTableRowsInserted(m_propagandaInstances.size() - addedRows,
									  m_propagandaInstances.size() - 1);
			}
		}
	}

	/**
	 * This is the implementation of the observer of the propaganda instances. So if the instances
	 * change the state, the table is updated automatically. If the instances reach the state
	 * HALTED, they are removed from the table.
	 *
	 * @param a_notifier The propaganda instance, which has changed the state.
	 * @patam a_message The notification message (should be null).
	 */
	public void update(Observable a_notifier, Object a_message)
	{
		synchronized (m_propagandaInstances)
		{
			if (m_propagandaInstances.contains(a_notifier))
			{
				/* the notifier is in the list of known forwarding server propagandists */
				if ( ( (ServerSocketPropagandist) a_notifier).getCurrentState() ==
					ServerSocketPropagandist.STATE_HALTED)
				{
					/* remove it from the list and stop observing the propagandist */
					a_notifier.deleteObserver(this);
					int row = m_propagandaInstances.indexOf(a_notifier);
					m_propagandaInstances.removeElement(a_notifier);
					/* update the table */
					fireTableRowsDeleted(row, row);
				}
				else
				{
					/* the state of the propagandist was changed, but not to HALTED */
					int row = m_propagandaInstances.indexOf(a_notifier);
					/* update the table */
					fireTableRowsUpdated(row, row);
				}
			}
		}
	}

	/**
	 * Returns the number of rows in the infoservice registration table. This is equal to the number
	 * of propaganda instances.
	 *
	 * @return The number of rows in the infoservice registration table.
	 */
	public int getRowCount()
	{
		return m_propagandaInstances.size();
	}

	/**
	 * Returns the number of columns in the infoservice registration table. This is always 2, where
	 * column 0 is the name of the infoservice and column 2 is the connection state.
	 *
	 * @return The number of columns in the infoservice registration table.
	 */
	public int getColumnCount()
	{
		return 2;
	}

	/**
	 * Returns the name of the specified column. The name is resolved via JAPMessages.
	 *
	 * @param a_column The number of the column to get the name for. If this is not a valid column
	 *                 number, null is returned.
	 *
	 * @return The name for the column.
	 */
	public String getColumnName(int a_column)
	{
		String returnValue = null;
		if (a_column == 0)
		{
			returnValue = JAPMessages.getString("routingInfoServiceRegistrationTableColumn0Name");
		}
		if (a_column == 1)
		{
			returnValue = JAPMessages.getString("routingInfoServiceRegistrationTableColumn1Name");
		}
		return returnValue;
	}

	/**
	 * Returns a value of a cell in the table. In column 0 always the names of the infoservices
	 * and in column 1 always the registration status appear. If the specified values for row
	 * or column are outside the borders of the table, null is returned.
	 *
	 * @param a_row The row (propaganda instance) to get the value for.
	 * @param a_column The column to get the value for.
	 *
	 * @return A String with the value of the specified cell.
	 */
	public Object getValueAt(int a_row, int a_column)
	{
		String returnValue = null;
		ServerSocketPropagandist selectedPropagandist = (ServerSocketPropagandist) (m_propagandaInstances.
			elementAt(a_row));
		if (a_column == 0)
		{
			/* the name of the infoservice is requested */
			returnValue = selectedPropagandist.getInfoService().getName();
		}
		if (a_column == 1)
		{
			/* the state of the propagandist is requested */
			if (selectedPropagandist.getCurrentState() == ServerSocketPropagandist.STATE_REGISTERED)
			{
				returnValue = JAPMessages.getString("routingInfoServiceRegistrationTableStateRegistrated");
			}
			if (selectedPropagandist.getCurrentState() == ServerSocketPropagandist.STATE_CONNECTING)
			{
				returnValue = JAPMessages.getString("routingInfoServiceRegistrationTableStateConnecting");
			}
			if (selectedPropagandist.getCurrentState() == ServerSocketPropagandist.STATE_RECONNECTING)
			{
				returnValue = JAPMessages.getString("routingInfoServiceRegistrationTableStateReconnecting");
			}
			if (selectedPropagandist.getCurrentState() == ServerSocketPropagandist.STATE_HALTED)
			{
				/* can only occur within a very short time, because if state HALTED is reached, the
				 * propagandist is removed from the list, but it is possible, if another thread is
				 * trying to read the value just at that moment, the propagandist has reached the state
				 * HALTED
				 */
				returnValue = JAPMessages.getString("routingInfoServiceRegistrationTableStateHalted");
			}
		}
		return returnValue;
	}

	/**
	 * Removes all propaganda instances from the table. Also observing of those propaganda instances
	 * is stopped.
	 */
	public void clearPropagandaInstancesTable()
	{
		synchronized (m_propagandaInstances)
		{
			int propagandaInstancesCount = m_propagandaInstances.size();
			if (propagandaInstancesCount > 0)
			{
				Enumeration propagandaInstances = m_propagandaInstances.elements();
				while (propagandaInstances.hasMoreElements())
				{
					ServerSocketPropagandist currentPropagandaInstance = (ServerSocketPropagandist) (
						propagandaInstances.nextElement());
					/* stop observing the propagandist */
					currentPropagandaInstance.deleteObserver(this);
				}
				/* clear the whole table */
				m_propagandaInstances.removeAllElements();
				fireTableRowsDeleted(0, propagandaInstancesCount - 1);
			}
		}
	}

}
