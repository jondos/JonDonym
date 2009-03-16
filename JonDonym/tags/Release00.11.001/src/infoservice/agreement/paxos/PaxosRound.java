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

import infoservice.agreement.common.TimeoutThread;
import infoservice.agreement.paxos.messages.CollectMessage;
import infoservice.agreement.paxos.messages.FreezeProofMessage;
import infoservice.agreement.paxos.messages.PaxosMessage;
import infoservice.dynamic.DynamicConfiguration;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

public class PaxosRound extends PaxosObject
{
    private TimeoutThread m_timeout;

    private PaxosMessage m_proposal = null;

    private Hashtable m_weakMessages = new Hashtable();

    private Hashtable m_strongMessages = new Hashtable();

    private Hashtable m_decideMessages = new Hashtable();

    private Vector m_freezeMessages = new Vector();

    private Vector m_freezeProofMessages = new Vector();

    private PaxosExecution m_execution;

    private String m_weakAccepted = null;

    private String m_strongAccepted = null;

    private boolean m_freezed = false;

    private int m_roundNumber = -1;

    private String m_leaderId;

    private String m_decided;

    private boolean m_collected = false;

    private boolean m_aborted = false;

    /**
     * Creates a new PaxosRound within the given PaxosExecution with the given
     * round number and the given leaderId
     * 
     * @param a_execution
     *            The PaxosExecution this round belongs to
     * @param a_round
     *            The round number
     * @param a_leaderId
     *            The leaders id
     */
    public PaxosRound(PaxosExecution a_execution, int a_round, String a_leaderId)
    {
        // First round needs to wait as long as the passive phase might have
        // been + the normal timeout to resynchronize
        /** @fixme Should be in integration package, as it is infoservice-dependent */
        if (a_round == 0)
        {
            m_timeout = new TimeoutThread(a_execution.getPaxosInstance().getAcceptor(), this,
                    DynamicConfiguration.getInstance().getPassivePhaseLength()
                            + DynamicConfiguration.getInstance().getPaxosRoundTimeout());
        }
        else
        {
            m_timeout = new TimeoutThread(a_execution.getPaxosInstance().getAcceptor(), this,
                    DynamicConfiguration.getInstance().getPaxosRoundTimeout());
        }
        m_timeout.start();
        this.m_execution = a_execution;
        this.m_roundNumber = a_round;
        this.m_leaderId = a_leaderId;
    }

    /**
     * Returns the ID of the leader of thi round
     * 
     * @return The ID of the leader of thi round
     */
    public String getLeaderId()
    {
        return m_leaderId;
    }

    /**
     * Returns the round number of this round
     * 
     * @return The round number of this round
     */
    public int getRoundNumber()
    {
        return m_roundNumber;
    }

    /**
     * Adds a proposal for tihs round
     * 
     * @param a_msg
     *            The ProposeMessage to be added
     * @return true if the message could be added, false otherwise
     */
    public boolean addProposal(PaxosMessage a_msg)
    {
        if (m_proposal == null && a_msg.belongsTo(m_execution))
        {
            m_proposal = a_msg;
            return true;
        }
        return false;
    }

    /**
     * Returns the PaxosExecution this round belongs to
     * 
     * @return The PaxosExecution this round belongs to
     */
    public PaxosExecution getExecution()
    {
        return m_execution;
    }

    /**
     * Adds a WeakMessage to this round
     * 
     * @param a_msg
     *            The message to be added
     * @return true if the message could be added, false otherwise
     */
    public boolean addWeakMessage(PaxosMessage a_msg)
    {
        return addMessage(m_weakMessages, a_msg);
    }

    /**
     * Adds a StrongMessage to this round
     * 
     * @param a_msg
     *            The message to be added
     * @return true if the message could be added, false otherwise
     */
    public boolean addStrongMessage(PaxosMessage a_msg)
    {
        return addMessage(m_strongMessages, a_msg);
    }

    /**
     * Adds a DecideMessage to this round
     * 
     * @param a_msg
     *            The message to be added
     * @return true if the message could be added, false otherwise
     */
    public boolean addDecideMessage(PaxosMessage a_msg)
    {
        return addMessage(m_decideMessages, a_msg);
    }

    /**
     * Adds a FreezeMessage to this round
     * 
     * @param a_msg
     *            The message to be added
     * @return true if the message could be added, false otherwise
     */
    public boolean addFreezeMessage(PaxosMessage a_msg)
    {
        if (!m_freezeMessages.contains(a_msg) && a_msg.belongsTo(m_execution))
        {
            m_freezeMessages.add(a_msg);
            return true;
        }
        return false;
    }

    /**
     * Weakly accepts the given proposal
     * 
     * @param a_proposal
     *            The proposal to be weakly accepted
     */
    public void weakAccept(String a_proposal)
    {
        this.m_weakAccepted = a_proposal;
    }

