package jap.pay;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import anon.util.Util;
import gui.GUIUtils;
import gui.JAPHtmlMultiLineLabel;
import gui.JAPMessages;
import gui.LinkMouseListener;
import gui.dialog.JAPDialog;
import jap.JAPController;
import jap.JAPUtil;
import jap.pay.wizardnew.PaymentInfoPane;
import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;
import platform.AbstractOS;
import javax.swing.JLabel;
import javax.swing.ImageIcon;


/**
 * Shows details for active payments
 * invoked by clicking on "Details" for an active payment transaction in the transactions overview
 * Shows detailed info for ALL active payments
 * (maybe highlight the method that was originally selected for the TAN?)
 *
 * @author Elmar Schraml
 *
 */
public class ActivePaymentDetails extends JAPDialog implements ActionListener
{
	private static final String MSG_HEADING = ActivePaymentDetails.class.getName() + "_heading";
	private static final String MSG_TITLE = ActivePaymentDetails.class.getName() + "_title";
	private static final String MSG_CLOSEBUTTON = ActivePaymentDetails.class.getName() + "_closebutton";
	private static final String MSG_COPYBUTTON = ActivePaymentDetails.class.getName() + "_copybutton";
	private static final String MSG_PAYBUTTON = ActivePaymentDetails.class.getName() + "_paybutton";

	private GridBagConstraints m_c;
	private JButton m_closeButton;

	public ActivePaymentDetails(JAPDialog a_parent, Vector activeOptions, String a_transferNumber, long a_amount, String a_planName)
	{
		super(a_parent, JAPMessages.getString(MSG_TITLE));

		try
			{
				setDefaultCloseOperation(DISPOSE_ON_CLOSE);
				buildDialog(activeOptions, a_transferNumber, a_amount, a_planName);
				setResizable(false);
				pack();
				setVisible(true);
			}
			catch (Exception e)
			{
				LogHolder.log(LogLevel.EXCEPTION, LogType.PAY,
							  "Could not create ActivePaymentDetails: ", e);
		}

	}

	private void buildDialog(Vector optionsToShow, String transferNumber, long amount, String planName)
	{
		m_c = new GridBagConstraints();
		m_c.anchor = GridBagConstraints.NORTH;
		m_c.insets = new Insets(10, 30, 10, 30);
		m_c.gridx = 0;
		m_c.gridy = 0;
		m_c.weighty = 0;
		m_c.weightx = 0;
		getContentPane().setLayout(new GridBagLayout());

		JAPHtmlMultiLineLabel heading = new JAPHtmlMultiLineLabel("<h3>" + JAPMessages.getString(MSG_HEADING) + "</h3>");
		getContentPane().add(heading,m_c);
		m_c.gridy++;

		m_c.weightx = 0;
		Hashtable curOption;
		JPanel allOptions = new JPanel(); //this will hold all panels for a single option, and be put inside a scollpane
		allOptions.setLayout(new GridBagLayout() ); //we "recycle" the constraints of the main panel
		Vector optionPanels = new Vector(); //will store all the options, so we can set them to an equal size
		for (Enumeration options = optionsToShow.elements(); options.hasMoreElements(); )
		{
			curOption = (Hashtable) options.nextElement();
			m_c.gridy++;
			JPanel curOptionPanel = buildOptionPanel(curOption, transferNumber,amount, planName);
			optionPanels.addElement(curOptionPanel);
			allOptions.add(curOptionPanel,m_c);
		}

		//the various option panels' widths depends on the longest String on the panel,
		//so we make them all the same (widest) size
		Dimension largestPanel = GUIUtils.getMaxSize(optionPanels);
		GUIUtils.setEqualWidths(optionPanels,largestPanel);

    	//add allOptions panel to the main panel, inside a scrollpand
		JScrollPane scrollingOptions = new JScrollPane();
		scrollingOptions.setViewportView(allOptions);
		scrollingOptions.setBorder(BorderFactory.createEmptyBorder());
		//get maximum size for scrollpane
		int scrollPaneWidth = new Double(largestPanel.width).intValue() + 80; //takes border into account
		int idealHeight = GUIUtils.getTotalSize(optionPanels).height + 80;
		Window parentWindow = GUIUtils.getParentWindow(getContentPane());
		int screenHeight = (int) Math.round(GUIUtils.getCurrentScreen(parentWindow).getHeight() * 0.8) - 100;  //100 px to allow for chrome, 20% for the rest of the dialog
		int scrollPaneHeight = Math.min(idealHeight,screenHeight);
		scrollingOptions.setPreferredSize(new Dimension(scrollPaneWidth,scrollPaneHeight));
		getContentPane().add(scrollingOptions,m_c);
		scrollingOptions.revalidate();


		m_closeButton = new JButton(JAPMessages.getString(MSG_CLOSEBUTTON));
		m_closeButton.addActionListener(this);
		m_c.gridy++;
		getContentPane().add(m_closeButton,m_c);
	}

