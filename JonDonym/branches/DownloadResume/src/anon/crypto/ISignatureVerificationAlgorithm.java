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
package anon.crypto;

/**
 * This interface represents an algorithm that verifies cryptographic signatures.
 */
public interface ISignatureVerificationAlgorithm
{
	/**
	 * Tests if the signature of a specified message is valid.
	 * @param a_message a message
	 * @param a_signature a signature
	 * @return true if the signature of a specified message is valid; false otherwiese
	 */
	public boolean verify(byte[] a_message, byte[] a_signature);

	/**
	 * Tests if the signature of a specified message is valid.
	 * @param a_message a message
	 * @param message_offset start of message
	 * @param message_len length of message
	 * @param a_signature a signature
	 * @param signature_offset start of signature
	 * @param signature_len length of signature
	 * @return true if the signature of a specified message is valid; false otherwiese
	 */
	 public boolean verify(byte[] a_message, int message_offset,int message_len,
						   byte[] a_signature,int signature_offset,int signature_len);


	/**
	 * Tries to decode a signature in a way as it would meet the W3C standard for XML
	 * signature values.
	 * Without this decoding, RSA XML signatures cannot be verified.
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
