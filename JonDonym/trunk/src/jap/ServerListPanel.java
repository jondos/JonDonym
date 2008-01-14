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



import java.util.Enumeration;
import java.util.Vector;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import javax.swing.AbstractButton;
import javax.swing.ButtonGroup;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSeparator;

import gui.GUIUtils;
import gui.JAPMessages;
import gui.JAPMultilineLabel;

/**
 * Class for painting a mix cascade in the configuration dialog
 */
final public class ServerListPanel extends JPanel implements ActionListener
{
	private static final String MSG_MIX_CLICK = ServerListPanel.class.getName() + "_mixClick";

	private boolean m_bEnabled;
	private ButtonGroup m_bgMixe;
	private int m_selectedIndex;
	private Vector m_itemListeners;
	private JRadioButton[] m_mixButtons;

	/**
	 * Creates a panel with numberOfMixes Mix-icons
	 * @param numberOfMixes int
	 */
	public ServerListPanel(int a_numberOfMixes, boolean a_enabled, int a_selectedIndex)
	{
		int selectedIndex = 0;
		if (a_numberOfMixes < 1)
		{
			a_numberOfMixes = 1;
		}
		if (a_selectedIndex > 0 && a_selectedIndex < a_numberOfMixes)
		{
			selectedIndex = a_selectedIndex;
		}
		m_mixButtons = new JRadioButton[a_numberOfMixes];
		m_itemListeners = new Vector();
		GridBagLayout la = new GridBagLayout();
		GridBagConstraints constraints = new GridBagConstraints();
		m_bgMixe = new ButtonGroup();
		m_selectedIndex = 0;

		setLayout(la);
		constraints.anchor = GridBagConstraints.WEST;
		constraints.weightx = 0;
		constraints.fill = GridBagConstraints.HORIZONTAL;

		for (int i = 0; i < a_numberOfMixes; i++)
		{
			//Insert a line from the previous mix
			if (i != 0)
			{
				JSeparator line = new JSeparator();
				line.setPreferredSize(new Dimension(50, 3));
				line.setMaximumSize(new Dimension(50, 3));
				line.setSize(50, 3);
				line.setMinimumSize(new Dimension(5,3));
				constraints.weightx = 0.5;
				la.setConstraints(line, constraints);
				constraints.weightx = 0;
				add(line);
			}
			//Create the mix icon and place it in the panel
			m_mixButtons[i] = new JRadioButton();
			if (a_enabled)
			{
				m_mixButtons[i].setToolTipText(JAPMessages.getString("serverPanelAdditional"));
			}
			m_mixButtons[i].addActionListener(this);
			m_mixButtons[i].setBorder(null);
			m_mixButtons[i].setFocusPainted(false);
			m_mixButtons[i].setRolloverEnabled(true);
			m_mixButtons[i].setIcon(GUIUtils.loadImageIcon(JAPConstants.IMAGE_SERVER, true));
			m_mixButtons[i].setRolloverIcon(GUIUtils.loadImageIcon(JAPConstants.IMAGE_SERVER_BLAU, true));
			m_mixButtons[i].setSelectedIcon(GUIUtils.loadImageIcon(JAPConstants.IMAGE_SERVER_ROT, true));
			m_mixButtons[i].setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
			if (i == selectedIndex)
			{
				m_selectedIndex = i;
				m_mixButtons[i].setSelected(true);
			}
			if (i == a_numberOfMixes - 1)
			{
				constraints.weightx = 1;
			}

			la.setConstraints(m_mixButtons[i], constraints);
			add(m_mixButtons[i]);

			m_bgMixe.add(m_mixButtons[i]);
			m_bEnabled = a_enabled;
			m_mixButtons[i].setEnabled(m_bEnabled);
		}
		constraints.weightx = 1.0;
		constraints.gridheight = 1;
		Color color = null;
		if (!a_enabled)
		{
			color = getBackground();
		}

		JAPMultilineLabel explain = new JAPMultilineLabel(JAPMessages.getString(MSG_MIX_CLICK), color);
		constraints.fill = GridBagConstraints.HORIZONTAL;
		constraints.anchor = GridBagConstraints.EAST;
		add(explain, constraints);

	}

	public synchronized void fontSizeChanged(final JAPModel.FontResize a_resize, final JLabel a_dummyLabel)
	{
		for (int i = 0; i < m_mixButtons.length; i++)
		{
			m_mixButtons[i].setIcon(GUIUtils.loadImageIcon(JAPConstants.IMAGE_SERVER, true));
			m_mixButtons[i].setRolloverIcon(GUIUtils.loadImageIcon(JAPConstants.IMAGE_SERVER_BLAU, true));
			m_mixButtons[i].setSelectedIcon(GUIUtils.loadImageIcon(JAPConstants.IMAGE_SERVER_ROT, true));
		}
	}

	public boolean areMixButtonsEnabled()
	{
		return m_bEnabled;
	}

	public int getNumberOfMixes()
	{
		return m_mixButtons.length;
	}

	/**
	 * Determine which mix was clicked and set m_selectedMix accordingly
	 * @param e ActionEvent
	 */
	public void actionPerformed(ActionEvent e)
	{
		Object source = e.getSource();
		Enumeration mixes = m_bgMixe.getElements();
		int index = 0;
		while (mixes.hasMoreElements())
		{
			if (source == mixes.nextElement())
			{
				m_selectedIndex = index;
				ItemEvent itemEvent = new ItemEvent( (AbstractButton) source, ItemEvent.ITEM_STATE_CHANGED,
					source, ItemEvent.SELECTED);
				Enumeration enumer = m_itemListeners.elements();
				while (enumer.hasMoreElements())
				{
					( (ItemListener) enumer.nextElement()).itemStateChanged(itemEvent);
				}
				return;
			}
			index++;
		}
	}

	public void addItemListener(ItemListener l)
	{
		m_itemListeners.addElement(l);
	}

	public void removeItemListener (ItemListener a_listener)
	{
		m_itemListeners.removeElement(a_listener);
	}

	public void setSelectedIndex(int a_index)
	{
		if (a_index < 0)
		{
			return;
			//throw new IndexOutOfBoundsException("Invalid index: " + a_index);
		}

		Enumeration mixes = m_bgMixe.getElements();
		int i = 0;
		for (; i < a_index && mixes.hasMoreElements(); i++)
		{
			mixes.nextElement();
		}
		if (!mixes.hasMoreElements())
		{
			return;
			//throw new IndexOutOfBoundsException("Invalid index: " + a_index);
		}
		m_selectedIndex = i;
		((JRadioButton)mixes.nextElement()).setSelected(true);
	}

	/**
	 * Getter method for m_selectedMix
	 */
	public int getSelectedIndex()
	{
		return m_selectedIndex;
	}

}
