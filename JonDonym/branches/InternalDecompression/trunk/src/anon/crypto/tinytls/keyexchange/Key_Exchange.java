/*
 Copyright (c) 2004, The JAP-Team
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
/*
 * Created on Mar 29, 2004
 *
 */
package anon.crypto.tinytls.keyexchange;

import java.math.BigInteger;

import anon.crypto.IMyPrivateKey;
import anon.crypto.JAPCertificate;
import anon.crypto.tinytls.TLSException;

/**
 * @author stefan
 *
 * Abstract Class which is performs the key exchange
 */
public abstract class Key_Exchange
{

	/**
	 * Constructor
	 *
	 */
	public Key_Exchange()
	{
	}

	public abstract byte[] generateServerKeyExchange(IMyPrivateKey key, byte[] clientrandom,
		byte[] serverrandom) throws TLSException;

	/**
	 * Decode the server keys and check the certificate
	 * @param bytes server keys
	 * @param clientrandom clientrandom
	 * @param serverrandom serverrandom
	 * @param servercertificate servercertificate
	 * @throws TLSException
	 */
	public abstract void processServerKeyExchange(byte[] b, int b_offset, int b_len,
												  byte[] clientrandom, byte[] serverrandom,
												  JAPCertificate cert) throws TLSException;

	/**
	 * calculates server finished message
	 * @param handshakemessages
	 * handshakemessages
	 * @return
	 * server finished message
	 */
	public abstract byte[] calculateServerFinished(byte[] handshakemessages);

	/**
	 * checks the server finished message
	 * @param b server finished message
	 * @throws TLSException
	 */
	public abstract void processServerFinished(byte[] b, int len, byte[] handshakemessages) throws
		TLSException;

	/**
	 * processes the client key exchange
	 * @param dh_y
	 * diffie hellman parameter
	 */
	public abstract void processClientKeyExchange(BigInteger dh_y);

	/**
	 * generates the client key exchange message (see RFC2246)
	 * @return client key exchange message
	 * @throws TLSException
	 */
	public abstract byte[] calculateClientKeyExchange() throws TLSException;

	/**
	 * checks the client finished message
	 * @param verify_data
	 * verify data
	 * @param handshakemessages
	 * handshakemessages
	 * @throws TLSException
	 */
	public abstract void processClientFinished(byte[] verify_data, byte[] handshakemessages) throws
		TLSException;

	/**
	 * generate the client finished message (see RFC2246)
	 * @param handshakemessages all handshakemessages that have been send before this
	 * @return client finished message
	 */
	public abstract byte[] calculateClientFinished(byte[] handshakemessages) throws TLSException;

	/**
	 * calculates the key material (see RFC2246 TLS Record Protocoll)
	 * @return key material
	 */
	public abstract byte[] calculateKeys();

}
