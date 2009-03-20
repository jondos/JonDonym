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

import java.io.File;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Enumeration;
import java.util.Observable;
import java.util.Properties;

import java.awt.Component;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import anon.util.JAPMessages;
import anon.util.Util;

import logging.FileLog;
import logging.Log;
import logging.LogLevel;
import logging.LogType;
import gui.*;
import gui.dialog.*;

/**
 * This class serves as a debugging interface.
 * It provides different debug types and levels for the output of debug
 * messages.
 * <P>
 * The debug level can be set with
 * <code>JAPDebug.setDebuglevel(int level)</code>.
 * <P>
 * The debug type can be set with
 * <code>JAPDebug.setDebugType(int type)</code>.
 * <P>
 * To output a debug message use
 * <code>JAPDebug.out(int level, int type, String txt)</code>
 * This is a Singleton!
 */
final public class JAPDebug extends Observable implements ActionListener, Log
{

	private int m_debugType = LogType.ALL;
	private int m_debugLevel = LogLevel.DEBUG;
	private static JTextArea m_textareaConsole;
	private static JAPDialog m_frameConsole;
	private static boolean m_bConsole = false;
	private static volatile boolean ms_bFile = false;
	private static String ms_strFileName = null;
	private static FileLog ms_FileLog = null;
	private static JAPDebug debug;
	private static SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy/MM/dd-hh:mm:ss, ");
	private WindowAdapter m_winAdapter;

	private JAPDebug()
	{
		m_debugType = LogType.ALL;
		m_debugLevel = LogLevel.DEBUG;
		m_bConsole = false;
		ms_bFile = false;
		ms_strFileName = null;
		m_winAdapter = new WindowAdapter()
		{
			public void windowClosing(WindowEvent e)
			{
				synchronized (JAPDebug.this)
				{
					m_bConsole = false;
					JAPDebug.this.setChanged();
					JAPDebug.this.notifyObservers();
				}
			}
			
			public void windowClosed(WindowEvent e)
			{
				synchronized (JAPDebug.this)
				{
					m_bConsole = false;
					JAPDebug.this.setChanged();
					JAPDebug.this.notifyObservers();
				}
			}
		};
	}

	public void finalize()
	{
		ms_bFile = false;
		try
		{
			super.finalize();
		}
		catch (Throwable ex)
		{
		}
	}

	public static JAPDebug getInstance()
	{
		if (debug == null)
		{
			debug = new JAPDebug();
		}
		return debug;
	}

	/** Output a debug message.
	 *  @param level The level of the debugging message (EMERG,ALERT,CRIT,ERR,WARNING,NOTICE,INFO,DEBUG)
	 *  @param type The type of the debugging message (GUI, NET, THREAD, MISC)
	 *  @param txt   The message itself
	 */
	public void log(int level, int type, String txt)
	{
		try
		{
			if ( (level <= m_debugLevel) && (m_debugType & type) != 0)
			{
				synchronized (this)
				{
					String str = "[" + dateFormatter.format(new Date()) + LogLevel.STR_Levels[level] + "] " +
						txt +
						"\n";
					if (!m_bConsole)
					{
						System.err.print(str);
					}
					else
					{
						m_textareaConsole.append(str);
						m_textareaConsole.setCaretPosition(m_textareaConsole.getText().length());
					}
					if (ms_bFile)
					{
						ms_FileLog.log(level, type, txt);
					}
				}
			}
		}
		catch (Throwable t)
		{
		}
	}

	/** Set the debugging type you like to output. To activate more than one type you simly add
	 *  the types like this <code>setDebugType(JAPDebug.GUI+JAPDebug.NET)</code>.
	 *  @param type The debug type (NUL, GUI, NET, THREAD, MISC)
	 */
	public void setLogType(int type)
	{
		m_debugType = type;
		if (ms_bFile)
		{
			ms_FileLog.setLogType(type);
		}
	}

	/** Get the current debug type.
	 */
	public int getLogType()
	{
		return m_debugType;
	}

	/** Set the debugging level you would like to output.
	 *  The possible parameters are (EMERG, ALERT, EXCEPTION, ERR, WARNING, NOTICE, INFO, DEBUG).
	 *  DEBUG means output all messages, EMERG means only emergency messages.
	 *  @param level The debug level (EMERG, ALERT, EXCEPTION, ERR, WARNING, NOTICE, INFO, DEBUG)
	 */
	public void setLogLevel(int level)
	{
		if (level < 0 || level > LogLevel.DEBUG)
		{
			return;
		}
		m_debugLevel = level;
		if (ms_bFile)
		{
			ms_FileLog.setLogLevel(level);
		}
	}

