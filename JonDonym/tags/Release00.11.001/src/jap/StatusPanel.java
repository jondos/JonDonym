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

import java.util.Random;

import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import anon.util.JAPMessages;

import gui.GUIUtils;
import gui.IStatusLine;
import logging.LogType;
import logging.LogLevel;
import logging.LogHolder;

/** A panel which display some status messages, one after each other*/
public class StatusPanel extends JPanel implements Runnable, IStatusLine
{
	private static final String MSG_CLICK_HERE = StatusPanel.class.getName() + "_clickHere";

	private final Object SYNC_MSG = new Object();
	private Random m_Random;
	private JLabel m_button;

	private final static int ICON_HEIGHT = 15;
	private final static int ICON_WIDTH = 16;

	private Image m_imageError;
	private Image m_imageInformation;
	private Image m_imageWarning;

	public static abstract class ButtonListener implements ActionListener
	{
		public boolean isButtonShown()
		{
			return true;
		}
	}

	private final class MessagesListNode
	{
		ActionListener listener;
		ButtonListener buttonAction;
		String m_Msg;
		Image m_Icon;
		int m_Id;
		MessagesListNode m_Next;
		int m_DisplayCount = -1;
	}

	private MessagesListNode m_firstMessage;
	private volatile boolean m_bRun;
	private volatile int m_aktY;
	private Thread m_Thread;

	public StatusPanel(JLabel a_button)
	{
		m_imageInformation = GUIUtils.loadImageIcon(JAPConstants.IMAGE_INFORMATION, true, false).getImage();
		m_imageError = GUIUtils.loadImageIcon(JAPConstants.IMAGE_ERROR, true, false).getImage();
		m_imageWarning = GUIUtils.loadImageIcon(JAPConstants.IMAGE_WARNING, true, false).getImage();
		m_button = a_button;
		if (m_button != null)
		{
			m_button.addMouseListener(new MouseAdapter()
			{
				public void mouseClicked(MouseEvent a_event)
				{
					boolean m_bClicked = false;

					ActionListener listener = null;
						if (m_bClicked)
						{
							return;
						}
						m_bClicked = true;

						synchronized (SYNC_MSG)
						{
							MessagesListNode entry = m_firstMessage;
							if (entry != null && entry.buttonAction != null)
							{
								listener = entry.buttonAction;
							}
						}
						if (listener != null)
						{
							listener.actionPerformed(new ActionEvent(StatusPanel.this, a_event.getID(),
								"mouseClicked"));
							StatusPanel.this.repaint();
						}

				m_bClicked = false;
				}
			});
		}

		addMouseListener(new MouseAdapter()
		{
			boolean m_bClicked = false;

			public void mouseClicked(MouseEvent a_event)
			{
				ActionListener listener = null;
				if (m_bClicked)
				{
					return;
				}
				m_bClicked = true;

				synchronized (SYNC_MSG)
				{
					MessagesListNode entry = m_firstMessage;
					if (entry != null)
					{
						listener = entry.listener;
					}
				}
				if (listener != null)
				{
					listener.actionPerformed(new ActionEvent(StatusPanel.this, a_event.getID(),
						"mouseClicked"));
					StatusPanel.this.repaint();
				}

				m_bClicked = false;
			}
		});

		m_Random = new Random();

		setLayout(null);
		//setFont(font);
		//setBackground(Color.red);
		//setSize(m_dimensionPreferredSize);
		m_firstMessage = null;
		m_Thread = new Thread(this, "StatusPanel");
		m_Thread.setDaemon(true);
		m_bRun = true;
		m_Thread.start();
	}

	public void finalize()
	{
		m_bRun = false;
		try
		{
			m_Thread.interrupt();
			m_Thread.join();
		}
		catch (Exception e)
		{
		}
	}

	/** Adds a message to be displayed in the status panel.
	 * @param type chose one of JOptionPane.*
	 * @param msg the message to be displayed
	 * @return an id useful for removing this message from the status panel
	 */
	public int addStatusMsg(String msg, int type, boolean bAutoRemove)
	{
		return addStatusMsg(msg, type, bAutoRemove, null, null);
	}

	public int addStatusMsg(String msg, int type, boolean bAutoRemove, ActionListener a_listener)
	{
		return addStatusMsg(msg, type, bAutoRemove, a_listener, null);
	}

	/** Adds a message to be displayed in the status panel.
	 * @param type chose one of JOptionPane.*
	 * @param msg the message to be displayed
	 * @return an id >= 0 useful for removing this message from the status panel
	 */
	public int addStatusMsg(String msg, int type, boolean bAutoRemove, ActionListener a_listener,
		ButtonListener a_ButtonListener)
	{
		MessagesListNode entry = null;
		synchronized (SYNC_MSG)
		{
			entry = new MessagesListNode();
			entry.listener = a_listener;
			entry.buttonAction = a_ButtonListener;
			entry.m_Msg = msg;
			entry.m_Id = Math.abs(m_Random.nextInt());
			if (bAutoRemove)
			{
				entry.m_DisplayCount = 2;
			}
			if (type == JOptionPane.WARNING_MESSAGE)
			{
				entry.m_Icon = m_imageWarning;
			}
			else if (type == JOptionPane.INFORMATION_MESSAGE)
			{
				entry.m_Icon = m_imageInformation;
			}
			else if (type == JOptionPane.ERROR_MESSAGE)
			{
				entry.m_Icon = m_imageError;
			}

			if (m_firstMessage == null)
			{
				m_firstMessage = entry;
				entry.m_Next = entry;
				m_aktY = ICON_HEIGHT;
			}
			else
			{
				entry.m_Next = m_firstMessage.m_Next;
				m_firstMessage.m_Next = entry;
			}
			m_Thread.interrupt(); //display next message
		}

		return entry.m_Id;

	}

