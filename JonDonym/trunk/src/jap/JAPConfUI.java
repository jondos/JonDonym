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

import java.io.File;
import java.util.Hashtable;
import java.util.Locale;
import java.util.Observable;
import java.util.Observer;
import java.util.Vector;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.LookAndFeel;
import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;
import javax.swing.border.TitledBorder;
import javax.swing.filechooser.FileFilter;

import anon.util.ClassUtil;
import anon.util.IReturnRunnable;
import anon.util.ProgressCapsule;
import gui.GUIUtils;
import gui.JAPDll;
import gui.JAPMessages;
import gui.LanguageMapper;
import gui.TitledGridBagPanel;
import gui.dialog.DialogContentPane;
import gui.dialog.DialogContentPaneOptions;
import gui.dialog.JAPDialog;
import gui.dialog.SimpleWizardContentPane;
import gui.dialog.WorkerContentPane;
import gui.help.AbstractHelpFileStorageManager;
import gui.help.JAPExternalHelpViewer;
import gui.help.JAPHelp;
import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;
import platform.AbstractOS;
import platform.WindowsOS;
import anon.infoservice.JavaVersionDBEntry;

final class JAPConfUI extends AbstractJAPConfModule
{
	private static final String MSG_ON_CLOSING_JAP = JAPConfUI.class.getName() + "_onClosingJAP";
	private static final String MSG_WARNING_ON_CLOSING_JAP = JAPConfUI.class.getName() +
		"_warningOnClosingJAP";
	private static final String MSG_FONT_SIZE = JAPConfUI.class.getName() + "_fontSize";
	private static final String MSG_WARNING_IMPORT_LNF = JAPConfUI.class.getName() + "_warningImportLNF";
	private static final String MSG_INCOMPATIBLE_JAVA = JAPConfUI.class.getName() + "_incompatibleJava";
	private static final String MSG_REMOVE = JAPConfUI.class.getName() + "_remove";
	private static final String MSG_IMPORT = JAPConfUI.class.getName() + "_import";
	private static final String MSG_COULD_NOT_REMOVE = JAPConfUI.class.getName() + "_couldNotRemove";
	private static final String MSG_TITLE_IMPORT = JAPConfUI.class.getName() + "_titleImport";
	private static final String MSG_PROGRESS_IMPORTING = JAPConfUI.class.getName() + "_progressImport";
	private static final String MSG_IMPORT_SUCCESSFUL = JAPConfUI.class.getName() + "_importSuccessful";
	private static final String MSG_NO_LNF_FOUND = JAPConfUI.class.getName() + "_noLNFFound";
	private static final String MSG_LOOK_AND_FEEL_CHANGED = JAPConfUI.class.getName() + "_lnfChanged";
	private static final String MSG_RESTART_TO_UNLOAD = JAPConfUI.class.getName() + "_restartToUnload";
	private static final String MSG_DIALOG_FORMAT = JAPConfUI.class.getName() + "_lblDialogFormat";
	private static final String MSG_DIALOG_FORMAT_TEST = JAPConfUI.class.getName() + "_dialogFormatTest";
	private static final String MSG_DIALOG_FORMAT_TEST_2 = JAPConfUI.class.getName() + "_dialogFormatTest2";
	private static final String MSG_DIALOG_FORMAT_TEST_BTN = JAPConfUI.class.getName()
		+ "_dialogFormatTestBtn";
	private static final String MSG_DIALOG_FORMAT_GOLDEN_RATIO = JAPConfUI.class.getName()
		+ "_dialogFormatGoldenRatio";
	
	private static final String MSG_TEST_BROWSER_PATH = JAPConfUI.class.getName() + "_testBrowserPath";
	private static final String MSG_BROWSER_SHOULD_OPEN = JAPConfUI.class.getName() + "_browserShouldOpen";
	private static final String MSG_BROWSER_DOES_NOT_OPEN = JAPConfUI.class.getName() + "_browserDoesNotStart";
	private static final String MSG_BROWSER_TEST_PATH = JAPConfUI.class.getName() + "_browserTestPath";
	private static final String MSG_BROWSER_NEW_PATH = JAPConfUI.class.getName() + "_browserNewPath";
	private static final String MSG_BROWSER_TEST_BUTTON = JAPConfUI.class.getName() + "_browserTestBtn";
	private static final String MSG_BROWSER_TEST_EXPLAIN = JAPConfUI.class.getName() + "_browserTestExplain";
	
	private static final String MSG_HELP_PATH = JAPConfUI.class.getName() + "_helpPath";
	private static final String MSG_HELP_PATH_CHOOSE = JAPConfUI.class.getName() + "_helpPathChange";
	
	private static final String MSG_BROWSER_PATH = JAPConfUI.class.getName() + "_browserPath";
	private static final String MSG_BROWSER_PATH_CHOOSE = JAPConfUI.class.getName() + "_browserPathChange";
	
	private static final String MSG_NO_NATIVE_LIBRARY = JAPConfUI.class.getName() + "_noNativeLibrary";
	private static final String MSG_NO_NATIVE_WINDOWS_LIBRARY = JAPConfUI.class.getName() +
		"_noNativeWindowsLibrary";
	private static final String MSG_WINDOW_POSITION = JAPConfUI.class.getName() + "_windowPosition";
	private static final String MSG_WINDOW_MAIN = JAPConfUI.class.getName() + "_windowMain";
	private static final String MSG_WINDOW_CONFIG = JAPConfUI.class.getName() + "_windowConfig";
	private static final String MSG_WINDOW_ICON = JAPConfUI.class.getName() + "_windowIcon";
	private static final String MSG_WINDOW_HELP = JAPConfUI.class.getName() + "_windowHelp";
	private static final String MSG_WINDOW_SIZE = JAPConfUI.class.getName() + "_windowSize";

	private static final String MSG_MINI_ON_TOP = JAPConfUI.class.getName() + "_miniOnTop";
	private static final String MSG_MINI_ON_TOP_TT = JAPConfUI.class.getName() + "_miniOnTopTT";

	private TitledBorder m_borderLookAndFeel, m_borderView;
	private JComboBox m_comboLanguage, m_comboUI, m_comboDialogFormat;
	private JCheckBox m_cbSaveWindowLocationMain, m_cbSaveWindowLocationIcon, m_cbSaveWindowLocationConfig,
		m_cbSaveWindowLocationHelp, m_cbSaveWindowSizeConfig, m_cbSaveWindowSizeHelp, m_cbAfterStart, m_cbShowSplash, m_cbStartPortableFirefox;
	private JRadioButton m_rbViewSimplified, m_rbViewNormal, m_rbViewMini, m_rbViewSystray;
	private JCheckBox m_cbWarnOnClose, m_cbMiniOnTop, m_cbIgnoreDLLUpdate;
	private JSlider m_slidFontSize;
	private JButton m_btnAddUI, m_btnDeleteUI;
	private File m_currentDirectory;
	private JTextField m_helpPathField;
	private JButton m_helpPathButton;
	
	private JTextField m_portableBrowserPathField;
	private JButton m_portableBrowserPathButton;
	private Observer m_modelObserver;