    /**
     * Strongly accepts the given proposal
     * 
     * @param a_proposal
     *            The proposal to be strongly accepted
     */
    public void strongAccept(String a_proposal)
    {
        this.m_strongAccepted = a_proposal;
    }

    /**
     * Returns the value which has been weakly accepted
     * 
     * @return The value which has been weakly accepted
     */
    public String getWeakAccepted()
    {
        return m_weakAccepted;
    }

    /**
     * Returns the value which has been strongly accepted
     * 
     * @return he value which has been strongly accepted
     */
    public String getStrongAccepted()
    {
        return m_strongAccepted;
    }

    /**
     * Indicates if there is a weakly accepted value for this round
     * 
     * @return true if there is a weakly accepted value for this round, false
     *         otherwise
     */
    public boolean isWeakAccepted()
    {
        return (m_weakAccepted != null);
    }

    /**
     * Returns the number of weak-messages received for the given proposal
     * 
     * @param a_proposal
     *            The proposal to be checked
     * @return The number of weak-messages received for the given proposal
     */
    public int getWeakMessageCount(String a_proposal)
    {
        Vector tmp = (Vector) m_weakMessages.get(a_proposal);
        if (tmp == null)
            return 0;
        return tmp.size();
    }

    /**
     * Returns the number of strong-messages received for the given proposal
     * 
     * @param a_proposal
     *            The proposal to be checked
     * @return The number of strong-messages received for the given proposal
     */
    public int getStrongMessageCount(String a_proposal)
    {
        Vector tmp = (Vector) m_strongMessages.get(a_proposal);
        if (tmp == null)
            return 0;
        return tmp.size();
    }

    /**
     * Returns the number of decide-messages received for the given proposal
     * 
     * @param a_proposal
     *            The proposal to be checked
     * @return The number of decide-messages received for the given proposal
     */
    public int getDecideMessageCount(String a_proposal)
    {
        Vector tmp = (Vector) m_decideMessages.get(a_proposal);
        if (tmp == null)
            return 0;
        return tmp.size();
    }

    /**
     * Returns the nunmber of totally received decide messages
     * 
     * @return he nunmber of totally received decide messages
     */
    public int getTotalDecideMessageCount()
    {
        int count = 0;
        Enumeration en = m_decideMessages.keys();
        while (en.hasMoreElements())
        {
            Object key = en.nextElement();
            Vector tmp = (Vector) m_decideMessages.get(key);
            count += tmp.size();
        }
        return count;
    }

    /**
     * Returns the nunmber of totally received freeze messages
     * 
     * @return he nunmber of totally received freeze messages
     */
    public int getFreezeMessageCount()
    {
        return m_freezeMessages.size();
    }

    /**
     * Does some message checking and then adds the given message to the given
     * hashtable
     * 
     * @param a_messages
     *            The message to be added
     * @param a_msg
     *            The hashtable the message should be added to
     * @return true if the message has been added, false otherwise
     */
    private boolean addMessage(Hashtable a_messages, PaxosMessage a_msg)
    {
        if (!a_msg.belongsTo(this))
        {
            return false;
        }

        Vector tmp = null;
        if (!a_messages.containsKey(a_msg.getProposal()))
        {
            tmp = new Vector();
            a_messages.put(a_msg.getProposal(), tmp);
        }
        tmp = (Vector) a_messages.get(a_msg.getProposal());
        if (!tmp.contains(a_msg))
        {
            tmp.add(a_msg);
            return true;
        }
        return false;
    }

    /**
     * Decides on the given value. This makes the execution decide too and the
     * timeout for this round is canceled
     * 
     * @param a_proposal
     *            The proposal to decide to
     */
    public void decide(String a_proposal)
    {
        this.m_decided = a_proposal;
        m_timeout.cancel();
        getExecution().decide(a_proposal);
    }

    /**
     * Indicates of this round has been decided
     * 
     * @return true if we decided for a proposal, false otherwise
     */
    public boolean isDecided()
    {
        return (m_decided != null);
    }

    /**
     * Indicates if we have strongly accepted a proposal in this round
     * 
     * @return true if we strongly accepted a proposal in this round, false
     *         otherwise
     */
    public boolean isStrongAccepted()
    {
        return (m_strongAccepted != null);
    }

    /**
     * Freezes this round. Timeout is canceled
     */
    public void freeze()
    {
        this.m_freezed = true;
        m_timeout.cancel();
    }

    /**
     * Indicates if this round has been frozen
     * 
     * @return true if the round is frozen, false otherwise
     */
    public boolean isFrozen()
    {
        return m_freezed;
    }