	/** Removes a message from the ones which are displayed in the status panel
	 * @param id the message to be removed
	 */
	public void removeStatusMsg(int id)
	{
		synchronized (SYNC_MSG)
		{
			//messages are stored as a linked list, with the last entry pointing to the first, and m_Msgs being one node
			if (m_firstMessage == null) //we don't have any messages
			{
				LogHolder.log(LogLevel.DEBUG, LogType.PAY, "Could not remove message with id of " + id + " since there are no messages at all");
				m_aktY = ICON_HEIGHT;
				return;
			}
			if (m_firstMessage.m_Id == id && m_firstMessage.m_Next == m_firstMessage) //one element
			{
				// the currently shown message is being removed
				m_firstMessage = null;
				m_aktY = ICON_HEIGHT;
				m_Thread.interrupt(); //display next message
			}
			else
			{
				//more than one
				MessagesListNode curEntry = m_firstMessage;
				MessagesListNode prevEntry = null;
				while (curEntry != null)
				{
					if (curEntry.m_Next.m_Id == id)
					{
						prevEntry = curEntry;
						curEntry = curEntry.m_Next;
						break;
					}
					curEntry = curEntry.m_Next;
					if (curEntry == m_firstMessage) //back to the starting entry
					{
						return; //not found
					}
				}
				if (curEntry == m_firstMessage) //remove current entry
				{
					m_firstMessage = curEntry.m_Next;
					m_aktY = ICON_HEIGHT;
					m_Thread.interrupt(); //display changes
				}
				prevEntry.m_Next = curEntry.m_Next; //remove entry from list
			}
		}
	}

	public void paint(Graphics g)
	{
		if (g == null)
		{
			return;
		}
		super.paint(g);
		synchronized (SYNC_MSG)
		{
			if (m_firstMessage != null)
			{
				String msg = m_firstMessage.m_Msg;
				if (m_firstMessage.buttonAction != null && !m_button.isVisible())
				{
					m_button.setVisible(m_firstMessage.buttonAction.isButtonShown());
				}
				else if (m_firstMessage.buttonAction == null && m_button.isVisible())
				{
					m_button.setVisible(false);
				}

				if (m_firstMessage.listener != null)
				{
					setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
					msg += " (" + JAPMessages.getString(MSG_CLICK_HERE) + ")";
					setToolTipText(JAPMessages.getString(MSG_CLICK_HERE));
					//g.setColor(Color.blue);
				}
				else
				{
					//g.setColor(Color.black);
					setToolTipText(null);
					setCursor(Cursor.getDefaultCursor());
				}

				//baseline drawing
				g.drawString(msg, ICON_WIDTH + 2, g.getFont().getSize() - m_aktY);
				if (m_firstMessage.m_Icon != null)
				{
					// top-left drawing
					g.drawImage(m_firstMessage.m_Icon, 0, ((getSize().height  - m_firstMessage.m_Icon.getHeight(this)) / 2) - m_aktY, this);
				}
			}
			else
			{
				setToolTipText(null);
				setCursor(Cursor.getDefaultCursor());
				m_button.setVisible(false);
			}
		}
	}

	public Dimension getPreferredSize()
	{
		if (m_button != null)
		{
			return new Dimension(100, Math.max((int) (ICON_HEIGHT * 1.2), m_button.getSize().height));
		}
		else
		{
			return new Dimension(100, (int) (ICON_HEIGHT * 1.2));
		}
	}

	public Dimension getMinimumSize()
	{
		return getPreferredSize();
	}

	public void run()
	{
		MessagesListNode msg;
		
		try
		{
			while (m_bRun)
			{
				msg = null;
				
				try
				{
					Thread.sleep(10000);
				}
				catch (InterruptedException e)
				{
					if (!m_bRun)
					{
						return;
					}
				}

				synchronized (SYNC_MSG)
				{
					if (m_firstMessage != null && m_firstMessage.m_DisplayCount == 0)
					{
						removeStatusMsg(m_firstMessage.m_Id);
					}

					if (m_firstMessage == null)
					{
						repaint();
						continue;
					}
					if (m_firstMessage.m_DisplayCount > 0)
					{
						msg = m_firstMessage;
						m_firstMessage.m_DisplayCount--;
					}

					if (m_firstMessage == null)
					{
						m_aktY = ICON_HEIGHT;
						repaint();
						continue;
					}
					else if (m_firstMessage.m_Next == m_firstMessage && m_firstMessage.listener != null && m_aktY == 0)
					{
						// there are no other status messages; leave this one on top
						repaint();
						continue;
					}
					else
					{
						m_firstMessage = m_firstMessage.m_Next;
						m_aktY = ICON_HEIGHT;
					}
				}


				for (int i = 0; i < ICON_HEIGHT && m_bRun; i++)
				{
					try
					{
						Thread.sleep(100);
						m_aktY--;
						repaint();
					}
					catch (InterruptedException e)
					{
						synchronized (SYNC_MSG)
						{
							if (m_firstMessage != null)
							{
								if (m_firstMessage.m_DisplayCount >= 0 && m_firstMessage == msg)
								{
									// this message should get another chance to be displayed
									m_firstMessage.m_DisplayCount++;
								}
								m_aktY = ICON_HEIGHT;
								i = -1;
								m_firstMessage = m_firstMessage.m_Next;
							}
						}
					}
				}
			}
		}
		catch (Exception e)
		{
		}
	}
}
