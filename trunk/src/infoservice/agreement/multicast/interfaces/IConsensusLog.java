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

public interface IConsensusLog extends TimeoutListener
{

    /**
     * Indicates if this consensus log is agreed upon
     * 
     * @return <code>true</code> if an agreement has been reached,
     *         <code>false</code> otherwise
     */
    public abstract boolean isAgreed();

    /**
     * Sets the value of m_agreed
     * 
     * @param a_agreed
     *            The new value
     */
    public abstract void setAgreed(boolean a_agreed);

    /**
     * Indicates if this consensus log is commited (i.e. a commit message has
     * been received and the confirmation message is on its way)
     * 
     * @return <code>true</code> if the log is commited, <code>false</code>
     *         otherwise
     */
    public abstract boolean isComitted();

    /**
     * Sets the log to status "commited" or "not commited"
     * 
     * @param a_comitted
     *            The new status
     */
    public abstract void setComitted(boolean a_comitted);

    /**
     * Stop the time out and close the log entry.
     * 
     */
    public abstract void stopTimeout();

    // /**
    // * This methode have to be called if the timout occures.
    // *
    // */
    // public abstract void notifyTimeout();

    /**
     * Returns the ID of the initiator of this consensus
     * 
     * @return The ID of the initiator of this consensus
     */
    public abstract String getInitiatorId();

    /**
     * Returns the unique consensus id for this consensus
     * 
     * @return The consensus id
     */
    public String getConsensusID();

    /**
     * Indicates if this log has been rejected because it used a wrong round id
     * (i.e. last common random)
     * 
     * @return
     */
    public abstract boolean isRejected();

    /**
     * Returns the round number (i.e. last common random)
     * 
     * @return The round number (i.e. last common random)
     */
    public abstract String getLastCommonRandom();

}