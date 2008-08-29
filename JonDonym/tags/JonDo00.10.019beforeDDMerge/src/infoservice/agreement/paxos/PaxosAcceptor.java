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
package infoservice.agreement.paxos;

import infoservice.agreement.common.TimeoutListener;
import infoservice.agreement.paxos.messages.CollectMessage;
import infoservice.agreement.paxos.messages.FreezeProofMessage;
import infoservice.agreement.paxos.messages.PaxosMessage;

import java.util.Collections;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

public abstract class PaxosAcceptor extends PaxosProposer implements TimeoutListener
{
    /** A collection of all running PAXOS instances */
    protected Hashtable m_paxosInstances = new Hashtable();

    /**
     * This gets called if a PaxosMessage arrives. The handling methods sort the
     * message to the matching instance, execution and round
     * 
     * @param a_msg
     *            The message to be handled
     */
    public void addMessage(PaxosMessage a_msg)
    {
        if (a_msg.getMessageType().equals(PaxosMessage.PROPOSE))
            addProposeMessage(a_msg);
        else if (a_msg.getMessageType().equals(PaxosMessage.WEAK))
            addWeakMessage(a_msg);
        else if (a_msg.getMessageType().equals(PaxosMessage.STRONG))
            addStrongMessage(a_msg);
        else if (a_msg.getMessageType().equals(PaxosMessage.DECIDE))
            addDecideMessage(a_msg);
        else if (a_msg.getMessageType().equals(PaxosMessage.FREEZE))
            addFreezeMessage(a_msg);
        else if (a_msg.getMessageType().equals(PaxosMessage.FREEZEPROOF))
            addFreezeProofMessage(a_msg);
        else if (a_msg.getMessageType().equals(PaxosMessage.COLLECT))
            addCollectMessage(a_msg);
    }

    /**
     * Adds a CollectMessage. A CollectMessage contains a new proposal which has
     * to be "good" according to the attached proof, otherwise it will not be
     * accepted
     * 
     * @param a_msg
     *            The CollectMessage to be handled
     */
    private void addCollectMessage(PaxosMessage a_msg)
    {
        info("Add collect message from: " + a_msg.getSender() + " with proposal: "
                + a_msg.getProposal());
        PaxosRound round = getPaxosRound(a_msg);
        if (round == null)
            return;
        if (round.addCollectMessage((CollectMessage) a_msg))
        {
            if (round.isGood(a_msg.getProposal()))
            {
                weakAccept(round, a_msg);
            }
            else
            {
                error(a_msg.getSender() + " proposed " + a_msg.getProposal()
                        + " which is not GOOD!");
            }
        }

    }

    /**
     * Adds a FreezeProofMessage. A FreezeProofMessage contains the weakly
     * accepted value and the strongly accepted value of the sender in the last
     * round. These values as signed and will be used to compute a "good"
     * proposal for the following round
     * 
     * @param a_msg
     *            The FreezeProofMessage to be handled
     */
    private void addFreezeProofMessage(PaxosMessage a_msg)
    {
        int oldRound = a_msg.getRound();
        PaxosExecution exec = getPaxosInstance(a_msg.getPaxosInstanceIdentifier()).getExecution(
                a_msg);
        PaxosRound round = exec.getRound(oldRound + 1);
        info("Received FreezeProof from " + a_msg.getSender() + " for " + a_msg.getInitiator()
                + ", Round: " + a_msg.getRound());
        if (round == null)
            round = exec.createRound(oldRound + 1);
        // If we are the new leader...
        if (round.getLeaderId().equals(exec.getPaxosInstance().getAcceptor().getIdentifier()))
        {
            if (round.addProof((FreezeProofMessage) a_msg))
            {
                // If there are enough proofs of others we can compute the
                // "good" proposal for the next round and send a CollectMessage
                if (round.getFreezeProofMessageCount() > 2 * getF())
                {
                    debug("That are enough FreezeProofs, sending COLLECT");
                    String good = round.getExecution().getGoodProposal(round.getRoundNumber());
                    CollectMessage msg = new CollectMessage();
                    msg.setInitiator(exec.getInitiator());
                    msg.setPaxosInstanceIdentifier(a_msg.getPaxosInstanceIdentifier());
                    msg.setProposal(good);
                    msg.setProofs(round.getFreezeProofs());
                    msg.setSender(getIdentifier());
                    msg.setRound(oldRound);
                    multicast(msg);
                }
                else
                {
                    debug("Not enough proves yet: need " + (2 * getF() + 1) + ", have "
                            + round.getFreezeProofMessageCount());
                }
            }
        }
    }

