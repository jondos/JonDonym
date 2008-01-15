/*
 Copyright (c) 2000 - 2006, The JAP-Team
 All rights reserved.
 Redistribution and use in source and binary forms, with or without modification,
 are permitted provided that the following conditions are met:

  - Redistributions of source code must retain the above copyright notice,
 this list of conditions and the following disclaimer.

  - Redistributions in bisnary form must reproduce the above copyright notice,
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

import java.util.EventListener;
import java.util.Vector;
import java.util.Enumeration;
import java.util.Hashtable;
import java.net.URL;
import java.lang.reflect.InvocationTargetException;
import javax.accessibility.Accessible;
import javax.accessibility.AccessibleContext;

import java.awt.AWTEvent;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Event;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.Image;
import java.awt.MenuComponent;
import java.awt.MenuContainer;
import java.awt.Point;
import java.awt.Window;
import java.awt.event.ComponentListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.awt.image.ImageObserver;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLayeredPane;
import javax.swing.JMenuBar;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRootPane;
import javax.swing.JTextPane;
import javax.swing.JCheckBox;
import javax.swing.RootPaneContainer;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

import platform.AbstractOS;
import gui.GUIUtils;
import gui.JAPHelpContext;
import gui.JAPHtmlMultiLineLabel;
import gui.JAPMessages;
import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;
import java.awt.Cursor;
import java.awt.Insets;
import java.awt.Rectangle;

/**
 * This is the generic implementation for an optionally modal, resizable a dialog. Use the getRootPane()
 * and getContentPane() methods for customization.
 * <BR>
 * The customizable dialogs show the same behaviour as the standard JDialog, except for the modality
 * attribute:
 * Modal JAPDialogs are only modal for their parent window and the parent of its parent and so on, but not
 * for other Dialogs or Windows. This allows for example to access a non-modal help window at the time a
 * modal dialog is displayed.
 * <BR>
 * This class is also a replacement for JOptionPane: it allows for the same type of dialogs, and
 * even has the same syntax (save that it only accepts String messages), but with additional features.
 * JAPDialog option panes
 * <UL>
 * <LI> are auto-formatted in the golden ratio if possible by a quick heuristic </LI>
 * <LI> interpret the text message as HTML, without the need to add html or body tags </LI>
 * <LI> may get an HTML link that triggers an arbitrary event, for example show a help window on clicking
 *      (interface ILinkedInformation) </LI>
 * </UL>
 * These features take the need to put newlines or HTML breaks into the message text to format the dialog.
 * This is done fully automatically. Also, dialog texts may get smaller, without ignoring important
 * information. This information may be stored behind the optional dialog link. For displaying a simple
 * link to a JAPHelp window, for example, there is a class named LinkedHelpContext. Its implementation
 * should cover most needs.
 *
 * <P> Warning: This is a really complex class handling many bugs and differences in different JDKs.
 * If you change something here, be sure you know what you are doing and test the class at least with
 * the following JDKs: </P>
 *
 * <UL>
 * <LI> Microsoft JView </LI>
 * <LI> 1.1.8 </LI>
 * <LI> 1.2.2 </LI>
 * <LI> 1.3.x </LI>
 * <LI> 1.4.x </LI>
 * <LI> 1.5.x </LI>
 * </UL>

 *
 * @see javax.swing.JDialog
 * @see javax.swing.JOptionPane
 * @see gui.dialog.DialogContentPane
 * @see ILinkedInformation
 *
 * @author Rolf Wendolsky
 */
