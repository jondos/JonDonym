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
package gui.dialog;

import java.util.Vector;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import javax.swing.Box;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.RootPaneContainer;
import javax.swing.SwingConstants;
import javax.swing.border.TitledBorder;

import gui.GUIUtils;
import gui.JAPHelp;
import gui.JAPHelpContext;
import gui.JAPHtmlMultiLineLabel;
import gui.JAPMessages;
import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;
import java.awt.event.MouseListener;

/**
 * This is a replacement for a dialog content pane. It defines an icon, buttons, a status bar for
 * information and error messages, an optional titled border around the content and a content pane
 * where own components can be placed. The content pane of the parent dialog is automatically replaced
 * with this one by calling the method <CODE>updateDialog()</CODE>. If the size of the dialog has not been
 * defined before, you will need to call pack() afterwards.
 * <P>Dialog content panes can be implemented as a  chained list, so that if someone clicks on a button,
 * the next or previous content pane in the list is displayed in the dialog. Use setDefaultButtonOperation()
 * and the ON_... events to activate this behaviour. Of course, the foward and back operations can be done
 * explicitly and without those events, too.</P>
 * If you have a chained list, you can display it as a Wizard, too. Every content pane in the list must
 * implement the interface DialogContentPane.IWizardSuitable and each content pane is recommended to
 * support BUTTON_OPERATION_WIZARD. Their "YES/OK" and "NO" buttons will automatically
 * be transformed into "Next" and "Previous", and all buttons are shown (Cancel, Previous, Next). If a class
 * wants to keep its own buttons as defined by the option type but act in a wizard, it has to implement
 * IWizardSuitableNoWizardButtons. This will prevent that is gets the wizard layout.
 *
 * @see gui.dialog.JAPDialog
 * @see javax.swing.JDialog
 * @see gui.dialog.DialogContentPane.Layout
 * @see gui.dialog.DialogContentPane.Options
 * @see gui.dialog.DialogContentPane.CheckError
 * @see gui.dialog.DialogContentPane.IWizardSuitable
 * @see gui.dialog.DialogContentPane.IWizardSuitableNoWizardButtons
 * @author Rolf Wendolsky
 */
public class DialogContentPane implements JAPHelpContext.IHelpContext, IDialogOptions
{
	private static final int MESSAGE_TYPES[] =
		{MESSAGE_TYPE_PLAIN, MESSAGE_TYPE_INFORMATION,
		MESSAGE_TYPE_QUESTION, MESSAGE_TYPE_WARNING, MESSAGE_TYPE_ERROR};
	private static final Icon MESSAGE_ICONS[] = new Icon[MESSAGE_TYPES.length];

	static
	{
		// preload java dialog icons
		if (!JAPDialog.isConsoleOnly())
		{
			JOptionPane pane;
			for (int i = 0; i < MESSAGE_TYPES.length; i++)
			{
				pane = new JOptionPane("", MESSAGE_TYPES[i]);
				pane.createDialog(null, "");
				MESSAGE_ICONS[i] = findMessageIcon(pane);
			}
		}
	}

	public static final int ON_CLICK_DO_NOTHING = 0;
	public static final int ON_CLICK_HIDE_DIALOG = 1;
	public static final int ON_CLICK_DISPOSE_DIALOG = 2;
	public static final int ON_CLICK_SHOW_NEXT_CONTENT = 4;
	public static final int ON_YESOK_SHOW_NEXT_CONTENT = 8;
	public static final int ON_NO_SHOW_NEXT_CONTENT = 16;
	public static final int ON_CANCEL_SHOW_NEXT_CONTENT = 32;
	public static final int ON_CLICK_SHOW_PREVIOUS_CONTENT = 64;
	public static final int ON_YESOK_SHOW_PREVIOUS_CONTENT = 128;
	public static final int ON_NO_SHOW_PREVIOUS_CONTENT = 256;
	public static final int ON_CANCEL_SHOW_PREVIOUS_CONTENT = 512;
	public static final int ON_YESOK_HIDE_DIALOG = 1024;
	public static final int ON_NO_HIDE_DIALOG = 2048;
	public static final int ON_CANCEL_HIDE_DIALOG = 4096;
	public static final int ON_YESOK_DISPOSE_DIALOG = 8192;
	public static final int ON_NO_DISPOSE_DIALOG = 16384;
	public static final int ON_CANCEL_DISPOSE_DIALOG = 32768;

	/**
	 * Is equal to ON_NO_SHOW_PREVIOUS_CONTENT | ON_YESOK_SHOW_NEXT_CONTENT | ON_CLICK_DISPOSE_DIALOG
	 * as the typical wizard behaviour . If the default button operation does not contain this behaviour,
	 * at least the class itself should implement an equal behaviour, or this content pane should not be
	 * displayed as a wizard (not implement IWizardSuitable).
	 */
	public static final int BUTTON_OPERATION_WIZARD =
		ON_NO_SHOW_PREVIOUS_CONTENT | ON_YESOK_SHOW_NEXT_CONTENT | ON_CLICK_DISPOSE_DIALOG;

	public static final String MSG_OK = DialogContentPane.class.getName() + "_OK";
	public static final String MSG_YES = DialogContentPane.class.getName() + "_yes";
	public static final String MSG_NO = DialogContentPane.class.getName() + "_no";
	public static final String MSG_NEXT = DialogContentPane.class.getName() + "_next";
	public static final String MSG_PREVIOUS = DialogContentPane.class.getName() + "_previous";
	public static final String MSG_FINISH = DialogContentPane.class.getName() + "_finish";
	public static final String MSG_CANCEL = DialogContentPane.class.getName() + "_cancel";
	public static final String MSG_OPERATION_FAILED = DialogContentPane.class.getName() + "_operationFailed";
	public static final String MSG_SEE_FULL_MESSAGE = DialogContentPane.class.getName() + "_seeFullMessage";

	public static final int DEFAULT_BUTTON_EMPTY = 0;
	public static final int DEFAULT_BUTTON_CANCEL = 1;
	public static final int DEFAULT_BUTTON_YES = 2;
	public static final int DEFAULT_BUTTON_OK = DEFAULT_BUTTON_YES;
	public static final int DEFAULT_BUTTON_NO = 3;
	public static final int DEFAULT_BUTTON_HELP = 4;
	public static final int DEFAULT_BUTTON_KEEP = 5;

	private static final int MIN_TEXT_WIDTH = 100;
	private static final int UNLIMITED_SIZE = 2500;
	private static final int SPACE_AROUND_TEXT = 5;
	private static final String MORE_POINTS = "...";
	/** @todo Implement a better heuristic */
	private static final int NUMBER_OF_HEURISTIC_ITERATIONS = 6;

	private DialogContentPane m_nextContentPane;
	private DialogContentPane m_previousContentPane;
	private RootPaneContainer m_parentDialog;
	private JComponent m_contentPane;
	private JPanel m_titlePane;
	private JPanel m_rootPane;
	private Container m_panelOptions;
	private JAPHtmlMultiLineLabel m_lblMessage;
	private LinkedDialog m_linkedDialog;
	private JAPHtmlMultiLineLabel m_lblText;
	private JAPHtmlMultiLineLabel m_lblSeeFullText;
	private int m_messageType;
	private int m_optionType;
	private int m_defaultButtonOperation;
	private int m_value;
	private JAPHelpContext.IHelpContext m_helpContext;
	private JButton m_btnHelp;
	private JButton m_btnYesOK;
	private JButton m_btnNo;
	private JButton m_btnCancel;
	private ButtonListener m_buttonListener;
	private Icon m_icon;
	private boolean m_bHasHadWizardLayout;
	private GridBagConstraints m_textConstraints;
	private Vector m_rememberedErrors = new Vector();
	private Vector m_rememberedUpdateErrors = new Vector();
	private Container m_currentlyActiveContentPane;
	private Vector m_componentListeners = new Vector();
	private ComponentListener m_currentlyActiveContentPaneComponentListener;
	private int m_defaultButton;
	private String m_strText;
	private String m_strTitle;
	private JDialog m_tempDialog; // this is a temporary dialog used to construct the content pane
	private boolean m_bDisposed = false;

	private int m_idStatusMessage = 0;

	/**
	 * Contructs a new dialog content pane. Its layout is predefined, but may change if the content pane
	 * is part of a wizard.
	 * @param a_parentDialog the parent dialog; a content pane is always registered to a dialog, and
	 * may not change it in lifetime.
	 * @param a_strText A text that is shown withing the content pane. The text is interpreted as HTML. If
	 * you call pack() on the dialog when it is updated with this content pane, the text length is
	 * auto-formatted so that its width is not bigger than the content with respect to a minimum size.
	 * Notice: this only works correctly if you call pack() on an invisible dialog.
	 */
	public DialogContentPane(JDialog a_parentDialog, String a_strText)
	{
		this((RootPaneContainer)a_parentDialog, a_strText, new Layout(""), null);
	}

	/**
	 * Contructs a new dialog content pane. Its layout is predefined, but may change if the content pane
	 * is part of a wizard.
	 * @param a_parentDialog the parent dialog; a content pane is always registered to a dialog, and
	 * may not change it in lifetime.
	 * @param a_strText A text that is shown withing the content pane. The text is interpreted as HTML. If
	 * you call pack() on the dialog when it is updated with this content pane, the text length is
	 * auto-formatted so that its width is not bigger than the content with respect to a minimum size.
	 * Notice: this only works correctly if you call pack() on an invisible dialog.
	 */
	public DialogContentPane(JAPDialog a_parentDialog, String a_strText)
	{
		this((RootPaneContainer)a_parentDialog, a_strText, new Layout(""), null);
	}

	/**
	 * Contructs a new dialog content pane. Its layout is predefined, but may change if the content pane
	 * is part of a wizard.
	 * @param a_parentDialog the parent dialog; a content pane is always registered to a dialog, and
	 * may not change it in lifetime.
	 * @param a_strText A text that is shown withing the content pane. The text is interpreted as HTML. If
	 * you call pack() on the dialog when it is updated with this content pane, the text length is
	 * auto-formatted so that its width is not bigger than the content with respect to a minimum size.
	 * Notice: this only works correctly if you call pack() on an invisible dialog.
	 * @param a_layout the general layout of the content pane (icon, title, border, ...)
	 */
	public DialogContentPane(JDialog a_parentDialog, String a_strText, Layout a_layout)
	{
		this((RootPaneContainer)a_parentDialog, a_strText, a_layout, null);
	}

	/**
	 * Contructs a new dialog content pane. Its layout is predefined, but may change if the content pane
	 * is part of a wizard.
	 * @param a_parentDialog the parent dialog; a content pane is always registered to a dialog, and
	 * may not change it in lifetime.
	 * @param a_strText A text that is shown withing the content pane. The text is interpreted as HTML. If
	 * you call pack() on the dialog when it is updated with this content pane, the text length is
	 * auto-formatted so that its width is not bigger than the content with respect to a minimum size.
	 * Notice: this only works correctly if you call pack() on an invisible dialog.
	 * @param a_layout the general layout of the content pane (icon, title, border, ...)
	 */
	public DialogContentPane(JAPDialog a_parentDialog, String a_strText, Layout a_layout)
	{
		this((RootPaneContainer)a_parentDialog, a_strText, a_layout, null);
	}

	/**
	 * Contructs a new dialog content pane. Its layout is predefined, but may change if the content pane
	 * is part of a wizard.
	 * @param a_parentDialog the parent dialog; a content pane is always registered to a dialog, and
	 * may not change it in lifetime.
	 * @param a_strText A text that is shown withing the content pane. The text is interpreted as HTML. If
	 * you call pack() on the dialog when it is updated with this content pane, the text length is
	 * auto-formatted so that its width is not bigger than the content with respect to a minimum size.
	 * Notice: this only works correctly if you call pack() on an invisible dialog.
	 * @param a_options the button definitions
	 */
	public DialogContentPane(JDialog a_parentDialog, String a_strText, Options a_options)
	{
		this((RootPaneContainer)a_parentDialog, a_strText, new Layout(""), a_options);
	}

	/**
	 * Contructs a new dialog content pane. Its layout is predefined, but may change if the content pane
	 * is part of a wizard.
	 * @param a_parentDialog the parent dialog; a content pane is always registered to a dialog, and
	 * may not change it in lifetime.
	 * @param a_strText A text that is shown withing the content pane. The text is interpreted as HTML. If
	 * you call pack() on the dialog when it is updated with this content pane, the text length is
	 * auto-formatted so that its width is not bigger than the content with respect to a minimum size.
	 * Notice: this only works correctly if you call pack() on an invisible dialog.
	 * @param a_options the button definitions
	 */
	public DialogContentPane(JAPDialog a_parentDialog, String a_strText, Options a_options)
	{
		this((RootPaneContainer)a_parentDialog, a_strText, new Layout(""), a_options);
	}

	/**
	 * Contructs a new dialog content pane. Its layout is predefined, but may change if the content pane
	 * is part of a wizard.
	 * @param a_parentDialog the parent dialog; a content pane is always registered to a dialog, and
	 * may not change it in lifetime.
	 * @param a_strText A text that is shown withing the content pane. The text is interpreted as HTML. If
	 * you call pack() on the dialog when it is updated with this content pane, the text length is
	 * auto-formatted so that its width is not bigger than the content with respect to a minimum size.
	 * Notice: this only works correctly if you call pack() on an invisible dialog.
	 * @param a_layout the general layout of the content pane (icon, title, border, ...)
	 * @param a_options the button definitions
	 */
	public DialogContentPane(JDialog a_parentDialog, String a_strText, Layout a_layout, Options a_options)
	{
		this((RootPaneContainer)a_parentDialog, a_strText, a_layout, a_options);
	}

