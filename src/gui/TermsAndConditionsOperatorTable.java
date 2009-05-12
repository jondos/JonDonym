package gui;

import gui.dialog.JAPDialog;
import jap.JAPConf;
import jap.JAPConfTC;
import jap.JAPController;

import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.BitSet;
import java.util.Date;
import java.util.Vector;

import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
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
	private static final String MSG_TAB_TITLE = JAPConfTC.class.getName() + "_tabTitle";
	private static final String MSG_ERR_REJECT_IMPOSSIBLE = JAPConfTC.class.getName() + "_errRejectImpossible";
	
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
		addMouseListener(this);
	}
	
	public void mouseClicked(MouseEvent e) 
	{
		if(controller != null)
		{
			ServiceOperator selectedOp = (ServiceOperator) getModel().getValueAt(getSelectedRow(), OPERATOR_COL);
			boolean accepted = ((Boolean) getModel().getValueAt(getSelectedRow(), ACCEPTED_COL)).booleanValue();
			if ((getSelectedColumn() == OPERATOR_COL) &&
					e.getClickCount() > 1)
			{
				getModel().setValueAt(
						new Boolean(controller.handleOperatorAction(selectedOp, accepted)),
						getSelectedRow(), ACCEPTED_COL);
			}
			//Columm ACCEPTED Is handled by default ComBoxEditor
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
	
	protected void handleAcceptAction(TermsAndConditions terms, boolean accept)
	{
		if(!accept && !JAPController.getInstance().isTCRejectingPossible(terms))
		{
			JAPDialog.showErrorDialog(JAPConf.getInstance(), 
					JAPMessages.getString(MSG_ERR_REJECT_IMPOSSIBLE, terms.getOperator().getOrganization()), LogType.MISC);
		}
		else
		{
			terms.setAccepted(accept);
		}
		terms.setRead(true);
	}
	
	public void setOperators(Vector operators)
	{
		if(getModel() != null && getModel() instanceof OperatorTableModel)
		{
			((OperatorTableModel) getModel()).setOperators(operators);
		}
		repaint();
	}
	
	public Vector getOperators()
	{
		if(getModel() != null && getModel() instanceof OperatorTableModel)
		{
			return ((OperatorTableModel) getModel()).getOperators();
		}
		return null;
	}
	
	public Vector getTermsAccepted()
	{
		if(getModel() != null && getModel() instanceof OperatorTableModel)
		{
			return ((OperatorTableModel) getModel()).getTermsAccepted();
		}
		return null;
	}
	
	public Vector getTermsRejected()
	{
		if(getModel() != null && getModel() instanceof OperatorTableModel)
		{
			return ((OperatorTableModel) getModel()).getTermsRejected();
		}
		return null;
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
						//ServiceOperator op = (ServiceOperator) m_vecOperators.elementAt(rowIndex);
						//TermsAndConditions tc = TermsAndConditions.getTermsAndConditions(op);
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
					if(((Boolean)aValue).booleanValue()) 
					{
						System.out.println("set index: "+rowIndex);
						accepted.set(rowIndex);
					}
					else 
					{
						System.out.println("clear index: "+rowIndex);
						accepted.clear(rowIndex);
					}
					
					if(controller != null)
					{
						//ServiceOperator op = (ServiceOperator) m_vecOperators.elementAt(rowIndex);
						//TermsAndConditions tc = TermsAndConditions.getTermsAndConditions(op);
						//controller.handleAcceptAction(tc, ((Boolean)aValue).booleanValue());
						
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
					v.add(TermsAndConditions.getTermsAndConditions(o));
				}
			}
			return v;
		}
		
		public void valueChanged(ListSelectionEvent e) 
		{
			// TODO Auto-generated method stub
			
		}
	}
}
