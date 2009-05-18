/*
Copyright (c) 2008 The JAP-Team, JonDos GmbH

All rights reserved.

Redistribution and use in source and binary forms, with or without modification, 
are permitted provided that the following conditions are met:

    * Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
    * Redistributions in binary form must reproduce the above copyright notice,
       this list of conditions and the following disclaimer in the documentation and/or
       other materials provided with the distribution.
    * Neither the name of the University of Technology Dresden, Germany, nor the name of
       the JonDos GmbH, nor the names of their contributors may be used to endorse or
       promote products derived from this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
"AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE REGENTS OR
CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package jap;


import java.awt.Dimension;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.BitSet;
import java.util.Date;
import java.util.Vector;

import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.AbstractTableModel;

import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;
import anon.infoservice.Database;
import anon.infoservice.ServiceOperator;
import anon.terms.TermsAndConditions;
import anon.util.JAPMessages;


public class TermsAndConditionsOperatorTable extends JTable implements MouseListener
{
	/**
	 * serial version UID
	 */
	private static final long serialVersionUID = 1L;	
	
	private final static int OPERATOR_COL = 0;
	private final static int DATE_COL = 1;
	private final static int ACCEPTED_COL = 2;
	
	private final static String OPERATOR_COL_NAMEKEY = "mixOperator";
	private final static String DATE_COL_NAMEKEY = "validFrom";
	private final static String ACCEPTED_COL_NAMEKEY = JAPConfTC.class.getName() + "_tncAccepted";
	
	private final static int COLS = 3;
	
	private TermsAndCondtionsTableController controller;
	
	public TermsAndConditionsOperatorTable()
	{
		this(null);
	}
	
	public TermsAndConditionsOperatorTable(Vector operators)
	{
		super();
		OperatorTableModel model = (operators != null) ? 
				new OperatorTableModel(operators) : new OperatorTableModel();
		setModel(model);
		setDefaultRenderer(ServiceOperator.class, new OperatorsCellRenderer());
		setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		getColumnModel().getColumn(OPERATOR_COL).setMinWidth(200);
		getColumnModel().getColumn(OPERATOR_COL).setPreferredWidth(200);
		
		getColumnModel().getColumn(DATE_COL).setMinWidth(100);
		getColumnModel().getColumn(DATE_COL).setPreferredWidth(100);
		
		int opertorsSize = getOperators().size();
		setPreferredSize(new Dimension(450, 
				Math.min(10+(opertorsSize*12), 100)));
		
		addMouseListener(this);
	}
	
	public void mouseClicked(MouseEvent e) 
	{
		if(controller != null)
		{
			ServiceOperator selectedOp = (ServiceOperator) getModel().getValueAt(getSelectedRow(), OPERATOR_COL);
			boolean accepted = ((Boolean) getModel().getValueAt(getSelectedRow(), ACCEPTED_COL)).booleanValue();
			if (((getSelectedColumn() == OPERATOR_COL) || (getSelectedColumn() == DATE_COL)) &&
				(e.getClickCount() > 1) )
			{
				getModel().setValueAt(
						new Boolean(controller.handleOperatorAction(selectedOp, accepted)),
						getSelectedRow(), ACCEPTED_COL);
			}
			//Columm ACCEPTED is handled by default ComBoxEditor
			else if ( (getSelectedColumn() != ACCEPTED_COL) )
			{
				controller.handleSelectLineAction(selectedOp);
			}
			repaint();
		}
	}

	public void mouseEntered(MouseEvent e) {}
	public void mouseExited(MouseEvent e) {}
	public void mousePressed(MouseEvent e) {}
	public void mouseReleased(MouseEvent e) {}
	
	
	public TermsAndCondtionsTableController getController() 
	{
		return controller;
	}

	public void setController(TermsAndCondtionsTableController controller) 
	{
		this.controller = controller;
	}
	
	//these methods all delegate to the model
	public void setOperators(Vector operators)
	{
		checkModel();
		((OperatorTableModel) getModel()).setOperators(operators);
		repaint();
	}
	
	public Vector getOperators()
	{
		checkModel();
		return ((OperatorTableModel) getModel()).getOperators();
	}
	
	public Vector getTermsAccepted()
	{
		checkModel();
		return ((OperatorTableModel) getModel()).getTermsAccepted();
	}
	
	public Vector getTermsRejected()
	{
		checkModel();
		return ((OperatorTableModel) getModel()).getTermsRejected();
	}
	
	public boolean areTermsRejected()
	{
		checkModel();
		return ((OperatorTableModel) getModel()).areTermsRejected();
	}
	
	//table operations are only supported for an OperatorTableModel
	private void checkModel()
	{
		if(getModel() == null)
		{
			throw new IllegalStateException("Current model is null");
		}
		if(!(getModel() instanceof OperatorTableModel)) 
		{
			throw new IllegalStateException("Wrong model set "+getModel().getClass());
		}
	}
	
	public void setAccepted(int row, boolean value)
	{
		setValueAt(new Boolean(value), row, ACCEPTED_COL);
	}
	
	private class OperatorTableModel extends AbstractTableModel
	{
		/**
		 * Vector containing all the operators in the list
		 */
		private Vector m_vecOperators = new Vector();
		
		/**
		 * The column names
		 */
		private String columnNames[];
		
		/**
		 * The column classes
		 */
		private Class columnClasses[]; 	
		
		private BitSet accepted = new BitSet();
		
		public OperatorTableModel(Vector operators)
		{
			columnClasses = new Class[COLS];
			columnNames = new String[COLS];
			
			columnClasses[OPERATOR_COL] = ServiceOperator.class;
			columnClasses[DATE_COL] = Date.class;
			columnClasses[ACCEPTED_COL] = Boolean.class;
			
			columnNames[OPERATOR_COL] = JAPMessages.getString(OPERATOR_COL_NAMEKEY);
			columnNames[DATE_COL] = JAPMessages.getString(DATE_COL_NAMEKEY);
			columnNames[ACCEPTED_COL] = JAPMessages.getString(ACCEPTED_COL_NAMEKEY);
			setOperators(operators);
		}
		
		public OperatorTableModel()
		{
			this(Database.getInstance(ServiceOperator.class).getEntryList());
		}
		
		public int getRowCount()
		{
			return m_vecOperators.size();
		}
		
		public int getColumnCount()
		{
			return columnNames.length;
		}
		
		public boolean isCellEditable(int rowIndex, int columnIndex)
		{
			if ( (columnIndex == ACCEPTED_COL) ||
				 (columnIndex == OPERATOR_COL)	) return true;
			else return false;
		}		
		
		public Class getColumnClass(int columnIndex)
		{
			return columnClasses[columnIndex];
		}

		public String getColumnName(int columnIndex)
		{
			return columnNames[columnIndex];
		}		
		
		public Object getValueAt(int rowIndex, int columnIndex)
		{
			try
			{
				switch (columnIndex)
				{
					case OPERATOR_COL:
					{
						return (ServiceOperator)m_vecOperators.elementAt(rowIndex);
						
					}
					case DATE_COL:
					{
						ServiceOperator op = (ServiceOperator) m_vecOperators.elementAt(rowIndex);
						TermsAndConditions tc = TermsAndConditions.getTermsAndConditions(op);
						return (tc != null) ? tc.getDate() : null;
					}
					case ACCEPTED_COL:
					{
						return new Boolean(accepted.get(rowIndex));
					}
					default:
					{
						throw new IndexOutOfBoundsException("No definition for column "+columnIndex);
					}
				}
			}
			catch(Exception ex) 
			{ 
				LogHolder.log(LogLevel.ERR, LogType.GUI, ex);
			}
			
			return null;
		}
		
		public void setValueAt(Object aValue, int rowIndex, int columnIndex)
		{
			switch (columnIndex)
			{
				case OPERATOR_COL:
				{
					break;
				}
				case DATE_COL:
				{
					break;
				}
				case ACCEPTED_COL:
				{
					boolean accept = ((Boolean)aValue).booleanValue();
					setAccepted(rowIndex, accept);
					
					if(controller != null)
					{
						try
						{
							controller.handleAcceptAction((ServiceOperator)getValueAt(rowIndex, OPERATOR_COL), accept);
						}
						catch(IllegalStateException e)
						{
							//reset state if the handler reports that the user action lead to
							//an invalid state
							setAccepted(rowIndex, !accept);
						}
					}
					
					
					break;
				}
				default:
				{
					throw new IndexOutOfBoundsException("No definition for column "+columnIndex);
				}
			}
		}

		public void setOperators(Vector operators)
		{
			Object obj = null;
			m_vecOperators.removeAllElements();
			
			for(int i = 0; i < accepted.size(); i++)
			{
				accepted.clear(i);
			}
			
			if(operators != null)
			{
				int operatorCount = 0;
				for (int i = 0; i < operators.size(); i++) 
				{
					obj = operators.elementAt(i);
					if( (obj instanceof ServiceOperator) && 
						( ((ServiceOperator) obj).hasTermsAndConditions()))
					{
						m_vecOperators.addElement(obj);
						if(TermsAndConditions.getTermsAndConditions((ServiceOperator) obj).isAccepted())
						{
							accepted.set(operatorCount);
						}
						operatorCount++;
					}
				}
			}
		}
		
		public Vector getOperators()
		{
			return m_vecOperators;
		}
		
		public ServiceOperator getOperator(int row)
		{
			return (ServiceOperator) getValueAt(row, OPERATOR_COL);
		}

		public Vector getTermsAccepted()
		{
			return getTermsWithAcceptStatus(true);
		}
		
		public Vector getTermsRejected()
		{
			return getTermsWithAcceptStatus(false);
		}
		
		public boolean areTermsRejected()
		{
			for (int i = 0; i < m_vecOperators.size(); i++)
			{
				if(!accepted.get(i)) return true;
			}
			return false;
		}
		
		// simple workaround because the corresponding bitset operation is 
		//not available for Java < 1.4
		public void setAccepted(int index, boolean value)
		{
			if(value) 
			{
				accepted.set(index);
			}
			else 
			{
				accepted.clear(index);
			}
		}
		
		private Vector getTermsWithAcceptStatus(boolean acceptStatus)
		{
			Vector v = new Vector();
			ServiceOperator o = null;
			for (int i=0; i < m_vecOperators.size(); i++)
			{
				o = (ServiceOperator) m_vecOperators.elementAt(i);
				if( o.hasTermsAndConditions() && 
					(accepted.get(i) == acceptStatus))
				{
					v.addElement(TermsAndConditions.getTermsAndConditions(o));
				}
			}
			return v;
		}
	}
}
