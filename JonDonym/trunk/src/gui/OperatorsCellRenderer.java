package gui;

import java.awt.Color;
import java.awt.Component;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.EventObject;

import javax.swing.JTable;
import javax.swing.event.CellEditorListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellEditor;

import anon.infoservice.ServiceOperator;

public class OperatorsCellRenderer extends DefaultTableCellRenderer
{
	/**
	 * serial version UID
	 */
	private static final long serialVersionUID = 1L;

	public void setValue(Object value)
	{
		if (value == null)
		{
			setText("");
			return;
		}
		else if (value instanceof ServiceOperator)
		{
			ServiceOperator op = (ServiceOperator) value;
			setForeground(Color.black);
			
			if (op.getCertificate() == null)
			{
				setForeground(Color.gray);
			}
			setText(op.getOrganization());
			setIcon(GUIUtils.loadImageIcon("flags/" + op.getCountryCode() + ".png"));
		}
	}
}