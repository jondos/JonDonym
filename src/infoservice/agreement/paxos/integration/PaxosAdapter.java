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
package infoservice.agreement.paxos.integration;

import infoservice.agreement.common.AgreementConstants;
import infoservice.agreement.paxos.IPaxosTarget;
import infoservice.agreement.paxos.PaxosAcceptor;
import infoservice.agreement.paxos.PaxosExecution;
import infoservice.agreement.paxos.PaxosInstance;
import infoservice.agreement.paxos.messages.PaxosMessage;
import infoservice.dynamic.DynamicConfiguration;

import java.security.SecureRandom;
import java.util.Calendar;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.TimeZone;
import java.util.Vector;

import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;
import anon.util.Util;

public abstract class PaxosAdapter extends PaxosAcceptor
{
    private static final int STATE_INACTIVE = 0;

    private static final int STATE_PASSIVE = 1;

    private static final int STATE_COMMITMENT = 2;

    private static final int STATE_PHASE_GAP = 3;

    private static final int STATE_REVEAL = 4;

    protected Hashtable m_commitments;

    protected Commitment m_ownCommitment;

    protected Vector m_passivelyReceivedMessages = new Vector();

    protected Vector m_potentialyCorrectMessage = new Vector();

    protected abstract void prepareTargets();

    AgreementStarterThread m_starterThread;

    protected int m_currentState = STATE_INACTIVE;

    /**
     * Creates a new PaxosAdapter and starts the AgreementStarterThread for
     * starting agreement at a specific time
     */
    protected PaxosAdapter()
    {
        m_starterThread = new AgreementStarterThread();
        m_starterThread.start();
    }

    /**
     * Manually start an agreement
     *
     * @fixme remove me in production!
     */
    public void startProtocolByOperator()
    {
        // If an agreement is running, ignore the call
        if (m_currentState > STATE_PASSIVE)
        {
            info("Won't start agreement, there is one running atm");
            return;
        }

        // if we are passive right now, ignore the call, an agreement will start
        // automatically within short time
        if (m_currentState == STATE_PASSIVE)
        {
            info("Won't start agreement, I am already passive and an agreement will start soon");
            return;
        }

        // If we are inactive, we start the passive phase immediatly and stop
        // the starter thread
        if (m_currentState == STATE_INACTIVE)
        {
            if (m_starterThread != null)
            {
                m_starterThread.cancel();
            }
            info("Initializing agreement");
            startPassivePhase();
        }
    }

    /**
     * Starts the protocol by proposing a value to the others
     */
    public void startAgreement()
    {
        info("Starting Agreement");
        m_currentState = STATE_COMMITMENT;
        prepareTargets();
        startRound();

        String proposal = Long.toString((new SecureRandom()).nextLong());
        m_ownCommitment = CommitmentScheme.createCommitment(proposal);
        propose(m_ownCommitment.getCommitmentAsString());

        // Handle the messages which arrived during the passive phase
        synchronized (m_passivelyReceivedMessages)
        {
            info("Will now handle " + m_passivelyReceivedMessages.size()
                    + " passively received messages");
            Enumeration en = m_passivelyReceivedMessages.elements();
            int i = 1;
            while (en.hasMoreElements())
            {

                PaxosMessage msg = (PaxosMessage) en.nextElement();
                debug("Now handling passively received " + msg.getMessageType() + "-message from "
                        + msg.getSender() + " (" + i + ")");
                handleIncommingMessage(msg);
                i++;
            }
            m_passivelyReceivedMessages.clear();
        }
    }

    /**
     * Starts a PaxosExection for all known IPaxosTargets so that timeouts are
     * synchronized
     */
    public void startRound()
    {
        InfoServicePaxosInstance instance = new InfoServicePaxosInstance(this, m_lastRandom);
        Vector roundLeaders = getRoundLeaders();
        Enumeration en2 = roundLeaders.elements();
        while (en2.hasMoreElements())
        {
            String leader = (String) en2.nextElement();
            IPaxosTarget tmp = (IPaxosTarget) m_targets.get(leader);
            PaxosExecution result = new PaxosExecution(instance, tmp.getId(), roundLeaders);
            // result.setInitiator(tmp.getId());
            instance.getExecutions().put(tmp.getId(), result);

        }
        m_paxosInstances.put(m_lastRandom, instance);
    }

