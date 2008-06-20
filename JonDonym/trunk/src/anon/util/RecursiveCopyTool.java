package anon.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;

public class RecursiveCopyTool {
	
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
	
	private static void copySingleFile(File src, File dest) throws IOException
	{
		FileInputStream fromSrcFile = new FileInputStream(src);
		FileOutputStream toDestFile = new FileOutputStream(dest);
		byte[] copyBuffer = new byte[COPY_BUFFER_SIZE];
		int bytesReadFromSrcFile = 0;
		
		while(fromSrcFile.available()>0)
		{
			bytesReadFromSrcFile = fromSrcFile.read(copyBuffer);
			toDestFile.write(copyBuffer);
			if(bytesReadFromSrcFile == EOF) break;
		}
		fromSrcFile.close();
		toDestFile.close();
	}
	
	public static void main(String args[])
	{
		copyRecursive(new File(args[0]), new File(args[1]));
	}
}
