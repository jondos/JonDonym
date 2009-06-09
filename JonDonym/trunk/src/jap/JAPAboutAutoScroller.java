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


import java.awt.Canvas;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JButton;
import javax.swing.JEditorPane;

public final class JAPAboutAutoScroller extends Canvas implements Runnable
{
	/**
	 * serial version UID
	 */
	private static final long serialVersionUID = 1L;
	
	private Image m_imgOffScreen;
	private Image m_imgBackground;
	private Image m_imgDoubleBuffer;
	private Image m_imgBackgroundPicture;

	private int m_iScrollAreaWidth;
	private int m_iScrollAreaHeight;
	private int m_iScrollAreaX;
	private int m_iScrollAreaY;
	private int m_iaktY;
	private int m_iTextHeight;
	private int m_iWidth;
	private int m_iHeight;
	private JEditorPane m_textArea;
	private Thread m_Thread;
	private int m_msSleep;
	private volatile boolean m_bRun;
	private Object oSync;
	private boolean isPainting;
	private JButton m_bttnOk;

	public JAPAboutAutoScroller(int width, int height, Image background,
								int scrollareax, int scrollareay,
								int scrollareawidth, int scrollareaheight,
								String htmlText)
	{
		oSync = new Object();
		isPainting = false;
		m_iScrollAreaWidth = scrollareawidth;
		m_iScrollAreaHeight = scrollareaheight;
		m_iScrollAreaX = scrollareax;
		m_iScrollAreaY = scrollareay;
		m_iWidth = width;
		m_iHeight = height;
		setSize(width, height);
		addMouseListener(new MouseAdapter()
		{
			public void mouseClicked(MouseEvent e)
			{
				if (m_bttnOk.getBounds().contains(e.getPoint()))
				{
					m_bttnOk.doClick();
				}
			}
		});
		m_imgBackgroundPicture = background;
		m_textArea = new JEditorPane();
		m_textArea.setEditable(false);
//		m_textArea.setHighlighter(null);
		//m_textArea.setEnabled(false);
		m_textArea.setDoubleBuffered(false);
		m_textArea.setBackground(new Color(204, 204, 204));
		m_textArea.setSize(m_iScrollAreaWidth, 10000);
		m_textArea.setContentType("text/html");
		m_textArea.setText(htmlText.trim());
		m_iTextHeight = m_textArea.getPreferredSize().height;

		m_bttnOk = new JButton("Ok");
		m_bttnOk.setMnemonic('O');
		m_bttnOk.setOpaque(false);
		m_bttnOk.setSelected(true);
		Dimension d = m_bttnOk.getPreferredSize();
		if (d.width > 76)
		{
			d.width = 76;
		}
		m_bttnOk.setSize(d); //resizing the OK-Button
		m_Thread = new Thread(this, "JAP - AboutScroller");
		m_bRun = false;
	}

	public void addActionListener(ActionListener l)
	{
		m_bttnOk.addActionListener(l);
	}

	public synchronized void startScrolling(int msScrollTime)
	{
		if (m_bRun)
		{
			return;
		}
		m_msSleep = msScrollTime;
		m_Thread.start();
	}

	public synchronized void stopScrolling()
	{
		m_bRun = false;
		try
		{
			m_Thread.join();
		}
		catch (Exception e)
		{}
	}

	/**
	 * override update to *not* erase the background before painting
	 */
	public void update(Graphics g)
	{
		paint(g);
	}

	public void paint(Graphics g1)
	{
		if (g1 == null)
		{
			return;
		}
		synchronized (oSync)
		{
			if (isPainting)
			{
				return;
			}
			isPainting = true;
		}

		if (m_imgOffScreen == null)
		{
			m_imgOffScreen = createImage(m_iScrollAreaWidth, m_iTextHeight + 2 * m_iScrollAreaHeight);
			Graphics graphicsOffScreen = m_imgOffScreen.getGraphics();
			try
			{
				m_textArea.paint(graphicsOffScreen);
			}
			catch (Exception e)
			{
				m_imgOffScreen = null;
			}
			if (graphicsOffScreen != null)
			{
				graphicsOffScreen.dispose();
			}
		}
		if (m_imgBackground == null)
		{
			m_imgBackground = createImage(m_iWidth, m_iHeight);
			Graphics g2 = m_imgBackground.getGraphics();
			g2.drawImage(m_imgBackgroundPicture, 0, 0, null);
			int x = m_iWidth - 5 - m_bttnOk.getSize().width; //calculating the coordinates for the Button...
			int y = m_iHeight - 5 - m_bttnOk.getSize().height; //.. it should appear 5 Points away from the right and bottom border
			m_bttnOk.setLocation(x, y); //the set the Position of the Button

			Font f = new Font("Sans", Font.PLAIN, 9);
			g2.setFont(f);
			g2.setColor(Color.black);
			FontMetrics fm = g2.getFontMetrics();
			int w = fm.stringWidth("Version:");
			//Finaly we do the same for the 'Version'-Text
			g2.drawString("Version", x - 5 - w, y);
			//Now the set the Position of the VersionNumber...
			//...it should appear 5 Points above the bottom border (as the OK-Button does)...
			//...and 5 Points away from the OK-Button
			w = fm.stringWidth(JAPConstants.aktVersion);
			g2.drawString(JAPConstants.aktVersion, x - 5 - w, m_iHeight - 5 - fm.getHeight());
			g2.translate(x, y);
			m_bttnOk.paint(g2);
			g2.dispose();
			m_imgDoubleBuffer = createImage(m_iWidth, m_iHeight);
		}

		Graphics g = m_imgDoubleBuffer.getGraphics();
		g.drawImage(m_imgBackground, 0, 0, null);
		if (m_imgOffScreen != null)
		{
			m_iaktY++;
			if (m_iaktY <= m_iScrollAreaHeight)
			{
				g.drawImage(m_imgOffScreen, m_iScrollAreaX, m_iScrollAreaY + m_iScrollAreaHeight - m_iaktY,
							m_iScrollAreaX + m_iScrollAreaWidth, m_iScrollAreaY + m_iScrollAreaHeight, 0, 0,
							m_iScrollAreaWidth, m_iaktY, null);
			}
			else
			{
				g.drawImage(m_imgOffScreen, m_iScrollAreaX, m_iScrollAreaY,
							m_iScrollAreaWidth + m_iScrollAreaX, m_iScrollAreaHeight + m_iScrollAreaY, 0,
							m_iaktY - m_iScrollAreaHeight, m_iScrollAreaWidth, m_iaktY, null);
			}
		}
		g.dispose();
		g1.drawImage(m_imgDoubleBuffer, 0, 0, null);
		isPainting = false;
	}

	public void run()
	{
		m_iaktY = 0;
		m_bRun = true;
		while (m_bRun)
		{
			paint(getGraphics());
			try
			{
				Thread.sleep(m_msSleep);
			}
			catch (Exception e)
			{}
			if (m_iaktY > m_iTextHeight + m_iScrollAreaHeight)
			{
				m_iaktY = 0;
			}
		}
	}
}
