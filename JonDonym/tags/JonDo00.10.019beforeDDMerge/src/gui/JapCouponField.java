/*
 Copyright (c) 2000-2007, The JAP-Team
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

import javax.swing.JTextField;
import javax.swing.text.Document;
import javax.swing.text.PlainDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.JMenuItem;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.ActionEvent;
import javax.swing.JPopupMenu;
import java.awt.event.ActionListener;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.Transferable;

/**
 * specialized version of a JTextField, similar to JAPJIntField
 * differences to regular text field:
 * - accepts only hexadecimal values (0-9, A-F)
 * - accepts only 4 characters, transfers focus once full
 * - displays characters as uppercase regardless of whether they were entered upper- or lowercase
 *
 * @author Elmar Schraml
 *
 */
public class JapCouponField extends JTextField
{
	/**
	 * serial version UID
	 */
	private static final long serialVersionUID = 1L;
	
	private static final int NR_OF_CHARACTERS = 4;
	private static final char[] ACCEPTED_CHARS = {'0','1','2','3','4','5','6','7','8','9','A','B','C','D','E','F'}; //assumes text has already been made uppercase
	private JapCouponField m_nextCouponField;

	private static final String MSG_INSERT_FROM_CLIP = JapCouponField.class.getName() + "_insertFromClip";


	public JapCouponField()
	{
		super(NR_OF_CHARACTERS);

		final JPopupMenu m_popup = new JPopupMenu();
		JMenuItem itemInsertCoupon = new JMenuItem(JAPMessages.getString(MSG_INSERT_FROM_CLIP));

		MouseAdapter popupListener = new MouseAdapter()
		{
			public void mouseClicked(MouseEvent a_event)
			{
				if (GUIUtils.isMouseButton(a_event, MouseEvent.BUTTON2_MASK) ||
					GUIUtils.isMouseButton(a_event, MouseEvent.BUTTON3_MASK))
				{
					m_popup.show(JapCouponField.this, a_event.getX(), a_event.getY());
				}
			}
		};
		addMouseListener(popupListener);


		itemInsertCoupon.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent a_event)
			{
				Clipboard clip = GUIUtils.getSystemClipboard();
				Transferable data = clip.getContents(this);
				if (data != null && data.isDataFlavorSupported(DataFlavor.stringFlavor))
				{
					try
					{
						setText( (String) data.getTransferData(DataFlavor.stringFlavor));
					}
					catch (Exception a_e)
					{
						// ignore it
					}
				}
			}
		});
		m_popup.add(itemInsertCoupon);
	}

	public void setNextCouponField(JapCouponField a_nextCouponField)
	{
		m_nextCouponField = a_nextCouponField;
	}

	protected final Document createDefaultModel()
	{
		return new CouponDocument();
	}

	private final class CouponDocument extends PlainDocument
	{
		/**
		 * serial version UID
		 */
		private static final long serialVersionUID = 1L;

		public void insertString(int offset, String string, AttributeSet attributeSet) throws BadLocationException
		{
			//make everything uppercase
			string = string.toUpperCase();

			//remove chars that are not in ACCEPTED_CHARs
			char[] originalString = string.toCharArray();
			char[] modifiedString = new char[originalString.length];
			int nrOfOkayChars = 0;
			for (int i = 0; i < originalString.length ; i++)
			{
				if ( isCharacterAccepted(originalString[i]) )
				{
					modifiedString[nrOfOkayChars] = originalString[i];
					nrOfOkayChars++;
				}
				else {
					continue; //exclude the char from modifiedString, do not increase nrOfOkayChars to give its space to next accepted char
				}
			}
			string = new String(modifiedString,0,nrOfOkayChars);

			//prevent more than NR_OF_CHARACTERS to be entered
			/* throws exception, needs debugging
			 if (getLength()+string.length() > NR_OF_CHARACTERS)
			 {
				 String shortenedString = string.substring(0,NR_OF_CHARACTERS);
			  string = new String(shortenedString);
					  }
			 */
			if (string.length() + getLength() > NR_OF_CHARACTERS)
			{
				// copy and paste error...
				if (m_nextCouponField != null)
				{
					m_nextCouponField.setText(string.substring(NR_OF_CHARACTERS, string.length()));
				}
				string = string.substring(0, NR_OF_CHARACTERS);
				super.insertString(0, string, attributeSet);
			}
			else
			{
				//modifications done, set the string
				super.insertString(offset, string, attributeSet);
			}



			//move on after 4 characters have been entered
			if (getLength() >= NR_OF_CHARACTERS)
			{
				if (getLength() > NR_OF_CHARACTERS)
				{
					super.remove(NR_OF_CHARACTERS, getLength() - NR_OF_CHARACTERS);
				}
				transferFocus();
			}
		}

		private boolean isCharacterAccepted(char charToCheck)
		{
			for (int i = 0; i < ACCEPTED_CHARS.length ; i++)
			{
				if (charToCheck == ACCEPTED_CHARS[i] )
				{
					return true;
				}
			}
			return false;
		}

	}

}
