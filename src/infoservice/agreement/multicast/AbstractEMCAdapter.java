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
package infoservice.agreement.multicast;

import infoservice.agreement.common.AgreementConstants;
import infoservice.agreement.logging.IAgreementLog;
import infoservice.agreement.multicast.interfaces.IAgreementHandler;
import infoservice.agreement.multicast.interfaces.IAgreementMessage;
import infoservice.agreement.multicast.interfaces.IInfoService;
import infoservice.dynamic.DynamicConfiguration;

import java.util.Calendar;
import java.util.TimeZone;

public abstract class AbstractEMCAdapter implements IInfoService
{

    /**
     * LERNGRUPPE The agreement handler. He encapsuletes all the logic for
     * getting an common random number.
     */
    protected IAgreementHandler m_agreementHandler;

    /**
     * LERNGRUPPE A boolean value, which indicates whether an agreement is
     * started yet.
     */
    protected boolean m_activeAgreementStarted = false;

    /**
     * LERNGRUPPE A boolean value, which indicates whether the handler is in
     * stop mode. This means that no incomming message will be handled.
     */
    private boolean doNotHandleAnything = true;

    /**
     * LERNGRUPPE A boolean value, which indicates whether the handler have to
     * restart the protocol. This is for security reasons. In fact, we set this
     * variable if the used last common random is equals to the default value.
     */
    private boolean m_mustBeRestartedForSecurityReason = true;

    /**
     * LERNGRUPPE A logger.
     */
    protected IAgreementLog m_log;

    /**
     * LERNGRUPPE A boolean value, which indicates whether an agreement is in
     * passive mode yet.
     */
    private boolean m_beInPassiveMode;

    /**
     * LERNGRUPPE The constructor sets only the logger. To start the adapter
     * call
     * <code>setIAgreementHandler(IAgreementHandler p_agreementhandler)</code>
     * and at next <code>startAdapter()</code>-method. We have to do that
     * explicitly an NOT in the constructor. So we can take care that all
     * infoservices know about each other befor starting the protocol.
     *
     * @param a_log
     *            The logger to use.
     */
    public AbstractEMCAdapter(IAgreementLog a_log)
    {
        this.m_log = a_log;
    }

    /**
     * Sets the given AgreementHandler. You have to do that BEFORE you call
     * <code>startAdapter()</code>-method.
     *
     * @param p_agreementhandler
     */
    public void setIAgreementHandler(IAgreementHandler p_agreementhandler)
    {
        this.m_agreementHandler = p_agreementhandler;
        m_agreementHandler.setLog(m_log);
    }

    /**
     * LERNGRUPPE Call this method to start the adapter. He will poll the
     * <code>tryToStartAgreement()</code>-method within an endless loop.
     *
     * Take care that
     * <code>setIAgreementHandler(IAgreementHandler p_agreementhandler)</code>
     * was called before!!!
     *
     */
    public void startAdapter()
    {
        info("AgreementAdapter has been started ...");
        reset();
        new Thread("AbstractEMCAdapter - startAdapter()")
        {
            public void run()
            {

                try
                {
                    while (true)
                    {
                        Thread.sleep(AgreementConstants.TIME_WATCH_POLLING_INTERVAL);
                        tryToStartAgreementMinute();
                        /** @fixme change in production */
                        // tryToStartAgreementHour();
                    }
                }
                catch (InterruptedException e)
                {
                    e.printStackTrace();
                }
            }
        }.start();
    }

    /**
     * LERNGRUPPE Watch at the clock and start agreement at the right time.
     *
     */
    protected void tryToStartAgreementMinute()
    {

        Calendar c = Calendar.getInstance(TimeZone.getTimeZone("CET"));
        int hour = DynamicConfiguration.getInstance().getHourOfAgreement();
        // debug("Stunde: "+ c.get(Calendar.HOUR_OF_DAY)+" Minute:"+
        // +c.get(Calendar.MINUTE)+" hour: " + hour);
        /* Try to start the passive part */
        int passive = (int) ((DynamicConfiguration.getInstance().getPassivePhaseLength() / 1000) / 60);
        if (c.get(Calendar.MINUTE) % hour >= hour - passive
                && this.m_activeAgreementStarted == false && this.m_beInPassiveMode == false)
        {
            startListeningMode();
            return;
        }
        /* Try to start the active part */
        if (c.get(Calendar.MINUTE) % hour == 0 && this.m_activeAgreementStarted == false
                && m_beInPassiveMode == true)
        {
            startAgreementCommitmentProtocol();
            return;
        }
    }

