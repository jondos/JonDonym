/*
 Copyright (c) 2000 - 2008, The JAP-Team
 All rights reserved.
 Redistribution and use in source and binary forms, with or without modification,
 are permitted provided that the following conditions are met:

  - Redistributions of source code must retain the above copyright notice,
 this list of conditions and the following disclaimer.

  - Redistributions in binary form must reproduce the above copyright notice,
 this list of conditions and the following disclaimer in the documentation and/or
 other materials provided with the distribution.

  - Neither the name of the University of Technology Dresden, Germany nor the names of its contributors
 may be used to endorse or promote products derived from this software without specific
 prior written permission.


 THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS ``AS IS'' AND ANY EXPRESS
 OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY
 AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE REGENTS OR CONTRIBUTORS
 BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA,
 OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER
 IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
 OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE
 */

package gui;

import gui.dialog.JAPDialog;

import java.awt.Color;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSeparator;
import javax.swing.JTabbedPane;
import javax.swing.SwingConstants;
import javax.swing.border.BevelBorder;
import javax.swing.border.EmptyBorder;

import anon.crypto.CertPathInfo;
import anon.crypto.JAPCertificate;
import anon.crypto.MultiCertPath;
import anon.crypto.MyECPublicKey;
import anon.crypto.MyRSAPublicKey;

/**
 * 
 * @author Robert Hirschberger
 */
public class MultiCertOverview extends JAPDialog implements MouseListener
{
	/** Messages */
	private static final String TITLE = MultiCertOverview.class.getName() + "_title";
	private static final String SUMMARY = MultiCertOverview.class.getName() + "_summary";
	private static final String EXPLANATION = MultiCertOverview.class.getName() + "_explanation";
	private static final String MSG_NUMBER_OF_CERTS_ONE = MultiCertOverview.class.getName() + "_numberOfCertsOne";
	private static final String MSG_NUMBER_OF_CERTS = MultiCertOverview.class.getName() + "_numberOfCerts";
	private static final String MSG_NUMBER_OF_TRUSTED_CERTS_ONE = MultiCertOverview.class.getName() + "_numberOfTrustedCertsOne";
	private static final String MSG_NUMBER_OF_TRUSTED_CERTS = MultiCertOverview.class.getName() + "_numberOfTrustedCerts";
	private static final String MSG_IDENTITY_ONE = MultiCertOverview.class.getName() + "_identityOne";
	private static final String MSG_IDENTITY = MultiCertOverview.class.getName() + "_identity";
	private static final String MSG_SHOW_DETAILS = MultiCertOverview.class.getName() + "_details";
	private static final String MSG_SYMBOLS = MultiCertOverview.class.getName() + "_symbols";
	private static final String MSG_TRUSTED = MultiCertOverview.class.getName() + "_trusted";
	private static final String MSG_NOT_TRUSTED = MultiCertOverview.class.getName() + "_notTrusted";
	private static final String MSG_VALID = MultiCertOverview.class.getName() + "_valid";
	private static final String MSG_INVALID = MultiCertOverview.class.getName() + "_invalid";
	private static final String MSG_ROOT_CERTS = MultiCertOverview.class.getName() + "_rootCerts";
	private static final String HINT_ROOT_CERTS = MultiCertOverview.class.getName() + "_hintRootCerts";
	private static final String MSG_OP_CERTS = MultiCertOverview.class.getName() + "_opCerts";
	private static final String HINT_OP = MultiCertOverview.class.getName() + "_hintOp";
	private static final String MSG_MIX_CERTS = MultiCertOverview.class.getName() + "_mixCerts";
	private static final String HINT_MIX = MultiCertOverview.class.getName() + "_hintMix";
	private static final String MSG_IS_CERTS = MultiCertOverview.class.getName() + "_isCerts";
	private static final String HINT_IS = MultiCertOverview.class.getName() + "_hintIS";
	private static final String HINT_ARROW = MultiCertOverview.class.getName() + "_hintArrow";
	private static final String HINT_CERT_DETAILS = MultiCertOverview.class.getName() + "_hintCertDetails";
	
