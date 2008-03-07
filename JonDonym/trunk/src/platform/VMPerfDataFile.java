/*
 Copyright (c) 2000 - 2008, The JAP-Team
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
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Hashtable;

/**
 * Util class to retrieve various details about running virtual machines.
 *
 * @author Christian Banse
 */
public final class VMPerfDataFile
{
	/**
	 * The performance data entries
	 */
	private Hashtable m_tblEntries;
	
	/**
	 * The buffer used to read from the data file.
	 * Original type: java.nio.ByteBuffer
	 */
	private Object m_buff;
	
	/**
	 * The underlaying perfomance data object
	 * Original type: sun.misc.Perf
	 */
	private Object m_perf;
	
	/**
	 *  This class will only be usable on some java runtimes
	 */
	private boolean m_bUsable = false;
	
	/**
	 * Position of the next data entry in the byte buffer 
	 */
	private int m_nextEntry;
	
	/**
	 * Data entry count
	 */
	private int m_numEntries;
	
	/**
	 * Position of the magic integer
	 */
	private static final int PERFDATA_MAGIC_POSITION = 0;
	
	/**
	 * Position of the byte order
	 */
	private static final int PERFDATA_BYTEORDER_POSITION = 4;
	
	/**
	 * Position of the accessible byte
	 */
	private static final int PERFDATA_ACCESSIBLE_POSITION = 7;
	
	/**
	 * Position of the data entries
	 */
	private static final int PERFDATA_ENTRYOFFSET_POSITION = 24;
	
	/**
	 * Position of the data entry count
	 */
	private static final int PERFDATA_NUMENTRIES_POSITION = 28;
	
	/**
	 * The magic integer
	 */
	private static final int PERFDATA_MAGIC = 0xcafec0c0;
	
	/**
	 * Virtual machine sync timeout
	 */
	private static final int PERFDATA_SYNC_TIMEOUT = 1000;
	
	/**
	 * java.nio.ByteBuffer
	 */
	private static Class m_javaNioByteBufferClass;
	
	/**
	 * java.nio.ByteOrder
	 */
	private Class m_javaNioByteOrderClass;
	
	/**
	 * sun.misc.Perf
	 */
	private Class m_sunMiscPerfClass;
	
	/**
	 * ByteBuffer.position(int)
	 */
	private Method m_byteBufferPositionMethod;
	
	/**
	 * ByteBuffer.get()
	 */
	private Method m_byteBufferGetMethod;
	
	/**
	 * ByteBuffer.getInt()
	 */
	private Method m_byteBufferGetIntMethod;
	
	/**
	 * The id of the virtual machine
	 */
	private int m_vmId;
	
	/**
	 * Creates a new VMPerfDataFile with the specified id.
	 * 
	 * @param a_vmId Id of the Virtual Machine
	 */
	public VMPerfDataFile(int a_vmId)
	{
		m_vmId = a_vmId;
		
		try
		{
			// Set up all needed classes
			m_javaNioByteBufferClass = Class.forName("java.nio.ByteBuffer");
			m_javaNioByteOrderClass = Class.forName("java.nio.ByteOrder");
			m_sunMiscPerfClass = Class.forName("sun.misc.Perf");
			
			m_byteBufferPositionMethod = m_javaNioByteBufferClass.getMethod("position", int.class);
			m_byteBufferGetMethod = m_javaNioByteBufferClass.getMethod("get");
			m_byteBufferGetIntMethod = m_javaNioByteBufferClass.getMethod("getInt");
							
			/*
			 * m_perf = (sun.misc.Perf) AccessController.doPrivileged(new sun.misc.Perf.GetPerfAction());
			 * m_buff = m_perf.attach(a_vmId, "r");
			 */ 
			m_perf = AccessController.doPrivileged((PrivilegedAction) Class.forName("sun.misc.Perf$GetPerfAction").getConstructor().newInstance());
			m_buff = m_sunMiscPerfClass.getMethod("attach", new Class[] { int.class, String.class } ).invoke(m_perf, new Object[] { a_vmId, "r" } );
			
			if(m_buff == null) return;
			
			if(getMagic() != PERFDATA_MAGIC)
				return;
			
			/*
			 * m_buff.order(getByteOrder());
			 */
			m_javaNioByteBufferClass.getMethod("order", m_javaNioByteOrderClass).invoke(m_buff, getByteOrder());
	
			m_bUsable = buildEntries();
		}
		catch(Exception ex) { ex.printStackTrace(); }			
	}
	
