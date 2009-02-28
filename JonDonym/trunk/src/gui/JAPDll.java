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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Hashtable;
import java.util.StringTokenizer;

import java.awt.Component;
import java.awt.Frame;
import java.awt.Point;
import java.awt.Window;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.swing.filechooser.FileFilter;
import javax.swing.SwingUtilities;

import anon.util.ClassUtil;
import anon.util.RecursiveFileTool;
import anon.util.ResourceLoader;
import anon.util.Util;
import gui.dialog.JAPDialog;
import jap.JAPController;
import jap.JAPModel;
import jap.JAPConstants;
import jap.SystrayPopupMenu;
import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;
import javax.swing.JDialog;
import platform.AbstractOS;

final public class JAPDll {

	public static final String MSG_IGNORE_UPDATE = JAPDll.class.getName() + "_ignoreUpdate";
	
	//required japdll.dll version for this JAP-version
	public static final String JAP_DLL_REQUIRED_VERSION = "00.04.009";
	public static final String START_PARAMETER_ADMIN = "--dllAdminUpdate";
	
	private static final String UPDATE_PATH;

	private static final String DLL_LIBRARY_NAME = "japdll";
	private static final String DLL_LIBRARY_NAME_32bit = DLL_LIBRARY_NAME;
	private static final String DLL_LIBRARY_NAME_64bit = DLL_LIBRARY_NAME + "_x64";
	private static final String JAP_DLL = DLL_LIBRARY_NAME + ".dll";
	private static final String JAP_DLL_NEW_32bit  = JAP_DLL + "." + JAP_DLL_REQUIRED_VERSION;
	private static final String JAP_DLL_NEW_64bit  = JAP_DLL + "." + JAP_DLL_REQUIRED_VERSION;
	private static final String JAP_DLL_OLD = DLL_LIBRARY_NAME + ".old";

	/** Messages */
	private static final String MSG_DLL_UPDATE = JAPDll.class.getName() + "_updateRestartMessage";
	private static final String MSG_DLL_UPDATE_SUCCESS_ADMIN = JAPDll.class.getName() + "_dllUpdateSuccessAdmin";
	private static final String MSG_DLL_UPDATE_FAILED = JAPDll.class.getName() + "_updateFailed";
	private static final String MSG_CONFIRM_OVERWRITE = JAPDll.class.getName() + "_confirmOverwrite";
	private static final String MSG_PERMISSION_PROBLEM = JAPDll.class.getName() + "_permissionProblem";
	private static final String MSG_COULD_NOT_SAVE = JAPDll.class.getName() + "_couldNotSave";
	
	

	private static Hashtable ms_hashOnTop = new Hashtable();
	private static boolean ms_bInTaskbar = false;

	private static final Object SYNC_POPUP = new Object();
	private static SystrayPopupMenu ms_popupMenu;
	private static Window ms_popupWindow;

	private static boolean m_sbHasOnTraffic = true;
	private static boolean m_bStartedAsAdmin = false;

	private static void loadDll()
	{
		//Load either 32 bit or 64 bit version of dll
		try
			{
				System.loadLibrary(DLL_LIBRARY_NAME_32bit);
			}
		catch(Throwable t)
			{
				
			}
		try
			{
				System.loadLibrary(DLL_LIBRARY_NAME_64bit);
			}
		catch(Throwable t)
			{
				
			}
	}
	static
	{
		File japdir = ClassUtil.getClassDirectory(JAPDll.class);
		if (japdir == null)
		{
			String strUpdatePath = null;
			// the update method might not work; maybe this is Java Webstart?
			try
			{
				strUpdatePath = System.getProperty("user.dir", (String)null);
			}
			catch (Throwable a_e)
			{
				a_e.printStackTrace();
			}
			UPDATE_PATH = strUpdatePath;
		}
		else
		{
			UPDATE_PATH = japdir.getParent();
		}
	}

