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

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

public class PaxosExecution extends PaxosObject
{
    /** The identifier for this execution */
    private String m_initiator;

    /** A Hashtable of all active rounds for this execution */
    private Hashtable m_rounds = new Hashtable();

    /** The PaxosInstance this execution belongs to */
    private PaxosInstance m_paxosInstance;

    private Vector m_roundLeaders = new Vector();

    /** Indicator if this execution has been decided */
    private boolean m_decided = false;

    /**
     * Creates a new PaxosExecution within the given PaxosInstance for the given
     * initiator. A PaxosRound with round number 0 is also created and its
     * timeout is started
     * 
     * @param a_instance
     *            The instance to which the Execution belongs
     * @param a_initiator
     *            The initiator of this execution
     */
    public PaxosExecution(PaxosInstance a_instance, String a_initiator, Vector a_roundLeaders)
    {
        this.m_initiator = a_initiator;
        this.m_paxosInstance = a_instance;
        m_roundLeaders = a_roundLeaders;
        createRound();
    }

    /**
     * Returns the round with the given round number or null if there is no such
     * round exists
     * 
     * @param a_round
     *            The round number
     * @return The corresponding PaxosRound or null if no such round exists
     */
    public PaxosRound getRound(int a_round)
    {
        PaxosRound result = (PaxosRound) m_rounds.get(new Integer(a_round));
        return result;
    }

    /**
     * Creates and returns a new PaxosRound with the given round number
     * 
     * @param a_round
     *            The number of the newly created round
     * @return The newly created PaxosRound
     */
    public PaxosRound createRound(int a_round)
    {
        PaxosRound result = new PaxosRound(this, a_round, getNextRoundLeader(a_round));
        m_rounds.put(new Integer(a_round), result);
        return result;
    }

    /**
     * Creates a PaxosRound with round number 0
     * 
     * @return A PaxosRound with round number 0
     */
    public PaxosRound createRound()
    {
        return createRound(0);
    }

    /**
     * Returns this executions initiators id
     * 
     * @return This executions initiators id
     */
    public String getInitiator()
    {
        return m_initiator;
    }

    /**
     * Returns the PaxosInstance this execution belongs to
     * 
     * @return he PaxosInstance this execution belongs to
     */
    public PaxosInstance getPaxosInstance()
    {
        return m_paxosInstance;
    }

    /**
     * Aborts all active rounds
     */
    public void cancel()
    {
        Enumeration en = m_rounds.keys();
        while (en.hasMoreElements())
        {
            PaxosRound tmp = (PaxosRound) m_rounds.get(en.nextElement());
            tmp.abort();
        }
    }

    /**
     * Decides the given proposal for this execution
     * 
     * @param proposal
     *            The proposal that will be decided
     */
    public void decide(String a_proposal)
    {
        this.m_paxosInstance.addDecision(this.getInitiator(), a_proposal);
        m_decided = true;
    }

    /**
     * Indicates if this execution has been decided
     * 
     * @return true if decided, false otherwise
     */
    public boolean isDecided()
    {
        return m_decided;
    }

    /**
     * Returns the ID of the next rounds leader. This will only be needed if the
     * previous round gets frozen
     * 
     * @param a_round
     *            The round number of the current round
     * @return The ID of the next rounds leader
     */
    public String getNextRoundLeader(int a_round)
    {
        int i = 0;
        for (; i < m_roundLeaders.size(); i++)
        {
            if (getInitiator().equals(m_roundLeaders.get(i)))
                break;
        }
        int index = (i + a_round) % m_roundLeaders.size();
        String g = (String) m_roundLeaders.get(index);
        return g;
    }

    /**
     * Returns a "good" (see Paxos at War) proposal for this execution A
     * proposal is p good if a) There is no pending decission, i.e. the set Poss
     * is empty in all rounds and p = "NULL" b) p is in acc in some round i and
     * in poss in all rounds i \<= k \< a_nextRound
     * 
     * @return A good proposal for this round
     */
    public String getGoodProposal(int a_nextRound)
    {
        String goodValue = null;
        for (int i = 0; i < a_nextRound; i++)
        {
            PaxosRound round = (PaxosRound) m_rounds.get(new Integer(i));
            if (round == null)
            {
                error("(acc) Uuuups, there is no round " + i);
                continue;
            }
            Vector acc = round.getAcc();
            if (acc.isEmpty())
                continue;
            Enumeration en = acc.elements();
            while (en.hasMoreElements())
            {
                String prop = (String) en.nextElement();
                boolean good = true;
                for (int j = i; j < a_nextRound; j++)
                {
                    PaxosRound r = (PaxosRound) m_rounds.get(new Integer(j));
                    if (r == null)
                    {
                        error("(poss) Uuuups, there is no round " + j);
                        continue;
                    }
                    Vector poss = r.getPoss();
                    if (!poss.isEmpty() && !poss.contains(prop))
                    {
                        good = false;
                        break;
                    }
                }
                if (good)
                {
                    goodValue = prop;
                    break;
                }
            }
            if (goodValue != null)
                break;
        }
        if (goodValue == null)
            goodValue = "NULL";
        return goodValue;
    }

}