	/**
	 * Builds all data entries from the byte buffer
	 * 
	 * @return true if the build was successful, false otherwise
	 * @throws Exception
	 */
	synchronized private boolean buildEntries() throws Exception
	{
		if(m_buff == null) return false;
		
		// Sync with target VM		
		long timeout = System.currentTimeMillis() + PERFDATA_SYNC_TIMEOUT;
		
		while(!isAccessible())
		{
			try 
			{
				Thread.sleep(20);
			}
			catch(InterruptedException ex) { }
			
			if(System.currentTimeMillis() > timeout) return false;
		}
		
		/*
		 * m_buff.position(PERFDATA_ENTRYOFFSET_POSITION);
		 * m_nextEntry = m_buff.getInt();
		 */
		m_byteBufferPositionMethod.invoke(m_buff, PERFDATA_ENTRYOFFSET_POSITION);
		m_nextEntry = (Integer) m_byteBufferGetIntMethod.invoke(m_buff);

		/* 
		 * m_buff.position(PERFDATA_NUMENTRIES_POSITION);
		 * m_numEntries = m_buff.getInt();
		 */			
		m_byteBufferPositionMethod.invoke(m_buff, PERFDATA_NUMENTRIES_POSITION);
		m_numEntries = (Integer) m_byteBufferGetIntMethod.invoke(m_buff);
		
		m_tblEntries = new Hashtable();
		
		while(buildNextEntry()) { }
	
		return true;
	}
	
	/**
	 * Builds the next data entry
	 * 
	 * @return true if the build was succesful, false otherwise
	 * @throws Exception
	 */
	synchronized private boolean buildNextEntry() throws Exception
	{
		if(m_buff == null) return false;
		
		// nextEntry MOD 4 must be 0
		if(m_nextEntry % 4 != 0) return false;
		
		if(m_nextEntry < 0 || m_nextEntry >= (Integer) m_javaNioByteBufferClass.getMethod("limit").invoke(m_buff)) return false;
		
		/* 
		 * m_buff.position(m_nextEntry);
		 * int entryLength = m_buff.getInt();
		 */
		m_byteBufferPositionMethod.invoke(m_buff, m_nextEntry);
		int entryLength = (Integer) m_byteBufferGetIntMethod.invoke(m_buff);
		
		if(m_nextEntry + entryLength > (Integer) m_javaNioByteBufferClass.getMethod("limit").invoke(m_buff) || entryLength == 0) return false;
		
		/*
		 * int offsetName = m_buff.getInt();
		 * int vectorLen = m_buff.getInt();
		 * byte typeCode = m_buff.get();
		 */
		int offsetName = (Integer) m_byteBufferGetIntMethod.invoke(m_buff);
		int vectorLen = (Integer) m_byteBufferGetIntMethod.invoke(m_buff);
		byte typeCode = (Byte) m_byteBufferGetMethod.invoke(m_buff);
		
		/*
		 * m_buff.get();
		 * byte units = m_buff.get();
		 * m_buff.get();
		 * int offsetData = m_buff.getInt();
		 */
		// Flags - not used
		m_byteBufferGetMethod.invoke(m_buff);
		byte units = (Byte) m_byteBufferGetMethod.invoke(m_buff);
		// Variability - not used
		m_byteBufferGetMethod.invoke(m_buff);
		int offsetData = (Integer) m_byteBufferGetIntMethod.invoke(m_buff);
					
		// include possible padding
		int maxNameLength = offsetData - offsetName;
		
		byte[] bytes = new byte[maxNameLength];
		byte b;
		int nameLength = 0;
		while((b = (Byte) m_byteBufferGetMethod.invoke(m_buff)) != 0 && maxNameLength > nameLength)
			bytes[nameLength++] = b;
		
		String name = new String(bytes, 0, nameLength);
		
		/*
		 * m_buff.position(m_nextEntry + offsetData);
		 */
		m_byteBufferPositionMethod.invoke(m_buff, m_nextEntry + offsetData);
		
		
		// we're only parsing non-scalar objects for now
		if(vectorLen > 0)
		{
			// only parse string objects (typeCode = byte and units = STRING)
			if(typeCode == 'B' && units == 5)
			{
				bytes = new byte[vectorLen];
				int dataLen = 0;
				while((b = (Byte) m_byteBufferGetMethod.invoke(m_buff)) != 0 && vectorLen > dataLen)
					bytes[dataLen++] = b;
				
				String value = new String(bytes, 0, dataLen);
				
				m_tblEntries.put(name, value);
			}
		}
		
		m_nextEntry += entryLength;
		
		return true;
	}

