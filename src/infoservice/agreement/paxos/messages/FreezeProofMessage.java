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

public class FreezeProofMessage extends PaxosMessage
{
    private String m_weakValue;

    private String m_strongValue;

    private Object m_signedData;

    /** @fixme false as default! */
    private boolean m_signatureOk = true;

    /**
     * Creates a new FreezeProofMessage
     */
    public FreezeProofMessage()
    {
        super(PaxosMessage.FREEZEPROOF);
    }

    /**
     * Returns the weakly accepted value contained in this proof
     * 
     * @return The weakly accepted value contained in this proof
     */
    public String getWeakValue()
    {
        return this.m_weakValue;
    }

    /**
     * Sets the weakly accepted value of this proof
     * 
     * @param a_value
     *            The weakly accepted value of this proof
     */
    public void setWeakValue(String a_value)
    {
        this.m_weakValue = a_value;
    }

    /**
     * Returns the strongly accepted value contained in this proof
     * 
     * @return The weaktrongly accepted value contained in this proof
     */
    public String getStrongValue()
    {
        return this.m_strongValue;
    }

    /**
     * Sets the strongly accepted value of this proof
     * 
     * @param a_value
     *            The strongly accepted value of this proof
     */
    public void setStrongValue(String a_value)
    {
        this.m_strongValue = a_value;
    }

    /**
     * Sets if the signature was ok.
     * 
     * @param a_sig
     */
    public void setSignatureOk(boolean a_sig)
    {
        this.m_signatureOk = a_sig;
    }

    /**
     * Indicates if the signature of this message has been ok
     * 
     * @return
     */
    public boolean isSignatureOk()
    {
        return this.m_signatureOk;
    }

    /**
     * Returns the signed raw data for this message. As we cannot forge
     * signatures and we need the signature for CollectMessages, the message
     * factory must set the raw data
     * 
     * @return
     */
    public Object getSignedData()
    {
        return this.m_signedData;
    }

    /**
     * Sets the signed raw data of this message
     * 
     * @param data
     */
    public void setSignedData(Object data)
    {
        this.m_signedData = data;
    }
}
