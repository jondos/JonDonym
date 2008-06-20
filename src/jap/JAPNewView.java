/*
 Copyright (c) 2000 - 2006, The JAP-Team
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
package jap;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Observable;
import java.util.Observer;
import java.util.Vector;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.ButtonGroup;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSeparator;
import javax.swing.LookAndFeel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.JFileChooser;

import anon.AnonServerDescription;
import anon.infoservice.BlacklistedCascadeIDEntry;
import anon.infoservice.CascadeIDEntry;
import anon.infoservice.ClickedMessageIDDBEntry;
import anon.infoservice.Database;
import anon.infoservice.DatabaseMessage;
import anon.infoservice.DeletedMessageIDDBEntry;
import anon.infoservice.JAPVersionInfo;
import anon.infoservice.JavaVersionDBEntry;
import anon.infoservice.MessageDBEntry;
import anon.infoservice.MixCascade;
import anon.infoservice.MixInfo;
import anon.infoservice.NewCascadeIDEntry;
import anon.infoservice.ServiceOperator;
import anon.infoservice.StatusInfo;
import anon.pay.IMessageListener;
import anon.pay.PayAccountsFile;
import anon.pay.PayMessage;
import anon.proxy.IProxyListener;
import anon.util.JobQueue;
import anon.util.Util;
import gui.CountryMapper;
import gui.FlippingPanel;
import gui.GUIUtils;
import gui.JAPDll;
import gui.JAPHelp;
import gui.JAPMessages;
import gui.JAPProgressBar;
import gui.MixDetailsDialog;
import gui.OperatorDetailsDialog;
import gui.PopupMenu;
import gui.dialog.DialogContentPane;
import gui.dialog.JAPDialog;
import gui.dialog.JAPDialog.LinkedInformationAdapter;
import gui.dialog.JAPDialog.Options;
import jap.forward.JAPRoutingMessage;
import jap.forward.JAPRoutingRegistrationStatusObserver;
import jap.forward.JAPRoutingServerStatisticsListener;
import jap.forward.JAPRoutingSettings;
import jap.pay.PaymentMainPanel;
import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;
import platform.AbstractOS;
import update.JAPUpdateWizard;
import javax.swing.BorderFactory;

final public class JAPNewView extends AbstractJAPMainView implements IJAPMainView, ActionListener,
	JAPObserver, Observer, IMessageListener
{
	public static final String MSG_UPDATE = JAPNewView.class.getName() + "_update";
	public static final String MSG_NO_REAL_PAYMENT = JAPNewView.class.getName() + "_noRealPayment";

	private static final String MSG_ANONYMETER_TOOL_TIP = JAPNewView.class.getName() + "_anonymeterToolTip";
	private static final String MSG_SERVICE_NAME = JAPNewView.class.getName() + "_ngAnonymisierungsdienst";
	private static final String MSG_ERROR_DISCONNECTED = JAPNewView.class.getName() + "_errorDisconnected";
	private static final String MSG_ERROR_PROXY = JAPNewView.class.getName() + "_errorProxy";
	private static final String MSG_TITLE_OLD_JAVA = JAPNewView.class.getName() + "_titleOldJava";
	private static final String MSG_OLD_JAVA = JAPNewView.class.getName() + "_oldJava";
	private static final String MSG_OLD_JAVA_HINT = JAPNewView.class.getName() + "_oldJavaHint";
	private static final String MSG_LBL_NEW_SERVICES_FOUND = JAPNewView.class.getName() + "_newServicesFound";
	private static final String MSG_TOOLTIP_NEW_SERVICES_FOUND = JAPNewView.class.getName() +
		"_tooltipNewServicesFound";
	private static final String MSG_NEW_SERVICES_FOUND_EXPLAIN =
		JAPNewView.class.getName() + "_newServicesFoundExplanation";
	private static final String MSG_NO_COSTS = JAPNewView.class.getName() + "_noCosts";
	private static final String MSG_WITH_COSTS = JAPNewView.class.getName() + "_withCosts";
	private static final String MSG_BTN_ASSISTANT = JAPNewView.class.getName() + "_btnAssistant";
	private static final String MSG_MN_ASSISTANT = JAPNewView.class.getName() + "_mnAssistant";
	private static final String MSG_MN_DETAILS = JAPNewView.class.getName() + "_mnDetails";
	private static final String MSG_IS_DISABLED_EXPLAIN = JAPNewView.class.getName() + "_isDisabledExplain";
	private static final String MSG_IS_DEACTIVATED = JAPNewView.class.getName() + "_isDisabled";
	private static final String MSG_IS_TOOLTIP = JAPNewView.class.getName() + "_isDisabledTooltip";

	private static final String MSG_IS_TRUST_PARANOID = JAPNewView.class.getName() + "_trustParanoid";
	private static final String MSG_IS_TRUST_SUSPICIOUS = JAPNewView.class.getName() + "_trustSuspicious";
	private static final String MSG_IS_TRUST_HIGH = JAPNewView.class.getName() + "_trustHigh";
	private static final String MSG_IS_TRUST_ALL = JAPNewView.class.getName() + "_trustAll";
	private static final String MSG_IS_EDIT_TRUST = JAPNewView.class.getName() + "_editTrust";

	private static final String MSG_TRUST_FILTER = JAPNewView.class.getName() + "_trustFilter";
	private static final String MSG_CONNECTED = JAPNewView.class.getName() + "_connected";

	private static final String MSG_DELETE_MESSAGE = JAPNewView.class.getName() + "_deleteMessage";
	private static final String MSG_HIDE_MESSAGE_SHORT = JAPNewView.class.getName() + "_hideMessageShort";
	private static final String MSG_DELETE_MESSAGE_EXPLAIN = JAPNewView.class.getName() + "_deleteMessageExplain";
	private static final String MSG_DELETE_MESSAGE_SHORT = JAPNewView.class.getName() + "_deleteMessageShort";
	private static final String MSG_VIEW_MESSAGE = JAPNewView.class.getName() + "_viewMessage";
	private static final String MSG_ANTI_CENSORSHIP = JAPNewView.class.getName() + "_antiCensorship";

	private static final String MSG_OBSERVABLE_EXPLAIN = JAPNewView.class.getName() + "_observableExplain";
	private static final String MSG_OBSERVABLE_TITLE = JAPNewView.class.getName() + "_observableTitle";
	private static final String MSG_EXPLAIN_NO_FIREFOX_FOUND = JAPNewView.class.getName() + "_explainNoFirefoxFound";

	private static final String MSG_LBL_ENCRYPTED_DATA =
		JAPNewView.class.getName() + "_lblEncryptedData";
	private static final String MSG_LBL_HTTP_DATA =
		JAPNewView.class.getName() + "_lblHTTPData";
	private static final String MSG_LBL_OTHER_DATA =
		JAPNewView.class.getName() + "_lblOtherData";

	private static final String IMG_ICONIFY = JAPNewView.class.getName() + "_iconify.gif";
	private static final String IMG_ABOUT = JAPNewView.class.getName() + "_about.gif";

	private static final String MSG_OPEN_FIREFOX = JAPNewView.class.getName() + "_openFirefox";



	private JobQueue m_transferedBytesJobs;
	private JobQueue m_packetMixedJobs;

	private static final String HLP_ANONYMETER = JAPNewView.class.getName() + "_anonymometer";

	private static final String[] METERFNARRAY =
		{
		JAPNewView.class.getName() + "_meterAnonDeactivated.gif", // anonymity deactivated
		JAPNewView.class.getName() + "_meterConnecting.gif", // connecting...
		JAPNewView.class.getName() + "_meterNoMeasure.gif", // no measure available
		JAPNewView.class.getName() + "_meter00.gif",
		JAPNewView.class.getName() + "_meter01.gif",
		JAPNewView.class.getName() + "_meter02.gif",
		JAPNewView.class.getName() + "_meter03.gif",
		JAPNewView.class.getName() + "_meter04.gif",
		JAPNewView.class.getName() + "_meter05.gif",
		JAPNewView.class.getName() + "_meter06.gif",
		JAPNewView.class.getName() + "_meter07.gif",
		JAPNewView.class.getName() + "_meter08.gif",
		JAPNewView.class.getName() + "_meter09.gif",
		JAPNewView.class.getName() + "_meter10.gif"
	};

	private final Object FONT_UPDATE = new Object();
	private boolean m_bFontsUpdated = false;
	private final JLabel DEFAULT_LABEL = new JLabel();

	//private JLabel meterLabel;
	private JLabel m_lblPrice;
	private JLabel m_labelVersion;
	private JPanel m_pnlVersion;
	private JButton m_bttnHelp, m_bttnQuit, m_bttnIconify, m_bttnConf, m_btnAssistant, m_btnAbout;

	JLabel m_buttonDeleteMessage;

	//private Icon[] meterIcons;
	private JAPConf m_dlgConfig;
	private Object LOCK_CONFIG = new Object();
	private JAPViewIconified m_ViewIconified;
	private Object SYNC_ICONIFIED_VIEW = new Object();
	private boolean m_bIsIconified;
	//private final static boolean PROGRESSBARBORDER = true;
	//private GuthabenAnzeige guthaben;
	private boolean m_bWithPayment = false;

	private JAPMixCascadeComboBox m_comboAnonServices;
	private JLabel m_labelAnonService, m_labelAnonymity, m_labelAnonymitySmall, m_labelAnonymityOnOff;
	private JLabel m_labelAnonMeter, m_labelAnonymityLow, m_labelAnonymityHigh;

	private JLabel m_labelAnonymityUser, m_labelAnonymityUserLabel, m_labelMixCountries, m_labelOperatorCountries;
	
	private JLabel m_labelMixFlags[], m_labelOperatorFlags[];
	private MixMouseAdapter m_adapterMix[];
	private OperatorMouseAdapter m_adapterOperator[];
	
	private JLabel m_labelOwnTraffic, m_labelOwnTrafficSmall;
	private JLabel m_labelOwnActivity, m_labelForwarderActivity;
	private JLabel m_labelOwnActivitySmall, m_labelForwarderActivitySmall;
	private JLabel m_labelOwnTrafficBytes, m_labelOwnTrafficUnit;
	private JLabel m_labelOwnTrafficBytesSmall, m_labelOwnTrafficUnitSmall;
	private JLabel m_labelOwnTrafficWWW, m_labelOwnTrafficOther;
	private JLabel m_labelOwnTrafficBytesWWW, m_labelOwnTrafficUnitWWW;
	private JLabel m_labelOwnTrafficBytesOther, m_labelOwnTrafficUnitOther;
	private JLabel m_labelForwarding, m_labelForwardingSmall;
	private JLabel m_labelForwardedTrafficBytes, m_labelForwardedTrafficBytesUnit;
	private JLabel m_labelForwarderCurrentConnections, m_labelForwarderAcceptedConnections;
	private JLabel m_labelForwarderRejectedConnections;
	private JLabel m_labelForwardedTraffic, m_labelForwarderUsedBandwidth;
	private JLabel m_labelForwarderCurrentConnectionsLabel, m_labelForwarderAcceptedConnectionsLabel;
	private JLabel m_labelForwarderRejectedConnectionsLabel, m_labelForwarderUsedBandwidthLabel;
	private JLabel m_labelForwarderConnections;
	private JLabel m_labelForwardingErrorSmall, m_labelForwardingError;
	private JAPProgressBar m_progressOwnTrafficActivity, m_progressOwnTrafficActivitySmall,
		m_progressAnonLevel;
	private JButton m_bttnAnonDetails, m_bttnReload;
	private JButton m_firefox;
	private JCheckBox m_cbAnonymityOn;
	private JRadioButton m_rbAnonOff, m_rbAnonOn;
	private JCheckBox m_cbForwarding, m_cbForwardingSmall;
	private FlippingPanel m_flippingpanelAnon, m_flippingpanelOwnTraffic, m_flippingpanelForward;
	private StatusPanel m_StatusPanel;
	private JPanel m_panelAnonService;
	private int m_iPreferredWidth;
	private Object SYNC_DISCONNECTED_ERROR = new Object();
	private boolean m_bDisconnectedErrorShown = false;
	private boolean m_bIgnoreAnonComboEvents = false;
	private PaymentMainPanel m_flippingPanelPayment;
	private Object m_connectionEstablishedSync = new Object();
	private boolean m_bShowConnecting = false;
	private JAPProgressBar m_progForwarderActivity;
	private JAPProgressBar m_progForwarderActivitySmall;

	private int m_ForwardingID = -1;
	private int m_updateAvailableID = -1;
	private Hashtable m_messageIDs = new Hashtable();
	private int m_enableInfoServiceID = -1;
	private int m_newServicesID = -1;
	private final Object SYNC_STATUS_ENABLE_IS = new Object();
	private final Object SYNC_STATUS_UPDATE_AVAILABLE = new Object();
	private final Object SYNC_NEW_SERVICES = new Object();
	private ActionListener m_listenerUpdate;
	private ActionListener m_listenerEnableIS;
	private ActionListener m_listenerNewServices;

	private volatile long m_lTrafficWWW, m_lTrafficOther;

	private Object SYNC_ACTION = new Object();
	private boolean m_bActionPerformed = false;

	private ComponentMovedAdapter m_mainMovedAdapter;
	private ComponentMovedAdapter m_configMovedAdapter;
	private ComponentMovedAdapter m_helpMovedAdapter;
	private ComponentMovedAdapter m_miniMovedAdapter;

	private boolean m_bTrustChanged = false;

	private boolean m_bIsSimpleView;

	private int m_msgIDInsecure;

	private String[] m_firefoxCommand; //the CLI command for re-opening firefox, usually just the path to the executable

	public JAPNewView(String s, JAPController a_controller, String[] a_firefoxCommand)
	{
		super(s, a_controller);
		m_bIsSimpleView = (JAPModel.getDefaultView() == JAPConstants.VIEW_SIMPLIFIED);
		m_Controller = JAPController.getInstance();
		m_dlgConfig = null; //new JAPConf(this);
		m_bIsIconified = false;
		m_transferedBytesJobs = new JobQueue("Transfered bytes update job queue");
		m_packetMixedJobs = new JobQueue("packet mixed update job queue");
		m_lTrafficWWW = 0;
		m_lTrafficOther = 0;
		m_firefoxCommand = a_firefoxCommand;
	}

	public void create(boolean loadPay)
	{
		m_bWithPayment = loadPay;
		LogHolder.log(LogLevel.INFO, LogType.GUI, "Initializing view...");
		init();
		setTitle(Double.toString(Math.random())); //ensure that we have an uinque title
		JAPDll.setWindowIcon(getTitle());
		setTitle(m_Title);
		LogHolder.log(LogLevel.INFO, LogType.GUI, "View initialized!");
	}

	private void init()
	{
		// important to initialise for TinyL&F!!!
		new SystrayPopupMenu(new SystrayPopupMenu.MainWindowListener()
		{
			public void onOpenBrowser()
			{
				m_Controller.startPortableFirefox(m_firefoxCommand);
			}

			public boolean isBrowserAvailable()
			{
				return (getBrowserCommand() != null);
			}

			public void onShowMainWindow()
			{
			}

			public void onShowSettings(String card, Object a_value)
			{
				showConfigDialog(card, a_value);
			}

			public void onShowHelp()
			{

			}
		});

		this.addMouseListener(new MouseAdapter()
		{
			public void mouseClicked(MouseEvent a_event)
			{
				if (a_event == null)
				{
					return;
				}
				if (SwingUtilities.isRightMouseButton(a_event) || a_event.isPopupTrigger())
				{

					final SystrayPopupMenu popup = new SystrayPopupMenu(
						new SystrayPopupMenu.MainWindowListener()
					{
						public void onOpenBrowser()
						{
							m_Controller.startPortableFirefox(m_firefoxCommand);
						}

						public boolean isBrowserAvailable()
						{
							return (getBrowserCommand() != null);
						}

						public void onShowMainWindow()
						{
							// do nothing
						}

						public void onShowSettings(String card, Object a_value)
						{
							showConfigDialog(card, a_value);
						}

						public void onShowHelp()
						{

						}
					});
					popup.registerExitHandler(new PopupMenu.ExitHandler()
					{
						public void exited()
						{
							popup.dispose();
						}
					});
					popup.show(JAPNewView.this,
							   new Point(a_event.getX() + getLocation().x,
										 a_event.getY() + getLocation().y));

				}
				else if (a_event.getClickCount() == 2)
				{
					showIconifiedView();
				}
			}
		});

		m_listenerUpdate = new ActionListener()
		{
			public void actionPerformed(ActionEvent a_event)
			{
				boolean bUpdated = false;
				Vector vecVersions = new Vector();
				JAPVersionInfo viTemp;

				viTemp = (JAPVersionInfo) Database.getInstance(JAPVersionInfo.class).getEntryById(
					JAPVersionInfo.ID_RELEASE);
				if (viTemp != null)
				{
					vecVersions.addElement(viTemp);
				}
				viTemp = (JAPVersionInfo) Database.getInstance(JAPVersionInfo.class).getEntryById(
					JAPVersionInfo.ID_DEVELOPMENT);
				if (viTemp != null)
				{
					vecVersions.addElement(viTemp);
				}

				Enumeration entries = vecVersions.elements();
				JavaVersionDBEntry versionEntry = null;
				if (!JAPController.getInstance().hasPortableJava() &&
					JAPModel.getInstance().isReminderForJavaUpdateActivated())
				{
					versionEntry = JavaVersionDBEntry.getNewJavaVersion();
				}

				while (entries.hasMoreElements())
				{
					JAPVersionInfo vi = (JAPVersionInfo) entries.nextElement();
					if (vi != null && vi.getJapVersion() != null &&
						vi.getJapVersion().compareTo(JAPConstants.aktVersion) > 0)
					{
						if (JAPConstants.m_bReleasedVersion && !vi.getId().equals(JAPVersionInfo.ID_RELEASE))
						{
							continue;
						}
						JAPUpdateWizard wz = new JAPUpdateWizard(vi,
							JAPController.getInstance().getViewWindow());
						/* we got the JAPVersionInfo from the infoservice */
						if (wz.getStatus() == JAPUpdateWizard.UPDATESTATUS_ERROR)
						{
							/* Download failed -> alert, and reset anon mode to false */
							LogHolder.log(LogLevel.ERR, LogType.MISC, "Some update problem.");
							JAPDialog.showErrorDialog(JAPController.getInstance().getViewWindow(),
								JAPMessages.getString("downloadFailed") +
								JAPMessages.getString("infoURL"), LogType.MISC);
						}
						else if (wz.getStatus() == JAPUpdateWizard.UPDATESTATUS_SUCCESS)
						{
							bUpdated = true;
						}
						break;
					}
				}
				// Do not execute other objects after successfully finishing the wizard!

				if (!bUpdated && versionEntry != null)
				{
					showJavaUpdateDialog(versionEntry);
				}
			}
		};

		m_listenerEnableIS = new ActionListener()
		{
			public void actionPerformed(ActionEvent a_event)
			{
				if (JAPModel.getInstance().isInfoServiceDisabled())
				{
					if (JAPDialog.showConfirmDialog(JAPNewView.this,
						JAPMessages.getString(MSG_IS_DISABLED_EXPLAIN),
						JAPDialog.OPTION_TYPE_YES_NO,
						JAPDialog.MESSAGE_TYPE_WARNING,
						GUIUtils.loadImageIcon(METERFNARRAY[2], true, true))
						== JAPDialog.RETURN_VALUE_YES)
					{
						JAPModel.getInstance().setInfoServiceDisabled(false);
					}
				}

				if (!JAPModel.getInstance().isInfoServiceViaDirectConnectionAllowed() &&
					!JAPController.getInstance().isAnonConnected())
				{
					if (JAPDialog.showConfirmDialog(JAPNewView.this, JAPMessages.getString(
						JAPController.MSG_IS_NOT_ALLOWED),
						JAPDialog.OPTION_TYPE_YES_NO,
						JAPDialog.MESSAGE_TYPE_WARNING)
						== JAPDialog.RETURN_VALUE_YES)
					{
						JAPModel.getInstance().allowInfoServiceViaDirectConnection(true);
					}
				}
			}
		};

		m_listenerNewServices = new ActionListener()
		{
			public void actionPerformed(ActionEvent a_event)
			{
				JAPDialog.showMessageDialog(JAPNewView.this,
											JAPMessages.getString(MSG_NEW_SERVICES_FOUND_EXPLAIN,
					JAPMessages.getString(MSG_SERVICE_NAME)));
				m_comboAnonServices.showPopup();
			}
		};

		m_flippingpanelOwnTraffic = new FlippingPanel(this);
		m_flippingpanelForward = new FlippingPanel(this);

		// Load Icon in upper left corner of the frame window
		ImageIcon ii = GUIUtils.loadImageIcon(JAPConstants.IICON16FN, true, false);
		if (ii != null)
		{
			setIconImage(ii.getImage());
			// Load Images for "Anonymity Meter"
		}
		// preload meter icons

		// "NORTH": Image
		ImageIcon headerImage = GUIUtils.loadImageIcon("JonDo.png", true, false);
		JLabel headerLabel = new JLabel(headerImage);
		//headerLabel.setHorizontalAlignment(SwingConstants.CENTER);
		JPanel mainPanel = new JPanel();
		GridBagLayout gbl = new GridBagLayout();
		GridBagConstraints c = new GridBagConstraints();
		mainPanel.setLayout(gbl);
		c.anchor = GridBagConstraints.NORTH;
		c.fill = GridBagConstraints.NONE;
		c.insets = new Insets(5, 5, 0, 0);
		c.weighty = 1;
		c.gridheight = 2;
		//c.gridwidth = 2; //header label should be centered, so fill the whole row with the panel
		mainPanel.add(headerLabel, c);

		c.anchor = GridBagConstraints.EAST;
		c.gridx++;
		c.gridx++;
		c.gridheight = 1;
		c.gridy = 0;

		m_pnlVersion = new JPanel(new GridBagLayout());
		GridBagConstraints constrVersion = new GridBagConstraints();
		constrVersion.anchor = GridBagConstraints.SOUTHEAST;
		constrVersion.insets = new Insets(0, 0, 0, 10);
		constrVersion.gridx = 0;
		constrVersion.gridy = 0;

		c.gridy = 1;
		c.anchor = GridBagConstraints.SOUTHEAST;
		c.weighty = 0;
		m_labelVersion = new JLabel(JAPConstants.aktVersion);
		m_labelVersion.setForeground(Color.blue);
		m_labelVersion.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		m_labelVersion.addMouseListener(new MouseAdapter()
		{
			public void mouseClicked(MouseEvent e)
			{
				JAPController.aboutJAP();
			}
		});
		c.insets = new Insets(0, 0, 0, 10);
		//northPanel.add(m_labelVersion, c);
		constrVersion.gridx++;
		constrVersion.insets = new Insets(0, 0, 0, 0);
		m_pnlVersion.add(m_labelVersion, constrVersion);

		if (m_Controller.isPortableMode())
		{
			m_firefox = new JButton(GUIUtils.loadImageIcon("firefox.png", true, false));
			m_firefox.setOpaque(false);
			m_firefox.setToolTipText(JAPMessages.getString(MSG_OPEN_FIREFOX));
			m_firefox.setMnemonic('W');

			m_firefox.addActionListener(new ActionListener()
			{
				public void actionPerformed(ActionEvent e)
				{
					m_Controller.startPortableFirefox(m_firefoxCommand);
				}
			});

			mainPanel.add(m_firefox, c);
		}

		//northPanel.add(m_pnlVersion, c);


		c.gridwidth = 2;
		c.gridx = 0;
		c.gridy++;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.weightx = 1;
		c.insets = new Insets(5, 10, 5, 10);
		mainPanel.add(new JSeparator(), c);

		GridBagLayout gbl1 = new GridBagLayout();
		GridBagConstraints c1 = new GridBagConstraints();

		m_panelAnonService = new JPanel(gbl1);
		m_labelAnonService = new JLabel(JAPMessages.getString(MSG_SERVICE_NAME) + ":");
		c1.insets = new Insets(0, 0, 0, 0);
		c1.anchor = GridBagConstraints.WEST;
		c1.weightx = 0;
		c1.fill = GridBagConstraints.NONE;
		m_panelAnonService.add(m_labelAnonService, c1);
		m_comboAnonServices = new JAPMixCascadeComboBox();
		m_comboAnonServices.addItemListener(new ItemListener()
		{
			public void itemStateChanged(ItemEvent e)
			{
				final MixCascade cascade = (MixCascade) m_comboAnonServices.getSelectedItem();
				if (cascade != null)
				{
					SwingUtilities.invokeLater(new Runnable()
					{
						public void run()
						{
							if (cascade.isPayment())
							{
								m_lblPrice.setText(JAPMessages.getString(MSG_WITH_COSTS));
								m_lblPrice.setForeground(Color.blue);
								m_lblPrice.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
							}
							else
							{
								m_lblPrice.setText(JAPMessages.getString(MSG_NO_COSTS));
								m_lblPrice.setForeground(new JLabel().getForeground());
								m_lblPrice.setCursor(Cursor.getDefaultCursor());
							}
						}
					});
				}

				if (m_bIgnoreAnonComboEvents)
				{
					return;
				}
				if (e.getStateChange() == ItemEvent.SELECTED)
				{
					SwingUtilities.invokeLater(new Runnable()
					{
						public void run()
						{
							m_Controller.setCurrentMixCascade(cascade);
						}
					});
				}
			}
		});

		c1.insets = new Insets(0, 5, 0, 0);
		c1.gridwidth = 3;
		c1.fill = GridBagConstraints.HORIZONTAL;
		c1.weightx = 1;
		m_panelAnonService.add(m_comboAnonServices, c1);
		c1.gridwidth = 1;
		m_bttnReload = new JButton(GUIUtils.loadImageIcon(JAPConstants.IMAGE_RELOAD, true, false));
		m_bttnReload.setOpaque(false);
		LookAndFeel laf = UIManager.getLookAndFeel();
		if (laf != null && UIManager.getCrossPlatformLookAndFeelClassName().equals(laf.getClass().getName())) //stupid but is necessary for JDK 1.5 and Metal L&F on Windows XP (and maybe others)
		{
			/*
			if (m_firefox != null)
			{
				m_firefox.setBackground(Color.gray);
			}*/
			m_bttnReload.setBackground(Color.gray);
		}
		m_bttnReload.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				fetchMixCascadesAsync(true);
			}
		});
		m_bttnReload.setRolloverEnabled(true);
		m_bttnReload.setToolTipText(JAPMessages.getString("ngCascadeReloadTooltip"));
		ImageIcon tmpIcon = GUIUtils.loadImageIcon(JAPConstants.IMAGE_RELOAD_ROLLOVER, true, false);
		m_bttnReload.setRolloverIcon(tmpIcon);
		m_bttnReload.setSelectedIcon(tmpIcon);
		m_bttnReload.setRolloverSelectedIcon(tmpIcon);
		m_bttnReload.setPressedIcon(tmpIcon);
		m_bttnReload.setDisabledIcon(GUIUtils.loadImageIcon(JAPConstants.IMAGE_RELOAD_DISABLED, true, false));
		m_bttnReload.setBorder(new EmptyBorder(0, 0, 0, 0));
		m_bttnReload.setFocusPainted(false);
		m_bttnReload.setBorderPainted(true);
		m_bttnReload.setContentAreaFilled(false);
		c1.gridx = 4;
		c1.weightx = 0;
		c1.fill = GridBagConstraints.NONE;
		m_panelAnonService.add(m_bttnReload, c1);
		m_bttnAnonDetails = new JButton(JAPMessages.getString("ngBttnAnonDetails"));
		m_bttnAnonDetails.setToolTipText(JAPMessages.getString("ngBttnAnonDetails"));
		m_bttnAnonDetails.setMnemonic(JAPMessages.getString(MSG_MN_DETAILS).charAt(0));

		m_bttnAnonDetails.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				showConfigDialog(JAPConf.ANON_TAB, JAPController.getInstance().getCurrentMixCascade());
			}
		});
		c1.gridx = 5;
		c1.weightx = 0;
		c1.fill = GridBagConstraints.NONE;
		m_panelAnonService.add(m_bttnAnonDetails, c1);

		c1.gridx = 0;
		c1.gridy = 1;
		c1.anchor = GridBagConstraints.WEST;
		c1.insets = new Insets(5, 0, 0, 0);
		JLabel lblTrust = new JLabel(JAPMessages.getString(MSG_TRUST_FILTER) + ":");
		//m_panelAnonService.add(lblTrust, c1);

		c1.gridx = 1;
		c1.gridy = 1;
		c1.anchor = GridBagConstraints.WEST;
		c1.insets = new Insets(5, 5, 0, 0);
		//c1.fill = GridBagConstraints.HORIZONTAL;
		m_lblPrice = new JLabel(JAPMessages.getString(MSG_NO_COSTS));
		m_lblPrice.addMouseListener(new MouseAdapter()
		{
			public void mouseClicked(MouseEvent a_event)
			{
				if (m_lblPrice.getCursor() == Cursor.getPredefinedCursor(Cursor.HAND_CURSOR))
				{
					JAPDialog.showMessageDialog(JAPController.getInstance().getViewWindow(),
												JAPMessages.getString(MSG_NO_REAL_PAYMENT));
				}
			}
		});
		//m_panelAnonService.add(m_lblPrice, c1);




		c1.insets = new Insets(5, 20, 0, 0);
		c.weighty = 1;
		c.gridwidth = 2;
		c.gridy++;
		c.gridx = 0;
		c.anchor = GridBagConstraints.WEST;
		mainPanel.add(m_panelAnonService, c);