	/**
	 * Contructs a new dialog content pane. Its layout is predefined, but may change if the content pane
	 * is part of a wizard.
	 * @param a_parentDialog the parent dialog; a content pane is always registered to a dialog, and
	 * may not change it in lifetime.
	 * @param a_strText A text that is shown withing the content pane. The text is interpreted as HTML. If
	 * you call pack() on the dialog when it is updated with this content pane, the text length is
	 * auto-formatted so that its width is not bigger than the content with respect to a minimum size.
	 * Notice: this only works correctly if you call pack() on an invisible dialog.
	 * @param a_layout the general layout of the content pane (icon, title, border, ...)
	 * @param a_options the button definitions
	 */
	public DialogContentPane(JAPDialog a_parentDialog, String a_strText, Layout a_layout, Options a_options)
	{
		this((RootPaneContainer)a_parentDialog, a_strText, a_layout, a_options);
	}

	/**
	 * Contructs a new dialog content pane. Its layout is predefined, but may change if the content pane
	 * is part of a wizard.
	 * @param a_parentDialog the parent dialog; a content pane is always registered to a dialog, and
	 * may not change it in lifetime.
	 */
	public DialogContentPane(JDialog a_parentDialog)
	{
		this((RootPaneContainer)a_parentDialog, null, new Layout(""), null);
	}

	/**
	 * Contructs a new dialog content pane. Its layout is predefined, but may change if the content pane
	 * is part of a wizard.
	 * @param a_parentDialog the parent dialog; a content pane is always registered to a dialog, and
	 * may not change it in lifetime.
	 */
	public DialogContentPane(JAPDialog a_parentDialog)
	{
		this((RootPaneContainer)a_parentDialog, null, new Layout(""), null);
	}

	/**
	 * Contructs a new dialog content pane. Its layout is predefined, but may change if the content pane
	 * is part of a wizard.
	 * @param a_parentDialog the parent dialog; a content pane is always registered to a dialog, and
	 * may not change it in lifetime.
	 * @param a_layout the general layout of the content pane (icon, title, border, ...)
	 */
	public DialogContentPane(JDialog a_parentDialog, Layout a_layout)
	{
		this((RootPaneContainer)a_parentDialog, null, a_layout, null);
	}

	/**
	 * Contructs a new dialog content pane. Its layout is predefined, but may change if the content pane
	 * is part of a wizard.
	 * @param a_parentDialog the parent dialog; a content pane is always registered to a dialog, and
	 * may not change it in lifetime.
	 * @param a_layout the general layout of the content pane (icon, title, border, ...)
	 */
	public DialogContentPane(JAPDialog a_parentDialog, Layout a_layout)
	{
		this((RootPaneContainer)a_parentDialog, null, a_layout, null);
	}

	/**
	 * Contructs a new dialog content pane. Its layout is predefined, but may change if the content pane
	 * is part of a wizard.
	 * @param a_parentDialog the parent dialog; a content pane is always registered to a dialog, and
	 * may not change it in lifetime.
	 * @param a_options the button definitions
	 */
	public DialogContentPane(JDialog a_parentDialog, Options a_options)
	{
		this((RootPaneContainer)a_parentDialog, null, new Layout(""), a_options);
	}

	/**
	 * Contructs a new dialog content pane. Its layout is predefined, but may change if the content pane
	 * is part of a wizard.
	 * @param a_parentDialog the parent dialog; a content pane is always registered to a dialog, and
	 * may not change it in lifetime.
	 * @param a_options the button definitions
	 */
	public DialogContentPane(JAPDialog a_parentDialog, Options a_options)
	{
		this((RootPaneContainer)a_parentDialog, null, new Layout(""), a_options);
	}

	/**
	 * Contructs a new dialog content pane. Its layout is predefined, but may change if the content pane
	 * is part of a wizard.
	 * @param a_parentDialog the parent dialog; a content pane is always registered to a dialog, and
	 * may not change it in lifetime.
	 * @param a_layout the general layout of the content pane (icon, title, border, ...)
	 * @param a_options the button definitions
	 */
	public DialogContentPane(JDialog a_parentDialog, Layout a_layout, Options a_options)
	{
		this((RootPaneContainer)a_parentDialog, null, a_layout, a_options);
	}

	/**
	 * Contructs a new dialog content pane. Its layout is predefined, but may change if the content pane
	 * is part of a wizard.
	 * @param a_parentDialog the parent dialog; a content pane is always registered to a dialog, and
	 * may not change it in lifetime.
	 * @param a_layout the general layout of the content pane (icon, title, border, ...)
	 * @param a_options the button definitions
	 */
	public DialogContentPane(JAPDialog a_parentDialog, Layout a_layout, Options a_options)
	{
		this((RootPaneContainer)a_parentDialog, null, a_layout, a_options);
	}

	/**
	 * Contructs a new dialog content pane. Its layout is predefined, but may change if the content pane
	 * is part of a wizard.
	 * @param a_parentDialog the parent dialog; a content pane is always registered to a dialog, and
	 * may not change it in lifetime.
	 * @param a_strText A text that is shown withing the content pane. The text is interpreted as HTML. If
	 * you call pack() on the dialog when it is updated with this content pane, the text length is
	 * auto-formatted so that its width is not bigger than the content with respect to a minimum size.
	 * Notice: this only works correctly if you call pack() on an invisible dialog.
	 * @param a_layout the general layout of the content pane (icon, title, border, ...)
	 * @param a_options the button definitions
	 */
	private DialogContentPane(RootPaneContainer a_parentDialog, String a_strText,
							  Layout a_layout, Options a_options)
	{
		if (a_layout == null)
		{
			a_layout = new Layout((String)null);
		}
		if (a_options == null)
		{
			a_options = new Options((JAPHelpContext.IHelpContext)null);
		}

		init(a_parentDialog, a_layout.getTitle(), a_strText, SwingConstants.CENTER, a_options.getOptionType(),
			 a_layout.getMessageType(), a_layout.getIcon(), a_layout.isCentered(), a_options.getHelpContext(),
			 a_options.getPreviousContentPane());
	}

	/**
	 * Initialises this content pane.
	 * @param a_parentDialog either a JDialog or a JAPDialog; <CODE>null</CODE> will lead to an Exception
	 * @param a_strTitle A title for the content pane that is shown in a TitledBorder. If the title is null
	 * or empty, no border is shown around the content pane. If the title is not null
	 * (may be an empty String), a status bar is shown between the content pane and the buttons.
	 * @param a_strText A text that is shown withing the content pane. The text is interpreted as HTML. If
	 * you call pack() on the dialog when it is updated with this content pane, the text length is
	 * auto-formatted so that its width is not bigger than the content with respect to a minimum size.
	 * Notice: this only works correctly if you call pack() on an invisible dialog.
	 * @param a_alignment text alignment; you may use the javax.swing.SwingConstants
	 * @param a_optionType one of the available option types the define the type and number of buttons
	 * @param a_messageType one of the available message types that define the message layout
	 * @param a_icon an Icon; if null, the icon will be chosen automatically depending on the message type
	 * @param a_bCentered if the text and content should be centered on the panel; otherwise, the content
	 * is expanded to fill the panel
	 * @param a_helpContext a IHelpContext; if it returns an other help context value than null,
	 * a help button is shown that opens the context;
	 * @param a_previousContentPane A DialogContentPane that will be linked with this one; it gets this
	 * content pane as next content pane. Call moveToNextContentPane() and moveToPreviousContentPane() to
	 * move between the panes.
	 * @todo implement the expansion on !a_bCentered
	 */
	private void init(RootPaneContainer a_parentDialog, String a_strTitle, String a_strText, int a_alignment,
					  int a_optionType, int a_messageType, Icon a_icon, boolean a_bCentered,
					  JAPHelpContext.IHelpContext a_helpContext, DialogContentPane a_previousContentPane)
	{
		if (a_parentDialog == null)
		{
			throw new IllegalArgumentException("The parent dialog must not be null!");
		}

		if (a_previousContentPane != null && a_previousContentPane.m_parentDialog != a_parentDialog)
		{
			throw new IllegalArgumentException("Chained content panes must refer to the same dialog!");
		}

		if (!(a_optionType == OPTION_TYPE_EMPTY || a_optionType == OPTION_TYPE_DEFAULT ||
			a_optionType == OPTION_TYPE_CANCEL || a_optionType == OPTION_TYPE_OK_CANCEL ||
			a_optionType == OPTION_TYPE_YES_NO_CANCEL || a_optionType == OPTION_TYPE_YES_NO))
		{
			throw new IllegalArgumentException("Unknown option type!");
		}

		if (!(a_messageType == MESSAGE_TYPE_PLAIN || a_messageType == MESSAGE_TYPE_QUESTION ||
			a_messageType == MESSAGE_TYPE_ERROR || a_messageType == MESSAGE_TYPE_WARNING ||
			a_messageType == MESSAGE_TYPE_INFORMATION))
		{
			throw new IllegalArgumentException("Unknown message type!");
		}

		if (this instanceof IWizardSuitable)
		{
			m_defaultButtonOperation = BUTTON_OPERATION_WIZARD;
		}
		else
		{
			m_defaultButtonOperation = ON_CLICK_DO_NOTHING;
		}

		m_parentDialog = a_parentDialog;
		m_previousContentPane = a_previousContentPane;
		m_messageType = a_messageType;
		m_optionType = a_optionType;

		m_icon = a_icon;
		if (m_icon == null)
		{
			m_icon = getMessageIcon(m_messageType);
		}

		m_helpContext = a_helpContext;
		m_rootPane = new JPanel(new BorderLayout());
		m_titlePane = new JPanel(new GridBagLayout());
		m_rootPane.add(m_titlePane, BorderLayout.CENTER);


		addDialogComponentListener(new DialogComponentListener());
		addDialogWindowListener(new DialogWindowListener());

		setContentPane(new JPanel());

		if (a_strTitle != null)
		{
			if (a_strTitle.trim().length() > 0)
			{
				m_strTitle = a_strTitle;
				m_titlePane.setBorder(new TitledBorder(a_strTitle));
			}
			// the status message bar is only shown if the title is a valid String (may be empty but not null)
			m_lblMessage = new JAPHtmlMultiLineLabel();
			m_lblMessage.setFontStyle(JAPHtmlMultiLineLabel.FONT_STYLE_BOLD);
			clearStatusMessage();
			m_rootPane.add(m_lblMessage, BorderLayout.SOUTH);
		}

		if (a_strText != null && a_strText.trim().length() > 0)
		{
			m_strText = JAPHtmlMultiLineLabel.removeHTMLHEADAndBODYTags(a_strText) ;
			m_lblText =
				new JAPHtmlMultiLineLabel("<font color=#000000>" + m_strText + "</font>", a_alignment);
			m_lblText.setFontStyle(JAPHtmlMultiLineLabel.FONT_STYLE_PLAIN);
		}
		// set the contraints for the text label; they are used later
		m_textConstraints = new GridBagConstraints();
		m_textConstraints.gridx = 0;
		m_textConstraints.gridy = 1;
		m_textConstraints.weightx = 1;
		m_textConstraints.weighty = 0;
		m_textConstraints.anchor = GridBagConstraints.NORTH;
		m_textConstraints.fill = GridBagConstraints.HORIZONTAL;
		m_textConstraints.insets =
			new Insets(SPACE_AROUND_TEXT, SPACE_AROUND_TEXT, SPACE_AROUND_TEXT, SPACE_AROUND_TEXT);

		if (a_bCentered)
		{
			// center text and content vertically
			GridBagConstraints contraints = new GridBagConstraints();
			contraints.gridx = 0;
			contraints.gridy = 0;
			contraints.weightx = 0;
			contraints.weighty = 10;
			contraints.anchor = GridBagConstraints.NORTH;
			contraints.fill = GridBagConstraints.VERTICAL;
			m_titlePane.add(new JPanel(), contraints);
			contraints.gridy = 4;
			m_titlePane.add(new JPanel(), contraints);
		}
		else
		{
			// expand the content; does not work on JDK 1.1.8...
		}

		// construct the chain of content panes
		if (m_previousContentPane != null)
		{
			m_previousContentPane.setNextContentPane(this);
		}

		m_bHasHadWizardLayout = false;
		setButtonValue(RETURN_VALUE_UNINITIALIZED);

		// create the buttons
		createOptions();

		// set the default button; if possible, OK is chosen, otherwise cancel or help
		if (a_optionType == OPTION_TYPE_DEFAULT || a_optionType == OPTION_TYPE_YES_NO ||
			a_optionType == OPTION_TYPE_OK_CANCEL || a_optionType == OPTION_TYPE_YES_NO_CANCEL)
		{
			setDefaultButton(DEFAULT_BUTTON_OK);
		}
		else if (a_optionType == OPTION_TYPE_CANCEL)
		{
			setDefaultButton(DEFAULT_BUTTON_CANCEL);
		}
		else
		{
			if (getButtonHelp() != null)
			{
				setDefaultButton(DEFAULT_BUTTON_HELP);
			}
			else
			{
				setDefaultButton(DEFAULT_BUTTON_KEEP);
			}
		}

		addComponentListener(new ComponentAdapter()
		{
			public void componentShown(ComponentEvent a_event)
			{
				if (hasWizardLayout())
				{
					setTextOfWizardNextButton();
				}
				validateDialog();
			}
		});
	}

	public static Icon getMessageIcon(int a_messageType)
	{
		Icon icon;
		int nrIcon;
		if (a_messageType == MESSAGE_TYPE_INFORMATION)
		{
			nrIcon = 1;
		}
		else if (a_messageType == MESSAGE_TYPE_QUESTION)
		{
			nrIcon = 2;
		}
		else if (a_messageType == MESSAGE_TYPE_WARNING)
		{
			nrIcon = 3;
		}
		else if (a_messageType == MESSAGE_TYPE_ERROR)
		{
			nrIcon = 4;
		}
		else
		{
			return MESSAGE_ICONS[0];
		}
		icon = MESSAGE_ICONS[nrIcon];
		if (icon == null)
		{
			// image was not preloaded; make another try...
			icon = findMessageIcon(new JOptionPane("", a_messageType));
			MESSAGE_ICONS[nrIcon] = icon;
		}
		return GUIUtils.createScaledIcon(icon, GUIUtils.getIconResizer());
		//return icon;
	}

