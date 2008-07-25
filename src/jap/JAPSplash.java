/*
 Copyright (c) 2000-2006, The JAP-Team
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

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.MediaTracker;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.Frame;
import java.awt.image.ColorModel;

/**
 * Shows the splash screen on startup
 *
 * Dimensions and other attributes of the image used can not be dynamically determined (old java version),
 * change the private static final variables at the beginning of the class if you replace the image used
 *
 */
final public class JAPSplash extends Window implements ISplashResponse
{
	private static final String IMGPATHHICOLOR = "images/";
	private static final String IMGPATHLOWCOLOR = "images/lowcolor/";
	private static final String SPLASH_FILE = "splash.jpg";
	private static final String BUSY_FILE = JAPConstants.BUSYFN;

	private static final int SPLASH_WIDTH = 501;
	private static final int SPLASH_HEIGHT = 330;
	private static final int SPLASH_FILESIZE = 150000;//image file is read into buffer array, increase its size here if necessary
	private static final int BUSY_FILESIZE = 7000;

	//positioning the version string on the image (pixels from lower-right corner)
	private static final int VERSION_OFFSET_X = 10;
	private static final int VERSION_OFFSET_Y = 15;

	//position the progress bar (pixels from upper-right corner)
	private static final int BUSY_POSITION_X = 15;
	private static final int BUSY_POSITION_Y = 312;

	//position the "loading..."-string (pixels from upper-right corner)
	private static final int MESSAGE_POSITION_X = 17;
	private static final int MESSAGE_POSITION_Y = 302;

	private Image m_imgSplash;
	private Image m_imgBusy;
	private Image m_imgOffScreen = null;
	private Font m_fntFont;
	private String m_strLoading;
	private String m_currentText;
	private String m_strVersion;
	private int m_iXVersion;
	private int m_iYVersion;

	public JAPSplash(Frame a_frmParent)
	{
		this (a_frmParent, null);
	}

	public JAPSplash(Frame frmParent, String a_message)
	{
		super(frmParent);
		setLayout(null);
		m_iXVersion = m_iYVersion = 100;
		Toolkit defaultToolkit = Toolkit.getDefaultToolkit();
		MediaTracker imageTracker = new MediaTracker(this);

		loadImages(imageTracker);

		if (a_message == null || a_message.trim().length() == 0)
		{
			setText("Busy");
		}
		else
		{
			setText(a_message);
		}

		m_strVersion = "Version: " + JAPConstants.aktVersion;
		m_fntFont = new Font("Sans", Font.PLAIN, 9);
		FontMetrics fontmetrics = defaultToolkit.getFontMetrics(m_fntFont);
		m_iXVersion = SPLASH_WIDTH - VERSION_OFFSET_X - fontmetrics.stringWidth(m_strVersion);
		m_iYVersion = SPLASH_HEIGHT - VERSION_OFFSET_Y;
		setSize(SPLASH_WIDTH, SPLASH_HEIGHT);
	    try
		{
			imageTracker.waitForAll();
		}
		catch (Exception e)
		{}
		;
		toFront();

	}

	private Image loadImage(String pathToFile,int filesize, MediaTracker imageTracker)
	{
		InputStream in = null;
		Class JapClass = null;
		try
		{
			JapClass = Class.forName("JAP");
		}
		catch (Exception e)
		{
		}

		in = JapClass.getResourceAsStream(pathToFile);
		if (in == null)
		{
			try
			{
				in = new FileInputStream(pathToFile);
			}
			catch (FileNotFoundException ex)
			{
				;
			}
		}
		int len;
		int aktIndex;
		Image imageResult = null;
		if (in != null)
		{
			Toolkit defaultToolkit = Toolkit.getDefaultToolkit();
			byte[] buff = new byte[filesize];
			len = 0;
			aktIndex = 0;
			try
			{
				while ( (len = in.read(buff, aktIndex, buff.length - aktIndex)) > 0)
				{
					aktIndex += len;
				}
				imageResult = defaultToolkit.createImage(buff, 0, aktIndex);
				imageTracker.addImage(m_imgSplash, 1);
				imageTracker.checkID(1, true);
			}
			catch (Exception e)
			{
			}
		}
		return imageResult;
	}


