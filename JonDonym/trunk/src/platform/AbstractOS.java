/*
 Copyright (c) 2000 - 2006, The JAP-Team
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
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.net.URL;
import java.security.AccessController;
import java.util.Iterator;
import java.util.Set;
import java.util.Vector;
import java.util.Hashtable;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;
import gui.JAPHelp.IExternalURLCaller;
import gui.JAPHelp.IExternalEMailCaller;
import gui.dialog.JAPDialog;


/**
 * This abstract class provides access to OS-specific implementations of certain
 * functions. It tries to instantiate an OS-specific class by determining on which
 * operating system JAP is currently running.
 */
public abstract class AbstractOS implements IExternalURLCaller, IExternalEMailCaller
{
	public static final String URL_MAIL_TO = "mailto:";

	/**
	 * Make sure that the default OS is the last OS in the array.
	 */
	private static Class[] REGISTERED_PLATFORM_CLASSES =
		{
		LinuxOS.class, WindowsOS.class, MacOS.class, UnknownOS.class};

	private static final String[] BROWSERLIST =
		{
		"firefox", "iexplore", "explorer", "mozilla", "konqueror", "mozilla-firefox", "opera"
	};

	/**
	 * The instanciated operation system class.
	 * (no, ms_operating system does not mean only Microsoft OS are supported... ;-))
	 */
	private static AbstractOS ms_operatingSystem;

	private IURLErrorNotifier m_notifier;
	private IURLOpener m_URLOpener;
	
	private static File ms_tmpDir;
	
	static
	{
		// Needs to be done according to the JDK because java.io.tmpdir 
		// seems to return wrong values on some Linux and Solaris systems.
		String tmpDir = System.getProperty("java.io.tmpdir");
		if(tmpDir.compareTo("/var/tmp/") == 0)
			tmpDir = "/tmp/";
		
		// Assure that the tmpDir has a trailing File.seperator
		if(tmpDir.lastIndexOf(File.pathSeparator) != (tmpDir.length() - 1))
			tmpDir = tmpDir + File.separator;
		
		ms_tmpDir = new File(tmpDir);
	}
	
	public final class VMPerfDataFile
	{
		private Hashtable m_tblEntries;
		private ByteBuffer m_buff;
		
		// This class will only be usable on some java runtimes
		private boolean m_bUsable = false;
		private sun.misc.Perf m_perf;
		
		private int m_nextEntry;
		private int m_numEntries;
		
		// Header
		private static final int PERFDATA_MAGIC_POSITION = 0;
		private static final int PERFDATA_BYTEORDER_POSITION = 4;
		private static final int PERFDATA_ACCESSIBLE_POSITION = 7;
		private static final int PERFDATA_ENTRYOFFSET_POSITION = 24;
		private static final int PERFDATA_NUMENTRIES_POSITION = 28;
		
		// TODO: to change
		public int m_vmId;
		
		public VMPerfDataFile(int a_vmId)
		{
			// SUN SUN SUN SUN SUN SUN 
			// TODO: use reflection API
			
			m_vmId = a_vmId;
			
			try
			{
				m_perf = (sun.misc.Perf) AccessController.doPrivileged(new sun.misc.Perf.GetPerfAction());
				m_buff = m_perf.attach(a_vmId, "r");
			}
			catch(Exception ex) { }
			
			if(getMagic() != 0xcafec0c0)
			{
				m_bUsable = false;
				return;
			}
			
			m_buff.order(getByteOrder());
			m_bUsable = buildEntries();
		}
		
		synchronized private boolean buildEntries()
		{
			// Sync with target VM, timeout is 1 second			
			long timeout = System.currentTimeMillis() + 5000;
			
			while(!isAccessible())
			{
				try 
				{
					Thread.sleep(20);
				}
				catch(InterruptedException ex) { }
				
				if(System.currentTimeMillis() > timeout) return false;
			}
			
			m_buff.position(PERFDATA_ENTRYOFFSET_POSITION);
			m_nextEntry = m_buff.getInt();
			
			m_buff.position(PERFDATA_NUMENTRIES_POSITION);
			m_numEntries = m_buff.getInt();
			
			m_tblEntries = new Hashtable();
			
			while(buildNextEntry()) { }
		
			return true;
		}
		
