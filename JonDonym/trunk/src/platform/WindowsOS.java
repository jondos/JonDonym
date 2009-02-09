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
package platform;

//import gui.JAPDll;

import java.io.File;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.StringTokenizer;

import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;

import java.lang.reflect.Method;
import java.net.URL;

import anon.util.RecursiveFileTool;

/**
 * This class is instantiated by AbstractOS if the current OS is Windows
 */
public class WindowsOS extends AbstractOS
{
	public static final int HKEY_CLASSES_ROOT = 0x80000000;
	public static final int HKEY_CURRENT_USER = 0x80000001;
	public static final int HKEY_LOCAL_MACHINE = 0x80000002;

	public static final int DELETE = 0x10000;
	public static final int KEY_QUERY_VALUE = 0x0001;
	public static final int KEY_SET_VALUE = 0x0002;
	public static final int KEY_CREATE_SUB_KEY = 0x0004;
	public static final int KEY_ENUMERATE_SUB_KEYS = 0x0008;
	public static final int KEY_READ = 0x20019;
	public static final int KEY_WRITE = 0x20006;
	public static final int KEY_ALL_ACCESS = 0xf003f;

	public static final int ERROR_SUCCESS = 0;
	public static final int ERROR_FILE_NOT_FOUND = 2;
	public static final int ERROR_ACCESS_DENIED = 5;

	public WindowsOS() throws Exception
	{
		String osName = System.getProperty("os.name", "").toLowerCase();
		if (osName.indexOf("win") == -1)
		{
			throw new Exception("Operating system is not Windows");
		}

		if (osName.indexOf("windows 9") > -1)
		{
			initEnv("command.com /c set");
		}
		else
		{
			initEnv("cmd.exe /c set");
		}
	}

	protected boolean openLink(String a_link)
	{
		try
		{
			// java.awt.Desktop.getDesktop().browse(new java.net.URI(a_link));
			Runtime.getRuntime().exec(
					"rundll32 url.dll,FileProtocolHandler " + a_link);
			// Runtime.getRuntime().exec(
			// new String[]{"C:\\Program Files\\Mozilla Firefox\\firefox.exe",
			// a_link});

			return true;
		}
		catch (Exception ex)
		{
			LogHolder.log(LogLevel.ERR, LogType.MISC, "Cannot open '" + a_link
					+ "' in Windows default program.", ex);
		}

		return false;
	}

	protected String getAsString(URL a_url)
	{
		String strURL = super.getAsString(a_url);
		if (new StringTokenizer(strURL).countTokens() > 1)
		{
			return "\"" + strURL + "\"";
		}
		else
		{
			return strURL;
		}
	}

	public boolean isHelpAutoInstalled()
	{
		return true;
	}

	public String getDefaultHelpPath(String a_applicationName)
	{
		// String dir = getEnvPath(a_applicationName, "ALLUSERSPROFILE");
		// ALLUSERSPROFILE has become virtual in Vista
		String dir = getAppdataDefaultDirectory(a_applicationName);
		if (dir == null)
		{
			dir = super.getDefaultHelpPath(a_applicationName);
		}

		return dir;
	}

	public String getConfigPath(String a_applicationName)
	{
		String vendor = System.getProperty("java.vendor", "unknown");
		String dir = "";
		if (vendor.trim().toLowerCase().startsWith("microsoft"))
		{
			try
			{
				BufferedReader winPropertiesReader = new BufferedReader(
						new InputStreamReader(Runtime.getRuntime().exec(
								"CMD /C SET").getInputStream()));
				String line;
				while ((line = winPropertiesReader.readLine()) != null)
				{
					if (line.startsWith("USERPROFILE"))
					{
						break;
					}
				}
				if (line != null)
				{
					StringTokenizer tokenizer = new StringTokenizer(line, "=");
					tokenizer.nextToken();
					dir = tokenizer.nextToken().trim();
				}
			}
			catch (Exception a_e)
			{
			}
			if (dir == null)
			{
				dir = System.getProperty("user.dir");
			}
		}
		else
		{
			dir = getAppdataDefaultDirectory(a_applicationName);
			if (dir == null)
			{
				dir = System.getProperty("user.home");
			}
		}

		return dir + File.separator;
	}

	public String getAppdataDefaultDirectory(String a_applicationName)
	{
		return getEnvPath(a_applicationName, "APPDATA");
	}

