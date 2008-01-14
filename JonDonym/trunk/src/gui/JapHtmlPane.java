package gui;

import java.awt.event.ActionEvent;
import gui.dialog.JAPDialog;
import java.net.URL;
import javax.swing.event.HyperlinkEvent;
import javax.swing.SwingUtilities;
import anon.util.ResourceLoader;
import javax.swing.event.HyperlinkListener;
import java.awt.Cursor;
import logging.LogType;
import javax.swing.text.Document;
import logging.LogLevel;
import logging.LogHolder;
import java.io.IOException;
import javax.swing.JScrollPane;
import javax.swing.JEditorPane;
import java.util.Vector;

/**
 * Generalized form of the private class HtmlPane found in JAPHelp
 *
 * Has no history, just shows a bunch of HTML in a scrolling pane,
 * and with clickable links that can also be copied to the clipboard
 *
 * ( WORK IN PROGRESS )
 *
 * @author Elmar Schraml
 *
 */
public class JapHtmlPane extends JScrollPane implements HyperlinkListener
{
	private JEditorPane html;
	private URL url;
	private Cursor cursor;

	public JapHtmlPane(String a_textToShow)
	{
		html = new JEditorPane("text/html", a_textToShow);
		new JTextComponentToClipboardCopier(true).registerTextComponent(html);
		html.setEditable(false);
		html.addHyperlinkListener(this);
		getViewport().add(html);
		cursor = html.getCursor();
	}

	public JEditorPane getPane()
	{
		return html;
	}

	public void hyperlinkUpdate(HyperlinkEvent e)
	{
		if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED)
		{
			linkActivated(e.getURL());
		}
		else if (e.getEventType() == HyperlinkEvent.EventType.ENTERED)
		{
			html.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		}
		else if (e.getEventType() == HyperlinkEvent.EventType.EXITED)
		{
			html.setCursor(cursor);
		}
	}

	private void linkActivated(URL u)
	{
		html.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
		SwingUtilities.invokeLater(new PageLoader(u));
	}

	/**
	 * Loads URL without adding it to the history
	 * @param a_url URL
	 */
	private void loadURL(URL a_url)
	{
		html.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
		SwingUtilities.invokeLater(new PageLoader(a_url));
	}


	/**
	 * This needs to be outside the class for compilation in JDK 1.1.8.
	 */
	private static final String MAILTO = "mailto";
	private final class PageLoader implements Runnable
	{
		PageLoader(URL u)
		{
			url = u;
		}

		public void run()
		{
			if (url == null)
			{
				// restore the original cursor
				html.setCursor(cursor);
				html.getParent().repaint();
			}
			else if (
				url.getProtocol().startsWith(ResourceLoader.SYSTEM_RESOURCE_TYPE_FILE) ||
				url.getProtocol().startsWith(ResourceLoader.SYSTEM_RESOURCE_TYPE_ZIP) ||
				url.getProtocol().startsWith(ResourceLoader.SYSTEM_RESOURCE_TYPE_JAR) ||
				url.getProtocol().startsWith(ResourceLoader.SYSTEM_RESOURCE_TYPE_GENERIC))
			{
				Document doc = html.getDocument();
				try
				{
					html.setPage(url);
				}
				catch (IOException ioe)
				{
					/** @todo if this is a context page, try to load the english version */
					html.setDocument(doc);
					getToolkit().beep();
				}
				finally
				{
					// schedule the cursor to revert after
					// the paint has happended.
					url = null;
					SwingUtilities.invokeLater(this);
				}
			}
			else
			{
				boolean success = true;

				if (!success)
				{
					html.setCursor(cursor);
				}
			}
		}

		/**
		 * Needed to copy an external URL that could not be opened to the clip board.
		 */
		private class ExternalLinkedInformation extends JAPDialog.LinkedInformationAdapter
		{
			private URL m_url;

			public ExternalLinkedInformation(URL a_url)
			{
				m_url = a_url;
			}

			/**
			 * Returns the URL that could not be opened in the help window.
			 * @return the URL
			 */
			public String getMessage()
			{
				return m_url.toString();
			}

			/**
			 * Returns TYPE_SELECTABLE_LINK.
			 * @return TYPE_SELECTABLE_LINK
			 */
			public int getType()
			{
				return TYPE_SELECTABLE_LINK;
			}

			/**
			 * Returns true, as the dialog does not need to open another window.
			 * @return true
			 */
			public boolean isApplicationModalityForced()
			{
				return true;
			}
		}
	}
	}
