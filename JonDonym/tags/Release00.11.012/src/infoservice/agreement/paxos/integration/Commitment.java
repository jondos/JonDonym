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

import org.bouncycastle.crypto.digests.SHA1Digest;

import anon.util.Base64;

public class Commitment
{
    private String m_value;

    private String m_random;

    /**
     * Creates a Commitment for the given value. The random is used to prevent
     * codebook attacks (@see CommitmentScheme)
     * 
     * @param a_value
     * @param a_random
     */
    public Commitment(String a_value, String a_random)
    {
        this.m_value = a_value;
        this.m_random = a_random;
    }

    /**
     * Returns this Commitment as String. The value is concatenated with the
     * random and then hashed
     * 
     * @return
     */
    public String getCommitmentAsString()
    {
        String commitment = m_random + "#" + m_value;
        SHA1Digest digest = new SHA1Digest();
        byte[] proposalBytes = commitment.getBytes();
        digest.update(proposalBytes, 0, proposalBytes.length);
        byte[] tmp = new byte[digest.getDigestSize()];
        digest.doFinal(tmp, 0);
        return Base64.encodeBytes(tmp);
    }

    /**
     * Returns the Reveal as string. The reveal is of the form \<random\>\#}\<value\>
     * 
     * @return The Reveal as string. The reveal is of the form \<random\>\#}\<value\>
     */
    public String getRevealAsString()
    {
        return m_random + "#" + m_value;
    }
}
