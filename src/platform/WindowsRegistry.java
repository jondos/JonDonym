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

import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;

public class WindowsRegistry 
{
	private static final int NATIVE_HANDLE = 0;
	private static final int ERROR_CODE = 1;
	
	private static Class ms_windowsPreferencesClass;
	private static Method ms_openKeyMethod;
	private static Method ms_queryValueMethod;
	private static Method ms_closeKeyMethod;
	
	static
	{
		try
		{
			Class accessibleObjectClass = Class.forName("java.lang.reflect.AccessibleObject");
			Method setAccessibleMethod = accessibleObjectClass.getMethod("setAccessible", new Class[]{ Boolean.TYPE });
			
			ms_windowsPreferencesClass = Class.forName("java.util.prefs.WindowsPreferences");
			ms_openKeyMethod = ms_windowsPreferencesClass.getDeclaredMethod("WindowsRegOpenKey", 
					new Class[] { int.class, byte[].class, Integer.TYPE });
			
			ms_queryValueMethod = ms_windowsPreferencesClass.getDeclaredMethod("WindowsRegQueryValueEx",
					new Class[] { Integer.TYPE, byte[].class });
			
			ms_closeKeyMethod = ms_windowsPreferencesClass.getDeclaredMethod("WindowsRegCloseKey",
					new Class[] { int.class});
			
			setAccessibleMethod.invoke(ms_openKeyMethod, new Object[] { Boolean.TRUE });
			setAccessibleMethod.invoke(ms_queryValueMethod, new Object[] { Boolean.TRUE });
			setAccessibleMethod.invoke(ms_closeKeyMethod, new Object[] { Boolean.TRUE });
		}
		catch(ClassNotFoundException ex)
		{
			ex.printStackTrace();
		}
		catch(NoSuchMethodException ex)
		{
			ex.printStackTrace();
		}
		catch(InvocationTargetException ex)
		{
			ex.printStackTrace();
		}
		catch(IllegalAccessException ex)
		{
			ex.printStackTrace();
		}
	}
	
	public static int openKey(int a_hKey, String a_subKey, int a_securityMask)
	{
		try
		{
			int[] result = (int[]) ms_openKeyMethod.invoke(null, new Object[] {new Integer(a_hKey), (a_subKey + "\0").getBytes(), new Integer(a_securityMask)});
			if(result != null && result[ERROR_CODE] == WindowsOS.ERROR_SUCCESS)
			{
				return result[NATIVE_HANDLE];
			}
		}
		catch(InvocationTargetException ex)
		{
			ex.printStackTrace();
		}
		catch(IllegalAccessException ex)
		{
			ex.printStackTrace();
		}
		
		return -1;
	}
	
	public static String queryValue(int a_hKey, String a_subKey)
	{
		try
		{
			byte[] b = (byte[]) ms_queryValueMethod.invoke(null, new Object[] {new Integer(a_hKey), (a_subKey + "\0").getBytes()});
			if(b != null)
			{
				String s = new String(b);
				if(s.charAt(s.length() - 1) == '\0')
				{
					s = s.substring(0, s.length() - 1);
				}
				return s;
			}
		}
		catch(InvocationTargetException ex)
		{
			ex.printStackTrace();
		}
		catch(IllegalAccessException ex)
		{
			ex.printStackTrace();
		}
		
		return null;
	}
	
	public static int closeKey(int a_hKey)
	{
		try
		{
			Integer i = (Integer) ms_closeKeyMethod.invoke(null, new Object[] {new Integer(a_hKey)});
			if(i != null)
			{
				return i.intValue();
			}
		}
		catch(InvocationTargetException ex)
		{
			ex.printStackTrace();
		}
		catch(IllegalAccessException ex)
		{
			ex.printStackTrace();
		}
		
		return -1;
	}
}
