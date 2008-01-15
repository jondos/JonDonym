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

import gui.JAPMessages;

/**
 * This content pane could be used as the last content pane in a wizard. It may have a title and a
 * text that is displayed to the user, and, of course, a previous content pane.
 * <P> Default button operations:
 * ON_CANCEL_DISPOSE_DIALOG | ON_YESOK_DISPOSE_DIALOG | ON_NO_SHOW_PREVIOUS_CONTENT</P>
 *
 * @author Rolf Wendolsky
 */
public class FinishedContentPane extends DialogContentPane implements
	DialogContentPane.IWizardSuitable
{
	private static final String MSG_FINISHING = FinishedContentPane.class.getName() + "_finishing";


	public FinishedContentPane(JAPDialog a_parentDialog, String a_strText,
							   DialogContentPane a_previousContentPane)
	{
		this(a_parentDialog, a_strText, JAPMessages.getString(MSG_FINISHING), a_previousContentPane);
	}

	public FinishedContentPane(JAPDialog a_parentDialog, String a_strText, String a_strTitle,
							   DialogContentPane a_previousContentPane)
	{
		super(a_parentDialog, a_strText,
			  new DialogContentPane.Layout(a_strTitle, DialogContentPane.MESSAGE_TYPE_INFORMATION),
			  new DialogContentPane.Options(a_previousContentPane));
		setDefaultButtonOperation(ON_CANCEL_DISPOSE_DIALOG | ON_YESOK_DISPOSE_DIALOG |
								  ON_NO_SHOW_PREVIOUS_CONTENT);
	}

}
