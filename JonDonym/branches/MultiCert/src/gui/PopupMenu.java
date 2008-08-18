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

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Window;
import javax.swing.event.PopupMenuListener;
import javax.swing.event.PopupMenuEvent;
import javax.swing.JPanel;
import javax.swing.JWindow;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;
import javax.swing.JSeparator;
import java.util.Random;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.Vector;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import anon.infoservice.JavaVersionDBEntry;
import java.awt.event.MouseMotionAdapter;

/**
 *
 * @author Rolf Wendolsky
 */
public class PopupMenu
{
	private ExitHandler m_exitHandler;
	private Component m_popup;
	private GridBagConstraints m_constraints;
	private Window m_parent;
	private boolean m_bParentOnTop = false;
	private Vector m_popupListeners;
	private Vector m_registeredComponents;
	private boolean m_bCompatibilityMode;

	public PopupMenu()
	{
		this (new JPopupMenu());
	}

	public PopupMenu(boolean a_bCompatibilityMode)
	{
		this(new JPopupMenu(), a_bCompatibilityMode);
	}


	public PopupMenu(JPopupMenu a_popup)
	{
		this(a_popup, false);
	}

	private PopupMenu(JPopupMenu a_popup, boolean a_bCompatibilityMode)
	{
		if (a_popup == null)
		{
			throw new IllegalArgumentException("Given argument is null!");
		}
		m_bCompatibilityMode = a_bCompatibilityMode;


		if (m_bCompatibilityMode)
		{
			m_popup = new JWindow();
			JPanel contentPane = new JPanel();
			contentPane.setBorder(new JPopupMenu().getBorder());
			((JWindow)m_popup).setContentPane(contentPane);
			((JWindow)m_popup).getContentPane().setLayout(new GridBagLayout());
			m_popup.addComponentListener(new ComponentAdapter()
			{
				public void componentHidden(ComponentEvent a_event)
				{
					synchronized (m_popupListeners)
					{
						for (int i = 0; i < m_popupListeners.size(); i++)
						{
							( (PopupMenuListener) m_popupListeners.elementAt(i)).popupMenuWillBecomeInvisible(
								new PopupMenuEvent(a_event.getSource()));
						}
					}

				}
			});
		}
		else
		{
			m_popup = a_popup;
			((JPopupMenu)m_popup).addPopupMenuListener(new PopupMenuListener()
			{
				public void popupMenuWillBecomeVisible(PopupMenuEvent a_event)
				{

				}
				public void popupMenuWillBecomeInvisible(PopupMenuEvent a_event)
				{
					resetParentOnTopAttribute();
				}
				public void popupMenuCanceled(PopupMenuEvent a_event)
				{
				}
			});
			((JPopupMenu)m_popup).addMouseMotionListener(new MouseMotionAdapter()
			{

			});
		}

		m_popupListeners = new Vector();
		m_registeredComponents = new Vector();


		m_constraints = new GridBagConstraints();
		m_constraints.gridx = 0;
		m_constraints.gridy = 0;
		m_constraints.weighty = 1.0;
		m_constraints.fill = GridBagConstraints.HORIZONTAL;
		m_constraints.anchor = GridBagConstraints.WEST;



		m_popup.setName(Double.toString(new Random().nextDouble()));
		MouseAdapter exitAdapter = new MouseAdapter()
		{
			public void mouseClicked(MouseEvent a_event)
			{
				if (SwingUtilities.isRightMouseButton(a_event))
				{
					//m_popup.setVisible(false);
					setVisible(false);
				}
			}
			/*public void mouseExited(MouseEvent a_event)
			{
				Component component = m_popup.getComponentAt(a_event.getPoint());
				if (component == null || component.getParent() != m_popup)
				{
					m_exitHandler.exited();
				}
			}*/
		};


		m_popup.addMouseListener(exitAdapter);

		registerExitHandler(null);
	}

	protected void removeAll()
	{
		if (m_bCompatibilityMode)
		{
			((JWindow)m_popup).getContentPane().removeAll();

			m_constraints.gridy = 0;
			m_registeredComponents.removeAllElements();
		}
		else
		{
			( (JPopupMenu) m_popup).removeAll();
		}
	}

	protected void insert(Component a_component, int a_index)
	{
		if (m_bCompatibilityMode)
		{
		add(a_component);
	}
		else
		{
			((JPopupMenu) m_popup).insert(a_component, a_index);
		}
	}

	protected void addSeparator()
	{
		addSeparator(new JSeparator());
	}

	protected void addSeparator(JSeparator a_separator)
	{
		add(a_separator);
		m_constraints.gridy++;
	}

	protected void pack()
	{
		if (m_bCompatibilityMode)
		{
			((JWindow)m_popup).pack();
		}
		else
		{
			((JPopupMenu)m_popup).pack();
		}
	}

	protected void add(Component a_component)
	{
		if (m_bCompatibilityMode)
		{
			if (a_component == null)
			{
				return;
			}
			((JWindow)m_popup).getContentPane().add(a_component, m_constraints);
			m_constraints.gridy++;
			m_registeredComponents.addElement(a_component);
		}
		else
		{
			((JPopupMenu)m_popup).add(a_component);
		}
	}

	public final void addPopupMenuListener(PopupMenuListener a_listener)
	{
		synchronized (m_popupListeners)
		{
			if (m_bCompatibilityMode)
			{
				if (a_listener != null && !m_popupListeners.contains(a_listener))
				{
					m_popupListeners.addElement(a_listener);
				}
			}
			else
			{
				((JPopupMenu)m_popup).addPopupMenuListener(a_listener);
			}
		}

	}

