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
package gui;

import gui.dialog.JAPDialog;

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.text.MutableAttributeSet;
import javax.swing.text.html.HTML.Attribute;
import javax.swing.text.html.HTML.Tag;
import javax.swing.text.html.HTMLEditorKit.ParserCallback;

import anon.util.JAPMessages;

import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;

/** This class provides a dialog showing a map section loaded from Yahoo(R)
 * according to the specified latitude and longitude.
 */
public class MapBox extends JAPDialog implements ChangeListener
{
	/** Messages */
	private static final String MSG_ERROR_WHILE_LOADING = MapBox.class.getName() + "_errorLoading";
	private static final String MSG_PLEASE_WAIT = MapBox.class.getName() + "_pleaseWait";
	private static final String MSG_CLOSE = MapBox.class.getName() + "_close";
	private static final String MSG_TITLE = MapBox.class.getName() + "_title";
	private static final String MSG_ZOOM = MapBox.class.getName() + "_zoom";
		
	/** The label containing the map in form of an <CODE>ImageIcon</CODE> */
	private JLabel m_lblMap;
	/** The slider for adjusting zoom level */
	private JSlider m_sldZoom;
	/** Close button */
	private JButton m_btnClose;

	/** The URL pointing to the map image */
	private String m_sImageURL;
	/** Key that is needed for querying maps */
	private static final String KEY = "ABQIAAAAvDhPn6b_F550GDisnEZpIxQrda7TSvuNFYSGo_31R-LaV_0iCRRJ7r3yduvtz_ZgBJjj2VOFap_JoQ";
	/** The latitude of the center of the map */
	private String m_sLatitude;
	/** The longitude of the center of the map */
	private String m_sLongitude;
	/** Desired size of the map image */
	private String m_sImageSize = "550x550";

	/** Constructs a new <CODE>MapBox</CODE> instance.
	 * @param parent The parent of the dialog window
	 * @param lat The latitude of the point to show on the map
	 * @param lon The longitude of the point to show on the map
	 * @param level The zoom level to be set (0 - 19)
	 * @throws Exception If an error occurs
	 */
	public MapBox(Component parent, String lat, String lon, int level) //throws IOException
	{
		super(parent, "");
		m_sLongitude = lon;
		m_sLatitude = lat;

		GridBagLayout layout = new GridBagLayout();
		GridBagConstraints c = new GridBagConstraints();
		getContentPane().setLayout(layout);
		c.anchor = GridBagConstraints.NORTHWEST;
		c.insets = new Insets(10, 10, 10, 10);
		m_lblMap = new JLabel();
		c.gridx = 0;
		c.gridy = 0;
		c.gridwidth = 2;
		c.gridheight = 2;
		layout.setConstraints(m_lblMap, c);
		getContentPane().add(m_lblMap);

		c.gridx = 2;
		c.gridy = 0;
		c.gridwidth = 1;
		c.gridheight = 1;
		c.insets = new Insets(20, 10, 5, 10);
		JLabel l = new JLabel(JAPMessages.getString(MSG_ZOOM));
		layout.setConstraints(l, c);
		getContentPane().add(l);

		// The zoom
		m_sldZoom = new JSlider(JSlider.VERTICAL, 0, 15, level);
		m_sldZoom.setPaintTicks(true);
		m_sldZoom.setMajorTickSpacing(1);
		m_sldZoom.setMinorTickSpacing(1);
		m_sldZoom.setSnapToTicks(true);
		m_sldZoom.setPaintLabels(true);
		m_sldZoom.setRequestFocusEnabled(false);
		m_sldZoom.addChangeListener(this);
		c.insets = new Insets(5, 10, 20, 10);
		c.gridx = 2;
		c.gridy = 1;
		c.fill = GridBagConstraints.VERTICAL;
		layout.setConstraints(m_sldZoom, c);
		getContentPane().add(m_sldZoom);

		/*
		Font font = new Font("Dialog", Font.BOLD, 20);
		JLabel site = new JLabel("PROCESSED BY:");
		site.setFont(font);
		c.insets = new Insets(10, 10, 10, 10);
		c.gridx = 0;
		c.gridy = 2;
		c.gridwidth = 1;
		c.anchor = GridBagConstraints.CENTER;
		c.fill = GridBagConstraints.NONE;
		layout.setConstraints(site, c);
		getContentPane().add(site);

		// This is how you would embed an image from the Internet:
		//String logo = "http://art.mapquest.com/mqsite_english/logo";
		//URL MapLogo = new URL(logo);
		
		ImageIcon maplogo = GUIUtils.loadImageIcon(IMG_GOOGLE);
		JLabel logolabel = new JLabel(maplogo);
		c.gridx = 1;
		c.gridy = 2;
		layout.setConstraints(logolabel, c);
		getContentPane().add(logolabel);
		*/
		
		m_btnClose = new JButton(JAPMessages.getString(MSG_CLOSE));
		m_btnClose.addActionListener(new ActionListener(){
		public void actionPerformed(ActionEvent a_event)
		{
			dispose();
		}});

		c.gridx = 2;
		c.gridy = 2;
		layout.setConstraints(m_btnClose, c);
		getContentPane().add(m_btnClose);

		refresh();

		pack();
		setResizable(false);
	}