//------------------------------------------------------
		c.gridwidth = 2;
		c.gridx = 0;
		c.gridy++;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.weightx = 1;
		mainPanel.add(new JSeparator(), c);

//------------------ Anon Panel
		m_flippingpanelAnon = new FlippingPanel(this);
		//the panel with all the deatils....
		JPanel p = new JPanel();
		gbl1 = new GridBagLayout();
		c1 = new GridBagConstraints();
		c1.anchor = GridBagConstraints.NORTHWEST;
		p.setLayout(gbl1);
		m_labelAnonymity = new JLabel(JAPMessages.getString("ngCascadeInfo"));
		c1.insets = new Insets(0, 5, 0, 0);
		p.add(m_labelAnonymity, c1);
		
		m_labelAnonymityUserLabel = new JLabel(JAPMessages.getString("ngNrOfUsers") + ":");
		c1.gridy = 1;
		c1.anchor = GridBagConstraints.WEST;
		c1.insets = new Insets(5, 15, 0, 10);
		p.add(m_labelAnonymityUserLabel, c1);
		
		m_labelMixCountries = new JLabel(JAPMessages.getString("ngMixCountries"));
		c1.gridy = 2;
		p.add(m_labelMixCountries, c1);
		
		m_labelOperatorCountries = new JLabel(JAPMessages.getString("ngOperatorCountries"));
		c1.gridy = 3;
		p.add(m_labelOperatorCountries, c1);
		
		m_labelAnonymityUser = new JLabel("", SwingConstants.LEFT);
		c1.insets = new Insets(0, 0, 0, 0);
		c1.anchor = GridBagConstraints.WEST;
		c1.weightx = 0;
		c1.fill = GridBagConstraints.HORIZONTAL;
		c1.gridy = 1;
		c1.gridx = 1;
		c1.gridwidth = 3;
		p.add(m_labelAnonymityUser, c1);

		m_labelMixFlags = new JLabel[3];
		m_labelOperatorFlags = new JLabel[3];
		m_adapterMix = new MixMouseAdapter[3];
		m_adapterOperator = new OperatorMouseAdapter[3];
		
		c1.gridwidth = 1;
		c1.insets = new Insets(5, 2, 0, 5);
		for(int i = 0; i < 3; i++)
		{
			c1.gridx = i + 1;
			
			m_labelMixFlags[i] = new JLabel("");
			c1.gridy = 2;
			p.add(m_labelMixFlags[i], c1);
			
			m_labelMixFlags[i].addMouseListener(m_adapterMix[i] = 
				new MixMouseAdapter(null, i));
			m_labelMixFlags[i].setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
			
			c1.gridy = 3;
			m_labelOperatorFlags[i] = new JLabel("");
			p.add(m_labelOperatorFlags[i], c1);
			
			m_labelOperatorFlags[i].addMouseListener(m_adapterOperator[i] =
				new OperatorMouseAdapter(null));
			m_labelOperatorFlags[i].setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		}
		
		m_labelAnonMeter = new JLabel(getMeterImage(3));
		m_labelAnonMeter.setToolTipText(JAPMessages.getString(MSG_ANONYMETER_TOOL_TIP));
		m_labelAnonMeter.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		m_labelAnonMeter.addMouseListener(new MouseAdapter()
		{
			public void mouseClicked(MouseEvent a_event)
			{
				JAPHelp.getInstance().getContextObj().setContext(HLP_ANONYMETER);
				JAPHelp.getInstance().setVisible(true);
			}
		});
		c1.gridx = 4;
		c1.gridy = 0;
		c1.gridheight = 4;
		c1.anchor = GridBagConstraints.EAST;
		c1.weightx = 1;
		c1.fill = GridBagConstraints.NONE;
		c1.insets = new Insets(0, 10, 0, 10);
		p.add(m_labelAnonMeter, c1);

		GridBagLayout gbl2 = new GridBagLayout();
		GridBagConstraints c2 = new GridBagConstraints();
		JPanel p2 = new JPanel(gbl2);
		p2.setBorder(LineBorder.createBlackLineBorder());
		//new BoxLayout(p2,BoxLayout.Y_AXIS);
		m_labelAnonymityOnOff = new JLabel(JAPMessages.getString("ngAnonymitaet"));
		c2.anchor = GridBagConstraints.NORTHWEST;
		c2.insets = new Insets(2, 2, 2, 2);
		p2.add(m_labelAnonymityOnOff, c2);
		m_rbAnonOn = new JRadioButton(JAPMessages.getString("ngAnonOn"));
		m_rbAnonOn.addActionListener(this);
		m_rbAnonOff = new JRadioButton(JAPMessages.getString("ngAnonOff"));
		m_rbAnonOff.addActionListener(this);
		ButtonGroup bg = new ButtonGroup();
		bg.add(m_rbAnonOn);
		bg.add(m_rbAnonOff);
		m_rbAnonOff.setSelected(true);
		c2.gridy = 1;
		c2.insets = new Insets(0, 7, 0, 0);
		p2.add(m_rbAnonOn, c2);
		c2.gridy = 2;
		p2.add(m_rbAnonOff, c2);

		c1.gridx = 5;
		c1.weightx = 0;
		c1.anchor = GridBagConstraints.WEST;
		c1.insets = new Insets(0, 10, 0, 0);
		p.add(p2, c1);
		m_flippingpanelAnon.setFullPanel(p);

		//the small panel
		gbl1 = new GridBagLayout();
		c1 = new GridBagConstraints();
		p = new JPanel(gbl1);
		m_labelAnonymitySmall = new JLabel(JAPMessages.getString("ngAnonymitaet") + ":");
		c1.gridx = 0;
		c1.anchor = GridBagConstraints.WEST;
		c1.weightx = 0;
		c1.insets = new Insets(0, 5, 0, 0);
		p.add(m_labelAnonymitySmall, c1);
		m_cbAnonymityOn = new JCheckBox(JAPMessages.getString("ngAnonOn"));
		m_cbAnonymityOn.setBorder(null);
		m_cbAnonymityOn.addActionListener(this);
		c1.gridx = 1;
		c1.insets = new Insets(0, 10, 0, 0);
		p.add(m_cbAnonymityOn, c1);
		m_labelAnonymityLow = new JLabel(JAPMessages.getString("ngAnonymityLow"), SwingConstants.RIGHT);
		c1.insets = new Insets(0, 20, 0, 5);
		c1.gridx = 2;
		c1.weightx = 0.5;
		c1.fill = GridBagConstraints.HORIZONTAL;
		c1.anchor = GridBagConstraints.EAST;
		p.add(m_labelAnonymityLow, c1);
		m_progressAnonLevel = new JAPProgressBar();
		m_progressAnonLevel.setMinimum(StatusInfo.ANON_LEVEL_MIN);
		m_progressAnonLevel.setMaximum(StatusInfo.ANON_LEVEL_MAX);
		m_progressAnonLevel.setBorderPainted(false);
		c1.weightx = 0.75;
		c1.fill = GridBagConstraints.HORIZONTAL;
		c1.anchor = GridBagConstraints.CENTER;
		c1.insets = new Insets(0, 0, 0, 0);
		c1.gridx = 3;
		p.add(m_progressAnonLevel, c1);
		m_labelAnonymityHigh = new JLabel(JAPMessages.getString("ngAnonymityHigh"));
		c1.gridx = 4;
		c1.weightx = 0.5;
		c1.insets = new Insets(0, 0, 0, 0);
		c1.fill = GridBagConstraints.HORIZONTAL;
		c1.anchor = GridBagConstraints.WEST;
		p.add(m_labelAnonymityHigh, c1);
		m_flippingpanelAnon.setSmallPanel(p);

		c.fill = GridBagConstraints.HORIZONTAL;
		c.weightx = 1;
		c.anchor = GridBagConstraints.NORTHWEST;
		c.gridy++;
		m_flippingpanelAnon.setFlipped(true);
		if (m_bIsSimpleView)
		{
			mainPanel.add(m_flippingpanelAnon.getFullPanel(), c);
		}
		else
		{
			mainPanel.add(m_flippingpanelAnon, c);
		}