	/**
	 * Content panes that are suitable for use in a wizard should implement this interface.
	 * If implemented, and the content pane is at least chained with one other content pane
	 * (next or previous), the buttons are displayed in the style of a wizard: "No" -> "Previous",
	 * "Yes" -> "Next", "Cancel". The last pane in the chain gets a "Finish" instead of "Next".
	 * <P> A class that implements the wizard layout has the button operation
	 * BUTTON_OPERATION_WIZARD by default. Of course, this may be altered by calling
	 * setDefaultButtonOperation(int). </P>
	 */
	public static interface IWizardSuitable
	{
	}

	/**
	 * Classes that are WizardSuitable but do not want to get the wizard buttons should implement this
	 * interface.
	 */
	public static interface IWizardSuitableNoWizardButtons extends IWizardSuitable
	{
	}

	/**
	 * A CheckError is used to set error conditions that prohibit operations. The error conditions may
	 * contain a message that is dispayed to the user and may do some additional actions.
	 */
	public static class CheckError
	{
		private String m_strMessage;
		private int m_logType;
		private Throwable m_throwable;

		/**
		 * Creates a new CheckError that does not show any message to the user.
		 */
		public CheckError()
		{
			this("", LogType.NUL, null);
		}

		/**
		 * A new CheckError with a message for the user.
		 * @param a_strMessage a message for the user; if empty, no message is displayed; if null,
		 * an error message is auto-generated
		 * @param a_logType the LogType for this error
		 */
		public CheckError(String a_strMessage, int a_logType)
		{
			this(a_strMessage, a_logType, null);
		}

		/**
		 * A new CheckError with a message for the user.
		 * @param a_strMessage a message for the user; if empty, no message is displayed; if null,
		 * an error message is auto-generated
		 * @param a_logType the LogType for this error
		 * @param a_throwable a throwable that should be logged
		 */
		public CheckError(String a_strMessage,int a_logType, Throwable a_throwable)
		{
			m_strMessage = a_strMessage;
			m_logType = a_logType;
			m_throwable = a_throwable;
		}

		/**
		 * The action that is done if this error is handled by any method.
		 */
		public void doErrorAction()
		{
		}

		/**
		 * The action that is done to reset the state before the call of doErrorAction().
		 * All methods that interpret doErrorAction() should interpret undoErrorAction() and call
		 * undoErrorAction() on all errors on that it had called doErrorAction() before interpreting
		 * new errors.
		 */
		public void undoErrorAction()
		{
		}

		/**
		 * Returns the LogType of this CheckError.
		 * @return the LogType of this CheckError
		 */
		public final int getLogType()
		{
			return m_logType;
		}

		/**
		 * Returns the throwable to be logged or null if this CheckError does not contain any Throwable.
		 * @return the throwable to be logged or null if this CheckError does not contain any Throwable.
		 */
		public final Throwable getThrowable()
		{
			return m_throwable;
		}

		/**
		 * Returns the message to display to the user or null if this CheckError does not contain any message.
		 * @return the message to display to the user or null if this CheckError does not contain any message.
		 */
		public final String getMessage()
		{
			return m_strMessage;
		}

		/**
		 * Returns if a user-displayable error message can be extracted from this CheckError.
		 * @return if a user-displayable error message can be extracted from this CheckError
		 */
		public final boolean hasDisplayableErrorMessage()
		{
			return JAPDialog.retrieveErrorMessage(m_strMessage, m_throwable) != null;
		}
	}

	/**
	 * Defines the buttons that are available in a dialog.
	 */
	public static final class Options
	{
		private int m_optionType;
		private DialogContentPane m_previousContentPane;
		private JAPHelpContext.IHelpContext m_helpContext;

		/**
		 * Creates new button options.
		 * @param a_optionType one of the available option types the define the type and number of buttons
		 */
		public Options(int a_optionType)
		{
			this(a_optionType, (JAPHelpContext.IHelpContext)null, null);
		}

		/**
		 * Creates new button options. No buttons are shown by default.
		 * @param a_strHelpContext a IHelpContext; if it returns an other help context value than null,
		 * a help button is shown that opens the context;
		 */
		public Options(String a_strHelpContext)
		{
			this(OPTION_TYPE_EMPTY, a_strHelpContext, null);
		}

		/**
		 * Creates new button options. No buttons are shown by default.
		 * @param a_helpContext a IHelpContext; if it returns an other help context value than null,
		 * a help button is shown that opens the context;
		 */
		public Options(JAPHelpContext.IHelpContext a_helpContext)
		{
			this(OPTION_TYPE_EMPTY, a_helpContext, null);
		}

		/**
		 * Creates new button options. No buttons are shown by default.
		 * @param a_previousContentPane A DialogContentPane that will be linked with this one; it gets this
		 * content pane as next content pane. Call moveToNextContentPane() and moveToPreviousContentPane() to
		 * move between the panes.
		 */
		public Options(DialogContentPane a_previousContentPane)
		{
			this(OPTION_TYPE_EMPTY, (JAPHelpContext.IHelpContext)null, a_previousContentPane);
		}

		/**
		 * Creates new button options. No buttons are shown by default.
		 * @param a_strHelpContext a IHelpContext; if it returns an other help context value than null,
		 * a help button is shown that opens the context;
		 * @param a_previousContentPane A DialogContentPane that will be linked with this one; it gets this
		 * content pane as next content pane. Call moveToNextContentPane() and moveToPreviousContentPane() to
		 * move between the panes.
		 */
		public Options(String a_strHelpContext, DialogContentPane a_previousContentPane)
		{
			this(OPTION_TYPE_EMPTY, a_strHelpContext, a_previousContentPane);
		}

		/**
		 * Creates new button options. No buttons are shown by default.
		 * @param a_helpContext a IHelpContext; if it returns an other help context value than null,
		 * a help button is shown that opens the context;
		 * @param a_previousContentPane A DialogContentPane that will be linked with this one; it gets this
		 * content pane as next content pane. Call moveToNextContentPane() and moveToPreviousContentPane() to
		 * move between the panes.
		 */
		public Options(JAPHelpContext.IHelpContext a_helpContext, DialogContentPane a_previousContentPane)
		{
			this(OPTION_TYPE_EMPTY, a_helpContext, a_previousContentPane);
		}

		/**
		 * Creates new button options.
		 * @param a_optionType one of the available option types the define the type and number of buttons
		 * @param a_previousContentPane A DialogContentPane that will be linked with this one; it gets this
		 * content pane as next content pane. Call moveToNextContentPane() and moveToPreviousContentPane() to
		 * move between the panes.
		 */
		public Options(int a_optionType, DialogContentPane a_previousContentPane)
		{
			this(a_optionType, (JAPHelpContext.IHelpContext)null, a_previousContentPane);
		}

		/**
		 * Creates new button options.
		 * @param a_optionType one of the available option types the define the type and number of buttons
		 * @param a_helpContext a IHelpContext; if it returns an other help context value than null,
		 * a help button is shown that opens the context;
		 */
		public Options(int a_optionType, JAPHelpContext.IHelpContext a_helpContext)
		{
			this(a_optionType, a_helpContext, null);
		}

		/**
		 * Creates new button options.
		 * @param a_optionType one of the available option types the define the type and number of buttons
		 * @param a_strHelpContext a IHelpContext; if it returns an other help context value than null,
		 * a help button is shown that opens the context;
		 */
		public Options(int a_optionType, String a_strHelpContext)
		{
			this(a_optionType, a_strHelpContext, null);
		}


		/**
		 * Creates new button options.
		 * @param a_optionType one of the available option types the define the type and number of buttons
		 * @param a_strHelpContext a IHelpContext; if it returns an other help context value than null,
		 * a help button is shown that opens the context;
		 * @param a_previousContentPane A DialogContentPane that will be linked with this one; it gets this
		 * content pane as next content pane. Call moveToNextContentPane() and moveToPreviousContentPane() to
		 * move between the panes.
		 */
		public Options(int a_optionType, final String a_strHelpContext, DialogContentPane a_previousContentPane)
		{
			this(a_optionType,
				 new JAPHelpContext.IHelpContext(){public String getHelpContext(){return a_strHelpContext;}},
				a_previousContentPane);
		}

		/**
		 * Creates new button options.
		 * @param a_optionType one of the available option types the define the type and number of buttons
		 * @param a_helpContext a IHelpContext; if it returns an other help context value than null,
		 * a help button is shown that opens the context;
		 * @param a_previousContentPane A DialogContentPane that will be linked with this one; it gets this
		 * content pane as next content pane. Call moveToNextContentPane() and moveToPreviousContentPane() to
		 * move between the panes.
		 */
		public Options(int a_optionType, JAPHelpContext.IHelpContext a_helpContext,
					   DialogContentPane a_previousContentPane)
		{
			m_optionType = a_optionType;
			m_helpContext = a_helpContext;
			m_previousContentPane = a_previousContentPane;
		}

		public int getOptionType()
		{
			return m_optionType;
		}

		public JAPHelpContext.IHelpContext getHelpContext()
		{
			return m_helpContext;
		}

		public DialogContentPane getPreviousContentPane()
		{
			return m_previousContentPane;
		}
	}

	/**
	 * Defines the general layout of a dialog.
	 */
	public static final class Layout
	{
		private String m_strTitle;
		private int m_messageType;
		private Icon m_icon;
		private boolean m_bCentered;

		/**
		 * Creates a new Layout for the dialog content pane. The title is empty, therefore a status bar
		 * will be shown in the content pane.
		 * @param a_messageType The content pane's message type,
		 * e.g. MESSAGE_TYPE_PLAIN, MESSAGE_TYPE_ERROR, ...
		 */
		public Layout(int a_messageType)
		{
			this("", a_messageType, null);
		}

		/**
		 * Creates a new Layout for the dialog content pane.
		 * @param a_strTitle A title for the content pane that is shown in a TitledBorder. If the title is
		 * null or empty, no border is shown around the content pane. If the title is not null
		 * (may be an empty String), a status bar is shown between the content pane and the buttons.
		 */
		public Layout(String a_strTitle)
		{
			this(a_strTitle, MESSAGE_TYPE_PLAIN, null);
		}

		/**
		 * Creates a new Layout for the dialog content pane. The title is empty, therefore a status bar
		 * will be shown in the content pane.
		 * @param a_icon The icon for the content pane. If is is null, the icon will be automatically chosen
		 * depending on the message type.
		 */
		public Layout(Icon a_icon)
		{
			this("", MESSAGE_TYPE_PLAIN, a_icon);
		}

		/**
		 * Creates a new Layout for the dialog content pane. The title is empty, therefore a status bar
		 * will be shown in the content pane.
		 * @param a_messageType The content pane's message type,
		 * e.g. MESSAGE_TYPE_PLAIN, MESSAGE_TYPE_ERROR, ...
		 * @param a_icon The icon for the content pane. If is is null, the icon will be automatically chosen
		 * depending on the message type.
		 */
		public Layout(int a_messageType, Icon a_icon)
		{
			this("", a_messageType, a_icon);
		}

		/**
		 * Creates a new Layout for the dialog content pane.
		 * @param a_strTitle A title for the content pane that is shown in a TitledBorder. If the title is
		 * null or empty, no border is shown around the content pane. If the title is not null
		 * (may be an empty String), a status bar is shown between the content pane and the buttons.
		 * @param a_messageType The content pane's message type,
		 * e.g. MESSAGE_TYPE_PLAIN, MESSAGE_TYPE_ERROR, ...
		 * depending on the message type.
		 */
		public Layout(String a_strTitle, int a_messageType)
		{
			this(a_strTitle, a_messageType, null);
		}

		/**
		 * Creates a new Layout for the dialog content pane.
		 * @param a_strTitle A title for the content pane that is shown in a TitledBorder. If the title is
		 * null or empty, no border is shown around the content pane. If the title is not null
		 * (may be an empty String), a status bar is shown between the content pane and the buttons.
		 * @param a_icon The icon for the content pane. If is is null, the icon will be automatically chosen
		 * depending on the message type.
		 */
		public Layout(String a_strTitle, Icon a_icon)
		{
			this(a_strTitle, MESSAGE_TYPE_PLAIN, a_icon);
		}

		/**
		 * Creates a new Layout for the dialog content pane.
		 * @param a_strTitle A title for the content pane that is shown in a TitledBorder. If the title is
		 * null or empty, no border is shown around the content pane. If the title is not null
		 * (may be an empty String), a status bar is shown between the content pane and the buttons.
		 * @param a_messageType The content pane's message type,
		 * e.g. MESSAGE_TYPE_PLAIN, MESSAGE_TYPE_ERROR, ...
		 * @param a_icon The icon for the content pane. If is is null, the icon will be automatically chosen
		 * depending on the message type.
		 */
		public Layout(String a_strTitle, int a_messageType, Icon a_icon)
		{
			m_strTitle = a_strTitle;
			m_messageType = a_messageType;
			m_icon = a_icon;
			m_bCentered = true;
		}

		public boolean isCentered()
		{
			return m_bCentered;
		}

		/**
		 * Returns the title of the content pane that is shown in a TitledBorder. If the title is
		 * null or empty, no border is shown around the content pane. If the title is not null
		 * (may be an empty String), a status bar is shown between the content pane and the buttons.
		 * @return the title of the dialog content pane
		 */
		public String getTitle()
		{
			return m_strTitle;
		}

		/**
		 * The content pane's message type, e.g. MESSAGE_TYPE_PLAIN, MESSAGE_TYPE_ERROR, ...
		 * @return content pane's message type
		 */
		public int getMessageType()
		{
			return m_messageType;
		}

		/**
		 * Returns the icon for the content pane. If is is null, the icon will be automatically chosen
		 * depending on the message type.
		 * @return icon for the content pane.
		 */
		public Icon getIcon()
		{
			return m_icon;
		}
	}

