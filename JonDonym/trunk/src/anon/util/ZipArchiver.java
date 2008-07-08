package anon.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.Iterator;
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
	
	/*public boolean extractArchive(String pathName, String destination)
	{
		
		long maxLen = 100l * ((long)Integer.MAX_VALUE);
		notifyAboutTotalExtractSize(maxLen);
		System.out.println("Max Len: "+maxLen);
		for (long i = 0; i <= maxLen; i+=((long)Integer.MAX_VALUE)) {
			notifyAboutExtractedEntry("dummy "+i, 879, i);
			try {
				Thread.sleep(50);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return true;
	}*/
	
	public boolean extractArchive(String pathName, String destination)
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
		
		long totalSize = 0;
		long sizeOfCopied = 0;
		//TreeSet matchedEntries
		
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
			while(allZipEntries.hasMoreElements() )
			{
				entry = (ZipEntry) allZipEntries.nextElement();
				
				entryName = entry.getName();
				if( (pathName == null) || (entryName.startsWith(pathName)) )
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
						matchedDirEntries.add(index, entryName);
					}
					else
					{
						matchedFileEntries.add(entry);
					}
				}
			}
			if( (matchedFileEntries.size() == 0) && (matchedDirEntries.size() == 0) )
			{
				LogHolder.log(LogLevel.ERR, LogType.MISC, "No matching files for "+pathName+"found in archive "+m_archive.getName());
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
								m_archive.getName()+": could not create directory "+dir.getName());
						extractErrorRollback(matchedDirEntries, dirIndex, dest);
						return false;
					}
				}
			}
			ZipEvent ze = new ZipEvent(0, totalSize);
			setChanged();
			notifyObservers(ze);
			
			for (Enumeration iterator = matchedFileEntries.elements(); iterator.hasMoreElements(); fileIndex++) 
			{
				ZipEntry zEntry = (ZipEntry) iterator.nextElement();
				File destFile = new File(dest+File.separator+zEntry.getName());
				InputStream zEntryInputStream = m_archive.getInputStream(zEntry);
				RecursiveCopyTool.copySingleFile(zEntryInputStream, destFile);
				sizeOfCopied += zEntry.getSize();
				
				ze = new ZipEvent(sizeOfCopied, totalSize);
				setChanged();
				notifyObservers(ze);
			
				/*try {
					Thread.sleep(20);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}*/
			}
		}
		catch(IllegalStateException ise)
		{
			LogHolder.log(LogLevel.ERR, LogType.MISC, "Cannot extract archive "+m_archive.getName()+": file already closed");
			return false;
		} 
		catch (IOException ioe) 
		{
			LogHolder.log(LogLevel.ERR, LogType.MISC, "Cannot extract archive "+m_archive.getName()+": I/O error occured: ", ioe);
			extractErrorRollback(matchedFileEntries, fileIndex, destination);
			extractErrorRollback(matchedDirEntries, matchedDirEntries.size(), destination);
			return false;
		}
		return true;
	}
	
	private static void extractErrorRollback(Vector entries, int dirIndex, String destination) 
	{	
		for(int i = dirIndex; i > 0; i--)
		{
			File f = new File(destination+File.separator+
								entries.elementAt(i-1));
			String was = f.delete() ? " " : " not ";
			LogHolder.log(LogLevel.DEBUG, LogType.MISC, "Rollback: file "+f.getName()+was+"successfully deleted");
		}
	}

	public class ZipEvent implements ProgressCapsule
	{
		public final static int UNDEFINED = -1;
		
		private int value;
		private int maxValue;
		private int minValue;
		
		public ZipEvent(long byteCount, long totalByteCount)
		{
			minValue = 0;
			value = (totalByteCount > Integer.MAX_VALUE) ? 
					(int) ((double) byteCount / (double) totalByteCount)*Integer.MAX_VALUE : (int) byteCount;
			maxValue = (totalByteCount > Integer.MAX_VALUE) ? 
				Integer.MAX_VALUE : (int) totalByteCount;
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
	}
}