	/** Get the current debug level.
	 */
	public int getLogLevel()
	{
		if (debug == null)
		{
			JAPDebug.getInstance();
		}
		return debug.m_debugLevel;
	}

	/** Shows or hiddes a Debug-Console-Window
	 * @param b set true to show the debug console or false to hidde them
	 * @param parent the parent frame of the debug console
	 */
	public static void showConsole(boolean b, Component parent)
	{
		debug.internal_showConsole(b, parent);
	}

	/** Enables or disables log output to a File.
	 * @param strFilename the Filename the logoutput should go to, if null (or the empty String "") log
	 * to file is disabled.
	 */
	public static void setLogToFile(String strFilename)
	{
		if (strFilename == null || strFilename.trim().equals(""))
		{
			ms_bFile = false;
			ms_FileLog = null;
		}
		else
		{
			ms_FileLog = new FileLog(strFilename, 10000000, 2);
			ms_FileLog.setLogLevel(getInstance().m_debugLevel);
			ms_FileLog.setLogType(getInstance().m_debugType);
			ms_bFile = true;
		}
		ms_strFileName = strFilename;
	}

	/* Leads to deadlock on startup.
	public static void setConsoleParent(Window parent)
	{
		if ( (debug != null) && (JAPDebug.m_bConsole) && (JAPDebug.m_frameConsole != null))
		{
			JAPDialog tmpDlg = new JAPDialog(parent, "Debug-Console");
			//tmpDlg.getContentPane().add(new JScrollPane(debug.textareaConsole));
			tmpDlg.setContentPane(JAPDebug.m_frameConsole.getContentPane());
			tmpDlg.addWindowListener(debug);
			tmpDlg.setSize(JAPDebug.m_frameConsole.getSize());
			tmpDlg.setLocation(JAPDebug.m_frameConsole.getLocation());
			tmpDlg.setVisible(true);
			JAPDebug.m_frameConsole.dispose();
			JAPDebug.m_frameConsole = tmpDlg;
		}
	}*/

	public static boolean isShowConsole()
	{
		return JAPDebug.m_bConsole;
	}

	public static boolean isLogToFile()
	{
		return JAPDebug.ms_bFile;
	}

	/** Returns the Filename log output goes to or null if logging to a File is disabled.*/
	public static String getLogFilename()
	{
		return ms_strFileName;
	}

	public void internal_showConsole(boolean b, Component parent)
	{
		if (!b && m_bConsole)
		{
			m_frameConsole.dispose();
			m_frameConsole.removeWindowListener(m_winAdapter);
			m_textareaConsole = null;
			m_frameConsole = null;
			m_bConsole = false;
		}
		else if (b && !m_bConsole)
		{
			m_frameConsole = new JAPDialog(parent, "Debug-Console", false);
			m_textareaConsole = new JTextArea(null, 20, 30);
			m_textareaConsole.setEditable(false);
			Font f = Font.decode("Courier");
			if (f != null)
			{
				m_textareaConsole.setFont(f);
			}
			JPanel panel = new JPanel();
			JButton bttnSave = new JButton(JAPMessages.getString("bttnSaveAs") + "...",
										   GUIUtils.loadImageIcon(JAPConstants.IMAGE_SAVE, true));
			bttnSave.setActionCommand("saveas");
			bttnSave.addActionListener(debug);
			JButton bttnCopy = new JButton(JAPMessages.getString("bttnCopy"),
										   GUIUtils.loadImageIcon(JAPConstants.IMAGE_COPY, true));
			bttnCopy.setActionCommand("copy");
			bttnCopy.addActionListener(debug);
			JButton bttnInsertConfig = new JButton(JAPMessages.getString("bttnInsertConfig"),
				GUIUtils.loadImageIcon(JAPConstants.IMAGE_COPY_CONFIG, true));
			bttnInsertConfig.setActionCommand("insertConfig");
			bttnInsertConfig.addActionListener(debug);
			JButton bttnDelete = new JButton(JAPMessages.getString("bttnDelete"),
											 GUIUtils.loadImageIcon(JAPConstants.IMAGE_DELETE, true));
			bttnDelete.setActionCommand("delete");
			bttnDelete.addActionListener(debug);
			JButton bttnClose = new JButton(JAPMessages.getString("bttnClose"),
											GUIUtils.loadImageIcon(JAPConstants.IMAGE_EXIT, true));
			bttnClose.setActionCommand("close");
			bttnClose.addActionListener(debug);
			GridBagLayout g = new GridBagLayout();
			panel.setLayout(g);
			GridBagConstraints c = new GridBagConstraints();
			c.insets = new Insets(5, 5, 5, 5);
			c.gridy = 0;
			c.gridx = 1;
			c.weightx = 0;
			g.setConstraints(bttnSave, c);
			panel.add(bttnSave);
			c.gridx = 2;
			g.setConstraints(bttnCopy, c);
			panel.add(bttnCopy);
			c.gridx = 3;
			g.setConstraints(bttnInsertConfig, c);
			panel.add(bttnInsertConfig);
			c.gridx = 4;
			g.setConstraints(bttnDelete, c);
			panel.add(bttnDelete);
			c.weightx = 1;
			c.anchor = GridBagConstraints.EAST;
			c.fill = GridBagConstraints.NONE;
			c.gridx = 5;
			g.setConstraints(bttnClose, c);
			panel.add(bttnClose);
			//panel.add("Center",new Canvas());
			m_frameConsole.getContentPane().add("North", panel);
			m_frameConsole.getContentPane().add("Center", new JScrollPane(m_textareaConsole));
			m_frameConsole.addWindowListener(m_winAdapter);
			m_frameConsole.pack();
			m_frameConsole.moveToUpRightCorner();
			m_frameConsole.setVisible(true);
			m_bConsole = true;
		}
	}

	