	/** Images */
	private static final String IMG_PATH = "certs/";
	private static final String IMG_CERT_ORANGE_OK = IMG_PATH + "cert_orange_ok.png";
	private static final String IMG_CERT_ORANGE_NOK = IMG_PATH + "cert_orange_nok.png";
	private static final String IMG_CERT_ORANGE_INVALID = IMG_PATH + "cert_orange_invalid.png";
	private static final String IMG_CERT_ORANGE_OK_DARK = IMG_PATH + "cert_orange_ok_dark.png";
	private static final String IMG_CERT_ORANGE_NOK_DARK = IMG_PATH + "cert_orange_nok_dark.png";
	private static final String IMG_CERT_ORANGE_INVALID_DARK = IMG_PATH + "cert_orange_invalid_dark.png"; 
	private static final String IMG_CERT_PURPLE_OK = IMG_PATH + "cert_purple_ok.png";
	private static final String IMG_CERT_PURPLE_NOK = IMG_PATH + "cert_purple_nok.png";
	private static final String IMG_CERT_PURPLE_INVALID = IMG_PATH + "cert_purple_invalid.png";
	private static final String IMG_CERT_PURPLE_OK_DARK = IMG_PATH + "cert_purple_ok_dark.png";
	private static final String IMG_CERT_PURPLE_NOK_DARK = IMG_PATH + "cert_purple_nok_dark.png";
	private static final String IMG_CERT_PURPLE_INVALID_DARK = IMG_PATH + "cert_purple_invalid_dark.png";
	private static final String IMG_CERT_BLUE_OK = IMG_PATH + "cert_blue_ok.png";
	private static final String IMG_CERT_BLUE_NOK = IMG_PATH + "cert_blue_nok.png";
	private static final String IMG_CERT_BLUE_INVALID = IMG_PATH + "cert_blue_invalid.png";
	private static final String IMG_CERT_BLUE_OK_DARK = IMG_PATH + "cert_blue_ok_dark.png";
	private static final String IMG_CERT_BLUE_NOK_DARK = IMG_PATH + "cert_blue_nok_dark.png";
	private static final String IMG_CERT_BLUE_INVALID_DARK = IMG_PATH + "cert_orange_invalid_dark.png";
	private static final String IMG_ARROW_NORTH = IMG_PATH + "arrow_north_ok.png";
	private static final String IMG_ARROW_NORTH_NOK = IMG_PATH + "arrow_north_nok.png";
	private static final String IMG_ARROW_NORTH_EAST = IMG_PATH + "arrow_north_east_ok.png";
	private static final String IMG_ARROW_NORTH_EAST_NOK = IMG_PATH + "arrow_north_east_nok.png";
	private static final String IMG_ARROW_NORTH_WEST = IMG_PATH + "arrow_north_west_ok.png";
	private static final String IMG_ARROW_NORTH_WEST_NOK = IMG_PATH + "arrow_north_west_nok.png";
	private static final String IMG_NOT_TRUSTED = IMG_PATH + "not_trusted.png";
	private static final String IMG_INVALID = IMG_PATH + "invalid.png";
	private static final String IMG_BOX_ORANGE = IMG_PATH + "box_orange.png";
	private static final String IMG_BOX_PURPLE = IMG_PATH + "box_purple.png";
	private static final String IMG_BOX_BLUE = IMG_PATH + "box_blue.png";
	
	private MultiCertPath m_multiCertPath;
	private String m_name;
	private Hashtable m_buttonsAndNodes;
	private ButtonGroup m_certButtons;
	//private JButton m_closeButton;
	private CertPathInfo[] m_pathInfos;
	private MultiCertTrustGraph m_graph;