    /**
     * LERNGRUPPE Watch at the clock and start agreement at the right time.
     *
     */
    protected void tryToStartAgreementHour()
    {

        Calendar c = Calendar.getInstance(TimeZone.getTimeZone("CET"));
        int hour = DynamicConfiguration.getInstance().getHourOfAgreement();
        // debug("Stunde: "+ c.get(Calendar.HOUR_OF_DAY)+" Minute:
        // "+c.get(Calendar.MINUTE)+" hour: " + hour);
        int passive = (int) ((DynamicConfiguration.getInstance().getPassivePhaseLength() / 1000) / 60);
        /* Try to start the passive part */
        if (c.get(Calendar.HOUR_OF_DAY) == hour - 1 && c.get(Calendar.MINUTE) >= 60 - passive
                && this.m_activeAgreementStarted == false && this.m_beInPassiveMode == false)
        {
            startListeningMode();
            return;
        }
        /* Try to start the active part */
        if (c.get(Calendar.HOUR_OF_DAY) == hour && c.get(Calendar.MINUTE) == 0
                && this.m_activeAgreementStarted == false && m_beInPassiveMode == true)
        {
            startAgreementCommitmentProtocol();
            return;
        }
    }

    /**
     * LERNGRUPPE Handle incomming messages. And start agreement by yourself if
     * not done so.
     *
     * @param The
     *            message.
     */
    public void handleMessage(IAgreementMessage a_msg)
    {
        if (doNotHandleAnything)
        {
            info("CAN'T HANDLE MASSAGES FOR BEEING IN STOPP-MODE.\n");
            return;
        }
        if (this.getNumberOfAllInfoservices() < 3)
        {
            info("CAN'T HANDLE MASSAGES. NUMBER OF INFOSERVICES IS TO SMALL: "
                    + this.getNumberOfAllInfoservices() + "\n");
            return;
        }
        this.startAgreementCommitmentProtocol();
        this.m_agreementHandler.handleMessage(a_msg);
    }

    /**
     * LERNGRUPPE This gets called when the AgreementHandler has reached
     * angreement with the others. When this happens it is time to wire new
     * cascades :-)
     *
     * @param a_newCommonRandomSeed
     *            the agreed-upon random seed
     */
    public void notifyAgreement(Long a_oldCommonRandomSeed, Long a_newCommonRandomSeed)
    {
        /* reset some variables */
        reset();

        /*
         * If the old LCR is equals to the default value, we have to restart the
         * agreement for security reasons.
         */
        if (a_oldCommonRandomSeed.equals(new Long(Long
                .parseLong(AgreementConstants.DEFAULT_COMMON_RANDOM))))
        {
            this.m_mustBeRestartedForSecurityReason = true;
        }
        else
        {
            this.m_mustBeRestartedForSecurityReason = false;
        }

        if (a_newCommonRandomSeed != null)
        {
            info("\n*********************************\nAGREEMENT REACHED: " + a_newCommonRandomSeed
                    + "\n*********************************\n");

            if (this.m_mustBeRestartedForSecurityReason == false)
            {
                this.buildCascades(a_newCommonRandomSeed.longValue());
            }
            else
            {
                info("\n*********************************\n"
                        + "ATTENTION! THE USED ROUNDNUMBER WAS THE DEFAULT ONE.  "
                        + "WE HAVE TO RESTART THE PROTOCOL FOR SECURITY REASONS!!!"
                        + "\n*********************************\n");
                restartAgreement();
                return;
            }

        }
        else
        {
            info("\n*********************************\n" + "NO AGREEMENT WAS REACHED, ABORTING"
                    + "\n*********************************\n");
        }
        info("I will be inactive until the next passive phase will be started ...\n");
    }

    /**
     * LERNGRUPPE Some times a restart of the protocol is necessary. This method
     * calls <code>startAgreementCommitmentProtocol()</code> after waiting.
     *
     */
    private void restartAgreement()
    {
        startListeningMode();
        new Thread()
        {
            public void run()
            {

                try
                {
                    Thread.sleep(DynamicConfiguration.getInstance().getEmcGlobalTimeout());
                    startAgreementCommitmentProtocol();
                }
                catch (InterruptedException e)
                {
                    error("Unable to sleep in thread!");
                }
            }
        }.start();

    }

    /**
     * LERNGRUPPE To start the protocoll we need two steps. At first we will
     * listen for incomming messages. After spending some time doing that we
     * start with our own random value. This method starts step one. The
     * listening mode.
     *
     */
    public synchronized void startListeningMode()
    {
        // info("StartListeningMode ....");
        this.doNotHandleAnything = false;
        this.m_activeAgreementStarted = false;
        this.m_beInPassiveMode = true;
        this.m_agreementHandler.reset();
        this.startListening();
        info(" START PASSIVE MODE ---> Now we are ready to start the agreement. Listening for incomming messages ...\n ");
    }

