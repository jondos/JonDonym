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
package gui;

import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.JLabel;
import javax.swing.JPanel;

import anon.util.JAPMessages;

import java.awt.Cursor;

/**
 * This class is used for the various panels on the Jap's
 * main view (JapNewView).
 *
 * It is a panel with two states (normal vs. flipped).
 * In normal state the contents of the full panel is shown,
 * in flipped state the contents of the small panel is shown,
 * which should be much smaller in height.
 *
 * The user can toggle the flipped state by clicking an arrow icon on the
 * left side.
 *
 * @author ??
 */
public class FlippingPanel extends JPanel
{
	/**
	 * serial version UID
	 */
	private static final long serialVersionUID = 1L;

	private static final String MSG_CLICK_TO_SHOW = FlippingPanel.class.getName() + "_clickToShow";

	private JPanel m_panelContainer;
	private JPanel m_panelSmall;
	private JPanel m_panelFull;
	private JLabel m_labelBttn;
	private CardLayout m_Layout;
	private Window m_Parent;
	private boolean m_bIsFlipped;
	private final static String IMG_UP = "arrow.gif";
	private final static String IMG_DOWN = "arrow90.gif";

	public FlippingPanel(Window parent)
	{
		m_bIsFlipped = false;
		m_Parent = parent;
		GridBagLayout gbl = new GridBagLayout();
		GridBagConstraints c = new GridBagConstraints();
		setLayout(gbl);
		m_labelBttn = new JLabel(GUIUtils.loadImageIcon(IMG_UP, true));
		m_labelBttn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		m_labelBttn.setToolTipText(JAPMessages.getString(MSG_CLICK_TO_SHOW));
		c.fill = GridBagConstraints.NONE;
		c.weightx = 0;
		c.weighty = 0;
		c.insets = new Insets(3, 0, 0, 0);
		c.anchor = GridBagConstraints.NORTHWEST;
		m_labelBttn.addMouseListener(new MouseListener()
		{
			public void mouseClicked(MouseEvent e)
			{
				m_bIsFlipped = !m_bIsFlipped;
				m_Layout.next(m_panelContainer);
				if (m_bIsFlipped)
				{
					m_labelBttn.setIcon(GUIUtils.loadImageIcon(IMG_DOWN, true));
				}
				else
				{
					m_labelBttn.setIcon(GUIUtils.loadImageIcon(IMG_UP, true));
				}

				m_Parent.pack();
			}

			public void mouseEntered(MouseEvent e)
			{
			}

			public void mouseExited(MouseEvent e)
			{
			}

			public void mousePressed(MouseEvent e)
			{
			}

			public void mouseReleased(MouseEvent e)
			{
			}
		}
		);
		add(m_labelBttn, c);
		c.gridx = 1;
		c.fill = GridBagConstraints.BOTH;
		c.weightx = 1;
		c.weighty = 1;
		c.insets = new Insets(0, 0, 0, 0);
		m_panelContainer = new JPanel();
		m_Layout = new CardLayout();
		m_panelContainer.setLayout(m_Layout);
		add(m_panelContainer, c);

		m_panelSmall = new JPanel(new GridLayout(1, 1));
		m_panelContainer.add(m_panelSmall, "SMALL", 0);
		m_panelFull = new JPanel(new GridLayout(1, 1));
		m_Layout.addLayoutComponent(m_panelFull, "FULL");
		m_panelContainer.add(m_panelFull, "FULL", 1);
	}

	public void setFullPanel(JPanel p)
	{
		m_panelFull.removeAll();
		m_panelFull.add(p);
	}

	public JPanel getFullPanel()
	{
		return (JPanel) m_panelFull.getComponent(0);
	}

	public void setSmallPanel(JPanel p)
	{
		m_panelSmall.removeAll();
		m_panelSmall.add(p);
	}

	public JPanel getSmallPanel()
	{
		return (JPanel) m_panelSmall.getComponent(0);
	}

	public Dimension getPreferredSize()
	{
		Dimension d1, d2;
		d1 = m_panelFull.getPreferredSize();
		d2 = m_panelSmall.getPreferredSize();
		d1.width = Math.max(d1.width, d2.width);
		d1.width += GUIUtils.loadImageIcon(IMG_UP, true).getIconWidth();
		if (!m_bIsFlipped)
		{
			d1.height = d2.height;
		}
		return d1;
	}

	public Dimension getMinimumSize()
	{
		Dimension d1, d2;
		d1 = m_panelFull.getMinimumSize();
		d2 = m_panelSmall.getMinimumSize();
		d1.width = Math.max(d1.width, d2.width);
		d1.width += GUIUtils.loadImageIcon(IMG_UP, true).getIconWidth();
		if (!m_bIsFlipped)
		{
			d1.height = d2.height;
		}
		d1.height = Math.max(d1.height, GUIUtils.loadImageIcon(IMG_DOWN, true).getIconHeight());
		return d1;
	}

	public Dimension getMaximumSize()
	{
		Dimension d1, d2;
		d1 = m_panelFull.getMaximumSize();
		d2 = m_panelSmall.getMaximumSize();
		d1.width = Math.max(d1.width, d2.width);
		d1.width += GUIUtils.loadImageIcon(IMG_UP, true).getIconWidth();
		if (!m_bIsFlipped)
		{
			d1.height = d2.height;
		}
		return d1;
	}

	public void setFlipped(boolean bFlipped)
	{
		if (bFlipped == m_bIsFlipped)
		{
			return;
		}
		else
		{
			m_labelBttn.dispatchEvent(
				new MouseEvent(
				m_labelBttn, MouseEvent.MOUSE_CLICKED,
				0, 0, 0, 0, 1, false));
		}
	}
}
