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

/* Hint: This file may be only a copy of the original file which is always in the JAP source tree!
 * If you change something - do not forget to add the changes also to the JAP source tree!
 */

package anon.crypto;

import java.security.InvalidKeyException;

/**
 * This interface represents a signature algorithm.
 */
public interface IMySignature extends ISignatureVerificationAlgorithm, ISignatureCreationAlgorithm
{
	/**
	 * Initialises the algorithm for verifying. This must be done before doing the verify operation.
	 * The general contract of this method is that it must check if the algorithm has previously
	 * been initialised with the given key. If yes the method does nothing to save resources.
	 * @param a_publicKey a public key
	 * @throws InvalidKeyException if the key is invalid
	 */
	public void initVerify(IMyPublicKey a_publicKey) throws InvalidKeyException;

	/**
	 * Initialises the algorithm for signing. This must be done before doing the sign operation.
	 * The general contract of this method is that it must check if the algorithm has previously
	 * been initialised with the given key. If yes the method does nothing to save resources.
	 * @param a_privateKey a private key
	 * @throws InvalidKeyException if the key is invalid
	 */
	public void initSign(IMyPrivateKey a_privateKey) throws InvalidKeyException;

	/**
	 * Tests if the signature of a specified message is valid.
	 * @param a_message a message
	 * @param a_signature a signature
	 * @return true if the signature of a specified message is valid; false otherwiese
	 */
	public boolean verify(byte[] a_message, byte[] a_signature);

	/**
	 * Signs a message and returns the signature.
	 * @param a_message a message
	 * @return the signature that was created
	 */
	public byte[] sign(byte[] a_message);

	/**
	 * Encodes a signature in a way it meets the W3C standard for XML signature values.
	 * Without this encoding, XML signatures cannot be created by this algorithm.
	 * @param a_signature an non-encoded signature
	 * @return the encoded signature or null if an error occured
	 * @see http://www.w3.org/TR/xmldsig-core/#sec-SignatureAlg
	 */
	public byte[] encodeForXMLSignature(byte[] a_signature);

	/**
	 * Tries to decode a signature in a way as it would meet the W3C standard for XML
	 * signature values.
	 * Without this decoding, XML signatures cannot be verified by this algorithm.
	 * @param a_encodedSignature an encoded signature
	 * @return the decoded signature or null if an error occured
	 * @see http://www.w3.org/TR/xmldsig-core/#sec-SignatureAlg
	 */
	public byte[] decodeForXMLSignature(byte[] a_encodedSignature);

	/**
	 * Returns a description of the the signature algorithm for XML signatures as defined in
	 * http://www.w3.org/TR/xmldsig-core/#sec-AlgID. This description is optional,
	 * documents may be signed without it.
	 * @return a description of the the signature algorithm for XML signatures
	 * @see http://www.w3.org/TR/xmldsig-core/#sec-AlgID
	 */
	public String getXMLSignatureAlgorithmReference();
}
