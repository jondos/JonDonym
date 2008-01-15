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
package jap;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.MalformedURLException;
import java.sql.Timestamp;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.StringTokenizer;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Window;
import javax.swing.AbstractButton;
import javax.swing.JFileChooser;
import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;

import anon.crypto.JAPCertificate;
import platform.AbstractOS;
import gui.JAPMessages;
import gui.dialog.JAPDialog;
import gui.SimpleFileFilter;
import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;
import java.util.Calendar;
import anon.util.Util;
import jap.pay.wizardnew.PaymentInfoPane;
import java.util.Locale;

/**
 * This class contains static utility functions for Jap
 */
public final class JAPUtil
{
	private static final String MSG_DATE_UNIT = JAPUtil.class.getName() + "_";

	public static final int MAX_FORMAT_BYTES = 0;
	public static final int MAX_FORMAT_KBYTES = 1;
	public static final int MAX_FORMAT_MBYTES = 2;
	public static final int MAX_FORMAT_GBYTES = 3;
	public static final int MAX_FORMAT_ALL = 4;



	public static JAPDialog.ILinkedInformation createDialogBrowserLink(String a_strUrl)
	{
		URL url;
		try
		{
			url = new URL(a_strUrl);
		}
		catch (MalformedURLException a_e)
		{
			return null;
		}
		final URL myUrl = url;

		return new JAPDialog.LinkedInformationAdapter()
		{
			public String getMessage()
			{
				return myUrl.toString();
			}

			public int getType()
			{
				return JAPDialog.ILinkedInformation.TYPE_SELECTABLE_LINK;
			}

			public void clicked(boolean a_bState)
			{
				AbstractOS.getInstance().openURL(myUrl);
			}
		};
	}

	/** Returns the desired unit for this amount of Bytes (Bytes, kBytes, MBytes,GBytes)*/
	public static String formatBytesValueWithUnit(long c)
	{
		return formatBytesValueWithUnit(c, MAX_FORMAT_ALL);
	}

	public static String formatBytesValueWithUnit(long c, int a_maxFormat)
	{
		return formatBytesValueWithoutUnit(c, a_maxFormat) + " " + formatBytesValueOnlyUnit(c, a_maxFormat);
	}


	public static String formatBytesValueOnlyUnit(long c)
	{
		return formatBytesValueOnlyUnit(c, MAX_FORMAT_ALL);
	}

	public static String formatBytesValueOnlyUnit(long c, int a_maxFormat)
	{
		if (c < 1000 || a_maxFormat < MAX_FORMAT_KBYTES)
		{
			return JAPMessages.getString("Byte");
		}
		else if (c < 1000000 || a_maxFormat < MAX_FORMAT_MBYTES)
		{
			return JAPMessages.getString("kByte");
		}
		else if (c < 1000000000 || a_maxFormat < MAX_FORMAT_GBYTES)
		{
			return JAPMessages.getString("MByte");
		}
		return JAPMessages.getString("GByte");
	}

	public static String formatBytesValueWithoutUnit(long c)
	{
		return formatBytesValueWithoutUnit(c, MAX_FORMAT_ALL);
}

	/** Returns a formated number which respects different units (Bytes, kBytes, MBytes, GBytes)*/
	public static String formatBytesValueWithoutUnit(long c, int a_maxFormat)
	{
		DecimalFormat df = (DecimalFormat) DecimalFormat.getInstance(JAPMessages.getLocale());
		double d = c;
		if (c < 1000 || a_maxFormat < MAX_FORMAT_KBYTES)
		{
			df.applyPattern("#,####");
		}
		else if (c < 1000000 || a_maxFormat < MAX_FORMAT_MBYTES)
		{
			d /= 1000.0;
			df.applyPattern("#,##0.0");
		}
		else if (c < 1000000000 || a_maxFormat < MAX_FORMAT_GBYTES)
		{
			d /= 1000000.0;
			df.applyPattern("#,##0.0");
		}
		else
		{
			d /= 1000000000.0;
			df.applyPattern("#,##0.0");
		}
		return df.format(d);
	}


