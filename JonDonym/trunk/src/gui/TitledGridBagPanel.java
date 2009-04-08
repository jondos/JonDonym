/*
 Copyright (c) 2000 - 2005, The JAP-Team
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

import java.util.Vector;

import java.awt.Color;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.LayoutManager;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.TitledBorder;

/**
 * This is a JPanel that uses a GridBaglayout to show vertical lists of two components.
 * @author Rolf Wendolsky
 */
public final class TitledGridBagPanel extends JPanel
{
	private GridBagConstraints m_constraints;
	private Vector m_rows;

	public TitledGridBagPanel()
	{
		this(null);
	}

	public TitledGridBagPanel(String a_strTitle)
	{
		this(a_strTitle, null);
	}

	public TitledGridBagPanel(String a_strTitle, Insets a_insets)
	{
		super(new GridBagLayout());
		if (a_strTitle != null)
		{
			setBorder(new TitledBorder(a_strTitle));
		}

		m_constraints = new GridBagConstraints();
		m_constraints.anchor = GridBagConstraints.WEST; // left-aligned
		setInsets(a_insets);
		m_rows = new Vector();
	}

	/**
	 * Sets new insets for this panel. All following rows will get the new
	 * insets.
	 * @param a_insets new Insets; if null, the panel will use the default
	 * insets
	 */
	public void setInsets(Insets a_insets)
	{
		if (a_insets == null)
		{
			a_insets = getDefaultInsets();
		}
		m_constraints.insets = a_insets;
	}

	/**
	 * The default insets that are used if no other insets are given.
	 * @return the default Insets
	 */
	public Insets getDefaultInsets()
	{
		return new Insets(5, 5, 5, 5);
	}

	/**
	 * It is not possible to define concrete insets because of a consistant layout.
	 * But by method the insets may be removed completely.
	 */
	public void removeInsets()
	{
		m_constraints.insets = new Insets(0,0,0,0);
	}

	public void setEnabled(boolean a_bEnabled)
	{
		TitledBorder border;
		if (getBorder() instanceof TitledBorder)
		{
			border = new TitledBorder( ( (TitledBorder) getBorder()).getTitle());
			if (!a_bEnabled)
			{
				border.setTitleColor(Color.gray);
			}
			setBorder(border);
		}

		super.setEnabled(a_bEnabled);


		Component[] components = getComponents();
		for (int i = 0; i < components.length; i++)
		{
			components[i].setEnabled(a_bEnabled);
		}
	}

	/**
	 * Throws an IllegalStateException as the LayoutManager is fixed to java.awt.GridBagLayout.
	 * @param a_layoutManager LayoutManager
	 */
	public void setLayout(LayoutManager a_layoutManager)
	{
		if (! (a_layoutManager instanceof GridBagLayout))
		{
			throw new IllegalStateException("Layout is fixed to GridBagLayout!");
		}
		super.setLayout(a_layoutManager);
	}

	/**
	 * Add a row with one component.
	 * @param a_component a Component (may be null)
	 */
	public int addRow(Component a_component)
	{
		return addRow(a_component, null);
	}

	/**
	 * Add a row with two components.
	 * @param a_component a Component (may be null)
	 * @param a_otherComponent an other Component (may be null)
	 */
	public int addRow(Component a_component, Component a_otherComponent)
	{
		return addRow(a_component, a_otherComponent, GridBagConstraints.HORIZONTAL);
	}

	public int addRow(Component a_component, Component a_otherComponent, int a_fill)
	{
		return replaceRow(a_component, a_otherComponent, getNextRow(), a_fill);
	}

	/**
	 * Add a row with two components.
	 * @param a_component a Component (may be null)
	 * @param a_otherComponent an other Component (may be null)
	 * @param a_thirdComponent a third component (may be null)
	 * @param a_fourthComponent a fourth component (may be null)
	 */
	public int addRow(Component a_component, Component a_otherComponent, Component a_thirdComponent,
					   Component a_fourthComponent)
	{
		return replaceRow(a_component, a_otherComponent, a_thirdComponent, a_fourthComponent, getNextRow());
	}


	public int addRow(Component a_component, Component a_otherComponent, Component a_thirdComponent,
					   Component a_fourthComponent, int a_fill)
	{
		return replaceRow(a_component, a_otherComponent, a_thirdComponent, a_fourthComponent, getNextRow(),
				   a_fill);
	}

	/**
	 * Add a row with two components.
	 * @param a_component a Component (may be null)
	 * @param a_otherComponent an other Component (may be null)
	 * @param a_thirdComponent a third component (may be null)
	 */
	public int addRow(Component a_component, Component a_otherComponent, Component a_thirdComponent)
	{
		return replaceRow(a_component, a_otherComponent, a_thirdComponent, getNextRow());
	}

	/**
	 * This method is useful if you plan to use your own GridBagLayout for one ore more rows.
	 * It instructs this panel to count a new row but not to fill it. Please set the x and y
	 * values correctly.
	 */
	public int addDummyRow()
	{
		m_rows.addElement(new JLabel());
		return m_rows.size() - 1;
	}

	/**
	 * This method is useful if you plan to use your own GridBagLayout for one ore more rows.
	 * It instructs this panel to count a new row but not to fill it. Please set the x and y
	 * values correctly.
	 * @param a_rows the number of rows you want to fill "manually"
	 */
	public void addDummyRows(int a_rows)
	{
		while (a_rows > 0)
		{
			m_rows.addElement(new JLabel());
			a_rows--;
		}
	}

