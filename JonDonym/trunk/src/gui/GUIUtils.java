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
package gui;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.Hashtable;
import java.util.Vector;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.MediaTracker;
import java.awt.Point;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.IllegalComponentStateException;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.AWTEvent;
import java.awt.event.KeyEvent;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.MouseListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import javax.swing.ToolTipManager;
import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.KeyStroke;
import javax.swing.JOptionPane;
import javax.swing.JTextPane;
import javax.swing.LookAndFeel;
import javax.swing.UIDefaults;
import javax.swing.UIManager;
import javax.swing.SwingUtilities;
import javax.swing.UIManager.LookAndFeelInfo;
import javax.swing.plaf.FontUIResource;

import anon.util.ClassUtil;
import anon.util.JobQueue;
import anon.util.ResourceLoader;
import anon.infoservice.ServiceLocation;
import gui.dialog.JAPDialog;
import gui.dialog.WorkerContentPane;
import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;
import java.util.Enumeration;
import java.awt.EventQueue;
import java.awt.MenuComponent;
import java.applet.Applet;
import java.awt.Container;
import java.awt.Color;
import java.awt.image.ColorModel;
import java.lang.reflect.Field;
import java.lang.reflect.Constructor;

/**
 * This class contains helper methods for the GUI.
 */
public final class GUIUtils
{
	/**
	 * The default path to store images.
	 */
	public static final String MSG_DEFAULT_IMGAGE_PATH = GUIUtils.class.getName() + "_imagePath";

	/**
	 * Images with a smaller pixes size than 16 bit should be stored in this path. Their names must
	 * be equal to the corresponding images in the default path.
	 */
	public static final String MSG_DEFAULT_IMGAGE_PATH_LOWCOLOR =
		GUIUtils.class.getName() + "_imagePathLowColor";

	private static final String MSG_PASTE_FILE = GUIUtils.class.getName() + "_pasteFile";
	private static final String MSG_COPY_FROM_CLIP = GUIUtils.class.getName() + "_copyFromClip";
	private static final String MSG_SAVED_TO_CLIP = GUIUtils.class.getName() + "_savedToClip";

	private static final int MAXIMUM_TEXT_LENGTH = 60;

	private static boolean ms_loadImages = true;

	private static boolean ms_bCapturingAWTEvents = false;
	private static Point ms_mousePosition;
	private static final Object SYNC_MOUSE_POSITION = new Object();
	private static AWTEventListener ms_mouseListener;

	private static final Vector AWT_EVENT_LISTENERS = new Vector();

	private static final IIconResizer DEFAULT_RESIZER = new IIconResizer()
	{
		public double getResizeFactor()
		{
			return 1.0;
		}
	};

	private static IIconResizer ms_resizer = DEFAULT_RESIZER;

	private static final NativeGUILibrary DUMMY_GUI_LIBRARY = new NativeGUILibrary()
	{
		public boolean setAlwaysOnTop(Window a_window, boolean a_bOnTop)
		{
			return false;
		}

		public boolean isAlwaysOnTop(Window a_window)
		{
			return false;
		}
	};
	private static NativeGUILibrary ms_nativeGUILibrary = DUMMY_GUI_LIBRARY;

	private static final IIconResizer RESIZER = new IIconResizer()
	{
		public double getResizeFactor()
		{
			return ms_resizer.getResizeFactor();
		}
	};



	// all loaded icons are stored in the cache and do not need to be reloaded from file
	private static Hashtable ms_iconCache = new Hashtable();

	public static interface NativeGUILibrary
	{
		public boolean setAlwaysOnTop(Window a_window, boolean a_bOnTop);

		public boolean isAlwaysOnTop(Window a_window);
	}

	/**
	 * Defines a resize factor for icons that is especially useful if the font size is altered.
	 */
	public static interface IIconResizer
	{
		/**
		 * 1.0 means no resizing is done
		 * @return 1.0 means 100%
		 */
		public double getResizeFactor();
	}

	public static final IIconResizer getIconResizer()
	{
		return RESIZER;
	}

	/**
	 * Stops loading of images, e.g. because of an update of the parent JAR file.
	 */
	public static void setLoadImages(boolean a_bLoadImages)
	{
		if (ms_loadImages && !a_bLoadImages)
		{
			LogHolder.log(LogLevel.NOTICE, LogType.GUI, "Loading of images has been stopped!");
		}
		ms_loadImages = a_bLoadImages;
	}

	public static boolean isLoadingImagesStopped()
	{
		return !ms_loadImages;
	}

	public static final void setIconResizer(IIconResizer a_resizer)
	{
		if (a_resizer != null)
		{
			ms_resizer = a_resizer;
		}
		else
		{
			ms_resizer = DEFAULT_RESIZER;
		}
	}

	/**
	 * Loads an ImageIcon from the classpath or the current directory.
	 * The icon may be contained in an archive (JAR) or a directory structure. If the icon could
	 * not be found in the classpath, it is loaded from the current directory.
	 * If even the current directory does not contain the icon, it is loaded from the default image path.
	 * Once an icon is loaded, it is stored in a memory cache, so that further calls of this method
	 * do not load the icon from the file system, but from the cache.
	 * @param a_strRelativeImagePath the relative resource path or filename of the Image
	 * @return the loaded ImageIcon or null if the icon could not be loaded
	 *         (getImageLoadStatus() == java.awt.MediaTracker.ERRORED)
	 */
	public static ImageIcon loadImageIcon(String a_strRelativeImagePath)
	{
		return loadImageIcon(a_strRelativeImagePath, true, true);
	}

	/**
	 * Loads an ImageIcon from the classpath or the current directory.
	 * The icon may be contained in an archive (JAR) or a directory structure. If the icon could
	 * not be found in the classpath, it is loaded from the current directory.
	 * If even the current directory does not contain the icon, it is loaded from the default image path.
	 * Once an icon is loaded, it is stored in a memory cache, so that further calls of this method
	 * do not load the icon from the file system, but from the cache.
	 * The image may be loaded synchronously so that the method only returns when the image has been
	 * loaded completely (or an error occured), or asynchronously so that the method returns even if
	 * the image has not been loaded yet.
	 * @param a_strRelativeImagePath the relative resource path or filename of the Image
	 * @param a_bSync true if the image should be loaded synchronously; false otherwise
	 * @return the loaded ImageIcon or null if the icon could not be loaded
	 *         (getImageLoadStatus() == java.awt.MediaTracker.ERRORED)
	 */
	public static ImageIcon loadImageIcon(String a_strRelativeImagePath, boolean a_bSync)
	{
		return loadImageIcon(a_strRelativeImagePath, a_bSync, true);
	}