	/**
	 * deprecated, since balances are not stored as
	 * @param centvalue long
	 * @return String
	 */
	public static String formatEuroCentValue(long centvalue)
	{
		long whole = centvalue / 100;
		long part = centvalue - (whole *100);
		String wholeString = (new Long(whole)).toString();
		String partString = (new Long(part)).toString();
		String partFiller = part<10?"0":"";
		String language = JAPMessages.getLocale().getLanguage();
		return wholeString+getCurrencyDelimiter(language)+partFiller+partString+" Euro";
	}



	public static String getCurrencyDelimiter(String language)
	{
		if (language.equalsIgnoreCase("en"))
		{
			return new String(".");
		} else
		{
			return new String(",");
		}
	}

	public static int applyJarDiff(File fileOldJAR, File fileNewJAR,
								   byte[] diffJAR)
	{
		try
		{
			ZipFile zold = null;
			ZipInputStream zdiff = null;
			ZipOutputStream znew = null;
			ZipEntry ze = null;
			// geting old names
			zold = new ZipFile(fileOldJAR);
			Hashtable oldnames = new Hashtable();
			Enumeration e = zold.entries();
			while (e.hasMoreElements())
			{
				ze = (ZipEntry) e.nextElement();
				oldnames.put(ze.getName(), ze.getName());
			}
			// it shouldn't be a FileStream but an ByteArrayStream or st like that
			//zdiff=new ZipInputStream(new FileInputStream(diffJAR));
			zdiff = new ZipInputStream(new ByteArrayInputStream(diffJAR));
			znew = new ZipOutputStream(new FileOutputStream(fileNewJAR));
			znew.setLevel(9);
			byte[] b = new byte[5000];
			while ( (ze = zdiff.getNextEntry()) != null)
			{
				ZipEntry zeout = new ZipEntry(ze.getName());
				if (!ze.getName().equalsIgnoreCase("META-INF/INDEX.JD"))
				{
					LogHolder.log(LogLevel.DEBUG, LogType.MISC, "JARDiff: " + ze.getName());
					oldnames.remove(ze.getName());
					int s = -1;
					zeout.setTime(ze.getTime());
					zeout.setComment(ze.getComment());
					zeout.setExtra(ze.getExtra());
					zeout.setMethod(ze.getMethod());
					if (ze.getSize() != -1)
					{
						zeout.setSize(ze.getSize());
					}
					if (ze.getCrc() != -1)
					{
						zeout.setCrc(ze.getCrc());
					}
					znew.putNextEntry(zeout);
					while ( (s = zdiff.read(b, 0, 5000)) != -1)
					{
						znew.write(b, 0, s);

					}
					znew.closeEntry();
				}
				else
				{
					BufferedReader br = new BufferedReader(new InputStreamReader(zdiff));
					String s = null;
					while ( (s = br.readLine()) != null)
					{
						StringTokenizer st = new StringTokenizer(s);
						s = st.nextToken();
						if (s.equalsIgnoreCase("remove"))
						{
							s = st.nextToken();
							LogHolder.log(LogLevel.DEBUG, LogType.MISC, "JARDiff: remove " + s);
							oldnames.remove(s);
						}
						else if (s.equalsIgnoreCase("move"))
						{
							LogHolder.log(LogLevel.DEBUG, LogType.MISC, "JARDiff: move " + st.nextToken());
						}
						else
						{
							LogHolder.log(LogLevel.DEBUG, LogType.MISC, "JARDiff: unkown: " + s);
						}
					}
				}
				zdiff.closeEntry();
			}
			e = oldnames.elements();
			while (e.hasMoreElements())
			{
				String s = (String) e.nextElement();
				LogHolder.log(LogLevel.DEBUG, LogType.MISC, s);
				ze = zold.getEntry(s);
				ZipEntry zeout = new ZipEntry(ze.getName());
				zeout.setTime(ze.getTime());
				zeout.setComment(ze.getComment());
				zeout.setExtra(ze.getExtra());
				zeout.setMethod(ze.getMethod());
				if (ze.getSize() != -1)
				{
					zeout.setSize(ze.getSize());
				}
				if (ze.getCrc() != -1)
				{
					zeout.setCrc(ze.getCrc());
				}
				znew.putNextEntry(zeout);
				LogHolder.log(LogLevel.DEBUG, LogType.MISC, "JARDiff: Getting in..");
				InputStream in = zold.getInputStream(ze);
				int l = -1;
				LogHolder.log(LogLevel.DEBUG, LogType.MISC, "JARDiff: Reading..");
				try
				{
					while ( (l = in.read(b, 0, 5000)) != -1)
					{
						znew.write(b, 0, l);
					}
				}
				catch (Exception er)
				{
					er.printStackTrace(System.out);
				}
				in.close();
				znew.closeEntry();

			}

			znew.finish();
			znew.flush();
			znew.close();
			zold.close();
			zdiff.close();
		}
		catch (Throwable e)
		{
			e.printStackTrace();
			return -1;
		}
		return 0;
	}