    /**
     * Adds a FreezeProofMessage to this round.
     * 
     * @param a_msg
     *            The FreezeProofMessage to be added
     * @return true if the message has been added, false otherwise
     */
    public boolean addProof(FreezeProofMessage a_msg)
    {
        if (a_msg.isSignatureOk())
        {
            if (!m_freezeProofMessages.contains(a_msg))
            {
                m_freezeProofMessages.add(a_msg);
                return true;
            }
        }
        return false;
    }

    /**
     * Returns the number of FreezeProofMessages received in this round
     * 
     * @return The number of FreezeProofMessages received in this round
     */
    public int getFreezeProofMessageCount()
    {
        return m_freezeProofMessages.size();
    }

    /**
     * Calculates if the given proposal belongs to the set Acc (see Paxos at
     * War)
     * 
     * @param a_proposal
     *            The proposal to check
     * @returne if the proposal belongs to Acc, false otherwise
     */
    public boolean acc(String a_proposal)
    {
        if (a_proposal == null)
            return false;
        int count = 0;
        Enumeration en = m_freezeProofMessages.elements();
        while (en.hasMoreElements())
        {
            FreezeProofMessage msg = (FreezeProofMessage) en.nextElement();
            if (a_proposal.equals(msg.getWeakValue()))
                count++;
        }
        if (count > getExecution().getPaxosInstance().getAcceptor().getF())
            return true;
        return false;
    }

    /**
     * Returns the set Acc for this round
     * 
     * @return The set Acc for this round
     */
    public Vector getAcc()
    {
        Vector result = new Vector();
        Enumeration en = m_freezeProofMessages.elements();
        while (en.hasMoreElements())
        {
            FreezeProofMessage msg = (FreezeProofMessage) en.nextElement();
            String weak = msg.getWeakValue();
            if (poss(weak) && !result.contains(weak))
                result.add(weak);
            String strong = msg.getStrongValue();
            if (poss(strong) && !result.contains(strong))
                result.add(strong);
        }
        return result;
    }

    /**
     * Returns the set Poss for this round
     * 
     * @return The set Poss for this round
     */
    public Vector getPoss()
    {
        Vector result = new Vector();
        Enumeration en = m_freezeProofMessages.elements();
        while (en.hasMoreElements())
        {
            FreezeProofMessage msg = (FreezeProofMessage) en.nextElement();
            String weak = msg.getWeakValue();
            if (poss(weak) && !result.contains(weak))
                result.add(weak);
            String strong = msg.getStrongValue();
            if (poss(strong) && !result.contains(strong))
                result.add(strong);

        }
        return result;
    }

    /**
     * Calculates if the given proposal belongs to the set Poss (see Paxos at
     * War)
     * 
     * @param a_proposal
     *            The proposal to check
     * @returne if the proposal belongs to Poss, false otherwise
     */
    public boolean poss(String a_proposal)
    {
        if (a_proposal == null)
            return false;
        int countWeak = 0;
        int countStrong = 0;
        Enumeration en = m_freezeProofMessages.elements();
        while (en.hasMoreElements())
        {
            FreezeProofMessage msg = (FreezeProofMessage) en.nextElement();
            if (a_proposal.equals(msg.getWeakValue()))
                countWeak++;
            if (a_proposal.equals(msg.getStrongValue()))
                countStrong++;
        }
        if (countWeak > getExecution().getPaxosInstance().getAcceptor().getQuorumStrong()
                || countStrong > getExecution().getPaxosInstance().getAcceptor().getF())
            return true;
        return false;
    }

    /**
     * Indicates if the given value is "good" (see Paxos at War)
     * 
     * @param a_value
     *            The value to be tested
     * @return true if the given value is "good", false otherwise
     */
    public boolean isGood(String a_value)
    {
        if (a_value == null)
            return false;
        String calculatedGood = m_execution.getGoodProposal(m_roundNumber);
        return calculatedGood.equals(a_value);
    }

    /**
     * Returns the FreezeProofMessages for tihs round
     * 
     * @return A vector containing the FreezeProofMessages of this round
     */
    public Vector getFreezeProofs()
    {
        return m_freezeProofMessages;
    }

    /**
     * Adds a CollectMessage to this round.
     * 
     * @param a_msg
     *            The CollectMessage to be added
     * @return true if the message has been added, false otherwise
     */
    public boolean addCollectMessage(CollectMessage a_msg)
    {
        if (!m_collected)
        {
            m_freezeProofMessages.addAll(a_msg.getProofs());
            m_collected = true;
            return true;
        }
        return false;
    }

    /**
     * Indicates if this round has been aborted
     * 
     * @return true if this round has been aborted, false otherwise
     */
    public boolean isAborted()
    {
        return m_aborted;
    }

    /**
     * Aborts this round
     */
    public void abort()
    {
        m_aborted = true;
        m_timeout.cancel();
    }
}