	/**
	 * Calculates the optimal dialog size for a chain of content panes. The optimal size is defined
	 * as the size that is needed to the contents pane with the maximum width or the maximum size in
	 * the chain. The chain is defined as the content panes that are returned by calling
	 * <CODE> getNextContentPane() </CODE> on each one.
	 * <P> Note 1: This method needs to call updateDialog() for every content pane in the chain. You should
	 * therefore never call this method on a dialog that is visible! This would cause serious flickering
	 * in the best case, in the worst case the wrong dialog is shown afterwards (that is the last dialog
	 * in the chain). </P>
	 * <P> Note 2: If the content pane that should show up first is not the content pane you gave as
	 * argument, you will have to call <CODE> updateDialog() </CODE> on it after calling this method.</P>
	 * <P> Note 3: Subsequent calls of pack() on the dialog will de-optimise the dialog size; it is highly
	 * recommended not to do pack() afterwards! </P>
	 * @param a_firstContentPane the first DialogContentPane in a chain of content panes; this method will
	 * call <CODE> updateDialog() </CODE> on it to initialise the dialog
	 * @todo make sure that several calls of this method lead to the same result
	 */
	public static void updateDialogOptimalSized(DialogContentPane a_firstContentPane)
	{
		int dialogWidth, dialogHeight;
		int maxTextWidth;
		DialogContentPane nextContentPane;

		if (a_firstContentPane == null)
		{
			return;
		}

		maxTextWidth = MIN_TEXT_WIDTH;
		nextContentPane = a_firstContentPane;
		do
		{
			// find out the maximum content pane width and set the maximum text width accordingly
			maxTextWidth =
				Math.max(maxTextWidth, nextContentPane.getContentPane().getPreferredSize().width);
			nextContentPane = nextContentPane.getNextContentPane();
		}
		while (nextContentPane != null);

		dialogWidth = 0;
		dialogHeight = 0;
		nextContentPane = a_firstContentPane;
		if (a_firstContentPane.m_parentDialog instanceof JDialog)
		{
			JDialog dialog = (JDialog) a_firstContentPane.m_parentDialog;
			do
			{
				if (nextContentPane.isDialogVisible())
				{
					throw new IllegalStateException(
									   "You may not optimise the dialog size while it is visible!");
				}
				nextContentPane.updateDialog(maxTextWidth, false);
				nextContentPane.m_rootPane.setPreferredSize(null);
				dialog.pack();
				dialogWidth = Math.max(dialogWidth, dialog.getSize().width);
				dialogHeight = Math.max(dialogHeight, dialog.getSize().height);
				nextContentPane = nextContentPane.getNextContentPane();
			}
			while (nextContentPane != null);
			dialog.setSize(new Dimension(dialogWidth, dialogHeight));
		}
		else
		{
			// this is copy&paste; I do not know how to make it better in this case...
			JAPDialog dialog = (JAPDialog) a_firstContentPane.m_parentDialog;
			do
			{
				if (nextContentPane.isDialogVisible())
				{
					throw new IllegalStateException(
									   "You may not optimise the dialog size while it is visible!");
				}
				nextContentPane.updateDialog(maxTextWidth, false);
				nextContentPane.m_rootPane.setPreferredSize(null);
				dialog.pack();
				dialogWidth = Math.max(dialogWidth, dialog.getSize().width);
				dialogHeight = Math.max(dialogHeight, dialog.getSize().height);
				nextContentPane = nextContentPane.getNextContentPane();
			}
			while (nextContentPane != null);
			dialog.setSize(new Dimension(dialogWidth, dialogHeight));
		}

		// reset the preferred size of each content pane
		nextContentPane = a_firstContentPane;

		do
		{
			/*
			 * The root pane's preferred size is set as big as the dialog, therefore the content pane
			 * should fit into the dialog. Please note that this trick will render the pack() command useless.
			 */
			//nextContentPane.m_rootPane.setPreferredSize(new Dimension(dialogWidth, dialogHeight));
			nextContentPane.m_rootPane.setPreferredSize(new Dimension(UNLIMITED_SIZE, UNLIMITED_SIZE));
			nextContentPane = nextContentPane.getNextContentPane();
		}
		while (nextContentPane != null);

		a_firstContentPane.updateDialog();
	}

	/**
	 * Returns if this content pane has a predecessor.
	 * @return if this content pane has a predecessor
	 */
	public final boolean hasPreviousContentPane()
	{
		DialogContentPane contentPane = this;

		while ((contentPane = contentPane.getPreviousContentPane()) != null)
		{
			if (!contentPane.isMoveBackAllowed())
			{
				return false;
			}
			try
			{
				if (!contentPane.isSkippedAsPreviousContentPane())
				{
					return true;
				}
			}
			catch (Exception a_e)
			{
				// state is unknown and interpreted as false
				return false;
			}
		}

		return false;
	}

	/**
	 * Returns if this content pane has a successor.
	 * @return if this content pane has a successor
	 */
	public final boolean hasNextContentPane()
	{
		DialogContentPane contentPane = this;

		while ((contentPane = contentPane.getNextContentPane()) != null)
		{
			if (!contentPane.isMoveForwardAllowed())
			{
				return false;
			}
			try
			{
				if (!contentPane.isSkippedAsNextContentPane())
				{
					return true;
				}
			}
			catch (Exception a_e)
			{
				// state is unknown and interpreted as false
				return false;
			}
		}

		return false;
	}

	/**
	 * Returns if this content pane is formatted with the wizard layout. The "Yes" and "OK" buttons will
	 * be transformed to "Next", the "No" button is replaced by "Previous" and all buttons are shown
	 * (Cancel, Previous, Next), not regarding what buttons have been defined by the option type.
	 * If the dialog window is opened, the focus will automatically be set on "Next".
	 * <P> If a class wants to keep its own buttons as defined by the option type but act in a wizard,
	 * it has to implement IWizardSuitableNoWizardButtons.
	 * This will prevent that is gets the wizard layout. </P>
	 * @return if this content pane is formatted with the wizard layout
	 */
	public final boolean hasWizardLayout()
	{
		DialogContentPane contentPane = this;

		if (// this content pane must be suitable to be a wizard
			  !(contentPane instanceof IWizardSuitable) ||
			  // it must want to get the wizard buttons
			  contentPane instanceof IWizardSuitableNoWizardButtons ||
			  // at least one next or previous content pane is needed to get the wizard layout
			  (getNextContentPane() == null && getPreviousContentPane() == null))
		{
			return false;
		}

		// traverse the previous and next contents panes; if all of them have a wizard layout, we have, too
		while ((contentPane = contentPane.getPreviousContentPane()) != null)
		{
			if (!(contentPane instanceof IWizardSuitable))
			{
				return false;
			}
		}

		contentPane = this;

		while ((contentPane = contentPane.getNextContentPane()) != null)
		{
			if (!(contentPane instanceof IWizardSuitable))
			{
				return false;
			}
		}

		return true;
	}

	/**
	 * Returns the next content pane in the chained list of content panes.
	 * @return the next content pane in the chained list of content panes
	 */
	public final DialogContentPane getNextContentPane()
	{
		return m_nextContentPane;
	}

	/**
	 * Returns the previous content pane in the chained list of content panes.
	 * @return the previous content pane in the chained list of content panes
	 */
	public final DialogContentPane getPreviousContentPane()
	{
		return m_previousContentPane;
	}

	/**
	 * Shows the previous content pane in the dialog if it exists. Otherwise, the dialog is closed according
	 * to the default ON_CLICK operation. If no ON_CLICK operation is set, nothing is done by default.
	 * If the content pane exists, its checkUpdate() method is interpreted and the errors are handled.
	 * @return if a move to the previous content pane was done; false if no previous content pane does exist
	 * or if it refused to update the dialog
	 */
	public final boolean moveToPreviousContentPane()
	{
		return moveToContentPane(false);
	}

	/**
	 * Is called when the "Yes", "OK" or "Next" button is clicked.
	 * If one or more error occured, they should be returned as CheckErrors to inform the user. In this case,
	 * the automatic reaction on the button click is prohibited and getValue() will not change.
	 * Overwrite this method to set your own check; it returns <CODE> null </CODE> by default.
	 * This method should never be called directly and is only used internally.
	 * @return errors that prohibit the operation or null or an empty array if the operation is allowed
	 */
	public CheckError[] checkYesOK()
	{
		return null;
	}

	/**
	 * Is called when the "No" or "Previous" button is clicked.
	 * If one or more error occured, they should be returned as CheckErrors to inform the user. In this case,
	 * the automatic reaction on the button click is prohibited and getValue() will not change.
	 * Overwrite this method to set your own check; it returns <CODE> null </CODE> by default.
	 * This method should never be called directly and is only used internally.
	 * @return errors that prohibit the operation or null or an empty array if the operation is allowed
	 */
	public CheckError[] checkNo()
	{
		return null;
	}

	/**
	 * Is called when the "Cancel" button is clicked.
	 * If one or more error occured, they should be returned as CheckErrors to inform the user. In this case,
	 * the automatic reaction on the button click is prohibited and getValue() will not change.
	 * Overwrite this method to set your own check; it returns <CODE> null </CODE> by default.
	 * This method should never be called directly and is only used internally.
	 * @return errors that prohibit the operation or null or an empty array if the operation is allowed
	 */
	public CheckError[] checkCancel()
	{
		return null;
	}

	/**
	 * Is called when someone calls updateDialog() on this content pane. The update operation
	 * is only performed if null is returned. Otherwise, the caller may interpret the errors he gets from
	 * updateDialog. This is done by <CODE> moveToNextContentPane() </CODE> and
	 * <CODE> moveToPreviousContentPane() </CODE>.
	 * Overwrite this method to set your own check; it returns <CODE> null </CODE> by default.
	 * This method should never be called directly and is only used internally.
	 * @return errors that prohibit the operation or null or an empty array if the operation is allowed
	 */
	public CheckError[] checkUpdate()
	{
		return null;
	}

	/**
	 * If the previous content pane of this one calls moveToNextContentPane(), this content pane may tell him
	 * to skip it and move forward to the next one.
	 * Returns <CODE> false </CODE> by default but may be overwritten by subclasses.
	 * @return true if this content pane would like to be skipped as next content pane; false otherwise
	 */
	public boolean isSkippedAsNextContentPane()
	{
		return false;
	}

	/**
	 * Returns if a move back to the direction of this content pane is allowed in a wizard.
	 * Stronger than isSkippedAsPreviousContentPane(), as this method does not allow to access previous
	 * content panes, either. hasPreviousContentPane() will return 'false' for all content panes after
	 * this one if isMoveBackAllowed() returns false
	 * @return if a move back to the direction of this content pane is allowed in a wizard
	 */
	public boolean isMoveBackAllowed()
	{
		return true;
	}

	/**
	 * Returns if a move forward to the direction of this content pane is allowed in a wizard.
	 * Stronger than isSkippedAsNextsContentPane(), as this method does not allow to access next
	 * content panes, either. hasNextContentPane() will return 'false' for all content panes after
	 * this one if isMoveForwardAllowed() returns false
	 * @return if a move forward to the direction of this content pane is allowed in a wizard
	 */
	public boolean isMoveForwardAllowed()
	{
		return true;
	}


	/**
	 * If the next content pane of this one calls moveToPreviousContentPane(), this content pane may tell him
	 * to skip it and move forward to the next one.
	 * Returns <CODE> false </CODE> by default but may be overwritten by subclasses.
	 * @return true if this content pane would like to be skipped as previous content pane; false otherwise
	 */
	public boolean isSkippedAsPreviousContentPane()
	{
		return false;
	}

	/**
	 * Shows the next content pane in the dialog if it exists. Otherwise, the dialog is closed according
	 * to the default ON_CLICK operation. If no ON_CLICK operation is set, nothing is done by default.
	 * If the content pane exists, its checkUpdate() method is interpreted and the errors are handled.
	 * @return if a move to the next content pane was done; false if no next content pane does exist
	 * or if it refused to update the dialog
	 */
	public final boolean moveToNextContentPane()
	{
		return moveToContentPane(true);
	}

	/**
	 * Returns the content pane where elements may be placed freely.
	 * @return the content pane
	 */
	public final JComponent getContentPane()
	{
		return m_contentPane;
	}

	/**
	 * Set the parent dialog visible.
	 */
	public final void showDialog()
	{
		if (m_parentDialog instanceof JDialog)
		{
			((JDialog)m_parentDialog).setVisible(true);
		}
		else
		{
			((JAPDialog)m_parentDialog).setVisible(true);
		}
	}

	/**
	 * Replace the content pane of this content pane by another one.
	 * @param a_contentPane JComponent
	 */
	public final void setContentPane(JComponent a_contentPane)
	{
		GridBagConstraints contraints = new GridBagConstraints();
		contraints.gridx = 0;
		contraints.gridy = 3;
		contraints.weightx = 1;
		//contraints.weighty = 1;
		contraints.weighty = 0;
		contraints.anchor = GridBagConstraints.NORTH;
		contraints.fill = GridBagConstraints.BOTH;

		if (m_contentPane != null)
		{
			m_titlePane.remove(m_contentPane);
		}
		m_titlePane.add(a_contentPane, contraints);
		m_contentPane = a_contentPane;
	}

	/**
	 * Returns the help context or null if no help context is provided by this object.
	 * @return the help context or null if no help context is provided by this object
	 */
	public final String getHelpContext()
	{
		if (m_helpContext == null)
		{
			return null;
		}

		return m_helpContext.getHelpContext();
	}

	public final void clearStatusMessage(int a_messageID)
	{
		if (m_idStatusMessage == a_messageID)
		{
			clearStatusMessage();
		}
	}