	/**
	 * Loads an ImageIcon from the classpath or the current directory.
	 * The icon may be contained in an archive (JAR) or a directory structure. If the icon could
	 * not be found in the classpath, it is loaded from the current directory.
	 * If even the current directory does not contain the icon, it is loaded from the default image path.
	 * Once an icon is loaded, it is stored in a memory cache, so that further calls of this method
	 * do not load the icon from the file system, but from the cache.
	 * The image may be loaded synchronously so that the method only returns when the image has been
	 * loaded completely (or an error occured), or asynchronously so that the method returns even if
	 * the image has not been loaded yet.
	 * @param a_strRelativeImagePath the relative resource path or filename of the Image
	 * @param a_bSync true if the image should be loaded synchronously; false otherwise
	 * @param a_bScale if the icon should be auto-scaled
	 * @return the loaded ImageIcon or null if the icon could not be loaded
	 *         (getImageLoadStatus() == java.awt.MediaTracker.ERRORED)
	 */
	public static ImageIcon loadImageIcon(String a_strRelativeImagePath, boolean a_bSync, boolean a_bScale)
	{
		ImageIcon img = null;
		int statusBits;
		boolean bScalingDone = false;
		String strScaledRelativeImagePath = null;

		if (a_strRelativeImagePath == null)
		{
			return null;
		}

		if (a_bScale && ms_resizer.getResizeFactor() != 1.0)
		{
			// we have to scale; look if there are pre-scaled graphics first
			strScaledRelativeImagePath =
				((int)(100 * ms_resizer.getResizeFactor())) + "/" +  a_strRelativeImagePath;
		}

		// try to load the image from the cache
		if (strScaledRelativeImagePath != null && ms_iconCache.containsKey(strScaledRelativeImagePath))
		{
			img = new ImageIcon((Image)ms_iconCache.get(strScaledRelativeImagePath));
			if (img != null)
			{
				bScalingDone = true;
			}
		}
		else if (ms_iconCache.containsKey(a_strRelativeImagePath))
		{
			img = new ImageIcon((Image)ms_iconCache.get(a_strRelativeImagePath));
		}

		if (img == null && ms_loadImages)
		{
			// load image from the local classpath or the local directory
			if (strScaledRelativeImagePath != null)
			{
				img = loadImageIconInternal(ResourceLoader.getResourceURL(strScaledRelativeImagePath));
				if (img != null)
				{
					bScalingDone = true;
				}
			}

			if (img == null)
			{
				img = loadImageIconInternal(ResourceLoader.getResourceURL(a_strRelativeImagePath));
			}
			ColorModel colorModel=null;
			try{//Do not remove the try-catch block as some faulty JRE throw a null pointer excpetion in getColorModel()
				colorModel = Toolkit.getDefaultToolkit().getColorModel();
			}catch(Throwable t1)
			{
			}
			if (img == null && (colorModel==null||colorModel.getPixelSize() <= 16))
			{
				// load the image from the low color image path
				if (strScaledRelativeImagePath != null)
				{
					img = loadImageIconInternal(
									   ResourceLoader.getResourceURL(
						JAPMessages.getString(MSG_DEFAULT_IMGAGE_PATH_LOWCOLOR) + strScaledRelativeImagePath));
					if (img != null)
					{
						bScalingDone = true;
					}
				}
				if (img == null)
				{
					img = loadImageIconInternal(
									   ResourceLoader.getResourceURL(
						JAPMessages.getString(MSG_DEFAULT_IMGAGE_PATH_LOWCOLOR) + a_strRelativeImagePath));
				}
			}

			if (img == null || img.getImageLoadStatus() == MediaTracker.ERRORED)
			{
				// load the image from the default image path
				if (strScaledRelativeImagePath != null)
				{
					img = loadImageIconInternal(
						ResourceLoader.getResourceURL(
							JAPMessages.getString(MSG_DEFAULT_IMGAGE_PATH) + strScaledRelativeImagePath));
						if (img != null)
						{
							bScalingDone = true;
						}
				}
				if (img == null)
				{
					img = loadImageIconInternal(
									   ResourceLoader.getResourceURL(
						JAPMessages.getString(MSG_DEFAULT_IMGAGE_PATH) + a_strRelativeImagePath));
				}
			}

			if (img != null)
			{
				if (a_bSync)
				{
					statusBits = MediaTracker.ABORTED | MediaTracker.ERRORED | MediaTracker.COMPLETE;
					while ( (img.getImageLoadStatus() & statusBits) == 0)
					{
						Thread.yield();
					}
				}

				// write the image to the cache
				if (strScaledRelativeImagePath != null && bScalingDone)
				{
					ms_iconCache.put(strScaledRelativeImagePath, img.getImage());
				}
				else
				{
					ms_iconCache.put(a_strRelativeImagePath, img.getImage());
				}
			}

			statusBits = MediaTracker.ABORTED | MediaTracker.ERRORED;
			if (img == null || (img.getImageLoadStatus() & statusBits) != 0)
			{
				LogHolder.log(LogLevel.INFO, LogType.GUI,
							  "Could not load requested image '" + a_strRelativeImagePath + "'!");
			}
		}

		if (a_bScale && !bScalingDone && ms_loadImages && ms_resizer.getResizeFactor() != 1.0)
		{
			// this image must be scaled

			final ImageIcon image = img;
			WorkerContentPane.IReturnRunnable run = new WorkerContentPane.IReturnRunnable()
			{
				private ImageIcon m_icon;
				public void run()
				{
					m_icon = GUIUtils.createScaledImageIcon(image, ms_resizer);
				}

				public Object getValue()
				{
					return m_icon;
				}
			};
			Thread thread = new Thread(run);
			thread.setDaemon(true);
			thread.start();
			try
			{
				thread.join(1000);
			}
			catch (InterruptedException ex)
			{
				// ignore
			}
			while (thread.isAlive())
			{
				thread.interrupt();
				try
				{
					thread.join();
				}
				catch (InterruptedException a_e)
				{
					// ignore
				}
			}
			if (run.getValue() != null)
			{
				return (ImageIcon)run.getValue();
			}
			if (img != null && run.getValue() == null)
			{
				LogHolder.log(LogLevel.ERR, LogType.GUI, "Interrupted while scaling image icon!");
			}
		}
		return img;
	}

	private static ImageIcon loadImageIconInternal(URL a_imageURL)
	{
		try
		{
			return new ImageIcon(a_imageURL);
		}
		catch (NullPointerException a_e)
		{
			return null;
		}
	}

	/**
	 * Combines two images vertically to one image.
	 * @param a_one an ImageIcon
	 * @param a_two another ImageIcon
	 * @return ImageIcon the combination of both ImageIcons or a_one
	 * if this method is not supported by the current JRE
	 */
	public static ImageIcon combine(ImageIcon a_one, ImageIcon a_two)
	{
		int totalWidth = a_one.getIconWidth() + a_two.getIconWidth();
		int totalHeight = Math.max(a_one.getIconHeight(), a_two.getIconHeight());

		try
		{
			Class classBufferedImage = Class.forName("java.awt.image.BufferedImage");
			Field fieldTYPE_INT_ARGB = classBufferedImage.getField("TYPE_INT_ARGB");
			Image objectBufferedImage;

			Constructor constructorBufferedImage =
				classBufferedImage.getConstructor(new Class[]{int.class, int.class, int.class});
			objectBufferedImage = (Image)
				constructorBufferedImage.newInstance(new Object[]{
				new Integer(totalWidth), new Integer(totalHeight),
				new Integer(fieldTYPE_INT_ARGB.getInt(classBufferedImage))});

			Graphics objectGraphics2D = (Graphics)
				classBufferedImage.getMethod("createGraphics", null).invoke(objectBufferedImage, null);
			objectGraphics2D.drawImage(a_one.getImage(), 0, 0, null);
			objectGraphics2D.drawImage(a_two.getImage(), a_one.getIconWidth(), 0, null);
			objectGraphics2D.dispose();
			return new ImageIcon(objectBufferedImage);


			/*
			BufferedImage totalImage =
				new BufferedImage(totalWidth, totalHeight, java.awt.image.BufferedImage.TYPE_INT_ARGB);
			Graphics2D g = totalImage.createGraphics();
			g.drawImage(a_one.getImage(), 0, 0, null);
			g.drawImage(a_two.getImage(), a_one.getIconWidth(), 0, null);
			g.dispose();
			return new ImageIcon(totalImage);*/
		}
		catch (Exception a_e)
		{
			return a_one;
		}
	}

	public static void setLocationRelativeTo(Component a_component, Window a_movedWindow)
	{
		if (a_component == null && a_movedWindow == null)
		{
			return;
		}

		Container root = null;

		if (a_component != null)
		{
			if (a_component instanceof Window || a_component instanceof Applet)
			{
				root = (Container) a_component;
			}
			else
			{
				Container parent;
				for (parent = a_component.getParent(); parent != null; parent = parent.getParent())
				{
					if (parent instanceof Window || parent instanceof Applet)
					{
						root = parent;
						break;
					}
				}
			}
		}

		if ( (a_component != null && !a_component.isShowing()) || root == null ||
			!root.isShowing())
		{
			Dimension paneSize = a_movedWindow.getSize();
			Dimension screenSize = a_movedWindow.getToolkit().getScreenSize();

			a_movedWindow.setLocation( (screenSize.width - paneSize.width) / 2,
									  (screenSize.height - paneSize.height) / 2);
		}
		else
		{
			Dimension invokerSize = a_component.getSize();
			Point invokerScreenLocation;

			if (root instanceof Applet)
			{
				invokerScreenLocation = a_component.getLocationOnScreen();
			}
			else
			{
				invokerScreenLocation = new Point(0, 0);
				Component tc = a_component;
				while (tc != null)
				{
					Point tcl = tc.getLocation();
					invokerScreenLocation.x += tcl.x;
					invokerScreenLocation.y += tcl.y;
					if (tc == root)
					{
						break;
					}
					tc = tc.getParent();
				}
			}

			Rectangle windowBounds = a_movedWindow.getBounds();
			int dx = invokerScreenLocation.x + ( (invokerSize.width - windowBounds.width) >> 1);
			int dy = invokerScreenLocation.y + ( (invokerSize.height - windowBounds.height) >> 1);
			Dimension ss = a_movedWindow.getToolkit().getScreenSize();

			if (dy + windowBounds.height > ss.height)
			{
				dy = ss.height - windowBounds.height;
				dx = invokerScreenLocation.x < (ss.width >> 1) ? invokerScreenLocation.x + invokerSize.width :
					invokerScreenLocation.x - windowBounds.width;
			}
			if (dx + windowBounds.width > ss.width) dx = ss.width - windowBounds.width;
			if (dx < 0) dx = 0;
			if (dy < 0) dy = 0;
			a_movedWindow.setLocation(dx, dy);
		}
	}

