/*
 Copyright (c) 2000-2005, The JAP-Team
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

/* Hint: This file may be only a copy of the original file which is always in the JAP source tree!
 * If you change something - do not forget to add the changes also to the JAP source tree!
 */
package logging;

/**
 * This class holds the available log types. Log types may be combined by bitwise or(|).
 * @see logging.LogHolder
 */
public final class LogType
{
	/** The human readyble names of all log types. New log types must be added here, too. */
	private static final String[] STR_LOG_TYPES =
		{"NUL", "GUI", "NET", "THREAD", "MISC", "PAY", "TOR", "CRYPTO", "FILTER", "AGREEMENT", "DB", "ALL"};

	/** The concatenation string of the human readable log types. */
	private static final String STR_ADD_LOG_TYPE = "+";

	/** The integer values of all log types. */
	private static final int[] LOG_TYPES = getAvailableLogTypes();

	public static final int NUL = LOG_TYPES[0];

	/** Indicates a GUI related message (binary: <code>00000001</code>) */
	public static final int GUI = LOG_TYPES[1];

	/** Indicates a network related message (binary: <code>00000010</code>) */
	public static final int NET = LOG_TYPES[2];

	/** Indicates a thread related message (binary: <code>00000100</code>) */
	public static final int THREAD = LOG_TYPES[3];

	/** Indicates a misc message (binary: <code>00001000</code>) */
	public static final int MISC = LOG_TYPES[4];

	/** Indicates a pay message (binary: <code>00001000</code>) */
	public static final int PAY = LOG_TYPES[5];

	/** Indicates a TOR message (binary: <code>00010000</code>) */
	public static final int TOR = LOG_TYPES[6];

	/** Indicates a message related to cryptographic operations (binary: <code>00100000</code>) */
	public static final int CRYPTO = LOG_TYPES[7];

	/** Indicates messages from the paw filter system */
	public static final int PAW = LOG_TYPES[8];

	/** Indicates messages from the http filter system */
	public static final int FILTER = LOG_TYPES[8];

    /** Indicates messages from the paw filter system */
    public static final int AGREEMENT = LOG_TYPES[9];

	/** messages about database records, e.g. those to be cleaned up * */
	public static final int DB = LOG_TYPES[10];

	/** Indicates all messages*/
	public static final int ALL = createLogTypeALL();

	/**
	 * Instances of this class are not allowed.
	 */
	private LogType()
	{
	}

	/**
	 * Returns if the given LogType is a valid log type. Valid means that the LogType is one of those
	 * defined in this class.
	 * @param a_logType a log type
	 * @return if the given LogType is a valid log type
	 */
	public static boolean isValidLogType(int a_logType)
	{
		return (a_logType >= 0 && a_logType < STR_LOG_TYPES.length);
	}

	/**
	 * Returns the integer values of all available log types. Log types may be combined by using the
	 * bitwise or(|)-operation.
	 * @return the integer values of all available log types
	 */
	public static int[] getAvailableLogTypes()
	{
		int[] logTypes = new int[STR_LOG_TYPES.length - 1];

		logTypes[0] = 0;

		for (int i = 1, j = 1; i < logTypes.length; i++)
		{
			logTypes[i] = j;
			j <<= 1;
		}

		return logTypes;
	}

	/**
	 * Returns the number of all available log types.
	 * @return the number of all available log types
	 */
	public static int getNumberOfLogTypes()
	{
		return STR_LOG_TYPES.length - 1;
	}

	/**
	 * Returns the name of the given log type as a human readable string. If more than one log type
	 * is used, the string is concatenated from the used log types.
	 * @param a_logType a log type (may consist of several log types that have been combined)
	 * @return the name of the given log type
	 */
	public static String getLogTypeName(int a_logType)
	{
		String strLogTypeName = "";

		if (a_logType == 0)
		{
			strLogTypeName = STR_LOG_TYPES[0];
		}
		else if ((a_logType & ALL) == ALL)
		{
			strLogTypeName = STR_LOG_TYPES[STR_LOG_TYPES.length - 1];
		}
		else
		{
			for (int i = 1; i < LOG_TYPES.length; i++)
			{
				if ((a_logType & LOG_TYPES[i]) > 0)
				{
					strLogTypeName += STR_LOG_TYPES[i] + STR_ADD_LOG_TYPE;
				}
			}
			if (strLogTypeName.length() == 0)
			{
				strLogTypeName = STR_LOG_TYPES[0];
			}
			else
			{
				strLogTypeName = strLogTypeName.substring(0,
					strLogTypeName.length() - STR_ADD_LOG_TYPE.length());
			}
		}

		return strLogTypeName;
	}

	private static int createLogTypeALL()
	{
		int all = 0;

		for (int i = 0; i < LOG_TYPES.length; i++)
		{
			all += LOG_TYPES[i];
		}

		return all;
	}
}