    /**
     * Tests if the PaxosInstance-identifier in this messages is acceptable,
     * i.e. is the same as our own
     *
     * @param a_msg
     *            The message to be tested
     * @return true if the identifier is ok, false otherwise
     */
    protected boolean roundNrAcceptable(PaxosMessage a_msg)
    {
        String r1 = Util.replaceAll(m_lastRandom,"--r", "");
        String r2 = r1 + "--r";
        return (a_msg.getPaxosInstanceIdentifier().equals(r1) || a_msg.getPaxosInstanceIdentifier()
                .equals(r2));
    }

    /**
     * Tests if the sender of this message is in the snapshot for the current
     * PaxosInstance.
     *
     * @param a_msg
     *            The message whose sender should be checked
     * @return true if the sender is not in the snapshot, false otherwise
     */
    protected boolean senderNotInSnapshot(PaxosMessage a_msg)
    {
        return (!m_targets.containsKey(a_msg.getSender()));
    }

    /**
     * Sends a RejectMessage to the sender of the given message. This happens if
     * the PaxosInstance-identifier of the given message doesn't match our own
     * one
     *
     * @param a_msg
     *            The message whose sender will receive a RejectMessage
     */
    protected void sendReject(PaxosMessage a_msg)
    {
        // You shalt not reject yourself!
        if (a_msg.getSender().equals(getIdentifier()))
            return;
        PaxosMessage reject = new PaxosMessage(PaxosMessage.REJECT);
        reject.setInitiator(a_msg.getInitiator());
        reject.setSender(getIdentifier());
        reject.setProposal(Util.replaceAll(m_lastRandom,"--r", ""));
        reject.setPaxosInstanceIdentifier(a_msg.getPaxosInstanceIdentifier());
        reject.setRound(a_msg.getRound());
        IPaxosTarget tmp = (IPaxosTarget) m_targets.get(a_msg.getSender());
        sendMessage(reject, tmp);
    }

    /**
     * Handles an incomming message. A new agreement is startet if none is
     * running and the PaxosIdenifier of the incomming message is correct.
     * otherwise a RejectMessage will be sent
     *
     * @param a_msg
     *            The message to be handled
     * @return
     */
    public void handleIncommingMessage(final PaxosMessage a_msg)
    {
        // We don't handle messages when we are in inactive state
        if (m_currentState == STATE_INACTIVE)
        {
            debug("Inactive! Not handling the message");
            return;
        }

        // If we are in passive state we put the messages on hold until we
        // start the commitment phase. Then the messages will be handled again
        if (m_currentState == STATE_PASSIVE || m_currentState == STATE_PHASE_GAP)
        {
            synchronized (m_passivelyReceivedMessages)
            {
                m_passivelyReceivedMessages.add(a_msg);
            }
            debug("Passive or PhaseGap! Queueing a " + a_msg.getMessageType() + "-message from "
                    + a_msg.getSender() + ", have now " + m_passivelyReceivedMessages.size());

            return;
        }

        if (a_msg.getMessageType().equals(PaxosMessage.REJECT))
        {
            InfoServicePaxosInstance inst = (InfoServicePaxosInstance) m_paxosInstances.get(a_msg
                    .getPaxosInstanceIdentifier());
            inst.handleRejectMessage(a_msg);
            return;
        }

        if (senderNotInSnapshot(a_msg))
        {
            debug("Sender not in Snapshop: " + a_msg.getSender());
            // This might cause problems, somehow an inconsistent view of
            // infoservices occured
            return;
        }

        if (!roundNrAcceptable(a_msg))
        {
            if (m_currentState == STATE_COMMITMENT
                    && a_msg.getMessageType().equals(PaxosMessage.PROPOSE))
                sendReject(a_msg);
            debug("Round number not acceptable: " + a_msg.getPaxosInstanceIdentifier());
            synchronized (m_potentialyCorrectMessage)
            {
                m_potentialyCorrectMessage.add(a_msg);
            }
            return;
        }
        new Thread()
        {
            public void run()
            {
                handleMessage(a_msg);
            }

        }.start();
        return;
    }

    /**
     * Handles the message by passing it to the underlying PaxosAcceptor
     *
     * @param a_msg
     *            The message to be handled
     */
    public synchronized void handleMessage(PaxosMessage a_msg)
    {
        addMessage(a_msg);
    }

