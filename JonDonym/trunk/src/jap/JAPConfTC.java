package jap;

import gui.JAPMessages;
import gui.OperatorsCellRenderer;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.Date;
import java.util.Enumeration;
import java.util.Observable;
import java.util.Observer;
import java.util.Vector;

import javax.swing.JEditorPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;

import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;
import anon.client.ITermsAndConditionsContainer;
import anon.infoservice.Database;
import anon.infoservice.ServiceOperator;
import anon.infoservice.TermsAndConditions;

public class JAPConfTC extends AbstractJAPConfModule implements ListSelectionListener, Observer
{
	private static final String MSG_TAB_TITLE = JAPConfTC.class.getName() + "_tabTitle";
	
	JTable m_tblOperators;
	private JEditorPane m_termsPane;
	private JScrollPane m_scrollingTerms;
	private ITermsAndConditionsContainer m_tcc;
	
	protected JAPConfTC(IJAPConfSavePoint savePoint, ITermsAndConditionsContainer tcc)
	{
		super(null);
		if (tcc == null)
		{
			throw new NullPointerException();
		}
		m_tcc = tcc;
	}
	
	protected boolean initObservers()
	{
		if (super.initObservers())
		{
			synchronized(LOCK_OBSERVABLE)
			{
				m_tcc.getTermsAndConditionsResponseHandler().addObserver(this);
				return true;
			}
		}
		return false;
	}
	
	public String getTabTitle() 
	{
		return JAPMessages.getString(MSG_TAB_TITLE);
	}
	
	public void recreateRootPanel() 
	{
		JPanel root = getRootPanel();
		root.setLayout(new GridBagLayout());
		
		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.HORIZONTAL;
		c.weightx = 1;
		c.weighty = 0.2;
		c.gridx = 0;
		c.gridy = 0;
		c.anchor = GridBagConstraints.NORTHWEST;
		
		m_tblOperators = new JTable();
		m_tblOperators.setModel(new OperatorsTableModel(JAPController.getInstance()));
		m_tblOperators.getColumnModel().getColumn(OperatorsTableModel.ACCEPTED_COL).setMinWidth(4);
		m_tblOperators.getColumnModel().getColumn(OperatorsTableModel.ACCEPTED_COL).setPreferredWidth(4);
		m_tblOperators.setDefaultRenderer(ServiceOperator.class, new OperatorsCellRenderer());
		m_tblOperators.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		m_tblOperators.getSelectionModel().addListSelectionListener(this);
		//m_tblOperators.getDefaultEditor(Boolean.class).addCellEditorListener(new AcceptedRejectListener(m_tblOperators));
		JScrollPane scroll;

		scroll = new JScrollPane(m_tblOperators);
		scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		//scroll.setMinimumSize(new Dimension(100, 100));
		//scroll.setPreferredSize(preferredSize);
		
		root.add(scroll, c);
		
		c.gridy++;
		
		m_termsPane = new JEditorPane("text/html", "");
		m_termsPane.addHyperlinkListener(new JAPHyperlinkAdapter());
		m_termsPane.setEditable(false);
		m_scrollingTerms = new JScrollPane(m_termsPane);
		/**@todo make this dynamic */
		m_scrollingTerms.setPreferredSize(new Dimension(400,200));
		root.add(m_scrollingTerms, c);
		
		root.validate();
	}

	public String getHelpContext() 
	{
		return "services_tc";
	}
	
	protected void onUpdateValues()
	{
		//synchronized (JAPConf.getInstance())
		{
			((OperatorsTableModel) m_tblOperators.getModel()).update();
		}
	}
	
	/**
	 * Handles the selection of an operator
	 * @param e ListSelectionEvent
	 */
	public void valueChanged(ListSelectionEvent e)
	{
		if(!e.getValueIsAdjusting())
		{
			ServiceOperator op = (ServiceOperator) m_tblOperators.getValueAt(
					m_tblOperators.getSelectedRow(), OperatorsTableModel.OPERATOR_COL);
			
			m_termsPane.setText("");
			
			if(op != null)
			{
				//String opIdWithoutColons = Util.replaceAll(op.getId(),":", "");
				TermsAndConditions tc = TermsAndConditions.getTermsAndConditions(op);
				if(tc == null)
				{
					return;
				}
				String tcHtmlText = tc.getHTMLText(JAPMessages.getLocale());
				//TermsAndConditionsFramework fr = TermsAndConditionsFramework.getById(tc.getReferenceId(), true);
				
				//if(fr == null)
				//{
				//	return;
				//}
				
				//fr.importData(tc);
				m_termsPane.setText(tcHtmlText);
			}
		}
	}
	
	/*private class AcceptedRejectListener implements CellEditorListener
	{

		JTable target = null;
		
		public AcceptedRejectListener(JTable target)
		{
			if(target == null) throw new NullPointerException("target table is null"); 
			this.target = target;
		}
		
		public void editingCanceled(ChangeEvent e)
		{
		}

		public void editingStopped(ChangeEvent e) 
		{
			TableCellEditor tced = (TableCellEditor) e.getSource();
			//boolean value = ((Boolean)).booleanValue();
			target.getModel().setValueAt(tced.getCellEditorValue(), target.getSelectedRow(), target.getSelectedColumn());
		}
		
	}*/
	