	private boolean isHighColor()
	{
		ColorModel colorModel=null;
		Toolkit defaultToolkit = Toolkit.getDefaultToolkit();
		try{//Do not remove the try-catch block as some faulty JRE throw a null pointer excpetion in getColorModel()
			colorModel = defaultToolkit.getColorModel();
		}catch(Throwable t1)
		{

		}
		if (colorModel == null)
		{
			return false; //if we can't be sure, safer to default to low color
		}
		if (colorModel.getPixelSize() > 16 )
		{
			return true;
		}
		else
		{
			return false;
		}

    }

	private void loadImages(MediaTracker imageTracker)
	{

		if (isHighColor())
		{
			m_imgSplash = loadImage(IMGPATHHICOLOR + SPLASH_FILE, SPLASH_FILESIZE, imageTracker);
			m_imgBusy = loadImage(IMGPATHHICOLOR + BUSY_FILE, BUSY_FILESIZE, imageTracker);
		}
		else
		{
			m_imgSplash = loadImage(IMGPATHLOWCOLOR + SPLASH_FILE, SPLASH_FILESIZE, imageTracker);
			m_imgBusy = loadImage(IMGPATHLOWCOLOR + BUSY_FILE, BUSY_FILESIZE, imageTracker);
		}
	}

	public void setText(String a_text)
	{
		if (a_text != null && a_text.trim().length() > 0)
		{
			m_currentText = a_text;
			m_strLoading = a_text + "...";
		}
	}
	
	public String getText()
	{
		return m_currentText;
	}

	public void update(Graphics g)
	{
		paint(g);
	}

	public void paint(Graphics g)
	{
		if (m_imgOffScreen == null)
		{
			m_imgOffScreen = createImage(SPLASH_WIDTH, SPLASH_HEIGHT);
		}
		Graphics offscreenGraphics = m_imgOffScreen.getGraphics();
		if (m_imgSplash != null)
		{
			offscreenGraphics.drawImage(m_imgSplash, 0, 0, this);
		}
		if (m_imgBusy != null)
		{
			offscreenGraphics.drawImage(m_imgBusy, BUSY_POSITION_X, BUSY_POSITION_Y, this);
		}
		offscreenGraphics.setColor(Color.gray);
		offscreenGraphics.drawRect(0, 0, SPLASH_WIDTH -1 , SPLASH_HEIGHT - 1);
		offscreenGraphics.setFont(m_fntFont);
		offscreenGraphics.setColor(Color.black);
		offscreenGraphics.drawString(m_strLoading, MESSAGE_POSITION_X, MESSAGE_POSITION_Y);
		offscreenGraphics.drawString(m_strVersion, m_iXVersion, m_iYVersion);
		g.drawImage(m_imgOffScreen, 0, 0, this);
	}

	public void centerOnScreen()
	{
		centerOnScreen(this);
	}

	/**
	 * Centers a window relative to the screen.
	 * @param a_window a Window
	 * @note copied form GUIUtils - because we want to have the smallest possible dependencies for JAPSplash-Screen to make it load faster
	 */
	private static void centerOnScreen(Window a_window)
	{
		Rectangle screenBounds;
		Dimension ownSize = a_window.getSize();

		try
		{
			// try to center the window on the default screen; useful if there is more than one screen
			Object graphicsEnvironment =
				Class.forName("java.awt.GraphicsEnvironment").getMethod(
						"getLocalGraphicsEnvironment", null).invoke(null, null);
			Object graphicsDevice = graphicsEnvironment.getClass().getMethod(
				 "getDefaultScreenDevice", null).invoke(graphicsEnvironment, null);
			Object graphicsConfiguration = graphicsDevice.getClass().getMethod(
				"getDefaultConfiguration", null).invoke(graphicsDevice, null);
			screenBounds = (Rectangle)graphicsConfiguration.getClass().getMethod(
				 "getBounds", null).invoke(graphicsConfiguration, null);
		}
		catch(Exception a_e)
		{
			// not all methods to get the default screen are available in JDKs < 1.3
			screenBounds = new Rectangle(new Point(0,0), a_window.getToolkit().getScreenSize());
		}

		a_window.setLocation(screenBounds.x + ((screenBounds.width - ownSize.width) / 2),
							 screenBounds.y + ((screenBounds.height - ownSize.height) / 2));
	}

}
