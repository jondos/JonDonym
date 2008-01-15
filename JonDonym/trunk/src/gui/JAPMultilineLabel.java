/*
 Copyright (c) 2000, The JAP-Team
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

import java.awt.GridLayout;
import java.awt.Font;
import java.util.StringTokenizer;
import java.awt.Color;

import javax.swing.JLabel;
import javax.swing.JPanel;

/**
 * This class is needed for JDKs < 1.3 as they cannot recognize new lines "\n" in a JLabel.
 */
final public class JAPMultilineLabel extends JPanel
{
	private Font m_font;

	public JAPMultilineLabel(String a_strText)
	{
		this(a_strText, new JLabel().getFont());
	}

	public JAPMultilineLabel(String a_strText, Color a_color)
	{
		this(a_strText, new JLabel().getFont(), a_color);
	}


	public JAPMultilineLabel(Font a_font)
	{
		this("", a_font);
	}

	public JAPMultilineLabel(String a_strText, Font a_font)
	{
		this(a_strText, a_font, null);
	}

	public JAPMultilineLabel(String a_strText, Font a_font, Color a_color)
	{
		m_font = a_font;
		this.setText(a_strText, a_color);
	}


	public void setText(String a_strText, Color a_color)
	{
		JLabel label;
		StringTokenizer st;

		removeAll();
		setLayout(new GridLayout(0, 1, 0, 0));
		st = new StringTokenizer(a_strText, "\n");
		while (st.hasMoreElements())
		{
			label = new JLabel(st.nextToken());
			if (m_font != null)
			{
				label.setFont(m_font);
			}
			if (a_color != null)
			{
				label.setForeground(a_color);
			}
			add(label);
		}
}

	/**
	 * Sets the text of this label.
	 * @param a_strText the text of this label
	 */
	public void setText(String a_strText)
	{
		setText(a_strText, null);
	}
}