    /**
     * Callback for the RejectManager. If this gets called, our last common
     * random is not correct. All executions with the old identifier will be
     * canceled and a new PaxosInstance with the correct identifier will be
     * started
     *
     * @param a_newRound
     */
    public void notifyReject(String a_newRound)
    {
        if (m_lastRandom.equals(a_newRound))
            return;

        PaxosInstance old = (PaxosInstance) m_paxosInstances.get(m_lastRandom);
        if (old != null)
        {
            Enumeration en = old.getExecutions().keys();
            while (en.hasMoreElements())
            {
                PaxosExecution ex = (PaxosExecution) old.getExecutions().get(en.nextElement());
                ex.cancel();
            }
        }
        if (a_newRound == null)
            babylonianConfusion();

        info(getIdentifier() + ": REJECT SUCCESSFUL, STARTING ROUND WITH: MY " + a_newRound
                + ", OLD WAS " + m_lastRandom);
        m_lastRandom = a_newRound;
        startRound();
        String proposal = Long.toString((new SecureRandom()).nextLong());
        m_ownCommitment = CommitmentScheme.createCommitment(proposal);
        propose(m_ownCommitment.getCommitmentAsString());

        synchronized (m_potentialyCorrectMessage)
        {
            debug("Will now handle " + m_potentialyCorrectMessage.size() + " ex-rejected messages");
            Enumeration en = m_potentialyCorrectMessage.elements();
            int i = 1;
            while (en.hasMoreElements())
            {

                PaxosMessage msg = (PaxosMessage) en.nextElement();
                handleIncommingMessage(msg);
                i++;
            }
            m_potentialyCorrectMessage.clear();
        }

    }

    /**
     * If something went terribly wrong, this gets called. Should never happen
     * but who knows ;-)
     *
     * @todo Handle confusion (maybe send mail to operator...)
     */
    private void babylonianConfusion()
    {
        fatal("BABYLONIAN CONFUSION");
        m_lastRandom = AgreementConstants.DEFAULT_COMMON_RANDOM;
    }

    /**
     * This gets called when an agreement has been reached, i.e. we decided on a
     * value for all known IPaxosTargets. If the currect execution has been a
     * commitment-phase, the corresponding reveal-phase will be started.
     * Otherwise the agreements will be combined and new cascades will be built
     */
    protected void notifyAgreement(Hashtable a_agreements)
    {
        synchronized (m_potentialyCorrectMessage)
        {
            m_potentialyCorrectMessage.clear();
        }
        if (m_currentState == STATE_COMMITMENT)
        {
            m_currentState = STATE_PHASE_GAP;
            info("Done with commitment phase...waiting for others");
            m_commitments = a_agreements;
            m_lastRandom = m_lastRandom + "--r";
            // startRound();
            new Thread()
            {
                public void run()
                {
                    try
                    {
                        Thread.sleep(DynamicConfiguration.getInstance().getAgreementPhaseGap());
                    }
                    catch (InterruptedException e)
                    {
                        error("Unable to wait between commitment phase and reveal phase: "
                                + e.toString());
                    }
                    info("Starting reveal phase");
                    m_currentState = STATE_REVEAL;
                    startRound();
                    propose(m_ownCommitment.getRevealAsString());

                    // Handle the messages which arrived during the passive
                    // phase
                    synchronized (m_passivelyReceivedMessages)
                    {
                        debug("Will now handle " + m_passivelyReceivedMessages.size()
                                + " passively received messages");
                        Enumeration en = m_passivelyReceivedMessages.elements();
                        int i = 1;
                        while (en.hasMoreElements())
                        {

                            PaxosMessage msg = (PaxosMessage) en.nextElement();
                            debug("Now handling passively received " + msg.getMessageType()
                                    + "-message from " + msg.getSender() + " (" + i + ")");
                            handleIncommingMessage(msg);
                            i++;
                        }
                        m_passivelyReceivedMessages.clear();
                    }
                }
            }.start();
            return;
        }
        Enumeration en = a_agreements.keys();
        long agreement = 0;
        int count = 0;
        while (en.hasMoreElements())
        {
            String is = en.nextElement().toString();
            if (a_agreements.get(is).equals("NULL"))
            {
                debug("Agreement for " + is + " is NULL");
            }
            else
            {
                String reveal = a_agreements.get(is).toString();
                if (CommitmentScheme.verifyCommitment(m_commitments.get(is).toString(), reveal))
                {
                    String proposal = reveal.split("#")[1];
                    agreement += Long.parseLong(proposal);
                    info("Agreement for " + is + ": " + Long.parseLong(proposal));
                    count++;
                }
                else
                {
                    debug("ARGS, the reveal " + reveal + " from " + is
                            + " doesn't match its commitment");
                }
            }

        }
        info("Agreement  : " + agreement);
        // If the last random was 00000, we don't use the outcome of this
        // agreement because of
        // a possible replay attack. Instead we restart the agreement :-)
        final boolean needRestart = AgreementConstants.DEFAULT_COMMON_RANDOM.equals(Util.replaceAll(m_lastRandom,"--r", ""));
        if (!needRestart)
        {
            // we don't use the value if less then 2 third of the infoservices
            // participated
            if (count < getQuorumTwoThird())
            {
                info("To few infoservices participated in the value, won't use it");
                return;
            }
            useAgreement(agreement);
        }
        else
        {
            info("Need restart, old LCR was the default one!");
        }

        final long tmp = agreement;
        new Thread()
        {
            public void run()
            {
                try
                {
                    Thread.sleep(DynamicConfiguration.getInstance().getAgreementPhaseGap());
                }
                catch (InterruptedException e)
                {
                    error("Unable to sleep in thread");
                }
                m_lastRandom = Long.toString(tmp);
                if (needRestart)
                {
                    startPassivePhase();
                    return;
                }

                m_currentState = STATE_INACTIVE;

                m_starterThread = new AgreementStarterThread();
                m_starterThread.start();

                info("Ready to start a new agreement");
            }
        }.start();
    }

