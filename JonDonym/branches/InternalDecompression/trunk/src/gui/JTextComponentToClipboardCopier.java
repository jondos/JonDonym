/*
 Copyright (c) 2000-2006, The JAP-Team
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
package gui;

import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;
import javax.swing.text.JTextComponent;

import anon.util.JAPMessages;

/**
 * Adds a context menu to JTextComponents that is activated by a click on the right mouse
 * button that enabled the user to copy the text to clipboard.
 *
 * @author Rolf Wendolsky
 */
public class JTextComponentToClipboardCopier
{
	private static final String MSG_COPY_TO_CLIP =
		JTextComponentToClipboardCopier.class.getName() + "_copyToClip";
	private static final String MSG_COPY_SELECTED_TO_CLIP =
		JTextComponentToClipboardCopier.class.getName() + "_copySelectedToClip";

	private JPopupMenu m_popup;
	private JTextComponent m_currentPopup;
	private MouseAdapter m_popupListener;

	/**
	 * Constructor.
	 * @param a_bCopySelectedTextOnly if only the selected text should be copied to clipboard; otherwise,
	 * the complete text from the component is copied.
	 */
	public JTextComponentToClipboardCopier(final boolean a_bCopySelectedTextOnly)
	{
		m_popup = new JPopupMenu();
		m_popupListener = new MouseAdapter()
		{
			public void mouseClicked(MouseEvent a_event)
			{
				if (SwingUtilities.isRightMouseButton(a_event))
				{
					m_currentPopup = (JTextComponent) a_event.getComponent();
					m_popup.show(a_event.getComponent(), a_event.getX(), a_event.getY());
				}
			}
		};

		JMenuItem itemCopyPassword;
		if (a_bCopySelectedTextOnly)
		{
			itemCopyPassword = new JMenuItem(JAPMessages.getString(MSG_COPY_SELECTED_TO_CLIP));
		}
		else
		{
			itemCopyPassword = new JMenuItem(JAPMessages.getString(MSG_COPY_TO_CLIP));
		}
		itemCopyPassword.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent a_event)
			{
				Clipboard clip = GUIUtils.getSystemClipboard();
				String text;
				if (a_bCopySelectedTextOnly)
				{
					text = m_currentPopup.getSelectedText();
				}
				else
				{
					text = m_currentPopup.getText();
				}

				if (text != null)
				{
					clip.setContents(new StringSelection(text), new ClipboardOwner()
					{
						public void lostOwnership(Clipboard cb, Transferable co)
						{
							// Don't care.
						}
					});
				}
			}
		});
		m_popup.add(itemCopyPassword);
	}

	/**
	 * Adds a popup menu for copying the text to this text component.
	 * @param a_textComponent a JTextComponent
	 */
	public void registerTextComponent(JTextComponent a_textComponent)
	{
		if (a_textComponent != null)
		{
			a_textComponent.addMouseListener(m_popupListener);
		}
	}

	/**
	 * Removes a previously registered popup menu from this text component.
	 * @param a_textComponent a JTextComponent
	 */
	public void unregisterTextComponent(JTextComponent a_textComponent)
	{
		if (a_textComponent != null)
		{
			a_textComponent.removeMouseListener(m_popupListener);
		}
	}
}
