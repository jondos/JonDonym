/*
 * Java Forward Error Correction Library
 * Copyright (C) 2001 Onion Networks
 * Copyright (C) 2000 OpenCola
 *
 * Portions derived from code by Luigi Rizzo:
 * fec.c -- forward error correction based on Vandermonde matrices
 * 980624
 * (C) 1997-98 Luigi Rizzo (luigi@iet.unipi.it)
 *
 * Portions derived from code by Phil Karn (karn@ka9q.ampr.org),
 * Robert Morelos-Zaragoza (robert@spectra.eng.hawaii.edu) and Hari
 * Thirumoorthy (harit@spectra.eng.hawaii.edu), Aug 1995
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above
 *    copyright notice, this list of conditions and the following
 *    disclaimer in the documentation and/or other materials
 *    provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE AUTHORS ``AS IS'' AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
 * PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE AUTHORS
 * BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY,
 * OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA,
 * OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
 * TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY
 * OF SUCH DAMAGE.
 */

package anon.mixminion.fec;


/**
 * This is the abstract class is subclassed in order to plug in new FEC
 * implementations.  If you wish to use the default implementation defined by
 * the property "com.onionnetworks.fec.defaultcodefactoryclass" you should
 * simply call:
 *
 * <code>
 *   FECCodeFactory factory = FECCodeFactory.getDefault();
 * </code>
 *
 * (c) Copyright 2001 Onion Networks
 * (c) Copyright 2000 OpenCola
 *
 * @author Justin F. Chapweske (justin@chapweske.com)
 * @author JAP-Team -- made some changes
 */
public class FECCodeFactory {


    protected FECCodeFactory() {}

    /**
     * @return An FECCode for the appropriate <code>k</code> and <code>n</code>
     * values.
     */
    public FECCode createFECCode(int k, int n) {

        // See if there is a cached code.
        if (k < 1 || k > 65536 || n < k || n > 65536) {
            throw new IllegalArgumentException
                    ("k and n must be between 1 and 65536 and n must not be " +
                     "smaller than k: k=" + k + ",n=" + n);
        }

         if (n <= 256) {
            return new PureCode(k, n);
        } else {
            return new Pure16Code(k, n);
        }
    }


    /**
     * @return this
     */
    public synchronized static FECCodeFactory getDefault() {

        return new FECCodeFactory();
    }
}