		synchronized private boolean buildNextEntry()
		{
			// nextEntry MOD 4 must be 0
			if(m_nextEntry % 4 != 0) return false;
			
			if(m_nextEntry < 0 || m_nextEntry >= m_buff.limit()) return false;
			
			m_buff.position(m_nextEntry);
			int entryLength = m_buff.getInt();
			
			if(m_nextEntry + entryLength > m_buff.limit() || entryLength == 0) return false;
			
			int offsetName = m_buff.getInt();
			int vectorLen = m_buff.getInt();
			byte typeCode = m_buff.get();
			byte flags = m_buff.get();
			byte units = m_buff.get();
			byte var = m_buff.get();
			int offsetData = m_buff.getInt();
			
			// include possible padding
			int maxNameLength = offsetData - offsetName;
			
			byte[] bytes = new byte[maxNameLength];
			byte b;
			int nameLength = 0;
			while((b = m_buff.get()) != 0 && maxNameLength > nameLength)
				bytes[nameLength++] = b;
			
			String name = new String(bytes, 0, nameLength);
			
			int dataSize = entryLength - offsetData;
			m_buff.position(m_nextEntry + offsetData);
			
			if(vectorLen == 0)
			{
				// don't parse scalar objects (yet)
			}
			else
			{
				// only parse string objects
				if(typeCode == 'B' && units == 5)
				{
					bytes = new byte[vectorLen];
					int dataLen = 0;
					while((b = m_buff.get()) != 0 && vectorLen > dataLen)
						bytes[dataLen++] = b;
					
					String value = new String(bytes, 0, dataLen);
					
					m_tblEntries.put(name, value);
				}
				
			}
			
			m_nextEntry += entryLength;
			
			return true;
		}
		
		private boolean isAccessible()
		{
			m_buff.position(PERFDATA_ACCESSIBLE_POSITION);
			byte value = m_buff.get();
			return value != 0;
		}
		
		private int getMagic()
		{
			ByteOrder order = m_buff.order();
			m_buff.order(ByteOrder.BIG_ENDIAN);
			
			m_buff.position(PERFDATA_MAGIC_POSITION);
			int r_magic = m_buff.getInt();
			
			m_buff.order(order);
			
			return r_magic;
		}
		
		private ByteOrder getByteOrder()
		{
			m_buff.position(PERFDATA_BYTEORDER_POSITION);
			
			byte order = m_buff.get();
			
			if(order == 0)
				return ByteOrder.BIG_ENDIAN;
			else
				return ByteOrder.LITTLE_ENDIAN;
		}
		
		public String getMainClass()
		{
			String value = (String) m_tblEntries.get("sun.rt.javaCommand");
			if(value != null)
			{
				int i = value.indexOf(' ');
				if(i > 0)
					return value.substring(0, i);
			}
			
			return null;
		}
		
		public String toString()
		{
			return getMainClass();
		}
		
		public boolean isUsable() 
		{
			return m_bUsable;
		}
		
		
	}

	public static interface IURLOpener
	{
		boolean openURL(URL a_url);
	}

	public static interface IURLErrorNotifier
	{
		void checkNotify(URL a_url);
	}

	/**
	 * Instantiates an OS-specific class. If no specific class is found, the default OS
	 * (which is a dummy implementation) is instanciated.
	 * @return the instanciated operating system class
	 */
	public static final AbstractOS getInstance()
	{
		for (int i = 0; ms_operatingSystem == null && i < REGISTERED_PLATFORM_CLASSES.length; i++)
		{
			try
			{
				ms_operatingSystem =
					(AbstractOS) REGISTERED_PLATFORM_CLASSES[i].newInstance();
			}
			catch (Exception a_e)
			{
				LogHolder.log(LogLevel.DEBUG, LogType.MISC,
							  "Cannot instantiate class " + REGISTERED_PLATFORM_CLASSES[i] +
							  ". Trying to instanciate another platform class.");
			}
			if (ms_operatingSystem != null)
			{
				ms_operatingSystem.m_notifier = new IURLErrorNotifier()
				{
					public void checkNotify(URL a_url)
					{
						// do nothing
					}
				};
			}
		}

		return ms_operatingSystem;
	}

