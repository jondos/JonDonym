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

import infoservice.agreement.paxos.messages.PaxosMessage;

import java.util.Enumeration;
import java.util.Hashtable;

public abstract class PaxosCommunicator extends PaxosObject
{
    protected Hashtable m_targets = new Hashtable();

    /**
     * Multicasts the given message to all known IPaxosTargets. Uses
     * <code>sendMessage</code>
     * 
     * @param a_msg
     *            The message to multicast
     */
    public void multicast(PaxosMessage a_msg)
    {
        Enumeration en = m_targets.keys();
        while (en.hasMoreElements())
        {
            IPaxosTarget target = (IPaxosTarget) m_targets.get(en.nextElement());
            sendMessage(a_msg, target);
        }
    }

    /**
     * Sends the given message to the IPaxosTarget with the given ID
     * 
     * @param a_id
     *            The ID of the IPaxosTarget to which to send
     * @param a_msg
     *            The message to send
     */
    public void sendMessage(String a_id, PaxosMessage a_msg)
    {
        IPaxosTarget target = (IPaxosTarget) m_targets.get(a_id);
        sendMessage(a_msg, target);
    }

    /**
     * Returns the Hashtable of all known targets
     * 
     * @return The Hashtable of all known targets
     */
    public Hashtable getTargets()
    {
        return m_targets;
    }

    /**
     * Returns the quorum needed to strong-accept a value: (n+f)/2
     * 
     * @return The quorum needed to strong-accept a value: (n+f)/2
     */
    protected int getQuorumStrong()
    {
        int result = (int) ((getN() + getF()) / 2.0);
        return result;
    }

    /**
     * Gets the quorum needed to fast decide (n+3f)/2
     * 
     * @return The quorum needed to fast decide (n+3f)/2
     */
    protected int getQuorumDecideWeak()
    {
        int result = (int) ((getN() + 3 * getF()) / 2.0);
        return result;
    }

    /**
     * Returns the quorum needed to decide if a proposal has been
     * strong-accepted (2f)
     * 
     * @return The quorum needed to decide if a proposal has been
     *         strong-accepted (2f)
     */
    protected int getQuorumDecideStrong()
    {
        int result = 2 * getF();
        return result;
    }

    /**
     * Returns f, the number of maximum tolerable faulty processes
     * 
     * @return f, the number of maximum tolerable faulty processes
     */
    protected int getF()
    {
        int result = nextLower(1 / 3.0 * getN());
        return result;
    }

    /**
     * Returns n, the number of all known processes
     * 
     * @return
     */
    public int getN()
    {
        return m_targets.size();
    }

    public int getQuorumTwoThird()
    {
        return nextUpper((2 * getN()) / 3.0);
    }

    /**
     * Sets the targets, i.e. known other participants
     * 
     * @param a_targets
     *            A Vector containing IPaxosTargets
     */
    public void setTargets(Hashtable a_targets)
    {
        this.m_targets = a_targets;
    }

    /**
     * Returns the next larger integer to the given double
     * 
     * @param a_bottom
     * @return
     */
    private int nextUpper(double a_bottom)
    {
        int tmp = (int) a_bottom;
        if (tmp < a_bottom)
            tmp++;
        return tmp;
    }

    /**
     * Returns the next lower integer for the given double
     * 
     * @param a_roof
     *            The double
     * @return The next lower integer
     */
    private int nextLower(double a_roof)
    {
        int tmp = (int) a_roof;
        if (tmp >= a_roof)
            tmp -= 1;
        return tmp;
    }

    /**
     * Sends the given message to the given target.
     * 
     * @param a_msg
     * @param a_target
     */
    public abstract void sendMessage(PaxosMessage a_msg, IPaxosTarget a_target);

    /**
     * Returns the unique identifier for this PaxosCommunicator
     * 
     * @return The unique identifier for this PaxosCommunicator
     */
    protected abstract String getIdentifier();
}
