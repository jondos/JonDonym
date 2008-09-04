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

import java.security.SecureRandom;

import org.bouncycastle.crypto.digests.SHA1Digest;

import anon.util.Base64;

public class CommitmentScheme
{
    /**
     * Creates a Commitment for the given value. A SecureRandom is computed and
     * concatenated with the value. Then everything is hashed
     * 
     * @param a_value
     * @return
     */
    public static Commitment createCommitment(String a_value)
    {
        String random = Long.toString(new SecureRandom().nextLong());
        Commitment result = new Commitment(a_value, random);
        return result;
    }

    /**
     * Verifies the given commitment against the given reveal. Reveal gets
     * hashed and compared to the commitment
     * 
     * @param a_commitment
     * @param a_reveal
     * @return
     */
    public static boolean verifyCommitment(String a_commitment, String a_reveal)
    {
        SHA1Digest digest = new SHA1Digest();
        byte[] proposalBytes = a_reveal.getBytes();
        digest.update(proposalBytes, 0, proposalBytes.length);
        byte[] tmp = new byte[digest.getDigestSize()];
        digest.doFinal(tmp, 0);
        String res = Base64.encodeBytes(tmp);
        return res.equals(a_commitment);
    }

}
