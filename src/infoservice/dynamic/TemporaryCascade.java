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
package infoservice.dynamic;

import anon.infoservice.AbstractDatabaseEntry;
import anon.infoservice.Constants;
import anon.infoservice.MixCascade;

/**
 * This class represents a temporary mix-cascade. A cascade is considered
 * temporary if it is not yet physically established. This happens in two
 * szenarios: a) In the semi-dynamic model a last mix' operator selected some
 * mixes for a cascade and the last mix posts the cascade-info to the
 * InfoService. b) The InfoService build new fully dynamic cascades.
 * 
 * A temporary cascade becomes a real cascade when the first mix posts a
 * cascade-status for the first time.
 * 
 * @author LERNGRUPPE
 * 
 */
public class TemporaryCascade extends AbstractDatabaseEntry
{

    /* The proxy'd MixCascade */
    private MixCascade cascade;

    /**
     * Creates a new temporary cascade using the given real MixCascade
     * 
     * @param a_cascade
     */
    public TemporaryCascade(MixCascade a_cascade)
    {
        this(System.currentTimeMillis() + Constants.TIMEOUT_TEMPORARY_CASCADE);
        this.cascade = a_cascade;
    }

    private TemporaryCascade(long a_expireTime)
    {
        super(a_expireTime);
    }

    /*
     * (non-Javadoc)
     * 
     * @see anon.infoservice.AbstractDatabaseEntry#getId()
     */
    public String getId()
    {
        return this.cascade.getId();
    }

    public long getLastUpdate()
    {
        return cascade.getLastUpdate();
    }

    /*
     * (non-Javadoc)
     * 
     * @see anon.infoservice.AbstractDatabaseEntry#getVersionNumber()
     */
    public long getVersionNumber()
    {
        return this.cascade.getVersionNumber();
    }

    /**
     * Returns the proxy'd real MixCascade
     * 
     * @return The proxy'd real MixCascade
     */
    public MixCascade getRealCascade()
    {
        return this.cascade;
    }

}
