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

import java.util.StringTokenizer;

import java.awt.Button;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.Label;
import java.awt.Panel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/** This is a simple Message Box. It works with JAVA 1.1 without Swing.*/
final public class JAPAWTMsgBox extends WindowAdapter implements ActionListener
{
	private Dialog d;
	private JAPAWTMsgBox(Frame parent, String msg, String title)
	{
		try
		{
			d = new Dialog(parent, title, true);
			d.addWindowListener(this);
			GridLayout g = new GridLayout(0, 1, 0, 0);
			Panel p = new Panel();
			p.setLayout(g);
			StringTokenizer st = new StringTokenizer(msg, "\n");
			while (st.hasMoreElements())
			{
				Label l = new Label(st.nextToken());
				p.add(l);
			}
			p.add(new Label(" "));
			d.add("Center", p);
			Button b = new Button("   Ok   ");
			b.addActionListener(this);
			p = new Panel();
			p.add(b);
			d.add("South", p);
			p = new Panel();
			p.setSize(7, 7);
			d.add("North", p);
			p = new Panel();
			p.setSize(7, 7);
			d.add("West", p);
			p = new Panel();
			p.setSize(7, 7);
			d.add("East", p);
			d.pack();
			d.setResizable(false);
			Dimension screenSize = d.getToolkit().getScreenSize();
			try //JAVA 1.1
			{
				Dimension ownSize = d.getSize();
				d.setLocation( (screenSize.width - ownSize.width) / 2,
							  (screenSize.height - ownSize.height) / 2);
			}
			catch (Error e) //JAVA 1.0.2
			{
				Dimension ownSize = d.size();
				d.locate( (screenSize.width - ownSize.width) / 2, (screenSize.height - ownSize.height) / 2);
			}
			d.show();
		}
		catch (Exception e)
		{
		}
	}

	/** Shows a Message Box.
	 * @param parent The owner of the Message Box.
	 * @param msg The Message to Display. Multiple line can be separate by \n
	 * @param title The Title of the Message Box (Title of the Window displayed.)
	 */
	final static public int MsgBox(Frame parent, String msg, String title)
	{
		try
		{
			JAPAWTMsgBox msgbox = new JAPAWTMsgBox(parent, msg, title);
		}
		catch (Exception e)
		{
			return -1;
		}
		return 0;
	}

	public void windowClosing(WindowEvent e)
	{
		d.dispose();
	}

	public void actionPerformed(ActionEvent e)
	{
		d.dispose();
	}
}