	/**
	 * Finds the first parent that is a window.
	 * @param a_component a Component
	 * @return the first parent that is a window (may be the component itself) or the
	 * default frame if no parent window was found
	 */
	public static Window getParentWindow(Component a_component)
	{
		Component component = a_component;

		if (component == null)
		{
			// no component given; get the default frame instead
			component = new JOptionPane().createDialog(component, "").getParent();
		}

		while (component != null && ! (component instanceof Window))
		{

			component = component.getParent();
		}

		return (Window)component;
	}


	/**
	 * Positions a window on the screen relative to a parent window so that its position is optimised.
	 * @param a_window a Window
	 * @param a_parent the Window's parent window
	 */
	public static void positionRightUnderWindow(Window a_window, Window a_parent)
	{
		if (a_window == null || a_parent == null)
		{
			return;
		}
		Dimension parentSize = a_parent.getSize();
		Dimension ownSize = a_window.getSize();
		Point parentLocation = a_parent.getLocationOnScreen();
		a_window.setLocation(parentLocation.x + (parentSize.width / 2) - (ownSize.width / 2),
							 parentLocation.y + 40);
    }

	/**
	 * Moves the given Window to the upright corner of the default screen.
	 * @param a_window a Window
	 */
	public static void moveToUpRightCorner(Window a_window)
	{
		Screen currentScreen =  getCurrentScreen(a_window);
		Dimension ownSize = a_window.getSize();
		a_window.setLocation(currentScreen.getX() + (currentScreen.getWidth() - ownSize.width),
							 currentScreen.getY());
	}

	public static void setNativeGUILibrary(NativeGUILibrary a_library)
	{
		if (a_library != null)
		{
			ms_nativeGUILibrary = a_library;
		}
	}

	public static boolean isAlwaysOnTop(Component a_component)
	{
		return isAlwaysOnTop(getParentWindow(a_component));
	}

	/**
	 * Returns if the alwaysOnTop method of JRE 1.5 is set on a given Window.
	 * @param a_Window a Window
	 * @return if the alwaysOnTop method of JRE 1.5 is set on a given Window
	 */
	public static boolean isAlwaysOnTop(Window a_Window)
	{
		if (a_Window == null)
		{
			return false;
		}

		try
		{
			Method m = Window.class.getMethod("isAlwaysOnTop", new Class[0]);
			return ( (Boolean) m.invoke(a_Window, new Object[0])).booleanValue();
		}
		catch (Throwable t)
		{
		}
		return ms_nativeGUILibrary.isAlwaysOnTop(a_Window);
	}

	public static void setFontStyle(Component a_component, int a_style)
	{
		if (a_component == null)
		{
			return;
		}
		a_component.setFont(
			  new Font(a_component.getFont().getName(), a_style, a_component.getFont().getSize()));
	}

	public static boolean setAlwaysOnTop(Component a_component, boolean a_bOnTop)
	{
		return setAlwaysOnTop(getParentWindow(a_component), a_bOnTop);
	}

	public static boolean hasJavaOnTop()
	{
		try
		{
			Class[] c = new Class[1];
			c[0] = boolean.class;
			Window.class.getMethod("setAlwaysOnTop", c);
		}
		catch (NoSuchMethodException a_e)
		{
			return false;
		}
		return true;
	}

	/**
	 * Tries to use the method setAlwaysOnTop of JRE 1.5.
	 * @param a_Window Window
	 * @param a_bOnTop boolean
	 * @return if the method setAlwaysOnTop could be called with the given arguments
	 */
	public static boolean setAlwaysOnTop(Window a_Window, boolean a_bOnTop)
	{
		if (a_Window == null)
		{
			return false;
		}

		try
		{
			Class[] c = new Class[1];
			c[0] = boolean.class;
			Method m = Window.class.getMethod("setAlwaysOnTop", c);
			Object[] args = new Object[1];
			args[0] = new Boolean(a_bOnTop);
			m.invoke(a_Window, args);
			return true;
		}
		catch (Throwable t)
		{
		}
		return ms_nativeGUILibrary.setAlwaysOnTop(a_Window, a_bOnTop);
	}

	public static class WindowDocker
	{
		private JobQueue m_queue;
		private Component m_component;
		private InternalListener m_listener;
		private Window m_parentWindow;

		public WindowDocker(Component a_window)
		{
			m_component = a_window;
			m_listener = new InternalListener();
			m_component.addMouseListener(m_listener);
			m_component.addMouseMotionListener(m_listener);
			//m_component.addComponentListener(m_listener);
			m_parentWindow = GUIUtils.getParentWindow(a_window);
			m_queue = new JobQueue("Docking queue for window: " + a_window.getName());

		}

		public void finalize()
		{
			m_queue.stop();
			m_queue = null;
			m_component.removeMouseListener(m_listener);
			m_component.removeMouseMotionListener(m_listener);
			m_component.removeComponentListener(m_listener);
			m_listener = null;
		}

		/**
		 * This needs to be an interface for compilation in JDK 1.1.8.
		 */
		private interface IDockInterface
		{
			static final int DOCK_DISTANCE = 10;
		}


		private class InternalListener extends MouseAdapter implements MouseMotionListener, ComponentListener,
			IDockInterface
		{

			private boolean m_bIsDragging = false;
			private Point m_startPoint;
			private final Object SYNC = new Object();

			public void componentHidden(ComponentEvent a_event)
			{
			}

			public void componentResized(ComponentEvent a_event)
			{
			}

			public void componentShown(ComponentEvent a_event)
			{
			}

			public void componentMoved(ComponentEvent a_event)
			{
				if (!m_bIsDragging)
				{
					move(null);
				}
			}

			public void mouseReleased(MouseEvent e)
			{
				synchronized (SYNC)
				{
					m_bIsDragging = false;
				}
			}

			public void mouseMoved(MouseEvent e)
			{
			}

			public void mouseDragged(MouseEvent e)
			{
				synchronized (SYNC)
				{
					if (!m_bIsDragging)
					{
						m_bIsDragging = true;
						m_startPoint = e.getPoint();
					}
					else
					{
						Point endPoint = e.getPoint();
						Point aktLocation = m_parentWindow.getLocationOnScreen();
						int x, y;

						x = aktLocation.x + endPoint.x - m_startPoint.x;
						y = aktLocation.y + endPoint.y - m_startPoint.y;
						move(new Point(x, y));
					}
				}
			}

			private void move(final Point a_location)
			{
				m_queue.addJob(new JobQueue.Job()
				{
					public void runJob()
					{
						//synchronized (SYNC)
						{
							GUIUtils.Screen currentScreen = GUIUtils.getCurrentScreen(m_parentWindow);
							int x, y, maxX, maxY;
							boolean bMove = a_location != null;
							Point location = a_location;
							if (location == null)
							{
								location = m_parentWindow.getLocationOnScreen();
							}

							x = location.x;
							y = location.y;

							maxX = (int) currentScreen.getWidth() + currentScreen.getX();
							maxY = (int) currentScreen.getHeight() + currentScreen.getY();
							if (x != currentScreen.getX() && Math.abs(x - currentScreen.getX()) < (DOCK_DISTANCE))
							{
								bMove = true;
								x = currentScreen.getX();
							}
							else if (x + m_parentWindow.getSize().width  > maxX - DOCK_DISTANCE &&
									 ! (x + m_parentWindow.getSize().width > maxX + DOCK_DISTANCE))
							{
								bMove = true;
								x = maxX - m_parentWindow.getSize().width;
							}
							//System.out.println(currentScreen.getWidth() + ":" + (x + m_parentWindow.getSize().width) + ":" + (maxX + DOCK_DISTANCE));

							if (y != currentScreen.getY() && Math.abs(y - currentScreen.getY()) < (DOCK_DISTANCE))
							{
								bMove = true;
								y = currentScreen.getY();
							}
							else if (y + m_parentWindow.getSize().height > maxY - DOCK_DISTANCE &&
									 ! (y + m_parentWindow.getSize().height > maxY + DOCK_DISTANCE))
							{
								bMove = true;
								y = maxY - m_parentWindow.getSize().height;
							}
							if (bMove)
							{
								m_parentWindow.setLocation(x, y);
							}
						}
					}
				});
			}
		}
	}

