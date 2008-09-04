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

public class ConfirmationMessage extends CommitMessage
{

    /**
     * Creates a <code>ConfirmationMessage</code>. It containts the
     * <code>CommitMessage</code> that should be confirmed among the other
     * InfoServices
     * 
     * @param a_commitMessage
     *            The <code>CommitMessage</code> to be confirmed
     */
    public ConfirmationMessage(CommitMessage a_commitMessage, String a_senderId)
    {
        super(a_commitMessage.getInitMessage(), a_senderId, a_commitMessage.getEchoMessages());
        this.m_messageType = AgreementMessageTypes.MESSAGE_TYPE_CONFIRMATION;
    }

    /**
     * Changes the type of this message to
     * <code>AgreementMessageTypes.MESSAGE_TYPE_COMMIT</code>
     */
    public void changeIntoCommitMessage()
    {
        this.m_messageType = AgreementMessageTypes.MESSAGE_TYPE_COMMIT;
    }
}
