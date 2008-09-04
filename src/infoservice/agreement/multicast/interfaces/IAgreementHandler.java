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

import infoservice.agreement.common.TimeoutListener;
import infoservice.agreement.logging.IAgreementLog;

/**
 * @author LERNGRUPPE Provides a set of methods for achieving an agreement
 *         between all infoservices.
 */

public interface IAgreementHandler extends TimeoutListener
{
    public void setLog(IAgreementLog a_log);

    /**
     * Handles incomming <code>IAgreementMessage</code>.
     * 
     * @param a_msg
     *            The given message.
     * @throws IllegalArgumentException
     */
    public void handleMessage(IAgreementMessage a_msg) throws IllegalArgumentException;

    /**
     * Starts the agreement protocol. Call <code>prepareAgreementsStart()</code>
     * befor and wait a minute.
     */
    public void startAgreementCommitmentProtocol();

    /**
     * Gets the infoservice which belongs to this agreement handler.
     * 
     * @return
     */
    public IInfoService getInfoService();

    /**
     * This method will be called if a log entry times out. It is used for
     * getting status information only.
     * 
     * @param a_log
     *            The log entry.
     */
    public void notifyConsensusLogTimeout(IConsensusLog a_log);

    // /**
    // * This method will be called if an agreement times out. At this moment
    // * there is knowledge about success or not. For commitment-scheme purpose
    // it
    // * will be called twice. One time in each commitment phase.
    // */
    // public void notifyAgreementTimeout();

    /**
     * This is needes for testing purposes only.
     * 
     * @param a_commonRandom
     *            The value of the current round number.
     */
    public void setLastCommonRandom(String a_commonRandom);

    /**
     * This activates the agreement protocol in passiv mode, whitch means that
     * incomming messages will be handled but we do not start our own
     * <code>InitMessage</code.
     */
    public void reset();

    /**
     * @return Whether the agreement is just running.
     */
    // public boolean isAgreementIsStillRunnig();
}
