package logging;

public class SystemErrLog implements Log
{
	private int m_logLevel;
	private int m_logType;

	public SystemErrLog()
	{
		this(LogLevel.DEBUG, LogType.ALL);
	}

	public SystemErrLog(int a_logLevel, int a_logType)
	{
		m_logLevel = a_logLevel;
		m_logType = a_logType;
	}

	public void log(int a_logLevel, int a_logType, String msg)
	{
		if ( (a_logLevel <= m_logLevel) && ( (a_logType & m_logType) == a_logType))
		{
			System.err.println("[" + LogLevel.STR_Levels[a_logLevel] + "] " + msg);
		}
	}

	public void setLogLevel(int a_logLevel)
	{
		if (a_logLevel >= 0 && a_logLevel < LogLevel.STR_Levels.length)
		{
			m_logLevel = a_logLevel;
		}
	}

	public void setLogType(int a_logType)
	{
		m_logType = a_logType;
	}

	/** Get the current debug type.
	 */
	public int getLogType()
	{
		return m_logType;
	}

	/** Get the current debug level.
	 */
	public int getLogLevel()
	{
		return m_logLevel;
	}

}
