package jap;

import gui.JAPHelpContext;
import gui.JAPHtmlMultiLineLabel;
import gui.JAPMessages;
import gui.dialog.DialogContentPane;
import gui.dialog.IDialogOptions;
import gui.dialog.JAPDialog;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import anon.crypto.MyRandom;
import anon.infoservice.MixCascade;
import anon.util.XMLUtil;

public class JAPExtension extends AbstractJAPConfModule
{
	private static final String HELP_CONTEXT = "JAPExtension";
	private static final String MSG_DIALOG_STATUS = "dialog_status";
	private static final String MSG_DIALOG_STATUS_ON = "dialog_status_on";
	private static final String MSG_DIALOG_STATUS_OFF = "dialog_status_off";
	private static final String MSG_DIALOG_STATUS_PENDING = "dialog_status_pending";
	private static final String MSG_DIALOG_DENY_PREV = "dialog_denyPrev";
	private static final String MSG_DIALOG_DENY_PREV_SUCCESS = "dialog_denyPrevSuccess";
	private static final String MSG_DIALOG_OPTOUT = "dialog_optout";
	private static final String MSG_DIALOG_OPTOUT_SUCCESS = "dialog_optout_success";
	
	protected JAPExtension()
	{
		super(null);
	}

	private static final String DATESTRING_STUDY_PERIOD_END = "07/20/2008";

	private static final Date DATE_STUDY_PERIOD_END;
	static
	{
		Date d = null;
		try
		{
			d = new SimpleDateFormat("MM/dd/yyyy").parse(DATESTRING_STUDY_PERIOD_END);
		}
		catch (Exception e)
		{
		}

		DATE_STUDY_PERIOD_END = d;
	}

	static class MyOptions extends JAPDialog.Options
	{
		private int m_default_button;
		private int m_bttn_type;

		public MyOptions(int type, int default_button, int bttn_type)
		{
			super(type);
			m_default_button = default_button;
			m_bttn_type = bttn_type;
		}

		public String getYesOKText()
		{
			if (m_bttn_type == 0)
				return JAPMessages.getString("dialog_machenichtmit");
			else
				return JAPMessages.getString("dialog_lehneab");

		}

		public String getNoText()
		{
			if (m_bttn_type == 0)
				return JAPMessages.getString("dialog_machemit");
			else
				return JAPMessages.getString("dialog_akzeptiere");
		}

		public int getDefaultButton()
		{
			return m_default_button;
		}

		public boolean isDrawFocusEnabled()
		{
			return m_default_button != DialogContentPane.DEFAULT_BUTTON_KEEP;
		}
	}

	private static final BigInteger REFUSED = new BigInteger("-1");

	public static BigInteger doIt()
	{
		if (DATE_STUDY_PERIOD_END == null || new Date().after(DATE_STUDY_PERIOD_END))
		{
			if (DATE_STUDY_PERIOD_END == null)
			{
				LogHolder.log(LogLevel.WARNING, LogType.MISC, "End of study is null!");
			}
			JAPModel.getInstance().setDialogVersion(REFUSED);
			return REFUSED;
		}
		
		BigInteger dialog = JAPModel.getInstance().getDialogVersion();
		
		MixCascade cascade = JAPController.getInstance().getCurrentMixCascade();		
		if (dialog.compareTo(REFUSED) >= 0 || !cascade.isActiveStudy()) // || cascade.isPayment()) 
		{
			return dialog;
		}		
		
		/*
		 * Do not show the window after the first connction, but after some.
		 * Otherwise new users, or users that make the update, might get upset.
		 */ 
		dialog = dialog.add(new BigInteger("1"));
		JAPModel.getInstance().setDialogVersion(dialog);
		if (!dialog.equals(REFUSED))
		{
			return dialog;
		}
		
		MyRandom rand = new MyRandom();
		int text = rand.nextInt(2);
		int buttontext = rand.nextInt(2);
		int defbutton = rand.nextInt(3);
		try
		{
			long starttime = System.currentTimeMillis();
			int res = showDialog(text, buttontext, defbutton);
			long endtime = System.currentTimeMillis();
			int diffseconds = Math.min(63, (int) ((endtime - starttime) / 1000L));
			boolean bHelpClicked = ms_bHelpClicked;
			String strMsgLabel;
			if (res == JAPDialog.RETURN_VALUE_NO)
			{
				strMsgLabel = JAPMessages.getString(MSG_DIALOG_STATUS, JAPMessages.getString(MSG_DIALOG_STATUS_ON));
				m_btnDeny.setText(JAPMessages.getString(MSG_DIALOG_OPTOUT));
				dialog = new BigInteger(117, rand.getRandSource());
				dialog.shiftLeft(11);				
			}
			else
			{
				strMsgLabel = JAPMessages.getString(MSG_DIALOG_STATUS, JAPMessages.getString(MSG_DIALOG_STATUS_OFF));
				dialog = new BigInteger("0");
			}
			m_lblStatus.setText(strMsgLabel);
			
			dialog = dialog.or(new BigInteger(Integer.toString(text)));
			dialog = dialog.or(new BigInteger(Integer.toString(buttontext << 1)));
			dialog = dialog.or(new BigInteger(Integer.toString(defbutton << 2)));
			if (bHelpClicked)
				dialog = dialog.or(new BigInteger("16"));
			dialog = dialog.or(new BigInteger(Integer.toString(diffseconds << 5)));
			//System.out.println("d: " + d.toString() + " -- " + diffseconds);
			JAPModel.getInstance().setDialogVersion(dialog);
		
			
			
			JAPController.getInstance().saveConfigFile();
		}
		catch (Throwable e)
		{
			LogHolder.log(LogLevel.EXCEPTION, LogType.MISC, e);
		}
		return dialog;
	}

