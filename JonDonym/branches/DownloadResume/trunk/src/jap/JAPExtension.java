package jap;

import gui.JAPHelpContext;
import gui.JAPHtmlMultiLineLabel;
import gui.JAPMessages;
import gui.dialog.DialogContentPane;
import gui.dialog.IDialogOptions;
import gui.dialog.JAPDialog;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Container;
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
import anon.util.ResourceLoader;
import anon.util.XMLUtil;

public class JAPExtension extends AbstractJAPConfModule
{
	private static final String HELP_CONTEXT = "studie";
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
	private static final BigInteger REFUSED = new BigInteger("-2"); //means everything is over and done (e.g period over, opt-out etc.)
	private static final BigInteger NOT_DECIDED = new BigInteger("-1"); //nothing is known (e.g first time etc.)

	public static BigInteger doIt()
	{
		if (DATE_STUDY_PERIOD_END == null || new Date().after(DATE_STUDY_PERIOD_END))
		{
			if (DATE_STUDY_PERIOD_END == null)
			{
				LogHolder.log(LogLevel.WARNING, LogType.MISC, "End of study is null!");
			}
			JAPModel.getInstance().setDialogVersion(REFUSED);
			JAPController.getInstance().saveConfigFile();
			return REFUSED;
		}

		BigInteger dialog = JAPModel.getInstance().getDialogVersion();

		MixCascade cascade = JAPController.getInstance().getCurrentMixCascade();		
		if (!dialog.equals(NOT_DECIDED)) // d !=-1 --> has already seen the opt-in dialog...
		{
			return dialog;
		}
				
		//d==-1 --> has not seen the opt-in dialog yet...
		if ((cascade != null && !cascade.isActiveStudy()) || JAPController.getInstance().isNewInstallation()) //do not show dialog if new installation...
		{
			return NOT_DECIDED;
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
				m_btnDeny.setEnabled(true);
				dialog = new BigInteger(117, rand.getRandSource());
				dialog=dialog.shiftLeft(11);				
			}
			else
			{
				strMsgLabel = JAPMessages.getString(MSG_DIALOG_STATUS, JAPMessages.getString(MSG_DIALOG_STATUS_OFF));
				m_btnDeny.setEnabled(false);
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
	//					String bs=d.toString(2);
	//					System.out.println("d: " + bs.substring(bs.length()-Math.min(11,bs.length())) + " -- " + diffseconds);
			JAPModel.getInstance().setDialogVersion(dialog);
			JAPController.getInstance().saveConfigFile();
		}
		catch (Throwable e)
		{
			LogHolder.log(LogLevel.EXCEPTION, LogType.MISC, e);
		}
		return NOT_DECIDED;
	}

	private static boolean ms_bHelpClicked = false;

