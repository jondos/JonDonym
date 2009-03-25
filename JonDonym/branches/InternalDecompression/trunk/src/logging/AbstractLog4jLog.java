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

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

/**
 * This is the logging service implementation for compatibility between JAP logging style and
 * log4j logging style. So it should be no problem to use log4j also in
 * JAP.
 */
public abstract class AbstractLog4jLog implements Log
{

	protected abstract Logger getLogger();

	/**
	 * Logs a message to the infoservice logging output.
	 *
	 * @param a_level The level of the message. See logging.LogLevel for more information.
	 *                Because we are using log4j, there are done some transformations:
	 *                LogLevel.DEBUG                        -> Level.DEBUG
	 *                LogLevel.INFO    , LogLevel.NOTICE    -> Level.INFO
	 *                LogLevel.WARNING                      -> Level.WARN
	 *                LogLevel.ERR     , LogLevel.EXCEPTION -> Level.ERROR
	 *                LogLevel.ALERT   , LogLevel.EMERGENCY -> Level.FATAL
	 * @param a_type The type of the message, see logging.LogType. This value is ignored,
	 *               because the infoservice doesn't use typed messaged, so everything is logged.
	 * @param a_message The message to log.
	 */
	public void log(int a_level, int a_type, String a_message)
	{
		/* use debug priority as default, if we don't know the loglevel */
		Level log4jPriority = Level.DEBUG;
		if (a_level == LogLevel.DEBUG)
		{
			log4jPriority = Level.DEBUG;
		}
		else if ( (a_level == LogLevel.INFO) || (a_level == LogLevel.NOTICE))
		{
			log4jPriority = Level.INFO;
		}
		else if (a_level == LogLevel.WARNING)
		{
			log4jPriority = Level.WARN;
		}
		else if ( (a_level == LogLevel.ERR) || (a_level == LogLevel.EXCEPTION))
		{
			log4jPriority = Level.ERROR;
		}
		else if ( (a_level == LogLevel.ALERT) || (a_level == LogLevel.EMERG))
		{
			log4jPriority = Level.FATAL;
		}
		/* log the message */
		getLogger().log(null, log4jPriority, a_message, null);
	}

	/**
	 * This method is needed for the implementation of the Log interface, but isn't supported by
	 * this class. So calls of this method are ignored.
	 *
	 * @a_type The types of the messages to log. This isn't supported by this class.
	 */
	public void setLogType(int a_type)
	{
	}

	/**
	 * This method is needed for the implementation of the Log interface, but isn't supported by
	 * this class. So always LogType.ALL is returned.
	 *
	 * @return LogType.ALL, because the method isn't supported by this class.
	 */
	public int getLogType()
	{
		return LogType.ALL;
	}

	/** Set the log level of this log instance.
	 *                Because we are using log4j, there are done some transformations:
	 *                LogLevel.DEBUG                        -> Level.DEBUG
	 *                LogLevel.INFO    , LogLevel.NOTICE    -> Level.INFO
	 *                LogLevel.WARNING                      -> Level.WARN
	 *                LogLevel.ERR     , LogLevel.EXCEPTION -> Level.ERROR
	 *                LogLevel.ALERT   , LogLevel.EMERGENCY -> Level.FATAL
	 *
	 * @param a_level Changes the log level. Only messages with a equal or higher priority are
	 *                logged.
	 */
	public void setLogLevel(int level)
	{
		Level l = Level.ALL;
		switch (level)
		{
			case LogLevel.DEBUG:
				l = Level.DEBUG;
				break;
			case LogLevel.INFO:
			case LogLevel.NOTICE:
				l = Level.INFO;
				break;
			case LogLevel.ALERT:
			case LogLevel.EMERG:
				l = Level.FATAL;
				break;
			case LogLevel.WARNING:
				l = Level.WARN;
				break;
			case LogLevel.ERR:
			case LogLevel.EXCEPTION:
				l = Level.ERROR;
				break;

		}
		getLogger().setLevel(l);
	}

	/**
	 * The current log level.
	 *
	 * @return The current log level.
	 */
	public int getLogLevel()
	{
		int level = LogLevel.EMERG;

		if (getLogger().isEnabledFor(Level.DEBUG))
		{
			level = LogLevel.DEBUG;
		}
		else if (getLogger().isEnabledFor(Level.INFO))
		{
			level = LogLevel.INFO;
		}
		else if (getLogger().isEnabledFor(Level.WARN))
		{
			level = LogLevel.WARNING;
		}
		else if (getLogger().isEnabledFor(Level.ERROR))
		{
			level = LogLevel.ERR;
		}
		else if (getLogger().isEnabledFor(Level.FATAL))
		{
			level = LogLevel.ALERT;
		}

		return level;
	}

}