//-----------------------------------------------------------
		c.gridwidth = 2;
		c.gridx = 0;
		c.gridy++;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.weightx = 1;
		mainPanel.add(new JSeparator(), c);

		m_labelOwnActivity = new JLabel(JAPMessages.getString("ngActivity"), SwingConstants.RIGHT);
//------------------ Payment Panel
		if (m_bWithPayment)
		{
			m_flippingPanelPayment = new PaymentMainPanel(this, m_labelOwnActivity);
			c.fill = GridBagConstraints.HORIZONTAL;
			c.weightx = 1;
			c.anchor = GridBagConstraints.NORTHWEST;
			c.gridy++;
			m_flippingPanelPayment.setFlipped(false);
			if (m_bIsSimpleView)
			{
				mainPanel.add(m_flippingPanelPayment.getSmallPanel(), c);
			}
			else
			{
				mainPanel.add(m_flippingPanelPayment, c);
			}
//-----------------------------------------------------------
			// Separator
			c.gridwidth = 2;
			c.gridx = 0;
			c.gridy++;
			c.fill = GridBagConstraints.HORIZONTAL;
			c.weightx = 1;
			mainPanel.add(new JSeparator(), c);
		}
//-----------------------------------------------------------

//------------------ Own Traffic Panel
		//m_flippingpanelOwnTraffic = new FlippingPanel(this);
		//full
		gbl1 = new GridBagLayout();
		c1 = new GridBagConstraints();
		p = new JPanel(gbl1);
		m_labelOwnTraffic = new JLabel(JAPMessages.getString(MSG_LBL_ENCRYPTED_DATA) + ":");
		c1.insets = new Insets(0, 5, 0, 0);
		c1.anchor = GridBagConstraints.WEST;
		c1.weightx = 0;
		c1.fill = GridBagConstraints.HORIZONTAL;
		p.add(m_labelOwnTraffic, c1);
		JComponent spacer = new JPanel();
		Dimension spacerDimension =
			new Dimension(m_labelVersion.getFontMetrics(m_labelVersion.getFont()).charWidth('9') * 6, 1);
		spacer.setPreferredSize(spacerDimension);
		c1.insets = new Insets(0, 0, 0, 0);
		c1.gridx = 1;
		c1.fill = GridBagConstraints.NONE;
		c1.weightx = 1;
		p.add(spacer, c1);
		m_labelOwnTrafficBytes = new JLabel("0");
		m_labelOwnTrafficBytes.setHorizontalAlignment(JLabel.RIGHT);
		c1.insets = new Insets(0, 5, 0, 0);
		c1.weightx = 0;
		c1.fill = GridBagConstraints.HORIZONTAL;
		c1.gridx = 2;
		p.add(m_labelOwnTrafficBytes, c1);
		m_labelOwnTrafficUnit = new JLabel(JAPMessages.getString("Byte"));
		c1.gridx = 3;
		p.add(m_labelOwnTrafficUnit, c1);
		//m_labelOwnActivity = new JLabel(JAPMessages.getString("ngActivity"), SwingConstants.RIGHT);
		c1.weightx = 0;
		c1.fill = GridBagConstraints.HORIZONTAL;
		c1.gridx = 4;
		c1.insets = new Insets(0, 10, 0, 0);
		p.add(m_labelOwnActivity, c1);
		m_progressOwnTrafficActivity = new JAPProgressBar();
		m_progressOwnTrafficActivity.setMinimum(0);
		m_progressOwnTrafficActivity.setMaximum(5);
		m_progressOwnTrafficActivity.setBorderPainted(false);
		c1.gridx = 5;
		c1.weightx = 0;
		c1.fill = GridBagConstraints.NONE;
		c1.insets = new Insets(0, 5, 0, 0);
		p.add(m_progressOwnTrafficActivity, c1);
		m_labelOwnTrafficWWW = new JLabel(JAPMessages.getString(MSG_LBL_HTTP_DATA) + ":");
		c1.insets = new Insets(10, 20, 0, 0);
		c1.gridx = 0;
		c1.gridy = 1;
		c1.anchor = GridBagConstraints.WEST;
		c1.weightx = 0;
		p.add(m_labelOwnTrafficWWW, c1);
		spacer = new JPanel();
		spacer.setPreferredSize(spacerDimension);
		c1.gridx = 1;
		c1.insets = new Insets(0, 0, 0, 0);
		c1.weightx = 1;
		c1.fill = GridBagConstraints.NONE;
		p.add(spacer, c1);
		m_labelOwnTrafficBytesWWW = new JLabel("0");
		m_labelOwnTrafficBytesWWW.setHorizontalAlignment(JLabel.RIGHT);
		c1.insets = new Insets(10, 5, 0, 0);
		c1.gridx = 2;
		c1.fill = GridBagConstraints.HORIZONTAL;
		c1.weightx = 0;
		p.add(m_labelOwnTrafficBytesWWW, c1);
		m_labelOwnTrafficUnitWWW = new JLabel(JAPMessages.getString("Byte"));
		c1.gridx = 3;
		p.add(m_labelOwnTrafficUnitWWW, c1);
		m_labelOwnTrafficOther = new JLabel(JAPMessages.getString(MSG_LBL_OTHER_DATA) + ":");
		c1.insets = new Insets(7, 20, 0, 0);
		c1.gridx = 0;
		c1.gridy = 2;
		p.add(m_labelOwnTrafficOther, c1);
		spacer = new JPanel();
		spacer.setPreferredSize(spacerDimension);
		c1.insets = new Insets(0, 0, 0, 0);
		c1.weightx = 1;
		c1.gridx = 1;
		c1.fill = GridBagConstraints.NONE;
		p.add(spacer, c1);
		m_labelOwnTrafficBytesOther = new JLabel("0");
		m_labelOwnTrafficBytesOther.setHorizontalAlignment(JLabel.RIGHT);
		c1.fill = GridBagConstraints.HORIZONTAL;
		c1.weightx = 0;
		c1.insets = new Insets(7, 5, 0, 0);
		c1.gridx = 2;
		p.add(m_labelOwnTrafficBytesOther, c1);
		m_labelOwnTrafficUnitOther = new JLabel(JAPMessages.getString("Byte"));
		c1.gridx = 3;
		p.add(m_labelOwnTrafficUnitOther, c1);
		m_flippingpanelOwnTraffic.setFullPanel(p);

		//small
		gbl1 = new GridBagLayout();
		c1 = new GridBagConstraints();
		p = new JPanel(gbl1);
		m_labelOwnTrafficSmall = new JLabel(JAPMessages.getString(MSG_LBL_ENCRYPTED_DATA) + ":");
		c1.insets = new Insets(0, 5, 0, 0);
		c1.weightx = 0;
		c1.fill = GridBagConstraints.NONE;
		c1.anchor = GridBagConstraints.WEST;
		p.add(m_labelOwnTrafficSmall, c1);
		m_labelOwnTrafficBytesSmall = new JLabel("0");
		m_labelOwnTrafficBytesSmall.setHorizontalAlignment(JLabel.RIGHT);
		c1.weightx = 1;
		c1.fill = GridBagConstraints.HORIZONTAL;
		c1.gridx = 1;
		p.add(m_labelOwnTrafficBytesSmall, c1);
		m_labelOwnTrafficUnitSmall = new JLabel(JAPMessages.getString("Byte"));
		c1.gridx = 2;
		c1.weightx = 0;
		c1.fill = GridBagConstraints.NONE;
		p.add(m_labelOwnTrafficUnitSmall, c1);
		m_labelOwnActivitySmall = new JLabel(JAPMessages.getString("ngActivity"), SwingConstants.RIGHT);
		c1.insets = new Insets(0, 10, 0, 0);
		c1.gridx = 3;
		p.add(m_labelOwnActivitySmall, c1);
		m_progressOwnTrafficActivitySmall = new JAPProgressBar();
		m_progressOwnTrafficActivitySmall.setMinimum(0);
		m_progressOwnTrafficActivitySmall.setMaximum(5);
		m_progressOwnTrafficActivitySmall.setBorderPainted(false);
		c1.weightx = 0;
		c1.insets = new Insets(0, 5, 0, 0);
		c1.fill = GridBagConstraints.NONE;
		c1.gridx = 4;
		p.add(m_progressOwnTrafficActivitySmall, c1);
		m_flippingpanelOwnTraffic.setSmallPanel(p);

		c.fill = GridBagConstraints.HORIZONTAL;
		c.weightx = 1;
		c.anchor = GridBagConstraints.NORTHWEST;
		c.gridy++;
		if (m_bIsSimpleView)
		{
			mainPanel.add(m_flippingpanelOwnTraffic.getSmallPanel(), c);
		}
		else
		{
			mainPanel.add(m_flippingpanelOwnTraffic, c);
		}

//-----------------------------------------------------------
		c.gridwidth = 2;
		c.gridx = 0;
		c.gridy++;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.weightx = 1;
		mainPanel.add(new JSeparator(), c);

// Forwarder Panel
		c.fill = GridBagConstraints.HORIZONTAL;
		c.weightx = 1;
		c.anchor = GridBagConstraints.NORTHWEST;
		c.gridy++;
		m_flippingpanelForward = buildForwarderPanel();
		if (!m_bIsSimpleView)
		{
			mainPanel.add(m_flippingpanelForward, c);

//-----------------------------------------------------------
			c.gridwidth = 2;
			c.gridx = 0;
			c.gridy++;
			c.fill = GridBagConstraints.HORIZONTAL;
			c.weightx = 1;
			mainPanel.add(new JSeparator(), c);
		}
