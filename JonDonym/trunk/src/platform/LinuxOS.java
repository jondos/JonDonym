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

import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;
import java.util.Properties;

/**
 * This class is instantiated by AbstractOS if the current OS is Linux
 */
public class LinuxOS extends AbstractOS
{
	private boolean m_bKDE = false;
	private boolean m_bGnome = false;


	public static final String[] BROWSERLIST =
		{
		"firefox", "iexplore", "explorer", "mozilla", "konqueror", "mozilla-firefox", "opera"
	};

	public LinuxOS() throws Exception
	{
		String osName = System.getProperty("os.name", "").toLowerCase();
		if (osName.toLowerCase().indexOf("linux") == -1)
		{
			throw new Exception("Operating system is not Linux");
		}

		Properties properties = new Properties();
		try
		{
			properties.load(Runtime.getRuntime().exec("env").getInputStream());
			m_bKDE = Boolean.valueOf(properties.getProperty("KDE_FULL_SESSION")).booleanValue();
		}
		catch (Exception a_e)
		{
		}
		m_bGnome = properties.getProperty("GNOME_DESKTOP_SESSION_ID") != null;

	}

	protected boolean openLink(String a_link)
	{
		if (a_link == null)
		{
			return false;
		}

		if (m_bKDE)
		{
			try
			{
				Runtime.getRuntime().exec("kfmclient exec " + a_link);
				return true;
			}
			catch (Exception ex)
			{
				LogHolder.log(LogLevel.ERR, LogType.MISC,
							  "Cannot open '" + a_link + "' in KDE default program.");
			}
		}
		else if (m_bGnome)
		{
			try
			{
				Runtime.getRuntime().exec("gnome-open " + a_link);
				return true;
			}
			catch (Exception ex)
			{
				LogHolder.log(LogLevel.ERR, LogType.MISC,
							  "Cannot open '" + a_link + "' in Gnome default program.");
			}
		}
		return false;
	}

	public String getConfigPath()
	{
		//Return path in user's home directory with hidden file (preceded by ".")
		return System.getProperty("user.home", "") + "/.";
	}
}
