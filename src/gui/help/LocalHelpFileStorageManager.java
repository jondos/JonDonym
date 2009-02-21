/*
	Copyright (c) The JAP-Team, JonDos GmbH
	All rights reserved.
	Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:
	Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
	Redistributions in binary form must reproduce the above copyright notice,
	 this list of conditions and the following disclaimer in the documentation and/or
	 other materials provided with the distribution.
	Neither the name of the University of Technology Dresden, Germany, nor the name of
	 the JonDos GmbH, nor the names of their contributors may be used to endorse or 
	 promote products derived from this software without specific prior written permission.
	THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
	"AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
	LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
	A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE REGENTS OR
	CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
	EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
	PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR 
	PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
	LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
	NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
	SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/
package gui.help;

import java.io.File;
import java.util.Observable;

import anon.util.ClassUtil;


public class LocalHelpFileStorageManager extends AbstractHelpFileStorageManager
{
	private final Observable DUMMY = new Observable();
	private final String LOCAL_HELP_PATH;

	public LocalHelpFileStorageManager(String a_applicationName)
	{
		if (a_applicationName == null)
		{
			throw new IllegalArgumentException("Application name is null!");
		}
		
		File classDir = ClassUtil.getClassDirectory(this.getClass());
		
		if (classDir != null)
		{
			LOCAL_HELP_PATH = classDir.getParent();
		}
		
		else
		{
			// this may happen in a web (start) environment without sufficient file rights
			LOCAL_HELP_PATH = null;
		}
	}
	
	public boolean ensureMostRecentVersion(String helpPath)
	{
		// do nothing
		return true;
	}

	public Observable getStorageObservable()
	{
		return DUMMY;
	}

	public boolean handleHelpPathChanged(String oldHelpPath, 
			String newHelpPath, boolean a_bIgnoreExistingHelpDir)
	{
		if (newHelpPath != null && newHelpPath.equals(LOCAL_HELP_PATH) &&
				(oldHelpPath == null || !oldHelpPath.equals(newHelpPath)))
		{
			return true;
		}
	
		return false;
	}

	public boolean helpInstallationExists(String helpPath)
	{
		if (helpPath == null || !helpPath.equals(LOCAL_HELP_PATH))
		{
			return false;
		}
		return true;
	}

	public String helpPathValidityCheck(String absolutePath, boolean a_bIgnoreExistingHelpDir)
	{	
		if (absolutePath == null || 
				!absolutePath.equals(LOCAL_HELP_PATH))
		{
			return AbstractHelpFileStorageManager.HELP_INVALID_NOWRITE;
		}
		return AbstractHelpFileStorageManager.HELP_JONDO_EXISTS;
	}
	
	public String getInitPath()
	{
		return LOCAL_HELP_PATH;
	}
}
