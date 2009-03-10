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
package jap;

import java.lang.reflect.InvocationTargetException;
import java.text.NumberFormat;

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
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JWindow;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.border.LineBorder;

import anon.infoservice.MixCascade;
import anon.infoservice.StatusInfo;
import gui.GUIUtils;
import gui.dialog.JAPDialog;
import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;
import anon.infoservice.JavaVersionDBEntry;
import anon.util.JAPMessages;

final public class JAPViewIconified extends JWindow implements ActionListener
{
	/**
	 * serial version UID
	 */
	private static final long serialVersionUID = 1L;

	public static final String XML_LOCATION = "IconifiedLocation";

	public static final String MSG_ANON_LOW = JAPViewIconified.class.getName() + "_anonLow";
	public static final String MSG_ANON_FAIR = JAPViewIconified.class.getName() + "_anonFair";
	public static final String MSG_ANON_HIGH = JAPViewIconified.class.getName() + "_anonHigh";
	public static final String MSG_ANON = JAPViewIconified.class.getName() + "_anon";
	private static final String MSG_TT_SWITCH_ANONYMITY =
		JAPViewIconified.class.getName() + "_ttSwitchAnonymity";


	private static final String STR_HIDDEN_WINDOW = Double.toString(Math.random());
	private static JFrame m_frameParent;

	private JAPController m_Controller;
	private AbstractJAPMainView m_mainView;
	private JLabel m_labelBytes, m_labelUsers, m_labelTraffic, m_labelAnon;
	private JLabel m_lblJAPIcon;
	private JAPDialog ms_popupWindow;
	private JLabel m_lblBytes;
	private Font m_fontDlg;
	private NumberFormat m_NumberFormat;
	private boolean m_anonModeDisabled = false;
	private Object SYNC_CURSOR = new Object();
	
	//private Thread m_threadCheckVisibility;

	private GUIUtils.WindowDocker m_docker;

	private static JFrame getParentFrame()
	{
		if (m_frameParent == null)
		{
			m_frameParent = new JFrame(STR_HIDDEN_WINDOW);
		}
		return m_frameParent;
	}

	private Runnable m_runnableValueUpdate;

	public JAPViewIconified(AbstractJAPMainView a_mainView)
	{
		super(getParentFrame());
		m_fontDlg = new Font("Sans", Font.BOLD, 11);
		setName(STR_HIDDEN_WINDOW);
		m_mainView = a_mainView;
		if (m_frameParent != null)
		{
			m_frameParent.setIconImage(m_mainView.getIconImage());
		}
		LogHolder.log(LogLevel.INFO, LogType.MISC, "Initializing...");
		m_Controller = JAPController.getInstance();
		m_NumberFormat = NumberFormat.getInstance();
		m_runnableValueUpdate = new Runnable()
		{
			public void run()
			{
				updateValues1();
			}
		};
		init();
		/*
		m_threadCheckVisibility = new Thread(new Runnable()
		{
			public void run()
			{
				while(true)
				{
					try 
					{
						Thread.sleep(1000);
						System.out.println("OnTop: " + GUIUtils.isAlwaysOnTop(JAPViewIconified.this) + " Showing: " + JAPViewIconified.this.isShowing() + " Focusable: " + JAPViewIconified.this.isFocusable() + " Valid: " + JAPViewIconified.this.isValid());
					}
					catch (InterruptedException e) 
					{
						break;
					}
				}
				//
			}
		});
		m_threadCheckVisibility.start();*/
	}

