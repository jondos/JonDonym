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

import infoservice.agreement.common.TimeoutThread;
import infoservice.agreement.logging.GiveThingsAName;
import infoservice.agreement.logging.IAgreementLog;
import infoservice.agreement.multicast.interfaces.IAgreementHandler;
import infoservice.agreement.multicast.interfaces.IConsensusLog;
import infoservice.agreement.multicast.messages.AMessage;
import infoservice.agreement.multicast.messages.CommitMessage;
import infoservice.agreement.multicast.messages.EchoMessage;
import infoservice.agreement.multicast.messages.InitMessage;
import infoservice.agreement.multicast.messages.RejectMessage;
import infoservice.dynamic.DynamicConfiguration;

import java.util.Enumeration;
import java.util.Hashtable;

/**
 * 
 * @author LERNGRUPPE A log entry for <code>AMessage</code>s.
 * 
 */
public class ConsensusLogEchoMulticast implements IConsensusLog
{

    /**
     * Saves the accepted initial message according to this log entry.
     */
    private InitMessage m_initMessage = null;

    /**
     * Holds the accepted <code>EchoMessage</code>s.
     */
    private Hashtable m_echoMessagesHashTable = new Hashtable();

    /**
     * Holds the accepted <code>CommitMessage</code>s.
     */
    private Hashtable m_commitMessagesHashTable = new Hashtable();

    /**
     * Holds the accepted <code>RejectMessage</code>s.
     */
    private Hashtable m_rejectMessagesHashTable = new Hashtable();

    /**
     * A logger.
     */
    private IAgreementLog m_logger = null;

    /**
     * Indicates whether the agreement for this log entry is reached.
     */
    private boolean m_agreed = false;

    /**
     * Indicates whether the a <code>CommitMessage</code> was send for this
     * log entry.
     */
    private boolean m_comitted = false;

    /**
     * Indicates whether the a lifecycle for this log entry is expired.
     */
    private boolean m_timedOut = false;

    /**
     * Holds the timeout thread.
     */
    private TimeoutThread timeoutThread;

    /**
     * An Instance of the agreement handler to get some information about the
     * current infoservice and for notifying agreements.
     * 
     */
    private IAgreementHandler m_handler;

    /**
     * The consensus log entry id.
     */
    private String m_consensusId = "";

    /**
     * Holds a value which determines the lower bound of good infoservices.
     */
    private int m_criticalMasses = Integer.MAX_VALUE;

    /**
     * The id of the infoservice which send the <code>InitMessag</code>.
     */
    private String m_initiatorId = "";

    /**
     * Indicates whether the a <code>RejectMessage</code> was send for this
     * log entry.
     */
    private boolean m_rejected = false;

    /**
     * Indicates whether the a log entry got enough <code>RejectMessages</code>
     * to reset the <code>lastCommonRandom</code> and restart the protocol.
     */
    private boolean m_restarted = false;

    /**
     * The result of the last agreement or an initalized value at startup which
     * is used as round number too.
     */
    private String m_lastCommonRandom = null;

    /**
     * Constructor for creating a log entry.
     * 
     * @param a_handler
     *            The corrosponding agreementhandler.
     * @param a_initMessage
     *            Initmessage. This ist the startmessage of the protocol.
     */
    public ConsensusLogEchoMulticast(EchoMulticastAgreementHandlerImpl a_handler,
            InitMessage a_initMessage)
    {
        this.initializeConsensusLog(a_handler, a_initMessage.getConsensusId(), null, a_initMessage
                .getInitiatorsId());
        this.addInitMessage(a_initMessage);
    }

    /**
     * Constructor for creating a logentry.
     * 
     * @param a_handler
     *            The corrosponding agreementhandler.
     * @param a_consensusId
     *            The id of this consensuslogentry.
     * @param a_log
     *            A filelogger.
     */
    public ConsensusLogEchoMulticast(EchoMulticastAgreementHandlerImpl a_handler,
            String a_consensusId, IAgreementLog a_log, String a_initiatorsId)
    {
        initializeConsensusLog(a_handler, a_consensusId, a_log, a_initiatorsId);
    }

    /**
     * Constructor for creating a logentry.
     * 
     * @param a_handler
     *            The corrosponding agreementhandler.
     * @param a_initMessage
     *            Initmessage. This ist the startmessage of the protocol.
     * @param a_log
     *            A filelogger.
     */
    public ConsensusLogEchoMulticast(EchoMulticastAgreementHandlerImpl a_handler,
            InitMessage a_initMessage, IAgreementLog a_log)
    {
        this.initializeConsensusLog(a_handler, a_initMessage.getConsensusId(), a_log, a_initMessage
                .getInitiatorsId());
    }