	public static void init(boolean a_bStartedAsAdmin, String a_username, Window a_window)
	{
		String strOSName = System.getProperty("os.name", "");
		
		m_bStartedAsAdmin = a_bStartedAsAdmin;
		try
		{
			if (strOSName == null // may be null in Java Webstart since version 1.6.0
				|| strOSName.toLowerCase().indexOf("win") > -1)
			{
				GUIUtils.setNativeGUILibrary(new GUIUtils.NativeGUILibrary()
				{
					public boolean setAlwaysOnTop(Window a_window, boolean a_bOnTop)
					{
						return setWindowOnTop(a_window, a_bOnTop);
					}

					public boolean isAlwaysOnTop(Window a_window)
					{
						return isWindowOnTop(a_window);
					}
				});
				
				
//				delete any temporary dll that might remain somewhere from an earlier manual update attempt
				try
				{
					String tempDir = AbstractOS.getInstance().getTempPath();
					File tempFile;
					if (tempDir != null)
					{
						tempFile = new File(tempDir + DLL_LIBRARY_NAME);
						if (tempFile.exists())
						{
							if (!RecursiveFileTool.deleteRecursion(tempFile))
							{
								throw new Exception("Delete recursive");
							}
						}
					}
				}
				catch (Throwable a_e)
				{
					LogHolder.log(LogLevel.EXCEPTION, LogType.MISC, "Could not delete temporary DLL!", a_e);
				}
				
				boolean bUpdateDone = false;
				if (getUpdatePath() != null && JAPModel.getInstance().getDllUpdatePath() != null)
				{
					update(a_window);
					bUpdateDone = true;					
				}

				loadDll();								

				if (getUpdatePath() == null)
				{
					// ignore the update methods
					LogHolder.log(LogLevel.ERR, LogType.GUI,
								  "Could not get DLL update path. Maybe Java Webstart?");
					return;
				}																

				String version = JAPDll.getDllVersion();
				if (bUpdateDone && (version == null || // == null means there were problems...
									version.compareTo(JAP_DLL_REQUIRED_VERSION) < 0))
				{
					// update was not successful
					 JAPModel.getInstance().setDLLupdate(getUpdatePath());
					 JAPController.getInstance().saveConfigFile();
				}
				else
				{
					JAPModel.getInstance().setDLLupdate(null);
				}
				
				JAPController.getInstance().addProgramExitListener(new JAPController.ProgramExitListener()
				{
					public void programExiting()
					{
						try
						{
							if (getDllVersion() != null)
							{
								hideSystray_dll();
							}
						}
						catch (Throwable a_e)
						{
							LogHolder.log(LogLevel.EXCEPTION, LogType.GUI, a_e);
						}
					}
				});
				
				
				if (m_bStartedAsAdmin && a_username != null)
				{
					// copy the current jar file to the program directory
					File filePhysical = ClassUtil.getClassDirectory(JAPDll.class);
					File fileVirtual;
					
					if (filePhysical != null && filePhysical.getPath().endsWith(".jar"))
					{
						fileVirtual = new File (a_username + "\\AppData\\Local\\VirtualStore" +
								filePhysical.getPath().substring(2, filePhysical.getPath().length()));
			
						if (fileVirtual.exists() && !fileVirtual.equals(filePhysical))
						{										
							String pathToJava = AbstractOS.getInstance().getProperty("java.home") + 
								File.separator + "bin" + File.separator + "javaw -jar ";
							String line1, line2;
							BufferedReader reader = 
								new BufferedReader(new InputStreamReader(
										Runtime.getRuntime().exec(pathToJava + "\"" + fileVirtual.getPath() + "\" --version").getInputStream()));
							line1 = reader.readLine();
						
							reader = 
								new BufferedReader(new InputStreamReader(
										Runtime.getRuntime().exec(pathToJava + "\"" + filePhysical.getPath() + "\" --version").getInputStream()));
							line2 = reader.readLine();
							
						
							if (line1 != null && line2 != null && !line1.equals(line2))
							{
								/*
								 *  Copy the file from the virtual store to the 
								 *  execution directory and restart.
								 */
								Util.copyStream(new FileInputStream(fileVirtual), 
									new FileOutputStream(filePhysical));
								final JAPController.IRestarter origRestarter = 
									JAPController.getInstance().getRestarter();
								JAPController.getInstance().setRestarter(
										new JAPController.IRestarter()
										{
											public void exec(String[] a_args) throws IOException
											{
												origRestarter.exec(a_args);
											}
											public boolean isConfigFileSaved()
											{
												// prevent an older version from distroying the config file
												return false;
											}
											public boolean hideWarnings()
											{
												return true;
											}
										});
								JAPController.goodBye(false); // restart and try to update DLL
							}
						}
					}					
				}				
			}
		}
		catch (Throwable a_t)
		{
			LogHolder.log(LogLevel.EXCEPTION, LogType.GUI, a_t);
		}
	}