	private void init()
	{
		GridBagLayout la = new GridBagLayout();
		GridBagConstraints c = new GridBagConstraints();
		JPanel pTop = new JPanel(la);
		pTop.setOpaque(false);
		m_lblBytes = new JLabel(JAPMessages.getString("iconifiedviewBytes") + ": ", JLabel.RIGHT);
		m_lblBytes.setFont(m_fontDlg);
		c.gridx = 0;
		c.gridy = 0;
		c.fill = GridBagConstraints.BOTH;
		c.weightx = 0;
		c.insets = new Insets(3, 3, 0, 0);
		c.anchor = GridBagConstraints.NORTHWEST;
		la.setConstraints(m_lblBytes, c);
		pTop.add(m_lblBytes);
		c.weightx = 1;
		m_labelBytes = new JLabel("000000,0", JLabel.LEFT);
		//m_labelBytes.setForeground(Color.red);
		m_labelBytes.setFont(m_fontDlg);
		c.gridx = 1;
		c.weightx = 0;
		la.setConstraints(m_labelBytes, c);
		pTop.add(m_labelBytes);

		JLabel x3 = new JLabel(JAPMessages.getString("iconifiedviewUsers") + ": ", JLabel.RIGHT);
		x3.setFont(m_fontDlg);
		c.gridx = 0;
		c.gridy = 1;
		la.setConstraints(x3, c);
		//pTop.add(x3);

		m_labelUsers = new JLabel("", JLabel.LEFT);
		//m_labelUsers.setForeground(Color.red);
		m_labelUsers.setFont(m_fontDlg);
		c.gridx = 1;
		la.setConstraints(m_labelUsers, c);
		//pTop.add(m_labelUsers);

		JLabel x4 = new JLabel(JAPMessages.getString("iconifiedviewTraffic") + ": ", JLabel.RIGHT);
		x4.setFont(m_fontDlg);
		c.gridy = 2;
		c.gridx = 0;
		la.setConstraints(x4, c);
		//pTop.add(x4);
		m_labelTraffic = new JLabel("", JLabel.LEFT);
		//m_labelTraffic.setForeground(Color.red);
		m_labelTraffic.setFont(m_fontDlg);
		c.gridx = 1;
		la.setConstraints(m_labelTraffic, c);
		//pTop.add(m_labelTraffic);

		JLabel tmpLabel = new JLabel(JAPMessages.getString(MSG_ANON) + ": ", JLabel.RIGHT);
		tmpLabel.setFont(m_fontDlg);
		c.gridy++;
		c.gridx = 0;
		pTop.add(tmpLabel, c);
		int anonLength = 0;
		anonLength = Math.max(anonLength, JAPMessages.getString(MSG_ANON_LOW).length());
		anonLength = Math.max(anonLength, JAPMessages.getString(MSG_ANON_FAIR).length());
		anonLength = Math.max(anonLength, JAPMessages.getString(MSG_ANON_HIGH).length());
		char[] charLength = new char[anonLength];
		for (int i = 0; i < charLength.length; i++)
		{
			charLength[i] = 'A';
		}
		m_labelAnon = new JLabel(new String(charLength), JLabel.LEFT);
		m_labelAnon.setFont(m_fontDlg);
		c.gridx++;
		pTop.add(m_labelAnon, c);

		JButton bttn = new JButton(GUIUtils.loadImageIcon(JAPConstants.ENLARGEYICONFN, true, false));
		bttn.setOpaque(false);
		bttn.addActionListener(this);
		bttn.setToolTipText(JAPMessages.getString("enlargeWindow"));
		JAPUtil.setMnemonic(bttn, JAPMessages.getString("iconifyButtonMn"));

		/*JLabel l1 = new JLabel(JAPUtil.loadImageIcon(JAPConstants.JAPEYEFN, true));
		   JPanel co = new JPanel();
		   co.add(pTop);
		   JPanel pBottom = new JPanel(new BorderLayout());
		   pBottom.setBackground(new Color(204, 204, 204));
		   pBottom.add(l1, BorderLayout.CENTER);
		   co.add(pBottom);
		   OverlayLayout lo = new OverlayLayout(co);
		   co.setLayout(lo);
		 */
		JPanel p = new JPanel(new BorderLayout());
		p.setBorder(new LineBorder(Color.black, 1));
		p.add(pTop, BorderLayout.CENTER);
		JPanel p2 = new JPanel(new BorderLayout());
		//p2.setBackground(new Color(204, 204, 204));
		m_lblJAPIcon = new JLabel(GUIUtils.loadImageIcon(JAPViewIconified.class.getName() + "_icon16discon.gif", true, false));
		//m_lblJAPIcon.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		m_lblJAPIcon.setToolTipText(JAPMessages.getString(MSG_TT_SWITCH_ANONYMITY));
		m_lblJAPIcon.addMouseListener(new MouseAdapter()
		{
			public void mouseClicked(MouseEvent a_event)
			{
				if (!m_anonModeDisabled && GUIUtils.isMouseButton(a_event, MouseEvent.BUTTON1_MASK))
				{
					m_lblJAPIcon.setBorder(BorderFactory.createLoweredBevelBorder());
					m_Controller.setAnonMode(!m_Controller.getAnonMode());
				}
			}
			public void mouseEntered(MouseEvent a_event)
			{
				synchronized (SYNC_CURSOR)
				{
					if (!m_anonModeDisabled)
					{
						// do it that 'complicated' as it would otherwise not work on JRE 1.5...
						setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
						getRootPane().setToolTipText(JAPMessages.getString(MSG_TT_SWITCH_ANONYMITY));
					}
				}
			}

			public void mouseExited(MouseEvent e)
			{
				synchronized (SYNC_CURSOR)
				{
					setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
					getRootPane().setToolTipText(null);
				}
			}
		});
		p2.add(m_lblJAPIcon, BorderLayout.SOUTH);
		p2.add(bttn, BorderLayout.NORTH);
		p.add(p2, BorderLayout.EAST);

		p.addMouseListener(new MouseAdapter()
		{
			public void mouseClicked(MouseEvent a_event)
			{
				if (SwingUtilities.isRightMouseButton(a_event) || a_event.isPopupTrigger())
				{
					final SystrayPopupMenu popup = new SystrayPopupMenu(
									   new SystrayPopupMenu.MainWindowListener()
					{
						public void onShowMainWindow()
						{
							switchBackToMainView();
						}


						public void onShowSettings(String card, Object a_value)
						{
							m_mainView.showConfigDialog(card, a_value);
						}
						public void onShowHelp()
						{
							switchBackToMainView();
						}
					});
					/*
						  popup.registerExitHandler(new PopupMenu.ExitHandler()
						  {
					 public void exited()
					 {
					  popup.dispose();
					 }
						  });*/

					 /** @todo Find a better way to distinguish JDK version compatibility!  */
					if (JavaVersionDBEntry.CURRENT_JAVA_VENDOR.toLowerCase().indexOf("sun") >= 0 &&
						 (JavaVersionDBEntry.CURRENT_JAVA_VERSION.compareTo("1.6.0_02") >= 0))
					{
						if (ms_popupWindow == null)
						{
							//ms_popupWindow = new JWindow(new Frame(STR_HIDDEN_WINDOW));
							// needed for JDK > 1.6.0_02 ... (J)Window is no more compatible with PopupMenus!
							ms_popupWindow = new JAPDialog(JAPViewIconified.this, STR_HIDDEN_WINDOW, false);
							ms_popupWindow.setName(STR_HIDDEN_WINDOW);
							ms_popupWindow.pack();
							ms_popupWindow.setLocation(20000, 20000); // needed for JDK > 1.6.0_02 to hide dialog
						}

						ms_popupWindow.setVisible(true);

						popup.show(ms_popupWindow.getContentPane(), JAPViewIconified.this,
								   new Point(a_event.getX() + JAPViewIconified.this.getLocation().x,
											 a_event.getY() + JAPViewIconified.this.getLocation().y));

					}
					else
					{
						popup.show(JAPViewIconified.this,
								   new Point(a_event.getX() + JAPViewIconified.this.getLocation().x,
											 a_event.getY() + JAPViewIconified.this.getLocation().y));
					}



				}
				else
				{
					if (a_event.getClickCount() > 1)
					{
						switchBackToMainView();
					}
				}
			}
		});
		setContentPane(p);

		m_docker = new GUIUtils.WindowDocker(p);

		pack();
		// fix for MacOS; sometimes pack increases the size to several meters...
		GUIUtils.Screen currentScreen = GUIUtils.getCurrentScreen(this);
		if (getSize().width > currentScreen.getWidth() || getSize().height > currentScreen.getHeight())
		{
			LogHolder.log(LogLevel.ERR, LogType.GUI, "Packed iconified view with illegal size! " +
						  "Width:" + getSize().width + " Height:" + getSize().height +
						  "\nSetting defaults...");
			if (JAPModel.getInstance().getIconifiedSize() != null &&
				JAPModel.getInstance().getIconifiedSize().width > 0 &&
				JAPModel.getInstance().getIconifiedSize().height > 0)
			{
				setSize(JAPModel.getInstance().getIconifiedSize());
			}
			else
			{
				// default size for MacOS
				setSize(new Dimension(151, 85));
			}
		}
		else
		{
			JAPModel.getInstance().setIconifiedSize(getSize());
		}

		GUIUtils.moveToUpRightCorner(this);
		GUIUtils.restoreLocation(this, JAPModel.getInstance().getIconifiedWindowLocation());
		//m_labelBytes.setText(JAPMessages.getString("iconifiedViewZero"));
		m_labelBytes.setText(JAPUtil.formatBytesValueWithoutUnit(0));
		m_lblBytes.setText(JAPUtil.formatBytesValueOnlyUnit(0) + ": ");
		m_labelUsers.setText(JAPMessages.getString("iconifiedViewNA"));
		m_labelTraffic.setText(JAPMessages.getString("iconifiedViewNA"));
		m_labelAnon.setText(JAPMessages.getString("iconifiedViewNA"));
		//JAPDll.setWindowOnTop(this,STR_HIDDEN_WINDOW, true);
		//GUIUtils.setAlwaysOnTop(this, true);
	}

