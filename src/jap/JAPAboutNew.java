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

import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import javax.swing.JEditorPane;
import javax.swing.JScrollPane;

import anon.util.JAPMessages;
import anon.util.ResourceLoader;
import gui.JAPDll;
import gui.dialog.DialogContentPane;
import gui.dialog.DialogContentPaneOptions;
import gui.dialog.JAPDialog;
import javax.swing.event.HyperlinkEvent;
import java.net.URL;
import platform.AbstractOS;
import javax.swing.event.HyperlinkListener;


/**
 * Shows information about the development of JAP.
 *
 * @author Rolf Wendolsky
 */
public class JAPAboutNew extends JAPDialog /* implements HyperlinkListener */
{
	private static final String MSG_VERSION = JAPAboutNew.class.getName() + "_version";
	private static final String MSG_DLL_VERSION = JAPAboutNew.class.getName() + "_dllVersion";

	public JAPAboutNew(Component a_parent)
	{
		super(a_parent, JAPMessages.getString(MSG_VERSION) + ": " + JAPConstants.aktVersion +
			(JAPConstants.m_bReleasedVersion ? "" : "-dev") +
		  (JAPDll.getDllVersion() != null ? " (" +  JAPMessages.getString(MSG_DLL_VERSION) + ": " +
		   JAPDll.getDllVersion() + ")" :
		   ""));
		DialogContentPane contentPane =
			new DialogContentPane(this, (DialogContentPane.Layout)null, new DialogContentPaneOptions(DialogContentPane.OPTION_TYPE_DEFAULT));
		contentPane.setDefaultButtonOperation(DialogContentPane.ON_CLICK_DISPOSE_DIALOG);

		String htmlText = new String(ResourceLoader.loadResource(JAPMessages.getString("htmlfileAbout")));
		JScrollPane scrollPane;
		JEditorPane textArea = new JEditorPane();

		textArea = new JEditorPane();
		textArea.setEditable(false);
		textArea.addHyperlinkListener(new JAPHyperlinkAdapter());
		textArea.setDoubleBuffered(false);
		setResizable(false);
		textArea.setContentType("text/html");
		textArea.setText(htmlText.trim());
		scrollPane = new JScrollPane(textArea, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
									 JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

		contentPane.getContentPane().setLayout(new GridBagLayout());
		GridBagConstraints constraints = new GridBagConstraints();
		constraints.gridx = 0;
		constraints.gridy = 0;
		constraints.fill = GridBagConstraints.BOTH;
		constraints.weighty = 1;
		constraints.weightx = 1;
		contentPane.getContentPane().add(scrollPane, constraints);
		scrollPane.setPreferredSize(new Dimension(400, 300));

		contentPane.updateDialog();
		pack();
	}

	/*public void hyperlinkUpdate(HyperlinkEvent e)
{
	if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED )
	{
		URL urlToOpen = e.getURL();
		if (urlToOpen.getProtocol().startsWith("mailto") )
		{
			AbstractOS.getInstance().openEMail(urlToOpen.toString());
		}
		else
		{
			AbstractOS.getInstance().openURL(urlToOpen);
		}
	}
}*/

}