	/**
	 * This method should be invoked on every JAP-start:
	 * It will check if the existing japdll.dll has the right version to work
	 * proper with this JAP version. If no japdll.dll exists at all, nothing will happen.
	 * If the japdll.dll has the wrong version, a backup of the old file is created and
	 * the suitable japdll.dll is extracted from the JAP.jar.
	 * In this case the user must restart the JAP.
	 * @param a_bShowDialogAndCloseOnUpdate if, in case of a neccessary update, a dialog is shown and JAP
	 * is closed
	 */
	public static void checkDllVersion(boolean a_bShowDialogAndCloseOnUpdate)
	{
		if (System.getProperty("os.name", "").toLowerCase().indexOf("win") < 0)
		{
			return;
		}

		LogHolder.log(LogLevel.INFO, LogType.GUI, "Existing " + JAP_DLL + " version: " + JAPDll.getDllVersion());
		LogHolder.log(LogLevel.INFO, LogType.GUI, "Required " + JAP_DLL + " version: " + JAP_DLL_REQUIRED_VERSION);

		// checks, if the japdll.dll must (and can) be extracted from jar-file.
		if (JAPDll.getDllVersion() != null && // != null means that there is a loaded dll
			JAPDll.getDllVersion().compareTo(JAP_DLL_REQUIRED_VERSION) < 0 &&
			ResourceLoader.getResourceURL(JAP_DLL_NEW_32bit) != null &&
			getUpdatePath() != null) // null means there is no new dll available
		{
			// check, if NO japdll.dll exists in jar-path
			File file = new File(getDllFileName());
			if (!file.exists())
			{
				askUserWhatToDo();
				return;
			}

			// tried to update AND there is still a problem
			if (JAPModel.getInstance().getDllUpdatePath() != null)
			{
				if (a_bShowDialogAndCloseOnUpdate)
				{
					askUserWhatToDo();
				}
				return;
			}

			// try to update, perhaps it even works right now when the dll is loaded
			if (update(JAPController.getInstance().getCurrentView()) && 
				JAPDll.getDllVersion() != null && // == null means that there were problems...
				JAPDll.getDllVersion().compareTo(JAP_DLL_REQUIRED_VERSION) < 0)
			{
				// update was successful
				LogHolder.log(LogLevel.INFO, LogType.GUI,
							  "Update successful, existing " + JAP_DLL + " version: " + JAPDll.getDllVersion());
				loadDll();
				if ( JAPDll.getDllVersion().compareTo(JAP_DLL_REQUIRED_VERSION) < 0 )
				{					
					a_bShowDialogAndCloseOnUpdate = true; // reloading the dll failed; recommend restart 
				}
				else
				{
					return;
				}
			}

			//write a flag to the jap.conf, that at the next startup the dll must be extracted from jar-file
			JAPModel.getInstance().setDLLupdate(getUpdatePath());
			JAPController.getInstance().saveConfigFile();
			if (a_bShowDialogAndCloseOnUpdate)
			{
				informUserAboutJapRestart();
			}
		}
		else
		{
			// version status OK
			// OR no dll loaded
			// OR no new-dll in jar-file
			if (JAPModel.getInstance().getDllUpdatePath() != null)
			{
				JAPModel.getInstance().setDLLupdate(null);
				JAPController.getInstance().saveConfigFile();
			}
		}
	}

	private static boolean update(Component a_window)
	{
		if (renameDLL(JAP_DLL, JAP_DLL_OLD) && extractDLL(new File(getDllFileName())))
		{
			JAPModel.getInstance().setDLLupdate(null);
			JAPController.getInstance().saveConfigFile();
			
			if (m_bStartedAsAdmin)
			{
				// we should switch back to user mode; therefore, close the program now
				if (a_window != null)
				{					
					JAPDialog.showMessageDialog(a_window,
							JAPMessages.getString(MSG_DLL_UPDATE_SUCCESS_ADMIN));
				}
				final JAPController.IRestarter origRestarter = JAPController.getInstance().getRestarter(); 
				JAPController.getInstance().setRestarter(
					new JAPController.IRestarter()
					{
						public void exec(String[] a_args) throws IOException
						{
							origRestarter.exec(a_args);
						}
						public boolean isConfigFileSaved()
						{
							return true;
						}
						public boolean hideWarnings()
						{
							return true;
						}
					});
				
				JAPController.goodBye(true);
			}
			
			return true;
		}
		else
		{
			renameDLL(JAP_DLL_OLD, JAP_DLL);
			return false;
		}
	}

