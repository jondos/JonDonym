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
package infoservice.dynamic;

import infoservice.agreement.IInfoServiceAgreementAdapter;
import infoservice.agreement.common.AgreementConstants;
import infoservice.agreement.multicast.InfoserviceEMCAdapter;

import java.util.Properties;

import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;
import anon.infoservice.Constants;

public class DynamicConfiguration
{
    private long m_emcGlobalTimeout = AgreementConstants.AGREEMENT_TIMEOUT;

    private long m_agreementPhaseGap = AgreementConstants.AGREEMENT_PHASE_GAP;

    private long m_emcConsensusLogTimeout = AgreementConstants.CONSENSUS_LOG_TIMEOUT;

    private long m_paxosRoundTimeout = AgreementConstants.PAXOS_ROUND_TIMEOUT;

    private int m_minCascadeLength = Constants.MIN_CASCADE_LENGTH;

    private int m_maxCascadeLength = Constants.MAX_CASCADE_LENGTH;

    private int m_hourOfAgreement = AgreementConstants.HOUR_OF_AGREEMENT;

    private long m_passivePhaseLength = AgreementConstants.AGREEMENT_PASSIVE_PHASE;

    private IInfoServiceAgreementAdapter m_agreementAdapter;

    private IDynamicCascadeBuildingStrategy m_cascadeBuildingStrategy;

    private int m_agreementLogLevel = LogLevel.DEBUG;

    private static final DynamicConfiguration m_instance = new DynamicConfiguration();

    /**
     * Returns the singleton instance of this class
     * 
     * @return
     */
    public static DynamicConfiguration getInstance()
    {
        return m_instance;
    }

    /**
     * Private constructor -> singleton
     */
    private DynamicConfiguration()
    {
        //
    }

    /**
     * Reads the configuration values out of the given Properties. If one or
     * more are not present, default values will be applied
     * 
     * @param a_properties
     *            The Properties holding the configuration values
     */
    public void readConfiguration(Properties a_properties)
    {
        m_emcGlobalTimeout = parseLong(a_properties.getProperty("emcGlobalTimeout"),
                AgreementConstants.AGREEMENT_TIMEOUT);
        m_passivePhaseLength = parseLong(a_properties.getProperty("passivePhaseLength"),
                AgreementConstants.AGREEMENT_PASSIVE_PHASE);
        m_agreementPhaseGap = parseLong(a_properties.getProperty("agreementPhaseGap"),
                AgreementConstants.AGREEMENT_PHASE_GAP);
        m_emcConsensusLogTimeout = parseLong(a_properties.getProperty("emcConsensusLogTimeout"),
                AgreementConstants.CONSENSUS_LOG_TIMEOUT);
        m_paxosRoundTimeout = parseLong(a_properties.getProperty("paxosRoundTimeout"),
                AgreementConstants.PAXOS_ROUND_TIMEOUT);
        m_minCascadeLength = parseInt(a_properties.getProperty("minCascadeLength"),
                Constants.MIN_CASCADE_LENGTH);
        m_maxCascadeLength = parseInt(a_properties.getProperty("maxCascadeLength"),
                Constants.MAX_CASCADE_LENGTH);
        m_hourOfAgreement = parseInt(a_properties.getProperty("hourOfAgreement"),
                AgreementConstants.HOUR_OF_AGREEMENT);
        String logLevel = a_properties.getProperty("logLevel");
        m_agreementLogLevel = parseLogLevel(logLevel);

        String handler = a_properties.getProperty("agreementMethod");
        if (handler != null)
        {
            try
            {
                m_agreementAdapter = (IInfoServiceAgreementAdapter) Class.forName(handler.trim())
                        .newInstance();
            }
            catch (Exception e)
            {
                LogHolder.log(LogLevel.WARNING, LogType.AGREEMENT,
                        "Unable to instanciate agreement handler of type " + handler
                                + ", using default");
                m_agreementAdapter = new InfoserviceEMCAdapter();
            }
        }
        else
        {
            m_agreementAdapter = new InfoserviceEMCAdapter();
        }

        String strategy = a_properties.getProperty("cascadeBuildingStrategy");

        if (strategy != null)
        {
            try
            {
                m_cascadeBuildingStrategy = (IDynamicCascadeBuildingStrategy) Class.forName(
                        strategy.trim()).newInstance();
            }
            catch (Exception e)
            {
                LogHolder.log(LogLevel.WARNING, LogType.AGREEMENT,
                        "Unable to instanciate cascade building strategy of type " + strategy
                                + ", using default");
                m_cascadeBuildingStrategy = new ComleteRandomStrategy();
            }
        }
        else
        {
            m_cascadeBuildingStrategy = new ComleteRandomStrategy();
        }

    }

