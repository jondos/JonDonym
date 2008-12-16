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
package infoservice.dynamic;

import java.util.Vector;

/**
 * Interface for cascade building strategies
 * 
 * @author LERNGRUPPE
 */
public interface IDynamicCascadeBuildingStrategy
{
    /**
     * Creates new mix cascades out of the given first, middle and last mixes.
     * The result is a <code>Vector</code> of <code>MixCascade</code>s. The
     * given seed must be used as the seed for a PRNG, as all InfoServices must
     * come to the same resulting cascades.
     * 
     * @param a_firstMixes
     *            The <code>MixInfo</code>s of the first mixes to be used
     * @param a_middleMixes
     *            The <code>MixInfo</code>s of the middle mixes to be used
     * @param a_lastMixes
     *            The <code>MixInfo</code>s of the last mixes to be used
     * @param a_seed
     *            The seed for the PRNG
     * @return A <code>Vector</code> if <code>MixCascade</code> containing
     *         the new cascades
     * @throws Exception
     */
    public Vector createCascades(Vector a_firstMixes, Vector a_middleMixes, Vector a_lastMixes,
            long a_seed) throws Exception;
}