    /**
     * Starts the passive phase of the protocol. Incomming messages will be
     * queued and handled when we enter the active phase
     */
    public void startPassivePhase()
    {
        info("Starting passive phase");
        m_currentState = STATE_PASSIVE;
        new Thread()
        {
            public void run()
            {
                try
                {
                    Thread.sleep(DynamicConfiguration.getInstance().getPassivePhaseLength());
                }
                catch (InterruptedException e)
                {
                    LogHolder.log(LogLevel.ERR, LogType.AGREEMENT,
                            "Unable to wait for passive phase interval", e);
                }
                // Start the agreement
                startAgreement();
            }
        }.start();

    }

    /**
     * What shall we do with an agreement? You can define it here
     *
     * @param a_agreement
     */
    protected abstract void useAgreement(long a_agreement);

    /**
     * The AgreementStarterThread looks from time to time if the next agreement
     * has to be startet. If so, it starts the passive phase of the protocol
     */
    class AgreementStarterThread extends Thread
    {
        private boolean m_canceled = false;

        /*
         * (non-Javadoc)
         *
         * @see java.lang.Thread#run()
         */
        public void run()
        {
            // First we wait for the time to start the agreement
            while (!timeForAgreement())
            {
                try
                {
                    Thread.sleep(AgreementConstants.TIME_WATCH_POLLING_INTERVAL);
                }
                catch (InterruptedException e)
                {
                    LogHolder.log(LogLevel.ERR, LogType.AGREEMENT,
                            "Unable to wait for agreement polling interval", e);
                }
                if (m_canceled)
                {
                    return;
                }

            }
            // Then we start the passive phase and the countdown to active
            startPassivePhase();
        }

        /**
         * Used to cancel this thread. This is only needed for the manual start
         * by an operator which has to be disabled when in production
         */
        public void cancel()
        {
            this.m_canceled = true;
        }

        /**
         * Checks if the current time is acceptable to start a the passive phase
         * for the new agreement
         *
         * @return true if a new agreement can be startet, falso otherwise
         */
        private boolean timeForAgreement()
        {
            Calendar c = Calendar.getInstance(TimeZone.getTimeZone("CET"));
            int hour = DynamicConfiguration.getInstance().getHourOfAgreement();
            /* Try to start the active part */

            /** @fixme Use hours instead of minutes */
            return (c.get(Calendar.MINUTE) % hour == 0);
            // return (c.get(Calendar.HOUR) == hour && c.get(Calendar.MINUTE) ==
            // 0 && m_currentState == STATE_INACTIVE)
        }
    }
}
