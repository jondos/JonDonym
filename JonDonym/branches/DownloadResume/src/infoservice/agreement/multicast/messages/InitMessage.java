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
package infoservice.agreement.multicast.messages;

import infoservice.agreement.multicast.AgreementMessageTypes;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

public class InitMessage extends AMessage
{

    /**
     * InitMessage for the EchoBroadcast-Protocol.
     * 
     * @param a_initiatorsId
     *            ID of the initiating InfoService
     * @param a_proposal
     *            The message to broadcase
     * @param a_lastCommonRandom
     *            The value of the last commonly known random number as a unique
     *            round identifier
     */
    public InitMessage(String a_initiatorsId, String a_proposal, String a_lastCommonRandom)
    {
        super(null, AgreementMessageTypes.MESSAGE_TYPE_INIT, a_initiatorsId, a_proposal,
                a_lastCommonRandom);
    }

    /*
     * (non-Javadoc)
     * 
     * @see infoservice.agreement.multicast.messages.AMessage#appendCustomNodes(org.w3c.dom.Document,
     *      org.w3c.dom.Node)
     */
    protected void appendCustomNodes(Document a_doc, Node a_rootElem)
    {
        // Nothing to be done here
        return;
    }
}
