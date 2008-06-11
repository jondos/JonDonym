package gui;

import java.awt.event.MouseListener;
import java.awt.event.MouseEvent;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.MouseEvent;
import java.net.URL;

import javax.swing.JLabel;
import javax.swing.JPanel;

import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;
import platform.AbstractOS;

import anon.infoservice.ServiceLocation;
import anon.infoservice.ServiceOperator;
import gui.dialog.JAPDialog;

public class OperatorDetailsDialog extends JAPDialog implements MouseListener
{
	private ServiceOperator m_operator;
	private JLabel m_lblOperator;
	private JLabel m_lblEMail;
	
	private static String MSG_TITLE = OperatorDetailsDialog.class.getName() + "_title";
	
	public OperatorDetailsDialog(Component a_parent, ServiceOperator a_operator)
	{
		super(a_parent, JAPMessages.getString(MSG_TITLE));
		m_operator = a_operator;
		
		GridBagConstraints c = new GridBagConstraints();
		JPanel p = (JPanel) getContentPane();
		p.setLayout(new GridBagLayout());
		JLabel lbl;

		if(m_operator == null)
		{
			return;
		}
		
		lbl = new JLabel(JAPMessages.getString("mixOperator"));
		c.gridx = 0;
		c.gridy = 0;
		c.anchor = GridBagConstraints.WEST;
		c.insets = new Insets(15, 15, 0, 15);
		p.add(lbl, c);
		
		m_lblOperator = new JLabel(m_operator.getOrganization());
		m_lblOperator.setToolTipText(m_operator.getUrl());
		m_lblOperator.setForeground(Color.blue);
		m_lblOperator.addMouseListener(this);
		m_lblOperator.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		if(m_operator.getCertificate() != null && m_operator.getCertificate().getSubject() != null)
		{
			m_lblOperator.setIcon(GUIUtils.loadImageIcon("flags/" + m_operator.getCertificate().getSubject().getCountryCode() + ".png"));
		}
		c.gridx = 1;
		p.add(m_lblOperator, c);
		
		lbl = new JLabel(JAPMessages.getString("eMail:"));
		c.gridx = 0;
		c.gridy = 1;
		c.insets = new Insets(10, 15, 15, 15);
		p.add(lbl, c);
		
		m_lblEMail = new JLabel(m_operator.getEMail());
		m_lblEMail.setToolTipText(m_operator.getEMail());
		m_lblEMail.setForeground(Color.blue);
		m_lblEMail.addMouseListener(this);
		m_lblEMail.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

		c.gridx = 1;
		p.add(m_lblEMail, c);
		
		this.pack();
		this.setResizable(false);
		p.setVisible(true);
	}
	
	
	public void mouseEntered(MouseEvent event)
	{
		
	}
	
	public void mouseExited(MouseEvent event)
	{
		
	}
	
	public void mouseClicked(MouseEvent event)
	{
		if(event.getSource() == m_lblOperator)
		{
			String url = m_lblOperator.getToolTipText();
			if (url == null)
			{
				return;
			}

			AbstractOS os = AbstractOS.getInstance();
			try
			{
				os.openURL(new URL(url));
			}
			catch (Exception a_e)
			{
				LogHolder.log(LogLevel.ERR, LogType.MISC, "Error opening URL in browser");
			}
		}
		else if (event.getSource() == m_lblEMail)
		{
			AbstractOS.getInstance().openEMail(m_lblEMail.getToolTipText());
		}
	}
	
	public void mouseReleased(MouseEvent event)
	{
		
	}
	
	public void mousePressed(MouseEvent event)
	{
		
	}
}