	/** Sets the mnemonic charcter of a component. The character must be set
	 *  in the properties file under a name that is given in mnPropertyString.
	 */
	public static void setMnemonic(AbstractButton bt, String mn)
	{
		if ( (bt == null) || (mn == null) || (mn.equals("")))
		{
			return;
		}
		bt.setMnemonic(mn.charAt(0));
	}

	public static void setPerfectTableSize(JTable table, Dimension maxDimension)
	{
		TableModel tableModel = table.getModel();
		int perfectWidth = 0;
		int perfectHeight = 0;
		// the Table uses the minimum height to draw itself, weird...
		// so we set the perfect heigt as the smallest column height
		int minimunColunmHeight = 0;
		for (int i = 0; i < tableModel.getColumnCount(); i++)
		{
			TableColumn column = table.getColumnModel().getColumn(i);
			TableCellRenderer headerRenderer = column.getHeaderRenderer();
			int headerWidth = column.getPreferredWidth();
			int columnHeight = 0;
			if (headerRenderer != null)
			{
				Component component = headerRenderer.getTableCellRendererComponent(null,
					column.getHeaderValue(), false, false, 0, 0);
				headerWidth = component.getPreferredSize().width;
				columnHeight = component.getPreferredSize().height;
			}
			if (tableModel.getRowCount() > 0)
			{
				// look at every entry
				TableCellRenderer tableCellRenderer = table.getDefaultRenderer(tableModel.getColumnClass(i));
				int cellWidth = 0;
				for (int row = 0; row < tableModel.getRowCount(); row++)
				{
					Object object = tableModel.getValueAt(row, i);
					Component component = tableCellRenderer.getTableCellRendererComponent(table, object, false, false,
						row, i);
					cellWidth = Math.max(cellWidth, component.getPreferredSize().width);
					columnHeight += component.getPreferredSize().height;
				}
				int preferredColumnWidth = Math.max(headerWidth, cellWidth);
				column.setPreferredWidth(preferredColumnWidth);
				perfectWidth += preferredColumnWidth;
				if (minimunColunmHeight == 0)
				{
					minimunColunmHeight = columnHeight;
				}
				else
				{
					minimunColunmHeight = Math.min(minimunColunmHeight, columnHeight);
				}
			}
		}
		// add some space for scrollbar,... (+ 30)
		perfectWidth = Math.min(maxDimension.width, perfectWidth + 30);
		perfectHeight = Math.min(maxDimension.height, minimunColunmHeight);
		table.setPreferredScrollableViewportSize(new Dimension(perfectWidth, perfectHeight));
	}

	public static JFileChooser showFileDialog(Window jf)
	{
		SimpleFileFilter active = null;
		JFileChooser fd2 = new JFileChooser();
		fd2.setFileSelectionMode(JFileChooser.FILES_ONLY);
		fd2.addChoosableFileFilter(active = new SimpleFileFilter());
		if (active != null)
		{
			fd2.setFileFilter(active);
		}
		fd2.setFileHidingEnabled(false);
		fd2.showOpenDialog(jf);
//										File m_fileCurrentDir = fd2.getCurrentDirectory();
		return fd2;
	}

	/** Shows a file open dialog and tries to read a certificate. Returns null, if the user canceld
	 * the open request. Throws IOException if certificate could not be readed or decoded.
	 */

	public static JAPCertificate openCertificate(Window jf) throws IOException
	{
		File file = showFileDialog(jf).getSelectedFile();
		JAPCertificate t_cert = null;
		if (file != null)
		{
			t_cert = JAPCertificate.getInstance(file);
			if (t_cert == null)
			{
				throw new IOException("Could not create certificate!");
			}
		}
		return t_cert;
	}