	/**
	 * Renames the existing japdll.dll to japdll.old before the new DLL is extracted from jar-file
	 * @param a_oldName old name of the file
	 * @param a_newName new name of the file
	 */
	private static boolean renameDLL(String a_oldName, String a_newName)
	{
		try
		{
			File file = new File(getUpdatePath() + File.separator + a_oldName);
			if(file.exists())
			{
				file.renameTo(new File(getUpdatePath() + File.separator + a_newName));
				//Util.copyStream(new FileInputStream(file), 
						//new FileOutputStream(new File(getUpdatePath() + File.separator + a_newName)));
								
				return true;
			}
			else 
			{
				//if the file does not exist, but a dll was loaded
				return false;
			}

		}
		catch (Exception e)
		{
			LogHolder.log(LogLevel.ERR, LogType.GUI, "Unable to copy " + getUpdatePath() + 
					File.separator + a_oldName + ".", e);
		}
		return false;
   }

   /**
	* Extracts the japdll.dll from the jar-file to the given file
	* @param a_file File
	* @return boolean
	*/
   private static boolean extractDLL(File a_file)
   {
		LogHolder.log(LogLevel.DEBUG, LogType.GUI, "Extracting " + JAP_DLL_NEW_32bit + 
				" from jar-file to: " + a_file);

		try
		{
			Util.copyStream(
					ResourceLoader.loadResourceAsStream(JAP_DLL_NEW_32bit),
					new FileOutputStream(a_file));
		
			return true;
		}
		catch (Exception e)
		{
			LogHolder.log(LogLevel.EXCEPTION, LogType.MISC, e);
			//unable to write to file, perhaps a permissions problem
			//JAPDialog.showMessageDialog( JAPController.getView(),
			//JAPMessages.getString(MSG_PERMISSION_PROBLEM, "'" + a_file + "'") );
		}
		return false;
	}

