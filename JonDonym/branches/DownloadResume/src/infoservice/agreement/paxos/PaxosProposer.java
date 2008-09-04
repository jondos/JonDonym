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

public abstract class PaxosProposer extends PaxosCommunicator
{
    public String m_lastRandom;

    /**
     * Proposes a value to the other known PaxosAcceptors
     * 
     * @param a_value
     *            The proposal to make
     */
    public void propose(String a_value)
    {
        debug("Proposing " + a_value);
        PaxosMessage msg = new PaxosMessage(PaxosMessage.PROPOSE);
        msg.setInitiator(getIdentifier());
        msg.setSender(getIdentifier());
        msg.setProposal(a_value);
        msg.setRound(0);
        msg.setPaxosInstanceIdentifier(m_lastRandom);
        multicast(msg);
    }

    /**
     * Sets the round of this proposal
     * 
     * @param a_round
     *            A unique identifier for the round
     */
    public void setRound(String a_round)
    {
        this.m_lastRandom = a_round;
    }
}
