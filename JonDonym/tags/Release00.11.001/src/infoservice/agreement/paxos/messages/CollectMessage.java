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

import java.util.Vector;

public class CollectMessage extends PaxosMessage
{
    private Vector m_proofs;

    /** @fixme false as default! */
    private boolean m_signatureOk = true;

    /**
     * Creates a new CollectMessage
     */
    public CollectMessage()
    {
        super(PaxosMessage.COLLECT);
    }

    /**
     * Sets the vector of FreezeProofMessages
     * 
     * @param a_value
     *            The vector of FreezeProofMessages to be set
     */
    public void setProofs(Vector a_value)
    {
        this.m_proofs = a_value;
    }

    /**
     * Returns the vector of FreezeProofMessages contained in this
     * CollectMessage
     * 
     * @return The vector of FreezeProofMessages contained in this
     *         CollectMessage
     */
    public Vector getProofs()
    {
        return this.m_proofs;
    }

    /**
     * Sets if the signature was ok. Note that this should only be set true, if
     * the signatures of alle FreezeProofMessages also have been good
     * 
     * @param a_sig
     */
    public void setSignatureOk(boolean a_sig)
    {
        this.m_signatureOk = a_sig;
    }

    /**
     * If this returns true, then all the signatures have been correct!
     * 
     * @return
     */
    public boolean isSignatureOk()
    {
        return this.m_signatureOk;
    }
}
