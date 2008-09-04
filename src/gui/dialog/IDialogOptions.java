/*
 Copyright (c) 2000 - 2005, The JAP-Team
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

import javax.swing.JOptionPane;

/**
 * Provides constants for creating dialogs and for evaluating user choices in dialogs. The constants
 * correspond directly to JOptionPane and may be used there, too.
 * @see javax.swing.JOptionPane
 * @author Rolf Wendolsky
 */
public interface IDialogOptions
{
	public static final int MESSAGE_TYPE_PLAIN = JOptionPane.PLAIN_MESSAGE;
	public static final int MESSAGE_TYPE_QUESTION = JOptionPane.QUESTION_MESSAGE;
	public static final int MESSAGE_TYPE_ERROR = JOptionPane.ERROR_MESSAGE;
	public static final int MESSAGE_TYPE_WARNING = JOptionPane.WARNING_MESSAGE;
	public static final int MESSAGE_TYPE_INFORMATION = JOptionPane.INFORMATION_MESSAGE;
	public static final int OPTION_TYPE_DEFAULT = JOptionPane.DEFAULT_OPTION;
	public static final int OPTION_TYPE_OK_CANCEL = JOptionPane.OK_CANCEL_OPTION;
	public static final int OPTION_TYPE_YES_NO_CANCEL = JOptionPane.YES_NO_CANCEL_OPTION;
	public static final int OPTION_TYPE_YES_NO = JOptionPane.YES_NO_OPTION;
	/**
	 * This is an extra option type not available in JOptionPane. If set, no buttons are displayed.
	 */
	public static final int OPTION_TYPE_EMPTY = Integer.MIN_VALUE;
	/**
	 * This is an extra option type not available in JOptionPane. If set, only the cancel button is displayed.
	 */
	public static final int OPTION_TYPE_CANCEL = OPTION_TYPE_EMPTY + 1;
	public static final int RETURN_VALUE_CANCEL = JOptionPane.CANCEL_OPTION;
	public static final int RETURN_VALUE_OK = JOptionPane.OK_OPTION;
	public static final int RETURN_VALUE_CLOSED = JOptionPane.CLOSED_OPTION;
	public static final int RETURN_VALUE_YES = JOptionPane.YES_OPTION;
	public static final int RETURN_VALUE_NO = JOptionPane.NO_OPTION;
	/** This is an extra return value not available in JOptionPane. */
	public static final int RETURN_VALUE_UNINITIALIZED = Integer.MIN_VALUE;
}
