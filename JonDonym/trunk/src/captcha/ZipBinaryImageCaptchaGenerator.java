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

/*
 * Idea was taken from "The Code Project" - CAPTCHA Image by BrainJar.
 * The original is available here: http://www.codeproject.com/aspnet/CaptchaImage.asp
 *
 * There are no license or copyright limitations on the original ASP.NET source code.
 * The ASP.NET code was translated into Java (with some modifications).
 */

package captcha;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Image;
import java.util.Random;

import anon.util.Base64;
import anon.util.ZLibTools;
import captcha.graphics.BinaryImageCreator;
import captcha.graphics.ImageFactory;
import captcha.graphics.PerspectiveProjection;
import captcha.graphics.Quadrangle;






/**
 * This class creates zipped binary image captchas (the embedded text is visible on an image in
 * our own binary image format).
 */
public class ZipBinaryImageCaptchaGenerator implements ICaptchaGenerator
{

	/**
	 * This is the font for drawing the text.
	 */
	private static final String DEFAULT_FONT = "SansSerif";

	/**
	 * This is the string of valid captcha characters. The data which are included in the captcha
	 * can be words over this alphabet.
	 */
	private static final String VALID_TEXT_CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";

	/**
	 * This is the format of the captcha data. So the JAP client of the blockee can present the data
	 * correctly.
	 */
	private static final String CAPTCHA_DATA_FORMAT = "ZIP_BINARY_IMAGE";

	/**
	 * Stores the width in pixels all generated images will have.
	 */
	private int m_width;

	/**
	 * Stores the height in pixels all generated images will have.
	 */
	private int m_height;

	/**
	 * Creates a new instance of ZipBinaryImageCaptchaGenerator. All created captchas will have the
	 * size specified here.
	 *
	 * @param a_width The width in pixels all generated images will have.
	 * @param a_height The height in pixels all generated images will have.
	 */
	public ZipBinaryImageCaptchaGenerator(int a_width, int a_height)
	{
		m_width = a_width;
		m_height = a_height;
	}

	/**
	 * Creates a new zipped binary image captcha and returns the image as Base64 encoded String. If
	 * there is an error when creating the captcha (maybe you have specified invalid sizes in the
	 * constructor or the text to embed is to long, ...) an Exception is thrown.
	 *
	 * @param a_embeddedText The text which shall be visible in the zipped binary image.
	 *
	 * @return A Base64 encoded zipped binary image.
	 */
	public String createCaptcha(String a_embeddedText) throws Exception
	{
		Image bitmap = generateCaptchaImage(a_embeddedText);
		byte binaryData[] = BinaryImageCreator.imageToBinary(bitmap);
		byte zippedData[] = ZLibTools.compress(binaryData);
		return (Base64.encode(zippedData, false));
	}

	/**
	 * Returns the string of valid captcha characters. The data which are included in the captcha
	 * can be words over this alphabet.
	 *
	 * @return An alphabet with characters this CaptchaGenerator supports.
	 */
	public String getValidCharacters()
	{
		return VALID_TEXT_CHARACTERS;
	}

	/**
	 * Returns the format of the captcha data (ZIP_BINARY_IMAGE). So the JAP client of the blockee
	 * can present the data correctly.
	 *
	 * @return A string with the data format of the captcha ("ZIP_BINARY_IMAGE").
	 */
	public String getCaptchaDataFormat()
	{
		return CAPTCHA_DATA_FORMAT;
	}

	/**
	 * Returns the maximum number of characters of the captcha generators alphabet, which are
	 * supported as input when creating a captcha.
	 *
	 * @return The maximum captcha generator input word length.
	 */
	public int getMaximumStringLength()
	{
		/* at the moment return always 8, the better solution is, if this value depends on the image
		 * size
		 */
		return 8;
	}