	public final boolean removePopupMenuListener(PopupMenuListener a_listener)
	{
		synchronized (m_popupListeners)
		{
			if (m_bCompatibilityMode)
			{
				if (a_listener != null)
				{
					return m_popupListeners.removeElement(a_listener);
				}
			}
			else
			{
				((JPopupMenu)m_popup).removePopupMenuListener(a_listener);
			}
		}
		return false;

	}


	public final Point getRelativePosition(Point a_pointOnScreen)
	{
		return GUIUtils.getRelativePosition(a_pointOnScreen, m_popup);
	}

	public final Point getMousePosition()
	{
		return GUIUtils.getMousePosition(m_popup);
	}

	public final void registerExitHandler(ExitHandler a_exitHandler)
	{
		if (a_exitHandler != null)
		{
			m_exitHandler = a_exitHandler;
		}
		else
		{
			m_exitHandler = new ExitHandler()
			{
				public void exited()
				{
					// do nothing
				}
			};
		}
	}

	public final synchronized void show(Component a_parent, Point a_pointOnScreen)
	{
		show(a_parent, null, a_pointOnScreen);
	}

	public final synchronized void show(Component a_parent, Window a_hiddenParent, Point a_pointOnScreen)
	{
		Point location = calculateLocationOnScreen(a_parent, a_pointOnScreen);
		Window parentWindow = GUIUtils.getParentWindow(a_parent);

		//	((JPopupMenu)m_popup).show(a_parent, location.x - parentWindow.getLocation().x,
		//	 location.y - parentWindow.getLocation().y);

		m_popup.setLocation(location);
		pack();
		m_parent = null;
		m_bParentOnTop = false;

		if (GUIUtils.isAlwaysOnTop(a_hiddenParent))
		{
			m_bParentOnTop = true;
			m_parent = a_hiddenParent;
		}
		else if (GUIUtils.isAlwaysOnTop(parentWindow))
		{
			m_bParentOnTop = true;
			m_parent = parentWindow;
		}

		//GUIUtils.setAlwaysOnTop(parentWindow, false);


		/** @todo Find a better way to distinguish JDK version compatibility!  */
		if (!m_bCompatibilityMode &&
			(JavaVersionDBEntry.CURRENT_JAVA_VENDOR.toLowerCase().indexOf("sun") >= 0 ||
			JavaVersionDBEntry.CURRENT_JAVA_VENDOR.toLowerCase().indexOf("apple") >= 0 /*&&
			 (JavaVersionDBEntry.CURRENT_JAVA_VERSION.compareTo("1.6") < 0 ||
			(JavaVersionDBEntry.CURRENT_JAVA_VERSION.compareTo("1.6.0_02") >= 0))*/))
		{
			( (JPopupMenu) m_popup).setInvoker(parentWindow);
		}


		if (m_bCompatibilityMode)
		{
			synchronized (m_popupListeners)
			{
				for (int i = 0; i < m_popupListeners.size(); i++)
				{
					( (PopupMenuListener) m_popupListeners.elementAt(i)).popupMenuWillBecomeVisible(
						new PopupMenuEvent(m_popup));
				}
			}
		}

		setVisible(true);
		if (parentWindow != null && m_bParentOnTop)
		{
			GUIUtils.setAlwaysOnTop(m_popup, true);
		}
	}

	public void repaint()
	{
		m_popup.repaint();
	}

	public final void setLocation(Point a_point)
	{
		m_popup.setLocation(a_point);
	}

	public final Point calculateLocationOnScreen(Component a_parent, Point a_pointOnScreen)
	{
			int x = a_pointOnScreen.x;
			int y = a_pointOnScreen.y;
			GUIUtils.Screen screen = GUIUtils.getCurrentScreen(a_parent);

			Dimension size = m_popup.getPreferredSize();
			if (x + size.width > screen.getX() + screen.getWidth())
			{
				x = screen.getX() + screen.getWidth() - size.width;
			}
			if (y + size.height > screen.getY() + screen.getHeight())
			{
				y = screen.getY() + screen.getHeight() - size.height;
			}

			// optimize the place on the screen
			x = Math.max(x, screen.getX());
			y = Math.max(y, screen.getY());

			return new Point(x, y);
	}

	public final int getWidth()
	{
		return (int)m_popup.getPreferredSize().width;
	}

	public final int getHeight()
	{
		return (int)m_popup.getPreferredSize().height;
	}


	public final boolean isVisible()
	{
		return m_popup.isVisible();
	}

	public final synchronized void dispose()
	{
		setVisible(false);
		if (m_bCompatibilityMode)
		{
			((JWindow)m_popup).dispose();
		}
		else
		{
			//((JPopupMenu)m_popup).d;
		}
	}

	private final synchronized void resetParentOnTopAttribute()
	{
		if (GUIUtils.isAlwaysOnTop(m_popup))
		{
			GUIUtils.setAlwaysOnTop(m_popup, false);
			Window parent = m_parent;
			if (parent != null)
			{
				if (m_bParentOnTop)
				{
					GUIUtils.setAlwaysOnTop(parent, false);
					parent.setVisible(true);
					GUIUtils.setAlwaysOnTop(parent, true);
				}
			}
			m_parent = null;
			m_bParentOnTop = false;
		}

	}

	public final synchronized void setVisible(boolean a_bVisible)
	{
		if (!a_bVisible)
		{
			resetParentOnTopAttribute();
		}
		if (a_bVisible && m_bCompatibilityMode)
		{
			synchronized (m_popupListeners)
			{
				for (int i = 0; i < m_popupListeners.size(); i++)
				{
					( (PopupMenuListener) m_popupListeners.elementAt(i)).popupMenuWillBecomeVisible(
						new PopupMenuEvent(m_popup));
				}
			}
		}


		m_popup.setVisible(a_bVisible);
	}

	public interface ExitHandler
	{
		public void exited();
	}
}
