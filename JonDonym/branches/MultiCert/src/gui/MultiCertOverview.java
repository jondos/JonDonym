package gui;

import gui.dialog.JAPDialog;

import java.awt.Color;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSeparator;
import javax.swing.JTabbedPane;
import javax.swing.SwingConstants;
import javax.swing.border.BevelBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;

import anon.crypto.CertPathInfo;
import anon.crypto.JAPCertificate;
import anon.crypto.MultiCertPath;
import anon.crypto.MyECPublicKey;
import anon.crypto.MyRSAPublicKey;

public class MultiCertOverview extends JAPDialog implements MouseListener
{
	private MultiCertPath m_multiCertPath;
	private String m_name;
	private Hashtable m_buttonsAndCerts;
	private ButtonGroup m_certButtons;
	private JButton m_closeButton;
	private CertInfoPanel m_certInfoPanel;
	private CertPathInfo[] m_pathInfos;
	private MultiCertTrustGraph m_graph;

	public MultiCertOverview(Component a_parent, MultiCertPath a_multiCertPath, String a_name)
	{
		super(a_parent, "Zertifikatsüberblick für " + a_name);
		
		m_multiCertPath = a_multiCertPath;
		m_pathInfos = m_multiCertPath.getPathInfos();
		m_graph = new MultiCertTrustGraph(m_pathInfos);
		m_name = a_name;
		if(m_name == null)
		{
			m_name = m_multiCertPath.getSubject().getCommonName();
		}
		
		m_buttonsAndCerts = new Hashtable();
		m_certButtons = new ButtonGroup();
		
		JPanel rootPanel = new JPanel();
		rootPanel.setLayout(new GridBagLayout());
		rootPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
		GridBagConstraints c = new GridBagConstraints();
		
		c.weightx = 1.0;
		c.weighty = 1.0;
		c.fill = GridBagConstraints.BOTH;
		c.anchor = GridBagConstraints.CENTER;
		c.insets = new Insets(5, 5, 5, 5);
		rootPanel.add(drawOverviewPanel(), c);
		
		
		JTabbedPane tabbedPane = new JTabbedPane();
		tabbedPane.add("Zusammenfassung", drawSummaryPanel());
		tabbedPane.add("Erklärung", drawExplanationPanel());
		c.gridy = 2;
		rootPanel.add(tabbedPane, c);
		
		//Close Button
		m_closeButton = new JButton("Schließen");
		m_closeButton.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				dispose();
			}
		});
		
		this.getContentPane().add(rootPanel);
		this.setSize(550, 550);
		//this.setResizable(false);
		this.setVisible(true);
	}

	private JPanel drawSummaryPanel()
	{
		JPanel summary = new JPanel(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		JAPMultilineLabel mlabel;
				
		//Info
		int number = m_multiCertPath.countPaths();
		int verified = m_multiCertPath.countVerifiedPaths();
		String info = m_name + " verwendet " + number + " Zertifikat" + (number > 1?"e":"") + ",\n" +
									"davon " + (verified != 1?"sind ":"ist ") + verified + " vertauenswürdig.";
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
		info = "Die Identität des Betreibers " + m_multiCertPath.getIssuer().getOrganisation() + 
		   "\nwird von " + count + " unabhängigen " + (count != 1?"Stellen":"Stelle") + " bestätigt.";
		if(count == 0)
		{
			mlabel = new JAPMultilineLabel(info, Color.RED);
		}
		else
		{
			mlabel = new JAPMultilineLabel(info, Color.GREEN.darker().darker());
		}
		
		c.gridy = 1;
		c.anchor = GridBagConstraints.SOUTHWEST;
		summary.add(mlabel, c);

		
		m_certInfoPanel = new CertInfoPanel();
		//c.gridy = 2;
		//details.add(m_certInfoPanel, c);
		
		return summary;
	}
	
	private JPanel drawExplanationPanel()
	{
		JPanel explanation =  new JPanel(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		JLabel label;
		
		label = new JLabel("Klicken Sie doppelt auf ein Zertifikat, um es anzuzeigen.");
		c.gridx = 0;
		c.gridy = 0;
		c.weightx = 1.0;
		c.weighty = 1.0;
		c.insets = new Insets(3, 3, 3, 3);
		c.fill = GridBagConstraints.BOTH;
		c.anchor = GridBagConstraints.NORTHWEST;
		c.gridheight = 2;
		c.gridwidth = 2;
		explanation.add(label, c);
		
		label = new JLabel("Symbole:");
		c.gridy = 2;
		c.gridheight = 1;
		c.fill = GridBagConstraints.NONE;
		explanation.add(label, c);
		
		label = new JLabel(GUIUtils.loadImageIcon("certs/trusted.png"));
		label.setText("vertrauenswürdig");
		c.insets.left = 7;
		c.gridy = 3;
		c.gridwidth = 1;
		explanation.add(label, c);
		
		label = new JLabel(GUIUtils.loadImageIcon("certs/not_trusted.png"));
		label.setText("nicht vertrauenswürdig");
		c.gridy = 4;
		explanation.add(label, c);
		
		label = new JLabel(GUIUtils.loadImageIcon("certs/valid2.png"));
		label.setText("gültig");
		c.insets.left = 3;
		c.gridy = 3;
		c.gridx = 1;
		explanation.add(label, c);
		
		label = new JLabel(GUIUtils.loadImageIcon("certs/invalid2.png"));
		label.setText("abgelaufen");
		c.gridy = 4;
		explanation.add(label, c);
		
		return explanation;
	}

	private JPanel drawOverviewPanel()
	{
		JPanel overview = new JPanel(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		JAPMultilineLabel mlabel;
		
		Insets none = new Insets(0, 0, 0, 0);
		Insets norm = new Insets(10, 10, 10, 10);
		
		overview.setBorder(BorderFactory.createLoweredBevelBorder());
		overview.setBackground(Color.WHITE);
		
		//first column
		mlabel = new JAPMultilineLabel("Root-\nZertifikate");
		mlabel.setBackground(Color.WHITE);
		mlabel.setToolTipText("Root-Zertifikate stellen den Vertrauensanker für Mix-Zertifikate dar.");
		c.gridx = 0;
		c.gridy = 0;
		c.anchor = GridBagConstraints.WEST;
		c.insets = norm;
		c.fill = GridBagConstraints.NONE;
		overview.add(mlabel, c);	
		
		for(int i=0; i<m_multiCertPath.getMaxLength()-3; i++)
		{
			mlabel = new JAPMultilineLabel("SubCA-\nZertifikate");
			mlabel.setBackground(Color.WHITE);
			mlabel.setToolTipText("Zertifikate zum Aufbau der Vertrauenskette.");
			c.gridy = c.gridy+2;
			overview.add(mlabel, c);
		}

		mlabel = new JAPMultilineLabel("Betreiber-\nZertifikate");
		mlabel.setBackground(Color.WHITE);
		mlabel.setToolTipText("Zertifikate des Betreibers " + m_multiCertPath.getIssuer().getOrganisation() + ".");
		c.gridy = c.gridy+2;
		overview.add(mlabel, c);
		
		mlabel = new JAPMultilineLabel("Mix-\nZertifikate");
		mlabel.setBackground(Color.WHITE);
		mlabel.setToolTipText("Zertifikate des Mixes " + m_name + ".");
		c.gridy = c.gridy+2;
		overview.add(mlabel, c);
		
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
		
		c.gridy = c.gridy+2;
		overview.add(new JSeparator(SwingConstants.HORIZONTAL), c);
		
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
			drawCertPanel(parent, x, y, width, node.getCertificate(), node.isTrusted());
			for(int i=0; i<count; i++)
			{
				middle = Math.round((float)width/(float)(i+1));
				
				if(i+1 == middle)
				{
					drawArrow(parent, x+i, y+1, SwingConstants.NORTH);
				}
				else if(i+1 > middle)
				{
					drawArrow(parent, x+i, y+1, SwingConstants.NORTH_WEST);
				}
				else
				{
					drawArrow(parent, x+i, y+1, SwingConstants.NORTH_EAST);
				}
			}
			return width;
		}
		else
		{
			drawCertPanel(parent, x, y, 1, node.getCertificate(), node.isTrusted());
			return 1;
		}
		
	}

	private void drawCertPanel(JPanel parent, int gridx, int gridy, int gridwidth, JAPCertificate cert, boolean trusted)
	{
		JPanel certPanel = new JPanel(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		JRadioButton radioButton;
		JLabel label;
		String color = "orange";
		
		//cert button
		if(cert == null)
		{
			return;
		}
		if(cert.getPublicKey() instanceof MyRSAPublicKey)
		{
			color = "green";
		}
		else if(cert.getPublicKey() instanceof MyECPublicKey)
		{
			color = "blue";
		}
		
		radioButton = new JRadioButton();
		radioButton.setIcon(GUIUtils.loadImageIcon("certs/cert_"+color+".png"));
		//radioButton.setSelectedIcon(GUIUtils.loadImageIcon("certs/cert_"+color+"_selected.png"));
		radioButton.setRolloverIcon(GUIUtils.loadImageIcon("certs/cert_"+color+"_rollover.png"));
		radioButton.setToolTipText(getToolTipText(cert));
		m_certButtons.add(radioButton);
		m_buttonsAndCerts.put(radioButton, cert);
		radioButton.addMouseListener(this);
		
		c.anchor = GridBagConstraints.CENTER;
		c.gridx = 0;
		c.gridy = 0;
		c.gridwidth = 3;
		certPanel.add(radioButton, c);
		
		//verification icon
		if(trusted)
		{
			label = new JLabel(GUIUtils.loadImageIcon("certs/trusted.png"));
			label.setToolTipText("Dieses Zertifikat ist vertrauenswürdig.");
		}
		else
		{
			label = new JLabel(GUIUtils.loadImageIcon("certs/not_trusted.png"));
			label.setToolTipText("Dieses Zertifikat ist nicht vertrauenswürdig.");
		}
		c.gridy = 1;
		c.gridwidth = 1;
		c.insets = new Insets(0, 6, 0, 0);
		certPanel.add(label, c);
		
		//validity icon
		if(cert.getValidity().isValid(new Date())) 
		{
			label = new JLabel(GUIUtils.loadImageIcon("certs/valid2.png"));
			label.setToolTipText("Das Zertifikat ist noch gültig bis: "+cert.getValidity().getValidTo());
		}
		else
		{
			label = new JLabel(GUIUtils.loadImageIcon("certs/invalid2.png"));
			label.setToolTipText("Das Zertifikat ist abgelaufen seit: "+cert.getValidity().getValidTo());
		}
		c.gridx = 1;
		certPanel.add(label,c);
		
		//flag icon
		CountryMapper country = new CountryMapper(cert.getSubject().getCountryCode(), JAPMessages.getLocale());
		label = new JLabel(GUIUtils.loadImageIcon("flags/" + country.getISOCode() + ".png"));
		label.setToolTipText("Der Inhaber des Zertifikats sitzt in "+country.toString() + ".");
		c.insets.left = 3;
		c.gridx = 2;
		certPanel.add(label, c);
		
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
	
	private void drawArrow(JPanel parent, int gridx, int gridy, int orientation)
	{
		JLabel label;
		GridBagConstraints c = new GridBagConstraints();
		
		if(orientation == SwingConstants.NORTH)
		{
			label = new JLabel(GUIUtils.loadImageIcon("certs/arrow_north.png"));
		}
		else if(orientation == SwingConstants.NORTH_WEST)
		{
			label = new JLabel(GUIUtils.loadImageIcon("certs/arrow_north_west.png"));
		}
		else if(orientation == SwingConstants.NORTH_EAST)
		{
			label = new JLabel(GUIUtils.loadImageIcon("certs/arrow_north_east.png"));
		}
		else
		{
			return;
		}
		label.setToolTipText("Ein Pfeil von A nach B bedeutet: A ist von B zertifiziert worden.");
		
		
		c.fill = GridBagConstraints.NONE;
		c.gridx = gridx;
		c.gridy = gridy;
		c.insets = new Insets(0, 0, 0, 0);
		parent.add(label, c);
	}
	
	private String getToolTipText(JAPCertificate a_cert)
	{
		StringBuffer ttt = new StringBuffer(); 
		String cn;
		String o;
		
		ttt.append("<html>Zertifikats-Inhaber:<br><blockquote>");
		cn = a_cert.getSubject().getCommonName().replace("<", "").replace("/>", "");
		ttt.append(cn+"<br>");
		o = a_cert.getSubject().getOrganisation();
		ttt.append(o != null ? "Organisation: "+o+"<br>" : "");
		ttt.append("</blockquote>"); 
		ttt.append("Herausgegeben durch:<br><blockquote>");
		ttt.append(a_cert.getIssuer().getCommonName()+"<br>");
		o = a_cert.getIssuer().getOrganisation();
		ttt.append(o != null ? "Organisation: "+o+"<br>" : "");
		ttt.append("</blockquote><hr>");
		ttt.append("Gültigkeit<br><blockquote>");
		ttt.append("von: "+a_cert.getValidity().getValidFrom()+"<br>");
		ttt.append("bis: "+a_cert.getValidity().getValidFrom()+"<br></blockquote><hr>"); 
		ttt.append("Schlüssel-Algorithmus: "+a_cert.getPublicKey().getAlgorithm()+"<br>");
		ttt.append("Schlüssel-Stärke: "+a_cert.getPublicKey().getKeyLength()+"<br>");
		ttt.append("Unterzeichnungs-Algorithmus: "+a_cert.getSignatureAlgorithmName());
		ttt.append("</html>");
		
		return ttt.toString();
	}

	public void mouseClicked(MouseEvent e)
	{
		if(m_buttonsAndCerts.containsKey(e.getSource()))
		{
			JAPCertificate cert = (JAPCertificate) m_buttonsAndCerts.get(e.getSource());
			
			if(e.getClickCount() == 1)
			{
				m_certInfoPanel.setFirstLine("Das gewählte Zertifikat ist vertrauenswürdig.", null);
				m_certInfoPanel.setSecondLine("Es ist außerdem gültig.", null);
			}
			
			else if(e.getClickCount() > 1)
			{
				CertDetailsDialog dialog = new CertDetailsDialog(this.getParentComponent(), cert, true, JAPMessages.getLocale());
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
	
	private final class CertInfoPanel extends JPanel
	{
		private JLabel m_firstLine;
		private JLabel m_secondLine;
		
		public CertInfoPanel()
		{
			setLayout(new GridBagLayout());
			GridBagConstraints c = new GridBagConstraints();
			setBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED));
			
			m_firstLine = new JLabel("Bitte wählen Sie ein Zertifikat aus.");
			c.fill = GridBagConstraints.BOTH;
			c.weightx = 1.0;
			c.weighty = 1.0;
			c.anchor = GridBagConstraints.WEST;
			c.gridy = 0;
			add(m_firstLine, c);
			
			m_secondLine = new JLabel("Klicken Sie doppelt um das Zertifikat anzuzeigen");
			c.gridy = 1;
			add(m_secondLine, c);
		}
		
		public void setFirstLine(String text, Color textColor)
		{
			m_firstLine.setText(text);
			if(textColor != null)
			{
				m_firstLine.setForeground(textColor);
			}
		}
		
		public void setSecondLine(String text, Color textColor)
		{
			m_secondLine.setText(text);
			if(textColor != null)
			{
				m_secondLine.setForeground(textColor);
			}
		}
		
	}
}
