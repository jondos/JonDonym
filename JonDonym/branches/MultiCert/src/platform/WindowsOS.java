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

import java.io.File;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.StringTokenizer;

import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;
import java.net.URL;

/**
 * This class is instantiated by AbstractOS if the current OS is Windows
 */
public class WindowsOS extends AbstractOS
{
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
			//java.awt.Desktop.getDesktop().browse(new java.net.URI(a_link));			
			Runtime.getRuntime().exec("rundll32 url.dll,FileProtocolHandler " + a_link);
			//Runtime.getRuntime().exec(
				//	new String[]{"C:\\Program Files\\Mozilla Firefox\\firefox.exe", a_link});
			return true;
		}
		catch (Exception ex)
		{
			LogHolder.log(LogLevel.ERR, LogType.MISC,
						  "Cannot open '" + a_link + "' in Windows default program.", ex);
		}

		return false;
	}

	protected String getAsString(URL a_url)
	{
		String strURL = super.getAsString(a_url);
		if (new StringTokenizer(strURL).countTokens() > 1)
		{
			return "\"" + strURL +"\"";
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
		//String dir = getEnvPath(a_applicationName, "ALLUSERSPROFILE"); 
//		 ALLUSERSPROFILE has become virtual in Vista
		String dir = getEnvPath(a_applicationName, "APPDATA");
		
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
				BufferedReader winPropertiesReader =
					new BufferedReader(
						new InputStreamReader(
							Runtime.getRuntime().exec("CMD /C SET").getInputStream()));
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
			dir = getEnvPath(a_applicationName, "APPDATA");
			if (dir == null)
			{
				dir = System.getProperty("user.home");
			}
		}

		return dir + File.separator;
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
	
		if (path != null && path.trim().length() > 0 &&
			new File(path).exists())
		{
			//dirAllUsers += "\\Application Data\\" + a_applicationName;
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