    /**
     * Sets up the necessary start variables.
     * 
     * @param a_handler
     * @param a_consensusId
     * @param a_log
     * @param a_initiatorsId
     */
    private void initializeConsensusLog(EchoMulticastAgreementHandlerImpl a_handler,
            String a_consensusId, IAgreementLog a_log, String a_initiatorsId)
    {
        this.m_handler = a_handler;
        this.m_criticalMasses = ((this.m_handler.getInfoService().getNumberOfAllInfoservices() * 2) + 1) / 3;
        this.m_consensusId = a_consensusId;
        this.m_initiatorId = a_initiatorsId;
        this.m_logger = a_log;
        this.timeoutThread = new TimeoutThread(this, DynamicConfiguration.getInstance()
                .getEmcConsensusLogTimeout());
        this.timeoutThread.start();

    }

    /**
     * Check the properties encapsulated in the <code>AMessage</code> super
     * class.
     * 
     * @param a_msg
     *            The message.
     * @return <code>true</code> if successful.
     */
    private boolean checkGenericMessage(AMessage a_msg)
    {
        if (!a_msg.isSignatureOK())
        {
            debug("checkGeneric: " + AgreementMessageTypes.getTypeAsString(a_msg.getMessageType())
                    + ":Invalid signature");
            return false;
        }

        // consusID-check
        if (!this.getConsensusID().equals(a_msg.getConsensusId()))
        {
            debug("checkGeneric: " + AgreementMessageTypes.getTypeAsString(a_msg.getMessageType())
                    + ": Wrong consensus id" + "\n" + this.getConsensusID() + "\n"
                    + a_msg.getConsensusId());
            return false;
        }
        // Timeout check
        if (this.m_timedOut)
        {
            debug("checkGeneric: " + AgreementMessageTypes.getTypeAsString(a_msg.getMessageType())
                    + ": Timed out!!!  LogId: " + this.getConsensusID());
            return false;
        }
        return true;
    }

    /**
     * If <code>checkInitMessage(a_msg)</code> was successful the messge will
     * be added to the consensuslog, otherwise discarded.
     * 
     * @param a_msg
     *            The message.
     */
    public boolean addInitMessage(InitMessage a_msg)
    {
        if (!checkInitMessage(a_msg))
            return false;
        this.m_initMessage = a_msg;
        this.m_initiatorId = a_msg.getInitiatorsId();
        String initiatorsName = GiveThingsAName.getNameForNumber(a_msg.getInitiatorsId());
        debug(" <---- ADD InitMessage FROM " + initiatorsName + " " + a_msg);
        return true;
    }

    /**
     * Checks the given InitMessage AND sets the ConsensusId.
     * 
     * @param a_msg
     * @return true for success otherwise false
     */
    private boolean checkInitMessage(InitMessage a_msg)
    {
        if (this.m_initMessage != null)
        {
            debug("OVERWRITE ERROR: addInitMessage(): " + a_msg);
            return false;
        }

        return checkGenericMessage(a_msg);

    }

    /**
     * If <code>checkEchoMessage(a_msg)</code> is successful the messge will
     * be added to the consensuslog, otherwise discarded.
     * 
     * @param a_msg
     *            The message.
     */
    public boolean addEchoMessage(EchoMessage a_msg)
    {
        if (!checkEchoMessage(a_msg))
            return false;

        this.m_echoMessagesHashTable.put(a_msg.getHashKey(), a_msg);
        String sendersName = GiveThingsAName.getNameForNumber(a_msg.getSenderId());
        debug(" <---- ADD EchoMessage FROM " + sendersName + " " + a_msg);
        return true;
    }

    /**
     * Checks the given EchoMessage.
     * 
     * @param a_msg
     * @return true for success otherwise false
     */
    private boolean checkEchoMessage(EchoMessage a_msg)
    {
        if (!checkGenericMessage(a_msg))
            return false;

        // Duplicate Check
        if (this.m_echoMessagesHashTable.containsKey(a_msg.getHashKey()))
        {
            debug(" ----- DISCARD DUPLICATE EchoMessage FROM " + a_msg.getSenderId() + " " + a_msg);
            return false;
        }
        return true;
    }