	/**
	 * Sets a window to the specified size and tries to put the window inside the screen by altering
	 * the size if needed. The location of the window should be set before.
	 * @param a_window Window
	 * @param a_size Dimension
	 * @return if the size has been changed
	 */
	public static boolean restoreSize(Window a_window, Dimension a_size)
	{
		if (a_window == null || a_size == null)
		{
			return false;
		}
		a_window.setSize(a_size);
		Screen currentScreen = getCurrentScreen(a_window);

		int width = a_window.getSize().width;
		int height = a_window.getSize().height;

		if ((a_window.getLocation().x + width) > (currentScreen.getX() + currentScreen.getWidth()))
		{
			width = currentScreen.getX() + currentScreen.getWidth() - a_window.getLocation().x;
		}
		if ((a_window.getLocation().y + height) > (currentScreen.getY() + currentScreen.getHeight()))
		{
			height = currentScreen.getY() + currentScreen.getHeight() - a_window.getLocation().y;
		}

		if (width == 0)
		{
			width =  a_window.getSize().width;
		}
		if (height == 0)
		{
			height = a_window.getSize().height;
		}

		a_window.setSize(width, height);
		return true;
	}

	public static Point getMiddlePoint(Window a_window)
	{
		if (a_window == null)
		{
			return new Point(0, 0);
		}
		return new Point (a_window.getLocation().x + (a_window.getSize().width / 2),
						  a_window.getLocation().y + (a_window.getSize().height / 2));
	}

	/**
	 * Sets a window to the specified position and tries to put the window inside the screen by altering
	 * the position if needed.
	 * @param a_window Window
	 * @param a_location Point
	 * @return if the location has been changed
	 */
	public static boolean restoreLocation(Window a_window, Point a_location)
	{
		if (a_window == null || a_location == null)
		{
			return false;
		}

		double bestArea = -1.0;
		double bestDistance = Double.MAX_VALUE;
		double currentDistanceVector;
		Screen currentScreen = null;
		a_window.setLocation(a_location);
		Point windowCenter = getMiddlePoint(a_window);
		Screen[] screens = getScreens(a_window);
		int x = a_location.x;
		int width = a_window.getSize().width;
		int y = a_location.y;
		int height = a_window.getSize().height;
		boolean bLeftDown, bLeftUp, bRightDown, bRightUp;
		int area, areaX1, areaX2, areaY1, areaY2;

		if (screens.length == 0)
		{
			return false;
		}

		/* Find the screen that contains the middle point of the window or, if such a screen does
		 * not exist, most of the window's area.
		 */
		for (int i = 0; i < screens.length; i++)
		{
			// check first if point is contained in the screen
			if (windowCenter.x >= screens[i].getX() && windowCenter.y >= screens[i].getY() &&
				windowCenter.x <= (screens[i].getX() + screens[i].getWidth()) &&
				windowCenter.y <= (screens[i].getY() + screens[i].getHeight()))
			{
				// the window lies in this screen
				currentScreen = screens[i];
				break;
			}

			// look if part of the window lies in this screen; get the corners
			bLeftUp = x >= screens[i].getX() && x <= (screens[i].getX() + screens[i].getWidth()) &&
				y >= screens[i].getY() && y <= (screens[i].getY() + screens[i].getHeight());
			bRightUp = x + width >= screens[i].getX() &&
				x + width <= (screens[i].getX() + screens[i].getWidth()) &&
				y >= screens[i].getY() && y <= (screens[i].getY() + screens[i].getHeight());
			bLeftDown = y + height >= screens[i].getY() &&
				y + height <= (screens[i].getY() + screens[i].getHeight()) &&
				x >= screens[i].getX() && x <= (screens[i].getX() + screens[i].getWidth());
			bRightDown = y + height >= screens[i].getY() && x + width >= screens[i].getX() &&
				y + height <= (screens[i].getY() + screens[i].getHeight()) &&
				x + width <= (screens[i].getX() + screens[i].getWidth());

			if (!bLeftUp && !bRightUp && !bLeftDown && !bRightDown)
			{
				// no area of the window lies in this screen
				continue;
			}

			// calculate the area
			if (bLeftUp || bLeftDown)
			{
				areaX1 = x;
			}
			else
			{
				areaX1 = screens[i].getX();
			}

			if (bRightUp || bRightDown)
			{
				areaX2 = x + width;
			}
			else
			{
				areaX2 = screens[i].getX() + screens[i].getWidth();
			}

			if (bLeftUp || bRightUp)
			{
				areaY1 = y;
			}
			else
			{
				areaY1 = screens[i].getY();
			}

			if (bLeftDown || bRightDown)
			{
				areaY2 = y + height;
			}
			else
			{
				areaY2 = screens[i].getY() + screens[i].getHeight();
			}

			area =  (areaX2 - areaX1) * (areaY2  - areaY1);
			LogHolder.log(LogLevel.INFO, LogType.GUI,
						  "Calculated partial overlapping area for restoring window location: " + area);
			if (area >= bestArea)
			{
				bestArea = area;
				currentScreen = screens[i];
			}
		}

		// if no screen with an overlapping area was found, take the one with the shortest distance to middle
		if (currentScreen == null)
		{
			Point screenCenter;
			for (int i = 0; i < screens.length; i++)
			{
				screenCenter = new Point (screens[i].getX() + (screens[i].getWidth() / 2),
										  screens[i].getY() + (screens[i].getHeight() / 2));
				currentDistanceVector = Math.sqrt(Math.pow(windowCenter.x - screenCenter.x, 2.0) +
												  Math.pow(windowCenter.y - screenCenter.y, 2.0));
				LogHolder.log(LogLevel.INFO, LogType.GUI,
						  "Calculated distance vector for restoring window location: " +
						  currentDistanceVector);
				if (currentDistanceVector < bestDistance)
				{
					currentScreen = screens[i];
					bestDistance = currentDistanceVector;
				}
			}
		}
		LogHolder.log(LogLevel.NOTICE, LogType.GUI,
					  "The following screen was chosen for restoring a window location:\n" + currentScreen);



		if ((x + a_window.getSize().width) > (currentScreen.getX() + currentScreen.getWidth()))
		{
			x = currentScreen.getX() +  currentScreen.getWidth() - a_window.getSize().width;
		}
		if ((y + a_window.getSize().height) > (currentScreen.getY() + currentScreen.getHeight()))
		{
			y = currentScreen.getY() + currentScreen.getHeight() - a_window.getSize().height;
		}

		if (x < currentScreen.getX())
		{
			x = currentScreen.getX();
		}
		if (y < currentScreen.getY())
		{
			y = currentScreen.getY();
		}

		a_window.setLocation(x, y);
		return true;
	}


