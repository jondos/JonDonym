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
package anon.crypto;

import java.security.NoSuchAlgorithmException;

/**
 * Provides methods that implement the Unix crypt command for creation of password hashes.
 */
public interface ICrypt
{
	/**
	 * Creates a UNIX/BSD/Linux-compatible hash value from a password String.
	 * @param a_password a password as String
	 * @return a hash value for the password
	 * @throws NoSuchAlgorithmException if the hash algorithm is not available on the current system
	 */
	public String crypt(String a_password) throws NoSuchAlgorithmException;

	/**
	 * Creates a UNIX/BSD/Linux-compatible hash value from a password String and a salt value.
	 * The maximum length of the salt may vary with the algorithm
	 * @param a_password a password as String
	 * @param a_salt a salt as String; the maximum length may vary
	 * @return a hash value for the password
	 * @throws NoSuchAlgorithmException if the hash algorithm is not available on the current system
	 */
	public String crypt(String a_password, String a_salt) throws NoSuchAlgorithmException;
}