    /**
     * If <code>checkCommitMessage(a_msg)</code> is successful the messge will
     * be added to the consensuslog, otherwise discarded.
     * 
     * @param a_msg
     *            The message.
     */
    public boolean addCommitMessage(CommitMessage a_msg)
    {
        if (!checkCommitMessage(a_msg))
            return false;
        this.setAgreed(true);

        this.m_commitMessagesHashTable.put(a_msg.getHashKey(), a_msg);
        String sendersName = GiveThingsAName.getNameForNumber(a_msg.getSenderId());
        debug(" <---- ADD CommitMessage FROM " + sendersName + " " + a_msg);
        return true;
    }

    /**
     * Checks the given CommitMessage.
     * 
     * @param a_msg
     * @return true for success otherwise false
     */
    private boolean checkCommitMessage(CommitMessage a_msg)
    {
        if (!checkGenericMessage(a_msg))
            return false;

        // Duplicate Check
        if (this.m_commitMessagesHashTable.containsKey(a_msg.getHashKey()))
        {
            debug("addCommitMessage(): Duplicate :: " + a_msg);
            return false;
        }
        // check whether all EchoMessages are unique and guilty
        Hashtable hashtable = new Hashtable(a_msg.getEchoMessages().size());
        Enumeration enEcho = a_msg.getEchoMessages().elements();
        while (enEcho.hasMoreElements())
        {
            EchoMessage echo = (EchoMessage) enEcho.nextElement();
            if (!echo.isSignatureOK())
            {
                return false;
            }
            if (!hashtable.containsKey(echo.getHashKey()))
            {
                hashtable.put(echo.getHashKey(), echo);
            }
        }
        a_msg.setEchoMessages(hashtable);

        // Check whether the amount of EchoMessages holds the crtitical amount
        if (a_msg.getEchoMessages().size() < this.m_criticalMasses)
        {
            debug("addCommitMessage(): CriticalMass (" + this.m_criticalMasses
                    + ") can't be achieved by " + a_msg.getEchoMessages().size()
                    + " EchoMessages:: \n" + a_msg);
            return false;
        }
        return true;
    }

    /**
     * If <code>checkRejectMessage(a_msg)</code> is successful the messge will
     * be added to the consensuslog, otherwise discarded.
     * 
     * @param a_msg
     *            The message.
     */
    public void addRejectMessage(RejectMessage a_msg)
    {
        if (!checkRejectMessage(a_msg))
            return;

        this.m_rejectMessagesHashTable.put(a_msg.getHashKey(), a_msg);
        String sendersName = GiveThingsAName.getNameForNumber(a_msg.getSenderId());
        debug(" <---- ADD RejectMessage FROM " + sendersName + " " + a_msg);
    }

    /**
     * Check the given reject-message and, if positive, extract and save the new
     * value of <code>lastCommonRandom</code>
     * 
     * @param a_msg
     *            The message.
     * @return True if the check was successful.
     */
    private boolean checkRejectMessage(RejectMessage a_msg)
    {
        if (!checkGenericMessage(a_msg))
            return false;

        // Duplicate Check
        if (this.m_rejectMessagesHashTable.containsKey(a_msg.getHashKey()))
        {
            debug("addRejectMessage(): Duplicate :: " + a_msg);
            return false;
        }

        // Check whether the amount of RejectMessages holds the crtitical amount
        if (this.m_rejectMessagesHashTable.size() + 1 > this.m_criticalMasses - 1)
        {
            this.m_rejected = true;
            checkRejectMajority(a_msg);
        }

        return true;
    }

    /**
     * Count the number of received <code>RejectMessage</code> and sets this
     * value to <code>sm_lastCommonRandom</code> if the amount reachs at least
     * <code>m_criticalMasses</code>.
     * 
     */
    private void checkRejectMajority(RejectMessage a_msg)
    {
        Enumeration en = m_rejectMessagesHashTable.elements();
        Hashtable amounts = new Hashtable();
        while (en.hasMoreElements())
        {
            RejectMessage msg = (RejectMessage) en.nextElement();
            putter(amounts, msg);
        }
        putter(amounts, a_msg);

        Enumeration en2 = amounts.keys();
        while (en2.hasMoreElements())
        {
            String prop = (String) en2.nextElement();
            int count = ((Integer) amounts.get(prop)).intValue();
            if (count >= this.m_criticalMasses)
            {
                this.m_lastCommonRandom = prop;
                debug(" ----> RESET lastCommonRandom TO " + m_lastCommonRandom);
                return;
            }
        }
    }

    /**
     * Counts the received <code>RejectMessages</code>.
     * 
     * @param amounts
     *            A hashtable which contains the various
     *            <code>RejectMessage</code>s.
     * @param msg
     *            The message.
     */
    private void putter(Hashtable amounts, RejectMessage msg)
    {
        String prop = msg.getLastCommonRandom();
        Integer count = (Integer) amounts.remove(prop);
        if (count == null)
        {
            count = new Integer(0);
        }
        count = new Integer(count.intValue() + 1);
        amounts.put(prop, count);
    }

