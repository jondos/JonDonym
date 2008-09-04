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
package logging;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.StringTokenizer;

import anon.util.Util;

/**
 * This class stores the Log instance.
 */
public final class LogHolder
{
	/**
	 * prints the log message only
	 */
	public static final int DETAIL_LEVEL_LOWEST = 0;
	/**
	 * prints the log message, the class name and the source line of the log message
	 */
	public static final int DETAIL_LEVEL_LOWER = 1;
	/**
	 * prints the log message, package, classname, method name and the source line of the log message;
	 * this enables some IDEs to jump to the log source by clicking on the message line
	 */
	public static final int DETAIL_LEVEL_HIGH = 2;
	/**
	 * additionally prints the whole stack trace of an error log if available
	 */
	public static final int DETAIL_LEVEL_HIGHEST = 3;

	private static final String TRACED_LOG_MESSAGE = "[Traced log Message]:";
	private static final String LOGGED_THROWABLE = " Logged Throwable: ";
	private static final int LINE_LENGTH_HIGH_DETAIL = 40;
	private static final int LINE_LENGTH_HIGHEST_DETAIL = 70;

	/**
	 * Stores the instance of LogHolder (Singleton).
	 */
	private static LogHolder ms_logHolderInstance;

	/**
	 * The current detail level of all log messages.
	 */
	private static int m_messageDetailLevel = DETAIL_LEVEL_HIGHEST;

	/**
	 * Stores the Log instance.
	 */
	private static Log ms_logInstance = new DummyLog();

	/**
	 * This creates a new instance of LogHolder. This is only used for setting some
	 * values. Use LogHolder.getInstance() for getting an instance of this class.
	 */
	private LogHolder()
	{
		ms_logInstance = new DummyLog();
	}

	public void finalize() throws Throwable
	{
		if (equals(ms_logHolderInstance))
		{
			ms_logHolderInstance = null;
		}

		super.finalize();
	}

	/**
	 * Sets the detail level of all log messages. Use one of the class constants to set it.
	 * The detail levels range from 0 (DETAIL_LEVEL_LOWEST) to DETAIL_LEVEL_HIGHEST.
	 * The higher the detail level, the more detailed information will be written to the logs.
	 * @param a_messageDetailLevel the detail level of all log messages
	 */
	public static void setDetailLevel(int a_messageDetailLevel)
	{
		if (a_messageDetailLevel < DETAIL_LEVEL_LOWEST)
		{
			m_messageDetailLevel = DETAIL_LEVEL_LOWEST;
		}
		else if (a_messageDetailLevel > DETAIL_LEVEL_HIGHEST)
		{
			m_messageDetailLevel = DETAIL_LEVEL_HIGHEST;
		}
		else
		{
			m_messageDetailLevel = a_messageDetailLevel;
		}
	}

	/**
	 * Gets the detail level of all log messages.
	 * @return a_messageDetailLevel the detail level of all log messages
	 */
	public static int getDetailLevel()
	{
		return m_messageDetailLevel;
	}

	/**
	 * Write the log data for a Throwable to the Log instance.
	 *
	 * @param a_logLevel The log level (see constants in class LogLevel).
	 * @param a_logType The log type (see constants in class LogType).
	 * @param a_throwable a Throwable to log
	 */
	public static synchronized void log(int a_logLevel, int a_logType, Throwable a_throwable)
	{
		log(a_logLevel, a_logType, null, a_throwable);
	}