	public static MouseListener addTimedTooltipListener(JComponent c)
	{
		Object imap;
		ToolTipMouseListener listener;
		Class classInputMap;

		try
		{
			//ensure InputMap and ActionMap are created
			classInputMap = Class.forName("javax.swing.InputMap");
			imap = JComponent.class.getMethod("getInputMap", new Class[0]).invoke(c, new Object[0]);
			JComponent.class.getMethod("getActionMap", new Class[0]).invoke(c, new Object[0]);

			//put dummy KeyStroke into InputMap if is empty:
			boolean removeKeyStroke = false;
			KeyStroke[] ks =
				(KeyStroke[]) (classInputMap.getMethod("keys", new Class[0]).invoke(imap, new Object[0]));
			//KeyStroke[] ks = imap.keys();
			if (ks == null || ks.length == 0)
			{
				classInputMap.getMethod("put", new Class[]
										{KeyStroke.class, Object.class}).invoke(imap, new Object[]
					{KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SLASH, 0), "backSlash"});
				//imap.put(KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SLASH, 0), "backSlash");
				removeKeyStroke = true;
			}
			//now we can register by ToolTipManager
			ToolTipManager.sharedInstance().registerComponent(c);
			//and remove dummy KeyStroke
			if (removeKeyStroke)
			{
				classInputMap.getMethod("remove", new Class[]
										{KeyStroke.class}).invoke(imap, new Object[]
					{KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SLASH, 0)});
				//imap.remove(KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SLASH, 0));
			}
			//now last part - add appropriate MouseListener and
			//hear to mouseEntered events
			listener = new ToolTipMouseListener();
			c.addMouseListener(listener);
			return listener;
		}
		catch (Exception a_e)
		{
			// JDK is too old
			LogHolder.log(LogLevel.NOTICE, LogType.GUI,
						  "Could not register component for timed tooltip!", a_e);
			return null;
		}

	}

	public static interface AWTEventListener
	{
		public void eventDispatched(AWTEvent event);
	}

	public static class Screen
	{
		private Point m_location;
		private Rectangle m_bounds;

		public Screen(Point a_location, Rectangle a_bounds)
		{
			m_location = a_location;
			m_bounds = a_bounds;
		}

		public int getX()
		{
			return m_location.x;
		}

		public int getY()
		{
			return m_location.y;
		}

		public int getWidth()
		{
			return m_bounds.width;
		}

		public int getHeight()
		{
			return m_bounds.height;
		}

		public Point getLocation()
		{
			return m_location;
		}
		public Rectangle getBounds()
		{
			return m_bounds;
		}
		public String toString()
		{
			return "x=" + getX() + " " + "y=" + getY() + " " + "width=" + getWidth() + " " +
				"height=" + getHeight();
		}

	}

	public static Screen[] getScreens(Window a_window)
	{
		Screen[] screens;
		Object graphicsConfiguration;
		Frame screenFrame;
		Rectangle screenBounds;
		Point screenLocation;

		try
		{
			Object graphicsEnvironment =
				Class.forName("java.awt.GraphicsEnvironment").getMethod(
					"getLocalGraphicsEnvironment", null).invoke(null, null);
			Object[] graphicsDevices = (Object[]) graphicsEnvironment.getClass().getMethod(
				"getScreenDevices", null).invoke(graphicsEnvironment, null);
			screens = new Screen[graphicsDevices.length];
			for (int i = 0; i < graphicsDevices.length; i++)
			{
				graphicsConfiguration = graphicsDevices[i].getClass().getMethod(
					"getDefaultConfiguration", null).invoke(graphicsDevices[i], null);
				screenFrame = (Frame) Frame.class.getConstructor(
					new Class[]
					{Class.forName("java.awt.GraphicsConfiguration")}).newInstance(
						new Object[]
						{graphicsConfiguration});
				screens[i] = new Screen(screenFrame.getLocation(),
										(Rectangle) graphicsConfiguration.getClass().getMethod(
											"getBounds", null).invoke(graphicsConfiguration, null));
			}
			return screens;
		}
		catch (Exception a_e)
		{
			// ignore
		}
		// for JDKs < 1.3
		return new Screen[]
			{
			new Screen(new Point(0, 0), getDefaultScreenBounds(a_window))};
	}

	public static Screen getCurrentScreen(Component a_component)
	{
		return getCurrentScreen(getParentWindow(a_component));
	}

	public static Screen getCurrentScreen(Window a_window)
	{
		if (a_window == null)
		{
			return null;
		}

		try
		{
			Object graphicsConfiguration;
			Frame screenFrame;
			Point windowMiddleLocation;
			Rectangle screenBounds;
			Point screenLocation;
			Object graphicsEnvironment =
				Class.forName("java.awt.GraphicsEnvironment").getMethod(
								"getLocalGraphicsEnvironment", null).invoke(null, null);
			Object[] graphicsDevices = (Object[])graphicsEnvironment.getClass().getMethod(
						 "getScreenDevices", null).invoke(graphicsEnvironment, null);

			// now look on which srceen the middle of the window is located
			windowMiddleLocation = getMiddlePoint(a_window);
			for (int i = 0; i < graphicsDevices.length; i++)
			{
				graphicsConfiguration = graphicsDevices[i].getClass().getMethod(
								"getDefaultConfiguration", null).invoke(graphicsDevices[i], null);
				screenFrame = (Frame)Frame.class.getConstructor(
								new Class[]{Class.forName("java.awt.GraphicsConfiguration")}).newInstance(
					new Object[]{graphicsConfiguration});
				screenLocation = screenFrame.getLocation();
				screenBounds = (Rectangle)graphicsConfiguration.getClass().getMethod(
								"getBounds", null).invoke(graphicsConfiguration, null);

				if (windowMiddleLocation.x >= screenLocation.x &&
					windowMiddleLocation.x <= (screenLocation.x + screenBounds.width) &&
					windowMiddleLocation.y >= screenLocation.y &&
					windowMiddleLocation.y <= (screenLocation.y + screenBounds.height))
				{
					// if this screen overlaps with the default screen, take the smallest common part
					return getOverlappingScreen(new Screen(screenLocation, screenBounds), a_window);
				}
			}
		}
		catch (Exception a_e)
		{
			// ignore
		}

		// for JDKs < 1.3
		return new Screen(new Point(0,0), getDefaultScreenBounds(a_window));
	}

	/**
	 * Centers a window relative to the default screen.
	 * @param a_window a Window
	 */
	public static void centerOnScreen(Window a_window)
	{
		Rectangle screenBounds = getDefaultScreenBounds(a_window);
		Dimension ownSize = a_window.getSize();

		a_window.setLocation(screenBounds.x + ((screenBounds.width - ownSize.width) / 2),
							 screenBounds.y + ((screenBounds.height - ownSize.height) / 2));
	}

	/**
	 * Positions a window on the screen centered to a parent window.
	 * @param a_window a Window
	 * @param a_parent the Window's parent window
	 */
	public static void centerOnWindow(Window a_window, Window a_parent)
	{
		if (a_window == null || a_parent == null)
		{
			return;
		}
		Dimension parentSize = a_parent.getSize();
		Dimension ownSize = a_window.getSize();
		Point parentLocation = a_parent.getLocationOnScreen();
		a_window.setLocation(parentLocation.x + (parentSize.width / 2) - (ownSize.width / 2),
							 parentLocation.y + (parentSize.height / 2) - (ownSize.height / 2));
	}

	/**
	 * Creates a JTextPane that may be used to simulate a selectable and resizeable JLabel.
	 * If you do not want the label to be selectable, you may set <i>enabled<i> to <i>false<i>.
	 * @param a_parent Component
	 * @return JTextPane
	 */
	public static JTextPane createSelectableAndResizeableLabel(Component a_parent)
	{
		Font jlFont;
		JTextPane selectableLabel = new JTextPane();
		selectableLabel.setBackground(a_parent.getBackground());
		selectableLabel.setEditable(false);
		selectableLabel.setDisabledTextColor(selectableLabel.getCaretColor());
		jlFont = new JLabel().getFont();
		selectableLabel.setFont(new Font(jlFont.getName(),Font.BOLD, jlFont.getSize()));
		return selectableLabel;
	}

	/**
	 * Tests which mouse button was the cause for the specified MouseEvent.
	 * Use the button masks from MouseEvent.
	 * @param a_event a MouseEvent
	 * @param a_buttonMask a button mask from MouseEvent
	 * @return if the event was triggered by the given mouse button
	 * @see java.awt.event.MouseEvent
	 */
	public static boolean isMouseButton(MouseEvent a_event, int a_buttonMask)
	{
		return ((a_event.getModifiers() & a_buttonMask) == a_buttonMask);
	}

	/**
	 * Returns the system-wide clipboard.
	 * @return the system-wide clipboard Clipboard
	 */
	public static Clipboard getSystemClipboard()
	{
		Clipboard r_cb = null;

		try
		{
			Method getSystemSelection = Toolkit.class.getMethod("getSystemSelection", new Class[0]);
			r_cb = (Clipboard) getSystemSelection.invoke(Toolkit.getDefaultToolkit(), new Object[0]);
		}
		catch (NoSuchMethodException nsme)
		{
			// JDK < 1.4 does not support getSystemSelection
		}
		catch (IllegalAccessException iae)
		{
			// this should not happen
		}
		catch (InvocationTargetException ite)
		{
			// this should not happen
		}

		// alternate way of retrieving the clipboard
		if (r_cb == null)
		{
			r_cb = Toolkit.getDefaultToolkit().getSystemClipboard();
		}
		return r_cb;
	}



	/**
	 * Registers all instanciable subclasses of javax.swing.LookAndFeel from a file in the UIManager.
	 * @return the files that contain the newly loaded look&feel classes
	 */
	public static Vector registerLookAndFeelClasses(File a_file) throws IllegalAccessException
	{
		if (a_file == null)
		{
			return new Vector();
		}

		LookAndFeelInfo lnfOldInfo[] = UIManager.getInstalledLookAndFeels();
		LookAndFeelInfo lnfNewInfo[];
		LookAndFeel lnf;
		Vector oldFiles = new Vector(lnfOldInfo.length);
		Vector newFiles;
		File file;
		for (int i = 0; i < lnfOldInfo.length; i++)
		{
			file = ClassUtil.getClassDirectory(lnfOldInfo[i].getClassName());
			if (file != null)
			{
				oldFiles.addElement(file);
			}
		}

		ClassUtil.addFileToClasspath(a_file);
		ClassUtil.loadClasses(a_file);

		Vector tempLnfClasses = ClassUtil.findSubclasses(LookAndFeel.class);
		for (int i = 0; i < tempLnfClasses.size(); i++)
		{
			try
			{
				lnf = (LookAndFeel)( (Class) tempLnfClasses.elementAt(i)).newInstance();
			}
			catch (IllegalAccessException ex)
			{
				continue;
			}
			catch (InstantiationException ex)
			{
				continue;
			}
			catch (ClassCastException a_e)
			{
				continue;
			}
			try
			{

				if (lnf.isSupportedLookAndFeel())
				{
					LookAndFeelInfo installed[] = UIManager.getInstalledLookAndFeels();
					boolean bInstalled = false;
					for (int j = 0; j < installed.length; j++)
					{
						if (installed[j].getClassName().equals(lnf.getClass().getName()))
						{
							// this theme has been previously installed
							bInstalled = true;
						}
					}
					if (!bInstalled)
					{
						UIManager.installLookAndFeel(lnf.getName(), lnf.getClass().getName());
					}
				}
			}
			catch (Throwable a_e)
			{
				continue;
			}
		}
		lnfNewInfo = UIManager.getInstalledLookAndFeels();
		if (lnfNewInfo.length > lnfOldInfo.length)
		{
			newFiles = new Vector(lnfNewInfo.length - lnfOldInfo.length);
			for (int i = 0; i < lnfNewInfo.length; i++)
			{
				file = ClassUtil.getClassDirectory(lnfNewInfo[i].getClassName());
				if (!oldFiles.contains(file))
				{
					newFiles.addElement(file);
				}
			}
		}
		else
		{
			newFiles = new Vector();
		}
		return newFiles;
	}

	/**
	 * Resizes all fonts of the UIManager by a fixed factor.
	 * @param a_resize  the factor to resize the fonts
	 */
	public static void resizeAllFonts(float a_resize)
	{
		java.util.Enumeration keys = UIManager.getDefaults().keys();
		while (keys.hasMoreElements())
		{
			Object key = keys.nextElement();
			Object value = UIManager.get (key);
			if (value instanceof FontUIResource)
			{
				adjustFontSize(key.toString(), a_resize);
			}
		}
	}

	public static String getTextFromClipboard(Component a_requestingComponent)
	{
		return getTextFromClipboard(a_requestingComponent, true);
	}

	public static void saveTextToClipboard(String strText, Component a_requestingComponent)
	{
		try
		{
			Clipboard cb = GUIUtils.getSystemClipboard();
			cb.setContents(new StringSelection(strText),
						   new ClipboardOwner()
			{
				public void lostOwnership(Clipboard cb, Transferable co)
				{
					// Don't care.
				}
			});

			if (strText.equals(getTextFromClipboard(a_requestingComponent, false)))
			{
				JAPDialog.showMessageDialog(a_requestingComponent, JAPMessages.getString(MSG_SAVED_TO_CLIP));
				return;
			}
		}
		catch (Exception e)
		{
			LogHolder.log(LogLevel.NOTICE, LogType.GUI, e);
		}

		// There are some problems with the access of the
		// clipboard, so after the try to copy it, we
		// still offer the ClipFrame.
		ClipFrame cf =
			new ClipFrame(a_requestingComponent, JAPMessages.getString(MSG_COPY_FROM_CLIP), false);
		cf.setText(strText);
		cf.setVisible(true, false);
	}

	public static ImageIcon createScaledImageIcon(ImageIcon a_icon, IIconResizer a_resizer)
	{
		if (a_icon == null)
		{
			return null;
		}
		if (a_resizer == null)
		{
			return a_icon;
		}
		return  new ImageIcon(a_icon.getImage().getScaledInstance(
			  (int) (a_icon.getIconWidth() * a_resizer.getResizeFactor()), -1, Image.SCALE_REPLICATE));
	}

	public static Icon createScaledIcon(Icon a_icon, IIconResizer a_resizer)
	{
		if (a_icon == null)
		{
			return a_icon;
		}

		return new IconScaler(a_icon, a_resizer.getResizeFactor());
	}

	/**
	 * Shortens a text received from the IS or in a certificate so that it is not to long to display.
	 * @param a_strOriginal String
	 * @param a_maximumLength the maximum length that is displayed
	 * @return the stripped text
	 */
	public static String trim(String a_strOriginal, int a_maximumLength)
	{
		if (a_strOriginal == null || a_maximumLength < 4)
		{
			return null;
		}
		// remove all html TAGS
		a_strOriginal = JAPHtmlMultiLineLabel.removeTagsAndNewLines(a_strOriginal);
		if (a_strOriginal.length() > a_maximumLength)
		{
			a_strOriginal = a_strOriginal.substring(0, a_maximumLength - 2) + "...";
		}
		return a_strOriginal;
	}

	/**
	 * Shortens a text received from the IS or in a certificate so that it is not to long to display.
	 * @param a_strOriginal String
	 * @return the stripped text
	 */
	public static String trim(String a_strOriginal)
	{
		return trim(a_strOriginal, MAXIMUM_TEXT_LENGTH);
	}

	public static void addAWTEventListener(AWTEventListener a_listener)
	{
		synchronized (AWT_EVENT_LISTENERS)
		{
			if (!ms_bCapturingAWTEvents)
			{
				Runnable run = new Runnable()
				{
					public void run()
					{
						SwingUtilities.invokeLater(new Runnable()
						{
							public void run()
							{
								EventQueue theQueue = Toolkit.getDefaultToolkit().getSystemEventQueue();
								try
								{
									while (!JAPDialog.isConsoleOnly())
									{
										AWTEvent event = theQueue.getNextEvent();

										Class classActiveEvent;
										try
										{
											// java.awt.ActiveEvent is not known in JDKs < 1.2
											classActiveEvent = Class.forName("java.awt.ActiveEvent");
										}
										catch (ClassNotFoundException a_e)
										{
											classActiveEvent = null;
										}
										if (classActiveEvent != null && classActiveEvent.isInstance(event))
										{
											// ((ActiveEvent) event).dispatch();
											classActiveEvent.getMethod("dispatch", null).invoke(event, null);
										}
										else if (event.getSource() instanceof Component)
										{
											try
											{
												( (Component) event.getSource()).dispatchEvent(event);
											}
											catch (IllegalMonitorStateException a_e)
											{
												LogHolder.log(LogLevel.NOTICE, LogType.GUI, a_e);
											}
										}
										else if (event.getSource() instanceof MenuComponent)
										{
											( (MenuComponent) event.getSource()).dispatchEvent(event);
										}

										synchronized (AWT_EVENT_LISTENERS)
										{
											for (int i = 0; i < AWT_EVENT_LISTENERS.size(); i++)
											{
												((AWTEventListener)AWT_EVENT_LISTENERS.elementAt(i)).
													eventDispatched(event);
											}
										}

										Thread.yield();
									}
								}
								catch (Exception a_e)
								{
									LogHolder.log(LogLevel.EXCEPTION, LogType.GUI, a_e);
								}
							}
						});
					}
				};

				if (SwingUtilities.isEventDispatchThread())
				{
					new Thread(run).start();
				}
				else
				{
					run.run();
				}

				/*
				  Toolkit.getDefaultToolkit().addAWTEventListener(new AWTEventListener()
				  {
				 public void eventDispatched(AWTEvent a_event)
				 {
				  MouseEvent event =  (MouseEvent) a_event;
				  if (event.getSource() != null && event.getSource() instanceof Component)
				  {
				   Component component = (Component)event.getSource();
				   synchronized (SYNC_MOUSE_POSITION)
				   {
				 try
				 {
				  ms_mousePosition = component.getLocationOnScreen();
				  ms_mousePosition.x += event.getX();
				  ms_mousePosition.y += event.getY();
				 }
				 catch (IllegalComponentStateException a_e)
				 {
				  // ignore
				 }
				   }
				  }
				 }
				  }, AWTEvent.MOUSE_EVENT_MASK);
				 */
				ms_bCapturingAWTEvents = true;
			}

			if (a_listener == null)
			{
				return;
			}


			if (!AWT_EVENT_LISTENERS.contains(a_listener))
			{
				AWT_EVENT_LISTENERS.addElement(a_listener);
			}
		}
	}

	public static void removeAWTEventListener(AWTEventListener a_listener)
	{
		AWT_EVENT_LISTENERS.removeElement(a_listener);
	}



	/**
	 * Returns the current mouse position on the screen.
	 * @return the current mouse position on the screen or null if the mouse position is unknown
	 */
	public static Point getMousePosition()
	{
		synchronized (SYNC_MOUSE_POSITION)
		{
			if (ms_mouseListener == null)
			{
				ms_mouseListener = new AWTEventListener()
				{
					public void eventDispatched(AWTEvent a_event)
					{
						if (a_event instanceof MouseEvent)
						{
							MouseEvent mouseEvent = (MouseEvent) a_event;
							if (a_event.getSource() != null && a_event.getSource() instanceof Component)
							{
								Component component = (Component) a_event.getSource();
								try
								{
									synchronized (SYNC_MOUSE_POSITION)
									{
										ms_mousePosition = component.getLocationOnScreen();
										ms_mousePosition.x += mouseEvent.getX();
										ms_mousePosition.y += mouseEvent.getY();
									}
								}
								catch (IllegalComponentStateException a_e)
								{
									// ignore
								}
							}
						}
					}
				};
				addAWTEventListener(ms_mouseListener);
			}
		}
		if (ms_mousePosition == null)
		{
			return null;
		}
		return new Point(ms_mousePosition.x, ms_mousePosition.y);
	}

	public static Point getRelativePosition(Point a_positionOnScreen, Component a_component)
	{
		if (a_positionOnScreen == null || a_component == null)
		{
			return null;
		}
		Point currentPoint = a_positionOnScreen;
		if (currentPoint == null)
		{
			return null;
		}

		Point componentPoint;
		try
		{
			componentPoint = a_component.getLocationOnScreen();
		}
		catch (IllegalComponentStateException a_e)
		{
			componentPoint = a_component.getLocation();
			//return null;
		}


		if (currentPoint.x < componentPoint.x - 1 ||
			currentPoint.x > componentPoint.x + a_component.getSize().width + 1 ||
			currentPoint.y < componentPoint.y - 1 ||
			currentPoint.y > componentPoint.y + a_component.getSize().height + 1)
		{
			return null;
		}

		currentPoint.x -= componentPoint.x;
		currentPoint.y -= componentPoint.y;

		return currentPoint;
	}

	public static Point getMousePosition(Component a_component)
	{
		return getRelativePosition(getMousePosition(), a_component);
	}

	/**
	 * Checks if a screen overlaps with the default screen and trims the screen area if needed.
	 * @param a_screen Screen
	 * @param a_window Window
	 * @return Screen
	 */
	private static Screen getOverlappingScreen(Screen a_screen, Window a_window)
	{
		if (a_screen == null)
		{
			return null;
		}

		Screen defaultScreen = new Screen(new Point(0, 0), getDefaultScreenBounds(a_window));
		if (defaultScreen.getX() == a_screen.getX() &&
			defaultScreen.getY() == a_screen.getY() &&
			defaultScreen.getWidth() == a_screen.getWidth() &&
			defaultScreen.getHeight() == a_screen.getHeight())
		{
			// these are the same screens
			return a_screen;
		}

		int x = a_screen.getX();
		int y = a_screen.getY();
		int width = a_screen.getWidth();
		int height = a_screen.getHeight();
		boolean bOverlap = false;

		if ((a_screen.getY() < defaultScreen.getY() &&
			 a_screen.getY() + a_screen.getHeight() > defaultScreen.getY()) ||
			(defaultScreen.getY() < a_screen.getY()) &&
			defaultScreen.getY() + defaultScreen.getHeight() > a_screen.getY())
		{
			// height is overlapping; get the minimum overlapping area as screen
			bOverlap = true;
			LogHolder.log(LogLevel.NOTICE, LogType.GUI,
						  "Found overlapping screen.");
			y = Math.max(a_screen.getY(), defaultScreen.getY());
			height = Math.min(a_screen.getY() + a_screen.getHeight(),
							  defaultScreen.getY() + defaultScreen.getHeight() -
							  Math.abs(a_screen.getY() - defaultScreen.getY()));
		}

		if ((a_screen.getX() < defaultScreen.getX() &&
			 a_screen.getX() + a_screen.getWidth() > defaultScreen.getX()) ||
			(defaultScreen.getX() < a_screen.getX() &&
			defaultScreen.getX() + defaultScreen.getWidth() > a_screen.getX()))
		{
			// width is overlapping; get the minimum overlapping area as screen
			bOverlap = true;

			x = Math.max(a_screen.getX(), defaultScreen.getX());
			width = Math.min(a_screen.getX() + a_screen.getWidth(),
							 defaultScreen.getX() + defaultScreen.getWidth() -
							 Math.abs(a_screen.getX() - defaultScreen.getX()));
		}

		if (bOverlap)
		{
			a_screen = new Screen(new Point(x, y), new Rectangle(width, height));
		}
		return a_screen;
	}

	private static class ToolTipMouseListener extends MouseAdapter
	{
		public void mouseEntered(MouseEvent e)
		{
			if (!(e.getComponent() instanceof JComponent))
			{
				return;
			}

			JComponent c = (JComponent) e.getComponent();
			Action action = null;
			try
			{
				Class classActionMap = Class.forName("javax.swing.ActionMap");
				Object map =  JComponent.class.getMethod(
								"getActionMap", new Class[0]).invoke(c, new Object[0]);
				action = (Action)(classActionMap.getMethod("get", new Class[]{Object.class}).invoke(
								map, new Object[]{"postTip"}));
			}
			catch (Exception a_e)
			{
				// Should not happen!
				LogHolder.log(LogLevel.EXCEPTION, LogType.GUI, a_e);
			}

			//it is also possible to use own Timer to display
			//ToolTip with custom delay, but here we just
			//display it immediately
			if (action != null)
			{
				action.actionPerformed(new ActionEvent(c, ActionEvent.ACTION_PERFORMED, "postTip"));
			}
		}
	}

	private static String getTextFromClipboard(Component a_requestingComponent, boolean a_bUseTextArea)
	{
		Clipboard cb = getSystemClipboard();
		String strText = null;

		Transferable data = cb.getContents(a_requestingComponent);
		if (data != null && data.isDataFlavorSupported(DataFlavor.stringFlavor))
		{
			try
			{
				strText = (String) data.getTransferData(DataFlavor.stringFlavor);
			}
			catch (Exception a_e)
			{
				LogHolder.log(LogLevel.NOTICE, LogType.GUI, a_e);
			}
		}

		if (a_bUseTextArea && strText == null)
		{
			ClipFrame cf =
				new ClipFrame(a_requestingComponent, JAPMessages.getString(MSG_PASTE_FILE), true);
			cf.setVisible(true, false);
			strText = cf.getText();
		}
		return strText;
	}

	/**
	 * Returns the bounds of the default screen. This method is private as is does not always return
	 * the right coordinates and screen bounds.
	 * @param a_window a Window
	 * @return the bounds of the default screen
	 */
	private static Rectangle getDefaultScreenBounds(Window a_window)
	{
		if (a_window == null)
		{
			return null;
		}

		Rectangle screenBounds;

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
		return screenBounds;
	}

	/**
	 * Resizes a specific default font of the UIManager by a fixed factor.
	 * @param a_fontObject a UIManager font object
	 * @param a_resize the factor to resize the given font
	 */
	private static void adjustFontSize(Object a_fontObject, float a_resize)
	{
		try
		{
			UIDefaults defaults = UIManager.getDefaults();
			Font font = defaults.getFont(a_fontObject);
			//defaults.put(a_fontObject, new FontUIResource(font.deriveFont(font.getSize() * a_resize)));
			defaults.put(a_fontObject, new FontUIResource(
						 font.getName(), font.getStyle(), (int)(Math.round(font.getSize() * a_resize))));
		}
		catch (Exception a_e)
		{
			LogHolder.log(LogLevel.ERR, LogType.GUI, a_e);
		}
	}

	/**
	 * getMaxSize: takes a Vector of JComponents and returns a Dimension with width and height
	 * equivalent to the maximum width and height among the Components of the Vector
	 *
	 * @param aComponents Vector
	 * @return Dimension
	 */
	public static Dimension getMaxSize(Vector aComponents) {
		Dimension targetSize = new Dimension(0,0);
		int targetWidth = 0;
		int targetHeight = 0;
		//find max width and height
		for (Enumeration widgets = aComponents.elements(); widgets.hasMoreElements(); )
		{
		  JComponent curWidget = (JComponent) widgets.nextElement();
		  targetWidth = Math.max(targetSize.width, curWidget.getPreferredSize().width);
		  targetHeight = Math.max(targetSize.height, curWidget.getPreferredSize().height);
		  targetSize.setSize(targetWidth, targetHeight);
		}
		targetSize.setSize(targetWidth,targetHeight);
		return targetSize;
  }

  /**
   * getTotalSize: takes a Vector of JComponents and returns a Dimension with width and height
   * equivalent to the cumulative width and height of all  the Components of the Vector
   *
   * Typically, you'll only use either height or width (e.g. find out how high a parent component
   * has to be to contain all the components of the Vector)
   *
   * @param aComponents Vector
   * @return Dimension
   */
  public static Dimension getTotalSize(Vector aComponents) {
	  int totalWidth = 0;
	  int totalHeight = 0;
	  //find max width and height
	  for (Enumeration widgets = aComponents.elements(); widgets.hasMoreElements(); )
	  {
		JComponent curWidget = (JComponent) widgets.nextElement();
		totalWidth += curWidget.getPreferredSize().width;
		totalHeight += curWidget.getPreferredSize().height;
	  }
	  return new Dimension(totalWidth,totalHeight);
}


	/**
	 * setSizes: takes a Vector of JCoponents and sets them all to the Dimension passed as parameter
	 * (sets preferredSize as well as maximumSize)
	 *
	 * @param aComponents Vector
	 * @param aDimension Dimension
	 */
	public static void setSizes(Vector aComponents, Dimension aDimension){
	  JComponent curComp;
	  for (Enumeration components = aComponents.elements(); components.hasMoreElements(); )
	  {
		  curComp = (JComponent) components.nextElement();
		  curComp.setPreferredSize( new Dimension(aDimension.width, aDimension.height));
		  curComp.setMaximumSize(new Dimension(aDimension.width, aDimension.height));
	  }
    }

	/**
     * Takes a Vector of components, and sets the width of all components to the width of the Dimension
	 * passsed as parameter
	 * Just like @see setSizes(), except it only affects the width, and leaves height unchanged
	 *
	 * @param aComponents Vector
	 * @param aDimension Dimension
	 */
	public static void setEqualWidths(Vector aComponents, Dimension aDimension)
	{
		JComponent curComp;
		for (Enumeration components = aComponents.elements(); components.hasMoreElements(); )
		{
			curComp = (JComponent) components.nextElement();
			double oldHeight = curComp.getPreferredSize().height;
			curComp.setPreferredSize( new Dimension(aDimension.width, (int) oldHeight));
			curComp.setMaximumSize(new Dimension(aDimension.width, (int) oldHeight));
	    }
	}


	/** * Diese Klasse dient dazu aus einem vorhandenen Icon ein neues Icon
	 * herzustellen. Dazu werden neben dem vorhanden Icon die Skalierungsfaktoren angegeben.
	 */
	private static class IconScaler implements Icon
	{
		private static Class GRAPHICS_2D;

		static
		{
			try
			{
				GRAPHICS_2D = Class.forName("java.awt.Graphics2D");
			}
			catch (ClassNotFoundException a_e)
			{
				GRAPHICS_2D = null;
			}
		}

		private Icon m_icon;
		private double m_scaleWidth;
		private double m_scaleHeight;


		/**
		 * Creates a new Icon that scales a given Icon with the given settings.
		 */
		public IconScaler(Icon icon, double a_scale)
		{
			this(icon, a_scale, a_scale);
		}

		public IconScaler(Icon icon, double a_scaleWidth, double a_scaleHeight)
		{
			m_icon = icon;
			if (GRAPHICS_2D != null)
			{
				m_scaleWidth = a_scaleWidth;
				m_scaleHeight = a_scaleHeight;
			}
			else
			{
				m_scaleWidth = 1.0;
				m_scaleHeight = 1.0;
			}
		}

		public int getIconHeight()
		{
			return (int) (m_icon.getIconHeight() * m_scaleHeight);
		}

		public int getIconWidth()
		{
			return (int) (m_icon.getIconWidth() * m_scaleWidth);
		}

		public void paintIcon(Component c, Graphics g, int x, int y)
		{
			scale(g, m_scaleWidth, m_scaleHeight);
			m_icon.paintIcon(c, g, x, y);
			scale(g, 1.0 / m_scaleWidth, 1.0 / m_scaleHeight);
		}

		private static void scale(Graphics a_graphics, double a_scaleWidth, double a_scaleHeight)
		{
			if (GRAPHICS_2D != null)
			{
				try
				{
					GRAPHICS_2D.getMethod("scale", new Class[]
										  {double.class, double.class}).invoke(
											  a_graphics,
											  new Object[]
											  {new Double(a_scaleWidth), new Double(a_scaleHeight)});
				}
				catch (Exception a_e)
				{
					LogHolder.log(LogLevel.ERR, LogType.GUI, a_e);
				}
				//Graphics2D g2 = (Graphics2D) g;
				//g2.scale(m_scaleWidth, m_scaleHeight);
			}
		}


	}
	
	public static String getCountryFromServiceLocation(ServiceLocation a_loc)
	{
		if(a_loc == null)
		{
			return "";
		}
			
		String strLocation = "";

		if (a_loc.getCity() != null && a_loc.getCity().trim().length() > 0)
		{
			strLocation = a_loc.getCity().trim();
		}

		if (a_loc.getState() != null && a_loc.getState().trim().length() > 0 &&
			!strLocation.equals(a_loc.getState().trim()))
		{
			if (strLocation.length() > 0)
			{
				strLocation += ", ";
			}
			strLocation += a_loc.getState().trim();
		}

		if (a_loc.getCountry() != null && a_loc.getCountry().trim().length() > 0)
		{
			if (strLocation.length() > 0)
			{
				strLocation += ", ";
			}

			try
			{
				strLocation += new CountryMapper(
						a_loc.getCountry(), JAPMessages.getLocale()).toString();
			}
			catch (IllegalArgumentException a_e)
			{
				strLocation += a_loc.getCountry().trim();
			}
		}

		if (strLocation.trim().length() == 0)
		{
			return "N/A";
		}
	
		return strLocation;
	}
}
