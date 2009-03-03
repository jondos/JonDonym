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



import java.util.Date;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Vector;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Insets;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSeparator;

import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;

import gui.GUIUtils;
import gui.JAPMultilineLabel;

import anon.infoservice.ServiceLocation;
import anon.infoservice.ServiceOperator;
import anon.util.CountryMapper;
import anon.util.JAPMessages;

/**
 * Class for painting a mix cascade in the configuration dialog
 */
final public class ServerListPanel extends JPanel implements ActionListener
{
	private static final String MSG_MIX_CLICK = ServerListPanel.class.getName() + "_mixClick";

	private static final String MSG_MIX_COUNTRY = ServerListPanel.class.getName() + "_mixCountry";
	private static final String MSG_OPERATOR_COUNTRY = ServerListPanel.class.getName() + "_operatorCountry";
	private static final String MSG_MIX_AND_OPERATOR_COUNTRY = ServerListPanel.class.getName() + "_mixAndOperatorCountry";

	
	private boolean m_bEnabled;
	private ButtonGroup m_bgMixe;
	private int m_selectedIndex;
	private Vector m_itemListeners;
	private JRadioButton[] m_mixButtons;
	private JLabel[] m_mixFlags;
	private JLabel[] m_operatorFlags;

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
		m_mixFlags = new JLabel[a_numberOfMixes];
		m_operatorFlags = new JLabel[a_numberOfMixes];
		m_itemListeners = new Vector();
		GridBagLayout la = new GridBagLayout();
		GridBagConstraints constraints = new GridBagConstraints();
		m_bgMixe = new ButtonGroup();
		m_selectedIndex = 0;

		setLayout(la);
		constraints.gridy = 0;
		constraints.gridx = 0;
		constraints.anchor = GridBagConstraints.WEST;
		constraints.weightx = 0;
		constraints.fill = GridBagConstraints.HORIZONTAL;

		for(int i = 0; i < a_numberOfMixes; i++)
		{
			constraints.gridy = 0;
			constraints.gridx = i * 2;
			constraints.gridheight = 3;
			constraints.insets = new Insets(0, 0, 0, 0);
			
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

			add(m_mixButtons[i], constraints);
			m_bgMixe.add(m_mixButtons[i]);
			m_bEnabled = a_enabled;
			m_mixButtons[i].setEnabled(m_bEnabled);
			
			constraints.gridy = 0;
			constraints.gridheight = 1;
			constraints.gridx = (i * 2) + 1;
			constraints.weightx = 0;			
			m_mixFlags[i] = new JLabel(" ");
			m_mixFlags[i].setFont(new Font("", Font.PLAIN, (int)(14.0 * (1.0 + JAPModel.getInstance().getFontSize() * 0.1))));
			add(m_mixFlags[i], constraints);			
			
			//if (i != a_numberOfMixes - 1)
			{
				constraints.gridx = (i * 2) + 1;
				constraints.gridheight = 1;
				constraints.gridy = 1;
				if (a_numberOfMixes == 1)
				{
					constraints.weightx = 0.5;
				}
				else
				{
					constraints.weightx = 0.5 / (a_numberOfMixes - 1);
				}
				JSeparator sep = new JSeparator();
				add(sep, constraints);
			}
			
			constraints.gridy = 2;
			constraints.gridheight = 1;
			constraints.gridx = (i * 2) +1;
			constraints.weightx = 0;
			m_operatorFlags[i] = new JLabel("");
			//m_operatorFlags[i].setBorder(BorderFactory.createLineBorder(Color.black, 2));
			m_operatorFlags[i].setFont(new Font("", Font.PLAIN, (int)(10.0 * (1.0 + JAPModel.getInstance().getFontSize() * 0.1))));
			
			/* create a dummy panel so that we can put a border around the image if we want */
			JPanel pnlDummy = new JPanel(new GridBagLayout());
			GridBagConstraints dummyConstraints = new GridBagConstraints();
			dummyConstraints.gridx = 0;
			dummyConstraints.gridy = 0;
			dummyConstraints.fill = GridBagConstraints.NONE;
			dummyConstraints.anchor = GridBagConstraints.WEST;
			pnlDummy.add(m_operatorFlags[i], dummyConstraints);
			dummyConstraints.weightx = 1.0;
			dummyConstraints.gridx = 1;
			dummyConstraints.fill = GridBagConstraints.HORIZONTAL;
			pnlDummy.add(new JLabel(), dummyConstraints);
			add(pnlDummy, constraints);
		}

		
		constraints.gridx++;
		constraints.gridy = 0;
		constraints.weightx = 0;
		constraints.gridheight = 3;
		constraints.fill = GridBagConstraints.HORIZONTAL;
		constraints.anchor = GridBagConstraints.EAST;	
		
		/*
		Icon icon = GUIUtils.createScaledIcon(GUIUtils.loadImageIcon("cloud.png", true), new GUIUtils.IIconResizer()
		{
			public double getResizeFactor()
			{
				return 0.6;
			}
		});*/
		Icon icon = GUIUtils.loadImageIcon("cloud.png", true);
		add(new JLabel(icon), constraints);
		
