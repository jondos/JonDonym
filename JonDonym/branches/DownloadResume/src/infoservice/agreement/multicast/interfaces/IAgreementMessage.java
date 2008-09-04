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

import org.w3c.dom.Document;

import anon.infoservice.IDistributable;

public interface IAgreementMessage extends IDistributable
{

    /**
     * Creates an XML representation of this message.
     * 
     * @return The Docunment:
     */
    public Document toXML();

    /**
     * Indicates if the signature of this message has been correct
     * 
     * @return <code>true</code> if the signature has been correct,
     *         <code>false</code> otherwise
     */
    public boolean isSignatureOK();

    /**
     * Returns an unique key for a Hashtable
     * 
     * @return An unique key for a Hashtable
     */
    public String getHashKey();

    /**
     * Returns the unique id of the consensus this message belongs to
     * 
     * @return The unique id of the consensus this message belongs to
     */
    public String getConsensusId();

    /**
     * Sets if the signature of this message has been ok
     * 
     * @param a_ok
     */
    public void setSignatureOk(boolean a_ok);

    /**
     * Returns the type of this message.
     * 
     * @see infoservice.agreement.multicast.AgreementMessageTypes
     * @return The type of this message
     */
    public int getMessageType();

    /**
     * Sets an XML representation of this message
     * 
     * @param a_node
     */
    public void setXmlDocument(Document a_node);

    /**
     * Sets the round number. This is for simulation purposes only (I think...)
     * 
     * @param string
     */
    public void setLastCommonRandom(String string);

}