public class JAPDialog implements Accessible, WindowConstants, RootPaneContainer, MenuContainer,
	ImageObserver, IDialogOptions
{
	public static final String XML_ATTR_OPTIMIZED_FORMAT = "dialogOptFormat";

	public static final int FORMAT_GOLDEN_RATIO_PHI = 0;
	public static final int FORMAT_DEFAULT_SCREEN = 1;
	public static final int FORMAT_WIDE_SCREEN = 2;


	private static final double[] FORMATS = {((1.0 + Math.sqrt(5.0)) / 2.0), (4.0 / 3.0), (16.0 / 9.0)};

	public static final String MSG_ERROR_UNKNOWN = JAPDialog.class.getName() + "_errorUnknown";
	public static final String MSG_TITLE_INFO = JAPDialog.class.getName() + "_titleInfo";
	public static final String MSG_TITLE_CONFIRMATION = JAPDialog.class.getName() + "_titleConfirmation";
	public static final String MSG_TITLE_WARNING = JAPDialog.class.getName() + "_titleWarning";
	public static final String MSG_TITLE_ERROR = JAPDialog.class.getName() + "_titleError";
	public static final String MSG_ERROR_UNDISPLAYABLE = JAPDialog.class.getName() + "_errorUndisplayable";

	public static final String MSG_PROCEED = JAPDialog.class.getName() + "_proceed";

	private static final int NUMBER_OF_HEURISTIC_ITERATIONS = 6;
	private static int m_optimizedFormat = FORMAT_WIDE_SCREEN;

	private static Hashtable ms_registeredDialogs = new Hashtable();
	private static boolean ms_bConsoleOnly = false;

	private boolean m_bLocationSetManually = false;
	private boolean m_bModal;
	private boolean m_bBlockParentWindow = false;
	private int m_defaultCloseOperation;
	private Vector m_windowListeners = new Vector();
	private Vector m_componentListeners = new Vector();
	private DialogWindowAdapter m_dialogWindowAdapter;
	private boolean m_bForceApplicationModality;
	private boolean m_bDisposed = false;
	private boolean m_bCatchCancel = false;
	private GUIUtils.WindowDocker m_docker;
	private final Object SYNC_DOCK = new Object();

	/**
	 * Stores the instance of JDialog for internal use.
	 */
	private JDialog m_internalDialog;

	/**
	 * This stores the parent component of this dialog.
	 */
	private Component m_parentComponent;

	/**
	 * This stores the parent window of this dialog.
	 */
	private Window m_parentWindow;

	private boolean m_bOnTop = false;

	/**
	 * Disables the output of static dialog methods. All those method will return RETURN_VALUE_UNINITIALIZED.
	 * @param a_bConsoleOnly if the output of static dialog methods should be disabled
	 */
	public static void setConsoleOnly(boolean a_bConsoleOnly)
	{
		ms_bConsoleOnly = a_bConsoleOnly;
		if (ms_bConsoleOnly)
		{
			Vector currentDialogs;
			Enumeration enumCurrentDialogs;
			synchronized (ms_registeredDialogs)
			{
				currentDialogs = new Vector(ms_registeredDialogs.size());
				enumCurrentDialogs = ms_registeredDialogs.elements();
				while (enumCurrentDialogs.hasMoreElements())
				{
					currentDialogs.addElement(enumCurrentDialogs.nextElement());
				}
			}
			enumCurrentDialogs = currentDialogs.elements();
			while (enumCurrentDialogs.hasMoreElements())
			{
				( (JAPDialog) enumCurrentDialogs.nextElement()).dispose();
			}
			currentDialogs.removeAllElements();

		}
	}

	public static boolean isConsoleOnly()
	{
		return ms_bConsoleOnly;
	}

	/**
	 * Creates a new instance of JAPDialog. It is user-resizable.
	 * @param a_parentComponent The parent component for this dialog. If it is null or the parent
	 *                          component is not within a frame, the dialog's parent frame is the
	 *                          default frame.
	 * @param a_strTitle The title String for this dialog.
	 * @param a_bModal if the dialog should be modal
	 */
	public JAPDialog(Component a_parentComponent, String a_strTitle, boolean a_bModal)
	{
		this(a_parentComponent, a_strTitle, a_bModal, false);
	}

	/**
	 * Creates a new instance of JAPDialog. It is user-resizable and modal.
	 * @param a_parentComponent The parent component for this dialog. If it is null or the parent
	 *                          component is not within a frame, the dialog's parent frame is the
	 *                          default frame.
	 * @param a_strTitle The title String for this dialog.
	 */
	public JAPDialog(Component a_parentComponent, String a_strTitle)
	{
		this(a_parentComponent, a_strTitle, true);
	}

	/**
	 * Creates a new instance of JAPDialog. It is user-resizable.
	 * @param a_parentDialog The parent dialog for this dialog. If it is null,
	 *                       the dialog's parent frame is the default frame.
	 * @param a_strTitle The title String for this dialog.
	 * @param a_bModal if the dialog should be modal
	 */
	public JAPDialog(JAPDialog a_parentDialog, String a_strTitle, boolean a_bModal)
	{
		this(getInternalDialog(a_parentDialog), a_strTitle, a_bModal);
	}

	/**
	 * Creates a new instance of JAPDialog. It is user-resizable.
	 * @param a_parentDialog The parent dialog for this dialog. If it is null,
	 *                       the dialog's parent frame is the default frame.
	 * @param a_strTitle The title String for this dialog.
	 */
	public JAPDialog(JAPDialog a_parentDialog, String a_strTitle)
	{
		this(getInternalDialog(a_parentDialog), a_strTitle);
	}


	/**
	 * Creates a new instance of JAPDialog. It is user-resizable.
	 * @param a_parentComponent The parent component for this dialog. If it is null or the parent
	 *                          component is not within a frame, the dialog's parent frame is the
	 *                          default frame.
	 * @param a_strTitle The title String for this dialog.
	 * @param a_bModal if the dialog should be modal
	 * @param a_bForceApplicationModality Force the JAPDialog to behave like a JDialog and block all
	 * application windows. This should not be available for the public, only for internal use!
	 */
	private JAPDialog(Component a_parentComponent, String a_strTitle, boolean a_bModal,
					  boolean a_bForceApplicationModality)
	{
		EventListener[] listeners;

		m_parentComponent = a_parentComponent;
		m_bForceApplicationModality = a_bForceApplicationModality;

		m_internalDialog = new JOptionPane().createDialog(a_parentComponent, a_strTitle);
		if (m_parentComponent == null)
		{
			// get the default frame
			m_parentComponent = m_internalDialog.getParent();
		}
		m_internalDialog.getContentPane().removeAll();
		m_internalDialog.setResizable(true);
		m_internalDialog.setModal(false);
		m_internalDialog.setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
		//setDefaultCloseOperation(m_internalDialog.getDefaultCloseOperation());
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);


		/* Old JDKs ignore the default closing operation, therefore it is tried to remove the window listener.
		 * This removes the flimmering effect that occurs when the internal dialog is closed before enabling
		 * the parent window.
		 */
		try
		{
			//listeners = m_internalDialog.getListeners(WindowListener.class);
			listeners = (EventListener[]) JDialog.class.getMethod(
						 "getListeners", new Class[]
						 {Class.class}).invoke(m_internalDialog, new Object[]{WindowListener.class});
		}
		catch (Exception a_e)
		{
			// method is only available in JDKs >= 1.3
			listeners = null;
		}
		for (int i = 0; listeners != null && i < listeners.length; i++)
		{
			m_internalDialog.removeWindowListener((WindowListener)listeners[i]);
		}

		m_dialogWindowAdapter = new DialogWindowAdapter();
		m_internalDialog.addWindowListener(m_dialogWindowAdapter);
		m_parentWindow = GUIUtils.getParentWindow(getParentComponent());
		setModal(a_bModal);


		ms_registeredDialogs.put(this, this);
		final JAPDialog thisDialog = this;
		addWindowListener(new WindowAdapter()
		{
			public void windowClosed(WindowEvent a_event)
			{
				ms_registeredDialogs.remove(thisDialog);
				thisDialog.removeWindowListener(this);
			}
		});
	}

	/**
	 * Classes of this type are used to append a clickable and/or selectable message at the end of a
	 * dialog message. You may define any after-click-action that you want, for example open a help window.
	 * If an ILinkedInformation implements the interface JAPHelpContext.IHelpContext,
	 * a help button is shown that opens the specified help context on clicking.
	 */
	public static interface ILinkedInformation
	{
		public static final String MSG_MORE_INFO = ILinkedInformation.class.getName() + "_moreInfo";

		/** Shows a clickable link or a help button if JAPHelpContext.IHelpContext is implemented */
		public static final int TYPE_DEFAULT = 0;
		/** Shows a clickable link and (!!) a help button if JAPHelpContext.IHelpContext is implemented */
		public static final int TYPE_LINK = 1;
		/** Shows a selectable link and (!!) a help button if JAPHelpContext.IHelpContext is implemented */
		public static final int TYPE_SELECTABLE_LINK = 2;
		/** Shows a checkbox and (!!) a help button if JAPHelpContext.IHelpContext is implemented */
		public static final int TYPE_CHECKBOX_TRUE = 3;
		/** Shows a checkbox and (!!) a help button if JAPHelpContext.IHelpContext is implemented */
		public static final int TYPE_CHECKBOX_FALSE = 4;

		/**
		 * Returns the information message. This must be normal text, HTML is not allowed and
		 * tags are filtered out. If the message is <CODE>null</CODE> or empty,
		 * no linked information will be displayed.
		 * @return the information message
		 */
		public String getMessage();
		/**
		 * Performs an action when the link is clicked, for example opening a browser
		 * window, an E-Mail client or a help page.
		 * @param a_bState sets the current state if the linked information is defined as a checkbox.
		 */
		public void clicked(boolean a_bState);
		/**
		 * Returns the type the the linked information. It may be a simple link in HTML style (TYPE_LINK),
		 * a selectable link that may be copied to the clipboard (TYPE_SELECTABLE_LINK) or a
		 * checkbox (TYPE_CHECKBOX_TRUE or TYPE_CHECKBOX_FALSE).
		 * In case of a checkbox, the <CODE>clicked</CODE> method is called with the state of the checkbox.
		 * You may initialise the checkbox with true (TYPE_CHECKBOX_TRUE) or false (TYPE_CHECKBOX_FALSE).
		 * @return if the user is allowed to copy the link text
		 */
		public int getType();
		/**
		 * Returns if the dialog should block all other windows. This should not be the default
		 * behaviour as, for example, no help window would be accessible.
		 * @return if the dialog should block all other windows
		 */
		public boolean isApplicationModalityForced();

		/**
		 * Returns if this dialog should be on top of all other windows in the system.
		 * @return if this dialog should be on top of all other windows in the system
		 */
		public boolean isOnTop();
	}

	/**
	 * Shows a checkbox with a message on the dialog window.
	 */
	public static class LinkedCheckBox extends LinkedHelpContext
	{
		public static final String MSG_REMEMBER_ANSWER = LinkedCheckBox.class.getName() + "_rememberAnswer";
		public static final String MSG_DO_NOT_SHOW_AGAIN = LinkedCheckBox.class.getName() + "_doNotShowAgain";

		private String m_strMessage;
		private boolean m_bDefault;
		private boolean m_bState;

		/**
		 * Creates a new linked checkbox with the default message MSG_DO_NOT_SHOW_AGAIN.
		 * @param a_bDefault the default value of the checkbox
		 */
		public LinkedCheckBox(boolean a_bDefault)
		{
			this(a_bDefault, (JAPHelpContext.IHelpContext)null);
		}

		/**
		 * Creates a new linked checkbox with the default message MSG_DO_NOT_SHOW_AGAIN.
		 * @param a_bDefault the default value of the checkbox
		 * @param a_helpContext the help context that is opened when the help button is clicked
		 */
		public LinkedCheckBox(boolean a_bDefault, JAPHelpContext.IHelpContext a_helpContext)
		{
			this(JAPMessages.getString(MSG_DO_NOT_SHOW_AGAIN), a_bDefault, a_helpContext);
		}

		/**
		 * Creates a new linked checkbox with the default message MSG_DO_NOT_SHOW_AGAIN.
		 * @param a_bDefault the default value of the checkbox
		 * @param a_strHelpContext the help context that is opened when the help button is clicked
		 */
		public LinkedCheckBox(boolean a_bDefault, String a_strHelpContext)
		{
			this(JAPMessages.getString(MSG_DO_NOT_SHOW_AGAIN), a_bDefault, a_strHelpContext);
		}

		/**
		 * Creates a new linked checkbox.
		 * @param a_strMessage a message to be displayed with the checkbox
		 * @param a_bDefault the default value of the checkbox
		 */
		public LinkedCheckBox(String a_strMessage, boolean a_bDefault)
		{
			this(a_strMessage, a_bDefault, (JAPHelpContext.IHelpContext)null);
		}

		/**
		 * Creates a new linked checkbox.
		 * @param a_strMessage a message to be displayed with the checkbox
		 * @param a_bDefault the default value of the checkbox
		 * @param a_strHelpContext the help context that is opened when the help button is clicked
		 */
		public LinkedCheckBox(String a_strMessage, boolean a_bDefault, final String a_strHelpContext)
		{
			this(a_strMessage, a_bDefault, new JAPHelpContext.IHelpContext()
			{
				public String getHelpContext(){ return a_strHelpContext;}
			});
		}

		/**
		 * Creates a new linked checkbox.
		 * @param a_strMessage a message to be displayed with the checkbox
		 * @param a_bDefault the default value of the checkbox
		 * @param a_helpContext the help context that is opened when the help button is clicked
		 */
		public LinkedCheckBox(String a_strMessage, boolean a_bDefault,
							  JAPHelpContext.IHelpContext a_helpContext)
		{
			super(a_helpContext);

			m_strMessage = a_strMessage;
			m_bDefault = a_bDefault;
			m_bState = m_bDefault;
		}

		/**
		 * Returns the information message of the checkbox.
		 * @return the information message of the checkbox
		 */
		public String getMessage()
		{
			return m_strMessage;
		}

		/**
		 * Updates the state when the checkbox is clicked.
		 * @param a_bState sets the current state of the checkbox
		 */
		public void clicked(boolean a_bState)
		{
			m_bState = a_bState;
		}

		/**
		 * Returns the state of the checkbox.
		 * @return the state of the checkbox
		 */
		public final boolean getState()
		{
			return m_bState;
		}

		/**
		 * Returns, depending on the default value, either TYPE_CHECKBOX_TRUE or TYPE_CHECKBOX_FALSE.
		 * @return if the user is allowed to copy the link text
		 */
		public final int getType()
		{
			if (m_bDefault)
			{
				return TYPE_CHECKBOX_TRUE;
			}
			return TYPE_CHECKBOX_FALSE;
		}
	}

	/**
	 * This class does nothing but implementing all ILinkedInformation methods.
	 */
	public static class LinkedInformationAdapter implements ILinkedInformation
	{
		/**
		 * Returns null
		 * @return null
		 */
		public String getMessage()
		{
			return null;
		}
		/**
		 * Does nothing.
		 * @param a_bState is ignored
		 */
		public void clicked(boolean a_bState)
		{
		}
		/**
		 * Returns TYPE_DEFAULT.
		 * @return TYPE_DEFAULT
		 */
		public int getType()
		{
			return TYPE_DEFAULT;
		}
		/**
		 * Returns true.
		 * @return true
		 */
		public boolean isApplicationModalityForced()
		{
			return false;
		}

		/**
		 * Returns false.
		 * @return false
		 */
		public boolean isOnTop()
		{
			return false;
		}
	}

	/**
	 * May be used to show a URL.
	 */
	public static abstract class AbstractLinkedURLAdapter extends LinkedInformationAdapter
	{
		public abstract URL getUrl();

		/**
		 * Returns <CODE>null</CODE> as no message is needed.
		 * @return <CODE>null</CODE>
		 */
		public String getMessage()
		{
			URL url = getUrl();
			if (url != null)
			{
				return getUrl().toString();
			}
			return null;
		}
		/**
		 * Opens the URL.
		 * @param a_bState is ignored
		 */
		public final void clicked(boolean a_bState)
		{
			AbstractOS.getInstance().openURL(getUrl());
		}

		/**
		 * Returns TYPE_LINK.
		 * @return TYPE_LINK
		 */
		public final int getType()
		{
			return TYPE_LINK;
		}
		/**
		 * Returns false by default
		 * @return false
		 */
		public boolean isApplicationModalityForced()
		{
			return false;
		}
	}


	/**
	 * This implementation of ILinkedInformation registers a help context in the dialog and displays a
	 * help button that opens this context. Subclasses may override it to show an additional link
	 * or a checkbox.
	 */
	public static class LinkedHelpContext extends LinkedInformationAdapter
		implements JAPHelpContext.IHelpContext
	{
		private JAPHelpContext.IHelpContext m_helpContext;


		public LinkedHelpContext(JAPHelpContext.IHelpContext a_helpContext)
		{
			m_helpContext = a_helpContext;
		}

		public LinkedHelpContext(final String a_strHelpContext)
		{
			m_helpContext = new JAPHelpContext.IHelpContext()
			{
				public String getHelpContext()
				{
					return a_strHelpContext;
				}
			};
		}

		public final String getHelpContext()
		{
			if (m_helpContext == null)
			{
				return null;
			}
			return m_helpContext.getHelpContext();
		}

		/**
		 * Returns <CODE>null</CODE> as no message is needed.
		 * @return <CODE>null</CODE>
		 */
		public String getMessage()
		{
			return null;
		}
		/**
		 * Does nothing.
		 * @param a_bState is ignored
		 */
		public void clicked(boolean a_bState)
		{
		}
		/**
		 * Returns TYPE_DEFAULT.
		 * @return TYPE_DEFAULT
		 */
		public int getType()
		{
			return TYPE_DEFAULT;
		}
		/**
		 * Returns false as otherwise the help window would not be accessible.
		 * @return false
		 */
		public final boolean isApplicationModalityForced()
		{
			return false;
		}
	}

	public static class Options
	{
		private int m_optionType;

		public Options(int a_optionType)
		{
			m_optionType = a_optionType;
		}
		public final int getOptionType()
		{
			return m_optionType;
		}

		public String getYesOKText()
		{
			return null;
		}
		public String getNoText()
		{
			return null;
		}
		public String getCancelText()
		{
			return null;
		}
	}

	/**
	 * Sets the format to which all automatically scaled dialogs are optimized. It is one of the constants
	 * FORMAT_GOLDEN_RATIO_PHI, FORMAT_DEFAULT_SCREEN and FORMAT_WIDE_SCREEN.
	 * @param a_optimizedFormat the format to which all automatically scaled dialogs are optimized
	 */
	public static void setOptimizedFormat(int a_optimizedFormat)
	{
		if (a_optimizedFormat < 0 || a_optimizedFormat >= FORMATS.length)
		{
			a_optimizedFormat = FORMATS.length - 1;
		}
		m_optimizedFormat = a_optimizedFormat;
	}

	/**
	 * Returns the format to which all automatically scaled dialogs are optimized. It is one of the constants
	 * GOLDEN_RATIO_PHI, DEFAULT_SCREEN_FORMAT and WIDE_SCREEN_FORMAT.
	 * @return the format to which all automatically scaled dialogs are optimized
	 */
	public static int getOptimizedFormat()
	{
		return m_optimizedFormat;
	}

	/**
	 * Returns the format to which all automatically scaled dialogs are optimized. It is one of the constants
	 * FORMAT_DEFAULT_SCREEN, FORMAT_GOLDEN_RATIO_PHI, and FORMAT_WIDE_SCREEN.
	 * @param a_format one of the constants
	 * FORMAT_DEFAULT_SCREEN, FORMAT_GOLDEN_RATIO_PHI, and FORMAT_WIDE_SCREEN
	 * @return the format to which all automatically scaled dialogs are optimized
	 */
	public static double getOptimizedFormatInternal(int a_format)
	{
		if (a_format < 0 || a_format >= FORMATS.length)
		{
			a_format = FORMATS.length - 1;
		}
		return FORMATS[a_format];
	}

	/**
	 * Calculates the difference from a window's size and the golden ratio.
	 * @param a_window a Window
	 * @return the difference from a window's size and the golden ratio
	 */
	public static double getOptimizedFormatDelta(Window a_window)
	{
		return a_window.getSize().height *
			getOptimizedFormatInternal(m_optimizedFormat) - a_window.getSize().width;
	}

	/**
	 * Calculates the difference from a JAPDialog's size and the golden ratio.
	 * @param a_dialog a JAPDialog
	 * @return the difference from a JAPDialog's size and the golden ratio
	 */
	public static double getOptimizedFormatDelta(JAPDialog a_dialog)
	{
		return a_dialog.getSize().height *
			getOptimizedFormatInternal(m_optimizedFormat) - a_dialog.getSize().width;
	}

	/**
	 * Displays an info message dialog. Words are wrapped automatically if a message line is too long.
	 * @param a_parentDialog The parent dialog for this dialog. If it is null,
	 *                       the dialog's parent frame is the default frame.
	 * @param a_message The message to be displayed. It is interpreted as HTML. You do not need to put in
	 * formatting tags, as the text will be auto-formatted in a way that the dialog's size is very close
	 * to the golden ratio.
	 */
	public static void showMessageDialog(JAPDialog a_parentDialog, String a_message)
	{
		showMessageDialog(getInternalDialog(a_parentDialog), a_message);
	}

	/**
	 * Displays an info message dialog. Words are wrapped automatically if a message line is too long.
	 * @param a_parentDialog The parent dialog for this dialog. If it is null,
	 *                       the dialog's parent frame is the default frame.
	 * @param a_message The message to be displayed. It is interpreted as HTML. You do not need to put in
	 * formatting tags, as the text will be auto-formatted in a way that the dialog's size is very close
	 * to the golden ratio.
	 * @param a_linkedInformation a clickable information message that is appended to the text
	 */
	public static void showMessageDialog(JAPDialog a_parentDialog, String a_message,
										 ILinkedInformation a_linkedInformation)
	{
		showMessageDialog(getInternalDialog(a_parentDialog), a_message, a_linkedInformation);
	}

	/**
	 * Displays an info message dialog. Words are wrapped automatically if a message line is too long.
	 * @param a_parentComponent The parent component for this dialog. If it is null or the parent
	 *                          component is not within a frame, the dialog's parent frame is the
	 *                          default frame.
	 * @param a_message The message to be displayed. It is interpreted as HTML. You do not need to put in
	 * formatting tags, as the text will be auto-formatted in a way that the dialog's size is very close
	 * to the golden ratio.
	 */
	public static void showMessageDialog(Component a_parentComponent, String a_message)
	{
		showMessageDialog(
			  a_parentComponent, a_message, JAPMessages.getString(MSG_TITLE_INFO), (Icon)null);
	}

	/**
	 * Displays an info message dialog. Words are wrapped automatically if a message line is too long.
	 * @param a_parentComponent The parent component for this dialog. If it is null or the parent
	 *                          component is not within a frame, the dialog's parent frame is the
	 *                          default frame.
	 * @param a_message The message to be displayed. It is interpreted as HTML. You do not need to put in
	 * formatting tags, as the text will be auto-formatted in a way that the dialog's size is very close
	 * to the golden ratio.
	 * @param a_linkedInformation a clickable information message that is appended to the text
	 */
	public static void showMessageDialog(Component a_parentComponent, String a_message,
										 ILinkedInformation a_linkedInformation)
	{
		showMessageDialog(
			  a_parentComponent, a_message, JAPMessages.getString(MSG_TITLE_INFO), (Icon)null,
			  a_linkedInformation);
	}

	/**
	 * Displays an info message dialog. Words are wrapped automatically if a message line is too long.
	 * @param a_parentDialog The parent dialog for this dialog. If it is null,
	 *                       the dialog's parent frame is the default frame.
	 * @param a_title The title of the message dialog
	 * @param a_message The message to be displayed. It is interpreted as HTML. You do not need to put in
	 * formatting tags, as the text will be auto-formatted in a way that the dialog's size is very close
	 * to the golden ratio.
	 */
	public static void showMessageDialog(JAPDialog a_parentDialog, String a_message, String a_title)
	{
		showMessageDialog(getInternalDialog(a_parentDialog), a_message, a_title);
	}

	/**
	 * Displays an info message dialog. Words are wrapped automatically if a message line is too long.
	 * @param a_parentDialog The parent dialog for this dialog. If it is null,
	 *                       the dialog's parent frame is the default frame.
	 * @param a_title The title of the message dialog
	 * @param a_message The message to be displayed. It is interpreted as HTML. You do not need to put in
	 * formatting tags, as the text will be auto-formatted in a way that the dialog's size is very close
	 * to the golden ratio.
	 * @param a_linkedInformation a clickable information message that is appended to the text
	 */
	public static void showMessageDialog(JAPDialog a_parentDialog, String a_message, String a_title,
										 ILinkedInformation a_linkedInformation)
	{
		showMessageDialog(getInternalDialog(a_parentDialog), a_message, a_title, a_linkedInformation);
	}

	/**
	 * Displays an info message dialog. Words are wrapped automatically if a message line is too long.
	 * @param a_parentComponent The parent component for this dialog. If it is null or the parent
	 *                          component is not within a frame, the dialog's parent frame is the
	 *                          default frame.
	 * @param a_title The title of the message dialog
	 * @param a_message The message to be displayed. It is interpreted as HTML. You do not need to put in
	 * formatting tags, as the text will be auto-formatted in a way that the dialog's size is very close
	 * to the golden ratio.
	 */
	public static void showMessageDialog(Component a_parentComponent, String a_message, String a_title)
	{
		showMessageDialog(a_parentComponent, a_message, a_title, (Icon)null);
	}

	/**
	 * Displays an info message dialog. Words are wrapped automatically if a message line is too long.
	 * @param a_parentComponent The parent component for this dialog. If it is null or the parent
	 *                          component is not within a frame, the dialog's parent frame is the
	 *                          default frame.
	 * @param a_title The title of the message dialog
	 * @param a_message The message to be displayed. It is interpreted as HTML. You do not need to put in
	 * formatting tags, as the text will be auto-formatted in a way that the dialog's size is very close
	 * to the golden ratio.
	 * @param a_linkedInformation a clickable information message that is appended to the text
	 */
	public static void showMessageDialog(Component a_parentComponent, String a_message, String a_title,
										 ILinkedInformation a_linkedInformation)
	{
		showMessageDialog(a_parentComponent, a_message, a_title, null, a_linkedInformation);
	}

	/**
	 * Displays an info message dialog. Words are wrapped automatically if a message line is too long.
	 * @param a_parentDialog The parent dialog for this dialog. If it is null,
	 *                       the dialog's parent frame is the default frame.
	 * @param a_message The message to be displayed. It is interpreted as HTML. You do not need to put in
	 * formatting tags, as the text will be auto-formatted in a way that the dialog's size is very close
	 * to the golden ratio.
	 * @param a_icon an icon that will be displayed on the dialog
	 */
	public static void showMessageDialog(JAPDialog a_parentDialog, String a_message, Icon a_icon)
	{
		showMessageDialog(getInternalDialog(a_parentDialog), a_message, a_icon);
	}

	/**
	 * Displays an info message dialog. Words are wrapped automatically if a message line is too long.
	 * @param a_parentDialog The parent dialog for this dialog. If it is null,
	 *                       the dialog's parent frame is the default frame.
	 * @param a_message The message to be displayed. It is interpreted as HTML. You do not need to put in
	 * formatting tags, as the text will be auto-formatted in a way that the dialog's size is very close
	 * to the golden ratio.
	 * @param a_icon an icon that will be displayed on the dialog
	 * @param a_linkedInformation a clickable information message that is appended to the text
	 */
	public static void showMessageDialog(JAPDialog a_parentDialog, String a_message, Icon a_icon,
										 ILinkedInformation a_linkedInformation)
	{
		showMessageDialog(getInternalDialog(a_parentDialog), a_message, a_icon, a_linkedInformation);
	}

	/**
	 * Displays an info message dialog. Words are wrapped automatically if a message line is too long.
	 * @param a_parentComponent The parent component for this dialog. If it is null or the parent
	 *                          component is not within a frame, the dialog's parent frame is the
	 *                          default frame.
	 * @param a_message The message to be displayed. It is interpreted as HTML. You do not need to put in
	 * formatting tags, as the text will be auto-formatted in a way that the dialog's size is very close
	 * to the golden ratio.
	 * @param a_icon an icon that will be displayed on the dialog
	 */
	public static void showMessageDialog(Component a_parentComponent, String a_message, Icon a_icon)
	{
		showMessageDialog(a_parentComponent, a_message, JAPMessages.getString(MSG_TITLE_INFO), a_icon);
	}

	/**
	 * Displays an info message dialog. Words are wrapped automatically if a message line is too long.
	 * @param a_parentComponent The parent component for this dialog. If it is null or the parent
	 *                          component is not within a frame, the dialog's parent frame is the
	 *                          default frame.
	 * @param a_message The message to be displayed. It is interpreted as HTML. You do not need to put in
	 * formatting tags, as the text will be auto-formatted in a way that the dialog's size is very close
	 * to the golden ratio.
	 * @param a_icon an icon that will be displayed on the dialog
	 * @param a_linkedInformation a clickable information message that is appended to the text
	 */
	public static void showMessageDialog(Component a_parentComponent, String a_message, Icon a_icon,
									  ILinkedInformation a_linkedInformation)
	{
		showMessageDialog(a_parentComponent, a_message, JAPMessages.getString(MSG_TITLE_INFO), a_icon,
						  a_linkedInformation);
	}

	/**
	 * Displays an info message dialog. Words are wrapped automatically if a message line is too long.
	 * @param a_parentDialog The parent dialog for this dialog. If it is null,
	 *                       the dialog's parent frame is the default frame.
	 * @param a_title The title of the message dialog
	 * @param a_message The message to be displayed. It is interpreted as HTML. You do not need to put in
	 * formatting tags, as the text will be auto-formatted in a way that the dialog's size is very close
	 * to the golden ratio.
	 * @param a_icon an icon that will be displayed on the dialog
	 */
	public static void showMessageDialog(JAPDialog a_parentDialog, String a_message, String a_title,
										 Icon a_icon)
	{
		showMessageDialog(getInternalDialog(a_parentDialog), a_message, a_title, a_icon);
	}

	/**
	 * Displays an info message dialog. Words are wrapped automatically if a message line is too long.
	 * @param a_parentDialog The parent dialog for this dialog. If it is null,
	 *                       the dialog's parent frame is the default frame.
	 * @param a_title The title of the message dialog
	 * @param a_message The message to be displayed. It is interpreted as HTML. You do not need to put in
	 * formatting tags, as the text will be auto-formatted in a way that the dialog's size is very close
	 * to the golden ratio.
	 * @param a_icon an icon that will be displayed on the dialog
	 * @param a_linkedInformation a clickable information message that is appended to the text
	 */
	public static void showMessageDialog(JAPDialog a_parentDialog, String a_message, String a_title,
										 Icon a_icon, ILinkedInformation a_linkedInformation)
	{
		showMessageDialog(getInternalDialog(a_parentDialog), a_message, a_title, a_icon, a_linkedInformation);
	}

	/**
	 * Displays an info message dialog. Words are wrapped automatically if a message line is too long.
	 * @param a_parentComponent The parent component for this dialog. If it is null or the parent
	 *                          component is not within a frame, the dialog's parent frame is the
	 *                          default frame.
	 * @param a_title The title of the message dialog
	 * @param a_message The message to be displayed. It is interpreted as HTML. You do not need to put in
	 * formatting tags, as the text will be auto-formatted in a way that the dialog's size is very close
	 * to the golden ratio.
	 * @param a_icon an icon that will be displayed on the dialog
	 */
	public static void showMessageDialog(Component a_parentComponent, String a_message, String a_title,
										 Icon a_icon)
	{
		showMessageDialog(a_parentComponent, a_message, a_title, a_icon, null);
	}

	/**
	 * Displays an info message dialog. Words are wrapped automatically if a message line is too long.
	 * @param a_parentComponent The parent component for this dialog. If it is null or the parent
	 *                          component is not within a frame, the dialog's parent frame is the
	 *                          default frame.
	 * @param a_title The title of the message dialog
	 * @param a_message The message to be displayed. It is interpreted as HTML. You do not need to put in
	 * formatting tags, as the text will be auto-formatted in a way that the dialog's size is very close
	 * to the golden ratio.
	 * @param a_icon an icon that will be displayed on the dialog
	 * @param a_linkedInformation a clickable information message that is appended to the text
	 */
	public static void showMessageDialog(Component a_parentComponent, String a_message, String a_title,
										 Icon a_icon, ILinkedInformation a_linkedInformation)
	{
		if (a_title == null)
		{
			a_title = JAPMessages.getString(MSG_TITLE_CONFIRMATION);
		}

		showConfirmDialog(a_parentComponent, a_message, a_title, OPTION_TYPE_DEFAULT,
						 MESSAGE_TYPE_INFORMATION, a_icon, a_linkedInformation);
	}

	/**
	 * Displays a warning message dialog. Words are wrapped automatically if a message line is too long.
	 * @param a_parentDialog The parent dialog for this dialog. If it is null,
	 *                       the dialog's parent frame is the default frame.
	 * @param a_message The message to be displayed. It is interpreted as HTML. You do not need to put in
	 * formatting tags, as the text will be auto-formatted in a way that the dialog's size is very close
	 * to the golden ratio.
	 */
	public static void showWarningDialog(JAPDialog a_parentDialog, String a_message)
	{
		showWarningDialog(a_parentDialog, a_message, null, null);
	}

	/**
	 * Displays a warning message dialog. Words are wrapped automatically if a message line is too long.
	 * @param a_parentComponent The parent component for this dialog. If it is null or the parent
	 *                          component is not within a frame, the dialog's parent frame is the
	 *                          default frame.
	 * @param a_message The message to be displayed. It is interpreted as HTML. You do not need to put in
	 * formatting tags, as the text will be auto-formatted in a way that the dialog's size is very close
	 * to the golden ratio.
	 */
	public static void showWarningDialog(Component a_parentComponent, String a_message)
	{
		showWarningDialog(a_parentComponent, a_message, null, null);
	}

	/**
	 * Displays a warning message dialog. Words are wrapped automatically if a message line is too long.
	 * @param a_parentDialog The parent dialog for this dialog. If it is null,
	 *                       the dialog's parent frame is the default frame.
	 * @param a_title The title of the message dialog
	 * @param a_message The message to be displayed. It is interpreted as HTML. You do not need to put in
	 * formatting tags, as the text will be auto-formatted in a way that the dialog's size is very close
	 * to the golden ratio.
	 */
	public static void showWarningDialog(JAPDialog a_parentDialog, String a_message, String a_title)
	{
		showWarningDialog(a_parentDialog, a_message, a_title, null);
	}

	/**
	 * Displays a warning message dialog. Words are wrapped automatically if a message line is too long.
	 * @param a_parentComponent The parent component for this dialog. If it is null or the parent
	 *                          component is not within a frame, the dialog's parent frame is the
	 *                          default frame.
	 * @param a_title The title of the message dialog
	 * @param a_message The message to be displayed. It is interpreted as HTML. You do not need to put in
	 * formatting tags, as the text will be auto-formatted in a way that the dialog's size is very close
	 * to the golden ratio.
	 */
	public static void showWarningDialog(Component a_parentComponent, String a_message, String a_title)
	{
		showWarningDialog(a_parentComponent, a_message, a_title, null);
	}

	/**
	 * Displays a warning message dialog. Words are wrapped automatically if a message line is too long.
	 * @param a_parentDialog The parent dialog for this dialog. If it is null,
	 *                       the dialog's parent frame is the default frame.
	 * @param a_title The title of the message dialog
	 * @param a_message The message to be displayed. It is interpreted as HTML. You do not need to put in
	 * formatting tags, as the text will be auto-formatted in a way that the dialog's size is very close
	 * to the golden ratio.
	 * @param a_linkedInformation a clickable information message that is appended to the text
	 */
	public static void showWarningDialog(JAPDialog a_parentDialog, String a_message, String a_title,
										 ILinkedInformation a_linkedInformation)
	{
		showWarningDialog(getInternalDialog(a_parentDialog), a_message, a_title, a_linkedInformation);
	}

	/**
	 * Displays a warning message dialog. Words are wrapped automatically if a message line is too long.
	 * @param a_parentDialog The parent dialog for this dialog. If it is null,
	 *                       the dialog's parent frame is the default frame.
	 * @param a_message The message to be displayed. It is interpreted as HTML. You do not need to put in
	 * formatting tags, as the text will be auto-formatted in a way that the dialog's size is very close
	 * to the golden ratio.
	 * @param a_linkedInformation a clickable information message that is appended to the text
	 */
	public static void showWarningDialog(JAPDialog a_parentDialog, String a_message,
										 ILinkedInformation a_linkedInformation)
	{
		showWarningDialog(getInternalDialog(a_parentDialog), a_message, null, a_linkedInformation);
	}

	/**
	 * Displays a warning message dialog. Words are wrapped automatically if a message line is too long.
	 * @param a_parentComponent The parent component for this dialog. If it is null or the parent
	 *                          component is not within a frame, the dialog's parent frame is the
	 *                          default frame.
	 * @param a_message The message to be displayed. It is interpreted as HTML. You do not need to put in
	 * formatting tags, as the text will be auto-formatted in a way that the dialog's size is very close
	 * to the golden ratio.
	 * @param a_linkedInformation a clickable information message that is appended to the text
	 */
	public static void showWarningDialog(Component a_parentComponent, String a_message,
										 ILinkedInformation a_linkedInformation)
	{
		showWarningDialog(a_parentComponent, a_message, null, a_linkedInformation);
	}

	/**
	 * Displays a warning message dialog. Words are wrapped automatically if a message line is too long.
	 * @param a_parentComponent The parent component for this dialog. If it is null or the parent
	 *                          component is not within a frame, the dialog's parent frame is the
	 *                          default frame.
	 * @param a_title The title of the message dialog
	 * @param a_message The message to be displayed. It is interpreted as HTML. You do not need to put in
	 * formatting tags, as the text will be auto-formatted in a way that the dialog's size is very close
	 * to the golden ratio.
	 * @param a_linkedInformation a clickable information message that is appended to the text
	 */
	public static void showWarningDialog(Component a_parentComponent, String a_message, String a_title,
										 ILinkedInformation a_linkedInformation)
	{
		if (a_title == null)
		{
			a_title = JAPMessages.getString(MSG_TITLE_WARNING);
		}

		showConfirmDialog(a_parentComponent, a_message, a_title, OPTION_TYPE_DEFAULT, MESSAGE_TYPE_WARNING,
						 null, a_linkedInformation);
	}

	/**
	 * Displays a message dialog. Words are wrapped automatically if a message line is too long.
	 * @param a_parentDialog The parent dialog for this dialog. If it is null,
	 *                       the dialog's parent frame is the default frame.
	 * @param a_title The title of the message dialog
	 * @param a_message The message to be displayed. It is interpreted as HTML. You do not need to put in
	 * formatting tags, as the text will be auto-formatted in a way that the dialog's size is very close
	 * to the golden ratio.
	 * @param a_icon an icon that will be displayed on the dialog
	 * @param a_messageType use the message types from JOptionPane
	 * @param a_optionType use the option types from JOptionPane
	 * @return The value the user has selected. RETURN_VALUE_UNINITIALIZED implies
	 * the user has not yet made a choice.
	 * @see javax.swing.JOptionPane
	 */
	public static int showConfirmDialog(JAPDialog a_parentDialog, String a_message, String a_title,
									   int a_optionType, int a_messageType, Icon a_icon)
	{
		return showConfirmDialog(a_parentDialog, a_message, a_title, a_optionType, a_messageType, a_icon,
								 null);
	}

	/**
	 * Displays a message dialog. Words are wrapped automatically if a message line is too long.
	 * @param a_parentComponent The parent component for this dialog. If it is null or the parent
	 *                          component is not within a frame, the dialog's parent frame is the
	 *                          default frame.
	 * @param a_title The title of the message dialog
	 * @param a_message The message to be displayed. It is interpreted as HTML. You do not need to put in
	 * formatting tags, as the text will be auto-formatted in a way that the dialog's size is very close
	 * to the golden ratio.
	 * @param a_icon an icon that will be displayed on the dialog
	 * @param a_messageType use the message types from JOptionPane
	 * @param a_optionType use the option types from JOptionPane
	 * @return The value the user has selected. RETURN_VALUE_UNINITIALIZED implies
	 * the user has not yet made a choice.
	 * @see javax.swing.JOptionPane
	 */
	public static int showConfirmDialog(Component a_parentComponent, String a_message, String a_title,
									   int a_optionType, int a_messageType, Icon a_icon)
	{
		return showConfirmDialog(a_parentComponent, a_message, a_title, a_optionType, a_messageType, a_icon,
								null);
	}

	/**
	 * Displays a message dialog. Words are wrapped automatically if a message line is too long.
	 * @param a_parentDialog The parent dialog for this dialog. If it is null,
	 *                       the dialog's parent frame is the default frame.
	 * @param a_title The title of the message dialog
	 * @param a_message The message to be displayed. It is interpreted as HTML. You do not need to put in
	 * formatting tags, as the text will be auto-formatted in a way that the dialog's size is very close
	 * to the golden ratio.
	 * @param a_icon an icon that will be displayed on the dialog
	 * @param a_messageType use the message types from JOptionPane
	 * @param a_optionType use the option types from JOptionPane
	 * @param a_linkedInformation a clickable information message that is appended to the text
	 * @return The value the user has selected. RETURN_VALUE_UNINITIALIZED implies
	 * the user has not yet made a choice.
	 * @see javax.swing.JOptionPane
	 */
	public static int showConfirmDialog(JAPDialog a_parentDialog, String a_message, String a_title,
									   int a_optionType, int a_messageType, Icon a_icon,
									   ILinkedInformation a_linkedInformation)
	{
		return showConfirmDialog(getInternalDialog(a_parentDialog), a_message, a_title, a_optionType,
								a_messageType, a_icon, a_linkedInformation);
	}

	/**
	 * Displays a confirm dialog. Words are wrapped automatically if a message line is too long.
	 * @param a_parentComponent The parent component for this dialog. If it is null or the parent
	 *                          component is not within a frame, the dialog's parent frame is the
	 *                          default frame.
	 * @param a_title The title of the message dialog
	 * @param a_message The message to be displayed. It is interpreted as HTML. You do not need to put in
	 * formatting tags, as the text will be auto-formatted in a way that the dialog's size is very close
	 * to the golden ratio.
	 * @param a_icon an icon that will be displayed on the dialog
	 * @param a_messageType use the message types from JOptionPane
	 * @param a_optionType use the option types from JOptionPane
	 * @param a_linkedInformation a clickable information message that is appended to the text
	 * @return The value the user has selected. RETURN_VALUE_UNINITIALIZED implies
	 * the user has not yet made a choice or that the current thread has been interrupted
	 * @see javax.swing.JOptionPane
	 */
	public static int showConfirmDialog(Component a_parentComponent, String a_message, String a_title,
										int a_optionType, int a_messageType, Icon a_icon,
										ILinkedInformation a_linkedInformation)
	{
		return showConfirmDialog(a_parentComponent, a_message, a_title, new Options(a_optionType),
								 a_messageType, a_icon, a_linkedInformation);
	}

