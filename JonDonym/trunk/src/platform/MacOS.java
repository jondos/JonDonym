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

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.zip.ZipFile;

import anon.util.ClassUtil;
import anon.util.RecursiveFileTool;
import anon.util.Util;
import anon.util.ZipArchiver;

import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;



/**
 * This class is instantiated by AbstractOS if the current OS is MacOS or MacOSX
 */
public class MacOS extends AbstractOS
{
	public static final String OS_NAME = "Mac OS";
	final static String BUNDLE_CONTENTS = "Contents"+File.separator;
	final static String BUNDLE_RESOURCES = BUNDLE_CONTENTS+"Resources"+File.separator;
	final static String BUNDLE_MAC_OS_EXECUTABLES = BUNDLE_CONTENTS+"MacOS"+File.separator;
	final static String BUNDLE_PROPERTY_FILE_NAME = "Info.plist";
	final static String BUNDLE_EXECUTABLE_PROPERTY_KEY = "CFBundleExecutable";
	
	/* some constants for the root copy method which is based on
	 * a interpreted apple script
	 */	
	final static String ROOT_SHELLSCRIPT_NAME = "rootShellScript";
	final static String OSA_EXEC_SHELLSCRIPT_STMT = 
		"do shell script " + ROOT_SHELLSCRIPT_NAME + 
		" with administrator privileges";
	
	final static String OSA_APPLET_NAME = "JonDoUpdater.app";
	final static String OSA_APPLET_PATH = (getDefaultTempPath() != null) ? getDefaultTempPath()+OSA_APPLET_NAME : null;
											
	/** Mac OS X built-in command for interpreting AppleScript statements */
	final static String[] OSASCRIPT_CMD  = new String[]{"osascript"};
	/** Mac OS X built-in command for compiling AppleScript code */
	final static String[] OSACOMPILE_CMD  
		= (OSA_APPLET_PATH != null) ? new String[]{"osacompile","-xo", OSA_APPLET_PATH} : null;
	final static String[] OPEN_UPDATER_CMD
		= (OSA_APPLET_PATH != null) ? new String[]{OSA_APPLET_PATH+File.separator+
													BUNDLE_MAC_OS_EXECUTABLES+"applet"} : null;
	
	//private HashMap m_bundleProperties = null;
	private String m_bundlePath = null;
	
	public MacOS() throws Exception
	{
		if (System.getProperty("mrj.version") == null)
		{
			throw new Exception("Operating system is not "+ OS_NAME);
		}
		//m_bundleProperties = new HashMap();
		setBundlePath();
		//loadBundleProperties();	
	}

	/*
	public boolean openURL(URL a_url)
	{
		//MRJFileUtils.openURL(a_url.toString());
		return openLink(a_url.toString());
	}*/
	
	
	protected boolean openLink(String a_link)
	{
		String urlString = Util.encodeWhiteSpaces(a_link);
		try
		{
			Runtime.getRuntime().exec("open " + urlString);
			return true;
		}
		catch (Exception ex)
		{
			LogHolder.log(LogLevel.ERR, LogType.MISC,
						  "Cannot open '" + urlString + "' in MacOS default program.");
		}

		return false;
	}

	public boolean isHelpAutoInstalled()
	{
		return true;
	}
	
	public String getConfigPath(String a_applicationName)
	{
		//Return path in users's home/Library/Preferences
		if (System.getProperty("os.name").equalsIgnoreCase(OS_NAME))
		{
			return System.getProperty("user.home", ".") +"/";
		}
		else
		{
			return System.getProperty("user.home", "") + "/Library/Preferences/";
		}
	}
	
	/* returns absolute path to application bundle or null,
	 * if JAP is not executed as application bundle
	 */
	public void setBundlePath()
	{
		File classParentFile = ClassUtil.getClassDirectory(this.getClass());
		if(classParentFile != null)
		{
			String path = classParentFile.getPath();
			if(path != null)
			{
				// remove file: prefix
				if(!(path.startsWith(File.separator)))
				{
					int s_index = path.indexOf("/");
					path = (s_index != -1) ? path.substring(s_index) : path;
				}
				int index_path = path.indexOf(BUNDLE_CONTENTS);
				if(index_path != -1)
				{
					/* JAP is started as an  application bundle */
					m_bundlePath = path.substring(0, index_path-1);
					return;
				}
			}
		}
		m_bundlePath = null;
	}
	
	public String getBundlePath()
	{
		return m_bundlePath;
	}
	
	public boolean isBundle() {
		return (m_bundlePath != null);
	}
	
