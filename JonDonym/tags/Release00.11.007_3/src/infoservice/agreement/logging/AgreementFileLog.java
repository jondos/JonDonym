/*
 Copyright (c) 2000 - 2006, The JAP-Team
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
package infoservice.agreement.logging;

import infoservice.dynamic.DynamicConfiguration;
import logging.LogLevel;
import logging.LogType;

/**
 * @author ss1
 * 
 */
public class AgreementFileLog implements IAgreementLog
{
    private static final String AGREEMENT_LOG = "agreement.log";

    private AlternateFileLogger m_fileLog;

    // private int m_logLevel = IAgreementLog.LOG_DEBUG;

    /**
     * Creates a new FileLogger
     * 
     */
    public AgreementFileLog()
    {
        m_fileLog = new AlternateFileLogger(AGREEMENT_LOG, 10000000, 2);
        m_fileLog.setLogLevel(DynamicConfiguration.getInstance().getAgreementLogLevel());
        m_fileLog.setLogType(LogType.AGREEMENT);
    }

    /*
     * (non-Javadoc)
     * 
     * @see infoservice.agreement.logging.IAgreementLog#debug(java.lang.String)
     */
    public void debug(String a_msg)
    {
        // if (m_logLevel <= LogLevel.DEBUG)
        m_fileLog.log(LogLevel.DEBUG, LogType.AGREEMENT, a_msg);
    }

    /*
     * (non-Javadoc)
     * 
     * @see infoservice.agreement.logging.IAgreementLog#info(java.lang.String)
     */
    public void info(String a_msg)
    {
        // if (m_logLevel <= LogLevel.INFO)
        m_fileLog.log(LogLevel.INFO, LogType.AGREEMENT, a_msg);
    }

    /*
     * (non-Javadoc)
     * 
     * @see infoservice.agreement.logging.IAgreementLog#error(java.lang.String)
     */
    public void error(String a_msg)
    {
        // if (m_logLevel <= LogLevel.ERR)
        m_fileLog.log(LogLevel.ERR, LogType.AGREEMENT, a_msg);
    }

    /*
     * (non-Javadoc)
     * 
     * @see infoservice.agreement.logging.IAgreementLog#fatal(java.lang.String)
     */
    public void fatal(String a_msg)
    {
        // if (m_logLevel <= LogLevel.EMERG)
        m_fileLog.log(LogLevel.EMERG, LogType.AGREEMENT, a_msg);
    }

    // /* (non-Javadoc)
    // * @see infoservice.agreement.logging.IAgreementLog#setLogLevel(int)
    // */
    // public void setLogLevel(int a_logLevel)
    // {
    // m_logLevel = a_logLevel;
    // }

}