    /**
     * Adds a FreezeMessage. A FreezeMessage is sent by others if their local
     * timeout for the matching round in the matching execution timed out. If we
     * get more than f FreezeMessages we also freeze the round (because within
     * the min f+1 FreezeMessages we received, at least one came frome an honest
     * party)
     * 
     * @param a_msg
     *            The FreezeMessage to add
     */
    private void addFreezeMessage(PaxosMessage a_msg)
    {
        info("Adding freeze message from: " + a_msg.getSender() + " for " + a_msg.getInitiator()
                + ", Round: " + a_msg.getRound());
        PaxosRound round = getPaxosRound(a_msg);
        if (round == null)
            return;

        if (round.addFreezeMessage(a_msg))
        {
            // Freeze if more than f have frozen the round
            if (round.getFreezeMessageCount() > getF() && !round.isFrozen())
            {
                freeze(round);
            }
            // Now we have a problem. This ensures that at least f+1 freezes
            // are in place at every correct acceptor. We have to start a new
            // round with a new round leader
            if (round.getFreezeMessageCount() > 2 * getF() && round.isFrozen()
                    && !round.isAborted())
            {
                round.abort();
                FreezeProofMessage msg = new FreezeProofMessage();
                cloneMessage(a_msg, msg);
                msg.setWeakValue(round.getWeakAccepted());
                msg.setStrongValue(round.getStrongAccepted());
                PaxosRound next = round.getExecution().createRound(a_msg.getRound() + 1);
                sendMessage(next.getLeaderId(), msg);
            }
        }
    }

    /**
     * Adds a ProposeMessage. A ProposeMesssage is the first message of each
     * execution. A value is proposed that should be accepted by all parties.
     * 
     * @param a_msg
     */
    private void addProposeMessage(PaxosMessage a_msg)
    {
        debug("Proposal received from: " + a_msg.getSender() + " for " + a_msg.getInitiator()
                + ", Round: " + a_msg.getRound());
        PaxosRound round = getPaxosRound(a_msg);
        if (round == null)
            return;
        // We always weak accept proposals
        if (round.addProposal(a_msg) && !round.isWeakAccepted())
        {
            weakAccept(round, a_msg);
        }
    }

    /**
     * Adds a WeakMessage. If a process weakly accepts a proposal, it sends a
     * WeakMessage to all the others. It might be that a process doesn't get the
     * matching proposal, so we add the posibility to weakly accept a value if
     * at least f+1 have done so
     * 
     * @param a_msg
     *            The WeakMessage to be added
     */
    private void addWeakMessage(PaxosMessage a_msg)
    {
        debug("Weak message received from: " + a_msg.getSender() + " for " + a_msg.getInitiator()
                + ", Round: " + a_msg.getRound());
        PaxosRound round = getPaxosRound(a_msg);
        if (round == null)
            return;

        if (round.addWeakMessage(a_msg))
        {
            // If we have more than (n+3f)/2 weak accepts for the porposal, we
            // may decide
            if (round.getWeakMessageCount(a_msg.getProposal()) > getQuorumDecideWeak()
                    && !round.getExecution().isDecided())
            {
                decide(round, a_msg, true);
            }
            // If we have more than (n+f)/2 weak accepts for the proposal from
            // others, we can strongly accept it
            if (round.getWeakMessageCount(a_msg.getProposal()) > getQuorumStrong()
                    && !round.isStrongAccepted())

            {
                strongAccept(round, a_msg);
            }
            // We also weak accept the value of a_msg, if at least f+1 others
            // weak accepted it this means at least one honest node also weak
            // accepted the value
            if (round.getWeakMessageCount(a_msg.getProposal()) > getF() && !round.isWeakAccepted())
            {
                weakAccept(round, a_msg);
            }
        }
    }

    /**
     * Adds a StrongMessage. A StrongMessage is sent by a process which strongly
     * accepted the proposal.
     * 
     * @param a_msg
     *            The StrongMessage to be added
     */
    private void addStrongMessage(PaxosMessage a_msg)
    {
        debug("Strong message received from: " + a_msg.getSender() + " for " + a_msg.getInitiator()
                + ", Round: " + a_msg.getRound());
        PaxosRound round = getPaxosRound(a_msg);
        if (round == null)
            return;

        if (round.addStrongMessage(a_msg))
        {
            if (round.getStrongMessageCount(a_msg.getProposal()) > getQuorumDecideStrong()
                    && !round.getExecution().isDecided())
            {
                decide(round, a_msg, true);
            }
        }
    }

    /**
     * Adds a DecideMessage. DecideMessages are sent if a process decides for
     * the porposal
     * 
     * @param a_msg
     *            The DecideMessage to be added
     */
    private void addDecideMessage(PaxosMessage a_msg)
    {
        debug("Decide message received from: " + a_msg.getSender() + " for " + a_msg.getInitiator()
                + ", Round: " + a_msg.getRound());
        PaxosRound round = getPaxosRound(a_msg);
        if (round == null)
            return;
        if (round.addDecideMessage(a_msg))
        {
            // We decide if at least one honest node decided too
            if (round.getDecideMessageCount(a_msg.getProposal()) > getF()
                    && !round.getExecution().isDecided())
            {
                decide(round, a_msg, true);
            }
        }

    }

    /**
     * Weakly accept the proposal contained in the given message and send a
     * WeakMessage to all processes
     * 
     * @param a_round
     *            The round the message belongs to
     * @param a_msg
     *            The message which triggered the weak acceptance
     */
    private void weakAccept(PaxosRound a_round, PaxosMessage a_msg)
    {
        a_round.weakAccept(a_msg.getProposal());
        PaxosMessage msg = new PaxosMessage(PaxosMessage.WEAK);
        cloneMessage(a_msg, msg);
        multicast(msg);
    }

