/*
 Copyright (c) 2000 - 2004, The JAP-Team
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
package jap.pay.wizardnew;

import java.lang.reflect.Field;

import java.net.MalformedURLException;
import java.net.URL;

import java.awt.Component;
import java.awt.Container;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

import anon.pay.xml.XMLPaymentOption;
import anon.pay.xml.XMLTransCert;
import anon.util.Util;
import gui.GUIUtils;
import gui.JAPMessages;
import gui.LinkMouseListener;
import gui.dialog.DialogContentPane;
import gui.dialog.DialogContentPane.IWizardSuitable;
import gui.dialog.JAPDialog;
import gui.dialog.WorkerContentPane;
import jap.JAPController;
import jap.JAPUtil;
import jap.JAPConstants;
import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;
import platform.AbstractOS;


public class PaymentInfoPane extends DialogContentPane implements IWizardSuitable,
	ActionListener
{
	/** Messages */
	private static final String MSG_INFOS = PaymentInfoPane.class.
		getName() + "_infos";
	private static final String MSG_BUTTONCOPY = PaymentInfoPane.class.
		getName() + "_buttoncopy";
	private static final String MSG_BUTTONOPEN = PaymentInfoPane.class.
		getName() + "_buttonopen";
	public static final String MSG_PAYPAL_ITEM_NAME = PaymentInfoPane.class.getName() + "_paypalitemname";
	private static final String MSG_COULD_OPEN = PaymentInfoPane.class.getName() + "_reminderLink";
	private static final String MSG_EXPLAIN_COULD_OPEN =
		PaymentInfoPane.class.getName() + "_reminderLinkExplain";


	private Container m_rootPanel;
	private GridBagConstraints m_c;
	private JButton m_bttnCopy, m_bttnOpen;
	private String m_language;
	private XMLPaymentOption m_selectedOption;
	private String m_strExtraInfo;
	private XMLTransCert transCert;
	private JCheckBox m_linkOpenedInBrowser;

	private String m_url;

	public PaymentInfoPane(JAPDialog a_parentDialog, DialogContentPane a_previousContentPane)
	{
		super(a_parentDialog, "Dummy",
			  new Layout(JAPMessages.getString(MSG_INFOS), MESSAGE_TYPE_PLAIN),
			  new Options(OPTION_TYPE_OK_CANCEL, a_previousContentPane));
		setDefaultButtonOperation(ON_CLICK_DISPOSE_DIALOG | ON_YESOK_SHOW_NEXT_CONTENT |
								  ON_NO_SHOW_PREVIOUS_CONTENT);

		m_language = JAPMessages.getLocale().getLanguage();
		m_rootPanel = this.getContentPane();
		m_rootPanel.setLayout(new GridBagLayout());
		m_c = new GridBagConstraints();
		m_c.gridx = 0;
		m_c.gridy = 0;
		m_c.weightx = 0;
		m_c.weightx = 0;
		m_c.insets = new Insets(5, 5, 5, 5);
		m_c.anchor = GridBagConstraints.NORTHWEST;
		m_c.fill = GridBagConstraints.NONE;

		m_linkOpenedInBrowser = new JCheckBox(JAPMessages.getString(MSG_COULD_OPEN));
		m_rootPanel.add(m_linkOpenedInBrowser, m_c);

		//Add some dummy labels for dialog sizing
		/*
		for (int i = 0; i < 12; i++)
		{
			m_rootPanel.add(new JLabel("..................................................."),
							m_c);
			m_c.gridy++;
		}*/

		//getButtonCancel().setVisible(false);
	}

	/**
	 * same as regular constructor, except it additionally takes a url
	 * which will be added as the last extraInfo of the selected paymentOption,
	 * and displayed prominently
	 *
	 * Use this for mixed payment options like paysafecard or call2pay,
	 * whenever you need to construct a URL to display that is dependent on the current transaction
	 * and therefor cannot be stored in the paymentoption itself
	 *
	 * @param a_parentDialog JAPDialog
	 * @param a_previousContentPane DialogContentPane
	 * @param a_url String
	 */
	public PaymentInfoPane(JAPDialog a_parentDialog, DialogContentPane a_previousContentPane, String a_url)
	{
		this(a_parentDialog,a_previousContentPane);
		m_url = a_url;
	}

	/**
     * takes the extrainfo as contained in the database's paymentoption configuration, and replaces the placeholders etc
	 * in a way suitable for the paysafecard syntax
	 */
	public static String createPaysafecardLink(String baseLink, long intAmount, String transferNumber)
	{
		baseLink = Util.replaceAll(baseLink, "%t", transferNumber);
		String pscAmount = amountAsString(intAmount);
		baseLink = Util.replaceAll(baseLink, "%a", pscAmount);
		return baseLink;
	}


	public static String createPaypalLink(String baseLink, long amount, String planName, String transferNumber)
	{
		String paypalCurrency = "EUR";


        String paypalAmount = amountAsString(amount);
		String localeLang = JAPMessages.getLocale().getLanguage();
		String paypalLang = localeLang.toUpperCase();
		String paypalItem = JAPMessages.getString(MSG_PAYPAL_ITEM_NAME) + "%20-%20" + planName; //URL-encode Spaces etc!!

		baseLink = Util.replaceAll(baseLink, "%t", transferNumber);
		baseLink = Util.replaceAll(baseLink, "%item%", paypalItem);
		baseLink = Util.replaceAll(baseLink, "%amount%", paypalAmount);
		baseLink = Util.replaceAll(baseLink, "%currency%", paypalCurrency);
		baseLink = Util.replaceAll(baseLink, "%lang%", paypalLang);

		return baseLink;
	}

	public static String createEgoldLink(String baseLink, long amount, String planName, String transferNumber)
	{
		String amountString = amountAsString(amount);
		String localeLang = JAPMessages.getLocale().getLanguage();
		String landingPageLang = localeLang.toLowerCase();
		//pages of jondos.de to which the user is sent after the e-gold payment only exist in German and English, so send to English pages if running Jap in another language
		if (!landingPageLang.equals("en") && !landingPageLang.equals("de") )
		{
			landingPageLang = "en";
		}
		String itemStrg = JAPMessages.getString(MSG_PAYPAL_ITEM_NAME) + "%20-%20" + planName; //URL-encode Spaces etc!!

		baseLink = Util.replaceAll(baseLink, "%t", transferNumber);
		baseLink = Util.replaceAll(baseLink, "%item%", itemStrg);
		baseLink = Util.replaceAll(baseLink, "%amount%", amountString);
		baseLink = Util.replaceAll(baseLink, "%currency%", "EUR");
		//JAP version 00.08.086 just uses createPaypalLink, i.e. replaces %lang% for all languages, not just the supported "en" and "de",
		//so we keep the hardcoded "en" in the link, and replace the url-encoded "/en/" part of the link
		baseLink = Util.replaceAll(baseLink, "%2fen%2f", "%2f" + landingPageLang + "%2f");

		return baseLink;
	}

	private static String amountAsString(long amount)
	{

		//amountString: eurocent, e.g. "500", transform it into a format suitable for paypal, e.g. "12.37"
		String amountString = new Long(amount).toString();
		String amountWhole;
		String amountFractions;
		amountString.trim();
		if (amountString.length() == 1)
		{
			amountWhole = "0";
			amountFractions = "0" + amountString;
		}
		else if (amountString.length() < 3)
		{
			amountWhole = "0";
			amountFractions = amountString;
		}
		else
		{
			amountWhole = amountString.substring(0, amountString.length() - 2);
			amountFractions = amountString.substring(amountString.length() - 2, amountString.length());
		}
		String result = amountWhole + "%2e" + amountFractions;
		return result;
	}

	public void showInfo()
	{
		DialogContentPane somePreviousPane = getPreviousContentPane();
		while (! (somePreviousPane instanceof MethodSelectionPane))
		{
			somePreviousPane = somePreviousPane.getPreviousContentPane();
		}
		MethodSelectionPane msp = (MethodSelectionPane) somePreviousPane;
		XMLPaymentOption selectedOption = msp.getSelectedPaymentOption();
	    transCert = (XMLTransCert) ( (WorkerContentPane) getPreviousContentPane()).getValue();
		String htmlExtraInfo = "";
		m_selectedOption = selectedOption;
		m_rootPanel.removeAll();
		m_rootPanel = this.getContentPane();
		m_rootPanel.setLayout(new GridBagLayout());
		m_c = new GridBagConstraints();
		m_c.gridx = 0;
		m_c.gridy = 0;
		m_c.weightx = 0;
		m_c.weightx = 0;
		m_c.insets = new Insets(5, 5, 5, 5);
		m_c.anchor = GridBagConstraints.NORTHWEST;
		m_c.fill = GridBagConstraints.NONE;


		m_strExtraInfo = selectedOption.getExtraInfo(m_language);
		boolean isURL = false;
		if (m_strExtraInfo != null)
		{

			somePreviousPane = getPreviousContentPane();
			while (! (somePreviousPane instanceof VolumePlanSelectionPane))
			{
				somePreviousPane = somePreviousPane.getPreviousContentPane();
				//warning: will loop endlessly if no VolumePlanSelectionPane to be found
			}
			VolumePlanSelectionPane planPane = (VolumePlanSelectionPane) somePreviousPane;
			String amountString = planPane.getAmount();
			String planName = planPane.getSelectedVolumePlan().getDisplayName();
			int intAmount = Integer.parseInt(amountString);
			String tan = String.valueOf(transCert.getTransferNumber());

			//take special care of links
			String imageLink = null; //url to open if logo is clicked, null if no image, or nothing should happen
			if (m_strExtraInfo.indexOf("paypal") != -1 )
			{
	            m_strExtraInfo = createPaypalLink(m_strExtraInfo,intAmount,planName,tan);
				imageLink = m_strExtraInfo;
            }
			else if (m_strExtraInfo.indexOf("gold") != -1)
			{
				m_strExtraInfo = createEgoldLink(m_strExtraInfo,intAmount,planName,tan);
				imageLink = m_strExtraInfo;
			}
			else if (m_strExtraInfo.indexOf("paysafecard") != -1)
			{
				m_strExtraInfo = createPaysafecardLink(m_strExtraInfo, intAmount, tan);
				imageLink = m_strExtraInfo;
			}
			else
			{
				//regualar extra infos, e.g. instructions for wire transfer
				m_strExtraInfo = Util.replaceAll(m_strExtraInfo, "%t",tan);
				String amount = JAPUtil.formatEuroCentValue(intAmount);
				m_strExtraInfo = Util.replaceAll(m_strExtraInfo, "%a",amount );
				m_strExtraInfo = Util.replaceAll(m_strExtraInfo, "%c","");
			}

			m_c.gridy++;
			m_rootPanel.add(new JLabel(" "), m_c);

			//add image if one is provided for the selected payment method
			String imageFilename = getMethodImageFilename(selectedOption.getName() );
			ImageIcon methodImage = GUIUtils.loadImageIcon(imageFilename, false,false);
			m_c.gridy++;
			if (methodImage != null)
			{
				JPanel imagePanel = new JPanel();
				imagePanel.setLayout(new BoxLayout(imagePanel,BoxLayout.X_AXIS));
			    JLabel imageLabel = new JLabel(methodImage);
				imageLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
				if (imageLink != null)
				{
					imageLabel.addMouseListener(new LinkMouseListener(imageLink));
				}
				imagePanel.add(imageLabel);
				m_c.gridwidth = 2;
				m_rootPanel.add(imagePanel, m_c);
				m_c.gridwidth = 1;
				m_c.gridy++;
			}

			//add buttons

			m_bttnOpen = new JButton(JAPMessages.getString(MSG_BUTTONOPEN));
			m_bttnOpen.addActionListener(this);
			m_rootPanel.add(m_bttnOpen, m_c);
			m_bttnOpen.setVisible(false);

			m_c.gridx++;
			m_bttnCopy = new JButton(JAPMessages.getString(MSG_BUTTONCOPY));
			m_bttnCopy.addActionListener(this);
			m_rootPanel.add(m_bttnCopy, m_c);
			m_bttnCopy.setVisible(false);


			isURL = selectedOption.getExtraInfoType(m_language).equalsIgnoreCase(XMLPaymentOption.EXTRA_LINK);
			if (isURL)
			{
				m_bttnOpen.setVisible(true);
				htmlExtraInfo = "<br> <font color=blue><u><b>" + m_strExtraInfo + "</b></u></font>";
			}
			else
			{
				m_bttnCopy.setVisible(true);
				htmlExtraInfo = "<p> <b>" + m_strExtraInfo + "</b> </p>";
			}
		}
		m_c.gridx = 0;
		m_c.gridy++;
		m_rootPanel.add(new JLabel(" "), m_c);

        if (isURL)
		{
			setText(selectedOption.getDetailedInfo(m_language));// + htmlExtraInfo); //links should never be shown, only confuse the user
			m_c.gridy++;
			m_c.anchor = GridBagConstraints.SOUTH;
			m_c.gridwidth = 2;
			m_rootPanel.add(m_linkOpenedInBrowser, m_c);

		}
		else
		{
			m_linkOpenedInBrowser.setSelected(true);
			setText(selectedOption.getDetailedInfo(m_language) + htmlExtraInfo);
		}
		if (isURL) setMouseListener(new LinkMouseListener());
	}

	public XMLTransCert getTransCert()
	{
		return transCert;
	}

	/**
	 *
	 * @param a_methodName String: String of the method name, as contained in db table paymentoptions and returned by <XMLPaymentOption>.getName()
	 * @return String: path to the image file as contained in JAPConstants in a String constant called IMAGE_<METHODNAME>, or null if not found, or on error
	 */
	public static String getMethodImageFilename(String a_methodName)
	{
		Class japConstantsClass = JAPConstants.class;
		String imageFieldName = "IMAGE_" + a_methodName.toUpperCase();
		Field pathVar = null;
		try
		{
			pathVar = japConstantsClass.getDeclaredField(imageFieldName);
		} catch (NoSuchFieldException nsfe)
		{
			LogHolder.log(LogLevel.DEBUG, LogType.PAY, "could not load image for payment method "+a_methodName+", there is not variable " + imageFieldName + " in JAPConstants");
			return null;
		}
		String path;
		try
		{
			path = (String) pathVar.get(null);
		} catch (Exception e)
		{
			LogHolder.log(LogLevel.DEBUG, LogType.PAY, "could not load image for payment method" + a_methodName + " , reason: " + e);
			return null;
		}
		return path;
	}

	/**
	 * Copies the extra payment info to the system clipboard
	 */
	private void copyExtraInfoToClipboard()
	{
		Clipboard sysClip = Toolkit.getDefaultToolkit().getSystemClipboard();
		String extraInfoString = m_strExtraInfo;
		if (m_selectedOption.getExtraInfoType(m_language).equalsIgnoreCase(XMLPaymentOption.EXTRA_TEXT))
		{
			//convert html to normal text
			extraInfoString = Util.replaceAll(extraInfoString, "<br>", "\n");
			extraInfoString = Util.replaceAll(extraInfoString, "<p>", "\n\n");
			extraInfoString = Util.replaceAll(extraInfoString,"&uuml;","\u00fc" );
			extraInfoString = Util.replaceAll(extraInfoString,"&Uuml;","\u00dc" );
			extraInfoString = Util.replaceAll(extraInfoString,"&auml;","\u00e4" );
			extraInfoString = Util.replaceAll(extraInfoString,"&Auml;","\u00c4" );
			extraInfoString = Util.replaceAll(extraInfoString,"&ouml;","\u00f6" );
			extraInfoString = Util.replaceAll(extraInfoString,"&Ouml;","\u00d6" );
			extraInfoString = Util.replaceAll(extraInfoString,"&szlig;","\u00df" );
			extraInfoString = Util.replaceAll(extraInfoString, "&nbsp;", " ");
		}
		else
		{
			//convert html to URL
			extraInfoString = Util.replaceAll(extraInfoString, "<br>", "");
			extraInfoString = Util.replaceAll(extraInfoString, "<p>", "");
			extraInfoString = Util.replaceAll(extraInfoString, "&nbsp;", "%20");
			extraInfoString = Util.replaceAll(extraInfoString, " ", "%20");
		}
		extraInfoString = Util.replaceAll(extraInfoString, "<html>", " ");
		extraInfoString = Util.replaceAll(extraInfoString, "</html>", " ");
		extraInfoString = Util.replaceAll(extraInfoString, "<font color=blue><u>", "");
		extraInfoString = Util.replaceAll(extraInfoString, "</u></font>", "");
		extraInfoString = extraInfoString.trim();

		Transferable transfer = new StringSelection(extraInfoString);
		sysClip.setContents(transfer, null);
	}

	public void openURL()
	{
		String link = m_strExtraInfo;

		if (!JAPController.getInstance().isAnonConnected() && JAPController.getInstance().getAnonMode())
		{
			/*
			 * JAP still tries to connect but fails... Switch of the anonymous connection so that
			 * the site may be opened in the browser window.
			 */
			JAPController.getInstance().stopAnonModeWait();
		}


		AbstractOS os = AbstractOS.getInstance();
		link = Util.replaceAll(link, "<br>", "");
		link = Util.replaceAll(link, "<p>", "");
		link = Util.replaceAll(link, "<html>", " ");
		link = Util.replaceAll(link, "</html>", " ");
		link = Util.replaceAll(link, "&nbsp;", "%20");
		link = Util.replaceAll(link, " ", "%20");
		link = Util.replaceAll(link, "<font color=blue><u>", "");
		link = Util.replaceAll(link, "</u></font>", "");
		link = link.trim();

		LogHolder.log(LogLevel.DEBUG, LogType.PAY, "Opening " + link + " in browser.");
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

	public void actionPerformed(ActionEvent e)
	{
		if (e.getSource() == m_bttnCopy)
		{
			copyExtraInfoToClipboard();
		}
		else if (e.getSource() == m_bttnOpen)
		{
			m_bttnCopy.setVisible(true);
			openURL();
		}
	}

	public CheckError[] checkUpdate()
	{
		m_linkOpenedInBrowser.setSelected(false);
		showInfo();
		return null;
	}

	public CheckError[] checkYesOK()
	{
		if (!m_linkOpenedInBrowser.isSelected())
		{
			return new CheckError[]{new CheckError(
		 JAPMessages.getString(MSG_EXPLAIN_COULD_OPEN), LogType.PAY)};
		}
		return null;
	}

}
