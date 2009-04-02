/*
 Copyright (c) 2000, The JAP-Team
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

final public class LogLevel
{
	/** Indicates level type of message: Emergency message*/
	public final static int EMERG = 0;

	/** Indicates level type of message: Alert message */
	public final static int ALERT = 1;

	/** Indicates level type of message: For instance to  use when catching Exeption to output a debug message.*/
	public final static int EXCEPTION = 2; //2000-07-31(HF): CRIT zu EXCEPTION geaendert, wegen besserem Verstaendnis

	/** Indicates level type of message: Error message */
	public final static int ERR = 3;

	/** Indicates level type of message: Warning */
	public final static int WARNING = 4;

	/** Indicates level type of message: Notice */
	public final static int NOTICE = 5;

	/** Indicates level type of message: Information */
	public final static int INFO = 6;

	/** Indicates level type of message, e.g. a simple debugging message to output something */
	public final static int DEBUG = 7;
	private final static String STR_Levels[] =
		{
		"Emergency",
		"Alert    ",
		"Exception",
		"Error    ",
		"Warning  ",
		"Notice   ",
		"Info     ",
		"Debug    "
	};
	
	/**
	 * Returns the number of log levels. Each log level is represented by an integer
	 * value from 0 to getLevelCount() - 1.
	 * @return
	 */
	public static int getLevelCount()
	{
		return STR_Levels.length;
	}
	
	public static String getLevelName(int a_level)
	{
		if (a_level < 0 || a_level > STR_Levels.length - 1)
		{
			return null;
		}
		return STR_Levels[a_level];
	}

}