	private static boolean ms_bHelpClicked;

	private static final class MyLinkedHelpContext extends JAPDialog.AbstractLinkedURLAdapter
		implements JAPHelpContext.IHelpContext
	{
		private int m_bHelpClicked = 0;
		
		public URL getUrl()
		{
			try
			{
				return new URL("mailto:study@anon.inf.tu-dresden.de");
			}
			catch (Exception a_e)
			{
				return null;
			}
		}		
		
		public void clicked(boolean a_bState)
		{
			//ms_bURLClicked = true;
			LogHolder.log(LogLevel.NOTICE, LogType.MISC, "User clicked URL.");
			super.clicked(a_bState);
		}
		
		public String getHelpContext()
		{
			/*
			 * The first call of this method is just to initialise the window. The second
			 * call indicates that the user clicked the button.
			 */
			if (m_bHelpClicked > 0)
			{
				ms_bHelpClicked = true;
				LogHolder.log(LogLevel.NOTICE, LogType.MISC, "User clicked help button.");
			}
			m_bHelpClicked++;			
			return HELP_CONTEXT;
		}
		
		public boolean isCloseWindowActive()
		{
			return false;
		}
	}

	private static int showDialog(int text, int buttontext, int defbttn) throws IOException
	{
		InputStream in = JAPExtension.class.getResourceAsStream(JAPMessages
				.getString("dialog_message_1_html"));
		byte[] buff = new byte[20000];
		in.read(buff);
		in.close();
		in = JAPExtension.class.getResourceAsStream(JAPMessages.getString("dialog_message_2_html"));
		byte[] buff2 = new byte[20000];
		in.read(buff2);
		in.close();
		String[] msg = new String[2];
		msg[0] = new String(buff);
		msg[1] = new String(buff2);
		int[] defbutton = new int[3];
		defbutton[0] = DialogContentPane.DEFAULT_BUTTON_YES;
		defbutton[1] = DialogContentPane.DEFAULT_BUTTON_NO;
		defbutton[2] = DialogContentPane.DEFAULT_BUTTON_KEEP;
		int[] bttntype = new int[2];
		bttntype[0] = 0;
		bttntype[1] = 1;
		ms_bHelpClicked = false;
		return JAPDialog.showConfirmDialog(JAPController.getInstance().getViewWindow(), msg[text], "JAP/JonDo",
				new MyOptions(IDialogOptions.OPTION_TYPE_YES_NO, defbutton[defbttn],
						bttntype[buttontext]), JAPDialog.MESSAGE_TYPE_PLAIN, (Icon)null,
				new MyLinkedHelpContext());
	}

	public static void sendDialog(Document keyDoc, MixCascade a_cascade)
	{
		if (a_cascade.isActiveStudy())
		{
			try
			{
				Element elemDialog = keyDoc.createElement("Dialog");
				BigInteger d = JAPModel.getInstance().getDialogVersion();
				XMLUtil.setValue(elemDialog, d);
				keyDoc.getDocumentElement().appendChild(elemDialog);
			}
			catch (Exception e)
			{
			}
		}
	}

	
	public static void successfulSend(MixCascade a_cascade)
	{
		if (a_cascade.isActiveStudy())
		{
			BigInteger d = JAPModel.getInstance().getDialogVersion();
			if (d.compareTo(REFUSED) > 0 && d.compareTo(new BigInteger("2048")) < 0)
			{
				JAPModel.getInstance().setDialogVersion(REFUSED);
				JAPController.getInstance().saveConfigFile();
			}	
		}		
	}


	public String getTabTitle()
	{
		return JAPMessages.getString("dialog_tree_title");
	}