	/**
	 * Creates a captcha image, which shows the specified text.
	 *
	 * @param a_embeddedText The text, which shall be visible on the captcha image.
	 *
	 * @return The captcha image, which shows the text.
	 */
	private Image generateCaptchaImage(String a_embeddedText) throws Exception
	{
		/* get the random number generator */
		Random random = new Random();

		/* create a new bitmap image (BufferedImage implementation -> Graphics is available) */
		Image bitmap = ImageFactory.createImage(m_width, m_height);

		/* create a graphics object for drawing and set some defaults */
		Graphics g = bitmap.getGraphics();

		/* fill the backround */
		g.setColor(Color.white);
		g.fillRect(0, 0, m_width, m_height);

		/* prepare to draw the text */
		g.setColor(Color.darkGray);
		/* initialize with the maximum font size */
		int fontSize = m_height + 1;
		Font font = null;
		int stringWidth = 0;
		/* adjust the font size until the text fits within the image */
		do
		{
			fontSize--;
			font = new Font(DEFAULT_FONT, Font.BOLD, fontSize);
			g.setFont(font);
			stringWidth = g.getFontMetrics().stringWidth(a_embeddedText);
		}
		while (m_width < stringWidth);

		/* we have found the maximum possible font size, now center the text in the image */
		int stringPosX = (m_width - stringWidth) / 2;
		int stringPosY = ( (m_height + g.getFontMetrics().getHeight()) / 2) - 1 -
			g.getFontMetrics().getDescent();
		/* draw the text */
		g.drawString(a_embeddedText, stringPosX, stringPosY);

		/* now add some random noise to the image -> first: remove some elliptic areas */
		g.setColor(Color.white);
		float m = Math.max( (float) m_width, (float) m_height);
		/* create the remove-ellipses, the number of ellipses depends on width and height */
		for (int i = 0; i < (m_width * m_height) / 800; i++)
		{
			int x = (int) Math.floor(random.nextDouble() * (float) m_width);
			int y = (int) Math.floor(random.nextDouble() * (float) m_height);
			/* the size of the ellipses depends on the maximum from width and height */
			int w = (int) Math.floor(random.nextFloat() * m / (float) 20);
			int h = (int) Math.floor(random.nextFloat() * m / (float) 20);
			g.fillOval(x, y, w, h);
		}

		/* second: add some elliptic areas to the image */
		g.setColor(Color.darkGray);
		/* create the add-ellipses, the number of ellipses depends on width and height */
		for (int i = 0; i < (m_width * m_height) / 800; i++)
		{
			int x = (int) Math.floor(random.nextFloat() * (float) m_width);
			int y = (int) Math.floor(random.nextFloat() * (float) m_height);
			/* the size of the remove-ellipses depends on the maximum from width and height */
			int w = (int) Math.floor(random.nextFloat() * m / (float) 20);
			int h = (int) Math.floor(random.nextFloat() * m / (float) 20);
			g.fillOval(x, y, w, h);
		}

		/* now bring some distortion into the image */
		PerspectiveProjection transformMatrix = new PerspectiveProjection(new Quadrangle(0, 0, m_width - 1, 0,
			0, m_height - 1, m_width - 1, m_height - 1),
			new Quadrangle( (int) Math.floor( (random.nextFloat() * (float) m_width) / (float) 4),
						   (int) Math.floor( (random.nextFloat() * (float) m_height) / (float) 4),
						   m_width - 1 - (int) Math.floor( (random.nextFloat() * (float) m_width) / (float) 4),
						   (int) Math.floor( (random.nextFloat() * (float) m_height) / (float) 4),
						   (int) Math.floor( (random.nextFloat() * (float) m_width) / (float) 4),
						   m_height - 1 -
						   (int) Math.floor( (random.nextFloat() * (float) m_height) / (float) 4),
						   m_width - 1 - (int) Math.floor( (random.nextFloat() * (float) m_width) / (float) 4),
						   m_height - 1 -
						   (int) Math.floor( (random.nextFloat() * (float) m_height) / (float) 4)));

		/* calculate the distorted positions of the pixels */
		bitmap = transformMatrix.transform(bitmap, Color.white);
		/* get the new Graphics object, because it's a new bitmap */
		g = bitmap.getGraphics();

		/* because the image is smaller than the original, there are some white edges around ->
		 * it would be to easy to calculate the inverse distortion -> fill the whole picture with
		 * some random noise again, we do this again by adding and removing some ellipses
		 */
		/* first: remove some elliptic areas */
		g.setColor(Color.white);
		/* create the remove-ellipses, the number of ellipses depends on width and height */
		for (int i = 0; i < (m_width * m_height) / 800; i++)
		{
			int x = (int) Math.floor(random.nextFloat() * (float) m_width);
			int y = (int) Math.floor(random.nextFloat() * (float) m_height);
			/* the size of the ellipses depends on the maximum from width and height */
			int w = (int) Math.floor(random.nextFloat() * m / (float) 20);
			int h = (int) Math.floor(random.nextFloat() * m / (float) 20);
			g.fillOval(x, y, w, h);
		}

		/* second: add some elliptic areas to the image */
		g.setColor(Color.darkGray);
		/* create the add-ellipses, the number of ellipses depends on width and height */
		for (int i = 0; i < (m_width * m_height) / 800; i++)
		{
			int x = (int) Math.floor(random.nextFloat() * (float) m_width);
			int y = (int) Math.floor(random.nextFloat() * (float) m_height);
			/* the size of the remove-ellipses depends on the maximum from width and height */
			int w = (int) Math.floor(random.nextFloat() * m / (float) 20);
			int h = (int) Math.floor(random.nextFloat() * m / (float) 20);
			g.fillOval(x, y, w, h);
		}

		return bitmap;
	}

}