	/**
	 * Resets the text in the status message line to an empty String.
	 */
	public final void clearStatusMessage()
	{
		if (m_lblMessage != null)
		{
			m_lblMessage.setText("T");
			m_lblMessage.setPreferredSize(m_lblMessage.getPreferredSize());
			m_lblMessage.removeMouseListener(m_linkedDialog);
			m_linkedDialog = null;
			m_lblMessage.setText("");
			m_lblMessage.setToolTipText(null);
		}
	}

	/**
	 * Prints an information message in the status bar. If the status bar is not available, a dialog window
	 * is opened. If the text is too long for the status bar, the text is cut and the user can see it by
	 * clicking on the stauts bar (a dialog window opens).
	 * @param a_message an information message
	 */
	public final void printStatusMessage(String a_message)
	{
		printStatusMessage(a_message, MESSAGE_TYPE_INFORMATION);
	}

	/**
	 * Prints a status message in the status bar. If the status bar is not available, a dialog window
	 * is opened. If the text is too long for the status bar, the text is cut and the user can see it by
	 * clicking on the stauts bar (a dialog window opens).
	 * @param a_message a status message
	 * @param a_messageType the message type; this has a influence on how the message is displayed
	 * (color, icon,...).
	 */
	public final void printStatusMessage(String a_message, int a_messageType)
	{
		if (m_lblMessage != null)
		{
			printStatusMessageInternal(a_message, a_messageType);
		}
		else
		{
			JAPDialog.showConfirmDialog(getContentPane(), a_message, OPTION_TYPE_DEFAULT, a_messageType);
		}
	}

	/**
	 * Prints an error message in the status bar. If the status bar is not available, a dialog window
	 * is opened. If the text is too long for the status bar, the text is cut and the user can see it by
	 * clicking on the stauts bar (a dialog window opens).
	 * @param a_logType the log type of this error
	 * @param a_throwable a Throwable that has been catched in the context of this error
	 */
	public final int printErrorStatusMessage(int a_logType, Throwable a_throwable)
	{
		return printErrorStatusMessage(null, a_logType, a_throwable);
	}


	/**
	 * Prints an error message in the status bar. If the status bar is not available, a dialog window
	 * is opened. If the text is too long for the status bar, the text is cut and the user can see it by
	 * clicking on the stauts bar (a dialog window opens).
	 * @param a_message an error message
	 * @param a_logType the log type of this error
	 */
	public final int printErrorStatusMessage(String a_message, int a_logType)
	{
		return printErrorStatusMessage(a_message, a_logType, null);
	}

	/**
	 * Prints an error message in the status bar. If the status bar is not available, a dialog window
	 * is opened. If the text is too long for the status bar, the text is cut and the user can see it by
	 * clicking on the stauts bar (a dialog window opens).
	 * @param a_message an error message
	 * @param a_logType the log type of this error
	 * @param a_throwable a Throwable that has been catched in the context of this error
	 */
	public final int printErrorStatusMessage(String a_message, int a_logType, Throwable a_throwable)
	{
		return printErrorStatusMessage(a_message, a_logType, a_throwable, true);
	}

	/**
	 * Calls validate() on the dialog.
	 */
	public final void validateDialog()
	{
		if (getDialog() instanceof JDialog)
		{
			( (JDialog) getDialog()).validate();
		}
		else
		{
			( (JAPDialog) getDialog()).validate();
		}
	}

	/**
	 * Replaces the content pane of the parent dialog with the content defined in this object.
	 * @return the errors returned by checkUpdate() or null or an empty array if no errors occured and
	 * the update has been done
	 */
	public final synchronized CheckError[] updateDialog()
	{
		return updateDialog(MIN_TEXT_WIDTH, true);
	}

	/**
	 * Replaces the content pane of the parent dialog with the content defined in this object.
	 * @param a_bCheckUpdate if checkUpdate should be called
	 * @return the errors returned by checkUpdate() or null or an empty array if no errors occured and
	 * the update has been done
	 */
	private final synchronized CheckError[] updateDialog(boolean a_bCheckUpdate)
	{
		return updateDialog(MIN_TEXT_WIDTH, a_bCheckUpdate);
	}

	/**
	 * Logs an error message an optionally prints it to the status bar.
	 * If the status bar is not available, a dialog window
	 * is opened. If the text is too long for the status bar, the text is cut and the user can see it by
	 * clicking on the stauts bar (a dialog window opens).
	 * @param a_message an error message
	 * @param a_logType the log type of this error
	 * @param a_throwable a Throwable that has been catched in the context of this error
	 * @param a_bShow if the message is shown to the user or logged only
	 */
	private int printErrorStatusMessage(String a_message, int a_logType, Throwable a_throwable,
								 boolean a_bShow)
	{
		boolean bPossibleApplicationError = false;
		int idStatusMessage = 0;

		try {

			a_message = JAPDialog.retrieveErrorMessage(a_message, a_throwable);
			if (a_message == null)
			{
				a_message = JAPMessages.getString(JAPDialog.MSG_ERROR_UNKNOWN);
				bPossibleApplicationError = true;
			}
			if (!LogType.isValidLogType(a_logType))
			{
				a_logType = LogType.GUI;
			}


			if (m_lblMessage != null)
			{
				if (a_bShow)
				{
					idStatusMessage = printStatusMessageInternal(a_message, MESSAGE_TYPE_ERROR);
				}
				LogHolder.log(LogLevel.ERR, a_logType, a_message, true);
				if (a_throwable != null)
				{
					// the exception is only shown in debug mode or in case of an application error
					if (bPossibleApplicationError)
					{
						LogHolder.log(LogLevel.ERR, a_logType, a_throwable);
					}
					else
					{
						LogHolder.log(LogLevel.DEBUG, a_logType, a_throwable);
					}
				}
			}
			else
			{
				// no status bar is available; show the error message in an extra dialog
				if (a_bShow)
				{
					JAPDialog.showErrorDialog(getContentPane(), a_message, a_logType, a_throwable);
				}
			}
		}
		catch (Throwable a_e)
		{
			JAPDialog.showErrorDialog(getContentPane(), LogType.GUI, a_e);
		}
		return idStatusMessage;
	}


	/**
	 * Replaces the content pane of the parent dialog with the content defined in this object.
	 * @return the errors returned by checkUpdate() or null or an empty array if no errors occured and
	 * the update has been done
	 * @param a_maxTextWidth the maximum width that is allowed for the (optional) text field
	 * @param a_bCallCheckUpdate if checkUpdate should be called
	 */
	private final synchronized CheckError[] updateDialog(int a_maxTextWidth, boolean a_bCallCheckUpdate)
	{
		JOptionPane pane;
		Object[] options;
		CheckError[] errors;

		if (isDisposed())
		{
			return null;
		}

		if (a_bCallCheckUpdate)
		{
			errors = checkUpdate();
		}
		else
		{
			errors = null;
		}

		if (errors != null && errors.length > 0)
		{
			return errors;
		}

		createOptions();
		options = new Object[1];
		options[0] = m_panelOptions;
		if (m_lblText != null)
		{
			m_titlePane.remove(m_lblText);
		}

		pane = new JOptionPane(m_rootPane, m_messageType, 0, m_icon, options );
		if (m_tempDialog != null)
		{
			m_tempDialog.dispose();
		}
		m_tempDialog = pane.createDialog(null, "");
		m_tempDialog.pack();
		if (m_lblText != null)
		{
			if (isDialogVisible())
			{
				// the dialog is visible and will resize the label automatically as needed
				m_lblText = new JAPHtmlMultiLineLabel(m_lblText.getText(), m_lblText.getFont(),
					SwingConstants.CENTER);
			}
			else
			{
				if (m_lblText.getPreferredSize().width > (m_contentPane.getWidth() - 2 * SPACE_AROUND_TEXT))
				{
					// the width of the label must be restricted to make the pack() operation possible
					m_lblText.setPreferredWidth(Math.max(m_lblText.getMinimumSize().width,
						Math.max(m_contentPane.getWidth() - 2 * SPACE_AROUND_TEXT, a_maxTextWidth)));
				}
			}

			m_titlePane.add(m_lblText, m_textConstraints);
		}

		clearStatusMessage();

		// initialize the new content pane
		if (m_currentlyActiveContentPane != null)
		{
			m_currentlyActiveContentPane.removeComponentListener(
						 m_currentlyActiveContentPaneComponentListener);
		}
		m_currentlyActiveContentPane = m_tempDialog.getContentPane();
		m_currentlyActiveContentPaneComponentListener = new ContentPaneComponentListener();
		m_currentlyActiveContentPane.addComponentListener(m_currentlyActiveContentPaneComponentListener);
		m_parentDialog.setContentPane(m_currentlyActiveContentPane);

		if (isDialogVisible())
		{
			// tell the listeners that the content pane is visible
			Vector listeners = (Vector)m_componentListeners.clone();
			for (int i = 0; i < listeners.size(); i++)
			{
				( (ComponentListener) listeners.elementAt(i)).componentShown(
					new ComponentEvent(m_currentlyActiveContentPane,
									   ComponentEvent.COMPONENT_SHOWN));
			}
		}


		// set default button
		if (m_defaultButton == DEFAULT_BUTTON_OK)
		{
			getDialog().getRootPane().setDefaultButton(getButtonYesOK());
		}
		else if (m_defaultButton == DEFAULT_BUTTON_CANCEL)
		{
			getDialog().getRootPane().setDefaultButton(getButtonCancel());
		}
		else if (m_defaultButton == DEFAULT_BUTTON_NO)
		{
			getDialog().getRootPane().setDefaultButton(getButtonNo());
		}
		else if (m_defaultButton == DEFAULT_BUTTON_HELP)
		{
			getDialog().getRootPane().setDefaultButton(getButtonHelp());
		}
		else if (m_defaultButton != DEFAULT_BUTTON_KEEP)
		{
			getDialog().getRootPane().setDefaultButton(null);
		}


		m_titlePane.invalidate();
		if (m_lblText != null)
		{
			m_lblText.invalidate();
		}
		m_rootPane.invalidate();
		m_contentPane.invalidate();
		if (m_parentDialog instanceof JAPDialog)
		{
			((JAPDialog) m_parentDialog).validate();

		}
		else
		{
			((JDialog) m_parentDialog).validate();
		}

		return null;
	}

	/**
	 * Returns the "Help" button.
	 * @return the "Help" button
	 */
	public final JButton getButtonHelp()
	{
		return m_btnHelp;
	}

	/**
	 * Returns the "Cancel" button.
	 * @return the "Cancel" button
	 */
	public final JButton getButtonCancel()
	{
		return m_btnCancel;
	}

	/**
	 * Returns the "Yes" or "OK" button.
	 * @return the "Yes" or "OK" button
	 */
	public final JButton getButtonYesOK()
	{
		return m_btnYesOK;
	}

	/**
	 * Returns the "No" button.
	 * @return the "No" button
	 */
	public final JButton getButtonNo()
	{
		return m_btnNo;
	}

	/**
	 * Defines the button to be set as default button of the dialog when updateDialog() is called.
	 * If the content pane has a wizard layout, this setting is ignored. The default behaviour (done by the
	 * constructor) is that it is first tried to make the OK button the default button. If it is not
	 * available, the CANCEL button and the HELP buttons are tried. If those are not available, too, then
	 * no default button is set.
	 * @param a_defaultButton the button to be set as default button of the dialog when updateDialog() is
	 * called, e.g. DEFAULT_BUTTON_OK or DEFAULT_BUTTON_HELP;
	 * DEFAULT_BUTTON_EMPTY will set no button as default (null), DEFAULT_BUTTON_KEEP will keep whatever
	 * has been set before
	 */
	public final void setDefaultButton(int a_defaultButton)
	{
		if (a_defaultButton < DEFAULT_BUTTON_EMPTY || a_defaultButton > DEFAULT_BUTTON_KEEP)
		{
			m_defaultButton = DEFAULT_BUTTON_EMPTY;
		}
		else
		{
			m_defaultButton = a_defaultButton;
		}
	}

	/**
	 * Returns the button to be set as default button of the dialog when updateDialog() is called.
	 * @return the button to be set as default button of the dialog when updateDialog() is called,
	 * e.g. DEFAULT_BUTTON_OK
	 */
	public final int getDefaultButton()
	{
		return m_defaultButton;
	}

	/**
	 * Returns what happens if one of the buttons is clicked. Several actions can be combined,
	 * for example ON_CLICK_DISPOSE_DIALOG | ON_YESOK_SHOW_NEXT_CONTENT will dispose the dialog on
	 * "Cancel" and "No" but will show the next content pane on "Yes" or "OK". The ON_CLICK operation
	 * definitions are always overwritten by the button-specific operation definitions. If no operation
	 * is defined for a button, it will not set a value on click automatically, that means the dialog
	 * will keep its state if no one else sets the value by calling <CODE> setValue() </CODE>.
	 * @return what happens if one of the buttons is clicked
	 */
	public final int getDefaultButtonOperation()
	{
		return m_defaultButtonOperation;
	}

	/**
	 * Defines what happens if one of the buttons is clicked. Several actions can be combined,
	 * for example ON_CLICK_DISPOSE_DIALOG | ON_YESOK_SHOW_NEXT_CONTENT will dispose the dialog on
	 * "Cancel" and "No" but will show the next content pane on "Yes" or "OK". The ON_CLICK operation
	 * definitions are always overwritten by the button-specific operation definitions. If no operation
	 * is defined for a button, it will not set a value on click automatically, that means the dialog
	 * will keep its state if no one else sets the value by calling <CODE> setValue() </CODE>.
	 * May throw an InvalidArgumentException if objects of this type do not support setting the default
	 * button operation.
	 * <P> It is a good idea to set additional button operations preserving the old ones, for example
	 * ON_CANCEL_DISPOSE_DIALOG | getDefaultButtonOperation(). Single button operations may be removed by,
	 * for example, getDefaultButtonOperation() - ON_CANCEL_DISPOSE_DIALOG, but before that make
	 * sure that (getDefaultButtonOperation() & ON_CANCEL_DISPOSE_DIALOG) == ON_CANCEL_DISPOSE_DIALOG returns
	 * <CODE> true </CODE>. </P>
	 * @param a_defaultButtonOperation the default button operation
	 * @throws java.lang.IllegalArgumentException if objects of this type do not support setting the default
	 * button operation
	 */
	public final void setDefaultButtonOperation(int a_defaultButtonOperation)
		throws IllegalArgumentException
	{
		m_defaultButtonOperation = a_defaultButtonOperation;
	}