    /**
     * Takes a string and returns a LogLevel. The strings may be the typical log
     * levels of log4j
     * 
     * @param logLevel
     *            The string representing a log4j log level
     * @return
     */
    private int parseLogLevel(String logLevel)
    {
        if ("DEBUG".equals(logLevel))
            return LogLevel.DEBUG;
        if ("INFO".equals(logLevel))
            return LogLevel.INFO;
        if ("ERROR".equals(logLevel))
            return LogLevel.ERR;
        if ("FATAL".equals(logLevel))
            return LogLevel.EMERG;
        return LogLevel.DEBUG;
    }

    /**
     * Returns the configured IInfoServiceAgreementAdapter
     * 
     * @return he configured IInfoServiceAgreementAdapter
     */
    public IInfoServiceAgreementAdapter getAgreementHandler()
    {
        return m_agreementAdapter;
    }

    /**
     * Returns the configured phase gap between commitment and reveal phase in
     * the agreement
     * 
     * @return The configured phase gap between commitment and reveal phase in
     *         the agreement
     */
    public long getAgreementPhaseGap()
    {
        return m_agreementPhaseGap;
    }

    /**
     * Returns the configured consensus log timeout for EMC
     * 
     * @return The configured consensus log timeout for EMC
     */
    public long getEmcConsensusLogTimeout()
    {
        return m_emcConsensusLogTimeout;
    }

    /**
     * Returns the configured global timeout for EMC
     * 
     * @return The configured global timeout for EMC
     */
    public long getEmcGlobalTimeout()
    {
        return m_emcGlobalTimeout;
    }

    /**
     * Returns the configured round timeout for paxos
     * 
     * @return The configured round timeout for paxos
     */
    public long getPaxosRoundTimeout()
    {
        return m_paxosRoundTimeout;
    }

    /**
     * Returns the configured max cascade length to be used by the dynamic
     * algorithms
     * 
     * @return he configured max cascade length to be used by the dynamic
     *         algorithms
     */
    public int getMaxCascadeLength()
    {
        return m_maxCascadeLength;
    }

    /**
     * Returns the configured min cascade length to be used by the dynamic
     * algorithms
     * 
     * @return he configured min cascade length to be used by the dynamic
     *         algorithms
     */
    public int getMinCascadeLength()
    {
        return m_minCascadeLength;
    }

    /**
     * Parses the given string to a long value. If the string is no long, the
     * given default value will be used
     * 
     * @param a_str
     *            The string to be parsed
     * @param a_default
     *            The default to be applied if the string is no long
     * @return The parsed long
     */
    private long parseLong(String a_str, long a_default)
    {
        long result = a_default;
        try
        {
            result = Long.parseLong(a_str.trim());
        }
        catch (Exception e)
        {
            //
        }
        return result;
    }

    /**
     * Parses the given string to an int value. If the string is no int, the
     * given default value will be used
     * 
     * @param a_str
     *            The string to be parsed
     * @param a_default
     *            The default to be applied if the string is no int
     * @return The parsed int
     */
    private int parseInt(String a_str, int a_default)
    {
        int result = a_default;
        try
        {
            result = Integer.parseInt(a_str.trim());
        }
        catch (Exception e)
        {
            //
        }
        return result;
    }

    /**
     * Returns the hour in which new cascades should be created, e.g. 5 for 5
     * a.m
     * 
     * @return he hour in which new cascades should be created
     */
    public int getHourOfAgreement()
    {
        return m_hourOfAgreement;
    }

    /**
     * Returns the strategy for building new cascades
     * 
     * @return The strategy for building new cascades
     */
    public IDynamicCascadeBuildingStrategy getCascadeBuildingStrategy()
    {
        return m_cascadeBuildingStrategy;
    }

    /**
     * Returns the length of the passive phase before the actual agreement
     * 
     * @return The length of the passive phase before the actual agreement
     */
    public long getPassivePhaseLength()
    {
        return m_passivePhaseLength;
    }

    /**
     * Returns the log level to be used for the agreement-specific file log
     * 
     * @return The log level to be used for the agreement-specific file log
     */
    public int getAgreementLogLevel()
    {
        return m_agreementLogLevel;
    }

}
