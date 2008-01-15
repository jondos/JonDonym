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
package jpi.db;
import java.util.Vector;

/**
 * Data container for an account's balance
 *
 * @author Andreas Mueller, Bastian Voigt
 *
 * Elmar: only called by PICommandUser on Database, changed it so now we get a (then unsigned) XMLBalance
 *        directly from the Database, so now this class is not needed any more and can be deleted
 *        (so I won't bother to update it to the new accountbalance format)
 */
public class Balance
{
    public long deposit;
    public long spent;

    // timestamps
    public java.sql.Timestamp timestamp;
    public java.sql.Timestamp validTime;

    // payment confirmations
    public Vector confirms;

    public Balance( long deposit, long spent,
		    java.sql.Timestamp timestamp,
		    java.sql.Timestamp validTime,
		    Vector confirms )
    {
        this.deposit = deposit;
        this.spent = spent;
				this.timestamp = timestamp;
				this.validTime = validTime;
        this.confirms = confirms;
    }
}
