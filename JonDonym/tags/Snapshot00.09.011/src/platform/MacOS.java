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

/**
 * This class is instantiated by AbstractOS if the current OS is MacOS or MacOSX
 */
public class MacOS extends AbstractOS
{
	public static final String OS_NAME = "Mac OS";


	public MacOS() throws Exception
	{
		if (System.getProperty("mrj.version") == null)
		{
			throw new Exception("Operating system is not "+ OS_NAME);
		}
	}

	/*
	public boolean openURL(URL a_url)
	{
		//MRJFileUtils.openURL(a_url.toString());
		return openLink(a_url.toString());
	}*/

	protected boolean openLink(String a_link)
	{
		try
		{
			Runtime.getRuntime().exec("open " + a_link);
			return true;
		}
		catch (Exception ex)
		{
			LogHolder.log(LogLevel.ERR, LogType.MISC,
						  "Cannot open '" + a_link + "' in MacOS default program.");
		}

		return false;
	}



	public String getConfigPath()
	{
		//Return path in users's home/Library/Preferences
		if (System.getProperty("os.name").equalsIgnoreCase(OS_NAME))
		{
			return System.getProperty("user.home", ".") +"/";
		}
		else
		{
			return System.getProperty("user.home", "") + "/Library/Preferences/";
		}
	}
}