//Status

		c.gridy++;
		JPanel panelTmp = new JPanel(new GridBagLayout());
		m_buttonDeleteMessage = new JLabel(JAPMessages.getString(MSG_HIDE_MESSAGE_SHORT));
		m_buttonDeleteMessage.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		m_buttonDeleteMessage.setToolTipText(JAPMessages.getString(MSG_DELETE_MESSAGE));
		m_StatusPanel = new StatusPanel(m_buttonDeleteMessage);
		GridBagConstraints constraints = new GridBagConstraints();
		constraints.gridx = 0;
		constraints.gridy = 0;
		constraints.weightx = 1.0;
		constraints.fill =  GridBagConstraints.HORIZONTAL;
		panelTmp.add(m_StatusPanel, constraints);
		constraints.weightx = 0.0;
		constraints.gridx = 1;
		constraints.anchor = GridBagConstraints.EAST;
		panelTmp.add(m_buttonDeleteMessage, constraints);
		mainPanel.add(panelTmp, c);
		//northPanel.add(m_buttonDeleteMessage, c);

    	//register as MessageListener with all existing accounts
		/*
		Enumeration allAccounts = PayAccountsFile.getInstance().getAccounts();
		while (allAccounts.hasMoreElements())
		{
			PayAccount curAccount = (PayAccount) allAccounts.nextElement();
			curAccount.addMessageListener(this);
			//load messages from the balances we already have (new messages are only displayed if different from existing ones)
			XMLBalance existingBalance = curAccount.getAccountInfo().getBalance();
			String existingMessage = existingBalance.getMessage();
			String existingMessageText = existingBalance.getMessageText();
			String existingMessageLink = existingBalance.getMessageLink();
			if (existingMessage != null && !existingMessage.equals(""))
			{
				this.messageReceived(existingMessage, existingMessageText, existingMessageLink);
			}
		}*/
		//make sure to be noticed of new or deleted accounts
	    //PayAccountsFile.getInstance().addPaymentListener(this);



		/*
		* Do not use - for some strange reason this line would prevent Jap from connecting to any pay cascade
		  //make sure to be noticed of new or deleted accounts
	      //PayAccountsFile.getInstance().addPaymentListener(this);
	    */

	    PayAccountsFile.getInstance().addMessageListener(this);
		PayAccountsFile.fireKnownMessages();



//-----------------------------------------------------------
//		c.gridwidth = 2;
//		c.gridx = 0;
//		c.gridy = 11;
		//	c.fill = GridBagConstraints.HORIZONTAL;
		//	c.weightx = 1;
		//	northPanel.add(new JSeparator(), c);
//---------------------------------------------------------

