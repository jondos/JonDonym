package anon.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.bouncycastle.crypto.digests.MD5Digest;

import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;

public class RecursiveFileTool {
	
	private final static int INIT_DEPTH = 0;
	private final static int MAX_DEPTH_IGNORE = -2;
	private final static int COPY_BUFFER_SIZE = 1024;
	private final static int EOF = -1;
	
	
	public static void copy(File src, File dest)
	{
		if(src == null)
		{
			LogHolder.log(LogLevel.EXCEPTION, LogType.MISC, 
					"Source file is null: This should never happen");
			return;
		}
		if(dest == null)
		{
			LogHolder.log(LogLevel.EXCEPTION, LogType.MISC, 
					"Destination file is null: This should never happen");
			return;
		}
		if(src.isDirectory())
		{
			LogHolder.log(LogLevel.ERR, LogType.MISC, 
					"File "+src.getName()+" is a directory: cannot copy it");
			return;
		}
		else if(!src.exists())
		{
			LogHolder.log(LogLevel.ERR, LogType.MISC, 
					"There is no such file or directory: "+src.getName());
			return;
		}
		else
		{
			try {
				copySingleFile(src, dest);
			} catch (IOException ioe) {
				LogHolder.log(LogLevel.EXCEPTION, LogType.MISC, 
						"An IO Exception while copying file "+src.getName()+": "+ioe.getMessage());
			}
		}
	}
	
	public static void copyRecursive(File src, File dest)
	{
		copyRecursion(src, dest, INIT_DEPTH, MAX_DEPTH_IGNORE);
	}
	
	public static void copyRecursive(File src, File dest, int maxDepth)
	{
		copyRecursion(src, dest, INIT_DEPTH, maxDepth);
	}
	
	private static void copyRecursion(File src, File dest, int depth, int maxDepth)
	{
		if(src == null)
		{
			LogHolder.log(LogLevel.EXCEPTION, LogType.MISC, 
					"Source file is null: This should never happen");
			return;
		}
		if(dest == null)
		{
			LogHolder.log(LogLevel.EXCEPTION, LogType.MISC, 
					"Destination file is null: This should never happen");
			return;
		}
		if( dest.getAbsolutePath().startsWith(src.getAbsolutePath()) )
		{
			LogHolder.log(LogLevel.ERR, LogType.MISC, 
					"destination path is in source path: to avoid endless loops, operation is not allowed");
			return;
		}
		if(!src.exists())
		{
			LogHolder.log(LogLevel.ERR, LogType.MISC, 
					"There is no such file or directory: "+src.getName());
			return;
		}
		
		if(src.isDirectory())
		{
			String[] filesInCurrentDirectory = src.list();
			boolean directoryCreated = dest.mkdir();
			if(directoryCreated)
			{
				for (int i = 0; i < filesInCurrentDirectory.length; i++) 
				{
					String currentFileName = filesInCurrentDirectory[i];
					if( (maxDepth == MAX_DEPTH_IGNORE) || (depth < maxDepth) )
					{
						//descend only if maximum Depths is ignored or not reached yet.  
						copyRecursion(new File(src.getAbsolutePath()+File.separator+currentFileName), 
										new File(dest.getAbsolutePath()+File.separator+currentFileName),
										(depth+1), maxDepth);
					}
				}
			}
			else
			{
				LogHolder.log(LogLevel.ERR, LogType.MISC, 
						"Cannot create directory: "+dest.getName());
				return;
			}
		}
		else
		{
			//recursion anchor
			try {
				copySingleFile(src, dest);
			} catch (IOException ioe) {
				LogHolder.log(LogLevel.EXCEPTION, LogType.MISC, 
						"An IO Exception while copying file "+src.getName()+": "+ioe.getMessage());
			}
		}
	}
	
	
	static void copySingleFile(File src, File dest) throws IOException
	{
		FileInputStream fromSrcFile = new FileInputStream(src);
		copySingleFile(fromSrcFile, dest);
	}
	