	/**
	 * This method should be invoked if there was no possibility to so a successfull dll update:
	 * Reasons can be:
	 * - japdll.dll cannot be written (e.g. not enough user privileges)
	 * - a japdll.dll is loaded, but there is no japdll.dll in the jar-path (japdll.dll is e.g. in Windows-System-Directory)
	 * - japdll.dll update was successfull, but the version loaded is still the wrong
	 *   (this can be happen, if there is a old japdll.dll in the Windows-System-Directory AND in the jar-path
	 */
	private static void askUserWhatToDo()
	{
		if (!JAPModel.getInstance().isDLLWarningActive())
		{
			return;
		}
		
		
		JAPDialog.LinkedCheckBox checkbox = new JAPDialog.LinkedCheckBox(
				JAPMessages.getString(MSG_IGNORE_UPDATE), false);
		
		String[] args = new String[2];
		args[0] = JAP_DLL;
		args[1] = getUpdatePath();
		int answer =
			JAPDialog.showConfirmDialog(JAPController.getInstance().getCurrentView(),
										JAPMessages.getString(MSG_DLL_UPDATE_FAILED, args) + "<br>&nbsp;",
										JAPMessages.getString(JAPDialog.MSG_TITLE_ERROR),
										JAPDialog.OPTION_TYPE_OK_CANCEL, JAPDialog.MESSAGE_TYPE_WARNING,
										checkbox);
		
		JAPModel.getInstance().setDllWarning(!checkbox.getState());
		
		if (answer == JAPDialog.RETURN_VALUE_OK)
		{
			JAPController.getInstance().setRestarter(new JAPController.IRestarter()
			{
				public boolean isConfigFileSaved()
				{
					/*
					if (JAPModel.getInstance().isDLLupdated())
					{
						JAPModel.getInstance().setDLLupdate(false);
					}*/
					return true;
				}
				
				public boolean hideWarnings()
				{
					return false;
				}
				
				public void exec(String[] a_args) throws IOException
				{
					String command = null;
					String parameters = "";
					String userhome = AbstractOS.getInstance().getProperty("user.home");
					
					if (a_args != null && a_args.length > 1)
					{
						command = "\"" + a_args[0] + "\"";
						for (int i = 1; i < a_args.length; i++)
						{
							if (new StringTokenizer(a_args[i]).countTokens() > 1)
							{
								// escape white spaces
								parameters += " \"" + a_args[i] + "\"";
							}
							else
							{
								parameters += " " + a_args[i];
							}
							//parameters += a_args[i];
						}
						if (!m_bStartedAsAdmin)
						{
							parameters += " " + START_PARAMETER_ADMIN;
							if (userhome != null)
							{
								parameters += " " + userhome;
							}
						}
					}
					
					if (command == null || !shellExecute(command, parameters, true))
					{
						showExplorerFiles();
					}
				}
				
				private void showExplorerFiles()
				{
					boolean bTmpDirCreated = false;
					String tempDir;
					File tempDirFile;
					tempDir = AbstractOS.getInstance().getTempPath();
					if (tempDir == null)
					{
						tempDir = AbstractOS.getInstance().getConfigPath(JAPConstants.APPLICATION_NAME);
					}
					tempDir += DLL_LIBRARY_NAME + File.separator;

					try
					{
						tempDirFile = new File(tempDir);
						if (tempDirFile.exists() && !tempDirFile.isDirectory())
						{
							tempDirFile.delete();
						}
						if (!tempDirFile.exists())
						{
							bTmpDirCreated = new File(tempDir).mkdir();
						}
						else
						{
							bTmpDirCreated = true;
						}
					}
					catch (SecurityException a_e)
					{
						LogHolder.log(LogLevel.EXCEPTION, LogType.MISC, 
								"Could not create temporary directory!", a_e);
					}
					
					if (bTmpDirCreated && extractDLL(new File(tempDir + JAP_DLL)))
					{
						try
						{
							Runtime.getRuntime().exec(new String[]{"CMD", "/C", "EXPLORER.EXE", tempDir});				
							Runtime.getRuntime().exec(new String[]{"CMD", "/C", "EXPLORER.EXE", getUpdatePath()});
						}
						catch (IOException e)
						{
							LogHolder.log(LogLevel.EXCEPTION, LogType.MISC, e);
						}				
					}
				}
			});			
			
			JAPController.goodBye(false);
		}
	}


	/**
	 * informs the user, that the JAP must be restarted, in order to finish the update
	 */
	private static void informUserAboutJapRestart()
	{
		//Inform the User about the necessary JAP restart
		JAPDialog.showMessageDialog(JAPController.getInstance().getCurrentView(),
									JAPMessages.getString(MSG_DLL_UPDATE, "'" + JAP_DLL + "'"));
		//close JAP
		JAPController.goodBye(false);
	}

	/**
	 * Checks if the onTop method of the dll has been used for this window.
	 * @param a_window Window
	 * @return if the onTop method of the dll has been used for this window
	 */
	private static boolean isWindowOnTop(Window a_window)
	{
		if (a_window == null)
		{
			return false;
		}
		return ms_hashOnTop.contains(a_window.getName());
	}

	private static boolean setWindowOnTop(Window theWindow, boolean onTop)
	{
		if (theWindow == null)
		{
			return false;
		}

		//theWindow.setName(Double.toString(Math.random()));
		String caption = theWindow.getName();
		if (caption == null)
		{
			return false;
		}

		try
		{
			synchronized (ms_hashOnTop)
			{
				setWindowOnTop_dll(caption, onTop);
				if (onTop)
				{
					ms_hashOnTop.put(caption, caption);
				}
				else
				{
					ms_hashOnTop.remove(caption);
				}
			}
			return true;
		}
		catch (Throwable t)
		{
		}
		return false;
	}

	public static synchronized boolean showWindowFromTaskbar()
	{
		try
		{
			if (ms_bInTaskbar)
			{
				ms_bInTaskbar = false;
				boolean value = showWindowFromTaskbar_dll();
				showMainWindow();
				ms_bInTaskbar = !value;
				return value;
			}
			return false;
		}
		catch (Throwable t)
		{
			return false;
		}
	}


	public static synchronized boolean hideWindowInTaskbar(String caption)
	{
		try
		{
			boolean bHidden = hideWindowInTaskbar_dll(caption);
			if (!ms_bInTaskbar)
			{
				ms_bInTaskbar = bHidden;
			}
			return bHidden;
		}
		catch (Throwable t)
		{
			return false;
		}
	}