	public JAPConfUI()
	{
		super(null);
		m_modelObserver = new Observer()
		{
			public void update(Observable a_notifier, Object a_message)
			{	
				if (a_message == JAPModel.CHANGED_HELP_PATH)
				{
					updateHelpPath();
					
					if (JAPModel.getInstance().isHelpPathChangeable())
					{
						m_helpPathButton.setEnabled(true);
					}
					else
					{
						m_helpPathButton.setEnabled(false);
					}
				}
				else if (a_message == JAPModel.CHANGED_DLL_UPDATE)
				{
					m_cbIgnoreDLLUpdate.setSelected(!JAPModel.getInstance().isDLLWarningActive());
					m_cbIgnoreDLLUpdate.setEnabled(JAPModel.getInstance().getDllUpdatePath() != null);
				}
			}
		};
	}
	
	protected boolean initObservers()
	{
		if (super.initObservers())
		{
			synchronized(LOCK_OBSERVABLE)
			{
				JAPModel.getInstance().addObserver(m_modelObserver);
				return true;
			}
		}
		return false;
	}

	public void chooseBrowserPath()
	{
		if (m_portableBrowserPathButton != null)
		{
			m_portableBrowserPathButton.doClick();
		}
	}
	
	public void recreateRootPanel()
	{
		JPanel panelRoot = getRootPanel();
		JPanel tempPanel;

		/* clear the whole root panel */
		panelRoot.removeAll();
		boolean bSimpleView = (JAPModel.getDefaultView() == JAPConstants.VIEW_SIMPLIFIED);
		GridBagLayout gbl1 = new GridBagLayout();
		GridBagConstraints c1 = new GridBagConstraints();
		panelRoot.setLayout(gbl1);
		c1.insets = new Insets(0, 0, 0, 0);
		c1.gridx = 0;
		c1.gridy = 0;
		c1.anchor = GridBagConstraints.NORTHWEST;
		c1.fill = GridBagConstraints.BOTH;
		c1.weightx = 1;
		c1.gridwidth = 2;
		panelRoot.add(createLookAndFeelPanel(), c1);

		c1.insets = new Insets(0, 0, 0, 0);
		c1.gridwidth = 1;
		c1.gridy++;
		c1.gridx = 0;
		panelRoot.add(createViewPanel(), c1);

		c1.insets = new Insets(0, 0, 0, 0);
		c1.gridx = 1;
		panelRoot.add(createAfterStartupPanel(), c1);

		c1.gridy++;
		c1.fill = GridBagConstraints.BOTH;
		tempPanel = createWindowSizePanel();
		if (!bSimpleView)
		{
			panelRoot.add(tempPanel, c1);
		}

		c1.gridy++;
		tempPanel = createAfterShutdownPanel();
		if (!bSimpleView)
		{
			panelRoot.add(tempPanel, c1);
		}

		c1.gridx = 0;
		c1.gridy--;
		c1.gridheight = 2;
		c1.fill = GridBagConstraints.BOTH;
		tempPanel = createWindowPanel();
		if (!bSimpleView)
		{
			panelRoot.add(tempPanel, c1);
		}
		
		c1.gridx = 0;
		c1.gridy+=2;
		c1.fill = GridBagConstraints.BOTH;		
		c1.gridwidth = 2;
		tempPanel = createHelpPathPanel();
		if (JAPModel.getInstance().isHelpPathChangeable())
		{
			panelRoot.add(tempPanel, c1);
		}
		c1.gridy++;
		tempPanel = createBrowserPathPanel();
		if (JAPController.getInstance().isPortableMode())
		{
			panelRoot.add(tempPanel, c1);
		}
		c1.gridy++;
		c1.weighty = 1;
		panelRoot.add(new JPanel(), c1);
	}

	/**
	 * @todo The combox must be made visible after the pack operation,
	 * as otherwise on MacOS pack does not work properly most of the time.
	 * Currrently, the reason for this error is not known.
	 */
	public void afterPack()
	{
		m_comboUI.setVisible(true);
	}

	public void beforePack()
	{
		m_comboUI.setVisible(false);
	}


