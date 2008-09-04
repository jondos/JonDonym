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

import org.bouncycastle.crypto.engines.AESFastEngine;
import org.bouncycastle.crypto.params.KeyParameter;

/**
 * This class is a wrapper for doing AES decryption stuff.
 */
public class AesDecryption {

  /**
   * Stores the used AES decryption algorithm.
   */
  private AESFastEngine m_decryptionInstance;


  /**
   * Creates a new instance of AesDecryption. The size of the key must be 16 bytes (128 bit),
   * 24 bytes (192 bit) or 32 bytes (256 bit). If the key size doesn't fit, an exception is
   * thrown.
   *
   * @param a_aesKey The 128 bit or 192 bit or 256 bit AES key.
   */
  public AesDecryption(byte[] a_aesKey) throws Exception {
    m_decryptionInstance = new AESFastEngine();
    m_decryptionInstance.init(false, new KeyParameter(a_aesKey));
  }


  /**
   * Decrypts one single cipher data block and returns the plain data block. The blocksize is
   * always 16 bytes (128 bit). If the cipher data block is shorter than 16 bytes, an exception
   * is thrown, if it is longer, only the first 16 bytes are decrypted and returned in the plain
   * block.
   *
   * @param a_cipherData The cipher data block.
   *
   * @return The plain data block. The length is always 16 bytes.
   */
  public byte[] decrypt(byte[] a_cipherData) throws Exception {
    byte[] plainBlock = new byte[16];
    m_decryptionInstance.processBlock(a_cipherData, 0, plainBlock, 0);
    return plainBlock;
  }

}