	public MultiCertOverview(Component a_parent, MultiCertPath a_multiCertPath, String a_name, boolean isInfoService)
	{
		super(a_parent, JAPMessages.getString(TITLE, a_name != null ? a_name : a_multiCertPath.getSubject().getCommonName()));
		
		m_multiCertPath = a_multiCertPath;
		m_pathInfos = m_multiCertPath.getPathInfos();
		m_graph = new MultiCertTrustGraph(m_pathInfos);
		if(m_multiCertPath.getSubject().getCommonName().startsWith("<Mix id=") && a_name != null)
		{
			m_name = a_name;
		}
		else
		{
			m_name = m_multiCertPath.getSubject().getCommonName();			
		}
		
		m_buttonsAndNodes = new Hashtable();
		m_certButtons = new ButtonGroup();
		
		JPanel rootPanel = new JPanel();
		rootPanel.setLayout(new GridBagLayout());
		rootPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
		GridBagConstraints c = new GridBagConstraints();
		
		c.weightx = 1.0;
		c.weighty = 2.0;
		c.fill = GridBagConstraints.BOTH;
		c.anchor = GridBagConstraints.CENTER;
		c.insets = new Insets(5, 5, 5, 5);
		rootPanel.add(drawOverviewPanel(isInfoService), c);
		
		
		JTabbedPane tabbedPane = new JTabbedPane();
		tabbedPane.add(JAPMessages.getString(SUMMARY), drawSummaryPanel(isInfoService));
		tabbedPane.add(JAPMessages.getString(EXPLANATION), drawExplanationPanel());
		c.weighty = 1.0;
		c.gridy = 2;
		rootPanel.add(tabbedPane, c);
		
		//Close Button
		/*m_closeButton = new JButton("Schlie§en");
		m_closeButton.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				dispose();
			}
		});*/
		
		this.getContentPane().add(rootPanel);
		this.setSize(550, 550);
		//this.setResizable(false);
		this.setVisible(true);
	}