		constraints.gridx++;
		constraints.weightx = 0.7;
		add(new JLabel(""), constraints);
		
	}

	public boolean areMixButtonsEnabled()
	{
		return m_bEnabled;
	}

	public int getNumberOfMixes()
	{
		return m_mixButtons.length;
	}
	
	public synchronized void moveToPrevious()
	{
		JRadioButton btn = setSelectedIndex(m_selectedIndex - 1);
		if (btn != null)
		{
			actionPerformed(new ActionEvent(btn, ActionEvent.ACTION_PERFORMED, null));
		}
	}
	
	public synchronized void moveToNext()
	{
		JRadioButton btn = setSelectedIndex(m_selectedIndex + 1);
		if (btn != null)
		{
			actionPerformed(new ActionEvent(btn, ActionEvent.ACTION_PERFORMED, null));
		}		
	}

	/**
	 * Determine which mix was clicked and set m_selectedMix accordingly
	 * @param e ActionEvent
	 */
	public synchronized void actionPerformed(ActionEvent e)
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

	public synchronized JRadioButton setSelectedIndex(int a_index)
	{
		JRadioButton btnAffected;
		if (a_index < 0)
		{
			return null;
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
			return null;
			//throw new IndexOutOfBoundsException("Invalid index: " + a_index);
		}
		m_selectedIndex = i;
		btnAffected = (JRadioButton)mixes.nextElement();
		btnAffected.setSelected(true);
		return btnAffected;
	}
	
	/**
	 * Updates the mix country flag
	 * 
	 * @param a_mix				The mix that should be updated
	 * @param a_location		The new ServiceLocation
	 * @param a_mixAndOperator	true, if Mix and Operator will have the same country code
	 */
	private synchronized void updateFlag(int a_mix, ServiceLocation a_location)
	{
		if(a_location != null)
		{
			try
			{
				CountryMapper county = 
					new CountryMapper(a_location.getCountryCode(), JAPMessages.getLocale());
				m_mixFlags[a_mix].setIcon(GUIUtils.loadImageIcon("flags/" + county.getISOCode() + ".png"));
				m_mixFlags[a_mix].setToolTipText(JAPMessages.getString(MSG_MIX_COUNTRY, county.toString()));
			}
			catch (IllegalArgumentException a_e)
			{
				// this does not seem to be a valid country code
				m_mixFlags[a_mix].setIcon(null);
				m_mixFlags[a_mix].setToolTipText(null);
			}
		}
		else
		{
			m_mixFlags[a_mix].setIcon(null);
			m_mixFlags[a_mix].setToolTipText(null);
		}
	}
	
	public synchronized void update(int a_mix, ServiceOperator a_operator, ServiceLocation a_location)
	{
		if (a_operator != null && a_operator.getCountryCode() != null)
		{
			if (a_location != null && a_location.getCountryCode() != null && 
				!a_location.getCountryCode().equals(a_operator.getCountryCode()))
			{
				updateFlag(a_mix, a_location);
				updateOperatorFlag(a_mix, a_operator, false);
			}
			else
			{
				updateOperatorFlag(a_mix, a_operator, true);
			}
			
			/* Show certification status. */
			if (a_operator.getCertPath().isVerified())
			{
				if (!a_operator.getCertPath().isValid(new Date()))
				{
					m_operatorFlags[a_mix].setBorder(BorderFactory.createLineBorder(Color.yellow, 2));
				}
				else if (a_operator.getCertPath().countVerifiedAndValidPaths() > 2)
				{
					m_operatorFlags[a_mix].setBorder(BorderFactory.createLineBorder(Color.green, 2));
				}
				else if (a_operator.getCertPath().countVerifiedAndValidPaths() > 1)
				{
					m_operatorFlags[a_mix].setBorder(BorderFactory.createLineBorder(new Color(100, 215, 255), 2));
				}
				else
				{
					m_operatorFlags[a_mix].setBorder(BorderFactory.createEmptyBorder());
				}
			}
			else
			{
				m_operatorFlags[a_mix].setBorder(BorderFactory.createLineBorder(Color.red, 2));
			}
		}
		else
		{
			updateOperatorFlag(a_mix, a_operator, false);
			updateFlag(a_mix, a_location);
		}
	}
	
	/**
	 * Updates the mix operator flag
	 * 
	 * @param a_mix			The mix that should be updated
	 * @param a_operator	The new ServiceOperator
	 */
	private synchronized void updateOperatorFlag(int a_mix, ServiceOperator a_operator, boolean a_mixAndOperator)
	{	
		if(a_operator != null && a_operator.getCountryCode() != null)
		{
			CountryMapper county = 
				new CountryMapper(a_operator.getCountryCode(), JAPMessages.getLocale());				
			m_operatorFlags[a_mix].setIcon(GUIUtils.loadImageIcon("flags/" + county.getISOCode() + ".png"));
			if (a_mixAndOperator)
			{
				m_operatorFlags[a_mix].setToolTipText(JAPMessages.getString(MSG_MIX_AND_OPERATOR_COUNTRY, county.toString()));
				updateFlag(a_mix, null);
			}
			else
			{
				m_operatorFlags[a_mix].setToolTipText(JAPMessages.getString(MSG_OPERATOR_COUNTRY, county.toString()));
			}
		}
		else
		{
			m_operatorFlags[a_mix].setIcon(null);
			m_operatorFlags[a_mix].setToolTipText(null);
		}
	}

	/**
	 * Getter method for m_selectedMix
	 */
	public int getSelectedIndex()
	{
		return m_selectedIndex;
	}

}