//Buttons
		gbl1 = new GridBagLayout();
		c1 = new GridBagConstraints();
		JPanel buttonPanel = new JPanel(gbl1);
		m_bttnHelp = new JButton(JAPMessages.getString(JAPHelp.MSG_HELP_BUTTON));
		//m_bttnHelp = new JButton();
		m_bttnHelp.setToolTipText(JAPMessages.getString(JAPHelp.MSG_HELP_BUTTON));
		m_btnAbout = new JButton();
		m_btnAbout.setToolTipText(JAPMessages.getString("aboutBox"));
		m_bttnQuit = new JButton(JAPMessages.getString("quitButton"));
		m_bttnQuit.setToolTipText(JAPMessages.getString("quitButton"));
		m_btnAssistant = new JButton(JAPMessages.getString(MSG_BTN_ASSISTANT));
		m_btnAssistant.setToolTipText(JAPMessages.getString(MSG_BTN_ASSISTANT));
		m_bttnConf = new JButton(JAPMessages.getString("confButton"));
		m_bttnConf.setToolTipText(JAPMessages.getString("confButton"));
		m_bttnIconify = new JButton();
		m_bttnIconify.setToolTipText(JAPMessages.getString("iconifyWindow"));

		// Add real buttons
		c1.fill = GridBagConstraints.VERTICAL;
		buttonPanel.add(m_bttnIconify, c1);
		//buttonPanel.add(m_bttnInfo);
		c1.gridx = 1;
		c1.insets = new Insets(0, 5, 0, 0);
		buttonPanel.add(m_btnAbout, c1);
		c1.gridx++;
		buttonPanel.add(m_bttnHelp, c1);
		c1.gridx++;
		buttonPanel.add(m_btnAssistant, c1);
		c1.gridx++;
		buttonPanel.add(m_bttnConf, c1);

		c1.gridx++;
		//c1.weightx = 1;
		c1.fill = GridBagConstraints.HORIZONTAL;
		buttonPanel.add(new JLabel(), c1);
		c1.gridx++;
		buttonPanel.add(m_bttnQuit, c1);
		m_bttnIconify.addActionListener(this);
		m_bttnConf.addActionListener(this);
		m_btnAbout.addActionListener(this);
		m_bttnHelp.addActionListener(this);
		m_bttnQuit.addActionListener(this);
		m_btnAssistant.addActionListener(this);
		JAPUtil.setMnemonic(m_bttnIconify, JAPMessages.getString("iconifyButtonMn"));
		JAPUtil.setMnemonic(m_bttnConf, JAPMessages.getString("confButtonMn"));
		JAPUtil.setMnemonic(m_bttnHelp, JAPMessages.getString("helpButtonMn"));
		JAPUtil.setMnemonic(m_bttnQuit, JAPMessages.getString("quitButtonMn"));
		JAPUtil.setMnemonic(m_btnAssistant, JAPMessages.getString(MSG_MN_ASSISTANT));

		c.gridy++;
		mainPanel.add(buttonPanel, c);

		getContentPane().setBackground(buttonPanel.getBackground());
		getContentPane().add(mainPanel, BorderLayout.CENTER);

		/*if (!JAPModel.isSmallDisplay())
		 {
		 getContentPane().add(northPanel, BorderLayout.NORTH);
		 getContentPane().add(westLabel, BorderLayout.WEST);
		 getContentPane().add(new JLabel("  "), BorderLayout.EAST); //Spacer
		 getContentPane().add(buttonPanel, BorderLayout.SOUTH);
		   }
		  }*/
		//tabs.setSelectedComponent(level);

		addWindowListener(new WindowAdapter()
		{
			public void windowClosing(WindowEvent e)
			{
				if (isEnabled())
				{
					JAPController.goodBye(true);
				}
			}

			public void windowDeiconified(WindowEvent e)
			{
				m_bIsIconified = false;
				updateValues(false);
			}

			public void windowIconified(WindowEvent e)
			{
				hideWindowInTaskbar();
				m_bIsIconified = true;
				updateValues(false);
			}
		});

		update(null, new JAPModel.FontResize(0, JAPModel.getInstance().getFontSize()));

		Dimension d = super.getPreferredSize();
		m_iPreferredWidth = Math.max(d.width, Math.max(m_flippingpanelOwnTraffic.getPreferredSize().width,
			Math.max(
				Math.max(m_panelAnonService.getPreferredSize().width,
						 m_flippingpanelForward.getPreferredSize().width),
				m_flippingpanelAnon.getPreferredSize().width)));
		if (!JAPModel.getInstance().isInfoServiceDisabled())
		{
			fetchMixCascadesAsync(false);
		}

		updateFonts();
		updateValues(true);
		setOptimalSize();
		GUIUtils.centerOnScreen(this);
		GUIUtils.restoreLocation(this, JAPModel.getInstance().getMainWindowLocation());

		Database.getInstance(StatusInfo.class).addObserver(this);
		Database.getInstance(JAPVersionInfo.class).addObserver(this);
		Database.getInstance(JavaVersionDBEntry.class).addObserver(this);
		Database.getInstance(MixCascade.class).addObserver(this);
		Database.getInstance(NewCascadeIDEntry.class).addObserver(this);
		Database.getInstance(CascadeIDEntry.class).addObserver(this);
		Database.getInstance(BlacklistedCascadeIDEntry.class).addObserver(this);
		Database.getInstance(MessageDBEntry.class).addObserver(this);
		TrustModel.addModelObserver(this);

		JAPModel.getInstance().addObserver(this);
		JAPModel.getInstance().getRoutingSettings().addObserver(this);

		JAPHelp.init(this, AbstractOS.getInstance(), AbstractOS.getInstance());
		JAPHelp.getInstance().setLocationCenteredOnOwner();
		JAPHelp.getInstance().resetAutomaticLocation(JAPModel.getInstance().isHelpWindowLocationSaved());
		JAPHelp.getInstance().restoreLocation(JAPModel.getInstance().getHelpWindowLocation());
		JAPHelp.getInstance().restoreSize(JAPModel.getInstance().getHelpWindowSize());

		m_mainMovedAdapter = new ComponentMovedAdapter();
		m_helpMovedAdapter = new ComponentMovedAdapter();
		m_configMovedAdapter = new ComponentMovedAdapter();
		addComponentListener(m_mainMovedAdapter);
		JAPHelp.getInstance().addComponentListener(m_helpMovedAdapter);

		//new GUIUtils.WindowDocker(this);


		new Thread(new Runnable()
		{
			public void run()
			{
				synchronized (LOCK_CONFIG)
				{
					if (m_dlgConfig == null)
					{
						m_dlgConfig = new JAPConf(JAPNewView.this, m_bWithPayment);
						m_dlgConfig.addComponentListener(m_configMovedAdapter);
					}
				}
			}
		}).start();
	}

	private FlippingPanel buildForwarderPanel()
	{
		//------------------ Forwarder Panel
		FlippingPanel flippingPanel = new FlippingPanel(this);
		//big view
		GridBagConstraints c1 = new GridBagConstraints();
		c1.insets = new Insets(0, 5, 0, 0);
		c1.anchor = GridBagConstraints.WEST;
		JPanel p = new JPanel(new GridBagLayout());
		GridBagLayout gbl = new GridBagLayout();
		GridBagConstraints c2 = new GridBagConstraints();
		JPanel p2 = new JPanel(new GridBagLayout());
		m_labelForwarding = new JLabel(JAPMessages.getString("ngForwarding"));
		c2.insets = new Insets(0, 0, 0, 0);
		c2.anchor = GridBagConstraints.WEST;
		p2.add(m_labelForwarding, c2);

		m_cbForwarding = new JCheckBox(JAPMessages.getString("ngForwardingOn"));
		m_cbForwarding.setBorder(null);
		ActionListener actionListener = new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				/* start or shutdown the forwarding server */
				m_Controller.enableForwardingServer( ( (JCheckBox) e.getSource()).isSelected());
			}
		};
		m_cbForwarding.addActionListener(actionListener);
		c2.gridx = 1;
		c2.weightx = 0;
		c2.fill = GridBagConstraints.NONE;
		c2.insets = new Insets(0, 5, 0, 0);
		p2.add(m_cbForwarding, c2);

		m_labelForwardingError = new JLabel();
		c2.gridx = 2;
		c2.weightx = 1;
		c2.fill = GridBagConstraints.NONE;
		c2.insets = new Insets(0, 15, 0, 0);
		p2.add(m_labelForwardingError, c2);

		m_labelForwarderActivity = new JLabel(JAPMessages.getString("ngActivity"));
		c2.insets = new Insets(0, 5, 0, 0);
		c2.gridx = 3;
		c2.weightx = 0;
		c2.fill = GridBagConstraints.NONE;
		p2.add(m_labelForwarderActivity, c2);
		m_progForwarderActivity = new JAPProgressBar();
		m_progForwarderActivity.setMinimum(0);
		m_progForwarderActivity.setMaximum(5);
		m_progForwarderActivity.setBorderPainted(false);
		c2.gridx = 4;
		p2.add(m_progForwarderActivity, c2);
		c1.fill = GridBagConstraints.HORIZONTAL;
		c1.weightx = 1;
		c1.gridx = 0;
		c1.gridwidth = 4;
		p.add(p2, c1);

		m_labelForwarderConnections = new JLabel(JAPMessages.getString("ngForwardedConnections"));
		c1.gridx = 0;
		c1.gridy = 1;
		c1.fill = GridBagConstraints.NONE;
		c1.weightx = 0;
		c1.gridwidth = 1;
		c1.insets = new Insets(10, 5, 0, 0);
		p.add(m_labelForwarderConnections, c1);
		JPanel spacer = new JPanel();
		Dimension spacerDimension = new Dimension(
			m_labelForwarderConnections.getFontMetrics(
				m_labelForwarderConnections.getFont()).charWidth('9') * 6, 1);
		spacer.setPreferredSize(spacerDimension);
		c1.fill = GridBagConstraints.NONE;
		c1.weightx = 0;
		c1.gridx = 1;
		c1.insets = new Insets(0, 0, 0, 0);
		p.add(spacer, c1);
		m_labelForwarderCurrentConnections = new JLabel("0");
		m_labelForwarderCurrentConnections.setHorizontalAlignment(JLabel.RIGHT);
		c1.insets = new Insets(10, 5, 0, 0);
		c1.fill = GridBagConstraints.HORIZONTAL;
		c1.weightx = 0;
		c1.gridx = 2;
		p.add(m_labelForwarderCurrentConnections, c1);
		m_labelForwarderCurrentConnectionsLabel = new JLabel(
			JAPMessages.getString("ngForwardedCurrentConnections"));
		c1.gridx = 3;
		p.add(m_labelForwarderCurrentConnectionsLabel, c1);
		m_labelForwarderAcceptedConnections = new JLabel("0");
		m_labelForwarderAcceptedConnections.setHorizontalAlignment(JLabel.RIGHT);
		c1.insets = new Insets(7, 5, 0, 0);
		c1.gridx = 2;
		c1.gridy = 2;
		p.add(m_labelForwarderAcceptedConnections, c1);
		m_labelForwarderAcceptedConnectionsLabel = new JLabel(
			JAPMessages.getString("ngForwardedAcceptedConnections"));
		c1.gridx = 3;
		p.add(m_labelForwarderAcceptedConnectionsLabel, c1);
		m_labelForwarderRejectedConnections = new JLabel("0");
		m_labelForwarderRejectedConnections.setHorizontalAlignment(JLabel.RIGHT);
		c1.gridx = 2;
		c1.gridy = 3;
		p.add(m_labelForwarderRejectedConnections, c1);
		m_labelForwarderRejectedConnectionsLabel = new JLabel(
			JAPMessages.getString("ngForwardedRejectedConnections"));
		c1.gridx = 3;
		p.add(m_labelForwarderRejectedConnectionsLabel, c1);
		m_labelForwardedTraffic = new JLabel(JAPMessages.getString("ngForwardedTraffic"));
		c1.gridx = 0;
		c1.gridy = 4;
		p.add(m_labelForwardedTraffic, c1);
		m_labelForwardedTrafficBytes = new JLabel("0");
		m_labelForwardedTrafficBytes.setHorizontalAlignment(JLabel.RIGHT);
		c1.gridx = 2;
		p.add(m_labelForwardedTrafficBytes, c1);
		m_labelForwardedTrafficBytesUnit = new JLabel(JAPMessages.getString("Byte"));
		c1.gridx = 3;
		p.add(m_labelForwardedTrafficBytesUnit, c1);
		m_labelForwarderUsedBandwidthLabel = new JLabel(JAPMessages.getString("ngForwardedUsedBandwidth"));
		c1.gridx = 0;
		c1.gridy = 5;
		p.add(m_labelForwarderUsedBandwidthLabel, c1);
		m_labelForwarderUsedBandwidth = new JLabel("0");
		m_labelForwarderUsedBandwidth.setHorizontalAlignment(JLabel.RIGHT);
		c1.gridx = 2;
		p.add(m_labelForwarderUsedBandwidth, c1);
		JLabel l = new JLabel("Byte/s");
		c1.gridx = 3;
		p.add(l, c1);

		flippingPanel.setFullPanel(p);

		//smallview
		c1 = new GridBagConstraints();
		p = new JPanel(new GridBagLayout());
		m_labelForwardingSmall = new JLabel(JAPMessages.getString("ngForwarding"));
		c1.insets = new Insets(0, 5, 0, 0);
		c1.anchor = GridBagConstraints.WEST;
		p.add(m_labelForwardingSmall, c1);
		c1.gridx = 1;
		c1.weightx = 0;
		c1.fill = GridBagConstraints.HORIZONTAL;
		m_cbForwardingSmall = new JCheckBox(JAPMessages.getString("ngForwardingOn"));
		m_cbForwardingSmall.setBorder(null);
		m_cbForwardingSmall.addActionListener(actionListener);
		p.add(m_cbForwardingSmall, c1);
		m_labelForwardingErrorSmall = new JLabel();
		c1.gridx = 2;
		c1.weightx = 1;
		c1.fill = GridBagConstraints.NONE;
		c1.insets = new Insets(0, 15, 0, 0);
		p.add(m_labelForwardingErrorSmall, c1);

		m_labelForwarderActivitySmall = new JLabel(JAPMessages.getString("ngActivity"));
		c1.gridx = 3;
		c1.weightx = 0;
		c1.fill = GridBagConstraints.NONE;
		c1.insets = new Insets(0, 5, 0, 0);
		p.add(m_labelForwarderActivitySmall, c1);
		m_progForwarderActivitySmall = new JAPProgressBar();
		m_progForwarderActivitySmall.setMinimum(0);
		m_progForwarderActivitySmall.setMaximum(5);
		m_progForwarderActivitySmall.setBorderPainted(false);
		c1.gridx = 4;
		p.add(m_progForwarderActivitySmall, c1);
		flippingPanel.setSmallPanel(p);

		Observer observer = new Observer()
		{
			public void update(Observable a_notifier, Object a_message)
			{
				try
				{
					if (a_notifier instanceof JAPRoutingServerStatisticsListener)
					{
						JAPRoutingServerStatisticsListener stats = (JAPRoutingServerStatisticsListener)
							a_notifier;
						long c = stats.getTransferedBytes();
						m_labelForwardedTrafficBytes.setText(JAPUtil.formatBytesValueWithoutUnit(c));
						m_labelForwardedTrafficBytesUnit.setText(JAPUtil.formatBytesValueOnlyUnit(c));
						m_labelForwarderAcceptedConnections.setText(Integer.toString(stats.
							getAcceptedConnections()));
						m_labelForwarderRejectedConnections.setText(Integer.toString(stats.
							getRejectedConnections()));
						m_labelForwarderCurrentConnections.setText(Integer.toString(stats.
							getCurrentlyForwardedConnections()));
						m_labelForwarderUsedBandwidth.setText(Integer.toString(stats.getCurrentBandwidthUsage()));
					}
				}
				catch (Throwable t)
				{
				}
			}
		};
		JAPModel.getInstance().getRoutingSettings().getServerStatisticsListener().addObserver(observer);
		return flippingPanel;
	}

	/**
	 * Used to disable activation on JAP
	 * Example: Activation of listener failed
	 *          --> disable activation checkboxes
	 */
	public void disableSetAnonMode()
	{
		synchronized (SYNC_ICONIFIED_VIEW)
		{
			m_ViewIconified.disableSetAnonMode();
		}
		//anonCheckBox.setEnabled(false);
		m_rbAnonOn.setEnabled(false);
		m_rbAnonOff.setEnabled(false);
	}

	/*
	 private void loadMeterIcons()
	 {
	  // Load Images for "Anonymity Meter"
	  meterIcons = new Icon[METERFNARRAY.length];
//		LogHolder.log(LogLevel.DEBUG,LogType.MISC,"JAPView:METERFNARRAY.length="+JAPConstants.METERFNARRAY.length);
	  if (!JAPModel.isSmallDisplay())
	  {
	   for (int i = 0; i < METERFNARRAY.length; i++)
	   {
		meterIcons[i] = GUIUtils.loadImageIcon(METERFNARRAY[i], true, false);
	   }
	  }
	  else
	  {
	   MediaTracker m = new MediaTracker(this);
	   for (int i = 0; i < METERFNARRAY.length; i++)
	   {
		Image tmp = GUIUtils.loadImageIcon(METERFNARRAY[i], true).getImage();
		int w = tmp.getWidth(null);
		tmp = tmp.getScaledInstance( (int) (w * 0.75), -1, Image.SCALE_SMOOTH);
		m.addImage(tmp, i);
		meterIcons[i] = new ImageIcon(tmp);
	   }
	   try
	   {
		m.waitForAll();
	   }
	   catch (Exception e)
	   {}
	  }
	 }*/

	/**Anon Level is >=0 amd <=5. If -1 no measure is available*/
	private Icon getMeterImage(int iAnonLevel)
	{
		boolean bAnonMode = m_Controller.getAnonMode();
		boolean bConnected = m_Controller.isAnonConnected();
		boolean bConnectionErrorShown = m_bShowConnecting;

		if (bAnonMode && bConnected)
		{
			//System.out.println("anon level");
			if (iAnonLevel >= 0 && iAnonLevel <= 10)
			{
				return GUIUtils.loadImageIcon(METERFNARRAY[iAnonLevel + 3], true, true);
			}
			else
			{
				return GUIUtils.loadImageIcon(METERFNARRAY[2], true, true); //No measure available
			}
		}
		else if (bAnonMode && !bConnected && bConnectionErrorShown)
		{
			//System.out.println("connection lost");
			return GUIUtils.loadImageIcon(METERFNARRAY[1], true, true); // connection lost
		}
		else
		{
			//System.out.println("AnonMode:" + bAnonMode + " " + "Connected:" + bConnected + " " +
			//			   "ShowError:" + bConnectionErrorShown);
			//System.out.println("deactivated");
			return GUIUtils.loadImageIcon(METERFNARRAY[0], true, true); // Anon deactivated
		}
	}

	/**
	 * Shows a blinking JAP icon.
	 */
	public void blink()
	{
		if (isVisible())
		{
			Thread blinkThread = new Thread(new Runnable()
			{
				public void run()
				{
					synchronized (m_progressOwnTrafficActivity)
					{
						if (m_currentChannels == 0)
						{
							return;
						}

						if (m_Controller.isAnonConnected())
						{
							m_progressOwnTrafficActivity.setValue(m_currentChannels - 1);
							m_progressOwnTrafficActivitySmall.setValue(m_currentChannels - 1);
							try
							{
								m_progressOwnTrafficActivity.wait(500);
							}
							catch (InterruptedException a_e)
							{
								// ignore
							}
						}
						if (!m_Controller.isAnonConnected())
						{
							m_currentChannels = 0;
						}
						m_progressOwnTrafficActivity.setValue(m_currentChannels);
						m_progressOwnTrafficActivitySmall.setValue(m_currentChannels);
					}
				}
			});
			blinkThread.setDaemon(true);
			blinkThread.start();
		}
	}




	public void update(Observable a_observable, final Object a_message)
	{
		Runnable run = null;
		final JAPNewView view = this;

		if (a_observable == Database.getInstance(StatusInfo.class))
		{
			Object data = ( (DatabaseMessage) a_message).getMessageData();
			if (data instanceof StatusInfo && ( (StatusInfo) data).getId().equals(
				JAPController.getInstance().getCurrentMixCascade().getId()))
			{
				updateValues(false);
			}
		}
		else if (a_observable == Database.getInstance(JAPVersionInfo.class))
		{
			updateValues(false);
		}
		else if (a_observable == Database.getInstance(BlacklistedCascadeIDEntry.class))
		{
			DatabaseMessage message = ( (DatabaseMessage) a_message);
			if (message == null)
			{
				return;
			}
			if (message.getMessageCode() != DatabaseMessage.INITIAL_OBSERVER_MESSAGE)
			{
				m_bTrustChanged = true;
				updateValues(false);
			}
		}
		else if (a_observable == Database.getInstance(MixCascade.class))
		{
			DatabaseMessage message = ( (DatabaseMessage) a_message);
			MixCascade cascade;

			if (message.getMessageData() == null || ! (message.getMessageData() instanceof MixCascade))
			{
				return;
			}

			cascade = (MixCascade) message.getMessageData();

			if (message.getMessageCode() != DatabaseMessage.ENTRY_RENEWED &&
				message.getMessageCode() != DatabaseMessage.INITIAL_OBSERVER_MESSAGE &&
				cascade.isUserDefined())
			{
				m_bTrustChanged = true;
			}

			if (message.getMessageCode() == DatabaseMessage.ENTRY_ADDED ||
				message.getMessageCode() == DatabaseMessage.ENTRY_RENEWED)
			{
				MixCascade currentCascade = JAPController.getInstance().getCurrentMixCascade();

				if (currentCascade.equals(cascade) &&
					TrustModel.getCurrentTrustModel().isTrusted(currentCascade) !=
					TrustModel.getCurrentTrustModel().isTrusted(cascade))
				{
					JAPController.getInstance().setCurrentMixCascade(cascade);
					m_bTrustChanged = true;
				}

				Database.getInstance(CascadeIDEntry.class).update(new CascadeIDEntry(cascade));

				/** @todo all databases should be synchronized... */
				/* Show the new services hint if a new service that had been hidden before becomes visible
				 * again.
				 */
				if (Database.getInstance(NewCascadeIDEntry.class).getEntryById(
					cascade.getMixIDsAsString()) != null)
				{
					m_bTrustChanged = true;
					synchronized (SYNC_NEW_SERVICES)
					{
						if (m_newServicesID < 0)
						{
							m_newServicesID = m_StatusPanel.addStatusMsg(
								JAPMessages.getString(MSG_LBL_NEW_SERVICES_FOUND),
								JOptionPane.INFORMATION_MESSAGE, false, m_listenerNewServices);
						}
					}
				}
			}
			else if (message.getMessageCode() == DatabaseMessage.ENTRY_REMOVED ||
					 message.getMessageCode() == DatabaseMessage.ALL_ENTRIES_REMOVED)
			{
				if (Database.getInstance(NewCascadeIDEntry.class).getEntryById(cascade.getMixIDsAsString()) != null)
				{
					m_bTrustChanged = true;
					/** @todo all databases should be synchronized... */
					/*
					 * Hide the new services hint if the last new service has been deleted from the database
					 * of currently registered services.
					 */
					Enumeration newEntries = Database.getInstance(NewCascadeIDEntry.class).
						getEntrySnapshotAsEnumeration();
					NewCascadeIDEntry currentEntry;
					boolean bHide = true;
					while (newEntries.hasMoreElements())
					{
						currentEntry = (NewCascadeIDEntry) newEntries.nextElement();
						if (Database.getInstance(MixCascade.class).getEntryById(
							currentEntry.getCascadeId()) != null &&
							!currentEntry.getCascadeId().equals(cascade.getId()))
						{
							bHide = false;
							break;
						}
					}
					if (bHide)
					{
						// hide the new services hint if no new service are visible any more
						synchronized (SYNC_NEW_SERVICES)
						{
							if (m_newServicesID >= 0)
							{
								m_StatusPanel.removeStatusMsg(m_newServicesID);
								m_newServicesID = -1;
							}
						}
					}
				}
			}
			updateValues(false);
		}
		else if (a_observable == Database.getInstance(CascadeIDEntry.class))
		{
			DatabaseMessage message = ( (DatabaseMessage) a_message);
			if (message.getMessageData() == null)
			{
				return;
			}

			if (message.getMessageCode() == DatabaseMessage.ENTRY_ADDED)
			{
				Database.getInstance(NewCascadeIDEntry.class).update(
					new NewCascadeIDEntry( (CascadeIDEntry) message.getMessageData()));
			}
		}
		else if (a_observable == Database.getInstance(NewCascadeIDEntry.class))
		{
			DatabaseMessage message = ( (DatabaseMessage) a_message);
			if (message.getMessageData() == null)
			{
				return;
			}
			boolean bHide = false;
			if (message.getMessageCode() == DatabaseMessage.ENTRY_ADDED ||
				message.getMessageCode() == DatabaseMessage.ENTRY_RENEWED)
			{
				synchronized (SYNC_NEW_SERVICES)
				{
					if (m_newServicesID < 0)
					{
						m_newServicesID = m_StatusPanel.addStatusMsg(
							JAPMessages.getString(MSG_LBL_NEW_SERVICES_FOUND),
							JOptionPane.INFORMATION_MESSAGE, false, m_listenerNewServices);
					}
				}
			}
			else if (message.getMessageCode() == DatabaseMessage.ENTRY_REMOVED)
			{
				Enumeration newCascades =
					Database.getInstance(NewCascadeIDEntry.class).getEntrySnapshotAsEnumeration();
				bHide = true;
				while (newCascades.hasMoreElements())
				{
					if (Database.getInstance(MixCascade.class).getEntryById(
						( (NewCascadeIDEntry) newCascades.nextElement()).getCascadeId()) != null)
					{
						bHide = false;
						break;
					}
				}
			}
			else if (message.getMessageCode() == DatabaseMessage.ALL_ENTRIES_REMOVED)
			{
				bHide = true;
			}
			if (bHide)
			{
				// hide the new services hint if no new service are visible any more
				synchronized (SYNC_NEW_SERVICES)
				{
					if (m_newServicesID >= 0)
					{
						m_StatusPanel.removeStatusMsg(m_newServicesID);
						m_newServicesID = -1;
					}
				}
			}
		}
		else if (a_message instanceof JAPModel.FontResize && a_message != null)
		{
			run = new Runnable()
			{
				public void run()
				{
					synchronized (FONT_UPDATE)
					{
						if (!m_bFontsUpdated)
						{
							m_bFontsUpdated = true;
							// preload meter icons
							for (int i = 0; i < METERFNARRAY.length; i++)
							{
								GUIUtils.loadImageIcon(METERFNARRAY[i], true, true);
							}
							SwingUtilities.updateComponentTreeUI(view);
							SwingUtilities.updateComponentTreeUI(DEFAULT_LABEL);
							updateFonts();
							onUpdateValues();

							setOptimalSize();
						}
					}
				}
			};
		}
		else if (a_observable instanceof TrustModel.InnerObservable)
		{
			m_bTrustChanged = true;
			//m_comboAnonServices.updateUI(); // immediately show blocked/unblocked cascade
			updateValues(false);
		}
		else if (a_message != null && (a_message.equals(JAPModel.CHANGED_INFOSERVICE_AUTO_UPDATE) ||
									   a_message.equals(JAPModel.CHANGED_ALLOW_INFOSERVICE_DIRECT_CONNECTION)))
		{
			run = new Runnable()
			{
				public void run()
				{
					if (!JAPController.getInstance().isShuttingDown()
						&& (JAPModel.getInstance().isInfoServiceDisabled() ||
							(!JAPModel.getInstance().isInfoServiceViaDirectConnectionAllowed() &&
							 !JAPController.getInstance().isAnonConnected())))
					{
						synchronized (SYNC_STATUS_ENABLE_IS)
						{
							if (m_enableInfoServiceID < 0)
							{
								m_enableInfoServiceID = m_StatusPanel.addStatusMsg(
									JAPMessages.getString(MSG_IS_DEACTIVATED),
									JAPDialog.MESSAGE_TYPE_WARNING, false, m_listenerEnableIS);
							}
						}
					}
					else
					{
						synchronized (SYNC_STATUS_ENABLE_IS)
						{
							if (m_enableInfoServiceID >= 0)
							{
								m_StatusPanel.removeStatusMsg(m_enableInfoServiceID);
								m_enableInfoServiceID = -1;
							}
						}
					}
				}
			};
		}
		else if (a_observable == Database.getInstance(MessageDBEntry.class))
		{
			DatabaseMessage message = ( (DatabaseMessage) a_message);
			if (message.getMessageData() == null)
			{
				return;
			}

			if (message.getMessageCode() == DatabaseMessage.INITIAL_OBSERVER_MESSAGE)
			{
				return;
			}
			final MessageDBEntry entry = (MessageDBEntry) message.getMessageData();

			if (entry.isForFreeCascadesOnly() &&
				JAPController.getInstance().getCurrentMixCascade().isPayment())
			{
				return;
			}

			synchronized (m_messageIDs)
			{
				if (entry != null &&
					(message.getMessageCode() == DatabaseMessage.ENTRY_ADDED ||
					 message.getMessageCode() == DatabaseMessage.ENTRY_RENEWED))
				{
					if (entry.isDummy())
					{
						MessageDBEntry temp = (MessageDBEntry) m_messageIDs.remove(entry.getId());
						if (temp != null)
						{
							m_StatusPanel.removeStatusMsg(temp.getExternalIdentifier());
						}
						return;
					}
					final StatusPanel.ButtonListener buttonListener =
						new StatusPanel.ButtonListener()
					{
						public void actionPerformed(final ActionEvent a_event)
						{
							int ret = JAPDialog.showConfirmDialog(JAPNewView.this,
								(a_event != null ? JAPMessages.getString(MSG_DELETE_MESSAGE_EXPLAIN) :
								 ((entry.getPopupText(JAPMessages.getLocale()) != null) ?
								  entry.getPopupText(JAPMessages.getLocale()) :
								  entry.getText(JAPMessages.getLocale()))),
								 JAPMessages.getString(JAPDialog.MSG_TITLE_INFO),
								 new JAPDialog.Options(JAPDialog.OPTION_TYPE_OK_CANCEL)
							{
								public String getCancelText()
								{
									return JAPMessages.getString(DialogContentPane.MSG_OK);
								}

								public String getYesOKText()
								{
									return JAPMessages.getString(MSG_DELETE_MESSAGE);
								}
							},
								JAPDialog.MESSAGE_TYPE_INFORMATION,
								new JAPDialog.AbstractLinkedURLAdapter()
							{
								public boolean isOnTop()
								{
									return true;
								}

								public URL getUrl()
								{
									return entry.getURL(JAPMessages.getLocale());
								}

								public String getMessage()
								{
									return JAPMessages.getString(MSG_VIEW_MESSAGE);
								}
							});
							if (ret == JAPDialog.RETURN_VALUE_OK)
							{
								synchronized (m_messageIDs)
								{
									m_StatusPanel.removeStatusMsg(entry.getExternalIdentifier());
									m_messageIDs.remove(entry.getId());
									Database.getInstance(DeletedMessageIDDBEntry.class).update(
										new DeletedMessageIDDBEntry(entry));
								}

							}
						}

						public boolean isButtonShown()
						{
							ClickedMessageIDDBEntry clickedEntry = (ClickedMessageIDDBEntry)
								Database.getInstance(ClickedMessageIDDBEntry.class).getEntryById(entry.
								getId());
							if (clickedEntry != null &&
								clickedEntry.getVersionNumber() >= entry.getVersionNumber())
							{
								// this message already has been clicked
								return true;
							}
							return false;
						}
					};

					DeletedMessageIDDBEntry deletedEntry = (DeletedMessageIDDBEntry)
						Database.getInstance(DeletedMessageIDDBEntry.class).getEntryById(entry.getId());
					if ( (deletedEntry == null || deletedEntry.getVersionNumber() < entry.getVersionNumber()) &&
						m_messageIDs.get(entry.getId()) == null && !entry.isDummy())
					{
						// this message has not been shown yet and is allowed to be shown
						int id = m_StatusPanel.addStatusMsg(entry.getText(JAPMessages.getLocale()),
							JAPDialog.MESSAGE_TYPE_INFORMATION,
							false, new ActionListener()
						{
							public void actionPerformed(ActionEvent a_event)
							{
								Database.getInstance(ClickedMessageIDDBEntry.class).update(
									new ClickedMessageIDDBEntry(entry));
								AbstractOS.getInstance().openURL(entry.getURL(JAPMessages.getLocale()));
							}
						},
							buttonListener);
						entry.setExternalIdentifier(id);
						m_messageIDs.put(entry.getId(), entry);
					}

					if (entry.isPopupShown() &&	!buttonListener.isButtonShown())
					{
						new Thread(new Runnable()
						{
							public void run()
							{

								// show popup messages for non-payment only
								Database.getInstance(ClickedMessageIDDBEntry.class).update(
									new ClickedMessageIDDBEntry(entry));
								buttonListener.actionPerformed(null);
							}
						}
						).start();
					}
				}
				else if (entry != null && message.getMessageCode() == DatabaseMessage.ENTRY_REMOVED)
				{
					MessageDBEntry temp = (MessageDBEntry) m_messageIDs.remove(entry.getId());
					if (temp != null)
					{
						m_StatusPanel.removeStatusMsg(temp.getExternalIdentifier());
					}
					return;
				}
				else if (message.getMessageCode() == DatabaseMessage.ALL_ENTRIES_REMOVED)
				{
					Enumeration enumIDs = m_messageIDs.elements();
					while (enumIDs.hasMoreElements())
					{
						m_StatusPanel.removeStatusMsg( ( ( (MessageDBEntry) enumIDs.nextElement())).
							getExternalIdentifier());
					}
					m_StatusPanel.removeAll();
				}
			}
		}
		else if (a_observable == Database.getInstance(JavaVersionDBEntry.class))
		{
			if (JAPController.getInstance().hasPortableJava())
			{
				// ignore Java update messages, if Java is portable
				return;
			}

			DatabaseMessage message = ( (DatabaseMessage) a_message);

			if (message.getMessageData() == null)
			{
				return;
			}
			if ( (message.getMessageCode() == DatabaseMessage.ENTRY_ADDED ||
				  message.getMessageCode() == DatabaseMessage.ENTRY_RENEWED))
			{
				final JavaVersionDBEntry entry = (JavaVersionDBEntry) message.getMessageData();
				if (JavaVersionDBEntry.isJavaTooOld(entry))
				{
					if (JAPModel.getInstance().isReminderForJavaUpdateActivated())
					{
						synchronized (SYNC_STATUS_UPDATE_AVAILABLE)
						{
							if (m_updateAvailableID < 0)
							{
								m_updateAvailableID = m_StatusPanel.addStatusMsg(
									JAPMessages.getString(MSG_UPDATE),
									JAPDialog.MESSAGE_TYPE_INFORMATION,
									false, m_listenerUpdate);
							}
						}
					}

					if (JAPModel.getInstance().isReminderForJavaUpdateActivated() &&
						!JAPController.getInstance().isConfigAssistantShown())
					{
						// do it as thread as otherwise this would block the database
						new Runnable()
						{
							public void run()
							{
								JAPDialog.LinkedCheckBox checkbox = new JAPDialog.LinkedCheckBox(false);
								if (JAPDialog.showYesNoDialog(JAPController.getInstance().getViewWindow(),
									JAPMessages.getString(MSG_OLD_JAVA_HINT,
									new Object[]
									{entry.getJREVersion()}), JAPMessages.getString(MSG_TITLE_OLD_JAVA),
									checkbox))
								{
									showJavaUpdateDialog(entry);
								}
								if (checkbox.getState())
								{
									JAPModel.getInstance().setReminderForJavaUpdate(false);
								}
							}
						}.run();
					}
				}
			}
		}
		else if (a_observable == JAPModel.getInstance().getRoutingSettings())
		{

			JAPRoutingMessage message = (JAPRoutingMessage)a_message;
			synchronized (JAPModel.getInstance().getRoutingSettings())
			{
				if (message != null &&
					message.getMessageCode() == JAPRoutingMessage.CLIENT_SETTINGS_CHANGED)
				{
					if (JAPModel.getInstance().getRoutingSettings().isConnectViaForwarder() &&
						m_ForwardingID < 0)
					{
						m_ForwardingID = m_StatusPanel.addStatusMsg(
							JAPMessages.getString(MSG_ANTI_CENSORSHIP),
							JAPDialog.MESSAGE_TYPE_WARNING,
							false, new ActionListener()
						{
							public void actionPerformed(ActionEvent a_event)
							{
								JAPDialog.showMessageDialog(JAPNewView.this,
									JAPMessages.getString(JAPConfNetwork.MSG_SLOW_ANTI_CENSORSHIP),
									new JAPDialog.LinkedHelpContext("forwarding_client"));
							}
						});
					}
					else if (!JAPModel.getInstance().getRoutingSettings().isConnectViaForwarder() &&
							 m_ForwardingID >= 0)
					{
						m_StatusPanel.removeStatusMsg(m_ForwardingID);
						m_ForwardingID = -1;
					}
				}
			}
		}
		if (run != null)
		{
			if (SwingUtilities.isEventDispatchThread())
			{
				run.run();
			}
			else
			{
				try
				{
					SwingUtilities.invokeAndWait(run);
				}
				catch (Exception a_e)
				{
					LogHolder.log(LogLevel.ERR, LogType.GUI, a_e);
				}
			}
		}
	}

	public void showIconifiedView()
	{
		synchronized (SYNC_ICONIFIED_VIEW)
		{
			if (m_ViewIconified != null)
			{
				m_ViewIconified.setVisible(true);
				setVisible(false);
				m_ViewIconified.toFront();
			}
		}
	}

	public void connectionEstablished(AnonServerDescription a_serverDescription)
	{
		removeStatusMsg(m_msgIDInsecure);

		if (a_serverDescription != null && a_serverDescription instanceof MixCascade)
		{
			Database.getInstance(NewCascadeIDEntry.class).remove(
						 ( (MixCascade) a_serverDescription).getMixIDsAsString());
			if (((MixCascade)a_serverDescription).getNumberOfOperators() <= 1)
			{
				m_msgIDInsecure = m_StatusPanel.addStatusMsg(JAPMessages.getString(MSG_OBSERVABLE_TITLE),
					JAPDialog.MESSAGE_TYPE_WARNING, false, new ActionListener()
				{
					public void actionPerformed(ActionEvent a_event)
					{
						JAPDialog.showWarningDialog(JAPNewView.this, JAPMessages.getString(MSG_OBSERVABLE_EXPLAIN));
						doClickOnCascadeChooser();
					}
				});
			}
		}




		new Thread(new Runnable()
		{
			public void run()
			{
				synchronized (m_connectionEstablishedSync)
				{
					m_connectionEstablishedSync.notifyAll();
				}
			}
		}).start();
	}

	public void dataChainErrorSignaled()
	{
		addStatusMsg(JAPMessages.getString(MSG_ERROR_PROXY), JAPDialog.MESSAGE_TYPE_ERROR, true);
	}

	public void dispose()
	{
		m_transferedBytesJobs.stop();
		m_packetMixedJobs.stop();
		m_flippingPanelPayment.stopUpdateQueue();
		super.dispose();
	}

	public void disconnected()
	{
		removeStatusMsg(m_msgIDInsecure);
		new Thread(new Runnable()
		{
			public void run()
			{
				synchronized (m_connectionEstablishedSync)
				{
					m_connectionEstablishedSync.notifyAll();
				}
			}
		}).start();
	}

	public void connecting(AnonServerDescription a_serverDescription)
	{
		removeStatusMsg(m_msgIDInsecure);
		showConnecting(false);
	}

	public void connectionError()
	{
		removeStatusMsg(m_msgIDInsecure);
		showConnecting(true);
	}

	public void actionPerformed(final ActionEvent event)
	{
		//		LogHolder.log(LogLevel.DEBUG,LogType.MISC,"GetEvent: "+event.getSource());
		final JAPNewView view = this;
		synchronized (SYNC_ACTION)
		{
			if (m_bActionPerformed)
			{
				return;
			}
			m_bActionPerformed = true;
		}

		Thread doIt = new Thread(new Runnable()
		{
			public void run()
			{
				Runnable run = null;

				Object source = event.getSource();
				if (source == m_bttnQuit)
				{
					JAPController.goodBye(true);
				}
				else if (source == m_bttnIconify)
				{
					showIconifiedView();
				}
				else if (source == m_bttnConf)
				{
					showConfigDialog();
					/*else if (event.getSource() == portB)
					 showConfigDialog(JAPConf.PORT_TAB);
					  else if (event.getSource() == httpB)
					 showConfigDialog(JAPConf.HTTP_TAB);
					  else if (event.getSource() == isB)
					 showConfigDialog(JAPConf.INFO_TAB);
					  else if (event.getSource() == anonB)
					 showConfigDialog(JAPConf.ANON_TAB);*/
				}
				else if (source == m_btnAbout)
				{
					//JAPController.getInstance().simuateProxyError();
					JAPController.aboutJAP();
				}
				else if (source == m_btnAssistant)
				{
					JAPController.getInstance().showInstallationAssistant();
				}
				//else if (source == m_bttnAnonConf)
				//{
				//showConfigDialog(JAPConf.ANON_TAB);
				//}
				else if (source == m_bttnHelp)
				{
					showHelpWindow();
					//else if (event.getSource() == anonCheckBox)
					//	controller.setAnonMode(anonCheckBox.isSelected());
				}
				else if (source == m_rbAnonOn || source == m_rbAnonOff)
				{
					m_bActionPerformed = false;
					run = new Runnable()
					{
						public void run()
						{
							if (m_rbAnonOn.isSelected())
							{
								m_Controller.startAnonymousMode(view);
							}
							else
							{
								m_Controller.setAnonMode(false);
							}
						}
					};

				}
				else if (source == m_cbAnonymityOn)
				{
					m_bActionPerformed = false;
					run = new Runnable()
					{
						public void run()
						{
							if (m_cbAnonymityOn.isSelected())
							{
								m_Controller.startAnonymousMode(view);
							}
							else
							{
								m_Controller.setAnonMode(false);
							}
						}
					};
				}
				else
				{
					LogHolder.log(LogLevel.DEBUG, LogType.GUI, "Event ?????: " + event.getSource());
				}
				if (run != null)
				{
					try
					{
						SwingUtilities.invokeAndWait(run);
					}
					catch (Exception a_e)
					{
						LogHolder.log(LogLevel.ERR, LogType.GUI, a_e);
					}
				}
				m_bActionPerformed = false;
			}
		});

		doIt.start();
	}

	private void showConnecting(final boolean a_bOnError)
	{
		Thread updateThread = new Thread(new Runnable()
		{
			public void run()
			{
				boolean bShowError = false;
				synchronized (m_connectionEstablishedSync)
				{
					if (!a_bOnError || JAPModel.getInstance().isAutomaticallyReconnected())
					{
						if (m_Controller.getAnonMode() && !m_Controller.isAnonConnected())
						{
							if (m_bShowConnecting)
							{
								// there already runs a wait-thread...
								return;
							}
							m_bShowConnecting = true;
							updateValues(true);
							// wait for auto-reconnect
							int msgID = addStatusMsg(JAPMessages.getString("setAnonModeSplashConnect"),
								JAPDialog.MESSAGE_TYPE_INFORMATION, false);
							/** @todo remove this cursor setting... */
							//setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
							try
							{
								m_connectionEstablishedSync.wait();
							}
							catch (InterruptedException a_e)
							{
							}
							removeStatusMsg(msgID);
							//setCursor(Cursor.getDefaultCursor());
							m_bShowConnecting = false;
						}
						updateValues(false);
					}
					else
					{
						if (!m_rbAnonOff.isSelected())
						{
							m_rbAnonOff.setSelected(true);
						}
						bShowError = true;
					}
					m_connectionEstablishedSync.notifyAll();
				}
				if (bShowError)
				{
					synchronized (SYNC_DISCONNECTED_ERROR)
					{
						if (m_bDisconnectedErrorShown)
						{
							return;
						}
						m_bDisconnectedErrorShown = true;
					}

					JAPDialog.showErrorDialog(JAPController.getInstance().getViewWindow(),
											  JAPMessages.getString(MSG_ERROR_DISCONNECTED), LogType.NET,
											  new JAPDialog.LinkedInformationAdapter()
					{
						public boolean isOnTop()
						{
							return true;
						}
					});
					synchronized (SYNC_DISCONNECTED_ERROR)
					{
						m_bDisconnectedErrorShown = false;
					}
				}
			}
		}, "Wait for connecting");
		updateThread.setDaemon(true);
		updateThread.start();
	}

	private void showHelpWindow()
	{
		JAPHelp help = JAPHelp.getInstance();
		help.getContextObj().setContext("index");
		help.loadCurrentContext();
	}

	public void setVisible(boolean a_bVisible)
	{
		boolean bShow = true;
		if (a_bVisible && !isVisible())
		{
			bShow = !JAPDll.showWindowFromTaskbar();
		}

		if (bShow)
		{
			super.setVisible(a_bVisible);
		}
	}

	public void saveWindowPositions()
	{
		//if (m_mainMovedAdapter.hasMoved())
		{
			JAPModel.getInstance().setMainWindowLocation(getLocation());
		}
		synchronized (SYNC_ICONIFIED_VIEW)
		{
			if (getViewIconified() != null && m_miniMovedAdapter != null) // && m_miniMovedAdapter.hasMoved())
			{
				JAPModel.getInstance().setIconifiedWindowLocation(getViewIconified().getLocation());
			}
		}
		if (m_dlgConfig != null) // && m_configMovedAdapter.hasMoved())
		{
			JAPModel.getInstance().setConfigWindowLocation(m_dlgConfig.getLocation());
		}
		//if (m_helpMovedAdapter.hasMoved())
		{
			JAPModel.getInstance().setHelpWindowLocation(JAPHelp.getInstance().getLocation());
		}
		JAPModel.getInstance().setHelpWindowSize(JAPHelp.getInstance().getSize());
	}

	public void showConfigDialog(String card, Object a_value)
	{
		synchronized (LOCK_CONFIG)
		{
			if (m_dlgConfig == null)
			{
				Cursor c = getCursor();
				setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
				m_dlgConfig = new JAPConf(this, m_bWithPayment);
				m_dlgConfig.addComponentListener(m_configMovedAdapter);
				setCursor(c);
			}

			m_dlgConfig.selectCard(card, a_value);
			m_dlgConfig.setVisible(true);
		}
	}

	private void setOptimalSize()
	{
		try
		{
			if (!JAPModel.isSmallDisplay()) //only do this on "real" Displays
			{
				pack(); // optimize size
				//setResizable( /*true*/true /*false*/); //2001-11-12(HF):Changed due to a Mac OS X problem during redraw of the progress bars
				//Let the main window be resizable - as you can not foresee the interest of users, their devices and preferences
				//I see virtually no reason why we should take away this freedom from the user! sk13
				setResizable(true);
			}
		}
		catch (Exception e)
		{
			LogHolder.log(LogLevel.EXCEPTION, LogType.GUI, "Hm.. Error by Pack - Has To be fixed!!");
		}
	}

	public void doClickOnCascadeChooser()
	{
		m_comboAnonServices.showPopup();
	}

	public String[] getBrowserCommand()
	{
		return m_firefoxCommand;
	}

	public void onUpdateValues()
	{
		synchronized (SYNC_ICONIFIED_VIEW)
		{
			if (m_ViewIconified != null)
			{
				m_ViewIconified.updateValues(false);
			}
		}
		//transferedBytes(0, IProxyListener.PROTOCOL_WWW);

		/*
		 * Only things that have really changed are updated!
		 */
		Enumeration entries =
			Database.getInstance(JAPVersionInfo.class).getEntrySnapshotAsEnumeration();
		String strTemp;
		JAPVersionInfo vi = null;
		while (entries.hasMoreElements())
		{
			vi = (JAPVersionInfo) entries.nextElement();
			if (JAPConstants.m_bReleasedVersion && !vi.getId().equals(JAPVersionInfo.ID_RELEASE))
			{
				vi = null;
				continue;
			}
			break;
		}

		if ( (vi != null && vi.getJapVersion() != null &&
			  vi.getJapVersion().compareTo(JAPConstants.aktVersion) > 0) ||
			 ( !JAPController.getInstance().hasPortableJava() &&
			   JAPModel.getInstance().isReminderForJavaUpdateActivated() &&
			   JavaVersionDBEntry.getNewJavaVersion() != null))
		{
			synchronized (SYNC_STATUS_UPDATE_AVAILABLE)
			{
				if (m_updateAvailableID < 0)
				{
					m_updateAvailableID = m_StatusPanel.addStatusMsg(
						JAPMessages.getString(MSG_UPDATE), JAPDialog.MESSAGE_TYPE_INFORMATION,
						false, m_listenerUpdate);
				}
			}
		}
		else
		{
			synchronized (SYNC_STATUS_UPDATE_AVAILABLE)
			{
				if (m_updateAvailableID >= 0)
				{
					m_StatusPanel.removeStatusMsg(m_updateAvailableID);
					m_updateAvailableID = -1;
				}
			}
		}

		if (!JAPController.getInstance().isShuttingDown()
			&& (JAPModel.getInstance().isInfoServiceDisabled() ||
				(!JAPModel.getInstance().isInfoServiceViaDirectConnectionAllowed() &&
				 !JAPController.getInstance().isAnonConnected())))
		{
			synchronized (SYNC_STATUS_ENABLE_IS)
			{
				if (m_enableInfoServiceID < 0)
				{
					m_enableInfoServiceID = m_StatusPanel.addStatusMsg(
						JAPMessages.getString(MSG_IS_DEACTIVATED),
						JAPDialog.MESSAGE_TYPE_WARNING, false, m_listenerEnableIS);
				}
			}
		}
		else
		{
			synchronized (SYNC_STATUS_ENABLE_IS)
			{
				if (m_enableInfoServiceID >= 0)
				{
					m_StatusPanel.removeStatusMsg(m_enableInfoServiceID);
					m_enableInfoServiceID = -1;
				}
			}
		}

		MixCascade currentMixCascade = m_Controller.getCurrentMixCascade();
		//String strCascadeName = currentMixCascade.getName();
		Hashtable hashAvailableCascades = Database.getInstance(MixCascade.class).getEntryHash();
		if (!hashAvailableCascades.containsKey(currentMixCascade))
		{
			hashAvailableCascades.put(currentMixCascade.getId(), currentMixCascade);
		}
		m_bIgnoreAnonComboEvents = true;
		if (currentMixCascade == null)
		{
			m_comboAnonServices.setNoDataAvailable();
		}
		else if (m_bTrustChanged || !equals(currentMixCascade, m_comboAnonServices.getMixCascade()))
		{
			m_bTrustChanged = false;
			boolean bShowPopup = m_comboAnonServices.isPopupVisible();
			m_comboAnonServices.removeAllItems();
			m_comboAnonServices.setMixCascade(null);
			m_comboAnonServices.setMixCascade(currentMixCascade);
			if (bShowPopup)
			{
				m_comboAnonServices.showStaticPopup();
			}
		}
		//m_comboAnonServices.setToolTipText(currentMixCascade.getName());
		m_comboAnonServices.setToolTipText(TrustModel.getCurrentTrustModel().getName());
		if (m_comboAnonServices.getSelectedItem() == null) // ||
//			!m_comboAnonServices.getSelectedItem().equals(currentMixCascade))
		{
			m_comboAnonServices.setSelectedItem(currentMixCascade);
		}
		m_bIgnoreAnonComboEvents = false;

		// Config panel
		LogHolder.log(LogLevel.DEBUG, LogType.GUI, "Start updateValues");
		// Meter panel
		try
		{
			m_rbAnonOn.setSelected(m_Controller.getAnonMode());
			m_rbAnonOff.setSelected(!m_Controller.getAnonMode());
			m_cbAnonymityOn.setSelected(m_Controller.getAnonMode());
			StatusInfo currentStatus = currentMixCascade.getCurrentStatus();
			int anonLevel = currentStatus.getAnonLevel();
			if (!GUIUtils.isLoadingImagesStopped())
			{
				/** @todo check if this helps against update freeze */
				m_labelAnonMeter.setIcon(getMeterImage(anonLevel));
			}
			Color color = Color.red;
			if (anonLevel > 7)
			{
				color = Color.green;
			}
			else if (anonLevel > 2)
			{
				color = Color.yellow;
			}
			m_progressAnonLevel.setFilledBarColor(color);
			m_progressAnonLevel.setValue(anonLevel + 1);

			String strSystrayTooltip = "JAP";
			if (m_Controller.isAnonConnected())
			{
				strSystrayTooltip += " (" + JAPMessages.getString(MSG_CONNECTED) + ")";
			}
			strSystrayTooltip += "\n" + GUIUtils.trim(currentMixCascade.getName(), 25);

			if (m_Controller.isAnonConnected())
			{
				//m_bShowConnecting = false;
				if (currentStatus.getNrOfActiveUsers() > -1)
				{
					// Nr of active users
					//if (currentStatus.getNrOfActiveUsers() > userProgressBar.getMaximum())
					//{
					//	userProgressBar.setMaximum(currentStatus.getNrOfActiveUsers());
					//}
					m_labelAnonymityUser.setText(Integer.toString(currentStatus.getNrOfActiveUsers()));
					//strSystrayTooltip += "\n" + JAPMessages.getString("ngNrOfUsers") + ": " +
					//currentStatus.getNrOfActiveUsers();
					if (anonLevel >= StatusInfo.ANON_LEVEL_MIN)
					{
						strSystrayTooltip += "\n" + JAPMessages.getString(JAPViewIconified.MSG_ANON) + ": ";
						//strSystrayTooltip += "\n" + "Test" + ": ";
						//strSystrayTooltip += "\n" + "T";
						if (anonLevel < StatusInfo.ANON_LEVEL_FAIR)
						{
							strSystrayTooltip += JAPMessages.getString(JAPViewIconified.MSG_ANON_LOW);
						}
						else if (anonLevel < StatusInfo.ANON_LEVEL_HIGH)
						{
							strSystrayTooltip += JAPMessages.getString(JAPViewIconified.MSG_ANON_FAIR);
						}
						else
						{
							strSystrayTooltip += JAPMessages.getString(JAPViewIconified.MSG_ANON_HIGH);
						}
					}
					//userProgressBar.setString(String.valueOf(currentStatus.getNrOfActiveUsers()));
					if (!isChangingTitle())
					{
						if (m_bIsIconified)
						{
							setTitle("JAP (" + Integer.toString(currentStatus.getNrOfActiveUsers()) + " " +
									 JAPMessages.getString("iconifiedviewUsers") + ")");
						}
						else
						{
							setTitle(m_Title);
						}
					}
				}
				else
				{
					strTemp = JAPMessages.getString("meterNA");
					if (m_labelAnonymityUser.getText() == null ||
						!m_labelAnonymityUser.getText().equals(strTemp))
					{
						// optimized change...
						m_labelAnonymityUser.setText(strTemp);
					}
				}
				/*int t = currentStatus.getTrafficSituation();
				if (t > -1)
				{
					//map 0..100 --> 0..5
					//0 --> 0
					//1..20 --> 1
					//21..40 --> 2
					//41..60 --> 3
					//61..80 --> 4
					//81..100 --> 5
					//m_progressAnonTraffic.setValue( (t + 19) / 20);
				}
				else
				{
					// no value from InfoService
					// TODO: labelFlags
					//m_progressAnonTraffic.setValue(0);

				}*/
			}
			else
			{
				/* we are not in anonymity mode */
				//m_progressAnonTraffic.setValue(0);
				m_labelAnonymityUser.setText("");
				m_progressAnonLevel.setValue(0);
			}
			
			int numMixes = currentMixCascade.getNumberOfMixes();
			if(currentMixCascade.getNumberOfOperators() <= 1)
			{
				numMixes = 1;
			}
			
			for(int i = 0; i < numMixes; i++)
			{
				MixInfo mixInfo = currentMixCascade.getMixInfo(i);
				
				if(mixInfo != null && mixInfo.getCertificate() != null && 
						mixInfo.getCertificate().getSubject() != null) 
				{
					String mixCountry = mixInfo.getCertificate().getSubject().getCountryCode();
					CountryMapper country = new CountryMapper(mixCountry, JAPMessages.getLocale());
					m_labelMixFlags[i].setIcon(GUIUtils.loadImageIcon("flags/" + mixCountry + ".png"));
					m_labelMixFlags[i].setToolTipText(country.toString());
					m_adapterMix[i].setMixInfo(mixInfo);
				}
				else
				{
					m_labelMixFlags[i].setIcon(null);
				}
				
				if(mixInfo != null && mixInfo.getOperatorCertificate() != null && 
						mixInfo.getOperatorCertificate().getSubject() != null) 
				{
					String operatorCountry = mixInfo.getOperatorCertificate().getSubject().getCountryCode();
					CountryMapper country = new CountryMapper(operatorCountry, JAPMessages.getLocale());
					m_labelOperatorFlags[i].setIcon(GUIUtils.loadImageIcon("flags/" + operatorCountry + ".png"));
					m_labelOperatorFlags[i].setToolTipText(country.toString());
					m_adapterOperator[i].setOperator(mixInfo.getServiceOperator());
				}
				else
				{
					m_labelOperatorFlags[i].setIcon(null);
				}
			}
			
			// clear the unused labels
			for(int i = numMixes; i < 3; i++)
			{
				m_labelMixFlags[i].setIcon(null);
				m_labelOperatorFlags[i].setIcon(null);
			}
						
			if (m_Controller.isAnonConnected())
			{
				JAPDll.setSystrayTooltip("JAP (connected)\n" + currentMixCascade.getName());
			}

			JAPDll.setSystrayTooltip(strSystrayTooltip);

			LogHolder.log(LogLevel.DEBUG, LogType.GUI, "Finished updateValues");
			boolean bForwaringServerOn = JAPModel.getInstance().getRoutingSettings().getRoutingMode() ==
				JAPRoutingSettings.ROUTING_MODE_SERVER;
			m_cbForwarding.setSelected(bForwaringServerOn);
			m_cbForwardingSmall.setSelected(bForwaringServerOn);
			Icon icon = null;
			String strError = null;
			if (bForwaringServerOn)
			{
				/* update the server state label and the reason of error, if necessary */
				int currentRegistrationState = JAPModel.getInstance().getRoutingSettings().
					getRegistrationStatusObserver().getCurrentState();
				int currentErrorCode = JAPModel.getInstance().getRoutingSettings().
					getRegistrationStatusObserver().getCurrentErrorCode();
				if (currentRegistrationState ==
					JAPRoutingRegistrationStatusObserver.STATE_NO_REGISTRATION)
				{
					if (!GUIUtils.isLoadingImagesStopped())
					{
						/** @todo check if this helps against update freeze */
						icon = GUIUtils.loadImageIcon(JAPConstants.IMAGE_WARNING, true);
					}
					if (currentErrorCode ==
						JAPRoutingRegistrationStatusObserver.ERROR_NO_KNOWN_PRIMARY_INFOSERVICES)
					{
						strError = "settingsRoutingServerStatusRegistrationErrorLabelNoKnownInfoServices";
					}
					else if (currentErrorCode ==
							 JAPRoutingRegistrationStatusObserver.ERROR_INFOSERVICE_CONNECT_ERROR)
					{
						strError = "settingsRoutingServerStatusRegistrationErrorLabelConnectionFailed";
					}
					else if (currentErrorCode ==
							 JAPRoutingRegistrationStatusObserver.ERROR_VERIFICATION_ERROR)
					{
						strError = "settingsRoutingServerStatusRegistrationErrorLabelVerificationFailed";
					}
					else if (currentErrorCode == JAPRoutingRegistrationStatusObserver.ERROR_UNKNOWN_ERROR)
					{
						strError = "settingsRoutingServerStatusRegistrationErrorLabelUnknownReason";

					}
					if (strError != null)
					{
						strError = JAPMessages.getString(strError);
					}
				}
			}
			if (!GUIUtils.isLoadingImagesStopped())
			{
				/** @todo check if this helps against update freeze */
				m_labelForwardingError.setIcon(icon);
				m_labelForwardingErrorSmall.setIcon(icon);
			}
			m_labelForwardingError.setToolTipText(strError);
			m_labelForwardingErrorSmall.setToolTipText(strError);

			/* if the forwarding client is running, it should not be possible to start the forwarding
			 * server, also it should not be possible to change the selected mixcascade
			 */
			m_cbForwarding.setEnabled(!JAPModel.getInstance().getRoutingSettings().isConnectViaForwarder());
			m_cbForwardingSmall.setEnabled(!JAPModel.getInstance().getRoutingSettings().
										   isConnectViaForwarder());
			m_comboAnonServices.setEnabled(!JAPModel.getInstance().getRoutingSettings().
										   isConnectViaForwarder());
			validate();
		}
		catch (Throwable t)
		{
			LogHolder.log(LogLevel.EMERG, LogType.GUI, t);
		}
	}

	public JAPViewIconified getViewIconified()
	{
		return m_ViewIconified;
	}

	public void registerViewIconified(JAPViewIconified v)
	{
		synchronized (SYNC_ICONIFIED_VIEW)
		{
			if (m_ViewIconified != null)
			{
				m_ViewIconified.removeComponentListener(m_miniMovedAdapter);
			}
			m_ViewIconified = v;
			m_miniMovedAdapter = new ComponentMovedAdapter();
			m_ViewIconified.addComponentListener(m_miniMovedAdapter);
		}
	}

	private int m_currentChannels = 0;

	public void channelsChanged(int c)
	{
		// Nr of Channels
		//int c=controller.getNrOfChannels();
		synchronized (m_progressOwnTrafficActivity)
		{
			m_currentChannels = c;
			c = Math.min(c, m_progressOwnTrafficActivity.getMaximum());
			m_progressOwnTrafficActivity.setValue(c);
			m_progressOwnTrafficActivitySmall.setValue(c);
		}
//			ownTrafficChannelsProgressBar.setString(String.valueOf(c));
	}

	public void packetMixed(final long a_totalBytes)
	{
		m_packetMixedJobs.addJob(new JobQueue.Job(true)
		{
			public void runJob()
			{
				synchronized (SYNC_ICONIFIED_VIEW)
				{
					if (m_ViewIconified != null)
					{
						m_ViewIconified.packetMixed(a_totalBytes);
					}
				}
				Runnable transferedBytesThread = new Runnable()
				{
					public void run()
					{
						String unit;
						String s;

						//unit = JAPUtil.formatBytesValueOnlyUnit(a_totalBytes, JAPUtil.MAX_FORMAT_KBYTES);
						unit = JAPUtil.formatBytesValueOnlyUnit(a_totalBytes);
						m_labelOwnTrafficUnit.setText(unit);
						m_labelOwnTrafficUnit.revalidate();
						m_labelOwnTrafficUnitSmall.setText(unit);
						m_labelOwnTrafficUnitSmall.revalidate();
						//s = JAPUtil.formatBytesValueWithoutUnit(a_totalBytes, JAPUtil.MAX_FORMAT_KBYTES);
						s = JAPUtil.formatBytesValueWithoutUnit(a_totalBytes);
						m_labelOwnTrafficBytes.setText(s);
						m_labelOwnTrafficBytes.revalidate();
						m_labelOwnTrafficBytesSmall.setText(s);
						m_labelOwnTrafficBytesSmall.revalidate();
					}
				};

				try
				{
					SwingUtilities.invokeAndWait(transferedBytesThread);
				}
				catch (InvocationTargetException ex)
				{
				}
				catch (InterruptedException ex)
				{
				}
				transferedBytesThread = null;
			}
		});
	}

	public void transferedBytes(final long c, final int protocolType)
	{
		// Nr of Bytes transmitted anonymously
		if (protocolType == IProxyListener.PROTOCOL_WWW)
		{
			//m_lTrafficWWW = JAPModel.getInstance().getMixedBytes();
			m_lTrafficWWW = c;
		}
		else if (protocolType == IProxyListener.PROTOCOL_OTHER)
		{
			m_lTrafficOther = c;
		}

		m_transferedBytesJobs.addJob(new JobQueue.Job()
		{
			public void runJob()
			{
				if (c > 0 && m_ViewIconified != null)
				{
					m_ViewIconified.blink();
				}
				blink();

				Runnable transferedBytesThread = new Runnable()
				{
					public void run()
					{
						String unit = JAPUtil.formatBytesValueOnlyUnit(m_lTrafficWWW);
						m_labelOwnTrafficUnitWWW.setText(unit);
						m_labelOwnTrafficUnitWWW.revalidate();
						String s = JAPUtil.formatBytesValueWithoutUnit(m_lTrafficWWW);
						m_labelOwnTrafficBytesWWW.setText(s);
						m_labelOwnTrafficBytesWWW.revalidate();
						unit = JAPUtil.formatBytesValueOnlyUnit(m_lTrafficOther);
						m_labelOwnTrafficUnitOther.setText(unit);
						m_labelOwnTrafficUnitOther.revalidate();
						s = JAPUtil.formatBytesValueWithoutUnit(m_lTrafficOther);
						m_labelOwnTrafficBytesOther.setText(s);
						m_labelOwnTrafficBytesOther.revalidate();
						JAPDll.onTraffic();
					}
				};

				try
				{
					SwingUtilities.invokeAndWait(transferedBytesThread);
				}
				catch (InvocationTargetException ex)
				{
				}
				catch (InterruptedException ex)
				{
				}
				transferedBytesThread = null;
			}
		});
	}

	public Dimension getPreferredSize()
	{
		Dimension d = super.getPreferredSize();
		/** todo why was this needed? */
		//d.width = m_iPreferredWidth;
		return d;
	}

	public int addStatusMsg(String msg, int type, boolean bAutoRemove)
	{
		return m_StatusPanel.addStatusMsg(msg, type, bAutoRemove);
	}

	public void removeStatusMsg(int id)
	{
		m_StatusPanel.removeStatusMsg(id);
	}

	private void showJavaUpdateDialog(JavaVersionDBEntry a_entry)
	{
		Object[] args = new Object[5];
		args[0] = JavaVersionDBEntry.CURRENT_JAVA_VERSION;
		args[1] = JavaVersionDBEntry.CURRENT_JAVA_VENDOR;
		args[2] = a_entry.getJREVersion();
		args[3] = a_entry.getVendorLongName();
		args[4] = a_entry.getVendor();
		// Uninstall old Java!! http://sunsolve.sun.com/search/document.do?assetkey=1-26-102557-1
		JAPDialog.showMessageDialog(JAPController.getInstance().getViewWindow(),
									JAPMessages.getString(MSG_OLD_JAVA, args) +
									(a_entry.getJREVersionName() == null ? "" : "<br>" + a_entry.getJREVersionName()),
									JAPMessages.getString(MSG_TITLE_OLD_JAVA),
									AbstractOS.getInstance().createURLLink(
										a_entry.getDownloadURL(), null, "updateJava"));
	}

	private synchronized void fetchMixCascadesAsync(final boolean bShowError)
	{
		m_bttnReload.setEnabled(false);
		Runnable doFetchMixCascades = new Runnable()
		{
			public void run()
			{
				//setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
				m_Controller.updateInfoServices(!bShowError);
				m_Controller.fetchMixCascades(bShowError, JAPController.getInstance().getViewWindow(),
											  !bShowError);
				//setCursor(Cursor.getDefaultCursor());
				SwingUtilities.invokeLater(new Runnable()
				{
					public void run()
					{
						m_bttnReload.setEnabled(true);
					}
				});
			}
		};
		Thread t = new Thread(doFetchMixCascades, "DoFetchMixCascades");
		t.setDaemon(true);
		t.start();
	}

	private void updateFonts()
	{
		Font labelFont = DEFAULT_LABEL.getFont();
		m_labelVersion.setFont(new Font(labelFont.getName(), labelFont.getStyle(),
										( (int) (labelFont.getSize() * 0.8))));
		/*
		   m_labelUpdate.setFont(new Font(labelFont.getName(), labelFont.getStyle(),
				  ( (int) (labelFont.getSize() * 0.8))));
		 */
		m_bttnIconify.setIcon(GUIUtils.loadImageIcon(IMG_ICONIFY, true));
		//m_bttnHelp.setIcon(GUIUtils.loadImageIcon(JAPHelp.IMG_HELP, true));
		m_btnAbout.setIcon(GUIUtils.loadImageIcon(IMG_ABOUT, true));
	}

	private static boolean equals(MixCascade a_one, MixCascade a_two)
	{
		if ( (a_one == null && a_two != null) || (a_one != null && a_two == null) ||
			(a_one != null && (!a_one.equals(a_two) || a_one.isPayment() != a_two.isPayment() ||
							   !a_one.getName().equals(a_two.getName()))))
		{
			return false;
		}

		return true;
	}

	//stores the id's of messages, key: String message, value: Integer id
	//this might seem backwards, but the String is what we get as message to remove, and the id is what the statuspanel needs to remove the message
	private Hashtable m_messagesShown = new Hashtable();

	public void messageReceived(PayMessage completeMessage)
	{
		final URL messageLink = completeMessage.getMessageLink();
		String messageText = completeMessage.getMessageText();
		String message = completeMessage.getShortMessage();
		int messageId = 0; //so store the return value, so we can find the message again to remove it

	    //check if a valid link was given

		boolean gotLink = messageLink != null ? true : false;

		boolean gotText = (messageText != null && !messageText.equals("") ) ? true : false;
		ActionListener messageClicked;

	    //neither link nor longer text -> just show the message, with nothing happening when the user clicks on it
	    if (!gotLink && !gotText )
		{
			messageClicked = null;
	    }
		// link given, but no text -> open link immediately on click
		else if (gotLink && !gotText )
		{
			messageClicked = new ActionListener()
			{
				public void actionPerformed(ActionEvent e)
				{
					AbstractOS.getInstance().openURL(messageLink);
				}
			};

		}
		//text given -> open dialog box on click
		else
		{
			final String messageString = message;
			final String messageTextString = messageText;
			final JAPNewView parentWindow = this;

			if (gotLink) //also got link -> show dialog with button opening the link
			{
				final JAPDialog.LinkedInformationAdapter infoLink = new LinkedInformationAdapter()
				{
					public void clicked (boolean state)
					{
						AbstractOS.getInstance().openURL(messageLink);
					}

					public String getMessage()
					{
						String displayedLink = messageLink.toString();
						displayedLink = Util.replaceAll(displayedLink,"mailto:","Email:");
						displayedLink = Util.replaceAll(displayedLink,"http://","Link:");
						return displayedLink;
					}

					public int getType()
					{
						return JAPDialog.ILinkedInformation.TYPE_SELECTABLE_LINK;
					}
				};
				final JAPDialog.Options dialogOptions = new Options(JAPDialog.OPTION_TYPE_OK_CANCEL)
				{
					public String getYesOKText()
					{
						return JAPMessages.getString("bttnOk");
					}
					public String getCancelText()
					{
						return JAPMessages.getString("bttnCancel");
					}
				};

	            messageClicked = new ActionListener()
				{
					public void actionPerformed(ActionEvent e)
					{
						int buttonClicked = JAPDialog.showConfirmDialog(parentWindow,messageTextString,messageString,dialogOptions,JOptionPane.INFORMATION_MESSAGE,infoLink);
						//react to oK buton clicked with opening the link
						if (buttonClicked == JOptionPane.OK_OPTION)
						{
							AbstractOS.getInstance().openURL(messageLink);
						}

					}
				};


			}
			else //just long text -> simple message dialog
			{
				messageClicked = new ActionListener()
				{
					public void actionPerformed(ActionEvent e)
					{
						JAPDialog.showMessageDialog(parentWindow,messageTextString,messageString);
					}
				};

			}
		}

		messageId = m_StatusPanel.addStatusMsg(message, JOptionPane.INFORMATION_MESSAGE,false,messageClicked);
		m_messagesShown.put(message, new Integer(messageId));
	}

	public void messageRemoved(PayMessage message)
	{
		String messageString = message.getShortMessage();
		Integer messageId = (Integer) m_messagesShown.get(messageString);
		if (messageId == null)
		{
			//should not happen, we can't remove a message without an id
			LogHolder.log(LogLevel.DEBUG, LogType.PAY, "Tried to remove a message, but failed, since no id exists, message is: " + message);
		}
		else
		{
			m_StatusPanel.removeStatusMsg(messageId.intValue());
		}
	}

	public void showChooseFirefoxPathDialog()
	{
		if(JAPDialog.showYesNoDialog(this, JAPMessages.getString(MSG_EXPLAIN_NO_FIREFOX_FOUND)))
		{
			JFileChooser chooser = new JFileChooser();
			if(chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION)
			{
				File f = chooser.getSelectedFile();
				if(m_firefoxCommand == null)
				{
					m_firefoxCommand = new String[1];
				}
				m_firefoxCommand[0] = f.getAbsolutePath();
				if(f != null)
				{
					m_Controller.startPortableFirefox(m_firefoxCommand);
				}

			}
		}
	}

	private final class ComponentMovedAdapter extends ComponentAdapter
	{
		private boolean m_bMoved = false;

		public void componentMoved(ComponentEvent a_event)
		{
			m_bMoved = true;
		}

		public boolean hasMoved()
		{
			return m_bMoved;
		}
	}
	
	private final class MixMouseAdapter extends MouseAdapter
	{
		private MixInfo m_mixInfo;
		private int m_mixType;
		
		public MixMouseAdapter(MixInfo a_mixInfo, int a_mixType)
		{
			m_mixInfo = a_mixInfo;
			m_mixType = a_mixType;
		}
		
		public void mouseClicked(MouseEvent a_event)
		{
			MixDetailsDialog dialog = new MixDetailsDialog(JAPNewView.this, m_mixInfo, m_mixType);
			dialog.pack();
			dialog.setVisible(true);
		}
		
		public void setMixInfo(MixInfo a_mixInfo)
		{
			m_mixInfo = a_mixInfo;
		}
	}
	
	private final class OperatorMouseAdapter extends MouseAdapter
	{
		private ServiceOperator m_operator;
		
		public OperatorMouseAdapter(ServiceOperator a_operator)
		{
			m_operator = a_operator;
		}
		
		public void mouseClicked(MouseEvent a_event)
		{
			OperatorDetailsDialog dialog = new OperatorDetailsDialog(JAPNewView.this, m_operator);
			dialog.pack();
			dialog.setVisible(true);
		}
		
		public void setOperator(ServiceOperator a_operator)
		{
			m_operator = a_operator;
		}
	}
}