	/**
	 * Write the log data for a Throwable to the Log instance.
	 *
	 * @param a_logLevel The log level (see constants in class LogLevel).
	 * @param a_logType The log type (see constants in class LogType).
	 * @param a_message an (optional) log message
	 * @param a_throwable a Throwable to log
	 */
	public static synchronized void log(int a_logLevel, int a_logType, String a_message,
										Throwable a_throwable)
	{
		if (a_throwable == null)
		{
			log(a_logLevel, a_logType, (String)null);
			return;
		}

		if (isLogged(a_logLevel, a_logType))
		{
			String message = "";
			if (a_message != null && a_message.length() > 0)
			{
				message = a_message;
			}

			if (m_messageDetailLevel <= DETAIL_LEVEL_LOWEST)
			{
				LogHolder.getLogInstance().log(a_logLevel, a_logType, a_throwable.getMessage());
			}
			else if (m_messageDetailLevel > DETAIL_LEVEL_LOWEST &&
					 m_messageDetailLevel < DETAIL_LEVEL_HIGHEST)
			{
				if (message.length() == 0)
				{
					message = a_throwable.getMessage();
				}
				else
				{
					message =message+ "\n" + LOGGED_THROWABLE + a_throwable.getMessage();
				}

				LogHolder.getLogInstance().log(a_logLevel, a_logType, a_throwable.toString());
			}
			else if (m_messageDetailLevel == DETAIL_LEVEL_HIGH)
			{
				if (message.length() == 0)
				{
					message = a_throwable.toString();
				}
				else
				{
					message += "\n" + LOGGED_THROWABLE + a_throwable.toString();
				}

				LogHolder.getLogInstance().log(a_logLevel, a_logType,
					Util.normaliseString(getCallingClassFile(false) + ": ", LINE_LENGTH_HIGH_DETAIL) +
					message);
			}
			else if (m_messageDetailLevel >= DETAIL_LEVEL_HIGHEST)
			{
				if (message.length() == 0)
				{
					message = Util.getStackTrace(a_throwable);
				}
				else
				{
					message += "\n" + LOGGED_THROWABLE + Util.getStackTrace(a_throwable);
				}

				LogHolder.getLogInstance().log(a_logLevel, a_logType,
					Util.normaliseString(
						getCallingMethod(false) + ": ", LINE_LENGTH_HIGHEST_DETAIL) +
					message);
			}
		}
	}

	/**
	 * Write the log data to the Log instance.
	 *
	 * @param logLevel The log level (see constants in class LogLevel).
	 * @param logType The log type (see constants in class LogType).
	 * @param message The message to log.
	 * @param a_bAddCallingClass true if not only the name and class of the current method should be logged
	 *                           but also the name of the method in the class that has called this method;
	 *                           false if only the name of the current method should be logged (default)
	 */
	public static void log(int logLevel, int logType, String message, boolean a_bAddCallingClass)
	{
		if (isLogged(logLevel, logType))
		{
			if (m_messageDetailLevel <= DETAIL_LEVEL_LOWEST)
			{
				ms_logInstance.log(logLevel, logType, message);
			}
			else if (m_messageDetailLevel == DETAIL_LEVEL_LOWER)
			{
				if (a_bAddCallingClass)
				{
					ms_logInstance.log(logLevel, logType,
									   Util.normaliseString(
										   getCallingClassFile(false) + ": ", LINE_LENGTH_HIGH_DETAIL)
									   + TRACED_LOG_MESSAGE);

				}
				ms_logInstance.log(logLevel, logType,
								   Util.normaliseString(
									   getCallingClassFile(a_bAddCallingClass) + ": ",
									   LINE_LENGTH_HIGH_DETAIL)
								   + message);
			}
			else
			{
				if (a_bAddCallingClass)
				{
					ms_logInstance.log(logLevel, logType,
									   Util.normaliseString(
										   getCallingMethod(false) + ": ", LINE_LENGTH_HIGHEST_DETAIL)
									   + TRACED_LOG_MESSAGE);
				}
				ms_logInstance.log(logLevel, logType,
								   Util.normaliseString(
									   getCallingMethod(a_bAddCallingClass) + ": ",
									   LINE_LENGTH_HIGHEST_DETAIL)
								   + message);
			}
		}
	}

	/**
	 * Write the log data to the Log instance.
	 *
	 * @param logLevel The log level (see constants in class LogLevel).
	 * @param logType The log type (see constants in class LogType).
	 * @param message The message to log.
	 */
	public static void log(int logLevel, int logType, String message)
	{
		log(logLevel, logType, message, false);
	}

