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

package update;

import java.io.File;

import javax.swing.filechooser.FileFilter;

public class JarFileFilter extends FileFilter
{

	private final String jarExtension = "jar";

	public JarFileFilter()
	{
	}

	public boolean accept(File parm1)
	{
		if (parm1.isDirectory())
		{
			return true;
		}

		String extension = getExtension(parm1);

		if (extension != null)
		{

			if (extension.equals(jarExtension))
			{
				return true;
			}
			else
			{
				return false;
			}
		}
		return false;
	}

	public String getDescription()
	{
		String description = "Jar File " + "(*." + jarExtension + ")";
		return description;
	}

	private String getExtension(File f)
	{
		String extension = null;
		String s;
		try
		{
			s = f.getName();
		}
		catch (Exception e)
		{
			e.printStackTrace();
			return null;
		}
		int i = s.lastIndexOf('.');

		if (i > 0 && i < s.length() - 1)
		{
			extension = s.substring(i + 1).toLowerCase();
		}
		return extension;
	}
}