	private static JButton m_btnDeny;
	private static JLabel m_lblStatus;
	public void recreateRootPanel()
	{
		final JPanel panelRoot = getRootPanel();
		/* clear the whole root panel */
		panelRoot.removeAll();
		
		panelRoot.setLayout(new GridBagLayout());
		

		
		String strMsgButton;
		String strMsgLabel;		
		if (JAPModel.getInstance().getDialogVersion().compareTo(REFUSED) < 0)
		{
			strMsgButton = JAPMessages.getString(MSG_DIALOG_DENY_PREV);
			strMsgLabel = JAPMessages.getString(MSG_DIALOG_STATUS, JAPMessages.getString(MSG_DIALOG_STATUS_PENDING));
		}
		else
		{
			strMsgButton = JAPMessages.getString(MSG_DIALOG_OPTOUT);
			strMsgLabel = JAPMessages.getString(MSG_DIALOG_STATUS, JAPMessages.getString(MSG_DIALOG_STATUS_ON));
		}
				
		m_btnDeny = new JButton(strMsgButton);
		m_btnDeny.addActionListener(new ActionListener()
		{

			public void actionPerformed(ActionEvent arg0)
			{
				String strMessage;
				if (JAPModel.getInstance().getDialogVersion().compareTo(REFUSED) > 0)
				{
					strMessage = JAPMessages.getString(MSG_DIALOG_OPTOUT_SUCCESS);
				}
				else
				{
					strMessage = JAPMessages.getString(MSG_DIALOG_DENY_PREV_SUCCESS);
				}								
				
				JAPModel.getInstance().setDialogVersion(REFUSED);
				m_btnDeny.setEnabled(false);
				m_lblStatus.setText(JAPMessages.getString(MSG_DIALOG_STATUS, JAPMessages.getString(MSG_DIALOG_STATUS_OFF)));
				JAPDialog.showMessageDialog(panelRoot, strMessage);
			}
		});		
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 0;
		c.weightx = 0;
		c.weighty = 0;
		c.fill = GridBagConstraints.HORIZONTAL;
		panelRoot.add(m_btnDeny, c);
		
		m_lblStatus = new JAPHtmlMultiLineLabel(strMsgLabel);
		c.gridy++;
		c.insets = new Insets(10, 0, 0, 0);		
		panelRoot.add(m_lblStatus, c);
		
		c.gridy++;
		c.fill = GridBagConstraints.BOTH;
		c.weightx = 1.0;
		c.weighty = 1.0;
		panelRoot.add(new JLabel(), c);
	}

	public String getHelpContext()
	{
		return HELP_CONTEXT;
	}

	public static void addOptOut(JAPConfModuleSystem system)
	{
		if (JAPModel.getInstance().getDialogVersion().compareTo(REFUSED) == 0 || 
			DATE_STUDY_PERIOD_END == null || new Date().after(DATE_STUDY_PERIOD_END)) 
		{
			return;
		}
		system.addConfigurationModule(system.getConfigurationTreeRootNode(), new JAPExtension(),
				"JAPEXTENSION");
	};
}

/*
 
 JAPMessages_de.properties
 #dialog
 dialog_message_1_html=dialogmessage_1_de.html
 dialog_message_2_html=dialogmessage_2_de.html
 dialog_machenichtmit=Ich mache nicht mit
 dialog_lehneab=Ich lehne ab
 dialog_machemit=Ich mache mit
 dialog_akzeptiere=Ich akzeptiere
 dialog_tree_title=Untersuchung
 dialog_optout=Teilnahme an Untersuchung zur Verbesserung der Dienst-Sicherheit beenden
 dialog_optout_success=Ihre Teilnahme an der Untersuchung ist hiermit beendet. Vielen Dank f\u00fcr Ihre Bereitschaft, uns bei der Verbesserung der Sicherheit von AN.ON/JonDonym zu helfen!
 dialog_denyPrev=Teilnahme an Untersuchung zur Verbesserung der Dienst-Sicherheit verbieten
 dialog_denyPrevSuccess=Ihr Wunsch wurde registriert. Sie werden nicht an unserer Studie teilnehmen.
 dialog_status=Teilnahme an Studie ist <i>{0}</i>.
 dialog_status_pending=inaktiv
 dialog_status_on=akzeptiert
 dialog_status_off=abgelehnt

 ---
 EN
 ---
 dialog_message_1_html=dialogmessage_1_en.html
 dialog_message_2_html=dialogmessage_2_en.html
 dialog_machenichtmit=I do not take part
 dialog_lehneab=I decline
 dialog_machemit=I take part
 dialog_akzeptiere=I accept
 dialog_tree_title=Study
 dialog_optout=Withdraw your participation on the study for improving service security
 dialog_optout_success=You have successfully withdrawn your participation on our study. Thank you for your cooperation to improve the security of AN.ON/JonDonym!
 dialog_denyPrev=Deny participation on the study for improving service security
 dialog_denyPrevSuccess=Your request has been registered. You will not participate in our study.
 dialog_status=Participation in study is <i>{0}</i>.
 dialog_status_pending=inactive
 dialog_status_on=accepted
 dialog_status_off=denied
 *
 */