	/**
	 * Sets the logInstance.
	 *
	 * @param logInstance The instance of a Log implementation.
	 */
	public static synchronized void setLogInstance(Log logInstance)
	{
		LogHolder.ms_logInstance = logInstance;
		if (LogHolder.ms_logInstance == null)
		{
			LogHolder.ms_logInstance = new DummyLog();
		}
	}

	/**
	 * Returns the logInstance. If the logInstance is not set, there is a new DummyLog instance
	 * returned.
	 *
	 * @return The current logInstance.
	 */
	private static Log getLogInstance()
	{
		return ms_logInstance;
	}

	public static boolean isLogged(int a_logLevel, int a_logType)
	{
		return (a_logLevel <= ms_logInstance.getLogLevel()) &&
			( (a_logType & ms_logInstance.getLogType()) == a_logType);
	}

	/**
	 * Returns the filename and line number of the calling method (from outside
	 * this class) in the form <Code> (class.java:<LineNumber>) </Code>.
	 * @param a_bSkipOwnClass if true, the true calling method is skipped and the class file
	 *                        of the first method in the stack trace of the calling class
	 *                        that is in another class than itself is returned;
	 *                        if false, the class file of the calling method is returned (default)
	 *
	 * @return the filename and line number of the calling method
	 */
	private static String getCallingClassFile(boolean a_bSkipOwnClass)
	{
		String strClassFile = getCallingMethod(a_bSkipOwnClass);
		strClassFile = strClassFile.substring(strClassFile.indexOf('('), strClassFile.indexOf(')') + 1);
		return strClassFile;
	}

	/**
	 * Returns the name, class, file and line number of the calling method (from outside
	 * this class) in the form <Code> package.class.method(class.java:<LineNumber>) </Code>.
	 * This method does need some processing time, as an exception with the stack trace is generated.
	 * @param a_bSkipOwnClass if true, the true calling method is skipped and the caller of the
	 *                        first method in the stack trace of calling method that is in another
	 *                        class than itself is returned; if false, the calling method is
	 *                             returned (default)
	 * @return the name, class and line number of the calling method
	 */
	private static String getCallingMethod(boolean a_bSkipOwnClass)
	{
		StringTokenizer tokenizer;
		String strCurrentMethod = "";
		StringWriter swriter = new StringWriter();
		PrintWriter pwriter = new PrintWriter(swriter);
		String strOwnClass = "   ";
		int index;

		new Exception().printStackTrace(pwriter);

		tokenizer = new StringTokenizer(swriter.toString());
		tokenizer.nextToken(); // jump over the exception message
		while (tokenizer.hasMoreTokens())
		{
			tokenizer.nextToken(); // jump over the "at"

			/* identify the current stack trace method */
			strCurrentMethod = tokenizer.nextToken().replace('/', '.');
			if (strCurrentMethod.indexOf('(') > 0)
			{
				while (strCurrentMethod.indexOf(')') < 0)
				{
					strCurrentMethod += tokenizer.nextToken();
				}
			}
			/* jump over all local class calls */
			if (!strCurrentMethod.startsWith(LogHolder.class.getName()) &&
				!strCurrentMethod.startsWith(strOwnClass) &&
				!strCurrentMethod.startsWith(Throwable.class.getName()) &&
				!strCurrentMethod.startsWith(Exception.class.getName()))
			{
				if (a_bSkipOwnClass && strOwnClass.trim().length() == 0)
				{
					// get the class name of the calling class
					strOwnClass = strCurrentMethod;
					if ( (index = strCurrentMethod.indexOf('(')) > 0)
					{
						strOwnClass = strCurrentMethod.substring(0, index);
					}
					strOwnClass = strOwnClass.substring(0, strOwnClass.lastIndexOf('.'));
					// filter out internal classes
					if (strOwnClass.indexOf("$") > 0)
					{
						strOwnClass = strOwnClass.substring(0, strOwnClass.indexOf("$"));
					}
				}
				else
				{
					break;
				}
			}
		}
		return strCurrentMethod;
	}
}