	private static final class MyLinkedHelpContext extends JAPDialog.AbstractLinkedURLAdapter
		implements JAPHelpContext.IHelpContext
	{
		private int m_bHelpClicked = 0;

		public URL getUrl()
		{
			try
			{
				return null;
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
		
		public Container getHelpExtractionDisplayContext() 
		{
			return JAPConf.getInstance().getContentPane();
		}
		
		public boolean isCloseWindowActive()
		{
			return false;
		}
	}

	private static int showDialog(int text, int buttontext, int defbttn) throws IOException
	{
		InputStream in=ResourceLoader.loadResourceAsStream("jap/"+JAPMessages.getString("dialog_message_1_html"));
		byte[] buff = new byte[20000];
		in.read(buff);
		in.close();
		in = ResourceLoader.loadResourceAsStream("jap/"+JAPMessages.getString("dialog_message_2_html"));
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
		ms_bHelpClicked = false;
		return JAPDialog.showConfirmDialog(JAPController.getInstance().getViewWindow(),
				msg[text], "JAP/JonDo", new MyOptions(IDialogOptions.OPTION_TYPE_YES_NO,
						defbutton[defbttn], buttontext),
				JAPDialog.MESSAGE_TYPE_PLAIN, (Icon)null,
				new MyLinkedHelpContext());
	}

	public static void sendDialog(Document keyDoc, MixCascade a_cascade)
	{
		try
		{
			if (a_cascade.isActiveStudy())
			{
				Element elemDialog = keyDoc.createElement("Dialog");
				BigInteger d = JAPModel.getInstance().getDialogVersion();
				XMLUtil.setValue(elemDialog, d);
				keyDoc.getDocumentElement().appendChild(elemDialog);
			}
		}
		catch (Exception e)
		{
		}
	}

	public static void successfulSend(MixCascade a_cascade)
	{
		try
		{
			if (a_cascade.isActiveStudy())
			{
				BigInteger d = JAPModel.getInstance().getDialogVersion();
				if (d.compareTo(new BigInteger("2048")) < 0&&!d.equals(NOT_DECIDED)&&!d.equals(REFUSED))
				{
					JAPModel.getInstance().setDialogVersion(REFUSED);
					JAPController.getInstance().saveConfigFile();
				}
			}
		}
		catch (Throwable t)
		{

		}
	}

	public String getTabTitle()
	{
		return JAPMessages.getString("dialog_tree_title");
	}

	private static JButton m_btnDeny=new JButton();
	private static JLabel m_lblStatus=new JLabel();

	public void recreateRootPanel()
	{
		final JPanel panelRoot = getRootPanel();
		/* clear the whole root panel */
		panelRoot.removeAll();

		panelRoot.setLayout(new GridBagLayout());

		String strMsgButton;
		String strMsgLabel;
		strMsgButton = JAPMessages.getString(MSG_DIALOG_OPTOUT);
		m_btnDeny = new JButton(strMsgButton);
		if (JAPModel.getInstance().getDialogVersion().compareTo(NOT_DECIDED) == 0)
		{
			strMsgLabel = JAPMessages.getString(MSG_DIALOG_STATUS, JAPMessages
					.getString(MSG_DIALOG_STATUS_PENDING));
			m_btnDeny.setEnabled(false);
		}
		else if (JAPModel.getInstance().getDialogVersion().compareTo(REFUSED) == 0)
			{
				strMsgLabel = JAPMessages.getString(MSG_DIALOG_STATUS, JAPMessages
						.getString(MSG_DIALOG_STATUS_OFF));
				m_btnDeny.setEnabled(false);
			}
		else
		{
			strMsgLabel = JAPMessages.getString(MSG_DIALOG_STATUS, JAPMessages
					.getString(MSG_DIALOG_STATUS_ON));
		}

		m_btnDeny.addActionListener(new ActionListener()
		{

			public void actionPerformed(ActionEvent arg0)
			{
				String strMessage = JAPMessages.getString(MSG_DIALOG_OPTOUT_SUCCESS);

				JAPModel.getInstance().setDialogVersion(REFUSED);
				m_btnDeny.setEnabled(false);
				m_lblStatus.setText(JAPMessages.getString(MSG_DIALOG_STATUS, JAPMessages
						.getString(MSG_DIALOG_STATUS_OFF)));
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
		try{
		if (JAPModel.getInstance().getDialogVersion().compareTo(REFUSED) == 0
				|| DATE_STUDY_PERIOD_END == null || new Date().after(DATE_STUDY_PERIOD_END))
		{
			return;
		}
		system.addConfigurationModule(system.getConfigurationTreeRootNode(),
				new JAPExtension(), "JAPEXTENSION");
		}
		catch(Throwable t)
			{
				
			}
	};

public static void loadDialogFromConfig(Element root)
	{
		try{
			BigInteger d=REFUSED;
			if(DATE_STUDY_PERIOD_END != null && !new Date().after(DATE_STUDY_PERIOD_END))
						{
							Element elemDialog = (Element) XMLUtil.getFirstChildByName(root,"Dialog");
							d=XMLUtil.parseValue(elemDialog, JAPModel.getInstance().getDialogVersion());
							if(d.compareTo(REFUSED)<0)
								d=REFUSED;
						}
			JAPModel.getInstance().setDialogVersion(d);
		}
		catch(Throwable t)
			{
				
			}
	}
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