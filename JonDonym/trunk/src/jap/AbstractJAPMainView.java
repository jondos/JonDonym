/*
 Copyright (c) 2000 - 2004, The JAP-Team
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

import javax.swing.JFrame;

import gui.AWTUpdateQueue;
import gui.JAPDll;

public abstract class AbstractJAPMainView extends JFrame implements IJAPMainView
{
	protected String m_Title;
	protected JAPController m_Controller;

	private boolean m_bChangingTitle = false;
	private final Object SYNC_TITLE = new Object();

	private final AWTUpdateQueue AWT_UPDATE_QUEUE = new AWTUpdateQueue(new Runnable()
	{
		public void run()
		{
			onUpdateValues();
		}
	});

	public AbstractJAPMainView(String s, JAPController a_controller)
	{
		super(s);
		setName(s);
		m_Controller = a_controller;
		m_Title = s;
		setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
		//setName(Double.toString(Math.random()));
	}

	public abstract void saveWindowPositions();

	public void setTitle(String a_title)
	{
		setName(a_title);
		super.setTitle(a_title);
	}

	public abstract void showIconifiedView();

	public void setVisible(boolean a_bVisible)
	{
		if (a_bVisible)
		{
			JAPViewIconified viewiconified=this.getViewIconified();
			if(viewiconified!=null)
				viewiconified.setVisible(false);
		}
		super.setVisible(a_bVisible);
	}

	public void showConfigDialog(String card, Object a_value)
	{
	}

	public final void showConfigDialog()
	{
		showConfigDialog(null, null);
	}


	public void packetMixed(long a_totalBytes)
	{
	}

	public final boolean isChangingTitle()
	{
		return m_bChangingTitle;
	}

	public boolean hideWindowInTaskbar()
	{
		synchronized (SYNC_TITLE) //updateValues may change the Title of the Window!!
		{
			m_bChangingTitle = true;
			setTitle(Double.toString(Math.random())); //ensure that we have an unique title
			boolean b = JAPDll.hideWindowInTaskbar(getTitle());
			if (b)
			{
				setVisible(false);
			}
			setTitle(m_Title);
			m_bChangingTitle = false;
			return b;
		}
	}

	public void updateValues(boolean bSync)
	{
		AWT_UPDATE_QUEUE.update(bSync);
	}
}
