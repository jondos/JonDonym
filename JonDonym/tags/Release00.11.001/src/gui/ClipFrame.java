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

import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.Choice;
import java.awt.Component;
import java.awt.TextArea;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import gui.dialog.JAPDialog;
import logging.*;

public class ClipFrame extends JAPDialog implements ActionListener, ItemListener
{
	private TextArea m_TextArea;
	private Choice chooser;
	private ClipChoice[] choices;

	public ClipFrame(Component a_owner, String title, boolean open, ClipChoice[] choices)
	{
		super(a_owner, title);
		init(open, choices);
	}

	public ClipFrame(Component a_owner, String title, boolean open)
	{
		super(a_owner, title);
		init(open, null);
	}


	public static class ClipChoice
	{
		public String name;
		public String text;

		public ClipChoice(String n, String t)
		{
			name = n;
			text = t;
		}
	}

	private void init(boolean open, ClipChoice[] c)
	{
		choices = c;
		if (choices == null)
		{
			chooser = null;
		}
		else
		{
			chooser = new Choice();
			for (int i = 0; i < choices.length; i++)
			{
				chooser.add(choices[i].name);
			}
			getContentPane().add(chooser, BorderLayout.NORTH);
			chooser.addItemListener(this);
		}

		m_TextArea = new TextArea(30, 80);
		m_TextArea.setText("");
		getContentPane().add(m_TextArea, BorderLayout.CENTER);

		if (open == true)
		{
			Button b = new Button("Open");
			b.addActionListener(this);
			b.setActionCommand("open");
			getContentPane().add(b, BorderLayout.SOUTH);
		}

		addWindowListener(new WindowAdapter()
		{
			public void windowClosing(WindowEvent e)
			{
				dispose();
			}
		});

		pack();
	}



	public void setText(String data)
	{
		m_TextArea.setText(data);
	}

	public String getText()
	{
		return m_TextArea.getText();
	}

	public void actionPerformed(ActionEvent ae)
	{
		if (ae.getActionCommand().equals("open"))
		{
			if (m_TextArea.getText().equals(""))
			{
				JAPDialog.showErrorDialog(getOwner(), "The Text Area is empty!", LogType.GUI);
			}
			else
			{
				dispose();
			}
		}
	}

	public void itemStateChanged(ItemEvent e)
	{
		setText(choices[chooser.getSelectedIndex()].text);
	}

}
