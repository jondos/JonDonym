/*
 Copyright (c) 2000 - 2005, The JAP-Team
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
package infoservice.agreement.paxos;

import infoservice.agreement.logging.IAgreementLog;

public abstract class PaxosObject
{
    private static IAgreementLog m_log = null;

    /**
     * Sets the logging instance. This is a write-once-method!
     * 
     * @param a_log
     *            An IAgreementLog used to do the logging of all Paxos related
     *            instances
     */
    protected static void setLog(IAgreementLog a_log)
    {
        if (m_log == null)
            m_log = a_log;
    }

    /**
     * Returns the IAgreementLog associated to this PaxosObject
     * 
     * @return The IAgreementLog associated to this PaxosObject
     */
    protected IAgreementLog getLog()
    {
        return m_log;
    }

    /**
     * Logs a message at debug level
     * 
     * @param a_msg
     *            The message to log
     */
    public void debug(String a_msg)
    {
        m_log.debug(a_msg);
    }

    /**
     * Logs a message at error level
     * 
     * @param a_msg
     *            The message to log
     */
    public void error(String a_msg)
    {
        m_log.error(a_msg);
    }

    /**
     * Logs a message at fatal level
     * 
     * @param a_msg
     *            The message to log
     */
    public void fatal(String a_msg)
    {
        m_log.fatal(a_msg);
    }

    /**
     * Logs a message at info level
     * 
     * @param a_msg
     *            The message to log
     */
    public void info(String a_msg)
    {
        m_log.info(a_msg);
    }
}