	static public boolean setWindowIcon(String caption)
	{
		try
		{
			return setWindowIcon_dll(caption);
		}
		catch (Throwable t)
		{
			return false;
		}
	}

	static public boolean onTraffic()
	{
		if (m_sbHasOnTraffic)
		{
			try
			{
				onTraffic_dll();
				return true;
			}
			catch (Throwable t)
			{
				m_sbHasOnTraffic = false;
				return false;
			}
		}
		return false;
	}

	/* Warning: This method is accessed using reflections from platform.WindowsOS!
	 * Have this in mind if you change the interface of this method!
	 */
	public static boolean xcopy(File a_file, File a_directory, boolean a_asAdmin)
	{
		if (a_file == null || a_directory == null || !a_directory.isDirectory())
		{
			return false;
		}
		
		String dirSwitch = "";
		String parameters;
		if (a_file.isDirectory())
		{
			dirSwitch = "/E ";
		}
		
		parameters = " /Y /R /Q /I /H " + dirSwitch + "\"" + a_file + "\" \"" + a_directory + "\"";
		//parameters = " /F /L /W /P /V /R /I " + dirSwitch + "\"" + a_file + "\" \"" + a_directory + "\"";
		LogHolder.log(LogLevel.NOTICE, LogType.MISC, "Doing xcopy: " + parameters);
		
		return shellExecute("xcopy", parameters, a_asAdmin);
	}
	
	public static String getDllVersion()
	{
		String version = null;
		int number;
		try
		{
			version = getDllVersion_dll();
			StringTokenizer tokenizer = new StringTokenizer(version,",");
			
			
			if (tokenizer.countTokens() > 1)
			{
				version = "";
		
				number = Integer.parseInt(tokenizer.nextToken());
				if (number < 10)
				{
					version += "0";
				}
				version += number + ".";
				
				number = Integer.parseInt(tokenizer.nextToken());
				if (number < 10)
				{
					version += "0";
				}
				version += number + ".";
				
				number = Integer.parseInt(tokenizer.nextToken());
				if (number < 10)
				{
					version += "0";
				}
				if (number < 100)
				{
					version += "0";
				}
				version += number;
			}
		}
		catch (Throwable t)
		{
		}
		return version;
	}

	/**
	 * Returns the path where the dll should be stored into.
	 * @return
	 */
	private static String getUpdatePath()
	{
		String fileDLL = getDllFileName();
		if (fileDLL != null)
		{
			fileDLL = (new File(fileDLL)).getParent();
		}
		return fileDLL;
	}
	
	/** Returns the file name of the JAPDll.
	 * @ret filename pf the JAP dll
	 * @ret null if getting the file name fails
	 */	
	static public String getDllFileName()
	{
		String strFileName = JAPModel.getInstance().getDllUpdatePath();
		if (strFileName == null)
		{		
			try
			{
				String s=getDllFileName_dll();
				if(s==null||s.length()==0)
					return null;
				return s;
			}
			catch (Throwable t)
			{
			}
			strFileName = UPDATE_PATH;
		}
		
		
		if (strFileName != null)
		{
			if (!strFileName.endsWith(File.separator))
			{
				strFileName += File.separator + JAP_DLL;
			}
			else
			{
				strFileName += JAP_DLL;
			}			
		}
		return strFileName;
	}

	static public long showMainWindow()
	{
		Window view = JAPController.getInstance().getViewWindow();
		view.setVisible(true);
		view.toFront();
		view.repaint();
		return 0;
	}

	static public long closePopupMenu()
	{
		synchronized (SYNC_POPUP)
		{
			if (ms_popupMenu != null)
			{
				Runnable run = new Runnable()
				{
					public void run()
					{
						ms_popupMenu.setVisible(false);
						ms_popupWindow.setVisible(false);
						//ms_popupWindow.dispose();
					}
				};

				if (SwingUtilities.isEventDispatchThread())
				{
					run.run();
				}
				else
				{
					SwingUtilities.invokeLater(run);
				}
			}
		}
		return 0;
	}

	private static final String STR_HIDDEN_WINDOW = Double.toString(Math.random());