	public void setVisible(boolean a_bVisible)
	{
		if (a_bVisible)
		{
			GUIUtils.setAlwaysOnTop(this, JAPModel.getInstance().isMiniViewOnTop());
		}

		JAPController.getInstance().switchViewWindow(!a_bVisible);
		super.setVisible(a_bVisible);
	}

	public void switchBackToMainView()
	{
		if (m_mainView == null || (!isVisible() && m_mainView.isVisible()))
		{
			return;
		}
		m_mainView.setVisible(true);
		setVisible(false);
		m_mainView.toFront();
	}

	public void actionPerformed(ActionEvent event)
	{
		switchBackToMainView();
	}

	private void updateValues1()
	{
		synchronized (m_runnableValueUpdate)
		{
			try
			{

				//System.out.println(getLocationOnScreen());
				/*
				if (isVisible())
				{
					GUIUtils.setAlwaysOnTop(this, JAPModel.getInstance().isMiniViewOnTop());
				}*/


				if (m_Controller.isAnonConnected())  //m_Controller.getAnonMode())
				{
					//transferedBytes(0, IProxyListener.PROTOCOL_WWW);
					MixCascade currentMixCascade = m_Controller.getCurrentMixCascade();
					StatusInfo currentStatus = currentMixCascade.getCurrentStatus();
					int anonLevel = currentStatus.getAnonLevel();
					m_labelAnon.setText(currentMixCascade.getDistribution() + "," + 
							(anonLevel < StatusInfo.ANON_LEVEL_MIN ? "?" : "" + anonLevel) + " / " + 
							MixCascade.DISTRIBUTION_MAX + "," + StatusInfo.ANON_LEVEL_MAX);

					if (currentStatus.getNrOfActiveUsers() != -1)
					{
						m_labelUsers.setText(m_NumberFormat.format(currentStatus.getNrOfActiveUsers()));
					}
					else
					{
						m_labelUsers.setText(JAPMessages.getString("iconifiedViewNA"));
					}
					int t = currentStatus.getTrafficSituation();
					if (t > -1)
					{
						if (t < 30)
						{
							m_labelTraffic.setText(JAPMessages.getString("iconifiedViewMeterTrafficLow"));
						}
						else
						{
							if (t < 60)
							{
								m_labelTraffic.setText(
									JAPMessages.getString("iconifiedViewMeterTrafficMedium"));
							}
							else
							{
								m_labelTraffic.setText(JAPMessages.getString("iconifiedViewMeterTrafficHigh"));
							}
						}
					}
					else
					{
						m_labelTraffic.setText(JAPMessages.getString("iconifiedViewNA"));
					}
					synchronized(m_lblJAPIcon)
					{
						String iconPath;
						if (JAPModel.getInstance().getProgramName().equals(JAPConstants.PROGRAM_NAME_JONDO))
						{
							iconPath = JAPConstants.ICON_JONDO;
						}
						else
						{
							iconPath = JAPConstants.IICON16FN;
						}
						m_lblJAPIcon.setIcon(GUIUtils.loadImageIcon(iconPath, true, false));
					}
				}
				else
				{
					/* not in anonymity mode */
					//m_lblBytes.setText(JAPMessages.getString("iconifiedviewBytes") + ": ");
					//m_labelBytes.setText(JAPMessages.getString("iconifiedViewNA"));
					m_labelUsers.setText(JAPMessages.getString("iconifiedViewNA"));
					m_labelTraffic.setText(JAPMessages.getString("iconifiedViewNA"));
					m_labelAnon.setText(JAPMessages.getString("iconifiedViewNA"));
					synchronized(m_lblJAPIcon)
					{
						m_lblJAPIcon.setIcon(GUIUtils.loadImageIcon(JAPViewIconified.class.getName() + "_icon16discon.gif", true, false));
					}
				}
				setButtonBorder();

			}
			catch (Throwable t)
			{
			}
		}
	}