	/**
	 * Checks if the VM perf data file is accessible
	 *  
	 * @return true if the VM is accessible, false otherwise
	 * @throws Exception
	 */
	private boolean isAccessible() throws Exception
	{
		if(m_buff == null) return false;
		
		/*
		 * m_buff.position(PERFDATA_ACCESSIBLE_POSITION);
		 * byte r_value = m_buff.get();
		 */
		m_byteBufferPositionMethod.invoke(m_buff, PERFDATA_ACCESSIBLE_POSITION);
		byte r_value = (Byte) m_byteBufferGetMethod.invoke(m_buff);
			
		return r_value != 0;
	}
	
	/**
	 * Gets the magic integer of the VM datafile
	 * 
	 * @return the magic integer
	 * @throws Exception
	 */
	private int getMagic() throws Exception
	{
		if(m_buff == null) return 0;
		
		/* 
		 * ByteOrder order = m_buff.order();
		 * m_buff.order(ByteOrder.BIG_ENDIAN);
		*/
		
		Object order = m_javaNioByteBufferClass.getMethod("order").invoke(m_buff);
		m_javaNioByteBufferClass.getMethod("order", m_javaNioByteOrderClass).invoke(m_buff, m_javaNioByteOrderClass.getField("BIG_ENDIAN").get(null));
		
		/* 
		 * m_buff.position(PERFDATA_MAGIC_POSITION);
		 * int r_magic = m_buff.getInt();
		 * m_buff.order(order);
		 */
		
		m_byteBufferPositionMethod.invoke(m_buff, PERFDATA_MAGIC_POSITION);
		int r_magic = (Integer) m_byteBufferGetIntMethod.invoke(m_buff);
		m_javaNioByteBufferClass.getMethod("order", m_javaNioByteOrderClass).invoke(m_buff, order);
		
		return r_magic;
	}
	
	/**
	 * Returns the byte order of the underlaying perfomance file
	 * Original return type: java.nio.ByteOrder
	 * 
	 * @return the byte order
	 */
	private Object getByteOrder() throws Exception
	{
		if(m_buff == null) return null;
		
		/* Pre-Reflection:
		 * m_buff.position(PERFDATA_BYTEORDER_POSITION);
		 * byte order = m_buff.get(); 
		 */
		
		m_byteBufferPositionMethod.invoke(m_buff, PERFDATA_BYTEORDER_POSITION);
		byte order = (Byte) m_byteBufferGetMethod.invoke(m_buff);
		
		if(order == 0)
			return m_javaNioByteOrderClass.getField("BIG_ENDIAN").get(null);
		else
			return m_javaNioByteOrderClass.getField("LITTLE_ENDIAN").get(null);
	}
	
	/**
	 * Returns the main class of the Virtual Machine
	 * 
	 * @return the main class as String
	 */
	public String getMainClass()
	{
		if(!m_bUsable) return null;
		
		String value = (String) m_tblEntries.get("sun.rt.javaCommand");
		if(value != null)
		{
			int i = value.indexOf(' ');
			if(i > 0)
				return value.substring(0, i);
			else return value;					
		}
		
		return null;
	}
	
	/**
	 * Returns the id of the Virtual Machine
	 * 
	 * @return the id
	 */
	public int getId()
	{
		return m_vmId;
	}
	
	/**
	 * toString() helper class
	 * 
	 * @return the main class
	 */
	public String toString()
	{
		return getMainClass();
	}
	
	/**
	 * Checks if the Class is usable with the current VM
	 * 
	 * @return true if the Class is usable, false otherwise
	 */
	public boolean isUsable() 
	{
		return m_bUsable;
	}
}