	public boolean copyAsRoot(File a_sourceFile, File a_targetDirectory, AbstractRetryCopyProcess a_checkRetry)
	{
		/*
		 * Removed dependency to JAPDll --> otherwise we will need the whole JAP
		 * Code for the MixConfig Tool..
		 * 
		 * Note that xcopy copies asynchronous, so that we have to wait for it finishing in a loop.
		 * 
		 * The original call was: return JAPDll.xcopy(a_sourceFile, a_targetDirectory, true);
		 */
		try
		{
			Class c = Class.forName("gui.JAPDll");
			Class arArgClasses[] = new Class[3];
			arArgClasses[0] = File.class;
			arArgClasses[1] = File.class;
			arArgClasses[2] = boolean.class;
			Method methodXcopy = c.getMethod("xcopy", arArgClasses);
			Object args[] = new Object[3];
			args[0] = a_sourceFile;
			args[1] = a_targetDirectory;
			args[2] = Boolean.TRUE;
						
			File fileToCopy = new File(a_targetDirectory.getPath() + File.separator + a_sourceFile.getName());
			byte[] md5OldFile;
			long sizeSource = RecursiveFileTool.getFileSize(a_sourceFile);
			long sizeOldFile = fileToCopy.length();
			try
			{
				md5OldFile = RecursiveFileTool.createMD5Digest(fileToCopy);
			}
			catch (Exception a_e)
			{
				LogHolder.log(LogLevel.EXCEPTION, LogType.MISC, a_e);
				md5OldFile = null;
			}
		
			boolean ret = ((Boolean) methodXcopy.invoke(null, args)).booleanValue();
			boolean bChanceToRetry = false;
			long lastSize, currentSize;
			
			
			//LogHolder.log(LogLevel.EXCEPTION, LogType.MISC, "Started copy: " + fileToCopy.getAbsolutePath() + " Exists: " + fileToCopy.exists());
			
			if (!ret)
			{
				if (a_checkRetry != null && a_checkRetry.checkRetry())
				{
					return copyAsRoot(a_sourceFile, a_targetDirectory, a_checkRetry);
				}
				LogHolder.log(LogLevel.ERR, LogType.MISC, "Root copy failed!");
				
				return false;
			}
			
			if (a_checkRetry == null || a_checkRetry.getMaxProgressSteps() <= 0)
			{
				// we cannot be sure whether we succeeded, but ok...
				return true;
			}
			
			//LogHolder.log(LogLevel.EXCEPTION, LogType.MISC, "Waiting for copy...");
			
			lastSize = -1 * a_checkRetry.getMaxProgressSteps();
			currentSize = lastSize;
			while (a_checkRetry.incrementProgress())
			{
				try
				{
					
					if (currentSize > 0 || !RecursiveFileTool.equals(fileToCopy, md5OldFile, sizeOldFile))
					{
						currentSize = RecursiveFileTool.getFileSize(fileToCopy);
					}
					else
					{
						currentSize++;
					}
				
					if (currentSize == sizeSource)
					{
						if (RecursiveFileTool.equals(fileToCopy, a_sourceFile, true))
						{
							// Copy successful!
							while (a_checkRetry.incrementProgress());
							return true;
						}
						else
						{//LogHolder.log(LogLevel.EXCEPTION, LogType.MISC, "failed compare");
							// Copying failed!
							LogHolder.log(LogLevel.ERR, LogType.MISC, "Root copy failed!");
							bChanceToRetry = true;
							break;
						}
					}
					else if (((a_checkRetry.getCurrentStep() > 1 || a_checkRetry.getMaxProgressSteps() == 1) &&
							 currentSize <= lastSize)) // file size did not change since last run)
					{//LogHolder.log(LogLevel.EXCEPTION, LogType.MISC, "failed size; Current: " + currentSize + " Last: " + lastSize + " Source: " + RecursiveFileTool.getFileSize(a_sourceFile) + " Step: " + a_checkRetry.getCurrentStep());
						// invalid file size state; copying seems to have failed
						LogHolder.log(LogLevel.ERR, LogType.MISC, "Root copy failed!");
						bChanceToRetry = true;
						break;
					}		
					lastSize = currentSize; // check if file size has changed in next loop
				}
				catch (SecurityException a_e)
				{
					// copying may have succeeded, but we cannot verify that
					LogHolder.log(LogLevel.ERR, LogType.MISC, a_e);
					bChanceToRetry = true;
					break;
				}
			
				Thread.sleep(a_checkRetry.getProgressLoopWaitMilliseconds());
			}
			
			//LogHolder.log(LogLevel.EXCEPTION, LogType.MISC, "Finished copy loop!");
			
			if (RecursiveFileTool.equals(fileToCopy, a_sourceFile, true))
			{
				return true;
			}
			else if (bChanceToRetry && a_checkRetry.checkRetry())
			{
				a_checkRetry.reset();
				return copyAsRoot(a_sourceFile, a_targetDirectory, a_checkRetry);
			}
			
			// timeout; we are unsure whether copying succeeded, but we should not interrupt it
		}
		catch (Throwable t)
		{
			LogHolder.log(LogLevel.EXCEPTION, LogType.MISC, t);
		}
		return false;
	}

	public String getTempPath()
	{
		String tempDir = super.getTempPath();

		if (tempDir == null)
		{
			tempDir = getenv("TEMP");
		}

		if (tempDir == null)
		{
			tempDir = getenv("TMP");
		}

		if (tempDir != null && !tempDir.endsWith(File.separator))
		{
			tempDir += File.separator;
		}

		return tempDir;
	}

	private String getEnvPath(String a_applicationName, String a_envPath)
	{
		if (a_applicationName == null)
		{
			throw new IllegalArgumentException("Application name is null!");
		}

		String path = null;
		File directory;

		path = getenv(a_envPath);

		if (path != null && path.trim().length() > 0 && new File(path).exists())
		{
			// dirAllUsers += "\\Application Data\\" + a_applicationName;
			path += File.separator + a_applicationName;
			directory = new File(path + File.separator);
			if (!directory.exists() && !directory.mkdir())
			{
				LogHolder.log(LogLevel.ERR, LogType.MISC,
						"Could not create storage directory: " + path);
				path = null;
			}
		}
		else
		{
			path = null;
		}

		return path;
	}
}
