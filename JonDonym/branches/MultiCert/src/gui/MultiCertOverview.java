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
import javax.swing.SwingConstants;
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

	public MultiCertOverview(Component a_parent, MultiCertPath a_multiCertPath, String a_name)
	{
		super(a_parent, "Zertifikatsüberblick für " + a_name);
	
		this.setResizable(false);
		
		m_multiCertPath = a_multiCertPath;
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
		c.gridy = 2;
		rootPanel.add(drawDetailsPanel(), c);
		
		this.getContentPane().add(rootPanel);
		this.setSize(550, 500);
		this.setVisible(true);
	}
		
	private JPanel drawDetailsPanel()
	{
		JPanel details = new JPanel(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		TitledBorder title;
		JButton button;
		JAPMultilineLabel mlabel;
		
		//Border
		title = BorderFactory.createTitledBorder(
				BorderFactory.createEtchedBorder(EtchedBorder.LOWERED), "Details");
		title.setTitleJustification(TitledBorder.LEFT);
		details.setBorder(title);
		
		//Info
		int number = m_multiCertPath.countPaths();
		int verified = m_multiCertPath.countVerifiedPaths();
		mlabel = new JAPMultilineLabel(m_name + " verwendet " + number + " Zertifikat" + 
										(number > 1?"e":"") + ".\nDavon " + (verified > 1?"sind ":"ist ") + verified + " vertauenswürdig.");
		c.gridx = 0;
		c.gridy = 0;
		c.insets = new Insets(5, 10, 5, 5);
		c.anchor = GridBagConstraints.NORTHWEST;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.weightx = 1.0;
		details.add(mlabel, c);
		
		//Close Button
		button = new JButton("Schließen");
		button.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				dispose();
			}
		});
		c.gridx = 1;//a_multiCertPath.countPaths();
		c.gridy = 1;//a_multiCertPath.getMaxLength();
		c.anchor = GridBagConstraints.SOUTHEAST;
		c.fill = GridBagConstraints.NONE;
		details.add(button, c);
		
		return details;
	}

	private JPanel drawOverviewPanel()
	{
		JPanel overview = new JPanel(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		JAPMultilineLabel mlabel;
		JLabel label;
		JRadioButton radioButton;
		JPanel panel;
		
		Insets none = new Insets(0, 0, 0, 0);
		Insets norm = new Insets(10, 10, 10, 10);
		
		overview.setBorder(BorderFactory.createLoweredBevelBorder());
		overview.setBackground(Color.WHITE);
		
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
		
		//drawArrow(overview, 2, 1, SwingConstants.NORTH_EAST);
		//drawArrow(overview, 3, 1, SwingConstants.NORTH_WEST);
				
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
		CertPathInfo[] data = m_multiCertPath.getPathInfos();
		MultiCertTrustGraph graph = new MultiCertTrustGraph(data);
		Enumeration rootNodes;
		MultiCertTrustGraph.Node node;
		int x = 2;
		rootNodes = graph.getRootNodes();
		
		while(rootNodes.hasMoreElements())
		{
			node = (MultiCertTrustGraph.Node) rootNodes.nextElement();
			x += drawSubGraph(parent, node, x, 0);
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
		radioButton.setSelectedIcon(GUIUtils.loadImageIcon("certs/cert_"+color+"_selected.png"));
		radioButton.setRolloverIcon(GUIUtils.loadImageIcon("certs/cert_"+color+"_rollover.png"));
		radioButton.setToolTipText(getToolTipText(cert));
		m_certButtons.add(radioButton);
		m_buttonsAndCerts.put(radioButton, cert);
		radioButton.addMouseListener(this);
		
		c.anchor = GridBagConstraints.CENTER;
		c.gridx = 0;
		c.gridy = 0;
		c.gridwidth = 2;
		certPanel.add(radioButton, c);
		
		//verification icon
		if(trusted)
		{
			label = new JLabel(GUIUtils.loadImageIcon("cenabled.gif"));
			label.setToolTipText("Dieses Zertifikat ist vertrauenswürdig.");
		}
		else
		{
			label = new JLabel(GUIUtils.loadImageIcon("cdisabled.gif"));
			label.setToolTipText("Dieses Zertifikat ist nicht vertrauenswürdig.");
		}
		c.anchor = GridBagConstraints.EAST;
		c.gridy = 1;
		c.gridwidth = 1;
		certPanel.add(label, c);
		
		//validity icon
		if(cert.getValidity().isValid(new Date())) 
		{
			label = new JLabel(GUIUtils.loadImageIcon("cenabled.gif"));
			label.setToolTipText("Das Zertifikat ist noch gültig bis: "+cert.getValidity().getValidTo());
		}
		else
		{
			label = new JLabel(GUIUtils.loadImageIcon("warning.gif"));
			label.setToolTipText("Das Zertifikat ist abgelaufen seit: "+cert.getValidity().getValidTo());
		}
		c.anchor = GridBagConstraints.WEST;
		c.gridx = 1;
		certPanel.add(label,c);
		
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
		label.setToolTipText("Ein Pfeil von A nach B bedeutet: A ist von B zertifiziert.");
		
		
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
		ttt.append("Unterzeichnungs-Algorithmus: "+a_cert.getSignatureAlgorithmName());
		ttt.append("</html>");
		
		return ttt.toString();
	}

	public void mouseClicked(MouseEvent e)
	{
		if(e.getClickCount() > 1)
		{
			if(m_buttonsAndCerts.containsKey(e.getSource()))
			{
				JAPCertificate cert = (JAPCertificate) m_buttonsAndCerts.get(e.getSource());
				
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
}
