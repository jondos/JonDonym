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

import infoservice.agreement.paxos.PaxosAcceptor;
import infoservice.agreement.paxos.PaxosInstance;
import infoservice.agreement.paxos.messages.PaxosMessage;

public class InfoServicePaxosInstance extends PaxosInstance
{

    private PaxosRejectManager m_rejectManager;

    /**
     * Creates a new InfoServicePaxosInstance for for the given acceptor with
     * the given instancenumber
     * 
     * @param a_acceptor
     *            The acceptor this instance belongs to
     * @param a_instanceNumber
     *            The unique identifier of this isntance
     */
    public InfoServicePaxosInstance(PaxosAcceptor a_acceptor, String a_instanceNumber)
    {
        super(a_acceptor, a_instanceNumber);
        m_rejectManager = new PaxosRejectManager(this);
    }

    /**
     * Handles incomming RejectMessages
     */
    public void handleRejectMessage(PaxosMessage a_msg)
    {
        debug("RECEIVED REJECT: MY " + getInstanceNumber() + ", NEW: " + a_msg.getProposal());
        m_rejectManager.addRejectMessage(a_msg);
        debug("HAVE NOW " + m_rejectManager.getRejectMessageCount() + ", NEED "
                + getAcceptor().getQuorumTwoThird());
        if (m_rejectManager.getRejectMessageCount() >= getAcceptor().getQuorumTwoThird())
        {
            String majority = m_rejectManager.getMajorityReject();
            if (majority == null)
            {
                if (!m_rejectManager.isMajorityPossible())
                {
                    ((PaxosAdapter) getAcceptor()).notifyReject(null);
                }
            }
            else
            {
                ((PaxosAdapter) getAcceptor()).notifyReject(majority);
            }

        }
    }
}