	public void dispose()
	{
		m_docker.finalize();
		super.dispose();
	}

	public void disableSetAnonMode()
	{
		m_anonModeDisabled = true;
	}

	public void updateValues(boolean bSync)
	{
		if (SwingUtilities.isEventDispatchThread())
		{
			m_runnableValueUpdate.run();
		}
		else
		{
			try
			{
				if (bSync)
				{
					SwingUtilities.invokeAndWait(m_runnableValueUpdate);
				}
			}
			catch (InvocationTargetException ex)
			{
			}
			catch (InterruptedException ex)
			{
			}
			SwingUtilities.invokeLater(m_runnableValueUpdate);
		}
	}

	public void channelsChanged(int c)
	{
	}


	public void packetMixed(final long a_totalBytes)
	{
		Runnable updateThread = new Runnable()
		{
			public void run()
			{
				//if (JAPController.getInstance().isAnonConnected())
				{
					m_lblBytes.setText(JAPUtil.formatBytesValueOnlyUnit(a_totalBytes) + ":");
					m_labelBytes.setText(JAPUtil.formatBytesValueWithoutUnit(a_totalBytes));
				}
				/*else
				{
					m_lblBytes.setText(JAPMessages.getString("iconifiedviewBytes") + ": ");
					m_labelBytes.setText(JAPMessages.getString("iconifiedViewNA"));
				}*/
			}
		};

		if (SwingUtilities.isEventDispatchThread())
		{
			updateThread.run();
		}
		else
		{
			SwingUtilities.invokeLater(updateThread);
		}

	}

