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
	private ILinkGenerator m_linkToOpen = null;

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
		m_linkToOpen = new ImmutableLinkGenerator(a_Link);
	}

	public LinkMouseListener(ILinkGenerator a_Link)
	{
		super();
		m_linkToOpen = a_Link;
	}


	public interface ILinkGenerator
	{
		String createLink();
	}

	public void mouseClicked(MouseEvent e)
	{
		String linkText;
		if (m_linkToOpen != null)
		{
			linkText = m_linkToOpen.createLink();
		}
		else if (e.getSource() instanceof JLabel)
		{
			linkText = ((JLabel) e.getSource()).getText();
		}
		else
		{
			// do nothing
			return;
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

	private class ImmutableLinkGenerator implements ILinkGenerator
	{
		private String m_LinkToOpen;
		public ImmutableLinkGenerator(String a_LinkToOpen)
		{
			m_LinkToOpen = a_LinkToOpen;
		}

		public String createLink()
		{
			return m_LinkToOpen;
		}
	}

}
