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

import infoservice.agreement.paxos.messages.PaxosMessage;

import java.util.Hashtable;

public class PaxosInstance extends PaxosObject
{
    /**
     * A hashtable containing the decissions of this instance, execution-id
     * mapped to decission
     */
    private Hashtable m_decissions = new Hashtable();

    /** A hashtable containing all executions which belong to this instance */
    protected Hashtable m_executions = new Hashtable();

    /** The identifier of this instance. */
    private String m_instanceNumber;

    /** The PaxosAcceptor to which this instance belongs */
    private PaxosAcceptor m_acceptor;

    private boolean m_notified = false;

    /**
     * Creates a new PaxosInstance for the given PaxosAcceptor and the given
     * instance number
     * 
     * @param a_acceptor
     *            The PaxosAcceptor to which this instance belongs
     * @param a_instanceNumber
     *            The unique instance number
     */
    public PaxosInstance(PaxosAcceptor a_acceptor, String a_instanceNumber)
    {
        this.m_instanceNumber = a_instanceNumber;
        this.m_acceptor = a_acceptor;
    }

    /**
     * Returns the PaxosAcceptor this instance belongs to
     * 
     * @return
     */
    public PaxosAcceptor getAcceptor()
    {
        return m_acceptor;
    }

    /**
     * Returns the PaxosExecution atching the given message
     * 
     * @param a_msg
     *            A message
     * @return The exectution the message belongs to
     */
    public PaxosExecution getExecution(PaxosMessage a_msg)
    {
        PaxosExecution result = (PaxosExecution) m_executions.get(a_msg.getInitiator());
        if (result == null)
        {
            error("Unknown PaxosExecution requested! (" + a_msg.getInitiator()
                    + "), problably inconsistent InfoService");
        }
        return result;
    }

    /**
     * Returns a hashtable containing the decissions of this instance. Key is
     * the ID of the PaxosTarget, value is the decission for this PaxosTarget
     * 
     * @return A Hashtable with the decissions
     */
    public Hashtable getDecissions()
    {
        return m_decissions;
    }

    /**
     * Returns a Hashtable containing all Executions of this round
     * 
     * @return
     */
    public Hashtable getExecutions()
    {
        return m_executions;
    }

    /**
     * Returns the round number of this round
     * 
     * @return The round number of this round
     */
    public String getInstanceNumber()
    {
        return m_instanceNumber;
    }

    /**
     * Adds the decission for the given initiator
     * 
     * @param a_initiator
     *            The decission for the execution of this initiator will be set
     * @param a_value
     *            The reached decission
     */
    public void addDecision(String a_initiator, String a_value)
    {
        this.m_decissions.put(a_initiator, a_value);
        if (m_decissions.size() >= m_acceptor.getN() && !m_notified)
        {
            m_notified = true;
            m_acceptor.notifyAgreement(m_decissions);
        }
    }
}
