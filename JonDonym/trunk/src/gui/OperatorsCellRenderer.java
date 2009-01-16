package gui;

import java.awt.Color;

import javax.swing.table.DefaultTableCellRenderer;

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
		}
	}
}