	/**
	 * This method adds several components. The gridwidth of each component
	 * must be given in a_gridwidths at the corresponding position. If a_gridwidths is NULL,
	 * every component gets assigned a gridwidth of 1 cell.
	 * @param a_components Component[]
	 * @param a_gridwidths int[]
	 */
	public int addRow(Component[] a_components, int a_gridwidths[])
	{
		return replaceRow(a_components, a_gridwidths, getNextRow());
	}

	/**
	 * Replaces a row at the given row number. If the row does not exist, it is added.
	 * @param a_component Component
	 * @param a_otherComponent Component
	 * @param a_rowNumber int
	 */
	public int replaceRow(Component a_component, Component a_otherComponent, int a_rowNumber)
	{
		return replaceRow(a_component, a_otherComponent, a_rowNumber, GridBagConstraints.HORIZONTAL);
	}


	public int  replaceRow(Component a_component, Component a_otherComponent, int a_rowNumber,
						   int a_fill)
	{
		Component[] comps = new Component[2];
		comps[0] = a_component;
		comps[1] = a_otherComponent;
		return replaceRow(comps, null, a_rowNumber, a_fill);
	}

	/**
	 * Replaces a row at the given row number. If the row does not exist, it is added.
	 * @param a_component Component
	 * @param a_otherComponent Component
	 * @param a_thirdComponent Component
	 * @param a_rowNumber int
	 */
	public int replaceRow(Component a_component, Component a_otherComponent,
						   Component a_thirdComponent, int a_rowNumber)
	{
		Component[] comps = new Component[3];
		comps[0] = a_component;
		comps[1] = a_otherComponent;
		comps[2] = a_thirdComponent;
		return replaceRow(comps, null, a_rowNumber);
	}

	/**
	 * Replaces a row at the given row number. If the row does not exist, it is added.
	 * @param a_component Component
	 * @param a_otherComponent Component
	 * @param a_thirdComponent Component
	 * @param a_fourthComponent Component
	 * @param a_rowNumber int
	 * @param a_fill int
	 */
	public int replaceRow(Component a_component, Component a_otherComponent,
						   Component a_thirdComponent, Component a_fourthComponent, int a_rowNumber,
						   int a_fill)
	{
		Component[] comps = new Component[4];
		comps[0] = a_component;
		comps[1] = a_otherComponent;
		comps[2] = a_thirdComponent;
		comps[3] = a_fourthComponent;
		return replaceRow(comps, null, a_rowNumber, a_fill);
	}


	/**
	 * Replaces a row at the given row number. If the row does not exist, it is added.
	 * @param a_component Component
	 * @param a_otherComponent Component
	 * @param a_thirdComponent Component
	 * @param a_fourthComponent Component
	 * @param a_rowNumber int
	 */
	public int replaceRow(Component a_component, Component a_otherComponent,
						   Component a_thirdComponent, Component a_fourthComponent, int a_rowNumber)
	{
		return replaceRow(a_component, a_otherComponent, a_thirdComponent, a_fourthComponent, a_rowNumber,
						  GridBagConstraints.HORIZONTAL);
	}


	/**
	 * Returns the position of the next row to add.
	 * @return the position of the next row to add
	 */
	public int getNextRow()
	{
		return m_rows.size();
	}


	/**
	 * Replaces a row at the given row number. If the row does not exist, it is added.
	 * @param a_components Component[]
	 * @param a_gridwidths int[]
	 * @param a_rowNumber int
	 */
	public int replaceRow(Component[] a_components, int a_gridwidths[], int a_rowNumber)
	{
		return replaceRow(a_components, a_gridwidths, a_rowNumber, GridBagConstraints.HORIZONTAL);
	}

	public int replaceRow(Component[] a_components, int a_gridwidths[], int a_rowNumber, int a_fill)
	{
		int[] gridwidths;
		int currentComponent;
		Vector components;
		Vector deletedComponents;

		if (a_components != null && a_components.length > 0)
		{
			components = new Vector();
			for (int i = 0; i < a_components.length; i++)
			{
				components.addElement(a_components[i]);
			}

			while (m_rows.size() < (a_rowNumber - 1))
			{
				m_rows.addElement(new Vector());
			}

			if (m_rows.size() > a_rowNumber)
			{
				// the components will replace others
				deletedComponents = (Vector)m_rows.elementAt(a_rowNumber);
				for (int i = 0; i < deletedComponents.size(); i++)
				{
					remove((Component)deletedComponents.elementAt(i));
				}
				m_rows.removeElementAt(a_rowNumber);
			}
			m_rows.insertElementAt(components, a_rowNumber);



			if (a_gridwidths != null)
			{
				gridwidths = a_gridwidths;
			}
			else
			{
				gridwidths = new int[a_components.length];
				for (int i = 0; i < gridwidths.length; i++)
				{
					gridwidths[i] = 1;

					currentComponent = i;
					while ( (currentComponent + 1) < a_components.length &&
						   a_components[currentComponent + 1] == null)
					{
						currentComponent++;
						gridwidths[i]++;
					}
				}
			}

			for (int i = 0; i < a_components.length; i++)
			{
				if (a_components[i] == null)
				{
					continue;
				}

				m_constraints.gridx = i;
				m_constraints.gridy = a_rowNumber;
				m_constraints.weightx = 1;
				m_constraints.gridwidth = gridwidths[i];
				if (i == a_components.length - 1)
				{
					m_constraints.weighty = 10;
				}
				else
				{
					m_constraints.weighty = 0;
				}
				m_constraints.fill = a_fill;
				add(a_components[i], m_constraints);
			}
		}
		return a_rowNumber;
	}
}