	/* WARNING False usage of this method is dangerous! */
	public static boolean deleteRecursion(File src)
	{
		if(src == null)
		{
			LogHolder.log(LogLevel.EXCEPTION, LogType.MISC, 
					"Source file is null: This should never happen");
			return true;
		}
		if(!src.exists())
		{
			LogHolder.log(LogLevel.ERR, LogType.MISC, 
					"There is no such file or directory: "+src.getName());
			return true;
		}
		if(src.isDirectory())
		{
			String[] filesInCurrentDirectory = src.list();
			for (int i = 0; filesInCurrentDirectory != null &&
				i < filesInCurrentDirectory.length; i++) 
			{
				String currentFileName = filesInCurrentDirectory[i];
				deleteRecursion(new File(src.getAbsolutePath()+File.separator+currentFileName));
			}
		}
		String lstr = src.getName() + 
						(src.delete() ? " was successfully deleted." : 
										" was not successfully deleted.");
		LogHolder.log(LogLevel.DEBUG, LogType.MISC, lstr);
		return !src.exists();
	}
	
	static void copySingleFile(InputStream src, File dest) throws IOException
	{
		IOException ex = null;
		
		if(src == null)
		{
			LogHolder.log(LogLevel.ERR, LogType.MISC, "Abort copy process: InputStream is null");
			return;
		}
		FileOutputStream toDestFile = null;
		try {
			toDestFile = new FileOutputStream(dest);
			byte[] copyBuffer = new byte[COPY_BUFFER_SIZE];
			int bytesReadFromSrcFile = 1;
			
			while(src.available() > 0)
			{
				bytesReadFromSrcFile = src.read(copyBuffer);
				if(bytesReadFromSrcFile == EOF) break;
				toDestFile.write(copyBuffer, 0, bytesReadFromSrcFile);
			}
		}
		catch(IOException ioe)
		{
			ex = ioe;
			/* Catch this Exception just to close the streams */ 			
		}
		try 
		{
			if(toDestFile != null)
			{
				toDestFile.close();
			}
			src.close();
		}
		catch (IOException ioe1) 
		{
		}
		if (ex != null)
		{
			throw ex;
		}
	}
	
	public static boolean equals(File a_oneFile, byte[] a_md5HashSecond, long a_sizeSecond) 
	{
		try 
		{
			if (Util.arraysEqual(createMD5Digest(a_oneFile), a_md5HashSecond))
			{
				return true;
			}
		}
		catch (Exception a_e) 
		{
			if (a_oneFile.length() == a_sizeSecond)
			{
				return true;
			}
		} 
		
		return false;
	}
	
	/**
	 * Compares two files. May optionally do a comparison of MD5 hashes if speed does not matter.
	 * @param a_oneFile
	 * @param a_otherFile
	 * @param a_hashComparison if true, the MD5 hashes of the two files are compared if they seem to
	 *                         be equal after a quick check; false otherwise
	 * @return
	 */
	public static boolean equals(File a_oneFile, File a_otherFile, boolean a_doHashComparison)
	{
		if (a_oneFile == null || a_otherFile == null)
		{
			return false;
		}
		
		try
		{
			if (!a_oneFile.exists() || !a_otherFile.exists() || 
				a_oneFile.length() != a_otherFile.length())
			{
				return false;
			}
			
			if (!a_doHashComparison)
			{
				// this was a quick comparison only, do not read the files
				return true;
			}
			
			if (!Util.arraysEqual(createMD5Digest(a_oneFile), createMD5Digest(a_otherFile)))
			{
				return false;
			}
		}
		catch (Exception a_e)
		{
			LogHolder.log(LogLevel.EXCEPTION, LogType.MISC, a_e);
			if (a_oneFile.length() == a_otherFile.length())
			{
				// maybe we succeeded, but we cannot be sure...
				return true;
			}
		}
		
		return true;
	}
	
	public static long getFileSize(File a_file) throws SecurityException
	{
		if (a_file == null || !a_file.exists())
		{
			return -1;
		}
		return a_file.length();
	}
	
	public static byte[] createMD5Digest(File a_file) throws IOException, SecurityException
	{
		byte[] content;
		MD5Digest digest;
		byte[] hash;
		
		content = ResourceLoader.getStreamAsBytes(new FileInputStream(a_file));
		digest = new MD5Digest();
		hash = new byte[digest.getDigestSize()];
		digest.update(content, 0, content.length);
		digest.doFinal(hash, 0);
		return hash;
	}
}
