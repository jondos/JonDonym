package gui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Insets;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.net.URL;
import java.util.Date;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

import platform.AbstractOS;

import anon.infoservice.MixInfo;
import anon.infoservice.ServiceLocation;
import anon.infoservice.ServiceOperator;
import gui.dialog.JAPDialog;
import gui.MultiCertOverview;
import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;

public class MixDetailsDialog extends JAPDialog
{	
	public static final String MSG_NOT_VERIFIED = MixDetailsDialog.class.getName() + "_notVerified";
	public static final String MSG_INVALID = MixDetailsDialog.class.getName() + "_invalid";
	public static final String MSG_VALID = MixDetailsDialog.class.getName() + "_valid";
	public static final String MSG_INDEPENDENT_CERTIFICATIONS = MixDetailsDialog.class.getName() + "_independentCertifications";
	public static final String MSG_MIX_X_OF_Y = MixDetailsDialog.class.getName() + "_mixXOfY";
	
	public static String MSG_MIX_NAME = MixDetailsDialog.class.getName() + "_mixName";	
	public static String MSG_LOCATION = MixDetailsDialog.class.getName() + "_mixLocation";
	public static String MSG_HOMEPAGE = MixDetailsDialog.class.getName() + "_operatorHomepage";
	public static String MSG_E_MAIL = MixDetailsDialog.class.getName() + "_email";
	public static String MSG_CERTIFICATES = MixDetailsDialog.class.getName() + "_certificates";
	public static String MSG_BTN_DATA_RETENTION = MixDetailsDialog.class.getName() + "_btnDataRetention";
	
	private static String MSG_TITLE = MixDetailsDialog.class.getName() + "_title";
	
	private MixInfo m_mixInfo;
	private ActionListener m_buttonListener;
	private JButton m_btnHomepage, m_btnEMail, m_btnCertificates, m_btnDataRetention;
	