/**
	 * Displays a confirm dialog. Words are wrapped automatically if a message line is too long.
	 * @param a_parentComponent The parent component for this dialog. If it is null or the parent
	 *                          component is not within a frame, the dialog's parent frame is the
	 *                          default frame.
	 * @param a_title The title of the message dialog
	 * @param a_message The message to be displayed. It is interpreted as HTML. You do not need to put in
	 * formatting tags, as the text will be auto-formatted in a way that the dialog's size is very close
	 * to the golden ratio.
	 * @param a_messageType use the message types from JOptionPane
	 * @param a_options use the option types from JOptionPane
	 * @param a_linkedInformation a clickable information message that is appended to the text
	 * @return The value the user has selected. RETURN_VALUE_UNINITIALIZED implies
	 * the user has not yet made a choice or that the current thread has been interrupted
	 * @see javax.swing.JOptionPane
	 */
	public static int showConfirmDialog(Component a_parentComponent, String a_message, String a_title,
										Options a_options, int a_messageType,
										ILinkedInformation a_linkedInformation)
	{
		return showConfirmDialog(a_parentComponent, a_message, a_title, a_options, a_messageType, null,
								 a_linkedInformation);
	}

	/**
	 * Displays a confirm dialog. Words are wrapped automatically if a message line is too long.
	 * @param a_parentComponent The parent component for this dialog. If it is null or the parent
	 *                          component is not within a frame, the dialog's parent frame is the
	 *                          default frame.
	 * @param a_title The title of the message dialog
	 * @param a_message The message to be displayed. It is interpreted as HTML. You do not need to put in
	 * formatting tags, as the text will be auto-formatted in a way that the dialog's size is very close
	 * to the golden ratio.
	 * @param a_icon an icon that will be displayed on the dialog
	 * @param a_messageType use the message types from JOptionPane
	 * @param a_options use the option types from JOptionPane
	 * @param a_linkedInformation a clickable information message that is appended to the text
	 * @return The value the user has selected. RETURN_VALUE_UNINITIALIZED implies
	 * the user has not yet made a choice or that the current thread has been interrupted
	 * @see javax.swing.JOptionPane
	 */
	public static int showConfirmDialog(Component a_parentComponent, String a_message, String a_title,
										Options a_options, int a_messageType, Icon a_icon,
										ILinkedInformation a_linkedInformation)
	{
		JAPDialog dialog;
		JAPHelpContext.IHelpContext helpContext = null;
		DialogContentPane dialogContentPane;
		JComponent contentPane;
		String message;
		String strLinkedInformation;
		JAPHtmlMultiLineLabel label;
		PreferredWidthBoxPanel dummyBox;
		JComponent linkLabel;
		boolean bForceApplicationModality = false;
		boolean bOnTop = false;
		String yesOKText = null;
		String cancelText = null;
		String noText = null;
		Vector vecOptions = new Vector();

		if (ms_bConsoleOnly)
		{
			LogHolder.log(LogLevel.ALERT, LogType.GUI, a_message);
			return RETURN_VALUE_UNINITIALIZED;
		}

		if (a_options.getYesOKText() != null && a_options.getYesOKText().trim().length() > 0)
		{
			yesOKText = a_options.getYesOKText();
		}
		if (a_options.getNoText() != null && a_options.getNoText().trim().length() > 0)
		{
			noText = a_options.getNoText();
		}
		if (a_options.getCancelText() != null && a_options.getCancelText().trim().length() > 0)
		{
			cancelText = a_options.getCancelText();
		}

		if (a_message == null)
		{
			a_message = "";
		}
		a_message = JAPHtmlMultiLineLabel.removeHTMLHEADAndBODYTags(a_message);
		message = a_message;

		if (a_title == null)
		{
			a_title = JAPMessages.getString(MSG_TITLE_CONFIRMATION);
		}

		if (a_linkedInformation != null)
		{
			bForceApplicationModality = a_linkedInformation.isApplicationModalityForced();
			bOnTop = a_linkedInformation.isOnTop();

			/*
			 * If the linked information contains a help context, display the help button instead of a link
			 */
			if (a_linkedInformation instanceof JAPHelpContext.IHelpContext)
			{
				helpContext = (JAPHelpContext.IHelpContext) a_linkedInformation;
				if (a_linkedInformation.getType() == ILinkedInformation.TYPE_DEFAULT)
				{
					a_linkedInformation = null;
				}
			}
		}

		if (a_linkedInformation != null && a_linkedInformation.getMessage() != null &&
			a_linkedInformation.getMessage().trim().length() > 0)
		{
			strLinkedInformation =
				JAPHtmlMultiLineLabel.removeTagsAndNewLines(a_linkedInformation.getMessage());

			if (a_linkedInformation.getType() != ILinkedInformation.TYPE_CHECKBOX_TRUE &&
				a_linkedInformation.getType() != ILinkedInformation.TYPE_CHECKBOX_FALSE)
			{
				// this is not a checkbox
				message += JAPHtmlMultiLineLabel.TAG_BREAK +
					JAPHtmlMultiLineLabel.TAG_A_OPEN + strLinkedInformation +
					JAPHtmlMultiLineLabel.TAG_A_CLOSE;
			}
		}
		else
		{
			strLinkedInformation = null;
		}

		/*
		 * Set the dialog parameters and get its label and content pane.
		 */
		dialog = new JAPDialog(a_parentComponent, a_title, true, bForceApplicationModality);
		dialogContentPane = new DialogContentPane(dialog,
												  new DialogContentPane.Layout(null, a_messageType, a_icon),
												  new DialogContentPane.Options(a_options.getOptionType(),
			helpContext));
		if (dialogContentPane.getButtonHelp() != null)
		{
			vecOptions.addElement(dialogContentPane.getButtonHelp().getText());
		}
		if (dialogContentPane.getButtonYesOK() != null)
		{
			if (yesOKText != null)
			{
				dialogContentPane.getButtonYesOK().setText(yesOKText);
			}
			vecOptions.addElement(dialogContentPane.getButtonYesOK().getText());
		}
		if (dialogContentPane.getButtonCancel() != null)
		{
			if (cancelText != null)
			{
				dialogContentPane.getButtonCancel().setText(cancelText);
			}
			vecOptions.addElement(dialogContentPane.getButtonCancel().getText());
		}
		if (dialogContentPane.getButtonNo() != null)
		{
			if (noText != null)
			{
				dialogContentPane.getButtonNo().setText(noText);
			}
			vecOptions.addElement(dialogContentPane.getButtonNo().getText());
		}
		dialogContentPane.setDefaultButtonOperation(DialogContentPane.ON_CLICK_DISPOSE_DIALOG);

		try
		{
			if (!SwingUtilities.isEventDispatchThread())
			{
				/**
				 * This logic is needed if the event thread is interrupted. In this case, the HTML rendering
				 * engine is interrupted, too, and the multi-line label shows always (0,0) as preferred size,
				 * for example.
				 */
				SwingUtilities.invokeAndWait(new Runnable()
				{
					public void run()
					{
						// Instanciate the label here, as otherwise it might cause a NullPinterException...
						JAPHtmlMultiLineLabel runLabel = new JAPHtmlMultiLineLabel("Text");
						runLabel.setText(runLabel.getText());
						runLabel.revalidate();
					}
				});
			}
		}
		catch (InterruptedException a_e)
		{
		}
		catch (InvocationTargetException a_e)
		{
		}
		// test if labels will be formatted correctly
		try
		{
			label = new JAPHtmlMultiLineLabel("Text");
		}
		catch (NullPointerException a_e)
		{
			if (Thread.currentThread().isInterrupted())
			{
				return RETURN_VALUE_UNINITIALIZED;
			}
			throw a_e;
		}
		if (label.getPreferredSize().width == 0 || label.getPreferredSize().height == 0)
		{
			LogHolder.log(LogLevel.EMERG, LogType.GUI,
						  "Dialog label size is invalid! This dialog might not show any label!");
		}
		try
		{
			label = new JAPHtmlMultiLineLabel(message);
			label.setFontStyle(JAPHtmlMultiLineLabel.FONT_STYLE_PLAIN);
		}
		catch (NullPointerException a_e)
		{
			if (Thread.currentThread().isInterrupted())
			{
				return RETURN_VALUE_UNINITIALIZED;
			}
			throw a_e;
		}

		dummyBox = new PreferredWidthBoxPanel();
		if (strLinkedInformation != null &&
			(a_linkedInformation.getType() == ILinkedInformation.TYPE_CHECKBOX_TRUE ||
			 a_linkedInformation.getType() == ILinkedInformation.TYPE_CHECKBOX_FALSE))
		{
			// add a dummy checkbox to get the needed additional height of it; the text field may not be empty
			linkLabel = new JCheckBox("Text");
			linkLabel.setFont(label.getFont());
			dummyBox.add(linkLabel);
		}
		dummyBox.add(label);
		dialogContentPane.setContentPane(dummyBox);
		dialogContentPane.updateDialog();
		//dialog.pack();
		// trick: a dialog's content pane is always a JComponent; it is needed to set the min/max size
		contentPane = (JComponent) dialog.getContentPane();

		/**
		 * Calculate the optimal dialog size with respect to the golden ratio.
		 * The width defines the longer side.
		 */
		Dimension bestDimension = null;
		Dimension minSize;
		double currentDelta;
		double bestDelta;
		int currentWidth;
		int bestWidth;
		int failed;
		int minLabelWidth;
		boolean bAlgorithmFailed;
		Icon icon;

		// get the minimum width and height that is needed to display this dialog without any text
		icon = a_icon;
		if (icon == null)
		{
			icon = DialogContentPane.getMessageIcon(a_messageType);
		}
		String[] options = new String[vecOptions.size()];
		for (int i = 0; i < options.length; i++)
		{
			options[i] = vecOptions.elementAt(i).toString();
		}
		JDialog tempDialog = new JOptionPane("", a_messageType, a_options.getOptionType(), icon,
											 options).createDialog(a_parentComponent, a_title);
		minSize = new Dimension(tempDialog.getContentPane().getSize());
		tempDialog.dispose();
		tempDialog = null;
		minSize.setSize(minSize.width / 2, minSize.height);

		// set the maximum width that is allowed for the content pane
		int maxWidth = 0;
		try
		{
			Component parentWindow = GUIUtils.getParentWindow(a_parentComponent);
			if (parentWindow == null)
			{
				return RETURN_VALUE_UNINITIALIZED;
			}
			maxWidth = (int) parentWindow.getSize().width;
		}
		catch (NullPointerException a_e)
		{
			if (Thread.currentThread().isInterrupted())
			{
				return RETURN_VALUE_UNINITIALIZED;
			}
			throw a_e;

		}
		if (maxWidth < minSize.width * 4)
		{
			maxWidth = minSize.width * 4;
		}
		// if the text in the content pane is short, reduce the max width to the text length
		maxWidth = Math.min(contentPane.getWidth(), maxWidth);

		/**
		 * Do a quick heuristic to approximate the golden ratio for the dialog size.
		 */
		bestDelta = Double.MAX_VALUE;
		//currentWidth = contentPane.getWidth() / 2;
		//currentWidth = maxWidth;
		currentWidth = Math.min(500, contentPane.getWidth());
		bestWidth = currentWidth;
		failed = 0;
		minLabelWidth = label.getMinimumSize().width;
		bAlgorithmFailed = true;
		for (int i = 0; i < NUMBER_OF_HEURISTIC_ITERATIONS; i++)
		{
			/**
			 * Set the exact width of the frame.
			 * The following trick must be explained:
			 * Put the content pane in a box and the box in the dialog. Set the total with of the box
			 * and pack the dialog, so that the internal label automatically gets the corresponding height.
			 * Get the HTML view of the label and set its preferred width to the current width of this label
			 * that is defined by the total width of the surrounding box. The height of the view may be
			 * unlimited, as the view will adapt its height automatically so that the whole text is
			 * displayed respecting the width that has been set.
			 * @see javax.swing.JLabel.Bounds()
			 * @see javax.swing.JLabel.getTextRectangle()
			 * @see javax.swing.SwingUtilities
			 * @see javax.swing.plaf.basic.BasicHTML
			 * @see javax.swing.text.html.HTMLEditorKit
			 */
			dummyBox = new PreferredWidthBoxPanel();
			dummyBox.add(contentPane);
			dummyBox.setPreferredWidth(currentWidth);
			dialog.setContentPane(dummyBox);

			dialog.pack();
			label.setPreferredWidth(label.getWidth());
			dialog.pack();
			// only accept an optimisation if the label height changed; otherwise, the label might be cut off

			if (dummyBox.getHeight() < minSize.height)
			{
				// the dialog has a smaller height as needed to display all elements, e.g. the icon
				LogHolder.log(LogLevel.NOTICE, LogType.GUI, "Dialog height was too small.");
				dummyBox.setPreferredHeigth(minSize.height);
				dialog.pack();
			}

			// patch for CDE/Motif
			/*
			   if (label.getSize().width < minLabelWidth)
			   {
			 System.out.println("before:" + dialog.getSize());
			 label.setSize(new Dimension(minLabelWidth, label.getSize().height));
			 System.out.println("after:" +  dialog.getSize());
			   }*/

			currentWidth = dummyBox.getWidth();
			currentDelta = getOptimizedFormatDelta(dialog);
			if (Math.abs(currentDelta) < Math.abs(bestDelta) &&
				(i == 0 || // patch for CDE/Motif, tolerate this error in the first run
				 label.getSize().width >= minLabelWidth))
			{
				bestDimension = new Dimension(dummyBox.getSize());
				bestDelta = currentDelta;
				bestWidth = currentWidth;
				currentWidth += bestDelta / 2.0;
				if (label.getSize().width < minLabelWidth)
				{
					// patch for CDE/Motif
					failed++;
				}
				else
				{
					bAlgorithmFailed = false;
					failed = 0;
				}
			}
			else
			{
				currentWidth = bestWidth + (int) (bestDelta / (3.0 * (failed + 1.0)));
				failed++;
			}

			// the objective function value
			//System.out.println("bestDelta: " + bestDelta + "  currentDelta:" + currentDelta);

			currentWidth = (int) Math.max(currentWidth, minSize.width);
			if (currentWidth == bestWidth)
			{
				break;
			}
		}
		if (bAlgorithmFailed)
		{
			LogHolder.log(LogLevel.ERR, LogType.GUI, "Auto-formatting of dialog failed!");
		}

		/*
		  System.out.println("CurrentSize: " + dummyBox.getSize() + "_" + contentPane.getSize());
		 System.out.println("MaximumSize: " + dummyBox.getMaximumSize() + "_" + contentPane.getMaximumSize());
		 System.out.println("PreferredSize: " + dummyBox.getPreferredSize() + "_" + contentPane.getPreferredSize());
		 */

		/**
		 * Recreate the dialog and set its final size.
		 */
		dummyBox = new PreferredWidthBoxPanel();
		try
		{
			label = new JAPHtmlMultiLineLabel("<font color=#000000>" + a_message + "</font>");
			label.setFontStyle(JAPHtmlMultiLineLabel.FONT_STYLE_PLAIN);
		}
		catch (NullPointerException a_e)
		{
			if (Thread.currentThread().isInterrupted())
			{
				return RETURN_VALUE_UNINITIALIZED;
			}
			throw a_e;
		}

		dummyBox.add(label);
		linkLabel = null;
		if (strLinkedInformation != null)
		{
			if (a_linkedInformation.getType() == ILinkedInformation.TYPE_SELECTABLE_LINK)
			{
				/** @todo this is not nice in most of the old JDKs) */
				JTextPane textPane = GUIUtils.createSelectableAndResizeableLabel(dummyBox);
				/*
				 SimpleAttributeSet attributes;
				 attributes = new SimpleAttributeSet(textPane.getCharacterAttributes());
				 attributes.addAttribute(CharacterConstants.Underline, Boolean.TRUE);
				 textPane.setCharacterAttributes(attributes, true);
				 */

				textPane.setText(strLinkedInformation);
				textPane.setFont(label.getFont());
				textPane.setMargin(new java.awt.Insets(0, 0, 0, 0));
				textPane.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 1, 0));
				textPane.setForeground(java.awt.Color.blue);
				textPane.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
				linkLabel = textPane;
				linkLabel.addMouseListener(new LinkedInformationClickListener(a_linkedInformation));
			}
			else if (a_linkedInformation.getType() == ILinkedInformation.TYPE_CHECKBOX_TRUE ||
					 a_linkedInformation.getType() == ILinkedInformation.TYPE_CHECKBOX_FALSE)
			{
				linkLabel =
					new JCheckBox(strLinkedInformation,
								  a_linkedInformation.getType() ==
								  ILinkedInformation.TYPE_CHECKBOX_TRUE);
				linkLabel.setFont(label.getFont());
				( (JCheckBox) linkLabel).addItemListener(
					new LinkedInformationClickListener(a_linkedInformation));
			}
			else
			{
				linkLabel = new JAPHtmlMultiLineLabel(JAPHtmlMultiLineLabel.TAG_A_OPEN +
					strLinkedInformation + JAPHtmlMultiLineLabel.TAG_A_CLOSE);
				linkLabel.addMouseListener(new LinkedInformationClickListener(a_linkedInformation));
				linkLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
			}

			dummyBox.add(linkLabel);
		}

		dialogContentPane.setContentPane(dummyBox);
		if (a_options.getOptionType() == OPTION_TYPE_CANCEL ||
			a_options.getOptionType() == OPTION_TYPE_OK_CANCEL ||
			a_options.getOptionType() == OPTION_TYPE_YES_NO_CANCEL)
		{
			dialogContentPane.setDefaultButton(DialogContentPane.DEFAULT_BUTTON_CANCEL);
		}
		else if (a_options.getOptionType() == OPTION_TYPE_YES_NO)
		{
			dialogContentPane.setDefaultButton(DialogContentPane.DEFAULT_BUTTON_NO);
		}

		dialogContentPane.updateDialog();
		( (JComponent) dialog.getContentPane()).setPreferredSize(bestDimension);
		dialog.pack();
		if (bestDelta != getOptimizedFormatDelta(dialog))
		{
			LogHolder.log(LogLevel.ERR, LogType.GUI,
						  "Calculated dialog size differs from real size!");
		}
		LogHolder.log(LogLevel.NOTICE, LogType.GUI,
					  "Dialog golden ratio delta: " + getOptimizedFormatDelta(dialog));

		dialog.setResizable(false);
		dialog.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		dialog.addWindowListener(new SimpleDialogButtonFocusWindowAdapter(dialogContentPane));
		dialog.m_bOnTop = bOnTop;
		dialog.setVisible(true);
		dialog = null;

		return dialogContentPane.getButtonValue();
	}


	/**
	 * Displays a message dialog that asks the user for a confirmation.
	 * Words are wrapped automatically if a message line is too long.
	 * @param a_parentDialog The parent dialog for this dialog. If it is null,
	 *                       the dialog's parent frame is the default frame.
	 * @param a_message The message to be displayed. It is interpreted as HTML. You do not need to put in
	 * formatting tags, as the text will be auto-formatted in a way that the dialog's size is very close
	 * to the golden ratio.
	 * @return true if the answer was 'yes'; fale otherwise
	 */
	public static boolean showYesNoDialog(JAPDialog a_parentDialog, String a_message)
	{
		return showYesNoDialog(getInternalDialog(a_parentDialog), a_message);
	}

	/**
	 * Displays a message dialog that asks the user for a confirmation.
	 * Words are wrapped automatically if a message line is too long.
	 * @param a_parentDialog The parent dialog for this dialog. If it is null,
	 *                       the dialog's parent frame is the default frame.
	 * @param a_message The message to be displayed. It is interpreted as HTML. You do not need to put in
	 * formatting tags, as the text will be auto-formatted in a way that the dialog's size is very close
	 * to the golden ratio.
	 * @param a_linkedInformation a clickable information message that is appended to the text
	 * @return true if the answer was 'yes'; fale otherwise
	 */
	public static boolean showYesNoDialog(JAPDialog a_parentDialog, String a_message,
										  ILinkedInformation a_linkedInformation)
	{
		return showYesNoDialog(getInternalDialog(a_parentDialog), a_message, a_linkedInformation);
	}

	/**
	 * Displays a message dialog that asks the user for a confirmation.
	 * Words are wrapped automatically if a message line is too long.
	 * @param a_parentComponent The parent component for this dialog. If it is null or the parent
	 *                          component is not within a frame, the dialog's parent frame is the
	 *                          default frame.
	 * @param a_message The message to be displayed. It is interpreted as HTML. You do not need to put in
	 * formatting tags, as the text will be auto-formatted in a way that the dialog's size is very close
	 * to the golden ratio.
	 * @return true if the answer was 'yes'; fale otherwise
	 */
	public static boolean showYesNoDialog(Component a_parentComponent, String a_message)
	{
		return showYesNoDialog(a_parentComponent, a_message, (String)null);
	}

	/**
	 * Displays a message dialog that asks the user for a confirmation.
	 * Words are wrapped automatically if a message line is too long.
	 * @param a_parentComponent The parent component for this dialog. If it is null or the parent
	 *                          component is not within a frame, the dialog's parent frame is the
	 *                          default frame.
	 * @param a_message The message to be displayed. It is interpreted as HTML. You do not need to put in
	 * formatting tags, as the text will be auto-formatted in a way that the dialog's size is very close
	 * to the golden ratio.
	 * @param a_linkedInformation a clickable information message that is appended to the text
	 * @return true if the answer was 'yes'; fale otherwise
	 */
	public static boolean showYesNoDialog(Component a_parentComponent, String a_message,
										  ILinkedInformation a_linkedInformation)
	{
		return showYesNoDialog(a_parentComponent, a_message, null, a_linkedInformation);
	}

	/**
	 * Displays a message dialog that asks the user for a confirmation.
	 * Words are wrapped automatically if a message line is too long.
	 * @param a_parentDialog The parent dialog for this dialog. If it is null,
	 *                       the dialog's parent frame is the default frame.
	 * @param a_title The title of the message dialog
	 * @param a_message The message to be displayed. It is interpreted as HTML. You do not need to put in
	 * formatting tags, as the text will be auto-formatted in a way that the dialog's size is very close
	 * to the golden ratio.
	 * @return true if the answer was 'yes'; fale otherwise
	 */
	public static boolean showYesNoDialog(JAPDialog a_parentDialog,  String a_message, String a_title)
	{
		return showYesNoDialog(getInternalDialog(a_parentDialog), a_message, a_title);
	}

	/**
	 * Displays a message dialog that asks the user for a confirmation.
	 * Words are wrapped automatically if a message line is too long.
	 * @param a_parentDialog The parent dialog for this dialog. If it is null,
	 *                       the dialog's parent frame is the default frame.
	 * @param a_title The title of the message dialog
	 * @param a_message The message to be displayed. It is interpreted as HTML. You do not need to put in
	 * formatting tags, as the text will be auto-formatted in a way that the dialog's size is very close
	 * to the golden ratio.
	 * @param a_linkedInformation a clickable information message that is appended to the text
	 * @return true if the answer was 'yes'; fale otherwise
	 */
	public static boolean showYesNoDialog(JAPDialog a_parentDialog, String a_message, String a_title,
										  ILinkedInformation a_linkedInformation)
	{
		return showYesNoDialog(getInternalDialog(a_parentDialog), a_message, a_title, a_linkedInformation);
	}

	/**
	 * Displays a message dialog that asks the user for a confirmation.
	 * Words are wrapped automatically if a message line is too long.
	 * @param a_parentComponent The parent component for this dialog. If it is null or the parent
	 *                          component is not within a frame, the dialog's parent frame is the
	 *                          default frame.
	 * @param a_title The title of the message dialog
	 * @param a_message The message to be displayed. It is interpreted as HTML. You do not need to put in
	 * formatting tags, as the text will be auto-formatted in a way that the dialog's size is very close
	 * to the golden ratio.
	 * @return true if the answer was 'yes'; fale otherwise
	 */
	public static boolean showYesNoDialog(Component a_parentComponent, String a_message, String a_title)
	{
		return showYesNoDialog(a_parentComponent, a_message, a_title, null);
	}

	/**
	 * Displays a message dialog that asks the user for a confirmation.
	 * Words are wrapped automatically if a message line is too long.
	 * @param a_parentComponent The parent component for this dialog. If it is null or the parent
	 *                          component is not within a frame, the dialog's parent frame is the
	 *                          default frame.
	 * @param a_title The title of the message dialog
	 * @param a_message The message to be displayed. It is interpreted as HTML. You do not need to put in
	 * formatting tags, as the text will be auto-formatted in a way that the dialog's size is very close
	 * to the golden ratio.
	 * @param a_linkedInformation a clickable information message that is appended to the text
	 * @return true if the answer was 'yes'; fale otherwise
	 */
	public static boolean showYesNoDialog(Component a_parentComponent, String a_message, String a_title,
										  ILinkedInformation a_linkedInformation)
	{
		int response;

		if (a_title == null)
		{
			a_title = JAPMessages.getString(MSG_TITLE_CONFIRMATION);
		}
		response = showConfirmDialog(a_parentComponent, a_message, a_title, OPTION_TYPE_YES_NO,
									MESSAGE_TYPE_QUESTION, null, a_linkedInformation);

		return RETURN_VALUE_YES == response;
	}

	/**
	 * Brings up a dialog where the number of choices is determined by the optionType parameter.
	 * The messageType parameter is primarily used to supply a default icon from the look and feel.
	 * Words are wrapped automatically if a message line is too long.
	 * @param a_parentDialog The parent dialog for this dialog. If it is null,
	 *                       the dialog's parent frame is the default frame.
	 * @param a_message The message to be displayed. It is interpreted as HTML. You do not need to put in
	 * formatting tags, as the text will be auto-formatted in a way that the dialog's size is very close
	 * to the golden ratio.
	 * @param a_messageType use the message types from JOptionPane
	 * @param a_optionType use the option types from JOptionPane
	 * @param a_icon an icon that will be displayed on the dialog
	 * @return an int indicating the option selected by the user
	 * @see javax.swing.JOptionPane
	 */
	public static int showConfirmDialog(JAPDialog a_parentDialog, String a_message,
										int a_optionType, int a_messageType, Icon a_icon)
	{
		return showConfirmDialog(getInternalDialog(a_parentDialog), a_message, null,
								 a_optionType, a_messageType, a_icon, null);
	}

	/**
	 * Brings up a dialog where the number of choices is determined by the optionType parameter.
	 * The messageType parameter is primarily used to supply a default icon from the look and feel.
	 * Words are wrapped automatically if a message line is too long.
	 * @param a_parentComponent The parent component for this dialog. If it is null or the parent
	 *                          component is not within a frame, the dialog's parent frame is the
	 *                          default frame.
	 * @param a_message The message to be displayed. It is interpreted as HTML. You do not need to put in
	 * formatting tags, as the text will be auto-formatted in a way that the dialog's size is very close
	 * to the golden ratio.
	 * @param a_messageType use the message types from JOptionPane
	 * @param a_optionType use the option types from JOptionPane
	 * @param a_icon an icon that will be displayed on the dialog
	 * @return an int indicating the option selected by the user
	 * @see javax.swing.JOptionPane
	 */
	public static int showConfirmDialog(Component a_parentComponent, String a_message,
										int a_optionType, int a_messageType, Icon a_icon)
	{
		return showConfirmDialog(a_parentComponent, a_message, null, a_optionType, a_messageType, a_icon,
								 null);
	}

	/**
	 * Brings up a dialog where the number of choices is determined by the optionType parameter.
	 * The messageType parameter is primarily used to supply a default icon from the look and feel.
	 * Words are wrapped automatically if a message line is too long.
	 * @param a_parentDialog The parent dialog for this dialog. If it is null,
	 *                       the dialog's parent frame is the default frame.
	 * @param a_message The message to be displayed. It is interpreted as HTML. You do not need to put in
	 * formatting tags, as the text will be auto-formatted in a way that the dialog's size is very close
	 * to the golden ratio.
	 * @param a_messageType use the message types from JOptionPane
	 * @param a_optionType use the option types from JOptionPane
	 * @return an int indicating the option selected by the user
	 * @see javax.swing.JOptionPane
	 */
	public static int showConfirmDialog(JAPDialog a_parentDialog, String a_message,
										int a_optionType, int a_messageType)
	{
		return showConfirmDialog(getInternalDialog(a_parentDialog), a_message, null,
								 a_optionType, a_messageType, null, null);
	}

	/**
	 * Brings up a dialog where the number of choices is determined by the optionType parameter.
	 * The messageType parameter is primarily used to supply a default icon from the look and feel.
	 * Words are wrapped automatically if a message line is too long.
	 * @param a_parentComponent The parent component for this dialog. If it is null or the parent
	 *                          component is not within a frame, the dialog's parent frame is the
	 *                          default frame.
	 * @param a_message The message to be displayed. It is interpreted as HTML. You do not need to put in
	 * formatting tags, as the text will be auto-formatted in a way that the dialog's size is very close
	 * to the golden ratio.
	 * @param a_messageType use the message types from JOptionPane
	 * @param a_optionType use the option types from JOptionPane
	 * @return an int indicating the option selected by the user
	 * @see javax.swing.JOptionPane
	 */
	public static int showConfirmDialog(Component a_parentComponent, String a_message,
										int a_optionType, int a_messageType)
	{
		return showConfirmDialog(a_parentComponent, a_message, null, a_optionType, a_messageType, null,
								 null);
	}

	/**
	 * Brings up a dialog where the number of choices is determined by the optionType parameter.
	 * The messageType parameter is primarily used to supply a default icon from the look and feel.
	 * Words are wrapped automatically if a message line is too long.
	 * @param a_parentDialog The parent dialog for this dialog. If it is null,
	 *                       the dialog's parent frame is the default frame.
	 * @param a_message The message to be displayed. It is interpreted as HTML. You do not need to put in
	 * formatting tags, as the text will be auto-formatted in a way that the dialog's size is very close
	 * to the golden ratio.
	 * @param a_messageType use the message types from JOptionPane
	 * @param a_optionType use the option types from JOptionPane
	 * @param a_linkedInformation a clickable information message that is appended to the text
	 * @return an int indicating the option selected by the user
	 * @see javax.swing.JOptionPane
	 */
	public static int showConfirmDialog(JAPDialog a_parentDialog, String a_message,
										int a_optionType, int a_messageType,
										ILinkedInformation a_linkedInformation)
	{
		return showConfirmDialog(getInternalDialog(a_parentDialog), a_message, null,
								 a_optionType, a_messageType, null, a_linkedInformation);
	}

	/**
	 * Brings up a dialog where the number of choices is determined by the optionType parameter.
	 * The messageType parameter is primarily used to supply a default icon from the look and feel.
	 * Words are wrapped automatically if a message line is too long.
	 * @param a_parentComponent The parent component for this dialog. If it is null or the parent
	 *                          component is not within a frame, the dialog's parent frame is the
	 *                          default frame.
	 * @param a_message The message to be displayed. It is interpreted as HTML. You do not need to put in
	 * formatting tags, as the text will be auto-formatted in a way that the dialog's size is very close
	 * to the golden ratio.
	 * @param a_messageType use the message types from JOptionPane
	 * @param a_optionType use the option types from JOptionPane
	 * @param a_linkedInformation a clickable information message that is appended to the text
	 * @return an int indicating the option selected by the user
	 * @see javax.swing.JOptionPane
	 */
	public static int showConfirmDialog(Component a_parentComponent, String a_message,
										int a_optionType, int a_messageType,
										ILinkedInformation a_linkedInformation)
	{
		return showConfirmDialog(a_parentComponent, a_message, null,
								 a_optionType, a_messageType, null, a_linkedInformation);
	}

	/**
	 * Brings up a dialog where the number of choices is determined by the optionType parameter.
	 * The messageType parameter is primarily used to supply a default icon from the look and feel.
	 * Words are wrapped automatically if a message line is too long.
	 * @param a_parentDialog The parent dialog for this dialog. If it is null,
	 *                       the dialog's parent frame is the default frame.
	 * @param a_title The title of the message dialog
	 * @param a_message The message to be displayed. It is interpreted as HTML. You do not need to put in
	 * formatting tags, as the text will be auto-formatted in a way that the dialog's size is very close
	 * to the golden ratio.
	 * @param a_messageType use the message types from JOptionPane
	 * @param a_optionType use the option types from JOptionPane
	 * @return an int indicating the option selected by the user
	 * @see javax.swing.JOptionPane
	 */
	public static int showConfirmDialog(JAPDialog a_parentDialog, String a_message, String a_title,
										int a_optionType, int a_messageType)
	{
		return showConfirmDialog(getInternalDialog(a_parentDialog), a_message, a_title,
								 a_optionType, a_messageType, null, null);
	}

	/**
	 * Brings up a dialog where the number of choices is determined by the optionType parameter.
	 * The messageType parameter is primarily used to supply a default icon from the look and feel.
	 * Words are wrapped automatically if a message line is too long.
	 * @param a_parentComponent The parent component for this dialog. If it is null or the parent
	 *                          component is not within a frame, the dialog's parent frame is the
	 *                          default frame.
	 * @param a_title The title of the message dialog
	 * @param a_message The message to be displayed. It is interpreted as HTML. You do not need to put in
	 * formatting tags, as the text will be auto-formatted in a way that the dialog's size is very close
	 * to the golden ratio.
	 * @param a_messageType use the message types from JOptionPane
	 * @param a_optionType use the option types from JOptionPane
	 * @return an int indicating the option selected by the user
	 * @see javax.swing.JOptionPane
	 */
	public static int showConfirmDialog(Component a_parentComponent, String a_message, String a_title,
										int a_optionType, int a_messageType)
	{
		return showConfirmDialog(a_parentComponent, a_message, a_title,
								 a_optionType, a_messageType, null, null);
	}


	/**
	 * Brings up a dialog where the number of choices is determined by the optionType parameter.
	 * The messageType parameter is primarily used to supply a default icon from the look and feel.
	 * Words are wrapped automatically if a message line is too long.
	 * @param a_parentDialog The parent dialog for this dialog. If it is null,
	 *                       the dialog's parent frame is the default frame.
	 * @param a_title The title of the message dialog
	 * @param a_message The message to be displayed. It is interpreted as HTML. You do not need to put in
	 * formatting tags, as the text will be auto-formatted in a way that the dialog's size is very close
	 * to the golden ratio.
	 * @param a_messageType use the message types from JOptionPane
	 * @param a_optionType use the option types from JOptionPane
	 * @param a_linkedInformation a clickable information message that is appended to the text
	 * @return an int indicating the option selected by the user
	 * @see javax.swing.JOptionPane
	 */
	public static int showConfirmDialog(JAPDialog a_parentDialog, String a_message, String a_title,
										int a_optionType, int a_messageType,
										ILinkedInformation a_linkedInformation)
	{
		return showConfirmDialog(getInternalDialog(a_parentDialog), a_message, a_title,
								 a_optionType, a_messageType, null, a_linkedInformation);
	}

	/**
	 * Brings up a dialog where the number of choices is determined by the optionType parameter.
	 * The messageType parameter is primarily used to supply a default icon from the look and feel.
	 * Words are wrapped automatically if a message line is too long.
	 * @param a_parentComponent The parent component for this dialog. If it is null or the parent
	 *                          component is not within a frame, the dialog's parent frame is the
	 *                          default frame.
	 * @param a_title The title of the message dialog
	 * @param a_message The message to be displayed. It is interpreted as HTML. You do not need to put in
	 * formatting tags, as the text will be auto-formatted in a way that the dialog's size is very close
	 * to the golden ratio.
	 * @param a_messageType use the message types from JOptionPane
	 * @param a_optionType use the option types from JOptionPane
	 * @param a_linkedInformation a clickable information message that is appended to the text
	 * @return an int indicating the option selected by the user
	 * @see javax.swing.JOptionPane
	 */
	public static int showConfirmDialog(Component a_parentComponent, String a_message, String a_title,
										int a_optionType, int a_messageType,
										ILinkedInformation a_linkedInformation)
	{
		return showConfirmDialog(a_parentComponent, a_message, a_title, a_optionType, a_messageType,
									null, a_linkedInformation);
	}

	/**
	 * Displays a dialog showing an error message to the user and logs the error message
	 * to the currently used Log.
	 * @param a_parentDialog The parent dialog for this dialog. If it is null or the parent
	 *                       dialog is not within a frame, the dialog's parent frame is the
	 *                       default frame.
	 * @param a_message a message that is shown to the user (may be null)
	 * @param a_logType the log type for this error
	 * @see logging.LogHolder
	 * @see logging.LogType
	 * @see logging.Log
	 */
	public static void showErrorDialog(JAPDialog a_parentDialog, String a_message, int a_logType)
	{
		showErrorDialog(a_parentDialog, a_message, a_logType, (Throwable)null);
	}

	/**
	 * Displays a dialog showing an error message to the user and logs the error message
	 * to the currently used Log.
	 * @param a_parentComponent The parent component for this dialog. If it is null or the parent
	 *                          component is not within a frame, the dialog's parent frame is the
	 *                          default frame.
	 * @param a_message a message that is shown to the user (may be null)
	 * @param a_title The title of the message dialog (may be null)
	 * @param a_logType the log type for this error
	 * @param a_linkedInformation a clickable information message that is appended to the text
	 * @see logging.LogHolder
	 * @see logging.LogType
	 * @see logging.Log
	 */
	public static void showErrorDialog(Component a_parentComponent, String a_message, String a_title,
									   int a_logType, ILinkedInformation a_linkedInformation)
	{
		showErrorDialog(a_parentComponent, a_message, a_title, a_logType,
						(Throwable)null, a_linkedInformation);
	}

	/**
	 * Displays a dialog showing an error message to the user and logs the error message
	 * to the currently used Log.
	 * @param a_parentComponent The parent component for this dialog. If it is null or the parent
	 *                          component is not within a frame, the dialog's parent frame is the
	 *                          default frame.
	 * @param a_message a message that is shown to the user (may be null)
	 * @param a_logType the log type for this error
	 * @param a_linkedInformation a clickable information message that is appended to the text
	 * @see logging.LogHolder
	 * @see logging.LogType
	 * @see logging.Log
	 */
	public static void showErrorDialog(Component a_parentComponent, String a_message, int a_logType,
									   ILinkedInformation a_linkedInformation)
	{
		showErrorDialog(a_parentComponent, a_message, null, a_logType,
						(Throwable)null, a_linkedInformation);
	}

	/**
	 * Displays a dialog showing an error message to the user and logs the error message
	 * to the currently used Log.
	 * @param a_parentComponent The parent component for this dialog. If it is null or the parent
	 *                          component is not within a frame, the dialog's parent frame is the
	 *                          default frame.
	 * @param a_message a message that is shown to the user (may be null)
	 * @param a_logType the log type for this error
	 * @see logging.LogHolder
	 * @see logging.LogType
	 * @see logging.Log
	 */
	public static void showErrorDialog(Component a_parentComponent, String a_message, int a_logType)
	{
		showErrorDialog(a_parentComponent, a_message, a_logType, (Throwable)null);
	}

	/**
	 * Displays a dialog showing an error message to the user and logs the error message
	 * to the currently used Log.
	 * @param a_parentComponent The parent component for this dialog. If it is null or the parent
	 *                          component is not within a frame, the dialog's parent frame is the
	 *                          default frame.
	 * @param a_title a title for the error message (may be null)
	 * @param a_message a message that is shown to the user (may be null)
	 * @param a_logType the log type for this error
	 * @see logging.LogHolder
	 * @see logging.LogType
	 * @see logging.Log
	 */
	public static void showErrorDialog(Component a_parentComponent, String a_message,  int a_logType,
									   String a_title)
	{
		showErrorDialog(a_parentComponent, a_message, a_title, a_logType, (Throwable)null,
			(ILinkedInformation)null);
	}


	/**
	 * Displays a dialog showing an error message to the user and logs the error message
	 * to the currently used Log.
	 * @param a_parentDialog The parent dialog for this dialog. If it is null or the parent
	 *                       dialog is not within a frame, the dialog's parent frame is the
	 *                       default frame.
	 * @param a_title a title for the error message (may be null)
	 * @param a_message a message that is shown to the user (may be null)
	 * @param a_logType the log type for this error
	 * @see logging.LogHolder
	 * @see logging.LogType
	 * @see logging.Log
	 */
	public static void showErrorDialog(JAPDialog a_parentDialog, String a_message,  int a_logType,
									   String a_title)
	{
		showErrorDialog(getInternalDialog(a_parentDialog), a_message, a_title, a_logType, (Throwable)null,
						(ILinkedInformation)null);
	}

	/**
	 * Displays a dialog showing an error message to the user and logs the error message
	 * to the currently used Log.
	 * @param a_parentComponent The parent component for this dialog. If it is null or the parent
	 *                          component is not within a frame, the dialog's parent frame is the
	 *                          default frame.
	 * @param a_title a title for the error message (may be null)
	 * @param a_message a message that is shown to the user (may be null)
	 * @param a_logType the log type for this error
	 * @see logging.LogHolder
	 * @see logging.LogType
	 * @see logging.Log
	 */
	public static void showErrorDialog(Component a_parentComponent, String a_message, String a_title,
									   int a_logType)
	{
		showErrorDialog(a_parentComponent, a_message, a_title, a_logType, (Throwable)null,
						(ILinkedInformation)null);
	}

	/**
	 * Displays a dialog showing an error message to the user and logs the error message
	 * to the currently used Log.
	 * @param a_parentDialog The parent dialog for this dialog. If it is null or the parent
	 *                       dialog is not within a frame, the dialog's parent frame is the
	 *                       default frame.
	 * @param a_throwable a Throwable that has been caught (may be null)
	 * @param a_logType the log type for this error
	 * @see logging.LogHolder
	 * @see logging.LogType
	 * @see logging.Log
	 */
	public static void showErrorDialog(JAPDialog a_parentDialog, int a_logType, Throwable a_throwable)
	{
		showErrorDialog(getInternalDialog(a_parentDialog), null, null, a_logType, a_throwable);
	}

	/**
	 * Displays a dialog showing an error message to the user and logs the error message
	 * to the currently used Log.
	 * @param a_parentComponent The parent component for this dialog. If it is null or the parent
	 *                          component is not within a frame, the dialog's parent frame is the
	 *                          default frame.
	 * @param a_throwable a Throwable that has been caught (may be null)
	 * @param a_logType the log type for this error
	 * @see logging.LogHolder
	 * @see logging.LogType
	 * @see logging.Log
	 */
	public static void showErrorDialog(Component a_parentComponent,  int a_logType,
									   Throwable a_throwable)
	{
		showErrorDialog(a_parentComponent, null, null, a_logType, a_throwable);
	}


	/**
	 * Displays a dialog showing an error message to the user and logs the error message
	 * to the currently used Log.
	 * @param a_parentDialog The parent dialog for this dialog. If it is null or the parent
	 *                       dialog is not within a frame, the dialog's parent frame is the
	 *                       default frame.
	 * @param a_throwable a Throwable that has been caught (may be null)
	 * @param a_message a message that is shown to the user (may be null)
	 * @param a_logType the log type for this error
	 * @see logging.LogHolder
	 * @see logging.LogType
	 * @see logging.Log
	 */
	public static void showErrorDialog(JAPDialog a_parentDialog, String a_message, int a_logType,
									   Throwable a_throwable)
	{
		showErrorDialog(getInternalDialog(a_parentDialog), a_message, a_logType, a_throwable);
	}

	/**
	 * Displays a dialog showing an error message to the user and logs the error message
	 * to the currently used Log.
	 * @param a_parentComponent The parent component for this dialog. If it is null or the parent
	 *                          component is not within a frame, the dialog's parent frame is the
	 *                          default frame.
	 * @param a_throwable a Throwable that has been caught (may be null)
	 * @param a_message a message that is shown to the user (may be null)
	 * @param a_logType the log type for this error
	 * @see logging.LogHolder
	 * @see logging.LogType
	 * @see logging.Log
	 */
	public static void showErrorDialog(Component a_parentComponent,  String a_message, int a_logType,
									   Throwable a_throwable)
	{
		showErrorDialog(a_parentComponent, a_message, null, a_logType, a_throwable);
	}

	/**
	 * Displays a dialog showing an error message to the user and logs the error message
	 * to the currently used Log.
	 * @param a_parentDialog The parent dialog for this dialog. If it is null or the parent
	 *                       dialog is not within a frame, the dialog's parent frame is the
	 *                       default frame.
	 * @param a_throwable a Throwable that has been caught (may be null)
	 * @param a_title a title for the error message (may be null)
	 * @param a_message a message that is shown to the user (may be null)
	 * @param a_logType the log type for this error
	 * @see logging.LogHolder
	 * @see logging.LogType
	 * @see logging.Log
	 */
	public static void showErrorDialog(JAPDialog a_parentDialog, String a_message, String a_title,
									   int a_logType, Throwable a_throwable)
	{
		showErrorDialog(getInternalDialog(a_parentDialog), a_message, a_title, a_logType, a_throwable);
	}

	/**
	 * Displays a dialog showing an error message to the user and logs the error message
	 * to the currently used Log.
	 * @param a_parentComponent The parent component for this dialog. If it is null or the parent
	 *                          component is not within a frame, the dialog's parent frame is the
	 *                          default frame.
	 * @param a_title a title for the error message (may be null)
	 * @param a_message a message that is shown to the user (may be null)
	 * @param a_logType the log type for this error
	 * @param a_throwable a Throwable that has been caught (may be null)
	 * @see logging.LogHolder
	 * @see logging.LogType
	 * @see logging.Log
	 */
	public static void showErrorDialog(Component a_parentComponent, String a_message, String a_title,
									   int a_logType, Throwable a_throwable)
	{
		showErrorDialog(a_parentComponent, a_message, a_title, a_logType, a_throwable, null);
	}


	/**
	 * Displays a dialog showing an error message to the user and logs the error message
	 * to the currently used Log.
	 * @param a_parentComponent The parent component for this dialog. If it is null or the parent
	 *                          component is not within a frame, the dialog's parent frame is the
	 *                          default frame.
	 * @param a_title a title for the error message (may be null)
	 * @param a_message a message that is shown to the user (may be null)
	 * @param a_logType the log type for this error
	 * @param a_throwable a Throwable that has been caught (may be null)
	 * @param a_linkedInformation a clickable information message that is appended to the text
	 * @see logging.LogHolder
	 * @see logging.LogType
	 * @see logging.Log
	 */
	public static void showErrorDialog(Component a_parentComponent, String a_message, String a_title,
									   int a_logType, Throwable a_throwable,
									   ILinkedInformation a_linkedInformation)
	{
		boolean bPossibleApplicationError = false;

		a_message = retrieveErrorMessage(a_message, a_throwable);
		if (a_message == null)
		{
			a_message = JAPMessages.getString(MSG_ERROR_UNKNOWN);
			bPossibleApplicationError = true;
		}
		if (!LogType.isValidLogType(a_logType))
		{
			a_logType = LogType.GUI;
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

		try
		{
			if (a_title == null)
			{
				a_title = JAPMessages.getString(MSG_TITLE_ERROR);
			}
			showConfirmDialog(a_parentComponent, a_message, a_title,
							 OPTION_TYPE_DEFAULT, MESSAGE_TYPE_ERROR, null, a_linkedInformation);
		}
		catch (Exception e)
		{
			LogHolder.log(LogLevel.EXCEPTION, LogType.GUI, JAPMessages.getString(MSG_ERROR_UNDISPLAYABLE));
			LogHolder.log(LogLevel.EXCEPTION, LogType.GUI, e);
		}
	}

	/**
	 * Retrieves an error message from a Throwable and a message String that may be shown to the
	 * user. By default, this is the given message. If no message is given, it is tried to get the error
	 * message from the Throwable. A log message for the error is written automatically.
	 * @param a_throwable a Throwable (may be null)
	 * @param a_message an error message (may be null)
	 * @return the retrieved error message or null if no error message could be found; this would
	 * indicate a serious application error (an empty String as error message will result in a return
	 * value of null, too)
	 */
	public static String retrieveErrorMessage(String a_message, Throwable a_throwable)
	{
		if (a_message == null || a_message.trim().length() == 0)
		{
			if (a_throwable == null || a_throwable.getMessage() == null)
			{
				a_message = null;
			}
			else
			{
				a_message = a_throwable.getMessage();
				if (a_message == null || a_message.trim().length() == 0)
				{
					a_message = null;
				}
			}
		}

		return a_message;
	}

	/**
	 * Returns the glass pane.
	 * @return the glass pane
	 */
	public Component getGlassPane()
	{
		return m_internalDialog.getGlassPane();
	}

	/**
	 * Returns the JLayeredPane.
	 * @return the JLayeredPane
	 */
	public JLayeredPane getLayeredPane()
	{
		return m_internalDialog.getLayeredPane();
	}

	/**
	 * Returns the root pane of the dialog.
	 * @return the root pane of the dialog
	 */
	public JRootPane getRootPane()
	{
		return m_internalDialog.getRootPane();
	}

	/**
	 * Returns the content pane that can be used to place elements on the dialog.
	 * @return the dialog's content pane
	 */
	public final Container getContentPane()
	{
		return m_internalDialog.getContentPane();
	}

	/**
	 * Sets a new content pane for this dialog.
	 * @param a_contentPane a new content pane for this dialog
	 */
	public void setContentPane(Container a_contentPane)
	{
		m_internalDialog.setContentPane(a_contentPane);
	}

	/**
	 * Sets a new glass pane for this dialog.
	 * @param a_glassPane a new glass pane for this dialog
	 */
	public void setGlassPane(Component a_glassPane)
	{
		m_internalDialog.setGlassPane(a_glassPane);
	}

	/**
	 * Sets a new JLayeredPane for this dialog.
	 * @param a_layeredPane a new JLayeredPane for this dialog
	 */
	public void setLayeredPane(JLayeredPane a_layeredPane)
	{
		m_internalDialog.setLayeredPane(a_layeredPane);
	}

	/**
	 * Returns the parent Component.
	 * @return the parent Component
	 */
	public final Component getParentComponent()
	{
		return m_parentComponent;
	}

	/**
	 * Returns the parent Window. The parent Window may be but does not need to be the same than the
	 * parent Component.
	 * @return the parent Window
	 */
	public final Window getOwner()
	{
		return m_parentWindow;
	}

	/**
	 * Sets if the default click on the <i>Cancel</i> button of a dialog content
	 * pane is caught by the WindowClosing-Event of this dialog
	 * @param a_bCatchCancel if the default click on the <i>Cancel</i> button of a dialog content
	 * pane is caught by the WindowClosing-Event of this dialog
	 */
	public void doClosingOnContentPaneCancel(boolean a_bCatchCancel)
	{
		m_bCatchCancel = a_bCatchCancel;
	}

	/**
	 * Returns if the default click on the <i>Cancel</i> button of a dialog content
	 * pane is caught by the WindowClosing-Event of this dialog.
	 * @return if the default click on the <i>Cancel</i> button of a dialog content
	 * pane is caught by the WindowClosing-Event of this dialog
	 */
	public boolean isClosingOnContentPaneCancel()
	{
		return m_bCatchCancel;
	}

	public void setName(String a_name)
	{
		m_internalDialog.setName(a_name);
	}

	public String getName()
	{
		return m_internalDialog.getName();
	}


	public void setAlwaysOnTop(boolean a_bOnTop)
	{
		//GUIUtils.setAlwaysOnTop(m_internalDialog, true);
		if (!isVisible())
		{
			m_bOnTop = a_bOnTop;
		}
	}

	/**
	 * Returns if the dialog is visible on the screen.
	 * @return true if the dialog is visible on the screen; false otherwise
	 */
	public boolean isVisible()
	{
		return m_internalDialog.isVisible();
	}

	/**
	 * Shows or hides the dialog. If shown, the dialog is centered over the parent component.
	 * Subclasses may override this method to change this centering behaviour.
	 * @param a_bVisible 'true' shows the dialog; 'false' hides it
	 */
	public void setVisible(boolean a_bVisible)
	{
		setVisible(a_bVisible, true);
	}

	/**
	 * Shows or hides the dialog.
	 * @param a_bVisible 'true' shows the dialog; 'false' hides it
	 * @param a_bCenterOnParentComponent if true, the dialog is centered on the parent component;
	 * otherwise, it is positioned right under the parent window (owner window)
	 */
	public final void setVisible(boolean a_bVisible, boolean a_bCenterOnParentComponent)
	{
		if (isDisposed())
		{
			return;
		}

		if (a_bVisible)
		{
			if (!m_bLocationSetManually && !isVisible())
			{
				if (a_bCenterOnParentComponent)
				{
					GUIUtils.setLocationRelativeTo(getParentComponent(), m_internalDialog);
				}
				else
				{
					GUIUtils.positionRightUnderWindow(m_internalDialog, getOwner());
				}
			}

		}

		String dialogName = m_internalDialog.getName();
		String newName = null;
		boolean bOwnerOnTop = GUIUtils.isAlwaysOnTop(getOwner());

		if (m_bOnTop || bOwnerOnTop)
		{
			GUIUtils.setAlwaysOnTop(getOwner(), false);
			getOwner().toBack(); // fix for KDE, as otherwise the dialog would be below the window
			newName = getOwner().getName();
			m_internalDialog.setName(newName);
			GUIUtils.setAlwaysOnTop(m_internalDialog, true);
		}

		setVisibleInternal(a_bVisible);

		if (m_bOnTop || bOwnerOnTop)
		{
			String tempName = m_internalDialog.getName();
			m_internalDialog.setName("JAP " + Double.toString(Math.random()));
			GUIUtils.setAlwaysOnTop(getOwner(), bOwnerOnTop);
			GUIUtils.setAlwaysOnTop(m_internalDialog, false);
			if (dialogName != null && newName != null && tempName != null && tempName.equals(newName))
			{
				// set the original name if the current name has not changed
				m_internalDialog.setName(dialogName);
			}
		}
	}

	/**
	 * Sets the title of this dialog.
	 * @param a_title the title of this dialog
	 */
	public void setTitle(String a_title)
	{
		m_internalDialog.setTitle(a_title);
	}

	public String getTitle()
	{
		return m_internalDialog.getTitle();
	}

	/**
	 * Sets the menubar for this dialog.
	 * @param a_menubar the menubar being placed in the dialog
	 */
	public void setJMenuBar(JMenuBar a_menubar)
	{
		m_internalDialog.setJMenuBar(a_menubar);
	}

	/**
	 * Returns the menubar set on this dialog.
	 * @return the menubar set on this dialog
	 */
	public JMenuBar getJMenuBar()
	{
		return m_internalDialog.getJMenuBar();
	}

	/**
	 * If this Window is visible, brings this Window to the front and may make it the focused Window.
	 */
	public void toFront()
	{
		m_internalDialog.toFront();
	}

	/**
	 * If this Window is visible, sends this Window to the back and may cause it to lose focus or
	 * activation if it is the focused or active Window.
	 */
	public void toBack()
	{
		m_internalDialog.toBack();
	}

	/**
	 * Defines the dialog as modal or not.
	 * @param a_bModal true if the dialog should be modal; false otherwise
	 */
	public final void setModal(boolean a_bModal)
	{
		if (m_bForceApplicationModality)
		{
			m_bModal = false;
			m_internalDialog.setModal(a_bModal);
		}
		else
		{
			if (!isVisible())
			{
				m_bModal = a_bModal;
			}
			// the internal dialog is always non-modal
		}
	}

	/**
	 * Returns if the dialog is modal.
	 * @return if the dialog is modal
	 */
	public boolean isModal()
	{
		if (m_bForceApplicationModality)
		{
			return m_internalDialog.isModal();
		}

		return m_bModal;
	}

	public boolean isEnabled()
	{
		return m_internalDialog.isEnabled();
	}

	/**
	 * Returns if the dialog is resizable by the user.
	 * @return if the dialog is resizable by the user
	 */
	public boolean isResizable()
	{
		return m_internalDialog.isResizable();
	}

	public boolean isDisposed()
	{
		return m_bDisposed;
	}

	/**
	 * Disposes the dialog (set it to invisible and releases all resources).
	 * @todo Causes a Thread deadlock or throws an exception if called from Threads other than the main
	 * Thread or the AWT Event Thread. Removing the setVisible(false) would solve this problem, but causes a
	 * java.lang.IllegalMonitorStateException with JDK 1.2.2.
	 * If Threads are startet with gui.dialog.WorkerContentPane, everything is OK, so don't worry.
	 */
	public final void dispose()
	{
		m_bDisposed = true;

		if (m_bBlockParentWindow)
		{
			m_bBlockParentWindow = false;
			m_parentWindow.setEnabled(true);
			if (m_parentWindow.isVisible())
			{
				m_parentWindow.setVisible(true);
			}
		}

		m_internalDialog.setVisible(false);
		m_internalDialog.dispose();

		synchronized (SYNC_DOCK)
		{
			if (m_docker != null)
			{
				m_docker.finalize();
				m_docker = null;
			}
		}

		synchronized (m_internalDialog.getTreeLock())
		{
			Enumeration listeners = m_windowListeners.elements();

			while (listeners.hasMoreElements())
			{
				final WindowListener currrentListener = (WindowListener)listeners.nextElement();
				// do this trick to bypass deadlocks
				Thread run = new Thread(new Runnable()
				{
					public void run()
					{
						SwingUtilities.invokeLater(new Runnable()
						{
							public void run()
							{
								currrentListener.windowClosed(
									new WindowEvent(m_internalDialog, WindowEvent.WINDOW_CLOSED));
							}
						});
					}
				});
				run.setDaemon(true);
				run.start();
			}
			m_windowListeners.removeAllElements();

			listeners = ((Vector)m_componentListeners.clone()).elements();
			while (listeners.hasMoreElements())
			{
				removeComponentListener((ComponentListener)listeners.nextElement());
			}
			m_componentListeners.removeAllElements();

			m_internalDialog.removeWindowListener(m_dialogWindowAdapter);
			m_dialogWindowAdapter = null;
			m_internalDialog.getContentPane().removeAll();
			m_internalDialog.getRootPane().removeAll();
			m_internalDialog.getLayeredPane().removeAll();
			m_internalDialog.getTreeLock().notifyAll();
		}
	}

	/**
	 * Validates the dialog. Should be called after changing the content pane when the dialog is visible.
	 */
	public void validate()
	{
		m_internalDialog.validate();
	}

	/**
	 * Try to get the focus.
	 */
	public void requestFocus()
	{
		m_internalDialog.requestFocus();
	}

	/**
	 * Returns the size of the dialog window.
	 * @return the size of the dialog window
	 */
	public final Dimension getSize()
	{
		return m_internalDialog.getSize();
	}

	/**
	 * Returns the preferred size of the dialog window.
	 * @return the preferred size of the dialog window
	 */
	public final Dimension getPreferredSize()
	{
		return m_internalDialog.getPreferredSize();
	}


	/**
	 * Sets the size of the dialog window.
	 * @param a_width the new window width
	 * @param a_height the new window height
	 */
	public final void setSize(int a_width, int a_height)
	{
			m_internalDialog.setSize(a_width, a_height);
	}

	/**
	 * Sets the location of the dialog 'manually'. After that, no automatic alignment is done by this dialog.
	 * @param a_location a Point on the screen
	 */
	public final void setLocation(Point a_location)
	{
		m_bLocationSetManually = true;
		m_internalDialog.setLocation(a_location);
	}

	/**
	 * The dialog is centered on the given Component.
	 * Sets the location of the dialog 'manually'. After that, no automatic alignment is done by this dialog.
	 * @param a_component a Component
	 */
	public final void setLocationCenteredOn(Component a_component)
	{
		m_bLocationSetManually = true;
		GUIUtils.setLocationRelativeTo(a_component, m_internalDialog);
	}

	/**
	 * The dialog is centered on the parent Component.
	 * Sets the location of the dialog 'manually'. After that, no automatic alignment is done by this dialog.
	 */
	public final void setLocationCenteredOnParent()
	{
		m_bLocationSetManually = true;
		GUIUtils.setLocationRelativeTo(getParentComponent(), m_internalDialog);
	}

	/**
	 * Centers the dialog on the parent window, that means either the parent, if it is a window,
	 * or the the first window that contains the parent.
	 * Sets the location of the dialog 'manually'. After that, no automatic alignment is done by this dialog.
	 */
	public final void setLocationCenteredOnOwner()
	{
		setLocationCenteredOn(m_parentWindow);
	}

	/**
	 * Returns the bounds of the screen where this dialog is shown.
	 * @return the bounds of the screen where this dialog is shown
	 */
	public final Rectangle getScreenBounds()
	{
		return GUIUtils.getCurrentScreen(m_internalDialog).getBounds();
	}

	public void setDockable(boolean a_bDockable)
	{
		synchronized (SYNC_DOCK)
		{
			if (m_docker == null && a_bDockable)
			{
				m_docker = new GUIUtils.WindowDocker(m_internalDialog);
			}
			else if (m_docker != null && !a_bDockable)
			{
				m_docker.finalize();
				m_docker = null;
			}
		}
	}

	/**
	 * After using a method that sets the location of the dialog, it will not automatically set
	 * its location any more. This may be reset by calling this method.
	 * @param a_bDoNotSetLocationAutomatically if the dialog should not set its location automatically
	 */
	public void resetAutomaticLocation(boolean a_bDoNotSetLocationAutomatically)
	{
		m_bLocationSetManually = a_bDoNotSetLocationAutomatically;
	}

	/**
	 * Centers this dialog relative to the screen.
	 * Sets the location of the dialog 'manually'. After that, no automatic alignment is done by this dialog.
	 */
	public final void setLocationCenteredOnScreen()
	{
		m_bLocationSetManually = true;
		GUIUtils.centerOnScreen(m_internalDialog);
	}

	/**
	 * The dialog is positioned right under the owner window.
	 * Sets the location of the dialog 'manually'. After that, no automatic alignment is done by this dialog.
	 */
	public final void setLocationRelativeToOwner()
	{
		m_bLocationSetManually = true;
		GUIUtils.positionRightUnderWindow(m_internalDialog, getOwner());
	}

	/**
	 * Sets a window to the specified position and tries to put the window inside the screen by altering
	 * the position if needed.
	 * @param a_point a Point; may be null
	 */
	public void restoreLocation(Point a_point)
	{
		if (GUIUtils.restoreLocation(m_internalDialog, a_point))
		{
			m_bLocationSetManually = true;
		}
	}

	/**
	 * Sets a window to the specified size and tries to put the window inside the screen by altering
	 * the size if needed. The location of the window should be set before.
	 * @param a_size a Dimension; may be null
	 */
	public void restoreSize(Dimension a_size)
	{
		GUIUtils.restoreSize(m_internalDialog, a_size);
	}

	public void moveToUpRightCorner()
	{
		GUIUtils.moveToUpRightCorner(m_internalDialog);
	}

	/**
	 * Sets the location of the dialog 'manually'. After that,
	 * no automatic alignment is done by this dialog.
	 * @param x a x cooredinate on the screen
	 * @param y a y cooredinate on the screen
	 */
	public final void setLocation(int x, int y)
	{
		m_bLocationSetManually = true;
		m_internalDialog.setLocation(x, y);
	}

	/**
	 * Sets the size of the dialog window.
	 * @param a_size the new size of the dialog window
	 */
	public final void setSize(Dimension a_size)
	{
		m_internalDialog.setSize(a_size);
	}

	/**
	 * Allows to set the dialog resizable or fixed-sized.
	 * @param a_bResizable true if the dialog should become resizable; false otherwise
	 */
	public void setResizable(boolean a_bResizable)
	{
		m_internalDialog.setResizable(a_bResizable);
	}

	/**
	 * Returns the dialog's location on the screen.
	 * @return the dialog's location on the screen
	 */
	public final Point getLocation()
	{
		return m_internalDialog.getLocation();
	}

	public boolean imageUpdate(Image a_image, int a_infoflags, int a_x, int a_y, int a_width, int a_height)
	{
		return m_internalDialog.imageUpdate(a_image, a_infoflags, a_x, a_y, a_width, a_height);
	}

	/**
	 * Returns the AccessibleContext associated with this dialog
	 * @return the AccessibleContext associated with this dialog
	 */
	public final AccessibleContext getAccessibleContext()
	{
		return m_internalDialog.getAccessibleContext();
	}

	public Font getFont()
	{
		return m_internalDialog.getFont();
	}

	public void remove(MenuComponent a_component)
	{
		m_internalDialog.remove(a_component);
	}

	/**
	 * This method is not needed and only implemented to fulfill interface requirements.
	 * @param a_event an Event
	 * @return if the event has been dispatched successfully
	 * @deprecated As of JDK version 1.1 replaced by dispatchEvent(AWTEvent).
	 */
	public boolean postEvent(Event a_event)
	{
		return m_internalDialog.postEvent(a_event);
	}

	/**
	 * Defines the reaction of this dialog on a click on the close button in the dialog's title bar.
	 * @param a_windowAction insert an element of javax.swing.WindowConstants
	 * @see javax.swing.WindowConstants
	 */
	public final void setDefaultCloseOperation(int a_windowAction)
	{
		m_defaultCloseOperation = a_windowAction;
	}

	/**
	 * Returns the reaction of this dialog on a click on the close button in the dialog's title bar.
	 * @return a javax.swing.WindowConstant
	 * @see javax.swing.WindowConstants
	 */
	public final int getDefaultCloseOperation()
	{
		return m_defaultCloseOperation;
	}

	/**
	 * Adds a WindowListener to the dialog.
	 * @param a_listener a WindowListener
	 * @see java.awt.event.WindowListener
	 */
	public final void addWindowListener(WindowListener a_listener)
	{
		if (a_listener != null)
		{
			synchronized (m_internalDialog.getTreeLock())
			{
				m_windowListeners.addElement(a_listener);
			}
		}
	}

	/**
	 * Adds a Componentistener to the dialog.
	 * @param a_listener a ComponentListener
	 * @see java.awt.event.ComponentListener
	 */
	public final void addComponentListener(ComponentListener a_listener)
	{
		synchronized (m_internalDialog.getTreeLock())
		{
			if (a_listener != null && !m_componentListeners.contains(a_listener))
			{
				m_componentListeners.addElement(a_listener);
				m_internalDialog.addComponentListener(a_listener);
			}
		}
	}

	/**
	 * Removes a specific ComponentListener from the dialog.
	 * @param a_listener a ComponentListener
	 * @see java.awt.event.ComponentListener
	 */
	public final void removeComponentListener(ComponentListener a_listener)
	{
		synchronized (m_internalDialog.getTreeLock())
		{
			m_componentListeners.removeElement(a_listener);
			m_internalDialog.removeComponentListener(a_listener);
		}
	}

	/**
	 * Removes a specific WindowListener from the dialog.
	 * @param a_listener a WindowListener
	 * @see java.awt.event.WindowListener
	 */
	public final void removeWindowListener(WindowListener a_listener)
	{
		synchronized (m_internalDialog.getTreeLock())
		{
			m_windowListeners.removeElement(a_listener);
		}
	}

	/**
	 * Sets the dialog to the optimal size.
	 */
	public final void pack()
	{
		m_internalDialog.pack();
	}

	public Insets getInsets()
	{
		return m_internalDialog.getInsets();
	}

	void doWindowClosing()
	{
		m_dialogWindowAdapter.windowClosing(new WindowEvent(m_internalDialog, WindowEvent.WINDOW_CLOSING));
	}

	/**
	 * Catches all window events and informs the window listeners about them.
	 */
	private class DialogWindowAdapter implements WindowListener
	{
		public void windowOpened(WindowEvent a_event)
		{
			Vector listeners = (Vector) m_windowListeners.clone();
			{
				for (int i = 0; i < listeners.size(); i++)
				{
					( (WindowListener) listeners.elementAt(i)).windowOpened(a_event);
				}
			}
		}
		public void windowIconified(WindowEvent a_event)
		{
			Vector listeners = (Vector) m_windowListeners.clone();
			{
				for (int i = 0; i < listeners.size(); i++)
				{
					( (WindowListener) listeners.elementAt(i)).windowIconified(a_event);
				}
			}

		}
		public void windowDeiconified(WindowEvent a_event)
		{
			Vector listeners = (Vector) m_windowListeners.clone();
			{
				for (int i = 0; i < listeners.size(); i++)
				{
					( (WindowListener) listeners.elementAt(i)).windowDeiconified(a_event);
				}
			}
		}
		public void windowDeactivated(WindowEvent a_event)
		{
			Vector listeners = (Vector)m_windowListeners.clone();
			{
				for (int i = 0; i < listeners.size(); i++)
				{
					( (WindowListener) listeners.elementAt(i)).windowDeactivated(a_event);
				}
			}
		}
		public void windowActivated(WindowEvent a_event)
		{
			Vector listeners = (Vector) m_windowListeners.clone();
			{
				for (int i = 0; i < listeners.size(); i++)
				{
					( (WindowListener) listeners.elementAt(i)).windowActivated(a_event);
				}
			}
		}


		public void windowClosed(WindowEvent a_event)
		{
			/* Does not work... Do this in the dispose method!
			 Vector listeners = (Vector)m_windowListeners.clone();
			 {
			 for (int i = 0; i < listeners.size(); i++)
			 {
			  ( (WindowListener) listeners.elementAt(i)).windowClosed(a_event);
			 }
			 }*/
		}

		/**
		 * @todo There is a problem on Linux systems: although a window/dialog is disabled, its window buttons
		 * are not. If the user clicks on the close button of the parent window, the child dialog may be
		 * closed, for example, ignoring which behaviour is set for it by default.
		 * For JAPDialogs as parent, the problem is solved by first checking if it is enabled and then
		 * processing the windowClosing event and informing its listeners. Therefore it is recommended
		 * only to use JAPDialogs as parent, and, if it is really needed to use as parent a JFrame, a JDialog
		 * or an other window, to check for it if it is enabled before performing the windowClosing operation.
		 * This may be already fixed?
		 * @param a_event WindowEvent
		 */
		public void windowClosing(WindowEvent a_event)
		{
			if (isEnabled())
			{
				if (getDefaultCloseOperation() == DISPOSE_ON_CLOSE)
				{
					try
					{
						dispose();
					}
					catch (IllegalMonitorStateException a_e)
					{
						LogHolder.log(LogLevel.DEBUG, LogType.GUI, a_e);
					}
				}
				else if (getDefaultCloseOperation() == HIDE_ON_CLOSE)
				{
					setVisible(false);
				}
				else
				{
					/*
					 * This covers the case that, in old JDKs, a click on the close icon will always close
					 * the dialog, not regarding which closing pocily has been set. As it is not possible to
					 * catch this event if we do not own the AWT event thread in the setVisible() method,
					 * we have to make the internal dialog visible again in this place.
					 * In never JDKs >= 1.3 all WindowListeners are removed from the internal dialog, so this
					 * problem does not come up there.
					 * Notice: This bug causes a little flickering, but this should not harm.
					 */
					if (!isVisible())
					{
						m_internalDialog.setVisible(true);
						LogHolder.log(LogLevel.INFO, LogType.GUI, "Fixed old JRE dialog closing bug.");
					}
				}

				Vector listeners = (Vector) m_windowListeners.clone();
				{
					for (int i = 0; i < listeners.size(); i++)
					{
						( (WindowListener) listeners.elementAt(i)).windowClosing(a_event);
					}
				}
			}
		}
	}

	private static class SimpleDialogButtonFocusWindowAdapter extends WindowAdapter
	{
		private DialogContentPane m_contentPane;

		public SimpleDialogButtonFocusWindowAdapter(DialogContentPane a_contentPane)
		{
			m_contentPane = a_contentPane;
		}

		public void windowOpened(WindowEvent a_event)
		{
			if (m_contentPane.getButtonCancel() != null)
			{
				m_contentPane.getButtonCancel().requestFocus();
			}
			else if (m_contentPane.getButtonNo() != null)
			{
				m_contentPane.getButtonNo().requestFocus();
			}
			else if (m_contentPane.getButtonYesOK() != null)
			{
				m_contentPane.getButtonYesOK().requestFocus();
			}
			else if (m_contentPane.getButtonHelp() != null)
			{
				m_contentPane.getButtonHelp().requestFocus();
			}
		}
	}


	/**
	 * Activates a LinkedInformation, if it is given as a link.
	 */
	private static class LinkedInformationClickListener extends MouseAdapter implements ItemListener
	{
		private ILinkedInformation m_linkedInformation;

		public LinkedInformationClickListener(ILinkedInformation a_linkedInformation)
		{
			m_linkedInformation = a_linkedInformation;
		}

		public void mouseClicked(MouseEvent a_event)
		{
			m_linkedInformation.clicked(false);
		}

		public void itemStateChanged(ItemEvent a_event)
		{
			m_linkedInformation.clicked(((JCheckBox)a_event.getSource()).isSelected());
		}
	}

	private static class PreferredWidthBoxPanel extends JPanel
	{
		private int m_preferredWidth;
		private int m_preferredHeigth;

		public PreferredWidthBoxPanel()
		{
			BoxLayout layout;
			m_preferredWidth = 0;
			m_preferredHeigth = 0;
			layout = new BoxLayout(this, BoxLayout.Y_AXIS);
			setLayout(layout);
		}
		public void setPreferredWidth(int a_preferredWidth)
		{
			m_preferredHeigth = 0;
			m_preferredWidth = a_preferredWidth;
		}

		public void setPreferredHeigth(int a_preferredHeigth)
		{
			m_preferredHeigth = a_preferredHeigth;
			m_preferredWidth = 0;

		}

		public Dimension getPreferredSize()
		{
			if (m_preferredWidth <= 0 && m_preferredHeigth <= 0)
			{
				return super.getPreferredSize();
			}
			else if (m_preferredWidth > 0)
			{
				return new Dimension(m_preferredWidth, super.getPreferredSize().height);
			}
			else
			{
				return new Dimension(super.getPreferredSize().width, m_preferredHeigth);
			}
		}
	}

	/**
	 * Returns the internal dialog of a JAPDialog or null if there is none.
	 * @param a_dialog a JAPDialog
	 * @return the internal dialog of a JAPDialog or null if there is none
	 */
	private static Window getInternalDialog(JAPDialog a_dialog)
	{
		if (a_dialog == null)
		{
			return null;
		}

		return a_dialog.m_internalDialog;
	}

	/**
	 * Finds the first focusable Component in a Container and sets the focus on it.
	 * @param a_container a Container
	 * @return if a Component has been focused
	 */
	private static boolean requestFocusForFirstFocusableComponent(Container a_container)
	{
		// see if isFocusable() is available; then we do not need this patch
		try
		{
			Container.class.getMethod("isFocusable", null).invoke(a_container, null);
			return true;
		}
		catch (Exception a_e)
		{
		}

		for (int i = 0; i < a_container.getComponentCount(); i++)
		{
			if (a_container.getComponent(i) instanceof Container)
			{
				if (requestFocusForFirstFocusableComponent((Container)a_container.getComponent(i)))
				{
					return true;
				}
			}

			if (a_container.getComponent(i).isFocusTraversable())
			{
				a_container.getComponent(i).requestFocus();
				return true;
			}
		}
		return false;
	}

	/**
	 * Keeps the blocked window in a disabled state and transfers the focus from it to the dialog,
	 * even if some other part of the application set it to enabled or set the focus on it.
	 */
	private class BlockedWindowDeactivationAdapter extends WindowAdapter implements FocusListener
	{
		public void windowActivated(WindowEvent a_event)
		{
			deactivate(a_event.getWindow());
		}

		public void focusGained(FocusEvent a_event)
		{
			deactivate((Window)a_event.getComponent());
		}

		public void focusLost(FocusEvent a_event)
		{
		}

		private void deactivate(Window a_window)
		{
			if (m_bBlockParentWindow)
			{
				//requestFocus(); // this would delete a focus that has been set before...
				toFront();
				if (a_window.isEnabled())
				{
					a_window.setEnabled(false);
				}
			}
		}
	}

	/**
	 * JAPDialog's main logic.
	 * @param a_bVisible true if the dialog is shown; false if it is hidden
	 */
	private void setVisibleInternal(boolean a_bVisible)
	{
		if (isVisible() && m_bBlockParentWindow && !a_bVisible)
		{
			m_parentWindow.setEnabled(true);
			if (m_parentWindow.isVisible())
			{
				// this is a bugfix
				m_parentWindow.setVisible(true);
			}
		}

		m_bBlockParentWindow = (a_bVisible && m_bModal);
		if (m_bBlockParentWindow)
		{
			// must be set disabled before showing the dialog
			m_parentWindow.setEnabled(false);
		}

		if (m_bForceApplicationModality)
		{
			m_internalDialog.setVisible(a_bVisible);
			return;
		}

		synchronized (m_internalDialog.getTreeLock())
		{
			m_internalDialog.setVisible(a_bVisible);
			if (a_bVisible)
			{
				if (getContentPane() != null && getContentPane().isVisible())
				{
					// needed for DialogContentPane's component listener (componentShown event is fired)
					getContentPane().setVisible(false);
					getContentPane().setVisible(true);
				}
				// this is needed to put the window to front in some cases, e.g. when closing JAP and not
				// all accounts are saved
				m_internalDialog.toFront(); /** @todo Check if this works and does not block anything! */
			}
			m_internalDialog.getTreeLock().notifyAll();
		}
		if (a_bVisible)
		{
			// fix for old JDKs, e.g. 1.1.8, that do not auto-focus the first focusable component
			requestFocusForFirstFocusableComponent(m_internalDialog.getContentPane()); // no TreeLock!
		}

		if (m_bBlockParentWindow)
		{
			Runnable dialogThread = new Runnable()
			{
				public void run()
				{
					try
					{
						BlockedWindowDeactivationAdapter windowDeactivationAdapter =
							new BlockedWindowDeactivationAdapter();

						m_parentWindow.addWindowListener(windowDeactivationAdapter);
						m_parentWindow.addFocusListener(windowDeactivationAdapter);

						if (SwingUtilities.isEventDispatchThread())
						{
							EventQueue theQueue = m_internalDialog.getToolkit().getSystemEventQueue();

							while (isVisible())
							{
								AWTEvent event = theQueue.getNextEvent();

								if (m_bBlockParentWindow && m_parentWindow.isEnabled())
								{
									// another dialog has enabled the parent; set it back to disabled
									m_parentWindow.setEnabled(false);
								}

								Class classActiveEvent;
								try
								{
									// java.awt.ActiveEvent is not known in JDKs < 1.2
									classActiveEvent = Class.forName("java.awt.ActiveEvent");
								}
								catch (ClassNotFoundException a_e)
								{
									classActiveEvent = null;
								}
								Object src = event.getSource();
								if (src == m_internalDialog)
								{
									if (event instanceof WindowEvent)
									{
										if ( ( (WindowEvent) event).getID() == WindowEvent.WINDOW_CLOSING)
										{
											m_dialogWindowAdapter.windowClosing( (WindowEvent) event);

											/*
											 * Hide this event from the internal dialog. This removes the flimmering
											 * effect that occurs when the internal dialog is closed before enabling
											 * the parent window.
											 */
											continue;
										}
									}
									else if (event instanceof KeyEvent && getRootPane().getDefaultButton() != null)
									{
										/*
													  // default button patch for old JDKs
												  KeyEvent keyEvent = ((KeyEvent) event);
													  if (keyEvent.getKeyCode() == KeyEvent.VK_ENTER &&
													  //	(keyEvent.getID() == KeyEvent.KEY_RELEASED ||
										  keyEvent.getID() == KeyEvent.KEY_TYPED)
													  {
										 if (keyEvent.getID() == KeyEvent.KEY_TYPED)
										 {
										  getRootPane().getDefaultButton().doClick();
										 }
										 continue;
													  }
										 */
									}
								}

								if (classActiveEvent != null && classActiveEvent.isInstance(event))
								{
									// ((ActiveEvent) event).dispatch();
									classActiveEvent.getMethod("dispatch", null).invoke(event, null);
								}
								else if (src instanceof Component)
								{
									if (src == getParentComponent() && event instanceof WindowEvent &&
										( (WindowEvent) event).getID() == WindowEvent.WINDOW_CLOSING)
									{
										// Prevent closing of parent Window. This will otherwise work in KDE systems.
										continue;
									}

									try
									{
										( (Component) src).dispatchEvent(event);
									}
									catch (IllegalMonitorStateException a_e)
									{
										LogHolder.log(LogLevel.NOTICE, LogType.GUI, a_e);
									}
								}
								else if (src instanceof MenuComponent)
								{
									( (MenuComponent) src).dispatchEvent(event);
								}
							}
						}
						else
						{
							/** @todo remove... */
							/**
							 * Dialogs going in here are less secure against 'conflicting' components that enable
							 * the parent. These event are only handled by focusGained() and windowActivated().
							 */
							synchronized (m_internalDialog.getTreeLock())
							{
								while (isVisible())
								{
									try
									{
										m_internalDialog.getTreeLock().wait();
									}
									catch (InterruptedException e)
									{
										break;
									}
								}
								m_internalDialog.getTreeLock().notifyAll();
							}
						}
						m_parentWindow.removeWindowListener(windowDeactivationAdapter);
						m_parentWindow.removeFocusListener(windowDeactivationAdapter);
					}
					catch (Exception a_e)
					{
						LogHolder.log(LogLevel.EXCEPTION, LogType.GUI, a_e);
					}
				}
			};

			if (SwingUtilities.isEventDispatchThread())
			{
				dialogThread.run();
			}
			else
			{
				try
				{
					SwingUtilities.invokeAndWait(dialogThread);
				}
				catch (Exception a_e)
				{
					LogHolder.log(LogLevel.EXCEPTION, LogType.GUI, a_e);
				}
			}

			if (!m_parentWindow.isEnabled())
			{
				m_bBlockParentWindow = false;
				m_parentWindow.setEnabled(!ms_bConsoleOnly);
				if (m_parentWindow.isVisible())
				{
					// this is a bugfix
					m_parentWindow.setVisible(true);
				}
			}
			if (ms_bConsoleOnly)
			{
				m_parentWindow.setEnabled(false);
			}
		}

		synchronized (m_internalDialog.getTreeLock())
		{
			m_internalDialog.getTreeLock().notifyAll();
		}
	}
}
