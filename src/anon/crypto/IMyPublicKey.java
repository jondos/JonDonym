/*
 Copyright (c) 2000, The JAP-Team
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

import java.security.PublicKey;

import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;

import anon.util.IXMLEncodable;

/**
 * Represents the public part of an asymmetric cryptographic key pair.
 */
public interface IMyPublicKey extends PublicKey, IXMLEncodable
{
	/**
	 * Gets the signature algorithm object that is held and initialised with this key.
	 * It is ready to verify messages and does not need to be reinitialised by the caller.
	 * Therefore, this method must make sure that the algorithm is initialised with this key.
	 * @return the signature algorithm object that is held and initialised by this key
	 */
	public ISignatureVerificationAlgorithm getSignatureAlgorithm();

	/**
	 * Gets the key as a SubjectPublicKeyInfo object.
	 * @return the key as a SubjectPublicKeyInfo object
	 */
	public SubjectPublicKeyInfo getAsSubjectPublicKeyInfo();

	/**
	 * Returns the length of the key. The length of the key often corresponds with the security it provides.
	 * @return the length of the key
	 */
	public int getKeyLength();

	/**
	 * This method returns if two public keys have the same public key parameters.
	 * @param a_object an other public key
	 * @return true if the keys have the same public key parameters; false otherwise
	 */
	public boolean equals(Object a_object);

	/**
	 * @return the public key`s hash code
	 * @see java.lang.Object#hashCode()
	 */
	public int hashCode();
}