	/**
	 * Optional return value that may be created by the content pane during its visibility.
	 * @return return value that may be created by the content pane during its visibility
	 */
	public Object getValue()
	{
		return null;
	}

	/**
	 * Returns the button value the user has selected.
	 * @return the button value the user has selected
	 */
	public final int getButtonValue()
	{
		return m_value;
	}

	/**
	 * Sets the button value. If the type is unknown, it is set to RETURN_VALUE_UNINITIALIZED.
	 * @param a_value the new button value
	 */
	public final void setButtonValue(int a_value)
	{
		if (RETURN_VALUE_CANCEL == a_value || RETURN_VALUE_OK == a_value ||
			RETURN_VALUE_CLOSED == a_value || RETURN_VALUE_YES == a_value || RETURN_VALUE_NO == a_value)
		{
			m_value = a_value;
		}
		else
		{
			m_value = RETURN_VALUE_UNINITIALIZED;
		}
	}

	/**
	 * Returns if <CODE> getValue() </CODE> returns an other value than RETURN_VALUE_CANCEL,
	 * RETURN_VALUE_CLOSED or RETURN_VALUE_UNINITIALIZED.
	 * @return if <CODE> getValue() </CODE> returns an other value than RETURN_VALUE_CANCEL,
	 * RETURN_VALUE_CLOSED or RETURN_VALUE_UNINITIALIZED
	 */
	public final boolean hasValidValue()
	{
		return getButtonValue() != RETURN_VALUE_CANCEL && getButtonValue() != RETURN_VALUE_CLOSED &&
			getButtonValue() != RETURN_VALUE_UNINITIALIZED;
	}

	/**
	 * Returns if the automatic focus setting of the DialogContentPane class is enabled.
	 * Subclasses may override this method if they want to set the focus in their internal logic.
	 * @return if this content pane sets the focus automatically
	 */
	public boolean isAutomaticFocusSettingEnabled()
	{
		return true;
	}

	/**
	 * Returns if this content pane is the currently active content pane in the dialog, that means that
	 * this content pane is shown if the dialog is shown.
	 * @return if this content pane is the currently active content pane in the dialog
	 */
	public final boolean isActive()
	{
		return m_currentlyActiveContentPane != null &&
			m_parentDialog.getContentPane() == m_currentlyActiveContentPane;
	}

	/**
	 * Returns if the content pane is part of a visible dialog.
	 * @return if the content pane is part of a visible dialog
	 */
	public final boolean isVisible()
	{
		return isActive() && isDialogVisible();
	}

	/**
	 * Returns the text that is displayed in the content pane.
	 * @return the text that is displayed in the content pane or <Code>null</Code> if no text is displayed
	 */
	public String getText()
	{
		return m_strText;
	}

	/**
	 * Use this method to set a new text for the content pane. Note that this is only possible if
	 * <UL>
	 *   <LI> the content pane is not part of a visible dialog (isVisible() == false) and </LI>
	 *   <LI> the content pane already has a text field (maybe a dummy String) and </LI>
	 *   <LI> the parent dialog has a width an height greater than zero (it already has its final size). </LI>
	 * </UL>
	 * The new text will not influence the current size or the preferred size of the content pane and
	 * the dialog. If the text is too big to show it completely, a link is generated that opens an
	 * extra dialog to view the text. HINT: Please use this method ONLY if the whole layout ouf the
	 * content pane has already been set. Otherwise, the text size cannot be calculated correctly if the
	 * text is too big for the dialog!
	 * @param a_strText a new text for this content pane
	 */
	public synchronized void setText(String a_strText)
	{
		if (m_strText == null)
		{
			throw new IllegalStateException("This content pane does not contain a text field!");
		}

		synchronized (getDialog())
		{
			if (isVisible())
			{
				getDialog().notifyAll();
				throw new IllegalStateException("You may not change the text in a visible content pane!");
			}

			JAPDialog dialog = new JAPDialog( (JAPDialog)null, "");
			RootPaneContainer ownerDialog = getDialog();
			boolean bWasActive = isActive();
			boolean bWasUnlimitedSize = false;
			// do not use m_contentPane as this may have a size of zero...
			int preferredWidth;

			int currentCut, bestCut;
			int totalLength;
			boolean bCutFound = false;
			GridBagConstraints contraints;
			Dimension dialogSize;

			if (ownerDialog instanceof JDialog)
			{
				dialogSize = ((JDialog)ownerDialog).getSize();
			}
			else
			{
				dialogSize = ((JAPDialog)ownerDialog).getSize();
			}
			if (dialogSize.width == 0 || dialogSize.height == 0)
			{
				throw new IllegalStateException("The parent dialog has a size <=0! " +
												"This is not allowed when changing the text.");
			}
			dialog.setSize(dialogSize);

			//preferredWidth = getDialogContentPane().getSize().width - 2 * SPACE_AROUND_TEXT;


			// remove and add the text field, as it may have been removed in a prior call to this method
			if (m_lblText != null)
			{
				m_titlePane.remove(m_lblText);
			}
			m_strText = JAPHtmlMultiLineLabel.removeHTMLHEADAndBODYTags(a_strText);
			if (m_strText == null || m_strText.trim().length() == 0)
			{
				// remove it an do nothing else
				m_strText = "";
				return;
			}
			m_lblText = new JAPHtmlMultiLineLabel(m_strText, SwingConstants.CENTER);
			m_lblText.setFontStyle(JAPHtmlMultiLineLabel.FONT_STYLE_PLAIN);
			//m_lblText.setPreferredWidth(preferredWidth);
			m_titlePane.add(m_lblText, m_textConstraints);


			// replace the parent dialog by a dummy dialog to fool the updateDialog() method
			m_parentDialog = dialog;

			if (m_rootPane.getPreferredSize().equals(new Dimension(UNLIMITED_SIZE, UNLIMITED_SIZE)))
			{
				bWasUnlimitedSize = true;
			}
			m_rootPane.setPreferredSize(null);
			if (m_lblSeeFullText != null)
			{
				m_titlePane.remove(m_lblSeeFullText);
				m_lblSeeFullText = null;
			}
			updateDialog(false);

			m_lblText.setText(m_strText);
			m_lblText.setPreferredWidth(getContentPane().getSize().width);
			contraints = (GridBagConstraints)m_textConstraints.clone();
			contraints.gridy = 2;
			contraints.insets = new Insets(0, 0, 0, 0);
			// add dummy label to set optimal width
			m_lblSeeFullText = new JAPHtmlMultiLineLabel();
			m_lblSeeFullText.setPreferredSize(new Dimension(getContentPane().getSize().width, 0));
			////m_lblSeeFullText.setPreferredWidth(getContentPane().getSize().width);
			m_titlePane.add(m_lblSeeFullText, contraints);
			updateDialog(false);
			m_titlePane.remove(m_lblSeeFullText);
			if (dialog.getContentPane().getSize().height < dialog.getContentPane().getPreferredSize().height)
			{
				int height = dialog.getSize().height;
				int width = dialog.getSize().width;
				dialog.pack();
				if (dialog.getSize().height > dialogSize.height * 1.2 ||
					dialog.getSize().width > dialogSize.width * 1.2 )
				{
					dialog.setSize(width, height);
				}
				else
				{
					if (ownerDialog instanceof JDialog)
					{
						( (JDialog) ownerDialog).setSize(dialog.getSize());
					}
					else
					{
						( (JAPDialog) ownerDialog).setSize(dialog.getSize());
					}
				}
			}


			if (dialog.getContentPane().getSize().height < dialog.getContentPane().getPreferredSize().height)
			{
				// OK, text height is too big to display
				m_lblSeeFullText = new JAPHtmlMultiLineLabel(
								"<A href=''>" + //"..." +
								"(" + JAPMessages.getString(MSG_SEE_FULL_MESSAGE) + ")</A>", m_lblText.getFont(),
								SwingConstants.CENTER);
				m_lblSeeFullText.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
				// set the with of the label so that is has the current width of the content pane
				m_lblSeeFullText.setPreferredSize(new Dimension(getContentPane().getSize().width,
					m_lblSeeFullText.getPreferredSize().height));
				m_lblSeeFullText.addMouseListener(new MouseAdapter()
				{
					public void mouseClicked(MouseEvent a_event)
					{
						if (m_strTitle != null)
						{
							JAPDialog.showMessageDialog(m_lblSeeFullText, m_strText, m_strTitle);
						}
						else
						{
							JAPDialog.showMessageDialog(m_lblSeeFullText, m_strText);
						}
					}
				});

				m_titlePane.add(m_lblSeeFullText, contraints);

				// do a short heuristic to optimize the label size (find the optimal cut)
				totalLength = m_lblText.getHTMLDocumentLength();
				currentCut = totalLength / 2;
				bestCut = 0;
				for (int i = 0; i < NUMBER_OF_HEURISTIC_ITERATIONS; i++)
				{
					if (i == (NUMBER_OF_HEURISTIC_ITERATIONS - 1))
					{
						if (bCutFound)
						{
							currentCut = bestCut;
						}
						else
						{
							// no feasible cut has been found; remove the text field completely
							m_titlePane.remove(m_lblText);
							m_lblText = null;
							updateDialog(false);
							break;
						}
					}
					m_lblText.setText(m_strText);
					m_lblText.cutHTMLDocument(currentCut);
					m_lblText.setText(JAPHtmlMultiLineLabel.removeHTMLHEADAndBODYTags(m_lblText.getText()) +
									  "...");
					updateDialog(false);
					if (dialog.getContentPane().getSize().height <
						dialog.getContentPane().getPreferredSize().height)
				   {
					   if (bCutFound)
					   {
						   currentCut = bestCut + (bestCut / (i + 2));
					   }
					   else
					   {
						   currentCut /= 2;
					   }
				   }
				   else
				   {
					   bCutFound = true;
					   if (bestCut < currentCut)
					   {
						   bestCut = currentCut;
					   }
					   currentCut += currentCut / 2;
				   }
				}
				if (bCutFound && bestCut >= totalLength)
				{
					// found an illegal cut; happens in JDK 1.3.x if everything fits in; Why??
					m_lblText.setText(m_strText);
					m_titlePane.remove(m_lblSeeFullText);
				}
			}

			if (m_lblText != null)
			{
				m_lblText.setText("<font color=#000000>" +
								  JAPHtmlMultiLineLabel.removeHTMLHEADAndBODYTags(m_lblText.getText())
								  + "</font>");
				//m_lblText.setPreferredWidth(preferredWidth);
			}


			m_parentDialog = ownerDialog;
			if (bWasUnlimitedSize)
			{
				// restore the preferred size of the root pane
				m_rootPane.setPreferredSize(new Dimension(UNLIMITED_SIZE, UNLIMITED_SIZE));
			}
			if (bWasActive)
			{
				updateDialog(false);
			}

			getDialog().notifyAll();
		}
	}

	/**
	 * Returns the parent Dialog. It is a JDialog or a JAPDialog.
	 * @return the parent Dialog
	 */
	public RootPaneContainer getDialog()
	{
		return m_parentDialog;
	}

	private Container getDialogContentPane()
	{
		if (m_parentDialog instanceof JAPDialog)
		{
			return ((JAPDialog) m_parentDialog).getContentPane();
		}
		return ((JDialog) m_parentDialog).getContentPane();
	}

	/**
	 * Returns if the parent dialog is visible. If your content pane contains a text, please do not
	 * perform a pack() operation on a visible dialog as there is a high possibility that the
	 * auto-formatting feature will not work. Packing is recommended on invisible dialogs only.
	 * @return if the parent dialog is visible
	 */
	public final boolean isDialogVisible()
	{
		return ((m_parentDialog instanceof JAPDialog && ( (JAPDialog) m_parentDialog).isVisible()) ||
				(m_parentDialog instanceof JDialog && ( (JDialog) m_parentDialog).isVisible()));
	}

	/**
	 * Adds a window listener to the parent dialog.
	 * @param a_listener a WindowListener
	 */
	public void addDialogWindowListener(WindowListener a_listener)
	{
		if (m_parentDialog instanceof JDialog)
		{
			((JDialog)m_parentDialog).addWindowListener(a_listener);
		}
		else
		{
			((JAPDialog)m_parentDialog).addWindowListener(a_listener);
		}
	}

	/**
	 * Adds a component listener. It it called in the following situations:
	 * <UL>
	 * <LI> componentShown: 1) Dialog window is opened or set to visible and the content pane is its
	 * current content pane. (Please note that with a JAPDialog no event is generated when the dialog is
	 * closed and made visible a second time.)
	 * 2) The Dialog window is already visible and is successfully updated with the
	 * current content pane. </LI>
	 * <LI> componentHidden: Dialog window is closed or set invisible, the content pane is set invisible
	 * while the dialog is visible, or the contentPane is shown in a JAPDialog (this is a hack to
	 * generate the componentShown event). </LI>
	 * <LI> componentResized: componentResized is called on the content pane </LI>
	 * <LI> componentMoved: componentMoved is called on the content pane </LI>
	 * </UL>
	 * The componentShown method is extremely useful if you wnat to to a specific action when the content
	 * pane is shown to the user, for example setting a focus on a special component or starting a thread.
	 * @param a_listener a ComponentListener
	 * @todo Find a way so that the componentShown method has full functionality in JDialogs; is there a way?
	 * I propose to use JAPDialog if you want this...
	 */
	public synchronized void addComponentListener(ComponentListener a_listener)
	{
		if (a_listener != null)
		{
			m_componentListeners.addElement(a_listener);
		}
	}