	static public long showPopupMenu(long a_x, long a_y)
	{
		synchronized (SYNC_POPUP)
		{
			if (ms_popupWindow == null)
			{
				//ms_popupWindow = new JWindow(new Frame(STR_HIDDEN_WINDOW));
				// needed for JDK > 1.6.0_02 ... (J)Window is no more compatible with PopupMenus!
				ms_popupWindow = new JDialog(new Frame(STR_HIDDEN_WINDOW), false);

				ms_popupWindow.setName(STR_HIDDEN_WINDOW);
				ms_popupWindow.pack();
				ms_popupWindow.setLocation(20000, 20000); // needed for JDK > 1.6.0_02 to hide dialog
			}

			Point mousePoint = new Point( (int) a_x, (int) a_y);

			ms_popupMenu = new SystrayPopupMenu(new SystrayPopupMenu.MainWindowListener()
			{
				public void onShowMainWindow()
				{
					showWindowFromTaskbar();
				}

				public void onShowHelp()
				{

				}
				public void onShowSettings(final String card, final Object a_value)
				{
					//showWindowFromTaskbar();
					new Thread(new Runnable()
					{
						public void run()
						{
							JAPController.getInstance().showConfigDialog(card, a_value);
						}
					}).start();
				}
			});
			//ms_popupWindow.setLocation(mousePoint);

			GUIUtils.setAlwaysOnTop(ms_popupWindow, true);
			ms_popupWindow.setVisible(true);
			ms_popupMenu.addPopupMenuListener(new PopupMenuListener()
			{
				public void popupMenuWillBecomeVisible(PopupMenuEvent e)
				{
				}

				public void popupMenuWillBecomeInvisible(PopupMenuEvent e)
				{
					//ms_popupWindow.setVisible(false);
					//ms_popupWindow.dispose();
					popupClosed_dll();
				}

				public void popupMenuCanceled(PopupMenuEvent e)
				{
				}
			});
			Point preferredLocation = new Point(mousePoint.x, mousePoint.y - ms_popupMenu.getHeight());
			ms_popupMenu.show(ms_popupWindow, preferredLocation);
			ms_popupMenu.repaint();

			return 0;
		}
	}

	public static void setSystrayTooltip(String a_strTooltipText)
	{
		if (a_strTooltipText == null)
		{
			return;
		}

		if (a_strTooltipText.length() >= 60)
		{
			a_strTooltipText = a_strTooltipText.substring(0, 60);
		}
		a_strTooltipText = a_strTooltipText.trim();
		if (a_strTooltipText.length() == 0)
		{
			return;
		}

		try
		{
			setTooltipText_dll(a_strTooltipText);
		}
		catch (Throwable a_e)
		{
			// ignore
		}
	}
	
	public static boolean shellExecute(String a_command, String a_parameters, boolean a_bAsAdmin)
	{
		boolean result;
		
		try
		{
			result = shellExecute_dll(a_command, a_parameters, a_bAsAdmin);
		}
		catch (Throwable a_e)
		{
			LogHolder.log(LogLevel.EXCEPTION, LogType.GUI, a_e);
			result = false;
		}
		return result;		
	}

	native static private void setWindowOnTop_dll(String caption, boolean onTop);

	native static private boolean hideWindowInTaskbar_dll(String caption);

	native static private boolean showWindowFromTaskbar_dll();

	native static private boolean setTooltipText_dll(String a_strTooltipText);

	native static private boolean setWindowIcon_dll(String caption);

	native static private void onTraffic_dll();

	native static private void popupClosed_dll();

	native static private void hideSystray_dll();

	native static private String getDllVersion_dll();

	native static private String getDllFileName_dll();
	
	native static private boolean shellExecute_dll(String a_command, String a_parameters, boolean a_bAsAdmin);


	// http://www.heimetli.ch/shellexec.html
	// http://blogs.msdn.com/vistacompatteam/archive/2006/09/25/771232.aspx
	
	private static class MyFileFilter extends FileFilter
	{
		public static final String DLL_EXTENSION = ".dll";
		private final String ACCOUNT_DESCRIPTION = "JAP dll file (*" + DLL_EXTENSION + ")";

		private int filterType;

		public int getFilterType()
		{
			return filterType;
		}

		public boolean accept(File f)
		{
			return f.isDirectory() || f.getName().endsWith(DLL_EXTENSION);
		}

		public String getDescription()
		{
			return ACCOUNT_DESCRIPTION;
		}
	}

}