	private JPanel createLookAndFeelPanel()
	{
		GridBagLayout gbl = new GridBagLayout();
		GridBagConstraints c = new GridBagConstraints();

		m_borderLookAndFeel = new TitledBorder(JAPMessages.getString("settingsLookAndFeelBorder"));
		final JPanel p = new JPanel(gbl);
		p.setBorder(m_borderLookAndFeel);
		JLabel l = new JLabel(JAPMessages.getString("settingsLookAndFeel"));
		c.insets = new Insets(10, 10, 10, 10);
		c.gridy = 1;
		c.anchor = GridBagConstraints.NORTHWEST;
		p.add(l, c);
		c.gridx = 1;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.weightx = 1;

		m_comboUI = new JComboBox();
		p.add(m_comboUI, c);
		m_comboUI.setVisible(false);

		m_btnDeleteUI = new JButton(JAPMessages.getString(MSG_REMOVE));
		c.gridx++;
		c.weightx = 0;
		c.fill = GridBagConstraints.NONE;
		p.add(m_btnDeleteUI, c);
		m_btnDeleteUI.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent a_event)
			{
				try
				{
					synchronized (m_comboUI)
					{
						LookAndFeelInfo[] oldLnFs = UIManager.getInstalledLookAndFeels();
						Vector tempLnFs = new Vector(oldLnFs.length - 1);
						LookAndFeelInfo[] alteredLnFs;
						File lnfFile = ClassUtil.getClassDirectory(
							(oldLnFs[m_comboUI.getSelectedIndex()].getClassName()));
						File tempLnfFile;

						for (int i = 0; i < oldLnFs.length; i++)
						{
							tempLnfFile = ClassUtil.getClassDirectory(oldLnFs[i].getClassName());
							if (tempLnfFile == null || !lnfFile.equals(tempLnfFile))
							{
								tempLnFs.addElement(oldLnFs[i]);
							}
						}
						alteredLnFs = new LookAndFeelInfo[tempLnFs.size()];
						for (int i = 0; i < alteredLnFs.length; i++)
						{
							alteredLnFs[i] = (LookAndFeelInfo) tempLnFs.elementAt(i);
						}
						UIManager.setInstalledLookAndFeels(alteredLnFs);
						JAPModel.getInstance().removeLookAndFeelFile(lnfFile);
						updateUICombo();
					}
					JAPDialog.showMessageDialog(getRootPanel(), JAPMessages.getString(MSG_RESTART_TO_UNLOAD));
				}
				catch (Exception a_e)
				{
					JAPDialog.showErrorDialog(
						getRootPanel(),
						JAPMessages.getString(MSG_COULD_NOT_REMOVE), LogType.MISC, a_e);
				}
			}
		});

		m_btnAddUI = new JButton(JAPMessages.getString(MSG_IMPORT));

		c.gridx++;
		//c.gridwidth = 1;
		p.add(m_btnAddUI, c);
		m_btnAddUI.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent a_event)
			{
				final JFileChooser fileChooser = new JFileChooser(m_currentDirectory);
				final JAPDialog dialog = new JAPDialog(getRootPanel(), JAPMessages.getString(MSG_TITLE_IMPORT));
				LookAndFeel currentLaf = UIManager.getLookAndFeel();

				final DialogContentPane pane =
					new SimpleWizardContentPane(dialog,
												"<font color='red'>" +
												JAPMessages.getString(MSG_WARNING_IMPORT_LNF) + "</font>",
												new DialogContentPane.Layout(
					JAPMessages.getString(JAPDialog.MSG_TITLE_WARNING),
					DialogContentPane.MESSAGE_TYPE_WARNING),
												(DialogContentPaneOptions)null)
				{
					boolean m_bCanceled = false;

					public CheckError[] checkYesOK()
					{
						m_bCanceled = false;
						CheckError[] errors = super.checkYesOK();
						fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
						FileFilter filter = new FileFilter()
						{
							public boolean accept(File a_file)
							{
								return a_file.isDirectory() || a_file.getName().endsWith(".jar");
							}

							public String getDescription()
							{
								return "*.jar";
							}
						};
						fileChooser.setFileFilter(filter);
						if (fileChooser.showOpenDialog(dialog.getContentPane()) !=
							JFileChooser.APPROVE_OPTION)
						{
							m_bCanceled = true;
						}
						return errors;
					}

					public Object getValue()
					{
						return new Boolean(m_bCanceled);
					}
				};

				final IReturnRunnable doIt = new IReturnRunnable()
				{
					Object m_value;

					public Object getValue()
					{
						return m_value;
					}

					public void run()
					{
						if (fileChooser.getSelectedFile() != null)
						{
							m_currentDirectory = fileChooser.getCurrentDirectory();
							Vector files;
							try
							{
								if ( (files = GUIUtils.registerLookAndFeelClasses(
									fileChooser.getSelectedFile())).size() > 0)
								{
									for (int i = 0; i < files.size(); i++)
									{
										LogHolder.log(LogLevel.NOTICE, LogType.GUI,
											"Added new L&F class file: " + files.elementAt(i));
										JAPModel.getInstance().addLookAndFeelFile(
											(File) files.elementAt(i));
									}
									updateUICombo();
									m_value = JAPMessages.getString(MSG_IMPORT_SUCCESSFUL);
								}
								else
								{
									m_value = new Exception(JAPMessages.getString(MSG_NO_LNF_FOUND));
								}
							}
							catch (IllegalAccessException a_e)
							{
								m_value = new Exception(JAPMessages.getString(MSG_INCOMPATIBLE_JAVA));
							}
							fileChooser.setSelectedFile(null);
						}
					}
				};

				DialogContentPane importPane = new WorkerContentPane(dialog,
					JAPMessages.getString(MSG_PROGRESS_IMPORTING) + "...", pane, doIt)
				{
					public boolean isSkippedAsNextContentPane()
					{
						return ( (Boolean) pane.getValue()).booleanValue();
					}
				};

				DialogContentPane goodResultPane = new SimpleWizardContentPane(dialog, "OK",
					new DialogContentPane.Layout(
						JAPMessages.getString(JAPDialog.MSG_TITLE_INFO),
						DialogContentPane.MESSAGE_TYPE_INFORMATION),
					new DialogContentPaneOptions(importPane))
				{
					public CheckError[] checkUpdate()
					{
						setText( (String) doIt.getValue());
						return null;
					}

					public boolean isSkippedAsNextContentPane()
					{
						return ( (Boolean) pane.getValue()).booleanValue() ||
							doIt.getValue() instanceof Exception;
					}

					public boolean isSkippedAsPreviousContentPane()
					{
						return true;
					}

				};
				goodResultPane.getButtonCancel().setVisible(false);

				DialogContentPane errorPane = new SimpleWizardContentPane(dialog, "ERROR",
					new DialogContentPane.Layout(
						JAPMessages.getString(JAPDialog.MSG_TITLE_ERROR),
						DialogContentPane.MESSAGE_TYPE_ERROR),
					new DialogContentPaneOptions(goodResultPane))
				{
					public boolean isSkippedAsPreviousContentPane()
					{
						return true;
					}

					public CheckError[] checkUpdate()
					{
						setText( ( (Exception) doIt.getValue()).getMessage());
						return null;
					}

					public boolean isSkippedAsNextContentPane()
					{
						return ( (Boolean) pane.getValue()).booleanValue() ||
							! (doIt.getValue() instanceof Exception);
					}
				};
				errorPane.getButtonCancel().setVisible(false);

				JLabel dummyLabel = new JLabel("AAAAAAAAAAAAAAAAAAAAAAAA");
				importPane.getContentPane().add(dummyLabel);
				DialogContentPane.updateDialogOptimalSized(pane);
				dummyLabel.setVisible(false);
				dialog.setVisible(true);
				if (currentLaf != UIManager.getLookAndFeel())
				{
					JAPDialog.showMessageDialog(
						getRootPanel(), JAPMessages.getString(MSG_LOOK_AND_FEEL_CHANGED));
				}
			}
		});

		m_comboUI.addItemListener(new ItemListener()
		{
			public void itemStateChanged(ItemEvent a_event)
			{
				synchronized (m_comboUI)
				{
					if (m_comboUI.getSelectedIndex() >= 0)
					{
						String selectedLaFClass = UIManager.getInstalledLookAndFeels()[
							m_comboUI.getSelectedIndex()].getClassName();
						String currentLaFClass = JAPModel.getInstance().getLookAndFeel();
						// the currently active lnf may differ from the laf that is set as active
						String activeLaFClass = UIManager.getLookAndFeel().getClass().getName();
						File currentLaFFile = null;
						File activeLaFFile = null;
						File selectedLaFFile = null;

						activeLaFFile = ClassUtil.getClassDirectory(activeLaFClass);
						currentLaFFile = ClassUtil.getClassDirectory(currentLaFClass);
						selectedLaFFile = ClassUtil.getClassDirectory(selectedLaFClass);

						if ( (selectedLaFFile != null && currentLaFFile != null &&
							  currentLaFFile.equals(selectedLaFFile)) ||
							(selectedLaFFile != null && activeLaFFile != null &&
							 activeLaFFile.equals(selectedLaFFile)) ||
							currentLaFClass.equals(selectedLaFClass) ||
							activeLaFClass.equals(selectedLaFClass) ||
							JAPModel.getInstance().isSystemLookAndFeel(selectedLaFClass))
						{
							m_btnDeleteUI.setEnabled(false);
						}
						else
						{
							m_btnDeleteUI.setEnabled(true);
						}
					}
				}
			}
		});

		l = new JLabel(JAPMessages.getString("settingsLanguage"));
		c.gridx = 0;
		c.gridy = 0;
		c.fill = GridBagConstraints.NONE;
		c.weightx = 0;
		p.add(l, c);
		m_comboLanguage = new JComboBox();
		String[] languages = JAPConstants.getSupportedLanguages();
		for (int i = 0; i < languages.length; i++)
		{
			m_comboLanguage.addItem(new LanguageMapper(languages[i], new Locale(languages[i], "")));
		}
		c.gridx = 1;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.weightx = 1;
		c.gridwidth = 1;
		p.add(m_comboLanguage, c);

		l = new JLabel(JAPMessages.getString(JAPMessages.getString(MSG_DIALOG_FORMAT)));
		c.gridx = 0;
		c.gridy = 2;
		c.fill = GridBagConstraints.NONE;
		c.weightx = 0;
		//c.gridwidth = 1;
		p.add(l, c);
		l.setVisible(JAPModel.getInstance().isDialogFormatShown());
		c.gridx = 1;
		c.fill = GridBagConstraints.HORIZONTAL;
		m_comboDialogFormat = new JComboBox();
		m_comboDialogFormat.addItem(new DialogFormat(JAPMessages.getString(MSG_DIALOG_FORMAT_GOLDEN_RATIO),
			JAPDialog.FORMAT_GOLDEN_RATIO_PHI));
		m_comboDialogFormat.addItem(new DialogFormat("4:3", JAPDialog.FORMAT_DEFAULT_SCREEN));
		m_comboDialogFormat.addItem(new DialogFormat("16:9", JAPDialog.FORMAT_WIDE_SCREEN));
		p.add(m_comboDialogFormat, c);
		JButton btnTestFormat = new JButton(JAPMessages.getString(MSG_DIALOG_FORMAT_TEST_BTN));
		btnTestFormat.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent a_event)
			{
				int currentFormat = JAPDialog.getOptimizedFormat();
				JAPDialog.setOptimizedFormat(
					( (DialogFormat) m_comboDialogFormat.getSelectedItem()).getFormat());
				JAPDialog.showMessageDialog(getRootPanel(), JAPMessages.getString(MSG_DIALOG_FORMAT_TEST));
				JAPDialog.setOptimizedFormat(currentFormat);
			}
		});
		c.gridx = 2;
		//c.fill = GridBagConstraints.NONE;
		c.weightx = 0;
		p.add(btnTestFormat, c);
		m_comboDialogFormat.setVisible(JAPModel.getInstance().isDialogFormatShown());
		btnTestFormat.setVisible(JAPModel.getInstance().isDialogFormatShown());

		btnTestFormat = new JButton(JAPMessages.getString(MSG_DIALOG_FORMAT_TEST_BTN));
		btnTestFormat.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent a_event)
			{
				int currentFormat = JAPDialog.getOptimizedFormat();
				JAPDialog.setOptimizedFormat(
					( (DialogFormat) m_comboDialogFormat.getSelectedItem()).getFormat());
				JAPDialog.showMessageDialog(getRootPanel(), JAPMessages.getString(MSG_DIALOG_FORMAT_TEST_2));
				JAPDialog.setOptimizedFormat(currentFormat);
			}
		});
		c.gridx = 3;
		//c.fill = GridBagConstraints.NONE;
		c.weightx = 0;
		p.add(btnTestFormat, c);
		btnTestFormat.setVisible(JAPModel.getInstance().isDialogFormatShown());

		c.gridx = 0;
		c.gridy++;
		c.weightx = 0;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridwidth = 1;
		p.add(new JLabel(JAPMessages.getString(MSG_FONT_SIZE)), c);

		m_slidFontSize = new JSlider(
			JSlider.HORIZONTAL, 0, JAPModel.MAX_FONT_SIZE, JAPModel.getInstance().getFontSize());
		m_slidFontSize.setPaintTicks(false);
		m_slidFontSize.setPaintLabels(true);
		m_slidFontSize.setMajorTickSpacing(1);
		m_slidFontSize.setMinorTickSpacing(1);
		m_slidFontSize.setSnapToTicks(true);
		m_slidFontSize.setPaintTrack(true);
		Hashtable map = new Hashtable(JAPModel.MAX_FONT_SIZE + 1);
		for (int i = 0; i <= JAPModel.MAX_FONT_SIZE; i++)
		{
			map.put(new Integer(i), new JLabel("1" + i + "0%"));
		}
		m_slidFontSize.setLabelTable(map);
		c.gridwidth = 3;
		c.gridx++;
		p.add(m_slidFontSize, c);

		return p;
	}

	private class DialogFormat
	{
		String m_description;
		int m_format;
		public DialogFormat(String a_description, int a_format)
		{
			m_description = a_description;
			m_format = a_format;
		}

		public String toString()
		{
			return m_description;
		}

		public int getFormat()
		{
			return m_format;
		}
	}

	private JPanel createWindowSizePanel()
	{
		GridBagConstraints c = new GridBagConstraints();
		JPanel p = new JPanel(new GridBagLayout());
		p.setBorder(new TitledBorder(JAPMessages.getString(MSG_WINDOW_SIZE)));

		m_cbSaveWindowSizeConfig = new JCheckBox(JAPMessages.getString(MSG_WINDOW_CONFIG));
		c.weightx = 1;
		c.gridx = 0;
		c.gridy = 0;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.insets = new Insets(0, 10, 0, 10);
		p.add(m_cbSaveWindowSizeConfig, c);

		m_cbSaveWindowSizeHelp = new JCheckBox(JAPMessages.getString(MSG_WINDOW_HELP));
		c.gridy++;
		//p.add(m_cbSaveWindowSizeHelp, c);

		return p;
	}

	private JPanel createWindowPanel()
	{
		GridBagConstraints c = new GridBagConstraints();
		JPanel p = new JPanel(new GridBagLayout());
		p.setBorder(new TitledBorder(JAPMessages.getString(MSG_WINDOW_POSITION)));

		m_cbSaveWindowLocationMain = new JCheckBox(JAPMessages.getString(MSG_WINDOW_MAIN));
		c.weightx = 1;
		c.gridx = 0;
		c.gridy = 0;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.insets = new Insets(0, 10, 0, 10);
		p.add(m_cbSaveWindowLocationMain, c);

		m_cbSaveWindowLocationConfig = new JCheckBox(JAPMessages.getString(MSG_WINDOW_CONFIG));
		c.gridy++;
		p.add(m_cbSaveWindowLocationConfig, c);

		m_cbSaveWindowLocationIcon = new JCheckBox(JAPMessages.getString(MSG_WINDOW_ICON));
		c.gridy++;
		p.add(m_cbSaveWindowLocationIcon, c);

		m_cbSaveWindowLocationHelp = new JCheckBox(JAPMessages.getString(MSG_WINDOW_HELP));
		c.gridy++;
		//p.add(m_cbSaveWindowLocationHelp, c);

		return p;
	}

	private JPanel createViewPanel()
	{
		GridBagLayout gbl = new GridBagLayout();
		GridBagConstraints c = new GridBagConstraints();
		m_borderView = new TitledBorder(JAPMessages.getString("ngSettingsViewBorder"));
		JPanel p = new JPanel(gbl);
		p.setBorder(m_borderView);
		m_rbViewNormal = new JRadioButton(JAPMessages.getString("ngSettingsViewNormal"));
		m_rbViewSimplified = new JRadioButton(JAPMessages.getString("ngSettingsViewSimplified"));
		ButtonGroup bg = new ButtonGroup();
		bg.add(m_rbViewNormal);
		bg.add(m_rbViewSimplified);
		c.insets = new Insets(0, 10, 10, 10);
		c.weightx = 1;
		c.anchor = GridBagConstraints.NORTHWEST;
		p.add(m_rbViewNormal, c);
		c.gridy = 1;
		p.add(m_rbViewSimplified, c);

		c.gridy++;
		m_cbMiniOnTop = new JCheckBox(JAPMessages.getString(MSG_MINI_ON_TOP));
		if (JAPDll.getDllVersion() == null && JavaVersionDBEntry.CURRENT_JAVA_VERSION.compareTo("1.5") < 0)
		{
			m_cbMiniOnTop.setEnabled(false);
			m_cbMiniOnTop.setToolTipText(JAPMessages.getString(MSG_MINI_ON_TOP_TT));
		}
		p.add(m_cbMiniOnTop, c);
		c.gridy++;
		
		m_cbIgnoreDLLUpdate = new JCheckBox(JAPMessages.getString(gui.JAPDll.MSG_IGNORE_UPDATE));
		if (JAPDll.getDllVersion() == null || JAPModel.getInstance().getDllUpdatePath() == null)
		{
			m_cbIgnoreDLLUpdate.setEnabled(false);			
		}
		p.add(m_cbIgnoreDLLUpdate, c);
		
		return p;
	}

	private JPanel createAfterShutdownPanel()
	{
		TitledGridBagPanel panel = new TitledGridBagPanel(JAPMessages.getString(MSG_ON_CLOSING_JAP),
			new Insets(0, 10, 0, 10));
		m_cbWarnOnClose = new JCheckBox(JAPMessages.getString(MSG_WARNING_ON_CLOSING_JAP));
		m_cbWarnOnClose.setEnabled(!JAPController.getInstance().isPortableMode());
		panel.addRow(m_cbWarnOnClose, null);
		return panel;
	}

	private JPanel createAfterStartupPanel()
	{
		GridBagLayout gbl = new GridBagLayout();
		GridBagConstraints c = new GridBagConstraints();
		JPanel p = new JPanel(gbl);
		p.setBorder(new TitledBorder(JAPMessages.getString("ngSettingsStartBorder")));
		m_cbAfterStart = new JCheckBox(JAPMessages.getString("ngViewAfterStart"));
		m_cbAfterStart.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				boolean b = m_cbAfterStart.isSelected();
				updateThirdPanel(b);
			}
		});
		c.insets = new Insets(0, 10, 0, 10);
		c.anchor = GridBagConstraints.NORTHWEST;
		c.weightx = 1;
		p.add(m_cbAfterStart, c);
		m_rbViewMini = new JRadioButton(JAPMessages.getString("ngViewMini"));
		m_rbViewSystray = new JRadioButton(JAPMessages.getString("ngViewSystray"));
		if (JAPDll.getDllVersion() == null)
		{
			// no library loaded
			if (AbstractOS.getInstance() instanceof WindowsOS)
			{
				m_rbViewSystray.setToolTipText(JAPMessages.getString(MSG_NO_NATIVE_WINDOWS_LIBRARY));
			}
			else
			{
				m_rbViewSystray.setToolTipText(JAPMessages.getString(MSG_NO_NATIVE_LIBRARY));
			}
		}
		ButtonGroup bg = new ButtonGroup();
		bg.add(m_rbViewMini);
		bg.add(m_rbViewSystray);
		c.gridy = 1;
		c.insets = new Insets(5, 30, 0, 10);
		p.add(m_rbViewMini, c);
		c.gridy = 2;
		p.add(m_rbViewSystray, c);

		m_cbShowSplash = new JCheckBox(JAPMessages.getString("ngViewShowSplash"));
		m_cbShowSplash.setEnabled(!JAPModel.getInstance().getShowSplashDisabled());
		c.gridy = 3;
		c.insets = new Insets(0, 10, 0, 10);
		p.add(m_cbShowSplash, c);
		
		m_cbStartPortableFirefox = new JCheckBox(JAPMessages.getString("ngViewStartPortableFirefox"));
		m_cbStartPortableFirefox.setEnabled(JAPController.getInstance().isPortableMode());
		c.gridy = 4;
		p.add(m_cbStartPortableFirefox, c);
		return p;
	}
	
	private JPanel createBrowserPathPanel()
	{
		GridBagConstraints c = new GridBagConstraints();
		JPanel p = new JPanel(new GridBagLayout());
		p.setBorder(new TitledBorder(JAPMessages.getString(MSG_BROWSER_PATH)));
		
		c.weightx = 1;
		m_portableBrowserPathField = new JTextField(10);
		m_portableBrowserPathField.setEditable(false);		
		m_portableBrowserPathButton = new JButton(JAPMessages.getString(MSG_BROWSER_PATH_CHOOSE));
		
		if (JAPModel.getInstance().getPortableBrowserpath() != null)
		{
			m_portableBrowserPathField.setText(JAPModel.getInstance().getPortableBrowserpath());
		}
		else if (AbstractOS.getInstance().getDefaultBrowserPath() != null)
		{
			m_portableBrowserPathField.setText(AbstractOS.getInstance().getDefaultBrowserPath());
		}		
		else
		{				
			m_portableBrowserPathField.setText("");
		}
		
		ActionListener buttonActionListener = 
			new ActionListener()
			{
				public void actionPerformed(ActionEvent aev)
				{
					File browserFile = null;
					JFileChooser chooser;
					
					if (aev.getActionCommand() != null && new File(aev.getActionCommand()).exists())
					{
						chooser = new JFileChooser(aev.getActionCommand());
					}
					else if (JAPModel.getInstance().getPortableBrowserpath() != null)
					{
						chooser = new JFileChooser(JAPModel.getInstance().getPortableBrowserpath());
					}
					else if (AbstractOS.getInstance().getDefaultBrowserPath() != null)
					{
						chooser = new JFileChooser(AbstractOS.getInstance().getDefaultBrowserPath());
					}
					else
					{
						chooser = new JFileChooser(System.getProperty("user.dir"));
					}
					chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
					
					if (chooser.showOpenDialog(JAPConfUI.this.getRootPanel()) == JFileChooser.APPROVE_OPTION)
					{
						browserFile = chooser.getSelectedFile();
					}
					if (browserFile != null)
					{
						final String filePath = AbstractOS.toRelativePath(browserFile.getPath());						
						JAPDialog browserTestDialog = new JAPDialog(JAPConfUI.this.getRootPanel(),
								JAPMessages.getString(MSG_TEST_BROWSER_PATH));
						final DialogContentPane pane = new DialogContentPane(browserTestDialog, 
								JAPMessages.getString(MSG_BROWSER_TEST_EXPLAIN),
								new DialogContentPaneOptions(JAPDialog.OPTION_TYPE_YES_NO_CANCEL))
						{
							private boolean m_bValid = false;
							
							public CheckError[] checkNo()
							{
								CheckError[] errors = null;
								
								if (AbstractOS.getInstance().openBrowser(
										AbstractOS.createBrowserCommand(filePath)))
								{
									printStatusMessage(JAPMessages.getString(MSG_BROWSER_SHOULD_OPEN));
									m_bValid = true;
								}
								else
								{
									errors = new CheckError[]{
											new CheckError(JAPMessages.getString(MSG_BROWSER_DOES_NOT_OPEN), LogType.GUI)};
								}
								return errors;
							}
							
							public CheckError[] checkYesOK()
							{
								CheckError[] errors = null;
								
								if (!m_bValid)
								{
									errors = new CheckError[]{new CheckError(JAPMessages.getString(MSG_BROWSER_TEST_PATH), LogType.GUI)};
								}
								
								return errors;
							}
						};
						
						pane.getButtonNo().setText(JAPMessages.getString(MSG_BROWSER_TEST_BUTTON));
						pane.getButtonCancel().setText(JAPMessages.getString(MSG_BROWSER_NEW_PATH));
						pane.getButtonYesOK().setText(JAPMessages.getString(DialogContentPane.MSG_OK));
					
						pane.setDefaultButtonOperation(DialogContentPane.ON_YESOK_DISPOSE_DIALOG | 
								DialogContentPane.ON_CANCEL_DISPOSE_DIALOG);
						DialogContentPane.updateDialogOptimalSized(pane);
						browserTestDialog.setVisible(true);
						
						if (pane.getButtonValue() == DialogContentPane.RETURN_VALUE_OK)
						{
							m_portableBrowserPathField.setText(filePath);
							m_portableBrowserPathField.repaint();
						}
						else if (pane.getButtonValue() == DialogContentPane.RETURN_VALUE_CANCEL)
						{
							// choose another file
							actionPerformed(new ActionEvent(aev.getSource(), aev.getID(), browserFile.getPath()));
						}
					}
				}
			};
			
		m_portableBrowserPathButton.addActionListener(buttonActionListener);
		if(!JAPController.getInstance().isPortableMode())
		{
			m_portableBrowserPathButton.setEnabled(false);
		}
			
		c.anchor = GridBagConstraints.NORTHWEST;
		c.gridy = 0;
		c.gridx = 0;
		c.insets = new Insets(0, 10, 0, 10);
		c.weightx = 1.0;
		c.fill = GridBagConstraints.HORIZONTAL;
		p.add(m_portableBrowserPathField, c);
		c.gridx++;
		c.weightx = 0.0;
		p.add(m_portableBrowserPathButton, c);
		
		return p;
	}
	
	private JPanel createHelpPathPanel()
	{
		GridBagConstraints c = new GridBagConstraints();
		JPanel p = new JPanel(new GridBagLayout());
		p.setBorder(new TitledBorder(JAPMessages.getString(MSG_HELP_PATH)));
		
		c.weightx = 1;
		
		m_helpPathField = new JTextField(10);
		m_helpPathField.setEditable(false);		
		m_helpPathButton = new JButton(JAPMessages.getString(MSG_HELP_PATH_CHOOSE));
		if (JAPModel.getInstance().isHelpPathDefined())
		{
			m_helpPathField.setText(JAPModel.getInstance().getHelpPath());
		}
		else
		{				
			m_helpPathField.setText("");
		}
		
		ActionListener helpInstallButtonActionListener = 
			new ActionListener()
			{
				public void actionPerformed(ActionEvent aev)
				{
					JAPModel model = JAPModel.getInstance();
					File hpFile = null;
					
					JFileChooser chooser = new JFileChooser(model.getHelpPath());
					chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
					if(chooser.showOpenDialog(JAPConfUI.this.getRootPanel()) == JFileChooser.APPROVE_OPTION)
					{
						hpFile = chooser.getSelectedFile();
					}
					if(hpFile != null)
					{
						String hpFileValidation = model.helpPathValidityCheck(hpFile);
						if(hpFileValidation.equals(AbstractHelpFileStorageManager.HELP_VALID) ||
							hpFileValidation.equals(AbstractHelpFileStorageManager.HELP_JONDO_EXISTS))
						{
							m_helpPathField.setText(hpFile.getPath());
							m_helpPathField.repaint();
						}
						else
						{
							JAPDialog.showErrorDialog(JAPConf.getInstance(), 
									JAPMessages.getString(hpFileValidation), LogType.MISC);
						}
					}
				}
			};
		m_helpPathButton.addActionListener(helpInstallButtonActionListener);
		if(!JAPModel.getInstance().isHelpPathChangeable())
		{
			m_helpPathButton.setEnabled(false);
		}
			
		c.anchor = GridBagConstraints.NORTHWEST;
		c.gridy = 0;
		c.gridx = 0;
		c.insets = new Insets(0, 10, 0, 10);
		c.weightx = 1.0;
		c.fill = GridBagConstraints.HORIZONTAL;
		p.add(m_helpPathField, c);
		c.gridx++;
		c.weightx = 0.0;
		p.add(m_helpPathButton, c);
		
		return p;
	}
	
	private void submitHelpPathChange()
	{	
		if (m_helpPathField == null)
		{
			return;
		}
		
		final JAPModel model = JAPModel.getInstance();
		String strCheck = JAPModel.getInstance().helpPathValidityCheck(m_helpPathField.getText());
		
		if ((strCheck.equals(AbstractHelpFileStorageManager.HELP_VALID) ||
			strCheck.equals(AbstractHelpFileStorageManager.HELP_JONDO_EXISTS)) &&
			(model.getHelpPath() == null || !model.getHelpPath().equals(m_helpPathField.getText())))
		{			
			final JAPDialog dialog = 
				new JAPDialog(this.getRootPanel(), 
						JAPMessages.getString(JAPExternalHelpViewer.MSG_HELP_INSTALL));
			Runnable run = new Runnable()
			{
				public void run()
				{
	//				When we set the path, the file storage manager does the rest (if the path is valid) */
					model.setHelpPath(new File(m_helpPathField.getText()));
				}
			};
						
			final WorkerContentPane workerPane = 
				new WorkerContentPane(dialog, JAPMessages.getString(
						JAPExternalHelpViewer.MSG_HELP_INSTALL_PROGRESS),
						run, model.getHelpFileStorageObservable());
			workerPane.updateDialog();
			dialog.setResizable(false);
			dialog.setVisible(true);
			if(workerPane.getProgressStatus() != ProgressCapsule.PROGRESS_FINISHED)
			{			
				resetHelpPath();
				JAPDialog.showErrorDialog(JAPConf.getInstance(), 
						JAPMessages.getString(JAPExternalHelpViewer.MSG_HELP_INSTALL_FAILED), LogType.MISC);
			}
		}
	}

	private void resetHelpPath()
	{
		if (m_helpPathField != null)
		{
			m_helpPathField.setText("");
		}
	}
	
	private void updateHelpPath()
	{
		if (m_helpPathField != null)
		{			
			if(JAPModel.getInstance().isHelpPathDefined() && JAPModel.getInstance().isHelpPathChangeable())
			{
				m_helpPathField.setText(JAPModel.getInstance().getHelpPath());
			}
			else
			{
				m_helpPathField.setText("");
			}
		}
	}
	
	public String getTabTitle()
	{
		return JAPMessages.getString("ngUIPanelTitle");
	}

	protected void onCancelPressed()
	{
		updateValues(false);
	}

	protected boolean onOkPressed()
	{
		JAPModel model = JAPModel.getInstance();
		int oldFontSize = model.getFontSize();
		if (model.setFontSize(m_slidFontSize.getValue()) &&
			!model.isConfigWindowSizeSaved())
		{
			beforePack();
			JAPConf.getInstance().doPack();
			afterPack();
		}
		else
		{
			oldFontSize = -1;
		}

		model.setSaveMainWindowPosition(m_cbSaveWindowLocationMain.isSelected());
		model.setSaveConfigWindowPosition(m_cbSaveWindowLocationConfig.isSelected());
		model.setSaveIconifiedWindowPosition(m_cbSaveWindowLocationIcon.isSelected());
		model.setSaveHelpWindowPosition(m_cbSaveWindowLocationHelp.isSelected());
		model.setSaveHelpWindowSize(m_cbSaveWindowSizeHelp.isSelected());
		model.setSaveConfigWindowSize(m_cbSaveWindowSizeConfig.isSelected());
		
		if(JAPHelp.getHelpDialog() != null)
		{
			JAPHelp.getHelpDialog().resetAutomaticLocation(m_cbSaveWindowLocationHelp.isSelected());
		}

		if (JAPModel.getInstance().isConfigWindowSizeSaved())
		{
			JAPModel.getInstance().setConfigSize(JAPConf.getInstance().getSize());
		}

		JAPController.getInstance().setMinimizeOnStartup(m_rbViewMini.isSelected() &&
			m_cbAfterStart.isSelected());
		JAPController.getInstance().setMoveToSystrayOnStartup(m_rbViewSystray.isSelected() &&
			m_cbAfterStart.isSelected());
		JAPModel.getInstance().setShowSplashScreen(m_cbShowSplash.isSelected());
		JAPModel.getInstance().setStartPortableFirefox(m_cbStartPortableFirefox.isSelected());
		JAPModel.getInstance().setNeverRemindGoodbye(!m_cbWarnOnClose.isSelected());
		JAPModel.getInstance().setMiniViewOnTop(m_cbMiniOnTop.isSelected());
		JAPModel.getInstance().setDllWarning(!m_cbIgnoreDLLUpdate.isSelected());

		Locale newLocale;
		if (m_comboLanguage.getSelectedIndex() >= 0)
		{
			newLocale = ( (LanguageMapper) m_comboLanguage.getSelectedItem()).getLocale();
		}
		else
		{
			newLocale = JAPMessages.getLocale();
		}
		if (!JAPMessages.getLocale().equals(newLocale))
		{
			final Locale localeRestart = newLocale;
			JAPConf.getInstance().addNeedRestart(
				new JAPConf.AbstractRestartNeedingConfigChange()
			{
				public String getName()
				{
					return JAPMessages.getString("settingsLanguage");
				}

				public void doChange()
				{
					JAPMessages.setLocale(localeRestart);
				}
			});
		}
		int newDefaultView = JAPConstants.VIEW_NORMAL;
		if (m_rbViewSimplified.isSelected())
		{
			newDefaultView = JAPConstants.VIEW_SIMPLIFIED;
		}

		if (JAPModel.getDefaultView() != newDefaultView)
		{
			final int defaultViewRestart = newDefaultView;
			JAPConf.getInstance().addNeedRestart(
				new JAPConf.AbstractRestartNeedingConfigChange()
			{
				public String getName()
				{
					return JAPMessages.getString("ngSettingsViewBorder");
				}

				public void doChange()
				{
					JAPController.getInstance().setDefaultView(defaultViewRestart);
				}
			});
		}
		
		if (m_portableBrowserPathField.getText() != null && 
			m_portableBrowserPathField.getText().trim().length() > 0 &&
			!AbstractOS.toAbsolutePath(m_portableBrowserPathField.getText()).equals(
					AbstractOS.toAbsolutePath(JAPModel.getInstance().getPortableBrowserpath())))
		{
			JAPConf.getInstance().addNeedRestart(
				new JAPConf.AbstractRestartNeedingConfigChange()
			{
				public String getName()
				{
					return JAPMessages.getString(MSG_BROWSER_PATH);
				}

				public void doChange()
				{
					JAPModel.getInstance().setPortableBrowserpath(m_portableBrowserPathField.getText());
				}
			});
		}

		JAPDialog.setOptimizedFormat( ( (DialogFormat) m_comboDialogFormat.getSelectedItem()).getFormat());

		String newLaF;
		if (m_comboUI.getSelectedIndex() >= 0)
		{
			newLaF = UIManager.getInstalledLookAndFeels()[m_comboUI.getSelectedIndex()].getClassName();
		}
		else
		{
			newLaF = UIManager.getLookAndFeel().getClass().getName();
		}
		if (UIManager.getLookAndFeel().getClass().getName().equals(newLaF))
		{
			newLaF = null;
		}


		if (newLaF != null || oldFontSize >= 0)
		{
			final String lafRestart = newLaF;
			final int OLD_FONT_SIZE = oldFontSize;
			JAPConf.getInstance().addNeedRestart(
				new JAPConf.AbstractRestartNeedingConfigChange()
			{
				public String getName()
				{
					return JAPMessages.getString("settingsLookAndFeel");
				}

				public void doChange()
				{
					if (lafRestart != null)
					{
						JAPModel.getInstance().setLookAndFeel(lafRestart);
					}
				}

				public void doCancel()
				{
					if (OLD_FONT_SIZE >= 0)
					{
						m_slidFontSize.setValue(OLD_FONT_SIZE);
						JAPModel.getInstance().setFontSize(OLD_FONT_SIZE);
						beforePack();
						JAPConf.getInstance().doPack();
						afterPack();
					}
				}
			});
		}
		submitHelpPathChange();
		return true;
	}

	private void setLanguageComboIndex(Locale a_locale)
	{
		LanguageMapper langMapper = new LanguageMapper(a_locale.getLanguage());
		int i = 0;

		for (; i < m_comboLanguage.getItemCount(); i++)
		{
			if (m_comboLanguage.getItemAt(i).equals(langMapper))
			{
				m_comboLanguage.setSelectedIndex(i);
				break;
			}
		}
		if (i == m_comboLanguage.getItemCount())
		{
			// the requested language was not found
			m_comboLanguage.setSelectedIndex(0);
		}
	}

	protected void onUpdateValues()
	{
		//synchronized (JAPConf.getInstance())
		{
			updateUICombo();
			
			if (JAPModel.getInstance().getPortableBrowserpath() != null)
			{
				m_portableBrowserPathField.setText(JAPModel.getInstance().getPortableBrowserpath());
			}
			else
			{
				m_portableBrowserPathField.setText(AbstractOS.getInstance().getDefaultBrowserPath());
			}
			m_slidFontSize.setValue(JAPModel.getInstance().getFontSize());
			setLanguageComboIndex(JAPMessages.getLocale());
			m_cbSaveWindowLocationMain.setSelected(JAPModel.isMainWindowLocationSaved());
			m_cbSaveWindowLocationConfig.setSelected(JAPModel.getInstance().isConfigWindowLocationSaved());
			m_cbSaveWindowLocationIcon.setSelected(JAPModel.getInstance().isIconifiedWindowLocationSaved());
			m_cbSaveWindowLocationHelp.setSelected(JAPModel.getInstance().isHelpWindowLocationSaved());
			m_cbSaveWindowSizeHelp.setSelected(JAPModel.getInstance().isHelpWindowSizeSaved());
			m_cbSaveWindowSizeConfig.setSelected(JAPModel.getInstance().isConfigWindowSizeSaved());
			m_rbViewNormal.setSelected(JAPModel.getDefaultView() == JAPConstants.VIEW_NORMAL);
			m_rbViewSimplified.setSelected(JAPModel.getDefaultView() == JAPConstants.VIEW_SIMPLIFIED);
			m_rbViewSystray.setSelected(JAPModel.getMoveToSystrayOnStartup());
			m_rbViewMini.setSelected(JAPModel.getMinimizeOnStartup());
			m_cbMiniOnTop.setSelected(JAPModel.getInstance().isMiniViewOnTop());
			m_cbIgnoreDLLUpdate.setSelected(!JAPModel.getInstance().isDLLWarningActive());
			m_cbWarnOnClose.setSelected(!JAPModel.getInstance().isNeverRemindGoodbye());
			m_cbShowSplash.setSelected(JAPModel.getInstance().getShowSplashScreen());
			m_cbStartPortableFirefox.setSelected(JAPModel.getInstance().getStartPortableFirefox());
			
			boolean b = JAPModel.getMoveToSystrayOnStartup() || JAPModel.getMinimizeOnStartup();
			for (int i = 0; i < m_comboDialogFormat.getItemCount(); i++)
			{
				if ( ( (DialogFormat) m_comboDialogFormat.getItemAt(i)).getFormat() ==
					JAPDialog.getOptimizedFormat())
				{
					m_comboDialogFormat.setSelectedIndex(i);
					break;
				}
			}
			updateThirdPanel(b);
			updateHelpPath();
		}
	}

	public void onResetToDefaultsPressed()
	{
		setLanguageComboIndex(JAPMessages.getSystemLocale());
		LookAndFeelInfo lookandfeels[] = UIManager.getInstalledLookAndFeels();
		for (int i = 0; i < lookandfeels.length; i++)
		{
			if (lookandfeels[i].getClassName().equals(UIManager.getCrossPlatformLookAndFeelClassName()))
			{
				m_comboUI.setSelectedIndex(i);
				break;
			}
		}

		m_portableBrowserPathField.setText(AbstractOS.getInstance().getDefaultBrowserPath());
		m_cbSaveWindowLocationConfig.setSelected(JAPConstants.DEFAULT_SAVE_CONFIG_WINDOW_POSITION);
		m_cbSaveWindowLocationIcon.setSelected(JAPConstants.DEFAULT_SAVE_MINI_WINDOW_POSITION);
		m_cbSaveWindowLocationMain.setSelected(JAPConstants.DEFAULT_SAVE_MAIN_WINDOW_POSITION);
		m_cbSaveWindowLocationHelp.setSelected(JAPConstants.DEFAULT_SAVE_HELP_WINDOW_POSITION);
		m_cbSaveWindowSizeHelp.setSelected(JAPConstants.DEFAULT_SAVE_HELP_WINDOW_SIZE);
		m_cbSaveWindowSizeConfig.setSelected(JAPConstants.DEFAULT_SAVE_CONFIG_WINDOW_SIZE);
		m_rbViewNormal.setSelected(JAPConstants.DEFAULT_VIEW == JAPConstants.VIEW_NORMAL);
		m_rbViewSimplified.setSelected(JAPConstants.DEFAULT_VIEW == JAPConstants.VIEW_SIMPLIFIED);
		m_rbViewSystray.setSelected(JAPConstants.DEFAULT_MOVE_TO_SYSTRAY_ON_STARTUP);
		m_rbViewMini.setSelected(JAPConstants.DEFAULT_MINIMIZE_ON_STARTUP);
		m_cbMiniOnTop.setSelected(true);
		m_cbIgnoreDLLUpdate.setSelected(false);
		m_cbShowSplash.setSelected(true);
		m_cbStartPortableFirefox.setSelected(true);
		m_cbWarnOnClose.setSelected(JAPConstants.DEFAULT_WARN_ON_CLOSE);
		updateThirdPanel(JAPConstants.DEFAULT_MOVE_TO_SYSTRAY_ON_STARTUP ||
						 JAPConstants.DEFAULT_MINIMIZE_ON_STARTUP);
		
		resetHelpPath();
	}

	private void updateThirdPanel(boolean bAfterStart)
	{
		m_cbAfterStart.setSelected(bAfterStart);
		m_rbViewMini.setEnabled(bAfterStart);
		m_rbViewSystray.setEnabled(bAfterStart && (JAPDll.getDllVersion() != null));
		if (bAfterStart && ! (m_rbViewSystray.isSelected() || m_rbViewMini.isSelected()))
		{
			m_rbViewMini.setSelected(true);
		}
	}

	public String getHelpContext()
	{
		return "appearance";
	}

	private void updateUICombo()
	{
		synchronized (m_comboUI)
		{
			LookAndFeelInfo[] lf = UIManager.getInstalledLookAndFeels();
			Vector vecLFs = new Vector(lf.length);
			Vector vecLFNames = new Vector(lf.length);
			String currentLf = UIManager.getLookAndFeel().getClass().getName();

			// eliminate duplicate L&Fs
			for (int i = 0; i < lf.length; i++)
			{
				if (!vecLFNames.contains(lf[i].getClassName()))
				{
					vecLFNames.addElement(lf[i].getClassName());
					vecLFs.addElement(lf[i]);
				}
			}
			lf = new LookAndFeelInfo[vecLFs.size()];
			for (int i = 0; i < lf.length; i++)
			{
				lf[i] = (LookAndFeelInfo) vecLFs.elementAt(i);
			}
			UIManager.setInstalledLookAndFeels(lf);

			m_comboUI.removeAllItems();
			for (int lfidx = 0; lfidx < lf.length; lfidx++)
			{
				m_comboUI.addItem(lf[lfidx].getName());
			}
			// select the current
			int lfidx;
			for (lfidx = 0; lfidx < lf.length; lfidx++)
			{
				if (lf[lfidx].getClassName().equals(currentLf))
				{
					m_comboUI.setSelectedIndex(lfidx);
					break;
				}
			}
			if (! (lfidx < lf.length))
			{
				m_comboUI.addItem("(unknown)");
				m_comboUI.setSelectedIndex(lfidx);
			}
		}
	}
}