	public void init(IURLErrorNotifier a_notifier, IURLOpener a_URLOpener)
	{
		if (a_notifier != null)
		{
			m_notifier = a_notifier;
		}
		if (a_URLOpener != null)
		{
			m_URLOpener = a_URLOpener;
		}
	}

	public JAPDialog.ILinkedInformation createURLLink(final URL a_url, final String a_optionalText)
	{
		return createURLLink(a_url, a_optionalText, null);
	}

	public JAPDialog.ILinkedInformation createURLLink(final URL a_url, final String a_optionalText,
		String a_helpContext)
	{
		if (a_url == null)
		{
			return null;
		}

		JAPDialog.ILinkedInformation link = new JAPDialog.LinkedHelpContext(a_helpContext)
		{
			public int getType()
			{
				return JAPDialog.ILinkedInformation.TYPE_LINK;
			}
			public void clicked(boolean a_bState)
			{
				openURL(a_url);
			}
			public String getMessage()
			{
				if (a_optionalText == null || a_optionalText.trim().length() == 0)
				{
					return a_url.toString();
				}
				else
				{
					return a_optionalText;
				}
			}
		};

		return link;
	}

	public final boolean openEMail(String a_mailto)
	{
		if (a_mailto == null)
		{
			return false;
		}
		if (!a_mailto.startsWith(URL_MAIL_TO))
		{
			return openLink(URL_MAIL_TO + a_mailto);
		}
		else
		{
			return openLink(a_mailto);
		}
	}

	public final boolean openURL(URL a_url)
	{
		boolean success = false;
		if (a_url == null)
		{
			return false;
		}

		String[] browser = BROWSERLIST;
		String url = getAsString(a_url);

		m_notifier.checkNotify(a_url);

		if (m_URLOpener != null)
		{
			success = m_URLOpener.openURL(a_url);
		}
		if (!success)
		{
			success = openLink(url);
		}
		if (!success)
		{
			for (int i = 0; i < browser.length; i++)
			{
				try
				{
					Runtime.getRuntime().exec(new String[]{browser[i], url});
					success = true;
					break;
				}
				catch (Exception ex)
				{
				}
			}
		}

		if (!success)
		{
			LogHolder.log(LogLevel.ERR, LogType.MISC, "Cannot open URL in browser");
		}
		return success;
	}

	/**
	 * Implementations must return a valid path to the config file.
	 */
	public abstract String getConfigPath();

	protected abstract boolean openLink(String a_link);

	protected String getAsString(URL a_url)
	{
		if (a_url == null)
		{
			return null;
		}
		return a_url.toString();
	}
	
	/**
	 * Returns a vector of all running VMs. This only works on the Sun VM
	 * 
	 * @return a vector of all running Virtual Mashine
	 */
	public Vector getActiveVMs()
	{
		// TODO: check for Sun VM
		
		Vector r_vms = new Vector();
		int id = 0;
		
		if(!ms_tmpDir.isDirectory())
			return r_vms;
		
		// Each user on has a distinct directory named hsperfdata_(user) which contains all VM performance data
		final Matcher matcher = Pattern.compile("hsperfdata_\\S*").matcher("");
		FilenameFilter filter = new FilenameFilter() 
		{
			public boolean accept(File a_dir, String a_name)
			{
				matcher.reset(a_name);
				return matcher.lookingAt();
			}
		};
		
		// Loop through all directories that match the filter
		File[] dirs = ms_tmpDir.listFiles(filter);
		for(int i = 0; i < dirs.length; i++)
		{
			if(!dirs[i].isDirectory())
				continue;
			
			// Loop through all files in the directory. Each file represents one VM
			File[] files = dirs[i].listFiles();
			
			if(files != null)
			{
				for(int j = 0; j < files.length; j++)
				{
					if(files[j].isFile() && files[j].canRead()) 
					{
						try 
						{
							if((id = Integer.parseInt(files[j].getName())) != 0) {
								//r_ids.add(id);
								r_vms.add(new VMPerfDataFile(id));
								//getClassName(id);
								
							}
						} 
						catch(NumberFormatException e) { }
					}
				}
			}
		}
		
		
		return r_vms;		
	}
}