    /**
     * Copies the attributes of the source message to the target message
     * 
     * @param a_original
     *            The source message
     * @param a_target
     *            The target message
     */
    private void cloneMessage(PaxosMessage a_original, PaxosMessage a_target)
    {
        a_target.setInitiator(a_original.getInitiator());
        a_target.setSender(getIdentifier());
        a_target.setProposal(a_original.getProposal());
        a_target.setRound(a_original.getRound());
        a_target.setPaxosInstanceIdentifier(a_original.getPaxosInstanceIdentifier());
    }

    /**
     * Strongly accept the proposal contained in the given message an send a
     * StrongMessage to all processes
     * 
     * @param a_round
     *            The round the message belongs to
     * @param a_msg
     *            The message which triggered the weak acceptance
     */
    private void strongAccept(PaxosRound a_round, PaxosMessage a_msg)
    {
        a_round.weakAccept(a_msg.getProposal());
        PaxosMessage msg = new PaxosMessage(PaxosMessage.STRONG);
        cloneMessage(a_msg, msg);
        multicast(msg);
    }

    /**
     * Decide for the proposal contained in the given message and send a
     * DecideMessage to all processes if a_sendDecide is true
     * 
     * @param a_round
     *            The round the message belongs to
     * @param a_msg
     *            The message which triggered the weak acceptance
     * @param a_sendDecide
     *            If true a decide message is sent, otherwise not
     */
    private void decide(PaxosRound a_round, PaxosMessage a_msg, boolean a_sendDecide)
    {
        info("DECIDED ON " + a_msg.getProposal() + " FOR " + a_msg.getInitiator());
        a_round.decide(a_msg.getProposal());
        a_round.getExecution().decide(a_msg.getProposal());
        if (a_sendDecide)
        {
            PaxosMessage msg = new PaxosMessage(PaxosMessage.DECIDE);
            cloneMessage(a_msg, msg);
            multicast(msg);
        }
    }

    /**
     * Freezes the given round either if a timeout oocured or if we got more
     * than f FreezeMessages
     * 
     * @param a_round
     *            The round to be frozen
     */
    private void freeze(PaxosRound a_round)
    {
        // debug("Freezed Agreement for " + a_round.getInitiator());
        info("FROZEN ROUND " + a_round.getRoundNumber() + " FOR "
                + a_round.getExecution().getInitiator());
        a_round.freeze();
        PaxosMessage msg = new PaxosMessage(PaxosMessage.FREEZE);
        msg.setInitiator(a_round.getExecution().getInitiator());
        msg.setSender(getIdentifier());
        msg.setRound(a_round.getRoundNumber());
        msg.setPaxosInstanceIdentifier(a_round.getExecution().getPaxosInstance()
                .getInstanceNumber());
        multicast(msg);
    }

    /**
     * Callback method for the PaxosTimeout of a PaxosRound. Freezes the round
     * if it hasn't been decided yet
     * 
     * @param m_round
     */
    public void timeout(Object m_round)
    {
        PaxosRound round = (PaxosRound) m_round;
        if (round.getExecution().isDecided() || round.isFrozen())
            return;
        freeze(round);
    }

    /**
     * Returns the PaxosInstance identified by the given id
     * 
     * @param a_paxosInstanceId
     *            The ID of the instance to be returned
     * @return The PaxosInstance with the given ID
     */
    public PaxosInstance getPaxosInstance(String a_paxosInstanceId)
    {
        return (PaxosInstance) m_paxosInstances.get(a_paxosInstanceId);
    }

    /**
     * Returns the PaxosRound to which the given message belongs
     * 
     * @param a_msg
     *            The message used to determine the Round to be returned
     * @return The PaxosRound to which the given message belongs
     */
    private PaxosRound getPaxosRound(PaxosMessage a_msg)
    {
        PaxosInstance inst = getPaxosInstance(a_msg.getPaxosInstanceIdentifier());
        if (inst == null)
        {
            error("PaxosInstance is for " + a_msg.getPaxosInstanceIdentifier()
                    + " is NULL, how can that be?");
            return null;
        }
        PaxosRound result = inst.getExecution(a_msg).getRound(a_msg.getRound());
        if (result == null)
            result = inst.getExecution(a_msg).createRound(a_msg.getRound());
        return result;
    }

    protected Vector getRoundLeaders()
    {
        Vector roundLeaders = new Vector();
        Enumeration en = m_targets.keys();
        while (en.hasMoreElements())
        {
            String leader = (String) en.nextElement();
            roundLeaders.add(leader);
        }
        Collections.sort(roundLeaders);
        return roundLeaders;
    }

    /**
     * Callback for implementing classes. This gets called when agreement has
     * been reached, that means every proposer proposed (which leads to an
     * execution) and every execution has beed decided
     * 
     * @param a_agreements
     *            Map of execution-identifier to decission
     */
    protected abstract void notifyAgreement(Hashtable a_agreements);
}
