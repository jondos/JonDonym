package gui;

import java.awt.Rectangle;

import javax.swing.JViewport;

/** a workaround-Viewport to avoid that a scroll-pane is scrolled to the end when it becomes visible */
public class UpperLeftStartViewport extends JViewport 
{
	public void scrollRectToVisible(Rectangle rect)
	{
		rect.y = 0;
		super.scrollRectToVisible(rect);
	}
}
