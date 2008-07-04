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
package gui;

import javax.swing.JProgressBar;
import javax.swing.plaf.ProgressBarUI;
import java.awt.Color;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;

import javax.swing.JComponent;
import javax.swing.JProgressBar;
import javax.swing.plaf.basic.BasicProgressBarUI;

/**
 * Implements a progress bar style for JAP.
 *
 * @author Rolf Wendolsky
 */
public class JAPProgressBar extends JProgressBar
{
	private final MyProgressBarUI m_ui;

	public JAPProgressBar()
	{
		m_ui = new MyProgressBarUI(true);
		m_ui.setFilledBarColor(Color.blue);
		super.setUI(m_ui);
	}

	public void setUI(ProgressBarUI a_ui)
	{
		// do nothing
	}

	public ProgressBarUI getUI()
	{
		return m_ui;
	}
	public void setFilledBarColor(Color a_color)
	{
		m_ui.setFilledBarColor(a_color);
	}
	public Color getFilledbarColor()
	{
		return m_ui.getFilledBarColor();
	}

	private final class MyProgressBarUI extends BasicProgressBarUI
	{
		final static int ms_dx = 13;
		final static int ms_width = 9;
		private boolean m_bOneBarPerValue = false;
		private Color m_colFilledBar;

		public MyProgressBarUI(boolean bOneBarPerValue)
		{
			super();
			m_bOneBarPerValue = bOneBarPerValue;
			m_colFilledBar = null;
		}

		public void paint(Graphics g, JComponent c)
		{
			JProgressBar pb = (JProgressBar) c;
			int max = pb.getMaximum();
			int anz = pb.getWidth() / ms_dx;
			int value = pb.getValue() * anz / max;
			int x = 0;
			int y = 0;
			int height = c.getHeight()-1;
			Color col = g.getColor();
			if (m_colFilledBar != null)
			{
				g.setColor(m_colFilledBar);
			}
			for (int i = 0; i < value; i++)
			{
				g.fill3DRect(x, y, ms_width, height+1, false);
				x += ms_dx;
			}
			g.setColor(col);
			for (int i = value; i < anz; i++)
			{
				g.draw3DRect(x, y, ms_width, height, false);
				x += ms_dx;
			}
		}

		public void setFilledBarColor(Color col)
		{
			m_colFilledBar = col;
		}

		public Color getFilledBarColor()
		{
			return m_colFilledBar;
		}

		public Dimension getPreferredSize(JComponent c)
		{
			if (!m_bOneBarPerValue)
			{
				return super.getPreferredSize(c);
			}
			JProgressBar pb = (JProgressBar) c;
			return new Dimension(ms_dx * pb.getMaximum(), 12);
		}

		public Dimension getMinimumSize(JComponent c)
		{
			if (!m_bOneBarPerValue)
			{
				return super.getMinimumSize(c);
			}
			return getPreferredSize(c);
		}

		public Dimension getMaximumSize(JComponent c)
		{
			if (!m_bOneBarPerValue)
			{
				return super.getMaximumSize(c);
			}
			return getPreferredSize(c);
		}

}
}
