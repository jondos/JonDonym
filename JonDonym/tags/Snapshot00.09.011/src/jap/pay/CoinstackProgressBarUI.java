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
package jap.pay;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import javax.swing.ImageIcon;
import javax.swing.JProgressBar;
import javax.swing.plaf.basic.BasicProgressBarUI;
import javax.swing.JComponent;

/**
 * This class is an extended progress bar that uses coin images for displaying
 * progress
 *
 * @author Hannes Federrath, Tobias Bayer
 */
public class CoinstackProgressBarUI extends BasicProgressBarUI
{
	private static final int Y_OFFSET = 6;
	private static final int X_OFFSET = 6;
	private static final int[] X_SHIFT = {0, 3, 1, -2, -1, 0, -1, 0};

	private Image m_imgCoinImage;
	private int m_yFactor;
	private int m_xPos;
	private int m_yPos;
	private int m_imageHeight;
	private int m_imageWidth;
	private int m_height;
	private int m_width;

	public CoinstackProgressBarUI(ImageIcon imageIcon, int min, int max)
	{
		super();
		m_imgCoinImage = imageIcon.getImage();
		m_imageHeight = m_imgCoinImage.getHeight(null);
		m_imageWidth = m_imgCoinImage.getWidth(null);
		m_yFactor = m_imageHeight / 3;
		m_width = 2 * X_OFFSET + m_imageWidth + 4 + 3;
		m_height = 2 * Y_OFFSET + m_imageHeight + m_yFactor * (max - min - 1);
	}

	public void paint(Graphics g,JComponent c)
	{
		JProgressBar pb = (JProgressBar) c;

		int y_pos_end;
		int y_rule_middle;

		//calculate height (necessary if setMaximum or setMinimum was called)
		m_height = 2 * Y_OFFSET + m_imageHeight + m_yFactor * (pb.getMaximum() - pb.getMinimum() - 1);
		//set color of lines
		g.setColor(Color.gray);
		//draw vertical line
		m_xPos = X_OFFSET;
		m_yPos = m_height - Y_OFFSET;
		y_pos_end = m_yPos - (m_imageHeight + (m_yFactor) * (pb.getMaximum() - pb.getMinimum() - 1));
		g.drawLine(m_xPos, m_yPos, m_xPos, y_pos_end);
		//draw horizontal lines
		y_rule_middle = m_yPos - (m_yPos - y_pos_end) / 2;
		g.drawLine(m_xPos, m_yPos, m_xPos + 3, m_yPos);
		g.drawLine(m_xPos, y_pos_end, m_xPos + 3, y_pos_end);
		g.drawLine(m_xPos, y_rule_middle, m_xPos + 3, y_rule_middle);
		//no coin to draw if mimimum value
		if (pb.getValue() == pb.getMinimum())
		{
			return;
		}
		//draw coin
		int x_pos = X_OFFSET + 4;
		int y_pos = m_height - Y_OFFSET - m_imageHeight + 1;
		for (int i = 0; i < (pb.getValue() - pb.getMinimum()); i++)
		{
			x_pos = x_pos + X_SHIFT[i % X_SHIFT.length];
			g.drawImage(m_imgCoinImage, x_pos, y_pos, null);
			y_pos = y_pos - m_yFactor;
		}
	}

	public Dimension getMinimumSize(JComponent c)
	{
		return new Dimension(m_width, m_height);
	}

	public Dimension getPreferredSize(JComponent c)
	{
		return new Dimension(m_width, m_height);
	}
}