	/* load the bundle properties specified in the Info.plist property file */
	/*protected void loadBundleProperties()
	{
		if(isBundle())
		{
			try 
			{
				File bundlePropertyFile = new File(new URI("file://"+m_bundlePath+File.separator+
						BUNDLE_CONTENTS+BUNDLE_PROPERTY_FILE_NAME));
				
				Document bundlePropertyDoc = XMLUtil.readXMLDocument(bundlePropertyFile);
				if(bundlePropertyDoc != null)
				{
					NodeList bundlePropertyDicts = bundlePropertyDoc.getElementsByTagName("dict");
					if(bundlePropertyDicts != null)
					{
						int bprop_length = bundlePropertyDicts.getLength();
						for(int index = 0; index < bprop_length; index++)
						{
							Node dictNode = bundlePropertyDicts.item(index);
							if(dictNode.hasChildNodes())
							{
								NodeList dictChildNodes = dictNode.getChildNodes();
								int nrChildNodes = dictChildNodes.getLength();
								String keyName = null;
								for (int i = 0; i < nrChildNodes; i++) {
									Node dictChildeNode = dictChildNodes.item(i);
									if(dictChildeNode.getNodeName().equals("key"))
									{
										keyName = dictChildeNode.getTextContent();
									} 
									else if(dictChildeNode.getNodeName().equals("string"))
									{
										if(keyName != null)
										{
											m_bundleProperties.put(keyName, dictChildeNode.getTextContent());
											keyName = null;
										}
									}
								}
							}
						}
					}
				}
			} 
			catch (IOException ioe) 
			{
				LogHolder.log(LogLevel.ERR, LogType.MISC,
						"Cannot open bundle property file: "+BUNDLE_PROPERTY_FILE_NAME+", cause:", ioe);
			} 
			catch (XMLParseException xpe) 
			{
				LogHolder.log(LogLevel.ERR, LogType.MISC,
						"Cannot parse bundle property file: "+BUNDLE_PROPERTY_FILE_NAME+", cause:", xpe);
			} catch (URISyntaxException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}*/
	
	/* returns absolute path to application bundle executable
	 * which is the stub to execute the jar file
	 * returns null, if JAP is not executed as an application bundle
	 */
	public String getBundleExecutablePath()
	{
		/*if(!isBundle())
		{
			return null;
		}
		String bundleExecutable = 
			(String) m_bundleProperties.get(BUNDLE_EXECUTABLE_PROPERTY_KEY);
		if (bundleExecutable == null)
		{
			return null;
		}
		return getBundlePath()+File.separator+BUNDLE_MAC_OS_EXECUTABLES+bundleExecutable;*/
		return null;
	}

	/**
	 * handle some AppleScriptStatements by passing them to the stdin of the specified
	 * handler process. The process can be either an osacript-Process which interpretes and
	 * executes the statements immediately or a osacompile-process which creates and executable
	 * outputfile
	 * @return returns the exit value of the handler process
	 */
	private static int handleAppleScriptCmds(String[] statements, Process handler) 
		throws IOException, InterruptedException
		
	{
		PrintWriter stdinWriter = new PrintWriter(handler.getOutputStream());
		for (int i = 0; i < statements.length; i++) 
		{
			stdinWriter.println(statements[i]);
		}
		stdinWriter.flush();
		stdinWriter.close();
		return handler.waitFor();
	}
	
	public boolean copyAsRoot(File src, File destDir, AbstractRetryCopyProcess a_checkRetry) 
	{
		String osaShellscript_stmt = 
			"set " + ROOT_SHELLSCRIPT_NAME + " to "+
			"\"cp "+src.getAbsolutePath()+" "+destDir.getAbsolutePath()+"\"";
		
		String[] allStmts = 
			new String[]
			{
				osaShellscript_stmt, 
				OSA_EXEC_SHELLSCRIPT_STMT
			};
		
		try
		{
			Runtime runtime = Runtime.getRuntime();
			int exitValue = 1;
			if(OSACOMPILE_CMD != null)
			{
				Process osaCompiler = runtime.exec(OSACOMPILE_CMD);
				exitValue = handleAppleScriptCmds(allStmts, osaCompiler);
			}
			if(exitValue == 0)
			{
				ZipFile japArchive = ClassUtil.getJarFile();
				if(japArchive != null)
				{
					ZipArchiver archiver = new ZipArchiver(japArchive);
					File oldImage = new File(OSA_APPLET_PATH+File.separator+
												BUNDLE_RESOURCES+"applet.icns");
					oldImage.delete();
					//TODO: better use a proper update icon.
					archiver.extractSingleEntry("images/JUpdate.icns", 
													OSA_APPLET_PATH+File.separator+
													BUNDLE_RESOURCES+"applet.icns");	
				}
				
				Process execCopy = runtime.exec(OPEN_UPDATER_CMD);
				exitValue = execCopy.waitFor();
			}
			else
			{
				Process osaInterpreter = runtime.exec(OSASCRIPT_CMD);
				exitValue = handleAppleScriptCmds(allStmts, osaInterpreter);
			}
			
			if(OSA_APPLET_PATH != null)
			{
				File appletFile = new File(OSA_APPLET_PATH);
				if(appletFile.exists() && (OSA_APPLET_PATH.endsWith(OSA_APPLET_NAME)))
				{
					RecursiveFileTool.deleteRecursion(appletFile);
				}
			}
			return RecursiveFileTool.equals(src, new File(destDir.getAbsolutePath()+File.separator+src.getName()), true);
			
		}
		catch(IOException ioe)
		{
			LogHolder.log(LogLevel.INFO, LogType.MISC, "Mac OS root copy failed: ", ioe);
			return false;
		}
		catch(InterruptedException ie)
		{
			LogHolder.log(LogLevel.EXCEPTION, LogType.MISC, "Interrupted while waiting for root copy process ", ie);
			return false;
		}
	}
}