    /**
     * LERNGRUPPE Sometimes for testing reasons only a operator have to start
     * the protocol. This method do that an should never called in real life.
     *
     */
    public void startProtocolByOperator()
    {
        if (this.m_beInPassiveMode)
        {
            info(" STATUS PASSIVE MODE. AGREEMNET WILL BE STARTED SHORTLY BY TIMETRIGGER.\n");
            return;
        }
        if (this.m_activeAgreementStarted)
        {
            info(" CAN'T START AGREEMENT BY OPERATOR. AGREEMENT IS JUST RUNNIG!!!\n");
            return;
        }

        info(" TRY TO START AGREEMENT BY OPERATOR ...\n");
        this.restartAgreement();
    }

    /**
     * LERNGRUPPE Starts the protocol after checking some status values for
     * security reasons. It delegates the call to the agreement handler.
     *
     */
    protected synchronized void startAgreementCommitmentProtocol()
    {

        if (this.doNotHandleAnything)
        {
            info(" CAN'T START AGREEMENT FOR BEEING IN STOPP-MODE. TRY IT AGAIN LATER.\n");
            return;
        }

        if (this.m_activeAgreementStarted)
            return;

        if (m_beInPassiveMode == false)
        {
            info(" CAN'T START AGREEMENT ONLY IN PASSIVE-MODE.\n");
            reset();
            return;
        }

        /* Freeze status of Infoservices an start the messagehandler thread. */
        try
        {
            this.prepareStart();
        }
        catch (NullPointerException e)
        {
            fatal(e.getMessage());
            reset();
            return;
        }

        if (this.getNumberOfAllInfoservices() < 3)
        {
            info(" CAN'T START AGREEMENT FOR: Number of all available Infoservices ("
                    + this.getNumberOfAllInfoservices() + ") are less than 3.\n");
            reset();
            return;
        }

        info(" START AGREEMENT NOW ");

        this.m_activeAgreementStarted = true;
        this.m_beInPassiveMode = false;
        this.m_agreementHandler.startAgreementCommitmentProtocol();

    }

    /**
     * Resets the state variables.
     *
     */
    private void reset()
    {
        info(" INTERNAL RESET  ");
        this.doNotHandleAnything = true;
        this.m_beInPassiveMode = false;
        this.m_activeAgreementStarted = false;
    }

    /**
     * Start to build the new cascades.
     *
     * @param l
     *            A long value which represents the common random number.
     */
    protected abstract void buildCascades(long l);

    /**
     * Initialize the message queue handler thread. At this point in time
     * incomming messages will be queued but not yet handled.
     *
     */
    protected abstract void startListening();

    /**
     * This method is called in <code>startAgreementCommitmentProtocol()</code>
     * befor the call will be delegated.
     *
     */
    protected abstract void prepareStart() throws NullPointerException;

    /**
     * Sends the given message to the InfoService with the given ID
     *
     * @param a_id
     *            The ID of the InfoService
     * @param a_echoMessage
     *            The message
     */
    public abstract void sendMessageTo(final String a_id, final IAgreementMessage a_message);

    /**
     * Multicasts the given message to all known InfoServices (except this one)
     *
     * @param a_message
     */
    public abstract void multicastMessage(final IAgreementMessage a_message);

    /**
     * Returns an unique identifier for this <code>IInfoService</code>
     *
     * @return An unique identifier for this <code>IInfoService</code>
     */
    public abstract String getIdentifier();

    /**
     * Returns the number of known InfoServices
     *
     * @return The number of known InfoServices
     */
    public abstract int getNumberOfAllInfoservices();

    /**
     * Set the Logger.
     *
     * @param a_log
     *            A logger.
     */
    public void setLog(IAgreementLog a_log)
    {
        this.m_log = a_log;
    }

    /**
     * Gets the current logger.
     *
     * @return
     */
    public IAgreementLog getLog()
    {
        return m_log;
    }

    /**
     * Loggs a debug message.
     *
     * @param a_msg
     *            A Message.
     */
    protected void debug(String a_msg)
    {
        m_log.debug(a_msg);
    }

    /**
     * Loggs a info message.
     *
     * @param a_msg
     *            A Message.
     */
    protected void info(String a_msg)
    {
        m_log.info(a_msg);
    }

    /**
     * Loggs a error message.
     *
     * @param a_msg
     *            A Message.
     */
    protected void error(String a_msg)
    {
        m_log.error(a_msg);
    }

    /**
     * Loggs a fatal message.
     *
     * @param a_msg
     *            A Message.
     */
    protected void fatal(String a_msg)
    {
        m_log.fatal(a_msg);
    }

}