	/**
	 * formats a timestamp in an easily readable format.
	 * @param date Timestamp
	 * @param withTime boolean if true, the date+time is returned, otherwise date only.
	 */
	public static String formatTimestamp(Timestamp date, boolean withTime)
	{
		return formatTimestamp(date, withTime, null);
	}

	/**
	 * formatTimestamp
	 *
	 * @param date Timestamp
	 * @param withTime boolean
	 * @param a_language String: 2-letter code
	 * @return String
	 */
	public static String formatTimestamp(Timestamp date, boolean withTime, String a_language)
	{
		SimpleDateFormat sdf;
		String country = JAPMessages.getLocale().getCountry();
		if (a_language.equalsIgnoreCase("en") && country.equals(Locale.US) )
		{
			if (withTime)
			{
				sdf = new SimpleDateFormat("MM/dd/yy - HH:mm");
			}
			else
			{
				sdf = new SimpleDateFormat("MM/dd/yy");
			}
			return sdf.format(date);
		}
		else if (a_language.equalsIgnoreCase("en")) //any other english-speaking country
		{
			if (withTime)
			{
				sdf = new SimpleDateFormat("dd/MM/yy - HH:mm");
			}
			else
			{
				sdf = new SimpleDateFormat("dd/MM/yy");
			}
			return sdf.format(date);
		}
		else if (a_language.equalsIgnoreCase("de"))
		{
			if (withTime)
			{
				sdf = new SimpleDateFormat("dd.MM.yyyy - HH:mm");
			}
			else
			{
				sdf = new SimpleDateFormat("dd.MM.yyyy");
			}
		}
		else  //ISO standard
		{
		    if (withTime)
			{
				sdf = new SimpleDateFormat("yyyy-MM-dd  HH:mm");
			}
			else
			{
				sdf = new SimpleDateFormat("yyyy-MM-dd");
			}
		}
		return sdf.format(date);
	}

	/**
	 * get the Enddate for a given duration
	 *
	 * @param duration int: e.g. 2
	 * @param durationUnit String: e.g. "months"
	 * @return Timestamp: now + (duration * unit)
	 */
	public static Timestamp getEnddate(int duration, String durationUnit)
	{
		Calendar now = Calendar.getInstance();
		if (durationUnit.equals("days") || durationUnit.equals("day") )
		{
			now.add(Calendar.DATE,duration);
		} else if (durationUnit.equalsIgnoreCase("weeks") || durationUnit.equalsIgnoreCase("week")  )
		{
			now.add(Calendar.WEEK_OF_YEAR,duration);
		} else if (durationUnit.equalsIgnoreCase("months") || durationUnit.equalsIgnoreCase("month")  )
		{
			now.add(Calendar.MONTH,duration);
		}
		else if (durationUnit.equalsIgnoreCase("years") || durationUnit.equalsIgnoreCase("year") )
		{
			now.add(Calendar.YEAR,duration);
		}
		return new Timestamp(now.getTime().getTime());
	}

	public static String getDuration(int duration, String durationUnit)
	{
		String message;

		if (durationUnit.equals("days") || durationUnit.equals("day") )
		{
			if (duration == 1)
			{
				message = "day";
			}
			else
			{
				message = "days";
			}
		} else if (durationUnit.equalsIgnoreCase("weeks") || durationUnit.equalsIgnoreCase("week")  )
		{
			if (duration == 1)
			{
				message = "week";
			}
			else
			{
				message = "weeks";
			}
		} else if (durationUnit.equalsIgnoreCase("months") || durationUnit.equalsIgnoreCase("month")  )
		{
			if (duration == 1)
			{
				message = "month";
			}
			else
			{
				message = "months";
			}
		}
		else if (durationUnit.equalsIgnoreCase("years") || durationUnit.equalsIgnoreCase("year") )
		{
			if (duration == 1)
			{
				message = "year";
			}
			else
			{
				message = "years";
			}

		}
		else
		{
			return duration + "";
		}

		return duration + " " + JAPMessages.getString(MSG_DATE_UNIT + message);
	}

}
