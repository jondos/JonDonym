package gui.dialog;

import javax.swing.RootPaneContainer;

import gui.JAPHelpContext;

/**
 * Defines the buttons that are available in a dialog.
 */
public final class DialogContentPaneOptions
{
	private int m_optionType;
	private DialogContentPane m_previousContentPane;
	private JAPHelpContext.IHelpContext m_helpContext;

	/**
	 * Creates new button options.
	 * @param a_optionType one of the available option types the define the type and number of buttons
	 */
	public DialogContentPaneOptions(int a_optionType)
	{
		this(a_optionType, (JAPHelpContext.IHelpContext)null, null);
	}

	/**
	 * Creates new button options. No buttons are shown by default.
	 * @param a_strHelpContext a IHelpContext; if it returns an other help context value than null,
	 * a help button is shown that opens the context;
	 */
	public DialogContentPaneOptions(String a_strHelpContext)
	{
		this(IDialogOptions.OPTION_TYPE_EMPTY, a_strHelpContext, null);
	}

	/**
	 * Creates new button options. No buttons are shown by default.
	 * @param a_helpContext a IHelpContext; if it returns an other help context value than null,
	 * a help button is shown that opens the context;
	 */
	public DialogContentPaneOptions(JAPHelpContext.IHelpContext a_helpContext)
	{
		this(IDialogOptions.OPTION_TYPE_EMPTY, a_helpContext, null);
	}

	/**
	 * Creates new button options. No buttons are shown by default.
	 * @param a_previousContentPane A DialogContentPane that will be linked with this one; it gets this
	 * content pane as next content pane. Call moveToNextContentPane() and moveToPreviousContentPane() to
	 * move between the panes.
	 */
	public DialogContentPaneOptions(DialogContentPane a_previousContentPane)
	{
		this(IDialogOptions.OPTION_TYPE_EMPTY, (JAPHelpContext.IHelpContext)null, a_previousContentPane);
	}

	/**
	 * Creates new button options. No buttons are shown by default.
	 * @param a_strHelpContext a IHelpContext; if it returns an other help context value than null,
	 * a help button is shown that opens the context;
	 * @param a_previousContentPane A DialogContentPane that will be linked with this one; it gets this
	 * content pane as next content pane. Call moveToNextContentPane() and moveToPreviousContentPane() to
	 * move between the panes.
	 */
	public DialogContentPaneOptions(String a_strHelpContext, RootPaneContainer a_rootPaneContainer, DialogContentPane a_previousContentPane)
	{
		this(IDialogOptions.OPTION_TYPE_EMPTY, a_strHelpContext, a_rootPaneContainer, a_previousContentPane);
	}

	/**
	 * Creates new button options. No buttons are shown by default.
	 * @param a_helpContext a IHelpContext; if it returns an other help context value than null,
	 * a help button is shown that opens the context;
	 * @param a_previousContentPane A DialogContentPane that will be linked with this one; it gets this
	 * content pane as next content pane. Call moveToNextContentPane() and moveToPreviousContentPane() to
	 * move between the panes.
	 */
	public DialogContentPaneOptions(JAPHelpContext.IHelpContext a_helpContext, DialogContentPane a_previousContentPane)
	{
		this(IDialogOptions.OPTION_TYPE_EMPTY, a_helpContext, a_previousContentPane);
	}

	/**
	 * Creates new button options.
	 * @param a_optionType one of the available option types the define the type and number of buttons
	 * @param a_previousContentPane A DialogContentPane that will be linked with this one; it gets this
	 * content pane as next content pane. Call moveToNextContentPane() and moveToPreviousContentPane() to
	 * move between the panes.
	 */
	public DialogContentPaneOptions(int a_optionType, DialogContentPane a_previousContentPane)
	{
		this(a_optionType, (JAPHelpContext.IHelpContext)null, a_previousContentPane);
	}

	/**
	 * Creates new button options.
	 * @param a_optionType one of the available option types the define the type and number of buttons
	 * @param a_helpContext a IHelpContext; if it returns an other help context value than null,
	 * a help button is shown that opens the context;
	 */
	public DialogContentPaneOptions(int a_optionType, JAPHelpContext.IHelpContext a_helpContext)
	{
		this(a_optionType, a_helpContext, null);
	}

	/**
	 * Creates new button options.
	 * @param a_optionType one of the available option types the define the type and number of buttons
	 * @param a_strHelpContext a IHelpContext; if it returns an other help context value than null,
	 * a help button is shown that opens the context;
	 */
	public DialogContentPaneOptions(int a_optionType, String a_strHelpContext, RootPaneContainer a_rootPaneContainer)
	{
		this(a_optionType, a_strHelpContext, a_rootPaneContainer, null);
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
	public DialogContentPaneOptions(int a_optionType, final String a_strHelpContext, final RootPaneContainer a_rootPaneContainer, DialogContentPane a_previousContentPane)
	{
		this(a_optionType,
			 new JAPHelpContext.IHelpContext(){
				public String getHelpContext(){return a_strHelpContext;}
				public RootPaneContainer getDisplayContext(){return a_rootPaneContainer;}
				},
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
	public DialogContentPaneOptions(int a_optionType, JAPHelpContext.IHelpContext a_helpContext,
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

