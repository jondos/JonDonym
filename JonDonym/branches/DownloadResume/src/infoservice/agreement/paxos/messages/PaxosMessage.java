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
package infoservice.agreement.paxos.messages;

import infoservice.agreement.paxos.PaxosExecution;
import infoservice.agreement.paxos.PaxosInstance;
import infoservice.agreement.paxos.PaxosRound;

public class PaxosMessage
{
    /** Static type of a propose message */
    public static String PROPOSE = "PROPOSE";

    /** Static type of a weak accept message */
    public static String WEAK = "WEAK";

    /** Static type of a strong accept message */
    public static String STRONG = "STRONG";

    /** Static type of a decide message */
    public static String DECIDE = "DECIDE";

    /** Static type of a freeze message */
    public static String FREEZE = "FREEZE";

    /** Static type of a reject message */
    public static String REJECT = "REJECT";

    /** Static type of a freeze-proof message */
    public static String FREEZEPROOF = "FREEZEPROOF";

    /** Static type of a collect message */
    public static String COLLECT = "COLLECT";

    private String m_messageType;

    private String m_proposal = "";

    private String m_paxosInstanceIdentifier = "";

    private String m_initiator = "";

    private String m_sender = "";

    private int m_round;

    /**
     * Creates a new PaxosMessage of the given type
     * 
     * @param a_messageType
     *            One of the message types
     */
    public PaxosMessage(String a_messageType)
    {
        this.m_messageType = a_messageType;
    }

    /**
     * Returns the message type of this message
     * 
     * @return The message type of this message
     */
    public String getMessageType()
    {
        return this.m_messageType;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#equals(java.lang.Object)
     */
    public boolean equals(Object ob)
    {
        if (!(ob instanceof PaxosMessage))
            return false;
        PaxosMessage other = (PaxosMessage) ob;
        boolean result = this.m_initiator.equals(other.getInitiator());
        result &= this.m_messageType.equals(other.getMessageType());
        result &= this.m_paxosInstanceIdentifier.equals(other.getPaxosInstanceIdentifier());
        result &= this.m_sender.equals(other.getSender());
        result &= this.m_proposal.equals(other.getProposal());
        result &= this.m_round == other.getRound();
        return result;
    }

    public int getRound()
    {
        return this.m_round;
    }

    /**
     * Returns the ID of the initiator of this messages execution
     * 
     * @return The ID of the initiator of this messages execution
     */
    public String getInitiator()
    {
        return this.m_initiator;
    }

    /**
     * Returns the proposal of this execution
     * 
     * @return The proposal of this execution
     */
    public String getProposal()
    {
        return this.m_proposal;
    }

    /**
     * Returns the round number of this message
     * 
     * @return The round number of this message
     */
    public String getPaxosInstanceIdentifier()
    {
        return this.m_paxosInstanceIdentifier;
    }

    /**
     * Returns the ID of the sender of this message
     * 
     * @return The ID of the sender of this message
     */
    public String getSender()
    {
        return this.m_sender;
    }

    public void setRound(int a_round)
    {
        this.m_round = a_round;
    }

    /**
     * Sets the initiator of the executions this message belongs to
     * 
     * @param a_initiator
     *            The ID of the initiator of the executions this message belongs
     *            to
     */
    public void setInitiator(String a_initiator)
    {
        this.m_initiator = a_initiator;
    }

    /**
     * Sets the proposal of this message
     * 
     * @param a_proposal
     *            The proposal contained in this message
     */
    public void setProposal(String a_proposal)
    {
        this.m_proposal = a_proposal;
    }

    public void setPaxosInstanceIdentifier(String a_value)
    {
        this.m_paxosInstanceIdentifier = a_value;
    }

    /**
     * Sets the ID of the sender of this message
     * 
     * @param a_sender
     *            The ID of the sender of this message
     */
    public void setSender(String a_sender)
    {
        this.m_sender = a_sender;
    }

    /**
     * Indicates if this message belongs to the given paxos instance
     * 
     * @param a_instance
     *            The instance to be tested
     * @return true if the message belongs to that instance, false otherwise
     */
    public boolean belongsTo(PaxosInstance a_instance)
    {
        return a_instance.getInstanceNumber().equals(this.m_paxosInstanceIdentifier);
    }

    /**
     * Indicates if this message belongs to the given execution
     * 
     * @param a_execution
     *            The execution to be tested
     * @return true if the message belongs to that execution, false otherwise
     */
    public boolean belongsTo(PaxosExecution a_execution)
    {
        return a_execution.getInitiator().equals(this.m_initiator)
                && belongsTo(a_execution.getPaxosInstance());
    }

    /**
     * Indicates if this message belongs to the given round
     * 
     * @param a_round
     *            The round to be tested
     * @return true if the message belongs to that round, false otherwise
     */
    public boolean belongsTo(PaxosRound a_round)
    {
        return belongsTo(a_round.getExecution()) && (a_round.getRoundNumber() == this.m_round);
    }

}