	/*
	public void transferedBytes(final long b, final int protocolType)
	{
		if (protocolType == IProxyListener.PROTOCOL_WWW)
		{
			//m_lTrafficWWW = JAPModel.getInstance().getMixedBytes();
			m_lTrafficWWW = b;
		}
		else if (protocolType == IProxyListener.PROTOCOL_OTHER)
		{
			m_lTrafficOther = b;
		}

		Runnable updateThread = new Runnable()
		{
			public void run()
			{
				if (JAPController.getInstance().isAnonConnected())
				{
					m_lblBytes.setText(JAPUtil.formatBytesValueOnlyUnit(m_lTrafficWWW + m_lTrafficOther) +
									   "s: ");
					m_labelBytes.setText(JAPUtil.formatBytesValueWithoutUnit(m_lTrafficWWW + m_lTrafficOther));
				}
				else
				{
					m_lblBytes.setText(JAPMessages.getString("iconifiedviewBytes") + ": ");
					m_labelBytes.setText(JAPMessages.getString("iconifiedViewNA"));
				}
			}
		};

		if (SwingUtilities.isEventDispatchThread())
		{
			updateThread.run();
		}
		else
		{
			SwingUtilities.invokeLater(updateThread);
		}

		blink();
	}*/

	private void setButtonBorder()
	{
		if (!m_anonModeDisabled)
		{
			m_lblJAPIcon.setBorder(BorderFactory.createRaisedBevelBorder());
		}
		else
		{
			m_lblJAPIcon.setBorder(BorderFactory.createEmptyBorder());
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
					synchronized (m_lblJAPIcon)
					{
						if (m_Controller.isAnonConnected())
						{
							m_lblJAPIcon.setIcon(GUIUtils.loadImageIcon(JAPViewIconified.class.getName() +
								"_icon16red.gif", true, false));
							try
							{
								m_lblJAPIcon.wait(1000);
							}
							catch (InterruptedException a_e)
							{
								// ignore
							}
						}
						if (m_Controller.isAnonConnected())
						{
							String iconPath;
							if (JAPModel.getInstance().getProgramName().equals(JAPConstants.PROGRAM_NAME_JONDO))
							{
								iconPath = JAPConstants.ICON_JONDO;
							}
							else
							{
								iconPath = JAPConstants.IICON16FN;
							}
							m_lblJAPIcon.setIcon(GUIUtils.loadImageIcon(iconPath, true, false));
						}
						else
						{
							m_lblJAPIcon.setIcon(GUIUtils.loadImageIcon(JAPViewIconified.class.getName() +
								"_icon16discon.gif", true, false));
						}
					}
				}
			});
			blinkThread.setDaemon(true);
			blinkThread.start();
		}
	}
}