	private JPanel buildOptionPanel(Hashtable optionToShow, String transferNumber, long amount, String planName)
	{
		JPanel optionPanel = new JPanel();
		BoxLayout verticalBoxLayout = new BoxLayout(optionPanel,BoxLayout.Y_AXIS);
		optionPanel.setLayout(verticalBoxLayout);

		//the option's name is not used, since heading servers as the localized, user-visible name of the option
		String curHeading = (String) optionToShow.get("heading");
		JAPHtmlMultiLineLabel headingLabel = new JAPHtmlMultiLineLabel("<b>" + curHeading + "</b>");
		optionPanel.add(headingLabel);
		optionPanel.add(Box.createRigidArea(new Dimension(0,10)));


		String curDetailedInfo = (String) optionToShow.get("detailedInfo");
		JAPHtmlMultiLineLabel detailsLabel = new JAPHtmlMultiLineLabel(curDetailedInfo);
		detailsLabel.setPreferredWidth(600);
		detailsLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
		optionPanel.add(detailsLabel);
		optionPanel.add(Box.createRigidArea(new Dimension(0,10)));

		for (Enumeration extraInfos = ( (Vector) optionToShow.get("extraInfos")).elements(); extraInfos.hasMoreElements(); )
			{
				String extraInfoString =  (String) extraInfos.nextElement();
				boolean isALink = true;
				//check if it's a link or text
				try
				{
					new URL(extraInfoString);
					//url is never used, just to see if it works, if yes the String is a link
				} catch ( MalformedURLException e)
				{
					isALink = false;
				}

				JPanel linkButtonsPanel;
				if (isALink)
				{
					if (extraInfoString.toUpperCase().indexOf("PAYPAL") != -1 )
					{
						extraInfoString = PaymentInfoPane.createPaypalLink(extraInfoString,amount,planName,transferNumber);
					}
					else if (extraInfoString.toUpperCase().indexOf("E-GOLD") != -1)
					{
						extraInfoString = PaymentInfoPane.createEgoldLink(extraInfoString,amount,planName, transferNumber);
					}
					
					else if (extraInfoString.toUpperCase().indexOf("PAYSAFECARD") != -1 )
					{
						extraInfoString = PaymentInfoPane.createPaysafecardLink(extraInfoString,amount, transferNumber);
					}
					else
					{
						extraInfoString = PaymentInfoPane.createPaysafecardLink(extraInfoString,amount, transferNumber);
						//extraInfoString = Util.replaceAll(extraInfoString,"%t", transferNumber);
						//extraInfoString = Util.replaceAll(extraInfoString,"%a",(new Long(amount)).toString());
						//extraInfoString = Util.replaceAll(extraInfoString,"%c",""); //currency is not used, so get rid of the placeholder
					}
					//if a link, store it in final variable (for anonymous inner class ActionListeners), but don't show it
					final String linkToUse = extraInfoString;

					//add image
					String optionName = (String) optionToShow.get("name");
					String imageFilename = PaymentInfoPane.getMethodImageFilename(optionName);
					ImageIcon methodImage = null;
					if (imageFilename != null)
					{
						methodImage = GUIUtils.loadImageIcon(imageFilename, false, false);
					}
	                if (methodImage != null)
					{
						JPanel imagePanel = new JPanel();
						imagePanel.setLayout(new BoxLayout(imagePanel, BoxLayout.X_AXIS));
						JLabel imageLabel = new JLabel(methodImage);
						if (linkToUse != null)
						{
							imageLabel.addMouseListener(new LinkMouseListener(linkToUse));
						}
						imagePanel.add(imageLabel);
						imagePanel.setAlignmentX(Component.LEFT_ALIGNMENT);
						optionPanel.add(imagePanel);
						optionPanel.add(Box.createRigidArea(new Dimension(0, 5)));
					}


					//add buttons and handlers
					linkButtonsPanel = new JPanel(); //default flow layout

					JButton bttnPay = new JButton(JAPMessages.getString(MSG_PAYBUTTON));
					bttnPay.addActionListener(new ActionListener() {
						public void actionPerformed(ActionEvent e)
						{
							openURL(linkToUse);
						}
					});
					linkButtonsPanel.add(bttnPay);

					JButton bttnCopy = new JButton(JAPMessages.getString(MSG_COPYBUTTON));
					bttnCopy.addActionListener(new ActionListener(){
						public void actionPerformed(ActionEvent e)
						{
							copyToClipboard(linkToUse, true);
						}
					});
					linkButtonsPanel.add(bttnCopy);

					linkButtonsPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
					optionPanel.add(linkButtonsPanel);
				}
				else //regular text
				{					
					//test could contain e.g. wiring instructions, so need to replace placeholders, too
					extraInfoString = Util.replaceAll(extraInfoString,"%t", transferNumber);
					extraInfoString = Util.replaceAll(extraInfoString,"%a",JAPUtil.formatEuroCentValue(amount));
					extraInfoString = Util.replaceAll(extraInfoString,"%c",""); //currency is not used, so get rid of the placeholder
					
					
					//add text
					JAPHtmlMultiLineLabel extraInfoLabel = new JAPHtmlMultiLineLabel(extraInfoString);
					extraInfoLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
					optionPanel.add(extraInfoLabel);
					optionPanel.add(Box.createRigidArea(new Dimension(0,5)));


					//add "copy to clipboard" button

					final String finalExtraInfo = extraInfoString; //needs to be final to use it in the anonymous actionlistener
					linkButtonsPanel = new JPanel(); //default flow layout
					JButton bttnCopy = new JButton(JAPMessages.getString(MSG_COPYBUTTON));
					bttnCopy.addActionListener(new ActionListener(){
						public void actionPerformed(ActionEvent e)
						{
							copyToClipboard(finalExtraInfo, false);
						}
					});
					linkButtonsPanel.add(bttnCopy);
					linkButtonsPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
					optionPanel.add(linkButtonsPanel);
				}

				optionPanel.add(Box.createRigidArea(new Dimension(0,5)));
				optionPanel.setSize(optionPanel.getPreferredSize().width, optionPanel.getPreferredSize().height);
			}
		optionPanel.setBorder(BorderFactory.createRaisedBevelBorder());
		return optionPanel;
	}