	public void actionPerformed(ActionEvent e)
	{
		if (e.getActionCommand().equals("saveas"))
		{
			saveLog();
		}
		else if (e.getActionCommand().equals("copy"))
		{
			m_textareaConsole.selectAll();
			m_textareaConsole.copy();
			m_textareaConsole.moveCaretPosition(m_textareaConsole.getCaretPosition());
//						PrintJob p=Toolkit.getDefaultToolkit().getPrintJob(JAPModel.getModel().getView(),"Print Log",null);
//						debug.textareaConsole.print(p.getGraphics());
//						p.end();
		}
		else if (e.getActionCommand().equals("delete"))
		{
			m_textareaConsole.setText("");
		}
		else if (e.getActionCommand().equals("insertConfig"))
		{
			try
			{
				Properties p = System.getProperties();
				//StringWriter s=new StringWriter();
				//p.list(new PrintWriter(s));
				Enumeration enumer = p.propertyNames();
				while (enumer.hasMoreElements())
				{
					String st = (String) enumer.nextElement();
					String value = p.getProperty(st);
					m_textareaConsole.append(st + ": " + value + "\n");
				}
//                textareaConsole.append(s.toString());
			}
			catch (Exception e1)
			{}
			m_textareaConsole.append("TotalMemory: " +
									 Util.formatBytesValueWithUnit(Runtime.getRuntime().totalMemory()) + "\n");
			try
			{
				Long result = (Long)
					Runtime.class.getMethod("maxMemory", new Class[0]).invoke(
									   Runtime.getRuntime(), new Object[0]);
				m_textareaConsole.append("MaxMemory: " +
										 Util.formatBytesValueWithUnit(result.longValue()) + "\n");
			}
			catch (Exception a_e)
			{
				// ignore
			}

			m_textareaConsole.append("FreeMemory: " +
									 Util.formatBytesValueWithUnit(Runtime.getRuntime().freeMemory()) + "\n");
			m_textareaConsole.append("\n");
			m_textareaConsole.append(JAPModel.getInstance().toString());
		}
		else
		{
			m_frameConsole.dispose();
			m_bConsole = false;
		}
	}

	private void saveLog()
	{
		JFileChooser fc = new JFileChooser();
		fc.setDialogType(JFileChooser.SAVE_DIALOG);
		int ret = fc.showDialog(JAPDebug.m_frameConsole.getRootPane(), null);
		if (ret == JFileChooser.APPROVE_OPTION)
		{
			File file = fc.getSelectedFile();
			try
			{
				FileWriter fw = new FileWriter(file);
				JAPDebug.m_textareaConsole.write(fw);
				fw.flush();
				fw.close();
			}
			catch (Exception e)
			{
				JAPDialog.showErrorDialog(JAPDebug.m_frameConsole, JAPMessages.getString("errWritingLog"),
										  LogType.MISC);
			}
		}
	}

//	/** Set the debugging output stream. Each debug level has his on outputstream. This defaults to System.out
//	 * @param level The debug level
//	 * @param out The assoziated otuput stream (maybe null)
//	 * @return true if succesful, false otherwise
//	*/
//	public static boolean setLevelOutputStream(int level, PrintWriter out) {
//	if(level<0 || level>JAPDebug.EMERG)
//		return false;
//	debug.outStreams[level]=out;
//	return true;
//	}

	/*	private static void printDebugSettings()
	 {
	  System.out.println("JAPDebug: debugtype =" + Integer.toString(debug.m_debugType));
	  System.out.println("JAPDebug: debuglevel=" + Integer.toString(debug.m_debugLevel));
	 }
	 */
}
