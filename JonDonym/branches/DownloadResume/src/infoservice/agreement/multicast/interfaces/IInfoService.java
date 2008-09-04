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
package infoservice.agreement.multicast.interfaces;

public interface IInfoService
{
    /**
     * Returns an unique identifier for this <code>IInfoService</code>
     * 
     * @return An unique identifier for this <code>IInfoService</code>
     */
    public abstract String getIdentifier();

    /**
     * Handles the incomming <code>IAgreementMessage</code>
     * 
     * @param a_message
     */
    public abstract void handleMessage(IAgreementMessage a_message);

    /**
     * Multicasts the given message to all known InfoServices (except this one)
     * 
     * @param a_message
     */
    public void multicastMessage(IAgreementMessage a_message);

    /**
     * Sends the given message to the InfoService with the given ID
     * 
     * @param a_id
     *            The ID of the InfoService
     * @param a_echoMessage
     *            The message
     */
    public void sendMessageTo(String a_id, IAgreementMessage a_echoMessage);

    /**
     * Callback of the agreement handler. Gets called if an agreement was
     * reached
     * 
     * @param a_newCommonRandomSeed
     *            The newly agreed upon common random
     */
    public void notifyAgreement(Long a_oldCommonRandomSeed, Long a_newCommonRandomSeed);

    /**
     * Returns the number of known InfoServices
     * 
     * @return The number of known InfoServices
     */
    public int getNumberOfAllInfoservices();
}