	public void actionPerformed(ActionEvent e)
	{
		if (e.getSource() == m_closeButton)
		{
			this.setVisible(false);
		}
	}


	public void openURL(String link)
	{
		if (!JAPController.getInstance().isAnonConnected() && JAPController.getInstance().getAnonMode())
		{
			/*
			 * JAP still tries to connect but fails... Switch of the anonymous connection so that
			 * the site may be opened in the browser window.
			 */
			JAPController.getInstance().stopAnonModeWait();

		}

		AbstractOS os = AbstractOS.getInstance();
		link = cleanupLink(link);
		try
		{
			URL urlToOpen = new URL(link);
			os.openURL(urlToOpen);
		}
		catch (MalformedURLException me)
		{
			LogHolder.log(LogLevel.EXCEPTION, LogType.PAY, "Malformed URL");
		}

	}

	private void copyToClipboard(String a_info, boolean isLink)
	{
		Clipboard sysClip = Toolkit.getDefaultToolkit().getSystemClipboard();
		if (isLink)
		{
			a_info = cleanupLink(a_info);
		}
		else
		{
			a_info = cleanupText(a_info);
        }
		Transferable transfer = new StringSelection(a_info);
		sysClip.setContents(transfer, null);
	}

	private String cleanupLink(String link)
	{
		link = Util.replaceAll(link, "<br>", "");
		link = Util.replaceAll(link, "<p>", "");
		link = Util.replaceAll(link, "<html>", " ");
		link = Util.replaceAll(link, "</html>", " ");
		link = Util.replaceAll(link, "&nbsp;", "%20");
		link = Util.replaceAll(link, " ", "%20");
		link = Util.replaceAll(link, "<font color=blue><u>", "");
		link = Util.replaceAll(link, "</u></font>", "");
		link = link.trim();
		return link;
	}

	private String cleanupText(String a_text)
	{

		a_text = Util.replaceAll(a_text, "<br>", "\n");
		a_text = Util.replaceAll(a_text, "<p>", "\n\n");
		a_text = Util.replaceAll(a_text,"&uuml;","\u00fc" );
		a_text = Util.replaceAll(a_text,"&Uuml;","\u00dc" );
		a_text = Util.replaceAll(a_text,"&auml;","\u00e4" );
		a_text = Util.replaceAll(a_text,"&Auml;","\u00c4" );
		a_text = Util.replaceAll(a_text,"&ouml;","\u00f6" );
		a_text = Util.replaceAll(a_text,"&Ouml;","\u00d6" );
		a_text = Util.replaceAll(a_text,"&szlig;","\u00df" );
		a_text = Util.replaceAll(a_text, "&nbsp;", " ");
		return a_text;
    }

}