	private JPanel drawSummaryPanel(boolean isInfoService)
	{
		JPanel summary = new JPanel(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		JAPMultilineLabel mlabel;
				
		//Info
		int number = m_multiCertPath.countPaths();
		int verified = m_multiCertPath.countVerifiedPaths();
		String info;
		if(number == 1)
		{
			info = JAPMessages.getString(MSG_NUMBER_OF_CERTS_ONE, m_name);
		}
		else
		{
			info = JAPMessages.getString(MSG_NUMBER_OF_CERTS, new Object[] {m_name, new Integer(number)}); 
		}
		if(verified == 1)
		{
			info += JAPMessages.getString(MSG_NUMBER_OF_TRUSTED_CERTS_ONE);
		}
		else
		{
			info += JAPMessages.getString(MSG_NUMBER_OF_TRUSTED_CERTS, new Integer(verified));
		}
		if(verified == 0)
		{
			mlabel = new JAPMultilineLabel(info, Color.RED);
		}
		else
		{
			mlabel = new JAPMultilineLabel(info);
		}
		c.gridx = 0;
		c.gridy = 0;
		c.insets = new Insets(5, 20, 5, 5);
		c.anchor = GridBagConstraints.NORTHWEST;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.weightx = 1.0;
		summary.add(mlabel, c);
		
		int count = m_graph.countTrustedRootNodes();
		String organisation;
		if(!isInfoService && m_multiCertPath.getIssuer().getOrganisation() != null)
		{
			organisation = m_multiCertPath.getIssuer().getOrganisation();
		}
		else if(isInfoService && m_multiCertPath.getSubject().getOrganisation() != null)
		{
			organisation = m_multiCertPath.getSubject().getOrganisation();
		}
		else
		{
			organisation = "";
		}
		if(count == 1)
		{
			info = JAPMessages.getString(MSG_IDENTITY_ONE, organisation);
		}
		else
		{
			info = JAPMessages.getString(MSG_IDENTITY, new Object[] {organisation, String.valueOf(count)});
		}
		if(count == 0)
		{
			mlabel = new JAPMultilineLabel(info, Color.RED);
		}
		else if (count == 1)
		{
			mlabel = new JAPMultilineLabel(info);
		}
		else
		{
			mlabel = new JAPMultilineLabel(info, Color.GREEN.darker().darker());
		}
		
		c.gridy = 1;
		c.anchor = GridBagConstraints.SOUTHWEST;
		summary.add(mlabel, c);
		
		return summary;
	}
	
	private JPanel drawExplanationPanel()
	{
		JPanel explanation =  new JPanel(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		JLabel label;
		
		label = new JLabel(JAPMessages.getString(MSG_SHOW_DETAILS));
		c.gridx = 0;
		c.gridy = 0;
		c.weightx = 1.0;
		c.weighty = 1.0;
		c.insets = new Insets(3, 3, 3, 3);
		c.fill = GridBagConstraints.BOTH;
		c.anchor = GridBagConstraints.NORTHWEST;
		c.gridheight = 2;
		c.gridwidth = 3;
		explanation.add(label, c);
		
		label = new JLabel(JAPMessages.getString(MSG_SYMBOLS));
		c.gridy = 2;
		c.gridheight = 1;
		c.fill = GridBagConstraints.NONE;
		explanation.add(label, c);
				
		label = new JLabel(GUIUtils.loadImageIcon(IMG_NOT_TRUSTED));
		label.setText(JAPMessages.getString(MSG_NOT_TRUSTED));
		c.insets.left = 7;
		c.gridy = 3;
		c.gridwidth = 1;
		explanation.add(label, c);
				
		label = new JLabel(GUIUtils.loadImageIcon(IMG_INVALID));
		label.setText(JAPMessages.getString(MSG_INVALID));
		c.gridy = 4;
		explanation.add(label, c);
		
		label = new JLabel(GUIUtils.loadImageIcon(IMG_BOX_PURPLE));
		label.setText("DSA");
		c.gridx = 2;
		c.gridy = 2;
		explanation.add(label, c);
		
		label = new JLabel(GUIUtils.loadImageIcon(IMG_BOX_ORANGE));
		label.setText("RSA");
		c.gridy = 3;
		explanation.add(label, c);
		
		label = new JLabel(GUIUtils.loadImageIcon(IMG_BOX_BLUE));
		label.setText("ECC");
		c.gridy = 4;
		explanation.add(label, c);
		
		return explanation;
	}

	private JPanel drawOverviewPanel(boolean isInfoService)
	{
		JPanel overview = new JPanel(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		CountryMapper country;
		JPanel panel;
		JLabel label, label2;
		
		Insets none = new Insets(0, 0, 0, 0);
		Insets norm = new Insets(10, 10, 10, 10);
		
		overview.setBorder(BorderFactory.createLoweredBevelBorder());
		overview.setBackground(Color.WHITE);
		
		//first column
		//root
		panel = new JPanel(new GridLayout(2, 1));
		panel.setBackground(Color.WHITE);
		label = new JLabel(JAPMessages.getString(MSG_ROOT_CERTS));
		label.setToolTipText(JAPMessages.getString(HINT_ROOT_CERTS));
		panel.add(label);
		c.gridx = 0;
		c.gridy = 0;
		c.anchor = GridBagConstraints.WEST;
		c.insets = norm;
		c.fill = GridBagConstraints.NONE;
		overview.add(panel, c);	
		
		/*for(int i=0; i<m_multiCertPath.getMaxLength()-3; i++)
		{
			mlabel = new JAPMultilineLabel("SubCA-\nZertifikate");
			mlabel.setBackground(Color.WHITE);
			mlabel.setToolTipText("Zertifikate zum Aufbau der Vertrauenskette.");
			c.gridy = c.gridy+2;
			overview.add(mlabel, c);
		}*/
		
		//operator or infoService
		panel = new JPanel(new GridLayout(2, 1));
		panel.setBackground(Color.WHITE);
		
		if(isInfoService)
		{
			label = new JLabel(JAPMessages.getString(MSG_IS_CERTS));
			country = new CountryMapper(m_multiCertPath.getSubject().getCountryCode(), JAPMessages.getLocale());
			label2 = new JLabel(m_name, GUIUtils.loadImageIcon("flags/" + country.getISOCode() + ".png"), SwingConstants.LEFT);
			label2.setToolTipText(JAPMessages.getString(HINT_IS, new Object[] {m_name, country.toString()}));
		}
		else
		{
			label = new JLabel(JAPMessages.getString(MSG_OP_CERTS));
			country = new CountryMapper(m_multiCertPath.getIssuer().getCountryCode(), JAPMessages.getLocale());
			label2 = new JLabel(m_multiCertPath.getIssuer().getOrganisation(), GUIUtils.loadImageIcon("flags/" + country.getISOCode() + ".png"), SwingConstants.LEFT);
			label2.setToolTipText(JAPMessages.getString(HINT_OP, new Object[] {m_multiCertPath.getIssuer().getOrganisation(), country.toString()}));
		}
		panel.add(label);
		panel.add(label2);
		c.gridy = c.gridy+2;
		overview.add(panel, c);
		
		//mix
		if(!isInfoService)
		{
			panel = new JPanel(new GridLayout(2, 1));
			panel.setBackground(Color.WHITE);
			label = new JLabel(JAPMessages.getString(MSG_MIX_CERTS));
			panel.add(label);
			country = new CountryMapper(m_multiCertPath.getSubject().getCountryCode(), JAPMessages.getLocale());
			label = new JLabel(m_name, GUIUtils.loadImageIcon("flags/" + country.getISOCode() + ".png"), SwingConstants.LEFT);
			label.setToolTipText(JAPMessages.getString(HINT_MIX, new Object[] {m_name, country.toString()}));
			panel.add(label);
			c.gridy = c.gridy+2;
			overview.add(panel, c);
		}
		
		//cert-buttons
		drawTrustGraph(overview);
		
		//separators		
		c.gridx = 1;
		c.gridy = 0;
		c.gridheight = 5;
		c.weighty = 1;
		c.insets = new Insets(5, 0, 5, 0);
		c.fill = GridBagConstraints.VERTICAL;
		overview.add(new JSeparator(SwingConstants.VERTICAL), c);
		
		c.gridx = 0;
		c.gridy = 1;
		c.gridheight = 1;
		c.gridwidth = 1;
		c.weighty = 0;
		c.weightx = 1;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.insets = none;
		overview.add(new JSeparator(SwingConstants.HORIZONTAL), c);
		
		for(int i=0; i<m_multiCertPath.getMaxLength()-3; i++)
		{
			c.gridy = c.gridy+2;
			overview.add(new JSeparator(SwingConstants.HORIZONTAL), c);
		}
		if(!isInfoService)
		{
			c.gridy = c.gridy+2;
			overview.add(new JSeparator(SwingConstants.HORIZONTAL), c);
		}
		
		return overview;
	}
	
	private void drawTrustGraph(JPanel parent)
	{
		Enumeration rootNodes;
		MultiCertTrustGraph.Node node;
		int x = 2;
		
		rootNodes = m_graph.getRootNodes();
		while(rootNodes.hasMoreElements())
		{
			node = (MultiCertTrustGraph.Node) rootNodes.nextElement();
			x += drawSubGraph(parent, node, x, 0);
		}
		rootNodes = m_graph.getOperatorNodes();
		while(rootNodes.hasMoreElements())
		{
			node = (MultiCertTrustGraph.Node) rootNodes.nextElement();
			x += drawSubGraph(parent, node, x, 2);
		}
		rootNodes = m_graph.getEndNodes();
		while(rootNodes.hasMoreElements())
		{
			node = (MultiCertTrustGraph.Node) rootNodes.nextElement();
			x += drawSubGraph(parent, node, x, 4);
		}		
	}
	
	private int drawSubGraph(JPanel parent, MultiCertTrustGraph.Node node, int x, int y)
	{
		Enumeration childs;
		MultiCertTrustGraph.Node childNode;
		int width = 0, count = 0;
		int middle;
		
		if(node.hasChildNodes())
		{
			childs = node.getChildNodes().elements();
			
			while(childs.hasMoreElements())
			{
				childNode = (MultiCertTrustGraph.Node) childs.nextElement();
				width += drawSubGraph(parent, childNode, x+(count++), y+2);
			}
			drawCertPanel(parent, x, y, width, node);
			for(int i=0; i<count; i++)
			{
				middle = Math.round((float)width/(float)(i+1));
				
				if(i+1 == middle)
				{
					drawArrow(parent, x+i, y+1, SwingConstants.NORTH, node.isTrusted());
				}
				else if(i+1 > middle)
				{
					drawArrow(parent, x+i, y+1, SwingConstants.NORTH_WEST, node.isTrusted());
				}
				else
				{
					drawArrow(parent, x+i, y+1, SwingConstants.NORTH_EAST, node.isTrusted());
				}
			}
			return width;
		}
		else
		{
			drawCertPanel(parent, x, y, 1, node);
			return 1;
		}
		
	}

	private void drawCertPanel(JPanel parent, int gridx, int gridy, int gridwidth, MultiCertTrustGraph.Node node)
	{
		JPanel certPanel = new JPanel(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		JRadioButton radioButton;
		JAPCertificate cert = node.getCertificate();
		
		//cert button
		if(cert == null)
		{
			return;
		}
		
		radioButton = new JRadioButton();
		if(cert.getPublicKey() instanceof MyRSAPublicKey)
		{
			if(node.isTrusted())
			{
				if(cert.getValidity().isValid(new Date()))
				{
					radioButton.setIcon(GUIUtils.loadImageIcon(IMG_CERT_ORANGE_OK));
					radioButton.setRolloverIcon(GUIUtils.loadImageIcon(IMG_CERT_ORANGE_OK_DARK));
					//radioButton.setSelectedIcon(GUIUtils.loadImageIcon(IMG_CERT_ORANGE_OK_DARK));
				}
				else
				{
					radioButton.setIcon(GUIUtils.loadImageIcon(IMG_CERT_ORANGE_INVALID));
					radioButton.setRolloverIcon(GUIUtils.loadImageIcon(IMG_CERT_ORANGE_INVALID_DARK));
					//radioButton.setSelectedIcon(GUIUtils.loadImageIcon(IMG_CERT_ORANGE_INVALID_DARK));
				}
			}
			else
			{
				radioButton.setIcon(GUIUtils.loadImageIcon(IMG_CERT_ORANGE_NOK));
				radioButton.setRolloverIcon(GUIUtils.loadImageIcon(IMG_CERT_ORANGE_NOK_DARK));
				//radioButton.setSelectedIcon(GUIUtils.loadImageIcon(IMG_CERT_ORANGE_NOK_DARK));
			}
			
		}
		else if(cert.getPublicKey() instanceof MyECPublicKey)
		{
			
			if(node.isTrusted())
			{
				if(cert.getValidity().isValid(new Date()))
				{
					radioButton.setIcon(GUIUtils.loadImageIcon(IMG_CERT_BLUE_OK));
					radioButton.setRolloverIcon(GUIUtils.loadImageIcon(IMG_CERT_BLUE_OK_DARK));
					//radioButton.setSelectedIcon(GUIUtils.loadImageIcon(IMG_CERT_BLUE_OK_DARK));
				}
				else
				{
					radioButton.setIcon(GUIUtils.loadImageIcon(IMG_CERT_BLUE_INVALID));
					radioButton.setRolloverIcon(GUIUtils.loadImageIcon(IMG_CERT_BLUE_INVALID_DARK));
					//radioButton.setSelectedIcon(GUIUtils.loadImageIcon(IMG_CERT_BLUE_INVALID_DARK));
				}
			}
			else
			{
				radioButton.setIcon(GUIUtils.loadImageIcon(IMG_CERT_BLUE_NOK));
				radioButton.setRolloverIcon(GUIUtils.loadImageIcon(IMG_CERT_BLUE_NOK_DARK));
				//radioButton.setSelectedIcon(GUIUtils.loadImageIcon(IMG_CERT_BLUE_NOK_DARK));
			}
		}
		else //certs with DSA or unknown keys
		{
			if(node.isTrusted())
			{
				if(cert.getValidity().isValid(new Date()))
				{
					radioButton.setIcon(GUIUtils.loadImageIcon(IMG_CERT_PURPLE_OK));
					radioButton.setRolloverIcon(GUIUtils.loadImageIcon(IMG_CERT_PURPLE_OK_DARK));
					//radioButton.setSelectedIcon(GUIUtils.loadImageIcon(IMG_CERT_PURPLE_OK_DARK));
				}
				else
				{
					radioButton.setIcon(GUIUtils.loadImageIcon(IMG_CERT_PURPLE_INVALID));
					radioButton.setRolloverIcon(GUIUtils.loadImageIcon(IMG_CERT_PURPLE_INVALID_DARK));
					//radioButton.setSelectedIcon(GUIUtils.loadImageIcon(IMG_CERT_PURPLE_INVALID_DARK));
				}
			}
			else
			{
				radioButton.setIcon(GUIUtils.loadImageIcon(IMG_CERT_PURPLE_NOK));
				radioButton.setRolloverIcon(GUIUtils.loadImageIcon(IMG_CERT_PURPLE_NOK_DARK));
				//radioButton.setSelectedIcon(GUIUtils.loadImageIcon(IMG_CERT_PURPLE_NOK_DARK));
			}
		}
		radioButton.setToolTipText(getToolTipText(cert));
		m_certButtons.add(radioButton);
		m_buttonsAndNodes.put(radioButton, node);
		radioButton.addMouseListener(this);
		
		c.anchor = GridBagConstraints.CENTER;
		c.gridx = 0;
		c.gridy = 0;
		c.gridwidth = 3;
		certPanel.add(radioButton, c);
				
		//add panel to parent
		certPanel.setBackground(Color.WHITE);
		c = new GridBagConstraints();
		c.anchor = GridBagConstraints.CENTER;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.weightx = 3;
		c.weighty = 1;
		c.gridx = gridx;
		c.gridy = gridy;
		c.gridwidth = gridwidth;
		c.insets = new Insets(5, 10, 5, 10);
		parent.add(certPanel, c);
	}
	
	private void drawArrow(JPanel parent, int gridx, int gridy, int orientation, boolean trusted)
	{
		JLabel label;
		GridBagConstraints c = new GridBagConstraints();
		
		if(orientation == SwingConstants.NORTH)
		{
			if(trusted)
			{
				label = new JLabel(GUIUtils.loadImageIcon(IMG_ARROW_NORTH));
			}
			else
			{
				label = new JLabel(GUIUtils.loadImageIcon(IMG_ARROW_NORTH_NOK));
			}
		}
		else if(orientation == SwingConstants.NORTH_WEST)
		{
			if(trusted)
			{
				label = new JLabel(GUIUtils.loadImageIcon(IMG_ARROW_NORTH_WEST));
			}
			else
			{
				label = new JLabel(GUIUtils.loadImageIcon(IMG_ARROW_NORTH_WEST_NOK));
			}
		}
		else if(orientation == SwingConstants.NORTH_EAST)
		{
			if(trusted)
			{
				label = new JLabel(GUIUtils.loadImageIcon(IMG_ARROW_NORTH_EAST));
			}
			else
			{
				label = new JLabel(GUIUtils.loadImageIcon(IMG_ARROW_NORTH_EAST_NOK));
			}
		}
		else
		{
			return;
		}
		label.setToolTipText(JAPMessages.getString(HINT_ARROW));
		
		c.fill = GridBagConstraints.NONE;
		c.gridx = gridx;
		c.gridy = gridy;
		c.insets = new Insets(0, 0, 0, 0);
		parent.add(label, c);
	}
	
	private String getToolTipText(JAPCertificate a_cert)
	{
		String[] details = new String[10];
		
		details[0] = a_cert.getSubject().getCommonName().replace("<", "&lt;").replace(">", "&gt;");
		details[1] = a_cert.getSubject().getOrganisation() != null ? a_cert.getSubject().getOrganisation() : "";
		details[2] = a_cert.getIssuer().getCommonName();
		details[3] = a_cert.getIssuer().getOrganisation() != null ? a_cert.getIssuer().getOrganisation() : ""; 
		details[4] = a_cert.getValidity().isValid(new Date())? JAPMessages.getString(MSG_VALID) : "<b>" + JAPMessages.getString(MSG_INVALID) + "</b>";
		details[5] = a_cert.getValidity().getValidFrom().toString();
		details[6] = a_cert.getValidity().getValidTo().toString();
		details[7] = a_cert.getPublicKey().getAlgorithm();
		details[8] = String.valueOf(a_cert.getPublicKey().getKeyLength());
		details[9] = a_cert.getSignatureAlgorithmName();						
		
		return JAPMessages.getString(HINT_CERT_DETAILS, details);
	}

	public void mouseClicked(MouseEvent e)
	{
		if(m_buttonsAndNodes.containsKey(e.getSource()))
		{
			MultiCertTrustGraph.Node node = (MultiCertTrustGraph.Node) m_buttonsAndNodes.get(e.getSource());
			
			if(e.getClickCount() == 1)
			{
				CertDetailsDialog dialog = new CertDetailsDialog(this.getParentComponent(), node.getCertificate(), node.isTrusted(), JAPMessages.getLocale());
				dialog.setVisible(true);
			}
		}
	}

	public void mouseEntered(MouseEvent e)
	{
		// TODO Auto-generated method stub
		
	}

	public void mouseExited(MouseEvent e)
	{
		// TODO Auto-generated method stub
		
	}

	public void mousePressed(MouseEvent e)
	{
		// TODO Auto-generated method stub
		
	}

	public void mouseReleased(MouseEvent e)
	{
		// TODO Auto-generated method stub
		
	}
}