	/**
	 * Removes a component listener.
	 * @param a_listener a ComponentListener
	 */
	public synchronized void removeComponentListener(ComponentListener a_listener)
	{
		m_componentListeners.removeElement(a_listener);
	}

	/**
	 * Adds a component listener to the parent dialog.
	 * @param a_listener a ComponentListener
	 */
	public void addDialogComponentListener(ComponentListener a_listener)
	{
		if (m_parentDialog instanceof JDialog)
		{
			((JDialog)m_parentDialog).addComponentListener(a_listener);
		}
		else
		{
			((JAPDialog)m_parentDialog).addComponentListener(a_listener);
		}
	}

	/**
	 * Removes a component listener from the parent dialog.
	 * @param a_listener a ComponentListener
	 */
	public void removeDialogComponentListener(ComponentListener a_listener)
	{
		if (m_parentDialog instanceof JDialog)
		{
			((JDialog)m_parentDialog).removeComponentListener(a_listener);
		}
		else
		{
			((JAPDialog)m_parentDialog).removeComponentListener(a_listener);
		}
	}


	/**
	 * Removes a window listener from the parent dialog.
	 * @param a_listener a WindowListener
	 */
	public void removeDialogWindowListener(WindowListener a_listener)
	{
		if (m_parentDialog instanceof JDialog)
		{
			((JDialog)m_parentDialog).removeWindowListener(a_listener);
		}
		else
		{
			((JAPDialog)m_parentDialog).removeWindowListener(a_listener);
		}
	}

	public boolean isDisposed()
	{
		return m_bDisposed;
	}

	public synchronized void dispose()
	{
		m_bDisposed = true;

		if (m_tempDialog != null)
		{
			m_tempDialog.dispose();
		}
		if (m_titlePane != null)
		{
			m_titlePane.removeAll();
		}
		m_titlePane = null;
		if (m_rootPane != null)
		{
			m_rootPane.removeAll();
		}
		m_rootPane = null;
		if (m_contentPane != null)
		{
			m_contentPane.removeAll();
		}
		m_contentPane = null;
		if (m_panelOptions != null)
		{
			m_panelOptions.removeAll();
		}
		m_panelOptions = null;
		m_parentDialog = null;
		m_lblText = null;
		m_componentListeners.removeAllElements();
		if (m_btnCancel != null)
		{
			m_btnCancel.removeActionListener(m_buttonListener);
		}
		if (m_btnYesOK != null)
		{
			m_btnYesOK.removeActionListener(m_buttonListener);
		}
		if (m_btnNo != null)
		{
			m_btnNo.removeActionListener(m_buttonListener);
		}
		if (m_btnHelp != null)
		{
			m_btnHelp.removeActionListener(m_buttonListener);
		}

		m_buttonListener = null;
		if (m_currentlyActiveContentPane != null)
		{
			m_currentlyActiveContentPane.removeComponentListener(
				m_currentlyActiveContentPaneComponentListener);
		}
		m_currentlyActiveContentPaneComponentListener = null;
		m_currentlyActiveContentPane = null;
		m_nextContentPane = null;
		m_previousContentPane = null;
	}

	/**
	 * Hides or disposed the parent dialog.
	 * @param a_bDispose if true, the dialog is disposed; otherwise, it is only hidden
	 */
	public final void closeDialog(boolean a_bDispose)
	{
		try
		{
			if (a_bDispose)
			{
				if (m_parentDialog instanceof JDialog)
				{
					( (JDialog) m_parentDialog).dispose();
				}
				else
				{
					try
					{
						( (JAPDialog) m_parentDialog).dispose();
					}
					catch (IllegalMonitorStateException a_e)
					{
						LogHolder.log(LogLevel.DEBUG, LogType.GUI, a_e);
					}
				}
			}
			else
			{
				if (m_parentDialog instanceof JDialog)
				{
					( (JDialog) m_parentDialog).setVisible(false);
				}
				else
				{
					( (JAPDialog) m_parentDialog).setVisible(false);
				}
			}
		}
		catch (NullPointerException a_e)
		{
			if (!isDisposed())
			{
				throw a_e;
			}
		}
	}

	private JAPDialog getJAPDialog()
	{
		if (m_parentDialog instanceof JAPDialog)
		{
			return (JAPDialog)m_parentDialog;
		}
		return null;
	}

	private static Icon findMessageIcon(JOptionPane a_optionPane)
	{
		Container currentPanel;
		Icon icon = null;
		for (int i = 0; i < a_optionPane.getComponentCount(); i++)
		{
			if (a_optionPane.getComponent(i) instanceof Container)
			{
				currentPanel = (Container) a_optionPane.getComponent(i);
				for (int j = 0; j < currentPanel.getComponentCount(); j++)
				{
					if (currentPanel.getComponent(j) instanceof JLabel)
					{
						icon = ( (JLabel) currentPanel.getComponent(j)).getIcon();
						break;
					}
				}
			}
		}
		return icon;
	}


	private final synchronized int printStatusMessageInternal(String a_strMessage, int a_messageType)
	{
		String strMessage;
		String strColor;
		String strHref;
		JAPHtmlMultiLineLabel dummyLabel;
		int bestSize, currentSize;

		if (a_strMessage == null || a_strMessage.trim().length() == 0)
		{
			return 0;
		}

		// no HTML Tags are allowed in the message
		strMessage = JAPHtmlMultiLineLabel.removeTagsAndNewLines(a_strMessage);

		if (MESSAGE_TYPE_ERROR == a_messageType || MESSAGE_TYPE_WARNING == a_messageType)
		{
			strColor = "red";
		}
		else
		{
			strColor = "black";
		}

		dummyLabel = new JAPHtmlMultiLineLabel(strMessage, m_lblMessage.getFont());
		if (dummyLabel.getPreferredSize().width > m_lblMessage.getSize().width)
		{
			String strMessageTitle;
			if (MESSAGE_TYPE_ERROR == a_messageType)
			{
				strMessageTitle = JAPMessages.getString(JAPDialog.MSG_TITLE_ERROR);
			}
			else if (MESSAGE_TYPE_WARNING == a_messageType)
			{
				strMessageTitle = JAPMessages.getString(JAPDialog.MSG_TITLE_WARNING);
			}
			else
			{
				strMessageTitle = JAPMessages.getString(JAPDialog.MSG_TITLE_INFO);
			}

			clearStatusMessage();
			bestSize = 0;
			currentSize = strMessage.length() / 2;
			for (int i = 0; currentSize > 1; i++)
			{
				if (i >= NUMBER_OF_HEURISTIC_ITERATIONS && bestSize < strMessage.length())
				{
					break;
				}

				dummyLabel.setText(strMessage.substring(0, currentSize) + MORE_POINTS);
				if (dummyLabel.getPreferredSize().width <= m_lblMessage.getSize().width)
				{
					bestSize = Math.max(bestSize, currentSize) - 2; // substract '2' to fix small errors
					currentSize += (currentSize / (i + 2));
				}
				else
				{
					currentSize /= 2;
				}
			}

			if (bestSize <= 5)
			{
				strMessage = MORE_POINTS;
			}
			else
			{
				strMessage = strMessage.substring(0, bestSize) + MORE_POINTS;
			}

			strHref =  " href=\"\"";
			m_lblMessage.setToolTipText(JAPMessages.getString(MSG_SEE_FULL_MESSAGE));
			m_linkedDialog = new LinkedDialog(a_strMessage, strMessageTitle,
											  OPTION_TYPE_DEFAULT, a_messageType);
			m_lblMessage.addMouseListener(m_linkedDialog);
			m_lblMessage.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		}
		else
		{
			clearStatusMessage();
			strHref = "";
		}
		strMessage = "<A style=\"color:" + strColor + "\"" + strHref + "> " + strMessage + " </A>";
		m_lblMessage.setText(strMessage);
		m_lblMessage.revalidate();

		m_idStatusMessage++;
		return m_idStatusMessage;
	}

	private class LinkedDialog extends MouseAdapter
	{
		private String m_strMessage;
		private String m_strTitle;
		private int m_optionType;
		private int m_messageType;

		public LinkedDialog(String a_strMessage, String a_strTitle, int a_optionType, int a_messageType)
		{
			m_strMessage = a_strMessage;
			m_strTitle = a_strTitle;
			m_optionType = a_optionType;
			m_messageType = a_messageType;
		}

		public void mouseClicked(MouseEvent a_event)
		{
			JAPDialog.showConfirmDialog(m_lblMessage, m_strMessage, m_strTitle, m_optionType, m_messageType);
		}
	}

	private void setNextContentPane(DialogContentPane a_nextContentPane)
	{
		if (m_nextContentPane != null)
		{
			// remove the next content pane from the chain
			m_nextContentPane.m_previousContentPane = null;
		}
		m_nextContentPane = a_nextContentPane;
	}

	/**
	 * Shows a given content pane in the dialog if it exists. Otherwise, the dialog is closed according
	 * to the default ON_CLICK operation. If no ON_CLICK operation is set, nothing is done by default.
	 * If the content pane exists, its checkUpdate() method is interpreted and the errors are handled.
	 * @param a_bNext true if a move to the next content pane is done; false if a move to
	 * the previous content pane is done
	 * @return if a move to the given content pane was done; false if no previous content pane does exist
	 * or if it refused to update the dialog
	 */
	private boolean moveToContentPane(boolean a_bNext)
	{
		DialogContentPane currentContentPane;

		currentContentPane = this;
		if (a_bNext)
		{
			while ((currentContentPane = currentContentPane.getNextContentPane()) != null &&
				   currentContentPane.isSkippedAsNextContentPane());
		}
		else
		{
			while ((currentContentPane = currentContentPane.getPreviousContentPane()) != null &&
				   currentContentPane.isSkippedAsPreviousContentPane());
		}

		if (currentContentPane != null)
		{
			CheckError[] errors = currentContentPane.updateDialog();
			boolean bFocused = false;

			if (checkErrors(errors, m_rememberedUpdateErrors))
			{
				if (currentContentPane.isVisible())
				{
					if (a_bNext)
					{
						if (currentContentPane.getButtonYesOK() != null &&
							currentContentPane.getButtonYesOK().isEnabled())
						{
							if (currentContentPane.isAutomaticFocusSettingEnabled())
							{
								currentContentPane.getButtonYesOK().requestFocus();
							}
							getDialog().getRootPane().setDefaultButton(currentContentPane.getButtonYesOK());
							bFocused = true;
						}
					}
					else
					{
						if (currentContentPane.getButtonNo() != null &&
							currentContentPane.getButtonNo().isEnabled())
						{
							if (currentContentPane.isAutomaticFocusSettingEnabled())
							{
								//currentContentPane.getButtonNo().requestFocus();
								currentContentPane.getButtonYesOK().requestFocus();
							}
							//getDialog().getRootPane().setDefaultButton(currentContentPane.getButtonNo());
							getDialog().getRootPane().setDefaultButton(currentContentPane.getButtonYesOK());
							bFocused = true;
						}
						else if (currentContentPane.getButtonYesOK() != null &&
								 currentContentPane.getButtonYesOK().isEnabled())
						{
							if (currentContentPane.isAutomaticFocusSettingEnabled())
							{
								currentContentPane.getButtonYesOK().requestFocus();
							}
							getDialog().getRootPane().setDefaultButton(currentContentPane.getButtonYesOK());
							bFocused = true;
						}
					}
					if (!bFocused)
					{
						if (currentContentPane.getButtonCancel() != null &&
							currentContentPane.getButtonCancel().isEnabled())
						{
							if (currentContentPane.isAutomaticFocusSettingEnabled())
							{
								currentContentPane.getButtonCancel().requestFocus();
							}
							getDialog().getRootPane().setDefaultButton(currentContentPane.getButtonCancel());
						}
					}
				}
				return true;
			}
			else
			{
				// errors occured
				return false;
			}
		}
		else
		{
			if ((getDefaultButtonOperation() & ON_CLICK_DISPOSE_DIALOG) > 0)
			{
				closeDialog(true);
			}
			else if ((getDefaultButtonOperation() & ON_CLICK_HIDE_DIALOG) > 0)
			{
				closeDialog(false);
			}
			return false;
		}
	}

	/**
	 * Undos all error actions of the remembered errors, checks if the given array of errors contains
	 * one or more errors and adds those errors to the vector
	 * of remembered errors.
	 * @param a_errors CheckError[]
	 * @param a_rememberedErrors Vector
	 * @return false if the given array of errors contains one or more errors
	 */
	private boolean checkErrors(CheckError[] a_errors, Vector a_rememberedErrors)
	{
		for (int i = a_rememberedErrors.size() - 1; i >= 0; i--)
		{
			( (CheckError) a_rememberedErrors.elementAt(i)).undoErrorAction();
			a_rememberedErrors.removeElementAt(i);
		}

		if (a_errors != null && a_errors.length > 0)
		{
			CheckError displayError = null;

			for (int i = 0; i < a_errors.length; i++)
			{
				if (a_errors[i] == null)
				{
					LogHolder.log(LogLevel.ERR, LogType.GUI, "Found a " + CheckError.class.getName() + " " +
								  "that is null! Ignoring it.");
					continue;
				}

				if (displayError == null || a_errors[i].hasDisplayableErrorMessage() && (
					displayError.getMessage() == null || displayError.getMessage().trim().length() == 0))
				{
					displayError = a_errors[i];
				}
				if (JAPDialog.retrieveErrorMessage(
					a_errors[i].getMessage(), a_errors[i].getThrowable()) != null)
				{
					printErrorStatusMessage(a_errors[i].getMessage(), a_errors[i].getLogType(),
											a_errors[i].getThrowable(), false);
				}
				a_rememberedErrors.addElement(a_errors[i]);
				a_errors[i].doErrorAction();
			}
			if (displayError == null)
			{
				printStatusMessage(JAPMessages.getString(MSG_OPERATION_FAILED), MESSAGE_TYPE_ERROR);
			}
			else
			{
				printStatusMessage(JAPDialog.retrieveErrorMessage(
								displayError.getMessage(), displayError.getThrowable()), MESSAGE_TYPE_ERROR);
			}
			return false;
		}
		return true;
	}


