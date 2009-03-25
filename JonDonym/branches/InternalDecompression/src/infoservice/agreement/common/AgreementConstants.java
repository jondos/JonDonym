/*
 Copyright (c) 2000 - 2004, The JAP-Team
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
package infoservice.agreement.common;

/**
 * @author LERNGRUPPE A set of used constants.
 */
public interface AgreementConstants
{

    /* Maximum time one phase of the agreement may take */
    long AGREEMENT_TIMEOUT = 100000;// For testing purposes in minute intervals

    // long AGREEMENT_TIMEOUT = 955000;

    /*
     * The time to sleep after phase one to allow others to reach the next phase
     * too
     */
    long AGREEMENT_PHASE_GAP = 25000;

    // long AGREEMENT_PASSIVE_PHASE = 20000;
    /* Maximum time a single agreement (i.e. a broadcast) may take */
    long CONSENSUS_LOG_TIMEOUT = 65000;// For testing purposes in minute

    // intervals

    // long CONSENSUS_LOG_TIMEOUT = 935000;

    /* The start and default round number for the agreement protocol */
    String DEFAULT_COMMON_RANDOM = "0000000000";

    /* The time between checks if a new agreement can be started */
    long TIME_WATCH_POLLING_INTERVAL = 10000;

    /* The timeout of a PaxosRound */
    long PAXOS_ROUND_TIMEOUT = 30000;

    /* When to start the agreement */
    int HOUR_OF_AGREEMENT = 15; // For testing, this is the minutes modulus

    /** @fixme Use 15 minutes or so */
    /* How log to stay passive before starting the agreement */
    long AGREEMENT_PASSIVE_PHASE = 1 * 60 * 1000;

}
