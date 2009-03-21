package infoservice.agreement.logging;

import java.io.IOException;

import logging.AbstractLog4jLog;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.RollingFileAppender;

public class AlternateFileLogger extends AbstractLog4jLog
{
	private Logger m_Log;
    public AlternateFileLogger(String fileName, int maxFileSize, int maxBackups)
    {
        super();
        m_Log = Logger.getLogger(fileName);
        PatternLayout layout = new PatternLayout("[%d{ISO8601} - %p] %m%n");
        try
        {
            m_Log.setLevel(Level.ALL);
            RollingFileAppender appender = new RollingFileAppender(layout, fileName, true);
            appender.setMaximumFileSize(maxFileSize);
            appender.setMaxBackupIndex(maxBackups);
            appender.setBufferedIO(false);
            appender.activateOptions();
            m_Log.removeAllAppenders();
            m_Log.addAppender(appender);
        }
        catch (IOException ex)
        {
            //
        }
    }
    protected Logger getLogger()
	{
		return m_Log;
	}

}
