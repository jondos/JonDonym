package gui;

import java.awt.event.MouseListener;
import java.awt.event.MouseEvent;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Insets;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.net.URL;

import javax.swing.JLabel;
import javax.swing.JPanel;

import platform.AbstractOS;

import anon.infoservice.MixInfo;
import anon.infoservice.ServiceLocation;
import anon.infoservice.ServiceOperator;
import gui.dialog.JAPDialog;
import jap.JAPConfAnon;
import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;

public class MixDetailsDialog extends JAPDialog implements MouseListener
{
	private MixInfo m_mixInfo;
	private JLabel m_lblOperator;
	
	public MixDetailsDialog(Component a_parent, MixInfo a_mixInfo, int a_mixType)
	{
		super(a_parent, a_mixInfo != null ? a_mixInfo.getName() : "");
		m_mixInfo = a_mixInfo;
		
		GridBagConstraints c = new GridBagConstraints();
		JPanel p = (JPanel) getContentPane();
		p.setLayout(new GridBagLayout());
		JLabel lbl;

		if(m_mixInfo == null)
		{
			return;
		}
		
		ServiceOperator op = m_mixInfo.getServiceOperator();
		ServiceLocation loc = m_mixInfo.getServiceLocation();
		
		if(op == null || loc == null)
		{
			return;
		}
		
		lbl = new JLabel(JAPMessages.getString(JAPConfAnon.class.getName() + "_mixPosition") + ":");
		c.gridy = 0;
		c.gridx = 0;
		c.insets = new Insets(15, 15, 10, 15);
		c.anchor = GridBagConstraints.WEST;
		p.add(lbl, c);
		
		String mixType = "";
		if(a_mixType == 0) mixType = JAPMessages.getString(JAPConfAnon.class.getName() + "_mixFirst");
		else if(a_mixType == 1) mixType = JAPMessages.getString(JAPConfAnon.class.getName() + "_mixMiddle");
		else if(a_mixType == 2) mixType = JAPMessages.getString(JAPConfAnon.class.getName() + "_mixLast");
		
		lbl = new JLabel(mixType);
		c.gridx = 1;
		p.add(lbl, c);
		
		lbl = new JLabel(JAPMessages.getString("mixLocation") + ":");
		c.gridy = 1;
		c.gridx = 0;
		c.insets = new Insets(0, 15, 10, 15);
		p.add(lbl, c);
		
		lbl = new JLabel(loc.getDisplayString());
		c.gridy = 1;
		c.gridx = 1;
		lbl.setIcon(GUIUtils.loadImageIcon("flags/" + loc.getCountry() + ".png"));
		p.add(lbl, c);
		
		lbl = new JLabel(JAPMessages.getString("mixOperator"));
		c.gridx = 0;
		c.gridy = 2;
		c.anchor = GridBagConstraints.WEST;
		c.insets = new Insets(0, 15, 15, 15);
		p.add(lbl, c);
		
		m_lblOperator = new JLabel(op.getOrganization());
		m_lblOperator.setToolTipText(op.getUrl());
		m_lblOperator.setForeground(Color.blue);
		m_lblOperator.addMouseListener(this);
		m_lblOperator.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		if(op.getCertificate() != null && op.getCertificate().getSubject() != null)
		{
			m_lblOperator.setIcon(GUIUtils.loadImageIcon("flags/" + op.getCertificate().getSubject().getCountryCode() + ".png"));
		}
		c.gridx = 1;
		p.add(m_lblOperator, c);
		
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
	}
	
	public void mouseReleased(MouseEvent event)
	{
		
	}
	
	public void mousePressed(MouseEvent event)
	{
		
	}
}