	public MixDetailsDialog(Component a_parent, MixInfo a_mixInfo, int a_mixType)
	{
		super(a_parent, JAPMessages.getString(MSG_TITLE));
		m_mixInfo = a_mixInfo;
		
		GridBagConstraints c = new GridBagConstraints();
		JPanel p = (JPanel) getContentPane();
		JPanel pnlButtons = new JPanel(new FlowLayout());
		p.setLayout(new GridBagLayout());
		JLabel lbl;
		String strText;
		
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
		
		lbl = new JLabel(JAPMessages.getString(MSG_MIX_NAME) + ":");
		c.gridy = 0;
		c.gridx = 0;
		c.insets = new Insets(15, 15, 10, 15);
		c.anchor = GridBagConstraints.WEST;
		p.add(lbl, c);
		
		lbl = new JLabel(a_mixInfo.getName());
		c.gridx = 1;
		p.add(lbl, c);
		
		lbl = new JLabel(JAPMessages.getString(MSG_LOCATION) + ":");
		c.gridy = 1;
		c.gridx = 0;
		c.insets = new Insets(0, 15, 10, 15);
		p.add(lbl, c);
		
		lbl = new JLabel(GUIUtils.getCountryFromServiceLocation(loc));
		c.gridy = 1;
		c.gridx++;
		lbl.setIcon(GUIUtils.loadImageIcon("flags/" + loc.getCountryCode() + ".png"));
		p.add(lbl, c);
		
		lbl = new JLabel(JAPMessages.getString("mixOperator"));
		c.gridx = 0;
		c.gridy++;
		c.anchor = GridBagConstraints.WEST;
		p.add(lbl, c);		
		
		strText = op.getOrganization();
		lbl = new JLabel();	
		
		if (op.getCountryCode() != null)
		{
			strText += "  (" + (new CountryMapper(
					op.getCountryCode(), 
					JAPMessages.getLocale()).toString()) + ")";
			lbl.setIcon(GUIUtils.loadImageIcon("flags/" + op.getCountryCode() + ".png"));
		}
		
		lbl.setText(strText);
		c.gridx = 1;
		p.add(lbl, c);
		
		m_buttonListener = new MyButtonListener();
		c.gridx = 0;
		c.gridy++;
		c.gridwidth = 2;
		p.add(pnlButtons, c);
		
		
		if (m_mixInfo.getCertPath() != null)
		{
			m_btnCertificates = new JButton(JAPMessages.getString(MSG_CERTIFICATES));
			m_btnCertificates.addActionListener(m_buttonListener);
			if (!m_mixInfo.getCertPath().isVerified())
			{
				m_btnCertificates.setIcon(GUIUtils.loadImageIcon(MultiCertOverview.IMG_NOT_TRUSTED));
				m_btnCertificates.setToolTipText(JAPMessages.getString(MSG_NOT_VERIFIED));
				m_btnCertificates.setForeground(Color.red);

			}
			else if (!m_mixInfo.getCertPath().isValid(new Date()))
			{
				m_btnCertificates.setIcon(GUIUtils.loadImageIcon(MultiCertOverview.IMG_INVALID));
				m_btnCertificates.setToolTipText(JAPMessages.getString(MSG_INVALID));
			}
			else if (m_mixInfo.getCertPath().countVerifiedPaths() > 1)
			{
				m_btnCertificates.setToolTipText(JAPMessages.getString(MSG_INDEPENDENT_CERTIFICATIONS, 
						m_mixInfo.getCertPath().countVerifiedPaths()));
				if (m_mixInfo.getCertPath().countVerifiedPaths() > 2)
				{
					m_btnCertificates.setIcon(GUIUtils.loadImageIcon(MultiCertOverview.IMG_TRUSTED_THREE_CERTS));				
				}
				else
				{
					m_btnCertificates.setIcon(GUIUtils.loadImageIcon(MultiCertOverview.IMG_TRUSTED_DOUBLE));
				}				
			}
			else 
			{
				m_btnCertificates.setToolTipText(JAPMessages.getString(MSG_VALID));
				m_btnCertificates.setIcon(GUIUtils.loadImageIcon(MultiCertOverview.IMG_TRUSTED));
			}
			
			pnlButtons.add(m_btnCertificates, c);
		}
		
		if (op.getEMail() != null)
		{
			m_btnEMail = new JButton(JAPMessages.getString(MSG_E_MAIL));
			m_btnEMail.setToolTipText(op.getEMail());
			m_btnEMail.addActionListener(m_buttonListener);
			pnlButtons.add(m_btnEMail, c);			
		}
		
		if (op.getUrl() != null)
		{
			m_btnHomepage = new JButton(JAPMessages.getString(MSG_HOMEPAGE));
			m_btnHomepage.setToolTipText(op.getUrl());
			m_btnHomepage.addActionListener(m_buttonListener);
			pnlButtons.add(m_btnHomepage, c);
		}
		
		URL urlDataRetention = a_mixInfo.getDataRetentionURL(JAPMessages.getLocale().getLanguage());
		if (urlDataRetention != null)
		{
			m_btnDataRetention = new JButton(JAPMessages.getString(MSG_BTN_DATA_RETENTION),
					GUIUtils.loadImageIcon(MultiCertOverview.IMG_INVALID));
			m_btnDataRetention.setToolTipText(urlDataRetention.toString());
			m_btnDataRetention.addActionListener(m_buttonListener);
			pnlButtons.add(m_btnDataRetention, c);
		}
		
		this.pack();
		this.setResizable(false);
		p.setVisible(true);
	}
	
	
	private class MyButtonListener implements ActionListener
	{
		public void actionPerformed(ActionEvent a_event)
		{
			if (a_event.getSource() == m_btnHomepage || a_event.getSource() == m_btnDataRetention)
			{
				String url = ((JButton)a_event.getSource()).getToolTipText();
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
			else if (a_event.getSource() == m_btnEMail)
			{
				String url = m_btnEMail.getToolTipText();
				if (url == null)
				{
					return;
				}

				AbstractOS os = AbstractOS.getInstance();
				try
				{
					os.openEMail(url);
				}
				catch (Exception a_e)
				{
					LogHolder.log(LogLevel.ERR, LogType.MISC, "Error creating E-Mail!");
				}
			}
			else if (a_event.getSource() == m_btnCertificates)
			{
				new MultiCertOverview(MixDetailsDialog.this.getContentPane(), m_mixInfo.getCertPath(), m_mixInfo.getName(), false);
			}
		}
	}
}