	private void createDefaultOptions()
	{
		m_panelOptions = new JPanel();

		if (m_btnCancel == null)
		{
			m_btnCancel = new JButton();
			m_btnCancel.addActionListener(m_buttonListener);
		}
		m_btnCancel.setText(JAPMessages.getString(MSG_CANCEL));
		// the cancel button is always the first one if present
		m_panelOptions.add(m_btnCancel);
		m_btnCancel.setVisible(false);


		if (OPTION_TYPE_YES_NO_CANCEL == m_optionType || OPTION_TYPE_OK_CANCEL == m_optionType ||
			OPTION_TYPE_CANCEL == m_optionType)
		{
			m_btnCancel.setVisible(true);
		}

		// Button No
		if (m_btnNo == null)
		{
			m_btnNo = new JButton();
			m_btnNo.addActionListener(m_buttonListener);
		}
		m_btnNo.setText(JAPMessages.getString(MSG_NO));
		m_panelOptions.add(m_btnNo);
		m_btnNo.setVisible(false);

		if (OPTION_TYPE_YES_NO == m_optionType || OPTION_TYPE_YES_NO_CANCEL == m_optionType)
		{
			m_btnNo.setVisible(true);
			// Button Yes/OK
			if (m_btnYesOK == null)
			{
				m_btnYesOK = new JButton();
				m_btnYesOK.addActionListener(m_buttonListener);
			}
			m_btnYesOK.setText(JAPMessages.getString(MSG_YES));
			m_panelOptions.add(m_btnYesOK);
		}
		else if (OPTION_TYPE_OK_CANCEL == m_optionType || OPTION_TYPE_DEFAULT == m_optionType)
		{
			if (m_btnYesOK == null)
			{
				m_btnYesOK = new JButton();
				m_btnYesOK.addActionListener(m_buttonListener);
			}
			m_btnYesOK.setText(JAPMessages.getString(MSG_OK));
			m_panelOptions.add(m_btnYesOK);
		}

		if (getHelpContext() != null)
		{
			m_btnHelp = JAPHelp.createHelpButton(this);
			m_panelOptions.add(m_btnHelp);
		}
	}

	private void createWizardOptions()
	{
		m_panelOptions = Box.createHorizontalBox();

		if (getHelpContext() != null)
		{
			if ( m_btnHelp == null)
			{
				m_btnHelp = JAPHelp.createHelpButton(this);
			}
			m_panelOptions.add(m_btnHelp);
			m_panelOptions.add(Box.createHorizontalStrut(5));
		}


		if (hasPreviousContentPane())
		{
			if (m_btnNo == null)
			{
				m_btnNo = new JButton();
				m_btnNo.addActionListener(m_buttonListener);
			}
			m_btnNo.setText("< " + JAPMessages.getString(MSG_PREVIOUS));
			if (getPreviousContentPane() == null)
			{
				m_btnNo.setEnabled(false);
			}
			m_panelOptions.add(m_btnNo);
		}
		if (m_btnYesOK == null)
		{
			m_btnYesOK = new JButton();
			m_btnYesOK.addActionListener(m_buttonListener);
		}
		setTextOfWizardNextButton();
		m_panelOptions.add(m_btnYesOK);

		m_panelOptions.add(Box.createHorizontalStrut(5));
		if (m_btnCancel == null)
		{
			m_btnCancel = new JButton();
			m_btnCancel.addActionListener(m_buttonListener);
		}
		m_btnCancel.setText(JAPMessages.getString(MSG_CANCEL));
		m_panelOptions.add(m_btnCancel);
	}

	private void setTextOfWizardNextButton()
	{
		if (hasNextContentPane())
		{
			m_btnYesOK.setText(JAPMessages.getString(MSG_NEXT) + " >");
		}
		else
		{
			m_btnYesOK.setText(JAPMessages.getString(MSG_FINISH));
		}
		m_btnYesOK.invalidate();
	}

	public void setMouseListener(MouseListener a_listener)
	{
		if (m_strText == null)
		{
			throw new IllegalStateException("This content pane does not contain a text field!");
		}
		else
		{
			m_lblText.addMouseListener(a_listener);
			m_lblSeeFullText.addMouseListener(a_listener);
		}
	}

	private void createOptions()
	{
		boolean bHasWizardLayout = hasWizardLayout();

		if (m_panelOptions != null &&
			((!bHasWizardLayout && !m_bHasHadWizardLayout)  || (bHasWizardLayout && m_bHasHadWizardLayout)))
		{
			// no need to change the option buttons
			m_bHasHadWizardLayout = bHasWizardLayout;
			return;
		}

		m_bHasHadWizardLayout = bHasWizardLayout;
		if (m_buttonListener == null)
		{
			m_buttonListener = new ButtonListener();
		}

		if (bHasWizardLayout)
		{
			createWizardOptions();
		}
		else
		{
			createDefaultOptions();
		}
	}

	private class DialogWindowListener extends WindowAdapter
	{
		public void windowClosed(WindowEvent a_event)
		{
			if (getButtonValue() == RETURN_VALUE_UNINITIALIZED)
			{
				setButtonValue(RETURN_VALUE_CLOSED);
			}

			ComponentListener listener = m_currentlyActiveContentPaneComponentListener;
			if (listener != null)
			{
				listener.componentHidden(new ComponentEvent(
								m_currentlyActiveContentPane, ComponentEvent.COMPONENT_HIDDEN));
			}
			if (!isDisposed())
			{
				dispose();
			}
		}
		public void windowOpened(WindowEvent a_event)
		{
			if (isVisible() && hasWizardLayout() && getButtonYesOK() != null && getButtonYesOK().isEnabled())
			{
				if (isAutomaticFocusSettingEnabled())
				{
					getButtonYesOK().requestFocus();
				}
				getDialog().getRootPane().setDefaultButton(getButtonYesOK());
			}

			if (getDialog() instanceof JDialog)
			{
				/**
				 * This is a patch for JDialog; componentShown at least called on the first opening.
				 * Alas, it is not possible to generate this event when the dialog is closed and
				 * opened a second time.
				 */
				ComponentListener listener = m_currentlyActiveContentPaneComponentListener;
				if (listener != null)
				{
					listener.componentShown(new ComponentEvent(
						m_currentlyActiveContentPane, ComponentEvent.COMPONENT_SHOWN));
				}
			}
		}
	}

	private class DialogComponentListener extends ComponentAdapter
	{
		public void componentHidden(ComponentEvent a_event)
		{
			if (getButtonValue() == RETURN_VALUE_UNINITIALIZED)
			{
				setButtonValue(RETURN_VALUE_CLOSED);
			}

			ComponentListener listener = m_currentlyActiveContentPaneComponentListener;
			if (listener != null)
			{
				listener.componentHidden(new ComponentEvent(
								m_currentlyActiveContentPane, ComponentEvent.COMPONENT_HIDDEN));
			}
		}

		public void componentShown(ComponentEvent a_event)
		{
			// does not work for JDialog in old JDKs (e.g. 1.1.8) and is therefore not used
		}
	}

	private class ContentPaneComponentListener extends ComponentAdapter
	{
		public void componentHidden(ComponentEvent a_event)
		{
			Vector listeners = (Vector)m_componentListeners.clone();
			for (int i = 0; i < listeners.size(); i++)
			{
				( (ComponentListener) listeners.elementAt(i)).componentHidden(a_event);
			}
		}

		public void componentShown(ComponentEvent a_event)
		{
			if (isVisible())
			{
				if (m_lblText != null)
				{
					// enable automatic resizing
					m_titlePane.remove(m_lblText);
					m_lblText = new JAPHtmlMultiLineLabel(m_lblText.getText(), m_lblText.getFont(),
						SwingConstants.CENTER);
					m_titlePane.add(m_lblText, m_textConstraints);
					m_titlePane.revalidate();
				}

				Vector listeners = (Vector)m_componentListeners.clone();
				for (int i = 0; i < listeners.size(); i++)
				{
					( (ComponentListener) listeners.elementAt(i)).componentShown(a_event);
				}
			}
		}

		public void componentResized(ComponentEvent a_event)
		{
			Vector listeners = (Vector)m_componentListeners.clone();
			for (int i = 0; i < listeners.size(); i++)
			{
				( (ComponentListener) listeners.elementAt(i)).componentResized(a_event);
			}
		}

		public void componentMoved(ComponentEvent a_event)
		{
			Vector listeners = (Vector)m_componentListeners.clone();
			for (int i = 0; i < listeners.size(); i++)
			{
				( (ComponentListener) listeners.elementAt(i)).componentMoved(a_event);
			}
		}
	}

	private class ButtonListener implements ActionListener
	{
		public void actionPerformed(ActionEvent a_event)
		{
			boolean bActionDone = false;
			CheckError[] errors;

			if (a_event == null || a_event.getSource() == null)
			{
				return;
			}

			if (a_event.getSource() == m_btnCancel)
			{
				errors = checkCancel();
				if (isSomethingDoneOnClick(errors,
										   ON_CANCEL_SHOW_NEXT_CONTENT, ON_CANCEL_SHOW_PREVIOUS_CONTENT,
										   ON_CANCEL_HIDE_DIALOG, ON_CANCEL_DISPOSE_DIALOG) ||
					(getJAPDialog() != null && getJAPDialog().isClosingOnContentPaneCancel()))
				{
					setButtonValue(RETURN_VALUE_CANCEL);
				}
				if ((getJAPDialog() == null || !getJAPDialog().isClosingOnContentPaneCancel()))
				{
					bActionDone = doDefaultButtonOperation(errors, ON_CANCEL_SHOW_NEXT_CONTENT,
						ON_CANCEL_SHOW_PREVIOUS_CONTENT,
						ON_CANCEL_HIDE_DIALOG, ON_CANCEL_DISPOSE_DIALOG);
				}
				else
				{
					bActionDone = true;
					getJAPDialog().doWindowClosing();
				}
			}
			else if (a_event.getSource() == m_btnYesOK)
			{
				errors = checkYesOK();
				if (isSomethingDoneOnClick(errors, ON_YESOK_SHOW_NEXT_CONTENT, ON_YESOK_SHOW_PREVIOUS_CONTENT,
										   ON_YESOK_HIDE_DIALOG, ON_YESOK_DISPOSE_DIALOG))
				{
					if (OPTION_TYPE_YES_NO == m_optionType || OPTION_TYPE_YES_NO_CANCEL == m_optionType)
					{
						setButtonValue(RETURN_VALUE_YES);
					}
					else
					{
						setButtonValue(RETURN_VALUE_OK);
					}
				}
				bActionDone = doDefaultButtonOperation(errors, ON_YESOK_SHOW_NEXT_CONTENT,
					ON_YESOK_SHOW_PREVIOUS_CONTENT, ON_YESOK_HIDE_DIALOG, ON_YESOK_DISPOSE_DIALOG);
			}
			else //if (a_event.getSource() == m_btnNo)
			{
				errors = checkNo();
				if (isSomethingDoneOnClick(errors, ON_NO_SHOW_NEXT_CONTENT, ON_NO_SHOW_PREVIOUS_CONTENT,
										   ON_NO_HIDE_DIALOG, ON_NO_DISPOSE_DIALOG))
				{
					setButtonValue(RETURN_VALUE_NO);
				}
				bActionDone = doDefaultButtonOperation(errors,
					ON_NO_SHOW_NEXT_CONTENT, ON_NO_SHOW_PREVIOUS_CONTENT,
					ON_NO_HIDE_DIALOG, ON_NO_DISPOSE_DIALOG);
			}

			if (!bActionDone && (errors == null || errors.length == 0))
			{
				doDefaultButtonOperation(errors, ON_CLICK_SHOW_NEXT_CONTENT, ON_CLICK_SHOW_PREVIOUS_CONTENT,
										 ON_CLICK_HIDE_DIALOG, ON_CLICK_DISPOSE_DIALOG);
			}
		}
	}

	/**
	 * Returns true if the click on a specific button will do an automatic action.
	 * @param a_errors CheckError[]
	 * @param a_opNext int
	 * @param a_opPrevious int
	 * @param a_opHide int
	 * @param a_opDispose int
	 * @return true if the click on a specific button will do an automatic action; false otherwise
	 */
	private boolean isSomethingDoneOnClick(CheckError[] a_errors,
										   int a_opNext, int a_opPrevious, int a_opHide, int a_opDispose)
	{
		return (a_errors == null || a_errors.length == 0) && ((getDefaultButtonOperation() & (
			  ON_CLICK_HIDE_DIALOG | ON_CLICK_DISPOSE_DIALOG | ON_CLICK_SHOW_NEXT_CONTENT |
			  ON_CLICK_SHOW_PREVIOUS_CONTENT |
			  a_opNext | a_opPrevious | a_opHide | a_opDispose)) > 0);
	}

	private boolean doDefaultButtonOperation(CheckError[] a_errors,
											 int a_opNext, int a_opPrevious, int a_opHide, int a_opDispose)
	{
		if (!checkErrors(a_errors, m_rememberedErrors))
		{
			return false;
		}

		if (m_nextContentPane != null && (getDefaultButtonOperation() & a_opNext) > 0 &&
			m_nextContentPane.isMoveForwardAllowed())
		{

			return moveToNextContentPane();
		}

		if (m_previousContentPane != null && (getDefaultButtonOperation() & a_opPrevious) > 0)
		{
			return moveToPreviousContentPane();
		}

		if ((getDefaultButtonOperation() & a_opDispose) > 0)
		{
			closeDialog(true);
			return true;
		}

		if ((getDefaultButtonOperation() & a_opHide) > 0)
		{
			closeDialog(false);
			return true;
		}

		return false;
	}
}
