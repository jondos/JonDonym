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

import infoservice.agreement.paxos.PaxosInstance;
import infoservice.agreement.paxos.PaxosObject;
import infoservice.agreement.paxos.messages.PaxosMessage;

import java.util.Enumeration;
import java.util.Hashtable;

public class PaxosRejectManager extends PaxosObject
{
    private PaxosInstance m_paxosInstance;

    private Hashtable m_rejectMessages = new Hashtable();

    private Hashtable m_rejectCount = new Hashtable();

    /**
     * Creates a new RejectManager for the given PaxosInstance
     * 
     * @param a_paxosInstance
     */
    public PaxosRejectManager(PaxosInstance a_paxosInstance)
    {
        this.m_paxosInstance = a_paxosInstance;
    }

    /**
     * Handles an incomming RejectMessage
     * 
     * @param a_msg
     *            The message to be handled
     * @return true if the message has been added, false otherwise
     */
    public boolean addRejectMessage(PaxosMessage a_msg)
    {
        if (a_msg == null || !a_msg.getMessageType().equals(PaxosMessage.REJECT))
            return false;
        if (m_rejectMessages.get(a_msg.getSender()) == null)
        {
            m_rejectMessages.put(a_msg.getSender(), a_msg);
            Integer count = (Integer) m_rejectCount.get(a_msg.getProposal());
            if (count == null)
                count = new Integer(0);
            int newCount = count.intValue() + 1;
            m_rejectCount.put(a_msg.getProposal(), new Integer(newCount));
            return true;
        }
        return false;
    }

    /**
     * Returns the number of received RejectMessages
     * 
     * @return The number of received RejectMessages
     */
    public int getRejectMessageCount()
    {
        return m_rejectMessages.size();
    }

    /**
     * Indicates if a majority of RejectMessages for some new roundnumber is
     * still possible
     * 
     * @return true if a majority of RejectMessages for some new roundnumber is
     *         still possible, false otherwise
     */
    public boolean isMajorityPossible()
    {
        int needed = m_paxosInstance.getAcceptor().getQuorumTwoThird();
        int open = m_paxosInstance.getAcceptor().getN() - m_rejectMessages.size();
        Enumeration en = m_rejectCount.keys();
        while (en.hasMoreElements())
        {
            String proposal = (String) en.nextElement();
            Integer count = (Integer) m_rejectCount.get(proposal);
            if (count.intValue() + open >= needed)
                return true;
        }
        return false;
    }

    /**
     * Returns the majorities lastCommonRandom if it exists, otherwise null
     * 
     * @return The majorities lastCommonRandom if it exists, otherwise null
     */
    public String getMajorityReject()
    {
        Integer needed = new Integer(m_paxosInstance.getAcceptor().getQuorumTwoThird());
        Enumeration en = m_rejectCount.keys();
        while (en.hasMoreElements())
        {
            String proposal = (String) en.nextElement();
            Integer count = (Integer) m_rejectCount.get(proposal);
            if (count.compareTo(needed) == 0 || count.compareTo(needed) == 1)
            {
                return proposal;
            }
        }
        return null;
    }
}
