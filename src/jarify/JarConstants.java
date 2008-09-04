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
package jarify;

/**
 * Defines some usefull gloabal constants.
 */
final class JarConstants
{
	/// Static variables/initializers

	/**
	 * the realtiv path (to jar file) to meta-if directory
	 */
	public final static String META_INF_DIR = "META-INF";

	/**
	 * the realtiv (to jar file) path for manifest file
	 */
	public final static String MANIFEST_FILE = META_INF_DIR + "/" + "MANIFEST.MF";

	/**
	 * the key that starts a new description block for a new file in the manifest file
	 */
	public final static String MANIFEST_ENTRY = "Name: ";

	/**
	 * the name of the MD2 Digest
	 */
	public final static String MD2_DIGEST = "MD2-Digest";

	/**
	 * the name of the MD4 Digest
	 */
	public final static String MD4_DIGEST = "MD4-Digest";

	/**
	 * the name of the MD5 Digest
	 */
	public final static String MD5_DIGEST = "MD5-Digest";

	/**
	 * the name of the RIPEMD128 Digest
	 */
	public final static String RIPEMD128_DIGEST = "RIPEMD128-Digest";

	/**
	 * the name of the RIPEMD160 Digest
	 */
	public final static String RIPEMD160_DIGEST = "RIPEMD160-Digest";

	/**
	 * the name of the SHA1 Digest
	 */
	public final static String SHA1_DIGEST = "SHA1-Digest";

	/**
	 * the name of the SHA256 Digest
	 */
	public final static String SHA256_DIGEST = "SHA256-Digest";

	/**
	 * the name of the SHA384 Digest
	 */
	public final static String SHA384_DIGEST = "SHA384-Digest";

	/**
	 * the name of the SHA512 Digest
	 */
	public final static String SHA512_DIGEST = "SHA512-Digest";

	/**
	 * the name of the Tiger Digest
	 */
	public final static String TIGER_DIGEST = "Tiger-Digest";

	/*
	 /**
	  * Maximum size in bytes of a certificate
	  *-/
	  public final static int MAX_CERT_SIZE		= 1048576;
	  */
}