	/** Sets the coordinates to be displayed on the map.
	 * @param a_latitude The latitude
	 * @param a_longitude The longitude
	 * @throws IOException If an error occurs while reading the map from www.mapquest.com
	 */
	public void setGeo(String a_latitude, String a_longitude) throws IOException
	{
		m_sLongitude = a_longitude;
		m_sLatitude = a_latitude;
		refresh();
	}

	public void setVisible(boolean a_bVisible)
	{
		super.setVisible(a_bVisible);
	}

	public void stateChanged(ChangeEvent e)
	{
		JSlider s1 = (JSlider) e.getSource();
		if (!s1.getValueIsAdjusting())
		{
			refresh();
		}
	}

	/** 
	 * Contact <a href="http://maps.google.com">Google Maps</a> to (re-)load the map.
	 * @throws IOException If an error occurs while retrieving the web site
	 */
	private void refresh()
	{
		// Set the icon to null at first
		m_lblMap.setIcon(null);
		m_lblMap.setText(JAPMessages.getString(MSG_PLEASE_WAIT) + "...");
		m_lblMap.repaint();
		
		// Create the URL
		m_sImageURL = "http://maps.google.com/staticmap?markers=" + m_sLatitude + "," + m_sLongitude +
		              "&zoom=" + (m_sldZoom.getValue()+2) + "&size=" + m_sImageSize + "&key=" + KEY;
		LogHolder.log(LogLevel.DEBUG, LogType.MISC, "Getting map: " + m_sImageURL);
		
		// Set the title
		String sTitle = JAPMessages.getString(MSG_TITLE, new String[]{m_sLatitude, m_sLongitude});
		setTitle(sTitle);
		
		try {
			// Instantiate the map image as ImageIcon
			ImageIcon map = new ImageIcon(new URL(m_sImageURL));
			// Check if there is a map image
			if (map.getIconHeight() == -1)
			{
				this.dispose();
				JAPDialog.showErrorDialog(this.getParentComponent(), 
						                  JAPMessages.getString(MSG_ERROR_WHILE_LOADING), 
						                  LogType.NET);
			} 
			else
			{
				// Set the map image as the icon
				m_lblMap.setText("");
				m_lblMap.setIcon(map);			
			}
		} catch (MalformedURLException e) {
			this.dispose();
			JAPDialog.showErrorDialog(this.getParentComponent(), e.getMessage(), LogType.NET);
		}
	}

    /** A subclass of <CODE>javax.swing.text.html.HTMLEditorKit.ParserCallback</CODE>
     * that parses the HTML page requested from http://maps.yahoo.com and searches for
     * the URL of the actual map image. As of June 2008, the page contains an img-tag
     * that contains the actual URL:<br>
	 * <br>
	 * &lt;img id=&quot;map&quot; name=&quot;map&quot;
	 * src=&quot;&lt;URL-of-the-map-image&gt;&quot;&gt;<br>
	 * <br>
	 * The 'src'-attribute contains the real URL of the map image.
     */
	// FIXME: Not needed anymore
	private class SiteParser extends ParserCallback
	{
		public void handleSimpleTag(Tag t, MutableAttributeSet a, int pos)
		{
			handleStartTag(t, a, pos);
		}

		public void handleStartTag(Tag t, MutableAttributeSet a, int pos)
		{
			if (t == Tag.IMG)
			{
				try
				{
					if (a.getAttribute(Attribute.ID).toString().equals("map"))
					{
						LogHolder.log(LogLevel.DEBUG, LogType.MISC, "Map image found: "+a.getAttribute(Attribute.SRC).toString());
						MapBox.this.m_sImageURL = a.getAttribute(Attribute.SRC).toString();
					}
				}
				catch (NullPointerException npe)
				{
					/* IGNORE: This happens if there is an IMG that has no attribute ID */
				}
			}
		}
	}
}