	private static class OperatorsTableModel extends AbstractTableModel
	{
		/**
		 * serial version UID
		 */
		private static final long serialVersionUID = 1L;

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
		
		private final static int OPERATOR_COL = 0;
		private final static int DATE_COL = 1;
		private final static int ACCEPTED_COL = 2;
		
		private final static String OPERATOR_COL_NAMEKEY = "mixOperator";
		private final static String DATE_COL_NAMEKEY = "validFrom";
		private final static String ACCEPTED_COL_NAMEKEY = JAPConfTC.class.getName() + "_tncAccepted";
		
		private final static int COLS = 3;
		
		private ITermsAndConditionsContainer tncModel;
		
		private OperatorsTableModel(ITermsAndConditionsContainer tncModel)
		{
			columnClasses = new Class[COLS];
			columnNames = new String[COLS];
			
			columnClasses[OPERATOR_COL] = ServiceOperator.class;
			columnClasses[DATE_COL] = Date.class;
			columnClasses[ACCEPTED_COL] = Boolean.class;
			
			columnNames[OPERATOR_COL] = JAPMessages.getString(OPERATOR_COL_NAMEKEY);
			columnNames[DATE_COL] = JAPMessages.getString(DATE_COL_NAMEKEY);
			columnNames[ACCEPTED_COL] = JAPMessages.getString(ACCEPTED_COL_NAMEKEY);
			this.tncModel = tncModel;
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
			if (columnIndex == ACCEPTED_COL) return true;
			else return false;
		}		
		
		public synchronized void update()
		{
			/*if(m_trustModelCopy != null)
				m_vecBlacklist = (Vector) ((Vector) m_trustModelCopy.getAttribute(TrustModel.OperatorBlacklistAttribute.class).getConditionValue()).clone();*/
			
			m_vecOperators.removeAllElements();
			Vector allOperators = Database.getInstance(ServiceOperator.class).getEntryList();
			for (Enumeration enumeration = allOperators.elements(); enumeration.hasMoreElements();)
			{
				ServiceOperator operator = (ServiceOperator) enumeration.nextElement();
				//System.out.println("Operator "+operator.getId());
				if(operator.hasTermsAndConditions())
				{
					//System.out.println("has tcs.");
					m_vecOperators.addElement(operator);
				}
			}
			//m_vecOperators = Database.getInstance(ServiceOperator.class).getEntryList();
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
						//if(op == null) return null;
						TermsAndConditions tc = TermsAndConditions.getTermsAndConditions(op);
						return (tc != null) ? tc.getDate() : null;
					}
					case ACCEPTED_COL:
					{
						ServiceOperator op = (ServiceOperator) m_vecOperators.elementAt(rowIndex);
						//if(op == null) return null; //must never happen
						TermsAndConditions tc = TermsAndConditions.getTermsAndConditions(op);
						//if(tc == null) return null; //must never happen
						return new Boolean(tc.isAccepted());
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
					boolean value = ((Boolean) aValue).booleanValue(); 
					ServiceOperator op = (ServiceOperator) m_vecOperators.elementAt(rowIndex);
					//if(op == null) return null; //must never happen
					TermsAndConditions tc = TermsAndConditions.getTermsAndConditions(op);
					//if(tc == null) return null; //must never happen
					tc.setAccepted(value);
					tc.setRead(true);
					break;
				}
				default:
				{
					throw new IndexOutOfBoundsException("No definition for column "+columnIndex);
				}
			}
			/*if(aValue instanceof Boolean)
			{
				
				ServiceOperator currentOp = 
					(ServiceOperator) getValueAt(rowIndex, OPERATOR_COL);
				
				if(!value && tncModel.hasAcceptedTermsAndConditions(currentOp))
				{
					tncModel.revokeTermsAndConditions(currentOp);
					
				}
				else if (!tncModel.hasAcceptedTermsAndConditions(currentOp))
				{
					tncModel.acceptTermsAndConditions(currentOp);
					
				}
			}*/
		}
		
		/*public void setValueAt(Object aValue, int rowIndex, int columnIndex)
		{
			try
			{
				switch (columnIndex)
				{
					case ACCEPTED_COL:
					{
						Object op = m_vecOperators.elementAt(rowIndex);
						if(aValue == Boolean.FALSE)
						{
							//if(!m_vecBlacklist.contains(op))
							//{
							//	m_vecBlacklist.addElement(op);
							//}
						}
						else
						{
							//m_vecBlacklist.removeElement(op);
						}
						break;
					}
					default:
					{
						throw new IndexOutOfBoundsException("No definition for column "+columnIndex+" or column not editable");
					}
				}
			}
			catch(Exception ex) 
			{ 
				LogHolder.log(LogLevel.ERR, LogType.GUI, ex);
			}
		}*/
	}

	public void update(Observable o, Object arg) 
	{
		//System.out.println("Updating table");
		//onUpdateValues();
	}
}
