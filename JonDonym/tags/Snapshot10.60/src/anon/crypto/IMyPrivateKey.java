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

import java.security.PrivateKey;

import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;

import anon.util.IXMLEncodable;

/**
 * An interface for a private key for encryption and signing.
 * All private keys should implement a constructor of the following type:
 * <Code> public IMyPublicKey(PrivateKeyInfo a_keyInfo) throws java.security.InvalidKeyException; </Code>
 * It is the only possibility to create them automatically at run time. The big advantage for the
 * code is that it does not need to "know" that a private key class exists. It can be instantiated
 * nevertheless! If you implement your own private key outside the AN.ON library, you must call
 * <Code> anon.util.ClassUtil.loadClasses() </Code> to read your classes into the class cache. Otherwise,
 * the private key may not be found.
 * @see anon.crypto.AsymmetricKeyPair
 * @see anon.util.ClassUtil#loadClasses()
 * @exception InvalidKeyException if no key can be created from this key info
 */
public interface IMyPrivateKey extends PrivateKey, IXMLEncodable
{
	/**
	 * Creates a private key from a private key info. All private keys should implement
	 * this constructor, as it is the only possibility to create them automatically at
	 * run time.
	 * @see anon.crypto.AsymmetricKeyPair
	 * @exception InvalidKeyException if no key can be created from this key info
	 */
	//public IMyPublicKey(PrivateKeyInfo a_keyInfo) throws java.security.InvalidKeyException;

	/**
	 * Creates the corresponding public key to this private key.
	 * @return the corresponding public key to this private key
	 */
	public IMyPublicKey createPublicKey();

	/**
	 * Gets the signature algorithm object that is held and initialised with this key.
	 * It is ready to sign messages and does not need to be reinitialised by the caller.
	 * Therefore, this method must make sure that the algorithm is initialised with this key.
	 * @return the signature algorithm object that is held and initialised by this key
	 */
	public ISignatureCreationAlgorithm getSignatureAlgorithm();

	/**
	 * Gets the private key as a PrivateKeyInfo object.
	 * @return the private key as a PrivateKeyInfo object
	 */
	public PrivateKeyInfo getAsPrivateKeyInfo();
}
