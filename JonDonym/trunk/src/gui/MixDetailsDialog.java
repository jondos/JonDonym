package gui;

import java.awt.Component;
import java.awt.Insets;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;

import anon.infoservice.MixInfo;
import anon.infoservice.ServiceLocation;
import anon.infoservice.ServiceOperator;
import gui.dialog.JAPDialog;

public class MixDetailsDialog extends JAPDialog 
{
	private MixInfo m_mixInfo;
	
	public MixDetailsDialog(Component a_parent, MixInfo a_mixInfo)
	{
		super(a_parent, a_mixInfo != null ? a_mixInfo.getName() : "");
		m_mixInfo = a_mixInfo;
		
		GridBagConstraints c = new GridBagConstraints();
		JPanel p = (JPanel) getContentPane();
		p.setLayout(new GridBagLayout());

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
		
		JLabel lbl = new JLabel(JAPMessages.getString("mixOperator"));
		c.gridx = 0;
		c.gridy = 0;
		c.anchor = GridBagConstraints.WEST;
		c.insets = new Insets(15, 15, 10, 15);
		p.add(lbl, c);
		
		lbl = new JLabel(op.getOrganization());
		if(op.getCertificate() != null && op.getCertificate().getSubject() != null)
		{
			lbl.setIcon(GUIUtils.loadImageIcon("flags/" + op.getCertificate().getSubject().getCountryCode() + ".png"));
		}
		c.gridx = 1;
		p.add(lbl, c);
		
		lbl = new JLabel(JAPMessages.getString("mixLocation"));
		c.gridy = 1;
		c.gridx = 0;
		c.insets = new Insets(0, 15, 15, 15);
		p.add(lbl, c);
		
		lbl = new JLabel(loc.getDisplayString());
		c.gridy = 1;
		c.gridx = 1;
		lbl.setIcon(GUIUtils.loadImageIcon("flags/" + loc.getCountry() + ".png"));
		p.add(lbl, c);
		
		this.pack();
		p.setVisible(true);
	}
}