    /**
     * Check the criteria for creating a CommitMessage an do it if possible.
     * 
     * @return the created CommitMessage or null.
     */
    public CommitMessage tryToCreateACommitMessage()
    {
        debug("tryToCreateACommitMessage() Got " + this.getEchoMessageCount() + " Need "
                + m_criticalMasses);
        CommitMessage commit = null;
        if (this.getEchoMessageCount() >= m_criticalMasses)
        {
            InitMessage mesg = this.getInitMessages();
            if (mesg == null)
            {
                debug("UHHHHHH MY INIT MESSAGE IS NULL!!");
                return null;
            }
            commit = new CommitMessage(this.getInitMessages(), this.m_handler.getInfoService()
                    .getIdentifier(), this.m_echoMessagesHashTable);
        }

        return commit;
    }

    /**
     * Count the <code>EchoMessage</code>s.
     * 
     * @return Number of <code>EchoMessage</code>s.
     */
    public int getEchoMessageCount()
    {
        return this.m_echoMessagesHashTable.keySet().size();
    }

    /**
     * Get the hashtable which holds the accepted <code>CommitMessage</code>s.
     * 
     * @return The hashtable.
     */
    public int getCommitMessageCount()
    {
        return this.m_commitMessagesHashTable.size();
    }

    /**
     * Indicates whether the agreement for this log entry is reached.
     */
    public synchronized boolean isAgreed()
    {
        return m_agreed;
    }

    /**
     * Indicates whether the agreement for this log entry is reached.
     */
    public synchronized void setAgreed(boolean agreed)
    {
        this.m_agreed = agreed;
        stopTimeout();
    }

    /**
     * Indicates whether the a <code>CommitMessage</code> was send for this
     * log entry.
     * 
     * @return <code>true</code> if so, otherwise <code>false</code>.
     */
    public boolean isComitted()
    {
        return m_comitted;
    }

    /**
     * Set a value which indicates whether the a <code>CommitMessage</code>
     * was send for this log entry.
     */
    public void setComitted(boolean comitted)
    {
        this.m_comitted = comitted;
    }

    /**
     * Stops the consensus log time out.
     */
    public void stopTimeout()
    {
        this.timeoutThread.cancel();
    }

    /**
     * Gets the consensus log entry id.
     * 
     * @return The id.
     */
    public String getConsensusID()
    {
        return this.m_consensusId;
    }

    /**
     * Gets the accepted initial message according to this log entry.
     * 
     * @return The initial message.
     */
    public InitMessage getInitMessages()
    {
        return m_initMessage;
    }

    /**
     * Gets the id of the infoservice which send the <code>InitMessag</code>.
     * 
     * @return The id.
     */
    public String getInitiatorId()
    {
        return m_initiatorId;
    }

    /**
     * Indicates whether the a <code>RejectMessage</code> was send for this
     * log entry.
     * 
     * @return <code>true</code> if so, otherwise <code>false</code>.
     */
    public boolean isRejected()
    {
        return m_rejected;
    }

    /**
     * Gets the result of the last agreement or an initalized value at start up
     * which is used as round number too.
     * 
     * @return The last common random.
     */
    public String getLastCommonRandom()
    {
        return m_lastCommonRandom;
    }

    /**
     * Indicates whether the a log entry got enough <code>RejectMessages</code>
     * to reset the <code>lastCommonRandom</code> and restart the protocol.
     * 
     * @return <code>true</code> if so, otherwise <code>false</code>.
     */
    public boolean isRestarted()
    {
        return this.m_restarted;
    }

    /**
     * Sets a value which indicates whether the a log entry got enough
     * <code>RejectMessages</code> to reset the <code>lastCommonRandom</code>
     * and restart the protocol.
     * 
     * @param a_restarted
     */
    public void setRestarted(boolean a_restarted)
    {
        this.m_restarted = a_restarted;

    }

    private void debug(String a_message)
    {
        this.m_logger.debug(a_message);
    }

    /**
     * On creating a consensuslog a timer will be set. If the timer expired, the
     * log entry will be closed and all stored data will be evaluated.
     */
    public void timeout(Object a_value)
    {
        this.m_timedOut = true;
        this.m_handler.notifyConsensusLogTimeout(this);

        debug(" TIMEOUT " + this.m_initMessage);
    }
}
