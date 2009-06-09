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
package jap;

import java.awt.Color;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.ImageIcon;

import anon.util.JAPMessages;
import anon.util.ResourceLoader;
import gui.GUIUtils;
import gui.dialog.JAPDialog;

final class JAPAbout extends JAPDialog
{
	private final static int ABOUT_DY = 173;
	private final static int ABOUT_DX = 350;
	private JAPAboutAutoScroller sp;

	public JAPAbout(Window p)
	{
		super(p, "Info...", false);
		super.setVisible(false);
		try
		{
			init();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	private void init()
	{
		setVisible(false);
		addWindowListener(new WindowAdapter()
		{
			public void windowClosing(WindowEvent e)
			{
				OKPressed();
			}
		});
		setLocation( -380, -200);
		setSize(10, 10);
		ImageIcon imageSplash = GUIUtils.loadImageIcon(JAPConstants.ABOUTFN, true, false); //loading the Background Image
		byte[] buff = ResourceLoader.loadResource(JAPMessages.getString("htmlfileAbout"));
		sp = new JAPAboutAutoScroller(ABOUT_DX, ABOUT_DY, imageSplash.getImage(), 5, 62, 210, 173 - 72,
									  new String(buff)); //Creating a new scrolling HTML-Pane with the specified size
		sp.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				OKPressed();
			}
		});
		getContentPane().setBackground(new Color(204, 204, 204));
		getContentPane().setLayout(null);
		getContentPane().add(sp);
		//setContentPane(sp);
		//Now we do a little bit tricky stuff.
		//String os = System.getProperty("os.name");
		setVisible(true); //now we have to ensure that the window is visible before the
		//	if(os==null||!os.toLowerCase().startsWith("mac"))
		{
			//setLocation(-380,-200); //First we move the Dialog to a position were it is not seen on the Screen...
			setVisible(true); //now we have to ensure that the window is visible before the
			setResizable(false); //get the insets (the border around the window) - also the window must look like it should
			Insets in = getInsets(); //so for instance we need the 'NoResizable'-Border
			//setResizable(true); //now we want to resize the whole dialog

			//We do not use pack() because it doesnt work well on Windows!
			setSize(ABOUT_DX + in.left + in.right, ABOUT_DY + in.bottom + in.top); // so what the background image does exactly fit
		}
		//	else
		{
			//		pack(); //--> Maybe a solution for MAC'S ??
		}
		setResizable(false); //but the user shouldn't resize the Dialog again
		this.setLocationCenteredOnOwner(); //now showing centerd to JAP-Main
		toFront();
		sp.startScrolling(95); //starting the scrolling...
	}

	private void OKPressed()
	{
		sp.stopScrolling();
		dispose();
	}
}
