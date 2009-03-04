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
 
package anon.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.util.Enumeration;
import java.util.Observable;
import java.util.Vector;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;

public class ZipArchiver extends Observable
{
	private ZipFile m_archive;
	
	public ZipArchiver(ZipFile archive)
	{
		m_archive = archive;
	}
	
	public boolean extractSingleEntry(String entryName, String destinationName)
	{
		try
		{
			ZipEntry entry = m_archive.getEntry(entryName);
			if(entry == null)
			{
				LogHolder.log(LogLevel.ERR, LogType.MISC, "Entry "+entryName+" not found.");
				return false;
			}
			RecursiveFileTool.copySingleFile(m_archive.getInputStream(entry), new File(destinationName));
			return true;
		}
		catch(IOException ioe)
		{
			LogHolder.log(LogLevel.ERR, LogType.MISC, "Extracting entry "+entryName+" failed", ioe);
			return false;
		}
	}
	
	public boolean extractArchive(String a_archivePathName, String destination)
	{	
		String dest = destination;
		Enumeration allZipEntries = null;
		Vector matchedFileEntries = new Vector();
		Vector matchedDirEntries = new Vector();
		
		Vector extractedFiles = new Vector();
		
		ZipEntry entry = null;
		String entryName = null;
		int index = 0;
		int dirIndex = 0;
		int fileIndex = 0;
		
		long totalSize = 0;
		long sizeOfCopied = 0;
		
		if(m_archive == null)
		{
			LogHolder.log(LogLevel.ERR, LogType.MISC, "Archive is null");		
			return false;
		}
		if(destination == null)
		{
				LogHolder.log(LogLevel.ERR, LogType.MISC, "Error while extracting archive "+
						m_archive.getName()+": destination address is null");
				return false;
		}
		try
		{	
			allZipEntries = m_archive.entries();
			while(allZipEntries.hasMoreElements() && !Thread.currentThread().isInterrupted())
			{
				entry = (ZipEntry) allZipEntries.nextElement();
				
				entryName = entry.getName();

				if( (a_archivePathName == null) || (entryName.startsWith(a_archivePathName)) )
				{
					totalSize += entry.getSize();
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
				LogHolder.log(LogLevel.ERR, LogType.MISC, "No matching files for "+a_archivePathName+" found in archive "+m_archive.getName());
				notifyAboutChanges(0, 0, ProgressCapsule.PROGRESS_FAILED);
				return false;
			}
			
			for (Enumeration iterator = matchedDirEntries.elements(); 
				(iterator.hasMoreElements() && !Thread.currentThread().isInterrupted()); 
				dirIndex++) 
			{
				String dirName = (String) iterator.nextElement();
				
				File dir = new File(dest+File.separator+dirName);
				if(dir != null)
				{
					if (!dir.exists() && !dir.mkdir())
					{
						LogHolder.log(LogLevel.ERR, LogType.MISC, "Error while extracting archive "+
								m_archive.getName()+": could not create directory "+dir.getAbsolutePath());
						extractErrorRollback(matchedDirEntries, destination);
						return false;
					}
					else
					{
						extractedFiles.addElement(dirName);
					}
				}
			}
			notifyAboutChangesInterruptable(sizeOfCopied, totalSize, ProgressCapsule.PROGRESS_ONGOING);
			
			for (Enumeration iterator = matchedFileEntries.elements(); 
				(iterator.hasMoreElements() && !Thread.currentThread().isInterrupted()); 
				fileIndex++) 
			{
				ZipEntry zEntry = (ZipEntry) iterator.nextElement();
				File destFile = new File(dest+File.separator+zEntry.getName());
				InputStream zEntryInputStream = m_archive.getInputStream(zEntry);
				RecursiveFileTool.copySingleFile(zEntryInputStream, destFile);
				extractedFiles.addElement(zEntry.getName());
				sizeOfCopied += zEntry.getSize();
				notifyAboutChangesInterruptable(sizeOfCopied, totalSize, ProgressCapsule.PROGRESS_ONGOING);
			}
		}
		catch(IllegalStateException ise)
		{
			LogHolder.log(LogLevel.ERR, LogType.MISC, "Cannot extract archive "+m_archive.getName()+": file already closed");
			notifyAboutChanges(sizeOfCopied, totalSize, ProgressCapsule.PROGRESS_FAILED);
			return false;
		} 
		catch (InterruptedIOException ioe) 
		{
			LogHolder.log(LogLevel.DEBUG, LogType.MISC, "Process of extracting "+m_archive.getName()+" cancelled");
			extractErrorRollback(extractedFiles, destination);
			notifyAboutChanges(sizeOfCopied, totalSize, ProgressCapsule.PROGRESS_ABORTED);
			return false;
		}		 
		catch (InterruptedException e) 
		{
			LogHolder.log(LogLevel.DEBUG, LogType.MISC, "Process of extracting "+m_archive.getName()+" cancelled");
			extractErrorRollback(extractedFiles, destination);
			notifyAboutChanges(sizeOfCopied, totalSize, ProgressCapsule.PROGRESS_ABORTED);
			return false;
		}
		catch (Exception a_e)
		{
			LogHolder.log(LogLevel.ERR, LogType.MISC, "Cannot extract archive "+m_archive.getName()+": error occured: ", a_e);
			extractErrorRollback(extractedFiles, destination);
			notifyAboutChanges(sizeOfCopied, totalSize, ProgressCapsule.PROGRESS_FAILED);
			return false;
		}
		notifyAboutChanges(sizeOfCopied, totalSize, ProgressCapsule.PROGRESS_FINISHED);
		return true;
	}
	
	private void notifyAboutChanges(long sizeOfCopied, long totalSize, int progressStatus)
	{
		ZipEvent ze = new ZipEvent(sizeOfCopied, totalSize, progressStatus);
		setChanged();
		notifyObservers(ze);
	}
	
	private void notifyAboutChangesInterruptable(long sizeOfCopied, long totalSize, int progressStatus) throws InterruptedException
	{
		notifyAboutChanges(sizeOfCopied, totalSize, progressStatus);
		if(Thread.interrupted())
		{
			throw new InterruptedException();
		}
	}
	
	private static void extractErrorRollback(Vector entries, String destination) 
	{	
		for (int i = entries.size(); i > 0; i--)
		{
			File f = new File(destination+File.separator+ entries.elementAt(i-1));
			if (!f.exists())
			{
				continue;
			}
			String was = f.delete() ? " " : " not ";

			LogHolder.log((was.trim().length() == 0 ? LogLevel.DEBUG : LogLevel.ERR), 
					LogType.MISC, "Rollback: file "+f.getAbsolutePath()+was+"successfully deleted");
		}
	}

	public class ZipEvent implements ProgressCapsule
	{
		//public final static int UNDEFINED = -1;
		
		private int value;
		private int maxValue;
		private int minValue;
		private int status;
		
		public ZipEvent(long byteCount, long totalByteCount, int progressStatus)
		{
			minValue = 0;
			if(totalByteCount > Integer.MAX_VALUE)
			{
				double byteCountD = (double) byteCount;
				double totalByteCountD = (double) totalByteCount;
				double ratio = byteCountD / totalByteCountD;
				double valueD = ratio * Integer.MAX_VALUE;
				value = (int) valueD;
				maxValue = Integer.MAX_VALUE;
			}
			else
			{
				value = (int) byteCount;
				maxValue = (int) totalByteCount;
			}
			status = progressStatus;
		}
		
		public int getMaximum() {
			return maxValue;
		}

		public int getMinimum() {
			return minValue;
		}

		public int getValue() {
			return value;
		}

		public int getStatus() 
		{
			return status;
		}
	}
}
