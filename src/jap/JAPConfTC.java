package jap;

import jap.pay.wizardnew.TermsAndConditionsPane;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.Enumeration;
import java.util.Vector;

import javax.swing.JEditorPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.table.AbstractTableModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import anon.infoservice.Database;
import anon.infoservice.MixCascade;
import anon.infoservice.ServiceOperator;
import anon.infoservice.TermsAndConditions;
import anon.infoservice.TermsAndConditionsFramework;
import anon.util.Util;

import gui.OperatorsCellRenderer;
import gui.JAPMessages;

public class JAPConfTC extends AbstractJAPConfModule implements ListSelectionListener
{
	private static final String MSG_TAB_TITLE = JAPConfTC.class.getName() + "_tabTitle";
	private static final String MSG_TNC_ACEPTED = JAPConfTC.class.getName() + "_tncAccepted";
	
	JTable m_tblOperators;
	private JEditorPane m_termsPane;
	private JScrollPane m_scrollingTerms;
	
	protected JAPConfTC(IJAPConfSavePoint savePoint)
	{
		super(null);
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
		c.weighty = 0.5;
		c.gridx = 0;
		c.gridy = 0;
		c.anchor = GridBagConstraints.NORTHWEST;
		
		m_tblOperators = new JTable();
		m_tblOperators.setModel(new OperatorsTableModel());
		m_tblOperators.getColumnModel().getColumn(0).setMinWidth(4);
		m_tblOperators.getColumnModel().getColumn(0).setPreferredWidth(4);
		m_tblOperators.getColumnModel().getColumn(1).setCellRenderer(new OperatorsCellRenderer());
		m_tblOperators.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		m_tblOperators.getSelectionModel().addListSelectionListener(this);
		
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
		((OperatorsTableModel) m_tblOperators.getModel()).update();
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
					m_tblOperators.getSelectedRow(), 1);
			
			m_termsPane.setText("");
			
			if(op != null)
			{
				String opIdWithoutColons = Util.replaceAll(op.getId(),":", "");
				TermsAndConditions tc = TermsAndConditions.getById(opIdWithoutColons, JAPMessages.getLocale());
				
				if(tc == null)
				{
					return;
				}
				TermsAndConditionsFramework fr = TermsAndConditionsFramework.getById(tc.getReferenceId(), true);
				
				if(fr == null)
				{
					return;
				}
				
				fr.importData(tc);
				//TODO: links don't work
				m_termsPane.setText(fr.transform());
			}
		}
	}
	
	private class OperatorsTableModel extends AbstractTableModel
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
		private String columnNames[] = new String[] { JAPMessages.getString(MSG_TNC_ACEPTED), JAPMessages.getString("mixOperator") };
		
		/**
		 * The column classes
		 */
		private Class columnClasses[] = new Class[] { Boolean.class, Object.class};		
		
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
			if (columnIndex == 0) return true;
			else return false;
		}		
		
		public synchronized void update()
		{
			/*if(m_trustModelCopy != null)
				m_vecBlacklist = (Vector) ((Vector) m_trustModelCopy.getAttribute(TrustModel.OperatorBlacklistAttribute.class).getConditionValue()).clone();*/
			
			Vector allOperators = Database.getInstance(ServiceOperator.class).getEntryList();
			for (Enumeration enumeration = allOperators.elements(); enumeration.hasMoreElements();)
			{
				ServiceOperator operator = (ServiceOperator) enumeration.nextElement();
				if(operator.hasTermsAndConditions())
				{
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
				if(columnIndex == 0)
				{
					//return new Boolean(!m_vecBlacklist.contains(m_vecOperators.elementAt(rowIndex)));
					
					ServiceOperator op = (ServiceOperator) m_vecOperators.elementAt(rowIndex);
					return new Boolean(JAPController.getInstance().hasAcceptedTermsAndConditions(op));
					//return Boolean.FALSE;
				}
				if(columnIndex == 1)
				{
					return m_vecOperators.elementAt(rowIndex);
				}
			}
			catch(Exception ex) { }
			
			return null;
		}
		
		public void setValueAt(Object aValue, int rowIndex, int columnIndex)
		{
			if(columnIndex == 0)
			{
				try
				{
					Object op = m_vecOperators.elementAt(rowIndex);
					
					if(aValue == Boolean.FALSE)
					{
						/*if(!m_vecBlacklist.contains(op))
						{
							m_vecBlacklist.addElement(op);
						}*/
					}
					else
					{
						//m_vecBlacklist.removeElement(op);
					}
				}
				catch(Exception ex) { }
			}
		}
	}
}
