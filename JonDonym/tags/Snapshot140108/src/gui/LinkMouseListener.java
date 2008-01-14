package gui;

import javax.swing.JComponent;
import logging.LogHolder;
import logging.LogLevel;
import java.awt.event.MouseAdapter;
import platform.AbstractOS;
import java.net.MalformedURLException;
import java.net.URL;
import javax.swing.JLabel;
import java.awt.event.MouseEvent;
import java.awt.Cursor;
import logging.LogType;

public class LinkMouseListener extends MouseAdapter
{
	private String linkToOpen = null;

	/**
	 * create a LinkMouseListener that can only be applied to a JLabel
	 * and will on click open an URL gotten from that JLabel's getText() method
	 */
	public LinkMouseListener()
	{
		super();
	}

	/**
	 * create a LinkMouseListener that can be applied to any component
	 * will open the supplied link on click
	 * does not check if the supplied String is a valid URL
	 * @param a_Link String
	 */
	public LinkMouseListener(String a_Link)
	{
		super();
		linkToOpen = a_Link;
	}



	public void mouseClicked(MouseEvent e)
	{
		String linkText;
		if (linkToOpen != null)
		{
			linkText = linkToOpen;
		}
		else
		{
			//Warning: will fail if LinkMouseListener is added to a JComponent other than a JLabel
			JLabel source = (JLabel) e.getSource();
			linkText = source.getText();
		}

		try
		{
			URL linkUrl = new URL(linkText);
			AbstractOS.getInstance().openURL(linkUrl);
		}
		catch (ClassCastException cce)
		{
			LogHolder.log(LogLevel.ERR, LogType.PAY,
						  "opening a link failed, reason: called on non-JLabel component");
		}
		catch (MalformedURLException mue)
		{
			LogHolder.log(LogLevel.ERR, LogType.PAY, "opening a link failed, reason: malformed URL");
		}

	}

	public void mouseEntered(MouseEvent e)
	{
		JComponent source = (JComponent) e.getSource();
		source.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
	}

	public void mouseExited(MouseEvent e)
	{
		JComponent source = (JComponent) e.getSource();
		source.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
	}

}
