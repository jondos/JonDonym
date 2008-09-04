/*
 Copyright (c) 2000, The JAP-Team
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

import java.util.Vector;

import java.awt.AWTEvent;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.IllegalComponentStateException;
import java.awt.Point;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import java.awt.event.MouseEvent;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JList;
import javax.swing.JPopupMenu;
import javax.swing.ListCellRenderer;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;

import anon.infoservice.Database;
import anon.infoservice.MixCascade;
import anon.infoservice.NewCascadeIDEntry;
import gui.GUIUtils;
import gui.JAPMessages;
import javax.swing.ImageIcon;

public class JAPMixCascadeComboBox extends JComboBox
{
	final static String ITEM_AVAILABLE_SERVERS = "ITEM_AVAILABLE_SERVERS";
	final static String ITEM_NO_SERVERS_AVAILABLE = "ITEM_NO_SERVERS_AVAILABLE";
	private MixCascade m_currentCascade;
	private JPopupMenu m_comboPopup;

	public JAPMixCascadeComboBox()
	{
		super();
		setModel(new JAPMixCascadeComboBoxModel());
		setRenderer(new JAPMixCascadeComboBoxListCellRender());
		setEditable(false);
		removeAllItems();
	}

	public void addItem(Object o)
	{
	}


	public MixCascade getMixCascade()
	{
		return m_currentCascade;
	}

	public void showStaticPopup()
	{
		if (m_comboPopup != null)
		{
			m_comboPopup.setVisible(true);
		}
		else
		{
			super.showPopup();
		}
	}

	public boolean isPopupVisible()
	{
		if (m_comboPopup != null)
		{
			return m_comboPopup.isVisible();
		}
		return false;
	}

	public synchronized void setMixCascade(MixCascade cascade)
	{
		if (m_currentCascade == cascade)
		{
			return;
		}

		if (m_currentCascade == null)
		{
			removeAllItems();
		}
		else
		{
			super.removeItem(m_currentCascade);
		}

		m_currentCascade = cascade;
		if (m_currentCascade != null)
		{
			super.addItem(cascade);
		}
	}

	public void removeAllItems()
	{
		//Note: We do not use super.removeAllItems() here because it does not correctly
		//reset the selected item at least on SUN JDK 1.4.1 !
		setModel(new JAPMixCascadeComboBoxModel());
		Vector trustModels = TrustModel.getTrustModels();
		TrustModel model;
		//super.addItem(ITEM_AVAILABLE_SERVERS); // this is not needed any more
		for (int i = 0; i < trustModels.size(); i++)
		{
			model = (TrustModel)trustModels.elementAt(i);
			if (model.isAdded())
			{
				super.addItem(model);
			}
		}
	}

	public void setNoDataAvailable()
	{
		super.insertItemAt(ITEM_NO_SERVERS_AVAILABLE, 1);
	}

	public Dimension getPreferredSize()
	{
		Dimension d = super.getPreferredSize();
		if (d.width > 50)
		{
			d.width = 50;
		}
		return d;
	}

	final class JAPMixCascadeComboBoxModel extends DefaultComboBoxModel
	{
		public void setSelectedItem(Object anObject)
		{

			if (anObject instanceof TrustModel ||
				anObject.equals(JAPMixCascadeComboBox.ITEM_NO_SERVERS_AVAILABLE)||
				anObject.equals(ITEM_AVAILABLE_SERVERS))
			{
				return;
			}
			super.setSelectedItem(anObject);
		}
	}

	final class JAPMixCascadeComboBoxListCellRender implements ListCellRenderer
	{
		private final Color m_newCascadeColor = new Color(255, 255, 170);

		private JLabel m_componentNoServer;
		private JLabel m_componentAvailableServer;
		private JLabel m_componentUserServer;
		private JLabel m_componentAvailableCascade;

		private JLabel m_lblCascadePopupMenu;
		private JLabel m_lblMenuArrow;
		private JPanel m_cascadePopupMenu;
		private CascadePopupMenu m_currentCascadePopup;

		public JAPMixCascadeComboBoxListCellRender()
		{
			m_componentNoServer = new JLabel(JAPMessages.getString("ngMixComboNoServers"));
			m_componentNoServer.setIcon(GUIUtils.loadImageIcon(JAPConstants.IMAGE_ERROR, true));
			m_componentNoServer.setBorder(new EmptyBorder(0, 3, 0, 3));
			m_componentNoServer.setForeground(Color.red);

			m_componentAvailableServer = new JLabel(JAPMessages.getString("ngMixComboAvailableServers"));
			m_componentAvailableServer.setOpaque(true);
			m_componentAvailableServer.setHorizontalAlignment(SwingConstants.LEFT);
			m_componentAvailableServer.setBorder(new EmptyBorder(1, 3, 1, 3));

			m_componentUserServer = new JLabel(JAPMessages.getString("ngMixComboUserServers"));
			m_componentUserServer.setBorder(new EmptyBorder(1, 3, 1, 3));
			m_componentUserServer.setHorizontalAlignment(SwingConstants.LEFT);
			m_componentUserServer.setOpaque(true);

			m_componentAvailableCascade = new JLabel();
			m_componentAvailableCascade.setHorizontalAlignment(SwingConstants.LEFT);
			m_componentAvailableCascade.setOpaque(true);
			m_componentAvailableCascade.setBorder(new EmptyBorder(1, 3, 1, 3));


			m_lblCascadePopupMenu = new JLabel();
			m_lblCascadePopupMenu.setOpaque(true);
			m_cascadePopupMenu = new JPanel(new GridBagLayout());
			m_cascadePopupMenu.setBorder(new EmptyBorder(1, 3, 1, 1));
			GridBagConstraints contraints = new GridBagConstraints();
			contraints.anchor = GridBagConstraints.WEST;
			contraints.gridx = 0;
			contraints.gridy = 0;
			m_cascadePopupMenu.add(m_lblCascadePopupMenu, contraints);
			contraints.gridx++;
			contraints.anchor = GridBagConstraints.EAST;
			contraints.weightx = 1.0;
			m_lblMenuArrow = new JLabel(GUIUtils.loadImageIcon("arrow46.gif", true));
			m_lblMenuArrow.setOpaque(true);
			m_cascadePopupMenu.add(m_lblMenuArrow, contraints);
			m_cascadePopupMenu.setOpaque(true);
			//m_cascadePopupMenu.setIcon(GUIUtils.loadImageIcon("arrow46.gif"));
			//m_cascadePopupMenu.setHorizontalTextPosition(JLabel.LEADING);
			//m_cascadePopupMenu.setIconTextGap(500);
			m_currentCascadePopup = new CascadePopupMenu(true);

			m_currentCascadePopup.registerExitHandler(new CascadePopupMenu.ExitHandler()
			{
				public void exited()
				{
					m_currentCascadePopup.setVisible(false);
					if (m_comboPopup == null || !m_comboPopup.isVisible())
					{
						JAPMixCascadeComboBox.this.showPopup();
					}
				}
			});


			GUIUtils.addAWTEventListener(new GUIUtils.AWTEventListener()
			{
				public void eventDispatched(AWTEvent a_event)
				{
					if (a_event instanceof MouseEvent)
					{
						MouseEvent event = (MouseEvent)a_event;
						if (a_event.getSource() instanceof Component)
						{
							Component component = (Component) a_event.getSource();
							Point positionOnScreen = null;
							try
							{
								positionOnScreen = component.getLocationOnScreen();
								positionOnScreen.x += event.getX();
								positionOnScreen.y += event.getY();
							}
							catch (IllegalComponentStateException a_e)
							{
								// ignore
							}
							if (m_currentCascadePopup.getRelativePosition(positionOnScreen) == null &&
								GUIUtils.getRelativePosition(positionOnScreen, m_comboPopup) == null)
							{
								if (m_currentCascadePopup.isVisible())
								{
									//m_currentCascadePopup.setVisible(false);
									if (m_comboPopup == null || !m_comboPopup.isVisible())
									{
										JAPMixCascadeComboBox.this.showStaticPopup();
									}
								}
							}
						}
					}
				}
			});
		}

		public Component getListCellRendererComponent(final JList list, Object value, int index,
													  boolean isSelected, boolean cellHasFocus)
		{
			if (m_comboPopup == null)
			{
				GUIUtils.getMousePosition();
				Component component = m_cascadePopupMenu.getParent();
				while (component != null)
				{
					component = component.getParent();
					if (component instanceof JPopupMenu)
					{
						m_comboPopup = (JPopupMenu) component;
						break;
					}
				}
			}

			if (value == null)
			{
				return new JLabel();
			}

			if (isSelected && m_currentCascadePopup.isVisible() &&
				m_currentCascadePopup.getTrustModel() != null &&
				!m_currentCascadePopup.getTrustModel().equals(value) &&
				m_currentCascadePopup.getMousePosition() == null) // important for some L&Fs
			{
				m_currentCascadePopup.setVisible(false);
			}

			if (value instanceof TrustModel)
			{
				if (isSelected)
				{
					if (!m_currentCascadePopup.isVisible())
					{
						int x, y;
						Point location = list.getLocationOnScreen();
						Point popupLocation;

						x = location.x + list.getWidth();
						y = location.y + (int)list.indexToLocation(index).y; // - list.getHeight();

						if (m_currentCascadePopup.update((TrustModel)value))
						{
							y -= m_currentCascadePopup.getHeaderHeight();
							popupLocation =
								m_currentCascadePopup.calculateLocationOnScreen(list, new Point(x, y));

							if (popupLocation.x < x)
							{
								x = location.x - m_currentCascadePopup.getWidth();
								popupLocation =
									m_currentCascadePopup.calculateLocationOnScreen(list, new Point(x, y));
							}
							m_currentCascadePopup.setLocation(popupLocation);
							m_currentCascadePopup.setVisible(true);
						}
					}
					m_cascadePopupMenu.setBackground(list.getSelectionBackground());
					m_cascadePopupMenu.setForeground(list.getSelectionForeground());
				}
				else
				{
					m_cascadePopupMenu.setBackground(list.getBackground());
					m_cascadePopupMenu.setForeground(list.getForeground());
				}
				m_lblMenuArrow.setBackground(m_cascadePopupMenu.getBackground());
				m_lblMenuArrow.setForeground(m_cascadePopupMenu.getForeground());
				m_lblCascadePopupMenu.setBackground(m_cascadePopupMenu.getBackground());
				m_lblCascadePopupMenu.setForeground(m_cascadePopupMenu.getForeground());

				if (((TrustModel)value).equals(TrustModel.getCurrentTrustModel()))
				{
					m_lblCascadePopupMenu.setFont(new Font(m_lblCascadePopupMenu.getFont().getName(),
						Font.BOLD,
						m_lblCascadePopupMenu.getFont().getSize()));
				}
				else
				{
					m_lblCascadePopupMenu.setFont(new Font(m_lblCascadePopupMenu.getFont().getName(),
						Font.PLAIN,
						m_lblCascadePopupMenu.getFont().getSize()));
				}

				m_lblCascadePopupMenu.setText(((TrustModel)value).getName());

				return m_cascadePopupMenu;
			}
			else if (value.equals(JAPMixCascadeComboBox.ITEM_NO_SERVERS_AVAILABLE))
			{
				return m_componentNoServer;
			}
			else if (value.equals(JAPMixCascadeComboBox.ITEM_AVAILABLE_SERVERS))
			{
				return m_componentAvailableServer;
			}

			MixCascade cascade = (MixCascade) value;
			JLabel l;
			ImageIcon icon;

			if (cascade.isUserDefined())
			{
				if (TrustModel.getCurrentTrustModel().isTrusted(cascade))
				{
					icon = GUIUtils.loadImageIcon(JAPConstants.IMAGE_CASCADE_MANUELL, true);
				}
				else
				{
					icon = GUIUtils.loadImageIcon(JAPConstants.IMAGE_CASCADE_MANUAL_NOT_TRUSTED, true);
				}
			}
			else if (cascade.isPayment())
			{
				if (TrustModel.getCurrentTrustModel().isTrusted(cascade))
				{
					icon = GUIUtils.loadImageIcon(JAPConstants.IMAGE_CASCADE_PAYMENT, true);
				}
				else
				{
					icon = GUIUtils.loadImageIcon(JAPConstants.IMAGE_CASCADE_PAYMENT_NOT_TRUSTED, true);
				}
			}
			else
			{
				if (TrustModel.getCurrentTrustModel().isTrusted(cascade))
				{
					icon = GUIUtils.loadImageIcon(JAPConstants.IMAGE_CASCADE_INTERNET, true);
				}
				else
				{
					icon = GUIUtils.loadImageIcon(JAPConstants.IMAGE_CASCADE_INTERNET_NOT_TRUSTED, true);
				}
			}
			if (cascade.isSocks5Supported())
			{
				icon = GUIUtils.combine(icon, GUIUtils.loadImageIcon("socks_icon.gif", true));
			}
			l = m_componentAvailableCascade;
			l.setIcon(icon);
			l.setText(GUIUtils.trim(cascade.getName()));
			if (isSelected)
			{
				l.setBackground(list.getSelectionBackground());
				l.setForeground(list.getSelectionForeground());
			}
			else
			{
				if ((Database.getInstance(NewCascadeIDEntry.class).getNumberOfEntries() * 2 <
					 Database.getInstance(MixCascade.class).getNumberOfEntries()) &&
					 Database.getInstance(NewCascadeIDEntry.class).getEntryById(
									   cascade.getMixIDsAsString()) != null)
				{
					l.setBackground(m_newCascadeColor);
				}
				else
				{
					l.setBackground(list.getBackground());
				}
				l.setForeground(list.getForeground());
			}

			return l;
		}
	}
}
