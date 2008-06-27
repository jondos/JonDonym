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
package anon.util;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.Vector;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.Inflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;

/**
 * This class provides some utility methods for ZLib compression.
 */
final public class ZLibTools
{

	/**
	 * Compresses the specified data with ZLib in the best compression mode.
	 *
	 * @param a_data The data to compress.
	 *
	 * @return The compressed data or null, if there was an error while the compression.
	 */
	public static byte[] compress(byte[] a_data)
	{
		byte[] resultData = null;
		try
		{
			Deflater zipper = new Deflater(Deflater.BEST_COMPRESSION);
			ByteArrayOutputStream zippedData = new ByteArrayOutputStream();
			DeflaterOutputStream zipStream = new DeflaterOutputStream(zippedData, zipper);
			zipStream.write(a_data, 0, a_data.length);
			zipStream.finish();
			resultData = zippedData.toByteArray();
		}
		catch (Throwable e)
		{
			/* should not happen */
		}
		return resultData;
	}

	/**
	 * Decompresses the specified data.
	 *
	 * @param a_data The ZLib compressed data (whole block, not only parts).
	 *
	 * @return The uncompressed data or null, if the specified data are not ZLib compressed.
	 */
	public static byte[] decompress(byte[] a_data)
	{
		byte[] resultData = null;
		try
		{
			ByteArrayOutputStream unzippedData = new ByteArrayOutputStream();
			Inflater unzipper = new Inflater();
			unzipper.setInput(a_data);
			byte[] currentByte = new byte[10000];
			int len;
			while ((len=unzipper.inflate(currentByte)) >0)
			{
				unzippedData.write(currentByte,0,len);
			}
			unzippedData.flush();
			resultData = unzippedData.toByteArray();
		}
		catch (Throwable e)
		{
			LogHolder.log(LogLevel.INFO, LogType.MISC,
						  "ZLIb decompress() decommpressed failed!");
			/* something was wrong with the compressed data */
		}
		return resultData;
	}
	
	public static boolean extractArchive(ZipFile archive, String pathName, String destination)
	{
		
		String dest = destination;
		Enumeration allZipEntries = null;
		Vector matchedFileEntries = new Vector();
		Vector matchedDirEntries = new Vector();
		ZipEntry entry = null;
		String entryName = null;
		int index = 0;
		int dirIndex = 0;
		int fileIndex = 0;
		//TreeSet matchedEntries
		
		if(archive == null)
		{
			LogHolder.log(LogLevel.ERR, LogType.MISC, "Archive is null");		
			return false;
		}
		if(destination == null)
		{
				LogHolder.log(LogLevel.ERR, LogType.MISC, "Error while extracting archive "+
						archive.getName()+": destination address is null");
				return false;
		}
		try
		{	
			allZipEntries = archive.entries();
			while(allZipEntries.hasMoreElements() )
			{
				entry = (ZipEntry) allZipEntries.nextElement();
				entryName = entry.getName();
				if( (pathName == null) || (entryName.startsWith(pathName)) )
				{
					if( entry.isDirectory() )
					{
						for(index=0; index < matchedDirEntries.size(); index++)
						{
							if( ((String)matchedDirEntries.elementAt(index)).compareTo(entryName) > 0)
							{
								break;		
							}
						}
						matchedDirEntries.insertElementAt(entryName, index);
					}
					else
					{
						matchedFileEntries.addElement(entry);
					}
				}
			}
			if( (matchedFileEntries.size() == 0) && (matchedDirEntries.size() == 0) )
			{
				LogHolder.log(LogLevel.ERR, LogType.MISC, "No matching files for "+pathName+"found in archive "+archive.getName());
				return false;
			}
			
			for (Enumeration iterator = matchedDirEntries.elements(); iterator.hasMoreElements(); dirIndex++) {
				String dirName = (String) iterator.nextElement();
				File dir = new File(dest+File.separator+dirName);
				if(dir != null)
				{
					if(!dir.mkdir() )
					{
						LogHolder.log(LogLevel.ERR, LogType.MISC, "Error while extracting archive "+
								archive.getName()+": could not create directory "+dir.getName());
						rollback(matchedDirEntries, dirIndex, dest);
						return false;
					}
				}
			}
			
			for (Enumeration iterator = matchedFileEntries.elements(); iterator.hasMoreElements(); fileIndex++) 
			{
				ZipEntry zEntry = (ZipEntry) iterator.nextElement();
				File destFile = new File(dest+File.separator+zEntry.getName());
				InputStream zEntryInputStream = archive.getInputStream(zEntry);
				RecursiveCopyTool.copySingleFile(zEntryInputStream, destFile);		
			}
		}
		catch(IllegalStateException ise)
		{
			LogHolder.log(LogLevel.ERR, LogType.MISC, "Cannot extract archive "+archive.getName()+": file already closed");
			return false;
		} 
		catch (IOException ioe) 
		{
			LogHolder.log(LogLevel.ERR, LogType.MISC, "Cannot extract archive "+archive.getName()+": I/O error occured: ", ioe);
			
			rollback(matchedFileEntries, fileIndex, destination);
			rollback(matchedDirEntries, matchedDirEntries.size(), destination);
			return false;
		}
		return true;
	}

	private static void rollback(Vector entries, int dirIndex, String destination) {
		
		for(int i = dirIndex; i > 0; i--)
		{
			File f = new File(destination+File.separator+
								entries.elementAt(i-1));
			String was = f.delete() ? " " : " not ";
			LogHolder.log(LogLevel.DEBUG, LogType.MISC, "Rollback: file "+f.getName()+was+"successfully deleted");
		}
	}

}
